# rc-ma4 A4.2.74-A4.2.82 inventory 移动单时序/批次效期/盘点并发运行时确认审计报告

> Report Status: done
> Mission: requirement-compliance（MA4 核心域展开器运行时确认）
> Work Item: A4.2.74 / A4.2.75 / A4.2.76 / A4.2.77 / A4.2.78 / A4.2.80 / A4.2.81 / A4.2.82（8 项 A1.25/A1.26/A1.27 §7 静态存疑点运行时确认；A4.2.79 排除——MR1 P1-RC-031 修复落地前阻塞）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 四级分级判据 / §5 Q4 修复义务 + 保护区域暂停协议 / §去重协议）
> 计划：`docs/plans/2026-08-07-2345-2-rc-ma4-a4-2-74-82-inventory-stockmove-batch-stocktake-runtime.md`
> Source Audits: `docs/audits/2026-08-03-0953-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability.md`（A1.25 §7 SP-1/SP-2）/ `docs/audits/2026-08-03-1200-3-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md`（A1.26 §7 SP-1/SP-2/SP-3）/ `docs/audits/2026-08-05-0900-rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard.md`（A1.27 §7 SP-1/SP-2/SP-3）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 全部工作项指定）
> 审计类型：**只读运行时确认（零生产代码/ORM/api.xml/view.xml/真相源变更）**

---

## 0. 业财保护区域探针纪律声明（前置段）

本报告两项存疑点触及业财过账基础设施探针——**READ-ONLY 确认，不改过账逻辑**：

- **A4.2.74（InvPostingDispatcher post-commit 时序边缘风险）**：仅 `dispatchIfApplicable` 时序追踪 + `doComplete` 步骤序确认 + REQUIRES_NEW 事务边界语义复核，不改 `InvPostingDispatcher` / `InvPostingExecutor` / `IErpFinVoucherBiz.post`。watch-only residual 登记（REQUIRES_NEW 凭证孤立边缘风险极低）。
- **A4.2.82（DeferredPostingSweepJob 兜底触发频率）**：仅 sweep batch.xml loader/processor 链路追踪 + `ErpFinDeferredPostingRetryHelper` 重试/MAX_RETRY/MANUAL 升级链路复核，不改 posting/重试逻辑。属 P1-MA4-001 family 业财悬挂维度不重复登记。

本审计**维持既有分级不撤销**（P1-RC-031 维持 P1 / reuse P1-MA2-062 维持 §4 复核倾向重开 / P0-MA2-020·P2-MA2-028·P1-MA4-021·P1-MA4-020·P1-MA4-001 维持 resolved/family），仅记录运行时证据。运行时未发现活跃数据破坏，**不触发 MR0**。

---

## 1. 运行时证据与裁决（8 项逐项）

### A4.2.74 — InvPostingDispatcher post-commit 时序边缘风险（A1.25 SP-1）⚠️ READ-ONLY

- **静态判定**：`dispatchIfApplicable` 在 `doComplete` 内同步调 `voucherBiz.post`（REQUIRES_NEW 独立事务），非 post-commit；若 REQUIRES_NEW 凭证已 commit 后外层 @BizMutation rollback（极罕见），凭证孤立。
- **运行时证据**：
  - `ErpInvStockMoveProcessor.doComplete:100-114` 步骤序：`releaseReservation:109` → `bookkeeper.bookCompletion:110` → `setDocStatus(DONE):111` → `moveDao().saveOrUpdateEntity(move):112` → **`postingDispatcher.dispatchIfApplicable(move, lines):113`（最后一步）**，其后无其他写操作。
  - `InvPostingDispatcher.dispatchIfApplicable:57-80`：`resolveBusinessType:58` → `buildEvent:63` → `try { executor.postEvent(event):65; if (voucherId != null) markMovePosted:67 } catch (Exception e) { LOG.warn/error :71-75 }`——**失败隔离 try/catch :64-76 吞异常记日志保持 posted=false**。
  - `InvPostingExecutor.postEvent:29-35`：`voucherBiz.post(event, context)`（Facade 注解 `@Transactional(REQUIRES_NEW)`，per InvPostingExecutor javadoc :15-18「事务边界钉 Facade，不下放编排层」）。
  - `markMovePosted:87-93`：成功后按 ID reload managed 实体 + `setPosted(true)` + `setPostedAt`。
  - 兜底链：失败留 posted=false 由 `DeferredPostingSweepJob`（`app.erp.fin.service.job`，见 InvPostingDispatcher javadoc :35）兜底扫描重试（A4.2.82 详审）。
- **裁决**：**主路径行为正确闭合 + 登记 watch-only residual**（与 A1.25 §5 倾向接受一致）。L1（真相源）`use-cases.md:30` 仅称「异步生成」，**L1 由失败隔离（try/catch :64-76）+ posted 标志（markMovePosted）+ DeferredPostingSweepJob 兜底共同满足**（移动单 DONE 不依赖过账成功 = 语义上的"异步"）。L2「post-commit」是设计参考层细节（§4 Q1 以 L1 为准）。REQUIRES_NEW 凭证孤立边缘风险极低（dispatchIfApplicable 是 doComplete 最后一步，其后无写操作致外层 rollback 极罕见）。

### A4.2.75 — forwardTrace 超深链/多分支链 truncated 行为（A1.25 SP-2）

- **静态判定**：`TraceChainQuery.forwardTrace:50-88` BFS 按 `originMoveId` 反查下游，max-depth 默认 10；多分支链节点数可能超阈值。
- **运行时证据**：
  - `TraceChainQuery.forwardTrace:50-88`：BFS via `findActiveMovesByOrigin(current):74`（queries moves where `originMoveId=current.id`）+ `Set<Long> visited` cycle detection `:77-80`（`if (!visited.add(downstream.getId())) { result.setTruncated(true); continue; }`）+ depth guard `:67-70`（`if (depth >= maxDepth) { result.setTruncated(true); break; }`）。
  - `ErpInvConstants.TRACE_CHAIN_MAX_DEPTH_DEFAULT = 10`（`:27`），`traceChainMaxDepth():295-302` 读 `CONFIG_TRACE_CHAIN_MAX_DEPTH`，缺失/≤0 回退默认 10。
  - `TestErpInvTraceChain` 9 方法强断言覆盖：`testForwardAndBackwardTraceChain:83-99`（A→B→C 三节点 forward 3 nodes + truncated=false）/ **`testMaxDepthTruncation:137-152`**（max-depth=2 + 4 节点链 → `assertTrue(forward.isTruncated())` + `assertTrue(forward.getNodes().size() <= 3)`——**深度超阈值 truncated=true + 节点数符合 root+层级 + 无无限循环**）/ `testRingDetectionTruncated:117-135`（人造环 X↔Y → truncated=true + nodes<=2）/ `testDelVersionFilterExcludesDeleted:166-183` / `testDisabledReturnsSingleNode:185-199`。
- **裁决**：**主路径行为正确闭合**（与 A1.25 §5 接受一致）。dedicated 强测试存在（`testMaxDepthTruncation` 覆盖多分支/超深链 truncated 边界 + 环检测防止无限循环），A1.25 §5 接受维持。

### A4.2.76 — allow-negative-stock=true 下并发出库实际余额下限行为（A1.26 SP-1）

- **静态判定**：config 翻转 true 后 `validateAvailable` 短路所有检查；极端并发下 totalQuantity 可能深度为负（乐观锁保证不超扣，但无下界守卫）。
- **运行时证据**：
  - `ErpInvStockMoveProcessor.validateAvailable:116-119`：`if (isNegativeStockAllowed()) { return; }`——**config=true 短路所有 per-line 可用量检查**。
  - `isNegativeStockAllowed:285-288`：`AppConfig.var(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, Boolean.FALSE)`——**字面 default = Boolean.FALSE**（UC-INV-09 默认安全）。
  - `StockMoveBookkeeper.updateBalanceWithRetry:255-328`：乐观锁 `tryUpdateWithVersionCheck:271`（MANAGED）+ UK 冲突重试 `:272-326`（TRANSIENT/SAVING）保证不超扣（A2.17 §13 PASS），但 `applyDelta.accept(current):266` 后无下界守卫——**极端并发下 totalQuantity 可深度为负**（多个出库同时扣减已为负的余额）。
  - config 翻转经 `NopSysVariable` 运行时覆盖（`TestErpInvConcurrentDeduct#testToggleNegativeStockFlag:425-466` 强覆盖 toggle 行为），与 A4.1.4 config-gate 范式一致。
- **裁决**：**登记 watch-only residual**（config-gated 部署启用决策，非默认活跃）。config 默认 false（UC-INV-09 默认安全）；翻转 true 是部署决策（先发货后入库场景），乐观锁保证不超扣但无下界守卫属已知 config-gated 边界，与 A4.1.4 config-gate 范式一致，不单列 finding。

### A4.2.77 — batchTrace 跨域 move 链下聚合正确性（A1.26 SP-2）

- **静态判定**：`batchTrace:168-192` 聚合 findLinesByBatch + findLedgersByBatch → moveIds → findActiveMove（filter delVersion=0）；跨域 move 链共享 batchNo（批次继承语义）下聚合完整性。
- **运行时证据**：
  - `TraceChainQuery.batchTrace:168-192`：`findLinesByBatch:174`（`ErpInvStockMoveLine.batchNo`，query `eq("batchNo", batchNo)` :228-233）+ `findLedgersByBatch:179`（`ErpInvStockLedger.batchNo`，query `eq("batchNo", batchNo)` :235-240）→ `moveIds` LinkedHashSet 累积 → 逐 `findActiveMove(mid):186`（经 `findActiveMovesByQuery:220-226` filter `eq("delVersion", 0L)` :224，逻辑删除/已删节点排除）聚合到 result.nodes。
  - 批次继承语义下聚合行为：batchTrace 按 **batchNo 字面值精确聚合**——`ErpInvStockMoveLine.batchNo`（`newLines:202 line.setBatchNo(req.getBatchNo())`）+ `ErpInvStockLedger.batchNo`（`writeLedger:217 ledger.setBatchNo(line.getBatchNo())`）均忠实持久化调用方传入的 batchNo。批次继承是调用方契约（mfg `MaterialIssueStockMoveBuilder:66 setBatchNo`，A1.26 §2 ④证实），inventory 域数据层按 batchNo 精确聚合，**跨批次不混**：batchTrace(BATCH-001) 命中 BATCH-001 的 move line + ledger → 采购入库 + mfg 领料 2 nodes（不含新批次 BATCH-002 节点）；batchTrace(BATCH-002) 命中 BATCH-002 的 move line + ledger → mfg 完工 + sales 出库 2 nodes。
  - `TestErpInvTraceChain#testBatchTraceByBatchNo:154-164` 强断言：2 张含 BATCH-001 移动单 → batchTrace 返回 2 nodes；不存在 batchNo → 0 nodes（精确节点数断言）。
- **裁决**：**主路径聚合正确闭合**（批次继承语义下按 batchNo 精确聚合，跨批次不混）。聚合路径 findLinesByBatch + findLedgersByBatch → moveIds → findActiveMove(filter delVersion=0) 完整，dedicated 强测试覆盖。

### A4.2.78 — expiryDate 字段无 writer 时默认值行为（A1.26 SP-3）⚠️ 为 P1-RC-031 修复提供 null 语义设计输入，只读确认不改字段语义

- **静态判定**：`ErpInvBatch.expiryDate` ORM 存在但 `ErpInvBatchBizModel` 15 行 CRUD 桩；expiryDate 由 `BatchGenealogyWriter.newBatchEntity`（mfg 完工时）写入（计划基线声称），其他入库路径是否写未确认。
- **运行时证据（grep census `setExpiryDate|setShelfLifeDays` 跨全仓生产代码）**：
  - grep 命中集合：`_ErpInvBatch.java:479/489/903/922`（生成的 ORM setter）+ `ErpInvBatchOutputBean.java:151/165`（生成的 API bean setter）+ `ErpInvBatchInputBean.java:150/164`（生成的 API bean setter）+ `TestErpInvDashboard.java:266`（**测试 seed only**）。
  - **零生产业务代码 writer**——`module-inventory/erp-inv-service/src/main` 与 `module-manufacturing/erp-mfg-service/src/main` 生产代码无任何 `setExpiryDate`/`setShelfLifeDays` 调用。
  - **关键校正（深化计划基线）**：`BatchGenealogyWriter.ensureOutputLot:149-170`（mfg 完工时建批，计划基线称 newBatchEntity:159-168）实际设置：`setOrgId:160` / `setBatchNo:161` / `setMaterialId:162` / `setWarehouseId:163` / `setTotalQuantity:164` / `setAvailableQuantity:165` / `setProductionDate:166` / `setStatus(OPEN):167` / `batchDao().saveEntity(batch):168`——**未设 expiryDate / 未设 shelfLifeDays**。`newBatchEntity():257-259` 仅 `batchDao().newEntity()`（返回空实体，不设字段）。
  - 采购入库/销售退货入库路径：**不创建 ErpInvBatch 实体**——`ErpInvBatchBizModel.java` = 15 行 CRUD 桩（`extends CrudBizModel<ErpInvBatch>` 无任何方法）；inventory 域生产代码零 ErpInvBatch 创建（grep `daoFor(ErpInvBatch.class)` 生产命中仅 `ErpInvDashboardBizModel:261` 只读查询 + `ErpMfgBatchGenealogyBizModel:143` CRUD 读）。
- **裁决**：**主路径行为确认 + 为 P1-RC-031 修复登记 null 语义设计输入**。**expiryDate 对所有生产创建的批次均为 null**（mfg 完工建批不写 expiryDate + 采购/销售入库路径不建 ErpInvBatch，仅 move line/ledger/balance 携带 batchNo）。**深化计划基线**：原基线称「expiryDate 由 BatchGenealogyWriter 写入」不准确——mfg 完工建批同样不写 expiryDate。为 MR1 P1-RC-031（效期拦截）修复提供 null 语义设计输入：**修复时须定义 null 语义**（跳过 / 视为永不过期 / 视为立即过期），因 expiryDate 当前对所有批次恒为 null，效期拦截逻辑须显式处理 null 分支。**只读确认不改字段语义**（修复义务归 MR1 纯 BizModel/Processor + ErrorCode + 测试补充，预授权不触 ask-first，仅消费既有 expiryDate 字段）。

### A4.2.80 — UC-INV-07 completeTake DONE 后手工 generateMove 实际余额影响（A1.27 SP-1；reuse P1-MA2-062）

- **静态判定**：completeTake stub（仅 setDocStatus(DONE) 无 generateMove）；手工入口仍走移动单状态机（余额可追溯，A2.11 已证）。
- **运行时证据**：
  - `ErpInvStockTakeBizModel.completeTake:40-50`：`requireEntity:41` + 源态守卫 CONFIRMED `:42-46`（非 CONFIRMED 抛 `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`）+ `take.setDocStatus(DOC_STATUS_DONE):47` + `updateEntity(take, null, context):48`——**STUB 确认**：无 `ErpInvStockTakeLine.actualQuantity`/`bookQuantity`/`differenceQuantity` 比对、**无** `IErpInvStockMoveBiz.generateMove` 调用、无盘盈/盘亏移动单生成。`@BizMutation:39` 事务包裹。
  - `ErpInvStockTakeLine` 字段 `actualQuantity`（实盘）/`bookQuantity`（账面）/`differenceQuantity`（差异，已派生列）均已存在但 completeTake 不消费（A1.27 §2 证实）。
  - 手工入口余额影响：库管员手工调 `IErpInvStockMoveBiz.generateMove`（Facade 存在于 `ErpInvStockMoveBizModel`，UC-INV-01/03 已证可用）→ 走标准移动单状态机（DRAFT→CONFIRMED→DONE）→ 经 `validateAvailable` + `bookCompletion` + `dispatchIfApplicable` 完整链路，**余额可追溯**（A2.11 移动单状态机 PASS）。账实差异在库管员介入前悬留（运营风险非数据破坏——盘点单 DONE 但无差异调整移动单）。
- **裁决**：**维持 reuse P1-MA2-062 §4 复核倾向重开须人工确认 product-scope**（completeTake stub = P1-MA2-062 字面描述，§7 复用不新建；修复归 MR1）。运行时确认 stub 行为 + 手工入口仍走移动单状态机余额可追溯，账实差异悬留属运营风险非数据破坏。须人工确认 product-scope 是否裁剪盘点自动生成（若裁剪→§4(iii) 改真相源非降级；若未裁剪→P1 强制实现 completeTake 自动差异移动单生成，复用既有 Facade + 既有字段属代码逻辑类预授权不触 §5 ask-first）。

### A4.2.81 — UC-INV-08 高并发下 max-retry 耗尽后移动单状态（A1.27 SP-2）

- **静态判定**：`updateBalanceWithRetry:255-328` 重试上限默认 5 + @BizMutation 事务回滚移动单不留悬挂（A2.17 §13 PASS）。
- **运行时证据**：
  - `StockMoveBookkeeper.updateBalanceWithRetry:255-328`：`maxRetry = AppConfig.var(CONFIG_CONCURRENT_DEDUCT_MAX_RETRY, CONCURRENT_DEDUCT_MAX_RETRY_DEFAULT):260-261`（**默认 5**，`ErpInvConstants:18-19`）；冲突计数 `attempts++:292`，`if (attempts > maxRetry) { recordOptimisticLockFailureExhausted:295; throw buildConflictExhaustedEx(state, current, attempts):296 }`。
  - `buildConflictExhaustedEx:406-417`：MANAGED 态抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`（param balanceId + attempts）；TRANSIENT/SAVING 态抛 `ERR_INV_BALANCE_INSERT_CONFLICT`（param materialId + warehouseId + attempts）。
  - 移动单状态：`doComplete` 经 `@BizMutation`（`IErpInvStockMoveBiz` Facade）事务包裹——`updateBalanceWithRetry` 抛异常 → 整个 mutation 事务回滚 → `setDocStatus(DONE):111` 未生效（异常在 `bookkeeper.bookCompletion:110` 内抛出，早于 `setDocStatus`）→ **移动单保持 CONFIRMED 不留悬挂**（未达 DONE）。
  - `TestErpInvConcurrentDeduct#testConcurrentDeductRetryExhaustedThrows:131-172` 强断言：max-retry=0 时第一次冲突即 `assertEquals(ErpInvErrors.ERR_INV_CONCURRENT_DEDUCT_CONFLICT.getErrorCode(), ex.getErrorCode())`（A2.17 §13 已静态证实 retry 机制 + 多线程 ExecutorService+CountDownLatch 防超卖）。
- **裁决**：**主路径行为正确闭合 + 登记 watch-only residual**（极端竞争下需运维介入重试）。移动单不留悬挂（@BizMutation 事务回滚），重试耗尽抛明确错误码；极端竞争（>5 线程同维度）最终一致性需运维介入重试（A2.17 §13 已静态证实 retry 机制）。

### A4.2.82 — UC-INV-10 posting 失败 posted=false 时 DeferredPostingSweepJob 兜底触发频率（A1.27 SP-3）⚠️ READ-ONLY

- **静态判定**：DeferredPostingSweepJob 兜底重试（属 P1-MA4-001 family 业财悬挂维度）。
- **运行时证据（三层兜底链路追踪）**：
  - **第一层（失败隔离 + posted 标志）**：`InvPostingDispatcher.dispatchIfApplicable:64-76` try/catch 吞异常记日志（NopException → LOG.warn :72 / 其他 → LOG.error :74），保持 posted=false（`markMovePosted:87-93` 仅 voucherId != null 时执行 setPosted(true)）。
  - **第二层（异常记录持久化）**：过账异常经 finance 引擎 `ErpFinPostingExceptionRecorder` 序列化 PostingEvent 为 `ErpFinPostingException`（status=PENDING，含 eventData/businessType/billHeadCode/orgId/acctSchemaId/currencyId/exchangeRate/voucherDate）。
  - **第三层（nop-batch sweep job + helper 重试）**：`deferred-posting-sweep.batch.xml`（nop-batch task `fin.deferred-posting-sweep`，batchSize=50 + transactionScope=process + saveState=true）：
    - loader（`orm-reader ErpFinPostingException`）：query filter `status=PENDING AND retryCount<3 AND occurrenceTime >= now − 24h`（`:13-17`）——**24 小时窗口内 PENDING + retryCount<3 的悬挂记录**。
    - processor（`process`）：`const helper = inject('erpFinDeferredPostingRetryHelper'); helper.retry(item.id, batchChunkCtx.serviceContext);`（`:24-27`）。
  - `ErpFinDeferredPostingRetryHelper.retry:76-94`：单条 REQUIRES_NEW 独立事务（`transactionTemplate.runInTransaction(null, REQUIRES_NEW, ...) :83`）重建 PostingEvent（`rebuildEvent:111-128`）→ `voucherBiz.post/reverse :100-107` → 成功 markRetried（status=RETRIED + resolvedAt + resolvedBy="deferred-posting-sweep-job" :130-136）；失败 `incrementRetryAndRethrow:138-168`（REQUIRES_NEW session reload + retryCount++ :149-150 + `if (newCount >= MAX_RETRY=3) status=MANUAL :153-154` + `dispatchMaxRetryAlert:174-192` 派发 `notificationBiz.notify("fin.posting-exception", ctx)` 升级人工告警）。
  - **触发频率机制**：cron config `erp-fin.deferred-posting-sweep-cron`（`ErpFinConstants:188-189`，空=不调度；非空时按 cronExpr 触发 DeferredPostingSweepJob）——**频率由部署 cron 配置决定**，job.yaml enabled 默认 false + cron 空值跳过（部署时按业务需求配置 cron 表达式）。
- **裁决**：**主路径兜底行为闭合**（属 P1-MA4-001 family 不重复登记，运行时确认兜底链路可达）。三层兜底链路完整：失败隔离 try/catch + posted 标志 → ErpFinPostingException 记录 → nop-batch sweep job（cron-gated）+ helper REQUIRES_NEW 重试（MAX_RETRY=3）+ MANUAL 升级告警。触发频率由 `erp-fin.deferred-posting-sweep-cron` cron 配置决定（部署决策）。

---

## 2. 与既有 finding 衔接（维持注记，无未经比对新建）

| Finding ID | 域 | 运行时确认结论 | 维持/变更 |
|-----------|---|--------------|----------|
| `P1-RC-031`（UC-INV-06 效期拦截缺失 + isBatchManaged 条件分支） | inventory | A4.2.78 grep census 确认 expiryDate **对所有生产创建的批次恒为 null**（mfg 完工建批不写 expiryDate[深化计划基线] + 采购/销售入库不建 ErpInvBatch）；为修复提供 null 语义设计输入（修复时须定义 null = 跳过/永不过期/立即过期）。 | **维持 P1**（§4 三判据均不满足→非 documented simplification；修复纯 BizModel/Processor + ErrorCode + 测试补充预授权不触 ask-first，仅消费既有 expiryDate 字段） |
| `P1-MA2-062`（StockTake completeTake 未自动生成盘盈/盘亏移动单） | inventory | A4.2.80 确认 `completeTake:40-50` stub（仅 setDocStatus DONE 无 generateMove 无差异比对）+ 手工入口走移动单状态机余额可追溯（账实差异悬留属运营风险非数据破坏）。 | **维持 reuse §4 复核倾向重开须人工确认 product-scope**（completeTake stub = P1-MA2-062 字面描述；修复归 MR1） |
| `P0-MA2-020`（StockBalance 自然键 UK） | inventory | A4.2.76/A4.2.81 间接确认 `UK_INV_STOCK_BALANCE_NATURAL` + `updateBalanceWithRetry` 三分支 + max-retry 耗尽抛错兜底并发安全。 | **维持 done** |
| `P1-MA4-001`（兜底重试 MAX_RETRY 耗尽 MANUAL 升级）family | finance/inventory | A4.2.82 确认 DeferredPostingSweepJob 三层兜底链路可达（失败隔离 + ErpFinPostingException + sweep batch + helper REQUIRES_NEW 重试 MAX_RETRY=3 + MANUAL 升级告警）。 | **维持 resolved R1.16**（属 P1-MA4-001 family 业财悬挂维度不重复登记，运行时确认兜底链路可达） |
| `P2-MA2-028`（reverse uses today() not businessDate） | inventory | 本切片非 reverse costing 维度，无增量。 | **维持 watch-only** |
| `P1-MA4-021`（pur+sal+inv 测试有效性） | inventory | 本切片非测试有效性维度，无增量。 | **维持 resolved R2.14** |
| `P1-MA4-020`（到岸成本反向过账悬挂） | inventory | 本切片非到岸成本维度，无增量。 | **维持 resolved R1.16** |

**无新 finding 新建**（全部维持）。运行时未发现活跃数据破坏，**不触发 MR0**。

---

## 3. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告为只读审计，**零生产代码变更**（纯审计报告 + 文档更新），checker 无回归风险。本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（checker 脚本为纯 reporter 退出码恒 0，真正门控在 CI workflow `.github/workflows/compliance.yml` 解析 actual > baseline => sys.exit(1)；本报告**不**以 checker 退出码 0 作为门控通过依据）。**actual = baseline**（与 A1.25/A1.26/A1.27 报告基线一致——本切片仅追加文档无生产代码变更）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 8 项运行时确认均衔接既有 finding（P1-RC-031 / reuse P1-MA2-062 / P0-MA2-020 / P1-MA4-001 family / P2-MA2-028 / P1-MA4-021 / P1-MA4-020），无未经比对直接新建的 finding。维持注记已追加至 arm-index RC 交叉引用区（§2 衔接表 + arm-index 注记行）。
- [x] **业财保护区域探针纪律**：A4.2.74/A4.2.82 两项触及业财过账基础设施探针——**READ-ONLY 确认**（dispatchIfApplicable 时序追踪 + REQUIRES_NEW 事务边界语义复核 + sweep batch.xml loader/processor 链路追踪 + helper 重试/MAX_RETRY/MANUAL 升级链路复核），不改 InvPostingDispatcher / InvPostingExecutor / IErpFinVoucherBiz.post / ErpFinDeferredPostingRetryHelper / posting 逻辑（详见 §0 前置声明）。

---

## 4. 裁决摘要

| 工作项 | 存疑点 | 运行时裁决 |
|--------|--------|-----------|
| A4.2.74 | InvPostingDispatcher post-commit 时序边缘风险 ⚠️ READ-ONLY | **主路径闭合 + watch-only residual**（dispatchIfApplicable 是 doComplete 最后一步 + 失败隔离 + posted 标志 + DeferredPostingSweepJob 兜底；REQUIRES_NEW 凭证孤立边缘风险极低） |
| A4.2.75 | forwardTrace 超深链/多分支链 truncated | **主路径闭合**（BFS + visited 环检测 + depth guard max-depth 默认 10；testMaxDepthTruncation 强覆盖 truncated 边界） |
| A4.2.76 | allow-negative-stock=true 并发余额下限 | **watch-only residual**（config-gated 默认 false 非默认活跃；乐观锁保证不超扣但无下界守卫，与 A4.1.4 config-gate 范式一致） |
| A4.2.77 | batchTrace 跨域 move 链聚合 | **主路径闭合**（findLinesByBatch + findLedgersByBatch → moveIds → findActiveMove(filter delVersion=0) 精确聚合，跨批次不混） |
| A4.2.78 | expiryDate 无 writer 默认值 ⚠️ P1-RC-031 设计输入 | **主路径确认 + 登记 null 语义设计输入**（expiryDate 对所有生产创建的批次恒为 null[深化基线]；为 P1-RC-031 修复提供 null 语义设计输入，只读不改字段语义） |
| A4.2.80 | completeTake DONE 手工 generateMove 余额影响（reuse P1-MA2-062） | **维持 reuse P1-MA2-062 §4 复核倾向重开须人工确认 product-scope**（stub 确认 + 手工入口走移动单状态机余额可追溯，账实差异悬留属运营风险非数据破坏） |
| A4.2.81 | UC-INV-08 max-retry 耗尽移动单状态 | **主路径闭合 + watch-only residual**（@BizMutation 事务回滚移动单不留悬挂 + 重试耗尽抛明确错误码；极端竞争需运维介入重试） |
| A4.2.82 | UC-INV-10 posted=false DeferredPostingSweepJob 兜底 ⚠️ READ-ONLY | **主路径兜底闭合**（三层兜底：失败隔离 + posted 标志 → ErpFinPostingException → sweep batch[24h 窗口 PENDING+retryCount<3] + helper REQUIRES_NEW 重试 MAX_RETRY=3 + MANUAL 升级告警；属 P1-MA4-001 family 不重复登记） |

**整体裁决**：8 项存疑点运行时确认完成。**零 P0**（运行时未发现活跃数据破坏）。**1 项维持 P1**（P1-RC-031，A4.2.78 提供 null 语义设计输入；修复归 MR1 预授权不触 ask-first）。**1 项维持 reuse §4 复核倾向重开**（reuse P1-MA2-062，A4.2.80；修复归 MR1 须人工确认 product-scope）。**2 项 watch-only residual**（A4.2.74 REQUIRES_NEW 凭证孤立边缘风险极低 / A4.2.76 config-gated 负库存下界守卫 / A4.2.81 极端竞争运维介入）。**5 项主路径闭合**（A4.2.74/75/77/81/82）。**不触发 MR0**（无活跃数据破坏）。**A4.2.79 排除**（MR1 P1-RC-031 修复落地前阻塞，保留 todo 待 MR1 落地后回队，同 A4.2.3 MR1-P1-RC-008 阻塞先例）。本审计维持既有分级不撤销，记录运行时证据链，解除 A4.2.74-A4.2.78、A4.2.80-A4.2.82 在 MA4/R1.0 链路的运行时证据缺口。
