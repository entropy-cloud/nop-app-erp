# 2026-07-31-1705-1-r4-1-cross-dimension-adjudication MR4 跨维度发现裁决

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MR4 工作项 R4.1（todo，依赖 MR1+MR2+MR3 done 全部满足）
> Related: `docs/audits/arm-index.md` §跨维度发现（待 MR4 裁决）；R4.1 是 MV 里程碑（plan `-1705-2`/`-1705-3`）的前置门控
> Audit: required

## Current Baseline

- **roadmap 状态**：MR1（R1.0–R1.29）+ MR2（R2.0–R2.15）+ MR3（R3.0–R3.7）+ MR5（R5.1–R5.8）全部 done。MR4 R4.1 是文档顺序中下一个 `todo` 工作项，其依赖 `MR1+MR2+MR3 done` 全部满足。
- **arm-index §跨维度发现（待 MR4 裁决）** 当前登记 3 条跨维度发现，且**均已预先标注初步处置**，但未给出 R4.1 最终裁决（裁决状态列仍为「待 R4.1 裁决 / MR3 协同」等中间态）：
  1. `P1-MA3-046` ↔ `P1-MA2-093/094` ↔ `P1-MA6-001`/`P1-MA6-002` — MA2 多公司隔离 + MA3 API 契约权限 + MA6 权限注解/数据权限 四维度交叉。处置列：`MR2/MR1/MR3 协同（待 R4.1 裁决）`。
  2. `P1-MA6-002` ↔ `P1-MA2-093` — MA2 多公司隔离 + MA6 数据权限。处置列：`MR3 协同（与 P1-MA2-093 一并裁决）`，并已注明「互补不重复」。
  3. `P1-MA3-048` ↔ `P1-MA2-054` — MA2 业务正确性 + MA3 API 契约 子例关系。处置列：`closed (MR5 R5.8)`。
- **各发现对应的 MR 修复落地状态**（实时仓库 + roadmap 实测）：
  - 发现 1 涉及的修复项：R2.7（API 契约权限/命名/影子契约，done）+ R1.29（orgId 隔离，done）+ R3.3（SoD approver≠creator，done）+ R3.4（角色侧行级过滤 data-auth.xml，done）。
  - 发现 2 涉及的修复项：R1.29（orgId 维度，done）+ R3.4（createdById/assigneeId/deptId 维度，done）。
  - 发现 3：MR5 R5.8 已闭合（purchase WithdrawApproval/Reject per-mutation Processor 接线 + BizModel repoint）。
- **剩余差距**：裁决状态列仍是中间态；roadmap R4.1 仍为 `todo`；MV 里程碑被 R4.1 阻塞无法启动。本计划无代码/ORM/契约变更，纯裁决记录 + 索引/路线图 bookkeeping。

## Goals

- 对 arm-index §跨维度发现 的 3 条发现逐一给出 R4.1 最终裁决（确认无冲突 / 已协同闭合 / 已 closed）。
- 验证「无未解决的跨维度冲突」：MR1–MR3 修复方案之间不存在矛盾方向，重复发现已去重归属。
- 将 arm-index §跨维度发现 裁决状态列从中间态更新为 `adjudicated (R4.1)`，并回填每条发现对应的已落地修复工作项编号。
- 将 roadmap R4.1 标记 done，解除 MV 里程碑阻塞。

## Non-Goals

- 不重新审计任何维度（MA1–MA7 已全部 done，本计划只裁决跨维度交叉，不产出新 finding）。
- 不修复任何 P1/P2 finding（所有跨维度涉及的 P1 均已在 MR1/MR2/MR3/MR5 闭合；本计划是裁决门控，非修复计划）。
- 不启动 MV 里程碑（V.1–V.5）的任何验证工作——那是 plan `-1705-2`/`-1705-3` 的范围。
- 不处理 P2 watch-only 项（如 P2-MA5-005/006/007、P2-MA7-005）——这些是 MV/G 范围或已登记 successor。

## Task Route

- Type: `verification or audit work`（跨维度裁决，纯文档/索引 bookkeeping，零代码变更）
- Owner Docs: `docs/audits/arm-index.md` §跨维度发现；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §跨维度
- Skill Selection Basis: roadmap R4.1 显式指派 `docs/skills/multi-dimensional-audit-prompt.md`（跨维度冲突识别方法）。本计划裁决对象是已登记的跨维度发现，技能用于结构化核对「协同 vs 冲突 vs 重复」三态分类，不新增审计维度。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 纯只读核对 + markdown 索引/路线图更新，无构建/测试/部署依赖。

## Execution Plan

### Phase 1 - 跨维度发现逐条裁决（只读核对 + 裁决记录）

Status: completed
Targets: `docs/audits/arm-index.md` §跨维度发现
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Proof`
- Prereqs: MR1 + MR2 + MR3 + MR5 done（均已满足）

- [x] **发现 1（四维度交叉）裁决**：核对 R2.7（action-level 权限注解 + action-auth + data-auth enforcement）+ R1.29（orgId 数据级行过滤）+ R3.3（SoD approver≠creator 工作流级）+ R3.4（角色侧行级过滤）均已 done 且修复层级互补不重叠（action-level vs data-row-orgId vs workflow-level vs data-row-role 四个不同控制层）。裁决=**无冲突，四者协同闭合**。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **发现 2（双维度互补）裁决**：核对 P1-MA6-002（createdById/assigneeId/deptId 维度，R3.4）与 P1-MA2-093（orgId 多公司维度，R1.29）声明来源不同（roles-and-permissions.md §数据权限 vs multi-company.md）、过滤列不同。裁决=**互补不重复，各自独立闭合**。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **发现 3（子例关系）裁决**：核对 P1-MA3-048（全域孤儿 Processor 汇总）与 P1-MA2-054（purchase 子例）经 MR5 R5.8 已 closed。裁决=**已 closed，无需重复处置**。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **冲突方向扫描**：grep MR1–MR3 修复中是否存在对同一控制点的矛盾修改（如一个修复收紧、另一个修复放宽同一守卫）。若发现矛盾方向，登记为 successor 并升级为 P1；若无，记录「零矛盾方向」。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **Proof**: 裁决证据=arm-index 中每条发现的「已落地修复工作项编号」回填 + roadmap 中对应工作项 Status=done 的实时核对。无需运行测试（零代码变更）。

Exit Criteria:

> 纯裁决记录计划。本阶段交付的可观察结果=裁决落盘于 arm-index；不触发构建/测试。

- [x] arm-index §跨维度发现：发现 1-2 裁决状态列从中间态更新为 `adjudicated (R4.1)`；发现 3 确认 `closed (MR5 R5.8)` 终态并追加 R4.1 adjudication 注记（不回退其已闭合状态）；每条回填对应已落地 MR 工作项编号
- [x] 「零矛盾方向 / 零未解决冲突」结论以 Decision 形式记录于本计划或 arm-index

### Phase 2 - roadmap bookkeeping + 索引一致性

Status: completed
Targets: `docs/backlog/audit-remediation-roadmap.md`（R4.1 行）；`docs/audits/arm-index.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 裁决完成

- [x] 将 roadmap R4.1 行 Status `todo`→`done`，并在 Work Item Details §MR4-MG 补充 R4.1 裁决结论（「无跨维度冲突，3 条发现均已协同/独立闭合」）
- [x] 在 arm-index §跨维度发现 段首更新说明：MR4 裁决已完成，本段从「待 MR4 裁决」降级为「MR4 已裁决（裁决记录）」
- [x] arm-index 与 roadmap 双向一致性复核：R4.1=done 在两处状态一致

Exit Criteria:

> 本阶段交付 roadmap/arm-index 双向一致的 bookkeeping。

- [x] roadmap R4.1 = done 且 arm-index §跨维度发现 标注 MR4 已裁决，两处文本一致

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_0489178e3ffeUb9ym07Jy2ShTu) — 独立子代理 fresh session 实时仓库复核：3 条跨维度发现的 ID/处置值/MR 工作项映射全部准确；R2.7/R1.29/R3.3/R3.4/R5.8 均 done 实测确认；R4.1 依赖满足；范围无泄漏（Non-Goals 排除 re-audit/P1-fix/MV/P2）；零 anti-slack 违规；零代码计划 Closure Gates 正确省略构建门控。1 项 non-blocking 观察已采纳修订（发现 3 已 `closed` 终态，Phase 1 exit criteria 措辞由「全部从中间态」改为区分发现 1-2 中间态→adjudicated + 发现 3 确认 closed 追加注记）。 consensus 达成，draft→active。

## Closure Gates

> 纯文档/索引计划（零代码/ORM/契约/view 变更）。删除验证命令门控——无可运行的 typecheck/build/test 适用于本计划。仅保留行为/一致性/审计门控。

- [x] 范围内行为完成（3 条发现裁决 + roadmap R4.1 done + arm-index 标注）
- [x] 相关文档对齐（arm-index §跨维度发现 + roadmap §MR4 一致）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P2 watch-only 跨维度观察项（非本计划范围）

- Classification: `watch-only residual`
- Why Not Blocking Closure: P2-MA5-005/006/007（测试隔离/E2E 计数/master-data.write.amis）+ P2-MA7-005（过账红冲有界 N+1 批量加载化）均为 watch-only，归 MV/G 里程碑或已登记 successor，与 R4.1 跨维度裁决（P1 层）不同层级。
- Successor Required: `no`（各自已被其归属里程碑/successor 覆盖）

## Closure

Status Note: R4.1 跨维度裁决完成（2026-07-31）。裁决结论=零跨维度冲突，3 条发现均已协同/独立闭合（发现 1 四维度交叉协同闭合 / 发现 2 双维度互补不重复 / 发现 3 维持 MR5 R5.8 closed）。冲突方向扫描=零矛盾方向（MR1–MR3 修复均单向收紧）。roadmap R4.1=done、arm-index §跨维度发现=MR4 已裁决，双向一致。MV 里程碑阻塞解除。零代码计划，无构建/测试门控。

Closure Audit Evidence:

- Auditor / Agent: independent closure-audit subagent, fresh session cold-context (task ses_04889f5fdffeV4dgFNdxWBZK01)
- Evidence: Verdict=PASS。独立复核 7 项：(1) 两 Phase `Status: completed` + 全部 `[x]` 零遗留 `[ ]`（仅 Closure Gates 预期未勾）；(2) arm-index §跨维度发现 header=「MR4 已裁决」+ 段首注记 + 发现 1/2 裁决状态=`adjudicated (R4.1)` + 发现 3=`closed (MR5 R5.8)` 含 R4.1 注记未回退 + R4.1 Decision 块「零矛盾方向/零未解决跨维度冲突」；(3) roadmap R4.1 行=done + §MR4-MG 结论行；(4) 依赖前提实测全 done（R1.29/R2.7/R3.3/R3.4/R5.8）；(5) arm-index↔roadmap 双向一致无矛盾；(6) git status 仅 2 .md 文件变更（零 Java/ORM/view/test），真实裁决文本非占位，无范围项降级 deferred；(7) MV 解除阻塞注记两处一致。零 issue。

Follow-up:

- <none in scope>
