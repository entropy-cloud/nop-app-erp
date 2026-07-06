# 打印模板

## 目的

定义 nop-app-erp 的单据打印与报表模板机制。**统一复用平台 `nop-report`**（`NopReportDefinition` 实体 + `IReportEngine` 渲染入口），不自建平行打印/报表框架。

## 平台默认路线（权威）

- **不自建** `ErpSysDocumentTemplate` 这类应用层打印模板实体——它与平台 `NopReportDefinition` 语义重叠，违反"应用项目不应自建平行报表/导出框架"的默认路线（见 `../nop-entropy/docs-for-ai/02-core-guides/reporting-and-notification-integration.md`）。
- 报表/打印模板的**渲染入口**是 `IReportEngine.getRenderer(path, renderType)` / `getHtmlRenderer(path)`，应用 BizModel 注入并调用（参考 `ReportDemoBizModel`、本仓 `ErpFinReportBizModel`）。
- 模板是 Excel 工作簿（`.xpt.xlsx` 推荐，Excel 设计；或 `.xpt.xml` XML 序列化，便于随仓版本化与审计），按单据类型 + 模板类型维度组织，存放于各域 `service` 模块 `_vfs/nop/main/report/<domain>/`。

## 模板类型

| 类型 | 用途 | 示例 | 平台载体 |
|------|------|------|----------|
| LIST | 列表/台账报表 | 账龄表、总账余额表 | `NopReportDefinition` + `.xpt.*` |
| DETAIL | 单据明细打印/套打 | 采购订单、发票、凭证 | `NopReportDefinition` + `.xpt.*`（套打用 `ExcelImage.print=false` 背景图） |
| FORM | 表单/标签打印 | 条码、二维码标签 | `NopReportDefinition` + `.xpt.*` |

## 实现机制

- **种子报表随仓 `.xpt.*` 维护**（开发者可版本化、可审计）；运行时模板定义经平台 `NopReportDefinition` / `NopReportDataset` / `NopReportDatasource` CRUD 管理（平台已内建管理页面）。
- 共存策略：VFS 种子模板优先；运行时 DB 定义作为覆盖/扩展。冲突时按"VFS 种子优先 + DB 覆盖"在对应 owner doc 记录。
- 按租户+账套+单据类型维度配置（`NopReportDefinition` 维度字段 + `NopReportDefinitionAuth` 角色权限）。
- 支持模板版本管理（`NopReportDefinition` 版本字段）。
- 三种输出载体：HTML（屏幕预览）、XLSX（OOXML）、PDF（PDFBox 直接渲染，**非 xlsx 转换**）。

## 应用层接线（已落地）

财务域渲染入口：`app.erp.fin.service.report.ErpFinReportBizModel`（`@BizModel("ErpFinReport")`）：
- 注入 `IReportEngine`，`@BizQuery renderHtml(reportName, data)` / `download(reportName, renderType, data)`。
- 模板名经 `StringHelper.isValidVPath` 校验防路径注入（参考 `ReportDemoBizModel`）。
- 五张财务种子报表模板：`module-finance/erp-fin-service/src/main/resources/_vfs/nop/main/report/fin/`（资产负债表/利润表/现金流量表/AR-AP 账龄/期末结账报告）。
- 报表口径以 `docs/design/finance/` 为业务真相，数据集经 `IEvalScope` 注入模板。

制造域渲染入口：`app.erp.mfg.service.report.ErpMfgReportBizModel`（`@BizModel("ErpMfgReport")`）：
- 镜像 `ErpFinReportBizModel` 域隔离范式，模板根 `/nop/main/report/mfg/`（与 `/fin/` 隔离，避免跨域 R 依赖）。
- 三张制造运营报表模板：`module-manufacturing/erp-mfg-service/src/main/resources/_vfs/nop/main/report/mfg/`（CRP 负荷报表 / 生产差异报表 / 预测差异报表）。
- 数据集聚合：CRP 负荷委托 `IErpMfgCrpLoadBiz.getLoadReport`（复用 1707-1 已审计的负荷/产能/超负荷计算）；生产差异从 `ErpMfgCostVariance`（1838-2 产物）聚合；预测差异从 `ErpMfgForecast`/`ErpMfgForecastLine`（0427-1，APPROVED）vs `ErpMfgWorkOrder` 完工数量对比。各报表口径以对应 owner doc 为业务真相。前端 AMIS 报表页面已接入（plan 2026-07-06-1247-3：`mfg-report` 菜单组 + crp-load-report/production-variance-report/forecast-variance-report 3 个 page.yaml，参数 workcenterId/workOrderId/materialId + 日期区间）。

**域隔离范式（复用约定）**：新域报表按 `ErpFinReportBizModel` / `ErpMfgReportBizModel` 范式复制——每域一个 `ErpXxxReportBizModel`（`@BizModel("ErpXxxReport")`），模板根 `/nop/main/report/<domain>/`，渲染入口 + 数据集聚合留在本域 service 内（不跨域 import）。

库存域渲染入口：`app.erp.inv.service.report.ErpInvReportBizModel`（`@BizModel("ErpInvReport")`，plan 2026-07-06-1247-1）：
- 镜像 `ErpMfgReportBizModel` 域隔离范式，模板根 `/nop/main/report/inv/`。
- 一张库存追溯可视化报表模板：`module-inventory/erp-inv-service/src/main/resources/_vfs/nop/main/report/inv/`（库存追溯链可视化，表格汇总口径）。
- 数据集聚合：`buildInventoryTraceDataset` 经同域 `IErpInvStockMoveBiz` 的 4 个追溯方法（forwardTrace/backwardTrace/returnTrace/batchTrace，0700-1 落地）聚合批次/物料的移动链路（originMoveId/originReturnedMoveId 上下游 + 批次号 + 退货标记 + 数量），口径对齐 `docs/design/inventory/trace-chain.md §追溯链查询`。前端 AMIS 报表页面已接入（plan 2026-07-06-1247-3：`inv-report` 菜单组 + `inventory-trace-report.page.yaml`，参数 batchNo/materialId/warehouseId）；交互式树/图可视化归前端 successor。

HR 域渲染入口：`app.erp.hr.service.report.ErpHrReportBizModel`（`@BizModel("ErpHrReport")`，plan 2026-07-06-1247-1）：
- 镜像域隔离范式，模板根 `/nop/main/report/hr/`。
- 两张 HR 报表模板：`module-hr/erp-hr-service/src/main/resources/_vfs/nop/main/report/hr/`（员工净余额 / 薪酬模拟对比）。
- 数据集聚合：员工净余额 `buildEmployeeNetBalanceDataset` 经跨域只读 `IErpFinArApItemBiz.findOpenItems(direction, ctx)` 聚合 finance 辅助账（按 sourceBillType=EMPLOYEE_ADVANCE/EXPENSE_CLAIM 二次过滤出员工项，净额=预支应收−报销应付，口径对齐 `docs/design/finance/expense-claim.md`）；薪酬模拟对比 `buildPayrollSimulationComparisonDataset` 从同域 `ErpHrSalarySimulationItemAdjustment`（2200-3）聚合源 vs 调整三列对比 + 部门小计，对齐 `docs/design/human-resource/payroll-simulation.md`。前端 AMIS 报表页面已接入（plan 2026-07-06-1247-3：`hr-report` 菜单组 + `employee-net-balance.page.yaml`（无参数，后端聚合全部员工）+ `payroll-simulation-comparison.page.yaml`（参数 simulationId））。

剩余 7 域渲染入口（plan 2026-07-06-1815-1，均镜像域隔离范式，各域 `Erp*Errors` 独立扩展 `ERR_REPORT_*` 不跨域 import）：
- **assets** `app.erp.ast.service.report.ErpAstReportBizModel`（`@BizModel("ErpAstReport")`，模板根 `/nop/main/report/ast/`）：资产折旧明细（`ErpAstAsset`+`ErpAstDepreciationSchedule` 按资产聚合原值/累计折旧/净值/本期折旧）+ 资产处置明细（`ErpAstDisposal` 处置行+清理损益），对齐 `assets/state-machine.md`。
- **projects** `app.erp.prj.service.report.ErpPrjReportBizModel`（模板根 `/nop/main/report/prj/`）：项目成本汇总（`ErpPrjProject` actualCost/budget + 预算执行率，实时聚合不引入 `ErpPrjProjectPnl`）+ 工时明细（`ErpPrjTimesheet` 按项目×员工聚合），对齐 `projects/cost-collection.md`。
- **maintenance** `app.erp.mnt.service.report.ErpMntReportBizModel`（模板根 `/nop/main/report/mnt/`）：维护历史（`ErpMntVisit`⨝`ErpMntVisitTask`+备件消耗单数）+ 停机统计（`ErpMntDowntimeEntry` 按设备×原因聚合停机分钟），对齐 `maintenance/state-machine.md`。
- **quality** `app.erp.qa.service.report.ErpQaReportBizModel`（模板根 `/nop/main/report/qa/`）：质检合格率统计（`ErpQaInspection` 按物料聚合，ACCEPTED+CONDITIONAL 视为合格）+ NCR-CAPA 统计（`ErpQaNonConformance`+`ErpQaAction` 按严重度聚合），对齐 `quality/state-machine.md`。
- **master-data** `app.erp.md.service.report.ErpMdReportBizModel`（模板根 `/nop/main/report/md/`）：物料价格清单（`ErpMdMaterial`⨝默认`ErpMdMaterialSku` 四档价格）+ 往来单位清单（`ErpMdPartner` 客户/供应商），对齐 `master-data/README.md`。
- **crm** `app.erp.crm.service.report.ErpCrmReportBizModel`（模板根 `/nop/main/report/crm/`）：线索转化漏斗（`ErpCrmLead` 按 stage 聚合线索数/期望收入）+ 销售预测准确率（`ErpCrmForecast`+`ErpCrmForecastLine` commit/weighted vs 行加权合计），对齐 `crm/README.md`+`crm/sales-forecast.md`。
- **customer-service** `app.erp.cs.service.report.ErpCsReportBizModel`（`@BizModel("ErpCsReport")`，模板根 `/nop/main/report/cs/`，包 `app.erp.cs`，模块 `module-cs`）：工单 SLA/CSAT 综合统计（`ErpCsTicket` SLA 命中/超时 + `ErpCsSurvey` csat/nps 均值，按工单类型聚合），对齐 `customer-service/sla.md`+`customer-service/csat.md`。
- 7 域 AMIS 报表菜单/页面归前端 successor（plan 2026-07-06-1815-2）。

## 单据打印（DETAL/套打，后续计划）

发票/凭证/订单套打是相关但独立的能力面（套打背景图 + 单据维度模板）。本期仅交付财务报表渲染能力；单据打印模板制作归后续计划，复用同一 `NopReportDefinition` + `IReportEngine` 入口（无需新建实体）。

## 打印触发

- 手动打印/下载：用户在 UI 点击（报表页面经 `ErpFinReportBizModel` 渲染入口）。
- 自动/批量打印：归 nop-job/nop-batch 后继（`docs/architecture/job-scheduling.md`）。
