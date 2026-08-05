# 2026-08-05-1400-1 rc-ma1-a1-45-46-contract-lifecycle-billing-rebate contract 全域（A1.45 生命周期与签署 + A1.46 计费与返利）需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.45（MA1 需求追踪矩阵审计 — contract-F1 生命周期与签署 UC-CT-01/02/05/06/07/09）+ A1.46（contract-F2 计费与返利 UC-CT-03/04/08/10）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.45 + A1.46
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.45/A1.46 的 0.2 依赖）、`2026-08-06-0245-3-rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard.md`（同 mission MA1 末批范式）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。A1.45 与 A1.46 同属 contract 域（同一 owner doc、同一结果表面、同一审计方法论），按 plan 指南规则 14 合并为单计划双阶段（Phase 1 = A1.45 生命周期与签署 6 UC，Phase 2 = A1.46 计费与返利 4 UC）。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md:379-380` 已为 A1.45/A1.46 给出 UC 清单 = `UC-CT-01/02/05/06/07/09`（6 UC）+ `UC-CT-03/04/08/10`（4 UC）= 10 UC，覆盖率均 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/contract/use-cases.md`（机制细节引用各 owner doc，L2 设计参考）：
  - **A1.45 切片（F1 生命周期与签署）**：
    - **UC-CT-01 合同创建与签署**（`:3`）：模板填充 → 填写合同头/行 → 校验总金额=∑行金额 + startDate<endDate → DRAFT → 提交审批 NEGOTIATION（创建 v1，isCurrent=true）→ 双方确认签署（attachmentId）→ 确认签署 ACTIVE（signDate=now）；异常：金额超预算→拦截+特批。
    - **UC-CT-02 合同变更与版本管理**（`:25`）：基于 ACTIVE 合同创建变更单（parentContractId）→ 复制原行增删改 → DRAFT→NEGOTIATION→签署 ACTIVE → 生效时原版本 isCurrent=false + 新版本 versionNo 递增 isCurrent=true → 后续订单引用新版本；异常：驳回→原合同保持 ACTIVE。
    - **UC-CT-05 合同到期提醒与续期**（`:89`）：nop-job 每日扫描 `status=ACTIVE AND endDate BETWEEN now() AND now()+30d` → 到期前 30/15/7 天分级通知（经办人/上级）→ 续期 auto-create-renewal-draft（parentContractId 关联）或不续期 → endDate 到达→EXPIRED；异常：endDate 到达仍有未完成开票计划→先完成开票再 EXPIRED。
    - **UC-CT-06 合同提前终止**（`:111`）：发起终止（原因+附件）→ 法务审批 → 通过后 status→TERMINATED + 当前版本 isCurrent=false + 截停未执行 InvoicePlan + 生成善后 TODO → 财务最终结算 → 尾款结清 TODO 完成；异常：法务驳回→保持原状态。
    - **UC-CT-07 合同审批工作流**（`:134`）：提交→读 ErpCtApprovalMatrix 按 totalAmount 匹配审批节点→生成 ErpCtApprovalRecord（首节点 PENDING 其余 WAITING）→逐节点审批→全通过可 ACTIVE；异常：驳回超限(3 次)锁定升级+超时(72h)升级上级；跨域：master-data + notification。
    - **UC-CT-09 电子签章**（`:158`）：版本定稿 FINALIZED→发起签署请求→创建 ErpCtSignatureRequest(PENDING_SIGNATURE)→调用 Provider.initSignature()→签署人逐个签署(PARTIALLY_SIGNED)→全部完成 FULLY_SIGNED→webhook 回调下载文件→ContractVersion.status=SIGNED+certificateUrl→拒签 REJECTED；异常：超时 EXPIRED+提供商失败重试；跨域：master-data + notification。
  - **A1.46 切片（F2 计费与返利）**：
    - **UC-CT-03 开票计划生成与执行**（`:47`）：合同 ACTIVE 时按 invoiceTerm(ADVANCE/MILESTONE/MONTHLY/COMPLETION) 批量生成 InvoicePlan(planDate/amount/isInvoiced=false) → 到达 planDate 自动生成 AP/AR Invoice 草稿（调 finance）→ 审核后 isInvoiced=true+invoiceDate+invoiceBillCode 回写；异常：SUSPENDED→拦截执行标记待恢复；后置：已开票不允许改金额。
    - **UC-CT-04 消耗计费与用量结算**（`:69`）：按周期记录 ErpCtConsumptionLine(consumptionDate/quantity/unitPrice/amount/sourceBillType/Code) → 周期末汇总对比预估总量 → 超量生成额外 InvoicePlan → 生成 AP/AR Invoice 草稿；异常：超预估 120%→超量审批通知。
    - **UC-CT-08 批量折扣与返利**（`:146`）：批量折扣——合同行配数量→折扣率映射，订单引用时匹配折扣率算折后价；返利——签订 ErpCtRebateAgreement+阶梯 ErpCtRebateTier → 发票过账记录 ErpCtRebateAccrual → 累计跨越层级自动追溯补差 → 到期触发 ErpCtRebateSettlement 生成 AP/AR 信用单；异常：退货冲销致层级回落→冲销多计提；跨域：purchase/sales + finance。
    - **UC-CT-10 合同仓库与全文检索**（`:170`）：上传文档→ErpCtDocument 识别类型→OCR/提取文字→ocrText 写入+fullTextSearch 构建→元数据标签→全文搜索/高级过滤→保留策略自动归档(retentionDate,isArchived)+销毁(purgeDate)→Legal Hold 阻止归档/销毁；异常：OCR 失败手动设置/跳过+legalHold=true 禁止归档销毁；跨域：master-data。

- **L3 代码实现现状（实测，`module-contract/erp-ct-service`）**——**合同状态机 + 变更版本 + 签章 Processor 完整 + 审批/计费/返利/仓库候选缺口待逐 UC 核验**：
  - **UC-CT-01 创建与签署（✅ 状态机+activate Processor）**：`ErpCtContractBizModel.java`#activate(`:67`)委派 `ErpCtContractActivateProcessor`#activate。**待核**：①校验总金额=∑行金额 + startDate<endDate（L1 步骤 4）；②NEGOTIATION 创建 v1 版本 isCurrent=true（L1 步骤 6）；③金额超预算特批流程（L1 异常）。
  - **UC-CT-02 变更与版本（✅ amend Processor）**：`ErpCtContractBizModel.java`#amend(`:130`)委派 `ErpCtContractAmendProcessor`。`ErpCtContractVersionBizModel.java` 存在。**待核**：①复制原行到变更单；②生效时原版本 isCurrent=false + 新版本 versionNo 递增 isCurrent=true（L1 步骤 4）；③后续订单引用新版本。
  - **UC-CT-05 到期提醒与续期（⚠️ expire mutation 存在 + job 扫描+分级通知+续期草稿待核）**：`ErpCtContractBizModel.java`#expire(`:118`)存在。**待核**：①nop-job 每日扫描 `status=ACTIVE AND endDate BETWEEN now() AND now()+30d`（grep job/cron/scheduler 跨 contract）；②30/15/7 天分级通知（经办人/上级）——grep notification 调用；③续期 auto-create-renewal-draft（parentContractId）——grep renewal/autoCreateDraft；④endDate 到达仍有未完成开票→先完成开票再 EXPIRED（L1 异常）。
  - **UC-CT-06 提前终止（✅ terminate mutation）**：`ErpCtContractBizModel.java`#terminate(`:97`)存在。**待核**：①法务审批流程（grep approval/legal）；②截停未执行 InvoicePlan；③善后 TODO 生成；④当前版本 isCurrent=false（归档）；⑤财务最终结算联动。
  - **UC-CT-07 审批工作流（⚠️ 实体存在 + 节点生成/驳回超限/超时升级待核）**：`ErpCtApprovalMatrixBizModel.java`+`ErpCtApprovalRecordBizModel.java` 存在。**待核**：①读 ApprovalMatrix 按 totalAmount 匹配节点列表+生成 ApprovalRecord（首 PENDING 其余 WAITING）；②逐节点审批激活下一节点；③驳回超限(3 次)锁定强制升级+超时(72h)升级上级（L1 异常）——grep rejectCount/timeout/escalate。
  - **UC-CT-09 电子签章（✅ 签章 Processor 完整）**：`ErpCtSignatureRequestBizModel.java` 存在；Processors：`ErpCtSignatureRequestInitSignatureRequestProcessor`+`ErpCtSignatureRequestHandleSignatureCallbackProcessor`+`ErpCtSignatureRequestQueryAndUpdateStatusProcessor`+`ErpCtContractVersionSignVersionProcessor`+`AbstractErpCtSignatureRequestProcessor`。**待核**：①Provider.initSignature() 调用（grep e签宝/DocuSign/Tsign provider）；②签署人逐个签署状态迁移 PENDING→PARTIALLY→FULLY→SIGNED；③webhook 回调下载文件+certificateUrl；④超时 EXPIRED+拒签 REJECTED（L1 异常）。
  - **UC-CT-03 开票计划（✅ Trigger Processor 完整 + 跨域 finance 调用待核）**：`ErpCtInvoicePlanBizModel.java`+`ErpCtInvoicePlanTriggerInvoiceProcessor`+`ErpCtInvoicePlanTriggerDuePlansProcessor`。**待核**：①ACTIVE 时按 invoiceTerm(ADVANCE/MILESTONE/MONTHLY/COMPLETION) 批量生成 InvoicePlan；②到达 planDate 自动生成 AP/AR Invoice 草稿（调 finance 域 API——grep IErpFinInvoice/invoice facade）；③SUSPENDED 拦截执行标记待恢复（L1 异常）；④已开票不允许改金额（后置条件）。
  - **UC-CT-04 消耗计费（⚠️ 实体存在 + 周期汇总+超量审批待核）**：`ErpCtConsumptionLineBizModel.java` 存在。**待核**：①周期末汇总 ConsumptionLine 总量对比预估总量；②超量生成额外 InvoicePlan；③超预估 120%→超量审批通知（L1 异常）；④生成 AP/AR Invoice 草稿。
  - **UC-CT-08 批量折扣与返利（✅ 返利 Processor 完整 + 折扣应用/层级追溯/退货冲销待核）**：`ErpCtVolumeDiscountBizModel.java`+`ErpCtRebateAgreementBizModel.java`+`ErpCtRebateTierBizModel.java`+`ErpCtRebateAccrualBizModel.java`+`ErpCtRebateSettlementBizModel.java` 存在；Processors：`ErpCtRebateAgreementRunAccrualProcessor`+`ErpCtRebateSettlementPostSettlementProcessor`。**待核**：①订单引用合同行时按数量匹配折扣率算折后价（grep purchase/sales 引用 VolumeDiscount）；②发票过账记录 RebateAccrual；③累计跨越层级自动追溯补差；④到期 Settlement 生成 AP/AR 信用单（调 finance Credit Memo）；⑤退货冲销致层级回落→冲销多计提（L1 异常）。
  - **UC-CT-10 合同仓库与全文检索（⚠️ 实体存在 + OCR/全文检索/保留策略/Legal Hold 待核）**：`ErpCtDocumentBizModel.java` 存在。**待核**：①OCR 引擎识别/提取文字→ocrText 写入+fullTextSearch 构建（grep ocr/fullText/index）；②全文搜索/高级过滤（日期/金额/标签范围）；③保留策略自动归档(retentionDate→isArchived)+销毁(purgeDate→delete)；④Legal Hold 阻止归档/销毁（grep legalHold）。

- **L4 测试证据现状**（`module-contract/erp-ct-service/src/test`）：
  - `TestErpCtContractCrudSmoke.java`（CRUD 冒烟）；`TestErpCtContractPosting.java`（过账）；`TestErpCtContractTerminate.java`（终止状态机）；`TestErpCtESignature.java`（电子签章）；`TestErpCtRebateSettlementEnd.java`（返利结算）；`TestErpCtContractRebate.java`（返利计算）；`CtFrozenClockExtension.java`（时间冻结）。
  - **待核**：①UC-CT-01 总金额校验+版本创建断言；②UC-CT-02 变更版本 isCurrent 切换断言；③UC-CT-05 到期提醒分级通知+续期草稿断言（候选缺测试）；④UC-CT-07 审批节点生成+驳回超限+超时升级断言（候选缺测试）；⑤UC-CT-03 InvoicePlan 按 invoiceTerm 生成+跨域 finance 断言；⑥UC-CT-04 消耗汇总+超量审批断言（候选缺测试）；⑦UC-CT-08 折扣应用+层级追溯+退货冲销断言；⑧UC-CT-10 OCR/全文检索/保留策略/Legal Hold 断言（候选缺测试）。MA5 评级待引用。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **无 contract 专属 MA2 状态机报告**。本切片为 contract 域行为的首份证据（contract 域 MA1 首审）。
  - **contract 相关既有 finding**：arm-index contract/ct 域 finding 待 grep（候选：跨域 daoFor P1-MA1-022 命中[Contract entities]，平台一致性维度 resolved 不重审）。**无任何 UC-CT-01~10 需求符合性 finding**。
  - 本切片须声明与 MA2 报告差异增量（报告段落 9）：无 contract 专属 MA2 报告；只补需求视角差异。

- **arm-index 既有 finding 衔接**：grep arm-index contract/ct/UC-CT/lifecycle/amend/rebate/invoicePlan/signature/approval/document/OCR → **无 UC-CT-01~10 finding**。本切片须 grep arm-index contract 同域同控制点后裁决复用 or 新建 `P*-RC-xxx`（续编，执行时取最新——当前至 P2-RC-059 / P1-RC-071）。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10。本切片候选偏差多为**代码逻辑**类（预授权——到期提醒 job/分级通知/续期草稿/审批节点生成/InvoicePlan 跨域/OCR/全文检索）；若触及 ORM 结构（如 ErpCtDocument 加 OCR/全文索引字段）→ **ORM 结构变更须 ask-first + 独立 plan-audit**；返利结算触及 finance Credit Memo 过账的修复须 ask-first（会计过账类）；UC-CT-10 保留策略 purgeDate→delete 触及**数据删除**类保护区域，修复须 ask-first + 独立 plan-audit；须在报告逐项标注触及保护区域。

- **剩余差距**：A1.45+A1.46 切片五级追踪审计报告缺失 = MA4 及 MR1 该切片证据缺口来源。本计划产出 contract 域全域审计报告并登记 finding，解除 contract 域证据缺口（本切片完成后 contract 域 A1.45+A1.46 全 done，contract 域 10 UC 全覆盖）。

## Goals

- 产出 A1.45+A1.46 合并切片审计报告 `docs/audits/2026-08-05-1400-1-rc-ma1-a1-45-46-contract-lifecycle-billing-rebate.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-CT-01/02/03/04/05/06/07/08/09/10 逐条核验**每条验收标准**（完整枚举，§3，禁止跳号）：逐 UC 五级追踪（L1 逐字引用→L2 owner doc→L3 代码路径→L4 测试断言→L5 运行时行为）。
- 对候选缺口给出分级结论：UC-CT-05 到期提醒 job/分级通知/续期草稿（待核倾向 P1/P2）、UC-CT-07 审批节点生成/驳回超限/超时升级（待核倾向 P1/P2）、UC-CT-04 消耗汇总/超量审批（待核 P1/P2）、UC-CT-10 OCR/全文检索/保留策略/Legal Hold（待核 P1/P2）、UC-CT-03 跨域 finance Invoice 生成（待核）、UC-CT-08 折扣应用/层级追溯/退货冲销（待核）——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（续编）并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复；**ORM 结构/会计过账类须 ask-first**）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；audit reports 表新增 A1.45+A1.46 行——contract 域首审行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/owner doc/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他域**（logistics/aps/notify 为各自独立 plan）。
- **不重审 P1-MA1-022**（跨域 daoFor 平台一致性维度，resolved，不复审）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.45/A1.46 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md:379-380`（A1.45/A1.46 UC 锚点）+ `docs/design/contract/use-cases.md`（L1 真相源）+ contract 域 owner doc（L2 设计参考，非真相源——Deferred/Non-Goal 标注须 §4 三判据复核）+ `docs/audits/arm-index.md`（finding 衔接）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-contract/erp-ct-service -Dtest=TestErpCtContractTerminate,TestErpCtESignature,TestErpCtContractRebate,TestErpCtRebateSettlementEnd`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - A1.45 contract-F1 生命周期与签署五级追踪矩阵（UC-CT-01/02/05/06/07/09）

Status: completed
Targets: `docs/audits/2026-08-05-1400-1-rc-ma1-a1-45-46-contract-lifecycle-billing-rebate.md`（产出 §1-§5 for UC-CT-01/02/05/06/07/09）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-CT-01/02/05/06/07/09 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:3/25/89/111/134/158` 验收标准原文；L2 引用 contract 域 owner doc（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpCtContractBizModel`#activate/amend/expire/terminate + 对应 Processor（`ErpCtContractActivateProcessor`/`ErpCtContractAmendProcessor`）+ `ErpCtContractVersionBizModel` + `ErpCtApprovalMatrixBizModel`/`ErpCtApprovalRecordBizModel` + `ErpCtSignatureRequestBizModel` + 签章 5 Processor + grep job/cron/notification/renewal/escalate/rejectCount 站点（含行号）；L4 引用 `TestErpCtContractTerminate`/`TestErpCtESignature`#method（注明断言强度）；L5 标注无 contract 专属 MA2 报告。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条对照）：UC-CT-01 总金额校验+版本创建（⚠️待核）；UC-CT-02 复制原行+isCurrent 切换+versionNo 递增（⚠️待核）；UC-CT-05 **到期提醒 job 扫描/30-15-7 天分级通知/续期草稿/未完成开票先完成**（⚠️ grep job/cron/notification/renewal 待核，候选 P1/P2）；UC-CT-06 法务审批+截停 InvoicePlan+善后 TODO+归档版本（⚠️待核）；UC-CT-07 **节点生成按 totalAmount 匹配/驳回超限 3 次锁定/超时 72h 升级**（⚠️ grep ApprovalMatrix 匹配逻辑/rejectCount/timeout/escalate 待核，候选 P1/P2）；UC-CT-09 Provider.initSignature 调用+签署人逐个状态迁移+webhook 回调+超时/拒签（⚠️待核 Provider 具体实现）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 UC-CT-01/02/05/06/07/09 给出符合性结论（取最高）。UC-CT-01/02/06/09 倾向**接受**（状态机+Processor 完整）；UC-CT-05 到期提醒候选 **P1/P2**（§2 ①/⑤ 功能缺失/测试断言缺失——**§4 三判据关键裁决**：若有 owner doc Deferred/Non-Goal 标注须核人工批准痕迹）；UC-CT-07 审批工作流候选 **P1/P2**（§2 ①/③/⑤）。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核 + 触及保护区域标注。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5（UC-CT-01/02/05/06/07/09 部分）已落盘：6 UC 矩阵行（逐验收标准进入 L5 判读，无跳号），L1 逐字引用、L3 含行号 + grep 站点、L4 注明断言强度、L5 标注无专属 MA2
- [x] UC-CT-01/02/05/06/07/09 有符合性结论且列明 §2 判据编号；P1 项核 owner doc/plan Deferred/Non-Goal 标注的人工批准痕迹；触及 ORM/会计过账保护区域项显式标注 ask-first

### Phase 2 - A1.46 contract-F2 计费与返利五级追踪矩阵（UC-CT-03/04/08/10）+ finding 登记 / arm-index / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-05-1400-1-rc-ma1-a1-45-46-contract-lifecycle-billing-rebate.md`（补 §1-§5 for UC-CT-03/04/08/10 + §6-§9 全域汇总）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 1 完成

- [x] `Proof` 对 UC-CT-03/04/08/10 **逐验收标准一矩阵行**填 L1-L5：L1 逐字引用 `use-cases.md:47/69/146/170`；L3 引用 `ErpCtInvoicePlanBizModel`+`ErpCtInvoicePlanTriggerInvoiceProcessor`/`TriggerDuePlansProcessor` + `ErpCtConsumptionLineBizModel` + `ErpCtVolumeDiscountBizModel`/`ErpCtRebateAgreementBizModel`/`ErpCtRebateTierBizModel`/`ErpCtRebateAccrualBizModel`/`ErpCtRebateSettlementBizModel` + `ErpCtRebateAgreementRunAccrualProcessor`/`ErpCtRebateSettlementPostSettlementProcessor` + `ErpCtDocumentBizModel` + grep finance invoice facade/consume summary/OCR/fullText/legalHold/retention 站点；L4 引用 `TestErpCtContractRebate`/`TestErpCtRebateSettlementEnd`/`TestErpCtContractPosting`#method。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**：UC-CT-03 **按 invoiceTerm 批量生成/到达 planDate 自动生成 AP-AR Invoice 草稿（调 finance）/SUSPENDED 拦截/已开票禁改金额**（⚠️ grep finance invoice facade 待核跨域）；UC-CT-04 **周期汇总对比预估/超量 InvoicePlan/超 120% 审批通知**（⚠️待核候选 P1/P2）；UC-CT-08 **订单引用折扣率匹配/发票过账记录 Accrual/层级跨越追溯补差/Settlement 生成信用单/退货冲销层级回落**（⚠️待核跨域 purchase/sales/finance）；UC-CT-10 **OCR 识别/提取→ocrText+fullTextSearch/全文搜索+高级过滤/保留策略归档销毁/Legal Hold**（⚠️ grep ocr/fullText/legalHold/retention 待核候选 P1/P2）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 UC-CT-03/04/08/10 给出符合性结论（取最高）。UC-CT-03/08 倾向**接受/P2**（Processor 完整但跨域+边界待核）；UC-CT-04 候选 **P1/P2**；UC-CT-10 候选 **P1/P2**（**§4 三判据关键裁决**：若有 owner doc Deferred/Non-Goal 标注须核人工批准痕迹）。每结论须列明 §2 判据编号 + 三源对照 + §4 三判据复核 + 触及保护区域标注（返利 Settlement Credit Memo 过账属会计过账类 ask-first；OCR/全文索引若需 ORM 字段属 ORM 结构类 ask-first）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` contract lifecycle/amend/rebate/invoicePlan/signature/approval/document/OCR 同域同控制点后裁决。执行时 grep arm-index 取最新续编号避免冲突（当前至 P2-RC-059 / P1-RC-071）。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段 + §7 静态存疑点清单（供 MA4 展开，逐存疑点一行；**P0 即时通道评估**）+ §8 过程纪律自检段（实际运行 checker 附 actual vs baseline 表；closure-audit 独立性声明；交叉去重声明；**不以 checker 退出码 0 为门控通过依据**）+ §9 与 MA2 报告差异增量声明（无 contract 专属 MA2 报告）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 RC finding 入 RC 发现追踪分区；audit reports 表新增 A1.45+A1.46 行（contract 域首审行——10 UC 全覆盖）。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告全域 §1-§9 已落盘，9 段齐全；UC-CT-01~10 共 10 UC 矩阵行（逐验收标准，无跳号）；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.2 展开）；P0 候选评估有结论
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02d749a80ffeUYJHytuCZQ0nLo，fresh session，未起草本计划）。范围/UC 覆盖（A1.45=UC-CT-01/02/05/06/07/09 + A1.46=UC-CT-03/04/08/10，10 UC 无跳号）/依赖（0.2 done）/结果表面/rule-14 合并正当性（同一 owner doc + 同一结果表面）/方法论（9 段 §6 + §4 三判据 + §5 ask-first + §7 reuse + §去重协议）/反 slack/模板全 PASS；load-bearing 引用经实仓复核 CONFIRMED TRUE：①ErpCtContractBizModel#activate:67/amend:130/expire:118/terminate:97 ✅；②13 BizModel + 11 Processor 全部存在 ✅；③UC 清单与 roadmap+baseline-inventory+use-cases.md 三方一致 ✅；④arm-index 无 UC-CT-01~10 finding ✅。修复 3 处 minor：TestErpCtRebate→TestErpCtContractRebate 文件名、Non--Goals 拼写、UC-CT-10 purgeDate→delete 补数据删除类 ask-first。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.45+A1.46 报告 9 段齐全 + UC-CT-01~10 矩阵行（逐验收标准，10 UC 无跳号）+ finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.45/A1.46 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留作未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；本切片候选偏差多为**代码逻辑**类（预授权——到期提醒 job/分级通知/续期草稿/审批节点生成/消耗汇总/超量审批/OCR/全文检索）；**返利 Settlement Credit Memo 过账属会计过账核心路径，须 ask-first + 独立 plan-audit**；**OCR/全文索引若需 ORM 结构变更（ErpCtDocument 加索引字段）须 ask-first + 独立 plan-audit**；跨域契约（UC-CT-03 finance Invoice / UC-CT-08 purchase-sales 折扣-finance 信用单）修复须跨域协调。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-CT-03/08 跨域修复须与 finance/purchase/sales 协调）

## Closure

Status Note: 执行完成（2026-08-05）。两 Phase 全 done：Phase 1（A1.45 contract-F1 生命周期与签署 UC-CT-01/02/05/06/07/09 §1-§5）+ Phase 2（A1.46 contract-F2 计费与返利 UC-CT-03/04/08/10 §1-§5 + §6-§9 全域汇总 + arm-index 登记）。报告 `docs/audits/2026-08-05-1400-1-rc-ma1-a1-45-46-contract-lifecycle-billing-rebate.md` 9 段齐全，10 UC 逐验收标准五级追踪矩阵无跳号。结论：8 新 P1（P1-RC-072 创建校验/v1/预算 + P1-RC-073 amend 行复制/驳回恢复 + P1-RC-074 invoiceTerm 生成/已开票锁 + P1-RC-075 消耗计费引擎 + P1-RC-076 终止编排 + P1-RC-077 审批工作流引擎 + P1-RC-078 折扣订单侧 wiring + P1-RC-079 文档仓库引擎/legalHold ORM）+ 1 reuse（P1-MA2-071 到期自动化 resolved R1.22 via deferral，§4 三判据裁决 deferral 不成立 L1 仍活跃）+ UC-CT-09 签章接受（核心生命周期完整）。零 P0。arm-index 已更新（audit reports 表 + RC 发现追踪分区 8 新行 + A1.45+A1.46 交叉引用注记 + P1-MA2-071 RC 复用注记）。本审计为只读（零代码/ORM/api.xml/真相源变更），checker 退出码 0 纯 reporter 无回归风险。待独立结束审计子代理（新会话）执行 Closure Gate 最后一项。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor，fresh session，未起草/执行本计划）
- Evidence: 实仓复核（2026-08-05）全部 CONFIRMED：①报告 `docs/audits/2026-08-05-1400-1-rc-ma1-a1-45-46-contract-lifecycle-billing-rebate.md`（61732 字节）§1-§9 9 段齐全，10 UC（UC-CT-01~10）逐验收标准五级追踪矩阵无跳号；②`docs/audits/arm-index.md:112` audit reports 表新增 A1.45+A1.46 行（contract 域首审行）+ `:249-256` 8 新 P1 finding（P1-RC-072~079 续编无冲突）+ `:469` P1-MA2-071 reuse 交叉引用注记（追加 RC A1.45 不新建）+ `:268` A1.45+A1.46 交叉引用注记；③`docs/backlog/requirement-compliance-roadmap.md:84-85` A1.45+A1.46 todo→done；④Plan checker strict 模式 PASS（22 checked / 0 unchecked）；⑤反 hollow：报告 grep 实仓站点/行号/方法锚点均有命中（如 defaultPrepareSave:57 / amend:54-60 / triggerInvoice:46-67 / 17 行 CRUD 桩 + grep sumConsumption|overage|120|ocr|fullText|legalHold|matchMatrix|rejectCount|72h 跨 main 零业务命中），findings 非空泛；⑥文本一致性：Plan Status=completed / 两 Phase Status=completed / 两 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / 日志条目一致；⑦deferred honesty：仅 finding 修复入 Deferred But Adjudicated（out-of-scope improvement，分类正确——本计划是审计不修复），无活缺陷隐藏；⑧Docs sync：`docs/logs/2026/08-05.md` 增 A1.45+A1.46 contract 全域审计日志条目。零代码/ORM/api.xml/真相源变更（只读审计），checker 退出码 0 纯 reporter 无回归风险。本审计为 contract 域 MA1 全覆盖收尾，解除 MA4（A4.1/A4.2 SP-1~SP-8）及 MR1（R1.0 → RC-R1.n）链路的 contract 域全域证据缺口。

Follow-up:

- MR0/MR1 按 §10 展开本报告 finding 修复（会计过账类[返利 Settlement]/ORM 结构类[OCR 索引]须 ask-first + 跨域 finance/purchase/sales 协调）。
- MA4 运行时探针展开 §7 静态存疑点清单。
