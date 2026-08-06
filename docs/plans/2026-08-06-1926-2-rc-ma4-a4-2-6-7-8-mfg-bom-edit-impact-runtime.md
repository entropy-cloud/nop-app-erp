# 2026-08-06-1926-2 rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime BOM 内容编辑后已开工工单成本/物料需求运行时影响确认（P1-RC-009 家族，P0 升级候选）

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.2.6 + A4.2.7 + A4.2.8（合并：MA4 运行时行为验证 — A1.10 §7 SP-1/SP-2/SP-3 同根因[P1-RC-009 BOM 快照缺失]同控制点[BomExpander.loadLines 实时查 ErpMfgBomLine 无版本/快照门控]同 owner doc[manufacturing/]；SP-1 = BOM 编辑后已开工工单运行时是否按新 BOM 重算物料需求/成本；SP-2 = 快照缺失运行时是否致成本结转凭证错误；SP-3 = bomId 弱隔离运行时边界[运营 BOM 变更实践：编辑 vs 新建]）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.2.6 + A4.2.7 + A4.2.8；存疑点来源 `docs/audits/2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md` §7 SP-1/SP-2/SP-3
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.6/A4.2.7/A4.2.8 实体行）、`docs/plans/2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md`（A1.10 done，P1-RC-009 BOM 快照缺失已登记）、`docs/audits/arm-index.md`（P1-RC-009 finding 行）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份验证报告（落盘 `docs/audits/2026-08-06-1926-rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：grep BomExpander.explode 调用方全集 + 差异计算/成本重算路径是否经 BOM 重展开 + 完工 materialCost 计算路径 + ErpMfgBomLine 无版本/快照门控 + config `erp-mfg.variance-auto-calc-enabled` 默认值 + seed BOM 变更实践）。范式对齐 A4.1.15（done — FIFO 到岸成本 delta 层消耗正确性同成本核算运行时探针 + **P0 升级候选裁决**先例）。

- **存疑点原文**（A1.10 报告 §7 SP-1/SP-2/SP-3，`2026-08-02-2231-1-...-a1-10-...md` §7:235-237）：
  - **SP-1**：「BOM 内容编辑后已开工工单运行时是否实际按新 BOM 重算物料需求/成本。UC-MFG-10 快照缺失（P1-RC-009）的运行时会计影响。完工 materialCost = Σ 领料单成本聚合（不经 BOM 重展开，默认完工过账不受影响），但差异计算（`ErpMfgCostVarianceCalculator`）/成本重算路径若读 BOM（经 BomExpander.explode）则受同 bomId 内容编辑影响。需运行时确认：(a) 差异计算是否经 BOM 重展开读标准用量；(b) 成本重算路径是否读 BOM；(c) 齐套重算（checkAvailability 二次调用）是否在 BOM 编辑后读新内容。」触发条件：BOM 子件行编辑 + 已审核工单触发差异计算/重算/二次齐套。
  - **SP-2**：「快照缺失运行时是否致成本结转凭证错误。与 SP-1 同根因，聚焦 GL 凭证层面——若 SP-1 确认差异计算/重算读新 BOM，则 PRODUCTION_VARIANCE 凭证 + 成本结转凭证金额错误。需运行时确认凭证行级金额是否偏离审核时 BOM 内容。」触发条件：同 SP-1 + config `erp-mfg.variance-auto-calc-enabled=true`。**SP-1/SP-2 协同**闭合 P1-RC-009 会计影响裁决。
  - **SP-3**：「bomId 弱隔离运行时边界。`KitAvailabilityChecker.resolveBomId:134-137` wo.bomId 优先 → 新建 BOM（新 bomId）不影响已建工单。但运营若"编辑同一 bomId 的 BOM 内容"（而非新建 bomId）则无隔离。需运行时确认运营 BOM 变更实践（编辑 vs 新建）+ 是否存在 BOM 版本化实践。」触发条件：运营 BOM 变更操作。

- **关联既有 finding**：
  - **P1-RC-009**（arm-index）：UC-MFG-10 BOM 快照原则缺失——`BomExpander.loadLines` 实时查 `ErpMfgBomLine`，无版本/快照门控。工单 bomId 引用锁定提供"新建 BOM 不影响已建工单"的弱隔离（resolveBomId 优先用 wo.bomId），但**同 bomId 内容编辑无隔离**。L1 `use-cases.md` UC-MFG-10 要求 BOM 快照。§4 三判据核验 + Q4 会计/成本无例外 → P1。**本验证确认 P1-RC-009 的运行时会计影响，决定是否升 P0**（SP-1/SP-2 明确声明「若运行时确认致成本结转凭证错误，重新定级」）。

- **需求契约（L1 权威）**：`docs/design/manufacturing/use-cases.md` UC-MFG-10 BOM 与工艺路线。L1 要求 BOM 快照（审核时锁定 BOM 内容，后续编辑不影响已审核工单的物料需求/成本计算）。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实）**：
  - **BomExpander**（`module-manufacturing/erp-mfg-service/.../bom/BomExpander.java`）：`explode(bomId, requestedQty, useMultiLevel)` → `loadLines` 实时查 `ErpMfgBomLine`，无版本/快照门控。
  - **调用方全集**（BomExpander.explode 消费方，待运行时 census）：
    - `KitAvailabilityChecker`（齐套校验：`bomExpander.explode(bomId, plannedQty, true)` + resolveBomId wo.bomId 优先）
    - `MrpEngine`（MRP 净需求：`bomExpander.explode(bom.getId(), planned, false)`）
    - `SimulationMrpEngine`（仿真：`bomExpander.explode(...)`）
    - 差异计算/成本重算路径（`ErpMfgCostVarianceCalculateVariancesProcessor` / `ErpMfgCostVarianceBizModel` / CostRollupService）——**待运行时确认是否经 BomExpander.explode 读 BOM 标准用量**
  - **完工 materialCost 计算路径**（A1.10 §5 已静态确认）：完工 materialCost = Σ 领料单成本聚合，**不经 BOM 重展开** → 完工过账默认不受 BOM 编辑影响。
  - **config**：`ErpMfgConstants.CONFIG_VARIANCE_AUTO_CALC_ENABLED = "erp-mfg.variance-auto-calc-enabled"`（默认 **false**——`ErpMfgConstants.java:171-173` 注释「默认 false=完工不自动触发差异计算」+ `ErpMfgWorkOrderProcessor.java:397 readBoolConfig(..., false)`；A1.10 报告记录差异计算 config-gated）。默认 false 意味着差异计算路径**非默认活跃**，P0 升级裁决须考虑 config-gated 因素。

- **既有证据（复用输入）**：
  - A1.10 §5（P1-RC-009 静态裁决：§4 三判据 + Q4 会计/成本无例外 → P1）
  - A1.10 §7 SP-1/SP-2/SP-3（静态存疑点清单）
  - A4.2a（MRP 代码质量审计，完工 materialCost 不经 BOM 重展开结论）

- **剩余差距**：BOM 内容编辑后差异计算/成本重算/二次齐套路径是否**实际读新 BOM 内容**（经 BomExpander.explode 实时查 ErpMfgBomLine）未做运行时确认。若确认 → P1-RC-009 升 P0（成本结转凭证错误，活跃会计数据破坏），触发 MR0；若否（差异/重算路径不经 BOM 重展开）→ 维持 P1（快照缺失仍是合规缺口但不致运行时凭证错误）。本验证闭合 P1-RC-009 的运行时会计影响裁决。

- **保护区域**：只读评估（grep BomExpander.explode 调用方 + 差异/重算/齐套路径 census + config 默认值 + seed BOM 变更实践），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若升 P0，登记 finding 触发 MR0 即时通道；修复触及 BOM 快照机制可能触及 ORM 结构[ErpMfgBomLine 版本/快照字段]须 ask-first + 独立 plan-audit）。

## Goals

- BomExpander.explode 调用方全集 census：grep 全 module-manufacturing `bomExpander.explode` / `BomExpander` 消费方，产出调用方矩阵（调用方 × 调用方法 × 是否传 bomId × 是否经 loadLines 实时查 ErpMfgBomLine）。
- 差异计算路径 BOM 重展开核验（SP-1 核心）：核验 `ErpMfgCostVarianceCalculateVariancesProcessor` / `ErpMfgCostVarianceBizModel` 是否经 BomExpander.explode 读 BOM 标准用量计算差异。给出 file:line 证据确认差异计算是否读新 BOM 内容。
- 成本重算路径 BOM 读取核验（SP-1 协同）：核验 CostRollupService / 成本重算路径是否经 BomExpander.explode 读 BOM。确认成本重算是否受 BOM 编辑影响。
- 二次齐套 BOM 编辑后读新内容核验（SP-1 协同）：核验 KitAvailabilityChecker.checkAvailability 二次调用（STOCK_PARTIAL 强制开工后补料齐套重算）是否在 BOM 编辑后读新内容（BomExpander.explode 实时查 ErpMfgBomLine）。
- GL 凭证影响裁决（SP-2 核心）：若 SP-1 确认差异/重算读新 BOM → 核验 PRODUCTION_VARIANCE 凭证 + 成本结转凭证行级金额是否偏离审核时 BOM 内容。若 SP-1 否定（差异/重算不经 BOM 重展开）→ 确认完工过账默认不受影响（materialCost = Σ 领料单成本聚合不经 BOM）。
- bomId 弱隔离运营实践核验（SP-3）：核验 seed/测试/部署文档中 BOM 变更实践（编辑同 bomId vs 新建 bomId）+ 是否存在 BOM 版本化实践（同 bomId 多版本）。
- config `erp-mfg.variance-auto-calc-enabled` 默认值核验：确认差异计算 config 默认 on/off（决定差异计算路径活跃性）。
- **P0 升级裁决**（SP-1/SP-2 明确声明）：①若差异计算/重算经 BOM 重展开读新内容 + config 默认 on → **P1-RC-009 升 P0**（成本结转凭证错误，活跃会计数据破坏），触发 MR0 即时通道；②若差异/重算不经 BOM 重展开（materialCost = Σ 领料单成本聚合不经 BOM）→ 维持 P1-RC-009（快照缺失合规缺口但不致运行时凭证错误）。裁决须列明 §2 判据编号 + L1/L2/L3 三源。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复 BOM 快照机制**（若升 P0 触发 MR0，本计划仅登记不实施修复；修复触及 BOM 快照[ErpMfgBomLine 版本/快照字段]属 ORM 结构变更须 ask-first + 独立 plan-audit）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 UC-MFG-02/10 全部验收标准**（A1.10 §5 已判 UC-MFG-02 接受 + UC-MFG-10 P1[P1-RC-009]；本验证只评 P1-RC-009 运行时会计影响差异）。
- **不展开 A1.10 §7 其他存疑点**（本验证仅覆盖 SP-1/SP-2/SP-3 BOM 编辑影响家族）。
- **不重审 P1-RC-009 的 P1 定级本身**（A1.10 §5 已裁决 P1；本验证只评是否升 P0 的运行时证据）。
- **不展开 A1.8/A1.9 §7 SP-3**（预留实现后 reservedQty/availableQuantity 一致性 = MR1 修复落地后 successor，Deps 不满足，归独立工作项 A4.2.3）。
- **不实际执行 BOM 编辑注入重现**（只读 BomExpander.explode 调用方 census + 差异/重算/齐套路径推理 + config/seed 普查；真实 BOM 编辑注入重现属 MR0/MR1 修复验证范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（BOM 内容编辑后已开工工单成本/物料需求运行时影响确认 + P1-RC-009 P0 升级裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.2.6 + A4.2.7 + A4.2.8 行）+ `docs/audits/2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md` §7 SP-1/SP-2/SP-3 + §5 P1-RC-009 裁决（输入）+ `docs/design/manufacturing/`（BOM/成本 owner doc）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。BOM 编辑影响评估需多维度归类（BomExpander.explode 调用方 census / 差异计算路径 / 成本重算路径 / 二次齐套路径 / 完工 materialCost 计算路径 / config 默认值 / bomId 弱隔离运营实践 / P1-RC-009 P0 升级裁决 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep BomExpander.explode 调用方 + 差异/重算/齐套路径 census + config 默认值 + seed BOM 变更实践）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - BomExpander.explode 调用方 census + 差异/重算/齐套路径 BOM 重展开核验

Status: completed
Targets: `docs/audits/2026-08-06-1926-rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`（6/6 Proof items；Phase 2 含 Decision/Add）
- Prereqs: A4.2 done（展开器已追加 A4.2.6/A4.2.7/A4.2.8 行）；A1.10 done（§7 SP-1/SP-2/SP-3 已落盘 + §5 P1-RC-009 裁决已登记）

- [x] `Proof` BomExpander.explode 调用方全集 census：grep 全 module-manufacturing `bomExpander.explode` / `BomExpander` 消费方，产出调用方矩阵（调用方类 × 方法 × 调用行号 × 是否传 bomId × 是否经 loadLines 实时查 ErpMfgBomLine）。确认 BomExpander.loadLines 无版本/快照门控（实时查 ErpMfgBomLine）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 差异计算路径 BOM 重展开核验（SP-1 核心）：核验 `ErpMfgCostVarianceCalculateVariancesProcessor` / `ErpMfgCostVarianceBizModel` 是否经 BomExpander.explode 读 BOM 标准用量计算差异。给出 file:line 证据。确认差异计算是否读新 BOM 内容（受 BOM 编辑影响）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 成本重算路径 BOM 读取核验（SP-1 协同）：核验 CostRollupService / CostRollup / 成本重算路径是否经 BomExpander.explode 读 BOM。确认成本重算是否受 BOM 编辑影响。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 二次齐套 BOM 编辑后读新内容核验（SP-1 协同）：核验 KitAvailabilityChecker.checkAvailability 二次调用是否在 BOM 编辑后读新内容（BomExpander.explode 实时查 ErpMfgBomLine）。给出 resolveBomId + explode 调用链 file:line 证据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` config `erp-mfg.variance-auto-calc-enabled` 默认值核验：确认差异计算 config 默认 on/off（决定差异计算路径活跃性）。grep ErpMfgConstants + application.yaml + AppConfig 消费点。**已知 baseline**：默认 **false**（`ErpMfgConstants.java:171-173` 注释 + `ErpMfgWorkOrderProcessor.java:397 readBoolConfig(..., false)`），Phase 1 复核 application.yaml 部署 override 是否存在。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（BOM 编辑是否致成本凭证错误），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] BomExpander.explode 调用方 census 矩阵落盘（全集，无遗漏），每条有证据（file:line）
- [x] 差异/重算/齐套路径 BOM 重展开核验有明确结论（读新 BOM / 不经 BOM 重展开），每条有证据（file:line）

### Phase 2 - GL 凭证影响裁决 + P0 升级裁决 + bomId 弱隔离实践 + finding 衔接 + §8 自检

Status: completed
Targets: `docs/audits/2026-08-06-1926-rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime.md`（定稿）；`docs/audits/arm-index.md`（P1-RC-009 注记更新或 P0 登记，若有）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 BomExpander.explode 调用方 census + 差异/重算/齐套路径核验完成

- [x] `Proof` GL 凭证影响裁决（SP-2 核心）：若 Phase 1 确认差异/重算经 BOM 重展开 → 核验 PRODUCTION_VARIANCE 凭证 + 成本结转凭证行级金额偏离审核时 BOM 内容的实际路径（file:line）；若 Phase 1 否定 → 确认完工过账默认不受影响（materialCost = Σ 领料单成本聚合不经 BOM，给出 file:line 证据）。
      - Skill: none
- [x] `Proof` bomId 弱隔离运营实践核验（SP-3）：核验 seed/测试/部署文档中 BOM 变更实践（编辑同 bomId vs 新建 bomId）+ 是否存在 BOM 版本化实践。grep seed BOM 数据 + ErpMfgBomLine 版本字段（是否存在 version 列）。
      - Skill: none
- [x] `Decision` P1-RC-009 P0 升级裁决（方法论 §2 判据 + 三源对照）：①若差异计算/重算经 BOM 重展开读新内容 + config 默认 on（`erp-mfg.variance-auto-calc-enabled=true`）→ **P1-RC-009 升 P0**（成本结转凭证错误，活跃会计数据破坏，§2 P0① 活跃数据破坏），触发 MR0 即时通道（本计划仅登记不实施修复）；②若差异/重算不经 BOM 重展开（materialCost = Σ 领料单成本聚合不经 BOM）→ 维持 P1-RC-009（快照缺失合规缺口但不致运行时凭证错误）；③若差异计算经 BOM 重展开但 config 默认 **off**（`false`，实测 `ErpMfgConstants.java:171-173` + `ErpMfgWorkOrderProcessor.java:397`）→ 维持 P1-RC-009（非默认活跃路径，config-enable 时方有风险，登记 config-enable 运营注意）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.10 §5 P1 裁决分层一致。
      - Skill: none
- [x] `Add` finding/注记更新：若升 P0 → arm-index P1-RC-009 行追加 P0 升级注记 + 触发 MR0 追加 R0.n 实体行（本计划记录「已触发 MR0」）；若维持 P1 → arm-index P1-RC-009 行追加运行时会计影响确认注记（不影响凭证/影响凭证二选一，按裁决）。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 P1-RC-009 / A1.10 §5/§7 / A4.2a 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（调用方 census + 差异/重算/齐套路径核验 + GL 凭证影响裁决 + P0 升级裁决 + bomId 弱隔离实践 + finding 衔接 + §8 自检齐全）
- [x] P1-RC-009 注记更新或 P0 登记已写入 arm-index + 若升 P0 已记录 MR0 触发

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_029279a7affe2LeMkP6536Z3m7，fresh session，未起草本计划）— 全 10 checklist 项 PASS（A 格式完整 / B Deps 满足[A4.2 展开器 done] / C 规则14 合并成立[同根因 P1-RC-009 + 源报告 §7 SP-1/SP-2 明确"同根因"+"协同闭合 P1-RC-009 会计影响裁决"] / D 单一结果表面 / E baseline 零信任核验[BomExpander/CostVarianceProcessor/CostVarianceBizModel/KitAvailabilityChecker/ErpMfgConstants:173 全存在 + P1-RC-009 arm-index:144 + §7 逐字匹配] / F 反松弛 / G item typing / H Skill / I 保护区域 / J 无矛盾）。零 Blocker。Non-blocking 已吸收：①Decision 框架中间分支[config off + BOM 重展开 → 维持 P1]已补为分支③；②config 默认值 baseline 从"待核验"更新为实测 false[ErpMfgConstants.java:171-173 + ErpMfgWorkOrderProcessor.java:397]；③Phase 1 item typing 从 Proof|Decision 修正为 Proof[6/6]；④Non-Goals 补 A4.2.3 排除声明。共识达成，转 active。

## Closure Gates

> 本计划为**只读 BOM 编辑影响评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 调用方 census + 差异/重算/齐套路径核验 + GL 凭证影响裁决 + P0 升级裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.2.6 + A4.2.7 + A4.2.8 验证报告调用方 census + 差异/重算/齐套路径核验 + GL 凭证影响裁决 + P0 升级裁决齐全 + finding/注记更新
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.10 §7 SP-1/SP-2/SP-3 + §5 P1-RC-009 裁决一致
- [x] 已运行验证：调用方 census + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### BOM 快照机制修复（若 P1-RC-009 维持 P1 或升 P0 后修复归口）

- Classification: `out-of-scope improvement`（本验证是影响评估，修复归 MR0/MR1）
- Why Not Blocking Closure: 本计划是影响评估，结果表面 = 验证报告 + finding/注记登记。修复归 MR0（若升 P0 即时通道）/ MR1（R1.0→RC-R1.n），修复触及 BOM 快照机制[ErpMfgBomLine 版本/快照字段]属 **ORM 结构变更须 ask-first + 独立 plan-audit**（§5 ORM 类保护区域）。本验证闭环不阻塞于修复落地（finding 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（MR0[若升 P0]/MR1 R1.0 展开器读取本报告 finding → RC-R1.n 修复，按报告裁决方向：①不经 BOM 重展开→快照仍须实现[P1 合规缺口]；②经 BOM 重展开→须阻断编辑影响已审核工单[P0 即时修复]）

## Closure

Status Note: completed — 全部 Phase 执行完毕（只读验证，无代码/ORM/api.xml/真相源变更）。裁决：维持 P1-RC-009 = P1（不升 P0，不触发 MR0），与 A1.10 §5/§5.3 分层一致。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0290e2b1cffetya9v2f7dbD4RB（fresh session，未执行本计划）。Verdict = **passes closure audit**。
- 全部 file:line 证据（claims A-G）经独立复核 live source 全部确认：BomExpander 5 注入方 + ProductionVarianceCalculator 零 BomExpander 使用（A）；差异材料标准 = FIRMED rollup 非 explode（B `:114,129`）；CostRollupService 自有 loadLines 读实时 + 产 CALCULATED（C `:103,162,317`）；KitAvailabilityChecker.explode 读实时（D `:66,134-135`）；完工 materialCost = Σ 领料单（E `ErpMfgMaterialIssueConfirmProcessor:121`）；config 默认 false 三源零 override（F）；ErpMfgBomLine 仅乐观锁 version/delVersion 无快照列（G `orm.xml:249-250`）。
- 五维度全 PASS：需求正确性 / owner-doc 对齐 / 证据健全 / closure 一致性 / 范围漂移。P0 升级裁决逻辑成立（材料标准冻结 FIRMED + 完工不经 BOM + config 默认 off → 无活跃会计数据破坏，§2 P0①/P0④ 不成立）。
- closure 一致性确认：Plan Status=completed + 两 Phase Status=completed + 全 Phase items/Exit Criteria [x] + roadmap A4.2.6/7/8=done ✅ + arm-index P1-RC-009 注记已追加。
- Non-blocking residual risks（不阻塞 closure）：①运营 FIRM 纪律依赖（若 FIRM 编辑后 BOM 的卷算则差异标准间接反映编辑内容，报告已记 config-enable 运营注意）；②二次齐套 live 读归 P1 待 MR1；③MR1 BOM 快照修复触及 ORM ask-first successor。
- §8 过程纪律自检：nop-compliance-checker.sh 已运行（纯 reporter，本计划零代码变更无回归风险，无引入漂移）；MA4↔A5.6 边界声明；与 arm-index 交叉去重声明齐全。

Follow-up:

- MR0/MR1 修复 BOM 快照机制（若登记 finding）：ORM 结构变更须 ask-first + 独立 plan-audit
