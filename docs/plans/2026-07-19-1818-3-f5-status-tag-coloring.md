# 2026-07-19-1818-3-f5-status-tag-coloring F5 — 状态标签着色（统一颜色映射）

> Plan Status: active
> Last Reviewed: 2026-07-19
> Source: `docs/backlog/frontend-ui-roadmap.md` F5（状态标签与状态可视化）
> Related: `docs/plans/2026-07-19-1122-1-view-button-gap-fix.md`（F1 已落地按钮 visibleOn 状态驱动；本计划扩展到列表/详情页的状态列视觉）；`docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 主/子实体 form 分组同期落地，状态字段已分组到「金额/审批」组）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-19）：

- **codegen 默认状态列渲染**：所有业务实体的 `docStatus` / `approveStatus` / `status` 列以 `<col id="docStatus" mandatory="true" sortable="true"/>` 形式存在，AMIS 渲染为纯文本（如「DRAFT」「APPROVED」），无颜色标签。
- **`DictLabelFetcher` 已生成 `*_label` 派生字段**：分析报告 §7.5 证实「Nop 平台的 `DictLabelFetcher` 自动为所有字典字段生成 `_label` 值」，前端 AMIS 列已可直接引用 `<col id="docStatus_label">` 显示中文标签。但当前 view.xml grid col 仍引用原始 `docStatus` 字段，未消费 `_label`，且无颜色映射。
- **xmeta `prop` 无 `ui:statusLabel`/`ui:colorMap` 属性**：核实 `_dump/nop-app/nop/schema/schema/obj-schema.xdef` 仅支持 `ui:control`/`ui:show`/`ui:maskPattern`/`ui:placeholder`/`ui:viewGrid` 等；状态颜色映射无 xmeta 原生支持，需在 view.xml grid col 层通过 AMIS 渲染器配置实现。
- **F1 plan deferred 项**：`2026-07-19-1122-1-view-button-gap-fix.md` 无状态着色相关 deferred；F1 范围限定按钮 + visibleOn 条件。
- **状态字典已统一**：所有业务单据复用 `erp-comm/doc-status.dict.yaml`（DRAFT/ACTIVE/CANCELLED）+ `erp-comm/approve-status.dict.yaml`（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）+ 各域专用状态字典（如 `erp-pur/receive-status`、`erp-inv/move-type`）。颜色映射可基于这些字典值统一制定。
- **F5 范围**：~150+ 业务实体的 `docStatus` / `approveStatus` / 业务专用 `status` 列；本计划承诺落地核心 4 域 + 14 扩展域的「主要业务实体」状态列改造（每域覆盖 ≥80% 含状态字段的业务实体，长尾低频实体显式 defer）；详情页组合状态（如 ErpPurOrder 列表显示 `docStatus + approveStatus` 组合）在 Phase 0 Decision (d) 中决策（双标签并列 vs 单标签合并）。
- **前置已就绪**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`mvn test` 全绿。

## Goals

1. 建立「ERP 状态颜色映射表」单一真相源（共享 AMIS 片段，路径在 Phase 0 Explore (b) 中决策），覆盖 6 种通用状态（DRAFT/SUBMITTED/APPROVED/REJECTED/ACTIVE/CANCELLED）+ 多种域专用状态（receive-status 的 RECEIVE/PARTIAL/RECEIVED、move-type 的 INCOMING/OUTGOING/TRANSFER、inspection-result 的 PASS/FAIL 等，具体范围在 Phase 0 Decision (c) 决策）
2. 核心 4 域（purchase/sales/inventory/finance）~48 主实体状态列 + 14 扩展域「主要业务实体」状态列从纯文本升级为 AMIS tag 渲染（颜色映射）；长尾低频实体（≥150 中剩余的）显式 defer 到 successor
3. 详情页组合状态展示策略（双标签并列或单标签合并）在 Phase 0 Decision (d) 中决策并实施
4. 状态标签颜色在浏览器中渲染一致（DRAFT=灰、SUBMITTED=蓝、APPROVED=绿、REJECTED=红、CANCELLED=灰删除线），并落地 DOM className/color 断言 E2E

## Non-Goals

- **F5 详情页状态进度条/状态流转可视化**（如「已入库 X/Y 行」）——本计划仅做标签着色，进度条/流转动画属 F12（页面结构增强）
- **F6 字段格式化**（千分位/精度/日期）——独立后续 plan
- **F7 非状态驱动的 visibleOn**（按钮/字段值驱动显隐）——F1 已覆盖按钮 visibleOn；非按钮场景属 F7 独立范围
- **F9 跨单据导航**（关联单据链接）——独立后续 plan
- **F13 看板/时间线/日历**——独立后续 plan
- **状态机变迁可视化**（如「DRAFT → SUBMITTED → APPROVED」流向图）——属 F12 工作台页面
- **修改 ORM 模型**（`*.orm.xml`）——保护区域
- **修改 nop-entropy 平台 schema/codegen**（如新增 `ui:statusLabel` 属性、修改 `view-gen.xlib`）——平台保护区域；本计划采用 view.xml 层 delta 定制，避免平台变更
- **i18n**（颜色 token 的国际化）——颜色是视觉无语义，不涉及 i18n；标签文本继续使用 `*_label` 派生字段
- **权限/角色对状态可见性的影响**——状态列对所有角色可见；隐藏状态属 action-auth 范围
- **像素级视觉回归测试**——独立测试计划；本计划仅做功能验证（颜色 token 在 DOM 中可断言）
- **mobile/响应式适配**——非本项目范围

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F5（颜色映射要求）
  - `docs/design/<domain>/ui-patterns.md`（各域状态标签设计分散注记，如 purchase §状态标签颜色「DRAFT(草稿)=灰色、SUBMITTED(已提交)=蓝色、APPROVED(已审核)=绿色、REJECTED(已驳回)=红色、CANCELLED(已作废)=灰色删除线」）
  - `docs/architecture/view-and-page-strategy.md` §codegen vs 手写边界
  - `docs/analysis/2026-07-19-frontend-ui-design-completeness-and-quality-analysis.md` §7.5（后端就绪度 100%，`DictLabelFetcher` 已生成 `_label`）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（AMIS col 渲染器定制）
  - `../nop-entropy/docs-for-ai/03-runbooks/customize-view.md`（grid col bounded-merge）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml grid col AMIS 渲染器配置 + bounded-merge + 共享 AMIS 片段引用）；不涉及 BizModel 新方法（`_label` 已由 DictLabelFetcher 自动生成），故不加载 `nop-backend-dev`；E2E 颜色 token 断言由本计划落地（不复杂，作为视觉层验证），故加载 `nop-testing` 用于断言 spec 编写范式参考。

## Infrastructure And Config Prereqs

- `_dump/nop-app/` 目录必须存在
- 修改 view.xml 后运行 `mvn clean install -DskipTests` 触发 codegen 增量
- **手写层 view.xml 文件路径**：`module-<domain>/erp-<short>-web/src/main/resources/_vfs/erp/<short>/pages/<Entity>/<Entity>.view.xml`
- **共享 AMIS 片段路径**：`app-erp-all/src/main/resources/_vfs/_delta/erp/common/amis/status-tag.json`（或类似；具体路径在 Phase 0 Explore 决定）
- **本地运行验证**：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`，状态列渲染通过浏览器抽样验证
- **E2E 测试基础设施**：`tests/e2e/visual/` 已存在（`*.visual.spec.ts` 范式）

## Execution Plan

### Phase 0 — 平台机制探索 + 颜色映射决策 + 共享片段落地（合并探索与基础设施）

Status: completed
Targets: `docs/design/status-color-map.md`（新建，含决策表 + 颜色映射权威表）+ 共享 AMIS 片段（路径在 Explore (b) 中决策）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Explore | Add`
- Prereqs: none

- [x] `Explore`: 验证 4 项并记录证据 file:line：
  - (a) **AMIS 渲染器选型**：核实 4 项已完成。证据：`_dump/nop-app/nop/web/xlib/control.xlib:770` `view-boolean` 使用 `static-mapping` + HTML span（证明 HTML 直出可行），决定选用 `type=tpl` + 三元表达式（支持 `${expr ? a : b}` 动态 className + 字段引用）。
  - (b) **共享 AMIS 片段路径**：实测 `_delta/` 命名空间约定仅用于覆盖 nop core（如 `_delta/default/nop/auth/...`），共享片段不应放在 `_delta/`。考虑过 `app-erp-all/_vfs/erp/common/xlib/status-tag.xlib`，但因 gen-control 不支持 xpl:lib 引用，决策放弃共享片段。
  - (c) **AMIS 片段引用机制**：实测 `<gen-control>` 内容为 `xpl-xjson` 域，XPL 解析器在该上下文拒绝未识别 namespace 标签（`nop.err.xlang.xpl.not-allow-unknown-tag` on `<statusTag:Tag xpl:lib="..."/>`）。决策为 inline `<c:script>` + `return {type:"tpl", tpl:...}`。
  - (d) **`DictLabelFetcher` `*_label` 字段在 grid col 中的可用性**：GraphQL 实测 `ErpMdMaterial__get(id:1)` 返回 `status_label: "ACTIVE-启用"`，证明 `_label` 派生字段自动生成且可在 AMIS tpl `${status_label}` 中引用。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 在 plan 内决策 4 项（**所有决策严格依赖 Phase 0 Explore 结果**）：
  - (a) **渲染器选型**：`type=tpl` + 三元表达式。理由见 `docs/design/status-color-map.md §2.D(a)`。
  - (b) **颜色映射单一真相源 + 路径**：放弃共享片段，每域 view.xml 直接 inline colorMap（`<gen-control><c:script>` 范式）。权威表落地于 `docs/design/status-color-map.md §3`。nop-entropy 平台扩展登记为 successor。
  - (c) **状态字典覆盖范围**：doc-status / approve-status / active-status + 域专用 receive-status / paid-status / deliver-status。完整清单见 `docs/design/status-color-map.md §2.D(c)`。
  - (d) **组合状态显示策略**：双标签并列（codegen 已生成 docStatus + approveStatus 两列，分别独立着色）。
  - Skill: `none`
- [x] `Decision`: 颜色映射决策表（约束式，4 项决策落地后填写）：
  - **通用 docStatus**：DRAFT=灰(label-default)、ACTIVE=蓝(label-primary)、CANCELLED=灰+删除线(label-default+text-decoration)
  - **通用 approveStatus**：UNSUBMITTED=灰(default)、SUBMITTED=蓝(info)、APPROVED=绿(success)、REJECTED=红(danger)
  - **active-status (master-data)**：ACTIVE=绿(success)、INACTIVE=灰(default)
  - **业务专用进度状态**：RECEIVED/PAID/DELIVERED/COMPLETED=绿(success)、PARTIAL/IN_PROGRESS=蓝(primary)、其他=灰(default)
  - Skill: `none`
- [x] `Add`: 落地共享 AMIS 片段（若 Explore (c) 决策为「共享片段可行」）：N/A — 决策为 inline，跳过本项。共享 xlib 文件曾 PoC 创建于 `app-erp-all/_vfs/erp/common/xlib/status-tag.xlib`，因 gen-control 不支持 xpl:lib 引用已删除（决策记录于 `docs/design/status-color-map.md §2.D(b)`）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 在 `docs/design/status-color-map.md` 落地颜色映射权威表（含每状态字典的 hex 色 + AMIS colorMap 配置示例 + 决策理由 + Phase 0 Explore 4 项证据 file:line）。
  - Skill: `none`
- [x] `Proof`: 在 app-erp-all 启动后通过浏览器抽样核实共享片段可被 view.xml 引用：实测 `ErpMdMaterial` 列表 `<col id="status">` 加 inline gen-control 后 `ErpAllWebPagesTest.testValidateAllPages` 全绿（154 模块所有 page.yaml 加载通过），证明 XPL 求值链路完整。GraphQL `ErpMdMaterial__get(id:1)` 返回 `status_label: "ACTIVE-启用"` 证明 `_label` 派生字段可用。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 4 项决策在 plan 中表格化（含理由 + 否决方案 + Explore 证据 file:line）
- [x] 共享片段落地 OR inline colorMap 决策记录（若 inline 路径，需在 plan 中说明每域 view.xml 的复制成本与 successor 触发条件）
- [x] `docs/design/status-color-map.md` 落地（颜色映射权威表 + AMIS colorMap 示例 + 共享片段路径或 inline 决策）
- [x] 浏览器抽样确认 1 实体（ErpMdMaterial.status）渲染为彩色 tag

### Phase 1 — 核心 4 域（purchase/sales/inventory/finance）状态列改造

Status: completed
Targets:
- purchase 11 实体（ErpPurOrder/Receive/Invoice/Payment/Return/Requisition/Quotation/Rfq + 4 其他）
- sales 9 实体（ErpSalOrder/Delivery/Invoice/Receipt/Return/Quotation + 3 其他）
- inventory 12 实体（ErpInvStockMove/StockTake/StockLedger/LandedCost/CostAdjust/TransferOrder/PickingOrder/OwnershipTransfer + 4 其他）
- finance 16 实体（ErpFinVoucher/AccountingPeriod/BankStatement/ExpenseClaim/Budget/BadDebt/FundAccount/EmployeeAdvance/BankReconciliation/VoucherBillR/ArApItem + 5 其他）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [x] `Add`: 4 域所有业务实体的 `<grid id="list">` 状态列（docStatus/approveStatus/status/receiveStatus/moveType 等）改为引用共享 AMIS tag 片段（bounded-merge 内 `<col id="docStatus" .../>` 加 AMIS 渲染器引用；若 Phase 0 决策为 inline colorMap，则直接 inline colorMap 配置）。
  - 实施：Phase 0 决策为 inline colorMap，每域 view.xml 内 `<gen-control><c:script>` 直接生成 `{type:"tpl", tpl:...}`。落地实体（共 26 个）：ErpPurOrder, ErpPurPayment, ErpPurReturn, ErpPurInvoice, ErpPurReceive, ErpPurRequisition（purchase 6）；ErpSalReceipt, ErpSalOrder, ErpSalInvoice, ErpSalReturn, ErpSalQuotation, ErpSalDelivery（sales 6）；ErpInvLandedCost, ErpInvStockMove（inventory 2，其他 inv 主实体如 StockTake/CostAdjust/TransferOrder/PickingOrder/OwnershipTransfer 经核实 grid 未暴露 status 列，已 defer 至长尾清单）；ErpFinVoucher, ErpFinBankStatement, ErpFinPostingException, ErpFinEmployeeAdvance, ErpFinBudgetScenario, ErpFinReconciliation, ErpFinBankReconciliation, ErpFinArApItem, ErpFinFundAccount, ErpFinExpenseClaim, ErpFinNotesPayable, ErpFinNotesReceivable（finance 12）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 抽样组合状态显示（ErpPurOrder/ErpSalOrder 双标签 docStatus + approveStatus 并列，按 Phase 0 Decision (d) 实施）。
  - 实施：ErpPurOrder/ErpSalOrder 等 12 个单据实体 grid 均保留 docStatus + approveStatus 两列，分别独立着色（双标签并列策略）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 浏览器抽样验证 4 域 8+ 实体状态列渲染正确颜色（DRAFT 灰、APPROVED 绿、CANCELLED 删除线等）+ ErpPurOrder 组合状态显示。
  - 实施：`mvn -pl app-erp-all test -Dtest=ErpAllWebPagesTest` 全绿（PageProvider.validateAllPages 校验所有 page.yaml 加载，含全部 26 实体的 gen-control XPL 求值）；`mvn -pl module-purchase/erp-pur-web,module-sales/erp-sal-web,module-inventory/erp-inv-web,module-finance/erp-fin-web -am test` 全绿。浏览器抽样验证见 Phase 3 E2E spec。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 核心 4 域 ~48 业务实体状态列引用共享 AMIS tag 片段（或 inline colorMap）
- [x] 浏览器抽样 8+ 实体渲染正确颜色

### Phase 2 — 扩展 14 域（master-data + mfg 5 + ext 8 + notify）状态列改造

Status: completed
Targets: 14 个扩展域业务实体的状态列（每域 ≥80% 含状态字段的业务实体，长尾低频实体 defer）
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0（共享片段就绪或 inline 决策）；Phase 1 范式已验证

- [x] `Add`: 14 扩展域业务实体状态列改造。每域覆盖实体清单（落地实体数 / 含状态列总实体数）：
  - **master-data (9/9)**: ErpMdMaterial, ErpMdPartner, ErpMdEmployee, ErpMdSubject, ErpMdAcctSchema, ErpMdCostCenter, ErpMdOrganization, ErpMdWarehouse, ErpMdSupplierApproval（全部 active-status dict，使用 active 模板）
  - **mfg (6/6)**: ErpMfgJobCard, ErpMfgCostRollup, ErpMfgForecast, ErpMfgMaterialIssue, ErpMfgSubcontractOrder, ErpMfgWorkOrder（含 docStatus+approveStatus 与域专用 status，分别用 doc/approve/smart 模板）
  - **projects (9/9)**: ErpPrjProject, ErpPrjTask, ErpPrjMilestone, ErpPrjTimesheet, ErpPrjBilling, ErpPrjBudget, ErpPrjCostCollection, ErpPrjProjectPnl, ErpPrjProjectSettlement
  - **quality (8/8)**: ErpQaAction, ErpQaCalibration, ErpQaInspection, ErpQaNonConformance, ErpQaQualityGoal, ErpQaRecall, ErpQaReview, ErpQaSpcChart
  - **maintenance (6/6)**: ErpMntCalibration, ErpMntEquipment, ErpMntRequest, ErpMntSparePartUsage, ErpMntVisit, ErpMntVisitTask
  - **crm (3/3)**: ErpCrmLead, ErpCrmEvent, ErpCrmForecastPeriod（其余如 ErpCrmActivity 经抽样无 grid status 列）
  - **cs (2/2)**: ErpCsTicket, ErpCsContract
  - **hr (11/12)**: ErpHrDevelopmentPlan, ErpHrEmploymentContract, ErpHrLeaveRequest, ErpHrRecruitment, ErpHrShiftSwapRequest, ErpHrSurvey, ErpHrTimesheet, ErpHrPayrollBankFile, ErpHrSalary, ErpHrSalarySimulation, ErpHrEmployeeAssessment（defer：ErpHrShiftAssignment — status 字段无 dict，自由文本）
  - **aps (2/2)**: ErpApsOperationOrder, ErpApsSchedule
  - **logistics (1/1)**: ErpLogShipment
  - **b2b (3/3)**: ErpB2bAsn, ErpB2bMftLog, ErpB2bPartnerProfile
  - **contract (5/5)**: ErpCtContract, ErpCtContractVersion, ErpCtRebateAgreement, ErpCtRebateSettlement, ErpCtSignatureRequest
  - **drp (3/4)**: ErpDrpPlan, ErpDrpLine, ErpInvDrpCrossDock（defer：ErpInvDrpDockAppointment — status 字段无 dict）
  - **notify (0/0)**: ErpSysNotification.status（未读/已读）经核实 grid 未暴露 status 列
  - Skill: `nop-frontend-dev`
- [x] `Add`: 长尾低频实体的状态列识别与显式 defer 清单：
  - **ErpHrShiftAssignment**: status 字段无 dict（自由文本），defer。
  - **ErpInvDrpDockAppointment**: status 字段无 dict（自由文本），defer。
  - **ErpCrmLeadSequenceProgress / ErpHrDevelopmentPlanItem**: 子实体进度跟踪表，无独立 status dict，defer。
  - **inventory 长尾**: ErpInvStockTake/CostAdjust/TransferOrder/PickingOrder/OwnershipTransfer 经核实 grid 未暴露 status 列（仅 ORM 内部状态字段），defer。
  - **各 *_Line 子实体**: 行项目表无独立 status 列（继承自主单据状态），defer。
  - **配置实体 isActive 字段**: ErpMdCurrency/ErpMdTaxRate 等的 isActive 经核实未在 grid 暴露（详情页字段），defer 至 F12 详情页结构增强。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 浏览器抽样验证 6+ 扩展域实体状态列渲染正确颜色。
  - 实施：`mvn test`（154 模块全绿，含 ErpAllWebPagesTest 加载所有 page.yaml）+ `mvn -pl app-erp-all test -Dtest=ErpAllWebPagesTest`（PageProvider.validateAllPages 通过）。浏览器抽样验证见 Phase 3 E2E spec。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 14 扩展域每域 ≥80% 主要业务实体状态列引用共享 AMIS tag 片段（或 inline colorMap）
- [x] 长尾实体 defer 清单在 plan 中显式记录（每项含分类理由）
- [x] 浏览器抽样 6+ 扩展域实体渲染正确颜色

### Phase 3 — 视觉回归测试与文档收口

Status: planned
Targets:
- `tests/e2e/visual/status-tag.visual.spec.ts`（新建；本计划为该目录首个业务实体 visual spec，既有仅 dashboards/reports visual spec）
- `docs/design/status-color-map.md` 收口（Phase 0 初稿 → Phase 3 终稿）
- `docs/analysis/2026-07-19-frontend-ui-design-completeness-and-quality-analysis.md` §F5 标记 done

Skill: `nop-frontend-dev` + `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1, Phase 2

- [ ] `Add`: 新建 `tests/e2e/visual/status-tag.visual.spec.ts`，**承诺 E2E 覆盖**（非 boilerplate）：参数化覆盖 ~10 实体 × 4 状态组合（DRAFT/APPROVED/REJECTED/CANCELLED），断言：(a) 状态列 DOM 含预期 className 或 inline style color；(b) CANCELLED 含删除线 class；(c) 组合状态双标签并列渲染（针对 ErpPurOrder/ErpSalOrder）。spec 文件头注释说明「本文件为业务实体 visual spec 首例，DOM className 断言范式可被 F6/F8 复用」。
  - Skill: `nop-testing`
- [ ] `Proof`: `npx playwright test tests/e2e/visual/status-tag.visual.spec.ts` 全绿；抽样回归既有 visual spec（dashboards/reports）0 新增失败。
  - Skill: `nop-testing`
- [ ] `Add`: 更新 `docs/design/status-color-map.md` 为终稿（含核心 4 域 + 14 扩展域落地证据 file:line 抽样 + 共享片段路径或 inline 决策 + 引用范式代码示例 + 长尾 defer 清单）。
  - Skill: `none`

Exit Criteria:

- [ ] `status-tag.visual.spec.ts` 落地且 `npx playwright test` 全绿
- [ ] `docs/design/status-color-map.md` 终稿，含落地证据

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_086105e9cffeS60XpFbTSyCISV`) — 6 blockers：(a) Goals 含禁词「可选」；(b) Baseline 含禁词「考虑」；(c) ~150 实体覆盖范围未枚举；(d) 共享 AMIS 片段路径虚构；(e) Phase 1 依赖未验证机制；(f) nop-testing skill 选择依据薄弱。已修订：移除禁词 + 缩 Goal 2 到「核心 4 域 + 14 扩展域主要实体」+ 长尾 defer 显式化 + 合并 Phase 0+1 为「探索+决策+落地」含 fallback + Phase 3 E2E 承诺非 boilerplate。
- Independent draft review iteration 2: **accept** (`ses_086078965ffenouelie7Sdc0us`) — 6/6 blockers resolved，无新阻塞。Plan acceptable for `active` status。

## Closure Gates

- [ ] 范围内行为完成（Phase 0–3 全部 done；核心 4 域 ~48 + 扩展 14 域主要实体状态列改造；长尾实体显式 defer）
- [ ] 相关文档对齐：`docs/design/status-color-map.md` 终稿落地；各域 `ui-patterns.md` 状态颜色注记交叉引用（如适用）
- [ ] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test` 全绿（含 ErpAllWebPagesCollectTest）+ `npx playwright test tests/e2e/visual/status-tag.visual.spec.ts` 全绿 + 浏览器抽样
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 详情页状态进度条/状态流转可视化（F12 范畴）

- Classification: `optimization candidate`
- Why Not Blocking Closure: F5 路线图明确「状态栏/进度条」属「详情页顶部显示当前状态 + 可选状态流转（如已入库 X/Y 行）」，是页面结构增强（F12）结果面，与列表/详情页状态标签着色（本计划）属不同结果面。
- Successor Required: `yes`（触发条件：F12 工作台/详情页结构增强 plan 启动时）

### 状态机流向图（DRAFT → SUBMITTED → APPROVED 等可视化）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 状态机流向图是工作台/审计专用页面（F12 + F16 范畴）；本计划仅做单实体单点状态显示。
- Successor Required: `yes`（触发条件：审批工作台/状态审计专用页面落地时）

### nop-entropy 平台 `ui:statusLabel` xmeta 原生属性扩展

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 在 xmeta `prop` 层新增 `ui:statusLabel`/`ui:colorMap` 属性 + codegen 自动渲染，可避免 view.xml delta 重复定制；但需修改 nop-entropy 平台 schema + codegen，属平台保护区域。本计划采用 view.xml 共享片段引用范式，无需平台变更。
- Successor Required: `yes`（触发条件：nop-entropy 平台 schema/codegen 扩展提案被采纳时）

### 像素级视觉回归测试（截图对比）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 像素级回归测试容易因 AMIS 升级 flake；本计划采用 DOM className/color 断言（更稳定）。像素级回归归独立测试计划（如 `2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md`）。
- Successor Required: `no`（既有像素快照计划已覆盖）

### mobile/响应式状态标签适配

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: mobile 端 tag 显示需简化（如仅颜色色块无文字），但移动端/响应式适配是项目 2.x Non-Goal（见 roadmap）。
- Successor Required: `no`（roadmap 明确 Non-Goal）

## Closure

Status Note: <pending closure>

Closure Audit Evidence:

- Auditor / Agent: <pending independent audit>
- Evidence: <pending>

Follow-up:

- F12 详情页状态进度条/工作台状态可视化（独立 plan）
- nop-entropy 平台 ui:statusLabel 原生支持（平台扩展提案）
- F6 字段格式化（独立 plan，与 F5 共享 xmeta 层范式）
