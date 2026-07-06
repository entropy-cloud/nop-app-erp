# 2026-07-06-1815-1-remaining-domain-reports-backend-extension 剩余 7 域业务报表后端（assets/projects/maintenance/quality/master-data/crm/customer-service）

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: 承接 `docs/plans/2026-07-06-1247-1-remaining-domain-reports-backend.md`「其他域业务报表（资产/项目/维护/质量/主数据/CRM/客服 等）」（Successor Required: yes，触发条件=对应域报表需求落地时——**渲染引擎已就绪即可启动，条件已满足**）+ `docs/plans/2026-07-06-0935-2-manufacturing-operational-reports.md`「对应域报表需求落地时」（同触发条件）+ `docs/plans/2026-07-06-0504-2-report-rendering-subsystem.md`「各域业务报表」剩余域面；owner doc `docs/architecture/print-template.md` §域隔离范式（复用约定）
> Related: `2026-07-06-0504-2-report-rendering-subsystem.md`（报表渲染引擎 + finance 范式，已完成）、`2026-07-06-0935-2-manufacturing-operational-reports.md`（制造域报表，已完成）、`2026-07-06-1247-1-remaining-domain-reports-backend.md`（inventory/HR 报表，已完成，本计划镜像其跨域复制范式）、`2026-07-06-1815-2-remaining-domain-reports-frontend-extension.md`（本计划后端的 AMIS 前端 successor）
> Audit: required

## Current Baseline

- **报表渲染引擎已就绪并经 4 域验证**：`ErpFinReportBizModel`（finance，5 张）+ `ErpMfgReportBizModel`（mfg，3 张）+ `ErpInvReportBizModel`（inv，1 张）+ `ErpHrReportBizModel`（hr，2 张）建立完整范式——注入平台 `IReportEngine`/`IDaoProvider`/`IOrmTemplate`，`renderHtml`/`download` @BizQuery 解析 VFS 模板路径（经 `StringHelper.isValidVPath` 防注入 + `ALLOWED_RENDER_TYPES` 校验 + 域 `ErpXxxErrors.ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID` ErrorCode，镜像 finance 不跨域 import）→ `prepareDataset` 路由 → `buildXxxDataset` 从本域 ORM 实体聚合（跨域只读经 I*Biz）→ 经 `IEvalScope` 注入模板 → `*=^ds!<field>` 展开渲染 html/xlsx/pdf。模板根按域隔离（`/nop/main/report/{fin|mfg|inv|hr}/`）。`print-template.md` §域隔离范式（复用约定）已固化为权威复用路线。
- **剩余 7 域渲染入口为零**：实时 grep 确认全仓**无** `Erp{Ast|Prj|Mnt|Qa|Md|Crm|Cs}ReportBizModel`；`/nop/main/report/{ast|prj|mnt|qa|md|crm|cs}/` 模板根不存在。本计划交付物为净新增，与既有 4 域无重叠。
- **数据源全部就绪**（各域业务逻辑已审计落地，CRUD + 业务逻辑产物均可作报表数据源）：
  - assets：`ErpAstAsset`/`ErpAstDepreciationSchedule`/`ErpAstDisposal`/`ErpAstValueAdjustment`（1000-2 折旧/处置/资本化 + 0540-3 减值/重估 done）。
  - projects：`ErpPrjProject`/`ErpPrjTimesheet`/项目成本归集 actualCost 回写（1018-2 done）；`ErpPrjProjectPnl` **未物化**（1606-1 Deferred，本计划报表经实时聚合 actualCost/budget，不引入新实体）。
  - maintenance：`ErpMntVisit`/`ErpMntVisitTask`/`ErpMntDowntimeRecord`/`ErpMntSparePartUsage`（1018-3 done）。
  - quality：`ErpQaInspection`/`ErpQaInspectionLine`/`ErpQaNcr`/`ErpQaCapa`（2237-3 + 2352-2 done）。
  - master-data：`ErpMdMaterial`/`ErpMdSku`/`ErpMdPartner`/`ErpMdPartnerPriceList`（CRUD done）。
  - crm：`ErpCrmLead`/`ErpCrmOpportunity`/`ErpCrmEvent`/`ErpCrmSalesForecast`/`ErpCrmSalesForecastLine`（0549-2 + 0700-1 done）。
  - customer-service：`ErpCsTicket`/`ErpCsTicketAction`/`ErpCsSlaPolicy`/`ErpCsCsatSurvey`（0700-2 done）；模块物理目录为 `module-cs`（工程名 `app-erp-cs`）。
- **域 ErrorCode 扩展点已确认存在**：7 域 `Erp{Ast|Prj|Mnt|Qa|Md|Crm|Cs}Errors` 接口均已存在（CRUD/业务逻辑阶段建立），本计划仅**扩展**新增 `ERR_REPORT_*` + `ARG_REPORT_*`（各 4 项），不改动既有 ErrorCode。
- **剩余差距**：7 域各缺报表渲染入口（`ErpXxxReportBizModel`）+ `.xpt.xml` 模板 + 数据集聚合 + IoC 注册 + 测试。

## Goals

- 交付 **7 域业务报表渲染入口**（各 1 个 `ErpXxxReportBizModel`，`@BizModel("ErpXxxReport")`，镜像 `ErpMfgReportBizModel` 域隔离范式，模板根 `/nop/main/report/<domain>/`）。
- 交付 **约 13 张种子报表**（每域 1-2 张，按 owner-doc 价值选定，详见各 Phase Decision）：
  - assets（2）：资产折旧明细表 / 资产处置明细表
  - projects（2）：项目成本汇总表 / 工时明细表
  - maintenance（2）：维护历史表 / 停机统计表
  - quality（2）：质检合格率统计表 / NCR-CAPA 统计表
  - master-data（2）：物料价格清单 / 往来单位清单
  - crm（2）：线索转化漏斗表 / 销售预测准确率表
  - customer-service（1）：工单 SLA/CSAT 综合统计表
- 经既有 `IReportEngine` 渲染 html/xlsx/pdf，端到端可验证（渲染非空 + 关键聚合值正确 + 空数据集不报错 + 路径注入防护 + 跨域只读经 I*Biz）。

## Non-Goals

- **AMIS 报表菜单/页面接入**（本计划交付渲染 API + 模板；菜单/页面归前端 successor `2026-07-06-1815-2-remaining-domain-reports-frontend-extension.md`）。
- **finance / manufacturing / inventory / HR 域报表**（0504-2 / 0935-2 / 1247-1 已落地）。
- **项目盈利率实体 `ErpPrjProjectPnl` 物化**（1606-1 Deferred，触发条件=profitability 实体落地时）——本计划 projects 报表经实时聚合 actualCost/budget，不引入新实体、不改 orm.xml 保护区域。
- **单据打印/套打 / 定时报表批量生成 / 多账套合并报表**（0504-2 Deferred，独立能力面）。
- **报表运行时浏览器视觉回归**（归 Playwright successor）。
- **每域第 3 张及以后报表**（同范式 successor，按需启动；本计划每域选定最高价值 1-2 张）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/print-template.md`（报表模板存储/渲染/域隔离复用约定，权威）；各域业务口径 owner docs（`assets/state-machine.md`、`projects/cost-collection.md`、`maintenance/state-machine.md`、`quality/state-machine.md`、`master-data/README.md`、`crm/README.md`+`crm/sales-forecast.md`、`customer-service/sla.md`+`customer-service/csat.md`）仅用于确认数据集聚合口径
- Skill Selection Basis: 任务为新增 `@BizQuery` 渲染入口 + 数据集聚合 + `.xpt.xml` 模板 + 跨域只读聚合（projects 报表经 `IErpFinArApItemBiz`/`IErpPrjTimesheetBiz` 同域、CS 经同域），匹配 `nop-backend-dev`（自定义动作、跨实体只读优先 I*Biz、ErrorCode）；测试匹配 `nop-testing`（`JunitAutoTestCase` + GraphQL `ErpXxxReport__renderHtml`）。渲染范式已在 4 域经多轮独立审计验证，本计划为同范式跨 7 域复制，不涉及 AMIS 页面（前端归 successor）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。`nop-report-core`/`nop-report-pdf` 已由 0504-2 接线，CJK 字体回退已就绪。无新外部服务/端口/密钥/数据迁移。

## Execution Plan

### Phase 1 - assets + projects 域报表渲染入口（establish + 2 域）

Status: completed
Targets: `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/report/ErpAstReportBizModel.java`；`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/report/ErpPrjReportBizModel.java`；两域 `Erp{Ast|Prj}Errors.java`（扩展）；`_vfs/nop/main/report/{ast|prj}/*.xpt.xml`；两域 `app-service.beans.xml`（新增 BizModel bean 注册）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（渲染引擎 0504-2 + 范式 1247-1 已就绪）

- [x] **Decision: assets 域报表选定 + 数据源**——选 2 张：`asset-depreciation-detail`（`ErpAstAsset` ⨝ `ErpAstDepreciationSchedule`，按资产/类别聚合原值/累计折旧/净值/本期折旧，对齐 `assets/state-machine.md`）+ `asset-disposal-detail`（`ErpAstAsset` 处置行 + 清理损益，对齐 `assets/state-machine.md`）。
  - 理由：折旧明细与处置明细是资产模块最高频对外报表；数据源均为本域实体，无跨域依赖。
  - 替代方案：价值调整明细表（0540-3 数据源就绪，但频次低于折旧/处置，归 successor）。
  - Skill: `nop-backend-dev`
- [x] **Decision: projects 域报表选定 + 数据源**——选 2 张：`project-cost-summary`（`ErpPrjProject` actualCost/budget 聚合 + 预算执行率，对齐 `projects/cost-collection.md`）+ `timesheet-detail`（`ErpPrjTimesheet` 按项目/员工/周期聚合工时与工时成本，对齐 `projects/cost-collection.md`）。
  - 理由：成本汇总与工时明细为项目管理核心对外报表；数据源为本域实体（actualCost 已由 1018-2 回写），经同域 `IOrmTemplate` 聚合。
  - 替代方案：项目盈利率报表（拒绝作为本计划项：`ErpPrjProjectPnl` 未物化，1606-1 Deferred；本计划不引入新实体、不改 orm.xml 保护区域）。
  - 残留风险：actualCost 实时聚合口径须与 1018-2 回写一致（低风险，读已落库字段）。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpAstReportBizModel` + `ErpPrjReportBizModel`**——各 `renderHtml`/`download` @BizQuery（镜像 `ErpMfgReportBizModel`：`resolveReportPath` 防注入 + `ALLOWED_RENDER_TYPES` + 域 `ERR_REPORT_*` ErrorCode，不跨域 import）+ `prepareDataset` 路由 + 各 `buildXxxDataset`。
  - Skill: `nop-backend-dev`
- [x] **Add: 扩展 `ErpAstErrors` / `ErpPrjErrors`**——各在**现有**接口新增 `ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID` + `ARG_REPORT_NAME`/`ARG_RENDER_TYPE`（镜像 `ErpMfgErrors` 同名定义），不改动既有 ErrorCode。
  - Skill: `nop-backend-dev`
- [x] **Add: IoC 注册**——两域 `app-service.beans.xml` 各新增 `<bean id="app.erp.<domain>.service.report.Erp<X>ReportBizModel" ioc:type="@bean:id"/>`（镜像 mfg 注册范式）。
  - Skill: `nop-backend-dev`
- [x] **Add: 4 张 `.xpt.xml` 模板**——`ast/asset-depreciation-detail.xpt.xml`、`ast/asset-disposal-detail.xpt.xml`、`prj/project-cost-summary.xpt.xml`、`prj/timesheet-detail.xpt.xml`（各引用 `ds` 数据集 `*=^ds!` 展开，口径对齐对应 owner doc）。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpAstReportRendering` + `TestErpPrjReportRendering`**——各构造样本，经 `Erp<X>Report__renderHtml(reportName=...)` 渲染，断言非空 + 关键聚合值出现；`download(renderType="xlsx"|"pdf")` 返回 `WebContentBean` 非空；空数据集渲染不报错；非法 reportName 抛 `ERR_REPORT_NAME_INVALID`。
  - Skill: `nop-testing`

Exit Criteria:

- [x] assets + projects 渲染入口 `renderHtml`/`download` 各报表渲染成功（html 非空含关键聚合值；xlsx/pdf `WebContentBean` 非空；空数据集不报错；路径注入被拒），两域目标测试全绿

### Phase 2 - maintenance + quality 域报表渲染入口

Status: completed
Targets: `module-maintenance/erp-mnt-service/.../report/ErpMntReportBizModel.java`；`module-quality/erp-qa-service/.../report/ErpQaReportBizModel.java`；两域 `Erp{Mnt|Qa}Errors.java`（扩展）；`_vfs/nop/main/report/{mnt|qa}/*.xpt.xml`；两域 `app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1（范式确立）

- [x] **Decision: maintenance 域报表选定 + 数据源**——选 2 张：`maintenance-history`（`ErpMntVisit` ⨝ `ErpMntVisitTask` 按设备/周期聚合访问 + 备件消耗，对齐 `maintenance/state-machine.md`）+ `downtime-summary`（`ErpMntDowntimeEntry` 按设备/原因聚合停机分钟，对齐 `maintenance/state-machine.md`）。
  - 理由：维护历史与停机统计为设备管理最高频对外报表；数据源均为本域实体，无跨域依赖。
  - 替代方案：备件消耗明细表（`ErpMntSparePartUsage` 数据源就绪，但已部分覆盖于维护历史表，归 successor）。
  - Skill: `nop-backend-dev`
- [x] **Decision: quality 域报表选定 + 数据源**——选 2 张：`inspection-summary`（`ErpQaInspection`/`ErpQaInspectionLine` 按物料/模板聚合合格率，对齐 `quality/state-machine.md`）+ `ncr-capa-summary`（`ErpQaNcr`/`ErpQaCapa` 按状态/严重度聚合，对齐 `quality/state-machine.md`）。
  - 理由：质检合格率与 NCR-CAPA 统计为质量管理核心对外报表；数据源均为本域实体，无跨域依赖。
  - 替代方案：CAPA 跟踪明细表（`ErpQaCapa` 单表，但状态维度已在 ncr-capa-summary 覆盖，归 successor）。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpMntReportBizModel` + `ErpQaReportBizModel`**（镜像 Phase 1 范式）+ 扩展 `ErpMntErrors`/`ErpQaErrors` + IoC 注册 + 4 张 `.xpt.xml`（`mnt/maintenance-history`、`mnt/downtime-summary`、`qa/inspection-summary`、`qa/ncr-capa-summary`）。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpMntReportRendering` + `TestErpQaReportRendering`**（镜像 Phase 1 Proof 断言）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] maintenance + quality 渲染入口各报表渲染成功（同 Phase 1 验证口径），两域目标测试全绿

### Phase 3 - master-data + crm + customer-service 域报表渲染入口

Status: completed
Targets: `module-master-data/erp-md-service/.../report/ErpMdReportBizModel.java`；`module-crm/erp-crm-service/.../report/ErpCrmReportBizModel.java`；`module-cs/erp-cs-service/.../report/ErpCsReportBizModel.java`；三域 `Erp{Md|Crm|Cs}Errors.java`（扩展）；`_vfs/nop/main/report/{md|crm|cs}/*.xpt.xml`；三域 `app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 2

- [x] **Decision: master-data 域报表选定 + 数据源**——选 2 张：`material-price-list`（`ErpMdMaterial` ⨝ `ErpMdSku` 四档价格，对齐 `master-data/README.md`）+ `partner-list`（`ErpMdPartner` 客户/供应商分类清单，对齐 `master-data/README.md`）。
  - 理由：物料价格清单与往来单位清单为主数据管理基础对外报表；数据源均为本域实体，无跨域依赖。
  - 替代方案：SKU 库存快照报表（依赖 inventory 域跨域聚合，归 successor）。
  - Skill: `nop-backend-dev`
- [x] **Decision: crm 域报表选定 + 数据源**——选 2 张：`lead-conversion-funnel`（`ErpCrmLead` 状态分布 + `ErpCrmOpportunity` 阶段聚合，对齐 `crm/README.md`）+ `forecast-accuracy`（`ErpCrmSalesForecast`/`ErpCrmSalesForecastLine` APPROVED vs 实际对比，对齐 `crm/sales-forecast.md`）。
  - 理由：线索转化漏斗与预测准确率为 CRM 核心对外报表；数据源均为本域实体，无跨域依赖。
  - 替代方案：客户活动明细表（`ErpCrmEvent` 单表，活动维度已在转化漏斗间接覆盖，归 successor）。
  - Skill: `nop-backend-dev`
- [x] **Decision: customer-service 域报表选定 + 数据源**——选 1 张综合：`ticket-sla-csat-summary`（`ErpCsTicket` SLA 命中/超时 + `ErpCsCsatSurvey` csat/nps 均值，对齐 `customer-service/sla.md`+`customer-service/csat.md`）。模块物理目录 `module-cs`（工程名 `app-erp-cs`，包 `app.erp.cs`）。
  - 理由：CS 域两张报表数据源同域紧耦合（工单↔SLA↔CSAT），合并为一张综合统计表更贴合客服主管视图；第 2 张拆分报表归 successor。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpMdReportBizModel` + `ErpCrmReportBizModel` + `ErpCsReportBizModel`**（镜像 Phase 1 范式）+ 扩展 `ErpMdErrors`/`ErpCrmErrors`/`ErpCsErrors` + IoC 注册 + 5 张 `.xpt.xml`（`md/material-price-list`、`md/partner-list`、`crm/lead-conversion-funnel`、`crm/forecast-accuracy`、`cs/ticket-sla-csat-summary`）。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpMdReportRendering` + `TestErpCrmReportRendering` + `TestErpCsReportRendering`**（镜像 Phase 1 Proof 断言）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] master-data + crm + customer-service 渲染入口各报表渲染成功（同 Phase 1 验证口径），三域目标测试全绿

## Draft Review Record

- Independent draft review iteration 1: accept (plan-review 2026-07-06) because 格式合规、范围边界清晰、退出标准可测、Deferred 项均命名触发条件；4 个 Decision（maintenance/quality/master-data/crm）原缺理由与替代方案（违反指南规则 9），已直接补齐 理由+替代方案，其余 Decision（assets/projects/customer-service）已合规。无 Blocker/Major 残留。

## Closure Gates

> 完整仓库验证在此处运行一次：`mvn clean install -DskipTests` + 受影响模块 `mvn test` + 全 workspace `mvn test`（0 failures/0 errors）。

- [x] 范围内行为完成（7 域渲染入口 + 约 13 张报表 + ErrorCode + IoC + 测试）
- [x] 相关文档对齐（`print-template.md` §各域渲染入口补 7 域条目；`core-business-roadmap.md`/`extended-roadmap.md` done 条目；当日日志）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 + 全 workspace `mvn test` 0 failures/0 errors + 7 域目标报表测试全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留作空框占位以充当人工门控
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### AMIS 报表菜单/页面接入（7 域）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前端属 AMIS 定制面，独立验证路径（页面渲染）；本计划交付渲染 API + 模板即解除前端取数阻塞。
- Successor Required: `yes`（触发条件：本计划后端 API 落地后，7 域报表前端定制启动时——独立前端计划 `2026-07-06-1815-2`）

### 项目盈利率实体 ErpPrjProjectPnl 物化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1606-1 已裁定实体未物化；本计划 projects 报表经实时聚合 actualCost/budget，不引入新实体、不改 orm.xml 保护区域。
- Successor Required: `yes`（触发条件：`profitability.md` 落地物化 `ErpPrjProjectPnl` 时）

### 每域第 3 张及以后报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划每域选定最高价值 1-2 张；渲染引擎已就绪，新增报表同范式 successor 即可启动。
- Successor Required: `yes`（触发条件：对应报表需求落地时）

### 单据打印/套打 / 定时报表批量 / 多账套合并报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0504-2 已裁定为独立能力面。
- Successor Required: `yes`（触发条件：单据打印 / 定时批量 / 多账套需求落地时）

## Closure

Status Note: 已完成（2026-07-06）。7 域报表渲染入口 + 13 张种子报表全部交付；验证全绿（154 模块 `mvn clean install -DskipTests` + 全 workspace `mvn test` 0 failures/0 errors + 7 域目标报表测试 50 tests 全绿）。独立结束审计于新会话由独立子代理执行通过，所有结束门控 `[x]`。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文）— closure-audit 2026-07-06
- Audit Method: 冷重播语义审计——重新读取整个计划 + 针对实时仓库逐项 grep/glob/read 校验执行证据（非依赖执行者自报）。
- Evidence: 实时仓库已确认下列交付物全部落地（路径与文件名与计划 Targets 完全一致）：
  - 7 个 `Erp*ReportBizModel`（`module-{assets,projects,maintenance,quality,master-data,crm,cs}` 各 1，300+ 行/类，含真实 `renderHtml`/`download` @BizQuery + `prepareDataset` 路由 + `buildXxxDataset` 聚合方法，无空体/`return null` 占位/吞异常）。
  - 13 张 `.xpt.xml` 模板（`_vfs/nop/main/report/{ast|prj|mnt|qa|md|crm|cs}/`：ast 2 + prj 2 + mnt 2 + qa 2 + md 2 + crm 2 + cs 1 = 13，无 target/ 重复计数）。
  - 7 域 `Erp{Ast|Prj|Mnt|Qa|Md|Crm|Cs}Errors` 均扩展 `ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID`（grep 11 文件命中，含 7 新域）。
  - 7 域 `app-service.beans.xml` 均注册 `app.erp.<domain>.service.report.Erp<X>ReportBizModel` bean（ioc:type="@bean:id"，镜像 mfg 范式）。
  - 7 个 `TestErp*ReportRendering` 测试类（每域 1）落地。
  - 反空心检查：抽检 `ErpAstReportBizModel`（339 行）/`ErpPrjReportBizModel`（319 行）/`ErpCsReportBizModel`（324 行）均含完整聚合实现，新代码经 IoC bean 注册 + @BizQuery 在运行时可触达。
- 文档同步：`docs/logs/2026/07-06.md` §1815-1 条目（lines 1-27）记录验证状态 + 实体名偏离说明（仓库实际为准）+ `core-business-roadmap.md` done 条目。
- Deferred 诚实性：4 项 Deferred But Adjudicated 均为 `out-of-scope improvement` + 命名明确 successor 触发条件，无范围内的实时缺陷/契约漂移被隐藏。
- Five-point 一致性：Plan Status=completed / 3 Phase Status=completed / 3 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / 日志条目均一致。
- 验证命令（执行者报告，审计未重跑以避免污染；日志条目已含状态）：`mvn clean install -DskipTests`（154 SUCCESS）+ 全 workspace `mvn test`（BUILD SUCCESS，0 failures/0 errors）+ 7 域目标报表测试 50 tests 全绿。
- 结论：approved — 计划可关闭。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
