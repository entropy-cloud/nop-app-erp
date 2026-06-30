# 2026-07-01-0215-1 收尾延迟 CRUD 冒烟测试：修复 manufacturing / customer-service 阻塞性模型缺陷，达成 18/18 域覆盖

> Plan Status: completed
> Last Reviewed: 2026-07-01
> Source: `docs/backlog/crud-roadmap.md` Phase 4（剩余 2 域阻塞）+ `docs/plans/2026-06-30-2328-2-phase4-crud-smoke-tests.md` Deferred（manufacturing / customer-service 模型缺陷阻塞）
> Related: `docs/plans/2026-06-30-2328-2-phase4-crud-smoke-tests.md`（其 Deferred 项的后继切片，触发条件已满足）、`docs/architecture/testing-strategy.md`（Nop 测试 runbook 落地小节，本计划复用）、`docs/design/finance/state-machine.md`（过账状态语义权威源）
> Audit: required

## Current Baseline

**CRUD 路线图状态**（实时核实 `docs/backlog/crud-roadmap.md`）：Phase 1-3 全 `done`（18 域 codegen/页面/菜单/action-auth）；Phase 4 CRUD 冒烟测试 `done`，但 **16/18 域通过**，manufacturing(mfg) 与 customer-service(cs) 因前置模型缺陷阻塞，测试类 `@Disabled` 或不存在。两域阻塞均在 `2026-06-30-2328-2` 计划 `Deferred But Adjudicated` 中显式记录，并命名了后继触发条件——本计划即该后继切片。

**缺陷 1 — manufacturing（字典缺失，实时核实）**：
- `module-manufacturing/model/app-erp-manufacturing.orm.xml:647` 在实体 `ErpMfgSubcontractOrder` 上声明列 `postedStatus`（过账状态，`stdSqlType="VARCHAR" precision="20" stdDataType="string" mandatory="true"`，`ext:dict="erp-md/posted-status"`）。
- 该字典 `erp-md/posted-status` **全仓不存在**（`find module-* -name posted-status.dict.yaml` 无命中；master-data 现有 16 个字典含 `active-status` 等但无 `posted-status`）。
- 经核对，全部 `ext:dict="erp-md/*"` 引用中，`posted-status` 是**唯一缺失**的字典；其余 15 个均存在。
- 影响：生成的 `_ErpMfgSubcontractOrder.xmeta:92` 引用 `dict="erp-md/posted-status"`，单域测试初始化加载 mfg xmeta 即抛 `nop.err.graphql.unknown-dict`，先于任何测试逻辑；该 xmeta 拖垮整个 mfg 域的 per-module 加载（即便测试抽样实体是 ErpMfgRouting 也无法绕过）。
- 现有测试类：`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgRoutingCrudSmoke.java` 已写好（抽样 `ErpMfgRouting`→`ErpMfgRoutingOperation` 头-行对，5 类操作），但整体 `@Disabled("blocked by pre-existing model defect: ... erp-md/posted-status")`。字典补齐后即可解除 `@Disabled`。

**缺陷 2 — customer-service（幽灵实体声明，实时核实）**：
- `module-cs/model/app-erp-cs.orm.xml:630-641` 声明了两个外部实体引用 `app.erp.pro.dao.entity.ErpProProject` / `app.erp.pro.dao.entity.ErpProTask`（`notGenCode="true"`，仅 `id` 列，与上方合法跨域引用 `ErpMdPartner`/`ErpMdOrganization`/`NopAuthUser` 同模式）。
- 这两个 "pro" 实体**全仓不存在对应 Java 类**（`find module-* -path "*app/erp/pro*"` 命中 0；无任何模块生成 `app.erp.pro` 包；不存在 "pro" 域）。实际项目域是 `module-projects`，实体为 `app.erp.prj.*`（`ErpPrjProject`/`ErpPrjTask`）。
- **零关系依赖**：全 cs 模块树（非 target）中除实体声明自身外，无任何 `<to-one>/<to-many refEntityName="app.erp.pro...">` 引用这两个实体（grep 已确认）。它们是纯死声明（疑似未完成的「专业服务」集成残留或复制粘贴遗留）。
- 派生产物：生成文件 `module-cs/erp-cs-dao/src/main/resources/_vfs/erp/cs/orm/_app.orm.xml:1122-1131` 同样含这两个幽灵实体（由源 orm 生成；`_` 前缀=生成文件，不可手改）。
- 影响：加载 cs ORM 模型即抛 `ClassNotFoundException: app.erp.pro.dao.entity.ErpProProject`，先于任何测试逻辑。
- 现有测试类：cs 冒烟测试**已存在**——`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsTicketTypeCrudSmoke.java`（抽样 `ErpCsTicketType`→`ErpCsSlaPolicy` 头-行对，5 类操作：testCreateHead/testQueryHead/testUpdateHead/testDeleteHead/testLineRelation，`createPrereqs()` 返回空即「无前置依赖」），但整体 `@Disabled("blocked by pre-existing model defect (cs: non-existent app.erp.pro.* entities)")`；`_cases/` 快照尚未录制（模块从未成功加载）。幽灵实体移除后即可解除 `@Disabled` 并录制快照。

**测试基线（复用，实时核实）**：`docs/architecture/testing-strategy.md` 已沉淀「Nop 测试 runbook 落地（Phase 4 CRUD 冒烟实践沉淀）」小节——落位约定（`-service` 模块 + `_cases/`）、schema bootstrap（`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)`）、动作映射（save=插入/update=按 id 合并/delete=逻辑删）、主键非自动变量（多步 Java 内传 id）、跨域强制外键处理（加 `app-erp-master-data-service` test 依赖 + `createPrereqs()` 自建主数据）、快照非确定性屏蔽（`DEL_VERSION`/decimal 列置 `*`）。本计划 mfg/cs 测试完全复用该模式。

**cs 测试抽样（实时核实）**：现有测试抽样 cs 内部头-行对 `ErpCsTicketType`→`ErpCsSlaPolicy`（行实体 `ticketTypeId` 外键引用头实体，`ErpCsSlaPolicy` 有 `to-one ticketType`→`ErpCsTicketType`）。两实体均为 cs 内部，`ErpCsTicketType` 强制字段仅 `code`/`name`（字符串，可编辑），无强制跨域外键；`createPrereqs()` 返回空（无前置依赖）——故 cs 测试**无需** master-data test 依赖（与 mfg 不同：mfg 因 `erp-md/posted-status` 字典解析而需 master-data-service test 依赖，cs 的 `erp-cs/*` 字典在 cs-meta 自身 classpath 内）。本计划沿用现有 TicketType 抽样、解除其 `@Disabled`（与 mfg 解除 `TestErpMfgRoutingCrudSmoke` 完全对称），不新建测试、不改抽样实体。

**工作树**：除未跟踪 `_tmp/` 外干净（`git status` 核实）。

**剩余差距**：(1) mfg 补齐 `posted-status` 字典（+ master-data-service test 依赖使字典上 classpath）并解除测试 `@Disabled`；(2) cs 移除 2 个幽灵实体声明、regen、解除现有测试 `@Disabled` 并录制快照；(3) 全量验证 18/18 绿 + 收尾 roadmap/前置计划 Deferred。

## Goals

- **manufacturing 域 CRUD 冒烟测试转绿**：补齐 `erp-md/posted-status` 字典，解除 `TestErpMfgRoutingCrudSmoke` 的 `@Disabled`，`mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿（CHECKING 模式）。
- **customer-service 域 CRUD 冒烟测试转绿**：移除 cs 源 orm 的 2 个 `app.erp.pro.*` 幽灵实体声明、regen cs dao，解除现有 `TestErpCsTicketTypeCrudSmoke` 的 `@Disabled` 并录制快照（`ErpCsTicketType`→`ErpCsSlaPolicy` 头-行对，5 类操作），`mvn test -pl module-cs/erp-cs-service -am` 全绿（CHECKING 模式）。
- **CRUD 路线图收尾至 18/18**：`crud-roadmap.md` Phase 4 表 mfg/cs 两行标 ✅；前置计划 `2026-06-30-2328-2` Deferred 中 mfg/cs 两项标记为已由本计划解决。
- **根 `mvn test` 全绿**：18 域冒烟测试 Errors=0（无 `@Disabled`/`Skipped`）。

## Non-Goals

- **不改变 `postedStatus` 的业务语义或状态机**——本计划只补齐字典以解除加载阻塞；过账的业务流转/业财过账逻辑属 `implementation-roadmap.md`。
- **不创建 "pro" 域**——cs 的 `app.erp.pro.*` 是死声明（零依赖），直接移除；不为它们新建域或 Java 类。
- **不重构 cs/mfg 模型其他部分**——仅最小化修复两处阻塞缺陷，不动其余实体/列/关系（保护区域最小改动原则）。
- **不测试业务规则/状态机/业财过账**——同前置计划 Non-Goal，本计划只验证标准 CRUD（`CrudBizModel` 通用操作）。
- **不做跨域端到端业务循环**——属独立 plan-first。
- **不创建 `app-erp-seed` 模块**——冒烟测试自包含。

## Task Route

- Type: `bug investigation`（定位两处模型缺陷根因）+ `implementation-only change`（补字典、移除死声明、regen、新建测试，不改公共 API 契约；标准 CRUD 行为不变）。模型缺陷修复属保护区域最小改动，需独立草案审查裁决。
- Owner Docs: `docs/backlog/crud-roadmap.md`（Phase 4）、`docs/architecture/testing-strategy.md`（runbook 落地）、`docs/design/finance/state-machine.md`（过账状态 DRAFT/POSTED 语义权威源）、平台 `../nop-entropy/docs-for-ai/03-runbooks/`（测试 runbook、codegen runbook）。
- Skill Selection Basis: `Skill: none`。`docs/skills/README.md` 现有技能均为审计方法，无字典编写/codegen/测试编写技能匹配。补字典遵循 master-data 现有 16 个 `.dict.yaml` 模式；regen 遵循各域 `Erp<X>CodeGen.java` 入口；测试编写遵循 testing-strategy runbook。独立草案/结束审计用 `plan-audit-prompt.md` / `closure-audit-prompt.md`。

## Infrastructure And Config Prereqs

- 无新增基础设施。H2 内存库（各 `-app` 模块已含 `quarkus-jdbc-h2`；`-service` 测试 `@NopTestConfig(localDb=true)` 强制 H2）。
- codegen 入口（已存在）：`module-manufacturing/erp-mfg-codegen/.../ErpMfgCodeGen.java`、`module-cs/erp-cs-codegen/.../ErpCsCodeGen.java`。regen 通过运行对应 codegen 测试类（Maven test 触发）。
- 字典文件直接创作于 `module-master-data/erp-md-meta/src/main/resources/_vfs/dict/erp-md/`（字典非 codegen 产物，与现有 16 个 `.dict.yaml` 同级手写）。
- 无回滚脚本需求（模型改动为纯新增 1 字典文件 + 删除 2 死实体声明；若 regen 出现非预期差异，`git` 可回退源 orm 与生成物）。

## Execution Plan

### Phase 1 - 修复 manufacturing 字典缺陷（补齐 `erp-md/posted-status`）

Status: completed
Targets: `module-master-data/erp-md-meta/src/main/resources/_vfs/dict/erp-md/posted-status.dict.yaml`（新增）、`module-manufacturing/erp-mfg-service/pom.xml`（新增 master-data test 依赖）、`module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgRoutingCrudSmoke.java`（解除 `@Disabled`）
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

- [x] `Decision`：`posted-status` 字典值定义。列 `postedStatus` 为 `VARCHAR(20)` 字符串存储，故字典 `valueType: string`。语义权威源 `docs/design/finance/state-machine.md`：过账生命周期为 `DRAFT（草稿/未过账）→ POSTED（已过账，终态）`，该文档亦将 `CANCELLED（已作废）` 列为单据终态。裁决：采用 **3 态字符串字典**——`DRAFT`(未过账) / `POSTED`(已过账) / `CANCELLED`(已作废)，与 finance 状态机文档一致，覆盖 mfg 委外订单的过账语义。备选（被否）：(a) 2 态仅 DRAFT/POSTED——语义不足，未涵盖作废；(b) 整型 code（10/20/30）——列类型为字符串，整型字典类型不匹配。残留风险：若后续业务深化发现 mfg 委外过账语义与凭证过账不同，可在 implementation-roadmap 阶段调整字典（届时需数据迁移，本计划仅建初始枚举）。
  - Skill: none
- [x] `Add`：新建 `posted-status.dict.yaml`，`valueType: string`，3 个 options（DRAFT/POSTED/CANCELLED，label 中英），格式对齐现有 `active-status.dict.yaml`（`label`/`locale`/`valueType`/`description`/`options` 结构）。
  - Skill: none
- [x] `Add`：`module-manufacturing/erp-mfg-service/pom.xml` 新增 test scope 依赖 `app-erp-master-data-service`。**关键**：字典文件位于 `erp-md-meta`，但 `erp-md-dao` 不依赖 `erp-md-meta`，故 master-data 的字典资源**不会**传递到 mfg-service 测试 classpath；不引入此依赖则 xmeta 解析 `erp-md/posted-status` 仍抛 `nop.err.graphql.unknown-dict`。引入 `master-data-service`（test scope）同时覆盖 `ErpMfgRouting` 的强制跨域外键校验（其引用 `ErpMdMaterial`/`ErpMdUoM` 等 ErpMd* 实体）——同前置计划 inventory/sales 模式（`erp-inv-service/pom.xml` test scope 依赖 `app-erp-master-data-service`，`TestErpInvStockMoveCrudSmoke` javadoc 已沉淀此约束）。
  - Skill: none
- [x] `Add`：解除 `TestErpMfgRoutingCrudSmoke` 的 `@Disabled` 注解与 `import org.junit.jupiter.api.Disabled`（保留测试类其余内容不变——抽样实体、5 方法、`_cases/` 快照均已就绪）。
  - Skill: none
- [x] `Proof`：`mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿（CHECKING 模式 Errors=0），证明字典补齐后 mfg xmeta 正常加载、5 类 CRUD 操作快照回放通过。
  - Skill: none

Exit Criteria:

> 本阶段交付 mfg 字典缺陷修复 + 测试转绿。完整仓库 `mvn test` 归 Closure Gates。

- [x] `posted-status.dict.yaml` 存在且 `valueType: string`、3 options；mfg `_ErpMfgSubcontractOrder.xmeta` 的 `erp-md/posted-status` 引用可解析（字典经新增的 `master-data-service` test 依赖上 mfg-service 测试 classpath；无需手改 xmeta）
- [x] `TestErpMfgRoutingCrudSmoke` 不再 `@Disabled`，`mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿（CHECKING，5 方法 Errors=0）

### Phase 2 - 修复 customer-service 幽灵实体缺陷 + 解除现有冒烟测试 `@Disabled` 并录制快照

Status: completed
Targets: `module-cs/model/app-erp-cs.orm.xml`（移除 2 实体声明）、`module-cs/erp-cs-dao/.../orm/_app.orm.xml`（regen 产物）、`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsTicketTypeCrudSmoke.java`（解除 `@Disabled`）、`module-cs/erp-cs-service/_cases/`（录制快照）
Skill: none

- Item Types: `Decision | Add | Fix | Proof`
- Prereqs: 无（与 Phase 1 独立，可并行；编号仅按 域顺序）

- [x] `Decision`：cs `app.erp.pro.*` 处置方案。实时核实两实体零关系依赖、无对应 Java、无 "pro" 域。裁决：**从源 orm 直接移除** `ErpProProject`/`ErpProTask` 两个 `<entity>` 声明（cs orm 611-641 行区段的末尾两个），保留其上方的合法跨域引用（`ErpMdPartner`/`ErpMdOrganization`/`NopAuthUser`）。备选（被否）：(a) 新建 "pro" 域并生成实体——零业务依据、无 owner doc，属凭空扩大范围；(b) 改引用 `app.erp.prj.*`（projects 域）——cs 无任何关系指向这两个实体，无引用可改，本质就是删除死声明。残留风险：无（零依赖删除，regen 后 `_app.orm.xml` 同步移除）。
  - Skill: none
- [x] `Decision`：cs 测试抽样实体沿用现有 `ErpCsTicketType`→`ErpCsSlaPolicy`，仅解除 `@Disabled`（与 mfg 解除 `TestErpMfgRoutingCrudSmoke` 完全对称），不新建测试、不改抽样。理由：现有测试已按 Phase 1 抽样规则写好（5 类操作、cs 内部头-行对、`createPrereqs()` 空=无前置依赖），`ErpCsTicketType` 强制字段仅 `code`/`name`、无强制跨域外键，故 cs 无需 master-data test 依赖。备选（被否）：(a) 改抽样 `ErpCsTicket`→`ErpCsTicketAction`——`ErpCsTicket` 有强制跨域外键 `customerId`→`ErpMdPartner`，需引入 master-data test 依赖 + 自建 Partner，徒增复杂度且浪费已写好的 TicketType 测试；(b) 同时保留两测试——违反 roadmap「每域 1 主实体 + 1 头-行对」抽样约定。
  - Skill: none
- [x] `Fix`：编辑源 `module-cs/model/app-erp-cs.orm.xml`，删除 `ErpProProject` 与 `ErpProTask` 两个 `<entity>...</entity>` 块（约 630-641 行）。
  - Skill: none
- [x] `Add`：触发 cs regen——`module-cs/erp-cs-codegen` 通过 exec-maven-plugin `postcompile` 绑定（`generate-sources` 阶段）执行 `gen-orm.xgen`，读取源 `model/app-erp-cs.orm.xml` 重生成 ORM 产物。故运行 `mvn generate-test-resources -pl module-cs/erp-cs-codegen` 即触发 regen（`mvn test -pl module-cs/erp-cs-codegen` 亦可，但该模块无测试，故 `generate-test-resources` 为最小规范阶段）。注意：`ErpCsCodeGen.java` 是调试用 `main` 类，**非** `mvn` 入口，勿以 `java -cp` 直接调用。regen 后核对生成文件 `_app.orm.xml` 不再含 `app.erp.pro`。
  - Skill: none
- [x] `Add`：解除 `TestErpCsTicketTypeCrudSmoke` 的 `@Disabled` 注解与 `import org.junit.jupiter.api.Disabled`（保留测试类其余内容不变——抽样实体、5 方法均已就绪）。
  - Skill: none
- [x] `Add`：录制快照基线——首次以 `@NopTestConfig(..., snapshotTest = SnapshotTest.RECORDING)` 运行录制 `_cases/` 快照（RECORDING 模式框架抛 `nop.err.autotest.snapshot-finished`、Maven 显示 Errors 为预期行为），人工审查快照正确性后切回日常 `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)`（CHECKING）。删除用例 `DEL_VERSION` 与 decimal 域列录制↔校验间非确定性，按 runbook 手工置 `*`。
  - Skill: none
- [x] `Proof`：`mvn test -pl module-cs/erp-cs-service -am` 全绿（CHECKING 模式 Errors=0），证明幽灵实体移除后 cs 模型正常加载、5 类 CRUD 操作通过。
  - Skill: none

Exit Criteria:

> 本阶段交付 cs 模型缺陷修复 + 现有冒烟测试转绿。完整仓库 `mvn test` 归 Closure Gates。

- [x] 源 `app-erp-cs.orm.xml` 与 regen 后 `_app.orm.xml` 均不再含 `app.erp.pro`；cs ORM 模型可正常加载（无 `ClassNotFoundException`）
- [x] `TestErpCsTicketTypeCrudSmoke` 不再 `@Disabled`，`mvn test -pl module-cs/erp-cs-service -am` 全绿（CHECKING，5 方法 Errors=0），快照经 RECORDING→CHECKING 切换

### Phase 3 - 全量验证 + roadmap / 前置计划 Deferred 收尾

Status: completed
Targets: `docs/backlog/crud-roadmap.md`、`docs/plans/2026-06-30-2328-2-phase4-crud-smoke-tests.md`、`docs/logs/2026/07-01.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2

- [x] `Proof`：根目录 `mvn test -fae` = BUILD SUCCESS，18 域冒烟测试 Errors=0（无 `@Disabled`/`Skipped`），与 Phase 1/2 逐域结果交叉核对一致。
  - Skill: none
- [x] `Add`：更新 `crud-roadmap.md` Phase 4「逐域通过状态」表——manufacturing 行（`ErpMfgRouting`→`ErpMfgRoutingOperation`）与 customer-service 行（`ErpCsTicketType`→`ErpCsSlaPolicy`）标 ✅；Phase 4 说明文字移除「16/18」「manufacturing/customer-service 阻塞」表述，改为 18/18 全绿。
  - Skill: none
- [x] `Add`：在前置计划 `2026-06-30-2328-2` 的两处 Deferred（mfg、cs）追加「已由 `2026-07-01-0215-1` 解决」标注（不改其 Plan Status，仅补后继指针，保持历史可追溯）。
  - Skill: none
- [x] `Add`：更新当日开发日志 `docs/logs/2026/07-01.md`（按 `docs/logs/00-log-writing-guide.md` 格式，时间倒序条目），记录两缺陷修复 + 18/18 收尾 + 验证状态。
  - Skill: none

Exit Criteria:

> 本阶段使 CRUD 路线图闭合至 18/18，并保持文档与前置计划可追溯。

- [x] 根 `mvn test -fae` = BUILD SUCCESS，18 域冒烟测试全绿（无 `@Disabled`/`Skipped`）
- [x] `crud-roadmap.md` Phase 4 反映 18/18 全绿；前置计划 Deferred 两项含本计划后继指针；当日日志已记

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0e63f49d7ffemU6CsiTyretFsP，独立 general 子代理，新会话）— 基线主张全部实时核实属实（mfg 字典缺失 + cs 幽灵实体零依赖、cs 抽样头-行对与跨域 FK 处理、regen 机制 `postcompile/gen-orm.xgen`、`valueType: string` 非前所未有）。1 项阻塞 B1：Maven 模块路径误用 `module-customer-service`（实际 reactor 目录为 `module-cs`），4 处命令执行即失败。2 项建议：S1 regen 触发措辞误指 `ErpCsCodeGen` 调试 main 类（应指 postcompile 阶段）；S2 `CANCELLED` 语义偏松但已在 Decision 裁决。迭代 1 已修订：4 处 `module-customer-service`→`module-cs`（B1）；regen 项改述为 postcompile/gen-orm.xgen 并警示勿 `java -cp` 调 `ErpCsCodeGen`（S1）；S2 无需改。
- Independent draft review iteration 2: **needs revision**（ses_0e639c3dbffeBKyc0wmpwMFhzO，独立 general 子代理，新会话）— 复审 B1/S1 已修复确认。发现 2 项新阻塞：B2 字典文件位于 `erp-md-meta`，但 `erp-md-dao` 不依赖 `erp-md-meta`，故 master-data 字典资源**不传递**到 mfg-service 测试 classpath，仅加字典文件不解 `@Disabled`（需 mfg-service 加 `master-data-service` test 依赖，同 inventory/cs 模式）；B3 全部逐域命令用 `-pl module-<parent> -am`，而 `module-manufacturing`/`module-cs` 是 pom 打包的父模块，Maven 仅构建父 pom、**执行 0 测试**（须 `-pl module-<domain>/erp-<short>-service`）。迭代 2 已修订：Phase 1 Targets 增 `erp-mfg-service/pom.xml` + 新增 master-data test 依赖 `Add` 项 + 修正退出标准措辞（字典经新 test 依赖上 classpath）；5 处命令改 `module-manufacturing/erp-mfg-service`、`module-cs/erp-cs-service`（B3）；regen 项采纳 S2 改 `generate-test-resources` 为最小规范阶段。
- Independent draft review iteration 3: **needs revision**（ses_0e631d1d0ffeBBtHcTSKYMAbiH，独立 general 子代理，新会话）— 复审 B2/B3 已修复确认（`erp-md-dao` 确不依赖 `erp-md-meta` 故字典不上 classpath、`erp-inv-service` 确为 test-scope 先例；父 `-pl` 形式实测仅构建父 pom 0 测试）。发现 1 项新阻塞 B4：基线不实——cs 冒烟测试 `TestErpCsTicketTypeCrudSmoke` **已存在**（抽样 `ErpCsTicketType`→`ErpCsSlaPolicy`、5 方法、`@Disabled`、`createPrereqs()` 空=无前置依赖），原基线误称「cs 测试不存在、需新建 `ErpCsTicket` 测试」；且若新建 Ticket 测试而不处置已存在的 `@Disabled` TicketType 测试，则 Closure Gate「无 `@Disabled`/`Skipped`」不可达。迭代 3 已修订：基线改为「cs 测试已存在」；Phase 2 重构——新增 `Decision`（抽样沿用现有 TicketType、与 mfg 解除 `@Disabled` 对称），删除「新建 Ticket 测试」「cs pom 加 master-data 依赖」两项（TicketType 为 cs 内部、无强制跨域外键，无需 master-data），新增「解除 `TestErpCsTicketTypeCrudSmoke` 的 `@Disabled` + 录制快照」项；Phase 3 roadmap cs 行改回 `ErpCsTicketType`→`ErpCsSlaPolicy`；Goals/剩余差距/Phase 1 master-data 项措辞（S3/S4 自指歧义）一并校正。
- Independent draft review iteration 4: **passes draft review**（ses_0e62b062effe4ug0GzuWBycWst，独立 general 子代理，新会话）— B4 修复经实测确认内部一致（cs 现有测试抽样/`createPrereqs()` 空/5 方法；基线、Goals、Phase 2 主体、Phase 3 roadmap 行均改对；Closure Gate「无 `@Disabled`/`Skipped`」可达——全仓仅 mfg+cs 两处 `@Disabled` CRUD 冒烟测试均将被解除）。全量复审无进一步阻塞：基线诚实、命令均可运行（无父 `-pl` 形式）、不手改 `_` 前缀文件、单计划范围正确、anti-slack 合规、master-data 先例核实。发现 2 项文本一致性残留（规则 11）：B5 Phase 2 标题仍写「+ 新建冒烟测试」与主体矛盾；B6 Closure Status Note 仍引用不存在的 `TestErpCsTicketCrudSmoke`（使关闭条件字面不可达）。**迭代 4 已修订**：Phase 2 标题改为「解除现有冒烟测试 `@Disabled` 并录制快照」；Closure Status Note 改为「解除现有 `TestErpCsTicketTypeCrudSmoke` 的 `@Disabled`（转绿）」。残留风险（非阻塞）：Phase 1 master-data 依赖项同时陈述字典上 classpath + ErpMfgRouting 跨域 FK 两个理由，即便后者不成立前者仍强制该依赖。

**共识达成**：迭代 4 复审在修订 B5/B6 后无阻塞问题，计划为可接受的执行契约。Plan Status 由 `draft` 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划含模型与测试代码改动，结束时运行全量 `mvn test`。

- [x] 范围内行为完成：mfg + cs 两域 CRUD 冒烟测试（5 类操作）落地并通过；18/18 域覆盖
- [x] 相关文档对齐：`crud-roadmap.md` Phase 4 = 18/18 全绿；前置计划 Deferred 含后继指针；当日日志已记
- [x] 已运行验证：`mvn test -pl module-manufacturing/erp-mfg-service -am`、`mvn test -pl module-cs/erp-cs-service -am` 逐域全绿；根 `mvn test -fae` = BUILD SUCCESS（无 `@Disabled`/`Skipped`）
- [x] 无范围内项目降级为 deferred/follow-up（两处缺陷均在本计划修复，非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### postedStatus 字典枚举的业务深化（如委外过账与凭证过账语义差异）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划只补齐字典以解除加载阻塞，采用与 finance 状态机文档一致的 DRAFT/POSTED/CANCELLED 3 态；业务语义深化（如状态迁移规则、与业财过账联动）属 BizModel 层，归 `implementation-roadmap.md`。
- Successor Required: yes（触发条件：implementation-roadmap 深化 mfg 委外过账 BizModel 时，若发现枚举不足则调整字典并做数据迁移）

## Closure

Status Note: 计划可关闭的条件——mfg 字典缺陷修复（`TestErpMfgRoutingCrudSmoke` 转绿）+ cs 幽灵实体移除并解除现有 `TestErpCsTicketTypeCrudSmoke` 的 `@Disabled`（转绿），CRUD 路线图 Phase 4 收尾至 18/18 全绿，根 `mvn test` BUILD SUCCESS，前置计划 Deferred 两项含后继指针。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 ses_0e619db29ffeQt1Oql1CL5FUIR（新会话，不重用执行者上下文），结论 **ACCEPT**。
- 审计核验（全部 PASS，针对实时仓库）：
  1. 文本一致性——Plan Status=completed、Phase 1/2/3 Status 均 completed、所有 phase items `[x]`、Exit Criteria `[x]`；仅 Closure Gates 末两项为审计占位（本审计后已勾选）。
  2. mfg 缺陷修复——`posted-status.dict.yaml` 存在且 `valueType: string`、3 options（DRAFT/POSTED/CANCELLED）；`erp-mfg-service/pom.xml` 含 test-scope `app-erp-master-data-service`；`TestErpMfgRoutingCrudSmoke` 无 `@Disabled`。
  3. cs 缺陷修复——源 `app-erp-cs.orm.xml` 与生成 `_app.orm.xml` 均不含 `app.erp.pro`；`TestErpCsTicketTypeCrudSmoke` 无 `@Disabled`；5 方法 `_cases/.../output/` 齐全。
  4. 构建+测试——`mvn clean install -DskipTests -o` = BUILD SUCCESS（01:16）；根 `mvn test -fae -o` = BUILD SUCCESS。18 个 `*CrudSmoke.java`，零 `@Disabled`；surefire 聚合 Tests=90 / Failures=0 / Errors=0 / Skipped=0。
  5. roadmap 收尾——`crud-roadmap.md` Phase 4 mfg/cs 两行 ✅、状态文字 18/18。
  6. 前置计划指针——`2026-06-30-2328-2` Deferred mfg/cs 两项含「已由 0215-1 解决」后继指针，其 Plan Status 保持 completed 未被改动。
  7. 当日日志——`docs/logs/2026/07-01.md` 顶部新条目记录本计划执行 + 验证状态。
  8. 快照非确定性——mfg/cs 删除用例 `output/tables/*.csv` 的 `DEL_VERSION` 均为通配符 `*`（非裸 currentTimeMillis）。
- 执行者验证证据（实时仓库）：
  - 构建：`mvn clean install -DskipTests` = BUILD SUCCESS（146 reactor 模块）。
  - 逐域：`mvn test -pl module-manufacturing/erp-mfg-service -am` 与 `mvn test -pl module-cs/erp-cs-service -am` 均 BUILD SUCCESS（各 5 方法，Errors=0）。
  - 全量：根 `mvn test -fae` = BUILD SUCCESS（18 域 × 5 方法 = 90，Failures=0/Errors=0/Skipped=0，无 `@Disabled`）。
  - 落位：字典置于 `erp-md-meta/.../dict/erp-md/`；mfg/cs 删除用例 `DEL_VERSION` 已 `*` 屏蔽；cs regen 经 `mvn generate-test-resources -pl module-cs/erp-cs-codegen` 触发。

Follow-up:

- mfg 委外过账字典枚举的业务深化（见上方 Deferred，触发：implementation-roadmap 深化 mfg BizModel）
