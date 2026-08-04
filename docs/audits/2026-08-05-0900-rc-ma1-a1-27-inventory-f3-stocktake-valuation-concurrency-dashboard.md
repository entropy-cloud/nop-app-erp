# rc-ma1-a1-27 inventory-F3 盘点/估值/并发/看板 需求-实现符合性五级追踪审计

> 报告类型：MA1(RC) 需求-实现符合性五级追踪审计（只读审计，无代码/ORM/api.xml/真相源变更）
> Mission: requirement-compliance
> Work Item: A1.27（inventory-F3 盘点/估值/并发/看板，UC-INV-07/08/10/11，4 UC + 8 候选缺口）
> 切片基线：`docs/audits/rc-requirement-baseline-inventory.md` A1.27 UC 锚点（`✅ 一致`，UC-INV-07/08/10/11 4 UC）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> L1 真相源：`docs/design/inventory/use-cases.md`（UC-INV-07/08/10/11）
> L2 设计参考：`state-machine.md` §盘点单状态机/§4 异常路径/§7 + `cross-domain.md` §与财务协作 + `dashboards.md` §3 库存看板
> L5 既有证据复用：A2.11（`2026-07-28-0400-arm-ma2-inventory-state-machine.md`）/ A2.17（`2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`）/ A4.5（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`）/ A2.18+A6.3（`...-multi-company-isolation.md` + `...-data-permission-runtime.md`，P1-MA2-093 resolved R1.29）

## 整体裁决

**4 UC 结论：UC-INV-07 = P1（reuse P1-MA2-062，§4 三判据复核倾向重开，须人工确认 product-scope）；UC-INV-08 = 接受；UC-INV-10 = 接受（含 caveat：命名漂移 reuse P2-RC-011/016 watch-only + 时序漂移行为等价 + 行级凭证断言缺失 new P2-RC-029）；UC-INV-11 = 接受 on ⑩KPI实时聚合，⑫行级权限 reuse P1-MA2-093（resolved R1.29）+ 存疑点交 MA4，⑬缺料阈值 new P2-RC-030 watch-only。** **零 P0**（无活跃数据破坏 / 会计过账破坏 / 安全漏洞 / 核心循环断裂），**0 项新 P1**（#1 UC-INV-07 reuse P1-MA2-062 不新建编号），**2 项新 P2**（P2-RC-029 UC-INV-10 行级凭证断言缺失 / P2-RC-030 UC-INV-11 缺料阈值 config 化边界），**3 项 reuse**（P1-MA2-062 / P1-MA2-093 / P2-RC-011+P2-RC-016）。

UC-INV-08（并发扣减乐观锁）+ UC-INV-10（移动单触发存货估值凭证）主路径**完整实现需求契约**：UC-INV-08 乐观锁 + UK 兜底 + 重试 + 负库存语义齐全（`StockMoveBookkeeper.updateBalanceWithRetry:255-328` + `tryUpdateWithVersionCheck` + `UK_INV_STOCK_BALANCE_NATURAL` + max-retry + `CONFIG_ALLOW_NEGATIVE_STOCK`），经 A2.11/A2.17 双重证实；UC-INV-10 过账链路 `doComplete:113 → dispatchIfApplicable:57 → createFacts:64-88` 完整（借贷方向 + 金额公式 = `billData.TOTAL_COST`），失败隔离 + DeferredPostingSweepJob 兜底成立。本切片只补需求视角增量差异（UC-INV-10 命名漂移 STOCK_MOVE→PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT reuse P2-RC-011/016 + 时序漂移同步 doComplete vs L1 post-commit 异步经 REQUIRES_NEW 隔离行为等价 + JUnit 行级断言弱 new P2-RC-029）。

UC-INV-07 盘点差异生成移动单 **stub**（`ErpInvStockTakeBizModel.completeTake:40-50` 仅 `setDocStatus(DONE)`，无 `actualQuantity vs totalQuantity` 比对、无 `IErpInvStockMoveBiz.generateMove` 调用、无盘盈/盘亏移动单生成；零 dedicated 测试），即既有 finding **P1-MA2-062**（`arm-index.md:328`，resolved R1.19 方案 B owner doc Deferred）。**§4 三判据复核**（遵循 A1.24 P1-MA2-061 同型先例）：(i) R1.19 plan 含独立 AI 子代理草案审查记录（`ses_05030be58ffe...` accept）但 methodology §4 line 168 明确「代理独立审计 ≠ 人工批准」，且 R1.19 `Closure Audit Evidence` 自承「执行代理（本会话）；独立结束审计为后续独立会话步骤」= 执行者自审的 hollow closure（`project-context.md §已知失败模式`）；(ii) owner doc `state-machine.md:159` Deferred 标注无人工批准痕迹（git log 全 AI commits）；(iii) `product-scope.md:16` 仅列「库存...盘点」为域能力，未将「盘点自动生成移动单」裁剪出范围 → **三判据在「人工批准」意义上不满足** → 按 Q4=(a) **倾向重开 P1 入 MR1**，须人工确认 product-scope（若裁剪→§4(iii) 改真相源非降级；若未裁剪→P1 强制实现 `completeTake` 自动差异移动单生成）。按 §7 同根因同控制点（completeTake stub = P1-MA2-062 字面描述）→ **reuse P1-MA2-062** + §4 复核注记，**不新建 P1-RC-xxx**。

UC-INV-11 库存看板 KPI 实时聚合（`ErpInvDashboardBizModel` `@BizQuery` 实时 DB SUM/聚合，非硬编码）**接受**；行级权限（L1 `:207`）QueryBean 无显式 orgId/createdById filter，但跨域 **P1-MA2-093 resolved R1.29**（全局 `ErpOrgIsolationQueryTransformer` 注入查询管道层强制 orgId scope）覆盖——遵循 sibling 先例 A1.7 UC-FIN-17 SP-4 / A1.11 UC-MFG-11 SP-3 / A1.21 UC-SAL-12 SP-3 / A1.24 UC-AST-12③ **reuse P1-MA2-093 + 登记运行时覆盖存疑点交 MA4**（dashboard 直连路径 `daoProvider.findAllByQuery`/`ormTemplate.findListByQuery` 运行时覆盖有效性），**不新建 P1-RC-xxx**；⑬缺料预警阈值取 `material.safetyStock`（物料级 master-data）非 NopSysVariable config key——L2 `dashboards.md §3:95` 字面「物料级配置」与实现一致，L1 `:204`「阈值来自系统配置,非硬编码」 operative 约束「非硬编码」满足（safetyStock 为 DB 字段非代码常量），仅 literal「系统配置」维度边界弱 → new **P2-RC-030** watch-only。

---

## 1. 需求契约原文（L1 逐字引用）

> 来源：`docs/design/inventory/use-cases.md`（L1 权威功能契约，§4 Q1 真相源层级 2）。**禁止转述**——验收标准逐字引用。

### UC-INV-07 盘点差异生成移动单（`use-cases.md:122`）

```
场景:盘点产生差异,不直接改余额,而是生成盘盈/盘亏移动单走标准流程。

可验证断言(见 state-machine.md §盘点单状态机):
  盘点单.确认 →
    计算差异: 差异 = 实盘数量 - 账面数量
    若 差异 > 0: 生成盘盈移动单(incoming)
    若 差异 < 0: 生成盘亏移动单(outgoing)
    盘点单本身不改余额
  盘盈/盘亏移动单走 DRAFT→DONE 后才影响余额
```

### UC-INV-08 并发扣减乐观锁（`use-cases.md:140`）

```
场景:多个出库移动单并发扣减同一批次,乐观锁保证不超扣。

可验证断言(见 state-machine.md §4):
  并发移动单A、B 扣同一批次:
    一个 DONE 成功, 另一个乐观锁冲突 → 重试或失败
  最终 批次.现有量 == 初始 - A - B (不出现负数,除非允许负库存)
```

### UC-INV-10 移动单触发存货估值凭证（`use-cases.md:171`）

```
场景:移动单 DONE 后异步生成存货估值凭证(成本来自流水)。

可验证断言(见 cross-domain.md §与财务协作):
  移动单.DONE → 发布事件(post-commit 异步)
  → 生成存货估值凭证(STOCK_MOVE 业务类型)
  凭证.金额 来自 移动单.单位成本 × 数量
  单位成本由库存流水维护(基线约定,见 cross-domain §数据契约)
  入库: 借存货 / 贷暂估应付(GR/IR)
  出库: 借销售成本 / 贷存货
```

### UC-INV-11 库存看板（`use-cases.md:193`）

```
场景:库存看板的指标展示与异常预警。见 ../dashboards.md §库存看板。

可验证断言:
  // KPI 指标数据源正确(实时聚合, 非硬编码)
  KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
    库存总值/周转率, 缺料/滞销/批次效期预警, 仓库分布

  // 预警触发
  预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

  // 权限
  看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

| UC | 代码路径（file:line） | 跨域调用链 |
|----|----------------------|-----------|
| **UC-INV-07** | `module-inventory/erp-inv-service/.../entity/ErpInvStockTakeBizModel.java:40-50 completeTake`（**STUB**：`requireEntity` + 源态守卫 CONFIRMED + `take.setDocStatus(ErpInvConstants.DOC_STATUS_DONE)` + `updateEntity`——**无任何** `ErpInvStockTakeLine.actualQuantity`/`bookQuantity`/`differenceQuantity` 比对、**无** `IErpInvStockMoveBiz.generateMove` 调用、**无** 盘盈/盘亏移动单生成）+ `ErpInvStockTakeLineBizModel.java`（11 行 CRUD 桩） | ⚠️ 应调用而未调用：`IErpInvStockMoveBiz.generateMove`（Facade 存在于 `ErpInvStockMoveBizModel`，UC-INV-01/03 已证可用）|
| **UC-INV-08** | `module-inventory/erp-inv-service/.../stock/StockMoveBookkeeper.java:255-328 updateBalanceWithRetry`（MANAGED 路径 `dao.tryUpdateWithVersionCheck:271` 乐观锁 + SAVING/TRANSIENT 路径 INSERT-UK-conflict 重试 `:272-282/316-326`；重试上限 `CONFIG_CONCURRENT_DEDUCT_MAX_RETRY` 默认 5 `ErpInvConstants.CONCURRENT_DEDUCT_MAX_RETRY_DEFAULT`；耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`/`ERR_INV_BALANCE_INSERT_CONFLICT` `:290-297`）+ `ErpInvStockBalance.version`（ORM `versionProp`）+ `UK_INV_STOCK_BALANCE_NATURAL`（P0-MA2-020 done 方案 A）+ `ErpInvConcurrencyMetrics`（`recordOptimisticLockFailure`/`recordOptimisticLockFailureExhausted`）+ `ErpInvStockMoveProcessor.validateAvailable:116-136`（出库前可用量校验 + `CONFIG_ALLOW_NEGATIVE_STOCK` 默认 false 放行门控 `:285-288`） | 同域（StockMoveBookkeeper → CostingStrategy → ErpInvStockBalance/Ledger） |
| **UC-INV-10** | `ErpInvStockMoveProcessor.java:100-114 doComplete`（`releaseReservation` + `bookkeeper.bookCompletion:110` + `setDocStatus(DONE):111` + `postingDispatcher.dispatchIfApplicable(move,lines):113`）+ `posting/InvPostingDispatcher.java:57-80 dispatchIfApplicable`（`resolveBusinessType:152-179` moveType→PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT 映射 + skip internal-transfer/pur-return/sal-return/mnt-spare/mfg-issue）+ `posting/InvPostingExecutor.java:29-35 postEvent`（跨域 Facade）+ `posting/InvAcctDocProvider.java:64-88 createFacts`（PURCHASE_INPUT 借1401库存/贷2202暂估应付 `:70-74`；MANUFACTURING_RECEIPT 借1401/贷1411 WIP `:75-80`；SALES_OUTPUT 借6401主营业务成本/贷1401库存 `:81-86`；金额=`readTotalCost`=`billData.TOTAL_COST` `:113-122`） | inventory→finance 经 `InvPostingExecutor.postEvent`→`IErpFinVoucherBiz.post`（REQUIRES_NEW，跨域 Facade）；失败隔离 try/catch `InvPostingDispatcher:64-76` 留 `posted=false`（DeferredPostingSweepJob 兜底） |
| **UC-INV-11** | `module-inventory/erp-inv-service/.../dashboard/ErpInvDashboardBizModel.java`（`@BizQuery` 实时聚合非硬编码）：`getDashboardKpi:69-100`（totalValue 经 `sumBalanceTotalCost:444-456` DB SUM；出入库量 `sumMoveQtyInRange:296-322`；`turnoverRate = outgoingCost/avgInventory:84-89`）、`getDashboardTrend:102-129`、`findWarehouseDistribution:132-164`（DB GROUP BY warehouseId + SUM(totalCost)）、`findShortageAlert:167-198`（阈值取 master-data `material.safetyStock` `loadSafetyStock:370-380`——边界，见 P2-RC-030）、`findSlowMovingAlert:203-245`（config `CONFIG_DASH_INV_SLOW_MOVING_DAYS`）、`findBatchExpiryAlert:250-291`（config `CONFIG_DASH_INV_BATCH_EXPIRY_DAYS`）。**行级权限**：所有 dashboard QueryBean **无显式 orgId/createdById/assigneeId filter**（`sumBalanceTotalCost`/`loadDoneMovesInRange:345-352`/`findAllByQuery` 无内联 scope） | 跨域：dashboard 经 `IDaoProvider`/`IOrmTemplate` 直访（P1-MA2-093 全局 `ErpOrgIsolationQueryTransformer` 覆盖，运行时有效性 = MA4 存疑点） |

**L3 字段存在性核实**（UC-INV-07 修复预授权依据）：`ErpInvStockTakeLine` 实际字段为 `bookQuantity`（账面）/`actualQuantity`（实盘）/`differenceQuantity`（差异，已派生列）——本计划 Current Baseline 称「StockTakeLine.qtyActual」为命名略称，权威名为 `actualQuantity`（`_ErpInvStockTakeLine.java:61`），字段均已存在，修复属代码逻辑类（复用既有 Facade + 既有字段，不涉 ORM 结构变更）。

---

## 3. 测试证据（L4 测试断言，注明断言强度）

| UC | 测试引用 | 断言强度 |
|----|---------|---------|
| **UC-INV-07** | ⚠️ **零 dedicated 测试**——全 `module-inventory/erp-inv-service/src/test/` 无 `TestErpInvStockTake*` 文件；grep `StockTake\|completeTake` 全 `module-inventory/erp-inv-service/src/test/` 零命中 | **完全缺失**（§2 P1⑤） |
| **UC-INV-08** | `TestErpInvConcurrentDeduct.java`（6 @Test）：`testConcurrentDeductRetrySucceeds:87-124`（version-skew 重试成功 + 落盘 total=3 + version≥2）/ `testConcurrentDeductRetryExhaustedThrows:131-172`（max-retry=0 抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT` + 落盘 total=7）/ `testConcurrentDeductNoOversell:179-182`（3 线程 ExecutorService+CountDownLatch 防超卖 + 落盘 total=1 + 出库流水计数=3）/ `testConcurrentDeductWithNegativeStockAllowed:189-192`（负库存并发 total=-2）/ `testConcurrentFirstMoveSameDimensionThrowsAndRetries:208-239`（UK 冲突单线程重试 + 行数=1 + total=9）/ `testConcurrentFirstMoveMultiThreadNoDuplicateRows:251-306`（多线程无重复行 + total=10） | **强**（含 ledger 行计数 + 落盘 DB 验证 + 多线程栅栏） |
| **UC-INV-10** | `TestErpInvPosting.java`（3 @Test）：`testMoveDoneGeneratesVoucherAndPosted:65-83`（DONE + posted=true + 业财回链 + voucher docStatus=POSTED + totalDebit/Credit==50 + countLines==2）/ `testInternalTransferNoPosting:86-104`（内部调拨 posted=false + 零回链）/ `testPostingFailureLeavesMoveDonePostedFalse:107-115`（失败留 DONE + posted=false + 零回链）+ E2E `tests/e2e/dashboards/inventory.value.spec.ts`（断言 `{totalValue,incomingQty,outgoingQty,turnoverRate}` 非硬编码值） | **强但行级弱**：断言合计+计数，**不断言行级 subjectCode(1401/2202/6401)/dcDirection/amount 精确值**（new P2-RC-029）；3 路径全覆盖（DONE/跳过/失败）|
| **UC-INV-11** | `TestErpInvDashboard.java`（8 @Test）：`testKpiEmptyDatasetReturnsZeros:55-61` / `testKpiTotalValueAndTurnover:64-82`（totalValue==1000 + incomingQty==50 + outgoingQty==10 + turnoverRate==0.2000——**实时聚合非硬编码强断言**）/ `testWarehouseDistribution:85-99` / `testShortageAlert:102-114`（30<50 触发 + 60>50 不触发）/ `testSlowMovingAlertDisabledByDefault:117-127` + `testSlowMovingAlertTriggersWhenNoOutgoing:130-146`（config 门控双路径）/ `testBatchExpiryAlertDisabledByDefault:149-159` + `testBatchExpiryAlertTriggers:162-180`（config 门控双路径） | **强**（config 门控双路径 + 非硬编码值断言）；⚠️ **零多组织行级权限运行时测试**（交 MA4 存疑点） |

---

## 4. 运行时行为证据（L5）

| UC | 运行时行为证据来源 |
|----|------------------|
| **UC-INV-07** | **行为已证实缺失**（A2.11 `2026-07-28-0400-arm-ma2-inventory-state-machine.md` 登记 P1-MA2-062：completeTake 仅置 DONE 无 generateMove）。静态存疑点：completeTake 置 DONE 后库管员手工 `generateMove` 的实际余额影响（手工入口仍走移动单状态机，余额可追溯——A2.11 已证）|
| **UC-INV-08** | **行为已证实 PASS**（A2.11 移动单状态机 + A2.17 `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md:100/:230/:260-303` inventory 并发扣减 sustained：versionProp 乐观锁 + UK 兜底 + retry + max-retry 耗尽抛错）。本切片复用，不重复验证（§去重协议）|
| **UC-INV-10** | **行为已证实 PASS**（A4.5 `2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md` `ErpInvStockMoveProcessor` 代码质量 PASS + A2.11 存货过账事件解耦 + DeferredPostingSweepJob 兜底 + 异常工作台）。静态存疑点：UC-INV-10 posting 失败留 posted=false 时 DeferredPostingSweepJob 兜底实际触发频率（属 P1-MA4-001 family，本切片不重复登记）|
| **UC-INV-11** | **行为部分已证实**：KPI 实时聚合（`TestErpInvDashboard` 强覆盖）+ E2E `inventory.value.spec.ts` 强断言。**行级权限运行时未证实**：全局 `ErpOrgIsolationQueryTransformer`（P1-MA2-093 resolved R1.29）对 dashboard 直连路径（`daoProvider.findAllByQuery`/`ormTemplate.findListByQuery`）的运行时覆盖有效性 = **MA4 存疑点**（遵循 sibling 先例 A1.7/A1.11/A1.21/A1.24，复用 P1-MA2-093 不新建）|

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论，§2 判据）

### 五级追踪矩阵

| UC | L1 use-case 需求契约 | L2 owner doc 契约（设计参考，冲突以 L1 为准） | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------------------|------------|------------|--------------|-----------|
| **UC-INV-07** | `use-cases.md:122` 盘点差异生成移动单（①确认→计算差异(实盘−账面) ②差异>0生成盘盈incoming ③差异<0生成盘亏outgoing ④盘点单本身不改余额 ⑤盘盈/盘亏移动单走DRAFT→DONE后才影响余额） | `state-machine.md §盘点单状态机:147-162`（**Deferred**：L159「差异调整移动单的自动生成=Deferred，当前经库管员手工 generateMove 处置」——**owner doc 已向实现妥协，以 L1 为准**） | `ErpInvStockTakeBizModel.java:40-50 completeTake`（**STUB**：仅 setDocStatus(DONE)，断言①-⑤全未实现）；`ErpInvStockTakeLine` 字段 `actualQuantity`/`bookQuantity`/`differenceQuantity` 已存在但 completeTake 不消费 | **零 dedicated 测试**（无 TestErpInvStockTake*） | 行为已证实缺失（A2.11 P1-MA2-062） | **P1**（reuse **P1-MA2-062**，§4 三判据复核倾向重开，须人工确认 product-scope） |
| **UC-INV-08** | `use-cases.md:140` 并发扣减乐观锁（①并发A、B扣同一批次一个DONE成功另一个乐观锁冲突→重试或失败 ②最终现有量==初始−A−B[不出现负数,除非允许负库存]） | `state-machine.md §4:66-67`（乐观锁+扣减失败重试+UK兜底+`erp-inv.concurrent-deduct-max-retry`）+ `cross-domain.md §余量校验:71-79` | `StockMoveBookkeeper.updateBalanceWithRetry:255-328` + `tryUpdateWithVersionCheck:271` + UK 重试 `:272-326` + max-retry `:260-261/293-297`；`ErpInvStockMoveProcessor.validateAvailable:116-136`（负库存 `CONFIG_ALLOW_NEGATIVE_STOCK` 默认 false） | `TestErpInvConcurrentDeduct`（6 @Test 强） | 行为已证实 PASS（A2.11+A2.17） | **接受** |
| **UC-INV-10** | `use-cases.md:171` 移动单触发存货估值凭证（①DONE→发布事件(post-commit异步) ②生成存货估值凭证(STOCK_MOVE业务类型) ③金额=单位成本×数量 ④入库借存货/贷暂估应付(GR/IR) ⑤出库借销售成本/贷存货） | `cross-domain.md §与财务协作:35-59`（DONE 发布事件 + 财务异步订阅 + 单位成本由流水维护）+ `state-machine.md §7:103`（DONE 后发布事件；财务域订阅，失败不影响移动单终态） | `ErpInvStockMoveProcessor.doComplete:113`→`InvPostingDispatcher.dispatchIfApplicable:57-80`→`InvPostingExecutor.postEvent:29-35`→`InvAcctDocProvider.createFacts:64-88`（借贷方向+金额=`billData.TOTAL_COST`） | `TestErpInvPosting`（3 @Test：合计+计数强，行级弱→P2-RC-029）+ E2E inventory.value | 行为已证实 PASS（A4.5+A2.11） | **接受 on ①③④⑤含 caveat**；②命名漂移 reuse **P2-RC-011/P2-RC-016** watch-only；①时序漂移（同步 doComplete + REQUIRES_NEW 隔离）行为等价；行级断言缺失 new **P2-RC-029** |
| **UC-INV-11** | `use-cases.md:193` 库存看板（⑩KPI卡片值==实时聚合(按期间/orgId/权限过滤)非硬编码  ⑪预警项==满足阈值条件的记录(阈值来自系统配置,非硬编码)  ⑫看板数据受行级权限约束) | `dashboards.md §3:86-101`（库存总值/周转率/缺料/滞销/批次效期/仓库分布指标表）+ `§实现约定:236-243`（实时聚合+orgId/部门/成本中心过滤[行级权限自动注入]+阈值放 NopSysVariable）+ `roles-and-permissions.md`（行级权限，设计参考） | `ErpInvDashboardBizModel`（`getDashboardKpi:69-100`/`findShortageAlert:167-198`/`findSlowMovingAlert:203-245`/`findBatchExpiryAlert:250-291`/`findWarehouseDistribution:132-164`，QueryBean 无显式 orgId filter） | `TestErpInvDashboard`（8 @Test 强）；⚠️零行级权限运行时测试 | KPI 实时聚合已证实；行级权限经 P1-MA2-093 R1.29 全局 transformer 覆盖（运行时有效性=MA4 存疑点） | **接受 on ⑩**；⑪缺料阈值取 `material.safetyStock` 非 config → new **P2-RC-030** watch-only（滞销/批次效期 config 化 PASS）；⑫行级权限 reuse **P1-MA2-093**（resolved R1.29）+ 存疑点交 MA4 |

### 候选缺口分级（8 项，逐条裁决）

| # | UC | 缺口 | 分级 | §2 判据 | finding 裁决 |
|---|----|------|------|---------|-------------|
| #1 | UC-INV-07 | completeTake stub（①②③全未实现：无差异计算 + 无 generateMove + 无盘盈/盘亏移动单） | **P1** | §2 P1①（功能完全缺失）+ §2 P1⑤（测试断言完全缺失） | **reuse P1-MA2-062**（§4 三判据复核倾向重开，须人工确认 product-scope；§7 同根因同控制点不新建编号） |
| #2 | UC-INV-07 | 零 dedicated 测试（无 TestErpInvStockTake*） | 并入 #1 | §2 P1⑤ | 并入 reuse P1-MA2-062（功能缺失致测试缺失，同 finding） |
| #3 | UC-INV-08 | 乐观锁+UK+重试+负库存语义复核 | **接受** | 全断言 PASS | —（行为经 A2.11+A2.17 双重证实；max-retry 耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT` 满足 L1「重试或失败」；负库存 `CONFIG_ALLOW_NEGATIVE_STOCK` 默认 false 满足 L1「不出现负数,除非允许」）|
| #4 | UC-INV-10 | 命名漂移（L1 `STOCK_MOVE` vs 实现 `PURCHASE_INPUT`/`SALES_OUTPUT`/`MANUFACTURING_RECEIPT`） | **P2** watch-only | §2 P2①（行为正确，命名漂移） | **reuse P2-RC-011 + P2-RC-016**（§7 同型不同切片投影：PURCHASE_INPUT↔GOODS_RECEIPT + SALES_OUTPUT↔SALES_DELIVERY，inventory 域 dispatcher 投影，不新建） |
| #5 | UC-INV-10 | 时序漂移（L1「post-commit 异步」vs 实现同步在 doComplete 内） | **接受** | 行为等价 | —（同步 doComplete + `IErpFinVoucherBiz.post` REQUIRES_NEW 隔离 + try/catch 失败留 posted=false + DeferredPostingSweepJob 兜底 = 失败隔离成立，与 L1「post-commit 异步」行为等价；移动单终态不阻塞） |
| #6 | UC-INV-10 | JUnit 行级凭证断言缺失（仅合计+计数，不断言行级 subjectCode/dcDirection） | **P2** watch-only | §2 P2①（主路径 L3 实现完整 + E2E 强，JUnit 单测断言强度弱） | **new P2-RC-029** |
| #7 | UC-INV-11 | 行级权限（QueryBean 无显式 orgId filter） | **P1 reuse resolved** | —（P1-MA2-093 resolved R1.29 全局 transformer 覆盖） | **reuse P1-MA2-093**（+ 登记运行时覆盖存疑点交 MA4，遵循 sibling 先例 A1.7/A1.11/A1.21/A1.24，不新建） |
| #8 | UC-INV-11 | 缺料阈值取 `material.safetyStock`（物料级 master-data）非 NopSysVariable config key | **P2** watch-only | §2 P2①（主路径[非硬编码 DB 字段]OK，边界[L1 literal「系统配置」]弱；L2 `dashboards.md §3:95`「物料级配置」与实现一致） | **new P2-RC-030** |

---

## 6. 与 arm-index 衔接（§7 "复用 or 新增"裁决）

### 6.1 裁决表（grep arm-index 同域同控制点后裁决，禁止未经比对新建）

| 候选缺口 | arm-index grep 结果 | 裁决 | 依据 |
|---------|---------------------|------|------|
| **#1/#2 UC-INV-07 completeTake stub** | grep `completeTake\|盘点\|StockTake\|盘盈\|盘亏` arm-index → 命中 **P1-MA2-062**（`:328`，字面描述=StockTake completeTake 未自动生成盘盈/盘亏移动单，resolved R1.19 方案 B） | **reuse P1-MA2-062** | §7 同根因（completeTake stub）同控制点（completeTake:40-50）= P1-MA2-062 字面描述本身 → 复用不新建。追加 RC 视角 §4 三判据复核注记 |
| **#4 UC-INV-10 命名漂移** | grep `GOODS_RECEIPT\|PURCHASE_INPUT\|SALES_OUTPUT\|SALES_DELIVERY\|businessType.*命名\|命名漂移` arm-index → 命中 **P2-RC-011**（A1.15 GOODS_RECEIPT↔PURCHASE_INPUT）+ **P2-RC-016**（A1.18 SALES_DELIVERY↔SALES_OUTPUT） | **reuse P2-RC-011 + P2-RC-016** | §7 同型不同切片投影（inventory 域 InvPostingDispatcher 是 PURCHASE_INPUT/SALES_OUTPUT 的产生源），L1 UC-INV-10 `STOCK_MOVE` 是 umbrella term → 三 specific types 命名漂移已在 P2-RC-011/016 覆盖，不新建 |
| **#6 UC-INV-10 行级凭证断言** | grep `行级凭证\|subjectCode.*断言\|voucher line assertion\|dcDirection.*断言` arm-index → 命中 **P2-RC-017**（A1.18 sales AR 凭证仅合计+计数） | **new P2-RC-029** | 不同控制点：P2-RC-017 = sales `TestErpSalInvoicePosting` AR_INVOICE 凭证；本 finding = inventory `TestErpInvPosting` 存货估值凭证（PURCHASE_INPUT/SALES_OUTPUT），不同域不同 Provider 不同测试文件。同型（断言强度不足）不同控制点 → 新建 |
| **#7 UC-INV-11 行级权限** | grep `orgId\|行级权限\|dashboard.*权限\|多公司\|ErpOrgIsolationQueryTransformer` arm-index → 命中 **P1-MA2-093**（A2.18+A6.3，orgId 查询隔离全仓未落地，resolved R1.29 全局 `ErpOrgIsolationQueryTransformer`，`:99-101` 显式列 `ErpInvDashboardBizModel` 为 11 dashboard 直访之一） | **reuse P1-MA2-093** | §7 同根因（无 IUserContext.getOrgId + 空 data-auth + dashboard 直访）同控制点（行级权限 orgId scope），A2.18 `:99-101` 显式列 inventory dashboard → 复用不新建。追加 RC 视角注记 + 运行时覆盖存疑点交 MA4（遵循 A1.7/A1.11/A1.21/A1.24 sibling 先例） |
| **#8 UC-INV-11 缺料阈值** | grep `缺料\|safetyStock\|安全库存阈值\|shortage.*threshold\|dashboard.*config` arm-index → **零命中** | **new P2-RC-030** | 新控制点（缺料预警阈值 derivation 非 NopSysVariable config key） |

### 6.2 §4 三判据复核：P1-MA2-062（UC-INV-07 completeTake Deferred）

> 方法论 §4「显式人工批准记录」三判据，用于复核 R1.19 resolution（方案 B owner doc Deferred 标注 only）是否构成合法 documented simplification。遵循 A1.24 P1-MA2-061 同型先例。

| 判据 | 证据要求 | P1-MA2-062 复核结果 |
|------|---------|---------------------|
| **(i) plan 含独立 plan-audit 通过记录** | plan 的 `Draft Review Record` / `## Closure` 含独立子代理或审查者的通过证据 | **字面满足但非人工批准**：R1.19 plan（`docs/plans/2026-07-30-0512-2-r1-19-inventory-stocktake-picking-deferred.md`）含 `Draft Review Record`（`ses_05030be58ffeFgem8okz3j5sGt` accept）。但 methodology §4 line 168 明确：「代理独立审计通过='审计裁决质量证据'...**不算**人工批准」。**额外削弱**：R1.19 `Closure Audit Evidence`（plan L122）自承「Auditor / Agent: 执行代理（本会话）；独立结束审计为后续独立会话步骤」= **执行者自审的 hollow closure**（`project-context.md §已知失败模式`：closure-pending 计划缺独立结束审计），Closure Gate `[x] 结束审计由独立子代理执行`（L99）被勾选但与证据矛盾 |
| **(ii) owner doc 显式 documented simplification 标注且经人工批准** | owner doc 含显式 `documented simplification`/`Deferred` 段落 + 批准来源可追溯（git log/commit/讨论文档） | **不满足**：`state-machine.md §盘点单状态机:159` 含 Deferred 标注（「差异调整移动单的自动生成=Deferred」），但 git log 全 AI commits，**无人工批准痕迹**（AI 落地补注不算，methodology §4 line 163） |
| **(iii) product-scope 范围裁剪登记** | product-scope 明确将功能列入"不在范围"或"后续阶段" + 理由 + 影响面 + 批准人 | **不满足**：`docs/requirements/product-scope.md:16` 仅列「库存 \| app-erp-inventory \| 库存移动单、库存流水、库存余额、调拨、**盘点**、批次/序列号」为域能力——盘点 IS in scope。grep `盘点自动生成\|盘盈\|盘亏\|差异移动单\|stocktake.*defer\|裁剪` product-scope **零命中**——未将「盘点自动生成移动单」裁剪出范围 |

**§4 三判据复核结论**：**在「人工批准」意义上不满足**。(i) 字面满足（AI 子代理草案审查记录存在）但 methodology §4 line 168 明确独立审计 ≠ 人工批准，且 R1.19 closure 为执行者自审 hollow closure；(ii) owner doc Deferred 标注无人工批准痕迹；(iii) product-scope 未裁剪。按 Q4=(a) P1 禁方案 B 关闭规则，**倾向重开 P1 入 MR1**——但本切片 Non-Goal 不自决范围，**须人工确认 product-scope 是否裁剪「盘点自动生成」**：
- 若裁剪 → §4 (iii) 改 product-scope 真相源（需求变更非降级，须登记变更理由 + 影响面 + 批准人）
- 若未裁剪 → P1 强制实现 `completeTake` 自动比对 `StockTakeLine.actualQuantity` vs `ErpInvStockBalance.totalQuantity` → 差异（盘盈正/盘亏负）经 `IErpInvStockMoveBiz.generateMove` Facade 生成差异移动单（复用既有 Facade + 既有字段，属代码逻辑类预授权，不触 ORM 结构变更 / 不触 §5 ask-first）

**与既有 finding 关系**：P1-MA2-062 是 completeTake stub 的既有 finding（A2.11 登记）。本切片为**复用**（§7 同根因同控制点）——追加 RC 视角 §4 三判据复核注记，**不新建编号**（对齐 A1.24 P1-MA2-061 复用先例）。

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点，每存疑点一行。

- **SP-1（UC-INV-07）**：completeTake 置 DONE 后库管员手工 `generateMove` 处置差异的实际余额影响——手工入口仍走移动单状态机（余额可追溯，A2.11 已证），但「自动生成」缺失致账实差异在库管员介入前悬留（账实一致性运营风险，非数据破坏）。
- **SP-2（UC-INV-08）**：高并发下 `max-retry`（默认 5）耗尽后抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT` 时移动单状态——`doComplete` 经 `@BizMutation` 事务回滚，移动单不留悬挂，但极端竞争（>5 线程同维度）的最终一致性需运行时确认（A2.17 已静态证实 retry 机制，运行时极限场景交 MA4）。
- **SP-3（UC-INV-10）**：posting 失败留 `posted=false` 时 `DeferredPostingSweepJob` 兜底实际触发频率与成功率（属 P1-MA4-001 family 业财悬挂维度，本切片不重复登记，交 MA4/A4.1 展开）。
- **SP-4（UC-INV-11）**：全局 `ErpOrgIsolationQueryTransformer`（P1-MA2-093 resolved R1.29）对 inventory dashboard 直连路径（`daoProvider.findAllByQuery`/`ormTemplate.findListByQuery`/`sumBalanceTotalCost`/`loadDoneMovesInRange`）的**运行时覆盖有效性**——单组织种子（orgId=2）掩盖跨组织泄漏，多组织部署 + 用户归属 orgA 查 orgB 数据是否泄漏需 MA4 运行时验证（遵循 sibling 先例 A1.7 SP-4 / A1.11 SP-3 / A1.21 SP-3 / A1.24③，复用 P1-MA2-093 运行时确认）。

**P0 即时通道**：本切片 Phase 1 未定级出 P0，未触发 MR0。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0）。actual vs baseline 汇总（与本审计相关 R1/R2 规则）：

  | 规则 | baseline（`compliance-baseline.md §BASELINE`） | actual（HEAD 实测） | 漂移 |
  |------|------|------|------|
  | R1a dao().saveEntity (BizModel) | 0 | 0 | — |
  | R1b dao().updateEntity (BizModel) | 0 | 0 | — |
  | R1c dao().getEntityById (BizModel) | 0 | 0 | — |
  | R1d dao().findAllByQuery (BizModel) | 14 | 14 | — |
  | R2a BizModel daoFor(ErpMd*) | 34 | 34 | — |
  | R2b BizModel daoFor(Erp*) 跨域 | 240 | 229 | −11（actual ≤ baseline，无回归） |
  | R2c 全生产代码 daoFor() 总量 | 1380 | 1382 | +2（**pre-existing HEAD state**，见下声明） |
  | R2d Processor daoFor(ErpMd*) | 32 | 34 | +2（**pre-existing HEAD state**，见下声明） |

  **声明**：本审计为**只读审计**（零生产代码/ORM/api.xml/view.xml 变更），故本审计**不引入任何 checker 回归**——R2c/R2d 的 +2 漂移是本审计执行前已存在的 HEAD 仓库状态（与本审计无关，属既有基线漂移，非本审计职责）。**不以 checker 脚本退出码 0 作为门控通过依据**（methodology §8：checker 是纯 reporter，真正门控在 CI workflow `.github/workflows/compliance.yml` 解析 actual > baseline => sys.exit(1)）。本报告无生产代码变更，checker 无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index inventory 盘点/并发/估值/看板同域同控制点后给出"复用 or 新增"裁决（§6.1 裁决表），无未经比对直接新建的 finding。P2-RC-029/P2-RC-030 经 grep 确认为新控制点；P1-MA2-062（UC-INV-07）/ P1-MA2-093（UC-INV-11 行级权限）/ P2-RC-011+P2-RC-016（UC-INV-10 命名漂移）经 grep 确认同根因同控制点 → 复用并列明差异依据。

---

## 9. 与 MA2 报告差异增量声明

> §去重协议：本审计不复跑 MA1-MA7，复用既有 MA2 报告已证实行为，只补"需求契约↔实际行为"差异。

| 既有报告 | 已证实内容 | 本切片补的「需求契约↔行为」差异增量 |
|---------|-----------|--------------------------------------|
| `2026-07-28-0400-arm-ma2-inventory-state-machine.md`（A2.11） | UC-INV-08 并发扣减乐观锁 + UK 兜底 PASS（P0-MA2-020 done 方案 A）；移动单状态机 + 出库 approve 可用量校验 PASS；**P1-MA2-062**（StockTake completeTake 未自动生成盘盈/盘亏移动单）resolved R1.19 方案 B（owner doc Deferred） | **UC-INV-07 需求视角差异**：A2.11 从状态机行为视角登记 P1-MA2-062 并 resolved R1.19 方案 B（owner doc Deferred）。本切片从需求契约视角（Q4=(a) P1 禁方案 B）复核 §4 三判据——R1.19 resolution 在「人工批准」意义上不满足（(i) AI 子代理草案审查 ≠ 人工批准 + R1.19 closure 为执行者自审 hollow closure + (ii) owner doc Deferred 无人工批准痕迹 + (iii) product-scope 未裁剪）→ **倾向重开 P1 入 MR1**，须人工确认 product-scope |
| `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（A2.17） | inventory 并发扣减 sustained（PASS），`:100/:230/:260-303` | 本切片复用（UC-INV-08 接受，不重复验证行为；补 L1「不出现负数,除非允许负库存」语义复核 = `CONFIG_ALLOW_NEGATIVE_STOCK` 默认 false 满足） |
| `2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5） | `ErpInvStockMoveProcessor` / `InvPostingDispatcher` / `StockMoveBookkeeper` 代码质量 PASS | **UC-INV-10 需求视角差异**：A4.5 从代码质量视角 PASS。本切片补 L1 字面「STOCK_MOVE 业务类型」vs 实现 `PURCHASE_INPUT`/`SALES_OUTPUT`/`MANUFACTURING_RECEIPT` 命名漂移（reuse P2-RC-011/016 watch-only）+ L1「post-commit 异步」vs 实现同步 doComplete + REQUIRES_NEW 隔离时序漂移（行为等价接受）+ JUnit 行级凭证断言缺失（new P2-RC-029） |
| `2026-07-28-1510-arm-ma2-multi-company-isolation.md`（A2.18）+ `2026-07-29-1410-arm-ma6-data-permission-runtime.md`（A6.3） | **P1-MA2-093 resolved R1.29**（2026-07-31，全局 `ErpOrgIsolationQueryTransformer` 注入查询管道层强制 orgId scope），A2.18 `:99-101` 显式列 `ErpInvDashboardBizModel` 为 11 dashboard 直访之一 | **UC-INV-11 需求视角差异**：L1 `:207`「看板数据受行级权限约束」满足性取决于全局 transformer 对 dashboard 直连路径（`daoProvider.findAllByQuery`/`ormTemplate.findListByQuery`）的运行时覆盖有效性——此为**静态存疑点交 MA4 展开**（遵循 sibling 先例 A1.7/A1.11/A1.21/A1.24 复用 P1-MA2-093 不新建）+ 缺料阈值 config 化边界（new P2-RC-030） |

---

## 报告 9 段完整性自检

- [x] §1 需求契约原文（4 UC L1 逐字引用）
- [x] §2 实现证据（L3 代码路径含行号 + 跨域调用链）
- [x] §3 测试证据（L4 测试断言 + 断言强度）
- [x] §4 运行时行为证据（L5）
- [x] §5 符合性结论（五级追踪矩阵 + 4 UC 结论 + 8 候选缺口分级）
- [x] §6 与 arm-index 衔接（§7 复用/新增裁决 + §4 三判据复核 P1-MA2-062）
- [x] §7 静态存疑点清单（4 存疑点供 MA4）
- [x] §8 过程纪律自检（checker actual vs baseline 表 + 独立性 + 交叉去重声明）
- [x] §9 与 MA2 报告差异增量声明

**pass（零 P0、0 新 P1[#1 UC-INV-07 reuse P1-MA2-062 §4 复核倾向重开须人工确认 product-scope]、2 新 P2[P2-RC-029 UC-INV-10 行级凭证断言 / P2-RC-030 UC-INV-11 缺料阈值]、3 reuse[P1-MA2-062 / P1-MA2-093 resolved R1.29 / P2-RC-011+P2-RC-016]、UC-INV-08 接受、UC-INV-10 接受含 caveat、UC-INV-11 接受 on ⑩+⑪滞销/批次效期 + ⑫reuse+存疑点交 MA4 + ⑬new P2）**。resolved finding HEAD 复核：P1-MA2-062（resolved R1.19 方案 B owner doc Deferred，本切片 §4 复核倾向重开须人工确认 product-scope）/ P1-MA2-093（resolved R1.29 全局 IQueryTransformer，inventory dashboard 视角维持 resolved + 运行时覆盖存疑点交 MA4）/ P2-RC-011+P2-RC-016（watch-only 命名漂移，UC-INV-10 投影复用）。本切片解除 A1.27 在 MA4（A4.1 业财展开器）及 MR1（R1.0）链路的该切片证据缺口。
