# 2026-08-08-0424-1-rc-mr1-r1-5-hr-attendance-last-wins RC-R1.5 — hr 考勤多次打卡 last-wins（P1-RC-012，MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.5（MR1 第一批纯预授权：hr 考勤多次打卡 last-wins，P1-RC-012）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.5 行 + `docs/audits/arm-index.md` P1-RC-012 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3.1（RC-R1.5 = 「纯 BizModel 代码逻辑修复 + 测试调整」）
> Related: `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`（A4.2.18 运行时影响面：reject 守卫 CONFIRMED + 逃生路径 = 标准 CRUD）；`docs/design/human-resource/use-cases.md`（L1 UC-HR-06⑬）；`docs/plans/2026-08-07-2340-3-rc-mr1-r1-4-hr-leave-approver-timeout.md`（hr 域 MR1 计划范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-012（arm-index 行）**：UC-HR-06⑬「多次打卡以最后一次为准」实现**相反**（reject）。L1 `use-cases.md:72` 异常段逐字「多次打卡以最后一次为准」；L3 `ErpHrAttendanceClockInProcessor.clockIn:32-35` `if (attendance.getClockIn() != null) throw new NopException(ERR_ALREADY_CLOCKED_IN)`——首次签到后拒绝重复打卡。
- **测试与实现同步偏离 L1**：`TestErpHrAttendanceEngine#testDuplicateClockInBlocked:69-78` 断言第二次 clockIn 抛 `ERR_ALREADY_CLOCKED_IN`；E2E `tests/e2e/business-actions/hr-leave-attendance.action.spec.ts:182-185` 同样断言 reject（「已签到」token）。
- **A4.2.18 运行时确认（`2026-08-07-0530` 报告）**：reject 守卫 HEAD 复核 CONFIRMED（`ClockInProcessor:32-35`）；逃生路径 = `ErpHrAttendanceBizModel extends CrudBizModel` 标准 save/update 可绕过 reject 守卫；无正式申诉工单。**维持 P1 不撤销**（行为与 L1 字面相反，Q4 强制实现）。
- **涉及文件**：`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrAttendanceClockInProcessor.java`（reject 守卫）；`module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrAttendanceBiz.java:17`（javadoc「若当日已签到则抛 ERR_ALREADY_CLOCKED_IN」同步偏离）；`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrAttendanceEngine.java`；`tests/e2e/business-actions/hr-leave-attendance.action.spec.ts`。
- **workHours 语义**：`AbstractErpHrAttendanceProcessor.computeWorkHours:44-50`（clockIn→clockOut 分钟差 /60 HALF_UP）；clockIn 覆盖后若 clockOut 已存在须重算 workHours（roadmap 行要求「覆盖原值 + 重算 workHours」）。
- **预授权判据**（第一批纯预授权）：纯 BizModel 代码逻辑修复 + 测试调整，不触 ORM/会计核心/删除；**无 ask-first checkbox**。roadmap RC-R1.5 行 `todo`，Deps（R1.0 done）已满足。

## Goals

- `ErpHrAttendanceClockInProcessor.clockIn` 移除 `ERR_ALREADY_CLOCKED_IN` reject 守卫：重复 clockIn 时**覆盖原值**（last-wins，`clockIn = CoreMetrics.currentTimestamp()`）；若 clockOut 已存在则**重算 workHours**（`computeWorkHours(新clockIn, clockOut)`）。
- 同步修正 `IErpHrAttendanceBiz` clockIn javadoc（不再承诺「若当日已签到则抛 ERR_ALREADY_CLOCKED_IN」，改为 last-wins 语义）。
- 测试调整：`TestErpHrAttendanceEngine#testDuplicateClockInBlocked` → last-wins 断言（第二次 clockIn 后 clockIn 时间戳为后值 + workHours 重算 + 不抛异常）。
- E2E 调整：`hr-leave-attendance.action.spec.ts` 重复 clockIn 断言由 reject → last-wins 覆盖。
- 回填 arm-index P1-RC-012 → `done (RC-R1.5)` + roadmap RC-R1.5 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM / api.xml / 数据字典**（零结构变更；`source` 保持原值 CARD 不被覆盖——重复打卡不改 source，仅覆盖 clockIn 时间戳）。
- **不改真相源**（use-cases.md 需求契约段；「以最后一次为准」是 L1 原文，无需修订）。
- **不做考勤申诉工单机制**（A4.2.18 已登记 watch-only 无正式申诉工单；本 finding 修复面 = last-wins 语义，申诉机制属未来增强）。
- **不动 `ERR_ALREADY_CLOCKED_IN` 的删除决策单独成行**——见 Execution Plan Decision（保留 vs 删除由执行期裁决，但删除与否都不影响行为修复）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/human-resource/use-cases.md`（L1 UC-HR-06⑬ 契约）+ `docs/design/human-resource/shift-scheduling.md`（roadmap RC-R1.5 行 owner doc；打卡/迟到早退判定基准，不持有 last-wins 契约段）+ `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-...md`（A4.2.18 运行时证据）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）
- Skill Selection Basis: 实现面 = 单文件 Processor 逻辑修复 + 测试调整（`nop-backend-dev`：last-wins 覆盖语义、workHours 重算复用既有 helper、不引入新 API）+ JUnit/E2E 测试调整（`nop-testing`：JunitAutoTestCase 断言改型、E2E spec 断言改型；无快照重录——既有 `_cases/` 若含 reject 快照须核验）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新增 infra/config（纯行为修复）。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-hr/erp-hr-service`。

## Execution Plan

### Phase 1 - ClockInProcessor last-wins + IBiz javadoc 修正

Status: completed
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrAttendanceClockInProcessor.java`；`module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrAttendanceBiz.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [x] `Fix` `ErpHrAttendanceClockInProcessor.clockIn`：移除 `:32-35` reject 守卫；重复 clockIn 时 `attendance.setClockIn(CoreMetrics.currentTimestamp())` 覆盖原值；若 `attendance.getClockOut() != null` 则重算 workHours（按 `ClockOutProcessor:26` 同模式 `.toLocalDateTime()` 转换后调 `computeWorkHours(新clockIn, clockOut)`）；`saveOrUpdateAttendance` 走既有路径。已落地（`ErpHrAttendanceClockInProcessor.java:30-35`）：守卫删除、覆盖 + 条件重算 + 清理 NopException/ErpHrErrors 死 import；首次 clockIn 行为不变（clockOut 为空时不预写 workHours）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `IErpHrAttendanceBiz.clockIn` javadoc：删「若当日已签到则抛 ERR_ALREADY_CLOCKED_IN」，改「重复签到以最后一次为准（last-wins），覆盖 clockIn 并重算 workHours」。已落地（`IErpHrAttendanceBiz.java:15-18`）：「重复签到以最后一次为准（last-wins），覆盖 clockIn；若 clockOut 已存在则重算 workHours」。
      - Skill: `nop-backend-dev`
- [x] `Decision` `ERR_ALREADY_CLOCKED_IN` 处置：**选项 A（选定）＝保留 ErrorCode 定义**。备选：选项 B = 删除。理由：框架约定 ErrorCode 可冗余；删除会引入 api/compat 面 churn（ErrorCode 定义于 `ErpHrErrors.java`，无独立版本化），且 E2E 迁移期仍可能引用（本计划已同步清理 E2E，但未来其它调用方/文档引用无法枚举）。**残留风险记录**：保留后该 ErrorCode 在当前 HEAD 零生产代码引用（grep 证实仅定义本身 `ErpHrErrors.java:240-243`），若未来重复打卡语义恢复或新场景需要可直接复用；删除与否都不影响行为修复（Non-Goals 已声明）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `clockIn` 重复调用后 clockIn 为后值（last-wins）+ 不抛异常；clockOut 已存在时 workHours 按新 clockIn 重算（成功模式——Phase 2 `testDuplicateClockInLastWins` + `testDuplicateClockInRecomputesWorkHours` 运行时证实）；首次 clockIn 行为不变（失败模式：无——行为单调收敛；`testClockInClockOutComputesWorkHours` 无回归）
- [x] IBiz javadoc 与实现语义一致（无契约漂移残留——grep `ERR_ALREADY_CLOCKED_IN` 于 IBiz javadoc 零命中）

### Phase 2 - 测试 + E2E 调整

Status: completed
Targets: `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrAttendanceEngine.java`；`tests/e2e/business-actions/hr-leave-attendance.action.spec.ts`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 完成

- [x] `Fix` `TestErpHrAttendanceEngine#testDuplicateClockInBlocked:69-78` → 改名 `testDuplicateClockInLastWins`：第二次 clockIn 后断言不抛异常 + `getTodayAttendance().getClockIn()` == 第二次调用返回的 clockIn + 与第一次 clockIn 不等（frozen clock 冻结下两次调用毫秒级不同，断言后值 > 前值）。已落地（`TestErpHrAttendanceEngine.java:75-88`）：`assertNotNull` + `Timestamp.after` 后值断言 + `getTodayAttendance` 持久化值 == 第二次返回值。
      - Skill: `nop-testing`
- [x] `Proof` 新增 workHours 重算断言（**确定性构造，不用「毫秒差」依赖**）：已落地（`TestErpHrAttendanceEngine.java:90-116` `testDuplicateClockInRecomputesWorkHours`）——DAO seed 路径直接写 `clockIn = now−4h`（显式过去）+ `clockOut = now+3h`（显式未来，均不经 clockIn/clockOut mutation；seed 日期用 `HrFrozenClockExtension.REFERENCE_DATE` 与 CoreMetrics.today() 冻结值一致）→ 调 `clockIn` mutation（last-wins 覆盖，clockIn=now）→ 断言返回行 workHours == 镜像公式 `Duration.between(返回clockIn, seededClockOut).toMinutes()/60 HALF_UP`（与 `computeWorkHours` 实现公式精确一致，秒级余量内确定成立）+ 断言 workHours != 覆盖前值（覆盖前 = 旧 clockIn[now−4h] 镜像计算 ≈7h vs 覆盖后 ≈3h，确定性不同——比计划原文「两次 clockIn 同分钟毫秒截断」更稳：seed 同时固定旧 clockIn，消除同分钟竞态）。
      - Skill: `nop-testing`
- [x] `Fix` E2E `hr-leave-attendance.action.spec.ts:182-185`：重复 clockIn 断言由 reject → last-wins（第二次 clockIn 成功返回 + `__get` 验证 clockIn 为后值）；同步更新 E2E 头注释 `:30`（「重复 clockIn 抛 ERR_ALREADY_CLOCKED_IN」→ last-wins）与测试标题 `:155`（「repeat clockIn guard」→「repeat clockIn last-wins」）。已落地 + 运行时执行 4 passed。**执行期发现**：应用 GraphQL 时间戳序列化为秒级精度（未设 `nop.graphql.ignore-millis-in-timestamp=false`），两次调用同秒时后值==前值致断言 flaky → 重复调用前加 `page.waitForTimeout(1100)` 跨秒边界（确定性），代码注释记录原因。
      - Skill: `nop-testing`
- [x] `Proof` 既有 `_cases/` 快照处理（**必做，非可跳过**）：实仓核验 `_cases/app/erp/hr/service/TestErpHrAttendanceEngine/testDuplicateClockInBlocked/` 目录存在（测试改名后成孤儿）→ 删除该目录（reject 路径不再存在；改名方法走 RECORDING 重录新目录）。已落地：目录删除 + `testDuplicateClockInLastWins`/`testDuplicateClockInRecomputesWorkHours` 两新目录 RECORDING 录制（input/output tables + autotest.yaml 齐全）；既有 3 方法重录后与 git 基线逐字节一致零 diff。同时更新 `TestErpHrAttendanceEngine` 类 javadoc `:31-32`（「场景2：重复打卡拦截」→ last-wins 语义，已落地）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] `mvn test -pl module-hr/erp-hr-service` 全绿（既有测试零回归 + 新 last-wins 断言绿）——**135 tests 全绿**（既有 134 基线含 TestErpHrLeaveApproverTimeoutJob 7 组零回归，net +1）；E2E spec 运行时验证在 Closure Gates（已执行：4 passed）
- [x] 无残留 reject 语义断言（grep `testDuplicateClockInBlocked|已签到|重复 clockIn 抛|repeat clockIn guard` 于 hr 测试/E2E/类 javadoc 零命中，除 ErrorCode 定义本身——实测仅 `ErpHrErrors.java:242` 定义描述命中，符合豁免）

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/audits/arm-index.md`（P1-RC-012 修复状态）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.5 done）；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [x] `Add` arm-index P1-RC-012 行「修复状态」→ `done (RC-R1.5)` + 修复落地摘要（last-wins 覆盖 + workHours 重算 + 测试/E2E 调整 + ErrorCode 保留说明）；roadmap RC-R1.5 → done + 文件头最后更新注记；`docs/logs/2026/08-08.md` 日志条目（含 nop-ioc refactor 环境回归注记 + E2E 秒级精度发现 + hr-shift-rotation 既有漂移记录）。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + 日志条目写入（实仓核验：arm-index `:151` 行 done 注记 + roadmap `:373` 行 done ✅ + 文件头注记 + 日志顶部条目）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_02213c5ecffeODiR7uE7W0J7jZ`，fresh session）——0 阻塞 + 2 MAJOR（M1：workHours 重算断言「与首次值不同」在同分钟毫秒截断下必然失败 + ThreadLocalFrozenClock 无时间推进 API → 改为 DAO-seed 显式未来 clockOut + 镜像公式断言；M2：E2E 行为变更仅语法校验，closure 须运行时执行被改 spec，对齐 R1.1 先例）+ 2 MINOR（E2E 头注释 :30/测试标题 :155/类 javadoc :31-32 陈旧注释未覆盖；Deferred 缺重新打开触发条件）+ 1 TRIVIAL（快照项措辞「若有」→ 必做，`testDuplicateClockInBlocked` 快照目录实仓存在）→ 全量修订。
- Independent draft review iteration 2: `accept`（本次独立草案审查，mission-driver 2026-08-07-181210 会话）——全量基线主张实仓核验通过（`ClockInProcessor:32-35` reject 守卫 / `IBiz:17` javadoc / `TestErpHrAttendanceEngine:69-78` + 类 javadoc :31 / E2E `:30,155,182-185` / 快照目录 `module-hr/erp-hr-service/_cases/.../testDuplicateClockInBlocked/` 实仓存在 / roadmap `:373` RC-R1.5 todo / arm-index `:151` / expander `:94` / `ThreadLocalFrozenClock:65-67` 仅冻结日期、currentTimeMillis 委托真实毫秒、无 advance API——Phase 2 DAO-seed 确定性构造成立 / R1.1 先例 `2026-08-07-1932-2` closure 运行时执行被改 E2E spec 对齐），0 BLOCKER 0 MAJOR；3 MINOR 修订：Task Route Owner Docs 补 roadmap 行 owner doc `shift-scheduling.md` / Phase 1 computeWorkHours 措辞补 `.toLocalDateTime()` 转换模式（对齐 `ClockOutProcessor:26`）/ 本迭代记录落盘。共识达成，转 active。

## Closure Gates

- [x] 范围内行为完成（last-wins 覆盖 + workHours 重算 + IBiz javadoc + 测试/E2E 调整 + arm-index/roadmap/日志回填全部落地）
- [x] 相关文档对齐（use-cases.md 契约段未改——实现向 L1「多次打卡以最后一次为准」收敛；arm-index/roadmap/日志已回填；bug note 记录既有漂移）
- [x] 已运行验证：`mvn test -pl module-hr/erp-hr-service` 135 tests 全绿 + 全仓 `mvn test`（19 域模块 + app-erp-all 全绿；中途受并行 agent 重装 refactored nop-ioc 干扰的运行已记录并复跑证实）+ `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（与 machine-readable 块逐行一致零漂移）+ **E2E spec 运行时执行**（`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/hr-leave-attendance.action.spec.ts` → 4 passed，app runner 重启后执行；hr-* 套件 35 passed + 1 既有漂移失败[`hr-shift-rotation` regenerate CANCELLED 断言 vs 后端逻辑删除，pre-existing 07-30 R1.28 引入，已记录 `docs/bugs/2026-08-08-0730-hr-shift-rotation-e2e-regenerate-cancelled-drift.md`，与 RC-R1.5 变更不相交]）
- [x] 无范围内项目降级为 deferred/follow-up（考勤申诉工单机制维持 watch-only residual——Deferred But Adjudicated 段，successor 触发条件已记录）
- [x] 独立草案审查已完成并记录（Draft Review Record ×2：iteration 1 needs revision + iteration 2 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（Plan Status 与各 Phase Status 同步；Exit Criteria 全 [x]；日志条目与 plan 一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（Closure 段已填充独立审计证据）

## Deferred But Adjudicated

### 考勤申诉工单机制（appeal/ticket）

- Classification: `watch-only residual`
- Why Not Blocking Closure: A4.2.18 已证无正式申诉工单（grep 零命中），且 L1 UC-HR-06 异常段仅要求「以最后一次为准」，未要求申诉通道；last-wins 修复后误触场景被覆盖语义吸收。
- Successor Required: no（watch-only；重新打开触发条件 = 运营调研发现申诉工单需求，届时新建独立工作项）

## Closure

Status Note: 执行完成 + 独立结束审计通过（draft → 独立草案审查 ×2 accept → active → 执行 → 独立结束审计子代理验收）。第一批纯预授权（无 ask-first）。实现向 L1「多次打卡以最后一次为准」收敛：`ErpHrAttendanceClockInProcessor.clockIn` 移除 ERR_ALREADY_CLOCKED_IN reject 守卫 → last-wins 覆盖 clockIn + clockOut 已存在时重算 workHours + IBiz javadoc 同步；测试 testDuplicateClockInBlocked→testDuplicateClockInLastWins + 新增 testDuplicateClockInRecomputesWorkHours（DAO-seed 确定性断言）；旧 reject 快照删除 + 新快照录制；E2E 断言 reject→last-wins（含秒级精度跨秒等待）。验证：erp-hr-service 135 tests 全绿 / 全仓 mvn test 全绿（env 干扰已排除并记录）/ 全量 mvn clean install -DskipTests 156 模块 BUILD SUCCESS / E2E spec 运行时 4 passed / checker actual==baseline 零漂移。arm-index P1-RC-012 → `done (RC-R1.5)` + roadmap RC-R1.5 → done ✅ + 日志条目落盘 + 既有漂移 bug note 登记。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver 2026-08-08-181210 会话，fresh session，不重用执行者上下文）
- Evidence: 实时仓库核验（本次审计会话）：① `ErpHrAttendanceClockInProcessor.java:30-35` 守卫已移除 + last-wins 覆盖 + clockOut 存在时条件重算 workHours；② `IErpHrAttendanceBiz.java:15-18` javadoc last-wins 语义（grep `ERR_ALREADY_CLOCKED_IN` 生产代码仅 `ErpHrErrors.java:240-243` 定义本身命中，符合 Decision A）；③ `TestErpHrAttendanceEngine` `testDuplicateClockInLastWins:75-88`（后值断言 + 持久化验证）+ `testDuplicateClockInRecomputesWorkHours:90-116`（DAO-seed 镜像公式断言）+ 类 javadoc last-wins；④ E2E `hr-leave-attendance.action.spec.ts:30/155/182-198` reject→last-wins（4 tests）；⑤ `_cases/` 旧 `testDuplicateClockInBlocked/` 目录已删 + 两个新目录 RECORDING 快照齐全（input/output/autotest.yaml）；⑥ arm-index:151 P1-RC-012 行 done (RC-R1.5) + roadmap:373 RC-R1.5 done ✅ + 文件头注记 + `docs/logs/2026/08-08.md` 顶部条目 + bug note `docs/bugs/2026-08-08-0730-...md` 存在。运行时验证（本次审计复跑）：`mvn test -pl module-hr/erp-hr-service` **135 tests 全绿 BUILD SUCCESS**（含 TestErpHrAttendanceEngine 5/5 + TestErpHrLeaveApproverTimeoutJob 7/7 零回归）——复跑前并行 agent 重装 refactored nop-ioc 致 7 类 NOP_SYS_SEQUENCE 环境故障（`05d03a1ac` 已知良好 nop-ioc 重建安装后消失），与 RC-R1.5 变更不相交（日志已记录同型先例）；E2E spec 运行时 4 passed + 全仓验证见 Closure Gates 执行期记录。

Follow-up:

- 无范围内 follow-up；hr-shift-rotation E2E regenerate CANCELLED 断言漂移为既有 pre-existing（07-30 R1.28 引入），已登记 `docs/bugs/2026-08-08-0730-hr-shift-rotation-e2e-regenerate-cancelled-drift.md`，修复归属建议随 hr 域后续 MR1 批次或 e2e-runbook 已知失败段登记
