# 2026-08-18-1849-3-rc-mr1-r1-71-cs-catalog-fulfillment-engine RC-R1.71 — cs 目录履行引擎（A 类 ORM：新增 ErpCsTicketFulfillmentStep 实体 + actionConfig 驱动五动作实化 + 失败暂停/重试 3 次/终态推进）

> Plan Status: active
> Last Reviewed: 2026-08-18
> Mission: requirement-compliance
> Work Item: RC-R1.71（P1-RC-061，UC-CS-12 ②actionConfig 驱动执行 + ③失败暂停记录通知 + ④最终状态更新 + 异常重试最多 3 次 + CREATE_CHILD_TICKET 实化）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.71 行 + `docs/audits/arm-index.md` P1-RC-061 行（:246）+ 2026-08-12 批量裁决 A 类（roadmap 头 :40：「cs: RC-R1.71（新增 ErpCsTicketFulfillmentStep 实体 或 ticket 加执行状态列）」ORM 修改授权已批量批准，对齐 Q3 纯加性类自动执行，越界回落双独立子 agent 批准；行标签仍携旧「越界项」措辞，done 回写时按 R1.61-67 先例同步改写）
> Related: `docs/design/customer-service/use-cases.md`（L1 UC-CS-12 :227-243）；`docs/design/customer-service/service-catalog.md`（§9.1 :「多步履行编排为产品基线外」AI 自标——三判据不成立已被 arm-index:246 裁决）；`docs/plans/2026-08-17-2125-1-rc-mr1-r1-65-cs-ticket-create-enrichment.md`（TicketAssignResolver 分配算法复用 + cs→crm dao 先例）；`docs/plans/2026-08-15-0320-3-rc-mr1-r1-31-mnt-report-additional-fault.md`（跨域 IBiz.save data map 建单先例）；`docs/plans/2026-08-15-1605-1-rc-mr1-r1-37-38-logistics-job-wiring-family.md`（R1.37 job 范式）；`docs/plans/2026-08-17-2125-2-rc-mr1-r1-66-cs-timer-session.md`（A 类新实体 + UK + 审批链轻量范式）
> Audit: required

## Current Baseline

- **finding P1-RC-061（arm-index:246，UC-CS-12 ②③④+异常）**：L1（`use-cases.md:227-243`）逐字要求：① 按 `ErpCsCatalogFulfillment.sequence` 升序依次执行 ✅（既有）+ ②「每个 actionType 根据 actionConfig 执行对应动作：ASSIGN_TEAM→按 mode 策略分配团队 / REQUEST_APPROVAL→发起审批链超时自动审批 / CREATE_CHILD_TICKET→创建子工单 / NOTIFY_CUSTOMER→发送通知 / UPDATE_STATUS→更新工单状态」+ ③「某一步失败 → 暂停流程，记录错误信息，通知管理员」+ ④「全部步骤完成 → 工单进入 IN_PROGRESS（或按配置 RESOLVED）」+ 后置「履行流程状态可跟踪，异常可重试」+ 异常「actionType 执行失败 → 支持重试（最多 3 次），超出后通知管理员人工介入」。L3 实仓（HEAD 核查）：
  - `ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor`（119 行）：`executeFulfillmentSteps:35-51` sequence 升序加载执行；**`executeStep:57-94` 全分支审计占位**——从不读 `step.getActionConfig()`（`ErpCsCatalogFulfillment.actionConfig` orm.xml propId 7 domain=json 列存在但零消费）：ASSIGN_TEAM/ASSIGN_AGENT :67-72「占位登记」/ REQUEST_APPROVAL :79-81「审批请求已登记」/ NOTIFY_CUSTOMER :73-75 无 notify 调用 / UPDATE_STATUS :76-78 不触 ticket.status / CREATE_CHILD_TICKET + INVOKE_WORKFLOW :85-88 SKIPPED（AI 自标 successor，arm-index:246 裁决并入本 finding）；`writeAudit:96-106` fromStatus/toStatus 恒 null；
  - **执行状态载体缺失**：`ErpCsCatalogFulfillment`（orm.xml:752-787，propId 1-16）无 status/retryCount 列；无 per-ticket 执行行实体（ErpCsTicketFulfillmentStep 不存在）——「状态可跟踪，异常可重试」后置结构性不可实现；
  - **失败永不暂停**：循环无中断/无 try-catch（:44-49），executeStep 恒返回 DONE/SKIPPED；grep `retryCount|pauseFlow|notifyAdmin` 零命中；
  - **上游降级点**：`ErpCsServiceCatalogItemCreateFromCatalogProcessor.createFromCatalog:60-75` catch `LOG.warn("fulfillment-execute-failed (建单已成功，降级)")` :65-73——履行异常被吞为降级（P2-RC-055 告警缺失为独立 watch-only，非本行义务）；
  - **可复用分配算法**：R1.65 `TicketAssignResolver`（SLA team → ErpCsTeam → crm 同码团队 → 成员池 + ROUND_ROBIN/LEAST_OPEN 纯函数 + config `erp-cs.assign-method`）；**可复用建单范式**：R1.31 `requestBiz.save(data map)`；**notify 注入范式**五处就绪；**cs→crm dao 依赖**已就绪（R1.65）；
  - **状态机实况（终态推进设计输入）**：`ErpCsTicketStateMachine`（注册 bean，app-service.beans.xml:35）迁移矩阵边 = assign(NEW→ASSIGNED)/start(ASSIGNED→IN_PROGRESS)/resolve(IN_PROGRESS→RESOLVED)/close/reopen/cancel——**无 NEW→IN_PROGRESS 直达边**；R1.65 assign 范式 = `setStatus(assignTargetStatus())`（ErpCsTicketBizModel:170 同款复合推进先例）；
  - **dict 既有**：`erp-cs/fulfillment-action-type` 9 值（orm.xml:99-109：CREATE_TICKET/ASSIGN_TEAM/ASSIGN_AGENT/REQUEST_APPROVAL/NOTIFY_CUSTOMER/UPDATE_STATUS/CREATE_CHILD_TICKET/INVOKE_WORKFLOW/CLOSE_TICKET）；L1 ② 仅枚举 5 值（ASSIGN_TEAM/REQUEST_APPROVAL/CREATE_CHILD_TICKET/NOTIFY_CUSTOMER/UPDATE_STATUS）——INVOKE_WORKWORK/CLOSE_TICKET/ASSIGN_AGENT/CREATE_TICKET 非 L1 义务面；
  - **`ErpCsServiceCatalogItem`**（orm.xml:693-751）有 `fulfillmentProcessId`（VARCHAR 100）关联履行流程 + `requestFormConfig`（json）——无终态配置列。
- **Q4 判据**：§2 P1①（actionConfig 驱动/占位无副作用/失败暂停/重试/终态全缺）+ P1②+P1⑤（弱断言 `testFulfillmentCreateTicketStepRegistered:306` 仅审计 anyMatch——须随实化改造）；三判据均不成立（service-catalog.md §9.1 AI 自标无人工批准痕迹，arm-index:246）→ Q4=(a) 强制实现。**2026-08-12 A 类批量裁决**：新增 `ErpCsTicketFulfillmentStep` 实体 ORM 授权已批量批准（纯加性新表 + 自有 UK，越界回落双独立子 agent 批准，R1.66 同型先例）。
- **测试基线**：erp-cs-service **144 @Test 全绿**（R1.67 后）；TestErpCsServiceCatalog **15 @Test**（含 fulfillment 1 弱断言）。`app-erp-all/src/main/resources/_vfs/nop/job/conf/` 现存 **26 个 .job.yaml**（TestErpAllJobYamlLoading 断言 26）→ 本计划 +1（同批 Plan 1849-1/-2 串行落地后链为 26→27→28→29，执行序内自校）。
- **notify seed 模板**：当前最大 7203 → 本计划新 **7206**（cs.fulfillment-step-failed）+ **7207**（cs.fulfillment-notify-customer）（7204/7205 已分配同批 Plan 1/2）。
- **compliance 基线**（§BASELINE 机器可读块）：R2b=235 / R2c=1439 / R2d=35 / R10=11 / R12a=70（设计取向：新 Processor/BizModel 全经 IBiz 注入 + 子工单经 IErpCsTicketBiz.save，预期 R2c 可能 +N per-mutation Processor daoFor 同型站点 baseline-raise per-site 证据，R1.33/51/56 先例）。

## Goals

- **UC-CS-12 ②五动作实化（actionConfig 驱动）**：JSON config 解析 + ASSIGN_TEAM/ASSIGN_AGENT（mode→TicketAssignResolver 真实分配）/ REQUEST_APPROVAL（轻量审批 + 超时自动审批）/ CREATE_CHILD_TICKET（真实子工单 + 双向弱指针）/ NOTIFY_CUSTOMER（notify 派发）/ UPDATE_STATUS（状态机守卫真实迁移）。
- **③失败暂停**：步骤失败 → status=FAILED + lastError + 中断后续（保持 PENDING）+ 管理员通知（cs.fulfillment-step-failed 模板 7206 ROLE 客服主管）。
- **④终态推进**：全部 DONE → 工单 IN_PROGRESS（状态机守卫）；「或按配置 RESOLVED」经尾部 UPDATE_STATUS 步骤 actionConfig.status=RESOLVED 组合达成（零新配置列）。
- **后置 + 异常**：新实体 `ErpCsTicketFulfillmentStep`（per-ticket per-step 执行行：status/retryCount/lastError + UK(ticketId, fulfillmentId) 幂等物化）承载「状态可跟踪，异常可重试」；`retryFulfillment` mutation（手动恢复）+ 自动重试 job（FAILED retryCount<max 重试 + REQUEST_APPROVAL 超时自动审批 + 链恢复推进）；超限通知管理员人工介入。
- **测试补强**：新 TestErpCsCatalogFulfillmentEngine 测试组 + 弱断言测试改造 + 144 基线零回归 + 全量构建 + checker 零漂移（或 baseline-raise per-site 证据）。
- **owner doc 收敛**：service-catalog.md §9.1「产品基线外」声明更新为已实现注记 + arm-index P1-RC-061 → done + roadmap 行 done + 行标签 A 类改写 + logs 条目。

## Non-Goals

- **不集成 nop-workflow 工作流引擎**（跨模块编排超 A 类授权面 + cs 域无工作流契约；REQUEST_APPROVAL 用 cs-local 轻量审批——见 D4；nop-workflow 集成 successor 注记）。
- **不实化 INVOKE_WORKFLOW / CLOSE_TICKET / ASSIGN_AGENT 独立语义**（L1 ② 未枚举：INVOKE_WORKFLOW 维持 SKIPPED + 注记；CLOSE_TICKET 维持审计 DONE 占位 + 注记；ASSIGN_AGENT 并入 ASSIGN 分支按 config 处理）。
- **不做 fulfillmentProcessId 级终态配置列**（「按配置 RESOLVED」经尾部 UPDATE_STATUS 组合达成；目录级配置列 successor）。
- **不做子工单 ORM 亲子关联列**（A 类授权面 = FulfillmentStep 实体；亲子双向弱指针 = 子单 remark + 父单 TicketAction content）。
- **不做 P2-RC-055 createFromCatalog catch 告警**（独立 watch-only 项，失败暂停后主链告警已由本计划 ③ 覆盖步骤级）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/customer-service/service-catalog.md` + `docs/design/customer-service/use-cases.md`（L1 正文不动）
- Skill Selection Basis: ORM 新实体（增量重生成）；Processor/BizModel/job（`nop-backend-dev`）；测试（`nop-testing`）；AMIS 履行进度展示最小接线（`nop-frontend-dev`）。

## Infrastructure And Config Prereqs

- 新 config 键（ErpCsConfigs 登记 + service-catalog.md 配置表）：`erp-cs.fulfillment-retry-cron`（默认空 = 跳过）+ `erp-cs.fulfillment-retry-max`（默认 3）+ `erp-cs.fulfillment-approval-timeout-hours`（默认 24，REQUEST_APPROVAL 超时兜底，actionConfig 可覆盖）。
- 新 job.yaml：`erp-cs-fulfillment-retry.job.yaml`（enabled 默认 false + cronExpr `@cfg:nop.job.erp-cs-fulfillment-retry.cron-expr|0 0/5 * * * ?`）。
- 新 seed 模板：`cs.fulfillment-step-failed`（ID **7206**，ROLE 客服主管）+ `cs.fulfillment-notify-customer`（ID **7207**，客户占位语境）三方言。
- ORM 纯加性新实体 `ErpCsTicketFulfillmentStep` + 新 dict `erp-cs/fulfillment-step-status`（PENDING/IN_PROGRESS/DONE/SKIPPED/FAILED）——`mvn clean install -DskipTests` 增量重生成；无数据迁移。

## Execution Plan

### Phase 1 - ORM 纯加性新实体 + 物化与执行引擎重构

Status: planned
Targets: `module-cs/model/app-erp-cs.orm.xml`、`module-cs/erp-cs-meta/_vfs/dict/erp-cs/fulfillment-step-status.dict.yaml`（新）、`module-cs/erp-cs-service/.../processor/ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java`（重构）、`ErpCsCatalogFulfillmentBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无（建议在 Plan 1/2 之后执行以共享 dict 追加/job 注册测试基线，非硬依赖）

- [ ] **D1 执行状态载体 = 新实体（A 类授权选项 A）**：`ErpCsTicketFulfillmentStep`（表 erp_cs_ticket_fulfillment_step）字段：ticketId（mandatory）/fulfillmentId（mandatory，to-one ErpCsCatalogFulfillment）/catalogItemId/sequence/actionType（dict 复用 erp-cs/fulfillment-action-type）/actionConfig（json 快照）/status（dict erp-cs/fulfillment-step-status）/retryCount（INTEGER）/lastError（VARCHAR 500）/executedAt/executedBy/remark + 审计列 + **UK(ticketId, fulfillmentId)**（幂等物化，R1.66 UK 范式）。否决 ticket 加执行状态列（单列无法承载多步骤状态/重试/错误跟踪，后置条件「状态可跟踪，异常可重试」结构性不可达）。
      - Skill: `nop-backend-dev`
- [ ] **D2 物化与推进**：`executeFulfillmentSteps` 重构——物化（per ticket+fulfillment 存在即复用，actionConfig 取模板快照写入）→ sequence 升序执行：DONE/SKIPPED 跳过；成功 DONE + executedAt/executedBy；失败 FAILED + lastError + **中断**（后续保持 PENDING）+ notify 管理员（7206）；REQUEST_APPROVAL 特例 IN_PROGRESS（等审批/超时）。CREATE_TICKET 分支保留 DONE 审计语义（主单已建）。
      - Skill: `nop-backend-dev`
- [ ] **D3 actionConfig 解析**（JsonTool，容错：非法 JSON/空 config → 按 actionType 缺省策略执行或显式 FAILED——执行期定稿并测试）：五动作实化——
  - **ASSIGN_TEAM/ASSIGN_AGENT**：config `{mode: ROUND_ROBIN|LEAST_OPEN}`（缺省 config `erp-cs.assign-method`）→ 复用 R1.65 `TicketAssignResolver`（成员池 + 纯函数算法）+ `assignToRole` 回退（无成员池时 ROLE 告警路径）；成功 → **`setStatus(ASSIGNED)` 状态迁移仅当 ticket 为 NEW（R1.65 `autoAssignOnCreate:153` NEW-guard 同款守卫；非 NEW 幂等跳过迁移仅更新 assignedToId + 审计，防止末步 ASSIGN 与 ensureInProgress 铺底后守卫互抛）** + `ticket.assignedToId` + ASSIGN 审计（真实分配）；
  - **REQUEST_APPROVAL**：config `{approverRole?, timeoutHours?}` → **cs-local 轻量审批**（否决 nop-workflow：跨模块编排超授权面 + cs 无工作流契约，successor 注记）：step IN_PROGRESS + notify 审批人（ROLE approverRole 缺省客服主管）+ 新 `@BizMutation approveFulfillmentStep(stepId, approved, comment)`（IN_PROGRESS 守卫；approved=true → DONE + 审计；**approved=false → step FAILED + retryCount 置为 max（人工决定终局语义：阻断自动重试链）+ lastError=「审批驳回: {comment}」**——替代方案「新 REJECTED dict 值」否决 = 避免重试/查询双状态语义分叉）；超时自动审批 = retry job 扫描 IN_PROGRESS + REQUEST_APPROVAL + `now - executedAt > timeoutHours` → 自动 DONE + 审计「超时自动审批」；
  - **CREATE_CHILD_TICKET**：经 `IErpCsTicketBiz.save(data map)`（R1.31 先例）——subject=`[子工单] {父subject}`、同 customerId/ticketTypeId、remark 承载 `parentTicketCode={code}`、code 走 R1.65 TK codeRule；父单写 TicketAction（content=`子工单已创建: {childCode}`）；**双向弱指针无 ORM 亲子列**（Non-Goal 声明）；
  - **NOTIFY_CUSTOMER**：`notificationBiz.notify("cs.fulfillment-notify-customer", {ticketCode, catalogItemName, stepRemark}, ctx)`（客户 IN_APP 占位语义既有范式）；
  - **UPDATE_STATUS**：config `{status}`（必填，缺省/非法值 → step FAILED + lastError 配置错误）→ **target == 当前 status 时幂等 DONE no-op**（防尾部 RESOLVED 重试/铺底后同态迁移被严格守卫误判失败）→ 经 `ErpCsTicketStateMachine` 合法迁移守卫真实 setStatus（非法迁移 → FAILED）。
      - Skill: `nop-backend-dev`
- [ ] **D4 终态推进（可达性定稿）**：迁移助手 `ensureInProgress(ticket)`——NEW（无 assignedToId）→ 自动指派当前操作员后经 assign 边 → ASSIGNED → start 边 → IN_PROGRESS；ASSIGNED → start → IN_PROGRESS；≥IN_PROGRESS → 幂等跳过。**调用时机 = 链推进执行最后一个步骤之前**（含单步链）：① 常规链（末步非状态类）——末步执行前已 IN_PROGRESS，全 DONE 后不再二次推进（L1 ④「进入 IN_PROGRESS」达成）；② 「按配置 RESOLVED」组合 = 尾部 UPDATE_STATUS(status=RESOLVED) 步骤——末步执行前 ensureInProgress 铺底 IN_PROGRESS，UPDATE_STATUS 经 resolve 边（IN_PROGRESS→RESOLVED）达成（零新配置列，owner doc 编排说明）。替代方案否决：扩矩阵 NEW→IN_PROGRESS 直达边（绕过分配纪律）；末步后统一推进（尾部 RESOLVED 组合将非法迁移自毁）；skip+管理员注记（违 L1 ④）。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：新 `TestErpCsCatalogFulfillmentEngine`：① 物化幂等（重复 executeFulfillmentSteps 复用行不重复）② ASSIGN_TEAM RR 真实分配（assignedToId + NEW→ASSIGNED 迁移 + ASSIGN 审计）③ REQUEST_APPROVAL → IN_PROGRESS + notify 审批人 → approve(true) DONE ④ 审批驳回 → FAILED + retryCount=max + lastError 含驳回意见 ⑤ NOTIFY_CUSTOMER notify 落库 ⑥ UPDATE_STATUS 合法迁移 + 非法迁移 FAILED ⑦ CREATE_CHILD_TICKET 子单创建 + 双向弱指针 ⑧ 失败中断（step2 失败 → step3 保持 PENDING）+ 管理员通知落库 ⑨ 全 DONE → IN_PROGRESS（含无 ASSIGN 步骤链：ensureInProgress NEW 自动指派路径）⑩ 尾部 UPDATE_STATUS RESOLVED 组合（末步前铺底 IN_PROGRESS → resolve 边 RESOLVED）。验证命令：`mvn test -pl module-cs/erp-cs-service`。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 范围内五动作实化 + 失败暂停 + 终态推进测试绿；既有 15 catalog 测试（含弱断言改造）零回归

### Phase 2 - 重试链（手动 mutation + 自动 job）+ 超时自动审批 + 查询

Status: planned
Targets: `module-cs/erp-cs-service/.../entity/ErpCsCatalogFulfillmentBizModel.java`（retryFulfillment/approveFulfillmentStep/findFulfillmentProgress）、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsFulfillmentRetryJob.java`（新）、`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-cs-fulfillment-retry.job.yaml`（新）、`ErpCsConstants.java`/`ErpCsConfigs.java`、seed 三方言、AMIS 履行进度最小接线
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1

- [ ] **D5 重试双入口**：`@BizMutation retryFulfillment(ticketId, ctx)`（手动：**仅针对 FAILED 步骤**——IN_PROGRESS 待审批步骤不重执行；FAILED 步骤 retryCount+1 后**刷新读取模板 actionConfig**（修正配置即生效；快照列保留最后执行配置作审计）再重执行，`retryCount >= erp-cs.fulfillment-retry-max`（默认 3）拒绝 + notify 管理员人工介入）+ `ErpCsFulfillmentRetryJob`（R1.37 简单 job 范式：cron 空值跳过 + limit + 逐条隔离）自动扫描 FAILED retryCount<max 重试（同样刷新模板 actionConfig）+ REQUEST_APPROVAL 超时自动审批 + 失败链恢复推进（ticket 有 FAILED 步骤且未超限 → 续执行）。超限 → 终态保留 + 管理员通知（L1「超出后通知管理员人工介入」）；审批驳回步骤（retryCount 已置 max）天然排除于自动重试。
      - Skill: `nop-backend-dev`
- [ ] **D6 状态可跟踪查询**：`@BizQuery List<Map> findFulfillmentProgress(ticketId, ctx)` 投影 step 行（sequence/actionType/status/retryCount/lastError/executedAt/executedBy）+ AMIS 工单详情履行进度最小展示（nop-frontend-dev 最小接线，对齐 R1.44 D4 范式）。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：Phase 2 测试组：⑪ 手动重试成功恢复（FAILED→刷新配置重执行→DONE→链推进；IN_PROGRESS 待审批步骤不被重执行断言）⑫ 重试计数达 3 → 拒绝 + 管理员通知 ⑬ job 自动重试 + 超时自动审批（timeoutHours 边界）+ 驳回步骤不被自动重试 ⑭ cron 空值跳过 ⑮ findFulfillmentProgress 投影 ⑯ GraphQL RPC 冒烟（retryFulfillment/approveFulfillmentStep）+ `_cases/` 快照 + TestErpAllJobYamlLoading 计数 +1。验证命令：`mvn test -pl module-cs/erp-cs-service`。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 重试/超限/超时/查询四路径测试绿

### Phase 3 - 验证收口 + 文档回填

Status: planned
Targets: `docs/design/customer-service/service-catalog.md`、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026-08/{当期}.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1-2 全绿

- [ ] 全量验证：`mvn test -pl module-cs/erp-cs-service` 全绿（144 基线 + 新增零回归）+ `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh`（actual ≤ baseline 或 baseline-raise per-site 证据）+ TestErpAllJobYamlLoading。
      - Skill: none
- [ ] owner doc 回填：service-catalog.md §9.1「产品基线外」→ 已实现注记（FulfillmentStep 载体 + D1-D6 裁决 + actionConfig 契约表 + 编排说明[末步前 ensureInProgress 铺底 + 尾部 UPDATE_STATUS 组合 RESOLVED] + INVOKE_WORKFLOW/CLOSE_TICKET 边界声明）+ 配置表补 3 键 + **arm-index P1-RC-061 → done (RC-R1.71) 行显式登记 INVOKE_WORKFLOW 残留为 L1 未枚举边界**（该值曾被 AI 自标 successor 并入 finding，done 注记须留痕防重开）+ roadmap 行 done + 行标签 A 类改写 + logs 条目（全绿验证状态）。
      - Skill: none

Exit Criteria:

- [ ] 五处回填一致（代码 / service-catalog.md / arm-index / roadmap / logs）

## Draft Review Record

- Independent draft review iteration 1: needs-revision（task `ses_feb7c2b28ffejkA2Zac4T5kHSH`，2026-08-18）——MAJOR：E1 D4 终态推进从 NEW 不可达（矩阵无 NEW→IN_PROGRESS 边，无 ASSIGN 步骤链守卫必抛）+ E2 尾部 UPDATE_STATUS(RESOLVED) 时序自毁（末步先于终态推进执行 → 非法迁移必失败）；MINOR：E3 审批驳回被自动重试链重复打扰 / E4 retryFulfillment 会重执行 IN_PROGRESS 待审批步骤 / E5 重试 actionConfig 快照陈旧未裁决 / B1 TestErpCsServiceCatalog 18→15 / B2 job conf 路径漂移 / arm-index 回写须显式登记 INVOKE_WORKFLOW 残留边界。授权面（A 类实体 + cs-local 审批合法性）与「按配置 RESOLVED 经 actionConfig 组合」解读均获确认 PASS。
- Independent draft review iteration 2: acceptable（task `ses_feb723831ffeWmqMouV2OxTGNj`，2026-08-18）——E1-E5/B1/B2/arm-index 全部确认解决（状态机矩阵实仓交叉核对通过，ensureInProgress 单步链/尾部 RESOLVED/中途 IN_PROGRESS 组合/已 RESOLVED 场景推演成立）；非阻塞 MINOR 三条已采纳修订（Phase 1 Exit 18→15、UPDATE_STATUS 同态幂等 DONE no-op 条款、ASSIGN_TEAM NEW-guard 显式化）。无新问题。

## Closure Gates

- [ ] 范围内行为完成（UC-CS-12 ②③④+后置+异常全路径）
- [ ] 相关文档对齐
- [ ] 已运行验证（分域全绿 + 全仓 install + checker）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### nop-workflow 工作流引擎集成（REQUEST_APPROVAL 通道升级）

- Classification: `optimization candidate`
- Why Not Blocking Closure: cs-local 轻量审批（notify 审批人 + approve mutation + 超时自动审批）满足 L1「发起审批链，超时自动审批」字面；nop-workflow 集成超 A 类授权面且 cs 域无工作流契约
- Successor Required: `yes`（cs 域引入工作流引擎时整体迁移）

### 子工单 ORM 亲子关联列 / fulfillmentProcessId 级终态配置列

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A 类授权面 = FulfillmentStep 实体；双向弱指针 + 尾部 UPDATE_STATUS 组合已满足 L1 追溯/终态语义
- Successor Required: `no`

### P2-RC-055 createFromCatalog catch 告警通知

- Classification: `watch-only residual`
- Why Not Blocking Closure: P2 登记不强制（arm-index:239）；本计划 ③ 已覆盖步骤级失败通知，上游 catch 降级告警为独立控制点
- Successor Required: `yes`（P2-RC-055 修复立项时）

## Closure

Status Note: draft（待独立草案审查）

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计
- Evidence: 待

Follow-up:

- 无（范围内零遗留预期）
