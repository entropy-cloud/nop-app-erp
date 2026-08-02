# MA4 finance+mfg view.xml vs 后端契约 drift 审计（A4.6）

> Audit Status: closed
> 里程碑: MA4（代码与前端质量层）
> 工作项: A4.6（finance+mfg view.xml vs 后端契约 drift，view drift 第一批 S 级）
> 范围文档: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「view.xml drift（MA4）」行 + §残留风险 5「未覆盖：AMIS view.xml drift」
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（7 维度适配「view.xml vs 后端契约 drift」主题）
> 来源计划: `docs/plans/2026-07-29-0430-3-audit-remediation-ma4-finance-mfg-view-xml-drift.md`
> 后端契约真相源基线: A4.1a/b（finance）+ A4.2a/b（mfg）已 done——后端 BizModel/xbiz 契约稳定

## 1. 审计对象与基线

- **审计对象**：finance 72 view.xml + manufacturing 62 view.xml = **合计 134 view.xml**（`module-{finance,manufacturing}/erp-*-web/src/main/resources/_vfs/erp/{fin,mfg}/pages/`）。其中 delta（非 `_gen`）定制层 finance 36 + mfg 31 = 67 套，`_gen` 生成层各对应一套。delta 层是手写 drift 的发源地；`_gen` 层由 XMeta 驱动生成，理论自洽，本审计以 delta 层为主、`_gen` 层为辅交叉对照。
- **后端真相源**：`module-{finance,manufacturing}/erp-*-service/`（BizModel Java + `_*.xbiz`/`*.xbiz`）+ `*-meta/`（XMeta）+ `module-{finance,manufacturing}/model/app-erp-*.orm.xml`（字段 + `ext:dict` 绑定）+ `*/_vfs/dict/{erp-fin,erp-mfg}/*.dict.yaml`（枚举真相）+ `Erp{Fin,Mfg}Constants.java`（状态常量）。
- **drift 维度**（`multi-dimensional-audit-prompt.md` 7 维度适配本主题）：(1) 字段名一致性 / (2) BizMutation 动作名一致性 / (3) 枚举值状态值一致性 / (4) 参数类型一致性 / (5) dict 绑定一致性 / (6) gen-control 内联脚本契约 / (7) 跨实体字段引用。

## 2. 7 维度逐项审查结果

### 维度 1 — 字段名一致性

**裁决：本维度无 drift（delta 层经 `x:override="bounded-merge"` 自愈，`replace` 布局字段集核验通过）。**

- delta 层 grid `<cols>` / form `<cells>` 普遍声明 `x:override="bounded-merge"`。Nop `bounded-merge` 语义：仅合并基础层（XMeta）已声明的 prop，未知 `id` 在运行时被静默丢弃而非报错——字段名拼写漂移被平台机制自愈（dropped，不致页面报错/空白，最坏退化为列缺失可见性）。
- 抽样核验 `ErpFinVoucherLine`（403 行，最复杂子表，17 列）delta col id 集合（`acctSchema/amountFunctional/amountSource/costCenterId/creditAmount/currencyId/dcDirection/debitAmount/departmentId/exchangeRate/id/lineNo/materialId/memo/orgId/partnerId/projectId/subjectCode/subjectId/subjectName/voucher/warehouseId`）全部命中 ORM `ErpFinVoucherLine` 实体字段或其 to-one 关联（`voucher`/`subjectCode`/`subjectName`）。
- 抽样核验 `ErpMfgWorkOrder` delta col/cell id（`code/orgId/productId/currencyId/plannedQuantity/completedQuantity/scrappedQuantity/businessDate/docStatus/approveStatus/priority/bomId/routingId/productionVersionId/plannedStartDate/plannedEndDate/actualStartDate/actualEndDate/sourceOrderType/sourceOrderCode/sourceMrpPlanId/sourceScheduleId/materialCost/laborCost/overheadCost/subcontractCost/totalCost/unitCost/amountSource/amountFunctional`）全部命中 ORM 字段。
- MA1 ORM 审计 P1-MA1-001/008/010 propId 缺失字段重编号属 codegen 元数据层缺陷，**不影响 view.xml 字段引用**（propId 是 ORM 内部排序号，view 按 `name` 引用而非 propId）。已确认无投影。

### 维度 2 — BizMutation/BizQuery 动作名一致性

**裁决：本维度无 drift（全部自定义动作引用解析到 BizModel @BizMutation/@BizQuery 或 `_*.xbiz`/`*.xbiz` 声明）。**

逐一核验 delta 层全部自定义动作引用（CRUD 标准 `save/update/delete/batchDelete/findPage/get` 由 `_*.xbiz` `DefaultBizGenExtends` 自动派生，不逐项列出）：

**finance 自定义动作**（全部解析通过）：
- `ErpFinVoucher__postVoucher` / `reverseVoucher` / `previewReverseVoucher` → `ErpFinVoucherBizModel:88/104/122`（@BizMutation + @BizQuery）✓
- `ErpFinGlMappingRule__refreshCache` → `ErpFinGlMappingRuleBizModel:87` ✓
- `ErpFinReconciliation__reverse` / `runAutoReconciliation` / `previewReverse` → `ErpFinReconciliationBizModel` ✓
- `ErpFinAccountingPeriod__closePeriod` / `reverseClose` → `ErpFinAccountingPeriodBizModel` ✓
- `ErpFinBudgetScenario__submit` / `approve` / `reject` / `cancel` / `carryForward` / `rollForward` / `findList` / `get` → `ErpFinBudgetScenarioBizModel:33/39/45/51/66/57` ✓
- `ErpFinEmployeeAdvance__` + `ErpFinExpenseClaim__` 审批五动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）→ BizModel + Processor ✓
- `ErpFinVoucherTemplate__renderTemplate` → `ErpFinVoucherTemplateBizModel` ✓

**manufacturing 自定义动作**（全部解析通过）：
- `ErpMfgWorkOrder__` 审批五动作 → `ErpMfgWorkOrder.xbiz` 委托 `ErpMfgWorkOrder{SubmitForApproval,Approve,Reject,ReverseApprove}Processor` + inline `withdrawApproval`（xbiz 内联脚本）✓
- `ErpMfgWorkOrder__start` / `close` / `cancel` / `reportCompletion` → `ErpMfgWorkOrderBizModel:51/69/75/81`（@BizMutation）✓
- `ErpMfgSubcontractOrder__` 审批五动作 + `issueMaterials` / `receiveFinished` / `postProcessingFee` / `cancel` → `ErpMfgSubcontractOrderBizModel:37/42/50/60` + Processor ✓
- `ErpMfgMrpScenario__runSimulation` / `promoteToFormalPlan` → `ErpMfgMrpScenarioBizModel:52/59`（@BizMutation）✓（`promoteToFormalPlan` 经 `ErpMfgMrpScenarioVersion` delta view 调用，目标 BizObj 正确指向 Scenario 而非 Version——场景级 promote 语义正确）✓

- **A4.1a-A4.5 已登记 P1 复核**：本维度与 P1-MA3-048（孤儿 Processor bean 携带 String 影子契约 dim3）交叉——本审计确认全部 Processor bean 经 xbiz `<source>` 正式接线，无孤儿悬挂动作引用。

### 维度 3 — 枚举值/状态值一致性

**裁决：本维度发现 1 项 P1 drift（ErpMfgWorkOrder Close 按钮引用不存在的 `STARTED` 状态值）。**

逐实体核验 delta view 的 `visibleOn`/`disabledOn`/gen-control 状态字面量 vs dict yaml + `Erp{Fin,Mfg}Constants`：

| 实体 | view 引用状态值 | dict 真值 | 裁决 |
|------|----------------|----------|------|
| ErpFinAccountingPeriod.status | OPEN/CLOSING/CLOSED/CLOSED_FINAL | period-status: OPEN/CLOSING/CLOSED/NEVER_OPENED/CLOSED_FINAL | ✓（NEVER_OPENED 未在 view 暴露按钮，对应 P1-MA2-033 后端迁移缺失，非 view drift） |
| ErpFinVoucher.docStatus | DRAFT/POSTED | voucher-status: DRAFT/POSTED/CANCELLED | ✓（visibleOn 层）；CANCELLED 经 gen-control line-through 处理 ✓。**gen-control badge 引用 `ACTIVE` 见维度 6（P2）** |
| ErpFinReconciliation.docStatus | POSTED | reconciliation-status: DRAFT/POSTED/REVERSED | ✓ |
| ErpFinBudgetScenario.docStatus | （经审批轴 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED + budget-status DRAFT/SUBMITTED/...） | budget-status + wf/approve-status | ✓ |
| ErpFinEmployeeAdvance / ErpFinExpenseClaim.approveStatus | UNSUBMITTED/SUBMITTED/REJECTED/APPROVED | wf/approve-status: 四态全中 | ✓ |
| ErpFinConsolidationElimination.status | CANDIDATE | elimination-status: CANDIDATE/DRAFT_VOUCHER/POSTED | ✓ |
| ErpMfgMrpScenario / Version.status | DRAFT/COMPLETED | simulation-status: DRAFT/RUNNING/COMPLETED/ARCHIVED | ✓ |
| ErpMfgSubcontractOrder.docStatus | DRAFT/SUBMITTED/APPROVED/ISSUED/RECEIVED | subcontract-status: DRAFT/SUBMITTED/APPROVED/ISSUED/RECEIVED/COMPLETED/CANCELLED/REJECTED | ✓（Processor `issueMaterials`→ISSUED:168 / `receiveFinished`→RECEIVED:202 与 view guard 一致） |
| ErpMfgWorkOrder.docStatus | NOT_STARTED/STOCK_RESERVED/STOCK_PARTIAL/IN_PROCESS/COMPLETED/CANCELLED/CLOSED + **`STARTED`** | work-order-status: DRAFT/SUBMITTED/NOT_STARTED/IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/COMPLETED/STOPPED/CLOSED/CANCELLED（**无 STARTED**） | **✗ 见 P1-MA4-023** |

**MA2 状态机 dict 死状态 view 层投影复核**（plan 要求）：
- **P1-MA2-035**（作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态）：`ErpMfgJobCard` delta view gen-control badge 调色板含此二值（与 dict 一致），但死状态属后端无 writer（MA2 已登记），**view 层与 dict 一致，无 view 层 drift**。
- **P1-MA2-036**（MRP CANCELLED + 预测 CONSUMED dict 死状态）：mrp-status/forecast-status dict 含此二值，view badge 调色板一致；死状态属后端，**无 view 层 drift**。
- **P1-MA2-031/032**（凭证 DRAFT→CANCELLED / IGNORED 悬挂）：voucher delta view 无 cancel 动作按钮（仅 post/reverse），DRAFT→CANCELLED 死状态不经 view 暴露；voucher-status dict 无 IGNORED 值。**无 view 层投影**。
- **P1-MA2-037**（mrp.md RELEASED vs isFirmed）：文档层 drift，view 无 RELEASED 引用。**无 view 层投影**。
- **P1-MA2-038**（委外 APPROVED 豁免）：config-gated，view APPROVED 状态显示正确。**无 view 层 drift**。

### 维度 4 — 参数类型一致性

**裁决：本维度无 drift（全部 `@mutation:...?param=$value` 绑定与 BizModel `@Name` 签名一致）。**

- 审批五动作（WorkOrder/SubcontractOrder/EmployeeAdvance/ExpenseClaim/BudgetScenario）：xbiz 声明 `<arg name="id" type="String" mandatory="true"/>`，view 传 `?id=$id`，Processor `requireEntity({id})` 接受 String→Long 转换。Nop 标准模式，跨域一致（与 P2-MA3-037 cancel Long↔String adapt 跨域不一致属后端 xbiz 层，view 侧统一传 `$id`，无 view 层投影）✓。
- `ErpMfgWorkOrder__start/close/cancel/reportCompletion?workOrderId=$id` → BizModel `@Name("workOrderId") Long workOrderId`（`reportCompletion` 额外 `completedQty=0` → `@Name("completedQty")`）✓。
- `ErpMfgMrpScenario__promoteToFormalPlan?scenarioVersionId=$id` → `promoteToFormalPlan(@Name("scenarioVersionId") Long)` ✓（Version view 行级 `$id` 即 scenarioVersionId，语义正确）。
- `ErpFinBudgetScenario__rollForward/carryForward` 经 `<simple ... withFormData="true">` 表单数据合并 `id` + 表单字段（newFiscalYear/strategy / targetScenarioId/rule），与 BizModel 多参签名一致 ✓。
- **日期参数**：frontend-ui-roadmap 已修复 12 日期参数报表下载 + input-date valueFormat。本审计复核 finance/mfg 业务页面（`businessDate` filterOp=`date-between` 在 WorkOrder/ExpenseClaim/Reconciliation 等）——DatePicker 经 AMIS 标准序列化，无残留裸字符串日期漂移 ✓。

### 维度 5 — dict 绑定一致性

**裁决：本维度无 drift（ORM `ext:dict` 引用的 dict yaml 全部存在）。**

- 全量核验 finance 20 + mfg 11 状态/枚举类 dict 绑定（`erp-fin/{voucher,period,ar-ap,reconciliation,fund-account,expense-claim,advance,notes-receivable,notes-payable,posting-exception,budget,intercompany-match,elimination}-status` + `erp-mfg/{work-order,subcontract,issue,job-card,mrp,forecast,cost-rollup,simulation}-status` + `wf/approve-status`）→ 对应 `_vfs/dict/{erp-fin,erp-mfg,wf}/*.dict.yaml` **全部存在** ✓。
- **P1-MA2-046**（hr 排班分配 status 无 dict 绑定 raw VARCHAR）同型复核：finance/mfg 全部 status/docStatus 字段在 ORM 均声明 `ext:dict`（见维度 3 表），**finance/mfg 无同型 dict 绑定缺失**。

### 维度 6 — gen-control 内联脚本契约

**裁决：本维度发现 2 项 P2（`ACTIVE` 系统性死状态 badge 映射 + 跨域通用调色板漂移）。**

- **P2-MA4-014 系统性 `ACTIVE` 死状态 badge 映射**：11 套 delta view 的 docStatus/status gen-control badge 采用模板 `${valueProp == 'ACTIVE' ? 'primary' : 'default'}`，但 **finance/mfg 全部状态 dict 无 `ACTIVE` 值**（voucher-status DRAFT/POSTED/CANCELLED；work-order-status 10 态无 ACTIVE；等）→ `== 'ACTIVE'` 永不命中 → 状态颜色恒渲染为 `default`（灰）而非 `primary`（蓝）。影响：纯视觉（label 经 dict `graphql:labelProp` 正确显示，仅颜色类错误）。涉及：`ErpFinVoucher` / `ErpFinBudgetScenario` / `ErpFinReconciliation` / `ErpFinBankReconciliation` / `ErpFinBankStatement` / `ErpFinArApItem` / `ErpFinExpenseClaim` / `ErpFinFundAccount` / `ErpMfgWorkOrder` / `ErpMfgSubcontractOrder` / `ErpMfgMaterialIssue`。
- **P2-MA4-015 跨域通用状态调色板漂移**：`ErpMfgJobCard` / `ErpMfgForecast`（及同型 `_gen` badge）gen-control 内联硬编码跨域状态值数组（`successVals/dangerVals/primaryVals` 含 DELIVERED/PAID/SETTLED/HONORED/DISCOUNTED/ENDORSED 等非本域值）。本域有效状态多数命中，但 `ON_HOLD`（job-card-status 有效值）/ `CONSUMED`（forecast-status 有效值）未入任何数组 → 渲染灰。可维护性隐患（dict 演进时调色板失同步）。
- **前端 UI-roadmap Phase 3 残留复核**：notify-inbox 裸变量 `data` / AMIS `ErpMdPartner` 非法 GraphQL——本审计复核 finance/mfg delta view，唯一 `data` 引用位于 `ErpFinVoucherTemplate` adaptor `'const gql = (payload.data||{}).data ...'`（正确解构响应对象，非裸变量 bug）；finance/mfg delta view **零 `ErpMdPartner__` 非法 GraphQL 引用**（唯一 `ErpMdWarehouse__findList` 在 SubcontractOrder delta view 为合法 picker 源）。**无 Phase 3 残留**。

### 维度 7 — 跨实体字段引用

**裁决：本维度无 drift（跨实体字段路径全部命中关联实体字段或经 picker 快照注入）。**

- **`ErpFinVoucherLine` 辅助维度 visibleOn**（`isAuxiliaryPartner/Department/Project/Warehouse/Product/CostCenter`）：此六字段非 VoucherLine 自身字段，而是经科目 picker（`ErpMdSubject`）`selectedItem` 快照注入 input-table row scope（view 注释 §57/§60 明示此设计 + 降级路径 `!subjectId || isAuxiliaryX == true`）。核验 `ErpMdSubject` ORM（master-data `app-erp-master-data.orm.xml:916-921`）六字段全部存在（BOOLEAN）。跨实体引用经 picker 快照合法，无悬挂 ✓。
- **跨实体 picker view 路径**：delta view 引用的子表 view 路径（`/erp/fin/pages/ErpFinVoucherLine/...`、`/erp/fin/pages/ErpFinVoucherTemplateLine/...`、`/erp/mfg/pages/ErpMfgMrpScenarioParam/...`、`/erp/mfg/pages/ErpMfgMrpScenarioVersion/...`、`/erp/mfg/pages/ErpMfgWorkOrderLine/...`）**全部存在** ✓。
- refEntityName 重命名/关系变更后字段路径悬挂：本批次无 ORM refEntityName 重命名历史（MA1 ORM 审计未报告 finance/mfg refEntityName drift），无悬挂 ✓。

## 3. P0-P3 finding 清单（按严重性排序）

> 起始编号 = A4.5 已分配最大 P1-MA4-N（022）+ 1 = **P1-MA4-023**。本审计零 P0（无活跃数据破坏路径——view.xml drift 最坏为按钮不可见/颜色错误，无 GL/库存写入破坏）。

| Finding ID | 严重性 | 域 | view.xml 文件:行 | 后端对照 | 缺陷描述 | 影响 | 目标 MR |
|-----------|--------|----|-----------------|---------|---------|------|---------|
| **P1-MA4-023** | **P1 (major)** | mfg | `module-manufacturing/erp-mfg-web/.../ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml:234`（`row-close-button` `<visibleOn>`） | dict `erp-mfg/work-order-status`（DRAFT/SUBMITTED/NOT_STARTED/IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/COMPLETED/STOPPED/CLOSED/CANCELLED，**无 STARTED**）+ `ErpMfgConstants`（无 STARTED 常量）+ `ErpMfgWorkOrderProcessor:136/128`（`start()`→IN_PROCESS，`stop()`→STOPPED） | 「结案」按钮 `visibleOn="${docStatus == 'STARTED' \|\| docStatus == 'COMPLETED'}"` 引用**不存在的 `STARTED` 状态值**（死枚举引用）。`STARTED` 全仓仅在此 view 出现 2 处，dict/code/constants 零定义 | IN_PROCESS / STOPPED 工单的「结案」按钮被隐藏——用户无法从 UI 强制结案在制/停工工单（仅 COMPLETED 工单可结案）。功能性按钮可见性 bug | **MR2**（view.xml 代码类） |
| P2-MA4-014 | P2 (minor) | fin+mfg | 11 套 delta view gen-control badge（`ErpFinVoucher:34`/`ErpFinBudgetScenario`/`ErpFinReconciliation`/`ErpFinBankReconciliation`/`ErpFinBankStatement`/`ErpFinArApItem`/`ErpFinExpenseClaim`/`ErpFinFundAccount`/`ErpMfgWorkOrder:24`/`ErpMfgSubcontractOrder`/`ErpMfgMaterialIssue`） | finance/mfg 全部状态 dict 无 `ACTIVE` 值 | 状态 badge 模板 `${x == 'ACTIVE' ? 'primary' : 'default'}` 的 `ACTIVE` 不匹配任何本域状态 → 状态颜色恒为灰（default），永不蓝（primary） | 纯视觉（label 正确）；状态活跃度颜色区分失效 | MR2（view.xml 代码类，watch-only） |
| P2-MA4-015 | P2 (minor) | mfg | `ErpMfgJobCard:21-24` / `ErpMfgForecast:19-22` gen-control badge 调色板 | job-card-status（`ON_HOLD` 未入调色板）/ forecast-status（`CONSUMED` 未入调色板） | 跨域通用状态值硬编码数组含非本域值 + 漏入 `ON_HOLD`/`CONSUMED` 等本域有效值 → 漏入值渲染灰；dict 演进时调色板失同步 | 视觉 + 可维护性隐患 | MR2（view.xml 代码类，watch-only） |

## 4. 已知 finding view 层投影复核汇总

| 来源 finding | view 层投影裁决 |
|-------------|----------------|
| P1-MA2-031（凭证 DRAFT→CANCELLED 死状态） | 无 view 层投影（voucher delta view 无 cancel 按钮，死状态不经 view 暴露） |
| P1-MA2-032（IGNORED 凭证悬挂） | 无 view 层投影（voucher-status dict 无 IGNORED） |
| P1-MA2-035（作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 死状态） | 无 view 层 drift（view badge 与 dict 一致，死状态属后端无 writer） |
| P1-MA2-036（MRP CANCELLED + 预测 CONSUMED 死状态） | 无 view 层 drift（view badge 与 dict 一致，死状态属后端） |
| P1-MA2-037（mrp.md RELEASED vs isFirmed） | 无 view 层投影（文档层 drift） |
| P1-MA2-038（委外 APPROVED 豁免） | 无 view 层 drift（config-gated，view APPROVED 显示正确） |
| P1-MA3-047（API 命名/参数跨域不一致 dim7） | 无 view 层 drift（view 侧统一 `$id` 传参，跨域不一致属后端 xbiz） |
| P1-MA3-048（孤儿 Processor bean String 影子契约 dim3） | 无 view 层 drift（全部 Processor 经 xbiz 正式接线，本审计确认无孤儿悬挂） |
| 前端 UI-roadmap Phase 3（notify-inbox 裸 data / ErpMdPartner 非法 GraphQL） | 无 finance/mfg 残留（VoucherTemplate adaptor 正确解构；零 ErpMdPartner 非法引用） |

## 5. Verdict

**FAIL（有 drift）**——零 P0（无活跃数据破坏路径；view.xml drift 最坏为按钮不可见/颜色错误）。**1 项 P1**（P1-MA4-023 WorkOrder Close 按钮死枚举引用，功能性按钮可见性 bug）+ **2 项 P2** watch-only（P2-MA4-014/015 gen-control badge 视觉/可维护性）。MA2/MA3/前端-roadmap 已知 finding view 层投影复核全部「无 view 层 drift」或「无投影」。

**drift 密度评估**：finance 72 + mfg 62 = 134 view.xml，自定义动作引用全量解析零 drift（动作名一致性维度 PASS），字段名/dict 绑定/参数类型/跨实体引用四维度全 PASS，drift 集中在枚举状态值（维度 3，1 项 P1）与 gen-control 内联脚本（维度 6，2 项 P2）。drift 密度 1 P1 / 134 view.xml ≈ 0.75%，属低密度——delta 层 `bounded-merge` 自愈机制 + xbiz/Processor 正式接线 + ORM `ext:dict` 绑定三道防线有效抑制了字段/动作/dict 三类高频 drift。

## 6. 剩余风险

- **A4.7（pur+sal+inv）+ A4.8（crm+hr）未覆盖**：本审计仅 finance+mfg（S 级第一批）。pur/sal/inv（A 级）+ crm（A 级）/hr（S 级）view.xml drift 归 A4.7/A4.8。hr view.xml 数 72（与 finance 持平）且 P1-MA2-039~048 hr 死状态密集，A4.8 需重点复核 hr 排班/工时单/银行文件 dict 死状态 view 层投影。
- **`_gen` 层未逐文件深审**：本审计以 delta 层为主。`_gen` 层由 XMeta 驱动生成，理论自洽，但若 XMeta 与 ORM 不同步（MA1 未报告此类），`_gen` 层可能携带字段 drift。MR2 修复 delta 层 P1-MA4-023 时建议同步核验 `_gen/_ErpMfgWorkOrder.view.xml` 是否同型。
- **gen-control 内联脚本无编译期校验**：P2-MA4-014/015 根因是 gen-control `<c:script>` 为运行期 JS，无 schema/类型校验——`ACTIVE`/通用调色板类漂移只能经运行时视觉回归或本型静态审计发现。A4.7/A4.8 应沿用本审计维度 6 方法。
- **P1-MA4-023 修复方向**：将 `row-close-button` visibleOn 的 `'STARTED'` 改为 `'IN_PROCESS'`（或补 `'STOPPED'`）以对齐 dict + Processor 实际写入值；同步核验进度 badge（P2-MA4-015 同型）。

## 7. 范围内/范围外

- **范围内**：finance 72 + mfg 62 view.xml vs 后端契约 7 维度 drift（done）。
- **范围外**（Deferred）：pur+sal+inv view.xml drift（A4.7）/ crm+hr view.xml drift（A4.8）/ i18n 完整性（A4.9）/ 后端代码实现质量（A4.1a/b + A4.2a/b 已 done）/ 报表 page.yaml 渲染层（前端 UI-roadmap 已修复）/ 像素级视觉回归（前端 UI-roadmap Deferred）。
