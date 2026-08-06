# 2026-08-06-2025-2 rc-ma4-a4-2-5-mfg-rejected-rework-workflow-traceability REJECTED 工单返工工作流 + 关联原工单可追溯性运行时确认（A1.9 SP-2 + A1.31 SP-2 合并，P2-RC-041 successor 运行时核验）

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.2.5（A1.9 SP-2 + A1.31 SP-2 合并：MA4 运行时行为验证 — 完工质检 REJECTED 后返工工单运行时操作流程 + 关联原工单可追溯性，同根因[config-gated 门控工单保持 IN_PROCESS 不在终态 + 操作员手动新建返工工单 + 无 originalWorkOrderId 自动关联]同控制点[REJECTED 工单后续可达动作 + 可追溯性机制]；mfg 侧 A1.9 SP-2 + quality 侧 A1.31 SP-2 同一存疑点[两报告 §7 明确标注「与 A1.9 SP-2 合并」]）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.2.5；存疑点来源 `docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md` §7 SP-2 + `docs/audits/2026-08-05-1830-2-rc-ma1-a1-31-quality-f1-inspection-gating.md` §7 SP-2（A1.31 §7:254 明确标注「与 A1.9 SP-2 合并」）
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.5 实体行）、`docs/audits/arm-index.md`（P2-RC-041 finding 行，successor watch-only）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份验证报告（落盘 `docs/audits/2026-08-06-2025-rc-ma4-a4-2-5-mfg-rejected-rework-workflow-traceability.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：REJECTED 门控 + 操作员后续可达动作 census + 可追溯性机制 census + 跨域 NCR↔工单关联现状）。范式对齐 A4.2.6-8（done — mfg 运行时探针先例）。

- **存疑点原文**：
  - **A1.9 §7 SP-2**（`2026-08-02-2042-3-...-a1-9-...md` §7:311）：「**UC-MFG-09 返工工单运行时操作流程**：L1 ㉕字面『原工单不可恢复（终态），新建返工工单（关联原工单）』，实现为 config-gated 门控（工单保持 IN_PROCESS 不在终态）+ 操作员手动新建标准工单（无 originalWorkOrderId 关联字段）。运行时操作员面对 REJECTED 工单时的实际工作流（是否手动关闭原工单→新建返工工单 / 或重置质检状态重新报工 / 或经 useLogicalDelete）需运行时确认，验证『关联原工单』的可追溯性是否经工单备注/手工关联实际可达。」触发条件：完工质检 REJECTED + 工单保持 IN_PROCESS。
  - **A1.31 §7 SP-2**（`2026-08-05-1830-2-...-a1-31-...md` §7:254）：「**UC-QA-04 完工返工跨域触发链运行时操作流程**（与 A1.9 SP-2 同一存疑点，quality 侧投影）：quality FINAL REJECTED + NCR 生成后，mfg 侧工单保持 IN_PROCESS，操作员面对 REJECTED 工单时实际工作流（手动关闭原工单→新建返工工单 / 重置质检状态重新报工 / 经 useLogicalDelete）+ 『关联原工单』可追溯性是否经工单备注/手工关联实际可达。」触发条件：完工质检 REJECTED + 工单保持 IN_PROCESS。

- **关联既有 finding**：
  - **P2-RC-041**（arm-index，successor watch-only，A1.31 quality 侧投影）：UC-QA-04 完工返工跨域触发链 successor（mfg 侧闭环经 A1.9 证实，quality 侧仅生成 NCR，自动建返工工单属 owner doc §3 successor）。**本验证确认 P2-RC-041 successor 的运行时现状**——操作员返工工作流可达性 + 可追溯性现状。若运行时确认手动工作流可达 + 可追溯性经工单备注/手工关联可达 → 维持 P2-RC-041 watch-only；若手动工作流受阻 + 可追溯性完全不可达 → 评估是否升 P1（主路径阻断）。

- **需求契约（L1 权威）**：`docs/design/manufacturing/use-cases.md` UC-MFG-09 完工质检不合格→返工工单（㉕「原工单不可恢复（终态），新建返工工单（关联原工单）」）+ `docs/design/quality/use-cases.md` UC-QA-04 完工检验不合格→返工。L2 `inspection-integration.md §2.3` + `§7.2` + `state-machine.md §场景D` 一致声明事件解耦 + 操作员驱动简化范式（owner doc §3 successor 触发条件）。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实）**：
  - **mfg 侧门控**（工单保持 IN_PROCESS 不在终态）：`ErpMfgWorkOrderReportCompletionProcessor.java:52` `int gate = InspectionTrigger.enforceGate(facade.inspectionBiz, ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, ...)`（REJECTED→工单不进终态）。
  - **无 originalWorkOrderId 关联字段**（已 live 核实）：全 `module-manufacturing` grep `originalWorkOrderId` **零命中**（无自动建返工工单代码 + 无关联字段）。
  - **quality 侧**：`ErpQaInspectionFailInspectionProcessor`（FINAL REJECTED→NCR via `NcrLifecycleService.autoCreateNcrFromInspection`，**不直接跨域建返工工单**）；grep `rework|ReworkOrder|createRework|返工工单` 跨 `module-quality/erp-qa-service/src/main` 零业务命中。
  - **useLogicalDelete**（工单软删机制，待 Phase 1 census 其在返工工作流中的角色）。
  - **工单状态机**：mfg 工单状态机（DRAFT→APPROVED→IN_PROCESS→COMPLETED/CANCELLED，待 Phase 1 census REJECTED 工单的可达后续迁移——手动 CANCELLED / 重新报工 / 软删）。

- **既有证据（复用输入）**：
  - A1.9 §5（UC-MFG-09 倾向接受：mfg 门控闭环 + 操作员手动新建返工工单承载「返工」语义）
  - A1.31 §5（UC-QA-04 倾向接受 / P2-RC-041 successor watch-only，§4 (i) A1.9 plan-audit 通过 → documented simplification 成立）
  - A1.9/A1.31 §6.1（P2-RC-041 finding 交叉引用 + §去重协议 MA1 同 mission 兄弟切片互补不重复）

- **剩余差距**：(a) 操作员面对 REJECTED 工单（IN_PROCESS）的**实际可达后续动作**未做运行时 census（手动 CANCELLED / 新建返工工单 / 重置质检重新报工 / useLogicalDelete 哪些可达）；(b) 「关联原工单」可追溯性机制现状未确认（工单备注？手工字段？工单号引用？或完全不可达）；(c) 跨域 NCR↔工单关联现状未确认（NCR 是否记录源工单 ID 供反向追溯）。本验证闭合 P2-RC-041 successor 的运行时现状裁决。

- **保护区域**：只读评估（grep REJECTED 门控 + 操作员可达后续动作 + 可追溯性机制 + 跨域 NCR↔工单关联），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若升 P1 触发 MR1；修复实现 originalWorkOrderId 自动关联属 ORM 结构变更须 ask-first + 独立 plan-audit §5）。

## Goals

- **REJECTED 门控运行时行为 census**（SP-2 核心）：核验 `ErpMfgWorkOrderReportCompletionProcessor:52` `enforceGate` 在 REJECTED 时的实际行为（工单保持 IN_PROCESS 不进终态），给出 file:line 证据 + 状态机迁移确认。
- **操作员可达后续动作 census**（SP-2 核心）：核验 REJECTED 工单（IN_PROCESS）后续可达动作全集——手动 CANCELLED（状态迁移 mutation）/ 新建返工工单（标准 create 流程）/ 重置质检状态重新报工 / useLogicalDelete（软删）。给出 file:line + xbiz mutation 注册证据，判定哪些动作运营可达。
- **可追溯性机制 census**（SP-2 核心）：核验「关联原工单」可追溯性现状——工单备注字段（remark/notes）是否承载关联 / 手工工单号引用 / originalWorkOrderId（已确认不存在）/ 其他关联载体。给出 file:line 证据，判定可追溯性"可达/部分可达/不可达"。
- **跨域 NCR↔工单关联 census**：核验 quality NCR（`autoCreateNcrFromInspection`）是否记录源工单 ID（relatedBillType/relatedBillId/源工单字段）供反向追溯——NCR→源工单追溯是否可达（弥补 mfg 侧 originalWorkOrderId 缺失的替代追溯路径）。
- **裁决**（方法论 §2 判据 + 三源对照）：①手动工作流可达（CANCELLED + 新建返工工单）+ 可追溯性经工单备注/NCR 反向关联可达 → **维持 P2-RC-041 watch-only**（successor[自动建返工工单 + originalWorkOrderId]非阻断主路径，手动闭环成立）；②手动工作流可达但可追溯性完全不可达 → 评估升 P1（追溯链断裂，合规风险）；③config-gated 门控默认行为复核（强制质检门控 config 默认值，决定 REJECTED 路径默认活跃性）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.9/A1.31 §5 P2-RC-041 successor 裁决分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不实现 originalWorkOrderId 自动关联 / 自动建返工工单**（属 MR1，触及 ORM 结构变更[ErpMfgWorkOrder 加 originalWorkOrderId 关联字段]须 ask-first + 独立 plan-audit §5；本计划仅登记不实施修复）。
- **不修改代码/ORM/api.xml/BizModel/Processor/真相源**（只读评估）。
- **不重新核实 UC-MFG-09/UC-QA-04 全部验收标准**（A1.9/A1.31 §5 已判倾向接受；本验证只评 P2-RC-041 successor 运行时现状差异）。
- **不重审 P2-RC-041 的 watch-only 定级本身**（A1.31 §5 已裁决；本验证只评是否升 P1 的运行时证据）。
- **不展开 A1.9 SP-1**（差异过账失败告警通道，归 A4.2.4 独立工作项）。
- **不展开 A1.9 SP-3**（预留实现后一致性，MR1 修复落地后 successor，归 A4.2.3）。
- **不展开 A1.31 SP-1/SP-3/SP-4/SP-5**（关键项否决/类别级模板/作废联动/强制质检门控 config，各自独立工作项）。
- **不实际执行 REJECTED 注入重现操作员行为**（只读门控 census + 可达动作 + 可追溯性机制推理 + config 普查；真实操作员行为调研属运维范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（REJECTED 工单返工工作流 + 关联原工单可追溯性运行时确认 + P2-RC-041 successor 运行时现状裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.2.5 行）+ `docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md` §7 SP-2 + §5 UC-MFG-09 倾向接受（输入）+ `docs/audits/2026-08-05-1830-2-rc-ma1-a1-31-quality-f1-inspection-gating.md` §7 SP-2 + §5 UC-QA-04 / P2-RC-041（输入）+ `docs/design/manufacturing/`（返工工单 owner doc）+ `docs/design/quality/`（完工返工 owner doc）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。返工工作流评估需多维度归类（REJECTED 门控 / 操作员可达动作 / 可追溯性机制 / 跨域 NCR↔工单关联 / config 默认值 / P2-RC-041 successor 裁决 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep REJECTED 门控 + 操作员可达动作 + 可追溯性机制 + 跨域 NCR↔工单关联 + config 默认值）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - REJECTED 门控 + 操作员可达后续动作 census + 可追溯性机制 census

Status: planned
Targets: `docs/audits/2026-08-06-2025-rc-ma4-a4-2-5-mfg-rejected-rework-workflow-traceability.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`（Phase 1 全 Proof）
- Prereqs: A4.2 done（展开器已追加 A4.2.5 行）；A1.9 + A1.31 done（§7 SP-2 已落盘 + §5 P2-RC-041 successor 已登记）

- [ ] `Proof` REJECTED 门控运行时行为 census：核验 `ErpMfgWorkOrderReportCompletionProcessor:52` `InspectionTrigger.enforceGate` 在 REJECTED 时的实际行为（工单保持 IN_PROCESS 不进终态）+ 状态机迁移确认。给出 file:line 证据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 操作员可达后续动作 census：核验 REJECTED 工单（IN_PROCESS）后续可达动作全集——手动 CANCELLED（状态迁移 mutation @BizMutation）/ 新建返工工单（标准 create 流程）/ 重置质检状态重新报工 / useLogicalDelete（软删）。给出 file:line + xbiz mutation 注册证据，判定哪些动作运营可达。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 可追溯性机制 census：核验「关联原工单」可追溯性现状——工单备注字段（remark/notes）是否承载关联 / 手工工单号引用 / originalWorkOrderId（已确认不存在）/ 其他关联载体。给出 file:line 证据，判定可追溯性"可达/部分可达/不可达"。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 跨域 NCR↔工单关联 census：核验 quality NCR（`NcrLifecycleService.autoCreateNcrFromInspection`）是否记录源工单 ID（relatedBillType/relatedBillId/源工单字段）供反向追溯——NCR→源工单追溯是否可达（弥补 mfg 侧 originalWorkOrderId 缺失的替代追溯路径）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 强制质检门控 config 默认值复核：核验强制质检门控 config（mandatory-inspection-bill-types / enforceGate 触发条件）默认值 + 全 application.yaml 部署 override 普查（决定 REJECTED 路径默认活跃性）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（返工工作流可达性 + 可追溯性），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] REJECTED 门控 + 操作员可达后续动作 census 有明确结论（哪些动作可达），每条有证据（file:line）
- [ ] 可追溯性机制 census 有明确结论（可达/部分可达/不可达），每条有证据（file:line）

### Phase 2 - 运行时工作流裁决 + finding 衔接 + §8 自检

Status: planned
Targets: `docs/audits/2026-08-06-2025-rc-ma4-a4-2-5-mfg-rejected-rework-workflow-traceability.md`（定稿）；`docs/audits/arm-index.md`（P2-RC-041 注记更新或升级登记，若有）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 REJECTED 门控 + 可达动作 + 可追溯性机制 census 完成

- [ ] `Decision` P2-RC-041 successor 运行时现状裁决（方法论 §2 判据 + 三源对照）：①手动工作流可达（CANCELLED + 新建返工工单）+ 可追溯性经工单备注/NCR 反向关联可达 → **维持 P2-RC-041 watch-only**（successor 非阻断主路径，手动闭环成立）；②手动工作流可达但可追溯性完全不可达 → 评估升 P1（追溯链断裂，合规风险，须人工确认 product-scope）；③config 默认 off → REJECTED 路径非默认活跃，登记 config-enable 运营注意。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.9/A1.31 §5 P2-RC-041 successor 分层一致。
      - Skill: none
- [ ] `Add` finding/注记更新：若升 P1 → arm-index P2-RC-041 行追加 P1 升级注记 + 触发 MR1 R1.0 展开器读取（本计划记录「归 MR1」，修复实现 originalWorkOrderId 触及 ORM ask-first）；若维持 watch-only → arm-index P2-RC-041 行追加运行时现状确认注记。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 P2-RC-041 / A1.9 §5/§7 / A1.31 §5/§7 的复用关系 + MA4↔A5.6 边界 + MA1 同 mission 兄弟切片互补不重复）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（REJECTED 门控 + 可达动作 + 可追溯性机制 + 跨域 NCR↔工单关联 + 运行时工作流裁决 + finding 衔接 + §8 自检齐全）
- [ ] P2-RC-041 注记更新或升级登记 + 若归 MR1 已记录

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_028f1d030ffepK5g9SePNJJNQS，fresh session，未起草本计划）— 全 10 checklist 项 PASS（A 格式完整 / B Deps 满足[A4.2 展开器 completed + P2-RC-041 arm-index:209 successor watch-only 注册 + A1.9/A1.31 done] / C 规则14 合并成立[A1.31 §7:254 逐字「与 A1.9 SP-2 同一存疑点」+「A4.2 运行时探针（与 A1.9 SP-2 合并）」+ roadmap A4.2.5 行标注合并 + 同根因同控制点] / D 单一结果表面 / E baseline 零信任核验全 VERIFIED[enforceGate:52 BLOCKED→throw 致工单保持 IN_PROCESS + originalWorkOrderId 跨 module-manufacturing 0 命中 + ErpQaInspectionFailInspectionProcessor 存在 + NcrLifecycleService.autoCreateNcrFromInspection:49 + rework/ReworkOrder 跨 qa-service 0 命中] / F 反松弛 / G item typing / H Skill / I 保护区域[只读，originalWorkOrderId 修复归 MR1 ORM ask-first + 独立 plan-audit，非 ORM Processor/Facade 逻辑预授权分类正确] / J 无矛盾）。零 Blocker。Non-blocking 已记录：①Phase 1 item 5 config 复核略 tangential 但正确 feed 裁决分支③故 justified。共识达成，转 active。

## Closure Gates

> 本计划为**只读返工工作流评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = REJECTED 门控 census + 可达动作 + 可追溯性机制 + 跨域关联 + 运行时工作流裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.2.5 验证报告 REJECTED 门控 + 可达动作 + 可追溯性机制 + 跨域关联 + 运行时工作流裁决齐全 + finding/注记更新
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.9/A1.31 §7 SP-2 + §5 P2-RC-041 successor 裁决一致
- [ ] 已运行验证：可达动作 + 可追溯性 census + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### originalWorkOrderId 自动关联 / 自动建返工工单（P2-RC-041 successor 修复）

- Classification: `out-of-scope improvement`（本验证是工作流现状评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是现状评估，结果表面 = 验证报告 + finding/注记登记。修复归 MR1（R1.0→RC-R1.n），修复实现 originalWorkOrderId 自动关联属 **ORM 结构变更[ErpMfgWorkOrder 加 originalWorkOrderId 关联字段]须 ask-first + 独立 plan-audit §5**；自动建返工工单的 quality→mfg 跨域触发属 Facade + Processor 代码逻辑预授权可自动执行（非 ORM 部分）。本验证闭环不阻塞于修复落地（finding 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（MR1 R1.0 展开器读取本报告 finding → RC-R1.n 修复，按报告裁决方向：①维持 watch-only→successor 仍登记；②升 P1→须实现 originalWorkOrderId + 自动建返工工单，ORM 部分 ask-first）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填写>

Follow-up:

- MR1 修复 originalWorkOrderId 自动关联 / 自动建返工工单（若登记 finding）
