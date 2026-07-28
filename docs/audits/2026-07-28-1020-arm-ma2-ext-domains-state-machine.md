# MA2 crm+cs+contract+b2b+maintenance 状态机审查（A2.14）

> 里程碑：MA2（业务正确性层 / 状态机正确性维度）
> 域/功能模块：crm（Lead/Event/stageId）+ cs（Ticket/SLA）+ contract（合同/InvoicePlan）+ b2b（EDI/ASN）+ maintenance（visit/request/PostingDispatcher）（A+B 合并，5 域）
> 审计 plan：`docs/plans/2026-07-28-1020-3-audit-remediation-ma2-ext-domains-state-machine.md`
> 行为基线：`docs/design/{crm,customer-service,contract,b2b,maintenance}/state-machine.md` + `docs/design/customer-service/sla.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
> 范围：5 域全部状态承载实体（plan baseline：crm Lead 5 态 + Event 3 态 + stageId + cs Ticket 6 态 + SLA + contract 合同 + InvoicePlan + b2b EDI 8 态 + ASN 4 态 + maintenance visit 5 态 + request 6 态 + DowntimeEntry），实际审查经逐文件全文阅读 + grep 验证
> 审计执行：2026-07-28
> 上游基线：MA1 done（P1-MA1-009 crm DECIMAL + P1-MA1-011/013 maintenance propId + P1-MA1-022 5 域跨域只读 + P1-MA1-029 contract InvoicePlan 半治理 + P2-MA1-027 contract CANCELLED drift + P2-MA1-028 maintenance IN_PROGRESS drift 已登记，本审计复核状态机角度）；A2.1 P2P done（b2b ASN→pur 收货 + ErpCtInvoicePlanBizModel 跨域写 P1-MA1-029 运行时复核）；A2.5a done（finance 凭证 reverseApprove 红冲 + tryPost 吞异常悬挂同型范式）；A2.8 purchase done（PurReversalListener 不对称同型）；A2.9 sales done（Contract reverseApprove + SalReversalListener 同型）；A2.10 assets done（linked visit maintenance 关联）；A2.12 quality done（NCR 过账 + 跨域只读 + tryPost 容错同型）；A2.13 projects done（TimesheetPostingDispatcher tryPost 吞异常同型 P1-MA2-068）

## 1. 审查范围与状态字段清单

| 域 / 实体 | 状态轴（dict） | 实现文件 | 审查方式 |
|----------|---------------|----------|---------|
| **crm / ErpCrmLead**（Lead 5 态） | `docStatus`(erp-crm/lead-doc-status NEW/QUALIFIED/CONVERTED/LOST/CANCELLED) + `stageId`(独立维度) | `ErpCrmLeadBizModel.java`（Facade）+ `ErpCrmLeadProcessor.java`（qualify/lose/cancel/moveStage + validateTransition* + doMoveStage） | 全文逐行 |
| **crm / Lead 转化** | — | `ErpCrmConversionProcessor.java`（convertToCustomer/convertToQuotation → IErpMdPartnerBiz + IErpSalQuotationBiz Facade） | 全文逐行 |
| **crm / stageId 迁移守卫** | ErpCrmStage.sequence 单向递增（owner doc 契约） | `ErpCrmLeadProcessor.java:24-25,138-143,173-184`（doMoveStage + validateMovable + requireStage） | 全文逐行 + grep |
| **crm / ErpCrmEvent**（Event 3 态） | `status`(erp-crm/event-status PLANNED/COMPLETED/CANCELLED) | `ErpCrmEventBizModel.java`（complete/cancel + findDueReminders） | 全文逐行 |
| **crm / 事件提醒 Job** | — | `ErpCrmEventReminderJob.java` + `app-erp-all/.../erp-crm-event-reminder.job.yaml`（cron 每 15min，默认 disabled） | 全文逐行 |
| **cs / ErpCsTicket**（Ticket 6 态） | `status`(erp-cs/ticket-status NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED) + SLA（startDateTime/deadlineDateTime/isSlaCompleted/duration/endDateTime） | `ErpCsTicketBizModel.java`（assign/start/resolve/close/reopen/cancel + matchAndAttachSla/scanOverdueTickets/findSlaWarnings） | 全文逐行 |
| **cs / SLA 计时** | — | `ErpCsTicketBizModel.java:135-162,209-211` + `SlaDeadlineCalculator.java`（calendar/working-days 两种模式） | 全文逐行 |
| **cs / SLA 升级 Job** | — | `ErpCsSlaScanJob.java` + `erp-cs-sla-scan.job.yaml`（cron 每分钟，默认 disabled） | 全文逐行 |
| **contract / ErpCtContract**（合同状态） | `status`(erp-ct/contract-status 6 态 DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/EXPIRED/TERMINATED) | `ErpCtContractBizModel.java`（activate/suspend/resume/terminate/expire/amend 6 迁移） | 全文逐行 |
| **contract / 到期 Job** | — | **无 Job 类 + 无 scheduler 注册**（expire() 仅手工 @BizMutation） | grep 全域 |
| **contract / ErpCtInvoicePlan**（跨域写） | — | `ErpCtInvoicePlanBizModel.java`（createApInvoiceDraft/createArInvoiceDraft 跨域 daoFor saveEntity） | 全文逐行 |
| **contract / 版本/电子签章** | version-status(DRAFT/FINALIZED/SIGNED) | `ErpCtContractVersionBizModel.java`(signVersion) + `IErpCtSignatureProvider` SPI + config-gated e-signature | 全文逐行 |
| **b2b / ErpB2bEdiDoc**（EDI 8 态） | `state`(erp-b2b/edi-doc-state TO_SEND/SENT/TO_CANCEL/CANCELLED/ERROR/RECEIVED/ACKNOWLEDGED/ARCHIVED) | `ErpB2bEdiDocBizModel.java`（createOutbound/markSent/markAcknowledged/markError/retry/cancel/createInbound/archive） | 全文逐行 |
| **b2b / EDI 自动化** | — | **TransportManager wired-but-uncalled + 无 nop-job + 无 ack-timeout config** | grep 全域 |
| **b2b / ErpB2bAsn**（ASN 4 态） | `status`(erp-b2b/asn-status RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED) | `ErpB2bAsnBizModel.java`（handleInboundWebhook/matchPurchaseOrder/createReceiveFromAsn/retryMatch） | 全文逐行 |
| **maintenance / ErpMntVisit**（visit 5 态） | `status`(erp-mnt/visit-status DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED) | `ErpMntVisitBizModel.java`（schedule/start/complete/cancel + doCancel reverseLabor） | 全文逐行 |
| **maintenance / ErpMntRequest**（request 6 态） | `status`(erp-mnt/request-status OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED) | `ErpMntRequestBizModel.java`（accept/startRepair/complete/rejectRequest/cancel） | 全文逐行 |
| **maintenance / 过账 Dispatcher** | — | `MaintenanceLaborPostingDispatcher.java`(tryPost 吞异常) + `MaintenanceIssuePostingDispatcher.java`(同型) + `MntPostingExecutor`→IErpFinVoucherBiz Facade | 全文逐行 |
| **maintenance / DowntimeEntry** | 无显式 status（时间驱动） | `ErpMntDowntimeEntryBizModel.java`（record/complete）+ `EquipmentStatusLinker.java` | 全文逐行 |

5 域状态字段分布在 crm(2)+cs(1)+contract(1+version)+b2b(2)+maintenance(2+DowntimeEntry) 共 9 个状态承载实体 + 跨域过账/SLA/Job 助手（与 plan baseline 一致 ✓；plan baseline 称「contract BizModel 仅实现 ACTIVE→TERMINATED」**已修正**——实仓 BizModel 实现 6 迁移 activate/suspend/resume/terminate/expire/amend）。

---

## 2. 10 维度审查

### 2.1 维度「状态定义」

**裁决：PASS（含 owner doc drift 注记 + b2b TO_CANCEL 死状态 + cs SLA 计时起止点偏离已登记）」

#### crm Lead docStatus（5 态）+ Event status（3 态）

✅ **每个状态表达「等待什么」**（owner doc crm/state-machine.md §1）：Lead NEW=新线索待跟进 / QUALIFIED=已验证进漏斗 / CONVERTED/LOST/CANCELLED 终态；Event PLANNED/COMPLETED/CANCELLED。dict option 与 `ErpCrmConstants.DOC_STATUS_*` / `EVENT_STATUS_*` 常量 1:1 对齐（`lead-doc-status.dict.yaml:1-27` 5 态 + `event-status.dict.yaml:1-19` 3 态确认）。

#### cs Ticket status（6 态）+ SLA

✅ **6 态语义清晰**（owner doc cs/state-machine.md §1 表）：NEW=待分派 / ASSIGNED=已分派待处理 / IN_PROGRESS=处理中 / RESOLVED=待客户确认（SLA 停止）/ CLOSED 终态 / CANCELLED 终态（不计绩效）。`ticket-status.dict.yaml:1-31` 6 态确认。

⚠️ **SLA 计时起止点偏离 owner doc §1 表（已登记）**——owner doc §1 表「SLA 从创建时开始计时」，实现 `ErpCsTicketBizModel.java:135-136` `startDateTime = 首次 IN_PROGRESS 时间`（start 动作设置）。owner doc §实现偏离补注 L150 已显式登记此偏离（「实现按 IN_PROGRESS 实际处理时长计 duration，更公平，NEW/ASSIGNED 未实际处理」）。**不登记为新 finding**（已登记偏离 + 实现比 owner doc 更公平）。

#### contract 合同 status（6 态）

✅ **6 态语义清晰**（owner doc contract/state-machine.md §1 表）：DRAFT=起草 / NEGOTIATION=谈判 / ACTIVE=执行 / SUSPENDED=中止 / EXPIRED 终态 / TERMINATED 终态。`contract-status.dict.yaml:8-30` 6 态确认。

⚠️ **CANCELLED owner doc drift（P2-MA1-027 复核维持）**——owner doc §1（经 L-5 补）列 7 态含 CANCELLED，但代码 dict 6 态无 CANCELLED + 无 `CONTRACT_STATUS_CANCELLED` 常量 + 无 setStatus(CANCELLED) writer（grep 全 `module-contract` 零匹配）。DRAFT 废弃走 `useLogicalDelete="true" deleteFlagProp="delVersion"`（`app-erp-contract.orm.xml:124`）。**纯 owner doc drift，无运行时影响**。维持 P2-MA1-027 watch-only。

#### b2b EDI state（8 态）+ ASN status（4 态）

✅ **EDI 8 态语义清晰**（owner doc b2b/state-machine.md §1 表）：TO_SEND=待发送 / SENT=待确认 / TO_CANCEL=待取消确认 / CANCELLED 终态 / ERROR=失败可重试 / RECEIVED=入站待解析 / ACKNOWLEDGED 终态 / ARCHIVED 终态。`edi-doc-state.dict.yaml:7-38` 8 态确认。

⚠️ **TO_CANCEL dict 死状态**——`edi-doc-state.dict.yaml:17` + `ErpB2bConstants.java:11` 定义 `EDI_DOC_STATE_TO_CANCEL`，但 `ErpB2bEdiDocBizModel` **无任何方法迁移到/自 TO_CANCEL**（取消直接 TO_SEND/SENT/ERROR→CANCELLED，不经 TO_CANCEL 中间态）。登记 **P2-MA2-069**（详 §4）。

✅ **ASN 4 态语义清晰**：RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED。`asn-status.dict.yaml:7-22` 4 态确认。

#### maintenance visit（5 态）+ request（6 态）+ DowntimeEntry（时间驱动）

✅ **visit 5 态语义清晰**（owner doc maintenance/state-machine.md §适用对象一 §1 表）。`visit-status.dict.yaml` 5 态确认。

✅ **request 6 态语义清晰**——`request-status.dict.yaml:9-29` 6 态含 IN_PROGRESS（维修中）确认。

⚠️ **request IN_PROGRESS owner doc drift（P2-MA1-028 复核维持）**——owner doc §适用对象二 prose 称「6 态」但状态定义表（`:128-134`）仅列 5 态（缺 IN_PROGRESS），ASCII 图将「维修中」归于 visit 而非 request。**代码 writer 存在**——`ErpMntRequestBizModel.java:122 setStatus(REQUEST_STATUS_IN_PROGRESS)`（startRepair ACCEPTED→IN_PROGRESS），IN_PROGRESS 完全可达。**纯 owner doc drift，无运行时影响**。维持 P2-MA1-028 watch-only。

✅ **DowntimeEntry 无显式 status 字段（时间驱动）**——`app-erp-maintenance.orm.xml:384-418` 仅 startTime/endTime/totalMinutes，生命周期由 endTime 是否为空隐式表达（owner doc §适用对象三 确认）。

---

### 2.2 维度「转换完整性」

**裁决：FAIL（4 项新 P1：contract EXPIRED Job 缺失 + NEGOTIATION→TERMINATED 缺失 + b2b EDI 自动化缺失 + crm stageId 守卫缺失）」

#### crm Lead 迁移矩阵（核心）

| From → To | 触发 | 前置（owner doc §2） | 实际前置（代码） | 代码位置 | 裁决 |
|-----------|------|---------------------|----------------|----------|------|
| NEW→QUALIFIED | `qualify` | leadType=LEAD，联系人信息必填 | status==NEW ✓ + 首次设置 stageId（findFirstStage 按 sequence asc） | `ErpCrmLeadProcessor.java:32,65-70,108-118` | ✅ |
| NEW/QUALIFIED→LOST | `lose` | lostReasonId 必填 | status∈{NEW,QUALIFIED} ✓ + **requireLostReason 强制** | `:39,72-78,99-104,120-127` | ✅ |
| NEW/QUALIFIED→CANCELLED | `cancel` | — | status∈{NEW,QUALIFIED} ✓ | `:47,80-86,129-132` | ✅ |
| QUALIFIED→CONVERTED | `convertToCustomer`/`convertToQuotation` | leadType 匹配 + partnerId | 拒已 CONVERTED ✓ + leadType 守卫 + partnerId 守卫 | `ErpCrmConversionProcessor.java:44-64,124-130` | ✅ |
| stageId 前移 | `moveStage` | **sequence 单向递增（不能跳级回退）** | **无 sequence 方向守卫——允许前移/回退** | `:54-61,91-97,138-143` | ❌ **P1-MA2-075** |

❌ **P1-MA2-075**（详 §4）：crm stageId 单向递增守卫未实现。owner doc §stageId 迁移规则「stageId 沿 ErpCrmStage.sequence 递增前移（**不能跳级回退**）」+ §审查提示「阶段迁移（stageId）的 sequence 单向递增约束」vs 代码 `ErpCrmLeadProcessor.java:24-25` Javadoc 显式声明「允许前移/回退（销售流程中阶段可能反复）」+ `:138-143 doMoveStage` 无 sequence 方向比较。代码比 owner doc 更宽松。Funnel/conversion-rate 报表（`FunnelAggregationEngine.java:203,274` 按 sequence 排序）假设 monotonic progression，阶段回退致漏斗/转化率统计漂移。按同型 owner doc 契约漂移裁决 P1（deliberate code design + reporting-metric skew，非数据破坏）。

#### crm Lead 转化（跨域 Facade）

✅ **跨域写经 I\*Biz Facade 合规**——`ErpCrmConversionProcessor.java:38-42` 注入 `IErpMdPartnerBiz` + `IErpSalQuotationBiz`；convertToCustomer→`partnerBiz.save`（:90）创建 ErpMdPartner；convertToQuotation→`quotationBiz.save`（:101）创建 ErpSalQuotation。**核心零污染**——转化结果以 crm 侧弱指针 `relatedBillType`/`relatedBillCode` 承载（`markLeadConverted:124-130`）。

#### cs Ticket 迁移矩阵（核心 + SLA 联动）

| From → To | 触发 | 前置 | SLA 副作用 | 代码位置 | 裁决 |
|-----------|------|------|-----------|----------|------|
| NEW→ASSIGNED | `assign` | status==NEW | — | `ErpCsTicketBizModel.java:108-124` | ✅ |
| ASSIGNED→IN_PROGRESS | `start` | status==ASSIGNED | **startDateTime=now（首次 IN_PROGRESS）** | `:126-141,135-136` | ✅ |
| IN_PROGRESS→RESOLVED | `resolve` | status==IN_PROGRESS | **duration 计算 + isSlaCompleted=(now≤deadline)** + CSAT 触发 | `:143-177,153-162` | ✅ |
| RESOLVED→CLOSED | `close` | status==RESOLVED + **超时须 remark** | endDateTime=now | `:179-199,187-192` | ✅ |
| RESOLVED→IN_PROGRESS | `reopen` | status==RESOLVED | **保留 startDateTime（时长累加重算）** + 取消未响应调查 | `:201-218,209-211` | ✅ |
| 非终态→CANCELLED | `cancel` | status∉{CLOSED,CANCELLED} | — | `:220-242,228-233` | ✅ |

✅ **cs SLA 计时联动完整**——候选 P0「SLA 计时恢复累加缺失致违约误判」**经证据证伪**：(1) `start:135-136` 首次 IN_PROGRESS 设 startDateTime；(2) `resolve:160-162 isSlaCompleted=(deadline==null || now≤deadline)`；(3) `reopen:209-211` **保留原 startDateTime 不重置**——duration 在下次 resolve 时经 `minutesBetween(startDateTime, now)` 累加重算（:155-158），实现 owner doc §2 L39「恢复计时（时长累加）」契约。**不登记为新 finding**（候选 P0 证伪）。

⚠️ **SLA 语义注记（残留风险，非 finding）**——reopen 保留 startDateTime 且 deadlineDateTime 不展期，故 RESOLVED 等待窗口（客户响应时间）计入下次 resolve 的 duration，可能致客户驳回后 SLA 违约（代理人被算入客户响应时间）。owner doc §2 L39「时长累加」语义对此含糊（未显式要求 RESOLVED 期间暂停 deadline）。当前实现是更严格解释（惩罚 reopen）。属业务策略语义问题，非状态机缺陷，归残留风险。

#### contract 合同迁移矩阵（核心 + 到期 Job）

| From → To | 触发 | 前置（owner doc §2） | 实际前置（代码） | 代码位置 | 裁决 |
|-----------|------|---------------------|----------------|----------|------|
| DRAFT→NEGOTIATION | （无 @BizMutation） | 合同内容完整 | 经 `__save` 直接置 NEGOTIATION（plan 2026-07-14-0215-2:13 显式） | `ErpCtContractBizModel.java`（无 submit 方法） | ⚠️ 设计简化（非 finding） |
| NEGOTIATION→ACTIVE | `activate` | 签署完成 | status==NEGOTIATION ✓ + type/direction 组合校验 + signVersion(FINALIZED→SIGNED) | `:58-75,63,177-192` | ✅ |
| ACTIVE→SUSPENDED | `suspend` | 双方确认中止 | status==ACTIVE ✓ | `:79-87` | ✅ |
| SUSPENDED→ACTIVE | `resume` | 中止解除 | status==SUSPENDED ✓ | `:91-99` | ✅ |
| ACTIVE→TERMINATED | `terminate` | 终止协议 + 法务审批 | status==ACTIVE ✓ | `:103-113` | ✅ |
| ACTIVE→EXPIRED | `expire` | endDate<now（**系统自动**） | status==ACTIVE ✓（**仅手工 @BizMutation，无 Job**） | `:117-125` | ❌ **P1-MA2-071** |
| ACTIVE→DRAFT | `amend` | 变更 | status==ACTIVE ✓ + 新版本 versionNo=max+1 原子 isCurrent 翻转 | `:129-160` | ✅ |
| NEGOTIATION→TERMINATED | （无） | 谈判破裂 | **未实现——terminate 仅守卫 ACTIVE** | （无） | ❌ **P1-MA2-072** |
| DRAFT→CANCELLED | （无） | 草稿废弃 | 经 useLogicalDelete（非状态迁移） | ORM `:124` | ⚠️ P2-MA1-027 维持 |

❌ **P1-MA2-071**（详 §4）：contract EXPIRED 自动到期 Job 缺失。owner doc §2 L47「ACTIVE→EXPIRED | **系统自动** | endDate<now」+ §7 L99「合同到期提醒 | nop-job 定时扫描 endDate」显式设计系统自动到期。**实仓无 Job 类 + 无 scheduler 注册 + 无 @CronProvider**（grep 全 `module-contract` 零匹配；对比 hr 域有 `ErpHrContractExpiryJob` 同型 Job）。`expire()` 仅手工 @BizMutation。生产环境 ACTIVE→EXPIRED 不可达除非运营手工触发——过期合同保持 ACTIVE，InvoicePlan triggerInvoice 仅守卫 status==ACTIVE（`ErpCtInvoicePlanBizModel.java:71-75`）致**过期合同仍可生成发票草稿**（虽 unposted DRAFT 经人工审批可拦截，但生命周期不变量 ACTIVE 应在 endDate 后退出被破坏）。

❌ **P1-MA2-072**（详 §4）：contract NEGOTIATION→TERMINATED 迁移缺失。owner doc §2 L34/L51 显式声明「NEGOTIATION→TERMINATED（谈判破裂，终态）」迁移。代码 `terminate:105-107` 仅守卫 status==ACTIVE——NEGOTIATION 合同谈判失败无状态机出口（仅经 useLogicalDelete 逻辑删除逃生，但逻辑删除≠TERMINATED 语义：TERMINATED=已生效合同提前终止需归档版本+关联终止协议；NEGOTIATION 失败=未生效合同放弃）。owner doc §3 L58 明示「已进入 NEGOTIATION 或后续态的合同不可作废（CANCELLED），只能 TERMINATED」——代码无此路径。

#### contract InvoicePlan 跨域写（P1-MA1-029 复核）

✅ **P1-MA1-029 半治理维持**——`ErpCtInvoicePlanBizModel.java:127,147,159,164,184,196` 6 处跨域 `daoFor(ErpPurInvoice/ErpPurInvoiceLine/ErpSalInvoice/ErpSalInvoiceLine).saveEntity` 绕过 `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz` 审批管道。Javadoc `:41-45` 显式 bypass rationale（「避免服务依赖级联」）。生成 unposted DRAFT（docStatus=DRAFT/approveStatus=UNSUBMITTED/posted=false）——A2.1 P2P 运行时复核业务正确性不受影响。**`docs/architecture/posting-exemptions.md` 登记同型 sibling `ErpCtRebateSettlementBizModel`（:26-41）但未登记 `ErpCtInvoicePlanBizModel`**——半治理维持 todo MR1。

⚠️ **审计文档准确性注记（非新 finding）**——`docs/audits/2026-07-27-1430-arm-ma1-platform-conformance-bc-tier.md:195,226,248` 称 contract「无跨模块写」+ b2b ASN「IErpPurReceiveBiz.createFromAsn I\*Biz Facade 非daoFor 直写」，实仓 grep 证实 contract 有 12 处跨域写（InvoicePlan 6 + RebateSettlement 6）+ b2b ASN 是 `daoFor` 直写（非 Facade，`ErpB2bAsnBizModel.java:215,226,266`）。**governance 工件正确**（posting-exemptions.md 登记两处豁免边界清晰），仅审计描述文字与代码机制不符。本审计交叉登记，归并到 §6 残留风险（MR1 顺手修正审计描述）。

#### b2b EDI 迁移矩阵（核心 + 自动化）

| From → To | 触发 | 前置（owner doc §2/§L-8） | 实际前置（代码） | 代码位置 | 裁决 |
|-----------|------|---------------------|----------------|----------|------|
| (new)→TO_SEND | `createOutbound` | — | checkDuplicate(UNIQUE formatId+relatedBillType+relatedBillCode) | `ErpB2bEdiDocBizModel.java:66-101` | ✅ |
| TO_SEND→SENT | `markSent`（**手工**） | status==TO_SEND | 手工 markSent（**TransportManager wired-but-uncalled**） | `:103-117` | ❌ **P1-MA2-073** |
| SENT→ACKNOWLEDGED | `markAcknowledged`（**手工**） | status==SENT | 手工（**无 ACK-timeout→ERROR**） | `:119-133` | ❌ **P1-MA2-073** |
| SENT→ERROR（L-8 补） | 系统（对方拒绝/超时未确认） | ack-timeout-seconds 默认 24h | **未实现——无 ack-timeout config + 无 Job** | （无） | ❌ **P1-MA2-073** |
| ERROR→TO_SEND | `retry`（**手工**） | status==ERROR | 手工 retry + retryCount++（**无自动重试 + 无指数退避**） | `:150-166` | ❌ **P1-MA2-073** |
| TO_SEND/SENT/ERROR→CANCELLED | `cancel` | status∈{TO_SEND,SENT,ERROR} | 手工 | `:168-183` | ✅ |
| (new)→RECEIVED | `createInbound` | — | checkDuplicate + Webhook HMAC 验签 | `:185-211` | ✅ |
| RECEIVED→ARCHIVED | `archive` | status==RECEIVED | 手工（ASN 处理完成） | `:213-226` | ✅ |
| ERROR>24h 升级 | 系统 | ERROR 超 24h 升级通知 | **未实现——无 Job + 无通知派发** | （无） | ❌ **P1-MA2-073** |

❌ **P1-MA2-073**（详 §4）：b2b EDI 出站自动发送 + ACK-timeout→ERROR + 自动重试 + ERROR>24h 升级 全部缺失。owner doc state-machine.md §L-8（:63）「自动重试最多 3 次（指数退避），耗尽后保留 ERROR 等待人工介入」+ §6（:84）「SENT→ERROR（ACK 超时 ack-timeout-seconds 默认 24h 触发）」+ §8（:126）「ERROR 超过 24 小时未处理升级通知」显式设计自动化控制点。**实仓**：(1) `TransportManager.java`（wired 在 `app-service.beans.xml:49-50`）**生产代码零调用**（仅 test `TestErpB2bMftTransport.java:54` 调用）——出站发送委托从未接线；(2) **无 `ErpB2b*Job.java` + 无 nop-job 注册 + 无 `*.job.xml`**；(3) **无 `erp-b2b.ack-timeout-seconds` config**（`ErpB2bConfigs.java` 仅 asn.match-timeout-hours）；(4) `retry:150-166` 仅手工 + retryCount++ 无自动触发。

**P0 升级评估裁决：维持 P1 不升 P0**——(1) **整个 b2b 子系统 config-gated OFF**（`erp-b2b.b2b-enabled` 默认 false，`ErpB2bConfigs.java:9,27`）→ 默认 config 零生产暴露；(2) **MFT transport 是 Mock-only Deferred SPI**（`MockTransportAdapter` 唯一 impl，真实 AS2/SFTP/FTPS = `managed-file-transfer.md` Non-Goal）→ 出站自动化属 Deferred transport 集成范畴，非生产路径回归；(3) 所有状态迁移方法手工可达（markSent/markAcknowledged/retry/cancel）——状态机不破坏，仅未自动化；(4) LOG.warn/error 提供运维可见性；(5) owner doc state-machine.md 自动化承诺 vs README/managed-file-transfer.md transport Deferred **文档内部不一致**（登记 P2-MA2-068）；(6) 按同型 missing-automation 裁决范式 P1（与 finance P1-MA2-033 NEVER_OPENED→OPEN missing + contract EXPIRED Job missing 同型）。

#### b2b ASN 迁移矩阵（跨域收货）

| From → To | 触发 | 前置 | 跨域副作用 | 代码位置 | 裁决 |
|-----------|------|------|-----------|----------|------|
| Webhook→RECEIVED | `handleInboundWebhook` | HMAC 验签 + eventId 去重 | createInbound EdiDoc(RECEIVED) | `ErpB2bAsnBizModel.java:89-122` | ✅ |
| RECEIVED→MATCHED | `matchPurchaseOrder` | status==RECEIVED + PO 查找 | EdiDoc ARCHIVED | `:124-187` | ✅ |
| MATCHED→RECEIVED_TO_STOCK | `createReceiveFromAsn` | config-gated + status==MATCHED | **daoFor(ErpPurReceive/Line) 直写**（豁免已登记） | `:189-237,215,226,266` | ✅（豁免） |
| retryMatch | `retryMatch` | 幂等（MATCHED/RECEIVED_TO_STOCK 跳过） | — | `:320-335` | ✅ |

✅ **ASN 跨域写豁免已登记**——`docs/architecture/posting-exemptions.md:43-61` §ErpB2bAsnBizModel 登记daoFor(ErpPurReceive/Line) 直写 + config-gated `erp-b2b.asn-auto-create-receive`（默认 false）+ 收敛条件「待采购域提供 purpose-built createFromAsn I\*Biz」。生成 ErpPurReceive docStatus=UNSUBMITTED（经 purchase 正常审批+过账管道）。**governance 工件正确**。

⚠️ **签名失败处理设计选择注记（非 finding）**——`handleInboundWebhook:107-110` 签名失败硬 throw（不创建 ERROR doc），仅 parse 失败才 createInbound→markError（:359-367）。owner doc §4 L67「Webhook 签名验证失败 | 拒绝请求，记录日志，设 blocking_level=ERROR」——实现硬拒绝（记录日志经 NopException），不创建 ERROR doc 是更安全设计（防伪造请求污染 EdiDoc 表）。设计选择，非缺陷。

#### maintenance visit 迁移矩阵（核心 + 过账）

| From → To | 触发 | 前置 | 副作用 | 代码位置 | 裁决 |
|-----------|------|------|--------|----------|------|
| DRAFT→SCHEDULED | `schedule` | status==DRAFT + 执行人已分配 + 排程无冲突 | — | `ErpMntVisitBizModel.java:50-57,149-152` | ✅ |
| SCHEDULED→IN_PROGRESS | `start` | status==SCHEDULED | **equipment→UNDER_MAINTENANCE** | `:61-67,154-160` | ✅ |
| IN_PROGRESS→COMPLETED | `complete` | status==IN_PROGRESS | equipment→RUNNING + **postLabor（tryPost 容错）** | `:71-77,162-181` | ⚠️ **P1-MA2-074** |
| 非终态→CANCELLED | `cancel` | 非终态 | equipment→RUNNING + **reverseLabor 红冲** | `:81-87,183-201` | ✅ |

#### maintenance request 迁移矩阵（6 态全迁移）

| From → To | 触发 | 前置 | 代码位置 | 裁决 |
|-----------|------|------|----------|------|
| OPEN→ACCEPTED | `accept` | status==OPEN | 生成 DRAFT visit + setStatus(ACCEPTED) | `ErpMntRequestBizModel.java:34-40,116-119` | ✅ |
| ACCEPTED→IN_PROGRESS | `startRepair` | status==ACCEPTED | **setStatus(IN_PROGRESS)**（P2-MA1-028 writer 存在确认） | `:44-49,121-124` | ✅ |
| IN_PROGRESS→COMPLETED | `complete` | status==IN_PROGRESS | `:53-58,126-130` | ✅ |
| OPEN/ACCEPTED→REJECTED | `rejectRequest` | status∈{OPEN,ACCEPTED} | `:62-71,132-135` | ✅ |
| OPEN/ACCEPTED→CANCELLED | `cancel` | status∈{OPEN,ACCEPTED} | `:75-84,137-140` | ✅ |

✅ **request 6 态全迁移 + IN_PROGRESS 可达**（证伪 P2-MA1-028 死状态假设——writer 存在）。

---

### 2.3 维度「终端状态和恢复」

**裁决：PASS（5 域终态无出边 + useLogicalDelete 统一 + cs RESOLVED 非终态可恢复）」

✅ **crm CONVERTED/LOST/CANCELLED 终态无出边**——`qualify`(仅 NEW)/`lose`/`cancel`(仅 NEW/QUALIFIED)/`moveStage`(仅 NEW/QUALIFIED)/`convert*`(拒已 CONVERTED) 全守卫源态；无 reactivate/reopen 方法（grep 零匹配）。LOST→QUALIFIED 重新激活**不可达**（owner doc §3「若需重新跟进，复制原线索创建新 Lead」）。

✅ **cs CLOSED/CANCELLED 终态无出边**——`cancel:228-233` 拒终态（ERR_TICKET_ALREADY_TERMINAL）；assign/start/resolve/close/reopen 全守卫源态。**RESOLVED 非终态**——reopen（RESOLVED→IN_PROGRESS）可恢复（owner doc §3「RESOLVED 不是终态，客户可驳回」）。

✅ **contract EXPIRED/TERMINATED 终态无出边**——activate/suspend/resume/terminate/expire/amend 全守卫源态（非 ACTIVE/SUSPENDED/NEGOTIATION 全拒）。SUSPENDED 非终态可 resume。owner doc §3「若需续签，从 EXPIRED 创建续期合同（parentContractId）」——续期经新建非恢复。

✅ **b2b CANCELLED/ACKNOWLEDGED/ARCHIVED 终态无出边**——markSent/markAcknowledged/retry/cancel/archive 全守卫源态。ERROR 非终态可 retry。owner doc §3 表确认。

✅ **maintenance COMPLETED/CANCELLED/REJECTED 终态无出边**——schedule/start/complete/cancel + accept/startRepair/complete/rejectRequest/cancel 全守卫源态。owner doc §3「终态不可恢复；若需再次维护，新建维护访问」。

✅ **归档与活跃区分**——5 域所有状态承载实体 `useLogicalDelete="true" deleteFlagProp="delVersion"`（crm 34 实体 + cs 16 实体 + contract 13 实体 + b2b 13 实体 + maintenance 12 实体全部声明）。

---

### 2.4 维度「异常路径」

**裁决：FAIL（2 项新 P1 同型悬挂/缺失交接：maintenance tryPost 吞异常 + contract EXPIRED Job）」

| 域 | 异常场景 | 处理 | 代码位置 | 裁决 |
|----|----------|------|----------|------|
| crm | LEAD 直接转报价单 | 系统拦截：convertToCustomer 先转 OPPORTUNITY | `ErpCrmConversionProcessor.java:141-147` | ✅ |
| crm | LOST 不填丢单原因 | 拒绝迁移：requireLostReason | `ErpCrmLeadProcessor.java:99-104` | ✅ |
| crm | 重复线索提交 | 查重服务提示（owner doc §4） | — | ✅ |
| crm | 阶段跳级/回退 | **允许（无守卫）** | `:138-143` | ❌ **P1-MA2-075**（§2.2） |
| crm | 事件提醒 Job | 存在（cron 每 15min，默认 disabled） | `ErpCrmEventReminderJob.java` | ✅（但 reminderMinutesBefore 死字段 → P1-MA2-076） |
| cs | SLA 超时未解决 | ESCALATE 审计 + 升级通知（cron 每分钟） | `ErpCsTicketBizModel.java:339-363` + `ErpCsSlaScanJob` | ✅ |
| cs | 客户驳回重处理 | reopen 恢复计时（保留 startDateTime） | `:201-218` | ✅ |
| cs | 重复工单取消 | cancel + 关联原工单 | `:220-242` | ✅ |
| cs | 关闭超时工单无原因 | 拒绝（ERR_TICKET_CLOSE_BREACHED_NO_REASON） | `:187-192` | ✅ |
| cs | NEW>1h / ASSIGNED>2h 滞留升级 | **未实现**（owner doc §避免工单滞留） | （无） | ⚠️ P2-MA2-067 |
| contract | ACTIVE 期间发现条款缺陷 | amend（ACTIVE→DRAFT 新版本） | `ErpCtContractBizModel.java:129-160` | ✅ |
| contract | endDate 到达 | **expire() 仅手工——无 Job 自动触发** | `:117-125` | ❌ **P1-MA2-071** |
| contract | SUSPENDED 期间开票 | 拦截（ERR_CT_CONTRACT_SUSPENDED + 仅 ACTIVE 可开票） | `ErpCtInvoicePlanBizModel.java:67-75` | ✅ |
| contract | 谈判破裂 | **NEGOTIATION→TERMINATED 未实现** | （无） | ❌ **P1-MA2-072** |
| contract | InvoicePlan 跨域写异常 | @BizMutation 事务回滚 + 生成 unposted DRAFT | `ErpCtInvoicePlanBizModel.java:125-197` | ✅（P1-MA1-029 维持） |
| b2b | EDI 发送失败（TO_SEND→ERROR） | markError（手工） | `ErpB2bEdiDocBizModel.java:135-148` | ✅ |
| b2b | **ACK 超时 / 自动重试 / ERROR>24h 升级** | **全部未实现** | （无） | ❌ **P1-MA2-073** |
| b2b | 同一业务单重复发送 | UNIQUE 防重 + ERR_B2B_EDI_DOC_ALREADY_PROCESSED | `:239-253` | ✅ |
| b2b | ASN 报文解析失败 | createInbound→markError + 保留原始报文 | `ErpB2bAsnBizModel.java:359-367` | ✅ |
| b2b | Webhook 签名失败 | 硬 throw（不创建 ERROR doc） | `:107-110` | ✅（设计选择） |
| b2b | 已取消 EDI 又收到确认 | 硬拒绝（ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION） | `ErpB2bEdiDocBizModel.java:124-126` | ✅ |
| maintenance | 排程冲突 | checkScheduleConflict 拒绝 | `ErpMntVisitBizModel.java:127-145` | ✅ |
| maintenance | 执行中需更多备件 | 中途补领（追加出库移动单，owner doc §4） | — | ✅ |
| maintenance | **工时/备件过账失败悬挂** | **tryPost 吞异常 + complete 无条件终态** | `MaintenanceLaborPostingDispatcher.java:110-122` + `ErpMntVisitBizModel.java:176-180` | ⚠️ **P1-MA2-074** |
| maintenance | doCancel reverseLabor 红冲 | 幂等 safe no-op（IErpFinVoucherBiz.reverse 平台守护） | `MaintenanceLaborPostingDispatcher.java:146-152` | ✅ |
| maintenance | DowntimeEntry 自引用 bug | relatedJobOrderId refEntityName 自指（owner doc 已登记） | `app-erp-maintenance.orm.xml:406-408` | ⚠️ 已登记（非状态机） |

#### maintenance 过账失败悬挂（同型根因交接）

**状态机角度复核**：

- **`MaintenanceLaborPostingDispatcher.tryPost:110-122`** try/catch 吞所有异常（NopException LOG.warn / 其他 LOG.error）返回 boolean。**不向上传播**。
- **`ErpMntVisitBizModel.complete:71-77`** → `doComplete:162-181` 在 `:163 setStatus(COMPLETED) + :171 updateEntity` **（先于过账）** → `:176-180 if (isPostingEnabled) postLabor(visit)` **不检查返回值**，失败仅 LOG.warn。visit 的 `posted` 列**从不被 BizModel 写入**（`MaintenanceLaborPostingDispatcher` 用 `ErpFinVoucherBillR` 反查判重，不依赖 visit.posted）。
- **悬挂窗口期**：visit COMPLETED 终态 + 无 MAINTENANCE_LABOR 凭证 + posted 永不写入 + 异常被吞不进入 finance 过账异常工作台 + **maintenance dispatcher javadoc 不引用 DeferredPostingSweepJob**（与 purchase/sales/assets/inventory/mfg peer dispatcher 不一致——后者显式引用「由 DeferredPostingSweepJob 兜底」）。

**裁决**：与 finance A2.5a P1-MA2-032（IGNORED 凭证悬挂）+ hr A2.7b P1-MA2-048（salary 过账悬挂）+ assets A2.10 P1-MA2-060（Capitalization/Disposal 过账悬挂）+ qa A2.12（MANUAL_POST NCR 过账悬挂）+ projects A2.13 P1-MA2-068（TimesheetPostingDispatcher tryPost 吞异常）**同型根因**（posting dispatcher 容错设计）。MaintenanceIssuePostingDispatcher（备件消耗，`:111-120`）同型。**不升 P0**：(1) **config-gated 默认 OFF**（`DEFAULT_LABOR_POSTING_ENABLED=false` + `DEFAULT_SPARE_PART_POSTING_ENABLED=false`，`ErpMntConstants.java:42,54`）→ 默认 config 零回归；(2) LOG.warn/error 可见性；(3) `reverseLabor` 幂等 safe no-op（`IErpFinVoucherBiz.reverse` 平台守护，无凭证时安全 no-op——比 assets P1-MA2-060 reverseApprove 不对称更优）；(4) 业财不一致经期末试算平衡人工发现。**aggravator**：dispatcher javadoc 不引用 DeferredPostingSweepJob（doc gap vs peers）。维持同型 P1（详 §4 P1-MA2-074）。

---

### 2.5 维度「可达性」

**裁决：FAIL（contract EXPIRED 生产不可达 + b2b TO_CANCEL dict 死状态 + crm stageId 无方向守卫）」

#### crm 可达性

✅ 从 NEW 可达 QUALIFIED→CONVERTED/LOST/CANCELLED + NEW→LOST/CANCELLED。无不可达状态，无死锁（终态无出边）。⚠️ stageId 无方向守卫（P1-MA2-075，§2.2）——可达性本身无问题（任何 stage 可达），但**单调前移不变量破坏**。

#### cs 可达性

✅ 从 NEW 可达 ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED + RESOLVED→IN_PROGRESS（reopen 往复，退出 CLOSED）+ 任意非终态→CANCELLED。无不可达状态，无死锁。

#### contract 可达性

❌ **EXPIRED 生产不可达**——`expire()` 仅手工 @BizMutation，**无 Job 自动扫描 endDate<now**（grep 全 `module-contract` 无 Job 类 + 无 scheduler）。生产环境 ACTIVE 合同 endDate 到达后**保持 ACTIVE**（除非运营手工调 expire）。owner doc §2 L47「系统自动」契约破坏。登记 **P1-MA2-071**。

⚠️ **CANCELLED dict 项不可达**（P2-MA1-027 维持）——代码无 setStatus(CANCELLED) writer，DRAFT 废弃走 useLogicalDelete。

✅ 其他状态（DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/TERMINATED）从 DRAFT 经手工 __save + activate/suspend/resume/terminate/amend 可达。

#### b2b 可达性

⚠️ **TO_CANCEL dict 死状态**——`edi-doc-state.dict.yaml:17` + `ErpB2bConstants.java:11` 定义，**无任何方法迁移到/自 TO_CANCEL**（取消直接 TO_SEND/SENT/ERROR→CANCELLED）。登记 **P2-MA2-069**。

✅ 其他 EDI 状态从 TO_SEND/RECEIVED 经手工 markSent/markAcknowledged/retry/cancel/archive 可达（生产自动化缺失归 P1-MA2-073，但手工路径完整）。

#### maintenance 可达性

✅ visit 5 态从 DRAFT 经 schedule/start/complete/cancel 全可达。✅ request 6 态从 OPEN 经 accept/startRepair/complete/rejectRequest/cancel 全可达（**IN_PROGRESS writer 存在证伪 P2-MA1-028 死状态假设**）。✅ DowntimeEntry 时间驱动（endTime null=活跃，非 null=终态）。

---

### 2.6 维度「角色和权限」

**裁决：PASS（5 域 owner doc §6 角色绑定齐全，运行时经 @BizMutation 入口权限）」

✅ **crm**（owner doc crm/state-machine.md §6）：销售员（owner/team）qualify/lose/moveStage + 销售员+报价权限 convert；创建者/管理员 cancel Event。危险操作（转化→CONVERTED 不可逆）owner doc §6「二次确认弹窗」。

✅ **cs**（owner doc cs/state-machine.md §6）：系统自动/客服主管 assign + 处理人 start/resolve/reopen + 客户/客服 close + **客服主管 cancel**（ERR_TICKET_ALREADY_TERMINAL 守卫）。危险操作（关闭超时工单）须 remark。

✅ **contract**（owner doc contract/state-machine.md §6）：合同经办人 DRAFT→NEGOTIATION + 合同管理员 activate/suspend/resume/terminate + 法务审批终止。⚠️ terminate 缺独立法务审批 mutation（owner doc §6「法务审批」——实现 terminate 仅 status==ACTIVE 守卫，无法务审批门控；config-gated e-signature 经 signVersion 间接覆盖签署环节）。**不登记为新 finding**（terminate 经 @BizMutation 入口权限 + e-signature config 覆盖 + owner doc Deferred 法务审批流 successor）。

✅ **b2b**（owner doc b2b/state-machine.md §6）：系统 markSent/markAcknowledged + B2B 管理员 retry/cancel/TO_CANCEL。危险操作（放弃 ERROR→CANCELLED）须确认业务单据未完成处理。

✅ **maintenance**（owner doc maintenance/state-machine.md §6）：维护主管 schedule/cancel + 维护人员 start/complete。

⚠️ **5 域状态迁移方法均无显式角色校验**——依赖 @BizMutation 入口权限（平台层统一）。与 finance/mfg/pur/sal/qa/prj 同型。**不登记为新 finding**。

---

### 2.7 维度「外部依赖」

**裁决：PASS（跨域写经 I\*Biz Facade 合规 + 跨域只读维持 P1-MA1-022 todo MR1 + 跨域写豁免登记）」

#### crm 跨域

✅ **Lead 转化跨域写经 I\*Biz Facade**——`IErpMdPartnerBiz.save`（createPartnerFromLead）+ `IErpSalQuotationBiz.save`（createQuotationFromOpportunity）。**crm production 代码无跨模块 daoFor**（grep 全 `module-crm/erp-crm-service/src/main` `daoFor(Erp` 43 hits 全部 intra-domain `ErpCrm*`，MA1 主张「crm 无跨域 daoFor」**实测确认**）。

✅ **事件提醒跨域通知经 IErpSysNotificationBiz Facade**——`ErpCrmEventReminderJob.notifyEvent:107`。

#### cs 跨域

✅ **cs production 代码无跨模块 daoFor**（grep 全 `module-cs/erp-cs-service/src/main` 13 hits 全部 intra-domain `ErpCs*`；Dashboard facade `ErpCsQualityDashboardBizModel` 严格 read-only——MA1 主张「cs Dashboard facade read-only 永久接受」**实测确认**）。

✅ **客户/通知跨域经 Facade**——`IErpMdPartnerBiz.findById`（客户名解析）+ `IErpSysNotificationBiz.notify`（SLA 升级）。⚠️ notifySlaOverdue 上下文用 `assignedToId` 置于 `escalationUserId` 键（`ErpCsTicketBizModel.java:403`）——通知模板经 ROLE resolver 解析实际接收人（class javadoc :387-393）。**设计选择，非缺陷**（残留风险注记）。

#### contract 跨域

⚠️ **InvoicePlan + RebateSettlement 跨域写经 daoFor 直写**（P1-MA1-029 维持）——12 处跨域 saveEntity（InvoicePlan 6 + RebateSettlement 6）。RebateSettlement 豁免已登记（`posting-exemptions.md:26-41`）；**InvoicePlan 豁免未登记**（半治理，P1-MA1-029 todo MR1）。

✅ **RebateAgreement 跨域只读**——`ErpCtRebateAgreementBizModel.java:134,137 daoFor(ErpPurInvoice/ErpSalInvoice).findAllByQuery`（period 发票聚合，read-only）。

#### b2b 跨域

⚠️ **ASN 跨域写经 daoFor 直写**（豁免已登记）——`ErpB2bAsnBizModel.java:215,226,266 daoFor(ErpPurReceive/ErpPurReceiveLine)` + `:267 daoFor(ErpMdMaterial)` read + `:462,469 daoFor(ErpPurOrder/Line)` read。`posting-exemptions.md:43-61` 登记豁免 + config-gated + createFromAsn successor 收敛条件。

✅ **UblInvoiceEdiProvider 跨域只读**——`:53 dao("...ErpSalInvoice")`（UBL payload 源，read-only）。

#### maintenance 跨域

⚠️ **maintenance 跨域只读 daoFor**（P1-MA1-022 维持）——`MaintenanceLaborPostingDispatcher.java:204 daoFor(ErpFinVoucherBillR)` 判重 + `:217 daoFor(ErpMdAcctSchema)` 账套解析；`MaintenanceIssuePostingDispatcher.java:182,191,202,213 daoFor(ErpFinVoucherBillR/ErpInvStockMove/ErpInvStockLedger/ErpMdAcctSchema)` 全 read-only；`ErpMntSparePartUsageBizModel.java:191 daoFor(ErpInvStockMove)` 反查。**状态机角度复核无升级**——跨域只读是判重/账套解析/反查副作用，不破坏状态机迁移（异常路径经 @BizMutation 事务回滚覆盖；complete→postLabor→resolveSubjectCode 跨域读失败→tryPost 吞异常返回 false→visit posted 悬挂 P1-MA2-074 同型交接）。维持 P1-MA1-022 todo MR1。

✅ **跨域写经 I\*Biz Facade**——`MntPostingExecutor`→`IErpFinVoucherBiz.post/reverse`（凭证）+ `IErpInvStockMoveBiz.generateMove/reverse`（备件出库移动单 REVERSAL）+ `IErpMntEquipmentBiz`（设备状态，同模块）。

✅ **DAG 无环**——5 域反向依赖 finance/master-data（经 Facade）+ inventory（经 Facade），不反向依赖 pur/sal（pur/sal 引用 contract/b2b 是反向——业务域→contract/b2b）。b2b ASN→pur 是已知跨域写（豁免登记）。

---

### 2.8 维度「TODO / 任务策略」

**裁决：PASS（5 域避免沉没设计基本落实；cs 滞留升级规则部分 Deferred）」

✅ **crm**——QUALIFIED 产生 assigned TODO（销售员按 stageId 待跟进）+ Event PLANNED 产生 assigned TODO（owner 待执行）。避免「QUALIFIED 长期停滞」owner doc §8「超过 7 天无 stageId 前移或 Event 记录时跟进提醒」——⚠️ 此 7 天提醒 Job **未实现**（grep 零匹配），归 owner doc Deferred（残留风险，非状态机 finding）。

✅ **cs**——NEW/ASSIGNED/IN_PROGRESS/RESOLVED 均产生 assigned TODO（owner doc cs/state-machine.md §8 表）。⚠️ **NEW>1h / ASSIGNED>2h 滞留升级规则未实现**（owner doc §避免工单滞留 L106-109）——仅 deadline-based SLA 升级（`scanOverdueTickets`）落实。登记 **P2-MA2-067**（watch-only，软 UX 升级，deadline-based 已覆盖 SLA breach）。

✅ **contract**——DRAFT/NEGOTIATION/SUSPENDED/TERMINATED 产生 assigned TODO（owner doc contract/state-machine.md §8 表）。⚠️ 「endDate 前 30/15/7 天 TODO 提醒」+ 「endDate 到达后 7 天未处理升级」**未实现**（无 Job）——与 P1-MA2-071（EXPIRED Job 缺失）同根因，一并裁决。

✅ **b2b**——TO_CANCEL/ERROR 产生 assigned TODO（B2B 管理员）（owner doc b2b/state-machine.md §8 表）。⚠️ 「ERROR 超过 24 小时未处理升级通知」**未实现**（无 Job）——与 P1-MA2-073（EDI 自动化缺失）同根因，一并裁决。

✅ **maintenance**——DRAFT 产生 assigned TODO（维护主管待排程）+ SCHEDULED 产生 assigned TODO（维护人员待执行）+ IN_PROGRESS 产生 confirm TODO（owner doc maintenance/state-machine.md §8 表）。

---

### 2.9 维度「场景演练」（最重要）

**裁决：FAIL（10 场景覆盖；场景 f contract 到期 + 场景 h b2b EDI 异步 + 场景 j maintenance 过账悬挂 三场景暴露 finding）」

#### 场景 a：crm Lead 转化 happy path

1. 销售员创建 Lead（NEW）→ `qualify`（NEW→QUALIFIED，首次设 stageId）→ `moveStage`（stageId 前移）→ `convertToCustomer`（leadType=LEAD→创建 ErpMdPartner + 新 OPPORTUNITY Lead，CONVERTED）→ `convertToQuotation`（leadType=OPPORTUNITY→创建 ErpSalQuotation，CONVERTED）

证据：`ErpCrmLeadProcessor.qualify:32,108-118` + `ErpCrmConversionProcessor.convertToCustomer:44-53,convertToQuotation:55-64`。✅ 跨域 Facade 合规。

#### 场景 b：crm Lead 流失（QUALIFIED→LOST）

1. Lead QUALIFIED → `lose`（须 lostReasonId，否则 ERR_LOST_REASON_REQUIRED）→ LOST 终态。

证据：`ErpCrmLeadProcessor.lose:39,99-104,120-127`。✅ lostReasonId 强制。

#### 场景 c：cs 工单 happy path（NEW→...→CLOSED + SLA 达标）

1. 客户门户提交工单（NEW）→ `assign`（→ASSIGNED）→ `start`（→IN_PROGRESS，startDateTime=now）→ `resolve`（→RESOLVED，isSlaCompleted=(now≤deadline)=true，duration 计算，CSAT 触发）→ `close`（→CLOSED，endDateTime=now）

证据：`ErpCsTicketBizModel.java:108-199`。✅ SLA 达标路径完整。

#### 场景 d：cs SLA 违约 + 客户驳回重处理（**候选 P0 证伪**）

1. 工单 IN_PROGRESS，deadline 到达，isSlaCompleted=false → `ErpCsSlaScanJob` 每分钟扫描 → `scanOverdueTickets:339-363` 匹配 status∈{ASSIGNED,IN_PROGRESS} + deadlineDateTime<now + isSlaCompleted=false → writeAction(ESCALATE) + notifySlaOverdue
2. 处理人 `resolve`（→RESOLVED，isSlaCompleted=false）→ 客户驳回 → `reopen`（RESOLVED→IN_PROGRESS，**保留 startDateTime 恢复累加**，取消未响应调查）→ 处理人再次 `resolve`（duration 经 minutesBetween(startDateTime, newNow) 累加重算）→ 客户 `close`（**isSlaCompleted=false 须 remark** 否则 ERR_TICKET_CLOSE_BREACHED_NO_REASON）

证据：`ErpCsTicketBizModel.java:339-363,143-177,201-218,187-192` + `ErpCsSlaScanJob`。✅ **候选 P0「SLA 计时恢复累加缺失」经证据证伪**——reopen 保留 startDateTime，duration 累加重算落实。

#### 场景 e：contract 合同 happy path（DRAFT→NEGOTIATION→ACTIVE→履约）

1. 经办人创建合同（DRAFT）→ `__save` 置 NEGOTIATION（无 @BizMutation，plan 2026-07-14-0215-2 显式）→ 双方签署 → `activate`（NEGOTIATION→ACTIVE，signDate=now，signVersion FINALIZED→SIGNED）→ 履约（InvoicePlan triggerInvoice 仅 ACTIVE 可开票）

证据：`ErpCtContractBizModel.java:58-75` + `ErpCtInvoicePlanBizModel.java:67-75`。✅ 主路径完整（DRAFT→NEGOTIATION 经 __save 简化已登记）。

#### 场景 f：contract 合同到期 cron-gated（**P1-MA2-071**）

1. ACTIVE 合同 endDate 到达 → **预期**（owner doc §2 L47「系统自动 endDate<now」）：nop-job 扫描 → `expire`（ACTIVE→EXPIRED）+ 归档版本
2. **实际**：**无 Job 类 + 无 scheduler**（grep 全 `module-contract` 零匹配）→ 合同**保持 ACTIVE** → InvoicePlan triggerInvoice（status==ACTIVE 守卫通过）**仍可生成发票草稿**（虽 unposted DRAFT 经人工审批可拦截，但生命周期不变量破坏）

❌ **P1-MA2-071**（详 §4）。owner doc §2/§7「系统自动」契约未落地。

#### 场景 g：contract InvoicePlan 跨域写（P1-MA1-029 维持）

1. ACTIVE 合同 InvoicePlan 到期 → `triggerInvoice` → `createApInvoiceDraft`/`createArInvoiceDraft`（daoFor(ErpPurInvoice/Line/ErpSalInvoice/Line).saveEntity）→ 生成 unposted DRAFT（docStatus=DRAFT/approveStatus=UNSUBMITTED/posted=false）→ purchase/sales 正常审批+过账管道

证据：`ErpCtInvoicePlanBizModel.java:125-197`。✅ 业务正确性不受影响（A2.1 P2P 复核）；P1-MA1-029 半治理维持（豁免未登记）。

#### 场景 h：b2b EDI 异步处理（**P1-MA2-073**）

1. 销售发票审核 → `createOutbound`（TO_SEND）→ **预期**（owner doc §L-8/§6）：TransportManager 自动 send（TO_SEND→SENT）+ ACK 超时 24h→ERROR + 自动重试 3 次（指数退避）+ ERROR>24h 升级
2. **实际**：`createOutbound` 留 TO_SEND → **TransportManager wired-but-uncalled**（生产代码零调用）→ **无 ACK-timeout config + 无 Job + 无自动重试 + 无升级** → TO_SEND 永久悬挂（除非运营手工 markSent/markAcknowledged/retry）

❌ **P1-MA2-073**（详 §4）。config-gated b2b-enabled default off + Mock transport Deferred 控制暴露面。

#### 场景 i：b2b ASN 跨域收货

1. 供应商 Webhook 推送 → `handleInboundWebhook`（HMAC 验签 + eventId 去重 + parseToAsn）→ EdiDoc(RECEIVED) + ASN(RECEIVED) → `matchPurchaseOrder`（RECEIVED→MATCHED，PO 查找）→ `createReceiveFromAsn`（config-gated，MATCHED→RECEIVED_TO_STOCK，daoFor(ErpPurReceive/Line) 直写，豁免已登记）

证据：`ErpB2bAsnBizModel.java:89-122,124-187,189-237` + `posting-exemptions.md:43-61`。✅ 跨域写豁免登记 + config-gated。

#### 场景 j：maintenance 工单 happy path + 工时过账失败悬挂（**P1-MA2-074**）

1. 维护计划到期 → `ErpMntDueVisitJob`（cron 每日 01:00，默认 disabled）生成 DRAFT visit → 维护主管 `schedule`（DRAFT→SCHEDULED）→ 维护人员 `start`（SCHEDULED→IN_PROGRESS，equipment→UNDER_MAINTENANCE）→ 消耗备件（出库）→ `complete`（IN_PROGRESS→COMPLETED，equipment→RUNNING）
2. **happy path**：`complete` → `doComplete:163 setStatus(COMPLETED)` → `:176-180 postLabor` 成功 → MAINTENANCE_LABOR 凭证创建
3. **过账失败悬挂**：finance 过账引擎异常（如 ERR_PAYROLL_SUBJECT_NOT_CONFIGURED）→ `MaintenanceLaborPostingDispatcher.tryPost:113-121 catch→LOG.warn→return false` → `complete` 不检查返回值 → **visit COMPLETED 终态 + 无凭证 + posted 永不写入 + 异常被吞** → `cancel` → `reverseLabor`（幂等 safe no-op，无凭证时安全）

⚠️ **P1-MA2-074**（详 §4）。同 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 + projects P1-MA2-068 同型根因。

---

### 2.10 维度「与设计文档一致性」

**裁决：FAIL（4 项 P1 owner doc 契约漂移 + 4 项新 P2 owner doc drift/章节缺失）」

| owner doc 章节 | 代码位置 | 一致性 | 裁决 |
|---------------|----------|--------|------|
| crm §stageId 迁移规则「sequence 单向递增（不能跳级回退）」 | `ErpCrmLeadProcessor.java:24-25,138-143` 允许前移/回退 | ❌ 契约漂移 | **P1-MA2-075** |
| crm §审查提示「阶段迁移 sequence 单向递增约束」 | 同上 | ❌ 未落地 | **P1-MA2-075** |
| crm §7「事件提醒 Job 读取 reminderMinutesBefore」 | `findDueReminders:82-101` 用全局 60-min window，**reminderMinutesBefore 列从不读取** | ❌ 字段死字段 | **P1-MA2-076** |
| cs §1 表「SLA 从创建时开始计时」 | `start:135-136` startDateTime=首次 IN_PROGRESS | ⚠️ 偏离已登记 | owner doc §实现偏离补注 L150 |
| cs §2 L39「reopen 恢复计时（时长累加）」 | `reopen:209-211` 保留 startDateTime | ✅ | ✓（候选 P0 证伪） |
| cs §避免工单滞留 L106-109「NEW>1h/ASSIGNED>2h 升级」 | 未实现 | ❌ Deferred 未落地 | **P2-MA2-067** |
| contract §2 L47「ACTIVE→EXPIRED 系统自动 endDate<now」 | `expire()` 仅手工，无 Job | ❌ 未落地 | **P1-MA2-071** |
| contract §2 L34/L51「NEGOTIATION→TERMINATED 谈判破裂」 | terminate 仅守卫 ACTIVE | ❌ 未实现 | **P1-MA2-072** |
| contract §1（L-5 补）「7 态含 CANCELLED」 | dict 6 态无 CANCELLED | ⚠️ owner doc drift | P2-MA1-027 维持 |
| contract §4「endDate 到达自动创建续期草稿」 | 未实现（无 config + 无 writer） | ⚠️ Deferred | 归 P1-MA2-071 一并 |
| b2b §L-8/§6/§8「自动重试+ACK-timeout+ERROR>24h 升级」 | TransportManager uncalled + 无 Job + 无 config | ❌ 未落地 | **P1-MA2-073** |
| b2b §L-8 自动化 vs README/MFT transport Deferred | state-machine.md 自动化承诺 vs managed-file-transfer.md Non-Goal | ⚠️ 文档内部不一致 | **P2-MA2-068** |
| b2b §2 ASCII 图「TO_CANCEL」 | 无方法迁移到/自 TO_CANCEL | ⚠️ dict 死状态 | **P2-MA2-069** |
| maintenance §适用对象二「6 态」prose vs 表 5 态 | 代码 6 态含 IN_PROGRESS（writer 存在） | ⚠️ owner doc drift | P2-MA1-028 维持 |
| 5 域 state-machine.md 缺独立章节 | crm Event + cs SLA + contract InvoicePlan/Version + b2b ASN + maintenance request/DowntimeEntry 散落各 owner doc | ⚠️ owner doc 章节缺失 | **P2-MA2-070** |

---

## 3. MA1 finding 运行时影响复核（5 域状态机角度）

| Finding ID | 原登记 | 本审计复核（状态机角度） | 终态 |
|-----------|--------|------------------------|------|
| **P1-MA1-009** | todo MR1（crm DECIMAL↔double 7 列） | **状态机角度无升级**——ORM 类型层缺陷（`ErpCrmForecastAccuracy.{commitAccuracy,upsideAccuracy}`/`ErpCrmPriceRule.discountPercent`/`ErpCrmLeadFunnel.avgSalesCycleDays`/`ErpCrmFunnelStageMetrics.{conversionRate,dropOffRate,avgDaysInStage}` stdSqlType=DECIMAL vs stdDataType=double）。这些字段不参与状态机迁移判定（qualify/lose/cancel/moveStage/convert 均不读 DECIMAL 字段决定 status）；漏斗/预测聚合经 sequence 排序读取，浮点精度损失影响报表精度不影响状态机 | **不升级**（维持 todo MR1） |
| **P1-MA1-011/013** | todo MR1（maintenance ErpMntVisit 5 列 propId 缺失 orgId/businessDate/posted/postedAt/postedBy） | **状态机角度无升级**——ORM 规范层缺陷（`app-erp-maintenance.orm.xml:268-272` 5 列无 propId 确认）。状态机守卫（`validateTransition:99-104`/`validateNotTerminal:106-112`）仅读 `visit.status`（propId=6, :258）。`posted`/`postedAt`/`postedBy` **从不被 visit BizModel 读写**（dispatcher 用 ErpFinVoucherBillR 反查判重）；`businessDate` 仅 `MaintenanceLaborPostingDispatcher.buildEvent:179-182` 作 voucherDate fallback，不门控状态迁移 | **不升级**（维持 todo MR1） |
| **P1-MA1-022** | todo MR1（5 域跨域只读 daoFor：mnt MaintenanceLabor/IssuePostingDispatcher + cs Dashboard facade read-only） | **状态机角度无升级**——跨域只读是判重/账套解析/反查/报表副作用，不破坏状态机迁移（异常路径经 @BizMutation 事务回滚覆盖；complete→postLabor→daoFor(ErpFinVoucherBillR) 判重失败→tryPost 吞异常返回 false→visit posted 悬挂 P1-MA2-074 同型交接；complete→doComplete 不跨域读；cancel→reverseLabor 经 Facade）。5 域 production 代码无跨域 daoFor 写直写（跨域写全经 Facade 或豁免登记）。维持 P1-MA1-022 todo MR1 | **不升级**（维持 todo MR1） |
| **P1-MA1-029** | todo MR1（contract ErpCtInvoicePlanBizModel 跨域写半治理） | **状态机角度无升级**——InvoicePlan 状态机迁移（triggerInvoice 仅 ACTIVE 守卫 + SUSPENDED 拦截）合规；跨域写生成 unposted DRAFT 不破坏业务正确性（A2.1 P2P 复核）；半治理（豁免未登记）维持 todo MR1。本审计交叉发现：bc-tier 审计描述「无跨模块写」不准确（实仓 12 处跨域写），但 governance 工件（posting-exemptions.md 登记 RebateSettlement）正确，仅审计文字待 MR1 修正 | **不升级**（维持 todo MR1） |
| **P2-MA1-027** | watch-only（contract CANCELLED owner doc drift） | **维持**——代码 dict 6 态 + 无常量 + 无 writer 实测确认；DRAFT 废弃走 useLogicalDelete。纯 owner doc drift，无运行时影响 | **维持 watch-only** |
| **P2-MA1-028** | watch-only（maintenance request IN_PROGRESS owner doc drift） | **维持**——代码 writer 存在（`ErpMntRequestBizModel.java:122`）+ dict 6 态 + startRepair ACCEPTED→IN_PROGRESS 可达；owner doc 表漏 IN_PROGRESS。纯 owner doc drift，无运行时影响 | **维持 watch-only** |

## 4. 新登记 finding

### P0（0 项）

**零 P0**（5 个候选 P0 经证据证伪或降级）：

1. **「cs SLA 计时恢复累加缺失致违约误判」候选 P0 证伪**：`ErpCsTicketBizModel.reopen:209-211` 保留原 startDateTime 不重置 → duration 在下次 resolve 经 `minutesBetween(startDateTime, now):155-158` 累加重算 → owner doc §2 L39「恢复计时（时长累加）」契约落实。**证伪**——SLA 恢复累加完整落地。（语义注记：reopen 不展期 deadlineDateTime，RESOLVED 等待窗口计入 duration 是更严格解释，归残留风险非缺陷。）

2. **「contract 合同到期 Job 未触发致过期合同仍 ACTIVE」候选 P0 降级 P1-MA2-071**：owner doc §2 L47「系统自动」确实未落地（无 Job）。**不升 P0**：(1) `expire()` 手工 @BizMutation 路径存在（运营可触发）；(2) InvoicePlan 生成发票是 unposted DRAFT（经人工审批可拦截过期合同发票，非 silent posted）；(3) 按 missing-automation 同型裁决范式 P1（与 finance P1-MA2-033 NEVER_OPENED→OPEN missing 同型）；(4) 不破坏业财一致（无 GL 数据错误，仅状态悬挂）。aggravator（InvoicePlan 仍可为过期 ACTIVE 合同生成 DRAFT 发票）记录在 P1 描述中。

3. **「b2b EDI ERROR 无重试/告警闭环致文档悬挂」候选 P0 降级 P1-MA2-073**：owner doc §L-8/§6/§8 自动化控制点确实未落地。**不升 P0**：(1) 整个 b2b 子系统 **config-gated OFF 默认**（`erp-b2b.b2b-enabled` default false）→ 默认 config 零生产暴露；(2) MFT transport 是 **Mock-only Deferred SPI**（真实 AS2/SFTP/FTPS = managed-file-transfer.md Non-Goal）→ 出站自动化属 Deferred transport 集成；(3) 所有状态迁移手工可达（状态机不破坏，仅未自动化）；(4) LOG.warn/error 可见性；(5) 按同型 missing-automation 裁决 P1。

4. **「maintenance 工时过账失败悬挂无告警闭环」候选 P0 降级 P1-MA2-074**：与 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 MANUAL_POST + projects P1-MA2-068 **同型根因**。**不升 P0**：(1) **config-gated 默认 OFF**（`DEFAULT_LABOR_POSTING_ENABLED=false` + `DEFAULT_SPARE_PART_POSTING_ENABLED=false`）→ 默认 config 零回归；(2) LOG.warn/error 可见性；(3) `reverseLabor` 幂等 safe no-op（比 assets P1-MA2-060 不对称更优）；(4) 业财不一致经期末试算平衡人工发现。按同型裁决 P1。

5. **「crm stageId 可逆向跳转」候选 P0 降级 P1-MA2-075**：代码确实允许前移/回退。**不升 P0**：(1) deliberate code design（Javadoc `:24-25`「销售流程中阶段可能反复」显式声明）；(2) 无数据破坏（仅 reporting-metric skew——漏斗/转化率假设 monotonic progression）；(3) owner doc §审查提示契约漂移按同型 P1。按 owner doc 契约漂移裁决范式 P1。

### P1（6 项，目标 MR1）

#### P1-MA2-071 contract EXPIRED 自动到期 Job 缺失 + 续期草稿自动创建缺失（owner doc §2/§4/§7 系统自动契约未落地）

- **位置**：`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtContractBizModel.java:117-125`（expire 仅手工 @BizMutation）+ **全域无 Job 类 + 无 scheduler 注册**（grep 全 `module-contract` 零匹配 Job/scheduler）
- **现象**：owner doc `state-machine.md §2 L47`「ACTIVE→EXPIRED | **系统自动** | endDate<now」+ `§7 L99`「合同到期提醒 | nop-job 定时扫描 endDate」+ `§4 L65`「endDate 到达但合同仍在执行中 | 先标记 EXPIRED，同时自动创建续期草稿（auto-create-renewal-draft 配置）」显式设计系统自动到期 + 续期。**实仓**：(a) `expire()` 仅手工 @BizMutation 接 contractId，无批量扫描；(b) **无 `ErpCt*Job.java` 类 + 无 nop-job 注册 + 无 @CronProvider**（对比 hr 域有 `ErpHrContractExpiryJob` 同型 Job，`module-hr/.../job/ErpHrContractExpiryJob.java:34`）；(c) **无 `erp-ct.auto-create-renewal-draft` config**（`ErpCtConfigs.java:17-54` 仅 volume-discount/rebate/invoiceplan-auto-trigger/settlement-mode/e-signature）；(d) `parentContractId` 字段存在（`app-erp-contract.orm.xml:140`）但 grep 全 `module-contract` `renewal|续期|续签|autoCreateRenewal` **零 Java 代码使用**（仅 ORM/view/i18n）。
- **影响**：生产环境 ACTIVE 合同 endDate 到达后**保持 ACTIVE**（除非运营手工调 expire）。`ErpCtInvoicePlanBizModel.triggerInvoice:71-75` 仅守卫 status==ACTIVE → **过期合同仍可生成发票草稿**（虽 unposted DRAFT 经人工审批可拦截，但 ACTIVE 应在 endDate 后退出被破坏——生命周期不变量破坏）。
- **裁决**：**P1 非 P0**——(1) `expire()` 手工路径存在（运营可触发）；(2) InvoicePlan 生成 unposted DRAFT（经人工审批可拦截，非 silent posted）；(3) 按 missing-automation 同型裁决范式 P1（与 finance P1-MA2-033 NEVER_OPENED→OPEN missing 同型）；(4) 不破坏业财一致（无 GL 数据错误，仅状态悬挂）；(5) aggravator（InvoicePlan 仍可为过期 ACTIVE 生成 DRAFT 发票）记录但经审批管道兜底。
- **修复方式**：MR1 裁决——方案 A（推荐）实现 `ErpCtContractExpiryJob`（cron-gated，扫描 ACTIVE 且 endDate<now 合同 → 批量 expire + config-gated auto-create-renewal-draft 经 parentContractId 关联），对齐 hr `ErpHrContractExpiryJob` 范式 + 单失败隔离 + `app-erp-all/.../erp-ct-contract-expiry.job.yaml` 注册；方案 B owner doc §2 L47 标注「ACTIVE→EXPIRED 经运营手工 expire() 触发，自动到期 Job Deferred」+ §4 L65 删除「自动创建续期草稿」语义或标注 Deferred。

#### P1-MA2-072 contract NEGOTIATION→TERMINATED 迁移缺失（owner doc §2 谈判破裂路径未实现）

- **位置**：`module-contract/erp-ct-service/src/main/java/app/erp/ct/service/entity/ErpCtContractBizModel.java:103-113`（terminate 仅守卫 ACTIVE）+ owner doc `state-machine.md §2 L34/L51`
- **现象**：owner doc §2 L34 ASCII 图「NEGOTIATION ─→ TERMINATED（谈判破裂，终态）」+ L51 迁移表「NEGOTIATION→TERMINATED | 合同管理员 | 谈判破裂，双方确认终止 | 版本归档」+ §3 L58「已进入 NEGOTIATION 或后续态的合同不可作废（CANCELLED），只能 TERMINATED」。代码 `terminate:105-107` 仅守卫 `status==ACTIVE`——NEGOTIATION 合同谈判失败**无状态机出口**（仅经 useLogicalDelete 逻辑删除逃生，但逻辑删除≠TERMINATED 语义：TERMINATED=已生效合同提前终止需归档版本+关联终止协议；NEGOTIATION 失败=未生效合同放弃，owner doc 明确区分两者）。
- **影响**：NEGOTIATION 合同谈判破裂时无合规状态机迁移——合同卡在 NEGOTIATION（或经逻辑删除，丢失 TERMINATED 审计语义）。
- **裁决**：**P1 非 P0**——(1) useLogicalDelete 提供逃生路径（NEGOTIATION 合同可逻辑删除，非真正卡死）；(2) NEGOTIATION 是中间态非终态，谈判失败是低频场景；(3) 按 owner doc 契约漂移裁决范式 P1。属设计契约漂移非数据破坏。
- **修复方式**：MR1 裁决——方案 A（推荐）`terminate` 守卫扩展为 `status∈{ACTIVE,NEGOTIATION}` + NEGOTIATION→TERMINATED 路径（版本归档，无法务协议时注明）；方案 B owner doc §2 L34/L51 删除「NEGOTIATION→TERMINATED」迁移 + 标注「NEGOTIATION 谈判失败经 useLogicalDelete 逻辑删除（非 TERMINATED 语义）」。

#### P1-MA2-073 b2b EDI 出站自动化（自动发送 + ACK-timeout→ERROR + 自动重试 + ERROR>24h 升级）全部缺失（owner doc §L-8/§6/§8 自动化控制点未落地）

- **位置**：`module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/entity/ErpB2bEdiDocBizModel.java:66-101`（createOutbound 留 TO_SEND）+ `TransportManager.java`（wired `app-service.beans.xml:49-50` 但**生产代码零调用**，仅 test `TestErpB2bMftTransport.java:54`）+ **全域无 `ErpB2b*Job.java` + 无 nop-job 注册 + 无 `*.job.xml`** + `ErpB2bConfigs.java` **无 `erp-b2b.ack-timeout-seconds` config**（仅 asn.match-timeout-hours）
- **现象**：owner doc `state-machine.md §L-8（L63）`「自动重试最多 3 次（指数退避），耗尽后保留 ERROR 等待人工介入」+ `§6（L84）`「SENT→ERROR（ACK 超时 `erp-b2b.ack-timeout-seconds` 默认 24h 触发）」+ `§8（L126）`「ERROR 超过 24 小时未处理升级通知」+ §9 场景 C「系统每 30 分钟自动重试，最多 3 次」显式设计自动化控制点。**实仓**：(a) `createOutbound:66-101` 留 TO_SEND，**TransportManager.send 生产代码零调用**（bean comment `app-service.beans.xml:47-48` 称「被 EdiDocBizModel 出站发送委托调用」但代码不匹配——ErpB2bEdiDocBizModel 不注入 TransportManager）；(b) `markSent/markAcknowledged/retry` 全手工；(c) **无 ACK-timeout→ERROR**（无 config + 无 Job 扫描 SENT 超时）；(d) `retry:150-166` 仅手工 + retryCount++ 无自动触发 + 无指数退避；(e) **无 ERROR>24h 升级 Job**。
- **影响**：出站 EDI 文档（TO_SEND/SENT/ERROR）生产环境**无自动化推进**——全部依赖运营手工 markSent/markAcknowledged/retry。ERROR 状态文档可静默悬挂（仅 LOG.warn/error 可见性，无升级通知）。owner doc 设计的异步处理闭环未闭合。
- **裁决**：**P1 非 P0**——(1) **整个 b2b 子系统 config-gated OFF 默认**（`erp-b2b.b2b-enabled` default false，`ErpB2bConfigs.java:9,27`）→ 默认 config 零生产暴露；(2) **MFT transport 是 Mock-only Deferred SPI**（`MockTransportAdapter` 唯一 impl，真实 AS2/SFTP/FTPS = `managed-file-transfer.md` Non-Goal）→ 出站自动化属 Deferred transport 集成范畴，非生产路径回归；(3) 所有状态迁移方法手工可达（markSent/markAcknowledged/retry/cancel/archive）——状态机不破坏，仅未自动化；(4) LOG.warn/error 提供运维可见性；(5) owner doc state-machine.md 自动化承诺 vs README/managed-file-transfer.md transport Deferred **文档内部不一致**（登记 P2-MA2-068）；(6) 按同型 missing-automation 裁决范式 P1。
- **修复方式**：MR1 裁决——方案 A（推荐）owner doc state-machine.md §L-8/§6/§8 标注「出站自动化（auto-send/ACK-timeout/auto-retry/escalation）Deferred——MFT transport 真实对接（AS2/SFTP/FTPS）上线时实现」+ bean comment `app-service.beans.xml:47-48` 修正 + 删除 §9 场景 C「系统每 30 分钟自动重试」自动化语义；方案 B 实现 `ErpB2bEdiOutboundJob`（cron-gated，调用 TransportManager.send 推进 TO_SEND→SENT + ACK-timeout 扫描 SENT→ERROR + 自动重试 + ERROR>24h 升级经 IErpSysNotificationBiz），config-gated `erp-b2b.b2b-enabled` + transport-enabled。

#### P1-MA2-074 maintenance Labor/Issue 过账 tryPost 吞异常悬挂致 posted=false 无告警闭环（同型悬挂）

- **位置**：`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborPostingDispatcher.java:110-122`（tryPost）+ `MaintenanceIssuePostingDispatcher.java:111-120`（tryPost 同型）+ `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/entity/ErpMntVisitBizModel.java:162-180`（complete 无条件 COMPLETED 不检查 postLabor 返回值）
- **现象**：`MaintenanceLaborPostingDispatcher.tryPost:110-122` try/catch 吞所有异常（NopException LOG.warn / 其他 LOG.error）返回 boolean，**不向上传播**。`ErpMntVisitBizModel.complete:71-77` → `doComplete:162-181` 在 `:163 setStatus(COMPLETED) + :171 updateEntity`（**先于过账**）→ `:176-180 if (isPostingEnabled) postLabor(visit)` **不检查返回值**，失败仅 LOG.warn。visit 的 `posted` 列**从不被 BizModel 写入**。`MaintenanceIssuePostingDispatcher.tryPost:111-120`（备件消耗）同型（void return）。悬挂窗口期：visit COMPLETED 终态 + 无 MAINTENANCE_LABOR/MAINTENANCE_ISSUE 凭证 + posted 永不写入 + 异常被吞不进入 finance 过账异常工作台 + **maintenance dispatcher javadoc 不引用 DeferredPostingSweepJob**（与 purchase/sales/assets/inventory/mfg peer dispatcher 不一致）。
- **影响**：与 finance P1-MA2-032（IGNORED 凭证悬挂）+ hr P1-MA2-048（salary 过账悬挂）+ assets P1-MA2-060（Capitalization/Disposal 过账悬挂）+ qa A2.12（MANUAL_POST NCR 过账悬挂）+ projects P1-MA2-068（TimesheetPostingDispatcher tryPost 吞异常）**同型根因**。
- **裁决**：**P1 非 P0**——(1) **config-gated 默认 OFF**（`DEFAULT_LABOR_POSTING_ENABLED=false` `ErpMntConstants.java:54` + `DEFAULT_SPARE_PART_POSTING_ENABLED=false` `:42`）→ 默认 config 零回归；(2) LOG.warn/error 提供运维可见性；(3) `reverseLabor:146-152` 幂等 safe no-op（`IErpFinVoucherBiz.reverse` 平台守护，无凭证时安全 no-op——比 assets P1-MA2-060 reverseApprove 仅 posted=true 时回滚不对称更优）；(4) 业财不一致经期末试算平衡人工发现；(5) aggravator：dispatcher javadoc 不引用 DeferredPostingSweepJob（doc gap vs peers）；(6) 与同型范式按既定裁决 P1；(7) 不破坏 complete/cancel 主路径。按同型裁决。
- **修复方式**：MR1 裁决（与 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 + projects P1-MA2-068 一并整体裁决）——方案 A（推荐）`complete` 检查 postLabor 返回值 + 失败时设 `posted=false` + 派发 `IErpSysNotificationBiz` 告警 + 不进 COMPLETED 终态（保持 IN_PROGRESS）+ 期末结账前置检查扩展至 visit COMPLETED-with-posted=false + dispatcher javadoc 补 DeferredPostingSweepJob 引用；方案 B owner doc `state-machine.md §实现偏离补注` 标注「过账失败吞异常为容错设计，业财不一致经期末试算平衡人工发现」+ posted 字段语义化。触及会计保护区域，修复须独立 plan-audit + 人工确认。

#### P1-MA2-075 crm stageId 单向递增守卫未实现（owner doc §stageId 迁移规则 + §审查提示 契约漂移）

- **位置**：`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/processor/ErpCrmLeadProcessor.java:24-25`（Javadoc 显式「允许前移/回退」）+ `:138-143 doMoveStage`（无 sequence 方向比较）+ `:91-97 validateMovable`（仅守卫 docStatus∈{NEW,QUALIFIED}，无方向）+ `:173-184 requireStage`（仅校验 target stage 存在）+ owner doc crm/`state-machine.md §stageId 迁移规则` + `§审查提示`
- **现象**：owner doc crm/`state-machine.md §2 stageId 迁移规则`「stageId 沿 ErpCrmStage.sequence **递增前移（不能跳级回退）**」+ `§审查提示`「阶段迁移（stageId）的 sequence **单向递增约束**」。代码 `ErpCrmLeadProcessor.java:24-25` Javadoc 显式声明「阶段流转（moveStage）：按 ErpCrmStage#getSequence() **允许前移/回退（销售流程中阶段可能反复）**」+ `:138-143 doMoveStage` 仅 `lead.setStageId(toStage.getId())` 无 sequence 方向比较。代码比 owner doc 更宽松。`FunnelAggregationEngine.java:203,274` 报表按 sequence 排序假设 monotonic progression——阶段回退致漏斗/转化率统计漂移。
- **影响**：crm 漏斗/转化率报表（FunnelAggregationEngine）假设 stageId 单调递增；阶段回退破坏 monotonic progression 不变量 → 转化率/流失率/dropOffRate 统计漂移。owner doc §stageId 迁移规则 + §审查提示 契约漂移。
- **裁决**：**P1 非 P0**——(1) deliberate code design（Javadoc `:24-25` 显式声明「销售流程中阶段可能反复」）；(2) 无数据破坏（仅 reporting-metric skew，非事务/GL 错误）；(3) owner doc §审查提示契约漂移按同型 P1（与 finance P1-MA2-031 + mfg P1-MA2-035/036 + hr P1-MA2-039~042 owner doc 契约漂移同型）。属设计契约漂移非数据破坏。
- **修复方式**：MR1 裁决——方案 A（推荐）`doMoveStage` 增 sequence 方向守卫：`if (toStage.sequence < fromStage.sequence) throw ERR_STAGE_BACKWARD_MOVE`（对齐 owner doc §stageId 单向递增契约）+ config-gated `erp-crm.allow-stage-backward`（默认 false=STRICT 拦截，true=允许回退对齐当前行为）；方案 B owner doc crm/`state-machine.md §stageId 迁移规则` 更新为「stageId 允许前移/回退（销售流程中阶段可能反复，对齐代码 Javadoc）」+ §审查提示 删除「单向递增约束」+ FunnelAggregationEngine 标注「阶段回退时转化率按 sequence 排序近似」。

#### P1-MA2-076 crm Event reminderMinutesBefore 字段死字段（silent functional gap）

- **位置**：`module-crm/model/app-erp-crm.orm.xml:472`（reminderMinutesBefore 列存在）+ `module-crm/erp-crm-service/src/main/java/app/erp/crm/service/entity/ErpCrmEventBizModel.java:82-101`（findDueReminders 用全局 DEFAULT_WINDOW_MINUTES=60 不读 reminderMinutesBefore）+ `module-crm/erp-crm-service/src/main/java/app/erp/crm/service/job/ErpCrmEventReminderJob.java:75-92`
- **现象**：ORM `app-erp-crm.orm.xml:472` ErpCrmEvent `reminderMinutesBefore` 列存在（owner doc crm/state-machine.md §7「事件提醒 Job 读取 PLANNED 事件，按 reminderMinutesBefore 发送通知」）。**实仓**：grep 全 `module-crm/erp-crm-service/src/main` `getReminderMinutesBefore|reminderMinutesBefore` **零匹配**——`ErpCrmEventBizModel.findDueReminders:82-101` 用全局 `DEFAULT_WINDOW_MINUTES=60`（:82）+ `dateTimeBetween("startDateTime", now, now+windowMinutes)`（:97-98）扫描 PLANNED 事件，**从不读取 per-event reminderMinutesBefore**。per-event 自定义提前提醒分钟数（如事件 A 设 reminderMinutesBefore=1440 提前 1 天、事件 B 设 15 提前 15 分钟）**静默忽略**——全部用全局 60 分钟窗口。
- **影响**：用户设置 reminderMinutesBefore 期望自定义提前提醒，实际全部用全局 60 分钟——silent functional gap（持久化字段从未生效）。owner doc §7 契约漂移。
- **裁决**：**P1**——silent functional gap on persisted field（与 hr P1-MA2-047 SalaryPostingDispatcher javadoc drift + posted 死字段同型）。**不破坏状态机主路径**（Event PLANNED→COMPLETED/CANCELLED 状态机完整；提醒是副作用非状态迁移），仅通知时点功能缺陷。
- **修复方式**：MR1 裁决——方案 A（推荐）`findDueReminders` 改为按 per-event reminderMinutesBefore 计算：`dateTimeBetween("startDateTime", now, now+event.reminderMinutesBefore)`（fallback 全局 windowMinutes 当 reminderMinutesBefore 为 null）；方案 B owner doc §7 标注「reminderMinutesBefore 字段预留，本期提醒用全局 windowMinutes=60 窗口」+ ORM 列标注 Deferred。

### P2 watch-only（4 项）

#### P2-MA2-067 cs NEW>1h / ASSIGNED>2h 滞留升级规则未实现 + findSlaWarnings 无 scheduler

- **位置**：owner doc cs/`state-machine.md §避免工单滞留 L106-109` + `ErpCsTicketBizModel.findSlaWarnings:365-383`（无 scheduler 注册）
- **现象**：owner doc §避免工单滞留「NEW 停留超过 1 小时→自动升级通知客服主管 / ASSIGNED 停留超过 2 小时→自动提醒处理人 / IN_PROGRESS 超 deadlineDateTime→触发 SLA 超时升级」。实仓仅第三条（deadline-based）落实（`scanOverdueTickets:339-363`）；前两条（NEW>1h / ASSIGNED>2h）**未实现**（grep 零匹配 logic + 无 config key）。`findSlaWarnings:365-383`（pre-breach 早期预警）`@BizQuery` 暴露但**无 scheduler job 注册**（仅 `erp-cs-sla-scan.job.yaml` 存在覆盖 post-breach）——预警通知仅在手工调用时触发。
- **裁决**：watch-only——软 UX 升级规则缺失（deadline-based SLA 升级已覆盖 breach 场景），归 owner doc Deferred。无运行时影响（工单状态机完整；缺失自动 TODO 升级是 UX 缺陷非数据缺陷）。
- **修复方式**：MR1 顺手——方案 A（推荐）owner doc §避免工单滞留 L106-109 标注「NEW>1h / ASSIGNED>2h 滞留升级 Deferred——归通知 successor（0642-1 范式）」+ 注册 `erp-cs-sla-warning.job.yaml` 调 findSlaWarnings；方案 B 实现 ErpCsTicketStaleScanJob（扫描 NEW>1h/ASSIGNED>2h + 派发 IErpSysNotificationBiz）。

#### P2-MA2-068 b2b state-machine.md 自动化控制点 vs README/MFT transport Deferred 文档内部不一致

- **位置**：owner doc b2b/`state-machine.md §L-8（L63）/§6（L84）/§8（L126）/§9 场景 C（L193-195）` vs `README.md:43,139` + `managed-file-transfer.md` Non-Goal
- **现象**：state-machine.md 显式设计自动化控制点（auto-retry 3 次指数退避 + ACK-timeout 24h→ERROR + ERROR>24h 升级 + 系统每 30 分钟自动重试），但 README.md:43「needsWebService=true 走异步队列」+ :139「erp-b2b.async-send-cron —」+ managed-file-transfer.md「真实 AS2/SFTP/FTPS = follow-up Non-Goal」显式 Deferred transport 层。**文档内部不一致**——state-machine.md 自动化承诺 vs transport Deferred。审查者按 state-machine.md 期望自动化，实际 transport 是 Mock-only Deferred。owner doc 内部不一致，无运行时影响（P1-MA2-073 已捕获实质缺失）。
- **裁决**：watch-only，MR1 顺手——owner doc b2b/state-machine.md §L-8/§6/§8/§9 场景 C 标注「自动化控制点 Deferred——MFT transport 真实对接上线时实现」+ 交叉链接 managed-file-transfer.md Non-Goal。

#### P2-MA2-069 b2b TO_CANCEL dict 死状态

- **位置**：`module-b2b/erp-b2b-meta/src/main/resources/_vfs/dict/erp-b2b/edi-doc-state.dict.yaml:17`（TO_CANCEL）+ `ErpB2bConstants.java:11`（EDI_DOC_STATE_TO_CANCEL 常量）
- **现象**：dict `edi-doc-state` 8 态含 TO_CANCEL + 常量定义，但 `ErpB2bEdiDocBizModel` **无任何方法迁移到/自 TO_CANCEL**（取消直接 TO_SEND/SENT/ERROR→CANCELLED，不经 TO_CANCEL 中间态）。owner doc §2 ASCII 图声明「SENT──(取消请求)──→ TO_CANCEL ──(确认取消)──→ CANCELLED」两步迁移，代码未落地（单步 SENT→CANCELLED）。与 finance P1-MA2-031 + mfg P1-MA2-035/036 + hr P1-MA2-040~043/045 + inv P1-MA2-063 + qa P1-MA2-065 + prj P1-MA2-069 dict 死状态同型，但**危害更轻**（TO_CANCEL 是设计的两步取消中间态，单步取消是功能等价简化，非完全缺失）。
- **裁决**：watch-only——按同型 dict 死状态裁决，但降级 P2（功能等价简化非死状态）。无运行时影响。
- **修复方式**：MR1 顺手——方案 A 实现 TO_CANCEL 两步取消（SENT→TO_CANCEL→CANCELLED）；方案 B（推荐）owner doc §2 ASCII 图标注「取消经单步 SENT→CANCELLED，TO_CANCEL 中间态 Deferred」+ 删除 dict TO_CANCEL 项 + 删除常量。

#### P2-MA2-070 5 域 state-machine.md 缺多状态承载实体独立章节（合并）

- **位置**：`docs/design/{crm,customer-service,contract,b2b,maintenance}/state-machine.md`
- **现象**：5 域 state-machine.md 缺多状态承载实体独立章节——crm（Event 无独立章节，散落 §适用对象二）+ cs（SLA 计时器无独立章节，散落 §1 表 + §实现偏离补注）+ contract（InvoicePlan 跨域写 + ContractVersion 版本状态机无独立章节，散落 §7 + approval-workflow.md）+ b2b（ASN 无独立章节，散落 §适用对象二，但 ErpB2bAsn 4 态 + EdiDoc 8 态关系散落）+ maintenance（request 6 态无独立迁移矩阵表 + DowntimeEntry 时间驱动无独立章节，散落 §适用对象二/三）。与 purchase P2-MA2-053 + sales P2-MA2-056 + mfg P2-MA2-045/047 + hr P2-MA2-047/052 + assets P2-MA2-059 + inv P2-MA2-062 + qa P2-MA2-063 + prj P2-MA2-065 同型（owner doc 缺独立章节）。无运行时影响（每实体状态机经代码 + plan 文件证据可追溯），仅 owner doc 可读性缺陷。
- **裁决**：watch-only，MR1 顺手——方案 A（推荐）各域 state-machine.md 新增缺独立章节（本审计 §2.1-2.5 状态图可直接采用）；方案 B 交叉链接到各 owner doc。

## 5. 并发敏感点（交接 A2.17）

| 敏感点 | 位置 | 风险 | 交接状态 |
|--------|------|------|----------|
| crm ErpCrmLead 并发 qualify/moveStage/convert | `ErpCrmLeadProcessor.qualify/moveStage/convert*` 读-改-写 docStatus/stageId 无显式锁 | 并发 moveStage 可能 silent lost-update（stageId 漂移）；并发 convert 重复创建 ErpMdPartner/ErpSalQuotation（IErpMdPartnerBiz/IErpSalQuotationBiz save 无幂等键） | 交接 A2.17（依赖 ErpCrmLead ORM `versionProp="version"` 透明乐观锁降级为 detectable conflict） |
| crm ErpCrmEvent 并发 complete/cancel + ReminderJob | `ErpCrmEventBizModel.complete/cancel` + `ErpCrmEventReminderJob` 读-改-写 status 无显式锁 | 并发 complete + cancel 可能 silent lost-update | 交接 A2.17（依赖 ErpCrmEvent ORM versionProp 透明乐观锁） |
| cs ErpCsTicket 并发 assign/start/resolve/close/reopen/cancel + SlaScanJob | `ErpCsTicketBizModel.*` + `scanOverdueTickets` 读-改-写 status + SLA 字段无显式锁 | 并发 close + reopen 可能状态漂移；并发 scanOverdueTickets + cancel 可能重复 ESCALATE 审计 | 交接 A2.17（依赖 ErpCsTicket ORM versionProp 透明乐观锁） |
| contract ErpCtContract 并发 activate/suspend/terminate/expire/amend | `ErpCtContractBizModel.*` 读-改-写 status 无显式锁 | 并发 expire + amend 可能 silent lost-update（expire ACTIVE→EXPIRED vs amend ACTIVE→DRAFT） | 交接 A2.17（依赖 ErpCtContract ORM versionProp 透明乐观锁） |
| contract ErpCtInvoicePlan 并发 triggerInvoice 跨域写 | `ErpCtInvoicePlanBizModel.triggerInvoice` daoFor(ErpPurInvoice/Line).saveEntity 无幂等键 | 并发 triggerInvoice 同 InvoicePlan 可能重复创建发票草稿 | 交接 A2.17（无幂等键——业务侧经 ErpPurInvoice.code 唯一性兜底，但 code 生成含 contractCode+planId+sequence 可能冲突） |
| b2b ErpB2bEdiDoc 并发 markSent/retry/cancel | `ErpB2bEdiDocBizModel.*` 读-改-写 state 无显式锁 | 并发 retry + cancel 可能状态漂移 | 交接 A2.17（依赖 ErpB2bEdiDoc ORM versionProp 透明乐观锁） |
| b2b ErpB2bAsn 并发 matchPurchaseOrder/createReceiveFromAsn + Webhook | `ErpB2bAsnBizModel.*` + handleInboundWebhook 读-改-写 status 无显式锁 | 并发 Webhook 重复回调（eventId 去重兜底）+ 并发 matchPurchaseOrder 可能状态漂移 | 交接 A2.17（依赖 ErpB2bAsn ORM versionProp + eventId 去重兜底） |
| maintenance ErpMntVisit 并发 schedule/start/complete/cancel + EquipmentStatusLinker | `ErpMntVisitBizModel.*` + `EquipmentStatusLinker` 读-改-写 status + equipment 状态无显式锁 | 并发 complete + cancel 可能 equipment 状态漂移（restoreToRunning 覆盖）；并发 start 同设备可能重复 UNDER_MAINTENANCE | 交接 A2.17（依赖 ErpMntVisit/ErpMntEquipment ORM versionProp 透明乐观锁 + checkScheduleConflict 防排程重叠） |
| maintenance ErpMntRequest 并发 accept/startRepair/complete/reject/cancel | `ErpMntRequestBizModel.*` 读-改-写 status 无显式锁 | 并发 accept 可能重复生成 DRAFT visit（无幂等键） | 交接 A2.17（依赖 ErpMntRequest ORM versionProp 透明乐观锁） |

## 6. 残留风险

1. **P1-MA2-071 contract EXPIRED Job 缺失**：owner doc §2 系统自动契约未落地。归 MR1 裁决。
2. **P1-MA2-072 contract NEGOTIATION→TERMINATED 缺失**：owner doc §2 谈判破裂路径未实现。归 MR1 裁决。
3. **P1-MA2-073 b2b EDI 自动化缺失**：出站闭环未闭合（config-gated OFF + Mock transport Deferred 控制暴露面）。归 MR1 裁决。
4. **P1-MA2-074 maintenance 过账悬挂**：同 finance/hr/assets/qa/projects 同型根因。归 MR1 整体裁决。
5. **P1-MA2-075 crm stageId 守卫缺失**：owner doc 契约漂移 + reporting skew。归 MR1 裁决。
6. **P1-MA2-076 crm reminderMinutesBefore 死字段**：silent functional gap。归 MR1 裁决。
7. **cs SLA reopen 语义注记**：reopen 不展期 deadline，RESOLVED 等待窗口计入 duration（更严格解释）。归残留风险（owner doc §2 L39「时长累加」语义含糊，非状态机缺陷）。
8. **cs notifySlaOverdue 用 assignedToId 置 escalationUserId 键**：通知模板经 ROLE resolver 解析（设计选择，非缺陷）。
9. **contract bc-tier 审计描述准确性**：bc-tier.md:195/226/248 称 contract「无跨模块写」+ b2b ASN「I*Biz Facade」与代码不符（实仓 12 处 contract 跨域写 + b2b ASN daoFor 直写）。governance 工件正确（posting-exemptions.md 登记两处豁免），仅审计文字待 MR1 修正。
10. **并发敏感点 9 处交接 A2.17**：本审计不做系统性并发正确性裁决。
11. **config-gated / Deferred 偏离**（cs SLA config / contract e-signature SPI / b2b EDI/MFT SPI / maintenance 备件消耗 config / contract 续期 auto-create）：owner doc 已裁定，本审计确认其在状态机上不引入悬挂。

## 7. 裁决

### 7.1 10 维度裁决汇总

| 维度 | 裁决 | 关键证据 |
|------|------|----------|
| 1. 状态定义 | ✅ PASS（含 owner doc drift 注记 + b2b TO_CANCEL 死状态 + cs SLA 起止点偏离已登记） | 5 域 dict 与常量 1:1 对齐；contract CANCELLED drift（P2-MA1-027 维持）+ maintenance IN_PROGRESS drift（P2-MA1-028 维持）+ b2b TO_CANCEL 死状态（P2-MA2-069）+ cs SLA 起止点偏离（owner doc §实现偏离补注已登记） |
| 2. 转换完整性 | ❌ FAIL（4 项新 P1 + cs SLA 联动证伪） | crm Lead/Event + cs Ticket 6 态全迁移 + maintenance visit/request 全迁移齐全；**P1-MA2-071** contract EXPIRED Job 缺失 + **P1-MA2-072** NEGOTIATION→TERMINATED 缺失 + **P1-MA2-073** b2b EDI 自动化缺失 + **P1-MA2-075** crm stageId 守卫缺失；cs SLA 计时联动完整（候选 P0 证伪） |
| 3. 终端与恢复 | ✅ PASS | 5 域终态无出边 + useLogicalDelete 统一 + cs RESOLVED 非终态可 reopen + contract SUSPENDED 非终态可 resume |
| 4. 异常路径 | ❌ FAIL（2 项新 P1 同型悬挂/缺失交接） | crm 重复线索/LOST 原因 + cs SLA 违约/驳回/重复取消/超时关闭守卫 + b2b 防重/解析失败/签名失败 + maintenance 排程冲突/备件补领/DowntimeEntry 全覆盖；**P1-MA2-071** contract 到期 + **P1-MA2-074** maintenance 过账悬挂同型交接 |
| 5. 可达性 | ❌ FAIL（contract EXPIRED 生产不可达 + b2b TO_CANCEL 死状态） | contract EXPIRED 生产不可达（P1-MA2-071）+ b2b TO_CANCEL dict 死状态（P2-MA2-069）；5 域其他状态全可达 |
| 6. 角色与权限 | ✅ PASS | 5 域 owner doc §6 角色绑定齐全 + @BizMutation 入口权限由平台层统一 |
| 7. 外部依赖 | ✅ PASS | 跨域写经 I\*Biz Facade（crm 转化 + maintenance 过账）+ 跨域 daoFor 直写豁免登记（contract InvoicePlan/RebateSettlement + b2b ASN）+ 跨域只读维持 P1-MA1-022 todo MR1；DAG 无环 |
| 8. TODO 任务策略 | ✅ PASS（含 cs 滞留升级部分 Deferred） | 5 域避免沉没设计基本落实；cs NEW>1h/ASSIGNED>2h 滞留升级 + b2b ERROR>24h 升级 + contract 到期提醒 未实现（归 P1/P2 同根因） |
| 9. 场景演练 | ❌ FAIL（10 场景覆盖；场景 f + h + j 暴露 finding） | 场景 a-e + g + i 覆盖 ✓；**场景 f contract 到期**（P1-MA2-071）+ **场景 h b2b EDI 异步**（P1-MA2-073）+ **场景 j maintenance 过账悬挂**（P1-MA2-074）；**场景 d cs SLA 违约+驳回**候选 P0 证伪 |
| 10. 与设计文档一致性 | ❌ FAIL（4 项 P1 owner doc 契约漂移 + 4 项新 P2） | owner doc 多章节被 P1-MA2-071/072/073/075/076 漂移；4 项新 P2 watch-only（P2-MA2-067/068/069/070） |

### 7.2 状态机正确性维度 crm/cs/ct/b2b/mnt 列推进

| 维度（前） | 列（前） | 列（后） | 推进依据 |
|------------|---------|---------|----------|
| 状态机正确性 | crm ❓ / cs ❓ / ct ❓ / b2b ❓ / mnt ❓ | crm **⚠️P1(A2.14✅)** / cs **✅(A2.14✅)** / ct **⚠️P1(A2.14✅)** / b2b **⚠️P1(A2.14✅)** / mnt **⚠️P1(A2.14✅)** | 5 域状态机核心契约经证据逐项确认；**零 P0**（5 个候选 P0 经证据证伪或降级：cs SLA 恢复累加证伪 / contract EXPIRED Job 降级 P1 manual expire 存在 / b2b EDI 自动化降级 P1 config-gated OFF + Mock transport Deferred / maintenance 过账悬挂降级 P1 同型 + config-gated OFF / crm stageId 降级 P1 deliberate design）；**6 项新 P1**（P1-MA2-071 contract EXPIRED Job / P1-MA2-072 contract NEGOTIATION→TERMINATED / P1-MA2-073 b2b EDI 自动化 / P1-MA2-074 maintenance 过账悬挂同型 / P1-MA2-075 crm stageId 守卫 / P1-MA2-076 crm reminderMinutesBefore 死字段）；**4 项新 P2** watch-only（P2-MA2-067 cs 滞留升级 / P2-MA2-068 b2b 文档不一致 / P2-MA2-069 b2b TO_CANCEL 死状态 / P2-MA2-070 5 域缺独立章节）；6 项已登记 MA1 finding（P1-MA1-009/011/013/022/029 + P2-MA1-027/028）运行时复核无升级；9 处并发敏感点交接 A2.17 含 @Version 透明乐观锁降级（5 域全部状态承载实体声明 versionProp） |

### 7.3 Verdict

**Verdict: pass（条件性 → pass after MR1）**——5 域状态机核心契约（crm Lead/Event + cs Ticket/SLA + contract 合同/InvoicePlan + b2b EDI/ASN + maintenance visit/request/DowntimeEntry）经证据逐项确认，**零 P0**（5 个候选 P0 经证据证伪或降级为 P1）。**6 项新 P1 + 4 项新 P2** 已登记待 MR1；6 项已登记 MA1 finding 运行时复核无升级；9 处并发敏感点交接 A2.17。**MR1 修复 P1 后 Verdict 转 pass**（当前为 conditional pass——主路径完整，P1 为契约漂移/同型悬挂/missing-automation/死字段非数据破坏；cs 域 zero P1 候选 P0 证伪）。

**审查范围**：5 域 9 个状态承载实体 + crm 转化/stageId + cs SLA 计时/升级 Job + contract 版本/电子签章/InvoicePlan 跨域写 + b2b EDI 自动化/ASN 跨域收货 + maintenance 过账 Dispatcher/EquipmentStatusLinker/DowntimeEntry + 5 域 state-machine.md owner doc + 2 个 architecture doc。

**可达性摘要**：crm Lead/Event + cs Ticket + contract（除 EXPIRED 生产不可达 P1-MA2-071 + CANCELLED dict 死状态 P2-MA1-027）+ b2b（除 TO_CANCEL dict 死状态 P2-MA2-069）+ maintenance visit/request/DowntimeEntry 全可达。

**角色/权限摘要**：每个迁移绑定执行角色（5 域 owner doc §6）；@BizMutation 入口权限由平台层统一。

**外部依赖摘要**：跨域写经 I\*Biz Facade（crm IErpMdPartnerBiz/IErpSalQuotationBiz + maintenance IErpFinVoucherBiz/IErpInvStockMoveBiz）；跨域 daoFor 直写豁免登记（contract InvoicePlan/RebateSettlement + b2b ASN）；跨域只读维持 P1-MA1-022 todo MR1（maintenance daoFor ErpFinVoucherBillR/ErpInvStockMove/ErpInvStockLedger/ErpMdAcctSchema + cs Dashboard facade read-only 永久接受）。

**剩余风险**：详 §6（11 项，均归 MR1 / Deferred successor / A2.17 / 残留语义注记）。

## 8. 引用

- 审计 plan：`docs/plans/2026-07-28-1020-3-audit-remediation-ma2-ext-domains-state-machine.md`
- 范本（projects A2.13）：`docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`
- 范本（quality A2.12）：`docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`
- 上游 A2.1 P2P（b2b ASN→pur 收货 + ErpCtInvoicePlanBizModel P1-MA1-029 运行时复核）：`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`
- 上游 A2.5a finance 凭证状态机（reverse 红冲同型 + tryPost 吞异常悬挂同型 P1-MA2-032）：`docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`
- 上游 A2.8 purchase（PurReversalListener 不对称同型 P1-MA2-051）：`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`
- 上游 A2.9 sales（Contract reverseApprove + SalReversalListener 同型）：`docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`
- 上游 A2.10 assets（Capitalization/Disposal 过账悬挂同型 P1-MA2-060）：`docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`
- 上游 A2.12 quality（MANUAL_POST NCR 过账悬挂同型）：`docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`
- 上游 A2.13 projects（TimesheetPostingDispatcher tryPost 吞异常同型 P1-MA2-068）：`docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`
- owner docs：`docs/design/{crm,customer-service,contract,b2b,maintenance}/state-machine.md` + `docs/design/customer-service/sla.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
- skill：`docs/skills/state-machine-business-review-prompt.md`
- 矩阵更新：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.2`（状态机正确性 crm/cs/ct/b2b/mnt 列推进）
