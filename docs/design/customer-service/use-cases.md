# 售后服务/客服工单域 — 典型用例

## UC-CS-01 客户创建工单（Ticket Creation）

**触发条件** 客户通过自助门户/邮件/电话提交服务请求。

**前置条件** 客户已在 ERP 中注册（ErpMdPartner）。

**流程** 
1. 客户提交工单信息（subject、description、customerId、priority）。
2. 系统读取客户信息，建议匹配的 ticketType、slaPolicy。
3. 系统自动计算 SLA 截止时间（deadlineDateTime = now + slaPolicy.resolveHours）。
4. 系统根据 ticketType + team 匹配规则自动分配处理人（轮转/最少未结工单）。
5. 工单状态 → NEW → ASSIGNED。
6. 系统向客户发送工单确认通知（含工单编号 TK{YYYYMM}{SEQ4}）。

**后置条件** 工单进入 ASSIGNED 状态，处理人待办列表出现新工单。

**异常** 自动分派无匹配处理人 → 留 NEW 状态，升级通知客服主管人工分派。

---

## UC-CS-02 工单分派与接受（Ticket Assignment）

**触发条件** 主管/系统将 NEW 工单分派给特定处理人。

**前置条件** 工单状态为 NEW 或 ASSIGNED（重新分派）。

**流程** 
1. 客服主管查看未分派工单列表。
2. 主管选择处理人（查看团队忙闲状态/技能匹配）。
3. 系统更新 assignedToId，工单状态 → ASSIGNED。
4. 处理人收到通知，点击"接受"→ IN_PROGRESS，startDateTime 记录。
5. 如处理人拒绝，填写拒绝原因，工单回到 NEW 待重新分派。

**后置条件** startDateTime 开始计时，SLA 调用。

**异常** 处理人长时间（>2h）不响应 → 自动升级通知主管。

---

## UC-CS-03 工单解决与客户确认（In-Progress Resolution）

**触发条件** 处理人完成问题定位/修复，提交解决方案。

**前置条件** 工单处于 IN_PROGRESS 状态。

**流程** 
1. 处理人填写 resolution 内容、操作步骤。
2. 处理人可上传附件（截图/补丁/文档）。
3. 系统校验必填项，工单状态 → RESOLVED。
4. SLA 停止计时，计算实际耗时（duration = now - startDateTime）。
5. 系统判断是否超时（now > deadlineDateTime → isSlaCompleted=false）。
6. 通知客户确认 通过门户/邮件查看解决方案。
7. 客户确认 → CLOSED（endDateTime=now）；客户驳回 → IN_PROGRESS。

**后置条件** RESOLVED 状态保持到客户确认，超时未确认（>7天）自动 CLOSED。

**异常** 处理人超时未解决 → 触发升级流程（参见用例 4）。

---

## UC-CS-04 SLA 超时与升级（SLA Breach & Escalation）

**触发条件** nop-job 定时扫描发现 deadlineDateTime 已过且工单未 RESOLVED。

**前置条件** 工单处于 ASSIGNED/IN_PROGRESS 状态且已超 deadlineDateTime。

**流程** 
1. nop-job 扫描 erp_cs_ticket 表，条件 `status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now()`。
2. 系统创建 ErpCsTicketAction（actionType=ESCALATE）。
3. 通知 slaPolicy.escalationUserId（客服经理）。
4. 客服经理评估超时原因，决策 
   - 重新分派 → 状态保持 ASSIGNED，更改 assignedToId。
   - 延长 deadline（系统管理员操作）→ 更新 deadlineDateTime。
5. 系统记录超时原因和升级处理记录。

**后置条件** 升级记录可追溯，超时率纳入 SLA 绩效报表。

**异常** 重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级。

---

## UC-CS-05 知识库搜索与建议（Knowledge Base Suggestion）

**触发条件** 客户或客服在创建/编辑工单时输入 subject。

**前置条件** 知识库中存在已发布的文章（isPublished=true）。

**流程** 
1. 系统实时解析 subject 关键词。
2. 按关键词全文搜索 erp_cs_knowledge_base 表，按相关性排序。
3. 在工单界面展示 Top 5 匹配文章（标题 + 摘要）。
4. 客户/客服点击查看完整内容，可"采纳"标记为已参考。
5. 如采纳的文章解决了问题，工单直接标记为 RESOLVED。

**后置条件** 采纳记录计入知识库使用统计。

**异常** 无匹配文章 → 提示客服创建新知识库条目（工单解决后自动推送建议）。

---

## UC-CS-06 工单升级为质量事件（Escalation to Quality NCR）

**触发条件** 处理人确认工单问题属于产品质量缺陷。

**前置条件** 工单处于 IN_PROGRESS 状态。

**流程** 
1. 处理人标记"质量问题"，填写缺陷描述、物料/批次信息。
2. 系统调用 quality 域 I*Biz 接口创建 ErpQaNonConformance。
3. 工单操作日志记录 ESCALATE 操作（关联 NCR 编号）。
4. 工单继续原有流程（RESOLVED → CLOSED），NCR 流程独立进行。
5. NCR 闭环后，工单可查看到 NCR 处理结果。

**后置条件** 工单与 NCR 关联，跨域可追溯。

**异常** quality 域服务不可用 → 延迟创建 NCR，工单先保留状态，后台自动重试。

---

## UC-CS-07 预设应答使用（Canned Response Usage）

**触发条件** 客服在处理工单时选择插入预设应答模板。

**前置条件** 预设应答模板已维护（ErpCsCannedResponse.isActive=true）。

**流程**
1. 客服在处理工单界面点击"插入预设应答"。
2. 系统展开分类树（billing/technical/account），显示可用模板列表。
3. 客服选择模板 → 系统预览渲染后内容（自动填充系统变量）。
4. 客服补充自定义变量（如 {email}）→ 系统校验必填变量。
5. 客服点击"插入"→ 渲染后的正文填入工单回复编辑框。
6. 客服可手动修改后发送 → 创建工单操作日志（actionType=NOTE）。
7. usageCount +1。

**后置条件** 工单操作日志记录渲染后的应答内容。

**异常** 缺失必填变量 → 禁止发送，高亮未填项。

---

## UC-CS-08 满意度调查发送与评分（CSAT Survey）

**触发条件** 工单进入 RESOLVED 状态。

**前置条件** erp-cs.survey-enabled=true。

**流程**
1. 工单 → RESOLVED，系统延迟 X 小时（配置项 erp-cs.survey-send-delay）后创建 ErpCsSurvey。
2. 系统按客户渠道发送含 surveyToken 的调查链接。
3. 客户点击链接 → 无鉴权访问调查页面。
4. 客户提交 CSAT/NPS/CES 评分及文字反馈。
5. 系统更新 ErpCsSurvey（respondedAt、评分字段）。
6. 评分计入客服绩效报表。

**后置条件** 调查终态为 COMPLETED 或 FAILED。

**异常** 延迟期间工单重回 IN_PROGRESS → 取消该次调查；发送失败 → 标记 FAILED 并重试。

---

## UC-CS-09 服务权益校验（Entitlement Check）

**触发条件** 创建工单时指定客户（customerId）。

**前置条件** erp-cs.entitlement-check-enabled=true。

**流程**
1. 创建工单 → 系统查询客户的有效权益（ErpCsEntitlement）。
2. 取有效期最近的权益 → 写入工单.slaPolicyId。
3. 更新 entitlement.usedTickets = usedTickets + 1。
4. 到期前 30 天 nop-job 扫描创建续约提醒通知。
5. 到期日 isActive 自动设为 false。

**后置条件** 工单关联 SLA 策略，权益使用计数+1。

**异常** 无有效权益 → 可选配置允许创建"无权益工单"；usedTickets >= maxTickets → 拒绝创建（按次计费场景）。

---

## UC-CS-10 服务目录请求提交（Catalog Request）

**触发条件** 客户通过门户或客服代提交选择服务目录项。

**前置条件** erp-cs.service-catalog-enabled=true。

**流程**
1. 客户/客服浏览服务目录分类树。
2. 选择目录项 → 加载 requestFormConfig（动态表单）。
3. 填写表单 → 提交。
4. 系统创建 ErpCsTicket（ticketTypeId、slaPolicyId、priority 自动填充，catalogItemId 记录来源）。
5. 按 ErpCsCatalogFulfillment 履行流程顺序执行 actionType 序列。

**后置条件** 工单创建成功，履行流程启动。

**异常** 表单必填项缺失 → 禁止提交；履行流程执行失败 → 工单状态保持 NEW，告警通知管理员。

---

## UC-CS-11 工单计时录入（Time Tracking）

**触发条件** 客服开始处理工单，需要记录处理时长。

**前置条件** erp-cs.time-tracking-enabled=true。

**流程**
1. 客服点击"开始计时"→ 系统创建计时器 session。
2. 客服可暂停/恢复计时（暂停原因可选）。
3. 客服点击"停止计时"→ 生成 ErpCsTimeEntry（startTime、endTime、duration 自动计算）。
4. 客服补充 description、isBillable 标识 → 提交。
5. 可计费条目自动进入审批（或超阈值触发审批）。
6. 审批通过 → 工单总工时聚合（totalTimeSpent、totalBillableTime）。

**后置条件** TimeEntry 进入 PENDING/APPROVED 状态，工单聚合工时可查。

**异常** 单次计时超 12h → 自动停止；同一客服同一时刻只能启动一个计时器。

---

## UC-CS-12 服务目录履行流程（Service Fulfillment）

**触发条件** 服务目录项提交后，系统按履行流程执行 actionType 序列。

**前置条件** 目录项已配置履行映射（ErpCsCatalogFulfillment）。

**流程**
1. 系统按 ErpCsCatalogFulfillment.sequence 升序依次执行。
2. 每个 actionType 根据 actionConfig 执行对应动作：
   - ASSIGN_TEAM → 按 mode 策略分配团队。
   - REQUEST_APPROVAL → 发起审批链，超时自动审批。
   - CREATE_CHILD_TICKET → 创建子工单。
   - NOTIFY_CUSTOMER → 发送通知。
   - UPDATE_STATUS → 更新工单状态。
3. 某一步失败 → 暂停流程，记录错误信息，通知管理员。
4. 全部步骤完成 → 工单进入 IN_PROGRESS（或按配置 RESOLVED）。

**后置条件** 履行流程状态可跟踪，异常可重试。

**异常** actionType 执行失败 → 支持重试（最多 3 次），超出后通知管理员人工介入。
