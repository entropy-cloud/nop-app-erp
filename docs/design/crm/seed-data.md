# CRM 域种子数据（Seed Data）

> Owner: 本文件（crm 域「种子数据」owner doc；plan `2026-09-01-2255-3-m14a-crm-cs-seed-expansion.md` Phase 2 落地）
> 上游规格: `docs/architecture/seed-data.md` 规格表 crm 节（30 实体逐行规格权威源）+ 对账表（计数权威源）
> 业务语义 owner docs: `docs/design/crm/`（lead-scoring / sales-sequence / sales-forecast / territory / cpq / marketing 等）；本文件只登记种子数据面，不重复业务语义

## 1. 范围与文件清单

M1.4a 批次（2026-09-02）为 crm 域补齐 30 个 seed CSV（既有 5 表 stage / lead / forecast_period / forecast / forecast_line 由 plan `1045-1` 落地，本批不触碰）。文件位于 `app-erp-all/src/main/resources/_vfs/_init-data/`，命名 = `<tableName>.csv`（`DataInitInitializer.loadCsvData` 按表名查找契约）。至此 crm 域 35 规格实体全量 seed 覆盖（35 / 35）。

| CSV | 行数 | 主子表 | 用例指示 | 说明 |
|---|---|---|---|---|
| erp_crm_lead_status.csv | 2 | 头/独立 | P | 新建 / 跟进中状态字典 |
| erp_crm_source.csv | 3 | 头/独立 | P | 官网表单 / 行业展会 / 客户转介绍 |
| erp_crm_lost_reason.csv | 2 | 头/独立 | P | 价格因素 / 功能不满足 |
| erp_crm_team.csv | 2 | 头（team 组头） | P | 华东销售一组 / 华南销售二组 |
| erp_crm_team_member.csv | 2 | 子表（team） | P | team1/team2 各 1 成员（USER_ID→nop_auth_user 3） |
| erp_crm_campaign.csv | 2 | 头/独立 | P | 夏季促销 / 行业展会（UTM medium+source） |
| erp_crm_event_category.csv | 2 | 头/独立 | P | 会议 / 电话（日历颜色） |
| erp_crm_event.csv | 3 | 头/独立 | P+N-TERM | COMPLETED×2 + CANCELLED（终态）；RELATED_LEAD_ID→lead 1/2 |
| erp_crm_activity.csv | 2 | 头/独立 | P | CALL + MEETING 记录（LEAD_ID 必填→lead 1/2） |
| erp_crm_lead_conv_log.csv | 2 | 头/独立 | P | lead1/lead2 阶段流转（stage1→stage2），CHANGED_AT 静态 |
| erp_crm_lead_funnel.csv | 2 | 头（funnel 组头） | P | 全量漏斗 + 华东区域漏斗（聚合指标行） |
| erp_crm_funnel_stage_metrics.csv | 2 | 头/独立（FK→funnel+stage） | P | funnel1 × stage1（验证）/ stage2（报价）度量行 |
| erp_crm_lead_score_config.csv | 2 | 头（score config 组头） | P+N-DIS | 默认规则集（active）+ 大客户规则集（N-DIS inactive） |
| erp_crm_lead_score_config_line.csv | 3 | 子表（lead_score_config） | P | config1 两准则（LOOKUP/FORMULA）+ config2 一准则（BOOLEAN） |
| erp_crm_lead_score.csv | 2 | 头（score 组头） | P | lead1 总分 85（AUTO_QUALIFY）+ lead2 总分 60 |
| erp_crm_lead_score_line.csv | 3 | 子表（lead_score） | P | score1 两明细 + score2 一明细（CONFIG_LINE_ID→本批） |
| erp_crm_sequence.csv | 2 | 头（sequence 组头） | P+N-DIS | 新线索序列（active+default）+ 谈判序列（N-DIS inactive） |
| erp_crm_sequence_assignment.csv | 2 | 子表（sequence） | P+N-DIS | seq1 LEAD_SOURCE（active）+ seq2 TERRITORY（N-DIS inactive） |
| erp_crm_sequence_step.csv | 3 | 子表（sequence） | P | seq1 两步（CALL/EMAIL）+ seq2 一步（MEETING） |
| erp_crm_lead_seq_progress.csv | 2 | 头/独立 | P+N-TERM | lead1 IN_PROGRESS + lead2 COMPLETED（终态） |
| erp_crm_territory.csv | 2 | 头（territory 组头） | P+N-DIS | 华东大区（active）+ 华南大区（N-DIS inactive，PARENT_ID→1） |
| erp_crm_territory_assignment_rule.csv | 2 | 子表（territory） | P+N-DIS | territory1 GEOGRAPHY（active）+ territory2 INDUSTRY（N-DIS inactive） |
| erp_crm_quota.csv | 2 | 头/独立 | P | 2026Q3 季度配额（territory+team+owner）+ 2026 年度配额 |
| erp_crm_forecast_accuracy.csv | 2 | 头/独立 | P | 挂 forecast 1/period 1〔已seed〕的承诺/乐观准确率两行 |
| erp_crm_product_configurator.csv | 2 | 头（configurator 组头） | P+N-DIS | 标准选配向导（active）+ 定制向导（N-DIS inactive） |
| erp_crm_config_rule.csv | 2 | 子表（product_configurator） | P | REQUIRED + EXCLUDED 规则各 1 |
| erp_crm_bundle_pricing.csv | 2 | 头（bundle 组头） | P+N-DIS | 入门捆绑包（PERCENTAGE）+ 旗舰捆绑包（FIXED，N-DIS inactive） |
| erp_crm_bundle_pricing_line.csv | 3 | 子表（bundle_pricing） | P | bundle1 两行 + bundle2 一行（PRODUCT_ID→erp_md_material 1/2） |
| erp_crm_price_rule.csv | 2 | 头/独立 | P+N-DIS | VOLUME 阶梯价（active）+ PROMOTIONAL 促销价（N-DIS inactive） |
| erp_crm_quote_template.csv | 2 | 头/独立 | P | 标准模板 + 默认模板（IS_DEFAULT=true） |

## 2. FK 闭环图

- **〔已 seed〕锚点**：`ErpCrmStage`（ID 1/2）、`ErpCrmLead`（ID 1/2）、`ErpCrmForecastPeriod`（ID 1）、`ErpCrmForecast`（ID 1）、`erp_md_organization`（ORG_ID=2）、`erp_md_partner`（ID 1/2）、`erp_md_material`（ID 1/2）、`erp_md_currency`（ID 1）、`nop_auth_user`（ID 3/20，userId 语义列）。
- **〔本批〕链路**（CRM 6 组主子表 + 交叉引用）：
  - team → team_member；territory → territory_assignment_rule（含 territory 自引用 PARENT_ID 2→1）；
  - lead_score_config → lead_score_config_line；lead_score → lead_score_line（→ config_line 交叉引用）；
  - sequence → sequence_step + sequence_assignment；sequence → lead_seq_progress（→ lead〔已seed〕）；
  - product_configurator → config_rule；bundle_pricing → bundle_pricing_line（→ erp_md_material〔跨域:md·已seed〕）；
  - lead_funnel → funnel_stage_metrics（→ stage〔已seed〕）；
  - 事件/活动/日志面：event_category → event（→ lead〔已seed〕+ partner〔跨域:md·已seed〕）；activity / lead_conv_log → lead〔已seed〕+ stage〔已seed〕；forecast_accuracy → forecast + forecast_period〔已seed〕+ team + territory〔本批〕。
- 全部必填 FK 落在〔已 seed〕∪〔本批〕集合内（`TestErpSeedDataIntegrity` 引用完整性断言门禁，白名单零增量）；可选 FK 一律留空（event.contactId、lead_score.configId 之外的可选维度等按行留空）。

## 3. 用例指示编码与 negative 行语义

- **P（最小正例行）**：每表主行，字段值取规整演示值（金额 `.0000` 四位小数、静态日期 2026-07-01 ~ 2026-10-05）。
- **N-TERM（终态行）**：event 3（CANCELLED）、lead_seq_progress 2（COMPLETED）——供业务动作非法迁移守卫负路径；全部静态时间戳，无滚动期间语义（冻结时钟纪律）。
- **N-DIS（禁用/停用行）**：lead_score_config 2 / sequence 2 / sequence_assignment 2 / territory 2 / territory_assignment_rule 2 / product_configurator 2 / bundle_pricing 2 / price_rule 2（统一 `IS_ACTIVE=false` 或 `IS_DEFAULT=false` + REMARK 后缀（N-DIS））——供启用前置守卫负路径。
- 行级用途以 REMARK 后缀（P）/（N-TERM）/（N-DIS）显式标注（沿 mnt/qa 批先例），与 `docs/architecture/seed-data.md`「seed 数据分层裁决」节 negative seed 语义一致。

## 4. 干扰面零漂移设计（本批核验结论）

- **campaignAttribution 报表零漂移**：`ErpCrmReportBizModel` campaignAttribution 数据集按 lead.campaignId 聚合后经 `resolveCampaignNames` 读 `ErpCrmCampaign`（`:292`，`:293` 空集短路实证）——既有 `erp_crm_lead.csv` 无 CAMPAIGN_ID 列 + 本批零新增 lead 行 → 聚合集空 → 本批 campaign 种子行不被该报表名解析消费。lead-conversion-funnel / forecast-accuracy 两数据集读 lead+stage / forecast 族〔已seed〕，零交集。
- **E2E 数值断言面零影响**：`crm.list-value` 断言 `ErpCrmLead` expectedCount=2（零新增 lead 行）；crm-lead.action / cross-doc-navigation.action 自建自清 + 存在性断言；3 个 report value spec token 断言消费〔已seed〕行。
- **视觉面零影响**：`ext-domains-list-filter` 对 `/ErpCrmFunnelStageMetrics-main` 为 readonly 无增删改按钮结构断言 + label 可见性断言，不消费行数据。
- **集成快照零漂移**：`app-erp-all/_cases` crm 相关用例（TestErpC15CrmLeadForecast）仅经 `@var:` 自建实体断言，无种子计数断言；纯加性插入不入既有快照（M1.2b 实证先例）。

## 5. 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准）；省略审计列；日期 ISO（`2026-07-01`）、时间戳空格分隔（`2026-07-03 10:00:00`，对齐同域既有 seed 与 mnt 批先例）；小写布尔；ID < 100000（各表独立自 1 起段）；字典码 ∈ `module-crm/model/app-erp-crm.orm.xml` `<dicts>`（erp-crm/ 命名空间 21 字典）。
- 拓扑序由 `DataInitInitializer` 按 ORM `getEntityModelsInTopoOrder()` 自动排序，同批主子表无需手工排序文件名。
