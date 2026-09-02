# CS 域种子数据（Seed Data）

> Owner: 本文件（cs 域「种子数据」owner doc；plan `2026-09-01-2255-3-m14a-crm-cs-seed-expansion.md` Phase 2 落地）
> 上游规格: `docs/architecture/seed-data.md` 规格表 cs 节（15 实体逐行规格权威源）+ 对账表（计数权威源）
> 业务语义 owner docs: `docs/design/customer-service/`（sla / service-catalog / entitlement / canned-response / time-tracking / csat 等）；本文件只登记种子数据面，不重复业务语义

## 1. 范围与文件清单

M1.4a 批次（2026-09-02）为 cs 域补齐 15 个 seed CSV（既有 3 表 ticket_type / ticket / survey 由 plan `1045-1` 落地，本批不触碰；`nop_sys_code_rule` 的 cs-ticket-code 规则行由 plan `0330-1` 落地，属平台 CSV 不在本域计数）。文件位于 `app-erp-all/src/main/resources/_vfs/_init-data/`，命名 = `<tableName>.csv`。至此 cs 域 18 规格实体全量 seed 覆盖（18 / 18），ticket 全生命周期周边表完整。

| CSV | 行数 | 主子表 | 用例指示 | 说明 |
|---|---|---|---|---|
| erp_cs_team.csv | 2 | 头/独立 | P | 一线客服组 / 二线技术支持组（TEAM_LEADER_ID→nop_auth_user 3） |
| erp_cs_sla_policy.csv | 2 | 头/独立 | P | 紧急 4 小时（type1/URGENT/team1）+ 普通 24 小时（type2/NORMAL/team2），二级升级链（user 3→20） |
| erp_cs_agent_rate.csv | 2 | 头/独立 | P+N-DIS | WARRANTY 200（active）+ SUPPORT_CONTRACT 350（N-DIS inactive）；AGENT_ID→erp_md_employee 1/2 |
| erp_cs_canned_category.csv | 2 | 头（canned 组头） | P | 问候应答 / 售后应答分类 |
| erp_cs_canned_response.csv | 2 | 头/独立（FK→canned_category） | P+N-DIS | 欢迎语模板（active，匹配 type1/NORMAL）+ 停用旧版致歉模板（N-DIS inactive） |
| erp_cs_knowledge_base.csv | 2 | 头/独立（FK→canned_category） | P | 密码重置指引（PUBLISHED）+ 报表导出说明（未发布） |
| erp_cs_contract.csv | 2 | 头/独立 | P+N-TERM | 华东年度合同（ACTIVE）+ 华南旧版合同（EXPIRED 终态，2025 窗口静态日期）；BUSINESS_DATE 必填 |
| erp_cs_entitlement.csv | 2 | 头/独立 | P+N-DIS | 年度保修权益（active，→partner 1〔跨域:md·已seed〕+contract 1+sla 1）+ 停用付费支持权益（N-DIS inactive） |
| erp_cs_catalog_category.csv | 2 | 头（catalog 组头） | P+N-DIS | 实施服务（active）+ 培训服务（N-DIS inactive） |
| erp_cs_service_catalog_item.csv | 2 | 头（catalog item 组头） | P+N-DIS | 标准实施服务（active+public，→type1+sla 1）+ 进阶培训服务（N-DIS inactive） |
| erp_cs_catalog_fulfillment.csv | 3 | 子表（service_catalog_item） | P | item1 两步（CREATE_TICKET/ASSIGN_TEAM，mandatory）+ item2 一步（NOTIFY_CUSTOMER，非强制） |
| erp_cs_ticket_action.csv | 3 | 头/独立 | P | ticket1 ASSIGN（NEW→IN_PROGRESS）+ CLOSE（IN_PROGRESS→RESOLVED）+ ticket2 NOTE，与既有 ticket 状态轴自洽 |
| erp_cs_ticket_fulfillment_step.csv | 3 | 头/独立（FK→ticket+fulfillment） | P+N-TERM | ticket1 两步 DONE + ticket2 一步 FAILED（终态，RETRY_COUNT=2）；UK(ticket,fulfillment) 零重复 |
| erp_cs_ticket_timer_session.csv | 3 | 头/独立 | P+N-TERM | 三段 STOPPED 会话（终态；ACTIVE_FLAG 留空满足 UK(agent,active) 单活跃约束，NULL-UK 放行历史行）；其一含暂停 10 分钟 |
| erp_cs_time_entry.csv | 2 | 头/独立 | P | ticket1/ticket2 计时行（APPROVED + PENDING；SOURCE=MANUAL/TIMER_IMPORT；金额 = rate × 1h 与 agent_rate 对账） |

## 2. FK 闭环图

- **〔已 seed〕锚点**：`ErpCsTicket`（ID 1/2）、`ErpCsTicketType`（ID 1/2）、`erp_md_partner`（ID 1/2，entitlement/contract 跨域边）、`erp_md_organization`（ORG_ID=2）、`erp_md_employee`（ID 1/2，agent_rate/time_entry 的 AGENT_ID BIGINT 列）、`nop_auth_user`（ID 3/20，userId 语义列与 SLA 升级人 BIGINT 列）。
- **〔本批〕链路**（cs ticket 全生命周期周边 + catalog 链）：
  - team → sla_policy（TEAM_ID）；sla_policy ← service_catalog_item（SLA_POLICY_ID）/ entitlement；
  - canned_category → canned_response + knowledge_base（CATEGORY_ID）；
  - contract → entitlement（CONTRACT_ID）；entitlement.partnerId → erp_md_partner〔跨域:md·已seed〕；
  - catalog_category → service_catalog_item → catalog_fulfillment → ticket_fulfillment_step（→ ticket〔已seed〕+ catalog_item 快照列）；
  - ticket 周边面：ticket_action / time_entry / ticket_timer_session / ticket_fulfillment_step 全部 TICKET_ID → ticket 1/2〔已seed〕。
- 全部必填 FK 落在〔已 seed〕∪〔本批〕集合内（`TestErpSeedDataIntegrity` 引用完整性断言门禁，白名单零增量）；可选 FK 一律留空（ticket_fulfillment_step 的 EXECUTED_BY、canned_response 的 VARIABLE_DEFS 等）。

## 3. 用例指示编码与 negative 行语义

- **P（最小正例行）**：每表主行，字段值取规整演示值（费率/金额 `.0000`、静态日期 2025-10-01 ~ 2026-07-04）。
- **N-TERM（终态行）**：contract 2（EXPIRED）、ticket_fulfillment_step 3（FAILED）、ticket_timer_session 全部（STOPPED 终态会话）——供守卫负路径；全部静态时间戳（冻结时钟纪律，timer_session/fulfillment_step 时效类实体尤其注意）。
- **N-DIS（禁用/停用行）**：agent_rate 2 / canned_response 2 / catalog_category 2 / service_catalog_item 2 / entitlement 2（统一 `IS_ACTIVE=false` + REMARK/名称停用语义词 +（N-DIS）后缀）——供启用前置守卫负路径。
- 行级用途以 REMARK 或名称语义 +（P）/（N-TERM）/（N-DIS）显式标注，与 `docs/architecture/seed-data.md`「seed 数据分层裁决」节 negative seed 语义一致。

## 4. 唯一约束与状态轴自洽

- **UK_CS_TIMER_SESSION_AGENT_ACTIVE（agentId, activeFlag）单活跃计时器**：3 行全部 STOPPED 且 ACTIVE_FLAG 留空（NULL）——NULL-UK 可重复放行历史行（owner doc `time-tracking.md` 语义），零冲突。
- **UK_CS_TICKET_FULFILLMENT_STEP（ticketId, fulfillmentId）幂等物化**：3 行 (1,1)/(1,2)/(2,1) 零重复。
- **ticket_action 状态轴自洽**：既有 ticket 1 = RESOLVED、ticket 2 = IN_PROGRESS；动作行 from/to 与其衔接（ticket1: NEW→IN_PROGRESS→RESOLVED；ticket2: IN_PROGRESS 内 NOTE）。
- **time_entry 金额对账**：BILLABLE_AMOUNT = BILLING_RATE × (DURATION/60)，与 agent_rate 费率行一致（200/350）。

## 5. 干扰面零漂移设计（本批核验结论）

- **quality dashboard 空集短路**：`ErpCsQualityDashboardBizModel` 经 `slaPolicyId → teamId` 关联读 `ErpCsSlaPolicy`/`ErpCsTeam`（`:271 loadSlaPolicyTeamMap` / `:285 loadTeamNames`，`:272`/`:287` 空集短路实证）——既有 `erp_cs_ticket.csv` 无 SLA_POLICY_ID 列（grep=0）→ 空集短路，本批 sla_policy/team 种子行不被该面板聚合。
- **ticket-sla-csat 报表零影响**：`ErpCsReportBizModel` 读 Ticket + Survey〔均已seed〕，本批零新增 ticket/survey 行。
- **E2E 数值断言面零影响**：`cs.list-value` 断言 `ErpCsTicket` expectedCount=2；cs-ticket.action / cs-canned-response.action / cs-kb-suggestion.smoke 自建自清 + 存在性断言（canned-response 种子行与 spec 自建行零冲突）。
- **集成快照零漂移**：`app-erp-all/_cases` cs 相关用例（TestErpC04SalReturnWithCs）仅经 `@var:` 自建实体断言；纯加性插入不入既有快照（M1.2b 实证先例）。

## 6. 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准）；省略审计列；日期 ISO（`2026-07-01`）、时间戳空格分隔（`2026-07-03 09:00:00`，对齐同域既有 seed 与 mnt 批先例）；小写布尔；ID < 100000（各表独立自 1 起段）；字典码 ∈ `module-cs/model/app-erp-cs.orm.xml` `<dicts>`（erp-cs/ 命名空间 17 字典）。
- 拓扑序由 `DataInitInitializer` 按 ORM `getEntityModelsInTopoOrder()` 自动排序，同批主子表无需手工排序文件名。
