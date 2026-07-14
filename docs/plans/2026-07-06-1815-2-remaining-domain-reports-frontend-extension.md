# 2026-07-06-1815-2-remaining-domain-reports-frontend-extension 剩余 7 域业务报表 AMIS 菜单/页面（assets/projects/maintenance/quality/master-data/crm/customer-service）

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: 承接 `docs/plans/2026-07-06-1247-3-domain-reports-frontend.md`「其余域报表前端（资产/项目/维护/质量/主数据/CRM/客服）」（Successor Required: yes，触发条件=对应域报表后端落地时）+ `docs/plans/2026-07-06-1247-1-remaining-domain-reports-backend.md`「inventory/HR 报表 AMIS 菜单/页面接入」同范式 successor；owner doc `docs/architecture/print-template.md`
> Related: `2026-07-06-0504-2-report-rendering-subsystem.md`（finance 报表前端范式，已完成）、`2026-07-06-1247-3-domain-reports-frontend.md`（mfg/inv/hr 报表前端，已完成，本计划镜像其 page.yaml+action-auth 范式）、`2026-07-06-1815-1-remaining-domain-reports-backend-extension.md`（7 域报表后端，本计划前端依赖其落地）
> Audit: required

## Current Baseline

- **报表前端范式已验证**（0504-2 finance + 1247-3 mfg/inv/hr，均经独立审计）：每域 action-auth 报表菜单组（`resourceType="SUBM"` + icon + orderNo + i18n + 各报表子资源，URL 指向 `/erp/<domain>/pages/report/<name>.page.yaml`）+ report page.yaml（`form` 参数表单 + `button` `actionType: ajax` 调 `/api/GenericApi` GraphQL `ErpXxxReport__renderHtml` 注入 `html` 容器 + `button-toolbar` 下载 XLSX/PDF `actionType: download` + `responseType: blob` 调 `ErpXxxReport__download{ fileName }`）。reportName 与各域后端 `.xpt.xml` 模板名逐一核对一致；page 参数严格对齐后端 `buildXxxDataset` 真实签名。
- **剩余 7 域报表前端为零**：实时核实 7 域 `erp-{ast|prj|mnt|qa|md|crm|cs}.action-auth.xml` 均**无** `<domain>-report` 菜单组、**无** report page.yaml（仅 finance/mfg/inv/hr 已接入）。
- **后端契约依赖**：本计划对 7 域后端的依赖是**接口契约**（`renderHtml`/`download` @BizQuery 签名 + reportName 值 + `buildXxxDataset` 参数），由 `2026-07-06-1815-1` 提供。后端 reportName 集合（约 13 张）：
  - ast（2）：`asset-depreciation-detail` / `asset-disposal-detail`
  - prj（2）：`project-cost-summary` / `timesheet-detail`
  - mnt（2）：`maintenance-history` / `downtime-summary`
  - qa（2）：`inspection-summary` / `ncr-capa-summary`
  - md（2）：`material-price-list` / `partner-list`
  - crm（2）：`lead-conversion-funnel` / `forecast-accuracy`
  - cs（1）：`ticket-sla-csat-summary`
- **域 action-auth 已确认存在**：7 域 `erp-{ast|prj|mnt|qa|md|crm|cs}.action-auth.xml` 均存在（CRUD 阶段建立），本计划仅**新增** `<domain>-report` 菜单组 + 子资源，不改动既有菜单项。
- **剩余差距**：7 域各缺报表 action-auth 菜单组 + report page.yaml。

## Goals

- 交付 **7 域业务报表 action-auth 菜单组** + **约 13 个 report page.yaml**，镜像 0504-2/1247-3 page.yaml 范式（参数表单 + 渲染按钮 + 下载 XLSX/PDF toolbar + html 容器），reportName 与 `2026-07-06-1815-1` 各域后端模板名严格一致，page 参数对齐各域后端 `buildXxxDataset` 真实签名。

## Non-Goals

- **finance / manufacturing / inventory / HR 报表前端**（0504-2 / 1247-3 已落地）。
- **新报表后端**——本计划纯前端接入，不改 BizModel/.xpt.xml。如发现某报表后端 reportName/参数缺口，须另起后端计划（后端由 `2026-07-06-1815-1` 提供）。
- **单据打印/套打 / 定时报表批量**（0504-2 Deferred）。
- **报表运行时浏览器视觉回归**——归 Playwright successor（同 `2026-07-06-1247-2`/`1606-2` Deferred）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/print-template.md`（报表模板存储/渲染/域隔离复用约定）；各域业务口径 owner docs 仅用于确认 page 参数对齐口径
- Skill Selection Basis: 任务为 AMIS page.yaml 定制 + action-auth 菜单接入 + GraphQL 消费 `Erp{Ast|Prj|Mnt|Qa|Md|Crm|Cs}Report__renderHtml`/`download`，匹配 `nop-frontend-dev`（page.yaml 定制 / bounded-merge / AMIS form+button+download 组件）。后端 API 由前置计划 `2026-07-06-1815-1` 提供，本计划不改后端。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。报表渲染引擎（`nop-report` + `IReportEngine`）已由 0504-2 接线。**前置依赖**：本计划全部 7 域前端须在 `2026-07-06-1815-1` 后端落地后方可端到端验证（reportName 一致性 + 构建通过可在后端落地前后做静态校验，但运行时渲染验证须后端就绪）。无新外部服务/端口/密钥。

## Execution Plan

### Phase 1 - assets + projects 报表菜单组与页面（establish frontend pattern）

Status: completed
Targets: `module-assets/erp-ast-web/.../auth/erp-ast.action-auth.xml`；`module-projects/erp-prj-web/.../auth/erp-prj.action-auth.xml`；`_vfs/erp/{ast|prj}/pages/report/*.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: `2026-07-06-1815-1` Phase 1 后端 reportName 契约（asset-depreciation-detail/asset-disposal-detail/project-cost-summary/timesheet-detail）

- [x] **Decision: 两域报表菜单组落位**——`erp-ast.action-auth.xml` 新增 `ast-report` 菜单组（displayName「资产报表」/ icon / orderNo / `resourceType="SUBM"`）+ 2 子资源；`erp-prj.action-auth.xml` 新增 `prj-report` 菜单组 + 2 子资源。镜像 finance `fin-report` 范式。
  - 理由：0504-2 finance `fin-report` + 1247-3 mfg/inv/hr 已验证 action-auth 报表菜单组范式；同构接入，reportName 与 1815-1 后端模板名严格一致。
  - 替代方案：复用跨域菜单组挂载（拒绝：域边界错位 + URL 前缀混乱）。
  - Skill: `nop-frontend-dev`
- [x] **Add: 4 个 report page.yaml**——`ast/asset-depreciation-detail.page.yaml`（参数：categoryId/startDate/endDate，对齐后端 `buildAssetDepreciationDetailDataset`）、`ast/asset-disposal-detail.page.yaml`（参数：startDate/endDate，对齐 `buildAssetDisposalDetailDataset`）、`prj/project-cost-summary.page.yaml`（参数：projectId/startDate/endDate，对齐 `buildProjectCostSummaryDataset`）、`prj/timesheet-detail.page.yaml`（参数：projectId/startDate/endDate，对齐 `buildTimesheetDetailDataset`——后端无 employeeId 维度，页面未引入该参数）。各 page.yaml 镜像 finance/mfg report 范式：参数表单 + 渲染按钮（调 `Erp<Ast|Prj>Report__renderHtml(reportName=...)`）+ 下载 XLSX/PDF toolbar + html 容器；action-auth 接入对应子资源。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 两域报表页面静态验证**——page.yaml `yaml.safe_load` 可解析（PASS）；reportName 与 1815-1 后端模板一致（4==4 PASS）；action-auth `xmllint --noout` well-formed（PASS）；参数与后端 `buildXxxDataset` 签名逐一对齐（PASS，无 stray/missing）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] assets + projects 报表菜单组 + 4 页面落地；reportName 与后端一致；action-auth well-formed；page.yaml 可解析

### Phase 2 - maintenance + quality 报表菜单组与页面

Status: completed
Targets: `erp-mnt.action-auth.xml`；`erp-qa.action-auth.xml`；`_vfs/erp/{mnt|qa}/pages/report/*.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + `2026-07-06-1815-1` Phase 2 后端 reportName 契约

- [x] **Add: `mnt-report` + `qa-report` 菜单组（各 2 子资源）+ 4 个 report page.yaml**——`mnt/maintenance-history.page.yaml`（参数：equipmentId/startDate/endDate，对齐 `buildMaintenanceHistoryDataset`）、`mnt/downtime-summary.page.yaml`（参数：equipmentId/startDate/endDate，对齐 `buildDowntimeSummaryDataset`）、`qa/inspection-summary.page.yaml`（参数：materialId/startDate/endDate，对齐 `buildInspectionSummaryDataset`——后端无质检模板维度，页面未引入该参数）、`qa/ncr-capa-summary.page.yaml`（参数：startDate/endDate，对齐 `buildNcrCapaSummaryDataset`——后端无状态过滤维度，页面未引入该参数）。镜像 Phase 1 page.yaml 范式。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 两域报表页面静态验证**（镜像 Phase 1 Proof 口径；reportName 4==4 PASS；page.yaml `yaml.safe_load` PASS；action-auth `xmllint --noout` PASS；参数与后端签名逐一核验 PASS）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] maintenance + quality 报表菜单组 + 4 页面落地；reportName 与后端一致；action-auth well-formed；page.yaml 可解析

### Phase 3 - master-data + crm + customer-service 报表菜单组与页面

Status: completed
Targets: `erp-md.action-auth.xml`；`erp-crm.action-auth.xml`；`erp-cs.action-auth.xml`；`_vfs/erp/{md|crm|cs}/pages/report/*.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 + `2026-07-06-1815-1` Phase 3 后端 reportName 契约

- [x] **Add: `md-report` + `crm-report` + `cs-report` 菜单组 + 5 个 report page.yaml**——`md/material-price-list.page.yaml`（参数：materialCode[String]，对齐 `buildMaterialPriceListDataset`——后端无独立价档维度，价格在 SKU 四档字段）、`md/partner-list.page.yaml`（参数：partnerType[String]，对齐 `buildPartnerListDataset`——后端按客户/供应商分类，非区间/团队）、`crm/lead-conversion-funnel.page.yaml`（**零参数**，对齐 `buildLeadConversionFunnelDataset()` 无参签名，镜像 hr `employee-net-balance.page.yaml` 范式）、`crm/forecast-accuracy.page.yaml`（参数：forecastId[BigDecimal]，对齐 `buildForecastAccuracyDataset`——后端按 forecastId 而非预测期间）、`cs/ticket-sla-csat-summary.page.yaml`（参数：ticketType[String]，对齐 `buildTicketSlaCsatSummaryDataset`——后端无区间维度，页面未引入该参数）。镜像 Phase 1 page.yaml 范式。CS 域物理目录 `module-cs/erp-cs-web`（工程名 `app-erp-cs`，URL 前缀 `/erp/cs/`）。
  - Skill: `nop-frontend-dev`
- [x] **Proof: 三域报表页面静态验证**（镜像 Phase 1 Proof 口径；reportName 5==5 PASS；page.yaml `yaml.safe_load` PASS；action-auth `xmllint --noout` PASS；参数与后端签名逐一核验 PASS，含 crm lead-conversion-funnel 零参确认）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] master-data + crm + customer-service 报表菜单组 + 5 页面落地；reportName 与后端一致；action-auth well-formed；page.yaml 可解析

## Draft Review Record

- Independent draft review iteration 1: accept (plan-review 2026-07-06) because 格式合规（全部必需段落+头部字段+Phase 结构+Item Types `Decision|Add|Proof` 均符合指南）；退出标准清晰可测（各 Phase 页面数+reportName 一致+action-auth well-formed+page.yaml 可解析）；范围边界清晰——单一结果面（7 域报表前端），遵循规则 14 将同范式多域合并为阶段，无范围蔓延，Non-Goals 与 Deferred 项均显式命名 successor 触发条件；Closure Gates 定义明确验证命令（mvn install/test+xmllint+yaml 解析+reportName ⊆ 后端模板集）+独立结束审计门控。reportName 契约已交叉核对后端 `2026-07-06-1815-1` 全 13 张逐一致（ast/prj/mnt/qa 各 2 + md/crm 各 2 + cs 1），各 page 参数对齐后端 `buildXxxDataset` 聚合维度。无 Blocker/Major 残留。

## Closure Gates

> 完整仓库验证在此处运行一次：`mvn clean install -DskipTests` + 全 workspace `mvn test`（0 failures/0 errors）+ 7 域 action-auth well-formed + 13 page.yaml 可解析 + reportName 集合 ⊆ 后端模板名集合。

- [x] 范围内行为完成（7 域菜单组 + 约 13 page.yaml）
- [x] 相关文档对齐（`print-template.md` §各域前端接入补 7 域条目；roadmap done 条目；当日日志）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 + 全 workspace `mvn test` 0 failures/0 errors（1962 tests）+ 7 action-auth `xmllint --noout` well-formed + 13 page.yaml YAML 可解析 + reportName 集合 ⊆ 后端模板名集合（13==13 精确匹配））
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 7 域报表运行时浏览器视觉回归（Playwright）

- Classification: `watch-only residual`
- Why Not Blocking Closure: page.yaml+action-auth 范式经 0504-2/1247-3 已验证可构建可解析；本计划静态验证（构建 + well-formed + reportName 一致 + GraphQL 签名一致）对齐既有前端结果面门控。
- Successor Required: `yes`（触发条件：Playwright 报表 e2e 套件建立时——同 1247-2/1247-3/1606-2 Deferred）

### 单据打印/套打 / 定时报表批量

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0504-2 已裁定为独立能力面；本计划仅交付各域报表列表/渲染/下载页面。
- Successor Required: `yes`（触发条件：单据打印 / 定时批量需求落地时）

### 每域第 3 张及以后报表前端

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 对应域报表后端 successor（1815-1 Deferred）落地后再做前端；本计划覆盖后端已就绪/即将就绪的 13 张。
- Successor Required: `yes`（触发条件：对应域报表后端落地时）

## Closure

Status Note: 已完成（2026-07-06）。7 域业务报表前端 action-auth 菜单组 + 13 个 report page.yaml 全部落地，镜像 0504-2 finance / 1247-3 mfg-inv-hr page.yaml 范式（参数 `form` + 渲染 `button`（`actionType: ajax` 调 `/api/GenericApi` GraphQL `Erp{Ast|Prj|Mnt|Qa|Md|Crm|Cs}Report__renderHtml`）+ `button-toolbar` 下载 XLSX/PDF（`actionType: download` + `responseType: blob` 调 `__download`）+ `html` 容器）。reportName 与各域后端 `.xpt.xml` 模板名逐一核对一致（13==13 精确匹配），page 参数严格对齐 1815-1 后端 `buildXxxDataset` 真实签名（ast categoryId + 日期 / prj projectId + 日期 / mnt equipmentId + 日期 / qa inspection materialId + 日期、ncr-capa 仅日期 / md materialCode、partnerType 字符串 / crm lead-conversion-funnel **零参**、forecast-accuracy forecastId / cs ticketType 字符串——多处以"页面未引入后端不存在的维度"为准，对齐 1247-3 Closure「inv batchNo 非 batchId / hr employee-net-balance 零参」同口径）。验证全绿（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 全 workspace `mvn test` BUILD SUCCESS 1962 tests 0 failures/0 errors + 7 action-auth `xmllint --noout` well-formed + 13 page.yaml `yaml.safe_load` 可解析 + reportName 集合 ⊆ 后端模板名集合）。解除 1815-1 Non-Goal「7 域报表 AMIS 菜单/页面（前端 successor 1815-2）」+ 1247-3 successor「其余域报表前端（资产/项目/维护/质量/主数据/CRM/客服）」+ 0504-2 Deferred「各域业务报表前端面」剩余域面。

Closure Audit Evidence:

- Auditor / Agent: 执行者自验证（MISSION_DRIVER 执行闭环）；独立结束审计由后续独立子代理（新会话）按 OPEN_AUDIT 流程复核（若发现缺陷将作为新 `open` finding 重开，非阻塞当前关闭）。
- Evidence: 执行者实时仓库核验全 PASS——(1) 13 page.yaml + 7 菜单组（ast-report 2 子 / prj-report 2 子 / mnt-report 2 子 / qa-report 2 子 / md-report 2 子 / crm-report 2 子 / cs-report 1 子）存在且 url 正确（`/erp/<domain>/pages/report/<name>.page.yaml`）；(2) 7 action-auth `xmllint --noout` well-formed（ast/prj/mnt/qa/md/crm/cs，src 路径）；(3) 13 page.yaml `yaml.safe_load` ALL YAML OK；(4) reportName 集合 == 后端模板名集合（13==13 精确匹配，`diff` 空输出确认）；(5) page 参数逐一核验后端 `prepareDataset` switch（ast:167/171、prj:165/169、mnt:159/164、qa:160/165、md:154/157、crm:155/158、cs:154），无 stray/missing 参数；(6) `mvn clean install -DskipTests` BUILD SUCCESS 全 reactor（154 模块）；(7) 全 workspace `mvn test` BUILD SUCCESS 1962 tests 0 failures/0 errors；(8) GraphQL 目标 `Erp{Ast|Prj|Mnt|Qa|Md|Crm|Cs}Report__renderHtml`/`__download` 与各域 `@BizModel("ErpXxxReport")` 注解一致；(9) 计划文本一致（3 Phase Status:completed + Phase 体全 `[x]` + Plan Status:completed + 8 Closure Gates 全 `[x]`）；(10) `print-template.md` line 62 successor 标注更新为已接入 + roadmap `core-business-roadmap.md` done 条目。反模式自检 PASS（13 page.yaml 均在保留层 `pages/report/` 非 `_gen/`；ast/prj/mnt/qa/md 用 `x:extends` 继承既有 `_erp-<domain>.action-auth.xml` 新增菜单组；crm/cs 既有结构直接定义 `<auth>` 内追加菜单组，与既有结构一致）。


- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS_WITH_NOTES**. All Phase exit criteria verified: 7 menu groups, 13 page.yaml, 13 backend templates, well-formed XML, valid YAML, reportName 13==13 exact match. NOTE: minor pre-existing prose inaccuracy in AMIS render-mechanism description (propagated from 1247-3) — does not affect deliverable correctness. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2; this evidence block appended by Phase 3 backfill.)
Follow-up:

- 报表运行时浏览器视觉回归（Playwright successor，触发条件=报表 e2e 套件建立时）——非阻塞，见「Deferred But Adjudicated」。
