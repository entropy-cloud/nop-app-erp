# 2026-08-06-0100-2 rc-ma1-a1-40 cs-F4 调查/权益/目录/履行需求符合性审计报告

> 报告状态：done
> Mission: requirement-compliance（MA1 切片 A1.40）
> Work Item: A1.40（MA1 需求追踪矩阵审计 — cs-F4 满意度调查 CSAT / 服务权益校验 / 服务目录请求 / 服务目录履行流程）
> Source Plan: `docs/plans/2026-08-06-0100-2-rc-ma1-a1-40-cs-f4-survey-entitlement-catalog-fulfillment.md`
> 方法论契约: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> L1 锚点: `docs/audits/rc-requirement-baseline-inventory.md` A1.40 = UC-CS-08/09/10/12（4 UC，覆盖率 ✅ 一致，无基线分歧 D-xx）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）
> 本审计为**只读审计**：不修改代码/ORM/api.xml/view.xml/真相源；结果表面 = 本报告 + arm-index 登记。finding 的修复按 §10 经 MR0/MR1 实施。

---

## §0 与既有 MA2 报告差异增量声明（methodology §6 段落 9 / §去重协议）

- **A2.14 范围不含本切片 UC**：`docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14 A+B 合并）cs 域范围 = Ticket 6 态状态机 + SLA 计时联动 + SLA 升级 cron Job（:21-23 / :218-228 显式声明范围不含 Survey/Entitlement/Catalog/Fulfillment BizModel）。**故 A2.14 不提供 UC-CS-08/09/10/12 的可复用行为证据**——本切片从需求契约视角独立五级追踪。
- **P1-MA2-086（resolved R1.28）复用为 cron 接线证据**：`erp-cs-entitlement-expiry` / `erp-cs-csat-reminder` 两 cron 的并发幂等守卫经 R1.28 修复（plan `2026-07-30-0841-2`）。本切片引用其作为 cron 接线**已证实行为**（不重审并发维度），只补需求视角差异（UC-CS-08 延迟发送/渠道派发/FAILED-重试、UC-CS-10 必填校验/失败告警、UC-CS-12 履行占位/不暂停/无重试）。
- **cs 域第 4 切片（收尾 cs 域 4 切片）**：A1.37（cs-F1 done）、A1.38（cs-F2 done）、A1.39（cs-F3 done）、A1.40（本切片，cs-F4）。本切片新 finding 续全仓 RC 序列 N=1（A1.39）之后最新编号。

---

## §1 需求契约原文（L1 use-case 需求契约，逐字引用）

> 真相源：`docs/design/customer-service/use-cases.md`（L1 权威功能契约，methodology §4 层级 2）。验收标准逐字引用，禁止转述（§1 L1 格式）。

### UC-CS-08 满意度调查发送与评分（CSAT Survey）— `use-cases.md:145`

**触发条件** 工单进入 RESOLVED 状态。**前置条件** erp-cs.survey-enabled=true。

**流程**
1. 工单 → RESOLVED，系统延迟 X 小时（配置项 erp-cs.survey-send-delay）后创建 ErpCsSurvey。
2. 系统按客户渠道发送含 surveyToken 的调查链接。
3. 客户点击链接 → 无鉴权访问调查页面。
4. 客户提交 CSAT/NPS/CES 评分及文字反馈。
5. 系统更新 ErpCsSurvey（respondedAt、评分字段）。
6. 评分计入客服绩效报表。

**后置条件** 调查终态为 COMPLETED 或 FAILED。

**异常** 延迟期间工单重回 IN_PROGRESS → 取消该次调查；发送失败 → 标记 FAILED 并重试。

### UC-CS-09 服务权益校验（Entitlement Check）— `use-cases.md:165`

**触发条件** 创建工单时指定客户（customerId）。**前置条件** erp-cs.entitlement-check-enabled=true。

**流程**
1. 创建工单 → 系统查询客户的有效权益（ErpCsEntitlement）。
2. 取有效期最近的权益 → 写入工单.slaPolicyId。
3. 更新 entitlement.usedTickets = usedTickets + 1。
4. 到期前 30 天 nop-job 扫描创建续约提醒通知。
5. 到期日 isActive 自动设为 false。

**后置条件** 工单关联 SLA 策略，权益使用计数+1。

**异常** 无有效权益 → 可选配置允许创建"无权益工单"；usedTickets >= maxTickets → 拒绝创建（按次计费场景）。

### UC-CS-10 服务目录请求提交（Catalog Request）— `use-cases.md:184`

**触发条件** 客户通过门户或客服代提交选择服务目录项。**前置条件** erp-cs.service-catalog-enabled=true。

**流程**
1. 客户/客服浏览服务目录分类树。
2. 选择目录项 → 加载 requestFormConfig（动态表单）。
3. 填写表单 → 提交。
4. 系统创建 ErpCsTicket（ticketTypeId、slaPolicyId、priority 自动填充，catalogItemId 记录来源）。
5. 按 ErpCsCatalogFulfillment 履行流程顺序执行 actionType 序列。

**后置条件** 工单创建成功，履行流程启动。

**异常** 表单必填项缺失 → 禁止提交；履行流程执行失败 → 工单状态保持 NEW，告警通知管理员。

### UC-CS-12 服务目录履行流程（Service Fulfillment）— `use-cases.md:223`

**触发条件** 服务目录项提交后，系统按履行流程执行 actionType 序列。**前置条件** 目录项已配置履行映射（ErpCsCatalogFulfillment）。

**流程**
1. 系统按 ErpCsCatalogFulfillment.sequence 升序依次执行。
2. 每个 actionType 根据 actionConfig 执行对应动作：ASSIGN_TEAM → 按 mode 策略分配团队 / REQUEST_APPROVAL → 发起审批链，超时自动审批 / CREATE_CHILD_TICKET → 创建子工单 / NOTIFY_CUSTOMER → 发送通知 / UPDATE_STATUS → 更新工单状态。
3. 某一步失败 → 暂停流程，记录错误信息，通知管理员。
4. 全部步骤完成 → 工单进入 IN_PROGRESS（或按配置 RESOLVED）。

**后置条件** 履行流程状态可跟踪，异常可重试。

**异常** actionType 执行失败 → 支持重试（最多 3 次），超出后通知管理员人工介入。

---

## §2 实现证据（L3 代码路径，方法锚点 + 关键行为断言）

> 引用格式：`module-cs/erp-cs-service/.../<X>.java#<method>`（方法锚点 + 行为断言；行号为写时实测导航，漂移不构成引用失效）。跨域调用链列全。跨域经 Facade：`IErpMdPartnerBiz`、`IErpSysNotificationBiz`（无生产代码跨域 daoFor，同 A1.37/38/39 基线）。

### UC-CS-08 调查

- **触发**：`ErpCsTicketResolveProcessor#resolve:35-65` —— IN_PROGRESS→RESOLVED 守卫（:38-40 非法迁移抛 ERR_INVALID_TICKET_STATUS_TRANSITION）+ SLA 停止计时算 duration（:43-46）+ isSlaCompleted 判定（:48-50）；**CSAT 触发 config-gated**（:60-63 `isSurveyEnabled() && getSurveyTriggerStatus()==RESOLVED` → `surveyBiz.createSurvey`）。
- **创建**：`ErpCsSurveyBizModel#createSurvey:54` @BizMutation → `ErpCsSurveyCreateSurveyProcessor#createSurvey:30-58` —— 唯一约束守卫（:38-41 findSurveyByTicket 非空抛 ERR_SURVEY_ALREADY_EXISTS）+ token 生成（:45 `SurveyTokenGenerator.generate`）+ **surveyChannel=PORTAL**（:46）+ **delayHours>0 → surveySentAt=null（PENDING）/ delayHours=0 → surveySentAt=now（SENT）**（:47-49）+ saveEntity（:50）+ 工单存在性经 ORM to-one 懒加载校验（:54-57）。
- **token**：`SurveyTokenGenerator#generate:16-18` —— 32 位无连字符 UUID（`ErpCsConstants`-前缀），字段 precision=50。
- **提交**：`ErpCsSurveyBizModel#submitSurvey:60-94` @BizMutation —— token 空守卫（:66-68 ERR_SURVEY_TOKEN_INVALID）+ token 查找（:69 findSurveyByToken）+ **已响应守卫**（:73-75 ERR_SURVEY_ALREADY_RESPONDED）+ **逐分 range 校验 config-gated**（:77-85 csat 1-5 / nps 0-10 / ces 1-7，`requireScoreRange:131-139` 超限抛 ERR_SURVEY_SCORE_OUT_OF_RANGE）+ 写 respondedAt（:90 `CoreMetrics.currentTimestamp()`）+ updateEntity。
- **重开取消**：`ErpCsTicketReopenProcessor#reopen:36-51` + `cancelUnrespondedSurvey:53-64` —— RESOLVED→IN_PROGRESS 守卫（:39-41）+ 查未响应调查（:55-58）+ **删除未响应调查**（:59-63 `respondedAt==null → surveyBiz.delete`）。
- **提醒/过期查询**：`ErpCsSurveyBizModel#findSurveyReminders:98-107` + `findExpiredSurveys:111-119` —— 均 filter `isNull(respondedAt) AND lt(surveySentAt, threshold)`。
- **CSAT 报表**：`ErpCsReportBizModel#buildTicketSlaCsatSummaryDataset`（聚合 csat/respondedAt）。
- **关键缺口（grep 实测，跨 `module-cs/erp-cs-service/src/main`）**：
  - 延迟发送：`ErpCsCsatReminderJob#execute:72-85` + `runReminders:90-97` 仅调 `findSurveyReminders`/`findExpiredSurveys`（均 `lt surveySentAt`），**PENDING（surveySentAt=null）调查永不被消费**；grep `delayedSend|sendPendingSurvey` **零命中** → L1 ①"延迟 X 小时后发送"❌。
  - 渠道派发链接：`createSurvey:46` 设 `surveyChannel=PORTAL` **但无实际派发**（无 `notificationBiz.notify` 调用、无 email/link push、无门户渲染触发）→ L1 ②"按客户渠道发送含 surveyToken 的调查链接"❌。
  - 无鉴权访问：`submitSurvey` 标准 @BizMutation（服务上下文），**无 @BizAuth 豁免/匿名 GraphQL 端点**；grep 匿名/anonymous 零 → L1 ③⚠️（部分）。
  - FAILED 终态+重试：`ErpCsSurvey` ORM **无 status 列**（实测 `app-erp-cs.orm.xml:483-490` 仅 surveyToken/csatScore/npsScore/cesScore/respondedAt/surveySentAt/surveyChannel，状态由时间戳派生）；grep `markSurveyFailed|retrySurvey|surveyStatus|FAILED`（业务语义）**零命中** → L1 后置"COMPLETED/FAILED"+异常"失败标记 FAILED 并重试"❌。

### UC-CS-09 权益

- **匹配**：`ErpCsEntitlementBizModel#matchForCustomer:200-203` @BizQuery → `EntitlementMatcher#match:37-55` 纯函数式 —— 过滤 partnerId（:48 `isPartnerMatched:73-75`）+ 期间有效（:49 `isPeriodValid:77-86` start≤now≤end）+ active（:50 `isActive:88-91`）+ 余量（:51 `hasRemainingQuota:93-100` max IS NULL OR used<max）+ **取 endDate 最近者**（:52-54 `min(endDate, Comparator.nullsLast)`）。
- **扣减**：`ErpCsEntitlementBizModel#consumeEntitlement:80-101` @BizMutation —— `validateConsumable:213-222`（active+未过期，否则 ERR_ENTITLEMENT_EXPIRED）+ **PAY_PER_TICKET usedTickets+1**（:93）+ **超限抛 ERR_ENTITLEMENT_EXHAUSTED**（:87-91 used>=max）；WARRANTY/SUPPORT_CONTRACT 仅记日志不增计（:95-99）。
- **回退**：`ErpCsEntitlementBizModel#releaseEntitlement:105-119` —— PAY_PER_TICKET **递减不低于 0**（:112-116 used<=0 幂等返回）。
- **30 天续约扫描**：`ErpCsEntitlementBizModel#scanExpiringEntitlements:124-135` @BizQuery（窗口默认 30 天 `getEntitlementExpiryWarningDays`，`dateBetween(endDate, now, now+window)`）+ `ErpCsEntitlementExpiryJob#runExpiryWarnings:79-95` + `notifyExpiry:103-117`（`notificationBiz.notify(NOTIFY_EVENT_ENTITLEMENT_EXPIRY, map)`）。
- **到期自动停用**：`ErpCsEntitlementBizModel#deactivateExpiredEntitlements:139-141` @BizMutation → `ErpCsEntitlementDeactivateExpiredEntitlementsProcessor#deactivateExpiredEntitlements:30-51`（`endDate<now AND isActive=true → isActive=false`，单条失败隔离 :45-48）。
- **建单集成**：`ErpCsTicketMatchAndAttachSlaProcessor#matchAndConsumeEntitlement:76-95`（config-gated `isEntitlementCheckEnabled` + `matchForCustomer` + 覆盖 slaPolicyId + `consumeEntitlement`，无权益按 `isAllowNoEntitlement` 放行或抛 ERR_ENTITLEMENT_NONE_ACTIVE）；**目录驱动建单** `ErpCsServiceCatalogItemCreateFromCatalogProcessor#applyEntitlementToTicketData:76-100` 同语义（建单前覆盖 ticketData）。
- **普通工单保存联动（关键缺口）**：`matchAndAttachSla` 是**独立 @BizMutation**，`ErpCsTicketBizModel#save` **不自动调用**；`ErpCsConfigs#isAutoAssignOnCreate:26-28` config 默认 true **但生产代码零调用方**（A1.37 已登记 P1-RC-054 死标志）→ 权益校验仅在显式 `matchAndAttachSla` / `createFromCatalog` 时发生 → L1 ①⚠️（**复用 P1-RC-054，不新建**）。

### UC-CS-10 目录请求

- **分类树**：`ErpCsCatalogCategory` 实体（`app-erp-cs.orm.xml:647` 区域 self-ref parentId）+ `ErpCsCatalogCategoryBizModel#defaultPrepareSave/Update/Delete:47-126` 维护守卫（自环/链环 ERR_CATALOG_CATEGORY_CYCLE:81/103/108 + 最大深度 ERR_CATALOG_CATEGORY_MAX_DEPTH_EXCEEDED:126 + 有子节点禁删 ERR_CATALOG_CATEGORY_HAS_CHILDREN:63）。config-gated 深度 `getCatalogCategoryMaxDepth`（默认 3）。
- **建单**：`ErpCsServiceCatalogItemBizModel#createFromCatalog:40` @BizMutation → `ErpCsServiceCatalogItemCreateFromCatalogProcessor#createFromCatalog:41-69` —— config-gated `isServiceCatalogEnabled`（:42）+ `requireCatalogItem`（:46）+ `validateCatalogItemUsable:115-123`（inactive 拒绝 ERR_CATALOG_ITEM_INACTIVE）+ **建单自动填充** `buildTicketData:129-166`（ticketTypeId :134-136 / slaPolicyId :137-139 / **catalogItemId 回写** :140 / priority from urgency :154 / status=NEW+docStatus=DRAFT+approveStatus=UNSUBMITTED :141-143 / subject 缺省回退目录项名 :158-160 / priority 缺省 NORMAL :162-164）。
- **必填表单校验（关键缺口）**：`requestFormConfig` 字段 `app-erp-cs.orm.xml:698`（domain=json）**存在但未被作为动态表单 schema 强制**；`buildTicketData:146-156` 盲拷 formData keys（`copyIfPresent`），**无按 schema 必填校验**；grep `requiredField|validateForm|formSchema` **零命中** → L1 异常"表单必填项缺失 → 禁止提交"❌。
- **失败告警（缺口）**：`CreateFromCatalogProcessor:60-67` catch Exception **仅 LOG.warn（"建单已成功，降级"），无管理员告警通知，工单已保存**（L1 "工单状态保持 NEW" 部分满足，"告警通知管理员" 缺）→ L1 异常⚠️（降级，无告警）。

### UC-CS-12 履行流程

- **序列执行**：`ErpCsCatalogFulfillmentBizModel#executeFulfillmentSteps:36` @BizMutation → `ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor#executeFulfillmentSteps:35-51` —— `loadStepsByCatalogItem:108-113` + **sequence 升序**（:42 `sort(Comparator.comparingInt(sequenceOf))`，null 视为 0 :115-118）+ 循环执行 :44-49。
- **关键缺口（逐条）**：
  - **actionConfig 未解析**：`executeStep:57-94` 仅 `switch(actionType)`，**从不读 `step.getActionConfig()`**（`actionConfig` 字段 `app-erp-cs.orm.xml:752` 存在但代码从不消费）→ L1 ②"根据 actionConfig 执行"❌。
  - **5 种 actionType 占位无副作用**：ASSIGN_TEAM/ASSIGN_AGENT :67-72 **仅写审计"占位登记"无真实团队分配**⚠️；REQUEST_APPROVAL :79-81 **仅写审计无审批链/超时自动审批**⚠️；NOTIFY_CUSTOMER :73-75 **仅写审计无 `notificationBiz.notify` 调用**❌；UPDATE_STATUS :76-78 **仅写审计无真实状态更新**❌；CREATE_CHILD_TICKET :86-88 + INVOKE_WORKFLOW :85-88 **SKIPPED（"归 successor 跨域编排/子工单"）**❌。
  - **失败暂停**：`executeFulfillmentSteps:44-49` 循环**从不因失败中断**（`executeStep` 恒返回 DONE/SKIPPED，从不抛/无 try-catch 暂停）；grep `pauseFlow|notifyAdmin` **零命中** → L1 ③"失败暂停+记录+通知管理员"❌。
  - **重试最多 3 次**：grep `retryCount|maxRetries|retry.*fulfillment` **零命中** → L1 异常"重试最多 3 次"❌。
  - **最终状态**：`executeStep` 从不触及 `ticket.status`，工单恒 NEW（`createFromCatalog:141` 设）→ L1 ④"全部完成进 IN_PROGRESS（或按配置 RESOLVED）"❌。

---

## §3 测试证据（L4 测试断言，注明断言强度）

> 测试根目录 `module-cs/erp-cs-service/src/test/java/`。引用 `<TestFile>.java#<method>`，注明断言强度（强/弱/仅冒烟）。

### UC-CS-08 调查 — `TestErpCsTicketSlaCsat.java`

- `testSurveyCreatedOnResolveAndSubmitted:177-204` —— **强**：resolve 触发 createSurvey + surveyToken 非空 + surveySentAt=now（delay=0 SENT）+ respondedAt 空 + submitSurvey(token,csat=5) 后 respondedAt 设置（COMPLETED）+ csatScore=5。
- `testSurveyDuplicateCreateRejected:206` —— 重复创建抛 ERR_SURVEY_ALREADY_EXISTS。
- `testSurveyInvalidTokenRejected:217` —— 无效 token 抛 ERR_SURVEY_TOKEN_INVALID。
- `testReopenCancelsUnrespondedSurvey:225-239` —— reopen 后 findSurveyByTicket 返回 null（删除未响应调查）。
- **缺口（零测试）**：延迟发送（delay>0 PENDING 调度）/ 渠道派发链接 / FAILED-重试 / 无鉴权访问匿名端点 / NPS-CES 提交（仅 CSAT 测）。

### UC-CS-09 权益 — `TestErpCsEntitlement.java`（13 @Test，强）+ `TestEntitlementMatcher.java`（14 @Test，强）

- `TestErpCsEntitlement`：`testConsumePayPerTicketIncrementsUsed:66` / `testConsumeExhaustedRejected:78` / `testConsumeWarrantyDoesNotIncrement:90` / `testConsumeSupportContractDoesNotIncrement:104` / `testConsumeExpiredRejected:116` / `testReleaseDecrementsNotBelowZero:128` / `testScanExpiringEntitlements:143` / `testDeactivateExpiredEntitlements:167` / `testGetEntitlementUsage:183` / `testTicketMatchAndAttachSlaConsumesEntitlement:204` / `testNoEntitlementAllowedByDefault:221` / `testNoEntitlementRejectedWhenDisallow:232` / `testEntitlementCheckDisabledSkipsConsume:250`。
- `TestEntitlementMatcher`：warranty/support/payPerTicket 匹配 + period expired/not-started + quota exhausted + inactive + partner mismatch + **nearest end date preferred:120** + null max unlimited + empty + null customer + resolveSla/Response override。
- **缺口（零测试）**：普通 save 自动联动（reuse P1-RC-054）/ 续约派发运行时（cron enabled=true 时）/ 自动停用 Job cron 接线端到端。

### UC-CS-10 目录请求 — `TestErpCsServiceCatalog.java`（8 @Test，强）

- 分类树守卫：`testCategorySelfCycleRejected:61` / `testCategoryChainCycleRejected:71` / `testCategoryMaxDepthExceededRejected:82` / `testCategoryDepthWithinLimitAllowed:95` / `testCategoryDeleteWithChildrenRejected:106`（5 例强断言各 ERR 码）。
- 建单填充：`testCreateFromCatalogFillsTicketFields:118` —— 强（ticketTypeId/slaPolicyId/catalogItemId/subject/description/priority=urgency + status=NEW）/ `testCreateFromCatalogInactiveRejected:147`（ERR_CATALOG_ITEM_INACTIVE）/ `testCreateFromCatalogSubjectFallbackToItemName:159`。
- **缺口（零测试）**：必填表单校验 / requestFormConfig schema 驱动校验 / 履行失败告警通知管理员。

### UC-CS-12 履行流程 — `TestErpCsServiceCatalog#testFulfillmentCreateTicketStepRegistered:179-218`（弱）

- **弱断言**：仅 `actions.stream().anyMatch(a -> actionType 等于 && content.contains("DONE"))` 验证 TicketAction 审计行存在（CREATE_TICKET DONE / INVOKE_WORKFLOW SKIPPED / ASSIGN_TEAM / NOTIFY_CUSTOMER），**不验证真实分配/通知/状态副作用**。
- **缺口（零测试）**：真实 ASSIGN_TEAM/NOTIFY/UPDATE_STATUS 副作用 / REQUEST_APPROVAL 链 / CREATE_CHILD_TICKET / 失败暂停 / 重试 3 次 / actionConfig 解析 / 最终状态更新。

### E2E（引用，非本切片范围）

- `tests/e2e/business-actions/cs-ticket.action.spec.ts`（6 态状态机 only，A1.37 范围）；`tests/e2e/reports/cs-ticket-sla-csat.value.spec.ts`（token 断言）。

---

## §4 运行时行为证据（L5，与 MA2 去重）

- **A2.14 不覆盖本切片 UC**（§0 已声明）→ L5 无可复用 MA2 行为证据，本切片以 L3 代码 + L4 单测为行为依据。
- **P1-MA2-086（resolved R1.28）**：`erp-cs-entitlement-expiry` / `erp-cs-csat-reminder` 两 cron 的并发幂等守卫经 R1.28 修复——本切片引作 **cron 接线已证实行为**（不重审并发维度）。
- **静态存疑点**（无法静态定论，需运行时确认）登记于 §7（SP-1~SP-5），交 MA4 展开。

---

## §5 符合性结论（五级追踪矩阵 + 每 UC 结论，methodology §2 判据）

> 每个 UC 一行；候选缺口逐条对照 L1 验收标准，§2 取最高分级；P1 项含 §4 三判据复核结论。

### 五级追踪矩阵

| UC 编号 | L2 owner doc 契约 | L3 代码 | L4 测试 | L5 运行时 | 符合性结论 |
|---------|------------------|---------|---------|-----------|-----------|
| UC-CS-08 | `csat.md §一-三/§实现约定`（设计参考，冲突以 L1 为准；§实现约定 AI 自标 Non-Goal，§4 三判据见下） | `ErpCsTicketResolveProcessor#resolve:60-63` + `ErpCsSurveyCreateSurveyProcessor#createSurvey:30-58` + `ErpCsSurveyBizModel#submitSurvey:60-94` + `ErpCsTicketReopenProcessor#cancelUnrespondedSurvey:53-64` | `TestErpCsTicketSlaCsat#testSurveyCreatedOnResolveAndSubmitted`（强）+ 3 例守卫；**延迟/派发/FAILED/匿名/NPS-CES 零测** | L3 静态：延迟发送调度/渠道派发/FAILED-重试/匿名端点缺（SP-1~SP-2 交 MA4） | **部分接受 + P1-RC-059 + P2-RC-054** |
| UC-CS-09 | `entitlement.md §一-五/§八`（§8.1 建单集成钩子 Decision 与实现一致） | `ErpCsEntitlementBizModel#matchForCustomer/consume/release/scan/deactivate` + `EntitlementMatcher#match:37-55` + `ErpCsEntitlementExpiryJob` + `ErpCsTicketMatchAndAttachSlaProcessor#matchAndConsumeEntitlement:76-95` | `TestErpCsEntitlement`（13 强）+ `TestEntitlementMatcher`（14 强） | 核心 5 路径 L3+L4 强证实；普通保存联动复用 P1-RC-054 | **接受（核心）+ reuse P1-RC-054（普通保存联动）** |
| UC-CS-10 | `service-catalog.md §一/§二/§九`（§9.1 履行范围收窄 AI 自标 successor，§4 三判据见下） | `ErpCsServiceCatalogItemCreateFromCatalogProcessor#createFromCatalog:41-69` + `buildTicketData:129-166` + `ErpCsCatalogCategoryBizModel#defaultPrepareSave/Update/Delete:47-126` | `TestErpCsServiceCatalog`（8 强：分类树 5 + 建单 3）；**必填校验/失败告警零测** | L3 静态：必填校验缺/失败告警降级（SP-4~SP-5 交 MA4） | **部分接受 + P1-RC-060 + P2-RC-055** |
| UC-CS-12 | `service-catalog.md §三/§9.1`（§9.1 多步履行"产品基线外 protected 扩展点" AI 自标，§4 三判据见下） | `ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor#executeFulfillmentSteps:35-51` + `executeStep:57-94` | `TestErpCsServiceCatalog#testFulfillmentCreateTicketStepRegistered:179-218`（**弱**，仅审计行 anyMatch） | L3 静态：actionConfig 未解析/NOTIFY-UPDATE_STATUS 占位无副作用/失败不暂停/无重试/最终状态不更新/CREATE_CHILD+INVOKE_WORKFLOW SKIPPED | **P1-RC-061** |

### 每 UC 符合性结论（逐条验收标准 + §2 判据 + §4 三判据复核）

#### UC-CS-08 满意度调查发送与评分 → **部分接受 + P1-RC-059 + P2-RC-054**

逐条：
- ① **延迟 X 小时后创建 ErpCsSurvey**：createSurvey 实现但 **delay>0 时 PENDING 调查孤立无 Job 消费** → 命中 **§2 P1①（功能完全缺失——"延迟发送"调度缺失）**。详见 **P1-RC-059**。
- ② **按客户渠道发送含 surveyToken 的调查链接**：surveyChannel=PORTAL 仅描述，**无实际派发（无 notify/email/link push）** → §2 P1①。详见 **P1-RC-059**。
- ③ **无鉴权访问调查页面**：submitSurvey 标准 @BizMutation 需服务上下文，**无匿名端点**；触及鉴权配置（ai-autonomy-policy `auth/permissions` plan-first）→ 倾向 **P2**（主路径 token 查找+评分 OK，匿名访问边界弱）。详见 **P2-RC-054**。
- ④ **CSAT/NPS/CES 评分及文字反馈**：字段在（orm.xml:484-486）+ config-gated range 校验 ✅（NPS/CES config 默认 off 属配置策略非缺陷）→ **接受**。
- ⑤ **更新 ErpCsSurvey（respondedAt、评分字段）**：submitSurvey :86-92 ✅ → **接受**。
- ⑥ **评分计入客服绩效报表**：`buildTicketSlaCsatSummaryDataset` ✅ → **接受**。
- **后置 COMPLETED/FAILED 终态**：无 status 列，**FAILED 终态不可持久化/不可达** → §2 P1① + ②。详见 **P1-RC-059**。
- **异常 重开取消**：cancelUnrespondedSurvey ✅ → **接受**。
- **异常 发送失败标记 FAILED 并重试**：**完全缺失** → §2 P1②（异常路径未实现）。详见 **P1-RC-059**。

**§4 三判据复核（UC-CS-08 候选 P1：延迟发送/渠道派发/FAILED-重试）**：
- **(i) plan 含独立 plan-audit 通过记录**：✗ 产生本功能的计划（`2026-07-04-0700-2` / `2026-07-06-0642-1`）为 2026-07 实现期产物，无 RC 式独立 plan-audit 通过记录裁决 cs-F4 调查延迟/派发/FAILED-重试裁剪。
- **(ii) owner doc 显式 documented simplification 标注且经人工批准**：`csat.md §实现约定` 显式标"扩 status 列归 Non-Goal / cron 注册归 Non-Goal / 实际邮件发送归 nop-notification 独立面"；`README.md §延迟项与非目标:100` "回访问卷状态派生：失败/过期仅在查询期判定"。**但 `git log -- docs/design/customer-service/csat.md /README.md` 全部提交作者 = `canonical`（AI），无人工批准痕迹**（commit/discussion/人工签字）。按 methodology §4 line 168「AI 自标 ≠ 人工批准」，判据 (ii) **不成立**。
- **(iii) product-scope 范围裁剪登记**：✗ CSAT 调查为 product-scope 列明的 CS 域核心能力，未列入"不在范围/后续阶段"。
- **三判据均不成立 → Q4=(a) P1 强制实现**，倾向 **P1-RC-059**（合并 ①②+后置 FAILED+异常重试；③无鉴权访问独立 P2-RC-054）。

#### UC-CS-09 服务权益校验 → **接受（核心）+ reuse P1-RC-054（普通保存联动）**

逐条：
- ① 查询有效权益（matchForCustomer ✅）/ ② 最近到期（EntitlementMatcher min endDate ✅）/ ③ usedTickets+1（consumeEntitlement ✅）/ ④ 30 天续约（scanExpiringEntitlements + ExpiryJob ✅）/ ⑤ 到期停用（deactivateExpiredEntitlements ✅）/ 异常 超限拒绝（ERR_ENTITLEMENT_EXHAUSTED ✅）/ 异常 无权益放行（isAllowNoEntitlement ✅）→ **全部接受**（命中 §2 接受判据，13+14 强测）。
- **① 普通工单保存自动联动**：`save` 不自动调 `matchAndAttachSla`；`auto-assign-on-create` config 死标志 → **复用 P1-RC-054**（A1.37 已登记，同根因同控制点 = 建单自动富化缺失，权益校验是其副作用之一）。**在既有 P1-RC-054 行追加 A1.40/UC-CS-09 交叉引用，不新建**（§去重协议）。

#### UC-CS-10 服务目录请求提交 → **部分接受 + P1-RC-060 + P2-RC-055**

逐条：
- ① 浏览分类树（实体 + 守卫 ✅，无独立浏览 @BizQuery 方法但 CRUD 列表可达）/ ② 加载 requestFormConfig（字段在 ✅）/ ③ 填写提交（createFromCatalog ✅）/ ④ 自动填充（buildTicketData ✅ ticketType/slaPolicy/priority/catalogItemId）/ ⑤ 履行顺序执行（executeFulfillmentSteps sequence ✅ 框架）/ 异常 inactive 拒绝 ✅ → **主路径接受**。
- **异常 必填项缺失禁止提交**：`buildTicketData:146-156` 盲拷 keys，**无 schema 必填校验** → §2 P1②（异常路径未实现）+ ①（行为实质偏离）。详见 **P1-RC-060**。
- **异常 履行失败 → 工单保持 NEW + 告警通知管理员**：工单保持 NEW ✅（:56-68 建单后降级），**告警通知管理员 缺**（仅 LOG.warn）→ §2 P2①（次要验收未满足，主路径"工单保持"OK）+ 可用性要求。详见 **P2-RC-055**。

**§4 三判据复核（UC-CS-10 候选 P1：必填校验）**：
- (i) ✗ 无独立 plan-audit 裁决"必填校验裁剪"；(ii) ✗ `service-catalog.md §1.4` requestFormConfig 含 `required:true` 字段定义（**暗示应强制**，未标 Non-Goal；§9.1 仅裁剪履行编排不涉表单校验），`git log` 全 `canonical` AI 提交无人工批准；(iii) ✗ product-scope 未裁剪。**三判据均不成立 → P1 强制实现** → **P1-RC-060**（纯 BizModel 代码逻辑，预授权不触 §5 ask-first）。

#### UC-CS-12 服务目录履行流程 → **P1-RC-061**

逐条：
- ① sequence 升序 ✅（接受，框架正确）。
- ② actionConfig 驱动 ❌（从不解析）/ ③ 5 种 actionType（ASSIGN_TEAM/REQUEST_APPROVAL 占位无副作用，NOTIFY_CUSTOMER/UPDATE_STATUS 占位无副作用，CREATE_CHILD_TICKET/INVOKE_WORKFLOW SKIPPED）/ ③ 失败暂停 ❌ / ④ 最终状态不更新 ❌ / 异常 重试最多 3 次 ❌ → §2 P1①（功能完全缺失/行为实质偏离验收标准）+ ②（异常路径未实现）+ ⑤（测试仅冒烟弱断言）。详见 **P1-RC-061**。

**§4 三判据复核（UC-CS-12 候选 P1：履行 actionType 实化/失败暂停/重试/最终状态）**：
- (i) ✗ 产生计划 `2026-07-07-1430-1` 无独立 plan-audit 裁决"多步履行裁剪"；(ii) **`service-catalog.md §9.1` 显式标"服务目录履行仅落地 CREATE_TICKET 首步……多步履行编排（ASSIGN_TEAM/.../CREATE_CHILD_TICKET/INVOKE_WORKFLOW）为产品基线外——executeStep 为 protected 方法，作为产品化扩展点"**，但 `git log -- docs/design/customer-service/service-catalog.md` 全部提交作者 = `canonical`（AI），**无人工批准痕迹**（commit/discussion/人工签字），按 §4 line 168「AI 自标 ≠ 人工批准」判据 (ii) **不成立**；(iii) ✗ product-scope 未将服务目录履行列入范围裁剪（服务目录为 CS 域列明能力）。
- **CREATE_CHILD_TICKET / INVOKE_WORKFLOW 的 successor 注记单独裁决**：代码 :86-88 注释"归 successor（跨域编排/子工单）"，§9.1 同源 AI 自标 successor——**非经人工批准的 successor**（§4 三判据均不满足），故**并入 P1-RC-061**（不独立 P2）。
- **三判据均不成立 → Q4=(a) P1 强制实现** → **P1-RC-061**。

### 本切片 finding 汇总（3 新 P1 + 2 新 P2 + 1 reuse）

| Finding | UC | 缺口摘要 | 分级 | §5 裁决依据 |
|---------|----|---------|------|------------|
| **P1-RC-059**（新） | UC-CS-08 | 调查延迟发送调度缺失（PENDING 孤立）+ 渠道派发链接缺失 + FAILED 终态/失败重试缺失 | P1 | §2 P1①+②；§4 三判据均不成立；部分触及 ORM（FAILED 持久化需 status/failedAt 列）ask-first + 核心为 cron 接线+notify 派发预授权 |
| **P2-RC-054**（新） | UC-CS-08 ③ | 无鉴权访问调查页面（submitSurvey 需认证，无匿名端点） | P2 | §2 P2①；触及鉴权配置 plan-first；主路径 OK 边界弱，声明 Q4=(a) 张力 |
| reuse **P1-RC-054** | UC-CS-09 | 普通工单 save 不自动联动权益校验（auto-assign-on-create 死标志） | P1 | A1.37 已登记，同根因同控制点，追加交叉引用不新建 |
| **P1-RC-060**（新） | UC-CS-10 | 必填表单校验缺失（requestFormConfig schema 未驱动校验，盲拷 keys） | P1 | §2 P1①+②；§4 三判据均不成立；纯 BizModel 逻辑预授权不触 ask-first |
| **P2-RC-055**（新） | UC-CS-10 | 履行失败告警缺失（仅 LOG.warn 无管理员告警通知） | P2 | §2 P2①；主路径"工单保持 NEW"OK，告警通知缺；纯 BizModel+notify 预授权 |
| **P1-RC-061**（新） | UC-CS-12 | 履行 actionConfig 未解析 + NOTIFY/UPDATE_STATUS 占位无副作用 + 失败不暂停 + 无重试 + 最终状态不更新 + CREATE_CHILD/INVOKE_WORKFLOW SKIPPED | P1 | §2 P1①+②+⑤；§4 三判据均不成立（§9.1 AI 自标 successor 不成立）；部分触及 ORM（retryCount 持久化）ask-first + 须协调 notify 域（IErpSysNotificationBiz）+ 审批引擎（nop-workflow） |

---

## §6 与 arm-index 衔接（methodology §7"复用 or 新增"裁决）

> 每条 finding 产出前已 grep `arm-index.md` 同域（cs）同控制点（survey/csat/entitlement/catalog/fulfillment/delay/token/approval/child-ticket/notify/status/retry/必填/告警）后裁决。

| Finding | 裁决 | 与既有 finding 的差异依据（grep 依据） |
|---------|------|--------------------------------------|
| **P1-RC-059**（UC-CS-08） | **新建** | grep arm-index cs survey/csat/delay/dispatch/FAILED：既有仅 `P1-MA2-086`（cron 并发幂等，resolved R1.28，不同维度=并发去重 vs 调度缺失）、`P1-RC-054`（auto-assign 死标志，不同控制点=建单富化 vs 调查发送链）、`P1-RC-056`（cs-F2 多级升级，不同 UC）。**调查延迟发送/渠道派发/FAILED-重试为新根因（调度消费链缺失 + 派发链缺失 + 终态持久化缺失）→ 新建** |
| **P2-RC-054**（UC-CS-08 ③） | **新建** | grep arm-index 匿名/anonymous/无鉴权/survey 公开端点：零命中 → 新建 |
| reuse **P1-RC-054**（UC-CS-09） | **复用** | `P1-RC-054`（A1.37 UC-CS-01 建单自动富化 + auto-assign 死标志）同根因同控制点——UC-CS-09 普通保存权益联动是 auto-assign 死标志的副作用。**既有 P1-RC-054 行追加"A1.40/UC-CS-09 权益保存联动交叉引用"，不新建**（§去重协议） |
| **P1-RC-060**（UC-CS-10） | **新建** | grep arm-index cs catalog/必填/requestFormConfig/表单校验：零命中 → 新建 |
| **P2-RC-055**（UC-CS-10） | **新建** | grep arm-index cs catalog 履行失败告警/notify admin：零命中（`P1-RC-061` 为履行占位执行，不同控制点=执行 vs 告警）→ 新建 |
| **P1-RC-061**（UC-CS-12） | **新建** | grep arm-index cs fulfillment/actionConfig/履行/actionType：零命中 → 新建（与 `P2-RC-055` 不同控制点=履行执行副作用 vs 失败告警通知） |

### 修复行预留（MR1 RC-R1.n 触发条件 + 触及区域标注）

- **P1-RC-059**：MR1 RC-R1.n 修复行须实现 ①nop-job 调度消费 PENDING（surveySentAt=null）调查发送链 + ②notificationBiz.notify 派发调查链接（EMAIL/PORTAL 渠道）+ ③FAILED 终态持久化（**触及 ORM 结构变更[ErpCsSurvey 加 status 或 failedAt/retryCount 列]须 ask-first + 独立 plan-audit §5 ORM 类**）+ ④失败重试逻辑。
- **P2-RC-054**：MR1 RC-R1.n 修复行须提供匿名/无鉴权提交端点（**触及鉴权配置[auth/permissions]plan-first + 独立 plan-audit**）。
- **P1-RC-060**：MR1 RC-R1.n 修复行须解析 requestFormConfig JSON schema + 必填字段校验（**纯 BizModel 代码逻辑，按 roadmap 预授权类目可自动执行，不触 §5 ask-first**）。
- **P2-RC-055**：MR1 RC-R1.n 修复行须履行失败时 notificationBiz.notify 告警管理员（**纯 BizModel + notify 接线，预授权不触 ask-first**）。
- **P1-RC-061**：MR1 RC-R1.n 修复行须 ①解析 actionConfig 驱动各 actionType + ②ASSIGN_TEAM 实化（mode 分配）+ ③REQUEST_APPROVAL 实化（**协调审批引擎 nop-workflow**）+ ④NOTIFY_CUSTOMER 实化（**协调 notify 域 IErpSysNotificationBiz，可与 A1.39 UC-CS-06 notify 模式协同**）+ ⑤UPDATE_STATUS 实化（更新 ticket.status）+ ⑥CREATE_CHILD_TICKET 实化（建子工单）+ ⑦失败暂停+记录+通知管理员 + ⑧重试最多 3 次（**触及 ORM 结构变更[ErpCsCatalogFulfillment 加 retryCount/lastError 列]须 ask-first + 独立 plan-audit §5 ORM 类**）+ ⑨最终状态更新（IN_PROGRESS/RESOLVED）。
- **reuse P1-RC-054**：修复随 A1.37 P1-RC-054 修复行（建单自动富化含权益保存联动）。

### 协调声明

- UC-CS-08 survey 终态修复若加 status 列 → **触及 ORM 须 ask-first**（§5 ORM 类）。
- UC-CS-12 履行 actionType 实化须**协调 notify 域（IErpSysNotificationBiz）+ 审批引擎（nop-workflow）**；retryCount 持久化触及 ORM 须 ask-first。

---

## §7 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行。无运行时探针（本计划纯静态）。

- **SP-1（UC-CS-08）**：`survey-send-delay` 默认 0 时 PENDING 路径是否实际可达——delay=0 时 surveySentAt=now（SENT），delay>0 时 PENDING 孤立；生产环境是否配置 delay>0（若恒 delay=0 则 PENDING 路径 dead 但 config 已暴露）。需 MA4 运行时确认 config 实际取值 + PENDING 调查是否在生产累积。
- **SP-2（UC-CS-08）**：`submitSurvey` 经 token 调用在真实鉴权配置下是否拒绝匿名调用——单测 `enableActionAuth=FALSE` 绕过鉴权；生产 enableActionAuth=TRUE 时匿名（无 ctx.getUserId()）调用是否被平台层拦截。需 MA4 运行时确认匿名端点缺失的实际阻断面。
- **SP-3（UC-CS-09，复用 P1-MA2-086 R1.28）**：`erp-cs-entitlement-expiry` / `erp-cs-csat-reminder` cron enabled=true 时幂等行为——R1.28 已修并发去重，本切片引作 cron 接线证据（不重审并发维度），MA4 可复核生产 cron 表达式 + 实际触发。
- **SP-4（UC-CS-10/12）**：`createFromCatalog` 履行占位审计行（"DONE: assignToRole=…(本期占位登记)"）在前端/运维是否被误读为真实执行——审计 content 含"占位"字样但 actionType=DONE，前端若按 actionType 渲染"已完成"会误导。需 MA4 复核前端 AMIS 对 TicketAction 的渲染口径。
- **SP-5（UC-CS-10）**：`requestFormConfig` 当前生产数据是否含可驱动校验的 schema（fields[].required）——种子测试未设 requestFormConfig；若生产 catalogItem 已维护含 required 的 schema 则 P1-RC-060 修复可纯逻辑解析，否则需配套数据治理。

### P0 即时通道评估

- 履行占位/调查缺是否破坏活跃数据/会计——**倾向否**：影响限于 SLA 计时/客户体验/服务交付质量，不破坏财务数据/库存/凭证正确性，不构成 §2 P0（活跃数据破坏/会计过账正确性/安全隔离）。**不触发 MR0**。

---

## §8 过程纪律自检（methodology §8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 见下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（本次实测退出码 = 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本审计为只读审计，无生产代码变更，checker 无回归风险**（actual 偏移为审计前既有状态，非本审计引入）。

  | 规则 | 描述 | baseline 命中 | actual 实测（本审计时） | 评估 |
  |------|------|--------------|------------------------|------|
  | R1a/b/c | dao() 直接调用 BizModel（save/update/getEntityById） | 0/0/0 | 0/0/0 | = baseline |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | = baseline |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | = baseline |
  | R2b | BizModel daoFor(Erp*) 跨域 | 240（基线表）/ 后续注记已下降 | 229 | ≤（审计前既有下降趋势） |
  | R2c | 全生产代码 daoFor() 总量 | 1380 | 1382 | +2（审计前既有，非本审计引入；本审计零代码变更） |
  | R2d | Processor daoFor(ErpMd*) | 32 | 34 | +2（审计前既有，非本审计引入） |
  | R3 | new Erp*() 构造实体 | 5 | （规则运行未超基线） | ≤ |
  | R5 | @Inject private | 0 | 0 | = baseline |

  > 声明：上表 actual 为本审计执行时实测快照；R2b/R2c/R2d 的 +N 偏移是审计前既有仓库状态（compliance-baseline.md 多版注记显示 R2 系列经多轮重构持续下降，基线表与 inline 注记存在口径差异），**本审计零生产代码变更，不引入任何新命中**，故无回归风险。CI 门控由 `.github/workflows/compliance.yml` 强制；如 CI 因既有偏移 fail，须由独立基线裁决计划处理，与本审计无关。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P1-RC-059/060/061 + P2-RC-054/055 + reuse P1-RC-054）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（见 §6 表），无未经比对直接新建的 finding。
- [x] **真相源未修改声明**：本审计未修改 `product-scope.md` / `use-cases.md` / owner doc 需求契约段落 / `model/*.orm.xml` / 代码（§9 冻结条款遵守）。分歧记入本报告 §5/§6。

---

## §9 与 MA2 报告差异增量声明（methodology §6 段落 9 / §去重协议）

（与 §0 一致，此处列明只补的需求视角差异）

- **A2.14 范围不含 UC-CS-08/09/10/12**（A2.14:21-23/218-228 显式声明范围 = Ticket 6 态 + SLA 计时联动 + SLA 升级 cron），无可复用行为证据。
- **P1-MA2-086（resolved R1.28）引作 cron 接线证据**：`erp-cs-entitlement-expiry` / `erp-cs-csat-reminder` 并发幂等经 R1.28 修复——本切片不重审并发维度，只补需求视角差异：
  - **UC-CS-08**：调查延迟发送调度缺失（PENDING 孤立）/ 渠道派发链接缺失 / FAILED 终态-重试缺失 / 无鉴权访问（A2.14 未涉调查发送链）。
  - **UC-CS-09**：核心 5 路径完整（A2.14 未涉权益）；普通保存联动复用 P1-RC-054。
  - **UC-CS-10**：必填表单校验缺失 / 履行失败告警降级（A2.14 未涉服务目录建单表单）。
  - **UC-CS-12**：actionConfig 未解析 / NOTIFY-UPDATE_STATUS 占位无副作用 / 失败不暂停 / 无重试 / 最终状态不更新 / CREATE_CHILD+INVOKE_WORKFLOW SKIPPED（A2.14 未涉履行编排）。

---

## §自检（报告 9 段完整性，methodology §6 段落完整性自检）

- [x] §0 与 MA2 报告差异增量声明（声明段，对应段落 9 前置）
- [x] §1 需求契约原文（L1 逐字引用 UC-CS-08/09/10/12）
- [x] §2 实现证据（L3 代码路径 + 行为断言 + 跨域 Facade）
- [x] §3 测试证据（L4 注明断言强度）
- [x] §4 运行时行为证据（L5 + A2.14 不含 + P1-MA2-086 cron 复用）
- [x] §5 符合性结论（五级矩阵 + 每 UC 结论 + §2 判据 + §4 三判据复核）
- [x] §6 与 arm-index 衔接（复用/新增裁决 + 修复行预留 + 协调声明）
- [x] §7 静态存疑点清单（SP-1~SP-5 + P0 评估）
- [x] §8 过程纪律自检（checker actual vs baseline 表 + 独立性 + 去重 + 真相源未修改）
- [x] §9 与 MA2 报告差异增量声明

**9 段齐全**（§0 为 §9 的前置声明段，§1-§9 完整）。本报告可交 closure audit。
