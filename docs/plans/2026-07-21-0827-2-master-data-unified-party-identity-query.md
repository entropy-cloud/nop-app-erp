# 2026-07-21-0827-2-master-data-unified-party-identity-query C1 — Master Data Unified Party Identity Query（统一 Party 身份查询）

> Plan Status: active
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/deepening-roadmap.md` §Milestone C §C1（line 65/86 — Abstract Party concept + reverse-index/materialized view + `IErpPartyBiz` query interface）
> Related: `docs/design/master-data/README.md` §核心业务对象（line 27-43：Partner + Employee + Organization 分离实体）；`docs/design/master-data/README.md` §跨域协作（line 97-108：IErpMd*Biz 接口范式）；`module-master-data/erp-md-web/.../_vfs/erp/md/pages/dashboard/main.page.yaml`（**首例手写非实体 page.yaml** —— `ErpMdDashboardBizModel` 不绑定单一实体，本计划 `PartySearch.picker.page.yaml` 与之同构）；`docs/plans/2026-07-20-1020-2-f7-non-status-visibleon-and-master-data-interactions.md`（F7 主数据交互范式，countReferences + `IErpMdPartnerReferenceChecker` SPI 模式 + 删除引用预览 —— 本计划 findReferences 复用并扩展该 SPI）；`docs/backlog/deepening-roadmap.md` §C2（line 66 — Cross-Border Trade Extensions，依赖本计划 Party 身份基础）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-21，对 master-data ORM 实体集 + master-data/README.md + 既有 IErpMd*Biz 接口范式 + picker.page.yaml 范式扫描）：

### 已落地的 Party 相关实体（分离结构）

| 实体 | 表名 | 业务语义 | 关键字段 | 与其他 Party 实体关联 |
|------|------|----------|----------|---------------------|
| `ErpMdPartner` | `erp_md_partner` | 客户与供应商统一主数据（外部 Party） | code/name/partnerType/taxNo/phone/email/status | 无 |
| `ErpMdEmployee` | `erp_md_employee` | 职员主数据（内部人员 Party） | code/name/orgId/position/phone/email/status/**partnerId** | `partnerId` 可选 FK → `ErpMdPartner`（"对应内部往来单位"语义：员工也可能是公司内部交易方） |
| `ErpMdOrganization` | `erp_md_organization` | 组织主数据（内部组织 Party） | code/name/parentId(tree)/orgType/functionalCurrencyId/status | 自关联 `parentId` 树形；**无 phone/email 字段**（仅组织级属性） |

**关键发现**：`ErpMdEmployee.partnerId` 已存在软关联（员工 → 内部 Partner），但 Partner 不反向持有 Employee；Organization 不直接关联 Partner 或 Employee（经 Employee.orgId 间接关联）。三实体公共字段为 `code/name/status` + 标准审计字段；Partner/Employee 另含 `phone/email`；Organization 缺这两个字段 —— 统一查询层需容忍字段异构（Organization 的 phone/email 投影为 null）。

### 已落地的查询接口范式

- `IErpMdPartnerBiz` / `IErpMdEmployeeBiz` / `IErpMdOrganizationBiz` 在 `module-master-data/erp-md-dao` 的 `app.erp.md.biz` 包暴露（继承 `IOrmEntityService` + 域专用 query 方法）—— 全 23 个 `IErpMd*Biz.java` 均位于此包，**非** `app.erp.md.dao.api`。
- 跨域调用经 `@Inject` `IErpMd*Biz`（如 `IErpMdPartnerBiz` 在 purchase/sales/finance 域被引用），不做 ORM 层跨工程 `refEntityName`（per README line 99 + domain-design-guidelines.md）。
- **既有非实体 BizModel 范式**：`module-master-data/erp-md-service/.../dashboard/ErpMdDashboardBizModel.java`（`@BizModel("ErpMdDashboard")` at line 32，**不** 继承 `CrudBizModel<?>`，`@Inject IDaoProvider daoProvider;` at line 42-43 + `@Inject IOrmTemplate ormTemplate;` at line 44-45 —— 暴露 `@BizQuery getDashboardKpi` 等方法）—— 平台支持非实体 BizModel + GraphQL 暴露。**但 Dashboard 未暴露 `IErpXxxBiz` 接口**（仅 UI 入口，无跨域消费者）；本计划 `IErpPartyBiz` 接口是首例"非实体 + 跨域 `@Inject`"组合，Phase 0 Decision 显式裁决此约定。
- **既有手写非实体 page.yaml 范式**：`module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/dashboard/main.page.yaml`（手写 AMIS page.yaml，绑定 `ErpMdDashboardBizModel`）—— 全仓库 80+ `picker.page.yaml` 均为 3 行 codegen stub；本计划 `party-search/main.picker.page.yaml` 是**首例手写 picker.page.yaml**（结构上与 dashboard/main.page.yaml 同构，因 Party 非实体无法走 codegen）。
- **既有 F7 SPI 跨域引用扫描范式**（关键 —— Path A 真实复用）：`IErpMdPartnerReferenceChecker`（`module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdPartnerReferenceChecker.java`）由 master-data 声明端口，purchase/sales/inventory/hr 等下游域注册实现（避免 master-data 反向依赖业务域 per Javadoc line 5-9）；签名 `Map<String, Long> countReferences(Long partnerId)` —— **无 IServiceContext 参数，返回 Map<String, Long> 非 long，无 listReferences 方法**；`ErpMdPartnerBizModel:34` 通过 `@Inject(required=false) protected IErpMdPartnerReferenceChecker partnerReferenceChecker;`（**单实例 nullable 注入，非 `List<>`**）+ line 77-81 `countReferences(id, context)` 调用 `partnerReferenceChecker.countReferences(id)`。本计划 `findReferences` 跨 Employee/Organization 引用扫描**严格复用此模式 Path A**（新 SPI 端口与既有同构；不做 contract upgrade；下游域实现归 Deferred）。
- 既有 F4 P1 picker 范式（plan `2026-07-19-1818-1`）使用 `<grid id="pick-list">` + `<form id="pick-query">` delta + codegen（**非** 手写 picker.page.yaml）；与本计划手写范式不同，仅作 AMIS 组件层级参考。

### 待深化差距（C1 范围）

| 差距 | 现状 | C1 目标 |
|------|------|---------|
| **跨实体 Party 检索** | 用户需先知道目标是 Partner / Employee / Organization 再进入对应模块搜索 | 单一入口：按 code/name/phone/email 关键字跨 3 实体联合检索 |
| **统一 Party 抽象** | 三实体各自独立，无抽象基类 / DTO 统一表达 | `PartyRef` DTO（partyType + partyId + code + name + phone + email + 实体特定属性） |
| **反向索引 / 物化视图** | 无 | 按 Explore 裁决：union SQL 查询（最小侵入）vs 物化视图（性能）vs 反向索引表（同步开销） |
| **`IErpPartyBiz` 查询接口** | 无 | 跨域统一入口，业务域注入即可跨实体查询 |
| **Party picker 组件** | 每个实体独立 picker | 联合 picker（选任意 Party 后回填 partyType + partyId + 显示字段） |

### 关键风险/缺口

- **统一 Party 抽象边界**：哪些实体属 Party？Partner + Employee + Organization 是核心三实体；是否扩展到 `ErpMdUserAccount`（平台账户）？默认 **Non-Goal**（`ErpMdUserAccount` 属 nop-auth 平台域，不在 master-data 范围）。
- **关键字检索的 SQL 性能**：union 3 表 + `LIKE '%keyword%'` 在大数据量下慢；ErpMdPartner EstRows 中等（< 10K），Employee EstRows 小（< 1K），Organization EstRows 极小（< 100），合计 < 11K，union 查询性能可接受。如未来 Partner 增长 > 100K 需物化视图或反向索引 successor。
- **跨实体引用语义**：业务单据引用 Party 时（如凭证的 partnerId 维度），需明确：`PartyRef.partyType` 区分实体类型，业务侧需 partyType-aware 处理（如凭证的 partnerId 仍仅接受 `ErpMdPartner`，不接受 Employee/Organization 作为辅助维度）。C1 仅提供查询 + picker，不改业务侧既有 FK 语义。
- **picker 回填的显示字段**：选 Party 后回填 partyType + partyId + code + name + 可选 phone/email；不同实体显示字段略不同（Organization 无 phone；Employee 有 position；Partner 有 partnerType）—— picker 列设计需统一 + 容忍差异。

## Goals

1. **新建 owner doc**：`docs/design/master-data/unified-party-identity.md`（Party 抽象边界 + 查询策略 + PartyRef DTO 契约 + picker 范式 + 反模式自检表）。
2. **实现 `IErpPartyBiz` 查询接口 + 实现**：`module-master-data/erp-md-service` 内 `ErpPartyBizModel` 暴露 `@BizQuery findParties(keyword, partyTypes, limit)` → 返回 `List<PartyRef>`；union SQL 实现（经 `@SqlLibMapper` 或 `IOrmTemplate` 跨实体查询，按 Explore 裁决）。
3. **`PartyRef` DTO 落地**：统一返回结构（partyType + partyId + code + name + phone + email + status + 实体特定扩展字段 Map），跨域调用方不需感知具体实体类型。
4. **联合 picker 页面**：`module-master-data/erp-md-web/.../pages/PartySearch/PartySearch.picker.page.yaml`（**NEW**），支持关键字搜索 + partyType 多选过滤 + 选中后 onEvent.setValue 回填字段。
5. **试点接入 1 个业务场景**：F7 主数据交互的"删除引用预览"扩展为跨 Party 类型查询（`ErpMdPartnerBizModel.countReferences` 兼容路径保留 + 新增 `IErpPartyBiz.findReferences` 跨 Partner/Employee/Organization 计数；返回 `Map<String, Long>` 与既有 SPI 同构 Path A）；或 finance 凭证 partner 维度 picker 试点（待 Phase 0 Explore 裁决）。
6. **回归测试**：扩展 master-data service 单元测试（union 查询正确性 + partyType 过滤 + 空结果处理 + 关键字 SQL 注入防护）。

## Non-Goals

- **C2 跨境贸易字段扩展**（`ErpMdMaterial` 加字段 + 新建 `ErpMdMaterialCustoms`）—— C2 是独立 successor plan，依赖 C1 的 Party 抽象基础但 ORM 改动归 C2。
- **ErpMdUserAccount 接入统一 Party**—— `ErpMdUserAccount` 属 nop-auth 平台域（非 master-data），其账户语义与业务 Party 不同（登录账户 vs 业务方）；接入需跨工程协调，归 Deferred。
- **既有业务单据 FK 改造**—— 业务单据（PurOrder.partnerId / SalOrder.partnerId / VoucherLine.partnerId 等）仍引用 `ErpMdPartner` 专用 FK，不改为通用 `partyId + partyType`。C1 仅提供查询 + picker，业务侧 FK 语义保持。
- **物化视图 / 反向索引表**—— C1 默认走 union SQL（最小侵入 + 性能可接受）；如生产数据量增长触发阈值（Partner > 100K 或查询 > 500ms），独立 successor plan 落地物化视图。
- **Party 合并 / 去重**—— 同一自然人在 Partner + Employee 都有记录时的合并语义（Golden Record）属 MDM（Master Data Management）范畴，非 C1 范围。
- **关键字模糊检索的全文索引**—— 默认 `LIKE '%keyword%'`；全文索引（MySQL FULLTEXT / ElasticSearch）归 Deferred。
- **partyType 字典化**—— 三实体类型（PARTNER/EMPLOYEE/ORGANIZATION）作为枚举常量（`ErpPartyType` Java enum）即可，无需字典 yaml。

## Task Route

- Type: `app-layer design change`（新增 owner doc + Party 抽象 + 跨实体查询契约）+ `implementation-only change`（query interface + DTO + picker 页面）
- Owner Docs:
  - `docs/backlog/deepening-roadmap.md` §C1（line 65/86）
  - `docs/design/master-data/unified-party-identity.md`（**NEW** — 本计划 Phase 0 落地）
  - `docs/design/master-data/README.md` §核心业务对象（line 27-43）+ §跨域协作（line 97-108）
  - `docs/design/master-data/ui-patterns.md`（picker 范式）
  - `docs/design/domain-design-guidelines.md`（跨域调用规则）
  - `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`（CrudBizModel 范式）
  - `../nop-entropy/docs-for-ai/04-reference/safe-api-reference.md`（IOrmTemplate / @SqlLibMapper 跨实体查询安全 API）
- Skill Selection Basis: 加载 `nop-backend-dev`（BizModel + IBiz + 跨实体 IOrmTemplate + DTO + xbiz action）；加载 `nop-frontend-dev`（picker.page.yaml + AMIS picker 范式）；不加载 `nop-testing`（既有 master-data service 测试范式直接复用）。最终：`nop-backend-dev` + `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- 无新 config / 端口 / 密钥依赖
- 查询限制配置：`erp-md.party-search.max-results`（默认 `50`，防止大数据量关键字查询拖垮 DB）

## Execution Plan

### Phase 0 — Explore + Owner Doc + 关键 Decision

Status: planned
Targets: `docs/design/master-data/unified-party-identity.md`（**NEW**）+ plan 内 Decision 记录
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision`
- Prereqs: deepening-roadmap.md C1 todo + master-data README 已有 Party 概念基础

- [ ] `Explore` (a)：跨实体查询的 SQL 实现方式（union vs IOrmTemplate 多次查询 vs @SqlLibMapper）。
  - 核实范围：`../nop-entropy/docs-for-ai/04-reference/safe-api-reference.md` 的 `IOrmTemplate` / `IDaoProvider` 跨实体查询范式；既有项目内 `@SqlLibMapper` 使用样例（grep 全仓）；Nop 平台是否提供 native union 查询支持。
  - 输出：3 方案权衡表（性能 / 代码复杂度 / 维护成本 / 平台范式一致性），入 owner doc §3。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (b)：3 实体的字段对齐表（统一查询投影基础）。
  - 核实范围：`module-master-data/model/app-erp-master-data.orm.xml` 的 Partner/Employee/Organization 字段集对齐（公共字段：id/code/name/status/phone/email + 各自独有字段：Partner.partnerType / Employee.position+orgId+partnerId / Organization.orgType+parentId）。
  - 输出：字段投影表（PartyRef DTO 字段 ← 各实体字段映射），入 owner doc §2。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (c)：试点接入场景候选评估。
  - 候选 1：F7 主数据 `ErpMdPartner.countReferences` 扩展为 `IErpPartyBiz.findReferences`（跨 Partner/Employee/Organization 引用预览）。
  - 候选 2：finance 凭证 partner 维度 picker（VoucherLine.partnerId）改为联合 picker，允许跨实体选 Party。
  - 候选 3：通知收件箱（notify inbox）的收件人选择改为联合 picker（已知 notify 收件人目前按 user 选，可能不适用 Party 概念）。
  - 输出：试点场景选择裁决（候选 1 / 2 / 3 / 其他），含理由 + 验证路径。
  - Skill: `nop-backend-dev`
- [ ] `Explore` (d)：跨域引用扫描的 SPI 模式扩展可行性（**独立审查 ses_07dea19d8ffe Major 2 反馈**）。
  - 核实范围：`module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdPartnerReferenceChecker.java`（既有 SPI 端口）+ 其在 purchase/sales/inventory/hr 下游域的实现样例（grep 全仓实现类）；评估扩展为 `IErpMdEmployeeReferenceChecker` + `IErpMdOrganizationReferenceChecker` 的可行性（是否需要新增端口 / 实现工作量 / 跨域注册机制）。
  - 输出：3 SPI 端口扩展方案 —— (a) 新增 2 独立 SPI（Employee + Organization）；(b) 抽象为统一 `IErpPartyReferenceChecker`（按 partyType dispatch）；(c) 复用既有 `IErpMdPartnerReferenceChecker` 接口但语义扩展（不推荐，破坏单一职责）。裁决 + 各方案工作量评估入 owner doc §4。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：基于 Explore (a)~(d)，确定 C1 实现方式。
  - **查询实现方式**（裁决依据 Explore a）：优先 **`IOrmTemplate` + 3 次查询后 Java 内 merge + limit**（最小侵入 + 平台范式一致，参考 `ErpMdDashboardBizModel.java:31-32` 既有 `@Inject IDaoProvider + IOrmTemplate` 范式）；若性能测试 > 500ms 则降级 `@SqlLibMapper` + native SQL union。理由：3 实体字段异构（Organization 无 phone/email），union SQL 投影复杂；3 次查询 + Java merge 可读性高 + 易于扩展新 Party 实体。
  - **PartyRef DTO 字段集**（裁决依据 Explore b）：`partyType(String)` + `partyId(Long)` + `code(String)` + `name(String)` + `phone(String)` + `email(String)` + `status(String)` + `displayName(String)`（拼接 `code + ' - ' + name`）+ `extension(Map<String, Object>)`（实体特定字段：Partner.partnerType / Employee.position+orgId / Organization.orgType）。**容忍 Organization 无 phone/email** —— 投影为 null，picker UI 不展示该列；与 Explore (b) 字段表对齐。
  - **试点场景**（裁决依据 Explore c）：**候选 1（F7 findReferences 扩展）**。理由：F7 已落地 `IErpMdPartnerReferenceChecker` SPI 范式（plan 2026-07-20-1020-2），扩展到跨 Party 类型是自然延伸；候选 2（凭证 partner 维度）需改 finance 保护区域 FK 语义，违反 Non-Goal；候选 3（notify 收件人）partyType 不匹配（user 非 Party）。
  - **跨域引用扫描 SPI 模式扩展**（裁决依据 Explore d）：选 **方案 (a) 新增 2 独立 SPI**（`IErpMdEmployeeReferenceChecker` + `IErpMdOrganizationReferenceChecker`），**严格同构既有 `IErpMdPartnerReferenceChecker` Path A**（独立审查 ses_07ddf93acffe Blocker 1 反馈）。理由：(i) 单一职责清晰；(ii) 与既有 `IErpMdPartnerReferenceChecker` 同构，下游域注册实现的工作量模式化重复；(iii) 方案 (b) 统一 `IErpPartyReferenceChecker` 按 partyType dispatch 增加 dispatch 逻辑复杂度且需大改既有 Partner SPI。**裁决**：方案 (a) Path A —— 新 SPI 端口签名 `Map<String, Long> countReferences(Long partyId)`（无 IServiceContext，无 listReferences，与既有 Partner SPI 完全同构）；`IErpPartyBiz.findReferences` 返回 `Map<String, Long>`（**非** rich DTO `List<PartyReference>` —— 与既有保持一致）；`ErpPartyBizModel` 注入方式 `@Inject(required=false) IErpMdEmployeeReferenceChecker employeeReferenceChecker;`（单实例 nullable，非 `List<>`）；下游域实现注册归 Deferred successor（仅试点 Employee/Organization 引用扫描机制；具体下游域实现按业务需求驱动落地）。
  - **关键字检索范围**：code/name/phone/email 4 字段 OR 模糊匹配（`LIKE '%keyword%'`，Organization 仅匹配 code/name）；keyword < 2 字符时返回空（避免全表扫描）；keyword SQL 注入防护经 Nop 平台标准 `QueryBean` + 参数化查询。
  - **接口约定**（独立审查 Major 3）：本计划 `IErpPartyBiz` 接口（dao 模块）+ `ErpPartyBizModel` 实现（service）是首例"非实体 + 跨域 `@Inject`"组合。既有 `ErpMdDashboardBizModel` 是非实体但无接口（仅 UI 入口）；本计划引入接口约定因 Party 有真实跨域消费者（finance/purchase/sales 可能 `@Inject IErpPartyBiz`）。**裁决**：当且仅当非实体 BizModel 有跨工程 `@Inject` 消费者时才暴露 `IErp*Biz` 接口；纯 UI 入口（如 Dashboard）保持无接口。owner doc §4 显式记录此约定。
  - **选择依据**：查询方式优先平台范式一致性 + 可读性 > 微优化（3 实体 EstRows < 11K）；PartyRef DTO 字段集最小覆盖 picker 显示需求 + 容器字段 extension 兼容未来扩展 + 容忍 Organization 字段缺失；试点场景最小风险（F7 既有 SPI 范式扩展）；SPI 模式扩展走方案 (a) 与既有同构。
  - Skill: none
- [ ] `Add`：owner doc `docs/design/master-data/unified-party-identity.md`（NEW）
  - 8 节完整文档：§1 目的与范围（C1 vs C2 vs MDM 边界）/ §2 Party 抽象边界（3 实体对齐表，**显式标注 Organization 无 phone/email** + PartyRef DTO 字段映射 + Non-Goal 实体清单）/ §3 查询策略（IOrmTemplate 3 查询 + Java merge + 降级路径）/ §4 IErpPartyBiz 接口契约 + **非实体 BizModel 接口暴露约定**（Dashboard vs Party 对比）+ SPI 端口扩展（方案 (a) 2 新 SPI）/ §5 联合 picker 范式（**首例手写 picker.page.yaml** + 与 dashboard/main.page.yaml 同构 + onEvent.setValue 回填）/ §6 试点场景实施记录（F7 findReferences 扩展 + 3 SPI 收集机制）/ §7 性能与扩展（EstRows 阈值 + 物化视图 successor 触发）/ §8 反模式自检表（包括"在 master-data 直接 import 业务域实体做引用扫描 → 应改 SPI 端口"）。
  - Skill: none

Exit Criteria:

- [ ] 4 个 Explore 结论已记录（含 SPI 模式扩展 (d)）；对应 Decision 已落地
- [ ] owner doc `docs/design/master-data/unified-party-identity.md` 落地（8 节完整，含非实体接口约定 + SPI 扩展方案 (a)）
- [ ] 查询方式 + PartyRef DTO 字段集（容忍 Organization 缺字段）+ 试点场景 + SPI 扩展方案 + 接口约定 5 项关键 Decision 在 owner doc §3-§4 明确

### Phase 1 — IErpPartyBiz 接口 + PartyRef DTO + 实现（**DAO 模块**）

Status: planned
Targets: `module-master-data/erp-md-dao/src/main/java/app/erp/md/biz/IErpPartyBiz.java`（**NEW**，与 23 个 `IErpMd*Biz.java` 同包）+ `module-master-data/erp-md-dao/src/main/java/app/erp/md/dao/dto/PartyRef.java`（**NEW** DTO）+ `module-master-data/erp-md-dao/src/main/java/app/erp/md/dao/dto/ErpPartyType.java`（**NEW** enum）+ `module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdEmployeeReferenceChecker.java`（**NEW** SPI 端口）+ `module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdOrganizationReferenceChecker.java`（**NEW** SPI 端口）+ `module-master-data/erp-md-service/.../party/ErpPartyBizModel.java`（**NEW**）
Skill: `nop-backend-dev`

- Item Types: `Add-heavy`
- Prereqs: Phase 0 完成

**层级裁决**（独立审查 ses_07dea19d8ffe Blocker 1+2）：DTO + enum + 接口全部入 `module-master-data/erp-md-dao`（非 service）；跨工程消费者（purchase/sales/finance）只需依赖 erp-md-dao 即可 `@Inject IErpPartyBiz` + 使用 `PartyRef`，不被迫依赖 erp-md-service。与既有 `PriceValidationResult.java` + `ResolvedPrice.java` 在 `dao/dto/` 的先例对齐。

- [ ] `Add`：`ErpPartyType` enum
  - 路径：`module-master-data/erp-md-dao/src/main/java/app/erp/md/dao/dto/ErpPartyType.java`（与既有 `PriceValidationResult`/`ResolvedPrice` 同目录）
  - 值：`PARTNER` / `EMPLOYEE` / `ORGANIZATION`；每值携带 `entityName` + `displayName` 元数据。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`PartyRef` DTO（**仅 PartyRef —— PartyReference 取消**）
  - 路径：`module-master-data/erp-md-dao/src/main/java/app/erp/md/dao/dto/PartyRef.java`
  - 字段（Phase 0 Decision 裁定）：partyType/partyId/code/name/phone/email/status/displayName/extension(Map)。
  - **取消 `PartyReference` rich DTO**（独立审查 ses_07ddf93acffe Blocker 1 + Major 3 反馈 Path A）：既有 `IErpMdPartnerReferenceChecker.countReferences(Long)` 返回 `Map<String, Long>` 非 rich DTO；为严格复用既有 SPI 模式，`IErpPartyBiz.findReferences` 也返回 `Map<String, Long>`（key=引用域名 + value=引用计数）；如未来需 rich 引用详情（billType + billCode + billId），独立 successor plan 升级 SPI contract（Path B，触发：业务需求驱动）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpPartyBiz` 接口
  - 路径：`module-master-data/erp-md-dao/src/main/java/app/erp/md/biz/IErpPartyBiz.java`（与既有 `IErpMdPartnerBiz.java` 等 23 个 IErpMd*Biz **同包** —— 独立审查 ses_07dea19d8ffe Blocker 1 核实 `app.erp.md.dao.api` 包不存在）
  - 方法签名：
    - `List<PartyRef> findParties(String keyword, Set<ErpPartyType> partyTypes, int limit)` —— 关键字跨实体检索；partyTypes=null 表示查所有 3 类；limit 默认经配置项控制。
    - `PartyRef getParty(ErpPartyType partyType, Long partyId)` —— 单点查询。
    - `Map<String, Long> findReferences(ErpPartyType partyType, Long partyId)` —— 跨实体引用计数预览（**返回类型与既有 `IErpMdPartnerReferenceChecker.countReferences` 一致** Path A；非 rich DTO）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpMdEmployeeReferenceChecker` + `IErpMdOrganizationReferenceChecker` SPI 端口
  - 路径：`module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdEmployeeReferenceChecker.java` + `IErpMdOrganizationReferenceChecker.java`（与既有 `IErpMdPartnerReferenceChecker.java` 同目录）
  - 接口签名（**严格复用既有 `IErpMdPartnerReferenceChecker` 模式 Path A** —— 独立审查 ses_07ddf93acffe Blocker 1 反馈，**不** contract upgrade）：`Map<String, Long> countReferences(Long partyId)` —— 无 IServiceContext 参数，返回 `Map<String, Long>`（key=引用域名 + value=引用计数），无 listReferences 方法。两新 SPI 与既有 Partner SPI 完全同构。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpPartyBizModel` 实现
  - 路径：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/party/ErpPartyBizModel.java`
  - 实现（Phase 0 Decision：IOrmTemplate 3 查询 + Java merge）：
    - `@BizModel("ErpParty")`（不继承 `CrudBizModel<?>`，参考 `ErpMdDashboardBizModel`）。
    - `@Inject IDaoProvider daoProvider` + `@Inject IOrmTemplate ormTemplate`（参考 `ErpMdDashboardBizModel:31-32`）。
    - `findParties` 实现：keyword < 2 字符返回空 List；否则循环 3 实体（或按 partyTypes 参数筛选），各调 `daoProvider().daoFor(EntityClass).findPage(QueryBean newQuery)` 取 limit 行，Java 内合并为 `List<PartyRef>` + 截断到 limit。**Organization 投影 phone/email = null**。
    - 字段映射：Partner/Employee/Organization → PartyRef 的 8 字段投影（per Phase 0 Explore b 字段表）；extension Map 装入实体特定字段。
    - displayName 拼接：`code + " - " + name`。
    - `getParty` 实现：按 partyType switch 调对应 Dao 取单条 + 投影 PartyRef。
  - `@BizQuery` 注解所有 query 方法（GraphQL 暴露）；空结果返回空 List 不抛异常。
  - Skill: `nop-backend-dev`
- [ ] `Add`：错误码（如有）
  - 路径：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/ErpMdErrors.java`（既有文件追加）
  - 错误码：`ERP_MD_PARTY_NOT_FOUND`（getParty 找不到时抛）；keyword 太短不抛异常（返回空）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `IErpPartyBiz`（`app.erp.md.biz` 包）+ `PartyRef` + `ErpPartyType`（均在 `app.erp.md.dao.dto` 包）3 类落地（**`PartyReference` 取消** Path A）
- [ ] `IErpMdEmployeeReferenceChecker` + `IErpMdOrganizationReferenceChecker` SPI 端口在 `app.erp.md.spi` 包落地（与既有 Partner SPI 严格同构：`Map<String, Long> countReferences(Long)`）
- [ ] `ErpPartyBizModel`（service 模块）落地，`findParties` 跨 3 实体检索 + 字段投影 + Organization 缺字段容忍 + limit 截断实现
- [ ] GraphQL `ErpParty__findParties` + `ErpParty__getParty` 经 `@BizQuery` 暴露

### Phase 2 — 试点接入 F7 findReferences 扩展（SPI 模式）+ 联合 picker

Status: planned
Targets: `IErpPartyBiz.findReferences` 实现（经 SPI 收集）+ F7 ErpMdPartner.countReferences 兼容路径 + `PartySearch.picker.page.yaml`
Skill: `nop-backend-dev` + `nop-frontend-dev`

- Item Types: `Add-heavy | Decision | Proof`
- Prereqs: Phase 1 完成

- [ ] `Add`：`IErpPartyBiz.findReferences` 实现（经 SPI 收集跨域引用，**Path A 严格复用既有模式**）
  - 实现路径：`ErpPartyBizModel.findReferences(ErpPartyType partyType, Long partyId)` 返回 `Map<String, Long>`；内部按 partyType dispatch：
    - PARTNER → `@Inject(required=false) protected IErpMdPartnerReferenceChecker partnerReferenceChecker;`（既有，F7 已落地，单实例 nullable per `ErpMdPartnerBizModel:34`）—— 调 `partnerReferenceChecker != null ? partnerReferenceChecker.countReferences(partyId) : Collections.emptyMap()`。
    - EMPLOYEE → `@Inject(required=false) protected IErpMdEmployeeReferenceChecker employeeReferenceChecker;`（Phase 1 新增 SPI 端口，单实例 nullable）—— 同上调用模式。
    - ORGANIZATION → `@Inject(required=false) protected IErpMdOrganizationReferenceChecker orgReferenceChecker;`（Phase 1 新增 SPI 端口，单实例 nullable）—— 同上。
  - 各 SPI 实现的注册由下游域自行落地（如 purchase 实现一个 `PurEmployeeReferenceChecker` 检查 PurOrder 的 buyerId 等字段引用）—— **本计划仅落地 SPI 端口 + ErpPartyBizModel 收集机制 + partnerReferenceChecker 接入**；employeeReferenceChecker / orgReferenceChecker 的下游域实现归 Deferred（按业务需求驱动）。
  - **架构约束**（独立审查 ses_07dea19d8ffe Major 2 + ses_07ddf93acffe Blocker 1）：master-data 是基础域，**禁止反向依赖** purchase/sales/inventory/hr 等（per `IErpMdPartnerReferenceChecker.java:5-9` Javadoc）；SPI 模式 Path A 是唯一合规路径。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：orgId 引用语义（独立审查反馈：埋在 item 内的 Decision 提升为兄弟项 per Rule 9）
  - 裁决：orgId 作为审计维度（每条业务单据都有），纳入引用扫描会产生海量噪声；**orgId 不纳入 Organization 的 findReferences**（仅扫描业务语义明确的引用，如 Warehouse.organizationId / Department.organizationId 等通过 `IErpMdOrganizationReferenceChecker` 实现自决）。
  - Skill: none
- [ ] `Add`：F7 ErpMdPartner.countReferences 改造（兼容路径）
  - 改造方式：保留 `ErpMdPartnerBizModel.countReferences` 既有签名（不破坏 F7 落地）；新增 `IErpPartyBiz.findReferences(ErpPartyType.PARTNER, partnerId)` 作为扩展入口；前端 F7 删除引用预览 dialog 增加 "显示其他 Party 类型引用" 链接（可选展开）。
  - 兼容性：既有 F7 删除引用预览行为不变（仅 Partner 引用），跨 Party 类型引用是新增功能。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`PartySearch.picker.page.yaml`（**NEW** —— 首例手写 picker.page.yaml）
  - 路径：`module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/party-search/main.picker.page.yaml`（路径采用既有非实体页面 lowercase 风格 `dashboard/main.page.yaml`，独立审查 ses_07dea19d8ffe Minor 4 反馈）
  - 结构（**参考 `module-master-data/erp-md-web/.../_vfs/erp/md/pages/dashboard/main.page.yaml` 既有手写非实体 page.yaml 范式**，独立审查 ses_07dea19d8ffe Blocker 3 反馈 —— **不参考 F4 P1 finance voucher ErpMdSubject picker**，因后者走 codegen delta 路径非手写 page.yaml）：
    - AMIS `crud` + `picker` mode；
    - asideFilter / filterForm：关键字 input-text + partyType 多选 tag（PARTNER/EMPLOYEE/ORGANIZATION，默认全选）；
    - pick-list grid：partyType / code / name / phone / email / status 6 列（Organization 行 phone/email 显示空）；
    - pick-query GraphQL：`@query:ErpParty__findParties(keyword=$keyword, partyTypes=$partyTypes, limit=50)`；
    - onSelect 回填：onEvent.setValue 触发父表单字段填充（partyType + partyId + displayName + 可选 phone/email）。
  - **首例模式登记**：owner doc §5 显式记录"PartySearch.picker.page.yaml 是全仓库首例手写 picker.page.yaml"，触发条件 = "BizModel 非实体 + 需要 AMIS picker 交互"。
  - Skill: `nop-frontend-dev`
- [ ] `Add`：menu / 路由注册（picker 不需独立菜单项，picker 经父页面引用即可）
  - 验证：picker 可经 `/erp/md/pages/party-search/main.picker.page.yaml` 路径被其他 view.xml 引用。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`：单元测试 + visual smoke
  - 单元测试 `TestErpPartyBiz`（**NEW**）：覆盖（a）关键字跨 3 实体检索 / (b) partyType 过滤 / (c) keyword < 2 字符返回空 / (d) limit 截断 / (e) getParty 三类型 / (f) PartyRef 字段投影正确性（含 Organization phone/email=null 容忍）/ (g) findReferences Partner 路径经既有 partnerCheckers 收集（Employee/Organization SPI 端口存在但下游实现未注册时返回空 List 不抛异常）。
  - visual smoke：`tests/e2e/visual/party-search-picker.visual.spec.ts`（**NEW**）—— DOM 渲染断言（picker 含关键字输入 + partyType tag + pick-list grid 至少 1 行种子数据 + onSelect 回填字段可见）。
  - 验证命令：`mvn test -pl module-master-data/erp-md-service -Dtest=TestErpPartyBiz` + `npx playwright test tests/e2e/visual/party-search-picker.visual.spec.ts`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `IErpPartyBiz.findReferences` Partner 路径接入既有 partnerCheckers；Employee/Organization SPI 端口存在 + ErpPartyBizModel 收集机制就绪（下游域实现归 Deferred）
- [ ] orgId 引用语义 Decision 落地（不纳入 Organization 引用扫描）
- [ ] F7 ErpMdPartner.countReferences 既有行为不破坏；新增跨 Party 类型引用预览可选展开
- [ ] `party-search/main.picker.page.yaml` 完整（首例手写 picker.page.yaml + 与 dashboard/main.page.yaml 同构 + Organization 字段缺失容忍）
- [ ] 单元测试 7 场景 + visual smoke 通过

### Phase 3 — 文档对齐 + 回归 + roadmap 更新

Status: planned
Targets: `docs/design/master-data/unified-party-identity.md` §6 试点记录 + `docs/design/master-data/README.md` Party 概念段落回链 + roadmap C1 状态更新
Skill: `nop-backend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 2 完成

- [ ] `Add`：owner doc §6 试点场景实施记录补齐
  - 内容：F7 findReferences 扩展实施细节 + 数据流图 + 跨实体引用扫描清单（Partner 引用经既有 partnerCheckers 收集；Employee/Organization 引用 SPI 端口就绪但下游实现归 Deferred）+ 性能数据（关键字查询响应时间 + 引用扫描响应时间）。
  - Skill: none
- [ ] `Add`：`docs/design/master-data/README.md` 回链
  - 在 §核心业务对象 表后新增段落"统一 Party 抽象"（指向 `unified-party-identity.md`）+ §跨域协作 表补一行 `IErpPartyBiz`（跨实体查询，跨域 `@Inject` 入口）。
  - Skill: none
- [ ] `Add`：`docs/backlog/deepening-roadmap.md` C1 状态更新
  - `todo → done`；记录落地证据（plan + owner doc + IErpPartyBiz + PartyRef + 试点 + 测试基线）。
  - Skill: none
- [ ] `Proof`：全仓库回归
  - 验证命令：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test -pl module-master-data/erp-md-service`（含 TestErpPartyBiz + 既有 md service 测试全绿）+ `npx playwright test tests/e2e/visual/party-search-picker.visual.spec.ts`（UI smoke 通过）。
  - Skill: none

Exit Criteria:

- [ ] owner doc §6 试点记录完整
- [ ] `master-data/README.md` 回链落地
- [ ] `deepening-roadmap.md` C1 状态 `done` + 落地证据记录
- [ ] 全仓库回归全绿

## Draft Review Record

- Independent draft review iteration 2: `needs revision` (`ses_07ddf93acffe47e0v8jm5Ooo0n`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-21) — 1 BLOCKER + 3 MAJORS + 4 MINORS。iter-1 全部 3 BLOCKERS 经核实**全部 genuine 修订落地**（IErpPartyBiz 包路径 app.erp.md.biz / DTO + enum 层级 dao/dto/ / F4 P1 picker 引用替换为 dashboard/main.page.yaml）。**iter-2 新发现 BLOCKER**：B1 SPI 模式错误描述 —— 计划原述"`@Inject List<IErpMdPartnerReferenceChecker>` + `long countReferences(Long, IServiceContext)` + `List<ErpMdReferenceInfo> listReferences`"与既有真实代码不符（`ErpMdPartnerBizModel:34` 是 `@Inject(required=false) protected IErpMdPartnerReferenceChecker partnerReferenceChecker;` 单实例 nullable 非 List；既有 SPI 签名是 `Map<String, Long> countReferences(Long)` 无 IServiceContext 无 listReferences；`ErpMdReferenceInfo` 类型 grep 0 命中不存在）；如按原计划实施 Phase 2 编译失败 + "F7 已落地"架构理由为伪。MAJORS：M1 "25 个 IErpMd*Biz" 应为 23（`ls | grep -c`）；M2 `ErpMdDashboardBizModel:31-32` 行号错，实际 42-45；M3 `PartyReference` rich DTO 与既有 SPI `Map<String, Long>` 不一致 —— 是 contract upgrade 非 extension。MINORS：m1 4 类 vs 6 类计数 drift；m2 ErpMdPartner.countReferences 应为 ErpMdPartnerBizModel.countReferences；m3 e2e harness 检查；m4 README 行号 anchor。**iter-2 修订全部落地**：(B1) Phase 0 Decision 改为"方案 (a) Path A 严格复用既有 SPI"——新 SPI 端口签名 `Map<String, Long> countReferences(Long)`（无 IServiceContext 无 listReferences）与既有 Partner SPI 完全同构；Phase 1 SPI 接口签名重写；Phase 2 ErpPartyBizModel 注入方式改 `@Inject(required=false)` 单实例 nullable（非 List<>）；删除 `ErpMdReferenceInfo` 引用；(M1) "25 个" 改 "23 个"（Current Baseline line 25 + Phase 1 Targets line 138 + line 156 三处同步）；(M2) Dashboard 行号 31-32 改 42-45（`@BizModel` line 32 + `@Inject IDaoProvider` line 42-43 + `@Inject IOrmTemplate` line 44-45 全部精确）；(M3) `PartyReference` rich DTO 取消 —— `IErpPartyBiz.findReferences` 返回 `Map<String, Long>` 与既有 SPI 同构；如未来需 rich 引用详情归 Path B successor（触发：业务需求驱动）；MINORS m1-m4 非阻塞。
- Independent draft review iteration 3: `accept` (`ses_07dd7c15effeKIlf5jDAF6dcNh`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-21) — 0 BLOCKER + 0 MAJOR + 0 MINOR。iter-2 全部 1 BLOCKER + 3 MAJORS 经实时仓库逐行核实**全部 genuine 修订落地**：(B1) SPI Path A 严格同构 —— 新 SPI 端口 `Map<String, Long> countReferences(Long)` 与既有 `IErpMdPartnerReferenceChecker` 完全一致（无 IServiceContext + 无 listReferences）；`@Inject(required=false)` 单实例 nullable（功能等价 `@Nullable`）；`IErpPartyBiz.findReferences` 返回 `Map<String, Long>`；`PartyReference` 取消 + 无 `ErpMdReferenceInfo` 引用；(M1) "23 个" 计数经 `ls | grep -c` fresh 核实为真；(M2) Dashboard 行号 `@BizModel` line 32 + `@Inject IDaoProvider` line 42-43 + `@Inject IOrmTemplate` line 44-45 经 fresh 核实精确匹配；(M3) `PartyReference` rich DTO 取消，归 Path B successor。R1-R14 + anti-slack + template 全 PASS。**共识达成 → `Plan Status: active`**。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [ ] 范围内行为完成（3 Phase 全部 `[x]`）
- [ ] 相关文档对齐（unified-party-identity.md NEW + master-data/README.md 回链 + roadmap C1 done）
- [ ] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test -pl module-master-data/erp-md-service`（含 TestErpPartyBiz 全绿）+ `npx playwright test tests/e2e/visual/party-search-picker.visual.spec.ts`（UI smoke 通过）
- [ ] 无范围内项目降级为 deferred/follow-up（ErpMdUserAccount 接入 / 物化视图 / Party 合并去重 / 全文索引 / C2 跨境贸易 是合法 Deferred，已在 §Deferred But Adjudicated 登记，不属此条）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 + 2 + 3 accept）
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Employee/Organization 引用扫描下游域 SPI 实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅落地 SPI 端口（`IErpMdEmployeeReferenceChecker` + `IErpMdOrganizationReferenceChecker`）+ ErpPartyBizModel 收集机制；下游域（purchase/sales/inventory/hr）按业务需求驱动逐域注册实现。Partner 路径在 F7 已落地，本计划复用。
- Successor Required: `yes`（触发条件：业务出现"删除 Employee 前需检查所有引用"的运维场景；或 A2/A3 启动需 org/employee 引用预览）

### ErpMdUserAccount 接入统一 Party

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpMdUserAccount` 属 nop-auth 平台域（非 master-data），其账户语义与业务 Party 不同（登录账户 vs 业务方）；接入需跨工程协调 + 平台层 owner doc 变更。
- Successor Required: `yes`（触发条件：业务出现"按登录账户反查业务方"的统一档案需求 / 平台层 UserAccount 与业务 Party 合并的明确 owner doc 授权）

### 物化视图 / 反向索引表

- Classification: `optimization candidate`
- Why Not Blocking Closure: 3 实体 EstRows < 11K，union 查询性能可接受；C1 默认 IOrmTemplate 3 查询 + Java merge。
- Successor Required: `yes`（触发条件：Partner EstRows > 100K 或关键字查询 P95 > 500ms）

### Party 合并 / 去重（Golden Record / MDM）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 同一自然人在 Partner + Employee 都有记录时的合并语义属 MDM 范畴，需独立的 Golden Record 模型 + 合并工作流；C1 仅提供查询，不引入合并语义。
- Successor Required: `yes`（触发条件：业务出现"同一自然人在多 Party 类型有重复记录"的运维痛点 ≥ 5 次/月）

### 全文索引（MySQL FULLTEXT / ElasticSearch）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 默认 `LIKE '%keyword%'` 在 EstRows < 11K 下足够；全文索引引入 DB 引擎差异 + 运维成本。
- Successor Required: `yes`（触发条件：物化视图 successor 落地后仍不满足性能 / 业务需求中文分词检索）

### C2 跨境贸易字段扩展

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: C2 是 deepening-roadmap 独立工作项，依赖 C1 的 Party 抽象基础但 ORM 改动归 C2 独立 plan（`ErpMdMaterial` 加字段 + 新建 `ErpMdMaterialCustoms`）。
- Successor Required: `yes`（触发条件：A1 落地后 mission driver 下一轮 draft）

### 既有业务单据 FK 改造为通用 partyId + partyType

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 改 PurOrder.partnerId / SalOrder.partnerId / VoucherLine.partnerId 等业务单据 FK 为通用 party 引用需改 ORM + 既有业务逻辑 + 全部既有测试，高风险大范围改造；C1 提供查询层已足够大部分场景（picker 选 Party 后回填到具体 FK）。
- Successor Required: `yes`（触发条件：业务出现"凭证 partnerId 需指向 Employee/Organization 而非 Partner"的明确需求，需财务保护区域 owner doc 授权）

## Closure

Status Note: <待执行 + 结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立子代理（新会话）执行>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- Employee/Organization 引用扫描下游域 SPI 实现（触发：删除前引用预览运维场景 / A2/A3 启动）
- ErpMdUserAccount 接入统一 Party（触发：跨工程协调 + 平台层授权）
- 物化视图 / 反向索引表（触发：Partner EstRows > 100K 或查询 P95 > 500ms）
- Party 合并 / 去重 Golden Record（触发：重复记录运维痛点）
- 全文索引（触发：物化视图 successor 后仍不满足）
- C2 跨境贸易字段扩展（C1 落地后下一轮 mission driver draft）
- 业务单据 FK 通用化（触发：跨 Party 类型 FK 明确需求）
