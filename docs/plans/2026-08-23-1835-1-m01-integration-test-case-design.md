# 2026-08-23-1835-1-m01-integration-test-case-design 集成测试用例设计文档（M0.1）

> Plan Status: completed
> Mission: integration-test
> Work Item: M0.1
> Last Reviewed: 2026-08-23
> Source: `docs/backlog/integration-test-roadmap.md`（M0.1 工作项规格，v3）
> Related: `2026-08-23-1835-2-m02-infra-pilot-mechanism-adjudication`（依赖本计划产物）、`2026-08-23-1835-3-m03-batch-work-item-expansion`
> Audit: required

## Current Baseline

- roadmap v3（2026-08-15 定稿）M0.1/M0.2/M0.3 均 `todo`；`docs/design/integration-testing.md` **不存在**（本计划需新建）。
- app-erp-all 单模块 12 个测试类全为基建类（auth 4：TestAuthSeedEncodingProof/TestErpDataAuthStructure/TestAppActionAuthMerge/TestAuthSeedLoadingProof；web 5：ErpAllFluxPagesTest/ErpAllWebPagesCollectTest/ErpAllWebPagesTest/ErpFluxDiffDemoTest/ErpFluxDualFileAndComplexTest；seed 1：TestErpSeedDataIntegrity；meta 1：TestModuleMetaReader；job 1：TestErpAllJobYamlLoading），**零业务集成测试**（roadmap 记「11 个」，实仓 find = 12 文件，计数漂移——本计划以实仓为准）。
- 部署期 seed：`app-erp-all/src/main/resources/_vfs/_init-data/` **94 CSV + 1 SQL**（zz-sequence-advance.sql）实证。
- 既有业务参照：12 个 `*EndToEnd` 集成测试类（fin 3 / inv 2 / mfg 3 / pur 1 / qa 1 / sal 2）构成单用例步骤与断言的业务参照，**不重复实现其内部细节**。
- 全仓测试基线（known-good-baselines 2026-08-23 权威计数源）：3808 tests / 0 failures / 0 errors / 1 skipped（唯一 skipped = `ErpAllWebPagesCollectTest` `@Disabled` 预存 JDK26/ANTLR H-2）；156 reactor 模块 BUILD SUCCESS。
- 机制约束实证：
  1. `JunitAutoTestCase` 硬编码 `@ExtendWith({NopJunitExtension.class, NopJunitParameterResolver.class})`（`../nop-entropy/nop-autotest/nop-autotest-junit/src/main/java/io/nop/autotest/junit/JunitAutoTestCase.java:29`）+ 缺 `@NopTestConfig` 抛异常（`:85`）；`TestAuthSeedLoadingProof` 宿主模式（BaseTestCase + 手动 `CoreInitialization.initialize()` + 文件型 H2 fresh 清理）绕开 NopJunitExtension 的 ALL_LAZY 时序——类继承层面二者共存无既有证据（M0.2 待证风险 ①，本计划给出判定方法）。
  2. 父 POM surefire `forkCount=4 + reuseForks=true + parallel=classes`（`../nop-entropy/pom.xml:209/216/223` 实证；roadmap 记 `1C`，实仓为 `4`，计数漂移以实仓为准），app-erp-all 无模块级覆盖（pom 仅 systemPropertyVariables）→ 文件型 H2（`jdbc:h2:./db/erp`，application.yaml）并行 fork 竞态（BLOCKER 级，M0.2 必须解决）。
  3. xwf 审批流限制：Payment/Receipt 的 `submitForApproval` 被 wf 步骤参与者 `user:$0` 拒绝（2026-07-09-2330-1 实证）——用例设计必须以当前实现为准，只选用已验证可执行的审批流。
- 横切关注点：本计划零 ORM/会计路径/数据删除变更（纯设计文档）；「以当前实现为准」——用例设计不含 RC-R1.44+ 未落地项与 entity-state-machine M5 未落地项。

## Goals

- 产出 `docs/design/integration-testing.md`：15-25 用例全量设计——每用例含业务目标（对应核心业务闭环）/前置 seed/关键路径步骤（GraphQL 动作序列）/三层断言（response 快照 + DB 状态 + JUnit 关键断言）/主导域/涉及域，覆盖全 19 子系统每域至少 1 次、目标每域 ≥2 次。
- 用例覆盖矩阵验收表（每域出现次数，目标每域 ≥2 次）。
- 测试机制选型论证（选项 a/b/c）+ 四风险可行性判据（NopJunitExtension 共存 / CHECKING 态 DB 组成 / per-method restart × 文件 H2 / seed 变更敏感性），M0.2 可直接逐项实证。
- 经独立子代理审查收敛（≥2 轮）后定稿；roadmap M0.1 状态 `todo` → `done`（经独立 closure audit）。

## Non-Goals

- 不写任何测试代码、不动 app-erp-all 构建配置（归 M0.2）。
- 不向 M1-Bn 里程碑追加工作项（归 M0.3）。
- 不改 seed CSV、不改生产代码、不接 CI。
- 不设计未实现功能的用例（以当前实现为准）。

## Task Route

- Type: `app-layer design change`（测试用例设计文档，纯文档产出）
- Owner Docs: `docs/backlog/integration-test-roadmap.md`（M0.1 规格）、`docs/testing/e2e-runbook.md`、`docs/architecture/seed-data.md`、`docs/testing/known-good-baselines.md`、`docs/design/integration-testing.md`（本计划新建）
- Skill Selection Basis: `nop-testing` 匹配测试基类选择/快照机制/三层验证模型/录制回放机制选型论证——其必读文档（`05-examples/test-examples.java` + `02-core-guides/testing.md` + 按场景 `03-runbooks/write-tests.md`）均已实证存在于 `../nop-entropy/docs-for-ai/`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯文档计划，不运行测试/构建/服务器）。

## Execution Plan

### Phase 1 — 机制选型论证（混合类型：2/4 Decision + 2/4 Proof，低于 80% 统一类型阈值，逐项标注）

Status: completed
Targets: `docs/design/integration-testing.md` §机制选型
Skill: nop-testing

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] Proof：盘点机制实证素材并写入设计文档——宿主模式源码要点（`TestAuthSeedLoadingProof` 手动初始化 + 文件 H2 fresh 清理）、`JunitAutoTestCase` 硬编码注解（nop-entropy :29/:85）、父 POM surefire 配置（forkCount=4）、94 seed 清单、12 个 EndToEnd 业务参照、xwf 审批流可执行性实证（2026-07-09-2330-1）。
  - Skill: nop-testing
  - 落地：设计文档 §2 基线盘点（B1-B17，实仓逐项核实零漂移）+ 附录 A 源码锚点表。
- [x] Decision：机制选项裁定——(a) 标准 input/tables 范式（94 seed 仅录制期富集，回放态=input/tables 快照）vs (b) 自研基类（手动初始化 + AutoTestOrmHook 级 DB 采集 + 自实现三层比对）vs (c) 抑制 tableInit 的文件 H2 双模方案；记录选择理由、考虑的替代方案、残留风险；四风险各给出判定方法（NopJunitExtension 共存 / CHECKING 态 DB 组成 / per-method `container.restart()` × 文件 H2 / seed 变更敏感性）。
  - Skill: nop-testing
  - 落地：设计文档 §3.1/§3.2（选定 (c)，(a) 为失败回退，(b) 被拒）+ §3.3 四风险判定方法（M0.2 试点 1-4，可直接逐项实证）。
- [x] Decision：用例粒度与规模约定——1 用例 = 1 测试类 1 测试方法（`@BeforeAll` 类级初始化，fresh-DB 每类 1 次）；预计耗时模型（15-25 类 × ~12s 启动 ≈ 3-6 分钟纯启动 + 各用例执行）写入设计文档。
  - Skill: nop-testing
  - 落地：设计文档 §4（1 用例 = 1 测试类 1 方法 + @BeforeAll 类级 fresh-DB + 22 类 ≈ 8-26 分钟总模型）。
- [x] Proof：xwf 限制规避清单——设计文档登记当前已验证可执行的审批流集合（排除 Payment/Receipt `submitForApproval` 的 `user:$0` 被拒路径），供用例设计引用。
  - Skill: none
  - 落地：设计文档 §5（DIRECT 轴默认 / xwf setUserId("0") 条件采用 / 被拒路径显式排除 + 用例设计规则 4 条）。

Exit Criteria:

- [x] 设计文档含机制选型论证完整节：三选项对比 + 选定方案 + 四风险判定方法（可执行性判据：M0.2 可直接逐项实证）
- [x] 用例粒度约定 + 耗时模型 + xwf 可执行审批流清单落盘

### Phase 2 — 用例全量设计 + 覆盖矩阵（混合类型：2/3 Add + 1/3 Proof，低于 80% 统一类型阈值，逐项标注）

Status: completed
Targets: `docs/design/integration-testing.md` §用例规格 + §覆盖矩阵
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add：15-25 用例全量设计——每用例规格含六要素（业务目标对应核心闭环 / 前置 seed 或自包含数据 / 关键路径 GraphQL 动作序列 / 三层断言 = response 快照模式 + DB 状态快照 + JUnit 关键断言：成功失败/状态翻转/金额余额正确性 / 主导域批次归属 / 涉及域清单）；复杂度判据每用例满足（跨 3+ 域 + 审批/过账/状态机关键路径至少其一）；主导域分组呼应 M0.3 分批（8-12 批，每批 2-3 用例，优先 18-22 用例校准）。
  - Skill: nop-testing
  - 落地：设计文档 §6（22 用例 C01-C22 全量规格，六要素完整 + 复杂度判据逐用例标注）+ §8（10 批 B1-B10）。
- [x] Add：用例覆盖矩阵验收表——全 19 子系统每域出现次数（目标每域 ≥2 次），标注每域至少 1 次的全覆盖核对。
  - Skill: nop-testing
  - 落地：设计文档 §7（19/19 域 ≥2 次；显式算术核验 Σ矩阵 = Σ用例涉及域数 = 88）。
- [x] Proof：审批流用例以当前实现为准——只选用 Phase 1 登记的已验证可执行审批流，在用例规格中标注 xwf 规避依据。
  - Skill: none
  - 落地：设计文档 §5 规则 + §6 涉 xwf 用例（C01/C03/C17）显式标注 `[xwf 规避依据: ...]`。

Exit Criteria:

- [x] 15-25 用例全量规格落盘，每用例六要素完整、复杂度判据满足
- [x] 覆盖矩阵全 19 域每域 ≥2 次（验收表可核验）；每域至少 1 次全覆盖
- [x] 用例涉及域/主导域与覆盖矩阵一致（计数核对：Σ矩阵 88 = Σ用例涉及域数 88，逐域独立复核通过）

### Phase 3 — 独立审查收敛与定稿（混合类型：1/3 Decision + 1/3 Fix + 1/3 Proof，逐项标注）

Status: completed
Targets: `docs/design/integration-testing.md`、`docs/backlog/integration-test-roadmap.md`（M0.1 → done）、`docs/logs/2026/08-23.md`
Skill: none

- Item Types: `Decision | Proof | Fix`
- Prereqs: Phase 2

- [x] Decision：独立子代理审查 ≥2 轮收敛（fresh 会话，逐轮修订直至无 BLOCKER；审查记录持久化在设计文档或本计划 Draft Review Record）。
  - Skill: none
  - 落地：设计文档 §10（iteration 1 `ses_fd1c0b042ffeUFNkSff8zPMUuu`：0 BLOCKER/1 MAJOR/5 MINOR 全部修订；iteration 2 `ses_fd1bc538dffeZ0aKucwxkpG65P`：accept 0 BLOCKER/0 MAJOR/7 MINOR 全部修订）。
- [x] Fix：审查发现全部修订落地（含覆盖矩阵/用例规格/机制选型表述）。
  - Skill: none
  - 落地：设计文档 MAJOR-1（覆盖矩阵 inventory 行 14→15 + 显式算术核验 88=88）+ MINOR 12 项（xwf 规避语义定一/preCheck 来源/耗时模型重算/审查记录内联/DIRECT 轴计数/3 处动作名 postNcr·releaseLine·AssetCapitalization/编号说明）全部修订。
- [x] Proof：roadmap M0.1 状态 `todo` → `done`；`docs/logs/2026/08-23.md` 日志条目（按日志书写指南）。
  - Skill: none
  - 落地：`docs/backlog/integration-test-roadmap.md` M0.1 行 → `done（2026-08-23：plan ... 三 Phase 完成 ... 独立结束审计证据见该计划 Closure 段）`；`docs/logs/2026/08-23.md` 顶部新增 M0.1 聚合条目。

Exit Criteria:

- [x] ≥2 轮独立审查收敛记录在案（无未决 BLOCKER/MAJOR）
- [x] 设计文档定稿且与 roadmap M0.1 规格逐项对应（§9 逐项对应表 11 数据行全对应）
- [x] roadmap M0.1 = done + 日志条目存在

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fd1cb296fffeTSMWWXfFW1xXLo，独立 general 子代理新会话) — 0 BLOCKER / 3 MAJOR / 2 MINOR。基线事实全部实测准确零漂移（12 测试类 / 94 seed / forkCount=4 / JunitAutoTestCase :29/:85 / 12 EndToEnd / 3808-0-0-1 / xwf 实证）。MAJOR-1 Phase 1 阶段级类型声明 3/4 Decision 与实际 2/4 不符（规则 7 需 ≥80%）；MAJOR-2 Phase 2 声明 3/3 Add 与实际 2/3 不符；MAJOR-3 日志路径 `docs/logs/2026/2026-08-23.md` 错误，正确为 `docs/logs/2026/08-23.md`（00-log-writing-guide.md:25，实仓该文件已存在须追加）。MINOR-1 Phase 3「Decision-heavy」标签不实；MINOR-2 nop-testing skill 未登记于 docs/skills/README.md（系统性模式，信息性）。修订落地：三阶段标签全部改为精确计数 + 「低于 80% 统一类型阈值，逐项标注」；日志路径两处改 `08-23.md`。
- Independent draft review iteration 2: accept (ses_fd1c7c11dffeBDvQxjdUg25nFp，独立 general 子代理新会话) — 0 BLOCKER / 0 MAJOR / 1 MINOR。四项迭代 1 发现全部核验 FIXED（类型计数精确、日志路径命令验证存在/不存在两侧命中）；全面复扫无新 MAJOR；roadmap M0.1 规格逐项对应（15-25 六要素/复杂度判据/覆盖矩阵/机制 a/b/c 四风险/≥2 轮审查/分批呼应）；基线抽查 8 项全过。唯一 MINOR：Draft Review Record 当时仍为占位符（本记录填写即兑现）。**共识达成，计划可执行。**

## Closure Gates

> 本计划为 docs-only（零生产代码/契约/模型/测试代码变更），按计划指南模板规范删除 typecheck/build/lint/test 验证命令门控，以 Phase 3 的覆盖矩阵核对 + 审查收敛记录替代。

- [x] 范围内行为完成（设计文档交付完整：15-25 用例 + 覆盖矩阵 + 机制选型论证）
- [x] 相关文档对齐（设计文档 ↔ roadmap M0.1 规格 ↔ e2e-runbook ↔ seed-data 无矛盾）
- [x] 已运行验证（docs-only：用例六要素 grep 检查 + 覆盖矩阵计数核对 + 机制选型四风险判定方法存在性检查——结束审计者独立复跑通过：Σ矩阵 88 = Σ用例涉及域数 88、19/19 域 ≥2、六要素存在性、四风险判定方法存在性）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（计划 Draft Review Record 2 轮 + 设计文档 §10 2 轮）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行（ses_fd1b44ad6ffe5Olyyjv1bgCks1，fresh session 无执行者上下文）；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（见下方 Closure 段）

## Deferred But Adjudicated

- （定稿后按需填写；候选：覆盖未实现功能的用例——roadmap 已裁决 out-of-scope，本计划默认不设计）

## Closure

Status Note: 全 3 Phase 执行完成（docs-only，零生产代码/契约/模型/测试代码变更，git diff 仅 .md 文件）。产出 `docs/design/integration-testing.md`：22 用例六要素全量设计 + 覆盖矩阵 19/19 域 ≥2 次（Σ=88 显式算术核验）+ 机制选型 (c) 选定 + 四风险判定方法 + 粒度约定/耗时模型 + xwf 规避清单 + M0.3 分批建议。设计文档经两轮独立子代理审查收敛（0 BLOCKER 全程，末轮 0 MAJOR）。roadmap M0.1 `todo` → `done`；`docs/logs/2026/08-23.md` 顶部 M0.1 聚合条目。结束审计由独立子代理（新会话）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（fresh session，无执行者上下文），task id `ses_fd1b44ad6ffe5Olyyjv1bgCks1`
- Evidence: 逐维度实测复核——A 文本一致性 PASS（三阶段 Status completed + 9 执行项/9 Exit Criteria 全 [x] + 日志互证）；B 产出物真实性 PASS（设计文档 591 行实仓核对：22 用例六要素/覆盖矩阵算术独立复算 88=88/机制选型三选项+四风险判定/粒度+耗时/xwf 清单/§9 逐项对应/§10 两轮审查含 session id/基线事实 12 项零漂移）；C roadmap M0.1 = done + 日志条目 PASS；D Closure Gates 逐项 PASS（验证门控独立复跑：覆盖矩阵算术 + 六要素存在性 + 四风险判定方法存在性）；E Closure 段由执行者按审计结论填写。结论 **passes closure audit**，0 BLOCKER / 0 MAJOR / 3 MINOR——MINOR-1（§9 行数口径）与 MINOR-2（roadmap 头部注记）已修订兑现；MINOR-3（Closure 段填写）即本段。

Follow-up:

- 无（范围内零缺陷；roadmap M0.2 为既定 successor，非本计划范围项）