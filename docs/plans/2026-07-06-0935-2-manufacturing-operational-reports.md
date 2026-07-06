# 2026-07-06-0935-2-manufacturing-operational-reports 制造运营报表（CRP 负荷/生产差异/预测差异）

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: deferred 项承接 `docs/plans/2026-07-06-0504-2-report-rendering-subsystem.md`「各域业务报表（制造差异/CRP/预测差异…）」（Successor Required: yes，触发条件=nop-report 接线，**已满足**——0504-2 交付 `ErpFinReportBizModel` + `IReportEngine` 接线）；0504-2 Closure 下游 Deferred 清单显式列「1707-1 CRP 报表 / 1838-2 差异报表 / 0427-1 预测差异报表」各域后续计划可启动；owner docs `docs/architecture/print-template.md`、`docs/design/manufacturing/crp.md`、`docs/design/manufacturing/state-machine.md`、`docs/design/manufacturing/mrp.md`
> Related: `2026-07-06-0504-2-report-rendering-subsystem.md`（报表渲染引擎，已完成，本计划复用其 `IReportEngine` + `.xpt.xml` 范式）、`2026-07-03-1707-1-manufacturing-crp-load-engine.md`（CRP 引擎，数据源）、`2026-07-05-1838-2-manufacturing-production-variance.md`（差异引擎，数据源）、`2026-07-05-0427-1-demand-forecast-entity-mrp-drp-source.md`（预测实体，数据源）
> Audit: required

## Current Baseline

- **报表渲染引擎已就绪**：`ErpFinReportBizModel`（erp-fin-service，`@BizModel("ErpFinReport")`）建立完整范式——注入平台 `IReportEngine`/`IDaoProvider`/`IOrmTemplate`，`renderHtml`/`download` @BizQuery 解析 VFS 模板路径（`/nop/main/report/fin/<name>.xpt.xml`，经 `StringHelper.isValidVPath` 防注入）→ `buildXxxDataset` 从 ORM 实体聚合 → 经 `IEvalScope` 注入模板 → 模板 `*=^ds!<field>` 展开渲染 html/xlsx/pdf。5 张财务种子报表（资产负债表/利润表/现金流量表/AR-AP 账龄/期末结账报告）已落地并测试全绿。
- **0 张域业务报表模板**：全仓 `.xpt.xml` 仅 5 张财务报表（grep 确认 `/nop/main/report/fin/` 下），制造/库存/HR 域报表模板为零。制造域无 `ErpMfgReportBizModel` 或等价渲染入口。
- **制造运营数据源全部就绪**：
  - CRP 负荷：`CrpLoadCalculator`（erp-mfg-service，1707-1 落地）产生工作中心×日期负荷快照（loadHours/setupHours/capacityHours/loadRate/overloaded）。
  - 生产差异：`ProductionVarianceCalculator` + `ErpMfgCostVarianceBizModel` 实体（1838-2 落地）记录差异类型/金额/工单。
  - 需求预测：`ErpMfgForecast`/`ErpMfgForecastLine`（0427-1 落地，头-行 cascade-delete，`erp-mfg/forecast-status` 字典 DRAFT/APPROVED/CONSUMED/CANCELLED）。
- **报表名防注入与下载范式可复用**：`ErpFinReportBizModel.resolveReportPath`/`download`（`ALLOWED_RENDER_TYPES`、临时资源 + 定时清理、`WebContentBean`）为已验证范式，本计划镜像之。
- **剩余差距**：制造域渲染入口（`ErpMfgReportBizModel`）+ 3 张 `.xpt.xml` 模板 + 数据集聚合 + 测试均缺失。

## Goals

- 交付**制造运营报表渲染入口** `ErpMfgReportBizModel`（`@BizModel("ErpMfgReport")`，镜像 `ErpFinReportBizModel` 范式，模板根 `/nop/main/report/mfg/`）。
- 交付 **3 张制造运营报表** `.xpt.xml` 模板 + 数据集聚合，口径对齐各 owner doc：
  1. **CRP 负荷报表**（工作中心×日期负荷/产能/负荷率/超负荷，对齐 `manufacturing/crp.md`）；
  2. **生产差异报表**（差异类型/金额/工单维度汇总，对齐 1838-2 差异引擎输出）；
  3. **预测差异报表**（预测 vs 实际消耗对比，对齐 `manufacturing/mrp.md` + 0427-1 预测实体）。
- 经既有 `IReportEngine` 渲染 html/xlsx/pdf，端到端可验证（渲染非空 + 关键单元格值正确）。

## Non-Goals

- **库存追溯可视化报表**（0700-1 Deferred）——不同域（inventory）/owner doc（`trace-chain.md`），独立 successor。
- **HR 报表**（员工净余额 0700-2 Deferred / 薪酬模拟对比 2200-3 Deferred）——不同域（HR）/owner doc，独立 successor。
- **单据打印/套打**（0504-2 Deferred，`print-template.md`）——相关但独立能力面（套打背景图 + 单据维度模板）。
- **定时报表/批量生成/结果缓存调度**（0504-2 optimization candidate）——归 nop-job/nop-batch 后继。
- **报表前端菜单/页面接入**（AMIS 报表列表页）——本计划交付渲染 API + 模板；菜单/页面接入归前端 successor（财务报表菜单已在 0504-2 接入，制造报表菜单可同步接入但非本计划硬性退出标准，见 Closure Gates 文档对齐项）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/print-template.md`（报表模板存储/渲染/复用 `NopReportDefinition` 决策）；`docs/design/manufacturing/crp.md`（CRP 负荷口径）、`docs/design/manufacturing/state-machine.md`（工单/差异状态）、`docs/design/manufacturing/mrp.md`（预测/实际需求）
- Skill Selection Basis: 任务为新增 `@BizQuery` 渲染入口 + 数据集聚合 + `.xpt.xml` 模板，匹配 `nop-backend-dev`（自定义动作、跨实体只读聚合、ErrorCode）。渲染范式已在 `ErpFinReportBizModel` 验证，本计划为同范式跨域复制。不涉及 AMIS 页面（前端归 successor）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。`nop-report-core`/`nop-report-pdf` 已由 0504-2 接线，CJK 字体回退已就绪。无新外部服务/端口/密钥。

## Execution Plan

### Phase 1 - 制造报表渲染入口 + CRP 负荷报表（prove pattern）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/report/ErpMfgReportBizModel.java`；`module-manufacturing/erp-mfg-service/src/main/resources/_vfs/nop/main/report/mfg/crp-load-report.xpt.xml`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（渲染引擎 0504-2 已就绪）

- [x] **Decision: 制造报表入口落位 + 模板根路径**——新建专用 `ErpMfgReportBizModel`（`@BizModel("ErpMfgReport")`），模板根 `/nop/main/report/mfg/`。
  - 理由：镜像 `ErpFinReportBizModel` 域隔离范式（finance→`/fin/`，manufacturing→`/mfg/`），保持域边界清晰；避免跨域 R 依赖（制造数据集聚合留在制造 service 内）。
  - 替代方案：扩 `ErpFinReportBizModel` 支持多域（拒绝：跨域耦合 + 路径前缀分支膨胀）；每报表一个 BizModel（拒绝：范式重复）。
  - 残留风险：无（与 0504-2 已审计的 owner doc `print-template.md`「复用 NopReportDefinition、不自建平行实体」一致）。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpMfgReportBizModel`**——`renderHtml`/`download` @BizQuery（镜像 `ErpFinReportBizModel`：`resolveReportPath` 经 `StringHelper.isValidVPath` 防注入 + `ALLOWED_RENDER_TYPES` 校验 + 制造域 `ErpMfgErrors.ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID` ErrorCode，镜像 finance 不跨域 import）+ `prepareDataset` 路由 + `buildCrpLoadDataset`。
  - `buildCrpLoadDataset(@Optional workcenterId, @Optional startDate, @Optional endDate)` → 从 CRP 负荷快照（1707-1 `CrpLoadCalculator` 产物）聚合：工作中心 × 日期 / loadHours / setupHours / capacityHours / loadRate / overloaded 标记，对齐 `manufacturing/crp.md`。
  - Skill: `nop-backend-dev`
- [x] **Add: `crp-load-report.xpt.xml`**——模板引用 `ds` 数据集展开（负荷表 + 超负荷高亮），口径对齐 `manufacturing/crp.md`。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpMfgReportRendering`**——构造 CRP 负荷快照样本，经 `ErpMfgReport__renderHtml(reportName="crp-load-report")` 渲染，断言渲染结果非空 + 关键负荷值/超负荷标记出现；`download(renderType="xlsx"|"pdf")` 返回 `WebContentBean` 非空。
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段交付的可观察结果 + 解除后续阶段阻塞的本地化检查。

- [x] `ErpMfgReport__renderHtml`/`download` 渲染 CRP 负荷报表成功（html 非空含关键负荷值；xlsx/pdf `WebContentBean` 非空；空数据集渲染不报错）
- [x] 渲染入口 + 模板根路径 Decision 已记录，后续 2 报表沿用同范式（解除 Phase 2-3 阻塞）

### Phase 2 - 生产差异报表

Status: completed
Targets: `ErpMfgReportBizModel.buildProductionVarianceDataset`；`_vfs/nop/main/report/mfg/production-variance-report.xpt.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] **Add: `buildProductionVarianceDataset(@Optional workOrderId, @Optional startDate, @Optional endDate)`** → 从 `ProductionVarianceCalculator`/`ErpMfgCostVarianceBizModel`（1838-2 产物）聚合：工单 × 差异类型 / 差异金额 / 标准成本 / 实际成本，对齐 1838-2 差异引擎输出与 `manufacturing/state-machine.md`。
  - Skill: `nop-backend-dev`
- [x] **Add: `production-variance-report.xpt.xml`**——差异汇总表（按工单/差异类型维度），口径对齐 1838-2。
  - Skill: `nop-backend-dev`
- [x] **Proof: 扩展 `TestErpMfgReportRendering`**——样本差异记录，渲染 `production-variance-report`，断言差异金额/类型出现。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 生产差异报表渲染成功（html 非空含差异金额/类型；空数据集不报错）

### Phase 3 - 预测差异报表

Status: completed
Targets: `ErpMfgReportBizModel.buildForecastVarianceDataset`；`_vfs/nop/main/report/mfg/forecast-variance-report.xpt.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] **Add: `buildForecastVarianceDataset(@Optional materialId, @Optional periodStart, @Optional periodEnd)`** → 预测 vs 实际对比：`ErpMfgForecast`/`ErpMfgForecastLine`（0427-1，status=APPROVED，按 `periodStart`/`periodEnd` 区间相交过滤）预测数量 vs 实际消耗/订单数量，按物料聚合 forecastQty/actualQty/variance/varianceRatio，对齐 `manufacturing/mrp.md` + 0427-1。
  - Skill: `nop-backend-dev`
- [x] **Add: `forecast-variance-report.xpt.xml`**——预测/实际/差异/差异率表（按物料维度），口径对齐 0427-1。
  - Skill: `nop-backend-dev`
- [x] **Proof: 扩展 `TestErpMfgReportRendering`**——样本预测 + 实际数据，渲染 `forecast-variance-report`，断言差异/差异率出现。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 预测差异报表渲染成功（html 非空含预测/实际/差异数值；空数据集不报错）

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (`ses_0cae9c3e0ffeik0AJmkG7w32DW`) — 全部 baseline 主张经实时仓库核实（`ErpFinReportBizModel` 范式、5 张财务 `.xpt.xml`/0 张制造模板、`CrpLoadCalculator`/`ProductionVarianceCalculator`/`ErpMfgForecast` 数据源、0504-2 Deferred 触发条件已满足、`print-template.md` 复用 NopReportDefinition 决策一致），规则 1-14/anti-slack/Exit 规则 7/命名/单结果面全通过。非阻塞 nit 已采纳：Source typo `0502-2`→`0504-2`、ErrorCode 收紧为制造域镜像 `ErpMfgErrors.ERR_REPORT_*`、Phase 3 参数 `periodId`→`periodStart/periodEnd`、forecast 字典补 `CONSUMED` 态。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（3 张制造运营报表经 `ErpMfgReport__renderHtml`/`download` 可渲染 html/xlsx/pdf）
- [x] 相关文档对齐（`print-template.md` 域报表扩展说明；`manufacturing/crp.md` 报表口径引用；0504-2 Deferred「各域业务报表-制造部分」标记承接 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（154+ reactor 模块全绿）+ `mvn test -pl module-manufacturing/erp-mfg-service -am`（制造报表测试全绿，含既有 CRP/差异/预测引擎测试无回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 库存追溯可视化报表 / HR 报表（员工净余额 / 薪酬模拟对比）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 不同域（inventory/HR）/owner doc，本计划聚焦制造域运营报表（渲染引擎已就绪后最高优先级域报表集）。
- Successor Required: `yes`（触发条件：对应域报表需求落地时——渲染引擎 0504-2 已就绪即可启动）

### 制造报表 AMIS 菜单/页面接入

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 渲染 API + 模板可独立验证（GraphQL/直调）；AMIS 报表列表页属前端定制面。
- Successor Required: `yes`（触发条件：制造报表前端展示需求落地时，镜像 0504-2 财务报表菜单接入范式）

## Closure

Status Note: 计划已执行完成。全部 3 个 Phase 执行完毕，制造运营报表渲染入口 `ErpMfgReportBizModel`（`@BizModel("ErpMfgReport")`，模板根 `/nop/main/report/mfg/`）+ 3 张制造运营报表（CRP 负荷/生产差异/预测差异）可经 `renderHtml`/`download` 渲染 html/xlsx/pdf，端到端测试全绿（`TestErpMfgReportRendering` 13 tests，0 failures/0 errors）。镜像 `ErpFinReportBizModel` 域隔离范式，制造域 `ErpMfgErrors.ERR_REPORT_*` ErrorCode，不跨域 import。CRP 负荷数据集委托 `IErpMfgCrpLoadBiz.getLoadReport` 复用 1707-1 已审计负荷/产能/超负荷计算；生产差异从 `ErpMfgCostVariance`（1838-2 产物）聚合；预测差异从 `ErpMfgForecast`/`ErpMfgForecastLine`（0427-1 APPROVED）vs `ErpMfgWorkOrder` 完工数量对比。验证：`mvn clean install -DskipTests` 154 模块全绿 + 全 workspace `mvn test` 全绿（既有 CRP/差异/预测引擎测试无回归）。文档对齐 `print-template.md` 域报表扩展说明 + `manufacturing/crp.md` 报表口径引用 + 0504-2 Deferred「各域业务报表-制造部分」标记承接 done + `core-business-roadmap.md` done 条目。

Closure Audit Evidence:

- 执行者已交付并自验证（非独立审计）：
  - `ErpMfgReportBizModel`：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/report/ErpMfgReportBizModel.java`（`renderHtml`/`download`/`resolveReportPath`/`prepareDataset`/`buildCrpLoadDataset`/`buildProductionVarianceDataset`/`buildForecastVarianceDataset` + 3 `@BizQuery` 原始数据入口 `crpData`/`productionVarianceData`/`forecastVarianceData`）。
  - `ErpMfgErrors`：`ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID` + `ARG_REPORT_NAME`/`ARG_RENDER_TYPE`（镜像 finance 不跨域 import）。
  - 3 张 `.xpt.xml` 模板：`_vfs/nop/main/report/mfg/{crp-load-report,production-variance-report,forecast-variance-report}.xpt.xml`。
  - IoC 注册：`app-service.beans.xml` 新增 `<bean id="app.erp.mfg.service.report.ErpMfgReportBizModel" ioc:type="@bean:id"/>`。
  - 测试：`TestErpMfgReportRendering`（13 tests：3 报表各 renderHtml/download/dataset/empty + 路径注入防护），全绿。
- 验证命令证据：`mvn clean install -DskipTests`（BUILD SUCCESS，154 模块）+ `mvn test -pl module-manufacturing/erp-mfg-service -am`（全绿，既有 CRP/差异/预测引擎测试无回归）+ 全 workspace `mvn test`（BUILD SUCCESS）。
- 文档对齐证据：`docs/architecture/print-template.md`（制造域渲染入口 + 域隔离范式复用约定）、`docs/design/manufacturing/crp.md`（报表渲染接线补注）、`docs/plans/2026-07-06-0504-2-*.md`（Deferred 制造部分标记 partial done）、`docs/backlog/core-business-roadmap.md`（done 条目）。
- **独立结束审计已通过**（independent closure auditor，新会话，未复用执行者上下文）：独立核实全部执行产物落地 + 语义一致性 + 运行时反空壳验证。
  - **执行产物落地核实**（grep/glob/read 实时仓库）：`ErpMfgReportBizModel.java`（422 行，`renderHtml`/`download`/`resolveReportPath`/`prepareDataset`/`buildCrpLoadDataset`/`buildProductionVarianceDataset`/`buildForecastVarianceDataset` + 3 `@BizQuery` 原始数据入口 `crpLoadData`/`productionVarianceData`/`forecastVarianceData`，全部真实方法体无空壳）；`ErpMfgErrors`（`ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID` + `ARG_REPORT_NAME`/`ARG_RENDER_TYPE`，镜像 finance 不跨域 import）；3 张 `.xpt.xml`（`crp-load-report`/`production-variance-report`/`forecast-variance-report`，含真实 `*=^ds!<field>` 展开与超负荷高亮样式）；IoC 注册 `app-service.beans.xml:95`（`<bean id="app.erp.mfg.service.report.ErpMfgReportBizModel" ioc:type="@bean:id"/>`，与 finance `ErpFinReportBizModel` 注册范式逐字对齐）；`TestErpMfgReportRendering.java`（13 个 `@Test`，覆盖 3 报表各 renderHtml/download(xlsx|pdf)/dataset/empty + 路径注入防护）。
  - **运行时反空壳验证**：`mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgReportRendering -Dsurefire.failIfNoSpecifiedTests=false` → `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0` `BUILD SUCCESS`。BizModel 经 IoC 注入可运行，模板渲染真实内容（非空 html、有效 xlsx/pdf File），数据集口径断言通过（CRP loadHours=9/overloaded=true；预测 forecastQty=100/actualQty=80/variance=-20）。
  - **Exit Criteria vs live repo**：3 Phase 退出标准逐条对照实时代码确认一致（不盲信 `[x]`）。
  - **Five-point 一致性**：Plan Status `completed` / 各 Phase Status `completed` / 各 Exit Criteria `[x]` / Closure Gates 全 `[x]` / Closure 证据非占位符——全部一致。
  - **Deferred honesty**：Deferred 项（库存追溯/HR 报表/制造 AMIS 菜单）均为不同域或前端 successor，无范围内缺陷或契约漂移隐藏。
  - **Docs sync**：`docs/logs/2026/07-06.md`（本计划聚合条目 + full-green 验证记录）、`docs/architecture/print-template.md`（制造域渲染入口 + 域隔离复用约定）、`docs/design/manufacturing/crp.md`（报表渲染接线补注）、`docs/plans/2026-07-06-0504-2-*.md`（Deferred 制造部分标记 `partial done`）、`docs/backlog/core-business-roadmap.md`（done 条目）——全部已更新。
  - 审计结论：**approved**，计划可关闭。
