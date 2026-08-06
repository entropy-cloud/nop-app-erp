# 2026-08-06-1044-1 rc-ma4-a4-1-10-auto-recon-config-gated-disabled-coverage AR/AP 自动核销 config-gated 禁用路径覆盖缺口评估

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.10（MA4 运行时行为验证 — A1.3 §7-3：`TestErpFinAutoReconciliation#testConfigGatedDisabled:120` 禁用路径覆盖缺口，@NopTestConfig 类级配置无法按方法覆盖）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.10；存疑点来源 `docs/audits/2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` §7 存疑点 3
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1715-1-rc-ma1-a1-3-finance-f3-ar-ap-reconciliation.md`（A1.3 done，§3 测试证据 + §7 存疑点 3）、`docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（MA2 A2.5c 自动核销引擎既有行为证据输入）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.10 验证报告（落盘 `docs/audits/2026-08-06-1044-rc-ma4-a4-1-10-auto-recon-config-gated-disabled-coverage.md`）+ 必要时 arm-index finding/successor 登记。**不改代码/ORM/api.xml/真相源**（只读评估：grep 测试断言 + 读 config-gated 守卫 + 复用 MA2/A1.3 + 覆盖缺口评级）。范式对齐 A4.1.9（已 done 的测试断言强度评估同型工作项）。

- **存疑点原文**（A1.3 报告 §7 存疑点 3，`2026-08-02-1715-...-a1-3-arap.md` §7）：「`TestErpFinAutoReconciliation.testConfigGatedDisabled:120` 禁用路径覆盖缺口」——javadoc 自述因类级 `@NopTestConfig` 无法测试 auto-reconcile 禁用路径。属已知覆盖缺口（in-code 声明），非合规缺陷。交 MA4 A4.1 按需评估。

- **关联既有结论**：
  - A1.3 §3 测试证据汇总：AR/AP 自动核销引擎测试覆盖评级，`TestErpFinAutoReconciliation` 6 @Test 覆盖 FIFO/BY_AMOUNT/BY_RATIO 三策略 + 幂等 + 超额拒绝 + 未匹配项报告。
  - A1.3 §5 命题族（UC-FIN-08 + 坏账 W1-W4）= **全接受**。本存疑点不推翻接受结论，只评估 config-gated 禁用路径覆盖缺口。
  - MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`：自动核销引擎 + config-gated 行为已证实（9 控制点含 config-gated 门禁）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md` UC-FIN-08（收款核销发票）。自动核销为 finance 域 period-end 正式核销的批量入口（`ar-ap-reconciliation.md §核销流程`），config-gated 守卫是运行时门禁（非 L1 验收标准显式要求"禁用路径须被测试覆盖"）。L1/L2 聚焦核销语义（核销明细 / 状态派生 / 余额恒等式），config-gated 禁用路径是运营开关，非核销语义验收标准。

- **实现现状（L3，实测锚点，本计划起草时核实）**：
  - config-gated 守卫：`ErpFinReconciliationRunAutoReconciliationProcessor#runAutoReconciliation:36-38` → `if (!isAutoReconcileEnabled()) throw new NopException(ErpFinErrors.ERR_AUTO_RECON_DISABLED):38-40`。
  - config 读取点：`isAutoReconcileEnabled():66-68` → `AppConfig.var(ErpFinConstants.CONFIG_AUTO_RECONCILE, Boolean.FALSE)`（默认 **FALSE = 禁用**）。config key = `"erp-fin.auto-reconcile"`（`ErpFinConstants.CONFIG_AUTO_RECONCILE:24`）。
  - 测试断言点：`TestErpFinAutoReconciliation#testConfigGatedDisabled:119-131`——**方法名误导**：实际测试 **enabled 路径**（`assertFalse(result.getReconciliationIds().isEmpty(), "config-gated=true 时应正常执行"):130`），**非** disabled 路径。javadoc:121-123 自述「config-gated false 抛错的覆盖留同会话单独的 disabled 配置（见下）」——但全类无此 disabled 配置测试方法。
  - 类级配置约束：`@NopTestConfig(..., testConfigFile = "classpath:auto-recon-test.yaml"):33-36`——`auto-recon-test.yaml` 设 `erp-fin.auto-reconcile=true`，类级配置无法按方法覆盖，故 disabled 路径（config=false → 抛 `ERR_AUTO_RECON_DISABLED`）在全类零覆盖。

- **既有证据（复用输入）**：
  - MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md` 控制点：config-gated 门禁行为已证实（禁用时抛 `ERR_AUTO_RECON_DISABLED`，启用时执行三策略）。本验证复用其「守卫行为正确」结论，**只补「禁用路径测试覆盖缺口」差异**。

- **初步实测（本计划起草时的部分核验，执行时复核）**：
  - grep `TestErpFinAutoReconciliation.java` `ERR_AUTO_RECON_DISABLED|assertThrows|disabled|false`——零命中（无任何测试断言禁用路径抛错）。
  - grep 全仓 `ERR_AUTO_RECON_DISABLED` 测试消费——确认无独立测试类覆盖此守卫。
  - 即本验证最可能结论 = **接受（守卫行为正确，禁用路径覆盖缺口属已知 in-code 声明的测试覆盖缺口，非合规缺陷）**或 **P2（测试覆盖补强 successor，非行为缺陷）**；属**确认性覆盖缺口评估**（守卫行为已由 MA2 证实，本评估只定级覆盖缺口）。

- **剩余差距**：config-gated 禁用路径（config=false → 抛 `ERR_AUTO_RECON_DISABLED`）是否有任何测试覆盖——A1.3 标注为「已知覆盖缺口（in-code 声明）」但未定级（接受 vs P2 测试覆盖补强）。本验证补全该评级。

- **保护区域**：只读评估（grep + 读守卫 + 读测试 + 引用 MA2/A1.3），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——若登记 P2 测试覆盖补强 successor，修复（测试代码补禁用路径断言）经 MR1（纯测试代码修复预授权类目）。

## Goals

- 覆盖缺口评级：对 `TestErpFinAutoReconciliation` 全类 6 @Test 的覆盖范围评级，确认 config-gated 禁用路径（config=false → 抛 `ERR_AUTO_RECON_DISABLED`）覆盖缺口是否削弱「AR/AP 自动核销」需求符合性覆盖。
- config-gated 守卫行为证据：`isAutoReconcileEnabled():66-68` 读 `CONFIG_AUTO_RECONCILE`（默认 FALSE）→ 禁用时 `runAutoReconciliation:38-40` 抛 `ERR_AUTO_RECON_DISABLED`——证实守卫行为正确（MA2 已证实），评估测试是否须镜像覆盖。
- 对齐 UC-FIN-08 + `ar-ap-reconciliation.md §核销流程` 给出结论：①若守卫行为已由 MA2 证实且禁用路径是运营开关非 L1 验收标准（config-gated 覆盖缺口属测试覆盖补强项非合规缺陷）→ 接受（覆盖缺口已知，守卫行为正确）；②若禁用路径覆盖缺失削弱语义覆盖且属可回归保护点 → P2（测试覆盖补强 successor，非行为缺陷）。
- 产出验证报告 + §8 过程纪律自检；finding/successor（若有）按 §7 裁决登记 arm-index。

## Non-Goals

- **不重新核实 UC-FIN-08 核销符合性 / 坏账 W1-W4 命题**（A1.3 §5 已判接受；本验证只评 config-gated 禁用路径覆盖缺口）。
- **不修改测试代码**（只读评估；补禁用路径断言经 MR1）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不实施修复**（修复经 MR1；本验证仅登记 finding/successor）。
- **不展开 A1.3 §7-1/§7-2**（A4.1.8/A4.1.9 范围，已 done）。

## Task Route

- Type: `verification or audit work`（覆盖缺口评估 + 接受/P2 裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议 + MA4↔A5.6 边界）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.10 行）+ `docs/audits/2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` §7 存疑点 3 + §3 测试证据（输入）+ `docs/design/finance/ar-ap-reconciliation.md §核销流程`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。覆盖缺口评估需多维度归类（守卫写入点 / 测试覆盖集 / 覆盖缺口评级 / MA4↔A5.6 边界 / 接受-or-P2 裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep + 读守卫 + 读测试 + 引用 MA2/A1.3）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - config-gated 禁用路径覆盖缺口评级

Status: completed
Targets: `docs/audits/2026-08-06-1044-rc-ma4-a4-1-10-auto-recon-config-gated-disabled-coverage.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.10 行）；A1.3 done（§7 存疑点 3 已落盘 + §3 测试证据 + §5 命题族接受）

- [x] `Proof` config-gated 守卫行为核验：给出 `isAutoReconcileEnabled():66-68` 读 `CONFIG_AUTO_RECONCILE`（默认 FALSE）+ `runAutoReconciliation:38-40` 禁用时抛 `ERR_AUTO_RECON_DISABLED` 证据（file:line）+ MA2 A2.5c 已证实守卫行为的引用。证实守卫行为正确。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §2.1（守卫 `runAutoReconciliation:36-40` + `isAutoReconcileEnabled:66-69` 读默认 FALSE + `ErpFinConstants.CONFIG_AUTO_RECONCILE:24` + `ErpFinErrors.ERR_AUTO_RECON_DISABLED:140` + `IErpFinReconciliationBiz:64` 契约 javadoc）+ 复用 MA2 A2.5c 9 控制点。
- [x] `Proof` 测试覆盖集全集核验：grep `TestErpFinAutoReconciliation.java` 全部 @Test + 覆盖范围分类，标注 `testConfigGatedDisabled:119-131` 实测 enabled 路径（非 disabled）+ 全类零 disabled 路径覆盖。引用 A1.3 §3 已有评级依据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §3.1。实测全类 **7 @Test**（比 A1.3 §3 / plan 起草时记录「6」多 1，计数修正声明已落盘）；`testConfigGatedDisabled:119-131` 方法名误导（实测 enabled 路径，`:130` 断言 enabled 正常执行）；全测试树 grep `auto-reconcile.*false|ERR_AUTO_RECON_DISABLED|isAutoReconcileEnabled` = 零命中，禁用路径零覆盖。
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（禁用路径覆盖缺口是否削弱核销语义覆盖），与 A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角）边界按此执行（方法论 §去重协议 MA4↔A5.6）。本验证不重做 A5.6 E2E 断言强度审计，只评单元测试 config-gated 禁用路径覆盖缺口。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §4.2（MA4 需求契约视角覆盖缺口 vs A5.6 测试质量全量评级边界声明）。
- [x] `Decision` 覆盖缺口裁决（方法论 §2 判据 + 三源对照）：①若守卫行为已由 MA2 证实且 config-gated 是运营开关非 L1 验收标准（覆盖缺口属测试覆盖补强项非合规缺陷）→ 接受（覆盖缺口已知，守卫行为正确，A1.3 §5 命题族接受维持）；②若禁用路径覆盖缺失削弱语义覆盖且属可回归保护点（如未来误删守卫无测试拦截）→ P2（测试覆盖补强 successor，非行为缺陷）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.3 §5 命题族接受结论分层一致。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §5.1。裁决 = **① 接受**（分支②「削弱语义覆盖」条件不成立——config-gated 是运营 feature gate 非核销语义验收标准，禁用路径无核销语义行为可验证；enabled 路径 7 @Test 语义覆盖完整）+ §5.2 三源对照 + §5.3 与 A1.3 §5.1 UC-FIN-08 接受分层一致 + §5.4 与 A4.1.8(P2)/A4.1.9(接受) 同族对照。

Exit Criteria:

- [x] config-gated 守卫行为 + 测试覆盖集清单落盘（全集，无遗漏），每条有证据（file:line）
- [x] 覆盖缺口裁决有明确结论（接受 / P2 测试覆盖补强 successor），与 A1.3 §5 命题族接受结论分层一致

### Phase 2 - finding/successor 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-06-1044-rc-ma4-a4-1-10-auto-recon-config-gated-disabled-coverage.md`（定稿）；`docs/audits/arm-index.md`（若新 finding/successor）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 覆盖缺口评级 + 裁决完成

- [x] `Add` 若定 P2 → 按 §7 grep arm-index finance auto-recon/config-gated/测试覆盖缺口同域同控制点裁决「复用 or 新建」`P*-RC-xxx` 或 successor 行，写入 arm-index MA4 分区；双向可追溯注记（finding/successor → MR1）。若接受 → 在报告登记「无新 finding，归 A1.3 §5 命题族接受 + §7 存疑点 3 闭合」。禁止未经比对新建。
      - Skill: none
      - 落盘：报告 §6。裁决 = 接受 → 报告 §6.2 登记「无新 finding」+ §6.3「A1.3 §7 存疑点 3 经本评估正向消解为接受，闭合」+ §6.1 grep 比对（P2-MA2-039 不同控制点 / P2-RC-082 不同控制点不同裁决性质 / A1.3 §5.1 UC-FIN-08 接受复用）。arm-index 无写入（接受维持，无 successor）。
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.3 §5 命题族接受 / MA2 A2.5c 自动核销守卫 / A5.6 E2E 断言强度边界 的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none
      - 落盘：报告 §8。checker 实测 actual == baseline（R1d=14 / R2a=34 / R2b=229 / R2c=1382 / R2d=34 全等；R3-R12 截断为既有工具行为）；零生产代码变更注明无回归风险；closure-audit 独立性声明 + arm-index 交叉去重声明齐全。

Exit Criteria:

- [x] 验证报告定稿（守卫 + 覆盖集 + 裁决 + finding/successor 衔接 + §8 自检齐全）
- [x] 新 finding/successor（若有）已写入 arm-index MA4 分区并有 grep 依据（本验证若维持接受则无写入，本条 N/A 满足）

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (2026-08-06 draft review) — 格式合规（必需章节齐全、Item Types 合法、Skill 逐项记录）、退出标准可测、范围单一（只读覆盖缺口评估单一结果表面）、结束证据门控完备（无代码变更故按指南删除 typecheck/build/test 门控并说明）。全部 file:line 锚点已对实时仓库核验（processor guard :36-39 / isAutoReconcileEnabled :66-67 / CONFIG_AUTO_RECONCILE ErpFinConstants:24 / ERR_AUTO_RECON_DISABLED ErpFinErrors:140 / TestErpFinAutoReconciliation.java 存在 / roadmap A4.1.10 行 todo / 三份关联审计+方法论+技能文件均存在）。无 Blocker/Major；Minor：技能 multi-dimensional-audit-prompt.md 属 AGENTS.md 标注「复制后须定制」的默认模板，执行时若复用须按方法论裁剪。

## Closure Gates

> 本计划为**只读覆盖缺口评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 守卫行为 + 覆盖集完整性 + 裁决 + finding/successor 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.10 验证报告守卫 + 覆盖集 + 裁决齐全 + finding/successor（若有）登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议（MA4↔A5.6 边界）一致；与 A1.3 §7-3 + §3 测试证据 + §5 命题族接受一致
- [x] 已运行验证：守卫 + 覆盖集完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### config-gated 禁用路径测试覆盖补强（若定 P2 测试覆盖补强）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是覆盖缺口评估，结果表面 = 验证报告 + 裁决 + finding/successor 登记。UC-FIN-08 核销符合性已接受（A1.3 §5）；config-gated 禁用路径覆盖缺口属测试覆盖补强项（非行为缺陷，守卫行为已由 MA2 证实正确）。补禁用路径断言经 MR1（R1.0→RC-R1.n，纯测试代码修复预授权类目）。若裁决为接受（覆盖缺口已知，守卫行为正确）则无 successor。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 finding/successor 交叉引用展开；若维持接受则无 successor）

## Closure

Status Note: 完成。A4.1.10 验证报告（`docs/audits/2026-08-06-1044-rc-ma4-a4-1-10-auto-recon-config-gated-disabled-coverage.md`）落盘——config-gated 守卫行为核验（§2.1 file:line 主证据）+ 测试覆盖集全集核验（§3，实测 7 @Test，`testConfigGatedDisabled:119-131` 方法名误导实测 enabled 路径，全测试树零 disabled 路径覆盖）+ MA4↔A5.6 边界声明（§4.2）+ 覆盖缺口裁决 = **接受**（§5.1，config-gated 是运营 feature gate 非 L1 验收标准，分支②「削弱语义覆盖」条件不成立）+ arm-index 衔接（§6，零新 finding，A1.3 §7 存疑点 3 正向消解为接受、闭合）+ §8 过程纪律自检（checker actual == baseline，零生产代码变更无回归风险）。roadmap A4.1.10 行已 todo→done ✅。只读评估，无代码/ORM/api.xml/真相源变更。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，task `ses_02aa051f0ffetEwqbUUvPADHYc`，general 类型，不重用执行者上下文）
- Verdict: **passes closure audit**（全 12 项检查清单 PASS：A1-A5 证据 file:line 实测核验 / B6 ACCEPT 裁决可辩护性确认 / C7-C11 计划完整性+roadmap+只读约束 / D12 文本一致性）
- Evidence: 独立子代理实测核验——守卫 `ErpFinReconciliationRunAutoReconciliationProcessor:38-40` + `isAutoReconcileEnabled:66-69` + `ErpFinConstants:24` + `ErpFinErrors:140`；`@Test` 计数 = 7（修正 plan 的 6）；`testConfigGatedDisabled:130` 断言 enabled 路径；grep 全测试树 disabled 路径覆盖 = 零命中；裁决可辩护性确认（config-gated 运营开关非 L1 验收标准，分支②「削弱语义覆盖」不成立，与 A4.1.9 同型、区别于 A4.1.8 语义边界）；`git status` 仅 docs 变更（无生产代码/ORM/api.xml）。1 项 Minor（MA2 config-gated 复用措辞偏松）已由执行者修正为 §2.1 主证据为准的精确归因。

Follow-up:

- 无阻塞跟进。若未来要求显式 config-gated 禁用路径测试覆盖（作为测试质量增强，非合规要求），属 A5.6 测试质量维度，经纯测试代码 MR1 预授权类目。
