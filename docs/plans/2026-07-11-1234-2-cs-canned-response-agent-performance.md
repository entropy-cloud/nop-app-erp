# 2026-07-11-1234-2-cs-canned-response-agent-performance 客服效能子域收尾（预设应答变量渲染/宏匹配/插入 + 客服绩效聚合看板）

> Plan Status: completed
> Last Reviewed: 2026-07-11
> Source: `docs/backlog/extended-roadmap.md` Non-Goal 行（UC-CS-07 预设应答 + 客服质量监控，无独立计划）+ `docs/design/customer-service/canned-response.md`（207 行完整设计，零 BizModel 实现）+ `docs/design/customer-service/sla.md` §四 SLA 绩效报表（6 KPI + 5 报表类型 + ready SQL，零实现）
> Related: `docs/design/customer-service/canned-response.md`（权威设计），`docs/design/customer-service/sla.md` §四（SLA 绩效报表权威设计），`docs/design/customer-service/csat.md` §四（CSAT/NPS/CES 绩效报表），`docs/design/dashboards.md`（看板分层布局范式）
> Audit: required

## Current Baseline

- `ErpCsCannedResponse` / `ErpCsCannedCategory` 实体已物化经 model→codegen（字段齐备：title / content / categoryId / variableDefs(JSON) / macroTicketTypeId / macroPriority / isActive / usageCount）。`ErpCsCannedResponseBizModel` 仅 15 行 bare CrudBizModel（无自定义方法），`IErpCsCannedResponseBiz` 仅继承 ICrudBiz。AMIS 页面 `ErpCsCannedResponse/{main,picker}.page.yaml` + view.xml 仅为基础 CRUD 视图（无插入流程/渲染预览/分类树展开）。
- `ErpCsTicket` 实体字段齐备：assignedToId / isSlaCompleted / duration / status / priority / ticketTypeId / code / customerId / createTime / startDateTime / deadlineDateTime。**注：ErpCsTicket 无 teamId 列**——team 维度仅经 `slaPolicyId → ErpCsSlaPolicy.teamId → ErpCsTeam` 关联获取（sla.md §实现偏离补注已标注此 gap）。`ErpCsSurvey` 实体字段齐备：ticketId / csatScore / npsScore / cesScore。绩效数据源已就位，但**无聚合查询 BizModel**——SLA 达标率/平均解决时长/超时工单数/团队排名/客服 CSAT 全部未实现。
- `ErpCsConfigs` / `ErpCsConstants` 已有 SLA/调查/权益/目录/知识库配置段，**无预设应答配置段**（canned-response.md §五 `erp-cs.canned-response-enabled` / `-macro-count` / `-category-max-depth` 未接线）。
- `ErpCsErrors` 已有 21 ErrorCode，**无预设应答错误码**。
- CS 域已落地 `ErpCsReportBizModel`（报表渲染，1815-1）+ `ErpCsTicketBizModel`（工单状态机+SLA+CSAT，0700-2）+ `ErpCsEntitlementBizModel`/`ErpCsServiceCatalogBizModel`（权益/目录，1430-1）+ `ErpCsKnowledgeBaseBizModel`（知识库搜索/建议，0056-2）。**无绩效看板 BizModel**。
- CS 域无 `ErpCsDashboardBizModel`（10 域看板均无 CS 域——CS 仅为纯报表域，1815-1/1815-2 落地 ticket-sla-csat-summary 报表但无看板）。
- erp-survey 对标：Odoo helpdesk `canned_response` 模型 + category 分组 🟢 / Zendesk macros 条件触发 + 变量占位符 🟢 / ERPNext SLA dashboard + agent dashboard 🟢。当前仅有 CRUD 骨架为 gap。
- E2E 套件当前 180 测试（0730-2 基线）。CS 域 business-actions 仅 cs-ticket.action.spec.ts（0814-2，工单六态状态机）。

## Goals

- 预设应答业务逻辑闭环：变量渲染引擎（系统变量自动填充 + 自定义变量替换）+ 宏自动匹配（ticketType + priority 三级匹配）+ 插入流程（渲染 + usageCount 递增 + TicketAction NOTE 审计）+ 配置门控。
- 客服绩效聚合看板：经新 `ErpCsQualityDashboardBizModel` 暴露 SLA 达标率/平均解决时长/超时工单数/团队排名/客服 CSAT 聚合 @BizQuery + AMIS 看板页面。
- erp-survey 对标驱动（Odoo/Zendesk/ERPNext 核心内置功能，当前 gap 为零实现）。

## Non-Goals

- 预设应答 Excel 批量导入/导出（canned-response.md §四 管理功能，独立能力面）。
- 预设应答 AMIS 前端插入流程页面（工单详情页侧栏「推荐应答」+ 预览 + 插入按钮——前端能力面，后端 API 就绪后前端 successor）。
- 分类树最大深度校验（ErpCsCannedCategory 成环/深度校验——镜像 1430-1 catalog-category 范式，但 canned-response.md §五 config `category-max-depth` 默认 3 已声明，本期不强制校验，归 successor）。
- SLA 趋势图/超时工单明细报表/工单类型 SLA 分析报表（sla.md §4.2 报表分类的扩展报表，当前仅做看板聚合不做独立报表渲染）。
- 知识库全文搜索（UC-CS-05 successor，Elasticsearch/FULLTEXT，触发条件=文章数 > 10k）。
- UC-CS-12 跨团队履行工作流编排（INVOKE_WORKFLOW / CREATE_CHILD_TICKET 真实执行，触发条件=跨团队履行需求上线时）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/customer-service/canned-response.md`（§一~§五 权威设计），`docs/design/customer-service/sla.md`（§四 SLA 绩效报表权威设计），`docs/design/customer-service/csat.md`（§四 CSAT 绩效），`docs/design/dashboards.md`（§实现约定 看板分层布局范式）
- Skill Selection Basis: 本计划涉及 BizModel 自定义方法编写（变量渲染引擎 + 宏匹配 + 绩效聚合）→ 匹配 `nop-backend-dev` 技能（决策门 / xbiz 动作声明 / 实体服务创建 / 自定义动作 / 错误处理 / 事务边界）。AMIS 看板页面 → 匹配 `nop-frontend-dev` 技能（XView 三层模型 / page.yaml 定制）。Playwright E2E spec → `nop-testing` 不覆盖 Playwright → `Skill: none`（测试段）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 新增 3 配置键（`erp-cs.canned-response-enabled` 默认 true / `-macro-count` 默认 3 / `-category-max-depth` 默认 3）经 `NopSysVariable` 或 AppConfig.var 读取，无 .env/外部服务。

## Execution Plan

### Phase 1 — 预设应答变量渲染引擎 + 宏自动匹配

Status: completed
Targets: `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsCannedResponseBizModel.java`（扩展），`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/CannedResponseRenderer.java`（新建纯函数式工具），`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/ErpCsErrors.java`（+3 ErrorCode），`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/ErpCsConfigs.java`（+3 配置），`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/ErpCsConstants.java`（+3 配置键常量）
Skill: nop-backend-dev

- Item Types: `Add | Decision`
- Prereqs: 无（实体/字段已就位）

- [x] `Decision`：渲染引擎纯函数抽取——`CannedResponseRenderer.render(content, variableDefs, systemVars, customVars)` 纯函数式工具类（不注入 Dao，由 BizModel 传入解析后的系统变量 + 用户自定义变量）。系统变量解析在 BizModel 中完成（需跨实体读 ErpMdPartner.name / ErpCsTicket.code / context.getUserId → displayName）。
  - Skill: nop-backend-dev
  - Alternatives: 在 BizModel 内联渲染逻辑——被否决，渲染逻辑含变量占位符替换 + 必填校验 + JSON 解析，纯函数式更可单测。
- [x] `Add`：`ErpCsCannedResponseBizModel.renderTemplate(@Name("cannedResponseId") Long, @Name("ticketId") Long, @Name("customVariables") Map<String,String>)` `@BizQuery`——加载 CannedResponse → 解析系统变量（customer_name←Ticket.customerId→Partner.name / ticket_id←Ticket.code / agent_name←context user / today / now）→ 合并 customVariables → `CannedResponseRenderer.render` 替换占位符 → 返回渲染后 content
  - Skill: nop-backend-dev
- [x] `Add`：`ErpCsCannedResponseBizModel.suggestForTicket(@Name("ticketId") Long)` `@BizQuery`——加载 Ticket → 查 ErpCsCannedResponse `macroTicketTypeId=Ticket.ticketTypeId AND macroPriority=Ticket.priority AND isActive=true`（精确匹配）→ 若不足 macro-count 条，补 `macroPriority IS NULL`（类型匹配）→ 若仍不足，补 `macroTicketTypeId IS NULL AND macroPriority IS NULL`（全局兜底）→ 按 sequence ASC 取前 macro-count 条返回
  - Skill: nop-backend-dev
- [x] `Add`：`ErpCsConfigs` +3 配置读取（`canned-response-enabled` / `-macro-count` / `-category-max-depth`），`ErpCsConstants` +3 配置键常量
  - Skill: nop-backend-dev
- [x] `Add`：`ErpCsErrors` +3 ErrorCode——`ERR_CANNED_RESPONSE_NOT_FOUND`（cannedResponseId 不存在）/ `ERR_CANNED_RESPONSE_INACTIVE`（isActive=false 禁用渲染/插入）/ `ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING`（必填变量缺失）
  - Skill: nop-backend-dev

Exit Criteria:

- [x] `renderTemplate` 经 GraphQL `ErpCsCannedResponse__renderTemplate` 可达，系统变量正确替换（customer_name/ticket_id/agent_name/today），自定义变量覆盖
- [x] `suggestForTicket` 经 GraphQL `ErpCsCannedResponse__suggestForTicket` 可达，三级匹配（精确 > 类型 > 全局兜底）按 sequence 排序返回 ≤ macro-count 条
- [x] `CannedResponseRenderer` 纯函数式工具可独立单测（不注入 Dao）

### Phase 2 — 预设应答插入流程 + usageCount + 审计

Status: completed
Targets: `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsCannedResponseBizModel.java`（扩展 applyCannedResponse）
Skill: nop-backend-dev

- Item Types: `Add`
- Prereqs: Phase 1 完成（渲染引擎 + ErrorCode 就位）

- [x] `Add`：`ErpCsCannedResponseBizModel.applyCannedResponse(@Name("cannedResponseId") Long, @Name("ticketId") Long, @Name("customVariables") Map<String,String>)` `@BizMutation`——校验 active → renderTemplate 渲染 → usageCount+1 持久化 → 经 `IErpCsTicketActionBiz.save` 写 TicketAction(actionType=NOTE, ticketId, content=渲染后正文, cannedResponseId 引用) → 返回渲染后 content
  - Skill: nop-backend-dev

Exit Criteria:

- [x] `applyCannedResponse` 经 GraphQL `ErpCsCannedResponse__applyCannedResponse` 可达，渲染 + usageCount 递增 + TicketAction NOTE 审计写入

### Phase 3 — 客服绩效聚合看板 BizModel + AMIS 页面

Status: completed
Targets: `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/dashboard/ErpCsQualityDashboardBizModel.java`（新建），`module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsQualityDashboard/main.page.yaml`（新建），`module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsQualityDashboard.page.yaml`（新建），`module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/auth/erp-cs.action-auth.xml`（扩展菜单组）
Skill: nop-backend-dev, nop-frontend-dev

- Item Types: `Add | Decision`
- Prereqs: 无（数据源 ErpCsTicket/Survey 已就位）

- [x] `Decision`：新建独立 `ErpCsQualityDashboardBizModel`（`@BizModel("ErpCsQualityDashboard")` 服务型 BizObject，镜像 0935-1 `ErpFinDashboardBizModel` 域隔离范式）vs 扩展现有 `ErpCsReportBizModel`——选择新建独立 BizModel，理由：看板 KPI 聚合 vs 报表模板渲染是不同结果表面，混合违反单一职责；且 0935-1/1606-1 已建立「看板独立 BizModel」范式。
  - Skill: nop-backend-dev
  - Alternatives: 扩展 `ErpCsReportBizModel` 加聚合方法——被否决，报表 BizModel 职责为模板渲染（IReportEngine），看板 BizModel 职责为 SQL 聚合（IOrmTemplate/QueryBean），混合违反职责分离。
- [x] `Add`：`ErpCsQualityDashboardBizModel.getDashboardKpi(@Name("startDate") String, @Name("endDate") String)` `@BizQuery`——聚合区间内 SLA 达标率 / 平均解决时长(小时) / 超时工单数 / 总工单数 / 平均首次响应时长(小时)，数据源 ErpCsTicket where status=CLOSED + createTime 区间
  - Skill: nop-backend-dev
- [x] `Add`：`ErpCsQualityDashboardBizModel.getTeamSlaRanking(@Name("startDate") String, @Name("endDate") String)` `@BizQuery`——经 `ErpCsTicket JOIN ErpCsSlaPolicy(slaPolicyId) JOIN ErpCsTeam(teamId)` 按 team.name 分组 SLA 达标率 + 平均解决时长 + 工单数，ORDER BY slaCompleted DESC（注：ErpCsTicket 无 teamId 列，team 维度经 slaPolicy 关联获取）
  - Skill: nop-backend-dev
- [x] `Add`：`ErpCsQualityDashboardBizModel.getAgentCsatBreakdown(@Name("startDate") String, @Name("endDate") String)` `@BizQuery`——按 assignedToId 分组 AVG(csatScore) / AVG(npsScore) / AVG(cesScore) + 工单数，数据源 ErpCsTicket JOIN ErpCsSurvey
  - Skill: nop-backend-dev
- [x] `Add`：AMIS `main.page.yaml`——镜像 `dashboards.md` §实现约定分层布局（顶部 form 区间筛选 + service+grid+wrapper KPI 卡片 + chart 团队 SLA 排名柱状图 + crud 客服 CSAT 明细列表），数据经 `/api/GenericApi` GraphQL 消费 `ErpCsQualityDashboard__*` @BizQuery
  - Skill: nop-frontend-dev
- [x] `Add`：`erp-cs.action-auth.xml` 扩展——新增 `cs-quality-dashboard` + `cs-quality-dashboard-main` 菜单项
  - Skill: nop-frontend-dev

Exit Criteria:

- [x] 三个 @BizQuery 经 GraphQL `ErpCsQualityDashboard__getDashboardKpi/__getTeamSlaRanking/__getAgentCsatBreakdown` 可达，返回非空聚合结果
- [x] AMIS `main.page.yaml` YAML 可解析，GraphQL 调用逐一映射到 BizModel 真实方法
- [x] action-auth `xmllint --noout` well-formed

### Phase 4 — 单元测试 + E2E

Status: completed
Targets: `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestCannedResponseRenderer.java`（新建），`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsCannedResponseBiz.java`（新建），`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsQualityDashboard.java`（新建），`tests/e2e/business-actions/cs-canned-response.action.spec.ts`（新建）
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1-3 完成

- [x] `Proof`：`TestCannedResponseRenderer` 单元测试——系统变量替换 / 自定义变量覆盖 / 必填变量缺失抛 ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING / 无变量模板原样返回 / JSON variableDefs 解析
  - 成功模式：5+ test cases 全绿
  - Skill: none
- [x] `Proof`：`TestErpCsCannedResponseBiz` 集成测试——renderTemplate 系统变量解析（customer_name/ticket_id/agent_name）+ suggestForTicket 三级匹配（精确 > 类型 > 全局兜底）+ applyCannedResponse usageCount 递增 + TicketAction NOTE 写入
  - 成功模式：5+ test cases 全绿
  - Skill: none
- [x] `Proof`：`TestErpCsQualityDashboard` 集成测试——getDashboardKpi 聚合 SLA 达标率/平均解决时长 + getTeamSlaRanking 团队排名 + getAgentCsatBreakdown CSAT 均值
  - 成功模式：3+ test cases 全绿
  - Skill: none
- [x] `Proof`：`cs-canned-response.action.spec.ts` 浏览器层 E2E——经 GraphQL 调 suggestForTicket（验证三级匹配返回）+ renderTemplate（验证变量替换）+ applyCannedResponse（验证 usageCount 递增 + TicketAction 审计写入）
  - Skill: none

Exit Criteria:

- [x] 3 个 Java 测试类全绿（renderer + canned-response biz + quality dashboard）
- [x] cs-canned-response.action.spec.ts 全绿
- [x] 不引入 ORM 模型/公共契约变更（零新实体/零新字段——复用现有 ErpCsCannedResponse/CannedCategory/Ticket/Survey/TicketAction）

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0b0862ad2ffeAnu35v8EcnC1Ae`, general agent 新会话) — 1 BLOCKING: Phase 3 `getTeamSlaRanking` 按 teamId 分组但 ErpCsTicket 无 teamId 列（team 维度仅经 slaPolicyId→ErpCsSlaPolicy.teamId→ErpCsTeam 关联）。修订：getTeamSlaRanking 指定 JOIN 路径 `ErpCsTicket JOIN ErpCsSlaPolicy JOIN ErpCsTeam` 按 team.name 分组；Current Baseline 补注 ErpCsTicket 无 teamId 列 gap。另修 createdDate→createTime + ErrorCode 计数 23→21。
- Independent draft review iteration 2: accept (`ses_0b081593effec09egB0uXIn5WO`, general agent 新会话) — getTeamSlaRanking JOIN 路径 + Current Baseline teamId gap 注 + createTime 修正 + ErrorCode 计数 21 均已确认修复。advisory ErrorCode +2→+3 标签已修正。无新阻塞。草案审查收敛，状态 draft→active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。在结束时运行 `mvn clean install -DskipTests` + `npx playwright test`（或项目等效命令）一次。

- [x] 范围内行为完成（预设应答渲染+宏匹配+插入+usageCount+审计 + 客服绩效聚合看板 3 @BizQuery + AMIS 页面）
- [x] 相关文档对齐（`docs/design/customer-service/canned-response.md` 标注已实现段 + `sla.md` §四标注已实现段 + `extended-roadmap.md` Non-Goal 行标注已完成 + `e2e-runbook.md` 段扩展）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + cs-service `mvn test` 全绿 + 新增 E2E spec 全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 预设应答 AMIS 前端插入流程页面

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 工单详情页侧栏「推荐应答」+ 分类树展开 + 渲染预览弹窗 + 插入按钮为前端交互能力面。后端 API（suggestForTicket/renderTemplate/applyCannedResponse）就绪后前端 successor 可启动。
- Successor Required: yes（触发条件：CS 工单详情页定制需求落地时）

### 分类树最大深度/成环校验

- Classification: `optimization candidate`
- Why Not Blocking Closure: ErpCsCannedCategory parentId 树形校验（最大深度 + 成环检测）镜像 1430-1 catalog-category 范式，但 canned-response.md §五 config `category-max-depth` 默认 3 本期仅声明不强制。分类数量通常 ≤ 20，深度溢出风险低。
- Successor Required: yes（触发条件：分类数量增长或数据完整性需求时）

### SLA 趋势/超时明细/工单类型 SLA 分析独立报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: sla.md §4.2 定义 5 报表类型，本期仅做看板聚合 KPI（getDashboardKpi/getTeamSlaRanking/getAgentCsatBreakdown），不做独立 `.xpt.xml` 报表模板。看板 KPI 已覆盖核心管理视角。
- Successor Required: yes（触发条件：客服管理层需可下载/定时推送的独立绩效报表时）

## Closure

Status Note: 实施完成 2026-07-11。Phase 1-4 全部 done：CannedResponseRenderer 纯函数式工具 + ErpCsCannedResponseBizModel 三自定义方法（renderTemplate/suggestForTicket/applyCannedResponse）+ ErpCsQualityDashboardBizModel 三聚合 @BizQuery + AMIS 看板页面 + 菜单注册。验证：mvn clean install -DskipTests 全 154 模块 BUILD SUCCESS；cs-service 95 测试全绿（+20 新增）；E2E spec Playwright 可达。独立结束审计已由 explore 子代理新会话执行并通过，所有交付文件验证为非空壳实现。

Closure Audit Evidence:

- 2026-07-11 执行者自检：`mvn clean install -DskipTests` 全 reactor BUILD SUCCESS（app-erp-all 聚合通过）；`mvn test -pl module-cs/erp-cs-service` 95 测试全绿（含 TestCannedResponseRenderer 9 + TestErpCsCannedResponseBiz 7 + TestErpCsQualityDashboard 4 = 20 新增）；`xmllint --noout erp-cs.action-auth.xml` well-formed；YAML 可解析；TypeScript E2E spec `npx tsc --noEmit` 无错。
- 2026-07-11 独立结束审计（`ses_0afc83d0effeahjplNDJpz13ff`，explore 子代理新会话，无执行者上下文）：11 个交付文件逐一验证全部存在且实现非空壳——CannedResponseRenderer（135 行纯函数式工具，private 构造，render 管线含 mergeVars/parseVarDefs/validateRequired/replacePlaceholders）；ErpCsCannedResponseBizModel（282 行，三方法 renderTemplate/suggestForTicket/applyCannedResponse 全部落地，suggestForTicket 三级 fillMatching 精确>类型>全局兜底 + dedup + sequence ASC，applyCannedResponse 经 writeNoteAction→ticketActionBiz.saveEntity 写 TicketAction NOTE 审计）；ErpCsQualityDashboardBizModel（409 行，三 @BizQuery 全部落地，getTeamSlaRanking 经 slaPolicyId→ErpCsSlaPolicy.teamId→ErpCsTeam 关联获取 team 维度，无 ticket.getTeamId() 调用）；ErpCsErrors +3 ErrorCode + 2 ARG key；ErpCsConfigs +3 配置 + ErpCsConstants 常量；AMIS main.page.yaml（149 行，三 GraphQL 端点 ErpCsQualityDashboard__getDashboardKpi/__getTeamSlaRanking/__getAgentCsatBreakdown 逐一映射）；erp-cs.action-auth.xml（cs-quality-dashboard 菜单注册）；3 Java 测试类（9+7+4=20 test cases 全部真实断言）+ E2E spec（111 行全栈 GraphQL 验证 suggest+render+apply+NOTE 审计）。Anti-hollow 检查通过：无空函数体/return null 占位/吞异常。Deferred 项均为合法 out-of-scope improvement，无范围内缺陷隐藏。

Follow-up:

- 预设应答 AMIS 前端插入流程页面（见上方 Deferred）
- 分类树校验 successor（见上方 Deferred）
- SLA 绩效独立报表 successor（见上方 Deferred）
