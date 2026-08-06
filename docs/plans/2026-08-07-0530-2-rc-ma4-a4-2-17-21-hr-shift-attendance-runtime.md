# 2026-08-07-0530-2 rc-ma4-a4-2-17-21-hr-shift-attendance-runtime HR 排班/考勤域异常路径运行时影响面验证

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.17 / A4.2.18 / A4.2.19 / A4.2.20 / A4.2.21
> Related: `docs/audits/2026-08-02-2344-rc-ma1-a1-13-hr-f2-shift-attendance.md`（A1.13 MA1 报告 §7 存疑点 SP-1..5 + §6 四项 P1-RC-011/012/013/014）、`docs/plans/2026-08-02-2250-2-rc-ma1-a1-13-hr-f2-shift-attendance.md`（A1.13 计划）
> Audit: required

## Current Baseline

A1.13（HR-F2 排班与考勤）MA1 报告 §7 列出 5 个静态存疑点（SP-1..SP-5），对应 §6 新登记的 4 项 P1 finding（P1-RC-011/012/013/014）+ 1 项 UC-HR-09⑲换班跨日期语义存疑。A1.13 §5 裁决：UC-HR-02 = P1（⑦审批人超时自动转派缺失）、UC-HR-06 = P1（⑬多次打卡 reject 行为偏离 + ⑭⑮异常路径未实现）、UC-HR-09 = 接受（7 验收标准全 PASS）。

这 5 项存疑点的共性：均涉及异常路径/边界行为的**运行时实际影响面**（运营频度、是否有替代运维流程、是否影响下游薪酬核算）。MA1 静态已确认代码缺陷存在，MA4 须确认运行时影响面以指导 MR1 修复优先级（不改变 P1 分级——Q4 裁决 P1 强制实现，但运行时证据指导优先级排序与降级证据）。

- **A4.2.17（SP-1 P1-RC-011 审批人超时自动转派缺失运行时影响面）**：`ErpHrLeaveRequestBizModel.resolveApproverId:203-206` return null + 全 module-hr 无 timeout/escalat/reassign 业务代码 + `scheduler.yaml` 无 leave-approver-timeout job。待确认：SUBMITTED 休假长期未审批的运行时累积量是否影响薪酬核算（UC-HR-04 缺勤数据来源）。
- **A4.2.18（SP-2 P1-RC-012 多次打卡 reject 运行时误判面）**：`ErpHrAttendanceClockInProcessor:32-35` 首次签到后拒绝重复打卡（实现与 L1「多次打卡以最后一次为准」相反）。待确认：员工误触多次 clockIn 被拒后的逃生路径（HR 手工 DB 修正频度）、是否存在考勤申诉工单。
- **A4.2.19（SP-3 P1-RC-013 夜班跨天 clockOut 运行时阻断）**：`ErpHrAttendanceClockOutProcessor:19-20` 按 clockOut 当日 `findAttendance` 查找，夜班跨天签退查不到记录抛 `ERR_NOT_CLOCKED_IN`（calc 侧跨天已实现，clocking 侧未实现）。待确认：夜班员工实际 clockOut 失败率、是否普遍存在「夜班次日补录」临时运维流程。
- **A4.2.20（SP-4 P1-RC-014 设备故障补卡运行时替代）**：全 module-hr grep `makeUp/manualClock/补卡/supplement/adjustClock` 零命中，`ErpHrAttendanceBizModel` 仅 clockIn/clockOut/getTodayAttendance 三方法，无手工补卡 mutation。待确认：设备故障时 HR 是否经标准 CRUD 直接 update `ErpHrAttendance.clockIn/clockOut` 绕过（CrudBizModel 默认 save/update 不受字段级守卫保护），存在越权风险。
- **A4.2.21（SP-5 UC-HR-09⑲换班跨日期语义）**：`ErpHrShiftSwapRequestSubmitProcessor` 未校验 source/target assignment 同日期，跨日期换班的运行时语义（empA 7/1 早班 ↔ empB 7/2 中班 → 交换后语义可疑）。L1 未显式要求同日期校验。待确认：是否需补同日期守卫（运行时是否产生困惑数据）。

剩余差距：上述五项均为只读运行时影响面确认（运营频度/替代路径/越权风险探查），无生产代码变更。P1 finding（P1-RC-011/012/013/014）的修复义务归 MR1，本计划仅确认运行时影响面以指导优先级，不改变 P1 强制实现义务（Q4 裁决）。

## Goals

- 对 A4.2.17-A4.2.21 五项存疑点产出运行时影响面证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：维持 P1（不降级，Q4 强制实现）+ 记录降级证据（若有替代运维流程/低频边界），或升级（若运行时发现活跃数据破坏则触发 MR0）。UC-HR-09⑲换班跨日期确认是否需登记 watch-only。
- 完成后回写 roadmap A4.2.17-A4.2.21 `todo → done`，并按裁决更新 arm-index。

## Non-Goals

- 不实现 P1-RC-011/012/013/014 的修复（审批人超时转派/多次打卡 last-wins/跨天 clockOut/手工补卡），修复义务归 MR1 R1.0 展开器。
- 不修改任何真相源。
- 不复跑 MA2 状态机审计或 A4.4 代码质量审计。
- 不改变 P1 finding 的强制实现义务（Q4 裁决=(a) 无例外）——本计划仅确认运行时影响面。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-02-2344-rc-ma1-a1-13-hr-f2-shift-attendance.md` §5/§6/§7 + `docs/design/human-resource/`（shift-scheduling.md / state-machine.md）+ `docs/design/flow-overview.md`（HR→薪酬下游耦合）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划为只读审计，无代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为代码可达性 + 运营影响面分析（grep census / 代码路径核验 / 下游耦合分析），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时影响面证据采集与验证报告撰写（A4.2.17-A4.2.21）

Status: completed
Targets: `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done ✓；A1.13 done ✓

- [x] **A4.2.17 审批人超时转派缺失运行时影响面**：确认 SUBMITTED 休假状态无超时自动转派（`resolveApproverId:203-206` return null + scheduler.yaml 无 leave-timeout job，A1.13 §6 已静态证实）；分析 SUBMITTED 长期悬挂是否影响 UC-HR-04 薪酬核算缺勤数据来源（`PayrollCalculator` 是否读 LeaveRequest 状态 → 休假审批悬挂是否致缺勤误判/工时偏差）；评估运营影响面（休假审批悬挂的累积频度，是否阻塞发薪）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.18 多次打卡 reject 运行时误判面**：确认 `ClockInProcessor:32-35` reject 行为（A1.13 §6 已静态证实）+ `TestErpHrAttendanceEngine#testDuplicateClockInBlocked:69-78` 测试与实现同步偏离 L1；分析员工误触多次 clockIn 被拒后的逃生路径（HR 是否经标准 CRUD 直接 update `ErpHrAttendance.clockIn` 绕过 reject）；评估 reject 行为对考勤数据完整性的实际运营影响。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.19 夜班跨天 clockOut 运行时阻断**：确认 `ClockOutProcessor:19-20` 按 clockOut 当日查找查不到夜班跨天记录抛 `ERR_NOT_CLOCKED_IN`（calc 侧 `ShiftAttendanceCalculator.isCrossDayShift:24-28` 已实现跨天，clocking 侧未实现）；分析夜班员工签退失败的运营影响（是否普遍存在「次日补录/手工 DB 修正」临时流程）；评估是否阻塞考勤结算。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.20 设备故障补卡运行时替代与越权风险**：确认无 `makeUp/manualClock/补卡` mutation（A1.13 §6 已静态证实）；分析 HR 是否经标准 CRUD（`ErpHrAttendanceBizModel` 继承 CrudBizModel 的 save/update）直接修改 `clockIn/clockOut` 字段绕过字段级守卫——CrudBizModel 默认 save/update 是否受字段级守卫保护（RBAC + XMeta 字段权限）；评估越权风险（普通员工是否也能经同一 CRUD 改自己打卡）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.21 换班跨日期语义运行时确认**：确认 `ErpHrShiftSwapRequestSubmitProcessor` 未校验 source/target assignment 同日期（A1.13 §7 SP-5 已静态确认）；分析跨日期换班的运行时数据后果（交换后 assignment 归属是否产生困惑数据）；L1 未显式要求同日期校验 → 按 §2 判据裁决是否登记 watch-only（决策树：跨日期换班是否致活跃数据破坏 → 否则 watch-only 不升 P1）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：五项存疑点各出 §裁决（维持 P1 + 降级证据记录 / 登记 watch-only / 触发 MR0）+ §与既有 finding 衔接（P1-RC-011/012/013/014 / P2-MA2-052 交叉引用）+ §过程纪律自检（checker 退出码门控——无代码变更 actual=baseline；closure-audit 独立性声明）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。

- [x] 验证报告落盘 `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`，含五项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [x] 每项裁决明确：维持 P1（Q4 强制实现，不降级）+ 降级证据记录，或升级触发 MR0；UC-HR-09⑲ watch-only（若有）已按 §去重协议裁决

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.17-21 done）、`docs/audits/arm-index.md`（若有新 watch-only）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：P1-RC-011/012/013/014 维持 P1（运行时影响面确认不撤销修复义务，降级证据记录指导 MR1 优先级）；A4.2.21 若裁决 watch-only 则按 §7 规则 grep arm-index 同域同控制点后裁决新建或复用。
- [x] `Add` roadmap A4.2.17-A4.2.21 `todo → done`；`docs/logs/2026/08-07.md` 追加完成条目。

Exit Criteria:

- [x] roadmap 五项状态已更新为 done 且与报告裁决一致
- [x] arm-index 无未经比对直接新建的 finding

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is (ses_028439cbfffepRTau1f2dOXB07) — no blocking issues. Scope (one result surface bundling A4.2.17-21 per rule 14), baseline honesty (file:line anchors verified against live repo), Deps satisfied (A4.2 done ✓ / A1.13 done ✓), Q4 honored (maintains P1 without downgrade), Closure Gates non-placeholder all confirmed. Non-blocking suggestions applied for consistency: Phase 2 `Item Types` corrected to `Decision | Add` with per-item prefixes. Consensus reached → flipped to active.

## Closure Gates

> 本计划为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更）。closure 时确认 checker 未触发 actual > baseline。

- [x] 范围内行为完成（五项存疑点均有 file:line 运行时影响面证据 + 明确裁决）
- [x] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [x] 已运行验证（checker actual=baseline 确认；无代码变更故无 build/test 回归风险）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-011/012/013/014 修复实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅确认运行时影响面；四项 P1 的修复（审批人超时转派/多次打卡 last-wins/跨天 clockOut/手工补卡）归 MR1 R1.0 展开器，Q4 裁决 P1 强制实现。本审计维持 P1 不撤销，降级证据记录指导 MR1 优先级排序。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接）

## Closure

Status Note: 五项存疑点（A4.2.17-A4.2.21）运行时影响面证据链已全数闭合——只读审计零生产代码变更，验证报告落盘 `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`（`> Audit Status: closed`，9 段齐全），roadmap A4.2.17-A4.2.21 全 `done ✅`，arm-index `:150-153` P1-RC-011/012/013/014 运行时影响面注记已追加（维持 P1 不撤销，0 新 finding）。四项 P1 维持既有 arm-index 行（Q4 强制实现义务不撤销，降级证据指导 MR1 优先级），A4.2.21 watch-only residual 记报告非 arm-index 行（归同型范式 A4.2.5）。不触发 MR0（五项均无 P0）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文）
- Evidence: 独立子代理会话逐项核实（live repo 实测，非信任 [x] 标记）：
  - 验证报告 `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md` 存在且 `> Audit Status: closed`，§0 TL;DR 含五项裁决（A4.2.17 维持 P1-RC-011 / A4.2.18 维持 P1-RC-012 / A4.2.19 维持 P1-RC-013 / A4.2.20 维持 P1-RC-014 + 越权风险代码层证实 / A4.2.21 watch-only residual）+ §2 逐项 file:line 证据链 + §8 checker actual==baseline（0 漂移：R1d 14/14、R2a 34/34、R2b 229/229、R2c 1382/1382、R3 5/5）
  - `docs/backlog/requirement-compliance-roadmap.md:170-174` A4.2.17/A4.2.18/A4.2.19/A4.2.20/A4.2.21 状态列均为 `done ✅` 且裁决注记与报告一致
  - `docs/audits/arm-index.md:150-153` P1-RC-011/012/013/014 行 `修复` 列已追加 `【MA4 A4.2.17/18/19/20 运行时影响面确认（2026-08-07）】` 注记（维持 P1 不撤销，无新 finding 行）
  - `docs/logs/2026/08-07.md:1-21` 含本切片完成条目（裁决摘要 + 报告路径 + 0 新 finding / 不触发 MR0 / 不归 MR1）
  - Anti-Hollow 自检：本计划为只读审计，零生产代码变更（git status 仅 .md），无空函数体 / 无 swallowed exception / 无 return null 占位（验证对象代码 grep 零命中即结论，非占位）
  - 五点一致性：Plan Status `completed` / Phase 1 `completed` + Exit Criteria `[x]` / Phase 2 `completed` + Exit Criteria `[x]` / Closure Gates 全 `[x]` / 日志条目均一致
  - Deferred honesty：`Deferred But Adjudicated` 仅含 P1-RC-011/012/013/014 修复（plan Non-Goals 明确归 MR1，非范围内项目降级），无范围内缺陷隐藏
