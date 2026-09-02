# 2026-09-01-2255-3 M1.4a 第一批扩展域 seed 扩面 A——crm + cs（45 CSV）

> Plan Status: completed
> Last Reviewed: 2026-09-02
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.4a（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁常量随批更新协议）、同批 `2026-09-01-2255-1-m12c-ast-notify-seed-expansion.md`（N=1）/ `2026-09-01-2255-2-m13-hr-seed-expansion.md`（N=2）
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体（className 口径；运行时注册 368 = 363 + finance 5 缺 className 补充档，本域不涉及），218 个有 seed（`_init-data/` 实测 222 CSV = 218 app.erp + 4 平台，+ 1 SQL）；`TestErpSeedDataIntegrity` 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 218` / `EXPECTED_PLATFORM_CSV_COUNT = 4` / `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（+ finance 5 缺 className 补充集，非本域不触碰）。
- 两域实测已 seed / 缺 seed（与 roadmap M1.4a 清单一致）：
  - crm：已 seed 5（stage / lead / forecast_period / forecast / forecast_line，1045-1），缺 **30**（activity / bundle_pricing / bundle_pricing_line / campaign / config_rule / event / event_category / forecast_accuracy / funnel_stage_metrics / lead_conv_log / lead_funnel / lead_score / lead_score_config / lead_score_config_line / lead_score_line / lead_seq_progress / lead_status / lost_reason / price_rule / product_configurator / quota / quote_template / sequence / sequence_assignment / sequence_step / source / team / team_member / territory / territory_assignment_rule）；
  - cs：已 seed 3（ticket_type / ticket / survey，均 1045-1；0330-1 贡献的是 `nop_sys_code_rule` cs-ticket-code 规则行），缺 **15**（agent_rate / canned_category / canned_response / catalog_category / catalog_fulfillment / contract / entitlement / knowledge_base / service_catalog_item / sla_policy / team / ticket_action / ticket_fulfillment_step / ticket_timer_session / time_entry）。
  - 逐实体规格见 `docs/architecture/seed-data.md` 规格表 crm / cs 两节，本计划逐行引用不复制。
- 既有 FK 锚点已就绪：`ErpCrmStage` / `ErpCrmLead` / `ErpCrmForecastPeriod`〔已seed〕（crm 规格表 6 行必填 FK 目标）+ `ErpCsTicket`〔已seed〕（cs 规格表 4 行必填 FK 目标）+ `ErpMdPartner`〔跨域:md·已seed〕（cs entitlement 必填 FK）。
- 当前验证基线：`mvn test -pl app-erp-all` **71/0/0/1** 全绿（M0.2 实测，经六批 M1.x 维持）；compliance 门控锚点 = `docs/audits/compliance-baseline.md` §BASELINE 机器块（R2c: 1537），**R2c=1542** 为历史实测值（+5 pre-existing 已登记，沿 plan `2026-09-01-0527-1` 口径协议）；全 reactor 已知 2 处预存回归（hr/drp）为 roadmap Non-Goal，与本计划无关。
- 既有 CSV 约定：列头为 DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；装载拓扑序由 `DataInitInitializer` 自动排序。
- 冻结时钟纪律（`docs/bugs/2026-09-01-0017` / `2026-09-01-0058` 家族教训）：本批全部 seed 行日期列使用静态固定值，禁止 `now()`/滚动期间语义（event / activity / contract / ticket_timer_session / sla_policy / lead_seq_progress / sequence_assignment / forecast_accuracy 时效类实体尤其注意）。
- **干扰面**（本计划核心风险，读取面均 2026-09-01 实仓核实）：`ErpCrmReportBizModel` 三个数据集——lead-conversion-funnel 读 lead+stage、forecast-accuracy 读 forecast 族（均〔已seed〕不在本批 30 表）、**campaignAttribution 按 lead.campaignId 聚合后经 resolveCampaignNames 读 `ErpCrmCampaign`（本批表）**——零漂移前提 = 既有 `erp_crm_lead.csv` 无 CAMPAIGN_ID 列（聚合集空 → 名解析空集短路）+ 本批不修改既有 CSV、不新增 lead 行；`ErpCsReportBizModel` ticket-sla-csat 读 `ErpCsTicket` + `ErpCsSurvey`（均〔已seed〕不在本批 15 表）；`ErpCsQualityDashboardBizModel` 读 Ticket/Survey〔已seed〕+ **`ErpCsSlaPolicy` / `ErpCsTeam`（本批表）**（经 slaPolicyId → teamId 关联）——零漂移静态论证 = 既有 `erp_cs_ticket.csv` 无 SLA_POLICY_ID 列 → `loadSlaPolicyTeamMap`/`loadTeamNames` 空集短路；E2E 消费面 = `crm.list-value` + `cs.list-value`（断言 `ErpCsTicket` expectedCount=2，本批零影响）/ `crm-lead.action.spec` / `cs-ticket.action.spec` / `cs-canned-response.action.spec` / `cs-kb-suggestion.smoke.spec` / `cross-doc-navigation.action.spec`（测试 6/7 消费 `ErpCrmActivity` / `ErpCsTicketAction`，自建自清 + 存在性断言，种子零破坏）/ `ext-domains-list-filter` visual spec（`/ErpCrmFunnelStageMetrics-main` readonly 结构型断言，种子零影响；`ext-domains-child-table` 实测仅覆盖 logistics/b2b/contract/hr/drp，不在本批消费面）；`app-erp-all/_cases` 集成用例 grep `ErpCrm|ErpCs` 零直接引用（2026-09-01 实证），集成快照 DB 状态面按 `_chgType` 增量机制评估（M1.2b 实证纯加性插入不入既有快照）。
- **保护区域**：无——crm/cs 两域规格表行不触 ORM、敏感字段、会计过账、外部集成、视觉 mask 五类保护区；标准草案审查即可，无独立 plan-audit 义务。
- **Deferred 消费**：seed-data.md L352「CRM/CS/HR 域配置/执行链 seed」Deferred 之 CRM 子集（config_rule / price_rule / bundle_pricing / territory / team / campaign）+ CS 子集（knowledge_base / sla_policy / entitlement / catalog）——本计划按 roadmap M1 全量覆盖口径消费该 Deferred（触发条件由全覆盖目标取代，沿 M1.2b 消费 L297 先例）。
- **owner doc 目录映射**：roadmap M1.4a 行记 `docs/design/crm/` + `cs/`；物理目录为 `docs/design/crm/` + `docs/design/customer-service/`（cs 域 owner doc 既有宿主目录），本计划 owner doc 落后者。

## Goals

- 45 个 seed CSV 落地（crm 30 + cs 15，逐行消费规格表两节），每实体最小可用数据集（行数 ≤ 20，按规格表建议行数与用例指示编码 P / N-TERM / N-DIS），FK 引用零悬空，CRM 6 组主子表（bundle_pricing+line / lead_score+line / lead_score_config+line / sequence+assignment+step / team+member / territory+assignment_rule）链路 + cs ticket 全生命周期周边表完整。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（基数以「218 + 已落地批次 CSV 数」为准，本批 +45），seed-data.md 对账表同步。
- `docs/design/crm/seed-data.md` + `docs/design/customer-service/seed-data.md`「种子数据」owner doc 段落地（2 个新文件）。

## Non-Goals

- 不修改任何 ORM 模型（保护区 auto + dual-agent-approval）——本计划纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` 的 CSV 基线常量按内置协议更新）；不触碰 lead scoring / sequence / SLA / catalog 引擎逻辑（seed 为被动数据）。
- 不覆盖 aps / logistics / b2b / contract / drp 及其余 M1.x 工作项的缺 seed 实体（归 M1.4b/M1.5 后续批次）；不触碰 finance 5 个缺 className 运行时实体。
- 不修改既有 8 个 crm/cs CSV 文件的任何行；不修改 `nop_sys_code_rule` cs-ticket-code 既有行。
- 不新增 CRM/CS 域 GL 凭证/业财一体 seed（seed-data.md L351 Deferred 维持）。
- 不新增 M2.x 像素断言、不做负向视觉断言、不做跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 hr/drp 2 处预存回归；不引入 production-grade 真实个人数据。
- 不做「seed 字段值与 owner doc 业务规则一致性」的逐字段回放测试（roadmap Non-Goal）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 crm/cs 两节 + 对账表 + L352 Deferred）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.4a 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）、`docs/design/crm/` + `docs/design/customer-service/`（lead-scoring / sales-sequence / sla / service-catalog / entitlement 等 owner doc）
- Skill Selection Basis: roadmap M1.4a 行指定 `nop-backend-dev`（数据资产须对齐实体/字典/列命名约定）；Proof 阶段运行测试套件与视觉 spec 属测试域，加载 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载；`init-database-data: true` 已默认开启）。
- 回滚策略：seed CSV 为纯新增文件，回滚 = 删除本批新增文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：本计划为同批三计划（N=1/2/3）之三；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，若 N=1/N=2 先落地，常量基数以「218 + 已落地批次 CSV 数」为准，避免覆盖写。

### Phase 1 - Seed CSV authoring（45 CSV = 30 + 15）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪；规格表 crm/cs 两节全部必填 FK 锚点均已 seed 或属本批）

- [x] 逐实体核对 ORM 表名与列（`module-crm` / `module-cs` 两域 `model/*.orm.xml` tableName / `code=`）后，按规格表 crm / cs 两节（45 行，逐行引用不复制）创建 45 个 CSV；crm 主子表组 6 组（bundle_pricing / lead_score / lead_score_config / sequence / team / territory），cs 全独立表；行数与用例指示编码（P / N-TERM / N-DIS）按规格表逐行执行；crm `lead_seq_progress` 文件名按规格表 `erp_crm_lead_seq_progress.csv`（表名缩写先核对 ORM tableName 为准）
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（ErpCrmStage / ErpCrmLead / ErpCrmForecastPeriod / ErpCsTicket / ErpMdPartner）或〔本批〕新增行；可选 FK 留空或指向已 seed 行；禁止悬空引用；字典码 ∈ ORM dict
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期；列集以 ORM/XMeta 生成的实体列为准（先抽样同域既有 CSV，如 `erp_crm_lead.csv` / `erp_cs_ticket.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c/M1.2b 先例）
      - Skill: `nop-backend-dev`
- [x] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——`ErpCrmReportBizModel` 三数据集读取面复证（含 campaignAttribution → `ErpCrmCampaign` 零漂移前提：既有 lead 行零 campaignId + 本批零新增 lead 行）、`ErpCsReportBizModel` 读取面复证、`ErpCsQualityDashboardBizModel` 空集短路论证复证、6 个域 action/smoke spec + `crm.list-value` + `cs.list-value` 消费表 grep、`ext-domains-list-filter` 结构断言面核实、`app-erp-all/_cases` 表引用面复证，判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`

执行证据（2026-09-02）：

- **45 CSV 落地**：crm 30（`erp_crm_activity` … `erp_crm_territory_assignment_rule`，含 `erp_crm_lead_seq_progress.csv` 缩写文件名按 ORM `tableName="erp_crm_lead_seq_progress"` 核实）+ cs 15（`erp_cs_agent_rate` … `erp_cs_time_entry`）；行数逐文件 2~3，均在规格表建议区间内；用例指示编码以 REMARK 后缀 `（P）/（N-TERM）/（N-DIS）` 落地（沿 mnt/qa 批先例）；N-DIS 行以 `IS_ACTIVE=false` / 状态停用语义词承载，N-TERM 行以 COMPLETED/EXPIRED/CANCELLED/FAILED/STOPPED 终态承载；全部日期列静态（2026-07-01 ~ 2026-10-05），零滚动期间。
- **门禁首跑拦截并修复 3 处列对齐偏差**（Proof 先行价值实证）：`erp_crm_quota.csv` 行 2 / `erp_cs_service_catalog_item.csv` 两行 / `erp_cs_ticket_timer_session.csv` 行 1/3 各多一个空字段（类型转换失败 / H2 `Value too long for column ACTIVE_FLAG` 暴露），修正后门禁 4 测试中 3 项通过、仅常量断言按预期报告 269→314 漂移（属 Phase 2 义务）。
- **FK 闭环证明**：`testNonNullRelationKeysPointToExistingRows` 首跑通过（45 表全部 to-one 非空键零悬空，含 cs entitlement → ErpMdPartner 跨域边、territory 自引用 parentId、bundle/score/sequence 子表 → 本批头表边）；字典码全部取自 `module-crm`/`module-cs` ORM `<dicts>`（activity-type / event-type / event-status / event-priority / scoring-method / scoring-triggered-action / scoring-trigger-event / sequence-progress-status / sequence-template-type / seq-assignment-condition-type / assignment-condition-type / assignment-method / territory-type / quota-period-type / config-rule-type / price-rule-type / bundle-discount-type / forecast 类 + cs service-type / contract-type / billing-cycle / contract-status / fulfillment-action-type / fulfillment-step-status / timer-session-status / time-entry-approve-status / time-entry-source / ticket-priority）。
- **干扰面预分析结论 = 零漂移预期**（2026-09-02 实仓复证）：
  1. `ErpCrmReportBizModel` campaignAttribution（`:292 resolveCampaignNames`，`:293 if (campaignIds.isEmpty()) return emptyMap()` 短路实证）：既有 `erp_crm_lead.csv` 无 CAMPAIGN_ID 列（grep=0）+ 本批零新增 lead 行 → 聚合集空 → 名解析空集短路；lead-conversion-funnel / forecast-accuracy 两数据集读 lead+stage / forecast 族〔已seed〕不受影响。
  2. `ErpCsReportBizModel` ticket-sla-csat 读 Ticket+Survey〔已seed〕不受影响。
  3. `ErpCsQualityDashboardBizModel`（`:271 loadSlaPolicyTeamMap` / `:285 loadTeamNames`，`:272`/`:287` 空集短路实证）：既有 `erp_cs_ticket.csv` 无 SLA_POLICY_ID 列（grep=0）→ 空集短路，本批 `erp_cs_sla_policy`/`erp_cs_team` 种子行不被该面板聚合。
  4. E2E 数值断言面：`crm.list-value`（ErpCrmLead expectedCount=2）+ `cs.list-value`（ErpCsTicket expectedCount=2）——本批零 lead/ticket 行新增，零影响；crm-lead.action / cs-ticket.action / cs-canned-response.action / cs-kb-suggestion.smoke / cross-doc-navigation.action 自建自清 + 存在性断言，种子零破坏；3 个 report value spec（crm-lead-conversion-funnel / crm-forecast-accuracy / cs-ticket-sla-csat）token 断言消费〔已seed〕行，零影响。
  5. `ext-domains-list-filter` visual spec：`/ErpCrmFunnelStageMetrics-main` 落在 `EXT_READONLY_ASSERTIONS`（readonly 无增删改按钮结构断言）+ label 可见性断言，均为结构型，不消费行数据，种子零影响；`ext-domains-child-table` 实测仅覆盖 logistics/b2b/contract/hr/drp，不在本批消费面。
  6. `app-erp-all/_cases` grep：crm/cs 相关集成用例（TestErpC15CrmLeadForecast / TestErpC04SalReturnWithCs）仅经 `@var:` 自建实体断言，无 findPage/totalCount 种子计数断言（grep 实证零命中），纯加性插入不入既有快照（M1.2b 先例）。

Exit Criteria:

- [x] 45 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [x] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内（含 cs entitlement 跨域 ErpMdPartner 边）
- [x] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单）

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/crm/seed-data.md`、`docs/design/customer-service/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「218 + 已落地批次 CSV 数」为准本批 +45，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +45，精确缺 seed −45）+ 新增 M1.4a 批次增量行 + L352 CRM/CS 子集 Deferred 消费注记 + 快照重录义务节资产计数注记（以对账表批次链为准）
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/crm/seed-data.md` + `docs/design/customer-service/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕/〔跨域:md〕标注）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）
      - Skill: `nop-backend-dev`

执行证据（2026-09-02）：

- 常量 269→314（基数 = 269 即「218 + M1.2c 19 + M1.3 32」，本批 +45；常量 javadoc 批次沿革追加 314 行；类 javadoc 引用同步更新；`EXPECTED_PLATFORM_CSV_COUNT`/`EXPECTED_APP_ERP_ENTITY_COUNT`/`EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES` 零触碰）；同执行序 N=1/N=2 已先落地（批内共享触点协议遵守，无覆盖写）。
- seed-data.md 五处同步：头部门禁段推进链 269→314（M1.4a 入链）；对账表「有 seed」269→314 复算行 +45；「精确缺 seed」99→54（crm/cs 节消费落地注记）；新增 M1.4a 批次增量行（干扰面零漂移设计 + L352 消费注记在行内）；快照重录义务节两处资产计数链追加「M1.4a 后 = 318 CSV + 1 SQL」；L352 CRM/CS 配置/执行链 Deferred 子集消费登记（GL 凭证子集维持原 Deferred）。
- 2 个域 owner doc 落地：`docs/design/crm/seed-data.md`（30 CSV 清单 + 6 组主子表 FK 闭环图 + N-TERM/N-DIS 行语义 + 干扰面零漂移节 + 约定对齐节）、`docs/design/customer-service/seed-data.md`（15 CSV 清单 + ticket 全生命周期/catalog 链 FK 闭环图 + UK 单活跃计时器/幂等物化自洽节 + 状态轴自洽与金额对账节 + 干扰面零漂移节）；格式沿 hr/notify 域 seed-data.md owner doc 先例。

Exit Criteria:

- [x] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [x] 2 个域 seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移，按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏 → 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行 crm/cs 相关视觉 spec（`ext-domains-list-filter` / `dashboards.visual` + `dashboards.snapshot` / `reports.visual` + `reports.snapshot`），实仓清单复核后如有额外两域相关 spec 一并纳入；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段（载体按 e2e-runbook §快照重录合规声明协议裁决落位）；禁止单面重录
      - Skill: `nop-testing`
- [x] E2E 数值断言联动评估：`crm.list-value` / `cs.list-value` / `crm-lead.action` / `cs-ticket.action` / `cs-canned-response.action` / `cs-kb-suggestion.smoke` / `cross-doc-navigation.action` + CRM 报表 spec（含 campaignAttribution 数据集消费面）+ cs-ticket-sla-csat value/smoke spec 及 Phase 1 预分析标记的消费面——有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 6 张本批表（crm ≥ 4 含主子表头 + cs ≥ 2）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [x] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [x] 独立结束审计通过后回写 roadmap 工作项 M1.4a `ready` → `done`（含批次证据摘要，格式沿 M1.1/M1.2 批先例）
      - Skill: none

执行证据（2026-09-02）：

1. **门禁**：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` **4/4 全绿**（16.9s）——scope-pinning 363 + `EXPECTED_APP_ERP_CSV_COUNT=314`（= 218 + M1.2c 19 + M1.3 32 + M1.4a 45）+ 零孤儿 CSV + 引用完整性零悬空白名单零增量。
2. **回归扫掠**：`mvn test -pl app-erp-all` **71/0/0/1 = M0.2 基线精确一致**（BUILD SUCCESS，2m31s）——C01-C21 全绿零集成快照漂移，**快照重录未触发**，与 Phase 1 预分析「零漂移预期」对账一致（6 条论证逐条成立：campaignAttribution 空集短路 / quality dashboard 空集短路 / report value token 消费〔已seed〕面 / list-value 零 lead/ticket 新增 / readonly 结构断言不消费行 / `_cases` 纯加性插入）。
3. **视觉快照双面义务核查（未触发重录）**：`dashboards.visual` + `dashboards.snapshot` 10+10 / `reports.visual` + `reports.snapshot` 24+6（含 crm-lead-conversion-funnel、crm-forecast-accuracy、cs-ticket-sla-csat 的 DOM + 像素基线）/ `ext-domains-list-filter`（crm Lead asideFilter + FunnelStageMetrics readonly）全绿零漂移；dashboards 双 spec 经 grep 实证零 crm/cs 消费（CRM/CS 纯报表域无看板），实跑零漂移证成。实仓清单复核发现**额外两域消费 spec `tree-entity-views`**（ErpCsServiceCatalogItem tree/picker）一并纳入——8 失败经**移除本批 45 CSV → 重建 jar → 同 spec 复跑**对照实验逐测试 diff **完全一致（IDENTICAL FAILURE SETS，M1.2a2/M1.3 先例同型）**裁决为预存 flux 迁移期交互面结构断言漂移（失败集含非本批 ErpMdMaterialCategory/ErpMdSubject/ErpHrDepartment 3 实体 6 测试，与 M1.3 批同日登记的 tree-entity-views 8 失败族同集，归 successor Follow-up）；本批 seed 变更零 DOM/像素漂移，无快照重录合规声明段（未触发即合规）。
4. **E2E 数值断言联动（26 spec = 25 绿 + 1 预存失败）**：`crm.list-value` + `cs.list-value` + `crm-lead.action` + `cs-ticket.action` + `cs-canned-response.action` + `cross-doc-navigation.action` + `cs-kb-suggestion.smoke` + crm/cs 报表 value/smoke 7 spec（crm-forecast-accuracy value+smoke / crm-lead-conversion-funnel value / crm-lead-funnel smoke / cs-ticket-sla-csat value+smoke，campaignAttribution 消费面含于 lead-conversion-funnel value）= 25 全绿**零期望值基线调整**；唯一失败 `cs-kb-suggestion.smoke` 经同款对照实验证成预存（无种子态同失败）+ 诊断 spec 实证请求零发出（`DIAG_HITS=[]`，/graphql 与 /r/ 双面均无）——双重根因：(a) commit `751749e17`（08-31）将 `ErpCsTicket.view.xml __kbSuggestion` data-source 取数由 /graphql 改 REST `/r/`（`:137-138` 注释自证「原走 /graphql，违反本仓 REST 约定」），spec 监听器仍按 `/graphql` URL 断言，违 runbook §API 断言「禁止在 flux 页面断言 GraphQL 请求」（spec 落后于页面迁移）；(b) 0811 已登记 add-dialog 未开启预存白名单族（08-11 日志 (a)-deferred 三 spec 白名单，subject fill 落列表过滤输入而非表单 → `sendOn: subject.length>=2` 挂空）。登记 Follow-up 归 successor（spec `/r/` 化 + dialog-open 族跨仓调查），非本批 seed 义务。
5. **运行时装载证明**：`./scripts/start-app.sh restart`（fresh-DB）12~13s ready、装载零冲突零列映射错误（318 CSV + 1 SQL 全量；服务端日志 105 处 ERROR/Exception 命中经核验全为 DDL 列名与列清单文本，零真实异常）；GraphQL `/r/{Entity}__findPage` 抽样 **13/13 表 total==CSV 行数（均为 2）**——crm 8：ErpCrmActivity / ErpCrmCampaign / ErpCrmBundlePricing / ErpCrmLeadScore / ErpCrmSequence / ErpCrmTeam / ErpCrmTerritory（bundle_pricing/lead_score/sequence/team/territory 5 组主子表头全在样）/ ErpCrmFunnelStageMetrics + cs 5：ErpCsSlaPolicy / ErpCsKnowledgeBase / ErpCsEntitlement / ErpCsServiceCatalogItem / ErpCsTimeEntry；对照实验后恢复 45 CSV 重建 jar 二轮 fresh-DB 重启同样 13s ready 稳定（jar 实测打包 53 个 crm/cs CSV）。
6. **compliance checker**：`bash docs/audits/nop-compliance-checker.sh` **exit 0 零新增漂移**——实测 R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42，与 §BASELINE 机器块逐值一致（R2c 1537+5 / R2b 240+2 / R12a 70+1 = 已登记 pre-existing 增量，沿 plan `2026-09-01-0527-1` 口径协议），且与同日 M1.3 批实测逐值一致；纯资源 + 测试常量 + docs 变更符合零漂移预期。
7. **日志**：`docs/logs/2026/09-02.md` 执行条目已追加（含本条全部证据摘要 + 执行期环境事故记录）。
8. **roadmap 回写**：独立结束审计通过后执行（见 Closure 段审计证据）。

执行期环境事故记录（非本批引入，2026-09-02 14:48）：

- **现象**：对照实验重建 jar 后 runner 启动失败 `ClassNotFoundException: io.nop.dao.seq.SnowflakeSequenceGenerator`（`SysSequenceGenerator.afterPropertiesSet` 反射装载点）。
- **根因**：并发 nop-stream-productization mission-driver（另一 AI 会话，`nop-entropy-wt/nop-entropy-nop-stream-productization` 工作树）于 14:48 经共享 `~/.m2` 安装 typo 态 `nop-dao`（提交态类名 `SnowflakeSequenceGeneator`，`git log -S` + 实仓 jar 常量池实证），覆盖了此前 typo 修复态构件；与 0828 旧 `nop-sys-dao`（常量池引用修复拼写）形成 m2 混态。本计划全程未触碰 m2 平台构件（`-pl app-erp-all` 不安装兄弟模块）。
- **处置**：自 nop-entropy-master 工作树 `mvn install -DskipTests -pl nop-sys/nop-sys-dao` 重建（**纯构建动作，零代码变更**）恢复 m2 一致态（typo↔typo）后 runner 恢复 13s ready；对照实验在一致态上完成后恢复 45 CSV 二轮重建验证。
- **残留风险登记**：共享 m2 并发构建可致平台构件混态，已记入 `docs/logs/2026/09-02.md` ⑦，归 M3.1 兜底考量。

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过
- [x] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 fresh session `ses_fa2811809ffewvoFuISceZXPzo`）——0 Blocker + 2 Major + 4 Minor。Major M1：CRM 报表实有第三数据集 campaignAttributionData 读本批 `ErpCrmCampaign`（lead.campaignId 聚合 + 名解析），零漂移结论成立但前提未记录（既有 lead 无 CAMPAIGN_ID 列 + 本批不改既有 CSV/不新增 lead 行），「三张报表已实证零交集」表述已改；Major M2：`ErpCsQualityDashboardBizModel` 实读本批 `ErpCsSlaPolicy`/`ErpCsTeam`（slaPolicyId→teamId 关联），零漂移静态论证（ticket 无 SLA_POLICY_ID 列 → 空集短路）已补入草案期基线。Minor m1：消费面补 `cs.list-value` + `cross-doc-navigation.action.spec`（测试 6/7）；m2：`ext-domains-list-filter` 含 FunnelStageMetrics readonly 结构断言（种子零影响）、`ext-domains-child-table` 实测不含 crm/cs 已移出；m3：cs 3 文件归属改 1045-1（0330-1 贡献 nop_sys_code_rule）；m4：冻结时钟实体清单补 lead_seq_progress / sequence_assignment / forecast_accuracy。全部已并入本稿。审查者实仓核验 15 项（实体清单 diff/常量/主子表组/FK 锚点/报表读取面 grep/quality dashboard helper 短路/spec 存在性/_cases 零命中/owner doc 目录/保护区声称/反松弛用语零命中等）全部记录在案。
- Independent draft review iteration 2: accept（独立子代理 fresh session `ses_fa272c155ffe7ju6I1KfWU8FXf`）——8/8 修订核验项全部通过（campaignAttribution 数据集 + 空集短路实仓逐点吻合 `ErpCrmReportBizModel:255/:292`、quality dashboard helper 短路论证吻合 `:271/:285`、cs.list-value/cross-doc-navigation 三处补齐、child-table 移出与五域覆盖实测一致、cs 归属 git 实证、冻结时钟 8 实体、Review Record 完整、基线锚点未误改 + 反松弛零命中），无新引入缺陷。共识达成，转 `active`。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（45 CSV + 常量 + 对账表 + 2 个 owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表/快照重录注记/L352 消费注记 + 2 个域 seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- **seed-data.md L352「CRM/CS/HR 域配置/执行链 seed」Deferred 之 CRM + CS 子集**：本计划 Phase 1 全量覆盖口径消费（触发条件由 roadmap M1 全量覆盖目标取代，沿 M1.2b 消费 L297 先例）；消费注记由 Phase 2 回写 seed-data.md。GL 凭证/业财一体子集（L351）不在本计划范围，维持 Deferred。
  - Classification: `consumed by this plan`（仅 CRM/CS 配置/执行链子集）
  - Why Not Blocking Closure: 非阻塞项——为消费登记而非遗留债务；L351 子集继续由其自身触发条件管辖。
  - Successor Required: `no`（L351 子集维持原 Deferred，不归本计划）

（其余待执行期裁定：若规格表某行证伪 CSV-only 可满足性，按反松弛规则移入本节分类登记）

## Closure

Status Note: 计划可闭包——45 seed CSV（crm 30 + cs 15，每表 2 行）+ 门禁常量 269→314 + seed-data.md 对账表（有 seed 314 / 精确缺 54 / 318 CSV + 1 SQL）+ `docs/design/crm/seed-data.md` + `docs/design/customer-service/seed-data.md` 2 个 owner doc 落地。验证终态：`TestErpSeedDataIntegrity` 4/4 全绿、`mvn test -pl app-erp-all` 71/0/0/1 = M0.2 基线精确一致（零集成快照漂移，快照重录双面义务未触发）、fresh-DB 装载 13/13 抽样表 findPage==CSV 行数（5 组 crm 主子表头全在样）、视觉双面 dashboards 10+10 / reports 24+6 + ext-domains-list-filter 零漂移、tree-entity-views 8 失败 + cs-kb-suggestion 1 失败经移除种子对照实验（IDENTICAL FAILURE SETS）裁决预存归 successor、E2E 消费面 26 spec 零期望值基线调整、compliance checker exit 0 R2c=1542 零新增漂移。执行期共享 m2 混态环境事故（并发 nop-stream mission-driver 构件覆盖）已记录并经纯构建动作修复（零代码变更）。L352 CRM/CS 配置/执行链 Deferred 子集已按全覆盖口径消费登记，L351 GL 子集维持 Deferred。独立草案审查（2 轮收敛）+ 独立结束审计 APPROVE 三重证据齐备。roadmap M1.4a 已回写 `done`。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 fresh session `ses_f9f0cf079ffecKBvjsS46D0mxP`（2026-09-02）
- Evidence: VERDICT **APPROVE**（0 Blocker / 0 Major / 2 Minor 非阻塞）。A..I 九项全 pass 实仓核验：①计划一致性（3 Phase completed + Exit Criteria 全勾 + Gates 1-6 有据）；②318 CSV 实测 + 45 文件逐一在位（行数 2~3）+ 3 文件全文抽样符合约定（大写列头/零审计列/ID<100000/ISO 日期/小写布尔/N-DIS 承载）；③常量 314（L133）与 javadoc 沿革链 218+19+32+45 算术自洽 + 实仓 erp_* 314 + 4 平台 + 1 SQL + seed-data.md 314/54/M1.4a 批次行/L352 消费注记逐一在位；④验证独立复现（gate 4/0/0/0 13.09s + checker exit 0 R2c=1542/R2b=242/R12a=71 逐位吻合）；⑤2 个 owner doc 在位（crm 31 文件名 = 30 批内 + 1 既有上下文引用 / cs 16 = 15 + 1）+ FK 闭环标注 + 抽查文件名实存；⑥干扰面复证（lead 无 CAMPAIGN_ID / ticket 无 SLA_POLICY_ID 列 + `git diff --stat _init-data/` 零既有 CSV 修改）；⑦Phase 3 证据自洽（日志条目在位 + M1.3 同日登记 tree-entity-views 8 同族先例 + 0811 白名单在案 + `git log -S` 证 751749e17 REST 改写 + view.xml `@query:` REST vs spec `/graphql` 监听双根因自洽 + checker 逐位复现）；⑧roadmap 审计时点仍为 `ready`（回写协议时序正确）；⑨反松弛（变更面 = 4 M + 45 ?? + 2 ?? owner doc，零 ORM/生产 Java/既有 CSV 触碰；L351 维持 Deferred successor=none；Goals 全落地零降级）。Minor m1：crm owner doc 31 文件名含 1 个既有上下文引用非批内文件（未来审计计数口径注记，不修正正文）；m2：Closure 占位段 Gates 7-8/roadmap 回写须审计后立即完成（即本段，已落实）。

Follow-up:

- `cs-kb-suggestion.smoke.spec.ts` 双重预存缺陷修复归 successor：(a) spec 监听器 `/graphql` → REST `/r/` 化对齐 `ErpCsTicket.view.xml __kbSuggestion` data-source（commit `751749e17` 页面迁移遗留，违 runbook §API 断言现行约定）；(b) add-dialog 未开启族（0811 白名单，cross-repo flux 调查 successor 同源）。
- `tree-entity-views` / `status-tag` / `f12-page-structure` / `sensitive-operation-confirmations` / `ext-domains-child-table` 交互面结构断言漂移族（flux 迁移期预存，M1.2a1/M1.3/本批三批同族登记）归既有 successor（roadmap §横切 Follow-up），非本批新增。
- 共享 `~/.m2` 并发构建混态风险（本批执行期实测 1 次：nop-stream mission-driver 构件覆盖致 runner ClassNotFoundException，纯构建修复）归 M3.1 兜底考量。
