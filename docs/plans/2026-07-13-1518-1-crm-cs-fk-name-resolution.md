# 2026-07-13-1518-1-crm-cs-fk-name-resolution CRM + CS 域外键名称解析批量推广（列表页 ID→名称）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「全量 1,036 FK 列名称解析」（Successor Required: yes，触发条件「高价值子集验证后批量推广需求」——**已满足**：经 6 批次 + finance + manufacturing + assets + projects + quality/maintenance 共 12 批次验证机制 D 全域可行性）
> Related: `2026-07-11-1643-1-amis-frontend-quality.md`（机制 D 范式源）、`2026-07-13-1518-2-hr-fk-name-resolution.md`（同批 N=2，无依赖）、`2026-07-13-1518-3-foundational-remaining-fk-name-resolution.md`（同批 N=3，无依赖）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 范围，独立子代理全量盘点 CRM ORM + CS ORM + 生成网格 + 生成 xmeta）：

### 机制 D 已验证（全域 12 批次 79+ 实体）

机制 D 三层接线（参考 `ErpSalOrder` + `ErpMfgWorkOrderLine` 先例）：(1) 自定义 xmeta 增派生 `*Name` prop（`queryable="false" sortable="false"` + `schema type="java.lang.String"`）；(2) BizModel 增 `@BizLoader(forType = Entity.class)` 方法（经 `orm().batchLoadProps(rows, Collections.singleton("<relation>"))` 批量加载 to-one 关系防 N+1，读 `getRelation().getName()` / `.getCode()`）；(3) 自定义 view.xml `<grid id="list"><cols x:override="bounded-merge">` 用 `*Name` 列替换原始 `*Id` 列。

### CRM 域覆盖现状

- **零 FK 名称解析覆盖**：全部 34 实体的自定义 view.xml 为空 `<grid id="list"/>`（继承生成基线列原样），无 `<cols>` bounded-merge 覆盖。
- **未覆盖（列表页显示原始数字 ID）34 实体**——本计划范围。

> **重要修正**：`ownerId` / `assignedToId` 等 `stdDomain="userId"` 列为 VARCHAR 字符串类型（非 BIGINT 数值 FK），在生成网格中**不**显示为 `ui:number="true"`，不属于机制 D 范畴。经 ORM + 生成网格核实，CRM+CS 域 `ownerId` 列均不在数值 FK 列表中。

### 未覆盖 34 CRM 实体清单（生成网格中显示为 `ui:number="true"` 的 FK 列）

| # | 实体 | 生成网格中原始 `*Id` FK 列 | 用户面价值 |
|---|------|---------------------------|-----------|
| 1 | **ErpCrmLead** | orgId, partnerId, sourceId, leadStatusId, stageId, campaignId, teamId, lostReasonId, territoryId | 线索（9 FK 列） |
| 2 | **ErpCrmStage** | orgId, teamId | 销售阶段 |
| 3 | **ErpCrmTeam** | orgId | 销售团队 |
| 4 | **ErpCrmCampaign** | orgId | 市场活动 |
| 5 | **ErpCrmEvent** | orgId, eventCategoryId, relatedLeadId, partnerId, contactId, parentEventId | 市场活动（6 FK 列） |
| 6 | **ErpCrmActivity** | leadId, orgId | 跟进活动 |
| 7 | **ErpCrmLeadConvLog** | leadId, orgId, fromStageId, toStageId | 转化日志 |
| 8 | **ErpCrmLeadScoreConfig** | orgId | 评分配置 |
| 9 | **ErpCrmLeadScoreConfigLine** | configId, orgId | 评分规则行 |
| 10 | **ErpCrmLeadScore** | leadId, orgId, configId | 评分记录 |
| 11 | **ErpCrmLeadScoreLine** | scoreId†, orgId, configLineId† | 评分明细（†目标实体无 name/code 显示列） |
| 12 | **ErpCrmForecastPeriod** | orgId | 预测期间 |
| 13 | **ErpCrmForecast** | orgId, periodId, territoryId, teamId, currencyId | 销售预测 |
| 14 | **ErpCrmForecastLine** | forecastId†, leadId, orgId | 预测行（†目标无显示列） |
| 15 | **ErpCrmForecastAccuracy** | forecastId†, orgId, periodId, teamId, territoryId | 预测准确度（†目标无显示列） |
| 16 | **ErpCrmTerritory** | orgId, parentId, managerId‡ | 销售区域（‡无 ext:relation） |
| 17 | **ErpCrmTerritoryAssignmentRule** | orgId, territoryId, groupId | 区域分配规则 |
| 18 | **ErpCrmQuota** | orgId, territoryId, teamId, currencyId | 销售定额 |
| 19 | **ErpCrmProductConfigurator** | orgId | 产品配置器 |
| 20 | **ErpCrmConfigRule** | configuratorId, orgId | 配置规则 |
| 21 | **ErpCrmBundlePricing** | orgId | 捆绑定价 |
| 22 | **ErpCrmBundlePricingLine** | bundleId, orgId, productId | 捆绑行 |
| 23 | **ErpCrmPriceRule** | orgId, productId, customerId, currencyId | 价格规则 |
| 24 | **ErpCrmSequence** | orgId | 销售序列 |
| 25 | **ErpCrmSequenceStep** | sequenceId, orgId | 序列步骤 |
| 26 | **ErpCrmSequenceAssignment** | orgId, sequenceId | 序列分配 |
| 27 | **ErpCrmLeadSequenceProgress** | leadId, sequenceId, orgId | 序列进度 |
| 28 | **ErpCrmLeadFunnel** | orgId, territoryId, teamId, sourceId | 漏斗 |
| 29 | **ErpCrmFunnelStageMetrics** | funnelId, orgId, stageId | 漏斗阶段指标 |
| 30 | **ErpCrmEventCategory** | — | 活动类别（无 FK 列） |
| 31 | **ErpCrmSource** | — | 来源（无 FK 列） |
| 32 | **ErpCrmLostReason** | — | 丢单原因（无 FK 列） |
| 33 | **ErpCrmQuoteTemplate** | — | 报价模板（无 FK 列） |
| 34 | **ErpCrmLeadStatus** | — | 线索状态（无 FK 列） |

> † = 有 ext:relation 但目标实体（ErpCrmLeadScore / ErpCrmLeadScoreConfigLine / ErpCrmForecast）无 `name`/`code` 列，执行时裁决显示字段（fallback `id` 或新增 `code` 列）或保留原始 ID。‡ = 无 ext:relation。ErpCrmEventCategory/Source/LostReason/QuoteTemplate/LeadStatus 无 FK 列，仅完整性记录。

### 未覆盖 16 CS 实体清单

| # | 实体 | 生成网格中原始 `*Id` FK 列 | 用户面价值 |
|---|------|---------------------------|-----------|
| 1 | **ErpCsTicket** | orgId, customerId, contactId, ticketTypeId, slaPolicyId, catalogItemId | 服务工单（6 FK 列） |
| 2 | **ErpCsTicketType** | defaultSlaPolicyId | 工单类型 |
| 3 | **ErpCsSlaPolicy** | ticketTypeId, teamId, escalationUserId‡ | SLA 策略（‡无 ext:relation） |
| 4 | **ErpCsTicketAction** | ticketId | 工单动作 |
| 5 | **ErpCsKnowledgeBase** | categoryId | 知识库 |
| 6 | **ErpCsCannedCategory** | orgId, parentId | 快捷回复分类 |
| 7 | **ErpCsCannedResponse** | orgId, categoryId, macroTicketTypeId | 快捷回复 |
| 8 | **ErpCsSurvey** | orgId, ticketId | 满意度调查 |
| 9 | **ErpCsAgentRate** | orgId, agentId‡ | 客服评分（‡无 ext:relation） |
| 10 | **ErpCsContract** | orgId, partnerId | 服务合同 |
| 11 | **ErpCsEntitlement** | orgId, partnerId, contractId, slaPolicyId | 服务权益 |
| 12 | **ErpCsCatalogCategory** | orgId, parentId | 服务目录类别 |
| 13 | **ErpCsServiceCatalogItem** | orgId, categoryId, parentId, ticketTypeId, slaPolicyId | 目录项（5 FK 列） |
| 14 | **ErpCsCatalogFulfillment** | orgId, catalogItemId | 目录履行 |
| 15 | **ErpCsTimeEntry** | orgId, ticketId, agentId‡, projectId‡, taskId‡ | 计时项（3 FK 无 ext:relation） |
| 16 | **ErpCsTeam** | — | 客服团队（无 FK 列） |

> ‡ = 无 ext:relation（ORM 有索引列但未声明 `<to-one>` 关系），保留原始 ID。ErpCsTeam 无 FK 列。

### ext:relation 缺口（保留原始 ID 的 FK 列）

| # | 实体 | FK prop | 处置裁决 |
|---|------|---------|---------|
| 1 | ErpCrmTerritory | managerId | 保留原始 ID（ORM 有索引但无 to-one relation） |
| 2 | ErpCsSlaPolicy | escalationUserId | 保留原始 ID（ORM 有索引但无 to-one relation） |
| 3 | ErpCsAgentRate | agentId | 保留原始 ID（ORM 有索引但无 to-one relation） |
| 4 | ErpCsTimeEntry | agentId | 保留原始 ID（同上） |
| 5 | ErpCsTimeEntry | projectId | 保留原始 ID（同上） |
| 6 | ErpCsTimeEntry | taskId | 保留原始 ID（同上） |

> 目标实体无显示列的 FK（scoreId/configLineId@ErpCrmLeadScoreLine、forecastId@ErpCrmForecastLine/ForecastAccuracy）有 ext:relation 可 batchLoadProps，执行时裁决显示字段（getCode() fallback 或保留 ID）。

剩余差距：44 实体（29 CRM + 15 CS）列表页显示原始数字 ID（用户面 P1 缺陷）。6 个无 FK 实体仅完整性记录。

## Goals

- 44 实体（29 CRM + 15 CS）列表页的高价值用户面 FK 列显示名称而非原始 ID（经机制 D）。
- 高价值 FK 定义：维度型外键（org→orgName 读 `ErpMdOrganization.name`；currency→currencyName 读 `ErpMdCurrency.name`；partner/customer→partnerName 读 `ErpMdPartner.name`；product/material→materialName 读 `ErpMdMaterial.name`；stage→stageName 读 `ErpCrmStage.stageName`；source→sourceName 读 `ErpCrmSource.name`；lostReason→lostReasonName 读 `ErpCrmLostReason.name`；campaign→campaignName 读 `ErpCrmCampaign.name`；team/group→teamName 读 `ErpCrmTeam.name`；territory→territoryName 读 `ErpCrmTerritory.name`；contact→contactName 读 `ErpMdPartnerContact.contactName`；eventCategory/category→categoryName 读对应实体 `.name`；parent→parentName 同实体自引用；ticketType→ticketTypeName 读 `ErpCsTicketType.name`；slaPolicy/defaultSlaPolicyId→slaPolicyName 读 `ErpCsSlaPolicy.name`；contract→contractName 读 `ErpCsContract.name`；catalogItem→catalogItemName 读 `ErpCsServiceCatalogItem.name`；configurator→configuratorName 读 `ErpCrmProductConfigurator.name`；sequence→sequenceName 读 `ErpCrmSequence.name`；funnel→funnelName 读 `ErpCrmLeadFunnel.funnelName`；config→configName 读 `ErpCrmLeadScoreConfig.configName`；bundle→bundleName 读 `ErpCrmBundlePricing.name`；period→periodCode 读 `ErpCrmForecastPeriod.code`）+ 高价值父单型内部链路（leadId→leadCode 读 `ErpCrmLead.code`；ticketId→ticketCode 读 `ErpCsTicket.code`；relatedLeadId→leadCode；parentEventId→eventCode 读 `ErpCrmEvent.code`）。
- 6 个无 ext:relation 的 FK 列（managerId/escalationUserId/agentId×2/projectId/taskId）保留原始 ID，归 successor。
- 派生 prop 名遵循 `{relation}Name`/`{relation}Code` 约定。
- 零 ORM/契约变更（机制 D 仅 xmeta 派生字段 + BizModel 只读 loader + view.xml 静态定制）。

## Non-Goals

- **HR 域 FK 名称解析**——由 `2026-07-13-1518-2` 承接。
- **Master Data/Logistics/Contract/B2B/DRP/APS 域 FK 名称解析**——由 `2026-07-13-1518-3` 承接。
- **codegen 模板层 FK 名称解析方案**——经 0600-1 裁决否决。
- **drawer 子表/明细行子网格 FK 名称**——本计划仅处理主列表网格 `<grid id="list">`。
- **6 个无 ext:relation 缺口 FK 的名称解析**——保留原始 ID，归 successor（触发条件：对应 ext:relation 落地或业务需求要求解析时）。
- **5 个 CRM + 1 个 CS 无 FK 列实体**——仅完整性记录，不做机制 D 变更。
- **看板/报表 FK 名称**——已由 1643-1 Phase 4 覆盖。
- **ownerId / assignedToId 等 userId 字符串列**——非数值 FK，不在机制 D 范畴。

## Task Route

- Type: `app-layer design change`（改用户可见的列表页显示行为，跨 CRM + CS 两域多实体，不改 API/模型/认证）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（机制 D 权威参考）、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`
- Skill Selection Basis: xmeta 派生 + view.xml bounded-merge → `nop-frontend-dev`；BizModel `@BizLoader` → `nop-backend-dev`；JUnit 测试 → `nop-testing`。
- Protected Areas: 无 ORM/ask-first 变更。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - CRM 域 FK 名称解析（29 实体含机制 D 变更）

Status: completed
Targets: `module-crm/erp-crm-meta/.../ErpCrm*/ErpCrm*.xmeta`；`module-crm/erp-crm-service/.../entity/ErpCrm*BizModel.java`；`module-crm/erp-crm-web/.../ErpCrm*/ErpCrm*.view.xml`
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无（机制 D 已由 12 批次验证）

- [x] `Decision`: 裁决 CRM 29 实体的目标 FK 清单 + 显示字段——维度型 FK 全部解析（org→orgName；partner/customer→partnerName；product/material→materialName；stage→stageName；source→sourceName；lostReason→lostReasonName；campaign→campaignName；team/group→teamName；territory→territoryName；contact→contactName；category→categoryName；parent→parentName 自引用；currency→currencyName；configurator→configuratorName；sequence→sequenceName；funnel→funnelName；config→configName；bundle→bundleName；period→periodCode）。父单型内部链路解析（leadId→leadCode 读 `.code`；relatedLeadId→leadCode；parentEventId→eventCode）。1 个 ext:relation 缺口 FK（managerId@Territory）保留原始 ID。3 个目标实体无 name/code 的 FK（scoreId/configLineId@ScoreLine、forecastId@ForecastLine/Accuracy）裁决保留原始 ID。contact@ErpCrmEvent 目标 ErpMdPartnerContact 读 `getContactPerson()`（master-data ORM 权威）。5 无 FK 实体不做变更。
  - Skill: `nop-backend-dev`
- [x] `Add`: 29 实体 xmeta 增派生 `*Name`/`*Code` prop（镜像 `ErpSalOrder.xmeta`，`queryable="false" sortable="false"` + `schema type="java.lang.String"`）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 29 实体 BizModel 增 `@BizLoader(forType = ErpCrm*.class)` 方法（镜像 `ErpSalOrderBizModel:228-269`，`orm().batchLoadProps` 批量加载 + null 安全读取）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 29 实体 view.xml `<grid id="list">` 由空占位改为 `<cols x:override="bounded-merge">`，用 `*Name`/`*Code` 列替换原始 `*Id` 列，保留非 FK 业务列。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 29 CRM 实体列表网格显示 `*Name`/`*Code` 而非原始 `*Id`（29 view.xml `xmllint --noout` well-formed + bounded-merge 含 `*Name` 列）

### Phase 2 - CS 域 FK 名称解析（15 实体含机制 D 变更）

Status: completed
Targets: `module-cs/erp-cs-meta/.../ErpCs*/ErpCs*.xmeta`；`module-cs/erp-cs-service/.../entity/ErpCs*BizModel.java`；`module-cs/erp-cs-web/.../ErpCs*/ErpCs*.view.xml`
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: Phase 1 范式已验证

- [x] `Decision`: 裁决 CS 15 实体（ErpCsTeam 无 FK 列除外）的目标 FK 清单——维度型 FK 全部解析（org→orgName；type/ticketType/macroTicketType→ticketTypeName 读 `ErpCsTicketType.name`；slaPolicy/defaultSlaPolicyId→slaPolicyName 读 `ErpCsSlaPolicy.name`；customer/partner→partnerName 读 `ErpMdPartner.name`；contact→contactName 读 `ErpMdPartner.name`；category→categoryName；contract→contractName 读 `ErpCsContract.name`；catalogItem→catalogItemName 读 `ErpCsServiceCatalogItem.name`；parent→parentName 自引用；team→teamName 读 `ErpCsTeam.name`）。父单型内部链路解析（ticketId→ticketCode 读 `ErpCsTicket.code`）。5 个 ext:relation 缺口 FK（escalationUserId@SlaPolicy、agentId@AgentRate、agentId/projectId/taskId@TimeEntry）保留原始 ID。ErpCsTeam 无 FK 列不做变更。
  - Skill: `nop-backend-dev`
- [x] `Add`: 15 实体 xmeta 增派生 `*Name`/`*Code` prop。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 15 实体 BizModel 增 `@BizLoader(forType = ErpCs*.class)` 方法。
  - Skill: `nop-backend-dev`
- [x] `Add`: 15 实体 view.xml `<grid id="list">` 改 `<cols x:override="bounded-merge">`，`*Name` 替换 `*Id`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 15 CS 实体列表网格显示 `*Name` 而非原始 `*Id`（15 view.xml `xmllint --noout` well-formed）

### Phase 3 - BizLoader 测试验证

Status: completed
Targets: `module-crm/erp-crm-service/src/test/java/app/erp/crm/service/TestErpCrmFkNameLoader.java` + `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsFkNameLoader.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [x] `Add`: 新建 `TestErpCrmFkNameLoader.java`（extends `JunitAutoTestCase`，镜像 `TestErpFinFkNameLoader`），经 `IGraphQLEngine` findList + `FieldSelectionBean` 请求 `*Name` 字段触发 `@BizLoader`，断言 `ErpCrmLead`（partnerName/stageName/campaignName/teamName/territoryName）+ `ErpCrmForecast`（periodCode/territoryName/teamName/currencyName）名称对齐 master-data。
  - Skill: `nop-testing`
- [x] `Add`: 新建 `TestErpCsFkNameLoader.java`（extends `JunitAutoTestCase`），经 `IGraphQLEngine` findList 请求 `*Name` 字段，断言 `ErpCsTicket`（ticketTypeName/slaPolicyName/customerName/contactName）+ `ErpCsServiceCatalogItem`（categoryName/ticketTypeName/slaPolicyName）名称对齐。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `TestErpCrmFkNameLoader` + `TestErpCsFkNameLoader` 全方法绿，验证 `@BizLoader` 批量加载防 N+1 且名称正确

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0a4a42d6bffeBRfEJWSwOeWInc`，general agent 新会话) — BLOCKER：基线 FK 列清单系统性错误（ErpCrmLead 列 currencyId/convertedPartnerId/convertedMaterialId 不存在且遗漏 partnerId/leadStatusId/teamId/territoryId；ErpCsTicket escalationContactId 不存在、typeId→ticketTypeId/slaId→slaPolicyId/assigneeId→assignedToId 列名错；ownerId 为 VARCHAR userId 非数值 FK）。经独立子代理全量 ORM+生成网格核实后，基线表已按真实列名重建。
- Independent draft review iteration 2: accept (`ses_0a4927021ffeC8TiuMjcJAjPcA`，general agent 新会话) — 全部 iteration 1 BLOCKER 已修正：ErpCrmLead 9 FK 列经 ORM 逐一核实正确（ownerId 确认 VARCHAR userId 已排除），ErpCsTicket 6 FK 列核实正确，6 个 ext:relation 缺口全部确认。格式合规、反松弛通过、无 Blocker/Major。2 项 Minor（H1 标题笔误已修、Goals 维度 FK 映射略泛但不影响权威基线表）均不阻塞。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-crm/erp-crm-service,module-cs/erp-cs-service -am`（含新增 `TestErpCrmFkNameLoader` + `TestErpCsFkNameLoader`）+ 44 view.xml `xmllint --noout` 一次。

- [x] 范围内行为完成（29 CRM + 15 CS 实体列表页 FK 显示名称）
- [x] 相关文档对齐（机制 D 范式无需更新；本计划为既有范式批量推广；父计划 `1643-1` Deferred Successor Progress 已追加 CRM+CS 批次完成记录）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + crm-service 133 + cs-service 97 `mvn test` 0 failures/0 errors + 44 view.xml `xmllint --noout` well-formed）
- [x] 无范围内项目降级为 deferred/follow-up（6 个 ext:relation 缺口 FK + 6 个无 FK 实体为 Non-Goals，已归 successor）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 6 个 ext:relation 缺口 FK 的名称解析

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: managerId@ErpCrmTerritory / escalationUserId@ErpCsSlaPolicy / agentId@ErpCsAgentRate / agentId+projectId+taskId@ErpCsTimeEntry 均为 ORM 有索引列但无 `<to-one>` relation 声明的弱指针，batchLoadProps 不可用。
- Successor Required: `yes`（触发条件：对应 ext:relation 落地或业务需求要求解析时）

### 6 个无 FK 列实体

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ErpCrmEventCategory/ErpCrmSource/ErpCrmLostReason/ErpCrmQuoteTemplate/ErpCrmLeadStatus/ErpCsTeam 均无 FK 列，无需机制 D 变更。
- Successor Required: `no`

## Closure

Status Note: closed

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 `ses_0a455140cffeOsBjdkpUYMJx5M`，未参与实现），2026-07-13 针对实时仓库验证（不依赖计划勾选）。
- Verdict: **PASS**。6 项审计任务全部通过：
  1. 机制 D 计数检查（44 实体）— 44/44 实体均具备 (a) ≥1 `queryable="false"` 派生 prop、(b) ≥1 `@BizLoader(forType=…)` 方法且含 `orm_attached()` 守卫、(c) `<cols x:override="bounded-merge">`。零遗漏。
  2. FK ID 移除抽检（5 实体）— ErpCrmLead/Forecast/Territory + ErpCsTicket/ServiceCatalogItem 数值 FK 均替换为 `*Name`/`*Code`；6 个 ext:relation 缺口例外（managerId/escalationUserId/agentId×2/projectId/taskId/scoreId/configLineId/forecastId×2）正确保留为 `*Id`。
  3. 测试文件 — `TestErpCrmFkNameLoader`（243 行）+ `TestErpCsFkNameLoader`（211 行）均 `extends JunitAutoTestCase`，经 `IGraphQLEngine` findList + `FieldSelectionBean` 请求 `*Name` 字段并 `assertEquals` 具体名称值。
  4. 构建产物 — 44 `_gen/_*.view.xml` + 44 `_templates/_*.json` 为 `mvn install` 自动重生成（派生 prop 的确定性结果，非手动编辑），不违反"禁手改生成物"规则。
  5. ORM 保持 — 全仓 `.orm.xml` 修改数=0，零 ORM/契约变更目标达成。
  6. 快照合法性 — 两个重录快照（BundlePricingCrudSmoke + TicketTypeCrudSmoke）仅为增量派生字段，`@var:` 时间戳引用保留，无字面时间戳腐蚀。
- 额外：44 view.xml `xmllint --noout` well-formed（namespace 警告为既有 Nop DSL 信息性输出，非错误）；`orm_attached()` 守卫全域 44 实体 loader 均存在（防 detached 聚合实体，如 `ErpCrmQuota.getQuotaRollup` 返回的内存聚合结果）。

Follow-up:

- HR 域 FK 名称解析 successor（见 `1518-2`）
- 其余域 FK 名称解析 successor（见 `1518-3`）
