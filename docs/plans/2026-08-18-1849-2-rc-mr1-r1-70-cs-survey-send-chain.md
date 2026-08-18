# 2026-08-18-1849-2-rc-mr1-r1-70-cs-survey-send-chain RC-R1.70 — cs 调查延迟派发与重试（A 类 ORM：ErpCsSurvey 加 status/failureCount 列 + 发送链 job + FAILED 重试）

> Plan Status: active
> Last Reviewed: 2026-08-18
> Mission: requirement-compliance
> Work Item: RC-R1.70（P1-RC-059，UC-CS-08 ①延迟发送调度 + ②渠道派发链接 + 后置 COMPLETED/FAILED 终态 + 异常发送失败标记 FAILED 并重试）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.70 行 + `docs/audits/arm-index.md` P1-RC-059 行（:236）+ 2026-08-12 批量裁决 A 类（roadmap 头 :40：「cs: RC-R1.70（ErpCsSurvey 加 status/failureCount 列）」ORM 修改授权已批量批准，对齐 Q3 纯加性类自动执行，越界回落双独立子 agent 批准；行标签仍携旧「越界项」措辞，done 回写时按 R1.61-67 先例同步改写）
> Related: `docs/design/customer-service/use-cases.md`（L1 UC-CS-08 :146-161）；`docs/design/customer-service/csat.md`（§实现约定「cron 注册归 Non-Goal / status 列归 Non-Goal」AI 自标——三判据不成立已被 arm-index:236 裁决）；`docs/plans/2026-08-15-1605-1-rc-mr1-r1-37-38-logistics-job-wiring-family.md`（R1.37 简单 job bean + dead config 转活跃范式）；`docs/plans/2026-08-17-2125-3-rc-mr1-r1-67-cs-multi-level-escalation.md`（cs 域 job/测试/合规面最新基线）；`docs/plans/2026-08-15-2119-2-rc-mr1-r1-45-fin-cash-flow-classification.md`（dict yaml 手写先例）
> Audit: required

## Current Baseline

- **finding P1-RC-059（arm-index:236，UC-CS-08 ①②+后置+异常）**：L1（`use-cases.md:146-161`）逐字要求：①「工单 → RESOLVED，系统延迟 X 小时（配置项 erp-cs.survey-send-delay）后**创建** ErpCsSurvey」（0.2 清单化语义 = 延迟后发送/生效，调查记录本身在 resolve 时创建）+ ②「系统按客户渠道发送含 surveyToken 的调查链接」+ 后置「调查终态为 COMPLETED 或 FAILED」+ 异常「延迟期间工单重回 IN_PROGRESS → 取消该次调查；发送失败 → 标记 FAILED 并重试」。L3 实仓（HEAD 核查）：
  - **PENDING 调查孤立**：`ErpCsSurveyCreateSurveyProcessor.createSurvey:30-59`——delayHours<=0 → `surveySentAt=now`（立即 SENT）；delayHours>0 → `surveySentAt=null`（PENDING，:47-49）——**无任何 Job/Processor 消费 PENDING 调查**（grep `delayedSend|sendPendingSurvey` 跨 main 零命中）；`ErpCsCsatReminderJob`（`job/ErpCsCsatReminderJob.java:72-97`）仅消费 `findSurveyReminders`/`findExpiredSurveys`（均 `lt(surveySentAt, threshold)` 过滤，`ErpCsSurveyBizModel:96-119`）——**PENDING（surveySentAt=null）调查永不被发送**；
  - **派发链缺失**：`:46` 设 `surveyChannel=PORTAL` 仅描述字段，无 notify/email/链接推送调用（通知基础设施 `IErpSysNotificationBiz` cs 侧五处注入范式就绪）；
  - **FAILED 终态不可持久**：`ErpCsSurvey`（`app-erp-cs.orm.xml:486-519`）字段 propId 1-17（id…updateTime），**无 status 列**（生命周期 PENDING/SENT/COMPLETED 由 surveySentAt/respondedAt 时间戳派生，BizModel javadoc :33-38 自述）+ grep `markSurveyFailed|retrySurvey|FAILED` 业务语义零命中；
  - **异常前半已实现**：`ErpCsTicketReopenProcessor.cancelUnrespondedSurvey:53-64` reopen 时删除未响应调查（覆盖 PENDING/SENT——补显式测试断言）；
  - **submitSurvey**（`ErpCsSurveyBizModel:58-94`）：token 查找 + 已响应守卫 + config-gated 评分范围校验 + `respondedAt` 回写——增写 status=COMPLETED 即可。
  - **dict 既有**：`erp-cs/survey-channel`（orm.xml:72-77：EMAIL/PHONE/PORTAL/CHAT）；无 survey-status dict。
- **config 既有**：`erp-cs.survey-send-delay`（默认 0，`ErpCsConfigs:54`）+ `erp-cs.survey-reminder-hours`（48）+ `erp-cs.survey-expire-days`（7）+ `erp-cs.csat-reminder-cron`（reminder job 运行时门控）；发送链无 cron 键。
- **job 基线**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/` 现存 **26 个 .job.yaml**（另 1 个 scheduler.yaml 非注册文件；cs 三件：sla-scan 每分钟/csat-reminder/entitlement-expiry，均 enabled 默认 false + `@cfg` 双键门控）——TestErpAllJobYamlLoading 断言 **26**。
- **notify seed 模板**：当前最大 ID 7203 → 本计划新模板 **7205**（7204 已分配给同批 Plan 1）。
- **Q4 判据**：§2 P1①（功能完全缺失——延迟发送调度 + 派发链 + FAILED 终态）+ P1②（异常路径未实现）；三判据复核均不成立（csat.md §实现约定 Non-Goal 系 AI 自标无人工批准痕迹，arm-index:236）→ Q4=(a) 强制实现。**2026-08-12 A 类批量裁决**：`ErpCsSurvey` 加 status/failureCount 列 ORM 授权已批量批准（纯加性：可空无默认无索引无 UK，越界回落双独立子 agent 批准）。
- **测试基线**：erp-cs-service **144 @Test 全绿**（R1.67 后）；survey 相关：TestErpCsTicketSlaCsat（:194/:223/:234/:242 四 survey 测试）+ job/TestErpCsCsatReminderJob（4 @Test）。
- **compliance 基线**（§BASELINE 机器可读块）：R2b=235 / R2c=1439 / R2d=35 / R10=11 / R12a=70（设计取向：job 经 `IErpCsSurveyBiz` 注入 + ticket 读取经既有 daoProvider 先例站点（CsatReminderJob:131-144 同型）；预期 R2c 可能 +1 job 内 daoFor(ErpCsTicket) ——同型既有站点若计为新站点则 baseline-raise per-site 证据）。

## Goals

- **UC-CS-08 ①②运行时成立**：PENDING 调查经新发送链 job 到期派发——`status=SENT` + `surveySentAt=now` + notify 派发含 surveyToken 调查链接（cs.survey-invitation 模板 7205，channel 携带，客户 IN_APP 占位语义）。
- **后置终态可持久**：ErpCsSurvey 加 `status`（dict erp-cs/survey-status：PENDING/SENT/COMPLETED/FAILED，propId 18）+ `failureCount`（INTEGER，propId 19）纯加性 2 列（A 类授权）；createSurvey/submitSurvey/发送链显式写 status。
- **异常路径成立**：发送失败 → `status=FAILED` + `failureCount++` → 同 job 后续扫描重试（< `erp-cs.survey-send-retry-max` 默认 3）→ 成功转 SENT；超限终态 FAILED 保留；reopen 取消（既有 cancelUnrespondedSurvey）补显式 PENDING 断言。
- **遗留行兼容**：status null → 派生语义（surveySentAt null=PENDING；respondedAt 非空=COMPLETED；否则 SENT）——既有读路径（findSurveyReminders/findExpiredSurveys surveySentAt 过滤）零改动零回归。
- **测试补强**：新 TestErpCsSurveySendJob 测试组 + 144 基线零回归 + 全量构建 + checker 零漂移（或 baseline-raise per-site 证据）+ TestErpAllJobYamlLoading 独立执行 26→27（同批 Plan 1849-1 串行落地后为 27→28，执行序内自校）。
- **「按客户渠道」语义口径**：L1 ② 在派发链接层面达成——surveyChannel 携带于 notify context + 模板渲染，渠道差异化实际投递归 nop-notification 独立面 successor（owner doc 回填显式注明，防同维度重开）。
- **owner doc 收敛**：csat.md §实现约定 Non-Goal 三条移除/更新（cron 注册/status 列已实现）+ arm-index P1-RC-059 → done + roadmap 行 done + 行标签 A 类改写 + logs 条目。

## Non-Goals

- **不做匿名无鉴权端点**（P2-RC-054 watch-only 独立项，非本行范围）。
- **不做 EMAIL/SMS 实际通道投递**（IN_APP 占位 + nop-notification 独立面 successor 既有范式）。
- **不做调查批量报表/催填策略变更**（reminder/expiry 既有行为不动）。
- **不改 createSurvey 的创建时机**（resolve 即建记录 + delay 控制发送生效，L1 ① 语义按 arm-index:236 裁决口径）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/customer-service/csat.md` + `docs/design/customer-service/use-cases.md`（L1 正文不动）
- Skill Selection Basis: ORM 加列 + 增量重生成（平台文档：`mvn clean install -DskipTests`，勿重跑 nop-cli gen）；job bean/BizModel（`nop-backend-dev`）；测试（`nop-testing`）。

## Infrastructure And Config Prereqs

- 新 config 键（ErpCsConfigs 登记 + csat.md 配置表）：`erp-cs.survey-send-cron`（默认空 = 跳过，运行时门控）+ `erp-cs.survey-send-retry-max`（默认 3）+ `erp-cs.survey-send-batch-limit`（默认 200，单批扫描上限）。
- 新 job.yaml：`erp-cs-survey-send.job.yaml`（enabled 默认 false + cronExpr `@cfg:nop.job.erp-cs-survey-send.cron-expr|0 0/10 * * * ?`）。
- 新 seed 模板：`cs.survey-invitation`（ID **7205**，RECIPIENT_CONFIG = ROLE 客服员转达口径——镜像既有 7105 `cs.csat-reminder` 同款，客户非系统用户经客服员转达 + IN_APP 占位，三方言）。
- ORM 纯加性 2 列 + 新 dict `erp-cs/survey-status`（orm dict 声明 + meta dict yaml 手写，R1.45 先例）——`mvn clean install -DskipTests` 增量重生成；无数据迁移（null=派生兼容）。

## Execution Plan

### Phase 1 - ORM 纯加性 2 列 + dict + 状态写入接线

Status: planned
Targets: `module-cs/model/app-erp-cs.orm.xml`（ErpCsSurvey + survey-status dict）、`module-cs/erp-cs-meta/_vfs/dict/erp-cs/survey-status.dict.yaml`（新）、`ErpCsSurveyCreateSurveyProcessor.java`、`ErpCsSurveyBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无

- [ ] **D1 列设计**：`ErpCsSurvey.status`（propId 18，VARCHAR 20，`ext:dict="erp-cs/survey-status"`，可空无默认无索引无 UK）+ `ErpCsSurvey.failureCount`（propId 19，INTEGER，可空无默认）；dict 四值 PENDING/SENT/COMPLETED/FAILED（yaml 手写 + orm dict 声明，R1.45 posted-status 先例）。
      - Skill: `nop-backend-dev`
- [ ] **D2 遗留兼容裁决**：status null → 派生（surveySentAt null=PENDING / respondedAt 非空=COMPLETED / 否则 SENT）；既有读路径（findSurveyReminders/findExpiredSurveys）**零改动**（surveySentAt 过滤保持）；新写路径显式赋值——createSurvey 写 status（delayHours<=0 → SENT；>0 → PENDING）+ submitSurvey 成功响应后写 status=COMPLETED。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：`mvn clean install -DskipTests` 增量重生成 BUILD SUCCESS + DDL 三方言 NULL 无默认核对 + 既有 TestErpCsTicketSlaCsat 四 survey 测试零回归。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 2 列 + dict 落地且既有 144 基线零回归（分域 mvn test）

### Phase 2 - 发送链 job + FAILED 重试 + reopen 取消断言

Status: planned
Targets: `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsSurveySendJob.java`（新）、`app-erp-all/_vfs/nop/job/conf/erp-cs-survey-send.job.yaml`（新）、`ErpCsConstants.java`、`ErpCsConfigs.java`、`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1

- [ ] **D3 job 形态 = 简单 job bean**（R1.37 D1 选项 A 范式，否决 batch-task REQUIRES_NEW：单入口浅查询无逐条新事务诉求）：`ErpCsSurveySendJob.execute()` 无参 + cron 空值跳过 + limit 批量 + 逐条 try/catch 失败隔离 + `ormTemplate.runInSession` 包裹；扫描条件 = （status=PENDING **或** [status null 且 surveySentAt null 派生 PENDING]）且 `createTime + delayHours <= now`。
      - Skill: `nop-backend-dev`
- [ ] **D4 派发语义**：`notificationBiz.notify("cs.survey-invitation", {surveyId, surveyToken, ticketCode, channel, customerName}, ctx)`（客户非系统用户 → IN_APP 占位 + 实际投递 successor 注记，R1.65/R1.67 Deferred 同款）；**成功判据 = notify 调用无异常**（占位语义下落库即成功）→ `status=SENT` + `surveySentAt=now`；异常 → `status=FAILED` + `failureCount++`。
      - Skill: `nop-backend-dev`
- [ ] **D5 FAILED 重试**：同 job 扫描 `status=FAILED` 且 `failureCount < erp-cs.survey-send-retry-max` → 重试派发（成功转 SENT / 失败 failureCount 再增）；超限 → 终态 FAILED 保留（可查询，L1「标记 FAILED 并重试」达成；L1 无超限管理员通知要求，不加）。
      - Skill: `nop-backend-dev`
- [ ] reopen 取消显式断言：`cancelUnrespondedSurvey` 覆盖 PENDING/FAILED 未响应行删除（既有逻辑核对 + 测试断言，L1 异常前半）。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：新 `job/TestErpCsSurveySendJob`：① cron 空值跳过 ② PENDING 到期 → SENT + surveySentAt + notify 落库 ③ 未到期（delay 窗口内）跳过 ④ 遗留 null 行派生兼容派发 ⑤ notify 抛异常 → FAILED + failureCount=1 ⑥ 重试成功 FAILED→SENT ⑦ 超限终态不再重试 ⑧ submitSurvey 写 COMPLETED ⑨ reopen 删 PENDING/FAILED 未响应行 + `_cases/` 快照；TestErpCsTicketSlaCsat/TestErpCsCsatReminderJob 零回归 + TestErpAllJobYamlLoading 计数 +1。验证命令：`mvn test -pl module-cs/erp-cs-service`。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 发送/失败/重试/取消四路径测试绿 + reminder/expiry 既有行为零回归

### Phase 3 - 验证收口 + 文档回填

Status: planned
Targets: `docs/design/customer-service/csat.md`、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026-08/{当期}.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1-2 全绿

- [ ] 全量验证：`mvn test -pl module-cs/erp-cs-service` 全绿（144 基线 + 新增零回归）+ `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh`（actual ≤ baseline 或 baseline-raise per-site 证据）+ TestErpAllJobYamlLoading。
      - Skill: none
- [ ] owner doc 回填：csat.md §实现约定 Non-Goal 三条修正（cron 注册已接线 / status 列已落 / 实际邮件发送维持 successor）+ 配置表补 3 键 + arm-index P1-RC-059 → done (RC-R1.70) + roadmap 行 done + 行标签 A 类改写 + logs 条目（全绿验证状态）。
      - Skill: none

Exit Criteria:

- [ ] 五处回填一致（代码 / csat.md / arm-index / roadmap / logs）

## Draft Review Record

- Independent draft review iteration 1: needs-revision（task `ses_feb7c53c2ffeh7NF0lSK5gOcHO`，2026-08-18）——MAJOR-1：job.yaml/TestErpAllJobYamlLoading 基线 off-by-one（27→实际 26，scheduler.yaml 误计），Goal 计数链同步错；MINOR：m1「如需」slack 措辞 / m2 手工重发配方在新扫描条件下失效（surveySentAt 置空不再回队）/ m3 seed 接收人口径未预提交 / m4「按客户渠道」语义口径未显式声明。其余维度（基线事实/scope/授权/设计/测试/协调）全 PASS（PENDING 孤立/dict/ORM/reopen/config/seed/compliance 基线逐项实测确认）。
- Independent draft review iteration 2: acceptable（task `ses_feb724fa7ffeM0C1DFCuyqCs6w`，2026-08-18）——四项阻塞全部确认解决（26 基线 + 串行链 / batch-limit 无条件化 / ROLE 客服员预提交镜像 7105 实测一致 / 重发配方修复 / 渠道语义声明）；非阻塞 MINOR 一条（Proof ⑨ 补 FAILED 行断言对称性）已采纳修订。无新问题。

## Closure Gates

- [ ] 范围内行为完成（UC-CS-08 ①②+后置+异常全路径）
- [ ] 相关文档对齐
- [ ] 已运行验证（分域全绿 + 全仓 install + checker）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### EMAIL/SMS 实际通道投递

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: IN_APP 占位 + 实际投递归 nop-notification 独立面（csat.md/R1.65/R1.67 既有范式三处一致）
- Successor Required: `yes`（nop-notification 独立面接入时）

### 超限终态 FAILED 管理员通知

- Classification: `optimization candidate`
- Why Not Blocking Closure: L1 异常条款仅要求「标记 FAILED 并重试」，无超限通知要求；终态 FAILED 可查询可手工重发（CRUD update 将 status 置回 PENDING + failureCount 清零即回队重发）
- Successor Required: `no`

## Closure

Status Note: draft（待独立草案审查）

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计
- Evidence: 待

Follow-up:

- 无（范围内零遗留预期）
