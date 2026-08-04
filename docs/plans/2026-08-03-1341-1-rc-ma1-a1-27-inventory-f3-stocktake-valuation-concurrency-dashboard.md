# 2026-08-03-1341-1 rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard inventory-F3 盘点/估值/并发/看板需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.27（MA1 需求追踪矩阵审计 — inventory-F3 盘点/估值/并发/看板）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.27
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.27 的 0.2 依赖）、`2026-08-03-1200-2-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability.md`（A1.25 done）+ `2026-08-03-1200-3-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md`（A1.26 done，同 inventory 域，先 F1/F2 后 F3 收尾 inventory）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.27 给出 UC 清单 = `UC-INV-07/08/10/11`（4 UC），含 `use-cases.md:122/:140/:171/:193` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/inventory/use-cases.md`（机制见 `state-machine.md` §盘点单状态机 / §4 异常路径 / §7，`cross-domain.md` §与财务域协作，`dashboards.md` §3 库存看板）：
  - UC-INV-07 盘点差异生成移动单（`:122`）：盘点单.确认 → 计算差异（实盘 − 账面）；差异>0 生成盘盈移动单(incoming)；差异<0 生成盘亏移动单(outgoing)；盘点单本身不改余额；盘盈/盘亏移动单走 DRAFT→DONE 后才影响余额。
  - UC-INV-08 并发扣减乐观锁（`:140`）：并发挥同一批次扣减 → 一个 DONE 成功、另一个乐观锁冲突 → 重试或失败；最终 现有量 == 初始−A−B（不出现负数，除非允许负库存）。
  - UC-INV-10 移动单触发存货估值凭证（`:171`）：移动单.DONE → 发布事件(post-commit 异步) → 生成存货估值凭证(STOCK_MOVE 业务类型)；凭证金额 = 单位成本×数量；入库 借存货/贷暂估应付(GR/IR)；出库 借销售成本/贷存货。
  - UC-INV-11 库存看板（`:193`）：KPI 卡片值 == 实时聚合（非硬编码，按期间/orgId/权限过滤）；预警项 == 满足阈值条件的记录（阈值来自系统配置非硬编码）；看板数据受行级权限约束（只看自己组织/部门/成本中心）。

- **L3 代码实现现状（实测）**——UC-INV-08/10/11 主路径已实现，UC-INV-07 为 stub（最高风险缺口），UC-INV-11 行级权限由全局 transformer 覆盖（运行时有效性为 MA4 存疑点）：
  - **UC-INV-07 盘点差异生成移动单**（⚠️ STUB / documented simplification）：`ErpInvStockTakeBizModel.java:40-50 completeTake` **仅** `take.setDocStatus(ErpInvConstants.DOC_STATUS_DONE)` + updateEntity。**缺失**：无 差异计算（qtyActual − totalQuantity）、无 `IErpInvStockMoveBiz.generateMove` 调用、无盘盈/盘亏移动单生成。grep `module-inventory/erp-inv-service/src/main/` for `generateGain|generateLoss|gainMove|lossMove` = **0 命中**。owner doc `state-machine.md:159-162` 标记为 Deferred（"差异调整移动单的自动生成 = Deferred，当前经库管员手工 generateMove 处置"）。既有 finding **P1-MA2-062**（`arm-index.md:328`）已以**方案 B**（R1.19 owner doc Deferred 标注）关闭。**RC 视角必须复核 §4 三判据**：(i) R1.19 plan-audit 是否独立子代理通过（非执行者自审）？(ii) owner doc Deferred 标注是否经**人工批准**（AI 落地补注不算，对齐 `ai-autonomy-policy.md`）？(iii) product-scope 是否将自动生成裁剪出范围？三判据均不满足 → 按 Q4=(a) **重开为 P1-RC-xxx**（与 A1.26 P1-RC-031 UC-INV-06 效期拦截同型重开逻辑）。修复属**代码逻辑**类（预授权——复用既有 `IErpInvStockMoveBiz.generateMove` Facade + 既有 StockTakeLine.qtyActual/StockBalance.totalQuantity 字段，不涉及 ORM 结构变更）。
  - **UC-INV-08 并发扣减乐观锁**（✅ 已实现 & 强）：`StockMoveBookkeeper.java:255-328 updateBalanceWithRetry`（MANAGED 路径 `dao.tryUpdateWithVersionCheck:271` 乐观锁 + SAVING/TRANSIENT 路径 INSERT-UK-conflict 重试 `:272-282`；重试上限 `erp-inv.concurrent-deduct-max-retry` 默认 5 `ErpInvConstants`；耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`/`ERR_INV_BALANCE_INSERT_CONFLICT` `:406-417`）；version 列 `ErpInvStockBalance.version`（ORM `versionProp`）；UK 兜底 `UK_INV_STOCK_BALANCE_NATURAL`（P0-MA2-020 done 方案 A）；可观测 `ErpInvConcurrencyMetrics`。
  - **UC-INV-10 移动单触发存货估值凭证**（✅ 已实现，businessType 命名漂移）：`ErpInvStockMoveProcessor.java:113 doComplete` 在 bookkeeper + setDocStatus(DONE) 后调 `postingDispatcher.dispatchIfApplicable(move,lines)`；`InvPostingDispatcher.java:57-80 dispatchIfApplicable`（`resolveBusinessType:152-179` 映射 moveType → PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT，skip internal-transfer + pur/sal-return 等）；`InvAcctDocProvider.java:64-88 createFacts`（PURCHASE_INPUT 借1401库存/贷2202暂估应付；SALES_OUTPUT 借6401主营业务成本/贷1401库存；金额来自 `PostingEvent.billData.TOTAL_COST`）；跨域 inventory→finance 经 `InvPostingExecutor.postEvent`→`IErpFinVoucherBiz.post`(REQUIRES_NEW)；失败隔离 try/catch `:64-76` 留 posted=false（DeferredPostingSweepJob 兜底）。**漂移**：①命名漂移（L1 businessType=`STOCK_MOVE` vs 实现 `PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT`）——已登记 P2-RC-016（SALES_DELIVERY↔SALES_OUTPUT watch-only）+ P2-RC-011（GOODS_RECEIPT↔PURCHASE_INPUT watch-only）；②时序漂移（L1 "post-commit 异步" vs 实现同步在 doComplete 内 + REQUIRES_NEW 隔离——行为等价，失败隔离成立）。
  - **UC-INV-11 库存看板**（⚠️ 主路径强；行级权限由全局 transformer 覆盖，运行时有效性为 MA4 存疑点）：`ErpInvDashboardBizModel.java`（`@BizQuery` 实时聚合非硬编码）：`getDashboardKpi:69-100`（totalValue 经 `sumBalanceTotalCost:444-456` DB SUM；出入库量 `sumMoveQtyInRange:296-322`；turnoverRate=outgoingCost/avgInventory）、`getDashboardTrend:102-129`、`findWarehouseDistribution:132-164`、`findShortageAlert:167-198`（阈值取自 master-data `material.safetyStock` 非配置项——边界）、`findSlowMovingAlert:203-245`（config `CONFIG_DASH_INV_SLOW_MOVING_DAYS`）、`findBatchExpiryAlert:250-291`（config `CONFIG_DASH_INV_BATCH_EXPIRY_DAYS`）。**行级权限（L1 `:207`）现状**：所有 dashboard QueryBean **无显式 orgId/createdById/assigneeId filter**（`sumBalanceTotalCost`/`loadDoneMovesInRange` 无内联 scope）。但跨域既有 finding **P1-MA2-093 已 resolved（R1.29 done，2026-07-31）**——经注入全局 `ErpOrgIsolationQueryTransformer` 在查询管道层强制 orgId scope（非各 BizModel 内联 filter）。**RC 视角**：L1 验收标准"看板数据受行级权限约束"的满足性取决于该全局 transformer 对 dashboard 直连路径（`daoProvider.findAllByQuery`/`ormTemplate.findListByQuery`）的运行时覆盖有效性——此为**静态存疑点交 MA4 展开**（对齐 sibling 先例：A1.7 UC-FIN-17 SP-4 / A1.11 UC-MFG-11 SP-3 / A1.21 UC-SAL-12 SP-3 / A1.24 UC-AST-12③ 均**复用 P1-MA2-093** 不新建 finding，登记运行时有效性存疑点）。本切片应遵循同一先例：**复用 P1-MA2-093**（同根因同控制点）+ 登记运行时覆盖存疑点，不新建 P1-RC-xxx。

- **L4 测试证据现状**（`module-inventory/erp-inv-service/src/test/`）：UC-INV-08 `TestErpInvConcurrentDeduct.java`（6 @Test：version-skew 重试成功/重试耗尽抛错/3 线程真实 ExecutorService+CountDownLatch 防超卖/负库存下并发/UK 冲突单线程重试/多线程无重复行——**强断言**，含 ledger 行计数）；UC-INV-10 `TestErpInvPosting.java`（3 @Test：DONE 生凭证+posted+totalDebit/Credit 平衡+2 行 / internal-transfer 不过账 / 失败留 DONE+posted=false——**强**，但未断言行级 subjectCode/DC 方向）；UC-INV-11 `TestErpInvDashboard.java`（8 @Test：空集/totalValue+turnover/warehouse 分布/缺料/滞销禁用+触发/批次效期禁用+触发）+ E2E `tests/e2e/dashboards/inventory.value.spec.ts`（断言 `{totalValue:10450,incomingQty:100,outgoingQty:0,turnoverRate:0}`——**强**，硬编码值会失败）；**⚠️ UC-INV-07 零 dedicated 测试**（无 `TestErpInvStockTake*` 文件，grep `StockTake|completeTake` 全仓零命中）；**⚠️ UC-INV-11 零多组织行级权限运行时测试**（全局 transformer 覆盖有效性无 dedicated 运行时测试——交 MA4 存疑点）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`（A2.11）：UC-INV-08 并发扣减乐观锁 + UK 兜底 PASS（P0-MA2-020 done 方案 A）；**P1-MA2-062**（StockTake completeTake 未自动生成盘盈/盘亏移动单）resolved R1.19 方案 B（owner doc Deferred）——RC 须复核 §4 三判据。
  - `docs/audits/2026-07-28-0400-arm-ma2-concurrency-optimistic-lock.md`：inventory 并发扣减 sustained（PASS），`:100/:230`。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）：`ErpInvStockMoveProcessor` 代码质量 PASS。
  - `docs/audits/...-arm-ma6-...`（A6.3 data-permission）：**P1-MA2-093 已 resolved R1.29**（全局 `ErpOrgIsolationQueryTransformer` 注入，2026-07-31 done）——UC-INV-11 行级权限由全局 transformer 覆盖；dashboard 直连路径运行时覆盖有效性 = MA4 存疑点（sibling 先例 A1.7/A1.11/A1.21/A1.24 复用）。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为（UC-INV-08 并发 PASS / UC-INV-10 posting 链路 PASS / UC-INV-11 行级权限经全局 transformer resolved R1.29），只补"需求契约↔行为"差异（UC-INV-07 自动生成 stub / UC-INV-10 命名+时序漂移 / UC-INV-11 dashboard 直连路径运行时覆盖有效性存疑点）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-062`（StockTake completeTake，`:328`，UC-INV-07 直接相关，resolved 方案 B 待 RC 复核）、`P0-MA2-020`（StockBalance UK，`:228`，UC-INV-08 支撑 done 方案 A）、`P1-MA2-063`（PickingOrder 死状态，`:329`，非本切片实体）、`P1-MA2-085`（LandedCost TOCTOU，`:352`，非本切片）、`P1-MA2-093`（dashboard 直连绕 auth，A2.18+A6.3，UC-INV-11 行级权限，**resolved R1.29 全局 transformer**）、`P2-RC-011`/`P2-RC-016`（businessType 命名漂移 watch-only，UC-INV-10）、`P2-MA3-034`（StockTake COUNTING vs CONFIRMED doc 漂移 watch-only）、`P1-MA4-001 family`（posted=false 悬挂，UC-INV-10 投影）。**RC 系列对 UC-INV-07/08/10/11 为零**（P1-RC-031 属 UC-INV-06 非本切片）。本切片须 grep arm-index inventory 盘点/并发/估值/看板同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1（R1.0 展开 RC-R1.n）。UC-INV-07 自动生成修复属**代码逻辑**类（预授权——复用既有 `IErpInvStockMoveBiz.generateMove` Facade + 既有字段，不涉及 ORM 结构变更）。须人工确认 product-scope 是否要求盘点自动生成（若 L1 明确要求则 P1 强制实现）。UC-INV-11 行级权限已由 P1-MA2-093 R1.29 全局 transformer resolved，本切片仅登记 dashboard 直连路径运行时覆盖存疑点（非新修复项）。

- **剩余差距**：A1.27 切片五级追踪审计报告缺失 = MA4（A4.1 业财展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源，且 UC-INV-07 自动生成 stub 是潜在合规风险（盘点差异无自动移动单致账实长期不符）。本计划产出 A1.27 报告并登记 finding，解除其链路证据缺口。

## Goals

- 产出 A1.27 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard.md`，含方法论 §6 **9 段全部内容**。
- 对 4 UC（UC-INV-07/08/10/11）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并、禁止跳号。
- 对候选缺口给出分级结论：#1 UC-INV-07 completeTake stub（L1 `:122` 要求确认后生成盘盈/盘亏移动单——`completeTake:40-50` 仅 setDocStatus(DONE) 无 diff 无 generateMove；既有 P1-MA2-062 方案 B 关闭——倾向 **P1**（须按 §4 三判据复核 R1.19 关闭合法性，三判据不满足则重开，与 A1.26 P1-RC-031 同型））、#2 UC-INV-07 测试完全缺失（无 TestErpInvStockTake*）、#3 UC-INV-08 并发（已实现 PASS——复核乐观锁+UK+重试+负库存语义）、#4 UC-INV-10 命名漂移（已登记 P2-RC-011/016 watch-only，复核）、#5 UC-INV-10 时序漂移（同步 doComplete vs L1 post-commit 异步——行为等价复核）、#6 UC-INV-10 行级凭证断言缺失、#7 UC-INV-11 行级权限（L1 `:207`——QueryBean 无显式 orgId filter，但全局 `ErpOrgIsolationQueryTransformer` R1.29 已覆盖；倾向**复用 P1-MA2-093 + 登记运行时覆盖存疑点交 MA4**，遵循 sibling 先例 A1.7/A1.11/A1.21/A1.24，不新建 P1-RC-xxx）、#8 UC-INV-11 缺料阈值取 master-data 而非 config（边界 P2）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/state-machine.md/cross-domain.md/dashboards.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.25/A1.26 done；A1.27 只覆盖 UC-INV-07/08/10/11）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：UC-INV-08 并发由 A2.11/concurrency 报告证实，只补需求视角差异）。
- **不复审 UC-INV-06 效期拦截**（P1-RC-031 属 A1.26，已登记 MR1 successor，非本切片）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议，**§4 三判据为本切片 UC-INV-07 复核 P1-MA2-062 方案 B 关闭合法性的关键**）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.27 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.27 UC 锚点）+ `docs/design/inventory/use-cases.md`（L1 真相源）+ `docs/design/inventory/state-machine.md`（L2 §盘点单状态机 / §4 异常路径，非真相源）+ `docs/design/inventory/cross-domain.md`（L2 §与财务域协作）+ `docs/design/dashboards.md`（L2 §3 库存看板）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/A4/A6 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测/E2E；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-inventory/erp-inv-service -Dtest=TestErpInvConcurrentDeduct,TestErpInvPosting,TestErpInvDashboard`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-05-0900-rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-INV-07/08/10/11 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:122/:140/:171/:193` 验收标准原文；L2 引用 `state-machine.md` §盘点单状态机/§4 异常路径/§7、`cross-domain.md` §与财务域协作、`dashboards.md` §3（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpInvStockTakeBizModel.java`/`StockMoveBookkeeper.java`/`ErpInvStockMoveProcessor.java`/`InvPostingDispatcher.java`/`InvAcctDocProvider.java`/`ErpInvDashboardBizModel.java`/`ErpInvConstants.java`（含行号）；L4 引用 `TestErpInvConcurrentDeduct.java#method`/`TestErpInvPosting.java#method`/`TestErpInvDashboard.java#method`（注明断言强度）；L5 复用 A2.11/concurrency/A4.5/A6.3 + E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-INV-07 completeTake stub（`completeTake:40-50` 仅 setDocStatus(DONE) 无 diff 无 generateMove，#1 最高风险）；②#2 UC-INV-07 零 dedicated 测试；③#3 UC-INV-08 乐观锁+UK+重试+负库存语义（`updateBalanceWithRetry:255-328` + tryUpdateWithVersionCheck + UK 重试 + max-retry——复核 L1 "不出现负数除非允许"语义）；④UC-INV-10 posting 链路（`doComplete:113`→dispatchIfApplicable→createFacts，复核 L1 借贷方向+金额公式）；⑤#4 UC-INV-10 命名漂移（已登记 P2-RC-011/016）；⑥#5 UC-INV-10 时序漂移（同步 vs post-commit 异步——行为等价复核）；⑦#6 UC-INV-10 行级凭证断言缺失；⑧#7 UC-INV-11 行级权限（L1 `:207`——QueryBean 无显式 orgId filter，但全局 `ErpOrgIsolationQueryTransformer`（P1-MA2-093 R1.29 done）覆盖；复核 dashboard 直连路径运行时覆盖有效性 → 遵循 sibling 先例复用 P1-MA2-093 + 存疑点交 MA4，不新建 finding）；⑨#8 UC-INV-11 缺料阈值取 `material.safetyStock` 非 config（边界复核）；⑩UC-INV-11 KPI 实时聚合非硬编码（已实现复核）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：#1 UC-INV-07 completeTake stub 属"功能完全缺失"（§2 P1①）——倾向 **P1**（须先按 §4 三判据复核 P1-MA2-062 方案 B 关闭合法性：R1.19 plan-audit 是否独立、owner doc Deferred 是否人工批准、product-scope 是否裁剪；三判据不满足则重开为 P1-RC-xxx，与 A1.26 P1-RC-031 同型）；#7 UC-INV-11 行级权限——P1-MA2-093 **已 resolved R1.29**（全局 transformer），L1 验收标准满足性取决于 dashboard 直连路径运行时覆盖有效性，倾向**复用 P1-MA2-093 + 登记存疑点交 MA4**（遵循 sibling 先例 A1.7/A1.11/A1.21/A1.24，不新建 P1-RC-xxx）；#3 UC-INV-08 已实现则接受；#4/#5/#6/#8 P2/watch-only 或接受。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-INV-07/08/10/11 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.11/concurrency/A4.5/A6.3 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#8 有明确分级（非悬空"待查"）；#1 completeTake stub 有明确 P1 倾向 + §4 三判据复核路径；#7 行级权限有明确"复用 P1-MA2-093（resolved R1.29）+ 存疑点交 MA4"路径

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-05-0900-rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` inventory 盘点/并发/估值/看板同域同控制点后裁决——#1 UC-INV-07 vs P1-MA2-062（§4 三判据复核：方案 B 关闭是否合法，不合法则重开 P1-RC-xxx 列明差异依据）；#7 UC-INV-11 vs P1-MA2-093（P1-MA2-093 **resolved R1.29** 全局 transformer——遵循 sibling 先例 A1.7/A1.11/A1.21/A1.24 **复用注记 + 登记运行时覆盖存疑点交 MA4**，不新建 P1-RC-xxx）；UC-INV-08/10 已证实→复用注记。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 completeTake 置 DONE 后手工 generateMove 的实际余额影响、UC-INV-08 高并发下 max-retry 耗尽后的余额一致性、UC-INV-10 posting 失败留 posted=false 时 DeferredPostingSweepJob 兜底实际触发、**UC-INV-11 全局 ErpOrgIsolationQueryTransformer 对 dashboard 直连路径（daoProvider.findAllByQuery/ormTemplate.findListByQuery）的运行时覆盖有效性**等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-0400-arm-ma2-inventory-state-machine.md`（A2.11 UC-INV-08 并发 PASS + P1-MA2-062 方案 B 关闭）+ concurrency 报告（inventory 并发 sustained）+ A4.5（代码质量 PASS）+ A6.3/R1.29（P1-MA2-093 dashboard 行级权限 resolved 全局 transformer），列明只补的需求视角差异（UC-INV-07 自动生成 stub / UC-INV-10 命名+时序漂移 / UC-INV-11 dashboard 直连路径运行时覆盖存疑点）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_039d74bc5ffeVrJk7d8rlBDVBv，fresh session，未起草本计划）。1 阻塞（Rule 1 诚实 live-repo baseline）：Current Baseline 称 P1-MA2-093 "**todo MR1**"，但实测 `arm-index.md:360` 为 `✅ resolved (R1.29 done, 2026-07-31)`——经注入全局 `ErpOrgIsolationQueryTransformer` 在查询管道层强制 orgId scope（早于本计划起草）。同型于 A1.26 iter1（虚假字段声明）。后果：#7 UC-INV-11 行级权限被误框定为"P1 候选缺口（缺失）"，实际应由全局 transformer 覆盖，运行时覆盖有效性为 MA4 存疑点（sibling 先例 A1.7 UC-FIN-17 SP-4 / A1.11 UC-MFG-11 SP-3 / A1.21 UC-SAL-12 SP-3 / A1.24 UC-AST-12③ 均**复用 P1-MA2-093** 不新建 finding）。其余 live-baseline 声明（completeTake stub:40-50 / updateBalanceWithRetry:255-328 / InvPostingDispatcher / ErpInvDashboardBizModel 无显式 filter / 测试计数 / L1 逐字引用）均实测 CONFIRMED。
- Independent draft review iteration 2: `accept`（同独立子代理 ses_039d74bc5ffeVrJk7d8rlBDVBv 复核）。修订全部验证通过：①Current Baseline UC-INV-11 段改述 P1-MA2-093 resolved R1.29（全局 transformer）+ 运行时覆盖有效性为 MA4 存疑点；②#7 全链路（Goals / Phase1 ⑧ / Phase1 Decision / Exit Criteria / Phase2 §7 / §9 / Deferred / Follow-up）一致改为"复用 P1-MA2-093 + 存疑点交 MA4，不新建 P1-RC-xxx"，遵循 sibling 先例；③保护区域/剩余差距移除"UC-INV-11 行级权限修复属代码逻辑类"（已 resolved 非新修复项）；④静态存疑点清单加入"全局 transformer 对 dashboard 直连路径运行时覆盖有效性"。无新问题。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.27 报告 9 段齐全 + 4 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.27 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；UC-INV-07 自动生成修复属**代码逻辑**类（预授权——复用既有 Facade/字段，不涉及 ORM 结构变更）。#1 UC-INV-07 须人工确认 product-scope 是否要求盘点自动生成（若裁剪→§4(iii) 改真相源非降级；若未裁剪→P1 强制实现）。UC-INV-11 行级权限已由 P1-MA2-093 R1.29 resolved（非新修复项）；其 dashboard 直连路径运行时覆盖存疑点交 MA4（非 MR1 修复项）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；#1 待人工确认 product-scope 范围；UC-INV-11 存疑点交 MA4）

## Closure

Status Note: 只读审计执行完成。产出报告 `docs/audits/2026-08-05-0900-rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard.md`（9 段齐全 + 4 UC 五级矩阵 + 8 候选缺口分级）+ arm-index 更新（P2-RC-029/P2-RC-030 新建 + P1-MA2-062 RC 视角 §4 三判据复核注记 + A1.27 报告清单行）。整体裁决：零 P0、0 新 P1（#1 UC-INV-07 reuse P1-MA2-062 §4 三判据复核倾向重开须人工确认 product-scope，§7 同根因同控制点不新建编号，对齐 A1.24 P1-MA2-061 复用先例）、2 新 P2（P2-RC-029 UC-INV-10 JUnit 凭证行级断言 / P2-RC-030 UC-INV-11 缺料阈值 derivation）、3 reuse（P1-MA2-062 / P1-MA2-093 resolved R1.29 / P2-RC-011+P2-RC-016）；UC-INV-08 接受、UC-INV-10 接受含 caveat、UC-INV-11 接受 on ⑩+⑪滞销/批次效期 + ⑫reuse+存疑点交 MA4 + ⑬new P2。零生产代码变更（git status 仅 docs/audits/+docs/plans/ 变更），故无 build/test 回归风险；§8 checker actual vs baseline 实测（R2c/R2d +2 漂移为 pre-existing HEAD 状态非本审计所致），不以 checker 退出码 0 为门控通过依据。4 项静态存疑点交 MA4 A4.1 运行时展开。#1 UC-INV-07 须人工确认 product-scope 是否裁剪盘点自动生成（涉账实一致合规风险）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计由独立子代理（新会话 `ses_03103bccaffe4A4gyPZ4Yr7RC9`，fresh session，未执行本计划）执行。**Verdict: pass**（10/10 checks PASS，无 blocker）。关键独立验证：(1) 报告 9 段齐全 + §5 矩阵 4 UC 行无合并；(2) L1 逐字引用 anti-Q1；(3) L3 行号实测核对（`ErpInvStockTakeBizModel.java:40-50` STUB / `StockMoveBookkeeper.java:256/271` / `InvPostingDispatcher.java:57/152` / `InvAcctDocProvider.java:64/113`）；(4) §6.2 §4 三判据复核 R1.19 hollow closure **独立证实**（R1.19 plan:122 自承「执行代理（本会话）」）+ reuse P1-MA2-062 不新建遵循 A1.24 先例；(5) §6.1 grep-based 裁决表无未比对新建；(6) arm-index P2-RC-029/030 无编号碰撞（max existing=028）；(7) §8 checker 表 + 纯 reporter 声明 + 零生产代码变更；(8) §9 真相源冻结——git status 仅 docs/audits/+docs/plans/ 变更，未触 docs/design//docs/requirements//module-*；(9) 计划内部一致性——Plan Status completed / 双 Phase completed / 全 `[x]` 无遗留 `[ ]` / `> Source Audits:` 行缺失（roadmap-sourced 计划，source-audit-closing 步骤不适用）。结束审计门控（plan Closure Gates line 139）由本独立裁决满足。

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围
- #1 UC-INV-07 盘点自动生成须人工确认 product-scope 是否要求（涉账实一致合规风险）
- UC-INV-11 行级权限 dashboard 直连路径运行时覆盖存疑点交 MA4（P1-MA2-093 已 resolved R1.29）
