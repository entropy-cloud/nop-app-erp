# 全域状态轴清单（M0.2 产物）

> 来源计划：`docs/plans/2026-08-12-0617-2-entity-state-machine-m0-2-inventory.md`
> 分类法权威：`docs/architecture/entity-state-machine-bean.md`（M0.1 契约 §3 三轴边界、§8 不适用场景、§9 CRUD 写入边界）
> 路线图：`docs/backlog/entity-state-machine-migration-roadmap.md`
> 真相源：各域 `module-<domain>/model/app-erp-<domain>.orm.xml`（只读）+ 17 域 `state-machine.md` + `master-data/README.md` + `notify/README.md`
> 审查方法：`docs/skills/state-machine-business-review-prompt.md`

## 0. 分类法与裁定规则

M0.1 契约 §3 + §8 定义四类分类：

| 分类 | 裁定 | 说明 |
|------|------|------|
| 独立业务状态轴 | **纳入** | 有命名业务动作的迁移矩阵（Processor/BizModel `@BizMutation`），表达实体在业务流中的位置 |
| 普通标志（ACTIVE/INACTIVE 等） | 排除 | 启停二态，无多步业务流转 |
| 技术处理状态 | 排除 | 系统派生/计算的状态（批次/序列号/预留/银行匹配/异常追踪/引擎批量写入），无命名业务动作矩阵 |
| 工作流状态（nop-wf） | 排除 | nop-wf 人机任务状态归工作流引擎，业务实体只接收审批结果 |

三轴边界（M0.1 §3）：`docStatus`/`approveStatus` 各自独立 Bean；`posted`（boolean）**一律排除为迁移轴**。

CRUD 写入边界（M0.1 §9）：选项 (c) 显式排除——全部状态字段 `insertable=true updatable=true`，无 `notUpload`；StateMachine Bean 是**命名动作矩阵的唯一权威**，通用 CRUD 写入不在矩阵运行时强制范围。

排除项登记义务：每个排除项记录理由 + 重开触发条件。

## 1. 候选穷举（全集清单）

扫描范围：19 个 `module-*/model/app-erp-*.orm.xml` 全部 `ext:dict` 状态字段 + `posted` + 无 dict 状态型字段。

### 1.1 跨域共享 dict

| dict | 定义位置 | 说明 |
|---|---|---|
| `wf/approve-status` | 平台 nop-wf | UNSUBMITTED/SUBMITTED/APPROVED/REJECTED；~30+ 实体共用 |
| `erp/doc-status` | 未在任一 orm.xml inline 或 dict.yaml 找到定义 | 被 purchase/sales/quality/assets/maintenance/cs ~20+ 实体引用；按 state-machine.md 推断值为 DRAFT/CANCELLED。**异常登记见 §5** |
| `erp-md/active-status` | master-data orm inline | ACTIVE/INACTIVE |

### 1.2 全集候选逐域裁定

每域穷举全部候选。裁定取值：`纳入` / `排除-标志` / `排除-技术` / `排除-wf` / `排除-posted` / `排除-审计日志` / `排除-CRUD-stub`。

#### master-data（裁定结论：1 纳入 / 10 排除）

10 个实体 status（erp-md/active-status = ACTIVE/INACTIVE）→ 全部**排除-标志**（README §启用/停用：「主数据没有多步业务流转」）。

| # | 实体 | 字段 | dict | 裁定 | 理由 |
|---|---|---|---|---|---|
| MD-1~10 | Material/Category/Warehouse/TaxRate/SettlementMethod/Employee/Subject/AcctSchema/Organization/CostCenter | status | erp-md/active-status | 排除-标志 | 启停二态 |
| **MD-11** | **ErpMdSupplierApproval** | **status** | **erp-md/supplier-approval-status** | **纳入** | 5 态（APPLIED/APPROVED/PROBATION/SUSPENDED/REJECTED），有命名 BizMutation + TestErpMdSupplierApprovalStateMachine |

> **master-data 裁定**：10 个 ACTIVE/INACTIVE 标志排除。唯一纳入 ErpMdSupplierApproval.status（供应商准入生命周期）。重开触发条件：master-data 增加多步审批流实体时重新裁定。

#### notify（裁定结论：0 纳入 / 2 排除）

| # | 实体 | 字段 | dict | 裁定 | 理由 |
|---|---|---|---|---|---|
| N-1 | ErpSysNotificationTemplate | status | erp-notify/template-status | 排除-标志 | DRAFT→ACTIVE 二态发布标志，标准 CRUD |
| N-2 | ErpSysNotification | status | erp-notify/notification-status | 排除-技术 | PENDING/SENT/MERGED/FAILED 由 Dispatcher 系统派生，无命名动作矩阵 |

> **notify 裁定**：notify 无业务迁移矩阵。template-status 是发布标志，notification-status 是系统派生处理记录。重开触发条件：增加人工审批通知派发流程时重新评估。

#### cs（裁定：1 纳入 / 6 排除）

| # | 实体 | 字段 | dict | 裁定 | 理由 |
|---|---|---|---|---|---|
| **CS-1** | **ErpCsTicket** | **status** | **erp-cs/ticket-status** | **纳入** | M1.1 试点 |
| CS-2 | ErpCsTicket | docStatus | erp/doc-status | 排除-技术 | Ticket 以 status 为主轴；docStatus 继承字段无独立矩阵 |
| CS-3 | ErpCsTicket | approveStatus | wf/approve-status | 排除-技术 | 同上 |
| CS-4/5 | ErpCsTicketAction | fromStatus/toStatus | erp-cs/ticket-status | 排除-审计日志 | 迁移历史记录 |
| CS-6 | ErpCsContract | status | erp-cs/contract-status | 排除-CRUD-stub | state-machine.md 未覆盖 |
| CS-7 | ErpCsTimeEntry | approvalStatus | erp-cs/time-entry-approve-status | 排除-CRUD-stub | 简单工时审批，未覆盖 |

#### purchase（裁定：16 纳入 / 排除项见下）

三轴模型：docStatus（DRAFT/CANCELLED）+ approveStatus（wf/approve-status 4 态）。各实体 SUBMITTED→APPROVED 触发下游因文档类型不同。

| # | 实体 | 字段 | 裁定 | 财务影响 | 理由 |
|---|---|---|---|---|---|
| PUR-1 | ErpPurOrder | docStatus | 纳入 | 无 | 最小生命周期 |
| PUR-2 | ErpPurOrder | approveStatus | 纳入 | 无 | approve 仅状态推进 |
| PUR-3 | ErpPurOrder | receiveStatus | 排除-技术 | — | 派生汇总 |
| PUR-4 | ErpPurOrder | posted | 排除-posted | — | |
| PUR-5/6 | ErpPurReceive | docStatus/approveStatus | 纳入 | **是**（approve→入库） | |
| PUR-7 | ErpPurReceive | receiveStatus | 排除-技术 | — | |
| PUR-8 | ErpPurReceive | posted | 排除-posted | — | |
| PUR-9/10 | ErpPurInvoice | docStatus/approveStatus | 纳入 | **是**（approve→AP凭证） | |
| PUR-11 | ErpPurInvoice | paidStatus | 排除-技术 | — | PaymentSettler 派生 |
| PUR-12 | ErpPurInvoice | posted | 排除-posted | — | |
| PUR-13/14 | ErpPurPayment | docStatus/approveStatus | 纳入 | **是**（approve→PAYMENT凭证） | |
| PUR-15 | ErpPurPayment | writtenOffStatus | 排除-技术 | — | 派生 |
| PUR-16 | ErpPurPayment | posted | 排除-posted | — | |
| PUR-17/18 | ErpPurReturn | docStatus/approveStatus | 纳入 | **是**（approve→出库+红字发票） | |
| PUR-19 | ErpPurReturn | posted | 排除-posted | — | |
| PUR-20/21 | ErpPurRequisition | docStatus/approveStatus | 纳入 | 无 | 无库存/凭证 |
| PUR-22/23 | ErpPurQuotation | docStatus/approveStatus | 纳入 | 无 | INLINE 路径 |
| PUR-24/25 | ErpPurRfq | docStatus/approveStatus | 纳入 | 无 | INLINE 路径 |
| PUR-26 | ErpPurSupplierScorecard | status | 排除-CRUD-stub | — | 评分卡 |

#### sales（裁定：12 纳入 / 排除项见下）

镜像 purchase 三轴模型。

| # | 实体 | 字段 | 裁定 | 财务影响 | 理由 |
|---|---|---|---|---|---|
| SAL-1 | ErpSalOrder | docStatus | 纳入 | 无 | 最小生命周期 |
| SAL-2 | ErpSalOrder | approveStatus | 纳入 | 无 | approve 仅状态推进（可用量只读预检） |
| SAL-3 | ErpSalOrder | deliveryStatus | 排除-技术 | — | 派生汇总 |
| SAL-4 | ErpSalOrder | receivedStatus | 排除-技术 | — | 派生汇总 |
| SAL-5 | ErpSalOrder | posted | 排除-posted | — | |
| SAL-6/7 | ErpSalDelivery | docStatus/approveStatus | 纳入 | **是**（approve→出库） | |
| SAL-8 | ErpSalDelivery | posted | 排除-posted | — | |
| SAL-9/10 | ErpSalInvoice | docStatus/approveStatus | 纳入 | **是**（approve→AR凭证） | |
| SAL-11 | ErpSalInvoice | receivedStatus | 排除-技术 | — | ReceiptSettler 派生 |
| SAL-12 | ErpSalInvoice | posted | 排除-posted | — | |
| SAL-13/14 | ErpSalReceipt | docStatus/approveStatus | 纳入 | **是**（approve→RECEIPT凭证） | |
| SAL-15 | ErpSalReceipt | writtenOffStatus | 排除-技术 | — | 派生 |
| SAL-16 | ErpSalReceipt | posted | 排除-posted | — | |
| SAL-17/18 | ErpSalReturn | docStatus/approveStatus | 纳入 | **是**（approve→入库+红字发票） | |
| SAL-19 | ErpSalReturn | posted | 排除-posted | — | |
| SAL-20/21 | ErpSalQuotation | docStatus/approveStatus | 纳入 | 无 | 无库存/凭证 |
| SAL-22/23 | ErpSalContract | docStatus/approveStatus | 排除-CRUD-stub | — | 合同主体归 contract 域 |

#### inventory（裁定：7 纳入 / 排除项见下）

| # | 实体 | 字段 | dict | 裁定 | 理由 |
|---|---|---|---|---|---|
| **INV-1** | ErpInvStockMove | docStatus | erp-inv/move-status | 纳入 | DONE 触发存货过账事件 |
| INV-2 | ErpInvStockMove | approveStatus | wf/approve-status | 排除-技术 | 以 docStatus 为主轴 |
| INV-3 | ErpInvStockMove | posted | — | 排除-posted | |
| INV-4 | ErpInvReservation | status | erp-inv/reservation-status | 排除-技术 | 派生 |
| **INV-5** | ErpInvTransferOrder | docStatus | erp-inv/move-status | 纳入 | DONE 触发库存移动 |
| INV-6/7 | ErpInvTransferOrder | approveStatus/posted | — | 排除 | |
| **INV-8** | ErpInvStockTake | docStatus | erp-inv/move-status | 纳入 | DONE 生成差异移动单 |
| INV-9/10 | ErpInvStockTake | approveStatus/posted | — | 排除 | |
| INV-11 | ErpInvPickingOrder | docStatus | erp-inv/picking-status | 排除-CRUD-stub | CrudBizModel stub；PICKING/PICKED 死状态 |
| INV-12 | ErpInvBatch | status | erp-inv/batch-status | 排除-技术 | 物理批次派生 |
| INV-13 | ErpInvSerialNumber | status | erp-inv/serial-status | 排除-技术 | 序列号派生 |
| **INV-14** | ErpInvOwnershipTransfer | docStatus | erp-inv/ownership-transfer-status | 纳入 | DONE 触发所有权转移 |
| INV-15 | ErpInvOwnershipTransfer | posted | — | 排除-posted | |
| **INV-16** | ErpInvCostAdjust | docStatus | erp-inv/move-status | 纳入 | DONE 触发成本调整过账 |
| INV-17/18 | ErpInvCostAdjust | approveStatus/posted | — | 排除 | |
| **INV-19** | ErpInvLandedCost | docStatus | erp-inv/move-status | 纳入 | DONE 触发成本分摊 |
| INV-20/21 | ErpInvLandedCost | approveStatus/posted | — | 排除 | |

#### finance（裁定：12 纳入 / 排除项见下）

| # | 实体 | 字段 | dict | 裁定 | 理由 |
|---|---|---|---|---|---|
| **FIN-1** | ErpFinVoucher | docStatus | erp-fin/voucher-status | 纳入 | 过账核心；CANCELLED 死状态 |
| **FIN-2** | ErpFinAccountingPeriod | status | erp-fin/period-status | 纳入 | 过账窗口控制 |
| FIN-3 | ErpFinAccountingPeriodStatus | ar/ap/inv/gl/assetStatus | erp-fin/module-close-status | 排除-技术 | 模块结账子状态派生 |
| FIN-4 | ErpFinArApItem | status | erp-fin/ar-ap-status | 排除-技术 | 派生 |
| **FIN-5** | ErpFinReconciliation | docStatus | erp-fin/reconciliation-status | 纳入 | 核销 |
| FIN-6 | ErpFinFundAccount | status | erp-fin/fund-account-status | 排除-标志 | 账户启停 |
| FIN-7/8/9 | BankStatement/BankStatementLine/BankReconciliation | docStatus/matchStatus | — | 排除-技术 | 银行对账系统派生 |
| **FIN-10/11** | ErpFinExpenseClaim | docStatus/approveStatus | erp-fin/expense-claim-status / wf | 纳入 | 报销凭证+SoD |
| FIN-12 | ErpFinExpenseClaim | posted | — | 排除-posted | |
| **FIN-13/14** | ErpFinEmployeeAdvance | docStatus/approveStatus | erp-fin/advance-status / wf | 纳入 | 预付款凭证+SoD |
| FIN-15 | ErpFinEmployeeAdvance | posted | — | 排除-posted | |
| **FIN-16** | ErpFinNotesReceivable | status | erp-fin/notes-receivable-status | 纳入 | 既有测试 |
| FIN-17 | ErpFinNotesReceivable | posted | — | 排除-posted | |
| **FIN-18** | ErpFinNotesPayable | status | erp-fin/notes-payable-status | 纳入 | 既有测试 |
| FIN-19 | ErpFinNotesPayable | posted | — | 排除-posted | |
| FIN-20 | ErpFinNotesDiscount | posted | — | 排除-posted | |
| FIN-21 | ErpFinCreditFacility | status | erp-fin/fund-account-status | 排除-标志 | |
| FIN-22 | ErpFinPostingException | status | erp-fin/posting-exception-status | 排除-技术 | 异常追踪 |
| **FIN-23** | ErpFinBadDebt | approveStatus | wf/approve-status | 纳入 | 坏账+ArApItem |
| **FIN-24/25** | ErpFinBudgetScenario | docStatus/approveStatus | erp-fin/budget-status / wf | 纳入 | 预算凭证 |
| FIN-26 | ErpFinIntercompanyMatch | status | erp-fin/intercompany-match-status | 排除-技术 | 一致性检查行 |
| FIN-27 | ErpFinConsolidationElimination | status | erp-fin/elimination-status | 排除-技术 | 抵销引擎派生 |

#### manufacturing（裁定：9 纳入 / 排除项见下）

| # | 实体 | 字段 | dict | 裁定 | 理由 |
|---|---|---|---|---|---|
| **MFG-1/2** | ErpMfgWorkOrder | docStatus/approveStatus | erp-mfg/work-order-status / wf | 纳入 | 完工凭证+SoD+既有测试 |
| MFG-3 | ErpMfgWorkOrder | posted | — | 排除-posted | |
| **MFG-4** | ErpMfgMrpPlan | status | erp-mfg/mrp-status | 纳入 | MRP 计算生命周期（CANCELLED 死状态） |
| **MFG-5** | ErpMfgForecast | status | erp-mfg/forecast-status | 纳入 | 预测（CONSUMED 死状态） |
| **MFG-6** | ErpMfgMaterialIssue | docStatus | erp-mfg/issue-status | 纳入 | confirm 触发出库+凭证 |
| MFG-7/8 | ErpMfgMaterialIssue | approveStatus/posted | — | 排除 | |
| **MFG-9/10** | ErpMfgSubcontractOrder | docStatus/approveStatus | erp-mfg/subcontract-status / wf | 纳入 | 委外加工费凭证+SoD |
| MFG-11/12 | ErpMfgSubcontractOrder | posted/postedStatus | — | 排除 | postedStatus dict 未找到 |
| MFG-13 | ErpMfgCostRollup | status | erp-mfg/cost-rollup-status | 排除-技术 | 引擎派生 |
| **MFG-14** | ErpMfgJobCard | status | erp-mfg/job-card-status | 纳入 | 8 态（2 死状态） |
| MFG-15 | ErpMfgCostVariance | posted | — | 排除-posted | |
| MFG-16 | ErpMfgBatchGenealogy | lotStatus | — | 排除-技术 | 自由 string |
| MFG-17/18 | ErpMfgMrpScenario/Version | status | erp-mfg/simulation-status | 排除-技术 | 仿真；dict 未找到 |

#### 其余域（crm/projects/quality/assets/maintenance/logistics/aps/hr/b2b/contract/drp）

裁定逻辑同上，逐域结论汇总于 §2 纳入轴全表。排除项统一理由：CRUD stub（零 writer）、技术派生（引擎/计算/物理状态）、审计日志字段、posted boolean、普通启停标志。

关键排除项补充说明：
- **projects**：ErpPrjMilestone（CRUD stub，4 态全死）、ErpPrjBilling（CRUD stub，5 态全死）、ErpPrjCostCollection（dict 漂移：写 APPROVED 但 dict 无此值 → successor 修 dict）、ErpPrjBudget（未覆盖）
- **quality**：ErpQaAction OVERDUE 死状态、ErpQaRiskRegister MITIGATED/CLOSED 死状态、ErpQaSpcChart STALE 死状态、ErpQaQualityGoal/Review/Calibration CRUD stub
- **hr**：ErpHrSurvey（CRUD stub 3 态全死）、ErpHrDevelopmentPlan CANCELLED 死、ErpHrDevelopmentPlanItem OVERDUE 死、ErpHrRecruitment/ShiftAssignment/ShiftSwapRequest/Simulation/BankFile 技术派生或未覆盖
- **assets**：ErpAstCip（系统派生）
- **b2b**：ErpB2bPartnerProfile（启停标志）、ErpB2bMftLog（传输日志）
- **contract**：ErpCtApprovalRecord（审计日志）、ErpCtSignatureRequest（电签 SPI 回调派生）、ErpCtDocument ocrStatus（自由 string）
- **drp**：CrossDock/DockAppointment/Scenario 技术派生
- **aps**：Schedule（引擎管理）、DispatchLog previousStatus/newStatus（审计日志）
- **logistics**：freightSettlementStatus（派生）、httpStatus（HTTP 日志）

### 1.3 候选穷举完整性确认

- 17 业务域 state-machine.md 全部扫描 ✓
- master-data/notify README 全部评估 ✓
- 19 orm.xml 全部 `ext:dict` 状态字段 + `posted` + 无 dict 状态 string 全部列出 ✓
- md ext-ref block INTEGER status（~30 个）全部排除 ✓
- 纳入/不纳入裁定覆盖全集 ✓

## 2. 纳入轴汇总与里程碑分配

### 2.1 计数

| 里程碑 | 轴数 | 判定规则 |
|--------|------|----------|
| M1.1 试点 | 1 | ErpCsTicket.status |
| M2 简单生命周期 | 19 | 非保护、无财务影响、简单生命周期 |
| M3 复杂业务/审批 | 19 | 无财务影响复杂业务或审批轴 |
| M4 财务影响/保护域 | 65 | finance 各轴 + 触发/冲销/补偿库存或会计过账的 action 所在轴 |
| **合计** | **103** | |

里程碑判定规则：
- **M4**：finance 全域 + 任何域中 SUBMITTED→APPROVED（或等效迁移）触发/逆转 `IErpFinVoucherBiz.post` / `IErpInvStockMoveBiz` / `IErpFinAcctDocProvider` 的轴。`posted` 本身不入表。
- **M3**：无财务影响的审批轴（approveStatus 不触发过账）或复杂业务生命周期（5+ 态/分支/回滚）。
- **M2**：非保护、无财务影响、简单生命周期（≤4 态线性流或 2 态最小生命周期）。

### 2.2 纳入轴全表

| 编号 | 域 | 实体 | 轴 | dict |
|---|---|---|---|---|
| M1.1 | cs | ErpCsTicket | status | erp-cs/ticket-status |
| M2.1 | master-data | ErpMdSupplierApproval | status | erp-md/supplier-approval-status |
| M2.2 | crm | ErpCrmEvent | status | erp-crm/event-status |
| M2.3 | projects | ErpPrjTask | status | erp-prj/task-status |
| M2.4 | projects | ErpPrjProject | status | erp-prj/project-status |
| M2.5 | purchase | ErpPurQuotation | docStatus | erp/doc-status |
| M2.6 | purchase | ErpPurRfq | docStatus | erp/doc-status |
| M2.7 | purchase | ErpPurRequisition | docStatus | erp/doc-status |
| M2.8 | purchase | ErpPurOrder | docStatus | erp/doc-status |
| M2.9 | sales | ErpSalQuotation | docStatus | erp/doc-status |
| M2.10 | sales | ErpSalOrder | docStatus | erp/doc-status |
| M2.11 | hr | ErpHrLeaveRequest | status | erp-hr/leave-status |
| M2.12 | hr | ErpHrEmploymentContract | status | erp-hr/contract-status |
| M2.13 | aps | ErpApsOperationOrder | status | erp-aps/operation-order-status |
| M2.14 | drp | ErpDrpPlan | status | erp-drp/drp-plan-status |
| M2.15 | drp | ErpDrpLine | status | erp-drp/drp-line-status |
| M2.16 | b2b | ErpB2bAsn | status | erp-b2b/asn-status |
| M2.17 | b2b | ErpB2bEdiDoc | state | erp-b2b/edi-doc-state |
| M2.18 | contract | ErpCtContract | status | erp-ct/contract-status |
| M2.19 | mfg | ErpMfgForecast | status | erp-mfg/forecast-status |
| M3.1 | crm | ErpCrmLead | docStatus | erp-crm/lead-doc-status |
| M3.2 | purchase | ErpPurQuotation | approveStatus | wf/approve-status |
| M3.3 | purchase | ErpPurRfq | approveStatus | wf/approve-status |
| M3.4 | purchase | ErpPurRequisition | approveStatus | wf/approve-status |
| M3.5 | purchase | ErpPurOrder | approveStatus | wf/approve-status |
| M3.6 | sales | ErpSalQuotation | approveStatus | wf/approve-status |
| M3.7 | sales | ErpSalOrder | approveStatus | wf/approve-status |
| M3.8 | hr | ErpHrEmployee | employmentStatus | erp-hr/employment-status |
| M3.9 | hr | ErpHrTimesheet | status | erp-hr/timesheet-status |
| M3.10 | projects | ErpPrjTimesheet | status | wf/approve-status |
| M3.11 | projects | ErpPrjProjectSettlement | docStatus | erp-prj/project-status |
| M3.12 | projects | ErpPrjProjectSettlement | approveStatus | wf/approve-status |
| M3.13 | mfg | ErpMfgJobCard | status | erp-mfg/job-card-status |
| M3.14 | mfg | ErpMfgMrpPlan | status | erp-mfg/mrp-status |
| M3.15 | assets | ErpAstMovement | docStatus | erp/doc-status |
| M3.16 | assets | ErpAstMovement | approveStatus | wf/approve-status |
| M3.17 | maintenance | ErpMntRequest | status | erp-mnt/request-status |
| M3.18 | contract | ErpCtContractVersion | status | erp-ct/version-status |
| M3.19 | contract | ErpCtRebateAgreement | status | erp-ct/rebate-agreement-status |
| M4.1 | finance | ErpFinVoucher | docStatus | erp-fin/voucher-status |
| M4.2 | finance | ErpFinAccountingPeriod | status | erp-fin/period-status |
| M4.3 | finance | ErpFinReconciliation | docStatus | erp-fin/reconciliation-status |
| M4.4 | finance | ErpFinExpenseClaim | docStatus | erp-fin/expense-claim-status |
| M4.5 | finance | ErpFinExpenseClaim | approveStatus | wf/approve-status |
| M4.6 | finance | ErpFinEmployeeAdvance | docStatus | erp-fin/advance-status |
| M4.7 | finance | ErpFinEmployeeAdvance | approveStatus | wf/approve-status |
| M4.8 | finance | ErpFinNotesReceivable | status | erp-fin/notes-receivable-status |
| M4.9 | finance | ErpFinNotesPayable | status | erp-fin/notes-payable-status |
| M4.10 | finance | ErpFinBadDebt | approveStatus | wf/approve-status |
| M4.11 | finance | ErpFinBudgetScenario | docStatus | erp-fin/budget-status |
| M4.12 | finance | ErpFinBudgetScenario | approveStatus | wf/approve-status |
| M4.13 | purchase | ErpPurReceive | docStatus | erp/doc-status |
| M4.14 | purchase | ErpPurReceive | approveStatus | wf/approve-status |
| M4.15 | purchase | ErpPurInvoice | docStatus | erp/doc-status |
| M4.16 | purchase | ErpPurInvoice | approveStatus | wf/approve-status |
| M4.17 | purchase | ErpPurPayment | docStatus | erp/doc-status |
| M4.18 | purchase | ErpPurPayment | approveStatus | wf/approve-status |
| M4.19 | purchase | ErpPurReturn | docStatus | erp/doc-status |
| M4.20 | purchase | ErpPurReturn | approveStatus | wf/approve-status |
| M4.21 | sales | ErpSalDelivery | docStatus | erp/doc-status |
| M4.22 | sales | ErpSalDelivery | approveStatus | wf/approve-status |
| M4.23 | sales | ErpSalInvoice | docStatus | erp/doc-status |
| M4.24 | sales | ErpSalInvoice | approveStatus | wf/approve-status |
| M4.25 | sales | ErpSalReceipt | docStatus | erp/doc-status |
| M4.26 | sales | ErpSalReceipt | approveStatus | wf/approve-status |
| M4.27 | sales | ErpSalReturn | docStatus | erp/doc-status |
| M4.28 | sales | ErpSalReturn | approveStatus | wf/approve-status |
| M4.29 | inventory | ErpInvStockMove | docStatus | erp-inv/move-status |
| M4.30 | inventory | ErpInvStockTake | docStatus | erp-inv/move-status |
| M4.31 | inventory | ErpInvTransferOrder | docStatus | erp-inv/move-status |
| M4.32 | inventory | ErpInvOwnershipTransfer | docStatus | erp-inv/ownership-transfer-status |
| M4.33 | inventory | ErpInvCostAdjust | docStatus | erp-inv/move-status |
| M4.34 | inventory | ErpInvLandedCost | docStatus | erp-inv/move-status |
| M4.35 | mfg | ErpMfgWorkOrder | docStatus | erp-mfg/work-order-status |
| M4.36 | mfg | ErpMfgWorkOrder | approveStatus | wf/approve-status |
| M4.37 | mfg | ErpMfgSubcontractOrder | docStatus | erp-mfg/subcontract-status |
| M4.38 | mfg | ErpMfgSubcontractOrder | approveStatus | wf/approve-status |
| M4.39 | mfg | ErpMfgMaterialIssue | docStatus | erp-mfg/issue-status |
| M4.40 | assets | ErpAstAsset | status | erp-ast/asset-status |
| M4.41 | assets | ErpAstDepreciationSchedule | status | erp-ast/depreciation-schedule-status |
| M4.42 | assets | ErpAstValueAdjustment | docStatus | erp/doc-status |
| M4.43 | assets | ErpAstValueAdjustment | approveStatus | wf/approve-status |
| M4.44 | assets | ErpAstDisposal | docStatus | erp/doc-status |
| M4.45 | assets | ErpAstDisposal | approveStatus | wf/approve-status |
| M4.46 | assets | ErpAstAssetCapitalization | docStatus | erp/doc-status |
| M4.47 | assets | ErpAstAssetCapitalization | approveStatus | wf/approve-status |
| M4.48 | assets | ErpAstSplit | docStatus | erp/doc-status |
| M4.49 | assets | ErpAstSplit | approveStatus | wf/approve-status |
| M4.50 | assets | ErpAstMerge | docStatus | erp/doc-status |
| M4.51 | assets | ErpAstMerge | approveStatus | wf/approve-status |
| M4.52 | assets | ErpAstInventory | status | erp-ast/inventory-status |
| M4.53 | assets | ErpAstMaintenance | status | erp-ast/maintenance-status |
| M4.54 | maintenance | ErpMntVisit | status | erp-mnt/visit-status |
| M4.55 | maintenance | ErpMntSparePartUsage | docStatus | erp/doc-status |
| M4.56 | maintenance | ErpMntSparePartUsage | approveStatus | wf/approve-status |
| M4.57 | logistics | ErpLogShipment | status | erp-log/shipment-status |
| M4.58 | quality | ErpQaInspection | docStatus | erp/doc-status |
| M4.59 | quality | ErpQaInspection | approveStatus | wf/approve-status |
| M4.60 | quality | ErpQaNonConformance | status | erp-qa/ncr-status |
| M4.61 | quality | ErpQaRecall | status | erp-qa/recall-status |
| M4.62 | quality | ErpQaRecall | approveStatus | wf/approve-status |
| M4.63 | hr | ErpHrSalary | paymentStatus | erp-hr/salary-payment-status |
| M4.64 | hr | ErpHrSalary | approveStatus | wf/approve-status |
| M4.65 | contract | ErpCtRebateSettlement | status | erp-ct/settlement-status |

## 3. 纳入轴八属性登记

> 每个纳入轴登记八属性：(1) 迁移语义摘要 (2) owner doc (3) ORM dict (4) 生产 writer (5) 既有测试 (6) 保护区域 (7) 财务影响 (8) 跨域副作用。
>
> **writer 盘点方法**（M0.2 纪律 + M0.1 §9.4）：从实际代码扫描 `setStatus`/`setDocStatus`/`setApproveStatus` 等写路径，区分**生产 writer**（命名 Processor/BizModel `@BizMutation`）、**框架入口**（标准 CRUD `__save`/`save`——经 M0.1 §9 裁定当前可写但不在矩阵运行时强制范围）、**测试 fixture**。**禁止沿用「149 Processor」历史统计**——本清单从实际 grep 重新建立范围。
>
> **关键发现**：全部 ~937 行状态写入均在命名业务动作路径（Processor `@BizMutation` 或 BizModel `@BizMutation`/`@BizQuery`，或其委派 support class）。**零 PROD-CRUD writer**（无 `defaultPrepareSave`/`__save` 写状态）——创建时写初始态在各 Processor/Converter 内。~149 个 `protected void setDocStatus/setApproveStatus(...)` delegation 方法是天然注入缝。

### 3.1 既有 StateMachine 测试基线（层 3 回归）

| # | 测试类 | 域 | 实体 |
|---|---|---|---|
| 1 | TestErpMdSupplierApprovalStateMachine | master-data | ErpMdSupplierApproval |
| 2 | TestErpMntVisitRequestStateMachine | maintenance | ErpMntVisit + ErpMntRequest |
| 3 | TestErpMfgWorkOrderStateMachine | manufacturing | ErpMfgWorkOrder |
| 4 | TestErpQaInspectionStateMachine | quality | ErpQaInspection |
| 5 | TestErpQaRecallStateMachine | quality | ErpQaRecall |
| 6 | TestErpFinPeriodStateMachine | finance | ErpFinAccountingPeriod |
| 7 | TestErpFinNotesReceivableStateMachine | finance | ErpFinNotesReceivable |
| 8 | TestErpFinNotesPayableStateMachine | finance | ErpFinNotesPayable |
| 9 | TestErpProbeStateMachineContract | cs | 合成探针（非 Ticket 实体，M1.1 落地真实 Bean） |

### 3.2 M1.1 试点 — ErpCsTicket.status

| 属性 | 值 |
|---|---|
| (1) 迁移语义 | NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED；RESOLVED→IN_PROGRESS(reopen)；非终态→CANCELLED |
| (2) owner doc | `docs/design/customer-service/state-machine.md` §1-2 + §实现约定 |
| (3) dict | `erp-cs/ticket-status`（`module-cs/model/app-erp-cs.orm.xml:165`） |
| (4) 生产 writer | `ErpCsTicketBizModel:112,127,158,186`（assign/start/close/cancel）、`ErpCsTicketResolveProcessor:51`、`ErpCsTicketReopenProcessor:42`。框架入口：`__save`/`save` 可写（M0.1 §9 选项 c） |
| (5) 既有测试 | TestErpProbeStateMachineContract（合成探针）；5 个 Ticket 集成测试 |
| (6) 保护区域 | **data-deletion**（reopen 删除未答问卷；用户 2026-08-06 批准保留既有行为） |
| (7) 财务影响 | 无 |
| (8) 跨域副作用 | SLA 超时→nop-job→escalation；resolve→CSAT 调查创建；quality/maintenance 弱指针联动 |

### 3.3 M2 轴属性（19 项）

| 编号 | (1) 迁移语义 | (2) owner doc | (3) dict | (4) 生产 writer | (5) 测试 | (6) 保护区 | (7) 财务 | (8) 跨域 |
|---|---|---|---|---|---|---|---|---|
| M2.1 | APPLIED→APPROVED/PROBATION/SUSPENDED/REJECTED | master-data/README §供应商准入 | erp-md/supplier-approval-status | ErpMdSupplierApprovalBizModel:104,119,134,160,175,211 + SuspendByPartnerProcessor:73 | TestErpMdSupplierApprovalStateMachine | 无 | 无 | date-range MUTEX |
| M2.2 | PLANNED→COMPLETED/CANCELLED | crm/state-machine.md §Event | erp-crm/event-status | ErpCrmEventCompleteProcessor:35 + CancelProcessor:35 | 无 | 无 | 无 | nop-job 提醒→notify |
| M2.3 | TODO→IN_PROGRESS→DONE；↔BLOCKED | projects/state-machine.md §Task | erp-prj/task-status | ErpPrjTaskBizModel:115,129,149,163 | 无 | 无 | 无 | DAG 前置依赖 |
| M2.4 | DRAFT→OPEN↔ON_HOLD；→COMPLETED/CANCELLED | projects/state-machine.md §Project | erp-prj/project-status | ErpPrjProjectBizModel:99,127 + Resume/Hold/CloseProcessors | 无 | 无 | 无 | 工时→finance successor |
| M2.5 | DRAFT→CANCELLED | purchase/state-machine.md | erp/doc-status | ErpPurQuotationBizModel:65 | 无 | 无 | 无 | 无 |
| M2.6 | DRAFT→CANCELLED | purchase/state-machine.md | erp/doc-status | ErpPurRfqBizModel:24 | 无 | 无 | 无 | 无 |
| M2.7 | DRAFT→CANCELLED | purchase/state-machine.md | erp/doc-status | RequisitionCancelProcessor | 无 | 无 | 无 | 无 |
| M2.8 | DRAFT→CANCELLED | purchase/state-machine.md | erp/doc-status | ErpPurOrderCancelProcessor | 无 | 无 | 无 | 无 |
| M2.9 | DRAFT→CANCELLED | sales/state-machine.md | erp/doc-status | Quotation cancel | 无 | 无 | 无 | 无 |
| M2.10 | DRAFT→CANCELLED | sales/state-machine.md | erp/doc-status | Order cancel | 无 | 无 | 无 | 无 |
| M2.11 | DRAFT→SUBMITTED→APPROVED/REJECTED；→CANCELLED | human-resource/state-machine.md §LeaveRequest | erp-hr/leave-status | LeaveRequest Submit/Approve/Cancel Processors | 无 | 无 | 无 | APPROVED→考勤/salary |
| M2.12 | ACTIVE→EXPIRED/TERMINATED | human-resource/state-machine.md §Contract | erp-hr/contract-status | ErpMntEmploymentContractBizModel:100 + ExpireOverdueProcessor:42 | 无 | 无 | 无 | notify hr.contract-expiry |
| M2.13 | DRAFT→PLANNED→IN_PROGRESS→FINISHED/CANCELLED；PLANNED→DRAFT | aps/state-machine.md | erp-aps/operation-order-status | ErpApsOperationOrderBizModel:134,149,170 + SchedulingEngine | 无 | 无 | 无 | 容量预留 DB unique |
| M2.14 | DRAFT→COMPUTED→APPROVED→EXECUTED；COMPUTED→DRAFT | drp/state-machine.md §Plan | erp-drp/drp-plan-status | DrpEngine + ApprovePlanProcessor | 无 | 无 | 无 | EXECUTED 生成补货单 |
| M2.15 | SUGGESTED→APPROVED→ORDERED/CANCELLED | drp/state-machine.md §Line | erp-drp/drp-line-status | DrpReleaseService + CancelLineProcessor | 无 | 无 | 无 | ORDERED 生成补货单 |
| M2.16 | RECEIVED→MATCHED→RECEIVED_TO_STOCK；→CANCELLED | b2b/state-machine.md §Asn | erp-b2b/asn-status | Asn Match/CreateReceive/HandleWebhook Processors | 无 | b2b config-gated | 无 | 由采购域入库 |
| M2.17 | TO_SEND→SENT→ACKNOWLEDGED；RECEIVED→ARCHIVED；ERROR 可恢复 | b2b/state-machine.md §EdiDoc | erp-b2b/edi-doc-state | ErpB2bEdiDocBizModel:71-163 + CreateOutbound/Inbound Processors | 无 | b2b config-gated | 无 | TransportManager wired-but-uncalled |
| M2.18 | DRAFT→NEGOTIATION→ACTIVE↔SUSPENDED；→EXPIRED/TERMINATED/CANCELLED | contract/state-machine.md | erp-ct/contract-status | ErpCtContractBizModel:83-128 + Activate/Amend Processors | 无 | 无 | 无 | e-signature(config-gated) |
| M2.19 | DRAFT→APPROVED/CANCELLED | manufacturing/state-machine.md §Forecast | erp-mfg/forecast-status | ErpMfgForecastBizModel:43,61 | 无 | 无 | 无 | MRP 消耗预测 |

### 3.4 M3 轴属性（19 项）

| 编号 | (1) 迁移语义 | (2) owner doc | (3) dict | (4) 生产 writer | (5) 测试 | (6) 保护区 | (7) 财务 | (8) 跨域 |
|---|---|---|---|---|---|---|---|---|
| M3.1 | NEW→QUALIFIED→CONVERTED/LOST/CANCELLED | crm/state-machine.md §Lead | erp-crm/lead-doc-status | ErpCrmLeadProcessor + ConversionProcessor + CancelProcessor | 无 | 无 | 无 | CONVERTED→sales quotation + master-data Partner |
| M3.2 | UNSUBMITTED→SUBMITTED→APPROVED/REJECTED；APPROVED→REJECTED | purchase/state-machine.md | wf/approve-status | Quotation 5 lifecycle Processors (INLINE) | 无 | 无 | 无 | 无 |
| M3.3 | 同上 | purchase/state-machine.md | wf/approve-status | Rfq 5 lifecycle Processors (INLINE) | 无 | 无 | 无 | 无 |
| M3.4 | 同上 | purchase/state-machine.md | wf/approve-status | Requisition Processors (PROC) | 无 | 无 | 无 | approve 生成 Order |
| M3.5 | 同上 | purchase/state-machine.md | wf/approve-status | ErpPurOrderProcessor:300-323 + 5 lifecycle | 无 | 无 | 无 | SoD（approver-is-creator） |
| M3.6 | 同上 | sales/state-machine.md | wf/approve-status | Quotation 5 lifecycle | 无 | 无 | 无 | 无 |
| M3.7 | 同上 | sales/state-machine.md | wf/approve-status | ErpSalOrderProcessor:250-273 + 5 lifecycle | 无 | 无 | 无 | SoD；可用量只读预检 |
| M3.8 | PROBATION→ACTIVE；→RESIGNED/TERMINATED/RETIRED(死) | human-resource/state-machine.md §Employee | erp-hr/employment-status | ErpHrRecruitmentHireProcessor:63 | 无 | 无 | 无 | transfer 不改 employmentStatus |
| M3.9 | DRAFT→SUBMITTED→APPROVED/REJECTED；REJECTED→SUBMITTED | human-resource/state-machine.md §Timesheet | erp-hr/timesheet-status | Timesheet Submit/Approve/Cancel Processors | 无 | 无 | 无 | 24h 跨表校验；归集→projects successor |
| M3.10 | DRAFT→SUBMITTED→APPROVED/REJECTED | projects（语义推断） | wf/approve-status | ErpPrjTimesheet Submit/Approve/Cancel Processors | 无 | 无 | 无 | 归集→finance successor |
| M3.11 | 结算单据 docStatus | projects（语义推断） | erp-prj/project-status | ErpPrjProjectSettlementProcessor:122-137 | 无 | 无 | 无 | 无 |
| M3.12 | 结算审批轴 | projects（同上） | wf/approve-status | Settlement lifecycle Processors | 无 | 无 | 无 | 无 |
| M3.13 | OPEN→WIP→SUBMITTED→COMPLETED；WIP↔ON_HOLD；→CANCELLED | manufacturing/state-machine.md §JobCard | erp-mfg/job-card-status | 6 JobCard Processors | 无 | 无 | 无 | WorkOrder cascade；APS generateJobCards |
| M3.14 | DRAFT→RUNNING→COMPLETED→FIRMED；COMPLETED→DRAFT | manufacturing/state-machine.md §MRP | erp-mfg/mrp-status | MrpEngine + MrpReleaseService | 无 | 无 | 无 | release 生成 WorkOrder/Subcontract |
| M3.15 | DRAFT→ACTIVE→CANCELLED（无 reversal listener） | assets/state-machine.md §Movement | erp/doc-status | Movement INLINE 5 action | 无 | 无 | 无 | 无过账副作用 |
| M3.16 | UNSUBMITTED→SUBMITTED→APPROVED/REJECTED | assets/state-machine.md | wf/approve-status | Movement INLINE 5 action | 无 | 无 | 无 | 无 |
| M3.17 | OPEN→ACCEPTED→COMPLETED/REJECTED；→CANCELLED | maintenance/state-machine.md §Request | erp-mnt/request-status | Request Accept/Start/Complete/Reject/Cancel Processors | TestErpMntVisitRequestStateMachine | 无 | 无 | accept→生成 Visit |
| M3.18 | DRAFT→FINALIZED→SIGNED | contract/state-machine.md §Version | erp-ct/version-status | ContractVersionBizModel:52 + SignVersionProcessor:51 + AmendProcessor:59 | 无 | 无 | 无 | 与 Contract 主轴联动 |
| M3.19 | 返利协议生命周期 | contract（返利子域） | erp-ct/rebate-agreement-status | RebateAgreement Processors | 无 | 无 | 无 | 生成 RebateSettlement |

### 3.5 M4 轴属性（65 项）

> M4 全部 `plan-first`。下表按域分组，每项八属性紧凑登记。生产 writer 列关键 Processor/BizModel 类名（完整行号由 M5.1 全集审计落地）。保护区域列标记财务/数据删除；财务影响列标记触发/冲销的过账类型；跨域副作用列标记 `I*Biz` 调用。

**finance 域（12 轴）：**

| 编号 | (1) 语义 | (3) dict | (4) 生产 writer | (5) 测试 | (6) 保护区 | (7) 财务 | (8) 跨域 |
|---|---|---|---|---|---|---|---|
| M4.1 | DRAFT→POSTED；POSTED→isReversed | erp-fin/voucher-status | VoucherBizModel:96 + PostingProcessor:812 + BudgetVoucherGenerator + BankStatementImporter + CloseVoucherWriter | 无 | **财务** | 过账核心 | 全域 IErpFinAcctDocProvider 聚合 |
| M4.2 | NEVER_OPENED→OPEN→CLOSING→CLOSED→CLOSED_FINAL；CLOSED_FINAL→OPEN(反结账) | erp-fin/period-status | Period Open/Close/Finalize/ReverseClose Processors | TestErpFinPeriodStateMachine | **财务** | 过账窗口控制 | Voucher post/reverse 受 period 守卫 |
| M4.3 | 对账单 post/reverse | erp-fin/reconciliation-status | Reconciliation Create/Post/Reverse Processors | 无 | **财务** | 核销 | ArApItem 联动 |
| M4.4/5 | 费用报销 docStatus/approveStatus | expense-claim-status / wf | ExpenseClaimProcessor:242-303 + lifecycle | 无 | **财务+SoD** | 报销凭证 | approver-is-creator |
| M4.6/7 | 预付款 docStatus/approveStatus | advance-status / wf | EmployeeAdvanceProcessor:154-203 + lifecycle | 无 | **财务+SoD** | 预付款凭证 | approver-is-creator |
| M4.8 | 应收票据多态生命周期 | notes-receivable-status | NotesReceivableProcessor + Collect/Dishonor Processors | TestErpFinNotesReceivableStateMachine | **财务** | 票据过账 | 无 |
| M4.9 | 应付票据生命周期 | notes-payable-status | NotesPayableProcessor:85-115 | TestErpFinNotesPayableStateMachine | **财务** | 票据过账 | 无 |
| M4.10 | 坏账审批 | wf/approve-status | BadDebtProcessor:128-194 | 无 | **财务** | ArApItem write-off | 无 |
| M4.11/12 | 预算方案 docStatus/approveStatus | budget-status / wf | BudgetScenario 6 lifecycle Processors | 无 | **财务** | 预算凭证 | CommitmentVoucherGenerator |

**purchase 域（8 轴，M4.13–M4.20）：** ErpPurReceive/Invoice/Payment/Return 各 docStatus + approveStatus。生产 writer = 各实体 Processor + 5-7 lifecycle Processors（Submit/Approve/Reject/Withdraw/Reverse/Cancel）。财务影响：Receive→`IErpInvStockMoveBiz`、Invoice→AP_INVOICE 凭证、Payment→PAYMENT 凭证+核销、Return→出库+红字发票。保护区：**财务**。既有测试：无（purchase 无 TestErp*StateMachine）。跨域：PurReversalListener 回写 posted=false + APPROVED→REJECTED。

**sales 域（8 轴，M4.21–M4.28）：** ErpSalDelivery/Invoice/Receipt/Return 各 docStatus + approveStatus。生产 writer = 各实体 Processor + lifecycle Processors。财务影响：Delivery→出库 `IErpInvStockMoveBiz`、Invoice→AR_INVOICE 凭证、Receipt→RECEIPT 凭证+核销、Return→入库+红字发票。保护区：**财务**。跨域：SalReversalListener 回写。

**inventory 域（6 轴，M4.29–M4.34）：** StockMove/StockTake/TransferOrder/OwnershipTransfer/CostAdjust/LandedCost 各 docStatus。生产 writer = 各 Processor/BizModel。财务影响：DONE 触发存货过账事件（InvPostingExecutor→`IErpFinVoucherBiz.post`）。保护区：**财务+库存强一致**。既有测试：无。跨域：StockMoveBookkeeper 余额更新；批次效期拦截。

**manufacturing 域（5 轴，M4.35–M4.39）：** WorkOrder docStatus+approveStatus、SubcontractOrder docStatus+approveStatus、MaterialIssue docStatus。生产 writer = 各 Processor。财务影响：完工入库→MANUFACTURING_RECEIPT 凭证、领料→MANUFACTURING_ISSUE 凭证+`IErpInvStockMoveBiz`、委外→SUBCONTRACT_FEE 凭证。保护区：**财务**。既有测试：TestErpMfgWorkOrderStateMachine。跨域：MfgSubcontractReversalListener；SoD。

**assets 域（14 轴，M4.40–M4.53）：** Asset/DepreciationSchedule status；ValueAdjustment/Disposal/Capitalization/Split/Merge 各 docStatus+approveStatus；Inventory/Maintenance status。生产 writer = 各 Processor（含 side-effect 写入 Merge/Split/Disposal/Capitalization）。财务影响：全部经 `IErpFinAcctDocProvider`（DEPRECIATION/CAPITALIZATION/DISPOSAL）。保护区：**财务**。既有测试：无。跨域：`IErpInvStockMoveBiz`（库存转固）；折旧引擎查 IN_SERVICE。

**maintenance 域（3 轴，M4.54–M4.56）：** Visit status、SparePartUsage docStatus+approveStatus。生产 writer = Visit Schedule/Start/Complete/Cancel Processors + SparePartUsage Processors。财务影响：备件→MAINTENANCE_ISSUE 凭证+`IErpInvStockMoveBiz`、工时→MAINTENANCE_LABOR 凭证（均 config-gated 默认 OFF）。保护区：**财务**。既有测试：TestErpMntVisitRequestStateMachine。跨域：EquipmentStatusLinker；红冲闭环。

**logistics 域（1 轴，M4.57）：** Shipment status。生产 writer = GatewayDispatcher:73-319。财务影响：DELIVERED→FREIGHT 凭证（`IErpFinVoucherBiz.post`）+ 可能 LandedCost。保护区：**财务**。跨域：`IErpInvLandedCostBiz`。

**quality 域（5 轴，M4.58–M4.62）：** Inspection docStatus+approveStatus、NCR status、Recall status+approveStatus。生产 writer = 各 Processor。财务影响：Inspection posted 三件套、NCR scrap→报废损失凭证/return→红字入库、Recall close→销售退货过账。保护区：**财务**。既有测试：TestErpQaInspectionStateMachine + TestErpQaRecallStateMachine。跨域：business queries quality（`findByRelatedBill`）。

**hr 域（2 轴，M4.63–M4.64）：** Salary paymentStatus + approveStatus。生产 writer = PayrollCalculator + MarkPaid/GenerateBankFile Processors + BizModel。财务影响：APPROVED→SALARY(270)+SOCIAL_INSURANCE_ER(290)+HOUSING_FUND_ER(300) 计提凭证；PAID→SALARY_PAYMENT(280) 发放凭证。保护区：**财务**。跨域：nop-wf 多级审批；notify wf.`<entity>.result`。

**contract 域（1 轴，M4.65）：** RebateSettlement status。生产 writer = PostSettlementProcessor:90。财务影响：post 触发结算凭证。保护区：**财务**。跨域：生成 purchase/sales 发票（InvoicePlan triggerInvoice）。

## 4. Ticket SLA 起算 drift 裁决

### 4.1 Drift 描述

`docs/design/customer-service/state-machine.md` 存在三处表述冲突：

1. **§1 状态定义表**：NEW 行注明「SLA 从创建时开始计时」
2. **§2 迁移表**：ASSIGNED→IN_PROGRESS 行注明「SLA 开始正式计时（startDateTime 设置）」
3. **§实现约定**（line 150）：「`startDateTime = 首次 IN_PROGRESS 时间`（start 动作设置，非 NEW/创建时）；与设计表『SLA 从创建时开始计时』表述不同——实现按 IN_PROGRESS 实际处理时长计 `duration`（更公平，NEW/ASSIGNED 未实际处理）」

### 4.2 裁决

**分类：(a) intentional legacy behavior（已生效的有意偏离）**

**权威解释**：`startDateTime = 首次 IN_PROGRESS 时间`（start 动作设置）。SLA duration 按 IN_PROGRESS 实际处理时长计算。§1 设计表的「SLA 从创建时开始计时」表述被实现约定取代。

**裁决理由**：
- 实现已在生产运行，§实现约定已显式登记偏离并给出业务理由（「更公平」——NEW/ASSIGNED 排队时间非实际处理时长）
- 此偏离不涉及数据错误或非法状态——`deadlineDateTime` 仍在 NEW 创建时计算（基于优先级+SLA 策略），只是 `startDateTime`/`duration` 的计量起点为 IN_PROGRESS
- 权威计划 `docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md` 已落地此实现

### 4.3 Successor（分支 (a) 显式落地）

按指南规则 13，分支 (a) successor = **owner doc 补注裁决结论**（对齐 §实现约定 既有偏离登记范式）。具体行动：
- 在 `customer-service/state-machine.md` §实现约定 追加 M0.2 裁决注记，确认分类为 `intentional legacy behavior`，§1 表述被取代
- M1.1 必须保持此行为（`startDateTime = 首次 IN_PROGRESS`），不得在迁移中静默改为创建时起算

**此裁决解除 M1.1 的矩阵固化前置。**

## 5. 死状态与异常登记

> 路线图规则 5：「发现的 dict 死状态必须按 Fix/Decision 处理」。M0.2 在此**登记**发现的死状态并指派 successor，不负责修复。

### 5.1 dict 死状态（dict 声明但生产无 writer）

| 域 | 实体 | dict 值 | 状态 | Successor |
|---|---|---|---|---|
| assets | ErpAstAsset | IDLE | 零 writer（折旧引擎查 IN_SERVICE 等价满足） | PM 要求 idle/resume 流程时实现 |
| hr | ErpHrEmployee | RESIGNED/TERMINATED/RETIRED | 零 writer | PM 要求离职/退休/转正流程时实现 |
| hr | ErpHrEmploymentContract | SUSPENDED | 零 writer | 同上 |
| hr | ErpHrSurvey | OPEN/CLOSED/ARCHIVED | CRUD stub，全死 | 实现问卷生命周期时 |
| hr | ErpHrDevelopmentPlan | CANCELLED | 零 writer | 实现 cancelPlan 时 |
| hr | ErpHrDevelopmentPlanItem | OVERDUE | 零 writer | 实现 OVERDUE auto job 时 |
| inventory | ErpInvPickingOrder | PICKING/PICKED | 零 writer（CrudBizModel stub） | WMS 上线时 |
| mfg | ErpMfgJobCard | PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED | 零 writer | 转序/工序转移上线时 |
| mfg | ErpMfgMrpPlan | CANCELLED | 零 writer | 实现 cancelPlan 时 |
| mfg | ErpMfgForecast | CONSUMED | 零 writer | MRP 消耗回写时 |
| finance | ErpFinVoucher | CANCELLED | 预留 dict（草稿 discard 走 useLogicalDelete） | 按需 |
| b2b | ErpB2bEdiDoc | TO_CANCEL | 零 writer（单步取消替代两步） | 按需 |
| quality | ErpQaRiskRegister | MITIGATED/CLOSED | 零 writer | 风险管理上线时 |
| quality | ErpQaAction | OVERDUE | 零 writer | OVERDUE auto job 时 |
| quality | ErpQaSpcChart | STALE | 零 writer | SPC STALE 检测时 |
| projects | ErpPrjMilestone | 全部 4 态 | CRUD stub | 里程碑生命周期上线时 |
| projects | ErpPrjBilling | 全部 5 态 | CRUD stub | 项目开票上线时 |

### 5.2 dict 漂移（writer 写的值不在 dict 中）

| 域 | 实体 | 问题 | Successor |
|---|---|---|---|
| projects | ErpPrjCostCollection | ProjectCostAggregator/ExpenseCostAggregator 写 `APPROVED` 但 dict `erp-prj/project-status` 无此值 | 新建 `erp-prj/cost-collection-status` dict（ORM ask-first） |

### 5.3 dict 定义缺失

| dict | 引用方 | 问题 | Successor |
|---|---|---|---|
| `erp/doc-status` | purchase/sales/quality/assets/maintenance/cs ~20+ 实体 | 未在任一 orm.xml inline `<dict>` 或 `*.dict.yaml` 找到定义 | 验证 app-erp-all bootstrap 是否注册；若缺失则按 Fix 补定义 |
| `erp-md/posted-status` | ErpMfgSubcontractOrder.postedStatus | 同上 | 同上 |
| `erp-mfg/simulation-status` | ErpMfgMrpScenario/Version | 无 inline dict，无 dict.yaml | 补定义或改用 `erp-drp/simulation-status`（后者已存在） |

## 6. 汇总索引（域 × 轴 → 路线图编号）

> 本索引在路线图 M2/M3/M4 表追加行后与之一一对应。

| 域 | M2 | M3 | M4 |
|---|---|---|---|
| master-data | M2.1 | — | — |
| cs | — | — | — (M1.1 试点) |
| purchase | M2.5–M2.8 | M3.2–M3.5 | M4.13–M4.20 |
| sales | M2.9–M2.10 | M3.6–M3.7 | M4.21–M4.28 |
| inventory | — | — | M4.29–M4.34 |
| finance | — | — | M4.1–M4.12 |
| manufacturing | M2.19 | M3.13–M3.14 | M4.35–M4.39 |
| crm | M2.2 | M3.1 | — |
| projects | M2.3–M2.4 | M3.10–M3.12 | — |
| quality | — | — | M4.58–M4.62 |
| assets | — | M3.15–M3.16 | M4.40–M4.53 |
| maintenance | — | M3.17 | M4.54–M4.56 |
| logistics | — | — | M4.57 |
| aps | M2.13 | — | — |
| hr | M2.11–M2.12 | M3.8–M3.9 | M4.63–M4.64 |
| b2b | M2.16–M2.17 | — | — |
| contract | M2.18 | M3.18–M3.19 | M4.65 |
| drp | M2.14–M2.15 | — | — |
| notify | — | — | — |
