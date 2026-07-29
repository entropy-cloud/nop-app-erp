# 2026-07-29-1430-2-ma5-cross-cutting-test-audit MA5 跨切测试审计（隔离性 + E2E 有效性）

> Plan Status: active
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MA5（A5.5 / A5.6）
> Related: plan `2026-07-29-1430-1`（A5.1-A5.4 S 级域覆盖深度，同批 MA5）；plan `2026-07-24-1945-1`（测试隔离污染诊断，已解除 5 项 Category (a)）
> Audit: required

## Current Baseline

- MA5（测试层审计）6 工作项全部 `todo`；本计划覆盖后 2 项（A5.5 测试隔离性 + A5.6 E2E 有效性）。A5.1-A5.4 由 plan `2026-07-29-1430-1` 覆盖。
- **A5.5 测试隔离性**：`docs/testing/known-good-baselines.md` 2026-07-25 绿基线记录——2026-07-23 全量门控发现的 5 项 Category (a) test-isolation 污染（inv totalValue / mfg KPI / md KPI / md noSkuAlert / o2c-chain COGS）经诊断 dump 证实**不再复现**（残留状态 = pristine 种子基线，diff=0）。诊断方法沉淀于 `docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md`。roadmap 仍记「已知 5 项残留收敛」— **审计须验证当前隔离态**（5 项是否真解除 / 是否出现新污染物）。
- **A5.6 E2E 有效性**：`tests/e2e/` 实测 258 spec（roadmap 记「260+」）。已知分布：crud / business-actions / orchestration / dashboards / reports / visual / examples。`docs/testing/test-depth-classification.md` E2E 不在其计数口径（仅计 `src/test` JUnit）。已知 E2E 基础设施缺口（来自各历史 plan Deferred）：AMIS `$var` 损坏 8 参数化看板数值断言 [bug 2026-07-09-1249] / 序列碰撞致 CRUD 写路径受限 / xwf 浏览器层审批轴不可行 [plan 2026-07-09-2330-1 裁决 NOT FEASIBLE]。
- 验证基线：`mvn test` 全绿（~2890 单元测试）；E2E 全套件 490 passed / 1 failed [master-data.write.amis 测试基础设施 Non-Goal] / 3 skipped（2026-07-25）。
- 剩余差距：测试隔离性未做过系统性闭包审计（仅有一次性诊断）；E2E 258 spec 的**业务断言强度**从未被抽样评估（冒烟层 GraphQL 200 / 数值层直调后端 / 视觉层 DOM 哪些域有 / 哪些仅冒烟）。

## Goals

- **A5.5**：系统审计全域 JUnit 测试隔离性，验证 5 项已知污染物当前状态 + 主动搜索新污染物（跨测试状态泄漏 / 共享夹具 / 顺序依赖），输出隔离性审计报告。
- **A5.6**：抽样评估 E2E 258 spec 业务断言强度，分类「数值断言 / DOM 断言 / 仅冒烟（GraphQL 200）」覆盖分布，标记仅冒烟无业务断言的薄弱 spec，输出 E2E 有效性审计报告。
- 注册 P0（即时通道）/ P1（目标 MR3）/ P2（watch-only）发现至 `docs/audits/arm-index.md`，与 MA1-MA4 + plan 2026-07-29-1430-1 已登记 P1 交叉去重。
- 推进 roadmap A5.5-A5.6 状态（审计产出后转 `ready`，独立 closure audit 后转 `done`）— **完成后 MA5 里程碑全部 done**。

## Non-Goals

- 不修复测试隔离缺陷或增强 E2E 断言（修复属 MR3 批量修复）。
- 不重复 A5.1-A5.4 的 S 级域单元覆盖深度（plan 2026-07-29-1430-1）；本计划聚焦**跨切**测试基础设施质量（隔离性是全域行为，E2E 是浏览器层）。
- 不解决 xwf 浏览器层审批轴不可行裁决（plan 2026-07-09-2330-1 已权威裁决 NOT FEASIBLE，非测试缺陷）。
- 不变更任何生产代码 / ORM / 契约（纯审计）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/testing/`（`known-good-baselines.md` + `test-depth-classification.md` + `e2e-runbook.md`）+ `docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md`。
- Skill Selection Basis: 两项工作项 roadmap 明确指定 `docs/skills/open-ended-audit-prompt.md`（隔离性与 E2E 有效性均需主动搜索未知污染物 / 未知薄弱断言，非结构化清单）。加载后读 `docs/skills/README.md §项目定制化层`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- A5.5 隔离性验证需可跑 `mvn test`（fresh-DB 累积执行 + 乱序执行对照）；A5.6 E2E 验证需可跑 Playwright（`tests/e2e/`，`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1`）。审计只读，不改代码。

## Execution Plan

### Phase 1 - 测试隔离性审计（A5.5）

Status: planned
Targets: 全域 `module-*/src/test/java/**`；报告 `docs/audits/2026-07-29-1430-arm-ma5-test-isolation.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0 锚点

- [ ] 验证已知污染物当前状态：5 项原 Category (a)（inv totalValue / mfg KPI / md KPI / md noSkuAlert / o2c-chain COGS）+ 第 6 项执行期发现并修复的 fin-period-close-wizard config 缺口，对照 `docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md` 确认是否真解除（含回归验证）
- [ ] 主动搜索新污染物：grep 共享静态/单例夹具、未清理的 `@BeforeEach`/`@AfterEach`、跨测试 `cleanupXxx` 顺序假设、依赖种子 id 硬编码的断言
- [ ] 抽样乱序 / 累积执行验证（fresh-DB 基线 vs 累积执行 diff），确认无新状态泄漏
      - Skill: `open-ended-audit-prompt.md`
- [ ] 评估隔离性根因模式：是否需提升为可复用 lesson（`docs/lessons/`）或 skill
- [ ] 产出隔离性审计报告，分类 P0/P1/P2，更新 arm-index.md（去重）
  - Skill: `open-ended-audit-prompt.md`

Exit Criteria:

- [ ] 隔离性报告产出，含 5 项已知污染物状态裁决表 + 新污染物搜索结果（若有）
- [ ] A5.5 P0/P1/P2 已登记 arm-index.md 且去重

### Phase 2 - E2E 有效性审计（A5.6）

Status: planned
Targets: `tests/e2e/**/*.spec.ts`（258 spec）；报告 `docs/audits/2026-07-29-1430-arm-ma5-e2e-effectiveness.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0 锚点（A5.6 审 Playwright 层，不依赖 A5.5 的 JUnit 隔离结论；两 phase 可并行）

- [ ] 建 E2E 258 spec 业务断言强度分类矩阵：数值断言（`*.value.spec.ts` 直调后端断言确定性数值）/ DOM 断言（`*.visual.spec.ts` 前端渲染结构）/ 业务动作（`*.action.spec.ts` 状态翻转 + 副作用）/ 编排链（`orchestration/` 跨域过账产物）/ 仅冒烟（GraphQL 200 + 存在性，无业务数值断言）
- [ ] 标记仅冒烟无业务断言的薄弱 spec（尤其 crud smoke / reports smoke），评估「GraphQL 200 即通过」是否掩盖后端返回空数据
- [ ] 交叉验证已知 E2E 缺口状态：AMIS `$var` 损坏 8 参数化看板 [bug 2026-07-09-1249] 是否仍有数值断言 successor / master-data.write.amis Non-Goal 是否仍为唯一失败
- [ ] 评估 E2E 与单元层覆盖互补性（哪些业务路径仅 E2E 覆盖 / 仅单元覆盖 / 双层无覆盖）
- [ ] 产出 E2E 有效性审计报告，分类 P0/P1/P2，更新 arm-index.md（去重）
  - Skill: `open-ended-audit-prompt.md`

Exit Criteria:

- [ ] E2E 有效性报告产出，含 258 spec 断言强度分类矩阵 + 薄弱 spec 清单
- [ ] A5.6 P0/P1/P2 已登记 arm-index.md 且去重

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (task `ses_053d6a71fffeImRu272n0UIPqj`) because 全部基线声明经实仓验证（E2E 258 spec、A5.5/A5.6 todo、5 项污染物与 analysis 文件一致、与 sibling plan 2026-07-29-1430-1 零范围重叠）；A5.5+A5.6 合并为单 plan 符合 rule 14（同为跨切测试基础设施质量审计）；item typing 正确；exit criteria 无全仓验证泄漏；anti-slack 零命中；xwf deferred 命名了重开触发条件。审查提出 3 项非阻塞观察，已采纳 2 项修订：①Phase 2 人工 prereq（A5.6 不依赖 A5.5）改为 `Prereqs: M0 锚点` 并注明可并行；②第 6 项污染物 fin-period-close-wizard 补入 Phase 1 验证项以增强可追溯性。第 3 项（审计 plan 跑全量 mvn test）已在 Closure Gates 头注明为回归基线确认，维持。

## Closure Gates

> 本 plan 为纯审计，不改代码。`mvn test` + E2E 仅作回归基线确认。

- [ ] A5.5 隔离性报告 + A5.6 E2E 有效性报告产出
- [ ] arm-index.md 已登记本批次全部 P0/P1/P2，且与 MA1-MA4 + plan 2026-07-29-1430-1 既有 P1 交叉去重
- [ ] roadmap A5.5-A5.6 状态推进至 `ready`（独立 closure audit 后转 `done`）— MA5 里程碑全部 done
- [ ] 已运行 `mvn clean install -DskipTests`（154 模块绿）+ `mvn test`（0 failures）+ 抽样 E2E 回归
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态 / 阶段 / 门控 / 日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### xwf 浏览器层审批轴 E2E

- Classification: `watch-only residual`
- Why Not Blocking Closure: plan `2026-07-09-2330-1` 已权威裁决 NOT FEASIBLE（sysUser(0) 兜底层阻断），4 实体审批浏览器层 E2E 不可达。非测试缺陷，属平台限制。
- Successor Required: `no`（触发条件：nop-entropy 修复 sysUser 兜底或提供浏览器层身份映射 API）

## Closure

Status Note: <待两报告产出 + arm-index 登记 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立审计子代理>
- Evidence: <task id / 报告链接 / walkthrough record>

Follow-up:

- 隔离缺陷 / E2E 断言增强修复不在此处；由 R3.0 展开机制将本批次 P1 转化为 MR3 具体修复工作项行。
