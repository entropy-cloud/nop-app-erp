# Extended Domains Business Logic Roadmap

> 最后更新：2026-07-04
> 本路线图覆盖**核心 5 域之外**的 13 域自定义 BizModel 方法与编排逻辑。
> 前置条件：`crud-roadmap.md` 中对应域的 CRUD 已完成。

## Work Item Status

> 状态在工作项上；Milestone 仅为分组。

### Milestone M2 — 扩展 5 域
- 2.5：✅ done（资产折旧/处置/资本化 BizModel + 业财过账，2026-07-02，`docs/plans/2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`）
- 2.1：✅ done（BOM/工艺路线 BizModel：默认 BOM 选择 + 多级展开 phantom/环/深度 + 成本卷算 → ErpMfgCostRollup/Line；含工时/费率列类型修正，2026-07-02，`docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`）
- 2.2：✅ done（WorkOrder/JobCard 状态机：10 态工单状态机 + 三轴审批 + 齐套校验 + 领料出库/报工/完工入库 + 成本归集 + 完工质检 config-gated 钩子；含工时/费率/实领数量列类型修正，2026-07-03，`docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`）
- 2.3：✅ done（MRP 计算引擎：需求整合(销售订单/安全库存/手工)→BOM 多级展开→净需求→按期分单(lot-for-lot/固定批量)→计划订单(WORK_ORDER_REQUEST/PURCHASE_REQUEST)→释放转采购订单/工单，2026-07-03，`docs/plans/2026-07-02-2237-2-manufacturing-mrp-engine.md`）
- 2.4：✅ done（质检触发 + NCR/CAPA 流程：质检单 4 态状态机(行级评测+结果汇总+posted) + 业务触发(采购入库/销售出库/工单完工 createForBusinessBill+模板匹配+强制质检阻塞 config-gated) + NCR 5 态状态机 + CAPA 生命周期(效果验证门控) + REJECTED 自动生成 NCR，2026-07-03，`docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md`）
- 2.6：✅ done（项目成本归集：工时状态机+成本率解析+PROJECT_COST_COLLECTION 业财过账 + 预算控制(WARNING/STRICT) + 成本归集汇总(actualCost 回写) + 项目状态引用校验 + 费用报销归集(projects 驱动只读聚合, config-gated)，2026-07-03，`docs/plans/2026-07-03-1018-2-projects-cost-collection.md`）
- 2.7：✅ done（维护计划/停机/备件消耗：访问 5 态+请求 6 态状态机+设备状态联动+备件消耗→inventory 出库(generateMove 扣余额)+维护计划到期生成 PLANNED 访问+停机记录(totalMinutes+设备 DOWN 联动)+维护全场景端到端，2026-07-03，`docs/plans/2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime.md`）
- 2.8：✅ done（CRP 负荷计算：工作中心日历/班次 + 按产品产能子实体(取代标量 capacity 反模式) + 负荷快照行 + calculateLoad(workcenter×date 聚合 loadHours+setupHours, 重算清旧快照) + getLoadReport(loadHours/capacityHours×efficiency/loadRate/overloaded) + 超负荷阈值门控(erp-mfg.crp-overload-threshold 默认1.0)；APS fallback WorkOrder 计划日期均匀分派，2026-07-03，`docs/plans/2026-07-03-1707-1-manufacturing-crp-load-engine.md`）
- 2.9：✅ done（供应商评分卡周期评分 + AVL 准入联动：AVL 准入 6 态状态机(APPLIED→APPROVED→PROBATION→SUSPENDED→REJECTED) + 评分卡周期/维度/变量三实体 + finalizeScorecard 引擎(criteria×formula×weight→totalScore→standing, 公式经 XLang 表达式非硬编码) + standing=RED→AVL SUSPENDED 跨域联动 + RFQ 收件人校验(prevent/warn/hold config-gated), 2026-07-03, `docs/plans/2026-07-03-1707-2-supplier-scorecard-avl.md`）
- 2.11：✅ done（批次召回事件：召回事件头/目标实体 + 5 态状态机(OPEN→APPROVED→IN_PROGRESS→CLOSED/CANCELLED) + 强制审批(erp-qua.recall-require-approval) + 目标定位(IErpInvStockMoveBiz.batchTrace 批次聚合查询 + batchId→batchNo 类型桥 + 追溯未启用报错) + 客户通知门控(close 前全 target returnStatus≠PENDING + notifyCustomer=true) + 批量退货编排(IErpSalReturnBiz 同步调，召回只登记不直接改余额) + NCR 升级召回(upgradeToRecall→ESCALATED_TO_RECALL + 建召回继承物料/严重程度)，2026-07-03，`docs/plans/2026-07-03-1707-3-quality-recall-event.md`）
- 2.10：✅ done（VMI 所有权转移：StockBalance/Ledger 加 ownerId/ownershipType 维度(config-gated, ownership-tracking-enabled 默认关) + ErpInvOwnershipTransfer/Line 三态状态机(DRAFT→CONFIRMED→DONE/CANCELLED) + 同库位余额重分类(sourceLocId=destLocId 物理不变) + OWNERSHIP_TRANSFER 业财过账(借存货/贷应付-供应商) + DIRECTION_PAYABLE 辅助账(待供应商采购发票核销) + vmi-auto-generate-ap config 门控，2026-07-04，`docs/plans/2026-07-04-0549-1-inventory-vmi-ownership-transfer.md`）

### Milestone M3 — 新增 8 域
- 3.1：✅ done（CRM 线索→商机→报价单转化：Lead docStatus 状态机(NEW→QUALIFIED/LOST/CANCELLED, lostReason 必填) + 漏斗阶段流转(moveStage 允许回退+convLog 全量留痕+probability 默认回填) + 线索查重(companyName/contactEmail/contactPhone, auto-convert-duplicate-lead 默认关仅提示) + 转化闭环(convertToCustomer 经 IErpMdPartnerBiz 建客户+新建 OPPORTUNITY+原 lead CONVERTED 弱指针；convertToQuotation 经 IErpSalQuotationBiz save 建报价单+弱指针+CONVERTED；幂等 ERR_LEAD_ALREADY_CONVERTED)；核心零污染 sales/master-data 实体零字段新增，2026-07-04，`docs/plans/2026-07-04-0549-2-crm-lead-opportunity-quotation-conversion.md`）
- 3.2：✅ done（CRM 活动提醒/时间线：Event complete/cancel 状态机(PLANNED→COMPLETED/CANCELLED) + 推模式回写 Lead.lastContactDate/nextActivityDate + findDueReminders(config-gated 窗口查询) + getLeadTimeline(Event+Activity 合并倒序)，2026-07-04，`docs/plans/2026-07-04-0700-1-crm-event-reminder-lead-scoring-forecast.md`）
- 3.3：✅ done（CRM 线索评分引擎：config 驱动 LOOKUP/FORMULA/BOOLEAN 准则 → 归一化 totalScore(0-100) → append-only 评分记录+行级快照 + auto-qualify 阈值触发(复用 qualify NEW→QUALIFIED, config-gated) + recalc-on-lead-update(config-gated) + 多 active config 抛错/无 active 不阻断，2026-07-04，同上计划）
- 3.4：✅ done（CRM 销售预测：refreshForecast 聚合引擎(commit/upside/best-case/weighted 分类+团队→公司层级 rollup+ForecastLine 快照清旧重建) + 期间状态机(OPEN→FROZEN/CLOSED, FROZEN/CLOSED 拒绝重算) + closePeriod 触发准确率(config-gated)，2026-07-04，同上计划）
- 3.5：✅ done（客服工单状态机：六态 assign/start/resolve/close/reopen/cancel + 非法迁移/终态 ErrorCode + TicketAction 审计 + SLA 策略匹配(ticketType+priority 精确度排序，无 isActive 过滤) + deadline 计算(日历小时 + 工作日跳周末，无工作时段窗口/节假日) + isSlaCompleted 标记 + scanOverdueTickets ESCALATE 升级 + findSlaWarnings 预警查询；config-gated sla-enabled/sla-warning-before/auto-assign-on-create，2026-07-04，`docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md`）
- 3.6：✅ done（CSAT 调查生命周期：createSurvey(RESOLVED 触发 config-gated，UUID token 无鉴权，一工单一调查唯一约束，delay=0 立即 SENT) + submitSurvey(csat 1-5/nps 0-10/ces 1-7 区间校验各 config-gated，状态时间戳派生 PENDING/SENT/COMPLETED) + NPS 分类(推荐者/被动者/贬损者派生不持久化) + reopen 取消未响应调查 + findSurveyReminders/findExpiredSurveys 查询；nop-job cron 注册归 Non-Goal，2026-07-04，同上计划）
- 3.7：✅ done（HR 薪酬核算引擎 + 个税累计预扣：6 配置实体新增(ErpHrSalaryItem/SocialInsuranceConfig/SocialInsuranceBase/TaxConfig/TaxSpecialDeduction/PayrollBankFile) + ErpHrSalary 扩展(approvalStatus 6 态审批状态机 PENDING→REVIEWED→APPROVED_FINANCE→APPROVED_MANAGER→PAID/VOID + 累计个税/考勤派生字段，paymentStatus 派生投影) + 核算引擎(出勤比例→基本工资→津贴→加班→绩效→社保基数 min/max 钳制→公积金→个税累计预扣法七级累进→实发) + runPayroll 批量幂等 + 审批链不可跳过(ERR_SALARY_ILLEGAL_STATUS_TRANSITION/LOCKED_AFTER_PAID) + 业财过账 SalaryPostingProvider(SALARY 270/SALARY_PAYMENT 280/SOCIAL_INSURANCE_ER 290/HOUSING_FUND_ER 300，ErpFinBusinessType 枚举+字典同步保护区域契约扩展) APPROVED_MANAGER 计提/PAID 发放 + 银行代发文件 CSV 生成，2026-07-04，`docs/plans/2026-07-04-0831-2-hr-payroll-engine-income-tax.md`）
- 3.8：✅ done（HR 排班管理：班次模板+排班分配(assignSingle 强制一人一天一排班唯一约束 ERR_SHIFT_DUPLICATE_ASSIGNMENT / assignBatch 员工组×日期范围 / copyFromPeriod 上期复制) + 轮换生成(generateRotation patternData JSON 序列+staggerDays 错峰+regenerate 清旧重生成，瞬态入参 Decision 不新增 RotationGroup 实体) + 迟到/早退/缺勤计算(calcAttendance 结果写 ErpHrAttendance 已有列，ShiftAssignment 保持标准输入角色，跨天夜班 endTime 次日基准，休假覆盖不计旷工) + 排班调换审批(submit→PENDING/approve→APPROVED 互换双方 assignment 班次+swapRequestId/replacedByAssignmentId 追溯/reject/cancel 非法迁移 ErrorCode) + 休假联动(onLeaveApproved 标记 ABSENT+LEAVE/onLeaveCancelled 解除，同域跨实体同步调用) + 新增 erp-hr/absence-reason 字典；本期无 ErpHrShiftAssignment 模型变更，2026-07-04，`docs/plans/2026-07-04-0831-3-hr-shift-scheduling.md`）
- 3.9–3.21：`todo`

## Implementation Order

### M2 — 扩展 5 域

| # | 工作项 | 域 | 设计文档 |
|---|--------|-----|---------|
| 2.1 | BOM/工艺路线 BizModel | manufacturing | `bom-and-routing.md` |
| 2.2 | WorkOrder/JobCard 状态机 + 审批 | manufacturing | `manufacturing/state-machine.md` |
| 2.3 | MRP 计算引擎 | manufacturing | `manufacturing/mrp.md` |
| 2.4 | 质检触发 + NCR/CAPA 流程 | quality | `quality/state-machine.md` |
| 2.5 | Asset 折旧/处置/资本化 | assets | `assets/state-machine.md` |
| 2.6 | Project 成本归集 | projects | `projects/cost-collection.md` |
| 2.7 | 维护计划/停机/备件消耗 | maintenance | `maintenance/state-machine.md` |
| 2.8 | CRP 负荷计算 | manufacturing | `manufacturing/crp.md` |
| 2.9 | 供应商评分卡计算 | purchase | `purchase/supplier-evaluation.md` |
| 2.10 | VMI 所有权转移 | inventory | `inventory/consignment.md` |
| 2.11 | 批次召回事件 | quality | `quality/recall.md` |

### M3 — 新增 8 域

| # | 工作项 | 域 | 设计文档 |
|---|--------|-----|---------|
| 3.1 | ✅ CRM Lead→Opportunity→Quotation 转化 | crm | `crm/README.md` |
| 3.2 | ✅ CRM 活动日历/事件提醒 | crm | `crm/README.md` |
| 3.3 | ✅ CRM 线索评分引擎 | crm | `crm/lead-scoring.md` |
| 3.4 | ✅ CRM 销售预测 | crm | `crm/sales-forecast.md` |
| 3.5 | ✅ 客服 Ticket + SLA 计时 | customer-service | `customer-service/README.md`, `customer-service/sla.md` |
| 3.6 | ✅ 客服满意度调查 | customer-service | `customer-service/csat.md` |
| 3.7 | HR 薪酬核算 + 个税计算 | human-resource | `human-resource/payroll.md` |
| 3.8 | ✅ HR 排班管理 | human-resource | `human-resource/shift-scheduling.md` |
| 3.9 | HR 薪酬模拟 | human-resource | `human-resource/payroll-simulation.md` |
| 3.10 | ✅ APS OperationOrder 排产引擎 | aps | `aps/scheduling.md` |
| 3.11 | ✅ APS ATP/CTP 交期承诺 | aps | `aps/scheduling.md` |
| 3.12 | ✅ 合同版本管理 + InvoicePlan 触发发票 | contract | `contract/README.md` |
| 3.13 | 合同电子签章 | contract | `contract/e-signature.md` |
| 3.14 | ✅ 合同批量折扣/返利计算 | contract | `contract/volume-discount.md` |
| 3.15 | ✅ DRP 净需求计算 | drp | `drp/README.md` |
| 3.16 | ✅ DRP 安全库存优化 | drp | `drp/safety-stock-optimization.md` |
| 3.17 | ✅ TMS 承运商网关三层 SPI | logistics | `logistics/carrier-integration.md` |
| 3.18 | ✅ TMS 运费双路径过账 | logistics | `logistics/README.md` |
| 3.19 | B2B EDI 格式 SPI + 信封状态机 | b2b | `b2b/edi-formats.md` |
| 3.20 | B2B ASN 入站处理 | b2b | `b2b/asn-processing.md` |
| 3.21 | B2B MFT AS2/SFTP 传输 | b2b | `b2b/managed-file-transfer.md` |
