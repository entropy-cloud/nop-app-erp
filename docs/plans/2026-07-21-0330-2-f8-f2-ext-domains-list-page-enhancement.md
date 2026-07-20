# 2026-07-21-0330-2-f8-f2-ext-domains-list-page-enhancement F8 ext + F2 long-tail — ext 8 域列表页搜索/筛选 + 只读视图收尾

> Plan Status: active
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/frontend-ui-roadmap.md` §F8（扩展域 8 个独立 plan 待启动）+ §F2（长尾只读实体逐域补齐归 Deferred）
> Related: `docs/plans/2026-07-20-0629-2-f8-f2-search-filter-and-readonly-views.md`（8 核心列表页 + 6 只读实体范式）；`docs/design/query-filter-patterns.md`（双筛选面 + filterOp 范式）；`docs/plans/2026-07-21-0330-1-f4p2-child-table-editor-p3-ext-domains.md`（同期 ext 域 child-table-editor，本计划与之并行但无强依赖）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-21，对 8 个 ext 域 `<form id="asideFilter">` + `<form id="query">` 现状 + 各域 ui-patterns.md 列表页章节 + 只读实体清单 + `docs/design/query-filter-patterns.md` 范式）：

### F8/F2 已落地范式（基线，plan `2026-07-20-0629-2`）

- **8 核心列表页 query + asideFilter 双筛选面**：inventory 4 只读（StockLedger/StockBalance/StockBatch/StockSerialNo — 长尾 StockMoveLine/StockTransferLog 见 F2）+ finance 2 只读（ErpFinGlBalance/ErpFinTrialBalance — 系统生成，无 CRUD）+ purchase/sales/finance 4 主域列表
- **6 核心 + 长尾只读实体实现「搜索 → 行点击 → 详情 dialog」模式**：CRUD 按钮移除（F1 Phase 1 覆盖）+ edit/add form `x:abstract="true"` 切断继承 + asideFilter + query 双筛选面 + dialog 详情
- **金额/数量列方向颜色**：正+负-（F6 已落地）
- **范式文档**：`docs/design/query-filter-patterns.md` 已冻结 1+2+3+4 节（双筛选面架构 / 8 核心字段集冻结表 / 只读实体清单 / 「搜索→行点击→详情」模式）

### ext 8 域 view.xml 现状

经抽样 8 域 `<form id="asideFilter">` + `<form id="query">`，**全部为 codegen 默认空 stub**：
- `module-{crm,cs,hr,aps,logistics,b2b,contract,drp}/erp-{module}-web/.../pages/{Head}/{Head}.view.xml` 中的 asideFilter/query 大多为空 `<form ... x:abstract="true"/>` 或仅含 code/name 2 字段
- 部分域经 F3 form 布局落地后，list 页 query 仍为 codegen 默认（F3 仅改 head form 不动 list filter）

### ext 8 域 ui-patterns.md 列表页筛选要求

经 grep 各域 `ui-patterns.md`，每域均明确给出 query/asideFilter 字段集与 filterOp 规范（典型例）：

| 域 | 列表页 | 双筛选面要求 | 现状 |
|----|--------|-------------|------|
| crm | ErpCrmLead 线索/商机 | leadType Tab + source/stage/owner 筛选 + 我的/团队/全部快捷 | codegen 默认 |
| crm | ErpCrmCampaign 营销活动 | date-between + status + ownerId | codegen 默认 |
| crm | ErpCrmForecast 预测 | periodId + ownerId + status | codegen 默认 |
| cs | ErpCsTicket 工单 | ticketTypeId + slaPolicyId + status + priority + agentId + createdAt range | codegen 默认 |
| cs | ErpCsSurvey 调查 | ticketId + status + createdAt range | codegen 默认 |
| hr | ErpHrEmployee 员工 | departmentId + positionId + status + entryDate range | codegen 默认 |
| hr | ErpHrAttendance 考勤 | employeeId + date range + shiftId | codegen 默认 |
| hr | ErpHrLeaveRequest 休假 | employeeId + status + leaveType + date range | codegen 默认 |
| hr | ErpHrPayrollBankFile 银行文件 | status + period + generateDate range | codegen 默认 |
| aps | ErpApsSchedule 排产方案 | status + workCenterId + date range | codegen 默认 |
| aps | ErpApsOperationOrder 操作单 | scheduleId + machineId + status + plannedStart range | codegen 默认 |
| aps | ErpApsDispatchLog 调度日志（只读） | scheduleId + operationId + dispatchDate range | codegen 默认 |
| logistics | ErpLogShipment 发运单 | carrierId + status + partnerId + shipDate range + 异常筛选 | codegen 默认 |
| logistics | ErpLogShipmentLog 追踪日志（只读） | shipmentId + status + logDate range | codegen 默认 |
| logistics | ErpLogCarrier 承运商 | carrierType + gatewayId + isActive | codegen 默认 |
| b2b | ErpB2bEdiDoc EDI 事务 | direction + state + partnerProfileId + docType + sentAt range | codegen 默认 |
| b2b | ErpB2bAsn ASN | partnerProfileId + state + expectedReceiptDate range | codegen 默认 |
| b2b | ErpB2bEdiLog EDI 日志（只读） | ediDocId + direction + processedAt range | codegen 默认 |
| contract | ErpCtContract 合同 | partnerId + status + contractType + effectiveDate range | codegen 默认 |
| contract | ErpCtRebateAgreement 返利协议 | partnerId + status + agreementType + effectiveDate range | codegen 默认 |
| drp | ErpDrpPlan DRP 计划 | status + warehouseId + planDate range | codegen 默认 |
| drp | ErpDrpParameter DRP 参数 | warehouseId + materialId + parameterType | codegen 默认 |

**共 22 个 ext 列表页**需 query + asideFilter 落地（crm 3 + cs 2 + hr 4 + aps 3 + logistics 3 + b2b 3 + contract 2 + drp 2 = 22；初稿"21"为加法错误，独立审计 ses_07ef781ddffe 复核）。

### ext 8 域只读实体（F2 长尾）清单

经各域 ORM + ui-patterns 抽样，**只读实体候选**（系统生成或日志性质，无人工 CRUD）：

| 域 | 只读实体 | 来源 |
|----|---------|------|
| aps | ErpApsDispatchLog | 调度引擎写入 |
| logistics | ErpLogShipmentLog | 网关回调写入 |
| logistics | ErpLogShipmentParcel | 部分场景由网关回传（部分手工，按 Explore 裁决） |
| b2b | ErpB2bEdiLog | EDI 网关写入 |
| crm | ErpCrmLeadConvLog | 线索转换引擎写入 |
| crm | ErpCrmLeadScore / ErpCrmLeadScoreLine | 评分引擎写入（部分手工，按 Explore 裁决） |
| crm | ErpCrmForecastAccuracy | 准确度计算引擎写入 |
| crm | ErpCrmFunnelStageMetrics | 漏斗聚合引擎写入 |
| crm | ErpCrmLeadSequenceProgress | 序列推进引擎写入 |
| hr | ErpHrLeaveBalance | 年初结转 + 请假审批引擎写入 |
| hr | ErpHrSurveyResult | 调查结果聚合 |
| contract | ErpCtApprovalRecord | 审批流程写入 |
| contract | ErpCtConsumptionLine | 上游单据回写（部分手工，按 Explore 裁决） |

**只读实体共 14 个候选**（11 confirmed + 3 pending Explore）：aps 1 + logistics 2 + b2b 1 + crm 6 + hr 2 + contract 2 = 14；其中 ErpLogShipmentParcel / ErpCrmLeadScore / ErpCtConsumptionLine 3 实体属"半只读"（部分场景由网关/引擎写入 + 部分手工），按 Explore 裁决保留 CRUD 或切断 edit/add form。需 F2 「搜索 → 行点击 → 详情 dialog」模式落地。

### 关键风险/缺口

- **ext 域 view.xml 普遍为 codegen 默认**：与 F8 核心域 plan 相同的 greenfield 状态，需要逐域 bounded-merge 补齐 asideFilter + query
- **leadType Tab/Chip 切换**（crm Lead）：F8 核心域 plan 未覆盖 Tab/Chip 切换 + 数据源刷新模式，需 Phase 0 Explore 裁决（参考 F13 看板视图但更简单）
- **「我的/团队/全部」快捷过滤**（crm Lead）：需后端支持按 ownerId/departmentId 过滤的 `@BizQuery` 或经 AMIS `quickFilter` 前端实现，需 Explore 裁决
- **特殊异常筛选**（logistics Shipment 的"仅显示网关异常/追踪超期/退回"）：可能需后端提供 `findExceptionList @BizQuery` 或前端 OR/IN 过滤，需 Explore 裁决
- **只读实体 CRUD 按钮是否已由 F1 全覆盖**：F1 Phase 1 覆盖 10 只读实体；本计划覆盖 ext 13 只读实体，需抽样核实是否有遗漏的 CRUD 按钮残留
- **金额/数量列方向颜色**（F2 §4）：F6 已在 col 层统一处理，本计划只确认只读实体复用 F6 col 范式
- **与 `2026-07-21-0330-1-f4p2-child-table-editor-p3-ext-domains.md` 的并行**：两计划都改 ext 域 view.xml，但分工——Plan 1 改头表单 cell + 行 grid，Plan 2 改 list 页 asideFilter/query/form。**3 实体 view.xml 文件存在重叠**（ErpLogShipmentLog / ErpB2bEdiLog / ErpApsDispatchLog）：Plan 1 在这些实体的父头表单内引用为 sub-grid-view；Plan 2 落地这些实体自身的 list 筛选 + 只读 dialog。**若并行执行，需顺序 commit 此 3 实体 view.xml**避免 git merge 冲突（不同 XML 段，同文件）。

## Goals

1. **Phase 0 Explore 闭环**：3 个未验证模式 PoC——(a) leadType Tab/Chip 切换 + 数据源刷新；(b) 「我的/团队/全部」快捷过滤（前端 vs 后端）；(c) logistics 异常筛选（前端 OR vs 后端 `@BizQuery`）
2. **22 个 ext 列表页双筛选面落地**：crm 3 + cs 2 + hr 4 + aps 3 + logistics 3 + b2b 3 + contract 2 + drp 2，每页 3-6 业务字段 + filterOp（按各域 ui-patterns.md 字段集冻结表）
3. **14 个只读实体（11 confirmed + 3 pending Explore）「搜索 → 行点击 → 详情 dialog」模式落地**：F1 CRUD 按钮移除复核 + edit/add form `x:abstract="true"` + asideFilter + query + dialog 详情
4. **范式文档扩展**：`docs/design/query-filter-patterns.md` 增 §5（ext 域字段集冻结表）+ §6（Tab/Chip 切换 + 快捷过滤 + 异常筛选模式）+ §7（只读实体清单）
5. **回归测试**：扩展 `tests/e2e/visual/` 抽样验证 ext 域列表页双筛选面渲染 + 只读实体 dialog

## Non-Goals

- **修改 ORM 模型 / xmeta / 后端 BizModel**（保护区域）——若 Explore 裁决需新增 `@BizQuery`（如 findExceptionList / findMyLeads），降级为前端实现或 flag 为后端 gap
- **F12 page 级 tabs/wizard**——属 `2026-07-21-0330-3-f12-page-structure-tabs-wizards.md`
- **F4 P3 子表行内编辑**——属 `2026-07-21-0330-1-f4p2-child-table-editor-p3-ext-domains.md`
- **F11 批量操作 / F13 看板 / F14 菜单 / F15 i18n**——独立 plan 范畴
- **F5 状态标签着色**——已全域覆盖（plan `2026-07-19-1818-3`），本计划仅复用既有 token
- **F6 字段格式化**——已全域覆盖（plan `2026-07-19-2200-2`），本计划仅复用既有 col 范式
- **F9 跨单据导航 ext 域补齐**——core 4 域已完成，ext 域按需逐域补齐归 Deferred
- **action-auth.xml / 菜单 / i18n**（F14/F15）
- **核心域已有的列表页双筛选面优化**——本计划仅覆盖 ext 8 域 + 长尾只读实体
- **ErpCrmLead 看板视图 / ErpCsTicket 看板视图 / Project 任务看板**——F13 范畴，非 F8 列表页筛选

## Task Route

- Type: `implementation-only change`（含 Explore 子阶段）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F8 + §F2
  - 各 ext 域 `docs/design/{crm,customer-service,human-resource,aps,logistics,b2b,contract,drp}/ui-patterns.md`（注：cs/hr 为简称，实际目录名 `customer-service`/`human-resource`）
  - `docs/design/query-filter-patterns.md`（双筛选面 + filterOp 范式）
  - `docs/design/status-color-map.md`（F5 状态着色，复用）
  - `docs/design/field-formatting-patterns.md`（F6 字段格式化，复用）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml asideFilter/query form + filterOp + Tab/Chip + dialog）；不加载 `nop-backend-dev`（不改 BizModel，特殊筛选优先前端实现）；不加载 `nop-testing`（既有 visual spec 归 Closure Gates）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- **Explore 阶段需可本地运行的 AMIS 页面**用于实测 Tab/Chip 切换 + 数据源刷新 + 快捷过滤
- 无新 config / 端口 / 密钥依赖

## Execution Plan

### Phase 0 — Explore：3 个未验证模式 PoC + 后端筛选裁决

Status: planned
Targets: plan 内 Explore 结论 + Decision 记录
Skill: `nop-frontend-dev`

- Item Types: `Explore | Decision`
- Prereqs: none

- [ ] `Explore` (a)：leadType Tab/Chip 切换 + 数据源刷新。crm Lead 列表页需支持「全部 / 线索 / 商机」3 态切换，切换时刷新 findPage 数据。
  - 验证方法：参考 AMIS `crud` 的 `filter` 配置 + `__findPage` 后端是否支持 `leadType` 作为 query 参数（已在 ORM 字段）
  - Skill: `nop-frontend-dev`
  - **降级方案**：若 AMIS Tab 模式与 Nop `asideFilter` 不兼容 → 改用 `asideFilter` 内 `leadType` 字段（radio-group）+ submitOnChange
- [ ] `Explore` (b):「我的/团队/全部」快捷过滤（crm Lead + cs Ticket）。
  - 验证方法：核实后端 `findPage` 是否支持按 `ownerId`/`assigneeId` 过滤 + 当前用户上下文获取方式（`SecurityContext.getUserId()`）
  - Skill: `nop-frontend-dev`
  - **降级方案**：若后端不支持当前用户上下文 → 前端经 AMIS 变量 `${user.id}` 注入 asideFilter.ownerId 字段
- [ ] `Explore` (c)：logistics Shipment 异常筛选「仅显示网关异常/追踪超期/退回」。
  - 验证方法：核实后端是否有 `findExceptionList @BizQuery` 或需新增；AMIS `asideFilter` 是否支持 OR/IN 过滤
  - Skill: `nop-frontend-dev`
  - **降级方案 A**：前端经 status 多选 + `filterOp=in`（若 Nop 支持）
  - **降级方案 B**：3 个快捷按钮独立跳转带预设 filter 的 URL（最简单但用户体验差）
  - **降级方案 C**：flag 为后端 gap，本计划仅落地常规筛选，异常筛选归 Deferred
- [ ] `Decision`：基于 Explore (a)/(b)/(c) 结果，确定 Tab 切换 / 快捷过滤 / 异常筛选的实现方式。在 plan 内记录选择、替代方案、残留风险。
  - Skill: none

Exit Criteria:

- [ ] 3 个 Explore 结论已记录；对应 Decision 已落地
- [ ] 22 个列表页 + 14 个只读实体（11 confirmed + 3 pending Explore）的字段集冻结表已确认（按各域 ui-patterns.md）

### Phase 1 — 22 个 ext 列表页双筛选面落地

Status: planned
Targets: 各 ext 域 `erp-{module}-web/.../pages/{Head}/{Head}.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（8/8 items tagged Add，按 Rule 7 统一 Add）
- Prereqs: Phase 0 Explore 完成

- [ ] `Add`：crm 3 列表页（Lead + Campaign + Forecast）
  - Lead：按 Explore (a)/(b) 裁决落地 leadType Tab + 快捷过滤 + 常规筛选（source/stage/owner/createdAt range）
  - Campaign：常规筛选（status + ownerId + startDate range）
  - Forecast：常规筛选（periodId + ownerId + status）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：cs 2 列表页（Ticket + Survey）
  - Ticket：常规筛选（ticketTypeId + slaPolicyId + status + priority + agentId + createdAt range）
  - Survey：常规筛选（ticketId + status + createdAt range）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：hr 4 列表页（Employee + Attendance + LeaveRequest + PayrollBankFile）
  - Employee：常规筛选（departmentId + positionId + status + entryDate range）
  - Attendance：常规筛选（employeeId + date range + shiftId）
  - LeaveRequest：常规筛选（employeeId + status + leaveType + date range）
  - PayrollBankFile：常规筛选（status + period + generateDate range）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：aps 3 列表页（Schedule + OperationOrder + DispatchLog）
  - Schedule：常规筛选（status + workCenterId + date range）
  - OperationOrder：常规筛选（scheduleId + machineId + status + plannedStart range）
  - DispatchLog（**只读**）：常规筛选（scheduleId + operationId + dispatchDate range）+ F2 只读模式落地
  - Skill: `nop-frontend-dev`
- [ ] `Add`：logistics 3 列表页（Shipment + ShipmentLog + Carrier）
  - Shipment：常规筛选（carrierId + status + partnerId + shipDate range）+ 按 Explore (c) 裁决异常筛选
  - ShipmentLog（**只读**）：常规筛选（shipmentId + status + logDate range）+ F2 只读模式落地
  - Carrier：常规筛选（carrierType + gatewayId + isActive）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：b2b 3 列表页（EdiDoc + Asn + EdiLog）
  - EdiDoc：常规筛选（direction + state + partnerProfileId + docType + sentAt range）
  - Asn：常规筛选（partnerProfileId + state + expectedReceiptDate range）
  - EdiLog（**只读**）：常规筛选（ediDocId + direction + processedAt range）+ F2 只读模式落地
  - Skill: `nop-frontend-dev`
- [ ] `Add`：contract 2 列表页（Contract + RebateAgreement）
  - Contract：常规筛选（partnerId + status + contractType + effectiveDate range）
  - RebateAgreement：常规筛选（partnerId + status + agreementType + effectiveDate range）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：drp 2 列表页（Plan + Parameter）
  - Plan：常规筛选（status + warehouseId + planDate range）
  - Parameter：常规筛选（warehouseId + materialId + parameterType）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 全 22 列表页 asideFilter + query 落地（每页 3-6 字段 + filterOp 按各域 ui-patterns.md）
- [ ] 特殊筛选（leadType Tab / 快捷过滤 / 异常筛选）按 Phase 0 Explore 裁决每个**显式判定 landed 或 deferred-with-trigger**（不得整组静默 defer）
- [ ] 本地运行时抽样 5 域通过：crm Lead（Tab 切换验证）+ cs Ticket（多维筛选）+ hr Employee（departmentId 筛选）+ logistics Shipment（date range + 异常筛选）+ b2b EdiDoc（direction 筛选）

### Phase 2 — 14 个只读实体 F2 模式落地（11 confirmed + 3 pending Explore）

Status: planned
Targets: 各 ext 域只读实体 `erp-{module}-web/.../pages/{Entity}/{Entity}.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 完成（部分只读实体在 Phase 1 已落地 list 筛选，本 Phase 落地 dialog 详情 + CRUD 按钮复核）

- [ ] `Fix`：F1 CRUD 按钮移除复核——抽样核实 14 个只读实体是否还有残留 add/edit/delete/view 按钮（F1 Phase 1 覆盖 10 实体但 ext 长尾可能遗漏）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：edit/add form `x:abstract="true"` 切断继承——确保只读实体不可通过 URL 直接打开编辑表单
  - Skill: `nop-frontend-dev`
- [ ] `Add`：dialog 详情落地——行点击展开 drawer/dialog 显示完整字段 + 关联实体摘要（如 ShipmentLog 详情含源 Shipment 摘要）
  - Skill: `nop-frontend-dev`
- [ ] `Fix`：金额/数量列方向颜色（F6 col 范式复用）——确认只读实体复用既有 `<col><gen-control>` 范式，无需独立改造
  - Skill: `nop-frontend-dev`
- [ ] `Add`：13 只读实体清单逐个落地（aps DispatchLog + logistics ShipmentLog/Parcel + b2b EdiLog + crm LeadConvLog/LeadScore×2/ForecastAccuracy/FunnelStageMetrics/LeadSequenceProgress + hr LeaveBalance/SurveyResult + contract ApprovalRecord/ConsumptionLine）
  - 11 confirmed 只读：完整 F2 模式（CRUD 按钮移除 + edit/add `x:abstract` + dialog 详情）
  - 3 pending Explore 半只读（Parcel/LeadScore/ConsumptionLine）：按 Explore 裁决保留 CRUD 或切断 edit/add form
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 全 14 只读实体（11 confirmed + 3 pending Explore 裁决后）落地「搜索 → 行点击 → 详情 dialog」模式
- [ ] 残留 CRUD 按钮 0（抽样 grep 验证 `add-button|edit-button|delete-button`）
- [ ] edit/add form `x:abstract="true"` 切断 100% 覆盖纯只读实体（11 confirmed）

### Phase 3 — 范式文档扩展 + 回归测试

Status: planned
Targets: `docs/design/query-filter-patterns.md` + `tests/e2e/visual/`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2 完成

- [ ] `Add`：范式文档扩展 `docs/design/query-filter-patterns.md`
  - §5 ext 域 22 列表页字段集冻结表（每域 1 子节，参考 §2 格式）
  - §6 特殊筛选模式（Tab/Chip 切换 + 快捷过滤 + 异常筛选 + 多选 IN + 布尔 includeX）
  - §7 ext 域只读实体清单（14 实体 = 11 confirmed + 3 pending Explore；含来源 + 是否纯只读）
  - Skill: none
- [ ] `Proof`：扩展 visual spec 抽样验证 ext 域列表页双筛选面 + 只读实体 dialog
  - 新增或扩展 `tests/e2e/visual/ext-domains-list-filter.visual.spec.ts`，抽样 5 域（crm Lead Tab 切换 + cs Ticket 多维筛选 + hr Employee departmentId 筛选 + logistics Shipment 日期范围 + b2b EdiDoc direction 筛选）+ 2-3 只读实体 dialog 详情（logistics ShipmentLog + b2b EdiLog + crm FunnelStageMetrics）
  - 断言：asideFilter 字段渲染 + filterOp 正确 + dialog 详情字段完整 + 无 CRUD 按钮
  - Skill: `nop-frontend-dev`
- [ ] `Proof`：本地运行时回归——既有 ext 域 E2E（`tests/e2e/crud/` + `tests/e2e/business-actions/`）无回归（筛选面落地不破坏既有 GraphQL 路径）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 范式文档 §5/§6/§7 已落地
- [ ] 新增/扩展 visual spec 通过（DOM 结构 + 字段 token 断言）
- [ ] 既有 ext 域 E2E 全绿

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_07ef781ddffe) — 0 blockers, 4 majors:
  1. Owner-doc paths wrong (`cs`/`hr` 简称) — actual dirs `customer-service`/`human-resource` → fixed in Owner Docs + added path note
  2. Count math inconsistent (21 vs 22 list pages, 13 vs 14 readonly entities) → corrected to 22 + 14 (11 confirmed + 3 pending Explore)
  3. Phase 1 Exit Criteria "抽样 4-5 域" unpinned → pinned to 5 specific domains
  4. Phase 1 Exit Criteria "落地或归 Deferred" too permissive → tightened to require per-pattern explicit landed or deferred-with-trigger adjudication
  Plus minors addressed: Phase 1 Item Types `Add | Fix` → `Add-heavy` (8/8 Add); added Plan 1↔Plan 2 file-overlap coordination note for ErpLogShipmentLog/ErpB2bEdiLog/ErpApsDispatchLog.
- Independent draft review iteration 2: needs revision (ses_07eee38d7ffe) — 0 blockers, 1 major:
  1. Count math regression in 3 stale locations: Phase 0 Exit Criteria "21+13" → corrected to "22+14（11 confirmed + 3 pending Explore）"；Phase 3 §5 "21 列表页" → "22 列表页"；Phase 3 §7 "13 实体" → "14 实体（11 confirmed + 3 pending Explore）"
  Plus minor: Phase 2 title "13 个只读实体" → "14 个只读实体（11 confirmed + 3 pending Explore）"
- Independent draft review iteration 3: accept (ses_07eea5bc9ffe) — focused verification pass: 7/7 checks pass (no "21 个" or "13 个" in scope / Phase 2 title 14 / Phase 0 Exit 22+14 / Phase 3 §5/§7 updated / Phase 2 line 224 / owner-doc paths correct). Plan ready for `Plan Status: active`.

## Closure Gates

- [ ] 范围内行为完成（3 Phase 全部 `[x]`）
- [ ] 相关文档对齐（query-filter-patterns.md §5/§6/§7 + 各 ext 域 ui-patterns.md 实施记录）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test` 抽样 visual + 既有 ext 域 E2E 全绿）
- [ ] 无范围内项目降级为 deferred/follow-up（特殊筛选经 Explore 裁决后的降级是合法 Decision）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### crm 看板视图 / cs 工单看板 / Project 任务看板（F13）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅覆盖列表页筛选；看板视图属 F13 范畴（独立 plan 待启动）
- Successor Required: `yes`（触发：F13 plan 启动时）

### logistics 异常筛选后端 `findExceptionList @BizQuery`（若 Explore (c) 裁决方案 C）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 前端 OR/IN 过滤或 3 快捷按钮已能覆盖基本场景；后端专用 `@BizQuery` 为性能优化（避免前端拉全量过滤）
- Successor Required: `yes`（触发：异常发运单数量 > 1000 时）

### 核心域已有的列表页优化（purchase/sales/inventory/finance 4 主域）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F8 核心 8 列表页已由 plan `2026-07-20-0629-2` 落地；本计划仅扩展 ext 域
- Successor Required: `no`

## Closure

Status Note: <pending>

Closure Audit Evidence:

- Auditor / Agent: <pending independent closure auditor>

Follow-up:

- F12 page 级 tabs/wizard 包装 ext 域只读实体详情（触发：`2026-07-21-0330-3-f12-page-structure-tabs-wizards.md` 启动时）
- F9 跨单据导航 ext 域补齐（触发：各域细化端到端验证时）
- F11 批量操作 ext 域（触发：批量审批/导入需求出现时）
- logistics `findExceptionList @BizQuery`（触发：异常发运单数量 > 1000 时）
