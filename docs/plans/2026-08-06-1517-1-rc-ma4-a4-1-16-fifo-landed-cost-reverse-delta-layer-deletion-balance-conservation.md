# 2026-08-06-1517-1 rc-ma4-a4-1-16-fifo-landed-cost-reverse-delta-layer-deletion-balance-conservation FIFO 物料到岸成本红冲 delta 层物理删除余额守恒评估

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.16（MA4 运行时行为验证 — A1.5 §7-2：UC-FIN-10 FIFO 物料到岸成本红冲 delta 层部分消耗后物理删除余额守恒，复用 P2-MA2-029 子场景；**触及数据删除行为探针**，但本验证为只读代码路径推理 + 既有测试覆盖普查，不修改成本过账/删除核心路径代码）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.16；存疑点来源 `docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 2
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done）、`docs/plans/2026-08-07-1400-3-rc-ma4-a4-1-15-fifo-landed-cost-delta-layer-consumption-correctness.md`（A4.1.15 done — 升级 P2-RC-004=P1，证明 delta 层结构性永不被消耗，是本存疑点「部分消耗」前提的直接前置）、`docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md`（A1.5 报告 §7 存疑点 2 + §5.3 P2-MA2-029 复用 + §3.5 FIFO 红冲测试缺口）、`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（A2.4 costing 三方对账既有行为证据输入）、`docs/audits/2026-08-06-1044-3-rc-ma4-a4-1-12-bank-recon-adj-voucher-line-correctness.md`（A4.1.12 done 数值正确性 + 断言强度评估同型范式）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.16 验证报告（落盘 `docs/audits/2026-08-06-1517-rc-ma4-a4-1-16-fifo-landed-cost-reverse-delta-layer-deletion-balance-conservation.md`）+ 必要时 arm-index finding/successor 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：读 `CostAdjustmentService.reverseLine` 红冲余额回退 + `removeFifoAdjustLayer` delta 层物理删除逻辑 + `appendFifoAdjustLayer` delta 层追加逻辑 + 既有 FIFO 红冲测试覆盖普查）。范式对齐 A4.1.12/A4.1.15（已 done 的数值正确性评估同型工作项）。

- **存疑点原文**（A1.5 报告 §7 存疑点 2，`2026-08-02-2045-...-a1-5-costing.md` §7）：「FIFO 物料到岸成本红冲 delta 层部分消耗后物理删除的余额守恒」——`CostAdjustmentService.removeFifoAdjustLayer` 按 `-line.id` 哨兵物理删除 delta 层，若该层已部分被后续出库消耗，物理删除可能破坏已扣减层 / 余额 totalCost 漂移。触发条件 = 实际启用 FIFO 物料的到岸成本分摊后部分出库再红冲。复用 P2-MA2-029 子场景。

- **A4.1.15 前置裁决对本存疑点的决定性影响**：A4.1.15（done，升级 P2-RC-004 = P1）经数值推理**证实 delta 层结构性永不被消耗**——`appendFifoAdjustLayer:160-161` `setIncomingQuantity(onHand)`+`setRemainingQuantity(onHand)`（全量现有量非增量）+ `incomingDate=adjust.businessDate`（≥ 原入库层）→ `FifoCostingStrategy.findFifoLayers` 升序消耗循环中原入库层（更早 incomingDate + remaining=onHand）总是先于且独占地满足出库量，delta 层永不被到达。**因此本存疑点原始前提「delta 层部分消耗后物理删除」在当前实现下可能不可达**——delta 层 `remainingQuantity` 恒为初始 `onHand`（全量未被消耗）。本验证须据此重评：(a) 「部分消耗」路径在当前实现是否真的不可达（复核 A4.1.15 结论）；(b) 若不可达，`removeFifoAdjustLayer` 物理删除的是**全量未消耗 delta 层**，此时余额守恒是否成立（`reverseLine:182-186` 回退 `balance.totalCost -= adjustAmount` + `avgCost = oldUnitCost` + 删除 delta 层——删除未消耗层不破坏 FIFO 出库已消耗的原入库层，余额回退与层删除一致性）。

- **关联既有 finding**：
  - **P2-MA2-029**（arm-index，CostAdjustment FIFO 红冲 delta 层物理删除三方一致性未测试）：到岸成本红冲是其子场景。A1.5 §5.3 裁决**复用 P2-MA2-029**（同根因同控制点：`removeFifoAdjustLayer` 物理删除）。**状态：watch-only successor**（`costing-methods.md:66` 已登记）。本验证只评估余额守恒是否成立（行为是否正确，还是仅测试缺失），确认/调整 P2-MA2-029 分级。
  - **P2-RC-004**（arm-index `:134`，A4.1.15 升级 P1）：delta 层 forward 消耗路径（结构性永不被消耗）。本验证是其 **reverse 路径**对应面（同 delta 层，不同方向[删除 vs 消耗]，不同控制点），交叉引用不重复。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:197` UC-FIN-10 LC-L3「后续出库按更新后的队列单价计算」隐含红冲闭环：成本调整红冲后余额/层状态应回退至调整前一致状态（余额守恒不变量）。L2 `costing-methods.md §成本调整 :415-477` + `:66` FIFO 红冲 delta 层 successor 声明。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实，全在 module-inventory）**：
  - 红冲入口：`CostAdjustmentService.reverseLine:175-192`（`:176` flushSession → `:182-186` 余额回退 `balance.totalCost -= adjustAmount` + `avgCost = oldUnitCost` + saveOrUpdate → `:188` 调 `removeFifoAdjustLayer(line)` → `:189-191` STANDARD_REVALUATION 额外 removeFirmedRollup）。
  - delta 层物理删除（本存疑点核心）：`removeFifoAdjustLayer:194-202`——`eq("incomingMoveId", -line.getId())` 哨兵精确查 delta 层（`:197`）→ `dao.findAllByQuery(q)`（`:198`）→ 逐层 `dao.deleteEntity(layer)`（`:199-201`）物理删除。
  - delta 层追加（红冲的镜像，A4.1.15 已确认）：`appendFifoAdjustLayer:149-171`（`:160-161` remainingQuantity=onHand + `:162` unitCost=Δ + `:165-166` incomingDate=adjust.businessDate + `:168` incomingMoveId=-lineId 哨兵）。
  - FIFO 升序消耗：`FifoCostingStrategy.findFifoLayers:178-205`（A4.1.15 证实原入库层独占满足出库，delta 层永不被到达）。

- **既有证据（复用输入）**：
  - A2.4 costing 三方对账（`2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`）：成本调整 delta 层 reverse 路径 `removeFifoAdjustLayer:194-202` 按 `-lineId` 哨兵物理删除行为已静态确认，但**三方一致性（成本层 + 余额 + 流水）未测试**（P2-MA2-029）。本验证复用其「删除逻辑存在 + 哨兵机制正确」结论，**只补「部分消耗后删除余额守恒」差异**——但须先据 A4.1.15 重评「部分消耗」可达性。

- **剩余差距**：P2-MA2-029 的 FIFO 红冲 delta 层删除余额守恒未运行时验证。关键张力：A4.1.15 证明「部分消耗」不可达，则本存疑点的「部分消耗后删除」实际触发面可能为空——本验证须裁决：(a) P2-MA2-029 的「部分消耗后删除」风险是否被 A4.1.15 结论消解（delta 层永不被消耗 → 物理删除的全量未消耗层 → 余额守恒成立）；(b) 或是否存在其他可达路径（如手工成本调整直接 reverse 而非到岸成本编排）使部分消耗可达。

- **保护区域**：只读评估（读红冲/删除代码路径 + 既有测试覆盖普查 + 余额守恒推理），不触及 ORM/会计过账逻辑/数据删除**修改**。**本验证为「数据删除行为探针」但经只读代码路径推理 + 既有测试普查执行，不修改删除核心路径代码**（`removeFifoAdjustLayer`/`reverseLine`/`appendFifoAdjustLayer` 属保护区域，修改须 ask-first）。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（P2-MA2-029 修复若需触及 delta 层删除逻辑归 MR1，触及成本过账核心路径须 ask-first）。

## Goals

- 余额守恒评估：核验 `reverseLine:182-186`（余额回退 totalCost -= adjustAmount + avgCost = oldUnitCost）+ `removeFifoAdjustLayer:194-202`（按 `-lineId` 哨兵物理删除 delta 层）在 delta 层「全量未消耗」状态下的余额守恒一致性（删除未消耗 delta 层不破坏 FIFO 出库已消耗的原入库层；余额回退与层删除量一致）。
- 「部分消耗」可达性复核：据 A4.1.15 结论（delta 层 remainingQuantity=onHand + 升序消耗原入库层独占满足）复核「delta 层部分消耗后物理删除」路径是否真的不可达；若有其他可达路径（如非到岸成本编排的手工成本调整 reverse）须识别。
- 既有测试覆盖边界普查：grep `TestErpInvFifoCosting#testReverseRestoresCostInvariant` + `TestErpInvLandedCostReversal` + `TestErpInvCostAdjust` 全集，确认 FIFO delta 层红冲删除路径的测试覆盖边界（FIFO 多层红冲不变量单测覆盖正常入库层非 delta 层 / 到岸成本红冲测试用 MOVING_AVERAGE / 成本调整 reverse 单测 MA 路径）。
- 对齐 UC-FIN-10 LC-L3 + `costing-methods.md §成本调整 + :66 successor` 给出结论：确认/调整 P2-MA2-029 分级——①若「部分消耗」不可达 + 物理删除全量未消耗层 + 余额回退一致 → P2-MA2-029 维持 P2 测试覆盖补强（删除逻辑正确，仅三方一致性测试缺失）；②若发现「部分消耗」可达且删除破坏已扣减层/余额漂移 → 升 P1 行为缺陷（触发 MR1，触及成本过账/删除核心路径须 ask-first）。
- 产出验证报告 + §8 过程纪律自检；finding/successor 注记（P2-MA2-029 已登记，本验证只更新分级注记或确认维持；与 P2-RC-004/A4.1.15 交叉引用不重复）。

## Non-Goals

- **不修复 P2-MA2-029**（FIFO 红冲 delta 层删除三方一致性测试缺口——修复为补 FIFO 物料到岸成本红冲 E2E/单测[FIFO 物料 receive → landed cost delta 层 → reverse → 删除 delta 层 + 余额守恒断言]，纯测试代码，归 MR1 预授权类目，不触发 §5 ask-first）。
- **不修复 P2-RC-004**（forward delta 层消耗缺陷，A4.1.15 已升级 P1，归 MR1 触及成本过账核心路径须 ask-first，非本验证范围）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不修改成本过账/删除核心路径代码**（`CostAdjustmentService`/`FifoCostingStrategy`/`LandedCostAllocationEngine`/`ErpInvLandedCostProcessor` 属保护区域，修改须 ask-first + 独立 plan-audit；本验证为只读探针）。
- **不重新核实 UC-FIN-10 全部验收标准**（A1.5 §5 已判整体接受；本验证只评 FIFO delta 层红冲删除余额守恒差异）。
- **不展开 A1.5 §7-1/§7-3**（A4.1.15 forward 消耗 done / A4.1.17 SELECT FOR UPDATE 锁行为范围）。

## Task Route

- Type: `verification or audit work`（余额守恒评估 + P2-MA2-029 分级确认/调整）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议 + MA4↔A5.6 边界）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.16 行）+ `docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 2 + §5.3 P2-MA2-029 复用 + §3.5 测试缺口（输入）+ `docs/design/finance/costing-methods.md §成本调整 + :66 successor 声明`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。余额守恒评估需多维度归类（红冲余额回退逻辑 / delta 层物理删除哨兵机制 / 「部分消耗」可达性[与 A4.1.15 结论交互] / 既有测试覆盖边界 / MA4↔A5.6 边界 / P2 维持-or-升 P1 裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读红冲/删除代码路径 + 既有测试覆盖普查 + 余额守恒推理）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - FIFO delta 层红冲删除余额守恒与「部分消耗」可达性评估

Status: completed
Targets: `docs/audits/2026-08-06-1517-rc-ma4-a4-1-16-fifo-landed-cost-reverse-delta-layer-deletion-balance-conservation.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.16 行）；A1.5 done（§7 存疑点 2 已落盘 + §5.3 P2-MA2-029 复用已登记）；A4.1.15 done（P2-RC-004=P1 升级 + delta 层结构性永不被消耗结论，是本存疑点前置）

- [x] `Proof` 红冲余额回退逻辑核验：给出 `CostAdjustmentService.reverseLine:175-192`（`:176` flushSession → `:182-186` balance.totalCost -= adjustAmount + avgCost = oldUnitCost + saveOrUpdate → `:188` removeFifoAdjustLayer → `:189-191` STANDARD_REVALUATION removeFirmedRollup）证据（file:line）。证实红冲先回退余额再删除 delta 层。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` delta 层物理删除哨兵机制核验：读取 `removeFifoAdjustLayer:194-202`（`eq("incomingMoveId", -line.getId())` 哨兵 :197 → findAllByQuery :198 → 逐层 deleteEntity :199-201）+ 与 `appendFifoAdjustLayer:168`（incomingMoveId=-lineId 哨兵写入）镜像对照——评估哨兵精确匹配（正向追加写 -lineId，红冲按 -lineId 删）正确性 + 是否误删正常入库层（正常层 incomingMoveId=正 move.id，哨兵为负不冲突）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 「部分消耗」可达性复核（与 A4.1.15 交互核心）：据 A4.1.15 结论（`appendFifoAdjustLayer:160-161` remainingQuantity=onHand 全量 + `findFifoLayers` 升序消耗原入库层独占满足 + delta 层 incomingDate≥原入库层永不被到达）复核「delta 层部分消耗后物理删除」是否不可达；识别是否存在其他可达路径（如非到岸成本编排的 `IErpInvCostAdjustBiz.reverseCostAdjust` 直接 reverse 而物料在 reverse 前已有 FIFO 出库——此时 delta 层是否可能被消耗）。给出明确裁决：部分消耗不可达（delta 层恒全量）vs 存在边角可达路径。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 余额守恒一致性评估（据可达性裁决推理）：①若「部分消耗」不可达（delta 层恒全量未消耗）→ `removeFifoAdjustLayer` 删除全量未消耗 delta 层不破坏 FIFO 出库已消耗的原入库层（原入库层 remainingQuantity 已被出库扣减，删除 delta 层不影响原层）+ 余额回退（totalCost -= adjustAmount + avgCost = oldUnitCost）与层删除量一致 → 余额守恒成立；②若存在可达「部分消耗」路径 → 评估删除已部分消耗 delta 层是否破坏已扣减量（删除 remaining>0 的层丢失其代表成本调整量）+ 余额漂移面。给出余额守恒裁决。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 既有测试覆盖边界普查：grep `TestErpInvFifoCosting#testReverseRestoresCostInvariant`（FIFO 多层红冲不变量 Σ remaining×unitCost 恢复，覆盖正常入库层非 delta 层）+ `TestErpInvLandedCostReversal`（到岸成本红冲用 MOVING_AVERAGE 物料，非 FIFO）+ `TestErpInvCostAdjust`（成本调整 MA 路径 reverse 单测）全集，产出测试覆盖边界清单 + 标注 FIFO delta 层红冲删除 E2E/单测缺口。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（FIFO delta 层红冲删除余额是否守恒），与 A5.6（audit-remediation）审「E2E 断言强度」边界按此执行（方法论 §去重协议）。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` P2-MA2-029 分级确认/调整（方法论 §2 判据 + 三源对照）：①若「部分消耗」不可达 + 物理删除全量未消耗层 + 余额回退一致 → P2-MA2-029 维持 P2 测试覆盖补强（删除逻辑正确，仅三方一致性测试缺失）；②若「部分消耗」可达且删除破坏已扣减层/余额漂移 → 升 P1 行为缺陷。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.5 §5.3 P2-MA2-029 复用结论 + A4.1.15 P2-RC-004=P1（forward 路径）分层一致性（不同方向不同控制点）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 红冲余额回退 + delta 层删除哨兵机制 + 「部分消耗」可达性 + 余额守恒一致性 + 测试覆盖边界证据落盘（全集，无遗漏），每条有证据（file:line）
- [x] P2-MA2-029 分级确认/调整有明确结论（维持 P2 或升 P1），与 A1.5 §5.3 + A4.1.15 P2-RC-004 分层一致（不同方向不同控制点交叉引用不重复）

### Phase 2 - finding/successor 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-06-1517-rc-ma4-a4-1-16-fifo-landed-cost-reverse-delta-layer-deletion-balance-conservation.md`（定稿）；`docs/audits/arm-index.md`（P2-MA2-029 分级注记更新，若有）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 余额守恒评估 + 分级确认完成

- [x] `Add` P2-MA2-029 分级注记更新：若 P2 维持 → 在 arm-index P2-MA2-029 行追加「A4.1.16 运行时余额守恒评估确认 P2 维持（delta 层恒全量未消耗[A4.1.15 结论]→ 删除全量层 + 余额回退一致）」注记；若升 P1 → 标注升级 + 触发 MR1（触及删除核心路径须 ask-first）。禁止未经比对新建重复 finding（P2-MA2-029 已登记，本验证只更新注记）。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.5 §5.3 P2-MA2-029 / A2.4 costing 三方对账 / P2-RC-004[A4.1.15 forward 路径，不同方向不同控制点] 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（红冲余额回退 + delta 层删除哨兵 + 可达性 + 余额守恒 + 测试覆盖边界 + 分级确认 + finding 衔接 + §8 自检齐全）
- [x] P2-MA2-029 分级注记已更新入 arm-index（若有变更）或有明确「维持 P2 无变更」记录并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: accept (mission-driver 2026-08-04-224309-mission-driver 独立子代理 ses_02a0b47d2ffeqa1H3YuyDdw1bz，新会话不重用执行者上下文) — 全 checklist 通过：live baseline file:line 精确核验（reverseLine:175-192 / removeFifoAdjustLayer:194-202 哨兵 eq("incomingMoveId",-line.getId()):197 / appendFifoAdjustLayer:160-168 delta 层 / findFifoLayers:202-203 升序 / 3 测试文件存在）零漂移；格式合规；单一结果表面；anti-slack 零命中；item typing 合规；Deps 门控满足（A4.1 expander done + A1.5 done + A4.1.15 done）；保护区域纪律（只读不改 removeFifoAdjustLayer/reverseLine/appendFifoAdjustLayer）；逻辑健全（A4.1.15「delta 层结构性永不被消耗」前置与「部分消耗」可达性交互经 Phase 1 Proof 显式裁决）；Closure Gates 删除全仓 typecheck/build（只读）对齐 A4.1.15。无 Blocker/Major。2 Minors（M1 expander 交叉引用引用 plan 文件非 audit deliverable — cosmetic + 与 A4.1.15 同型；M2 从展开器「新增探针」收窄为只读推理 — 透明记录且 risk-reducing，closure auditor 关注即可）非阻塞。promote to active。

## Closure Gates

> 本计划为**只读余额守恒评估**（无代码/ORM/api.xml/view.xml/真相源变更；删除核心路径代码经只读探针评估，不修改），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 红冲余额回退 + delta 层删除哨兵 + 可达性 + 余额守恒一致性 + 测试覆盖边界 + 分级确认 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.16 验证报告红冲余额回退 + delta 层删除哨兵 + 可达性 + 余额守恒 + 测试覆盖边界 + 分级确认齐全 + P2-MA2-029 分级注记更新（若有）
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议（MA4↔A5.6 边界）一致；与 A1.5 §7-2 + §5.3 P2-MA2-029 复用 + A4.1.15 P2-RC-004（不同方向不同控制点）一致
- [x] 已运行验证：红冲余额回退 + delta 层删除哨兵 + 可达性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up（若升级 P1 是验证**输出**，非范围内项目降级；修复归 MR1 在 §Deferred But Adjudicated 预声明）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P2-MA2-029 FIFO 红冲 delta 层删除三方一致性测试补强（若 A4.1.16 升级 P1 后修复归口）

- Classification: `out-of-scope improvement`（本验证是余额守恒评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是余额守恒评估，结果表面 = 验证报告 + P2-MA2-029 分级确认。修复（若触及删除逻辑）归 MR1（R1.0→RC-R1.n），触及成本过账/删除核心路径须 ask-first。本验证闭环不阻塞于修复落地（修复是独立 plan）。
- Successor Required: yes（MR1 按本报告修复方向[若升 P1]展开，须补 FIFO delta 层红冲删除三方一致性测试[成本层 + 余额 + 流水]替代当前测试缺口）

## Closure

Status Note: 已完成。A4.1.16 经运行时余额守恒静态推理裁决：维持 P2-MA2-029 = P2（余额回退对称 + 哨兵精确删除 + 常态余额守恒成立；边角部分消耗漂移被 P2-RC-004[P1 forward] 同根因涵盖，按 §去重协议不重复升级）。报告落盘 `docs/audits/2026-08-06-1517-rc-ma4-a4-1-16-...md`；arm-index P2-MA2-029 行追加 A4.1.16 注记；roadmap A4.1.16 ✅。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 ses_029f9d2a8ffeINjvdXi7iPmrAc，不重用执行者上下文）— verdict `passes closure audit`，零 Blocker，3 非阻塞 Minor（P1⑤ 未显式列表但 P2 结论成立 / §去重协议应用为合理外推非字面条款锚定 / Plan Status+Closure 占位符为本审计前预期状态，均非交付缺陷）。
- Evidence: Task1-8 全 pass — file:line 证据零漂移（reverseLine:175-192 / removeFifoAdjustLayer:194-202 / appendFifoAdjustLayer:149-171 / applyFifo:140 / findFifoLayers:178-205 全部精确匹配 live repo HEAD 112a4b493）；§4 部分消耗可达性逻辑健全（onOutgoing 升序消耗循环 + 跨期场景证明确凿非 hand-waving）；§5.2 漂移算术复核精确（drift=100=50×Δ(2)）；§8 维持 P2 裁决可辩护（§2 P1① reverse 路径无独立缺陷 / §2 P2① 测试缺口命中 / §去重协议 不重复升级同根因）；§6 测试覆盖普查准确（FIFO delta 层 reverse 零覆盖证实）；arm-index 一致无重复 finding；git diff 仅 docs .md 变更（无保护区域代码修改）；§9 checker actual 匹配报告（R1-R2 计数与输出一致 + 诚实声明纯 reporter 非门控）。

Follow-up:

- MR1 修复 P2-MA2-029（若升 P1）：触及删除核心路径须 ask-first + 独立 plan-audit
- MR1 修复 P2-RC-004（P1 forward）：将一并消除 P2-MA2-029 的 reverse 漂移面（forward+reverse 同 delta 层结构紧耦合）+ 补 FIFO delta 层红冲三方一致性深断言测试
