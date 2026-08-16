# rc-ma4 A4.2.3 mfg 预留写路径落地后跨工单并发 + reserved/available 一致性运行时确认审计报告

> Report Status: done
> Mission: requirement-compliance（MA4 回队行运行时确认——A4.2.3 展开器，MR1 P1-RC-008 预留写路径落地后）
> Work Item: A4.2.3（MA4 回队行：MR1 P1-RC-008 预留写路径落地后 reservedQty/availableQuantity 实时一致性 + 跨工单并发预留 lost-update 防护运行时核验）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 四级分级判据 / §5 Q4 修复义务 / §去重协议）
> 计划：`docs/plans/2026-08-16-0424-3-rc-ma4-a4-2-3-mfg-reservation-runtime-confirmation.md`
> Source Audits: `docs/audits/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（§7 SP-3）+ `docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`（§7 SP-3）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定 Skill）+ `nop-testing`（测试探针，镜像 `TestErpInvReservationWriteApi`/`TestErpMfgReservationLifecycle` 范式）
> 审计类型：**运行时行为验证（只读确认 + 无条件新增测试探针，零生产代码/ORM/api.xml/config 默认值/真相源变更）**

---

## 0. TL;DR 裁决表

| 维度 | 结论 |
|------|------|
| 跨工单并发预留 lost-update 防护 | **成立（双实证）**——既有写 API 层并发断言复跑（4+4=8 无丢失 + available=2）+ 新增 mfg 集成层跨工单并发探针（两工单同物料并发 approve → 两工单预留均落库 + reservedQuantity 累加 4+4=8 + available=2，无异常/无重试耗尽） |
| reserved/available 一致性 | **恒等式运行时成立**——创建（reserved+ / available=total−reserved）/ 释放（reserved− / available 恢复）/ 消耗（reserved− 与 total 守恒）断言链复跑 + 探针守恒断言 |
| 并发防护机制 | `updateBalanceWithRetry` versionProp 乐观锁（tryUpdateWithVersionCheck）+ UK 冲突 evict+reload+重试（`erp-inv.concurrent-deduct-max-retry`=5）运行时确认，与 A4.2.1 前置结论对照一致（无 silent split-quantity corruption） |
| 新 finding | **0**（不新建，不重新裁决 P1-RC-008 分级——修复已由 RC-R1.48 落地） |
| MR0 | 不触发（运行时未发现活跃数据破坏） |
| 验证 | `mvn test -pl module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service` **218 + 270 tests 全绿**（既有 218/269 零回归 + 新增探针 1）+ `mvn clean install -DskipTests` 全量 BUILD SUCCESS + compliance checker actual == baseline 零漂移（探针零生产面漂移） |
| roadmap | A4.2.3 `todo → done ✅` + arm-index P1-RC-008 行追加 A4.2.3 运行时注记（不新建 finding） |

---

## 1. 存疑点原文（A1.8 §7 SP-3 + A1.9 §7 SP-3 合并摘录）

两源切片存疑点行合并摘录如下（`2026-08-02-2042-2-...-a1-8-...md` §7 SP-3 + `2026-08-02-2042-3-...-a1-9-...md` §7 SP-3）：

> **SP-3**：MR1 修复落地后 reservedQty/availableQuantity 实时一致性 + 跨工单并发预留 lost-update 防护——预留写路径实现后（RC-R1.48 修复 P1-RC-008），reserved/available 一致性 + 跨工单并发预留运行时核验。与 A1.8 SP-3 同根因同控制点（MaterialReservation 子系统写路径一致性）。
>
> 触发条件：MR1 P1-RC-008 修复落地（2026-08-16 RC-R1.48 已落地）。
>
> 验证方式：A4.2.3 展开器按「运行时确认」执行——跨工单并发预留真实并发场景 + reserved/available 一致性运行时核验（roadmap A4.2.3 行 2026-08-16 回队解锁注记逐字义务）。

---

## 2. 既有测试复跑证据（Proof，Phase 1 item 1）

### 2.1 模块测试全绿复跑

`mvn test -pl module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service`（2026-08-16 复跑）：

- **erp-inv-service：218 tests，0 Failures，0 Errors**（含 `TestErpInvReservationWriteApi` 10 组，RC-R1.48 基线）
- **erp-mfg-service：270 tests，0 Failures，0 Errors**（RC-R1.48 基线 269 + 本计划新增探针 1，零回归）

### 2.2 `testConcurrentCreateReservationNoLostUpdate`（写 API 层并发，既有断言）

`TestErpInvReservationWriteApi.java:234-285` 复跑全绿（实测包含在 218 内），断言链：

- 线程构造：`ExecutorService(2)` + `CountDownLatch(1)` 起步栅栏 + `CountDownLatch(2)` 完成栅栏 + `AtomicReference<Throwable> firstError`；每线程 `ContextProvider.newContext()` + `ormTemplate.runInSession(workerSession -> ...)` 独立会话直调 `reservationBiz.createReservation`（`ReservationCreateRequest`，同物料同余额行，各自 sourceBillCode）。
- 最终断言（`TestErpInvReservationWriteApi.java:277-284`）：`reservedQuantity == 8`（**4 + 4 = 8，无丢失更新**）+ `availableQuantity == 2`（**可用量 = 10 − 8 = 2**）。

### 2.3 创建/释放/消耗后余额断言链（reserved/available 一致性）

`TestErpInvReservationWriteApi` 余额断言链复跑全绿（含在 218 内）：

| 测试 | 场景 | 余额断言 |
|------|------|----------|
| `testCreateReservationReservesBalance` :97-99 | 创建预留 | `reservedQuantity = 5`（+= 5）+ `availableQuantity = 5`（10 − 5） |
| `testCreateReservationMinSemantics` :112-113 | min 语义 | `reserved = 3` + `available = 0`（3 − 3） |
| `testReleaseCancelledReleasesAll` :142-143 | 取消释放 | `reserved = 0`（− 5）+ `available = 10`（恢复） |
| `testReleaseCompletedAfterPartialConsume` :162 | 部分领料+完工释放 | `reserved = 0`（余额清零） |
| `testConsumeTracksAndDecrements` :192-196 | 消耗 | 领 4 后 `reserved = 6`（10 − 4）；领完后 `reserved = 0` |
| `testConcurrentCreateReservationNoLostUpdate` :279-282 | 并发 | `reserved = 8` + `available = 2` |

`testCreateReservationReservesBalance` :99 显式断言 `availableQuantity == totalQuantity − reservedQuantity`（10 − 5 = 5），恒等式 `available = total − reserved − locked` 语义与 `recomputeAvailable` 派生口径一致。

### 2.4 mfg 集成层生命周期复跑（四接线）

`TestErpMfgReservationLifecycle` 既有 9 组复跑全绿（①审核创建②取消释放③完工释放④领料消耗⑤超预留放行⑥config 关闭全链跳过⑦头状态终态⑧旧数据 no-op⑨无 BOM 不阻断），与探针（⑩）合计 10 组全绿（含在 270 内）。

---

## 3. 并发防护机制运行时确认（Proof，Phase 1 item 2）

### 3.1 `updateBalanceWithRetry` 版本乐观锁 + UK 冲突重试（只读 read）

`StockMoveBookkeeper.java:256-328` 运行时机制（grep/read 实录）：

- **MANAGED 路径**（余额行已存在，预留写主路径）：`dao.tryUpdateWithVersionCheck(current)`（:271）——**versionProp 乐观锁**，`WHERE version = ?` 0-row 返回判冲突。
- **TRANSIENT/SAVING 路径**（首笔余额行）：`dao.saveEntity(current)` queue INSERT + `flushAndCheckConflict`（:274-278）——flush 捕获 `ERR_SQL_DUPLICATE_KEY`（自然键 `UK_INV_STOCK_BALANCE_NATURAL` 冲突，`flushAndCheckConflict:334-344` 经 `isUniqueConstraintViolation` 判定）。
- **冲突处理**（:288-326）：`ErpInvConcurrencyMetrics.recordOptimisticLockFailure` 计数（:290，observability.md §5.1 指标 4）→ `attempts++` → 超 `maxRetry` 抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`（MANAGED，`buildConflictExhaustedEx:408-411`）或 `ERR_INV_BALANCE_INSERT_CONFLICT`（INSERT，:412-415）→ 未超限则 **evict 失败实例 + reload**（MANAGED 按主键 `requireEntityById`；INSERT 按自然键 `findBalanceByNaturalKey`，找不到退化 newBlankBalance 重试）→ 重新 `applyDelta` 循环。
- **重试上限**：`ErpInvConstants.CONFIG_CONCURRENT_DEDUCT_MAX_RETRY = "erp-inv.concurrent-deduct-max-retry"` 默认 5（`ErpInvConstants.java:18-19`）。
- **readonly 粘性注记**（:249-250）：冲突 reload 必须 evict 后取新实例，复用旧实例会致后续 flush 跳过该实体（机制完整性说明）。

### 3.2 与 A4.2.1 前置结论对照

A4.2.1（`2026-08-06-1926-rc-ma4-a4-2-1-2-mfg-reservation-availability-runtime.md`）结论：「预留量并发扣减无 silent split-quantity corruption——`updateBalanceWithRetry` versionProp 乐观锁 + P0-MA2-020 UK + 重试串行化扣减，无 delta 丢失」。本计划 HEAD 复核（RC-R1.48 落地后同站点 :256-328）**机制一致，无回退**——预留写（create/release/consume）全经同一 `updateBalanceWithRetry` 入口（RC-R1.48 修复落地证据），跨工单并发双写同一余额行经乐观锁串行化，**无 silent split-quantity corruption**。

---

## 4. dedicated 跨工单并发探针（Add，Phase 1 item 3，无条件新增）

### 4.1 探针设计（`TestErpMfgReservationLifecycle#testConcurrentCrossWorkOrderApproveNoLostUpdate`）

**seed 约束（镜像 `TestErpMfgReservationLifecycle` seed 范式）**：`seedBase(9111L, "WO-RSV-CONC-A", "2")`（P + M1 + BOM P→M1×2）+ `generateIncoming(M1, 10)` → 可用 10；两工单 `WO-RSV-CONC-A`/`WO-RSV-CONC-B`（woId 8690/8689，无碰撞），各含 INPUT 行 M1 planned 2（sourceWarehouse=WAREHOUSE_ID）+ OUTPUT 行 P。BOM 展开需求 = BOM qty 2 × planned 2 = **每工单 4，两工单合计 8**（镜像写 API 层 4+4=8 数值口径）。

**探针流程**（对齐 `testConcurrentCreateReservationNoLostUpdate` 线程框架 + mfg 集成层语义）：

1. `ExecutorService(2)` + `CountDownLatch(1)` 起步栅栏 + `CountDownLatch(2)` 完成栅栏 + `AtomicReference<Throwable> firstError`。
2. 每 worker：`ContextProvider.newContext()` → `startGate.await()` → 经 **GraphQL 引擎**调 `ErpMfgWorkOrder__submitForApproval` + `ErpMfgWorkOrder__approve`（各自工单）→ 断言 `resp.getStatus() == 0`（失败即抛 AssertionError 入 firstError）→ finally `detachContext()` + `doneLatch.countDown()`。
3. 主线程：`doneLatch.await(60, SECONDS)` + `firstError` 非空即抛。
4. 终态断言：两工单预留头均落库（`findReservation` per WO code）+ 各 1 行 `reservedQuantity == 4`（min(需求 4, 可用)）+ 余额 `total == 10`（守恒）+ `reservedQuantity == 8`（**4 + 4 = 8 无丢失**）+ `availableQuantity == 2`（**available = total − reserved 恒等式**）。

**断言可区分性**：无 lost-update 防护时并发双写会丢失一写（如最终 reserved=4 或 6）——断言 reserved==8 可区分「防护成立」与「丢失更新」两种结局；worker 内 status!=0 断言捕获重试耗尽（`ERR_INV_CONCURRENT_DEDUCT_CONFLICT` 会以非零 status 返回）。

### 4.2 探针结果

- **探针测试 PASS**：`TestErpMfgReservationLifecycle` 10/10 全绿（`Tests run: 10, Failures: 0, Errors: 0`，2026-08-16 复跑）。
- 两工单预留均落库：`resA`（WO-RSV-CONC-A）行 `reservedQuantity=4` + `resB`（WO-RSV-CONC-B）行 `reservedQuantity=4`——**双工单并发 approve 各自成功建预留，零丢失**。
- 余额守恒：`total=10` 不变 + `reservedQuantity=8`（4+4）+ `availableQuantity=2`——**available = total − reserved 恒等式在跨工单并发双写后保持**。
- 无异常/无重试耗尽：worker `firstError == null`（submitForApproval/approve 全 status=0，无 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT` 返回）——冲突经乐观锁重试（max-retry 5 内）串行化解，未耗尽。
- 按计划 Exit Criteria 预期行为实现——**未升级 finding**（若探针暴露丢失更新或重试耗尽则按 methodology 判据处置；实仓未发生）。

---

## 5. 与既有 finding 衔接（去重声明）

| Finding ID | 域 | 本审计结论 | 维持/变更 |
|-----------|---|--------------|----------|
| `P1-RC-008`（UC-MFG-05/08/06 预留写路径） | mfg（+inv 跨域） | 本审计确认修复落地后运行时一致性成立：跨工单并发预留 lost-update 防护（写 API 层既有断言复跑 + mfg 集成层新探针双实证）+ reserved/available 一致性恒等式成立。 | **维持 done（RC-R1.48）**——本审计只做落地后运行时一致性验证，**不重新裁决/不撤销分级**（plan Non-Goals「不重新裁决/不撤销 P1-RC-008 分级」） |
| `P0-MA2-020`（StockBalance 自然键 UK） | inventory | 本审计并发防护机制依赖该 UK 冲突捕获路径（`flushAndCheckConflict`），与 A4.2.1 结论一致，无增量。 | 维持 resolved |
| A2.17（真并发 check-then-act over-commitment 窗口） | inventory | 本审计探针验证的是**预留写路径** lost-update 防护（写串行化），非 read-time over-commitment 窗口——后者仍归 A2.17 既有追踪（R1.48 plan Deferred 登记），本计划不覆盖（Non-Goals）。 | 维持（不重复登记） |
| `A4.2.119`（P1-RC-049 物料归集） | mfg | 本审计不覆盖——不同控制点，仍 MR1-blocked todo。 | 维持 todo |
| `P1-MA4-007`/`P1-MA4-009` 等 | mfg | 本审计非这些维度，无增量。 | 维持 |

**无新 finding 新建**（0 新 finding，全部衔接既有分级）。**A4.2.3 与 A4.2.1（预留并发扣减运行时安全确认，本行修复前基线）为前后关系**——本行消费其结论（updateBalanceWithRetry 串行化）+ 新基线（写路径落地后跨工单语义）；与 A4.2.79（批次效期拦截 reserved 一致性）不同控制点。运行时未发现活跃数据破坏，**不触发 MR0**。

---

## 6. 裁决摘要

| 工作项 | 存疑点 | 运行时裁决 |
|--------|--------|-----------|
| A4.2.3 | SP-3（A1.8+A1.9 合并）MR1 P1-RC-008 修复落地后 reservedQty/availableQuantity 实时一致性 + 跨工单并发预留 lost-update 防护 | **成立，0 新 finding**——跨工单并发预留 lost-update 防护双实证（写 API 层 `testConcurrentCreateReservationNoLostUpdate` 4+4=8 复跑 + mfg 集成层新增探针两工单并发 approve 4+4=8 无丢失 + available=2）；reserved/available 一致性恒等式运行时成立（创建/释放/消耗断言链 + 探针守恒断言）；防护机制 = `updateBalanceWithRetry` versionProp 乐观锁 + UK 冲突 evict+reload+重试（max-retry 5），与 A4.2.1 前置结论对照无回退 |

**整体裁决**：A4.2.3 运行时确认完成。**0 新 finding / 0 翻转 / 不触发 MR0 / 不归 MR1（本审计）**。P1-RC-008 修复（RC-R1.48）落地后的运行时义务——跨工单并发预留 lost-update 防护（含 mfg 集成层跨工单语义）+ reserved/available 实时一致性——经既有断言链复跑 + 无条件新增探针双实证成立。A4.2.3 在 MA4/R1.0 链路的运行时证据缺口**闭合**。

---

## 7. 过程纪律自检

- [x] **checker 门控核查**：本审计零生产代码变更（仅测试探针新增，测试类目限 erp-mfg-service），checker 无回归风险。本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`——**actual == baseline**（与 `compliance-baseline.md` §BASELINE machine-readable 块逐行一致，0 漂移；探针零新增 daoFor/import 生产面）。checker 脚本为纯 reporter 退出码恒 0，真正门控在 CI workflow 解析 actual > baseline => sys.exit(1)；本报告以 actual == baseline 作为零漂移依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本审计全部结论衔接既有 finding（P1-RC-008 维持 done / P0-MA2-020 + A2.17 维持 / A4.2.119 不覆盖），无未经比对直接新建的 finding。A4.2.3 运行时注记已追加至 arm-index P1-RC-008 行。
- [x] **业财保护区域探针纪律**：探针仅 seed 物料/BOM/库存余额/两工单 + 并发 approve 断言——**READ-ONLY 不改生产代码/ORM/api.xml/view.xml/config 默认值/真相源**；不触及 InvPostingDispatcher/过账逻辑（预留写经既有 `updateBalanceWithRetry` 入口，与 RC-R1.48 同路径）。
- [x] **分层与去重协议**：A4.2.3 与 A4.2.1/2（修复前基线，前后关系）消费其结论 + 新基线；与 A4.2.79（批次效期拦截）不同控制点；不重复核实 P1-RC-008 分级（RC-R1.48 已落地，无分级裁决义务）。

---

## 8. 完整性自检

- [x] 存疑点原文摘录（A1.8 + A1.9 SP-3 合并，已标注）
- [x] 既有测试复跑证据（模块全绿 218+270 + 并发断言链 + 余额断言链表 + mfg 生命周期复跑）
- [x] 并发防护机制运行时确认（updateBalanceWithRetry :256-328 机制实录 + 与 A4.2.1 对照）
- [x] 测试证据（无条件新增跨工单探针 + 断言可区分性 + 探针结果）
- [x] §2 判据（0 新 finding，维持既有分级，去重声明）
- [x] 过程纪律自检（checker 门控核查 + closure-audit 独立性声明 + 交叉去重 + 业财保护区域探针纪律）
