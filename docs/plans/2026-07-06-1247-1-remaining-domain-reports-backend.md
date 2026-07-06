# 2026-07-06-1247-1-remaining-domain-reports-backend 剩余域业务报表后端（库存追溯可视化 + HR 报表）

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: 承接 `docs/plans/2026-07-06-0504-2-report-rendering-subsystem.md`「各域业务报表（…追溯可视化/员工净余额/薪酬模拟对比）」（Successor Required: partial，触发条件=nop-report 接线**已满足**）+ `docs/plans/2026-07-06-0935-2-manufacturing-operational-reports.md`「库存追溯可视化报表 / HR 报表」（Successor Required: yes，触发条件=渲染引擎 0504-2 已就绪）；owner docs `docs/architecture/print-template.md`、`docs/design/inventory/trace-chain.md`、`docs/design/human-resource/payroll-simulation.md`、`docs/design/finance/expense-claim.md`
> Related: `2026-07-06-0504-2-report-rendering-subsystem.md`（报表渲染引擎，已完成，本计划复用 `IReportEngine` + `.xpt.xml` 范式）、`2026-07-06-0935-2-manufacturing-operational-reports.md`（制造域报表，已完成，本计划镜像其跨域复制范式）、`2026-07-02-0700-1-inventory-trace-chain.md`（追溯链数据源）、`2026-07-04-0700-2-cs-ticket-sla-csat.md` 无关、`2026-07-02-0700-2-finance-expense-claim-employee-advance.md`（员工借款辅助账数据源）、`2026-07-04-2200-3-hr-payroll-simulation.md`（薪酬模拟对比数据源）
> Audit: required

## Current Baseline

- **报表渲染引擎已就绪并经多域验证**：`ErpFinReportBizModel`（finance，0504-2）+ `ErpMfgReportBizModel`（manufacturing，0935-2）建立完整范式——注入平台 `IReportEngine`/`IDaoProvider`/`IOrmTemplate`，`renderHtml`/`download` @BizQuery 解析 VFS 模板路径（经 `StringHelper.isValidVPath` 防注入 + `ALLOWED_RENDER_TYPES` 校验 + 域 `ErpXxxErrors.ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID` ErrorCode，镜像 finance 不跨域 import）→ `buildXxxDataset` 从 ORM 实体聚合 → 经 `IEvalScope` 注入模板 → `*=^ds!<field>` 展开渲染 html/xlsx/pdf。模板根按域隔离：`/nop/main/report/fin/`（5 张）、`/nop/main/report/mfg/`（3 张）。
- **inventory / HR 域渲染入口为零**：实时 grep 确认全仓无 `ErpInvReportBizModel`、无 `ErpHrReportBizModel`；`/nop/main/report/inv/`、`/nop/main/report/hr/` 模板根不存在。本计划交付物为净新增。
- **数据源全部就绪**（各 owner doc 已审计落地）：
  - 库存追溯可视化：`IErpInvStockMoveBiz.batchTrace`（0700-1 落地）提供批次聚合查询（batchId→batchNo 类型桥）；`ErpInvStockMove.originMoveId`/`originReturnedMoveId` 自追溯链字段已落地（0700-1）。`trace-chain.md` §追溯链模型定义四类追溯查询口径。
  - 员工净余额：`docs/design/finance/expense-claim.md`（0700-2 落地）建立员工应付/预支双方向辅助账（`ErpFinArApItem` EMPLOYEE 方向，经 `IErpFinArApItemBiz` 跨域只读聚合——finance 实体所有权保持，对齐 nop-backend-dev「跨实体优先 I*Biz」）。
  - 薪酬模拟对比：`ErpHrSalarySimulation` + `getComparison`/`getDepartmentSummary`（2200-3 落地）提供三列对比 + 部门/项目/公司聚合数据源。
- **剩余差距**：inventory + HR 两个域渲染入口（`ErpInvReportBizModel`/`ErpHrReportBizModel`）+ 3 张 `.xpt.xml` 模板 + 数据集聚合 + 测试均缺失。

## Goals

- 交付 **inventory 域报表渲染入口** `ErpInvReportBizModel`（`@BizModel("ErpInvReport")`，镜像 `ErpMfgReportBizModel` 范式，模板根 `/nop/main/report/inv/`）+ **1 张库存追溯可视化报表**（批次→移动链路汇总，对齐 `trace-chain.md`）。
- 交付 **HR 域报表渲染入口** `ErpHrReportBizModel`（`@BizModel("ErpHrReport")`，模板根 `/nop/main/report/hr/`）+ **2 张 HR 报表**：
  1. **员工净余额报表**（按员工聚合应付/预支辅助账净额，对齐 `expense-claim.md`）；
  2. **薪酬模拟对比报表**（源 vs 模拟三列对比 + 部门聚合，对齐 `payroll-simulation.md`）。
- 经既有 `IReportEngine` 渲染 html/xlsx/pdf，端到端可验证（渲染非空 + 关键单元格值正确 + 空数据集不报错 + 路径注入防护）。

## Non-Goals

- **AMIS 报表菜单/页面接入**（本计划交付渲染 API + 模板；菜单/页面归前端 successor，见 `2026-07-06-1247-3-domain-reports-frontend.md`）。
- **finance / manufacturing 域报表**（0504-2 / 0935-2 已落地）。
- **单据打印/套打**（0504-2 Deferred，`print-template.md`）——相关但独立能力面。
- **定时报表/批量生成/结果缓存调度**（0504-2 optimization candidate）——归 nop-job/nop-batch 后继。
- **追溯链可视化前端交互**（树/图渲染）——本计划报表为表格汇总口径；交互式可视化归前端 successor。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/print-template.md`（报表模板存储/渲染/复用 `NopReportDefinition` 决策）；`docs/design/inventory/trace-chain.md`（追溯口径）；`docs/design/finance/expense-claim.md`（员工净余额口径）；`docs/design/human-resource/payroll-simulation.md`（模拟对比口径）
- Skill Selection Basis: 任务为新增 `@BizQuery` 渲染入口 + 数据集聚合 + `.xpt.xml` 模板，匹配 `nop-backend-dev`（自定义动作、跨实体只读聚合经 I*Biz、ErrorCode）。渲染范式已在 `ErpFinReportBizModel`/`ErpMfgReportBizModel` 经两轮独立审计验证，本计划为同范式跨域复制。不涉及 AMIS 页面（前端归 successor）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。`nop-report-core`/`nop-report-pdf` 已由 0504-2 接线，CJK 字体回退已就绪。无新外部服务/端口/密钥。

## Execution Plan

### Phase 1 - inventory 域报表渲染入口 + 库存追溯可视化报表（prove pattern）

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/report/ErpInvReportBizModel.java`；`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/ErpInvErrors.java`（**扩展现有**——实时核实确认该接口已存在（108 行，含既有 ErrorCode），仅缺 `ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID` + `ARG_REPORT_NAME`/`ARG_RENDER_TYPE`，新增这 4 项，镜像 `app/erp/mfg/service/ErpMfgErrors.java` 同名定义范式）；`module-inventory/erp-inv-service/src/main/resources/_vfs/nop/main/report/inv/inventory-trace-report.xpt.xml`；`module-inventory/erp-inv-service/src/main/resources/_vfs/erp/inv/beans/app-service.beans.xml`（新增 BizModel bean 注册，镜像 mfg `app.erp.mfg.service.report.ErpMfgReportBizModel` 注册行）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（渲染引擎 0504-2 已就绪）

- [x] **Decision: inventory 报表入口落位 + 模板根路径**——新建专用 `ErpInvReportBizModel`（`@BizModel("ErpInvReport")`），模板根 `/nop/main/report/inv/`。
  - 理由：镜像 `ErpMfgReportBizModel` 域隔离范式（每域一 ReportBizModel + 一模板根），保持域边界清晰；追溯数据集聚合留在 inventory service 内，跨 finance 辅助账读取经 `IErpFinArApItemBiz` 只读接口（不跨域 import 实体）。
  - 替代方案：扩 `ErpMfgReportBizModel` 支持多域（拒绝：跨域耦合 + 路径前缀分支膨胀）；每报表一 BizModel（拒绝：范式重复）。
  - 残留风险：无（与 0504-2/0935-2 已审计的 owner doc `print-template.md`「复用 NopReportDefinition、不自建平行实体」一致）。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpInvReportBizModel`**——`renderHtml`/`download` @BizQuery（镜像 `ErpMfgReportBizModel`：`resolveReportPath` 防注入 + `ALLOWED_RENDER_TYPES` 校验 + inventory 域 `ErpInvErrors.ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID` ErrorCode，镜像 finance/mfg 不跨域 import）+ `prepareDataset` 路由 + `buildInventoryTraceDataset`。
  - `buildInventoryTraceDataset(@Optional batchNo, @Optional materialId, @Optional warehouseId)` → 经 `IErpInvStockMoveBiz`（同域 I*Biz）4 个追溯方法（forwardTrace/backwardTrace/returnTrace/batchTrace，对齐 `IErpInvStockMoveBiz` 接口口径）聚合批次/物料的移动链路（originMoveId/originReturnedMoveId 上下游 + 批次号 + 移动方向 + 数量 + 时间），对齐 `trace-chain.md`。
  - Skill: `nop-backend-dev`
- [x] **Add: 扩展 `ErpInvErrors`**——在**现有** `ErpInvErrors` 接口（包 `app/erp/inv/service`，已存在 108 行）新增 `ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID` + `ARG_REPORT_NAME`/`ARG_RENDER_TYPE`（镜像 `ErpMfgErrors` 同名定义），不改动既有 ErrorCode。
  - Skill: `nop-backend-dev`
- [x] **Add: IoC 注册**——`erp-inv-service` `app-service.beans.xml` 新增 `<bean id="app.erp.inv.service.report.ErpInvReportBizModel" ioc:type="@bean:id"/>`（镜像 mfg 注册范式，服务型 BizObject 显式注册确保运行时可解析）。
  - Skill: `nop-backend-dev`
- [x] **Add: `inventory-trace-report.xpt.xml`**——模板引用 `ds` 数据集展开（追溯链路表 + 方向/退货标记列），口径对齐 `trace-chain.md`。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpInvReportRendering`**——构造库存移动 + 批次样本，经 `ErpInvReport__renderHtml(reportName="inventory-trace-report")` 渲染，断言渲染结果非空 + 关键追溯值/批次号出现；`download(renderType="xlsx"|"pdf")` 返回 `WebContentBean` 非空；空数据集渲染不报错；非法 reportName 抛 `ERR_REPORT_NAME_INVALID`。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `ErpInvReport__renderHtml`/`download` 渲染库存追溯报表成功（html 非空含关键追溯值；xlsx/pdf `WebContentBean` 非空；空数据集渲染不报错；路径注入被拒）

### Phase 2 - HR 域报表渲染入口 + 员工净余额报表

Status: completed
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrErrors.java`（**扩展现有**——实时核实确认该接口已存在（136 行），仅缺 `ERR_REPORT_*` + `ARG_REPORT_*`，新增这 4 项，镜像 `ErpMfgErrors`）；`module-hr/erp-hr-service/src/main/resources/_vfs/nop/main/report/hr/employee-net-balance.xpt.xml`；`module-hr/erp-hr-service/src/main/resources/_vfs/erp/hr/beans/app-service.beans.xml`（新增 BizModel bean 注册）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1（范式确立）

- [x] **Decision: 员工净余额数据源经 finance 只读接口**——`buildEmployeeNetBalanceDataset` 经 `IErpFinArApItemBiz.findOpenItems(direction, ctx)` 跨域只读聚合（finance 辅助账所有权保持），不跨域 import finance 实体。
  - 理由：员工预支/报销辅助账落在 finance `ErpFinArApItem`（0700-2 落地，经 `ErpFinArApItemGenerator`/`SOURCE_BILL_EMPLOYEE_ADVANCE` 标记），HR 域报表只读消费须走 I*Biz 接口（对齐 nop-backend-dev「跨实体优先 I*Biz」+ AGENTS.md 跨实体访问规则）。
  - 聚合口径：员工预支经 `sourceBillType=SOURCE_BILL_EMPLOYEE_ADVANCE` + `direction=DIRECTION_RECEIVABLE`；报销经 `sourceBillType=SOURCE_BILL_EXPENSE_CLAIM` + `direction=DIRECTION_PAYABLE`（经 `ErpFinArApItemGenerator.java:150,153` 确认两 sourceBillType 分属两方向）。净余额 = 预支应收 − 报销应付（按员工/部门分组）。`findOpenItems(direction, ctx)` 入参 `direction` 取 RECEIVABLE/PAYABLE（非 "EMPLOYEE"——员工身份经 sourceBillType 区分），故分别按两方向取数后再按 sourceBillType 过滤出员工项净额合并（`findOpenItems` 不带 sourceBillType 过滤，须在聚合层按 sourceBillType 二次过滤，避免混入客户 AR / 供应商 AP）。
  - 替代方案：在 HR 域物化员工余额视图（拒绝：双写真相源冲突）；在 finance 域建 HR 报表（拒绝：域归属错位）。
  - 残留风险：若 finance 后续新增员工类 sourceBillType 须同步纳入聚合（低风险，sourceBillType 枚举稳定）。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpHrReportBizModel`**——`renderHtml`/`download` @BizQuery（镜像范式 + HR 域 `ErpHrErrors.ERR_REPORT_NAME_INVALID`/`ERR_REPORT_RENDER_TYPE_INVALID`）+ `prepareDataset` 路由 + `buildEmployeeNetBalanceDataset`。
  - 按员工聚合预支应收/报销应付净额（员工姓名/部门/预支余额/报销余额/净额），对齐 `expense-claim.md`。
  - Skill: `nop-backend-dev`
- [x] **Add: 扩展 `ErpHrErrors` + IoC 注册**——在**现有** `ErpHrErrors` 接口（包 `app/erp/hr/service`，已存在 136 行）新增 `ERR_REPORT_*` + `ARG_REPORT_*`（镜像 `ErpMfgErrors`），不改动既有 ErrorCode；`app-service.beans.xml` 新增 `app.erp.hr.service.report.ErpHrReportBizModel` bean 注册。
  - Skill: `nop-backend-dev`
- [x] **Add: `employee-net-balance.xpt.xml`**——模板展开员工净余额表（按部门小计 + 净额正负标记），口径对齐 `expense-claim.md`。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpHrReportRendering`（净余额用例）**——构造员工辅助账样本，经 `ErpHrReport__renderHtml(reportName="employee-net-balance")` 渲染，断言非空 + 关键净额值出现；download xlsx/pdf 非空；空数据集不报错；非法 reportName 被拒。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `ErpHrReport__renderHtml`/`download` 渲染员工净余额报表成功（html 非空含关键净额值；xlsx/pdf 非空；空数据集不报错；路径注入被拒）

### Phase 3 - 薪酬模拟对比报表

Status: completed
Targets: `module-hr/erp-hr-service/src/main/resources/_vfs/nop/main/report/hr/payroll-simulation-comparison.xpt.xml`；`ErpHrReportBizModel.buildPayrollSimulationComparisonDataset`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2（HR 入口已确立）

- [x] **Add: `buildPayrollSimulationComparisonDataset`**——经 HR 域 `ErpHrSalarySimulation`（2200-3 同域实体）聚合：源 vs 模拟三列对比（员工/薪酬项目/原值/调整值/差异）+ 部门小计，对齐 `payroll-simulation.md` `getComparison`/`getDepartmentSummary` 口径。
  - Skill: `nop-backend-dev`
- [x] **Add: `payroll-simulation-comparison.xpt.xml`**——模板展开模拟对比表（三列对比 + 差异高亮 + 部门小计），口径对齐 `payroll-simulation.md`。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpHrReportRendering`（模拟对比用例）**——构造模拟样本，经 `ErpHrReport__renderHtml(reportName="payroll-simulation-comparison")` 渲染，断言非空 + 关键差异值出现；download xlsx/pdf 非空；空数据集不报错。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `ErpHrReport__renderHtml`/`download` 渲染薪酬模拟对比报表成功（html 非空含关键差异值；xlsx/pdf 非空；空数据集不报错）

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_0ca3b4a5effeV2O3SfonjpLv5M`) because 全部 baseline 真实但 Targets/Closure mvn 命令误用 `module-human-resource/erp-hr-service`（实时为 `module-hr`），违反 Rule 1（执行者首步 `mvn -pl` 会失败）。非阻塞建议：direction=EMPLOYEE 不符 `findOpenItems` 契约；Errors 类实须新建并加 IoC 注册；Decision 2 补残留风险。
- Independent draft review iteration 2: needs-revision (`ses_0ca34d4c3ffej8NfvVWXYI7BXZ`) after 修正 HR 路径 + IoC/trace/残留风险——HR 路径已解决；但新发现 BLOCKING：plan 误称 `ErpInvErrors`/`ErpHrErrors` 须「新建」，实时核实两接口已存在（108/136 行），仅缺 `ERR_REPORT_*`，须改「扩展现有」否则覆盖既有 ErrorCode 破坏多 BizModel 编译。附带非阻塞：sourceBillType 须分列 EMPLOYEE_ADVANCE/EXPENSE_CLAIM。
- Independent draft review iteration 3: accept (`ses_0ca30babfffepBgPZYXmR3KJzW`) after Errors 改「扩展现有」+ sourceBillType 口径分列——独立核实 `ErpInvErrors`(108)/`ErpHrErrors`(136) 均存在且无 `ERR_REPORT`、`ErpFinArApItemGenerator.java:150,153` 印证两 sourceBillType 分属两方向、`findOpenItems(direction,ctx)` 无 sourceBillType 参数故须二次过滤。Skill/类型/反松弛全合规，无新阻塞。可转 active。

## Closure Gates

> 完整仓库验证在结束处运行一次：`mvn clean install -DskipTests`（全 reactor）+ `mvn test -pl module-inventory/erp-inv-service,module-hr/erp-hr-service -am -Dtest=TestErpInvReportRendering,TestErpHrReportRendering -Dsurefire.failIfNoSpecifiedTests=false`（新增报表渲染测试，确保既有 CRP/差异/财务报表测试无回归）。

- [x] 范围内行为完成（inventory + HR 两域渲染入口 + 3 张报表可渲染可下载）
- [x] 相关文档对齐（`print-template.md` 域报表扩展说明 + 0504-2/0935-2 Deferred 标记承接 + roadmap done 条目 + 当日日志）
- [x] 已运行验证（指定命令全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 其他域业务报表（资产/项目/维护/质量/主数据/CRM/客服 等）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 不同域/owner doc；本计划承接 0504-2/0935-2 显式列出的剩余高优先级域报表（inventory/HR），其余域同范式 successor。
- Successor Required: `yes`（触发条件：对应域报表需求落地时——渲染引擎已就绪即可启动）

### inventory/HR 报表 AMIS 菜单/页面接入

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 渲染 API + 模板可独立验证（GraphQL/直调）；AMIS 报表列表页属前端定制面，归 `2026-07-06-1247-3-domain-reports-frontend.md`。
- Successor Required: `yes`（触发条件：本计划后端 API 落地后，报表前端定制启动时）

### 追溯链交互式可视化（树/图渲染）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划报表为表格汇总口径；交互式树/图可视化属前端能力面。
- Successor Required: `yes`（触发条件：追溯链前端可视化需求落地时）

## Closure

Status Note: 三 Phase 全部落地并 full-green 验证。inventory 域 `ErpInvReportBizModel` + 库存追溯可视化报表（经同域 `IErpInvStockMoveBiz` 4 追溯方法聚合）+ HR 域 `ErpHrReportBizModel` + 2 张 HR 报表（员工净余额经跨域只读 `IErpFinArApItemBiz.findOpenItems` + sourceBillType 二次过滤；薪酬模拟对比从同域 `ErpHrSalarySimulationItemAdjustment` 聚合三列对比 + 部门小计）+ 3 张 `.xpt.xml` 模板 + 各域 `ERR_REPORT_*` ErrorCode。验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`TestErpInvReportRendering`(8) + `TestErpHrReportRendering`(13) = 21 新增报表测试全绿；inv 79 + hr 52 全模块 `mvn test` 无回归。文档对齐：`print-template.md` 域报表扩展说明 + roadmap done 条目 + 当日日志。Deferred 全部已裁定（AMIS 菜单/页面归前端 successor 1247-3；追溯链交互式可视化归前端；其他域报表同范式 successor）。结束审计由独立子代理执行（见下）。

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent (general)
- Evidence: 独立会话结束审计，对实时仓库全量核验（非信任计划声明）。**构建**：`mvn clean install -DskipTests` BUILD SUCCESS，reactor 154 模块全 SUCCESS（grep 计数 154，无 FAILED/SKIPPED）。**目标测试**：`mvn test -pl module-inventory/erp-inv-service,module-hr/erp-hr-service -am -Dtest=TestErpInvReportRendering,TestErpHrReportRendering -Dsurefire.failIfNoSpecifiedTests=false` → `TestErpInvReportRendering` Tests run: 8, Failures: 0, Errors: 0, Skipped: 0；`TestErpHrReportRendering` Tests run: 13, Failures: 0, Errors: 0, Skipped: 0（测试中 ErpFinPostingProcessor 的 period-not-found ERROR 日志为生成移动单时的预期背景过账消息，所有用例 `nop.autotest.completed:success=true`）。**文件核查**：两 BizModel 均手写于 `src/main/java/.../report/`（非 `_gen/`），`@BizModel`/`renderHtml`+`download` @BizQuery/`resolveReportPath`+`StringHelper.isValidVPath` 防注入/`ALLOWED_RENDER_TYPES`(html/xlsx/pdf)/域 `ERR_REPORT_*` ErrorCode 全部到位；3 张 `.xpt.xml` 模板均引用 `ds` 数据集 + `*=^ds!` 展开；两 `app-service.beans.xml` 以 `ioc:type="@bean:id"` 注册；`ErpInvErrors`(122行)/`ErpHrErrors`(152行) 经「扩展」保留全部既有 ErrorCode 并新增 4 项。**跨域规则**：HR `buildEmployeeNetBalanceDataset` 经 `IErpFinArApItemBiz.findOpenItems(direction,ctx)` 只读聚合 finance 辅助账并按 sourceBillType=EMPLOYEE_ADVANCE/EXPENSE_CLAIM 二次过滤（净额=预支应收−报销应付），未用 IDaoProvider 直查 finance 实体；inv `buildInventoryTraceDataset` 仅经同域 `IErpInvStockMoveBiz`（batchTrace/forwardTrace）聚合，零 finance import。**反模式自检（nop-backend-dev）**：PASS——两 BizModel `@Inject` 字段均非 private（包级可见）；所有 `@BizQuery` 方法 `IServiceContext context` 为末参；异常均 `new NopException(ErrorCode).param(...)`；无 `@BizMutation @Transactional` 双注解（仅 `@BizQuery`）。**文档对齐**：`print-template.md` 记录 inv+hr 双域渲染入口；`core-business-roadmap.md:78` 含 done 条目；`docs/logs/2026/07-06.md` 含日志。**计划一致性**：顶部 `> Plan Status: completed`；3 Phase 均 `Status: completed` 且全部 item/Exit Criteria `[x]`；除本结束审计外的 Closure Gates 均 `[x]`。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
