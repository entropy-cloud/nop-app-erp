# MA2 并发与乐观锁审计报告（A2.17）

> Audit Status: closed
> 里程碑：MA2 | 维度：并发与乐观锁（A2.17 收口裁决）
> 域/功能模块：全 19 域（@Version 覆盖矩阵 + lost-update 防护 + REQUIRES_NEW 跨域隔离 + 幂等 + 并发状态翻转 + 定时任务并发）
> 报告日期：2026-07-28
> Skill：`docs/skills/open-ended-audit-prompt.md`（开放式并发正确性审查）
> 来源 plan：`docs/plans/2026-07-28-1249-3-audit-remediation-ma2-concurrency-optimistic-lock.md`
> 交接范围：MA2 A2.1–A2.16 全部状态机/业财端到端审计交接的 40+ 并发敏感点（finance 5+2 + mfg 5+5 + hr 5+5 + inventory 4 + ext 9 + aps/logistics 2 + 承付 4 + use-case-implementation-audit 3）

## 0. 执行摘要

本审计是 MA2 并发维度的**收口裁决**。结论：

- **3 项 P0**（已注入异步 fix plan，触击 ask-first ORM 保护区域 / 排产引擎逻辑，须经独立 plan-audit + 人工确认）：
  - `P0-MA2-018` finance `erp_fin_voucher_bill_r(billCode, businessType)` **无 DB 唯一约束** + REQUIRES_NEW 下 `alreadyPosted()` 是 TOCTOU pre-check query → 并发 `IErpFinVoucherBiz.post()` / 兜底重试 / 人工重试可创建**重复凭证**（业财幂等键破坏）
  - `P0-MA2-019` aps **排产产能并发双倍占用**（owner doc `docs/design/aps/state-machine.md §4` 显式声明"乐观锁/资源锁"防护**未落地**）→ 并发 `scheduleForward` 在共享工作中心上分配重叠产能（产能预留不变量破坏）
  - `P0-MA2-020` inventory **`erp_inv_stock_balance` 自然键无唯一约束** → `StockMoveBookkeeper.upsertBalance` / `ErpInvOwnershipTransferProcessor.upsertTargetBalance` 的 check-then-insert 窗口下并发首次移动单 INSERT 重复余额行（silent split-quantity corruption，破坏余额守恒不变量）
- **8 项新 P1**（登记 arm-index §P1 汇总，目标 MR1）
- **6 项 MA2 交接敏感点终态裁决**为 sustained（UC-INV-08 超卖 / UC-SAL-10 双重核销 / 期间重复结账 / 核销 ErpFinArApItem 降级 / 承付 commit-release 竞态 / 过账 REQUIRES_NEW 主事务一致性——**全部经证据证伪**为 P0，@Version 透明乐观锁在临界 read-modify-write 路径**生效**）
- **@Version 覆盖矩阵**：全 19 域 **336 个自有实体 100% 声明 `versionProp="version"`**（含交接点假定的缺口 ErpMfgMrpPlanLine——A2.6b 交接"无 versionProp 行级缺口"经实仓 `module-manufacturing/model/app-erp-manufacturing.orm.xml:809` **证伪**，行已声明 versionProp）
- **透明乐观锁降级**：6 处候选经证据**全部证伪**——业务方法均经 `dao.findAllByQuery`/`getEntityById` 加载托管实体 + 原地 mutate + `updateEntity`/`saveOrUpdateEntity`/`tryUpdateWithVersionCheck` flush，@Version 自动校验生效（无 new-instance / 裸 SQL / 批量 update 绕过路径）
- **REQUIRES_NEW 跨域失败隔离正确性**：经 `IErpFinVoucherBiz.post:71` + `reverseVoucher:79` 显式 `@Transactional(REQUIRES_NEW)` 钉 Facade 一致落地（11 域 PostingExecutor/Dispatcher 复用），主事务单据审核落库 + 过账悬挂 PENDING 经 `ErpFinDeferredPostingRetryHelper` 兜底重试 + `ErpFinPostingExceptionRecorder` 双 REQUIRES_NEW 异常落库；**但幂等键无 DB 唯一约束兜底**（P0-MA2-018）
- **定时任务并发**：全 19 个 cron job 运行于 `nop-job-local` 的 `LocalJobScheduler`（**非分布式**，无 leader-lock，无 `IErpSysLockBiz`）+ 全部默认 `enabled=false`；9 job 幂等设计（recompute/refresh 类 + 兜底扫描经引擎 `alreadyPosted()` 去重），**10 job 并发执行产生重复副作用**（P1-MA2-086）
- **多轴状态机并发翻转**：8/9 Processor 组 **sustained**（显式 `validateTransitionFor*` 源态守卫 + `versionProp` 乐观锁），仅 `ErpAstDepreciationScheduleProcessor.executeDepreciation` 缺 `status==PENDING` 守卫（P1-MA2-089）

并发维度终态：**全域 ⚠️(P0→fix-plan + P1)**——并发正确性主路径经透明乐观锁保护成立，但 3 处 P0 缺 DB 唯一约束 / 显式锁落地破坏幂等与余额/产能不变量，须经 fix plan 闭包。

## 1. Current Baseline 复核

实时仓库已落地的并发防护（经实仓证据复核）：

- **@Version 透明乐观锁**（全 19 域自有实体 100% 声明 `versionProp="version"`）— 见 §2 覆盖矩阵
- **事务边界**（`flow-overview.md §6.1` 三种事务范围经证据落地）：
  - 单据审核 + 库存变更：跨域 `REQUIRED` 强一致（`ErpInvStockMoveBizModel.generateMove` 等经 `@BizMutation` 包装 + `IErpInvStockMoveBiz` Facade）
  - 单据审核 + 凭证生成：`IErpFinVoucherBiz.post` `REQUIRES_NEW` 独立事务隔离（`ErpFinVoucherBizModel.java:71` 显式声明，11 域 PostingExecutor/Dispatcher 复用）
  - 期末结账：单库 `REQUIRED`（`ErpFinAccountingPeriodProcessor.closePeriod` 全链同事务）
- **兜底机制**（`flow-overview.md §6.2`）：`ErpFinDeferredPostingRetryHelper` 每 5 分钟 REQUIRES_NEW 单条重试 `PENDING` 且 `retryCount<3` 的过账异常 + `markRetried` 状态守卫 + O-16 已提交但 posted=true 失败补偿
- **对账机制**（`flow-overview.md §6.3`）：每日对账四项（库存余额 vs 流水 / 应收余额 vs 发票-收款 / 应付余额 vs 发票-付款 / 总账余额 vs 凭证明细）
- **REQUIRES_NEW 跨域失败隔离**：`ErpFinPostingExceptionRecorder` 双 REQUIRES_NEW（异常记录 + 通知派发各自独立事务）+ `ErpFinPostingException` 状态机 PENDING→RETRIED/RESOLVED/IGNORED 经 `versionProp` + `requirePending` 守卫
- **未落地防护**（本审计发现）：`erp_fin_voucher_bill_r(billCode, businessType)` 无 DB 唯一约束（P0-MA2-018）；aps 排产产能无锁（P0-MA2-019）；`erp_inv_stock_balance` 自然键无唯一约束（P0-MA2-020）

**审计方法论**：开放式审查（`open-ended-audit-prompt.md`）—— 不局限于结构化检查清单，跨工件跨维度搜索隐藏 lost-update。审查维度：(1) @Version 覆盖矩阵；(2) 透明乐观锁降级；(3) lost-update 防护；(4) REQUIRES_NEW 跨域失败隔离；(5) 幂等性；(6) 并发状态翻转竞争；(7) 定时任务并发；(8) 与设计文档一致性。

**证据采集**：(a) `module-*/model/app-erp-*.orm.xml` 全域 `versionProp` + 唯一约束 grep；(b) 4 个并发域专项子代理（inventory / finance / mfg+hr+aps+ext / 状态机+cron-job）并行实仓审计；(c) `REQUIRES_NEW` 全文 grep（46 命中跨 11 域）；(d) `FOR UPDATE`/`withLock`/`pessimistic`/`executeUpdate`/`sqlUpdate` 全 `erp-*-service` grep（**0 命中**——无悲观锁、无裸 SQL 写、无批量 update 绕过）。

## 2. 维度一：@Version 覆盖矩阵（核心）

### 2.1 全域覆盖统计

经 `module-*/model/app-erp-*.orm.xml` 全量解析（脚本遍历 `<entity>` 块，区分自有实体与外部实体引用）：

| 域 | 自有实体 | 声明 versionProp | 覆盖率 |
|----|---------|------------------|--------|
| finance | 48 | 48 | 100% |
| hr | 42 | 42 | 100% |
| manufacturing | 41 | 41 | 100% |
| crm | 39 | 39 | 100% |
| assets | 24 | 24 | 100% |
| quality | 21 | 21 | 100% |
| projects | 21 | 21 | 100% |
| contract | 19 | 19 | 100% |
| purchase | 32 | 32 | 100% |
| cs | 18 | 18 | 100% |
| sales | 27 | 27 | 100% |
| maintenance | 20 | 20 | 100% |
| b2b | 16 | 16 | 100% |
| inventory | 31 | 31 | 100% |
| master-data | 25 | 25 | 100% |
| drp | 16 | 16 | 100% |
| aps | 7 | 7 | 100% |
| logistics | 12 | 12 | 100% |
| notify | 3 | 3 | 100% |
| **合计** | **462（含主键实体）** | **462** | **100%** |
| **去重（按自有实体）** | **336** | **336** | **100%** |

**裁决**：@Version 覆盖矩阵 **全 19 域 100% 覆盖**，无任何自有实体遗漏 versionProp。

### 2.2 关键临界实体逐一核验

| 实体 | versionProp 声明位置 | 临界方法 | 裁决 |
|------|---------------------|---------|------|
| `ErpInvStockBalance`（库存余额——并发扣减核心） | `module-inventory/model/app-erp-inventory.orm.xml:369` | `StockMoveBookkeeper.updateBalanceWithRetry`（DONE 路径，retry-on-conflict）/ `applyReservation` / `reclassifyBalance` / 7 costing strategies | ✅ sustained |
| `ErpFinArApItem`（核销——并发核销核心，A2.5c P2-MA2-008/014 降级交接） | `module-finance/model/app-erp-finance.orm.xml:732` | `ErpFinReconciliationBizModel.post` → `ReconciliationSettler.applySettlement`（managed-instance flush） | ✅ sustained（交接点 P2-MA2-008/014 降级维持——@Version 生效，silent lost-update 降为 detectable conflict） |
| `ErpMfgMrpPlanLine`（A2.6b 交接"无 versionProp 行级缺口"） | `module-manufacturing/model/app-erp-manufacturing.orm.xml:809`（**已声明 versionProp="version"**） | `MrpEngine.runMrp`（plan 级 @Version 守护，lines 走 delete+insert） | ✅ 交接缺口证伪（versionProp 已声明）；plan 级 @Version 有效，lines 写入经 plan RUNNING 状态守卫串行化（详见 §6） |
| `ErpFinGlBalance`（总账余额） | `module-finance/model/app-erp-finance.orm.xml:904` | （由过账引擎维护，非 read-modify-write） | ✅ sustained（insert/update 经引擎） |
| hr 7 状态机实体（A2.7a/b 交接覆盖完整性） | `module-hr/model/app-erp-hr.orm.xml`（全 36 实体 100% 声明） | LeaveRequest / Timesheet / Salary / SalarySimulation / BankFile / ShiftSwapRequest / ShiftAssignment | ✅ sustained（7 状态机实体全声明 versionProp） |
| `ErpFinAccountingPeriod`（期间状态机） | `module-finance/model/app-erp-finance.orm.xml:653` | `closePeriod` / `finalizePeriod` / `reverseClose`（managed-instance + flushSession） | ✅ sustained |
| `ErpFinVoucher`（凭证状态机） | `module-finance/model/app-erp-finance.orm.xml:413` | `postVoucher` / `reverseVoucher`（managed-instance + updateEntity） | ✅ sustained |
| `ErpFinPostingException`（过账异常工作台） | `module-finance/model/app-erp-finance.orm.xml`（声明 versionProp） | `retry` / `ignore` / `manualResolve`（managed-instance + requirePending 守卫） | ✅ sustained |

## 3. 维度二：透明乐观锁降级

@Version 存在但是否真保护临界 read-modify-write？— 核验业务方法在"读余额→计算新余额→写回"序列中是否持有同一实体实例使 @Version 生效，还是读出后用新实体/裸 SQL/daoProvider.update 绕过。

**全域 grep 基线**（`erp-*-service/src/main`）：`FOR UPDATE` = 0 / `withLock` = 0 / `pessimistic` = 0 / `executeUpdate` = 0 / `sqlUpdate` = 0 / `IOrmTemplate.batchUpdate` = 0。**无任何悲观锁、裸 SQL 写、批量 update 绕过路径**。

| 临界方法 | 读 | 写 | 写模式 | @Version 有效？ | 裁决 |
|---------|----|----|--------|----------------|------|
| `StockMoveBookkeeper.updateBalanceWithRetry`（inventory DONE 路径） | `upsertBalance:142 findBalance:301 findAllByQuery` | `:243 dao.tryUpdateWithVersionCheck(current)` | managed-instance + 显式 version-check API + retry 循环（max `erp-inv.concurrent-deduct-max-retry=5`） | YES + retry-on-conflict | **sustained**（最强模式） |
| `ErpInvStockMoveProcessor.applyReservation`（inventory） | `:245-246 findAllByQuery` | `:251 saveOrUpdateEntity(balance)` | managed-instance | YES | sustained（fail-fast） |
| `ErpInvOwnershipTransferProcessor.reclassifyBalance`（inventory） | `:130/:194 findAllByQuery` | `:157/:168 saveOrUpdateEntity` | managed-instance（source + target 双实体） | YES | sustained（fail-fast） |
| `CostAdjustmentService.applyAverageLike/applyFifo/reverseLine`（inventory 6 方法） | `findBalance findAllByQuery` | `saveOrUpdateEntity(balance)` | managed-instance | YES | sustained（fail-fast） |
| `ReconciliationSettler.applySettlement`（finance AR-AP 核销） | `loadItems:99 getEntityById` | `ErpFinReconciliationBizModel.post:137 flushBeforeBalance()` → `orm().flushSession()` | managed-instance dirty-check flush | YES（flush 增/校验 version） | sustained（UC-SAL-10 双重核销缺口**证伪**） |
| `ErpFinAccountingPeriodProcessor.closePeriod/finalizePeriod/reverseClose`（finance 期间翻转） | `requirePeriod:620 getEntityById` | `:161/:208/:307 flushSession()` | managed-instance flush | YES | sustained（期间重复结账缺口**证伪**） |
| `ErpFinBudgetCommitmentBizModel.reverseCommitment`（finance 承付 release） | `findCommitmentVouchers` query | `:89 updateEntity(original)`（isReversed=true） | managed-instance update | YES | sustained（承付 release 竞态**证伪**） |
| `ErpHrSalaryBizModel.markPaid`（hr 工资支付轴） | `requireSalary` | `:116 updateEntity(salary)` | managed-instance update + 状态守卫 | YES | sustained |
| `ErpAstDepreciationScheduleProcessor.executeDepreciation`（assets 折旧批量） | `findSchedule may return null` | `:96 newEntity+saveOrUpdateEntity`（INSERT）+ `:114 setAccumulatedDepreciation` mutate asset + `:120 tryPost` | new-entity INSERT（无 version 校验） | NO（首次执行 INSERT 无 version 检查） | **downgrade watch-only**（P1-MA2-089） |
| `ErpMfgMrpPlanBizModel.runMrp`（mfg MRP 并发运算） | plan DRAFT 守卫 | `:85 setStatus(RUNNING)+updateEntity(plan)` + clearLines(delete) + saveEntity(new) + setStatus(COMPLETED)+updateEntity | managed-instance update（plan 级）+ delete+insert（line 级） | plan YES / line 经 RUNNING 守卫串行化 | sustained（plan @Version 守护，重计算浪费但无 lost-update） |

**裁决**：10 处临界方法 **9 处 sustained**（@Version 透明乐观锁在 read-modify-write 路径生效），仅 `executeDepreciation` 首次执行路径 INSERT 无 version 校验（P1-MA2-089）。**6 处 MA2 交接的"透明乐观锁降级"敏感点全部证伪**——业务方法均正确持有托管实体实例使 @Version 生效。

## 4. 维度三：lost-update 防护

并发库存扣减（超卖）/ 并发双重核销 / 并发重复结账 / 并发产能双倍占用——是否存在无 @Version + 无悲观锁 + 无唯一约束的裸 read-modify-write。

| 控制点 | 触发场景 | 防护 | 裁决 |
|--------|---------|------|------|
| **UC-INV-08 超卖**（use-case-implementation-audit 交接） | 并发出库扣减穿透 availableQuantity | `StockMoveBookkeeper.updateBalanceWithRetry` 经 `tryUpdateWithVersionCheck` + retry 循环；`validateAvailable:227` 是 TOCTOU pre-check 但 flush 时 version 冲突触发回滚 | **sustained（缺口证伪）**——@Version + retry 防护成立 |
| **UC-SAL-10 双重核销**（use-case-implementation-audit 交接） | 并发核销同一 AR-AP item | `ReconciliationSettler.applySettlement` 经 managed-instance flush，version 冲突触发回滚 | **sustained（缺口证伪）** |
| **UC-SAL-10 并发扣批次**（use-case-implementation-audit 交接） | 并发核销消耗同一 batch | 批次消耗经 `applySettlement` managed-instance 路径 + `ErpInvStockBalance` versionProp | **sustained（缺口证伪）** |
| **期间重复结账**（finance A2.5b 交接） | 并发 closePeriod 同一期间 | `requirePeriod` getEntityById + `assertPeriodStatus(OPEN)` + flushSession version 校验 | **sustained（缺口证伪）**——并发 close 一个成功一个 OptimisticLock 回滚 |
| **承付并发 commit/release**（A2.16 交接） | 并发 commit 同一订单 / 并发 release 同一 commitment | release: managed-instance update version 有效；commit: 新凭证 INSERT（依赖幂等键——见 P0-MA2-018）；预算余量经 `aggregateAmount` 聚合-on-read（无 consumed 列回写，**无 lost-update 风险**） | **预算控制 sustained**；commit 凭证幂等经 P0-MA2-018 落地 |
| **排产产能双倍占用**（A2.15 交接） | 并发 scheduleForward 共享工作中心 | owner doc `state-machine.md §4` 声明"乐观锁/资源锁"**未落地**——`ErpApsSchedulingProcessor` 无锁、无产能预留实体、无 `(workcenterId, plannedStartT, plannedEndT)` UK | **P0-MA2-019（缺口确认）** |
| **库存余额首次 INSERT 竞态**（本审计发现） | 并发首次移动单同一新维度（material+sku+warehouse+location+batch+owner） | `StockMoveBookkeeper.upsertBalance:137-170` + `ErpInvOwnershipTransferProcessor.upsertTargetBalance:211-237` check-then-insert；`erp_inv_stock_balance` **无自然键 UK** | **P0-MA2-020（缺口确认）** |
| **LandedCost 并发同 receiveId 窗口期**（A2.11 交接） | 并发 approve 同 receiveId 的两个 LandedCost 单 | `ErpInvLandedCostProcessor.validateNotAlreadyAllocated:90` TOCTOU pre-check（query APPROVED siblings）；`erp_inv_landed_cost` 仅 `IDX_INV_LANDED_COST_RECEIVE_ID`（非唯一）；下游 cost 经 managed-instance 写入有 version 守护但双 allocation 落地 | **downgrade watch-only**（P1-MA2-085） |

## 5. 维度四：REQUIRES_NEW 跨域失败隔离正确性

业财过账 `IErpFinVoucherBiz.post` `REQUIRES_NEW` 跨域失败不回滚主事务（记录 PENDING 重试）。核验：

| 控制点 | 证据 | 裁决 |
|--------|------|------|
| (1) 主事务单据审核已落库但过账悬挂的最终一致性 | `ErpFinDeferredPostingRetryHelper.retry:74` 每 5 分钟 REQUIRES_NEW 单条重试 PENDING + retryCount<3 的异常；O-16 补偿（REQUIRES_NEW 已提交但 posted=true 失败）；`markRetried:121-127` 状态守卫 | **sustained** |
| (2) REQUIRES_NEW 是否在所有过账 Facade 一致 | `ErpFinVoucherBizModel.java:71,79` 显式 `@Transactional(REQUIRES_NEW)`；11 域 PostingExecutor/Dispatcher（assets/hr/inv/mnt/mfg/prj/pur/sal/qa/logistics）全部经 `IErpFinVoucherBiz.post` Facade 承接（grep 46 命中） | **sustained**（事务边界钉 Facade 硬规则一致落地） |
| (3) 并发重试是否幂等（同一单据并发重试不重复过账——幂等键 `(billType, billCode)`） | `ErpFinPostingProcessor.alreadyPosted:472` 是 query pre-check（TOCTOU）；`erp_fin_voucher_bill_r` **无 `(billCode, businessType)` UK**（仅 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID` 非唯一） | **P0-MA2-018（缺口确认）**——并发 post/重试可创建重复凭证 |

## 6. 维度五：幂等性

`flow-overview.md §八.3` "所有操作支持重复调用"——核验并发重复触发（轮询重启/重复回调/并发重试）的幂等：

| 控制点 | 幂等机制 | 裁决 |
|--------|---------|------|
| b2b EDI 异步重复回调（A2.14 交接） | `ErpB2bAsnBizModel.handleInboundWebhook:89` → `isDuplicateEvent:439`（query remark="WEBHOOK eventId=..."）TOCTOU；eventId 存 remark 非 UK 列；`EdiDoc UK(formatId,relatedBillType,relatedBillCode)` 但 webhook eventId 不入 UK | **downgrade watch-only**（P1-MA2-086 子项，b2b 默认 config-gated OFF） |
| logistics 网关回调 + scanForPolling（A2.15 交接） | `ErpLogShipmentBizModel` 状态迁移经 `versionProp` 守护；`advanceTracking:154` 已 DELIVERED 返回 false 幂等守卫 | sustained（status 轴）；运费过账侧依赖 P0-MA2-018 修复 |
| 过账兜底重试（flow §6.2） | 依赖 `alreadyPosted` pre-check（TOCTOU，见 P0-MA2-018） | **P0-MA2-018** |
| 承付重复释放守卫（A2.16 交接） | `ErpFinBudgetCommitmentBizModel.reverseCommitment` + `hasUnreversedCommitment` query pre-check + `versionProp` 校验 | sustained（version 有效，pre-check race 由 version 兜底） |
| 期间结账幂等 | `closePeriod` `assertPeriodStatus(OPEN)` 守卫，重复调用直接抛错 | sustained |
| FX 重估（finance） | `ExchangeRevaluationService.revalueArAp` 仅读 ArApItem + 写新凭证；`CloseVoucherWriter.writeVoucher:151` **无幂等 pre-check**；bounded by period version guard（同期间并发 revalue 经期间 version 守护） | downgrade watch-only（P1-MA2-087） |

## 7. 维度六：并发状态翻转竞争

多轴状态机（docStatus + approveStatus + posted）并发翻转——是否经 @Version 或状态守卫防止非法并发迁移。

| Processor | 源态守卫（quote） | @Version | 裁决 |
|-----------|------------------|----------|------|
| `ErpPurOrderProcessor.approve/cancel/reverseApprove` | `validateTransitionForApprove:158-163` (SUBMITTED) + `validateTransitionForCancel:179-184` (docStatus!=CANCELLED) + `validateTransitionForReverseApprove:172-177` (APPROVED) + early-return `isApproved/isRejected` | ✓ | sustained |
| `ErpSalOrderProcessor.approve/cancel/reverseApprove` | 同型 `validateTransitionFor*:159-185` | ✓ | sustained |
| `ErpFinVoucherBizModel.postVoucher/reverseVoucher` | `if (!DRAFT) throw :90-94` / `if (!POSTED) throw :106-110` | ✓ | sustained（状态机轴）；**注：缺 CLOSED_FINAL 期间守卫**（P1-MA2-021 已登记） |
| `ErpFinAccountingPeriodProcessor.closePeriod/finalizePeriod/reverseClose` | `assertPeriodStatus(OPEN/CLOSED_FINAL) :132/206/276` | ✓（managed flush） | sustained（但 closePeriod 重 body 在 setStatus 前执行——REQUIRES_NEW 副作用靠 `alreadyPosted` 去重，依赖 P0-MA2-018 修复） |
| `ErpAstAssetCapitalizationProcessor.approve/reverseApprove` | `validateTransitionForApprove:139-144` + `validateTransitionForReverseApprove:153-158` | ✓ | sustained |
| `ErpAstDisposalProcessor.approve/reverseApprove` | `validateTransitionForApprove:154-159` + `validateTransitionForReverseApprove:168-173` | ✓ | sustained |
| `ErpAstDepreciationScheduleProcessor.reverseDepreciation` | `if (schedule==null \|\| status!=EXECUTED) throw :156-161` | ✓ | sustained |
| `ErpAstDepreciationScheduleProcessor.executeDepreciation` | **无 status==PENDING 守卫**——`requireAsset + validateAssetInService + requirePeriodOpen` 后直接 `newEntity`+`saveOrUpdateEntity` | 部分（INSERT 无 version 校验） | **downgrade watch-only**（P1-MA2-089） |
| `ErpInvStockMoveProcessor.doConfirm/doComplete/cancel/reverse` | `!DRAFT→throw :187-192` / `!CONFIRMED→throw :202-207` / `!in(DRAFT,CONFIRMED)→throw :99-106` / `!DONE→throw :117-121` | ✓ | sustained |
| `ErpInvStockTakeBizModel.startTake/completeTake/cancelTake` | `!DRAFT→throw :29` / `!CONFIRMED→throw :43` / `in(DONE,CANCELLED)→throw :57-62` | ✓ | sustained |

**裁决**：8/9 Processor 组 sustained（显式源态守卫 + versionProp），仅 `executeDepreciation` 缺 PENDING 守卫（P1-MA2-089）。`ErpApsOperationOrderBizModel.start/complete/cancel` 缺状态守卫经 P1-MA2-077（A2.15 已登记）承接——@Version 防止 lost-update 但不防止非法迁移。

## 8. 维度七：定时任务并发

**调度基础设施**：全 19 个 cron job（`app-erp-all/.../nop/job/conf/*.job.yaml` + `*.batch.xml`）运行于 `nop-job-local` 的 `LocalJobScheduler`。经 `nop-entropy/docs-for-ai/03-modules/nop-job.md:209-213` 明示：**"LocalJobScheduler 不参与分布式协调，不要在多实例部署中使用它调度需要唯一执行的作业。不支持 Misfire 补偿、阻塞策略、超时控制、分片。"**

- **无任何 job 声明 leader-lock / singleInstance / dispatch 属性**（local 模式 schema 不支持）
- **无 `nop_sys_cluster_leader` 选举**（无 nop-job-service 依赖）
- **无 `IErpSysLockBiz` / `ISysLock` / 分布式锁**（全域 grep = 0）
- **无 `@CronProvider` / `*.job.xml`**
- 全部 job 默认 `enabled="@cfg:nop.job.<name>.enabled|false"` → **参考部署为 job-silent**；以下风险仅在启用 + 多实例（部分 job 即使单实例也产生重复副作用）

### 8.1 Job 清单与并发裁决

| Job | cron | 幂等机制 | 裁决 |
|-----|------|---------|------|
| `erp-fin-deferred-posting-sweep` | 5m | loader `status=PENDING AND retryCount<3` + per-record REQUIRES_NEW + 引擎 `alreadyPosted()` 去重 | **idempotent**（重复运行只重调用 post，alreadyPosted no-op） |
| `erp-ast-depreciation` | 月度 | 调用 `executeDepreciation`（P1-MA2-089 缺 PENDING 守卫） | **NEITHER**（P1-MA2-089） |
| `erp-fin-ar-ap-auto-recon` | 日度 | AR/AP item status filter + versionProp；concurrent batch 同 item 一个 OL 失败 | **mostly idempotent** |
| `erp-fin-cash-forecast-refresh` | 日度 | 先清区间再写入（recompute） | **idempotent** |
| `erp-mfg-jobcard-auto-generate` | 日度 | 扫描 APS-scheduled 但无 JobCard 的工单；per-wo 去重未验证 | **NEITHER**（P1-MA2-086） |
| `erp-qa-spc-sampling` | 小时 | 收集 subgroup samples + 重算控制限；**append** 语义 | **NEITHER**（P1-MA2-086） |
| `erp-qa-spc-capability` | 日度 | capability 重算 snapshot | **idempotent** |
| `erp-prj-pnl-calc` | 日度 | 项目 PnL 聚合 refresh | **idempotent** |
| `erp-crm-lead-scoring-recalc` | 日度 | lead score recompute overwrite | **idempotent** |
| `erp-crm-funnel-aggregation` | 日度 | funnel rollup recompute | **idempotent** |
| `erp-hr-contract-expiry` | 日度 | ACTIVE+overdue 扫描 → setStatus EXPIRED（status filter 幂等）+ 通知派发（无 dedup） | **NEITHER**（通知重复，P1-MA2-086） |
| `erp-cs-sla-scan` | **每分钟** | 查询 `isSlaCompleted=false AND deadline<now` → 每工单 `writeAction(ESCALATE)` + notify；**无 escalation dedup / 无 isSlaCompleted=true 翻转** | **NEITHER（最严重）**——单实例每分钟重复 ESCALATE 审计行 + 通知（P1-MA2-086） |
| `erp-cs-entitlement-expiry` | 日度 | 通知 + isActive=false 翻转 | **NEITHER**（通知重复） |
| `erp-cs-csat-reminder` | 日度 | survey 提醒发送 | **NEITHER**（提醒重复） |
| `erp-crm-event-reminder` | 15m | 扫描 PLANNED 事件窗口派发提醒 | **NEITHER**（事件跨 tick 重复提醒） |
| `erp-crm-sequence-overdue` | 日度 | sequence 逾期 flag/notify | **NEITHER**（通知重复） |
| `erp-crm-forecast-recalc` | 日度 | refreshForecast aggregation rollup | **idempotent** |
| `erp-mnt-due-visit-generation` | 日度 | 创建 visit（确定性 code `VST-SCH-{schedId}-{date}`）+ 推进 nextDueDate；**无 existence check before insert** | **NEITHER**（重复 visit 行，P1-MA2-086） |
| `erp-mfg-crp-run` | 日度 | CRP load snapshot per work-center | **idempotent** |

**裁决**：9 job 幂等（recompute/refresh 类 + posting sweep 经引擎去重）；**10 job 并发执行产生重复副作用**（通知重复 / 审计行重复 / 实体重复）—— P1-MA2-086 合并登记。**当前默认全部 enabled=false，参考部署 silent**，风险激活条件：启用 + ≥2 实例（或 `erp-cs-sla-scan` 单实例）。

## 9. 维度八：与设计文档一致性

`flow-overview.md §6.1/§6.2/§6.3/§八` vs 实现：

| owner doc 声明 | 实现证据 | 裁决 |
|---------------|---------|------|
| §6.1 三种事务范围（REQUIRED / REQUIRES_NEW / 单库 REQUIRED） | `ErpFinVoucherBizModel:71,79` REQUIRES_NEW + `@BizMutation` REQUIRED + 期末结账单库 REQUIRED | **PASS** |
| §6.2 兜底重试 3 次告警 | `ErpFinDeferredPostingRetryHelper` retryCount<3 + REQUIRES_NEW 单条重试 | **PASS** |
| §6.3 每日对账四项兜底 | 对账机制经业务证据成立（库存/AR/AP/GL 四方对账） | **PASS**（设计声明，归 MA5 测试覆盖审查） |
| §八.2 跨域失败隔离 | `IErpFinVoucherBiz.post REQUIRES_NEW` + PostingExceptionRecorder 双 REQUIRES_NEW | **PASS**（但幂等键无 DB UK 兜底——P0-MA2-018） |
| §八.3 幂等性（所有操作支持重复调用） | 大部分操作经状态守卫 + versionProp 幂等；**但 `post` 重复调用依赖 query pre-check 无 DB UK** | **PARTIAL FAIL**（P0-MA2-018） |
| aps `state-machine.md §4` 乐观锁/资源锁 | **未落地**（`ErpApsSchedulingProcessor` 无锁、无产能预留实体、无 UK） | **FAIL**（P0-MA2-019） |

## 10. MA2 交接敏感点逐项裁决表

| 来源审计 | 交接敏感点 | 证据 | 终态 |
|---------|-----------|------|------|
| finance A2.5b（5 处） | 期间结账并发竞争 + @Version 降级 + CLOSED_FINAL 锁 | closePeriod/finalizePeriod/reverseClose managed flush + versionProp；CLOSED_FINAL 凭证锁定经 P1-MA2-021 已登记 | **sustained**（@Version 有效；P1-MA2-021 维持） |
| finance A2.5c（P2-MA2-008/014） | ErpFinArApItem versionProp 乐观锁降级（并发核销 SETTLED 漂移） | `ReconciliationSettler.applySettlement` managed flush + versionProp 有效 | **sustained**（降级证伪，silent lost-update 降为 detectable conflict；P2 维持） |
| finance A2.16（4 处） | 承付 commit/release 同事务竞争 + 部分开票并发释放 | release 经 managed update version 有效；commit 经新凭证 INSERT（P0-MA2-018 修复幂等）；预算余量 aggregate-on-read 无 lost-update | **sustained**（预算控制）；commit 幂等经 P0-MA2-018 |
| mfg A2.6a（5 处） | 工单状态机并发翻转 @Version 降级 | `ErpMfgWorkOrderProcessor` 每方法显式 `requireStatus`/`validateTransitionFor*` + versionProp + managed update | **sustained** |
| mfg A2.6b（5 处） | MRP 并发运算 + ErpMfgMrpPlanLine 行级缺口 | plan @Version 守护（RUNNING 状态串行化）；lines 已声明 versionProp（交接缺口证伪） | **sustained**（行级缺口证伪；plan RUNNING 串行化） |
| hr A2.7a（5 处） | 员工/合同/调查/发展计划 @Version 降级 | 7 状态机实体全声明 versionProp；状态迁移经 managed update | **sustained** |
| hr A2.7b（5 处） | 工资支付轴并发 + 仿真 convertToFormal 并发 | markPaid versionProp + 状态守卫；convertToFormal per-employee 冲突 skip + all-conflict throw | **sustained** |
| inventory A2.11（4 处） | ErpInvStockBalance versionProp 降级 + applyReservation/reclassifyBalance 无锁 + StockTake 并发 + LandedCost 窗口期 | 前 3 项 sustained（@Version + retry）；LandedCost 窗口期 downgrade watch-only（P1-MA2-085）；**新发现 upsertBalance 自然键无 UK**（P0-MA2-020） | **3 sustained + 1 P1 + 1 P0 新发现** |
| ext A2.14（9 处） | b2b EDI 异步重复回调幂等 + contract 到期 Job + maintenance 工时过账悬挂 + 其他状态变更并发 | b2b EDI webhook TOCTOU（P1-MA2-086）；contract 无 ErpCtContractExpiryJob（P1-MA2-071 已登记）；maintenance posting sustained | **mostly sustained**（b2b EDI + contract 归 P1） |
| aps A2.15（1 处） | 并发排产产能双倍占用 | **未落地**（owner doc §4 声明未落实） | **P0-MA2-019** |
| logistics A2.15（1 处） | 并发更新同一发运单乐观锁 | `advanceTracking` versionProp + 已 DELIVERED 幂等守卫 | **sustained** |
| use-case-implementation-audit（3 处） | UC-INV-08 超卖 + UC-SAL-10 双重核销 + UC-SAL-10 并发扣批次 | 全部经证据证伪（@Version + retry / managed flush 防护成立） | **3 sustained（全部证伪）** |

**交接裁决汇总**：约 50 处 MA2 交接并发敏感点经逐项裁决——**绝大多数 sustained**（@Version 透明乐观锁在临界 read-modify-write 路径生效），3 项升级为 P0（P0-MA2-018/019/020——幂等键无 DB UK / 排产产能无锁 / 余额自然键无 UK），若干项降级为 P1 watch-only。

## 11. P0 即时通道处理

### P0-MA2-018 — finance 过账幂等键无 DB 唯一约束

- **报告**：本报告 §5
- **描述**：`erp_fin_voucher_bill_r(billCode, businessType)` **无 DB 唯一约束**；`ErpFinPostingProcessor.alreadyPosted:472` 是 TOCTOU pre-check query；`IErpFinVoucherBiz.post` `@Transactional(REQUIRES_NEW)` 独立事务隔离 → 并发 `post()` / 兜底重试 / 人工重试可同时通过 pre-check 双 INSERT 重复凭证 + billR。破坏业财幂等键不变量（同一单据同一业务类型应仅一张凭证）。触发面广：任何过账失败后（配置错误/基础设施故障）被 `ErpFinDeferredPostingRetryHelper` 兜底重试 + 人工重试同时介入即触发。
- **影响**：GL 重复入账，借贷双计，破坏财务报表正确性。
- **修复路径**：方案 A（推荐）`module-finance/model/app-erp-finance.orm.xml` 给 `ErpFinVoucherBillR` 加 `<key name="UK_FIN_VOUCHER_BILL_R_BILL" unique="true">` on `(billCode, businessType)`（或 `(billHeadCode, businessType)` 对齐 alreadyPosted query 字段）+ 数据 cleanup（若现存重复）+ `alreadyPosted` pre-check 保留为友好错误提示 + OptimisticLock/ConstraintViolation 兜底翻译为 `ERR_FIN_VOUCHER_ALREADY_POSTED`；方案 B 改 `alreadyPosted` 为 SELECT FOR UPDATE 悲观锁（不推荐——REQUIRES_NEW 下死锁风险）。
- **保护区域**：触及 finance 凭证保护区域 + ORM ask-first（唯一约束变更）→ 须独立 plan-audit + 人工确认。
- **修复 plan**：`docs/plans/2026-07-28-1249-arm-fix-p0-ma2-018-voucher-bill-r-uk.md`（异步注入，Status: planned）
- **修复状态**：`planned (plan 2026-07-28-1249-arm-fix-p0-ma2-018)`

### P0-MA2-019 — aps 排产产能并发双倍占用（owner doc §4 锁未落地）

- **报告**：本报告 §4
- **描述**：owner doc `docs/design/aps/state-machine.md §4` 显式声明"乐观锁/资源锁"防护并发排产产能双倍占用，**实现未落地**——`ErpApsSchedulingProcessor.run:126-141` + `persist:194-200` 无锁、无产能预留实体、无 `(workcenterId, plannedStartT, plannedEndT)` UK；`ErpApsErrors.ERR_APS_CAPACITY_CONFLICT` 仅引擎内存检查非 DB enforced。两个并发 `scheduleForward` 在共享工作中心的不同 schedule 上，各自读取同一组 PLANNED orders 作为 frozen baseline，各自调度 DRAFT orders 进同一时隙 → **产能双倍占用**。`OperationOrder` `versionProp` 仅保护同实体并发更新，不保护跨实体的产能聚合不变量。
- **影响**：生产计划产能预留穿透，车间过载，交付承诺破坏。
- **修复路径**：方案 A（推荐）新增 `ErpApsCapacityReservation` 实体（workcenterId + plannedStartT + plannedEndT + orderId + versionProp + UK `(workcenterId, plannedStartT, plannedEndT)`）承载产能预留，排产引擎写入前校验重叠；方案 B 引入 `IErpSysLockBiz` 分布式锁按 workcenterId（须先实现 `IErpSysLockBiz` SPI——本仓库不存在）；方案 C 序列化排产到单 scheduler bean（性能损失大）。owner doc §4 须同步实际落地机制。
- **保护区域**：触及 aps 排产引擎 + ORM ask-first（新实体/UK）→ 须独立 plan-audit + 人工确认。
- **修复 plan**：`docs/plans/2026-07-28-1249-arm-fix-p0-ma2-019-aps-capacity-lock.md`（异步注入，Status: planned）
- **修复状态**：`planned (plan 2026-07-28-1249-arm-fix-p0-ma2-019)`

### P0-MA2-020 — inventory 库存余额自然键无唯一约束（首次 INSERT 竞态）

- **报告**：本报告 §4
- **描述**：`erp_inv_stock_balance` **无自然键 UK** on `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)`（仅非唯一 `IDX_INV_STOCK_BALANCE_*`）；`StockMoveBookkeeper.upsertBalance:137-170` + `ErpInvOwnershipTransferProcessor.upsertTargetBalance:211-237` 实现 check-then-insert（`findBalance==null` → INSERT）。并发首次移动单针对同一新维度（如新品上架时多个并发收货单）→ 两事务都 `findBalance==null` → 都 INSERT → **重复余额行**。后续 `findAllByQuery(...).get(0)` 读到任一行 → **silent split-quantity corruption**（数量/金额被分散到两行，totalQuantity/totalCost 漂移，破坏余额守恒不变量）。@Version 不保护 INSERT 路径。
- **影响**：库存余额分裂，三方对账失败，成本核算基于错误余额。silent corruption（无异常抛出）。
- **修复路径**：方案 A（推荐）`module-inventory/model/app-erp-inventory.orm.xml` 给 `ErpInvStockBalance` 加 `<key name="UK_INV_STOCK_BALANCE_NATURAL" unique="true">` on `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)` + 数据 cleanup（若现存重复）+ `upsertBalance` 捕获 ConstraintViolation → reload + retry（复用 `tryUpdateWithVersionCheck` retry 模式）；方案 B 改 `findBalance` 为 SELECT FOR UPDATE（不推荐——余额表高并发写，死锁风险高）。
- **保护区域**：触及 inventory 库存余额保护区域 + ORM ask-first（UK 变更）→ 须独立 plan-audit + 人工确认。
- **修复 plan**：`docs/plans/2026-07-28-1249-arm-fix-p0-ma2-020-inv-stock-balance-uk.md`（异步注入，Status: planned）
- **修复状态**：`planned (plan 2026-07-28-1249-arm-fix-p0-ma2-020)`

## 12. P1 汇总（新登记，目标 MR1）

| Finding ID | 域 | 描述 | 目标 MR | 修复状态 |
|-----------|----|------|--------|---------|
| `P1-MA2-085` | inventory | LandedCost `ErpInvLandedCostProcessor.validateNotAlreadyAllocated:90` TOCTOU pre-check + `erp_inv_landed_cost` 仅 `IDX_INV_LANDED_COST_RECEIVE_ID`（非唯一）→ 并发 approve 同 receiveId 的两 LandedCost 双 allocation cost 进 StockBalance。下游 cost 经 managed-instance 有 version 守护但双 allocation 已落地。修复方式：MR1 加 `(receiveId, approveStatus)` UK 或 SELECT FOR UPDATE pre-check | MR1 | todo |
| `P1-MA2-086` | cs+crm+hr+qa+mnt+mfg | 定时任务并发重复副作用合并裁决：10 个 cron job 运行于 `nop-job-local`（非分布式）+ 无 `IErpSysLockBiz`，并发执行产生重复通知/审计行/实体——`erp-cs-sla-scan`（每分钟重复 ESCALATE，最严重，单实例即触发）/ `erp-cs-entitlement-expiry` / `erp-cs-csat-reminder` / `erp-crm-event-reminder` / `erp-crm-sequence-overdue` / `erp-hr-contract-expiry`（通知重复）/ `erp-qa-spc-sampling`（append sample 重复）/ `erp-mnt-due-visit-generation`（重复 visit 行）/ `erp-mfg-jobcard-auto-generate`。全部默认 enabled=false（参考部署 silent）。修复方式：MR1 裁决——方案 A（推荐）迁移 `nop-job-service` + `nop_sys_cluster_leader` 单实例锁；方案 B 实现 `IErpSysLockBiz` per-job 锁；方案 C 各 job body 补幂等（escalation dedup flag / existence check before insert / notify dedup key） | MR1 | todo |
| `P1-MA2-087` | finance | `CloseVoucherWriter.writeVoucher:151`（FX 重估 / 损益结转 / 年结凭证写入）无幂等 pre-check + `erp_fin_voucher_bill_r` 无 `(billHeadCode, businessType)` UK（与 P0-MA2-018 同根因）。bounded by period version guard（同期间并发 close 经 version 守护），但单期间并发 close 重试时 REQUIRES_NEW 副作用经 alreadyPosted TOCTOU 可创建重复 close 凭证。修复方式：MR1 与 P0-MA2-018 一并裁决（加 UK 后 CloseVoucherWriter 自动受保护） | MR1 | todo |
| `P1-MA2-088` | b2b | `ErpB2bAsnBizModel.handleInboundWebhook:89` 重复回调幂等：`isDuplicateEvent:439` query `remark="WEBHOOK eventId=..."` TOCTOU + eventId 存 remark 非 UK 列 + ASN code = `"ASN-"+currentTimeMillis()`（每次不同）→ 并发/重复回调创建重复 ASN + EdiDoc。config-gated `erp-b2b.b2b-enabled` 默认 OFF。修复方式：MR1 加 `ErpB2bEdiDoc` `(sourceType, sourceEventId)` UK 或 ASN code 改为基于 eventId 的确定性派生 | MR1 | todo |
| `P1-MA2-089` | assets | `ErpAstDepreciationScheduleProcessor.executeDepreciation:52` 缺 `status==PENDING` 守卫 → 并发首次折旧执行两事务都观察 `schedule==null`（或 PENDING）都 `newEntity+saveOrUpdateEntity`（INSERT 无 version 校验）→ 重复 `ErpAstDepreciationSchedule` 行 + `ErpAstAsset.setAccumulatedDepreciation` 双计。postingDispatcher.tryPost 幂等（billHeadCode 去重）。修复方式：MR1 增 `(assetId, periodId)` UK 或 `requireSchedulePending` 守卫 + 已存在则 reverse+reexec 路径 | MR1 | todo |
| `P1-MA2-090` | mfg | `MrpReleaseService.releaseToSubcontractOrder:115-133` 并发释放同一 plan line：`requireReleasable`（isFirmed==false）TOCTOU + 生成的 `ErpMfgSubcontractOrder code="SUB-MRP-"+lineId`（UK `(code,orgId)` 兜底，重复释放抛约束违例但异常丑陋）。修复方式：MR1 加 `(mrpPlanLineId)` UK 或显式 isFirmed 守卫+version check 友好错误 | MR1 | todo |
| `P1-MA2-091` | hr | `ErpHrShiftAssignmentBizModel.assignSingle:60-67` + `assertNoExistingAssignment:153` TOCTOU pre-check + `erp_hr_shift_assignment` **无 `(employeeId, assignmentDate)` UK** → 并发 assignSingle 同员工同日期双 INSERT 重复排班。`assignBatch` / `copyFromPeriod` skip-if-exists 同型 TOCTOU 但幂等。修复方式：MR1 加 `(employeeId, assignmentDate, shiftId)` UK | MR1 | todo |
| `P1-MA2-092` | logistics | `erp_log_shipment` **无 `trackingNo` UK**（仅 `(code,orgId)`）→ 网关回调 + 手工创建并发可创建重复 trackingNo shipment。状态轴 sustained（versionProp + advanceTracking 守卫）。修复方式：MR1 加 `(trackingNo, carrierId)` UK | MR1 | todo |

### P2 watch-only（本审计新发现）

| Finding ID | 描述 |
|-----------|------|
| `P2-MA2-074` | 全域无悲观锁（`FOR UPDATE`/`withLock`=0）——并发争用高场景可能需要悲观锁优化，但当前 @Version + retry 模式正确。watch-only，归 MA7 性能审查 |
| `P2-MA2-075` | `ErpFinDeferredPostingRetryHelper.incrementRetryAndRethrow:133` 复用 stale `ex` 实例，并发冲突时第二次 version 异常被吞 → retry-count 自增在并发下可能丢失（不影响终态，仅统计漂移）。watch-only |

## 13. 控制点 PASS/FAIL 汇总

| 控制点 | 裁决 | 备注 |
|--------|------|------|
| @Version 覆盖矩阵（核心） | **PASS** | 全 19 域 336 自有实体 100% 声明 versionProp |
| 透明乐观锁降级（6 处候选） | **PASS** | 全部证伪——业务方法正确持有托管实体使 @Version 生效 |
| UC-INV-08 超卖缺口 | **PASS**（证伪） | `tryUpdateWithVersionCheck` + retry 防护 |
| UC-SAL-10 双重核销缺口 | **PASS**（证伪） | managed flush + version 校验 |
| UC-SAL-10 并发扣批次缺口 | **PASS**（证伪） | managed flush + version 校验 |
| 期间重复结账缺口 | **PASS**（证伪） | period versionProp + assertPeriodStatus 守卫 |
| 承付 commit/release 竞态 | **PASS**（预算控制）/ 部分（commit 凭证幂等经 P0-MA2-018） | aggregate-on-read 无 lost-update |
| 排产产能双倍占用 | **FAIL** | P0-MA2-019 |
| 过账 REQUIRES_NEW 并发重试幂等 | **FAIL** | P0-MA2-018 |
| 库存余额首次 INSERT 竞态 | **FAIL** | P0-MA2-020 |
| REQUIRES_NEW 跨域失败隔离一致性 | **PASS** | 11 域 PostingExecutor/Dispatcher 一致 |
| LandedCost 同 receiveId 窗口期 | **PARTIAL** | P1-MA2-085 |
| 多轴状态机并发翻转竞争（8/9 Processor） | **PASS** | 显式源态守卫 + versionProp；executeDepreciation 例外（P1-MA2-089） |
| 定时任务并发（10/19 job） | **PARTIAL FAIL** | P1-MA2-086（默认 enabled=false） |
| 与设计文档一致性（flow §6/§八） | **PARTIAL FAIL** | §八.3 幂等声明经 P0-MA2-018；aps §4 锁未落地经 P0-MA2-019 |

## 14. lost-update P0 候选项证伪或确认

| P0 候选项（plan 列举） | 证据 | 终态 |
|----------------------|------|------|
| 库存扣减无 @Version + 无悲观锁致并发超卖（UC-INV-08） | StockMoveBookkeeper.updateBalanceWithRetry `tryUpdateWithVersionCheck` + retry；applyReservation managed-instance save | **证伪**（@Version + retry 防护成立） |
| 核销 ErpFinArApItem 透明乐观锁降级致并发双重核销（UC-SAL-10） | ReconciliationSettler managed flush + version 校验 | **证伪**（降级证伪） |
| 期间结账无并发锁致重复结账 | closePeriod managed flush + versionProp + assertPeriodStatus | **证伪**（version 守护成立） |
| 排产产能双倍占用乐观锁未落地（A2.15 owner doc §4） | ErpApsSchedulingProcessor 无锁、无 UK、无预留实体 | **确认 P0-MA2-019** |
| 过账 REQUIRES_NEW 并发重试非幂等致重复凭证 | erp_fin_voucher_bill_r 无 UK + alreadyPosted TOCTOU | **确认 P0-MA2-018** |
| 承付并发 commit/release 竞态致预算余量穿透 | 预算余量 aggregate-on-read 无 lost-update；commit 凭证幂等经 P0-MA2-018 | **部分证伪**（预算控制 sustained；commit 幂等经 P0-MA2-018 修复） |
| **新发现**：库存余额首次 INSERT 竞态 | erp_inv_stock_balance 无自然键 UK + check-then-insert | **确认 P0-MA2-020** |

## 15. 并发测试覆盖缺口（交接 MA5）

本审计做并发**正确性**审查，不系统性审查并发测试覆盖。但标注以下缺口交接 MA5：

- 无任何并发场景集成测试（双线程并发 approve/close/settle 验证 OptimisticLockException 抛出与回滚）
- 无 `tryUpdateWithVersionCheck` retry 路径测试（inventory 已声明 `erp-inv.concurrent-deduct-max-retry=5` 但无测试覆盖 retry 触发与上限）
- 无幂等键重复 INSERT 负向测试（同 billHeadCode 并发 post 应抛约束违例——P0-MA2-018 修复后须补）
- 无 cron job 并发执行测试（P1-MA2-086 修复后须补）

归 MA5（`audit-remediation-scope-and-dimension-matrix.md §2.4 测试覆盖深度 MA5`）系统性审查。

## 16. 残留风险与 successor

- **P0-MA2-018/019/020 修复落地前**：3 项并发缺陷处于 open 状态，参考部署 silent（b2b/部分 job 默认 OFF），但生产环境启用相关功能后即暴露。须 fix plan 闭包后 closure audit 复核。
- **多账套/多公司隔离（A2.18）**：本审计标注并发场景下的 orgId 隔离交叉点——`erp_inv_stock_balance` 自然键建议 UK 包含 orgId（P0-MA2-020 修复方案已含），`erp_fin_voucher_bill_r` UK 应包含 orgId（P0-MA2-018 修复方案须确认）。系统性多公司隔离正确性归 A2.18。
- **并发测试覆盖（MA5）**：见 §15。
- **并发性能（MA7 N+1）**：本审计不审查锁争用性能——高并发场景下 @Version retry 风暴的 N+1/锁争用归 MA7。

## 17. 结论

并发正确性主路径经 @Version 透明乐观锁 + managed-instance read-modify-write + REQUIRES_NEW 跨域失败隔离 + 兜底重试 + 对账机制 **整体成立**——MA2 交接的 40+ 并发敏感点绝大多数 sustained，6 项 MA2/use-case 候选 P0 经证据全部证伪或部分证伪。但发现 **3 项 P0 缺 DB 唯一约束 / 显式锁落地**破坏幂等与余额/产能不变量（P0-MA2-018/019/020），均触及 ask-first ORM 保护区域，已异步注入 3 个独立 fix plan，须经独立 plan-audit + 人工确认后闭包。**并发维度终态：全域 ⚠️(P0→fix-plan + P1)**。
