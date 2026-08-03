# 2026-08-03-1200-3 rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock inventory-F2 批次/可用量/负库存需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.26（MA1 需求追踪矩阵审计 — inventory-F2 批次与可用量）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.26
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.26 的 0.2 依赖）、`2026-08-03-1200-2-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability.md`（同批次 inventory 域，先 F1 主链后 F2 批次/可用量）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.26 给出 UC 清单 = `UC-INV-02/06/09`（3 UC），含 `use-cases.md:37/:106/:155` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/inventory/use-cases.md`（机制见 `docs/design/inventory/state-machine.md` §4、`trace-chain.md` §追溯链与批次、`cross-domain.md` §余量校验）：
  - UC-INV-02 销售出库可用量不足拒绝（`:37`）：校验维度 = 物料×仓库×库位×批次；若 可用量 < 出库数量：generateMove(outgoing) 拒绝；销售订单审核回滚（approveStatus 保持 SUBMITTED）；库存余额不变。可用量 = 现有量 − 预留量（见 cross-domain §余量校验）。负库存配置 erp-inv.allow-negative-stock 决定是否放行。
  - UC-INV-06 批次追溯与效期拦截（`:106`）：领料移动单.批次 == 入库移动单.批次（批次继承）；若 批次.效期 < 当前日期 且 物料.批次管控 == 强制：出库移动单确认失败（批次过期拦截）；完工入库生成新批次（不继承）。
  - UC-INV-09 负库存放行（`:155`）：配置 erp-inv.allow-negative-stock == true：出库时 可用量 < 出库数量 → 放行（现有量变负）；后续入库移动单补回；若 配置 == false：拒绝（回到 UC-INV-02）。

- **L3 代码实现现状（实测）**——2/3 UC 完全实现，UC-INV-06 批次追溯 ✅ 但效期拦截 ❌ 缺失（最高风险缺口）：
  - **UC-INV-02 销售出库可用量不足拒绝**（✅ 已实现）：入口 `ErpInvStockMoveBizModel.java:56-60 generateMove`（sales `ErpSalDeliveryProcessor.java:244` 调用）；校验触发 `ErpInvStockMoveProcessor.java:94 doConfirm` 调 `validateAvailable(move,lines,context)` 前 status flip；`validateAvailable:116-136`（`:117-119` 短路 `if(isNegativeStockAllowed()) return;`；`:120-122` skip if not `reservesOnConfirm(moveType)`（仅 OUTGOING+INTERNAL_TRANSFER）；per-line `available < required` → 抛 `NopException(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT)` with `ARG_MATERIAL_ID/WAREHOUSE_ID/AVAILABLE/REQUIRED` params `:128-134`）；`reservesOnConfirm:242-248`（MOVE_TYPE_OUTGOING or MOVE_TYPE_INTERNAL_TRANSFER）；`availableQuantity:278-283`（= totalQuantity − reservedQuantity − lockedQuantity）；`@BizMutation` on generateMove（`IErpInvStockMoveBiz.java:31`）原子事务——拒绝回滚。跨域 sales `ErpSalDeliveryProcessor.java:244 stockMoveBiz.generateMove` → validateAvailable 失败 → exception bubble up → `@BizMutation` 事务回滚 → sales delivery approval 原子失败。
  - **UC-INV-06 批次追溯与效期拦截**（⚠️ PARTIAL — 批次追溯 ✅；效期拦截 ❌ 缺失）：
    - **批次追溯 ✅ 已实现**：`ErpInvStockMoveBizModel.java:112-116 batchTrace`（@BizQuery）；`TraceChainQuery.java:168-192 batchTrace`（聚合 moveId from `findLinesByBatch`（ErpInvStockMoveLine.batchNo）+ `findLedgersByBatch`（ErpInvStockLedger.batchNo），filter delVersion=0L `:224`）；ORM `app-erp-inventory.orm.xml` `ErpInvStockMoveLine.batchNo`（set in `ErpInvStockMoveProcessor.newLines:202`）；`ErpInvStockLedger.batchNo` written in `StockMoveBookkeeper.writeLedger:217`；balance dimension keyed by `(orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId)`（`StockMoveBookkeeper.upsertBalance:148-159`）——batch 是余额维度。
    - **效期拦截 ❌ 缺失**（最高风险缺口）：`ErpInvBatch.expiryDate` 字段 ✅ exists（`app-erp-inventory.orm.xml:908 EXPIRY_DATE`，`_ErpInvBatch.java:60-66 PROP_ID_expiryDate=10`，`:211-212 private _expiryDate`）；`ErpInvBatch.shelfLifeDays` ✅ exists（`:909 SHELF_LIFE_DAYS`，`_ErpInvBatch.java:65-66`）；`EXPIRED` dict status ✅ exists（`:56,:71 option code="EXPIRED"` in batch-status/reservation-status dicts，`_ErpInvDaoConstants.java:69 BATCH_STATUS_EXPIRED="EXPIRED"`）；但 `ErpInvBatchBizModel.java` 是 **15 行空 CRUD 桩**（`extends CrudBizModel<ErpInvBatch>` 无方法——无 ACTIVE→EXPIRED 状态迁移，无 expiry scheduler）；outgoing move expiry check **缺失**（grep `getExpiryDate`/`BATCH_STATUS_EXPIRED`/`效期` in `erp-inv-service/*.java` 仅返回 dashboard warning code `ErpInvDashboardBizModel.java:248-285`——advisory only，非 hard interception；`ErpInvStockMoveProcessor.doConfirm/doComplete`/`validateAvailable` 无 expiry check）；L1 要求"物料.批次管控 == 强制"条件分支**未被消费**——`ErpMdMaterial.isBatchManaged`（Boolean，`app-erp-master-data.orm.xml:203 IS_BATCH_MANAGED`，`_ErpMdMaterial.java:57-58 PROP_NAME_isBatchManaged`，default false）字段**存在**但 `validateAvailable` 从不查询它，outgoing move confirm 不按 isBatchManaged + expiryDate 做拦截分支。**结论**：UC-INV-06 效期拦截验收标准实质偏离——L1 `:113` "若批次.效期 < 当前日期 且 物料.批次管控 == 强制：出库移动单确认失败（批次过期拦截）"**未实现**——过期批次出库不会被拦截，可能导致发放过期物料（食品安全/药品合规风险）。修复属**代码逻辑**类（预授权，query 既有 isBatchManaged + expiryDate 字段），不涉及 ORM 结构变更。
  - **UC-INV-09 负库存放行**（✅ 已实现 & 可配置）：config key `ErpInvConstants.java:14 CONFIG_ALLOW_NEGATIVE_STOCK="erp-inv.allow-negative-stock"`；reader `ErpInvStockMoveProcessor.java:285-288 isNegativeStockAllowed()` reads `AppConfig.var(...,Boolean.FALSE)`（**default false** refuse）；短路 `validateAvailable:117-119 if(isNegativeStockAllowed()) return;` skip all per-line checks；runtime 可经 NopSysVariable 翻转（标准 Nop 平台机制，无需重启）。
  - **跨域 Facade 汇总**：UC-INV-02 的拒绝行为经 `IErpInvStockMoveBiz.generateMove` 传播到 sales 域（`@BizMutation` 原子回滚）；UC-INV-06/09 为库存域内行为，无跨域调用。

- **L4 测试证据现状**（`module-inventory/erp-inv-service/src/test/`）：UC-INV-02 `TestErpInvStockMoveBizModel.java:86-93 testIllegalTransitionRejected`（GraphQL error code propagation）/ `:95-115 testCancelReleasesReservation`（CONFIRMED outgoing reserves available=10−5=5，CANCEL restores 10）/ `TestErpInvStockMoveBookkeeping.java:268-278`（default false → ERR_AVAILABLE_INSUFFICIENT）；A2.9（`arm-index.md:451`）confirms "出库 approve 可用量校验销售独有约束已落实"。UC-INV-06 批次追溯 `TraceChainQuery` 有 `batchTrace` 但 **⚠️ 无 dedicated expiry interception test**（无过期批次出库被拦截的测试——路径不存在）；UC-INV-09 `TestErpInvStockMoveBookkeeping.java:154`（allow-negative-stock=true，无初始库存，outgoing 5 → total & available = −5）/ `:217`（CONFIRMED outgoing reserves correctly under negative-stock=true）/ `:268-278`（default false → ERR_AVAILABLE_INSUFFICIENT）/ `TestErpInvConcurrentDeduct.java:425-466`（toggle flag）/ 各 CostingStrategy test（FIFO:356/LIFO:263/Batch:248/Specific:314/Standard:380）均 flip config。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`（A2.11，`:459`）：**出库 approve 可用量校验已落实**——经 IErpInvStockMoveBiz.generateMove→ErpInvStockMoveProcessor.doConfirm→validateAvailable 强制（available < required）抛 ERR_AVAILABLE_INSUFFICIENT；**负库存放行权限缺失——证伪**（config 默认 false + NopSysVariable 运行时覆盖权限保护）。P1-MA2-062（StockTake completeTake）/ P1-MA2-063（PickingOrder 死状态）——均不在本切片范围。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）：`ErpInvStockMoveProcessor`/`validateAvailable` 代码质量 PASS。
  - `docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`（A2.9，`:451`）：**出库 approve 可用量校验销售独有约束已落实**——确认 UC-INV-02 跨域传播。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为，只补"需求契约↔行为"差异（UC-INV-06 效期拦截完全缺失 / 批次继承断言 / 物料.批次管控 flag 查询缺失等）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P0-MA2-020`（StockBalance UK resolved，`:584`/`:322-323`——UK_INV_STOCK_BALANCE_NATURAL 支撑 UC-INV-02/09 并发安全）、`P1-MA2-062`（StockTake completeTake，`:459`——盘点单非本切片）、`P1-MA2-063`（PickingOrder 死状态，`:459`——不在本切片）、`P1-MA2-023`（SPECIFIC 历史成本守卫，`:415`——cost method 非本切片 UC）、`P1-MA4-021`（trace chain 测试 gap resolved R2.14，`:626`）。**RC 系列对 inventory 为零**——A1.26 为 inventory 域第二个 RC 切片（A1.25 先行）。本切片须 grep arm-index inventory 批次/效期同域同控制点后裁决：UC-INV-06 效期拦截缺失 vs 既有 finding——**A2.11 `:459` 仅列 `ErpInvBatch.expiryDate` under dashboard context（advisory），未标记 outgoing move expiry interception 缺失**——裁决**新建 P1-RC-xxx** 并列明差异依据（既有 MA2 从未审计 expiry interception at move-confirm layer）。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1；UC-INV-06 效期拦截缺失属功能完全缺失（§2 P1①），修复属**代码逻辑**类（预授权——query 既有 `ErpMdMaterial.isBatchManaged` + `ErpInvBatch.expiryDate` 字段，不涉及 ORM 结构变更）。须人工确认 product-scope 是否要求效期拦截（若 L1 明确要求"物料.批次管控==强制"则 P1 强制实现）。

- **剩余差距**：A1.26 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源，且 UC-INV-06 效期拦截缺失是潜在合规风险（过期物料发放）。本计划产出 A1.26 报告并登记 finding，解除其链路证据缺口。

## Goals

- 产出 A1.26 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md`，含方法论 §6 **9 段全部内容**。
- 对 3 UC（UC-INV-02/06/09）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并。
- 对候选缺口给出分级结论：#1 UC-INV-06 效期拦截完全缺失（L1 `:113` "若批次.效期 < 当前日期 且 物料.批次管控 == 强制：出库移动单确认失败"——`ErpInvBatchBizModel` 15 行 CRUD 桩 + `validateAvailable` 无 expiry check + `ErpMdMaterial.isBatchManaged` 存在但从不被查询——倾向 **P1**（功能完全缺失/行为实质偏离验收标准，涉食品安全/药品合规风险，须人工确认 product-scope 是否要求效期拦截））、#2 UC-INV-06 物料.批次管控 flag（L1 `:113` "物料.批次管控 == 强制"——`ErpMdMaterial.isBatchManaged`（`app-erp-master-data.orm.xml:203`）字段**存在**但 `validateAvailable` 从不查询 → 代码逻辑缺口非 ORM 结构缺口）、#3 UC-INV-06 完工入库新批次（L1 `:115` "完工入库生成新批次（不继承）"——复核 mfg 完工入库是否生成新 batchNo）、#4 UC-INV-06 批次继承（L1 `:111` "领料移动单.批次 == 入库移动单.批次"——复核 OUTGOING move line batchNo 是否继承自 INCOMING）、#5 UC-INV-09 默认 false 安全性（config default false + NopSysVariable runtime——已实现，复核 L1 "若配置 == false：拒绝"语义是否满足）、#6 UC-INV-02 回滚原子性（`@BizMutation` 事务回滚——已实现，复核 approveStatus 保持 SUBMITTED）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/state-machine.md/trace-chain.md/cross-domain.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.25 inventory-F1 各自独立 plan；A1.26 只覆盖 UC-INV-02/06/09）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：可用量校验/负库存放行由 A2.11/A2.9 证实，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议，**§2 P1① 功能完全缺失判据为本切片 UC-INV-06 效期拦截定级的关键**）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.26 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.26 UC 锚点）+ `docs/design/inventory/use-cases.md`（L1 真相源）+ `docs/design/inventory/state-machine.md`（L2 §4，非真相源）+ `docs/design/inventory/trace-chain.md`（L2 §追溯链与批次）+ `docs/design/inventory/cross-domain.md`（L2 §余量校验）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/A4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测/E2E；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-inventory/erp-inv-service -Dtest=TestErpInvStockMoveBizModel,TestErpInvStockMoveBookkeeping,TestErpInvConcurrentDeduct`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-INV-02/06/09 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:37/:106/:155` 验收标准原文；L2 引用 `state-machine.md` §4、`trace-chain.md` §追溯链与批次、`cross-domain.md` §余量校验（标注"设计参考，冲突以 L1 为准"）；L3 引用 `module-inventory/.../ErpInvStockMoveBizModel.java` / `ErpInvStockMoveProcessor.java` / `TraceChainQuery.java` / `StockMoveBookkeeper.java` / `ErpInvBatchBizModel.java` / `ErpInvDashboardBizModel.java` / `ErpInvConstants.java`（含行号）；L4 引用 `TestErpInvStockMove*.java#method`（注明断言强度）；L5 复用 MA2 A2.11/A2.9/A4.5 + E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-INV-02 可用量校验（validateAvailable `:116-136` + @BizMutation 原子回滚，已实现 `TestErpInvStockMoveBizModel:86-115`）；②#6 UC-INV-02 approveStatus 保持 SUBMITTED（复核 sales delivery approval rollback 是否保持 SUBMITTED——经 `@BizMutation` 事务回滚传播）；③UC-INV-02 可用量公式（availableQuantity = total − reserved − locked `:278-283`——复核 L1 "可用量 = 现有量 − 预留量"是否一致）；④UC-INV-06 批次追溯（batchTrace `TraceChainQuery:168-192`——已实现）；⑤#4 UC-INV-06 批次继承（L1 `:111` "领料.批次 == 入库.批次"——复核 OUTGOING move line batchNo 是否继承自 INCOMING）；⑥#1 UC-INV-06 效期拦截缺失（L1 `:113` "若批次.效期 < 当前日期 且 物料.批次管控 == 强制：出库确认失败"——`ErpInvBatchBizModel` 15 行 CRUD 桩 + `validateAvailable` 无 expiry check + dashboard advisory only `ErpInvDashboardBizModel:248-285`，**最高风险**）；⑦#2 UC-INV-06 物料.批次管控 flag（L1 `:113` "物料.批次管控 == 强制"——`ErpMdMaterial.isBatchManaged`（`app-erp-master-data.orm.xml:203`）字段**存在**但 `validateAvailable` 从不查询 → 条件分支未被消费，代码逻辑缺口非 ORM 缺口）；⑧#3 UC-INV-06 完工入库新批次（L1 `:115` "完工入库生成新批次（不继承）"——复核 mfg 完工入库 StockMove 是否生成新 batchNo）；⑨UC-INV-09 负库存放行（config default false + runtime NopSysVariable `ErpInvStockMoveProcessor:285-288`——已实现）；⑩#5 UC-INV-09 默认 false 安全性（复核 config `AppConfig.var(...,Boolean.FALSE)` default 值与 L1 "若配置 == false：拒绝"语义一致）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：#1 UC-INV-06 效期拦截缺失属"功能完全缺失"（§2 P1①）——倾向 **P1**（须人工确认是否 product-scope 范围含效期拦截，若含则 P1 强制实现；涉食品安全/药品合规风险不可方案 B 降级）；#2 物料.批次管控 flag `ErpMdMaterial.isBatchManaged`（`app-erp-master-data.orm.xml:203`）**存在**但 `validateAvailable` 从不查询 → 代码逻辑缺口倾向 P1（条件分支未被消费，与 #1 同根因）；#3 完工入库新批次若 mfg 未生成则倾向 P2（次要验收标准）；#4 批次继承若已实现则接受；#5/#6 已实现则接受。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-INV-02/06/09 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.11/A2.9/A4.5 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#6 有明确分级（非悬空"待查"）；#1 效期拦截缺失有明确 P1 倾向 + 人工确认范围标记

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` inventory 批次/效期/可用量同域同控制点后裁决——UC-INV-06 效期拦截缺失 vs 既有 A2.11（仅 dashboard advisory）→ **新建 P1-RC-xxx** 列明差异依据（既有 MA2 从未审计 move-confirm layer expiry interception）；UC-INV-02/09 已由 A2.11/A2.9 证实 → 复用注记。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 allow-negative-stock=true 下并发出库的实际余额下限行为、batchTrace 在跨域 move 链下的聚合正确性、expiryDate 字段在 ORM 存在但无 writer 时的默认值行为等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-0400-arm-ma2-inventory-state-machine.md`（A2.11 可用量校验 + 负库存证伪 PASS）+ `2026-07-28-0400-arm-ma2-sales-state-machine.md`（A2.9 出库 approve 可用量校验销售独有 PASS）+ `2026-07-29-0430-...-code-quality.md`（A4.5 代码质量 PASS），列明只补的需求视角差异（UC-INV-06 效期拦截缺失 / 物料.批次管控 flag / 完工入库新批次 / 批次继承断言）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [ ] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_03af6f594fferjttJVLOhGFAAi，fresh session，未起草本计划）。1 阻塞：plan 在 6 处声称 `ErpMdMaterial` 无 batchMandatory/batchControl 字段并据此触发 ORM ask-first，但实测 `ErpMdMaterial.isBatchManaged`（Boolean，`app-erp-master-data.orm.xml:203 IS_BATCH_MANAGED`，`_ErpMdMaterial.java:57-58`，default false）**存在** — 违反规则 1（诚实 live-repo baseline）。真实缺口 = `validateAvailable` 不查询既有 isBatchManaged + expiryDate，属代码逻辑类修复（预授权），非 ORM 结构变更。
- Independent draft review iteration 2: `accept`（独立子代理 ses_03af43e85ffeN3O58hvJermBT5，fresh session）。6 处修订全部验证通过：①isBatchManaged 存在（orm.xml:203 确认）；②gap 正确归类为"代码逻辑缺口非 ORM 缺口"；③`rg batchMandatory|补 ORM 字件|ask-first` 零匹配；④Deferred 保护区域不再提 ORM ask-first；⑤保护区域不再提 ORM ask-first；⑥P1 分类 + 人工确认要求保留。`validateAvailable:116-136` 确认不查询 isBatchManaged。无新问题。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.26 报告 9 段齐全 + 3 UC 逐矩阵行 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.26 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；UC-INV-06 效期拦截缺失属**代码逻辑**类修复（预授权——query 既有 `ErpMdMaterial.isBatchManaged` + `ErpInvBatch.expiryDate` 字段，不涉及 ORM 结构变更）。#1 效期拦截缺失须人工确认 product-scope 是否范围裁剪（若裁剪则改真相源非降级；若未裁剪则 P1 强制实现）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；#1 待人工确认 product-scope 范围）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围
- UC-INV-06 效期拦截缺失须人工确认 product-scope 是否要求效期拦截（涉食品安全/药品合规风险）
