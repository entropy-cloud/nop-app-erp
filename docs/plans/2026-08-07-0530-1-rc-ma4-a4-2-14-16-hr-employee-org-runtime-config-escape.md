# 2026-08-07-0530-1 rc-ma4-a4-2-14-16-hr-employee-org-runtime-config-escape HR 员工/组织域运行时配置覆盖与未到岗逃生路径验证

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.14 / A4.2.15 / A4.2.16
> Related: `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md`（A1.12 MA1 报告 §7 存疑点 3/4/5）、`docs/plans/2026-08-06-2247-2-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`（A4.2.12 done，同 A1.12 §7 范式）、`docs/plans/2026-08-06-2247-3-rc-ma4-a4-2-13-hr-contract-expiry-multi-tier-alert.md`（A4.2.13 done，同范式）
> Audit: required

## Current Baseline

A1.12（HR-F1 员工与组织）MA1 报告 §7 列出 5 个静态存疑点，其中 §7-1（cron 调度接线）= A4.2.12 done、§7-2（30/60/90 多档预警）= A4.2.13 done。本计划覆盖剩余 §7-3/§7-4/§7-5 三个存疑点，均依赖 config 默认值覆盖行为或逃生路径可达性的运行时确认。

- **A4.2.14（§7-3 UC-HR-12 评估聚合权重运行时配置覆盖）**：`ErpHrConfigs.assessmentSelfWeight/assessmentManagerWeight/assessmentPeerWeight/assessmentSubordinateWeight()` 默认 15%/50%/25%/10%（与 L1 `use-cases.md` 一致）+ `AppConfig.var` 可覆盖。A1.12 §5 静态已确认 config 驱动非硬编码、UC-HR-12 裁决 = 接受。待运行时确认：全 20 生产 `application.yaml` 是否有非默认权重 override（若有，权重运行时是否与 L1 一致需复核；若全默认，则 config-gate = 部署启用决策非契约缺失，对齐 A4.1.4/A4.2.12 范式）。
- **A4.2.15（§7-4 UC-HR-08 handleContract 三态运行时行为）**：`ErpHrEmployeeBizModel.resolveHandleContract` 三态 AUTO/YES/NO，AUTO 模式依赖 `ErpHrConfigs.transferAutoHandleContract()` 默认 true（调动时自动终止旧合同+建新合同）。A1.12 §5 UC-HR-08 裁决 = 接受（㉚三态 + config-gated）。待运行时确认：全 20 生产 `application.yaml` 是否覆盖 `transfer-auto-handle-contract` 为 false（若覆盖，调动不自动处理合同需手工；若全默认 true，主路径满足）。
- **A4.2.16（§7-5 UC-HR-05 未到岗回退运行时处理）**：P2-RC-010（候选人接受 Offer 后未到岗状态回退异常路径未实现，A1.12 §6 新登记）。A1.12 §5 UC-HR-05 裁决 = P2（⑱边界场景弱）。待运行时探查：`hire`→HIRED 终态后无 `rollbackHire` mutation，HR 是否经 `close`（行政关闭，无状态守卫 P2-MA2-048）+ `useLogicalDelete` + 重新申请新 `ErpHrRecruitment` 作为逃生路径可达且可操作；该逃生路径是否满足"候选人接受 offer 但未到岗"的运营场景。

剩余差距：上述三项均为只读运行时确认（config 部署普查 + 逃生路径可达性探查），无生产代码变更。结论模式 = 维持现有裁决（接受/P2）或登记 watch-only residual（对齐 A4.1.4/A4.2.12 config-gate 范式）。

## Goals

- 对 A4.2.14/A4.2.15/A4.2.16 三项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：维持现有结论（接受/P2）+ 登记 watch-only residual（若有），或升级 finding（若运行时证据与静态判定矛盾）。
- 完成后回写 roadmap A4.2.14/A4.2.15/A4.2.16 `todo → done`，并按裁决更新 arm-index（若有新 watch-only finding）。

## Non-Goals

- 不实现 `rollbackHire` mutation 或修改任何 config 默认值（P2-RC-010 的修复义务归 MR1 R1.0 展开器，本计划仅运行时确认）。
- 不重审 A1.12 §7-1/§7-2（A4.2.12/A4.2.13 已 done）。
- 不复跑 MA2 状态机审计或 A4.4 代码质量审计（去重协议，见 roadmap §MA1 去重表）。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落，§9 冻结条款）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md` §5/§6/§7 + `docs/design/human-resource/`（recruitment.md / competency-management.md / state-machine.md）+ `docs/architecture/job-scheduling.md §3.15`
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`（运行时行为多维度确认）。本计划为只读审计，无代码变更，不触发 nop-backend-dev/nop-frontend-dev/nop-testing skill。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为静态 + 部署普查审计（grep config override / census application.yaml / 代码可达性分析），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.14/A4.2.15/A4.2.16）

Status: completed
Targets: `docs/audits/2026-08-07-0530-rc-ma4-a4-2-14-16-hr-employee-org-runtime-config-escape.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done（展开器已完成，实体行已追加）✓；A1.12 done（§7 存疑点清单存在）✓

- [x] **A4.2.14 评估聚合权重 config 部署普查**：grep 全 20 生产 `application.yaml` 是否含 `assessment-self-weight|assessment-manager-weight|assessment-peer-weight|assessment-subordinate-weight` override；确认 `ErpHrConfigs` 四个权重 getter 的 `AppConfig.var` 默认值与 L1 一致（15/50/25/10）。全默认 → config-gate = 部署启用决策，维持 UC-HR-12 接受（对齐 A4.1.4 范式）；有 override → 复核权重运行时值与 L1 契约一致性。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.15 handleContract 三态 config 部署普查**：grep 全 20 生产 `application.yaml` 是否含 `transfer-auto-handle-contract` override（非 true 值）；确认 `ErpHrConfigs.transferAutoHandleContract()` 默认 true + `resolveHandleContract` AUTO 分支可达。全默认 true → 主路径满足，维持 UC-HR-08 接受；有 false override → 确认 YES/NO 手工路径可达且文档引导齐全。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.16 未到岗回退逃生路径运行时可达性探查**：确认 `ErpHrRecruitmentBizModel.hire`→HIRED 终态后无 `rollbackHire` mutation（grep `rollback|undoHire|revertHire` 零业务命中）；确认 `close`（`:130-135` 无状态守卫）+ `useLogicalDelete`（平台标准逻辑删除）+ 重新申请新 `ErpHrRecruitment` 三步逃生路径在运行时可达且可操作（代码可达性 + owner doc `recruitment.md §关键业务规则 #3` REJECTED 候选人可重新申请是否覆盖 HIRED 未到岗场景）；评估逃生路径是否满足"候选人接受 offer 但未到岗"运营场景。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：三项存疑点各出 §裁决（维持 P2-RC-010 watch-only 或登记 config-gate watch-only residual）+ §与既有 finding 衔接（P2-RC-010 / P1-MA2-039 / P2-MA2-048 交叉引用）+ §过程纪律自检（checker 退出码门控——本计划无生产代码变更 actual=baseline；closure-audit 独立性声明）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。完整仓库 build/test 属于 Closure Gates 而非此处。

- [x] 验证报告落盘 `docs/audits/2026-08-07-0530-rc-ma4-a4-2-14-16-hr-employee-org-runtime-config-escape.md`，含三项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [x] 每项裁决明确：维持现有结论（接受/P2）或升级；config-gate watch-only residual（若有）已按 §去重协议裁决是否新建 arm-index 行

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.14/15/16 done）、`docs/audits/arm-index.md`（若有新 watch-only）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：对每项按 §7 规则 grep arm-index 同域同控制点后裁决"复用 or 新增"。config-gate watch-only 若与 A4.1.4/P1-MA2-086 同范式（部署启用决策非契约缺失）则不新建行，仅在报告记录；P2-RC-010 运行时现状确认注记追加（不撤销 watch-only）。
- [x] `Add` roadmap A4.2.14/A4.2.15/A4.2.16 `todo → done`；`docs/logs/2026/08-07.md` 追加完成条目（裁决摘要 + 报告路径）。

Exit Criteria:

- [x] roadmap 三项状态已更新为 done 且与报告裁决一致
- [x] arm-index 无未经比对直接新建的 finding（§7 规则合规）

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_02843be18ffeoe7vWWlB9P1jkD) because Phase 2 item typing was `Proof` while its two items are a `Decision` (arm-index 衔接裁决) and an `Add` (roadmap/log sync) — violates rule 7 (80% phase-level typing threshold not met) and deviates from sibling pattern.
- Independent draft review iteration 2: accept (ses_0284019f0ffeqkZVKBVAjctoK) after Phase 2 `Item Types` corrected to `Decision | Add` with per-item type prefixes; baseline honesty, Deps satisfaction, anti-slack, Q4 compliance, and Closure Gates all confirmed clean. Consensus reached → flipped to active.

## Closure Gates

> 本计划为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更），无新增代码空洞风险。完整仓库验证门控：本审计无代码变更，`mvn` build/test 无回归风险（actual=baseline），但仍须在 closure 时确认 checker 未触发 actual > baseline。无代码变更计划可酌情简化 build 门控，但须在 closure 记录"无代码变更，checker actual=baseline"证据。

- [x] 范围内行为完成（三项存疑点均有 file:line 运行时证据 + 明确裁决）
- [x] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [x] 已运行验证（checker actual=baseline 确认；无代码变更故无 build/test 回归风险）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### rollbackHire mutation 实现（A4.2.16 触及的 P2-RC-010 修复义务）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认逃生路径可达性；P2-RC-010 的修复（实现 rollbackHire 或 owner doc 标注）归 MR1 R1.0 展开器，本审计裁决维持 P2 watch-only 不撤销
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接）

## Closure

Status Note: 计划已完成。Phase 1（运行时证据采集 + 验证报告撰写）与 Phase 2（finding 衔接 + roadmap/log/arm-index 同步）全 `[x]` + Status `completed`。三项存疑点（A4.2.14/A4.2.15/A4.2.16）经全 20 生产 application.yaml config override 普查 + config getter 默认值 census + 逃生路径代码可达性分析，均维持 A1.12 §5 既有裁决（UC-HR-12 接受 / UC-HR-08 接受 / UC-HR-05 P2-RC-010 watch-only 不撤销），0 新 finding / 不触发 MR0 / 不归 MR1。本计划为只读审计，零生产代码/ORM/api.xml/view.xml/config 默认值/真相源变更；git status 仅 .md 文件；checker actual == baseline（R1d/R2a/R2b/R2c/R3/R8 0 漂移，由零代码变更结构性保证）。A1.12 §7 族五项存疑点（§7-1 A4.2.12 + §7-2 A4.2.13 + §7-3 A4.2.14 + §7-4 A4.2.15 + §7-5 A4.2.16）全数收口。

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent（独立结束审计子代理，新会话 ses_028376912ffe9Sp3D1GL2zVyyN，未执行本计划，未重用执行者上下文）
- Methodology: `docs/audits/requirement-compliance-methodology.md` §2/§7/§8/§9 + `docs/skills/multi-dimensional-audit-prompt.md`（7+1 维度）
- Independent re-verification performed (live repo, not trusting plan checkboxes):
  - `grep -rn "assessment-self-weight|assessment-manager-weight|assessment-peer-weight|assessment-subordinate-weight" --include="application*.yaml"` → exit 1, **zero hits** ✅
  - `grep -rn "transfer-auto-handle-contract" --include="application*.yaml"` → exit 1, **zero hits** ✅
  - `grep -rni "rollbackHire|undoHire|revertHire|cancelHire|revokeHire" --include="*.java" module-hr` → exit 1, **zero business hits** ✅
  - `ErpHrConfigs.java:33-39` defaults 0.15/0.50/0.25/0.10 + `:43` DEFAULT_TRANSFER_AUTO_HANDLE_CONTRACT=true + `:129-163` getters ✅
  - `ErpHrRecruitmentBizModel.java:130-135` close no status guard ✅
  - `app-erp-hr.orm.xml:793` erp_hr_recruitment `useLogicalDelete="true" deleteFlagProp="delVersion"` ✅
  - `git status --short` → only `.md` files modified/added（零 .java/.xml/.yaml 生产代码）✅ read-only confirmed
  - roadmap `:167-169` A4.2.14/15/16 `done ✅` with audit evidence ✅
  - arm-index `:149` P2-RC-010 行追加「A4.2.16 运行时现状确认」注记（非新建 finding 行，§7 合规）✅
  - `docs/logs/2026/08-07.md:1-19` 完成条目已落盘 ✅
- Dimensions ruling: 7+1 维度全部 passes（需求正确性 / owner-doc 对齐 / 架构边界 / 验证充分性 / 回归风险 / 路由技能 / 待办漂移 + view.xml gen-control N/A）。反窄化自检：覆盖 7 维度 + 1 项目特定维度，每维度给出裁决。
- Closure Gates 核验：8 项全 ✅（范围内行为完成 / 相关文档对齐 / 已运行验证 checker actual=baseline / 无范围内项目降级[rollbackHire 修复义务归 MR1 R1.0 展开器 plan Deferred But Adjudicated 正确分类非降级] / 独立草案审查已记录[iter 1 needs-revision → iter 2 accept 两独立会话] / 文本一致性已验证 / 结束审计由独立子代理执行[本审计满足] / 结束证据存在于文件中[本段]）
- Residual risks (non-blocking): (1) "20 生产 application.yaml" 计数未独立复核（operative 证据 = 全 application*.yaml grep 零命中）；(2) owner-doc 锚点未逐行复核（§4 owner doc 为设计参考，L1 真相源锚点承载裁决）；(3) P2-RC-010 watch-only 维持，修复义务归 MR1 R1.0 展开器（plan Deferred But Adjudicated successor required = yes）
- Verdict: **passes closure audit**（无需 revision）
