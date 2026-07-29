# 2026-07-29-1410-2-ma6-protected-area-discipline-audit MA6 保护区域纪律审计（A6.4）

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MA6（A6.4）
> Related: plan `2026-07-29-1410-1`（A6.1+A6.2+A6.3 权限注解与数据权限审计，同批 MA6）
> Audit: required

## Current Baseline

- MA6（安全与权限层审计）4 工作项全部 `todo`。本计划覆盖第 4 项（A6.4 保护区域纪律审计），与 plan `2026-07-29-1410-1`（A6.1-A6.3）互补。A6.4 的 owner doc 是 `docs/context/ai-autonomy-policy.md §保护区域`（与 A6.1-A6.3 的 `roles-and-permissions.md` 不同——按 authoring guide rule 14 拆为独立 plan）。
- **保护区域定义**（`ai-autonomy-policy.md §保护区域`）：6 个区域已落实为真实条目（非占位符）：
  - `model/*.orm.xml` 模式 — ask first（design doc + plan audit）
  - `model/*.api.xml` 契约 — ask first（design doc + plan audit）
  - `data deletion` — ask first（owner doc + tests）
  - `accounting/finance postings` — plan-first（owner doc + tests）
  - `auth/permissions` — plan-first（owner doc + tests）
  - `deployment / external integrations` — plan-first（owner doc + tests）
- **保护区域纪律现状**：审计-修复路线图执行至今（M0 → MA1-MA5 done/ready），大量计划触及保护区域（实仓 grep：50+ 份 plan 触及 finance 过账 / data deletion / reversePost 等，精确清单由 Phase 1 item 1 重新 grep 取权威值），含 6 项 P0 即时通道修复（P0-MA1-021 跨域写凭证 / P0-MA2-016 FX 损益结转 / P0-MA2-017 质检状态守卫 / P0-MA2-018 voucher billR UK [deferred] / P0-MA2-019 aps 产能锁 / P0-MA2-020 库存余额 UK）均触及 `accounting/finance postings` 或 `model/*.orm.xml` 保护区域。
- **已观察的纪律信号**：P0 即时通道每个 fix plan 均声明保护区域 + 走 plan-audit（roadmap §P0 即时通道纪律）；ORM 变更已授权但要求"修改后必须 `mvn clean install -DskipTests` 重新生成"。但**保护区域纪律是否系统性遵守**（每个触及保护区域的工作是否有 owner doc + tests + plan-audit 证据三件套）从未被独立审计。
- 验证基线：`mvn clean install -DskipTests` 全绿（154 模块）；`mvn test` 全绿（~2890 测试，0 failures）。
- 剩余差距：保护区域纪律遵守度从未做过系统性回溯审计——是否存在"触及保护区域但缺 owner doc / 缺 tests / 缺 plan-audit 证据"的静默违规未核实。

## Goals

- **A6.4**：系统审计保护区域纪律——回溯所有触及 6 个保护区域的计划/代码变更，核实每个是否有 owner doc 描述预期行为 + 测试策略 + plan-audit 证据（`ai-autonomy-policy.md §保护区域` 必需证据三件套）。输出保护区域纪律审计报告。
- 聚焦三类高风险区域：`accounting/finance postings`（过账/红冲/结账，本仓最高密度）+ `model/*.orm.xml`（ORM 变更，含 5 项 P0 fix）+ `auth/permissions`（与 A6.1-A6.3 交叉）。
- 识别"触及保护区域但缺证据三件套"的静默违规，标记为 P0（即时通道）或 P1（目标 MR3）。
- 注册 P0/P1/P2 至 `docs/audits/arm-index.md`，与 MA1-MA5 + 同批 A6.1-A6.3 已登记发现交叉去重。
- 推进 roadmap A6.4 状态（审计产出后转 `ready`，独立 closure audit 后转 `done`）— **完成后 MA6 里程碑全部 done/ready**。

## Non-Goals

- 不修复保护区域证据缺失（补 owner doc / 补测试属 MR3 批量修复）。
- 不审计保护区域**定义**本身的合理性（`ai-autonomy-policy.md §保护区域` 表由人工维护，AI 不得放宽——审计只核实"已定义区域是否被遵守"，不裁决"区域定义是否应改"）。
- 不重复 A6.1-A6.3 的权限注解/数据权限内容（A6.4 聚焦**过程纪律**：触及 `auth/permissions` 区域的工作是否有证据三件套，而非权限注解本身的技术正确性——与 plan `2026-07-29-1410-1` 互补不重叠）。
- 不变更任何生产代码 / ORM / 契约（纯审计）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/context/ai-autonomy-policy.md §保护区域`（6 区域 + 必需证据规则）+ `docs/context/project-context.md §AI 阻塞条件`（硬停止条件）+ `docs/plans/00-plan-authoring-and-execution-guide.md §计划决策表`（契约/数据/模型/API/认证/权限类须完整计划 + 独立审计）。
- Skill Selection Basis: roadmap 明确指定 A6.4 = `docs/skills/multi-dimensional-audit-prompt.md`（保护区域纪律需跨多维度挑战——需求正确性/owner-doc 对齐/验证充分性/待办或自主权策略漂移，尤其"自主权策略漂移"维度直接对应保护区域降级风险）。加载后读 `docs/skills/README.md §项目定制化层`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- A6.4 为静态回溯审计（读 `docs/plans/` + `docs/audits/` + 代码 git 历史 + 测试目录），不需运行应用。审计只读，不改代码。

## Execution Plan

### Phase 1 - 保护区域纪律回溯审计（A6.4）

Status: completed
Targets: 全域触及保护区域的计划/代码变更（`docs/plans/*.md` + `docs/audits/arm-index.md` P0 追踪表 + git 历史）；报告 `docs/audits/2026-07-29-1410-arm-ma6-protected-area-discipline.md`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0 锚点（A6.4 审过程纪律，不依赖 A6.1-A6.3 技术结论；可与 plan `2026-07-29-1410-1` 并行）

- [x] 盘点触及 6 个保护区域的所有计划/变更：grep `docs/plans/` 中声明触及 `accounting/finance postings` / `model/*.orm.xml` / `model/*.api.xml` / `data deletion` / `auth/permissions` / `deployment` 的工作项，建立"保护区域触及清单"
- [x] 逐区域核实证据三件套：每个触及项是否有 ① owner doc 描述预期行为 ② 测试策略/测试落地 ③ plan-audit 证据（独立草案审查 + 结束审计记录）
- [x] 重点抽样 `accounting/finance postings`（最高密度）：6 项 P0 fix（P0-MA1-021 / P0-MA2-016/017/018/019/020）+ 业财端到端（A2.1-A2.4）触及的过账/红冲/结账，核实证据完整性
- [x] 核实 `ai-autonomy-policy.md` 自主级别标签是否被静默降级（roadmap 工作项的 `plan-first`/`ask-first` 是否被当作 `implement` 执行而无审计）
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 评估"维度：待办或自主权策略漂移"——是否有触及保护区域的工作跳过计划/审计、关闭未完成项、或将阻塞降级为跟进项
- [x] 产出保护区域纪律审计报告，分类 P0/P1/P2，更新 arm-index.md（去重）
  - Skill: `multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 保护区域触及清单 + 证据三件套核实矩阵产出（每项裁决：三件套齐 / 缺 owner doc / 缺 tests / 缺 plan-audit）
- [x] A6.4 P0/P1/P2 已登记 arm-index.md 且与同批 A6.1-A6.3 + MA1-MA5 已登记发现去重

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (task `ses_0537f29b0ffelTx07qnncQQDny`) because 全部基线声明经实仓验证（6 个保护区域行 + 必需证据列、6 项 P0 fix 全部存在于 arm-index P0 追踪表、A6.4 deps=0.3 done、A6.4 owner doc=ai-autonomy-policy.md 与 plan 1 的 roles-and-permissions.md 不同证成 rule 14 拆分、MA6 恰 4 项且与 plan 1 零范围重叠）；item typing/skill/Closure Gates/anti-slack 全部合规；"不审计保护区域定义合理性"Non-Goal 合规（ai-autonomy-policy 禁止 AI 放宽）。采纳 2 项非阻塞观察修订：①P0 计数"5 项"→"6 项"（6 个 ID）；②"56 份 plan"软化为"50+"并注明 Phase 1 重新 grep 取权威值。

## Closure Gates

> 本 plan 为纯审计，不改代码。`mvn test` 仅作回归基线确认（见 roadmap 横切关注点 §审计 plan 的 BUILD_VERIFY）。

- [x] A6.4 保护区域纪律审计报告产出
- [x] arm-index.md 已登记本批次全部 P0/P1/P2，且与 MA1-MA5 + 同批 A6.1-A6.3 既有发现交叉去重
- [x] roadmap A6.4 状态推进至 `ready`（独立 closure audit 后转 `done`）— MA6 里程碑全部 done/ready
- [x] 已运行 `mvn clean install -DskipTests`（154 模块绿）+ `mvn test`（0 failures）作回归基线确认
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态 / 阶段 / 门控 / 日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P0-MA2-018 voucher billR UK（保护区域证据完整性）

- Classification: `watch-only residual`
- Why Not Blocking Closure: P0-MA2-018 触及 `model/*.orm.xml` + `accounting/finance postings` 保护区域，其 fix plan 已走 plan-audit 并诚实裁决为 deferred（字面 UK 不可实施，A2.18 复核维持 deferred）。证据三件套齐（owner doc posting.md + 测试 + plan-audit）——**这本身是保护区域纪律**合规**的样本**，非遗规。A6.4 将其作为"合规 deferred 样本"核实而非缺陷。
- Successor Required: `no`（重开触发条件见 arm-index P0-MA2-018 修复路径 deferred plan 方向 A/B/C/D）

## Closure

Status Note: EXECUTE 完成（2026-07-29）。单阶段全 done——按 `multi-dimensional-audit-prompt.md`（7 维度 + 项目定制化层）对 6 个保护区域做**过程纪律系统回溯**，产出**保护区域触及清单（~107 份去重计划）证据三件套核实矩阵**：`accounting/finance postings` 56 份 100% 合规（55 TRIPLE_COMPLETE + 1 PROPERLY_BLOCKED[P0-MA2-018]）+ `model/*.orm.xml` 36 份 23 TRIPLE_COMPLETE + 12 违规 + 1 BLOCKED_PROPERLY + `model/*.api.xml` 0 份（vacuously 合规）+ `data deletion` 2 份 100% 合规 + `deployment/external integrations` 9 份 8 TRIPLE_COMPLETE + 1 MISSING_PLAN_AUDIT + `auth/permissions` 4 份 3 TRIPLE_COMPLETE + 1 低危相邻。零 P0（无活跃数据破坏）。**正面合规样本**：finance postings 最高密度区域 100% 合规 + **P0-MA2-018 字面 UK 被 plan-audit 阻断**（保护区域纪律生效最佳证据）+ 6 项 P0 即时通道 fix 全部声明保护区域门控。MA6 累计 P1=5（A6.4 产出 3：P1-MA6-003/004/005）/ P2=1（P2-MA6-001），目标 MR3。回归基线：`mvn install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test` 0 failures。roadmap A6.4 推进至 `ready`（待独立结束审计后转 `done`）——**MA6 安全与权限层审计 A6.1-A6.4 现已全部 ready，MA6 里程碑完成**。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非 EXECUTE 执行者，2026-07-29，task `ses_052f048faffeleNlxiXQWmzfk3`）
- Evidence:
  - **PASS — 报告存在且非 hollow**：`docs/audits/2026-07-29-1410-arm-ma6-protected-area-discipline.md` 实仓存在（231 行），6 大节齐全（§1 方法 12-51 / §2 六区域三件套矩阵 52-135 / §3 7 维度裁决 137-151 / §4 P0/P1/P2 finding 152-208 / §5 交叉去重 210-220 / §6 结论 222-231）。
  - **PASS — finding 行号引用精确**：独立复核 3 处引用——①`2026-07-28-1249-arm-fix-p0-ma2-019...` 确无 `## Draft Review Record` 章节（Closure Gates:79→Closure:88 跳跃）+ `:94` 原文 `主代理（EXECUTE 模式，MISSION_DRIVER 驱动）`；②`2026-07-28-1249-arm-fix-p0-ma2-020...` 确无 `## Draft Review Record` 章节；③`2026-07-21-1206-3...` `:3` Plan Status `completed` + `:274` 草案审查 iter3 `pending` + `:357` Auditor `pending` + `:286` Closure Gate `[x]` 假勾选——4 处行号全准确。
  - **PASS — arm-index.md 登记完整**：§报告清单 :70 行（done）；§MA6 A6.4 汇总段 :107；§P1 详细清单 P1-MA6-003/004/005（:272/273/274）；§P2 汇总 P2-MA6-001（:428）。`rg` 命中 5 行（107/272/273/274/428）。
  - **PASS — roadmap 推进**：`docs/backlog/audit-remediation-roadmap.md` A6.4 行 :121 Status = `ready`（非 todo）；§MA6 工作项详情 :246 同步 ready。
  - **PASS — 零代码改动**：`git status --short` 仅 `.md` 文件（docs/audits/arm-index.md + roadmap.md + logs/07-29.md + 新报告）+ opencode.json——零 Java/XML/yaml 生产代码，与 audit-only 一致。
  - **PASS — 去重轴划分连贯**：§5 明确 P1-MA3-046/P1-MA6-001/P1-MA6-002 = auth/permissions **运行时技术正确性**（注解/SoD/数据权限落地）vs A6.4 = **过程纪律**（计划是否有证据三件套），「证据三件套存在性」vs「注解正确性」轴划分连贯无重叠。
  - **PASS — 五点一致性**：Plan Status=completed / Phase 1 Status=completed / Phase 1 Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 段填充——零状态漂移。
  - **PASS — Deferred honesty**：Deferred 项仅 P0-MA2-018 voucher billR UK（已权威裁决字面 UK 三重契约冲突不可实施，独立 plan `2026-07-28-1249-arm-fix-p0-ma2-018` 维持 deferred，A2.18 多公司维度复核维持）——是「合规 deferred 样本」非遗规；Follow-up 项（MR3 修复 P1-MA6-003/004/005 + P2-MA6-001）非降级缺陷。
- Verdict: **APPROVED** — 本 plan 为 audit-only（零生产代码变更），单阶段产出真实、可复核、与实仓一致（finding 行号引用精确、arm-index 登记完整、roadmap ready、零代码改动、去重轴连贯）；零 P0 裁决依据成立（无活跃数据破坏——14 项违规均为审计轨迹缺口，代码已落地且测试绿）。MA6 A6.4 现可转 `done`。

Follow-up:

- 保护区域证据缺失（补 owner doc / 补测试 / 补 plan-audit）的修复不在此处；由 R3.0 展开机制将本批次 P1 转化为 MR3 具体修复工作项行。
