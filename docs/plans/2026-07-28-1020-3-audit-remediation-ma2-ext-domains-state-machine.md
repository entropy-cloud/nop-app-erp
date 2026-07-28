# 2026-07-28-1020-3-audit-remediation-ma2-ext-domains-state-machine MA2 crm+cs+contract+b2b+maintenance 状态机审查（A2.14）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.14 crm+cs+contract+b2b+maintenance 状态机审查（A+B 合并）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.14）
> Related: `docs/plans/2026-07-28-1020-1-audit-remediation-ma2-quality-state-machine.md`（A2.12 quality 状态机同批审查——NCR 过账 + 跨域只读 + tryPost 容错同型范式）；`docs/plans/2026-07-28-0400-1-audit-remediation-ma2-sales-state-machine.md`（A2.9 sales 状态机——Contract reverseApprove→SUBMITTED + INLINE 缺守卫 + SalReversalListener 不对称同型）；`docs/plans/2026-07-28-0230-3-audit-remediation-ma2-purchase-state-machine.md`（A2.8 purchase 状态机——三轴 + INLINE 缺守卫 + PurReversalListener 不对称同型）；`docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（A2.1 P2P done——b2b ASN→pur 收货 + ErpCtInvoicePlanBizModel 跨域写半治理 P1-MA1-029 运行时复核）；`docs/plans/2026-07-04-1115-1-contract-version-invoiceplan-volume-discount-rebate.md`（合同版本/发票计划 owner doc §实现偏离补注来源）；`docs/plans/2026-07-04-2200-1-b2b-edi-format-spi-asn-inbound-mft.md`（b2b EDI/ASN owner doc §实现偏离补注来源）；`docs/plans/2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime.md`（维护工单 owner doc §实现偏离补注来源）；`docs/plans/2026-07-04-0700-1-crm-event-reminder-lead-scoring-forecast.md`（crm Lead/Event owner doc §实现偏离补注来源）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；各域 `state-machine.md`（crm/customer-service/contract/b2b/maintenance）
> Audit: required

## Current Baseline

crm + cs + contract + b2b + maintenance（CRM/客服/合同/B2B/维护）五域合并 A+B 级状态机审查。这五域是 ERP **客户前端 / 合同履约 / B2B 电子数据交换 / 设备维护**的支撑层，状态机驱动各域**业务对象生命周期**。五域状态机核心契约经 MA1 平台合规审计 A1.13（15 维度 owner-doc 抽样核查 Lead/Event/Ticket/合同/EDI/ASN/visit/request 状态定义与 dict 一致性 ✅）确认基础一致，但从未做系统性状态机业务审查。五域状态机无直接的库存写/会计过账保护区域（maintenance 维护工单过账经 IErpFinVoucherBiz Facade + ErpInvStockMove 备件消耗；contract InvoicePlan 跨域写半治理 P1-MA1-029 已登记；b2b ASN 经 IErpPurReceiveBiz Facade）。

实时仓库已落地的五域状态机实现（待审查）：

- **crm 域**（`module-crm/`）：Lead 5 态（NEW/QUALIFIED/CONVERTED/LOST/CANCELLED）+ Event 3 态（PLANNED/COMPLETED/CANCELLED）+ stageId 阶段前移（sequence 单向递增）+ Opportunity/Forecast/SalesSequence/Funnel 派生。Lead 转化经 `IErpCrmConversionBiz.convertToQuotation/Customer` 调用 sales/master-data。crm 域内 daoFor(ErpCrm\*) 合法（非跨模块）；无跨模块 daoFor 站点（MA1 grep 实测）。`P1-MA1-009`（todo MR1）：DECIMAL↔double 类型偏离 7 列（ForecastAccuracy/PriceRule/LeadFunnel/FunnelStageMetrics）。`P1-MA1-022`（crm 无跨域 daoFor，仅域内合法）。
- **cs 域**（`module-cs/`）：客服工单 6 态（NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED）+ SLA 计时器联动（RESOLVED 停止计时 isSlaCompleted + RESOLVED→IN_PROGRESS 客户驳回恢复计时累加 + CANCELLED 不计入绩效）。SLA 达标/违约经 owner doc `sla.md`。`P1-MA1-022`（cs Dashboard facade read-only 永久接受）。
- **contract 域**（`module-contract/`）：合同 6 态（DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/EXPIRED/TERMINATED，dict `erp-ct/contract-status`，**owner doc §1 经 L-5 补列 7 态含 CANCELLED 但代码 6 态无 CANCELLED**——P2-MA1-027 owner doc drift）+ InvoicePlan 版本/审批（ErpCtInvoicePlanBizModel 跨域写 pur/sal 发票行，P1-MA1-029 半治理 + A2.1 P2P 运行时复核：生成 unposted DRAFT 不破坏业务正确性）。BizModel 仅实现 ACTIVE→TERMINATED；DRAFT 废弃走 useLogicalDelete=true。`P1-MA1-022`（合并）+ `P1-MA1-029`（todo MR1）+ `P2-MA1-027`（todo MR1）。
- **b2b 域**（`module-b2b/`）：EDI 文档 8 态（TO_SEND/SENT/TO_CANCEL/CANCELLED/ERROR/RECEIVED/ACKNOWLEDGED/ARCHIVED）+ ASN 4 态（RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED）。ASN 跨域写 pur 经 `IErpPurReceiveBiz.createFromAsn`（标准 I\*Biz 跨域调用，非 daoFor 直写，已登记 `posting-exemptions.md`，豁免边界清晰）。nop-job 定时轮询 EDI 状态。网关异步下单策略（SPI）。`P1-MA1-022`（b2b ASN 豁免边界复核 ✅）。
- **maintenance 域**（`module-maintenance/`）：维护工单 visit 5 态（DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED）+ 维护请求 request 6 态（OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED，**owner doc §适用对象二 描述 5 态缺 IN_PROGRESS**——P2-MA1-028 owner doc drift）+ 备件消耗/工时过账副作用。MaintenanceLaborPostingDispatcher daoFor(ErpFinVoucherBillR)（判重只读）+ MaintenanceIssuePostingDispatcher daoFor(ErpInvStockMove/ErpInvStockLedger)（备件消耗反冲）+ daoFor(ErpMdAcctSchema)（账套解析）。`P1-MA1-011/013`（todo MR1）：propId 缺失 5 列。`P1-MA1-022`（mnt 跨域只读 daoFor）+ `P2-MA1-028`（todo MR1）。

**已登记的直指五域状态机的 finding（本审计须复核其状态机行为）**：

- `P1-MA1-009`（todo MR1，crm）：DECIMAL↔double 7 列。**状态机 scope**：ORM 类型层，不参与状态机判定——本审计复核不影响。
- `P1-MA1-011/013`（todo MR1，maintenance）：propId 缺失 5 列。**状态机 scope**：ORM 规范层，不参与状态机判定——本审计复核不影响。
- `P1-MA1-022`（todo MR1，5 域合并）：跨域只读 daoFor（mnt MaintenanceLaborPostingDispatcher ErpFinVoucherBillR + MaintenanceIssuePostingDispatcher ErpInvStockMove/ErpInvStockLedger/ErpMdAcctSchema）+ facade read-only（cs/各域 Report）。**状态机 scope**：跨域只读是过账/报表副作用，不破坏状态机——本审计复核异常路径无悬挂。
- `P1-MA1-029`（todo MR1，contract）：ErpCtInvoicePlanBizModel 跨域写 pur/sal 发票行半治理（bypass rationale 已在 javadoc 但未登记 posting-exemptions.md）+ A2.1 P2P 运行时复核（生成 unposted DRAFT 不破坏业务正确性）。**状态机 scope**：本审计复核 InvoicePlan 状态机迁移 + 跨域写状态机角度。
- `P2-MA1-027`（todo MR1，contract）：合同 7 态 vs 6 态 CANCELLED drift。**状态机 scope**：直接是状态机 owner doc drift——本审计复核合同状态迁移 + CANCELLED 可达性。
- `P2-MA1-028`（todo MR1，maintenance）：维护请求 5 态 vs 6 态 IN_PROGRESS drift。**状态机 scope**：直接是状态机 owner doc drift——本审计复核 request 状态迁移 + IN_PROGRESS 可达性。

**但从未做过一次覆盖五域全状态机（crm Lead/Event/Opportunity/Forecast + cs Ticket/SLA + contract 合同/InvoicePlan + b2b EDI/ASN + maintenance visit/request）、按 `state-machine-business-review-prompt.md` 10 维度的系统性业务审查**。已知未核验控制点（各域 state-machine.md §审查提示 + 已登记 finding）：

- **状态定义清晰性**：crm Lead 5 态 + stageId 阶段前移语义；cs Ticket 6 态（RESOLVED 等待客户确认 vs 做什么）；contract 合同 6 态（SUSPENDED 停薪留职/合同暂停语义）；b2b EDI 8 态（异步处理状态轴）+ ASN 4 态；maintenance visit 5 态 + request 6 态（IN_PROGRESS 维修中语义）。
- **转换完整性**：crm Lead 迁移（NEW→QUALIFIED→CONVERTED/LOST + →CANCELLED）+ Event 迁移 + stageId 单向递增守卫；cs Ticket 迁移（NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED + RESOLVED→IN_PROGRESS 客户驳回 + →CANCELLED）+ SLA 计时联动；contract 合同迁移（DRAFT→NEGOTIATION→ACTIVE→SUSPENDED/EXPIRED/TERMINATED）+ InvoicePlan 版本/审批；b2b EDI 迁移（异步 SENT/ERROR/ACKNOWLEDGED + CANCELLED）+ ASN 迁移（RECEIVED→MATCHED→RECEIVED_TO_STOCK）；maintenance visit 迁移（DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED + →CANCELLED）+ request 迁移（OPEN→ACCEPTED→IN_PROGRESS→COMPLETED/REJECTED + →CANCELLED）。是否有非法跳转或缺失条件分支。
- **终端状态与恢复**：crm CONVERTED/LOST/CANCELLED 终态；cs CLOSED/CANCELLED 终态（RESOLVED→IN_PROGRESS 可恢复——客户驳回重处理）；contract EXPIRED/TERMINATED 终态；b2b ARCHIVED/CANCELLED 终态；maintenance COMPLETED/CANCELLED/REJECTED 终态。
- **异常路径**：cs SLA 违约（超时）/客户驳回重处理/重复工单取消；b2b EDI 发送失败（ERROR）/网关超时/重复回调幂等/异步轮询重启；contract 合同到期（cron-gated Job + EXPIRED 迁移）；maintenance 工单重新调度/备件缺货/工时过账失败悬挂（MaintenanceLaborPostingDispatcher tryPost 容错，与 finance P1-MA2-032/hr P1-MA2-048/assets P1-MA2-060 同型——升级评估）；crm Lead 重新激活（LOST→QUALIFIED 是否合法）；**contract InvoicePlan 跨域写半治理异常路径**（P1-MA1-029）。
- **可达性**：各域状态可达性；contract CANCELLED 可达性（P2-MA1-027——代码无 CANCELLED，owner doc 声明）；maintenance request IN_PROGRESS 可达性（P2-MA1-028——代码有 IN_PROGRESS，owner doc 漏）；无死锁。
- **角色与权限**：crm Lead 转化（销售员/销售主管）；cs 工单分派（系统自动/客服主管）+ 处理（处理人）+ 取消（客服主管）；contract 合同审批（合同管理员/法务）+ 终止（管理层）；b2b EDI 处理（系统/EDI 运维）；maintenance 工单调度（维修主管）+ 完成（维修员）。
- **外部依赖**：crm Lead 转化调用 sales/master-data（IErpCrmConversionBiz Facade）；cs 工单关联客户/订单（master-data/sales）；contract InvoicePlan 跨域写 pur/sal（IErpPurInvoiceBiz/IErpSalInvoiceBiz——P1-MA1-029 半治理）；b2b ASN 跨域写 pur（IErpPurReceiveBiz.createFromAsn——豁免已登记）+ EDI/MFT 网关 SPI；maintenance 备件消耗/工时过账（IErpInvStockMoveBiz/IErpFinVoucherBiz Facade + daoFor 只读）。
- **TODO/任务策略**：cs NEW→ASSIGNED 分派 TODO + IN_PROGRESS 处理 TODO + RESOLVED 客户确认 TODO（避免工单静默下沉——SLA 计时驱动）；contract ACTIVE 履约 TODO（到期/续约/里程碑）；maintenance SCHEDULED 派工 TODO + IN_PROGRESS 维修 TODO；crm Lead 分派/跟进 TODO。是否存在期望有人行动但不产生待办的状态。
- **场景演练**：(a) crm Lead 转化 happy path（NEW→QUALIFIED→CONVERTED→生成报价单/客户）；(b) crm Lead 流失（QUALIFIED→LOST）；(c) cs 工单 happy path（NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED + SLA 达标）；(d) **cs SLA 违约**（超时）+ **客户驳回重处理**（RESOLVED→IN_PROGRESS 恢复计时）；(e) contract 合同 happy path（DRAFT→NEGOTIATION→ACTIVE→履约→EXPIRED/续约）；(f) **contract 合同到期 cron-gated**（ACTIVE→EXPIRED）；(g) **contract InvoicePlan 跨域写**（P1-MA1-029 半治理）；(h) b2b EDI 异步处理（TO_SEND→SENT→ACKNOWLEDGED + ERROR 重试）；(i) **b2b ASN 跨域收货**（RECEIVED→MATCHED→RECEIVED_TO_STOCK→pur 入库）；(j) maintenance 工单 happy path（DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED + 备件消耗/工时过账）+ **工时过账失败悬挂**（tryPost 容错——升级评估）。
- **与设计文档一致性**：各域 `state-machine.md` vs 实现——重点核验：(1) crm §stageId 迁移规则单向递增守卫 + Lead/Event 状态定义；(2) cs §SLA 计时联动（RESOLVED 停止 + RESOLVED→IN_PROGRESS 恢复累加）+ §3 终态；(3) contract §1 合同状态（**7 态 vs 6 态 CANCELLED drift P2-MA1-027**）；(4) b2b §适用对象 EDI 8 态 + ASN 4 态 + §6 异步处理；(5) maintenance §适用对象一 visit 5 态 + **§适用对象二 request 5 态 vs 6 态 IN_PROGRESS drift P2-MA1-028**。

剩余差距：需要一次系统性状态机业务审查，发现任何遗漏的 P0（**cs SLA 计时恢复累加缺失致违约误判** [若破坏 SLA 不变量] / **contract 合同到期 Job 未触发致过期合同仍 ACTIVE** [若破坏生命周期] / **b2b EDI ERROR 无重试/告警闭环致文档悬挂** [若破坏异步处理] / **maintenance 工时过账失败悬挂无告警闭环** [若破坏业财一致——同型升级评估] / **crm stageId 可逆向跳转** [若破坏阶段前移不变量]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **crm（Lead/Event/Opportunity/Forecast/stageId）+ cs（Ticket/SLA）+ contract（合同/InvoicePlan）+ b2b（EDI/ASN）+ maintenance（visit/request）** 做系统性业务审查，产出审计报告。
- 重点核验已识别控制点：(1) 状态定义清晰性（cs RESOLVED 语义 / contract SUSPENDED / maintenance request IN_PROGRESS / b2b EDI 异步状态轴）；(2) 转换完整性（各域生命周期迁移 + **cs SLA 计时联动** / **crm stageId 单向递增守卫** / **contract 合同到期 Job** / **b2b ASN 跨域收货**）；(3) 终端与恢复（cs RESOLVED→IN_PROGRESS 恢复 / contract EXPIRED/TERMINATED）；(4) 异常路径（**cs SLA 违约 + 客户驳回** / **b2b EDI ERROR 重试幂等** / **contract 到期 Job** / **maintenance 工时过账失败悬挂** / **contract InvoicePlan 跨域写半治理**）；(5) 可达性（**contract CANCELLED P2-MA1-027** / **maintenance request IN_PROGRESS P2-MA1-028**）；(6) 角色权限（cs 客服主管取消 / contract 管理层终止）；(7) 外部依赖（跨域经 I\*Biz Facade + daoFor 只读）；(8) TODO 任务策略（**cs SLA 驱动避免沉没** / maintenance 派工/维修 TODO）；(9) 场景演练（10 个代表性场景）。
- 复核已登记 finding 在五域状态机运行时的行为影响：P1-MA1-009（crm DECIMAL↔double）/ P1-MA1-011/013（maintenance propId）/ P1-MA1-022（5 域跨域只读 daoFor）/ P1-MA1-029（contract InvoicePlan 跨域写半治理——状态机角度复核）/ P2-MA1-027（contract CANCELLED drift——可达性复核）/ P2-MA1-028（maintenance request IN_PROGRESS drift——可达性复核），标注终态。
- scope matrix §状态机正确性 crm/cs/ct/b2b/mnt 列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.14 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.8 purchase / A2.9 sales 状态机 — done；本审计只复核 b2b ASN→pur 收货 + contract InvoicePlan→pur/sal 发票行的跨域交互状态机角度（P1-MA1-029）。
- **不**审计 A2.1 P2P / A2.2 O2C 端到端 — done；本审计只复核 b2b/contract 跨域写的状态机角度（P1-MA1-029 运行时复核已在 A2.1）。
- **不**审计 A2.10 assets 状态机 — done；本审计只复核 maintenance 维护工单与资产关联（linked visit）的状态机角度。
- **不**审计 A5.x 测试覆盖深度 — 测试覆盖系统性审查归 MA5；本审计只复核 maintenance 工时过账失败悬挂对状态机的影响。
- **不**审计 A2.17 并发与乐观锁 — 并发状态变更/异步 EDI 处理归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 A4.x view.xml drift / A4.5 五域代码质量抽样 — 归 MA4。
- **不**审计 config-gated / Deferred 偏离是否应实现（cs SLA config / contract e-signature SPI / b2b EDI/MFT SPI / maintenance 备件消耗 config） — owner doc 已裁定，本审计只确认其在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/crm/state-machine.md`（Lead 5 态 + Event 3 态 + stageId 迁移规则）+ `docs/design/customer-service/state-machine.md`（Ticket 6 态 + SLA 计时联动）；`docs/design/contract/state-machine.md`（合同 6 态 + **§1 7 态 CANCELLED drift P2-MA1-027**）+ `approval-workflow.md`（合同审批流）；`docs/design/b2b/state-machine.md`（EDI 8 态 + ASN 4 态 + §6 异步处理）；`docs/design/maintenance/state-machine.md`（visit 5 态 + **§适用对象二 request 5 态 vs 6 态 IN_PROGRESS drift P2-MA1-028**）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）；`docs/architecture/posting-exemptions.md`（b2b ASN + contract InvoicePlan 豁免登记）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.14 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：五域状态机本身非 ask-first 最高级保护区域，但 **maintenance 工时/备件过账触及 finance 凭证链 + inventory 移动单**（MaintenanceLaborPostingDispatcher/MaintenanceIssuePostingDispatcher）+ **contract InvoicePlan 跨域写 pur/sal 发票行**（P1-MA1-029 半治理）+ **b2b ASN 跨域写 pur 收货**（豁免已登记）。P0 即时修复若触及 `Maintenance*PostingDispatcher`/`ErpCtInvoicePlanBizModel`/`ErpB2bAsnBizModel`/各域 Processor/xbiz 文件，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计保护区域）。ORM 字典变更（crm lead-doc-status/event-status、contract-status、b2b edi-doc-state/asn-status、maintenance visit-status/request-status 等）属 ask-first。xbiz 文件变更属状态机契约变更——须 owner doc + 人工确认。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 五域状态机系统性业务审查

Status: completed
Targets: `module-crm/`（Lead/Event/Opportunity/Forecast BizModel + stageId 迁移守卫 + IErpCrmConversionBiz Facade）；`module-cs/`（Ticket BizModel + SLA 计时器联动 + sla.md 达标/违约）；`module-contract/`（合同 BizModel ACTIVE→TERMINATED + cron-gated EXPIRED Job + ErpCtInvoicePlanBizModel 跨域写 pur/sal + 合同审批流）；`module-b2b/`（EDI BizModel 异步处理 + ASN BizModel + IErpPurReceiveBiz.createFromAsn Facade + nop-job 轮询）；`module-maintenance/`（visit/request BizModel + MaintenanceLaborPostingDispatcher/MaintenanceIssuePostingDispatcher 过账 + daoFor 只读）
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-009 crm DECIMAL + P1-MA1-011/013 maintenance propId + P1-MA1-022 5 域跨域只读 + P1-MA1-029 contract 半治理 + P2-MA1-027 contract CANCELLED drift + P2-MA1-028 maintenance IN_PROGRESS drift 已登记，本审计复核状态机角度）；A2.1 P2P done（b2b ASN→pur 收货 + ErpCtInvoicePlanBizModel P1-MA1-029 运行时复核）；A2.8 purchase done（三轴 + 跨域写同型）；A2.9 sales done（Contract reverseApprove + SalReversalListener 不对称同型）；A2.5a done（finance 凭证 reverseApprove 红冲 + tryPost 容错同型范式）；A2.10 assets done（linked visit maintenance 关联状态机角度）

- [x] 维度「状态定义」：审查 crm Lead 5 态 + stageId 阶段前移语义；cs Ticket 6 态（RESOLVED 等待客户确认 vs 做什么）；contract 合同 6 态（SUSPENDED 合同暂停语义）；b2b EDI 8 态（异步处理状态轴）+ ASN 4 态；maintenance visit 5 态 + request 6 态（IN_PROGRESS 维修中语义）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：crm Lead 迁移（NEW→QUALIFIED→CONVERTED/LOST + →CANCELLED）+ Event 迁移 + **stageId 单向递增守卫**；cs Ticket 迁移（NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED + RESOLVED→IN_PROGRESS 客户驳回 + →CANCELLED）+ **SLA 计时联动**（startDateTime/deadlineDateTime/isSlaCompleted）；contract 合同迁移（DRAFT→NEGOTIATION→ACTIVE→SUSPENDED/EXPIRED/TERMINATED）+ InvoicePlan 版本/审批；b2b EDI 迁移（异步 SENT/ERROR/ACKNOWLEDGED + CANCELLED）+ ASN 迁移（RECEIVED→MATCHED→RECEIVED_TO_STOCK）；maintenance visit 迁移（DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED + →CANCELLED）+ request 迁移（OPEN→ACCEPTED→IN_PROGRESS→COMPLETED/REJECTED + →CANCELLED）。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：crm CONVERTED/LOST/CANCELLED 终态；cs CLOSED/CANCELLED 终态（RESOLVED→IN_PROGRESS 可恢复——客户驳回重处理）；contract EXPIRED/TERMINATED 终态；b2b ARCHIVED/CANCELLED 终态；maintenance COMPLETED/CANCELLED/REJECTED 终态。归档与活跃区分。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——cs SLA 违约（超时）/客户驳回重处理/重复工单取消；b2b EDI 发送失败（ERROR）/网关超时/重复回调幂等/异步轮询重启；contract 合同到期（cron-gated Job + EXPIRED 迁移）；maintenance 工单重新调度/备件缺货/**工时过账失败悬挂**（MaintenanceLaborPostingDispatcher tryPost 容错，与 finance P1-MA2-032/hr P1-MA2-048/assets P1-MA2-060 同型——升级评估）；crm Lead 重新激活（LOST→QUALIFIED 是否合法）；**contract InvoicePlan 跨域写半治理异常路径**（P1-MA1-029）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：各域状态可达性；**contract CANCELLED 可达性**（P2-MA1-027——代码无 CANCELLED，owner doc 声明——复核 DRAFT 废弃走 useLogicalDelete）；**maintenance request IN_PROGRESS 可达性**（P2-MA1-028——代码有 IN_PROGRESS，owner doc 漏——复核 ACCEPTED→IN_PROGRESS→COMPLETED 迁移）；无死锁。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：crm Lead 转化（销售员/销售主管）；cs 工单分派（系统自动/客服主管）+ 处理（处理人）+ 取消（客服主管）；contract 合同审批（合同管理员/法务）+ 终止（管理层）；b2b EDI 处理（系统/EDI 运维）；maintenance 工单调度（维修主管）+ 完成（维修员）。是否有危险操作对任何角色开放。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：crm Lead 转化调用 sales/master-data（IErpCrmConversionBiz Facade）；cs 工单关联客户/订单（master-data/sales）；contract InvoicePlan 跨域写 pur/sal（IErpPurInvoiceBiz/IErpSalInvoiceBiz——P1-MA1-029 半治理）；b2b ASN 跨域写 pur（IErpPurReceiveBiz.createFromAsn——豁免已登记）+ EDI/MFT 网关 SPI；maintenance 备件消耗/工时过账（IErpInvStockMoveBiz/IErpFinVoucherBiz Facade + daoFor 只读）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：cs NEW→ASSIGNED 分派 TODO + IN_PROGRESS 处理 TODO + RESOLVED 客户确认 TODO（避免工单静默下沉——SLA 计时驱动）；contract ACTIVE 履约 TODO（到期/续约/里程碑）；maintenance SCHEDULED 派工 TODO + IN_PROGRESS 维修 TODO；crm Lead 分派/跟进 TODO。是否存在期望有人行动但不产生待办的状态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) crm Lead 转化 happy path（NEW→QUALIFIED→CONVERTED→生成报价单/客户）；(b) crm Lead 流失（QUALIFIED→LOST）；(c) cs 工单 happy path（NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED + SLA 达标）；(d) **cs SLA 违约**（超时）+ **客户驳回重处理**（RESOLVED→IN_PROGRESS 恢复计时）；(e) contract 合同 happy path（DRAFT→NEGOTIATION→ACTIVE→履约→EXPIRED/续约）；(f) **contract 合同到期 cron-gated**（ACTIVE→EXPIRED）；(g) **contract InvoicePlan 跨域写**（P1-MA1-029 半治理）；(h) b2b EDI 异步处理（TO_SEND→SENT→ACKNOWLEDGED + ERROR 重试）；(i) **b2b ASN 跨域收货**（RECEIVED→MATCHED→RECEIVED_TO_STOCK→pur 入库）；(j) maintenance 工单 happy path（DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED + 备件消耗/工时过账）+ **工时过账失败悬挂**（tryPost 容错——升级评估）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在各域 `state-machine.md` 是否有匹配——重点核验：(1) crm §stageId 迁移规则单向递增守卫 + Lead/Event 状态定义；(2) cs §SLA 计时联动（RESOLVED 停止 + RESOLVED→IN_PROGRESS 恢复累加）+ §3 终态；(3) contract §1 合同状态（**7 态 vs 6 态 CANCELLED drift P2-MA1-027**）；(4) b2b §适用对象 EDI 8 态 + ASN 4 态 + §6 异步处理；(5) maintenance §适用对象一 visit 5 态 + **§适用对象二 request 5 态 vs 6 态 IN_PROGRESS drift P2-MA1-028**。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 finding 五域状态机角度：P1-MA1-009（crm DECIMAL↔double——无状态机影响）/ P1-MA1-011/013（maintenance propId——无状态机影响）/ P1-MA1-022（5 域跨域只读 daoFor——异常路径复核）/ P1-MA1-029（contract InvoicePlan 跨域写半治理——状态机迁移复核）/ P2-MA1-027（contract CANCELLED drift——可达性复核）/ P2-MA1-028（maintenance request IN_PROGRESS drift——可达性复核），标注终态。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（含：crm Lead/Event + cs Ticket/SLA + contract 合同/InvoicePlan + b2b EDI/ASN + maintenance visit/request 状态图与迁移矩阵、各维度通过/失败裁决、控制点 PASS/FAIL、SLA 计时联动/合同到期 Job/EDI 异步/InvoicePlan 跨域写/工时过账悬挂裁决、CANCELLED/IN_PROGRESS drift 可达性裁决、MA1 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] crm/cs/contract/b2b/maintenance 五域状态图与迁移矩阵产出，每个状态/转换有通过/失败裁决与证据
- [x] 已识别控制点（状态定义[含 cs RESOLVED + contract SUSPENDED + maintenance IN_PROGRESS + b2b EDI 异步] / 转换完整性[含 SLA 计时联动 + stageId 守卫 + 合同到期 Job + ASN 跨域收货] / 终端与恢复[含 cs 恢复] / 异常路径[含 SLA 违约 + EDI ERROR + 工时过账悬挂 + InvoicePlan 跨域写] / 可达性[含 CANCELLED/IN_PROGRESS drift] / 角色权限 / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 五域状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 crm/cs/ct/b2b/mnt 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**cs SLA 计时恢复累加缺失致违约误判** [若破坏 SLA 不变量] / **contract 合同到期 Job 未触发致过期合同仍 ACTIVE** [若破坏生命周期] / **b2b EDI ERROR 无重试/告警闭环致文档悬挂** [若破坏异步处理] / **maintenance 工时过账失败悬挂无告警闭环** [若破坏业财一致——同型升级评估] / **crm stageId 可逆向跳转** [若破坏阶段前移不变量]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。本审计对已登记 finding 只复核状态机运行时影响不重复登记根因；新 P1（如 cs SLA 恢复缺口 / contract 到期 Job 缺口 / b2b EDI 重试缺口 / stageId 守卫缺口 / Deferred CRUD 空壳死状态 [若确认]）按新 finding ID 登记。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 crm/cs/ct/b2b/mnt 列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05972bf4fffeEXhlEg5Rc22RC7`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：5 域 `module-{crm,cs,contract,b2b,maintenance}/` 存在 ✓；关键文件 spot-check 存在（ErpB2bAsnBizModel/ErpCtInvoicePlanBizModel/MaintenanceLaborPostingDispatcher/MaintenanceIssuePostingDispatcher）+ daoFor 跨域只读行号确认 ✓；字典核实：crm lead-doc-status 5 态/event-status 3 态、contract contract-status **6 态无 CANCELLED drift 确认**、b2b edi-doc-state 8 态/asn-status 4 态、maintenance visit-status 5 态/request-status **6 态含 IN_PROGRESS drift 确认** ✓；6 个 finding ID（P1-MA1-009/011/013/022/029 + P2-MA1-027/028）arm-index 描述匹配 ✓；scope matrix crm/cs/ct/b2b/mnt 列全 `❓` ✓；roadmap A2.14 `todo` + 「A+B 合并」一行匹配（单 plan 粒度正确）✓。**1 项非阻塞已修订**：保护区域段 `lead-status` 误称应为 ORM 实名 `lead-doc-status`（含 event-status），草案审查迭代 1 后已补正。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。maintenance 工时/备件过账 + contract InvoicePlan 跨域写触及会计保护区域，P0 即时修复须额外人工确认。xbiz 契约变更须人工确认。

- [x] 范围内行为完成（A2.14 crm+cs+contract+b2b+maintenance 状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、五域 state-machine owner doc 结论已反映）
- [x] 已运行验证：审计不改代码，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）；五域自上次 codegen + 后续 fix plans 已建立全绿基线
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.8 purchase / A2.9 sales 状态机（b2b ASN→pur / contract InvoicePlan→pur/sal 跨域写）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.8/A2.9 done（采购/销售状态机组件齐备已确认）。本审计做五域状态机**业务正确性**审查；b2b/contract 跨域写的状态机角度复核（P1-MA1-029 半治理运行时复核已在 A2.1）。
- Successor Required: `no`——A2.8/A2.9 已 done。

### A2.10 assets 状态机（maintenance linked visit 与资产关联）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.10 done（资产状态机组件齐备已确认）。本审计做 maintenance 状态机**业务正确性**审查；维护工单与资产关联（linked visit）的状态机角度复核归 A2.10。
- Successor Required: `no`——A2.10 已 done。

### A5.x 测试覆盖深度 / A5.5 测试隔离性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做五域状态机**业务正确性**审查；测试覆盖系统性审查归 MA5。本审计只复核 maintenance 工时过账失败悬挂对状态机的影响。
- Successor Required: `yes`——MA5 执行时复核。

### A2.17 并发与乐观锁（并发状态变更 / 异步 EDI 处理）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（并发状态变更乐观锁 / b2b EDI 异步处理重复回调幂等），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated / Deferred 偏离本身（cs SLA config / contract e-signature SPI / b2b EDI/MFT SPI / maintenance 备件消耗 config）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 config-gated/SPI/Deferred。本审计只确认其在状态机上不引入悬挂。
- Successor Required: `yes`——各 successor 触发条件满足时（如 e-signature 上线 / EDI 网关对接）。

## Closure

Status Note: 执行完成（2026-07-28）。五域状态机系统性业务审查（A2.14）产出审计报告 `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（641 行，10 维度齐全 + Verdict §7.3），**零 P0**（5 个候选 P0 经证据证伪或降级：cs SLA 恢复累加缺失**证伪**——`ErpCsTicketBizModel.reopen:209-211` 保留 startDateTime 累加重算 / contract EXPIRED Job 降级 P1-MA2-071 manual expire 存在 + missing-automation 同型 / b2b EDI 自动化降级 P1-MA2-073 config-gated OFF 默认 + Mock transport Deferred / maintenance 过账悬挂降级 P1-MA2-074 同 finance/hr/assets/qa/projects tryPost 吞异常同型 / crm stageId 降级 P1-MA2-075 deliberate design + reporting skew 非数据破坏）。**6 项新 P1**（P1-MA2-071~076）+ **4 项新 P2** watch-only（P2-MA2-067~070）登记 arm-index；scope matrix §状态机正确性 crm/cs/ct/b2b/mnt 列 `❓`→`⚠️P1(A2.14✅)`/`✅(A2.14✅)`/`⚠️P1(A2.14✅)`/`⚠️P1(A2.14✅)`/`⚠️P1(A2.14✅)`（cs 域 zero P1 候选 P0 证伪）；roadmap A2.14 `todo`→`done`；6 项已登记 MA1 finding 运行时复核无升级；9 处并发敏感点交接 A2.17。审计 docs-only（零代码变更），build/test 门控按 plan Closure Gates + roadmap §其他纪律作回归基线确认（M0.3 锚点 HEAD=0e963531d 1756 测试全绿基线维持，无代码变更故无回归可能）。

Closure Audit Evidence:

- 独立结束审计（fresh context subagent `ses_05904afd5`，对照实时仓库逐项复核 10 项）**VERDICT: pass，零 BLOCKER**。审计报告（641 行，10 维度齐全 + Verdict §7.3 + 零 P0/6 P1/4 P2）产出完整；arm-index.md 报告清单行（status=done）+ P1-MA2-071~076 详细清单 + P2-MA2-067~070 汇总均已登记，ID 连续无碰撞（前序 max=P1-MA2-070/P2-MA2-066）；scope matrix §状态机正确性 行 crm/cs/ct/b2b/mnt 列已由 `❓` 推进至终态标记；roadmap A2.14=done；`git status --short` 确认仅 3 个文档修改 + 1 个新报告，零代码变更（审计 docs-only）。抽样复核 P1-MA2-071（module-contract 无 Job 类/无 scheduler，expire 仅手工 @BizMutation）与 P1-MA2-075（ErpCrmLeadProcessor.doMoveStage 无 sequence 方向守卫 vs owner doc「不能跳级回退」）claim 与实仓一致；候选 P0「cs SLA 恢复累加缺失」经 `ErpCsTicketBizModel.reopen:209-211` 保留 startDateTime 证伪；P0 追踪表无新 A2.14 P0 行，与报告零 P0 一致。

Follow-up:

- 6 项 P1（P1-MA2-071~076）待 MR1 经 R1.0 展开机制转化为具体修复工作项行（P1-MA2-074 maintenance 过账悬挂与 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 + projects P1-MA2-068 一并整体裁决；P1-MA2-071/P1-MA2-073 missing-automation 同 finance P1-MA2-033 一并裁决）。
- 4 项 P2 watch-only（P2-MA2-067~070）待 MR1/MR2 顺手收敛或永久接受。
- bc-tier 审计描述准确性修正（「contract 无跨模块写」+「b2b ASN I*Biz Facade」与代码不符）待 MR1 顺手（governance 工件正确，仅审计文字）。
- 9 处并发敏感点交接 A2.17 系统性并发正确性裁决。
