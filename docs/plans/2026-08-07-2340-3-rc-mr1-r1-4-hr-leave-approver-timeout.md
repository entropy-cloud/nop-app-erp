# 2026-08-07-2340-3-rc-mr1-r1-4-hr-leave-approver-timeout RC-R1.4 — hr 休假审批超时自动转派（P1-RC-011，MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: RC-R1.4（MR1 第一批纯预授权：hr 休假审批超时自动转派，P1-RC-011）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.4 行 + `docs/audits/arm-index.md` P1-RC-011 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3.1（RC-R1.4 = 「纯 BizModel + scheduler 接线 + config key」）
> Related: `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`（A4.2.17 运行时影响面：SUBMITTED 与薪酬核算完全解耦，降级证据指导 MR1 优先级）；`docs/design/human-resource/use-cases.md`（L1 UC-HR-02⑦）+ `docs/design/human-resource/state-machine.md`（L2 休假 §4 异常）；`docs/architecture/job-scheduling.md`（全局作业目录权威 + §3.15 HR job 接线范式）；`docs/plans/2026-08-07-1932-1-rc-mr1-r1-20-inv-batch-expiry-interception.md`（job/config/测试范式参照：RC-R1.20 落地先例）
> Audit: required

## Current Baseline

- **finding P1-RC-011（arm-index 行）**：UC-HR-02⑦「审批人超时自动转派」缺失。L1 `use-cases.md:24` 异常段逐字「审批人超时自动转派」；L2 `state-machine.md:51` §适用对象一 休假 §4 异常逐字「审批人长期不处理 | 超时自动转上级或代班人（可配置）」。
- **A4.2.17 运行时确认（`2026-08-07-0530` 报告）**：`ErpHrLeaveRequestBizModel.resolveApproverId:203-206` return null（注释自承「非关键——仅记录审批轨迹」）；全 module-hr grep `timeout|escalat|reassign|autoForward` 零业务命中；调度 census：全仓仅 1 个 hr `.job.yaml`（`erp-hr-contract-expiry.job.yaml`），**无 leave-approver-timeout job**。SUBMITTED 休假悬挂与薪酬核算/余额/考勤缺勤**完全解耦**（`sumUnpaidLeaveDays:316-332` + `sumUsedDays:187-201` + `onLeaveApproved` 均仅读/触发 APPROVED）→ 运行时影响 = SLA/流程效率类。**维持 P1**（Q4=(a) 强制实现义务不撤销），降级证据记录指导 MR1 优先级。
- **审批流实仓**：`ErpHrLeaveRequestSubmitProcessor.submit:14-23` 仅 DRAFT→SUBMITTED（不记录审批人）；`ErpHrLeaveRequestApproveProcessor.approve:20-31` 状态守卫（SUBMITTED/APPROVED）→ 校验余额 → `setApproverId(resolveApproverId(context))`（记录当前操作用户关联员工）→ `onLeaveApproved`。`ErpHrLeaveRequest` ORM 有 `approverId`（APPROVER_ID propId 10）+ `approvedAt` + `status`；无 `submitterId` 专列（`employeeId` 即申请员工）。
- **目标人载体**：`ErpHrEmployee.superiorId`（ORM :293「直接上级」SUPERIOR_ID propId 19）存在 = 直接上级载体（L2「转上级」落地载体）；`ErpHrDepartment.managerId`（ORM :357「部门负责人」MANAGER_ID propId 5）存在 = 兜底载体；**代班人（substitute）无 ORM 载体**（grep 零命中）——L2「或代班人」分支触 ORM 变更须 ask-first，越出第一批边界。
- **job 接线范式（`ErpHrContractExpiryJob` + `erp-hr-contract-expiry.job.yaml`，A4.2.12 运行时确认）**：双层门控（`enabled: "@cfg:nop.job.erp-hr-contract-expiry.enabled|false"` + cronExpr `@cfg:nop.job.erp-hr-contract-expiry.cron-expr|0 0 1 * * ?`）+ bean 内 config 门控（`erp-hr.contract-expiry-cron` 空则跳过）+ `BeanMethodJobInvoker` 反射调 execute() + 单条失败隔离 try/catch + `IErpSysNotificationBiz.notify(event, ctx, new ServiceContextImpl())`。
- **notify USER_LIST 插值范式（A4.2.183 运行时确认）**：模板 `{"userIds":["${superiorUserId}"]}` + context 含 superiorUserId → 接收人动态解析运行时正确（`interpolateConfig:206-221`）。
- **预授权判据**（第一批纯预授权）：纯 BizModel + scheduler 接线 + config key，不触 ORM/会计核心/删除；**无 ask-first checkbox**。roadmap RC-R1.4 行 `todo`，Deps（R1.0 done）已满足。

## Goals

- 新增 config key：`erp-hr.leave-approver-timeout-hours`（超时阈值，默认 72h）+ `erp-hr.leave-approver-timeout-cron`（调度 cron，空=不调度，对齐 contract-expiry 范式）。
- 注册调度作业：`erp-hr-leave-approver-timeout.job.yaml`（enabled 默认 false opt-in + cronExpr 默认每日 01:00）+ Job bean `ErpHrLeaveApproverTimeoutJob`（`module-hr/erp-hr-service/.../job/`，镜像 `ErpHrContractExpiryJob`）。
- Job 逻辑（对齐 L2「超时自动转上级或代班人（可配置）」）：扫描 `status=SUBMITTED` 且 `updateTime < now - timeoutHours` 的休假单 → 解析申请员工直接上级（`ErpHrEmployee.superiorId`）→ 更新 `approverId`（转派记录）+ 派发 notify `hr.leave-approver-timeout`（USER_LIST `${superiorUserId}` 插值，context 含休假单/员工/上级信息）→ 幂等（approverId 已 == 上级则跳过，防重复派发）。
- 兜底裁决：superiorId 为 null → 兜底部门负责人（`ErpHrDepartment.managerId`）或跳过并 LOG.warn（Decision，见 Execution Plan）。
- 新增 dedicated 测试（job 扫描 + 转派 + notify 落库断言 + 幂等 + config 门控）。
- 回填 arm-index P1-RC-011 → `done (RC-R1.4)` + roadmap RC-R1.4 → `done` + owner doc 补注 + `docs/logs/` 日志条目。

## Non-Goals

- **不做代班人（substitute）载体**（ORM 无字段，触 ask-first 越出第一批边界——登记 Deferred But Adjudicated，Successor Required: yes）。
- **不修改 approve 审批权限模型**（approve 仍为既有 per-mutation Processor：任意有权限者可审批，`approverId` 审批时回写当前操作人；本计划只做超时转派的 approverId 预置 + 通知，不改审批入口守卫）。
- **不改 ORM / api.xml / 数据字典**（零结构变更；仅消费既有 superiorId/managerId/approverId 字段）。
- **不做审批超时→自动拒绝/自动通过**（L2 仅要求「转上级或代班人」，无自动终态语义）。
- **不改真相源**（use-cases/state-machine.md 需求契约段；job-scheduling.md 仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/human-resource/use-cases.md`（L1 UC-HR-02⑦）+ `docs/design/human-resource/state-machine.md`（L2 休假 §4 异常）+ `docs/architecture/job-scheduling.md`（全局作业目录权威 + §3.15 HR job 接线范式）+ `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-...md`（A4.2.17 降级证据）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）
- Skill Selection Basis: 实现面 = BizModel/Processor + nop-job 接线 + config + notify（`nop-backend-dev`：protected step、Job bean 形态、跨实体访问经 I*Biz 注入规则——superior/manager 解析优先注入 I*Biz，仅当无法满足时用 daoProvider 并注释原因）+ JUnit 测试（`nop-testing`：JunitAutoTestCase/@NopTestConfig/seed 范式 + notify 断言镜像 `TestErpMfgVarianceAlert`/`TestErpSysNotificationCrossDomain`）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 新增 config keys：`erp-hr.leave-approver-timeout-hours`（默认 72）+ `erp-hr.leave-approver-timeout-cron`（默认空=不调度）——登记于 `ErpHrConstants`，经 `AppConfig.var` 读取（对齐 `ErpHrContractExpiryJob.resolveCronConfig:108-110` 范式）；`nop.job.erp-hr-leave-approver-timeout.enabled` 门控（job.yaml `@cfg:...|false`）。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-hr/erp-hr-service`。

## Execution Plan

### Phase 1 - config + job.yaml + Job bean + IoC 注册

Status: completed
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrConstants.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/job/ErpHrLeaveApproverTimeoutJob.java`（新建，镜像 `ErpHrContractExpiryJob`）；`module-hr/erp-hr-service/src/main/resources/_vfs/erp/hr/beans/app-service.beans.xml`（bean 注册，镜像 `erpHrContractExpiryJob` 注册先例 `:44`）；`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-hr-leave-approver-timeout.job.yaml`（新建）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Fix`
- Prereqs: 无（既有基线）

- [x] `Decision` 转派目标解析链（L2「转上级或代班人（可配置）」落地裁决）：主路径 = 直接上级（`ErpHrEmployee.superiorId`）；superiorId 为 null → 兜底部门负责人（`ErpHrDepartment.managerId`，经 employee.departmentId 关联）；两者均 null → 跳过并 LOG.warn（可观测，不静默）。代班人载体无 ORM 字段 → 不实施（Non-Goals + Deferred）。备选（否决）：仅 notify 不更新 approverId——否决理由：L1/L2 字面「转派」，approverId 是既有唯一转派记录载体（`ErpHrLeaveRequest.approverId`）；**残留风险记录**：`ErpHrLeaveRequestApproveProcessor.approve:27` 审批时 `setApproverId(resolveApproverId(context))` 会覆盖转派记录（且 `resolveApproverId` 现返回 null）——转派的**持久可追溯载体 = notify 落库行**（eventType + recipientUserId），approverId 为过程性转派标记；备选（否决）：跳过兜底——否决理由：上级缺失时部门负责人是既有组织载体，兜底可避免「上级缺失即整单漏转派」。
      - Skill: `nop-backend-dev`
- [x] `Decision` 超时基准时间与幂等守卫：基准 = `updateTime`（submit 更新时点，`CoreMetrics.currentTimestamp()` 对比；ORM 无 submittedAt 专列，updateTime 为 submit updateEntity 自动填充的最佳代理）；幂等 = `approverId != null && approverId == 目标人` 时跳过（首转派后 approverId 已记录 → 二次扫描不重复派发；对齐 hasEscalationAction 幂等守卫思想——不新增 ORM 字段，用既有 approverId 承载转派状态）。备选（否决）：新增 lastEscalationAt 字段——触 ORM ask-first 越界；approverId 承载已足够（转派目标唯一）。
      - Skill: `nop-backend-dev`
- [x] `Add` `ErpHrConstants` 登记 config keys：`CONFIG_LEAVE_APPROVER_TIMEOUT_HOURS = "erp-hr.leave-approver-timeout-hours"`（默认 72）+ `CONFIG_LEAVE_APPROVER_TIMEOUT_CRON = "erp-hr.leave-approver-timeout-cron"`（默认空）+ notify event `NOTIFY_EVENT_LEAVE_APPROVER_TIMEOUT = "hr.leave-approver-timeout"`（对齐 `NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING` 命名）。
      - Skill: `nop-backend-dev`
- [x] `Add` `erp-hr-leave-approver-timeout.job.yaml`：jobName/enabled(`@cfg:nop.job.erp-hr-leave-approver-timeout.enabled|false`)/displayName/description/jobGroup(erp-hr)/cronExpr(`@cfg:nop.job.erp-hr-leave-approver-timeout.cron-expr|0 0 1 * * ?`)/invoker(bean=erpHrLeaveApproverTimeoutJob, method=execute)——镜像 `erp-hr-contract-expiry.job.yaml`（含 jobGroup 字段）。
      - Skill: `nop-backend-dev`
- [x] `Add` **IoC bean 注册**：`app-service.beans.xml` 注册 `erpHrLeaveApproverTimeoutJob` bean（镜像 `erpHrContractExpiryJob` 注册先例 `:44`——job.yaml invoker 的 `bean:` 经 IoC 容器解析，未注册则 `BeanMethodJobInvoker` 运行时失败）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpHrLeaveApproverTimeoutJob`：execute() = cron config 空则跳过（对齐 `ErpHrContractExpiryJob.execute:54-59`）；扫描 `status=SUBMITTED` 且 `updateTime < now - timeoutHours`（`eq("status")` + `lt("updateTime")`，分页 limit 保护）→ 逐条 runTimeoutEscalation：解析 employee → superior/manager 兜底 → 幂等检查 → `setApproverId` + updateEntity + 派发 notify（`hr.leave-approver-timeout`，ctx = leaveCode/leaveType/employeeId/submitterUserId/superiorUserId/superiorId，USER_LIST 模板 `${superiorUserId}` 插值）+ 单条失败隔离 try/catch LOG.warn（对齐 `ErpHrContractExpiryJob.runExpiryWarnings:71-87`）。submitterUserId 解析：employee → 关联 NopAuthUser（userName 约定，经 I*Biz/平台查询——执行时按既有 `TestErpSysNotificationRecipientResolverRuntime` seed 范式确认映射键）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **通知模板前置契约**：派发依赖 ACTIVE `ErpSysNotificationTemplate`（eventType=`hr.leave-approver-timeout`）——无 ACTIVE 模板时 `IErpSysNotificationBiz.notify` config-gated 静默跳过（对齐 `hr.contract-expiry-warning` 先例：不预置模板，运营侧 CRUD 登记后生效；测试侧 seed 模板经 `TestErpSysNotificationRecipientResolverRuntime`/`TestErpMfgVarianceAlert` seed 范式）。记录于 owner doc 补注（部署启用说明）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 超时 SUBMITTED 休假单经 job 扫描后：approverId 更新为上级/部门负责人 + notify 落库（成功模式——测试侧 ACTIVE 模板 seed 后断言；生产侧无 ACTIVE 模板时静默跳过为既有 notify 契约，可观测行为在 owner doc 注记说明）；幂等跳过（approverId==目标人时不重复派发）；cron 空跳过（失败模式：无活跃扫描，行为可观测 LOG.info）
- [x] 无 ORM/契约变更（本阶段产物仅 Java 代码 + job.yaml + beans.xml 注册 + 常量）

### Phase 2 - dedicated 测试

Status: completed
Targets: `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/`（新增 `TestErpHrLeaveApproverTimeoutJob`，镜像 `TestErpHrContractExpiryJob` 若有 + `TestErpSysNotificationRecipientResolverRuntime` 的 seed 范式）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` 测试矩阵：① 超时休假单（seed SUBMITTED + updateTime 早于阈值）→ execute → approverId == 上级 + ErpSysNotification 落库（eventType=hr.leave-approver-timeout + recipientUserId == 上级用户 + status=SENT）；② 未超时（updateTime 近）→ 不动；③ 幂等守卫直测：seed 超时休假单且 **approverId 已 == 目标上级 + updateTime 早于阈值** → execute → 断言跳过分支（通知数不变 + approverId 不变）——**须直接构造守卫前置态**（首扫会经 updateEntity 刷新 updateTime 致超时过滤不再命中，仅靠「二次扫描」测不到守卫分支）；④ superiorId null → 兜底部门负责人（managerId）；⑤ 两者均 null → 跳过 + 不抛（LOG.warn 路径）；⑥ cron config 空 → execute 直接返回不扫描；⑦ job 门控绑定断言：`assignConfigValue("nop.job.erp-hr-leave-approver-timeout.enabled","false")` + 断言 job.yaml 资源 `@cfg:` 绑定生效（nop-job enabled 由调度器消费，bean 层直调 execute 不经调度器——测试以 config 绑定断言替代"不派发"直测，对齐 crm job 测试缺口处理范式）。
      - Skill: `nop-testing`
- [x] `Proof` 断言强度：approverId 更新值 + 通知行 eventType/recipientUserId/status + 幂等计数；`@NopTestConfig` 隔离零外部依赖（镜像 `TestErpInvBatchExpiryInterception` 隔离范式）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 7 组测试全部落地并绿：`mvn test -pl module-hr/erp-hr-service` 全绿（既有 tests 零回归）；`_cases/` 快照录制落盘（对齐 `TestErpHrContractExpiry` 既有快照范式）

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/architecture/job-scheduling.md`（§3.15 HR 作业行补注）；`docs/audits/arm-index.md`（P1-RC-011 修复状态）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.4 done）；`docs/logs/2026/08-08.md`（当日实际日期为 2026-08-08，对齐 RC-R1.20 日志路径修正先例）
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [x] `Add` owner doc 补注：`docs/architecture/job-scheduling.md §3.15` 增 `erp-hr-leave-approver-timeout` 行（config keys + 转派解析链 + 幂等 + notify event + 模板前置契约），镜像 `erp-hr-contract-expiry` 行形态；不修改需求契约段（真相源冻结条款遵守）。
      - Skill: none
- [x] `Add` arm-index P1-RC-011 行「修复状态」→ `done (RC-R1.4)` + 修复落地摘要；roadmap RC-R1.4 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 补注落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_023173060ffeCSXcMWkocnOHf5`，fresh session）——2 MAJOR（M1：owner doc 路径 `docs/design/human-resource/job-scheduling.md` 不存在 → 改 `docs/architecture/job-scheduling.md`[全局作业目录权威，§3.15 HR 行] 全 4 处 + Phase 3 增 §3.15 行；M2：Phase 1 缺 `app-service.beans.xml` IoC 注册——job.yaml invoker bean 不经 IoC 注册则运行时失败）→ 修订：Targets 增 beans.xml + Add 项注册 `erpHrLeaveApproverTimeoutJob`（镜像 `erpHrContractExpiryJob` 注册先例 `:44`）；4 MINOR（superiorId 行号 `:307`→`:293` / approverId 覆盖残留风险[approve:27 审批时覆盖转派记录，持久可追溯载体 = notify 行]记录于 Decision / 通知模板前置契约[无 ACTIVE 模板静默跳过]增 Decision + Exit 说明 / 测试⑦ job.yaml enabled 门控 bean 层不可测 → 改 config 绑定断言）+ 1 trivial（job.yaml 补 jobGroup 字段）全量修订。
- Independent draft review iteration 2: `needs revision`（独立子代理 `ses_0230f6b16ffePOFlQhmxFg2eud`，fresh session）——7 项 round-1 问题全量实仓核验 RESOLVED + 全量复检 PASS（Deps/预授权/范围/反松弛/规则 13/updateTime 代理/notify 插值先例）；2 新 MINOR（A：Phase 2 Exit「`_cases/` 快照落盘（若框架要求）」条件化措辞 → 改无条件「对齐 `TestErpHrContractExpiry` 既有快照范式」；B：测试③幂等「二次扫描」测不到守卫分支[首扫 updateEntity 刷新 updateTime 致超时过滤不命中] → 改为直接构造 approverId==目标人 + 旧 updateTime 前置态直测守卫）→ 修订。
- Independent draft review iteration 3: `accept`（独立子代理 `ses_023097250ffe6KdBwnl29Jc5sh`，fresh session）——2 项 round-2 MINOR 实仓核验 RESOLVED（A：Phase 2 Exit `_cases/` 无条件化 + `TestErpHrContractExpiry/_cases/` 7 组快照先例实仓确认；B：测试③直接构造守卫前置态[approverId==目标人 + 旧 updateTime]，且与 Phase 1 扫描过滤[eq status + lt updateTime → runTimeoutEscalation 守卫]技术自洽——无短路致平凡绿）+ 关键实仓 spot-check 全 PASS（beans.xml:44 / job.yaml:5 jobGroup / orm :293/:357 / roadmap :372 / job-scheduling §3.15）+ 无新增问题（规则 7/13 Fix 类型、预授权无 ORM、单一结果表面、反松弛）。共识达成，转 active。

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-hr/erp-hr-service` + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline——实体访问经 I*Biz/既有 dao 形态，防基线漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 代班人（substitute）载体与转派

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L2「转上级或代班人（可配置）」的「或代班人」分支——ORM 无 substitute 载体（grep 零命中），新增字段触 §5 ask-first 越出第一批纯预授权边界。直接上级 + 部门负责人兜底已覆盖主路径（员工-上级组织链是常态审批链），代班人属个性化配置增强。
- Successor Required: yes（触发条件 = 第二批启动或人工裁决 ORM 授权新增 substitute 字段）

### 审批超时自动拒绝/自动通过

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1/L2 均仅要求「超时自动转派」，无自动终态语义（自动拒绝/通过会破坏休假确认与排班联动语义，且 A4.2.17 已证 SUBMITTED 与薪酬/排班解耦——悬挂无数据破坏）。
- Successor Required: no

## Closure

Status Note: 全部 3 阶段完成 + 全绿验证 + 独立结束审计 PASS。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，ses_0222f6b4affet5aSaO6jifbhV2）
- Evidence: Verdict **PASS**——①计划状态一致性（3/3 阶段 completed + 全 `[x]`）；②实现实仓核验（ErpHrConstants 三键 + ErpHrLeaveApproverTimeoutJob[execute 无参 public/cron 空跳过/dateTimeBetween 超时过滤[注释 XMeta 过滤操作集限制]/superior→manager 兜底/幂等守卫/notify 六键 ctx/逐条隔离/生产零 daoFor] + job.yaml[@cfg 双层门控/jobGroup=erp-hr/invoker bean] + beans.xml 注册 + TestErpAllJobYamlLoading 21）；③文档回填（job-scheduling §3.15 行/arm-index P1-RC-011 done/roadmap RC-R1.4 done/日志首条）；④验证抽查（TestErpHrLeaveApproverTimeoutJob 7 绿 + erp-hr-service 134 绿 + TestErpAllJobYamlLoading 1 绿 + checker R2c=1383/R10=7 等 19 规则 == baseline 零漂移；全量 `mvn clean install -DskipTests` + 全仓 `mvn test` 已于执行期通过）；⑤范围守卫（git status 仅预期文件，零 ORM/api.xml/view.xml/真相源变更）。唯一待办（Closure Gates 勾选 + 证据回填）由执行者在本审计 PASS 后完成。

Follow-up:

- 无（范围内项目全落地后关闭；代班人载体 successor 触发条件见 Deferred But Adjudicated）
