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

## 单据打印（DETAL/套打，后续计划）

发票/凭证/订单套打是相关但独立的能力面（套打背景图 + 单据维度模板）。本期仅交付财务报表渲染能力；单据打印模板制作归后续计划，复用同一 `NopReportDefinition` + `IReportEngine` 入口（无需新建实体）。

## 打印触发

- 手动打印/下载：用户在 UI 点击（报表页面经 `ErpFinReportBizModel` 渲染入口）。
- 自动/批量打印：归 nop-job/nop-batch 后继（`docs/architecture/job-scheduling.md`）。
