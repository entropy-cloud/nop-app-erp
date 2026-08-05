# 2026-08-06-0100-2 rc-ma1-a1-40-cs-f4-survey-entitlement-catalog-fulfillment 客服域 cs-F4 调查/权益/目录/履行需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A1.40（MA1 需求追踪矩阵审计 — cs-F4 满意度调查 CSAT / 服务权益校验 / 服务目录请求 / 服务目录履行流程：survey 延迟发送+token+取消 / entitlement 匹配+消费+到期 / catalog 建单+表单 / fulfillment 序列执行+actionType+重试）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.40
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.40 的 0.2 依赖）、`2026-08-05-2330-2-rc-ma1-a1-37-cs-f1-ticket-lifecycle.md`（cs-F1 done，P1-RC-054 `auto-assign-on-create` 死标志本切片 UC-CS-09 复用）、`2026-08-05-2330-3-rc-ma1-a1-38-cs-f2-sla-escalation.md`（cs-F2 done）、`2026-08-06-0100-1-rc-ma1-a1-39-cs-f3-knowledge-quality-canned.md`（cs-F3 同批 N=1，cs 域第 3 切片；本切片 N=2 收尾 cs 域 4 切片；本批次续编自 N=1 之后的最新 RC 编号）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.40 给出 UC 清单 = `UC-CS-08/09/10/12`（4 UC），覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 cs 域第 4 个（最后一个）RC 切片（A1.37/38 done；A1.39 同批 N=1；本切片收尾 cs 域）。

- **L1 需求契约（权威真相源）**：`docs/design/customer-service/use-cases.md`：
  - **UC-CS-08 满意度调查发送与评分**（`:145`）：触发=工单进入 RESOLVED；前置=`erp-cs.survey-enabled=true`。流程：① 工单 RESOLVED，**系统延迟 X 小时（配置 erp-cs.survey-send-delay）后创建 ErpCsSurvey**；② **系统按客户渠道发送含 surveyToken 的调查链接**；③ **客户点击链接 → 无鉴权访问调查页面**；④ 客户提交 CSAT/NPS/CES 评分及文字反馈；⑤ 系统更新 ErpCsSurvey（respondedAt、评分字段）；⑥ 评分计入客服绩效报表。后置：**调查终态为 COMPLETED 或 FAILED**。异常：**延迟期间工单重回 IN_PROGRESS → 取消该次调查；发送失败 → 标记 FAILED 并重试**。
  - **UC-CS-09 服务权益校验**（`:165`）：触发=创建工单指定 customerId；前置=`erp-cs.entitlement-check-enabled=true`。流程：① 创建工单 → **系统查询客户有效权益（ErpCsEntitlement）**；② **取有效期最近的权益 → 写入工单.slaPolicyId**；③ **更新 entitlement.usedTickets = usedTickets+1**；④ **到期前 30 天 nop-job 扫描创建续约提醒通知**；⑤ **到期日 isActive 自动设为 false**。后置：工单关联 SLA 策略，权益使用计数+1。异常：无有效权益 → 可选配置允许"无权益工单"；**usedTickets>=maxTickets → 拒绝创建（按次计费场景）**。
  - **UC-CS-10 服务目录请求提交**（`:184`）：触发=客户门户/客服代提交选择服务目录项；前置=`erp-cs.service-catalog-enabled=true`。流程：① 客户/客服浏览服务目录分类树；② 选择目录项 → **加载 requestFormConfig（动态表单）**；③ 填写表单 → 提交；④ 系统创建 ErpCsTicket（ticketTypeId、slaPolicyId、priority 自动填充，catalogItemId 记录来源）；⑤ 按 ErpCsCatalogFulfillment 履行流程顺序执行 actionType 序列。后置：工单创建成功，履行流程启动。异常：**表单必填项缺失 → 禁止提交**；履行流程执行失败 → 工单状态保持 NEW，告警通知管理员。
  - **UC-CS-12 服务目录履行流程**（`:223`）：触发=目录项提交后系统按履行流程执行 actionType 序列；前置=目录项已配置履行映射（ErpCsCatalogFulfillment）。流程：① 系统按 ErpCsCatalogFulfillment.sequence 升序依次执行；② 每个 actionType 根据 actionConfig 执行：**ASSIGN_TEAM → 按mode分配团队 / REQUEST_APPROVAL → 发起审批链，超时自动审批 / CREATE_CHILD_TICKET → 创建子工单 / NOTIFY_CUSTOMER → 发送通知 / UPDATE_STATUS → 更新工单状态**；③ **某一步失败 → 暂停流程，记录错误信息，通知管理员**；④ 全部步骤完成 → 工单进入 IN_PROGRESS（或按配置 RESOLVED）。后置：履行流程状态可跟踪，异常可重试。异常：**actionType 执行失败 → 支持重试（最多 3 次），超出后通知管理员人工介入**。
  - **L1 关键不变量**：UC-CS-08：延迟发送/渠道发送链接/无鉴权访问/CSAT-NPS-CES/COMPLETED-FAILED 终态/重开取消/失败重试；UC-CS-09：查询有效权益/最近到期/usedTickets+1/30 天续约 job/到期自动停用/超限拒绝；UC-CS-10：分类树/requestFormConfig 动态表单/自动填充/必填校验禁止；UC-CS-12：sequence 升序/actionConfig 驱动/5 种 actionType/失败暂停告警/重试最多 3 次/最终状态更新。

- **L3 代码实现现状（实测）**——**UC-CS-09 核心完整+强测，UC-CS-08/10/12 部分关键路径缺/占位（candidate P1/P2）**：
  - **UC-CS-08 调查（⚠️ 创建/字段/取消完整，❌ 延迟发送调度/渠道派发/FAILED-重试/无鉴权访问缺）**：
    - `createSurvey(ticketId)` @BizMutation `ErpCsSurveyBizModel.createSurvey:54` → `ErpCsSurveyCreateSurveyProcessor.createSurvey:30`；RESOLVED 触发 `ErpCsTicketResolveProcessor.resolve:60-63`（isSurveyEnabled && trigger=RESOLVED）；surveyToken `orm.xml:483` + `SurveyTokenGenerator.generate`；CSAT/NPS/CES 字段 `orm.xml:484-486`；`submitSurvey` @BizMutation `ErpCsSurveyBizModel.submitSurvey:60-94`（token 查找+已响应守卫+逐分 range 校验+respondedAt）；**重开取消** `ErpCsTicketReopenProcessor.cancelUnrespondedSurvey:53-64`（删除未响应 survey）；CSAT 报表 `ErpCsReportBizModel.buildTicketSlaCsatSummaryDataset`。✅ 触发/token/字段/取消/报表。
    - **延迟发送**：`ErpCsSurveyCreateSurveyProcessor:47-49` delayHours>0 时设 surveySentAt=null（PENDING），**但无 Job/Processor 后续真正发送 PENDING survey**（`ErpCsCsatReminderJob` 仅对已发送 survey 提醒，`findSurveyReminders` 过滤 surveySentAt != null）；grep `delayedSend|sendPendingSurvey` 零 → UC-CS-08 ①"延迟 X 小时后发送"❌（PENDING 调查孤立）。
    - **渠道派发链接**：`:46` 设 surveyChannel=PORTAL **但无实际派发**（无 email/link push，无 notificationBiz 调用，channel 字段仅描述）→ UC-CS-08 ②❌。
    - **无鉴权访问**：submitSurvey 是标准 @BizMutation（服务上下文），**无 public/匿名端点**（无 @BizAuth 豁免/匿名 GraphQL 路径；token 作 capability 但仍需认证调用）→ UC-CS-08 ③⚠️（部分）。
    - **FAILED 终态+重试**：**无 status 列、无 FAILED 状态**；grep `markSurveyFailed|retrySurvey|surveyStatus|FAILED` 零 → UC-CS-08 后置"COMPLETED/FAILED"+异常"失败标记 FAILED 并重试"❌。
  - **UC-CS-09 权益（✅ 核心+强测，⚠️ 普通工单保存自动联动缺——复用 P1-RC-054）**：
    - `matchForCustomer` @BizQuery `ErpCsEntitlementBizModel.matchForCustomer:200-203` → `EntitlementMatcher.match:37-55`（最近到期 min(endDate,nullsLast) `:52-54` + active/period/partner/quota 过滤 `:73-100`）；`consumeEntitlement:84-94`（PAY_PER_TICKET usedTickets+1 + 超限抛 `ERR_ENTITLEMENT_EXHAUSTED`）；`releaseEntitlement:105-119`（递减不低于 0）；写 `ticket.slaPolicyId` `ErpCsTicketMatchAndAttachSlaProcessor:45-47`；config-gated `matchAndConsumeEntitlement:77`（isEntitlementCheckEnabled + isAllowNoEntitlement）；**30 天续约** `scanExpiringEntitlements:124-135` + `ErpCsEntitlementExpiryJob.runExpiryWarnings:79-95` + `notifyExpiry:103-117`；**到期自动停用** `ErpCsEntitlementDeactivateExpiredEntitlementsProcessor.deactivateExpiredEntitlements:30-51`（endDate<now → isActive=false）。✅ 查询/最近到期/usedTickets+1/30 天续约/到期停用/超限拒绝。
    - **普通工单保存自动联动**：`matchAndAttachSla` 是**独立 mutation**，`save` **不自动调用**；`auto-assign-on-create` config 是**死标志**（A1.37 P1-RC-054 已登记）→ 权益校验仅在显式 matchAndAttachSla/createFromCatalog 时发生 → UC-CS-09 ①⚠️（复用 P1-RC-054 候选，不新建）。
  - **UC-CS-10 目录请求（⚠️ 建单+分类树+自动填充完整，❌ 必填表单校验缺/失败告警降级）**：
    - 分类树实体 `ErpCsCatalogCategory`（:647 self-ref parentId）+ 维护守卫 `ErpCsCatalogCategoryBizModel.defaultPrepareSave/Update/Delete:46-66`（自环/链环/最大深度/有子节点）；`createFromCatalog` @BizMutation `ErpCsServiceCatalogItemBizModel.createFromCatalog:40` → Processor `:41`；建单自动填充 ticketType/slaPolicy/priority `buildTicketData:129-166`；catalogItemId 回写 `:140`；inactive 拒绝 `validateCatalogItemUsable:115-123`；service-catalog config-gated `:42`。✅ 分类树实体/守卫/建单填充/catalogItemId 来源。
    - **必填表单校验**：`requestFormConfig` 字段 `orm.xml:698`（domain=json）**存在但未作为动态表单 schema 强制**；`buildTicketData:146-156` 盲拷 formData keys，**无按 schema 必填校验**；grep `requiredField|validateForm|formSchema` 零 → UC-CS-10 异常"表单必填项缺失禁止提交"❌。
    - **失败告警**：`ErpCsServiceCatalogItemCreateFromCatalogProcessor:60-67` catch Exception **仅 LOG.warn，无管理员告警，工单已保存**（决策注记"建单已成功，降级"）→ UC-CS-10 异常"履行失败→工单保持 NEW+告警"⚠️（降级，无告警）。
  - **UC-CS-12 履行流程（⚠️ 序列执行框架在，❌ actionConfig 未解析/5 种 actionType 占位/失败不暂停/无重试/最终状态不更新）**：
    - `executeFulfillmentSteps` @BizMutation `ErpCsCatalogFulfillmentBizModel.executeFulfillmentSteps:36` → Processor `:35`；**sequence 升序** `:42`（sort Comparator.comparingInt(sequenceOf)）；actionConfig JSON 字段 `orm.xml:752` 存在**但代码从不解析**（executeStep:57-94 仅 switch actionType，从不读 step.getActionConfig()）→ UC-CS-12 ②"根据 actionConfig 执行"❌。
    - **5 种 actionType**：ASSIGN_TEAM `:67-72` **仅写审计"占位登记"，无真实团队分配**⚠️；REQUEST_APPROVAL `:79-81` **仅写审计，无审批链/超时自动审批**⚠️；CREATE_CHILD_TICKET `:86-88` **SKIPPED（归 successor 跨域编排/子工单）**❌；NOTIFY_CUSTOMER `:73-75` **仅写审计，无 notificationBiz.notify 调用**❌；UPDATE_STATUS `:76-78` **仅写审计，无真实状态更新**❌；INVOKE_WORKFLOW `:85-88` SKIPPED（Non-Goal）❌。
    - **失败暂停**：`executeFulfillmentSteps:44-49` 循环**从不因失败中断**（executeStep 恒返回 DONE/SKIPPED，从不抛/无 try-catch 暂停）；grep `pauseFlow|notifyAdmin` 零 → UC-CS-12 ③"失败暂停+记录+通知管理员"❌。
    - **重试最多 3 次**：grep `retryCount|maxRetries|retry.*fulfillment` 零 → UC-CS-12 异常"重试最多 3 次"❌。
    - **最终状态更新**：executeStep 从不触及 ticket.status，工单恒 NEW（createFromCatalog:141 设）→ UC-CS-12 ④"全部完成进 IN_PROGRESS"❌。
  - **跨域 daoFor**：module-cs 生产代码零跨域 daoFor（同 A1.37/38/39 基线，A2.14:320 复用）。跨域经 Facade：`IErpMdPartnerBiz`、`IErpSysNotificationBiz`。P1-MA1-022 不涉及 cs。

- **L4 测试证据现状**（`module-cs/erp-cs-service/src/test/java/`）：
  - UC-CS-08：`TestErpCsTicketSlaCsat.java`——`testSurveyCreatedOnResolveAndSubmitted:177-204`（**强**：token+surveySentAt+respondedAt+csat=5）/ `testSurveyDuplicateCreateRejected:206`（ERR_SURVEY_ALREADY_EXISTS）/ `testSurveyInvalidTokenRejected:217` / `testReopenCancelsUnrespondedSurvey:225-239`。**缺口**：延迟发送/渠道派发/FAILED-重试/无鉴权访问/NPS-CES 提交零测试（仅 CSAT 测）。
  - UC-CS-09：`TestErpCsEntitlement.java`（10 @Test，**强**）——consume PAY_PER_TICKET 增量/超限拒绝/到期拒绝/递减/30 天扫描/到期停用/matchAndAttachSla 消费/无权益放行-拒绝/禁用跳过；`TestEntitlementMatcher`（cases 文件，9 例）。**缺口**：普通 save 自动联动/续约派发运行时/自动停用 Job cron 接线零测试。
  - UC-CS-10：`TestErpCsServiceCatalog.java`（8 @Test，**强**）——分类树自环/链环/最大深度/有子节点守卫 + createFromCatalog 填充 ticketType/slaPolicy/catalogItemId/subject/priority + inactive 拒绝 + subject 兜底。**缺口**：必填表单校验/requestFormConfig schema 驱动校验/履行失败告警零测试。
  - UC-CS-12：`TestErpCsServiceCatalog#testFulfillmentCreateTicketStepRegistered:179-218`——**弱**：仅断言 TicketAction 审计行存在（CREATE_TICKET/INVOKE_WORKFLOW/ASSIGN_TEAM/NOTIFY_CUSTOMER，`actions.stream().anyMatch(actionType + content.contains("DONE"))`），**不验证真实分配/通知/状态副作用**。**缺口**：真实 ASSIGN_TEAM/NOTIFY/UPDATE_STATUS 副作用/REQUEST_APPROVAL 链/CREATE_CHILD_TICKET/失败暂停/重试 3 次/actionConfig 解析零测试。
  - E2E：`tests/e2e/business-actions/cs-ticket.action.spec.ts`（6 态状态机 only，A1.37 范围）；报告 `tests/e2e/reports/cs-ticket-sla-csat.value.spec.ts`（token 断言）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14）：**cs 域 verdict `✅(A2.14✅) zero P1 zero P0`**——范围 = Ticket 6 态 + SLA 计时联动 + SLA 升级 cron Job；**:21-23 范围不含 Survey/Entitlement/Catalog/Fulfillment BizModel**——**A2.14 不覆盖 UC-CS-08/09/10/12**（这些属 RC A1.39/A1.40 范围，arm-index:218 注记"cs 域共 4 切片"）。
  - **cs 相关既有 finding**：`P2-MA2-067`（cs 滞留升级 watch-only）、`P1-MA2-086`（cron 并发幂等 resolved R1.28，**含 erp-cs-entitlement-expiry / erp-cs-csat-reminder** 两 cron）；RC 系列：`P1-RC-054`（A1.37 UC-CS-01 + `auto-assign-on-create` 死标志——**本切片 UC-CS-09 复用**）、`P1-RC-055`（A1.37 UC-CS-11）、`P2-RC-051`（A1.37）、`P1-RC-056`（A1.38）、`P2-RC-052`（A1.38）。**UC-CS-08/09/10/12 无既有直接 finding**（UC-CS-09 仅经 P1-RC-054 死标志间接涉及）。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：A2.14 范围不含本切片 UC，无可复用行为证据；`P1-MA2-086`（resolved R1.28）涉及 entitlement-expiry/csat-reminder cron 幂等——本切片引用其作为 cron 接线证据（不重审并发维度），只补需求视角差异。

- **arm-index 既有 finding 衔接**：grep arm-index cs survey/csat/entitlement/catalog/fulfillment/nps → UC-CS-08/10/12 无直接 finding；UC-CS-09 经 `P1-RC-054`（auto-assign-on-create 死标志）间接涉及。本切片新 finding 续全仓 RC 序列（执行时 grep arm-index 取 N=1 之后的最新续编，本切片为同批 N=2）。本切片须 grep arm-index cs survey/csat/entitlement/catalog/fulfillment/delay/token/approval/child-ticket 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10。本切片候选偏差（survey 延迟发送/渠道派发/FAILED-重试/必填表单校验/履行 actionType 占位/失败暂停/重试）属**代码逻辑**类（预授权——BizModel/Processor 逻辑 + cron Job 接线）；UC-CS-08 若加 status 列 / 无鉴权端点触及 ORM/鉴权配置须 ask-first；UC-CS-09 复用 P1-RC-054（不新建）。

- **剩余差距**：A1.40 切片五级追踪审计报告缺失 = MA4 及 MR1 该切片证据缺口来源。本计划产出 A1.40 报告并登记 finding，**收尾 cs 域全部 4 切片**证据缺口。

## Goals

- 产出 A1.40 切片审计报告 `docs/audits/2026-08-06-0100-2-rc-ma1-a1-40-cs-f4-survey-entitlement-catalog-fulfillment.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-CS-08/09/10/12 逐条核验**每条验收标准**（完整枚举，§3）：UC-CS-08 延迟发送/渠道链接/无鉴权/CSAT-NPS-CES/COMPLETED-FAILED/重开取消/失败重试；UC-CS-09 查询/最近到期/usedTickets+1/30 天续约/到期停用/超限拒绝/普通保存联动；UC-CS-10 分类树/requestFormConfig/自动填充/必填校验/失败告警；UC-CS-12 sequence/actionConfig/5 种 actionType/失败暂停/重试 3 次/最终状态 全链逐条。
- 对候选缺口给出分级结论：①UC-CS-12 **actionConfig 未解析 + NOTIFY_CUSTOMER/UPDATE_STATUS 占位无副作用 + 失败不暂停 + 无重试 + 最终状态不更新**倾向 **P1**（**§4 三判据关键裁决**——L1 全条无 Deferred 注记，须核 owner doc `service-catalog.md`/README 是否显式标 Non-Goal 且经人工批准；CREATE_CHILD_TICKET/INVOKE_WORKFLOW 占位有 successor 注记须单独裁决接受/P2）；②UC-CS-08 **延迟发送调度缺（PENDING 孤立）/渠道派发缺/FAILED-重试缺**倾向 **P1/P2**（L1 ①②+后置+异常明确要求；无鉴权访问倾向 P2）；③UC-CS-10 **必填表单校验缺**倾向 **P1/P2**（L1 异常明确禁止提交），失败告警倾向 P2；④UC-CS-09 普通工单保存联动 **复用 P1-RC-054**（不新建）；⑤UC-CS-09 核心 + UC-CS-08 创建/字段/取消 + UC-CS-10 建单/分类树/自动填充 + UC-CS-12 序列框架 → 倾向**接受**——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（续编，执行时取最新）并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/service-catalog.md/README.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.37/38/39 cs-F1/F2/F3；本切片仅 UC-CS-08/09/10/12）。
- **不复审工单生命周期/SLA/知识/质量/预设应答**（UC-CS-01/02/03/11 属 A1.37 / UC-CS-04 属 A1.38 / UC-CS-05/06/07 属 A1.39）。
- **不重审 P2-MA2-067 / P1-MA2-086 / P1-RC-054~056**（§去重协议：P1-MA2-086 resolved R1.28 引作 cron 接线证据；UC-CS-09 普通保存联动复用 P1-RC-054，不重审）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.40 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.40 UC 锚点）+ `docs/design/customer-service/use-cases.md`（L1 真相源）+ `docs/design/customer-service/csat.md`+`entitlement.md`+`service-catalog.md`+`README.md`（L2 设计参考，非真相源——履行 actionType/调查终态若标 Non-Goal 须 §4 三判据复核）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.14 报告（L5 既有证据，范围不含本切片 UC；P1-MA2-086 cron 接线复用）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-cs/erp-cs-service -Dtest=TestErpCsTicketSlaCsat,TestErpCsEntitlement,TestErpCsServiceCatalog,TestEntitlementMatcher`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-06-0100-2-rc-ma1-a1-40-cs-f4-survey-entitlement-catalog-fulfillment.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-CS-08/09/10/12 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:145/165/184/223` 验收标准原文；L2 引用 `csat.md`/`entitlement.md`/`service-catalog.md`/`README.md`（标注"设计参考，冲突以 L1 为准"——履行 actionType/调查终态 Non-Goal 标注须 §4 三判据复核）；L3 引用 `ErpCsSurveyBizModel`/`ErpCsSurveyCreateSurveyProcessor`/`ErpCsTicketResolveProcessor`/`ErpCsTicketReopenProcessor`/`ErpCsEntitlementBizModel`/`EntitlementMatcher`/`ErpCsEntitlementExpiryJob`/`ErpCsServiceCatalogItemBizModel`/`ErpCsCatalogFulfillmentBizModel`/`ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor`/`ErpCsConfigs`/`ErpCsSurvey`+`ErpCsEntitlement`+`ErpCsCatalogFulfillment` ORM（含行号）；L4 引用 `TestErpCsTicketSlaCsat`#method + `TestErpCsEntitlement`#method + `TestErpCsServiceCatalog`#method + `TestEntitlementMatcher`（注明断言强度；UC-CS-12 弱断言）；L5 标注 A2.14 范围不含本切片 UC + P1-MA2-086 cron 接线复用。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条对照）：UC-CS-08 ①**延迟发送**（PENDING 无 Job 消费 ❌）②**渠道派发链接**（surveyChannel 仅描述无派发 ❌）③**无鉴权访问**（submitSurvey 需认证 ⚠️）④CSAT/NPS/CES（字段在 ✅，NPS/CES config 默认 off）⑤COMPLETED/FAILED 终态（无 status 列 ❌）⑥重开取消（cancelUnrespondedSurvey ✅）⑦**失败重试**（无 ❌）；UC-CS-09 ①查询有效权益（matchForCustomer ✅）②最近到期（EntitlementMatcher ✅）③usedTickets+1（consumeEntitlement ✅）④30 天续约（scanExpiringEntitlements+Job ✅）⑤到期停用（deactivateExpiredEntitlements ✅）⑥超限拒绝（ERR_ENTITLEMENT_EXHAUSTED ✅）⑦**普通保存联动**（save 不自动调 matchAndAttachSla ⚠️ 复用 P1-RC-054）；UC-CS-10 ①分类树（实体+守卫 ✅，无独立浏览方法 ⚠️）②requestFormConfig（字段在未 schema 强制 ⚠️）③自动填充（buildTicketData ✅）④**必填校验**（无 ❌）⑤失败告警（仅 LOG.warn 无告警 ⚠️）；UC-CS-12 ①sequence 升序（✅）②**actionConfig 驱动**（从不解析 ❌）③5 种 actionType（ASSIGN_TEAM/REQUEST_APPROVAL 占位 ⚠️，NOTIFY_CUSTOMER/UPDATE_STATUS 无副作用 ❌，CREATE_CHILD_TICKET/INVOKE_WORKFLOW SKIPPED ❌）④**失败暂停**（循环不中断 ❌）⑤**重试 3 次**（无 ❌）⑥最终状态（不更新 ❌）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 UC-CS-08/09/10/12 给出符合性结论（取最高）：UC-CS-12 → actionConfig 未解析+NOTIFY/UPDATE_STATUS 占位无副作用+失败不暂停+无重试+最终状态不更新倾向 **P1**（**§4 三判据关键裁决**：`use-cases.md:223-242` 全条无 Deferred 注记——核 service-catalog.md/README 是否标 Non-Goal：判据[i]plan-audit / [ii]owner doc 显式标注经**人工批准**痕迹（grep git log，AI 自标 ≠ 人工批准）/ [iii]product-scope 裁剪；CREATE_CHILD_TICKET/INVOKE_WORKFLOW 占位有 successor 注记须单独裁决——若为经批准的 successor 则接受/P2，否则并入 P1）；UC-CS-08 延迟发送/渠道派发/FAILED-重试缺失倾向 **P1/P2**（L1 ①②+后置+异常明确；无鉴权访问倾向 P2）；UC-CS-10 必填表单校验缺失倾向 **P1/P2**（L1 异常明确禁止），失败告警倾向 P2；UC-CS-09 普通保存联动**复用 P1-RC-054**（在既有行追加 A1.40 交叉引用，不新建）；UC-CS-09 核心 + UC-CS-08 创建/字段/取消 + UC-CS-10 建单/分类树/自动填充 + UC-CS-12 序列框架 → **接受**。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（**P1 项核 owner doc Non-Goal/Deferred 标注的人工批准痕迹**）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-CS-08/09/10/12 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用、L3 含行号、L4 注明断言强度（UC-CS-12 弱断言）、L5 标注 A2.14 不含 + P1-MA2-086 cron 复用
- [x] UC-CS-08/09/10/12 有符合性结论且列明 §2 判据编号；候选缺口有明确分级；UC-CS-12 P1 裁决须含 owner doc Non-Goal 标注的人工批准痕迹核查结论；UC-CS-09 普通保存联动复用 P1-RC-054 裁决成立

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-06-0100-2-rc-ma1-a1-40-cs-f4-survey-entitlement-catalog-fulfillment.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` cs survey/csat/entitlement/catalog/fulfillment/delay/token/approval/child-ticket/notify/status/retry 同域同控制点后裁决——UC-CS-12 履行占位+无副作用+不暂停+无重试为**新根因** → 新建 P1-RC（UC-CS-12，续 N=1 之后最新编号）；UC-CS-08 延迟发送/渠道派发/FAILED-重试为**新根因** → 新建 P1/P2-RC（UC-CS-08）；UC-CS-10 必填校验为**新根因** → 新建 P1/P2-RC（UC-CS-10）；UC-CS-09 普通保存联动**复用 P1-RC-054**（既有行追加 A1.40 交叉引用，不新建）。执行时 grep arm-index 取 N=1 之后的最新续编号避免冲突。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）+ **UC-CS-08 survey 终态修复若加 status 列触及 ORM 须 ask-first** + **UC-CS-12 履行 actionType 实化须协调 notify 域（IErpSysNotificationBiz）/审批（nop-workflow）**。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（SP-1 survey delayHours 默认 0 时 PENDING 路径是否实际可达 / SP-2 submitSurvey 经 token 调用在真实鉴权配置下是否拒绝匿名 / SP-3 entitlement-expiry/csat-reminder cron enabled=true 时幂等行为[P1-MA2-086 R1.28 复用] / SP-4 createFromCatalog 履行占位审计行在前端是否被误读为真实执行 / SP-5 requestFormConfig 当前数据是否含可驱动校验的 schema；每存疑点一行）。**P0 即时通道评估**（履行占位/调查缺是否破坏活跃数据/会计——倾向否：影响 SLA/客户体验不破坏数据；不触发 MR0）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**（无生产代码变更，注明"无回归风险"）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：A2.14 范围不含 UC-CS-08/09/10/12，无可复用行为证据；P1-MA2-086（resolved R1.28）引作 entitlement-expiry/csat-reminder cron 接线证据（不重审并发维度），列明只补的需求视角差异（调查延迟/派发/FAILED / 目录必填 / 履行占位/不暂停/无重试）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 RC finding（UC-CS-12/08/10 视裁决）入 RC 发现追踪分区；P1-RC-054 行追加 A1.40 交叉引用（UC-CS-09）；audit reports 表新增 A1.40 行。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding 已写入 `arm-index.md`；P1-RC-054 追加 A1.40 交叉引用；静态存疑点清单已登记（SP-1~SP-5 供 A4.1/A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02ec40967ffeS65xYOn03yibSj，fresh session，未起草本计划）。范围/依赖/方法论/反 slack/模板/保护区域全 PASS；17+ load-bearing 引用经实仓复核 CONFIRMED TRUE：①A1.40 UC-CS-08/09/10/12 锚点（UC-CS-11 正确排除属 A1.37）✅；②UC-CS-12 executeStep:57-94 仅 switch actionType **从不读 step.getActionConfig()** + NOTIFY_CUSTOMER:73-75/UPDATE_STATUS:76-78 审计占位无副作用 + 循环:44-49 不因失败中断 + grep retryCount/maxRetries/pauseFlow/notifyAdmin 零 ✅；③UC-CS-08 仅 3 cs job（CsatReminder/EntitlementExpiry/SlaScan），grep delayedSend/sendPendingSurvey/markSurveyFailed 零 + ErpCsSurvey ORM 无 status 列 + submitSurvey:60-94 标准 @BizMutation 无匿名端点 ✅；④UC-CS-09 P1-RC-054 存在（arm-index:212）→ 复用有效 ✅；⑤UC-CS-10 requestFormConfig(orm.xml:698) 存在但 CreateFromCatalogProcessor 仅注释提及无 schema 强制 ✅；⑥A2.14:21-23/218-228 范围不含 Survey/Entitlement/Catalog/Fulfillment ✅。UC-CS-08/10 候选 P1/P2 对冲与 A1.38 同型（最终级别报告时 §2"取最高"定），非违规。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.40 报告 9 段齐全 + UC-CS-08/09/10/12 矩阵行（逐验收标准）+ finding 登记入 arm-index（收尾 cs 域 4 切片）
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.40 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Closure Audit Record

- Independent closure audit: `pass`（独立子代理 ses_02de5c7caffegP0t8OKAa2uYa3，fresh session，未起草/执行本计划）。7 项核查全 PASS：①报告 §1-§9 9 段齐全；②L1 逐字引用 UC-CS-08/09/10/12 与 `use-cases.md:145-242` 一致无语义漂移；③L3 代码锚点独立复核（UC-CS-08 `createSurvey:49` delay>0→surveySentAt=null + `CsatReminderJob` 仅消费 surveySentAt 非空 + grep delayedSend/markSurveyFailed/retryCount/pauseFlow 零 + `ErpCsSurvey` ORM 无 status 列；UC-CS-09 `EntitlementMatcher.match:52-54` min endDate nullsLast + `consumeEntitlement` ERR_ENTITLEMENT_EXHAUSTED + `auto-assign-on-create` 死 flag；UC-CS-10 `buildTicketData:146-156` 盲拷无 required 校验 + grep requiredField/validateForm/formSchema 零 + 失败 catch 仅 LOG.warn 无 notify；UC-CS-12 `executeStep:57-94` 从不读 getActionConfig + 占位仅写审计 + 循环:44-49 不中断 + ticket.status 不更新）；④§4 三判据 `git log` 全 `canonical`（AI）无人工批准痕迹 → 判据(ii) 不成立（与报告一致）；⑤finding 分级 3 新 P1 + 2 新 P2 + 1 reuse P1-RC-054（同根因同控制点 auto-assign 死 flag）合理；⑥arm-index 5 新 finding 各唯一 1 行 + A1.40 报告行 + P1-RC-054 A1.40 交叉引用；⑦`git status` 确认未改 cs 真相源/代码/ORM（仅新建报告 + 改 arm-index）。无阻塞项，结束证据 = 本记录 + 报告 §自检 + arm-index 行。

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（调查延迟/派发/FAILED-重试 / 目录必填校验 / 履行 actionType 实化/失败暂停/重试）属**代码逻辑**类（预授权——BizModel/Processor 逻辑 + cron Job 接线）；**UC-CS-08 survey status 列 / 无鉴权端点触及 ORM/鉴权配置须 ask-first + 独立 plan-audit**；UC-CS-12 履行实化须协调 notify 域 + 审批引擎。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-CS-09 普通保存联动修复随 P1-RC-054 修复行；UC-CS-12 履行实化须与 A1.39 UC-CS-06 notify 协同[若 UC-CS-06 修复引入 IErpSysNotificationBiz 模式可复用]）

## Closure

Status Note: A1.40 cs-F4（survey/entitlement/catalog/fulfillment）五级追踪审计报告 9 段齐全，UC-CS-08/09/10/12 逐验收标准矩阵行已落盘，finding 已登记入 arm-index，收尾 cs 域全部 4 切片（A1.37/38/39/40）证据缺口。本计划为只读审计，无代码/ORM/真相源变更（git status 已确认）。独立草案审查与独立结束审计均由独立子代理 fresh session 完成，无自我审计。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor subagent ses_02de5c7caffegP0t8OKAa2uYa3（fresh session，未起草/执行本计划，详见 `## Closure Audit Record`）
- Evidence: 7 项核查全 PASS —— 报告 §1-§9 9 段齐全；L1 逐字引用与 `use-cases.md:145-242` 一致；L3 代码锚点独立复核确认 UC-CS-08/09/10/12 候选缺口（delayedSend/markSurveyFailed/retryCount/pauseFlow/requiredField grep 零 + ErpCsSurvey 无 status 列 + executeStep 从不读 getActionConfig + 循环不中断 + ticket.status 不更新）；§4 三判据 `git log` 全 `canonical`（AI）无人工批准痕迹；finding 分级 3 新 P1 + 2 新 P2 + 1 reuse P1-RC-054 合理；arm-index 5 新 finding 各唯一 1 行 + A1.40 报告行 + P1-RC-054 A1.40 交叉引用；`git status` 确认未改 cs 真相源/代码/ORM。
- 报告产物：`docs/audits/2026-08-06-0100-2-rc-ma1-a1-40-cs-f4-survey-entitlement-catalog-fulfillment.md`（§1-§9）
- arm-index 更新：`docs/audits/arm-index.md`（RC 发现追踪分区 5 新行 + audit reports 表 A1.40 行 + P1-RC-054 交叉引用）

Follow-up:

- finding 修复属 MR0（P0）/ MR1（RC-R1.n，P1/P2）范围，按 §10 + `## Deferred But Adjudicated` 衔接，不在本审计计划实施
