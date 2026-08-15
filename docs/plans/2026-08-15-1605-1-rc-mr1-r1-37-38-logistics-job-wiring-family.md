# 2026-08-15-1605-1-rc-mr1-r1-37-38-logistics-job-wiring-family RC-R1.37 + RC-R1.38 — logistics 调度接线族（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.37（P1-RC-084：DRAFT 24h 升级通知）+ RC-R1.38（P1-RC-085：追踪轮询调度接线）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.37 / RC-R1.38 行 + `docs/audits/arm-index.md` P1-RC-084 / P1-RC-085 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`
> Related: `docs/design/logistics/use-cases.md`（L1 UC-LOG-01 / UC-LOG-03）；`docs/design/logistics/state-machine.md`（§8 TODO 策略）；`docs/design/logistics/carrier-integration.md`（§五 轮询兜底配置表）；`docs/audits/2026-08-07-1410-rc-ma4-a4-2-174-177-logistics-runtime.md`（A4.2.174/175 运行时证据）；R1.4/R1.23/R1.27/R1.34/R1.35 job 接线先例
> Audit: required

## Current Baseline

- **finding P1-RC-084（arm-index 行，UC-LOG-01 补充说明）**：L1（`use-cases.md:15`）逐字「超过 24 小时未确认的 DRAFT 发运单触发升级通知」。L2（`state-machine.md §8 TODO 策略`）「避免『草稿发运单长期滞留』：DRAFT 超过 24 小时产生升级 TODO，通知物流主管」。L3 实仓：grep `escalation|24` erp-log-service/src/main **零业务命中**；**无 config key**（`ErpLogConfigs` 当前键集：gateway-timeout/gateway-max-retries/retry-base-interval/tracking-poll-cron/shipment-settlement-mode/webhook-signature-required/sales-freight-expense-subject，无 draft-escalation 类键；`path2-landed-cost-auto-create` 在 `ErpLogConstants.java:53` 非 ErpLogConfigs）；**无 scheduler/cron/Job bean**（`app-erp-all/_vfs/nop/job/conf/` 当前 24 job.yaml 零 `erp-log-*` job + module-logistics 零 Job 类）；**无 draft-escalation 类 notify 调用**（logistics 域既有 notify 事件 2 个——`log.gateway-dead-letter`（`GatewayDispatcher.java:62` 常量，`deadLetter` 派发 `:392`，R1.16 落地）+ `log.freight-posting-failure`（`AbstractErpLogShipmentDeliveredProcessor.java:43` 常量，派发 `:174`，R1.16 落地），均与 draft 滞留升级无关）。
- **finding P1-RC-085（arm-index 行，UC-LOG-03 补充说明）**：L1（`use-cases.md:39`）「定时轮询间隔可配置（默认 4 小时）」。L3 实仓：config key `erp-log.tracking-poll-cron` 存在（`ErpLogConfigs:15` + `DEFAULT_TRACKING_POLLING_CRON="0 0 */4 * * ?"` `:32`）但**零生产代码消费**（A4.2.175 census：module-logistics 无 job.yaml/scheduler.yaml + app-erp-all job conf 零 erp-log job + 常量仅 javadoc 引用）——dead config；`scanForPolling` 仅 manual @BizMutation（`ErpLogShipmentBizModel:126` → `ErpLogShipmentScanForPollingProcessor.scanForPolling` → `GatewayDispatcher.scanForPolling(context)` `:236` 扫描 DISPATCHED/IN_TRANSIT 运单（单轮 `q.setLimit(100)` `:242`，无分页循环，重复运行追补） + DELIVERED 翻转后逐单 `onDelivered` 失败隔离）——`advanceTracking` 推进仅 webhook/scanForPolling 两入口，无 webhook 承运商 + manual 不调用 ⇒ **DISPATCHED 滞留成立**（A4.2.175 运行时证实：`erp-log.tracking-poll-cron` 死 config + 推进两入口 census + TestErpLogCarrierGatewayIntegration#testPollingAdvancesMultipleShipments PASS 佐证 manual 路径可用）。
- **可镜像范式（同仓先例）**：
  - **简单 job bean 范式（R1.4/R1.34/R1.35）**：`ErpHrLeaveApproverTimeoutJob` / `ErpCtApprovalTimeoutEscalationJob` / `ErpCtContractExpiryJob`——`app.erp.<域>.service.job` 包 job bean（`execute()` 无参 + cron 空值跳过 + 扫描 `dateTimeBetween(updateTime, epoch, cutoff)`（XMeta 过滤算子白名单无 lt/le）+ 逐条失败隔离 WARN + `IErpSysNotificationBiz.notify` 派发）+ `app-erp-all/_vfs/nop/job/conf/erp-<域>-*.job.yaml`（`enabled: '@cfg:nop.job.<name>.enabled|false'` + cronExpr `@cfg:<业务cron键>|默认` + invoker bean/method）+ 域 `app-service.beans.xml` 注册 bean；通知事件常量 `NOTIFY_EVENT_*` 登记域 Constants；无 ACTIVE 模板时 notify 静默跳过。
  - **cron 键单键模式（R1.35 D5 / R1.23 / R1.27）**：job.yaml cronExpr 与 bean 空值跳过**共用同一业务键**（dead config 转活跃），如 `erp-prj.pnl-calc-cron` 由 dead 转双消费点；`@cfg:key|default` 空值回退默认实证。
  - **batch-task 范式（R1.23/R1.27，备选）**：job.yaml invoker `nopBatchTaskRunner.executeAsync` + taskPath `batch.xml` + REQUIRES_NEW helper 逐条隔离——**低基数扫描 + notify 派发不采用**（R1.34 D6 同型裁决：R10 基线漂移代价 vs 收益）。
- **测试基线**：erp-log-service **39 tests**（2026-08-14 M4.57 `2026-08-14-0456-3-erplog-shipment-state-machine-bean.md` 后基线：`TestErpLogShipmentStateMachineMatrix` 12 新增 + 既有 27，39/39 全绿；含 TestErpLogFreightPosting/TestErpLogCarrierGatewayIntegration/TestErpLogShipmentGateway 等）；`TestErpAllJobYamlLoading`（app-erp-all）当前 **24/24**（`assertEquals(24, resources.size())` + `assertEquals(24, configs.size())` 两处硬编码，`2026-08-15-1023-3` R1.36 后 24 job.yaml）——**新增 2 个 job.yaml 后 24→26 须同步**。
- **预授权判据**（第一批纯预授权）：调度接线 + config + notify（job bean + job.yaml + `ErpLogConfigs` 新键 + `IErpSysNotificationBiz`），**不触 ORM 结构/会计过账/数据删除**（P1-RC-083 重复发运 DB UK 与 P1-RC-086 容量预约实体为独立越界行 RC-R1.83 / RC-R1.84，不在本行）；roadmap RC-R1.37 / RC-R1.38 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：新 `ErpLogDraftEscalationJob.java` + `ErpLogTrackingPollJob.java`（`app.erp.log.service.job` 包）+ `app-erp-all/_vfs/nop/job/conf/erp-log-draft-escalation.job.yaml` + `erp-log-tracking-poll.job.yaml`；`ErpLogConfigs.java`（新 config 键）；`ErpLogConstants.java`（notify 事件常量）；`module-logistics/erp-log-service` app-service.beans.xml（job bean 注册）；`module-notify/deploy/sql/{postgresql,oracle,mysql}/_seed_erp-notify.sql`（通知模板行，Decision）；测试类（新增 `TestErpLogDraftEscalationJob` + `TestErpLogTrackingPollJob`）+ `TestErpAllJobYamlLoading` 计数更新；owner doc（state-machine.md / carrier-integration.md 注记）+ arm-index + roadmap + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **RC-R1.37 落地（P1-RC-084，Q4 强制实现）**：`erp-log.draft-escalation-hours` config key（默认 24，对齐 L1 字面）+ `ErpLogDraftEscalationJob` job bean（扫描 DRAFT 且 updateTime 超阈值发运单，逐条 `IErpSysNotificationBiz.notify` 派发 `log.draft-escalation` 事件，逐条失败隔离）+ `erp-log-draft-escalation.job.yaml` 注册（enabled 默认 false + cronExpr 消费 `erp-log.draft-escalation-cron` 门控，空值=跳过语义对齐 R1.4）——「DRAFT 超 24h 未确认触发升级通知」运行时成立。
- **RC-R1.38 落地（P1-RC-085，Q4 强制实现）**：`ErpLogTrackingPollJob` job bean（cron 空值跳过 + 调既有 `IErpLogShipmentBiz.scanForPolling` 全量轮询推进，inherit 既有内部隔离）+ `erp-log-tracking-poll.job.yaml` 注册（cronExpr 消费**既有 dead config** `erp-log.tracking-poll-cron` 默认 `0 0 */4 * * ?`，转活跃对齐 R1.23/R1.27）——「定时轮询间隔可配置（默认 4 小时）」运行时成立，DISPATCHED 滞留由自动机制兜底。
- **config 键登记 + 文档收敛**：`ErpLogConfigs` 新键（draft-escalation-hours / draft-escalation-cron）+ 既有 `tracking-poll-cron` 由 dead 转活跃（job.yaml 双消费点注记）；state-machine.md §8 DRAFT 24h TODO 段 + carrier-integration.md §5.4 轮询兜底段补实现注记（不修改 L1 契约段）。
- **测试**：`TestErpLogDraftEscalationJob`（超时扫描+通知落库+recipient/未超时零动作/cron 空值跳过/单条失败隔离/无模板静默跳过）+ `TestErpLogTrackingPollJob`（扫描推进 DELIVERED+运费过账联动/manual 行为回归/cron 空值跳过/单条失败隔离）+ `TestErpAllJobYamlLoading` 24→26 同步。
- **零回归**：erp-log-service 既有 39 tests 全绿 + 全量构建 + compliance checker 零漂移（job bean 全经 IBiz 注入零新增 daoFor 站点；R1.4 简单 job 无 REQUIRES_NEW，R2c=1399 / R10=9 不变）。
- **回填**：arm-index P1-RC-084 / P1-RC-085 → `done (RC-R1.37)` / `done (RC-R1.38)` + roadmap 行 → done ✅ + owner doc 注记 + `docs/logs/2026/08-15.md` 日志条目。

## Non-Goals

- **不实现 P1-RC-083**（重复发运防护——DB UK 触 ORM，独立越界行 RC-R1.83 须 ask-first）。
- **不实现 P1-RC-086**（配送窗口容量预约——新增实体触 ORM，独立越界行 RC-R1.84 须 ask-first）。
- **不实现 P1-RC-087**（交付状态通知 sales——跨域契约，独立越界行 RC-R1.85 须 ask-first）。
- **不实现 P2-RC-073~078**（async-dispatch 死 config / 追踪异常 3 天标记 / 加密 stub / 连通性测试 / signatureImage-POD / 0 金额凭证守卫[P2-RC-075 触会计 ask-first]——登记不强制，successor）。
- **不触 ORM 结构**（零列/零索引/零 UK；「已通知」标记列触 ORM 移出范围登记 successor）。
- **不改 `scanForPolling` / `GatewayDispatcher` 既有语义**（job 仅接线调用，不重构 per-shipment 轮询）。
- **不注册物流角色**（roles-and-permissions.md:195 物流域角色权限边界未设计，臆造角色种子属投机性工作；主管级路由登记 successor）。
- **不做前端 AMIS 接线**（发运单滞留视图/TODO 展示不在本行）。
- **不改真相源契约段落**（use-cases L1 不动）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权调度接线修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/logistics/use-cases.md`（L1 UC-LOG-01/03）+ `docs/design/logistics/state-machine.md`（§8 TODO 策略）+ `docs/design/logistics/carrier-integration.md`（§5.4 轮询兜底配置表）+ `docs/audits/2026-08-07-1410-rc-ma4-a4-2-174-177-logistics-runtime.md`（A4.2.174/175 运行时证据）
- Skill Selection Basis: 实现面 = job bean + job.yaml + config 门控 + notify 派发（`nop-backend-dev`——镜像 R1.4 `ErpHrLeaveApproverTimeoutJob` 简单 job bean 范式 + R1.35 cron 单键模式 + `processor-extension-pattern.md`）；测试（`nop-testing`：JunitAutoTestCase + job 级测试 + notify 模板 seed 范式——对齐 R1.4/R1.34 测试范式）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新外部服务/环境变量。config key 登记 `ErpLogConfigs`：`erp-log.draft-escalation-hours`（默认 24）/`erp-log.draft-escalation-cron`（job.yaml cronExpr 消费，默认 `0 30 1 * * ?` 每日 01:30，空值=跳过语义对齐 R1.4）；`erp-log.tracking-poll-cron` 既有键由 dead 转活跃（job.yaml cronExpr 消费，默认 `0 0 */4 * * ?`）。
- job.yaml 注册于 `app-erp-all/src/main/resources/_vfs/nop/job/conf/`（`enabled: '@cfg:nop.job.erp-log-<name>.enabled|false'` + cronExpr `@cfg:<业务键>|默认` + invoker bean/method；jobGroup erp-log）。
- notify 依赖：erp-log-service pom 已含 notify-service（`app-service.beans.xml` 既有 `log.gateway-dead-letter` 消费证实 compile 可用）；通知事件常量登记 `ErpLogConstants`。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-logistics/erp-log-service`。

## Execution Plan

### Phase 1 - 决策裁决（job 形态 / notify 接收人 / 模板 seed / 时间基准）

Status: completed
Targets: 本计划范围裁决（无代码）
Item Types: `Decision`
Skill: `nop-backend-dev`

- [x] **D1**: tracking-poll job 形态 = **选项 A 简单 job bean（推荐）**——`ErpLogTrackingPollJob.execute()` 无参 + cron 空值跳过 + 调既有 `IErpLogShipmentBiz.scanForPolling(ctx)` 一次（内部已封装扫描 + per-shipment onDelivered 失败隔离 + gateway 重试，单轮 `q.setLimit(100)` 无分页循环、重复运行追补；TestErpLogCarrierGatewayIntegration#testPollingAdvancesMultipleShipments 已证 manual 路径）；vs 选项 B batch-task + REQUIRES_NEW helper 逐条隔离（R1.23/R1.27 范式）——否决理由：scanForPolling 是全量语义单入口，batch 化需重构 per-shipment 轮询方法（超范围）且引入 R10 基线漂移；外部网关调用超时风险与 manual 入口既有已接受行为一致（watch-only residual 登记）。roadmap 行「nop-batch job.yaml 注册」按意图解释 = job.yaml 注册 + 批量调 scanForPolling（简单 bean 满足）。
  - Skill: `nop-backend-dev`
- [x] **D2**: draft-escalation notify 接收人 = **选项 A USER_LIST `${submitterUserId}`（shipment.createdBy 发货员）**——物流域角色权限边界未设计（roles-and-permissions.md:195），ROLE 路由无落地点；对齐既有 `wf.*.result` USER_LIST 插值范式（context 键 `submitterUserId`，与 `ErpCtApprovalTimeoutEscalationJob` 的 `${escalationUserId}` 键注入范式同构）；「通知物流主管」语义登记 successor（角色基础设施落地后切换 ROLE 模板）。备选 B（ROLE「物流主管」+ auth 角色种子）属投机性角色种子，否决。
  - Skill: `nop-backend-dev`
- [x] **D3**: 通知模板 seed = **选项 A 新增 seed 模板行**（`module-notify/deploy/sql/{postgresql,oracle,mysql}/_seed_erp-notify.sql`，ID 递增段，`log.draft-escalation` + USER_LIST `${submitterUserId}`，对齐既有 71xx 段样式）使通知端到端可验证；备选 B（不 seed，无 ACTIVE 模板静默跳过——R1.34/35 先例）——推荐 A：P1-RC-084 验收标准含「触发升级通知」，seed 模板使运行时可达性可断言。
  - Skill: `nop-backend-dev`
- [x] **D4**: 超时时间基准 = `updateTime`（对齐 R1.4/R1.34 超时语义先例：最近活动时间而非创建时间，防「创建超时但持续编辑」误报）；扫描窗口 = `dateTimeBetween(updateTime, epoch, cutoff)`（XMeta 过滤算子白名单无 lt，对齐 ErpCtApprovalTimeoutEscalationJob:113-117 先例）；SCAN_LIMIT = 200（对齐同先例分页保护）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] D1-D4 裁决记录于本计划（选择 + 备选 + 理由 + 残余风险），Phase 2/3 实现按裁决执行

### Phase 2 - R1.37 draft escalation job 落地

Status: completed
Targets: `module-logistics/erp-log-service`（新 `ErpLogDraftEscalationJob` + `ErpLogConfigs` + `ErpLogConstants` + app-service.beans.xml）+ `app-erp-all/_vfs/nop/job/conf/erp-log-draft-escalation.job.yaml` + `module-notify/deploy/sql/*/_seed_erp-notify.sql`
Item Types: `Add`
Skill: `nop-backend-dev`

- [x] `ErpLogConfigs` 增 `erp-log.draft-escalation-hours`（DEFAULT_DRAFT_ESCALATION_HOURS=24）+ `erp-log.draft-escalation-cron`（DEFAULT_DRAFT_ESCALATION_CRON=`0 30 1 * * ?`）；`ErpLogConstants` 增 `NOTIFY_EVENT_DRAFT_ESCALATION="log.draft-escalation"`
- [x] 新 `ErpLogDraftEscalationJob`（`app.erp.log.service.job` 包）：`execute()` 无参 + cron 空值跳过 + `runDraftEscalation`（`ormTemplate.runInSession` + `IErpLogShipmentBiz.findList` 查 `status=DRAFT` + `dateTimeBetween(updateTime, epoch, cutoff)` + limit 200，逐单 `notifyDraftEscalation`：context {shipmentId, shipmentCode, **submitterUserId**=createdBy, elapsedHours} 经 `notificationBiz.notify` 派发——**键名 submitterUserId 对齐 D2 模板 `${submitterUserId}` 插值**（对齐 `ErpCtApprovalTimeoutEscalationJob` 的 `${escalationUserId}` 键注入范式），逐条 try/catch WARN 隔离）——镜像 ErpCtApprovalTimeoutEscalationJob 结构（含 setter 注入 + javadoc 双键门控说明）
- [x] `erp-log-draft-escalation.job.yaml`（app-erp-all 注册：enabled 默认 false + cronExpr `@cfg:erp-log.draft-escalation-cron|0 30 1 * * ?` + invoker erpLogDraftEscalationJob/execute + jobGroup erp-log + description 双键门控说明）
- [x] app-service.beans.xml 注册 job bean + seed 模板行三方言 SQL（D3 选项 A：`log.draft-escalation` + USER_LIST `${submitterUserId}`，ID 递增段不与既有 71xx 冲突）

Exit Criteria:

- [x] DRAFT 超阈值发运单运行时扫描 + 通知落库（D2 接收人）可经 job 级测试断言（Phase 4 测试）；未超阈值零动作
- [x] 未超阈值 / cron 空值 / 单条失败 / 无模板四路径行为正确

### Phase 3 - R1.38 tracking-poll job 接线

Status: completed
Targets: `module-logistics/erp-log-service`（新 `ErpLogTrackingPollJob` + app-service.beans.xml）+ `app-erp-all/_vfs/nop/job/conf/erp-log-tracking-poll.job.yaml`
Item Types: `Add`
Skill: `nop-backend-dev`

- [x] 新 `ErpLogTrackingPollJob`（`app.erp.log.service.job` 包，D1 选项 A）：`execute()` 无参 + cron 空值跳过（消费 `erp-log.tracking-poll-cron` 既有键，dead config 转活跃）+ 调 `IErpLogShipmentBiz.scanForPolling(ctx)` 一次（`ormTemplate.runInSession` 包裹，对齐 manual 入口 session 语义）+ 成功条数 LOG.info + 顶层 try/catch LOG.error（镜像 ErpLogShipmentScanForPollingProcessor 语义，零业务逻辑改动）
- [x] `erp-log-tracking-poll.job.yaml`（app-erp-all 注册：enabled 默认 false + cronExpr `@cfg:erp-log.tracking-poll-cron|0 0 */4 * * ?` + invoker erpLogTrackingPollJob/execute + jobGroup erp-log + description 消费既有键注记）
- [x] app-service.beans.xml 注册 job bean；`ErpLogConfigs` CONFIG_TRACKING_POLLING_CRON javadoc 增双消费点注记（dead config 转活跃）

Exit Criteria:

- [x] 定时路径调用 scanForPolling 推进 DISPATCHED→DELIVERED + 运费过账联动可经 job 级测试断言（Phase 4）；cron 空值跳过
- [x] 既有 manual `scanForPolling` @BizMutation 行为零回归（Phase 4 回归断言）

### Phase 4 - 测试与验证

Status: completed
Targets: `module-logistics/erp-log-service/src/test` + `app-erp-all/src/test/java/io/nop/job/local/config/TestErpAllJobYamlLoading.java`
Item Types: `Add | Proof`
Skill: `nop-testing`

- [x] 新 `TestErpLogDraftEscalationJob`（Job 级测试，镜像 TestErpCtApprovalTimeoutJob 范式）：① 超阈值扫描 + 通知落库 + recipient==createdBy 断言 ② 未超阈值零动作 ③ cron 空值跳过 ④ 单条失败隔离（异常单不阻断其余）⑤ 无 ACTIVE 模板静默跳过
- [x] 新 `TestErpLogTrackingPollJob`：① 调度路径推进多运单（DISPATCHED→DELIVERED + onDelivered 运费过账联动，镜像 TestErpLogCarrierGatewayIntegration#testPollingAdvancesMultipleShipments 种子结构）② cron 空值跳过 ③ 单条 onDelivered 失败隔离（保持 PENDING 不中断）
- [x] `TestErpAllJobYamlLoading` 24→26 两处硬编码同步；重跑既有 erp-log-service 39 tests（含 TestErpLogShipmentStateMachineMatrix 12）零回归
- [x] Proof: 分域 `mvn test -pl module-logistics/erp-log-service` 全绿 + `mvn test -pl app-erp-all`（JobYamlLoading）+ 全量 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` actual==baseline 零漂移
  - Skill: `nop-testing`

Exit Criteria:

- [x] 两个 job 级测试类全绿 + erp-log-service 既有 39 tests 零回归 + TestErpAllJobYamlLoading 26/26
- [x] 全量构建通过 + checker 零漂移（R2c=1399 / R10=9 不变）

### Phase 5 - 回填

Status: completed
Targets: arm-index + roadmap + owner doc + docs/logs
Item Types: `Follow-up`
Skill: `none`

- [x] arm-index P1-RC-084 → `done (RC-R1.37)`（含修复落地摘要：config 键/job 接线/notify 事件/seed 模板/D2 接收人裁决）+ P1-RC-085 → `done (RC-R1.38)`（含修复落地摘要：dead config 转活跃/简单 job bean 接线/D1 裁决）
- [x] roadmap RC-R1.37 / RC-R1.38 行 → done ✅（含落地摘要）
- [x] owner doc 注记：state-machine.md §8 DRAFT 24h TODO 段补实现注记（config 键 + job + notify 事件 + 接收人裁决 + 主管路由 successor）+ carrier-integration.md §5.4 轮询兜底段补接线实现注记（cron 键转活跃 + job 接线）
- [x] `docs/logs/2026/08-15.md` 顶部追加本计划落地日志条目（格式见 `docs/logs/00-log-writing-guide.md`）

Exit Criteria:

- [x] 回填完成且与 roadmap/arm-index/owner doc/logs 四源一致

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_ffb8abb36ffeonNviMEKRTi6za`) because 3 blocking issues — B1 测试计数陈旧 27→39（M4.57 后实仓 39 tests）/ B2 notify 事件 census 错误（实为 2 个事件 `log.gateway-dead-letter` GatewayDispatcher:62+log.freight-posting-failure AbstractErpLogShipmentDeliveredProcessor:43 +「无 notify 调用」措辞自相矛盾）/ B3 执行项缺 Item Types 标签；N1-N3 非阻塞（context 键 submitterUserId 统一 / path2 常量位置 / scanForPolling setLimit(100) 措辞）
- Independent draft review iteration 2: accept (`ses_ffb814791ffe62abvRJSv6s7n3`) because B1-B3+N1 全部修订核实 + 实仓 sanity 全过（39 tests / 24 job.yaml / setLimit(100) / notify 依赖 / seed ID 无冲突 / 预授权边界 + Closure Gates 完整），无阻塞问题

## Closure Gates

- [x] 范围内行为完成（RC-R1.37 + RC-R1.38 两个结果表面落地）
- [x] 相关文档对齐（owner doc 注记 + arm-index + roadmap + logs）
- [x] 已运行验证（`mvn test -pl module-logistics/erp-log-service` + `mvn test -pl app-erp-all` + 全量 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` 零漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 重复升级通知（DRAFT 持续滞留场景每日重复派发）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 「已通知」标记列触 ORM 结构变更（ask-first），移出本行；重复派发语义与 R1.34 `ErpCtApprovalTimeoutEscalationJob` 超时未处理逐次升级先例一致（滞留期持续提醒直至人工确认，业务可接受）；L1 仅要求「触发升级通知」未要求单次语义
- Successor Required: `yes`（通知去重标记列需求立项后按 ask-first 流程实施）

### 主管级升级路由（D2 接收人）

- Classification: `watch-only residual`
- Why Not Blocking Closure: roles-and-permissions.md:195 物流域角色权限边界未设计，臆造角色种子属投机性工作；USER_LIST `${submitterUserId}` 满足 L1 触发语义
- Successor Required: `yes`（物流角色基础设施落地后切换 ROLE 模板 + 上级链解析，对齐 R1.4/R1.35 上级兜底链范式）

### 外部网关调用长事务风险（D1 选项 A）

- Classification: `watch-only residual`
- Why Not Blocking Closure: scanForPolling 内外部 trackShipment 调用与 manual 入口既有已接受行为一致；job 低频率（默认 4h）+ gateway 超时/重试既有兜底；batch-task REQUIRES_NEW 化属重构超范围
- Successor Required: `no`

## Closure

Status Note: 五 Phase 全部执行完成（Phase 1 裁决 + Phase 2 R1.37 落地 + Phase 3 R1.38 接线 + Phase 4 测试/验证 + Phase 5 回填）；独立结束审计 5 门全 PASS，零阻塞发现；RC-R1.37 + RC-R1.38 两个结果表面落地 + 四源回填一致，可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_ffb571f32ffe6sprzsP6Y379nO（新会话结束审计，执行者未自我审计）
- Evidence: 独立审计 5 门——(1) 计划一致性：5 Phase 全 `completed` + 全执行项/Exit Criteria `[x]`（审计时 Closure Gates 留 `[ ]` 符合 rule 12，闭包补齐后经文本一致性复查）；(2) 代码存在性：`ErpLogDraftEscalationJob`（cron 空值跳过 + runDraftEscalation[runInSession + findList DRAFT + dateTimeBetween(updateTime) + limit 200 + notify context{submitterUserId=createdBy} + 逐条 WARN 隔离]）+ `ErpLogTrackingPollJob`（cron 空值跳过 + runInSession 包裹 scanForPolling 一次）+ `ErpLogConfigs` 2 新键 + `ErpLogConstants.NOTIFY_EVENT_DRAFT_ESCALATION` + 2 job.yaml（enabled 默认 false + cronExpr 消费业务键）+ beans.xml 2 bean 注册 + seed 模板 ID 7201 三方言 + 2 测试类（6+3 tests）+ `TestErpAllJobYamlLoading` 26/26 逐文件核验 PASS；(3) 反模式：两 job 类零 `@Inject private`/零 `System.currentTimeMillis`/零 `new ErpLogShipment`/零 daoFor（全 IBiz 注入）+ `git status module-logistics/model` 空（零 ORM 变更）PASS；(4) 文档回填：arm-index P1-RC-084/085 `done (RC-R1.37/38)` + roadmap RC-R1.37/38 `done ✅` + state-machine.md §8/carrier-integration.md §5.4 实现注记 + `docs/logs/2026/08-15.md` 首条 PASS；(5) 验证复跑：`mvn test -pl module-logistics/erp-log-service` **48/48**（39 基线 + 9 新增）+ `mvn test -pl app-erp-all -Dtest=TestErpAllJobYamlLoading` 26 job.yaml PASS + compliance checker R2c=1399/R10=9/R12a=69/R12b=66/R12c=40 与 baseline 精确一致零漂移 + 全量 `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）；全 reactor `mvn test` 3 项预存在失败（TestAuthSeedLoadingProof NPE + mfg materialBand cell-not-prop 双页面测试）经 known-good-baselines.md 核实与本次变更无关。审计结论：实现/测试/文档/验证全 PASS，零阻塞发现。

Follow-up:

- 重复升级通知（DRAFT 持续滞留每日重复派发）：watch-only residual，successor = 通知去重标记列需求立项（触 ORM ask-first）
- 主管级升级路由（D2 接收人）：watch-only residual，successor = 物流角色基础设施落地后切换 ROLE 模板 + 上级链解析（对齐 R1.4/R1.35 上级兜底链范式）
