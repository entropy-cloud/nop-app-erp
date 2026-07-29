# 2026-07-30-0341-1-r1-15-hr-state-machine-dict-dead-state hr 死状态 + dict/常量/字段对齐修复

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.15（P1-MA2-039/040/041/042/043/044/045/046/047，源自 A2.7a/A2.7b hr 状态机审查）
> Related: `docs/audits/2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`、`docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`、`docs/audits/2026-07-29-0430-arm-ma4-hr-code-quality.md`（P1-MA2-047 posted 死字段复核）、plan `2026-07-30-0143-3-r1-14-mfg-dict-dead-state-owner-doc-drift.md`（同型裁决先例）、plan R1.26（P1-MA4-017 计提/公司承担过账接线，与本计划 P1-MA2-047 posted 字段协同）
> Audit: required

## Current Baseline

九项 finding 经实仓逐项确认：均为「dict 死状态 / CRUD 桩 / 硬编码常量 / posted 死字段 / 排班无 dict 绑定」类型的 owner-doc 契约漂移，**不破坏已实现主路径**（员工 ACTIVE/PROBATION 在职 + transferEmployee 部门调动 + 合同主生命周期 + 请假/考勤/工资支付轴 + 仿真/换班主路径完整）。

**P1-MA2-039（员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死 + 离职/退休/转正迁移未实现）— 确认：**
- `ErpHrEmployeeBizModel.java` 仅有 `transferEmployee:89`（部门调动，不改 employmentStatus，保留 ACTIVE/PROBATION）；RESIGNED/TERMINATED/RETIRED 仅出现在 `nonTransferableStatuses():334-338` 只读守卫。
- 全模块 grep `setEmploymentStatus(EMPLOYMENT_RESIGNED|TERMINATED|RETIRED)` = 零 writer；无 resignEmployee/retireEmployee/terminateEmployee mutation。owner doc `state-machine.md §场景D/E` 声明离职/退休/试用期转正迁移未落地。

**P1-MA2-040（合同 SUSPENDED 死状态）+ P1-MA2-041（调查 OPEN/CLOSED/ARCHIVED 三态死 + 18 行 CRUD 桩）— 确认：**
- `ErpHrSurveyBizModel.java` = 18 行（CrudBizModel 桩，零状态机 mutation）。
- 合同 SUSPENDED dict 值存在、零 writer；owner doc 无合同/调查独立章节。

**P1-MA2-042（发展计划 DRAFT/CANCELLED + 计划项 OVERDUE 死状态 + 无 cancelPlan + 无 OVERDUE 自动 job）— 确认：** dict 值存在、零 setStatus writer（仅 APPROVED 等活态写入）。

**P1-MA2-043（工时单 APPROVED/REJECTED 死状态 + 仅 submit）— 确认：** `ErpHrTimesheetBizModel.java:38,43` 仅 submit（DRAFT→SUBMITTED），无 approve/reject。

**P1-MA2-044（工时单硬编码 "DRAFT"/"SUBMITTED" vs ErpHrConstants 不一致）— 确认：**
- `ErpHrTimesheetBizModel.java:38` `Objects.equals(timesheet.getStatus(), "DRAFT")`、`:43` `timesheet.setStatus("SUBMITTED")` — 硬编码字符串字面量。
- `ErpHrConstants.java` 有 `APPROVE_STATUS_*`/`SIMULATION_STATUS_*`/`ASSESSMENT_STATUS_*` 等常量，**无 TIMESHEET_STATUS_*** 常量。owner doc §场景 F 声明漂移。

**P1-MA2-045（银行付款文件 UPLOADED/CONFIRMED 死状态 + 18 行 CRUD 桩）— 确认：** `ErpHrPayrollBankFileBizModel.java` = 18 行（CrudBizModel 桩），UPLOADED/CONFIRMED dict 值零 writer。

**P1-MA2-046（排班分配 status 无 dict 绑定 [ORM ask-first]）— 确认：**
- `module-hr/model/app-erp-hr.orm.xml` ErpHrShiftAssignment `status` 列（propId=13）：`stdSqlType="VARCHAR" precision="50"`，**无 `ext:dict` 绑定**（raw VARCHAR），owner doc §二 声明漂移。

**P1-MA2-047（SalaryPostingDispatcher javadoc drift + ErpHrSalary.posted 死字段）— 确认：**
- `module-hr/model/app-erp-hr.orm.xml:758` ErpHrSalary `posted` 列存在（stdSqlType=BOOLEAN defaultValue=false）。
- 全 `module-hr/erp-hr-service/src/main/java` grep `setPosted` = 零 writer。SalaryPostingDispatcher 仅 tryPostPayment(SALARY_PAYMENT 280) 在 markPaid 触发；tryPostAccrual 死代码零调用方（与 R1.26/P1-MA4-017 协同）。

**保护区域：** P1-MA2-039 方案A（实现离职/退休 mutation）触及 nop-auth 用户禁用副作用属保护区域；P1-MA2-046 触及 ORM 模型（[ORM ask-first]）。本计划裁决 P1-MA2-039 为方案B（owner doc Deferred，不实现 mutation），故实际不触及 nop-auth 保护区域；P1-MA2-046 仅增 dict 绑定（不改列类型/不删列），按 roadmap §ORM 变更已授权 + 规则 8 走标准 plan-audit + closure-audit。

## Goals

- 消除 hr 域 owner doc 与代码间 9 项契约悬空：死状态/桩/硬编码/posted 死字段/排班无 dict 全部得到明确裁决（Deferred 标注或实现对齐）。
- 工时单硬编码字符串替换为 ErpHrConstants 常量（机械 Fix，P1-MA2-044）。
- 排班分配 status 列补 dict 绑定（ORM ask-first，P1-MA2-046）。
- owner doc（state-machine.md / payroll.md）与代码实际行为一致，无「dict 含值但无迁移/无 writer」的悬空。

## Non-Goals

- 不实现员工离职/退休/转正 mutation（P1-MA2-039 方案A 触及 nop-auth 保护区域，裁决为方案B Deferred，successor 命名触发条件）。
- 不充实 Survey/PayrollBankFile/DevelopmentPlan 的完整状态机业务逻辑（裁决 Deferred，CRUD 桩保留为主路径可用）。
- 不实现工时单 approve/reject 审批流、银行文件 UPLOADED/CONFIRMED 上传确认流（Deferred successor）。
- 不接线 hr 计提/公司承担社保/公积金过账链路——归 R1.26（P1-MA4-017）；本计划仅处理 posted 字段的 javadoc/drift 文档侧。
- 不从 ORM 删除任何死状态 dict 值（采纳「保留为预留 + 文档 Deferred」对齐 mfg R1.14 / finance R1.13 既有先例）。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐）+ 少量 `implementation-only change`（工时单常量替换、排班 dict 绑定）
- Owner Docs: `docs/design/human-resource/state-machine.md`、`docs/design/human-resource/payroll.md`
- Skill Selection Basis: P1-MA2-044 常量替换 + P1-MA2-046 ORM dict 绑定涉及 Java/ORM 编辑 → `Skill: nop-backend-dev`（用于确认 ErpHrConstants 扩展与 ORM ext:dict 绑定符合平台模式）；owner doc Deferred 标注为纯文档 → `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 九项 finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：九项 finding 处置方案逐项裁决（同型裁决范式，对齐 R1.13/R1.14 先例）。
      - P1-MA2-039 员工 RESIGNED/TERMINATED/RETIRED：**方案B（owner doc Deferred）**。理由：方案A 触及 nop-auth 用户禁用副作用属保护区域 + 离职/退休/转正是低频 HR 场景 + 字段 employmentStatus/probationEndDate 已承载状态数据可由 HR 主动维护；采纳 Deferred 标注 + successor（PM 要求正式离职/退休工作流时实现 resignEmployee/retireEmployee mutation + nop-auth 联动）。
      - P1-MA2-040 合同 SUSPENDED：方案B（owner doc Deferred）。
      - P1-MA2-041 调查 OPEN/CLOSED/ARCHIVED + CRUD 桩：方案B（owner doc Deferred；CRUD 桩保留主路径可用）。
      - P1-MA2-042 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE：方案B（owner doc Deferred；OVERDUE 自动 job 为增强 successor）。
      - P1-MA2-043 工时单 APPROVED/REJECTED：方案B（owner doc Deferred；仅 submit 是本期已落地行为）。
      - P1-MA2-044 工时单硬编码 "DRAFT"/"SUBMITTED"：**方案A（机械 Fix）**——ErpHrConstants 新增 TIMESHEET_STATUS_DRAFT/SUBMITTED 常量 + BizModel 替换字面量。
      - P1-MA2-045 银行付款文件 UPLOADED/CONFIRMED + CRUD 桩：方案B（owner doc Deferred；config-gated 银行文件流 successor）。
      - P1-MA2-046 排班分配 status 无 dict 绑定：**方案A（ORM ask-first）**——新增 `erp-hr/shift-assignment-status` dict（或复用既有合适 dict）+ ORM `ext:dict` 绑定 + regen。
      - P1-MA2-047 SalaryPostingDispatcher posted 死字段 + javadoc drift：**方案B（owner doc Deferred + javadoc 对齐）**——posted 字段标注「R1.26 接线计提/公司承担过账后激活」，javadoc 修正；不删字段（R1.26 将写入）。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 逐项记录选择 + 理由 + 残留风险，后续 Phase 严格遵循（方案A 的 044/046 进 Phase 3，方案B 的死状态进 Phase 2，047 进 Phase 4）。

### Phase 2 - hr 死状态 owner doc Deferred 标注（6 findings：039/040/041/042/043/045）

Status: completed
Targets: `docs/design/human-resource/state-machine.md`、`docs/design/human-resource/payroll.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] state-machine.md 增/补 hr 各组件死状态 Deferred 标注段：员工 RESIGNED/TERMINATED/RETIRED（§场景D/E）、合同 SUSPENDED、调查 OPEN/CLOSED/ARCHIVED、发展计划 DRAFT/CANCELLED + 计划项 OVERDUE、工时单 APPROVED/REJECTED、银行付款文件 UPLOADED/CONFIRMED —— 每项明确「本期无 setStatus writer / 无 mutation，dict 值保留为预留 successor」+ 命名 successor 触发条件。
- [x] 核对 CRUD 桩（Survey 18 行 / PayrollBankFile 18 行 / DevelopmentPlan）owner doc 描述与「桩为主路径可用、状态机 Deferred」一致。
      - Skill: `none`

Exit Criteria:

- [x] state-machine.md / payroll.md 明确 6 项死状态为预留/Deferred，owner doc 与代码零 writer 一致；每项命名 successor 触发事件。

### Phase 3 - 机械修复：工时单常量替换（P1-MA2-044）+ 排班 dict 绑定（P1-MA2-046）

Status: completed
Targets: `module-hr/erp-hr-service/.../ErpHrConstants.java`、`module-hr/erp-hr-service/.../entity/ErpHrTimesheetBizModel.java`、`module-hr/model/app-erp-hr.orm.xml`、新增/绑定 shift-assignment-status dict
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [x] **Fix（P1-MA2-044）**：ErpHrConstants 新增 `TIMESHEET_STATUS_DRAFT` / `TIMESHEET_STATUS_SUBMITTED` 常量（值 "DRAFT"/"SUBMITTED"）；`ErpHrTimesheetBizModel.java:38,43` 替换硬编码字面量为常量引用。
      - Skill: `nop-backend-dev`
- [x] **Add（P1-MA2-046，ORM ask-first）**：新增 `erp-hr/shift-assignment-status` dict YAML（值覆盖代码 `ErpHrConstants.ASSIGNMENT_STATUS_*` 实际写入值 SCHEDULED/PRESENT/ABSENT[+ CANCELLED 若有 writer]，以代码与 owner doc §二 实际写入值为真相源）；ORM `app-erp-hr.orm.xml` ErpHrShiftAssignment `status` 列补 `ext:dict="erp-hr/shift-assignment-status"`；`mvn clean install -DskipTests` 增量 regen。
      - Skill: `nop-backend-dev`
- [x] **Proof（P1-MA2-046）**：regen 后确认 dict 绑定生效（XMeta/dict 生成产物含 shift-assignment-status 绑定），`xmllint --noout module-hr/model/app-erp-hr.orm.xml` 通过。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] ErpHrTimesheetBizModel 零硬编码 "DRAFT"/"SUBMITTED" 字面量（grep 退出码非零）。
- [x] ShiftAssignment status 列带 ext:dict 绑定；ORM well-formed 通过；增量 regen 产物落地（Closure Gates 跑全量 mvn）。

### Phase 4 - SalaryPostingDispatcher posted 字段 / javadoc 对齐（P1-MA2-047，与 R1.26 协同）

Status: completed
Targets: `module-hr/erp-hr-service/.../posting/SalaryPostingDispatcher.java`（javadoc）、`docs/design/human-resource/payroll.md`
Skill: `none`

- Item Types: `Fix | Decision`
- Prereqs: Phase 1

- [x] **Decision（协调 R1.26）**：posted 字段不删除（R1.26/P1-MA4-017 接线计提+公司承担过账后将写入 posted=true）；本计划仅修正 javadoc drift（移除「无 posted 字段」误导措辞，标注 posted 字段当前为 Deferred，由 R1.26 激活）。
      - Skill: `none`
- [x] **Fix（javadoc）**：SalaryPostingDispatcher javadoc 对齐实际（posted 字段存在但当前无 writer；tryPostAccrual 死代码将由 R1.26 接线）；payroll.md §过账 标注 posted 字段 Deferred successor = R1.26 完成。
      - Skill: `none`

Exit Criteria:

- [x] SalaryPostingDispatcher javadoc 不再声明「无 posted 字段」；payroll.md 标注 posted 字段由 R1.26 激活；本计划不写 posted writer（留给 R1.26）。

## Draft Review Record

- Independent draft review iteration 1: accept (ses_05094fe1fffeXi65Eb5z2DO2FL) because 9 项 finding 干净映射 4 个 Phase，实仓基线锚点全部精确命中（Survey/PayrollBankFile 18 行桩 / Timesheet 硬编码 / ErpHrConstants 无 TIMESHEET 常量 / ShiftAssignment status 无 ext:dict / posted 零 writer / RESIGNED/TERMINATED/RETIRED 仅只读守卫），单一结果表面（hr owner-doc/代码契约对齐，规则 14），mvn 正确置于 Closure Gates（ORM+代码变更），P1-MA2-047 正确限定为 javadoc/文档侧（接线归 R1.26），P1-MA2-046 ORM ask-first 正确声明。无 Blocker/Major。Minor（已处置）：P1-MA2-046 dict 值示例 SCHEDULED/SWAPPED/CANCELLED 与代码实际写入值 SCHEDULED/PRESENT/ABSENT 不符——已更正为「以 ErpHrConstants.ASSIGNMENT_STATUS_* 实际写入值为真相源」。

## Closure Gates

- [x] 范围内行为/文档对齐完成（9 项 finding 全部裁决落地或明确 Deferred）
- [x] 相关文档对齐（state-machine.md / payroll.md）
- [x] 已运行验证（`mvn clean install -DskipTests` 全绿 + `xmllint --noout` ORM well-formed + compliance checker 本计划零新增命中；grep 验证 044 零硬编码、046 dict 绑定生效）
- [x] 无范围内项目降级为 deferred/follow-up（方案B Deferred 是处置裁决 + 已命名 successor，非范围内缺陷隐瞒；044/046 为范围内存活实现项）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 员工离职/退休/转正 mutation（P1-MA2-039 方案A successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 三态为零 writer 死状态；employmentStatus/probationEndDate 字段已承载状态可由 HR 主动维护；方案A 触及 nop-auth 用户禁用副作用属保护区域；员工 ACTIVE/PROBATION 在职主路径完整。
- Successor Required: `yes`（PM 要求正式离职/退休/试用期转正工作流时实现 resignEmployee/retireEmployee/probationToRegular mutation + nop-auth 联动 + 长期 PROBATION 转 TODO 提醒）

### 调查/银行付款文件/发展计划/工时单审批 完整状态机（P1-MA2-040/041/042/043/045 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: dict 死状态保留为预留语义入口；CRUD 桩为主路径可用；各组件主生命周期（合同/工资支付轴/仿真/换班）完整。
- Successor Required: `yes`（各组件审批/确认/上传业务流落地时实现 setStatus writer + 状态迁移守卫）

### SalaryPostingDispatcher 计提/公司承担过账接线（P1-MA2-047 posted 字段激活）

- Classification: `moved to explicit successor ownership`
- Why Not Blocking Closure: posted 字段 drift 已由本计划 javadoc/owner doc 对齐消除；实际 posted writer 接线归属 R1.26（P1-MA4-017）。
- Successor Required: `yes`（R1.26 完成计提 SALARY + 公司承担社保/公积金 PostingEvent 接线时 posted 字段激活）

## Closure

Status Note: 全部 4 个 Phase 执行完成，9 项 finding 全部裁决落地（方案B Deferred 6 项 + 方案A 实现对齐 044/046 + javadoc/doc 对齐 047）。独立结束审计 PASS。

Closure Audit Evidence:

- 独立结束审计（ses_050852461ffepLFJMdHUYz4xmZ，新会话，read-only）：**PASS**，9 项 finding 逐项对照实仓验证，无缺陷、无范围蔓延。
  - P1-MA2-039/040/041/042/043/045：owner doc Deferred 标注全部落地（state-machine.md 适用对象二/三/五 + payroll.md §七），每项命名 zero writer + successor 触发条件；Survey/PayrollBankFile 确认 18 行桩。
  - P1-MA2-044：`ErpHrConstants.java:231-232` 常量存在；`ErpHrTimesheetBizModel.java:39,44` 引用常量；`grep '"DRAFT"|"SUBMITTED"'` 退出码 1（零硬编码）。
  - P1-MA2-046：ORM dict 定义 `app-erp-hr.orm.xml:177-182`（SCHEDULED/PRESENT/ABSENT/CANCELLED）+ `ext:dict` 绑定 `:1192`；`shift-assignment-status.dict.yaml` 已生成；生成 XMeta `_ErpHrShiftAssignment.xmeta:72` 含 `dict="erp-hr/shift-assignment-status"`；`xmllint --noout` 退出 0（仅 namespace 警告，平台常态）。
  - P1-MA2-047：`SalaryPostingDispatcher.java:29-35` javadoc 不再含「无 posted 字段」误导措辞，标注 posted Deferred + tryPostAccrual 死代码 + successor R1.26；`grep setPosted` 仅 javadoc 引用，零 writer（未越界写 posted writer）。
  - 无范围蔓延：未实现 resign/retire mutation、未加 timesheet approve/reject、未写 posted writer——均按计划保持 Deferred。
- 验证执行：`mvn clean install -DskipTests`（全 154 模块 reactor）= BUILD SUCCESS；`mvn test -pl module-hr/erp-hr-service -am` = 113 tests, 0 failures, 0 errors；`bash docs/audits/nop-compliance-checker.sh` 本计划零新增命中（R1a/b/c/R4/R5/R7/R11=0，R1d=17 不变，R8=42 不变；R2 较 2026-07-20 快照偏高为期间其他计划累积，非本计划引入）。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件（P1-MA2-039 → PM 要求正式离职/退休/转正工作流时；P1-MA2-040/041/042/043/045 → 各组件审批/确认/上传业务流落地时；P1-MA2-047 → R1.26/P1-MA4-017 接线计提+公司承担过账时 posted 字段激活）。
