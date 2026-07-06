-- 通知派发子系统种子模板（plan 2026-07-06-0504-1 Phase 4 + 2026-07-06-0642-1 Phase 2/3 + 2026-07-06-0642-2 审批通知）
-- 三类通知各一样例，证明三类频控与通道分支可端到端运行。
-- 接收人解析：ROLE（角色名 → nop-auth NopAuthUserRole → userId）；角色名取自 docs/design/roles-and-permissions.md。
-- 角色未落地时 recipientResolver 静默返回空并 WARN（config-gated，不阻断业务）。
-- USER_LIST 接收人支持 ${var} 占位符从 notify context 插值（提单人 submitterUserId）。

INSERT INTO erp_sys_notification_template
  (ID, NOTIFICATION_TYPE, NAME, CHANNEL_SET, SUBJECT_TPL, BODY_TPL,
   RECIPIENT_RESOLVER, RECIPIENT_CONFIG, MERGE_WINDOW_SECONDS, MERGE_STRATEGY, STATUS,
   REMARK, DEL_VERSION, VERSION, CREATED_BY, CREATE_TIME, UPDATED_BY, UPDATE_TIME)
VALUES
  -- 业务提醒：SLA 超期预警（customer-service），5 分钟窗口，合并为一条
  (7101, 'cs.sla-overdue', 'SLA超期预警', 'IN_APP',
   'SLA超期预警: ${customerName}',
   '客户 ${customerName} 工单 ${ticketCode} 已超 SLA，请跟进',
   'ROLE', '{"roles":["客服主管"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '业务提醒样例（notification-strategy.md 业务提醒类）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  -- 异常告警：过账异常（finance ErpFinPostingException 未处置），1 分钟窗口，合并含次数
  (7102, 'fin.posting-exception', '过账异常告警', 'IN_APP',
   '过账异常告警',
   '过账异常: 单据 ${postingNo} 金额 ${amount}，请立即处置',
   'ROLE', '{"roles":["财务员"]}', 60, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '异常告警样例（notification-strategy.md 异常告警类）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  -- 系统通知：信用超额度（sales），不合并，每条独立
  (7103, 'sal.credit-over-limit', '信用超额度通知', 'IN_APP',
   '信用超额度通知: ${customerName}',
   '客户 ${customerName} 信用额度超限 ${overAmount}，订单 ${orderNo} 已挂起',
   'ROLE', '{"roles":["销售员"]}', 0, 'NONE', 'ACTIVE',
   '系统通知样例（notification-strategy.md 系统通知类）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  -- 业务提醒：CRM 活动到期提醒（plan 2026-07-06-0642-1 Phase 2），5 分钟窗口，合并为一条
  (7104, 'crm.event-reminder', 'CRM活动到期提醒', 'IN_APP',
   '活动到期提醒: ${title}',
   '活动 ${title} 即将于 ${dueTime} 到期，负责人 ${ownerUserId}，请跟进',
   'USER_LIST', '{"userIds":["${ownerUserId}"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '业务提醒样例（CRM 活动到期，ownerUserId 模板字段占位；精确路由待角色基础设施）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  -- 业务提醒：CSAT 调查到期提醒（plan 2026-07-06-0642-1 Phase 2），5 分钟窗口，合并为一条
  (7105, 'cs.csat-reminder', 'CSAT调查到期提醒', 'IN_APP',
   'CSAT调查到期提醒: ${ticketCode}',
   '工单 ${ticketCode} 的满意度调查（state=${state}）请跟进',
   'ROLE', '{"roles":["客服员"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '业务提醒样例（CSAT 调查未响应/过期提醒）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
   -- 异常告警：生产差异超阈值（plan 2026-07-06-0642-1 Phase 3），1 分钟窗口，合并含次数
  (7106, 'mfg.production-variance', '生产差异超阈值告警', 'IN_APP',
   '生产差异超阈值告警: ${workOrderCode}',
   '工单 ${workOrderCode}（产品 ${productCode}）${varianceType} 差异金额 ${varianceAmount} 已超阈值 ${threshold}，请核查',
   'ROLE', '{"roles":["生产主管"]}', 60, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '异常告警样例（生产差异超阈值，varianceType/varianceAmount/threshold 字段）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  -- 业务提醒：审批结果通知提单人（plan 2026-07-06-0642-2 Phase 1），5 分钟窗口合并。
  -- 接收人=提单人 createdBy（USER_LIST ${submitterUserId} 从 notify context 插值）。
  (7111, 'wf.pur-payment.result', '付款单审批结果通知', 'IN_APP',
   '付款单 ${docNo} 审批${resultText}',
   '您提交的付款单 ${docNo} 审批${resultText}，审批人 ${approverUserId}',
   'USER_LIST', '{"userIds":["${submitterUserId}"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '审批结果通知（提单人，付款单）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  (7112, 'wf.sal-receipt.result', '收款单审批结果通知', 'IN_APP',
   '收款单 ${docNo} 审批${resultText}',
   '您提交的收款单 ${docNo} 审批${resultText}，审批人 ${approverUserId}',
   'USER_LIST', '{"userIds":["${submitterUserId}"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '审批结果通知（提单人，收款单）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  (7113, 'wf.ast-disposal.result', '资产处置审批结果通知', 'IN_APP',
   '资产处置单 ${docNo} 审批${resultText}',
   '您提交的资产处置单 ${docNo} 审批${resultText}，审批人 ${approverUserId}',
   'USER_LIST', '{"userIds":["${submitterUserId}"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '审批结果通知（提单人，资产处置）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  (7114, 'wf.hr-salary.result', '薪酬审批结果通知', 'IN_APP',
   '薪酬单 ${docNo} 审批${resultText}',
   '您提交的薪酬单 ${docNo} 审批${resultText}，审批人 ${approverUserId}',
   'USER_LIST', '{"userIds":["${submitterUserId}"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '审批结果通知（提单人，薪酬，docNo=year-month）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  -- 业务提醒：任务到达通知候选审批人（plan 2026-07-06-0642-2 Phase 3），5 分钟窗口合并。
  -- 接收人=ROLE（审批人角色）；角色未落地 config-gated 静默跳过。
  (7121, 'wf.pur-payment.task-assigned', '付款单待审批通知', 'IN_APP',
   '付款单 ${docNo} 待您审批',
   '付款单 ${docNo} 到达${stepName}步骤，请及时处理',
   'ROLE', '{"roles":["财务员"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '任务到达通知（候选审批人，付款单）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  (7122, 'wf.sal-receipt.task-assigned', '收款单待审批通知', 'IN_APP',
   '收款单 ${docNo} 待您审批',
   '收款单 ${docNo} 到达${stepName}步骤，请及时处理',
   'ROLE', '{"roles":["经理"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '任务到达通知（候选审批人，收款单）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  (7123, 'wf.ast-disposal.task-assigned', '资产处置待审批通知', 'IN_APP',
   '资产处置单 ${docNo} 待您审批',
   '资产处置单 ${docNo} 到达${stepName}步骤，请及时处理',
   'ROLE', '{"roles":["经理"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '任务到达通知（候选审批人，资产处置）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  (7124, 'wf.hr-salary.task-assigned', '薪酬待审批通知', 'IN_APP',
   '薪酬单 ${docNo} 待您审批',
   '薪酬单 ${docNo} 到达${stepName}步骤，请及时处理',
   'ROLE', '{"roles":["HR专员","财务主管","部门负责人"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '任务到达通知（候选审批人，薪酬多级链）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  -- 业务提醒：抄送通知（plan 2026-07-06-0642-2 Phase 2），5 分钟窗口合并。
  -- 接收人=ROLE（CC 角色）；角色未落地 config-gated 静默跳过。
  (7131, 'wf.pur-payment.cc', '付款单抄送通知', 'IN_APP',
   '付款单 ${docNo} 抄送知会',
   '付款单 ${docNo} 已审批通过，特此抄送知会',
   'ROLE', '{"roles":["财务经理"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '抄送通知（CC 接收人，付款单）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  (7132, 'wf.sal-receipt.cc', '收款单抄送通知', 'IN_APP',
   '收款单 ${docNo} 抄送知会',
   '收款单 ${docNo} 已审批通过，特此抄送知会',
   'ROLE', '{"roles":["销售经理"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '抄送通知（CC 接收人，收款单）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  (7133, 'wf.ast-disposal.cc', '资产处置抄送通知', 'IN_APP',
   '资产处置单 ${docNo} 抄送知会',
   '资产处置单 ${docNo} 已审批通过，特此抄送知会',
   'ROLE', '{"roles":["资产管理员"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '抄送通知（CC 接收人，资产处置）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  (7134, 'wf.hr-salary.cc', '薪酬抄送通知', 'IN_APP',
   '薪酬单 ${docNo} 抄送知会',
   '薪酬单 ${docNo} 已审批通过，特此抄送知会',
   'ROLE', '{"roles":["HR专员"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '抄送通知（CC 接收人，薪酬）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);
