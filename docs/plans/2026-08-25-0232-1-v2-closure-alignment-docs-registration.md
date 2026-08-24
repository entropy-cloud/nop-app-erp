# 2026-08-25-0232-1-v2-closure-alignment-docs-registration V.2 收尾对齐：快照重录义务双面登记 + runbook 基线刷新 + README/roadmap 收口

> Plan Status: completed（2026-08-25 三 Phase 全部完成 + 独立结束审计 **PASS**（0 BLOCKER/0 MAJOR/0 MINOR，见 Closure Audit Evidence）；前置：独立草案审查 1 轮 accept，见 Draft Review Record，两枚 MINOR 就地修正后转 active）
> Mission: integration-test
> Work Item: V.2
> Last Reviewed: 2026-08-25
> Source: `docs/backlog/integration-test-roadmap.md` MV V.2（Deps = V.1 done，2026-08-25 V.1 闭包后解锁）
> Related: V.1（`docs/plans/2026-08-24-1943-1-v1-full-regression-green-baseline.md`，其 Follow-up 显式指向本计划）；M0.x + B1-B10 各批计划（继承自 V.1 `Related:`）
> Audit: required

## Current Baseline

- **roadmap 状态**：M0.1/M0.2/M0.3 + B1-B10 + V.1 全部 done（2026-08-25 V.1 闭包）；**V.2 = 唯一剩余 todo**，Deps（V.1）已满足。V.1 计划 Follow-up：「V.2 收尾对齐计划另行起草（下一 mission driver 轮次）」——即本计划。
- **V.1 全绿基线**（`docs/testing/known-good-baselines.md` 2026-08-25 V.1 行，权威计数源）：`mvn test -pl app-erp-all` = **54/0/0/1（套件墙钟 104s，23 集成类）**；`mvn clean install -DskipTests` = **156 模块 BUILD SUCCESS（01:47）**；全 reactor `mvn test` = **3834/0/0/1/642（13:58）**；Known Failures = none + 1 skipped（`ErpAllWebPagesCollectTest` `@Disabled` 预存 JDK26/ANTLR H-2）；证据 `_tmp/v1-surefire-evidence/`（642 XML）。
- **V.2 义务面 1（seed-data.md）**：`docs/architecture/seed-data.md`（436 行，逐域 seed 注册结构）**零快照重录义务内容**（grep「快照/重录/_cases」无相关命中）——双面登记缺失。
- **V.2 义务面 2（e2e-runbook.md）**：「集成测试」段（L1002-1060）M0.2 落地，四小节（运行方式/fresh-DB 纪律/与 E2E live server 互斥纪律/基线）+ delVersion 语义段在位。**陈旧点**：①「基线」小节仍为试点期口径（「试点 `TestErpP2pPilot` …… 全量 app-erp-all 29/0/0/1」，M0.2 时点，现为 54/0/0/1）；②「运行方式」全量命令注释「含既有 12 基建类，29/0/0/1 基线」陈旧；③ 单用例示例仅试点范本，未反映 22 用例类。fresh-DB 纪律/互斥纪律/delVersion 段无陈旧引用（已含 seed 变更联动与互斥纪律）。
- **V.2 义务面 3（backlog README）**：`docs/backlog/README.md` P8 行（L69）在位（roadmap v2 修订期登记：优先级/工作项/路线图/状态/自主权五要素齐备），状态 `todo`——roadmap 全部工作项 done 后须翻 `✅ done`（先例：P0 行 `id-string-migration-roadmap.md` → `✅ done`）。
- **可选采纳项（index.md 核查）**：roadmap Draft Review Record 覆盖面审查 MINOR「index.md 核查（可选采纳为 V.2 核查）」。实仓 grep：`docs/index.md` **零** integration-testing/integration-test/集成测试 路由——`docs/design/integration-testing.md`（设计真相源）与 e2e-runbook「集成测试」节未被顶级路由器发现，核查揭示真实缺口。
- **快照资产双面位置**（登记内容的事实基础）：面 1 = 既有各域测试快照，各域 `<module>-*/erp-*-service/_cases/`（录制回放范式，391 测试类先例）；面 2 = 新集成用例快照，`app-erp-all/_cases/io/nop/app/all/it/`（23 类目录：22 用例 + 试点 `TestErpP2pPilot`）。
- **部署期 seed 资产**（重录义务触发源）：`app-erp-all/src/main/resources/_vfs/_init-data/` **94 CSV + 1 SQL**（`zz-sequence-advance.sql`），`DataInitInitializer` 拓扑序加载。
- **Deferred 项核查（本轮不可重触发）**：CI 自动接线（触发条件 = CI 已有 fresh-DB seed 装载机制，未满足）、覆盖未实现功能用例（未触发）、mission-driver monitor 解析限制（外部工具链）、V.1 E2E 全量重跑（watch-only，构造性满足裁定后未触发）——均维持原裁定，不并入本计划。

## Goals

- 完成 roadmap V.2 定义的四项登记义务：① seed-data.md 快照重录义务**双面**登记；② e2e-runbook「集成测试」段基线刷新（运行方式/基线小节对齐 V.1 口径）；③ backlog README 本路线图行收口；④ 日志更新。
- 完成已采纳的 index.md 核查（roadmap 覆盖面审查 MINOR 采纳项；补集成测试文档路由）。
- roadmap V.2 → done + README P8 → `✅ done`，mission 全工作项（M0.x + B1-B10 + V.1 + V.2）收口。

## Non-Goals

- CI 自动接线（roadmap Deferred：out-of-scope improvement，触发条件未满足）。
- 任何生产代码 / 测试代码 / `_cases` 快照 / seed CSV / ORM 变更（V.1 基线口径冻结）。
- E2E 套件重跑（V.1 构造性满足裁定维持，无新生产/seed 变更）。
- 集成测试用例增删或设计文档（`docs/design/integration-testing.md`）内容变更（B1-B10 勘误已闭包，§4 耗时注记 V.1 已落盘）。
- roadmap Deferred 既有三项的再裁决（维持原裁定与触发条件）。

## Task Route

- Type: `implementation-only change`（纯文档登记与状态翻转，零代码）
- Owner Docs: `docs/backlog/integration-test-roadmap.md`（MV V.2 行 + 横切关注点 1/3 + 规则 2）、`docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`「集成测试」节、`docs/backlog/README.md`、`docs/index.md`、`docs/testing/known-good-baselines.md`（只读引用，不内联复制计数）
- Skill Selection Basis: `none`——纯文档登记与状态收口，无测试编写/后端/前端工作面；roadmap V.2 行 Skill 列 = `none`（roadmap 自身指定），`nop-testing` 覆盖测试编写不匹配本任务。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯文档计划，无构建/运行依赖）。

## Execution Plan

### Phase 1 - seed-data.md 双面快照重录义务登记

Status: completed
Targets: `docs/architecture/seed-data.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: 无

- [x] Add: seed-data.md 新增「快照重录义务（seed 变更联动，强制）」节（置于文档级章节位，如「导入策略」之后或文档尾部，遵循现有逐节结构）：
      - **双面资产盘点**：面 1 = 既有各域测试快照（各域 `erp-*-service/_cases/`，录制回放）；面 2 = app-erp-all 集成用例快照（`app-erp-all/_cases/io/nop/app/all/it/`，22 用例 + 试点共 23 类）；
      - **义务规则**：任何部署期 seed 资产（`app-erp-all/src/main/resources/_vfs/_init-data/` 94 CSV + `zz-sequence-advance.sql`）变更 → 变更方**同步重录受影响快照（双面）**，并在变更提交说明中登记重录范围（roadmap V.2 行文「含变更 PR 说明」）；
      - **交叉引用**：roadmap 横切关注点 1（快照重录义务）/3（seed 修正授权 + 既有 E2E 数值断言期望值联动评估）、e2e-runbook「集成测试」节 fresh-DB 纪律（双向互指）。

Exit Criteria:

- [x] seed-data.md 含双面快照重录义务节：两面资产路径与实仓一致（面 2 路径 `app-erp-all/_cases/io/nop/app/all/it/` 经 grep 复核）、触发条件、提交说明义务、交叉引用齐备，与 roadmap V.2 行文及横切关注点 1/3 口径一致。

### Phase 2 - e2e-runbook 集成测试段基线刷新 + index.md 路由补全

Status: completed
Targets: `docs/testing/e2e-runbook.md`「集成测试」节、`docs/index.md`
Skill: `none`

- Item Types: `Fix | Add`（Fix = e2e-runbook 集成测试节陈旧计数刷新；Add = index.md 路由补全）
- Prereqs: 无（与 Phase 1 无顺序依赖，可并行）

- [x] Add: 「基线」小节刷新为 V.1 口径：22 用例 + 试点共 **23 集成类**三层全比对全绿；`mvn test -pl app-erp-all` = **54/0/0/1（套件墙钟 104s）**；权威计数源 = `docs/testing/known-good-baselines.md` 2026-08-25 V.1 行（全 reactor 3834/0/0/1/642 + 156 模块，不内联复制为第二真相源）；唯一 skipped 预存口径（`ErpAllWebPagesCollectTest` `@Disabled`）保持。
- [x] Add: 「运行方式」小节注释刷新：全量命令计数注释 `29/0/0/1` → `54/0/0/1`；单用例示例在试点范本外补用例类范本（如 `-Dtest=TestErpC01P2pGoldenPath`）；核对「fresh-DB 纪律」「与 E2E live server 互斥纪律」「delVersion 列语义」三小节无陈旧计数引用（M0.2 落地内容以当前实现为准已核对，不预期变更——若发现陈旧点就地修正并记录）。
- [x] Add: `docs/index.md` 补集成测试路由（roadmap 覆盖面审查 MINOR 可选项采纳，实仓 grep 零命中为真实缺口）：在测试/验证相关路由区增补 `docs/design/integration-testing.md`（设计真相源：22 用例/覆盖矩阵/机制选型）与 e2e-runbook「集成测试」节（运行方式/基线/互斥纪律）两处发现路径，遵循 index.md 现有行格式。

Exit Criteria:

- [x] e2e-runbook「集成测试」段与 V.1 基线行计数一致（54/0/0/1 + 104s + 23 类），无试点期陈旧口径残留（grep `29/0/0/1` 于该节零命中）。
- [x] `docs/index.md` 含集成测试设计文档与 runbook 节的路由行（grep `integration-testing` 命中）。

执行期注记：三小节核对结论 = 零陈旧计数引用；fresh-DB 纪律小节就地补 seed-data.md「快照重录义务」节回指（完成 Phase 1 交叉引用的「双向互指」要求），其余内容不变。

### Phase 3 - roadmap/README 收口 + 日志

Status: completed
Targets: `docs/backlog/integration-test-roadmap.md`、`docs/backlog/README.md`、`docs/logs/2026/08-25.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1、Phase 2

- [x] Add: roadmap V.2 → `done`（Status 单元格引用本计划 + 登记摘要）+ 头部「最后更新」增 V.2/mission 收口注记（M0.x + B1-B10 + V.1 + V.2 全 done）。
- [x] Add: README P8 行状态 `todo` → `✅ done`，行内补完成口径（22 用例 19 域 ≥2 覆盖 + V.1 全绿基线行 + V.2 收尾登记，引用 roadmap 为动态状态真相源）。
- [x] Add: `docs/logs/2026/08-25.md` 增 V.2 条目（按日志指南倒序格式；纯文档变更，无验证命令状态可标 n/a——登记四义务 + index.md 路由 + mission 收口）。

Exit Criteria:

- [x] roadmap V.2 = done + 头部注记在盘；README P8 = `✅ done`；日志条目在盘。
- [x] 五处文本（roadmap / README / e2e-runbook / seed-data / 日志）计数与结论互相一致（54/0/0/1、23 类、双面路径口径无冲突）。

## Draft Review Record

- Independent draft review iteration 1: accept（ses_fcaf31b7effewwF3Vycc6XVeb3，2026-08-25）——全部事实断言经实仓独立核验通过（基线计数 ↔ known-good-baselines 2026-08-25 V.1 行逐项一致；seed-data 436 行零重录义务；runbook 集成测试节陈旧点 = L1012/L1047 两处 `29/0/0/1`；README P8 L69 todo；index.md 零路由；`app-erp-all/_cases/io/nop/app/all/it/` 23 目录；94 CSV + 1 SQL；19 域级 `_cases` 双面资产）；五审查维度（范围忠实/基线准确/格式合规/可执行/风险）全 PASS；0 BLOCKER / 0 MAJOR + 2 MINOR（① Phase 2 Item Types 补 `Fix | Add`——陈旧计数刷新属确认漂移修正；② Goals「可选采纳」措辞改「已采纳」防反松弛误读）+ 2 INFO（done 翻转时序 = V.1 先例一致性；Phase 1 位置 latitude 属实施自由度）。两枚 MINOR 已就地修正。**审查者结论：可接受的执行契约，转 active。**

## Closure Gates

> 纯文档计划（零代码/零快照/零 seed 变更）：删除构建/测试验证命令门控并说明原因——本计划不改任何可执行资产，V.1 基线口径冻结不变；验证 = 文本一致性核查 + 实仓 grep 复核（路径存在性、计数与 known-good-baselines V.1 行一致、陈旧口径清零）。

- [x] 范围内登记完成（V.2 四项义务 + index.md 核查采纳项全部落地）
- [x] 相关文档对齐（五处文本计数与结论一致）
- [x] 已运行验证：文本一致性核查 + grep 复核（seed-data 双面路径实仓存在；e2e-runbook 集成测试节 `29/0/0/1` 零残留；index.md `integration-testing` 命中；README P8 = ✅ done；roadmap V.2 = done）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无新增——roadmap 既有三项 Deferred：CI 自动接线 / 覆盖未实现功能用例 / mission-driver monitor 解析限制，触发条件均未满足，维持原裁定归 successor。）

## Closure

Status Note: 三 Phase 全部完成并勾选（2026-08-25）。V.2 四项登记义务（seed-data 双面义务 / runbook 基线刷新 / README 收口 / 日志）+ index.md 核查采纳项全部落地；roadmap V.2 = done + 头部 mission 收口注记（M0.x + B1-B10 + V.1 + V.2 全 done）；README P8 = ✅ done。纯文档计划：Closure Gates 以文本一致性核查 + grep 复核替代构建/测试门控（零代码/零快照/零 seed 变更，V.1 基线口径冻结，验证命令状态 n/a）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，与执行者无关，无执行者上下文，ses_fcaec104fffeM0fgHfUog1uJjw），2026-08-25。
- Evidence: 实仓独立核验全项通过——① Phase 1：`docs/architecture/seed-data.md:67-91`「快照重录义务（seed 变更联动，强制）」节在位（双面资产盘点/四条义务规则/交叉引用），实仓路径复核 19 域级 `_cases`、`app-erp-all/_cases/io/nop/app/all/it/` 23 类、`_vfs/_init-data/` 94 CSV + `zz-sequence-advance.sql`，与登记口径逐项一致；② Phase 2：e2e-runbook.md:1008-1052 集成测试节 = V.1 口径（基线小节 23 集成类 / 54/0/0/1 / 104s / 权威计数源 known-good-baselines 2026-08-25 V.1 行引用不内联），`rg '29/0/0/1' docs/testing/e2e-runbook.md` 全文件零命中，运行方式注释 54/0/0/1 + `TestErpC01P2pGoldenPath` 范本，fresh-DB 纪律（L1032-1033）与 seed-data.md 回指双向闭合；`docs/index.md:53-54` 集成测试双路由在位；③ Phase 3：roadmap:3 头部 V.2/mission 收口注记 + roadmap:58 V.2 行 done 引用本计划；README:69 P8 行 = ✅ done 含完成口径；日志 08-25.md:3-9 顶部 V.2 条目；④ 权威计数对照 known-good-baselines.md:13（54/0/0/1 + 104s + 3834/0/0/1/642 + 156 模块）与五处落盘文本零漂移；⑤ 范围检查：git 变更面 = 计划声明 6 文件 + 计划自身，diff 逐文件复核纯文档，零代码/零快照/零 seed 变更。问题清单 0 BLOCKER / 0 MAJOR / 0 MINOR。**审计结论：PASS。**

Follow-up:

- 无（Deferred 项均已有 successor 登记于 roadmap，不重复挂起）。
