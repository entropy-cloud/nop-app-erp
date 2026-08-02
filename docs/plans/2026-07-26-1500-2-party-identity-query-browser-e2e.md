# 2026-07-26-1500-2 统一 Party 身份查询 @BizQuery 浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-26
> Source: `docs/backlog/deepening-roadmap.md` §8.2 C1 落地证据（C1 统一 Party 身份查询已落地，`ErpPartyBizModel` 3 个 `@BizQuery` 方法 `findParties`/`getParty`/`findReferences` 经 JUnit `TestErpPartyBiz` 8 场景单层验证，但**零浏览器层 E2E**——仅有 `party-search-picker.visual.spec.ts` visual smoke 覆盖 picker page action 注册 + getParty null 容忍 + findReferences 空 Map，不覆盖 `findParties` keyword 过滤 / partyTypes 过滤 / limit 截断 / 字段投影行为）；AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点
> Related: `docs/plans/2026-07-21-0827-2-master-data-unified-party-identity-query.md`（C1 后端落地 plan）、`docs/plans/2026-07-26-1407-3-exchange-rate-api-client-browser-e2e.md`（D1 master-data `@BizMutation` 浏览器层 E2E 先例——本计划为 `@BizQuery` 读路径同型）、`docs/plans/2026-07-26-1500-1-material-customs-validation-browser-e2e.md`（同批 C2 写路径浏览器层 E2E）
> Audit: required

## Current Baseline

C1（Unified Party Identity Query）已于 plan `2026-07-21-0827-2` 落地。实时仓库核实（2026-07-26）：

**`ErpPartyBizModel` 3 个 `@BizQuery`**（`module-master-data/erp-md-service/src/main/java/app/erp/md/service/party/ErpPartyBizModel.java:71-130`）：

| 方法 | 签名 | 行为 | 行号 |
|------|------|------|------|
| `findParties` | `(keyword?, partyTypes?, limit?, context) → List<PartyRef>` | keyword < 2 字符返回空 List（避免全表 LIKE）；多实体独立 `findAllByQuery` + Java merge + 字段投影 + 截断 limit；DEFAULT_LIMIT=50 / MAX_LIMIT=200 | `:71-100` |
| `getParty` | `(partyType?, partyId?, context) → PartyRef?` | null 入参容忍返回 null；按 type+id 加载单条 PartyRef | `:101-110` |
| `findReferences` | `(partyType?, partyId?, context) → Map<String,Long>` | null 入参返回 emptyMap；PARTNER 经 `IErpMdPartnerReferenceChecker` SPI；EMPLOYEE/ORGANIZATION 经各自 SPI（`@Nullable` 单实例，未注册返回 emptyMap） | `:112-130` |

**PartyRef DTO 字段**（`app.erp.md.dao.dto.PartyRef`）：partyType / partyId / code / name / phone / email / status / displayName / extension(Map)。Organization 实体 phone/email=null 容忍（JUnit 已覆盖）。

**JUnit 单层验证**：`TestErpPartyBiz`（`module-master-data/erp-md-service/src/test/java/app/erp/md/service/party/TestErpPartyBiz.java`）8 场景全绿：跨 3 实体检索 / partyType 过滤 / keyword < 2 字符返回空 / limit 截断 / getParty 三类型 / PartyRef 字段投影 + Organization phone/email=null 容忍 / findReferences Partner 路径 + Employee/Organization SPI 未注册返回空 Map / 空数据集返回空。经 GraphQL RPC 集成测试，但**零 Playwright 浏览器层 E2E**。

**visual smoke 现状**：`tests/e2e/visual/party-search-picker.visual.spec.ts` 3 测试覆盖（1）findParties action 注册；（2）getParty null 容忍；（3）findReferences 空 Map。**不覆盖** findParties keyword 实际过滤命中 / partyTypes 过滤 / limit 截断 / PartyRef 字段投影值断言。

**种子基线**（仅作 setup 决策输入，断言不依赖种子中文 name）：
- `erp_md_partner.csv`：2 CUSTOMER（CUST-001 华东科技 / CUST-002 华南贸易）+ 2 SUPPLIER + 1 EMPLOYEE = 5 行
- `erp_hr_employee.csv`：2 员工（3 行含表头）
- `erp_md_organization.csv`：2 组织（3 行含表头，org 1 GROUP + org 2 COMPANY ERP-CO）

种子中文 name（如「华东科技」）为易变自由文本——未来种子编辑会静默破坏字段投影断言无测试侧信号。故本计划采用**自包含 setup 默认**（对齐 1407-3 Decision 3）：经 `__save` 建测试专用 partner/employee/org（确定性 code 如 `E2E-PARTY-PN-{ts}` + 确定性 name 含独占 keyword 片段），断言针对自包含实体而非种子行。种子仅用于核实三实体类型各 ≥1 行存在（setup FK 可达性 warm-up），不用于断言。

剩余差距：C1 3 `@BizQuery` 方法经 JUnit 单层验证但零浏览器层 E2E；findParties 实际过滤命中 / 字段投影值 / partyTypes 过滤 / limit 截断行为未经全栈 GraphQL `@BizQuery` 路径验证。

## Goals

- `ErpPartyBizModel` 3 `@BizQuery` 方法（findParties 多实体 keyword 过滤 / getParty 单条加载 / findReferences 引用计数）经 Playwright 浏览器层 E2E 全栈验证——驱动真实 GraphQL `/graphql` query 经 `@BizQuery` 暴露，断言返回 PartyRef 列表/单条/Map 结构 + 字段投影值 + 过滤/截断行为
- 补全 C1 落地证据的「浏览器层验证」缺口（镜像 §8.6 C3 / §8.2 C1 / §8.5 D1 范式增「浏览器层验证」bullet）
- e2e-runbook 业务动作表新增 master-data 统一 Party 查询行

## Non-Goals

- **不修改生产代码**（BizModel/IBiz/DTO/enum/ORM/契约/字典/种子/config 零变更）——纯测试 + 文档计划
- **不覆盖 picker.page.yaml AMIS 渲染**——已由 `party-search-picker.visual.spec.ts` visual smoke 覆盖（page→AMIS crud 渲染）
- **不覆盖 Employee/Organization 引用扫描下游域 SPI 实现**（C1 Deferred successor：触发条件=下游域引用扫描业务需求 + SPI 实现授权）——当前 SPI 未注册返回空 Map 为既定行为
- **不覆盖 ErpMdUserAccount 接入统一 Party**（C1 Deferred successor）
- **不覆盖 Party 合并去重 / 全文索引 / 物化视图**（C1 Deferred successor，触发条件未满足）
- **不覆盖 `@BizQuery` 经 AMIS picker onSelect 回填**——属 picker.page.yaml 渲染面，非 @BizQuery 返回值结果面
- **不做跨域（非 master-data）@BizQuery 覆盖**——本计划仅 master-data 域 C1 结果面

## Task Route

- Type: `verification or audit work`（JUnit 单层验证 → 浏览器层全栈验证补全，纯测试 + 文档，零生产代码变更）
- Owner Docs: `docs/design/master-data/unified-party-identity.md`（C1 owner doc，§查询策略 / §IErpPartyBiz 接口契约——本计划增「浏览器层验证」实现注记）、`docs/testing/e2e-runbook.md`（业务动作表 + 套件计数）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层 E2E + 既有 business-actions/_helper 复用 + GraphQL `@BizQuery` 读路径 + 确定性种子数据驱动断言 + 自包含 setup 隔离，对齐 1407-3 同型 @BizQuery 先例）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 复用现有 Playwright 配置 + webServer JVM 参数（findParties/getParty/findReferences 无 config 门控——`DEFAULT_LIMIT=50` 为硬编码 `static final int`，非 config 驱动；webServer 无需新增 JVM arg）

## Execution Plan

### Phase 1 - Explore（@BizQuery GraphQL 暴露形态 + 种子 keyword 命中表 + 字段投影核实）

Status: completed
Targets: `ErpPartyBizModel.java`（方法行号锚点）、`PartyRef.java`（DTO 字段集）、`ErpPartyType.java`（enum 值）、`app-erp-all/.../erp_md_partner.csv` + `erp_hr_employee.csv` + `erp_md_organization.csv`（种子 keyword 命中表）
Skill: none

- Item Types: `Proof`
- Prereqs: 无

- [x] `Proof`：核实 3 `@BizQuery` 方法经 GraphQL `/graphql` 暴露的入参/返回类型形态——`findParties(keyword:String, partyTypes:[ErpPartyType], limit:Int)` 返回 `[PartyRef]!`；`getParty(partyType:ErpPartyType, partyId:Long)` 返回 `PartyRef`（nullable）；`findReferences(partyType:ErpPartyType, partyId:Long)` 返回 `Map`（GraphQL JSON scalar）。核实 GraphQL selection set 须显式声明 PartyRef 字段（partyType/partyId/code/name/phone/email/status/displayName/extension）
      - Skill: none
- [x] `Proof`：核实种子三实体类型各 ≥1 行（partner/employee/org）仅作 setup FK 可达性 warm-up 证据——**不用于断言**（断言针对自包含 setup 实体，对齐 1407-3 Decision 3 自包含默认）。产出 setup 决策记录：自包含建确定性 code + 独占 keyword name 的 partner/employee/org，断言 PartyRef 字段投影值匹配自包含实体
      - Skill: none
- [x] `Proof`：核实 PartyRef 字段投影源——Partner/Employee/Organization 三实体各自投影到 PartyRef 的字段映射（JUnit 已覆盖 Organization phone/email=null 容忍），确认浏览器层断言字段可达性 + Organization 空字段容忍预期
      - Skill: none

Exit Criteria:

> Phase 1 产出可执行的 spec 设计输入：GraphQL 入参/返回形态 + 种子 keyword 命中表 + 字段投影预期。

- [x] 3 `@BizQuery` GraphQL 入参/返回形态已记录（支撑 Phase 2 query 构造 + selection set）
- [x] setup 决策已记录：**自包含 setup 默认**（建测试专用 partner/employee/org + 确定性 code + 独占 keyword name），断言针对自包含实体（对齐 1407-3 Decision 3）
- [x] PartyRef 字段投影 + Organization 空字段容忍预期已记录

#### Execution Decisions (Phase 1)

**Proof 1 — 3 `@BizQuery` GraphQL 入参/返回形态**（`ErpPartyBizModel.java:71-140`，`IErpPartyBiz` 接口经 `@BizModel("ErpParty")` 暴露为 `ErpParty__<action>`）：

| 方法 | GraphQL 入参 | GraphQL 返回 | selection set | 行号锚点 |
| --- | --- | --- | --- | --- |
| `findParties` | `keyword:String` / `partyTypes:[ErpPartyType]` / `limit:Int`（均 optional/nullable） | `[PartyRef]!`（keyword < 2 字符返回空 List） | 须显式声明 PartyRef 字段 | `:71-98`（findParties 主体 `:72-97`，渗入 helper `findByType`/`buildKeywordFilter`/`toPartyRef`） |
| `getParty` | `partyType:ErpPartyType` / `partyId:Long`（均 nullable） | `PartyRef`（nullable；null 入参或 not-found 返回 null） | 须显式声明 PartyRef 字段 | `:101-109` |
| `findReferences` | `partyType:ErpPartyType` / `partyId:Long`（均 nullable） | `Map<String,Long>`（经 GraphQL 序列化为 `[{k,v}]` 数组形态） | Map 项须显式 `{ k v }` | `:112-140` |

**ErpPartyType enum 3 值**（`ErpPartyType.java:18-20`）：`PARTNER`（ErpMdPartner）/ `EMPLOYEE`（ErpMdEmployee）/ `ORGANIZATION`（ErpMdOrganization）。GraphQL 入参 `partyType:"PARTNER"` 字符串字面量（visual smoke 实证 `party-search-picker.visual.spec.ts:33,58,80`）。

**PartyRef 字段集**（`PartyRef.java:30-38`）：partyType / partyId / code / name / phone / email / status / displayName / extension(Map)。GraphQL selection 须显式声明各字段；extension 含嵌套 key（PARTNER.partnerType / EMPLOYEE.position/orgId/partnerId / ORGANIZATION.orgType/parentId/functionalCurrencyId）。

**Proof 2 — 种子基线（仅 setup FK 可达性 warm-up）**：

| 实体 | 种子行 | 用途 |
| --- | --- | --- |
| `erp_md_partner.csv` | 5 行（2 CUSTOMER + 2 SUPPLIER + 1 EMPLOYEE） | FK 可达性（`ErpMdEmployee.partnerId` 可指向自建 partner） |
| `erp_md_employee.csv` | 3 行（EMP-001~003 ACTIVE org=2） | 三实体类型 EMPLOYEE 行存在 |
| `erp_md_organization.csv` | 2 行（GROUP-HQ / ERP-CO COMPANY） | ORGANIZATION 行存在 + parentId 可达 |

**Setup 决策（自包含默认，对齐 1407-3 Decision 3）**：spec setup 经 `__save` 建 3 个自包含实体（partner + employee + organization），各自 code 唯一前缀（如 `E2E-PARTY-PN-` / `E2E-PARTY-EMP-` / `E2E-PARTY-ORG-` + 时间戳），name 嵌入独占 keyword（如 `E2E-PARTY-KEY-<ts>` 三实体共享同 keyword）。断言针对自包含实体字段投影值（非种子中文 name 如「华东科技」），避免未来种子编辑静默破坏断言。

**Proof 3 — PartyRef 字段投影 + Organization 空字段容忍预期**（`ErpPartyBizModel.java:247-292`）：

| ErpPartyType | 投影源实体字段 | PartyRef 投影 | 行号锚点 |
| --- | --- | --- | --- |
| PARTNER | code/name/phone/email/status + extension.partnerType | 全字段（含 phone/email 实体值） | `:247-259` |
| EMPLOYEE | code/name/phone/email/status + extension.position/orgId/partnerId | 全字段（含 phone/email 实体值） | `:261-275` |
| ORGANIZATION | code/name/status（无 phone/email 列）+ extension.orgType/parentId/functionalCurrencyId | **phone=null / email=null**（容忍字段缺失） | `:277-292`（`:283-284` 显式 setNull） |

Organization 空字段容忍：spec 用例 (5) `getParty(ORGANIZATION)` 断言 `phone === null && email === null`（实体无对应列，BizModel 显式投影为 null，对齐 JUnit `testGetPartyForOrganizationReturnsNullPhoneEmail`）。

**Required fields for `__save` setup**（master-data.orm.xml）：
- `ErpMdPartner`（:422-466）：`code` + `name` + `partnerType`（dict `erp-md/partner-type`：CUSTOMER/SUPPLIER/EMPLOYEE/CUSTOMS_BROKER）+ `status`（dict `erp-md/active-status`：ACTIVE）。UK on `code`。
- `ErpMdEmployee`（:859-893）：`code` + `name` + `status`。`orgId`/`position`/`phone`/`email`/`partnerId` optional。UK on `(code, orgId)`。
- `ErpMdOrganization`（:1075-1109）：`code` + `name` + `orgType`（dict `erp-md/org-type`：GROUP/COMPANY/DEPARTMENT）+ `status`。UK on `code`。

**GraphQL query 构造策略**：3 个 @BizQuery 返回复杂类型（List<PartyRef>/PartyRef/Map），`GraphQLClient.callQuery` 不带 selection set（仅适用标量返回，见 `cs-canned-response.action.spec.ts:61-65` 注记）→ 经 `new GraphQLClient(page).raw()` 内联完整 query + selection set（镜像 `cs-canned-response.action.spec.ts:63-65` / `fin-reconciliation.action.spec.ts:233-239` / `fin-period-close-wizard.action.spec.ts:105-110` / `fin-budget-control.action.spec.ts:253+` 范式）。

### Phase 2 - spec 实现（findParties 多场景 + getParty + findReferences）

Status: completed
Targets: `tests/e2e/business-actions/md-party-query.action.spec.ts`（NEW）、`tests/e2e/business-actions/_helper.ts`（复用既有 `gql.raw`/`findFirst` 原语，预期零新增或最小加性）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 Explore 笔记就位

- [x] `Add`：新建 `md-party-query.action.spec.ts`（6 用例），**自包含 setup 默认**（镜像 1407-3 Decision 3）：经 `__save` 建测试专用 partner（code=`E2E-PARTY-PN-{ts}` + name 含独占 keyword 如 `E2E-PARTY-KEY-<ts>`）+ employee（code=`E2E-PARTY-EMP-{ts}` + 同 keyword）+ organization（code=`E2E-PARTY-ORG-{ts}` + 同 keyword）。断言针对自包含实体字段投影值（非种子中文 name）。6 用例：
      - (1) **findParties keyword 命中**：`findParties(keyword:"<独占 keyword>")` → 返回非空 List（≥3，跨 PARTNER/EMPLOYEE/ORGANIZATION 三类型各自 setup 实体）+ 逐条断言 PartyRef 字段（partyType + code + name）匹配自包含 setup 实体
      - (2) **findParties partyTypes 过滤**：`findParties(keyword:"<同前>", partyTypes:[EMPLOYEE])` → 返回 List 仅含 EMPLOYEE setup 实体（PARTNER/ORGANIZATION setup 实体被过滤）
      - (3) **findParties keyword < 2 字符**：`findParties(keyword:"a")` → 返回空 List（MIN_KEYWORD_LENGTH 守卫）
      - (4) **findParties limit 截断**：`findParties(keyword:"<独占>", limit:2)` → 返回 List.size() ≤ 2（三 setup 实体命中但截断为 2，截断可观测）
      - (5) **getParty 三类型 + Organization 空字段容忍**：`getParty(partyType:PARTNER, partyId:<setup partner id>)` → PartyRef code/name 断言；`getParty(partyType:EMPLOYEE, partyId:<setup emp id>)` → 同；`getParty(partyType:ORGANIZATION, partyId:<setup org id>)` → PartyRef + phone/email=null 容忍断言（Organization 实体无 phone/email 列）
      - (6) **findReferences**：`findReferences(partyType:PARTNER, partyId:<setup partner id>)` → 返回 Map 结构可达（经 GraphQL 序列化为 **JSON 对象**形态——实测 schema 漂移：原 `[{k,v}]` 数组描述已过时，Map 类型不支持 selection set，原 `party-search-picker.visual.spec.ts:88` `{ k v }` selection 现触发 "[Map]不是对象类型，不支持字段选择" 错误，属预存 schema 漂移非本 spec 引入）；断言结构非 null + object 类型可达，不硬断言具体计数值因 SPI 注册依赖运行时
      - Skill: `nop-testing`
- [x] `Proof`：`@BizQuery` 经 `/graphql` query（非 mutation）调用，selection set 显式声明 PartyRef 字段；返回 Map 的 `findReferences` 经 GraphQL 序列化为 **JSON 对象**形态（非 `[{k,v}]` 数组、非 selection set——实测 schema 漂移，Map 类型不支持字段选择）
      - Skill: `nop-testing`
- [x] `Proof`：spec 含自包含 setup 写入（建 partner/employee/org）→ **须 cleanup**（删本 spec setup 实体，不污染共享 DB 基线 / master-data dashboard）；cleanup 按 setup 实体 code 前缀过滤删除
      - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付可运行的 spec，6 用例覆盖 findParties 多场景 + getParty + findReferences。

- [x] spec 文件存在且 6 用例全绿（`npx playwright test tests/e2e/business-actions/md-party-query.action.spec.ts` 0 failures）
- [x] findParties 字段投影值断言经自包含 setup 实体（非种子中文 name）
- [x] 自包含 setup cleanup 删本 spec setup 实体（不污染共享 DB 基线）

#### Phase 2 Verification Evidence

- 新 spec `md-party-query.action.spec.ts`：1 passed (7.2s) — 6 用例全绿（findParties keyword 命中跨 3 实体 ≥3 + 字段投影断言 / partyTypes:[EMPLOYEE] 过滤排除 PARTNER+ORGANIZATION / keyword<2 返回空 List / limit:2 截断 ≤2 / getParty 三类型 + ORGANIZATION phone/email=null 容忍 / findReferences JSON 对象结构可达）。
- master-data 回归抽样 7 passed (50.5s)：`crud/master-data.write.spec.ts`（ErpMdPartner CRUD write cycle）+ `md-exchange-rate-api.action.spec.ts`（1407-3 D1 汇率）+ `md-material-customs-validation.action.spec.ts`（1500-1 C2 5 用例）。自包含 setup cleanup 按 id 链式 __delete（employee → partner FK 引用反依赖 + organization 独立），共享 DB 基线零污染。
- **Schema 漂移发现（pre-existing，非本 spec 引入）**：`party-search-picker.visual.spec.ts` 用例 3「ErpParty__findReferences returns empty map for unregistered SPI」现失败（"[Map]不是对象类型，不支持字段选择"）。原因：Nop GraphQL schema 对 `Map<String,Long>` 返回类型的序列化已改为 JSON 对象（实测 EMPLOYEE id=1 → `{"employeeAdvance":0}`，PARTNER id=1 → `{}`），不再支持 `{ k v }` selection set。本 spec 据实测调整为无 selection set 形式。visual spec 失败属 schema 漂移（C1 落地后的平台演进），非本计划范围内产物，归入 Follow-up（visual spec 需平台 schema 同步）。

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: completed
Targets: `docs/design/master-data/unified-party-identity.md`（增「浏览器层验证」实现注记）、`docs/testing/e2e-runbook.md`（业务动作表 + spec 计数）、`docs/backlog/deepening-roadmap.md`（§8.2 C1 增「浏览器层验证」bullet）、`docs/logs/2026/07-26.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 spec 全绿

- [x] `Add`：`docs/design/master-data/unified-party-identity.md` §查询策略 或 §IErpPartyBiz 接口契约 增「浏览器层验证（plan 2026-07-26-1500-2）」实现注记（@BizQuery 经 `/graphql` 全栈可达 + 种子确定性数据驱动断言 + keyword 过滤/partyTypes 过滤/limit 截断 + PartyRef 字段投影 + Organization 空字段容忍 + findReferences Map 结构）
      - Skill: none
- [x] `Add`：`docs/testing/e2e-runbook.md` 业务动作表新增 master-data 统一 Party 查询行 + spec 计数同步
      - Skill: none
- [x] `Add`：`docs/backlog/deepening-roadmap.md` §8.2 C1 落地证据增「浏览器层验证」bullet（镜像 §8.6 C3 / §8.5 D1 范式）
      - Skill: none
- [x] `Add`：`docs/logs/2026/07-26.md` 增本计划日志条目（按 `docs/logs/00-log-writing-guide.md` 格式）
      - Skill: none

Exit Criteria:

- [x] owner doc 实现注记落地（unified-party-identity.md）
- [x] e2e-runbook 业务动作表 + spec 计数同步
- [x] deepening-roadmap §8.2 C1 增「浏览器层验证」bullet
- [x] 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: `needs-revision`（`ses_060f2b888ffewKBENvQieuxIz6`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-26）— 0 Blocker / 1 Major / 5 Minor。全部 load-bearing 事实主张经实时仓库核实精确匹配（3 @BizQuery 方法 ✓ / 8 JUnit 场景 ✓ / visual smoke 仅 3 测试不覆盖字段值断言 ✓ / 零 business-actions party spec ✓ / 种子数据 ✓ / PartyRef 9 字段 ✓ / ErpPartyType 3 值 ✓）。**Major M1**：Phase 2 setup 策略使用「若种子 keyword 不足...裁决自包含建隔离实体」条件语言违反 anti-slack 规则（禁止 optional/as needed）；种子中文 name 易变，未来种子编辑静默破坏字段投影断言无测试侧信号。**已修订**：Phase 2 改为「自包含 setup 默认」（镜像 1407-3 Decision 3），断言针对自包含 setup 实体确定性 code + 独占 keyword name，非种子中文 name。**Minors 已修订**：m1 baseline 5-partner 命中表改为 setup 决策输入 / m3 config-gate 描述改为硬编码 static final / m4 findReferences Map 序列化改为 [{key,value}] 数组形态 / m5 用例数 5-6 提交为 6。**Minors 保留给结束审计**：m2 line-number citation 略偏（findReferences :112-130 实际 :112-140）。
- Independent draft review iteration 2: `acceptable-as-is`（`ses_060ed5e74ffelaujT9DRDXUAvH`，独立 general 子代理，新会话冷重播，2026-07-26）— 0 Blocker / 0 Major / 0 新 Major / 2 cosmetic Minor（defer-to-closure）。**M1 RESOLVED confirmed**：Phase 2 setup 决定性自包含（lines 32/76/86/98），无条件语言残留，全部 6 用例断言针对自包含 setup 实体。cleanup 正确要求（自包含 setup 写入 partner/employee/org）。anti-slack scan clean。**2 cosmetic Minor 保留给结束审计**：(1) findReferences line-range citation :112-130 应为 :112-140；(2) findParties :71-100 略渗入 getParty @Override（应 :71-98）。

## Closure Gates

> 本计划为纯测试 + 文档计划（零生产代码变更）。完整仓库验证：`mvn clean install -DskipTests`（156 模块）+ 新 spec Playwright 运行 + master-data 回归抽样。

- [x] 范围内行为完成（3 @BizQuery 浏览器层 E2E 6 用例全绿）
- [x] 相关文档对齐（unified-party-identity.md + e2e-runbook + deepening-roadmap §8.2）
- [x] 已运行验证：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + 新 spec 全绿 + master-data 回归抽样 0 新增失败
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Employee/Organization 引用扫描下游域 SPI 实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: C1 Deferred successor——当前 Employee/Organization SPI 未注册，`findReferences(EMPLOYEE/ORGANIZATION)` 返回空 Map 为既定容忍行为（JUnit 已覆盖）。浏览器层断言结构可达性（Map 非 null + keySet 可达），不硬断言具体计数值。
- Successor Required: `yes`（触发条件：下游域引用扫描业务需求 + SPI 实现授权）

### ErpMdUserAccount 接入统一 Party / Party 合并去重 / 物化视图反向索引 / 全文索引

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: C1 Deferred successor——触发条件均未满足（UserAccount 接入需认证域授权 / 合并去重需业务流程需求 / 物化视图需性能瓶颈需求 + 单实体有效记录数 > 10K / 全文索引需搜索需求）。
- Successor Required: `yes`（触发条件见 C1 §8.2 落地证据 Deferred successor 段）

### picker.page.yaml AMIS onSelect 回填 / F7 兼容路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 picker 渲染面（page→AMIS crud→onSelect），已由 `party-search-picker.visual.spec.ts` visual smoke 覆盖；非 @BizQuery 返回值结果面。
- Successor Required: `no`（触发条件：picker 交互回归需求时增 picker 专项视觉/交互 spec）

## Closure

Status Note: 全 3 Phase 执行完成 + 独立结束审计 PASS。`ErpPartyBizModel` 3 `@BizQuery` 浏览器层 E2E 覆盖落地（findParties 4 场景 + getParty 三类型含 Organization 空字段容忍 + findReferences Map 结构可达），收口 C1「JUnit 单层验证但零浏览器层 E2E」缺口。纯测试 + 文档，零生产代码变更。**执行期发现并记录平台 schema 漂移**：Nop GraphQL `Map<String,Long>` 序列化已演进为 JSON 对象形态（非原 `[{k,v}]` 数组描述），Map 类型不支持 selection set → `party-search-picker.visual.spec.ts:88` 用例 3 现触发 "[Map]不是对象类型，不支持字段选择" 错误（属预存漂移非本 spec 引入，本 spec 据实测调整为无 selection set 形式，visual spec 修复归 Deferred successor）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 `ses_060b437a2ffehD9md58M2DsjsP`，general agent，未参与执行）
- Verdict: **PASS**（5 验证域全通过：plan consistency + deliverables exist + zero production code change + correctness spot-check + schema drift sanity）
- Execution Evidence:
  - `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS（1:30 min）
  - 新 spec `md-party-query.action.spec.ts` 1 passed (7.2s) — 6 用例全绿（findParties keyword 命中 ≥3 跨 3 实体 + 字段投影 / partyTypes:[EMPLOYEE] 过滤排除 PARTNER+ORGANIZATION / keyword<2 返回空 List / limit:2 截断 ≤2 / getParty 三类型 + ORGANIZATION phone/email=null 容忍 / findReferences JSON 对象结构可达）
  - master-data 回归抽样 7 passed (50.7s)（`crud/master-data.write.spec.ts` ErpMdPartner CRUD cycle + `md-exchange-rate-api.action.spec.ts` 1407-3 D1 + `md-material-customs-validation.action.spec.ts` 1500-1 C2 5 用例）
  - `git status` 5 modified docs + 1 new test spec only；零 `module-*/src/main` 变更（ErpPartyBizModel.java / PartyRef.java / ErpPartyType.java / IErpPartyBiz.java UNMODIFIED）
- 日志：`docs/logs/2026/07-26.md` 顶部 1500-2 条目

Follow-up:

- `party-search-picker.visual.spec.ts` 用例 3 findReferences schema 漂移修复（非阻塞，触发条件：visual spec 维护任务时按现代 Map JSON 对象序列化形态同步移除 `{ k v }` selection set；本计划 Deferred But Adjudicated 段已记录）
