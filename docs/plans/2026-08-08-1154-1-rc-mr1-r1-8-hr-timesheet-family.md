# 2026-08-08-1154-1-rc-mr1-r1-8-hr-timesheet-family RC-R1.8 — hr 工时单族（P1-RC-015 + P1-MA2-043 reuse 重开，MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.8（MR1 第一批纯预授权：hr 工时单族——24h 日工时上限校验 + totalHours 派生汇总 + approve/reject 状态机，P1-RC-015 + P1-MA2-043 reuse 重开）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.8 行 + `docs/audits/arm-index.md` P1-RC-015 / P1-MA2-043 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3（RC-R1.8 = 纯 BizModel 修复，G1 hr 工时单族同域同修复方式）
> Related: `docs/design/human-resource/use-cases.md`（L1 UC-HR-03）；`docs/audits/2026-08-07-0530-rc-ma4-a4-2-22-26-hr-payroll-survey-runtime.md`（A4.2.24：24h 校验缺失运行时确认）；`docs/plans/2026-08-08-0424-3-rc-mr1-r1-7-hr-attendance-makeup-clock.md`（同域同批 MR1 计划范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-015（arm-index 行）**：UC-HR-03 ②③——「同一日工时超过 24h 校验」+「totalHours 已汇总」缺失。L1 `use-cases.md:35-36` 逐字「后置条件：totalHours 已汇总」+「异常：同一日工时超过 24h 校验」。L3 实仓：`ErpHrTimesheetBizModel.java:35-47` 仅 `submit`（DRAFT→SUBMITTED）；`ErpHrTimesheetLineBizModel.java` 18 行 CRUD 桩；grep `totalHours|setTotalHours|24|MAX_HOURS` 全 `module-hr/erp-hr-service/src/main/` 零业务命中；ORM `app-erp-hr.orm.xml:592` `totalHours` 列（propId 6，domain=hours，可空）存在但**无 writer**。
- **finding P1-MA2-043（arm-index 行，reuse 重开）**：工时单 APPROVED/REJECTED dict 死状态 + `ErpHrTimesheetBizModel` 仅 submit。L1 基本流程 4/5 逐字「项目经理审批 → APPROVED（工时归集到 projects 域 cost-collection）/ 或驳回 → REJECTED，员工修改后重新提交」。L3：dict `erp-hr/timesheet-status` 含 DRAFT/SUBMITTED/APPROVED/REJECTED 四态（orm.xml:57-61），但 `ErpHrConstants.java:231-232` 仅 `TIMESHEET_STATUS_DRAFT/SUBMITTED` 常量，无 approve/reject mutation，REJECTED 亦不可达。RC 复核（2026-08-03）在 Q4=(a) 下重开经 MR1 实现。
- **A4.2.24 运行时确认（`2026-08-07-0530` 报告）**：grep `MAX_HOURS|maxHours|totalHours.*24|24.*hours` 跨 hr main 零业务命中 + `setTotalHours` 零业务 writer[仅 codegen _gen + API bean setter] + `ErpHrTimesheetBizModel:35-47` 仅 submit → 24h 校验缺失 + totalHours 永远 null。**维持 P1 不撤销**（Q4 强制实现）。
- **归集触发链路实仓核查（本计划基线）**：projects 侧 `TimesheetPostingDispatcher`（module-projects/erp-prj-service/.../posting/）消费的是 **projects 域自有实体 `ErpPrjTimesheet`**（`IErpPrjTimesheetBiz` 已有 submit/approve/reject 三 mutation），**非 hr 域 `ErpHrTimesheet`**；hr→projects 跨域 seam（IErpPrj* 消费 hr timesheet）grep 零命中；仓库无全局事件总线（对齐 A4.2.152 maintenance 事件普查先例）。→ 「工时归集到 projects 域 cost-collection」在 hr 侧无既有接线，归集触发机制为 Decision 项（见 Phase 2）。
- **ORM 载体**：`ErpHrTimesheet`（id/code/employeeId/periodFrom/periodTo/totalHours/status/orgId/remark/业务审计字段/businessDate，**无 approvedBy/approvedAt 列**）；`ErpHrTimesheetLine`（timesheetId/employeeId/workDate/projectId/taskId/activityType/hours[propId 8, mandatory]/description，workDate mandatory）。`lines` to-many 关系已声明（orm.xml:606）。
- **错误码载体**：`ErpHrErrors.java:284` `ERR_HR_TIMESHEET_ILLEGAL_TRANSITION` 已存在（submit 复用），`ARG_TIMESHEET_ID/ARG_CURRENT_STATUS` 参数已定义。
- **预授权判据**（第一批纯预授权）：纯 BizModel 代码逻辑修复（新增 mutation + 常量 + 校验 + 测试），**不触 ORM 结构/会计核心/删除**（approvedBy/approvedAt 审计列触 ORM → 不做，见 Non-Goals）；**无 ask-first checkbox**。roadmap RC-R1.8 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrTimesheetBiz.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrTimesheetBizModel.java`；`module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrTimesheetLineBiz.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrTimesheetLineBizModel.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrConstants.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrErrors.java`；`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/`（新增测试类）。

## Goals

- **P1-RC-015 修复**：
  - 24h 日工时上限校验：`ErpHrTimesheetLineBizModel` 行保存/更新守卫 + `ErpHrTimesheetBizModel.submit` 终检——按 `employeeId + workDate` 汇总工时（跨工时表全量，见 Decision），Σ > 24 抛新 ErrorCode `ERR_TIMESHEET_DAILY_HOURS_EXCEEDED`。
  - totalHours 派生汇总：行保存/更新/删除时重算父 `ErpHrTimesheet.totalHours = Σ lines.hours` 并写回；submit 时终检汇总。
- **P1-MA2-043 修复**：
  - `approve(timesheetId)`：SUBMITTED→APPROVED；`reject(timesheetId, reason)`：SUBMITTED→REJECTED（reason 写入 remark）；`submit` 扩展允许 REJECTED→SUBMITTED（员工修改后重新提交）。
  - 新增 `ErpHrConstants.TIMESHEET_STATUS_APPROVED/REJECTED`（对齐 dict 既有值）；审计字段 = 平台 `updatedBy/updateTime` 自动填充（无 approvedBy/approvedAt 列，不触 ORM，见 Decision/Non-Goals）。
- **归集触发裁决**：Phase 2 Decision 明确「cost-collection 归集触发」落地形态（默认登记 successor，见 Decision）。
- owner doc `payroll.md`（或工时相关设计段）补实现注记；回填 arm-index P1-RC-015/P1-MA2-043 → `done (RC-R1.8)` + roadmap RC-R1.8 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**：不新增 `approvedBy/approvedAt` 审计列、不新增 24h 校验表（对齐第一批纯预授权边界；审计列需求登记 Deferred But Adjudicated successor，触发条件 = 第二批 ORM 授权或人工裁决）。
- **不做「工时归集到 projects 域 cost-collection」的跨域写接线**（projects 域自有 `ErpPrjTimesheet` 平行流 + 无 hr→projects seam + 无事件总线；跨域契约属越界项须 ask-first——登记 Deferred But Adjudicated，见 Phase 2 Decision）。
- **不做 XMeta/view.xml 变更**（字段级校验/前端交互不改；AMIS 侧按钮接线非本 finding 修复面，纯后端 mutation 暴露即满足契约）。
- **不改真相源**（use-cases/payroll 需求契约段；仅补实现注记）。
- **不做「项目工时段落不可重叠」可选异常**（L1 标"可选"，非强制项）。
- **不做已审批工时表的反审批/修改守卫扩展**（APPROVED 后行修改守卫：L1 未要求，登记 watch-only 观察，见 Deferred But Adjudicated）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/human-resource/use-cases.md`（L1 UC-HR-03）+ `docs/design/human-resource/payroll.md`（L2 工时注记锚点）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）+ `docs/audits/2026-08-07-0530-rc-ma4-a4-2-22-26-hr-payroll-survey-runtime.md`（A4.2.24 运行时证据）
- Skill Selection Basis: 实现面 = BizModel mutation + IBiz 接口 + 校验逻辑（`nop-backend-dev`：@BizMutation/@Name 签名、CrudBizModel 生命周期钩子 defaultPrepareSave/Update/Delete 覆写、跨实体访问规则、ErrorCode 定义）；测试（`nop-testing`：JunitAutoTestCase/IGraphQLEngine 断言 + _cases/ 快照录制范式镜像 `TestErpHrAttendanceMakeUp`）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新增 infra/config。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-hr/erp-hr-service`。

## Execution Plan

### Phase 1 - 24h 校验 + totalHours 派生汇总（P1-RC-015）

Status: completed
Targets: `ErpHrTimesheetLineBizModel.java`；`ErpHrTimesheetBizModel.java`；`ErpHrConstants.java`；`ErpHrErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [x] `Decision` **24h 校验的汇总口径**：选项 A（推荐）= 跨工时表全量——按 `employeeId + workDate` 查询全部未逻辑删除 `ErpHrTimesheetLine`（经 `daoProvider.daoFor(ErpHrTimesheetLine.class)`，对齐 arm-index 修复描述「sum hours by employee+date > 24」），Σ > 24 抛 `ERR_TIMESHEET_DAILY_HOURS_EXCEEDED`（24h 是物理上限，跨表全量无误报）；选项 B = 仅本工时表内同 workDate 行合计（无跨行查询、范围小，但员工同日期跨两张表时可绕过）。备选与理由记录于本 Decision；**残留风险**：选项 A 需在行级 save/update 守卫中执行跨表查询（性能可接受——按 employeeId+workDate 过滤 + 索引 IDX_HR_TIMESHEET_LINE_EMPLOYEE_ID 存在）。执行期按选项 A 落地，除非实仓核验发现性能/契约障碍则回退 B 并记录。
      - Skill: `nop-backend-dev`
      - **Decision 记录（2026-08-08 执行）**：**选项 A 落地**（跨表全量口径）。查询载体执行期调整：以同实体 `findList(query, null, context)`（行 BizModel 自身查询）+ 注入 `IErpHrTimesheetLineBiz.findList`（timesheet BizModel 跨实体查询）替代 `daoProvider.daoFor(...)`——实仓核验发现 `daoFor` 在 BizModel 中会触发 compliance checker R2b（229→230）/R2c（1383→1384）基线漂移（CI red，Closure Gates 要求 actual ≤ baseline；R1.6 同批先例已记录「I*Biz 优先，daoFor 回落会漂移」），I*Biz findList 达成相同跨表口径（ORI 逻辑删除行自动过滤）且零漂移；性能核验：employeeId+workDate 过滤 + 既有索引 IDX_HR_TIMESHEET_LINE_EMPLOYEE_ID 命中，无契约障碍。残留风险维持：并发窗口内同员工同日期两行同时保存可能双双通过守卫（无行级锁）——与 R1.5 last-wins 同型接受（24h 上限为软守卫，提交终检 + 后续审计可拦截），watch-only。
- [x] `Fix` 24h 校验接线：`ErpHrTimesheetLineBizModel` 覆写 `defaultPrepareSave`/`defaultPrepareUpdate`——在既有校验后追加 per-line 日工时守卫（本行 hours + 既有同日行 Σ > 24 拒绝）；`ErpHrTimesheetBizModel.submit` 增终检（提交时全量重算校验，防御绕过行级守卫的路径）。守卫只针对 hours > 0 的行。
      - Skill: `nop-backend-dev`
      - 落地：`ErpHrTimesheetLineBizModel.checkDailyHoursLimit`（package-private static `sumHours`/`dailyTotalFor` 共享汇总 helper：update 路径按 line.id 排除本行旧值再累加新值；save 路径新行 id 为空天然不含）接入 defaultPrepareSave/defaultPrepareUpdate；`ErpHrTimesheetBizModel.submit` 提交前对每行执行同规则终检（跨表口径，防御直改 DB/绕过行级守卫路径）。守卫 `hours ≤ 0` 跳过（对齐「守卫只针对 hours > 0 的行」）。
- [x] `Fix` totalHours 派生汇总：行保存/更新/删除后重算父表 `ErpHrTimesheet.totalHours = Σ lines.hours` 并 `updateEntity` 写回（删除路径覆写 delete 钩子——CrudBizModel 逻辑删除行后同步重算，防 stale totalHours）；submit 时若 totalHours 为空或与 Σ 不一致则按 Σ 修正后再提交。
      - Skill: `nop-backend-dev`
      - 落地：覆写 `ErpHrTimesheetLineBizModel.afterEntityChange(entity, context)`（**2-arg 重载**——实仓字节码核验：save/update 路径经 3-arg→2-arg 委托、delete 路径**直接调 2-arg**，覆写 3-arg 会漏 delete 路径[首轮测试实证：删除后 totalHours 停留旧值]）——保存/更新/删除后 `orm().flushSession()`（显式 flush 保证刚保存/删除行对查询可见，防 stale）→ 按 timesheetId 重查 Σ → `timesheetBiz.updateEntity` 写回；`submit` 无条件按 Σ 修正 totalHours（null/不一致均收敛）。
- [x] `Add` 错误码 `ERR_TIMESHEET_DAILY_HOURS_EXCEEDED`（`erp.err.hr.timesheet-daily-hours-exceeded`，参数含 employeeId/workDate/合计值）+ 常量（如需）`MAX_DAILY_HOURS = 24`（`ErpHrConstants`）。
      - Skill: `nop-backend-dev`
      - 落地：`ErpHrErrors` 新增 `ERR_TIMESHEET_DAILY_HOURS_EXCEEDED`（ARG_EMPLOYEE_ID/ARG_WORK_DATE/ARG_TOTAL_HOURS）+ `ERR_TIMESHEET_REJECT_REASON_REQUIRED`（Phase 2 reject 共用）；`ErpHrConstants` 新增 `MAX_DAILY_HOURS = 24` + `TIMESHEET_STATUS_APPROVED/REJECTED`（Phase 2 共用）。

Exit Criteria:

- [x] 行保存/更新时同日 Σ > 24 被拒绝（含跨表口径命中），=24 边界放行；submit 终检同规则
- [x] 行保存/更新/删除后 `ErpHrTimesheet.totalHours` 与 Σ 一致（stale 场景有断言）

### Phase 2 - approve/reject/resubmit 状态机（P1-MA2-043）+ 归集触发裁决

Status: completed
Targets: `IErpHrTimesheetBiz.java`；`ErpHrTimesheetBizModel.java`；`ErpHrConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1 完成

- [x] `Decision` **「cost-collection 归集触发」落地形态**：实仓证据——projects 侧 `TimesheetPostingDispatcher`/`IErpPrjTimesheetBiz` 消费的是 projects 域自有 `ErpPrjTimesheet` 实体（hr 域 `ErpHrTimesheet` 无任何消费方）；hr→projects 跨域 IBiz seam grep 零命中；仓库无全局事件总线（对齐 A4.2.152 先例）。选项 A（默认）= hr 侧义务落定为「状态机完整 + 审计字段（平台 updatedBy/updateTime）」，「hr 工时→projects cost-collection 归集」登记 Deferred But Adjudicated successor（跨域契约 + 双域语义统一[ErpHrTimesheet vs ErpPrjTimesheet 平行流]须人工裁决/第二批 ask-first）；选项 B = 本期实现跨域归集接线（新建 hr→projects 契约，属越界项须 ask-first，越出第一批纯预授权边界）。备选与理由记录于本 Decision；**残留风险**：若人工裁决要求跨域归集，本计划产出的 APPROVED 状态/事件点是其接线锚点。
      - Skill: `nop-backend-dev`
      - **Decision 记录（2026-08-08 执行）**：**选项 A 落地**。实仓核验：`grep IErpHrTimesheet*` 跨 `module-projects`/其余业务域 service main **零命中**（hr 域实体无任何外部消费方）；`TimesheetPostingDispatcher.java:10,60,77,99,103` 全量消费 projects 域自有 `ErpPrjTimesheet`。hr 侧义务本计划全部落地：状态机四态可达 + 审计字段（updatedBy/updateTime 平台自动填充，无 approvedBy/approvedAt 列，不触 ORM）。跨域归集接线（选项 B）需 ask-first 越界 → 登记 Deferred But Adjudicated（已在计划尾部登记），APPROVED 状态点为后续接线锚点。
- [x] `Add` `IErpHrTimesheetBiz`：`approve(@Name("timesheetId") Long, IServiceContext)` + `reject(@Name("timesheetId") Long, @Name("reason") String, IServiceContext)`（@BizMutation）。
      - Skill: `nop-backend-dev`
- [x] `Add` `ErpHrTimesheetBizModel` 实现：`approve`（SUBMITTED→APPROVED，复用 `ERR_HR_TIMESHEET_ILLEGAL_TRANSITION` 非法迁移守卫）+ `reject`（SUBMITTED→REJECTED，reason 非空必填——空抛 `ERR_TIMESHEET_REJECT_REASON_REQUIRED`[新增] 或复用平台必填错误，执行期定；reason 写入 remark）；`submit` 扩展状态判定从「仅 DRAFT」改为「DRAFT 或 REJECTED」→SUBMITTED（重新提交路径）；无角色守卫（L1 未定义项目经理角色载体，守卫 = 状态机本身；与 submit 同型——如审查要求角色守卫则按 R1.7 `IUserContext.isUserInRole` 范式补充并记录）。
      - Skill: `nop-backend-dev`
      - 落地：`approve`/`reject` 均 SUBMITTED 守卫 + `ERR_HR_TIMESHEET_ILLEGAL_TRANSITION`（含 timesheetId/currentStatus 参数）；reject reason 空白抛新增 `ERR_TIMESHEET_REJECT_REASON_REQUIRED`（专用错误码，测试可断言，对齐 R1.7 `ERR_MAKEUP_REASON_REQUIRED` 先例——执行期裁决选新增而非复用平台必填错误）；submit 状态判定 `DRAFT || REJECTED`；无角色守卫（同 submit 现状，L1 无角色载体）。
- [x] `Add` 常量 `ErpHrConstants.TIMESHEET_STATUS_APPROVED = "APPROVED"` / `TIMESHEET_STATUS_REJECTED = "REJECTED"`（对齐 dict `erp-hr/timesheet-status` 既有值，消除 dict 死状态）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] APPROVED/REJECTED 可达：approve/reject 守卫正确（非 SUBMITTED 拒绝）、REJECTED→submit→SUBMITTED 重新提交闭环、reason 必填拒绝
- [x] dict `erp-hr/timesheet-status` 四态全部可达（无死状态）；归集触发裁决记录于 plan + Deferred But Adjudicated

### Phase 3 - dedicated 测试

Status: completed
Targets: `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrTimesheetFamily.java`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [x] `Add` 测试矩阵（DAO seed + GraphQL 双路径，镜像 `TestErpHrTimesheet` 既有范式与 `_cases/` 快照录制）：① 行保存同日 Σ>24 拒绝（错误码断言）+ =24 边界放行；② 跨表口径命中（两张 timesheet 同员工同日期合计 >24 拒绝）；③ totalHours 汇总正确（行增/删后重算）；④ approve SUBMITTED→APPROVED + 审计字段（updatedBy/updateTime 非空）；⑤ reject reason 必填 + REJECTED→submit 重新提交→再 approve 全链；⑥ 非法迁移拒绝（DRAFT approve / APPROVED 再 submit 等）。
      - Skill: `nop-testing`
      - 落地：`TestErpHrTimesheetFamily`（新建，8 测试方法，`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)` + `HrFrozenClockExtension` 冻结 + `@BeforeEach` 注入 UserContextImpl + `_cases/` 快照 RECORDING 录制落盘 8 case 目录）：① `testLineSaveDailyHoursExceededAndBoundary`（20h→+4h=24 边界放行→+1h=25 拒绝 + 零落库断言）；② `testLineUpdateDailyHoursExceededRejected`（20h+4h 行更新→5h=25 拒绝且原值保持 + 更新→3h=23 放行，defaultPrepareUpdate 守卫）；③ `testDailyHoursExceededCrossTimesheetScope`（ts1 10h + ts2 15h 跨表=25 拒绝）；④ `testTotalHoursRecomputedAfterLineChanges`（DAO 直 seed stale totalHours=null 断言 → 行增 17.50 → 行改 11.50 → 行删 2.00）；⑤ `testApproveFromSubmittedSetsAuditFields`（APPROVED + updatedBy/updateTime 非空）；⑥ `testRejectReasonRequiredAndResubmitChain`（空白 reason 拒绝 + REJECTED/remark + 重提 + 再 approve 闭环）；⑦ `testIllegalTransitionsRejected`（DRAFT approve/reject / APPROVED submit / REJECTED approve 全拒）；⑧ `testGraphQLSmokeSubmitRejectApprove`（executeRpc 四连调 status=0 + 状态/remark 落库断言）。范式注记：`TestErpHrTimesheet` 不存在（实仓 grep 零命中），按计划 Task Route 指定的 `TestErpHrAttendanceMakeUp` 为镜像范式。
      - **快照处置决策（执行期实证）**：`testTotalHoursRecomputedAfterLineChanges` 含逻辑删除行——Nop 逻辑删除将 delVersion 置为毫秒时间戳，快照回放时 var 注册值与最终行值存在 1ms 竞态（`nop.err.match.not-equals-var-value` 实测失败）→ 该测试方法降级 `@EnableSnapshot(checkOutput = false)`（输入表回放保留、输出表不比对），行为正确性由显式断言（totalHours==2.00 等）承担——对齐 nop-testing skill「DB snapshot 不可比时降级 + 显式断言」先例；其余 7 方法全量快照比对稳定（连续 3 次运行全绿）。
- [x] `Proof` GraphQL 冒烟断言（`graphQLEngine.executeRpc` 调 `ErpHrTimesheet__approve/reject/submit`，证明 mutation 经 GraphQL 可达，镜像 `TestErpPurPaymentApprovalNotifications:210-214` 范式）+ `@NopTestConfig` 隔离（对齐既有 `enableActionAuth=FALSE` 范式）+ 快照录制。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 测试矩阵全绿：`mvn test -pl module-hr/erp-hr-service` 全绿（既有 tests 零回归）
- [x] 24h 拒绝/边界/汇总/状态机全链断言落地（无「守卫存在但零覆盖」缺口）；GraphQL 可达性有证据

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/human-resource/payroll.md`（或工时设计段）；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 补注：工时段补「24h 日工时上限校验 + totalHours 派生汇总 + approve/reject/resubmit 状态机」实现注记（mutation 名 + 校验口径 + 归集 successor 注记）；不修改需求契约段（真相源冻结条款遵守）。
      - Skill: none
      - 落地：`docs/design/human-resource/state-machine.md §适用对象三`（工时状态机设计段，payroll.md 无工时段——实仓核验 payroll.md 零「工时」命中）Deferred blockquote 替换为「实现注记（RC-R1.8）」（mutation 名 submit/approve/reject + 24h 跨表全量口径 + totalHours 派生语义 + 审计字段 updatedBy/updateTime + 归集 successor 注记与接线锚点）+ 迁移图 `REJECTED → DRAFT` 修正为 `REJECTED → SUBMITTED`（重提路径落地，与实现一致）；use-cases.md（真相源契约）零改动。
- [x] `Add` arm-index P1-RC-015 / P1-MA2-043 行「修复状态」→ `done (RC-R1.8)` + 修复落地摘要；roadmap RC-R1.8 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none
      - 落地：arm-index P1-RC-015/P1-MA2-043 行追加「修复落地（RC-R1.8）」注记（校验口径/状态机/测试/Decision 摘要）；roadmap RC-R1.8 `todo → done ✅` + Owner Doc 列修正（payroll.md→state-machine.md）+ 文件头最后更新注记；`docs/logs/2026/08-08.md` 顶部新增 RC-R1.8 日志条目（产出/决策记录/快照处置决策/验证/文档回填/下一步）。

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 补注落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: accept（2026-08-08 draft review）— 格式合规（模板全部必需段齐全）、范围边界清晰（Non-Goals + 两项 Decision 均有默认选项与裁决记录）、基线证据已对照实仓核验（BizModel 桩/ORM 列与索引/ErrorCode 约定/arm-index+roadmap+expander 行全部吻合）；Minor：Closure Status Note 随激活同步更新

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-hr/erp-hr-service` + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline——新增 ErrorCode/常量不产生 checker 新违规）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### hr 工时 → projects cost-collection 跨域归集接线

- Classification: `out-of-scope improvement`（跨域契约 + 双域语义统一）
- Why Not Blocking Closure: projects 域自有 `ErpPrjTimesheet` 平行流 + 无 hr→projects seam + 无事件总线；跨域写接线属越界项须 ask-first，第一批纯预授权边界外。hr 侧义务（状态机 + 审计字段）本计划落地。
- Successor Required: yes（触发条件 = 人工裁决跨域归集形态或第二批启动——此时本计划产出的 APPROVED 状态点是接线锚点）

### approvedBy/approvedAt 审计列（ORM 结构变更）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 触 ORM 结构变更（加列）属第一批纯预授权边界外；平台 `updatedBy/updateTime` 已提供操作人/时间审计（对齐 A4.1.20 降级证据范式）。
- Successor Required: yes（触发条件 = 第二批 ORM 授权或人工裁决）

### APPROVED 后行修改守卫

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1 未要求 APPROVED 后锁定行的规则；本期状态机只拦状态迁移，不拦行级编辑。
- Successor Required: no（触发条件 = 出现 APPROVED 工时被篡改的活跃业务证据）

## Closure

Status Note: 全部 4 阶段完成并验证全绿（`mvn test -pl module-hr/erp-hr-service` 154 tests 零回归 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + compliance checker actual==baseline 零漂移）；独立结束审计 PASS；文档回填完成（state-machine.md 实现注记 + arm-index 两行 done（RC-R1.8）+ roadmap RC-R1.8 done ✅ + 日志条目）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，task ses_0205af632ffeJTaEwqxSalUOpy）
- Verdict: PASS（1 Minor：Closure 段执行前占位符滞后——本闭包动作即闭合该 Minor，非阻塞）
- Evidence: 逐项核验计划 items/exit criteria 全 [x]、代码/文档实仓落盘（5 Java 文件 + 测试类 + 4 文档，`git diff --stat` 231+/10-，零 ORM/api.xml/view.xml/会计核心变更）、独立复跑 `mvn test -pl module-hr/erp-hr-service` 154 全绿 + checker 19 规则 actual==baseline（R1d=14/R2a=34/R2b=229/R2c=1383/R2d=34/R3=5/R5=0/R6=2/R10=7/R12a=69/R12b=66/R12c=40）

Follow-up:

- 无（范围内项目全落地；跨域归集接线/审计列/APPROVED 行修改守卫三项按 Deferred But Adjudicated 登记，见上节；`TestErpHrTimesheetFamily#testTotalHoursRecomputedAfterLineChanges` 快照输出比对因 delVersion 毫秒竞态降级 `@EnableSnapshot(checkOutput=false)`，行为由显式断言承担，已在 plan Phase 3 与日志记录）
