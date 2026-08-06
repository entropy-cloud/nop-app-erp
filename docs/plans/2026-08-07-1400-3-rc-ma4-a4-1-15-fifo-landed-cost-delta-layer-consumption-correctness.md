# 2026-08-07-1400-3 rc-ma4-a4-1-15-fifo-landed-cost-delta-layer-consumption-correctness FIFO 物料到岸成本 delta 层后续出库消耗数值正确性评估

> Plan Status: active
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.1.15（MA4 运行时行为验证 — A1.5 §7-1：UC-FIN-10 LC-L3 FIFO 物料 + 到岸成本 delta 层 + 后续出库消耗的运行时数值正确性，关联 P2-RC-004；**触及成本过账行为探针**，但本验证为只读代码路径推理 + 既有测试覆盖普查，不修改成本过账核心路径代码）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.15；存疑点来源 `docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 1
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1815-2-rc-ma1-a1-5-finance-f5-costing.md`（A1.5 plan done）+ `docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md`（A1.5 报告 §7 存疑点 1 + §6 P2-RC-004 finding + §3 测试覆盖缺口）、`docs/plans/2026-08-06-1044-3-rc-ma4-a4-1-12-bank-recon-adj-voucher-line-correctness.md`（A4.1.12 done，数值正确性 + 断言强度评估同型范式）、`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（MA2 inventory costing 三方对账既有行为证据输入）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.15 验证报告（落盘 `docs/audits/2026-08-07-1400-rc-ma4-a4-1-15-fifo-landed-cost-delta-layer-consumption-correctness.md`）+ 必要时 arm-index finding/successor 登记。**不改代码/ORM/api.xml/真相源**（只读评估：读 `CostAdjustmentService.appendFifoAdjustLayer` delta 层追加逻辑 + 读 `FifoCostingStrategy.findFifoLayers/onOutgoing` delta 层消耗排序逻辑 + 复用 MA2/A1.5 + delta 层 unitCost=Δ 混合排序 Σ 数值正确性推理）。范式对齐 A4.1.12（已 done 的数值正确性 + 断言强度评估同型工作项）。

- **存疑点原文**（A1.5 报告 §7 存疑点 1，`2026-08-02-2045-...-a1-5-costing.md` §7）：「FIFO 物料 + 到岸成本 delta 层 + 后续出库消耗的运行时数值正确性」——delta 层 unitCost=Δ 被后续 FIFO 出库消耗时，出库成本是否正确含调整；多个 delta 层 + 原入库层混合排序时的 Σ 正确性。触发条件 = 实际启用 FIFO 物料的到岸成本分摊后再多笔出库。交 MA4 A4.1 运行时探针（补 E2E 即闭合 P2-RC-004）。

- **关联既有 finding**：
  - **P2-RC-004**（arm-index `:134`）：UC-FIN-10 LC-L3 FIFO 物料 + 到岸成本交互 E2E 测试缺口——`TestErpInvLandedCostEndToEnd` 物料均用 MOVING_AVERAGE（`:82-83 seedMaterial(matA, COST_METHOD_MOVING_AVERAGE)`），**FIFO 物料 + 到岸成本交互（delta 层追加 + 后续 FIFO 出库消耗更新后单价）无 E2E 测试**。LC-L3 的"后续出库按更新单价"语义在 FIFO 物料路径下经 `CostAdjustmentService.appendFifoAdjustLayer` 追加 delta 层实现，但无 E2E 断言。该交互在 MOVING_AVERAGE 路径下经 `applyAverageLike` 直接更新 `balance.avgCost` 已强覆盖，FIFO delta 层路径仅单测间接覆盖（`TestErpInvCostAdjust` MA 路径）。修复 = 补 FIFO 物料到岸成本 E2E 测试（纯测试补充），按 roadmap 预授权类目经 MR1 自动执行，不触发 §5 ask-first。**状态：todo（MR1 RC-R1.n 展开待修复）。**
  - 本验证**不重复登记** P2-RC-004（已登记），只评估 delta 层 FIFO 出库消耗的数值正确性（行为是否正确，还是仅测试缺失），确认/调整 P2-RC-004 分级（P2 测试覆盖补强维持 vs 若数值错误升 P1 行为缺陷）。

- **关联既有结论**：
  - A1.5 §5：UC-FIN-10 LC-L3 = **接受**（行为实现，FIFO 交互测试为 P2 watch-only）；P2-RC-004 = 测试覆盖补强项（非行为缺陷）。
  - MA2 A2.4 `2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`：到岸成本分摊的三方一致性经 `TestErpInvLandedCostEndToEnd` 4 场景覆盖（MA 主路径）；FIFO 物料到岸成本分摊 + 红冲边界为 costing-methods.md:66 已登记 successor（P2-MA2-029 维持）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:197` UC-FIN-10 LC-L3 逐字「后续出库按更新后队列单价」——FIFO 物料经到岸成本分摊后，后续出库成本须含调整。L2（`costing-methods.md §FIFO 出库逻辑 :224-249` + `§成本调整 :415-477`）描述 delta 调整层（unitCost=Δ、incomingMoveId=-lineId 哨兵、incomingDate=adjust.businessDate）+ FIFO 出库按 incomingDate 升序消耗。

- **实现现状（L3，实测锚点，本计划起草时核实，本切片实现全在 module-inventory）**：
  - delta 层追加：`CostAdjustmentService#applyFifo:137-147` → `appendFifoAdjustLayer:149-171`——新建 `ErpInvCostLayer` delta 调整层：`unitCost = newUnitCost - oldUnitCost`（`:162`，Δ 值）、`incomingMoveId = -line.getId()`（`:168`，负值哨兵区别正常移动单正 ID）、`incomingDate = adjust.businessDate`（`:165-166`）。
  - delta 层消耗（本存疑点核心）：`FifoCostingStrategy#findFifoLayers:178-205`（`:202-203` 按 `incomingDate` 升序排序，含原入库层 + delta 调整层混合排序）；`onOutgoing:105-120`（FIFO-F2 跨队列循环）+ `:114/118`（FIFO-F3 Σ 消耗量×单价）。
  - 正确性推理（静态）：delta 层 unitCost=Δ + incomingDate=adjust.businessDate → 按 incomingDate 升序消耗时，若 delta 层 incomingDate 晚于原入库层则先消耗原入库层（旧单价）再消耗 delta 层（含 Δ 的新单价）；多 delta 层 + 原入库层混合排序时 Σ 消耗量×单价 累加。语义 = 后续出库成本含调整。**关键正确性依赖**：FIFO 升序消耗保证 delta 层在正确时序被消耗 + unitCost=Δ 保证累计单价正确。

- **既有证据（复用输入）**：
  - MA2 A2.4：到岸成本分摊三方一致性（MA 主路径）已证实 + 成本调整 delta 层（`applyFifo:146` unitCost=新旧差值经 roundCost scale 4；reverse `removeFifoAdjustLayer:194-202` 按 `-lineId` 哨兵物理删除）行为已证实。本验证复用其「delta 层追加/删除逻辑正确」结论，**只补「FIFO 出库消耗 delta 层的数值正确性」差异**。
  - A1.5 §6 P2-RC-004：已静态确认 FIFO + 到岸成本交互 E2E 测试缺口。

- **初步实测（本计划起草时的部分核验，执行时复核）**：
  - grep `FifoCostingStrategy.java` `findFifoLayers|onOutgoing|incomingDate|ORDER BY`——确认 delta 层与原入库层混合按 incomingDate 升序消耗（A1.5 §2 已确认 :178-205 :202-203 :114/118）。
  - grep `TestErpInvFifoCosting` + `TestErpInvLandedCostEndToEnd` + `TestErpInvCostAdjust` 全集——A1.5 §3 已确认 `TestErpInvLandedCostEndToEnd` 物料均 MOVING_AVERAGE（`:82-83`），FIFO + 到岸成本交互 E2E 缺口；`TestErpInvFifoCosting#testOutgoingSpansMultipleLayersWeightedCost:111-143` 覆盖 FIFO 多层消耗 Σ（但非 delta 调整层，是正常入库层）。
  - 即本验证核心 = 评估「FIFO 出库消耗 delta 调整层」的数值正确性（行为是否正确，还是仅 E2E 测试缺失），确认 P2-RC-004 分级（P2 测试覆盖补强维持最可能：delta 层追加逻辑静态正确 + FIFO 升序消耗保证时序 + MA 路径单测间接覆盖；E2E 缺口属测试补强项非行为缺陷）。

- **剩余差距**：P2-RC-004 的 FIFO + 到岸成本 delta 层消耗数值正确性未运行时验证——delta 层 unitCost=Δ 被后续 FIFO 出库消耗时 Σ 是否正确含调整 + 多 delta 层混合排序正确性。本验证补全该数值正确性评估。

- **保护区域**：只读评估（读 delta 层追加/消耗代码路径 + 既有测试覆盖普查 + 数值正确性推理），不触及 ORM/会计过账逻辑/数据删除。**本验证为「成本过账行为探针」但经只读代码路径推理 + 既有测试普查执行，不修改成本过账核心路径代码（CostAdjustmentService/FifoCostingStrategy/LandedCostAllocationEngine 属保护区域，修改须 ask-first）**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（P2-RC-004 修复为补 E2E 测试[纯测试代码]，归 MR1 预授权类目）。

## Goals

- delta 层消耗数值正确性评估：核验 `CostAdjustmentService.applyFifo:146` Δ 计算（`newUnitCost.subtract(oldUnitCost)`）→ `appendFifoAdjustLayer:149-171`（delta 层 setUnitCost :162 + incomingMoveId=-lineId 哨兵 :168 + incomingDate=adjust.businessDate :165-166）+ `FifoCostingStrategy.findFifoLayers:178-205,202-203`（按 incomingDate 升序混合排序原入库层 + delta 层）+ `onOutgoing:105-120,114/118`（Σ 消耗量×单价）——评估「delta 层被后续 FIFO 出库消耗时出库成本是否正确含调整」+「多 delta 层 + 原入库层混合排序时 Σ 正确性」。
- 既有测试覆盖普查：grep `TestErpInvFifoCosting` + `TestErpInvLandedCostEndToEnd` + `TestErpInvCostAdjust` 全集，确认 FIFO delta 调整层消耗路径的测试覆盖边界（FIFO 多层 Σ 单测覆盖正常入库层非 delta 层 / 到岸成本 E2E 仅 MA 路径 / MA 路径单测间接覆盖 delta 层）。
- 对齐 UC-FIN-10 LC-L3 + `costing-methods.md §FIFO 出库逻辑 + §成本调整` 给出结论：确认/调整 P2-RC-004 分级——①若 delta 层追加 + FIFO 升序消耗逻辑静态正确（unitCost=Δ 累计正确 + 时序正确）+ MA 路径单测间接覆盖 → P2 测试覆盖补强维持（FIFO 交互 E2E 缺口属测试补强项非行为缺陷，A1.5 §5 接受维持）；②若发现 delta 层消耗数值有静态 bug（如 unitCost=Δ 累计错误 / 时序错位）→ 升 P1 行为缺陷（触发 MR1 优先修复，触及成本过账核心路径须 ask-first）。
- 产出验证报告 + §8 过程纪律自检；finding/successor（若有）按 §7 裁决登记 arm-index（P2-RC-004 已登记，本验证只更新分级注记或确认维持）。

## Non-Goals

- **不修复 P2-RC-004**（FIFO + 到岸成本交互 E2E 测试缺口——修复为补 FIFO 物料到岸成本 E2E 测试[FIFO 物料 receive → landed cost delta 层 → 后续 FIFO 出库消耗含 delta 单价断言]，纯测试代码，归 MR1 预授权类目，不触发 §5 ask-first）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不修改成本过账核心路径代码**（CostAdjustmentService/FifoCostingStrategy/LandedCostAllocationEngine/ErpInvLandedCostProcessor 属保护区域，修改须 ask-first + 独立 plan-audit；本验证为只读探针）。
- **不重新核实 UC-FIN-10 全部验收标准**（A1.5 §5 已判整体接受；本验证只评 FIFO delta 层消耗数值正确性差异）。
- **不展开 A1.5 §7-2/§7-3**（A4.1.16/A4.1.17 范围）。

## Task Route

- Type: `verification or audit work`（数值正确性评估 + P2-RC-004 分级确认/调整）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议 + MA4↔A5.6 边界）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.15 行）+ `docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 1 + §6 P2-RC-004 + §3 测试覆盖缺口（输入）+ `docs/design/finance/costing-methods.md §FIFO 出库逻辑 + §成本调整`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。数值正确性评估需多维度归类（delta 层追加逻辑 / FIFO 升序消耗排序 / Σ 累加正确性 / 既有测试覆盖边界 / MA4↔A5.6 边界 / P2 维持-or-升 P1 裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读 delta 层追加/消耗代码路径 + 既有测试覆盖普查 + 数值正确性推理）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - FIFO delta 层消耗数值正确性与测试覆盖边界评估

Status: planned
Targets: `docs/audits/2026-08-07-1400-rc-ma4-a4-1-15-fifo-landed-cost-delta-layer-consumption-correctness.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.15 行）；A1.5 done（§7 存疑点 1 已落盘 + §6 P2-RC-004 已登记 + §3 测试覆盖缺口已记录）

- [ ] `Proof` delta 层追加逻辑核验：给出 `CostAdjustmentService.applyFifo:146` Δ 计算（`newUnitCost.subtract(oldUnitCost)`）→ `appendFifoAdjustLayer:149-171` delta 层追加逻辑（setUnitCost :162 + incomingMoveId=-lineId 哨兵 :168 + incomingDate=adjust.businessDate :165-166）证据（file:line）。证实 delta 层 unitCost=Δ + 时序键 incomingDate=adjust.businessDate。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` FIFO 升序消耗 delta 层核验：读取 `FifoCostingStrategy.findFifoLayers:178-205,202-203`（按 incomingDate 升序排序查找 FIFO 层含原入库层 + delta 层混合排序）+ `onOutgoing:105-120,114/118`（跨队列循环 + Σ 消耗量×单价）决策逻辑——评估 delta 层在正确时序被消耗（delta 层 incomingDate 晚于原入库层则后消耗）+ 多 delta 层 + 原入库层混合排序 Σ 累加正确性。证实「后续出库按更新单价」语义在 FIFO 路径下经 delta 层实现。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 既有测试覆盖边界普查：grep `TestErpInvFifoCosting`（FIFO 多层 Σ 单测，覆盖正常入库层非 delta 层）+ `TestErpInvLandedCostEndToEnd`（到岸成本 E2E 仅 MA 路径，物料均 MOVING_AVERAGE :82-83）+ `TestErpInvCostAdjust`（成本调整单测 MA 路径间接覆盖 delta 层）全集，产出测试覆盖边界清单 + 标注 FIFO delta 层消耗 E2E 缺口。引用 A1.5 §3 已有评级依据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（FIFO delta 层消耗数值是否正确），与 A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角）边界按此执行（方法论 §去重协议 MA4↔A5.6）。本验证不重做 A5.6 E2E 断言强度审计，只评 delta 层消耗行为正确性 + 既有测试覆盖边界。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` P2-RC-004 分级确认/调整（方法论 §2 判据 + 三源对照）：①若 delta 层追加 + FIFO 升序消耗逻辑静态正确（unitCost=Δ 累计正确 + 时序正确）+ MA 路径单测间接覆盖 → P2 测试覆盖补强维持（FIFO 交互 E2E 缺口属测试补强项非行为缺陷，A1.5 §5 接受维持）；②若发现 delta 层消耗数值有静态 bug → 升 P1 行为缺陷（触发 MR1 优先修复，触及成本过账核心路径须 ask-first）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.5 §5 LC-L3 接受 + P2-RC-004 P2 结论分层一致。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] delta 层追加逻辑 + FIFO 升序消耗 + 测试覆盖边界证据落盘（全集，无遗漏），每条有证据（file:line）
- [ ] P2-RC-004 分级确认/调整有明确结论（P2 测试覆盖补强维持 / 升 P1 行为缺陷），与 A1.5 §5 LC-L3 接受 + P2-RC-004 P2 结论分层一致

### Phase 2 - finding/successor 衔接 + §8 自检 + 报告定稿

Status: planned
Targets: `docs/audits/2026-08-07-1400-rc-ma4-a4-1-15-fifo-landed-cost-delta-layer-consumption-correctness.md`（定稿）；`docs/audits/arm-index.md`（P2-RC-004 分级注记更新）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 数值正确性评估 + 分级确认完成

- [ ] `Add` P2-RC-004 分级注记更新：若 P2 维持 → 在 arm-index `:134` P2-RC-004 行追加「A4.1.15 运行时数值正确性评估确认 P2 维持」注记（含 delta 层消耗数值正确性结论 + 测试覆盖边界证据 + file:line）；若升 P1 → 在 P2-RC-004 行标注升级 + 触发 MR1 优先修复（触及成本过账核心路径须 ask-first）。禁止未经比对新建重复 finding。若维持接受则登记「无新 finding，归 A1.5 §5 LC-L3 接受 + §7 存疑点 1 闭合」。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.5 §6 P2-RC-004 / MA2 A2.4 costing 三方对账 / P2-MA2-029[reverse 路径物理删除，不同控制点] / A4.1.16[A1.5 §7-2 FIFO 红冲物理删除余额守恒] 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（delta 层追加 + FIFO 消耗 + 测试覆盖边界 + 分级确认 + finding 衔接 + §8 自检齐全）
- [ ] P2-RC-004 分级注记已更新入 arm-index（确认维持/升 P1）并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: needs revision (mission-driver 2026-08-04-224309 独立子代理 ses_02a713a17ffex836WBPDmZ3Vpu) — 3 Majors：①arm-index P2-RC-004 行号 `:298` 实为 `:134`；②L1 契约 use-cases.md LC-L3 行 `:183` 实为 `:197`；③Task Route costing-methods.md 路径 `docs/design/inventory/` 实为 `docs/design/finance/`（inventory/ 副本不存在）。2 Minors：Δ-subtract 在 `applyFifo:146`（存储在 `appendFifoAdjustLayer:162`）；升序排序 `:202-203` 在 `findFifoLayers` 内非 `onOutgoing`。范围/保护区域纪律/只读门控/格式/退出标准可测性均 sound，Deps 门控满足（expander completed）。
- Independent draft review iteration 2: accept (mission-driver 2026-08-04-224309 独立子代理 ses_02a6cec3dffeN6TuIXzA3WG3Ms) — 全 5 项修订逐项独立复核（live repo file:line 精度）：MAJOR1 `:134` ✅（arm-index P2-RC-004 行 = :134）/ MAJOR2 `:197` ✅（use-cases.md:197 逐字「后续出库按更新后的队列单价计算」）/ MAJOR3 finance/ 路径 ✅（finance/costing-methods.md 存在，inventory/ 副本不存在）/ MINOR1 Δ `applyFifo:146` subtract + `appendFifoAdjustLayer:162` setUnitCost ✅ / MINOR2 `:202-203` list.sort 在 findFifoLayers 内 ✅。其余 format/Exit Criteria/单一结果表面/5 项 Non-Goals（含 4 保护类）/只读门控/Deps 门控均 sound。无 Blocker/Major，promote to active。

## Closure Gates

> 本计划为**只读数值正确性评估**（无代码/ORM/api.xml/view.xml/真相源变更；成本过账核心路径代码经只读探针评估，不修改），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = delta 层追加逻辑 + FIFO 升序消耗 + 测试覆盖边界 + 分级确认 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.1.15 验证报告 delta 层追加 + FIFO 消耗 + 测试覆盖边界 + 分级确认齐全 + P2-RC-004 分级注记更新入 arm-index
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议（MA4↔A5.6 边界）一致；与 A1.5 §7-1 + §6 P2-RC-004 + §3 测试覆盖缺口 + §5 LC-L3 接受一致
- [ ] 已运行验证：delta 层追加 + FIFO 消耗 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P2-RC-004 FIFO + 到岸成本交互 E2E 测试补强

- Classification: `out-of-scope improvement`（已登记 P2-RC-004，修复归 MR1）
- Why Not Blocking Closure: 本计划是数值正确性评估，结果表面 = 验证报告 + P2-RC-004 分级确认。UC-FIN-10 LC-L3 已接受（A1.5 §5）；FIFO delta 层消耗数值正确性经静态推理（delta 层追加逻辑正确 + FIFO 升序消耗保证时序）评估。补 FIFO + 到岸成本交互 E2E 测试经 MR1（R1.0→RC-R1.n，纯测试代码修复预授权类目）。若裁决为接受（数值正确性充分）则 successor 维持 P2 测试补强。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 P2-RC-004 分级确认展开修复；若维持 P2 不触发 MR0 即时通道）

## Closure

Status Note: <待执行后填充>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理（新会话），待执行后填充>
- Evidence: <task id / log link / walkthrough record，待执行后填充>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
