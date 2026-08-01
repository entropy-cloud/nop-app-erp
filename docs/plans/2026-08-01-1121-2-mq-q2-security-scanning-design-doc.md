# 2026-08-01-1121-2-mq-q2-security-scanning-design-doc 安全扫描流水线 Phase 1 设计文档

> Plan Status: active
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q2
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q2（line 675, 784-785）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（位 5，独立性强不阻塞其他维度）
> Related: `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md`（Q0 顺序基线，前置 done）；`docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`（Q4，文档结构参照）；MR1.16 / A6.1-A6.4（既有 RBAC 注解审计 + 数据权限运行验证基线，Q2 在其之上补传递依赖 CVE + 静态安全规则）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 1**：产出审查收敛的设计文档 `docs/architecture/quality-engineering/security-scanning.md`，**不改任何代码/ORM/CI**。MQ roadmap（line 843-862）与 Q0 README（line 20-22）明确：Phase 1 设计文档经独立子代理 ≥2 轮审查收敛后，方可编写 Phase 2 实现 plan。

**audit-remediation 主线状态**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。MQ 进行中：Q0/Q1/Q4/Q6 已 done，Q2/Q3/Q5/Q7 待办。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 全绿（1903 测试 0 failures）。

**Q2 现状（NOT FOUND 证据，引用 Q0 README §范围矩阵 §Q2 + roadmap line 784-785 + 实仓复核，核验日期 2026-08-01）**：

- 全仓零安全扫描依赖/CI job：`rg -il "owasp|dependency-check|spotbugs|findsecbugs|sonarqube|snyk" --glob '*.xml' --glob '*.yml' --glob '*.yaml'` 零命中（2026-08-01 复核确认）。
- 现有 CI 工作流（`.github/workflows/`）共 5 个，均无安全扫描 job：`maven.yml`（构建+测试）/ `compliance.yml`（19 规则 compliance checker + i18n checker + fault-injection-coverage）/ `e2e.yml`（Playwright）/ `mutation.yml`（nightly pitest）/ `clock-rollover.yml`（nightly 时钟翻车）。
- **MA6 仅做 RBAC 注解审计 + 数据权限运行验证**（A6.1-A6.4 全 done）：action-level `@BizMutation`/`@BizQuery` 权限注解完整性 + 4 S/A 域 approve SoD（R3.3）+ 角色侧行级过滤 data-auth.xml（R3.4）+ 保护区域过程纪律。**但传递依赖 CVE 与静态安全规则（注入/XSS/反序列化/硬编码密钥）完全无人看**——MA6 是运行时授权维度，Q2 是依赖链 + 静态代码安全维度，二者正交互补。
- **关键风险（roadmap line 784 明示）**：156 模块传递依赖 CVE 查询受 NVD API 限速，per-commit 全量扫描不现实。Phase 1 须裁决 `aggregate` 聚合模式 + 调度策略（nightly vs per-commit）。
- **既有单向收紧门控先例**：compliance checker（`docs/audits/compliance-baseline.md` §BASELINE 机器可读块）+ i18n checker（R3.7 plan `2026-07-31-1439-3`）+ fault-injection-coverage（Q4）+ mutation（Q1 nightly 软门控）。Q2 须对齐此范式：首次扫描必然有发现 → 分类 → 基线落盘 → 单向收紧（actual > baseline => CI red）。

**剩余差距**：无 Q2 设计 owner doc。工具选型（Dependency-Check 为默认）/ 规则集（FindSecBugs）/ CI 调度（aggregate + nightly）/ 基线建立策略（首次发现分类工作流）/ 门控形态均未裁决，须在 Phase 1 文档中独立审查后定夺。

## Goals

- 产出 MQ Q2 的 Phase 1 设计文档 `docs/architecture/quality-engineering/security-scanning.md`，经独立子代理 ≥2 轮审查收敛（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），覆盖 MQ 文档先行工作流要求的 6 节：
  1. **现状评估**（引用实仓证据：零安全扫描 + 5 CI job 清单 + MA6 RBAC 基线边界 + 单向收紧先例）
  2. **目标与非目标**（覆盖传递依赖 CVE + 静态安全规则两条线；不替代运行时 RBAC）
  3. **技术选型**——OWASP Dependency-Check（默认——开源本地可跑）vs Snyk（需联网+商业许可）+ SpotBugs FindSecBugs（静态安全规则）+ CI 调度（aggregate + nightly）的替代评估与裁决理由
  4. **实施步骤**（Dependency-Check 接入 + FindSecBugs 接入 + 首次扫描 + 发现分类工作流 + 基线落盘）
  5. **验收判据**（CVE 扫描 + 静态安全规则扫描均接入 CI + 基线落盘 + 单向收紧门控成立 + 不阻塞 per-commit 构建）
  6. **CI 门控设计**（nightly 全量扫描 + 基线单向收紧 + 发现分类工作流 + 与现有 5 CI job 不冲突）
- 文档须显式声明 CVE 限速下的调度裁决（nightly aggregate vs per-commit），这是 roadmap line 784 明示的关键风险。
- 文档须显式声明基线建立策略（首次扫描发现分类工作流 + 单向收紧，对齐 compliance-baseline 范式）。

## Non-Goals

- **不实现任何代码/ORM/CI 变更**——本计划仅产出设计文档。Phase 2 实现（Dependency-Check + FindSecBugs 接入 CI + 首次扫描 + 基线落盘）是**独立的后续 plan**，须在本设计文档审查收敛后方可起草（MQ 文档先行工作流）。
- 不替代运行时 RBAC（MA6 action-level + data-row-level + SoD 是授权维度；Q2 是依赖链 + 静态代码安全维度，正交）。
- 不修改 `nop-entropy` 源码（安全扫描在依赖树 + 编译产物层面，不动平台生产代码）。
- 不修复首次扫描发现的具体 CVE/规则违规（那是 Phase 2 后的分类修复 workflow）。
- 不重新推导 NOT FOUND 证据（引用 Q0 README，避免双真相源）。
- 不编写 Q3/Q5 设计（同批独立 plan）。

## Task Route

- Type: `app-layer design change`（设计文档编写；纯文档，零代码）
- Owner Docs: `docs/architecture/quality-engineering/README.md`（Q0 顺序基线 + 文档先行工作流引用）；`docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q2 + §横切关注点 §文档先行工作流；`docs/audits/compliance-baseline.md`（单向收紧门控范式 + §BASELINE 机器可读块参照）；`docs/design/roles-and-permissions.md`（MA6 RBAC 基线边界，Q2 与之正交）；`.github/workflows/compliance.yml`（既有 CI job 集成参照）
- Skill Selection Basis: AGENTS.md 强制技能扫描已完成——`nop-backend-dev`/`nop-frontend-dev`/`nop-testing`/`nop-debugging` 均不匹配"编写安全扫描流水线设计文档"。故 `Skill: none`（与 roadmap Q2 行 Skill 列 `none` + Q1/Q4 设计文档计划 Skill 列一致）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 本计划纯文档，不涉及端口/环境变量/CORS/密钥/.env/外部服务。

> 注：Phase 2 实现时，Dependency-Check NVD 数据库需要本地缓存或网络访问（NVD API 限速）。此为 Phase 2 实施关注，本 Phase 1 仅在文档中评估其调度策略。

## Execution Plan

### Phase 1 - 编写 Q2 设计文档草稿

Status: planned
Targets: `docs/architecture/quality-engineering/security-scanning.md`（新建）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Q0 done（已满足）；Q0 README §实施顺序裁决落盘（已满足，Q2 位 5）；MA6 RBAC 基线已落地（已满足，作为 Q2 正交边界参照）

- [ ] Add: 创建 `security-scanning.md`，含 MQ 文档先行工作流要求的 6 节骨架
      - Skill: none
- [ ] Add: §现状评估 —— 引用（非重推导）Q0 README §Q2 + roadmap line 784-785 + 本计划 Current Baseline 实仓复核：零安全扫描（核验命令零命中）、5 CI job 清单、MA6 RBAC 基线边界（正交不重复）、单向收紧先例（compliance/i18n/fault-injection/mutation）。标注可复现核验命令 + 核验日期。
      - Skill: none
- [ ] Decision: §技术选型 —— 评估并裁决安全扫描工具链：
      - **CVE 依赖扫描**：OWASP Dependency-Check（候选首选——开源本地可跑，Maven 插件 + CLI + aggregate 模式）vs Snyk（替代——需联网 + 商业许可）vs GitHub Dependabot（替代——平台内建但规则可控性低）
      - **静态安全规则**：SpotBugs + FindSecBugs（候选首选——开源，Maven 插件，注入/XSS/反序列化/硬编码密钥规则集）vs SonarQube（替代——重，需独立服务）
      - 记录候选 + 考虑的替代 + 残留风险（Dependency-Check NVD 限速 + 误报率 / Snyk 许可与离线 / FindSecBugs 规则集覆盖与误报 / SonarQube 运维成本）
      - Skill: none
- [ ] Decision: §CI 调度裁决 —— 裁决 CVE 限速下的扫描调度策略（roadmap line 784 明示关键风险）：
      - 路径 A：nightly aggregate 全量扫描（Dependency-Check aggregate 模式聚合 156 模块依赖树，一次 NVD 查询）+ per-commit 跳过
      - 路径 B：per-commit 增量扫描（仅 changed module，但 NVD 限速可能阻塞 PR）
      - 路径 C：weekly 全量 + per-commit 仅 FindSecBugs（静态规则不限速）
      - 记录候选 + 考虑的替代 + 残留风险（nightly 延迟发现 / per-commit 限速阻塞 / weekly 粒度）
      - Skill: none
- [ ] Add: §基线建立策略 —— 首次扫描必然有发现 → 分类工作流（Critical/High/Medium/Low + 真实漏洞 vs 误报 vs 已接受风险）→ 基线落盘（对齐 compliance-baseline.md §BASELINE 机器可读块范式）→ 单向收紧（actual > baseline => CI red）。须裁决基线载体（独立文件 vs 复用 compliance-baseline.md 新增 §F 块）。
      - Skill: none
- [ ] Add: §实施步骤 —— Dependency-Check Maven 插件配置（aggregate profile）+ FindSecBugs Maven 插件配置 + 首次扫描执行 + 发现分类 + 基线落盘 + CI job 落地
      - Skill: none
- [ ] Add: §验收判据 —— CVE 扫描 + 静态安全规则扫描均接入 CI + 基线落盘 + 单向收紧门控成立（actual > baseline => CI red）+ 不阻塞 per-commit 构建（nightly/weekly 调度）+ 与现有 5 CI job 不冲突
      - Skill: none
- [ ] Add: §CI 门控设计 —— nightly/weekly job 设计 + 基线单向收紧门控 + 与 `compliance.yml` 集成方式（独立 job vs 复用）+ 发现分类工作流的自动化程度
      - Skill: none
- [ ] Add: §与 MA6 的正交边界 —— 显式声明 Q2（依赖链 CVE + 静态代码安全）与 MA6（运行时 RBAC 授权）正交互补不重复
      - Skill: none

Exit Criteria:

> 本计划纯文档，零代码/ORM/CI 变更。完整仓库 `typecheck`/`build`/`test` 不适用（按 plan authoring guide，无代码更改的计划删除验证命令门控）。

- [ ] `security-scanning.md` 落盘，含上述 6 节 + CVE 调度裁决 + 基线建立策略 + MA6 正交边界，两个 Decision 记录候选+替代+残留风险三要素
- [ ] §现状评估每条证据标注可复现核验命令 + 核验日期

### Phase 2 - 独立子代理设计文档审查循环（≥2 轮至收敛）

Status: planned
Targets: `docs/architecture/quality-engineering/security-scanning.md`（`## Review Record` 节）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1 草稿落盘

- [ ] Proof: 第 1 轮审查——**规范合规审查**，由独立子代理（新会话）执行。审查项：6 节结构完整性 / 与项目约定一致性 / 反模式检查（无双真相源、是否误把 MA6 RBAC 当安全扫描覆盖、单向收紧范式是否对齐 compliance-baseline）/ owner doc 引用正确性（compliance-baseline.md 范式 / roles-and-permissions.md MA6 边界）/ 5 CI job 清单准确性。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [ ] Proof: 第 2 轮审查——**覆盖面与可执行性审查**，由**另一个**独立子代理（不同 task id，新会话）执行。审查项：CVE 工具选型替代是否充分评估 / 静态规则工具选型是否充分 / nightly/per-commit/weekly 三调度路径是否可执行且充分评估限速风险 / 基线建立策略（首次发现分类工作流）是否可落地 / 单向收紧门控是否成立 / 与现有 5 CI job 是否冲突 / NVD 数据缓存策略。输出 BLOCKER/MAJOR/MINOR。
      - Skill: none
- [ ] Add: 作者据审查意见修订文档并重审，直至两轮均无 BLOCKER/MAJOR；`## Review Record` 节持久化两轮审查者 task id + 轮次 + 结论 + 修改摘要
      - Skill: none

Exit Criteria:

- [ ] §Review Record 记录 ≥2 轮审查，两轮由不同子代理会话执行，无残留 BLOCKER/MAJOR
- [ ] CVE 调度裁决 + 基线建立策略经审查后可执行（或据审查修订后可执行）

## Draft Review Record

- Independent draft review iteration 1: **accept-as-is**（`ses_04220e20affewLm7hTy33HjZcu`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 1 MINOR。M1（Baseline `mvn test 全绿` 缺测试计数 1903，与 Q4 sibling 不一致——已补 "1903 测试 0 failures"）。MINOR 已修订。Baseline 核验全 PASS（OWASP/spotbugs/snyk 零命中确认；5 CI workflow 名称精确匹配确认[compliance/e2e/maven/mutation/clock-rollover]；MA6 done 确认；compliance-baseline.md 单向收紧范式 §BASELINE 机器可读块确认；Q0 README Q2 位 5 确认）。NVD 限速关键风险在 Current Baseline + Goals + §CI 调度裁决 + Phase 2 R2 审查四点覆盖。MA6 RBAC 正交边界三处强化（Current Baseline / Non-Goals / §与 MA6 正交边界 item）。Phase-1 doc-only 范围保持。converged → 转 active。

## Closure Gates

> 本计划无代码/ORM/view/CI 变更（纯设计文档）。按 plan authoring guide §Closure Gates："对于无代码更改的计划（仅文档），删除验证命令门控并说明原因"——故不设 `mvn typecheck/build/test` 门控，原因：零 Java/ORM/CI 变更，全量构建无回归面。

- [ ] 范围内行为完成：`security-scanning.md` 6 节 + CVE 调度裁决 + 基线建立策略 + MA6 正交边界落盘且 Review Record 收敛
- [ ] 相关文档对齐：文档引用 Q0 README（无双真相源）；与 roadmap §MQ Q2 + compliance-baseline.md 范式一致
- [ ] 无验证命令门控（纯文档计划，原因如上）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查（本计划本身）已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中
- [ ] `docs/logs/{year}/{month}-{day}.md` 追加本计划日志条目（计划级结束步骤）

## Deferred But Adjudicated

### Q2 Phase 2 实现（Dependency-Check + FindSecBugs 接入 CI + 首次扫描 + 基线落盘）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: MQ 文档先行工作流强制要求 Phase 1 设计文档审查收敛后方可编写 Phase 2 实现 plan。本计划仅交付设计文档。
- Successor Required: yes —— 触发条件：本计划 done（设计文档审查收敛）+ 工具选型 + CI 调度 Decision 落定。届时 DRAFT_PLANS 起草 Phase 2 实现 plan，plan 引用本文档作为范围与验收依据。

### 首次扫描发现的具体 CVE/规则违规修复

- Classification: `optimization candidate`
- Why Not Blocking Closure: Q2 Phase 2 首次扫描必然有发现，修复是 Phase 2 基线建立后的分类修复 workflow，不属本 Phase 1 设计文档范围。
- Successor Required: yes —— 触发条件：Phase 2 首次扫描完成 + 发现分类完成，Critical/High 漏洞修复作为后续工作项。

## Closure

Status Note: （计划完成时填写）

Closure Audit Evidence:

- Auditor / Agent: （独立结束审计子代理，新会话 fresh cold context）
- Evidence: （完成时填写）

Follow-up:

- Q2 Phase 2 实现 plan（设计文档收敛后起草）。
