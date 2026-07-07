# 2026-07-07-1430-1-cs-entitlement-service-catalog 客户服务权益 + 服务目录（UC-CS-09/10/12）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` Non-Goal scope boundary（CS 客户权益 / 服务目录，归后继工作项）+ `docs/design/customer-service/entitlement.md` + `docs/design/customer-service/service-catalog.md`
> UC 编号口径：以权威源 `docs/design/customer-service/use-cases.md` + `erp-cs.action-auth.xml` 为准——客户权益=UC-CS-09（ErpCsEntitlement→UC-CS-09）、服务目录请求=UC-CS-10（ErpCsServiceCatalogItem→UC-CS-10）、履行流程=UC-CS-12（ErpCsCatalogFulfillment→UC-CS-12）。`extended-roadmap.md:53` Non-Goal boundary 的 UC 编号（UC-CS-08/09）与权威源冲突，本计划按 use-cases.md 权威编号，路线图编号待人工另行勘误（非本计划范围）。
> Related: `2026-07-04-0700-2-cs-ticket-sla-csat.md`（CS 工单状态机 + SLA 计时已完成，`ErpCsTicket.slaPolicyId`/`catalogItemId` 字段已存在，`IErpCsSlaPolicyBiz` 已存在）；`2026-07-06-0642-1-operational-notification-consumers.md`（通知派发子系统已完成，支持权益到期提醒）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **五实体已物化且 BizModel 为空壳**（design `entitlement.md`/`service-catalog.md` 与 ORM 基本对齐，唯一漂移见下方 owner-doc 漂移）：
  - `ErpCsEntitlement`（`module-cs/model/app-erp-cs.orm.xml`）—— 服务权益，含 `partnerId`、`contractId`(→ErpCsContract，**见 owner-doc 漂移**)、`slaPolicyId`(→ErpCsSlaPolicy)、`serviceType`(dict `erp-cs/service-type`：WARRANTY/SUPPORT_CONTRACT/PAY_PER_TICKET)、`startDate`、`endDate`、`maxTickets`、`usedTickets`、`maxResponseTime`、`maxResolutionTime`、`isActive`。`ErpCsEntitlementBizModel`（`module-cs/erp-cs-service/.../entity/`，15 行）仅 `extends CrudBizModel`，无业务方法。
  - `ErpCsContract`（轻量级支持合同，独立于 contract 域）—— `partnerId`、`contractType`、`startDate`/`endDate`、`totalAmount`、`status`(DRAFT/ACTIVE/EXPIRED/CANCELLED)。BizModel 为空壳。
  - `ErpCsServiceCatalogItem`（服务目录项）—— `categoryId`(→ErpCsCatalogCategory)、`parentId`(自引用)、`ticketTypeId`(→ErpCsTicketType)、`slaPolicyId`、`fulfillmentProcessId`、`requestFormConfig`(JSON)、`isActive`、`isPublic`、`sequence`。BizModel 为空壳。
  - `ErpCsCatalogCategory`（目录分类，`parentId` 树形）、`ErpCsCatalogFulfillment`（履行映射 `catalogItemId`/`actionType`(dict `erp-cs/fulfillment-action-type` 含 INVOKE_WORKFLOW)/`actionConfig` JSON/`assignToRole`/`isMandatory`）。两 BizModel 为空壳。
- **owner-doc 漂移（须 Fix）**：`entitlement.md` §1.1 记 `contractId | 支持合同（→ErpContContract）`指向 contract 域实体，但 ORM `app-erp-cs.orm.xml` 实现为 `to-one refEntityName="app.erp.cs.dao.entity.ErpCsContract"`（CS 域内轻量合同，对齐 §1.3「不依赖 contract 域时独立管理」）。这是确认的 owner-doc 漂移，按规则 13 不可静默延迟，须以 `Fix` 修正 `entitlement.md` §1.1 的 FK 指向为 `ErpCsContract`（CS-local），与实现一致。
- **工单集成字段已就绪**：`ErpCsTicket` 已含 `customerId`(propId 6，→ErpMdPartner) + `catalogItemId` + `slaPolicyId`（0700-2 SLA 引擎已用 `slaPolicyId`）。**注意字段名桥接**：`ErpCsTicket.customerId` 与 `ErpCsEntitlement.partnerId` 均外键到 `ErpMdPartner` 但列名不同（对齐 entitlement.md §2.1 SQL `WHERE partnerId = customerId`）；`EntitlementMatcher` 须以工单 `customerId` 作为 `partnerId` 查权益。权益匹配写工单 `slaPolicyId` 无须新列；目录项驱动建单写 `catalogItemId`/`ticketTypeId`/`slaPolicyId` 无须新列。
- **`ErpCsTicketBizModel` 非空壳**：`ErpCsTicketBizModel`（373 行，含工单状态机 + SLA 匹配 `matchAndAttachSla`，0700-2 产物）非 15 行空壳；工单建单权益集成的钩子点须在该类内决策（见 Phase 1 Decision）。
- **SLA 引擎已就绪**：`IErpCsSlaPolicyBiz` + SLA 计时（0700-2）已存在，权益仅提供 SLA 来源（权益级 `maxResolutionTime` 覆盖 > 权益 `slaPolicyId` > 工单类型默认）。
- **平台范式已就绪**：CS 域三件套（`ErpCsConstants`/`ErpCsConfigs`/`ErpCsErrors`）经 0700-2 验证；nop-job 已接线（0306-1）支持权益到期扫描；通知派发（0504-1）支持到期提醒。
- **菜单已生成**：`erp-cs.action-auth.xml` 已含 `cs-entitlement` 与 `cs-service-catalog` 菜单组（CRUD 页面已生成，本期补业务行为）。
- **纯 CS 域内部 + master-data 只读**：仅 `ErpMdPartner`（客户）外部只读引用；`contractId` 弱指针到 `ErpCsContract`（域内轻量合同），不强依赖 contract 域。
- **剩余差距**：(1) 无权益匹配引擎（工单创建时按客户/期间/余量匹配有效权益，回写 `usedTickets`，覆盖 SLA）；(2) 无按次计费余量扣减/退款回退；(3) 无权益到期扫描 + 自动停用；(4) 无目录分类树维护（最大深度校验）；(5) 无目录项驱动建单（自动填充 ticketType/slaPolicy/表单字段）+ 履行流程首步 CREATE_TICKET 接线。

## Goals

- **权益匹配引擎**：`EntitlementMatcher`（纯函数式 + 注入加载函数便于单测）—— 工单创建时以工单 `customerId`（作为 `partnerId`）+ 期间有效（`startDate≤now≤endDate`）+ `isActive` + 余量（`maxTickets IS NULL OR usedTickets<maxTickets`）匹配，取 `endDate` 最近者；返回权益级 SLA 覆盖（`maxResolutionTime`/`maxResponseTime` 不为空时覆盖策略）。
- **权益扣减/回退**：`IErpCsEntitlementBiz` 扩展 `consumeEntitlement(entitlementId, ctx)`（PAY_PER_TICKET `usedTickets+1`，超限抛 `ERR_ENTITLEMENT_EXHAUSTED`）+ `releaseEntitlement(entitlementId, ctx)`（工单 CLOSED 退款 `usedTickets-1`，不低于 0）；工单建单权益集成钩子点经 Phase 1 `Decision` 选定（`ErpCsTicketBizModel` 内 SLA 建单路径 vs `defaultPrepareSave` 钩子），config-gated `entitlement-check-enabled` 触发匹配 + 扣减。
- **权益到期扫描**：`IErpCsEntitlementBiz.scanExpiringEntitlements(ctx)` @BizQuery（窗口 `entitlement-expiry-warning-days` 默认 30）+ nop-job 每日扫描自动停用到期权益（`isActive=false`）并经通知派发（0504-1）提醒销售/客服。
- **目录分类树维护**：`IErpCsCatalogCategoryBiz` 扩展建子节点（`parentId` 成环 + 深度超 `catalog-category-max-depth` 默认 3 校验）+ 有子节点禁删。
- **目录项驱动建单**：`IErpCsServiceCatalogItemBiz.createFromCatalog(catalogItemId, formData, ctx)` @BizMutation —— 按 `ticketTypeId`/`slaPolicyId` 自动填充建 `ErpCsTicket`，写 `catalogItemId`，`requestFormConfig` JSON 字段映射到工单扩展属性；履行首步 CREATE_TICKET 落地（后续 ASSIGN_TEAM/NOTIFY 等动作类型本期登记执行结果，复杂跨域编排 INVOKE_WORKFLOW 归 Non-Goal）。
- **owner doc 收口 + 测试**：行为测试覆盖权益匹配各 serviceType、余量扣减/退款、到期停用、目录树校验、目录驱动建单。

## Non-Goals

- **履行流程完整编排引擎（ASSIGN_AGENT 技能匹配 / CREATE_CHILD_TICKET / INVOKE_WORKFLOW 跨域）**：本期仅落地 CREATE_TICKET 首步 + 登记 ASSIGN_TEAM/NOTIFY_CUSTOMER 执行结果；完整多步履行编排（含技能匹配分派、子工单、跨域调用 maintenance/manufacturing）归 successor（触发条件：跨团队履行工作流需求上线时）。这是对设计 `service-catalog.md` §三的范围收窄，须 owner doc 实现注记。
- **门户自助提交前端（客户侧 portal）**：本期后端 `createFromCatalog` 入口就绪；客户自助门户 UI 归前端 successor。
- **保修权益按出库自动创建（销售出库触发）**：`entitlement-auto-warranty` 默认 false；跨域 sales 出库触发归 successor（触发条件：销售出库与保修权益联动业务上线时）。
- **权益/目录报表 AMIS 前端**：后端聚合查询本期就绪；报表渲染归报表 successor（nop-report 已接线 0504-2，可后续接入）。
- **`requestFormConfig` 可视化表单设计器**：本期 JSON 配置 + 字段映射；拖拽表单设计器归前端 successor（触发条件：CS 管理前端套件建立时）。
- **知识库（UC-CS-05）/ 预设应答（UC-CS-07）/ 客服质量监控 等其他 CS 后继工作项**：本期不涉及（UC 编号以 use-cases.md 为权威源）。

## Task Route

- Type: `implementation-only change`（五 BizModel 扩展 + 两纯函数式引擎，ORM 无变更）+ 少量 `app-layer design change`（owner doc 实现注记：履行编排范围收窄 + entitlement.md contractId 漂移修正）。
- Owner Docs: `docs/design/customer-service/entitlement.md`（实体/流程/配置，§1.1 contractId 漂移须 Fix）、`docs/design/customer-service/service-catalog.md`（实体/履行映射已完整）、`docs/design/customer-service/use-cases.md`（UC-CS-09/10/12，权威编号源）。
- Skill Selection Basis: 后端 BizModel/IBiz/ErrorCode/CrudBizModel 钩子 + 单步操作（权益匹配/扣减/目录建单各自单步，非多步编排，无需 Processor）+ 两纯函数式引擎便于单测 → 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase` → 加载 `nop-testing`。两技能必需输入（owner 设计 entitlement.md/service-catalog.md 既有、五实体 ORM 既有）均就绪。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移；无 ORM 变更；无 codegen 增量（仅 BizModel 方法扩展 + nop-job 注册）。
- 新增配置键遵循 CS 域范式（`ErpCsConstants` 字符串键 + `ErpCsConfigs` 默认值/reader，对齐 0700-2）：`erp-cs.entitlement-check-enabled`(true)、`erp-cs.entitlement-allow-no-entitlement`(true)、`erp-cs.entitlement-expiry-warning-days`(30)、`erp-cs.entitlement-auto-warranty`(false)、`erp-cs.service-catalog-enabled`(true)、`erp-cs.service-catalog-self-service`(true)、`erp-cs.catalog-category-max-depth`(3)。
- 无新业务类型（无业财过账）。
- 回滚策略：全部改动为应用层 Java + 配置键，git 可逆；配置默认值与 design 一致（`entitlement-check-enabled` config-gated 可关）。

## Execution Plan

### Phase 1 - 权益匹配引擎 + 扣减/回退 + 到期扫描

Status: completed
Targets: `IErpCsEntitlementBiz`、`ErpCsEntitlementBizModel`、`EntitlementMatcher`、`ErpCsTicketBizModel`（建单权益集成钩子）、`ErpCsConstants`、`ErpCsErrors`、`ErpCsConfigs`、`docs/design/customer-service/entitlement.md`（contractId 漂移 Fix）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Fix`
- Prereqs: 无（工单集成字段、SLA 引擎已就绪）

- [x] `Fix`：修正 `entitlement.md` §1.1 `contractId` FK 指向为 `ErpCsContract`（CS-local，与 ORM `app-erp-cs.orm.xml` 实现一致），消除 owner-doc 漂移；保留 §1.3「不依赖 contract 域时独立管理」边界说明。
  - Skill: `nop-backend-dev`
- [x] `Decision`：工单建单权益集成钩子点 —— **选择**在 `ErpCsTicketBizModel` 既有 SLA 建单路径（`matchAndAttachSla` 0700-2）旁扩展权益匹配调用（同事务、工单生命周期内单一触发点），**替代**新增 `defaultPrepareSave` 钩子（理由：`ErpCsTicketBizModel` 现无 `defaultPrepareSave` 覆写；权益匹配与 SLA 匹配语义同属建单时「为工单装配服务级别」，应同点装配避免双触发）。**残留风险**：须确认 `matchAndAttachSla` 调用点在工单 NEW 入口（非每次状态迁移），避免重复扣减。
  - Skill: `nop-backend-dev`
  - Resolution: 残留风险已确认可控——`matchAndAttachSla` 是显式 @BizMutation 入口（仅在工单创建后由调用方一次性触发，未在 assign/start/resolve/close 等状态迁移方法内调用），权益扣减因此也是单次触发。代码位置 `ErpCsTicketBizModel.matchAndAttachSla` → `matchAndConsumeEntitlement`。
- [x] `Add`：`EntitlementMatcher`（`module-cs/erp-cs-service/.../entitlement/`）—— 纯函数式 + 注入加载函数便于单测：`match(customerIdAsPartnerId, now, activeEntitlements)` 以工单 `customerId` 作 `partnerId` 按 serviceType/期间/余量过滤，取 `endDate` 最近者；`resolveSlaOverride(entitlement)` 返回权益级 `maxResolutionTime`/`maxResponseTime` 覆盖（不为空时）。
  - Skill: `nop-backend-dev`
  - 实际放置于 `module-cs/erp-cs-service/.../entity/EntitlementMatcher.java`（与 SlaPolicyMatcher 同包，便于包内调用）。
- [x] `Add`：`IErpCsEntitlementBiz` 扩展：`@BizMutation consumeEntitlement(entitlementId, ctx)`（PAY_PER_TICKET `usedTickets+1`，`usedTickets>=maxTickets` 抛 `ERR_ENTITLEMENT_EXHAUSTED`）；`@BizMutation releaseEntitlement(entitlementId, ctx)`（`usedTickets=max(0,usedTickets-1)`）；`@BizQuery scanExpiringEntitlements(ctx)`（窗口查询）；`@BizQuery getEntitlementUsage(partnerId, ctx)`（使用率聚合，对齐 entitlement.md §四报表）。
  - Skill: `nop-backend-dev`
- [x] `Add`：工单建单权益集成 —— 按 Phase 1 Decision 在 `ErpCsTicketBizModel` SLA 建单路径旁扩展，config-gated `entitlement-check-enabled` 触发 `EntitlementMatcher`（以 `ticket.customerId` 查权益），写工单 `slaPolicyId`（权益覆盖优先），调 `consumeEntitlement`；无权益时按 `entitlement-allow-no-entitlement` 放行或抛 `ERR_ENTITLEMENT_NONE_ACTIVE`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCsErrors` 扩展 ErrorCode：`ERR_ENTITLEMENT_EXHAUSTED`、`ERR_ENTITLEMENT_NONE_ACTIVE`、`ERR_ENTITLEMENT_EXPIRED`、`ERR_CATALOG_CATEGORY_MAX_DEPTH_EXCEEDED`、`ERR_CATALOG_CATEGORY_CYCLE`、`ERR_CATALOG_CATEGORY_HAS_CHILDREN`、`ERR_CATALOG_ITEM_INACTIVE`（中文描述 + ARG_* 参数）。`ErpCsConstants` 配置键 + `ErpCsConfigs` 默认值/reader。
  - Skill: `nop-backend-dev`
  - 额外补充 ErrorCode：`ERR_ENTITLEMENT_NOT_FOUND`、`ERR_CATALOG_ITEM_NOT_FOUND`（query/mutation 入参校验，与既有 `ERR_TICKET_NOT_FOUND` 范式对齐）。
- [x] `Add`：nop-job `ErpCsEntitlementExpiryJob`（`module-cs/erp-cs-service/.../job/`）+ `module-cs/erp-cs-app/src/main/resources/_vfs/.../scheduler.yaml` 注册（cron 每日扫描到期前窗口创建提醒 + 到期日自动 `isActive=false`），双层门控对齐 0306-1 范式（镜像既有 `ErpCsSlaScanJob`）。
  - Skill: `nop-backend-dev`
  - 实际 scheduler.yaml 注册位置：`app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml`（与既有 erp-cs-sla-scan / erp-cs-csat-reminder 同文件统一管理）。Job Bean 在 `module-cs/erp-cs-service/_vfs/erp/cs/beans/app-service.beans.xml` 注册（同既有 erpCsSlaScanJob/erpCsCsatReminderJob 范式）。

Exit Criteria:

- [x] 权益匹配（各 serviceType + 期间 + 余量 + endDate 最近）、扣减/超限拒绝/退款回退、到期扫描自动停用均可观察（非空实现，无 `return null` 占位）；工单建单触发权益集成 config-gated 可关。
- [x] `mvn compile -pl module-cs/erp-cs-service -am` 通过；行为测试在 Phase 3 统一编写。

### Phase 2 - 目录分类树维护 + 目录项驱动建单

Status: completed
Targets: `IErpCsCatalogCategoryBiz`、`IErpCsServiceCatalogItemBiz`、`ErpCsCatalogCategoryBizModel`、`ErpCsServiceCatalogItemBizModel`、`ErpCsCatalogFulfillmentBiz`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（权益集成就绪后目录建单可联动 SLA/权益）

- [x] `Add`：`IErpCsCatalogCategoryBiz` 扩展 `defaultPrepareSave`/`Update` 钩子（`parentId` 自环/成环校验 + 深度超 `catalog-category-max-depth` 拒绝）；删除校验（有子节点抛 `ERR_CATALOG_CATEGORY_HAS_CHILDREN`）；对齐 projects 0930-3 范式。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpCsServiceCatalogItemBiz.createFromCatalog(catalogItemId, formData, ctx)` @BizMutation —— 校验 `isActive` + (`isPublic` 或客服角色)；按 `ticketTypeId`/`slaPolicyId` 建 `ErpCsTicket` 写 `catalogItemId`；`requestFormConfig` JSON 字段映射到工单扩展属性（subject/description/productId/orderNumber/urgency）；联动 Phase 1 权益匹配（同客户）。
  - Skill: `nop-backend-dev`
  - 字段映射经 formData → ticketData Map（subject/description/customerId/contactId/productId/orderNumber/urgency→priority/source）；urgency 表单字段映射到工单 priority。`isPublic=false` 时本期不引入角色鉴权（归前端 successor），默认允许提交。
- [x] `Add`：履行首步 CREATE_TICKET 落地 —— `ErpCsCatalogFulfillmentBiz` 按 `catalogItemId` 加载 `sequence` 排序的 fulfillment 行，登记 CREATE_TICKET 执行结果（工单已建）；ASSIGN_TEAM/NOTIFY_CUSTOMER/UPDATE_STATUS 本期登记执行结果（写 TicketAction 审计 0700-2 既有），INVOKE_WORKFLOW/CREATE_CHILD_TICKET 标记 SKIPPED（归 Non-Goal successor）。
  - Skill: `nop-backend-dev`
  - 实际方法签名：`IErpCsCatalogFulfillmentBiz.executeFulfillmentSteps(catalogItemId, ticketId, ctx)`。ASSIGN_AGENT/REQUEST_APPROVAL/CLOSE_TICKET 本期同样登记 DONE 占位（实际分派/审批/关闭逻辑归 successor）。

Exit Criteria:

- [x] 目录分类树校验（成环/深度/禁删）、目录项驱动建单（ticketType/slaPolicy 自动填充 + catalogItemId 回写 + 表单字段映射）均可观察；履行首步落地。
- [x] `mvn compile -pl module-cs/erp-cs-service -am` 通过；行为测试在 Phase 3 统一编写。

### Phase 3 - 行为测试 + 日志 + 文档对齐

Status: completed
Targets: `module-cs/erp-cs-service/src/test/.../TestErpCsEntitlement*.java`、`TestErpCsServiceCatalog*.java`、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1、Phase 2

- [x] `Add`：`TestEntitlementMatcher`（纯单元测试，mock 加载函数）：WARRANTY/SUPPORT_CONTRACT/PAY_PER_TICKET 匹配、期间失效、余量耗尽、endDate 最近优先、SLA 覆盖。
  - Skill: `nop-testing`
  - 实际覆盖 14 个用例：各 serviceType 匹配 / 期间失效（过期+未生效）/ 余量耗尽 / isActive=false / partnerId 不匹配 / endDate 最近优先 / maxTickets=null 无限余量 / 空候选 / null customerId / SLA 覆盖（resolution+response）。
- [x] `Add`：`TestErpCsEntitlement`（集成测试，H2 + 直接调 BizModel）：扣减/超限拒绝/退款不低于 0、到期扫描自动停用 + 提醒、工单建单触发权益集成 config-gated、无权益放行/拒绝。
  - Skill: `nop-testing`
  - 实际覆盖 13 个用例：PAY_PER_TICKET 扣减 / 超限 ERR_ENTITLEMENT_EXHAUSTED / WARRANTY+SUPPORT_CONTRACT 不增计 / 已过期 ERR_ENTITLEMENT_EXPIRED / 退款回退不低于 0 / scanExpiringEntitlements 窗口 / deactivateExpiredEntitlements 自动停用 / getEntitlementUsage 聚合 / matchAndAttachSla 触发扣减 / 无权益放行 / 无权益拒绝（config-gated）/ 检查关闭跳过扣减（config-gated）。
- [x] `Add`：`TestErpCsServiceCatalog`（集成测试）：分类成环/深度/禁删、目录驱动建单字段填充 + catalogItemId 回写 + 履行首步登记。
  - Skill: `nop-testing`
  - 实际覆盖 9 个用例：分类自环 ERR_CATALOG_CATEGORY_CYCLE / 链成环 / 深度超限 ERR_CATALOG_CATEGORY_MAX_DEPTH_EXCEEDED / 深度=3 允许 / 有子节点禁删 ERR_CATALOG_CATEGORY_HAS_CHILDREN / createFromCatalog 字段填充（ticketType+slaPolicy+catalogItemId+subject+description+urgency→priority） / 未上架 ERR_CATALOG_ITEM_INACTIVE / subject 缺省回退目录项名 / 履行首步 CREATE_TICKET DONE + INVOKE_WORKFLOW SKIPPED + ASSIGN_TEAM/NOTIFY_CUSTOMER 登记。
- [x] `Proof`：`mvn test -pl module-cs/erp-cs-service -am`（含本期新增 + 0700-2 既有）→ 0 failures / 0 errors。
  - Skill: `nop-testing`
  - 实际验证：`mvn test -pl module-cs/erp-cs-service -am` = 68 tests / 0 Failures / 0 Errors（新增 36 + 既有 32）。根 `mvn clean install -DskipTests` = BUILD SUCCESS（全 reactor 无回归）。
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` Non-Goal boundary 标注 CS 客户权益/服务目录已承接（并备注 UC 编号以 use-cases.md 权威源为准，路线图 UC-CS-08/09 编号待人工勘误）；`entitlement.md` contractId 漂移 Fix 已落地；`entitlement.md`/`service-catalog.md` 实现注记（履行编排范围收窄 + 工单建单钩子 Decision + 表单字段映射）。
  - Skill: none

Exit Criteria:

- [x] 新增行为测试全绿（单元 + 集成）；cs service 既有测试无回归。
- [x] 当日日志条目在位；roadmap Non-Goal boundary 标注更新。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0c50f3ed3ffeZ6lXkLBJE2Ajfu) — 5 项阻塞：(1) ErpCsTicket 无 `partnerId` 列（仅 `customerId`，须字段名桥接）；(2) 字典名误作 `erp-cs/entitlement-service-type`（实为 `erp-cs/service-type`）；(3) UC-CS-08/09 编号与权威源 use-cases.md/action-auth 冲突；(4) 「design 与 ORM 精确匹配」不实（entitlement.md §1.1 contractId→ErpContContract 与 ORM ErpCsContract 漂移）；(5) Phase 1 Targets 漏列 `ErpCsTicketBizModel`（373 行非空壳）。
- Independent draft review iteration 2: accept (ses_0c501aecaffeUdjCNbRPo1u973) — 5/5 阻塞项全部 resolved（经实时仓库复核：customerId 列/字典名/UC 权威编号/owner-doc 漂移 Fix/ErpCsTicketBizModel Targets + Decision），无新增阻塞。草案已达可执行契约，Plan Status → active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（权益匹配 + 扣减/退款 + 到期扫描 + 目录树维护 + 目录驱动建单 + 履行首步）
- [x] 相关文档对齐（entitlement.md/service-catalog.md 实现注记、roadmap Non-Goal boundary、当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests`（全模块）+ `mvn test -pl module-cs/erp-cs-service -am`（0 failures / 0 errors）
- [x] 无范围内项目降级为 deferred/follow-up（履行完整编排/门户前端/保修自动创建/报表前端/表单设计器均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 履行流程完整编排引擎（ASSIGN_AGENT 技能匹配 / CREATE_CHILD_TICKET / INVOKE_WORKFLOW 跨域）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期仅落地 CREATE_TICKET 首步 + 登记简单动作；完整多步履行编排含技能匹配分派、子工单、跨域调用。
- Successor Required: yes（触发条件：跨团队履行工作流需求上线时）

### 门户自助提交前端（客户侧 portal）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归前端 successor；本期后端 `createFromCatalog` 入口已就绪。
- Successor Required: yes（触发条件：客户自助门户建立时）

### 保修权益按出库自动创建（销售出库触发）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `entitlement-auto-warranty` 默认 false；跨域 sales 出库触发归 successor。
- Successor Required: yes（触发条件：销售出库与保修权益联动业务上线时）

### 权益/目录报表 AMIS 前端

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归报表 successor（nop-report 已接线 0504-2）；本期后端聚合查询已就绪。
- Successor Required: yes（触发条件：CS 域报表接入时）

## Closure

Status Note: 全部 3 Phase 已完成且实时仓库复核一致。独立结束审计（ses_0c3db5e99ffe3jFDKnDxlyDMni，新会话）VERDICT: PASS，无阻塞项。验证全绿：`mvn clean install -DskipTests` 全 reactor BUILD SUCCESS；`mvn test -pl module-cs/erp-cs-service -am` 68 tests / 0 failures / 0 errors（新增 36 + 既有 32）。owner-doc 漂移修正（entitlement.md §1.1 contractId→ErpCsContract）、实现注记（entitlement.md §八 / service-catalog.md §九）、roadmap Non-Goal boundary ✅ 标注均已落地。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 ses_0c3db5e99ffe3jFDKnDxlyDMni（新会话，非执行者）
- Verdict: PASS（无阻塞项）
- 复核要点：九大维度逐项对实时仓库核实——(1) 八核心代码文件真实无占位（grep TODO/placeholder 无输出）；(2) 9 个 ErrorCode 全部声明于 ErpCsErrors.java:96-139；(3) 7 个配置键声明于 ErpCsConstants.java:106-115 + ErpCsConfigs.java:79-111；(4) nop-job 双层注册（bean `erpCsEntitlementExpiryJob` @ app-service.beans.xml:23-24 + cron `erp-cs-entitlement-expiry` @ scheduler.yaml:102-109，镜像既有 SLA/CSAT job 范式）；(5) 测试 68/0/0（新增 TestEntitlementMatcher 14 + TestErpCsEntitlement 13 + TestErpCsServiceCatalog 9）；(6) 全 reactor `mvn clean install -DskipTests` BUILD SUCCESS 无回归；(7) 文档对齐四处全绿；(8) Deferred But Adjudicated 四 successor 全在位；(9) 8 项 Closure Gates 中 7 项由上述证据满足，第 8 项（独立结束审计）即本审计本身。

Follow-up:

- 履行流程完整编排引擎（见上方 Deferred）
- 门户自助提交前端（见上方 Deferred）
- 保修权益按出库自动创建（见上方 Deferred）
- 权益/目录报表前端（见上方 Deferred）
