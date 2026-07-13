# 2026-07-13-1419-2-projects-fk-name-resolution Projects 域外键名称解析批量推广（列表页 ID→名称）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「全量 1,036 FK 列名称解析」（Successor Required: yes，触发条件「高价值子集验证后批量推广需求」——**已满足**：经 6 批次验证机制 D 可行性 55 实体落地）
> Related: `2026-07-11-1643-1-amis-frontend-quality.md`（机制 D 范式源）、`2026-07-13-1043-1-finance-fk-name-resolution.md`（finance 先例）、`2026-07-13-1419-1-assets-fk-name-resolution.md`（同批 N=1，无依赖）、`2026-07-13-1419-3-quality-maintenance-fk-name-resolution.md`（同批 N=3，无依赖）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 范围，独立子代理全量盘点 `module-projects/erp-prj-meta/` + `erp-prj-service/` + `erp-prj-web/`）：

### 机制 D 已验证（全域 6 批次 55 实体）

机制 D 三层接线（参考 `ErpSalOrder` + `ErpMfgWorkOrderLine` 先例）：(1) 自定义 xmeta 增派生 `*Name` prop；(2) BizModel 增 `@BizLoader(forType = Entity.class)` 经 `orm().batchLoadProps` 批量加载；(3) view.xml `<grid id="list"><cols x:override="bounded-merge">` 替换列。`ext:relation` 已在全部相关 FK `*Id` prop 声明，零 ORM 变更。

### Projects 域覆盖现状

- **零 FK 名称解析覆盖**：全部 16 实体的自定义 view.xml 为空 `<grid id="list"/>`（继承生成基线列原样），无 `<cols>` bounded-merge 覆盖。
- **未覆盖（列表页显示原始数字 ID）16 实体**——本计划范围。

### 未覆盖 16 实体清单（生成网格中显示为 `ui:number` 的 FK 列）

| # | 实体 | 生成网格中原始 `*Id` FK 列 | 用户面价值 |
|---|------|---------------------------|-----------|
| 1 | **ErpPrjProject** | orgId, projectTypeId, customerId, currencyId, managerId | 项目头，项目管理核心 |
| 2 | **ErpPrjProjectType** | defaultSubjectId | 项目类型（配置） |
| 3 | **ErpPrjProjectUser** | projectId, userId | 项目成员 |
| 4 | **ErpPrjTask** | projectId, parentTaskId, assigneeId, dependsOnId | 任务（DAG 依赖） |
| 5 | **ErpPrjMilestone** | projectId | 里程碑 |
| 6 | **ErpPrjActivityType** | subjectId | 活动类型（配置） |
| 7 | **ErpPrjTimesheet** | orgId, projectId, taskId, userId, activityTypeId, currencyId | 工时单（6 FK 列） |
| 8 | **ErpPrjBilling** | projectId, orgId, customerId, milestoneId, currencyId | 开单 |
| 9 | **ErpPrjBillingLine** | billingId, taskId, subjectId | 开单明细行 |
| 10 | **ErpPrjBudget** | projectId, orgId, currencyId | 项目预算 |
| 11 | **ErpPrjBudgetLine** | budgetId, subjectId, taskId | 预算明细行 |
| 12 | **ErpPrjCostCollection** | projectId, orgId, currencyId | 成本归集 |
| 13 | **ErpPrjCostCollectionLine** | costCollectionId, subjectId, taskId | 成本归集明细行 |
| 14 | **ErpPrjProjectPnl** | projectId, orgId, currencyId | 项目损益 |
| 15 | **ErpPrjProjectSettlement** | projectId, orgId, customerId, pnlSnapshotId, currencyId | 竣工结算 |
| 16 | **ErpPrjProjectSettlementLine** | settlementId, subjectId | 结算明细行 |

### ext:relation 缺口（1 个 FK 列无 `ext:relation`，阻碍名称解析）

| # | 实体 | FK prop | 处置裁决 |
|---|------|---------|---------|
| 1 | ErpPrjProjectSettlement | assetCardId | 保留原始 ID（转固资产卡片回指，归 successor） |

> 裁决原则：与 finance 批次（1043-1）同口径——纯链路型内部 ID 无独立业务"名称"语义时保留原始 ID。

剩余差距：16 projects 实体列表页显示原始数字 ID（用户面 P1 缺陷）。

## Goals

- 16 projects 实体列表页的高价值用户面 FK 列显示名称而非原始 ID（经机制 D：xmeta `*Name` + BizModel `@BizLoader` 批量加载 + view.xml `bounded-merge`）。
- 高价值 FK 定义：维度型外键（project→projectName 读 `ErpPrjProject.name`；projectType→projectTypeName 读 `ErpPrjProjectType.name`；customer→customerName 读 `ErpMdPartner.name`；currency→currencyName；org→orgName；manager→managerName 读 `ErpMdEmployee.name`；assignee→assigneeName 读 `ErpMdEmployee.name`；user→userName 读 `ErpMdEmployee.name`；task→taskName 读 `ErpPrjTask.name`；parentTask→parentTaskName；milestone→milestoneName 读 `ErpPrjMilestone.name`；activityType→activityTypeName 读 `ErpPrjActivityType.name`；subject→subjectName 读 `ErpMdSubject.name`；billing→billingCode 读 `ErpPrjBilling.code`；budget→budgetCode 读 `ErpPrjBudget.code`；costCollection→costCollectionCode 读 `ErpPrjCostCollection.code`；settlement→settlementCode 读 `ErpPrjProjectSettlement.code`）+ 高价值父单型内部链路（dependsOnId→dependsOnTaskName 同实体 DAG 依赖，承载业务上下文）。派生 prop 名遵循 `{relation}Name`/`{relation}Code` 约定（镜像 ErpSalOrder `customerName`/`warehouseName` 范式），不统一用 `employeeName`。
- 零 ORM/契约变更（机制 D 仅 xmeta 派生字段 + BizModel 只读 loader + view.xml 静态定制）。

## Non-Goals

- **其他域 FK 名称解析**（assets 由 `2026-07-13-1419-1` 承接；quality/maintenance 由 `2026-07-13-1419-3` 承接；CRM/CS/HR/master-data/logistics/contract/b2b/drp/aps 归后续 successor）。
- **codegen 模板层 FK 名称解析方案**——经 0600-1 裁决否决。
- **drawer 子表/明细行子网格 FK 名称**——本计划仅处理主列表网格 `<grid id="list">`。
- **1 个 ext:relation 缺口 FK 的名称解析**（assetCardId@ErpPrjProjectSettlement）——转固资产卡片回指，无独立业务"名称"语义，保留原始 ID，归 successor。
- **看板/报表 FK 名称**——已由 1643-1 Phase 4 覆盖。

## Task Route

- Type: `app-layer design change`（改用户可见的列表页显示行为，跨 projects 域多实体，不改 API/模型/认证）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（机制 D 权威参考）、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`
- Skill Selection Basis: xmeta 派生 + view.xml bounded-merge → `nop-frontend-dev`；BizModel `@BizLoader` → `nop-backend-dev`；JUnit 测试 → `nop-testing`。
- Protected Areas: 无 ORM/ask-first 变更。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - 核心项目管理实体 FK 名称解析（Project/ProjectType/ProjectUser/Task/Milestone/ActivityType）

Status: completed
Targets: `module-projects/erp-prj-meta/.../ErpPrj{Project,ProjectType,ProjectUser,Task,Milestone,ActivityType}/ErpPrj*.xmeta`；`module-projects/erp-prj-service/.../entity/ErpPrj*BizModel.java`；`module-projects/erp-prj-web/.../ErpPrj*/ErpPrj*.view.xml`
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无（机制 D 已由 6 批次验证）

- [x] `Decision`: 裁决 Phase 1 实体的目标 FK 清单 + 显示字段——维度型 FK 全部解析（project→projectName；projectType→projectTypeName；customer→customerName；currency→currencyName；org→orgName；manager→managerName 读 `ErpMdEmployee.name`；assignee→assigneeName 读 `ErpMdEmployee.name`；user→userName 读 `ErpMdEmployee.name`；task→taskName；parentTask→parentTaskName；dependsOn→dependsOnTaskName 同实体 DAG 依赖（ext:relation `dependsOn` 已声明）；milestone→milestoneName；activityType→activityTypeName；subject→subjectName）。派生 prop 名遵循 `{relation}Name` 约定（manager→managerName 非 employeeName，assignee→assigneeName 非 employeeName），镜像 `ErpSalOrder` 范式。
  - Skill: `nop-backend-dev`
- [x] `Add`: 6 实体 xmeta 增派生 `*Name` prop（镜像 `ErpSalOrder.xmeta`，`queryable="false" sortable="false"` + `schema type="java.lang.String"`）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 6 实体 BizModel 增 `@BizLoader(forType = ErpPrj*.class)` 方法（镜像 `ErpSalOrderBizModel:228-269`，`orm().batchLoadProps` 批量加载 + null 安全读取）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 6 实体 view.xml `<grid id="list">` 由空占位改为 `<cols x:override="bounded-merge">`，用 `*Name` 列替换原始 `*Id` 列。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 6 核心项目管理实体列表网格显示 `*Name` 而非原始 `*Id`（6 view.xml `xmllint --noout` well-formed + bounded-merge 含 `*Name` 列）

### Phase 2 - 工时/开单/预算/成本/损益/结算实体 FK 名称解析（Timesheet/Billing/BillingLine/Budget/BudgetLine/CostCollection/CostCollectionLine/ProjectPnl/ProjectSettlement/ProjectSettlementLine）

Status: completed
Targets: 10 实体的 xmeta + BizModel + view.xml（同 Phase 1 三层）
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: Phase 1 范式已验证

- [x] `Decision`: 逐项裁决 Phase 2 父单型/内部链路型 FK 的处置：billingId→billingCode、budgetId→budgetCode、costCollectionId→costCollectionCode、settlementId→settlementCode（均为父单 code 承载业务上下文，解析）。assetCardId@ProjectSettlement 保留原始 ID（ext:relation 缺口，归 successor）。pnlSnapshotId@ProjectSettlement 保留原始 ID（ext:relation `pnlSnapshot` 已声明但为内部技术快照链路——结算时点损益快照的回指，用户面价值低，保留原始 ID 归 successor；触发条件：结算明细需展示损益快照名称时）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 10 实体 xmeta 增派生 `*Name` prop（按 Phase 2 Decision 裁决清单）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 10 实体 BizModel 增 `@BizLoader` 方法（维度型 FK 读 `.name`；父单型读父实体 `.code`）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 10 实体 view.xml `<grid id="list">` 改 `<cols x:override="bounded-merge">`，`*Name` 替换 `*Id`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 10 工时/财务类实体列表网格显示 `*Name` 而非原始 `*Id`（10 view.xml `xmllint --noout` well-formed）

### Phase 3 - BizLoader 测试验证

Status: completed
Targets: `module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjFkNameLoader.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [x] `Add`: 新建 `TestErpPrjFkNameLoader.java`（extends `JunitAutoTestCase`，镜像 `TestErpFinFkNameLoader`），经 `IGraphQLEngine` findList 请求 `*Name` 字段触发 `@BizLoader`，断言 `ErpPrjProject`（projectTypeName/customerName/managerName）+ `ErpPrjTask`（projectName/assigneeName/dependsOnTaskName）名称对齐。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `TestErpPrjFkNameLoader` 全方法绿，验证 `@BizLoader` 批量加载防 N+1 且名称正确

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0a5d07da2ffeWx5SQJZ5lSERjP, 2026-07-13) — 0 Blocker / 1 Major / 1 Minor. M1 (derived prop naming inconsistency: managerId→`managerName` not `employeeName`, assigneeId→`assigneeName`, userId→`userName` across Goals/Decision/Test) — fixed: Goals + Phase 1 Decision reconciled to `{relation}Name` convention. m1 (Phase 3 representative test coverage) — non-blocking, intentional sampling per finance precedent.
- Independent draft review iteration 2: `needs revision` (ses_0a5be13b8ffetFMGii8T3ebzqT, 2026-07-13) — M1 fully resolved (naming consistent across Goals/Decision/Test, ext:relation names verified against live repo). New MAJOR surfaced: `pnlSnapshotId`@ErpPrjProjectSettlement resolvable FK (ext:relation `pnlSnapshot` exists) neither resolved nor deferred — violates anti-slack Rule 10. Fixed: added explicit adjudication in Phase 2 Decision (保留原始 ID as internal technical snapshot link) + Deferred section updated to 2 items.
- Independent draft review iteration 3: `accept` (ses_0a5b8e433ffetgbzlbOXbi2V1n, 2026-07-13) — pnlSnapshotId fix verified: Phase 2 Decision explicitly adjudicates + Deferred section contains both assetCardId and pnlSnapshotId. Anti-slack satisfied. 16 entities consistent. Naming convention `{relation}Name` holds. Template compliant. Plan ready for `active`.

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-projects/erp-prj-service -am`（含新增 `TestErpPrjFkNameLoader`）+ 16 view.xml `xmllint --noout` 一次。

- [x] 范围内行为完成（16 projects 实体列表页 FK 显示名称）
- [x] 相关文档对齐（`view-and-page-strategy.md` / `cross-module-entity-reference.md` 机制 D 范式无需更新）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + projects-service `mvn test` 0 failures/0 errors + 16 view.xml `xmllint --noout` well-formed）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 2 个内部链路型 FK 的名称解析（assetCardId + pnlSnapshotId）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `assetCardId`@ErpPrjProjectSettlement（转固资产卡片回指，ext:relation 缺失）+ `pnlSnapshotId`@ErpPrjProjectSettlement（结算时点损益快照回指，ext:relation `pnlSnapshot` 已声明但为内部技术快照链路，用户面价值低）均无独立业务"名称"语义或属内部技术链路。
- Successor Required: `yes`（触发条件：转固资产卡片实体落地 code 列或结算明细需展示损益快照名称时）

### 其他域 FK 名称解析

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅覆盖 projects 域。其余域归后续 successor。
- Successor Required: `yes`（触发条件：对应域 FK 名称解析需求落地时）

## Closure

Status Note: 全 3 Phase 完成且独立结束审计通过。机制 D 三层接线在 projects 域 16 实体全量落地（16 xmeta 派生 prop + 16 BizModel `@BizLoader` 共 49 个 loader + 16 view.xml bounded-merge），零 ORM 变更；`TestErpPrjFkNameLoader` 2 测试经 `IGraphQLEngine` 触发 `@BizLoader` 断言 Project/Task 名称对齐 master-data；2 内部链路 FK（assetCardId/pnlSnapshotId）按 Decision 诚实保留原始 ID 并归 successor。仓库验证全绿（`mvn clean install -DskipTests` 154 模块 + projects-service 69 测试 0 失败）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非执行者上下文）
- 实时仓库语义核实（grep/glob/read 全量）：
  - 16 view.xml 全部含 `<cols x:override="bounded-merge">`（抽样 ErpPrjProject/ErpPrjProjectSettlement 确认 `*Name` 列替换 `*Id`，assetCardId/pnlSnapshotId 仅在 view/edit 表单非 list grid，诚实保留）
  - 16 BizModel 全部含 `@BizLoader(forType = ErpPrj*.class)` 共 49 个 loader，抽样核实为真实实现（`orm().batchLoadProps` + null 安全读取），非空体/`return null` 占位（反空心检查通过）
  - 16 xmeta 全部含派生 prop（`queryable="false" sortable="false"` + `schema type="java.lang.String"`）
  - `TestErpPrjFkNameLoader.java` 含 2 真实 `@Test`（Project 5 名称 + Task 3 名称含 DAG dependsOnTaskName），断言非占位
- 五点一致性：Plan Status=completed / 3 Phase Status=completed / 3 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / `docs/logs/2026/07-13.md` 条目（projects 域 16 实体）一致
- Deferred 诚实性：2 内部链路 FK（assetCardId 无 ext:relation + pnlSnapshotId 内部技术快照）显式裁决保留原始 ID 且命名 successor 触发条件，未隐藏为"已完成"
- Docs sync：`docs/logs/2026/07-13.md` 已含本计划完整条目（机制 D + Phase 1/2 清单 + 测试 + full-green 验证）

Follow-up:

- 其他域 FK 名称解析 successor（见上方 Deferred）
