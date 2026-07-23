# 硬编码状态字面量权威清单 + 语义轴判定

> Source: `docs/plans/2026-07-24-0605-2-hardcoded-status-literal-constant-convergence.md` Phase 1
> Produced: 2026-07-24（实时仓库核实，仅 `module-*/erp-*-service/src/main/java` 生产代码）
> 方法：`rg -n '"(SUBMITTED|DRAFT|APPROVED|CANCELLED|REJECTED|UNSUBMITTED|ACTIVE)"'` 全 9 域 service 层

## 判定口径

- **REPLACE（替换框）**：字面量确属本域 doc-status 或 approve-status 轴（比较目标 / setter / 查询过滤 / 错误消息期望状态显示 / 本地重复常量定义），替换为 `Erp*DocStatus.<CONST>`（经 `Erp*Constants.<CONST>` 继承或直接 import dao 常量）。
- **EXCLUDE（排除集）**：异语义轴（visit-status / recall-status / posted-status / schema-active / ticket-status / voucher-status / budget-status / lot-status 等）、mfg 域 doc-status 轴（绑域专属字典，Non-Goal）、跨域镜像常量定义（已在 Erp*Constants 封装）、其他轴常量定义、组合显示串（如 `"UNSUBMITTED 或 REJECTED"`）。

## A. REPLACE 替换框（按域分批）

### purchase（ErpPurDocStatus via ErpPurConstants）

| file:line | 字面量 | 上下文 | 替换为 |
|---|---|---|---|
| ErpPurOrderProcessor.java:138,145,152 | "SUBMITTED" | illegalTransition expected（approve 轴） | APPROVE_STATUS_SUBMITTED |
| ErpPurOrderProcessor.java:159 | "APPROVED" | illegalTransition expected（approve 轴） | APPROVE_STATUS_APPROVED |
| ErpPurRequisitionProcessor.java:125,132,139 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpPurRequisitionProcessor.java:146 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpPurInvoiceProcessor.java:156,163,170 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpPurInvoiceProcessor.java:177 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpPurReceiveProcessor.java:148,155,162 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpPurReceiveProcessor.java:169 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpPurReturnProcessor.java:150,157,164 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpPurReturnProcessor.java:171 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpPurPaymentProcessor.java:161,168,175 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpPurPaymentProcessor.java:182 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |

### sales（ErpSalDocStatus via ErpSalConstants）

| file:line | 字面量 | 上下文 | 替换为 |
|---|---|---|---|
| ErpSalOrderProcessor.java:123,130,137 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpSalOrderProcessor.java:144 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpSalQuotationProcessor.java:135,142,149 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpSalQuotationProcessor.java:156,170 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpSalDeliveryProcessor.java:150,157,164 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpSalDeliveryProcessor.java:171 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpSalInvoiceProcessor.java:136,143,150 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpSalInvoiceProcessor.java:157 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpSalReturnProcessor.java:149,156,163 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpSalReturnProcessor.java:170 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpSalReceiptProcessor.java:139,146,153 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpSalReceiptProcessor.java:160 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |

### finance（ErpFinDocStatus via ErpFinConstants）

| file:line | 字面量 | 上下文 | 替换为 |
|---|---|---|---|
| ErpFinExpenseClaimProcessor.java:115,122,129 | "SUBMITTED" | illegalTransition expected（approve 轴） | APPROVE_STATUS_SUBMITTED |
| ErpFinExpenseClaimProcessor.java:136 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpFinEmployeeAdvanceProcessor.java:95,102,109 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpFinEmployeeAdvanceProcessor.java:116 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpFinBadDebtProcessor.java:241 | "UNSUBMITTED" | illegalTransition expected（approve 轴） | APPROVE_STATUS_UNSUBMITTED |
| ErpMdEmployeeReferenceCheckerImpl.java:53 | "CANCELLED" | ne("docStatus",...) 查询过滤（doc 轴，ErpFinEmployeeAdvance） | DOC_STATUS_CANCELLED |

### inventory（ErpInvDocStatus via ErpInvConstants）

| file:line | 字面量 | 上下文 | 替换为 |
|---|---|---|---|
| ErpInvCostAdjustProcessor.java:44-47 | UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | 本地重复常量定义（与 dao 层 ErpInvDocStatus 重复） | 删除本地定义，引用 ErpInvConstants.APPROVE_STATUS_* |
| ErpInvCostAdjustProcessor.java:185,192,199 | "SUBMITTED" | illegalTransition expected（approve 轴） | APPROVE_STATUS_SUBMITTED |
| ErpInvCostAdjustProcessor.java:206 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpInvLandedCostProcessor.java:55-58 | UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | 本地重复常量定义 | 删除本地定义，引用 ErpInvConstants.APPROVE_STATUS_* |
| ErpInvLandedCostProcessor.java:284 | "CANCELLED" | ne("docStatus",...) 查询过滤（doc 轴，ErpInvLandedCost） | DOC_STATUS_CANCELLED |
| ErpInvStockMoveProcessor.java:191 | "DRAFT" | ARG_EXPECTED_STATUS 错误参数（doc 轴，比较目标 DOC_STATUS_DRAFT） | DOC_STATUS_DRAFT |

### assets（ErpAstDocStatus via ErpAstConstants）

| file:line | 字面量 | 上下文 | 替换为 |
|---|---|---|---|
| ErpAstAssetCapitalizationProcessor.java:135,142,149 | "SUBMITTED" | illegalTransition expected（approve 轴） | APPROVE_STATUS_SUBMITTED |
| ErpAstAssetCapitalizationProcessor.java:156 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpAstDisposalProcessor.java:151,158,165 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpAstDisposalProcessor.java:172 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |
| ErpAstMergeProcessor.java:173,180,187 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpAstSplitProcessor.java:171,178,185 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpAstValueAdjustmentProcessor.java:141,148,155 | "SUBMITTED" | illegalTransition expected | APPROVE_STATUS_SUBMITTED |
| ErpAstValueAdjustmentProcessor.java:162 | "APPROVED" | illegalTransition expected | APPROVE_STATUS_APPROVED |

### quality（ErpQaDocStatus via ErpQaConstants；跨域 NcrReturnOrchestrator 用 pur/sal dao 常量）

| file:line | 字面量 | 上下文 | 替换为 |
|---|---|---|---|
| ErpQaRecallProcessor.java:84,91,98 | "SUBMITTED" | illegalTransition expected（approve 轴） | ErpQaConstants.APPROVE_STATUS_SUBMITTED |
| ErpQaRecallProcessor.java:105 | "APPROVED" | illegalTransition expected | ErpQaConstants.APPROVE_STATUS_APPROVED |
| NcrReturnOrchestrator.java:94 | "DRAFT" | data.put("docStatus",...) ErpPurReturn 创建（pur doc 轴） | ErpPurDocStatus.DOC_STATUS_DRAFT |
| NcrReturnOrchestrator.java:95 | "UNSUBMITTED" | data.put("approveStatus",...) ErpPurReturn（pur approve 轴） | ErpPurDocStatus.APPROVE_STATUS_UNSUBMITTED |
| NcrReturnOrchestrator.java:112 | "DRAFT" | data.put("docStatus",...) ErpSalReturn 创建（sal doc 轴） | ErpSalDocStatus.DOC_STATUS_DRAFT |
| NcrReturnOrchestrator.java:113 | "UNSUBMITTED" | data.put("approveStatus",...) ErpSalReturn（sal approve 轴） | ErpSalDocStatus.APPROVE_STATUS_UNSUBMITTED |

### manufacturing（仅 approve 轴；mfg doc 轴见排除集）

| file:line | 字面量 | 上下文 | 替换为 |
|---|---|---|---|
| ErpMfgWorkOrderProcessor.java:259,266,273 | "SUBMITTED" | illegalTransition expected（approve 轴，status=getApproveStatus） | ErpMfgConstants.APPROVE_STATUS_SUBMITTED |
| ErpMfgWorkOrderProcessor.java:280 | "APPROVED" | illegalTransition expected（approve 轴） | ErpMfgConstants.APPROVE_STATUS_APPROVED |
| ErpMfgSubcontractOrderProcessor.java:355,362,369 | "SUBMITTED" | illegalTransition expected（approve 轴） | ErpMfgConstants.APPROVE_STATUS_SUBMITTED |
| ErpMfgSubcontractOrderProcessor.java:376 | "APPROVED" | illegalTransition expected（approve 轴） | ErpMfgConstants.APPROVE_STATUS_APPROVED |
| MrpReleaseService.java:56-57,148,149 | PUR_DOC_STATUS_DRAFT/PUR_APPROVE_STATUS_UNSUBMITTED 本地副本 | ErpPurOrder 创建跨域本地常量副本（已 import pur-dao entity） | 删除本地副本，引用 ErpPurDocStatus.DOC_STATUS_DRAFT/APPROVE_STATUS_UNSUBMITTED |

**替换框合计**：~75 处字面量 + 6 处本地重复常量定义删除（inv×2 文件 8 常量 + mfg MrpRelease 2 常量）。

## B. EXCLUDE 排除集（异语义 / Non-Goal / 已封装常量定义）

### B1. 异语义轴（非 doc/approve-status）— 不替换

| file:line | 字面量 | 实际语义轴 | 排除依据 |
|---|---|---|---|
| ErpMntVisitBizModel.java:52 | "DRAFT" | visit-status（ErpMntDaoConstants.VISIT_STATUS_DRAFT 的显示串） | visit-status 轴，非 mnt doc/approve-status |
| SchemaPropagator.java:117 | "ACTIVE" | ErpMdAcctSchema.status 账套启用态（master-data active-status） | schema 启用语义，非 doc-status ACTIVE |
| MrpReleaseService.java:203 | "DRAFT" | ErpMfgSubcontractOrder.postedStatus 过账状态 | posted-status 轴，非 doc/approve-status |
| ErpQaRecallBizModel.java:123 | "APPROVED" | recall-status（RECALL_STATUS_APPROVED 显示串） | recall-status 轴，常量在 ErpQaConstants 非 ErpQaDocStatus |

### B2. mfg 域 doc-status 轴（Non-Goal：绑域专属字典 work-order/issue/subcontract/job-card）

| file:line | 字面量 | 语义轴 |
|---|---|---|
| ErpMfgWorkOrderProcessor.java:287 | "DRAFT" | WORK_ORDER_STATUS_DRAFT 显示串 |
| ErpMfgWorkOrderProcessor.java:291 | "SUBMITTED" | WORK_ORDER_STATUS_SUBMITTED 显示串 |
| ErpMfgJobCardProcessor.java:87 | "SUBMITTED" | JOB_CARD_STATUS_SUBMITTED 显示串 |
| ErpMfgSubcontractOrderProcessor.java:153 | "APPROVED" | SUBCONTRACT_STATUS_APPROVED 显示串 |
| ErpMfgMaterialIssueBizModel.java:96 | "DRAFT" | ISSUE_STATUS_DRAFT 显示串 |

### B3. Erp*Constants 中其他轴/跨域镜像的常量定义（已封装，非裸使用字面量）

- finance: AR_AP_STATUS_CANCELLED / RECON_STATUS_DRAFT / EMPLOYEE_STATUS_ACTIVE / VOUCHER_STATUS_DRAFT/CANCELLED / BUDGET_STATUS_*（budget 自有状态轴）
- assets: ASSET_STATUS_DRAFT / SCHEDULE_STATUS_CANCELLED / CIP_STATUS_DRAFT / INVENTORY_STATUS_DRAFT/CANCELLED / MAINTENANCE_STATUS_*
- inventory: OWNERSHIP_TRANSFER_STATUS_DRAFT/CANCELLED（ownership-transfer 自有状态轴）
- cs: TICKET_STATUS_CANCELLED
- quality: INSPECTION_RESULT_REJECTED / NCR_STATUS_CANCELLED / RECALL_STATUS_APPROVED/CANCELLED
- purchase: PARTNER_STATUS_ACTIVE / APPROVAL_STATUS_REJECTED（supplier-approval 轴）/ SCORECARD_STATUS_DRAFT
- sales: PARTNER_STATUS_ACTIVE
- mfg: COST_ROLLUP_STATUS_* / WORK_ORDER_STATUS_* / ISSUE_STATUS_* / JOB_CARD_STATUS_* / MRP_STATUS_* / FORECAST_STATUS_* / LOT_STATUS_* / SUBCONTRACT_STATUS_* / SIMULATION_STATUS_*（全部 mfg 域专属状态轴常量定义）
- 跨域镜像：mfg SAL_DOC_STATUS_CANCELLED / qa SAL_DOC_STATUS_DRAFT + SAL_APPROVE_STATUS_UNSUBMITTED（已注释说明为避免上行 service 依赖的本地副本，属封装常量非裸字面量）

### B4. 组合显示串（非单一常量可表达）

`"UNSUBMITTED 或 REJECTED"` / `"非已作废"` / `"STOCK_RESERVED 或 STOCK_PARTIAL"` / `"SCHEDULED"` 等——错误消息期望状态的人读组合或异语义显示，保留原样。

## 残留风险

- `illegalTransition(entity, status, "LITERAL")` 第三参为错误消息期望状态显示串，替换为常量后值不变（常量即原字面量），错误消息文本逐字节不变，无 i18n/快照影响。
- NcrReturnOrchestrator 引入 `ErpPurDocStatus`/`ErpSalDocStatus` import——qa-service 已依赖 pur-dao/sal-dao entity（ErpPurReturn/ErpSalReturn），dao 常量 import 不新增模块依赖边。
- MrpReleaseService 引入 `ErpPurDocStatus` import——mfg-service 已依赖 pur-dao entity（ErpPurOrder），同上。
