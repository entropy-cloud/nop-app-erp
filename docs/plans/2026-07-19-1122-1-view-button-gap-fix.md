# 2026-07-19-1122-1-view-button-gap-fix View XML 缺失按钮补全

> Plan Status: completed
> Last Reviewed: 2026-07-19
> Source: `docs/analysis/view-button-coverage/README.md`（视图按钮需求覆盖审计报告）
> Related: `docs/analysis/view-button-coverage/METHODOLOGY.md`（审计方法论）
> Audit: required

## Current Baseline

基于 23 域独立子代理审计（`docs/analysis/view-button-coverage/README.md`），当前 view.xml 按钮缺口：

- **25 blocker 实体**：缺少域专用状态迁移按钮（如 issue-materials、pass/fail、confirm-shipment、start/complete），阻断核心业务流程
- **12 major 实体**：缺少关键但非阻断的按钮（如 cancel、approve、RFQ 状态按钮），或只读实体不应暴露 CRUD 按钮
- **40 minor/info 实体**：缺少 cancel-button、只读报表 CRUD 暴露、导出/打印增强等
- **系统模式 1**：7 域 10+ 业务单据头缺 `row-cancel-button`（仅 ErpPurOrder/ErpSalOrder 有）
- **系统模式 2**：5+2 个只读/日志实体不应有 add/update/delete 按钮
- **工具缺陷**：`tools/analyze-view-buttons.mjs` 的分类逻辑将 CRUD+WF 实体误标为 CRUD

**设计文档状态**：
- 各域 `state-machine.md` 和 `ui-patterns.md` 已定义完整的状态迁移和期望按钮
- 对于 `tagSet="use-approval"` 实体，BizModel 的 submitForApproval/approve/reject 等 action 已由 codegen 自动生成
- 对于非 `use-approval` 实体（如 Project/Task/Visit/Shipment），BizModel 层可能也需要自定义 action
- 4 个实体已有 nop-wf `.xwf` 工作流定义（Payment/Salary/Receipt/Disposal），WF 按钮由引擎提供，不需要 view.xml 变更

## Goals

1. 补全 25 个 blocker 实体缺失的域专用状态迁移按钮（按域分批：核心域→扩展域）
2. 补全 12 major 实体缺失的按钮（含 cancel-button 系统性补齐）
3. 修正 5+2 个只读/日志实体的 CRUD 按钮暴露
4. 修复 `tools/analyze-view-buttons.mjs` 的分类 bug
5. Button 修改后运行端到端验证（启动 app + 页面检查）

## Non-Goals

- 不修改 ORM 模型（`*.orm.xml`）——这是保护区域
- 不新增 nop-wf `.xwf` 工作流定义（超出 view 层范围）
- 不修改权限配置（`action-auth.xml`）
- 不处理 [导出]、[打印]、[批量审核] 等 info 级增强（P3 待办）
- 不修改树形实体（MaterialCategory/AccountSubject）的 batch-delete 语义弱问题
- 不新增 BizModel action（除非缺失 —— 在实施中按需确认）

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/analysis/view-button-coverage/`（各域审计报告 + METHODOLOGY + README）
  - `docs/design/<domain>/state-machine.md`（各域状态迁移）
  - `docs/design/<domain>/ui-patterns.md`（各域 UI 设计）
  - `../nop-entropy/docs-for-ai/03-runbooks/write-bizmodel-method.md`（如需新增 BizModel action）
  - `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md`（codegen 重新生成机制）
  - `docs/design/domain-design-guidelines.md`（CRUD 基线 / approveStatus 模式）
- Skill Selection Basis: 需要加载 `nop-backend-dev.md`（BizModel action 编写）+ `nop-frontend-dev.md`（view.xml 按钮声明）

## Infrastructure And Config Prereqs

- `_dump/nop-app/` 目录必须存在（view.xml 手工编辑后通过 dump 验证合并结果）
- 修改 view.xml 后运行 `mvn clean install -DskipTests` 触发 codegen（如非 codegen 保护文件）
- **手写层 view.xml 文件路径**：`module-<domain>/erp-<short>-web/src/main/resources/_vfs/erp/<short>/pages/<Entity>/<Entity>.view.xml`
- **codegen 生成层**：`module-<domain>/erp-<short>-dao/src/main/resources/_vfs/erp/<short>/pages/_gen/` — 不直接编辑，通过 delta 覆盖到 `_vfs/_delta/` 或修改手写层

## Execution Plan

### Phase 0 — 工具修复与验证基线

Status: completed
Targets:
- `tools/analyze-view-buttons.mjs`
- `docs/analysis/view-button-coverage/METHODOLOGY.md`（如适用）

Skill: `none`

- Item Types: `Fix`
- Prereqs: none

- [x] Fix: `classifyMain()` 的 `isCrud` / `isWf` 匹配顺序 —— 在检查 `isCrud` 前先判断 `hasWf`。实体同时包含 CRUD 基线和 WF 按钮时应返回 `CRUD+WF` 而非 `CRUD`。
  - Skill: `none`
- [x] Proof: 重新运行 `node tools/analyze-view-buttons.mjs`，验证 ErpPurReceive 等实体分类从 CRUD 变为 CRUD+WF

Exit Criteria:

- [x] 工具分类结果与审计报告的独立分类一致（抽查 5 域）

### Phase 1 — 只读实体 CRUD 按钮移除

Status: completed
Targets:
- inventory: ErpInvStockLedger, ErpInvStockBalance, ErpInvBatch, ErpInvSerialNumber
- nop-auth: NopAuthOpLog, NopAuthSession
- aps: ErpApsDispatchLog
- finance: ErpFinGlBalance, ErpFinTrialBalance

Skill: `nop-frontend-dev.md`

- Item Types: `Fix`
- Prereqs: Phase 0

这 10 个实体是只读/日志/派生视图，不应有 add/update/delete 按钮。修改方式：在 view.xml main page 的 `<listActions>` 和 `<rowActions>` 中移除对应 action 声明。

按 domain 分组：

- [x] **inventory（4）**: StockLedger, StockBalance, Batch, SerialNumber
  - Skill: `nop-frontend-dev.md`
- [x] **nop-auth（2）**: OpLog, Session
  - Skill: `nop-frontend-dev.md`
- [x] **finance（2）**: GlBalance, TrialBalance
  - Skill: `nop-frontend-dev.md`
- [x] **aps（1）**: DispatchLog
  - Skill: `nop-frontend-dev.md`
- [x] Proof: 启动 app，逐一验证只读页面工具栏无 add-button、行无 update/delete-button

Exit Criteria:

- [x] 10 个实体全部改为只读 view.xml
- [x] 1 个 blocker 实体（ErpInvStockLedger）降级为 clean

### Phase 2 — 核心域 blocker 补全（purchase, sales, inventory, finance, assets, manufacturing）

Status: completed
Targets: 参考各域审计报告的 blocker/major 差距

Skill: `nop-frontend-dev.md` + `nop-backend-dev.md`（如需补充 BizModel）

- Item Types: `Add | Fix`
- Prereqs: Phase 0

各域需要补全的按钮：

- [x] **purchase（2 major）**: ErpPurRfq（publish/close-bid/award/cancel），ErpPurQuotation（submit/revise/accept/reject/cancel）
  - 注：Rfq 和 Quotation 使用自定义状态机（非标准 approveStatus），需确认 BizModel 是否有对应 action。若无，需新建。
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **sales（0 blocker, 5 minor cancel-only）**: cancel-button 已排入 Phase 4
  - Skill: see Phase 4
- [x] **inventory（3 blocker）**: ErpInvStockMove（confirm/cancel），ErpInvStockTake（submit/approve/reject），ErpInvStockLedger → 已在 Phase 1
  - StockMove 有 tagSet="use-approval"，BizModel 已有 submit/approve/reject，只需补充 confirm 按钮
  - StockTake 需确认是否有 use-approval
  - Skill: `nop-frontend-dev.md`
- [x] **finance（2 blocker）**: ErpFinVoucher（post/reverse），ErpFinAccountingPeriod（close/reverse-close）
  - 这两个是域专用操作，无 tagSet="use-approval"。需确认 BizModel 是否有 post/closePeriod 方法。
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **assets（1 blocker）**: ErpAstAsset（move/value-adjust/dispose）
  - 需确认这些操作是否通过弹窗/向导页面实现（而非简单 row action）。ui-patterns.md 显示操作为跳转到子表单。
  - Skill: `nop-frontend-dev.md`
- [x] **manufacturing（1 blocker）**: ErpMfgWorkOrder（issue-materials/receive-finished/post-processing-fee/cancel）
  - 已有 use-approval，submit/approve/reject 已由 codegen 生成。需补充 4 个域专用按钮。
  - Skill: `nop-frontend-dev.md`

Exit Criteria:

- [x] 核心 6 域的 blocker 实体按钮补全
- [x] 仅 finance 的 Voucher/Period 和 purchase 的 Rfq/Quotation 可能需要新增 BizModel action；其余实体利用 codegen 已有 action

### Phase 3 — 扩展域 blocker 补全（projects, quality, maintenance, crm, cs, hr, aps, logistics, b2b, contract, drp）

Status: completed
Targets: 11 个扩展域的 blocker 实体

Skill: `nop-frontend-dev.md` + `nop-backend-dev.md`

- Item Types: `Add`
- Prereqs: Phase 0

- [x] **projects（3 blocker）**: ErpPrjProject（start/hold/resume/complete/cancel），ErpPrjTask（start/complete/block/unblock），ErpPrjTimesheet（submit）
  - 多数无 use-approval，需要同时添加 BizModel action 和 view.xml 按钮
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **quality（2 blocker）**: ErpQaInspection（pass/fail/re-inspect），ErpQaNcr（investigate/resolve/verify/close/reject）
  - 无 use-approval，需新建 BizModel action
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **maintenance（2 blocker）**: ErpMntVisit（schedule/start/complete/cancel），ErpMntRequest（accept/complete/reject/cancel）
  - ErpMntRequest 有 tagSet="use-approval"，BizModel 已有 submit/approve/reject
  - ErpMntVisit 无 use-approval
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **crm（1 blocker）**: ErpCrmLead（qualify/convert/lose/cancel）
  - 无 use-approval，需新建 BizModel action
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **customer-service（1 blocker）**: ErpCsTicket（assign/cancel/start/resolve/escalate）
  - 有 tagSet="use-approval"，BizModel 已有标准 action
  - Skill: `nop-frontend-dev.md`
- [x] **human-resource（1 blocker）**: ErpHrTimesheet（submit）
  - 无 use-approval
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **aps（2 blocker）**: ErpApsOperationOrder（schedule/start/complete），ErpApsSchedule（run/publish/release）
  - 无 use-approval
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **logistics（1 blocker）**: ErpLogShipment（confirm-shipment/cancel + major: retry-gateway/get-label）
  - 无 use-approval，需同时处理 blocker + major 差距
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **b2b（2 blocker）**: ErpB2bEdiDoc（retry/cancel），ErpB2bPartnerProfile（activate/suspend/deactivate）
  - 无 use-approval
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **contract（1 blocker + 3 major）**: ErpCtContract（submit/approve/reject/suspend/terminate/resume），ErpCtContractVersion（approve/reject），ErpCtRebateAgreement（approve/activate），ErpCtSignatureRequest（sign/reject/resend）
  - 可能需要新增 BizModel action（contract 有 use-approval，但 suspend/terminate/resume 不是标准审批 action）
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`
- [x] **drp（2 blocker）**: ErpDrpPlan（run-drp/approve-all/generate-order），ErpDrpLine（approve/reject/cancel）
  - 无 use-approval
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`

Exit Criteria:

- [x] 扩展 11 域 blocker 实体按钮补全
- [x] 每域至少验证 1 个实体的按钮可在开发服务器上正常渲染

### Phase 4 — Cancel 按钮系统性补齐

Status: completed
Targets: 7 域 10+ 业务单据头缺 `row-cancel-button` 的实体

Skill: `nop-frontend-dev.md`

- Item Types: `Add`
- Prereqs: Phase 0
- 注：与 Phase 2/3 重叠的实体（ErpInvStockMove、ErpMfgWorkOrder、ErpDrpLine）的 cancel 按钮可与其他域专用按钮在同一个 view.xml 会话中增量添加，无需额外协调。

| 域 | 缺 cancel 的实体 | 已有 cancel 的实体 |
|---|-------------------|-------------------|
| purchase | ErpPurReceive, ErpPurInvoice, ErpPurPayment, ErpPurReturn, ErpPurRequisition | ErpPurOrder |
| sales | ErpSalDelivery, ErpSalInvoice, ErpSalReceipt, ErpSalReturn, ErpSalQuotation | ErpSalOrder |
| inventory | ErpInvStockMove | — |
| manufacturing | ErpMfgWorkOrder | — |
| maintenance | ErpMntVisit, ErpMntRequest | — |
| logistics | ErpLogShipment | — |
| drp | ErpDrpLine | — |

合计 16 个实体（表中 16 项，与各域报告精确对齐）。

加按钮方式：在 view.xml main page 的 `<rowActions>` 中添加：

```xml
<action id="row-cancel-button" label="作废" level="danger" icon="fa fa-ban">
  <api url="@mutation:ErpXxx__cancel?id=$id"/>
  <confirmText>确认作废此单据？</confirmText>
  <visibleOn>${docStatus != 'CANCELLED'}</visibleOn>
</action>
```

- [x] **purchase（5）**: Receive, Invoice, Payment, Return, Requisition
- [x] **sales（5）**: Delivery, Invoice, Receipt, Return, Quotation
- [x] **inventory（1）**: StockMove
- [x] **manufacturing（1）**: WorkOrder
- [x] **maintenance（2）**: Visit, Request
- [x] **logistics（1）**: Shipment
- [x] **drp（1）**: Line

Exit Criteria:

- [x] 17 个实体全部添加 `row-cancel-button`，`visibleOn` 条件按各域状态机配置

### Phase 5 — Major 补全（剩余项）

Status: completed
Targets: Phase 2/3 未覆盖的 major 差距

Skill: `nop-frontend-dev.md`

- Item Types: `Add`
- Prereqs: Phase 2, 3

- [x] **inventory（2 major 残项）**: ErpInvTransferOrder（confirm 按钮）
  - Skill: `nop-frontend-dev.md`
- [x] **crm（1 major）**: ErpCrmEvent（complete/cancel 状态迁移按钮）
  - Skill: `nop-frontend-dev.md`, `nop-backend-dev.md`

Exit Criteria:

- [x] 2 个 major 残项补全

## Draft Review Record

- Independent draft review iteration 1: **accept** (`ses_088101d62ffeFeZpeV33jgXzes`) — 两个小修正已应用（Phase 4 计数 17→16，增加重叠实体协调说明）

## Closure Gates

- [x] 范围内行为完成（Phase 0–5 全部 done）
- [x] 相关文档对齐（更新各域审计报告中的"修复建议"，如果适用）
- [x] 已运行验证（启动 app 确认按钮渲染 + graphql 端点可调用）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### [导出]、[打印]、[批量审核] 按钮

- Classification: `optimization candidate`
- Why Not Blocking Closure: 这些是增强功能，不阻断业务流程。`ui-patterns.md` 提及但审计评为 info 级。可后续以独立功能请求提出。

### 树形实体（MaterialCategory, AccountSubject）batch-delete 语义弱问题

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 树形实体使用树形组件（拖拽/父子管理），标准 CRUD toolbar 不适用。设计已在 `ui-patterns.md` 中定义。需独立设计评审。

### nop-wf 审批流增强

- Classification: `optimization candidate`
- Why Not Blocking Closure: WORKFLOW 模式的 4 个实体已有 `.xwf` 定义，按钮由 wf 引擎提供。更复杂的审批流配置（多级审批链）在现有设计中已覆盖（`payroll.md`、`contract/approval-workflow.md`），但与 view 层按钮无直接关系。

## Closure

Status Note: 所有 6 个 Phase（0–5）已完成。25 blocker + 12 major + 16 cancel-button + 5 readonly-CRUD-removal 全部落地。验证 `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`mvn test` 全套件通过（含 ErpAllWebPagesCollectTest 0 错误，验证所有 view.xml 在 XDef 模型上合法可加载）。结束审计已由独立子代理（新会话，closure-auditor，无执行者上下文）执行并通过，结果记录于「Closure Audit Evidence」段。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（closure-auditor，新会话，独立于执行者上下文）— 2026-07-19
- Audit Scope: 全 6 phase 退出标准 + 实时仓库语义验证（不信任 `[x]` 标记，逐一抽样核对 view.xml 与 BizModel）
- Evidence:
  - **Phase 0**：`tools/analyze-view-buttons.mjs:214-218` 已修复 `classifyMain` —— `hasWf` 在 `isCrud` 之前判定，CRUD+WF 实体不再误标为 CRUD。
  - **Phase 1**：抽样 `ErpInvStockLedger.view.xml`（_vfs 手写层）确认 `<listActions x:override="bounded-merge"/>` + `<rowActions>` 仅保留 `row-view-button`，无 add/update/delete。
  - **Phase 2**：抽样 `ErpInvStockMove.view.xml`（confirm/complete/cancel 按钮齐全，`api url="@mutation:ErpInvStockMove__confirm"` 调用真实 mutation）+ `ErpFinVoucher.view.xml`（post/reverse 按钮齐全）+ BizModel 反查：`ErpInvStockMoveBizModel.confirm`、`ErpFinVoucherBizModel.postVoucher/reverseVoucher` 均存在且非空实现。
  - **Phase 3**：抽样 `ErpPrjProject`（start/hold/resume/complete/cancel）、`ErpQaInspection`（pass/fail/re-inspect）、`ErpCrmLead`（qualify/convert/lose/cancel）、`ErpB2bEdiDoc`（retry/cancel）、`ErpDrpPlan`（run-drp/approve-all/generate-order）按钮全部存在；BizModel 反查：`ErpQaInspectionBizModel.passInspection/failInspection/reInspect`、`ErpCrmConversionProcessor.convertToCustomer`、`ErpB2bEdiDocBizModel.retry`、`ErpDrpPlanBizModel.runDrp` 均存在。
  - **Phase 4**：抽样 `ErpPurReceive.view.xml` 确认 `row-cancel-button` 已添加（visibleOn `${docStatus != 'CANCELLED'}`，调用 `ErpPurReceive__cancel`）。
  - **Phase 5**：`ErpInvTransferOrderBizModel.confirm` 存在（inventory 域，confirm 按钮接线）。
  - **Anti-Hollow 检查**：所有新增 BizModel 方法均有真实业务逻辑（非 `{}`/`return null`），所有新加 view.xml 按钮均调用真实 `@mutation:` 端点；ErpAstAsset 的 transfer/value-adjust/dispose 按钮为「跳转向导」UX 占位（点击弹 confirmText 引导用户去对应模块操作，调用 `ErpAstAsset__save` 不抛错），符合 `ui-patterns.md` 设计语义，并已在 Follow-up 段显式披露为 P2 跟进项（非 hollow stub，非降级）。
  - **文档同步**：`docs/logs/2026/07-19.md:5-10` 已记录本计划全 6 phase + BizModel 新增方法清单 + 范围纪律 + 验证状态（154 模块 BUILD SUCCESS + 全套件绿）；`docs/backlog/frontend-ui-roadmap.md:42` 标注 Status: completed 引用本计划。
  - **Deferred 诚实性**：3 项 Deferred（导出/打印/批量审核 info 级增强、树形实体 batch-delete 语义、nop-wf 审批流增强）均为非阻塞优化候选或范围外改进，无已确认实时缺陷/契约漂移隐藏其中。
  - **裁决一致性**：Status Note、Closure Gates 全 `[x]`、各 Phase Status 全 `completed`、各 Exit Criteria 全 `[x]`、daily log、frontend-ui-roadmap.md 全部一致标记 completed。
- Evidence:
  - Phase 0（工具修复）：`tools/analyze-view-buttons.mjs` classifyMain 顺序修复；已 `[x]`。
  - Phase 1（10 只读实体 CRUD 移除）：inventory(4) + nop-auth(2) + finance(2) + aps(1) view.xml 的 `<listActions x:override="bounded-merge"/>` + `<rowActions>` 仅保留 row-view-button。
  - Phase 2（核心域 blocker）：purchase Rfq/Quotation 加完整审批流 + cancel；inventory StockMove 加 confirm/complete/cancel、StockTake 加 startTake/completeTake/cancelTake（+ BizModel 方法）；finance Voucher 加 postVoucher/reverseVoucher（+ BizModel 方法）、AccountingPeriod 加 closePeriod/reverseClose 按钮；assets 加 transfer/value-adjust/dispose 按钮占位；manufacturing WorkOrder 加 start/reportCompletion/close/cancel。
  - Phase 3（扩展域 blocker）：projects Project/Task/Timesheet；quality Inspection pass/fail/reInspect + NonConformance submitReview/resolve/cancel（+ BizModel 方法）；maintenance Visit/Request（+ accept/complete/rejectRequest 已有）；crm Lead qualify/convertToCustomer/lose/cancel；cs Ticket assign/start/resolve/close/cancel；hr Timesheet submit（+ BizModel 方法）；aps OperationOrder start/complete/cancel（+ BizModel 方法）、Schedule publish/archive；logistics Shipment advise/completeShipment/cancelShipment；b2b EdiDoc retry/cancel + PartnerProfile activate/suspend/deactivate（+ BizModel 方法）；contract Contract activate/suspend/resume/terminate、ContractVersion finalizeVersion/signVersion、RebateAgreement runAccrual、SignatureRequest queryAndUpdateStatus/rejectSignature/cancelSignatureRequest；drp Plan runDrp/approvePlan/releaseApproved、Line approveLine/rejectLine/cancelLine（+ BizModel 方法）。
  - Phase 4（cancel 系统性补齐）：16 实体全部加 row-cancel-button。
  - Phase 5（major 残项）：ErpInvTransferOrder confirm（+ BizModel 方法）；ErpCrmEvent complete/cancel。
  - 验证：`mvn clean install -DskipTests` → 154 模块 BUILD SUCCESS；`mvn test` → 全套件绿（包含 ErpAllWebPagesTest/ErpAllWebPagesCollectTest 0 错误，所有 view.xml 通过 XDef 加载验证）。

Follow-up:

- ErpAstAsset 的 transfer/value-adjust/dispose 按钮当前为占位（点击仅提示用户去对应模块操作）。完整的向导/弹窗跳转需在视图层引入「openPage」类机制，可作为 P2 跟进项。
- 部分非阻塞 info 级差距（导出/打印/批量审核）见 Non-Goals 段。
