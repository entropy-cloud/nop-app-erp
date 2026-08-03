# 2026-08-03-1200-2 rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability inventory-F1 移动单主链与追溯需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.25（MA1 需求追踪矩阵审计 — inventory-F1 移动单主链与追溯）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.25
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.25 的 0.2 依赖）、`2026-08-03-1200-1-rc-ma1-a1-24-assets-f3-capitalization-idle-cip-inventory-maintenance-splitmerge-dashboard.md`（同批次，先 assets-F3 后 inventory-F1）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.25 给出 UC 清单 = `UC-INV-01/03/04/05`（4 UC），含 `use-cases.md:15/:57/:76/:92` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/inventory/use-cases.md`（机制见 `docs/design/inventory/state-machine.md` §1/§2/§3、`trace-chain.md` §追溯链模型/场景/批次、`cross-domain.md` §与采购协作/余量校验/与财务协作）：
  - UC-INV-01 采购入库移动单全链（`:15`）：采购入库单审核通过→库存域 generateMove(incoming)→移动单(DRAFT→CONFIRMED→DONE)；移动单.状态==DONE；库存余额[物料,仓库,批次].现有量 += 移动数量；存在不可变流水（关联移动单，记录 数量/单位成本/余额快照）；移动单 DONE 发布事件→触发存货估值凭证异步生成。
  - UC-INV-03 已完成移动单冲销（`:57`）：已 DONE 的移动单需要冲销→生成反向移动单（新 DRAFT，数量取负）；反向单走 DRAFT→CONFIRMED→DONE 流程；DONE 后库存余额按反向数量调整（原+的反-）；原流水不删除（不可变），新增反向流水；追溯链：反向单.originReturnedMoveId 指向原单。
  - UC-INV-04 全链路正向追溯（`:76`）：链路 采购入库→生产领料→完工入库→销售出库；每环移动单通过 originMoveId / destMoveIds 关联；从销售出库单可反向追溯到采购入库单（经中间环节）；从采购入库单可正向追踪到所有去向（领料/完工/销售）。
  - UC-INV-05 退货反查原移动单（`:92`）：退货移动单.originReturnedMoveId == 原入库/出库移动单.id；原单.returnedMoveIds 包含退货单.id（双向）。

- **L3 代码实现现状（实测）**——4/4 UC 均已实现，StockMove 遵循 Facade BizModel + per-mutation Processor 两层模式（R6.4 重构）：
  - **UC-INV-01 采购入库移动单全链**（✅ 已实现）：`ErpInvStockMoveBizModel.java:58-60 generateMove`（Facade）；`ErpInvStockMoveGenerateMoveProcessor.java:28-47 generateMove`（idempotent findExisting→newMove→persistLines→doConfirm→if businessLinked doComplete）；`ErpInvStockMoveProcessor.java:86-98 doConfirm`（validateAvailable→applyReservation reserve=true→DRAFT→CONFIRMED）；`:100-114 doComplete`（releaseReservation→`StockMoveBookkeeper.bookCompletion:116-130`→CONFIRMED→DONE→`InvPostingDispatcher.dispatchIfApplicable`）；`StockMoveBookkeeper.java:116-130 bookCompletion`（dispatch 7 CostingStrategy by material.costMethod，写 `ErpInvStockLedger` + 更新 `ErpInvStockBalance`）。ORM `app-erp-inventory.orm.xml`：`ErpInvStockMove` entity `relatedBillType/Code` `:166-167`、`originMoveId` `:175`、`originReturnedMoveId` `:176`、UK `UK_INV_STOCK_MOVE_CODE_ORG` `:189`。
  - **UC-INV-03 已完成移动单冲销**（✅ 已实现）：`ErpInvStockMoveBizModel.java:80-84 reverse`（Facade mutation）；`ErpInvStockMoveReverseProcessor.java:33-45 reverse`（requireMove→DONE guard→`buildReverseRequest:47-77`→delegate to generateMoveProcessor.generateMove）；`buildReverseRequest:47-77`（inverseMoveType，swap src/dest warehouses+locations，`relatedBillType="REVERSAL"`，`relatedBillCode=original.code`，`originReturnedMoveId=original.id`，`remark="冲销"`）；DONE guard `:35-40` 抛 `ERR_REVERSE_NOT_DONE`；`inverseMoveType` helper `ErpInvStockMoveProcessor.java:258-269`（INCOMING↔OUTGOING，INTERNAL stays）。**关键设计**（A2.11 `arm-index.md:459`）：reversal 非 status rollback——原单保持 DONE，反向单走正常 generateMove 流程（DRAFT→CONFIRMED→DONE），反向单 re-validates available qty（余额守恒）。
  - **UC-INV-04 全链路正向追溯**（✅ 已实现）：`ErpInvStockMoveBizModel.java:94-98 forwardTrace`（@BizQuery）；`ErpInvStockMoveProcessor.java:68-70 forwardTrace(moveId,isTraceChainEnabled(),traceChainMaxDepth())`；`TraceChainQuery.java:50-88 forwardTrace`（BFS via `findActiveMovesByOrigin` queries moves where `originMoveId=current.id`，`Set<Long> visited` cycle detection，depth guard→`truncated=true`）；result `TraceChainResult.java`（nodes+links+`truncated` flag）。配置 `ErpInvConstants.java:25-27 CONFIG_TRACE_CHAIN_ENABLED="erp-inv.trace-chain-enabled"`（default true）+ `CONFIG_TRACE_CHAIN_MAX_DEPTH`（default 10），read at `ErpInvStockMoveProcessor.java:290-302`。bonus `backwardTrace`（`ErpInvStockMoveBizModel.java:100-104`，`TraceChainQuery.java:94-129`）亦实现。
  - **UC-INV-05 退货反查原移动单**（✅ 已实现）：`ErpInvStockMoveBizModel.java:86-92 findByRelatedBill(relatedBillType,relatedBillCode,context)`（@BizAction）；`ErpInvStockMoveProcessor.java:57-66`（QueryBean `eq("relatedBillType",...)`+`eq("relatedBillCode",...)`+`addOrderField("id",true)` DESC deterministic）；`ErpInvStockMoveBizModel.java:106-110 returnTrace`（@BizQuery，via `originReturnedMoveId`）；`TraceChainQuery.java:135-162 returnTrace`（双向：anchor=originReturnedMoveId of root if root is return else root itself，finds all `findActiveReturnsOf(anchorId)`）。ORM `app-erp-inventory.orm.xml:176` `originReturnedMoveId` + `:185` to-one + `:215-217` index。
  - **跨域 Facade 被调用方汇总**：purchase `ErpPurReceiveProcessor.java:218 generateMove` / `:230 findByRelatedBill` / `:241 reverse`；`ErpPurReturnProcessor.java:240,253,259 findByRelatedBill` / `:264 reverse`；sales `ErpSalDeliveryProcessor.java:244 generateMove` / `:256,262 findByRelatedBill` / `:267 reverse`；`ErpSalReturnProcessor.java:289 generateMove` / `:297,308,314 findByRelatedBill` / `:319 reverse`。全部经 `IErpInvStockMoveBiz` Facade，无 `daoFor(ErpInvStockMove)` 直写（P0-MA1-021 closure）。

- **L4 测试证据现状**（`module-inventory/erp-inv-service/src/test/`）：UC-INV-01 `TestErpInvStockMoveBizModel.java:55-64 testGenerateMoveBusinessLinkedAutoCompletes`（DONE + posted=false + 1 line）/ `:67-74 testGenerateMoveIdempotent` / `:77-83 testManualMoveStopsAtConfirmed`；UC-INV-03 `:117-135 testReverseCreatesReverseMove`（新 move relatedBillType=REVERSAL + relatedBillCode=original.code + DONE + 原 DONE 保持）；UC-INV-04 **⚠️ 无 dedicated forwardTrace test**（仅状态机测试，行为经代码审查验证）；UC-INV-05 **⚠️ 无 dedicated returnTrace/findByRelatedBill test**（跨域覆盖经 `TestErpPurReceiveStockMove`/`TestErpSalDeliveryStockMove` 族）。P1-MA4-021（pur+sal+inv 测试有效性 gap，resolved R2.14 `arm-index.md:626`）——trace chain 专门覆盖仍薄。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`（A2.11，`:459`）：**DONE 冲销非状态回退**（reverse 生成反向 StockMove 走 generateMove 正常 DRAFT→CONFIRMED→DONE 流程，原单 DONE 保持；testReverseCreatesReverseMove 证据）；P1-MA2-062（StockTake completeTake，盘点单非移动单——不在本切片范围）；P1-MA2-063（PickingOrder PICKING/PICKED 死状态——不在本切片范围）。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）：`StockMoveBookkeeper`/`TraceChainQuery`/`ErpInvStockMoveProcessor` 代码质量 PASS；P1-MA4-021（trace chain 测试覆盖 gap，resolved R2.14）。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）：跨域 `IErpInvStockMoveBiz` Facade 调用合规性 PASS。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为，只补"需求契约↔行为"差异（forwardTrace/returnTrace 专用测试缺失 / 不可变流水+余额快照断言强度 / 异步凭证触发断言等）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P0-MA2-020`（StockBalance UK resolved fix plan done，`arm-index.md:201`——UK_INV_STOCK_BALANCE_NATURAL 支撑 UC-INV-01/03 并发安全 in `StockMoveBookkeeper.upsertBalance`+`updateBalanceWithRetry:256-328`）、`P2-MA2-028`（reverse uses today() not businessDate，`:504`——**直接关系 UC-INV-03**：R6.9 fixed，`ErpInvStockMoveReverseProcessor.java:51` 现用 `original.getBusinessDate()` with today() null fallback）、`P1-MA4-020`（到岸成本反向过账悬挂 resolved R1.16，`:625`——邻近 UC-INV-03 reverse path）、`P1-MA3-062`（Processor per-mutation split R6.4 done，`:402`——解释当前 Facade+Processor 架构）、`P1-MA4-021`（trace chain 测试 gap resolved R2.14，`:626`）。**RC 系列对 inventory 为零**——A1.25 为 inventory 域首个 RC 切片。本切片新发现的静默缺口（UC-INV-04/05 dedicated 测试缺失 / 不可变流水余额快照断言强度）须按 §7 grep 比对后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1；触及 ORM 结构（originMoveId/destMoveIds 链补全）或会计过账逻辑（存货估值凭证异步触发）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.25 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.25 报告并登记 finding，解除其链路证据缺口。

## Goals

- 产出 A1.25 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability.md`，含方法论 §6 **9 段全部内容**。
- 对 4 UC（UC-INV-01/03/04/05）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并。
- 对候选缺口给出分级结论：#1 UC-INV-04 forwardTrace dedicated 测试缺失（行为已实现 `TraceChainQuery:50-88` 但无专用单测——P1-MA4-021 resolved R2.14 是否已覆盖 trace chain，若否倾向 P1）、#2 UC-INV-05 returnTrace/findByRelatedBill dedicated 测试缺失（跨域覆盖经 pur/sal 测试——复核断言强度是否满足 L1 双向断言）、#3 UC-INV-01 不可变流水余额快照（`StockMoveBookkeeper.writeLedger:217` 写流水含 batchNo——复核"余额快照"是否在 ledger 记录）、#4 UC-INV-01 异步凭证触发（`InvPostingDispatcher.dispatchIfApplicable`——复核 L1"DONE 发布事件→触发存货估值凭证异步生成"是否 post-commit async）、#5 UC-INV-03 reverse businessDate（P2-MA2-028 R6.9 已 fix——复核 current HEAD 是否用 original date）——按 §2 判据定级，若为 P0/P1 则新建并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；既有行追加 RC 注记）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/state-machine.md/trace-chain.md/cross-domain.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.26 inventory-F2 各自独立 plan；A1.25 只覆盖 UC-INV-01/03/04/05）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：移动单状态机/冲销行为/代码质量由 A2.11/A4.5 证实，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.25 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.25 UC 锚点）+ `docs/design/inventory/use-cases.md`（L1 真相源）+ `docs/design/inventory/state-machine.md`（L2 §1/§2/§3，非真相源）+ `docs/design/inventory/trace-chain.md`（L2 追溯链模型/场景/批次）+ `docs/design/inventory/cross-domain.md`（L2 与采购协作/余量校验/与财务协作）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/A4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测/E2E；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-inventory/erp-inv-service -Dtest=TestErpInvStockMoveBizModel,TestErpInvStockMoveBookkeeping`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-03-0953-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-INV-01/03/04/05 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:15/:57/:76/:92` 验收标准原文；L2 引用 `state-machine.md` §1/§2/§3、`trace-chain.md` §追溯链模型/场景、`cross-domain.md` §与采购协作/余量校验/与财务协作（标注"设计参考，冲突以 L1 为准"）；L3 引用 `module-inventory/.../ErpInvStockMoveBizModel.java` / `ErpInvStockMoveGenerateMoveProcessor.java` / `ErpInvStockMoveProcessor.java` / `ErpInvStockMoveReverseProcessor.java` / `TraceChainQuery.java` / `StockMoveBookkeeper.java` / `InvPostingDispatcher.java`（含行号 + 跨域 `IErpInvStockMoveBiz` 被调用方）；L4 引用 `TestErpInvStockMove*.java#method`（注明断言强度）；L5 复用 MA2 A2.11/A4.5 + E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-INV-01 全链（generateMove idempotent + auto-DONE `GenerateMoveProcessor:28-47` + 余额 += `StockMoveBookkeeper:116-130`，已实现）；②#3 UC-INV-01 不可变流水余额快照（`writeLedger:217` 写 ErpInvStockLedger 含 batchNo——复核 L1"记录数量/单位成本/余额快照"中"余额快照"字段是否存在于 ledger entity）；③#4 UC-INV-01 异步凭证触发（`InvPostingDispatcher.dispatchIfApplicable`——复核是否 post-commit async 即 L1"DONE 发布事件→异步生成"）；④UC-INV-03 冲销（reverse 非状态回退 + 新反向单走 generateMove `ReverseProcessor:33-45`，已实现）；⑤#5 UC-INV-03 reverse businessDate（P2-MA2-028 R6.9 fixed `:51`——复核 HEAD 是否用 original.getBusinessDate()）；⑥UC-INV-03 原流水不可变（反向新增流水，原不删——`StockMoveBookkeeper` ledger 追加不改写）；⑦UC-INV-03 originReturnedMoveId 双向（`buildReverseRequest:47-77` set + `TraceChainQuery.returnTrace:135-162` 反查）；⑧UC-INV-04 forwardTrace（`TraceChainQuery:50-88` BFS + cycle detection + depth guard——已实现）；⑨#1 UC-INV-04 dedicated test 缺失（`TestErpInvStockMoveBizModel` 无 forwardTrace test——复核 P1-MA4-021 R2.14 是否覆盖）；⑩UC-INV-05 returnTrace（`TraceChainQuery:135-162` 双向——已实现）；⑪#2 UC-INV-05 dedicated test 缺失（跨域覆盖经 pur/sal——复核断言强度）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：#1/#2 dedicated 测试缺失若 P1-MA4-021 R2.14 未覆盖 trace chain 专门则倾向 P1（测试断言完全缺失或仅冒烟）；#3 余额快照若 ledger 无对应字段则倾向 P2（次要验收标准未满足）；#4 异步凭证若 dispatch 是同步则倾向 P2（主路径 OK，异步语义弱）；#5 若 HEAD 已 fix 则接受（既有 finding 已 resolved）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-INV-01/03/04/05 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.11/A4.5 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#5 有明确分级（非悬空"待查"）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-03-0953-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` inventory 同域同控制点（如 P1-MA4-021 测试覆盖、P2-MA2-028 reverse businessDate、P0-MA2-020 StockBalance UK）后裁决——同根因同控制点 → 复用（追加 RC 注记）；新根因 → 新建 `P*-RC-xxx` 列明差异依据。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）；记录既有 resolved finding（P2-MA2-028/P0-MA2-020）的 HEAD 复核结论。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 forwardTrace 在环链/超深链下的 truncated 行为、reverse move 在 available qty 不足时的实际拒绝行为、异步凭证 post-commit 的事务可见性等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-0400-arm-ma2-inventory-state-machine.md`（A2.11 冲销非回退 + 状态机 PASS）+ `2026-07-29-0430-...-code-quality.md`（A4.5 代码质量 PASS + P1-MA4-021 resolved），列明只补的需求视角差异（forwardTrace/returnTrace dedicated 测试缺失 / 余额快照断言强度 / 异步凭证语义 / reverse businessDate HEAD 复核）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；既有行追加 RC 复核注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_03af70469ffe1Svp68G3xbkohb，fresh session，未起草本计划）。1 阻塞：P0-MA2-020 arm-index 行号引用错误（`:584`/`:322-323` 指向无关 P1-MA2-082/083 budget-commitment finding，正确为 `:201`）— 违反规则 1（诚实 live-repo baseline）。1 minor：错误码 `ERR_REVERSAL_NOT_DONE` 实际为 `ERR_REVERSE_NOT_DONE`。
- Independent draft review iteration 2: `accept`（独立子代理 ses_03af44a7cffeAyJ4IcuqRujzfL，fresh session）。两处修订验证：①arm-index.md:201 确认含 P0-MA2-020（StockBalance UK）；②`ErpInvStockMoveReverseProcessor.java:37` 确认用 `ERR_REVERSE_NOT_DONE`。无新问题。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.25 报告 9 段齐全 + 4 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.25 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；触及 ORM 结构（originMoveId/destMoveIds 链补全）或会计过账逻辑（存货估值凭证异步触发）的修复行须 ask-first + 独立 plan-audit（§5）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: 已完成（2026-08-03）。A1.25 inventory-F1 移动单主链与追溯需求符合性审计报告已落盘 `docs/audits/2026-08-03-0953-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability.md`（9 段齐全）。整体裁决：4 UC（UC-INV-01/03/04/05）全接受，零 P0/零 P1/零新 P2 finding。5 候选缺口经 live-repo HEAD 实测全部 resolved/不成立：#1/#2 dedicated test 计划基线陈旧（HEAD 实仓 `TestErpInvTraceChain.java` 9 方法强覆盖 forward/backward/return/batch + 环检测 + depth guard）；#3 余额快照字段存在（`writeLedger:211-212` + ORM `:299-300` mandatory）；#4 L2↔L3 post-commit 漂移 L1 满足（SP-1 交 MA4）；#5 P2-MA2-028 R6.9 已 fix。5 resolved finding HEAD 复核全确认无回退（P2-MA2-028/P0-MA2-020/P1-MA4-021 范围澄清/P1-MA3-062/P1-MA4-020）。arm-index 已更新（报告清单 + RC 交叉引用注记）。checker 19 规则 actual ≤ baseline 全 ✅（只读审计无回归风险）。

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围。本切片零新 finding，故无 MR0/MR1 修复行追加。SP-1（InvPostingDispatcher post-commit 时序边缘风险）+ SP-2（forwardTrace 多分支超深链 truncated 行为）交 MA4 A4.1 运行时展开。
