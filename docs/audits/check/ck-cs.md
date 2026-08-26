# ck-cs — cs（customer-service 客服域）实现代码检查报告

> 工作项：C7.1。执行日期：2026-08-26。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-cs/erp-cs-service/src/main/java` 全部 55 个手写生产文件（约 8,363 行）——工单生命周期（`entity/ErpCsTicketBizModel` 666 行 + `statemachine/ErpCsTicketStateMachine` 165 行 + resolve/reopen/scanOverdue/matchAndAttachSla/escalateToQuality 5 个 Processor）、SLA 链（`SlaDeadlineCalculator` / `SlaPolicyMatcher` / `TicketPriorityRank`）、满意度链（`ErpCsSurveyBizModel` + `ErpCsSurveyCreateSurveyProcessor` + `NpsClassifier` / `SurveyTokenGenerator`）、知识库（`ErpCsKnowledgeBaseBizModel`）、预设应答（`CannedResponseRenderer` + BizModel + ApplyProcessor）、权益/保修（`ErpCsEntitlementBizModel` + `EntitlementMatcher` + `MatchAndAttachSlaProcessor` / `DeactivateProcessor`）、服务目录/履行引擎（`CreateFromCatalogProcessor` + `ExecuteFulfillmentStepsProcessor` 873 行 + `ErpCsFulfillmentRetryJob`）、计时子系统（TimerSession 4 Processor + `TimerSessionOps` + `TimerSessionCalculator` + `ErpCsTimeEntryBizModel` + `CsTicketMonthSeqCodeRuleVariable`）、6 个 job、看板（`ErpCsQualityDashboardBizModel` 409 行）与报表（`ErpCsReportBizModel` 324 行）+ 配置/常量/错误码。跨文件核实：`module-cs/model/app-erp-cs.orm.xml`（versionProp/UK/dict/列集）、`erp-cs-service/_vfs/erp/cs/beans/app-service.beans.xml`（27 bean）、`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-cs-*.job.yaml`（6 个）、`erp-cs-meta` ticket-status 等 15 个 dict + `ErpCsTicket.xmeta`（biz:codeRule autoExpr）、`app-erp-all/_vfs/_init-data/erp_sys_notification_template.csv`（cs.* 模板种子）、E2E `tests/e2e/reports/cs-ticket-sla-csat.value.spec.ts`（数值断言口径）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。55 文件核心链逐行深读（4 个裸 stub BizModel 桩确认）+ 平台源码实证 3 处（`FilterBeans.like` 直传不自动包 `%`、`CrudBizModel.get(id, ignoreUnknown)` 非锁先例沿用 ck-crm-lead 实证、`ErpSysNotificationNotifyProcessor` 无 ACTIVE 模板时 WARN 静默跳过）+ orm/beans/job-yaml/dict/xmeta/通知模板种子接线核对 + arm-index cs 14 条 finding 逐条裁决。
> 切片边界：`erp-cs-web` AMIS 页面契约 drift 归 C8.2（本报告仅抽查 status badge 死值消费）；api 骨架 beans 不深查；测试代码仅用于行为语义交叉验证。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-cs-001（D6/D8）matchAndAttachSla 权益级 SLA 覆盖被策略匹配无条件覆写 + deadline 与 slaPolicyId 来源错配 + 无 hours/days 策略 NPE

- **控制点**：`app/erp/cs/service/processor/ErpCsTicketMatchAndAttachSlaProcessor.java#matchAndAttachSla`（L42-66：`if (matched != null) { if (matched.getSlaPolicyId() != null) ticket.setSlaPolicyId(...); applyEntitlementSlaOverride(ticket, matched); }` → `applyEntitlementSlaOverride` L101-108 注释自称「权益级覆盖优先于 SLA 策略计算的 deadline（entitlement.md §三 优先级 1）」并写 `ticket.setDeadlineDateTime(now + maxResolutionTime)` → **随后** L52-63 `policy = SlaPolicyMatcher.match(...)` 命中任何策略时 L62-63 `LocalDateTime deadline = SlaDeadlineCalculator.calculate(now, policy); ticket.setDeadlineDateTime(Timestamp.valueOf(deadline));` **无条件覆写**）
- **证据**：三个子症状：① **权益 maxResolutionTime 覆盖失效**——entitlement.md §三 SLA 优先级表声明「1 = entitlement.maxResolutionTime（不为空时）权益级覆盖 > 2 = entitlement.slaPolicyId > 3 = ticketType 默认」，但只要存在任何可匹配策略（SlaPolicyMatcher 含 teamId IS NULL + ticketTypeId IS NULL 的通用兜底，极易命中），权益覆盖 deadline 即被策略 deadline 覆写——覆盖仅在「无任何策略匹配」的角落存活，与注释声明正好相反；② **deadline 与 slaPolicyId 来源错配**——slaPolicyId 已写为权益策略（L45-47 先写、L59-61 仅 null 时补），但 deadline 按 **matcher 匹配到的另一条策略** 计算：两者可指向不同策略的不同 resolveHours，工单挂 A 策略却按 B 策略计时（违反优先级 2「entitlement.slaPolicyId → resolveHours」）；③ **NPE**——`SlaDeadlineCalculator.calculate` 明确契约「策略无 hours/days 配置返回 null」（`SlaDeadlineCalculator.java` L21/L43-45），`Timestamp.valueOf(deadline)` 对 null 抛裸 NPE：手动 `ErpCsTicket__matchAndAttachSla` mutation 直接 500（非 NopException）；创建自动路径被 `enrichAfterCreate` 的 try/catch 降级吞掉（ErpCsTicketBizModel L134-139），但结果是策略挂载失败 + 权益已扣减（`matchAndConsumeEntitlement` 先于 calculate 执行）——**扣了权益没挂上 SLA**。
- **问题**：SLA 核心循环（工单→权益→截止时间）对「有权益客户」系统性计算错误；无 hours/days 策略配置触发 NPE/降级悬挂。D6 计算正确性 + D8 权益-策略联动一致性。
- **建议修复方向**：重排计算序——权益 slaPolicyId 命中时**直接按该策略**计算 deadline（或完全跳过 matcher）；maxResolutionTime 非空时最终以权益覆盖值为准（任何策略计算之后应用）；calculate 返回 null 时跳过 deadline 写入（保持 null）而非 `Timestamp.valueOf(null)`。
- **arm-index 裁决**：新增（grep arm-index「maxResolutionTime 覆盖/matchAndAttachSla 覆写」零命中；A1.40 cs-F4 对 UC-CS-09 的 RC finding 只覆盖「扣减集成缺失」维度且已修复 RC-R1.65/1430-1，未覆盖覆盖序错配）。

### P1-CK-cs-002（D6）CSAT/NPS 聚合分母包含未响应调查（评分 null 按 0 填充）——看板与报表双站点均分系统性偏低，E2E 数值断言无法捕获

- **控制点 A**：`app/erp/cs/service/dashboard/ErpCsQualityDashboardBizModel.java#loadSurveyByTicket`（L306-326：`q.addFilter(in("ticketId", ticketIds)); List<ErpCsSurvey> surveys = daoProvider.daoFor(ErpCsSurvey.class).findAllByQuery(q);` —— **无 respondedAt/status 过滤**；L321-323 `a.csatSum += nz(s.getCsatScore())` 其中 `nz(null)=0`）→ `#getAgentCsatBreakdown` L243-245 `avg(a.surveyCount, a.csatSum)` 以 `surveyCount`（含未响应）为分母
- **控制点 B**：`app/erp/cs/service/report/ErpCsReportBizModel.java#aggregateSurveys`（L253-268：同样 `in("ticketId", ...)` 无 respondedAt 过滤，`nz(s.orm_propValueByName("csatScore"))` null→0）→ `#buildTicketSlaCsatSummaryDataset` L224-229 `a.csatSum.divide(new BigDecimal(a.surveyCount), ...)`
- **证据**：csat.md §4.3 查询示例逐字 `WHERE s.respondedAt IS NOT NULL`、§4.1「CSAT 平均分 = AVG(csatScore)」语境为已响应调查。实现把 `createSurvey` 在 resolve 时刻即创建的 PENDING/SENT 行（`csatScore=null`）全部计入分母且分子按 0 填充：1 条已响应 5 分 + 9 条未响应 → avgCsat=0.5 而非 5.0。E2E `tests/e2e/reports/cs-ticket-sla-csat.value.spec.ts` 断言 `'5.00','9.00'` 之所以通过，是因为种子数据恰好只有已响应调查——聚合口径缺陷对 E2E 不可见（任务点名「E2E 已覆盖 CSAT 报表数值断言——验证聚合口径」核查结论：**断言在但口径错，种子掩盖**）。`surveyCount` 语义同步漂移（计未响应）。NPS 同病（`a.npsSum += nz(...)`）。
- **问题**：满意度报表（客服个人 CSAT 看板 + SLA/CSAT 综合报表）数值系统性失真，未响应率越高失真越大。
- **建议修复方向**：两站点聚合查询补 `isNull("respondedAt")` 反向过滤（或 eq status COMPLETED）；分母改「有评分的调查数」并按评分列分别计数（csat/nps/ces 可独立缺失）；E2E 种子补一条未响应调查样本固化口径。
- **arm-index 裁决**：新增（grep「csat 分母/respondedAt 聚合」arm-index 零命中；P1-RC-059 覆盖发送链已修复维度不同）。

### P1-CK-cs-003（D8/D6）目录建单（createFromCatalog）永不计算 SLA deadline——目录工单全链路无 SLA 计时/超时升级/违约统计；权益扣减在特定配置下双计；编号绕过 TK 月序列且毫秒碰撞

- **控制点 A**：`app/erp/cs/service/processor/ErpCsServiceCatalogItemCreateFromCatalogProcessor.java#buildTicketData`（L189-199：`data.put("code", "TK-" + CoreMetrics.currentTimeMillis()); if (item.getSlaPolicyId() != null) data.put("slaPolicyId", item.getSlaPolicyId());`）+ `#applyEntitlementToTicketData`（L82-106：权益命中时 `ticketData.put("slaPolicyId", matched.getSlaPolicyId()); entitlementBiz.consumeEntitlement(...)`，**不计算 deadline、不应用 maxResolutionTime**）对照 `ErpCsTicketBizModel#enrichAfterCreate`（L132-139：`if (ticket.getSlaPolicyId() == null && ticket.getDeadlineDateTime() == null)` 才触发 `matchAndAttachSla`——**目录路径预填了 slaPolicyId → 守卫跳过 → deadline 永不计算**）
- **证据**：① UC-CS-10 ④ 明文「系统创建 ErpCsTicket（ticketTypeId、**slaPolicyId**、priority 自动填充）」——目录项配置默认策略是文档化正常路径，该路径下 `deadlineDateTime` 恒 null：`scanOverdueTickets` 过滤 `lt("deadlineDateTime", now)` 天然排除 null（ScanOverdueTicketsProcessor L65）→ 目录工单永不超时升级；resolve 时 `deadline == null → isSlaCompleted=true`（ResolveProcessor L67-68）→ 全部「达标」；② 权益扣减双计——目录路径 `applyEntitlementToTicketData` 先扣一次（L105），若该权益**无 slaPolicyId**（纯 PAY_PER_TICKET 计次权益）则 ticketData 无 slaPolicyId → enrichAfterCreate 守卫通过 → `matchAndAttachSla` 内 `matchAndConsumeEntitlement` **再扣一次**（同单 usedTickets+2）；③ 编号——注释自承「code 为必填字段（domain=orderCode 不自动生成）」已过时（xmeta `ErpCsTicket.xmeta` L13 `biz:codeRule="cs-ticket-code"` + autoExpr fill-when-absent 已落地 TK{YYYYMM}{SEQ4}，测试 `TestErpCsTicketCreateEnrichment` L177-191 断言生成/显式共存），显式 `"TK-"+millis` 覆盖规则：目录工单编号格式偏离 UC-CS-01 ⑥ TK{YYYYMM}{SEQ4} 约定，且同毫秒并发提交撞 `UK_CS_TICKET_CODE_ORG`（orm L214）→ 整个 createFromCatalog 失败。
- **问题**：SLA 体系对目录渠道工单系统性缺位（计时、升级、违约统计、权益时限覆盖全链）；entitlement.md §8.1 注记只声明「不调 matchAndAttachSla 避免实体状态冲突」的 slaPolicyId 覆盖取舍，未声明 deadline 缺失与双计。
- **建议修复方向**：catalog 路径 save 前直接计算 deadline（复用 SlaDeadlineCalculator + 最终生效策略，含 maxResolutionTime 覆盖）写入 ticketData；或 save 后调 SLA 挂载（先 flush，同 ErpCsTicketBizModel.doSaveEntity 范式）；`applyEntitlementToTicketData` 命中权益时在 ticketData 打标使 enrichAfterCreate 不再二次扣减；去掉显式 millis code 让 codeRule 兜底。
- **arm-index 裁决**：新增（grep「catalog deadline/matchAndAttachSla 跳过」arm-index 零命中；entitlement.md §8.1 注记与 P1-RC-054 done 注记均未覆盖 deadline 维度）。

### P2-CK-cs-004（D3/D8）重开（reopen）后 SLA 升级链断裂——isSlaCompleted 不重置，超时扫描把重开工单永久排除

- **控制点**：`app/erp/cs/service/processor/ErpCsTicketScanOverdueTicketsProcessor.java#scanOverdueTickets`（L63-66：`q.addFilter(in("status", [ASSIGNED, IN_PROGRESS])); q.addFilter(lt("deadlineDateTime", now)); q.addFilter(eq("isSlaCompleted", Boolean.FALSE));`——**isSlaCompleted=false 是扫描必要条件**）对照 `ErpCsTicketReopenProcessor.java#reopen`（L46-48：仅 `setStatus(IN_PROGRESS)` + updateEntity，**不重置 isSlaCompleted**；注释只提 duration 重算）
- **证据**：工单按时解决（resolve 置 `isSlaCompleted=true`，ResolveProcessor L68-69）→ 客户驳回 reopen 回 IN_PROGRESS → 若此后超出原 deadline，扫描查询 `eq("isSlaCompleted", false)` 不命中该工单——state-machine.md §2「RESOLVED→IN_PROGRESS 恢复计时（时长累加）」与 §8「IN_PROGRESS 超 deadlineDateTime → 触发 SLA 超时升级」的语义在重开路径落空：重开工单超期永不升级、永不产生 ESCALATE 审计。下一次 resolve 会重算 isSlaCompleted（终值正确），但重开窗口内的升级保护为零。
- **问题**：SLA 升级链对重开工单存在系统性盲区（任务点名「重开后 SLA 重置」核查确认缺失）。
- **建议修复方向**：reopen 时将 isSlaCompleted 重置为 null/false（或扫描条件改为 `status IN (...) AND deadline < now AND (isSlaCompleted IS NULL OR isSlaCompleted = false) AND status != RESOLVED`——即以「当前未解决」替代「从未达标」语义）；与 sla.md §3.1 扫描条件对齐裁决。
- **arm-index 裁决**：新增（grep「reopen isSlaCompleted/重开 升级」arm-index 零命中；P1-RC-056 修复的 R1.67 升级链只覆盖计数器维度）。

### P2-CK-cs-005（D5）matchAndAttachSla 手动 mutation 无幂等守卫——重复调用重复匹配并重复扣减权益

- **控制点**：`app/erp/cs/service/processor/ErpCsTicketMatchAndAttachSlaProcessor.java#matchAndAttachSla`（L34-67：方法入口无「已挂载 slaPolicyId/deadline」守卫；L76-95 `matchAndConsumeEntitlement` 每次调用都 `entitlementBiz.consumeEntitlement(matched.getId(), ...)`——PAY_PER_TICK 权益 usedTickets+1/次）对照创建路径唯一守卫位置 `ErpCsTicketBizModel#enrichAfterCreate`（L133 `if (ticket.getSlaPolicyId() == null && ticket.getDeadlineDateTime() == null)`——守卫在**调用方**而非 Processor）
- **证据**：`ErpCsTicket__matchAndAttachSla` 是公开 @BizMutation（IErpCsTicketBiz 接口方法）：对同一工单调用两次 = 权益扣两次 + deadline 从 now 重算（顺延）。entitlement.md §8.1 声明「工单生命周期内单一触发点…避免重复扣减」仅对自动路径成立，手动入口裸露。并发双调用由 versionProp 乐观锁兜底一胜一败，但**串行重复调用**无任何拦截。
- **问题**：D5 幂等边界——按次计费权益（usedTickets）可被误操作放大扣减。
- **建议修复方向**：Processor 入口加守卫（slaPolicyId 与 deadlineDateTime 均非空时幂等返回当前工单，或抛 `ERR_*` 提示已挂载）；与 enrichAfterCreate 守卫语义对齐。
- **arm-index 裁决**：新增（grep「matchAndAttachSla 幂等」零命中）。

### P2-CK-cs-006（D4/D2）cs.entitlement-expiry / cs.fulfillment-approval-request 通知模板种子缺失——生产种子下权益到期提醒与履行审批请求 notify 全部静默丢弃

- **控制点**：`app-erp-all/src/main/resources/_vfs/_init-data/erp_sys_notification_template.csv`（grep 实证 cs.* 模板种子仅 8 条：sla-overdue/survey-invitation/knowledge-suggest-create/ticket-created/ticket-assign-no-match/csat-reminder/fulfillment-step-failed/fulfillment-notify-customer——**无 `cs.entitlement-expiry`、无 `cs.fulfillment-approval-request`**；两者仅存在于 module-cs 测试 `_cases` CSV autotest-ref 种子）对照消费点 `ErpCsEntitlementExpiryJob#notifyExpiry`（L103-117 `notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_ENTITLEMENT_EXPIRY, ...)`）与 `ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor#notifyApprovalRequest`（L677-691）
- **证据**：平台实证 `ErpSysNotificationNotifyProcessor`（module-notify）L42-45：`ErpSysNotificationTemplate template = findActiveTemplate(eventType, ctx); if (template == null) { LOG.warn("notify: 业务事件[{}]无 ACTIVE 模板，config-gated 静默跳过", eventType); return; }`。后果：① `ErpCsEntitlementExpiryJob` 的「到期预警」步骤（entitlement.md §2.2 核心 UC、job javadoc 首条职责）在生产种子下每条到期权益只产生 LOG.warn，零通知落地（自动停用步骤不受影响）；② 履行链 REQUEST_APPROVAL 步骤的审批人通知被静默丢弃（Processor javadoc 自认「无 ACTIVE 模板时 notify 静默跳过」——但该状态在生产种子下是**常态**而非边界，审批只能靠 24h 超时自动审批兜底或人工发现）。
- **问题**：D4 调度链完整性——两条通知链在默认部署种子下断裂（job/流程照常运行，输出静默蒸发，仅 WARN 日志）。
- **建议修复方向**：`erp_sys_notification_template.csv` 补 7208/7209 两条 ACTIVE 模板种子（entitlement-expiry → ROLE 客服主管/销售；fulfillment-approval-request → ROLE 客服主管缺省 approverRole 插值）；或消费方在模板缺失时走 WARN+降级 notify 通道。
- **arm-index 裁决**：新增（grep「模板种子缺失/entitlement-expiry 模板」arm-index 零命中；R1.68/R1.71 done 注记只声明 job 接线，未声明模板种子）。

### P2-CK-cs-007（D2/D4）CSAT 提醒 job 无去重无终态——「一次提醒」「7 天后不再提醒」双违背 + 同一调查 REMINDER/EXPIRED 双发 + 扫描集合无界

- **控制点**：`app/erp/cs/service/job/ErpCsCsatReminderJob.java#runReminders`（L90-97：`surveyBiz.findSurveyReminders(null, ctx)` + `findExpiredSurveys(null, ctx)` 两组**全量**结果各自 notifyAll——无 reminderSentAt 去重、无 EXPIRED 终态标记）对照 `ErpCsSurveyBizModel#findSurveyReminders`（L101-110：`isNull("respondedAt") + lt("surveySentAt", threshold)`——**无上界、无状态翻转**）与 `#findExpiredSurveys`（L113-122：同构——过期调查只是「更老的超期调查」，**与 reminders 集合是包含关系**：sent > 7d 的调查同时命中 48h 阈值）
- **证据**：csat.md §3.2 逐字「SENT 后 48h 未响应 → 自动发送**一次**提醒；SENT 后 7d 未响应 → 标记为 EXPIRED，**不再提醒**」。实现：① job 每次运行对全部未响应超龄调查重发提醒（若 cron 每日 → 每天一条直至永远——survey 无 EXPIRED 终态可写，dict `erp-cs/survey-status` 仅 PENDING/SENT/COMPLETED/FAILED）；② 超过 expire-days 的调查**同时**出现在 reminders 与 expired 两组（`lt(sentAt, now-48h)` ⊇ `lt(sentAt, now-7d)`）→ 每次 job 对同一调查派发 REMINDER + EXPIRED 两条通知；③ 集合随历史未响应调查单调增长（无分页 limit，D9 无界）。
- **问题**：通知骚扰 + 文档契约违背 + job 扫描量随时间劣化。
- **建议修复方向**：expired 优先短路（先查 expired 并从 reminders 排除其 ticketId），或 reminders 补 `ge("surveySentAt", now-expireDays)` 上界；补 reminderSentAt/提醒计数载体（或以 status 扩展）实现「一次提醒 + 到期停发」。
- **arm-index 裁决**：新增（grep「csat-reminder 去重/不再提醒」arm-index 零命中；R1.70 只交付发送链，未覆盖提醒链行为）。

### P2-CK-cs-008（D7/D8）ErpCsSurvey 无 DB 唯一键 + reopen 清理 limit(1)——并发/重复调查时误发问卷且清理不全

- **控制点**：`module-cs/model/app-erp-cs.orm.xml`（survey 实体 unique-keys 清单零命中——**cs 域 14 个 UK 中没有 survey 的业务 UK**（ticket_action/agent_rate/time_entry 等亦无，但均无「一对一」设计承诺），对照设计 csat.md §1.1「ticketId | 关联工单 | 唯一（一工单一调查）」）+ `ErpCsSurveyCreateSurveyProcessor#createSurvey`（L37-41 唯一性仅靠先查后插 `findSurveyByTicket` → `ERR_SURVEY_ALREADY_EXISTS`，无 DB 兜底）+ `ErpCsTicketReopenProcessor#cancelUnrespondedSurvey`（L57-68：`q.setLimit(1)` + 遍历删除 respondedAt 空的调查——**只处理一行**，无排序）
- **证据**：① 并发双 resolve（或 resolve + 手动 createSurvey）同 ticket：两事务都 findSurveyByTicket 空 → 双双插入 → 一工单两调查（UK 缺失无兜底）；② 一旦存在两行未响应调查，reopen 的 limit(1) 只删一行——另一行残留，survey-send job 照常派发该工单的「已驳回」问卷（state-machine.md 实现约定「reopen 取消未响应调查避免误发」落空）；③ 删除用 `surveyBiz.delete` 硬删（设计口径即删除，可接受）。
- **问题**：D7 并发窗口 + D8 清理不完备——违背「一工单一调查」不变量后无自愈路径。
- **建议修复方向**：orm 补 `UK_CS_SURVEY_TICKET`（ticketId）纯加性 UK（修复阶段走 dual-agent-approval）；cancelUnrespondedSurvey 去 limit(1) 全量处理未响应行。
- **arm-index 裁决**：新增（grep「survey 唯一/一工单一调查」arm-index 零命中）。

### P2-CK-cs-009（D3/D6）履行链 UPDATE_STATUS 直改 setStatus 绕过 resolve/close 副作用与守卫——无 duration/isSlaCompleted/endDateTime/survey，close 超时原因守卫被旁路

- **控制点**：`app/erp/cs/service/processor/ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java#executeUpdateStatus`（L370-398：`transitionAction(from, target)` 校验边合法后 `ticket.setStatus(target); ticketBiz.updateEntity(...)`——**不调用 resolve/close 编排**；owner 注记自认「『按配置 RESOLVED』经尾部 UPDATE_STATUS(status=RESOLVED) 步骤组合达成」）对照 `ErpCsTicketResolveProcessor#resolve`（duration 计算 + isSlaCompleted 判定 + CSAT 触发 + knowledge-suggest 后置）与 `ErpCsTicketBizModel#close`（endDateTime + `ERR_TICKET_CLOSE_BREACHED_NO_REASON` 守卫 L273-278——守卫条件 `Boolean.FALSE.equals(ticket.getIsSlaCompleted())`，**null 不触发**）
- **证据**：目录工单经链尾 UPDATE_STATUS(status=RESOLVED) → 再 UPDATE_STATUS(status=CLOSED)（close 边 RESOLVED→CLOSED 合法）：全程 isSlaCompleted=null（resolve 从未执行）、duration=null、endDateTime=null、survey 永不创建（CSAT 触发在 resolve 内）——sla.md §2.1「RESOLVED：SLA 停止计时，duration/isSlaCompleted」与 UC-CS-08 触发链对链解决工单整体缺位；且 isSlaCompleted=null 使 close 超时原因守卫永不触发（P2-CK-cs-013 的 null→breached 桶同源）。sla.md §3.3「标记超时原因/忽略超时」管理动作同样无载体。
- **问题**：状态机守卫保住了迁移合法性，但每条边的业务副作用与危险操作守卫被绕过——链渠道工单的 SLA/CSAT 数据残缺。
- **建议修复方向**：UPDATE_STATUS 对 resolve/close 边改为委托既有 resolve/close 编排（或至少补齐 duration/isSlaCompleted/endDateTime + survey 触发 + breach 守卫）；至少 owner doc 登记「链渠道无 SLA/CSAT 语义」为显式取舍。
- **arm-index 裁决**：新增（grep「UPDATE_STATUS 副作用/绕过 close 守卫」零命中；P1-RC-061 done 注记声明「状态机守卫真实迁移」恰恰确认了副作用缺失现状）。

### P2-CK-cs-010（D5/D3）重新分派路径缺失——assign 仅 NEW 守卫，ASSIGNED 态改派无 mutation 载体；assignedToId 无存在性校验

- **控制点**：`app/erp/cs/service/entity/ErpCsTicketBizModel.java#assign`（L228-242：`assertCan("assign", ticket, from, TICKET_STATUS_NEW)`——仅 NEW；`ticket.setAssignedToId(assignedToId)` **任意字符串无存在性校验**）对照 `statemachine/ErpCsTicketStateMachine.java#assertCanAssign`（L34-38 仅 NEW 合法）与 owner docs：use-cases.md UC-CS-02 前置「工单状态为 NEW **或 ASSIGNED（重新分派）**」、sla.md §3.3 超时后操作表「重新分派 → 状态保持 ASSIGNED，更改 assignedToId」、state-machine.md §4「工单分派后处理人不响应 → 客服主管手动重新分派（→ ASSIGNED）」、§场景 B 第 5 步「客服经理重新分派给王五 → ASSIGNED」
- **证据**：升级链（P1-RC-056/R1.67）通知客服经理后的第一个处置动作就是重派，但系统无 `reassign` mutation：ASSIGNED 态调 assign 抛 `ERR_INVALID_TICKET_STATUS_TRANSITION`；唯一途径是 CRUD `update_` 直改 assignedToId（无 ASSIGN 审计、绕过全部命名通道——P2-CK-cs-012 同型面）。escalateToQuality 场景 B 的闭环在产品内断头。assign 的 assignedToId 也不校验存在性（写悬挂 userId → 通知/看板失明，同 crm P3-CK-crm-016 模式）。
- **问题**：文档承诺的核心运营动作（超时重派/拒绝重派）无产品载体；D5 入参边界（存在性）缺失。
- **建议修复方向**：增 `reassign` mutation（ASSIGNED/IN_PROGRESS 态改 assignedToId + ASSIGN 审计 + 状态保持）；assign/reassign 对非空 assignedToId 反查用户存在。
- **arm-index 裁决**：部分复用——「拒绝路径缺失」归 **P2-RC-051**（todo，本报告不重复展开）；「ASSIGNED 态重派无载体 + ID 无校验」为**新增**维度（grep「reassign/重新分派」arm-index 零命中）。

### P2-CK-cs-011（D8/D9，同型 orgId 隔离族）cs 站点 orgId 过滤缺失 + 聚合查询无界全量加载

- **控制点**：`ErpCsTicketBizModel#findBoardData`（L408-414：`query.setLimit(200)` 无排序静默截断 + **无 orgId filter**）+ `ErpCsQualityDashboardBizModel#loadClosedTickets`（L254-267：`eq("status", CLOSED)` + 可选时间窗，**无 orgId、无 limit**）+ `#loadSurveyByTicket`/`#loadSlaPolicyTeamMap`（同构）+ `ErpCsReportBizModel#loadTickets`/`#aggregateSurveys`（L247-268：**全表全状态**无 orgId 无 limit）+ `TicketAssignResolver#resolveCandidatePool`（L49-59：`eq("code", csTeam.getCode())` 查 crm 团队**无 orgId**，跨组织同码团队先到先得）+ `ErpCsEntitlementBizModel#loadActiveByPartner`（L184-193：entitlement 有 orgId 列（UK code,orgId）但查询无 orgId）
- **证据**：工单/调查实体均含 orgId（ticket UK=code,orgId）；多组织部署下看板混单、SLA/CSAT 报表跨组织聚合、自动分配候选池可解析到别组织团队成员。截断族同型：findBoardData limit 200（同 P3-CK-crm-010/P2-CK-hr-003）；报表/看板 findAllByQuery 无界（数据量增长后内存/时延劣化；canned `suggestForTicket` 全量加载 active 模板内存过滤同病，`ErpCsCannedResponseBizModel` L102-106）。
- **问题**：同型族 cs 站点登记（P2-CK-fin2-007 / P2-CK-mfg-007 / P2-CK-hr-003④ orgId 隔离族 + limit 静默截断族 + 全量加载族）。
- **建议修复方向**：聚合入口从 IUserContext 取 orgId 下推 filter；limit 配置化 + truncated 标志；canned 宏匹配下推 macroTicketTypeId/macroPriority or-filter。
- **arm-index 裁决**：同型登记（orgId 族 + 截断族，cs 站点新增计数；单组织部署无影响——与 RC-R1.68 弱指针残留风险同边界）。

### P2-CK-cs-012（D5，同型 P1-CK-pur-003 族）CRUD update 无已审守卫——ticket status/deadline/isSlaCompleted、survey respondedAt/score、timer session status、entitlement usedTickets 等可经 update_ 直改

- **控制点**：`ErpCsTicketBizModel`（`defaultPrepareSave` 仅填 businessDate/status/priority 缺省，`defaultPrepareUpdate` 未 override——**无 status 迁移守卫、无 deadline/isSlaCompleted 保护**）+ `ErpCsSurveyBizModel`（respondedAt/csatScore 可 update_ 直写绕过 token 一次性提交语义）+ `ErpCsTicketTimerSessionBizModel`（status/activeFlag/cumulativePauseMinutes 可直改破坏单活跃 UK 语义）+ `ErpCsEntitlementBizModel`（usedTickets 直改绕过 consume/release 编排）+ `ErpCsSlaPolicyBizModel`（19 行裸 stub——resolveHours 负值/escalationDelayHours 负值无校验，负 delay 在 `resolveDelayHours` 被 Math.max(0,·) 钳掉、负 hours 在 calculator 走 `<=0 return null`→P1-CK-cs-001③ 的 NPE 入口）
- **证据**：`ErpCsTicket__update_` 可把 CLOSED 工单 status 改回 IN_PROGRESS（终态复活，绕过状态机全部 assertCan*）、直改 deadlineDateTime 规避违约、直写 isSlaCompleted 粉饰达标率；survey 的 token 一次性防线（respondedAt 判重）可被 update_ 清空重置。与全域名 mutation 守卫形成旁路面。
- **问题**：同型 P1-CK-pur-003 / P2-CK-crm-007 族——cs 站点登记（不复用展开）。
- **建议修复方向**：`defaultPrepareUpdate/Save` 守卫：status 变更仅允许经命名 mutation；deadline/isSlaCompleted/respondedAt/activeFlag 拒绝 CRUD 写入（或白名单字段集）；SlaPolicy 补 hours/days 非负校验。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，cs 站点新增计数）。

### P2-CK-cs-013（D6）SLA 聚合口径三处污染——deadline null → isSlaCompleted=true 计入达标分子；null → breached 桶；报表 totalTickets 不过滤 CLOSED

- **控制点 A**：`ErpCsTicketResolveProcessor#resolve`（L67-69：`boolean completed = deadline == null || !now.isAfter(deadline); ticket.setIsSlaCompleted(completed);`——**无 SLA/无 deadline 工单一律标达标**）
- **控制点 B**：`ErpCsQualityDashboardBizModel#getDashboardKpi`（L81-86：`if (Boolean.TRUE.equals(t.getIsSlaCompleted())) slaCompleted++; else slaBreached++;`——**null 归入 breached**）+ `ErpCsReportBizModel#buildTicketSlaCsatSummaryDataset`（L196-200 同构 else-bucket）
- **控制点 C**：`ErpCsReportBizModel#loadTickets`（L247-251：仅可选 ticketTypeId 过滤，**不限制 status=CLOSED**）对照 sla.md §4.3 报表示例逐字 `WHERE t.status = 'CLOSED'`
- **证据**：① entitlement.md §三「无 SLA → 不启用 SLA 计时」——无策略工单被 resolve 标 true 后计入「SLA 达标率」分子（sla.md §4.1「已完成工单中 isSlaCompleted=true 的比例」），叠加 P1-CK-cs-003（目录工单有策略无 deadline）系统性虚高达标率；② 经 UPDATE_STATUS 链关闭（P2-CK-cs-009）或从未 resolve 的工单 isSlaCompleted=null → 全部落入超时桶，虚高超时数——同一字段 null 语义在分子侧当 true、分桶侧当 false，自相矛盾；③ 报表把 RESOLVED/IN_PROGRESS 在途工单计入 totalTickets/达标统计（dashboard 正确过滤了 CLOSED，报表没有）。
- **问题**：SLA 达标率/超时数（看板 KPI + 综合报表）口径失真，与 sla.md §4.1/§4.3 定义漂移。
- **建议修复方向**：resolve 对 deadline null 保持 isSlaCompleted=null（不适用）；聚合侧三分桶（true/false/null-excluded）或 owner doc 裁决 null 语义；报表 loadTickets 补 `eq("status", CLOSED)`。
- **arm-index 裁决**：新增（grep「isSlaCompleted null/达标率口径」arm-index 零命中）。

### P2-CK-cs-014（D4，同型 cron 键漂移家族）6 个 cs job 内外层 cron 键不一致——启用需同时配 3 个键，description 声明的门控键与 trigger 实际消费键不同

- **控制点**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-cs-{sla-scan,survey-send,csat-reminder,entitlement-expiry,quality-retry,fulfillment-retry}.job.yaml`（6 文件同构：`enabled: "@cfg:nop.job.erp-cs-<name>.enabled|false"` + `cronExpr: "@cfg:nop.job.erp-cs-<name>.cron-expr|<default>"` 外层双层键；description 逐字「cron 经 erp-cs.<name>-cron 配置门控」）对照 bean 内层门控 `ErpCsSlaScanJob#execute`（L37-41 `AppConfig.var(ErpCsConstants.CONFIG_SLA_SCAN_CRON, "")` 默认空=永远跳过）及 SurveySend/CsatReminder/EntitlementExpiry/QualityEscalationRetry/FulfillmentRetry 5 个同构 execute()
- **证据**：真正启用任一 cs job 需同时配置 `nop.job.erp-cs-<name>.enabled=true`（job 被调度）+ `nop.job.erp-cs-<name>.cron-expr`（默认可用）+ `erp-cs.<name>-cron` 非空（execute 不跳过）。运维按 description 配 `erp-cs.sla-scan-cron=...`：job 因 enabled 默认 false 根本不被调度；反之只配 enabled=true：job 触发但每次 INFO 跳过。sla.md §5.3 对 sla-scan 声明「空值=跳过门控」为有意双层设计，但 description 引导的键与 trigger 消费键不一致的漂移形态与已裁决家族相同。
- **问题**：同型家族（P3-CK-crm-014 / P3-CK-mfg-013 / inv-021 / sal-023 / qa-019 / hr-012）cs 站点 ×6。
- **建议修复方向**：与家族联合裁决（job.yaml cronExpr 改消费 `erp-cs.<name>-cron` 对齐 lead-scoring-recalc 修复形态，或删内层门控）；description 修正。
- **arm-index 裁决**：同型登记（cron 键漂移家族，cs 站点 6 处新增计数）。

### P3-CK-cs-015（D5）close 超时原因守卫被 remark 复用稀释——resolve 的 resolution 文本即可满足「注明超时原因」

- **控制点**：`ErpCsTicketBizModel#close`（L273-278：`if (Boolean.FALSE.equals(ticket.getIsSlaCompleted()) && (ticket.getRemark() == null || ticket.getRemark().trim().isEmpty())) throw ERR_TICKET_CLOSE_BREACHED_NO_REASON`）对照 `ErpCsTicketResolveProcessor#resolve`（L71-73 `if (resolution != null) ticket.setRemark(resolution)`——**resolution 与超时原因共用 remark 列**）
- **证据**：超时工单 resolve 时填写任意解决方案文本（如「已修复」）→ remark 非空 → close 守卫恒通过，从未真正收集超时原因。state-machine.md 危险操作「CLOSED 前必须检查 isSlaCompleted——超时工单关闭需注明超时原因」的语义被列复用架空（守卫仅在 resolve 未填 resolution 且 remark 为空时才拦）。
- **问题**：D5 守卫正确性稀释（有守卫形，无常实效）。
- **建议修复方向**：close 增独立 `overtimeReason` 参数（写 remark 前缀/审计行），守卫改为检查该次调用提供的原因而非 remark 列存量。
- **arm-index 裁决**：新增（grep「close 超时原因 remark」零命中）。

### P3-CK-cs-016（D10）CsatReminderJob 通知上下文 customerName 错填 ticket.getSubject()——提醒通知渲染数据错位

- **控制点**：`app/erp/cs/service/job/ErpCsCsatReminderJob.java#notifySurvey`（L122-128：`map.put("ticketCode", ticket == null ? null : ticket.getCode()); map.put("customerName", ticket == null ? null : ticket.getSubject());`——**customerName 承载工单主题**；同文件 L131-144 loadTicket 已查出工单但未取 customer 关系/名称）
- **证据**：模板若渲染「客户 {customerName} 的调查…」将显示主题字符串；对照组内正向范式 `ErpCsSurveySendJob#resolveCustomerName`（L184-190 经 `ticket.getCustomer().getName()` 懒加载）。同 job 注入的 `ticketBiz` 无消费方（死注入）。
- **问题**：D10 数据错位（通知内容质量；无数据损坏）。
- **建议修复方向**：改经 `ticket.getCustomer()` 懒加载客户名（镜像 SurveySendJob）；清理死注入。
- **arm-index 裁决**：新增（grep「customerName getSubject」零命中）。

### P3-CK-cs-017（D4/D9）质量升级重试 PENDING 行扫描窗饿死——updateTime desc + limit 200 + Java 前缀过滤，老 PENDING 行被新成功行永久挤出

- **控制点**：`app/erp/cs/service/job/ErpCsQualityEscalationRetryJob.java#findPendingActions`（L113-122：`q.addFilter(eq("actionType", QUALITY_ESCALATE)); q.addOrderField("updateTime", true); q.setLimit(SCAN_LIMIT=200);` 取回后 `.filter(a -> a.getContent().startsWith(CONTENT_PENDING_PREFIX))`——**PENDING 判定在 Java 侧**）
- **证据**：QUALITY_ESCALATE 成功行（content=`NCR:...`）随业务增长持续追加；当最近 200 条 QUALITY_ESCALATE 全为成功行时，更早的 PENDING 行不再进入扫描窗——重试队列静默饿死（重试上限判定 `parseRetryCount` 也永不被触达，无「超限放弃」终态写入）。content 前缀可下推（`like("content", "PENDING:%")`——经 dao 直查不受 xmeta 限制，同文件已用 dao 直查先例）。
- **问题**：D4 重试链完整性——规模化后 PENDING 降级行失去恢复通道。
- **建议修复方向**：findPendingActions 过滤下推 `startsWith`（FilterBeans.startsWith 或 like 前缀）；或独立重试队列表/计数列。
- **arm-index 裁决**：新增（grep「PENDING 扫描/饿死」零命中）。

### P3-CK-cs-018（D5）计时器 pause/resume/stop 无属主校验——任意登录用户可操作他人会话

- **控制点**：`ErpCsTicketTimerSessionPauseTimerProcessor#pauseTimer` / `ResumeTimerProcessor#resumeTimer` / `StopTimerProcessor#stopTimer`（三者均 `ops.requireSession(sessionId)` 后直接操作——**无 `session.getAgentId().equals(context.getUserId())` 校验**）对照 `StartTimerProcessor#startTimer`（L46 `String agentId = context.getUserId()`——会话属主=启动者）
- **证据**：UC-CS-11 语境计时器是「客服个人工时」载体；A 启动的会话可被 B 停止/暂停，生成 agentId=A 的条目但 stopBy=B（executedBy 记 B）。修改他人计时影响其工时统计与单活跃槽位。
- **问题**：D5 权限边界（低危——内部工具面，无数据损坏路径）。
- **建议修复方向**：三 Processor 补属主校验（不匹配抛领域错误码；主管代操作可留 config 后门）。
- **arm-index 裁决**：新增（grep「timer 属主/sessionId 权限」零命中）。

### P3-CK-cs-019（D6）{agent_name} 系统变量填充 userId 而非 displayName——预设应答渲染口径与 owner doc 漂移

- **控制点**：`ErpCsCannedResponseApplyCannedResponseProcessor#resolveSystemVars`（L89-91：`vars.put("{agent_name}", context.getUserId())`）与 `ErpCsCannedResponseBizModel#resolveSystemVars`（同构复制）对照 canned-response.md §1.3 内置变量表「`{agent_name}` | 当前用户.**displayName**」
- **证据**：模板正文「请联系客服 {agent_name}」渲染出登录 ID 而非姓名。两处复制粘贴同病（修一处漏一处的典型形态）。
- **问题**：D6 渲染口径（用户可见文案质量）。
- **建议修复方向**：经平台用户服务解析 displayName（缓存），缺失回退 userId；两处复制收敛到共享 helper。
- **arm-index 裁决**：新增（grep「agent_name displayName」零命中）。

### P3-CK-cs-020（D5/D6）submitSurvey 全空评分仍标记 COMPLETED + 调查链接有效期（30 天/expire-days）不校验

- **控制点**：`ErpCsSurveyBizModel#submitSurvey`（L62-97：csat/nps/ces 全 null 仍 `setRespondedAt(now) + setStatus(COMPLETED)`——**无至少一项非空校验**；亦无 surveySentAt 起算的有效期拒绝）
- **证据**：① 空提交固化终态 COMPLETED → 回复率（csat.md §4.1「COMPLETED/(SENT+COMPLETED)」）虚高、该调查永不再被提醒/重发；② csat.md §3.2「链接有效期 30 天，过期后提示重新发送」与 §五 `survey-expire-days=7` 配置——findExpiredSurveys 只供通知（P2-CK-cs-007），提交侧不拒绝过期 token（占位公网端点无限期可提交）。
- **问题**：D5 入参边界 + D6 有效期口径（低危数据质量）。
- **建议修复方向**：提交要求至少一项启用维度的评分非空（否则 ERR）；submitSurvey 校验 surveySentAt + expireDays（或 status 非 COMPLETED）拒绝过期提交。
- **arm-index 裁决**：新增（P2-RC-054 覆盖「匿名/鉴权」维度，未覆盖空提交/有效期维度）。

### P3-CK-cs-021（D10）SurveyTokenGenerator 令牌前缀误用类名字面量——"ErpCsConstants-" 前缀无业务语义且长度贴边

- **控制点**：`app/erp/cs/service/entity/SurveyTokenGenerator.java#generate`（L16-18：`return ErpCsConstants.class.getSimpleName() + "-" + UUID.randomUUID().toString().replace("-", "");`——**`ErpCsConstants.class.getSimpleName()` 是对常量类的误引用**（疑为重构残留：原意应是某业务前缀常量），产出 `"ErpCsConstants-<32hex>"` = 46 字符，surveyToken 列 precision=50 贴边）
- **证据**：注释自称「生成 32 位无连字符 UUID」——前缀不在注释契约内；对外分发的调查链接 token 暴露 Java 类名（信息泄露面极小但无意义）；若未来前缀再加长 4 字符即截断风险。功能上唯一性成立（UUID 随机）。
- **问题**：D10 代码质量（行为影响=token 展示形态与类名泄露）。
- **建议修复方向**：改为业务前缀常量（如 `SURVEY-`）或纯 UUID。
- **arm-index 裁决**：新增（grep「SurveyTokenGenerator」arm-index 零命中）。

### P3-CK-cs-022（D1/D4）findSlaWarnings 在 @BizQuery 内派发通知副作用且无 warningSentAt 去重

- **控制点**：`ErpCsTicketBizModel#findSlaWarnings`（L384-402：`@BizQuery` 方法体内 `for (ErpCsTicket ticket : warnings) { notifySlaOverdue(ticket, context); }`——查询入口内嵌 notify 写副作用）对照 sla.md §3.4「记录 **warningSentAt（避免重复发送）**」——ORM 无 warningSentAt 列、代码无任何去重载体
- **证据**：每次调用（人工或未来接线 job）对同一临期工单重复派发 `cs.sla-overdue`（7101）通知；查询语义污染（调用方以为纯读，实际产生通知行）。cron 接线本身是 owner doc 声明的 Non-Goal（sla.md 实现约定「cron 实际注册归 Non-Goal」——不登记调度缺失），但「query 内副作用 + 无去重」是代码层缺陷。缓解：当前无自动调用方（grep 零命中），触发频率=人工调用。
- **问题**：D1 注解语义（查询走副作用）+ D4 去重缺失。
- **建议修复方向**：通知移出查询（独立 mutation 或 job 消费该查询）；补 warningSentAt 载体或以 ESCALATE 审计行去重。
- **arm-index 裁决**：新增（grep「findSlaWarnings 副作用/warningSentAt」零命中；调度缺失维度归 owner doc Non-Goal 不登）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | cs 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-pur-003` 族（CRUD update 无守卫全域族） | Ticket/Survey/TimerSession/Entitlement/SlaPolicy 裸 update 面 | 同型登记为 P2-CK-cs-012 |
| cron 键漂移家族（mfg-013/inv-021/sal-023/qa-019/hr-012/crm-014） | 6 个 erp-cs-*.job.yaml 内层 `erp-cs.<name>-cron` + 外层 `nop.job.*` 双层键 | 同型登记为 P2-CK-cs-014 |
| `P2-CK-fin2-007`/`P2-CK-mfg-007`/`P2-CK-hr-003`④（orgId 隔离族）+ `P3-CK-crm-010`（limit 200 截断族） | findBoardData / dashboard×3 / report×2 / TicketAssignResolver / loadActiveByPartner | 同型登记为 P2-CK-cs-011 |
| `P2-RC-051`（todo — rejectAssignment/customerConfirm/7 天自动关闭） | UC-CS-02 ③ + UC-CS-03 ④⑤⑦ 维度 | 复用不展开（7 天自动关闭缺失归其所有，本报告 009 不含该维度） |
| `P2-RC-052`（todo — extendDeadline） | sla.md §3.3 延长 deadline 缺失 | 复用不登记 |
| `P2-RC-053`（todo — canned 分类树 getCategoryTree） | 快捷回复分类树维度 | 复用不登记 |
| `P2-RC-054`（todo — submitSurvey 匿名/无鉴权端点） | 调查 token 公开提交面 | 复用不登记（020 只覆盖空提交/有效期维度） |
| `P2-RC-055`（todo — 建单履行登记失败 notify） | `CreateFromCatalogProcessor` L65-73 catch 仍 LOG.warn 无 notify（步骤级失败已有 7206，顶层 throw 路径无告警） | 复用不登记 |
| `P2-MA2-067`（watch-only — NEW>1h/ASSIGNED>2h 滞留升级） | 滞留自动升级未实现维度 | 复用 watch-only 不登记 |
| `P1-MA2-086`（10 cron job 并发副作用全局裁决） | SLA 扫描/调查发送等 job 并发重复 | 全局已裁决，不重复 |
| RC-R1.68 弱指针跨 org 残留风险（NCR sourceCode 无 org 维度） | ticket UK(code,orgId) vs NCR 反查 | owner doc 已声明残留风险，不新登 |
| mfg-002 族（双轴状态机 doReject 不回写 docStatus 死锁）同型核查 | cs 工单单轴 `status`；`docStatus`/`approveStatus` 列存在但 cs 域无审批流转 writer（无 doReject/双轴联动点） | **核查不适用**——无双轴状态机联动，无死锁形态（见「验证为正确」） |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：55 文件 `@Inject private`=0、`System.currentTimeMillis()/LocalDateTime.now()/new Date()`=0（时间一律 `CoreMetrics.currentTimestamp()/currentDateTime()/today()/currentTimeMillis()`）、`extends RuntimeException/Exception`=0（业务异常全部 `NopException` + `ErpCsErrors`）、`printStackTrace`=0、字典字符串 `==` 比较=0（grep 命中均为 null 判断与 int 常量比较）。
- **addOrderField 布尔参数 8 处全部正确**（C1.2 校准先例：第二参是 desc；grep 实证全域 8 个调用点）：`ErpCsTicketBizModel#findLastAssigned` L192 `("createTime", true)`=降序取最近分配 ✓ / `ErpCsSurveySendJob#findSendableSurveys` L112 `("createTime", false)`=升序最旧优先（批量公平）✓ / `ErpCsQualityEscalationRetryJob` L116 `("updateTime", true)`=降序新近优先 ✓（语义对，扫描窗缺陷另登 017）/ `ErpCsKnowledgeBaseBizModel#searchKnowledge` L72 `("createTime", true)`=降序新文章优先 ✓ + 内存二次排序 title 命中优先 + createTime `nullsLast(reverseOrder())` ✓ / `ErpCsCannedResponseBizModel#suggestForTicket` L105 `("sequence", false)`=升序 ✓ / `TicketAssignResolver#resolveCandidatePool` L58 `("id", false)`=成员 id 升序 ✓ / 履行 `findLastAssigned` L824 `("createTime", true)`=降序取最近 ✓ / 履行 `loadStepsByTicket` L849 `("sequence", true)` 降序 + Java 重排升序（冗余但最终序正确）✓。
- **`FilterBeans.like(name, value)` 直传不自动包裹 `%`**（平台源码实证 `nop-entropy/nop-kernel/nop-api-core/.../FilterBeans.java` L248-250 `compareOp(FILTER_OP_LIKE, name, value)`）：`searchKnowledge` 显式 `"%"+kw+"%"` 正确，不会产生双包裹。
- **`ticketBiz.get(id, true, ctx)` 第二参 ignoreUnknown 非锁**（ck-crm-lead 平台实证先例沿用）：不存在时返回 null，quality-retry/fulfillment 调用点 null 检查在位。
- **任务点名「CLOSED 可否直接 resolve」——不可**：`assertCanResolve` 仅放行 IN_PROGRESS（`ErpCsTicketStateMachine` L54-58）；CLOSED/CANCELLED 无任何出边（resolve/close/reopen/assign/start 全被 assertCan 拦截，cancel 走 `ERR_TICKET_ALREADY_TERMINAL` 领域码优先）；CRUD 旁路面归 P2-CK-cs-012 同型。
- **dict 无死状态（B2 核查）**：`erp-cs/ticket-status` 6 值与 `ErpCsConstants` 逐一相符且全部有 writer（NEW=save 缺省 + UPDATE_STATUS、ASSIGNED=assign/autoAssign/UPDATE_STATUS、IN_PROGRESS=start/reopen/UPDATE_STATUS、RESOLVED=resolve/UPDATE_STATUS、CLOSED=close/UPDATE_STATUS、CANCELLED=cancel/UPDATE_STATUS）；`transitions()` 9 边与 state-machine.md §2 M1.3 补全后表格逐条一致；survey-status 4 值、timer-session-status 3 值、fulfillment-step-status 5 值、time-entry-approve-status（NULL 承载 DRAFT）均有 writer/终态语义。web status badge 的通用调色板数组（ErpCsTicket.view.xml L20-23）含大量非本 dict 值但仅作颜色映射（NEW/ASSIGNED/RESOLVED 落 default 灰色）——无功能性死值消费（对比 P2-MA4-020 的 ACTIVE badge 消费不同形态），展示美化归 C8.2。
- **SLA deadline 工作日模式边界推演正确**（任务点名「跨午夜/周末」）：周五 22:00 + 4h → 周五消耗 2h（22→24）→ Sat 00:00 起点跳周一 00:00 → 周一 02:00 ✓；周五 23:00 + 1h → 周一 00:00（消耗满 1h 后落周末被推周一，方向宽松、语义可接受）；L78-82 回退分支（`candidate` 跨日且周末且 hour!=0）经推导不可达（chunk ≤ 24-hour 保证跨日必落 00:00）——死代码但零行为影响。日历模式 days×24h 折算与 owner doc 实现约定一致（工作时段窗口/节假日为声明 Non-Goal）。
- **多级升级链与 sla.md §3.2 实现注记逐条一致**（R1.67 修复在位验证）：窗口幂等（`now − lastEscalationAt < delayHours` 跳过）、max-repeat=重复上限（count ≥ 1+max 才升 L2）、secondEscalationUserId 空跳级 L3、L3 config 空仅推进时间戳 + WARN、level=3 封顶、`resolveDelayHours` 负值 Math.max(0,·) 钳位；逐单 try/catch 失败隔离 + `versionProp="version"` 乐观锁并发防护（P1-MA2-086 全局裁决不重复）；L1 双 null 仍写审计推进计数链（通知降级不阻断）。
- **resolve 计时语义正确**：`duration`（分钟）= startDateTime→now（reopen 保留 startDateTime、下次 resolve 重算总耗时——owner doc 实现约定「时长累加」等价实现）；`isSlaCompleted = now ≤ deadline`（null 语义缺陷另登 013，比较逻辑本身正确）；`minutesBetween` abs 防时钟回拨负值。
- **TK 编号机制完整**（R1.65 修复在位验证）：`CsTicketMonthSeqCodeRuleVariable` REQUIRES_NEW 月行懒建（并发冲突按已有行续行）+ stepSize=1/cacheSize=0 连续号 + DB 行锁 + 超长右截断回绕 + beans 注册 `nopCodeRuleVariable_csTicketMonthSeq` + xmeta `biz:codeRule="cs-ticket-code"` autoExpr fill-when-absent（显式 code 不覆盖）——主路径（直接 save）编号正确；catalog 路径绕过该机制归 P1-CK-cs-003③。
- **计时器子系统（R1.66 修复在位验证）**：`UK_CS_TIMER_SESSION_AGENT_ACTIVE`（orm L889，agentId+activeFlag）单活跃 DB 兜底并发双启动一胜一败；12h 惰性结算三事务边界正确（start/stop 当前事务、pause/resume/findActiveTimer REQUIRES_NEW 重载防外层回滚）；封顶时刻反推含累计暂停（closeOpenPause 先闭合再计算，序正确）；费率 `divide(60, 2, HALF_UP)`；config 钳位齐全（timerMaxHours≤0→12、retryMax/batchLimit/approvalTimeoutHours Math.max）。
- **通知降级范式一致**（8 个 notify 站点全部 try/catch WARN 不阻断主流程）；`notifyStepFailedRequireNew` REQUIRES_NEW + runInNewSession 使拒绝路径通知不被随后异常回滚（事务边界正确，R10 baseline-raise 注记吻合）。
- **CannedResponseRenderer 键空间一致**：variableDefs 文档规范（canned-response.md §1.3）与测试（`customVars.put("{customer_name}", ...)`）均为**含大括号 key**，`replacePlaceholders` 对 braced key 逐字替换正确、`validateRequired` 对 braced key 校验正确——裸 key 配置属 schema 维护方数据治理边界（CreateFromCatalog 校验注释同口径）。{agent_name} 值口径漂移另登 019。
- **escalateToQuality 编排正确**（R1.68 修复在位验证）：config 门控 → IN_PROGRESS 守卫 → materialId/defectDescription 必填 → `NCR-CS-{ticket.code}` 显式幂等命名（重复升级 UK 冲突 → 降级 PENDING → 重试反查命中既有 NCR 幂等收口）→ PENDING 载荷自足（截断 1000 保 content 2000 内）→ 重试上限 WARN 跳过 → 工单不迁移状态；跨 org 弱指针风险 owner doc 已声明（复用注记）。
- **履行引擎主干正确**（R1.71 修复在位验证）：物化 `UK_CS_TICKET_FULFILLMENT_STEP`（ticketId,fulfillmentId）幂等复用；FAILED/IN_PROGRESS 中断 + 后续 PENDING 保持；审批驳回 retryCount 置 max 阻断自动重试（终局语义）；超时自动审批 actionConfig.timeoutHours 覆盖 > config 兜底且钳 ≥1；手动重试 refresh 模板 actionConfig；`ensureInProgress` 终态/在途幂等跳过 + NEW 铺底双跳（副作用缺失维度另登 009）。
- **entitlement 匹配/扣减/回退正确**：`EntitlementMatcher` 纯函数过滤（partnerId/期间/active/余量）+ `min(endDate)` 取最近到期 ✓ §2.1；`consumeEntitlement` 仅 PAY_PER_TICK 增计 + 超限抛 `ERR_ENTITLEMENT_EXHAUSTED`；`releaseEntitlement` 幂等不降至负；`deactivateExpiredEntitlements` 逐条隔离 + endDate null 永不停用 ✓。
- **beans.xml 接线完整（D4）**：15 Processor + 状态机 + TicketAssignResolver + dashboard + report + 6 job bean + codeRule 变量共 27 bean 全注册；6 个 job.yaml invoker `bean: erpCs*Job, method: execute` 与 bean id 逐一对应——无孤立声明/漏调（键漂移形态归 014，模板种子缺失归 006）。
- **mfg-002 族（双轴 doReject 不回写 docStatus）核查不适用**：cs 域无审批流转——ticket 的 docStatus/approveStatus 列仅在建单写 DRAFT/UNSUBMITTED 初始值，域内无 doReject/审批 mutation 消费双轴，不存在「审批轴驳回后业务轴死锁」形态。
- **submitSurvey 评分区间校验正确**：csat 1-5 / nps 0-10 / ces 1-7 分维度 config-gated 校验 + `ERR_SURVEY_SCORE_OUT_OF_RANGE`；`NpsClassifier` 9-10/7-8/0-6 分类与 csat.md §1.2 一致（报表未消费 NPS 分类归 002 关联注记）。

## arm-index 复用 or 新增裁决（汇总）

- 新增关键符号（arm-index 零命中）：`matchAndAttachSla 权益覆盖覆写`/`maxResolutionTime`/`CSAT 分母未响应`/`respondedAt 聚合`/`catalog deadline 缺失`/`reopen isSlaCompleted`/`matchAndAttachSla 幂等`/`entitlement-expiry 模板`/`csat-reminder 去重`/`survey UK`/`UPDATE_STATUS 副作用`/`reassign`/`isSlaCompleted null`/`close remark 稀释`/`customerName subject`/`PENDING 扫描饿死`/`timer 属主`/`agent_name displayName`/`空评分 COMPLETED`/`SurveyTokenGenerator 前缀`/`findSlaWarnings 副作用` → **新增**。
- **复用（不重复登记，报告中注记）**：P2-RC-051/052/053/054/055（todo 修复方向已立案）、P2-MA2-067（watch-only）、P1-MA2-086（全局 job 并发）、RC-R1.68 跨 org 弱指针残留风险（owner doc 声明）。
- **同型登记**：P2-CK-cs-012（pur-003 族）、P2-CK-cs-014（cron 键漂移家族 ×6 站点）、P2-CK-cs-011（orgId/截断/全量加载三族合并）。
- **同型核查不成立/不适用**：mfg-002 双轴死锁族（cs 无双轴联动）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 3 | P1-CK-cs-001/002/003 |
| P2 | 11 | P2-CK-cs-004..014 |
| P3 | 8 | P3-CK-cs-015..022 |

按主维度：D6×5（001/002[跨 D8]/003[主 D8 跨 D6]/013/019、020 跨 D5）、D8×3（003/004/008[跨 D7]）、D5×3（005/010/012）、D4×3（006/014/017[跨 D9]）、D2×1（007，跨 D4）、D3×1（009，跨 D6）、D9×1（011，主 D8 跨 D9）、D10×3（016/021/022，015 主 D5、018 主 D5）。（精确主维度归属：001 D6、002 D6、003 D8、004 D3、005 D5、006 D4、007 D2、008 D7、009 D3、010 D5、011 D8、012 D5、013 D6、014 D4、015 D5、016 D10、017 D4、018 D5、019 D6、020 D5、021 D10、022 D1。）

同型/复用裁决：同型登记 3（012 pur-003 族 / 014 cron 键漂移家族×6 / 011 orgId+截断+全量三族合并）+ arm-index 复用不登记 8 项（RC-051/052/053/054/055、MA2-067、MA2-086、R1.68 残留风险）+ 同型核查不适用 1（mfg-002 双轴族）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-cs-service 55 文件核心链逐行深读（TicketBizModel 666 / FulfillmentProcessor 873 / Dashboard 409 / Report 324 / ScanOverdue 251 / EscalateToQuality 315 / SurveySendJob 199 / Configs 299 等；4 个裸 stub BizModel 桩确认）；平台源码实证 3 处（FilterBeans.like 直传、ErpSysNotificationNotifyProcessor 模板缺失 WARN 跳过、CrudBizModel.get ignoreUnknown 先例沿用）；orm 核对（versionProp 全实体在位 / 13 个 UK 清单含 survey 无 UK 实证 / ticket 列集 / sla_policy 无 orgId 列）；接线核对（beans.xml 27 bean / 6 job.yaml 键形态 / 15 dict / ErpCsTicket.xmeta codeRule / 通知模板种子 8 条 vs 常量 10 条）；消费面抽查（ErpCsTicket.view.xml badge、E2E cs-ticket-sla-csat.value.spec.ts 数值断言口径）；arm-index cs 相关 14 条（P1-RC-054..061、P2-RC-051..055、P2-MA2-067 及 R1.65-71 修复注记）逐条裁决。
- **未深查**：`erp-cs-web` 其余 AMIS/flux 页面契约 drift（归 C8.2，本报告仅抽查 status badge）；`erp-cs-api` 骨架 beans；`erp-cs-dao` 生成物正确性（问题应回溯模型）；测试代码正确性（仅用于行为交叉验证——TestErpCsTicketCreateEnrichment/TestErpCsCatalogFulfillmentEngine 的 TK/审批断言用于口径确认）；notify 域模板渲染细节（ROLE/USER_LIST resolver 行为）；`TestErpC16CsSlaNotification` 集成用例的升级链数值边界；`like` 通配符未转义（kw 含 `%`/`_` 的过滤放宽——全域同型未裁决，cs 不单列）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-cs-001**——权益 maxResolutionTime 被覆写的判定依赖「SlaPolicyMatcher 必然命中」（通用兜底策略存在时）。建议集成测试实证：建 PAY_PER_TICK 权益（maxResolutionTime=60）+ 一条 teamId IS NULL 通用策略 → 建单 → 断言 deadlineDateTime 是否= now+60min（我判定会被策略 deadline 覆写）。若产品语义就是「策略优先于权益时限」，需 owner doc 裁决后降级 not-a-problem。
  2. **P1-CK-cs-003①**——「目录工单无 deadline」依赖 enrichAfterCreate 守卫跳过链推演（slaPolicyId 预填 → matchAndAttachSla 不执行）。同链的双计症状（权益无 slaPolicyId 时扣两次）建议以测试实证 usedTickets 增量。
  3. **P2-CK-cs-006**——生产部署是否依赖 `_init-data` CSV 作为模板唯一来源（若部署流程另有模板初始化则影响面降级）；两条缺失模板的预期接收人（ROLE 客服主管 vs 销售）需 owner doc 裁决后补种子。
  4. **P2-CK-cs-013**——「无 SLA 工单（isSlaCompleted=true by null-deadline）是否应计入 SLA 达标率分母」sla.md §4.1 未显式排除；修复方向（null 三分桶 vs resolve 保持 null）需与 sla.md/看板消费面联合裁决，避免破坏 E2E 既有断言（5.00/9.00 种子口径）。
  5. **P2-CK-cs-007/020**——EXPIRED 终态的引入方式（dict 追加 option 属批量预授权清单外类型，须 dual-agent-approval，见 skills README MR1 裁决留痕）与「一次提醒」的载体选择（扩列 vs 派生）需修复阶段方案裁决。
