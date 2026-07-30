# 2026-07-30-0720-1-r1-24-crm-stage-guard-reminder-dead-field crm stageId 单向递增守卫实现 + Event reminderMinutesBefore 死字段激活

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.24（P1-MA2-075 + P1-MA2-076，源自 A2.14 crm 状态机审查）
> Related: `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`、`docs/audits/arm-index.md §P1-MA2-075/076`；plan `2026-07-30-0341-3-r1-17-pur-sal-ast-reverse-approve-state-machine.md`（owner doc 强制契约 vs 代码更宽松，实现守卫对齐 owner doc 先例）、plan `2026-07-30-0631-1-r1-21-projects-close-start-precondition-dict-deferred.md`（config-gated STRICT/WARN 前置门控先例）
> Audit: required

## Current Baseline

两项 finding 经实仓逐项确认：均为「owner doc 声明单向递增/per-event 提醒但代码更宽松或忽略」类型，**不破坏已实现主路径**（crm Lead 5 态 NEW→QUALIFIED→CONVERTED/LOST/CANCELLED 全迁移 + Event PLANNED→COMPLETED/CANCELLED 状态机完整 + Lead 转化跨域 Facade + convLog 全量留痕）。

**P1-MA2-075（crm stageId 单向递增守卫未实现）— 确认：**
- owner doc `docs/design/crm/state-machine.md §stageId 迁移规则 L40`「stageId 沿 ErpCrmStage.sequence **递增前移（不能跳级回退）**」+ L56「阶段跳级（跳过 sequence 递增）拒绝：只能前移到下一阶段」+ §审查提示 L194「阶段迁移（stageId）的 sequence **单向递增约束**」。
- 代码 `ErpCrmLeadProcessor.java:24-25` Javadoc 显式声明「阶段流转（moveStage）：按 ErpCrmStage#getSequence() **允许前移/回退（销售流程中阶段可能反复）**」+ `:138-143 doMoveStage` 仅 `lead.setStageId(toStage.getId())` **无 sequence 方向比较**；`validateMovable:91-97` 仅守卫 docStatus∈{NEW,QUALIFIED}，无方向。代码比 owner doc 更宽松。
- `FunnelAggregationEngine.java:200-202`（排序）+ `:274` 报表按 stage `sequence` 排序，假设 monotonic progression——阶段回退致漏斗/转化率/dropOffRate 统计漂移。
- 实仓 `ErpCrmStage.getSequence()` 存在（`:110-115 doQualify` 经 `findFirstStage` 按 sequence 升序取首条印证）。

**P1-MA2-076（crm Event reminderMinutesBefore 字段死字段）— 确认：**
- ORM `module-crm/model/app-erp-crm.orm.xml` ErpCrmEvent `reminderMinutesBefore` 列存在（owner doc `state-machine.md §7 L168`「事件提醒 Job 读取 PLANNED 事件，按 reminderMinutesBefore 发送通知」）。
- 代码 `ErpCrmEventBizModel.findDueReminders:82-101` 用全局 `window = windowMinutes==null?60:windowMinutes`（:89）+ `le("startDateTime", now+window)`（:98）扫描 PLANNED 事件，**从不读取 per-event reminderMinutesBefore**。grep 全 `module-crm/erp-crm-service/src/main` `getReminderMinutesBefore|reminderMinutesBefore` 业务读取零匹配。per-event 自定义提前提醒分钟数静默忽略——全部用全局 60 分钟窗口。
- view 层（`ErpCrmEvent.view.xml:68/:92`）暴露 reminderMinutesBefore 为用户可编辑字段（A4.8 复核：view 忠实绑定 ORM，死字段根因在后端）——用户可设置但后端忽略。

**保护区域：** 不触及会计/财务/数据删除保护区域（无凭证/折旧/删除写路径变更）。两项均为 BizModel/Processor 行为变更 + ErrorCode + config，按 roadmap 规则走标准 plan-audit + closure-audit（不触及 ORM ask-first——不改 model/*.orm.xml，reminderMinutesBefore 列已存在）。

## Goals

- 消除 crm 域 owner doc 与代码间两项悬空：(1) **实现** stageId sequence 单向递增守卫（config-gated，对齐 owner doc §stageId 迁移规则契约）；(2) **实现** findDueReminders 按 per-event reminderMinutesBefore 计算（fallback 全局 window）。
- owner doc 与代码一致；漏斗/转化率报表 monotonic progression 不变量经显式门控保护；per-event 提醒分钟数真正生效。

## Non-Goals

- 不删除/合并 stageId 回退路径（采纳 config-gated 守卫机制——默认 STRICT 拦截回退对齐 owner doc，业务确需回退时设 `erp-crm.allow-stage-backward=true` 放行）。
- 不改 ErpCrmStage 的 sequence 语义或重排既有阶段数据（sequence 字段为全局配置记录，既有种子数据不动）。
- 不改 ORM `reminderMinutesBefore` 列（列已存在；本计划仅激活后端读取）。
- 不实现「QUALIFIED 超过 7 天无 stageId 前移跟进提醒 Job」（owner doc §L104 声明，grep 零匹配）——归 owner doc Deferred（残留风险，非本两项 finding 范畴）。
- 不重构 FunnelAggregationEngine 算法（仅标注阶段回退对 monotonic 报表的近似影响）。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐）+ `implementation-only change`（Processor 守卫 + BizModel 提醒计算 + ErrorCode + config）
- Owner Docs: `docs/design/crm/state-machine.md`
- Skill Selection Basis: P1-MA2-075 涉及 Processor 方法行为变更 + sequence 方向守卫 + ErrorCode + config-gated → `Skill: nop-backend-dev`；P1-MA2-076 涉及 BizModel 查询逻辑变更 → `Skill: nop-backend-dev`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 两项 finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：两项 finding 处置方案逐项裁决（**选择性裁决**——075 实现守卫对齐 owner doc 强制契约[对齐 R1.17 先例] + 076 实现激活既有字段）。
      - P1-MA2-075 stageId 单向递增守卫：**实现（arm-index 推荐方向方案A）**。理由：(1) owner doc §stageId 迁移规则 + §审查提示 是强制契约「不能跳级回退」，项目原则 owner doc 为行为基线；(2) arm-index §P1-MA2-075 方案A（推荐）即 doMoveStage 增 sequence 方向守卫；(3) containment 友好（doMoveStage 单点 sequence 比较 + 1 ErrorCode + config-gated）；(4) FunnelAggregationEngine 按 sequence 排序假设 monotonic（:200-202 排序 + :274）——守卫保护报表完整性。**默认值子裁决**：config `erp-crm.allow-stage-backward` 默认 **false（STRICT 拦截回退）** 对齐 owner doc 契约；业务确需回退（销售流程阶段反复）时设 true 放行。**WARN-as-default 为已考虑并否决的替代方案**——保留当前「允许回退」行为看似零回归，但会使 owner doc §审查提示「单向递增约束」永远不被强制（守卫永不触发），等于变相采纳方案B（owner doc 对齐代码），与「owner doc 为行为基线」原则冲突。**行为变更影响核实**：grep `module-crm/erp-crm-service/src/test` moveStage 仅 `TestErpCrmLeadConversion:86-92` 前移 STAGE_QUALIFIED→STAGE_DEMO（前移路径），**零测试行使回退**——STRICT 默认不破坏既有测试。残留风险：生产既有 Lead 若存在历史回退 stageId 数据，仅影响后续 moveStage 调用（历史数据不动），业务确需回退设 config=true。
      - P1-MA2-076 reminderMinutesBefore 死字段：**实现（arm-index 推荐方向方案A）**。理由：(1) ORM 列已存在 + view 已暴露为可编辑——用户可设置但后端忽略是 silent functional gap；(2) containment 友好（findDueReminders 单点改窗口计算：per-event reminderMinutesBefore 优先，null fallback 全局 windowMinutes）；(3) owner doc §7 契约「按 reminderMinutesBefore 发送通知」落地。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 逐项记录选择 + 理由 + 默认值子裁决 + 残留风险 + draft review 复核点；075/076 均进 Phase 2/3（实现）。

### Phase 2 - stageId 单向递增守卫实现（P1-MA2-075）

Status: completed
Targets: `module-crm/erp-crm-service/.../processor/ErpCrmLeadProcessor.java`、`ErpCrmConstants.java`、`ErpCrmConfigs.java`、`ErpCrmErrors.java`、`docs/design/crm/state-machine.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [x] **Add（config + constants）**：`ErpCrmConstants` 增 `CONFIG_ALLOW_STAGE_BACKWARD = "erp-crm.allow-stage-backward"` + `DEFAULT_ALLOW_STAGE_BACKWARD = false`；`ErpCrmConfigs` 增 `allowStageBackward()` 读 config 默认 false。
      - Skill: `nop-backend-dev`
- [x] **Add（ErrorCode）**：`ErpCrmErrors` 增 `ERR_STAGE_BACKWARD_MOVE`（`erp.err.crm.stage-backward-move`，描述「线索 {leadCode} 不允许阶段回退：fromStage sequence={fromSeq} → toStage sequence={toSeq}（如业务确需回退，设 erp-crm.allow-stage-backward=true）」，复用 ARG_LEAD_CODE + 新增 ARG_FROM_SEQUENCE/ARG_TO_SEQUENCE）。
      - Skill: `nop-backend-dev`
- [x] **Fix（doMoveStage 方向守卫）**：`ErpCrmLeadProcessor.moveStage:54-61` 在 `requireStage` 后、`doMoveStage` 前增 `validateStageDirection(lead, toStage, context)`：当 fromStageId 非 null 时加载 fromStage，比较 `toStage.sequence < fromStage.sequence`——STRICT 模式（allowStageBackward()=false）抛 `ERR_STAGE_BACKWARD_MOVE`；allow-backward=true 时 LOG.warn 放行（保留审计 convLog）。fromStageId 为 null（首次入漏斗）跳过方向校验。更新 :24-25 + :135 Javadoc 删除「允许前移/回退」，改为「stageId 沿 sequence 单向递增（owner doc §stageId 迁移规则），回退经 `erp-crm.allow-stage-backward`=true 放行」。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——(1) stage sequence 3→2 回退（STRICT 默认）assertThrows `ERR_STAGE_BACKWARD_MOVE`；(2) sequence 2→3 前移成功（行为不变）+ convLog 写入；(3) STRICT 模式回退 + config=true（WARN）→ moveStage 成功（LOG.warn 放行）+ convLog 写入；(4) fromStageId=null（首次入漏斗）跳过方向校验成功。迁移既有依赖「允许回退」的测试（若有——设 config=true 或调整阶段顺序）。
      - Skill: `nop-backend-dev`
- [x] **Add（owner doc）**：state-machine.md §stageId 迁移规则 L40 + L56 + §审查提示 L194 更新为「stageId 沿 sequence 单向递增，回退经 doMoveStage `validateStageDirection` 守卫：STRICT 模式（`erp-crm.allow-stage-backward` 默认 false）抛 `ERR_STAGE_BACKWARD_MOVE`；true 时 LOG.warn 放行」；FunnelAggregationEngine 段补注「报表按 sequence 排序假设 monotonic——STRICT 守卫保护；allow-backward=true 放行回退时转化率按 sequence 排序近似」。
      - Skill: `none`

Exit Criteria:

- [x] doMoveStage STRICT 模式回退抛 `ERR_STAGE_BACKWARD_MOVE`（grep 确认 validateStageDirection 落地）；config 默认 false；前移行为不变；新增/迁移测试全绿（Closure Gates 跑全量 mvn）；owner doc §stageId 迁移规则/§审查提示 与代码一致。

### Phase 3 - findDueReminders 按 per-event reminderMinutesBefore 计算（P1-MA2-076）

Status: completed
Targets: `module-crm/erp-crm-service/.../entity/ErpCrmEventBizModel.java`、`docs/design/crm/state-machine.md`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] **Fix（per-event 窗口计算）**：`ErpCrmEventBizModel.findDueReminders:82-101` 当前用全局 `window` 单一 `le("startDateTime", now+window)` 过滤。改为支持 per-event reminderMinutesBefore：查询窗口上界取 `max(windowMinutes 全局)`（保持向后兼容扫描所有候选 PLANNED 事件），在流式过滤中对每个 event 计算 `effectiveReminder = event.getReminderMinutesBefore()!=null ? event.getReminderMinutesBefore() : window`，仅保留 `startDateTime ∈ [now, now+effectiveReminder]` 的 event（即 per-event 到期判定）。null reminderMinutesBefore fallback 全局 window 行为不变。保持 @BizQuery 签名 + @Optional windowMinutes 入参不变。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——(1) event A reminderMinutesBefore=1440（提前 1 天）+ startDateTime=now+1天 → 命中；同 event 用旧全局 60min 窗口不命中（证明 per-event 生效）；(2) event B reminderMinutesBefore=null + startDateTime=now+30min → 命中（fallback 全局 60min 行为不变）；(3) event C reminderMinutesBefore=15 + startDateTime=now+45min → 不命中（45>15）；(4) PLANNED 守卫 + enabled=false 返回空（行为不变）。
      - Skill: `nop-backend-dev`
- [x] **Add（owner doc）**：state-machine.md §7 L168 核对一致——「事件提醒 Job 读取 PLANNED 事件，按 per-event reminderMinutesBefore 计算到期窗口（null fallback 全局 windowMinutes 默认 60）」。
      - Skill: `none`

Exit Criteria:

- [x] findDueReminders 按 per-event reminderMinutesBefore 命中（grep 确认 getReminderMinutesBefore 读取落地）；null fallback 行为不变；新增测试全绿；owner doc §7 与代码一致。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_04fd0002fffe9G6lM7V0pLILfS, fresh session) because 全部基线声明经实仓 file:line 验证 TRUE（doMoveStage:138-143 无 sequence 方向比较 / Javadoc :24-25「允许前移/回退」/ findDueReminders:82-101 用全局 window 不读 reminderMinutesBefore / ORM reminderMinutesBefore 列存在 / owner doc §stageId 迁移规则「不能跳级回退」+ §审查提示「单向递增约束」）；075/076 均实现=arm-index 方案A 推荐方向；两项构成单一结果表面（同 state-machine.md + 同 erp-crm-service + 同 R1.24/A2.14 批次，规则 14 认可）；Closure Gates 含 mvn 门控（含代码变更）。采纳非阻塞修订：(1) 075 默认值子裁决补充「WARN-as-default 为已考虑并否决的替代方案」+ 行为变更影响核实（grep 测试仅前移路径 STAGE_QUALIFIED→STAGE_DEMO，零回退行使 → STRICT 默认不破坏既有测试）；(2) FunnelAggregation 引用行号校正（:200-202 排序 + :274）。

## Closure Gates

> 本计划含代码变更（P1-MA2-075/076），故 Closure Gates 含全量 `mvn` 验证（见执行时规则 7）。

- [x] 范围内行为完成（075 stageId 守卫实现 + 076 reminderMinutesBefore 激活）
- [x] 相关文档对齐（crm/state-machine.md）
- [x] 已运行验证（`mvn clean install -DskipTests` 全绿 + crm 域 `mvn test` 全绿（137 tests，0 failures）+ compliance checker 本计划零新增命中；grep 验证 075/076 落地）
- [x] 无范围内项目降级为 deferred/follow-up（075/076 均为范围内存活实现项）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### QUALIFIED 超过 7 天无 stageId 前移跟进提醒 Job（owner doc §L104）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 非 P1-MA2-075/076 范畴（owner doc §L104 声明但 grep 零匹配）；属 missing-automation（新 Job + scheduler），与两项 finding 不同根因。
- Successor Required: `yes`（crm 跟进提醒自动化需求时实现 ErpCrmLeadFollowupJob）

## Closure

Status Note: PASS — 独立结束审计（新会话）已通过，075/076 两项 finding 均实现落地并经实仓复核 + mvn 全绿。

Closure Audit Evidence:

- 独立结束审计（新会话 ses_04fbea97fffeKsMhUl9saqQaGP）已对实仓逐项复核：`ErpCrmLeadProcessor.validateStageDirection`（:112-131）落地方向守卫，moveStage 在 :65 调用先于 doMoveStage :66，fromStageId=null 跳过、toSeq<fromSeq 在 STRICT 默认（allowStageBackward()=false，ErpCrmConfigs:17-20）抛 ERR_STAGE_BACKWARD_MOVE、true 时 LOG.warn 放行；`ErpCrmEventBizModel.findDueReminders`（:83-127）按 per-event reminderMinutesBefore 计算到期窗口、null fallback 全局 window，PLANNED 守卫与 enabled=false 早返回保持；owner doc state-machine.md L40/L56/L90/L170/L196 与代码一致。验证由审计者本人运行：`mvn clean install -DskipTests` → BUILD SUCCESS；`mvn test -pl module-crm/erp-crm-service -am` → Tests run: 137, Failures: 0, Errors: 0, Skipped: 0（含 4 个新测试类共 10 测试全绿）；既有 TestErpCrmLeadConversion moveStage 为前移（seq 10→20）零回退行使，STRICT 默认无回归；无 model/*.orm.xml 或财务写路径变更；roadmap R1.24=done。结论 PASS。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件。
