# 2026-08-23-1835-2-m02-infra-pilot-mechanism-adjudication 基建试点与机制裁决（M0.2）

> Plan Status: active
> Mission: integration-test
> Work Item: M0.2
> Last Reviewed: 2026-08-23
> Source: `docs/backlog/integration-test-roadmap.md`（M0.2 工作项规格，v3）
> Related: `2026-08-23-1835-1-m01-integration-test-case-design`（前置，本计划执行时须已 done）、`2026-08-23-1835-3-m03-batch-work-item-expansion`
> Audit: required

## Current Baseline

- **前置依赖**：M0.1 设计文档（`docs/design/integration-testing.md`）定稿为本计划执行前提——机制选型（a/b/c 裁定方案）、试点用例规格（P2P 简化链）、四风险判定方法均出自该文档；本计划按该方案落地并**逐项实证待证风险清单**（M0.2 规格 item 3），试点失败按回退路线降级（item 4）。
- app-erp-all 已含 `nop-autotest-junit` test 依赖（`app-erp-all/pom.xml:222-226` 实证）——录制回放基类（`JunitAutoTestCase`）可用。
- 父 POM surefire `forkCount=4 + reuseForks=true + parallel=classes`（`../nop-entropy/pom.xml:209/216/223` 实证），app-erp-all pom 无模块级覆盖（实测仅 systemPropertyVariables）→ **并行 fork × 文件型 H2（`jdbc:h2:./db/erp`，application.yaml）竞态（BLOCKER 级，本计划必须解决）**。
- `TestAuthSeedLoadingProof` 宿主模式（BaseTestCase + 手动 `CoreInitialization.initialize()` + 文件 H2 fresh 清理）与 `JunitAutoTestCase` 硬编码 NopJunitExtension 的类继承共存无既有证据（待证风险 ①）。
- CHECKING 态 DB 组成：`AutoTestCaseDataBaseInitializer.createTables()` 只建 input/output 文件涉及的表 → 回放态 DB ≠ 全量 94 seed（待证风险 ②）。
- 运行时互斥：集成测试与 Playwright E2E live server 共用 `db/erp.mv.db`——执行期不得与 live E2E server 同时运行（`_tmp-server.sh` 或 `lsof -i :8011` 确认）。
- 既有测试基线（known-good-baselines 2026-08-23 权威计数源）：3808 tests / 0 failures / 0 errors / 1 skipped；app-erp-all 12 测试类全基建类零业务集成测试。
- 用例粒度约定（roadmap M0.2 规格，roadmap:70「用例粒度约定：1 用例 = 1 测试类 1 测试方法」；M0.1 设计文档 Phase 1 同步确认）：1 用例 = 1 测试类 1 测试方法，`@BeforeAll` 类级初始化，fresh-DB 每类 1 次。

## Goals

- app-erp-all surefire **串行化**落地（模块级 `forkCount=1` + `parallel=none` 或独立 surefire execution 隔离）并验证生效，消除文件 H2 并行 fork 竞态。
- 1 个试点用例（P2P 简化链，按 M0.1 设计文档规格）执行 **RECORDING→CHECKING 往返**，证明三层全比对机制（response + tables + JUnit 关键断言）可行。
- **逐项实证 M0.1 选型机制的待证风险清单**（5 项：NopJunitExtension 共存 / CHECKING 态 DB 组成 / per-method restart × 文件 H2 / 快照构成与体积 / 单用例耗时），记录结论。
- 试点失败回退路线：若选型机制证伪 → 降级标准范式 (a) + 调整用户裁决口径（须人工裁决登记）。
- 共享 step helper 集（跨域链步骤/过账断言 helper，对标 Playwright `_helper.ts` 先例）供 B1-Bn 复用。
- `docs/testing/e2e-runbook.md` 增「集成测试」运行注记（运行方式/fresh-DB 纪律/与 E2E server 互斥）。
- roadmap M0.2 状态 `todo` → `done`（经独立 closure audit）。

## Non-Goals

- 不实现 B1-Bn 用例（归 M0.3 后的分批计划）。
- 不默认改 seed CSV（不属本计划默认执行面；确需修正时走 roadmap 横切关注点 3「seed 修正授权」流程——修改部署期 CSV 属授权例外路径，须登记理由 + 同步快照重录 + 评估既有 E2E 数值断言影响）；不修生产代码/ORM/契约（**产品缺陷按横切关注点 4 走保护区域门禁，不在本计划内直接改生产代码**）。
- 不接 CI（roadmap 已裁决出 scope）。
- 不做浏览器层 E2E 页面验证（与 Playwright 分工边界）。

## Task Route

- Type: `implementation-only change` + 机制裁决（测试基建 + 试点用例，零生产代码/ORM/契约变更）
- Owner Docs: `docs/design/integration-testing.md`（M0.1 产物，本计划执行时存在）、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/backlog/integration-test-roadmap.md`
- Skill Selection Basis: `nop-testing` 匹配测试基类选择/@NopTestConfig/快照录制回放（RECORDING→CHECKING）/三层验证模型——必读 `05-examples/test-examples.java` + `02-core-guides/testing.md` + `03-runbooks/write-integration-test-with-noptestconfig.md` 均实证存在。机制裁决中的待证风险判定需要结合宿主模式源码实证（`TestAuthSeedLoadingProof`）与 JunitAutoTestCase 源码（nop-entropy）。

## Infrastructure And Config Prereqs

- 文件型 H2 `db/erp.mv.db`：试点执行前 fresh 清理（复用 `TestAuthSeedLoadingProof` 的 `.mv.db/.trace.db` 删除模式）；**与 E2E live server 互斥**——执行前 `lsof -i :8011` 确认无 live server。
- 无端口/密钥/外部服务新增；无环境变量新增（surefire 配置为 pom 内模块级覆盖）。
- 若试点发现需要 seed 修正：走 roadmap 横切关注点 3「seed 修正授权」流程（修改部署期 CSV + 同步快照重录 + 评估既有 E2E 数值断言影响），并登记理由。

## Execution Plan

### Phase 1 — surefire 串行化 + 集成测试脚手架（Add：2/2 项为 Add）

Status: planned
Targets: `app-erp-all/pom.xml`（surefire 模块级覆盖）、`app-erp-all/src/test/java/`（试点用例类）
Skill: nop-testing

- Item Types: `Add`
- Prereqs: M0.1 done（设计文档存在）

- [ ] Add：app-erp-all surefire 串行化——模块级 `<forkCount>1</forkCount>` + `<parallel>none</parallel>`（或独立 surefire execution 隔离），消除并行 fork × 文件 H2 竞态（roadmap 已知约束 1）。
  - Skill: nop-testing
- [ ] Add：试点用例类脚手架（按 M0.1 选型机制的基类/装配 + 1 个试点用例 P2P 简化链测试类 + `_cases` 目录结构）。
  - Skill: nop-testing

Exit Criteria:

- [ ] app-erp-all surefire 配置生效（`mvn test -pl app-erp-all` 单 fork 运行实证——可观测判据：surefire 测试输出显示同一 JVM 顺序执行全部测试类且无 fork 重启特征；`-Dtest=...` 全类跑完无并行 fork 日志特征）
- [ ] 试点用例类存在且可按 M0.1 机制装配

### Phase 2 — 试点 RECORDING→CHECKING 往返（Proof：2/2 项为 Proof）

Status: planned
Targets: 试点用例 `_cases` 快照（input/output）+ 测试类
Skill: nop-testing

- Item Types: `Proof`
- Prereqs: Phase 1

- [ ] Proof：试点用例 RECORDING 录制往返——`snapshotTest=RECORDING` 首次录制生成 output/response + output/tables（确认快照构成与体积：只写变更行，非全库）。
  - Skill: nop-testing
- [ ] Proof：切换 CHECKING 校验往返全绿——三层全比对（response 快照模式 + DB 状态快照 + JUnit 关键断言：成功/失败、状态翻转、金额正确性）通过。
  - Skill: nop-testing

Exit Criteria:

- [ ] 试点用例录制→回放往返通过（三层全比对机制可行性证明；成功/失败模式明确记录）
- [ ] 快照构成与体积结论记录（待证风险 ④ 初步实证）；若结论为「快照体积不可接受/构成含噪声不可比」→ 落入 Phase 3 回退路线（降级标准范式 (a) + 人工裁决）

### Phase 3 — 待证风险逐项实证 + 机制裁决（混合类型：1/3 Proof + 1/3 Decision + 1/3 Add，逐项标注）

Status: planned
Targets: 设计文档/裁决记录、`app-erp-all/src/test/java/`（step helper 集）、`docs/testing/e2e-runbook.md`
Skill: nop-testing

- Item Types: `Decision | Proof | Add`
- Prereqs: Phase 2

- [ ] Proof：**逐项实证 5 项待证风险**——① NopJunitExtension 共存（JunitAutoTestCase 硬编码 NopJunitExtension × 宿主模式类继承/装配矛盾是否成立，记录实证证据）；② CHECKING 态 DB 组成（回放态 DB ≠ 全量 94 seed 是否导致断言失败或需 input/tables 补齐）；③ per-method `container.restart()` × 文件 H2（重启语义与文件 DB 的兼容性）；④ 快照构成与体积（变更行非全库确认 + 体积量级）；⑤ 单用例耗时（启动 + 执行实测）——逐项记录结论。
  - Skill: nop-testing
- [ ] Decision：机制裁决——选型机制成立（记录结论与证据）或证伪（触发回退路线：降级标准范式 (a) + 用户裁决口径调整登记，须人工裁决）。
  - Skill: nop-testing
- [ ] Add：共享 step helper 集（跨域链步骤/过账断言 helper，供 B1-Bn 复用；对标 Playwright `_helper.ts` 先例）+ `docs/testing/e2e-runbook.md` 增「集成测试」运行注记（运行方式/基线/fresh-DB 纪律/与 E2E server 互斥纪律）。
  - Skill: nop-testing

Exit Criteria:

- [ ] 5 项待证风险全部实证并有结论记录（每项：实证方式 + 结论 + 影响）
- [ ] 机制裁决结论落盘（成立证据或回退登记 + 人工裁决痕迹）
- [ ] step helper 集存在 + runbook 集成测试注记落地

### Phase 4 — 收尾与回归（Proof-heavy）

Status: planned
Targets: `docs/backlog/integration-test-roadmap.md`（M0.2 → done）、`docs/logs/2026/08-23.md`、`docs/testing/known-good-baselines.md`（如适用）
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: Phase 3

- [ ] Proof：全量 reactor `mvn clean install -DskipTests` BUILD SUCCESS + 全量 `mvn test` 零新增失败（与 3808/0/0/1 基线口径一致；surefire 串行化对既有测试无回归）。
  - Skill: none
- [ ] Fix：roadmap M0.2 → done + `docs/logs/2026/08-23.md` 日志条目（按日志书写指南）+ 如适用登记 known-good-baselines（试点用例新增测试计入计数）。

Exit Criteria:

- [ ] 全量 build + test 零新增失败（基线口径核对）
- [ ] roadmap M0.2 = done + 日志条目存在

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fd1cb18fcffekx2YLbO1gyaTl0，独立 general 子代理新会话) — 0 BLOCKER / 1 MAJOR / 4 MINOR。基线事实全部实测准确零漂移（nop-autotest-junit 依赖 / 无 forkCount 模块级覆盖 / 父 POM forkCount=4 / TestAuthSeedLoadingProof 宿主模式 / JunitAutoTestCase :29/:85 / application.yaml jdbc:h2:./db/erp / 3808-0-0-1 / 12 测试类 / skill 文档实证）；M0.2 规格 6 项全覆盖 + 横切关注点 #4 门禁语句正确 + M0.1 依赖正确声明为执行前提。MAJOR-1 Phase 1 声明 `Add | Proof` 但 2 项全 Add（应统一 `Add`）、Phase 2 声明 `Add | Proof` 但 2 项全 Proof（应统一 `Proof`），违反规则 7。MINOR-1 Non-Goals「不改 seed CSV」与 Infra prereq 授权流程字面矛盾；MINOR-2 粒度约定归因「M0.1 Phase 1 产物」不可核验（实为 roadmap M0.2 规格）；MINOR-3 Phase 1 退出标准「surefire 日志/配置检查」验证手段过宽；MINOR-4 快照构成风险 ④ 结论未与 Phase 3 回退路线联动。修订全部落地 + 日志路径 `2026-08-23.md` → `08-23.md`。
- Independent draft review iteration 2: accept (ses_fd1c7b45cffeTT9oKXdpHY1YCI，独立 general 子代理新会话) — 0 BLOCKER / 0 MAJOR / 2 MINOR。迭代 1 五项发现 + 日志路径全部核验 FIXED（含可观测判据、回退联动、归因改 roadmap:70）；复扫 5 风险清单与 roadmap M0.2 规格逐字一致、回退路线/step helper/runbook 注记对应 item 4/5/6、Closure Gates 完整。MINOR-1 Phase 3 头「Decision-heavy 2/3」计数不实（实际 1/3，装饰性）；MINOR-2 pom 行号 220-224 应为 222-226（内容实证正确）。**共识达成，计划可执行。**（两 MINOR 已在本轮修订落地）

## Closure Gates

- [ ] 范围内行为完成（串行化 + 试点往返 + 5 风险实证 + 裁决 + step helper + runbook 注记）
- [ ] 相关文档对齐（设计文档 ↔ roadmap ↔ e2e-runbook ↔ known-good-baselines 无矛盾）
- [ ] 已运行验证（`mvn clean install -DskipTests` + 全量 `mvn test` 零新增失败；surefire 串行化生效确认）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

- （执行后按需填写；候选：试点用例覆盖不足的领域——若 P2P 链试点仅覆盖 pur/fin/inv 部分路径，其余域归 B1-Bn）

## Closure

Status Note: （执行后填写）

Closure Audit Evidence:

- Auditor / Agent: （独立子代理新会话）
- Evidence: （任务 id / 核对记录）

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得出现于此）