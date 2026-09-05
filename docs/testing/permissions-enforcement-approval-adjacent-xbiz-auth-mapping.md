# 审批邻接 xbiz mutation auth 映射表（plan 2026-08-25-1956-3 Phase 1 Proof）

> 生成：2026-09-05（活仓核验）。39 文件 × 3 mutation = 117 处，修复前全部缺 `<auth>`（fail-open 旁路面）。
> 映射基线：`submitForApproval`→`<Entity>:mutation`（编制者轴，生成 FNPT）；`reject`→`<Entity>:approve`（审批者轴，delta FNPT）；`withdrawApproval`→`<Entity>:reverseApprove`（撤审轴，delta FNPT）。
> 例外：`ErpCsTicket`（cs）`:approve`/`:reverseApprove` 未声明（其既有 approve/reverseApprove `<auth>` 已引用未声明权限，ErpCsTicket.xbiz:29,80）——按计划预裁决映射到未声明权限点 = fail-closed admin-only，非 fail-open；successor 触发条件 = cs 域引入业务角色/FNPT 种子时一并声明 `ErpCsTicket:approve`/`:reverseApprove` 并补角色映射。
> 语义核验：三动作状态机均为标准审批轴（submitForApproval UNSUBMITTED/REJECTED→SUBMITTED；reject SUBMITTED→REJECTED；withdrawApproval 经 AbstractWithdrawApprovalProcessor 或 inline source 撤回→UNSUBMITTED），39 实体无状态机例外，无越权放大。
> 种子现状（正/负向抽样依据）：`:mutation` 全 39 实体零角色种子（submitForApproval 对业务角色 fail-closed，仅 admin 经 skip-check 通过——映射本身不新增种子，属计划 Non-Goal 边界内既有姿态）；`:approve` 38 实体有种子（pur/sal=审核人，ast=资产管理员,管理员，mfg=生产主管，prj=项目经理，qa=质量主管，fin=财务员，mnt=维护主管，inv=库管员，hr=薪酬审批人）；`:reverseApprove` 38 实体种子=管理员。

| # | 域 | 实体 | xbiz 文件 | submitForApproval → | reject → | withdrawApproval → |
|---|----|------|-----------|--------------------|----------|-------------------|
| 1 | pur | ErpPurInvoice | `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/model/ErpPurInvoice/ErpPurInvoice.xbiz` | `ErpPurInvoice:mutation` | `ErpPurInvoice:approve` | `ErpPurInvoice:reverseApprove` |
| 2 | pur | ErpPurOrder | `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/model/ErpPurOrder/ErpPurOrder.xbiz` | `ErpPurOrder:mutation` | `ErpPurOrder:approve` | `ErpPurOrder:reverseApprove` |
| 3 | pur | ErpPurPayment | `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/model/ErpPurPayment/ErpPurPayment.xbiz` | `ErpPurPayment:mutation` | `ErpPurPayment:approve` | `ErpPurPayment:reverseApprove` |
| 4 | pur | ErpPurQuotation | `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/model/ErpPurQuotation/ErpPurQuotation.xbiz` | `ErpPurQuotation:mutation` | `ErpPurQuotation:approve` | `ErpPurQuotation:reverseApprove` |
| 5 | pur | ErpPurReceive | `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/model/ErpPurReceive/ErpPurReceive.xbiz` | `ErpPurReceive:mutation` | `ErpPurReceive:approve` | `ErpPurReceive:reverseApprove` |
| 6 | pur | ErpPurRequisition | `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/model/ErpPurRequisition/ErpPurRequisition.xbiz` | `ErpPurRequisition:mutation` | `ErpPurRequisition:approve` | `ErpPurRequisition:reverseApprove` |
| 7 | pur | ErpPurReturn | `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/model/ErpPurReturn/ErpPurReturn.xbiz` | `ErpPurReturn:mutation` | `ErpPurReturn:approve` | `ErpPurReturn:reverseApprove` |
| 8 | pur | ErpPurRfq | `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/model/ErpPurRfq/ErpPurRfq.xbiz` | `ErpPurRfq:mutation` | `ErpPurRfq:approve` | `ErpPurRfq:reverseApprove` |
| 9 | sal | ErpSalContract | `module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/model/ErpSalContract/ErpSalContract.xbiz` | `ErpSalContract:mutation` | `ErpSalContract:approve` | `ErpSalContract:reverseApprove` |
| 10 | sal | ErpSalDelivery | `module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/model/ErpSalDelivery/ErpSalDelivery.xbiz` | `ErpSalDelivery:mutation` | `ErpSalDelivery:approve` | `ErpSalDelivery:reverseApprove` |
| 11 | sal | ErpSalInvoice | `module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/model/ErpSalInvoice/ErpSalInvoice.xbiz` | `ErpSalInvoice:mutation` | `ErpSalInvoice:approve` | `ErpSalInvoice:reverseApprove` |
| 12 | sal | ErpSalOrder | `module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/model/ErpSalOrder/ErpSalOrder.xbiz` | `ErpSalOrder:mutation` | `ErpSalOrder:approve` | `ErpSalOrder:reverseApprove` |
| 13 | sal | ErpSalQuotation | `module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/model/ErpSalQuotation/ErpSalQuotation.xbiz` | `ErpSalQuotation:mutation` | `ErpSalQuotation:approve` | `ErpSalQuotation:reverseApprove` |
| 14 | sal | ErpSalReceipt | `module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/model/ErpSalReceipt/ErpSalReceipt.xbiz` | `ErpSalReceipt:mutation` | `ErpSalReceipt:approve` | `ErpSalReceipt:reverseApprove` |
| 15 | sal | ErpSalReturn | `module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/model/ErpSalReturn/ErpSalReturn.xbiz` | `ErpSalReturn:mutation` | `ErpSalReturn:approve` | `ErpSalReturn:reverseApprove` |
| 16 | ast | ErpAstAssetCapitalization | `module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/model/ErpAstAssetCapitalization/ErpAstAssetCapitalization.xbiz` | `ErpAstAssetCapitalization:mutation` | `ErpAstAssetCapitalization:approve` | `ErpAstAssetCapitalization:reverseApprove` |
| 17 | ast | ErpAstDisposal | `module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/model/ErpAstDisposal/ErpAstDisposal.xbiz` | `ErpAstDisposal:mutation` | `ErpAstDisposal:approve` | `ErpAstDisposal:reverseApprove` |
| 18 | ast | ErpAstMerge | `module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/model/ErpAstMerge/ErpAstMerge.xbiz` | `ErpAstMerge:mutation` | `ErpAstMerge:approve` | `ErpAstMerge:reverseApprove` |
| 19 | ast | ErpAstMovement | `module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/model/ErpAstMovement/ErpAstMovement.xbiz` | `ErpAstMovement:mutation` | `ErpAstMovement:approve` | `ErpAstMovement:reverseApprove` |
| 20 | ast | ErpAstSplit | `module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/model/ErpAstSplit/ErpAstSplit.xbiz` | `ErpAstSplit:mutation` | `ErpAstSplit:approve` | `ErpAstSplit:reverseApprove` |
| 21 | ast | ErpAstValueAdjustment | `module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/model/ErpAstValueAdjustment/ErpAstValueAdjustment.xbiz` | `ErpAstValueAdjustment:mutation` | `ErpAstValueAdjustment:approve` | `ErpAstValueAdjustment:reverseApprove` |
| 22 | mfg | ErpMfgMaterialIssue | `module-manufacturing/erp-mfg-service/src/main/resources/_vfs/erp/mfg/model/ErpMfgMaterialIssue/ErpMfgMaterialIssue.xbiz` | `ErpMfgMaterialIssue:mutation` | `ErpMfgMaterialIssue:approve` | `ErpMfgMaterialIssue:reverseApprove` |
| 23 | mfg | ErpMfgSubcontractOrder | `module-manufacturing/erp-mfg-service/src/main/resources/_vfs/erp/mfg/model/ErpMfgSubcontractOrder/ErpMfgSubcontractOrder.xbiz` | `ErpMfgSubcontractOrder:mutation` | `ErpMfgSubcontractOrder:approve` | `ErpMfgSubcontractOrder:reverseApprove` |
| 24 | mfg | ErpMfgWorkOrder | `module-manufacturing/erp-mfg-service/src/main/resources/_vfs/erp/mfg/model/ErpMfgWorkOrder/ErpMfgWorkOrder.xbiz` | `ErpMfgWorkOrder:mutation` | `ErpMfgWorkOrder:approve` | `ErpMfgWorkOrder:reverseApprove` |
| 25 | prj | ErpPrjBilling | `module-projects/erp-prj-service/src/main/resources/_vfs/erp/prj/model/ErpPrjBilling/ErpPrjBilling.xbiz` | `ErpPrjBilling:mutation` | `ErpPrjBilling:approve` | `ErpPrjBilling:reverseApprove` |
| 26 | prj | ErpPrjBudget | `module-projects/erp-prj-service/src/main/resources/_vfs/erp/prj/model/ErpPrjBudget/ErpPrjBudget.xbiz` | `ErpPrjBudget:mutation` | `ErpPrjBudget:approve` | `ErpPrjBudget:reverseApprove` |
| 27 | prj | ErpPrjCostCollection | `module-projects/erp-prj-service/src/main/resources/_vfs/erp/prj/model/ErpPrjCostCollection/ErpPrjCostCollection.xbiz` | `ErpPrjCostCollection:mutation` | `ErpPrjCostCollection:approve` | `ErpPrjCostCollection:reverseApprove` |
| 28 | qa | ErpQaCalibration | `module-quality/erp-qa-service/src/main/resources/_vfs/erp/qa/model/ErpQaCalibration/ErpQaCalibration.xbiz` | `ErpQaCalibration:mutation` | `ErpQaCalibration:approve` | `ErpQaCalibration:reverseApprove` |
| 29 | qa | ErpQaInspection | `module-quality/erp-qa-service/src/main/resources/_vfs/erp/qa/model/ErpQaInspection/ErpQaInspection.xbiz` | `ErpQaInspection:mutation` | `ErpQaInspection:approve` | `ErpQaInspection:reverseApprove` |
| 30 | qa | ErpQaRecall | `module-quality/erp-qa-service/src/main/resources/_vfs/erp/qa/model/ErpQaRecall/ErpQaRecall.xbiz` | `ErpQaRecall:mutation` | `ErpQaRecall:approve` | `ErpQaRecall:reverseApprove` |
| 31 | qa | ErpQaReview | `module-quality/erp-qa-service/src/main/resources/_vfs/erp/qa/model/ErpQaReview/ErpQaReview.xbiz` | `ErpQaReview:mutation` | `ErpQaReview:approve` | `ErpQaReview:reverseApprove` |
| 32 | qa | ErpQaSpcChart | `module-quality/erp-qa-service/src/main/resources/_vfs/erp/qa/model/ErpQaSpcChart/ErpQaSpcChart.xbiz` | `ErpQaSpcChart:mutation` | `ErpQaSpcChart:approve` | `ErpQaSpcChart:reverseApprove` |
| 33 | fin | ErpFinEmployeeAdvance | `module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/model/ErpFinEmployeeAdvance/ErpFinEmployeeAdvance.xbiz` | `ErpFinEmployeeAdvance:mutation` | `ErpFinEmployeeAdvance:approve` | `ErpFinEmployeeAdvance:reverseApprove` |
| 34 | fin | ErpFinExpenseClaim | `module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/model/ErpFinExpenseClaim/ErpFinExpenseClaim.xbiz` | `ErpFinExpenseClaim:mutation` | `ErpFinExpenseClaim:approve` | `ErpFinExpenseClaim:reverseApprove` |
| 35 | mnt | ErpMntCalibration | `module-maintenance/erp-mnt-service/src/main/resources/_vfs/erp/mnt/model/ErpMntCalibration/ErpMntCalibration.xbiz` | `ErpMntCalibration:mutation` | `ErpMntCalibration:approve` | `ErpMntCalibration:reverseApprove` |
| 36 | mnt | ErpMntRequest | `module-maintenance/erp-mnt-service/src/main/resources/_vfs/erp/mnt/model/ErpMntRequest/ErpMntRequest.xbiz` | `ErpMntRequest:mutation` | `ErpMntRequest:approve` | `ErpMntRequest:reverseApprove` |
| 37 | cs | ErpCsTicket | `module-cs/erp-cs-service/src/main/resources/_vfs/erp/cs/model/ErpCsTicket/ErpCsTicket.xbiz` | `ErpCsTicket:mutation` | `ErpCsTicket:approve`（例外：未声明→fail-closed admin-only） | `ErpCsTicket:reverseApprove`（例外：未声明→fail-closed admin-only） |
| 38 | hr | ErpHrSalary | `module-hr/erp-hr-service/src/main/resources/_vfs/erp/hr/model/ErpHrSalary/ErpHrSalary.xbiz` | `ErpHrSalary:mutation` | `ErpHrSalary:approve` | `ErpHrSalary:reverseApprove` |
| 39 | inv | ErpInvCostAdjust | `module-inventory/erp-inv-service/src/main/resources/_vfs/erp/inv/model/ErpInvCostAdjust/ErpInvCostAdjust.xbiz` | `ErpInvCostAdjust:mutation` | `ErpInvCostAdjust:approve` | `ErpInvCostAdjust:reverseApprove` |

## 核验结论（Phase 1 Decision 三点）

- **(a) 权限点声明核验**：38/39 实体三权限点齐备（`:mutation`=生成 `_erp-*.action-auth.xml`；`:approve`/`:reverseApprove`=delta `erp-*.action-auth.xml`）；唯一例外 `ErpCsTicket` 无 `:approve`/`:reverseApprove` 声明，按计划预裁决处理（未声明 = 无人可持有 = fail-closed，与该实体既有 approve/reverseApprove `<auth>` 姿态一致，不产生 fail-open）。
- **(b) 状态机语义核验**：39 实体三动作无轴归属例外（脚本态 inline source 与 Processor 态 AbstractWithdrawApprovalProcessor 语义一致：撤审/撤回轴），维持基线映射，无单独记录项。
- **(c) 越权放大核验**：映射均为同实体审批轴对应权限点（编制者轴→mutation、审批者轴→approve、撤审轴→reverseApprove），不产生「提交者即可自审」放大；SoD 守卫正交（R2-E-stack-1 独立工作项）。替代方案（新增 117 FNPT + 种子）按计划 Non-Goals 否决。

## 声明核验机读校验：PASS（39 实体全部映射权限点声明核验通过，例外按预裁决登记）
