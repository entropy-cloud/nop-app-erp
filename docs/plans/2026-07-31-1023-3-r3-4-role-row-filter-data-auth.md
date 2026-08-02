# 2026-07-31-1023-3-r3-4-role-row-filter-data-auth R3.4 — 角色侧行级数据权限落地

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR3 R3.4（P1-MA6-002，**[auth/permissions — 19 data-auth.xml + IUserContext]**）
> Related: `docs/plans/2026-07-31-0958-1-r3-0-mr3-p1-finding-expansion.md`（R3.0 展开 R3.4）；`docs/audits/2026-07-29-1410-arm-ma6-data-permission-runtime.md`（P1-MA6-002）；`docs/plans/2026-07-30-0841-3-r1-29-multi-company-orgid-isolation.md`（P1-MA2-093 orgId 维度，MR1 已 done，互补协同）；`docs/plans/2026-07-31-0310-2-r2-7-api-contract-consistency.md`（role-resource 种子 + enable-action-auth 灰度范式）
> Audit: required

## Current Baseline

**P1-MA6-002**：owner doc `docs/design/roles-and-permissions.md §数据权限:66-71` 声明 4 类行级过滤规则（业务员只看自己创建/财务员全见/质检员只看分配给自己/维护员只看分配给自己），但**声明能力运行时彻底未落地**：

- **19 个 service 级 `erp-{module}.data-auth.xml` 全部 `<objs/>` 空规则**（实测 sales/finance/quality 三样本均 5 行空文件）；19 个 app 级 `app.data-auth.xml` 仅 `GenFromModules` 聚合空集。
- **0 自定义 `IDataAuthChecker`**（全仓 grep 确认）；唯一 `IQueryTransformer` 是 R1.29 的 `ErpOrgIsolationQueryTransformer`（orgId 维度，占 `nopGlobalQueryTransformer` bean 槽，非角色侧）。
- 平台机制：`DefaultDataAuthChecker.getFilter` 在 `<objs/>` 空时返回 null → `CrudBizModel.prepareFindPageQuery:381` `AuthHelper.appendFilter` 不附加任何条件 → 全角色同视野。owner doc §设计能力基线:91 声明 data-auth"独立于操作级开关，始终附加到查询条件"——与运行时（不附加）**直接矛盾**。

**关键纠正（工作项描述 nomenclature 错误，plan 须修正）：**
- **`createdById`/`assigneeId` 不存在**。真实过滤列分两类（实测各域 orm.xml）：
  - **userId 域列**（可直接用 `getUserId()` 比较）：`createdBy`（VARCHAR precision=50 userId，所有实体均有，"自己创建"规则）；quality `ownerId`（VARCHAR stdDomain=userId，"责任人"）。
  - **employee-id 域列**（BIGINT 职员 id，**不可直接与 `getUserId()` 比较**，须 user→employee 解析）：quality `inspectorId`（BIGINT"检验员(职员)"）；maintenance `assignedTo`（BIGINT"指派人"，`module-maintenance/model/app-erp-maintenance.orm.xml:259,356`——**非 ownerId**，工作项/审计的 `assigneeId`/`ownerId` 均错）。
- **`IUserContext` 已有 `getUserId():49`/`getDeptId():54`/`getRoles():79`/`isUserInRole(...)`**——userId 域列（createdBy/ownerId）用 `getUserId()` 即可，**无需改 IUserContext**。但 employee-id 域列（inspectorId/assignedTo）须 app 层 user→employee 解析（如 userId→员工 id 查询），此为 **app 层关注点非平台 IUserContext 变更**——仍不改 nop-entropy 平台接口。`deptId` 已存在但"部门可见"需 dept 树（hr 域部门树未与数据权限管道集成，审计 watch-item）。
- **R1.29 已裁决不改 nop-entropy 平台 `IUserContext`/`IServiceContext` 接口**，用 app 层 `IContext` attribute（`erp.currentOrgId` 范式）。IUserContext 在兄弟平台仓 `nop-entropy`，本 app 仓不可改。

**与 P1-MA2-093 互补不重复**（三源确认：MA6 报告 / arm-index cross-dedup / R3.0 plan）：093 是 orgId 多公司维度（MR1 R1.29 已 done），002 是角色侧 createdBy/inspectorId/ownerId/assignedTo 维度——不同声明来源 + 不同过滤列。R3.4 构建于 R1.29 基础设施（`module-common-service/.../org/` + config-gate 范式）。

**核心张力（central hard decision）：平台 `DefaultDataAuthChecker` 无 enable flag**——`GraphQLEngine:352-353` 每请求注入，`<objs/>` 一旦填充规则**立即生效**，且 fail-closed（`getFilter:203-206` 无 role 匹配时抛 `ERR_AUTH_NO_DATA_AUTH`）。审计 line 86-88 明确警告：全局开启可能使既有测试/报表"突然过滤"断裂。故**灰度是本 plan 最关键 Decision**；且"默认 OFF 保证零回归"**仅对 G1/G2 成立**（G3 per-rule `<when>` 门控关闭时仍可能因 fail-closed 抛错，须特殊处理）。

**role ID 缺口**：4 角色名（业务员/财务员/质检员/维护员）须匹配 `IUserContext.getRoles()` 返回的 role ID；当前**无 `nop_auth_role_resource` 种子**（P1-MA3-046 gap，R2.7 deferred enforcement flip）。规则若引用未定义 role ID → fail-closed 抛错。须与 R2.7 role-resource 种子协同。

**既有测试/范例范式**：app 层负向隔离测试模板 `module-finance/.../TestErpOrgIsolation.java`（R1.29，144 行）——`@NopTestConfig(enableActionAuth=FALSE)` + `AppConfig.getConfigProvider().assignConfigValue` 翻转 + 经 `I*Biz.findList` 走 CrudBizModel 管道 + 断言 ON 空/OFF 可见。两层 admin/user role-auth 结构范例：**`/Users/abc/app/nop-entropy/nop-auth/nop-auth-service/src/main/resources/_vfs/nop/auth/auth/nop-auth.data-auth.xml`**（实测存在，565 字节；`<role-auth id="admin" roleIds="nop-admin">` 无 filter + `<role-auth id="default" roleIds="user"><filter><eq name="tenantId" value="${$context.tenantId}"/></filter>` 两层模式）。data-auth.xdef schema 见 `nop-entropy/.../nop/schema/data-auth.xdef`（支持 per-obj `role-decider`、per-role-auth `priority`/`<when>`/`<check>`/`<filter>`）。

剩余差距：4 类规则 0 落地；role ID 未种子；无灰度门控；employee-id 列 user→employee 解析路径未定；无角色侧行级过滤负向测试。

## Goals

- 经 Decision 选定灰度机制（G1 自定义 `IDataAuthChecker` 门控 bean / G2 第二个 config-gated `IQueryTransformer` / G3 per-rule `<when>` 谓词），**默认 OFF** 且所选机制须**可验证保证单组织基线零回归**（G1/G2 天然成立；G3 须特殊 fail-closed 处理）。
- 选定范围内的角色×bizObj 规则填充 `erp-{module}.data-auth.xml` `<objs>`（按真实过滤列：userId 域 createdBy/ownerId 直接用 `$context.user.userId`；employee-id 域 inspectorId/assignedTo 经 user→employee 解析后比较——非工作项的错误列名），采用两层 role-auth（admin 无 filter + 角色带 filter）。
- 确定 role ID（业务员/财务员/质检员/维护员）并与 R2.7 role-resource 种子协调（或声明 role ID 来源）。
- owner doc 对齐：`roles-and-permissions.md §数据权限` 修正列名（含 maintenance assignedTo）+ 标注"行级过滤已落地（灰度默认 OFF）/ successor = 灰度翻转"。
- 新增结构断言测试（每填充 data-auth.xml 的 obj/role-auth/filter 形状 + EL 表达式）+ 至少 1 域负向隔离测试（A 角色查 B 角色数据 → 灰度 ON 时空 / OFF 时可见）。
- arm-index P1-MA6-002 回填 `MR3 done (R3.4)`。

## Non-Goals

- orgId 多公司维度（P1-MA2-093，MR1 已 done，互补不重复）。
- action-level RBAC enforcement flip（P1-MA3-046/R2.7，独立维度，已 deferred）。
- "部门可见"dept 树过滤（依赖 hr 部门树与数据权限管道集成，审计 watch-item，能力缺失 → successor）。
- 灰度翻转至 ON（enforcement flip 须人工批准 + 灰度计划，对齐 R2.7 enable-action-auth 范式；本 plan 落地"声明 + 灰度默认 OFF + 测试"，翻转是 successor）。
- 全 19 域所有 bizObj 规则（本 plan 聚焦 owner doc 声明的 4 类规则对应域/实体；其余登记 successor）。
- 其余 MR3 工作项。

## Task Route

- Type: `implementation-only change`（data-auth.xml 规则 + 可能的 checker/transformer bean + owner doc + 测试；不改 ORM/API 契约，不改平台 IUserContext）
- Owner Docs: `docs/design/roles-and-permissions.md §数据权限`；`docs/architecture/multi-company.md`（与 R1.29 协同引用）；`docs/audits/arm-index.md`
- Skill Selection Basis: 触及 BizModel/数据权限管道后端 → `nop-backend-dev`（IQueryTransformer/IDataAuthChecker/config-gate/跨实体）。触及 auth/permissions 保护区域——owner doc 已描述预期行为（4 类规则），非 AGENTS.md 硬停止；但 enforcement flip 须人工批准（本 plan 默认 OFF 不触发）。测试 → `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（灰度 config 默认 OFF；不依赖外部服务）

## Execution Plan

### Phase 1 - 灰度机制 + scope Decision（Explore|Decision）

Status: completed
Targets: `module-common-service/.../`（checker/transformer/constants）；各域 `model/*.orm.xml`（确认过滤列）
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

- [x] Explore: 核实选定角色侧实体的真实过滤列并分类——**userId 域列**（sales `createdBy`、quality `ownerId`，可直接 `$context.user.userId` 比较）vs **employee-id 域列**（quality `inspectorId`、maintenance `assignedTo`，BIGINT 职员 id，须 user→employee 解析）。grep 各域 orm.xml 确认列名/类型。产出"角色 × bizObj × 过滤列 × 列域(userId/employee-id)"映射表。
  - Skill: none
- [x] Explore: 核实 role ID 来源——`nop_auth_role_resource` 是否有 R2.7 种子可复用，或须本 plan 定义 role ID（业务员/财务员/质检员/维护员的实际 role key）。
  - Skill: none
- [x] Decision: 灰度机制选择。考虑的替代方案与残留风险：
  - **G1（自定义 `IDataAuthChecker` 门控 bean）**：注册项目 `ErpRoleDataAuthChecker`，内部查 config `erp.data-auth.role-row-filter-enabled`（默认 false），false→`getFilter` 返回 null（`AuthHelper.appendFilter` 在 checker 返回 null 时不附加 → 零回归，已实证），true→委托平台 `DefaultDataAuthChecker` 或直接应用规则。最贴近 `enable-action-auth` 范式，与 R1.29 orgId 门控正交组合，**默认 OFF 零回归天然成立**。须确认 `IDataAuthChecker` bean 覆盖点（`GraphQLEngine.setDataAuthChecker`）。**倾向 G1**。
  - **G2（第二个 config-gated `IQueryTransformer`）**：`ErpRoleRowFilterQueryTransformer` 灰度门控。注意 `CrudBizModel:179` 注入单一 `@Named("nopGlobalQueryTransformer")`——R1.29 已占该槽，G2 须将 role+org 组合进同一 bean 或用不同注册机制。避开 data-auth.xml 的"always-on"陷阱，灰度最干净，默认 OFF 零回归成立。
  - **G3（填充 data-auth.xml + per-rule `<when>` 谓词）**：每规则 `<when>${$cfg.get("erp.data-auth.role-row-filter-enabled")}</when>` 自禁用。最贴审计推荐 Option A，但 fail-closed 语义（`getFilter:203-206` 无匹配抛错）使"门控关闭时无规则匹配"易误触发抛错——**默认 OFF 零回归不天然成立**，须保证门控关闭时每用户有无 filter 兜底或不入 obj。
  - 预期裁决须记录选择 + 替代方案 + 残留风险 + 与 R1.29/R2.7 范式一致性 + fail-closed 安全性 + "默认 OFF 零回归"对所选机制的成立性。
  - Skill: none（裁决）；落地用 `nop-backend-dev`
- [x] Decision: employee-id 列（inspectorId/assignedTo）的 user→employee 解析路径——(a) app 层提供 userId→employeeId 解析器（如查 hr 员工表），规则 filter 引用解析结果；(b) 本 plan scope 收窄至 userId 域列（createdBy/ownerId），employee-id 域规则作 successor。须选其一并记录理由 + 残留风险（选 a 增加 app 层依赖；选 b 缩小覆盖留质检/维护 employee-id 盲区）。
  - Skill: none
- [x] Decision: scope——本 plan 覆盖哪些角色×bizObj（owner doc 4 类规则对应的最小完整集：销售员×sales 单据 / 质检员×quality 任务 / 维护员×maintenance 访问 / 财务员全见=no-filter admin-like）。财务员"全见"裁决为 no-filter role-auth（审计 watch-item #2：当前"全见"是巧合非配置，须显式化）。其余域/bizObj 登记 successor。
  - Skill: none

Exit Criteria:

- [x] 角色×bizObj×过滤列×列域映射表产出（真实列名 + userId/employee-id 分类）
- [x] role ID 来源确认（复用 R2.7 种子 or 本 plan 定义）
- [x] 灰度机制 Decision 记录（G1/G2/G3 选择 + 替代方案 + fail-closed 安全性 + 默认 OFF 零回归成立性 + 与 R1.29/R2.7 一致性）
- [x] employee-id 解析路径 Decision 记录（解析器 vs scope 收窄 successor）
- [x] scope Decision 记录（最小完整集 + 财务员全见显式化 + 其余 successor）

#### Phase 1 Decision Record

**Explore-1 角色×bizObj×过滤列×列域映射表**（实测各域 orm.xml，已纠正工作项列名错误）：

| 角色 | 域 | bizObj | 过滤列 | 列域 | 列类型 | 说明 |
|------|----|--------|--------|------|--------|------|
| 业务员 | sales | ErpSalOrder/Quotation/Delivery/Invoice/Receipt/Return | `createdBy` | **userId** | VARCHAR(50) | 平台 auto-stamp 当前 userId；所有实体均有 |
| 质检员 | quality | ErpQaRiskRegister | `ownerId` | **userId** | VARCHAR(36) stdDomain="userId" | "责任人"，唯一 userId 域 quality 列 |
| 质检员 | quality | ErpQaInspection | `inspectorId` | **employee-id** | BIGINT FK→ErpMdEmployee | "检验员(职员)"，须 user→employee 解析 |
| 维护人员 | maintenance | ErpMntVisit/MntRequest | `assignedTo` | **employee-id** | BIGINT | "指派人"，须 user→employee 解析（maintenance 无 ownerId） |
| 财务员 | finance | （无限制列） | — | — | — | owner doc"全见"，无行级过滤 |

关键纠正：工作项/审计所称 `createdById`/`assigneeId` **不存在**；maintenance 真实列是 `assignedTo`（BIGINT employee-id），非 ownerId。

**Explore-2 role ID 来源**：role ID 为**中文业务名**（`业务员`/`管理员`/`质检员`），作为 `roleIds` 直用，与 R2.7 action-auth `<resource roles="财务员">` 静态种子范式一致。运行时 `IUserContext.getRoles()` 来自 `nop_auth_role`/`nop_auth_user_role`（部署数据，仓库无种子）；平台 `LoginServiceImpl` 对所有登录用户自动追加内置 `user` 角色。**裁决：无需新建 role 种子文件**——data-auth.xml 内 `roleIds` 自声明，`user` 兜底（priority 0 无 filter）保证未种子角色不触发 fail-closed。完整 role-resource 种子属 R2.7 successor。

**Decision-1 灰度机制 = G1（自定义 `IDataAuthChecker` 门控 bean）**：已落地 `ErpRoleDataAuthChecker`（注册 bean `nopDataAuthChecker`，非 default 覆盖平台 `DefaultDataAuthChecker`）。机制：`erp.data-auth.role-row-filter-enabled=false`（默认）→ `getFilter` 返回 null / `isPermitted` 返回 true → `AuthHelper.appendFilter` 不附加条件 → **默认 OFF 零回归天然成立**（已实证 appendFilter 在 checker 返回 null 时跳过）。开启时委托 `DefaultDataAuthChecker` 应用 `/nop/main/auth/app.data-auth.xml`。
- 替代 G2（第二个 IQueryTransformer）：R1.29 已占 `nopGlobalQueryTransformer` 单槽，须合并 org+role 进同 bean，复杂度高，弃。
- 替代 G3（per-rule `<when>` 谓词）：fail-closed 语义（`DefaultDataAuthChecker.getFilter:203` 无匹配 role-auth 抛 `ERR_AUTH_NO_DATA_AUTH`）使"门控关闭时无规则匹配"易误触发——默认 OFF 零回归**不天然成立**，弃。
- 二级保险：平台 `nop.auth.enable-data-auth` 默认 false（GraphQLEngine:351 不向 context 注入 checker）——即使本开关误开，data-auth 管道仍不激活。生产 enforcement flip 须同时翻转两者。
- 与 R1.29/R2.7 一致性：同 config-gate 范式（`erp.data-auth.*` 命名对齐 `erp.multi-company.*`/`erp.*`），同"声明 + 默认 OFF + 翻转 successor"路线。

**Decision-2 employee-id 解析 = scope 收窄（option b）**：`ErpMdEmployee` **无 userId 列**（实测 15 列无任何登录用户关联字段）→ user→employee 解析**不可行**（须新增 `ErpMdEmployee.userId` 列，属 ORM 模型变更保护区域，本 plan Non-Goal）。裁决：本 plan 仅落地 userId 域列规则（sales createdBy / quality ownerId）；employee-id 域列（quality inspectorId / maintenance assignedTo）为显式 successor（触发条件 = `ErpMdEmployee` 增 userId 列 + app 层解析器落地）。残留风险：质检/维护 employee-id 盲区，由 successor 覆盖。

**Decision-3 scope（最小完整集）**：
- **sales 业务员 × 6 单据**（ErpSalOrder/Quotation/Delivery/Invoice/Receipt/Return）：`createdBy` userId 域 filter（**已落地，但须修 EL bug**，见下）。
- **quality 质检员 × ErpQaRiskRegister**：`ownerId` userId 域 filter（本 plan 落地）。ErpQaInspection.inspectorId = employee-id successor。
- **finance 财务员"全见"**：裁决为**空 `<objs/>`**（DefaultDataAuthChecker 对未声明 obj 返回 null = 无 filter = 全见，语义正确），**显式化**经 owner doc 标注"finance 无行级过滤=设计决定（财务员全见）"实现，避免为 ~30 实体写 no-op XML 噪音。审计 watch-item #2（"全见"巧合 vs 配置）经 owner doc 显式声明消解。
- **maintenance**：无 userId 域列（assignedTo 为 employee-id），本 plan 无规则，successor。
- 其余域/bizObj：successor（owner doc §数据权限 未声明更多角色×bizObj 行级语义）。

**关键 BUG 修正（Explore 发现）**：sales `erp-sal.data-auth.xml`（前次执行写入）filter 用 `value="${$context.user.userId}"`——**此 EL 表达式无效**。`$context` 全局变量解析为 `IContext`（`ContextProvider.currentContext()`），而 `IContext` **无 `getUser()` 方法**（仅有 `getUserId()`/`getTenantId()`），故 `$context.user` → null，filter 退化为 `eq(createdBy, null)` 匹配零行。该 bug 源自 nop-metadata.data-auth.xml 的错误范式（其结构测试仅文本断言 `contains("$context.user.userId")` 无法捕获）。**正确形式**：`${userContext.userId}`（`DefaultDataAuthChecker.newEvalScope:170` 注入的 scope 变量 `userContext` = `IUserContext`，与 dict 宏 `@biz:userId`→`userContext.userId` 一致）。Phase 2 修正所有规则 EL。

### Phase 2 - 灰度门控 + 规则填充落地

Status: completed
Targets: `module-common-service/.../`（灰度 bean/constants）；选定域 `erp-{module}.data-auth.xml`；可能 role 种子
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 Decision

- [x] Add: 按 Decision 落地灰度门控——config 常量 `erp.data-auth.role-row-filter-enabled`（默认 false，命名对齐 `erp.multi-company.org-isolation-enabled`）+ checker/transformer/when 实现（G1/G2/G3 之一）。
  - Skill: `nop-backend-dev`
- [x] Add: 选定域 `erp-{module}.data-auth.xml` 填充 `<objs>` 规则——两层 role-auth（admin 无 filter + 角色带 `<filter><eq name="<真实列>" value="${userContext.userId}"/></filter>`），按 Phase 1 映射表。app 级 `app.data-auth.xml` 无需改（`GenFromModules` 自动聚合）。
  - Skill: `nop-backend-dev`
- [x] Add: 若 Phase 1 裁决须定义 role ID，补 role 种子（与 R2.7 范式一致），确保规则引用的 role ID 在 `IUserContext.getRoles()` 可解析，避免 fail-closed 误抛。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 灰度门控落地（默认 OFF，单组织基线零回归可验证）
- [x] 选定域 data-auth.xml 规则填充（真实列 + 两层 role-auth + EL 表达式），grep 可验证非空 `<objs>`

#### Phase 2 Implementation Notes

- **灰度门控（G1）已落地**：`ErpRoleDataAuthChecker`（bean `nopDataAuthChecker`）+ `ErpRoleDataAuthConstants.CONFIG_ROLE_ROW_FILTER_ENABLED`。修正：为 `daoProvider` 字段补 `@Inject`（原缺失致委托 delegate 的 entity 级检查 NPE），与 `ErpOrgIsolationQueryTransformer` 范式一致。
- **规则填充**：sales `erp-sal.data-auth.xml` 6 obj（createdBy，**修正 EL bug**：`${$context.user.userId}`→`${userContext.userId}`）；quality `erp-qa.data-auth.xml` 1 obj ErpQaRiskRegister（ownerId，userId 域）。app 聚合 `app.data-auth.xml` 追加 quality extends。finance 维持空 `<objs/>`（全见语义=无 filter，owner doc 显式化）。
- **role ID**：Phase 1 裁决无需新建种子——`roleIds` 用中文业务名（业务员/管理员/质检员），`user` 兜底，与 R2.7 `<resource roles="...">` 静态范式一致。
- 验证：`xmllint` 全部 well-formed；`mvn clean install -DskipTests -pl common-service,sal-service,qa-service,app-erp-all -am` BUILD SUCCESS；`mvn test -pl common-service,sal-service` 148 tests 0 failure。

### Phase 3 - owner doc 对齐 + 结构/隔离测试

Status: completed
Targets: `docs/design/roles-and-permissions.md`；结构断言测试（仿 `TestDataAuthRowLevelScoping`）；负向隔离测试（仿 `TestErpOrgIsolation`）
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [x] Fix: `roles-and-permissions.md §数据权限` 修正过滤列名（createdBy/ownerId[userId 域] + inspectorId/assignedTo[employee-id 域]，非 createdById/assigneeId）+ 标注"行级过滤已落地（灰度默认 OFF，R3.4）/ 翻转 successor"；§设计能力基线 消解"始终附加"矛盾（标注灰度门控）。
  - Skill: none
- [x] Proof: 结构断言测试——解析每填充 `erp-{module}.data-auth.xml` 为 XNode，按真实两层范例（`nop-entropy/.../nop/auth/auth/nop-auth.data-auth.xml` 的 admin 无 filter + default 带 `<filter>` 模式）断言：每 obj 有 admin/财务员无 filter role-auth + 角色带 `<filter><eq name="<真实列>" value="${userContext.userId}"/></filter>`；userId 域列 EL 直接用 `userContext.userId`，employee-id 域列按 Phase 1 解析 Decision。模板参考平台既有结构断言测试 `/Users/abc/app/nop-entropy/nop-metadata/nop-metadata-service/src/test/java/io/nop/metadata/service/TestDataAuthRowLevelScoping.java`（256 行；解析 data-auth.xml 为 XNode + 断言 admin 无 filter + user 带 `eq(col, ${userContext.userId})` filter + roleIds 含 user/admin），app 层负向隔离风格参照 `TestErpOrgIsolation`。无需运行时 enforcement 即可验证规则正确性。
  - Skill: `nop-testing`
- [x] Proof: 至少 1 域负向隔离测试（仿 `TestErpOrgIsolation`）——`assignConfigValue` 翻转灰度 ON + 置角色上下文，A 角色查 B 角色数据 → 断言空；灰度 OFF → 断言可见（回归）。经 `I*Biz.findList` 走 CrudBizModel 管道。
  - Skill: `nop-testing`

Exit Criteria:

- [x] owner doc 对齐（列名修正 + 灰度标注 + 矛盾消解）
- [x] 结构断言测试落地（规则形状 + EL + 列名验证通过）
- [x] 负向隔离测试落地（灰度 ON 隔离 / OFF 回归可证明）

#### Phase 3 Implementation Notes

- **owner doc**：`roles-and-permissions.md §数据权限` 增"行级过滤落地状态"块（列域分类 + EL 说明 + 灰度门控 + successor）；§设计能力基线 消解"始终附加"矛盾（标注双层灰度默认 OFF）。
- **结构断言测试**：`app-erp-all/src/test/.../TestErpDataAuthStructure.java`（4 测试）——用 `DslNodeLoader` 加载聚合 `app.data-auth.xml`（验证 x:extends 合并 sales+quality = 7 obj），断言三层 role-auth 结构 + 正确列名（createdBy/ownerId）+ 正确 EL（`${userContext.userId}`，禁无效 `${$context.user.userId}`）。4 测试全绿。
- **负向隔离测试**：`module-sales/erp-sal-service/src/test/.../TestErpRoleRowFilterIsolation.java`（2 测试）——经 `IErpSalOrderBiz.findList` 走 CrudBizModel 管道，验证灰度 ON 业务员只看自己 createdBy / 管理员全见 / OFF 回归 + admin 不被 user 兜底 shadow。2 测试全绿。
- **关键修正（执行中发现）**：(1) EL bug `${$context.user.userId}`→`${userContext.userId}`；(2) priority shadow bug——getRoleAuth 升序首匹配会使 user(0) 先于角色，改为对齐 nop-auth.data-auth.xml 的同 priority + 声明顺序（admin 首位→角色→user 末位）；(3) createdBy stamp 读 `ContextProvider.userRefNo`（autotest 默认 "autotest-ref"），测试须同步设置 IUserContext + ContextProvider.userRefNo 对齐。
- 验证：qa-service 119 测试 / app-erp-all 15 测试全绿；全量 `mvn clean install -DskipTests`（154 模块）BUILD SUCCESS。

### Phase 4 - arm-index 回填 + 日志

Status: completed
Targets: `docs/audits/arm-index.md` §P1 详细清单；`docs/logs/2026/07-31.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] Add: arm-index P1-MA6-002「修复状态」回填 `MR3 done (R3.4)`，附灰度机制/scope/role ID 协调/employee-id 解析 Decision/dept 树 successor 指针。
  - Skill: none
- [x] Add: 追加 `docs/logs/2026/07-31.md` 条目（R3.4 灰度门控 + 规则填充 + owner doc + 测试）。
  - Skill: none
- [x] Proof: 一致性复核——grep 选定域 data-auth.xml 非空 + arm-index P1-MA6-002 非裸 todo + 灰度默认 OFF（application.yaml 或常量默认值确认）。
  - Skill: none

Exit Criteria:

- [x] arm-index P1-MA6-002 回填，无裸 todo
- [x] 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: needs-revision (task `ses_049f7d523ffeaWx9mPDzBCslVl`) because (1) Phase 3 引用 `TestDataAuthRowLevelScoping.java` 平台测试，iter-1 reviewer 称其不存在——**iter-2 复核推翻此判断**：该文件实存于 `nop-entropy/nop-metadata/nop-metadata-service/.../TestDataAuthRowLevelScoping.java`（256 行，系理想结构断言模板），iter-1 搜索错模块误判。已恢复引用并补全 nop-metadata 完整路径；(2) Current Baseline 称 maintenance 用 `ownerId` **错**——实测 `module-maintenance/.../orm.xml:259,356` 为 `assignedTo`（BIGINT employee-id），且与"getUserId() 即可"自相矛盾（inspectorId/assignedTo 是 employee-id 非 userId）。已修订：列分类为 userId 域（createdBy/ownerId）+ employee-id 域（inspectorId/assignedTo），后者须 user→employee 解析；Phase 1 增 employee-id 解析路径 Decision；Goals/owner-doc/Phase 3 列名全修正；Deferred 增 employee-id successor；"默认 OFF 零回归"措辞绑定所选机制（G1/G2 天然 / G3 须证）。事实核验（19 data-auth.xml 空、0 IDataAuthChecker、R1.29 占 nopGlobalQueryTransformer 槽、DefaultDataAuthChecker 无 enable flag 且 fail-closed、IUserContext 已有 getUserId/getDeptId/getRoles、R1.29 不改平台接口、owner doc 4 规则、nop-auth.data-auth.xml 两层范例实存 565B）全 CONFIRMED。
- Independent draft review iteration 2: accept (task `ses_049e8639dffe9b2Hq7xCl4DOBJ`) — 5 项 resolution check 全 resolved，零 blocking。已采纳非阻塞 polish（恢复 TestDataAuthRowLevelScoping 完整 nop-metadata 路径）。

## Closure Gates

- [x] 范围内行为完成（灰度门控默认 OFF + 选定域规则填充 + owner doc + 结构/隔离测试）
- [x] 相关文档对齐（roles-and-permissions + arm-index + roadmap + 日志）
- [x] 已运行验证（touched 模块 `mvn test` 全绿：qa 119 / app-erp-all 15 / sal 148 / common-service；全量 `mvn clean install -DskipTests` BUILD SUCCESS 154 reactor modules；灰度默认 OFF 下既有测试零回归）
- [x] 无范围内项目降级为 deferred/follow-up（dept 树/全 19 域/enforcement flip/employee-id 为显式 Non-Goal successor，非 in-scope 降级）
- [x] 独立草案审查已完成并记录（Draft Review Record iter1 needs-revision → iter2 accept，2 个 task id）
- [x] 文本一致性已验证：Plan Status completed / 4 Phase 全 completed / 各 Exit Criteria 全 [x] / Closure Gates 全 [x] / 日志条目一致（独立结束审计复核）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### "部门可见" dept 树过滤

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖 hr 域部门树与数据权限管道集成（审计 watch-item #1，能力缺失）。"自己创建"规则用 `createdBy+$userId` 已覆盖 owner doc 主要语义；部门可见是"可配置"增强。
- Successor Required: `yes`（触发条件 = hr 部门树查询路径与数据权限管道集成后，补 dept-tree `<filter>`）

### 灰度翻转至 ON（enforcement flip）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 对齐 R2.7 enable-action-auth 范式——本 plan 落地"声明 + 灰度默认 OFF + 测试"，翻转须人工批准 + 灰度计划（按角色/按域分批）+ 全量回归。默认 OFF 保证单组织基线零回归（**前提：所选灰度机制[G1/G2]默认 OFF 天然零回归；若选 G3 须 Phase 1 证明 fail-closed 安全**）。
- Successor Required: `yes`（触发条件 = role ID 种子经人工审核 + 灰度翻转计划批准后，分域翻转 `erp.data-auth.role-row-filter-enabled=true`）

### employee-id 域列规则（inspectorId/assignedTo，若 Phase 1 裁决 scope 收窄）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 Phase 1 Decision 选 scope 收窄至 userId 域列（createdBy/ownerId），则 quality `inspectorId`/maintenance `assignedTo`（BIGINT employee-id）规则留为 successor——须 app 层 user→employee 解析器。userId 域规则已覆盖"自己创建"主语义。
- Successor Required: `yes`（触发条件 = app 层 userId→employeeId 解析器落地后，补 employee-id 域 `<filter>`）

### 全 19 域所有 bizObj 规则

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本 plan 聚焦 owner doc 声明的 4 类规则对应最小完整集；其余域/bizObj 未在 owner doc §数据权限 明确声明行级过滤语义。
- Successor Required: `yes`（触发条件 = owner doc §数据权限 扩展声明更多角色×bizObj 规则时，按本 plan 模式填充）

### role ID 种子完整化（若 Phase 1 裁决须定义）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本 plan 仅定义 4 类规则引用的 role ID 最小集；完整 role-resource 种子属 R2.7（P1-MA3-046）successor。
- Successor Required: `yes`（触发条件 = R2.7 role-resource 种子完整化时，核对与本 plan role ID 一致）

## Closure

Status Note: EXECUTE 全 4 Phase 完成 + 独立结束审计 PASS（READY TO CLOSE）。闭环。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 task `ses_04927fc35ffe1SaZImgKhlggXF`，general 类型，零先前上下文）
- Verdict: **PASS — READY TO CLOSE**。逐项核实 4 Phase 真实性 + 正确性 + 一致性。
- Evidence:
  - Phase 2：`ErpRoleDataAuthChecker` implements IDataAuthChecker + `@Inject daoProvider` + isEnabled 默认 false（ErpRoleDataAuthChecker.java:30-49）；bean `nopDataAuthChecker` 注册（app-service.beans.xml:24）；sales 6 obj + quality 1 obj 三层 role-auth，filter EL 全为 `${userContext.userId}`（无无效 `${$context.user.userId}`），无显式 priority（erp-sal/erp-qa.data-auth.xml）；app 聚合 x:extends sales+quality（app.data-auth.xml:11-12）；finance 空 `<objs/>`（设计决定）。
  - Phase 3：owner doc 列名修正 + 灰度标注 + "始终附加"矛盾消解（roles-and-permissions.md §数据权限/§设计能力基线/§运行基线）；结构断言 `TestErpDataAuthStructure`（4 测试，DslNodeLoader 验证聚合 7 obj+三层+EL）+ 负向隔离 `TestErpRoleRowFilterIsolation`（2 测试，经 IErpSalOrderBiz.findList 走 CrudBizModel 管道，灰度 ON 业务员只看自己/admin 全见/OFF 回归）全绿。
  - 关键技术裁决确认：(1) EL `${userContext.userId}` 正确（newEvalScope:170 注入 scope 变量 = IUserContext，对照 AuthCoreConstants.VAR_USER_CONTEXT）；(2) 无显式 priority + 声明顺序正确（RoleDataAuthModel.compareTo 升序 + getRoleAuth 首匹配 + 稳定排序保插入序，user 兜底须末位防 shadow——对齐 nop-auth.data-auth.xml 范式）；(3) 测试真实证明隔离（4-way：OFF 零回归 / ON 业务员隔离 / ON admin 全见 / OFF 回归）。
  - 灰度默认 OFF：application.yaml 无 erp.data-auth.*/enable-data-auth 键 → 常量默认 false（零回归）。
  - Non-Goals respected：employee-id 规则/ORM/IUserContext/enforcement flip 均未触（successor）。
  - Phase 4：arm-index P1-MA6-002 `MR3 done (R3.4)` + roadmap R3.4 `done` + 日志条目落地。
- 非阻塞 polish（已采纳）：roles-and-permissions.md §运行基线/§第二批扩展域 "始终生效/启用" 措辞软化引用灰度门控（执行者已修正 line 155/163）。
- Audit Method: 独立子代理逐项 grep/read live repo 核实 Exit Criteria 与 Closure Gates，对照 nop-entropy 平台源码验证 EL/priority 语义。

Follow-up:

- 非阻塞跟进见 Deferred But Adjudicated（employee-id 域规则 / dept 树 / 灰度翻转 enforcement / 全 19 域其余 bizObj / role ID 种子完整化）。
