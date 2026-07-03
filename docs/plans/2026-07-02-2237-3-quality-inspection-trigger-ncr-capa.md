# 2026-07-02-2237-3-quality-inspection-trigger-ncr-capa 质检触发 + 质检单状态机 + NCR/CAPA 闭环

> Plan Status: active
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.4（质检触发 + NCR/CAPA 流程）；`docs/design/quality/state-machine.md`；`docs/design/manufacturing/state-machine.md §质检对工单状态的约束声明`
> Related: `2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`（N=1 工单完工质检 config-gated 钩子，本计划提供 quality 侧实现 + flip `erp-mfg.inspection-gate-enabled`）
> Mission: erp
> Work Item: 2.4 质检触发 + NCR/CAPA 流程（质检单状态机 + 结果回写 + NCR 闭环 + CAPA 效果验证）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **质检实体已完备（非新建）**：`ErpQaInspection`（quality.orm.xml:127）`code`/`inspectionType`(dict `erp-qa/inspection-type` INCOMING=10/IN_PROCESS=20/FINAL=30/OUTGOING=40)/`relatedBillType`+`relatedBillCode`+`relatedLineCode`(关联业务单据)/`materialId`/`templateId`/`supplierId`/`warehouseId`/`batchNo`/`lotQuantity`/`sampleQuantity`/`inspectorId`/`result`(dict `erp-qa/inspection-result` PENDING=10/ACCEPTED=20/CONDITIONAL=30/REJECTED=40)/`docStatus`(dict `erp-qa/doc-status`)/`approveStatus`(dict `erp-qa/approve-status`)/`posted`(质量结论已回写业务单据)。`ErpQaInspectionLine`（:176）`parameterId`/`parameterName`/`specMin`/`specMax`/`measuredValue`/`result`(同 inspection-result)。
- **NCR + CAPA 实体已完备**：`ErpQaNonConformance`（:258）`sourceType`+`sourceCode`(来源单号)/`materialId`/`inspectionId`/`quantity`/`description`/`severity`(dict `erp-qa/severity`)/`dispositionType`(dict `erp-qa/disposition-type` SCRAP/RETURN/CONCESSION/DOWNGRADE)/`status`(dict `erp-qa/ncr-status` OPEN=10/IN_REVIEW=20/RESOLVED=30/ESCALATED_TO_RECALL=35/CANCELLED=40)/`assignedTo`/`resolvedBy`/`resolvedAt`/`resolution`。`ErpQaAction`（:304）`ncrId`/`actionType`(dict `erp-qa/action-type` CORRECTIVE/PREVENTIVE/CAPA)/`description`/`responsiblePerson`/`dueDate`/`status`(dict `erp-qa/action-status` PENDING/IN_PROGRESS/COMPLETED/OVERDUE)/`completedBy`/`completedAt`/`verificationPerson`/`verificationDate`。
- **质检模板已完备**：`ErpQaInspectionTemplate`（:204）`inspectionType`/`materialId`/`isActive`；`ErpQaInspectionTemplateLine`（:231）`parameterName`/`specMin`/`specMax`/`unit`/`isRequired`/`inspectionMethod`（**无 parameterId 列**——模板行以 parameterName 文本标识；质检单行 `ErpQaInspectionLine` 有 parameterId+parameterName，模板→行复制时 parameterName 对应、parameterId 留空）——质检单生成时按物料 × 检验类型匹配模板，复制模板行到质检单行。
- **BizModel 全为空 CRUD 壳**：`ErpQaInspectionBizModel`/`ErpQaInspectionLineBizModel`/`ErpQaNonConformanceBizModel`/`ErpQaActionBizModel` —— **无状态机、无结果录入、无 NCR 闭环、无 CAPA 效果验证、无业务触发**。`ErpQaErrors`/`ErpQaConstants`/`ErpQaConfigs` 已存在为空接口骨架（须扩展，非新建）。
- **业务触发当前无钩子**：采购入库/销售出库/工单完工均**无调 quality 生成质检单的钩子**。N=1 工单计划已留 config-gated 钩子（`erp-mfg.inspection-gate-enabled` 默认 false，待本计划 quality 侧实现后 flip）。采购/销售 Processor（1.6/1.7 done）无 quality 触发。
- **结果反馈无机制**：质检结果 ACCEPTED/REJECTED 无回写业务域的机制（业务域不知道质检结论）。
- **NCR 过账为 Non-Goal**：`state-machine.md §NCR 财务影响规则`（退货/返工/报废凭证）依赖 finance 域过账 Provider + purchase/sales 退货流程，属后续业财一体面。
- **DAG 依赖方向**：quality 引用 master-data（物料/供应商/仓库/职员/模板）；**业务域→quality 触发**（purchase/sales/manufacturing 调 quality I*Biz 创建质检单，DAG business→quality 合法 I*Biz 写触发，非 ORM refEntityName 引用）；**quality→business 结果反馈**经查询 API（business 查 quality 结果，quality 不反向依赖 business，无环）。
- **剩余差距**：(1) 无质检单状态机（PENDING→ACCEPTED/CONDITIONAL/REJECTED + 行级评测 + 结果汇总）；(2) 无业务触发（采购入库/销售出库/工单完工→生成质检单）；(3) 无 NCR 状态机（OPEN→IN_REVIEW→RESOLVED/ESCALATED/CANCELLED）；(4) 无 CAPA 效果验证闭环；(5) 无结果反馈查询（业务域查质检结论）。

## Goals

- **质检单状态机（4 态 + 行级评测）**：`IErpQaInspectionBiz`（quality 域）实现 `recordResult(inspectionId, lineResults)` —— 按行级 specMin/specMax vs measuredValue 自动评测行结果；汇总：全行 ACCEPTED → ACCEPTED；部分不合格但非关键 + 让步审批 → CONDITIONAL；关键不合格或整体不达标 → REJECTED。迁移遵循 `state-machine.md §适用对象一`（PENDING→ACCEPTED/CONDITIONAL/REJECTED，终态不可恢复）。质检单审批（approveStatus SUBMITTED→APPROVED）+ posted=true（结论已回写）。
- **业务触发 `createForBusinessBill(billType, billCode, materialId, inspectionType, ...)`**：业务域 Processor（采购入库→INCOMING / 销售出库→OUTGOING / 工单完工→FINAL）调此入口生成 ErpQaInspection（result=PENDING）+ 按物料 × inspectionType 匹配 `ErpQaInspectionTemplate`（active）复制模板行到质检单行；无匹配模板时按全局默认模板或提示先配置（`state-machine.md §异常路径`）。
- **强制质检业务单据阻塞**：`erp-qua.mandatory-inspection-bill-types`（配置：强制质检的业务单据类型列表）列出的业务单据，在质检 PENDING 时阻塞流转——业务域经 `IErpQaInspectionBiz.findByRelatedBill(billType, billCode)` 查质检结论，未 ACCEPTED/CONDITIONAL 时拒绝继续。
- **NCR 状态机（5 态 + CAPA 闭环）**：`IErpQaNonConformanceBiz` 实现 `submitReview`（OPEN→IN_REVIEW）、`resolve`（IN_REVIEW→RESOLVED，须全部 CAPA ErpQaAction.status=COMPLETED + verificationPerson/verificationDate 已填——效果验证）、`escalateToRecall`（IN_REVIEW→ESCALATED_TO_RECALL）、`cancel`（OPEN/IN_REVIEW→CANCELLED）。CAPA `IErpQaActionBiz` 实现 `startAction`/`completeAction`/`verifyAction`（PENDING→IN_PROGRESS→COMPLETED + 验证人/验证日期）。
- **NCR 自动生成**：质检单 REJECTED 时自动生成 ErpQaNonConformance（sourceType=INSPECTION/sourceCode=质检单号/inspectionId/materialId/quantity/dispositionType 待 NCR 评审裁决）。
- **结果反馈查询**：`IErpQaInspectionBiz.findByRelatedBill(billType, billCode)` 返回质检单 + result（供业务域查结论决定继续/退货/返工）。
- 行为测试覆盖：质检单行级评测（全合格/部分不合格/关键不合格）、结果汇总（ACCEPTED/CONDITIONAL/REJECTED）、业务触发（采购入库/工单完工→生成质检单）、强制质检阻塞、NCR 自动生成、NCR 状态机（评审/解决/升级召回/取消）、CAPA 闭环（须全部 COMPLETED + 验证才能 resolve）、模板匹配（有模板/无模板默认）。

## Non-Goals

- **NCR 财务过账（退货/返工/报废凭证）**：`state-machine.md §NCR 财务影响规则`；依赖 finance 域过账 Provider + purchase/sales 退货流程（1.9/1.10 done 但 NCR 驱动退货属业财一体面）。**触发条件**：NCR 驱动自动退货/报废过账 Provider 落地时（successor）。
- **召回事件（ErpQaRecall）**：`state-machine.md §NCR 状态机` ESCALATED_TO_RECALL 终态指向 recall.md；召回属工作项 2.11。**触发条件**：2.11 批次召回落地时。
- **让步接收审批流（条件审批工作流）**：`state-machine.md §迁移完整性` CONDITIONAL 需「让步审批」；本期以 approveStatus APPROVED 简化（质量主管审核），完整多级审批工作流 Non-Goal。**触发条件**：多级让步审批需求时。
- **抽检方案自动计算（sampling plan）**：`ErpQaSamplingPlan` 实体存在但抽样数量自动计算 Non-Goal。**触发条件**：统计抽样方案（AQL/GB2828）需求时。
- **校准管理（ErpQaCalibration）**：检验设备校准属独立面。**触发条件**：计量管理需求时。
- **风险登记/质量目标/评审（ErpQaRiskRegister/QualityGoal/Review）**：QMS 高级功能，独立面。**触发条件**：QMS 全面落地时。

## Task Route

- Type: `app-layer design change + implementation`（质检单状态机 + NCR/CAPA 闭环 + 业务触发 + 结果反馈查询；纯服务层 + 既有实体，不新增实体/列/字典，不触及 model/*.orm.xml）。
- Owner Docs: `docs/design/quality/state-machine.md`（质检单 4 态 + NCR 5 态 + 迁移完整性 + 异常路径 + 外部依赖 + NCR 财务影响规则）、`docs/design/quality/README.md`（质检对制造域约束声明）、`docs/design/manufacturing/state-machine.md §质检对工单状态的约束声明`（双向约束声明）、`docs/architecture/data-dependency-matrix.md`（business→quality R + quality 不反向依赖）。
- Skill Selection Basis: BizModel + 跨实体（质检单/行 + NCR + Action + 模板 + 物料 + 业务单据）+ 状态机 + 跨域（business→quality 触发 I*Biz + business 查结果 R）+ 事务 + 错误码 → 加载 `nop-backend-dev`。
- **Decision（结果反馈方向）**：**选择** 业务域查 quality 结果（`IErpQaInspectionBiz.findByRelatedBill`），quality 不反向依赖 business（无环）。**替代**：quality 反向调 business I*Biz 回写（循环依赖，rejected）。**残留风险**：业务域须主动查（经 config-gated 钩子在 confirm/DONE 前查）。
- **Decision（行级评测规则）**：**选择** specMin/specMax vs measuredValue 数值比较（VARCHARM→ 解析为 Double 比较；解析失败视为不合格）；行结果汇总：全 ACCEPTED→ACCEPTED；含 REJECTED 但经让步→CONDITIONAL；含 REJECTED 且未让步→REJECTED。**替代**：人工逐行判定不自动汇总（效率低，rejected 作唯一路径；自动评测 + 人工覆盖更优）。**残留风险**：specMin/specMax/measuredValue 为 VARCHAR，非数值规格（如外观「合格/不合格」）须人工录入行结果。
- **Decision（触发耦合度）**：**选择** 业务域 Processor 调 `IErpQaInspectionBiz.createForBusinessBill`（business→quality 合法 I*Biz 写触发，非 ORM refEntityName 引用）；quality-dao 声明 createForBusinessBill + findByRelatedBill 接口；purchase/sales/manufacturing-service compile 依赖 quality-dao。**替代**：事件驱动异步（Nop 跨实体以 I*Biz 同步为主，事件驱动增加最终一致性复杂度，rejected 作本期）。**残留风险**：同步触发失败回滚业务单据审核（符合 `state-machine.md §异常路径` 强制质检阻塞语义）。
- **Decision（NCR resolve 门控）**：**选择** NCR resolve 须全部关联 ErpQaAction.status=COMPLETED + verificationPerson/verificationDate 已填（效果验证闭环，对齐 `state-machine.md §NCR 与 CAPA 的关系`）。**替代**：无门控直接 resolve（CAPA 闭环断裂，rejected）。
- **Decision（模板匹配）**：**选择** 按 materialId × inspectionType 匹配 active 模板；复制模板行（`parameterName`/`specMin`/`specMax` → 质检单行，InspectionLine.parameterId 留空）；无匹配时用 `erp-qua.default-inspection-template`（全局默认模板 id），仍无则质检单无行（人工补录）。**替代**：无模板拒绝创建（阻断业务，与 `state-machine.md §异常路径`「按全局默认模板」矛盾，rejected）。

## Infrastructure And Config Prereqs

- 配置项：`erp-qua.mandatory-inspection-bill-types`（默认空=不强制；逗号分隔业务单据类型如 `ERP_PUR_RECEIPT,ERP_MFG_WORK_ORDER`，列出的单据强制质检阻塞）、`erp-qua.default-inspection-template`（默认空=无全局默认模板 id）、`erp-qua.auto-create-ncr-on-reject`（默认 true，REJECTED 自动生成 NCR）。经 `AppConfig.var(..., defaultValue)` 读取，无 .env。
- 模块依赖：`erp-qa-service` 已 compile 依赖 master-data-dao（物料/供应商/仓库/职员）；**业务触发需** purchase-service/sales-service/manufacturing-service compile 依赖 `app-erp-quality-dao`（`IErpQaInspectionBiz` 声明于 quality-dao，DAG business→quality 合法 I*Biz 写触发）。
- **无 ORM 变更**（不加实体/列/字典）：Inspection/InspectionLine/NonConformance/Action/Template 表列齐备。**故无 ask-first 保护区域门控**（纯服务层 + 既有表）。
- 无数据迁移；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — 质检单状态机（行级评测 + 结果汇总 + 审批 + posted）+ 测试

Status: planned
Targets: `module-quality/erp-qa-service/.../entity/ErpQaInspectionBizModel.java`(扩)、`IErpQaInspectionBiz.java`(扩)、`InspectionResultEvaluator.java`(新)、`ErpQaErrors.java`(扩)、`ErpQaConstants.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无（既有质检实体 + 模板为基线）。

- [ ] `Add`：`IErpQaInspectionBiz.recordResult(inspectionId, lineResults)` —— 行级评测（specMin/specMax vs measuredValue 数值比较，解析失败视为不合格）；汇总全行 → ACCEPTED / CONDITIONAL（部分不合格+让步审批 approveStatus APPROVED）/ REJECTED；迁移 result PENDING→终态；posted=true。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpQaInspectionBiz.findByRelatedBill(billType, billCode)` —— 按关联业务单据反查质检单 + result（供业务域查结论，跨域只读经 I*Biz）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：行级评测规则（specMin/specMax vs measuredValue 数值比较 + 汇总）+ 结果反馈方向（business 查 quality），见 Task Route Decision。
  - Skill: none
- [ ] `Proof`：`TestErpQaInspectionStateMachine`（全合格→ACCEPTED；部分不合格+让步→CONDITIONAL；关键不合格→REJECTED；行级 specMin/specMax 评测；终态不可恢复重复 recordResult 抛错；findByRelatedBill 返回正确 result）。`mvn test -pl module-quality/erp-qa-service -am -Dtest=TestErpQaInspectionStateMachine*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付质检单状态机 + 行级评测 + 结果汇总 + 反查。解除 Phase 2 业务触发的 quality 侧 API + Phase 3 NCR 自动生成基线。

- [ ] 质检单 4 态（行级评测 + 汇总 + posted + findByRelatedBill）单测通过

### Phase 2 — 业务触发（createForBusinessBill + 模板匹配 + 强制质检阻塞）+ 测试

Status: planned
Targets: `module-quality/erp-qa-service/.../entity/ErpQaInspectionBizModel.java`(扩 createForBusinessBill)、`IErpQaInspectionBiz.java`(扩)、`InspectionTriggerService.java`(新)、`ErpQaConfigs.java`(扩)、各业务域 Processor/Pom（config-gated 钩子接线）、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（recordResult + findByRelatedBill）。

- [ ] `Add`：`IErpQaInspectionBiz.createForBusinessBill(billType, billCode, materialId, inspectionType, lotQuantity, supplierId, warehouseId, batchNo)` —— 生成 ErpQaInspection（result=PENDING + relatedBillType/Code + inspectionType + 模板匹配复制行）；按 materialId × inspectionType 匹配 active `ErpQaInspectionTemplate`；无匹配走 `erp-qua.default-inspection-template`；仍无则无行（人工补录）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：config-gated 业务触发接线——采购入库 confirm（INCOMING）/ 工单完工 reportCompletion（FINAL，连 N=1 `erp-mfg.inspection-gate-enabled`）/ 销售出库 confirm（OUTGOING）时，若物料/单据类型在 `erp-qua.mandatory-inspection-bill-types` 中，调 `createForBusinessBill`；业务域 confirm/DONE 前查 `findByRelatedBill`，未 ACCEPTED/CONDITIONAL 时拒绝（强制质检阻塞）。purchase/sales/manufacturing-service compile 依赖 quality-dao。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：触发耦合度（business→quality 同步 I*Biz R）+ 模板匹配（materialId×inspectionType → 全局默认 → 无行人工补录），见 Task Route Decision。
  - Skill: none
- [ ] `Proof`：`TestErpQaInspectionTrigger`（采购入库 confirm→生成 INCOMING 质检单 + 模板匹配复制行；强制质检阻塞（PENDING 时 confirm 拒绝）；ACCEPTED 后 confirm 通过；无模板走默认；销售出库 OUTGOING 触发；工单 FINAL 触发 + inspection-gate-enabled=true 门控联动）。`mvn test -pl module-quality/erp-qa-service -am -Dtest=TestErpQaInspectionTrigger*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付业务触发 + 模板匹配 + 强制质检阻塞。解除 Phase 3 NCR 自动生成的触发基线。

- [ ] 业务触发（采购/销售/工单 → 生成质检单 + 模板匹配 + 强制阻塞）单测通过

### Phase 3 — NCR 状态机 + CAPA 闭环 + 自动生成 + 端到端 + 文档/日志

Status: planned
Targets: `module-quality/erp-qa-service/.../entity/ErpQaNonConformanceBizModel.java`(扩)、`IErpQaNonConformanceBiz.java`(扩)、`ErpQaActionBizModel.java`(扩)、`IErpQaActionBiz.java`(扩)、`NcrLifecycleService.java`(新)、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/quality/state-machine.md`(偏离补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（REJECTED 结果）+ Phase 2（业务触发）。

- [ ] `Add`：NCR 自动生成——`IErpQaInspectionBiz.recordResult` REJECTED 时，若 `erp-qua.auto-create-ncr-on-reject=true`，自动生成 ErpQaNonConformance（sourceType=INSPECTION/sourceCode=质检单号/inspectionId/materialId/quantity=不合格量/status=OPEN/dispositionType 待裁决）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpQaNonConformanceBiz` NCR 5 态迁移——`submitReview`（OPEN→IN_REVIEW）、`resolve`（IN_REVIEW→RESOLVED，**须全部关联 ErpQaAction.status=COMPLETED + verificationPerson/verificationDate 已填**）、`escalateToRecall`（IN_REVIEW→ESCALATED_TO_RECALL）、`cancel`（OPEN/IN_REVIEW→CANCELLED）。非法迁移抛 `ErpQaErrors.ERR_INVALID_NCR_STATUS_TRANSITION`。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpQaActionBiz` CAPA 生命周期——`startAction`（PENDING→IN_PROGRESS）、`completeAction`（IN_PROGRESS→COMPLETED + completedBy/completedAt）、`verifyAction`（COMPLETED + verificationPerson/verificationDate 填写）；逾期 `erp-qa.action-overdue-check` 配置门控标记 OVERDUE。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：NCR resolve 门控（须全部 CAPA COMPLETED + 验证）+ NCR 自动生成（REJECTED 触发），见 Task Route Decision。
  - Skill: none
- [ ] `Proof`：端到端 `TestErpQaNcrCapaEndToEnd`（采购入库→质检 REJECTED→自动生成 NCR OPEN→评审 IN_REVIEW→制定 CAPA Action PENDING→执行 IN_PROGRESS→完成 COMPLETED + 验证→NCR resolve RESOLVED；NCR 未全部 CAPA COMPLETED 时 resolve 拒绝；escalateToRecall → ESCALATED_TO_RECALL 终态；cancel）。`mvn test -pl module-quality/erp-qa-service -am -Dtest=TestErpQaNcrCapaEndToEnd*`。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.4 标注 done；`quality/state-machine.md` 偏离（NCR 财务过账 Non-Goal + 召回 2.11 Non-Goal + 让步审批简化 + 抽检方案/校准/QMS 高级 Non-Goal）补注。
  - Skill: none

Exit Criteria:

> Phase 3 交付 NCR 状态机 + CAPA 闭环 + 自动生成 + 端到端。完整仓库验证属 Closure Gates。

- [ ] NCR 5 态（+ CAPA 闭环门控）+ 自动生成 + 端到端单测通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0dcb270ebffe7sWYQSC91CcsgB`，独立 general 子代理）。1 BLOCKER：(B1) `ErpQaInspectionTemplateLine`（quality.orm.xml:231）无 `parameterId` 列——实际为 `parameterName`（质检单行 InspectionLine 有 parameterId+parameterName，模板行只有 parameterName），原计划 baseline 误列 parameterId。**已修订**：baseline 改 parameterName + 补注模板→行复制时 parameterId 留空；Decision 模板匹配补注复制映射。非阻塞 nit（ErpQaErrors/Constants/Configs 已存在空骨架标 扩 非 新；DAG 标签 business→quality 为 I*Biz 写触发非 R）已吸收。
- Independent draft review iteration 2: **accept / consensus**（`ses_0dca3ae75ffeC0NVOQ6frwi0bQ`，独立 general 子代理）。iter-1 B1（TemplateLine 无 parameterId 只有 parameterName）**确认已解决**（核实 quality.orm.xml:234-251 列为 parameterName/specMin/specMax/unit/isRequired/inspectionMethod 无 parameterId；baseline + Decision 模板→行复制 parameterId 留空 补注一致）。ErpQaErrors/Constants/Configs 已存在空骨架标 扩 非 新 已落地。DAG 标签 business→quality I*Biz 写触发非 R 已修正。无新 BLOCKER。全部实体/字典/BizModel 壳/业务触发空白/ErpMfgBom.inspectionRequired/N=1 inspection-gate-enabled 经逐条核实。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [ ] 范围内行为完成：质检单 4 态（行级评测 + 结果汇总）+ 业务触发（采购/销售/工单 + 模板匹配 + 强制阻塞）+ NCR 5 态 + CAPA 闭环（效果验证门控）+ 自动生成，行为测试通过
- [ ] 相关文档对齐：`extended-roadmap.md` 2.4 done 标注；当日日志已记；`quality/state-machine.md` Non-Goal 偏离补注
- [ ] 已运行验证：`mvn test -pl module-quality/erp-qa-service -am`（CRUD 0 回归 + 新增质检/NCR/CAPA）；根 `mvn clean install -DskipTests`
- [ ] 无范围内项目静默降级（NCR 财务过账/召回/让步审批流/抽检方案/校准/QMS 高级 均为计划内 Non-Goal）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### NCR 财务过账（退货/返工/报废凭证）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖 finance 域 NCR 驱动过账 Provider + purchase/sales 退货流程驱动；属业财一体面。
- Successor Required: yes（触发条件：NCR 驱动自动退货/报废过账 Provider 落地时）

### 召回事件（ErpQaRecall，工作项 2.11）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: NCR ESCALATED_TO_RECALL 为终态指向 recall.md；召回属 2.11。
- Successor Required: yes（触发条件：2.11 批次召回落地时）

### 让步接收多级审批流 / 抽检方案自动计算 / 校准管理 / 风险登记/质量目标/评审

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 各为 QMS 独立深化面；本计划仅质检触发 + 质检单状态机 + NCR/CAPA 基础闭环。
- Successor Required: yes（触发条件：多级审批/统计抽样/计量管理/QMS 全面需求时）

## Closure

Status Note: <待执行完成后填写>

Closure Audit Evidence:

<待独立结束审计后填写>

Follow-up:

- NCR 财务过账（见上方 Deferred）
- 召回事件 2.11（见上方 Deferred）
- 让步审批流 / 抽检方案 / 校准 / QMS 高级（见上方 Deferred）
