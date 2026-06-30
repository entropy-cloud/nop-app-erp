# 2026-06-30-2328-2 Phase 4 CRUD 冒烟测试：18 域标准 CRUD 行为验证

> Plan Status: active
> Last Reviewed: 2026-06-30
> Source: `docs/backlog/crud-roadmap.md` Phase 4（CRUD 冒烟测试）
> Related: `docs/plans/2026-06-30-2328-1-phase3-new-domains-app-aggregation.md`（本计划 Phase 3 依赖其完成）、`docs/architecture/testing-strategy.md`、`docs/plans/01-product-grade-erp-model-overhaul.md`（Deferred「端到端业务循环验证」的前置切片）
> Audit: required

## Current Baseline

**项目阶段**（实时仓库核实，2026-06-30）：post-codegen。18 域 codegen 骨架完整（271 实体），各域 BizModel 为标准 `CrudBizModel<T>` 空壳（标准 CRUD 由平台 `CrudBizModel` 提供，无需手写即可跑通 create/read/update/delete）。

**测试现状（实时核实）**：
- 全仓仅 **36 个测试类**，全部是 codegen 验证类（`Erp<X>CodeGen.java` / `Erp<X>WebCodeGen.java`，位于 `*-codegen` / `*-web` 模块）。这些是代码生成/页面生成的回归保护，**不是 CRUD 行为测试**。
- **零 `JunitAutoTestCase` 使用**（`grep -rln JunitAutoTestCase module-*` 无命中）。
- `app-erp-all/src/test` 无任何测试。
- 即：**CRUD 冒烟测试覆盖为 0**。Phase 4 是 greenfield。

**测试框架基线**（`docs/architecture/testing-strategy.md` 已定义，本计划遵循）：
- L2 集成测试：`JunitAutoTestCase`（快照录制/回放）+ `IGraphQLEngine`（GraphQL 查询引擎）。
- 输入：`request.json5`（支持 `@var` 变量）。
- 模式：RECORDING（录制快照→人工审查）→ CHECKING（比对快照，不一致即失败）。
- 数据：H2 内存库（各 `-app` 模块已含 `quarkus-jdbc-h2` 依赖）；测试间事务隔离、自动回滚。
- `testing-strategy.md` 提及的种子数据模块 `app-erp-seed` **尚不存在**；本计划冒烟测试采用自包含设计（每个用例先 mutation 建实体再验证），不依赖 seed。

**roadmap Phase 4 定义（本计划范围）**：5 类操作 × 18 域，`mvn test -pl module-{xx} -am`，按 Phase 1→2→3 顺序推进。

| 测试 | 覆盖 | 方法 | 通过标准 |
|------|------|------|---------|
| 新建实体 | 每域 1 主实体 + 1 头-行对 | GraphQL mutation | 返回成功，非空 ID |
| 查询/筛选 | 列表加载 + 搜索条件 | GraphQL query | 返回不报错 |
| 编辑保存 | 修改 1-2 关键字段 | mutation → query 验证 | 修改值与输入一致 |
| 逻辑删除 | 验证 delVersion=1 | mutation → query | delVersion=1 |
| 关系导航 | 主子表级联 | 新建头→添加行→查询行 | 外键正确引用 |

**剩余差距**：上述 5 类 × 18 域测试用例从零编写，建立快照基线，并使 `mvn test`（按域）全绿。

## Goals

- **18 域 CRUD 冒烟测试覆盖**：每域至少 1 主实体 + 1 头-行对，覆盖 roadmap Phase 4 的 5 类操作（新建/查询筛选/编辑保存/逻辑删除/关系导航）。
- **测试可独立运行**：每域 `mvn test -pl module-{domain} -am` 全绿（H2 内存库，自包含数据，无外部依赖）。
- **快照基线建立并经人工审查**：RECORDING 录制的快照经审查确认为正确期望输出，CHECKING 模式可重复通过。
- **roadmap Phase 4 状态收尾**：Phase 4 表标注完成，CRUD 标准操作验证有可重复证据。

## Non-Goals

- **不测试业务规则/状态机/业财过账**——本计划只验证**标准 CRUD**（`CrudBizModel` 提供的通用操作）。业务逻辑深化测试属 `implementation-roadmap.md`（BizModel 方法/状态机/ErrorCode 测试，见 testing-strategy 覆盖要求表）。
- **不做端到端跨域业务循环**（如采购→入库→凭证）——属 `project-context.md`「跨域端到端循环需先编写计划」的独立 plan-first 工作，规模远超冒烟测试。
- **不创建 `app-erp-seed` 模块**——冒烟测试自包含；seed 数据架构属独立架构决策。
- **不修改任何 `model/*.orm.xml`、BizModel、xbiz**（保护区域）。
- **不要求 18 域实体逐一测试**——按 roadmap「每域 1 主实体 + 1 头-行对」抽样，非全实体覆盖（全实体覆盖非冒烟测试目的）。
- **不在 app-erp-all 聚合层跑全量测试**——按 roadmap「`mvn test -pl module-{xx} -am`」按域运行。

## Task Route

- Type: `verification or audit work`（为已生成 CRUD 建立可重复行为验证）+ `implementation-only change`（新增测试代码，不改产品契约）。
- Owner Docs: `docs/backlog/crud-roadmap.md`（Phase 4）、`docs/architecture/testing-strategy.md`、平台 `../nop-entropy/docs-for-ai/03-runbooks/`（测试 runbook）、`../nop-entropy/docs-for-ai/02-core-guides/`（CrudBizModel 安全 API）。
- Skill Selection Basis: `Skill: none`。编写 Nop 测试用例遵循平台测试 runbook；`docs/skills/README.md` 现有技能均为审计方法，无测试编写技能匹配。独立草案/结束审计用 `plan-audit-prompt.md` / `closure-audit-prompt.md`。

## Infrastructure And Config Prereqs

- H2 内存库（各 `-app` 模块已含 `quarkus-jdbc-h2`；`-service` 测试通过 `@NopTestConfig(localDb=true)` 强制 H2）。无外部数据库/端口/密钥需求。
- **不硬依赖** Plan 1（`2026-06-30-2328-1`）：`mvn test -pl module-<domain> -am` 仅构建该域自身 module 链，`app-erp-all` 是下游消费者、非测试上游依赖。唯一真实前置：各域 module 链可干净构建（新 8 域受 Plan 1 标记的脏工作树 regen 产物风险影响，构建前须确认）。原 10 域 Phase 2 可独立先行。
- 平台 `JunitAutoTestCase` / `IGraphQLEngine` 来自 `nop-entropy` 测试依赖（已在父 POM 测试 scope）。

## Execution Plan

### Phase 1 - 测试脚手架 + 首域样板（master-data）

Status: planned
Targets: `module-master-data/erp-md-service/src/test/`（样板）、`docs/architecture/testing-strategy.md`（补 Nop 测试 runbook 落地引用）
Skill: none

- Item Types: `Add | Decision | Proof`
- Prereqs: 无（原 10 域已可独立构建）

- [ ] `Decision`：测试落位约定——每域 CRUD 冒烟测试置于 `module-<domain>/erp-<short>-service/src/test/java/app/erp/<short>/service/`，测试类继承 `JunitAutoTestCase`，输入用 `request.json5`，快照置于测试类相对的 `_cases/`（平台 `nop-entropy/docs-for-ai/02-core-guides/testing.md` §测试数据位置 约定）。理由：service 层是 CRUD 行为归属层。备选（被否）：置于 `-app` 模块——会引入 Quarkus 完整启动开销，冒烟测试不需要。
  - Skill: none
- [ ] `Decision`：schema bootstrap 机制（关键，平台测试 #1/#5 陷阱）——实时核实：`-service` 模块**无 `application.yaml`**，而 `init-database-schema: true` 仅存在于 `-app/src/main/resources/application.yaml`（`-app` 依赖 `-service`，反向不传递），故 `-service` 测试类路径得不到 schema 初始化，首次 RECORDING 会因空库无 schema 失败。裁决：用类级 `@NopTestConfig` 显式控制——**首次录制**（空 H2）用 `@NopTestConfig(localDb = true, initDatabaseSchema = OptionalBoolean.TRUE, snapshotTest = SnapshotTest.RECORDING)`；快照经人工审查后切**日常校验**为裸 `@NopTestConfig`（CHECKING 模式自动从 `_cases/` 加载快照 CSV 恢复 H2 内存库，无需 schema-init）。备选（被否）：在 `-service/src/test/resources/` 放测试 scope `application.yaml` 镜像 `nop.orm.init-database-schema: true`——可行但每域多一文件且与注解方式重复。
  - Skill: none
- [ ] `Decision`：实体抽样规则——每域选 **1 主实体**（独立、字段完整）+ **1 头-行对**（有主子关系的头实体及其行实体，用于关系导航）。主实体须含可编辑文本/数值字段（编辑保存用）与 delVersion 字段（逻辑删除用）。头-行对须有外键关系。从各域 `model/*.orm.xml` 选取并在测试类 javadoc 标注所选实体与选取理由。
  - Skill: none
- [ ] `Add`：master-data 样板测试套件——实现 roadmap Phase 4 全部 5 类操作（新建/查询筛选/编辑保存/逻辑删除/关系导航），覆盖 1 主实体（如 `ErpMdMaterial`）+ 1 头-行对。RECORDING 录制快照 → 人工审查快照正确性 → 切换 CHECKING 验证通过。**注意**：RECORDING 模式每方法执行后框架抛 `nop.err.autotest.snapshot-finished`，Maven 显示 `Tests run: X, Errors: X` 为**预期行为非失败**；切 CHECKING 后 Errors 归零。
  - Skill: none
- [ ] `Proof`：`mvn test -pl module-master-data -am` 全绿，快照 CHECKING 通过。证明脚手架模式可行，解除其余 17 域并行阻塞。
  - Skill: none

Exit Criteria:

> 本阶段交付可复制的测试模式 + 首域通过证据。完整仓库 `mvn test` 归 Closure Gates。

- [ ] master-data 5 类操作测试用例存在且 `mvn test -pl module-master-data -am` 全绿（CHECKING 模式，Errors 归零）
- [ ] 测试落位约定（`_cases/`）、schema bootstrap 机制、实体抽样规则均以 `Decision` 记录，可在其余 17 域复制
- [ ] 快照基线经人工审查（RECORDING→CHECKING 切换确认）

### Phase 2 - 原 10 域（roadmap Phase 1+2）冒烟测试

Status: planned
Targets: `module-{purchase,sales,inventory,finance,assets,projects,manufacturing,quality,maintenance}/erp-*-service/src/test/`（9 域；master-data 已在 Phase 1）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1（复用脚手架与抽样规则）

- [ ] `Add`：按 Phase 1 抽样规则，为 9 个原域各编写 5 类 CRUD 冒烟测试（1 主实体 + 1 头-行对/域）。
  - Skill: none
- [ ] `Proof`：逐域 `mvn test -pl module-<domain> -am` 全绿（10 域，含 master-data），快照 CHECKING 通过。
  - Skill: none

Exit Criteria:

> 本阶段覆盖原 10 域（roadmap Phase 1+2）标准 CRUD 行为。

- [ ] 9 域测试用例存在（每域 5 类操作 + 1 主实体 + 1 头-行对）
- [ ] 原 10 域 `mvn test -pl module-<domain> -am` 逐域全绿

### Phase 3 - 新 8 域（roadmap Phase 3）冒烟测试

Status: planned
Targets: `module-{crm,cs,hr,aps,contract,drp,logistics,b2b}/erp-*-service/src/test/`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1（模式）；**软排序**：在新 8 域 module 链可独立构建后即可开测（见下方说明，**不硬依赖** Plan 1）

- [ ] `Add`：按 Phase 1 抽样规则，为 8 个新域各编写 5 类 CRUD 冒烟测试（1 主实体 + 1 头-行对/域）。注意 cs/hr/ct/log 短名与目录一致。
  - Skill: none
- [ ] `Proof`：逐域 `mvn test -pl module-<domain> -am` 全绿，快照 CHECKING 通过。
  - Skill: none

Exit Criteria:

> 本阶段覆盖新 8 域（roadmap Phase 3）标准 CRUD 行为，至此 18 域全覆盖。
>
> **依赖说明**：`mvn test -pl module-<domain> -am` 的 `-am` 仅构建该域自身依赖链（codegen→dao→meta→service→web→app），`app-erp-all` 是下游消费者、非上游依赖，故新 8 域冒烟测试**不硬依赖** Plan 1（`2026-06-30-2328-1`）的 app-erp-all 聚合 wiring。仅按 roadmap「Phase 3→4 顺序」与执行便利做软排序；若 Plan 1 未完成但 8 域 module 链可构建，本阶段可先行。新 8 域各自 module 链能否干净构建是唯一真实前置（含 Plan 1 标记的脏工作树 regen 产物风险）。

- [ ] 8 域测试用例存在（每域 5 类操作 + 1 主实体 + 1 头-行对）
- [ ] 新 8 域 `mvn test -pl module-<domain> -am` 逐域全绿

### Phase 4 - roadmap Phase 4 收尾 + 全量验证

Status: planned
Targets: `docs/backlog/crud-roadmap.md`、`docs/architecture/testing-strategy.md`
Skill: none

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1-3

- [ ] `Add`：`crud-roadmap.md` Phase 4 表补「完成」标记（每域 5 类操作通过证据），Phase 4 整体状态标注完成。
  - Skill: none
- [ ] `Add`：`testing-strategy.md` 新增「Nop 测试 runbook 落地」小节——记录本计划确立的落位约定（`-service` + `_cases/`）、schema bootstrap（`@NopTestConfig` + `initDatabaseSchema`）、抽样规则、RECORDING/CHECKING 实践，供后续 BizModel 业务测试复用。
  - Skill: none
- [ ] `Fix`：`testing-strategy.md:60` owner-doc 漂移——原文「测试数据使用种子数据模块（`app-erp-seed`）」所述模块**不存在**；本计划采用自包含设计（先建后验，CHECKING 从 `_cases/` 恢复），故修正该句为指向本计划的自包含实践（`app-erp-seed` 降级为可选 follow-up，见 Deferred）。
  - Skill: none
- [ ] `Proof`：全量 `mvn test`（根目录，覆盖 18 域 service 测试）汇总通过率，记录于日志；与逐域运行结果交叉核对。
  - Skill: none

Exit Criteria:

> 本阶段使 roadmap Phase 4 闭合，测试实践沉淀为可复用文档，并顺手纠正 testing-strategy 的 seed 漂移。

- [ ] roadmap Phase 4 标注完成，含 18 域通过证据
- [ ] testing-strategy.md 含本计划沉淀的测试实践小节，且 `app-erp-seed` 漂移已修正
- [ ] 18 域 CRUD 冒烟测试全绿（根 `mvn test` + 逐域，日志记录）

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0e6d6c689ffecdKqpK5Me3Vjn，独立 general 子代理，研究型）— 基线主张全部实时核实属实（36 测试类全为 codegen 类、零 JunitAutoTestCase、-app 含 H2、18 域-service 存在）。1 项阻塞 B1：schema 初始化隐藏阻塞——`-service` 无 `application.yaml`、`init-database-schema` 仅在 `-app`、反向不传递，故 `-service` 测试首次 RECORDING 会因空库无 schema 失败（平台测试 #1/#5 陷阱），且 Decision #1 原理由「已有 IGraphQLEngine 路径」不成立。另指出「Phase 3 硬依赖 Plan 1」技术上不准确（`-am` 不含 app-erp-all）。迭代 1 已修订：新增 schema bootstrap Decision（`@NopTestConfig(localDb=true, initDatabaseSchema=OptionalBoolean.TRUE, snapshotTest=RECORDING)` 首录→裸 `@NopTestConfig` 日常校验）、纠正 Decision #1 理由与快照目录 `_cases/`、软排序化 Plan 1 依赖（附准确依赖说明）、补 RECORDING `snapshot-finished` Errors 为预期行为的注记、Phase 4 项类型 Fix→Add、新增 testing-strategy:60 `app-erp-seed` 漂移修正项、Closure 增根 `mvn test`。
- Independent draft review iteration 2: 待执行（修订后复审）

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划为测试代码，结束时运行全量 `mvn test`。

- [ ] 范围内行为完成：18 域 CRUD 冒烟测试（5 类操作 × 每域 1 主实体 + 1 头-行对）全部存在且通过
- [ ] 相关文档对齐：roadmap Phase 4 完成、testing-strategy 沉淀测试实践
- [ ] 已运行验证：18 域 `mvn test -pl module-<domain> -am` 逐域全绿（CHECKING 模式）；**根目录 `mvn test`** 全量覆盖 18 域 service 测试通过；快照基线经人工审查
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、`docs/logs/` 一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为人工门控占位符
- [ ] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### BizModel 业务逻辑/状态机/业财过账测试

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划只验证标准 CRUD（平台 `CrudBizModel` 通用操作）；业务规则测试属 BizModel 深化范畴（testing-strategy 覆盖要求表：BizModel 方法/状态机迁移/ErrorCode 各至少 1 测试），归 `implementation-roadmap.md`。
- Successor Required: yes（触发条件：按 implementation-roadmap 深化各域 BizModel 时）

### 跨域端到端业务循环验证

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 规模远超冒烟测试；`project-context.md` 明确「跨域端到端循环需先编写计划」。本计划只覆盖单域内主子表级联（关系导航）。
- Successor Required: yes（触发条件：独立 plan-first，如采购→入库→应付→凭证）

### 全实体覆盖（非抽样）

- Classification: `optimization candidate`
- Why Not Blocking Closure: roadmap 明确「每域 1 主实体 + 1 头-行对」抽样；全 271 实体覆盖非冒烟测试目的，性价比低。
- Successor Required: yes（触发条件：特定域出现高频 CRUD 缺陷时，对该域扩展为全实体覆盖）

### app-erp-seed 种子数据模块

- Classification: `optimization candidate`
- Why Not Blocking Closure: 冒烟测试自包含（先建后验）；seed 架构属独立决策，`testing-strategy.md` 引用但未落地。
- Successor Required: yes（触发条件：业务测试需要共享主数据夹具时）

## Closure

Status Note: 待执行。完成后此处记录：18 域 CRUD 冒烟测试全绿、roadmap Phase 4 闭合、测试实践沉淀到 testing-strategy.md。

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计子代理填写
- Evidence: 待填

Follow-up:

- BizModel 业务逻辑/状态机/业财过账测试（见 implementation-roadmap）
- 跨域端到端业务循环验证（独立 plan-first）
