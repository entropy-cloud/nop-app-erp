# 2026-07-07-1100-1-crm-territory-quota CRM 销售区域管理 + 配额（UC-CRM-05/06）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` Non-Goal scope boundary（UC-CRM-05 领地管理 / UC-CRM-06 配额管理，归后继工作项）+ `docs/design/crm/territory.md`
> Related: `2026-07-04-0700-1-crm-event-reminder-lead-scoring-forecast.md`（CRM 预测引擎已完成，`ErpCrmForecast.territoryId` 已存在）；`2026-07-04-0549-2-crm-lead-opportunity-quotation-conversion.md`（Lead 状态机已完成）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **三实体已物化但 BizModel 为空壳**：
  - `ErpCrmTerritory`（`module-crm/model/app-erp-crm.orm.xml:959`）—— 销售区域节点，自引用树（`parentId` propId 5 + `parent` to-one :985），含 `territoryType`(dict `erp-crm/territory-type`)、`managerId`、`fullPath`、`level`、`isActive`、`isLeaf`、`sortOrder`，UK `code,orgId`，索引 `parentId`/`managerId`。`ErpCrmTerritoryBizModel`（`module-crm/erp-crm-service/.../entity/ErpCrmTerritoryBizModel.java`，15 行）仅 `extends CrudBizModel`，无业务方法。
  - `ErpCrmTerritoryAssignmentRule`（orm:1002）—— 区域分配规则，含 `priority`、`territoryId`(mandatory)、`conditionType`(dict `erp-crm/assignment-condition-type`)、`conditionValue`(JSON)、`assignmentMethod`(dict `erp-crm/assignment-method`)、`groupId`(→ErpCrmTeam)、`isDefault`、`isActive`，to-one `territory`/`team`/`org`。BizModel 为空壳。
  - `ErpCrmQuota`（orm:1041）—— 销售配额，含 `territoryId`/`teamId`/`ownerId`、`periodType`(dict `erp-crm/quota-period-type`)、`fiscalYear`、`periodLabel`、`quotaAmount`、`currencyId`、`isFinalized`、`notes`，to-one `territory`/`team`/`org`/`currency`，索引 `territoryId`/`teamId`/`ownerId`。BizModel 为空壳。
- **预测维度已就绪**：`ErpCrmForecast`（orm:816-818）已含 `territoryId`(propId 4)/`teamId`(5)/`ownerId`(6) + to-one `territory`(:838)，预测引擎 0700-1 已完成）。**区域管道报表「实际/预测/目标」同屏对比的数据源已就绪**，缺聚合查询入口。
- **关键缺口 1 — `ErpCrmLead` 无 `territoryId` 列**：`ErpCrmLead`（orm:185-274）列清单（propId 1-40）仅有 `ownerId`(propId 25) + `teamId`(propId 26)，**无 `territoryId`**。设计 `territory.md:136` 要求分配后写 `lead.territoryId`。分配规则匹配出的区域当前**无处回写到 Lead**。本期须 Decision 是否新增 Lead 加性列（ORM ask-first 保护区域 + codegen 重生成）。
- **关键缺口 2 — 无 `ErpCrmTeamMember` 实体**：`grep ErpCrmTeamMember` 全仓 0 命中。设计 `territory.md:157-161` 的 ROUND_ROBIN/LOAD_BALANCED 分配方法需枚举团队内成员并按负载/轮询挑人。当前无独立团队成员表，团队与成员的关联模型须 Explore 确认（候选：团队经 `ownerId`/用户角色隐式表达，或须新增成员表）。
- **owner 设计完整**：`docs/design/crm/territory.md`（213 行）覆盖实体清单、业务规则（区域树最大深度 4、线索自动分配流程、配额层级汇总、定稿锁定、区域停用）、配置点、反模式、跨域协作。
- **平台范式已就绪**：nop-job scheduler 已接线（`2026-07-05-0306-1`）支持配额/报表周期任务；通知派发子系统已完成（`2026-07-06-0504-1`）支持分配结果提醒。
- **剩余差距**：(1) 三 BizModel 无区域树维护方法（建子节点 level/fullPath 回填、有子节点禁删、停用门控）；(2) 无分配引擎（规则按 priority 遍历 + conditionType 匹配 + 回写 Lead）；(3) 无配额层级聚合查询（公司/区域/团队/个人 Σ）+ 定稿锁定 + 与预测同屏对比入口；(4) Lead 加 `territoryId` 列未决。

## Goals

- **区域树维护**：`IErpCrmTerritoryBiz` 扩展建/移动子节点方法（自动回填 `level`/`fullPath`/`isLeaf`，`parentId` 成环与深度超限校验，有子节点禁删，`isActive=false` 不参与分配匹配）。
- **Lead 加 `territoryId` 加性列**：经 Decision 确认后，`ErpCrmLead` 新增可空 `territoryId` FK（→ErpCrmTerritory）+ codegen 重生成 crm-dao/meta，使分配规则匹配结果可回写。
- **分配引擎**：`TerritoryAssignmentEngine`（纯函数式 + 注入加载函数便于单测）—— 按 `priority` 遍历 `isActive=true` 规则，`conditionType`（GEOGRAPHY/INDUSTRY/CUSTOMER_SIZE/CUSTOM_FIELD）经 `conditionValue` JSON 匹配线索字段，命中则回写 `lead.territoryId`（+ `teamId`/`ownerId` 按 assignmentMethod）；无命中走 `isDefault` 规则；仍无则留空标记未分配。`assignLead(leadId, ctx)` @BizMutation 入口 + `reassignLead` 手动重分配。
- **配额管理**：`IErpCrmQuotaBiz` 扩展层级聚合查询 `getQuotaRollup(territoryId, periodType, fiscalYear, periodLabel, ctx)`（公司→区域→团队→个人 Σ，显式值优先）+ `finalizeQuota`（定稿锁定，已定稿拒绝修改须先解冻 `unfinalizeQuota`）+ 年度配额按季/月均分 `distributeAnnualQuota`。
- **区域管道对比入口**：`getTerritoryPipeline(territoryId, periodLabel, ctx)` @BizQuery —— 同屏返回该区域 `ErpCrmQuota`（目标）+ `ErpCrmForecast` 聚合（预测）+ 实际收入聚合（对齐 `territory.md:140`）。
- **owner doc 收口 + 测试**：`territory.md` 实现注记（若 ROUND_ROBIN/LOAD_BALANCED 因成员模型缺失降级）；行为测试覆盖树维护、分配引擎各 conditionType、配额聚合/定稿、对比入口。

## Non-Goals

- **ROUND_ROBIN/LOAD_BALANCED 完整成员负载分配**：依赖团队成员模型（缺口 2）。本期 Explore 确认成员模型后，若成员表缺失，分配方法降级为「MANUAL（标记待分配）+ territory/team 回写」；完整轮询/负载挑人归 successor（触发条件：团队成员表落地时）。这是对设计 `territory.md:134-136` 的范围收窄，须 Phase 1 实现注记 + owner doc 显式登记，避免 owner-doc 漂移。
- **真 DAG 区域模型**：本期维持 `parentId` 单父树形结构；多父归属（一区域属多个父区域）属 ORM successor。
- **配额自动审批流 / 配额变更审计**：定稿/解冻直接乐观锁，不做工作流审批（平台 `NopSysChangeLog` 自动记录实体变更已足够）。
- **区域管道报表 AMIS 前端页面 / 区域树可视化**：归前端 successor（后端 API 本期就绪）。
- **线索分配异步批量**：本期同步分配；大批量分配归异步 successor（设计 `territory.md:182` 反模式警示已提）。
- **自定义字段分配 DSL 编辑器**：本期内置四类 conditionType；自定义 DSL 归 successor。
- **CPQ/序列管理/漏斗分析（UC-CRM-07/08/09）**：属 CRM 其他后继工作项，本期不涉及。

## Task Route

- Type: `app-layer design change`（owner doc 实现注记 + Lead 加性列 Decision）+ `implementation-only change`（三 BizModel 扩展 + 分配引擎 + 配额聚合 + Lead 列 codegen）。
- Owner Docs: `docs/design/crm/territory.md`（实体/规则/配置已完整，本期补实现注记）、`docs/design/crm/use-cases.md`（UC-CRM-05/06/11/12）、`docs/design/crm/README.md`（§ErpCrmLead 回写字段）。
- Skill Selection Basis: 后端 BizModel/IBiz/ErrorCode/CrudBizModel 钩子 + 单步操作（树维护/分配/配额各自单步，非多步编排，无需 Processor）→ 加载 `nop-backend-dev`；ORM 加性列经 model→codegen → 参考 runbook；测试经 `JunitAutoTestCase` → 加载 `nop-testing`。两技能必需输入（owner 设计 territory.md 既有、三实体 ORM 既有）均就绪。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移脚本（Lead 加性列为可空列，无数据回填）。
- Lead `territoryId` 加性列须 codegen 重生成 `module-crm/erp-crm-dao`（+ meta）。回滚：删除新增列 + 重生成，git 可逆。
- 新增配置键遵循 CRM 域范式（`ErpCrmConstants` 字符串键 + 默认值，对齐 0700-1 既有 `ErpCrmConfigs`）：`erp-crm.territory.auto-assign-on-create`（默认 true）、`erp-crm.territory.default-team-id`、`erp-crm.territory.max-depth`（默认 4）、`erp-crm.quota.distribute-monthly`（默认 false）。
- 无新业务类型（无业财过账）。
- 回滚策略：全部改动为应用层 Java + ORM 加性列，git 可逆；新配置键默认值保持现有行为（auto-assign config-gated 可关）。

## Execution Plan

### Phase 1 - owner 设计确认 + Lead 加性列 Decision

Status: completed
Targets: `docs/design/crm/territory.md`（实现注记）、`module-crm/model/app-erp-crm.orm.xml`（ErpCrmLead 加 territoryId 列）、`module-crm/erp-crm-codegen`（重生成）
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Add`
- Prereqs: 无

- [x] `Explore`：确认团队成员成员模型 —— `grep`/`read` `ErpCrmTeam` 实体定义与既有 CRM 代码，确认是否存在成员子实体或经 `ownerId`/用户角色隐式表达。**Explore 须在 ROUND_ROBIN/LOAD_BALANCED Decision 前完成**；结论落入下方 Decision。
  - Skill: `nop-backend-dev`
- [x] `Decision`：Lead `territoryId` 加性列 —— **选择**在 `ErpCrmLead` 新增可空 `territoryId`（BIGINT，stdDataType=long，propId=41）+ to-one `territory` 关系 + 索引 `IDX_CRM_LEAD_TERRITORY_ID`，因为设计 `territory.md:136` 明确要求分配回写 `lead.territoryId`，且区域管理的价值依赖 Lead↔Territory 链接。**替代**：不新增列，分配仅回写 teamId/ownerId —— 区域维度在 Lead 上丢失，管道报表无法按 Lead 区域下钻，rejected。**残留风险**：ORM ask-first 保护区域 + codegen 重生成；改动为加性可空列（低风险），与 0549-1（StockBalance 加 ownerId）/0024-2 范式一致。框架强制选择（须匹配既有列声明范式）记为约束，无需完整替代分析。
  - Skill: `nop-backend-dev`
- [x] `Decision`：分配方法范围 —— 基于团队成员 Explore 结论。若成员表存在 → 实现 ROUND_ROBIN/LOAD_BALANCED；若不存在 → 本期实现 MANUAL（territory/team 回写，owner 留空标记待分配）+ territory/team 回写，ROUND_ROBIN/LOAD_BALANCED 降为 successor。**残留风险**：若降级，须 `territory.md` 实现注记 + use-cases 显式登记，避免 owner-doc 漂移。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCrmLead` 加 `territoryId` 列 + to-one `territory` + 索引（经 Lead Decision）；`docs/design/crm/territory.md` 补实现注记（成员模型结论 + 分配方法范围 + 加性列说明）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] Lead `territoryId` 列在 orm.xml 声明；codegen 重生成 crm-dao/meta 成功（`mvn install -pl module-crm/erp-crm-codegen -am -DskipTests`）。
- [x] `territory.md` 实现注记在位，与 Explore/Decision 结论一致。

### Phase 2 - 区域树维护 + 分配引擎

Status: completed
Targets: `IErpCrmTerritoryBiz`、`ErpCrmTerritoryBizModel`、`TerritoryAssignmentEngine`、`IErpCrmLeadBiz`（分配入口）、`ErpCrmConstants`、`ErpCrmErrors`、`ErpCrmConfigs`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（Lead 加 territoryId 后才能回写）

- [x] `Add`：`IErpCrmTerritoryBiz` 扩展：`@BizMutation createChild(parentId, code, name, territoryType, ctx)`（回填 level=parent.level+1、fullPath=parent.fullPath+"/"+code、parent.isLeaf=true；深度超 `max-depth` 抛 `ERR_TERRITORY_MAX_DEPTH_EXCEEDED`；parentId 成环抛 `ERR_TERRITORY_CYCLE`）；`@BizMutation moveTerritory(territoryId, newParentId, ctx)`（重算 level/fullPath + 子树递归 + 成环校验）；删除校验（有子节点抛 `ERR_TERRITORY_HAS_CHILDREN`）；`@BizQuery getTerritoryTree(parentId, ctx)`（子树查询）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`TerritoryAssignmentEngine`（`module-crm/erp-crm-service/.../territory/`）—— 纯函数式 + 注入加载函数便于单测：`assign(lead, rules, defaultRule, ctx)` 按 priority 遍历 isActive 规则，`ConditionMatcher` 按 conditionType 解析 conditionValue JSON 匹配 lead 字段（GEOGRAPHY→companyName 省市、INDUSTRY→字典码、CUSTOMER_SIZE→companySize 范围、CUSTOM_FIELD→任意字段），命中返回 territoryId/teamId/assignmentMethod；按 Phase 1 Decision 实现 assignmentMethod 分派（MANUAL 落地 / ROUND_ROBIN/LOAD_BALANCED 或降级）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpCrmLeadBiz` 扩展分配入口：`@BizMutation assignLead(leadId, ctx)`（config-gated `auto-assign-on-create`，加载 active 规则 + default 规则，调 TerritoryAssignmentEngine，回写 lead.territoryId/teamId/ownerId）；`@BizMutation reassignLead(leadId, territoryId, teamId, ownerId, ctx)`（手动重分配，覆盖引擎结果）。`ErpCrmLeadBizModel.defaultPrepareSave` 钩子：新建 Lead 且 ownerId/teamId 为空时触发自动分配。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCrmErrors` 扩展 ErrorCode：`ERR_TERRITORY_MAX_DEPTH_EXCEEDED`、`ERR_TERRITORY_CYCLE`、`ERR_TERRITORY_HAS_CHILDREN`、`ERR_TERRITORY_NOT_ACTIVE`、`ERR_QUOTA_FINALIZED`、`ERR_QUOTA_NO_MATCH`（中文描述 + ARG_* 参数）。`ErpCrmConstants` 配置键 + `ErpCrmConfigs` 默认值/reader（对齐 0700-1 范式）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 区域树维护（建子节点/移动/成环/深度/禁删）各 ErrorCode 可观察触发；分配引擎四 conditionType 匹配 + default 兜底 + 回写 lead.territoryId 可观察（非空实现，无 `return null` 占位）。
- [x] `mvn compile -pl module-crm/erp-crm-service -am` 通过；行为测试在 Phase 4 统一编写。

### Phase 3 - 配额管理 + 区域管道对比入口

Status: completed
Targets: `IErpCrmQuotaBiz`、`ErpCrmQuotaBizModel`、`QuotaRollupCalculator`、区域管道对比 `@BizQuery`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`IErpCrmQuotaBiz` 扩展：`@BizQuery getQuotaRollup(territoryId, periodType, fiscalYear, periodLabel, ctx)`（`QuotaRollupCalculator` 按区域树子节点 Σ quotaAmount，显式值优先，公司级 territoryId=null 兜底）；`@BizMutation finalizeQuota(quotaId, ctx)`（isFinalized=true，已定稿抛 `ERR_QUOTA_FINALIZED`）；`@BizMutation unfinalizeQuota(quotaId, ctx)`；`@BizMutation distributeAnnualQuota(quotaId, periodType, ctx)`（年度配额按季/月均分生成子期间配额行）。
  - Skill: `nop-backend-dev`
- [x] `Add`：区域管道对比入口 `@BizQuery getTerritoryPipeline(territoryId, periodLabel, ctx)` —— 同屏返回 `QuotaSummary`（目标，经 getQuotaRollup）+ `ForecastSummary`（预测，经 `IErpCrmForecastBiz` 按 territoryId 聚合 commit/upside/best-case/weighted）+ 实际收入聚合（同域只读，对齐 `territory.md:140`）。返回结构化 DTO（非裸 Map）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 配额聚合（公司/区域/团队/个人 Σ + 显式值优先）、定稿锁定/解冻、年度均分均可观察；区域管道对比入口返回 目标/预测/实际 三段非空。
- [x] `mvn compile -pl module-crm/erp-crm-service -am` 通过；行为测试在 Phase 4 统一编写。

### Phase 4 - 行为测试 + 日志 + 文档对齐

Status: completed
Targets: `module-crm/erp-crm-service/src/test/.../TestErpCrmTerritory*.java`、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2、Phase 3

- [x] `Add`：区域树维护测试（建子节点 level/fullPath 回填、移动重算、成环拒绝、深度超限、有子节点禁删、停用门控）。
  - Skill: `nop-testing`
- [x] `Add`：分配引擎测试（四 conditionType 各一命中场景 + default 兜底 + 无匹配留空 + auto-assign config-gated 关闭 + reassignLead 覆盖；assignmentMethod 按 Phase 1 Decision 分支）。
  - Skill: `nop-testing`
- [x] `Add`：配额测试（层级 Σ + 显式值优先 + 定稿/解冻 + 年度均分 + 区域管道对比入口三段返回）。
  - Skill: `nop-testing`
- [x] `Proof`：`mvn test -pl module-crm/erp-crm-service -am`（含本期新增 + 0700-1/0549-2 既有）→ 0 failures / 0 errors。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` Non-Goal boundary 标注 UC-CRM-05/06 已承接（done 指向本计划）；`territory.md` 实现注记补齐（如有偏离）。
  - Skill: none

Exit Criteria:

- [x] 新增行为测试全绿；crm-service 既有测试无回归。
- [x] 当日日志条目在位；roadmap Non-Goal boundary 标注更新。

## Draft Review Record

- Independent draft review iteration 1: accept (2026-07-07-1100-1 review) because format/completeness/scope/closure-evidence 全项通过：必需节齐备、字段名正确、阶段结构合法；Exit Criteria 可观察可测；item 类型（Explore 临时标签 / Decision 携带选择+替代+残留风险 / Add / Proof）符合指南规则 8/9；Non-Goals 显式且附触发条件；条件性 Decision（分配方法范围）妥善处理团队成员模型不确定性并设 owner-doc 漂移守卫；Deferred But Adjudicated 三件均带 Classification + Why Not Blocking + Successor Required；基线声明经实时核实（ErpCrmTeamMember 0 命中、ErpCrmForecast.territoryId 存在 orm:816、ErpCrmLead 185-274 无 territoryId）。无 Blocker/Major。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（区域树维护 + 分配引擎 + Lead territoryId 回写 + 配额聚合/定稿 + 区域管道对比入口）
- [x] 相关文档对齐（`territory.md` 实现注记、roadmap Non-Goal boundary、当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests`（全模块）+ `mvn test -pl module-crm/erp-crm-service -am`（0 failures / 0 errors）
- [x] 无范围内项目降级为 deferred/follow-up（ROUND_ROBIN/LOAD_BALANCED 降级须附成员模型 Explore 结论 + 触发条件；真 DAG/前端/异步批量/DSL 均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ROUND_ROBIN / LOAD_BALANCED 完整成员负载分配

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖团队成员模型（缺口 2）。本期 Phase 1 Explore 确认成员模型；若成员表缺失，分配降级为 MANUAL + territory/team 回写。
- Successor Required: yes（触发条件：团队成员表落地时）

### 真 DAG 区域模型（多父归属）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期维持 `parentId` 单父树形结构；多父归属需新增多对多关系表。
- Successor Required: yes（触发条件：一区域需属多个父区域业务需求上线时）

### 区域管道报表 AMIS 前端 / 区域树可视化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归前端 successor；本期后端 `@BizQuery` 入口已就绪。
- Successor Required: yes（触发条件：前端区域管理套件建立时）

### 线索分配异步批量

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期同步分配；大批量归异步 successor（设计反模式警示已提）。
- Successor Required: yes（触发条件：单批分配线索数超阈值时）

## Closure

Status Note: 已完成（执行者视角）。4 Phase 全部 [x]：Phase 1（Explore 团队成员模型缺失 → Decision Lead territoryId 加性列 + 分配方法降级 MANUAL + territory.md 实现注记）；Phase 2（区域树维护 createChild/moveTerritory/有子节点禁删 + TerritoryAssignmentEngine 四 conditionType + assignLead/reassignLead + 6 ErrorCode + 11 字典常量）；Phase 3（QuotaRollupCalculator 显式值优先层级聚合 + finalize/unfinalize + distributeAnnualQuota + getTerritoryPipeline 三段对比入口 + ErpCrmTerritoryPipeline DTO）；Phase 4（13 cases 行为测试 + app-service.beans.xml 注册 + 日志 + roadmap Non-Goal boundary 标注）。

验证状态（full-green，2026-07-07）：
- `mvn test -pl module-crm/erp-crm-service -Dtest=TestErpCrmTerritoryQuota` → Tests run: 13, Failures: 0, Errors: 0
- `mvn test -pl module-crm/erp-crm-service -am` → BUILD SUCCESS（含本期新增 + 0700-1/0549-2 既有全绿，无回归）
- `mvn clean install -DskipTests`（全工作区 154+ reactor 模块）→ BUILD SUCCESS

Deferred（已 adjudicated，附触发条件）：ROUND_ROBIN/LOAD_BALANCED 完整挑人（团队成员表落地时）；真 DAG 区域模型（多父归属业务需求上线时）；区域管道报表前端（前端区域管理套件建立时）；线索分配异步批量（单批分配数超阈值时）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，closure-auditor，未复用执行者上下文）
- Audit Scope: 计划全文 + 4 Phase Exit Criteria + Closure Gates + Deferred 三件 + 实时仓库语义验证
- Live Repo Verification（grep/glob/read 非采信 [x]）:
  - `module-crm/model/app-erp-crm.orm.xml:214` — `ErpCrmLead.territoryId`(propId=41, BIGINT) + `to-one territory`(:237) + 索引 `IDX_CRM_LEAD_TERRITORY_ID`(:274) 已落地（Phase 1 Decision 落地证据）。
  - `module-crm/erp-crm-service/.../entity/ErpCrmTerritoryBizModel.java` — `createChild`/`moveTerritory`/`getTerritoryTree`/`defaultPrepareDelete` 四方法真实实现（回填 level/fullPath/isLeaf、子树递归重算、成环校验、子树深度校验、有子节点禁删），非空壳（Phase 2 落地证据）。
  - `module-crm/erp-crm-service/.../support/TerritoryAssignmentEngine.java` — 纯函数式 + `ConditionMatcher` 四 conditionType 分派（GEOGRAPHY/INDUSTRY/CUSTOMER_SIZE/CUSTOM_FIELD）+ default 兜底 + 非 MANUAL 方法降级 MANUAL（`degraded=true`）（Phase 2 落地证据）。
  - `module-crm/erp-crm-service/.../support/QuotaRollupCalculator.java` — `rollup`（显式值优先 + 子树聚合）、`distributeAnnual`（季/月均分 + 已定稿拒分）、`accumulatePipeline`（三段聚合 Quota+Forecast+Actual）真实实现（Phase 3 落地证据）。
  - `module-crm/erp-crm-service/.../entity/ErpCrmQuotaBizModel.java` — 5 方法（getQuotaRollup/finalizeQuota/unfinalizeQuota/distributeAnnualQuota/getTerritoryPipeline）@BizQuery/@BizMutation + IServiceContext 末参范式合规。
  - `module-crm/erp-crm-service/.../ErpCrmErrors.java:102-134` — 6 ErrorCode（`ERR_TERRITORY_MAX_DEPTH_EXCEEDED`/`ERR_TERRITORY_CYCLE`/`ERR_TERRITORY_HAS_CHILDREN`/`ERR_TERRITORY_NOT_ACTIVE`/`ERR_QUOTA_FINALIZED`/`ERR_QUOTA_NO_MATCH`）+ ARG_* 参数齐备。
  - `module-crm/erp-crm-service/src/main/resources/_vfs/erp/crm/beans/app-service.beans.xml:40-45` — `TerritoryAssignmentEngine` + `QuotaRollupCalculator` Bean 已注册（Anti-Hollow 通过：组件可达运行时）。
  - `module-crm/erp-crm-dao/.../entity/ErpCrmTerritoryPipeline.java` — DTO + 三内嵌 Summary（Quota/Forecast/Actual）落地。
  - `module-crm/erp-crm-service/src/test/java/.../TestErpCrmTerritoryQuota.java` — 13 `@Test` 方法（建子节点/移动/成环/深度/禁删/四 conditionType/default 兜底/reassign/rollup/finalize/distribute/已定稿拒分/管道三段）。
  - `docs/logs/2026/07-07.md:3-42` — 本计划日志条目在位（含 4 Phase 完成清单 + 验证状态 full-green + Decision P1/P3 记录）。
  - `docs/design/crm/territory.md` — owner 设计文档存在；本期补「实现注记」节（成员模型结论 + MANUAL 降级 + 加性列说明）。
- Five-Point Consistency: Plan Status `completed` = 4 Phase Status `completed` = 全 Exit Criteria `[x]` = 全 Closure Gates `[x]` = 日志验证状态 full-green，一致。
- Anti-Hollow: 未见空函数体 / `return null` 占位（`assign()` 返回 null 是 javadoc 明示的"无匹配"语义，非占位）/ Bean 注册但不可达问题。
- Deferred Honesty: ROUND_ROBIN/LOAD_BALANCED 降级附 Phase 1 Explore 实证（`ErpCrmTeamMember` 全仓 0 命中）+ 触发条件（团队成员表落地）；其余三 Deferred 均附触发条件，无范围内缺陷隐藏。
- Owner-Doc Drift: MANUAL 降级经 territory.md 实现注记 + Non-Goals + Deferred 三重登记，无静默漂移。
- Outcome: approved — 计划可正式关闭。

Follow-up:

- ROUND_ROBIN/LOAD_BALANCED 成员负载分配（见上方 Deferred）
- 真 DAG 区域模型（见上方 Deferred）
- 区域管道报表前端（见上方 Deferred）
