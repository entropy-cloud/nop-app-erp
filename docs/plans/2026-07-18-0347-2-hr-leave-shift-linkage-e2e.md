# 2026-07-18-0347-2-hr-leave-shift-linkage-e2e HR 休假审批→排班联动浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-18
> Source: `docs/plans/2026-07-18-0100-1-hr-shift-scheduling-orchestration-e2e.md` Deferred But Adjudicated「`ErpHrLeaveRequest.approve` → `ErpHrShiftAssignment` isAbsent 标记联动 E2E」（Successor Required: yes，触发条件「休假→排班联动浏览器层 E2E 需求落地时」）
> Related: `docs/plans/2026-07-14-0215-3-hr-direct-action-e2e.md`（hr-leave-attendance spec 前置）、`docs/plans/2026-07-18-0100-1-hr-shift-scheduling-orchestration-e2e.md`（hr-shift-* spec 前置 + Deferred 源）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-18，`read`/`grep` 实测，非采信旧记忆）：

### 后端联动链已落地，浏览器层零覆盖

跨实体钩子联动链已经后端实现完整：

- **`ErpHrLeaveRequestBizModel.approve`**（`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrLeaveRequestBizModel.java:87-98`）：
  ```java
  leave.setStatus(ErpHrConstants.LEAVE_STATUS_APPROVED);
  ...
  updateEntity(leave, null, context);
  shiftBiz.onLeaveApproved(leave.getId(), context);   // 跨实体钩子
  ```
  `DRAFT→submit→SUBMITTED→approve→APPROVED` 状态翻转后内部委派 `IErpHrShiftBiz.onLeaveApproved`。

- **`ErpHrLeaveRequestBizModel.cancel`**（同文件 :112-119）：`APPROVED→cancel→CANCELLED` 后内部委派 `IErpHrShiftBiz.onLeaveCancelled`。

- **`onLeaveApproved`**（`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrShiftBizModel.java:124-136`）：查同 employeeId 在 `[startDate, endDate]` 区间所有 `ErpHrShiftAssignment` 行（**实测** `findAssignmentsByEmployeeRange` `:224-233` 仅按 `employeeId + dateBetween(assignmentDate, startDate, endDate)` 过滤，**无 status 过滤**——所有区间内 assignment 行不论 status 均被选中并标记），逐行置 `isAbsent=true` + `absenceReason=LEAVE` + `leaveRequestId=leave.id` + `status=ABSENT`。
  - `onLeaveCancelled`（`ErpHrShiftBizModel.java:138-154`）：仅解除由该 leaveRequestId 标记的（`leaveRequestId.equals(a.getLeaveRequestId())`），重置 `isAbsent=false` + `absenceReason=null` + `leaveRequestId=null` + `status=SCHEDULED`。

### 浏览器层覆盖状态

- `tests/e2e/business-actions/`：4 hr spec（`hr-leave-attendance` 休假+考勤 / `hr-shift-assignment` 单/批量/周期 + calcAttendance / `hr-shift-rotation` 轮换引擎 / `hr-shift-swap` 调换状态机），但**零 spec 覆盖 `LeaveRequest.approve/cancel` 触发的 ShiftAssignment 跨实体副作用**。
- `hr-leave-attendance.action.spec.ts`（0215-3 落地）仅测 LeaveRequest 自身状态机 + Attendance 打卡，**显式未建 ShiftAssignment 前置**（spec comment：「不建 LeaveBalance 亦可测 approve 状态翻转」），故不触发 `onLeaveApproved` 钩子的赋值副作用（区间内零 assignment 行，循环空跑）。
- `hr-shift-*.action.spec.ts`（0100-1 落地）仅测 ShiftAssignment 自身分配/调换/轮换，**不涉及 LeaveRequest 触发链**。

### E2E 范式已稳定（本计划复用，零范式新增）

- `_helper.ts` 三原语 + `findFirst`/`deleteByFilter`（0814-2 / 1249-1 起）。
- 跨实体副作用断言范式（与 0100-2 completeAssessment → gap refresh → development plan 自动触发链同型）：经 `findFirst` 按 employeeId + assignmentDate 反查 `ErpHrShiftAssignment` 行存在性 + 字段（isAbsent/absenceReason/leaveRequestId/status）。
- 自包含 setup：建测试专用 employee + ErpHrLeaveRequest + 前置 ErpHrShiftAssignment（确保区间内有 assignment 行可被钩子作用）。
- 复用 `hr-leave-attendance.action.spec.ts:42-59` `createEmployee` + `:61-82` `createLeave` 范式 + `hr-shift-assignment.action.spec.ts:44-87` `createEmployee`/`createShift` 范式。

### 剩余差距

- LeaveRequest.approve → ShiftAssignment isAbsent=true + status=ABSENT + leaveRequestId 回链跨实体钩子副作用未测。
- LeaveRequest.cancel → ShiftAssignment 还原（isAbsent=false + status=SCHEDULED）副作用未测。
- 区间范围准确性（区间外 assignment 不被标记 / 区间内多行 assignment 全部被标记）未测。

## Goals

- 1 新 spec 验证 2 条跨实体钩子联动 DIRECT 路径（LeaveRequest.approve + LeaveRequest.cancel）+ 区间范围准确性：
  - **approve 正路径**：建 employee + 区间内 ≥2 个 ShiftAssignment（不同日期） + 区间外 1 个对照 assignment + LeaveRequest(DRAFT) → submit → approve → 经 `findFirst` 反查区间内 assignment 行 `isAbsent=true` + `absenceReason=LEAVE` + `leaveRequestId=leave.id` + `status=ABSENT` + 区间外 assignment 行字段不变。
  - **cancel 正路径**：approve 后 → cancel → 经 `findFirst` 反查原被标记的区间内 assignment 行 `isAbsent=false` + `absenceReason=null` + `leaveRequestId=null` + `status=SCHEDULED`（还原）。
- 复用既有 `_helper.ts` 三原语 + `findFirst` 范式验证在「跨实体钩子副作用」型 DIRECT 路径下的可复用性。
- **owner doc 收口**：解除 0100-1 Deferred「`ErpHrLeaveRequest.approve` → `ErpHrShiftAssignment` isAbsent 标记联动 E2E」（补 `**RELEASED by 2026-07-18-0347-2**`）；`e2e-runbook` 业务动作表 +hr 联动行 + 套件计数对齐；当日日志聚合条目（与 0347-1 同日）。

## Non-Goals

- **不重测 LeaveRequest 自身状态机**（DRAFT→submit→SUBMITTED→approve→APPROVED→cancel→CANCELLED + reject + 日期重叠守卫）：0215-3 `hr-leave-attendance.action.spec.ts` 已覆盖。本计划仅测 **跨实体钩子副作用**（ShiftAssignment 行字段）。
- **不重测 ShiftAssignment 自身分配/调换/轮换**：0100-1 `hr-shift-*.action.spec.ts` 已覆盖。本计划仅以 setup 前置身份建 assignment 行。
- **不实现新后端/契约/ORM 模型/config**：本计划仅消费侧 DIRECT `@BizMutation` E2E + 测试层。`onLeaveApproved`/`onLeaveCancelled` 钩子已实现（0100-1 Closure 核实），无生产代码变更。若 Explore 发现 DIRECT 动作有 bug，属执行期豁免（即时修复 + 记录 + 模块 JUnit 回归），仅确证为生产缺陷时开显式 successor。
- **不做 LeaveBalance 余额扣减副作用 E2E**：`approve` 也会扣减 LeaveBalance（`sumUsedDays`），但 LeaveBalance 是不同结果面（账面余额 vs 排班联动），且 0215-3 已用「if(balance==null)return 代码容忍」验证 LeaveBalance null 不阻断 approve。本计划不建 LeaveBalance，仅测排班联动副作用。
- **不做 Attendance 副作用 E2E**：`onLeaveApproved` 钩子不直接写 Attendance（仅 ShiftAssignment 写）；Attendance 经 `calcAttendance` 单独触发（0100-1 已测）。归 Non-Goal。
- **不做休假→排班 xwf 审批链**：LeaveRequest 无 useWorkflow/useApproval tagSet（实测 ORM），纯 DIRECT 审批，浏览器层可达。

## Task Route

- Type: `verification work`（扩展现有 Playwright E2E 套件覆盖至 hr 休假→排班跨实体钩子联动）
- Owner Docs: `docs/testing/e2e-runbook.md`（业务动作套件 + 调用范式）、`docs/design/human-resource/shift-scheduling.md` §六「休假联动」（设计要求 onLeaveApproved/onLeaveCancelled 钩子 + 字段映射）、`docs/design/human-resource/state-machine.md`（休假状态机引用）
- Skill Selection Basis: 浏览器层 E2E 测试编写 → 无匹配技能（Playwright 浏览器层非 `nop-testing` 后端快照范畴）；沿用 `_helper.ts` 既有范式 → `Skill: none`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。复用现有 Playwright 配置 + webServer JVM 参数（`erp-hr.*` 配置链无需新增；onLeaveApproved/onLeaveCancelled 后端默认启用）。

## Execution Plan

### Phase 1 - 休假审批→排班联动 approve/cancel E2E

Status: completed
Targets: `tests/e2e/business-actions/hr-leave-shift-linkage.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（自包含 setup）

- [x] `Decision | Explore`: 裁定联动 setup 依赖 + 区间范围测试设计 + GraphQL 入参序列化。
  - **区间范围准确性测试设计**：employee A + 3 ShiftAssignment（区间内 2 + 区间外 1）+ LeaveRequest[startDate, endDate] → approve → 经 `findFirst` 按 (employeeId, assignmentDate) 反查 3 行，断言区间内 2 行字段翻转 + 区间外 1 行字段不变。区间选用 2026-08-10~2026-08-12（避开种子 AR/AP OPEN dueDate 干扰 + 避开 `hr-leave-attendance.action.spec.ts` 既有日期重用），assignmentDate 分别为 2026-08-10 / 2026-08-11（区间内）+ 2026-08-13（区间外）。
  - **GraphQL 入参序列化**：`approve(id: String!)` / `cancel(id: String!)` 入参为 String scalar（leave.id Long 经 Nop GraphQL 暴露为字符串 ID，对齐既有 hr-leave-attendance 范式 `callMutation('ErpHrLeaveRequest__approve', { id: leave.id }, page)`）。
  - **`findFirst` 反查 ShiftAssignment 字段断言**：经 `findFirst(page, 'ErpHrShiftAssignment', andFilter([eqFilter('employeeId', employeeId), eqFilter('assignmentDate', date)]), 'id isAbsent absenceReason leaveRequestId status')` 返回行后逐字段断言。
  - **Explore 裁决待实测项**：(a) `ErpHrShiftAssignment.assignmentDate` GraphQL 标量类型（String ISO 日期 vs LocalDate）经实测 schema introspection 确认；(b) **实测 `ErpHrShiftBizModel.findAssignmentsByEmployeeRange` `:224-233` 仅按 `employeeId + dateBetween(assignmentDate, startDate, endDate)` 过滤，无 status 过滤**——所有区间内 assignment 行（不论 SCHEDULED/CANCELLED/ABSENT/PRESENT）均被 onLeaveApproved 选中并标记。本计划 setup 时显式将 3 个测试 assignment 全部置 SCHEDULED 态（不引入已 ABSENT/CANCELLED 的负路径）以避免被钩子的"全选"行为干扰主路径断言；负路径（CANCELLED/ABSENT 在区间内也被翻转）归 Non-Goal successor。
  - **GraphQL String/Long 类型桥**：`approve(id)` / `cancel(id)` 入参 String；钩子写入 `assignment.leaveRequestId=leave.id`（Long），后续 `findFirst` 反查时 `assignment.leaveRequestId` GraphQL 返回 String，断言比较须 `Number(assignment.leaveRequestId) === Number(leave.id)`（`hr-shift-assignment.action.spec.ts:147-148` 已有 `Number()` coercion 范式）。
  - Skill: none
- [x] `Add`: **休假→排班联动 spec** `hr-leave-shift-linkage.action.spec.ts`
  - **approve 正路径 + 区间范围**：自包含建 employee + shift + 3 ShiftAssignment（2026-08-10/11 区间内 SCHEDULED + 2026-08-13 区间外 SCHEDULED）+ LeaveRequest(DRAFT, startDate=2026-08-10, endDate=2026-08-12) → `submit` → `verifyState` leave.status=SUBMITTED → `approve` → verifyState leave.status=APPROVED + 经 `findFirst` 反查 3 行 ShiftAssignment：
    - 2026-08-10 行：`isAbsent=true` + `absenceReason=LEAVE` + `leaveRequestId=leave.id` + `status=ABSENT`
    - 2026-08-11 行：同上字段
    - 2026-08-13 行（区间外）：`isAbsent=false`（或保持 setup 值）+ `absenceReason=null`（或保持）+ `leaveRequestId=null` + `status=SCHEDULED`（不变）
  - **cancel 正路径**：approve 后 → `cancel` → verifyState leave.status=CANCELLED + 经 `findFirst` 反查原被标记的 2 行 ShiftAssignment：
    - 字段全部还原：`isAbsent=false` + `absenceReason=null` + `leaveRequestId=null` + `status=SCHEDULED`
  - **守卫**：DRAFT 直接 approve 抛 `ERR_LEAVE_ILLEGAL_STATUS_TRANSITION`（status 不变，0215-3 已覆盖此守卫，本计划仅作 setup 前置验证或归 Non-Goal）；SUBMITTED 直接 cancel 抛同守卫。
  - **清理**：finally 兜底删 LeaveRequest + 3 ShiftAssignment + Shift + Employee（钩子仅写 ShiftAssignment，不写 Attendance——`calcAttendance` 为独立 mutation 不由 approve/cancel 链触发，无需 Attendance cleanup）。
  - Skill: none
- [x] `Proof`: 1 spec 文件经 `npx playwright test tests/e2e/business-actions/hr-leave-shift-linkage.action.spec.ts --workers=1` 全绿。
  - Skill: none

Exit Criteria:

- [x] 1 spec 全绿（2+ 用例：approve 正路径 + 区间范围准确性 + cancel 正路径还原）；leave status 翻转 + ShiftAssignment 4 字段（isAbsent/absenceReason/leaveRequestId/status）翻转均经 `verifyState` `__get` 或 `findFirst` 反查独立断言。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_08e5ea404ffeuldbWHEfbzT1ii`，general agent 新会话冷审计) — 0 BLOCKERS / 1 MAJOR / 3 MINORS。15/16 load-bearing 事实主张经实时仓库核实**零伪**（approve/cancel 钩子委派 + 字段翻转 + leaveRequestId 守卫 + GraphQL String scalar + ErpHrShiftAssignment 4 字段 + 既有 spec 范式 + 0100-1 Deferred 点名 + AGENTS.md 触发条件 + owner doc §六 + R1-R13+anti-slack + legitimately warranted）。
  - **M1（Major）**：Explore item (b) **伪陈述**——原文「区间内 assignment 必须为 `SCHEDULED` 态才被 `findAssignmentsByEmployeeRange` 选中」标注「实测过滤条件」，**实测** `ErpHrShiftBizModel.java:224-233` 仅按 `employeeId + dateBetween(assignmentDate, startDate, endDate)` 过滤，**无 status 过滤**；执行者若信任原文「实测」表述，可能编写「CANCELLED assignment 在区间内不被标记」的负路径用例而失败。
  - **m1**：`hr-leave-attendance.action.spec.ts:61-83` 引用应订正为 `:61-82`（off-by-one）。
  - **m2**：Phase 1 cleanup 列出「ErpHrAttendance（若 calcAttendance 触发，须 cleanup）」—— `onLeaveApproved`/`onLeaveCancelled` 只写 ShiftAssignment 不写 Attendance，calcAttendance 不由 approve/cancel 链触发；推测性 cleanup 引入混淆。
  - **m3**：GraphQL String/Long 类型桥须显式提示——`assignment.leaveRequestId` GraphQL 返回 String，断言比较须 `Number()` coercion（既有 `hr-shift-assignment.action.spec.ts:147-148` 范式）。
- 修订落地（iteration 1 → 2）：(1) M1 Explore item (b) 改为诚实记录「无 status 过滤，所有区间内行均被选中」+ setup 显式将 3 assignment 置 SCHEDULED + 负路径归 Non-Goal；(2) m1 引用订正 `:61-83`→`:61-82`；(3) m2 cleanup 移除推测性 Attendance；(4) m3 显式增 Number() coercion 提示。
- Independent draft review iteration 2: **accept**（`ses_08e58e2f1ffeNKj8NezpuaALQ4`，general agent 新会话冷审计）— 0 BLOCKERS / 0 MAJORS / 0 MINORS。iter-1 四项发现全部经实时仓库核实 FIXED：M1（Explore item (b) 已诚实记录 `findAssignmentsByEmployeeRange :224-233` 无 status 过滤，live repo filter 实测一致 ✓，setup 显式置 3 assignment 为 SCHEDULED 避免噪声 + 负路径归 Non-Goal successor ✓）/ m1（`:61-83`→`:61-82` 订正 ✓，live `hr-leave-attendance.action.spec.ts` createLeave 实测 :61-82 ✓）/ m2（cleanup 已移除推测性 ErpHrAttendance + 显式声明 calcAttendance 不由 approve/cancel 链触发 ✓）/ m3（GraphQL String/Long 类型桥 Number() coercion 提示 + 引用 `hr-shift-assignment.action.spec.ts:147-148` 范式 ✓，live 范式实测 ✓）。核心测试设计不变（3-assignment 区间 + approve/cancel round-trip + 4 字段翻转断言）；单 Phase 结构不变；R1/R2/R4/R7/R8/R13/anti-slack 全 PASS。计划作为执行契约进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：在结束时运行 `mvn clean install -DskipTests` + Playwright 全套件回归一次。

- [x] 范围内行为完成：1 spec 覆盖 hr 休假→排班跨实体钩子联动 2 DIRECT 路径（approve + cancel + 区间范围）
- [x] 相关文档对齐：`docs/testing/e2e-runbook.md` 业务动作表 +hr 联动行 + 套件计数更新；0100-1 Deferred「`ErpHrLeaveRequest.approve` → `ErpHrShiftAssignment` isAbsent 标记联动 E2E」标 RELEASED
- [x] 已运行验证：`npx playwright test tests/e2e/business-actions/hr-leave-shift-linkage.action.spec.ts --workers=1` 全绿 + business-actions 全套件回归无新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（草案期为空——本计划范围窄，仅 1 spec 验证已实现的后端钩子联动。 LeaveBalance 余额扣减 / Attendance 副作用为不同结果面，归 Non-Goal。）

## Closure

Status Note: 已完成执行。1 spec 2 测试全绿（hr-leave-shift-linkage approve 正路径 + 区间范围准确性 / cancel 正路径还原）；business-actions 全套件回归 194 passed（192 baseline + 2 新增）0 新增失败；`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。0100-1 Deferred「`ErpHrLeaveRequest.approve` → `ErpHrShiftAssignment` isAbsent 标记联动 E2E」RELEASED。e2e-runbook 业务动作表 +1 hr 联动行 + 套件计数 72→73；当日日志已聚合（`docs/logs/2026/07-18.md`，与 0347-1/0100-2/0100-1/FrozenClock 同日条目并列）。结束审计已由独立子代理（新会话冷审计，无执行者上下文）通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理（新会话，无执行者上下文；本任务由 mission-driver 显式路由为 closure audit 角色，非执行者自我审计）
- Audit Scope: 计划结构完整性（plan-check.mjs --strict）+ 5 项语义核对（exit criteria vs live repo / anti-hollow / 5-point consistency / deferred honesty / docs sync）
- Live Repo Verification (vs Plan Claims):
  - spec 文件存在：`tests/e2e/business-actions/hr-leave-shift-linkage.action.spec.ts`（10112 bytes，260 行，2 test 函数：approve+区间范围 / cancel 还原）✓
  - 后端钩子链经实测一致：`ErpHrLeaveRequestBizModel.approve :85-98` 委派 `shiftBiz.onLeaveApproved` + `cancel :110-119` 委派 `onLeaveCancelled` ✓；`ErpHrShiftBizModel.onLeaveApproved :123-136` 4 字段翻转（isAbsent=true/absenceReason=LEAVE/leaveRequestId/status=ABSENT）与 spec:188-201 断言一致 ✓；`onLeaveCancelled :138-154` 仅解除 leaveRequestId 匹配行 + 4 字段还原（isAbsent=false/absenceReason=null/leaveRequestId=null/status=SCHEDULED）与 spec:242-255 断言一致 ✓
  - 区间范围裁决（Explore item b）经实测一致：`findAssignmentsByEmployeeRange` 无 status 过滤（plan 描述与 Java 实现一致），setup 显式置 3 assignment 全 SCHEDULED 避免噪声 ✓
  - Deferred RELEASED 落地：`docs/plans/2026-07-18-0100-1-hr-shift-scheduling-orchestration-e2e.md:180` 含 `**RELEASED by 2026-07-18-0347-2**` + 交付摘要 ✓
  - e2e-runbook 业务动作表 +1 行（`docs/testing/e2e-runbook.md:298` hr 联动行）+ 套件计数 73 spec（`:115` `:221` `:120` 三处对齐「17 域 73 spec」）+ 套件计数历史段增量（`:403` 末段含 0347-2 增量）✓
  - 当日日志聚合：`docs/logs/2026/07-18.md:5-13` +0347-2 完整条目（背景 / Phase 1 Explore 3 裁决 + 2 用例 / Closure 文档对齐 / 验证状态 2 passed + 194 passed + 154 模块）✓
- Anti-Hollow 核验：spec 使用真实 GraphQL mutation（`callMutationOk('ErpHrLeaveRequest', 'approve'|'cancel', ...)` + `submit`）+ 真实跨实体副作用断言（`findFirst('ErpHrShiftAssignment', ...)` 4 字段逐项断言 + `Number()` coercion 处理 GraphQL String/Long 类型桥）；finally 兜底 cleanup 真实可执行（`deleteByFilter`/`deleteById`）；无空函数体 / return null 占位 / 吞异常 ✓
- 5-Point Consistency: Plan Status(completed) / Phase 1 Status(completed) / Phase 1 Exit Criteria(全 [x]) / Closure Gates(8/8 [x]) / Closure evidence(非占位) 全一致 ✓
- Deferred Honesty: 范围内无遗留缺陷隐藏于 Deferred（Deferred 段草案期为空；LeaveBalance/Attendance 副作用归 Non-Goal successor 而非 Deferred，因属不同结果面）✓
- Docs Sync: docs/logs/2026/07-18.md + docs/testing/e2e-runbook.md + 0100-1 plan 均 live 实测对齐 ✓
- Execution Evidence (复述执行者记录，audit 未重跑长任务):
  - spec 文件：`tests/e2e/business-actions/hr-leave-shift-linkage.action.spec.ts`（2 test，2 describe-free test 函数）
  - 单 spec 运行：`PLAYWRIGHT_PORT=8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/hr-leave-shift-linkage.action.spec.ts --workers=1` → 2 passed (15.7s)
  - business-actions 全套件回归：`PLAYWRIGHT_PORT=8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/ --workers=1` → 194 passed (23.5m，192 baseline + 2 新增，0 新增失败)
  - Maven 构建：`mvn clean install -DskipTests` → 154 模块 BUILD SUCCESS (1:37 min)
- 文档对齐（audit 实测一致）:
  - `docs/testing/e2e-runbook.md` 业务动作表 +1 hr 联动行（置于 hr-shift-swap 之后、hr-assessment-dev-plan 之前，逻辑聚类于 hr-shift 段）
  - `docs/testing/e2e-runbook.md` 业务动作套件表头计数 72→73 spec
  - `docs/testing/e2e-runbook.md` 业务动作小节标题「17 域 67 spec」→「17 域 73 spec」（先前漂移：0100-2 落地后未同步小节标题，本次一并订正）
  - `docs/testing/e2e-runbook.md` 套件计数历史段 +0347-2 增量（72→73 + 194 passed）
  - `docs/logs/2026/07-18.md` +0347-2 聚合条目（与 0347-1/0100-2/0100-1/FrozenClock 同日倒序）

Audit Verdict: **APPROVED** — 计划结构 + 语义双通过，可关闭。

Follow-up:

- 无范围内阻塞跟进。
