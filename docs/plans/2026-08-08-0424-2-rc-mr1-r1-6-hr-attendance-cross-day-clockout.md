# 2026-08-08-0424-2-rc-mr1-r1-6-hr-attendance-cross-day-clockout RC-R1.6 — hr 夜班跨天 clockOut 回退（P1-RC-013，MR1 第一批纯预授权，方案 A）

> Plan Status: completed
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.6（MR1 第一批纯预授权：hr 夜班跨天 clockOut 回退，P1-RC-013）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.6 行 + `docs/audits/arm-index.md` P1-RC-013 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3.1（RC-R1.6 = 「方案 A 纯 BizModel 逻辑预授权（方案 B 触 ORM 标注）」）
> Related: `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`（A4.2.19 运行时影响面：clocking 侧未实现 CONFIRMED + 逃生路径 = 标准 CRUD）；`docs/design/human-resource/use-cases.md`（L1 UC-HR-06⑭）+ `docs/design/human-resource/shift-scheduling.md`（L2 §4.2 跨天班次处理 + §九.6 跨天班次日历归属）；`docs/plans/2026-08-08-0424-1-rc-mr1-r1-5-hr-attendance-last-wins.md`（同域同批 MR1 计划范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-013（arm-index 行）**：UC-HR-06⑭「跨天打卡处理」clocking 侧未实现。L1 `use-cases.md:72` 异常段逐字「跨天打卡处理」；L2 `shift-scheduling.md §4.2:200-212` 逐字「夜班（23:00-08:00）等跨天班次…自动识别次日 endTime」。
- **calc 侧已实现且强测**：`ShiftAttendanceCalculator.isCrossDayShift:24-28`（endTime ≤ startTime 判跨天）+ `calcEarlyLeaveMinutes:61-74`（跨天班次 endTime 取次日），`TestErpMfgAttendanceCrossDayNightShift`/`TestErpHrShiftScheduling` 强测覆盖（A4.2.19 复核确认）。
- **clocking 侧未实现（本计划修复面）**：`ErpHrAttendanceClockOutProcessor.clockOut:19-20` `findAttendance(employeeId, today)` 按 clockOut 当日查找；夜班跨天签退（23:00 Mon 签到 date=Mon、08:00 Tue 签退）查 date=Tue → null → 抛 `ERR_NOT_CLOCKED_IN`。
- **A4.2.19 运行时确认（`2026-08-07-0530` 报告）**：`ClockOutProcessor:19-20` + `AbstractErpHrAttendanceProcessor.findAttendance:32-38`（`eq("date", date)` 精确查找无跨天回退）CONFIRMED；夜班签退运行时不可用；逃生路径 = 标准 CRUD；无补录自动化。**维持 P1 不撤销**（Q4 强制实现）。
- **跨天语义锚点**：`shift-scheduling.md §九.6`「夜班排班归属到开始日期」——`ErpHrShiftAssignment.assignmentDate` = 夜班开始日（如 Mon）；签到记录 `ErpHrAttendance.date` = 当日（Mon）。签退次日（Tue）须回退查 `date=yesterday` 的记录。
- **方案 B（触 ORM ask-first）**：`ErpHrAttendance` 增 `originAssignmentDate` 列明确跨天归属——本计划**不采用**（越出第一批纯预授权边界，Non-Goals）。
- **预授权判据**（第一批纯预授权）：方案 A 纯 BizModel 逻辑（`ErpHrAttendanceClockOutProcessor` 查找逻辑增强），不触 ORM/会计核心/删除；**无 ask-first checkbox**。roadmap RC-R1.6 行 `todo`，Deps（R1.0 done）已满足。

## Goals

- `ErpHrAttendanceClockOutProcessor.clockOut` 查找逻辑增强（方案 A）：`findAttendance(employeeId, today)` 无记录（或记录 clockIn 为空）时，**回退查 `date=yesterday` 的记录**，且该员工 **yesterday 有跨天班次排班**（`ErpHrShiftAssignment` 存在 assignmentDate=yesterday + 关联 `ErpHrShift` 且 `isCrossDayShift(shift)` 成立）时，对昨日记录执行 clockOut（`setClockOut(now)` + 重算 workHours）。
- 回退条件裁决：既要求「昨日存在跨天排班」也要求「昨日记录 clockIn 非空」（对齐既有 `ERR_NOT_CLOCKED_IN` 守卫语义：clockIn==null 仍拒绝）。
- 失败模式保持：today 无记录 + 昨日无跨天排班/昨日无记录/昨日 clockIn 空 → 仍抛 `ERR_NOT_CLOCKED_IN`（与现状一致，无行为回退）。
- 新增 dedicated 测试（夜班跨天 clockOut 场景：seed 昨日 attendance(clockIn=23:00) + 昨日跨天排班 → 今日 clockOut → 断言昨日记录 clockOut 落值 + workHours 正确）。
- 回填 arm-index P1-RC-013 → `done (RC-R1.6)` + roadmap RC-R1.6 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不做方案 B**（`originAssignmentDate` ORM 列，触 ask-first 越出第一批边界——登记 Deferred But Adjudicated，Successor Required: yes 触发条件 = 第二批启动或人工裁决 ORM 授权）。
- **不改 `ErpHrAttendance` / `ErpHrShiftAssignment` / `ErpHrShift` ORM 结构**（零结构变更）。
- **不做昨日记录的自动补建**（yesterday 无 attendance 记录时不新建——跨天签退仅作用于已签到记录；补建属补卡范畴归 RC-R1.7）。
- **不改 `getTodayAttendance` / clockIn 行为**（仅 clockOut 查找链增强）。
- **不改真相源**（use-cases/shift-scheduling 需求契约段；shift-scheduling.md 仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/human-resource/use-cases.md`（L1 UC-HR-06⑭）+ `docs/design/human-resource/shift-scheduling.md`（L2 §4.2 跨天班次处理 + §九.6 日历归属）+ `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-...md`（A4.2.19 降级证据）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）
- Skill Selection Basis: 实现面 = Processor 查找逻辑 + 跨实体访问（`nop-backend-dev`：注入 `IErpHrShiftAssignmentBiz`/`IErpHrShiftBiz` 或经既有 `ShiftAttendanceCalculator.isCrossDayShift` 静态工具，跨实体访问优先 I*Biz 注入规则）+ JUnit 测试（`nop-testing`：JunitAutoTestCase/seed 范式 + 冻结时钟推进日期——`HrFrozenClockExtension` 冻结 2026-07-17，yesterday=2026-07-16 天然可测）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新增 infra/config（纯查找逻辑修复）。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-hr/erp-hr-service`。

## Execution Plan

### Phase 1 - ClockOutProcessor 跨天回退查找

Status: completed
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrAttendanceClockOutProcessor.java`（主）+ `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/AbstractErpHrAttendanceProcessor.java`（helper，若需）
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [x] `Decision` 回退判据精确化：today 无记录**或**今日记录 clockIn==null → 触发回退探查；回退探查 = `findAttendance(employeeId, yesterday)` 非空 && 该记录 clockIn 非空 && 员工 yesterday 存在跨天排班（`ErpHrShiftAssignment` employeeId+assignmentDate=yesterday → 关联 `ErpHrShift` 非空 → `shift.isCrossDayShift`；**shift 关联为空（孤儿 assignment）→ 保守判不命中回退**，防 `isCrossDayShift(null)` NPE，对齐 `ErpHrShiftCalcAttendanceProcessor:29-32` null shift 跳过范式）。备选（否决）：仅回退查昨日记录不校验排班——否决理由：无排班校验会误接纳「昨日普通班次今日补签退」的异常场景，且与 L2「跨天班次日历归属」语义不符；残留风险记录：极端场景「昨日排班+今日记录也排班」双记录并存时仍以 today 记录优先（回退仅在 today 无可用记录时触发）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpHrAttendanceClockOutProcessor.clockOut` 回退分支：today 记录可用（clockIn 非空）→ 既有路径；否则回退探查 yesterday（含排班校验）→ 命中则 `setClockOut(now)` + `computeWorkHours` + `saveOrUpdateAttendance`；未命中 → 抛 `ERR_NOT_CLOCKED_IN`（与现状一致）。
      - Skill: `nop-backend-dev`
- [x] `Fix` 跨天排班校验 helper：优先注入 `IErpHrShiftAssignmentBiz` 并复用既有 `findByEmployeeAndDate(employeeId, assignmentDate, context)`（`module-hr/erp-hr-dao/.../IErpHrShiftAssignmentBiz.java:52-55` 已存在，恰好匹配本查找需求）→ 取关联 `ErpHrShift` → `ShiftAttendanceCalculator.isCrossDayShift(shift)`；仅当 I*Biz 无法满足时回落 `daoProvider.daoFor(ErpHrShift.class)`（注释记录原因，对齐 AGENTS.md 跨实体访问规则）。**执行期决策（偏离注记）**：shift 关联未随查询加载时的回落采用 `IErpHrShiftBiz.get(shiftId, false, context)`（同域 I*Biz，返回 null 语义同 daoFor）而非 `daoProvider.daoFor(ErpHrShift.class)`——实跑 checker 验证 daoFor 回落使 R2c actual 1383→1384 超基线（已知失败模式「Compliance 基线漂移」，project-context.md），I*Biz 可完全满足查找需求故按「I*Biz 优先」原则全程零 daoFor，checker actual==baseline 零漂移。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] Phase 1 交付可观察结果：回退分支代码落地（today 无可用记录 → 回退探查昨日跨天排班 → 命中写 clockOut / 未命中抛 `ERR_NOT_CLOCKED_IN`），`mvn compile -pl module-hr/erp-hr-service` 类型检查通过（行为正确性由 Phase 2 测试证明）
- [x] 无 ORM/契约变更（本阶段产物仅 Java 代码）

### Phase 2 - dedicated 测试

Status: completed
Targets: `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrAttendanceEngine.java`（扩展）或新建 `TestErpHrAttendanceCrossDayClockOut.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` 测试矩阵：① 跨天命中——seed `ErpHrShift`（NIGHT，startTime 23:00 / endTime 08:00）+ `ErpHrShiftAssignment`（employeeId + assignmentDate=yesterday）+ `ErpHrAttendance`（date=yesterday + clockIn=yesterday 23:00）→ `clockOut` 今日 → 断言返回记录 date==yesterday + clockOut 非空 + workHours 断言方式（**注意**：`HrFrozenClockExtension` 仅冻结日期，`CoreMetrics.currentTimestamp()` 走真实系统毫秒——clockOut 实际写入 ≈ 当前真实时刻（2026-08-08 附近），非 2026-07-17T08:00。故 workHours 的**绝对 9h 断言不可行**（Duration 实为 clockIn(2026-07-16T23:00) → now(2026-08-08) ≈ 546h）。可行断言（采用）：seed `clockIn = LocalDateTime.now().minusHours(9)`（记录 date 仍 = REFERENCE_DATE.minusDays(1)）→ clockOut → 断言 `workHours == 9.00`（Duration 精确 9h）**或**断言 `workHours == Duration.between(返回记录clockIn, 返回记录clockOut).toMinutes()/60` 一致性（实现公式镜像，鲁棒不依赖具体时刻）；② 今日已有记录优先——seed 今日 attendance(clockIn 非空) + 昨日跨天记录 → clockOut → 断言今日记录被更新（不回退）；③ 昨日无跨天排班——seed 昨日记录(clockIn 非空) 无排班 → clockOut 抛 `ERR_NOT_CLOCKED_IN`；④ 昨日记录 clockIn 空 → 抛 `ERR_NOT_CLOCKED_IN`；⑤ 常规路径回归（无昨日数据 → 行为不变）。
      - Skill: `nop-testing`
- [x] `Proof` 断言强度：返回记录 date/clockOut/workHours 精确值 + 异常错误码；`@NopTestConfig` 隔离零外部依赖（镜像 `TestErpHrAttendanceEngine` 既有 `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)` 范式 + `HrFrozenClockExtension` 冻结时钟）。昨日记日期用 `HrFrozenClockExtension.REFERENCE_DATE.minusDays(1)` 显式构造（不硬编码），避免时钟推进耦合。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 5 组测试全部落地并绿：`mvn test -pl module-hr/erp-hr-service` 全绿（既有 tests 零回归）
- [x] 覆盖回退命中 + 未命中 + 优先级三分支（无死代码分支）

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/human-resource/shift-scheduling.md`（§4.2 或 §九 补实现注记，可选）；`docs/audits/arm-index.md`（P1-RC-013 修复状态）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.6 done）；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [x] `Add` owner doc 补注：`shift-scheduling.md §4.2` 无条件补「clocking 侧跨天签退回退」实现注记（今日无记录回退昨日跨天排班记录）；不修改需求契约段（真相源冻结条款遵守）。
      - Skill: none
- [x] `Add` arm-index P1-RC-013 行「修复状态」→ `done (RC-R1.6)` + 修复落地摘要（跨天回退 + 排班校验 + 测试矩阵）；roadmap RC-R1.6 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 补注落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_02213a874ffe6IzT6FfhNh13gt`，fresh session）——1 MAJOR（test① workHours ≈9h 绝对断言不可行：冻结时钟仅冻结日期、clockOut 写真实系统时刻 → seed clockIn=now-9h + 镜像公式断言替代）+ 2 MINOR（Phase 1 项 2-3 已确认 P1 缺陷应标 `Fix` 而非 `Add`；Phase 1 Exit 断言行为属 Phase 2 证明范围 → 改为本地化交付检查）+ 2 minor（「如实现形态非纯内部逻辑/（如适用）」条件化措辞 → 无条件 §4.2 注记；helper 可复用既有 `IErpHrShiftAssignmentBiz.findByEmployeeAndDate:52-55`）→ 全量修订。
- Independent draft review iteration 2: `accept`（本次独立草案审查，mission-driver 2026-08-07-181210 会话）——全量基线主张实仓核验通过（`ClockOutProcessor:19-29` 按 eq(date) 精确查找无跨天回退 / `AbstractErpHrAttendanceProcessor.findAttendance:32-38` / `ShiftAttendanceCalculator.isCrossDayShift:24-28` + `calcEarlyLeaveMinutes:61-74` 夜班 endTime 取次日 / `IErpHrShiftAssignmentBiz.findByEmployeeAndDate` 接口 `:53-55` + BizModel 实现 `:97-107`[active 状态过滤] / `ErpHrShiftCalcAttendanceProcessor:24-32` 同域 Processor 注入 assignmentBiz + `assignment.getShift()` 复用范式 / `AbstractErpHrShiftProcessor:34-35` @Inject 非 private 先例 / `ThreadLocalFrozenClock:65-67` 仅冻结日期 currentTimeMillis 走真实毫秒——test① now-9h 构造成立 / `HrFrozenClockExtension.REFERENCE_DATE=2026-07-17` / `TestErpHrAttendanceEngine` @NopTestConfig 范式 / arm-index `:152` P1-RC-013 行 + roadmap `:374` RC-R1.6 todo + expander `:95` §3.1 映射 / `TestErpHrShiftScheduling:304-306` NIGHT 23:00-08:00 seed 范式 / L2 `shift-scheduling.md §4.2:200-212` + §九.6 跨天归属），0 BLOCKER 0 MAJOR；1 MINOR 修订：Phase 1 Decision 回退判据补孤儿 assignment（shift 关联空 → 保守不命中回退）防 `isCrossDayShift(null)` NPE（对齐 calc 侧 null shift 跳过范式）；行号微漂（`IErpHrShiftAssignmentBiz.java:52-55` 实为 `:53-55`）留待执行期自然对齐。共识达成，转 active。

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-hr/erp-hr-service` + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 方案 B：`originAssignmentDate` ORM 列

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 触 §5 ask-first 越出第一批纯预授权边界；方案 A 已覆盖主路径（夜班跨天签退回退），ORM 列仅显式化跨天归属记录，属深化增强。
- Successor Required: yes（触发条件 = 第二批启动或人工裁决 ORM 授权新增 originAssignmentDate 列）

### 昨日记录自动补建（跨天签退时昨日无 attendance 记录）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 跨天签退语义 = 作用于已签到记录；昨日无记录说明昨日未签到（缺勤/漏打卡），自动补建会掩盖缺勤判定，与 §4.1 缺勤规则冲突；漏打卡补救归 RC-R1.7 补卡范畴。
- Successor Required: no（watch-only）

## Closure

Status Note: 已完成。3 Phase 全 done（Phase 1 回退分支 + I*Biz 优先排班校验 helper；Phase 2 dedicated 测试 5 组 + `_cases/` 快照；Phase 3 文档回填）。验证：`mvn test -pl module-hr/erp-hr-service` 140 tests 全绿（既有 135 零回归）+ 全量 `mvn clean install -DskipTests` BUILD SUCCESS + 全仓 `mvn test` BUILD SUCCESS + compliance checker actual==baseline 零漂移（R1d=14/R2a=34/R2b=229/R2c=1383/R2d=34/R3=5/R5=0/R6=2/R10=7/R12a=69/R12b=66/R12c=40 与 §BASELINE 逐行一致——执行期决策：shift 回落改 `IErpHrShiftBiz.get` 全程零 daoFor，防 R2c 1383→1384 基线漂移，已记录于 Phase 1 项 3 偏离注记）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计（新会话子代理 `ses_0210da14bffePqZuAsWECCPmDa`）Verdict **pass**——独立重跑三项验证门控（erp-hr-service 140/140 绿 + 全量构建 BUILD SUCCESS + checker 19 规则 actual==baseline 零漂移）而非信任记录证据；Phase 一致性/代码实现（today 优先 → 昨日回退探针 → `hasCrossDayAssignment` I*Biz 链 + 孤儿保守不命中 → `ERR_NOT_CLOCKED_IN`）/5 组测试 + 快照/文档回填/零 ORM 变更全部核验通过；0 BLOCKER 0 MAJOR 0 MINOR + 2 cosmetic（日志验证措辞已修正；Closure 段待审计后回填——本段即回填结果）。

Follow-up:

- 无（范围内项目全落地后关闭；方案 B successor 触发条件见 Deferred But Adjudicated）
