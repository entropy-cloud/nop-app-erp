# 2026-08-08-0424-3-rc-mr1-r1-7-hr-attendance-makeup-clock RC-R1.7 — hr 设备故障手工补卡（P1-RC-014，MR1 第一批纯预授权）

> Plan Status: active
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.7（MR1 第一批纯预授权：hr 设备故障手工补卡，P1-RC-014）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.7 行 + `docs/audits/arm-index.md` P1-RC-014 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3.1（RC-R1.7 = 「纯 BizModel 代码逻辑修复」）
> Related: `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`（A4.2.20 运行时影响面：makeUp 缺失 CONFIRMED + XMeta clockIn/clockOut/source 无字段级 auth 守卫 → 越权风险代码层证实）；`docs/design/human-resource/use-cases.md`（L1 UC-HR-06⑮）+ `docs/design/human-resource/shift-scheduling.md`（L2 §4.2/§九）；`docs/plans/2026-08-08-0424-1-rc-mr1-r1-5-hr-attendance-last-wins.md`（同域同批 MR1 计划范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-014（arm-index 行）**：UC-HR-06⑮「设备故障时支持手工补卡」完全缺失。L1 `use-cases.md:72` 异常段逐字「设备故障时支持手工补卡」；L3 全 module-hr grep `makeUp/manualClock/补卡/supplement/adjustClock` 零命中。
- **实仓**：`ErpHrAttendanceBizModel` 仅 clockIn/clockOut/getTodayAttendance 三方法（`:49-74`），`IErpHrAttendanceBiz` 同（`:15-35`）——**无手工补卡 mutation**。
- **A4.2.20 运行时确认（`2026-08-07-0530` 报告）**：补卡 mutation 缺失 HEAD 复核 CONFIRMED；**越权风险代码层证实**：XMeta `_ErpHrAttendance.xmeta:36,40,60` clockIn/clockOut/source **无字段级 auth 守卫** → CrudBizModel 标准 save/update 可直接修改 clockIn/clockOut 绕过 clockIn/clockOut Processor 守卫（持 ErpHrAttendance 写权限者含 HR 角色）+ 无 source=MANUAL/reason 必填审计标记。**维持 P1 不撤销**（Q4 强制实现）。
- **涉及文件**：`module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrAttendanceBiz.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrAttendanceBizModel.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrConstants.java`（新增 `ATTENDANCE_SOURCE_MANUAL`）；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrErrors.java`（reason 必填守卫错误码，如需）；`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrAttendanceEngine.java`。
- **ORM 载体**：`ErpHrAttendance` 已有 `source`（propId 10，dict `erp-hr/attendance-source`）+ `remark`（propId 13）列；`clockIn`/`clockOut`（propId 4/5）可写。dict `erp-hr/attendance-source` 现含 CARD/BIOMETRIC/MOBILE，**无 MANUAL**——补卡须写 source=MANUAL：写字符串值本身不触 ORM（VARCHAR 列自由值，对齐 A4.2.145 孤儿非字典值先例），dict 注册 MANUAL 属 Q3 纯加性类（见 Decision）。
- **角色守卫载体**：`IUserContext.getRoles()`（平台 `io.nop.api.core.auth.IUserContext`）可获当前用户角色集；仓库无既有 Java 角色守卫先例（grep `getRoles/hasRole/checkRole` 生产代码零命中）——守卫机制为 Decision 项。
- **预授权判据**（第一批纯预授权）：纯 BizModel 代码逻辑修复（新增 mutation + 常量 + 错误码 + 测试），不触 ORM 结构/会计核心/删除；**无 ask-first checkbox**（dict 注册若选则属 Q3 纯加性授权范围，见 Decision）。roadmap RC-R1.7 行 `todo`，Deps（R1.0 done）已满足。

## Goals

- `IErpHrAttendanceBiz` + `ErpHrAttendanceBizModel` 新增 `makeUpClockIn` / `makeUpClockOut` mutation：
  - 入参：`employeeId` + `date`（补卡日期，可历史日期）+ `clockTime`（补录时间）+ `reason`（必填）。
  - 行为：定位该员工该日期 attendance（无则新建，镜像 clockIn 建行逻辑）→ `setClockIn/setClockOut(clockTime)` → `setSource(ATTENDANCE_SOURCE_MANUAL)` → `setRemark(reason)` → 写审计字段（平台 `OrmTimestampHelper` 自动填充 createdBy/updatedBy）。
  - 守卫：`reason` 非空必填（空抛新 ErrorCode）；**HR 角色守卫**（机制见 Decision）；补卡不绕过既有业务守卫语义——直接写目标时间戳，不触 clockIn/clockOut 现有时序守卫（补卡本质 = 绕过打卡时序的受控通道）。
- 新增 `ErpHrConstants.ATTENDANCE_SOURCE_MANUAL = "MANUAL"`。
- owner doc `shift-scheduling.md` 补注「手工补卡入口」（HR 角色 + reason 必填 + source=MANUAL 标记）。
- 回填 arm-index P1-RC-014 → `done (RC-R1.7)` + roadmap RC-R1.7 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不改 XMeta 字段级 auth 守卫本身**（`_ErpHrAttendance.xmeta` 不改——补卡 mutation 是受控补录通道；XMeta 字段守卫变更属平台级行为变更且会影响既有 CRUD 全路径，越出本 finding 修复面。越权风险的**修复面** = 提供专用补卡通道 + reason/source 审计标记，使标准 CRUD 不再是唯一逃生路径）。
- **不触 ORM 结构**（不新增补卡专属列；remark/source 既有列承载；dict 注册 MANUAL 仅当 Decision 选 Q3 纯加性路径，否则 source=MANUAL 字符串直接写入）。
- **不做补卡审批流**（L1 仅要求「支持手工补卡」，未要求审批；reason 必填 + HR 角色守卫是审计最小化）。
- **不改真相源**（use-cases/shift-scheduling 需求契约段；shift-scheduling.md 仅补实现注记）。
- **不做 makeUp 对 workHours/lateMinutes/earlyLeaveMinutes 的级联重算的跨天语义**（补卡只写时间戳 + 若 clockIn/clockOut 均存在则按既有 computeWorkHours 重算 workHours；迟到/早退/缺勤级联由 calc 侧既有调度消费，非本计划范围）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/human-resource/use-cases.md`（L1 UC-HR-06⑮）+ `docs/design/human-resource/shift-scheduling.md`（L2 补卡注记锚点）+ `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-...md`（A4.2.20 越权风险证据）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）
- Skill Selection Basis: 实现面 = 新增 BizModel mutation + IBiz 接口 + 角色守卫（`nop-backend-dev`：@BizMutation/@Name 签名、IUserContext 角色读取、ErrorCode 定义、跨实体访问规则）+ JUnit 测试（`nop-testing`：IGraphQLEngine/JunitAutoTestCase 断言 + 角色 seed 范式镜像 `TestErpPurPaymentApprovalNotifications.seedRole:259-286`）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新增 infra/config。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-hr/erp-hr-service`。

## Execution Plan

### Phase 1 - IBiz 接口 + BizModel mutation + 常量/错误码

Status: planned
Targets: `module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrAttendanceBiz.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrAttendanceBizModel.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrConstants.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无（既有基线）

- [ ] `Decision` **HR 角色守卫机制**：平台语义澄清——`IUserContext.getRoles()` 返回**角色 ID（roleId）集**而非角色名（`LoginServiceImpl` roleIds 构建 + `IUserContext.isUserInRole(roleId)`），`erp-hr.action-auth.xml:78` 菜单 `roles="HR 专员"` 匹配的也是 roleId（SiteMapProvider containsRole 语义）。故选项 A（推荐）= Java 侧 `IUserContext.get()` + `isUserInRole(HR_ROLE_ID)`（常量 `HR_ROLE_ID = "HR 专员"`，与 seed 数据/action-auth 的 roleId 一致——执行期须实仓核验 nop-auth seed 中该 role 的 roleId 字面值，若 seed roleId≠roleName 则以 seed roleId 为准）→ 不满足抛权限 ErrorCode（`ERR_MAKEUP_ROLE_REQUIRED` 或复用平台 `nop.err.biz.insufficient-permission`）；选项 B = xbiz `<mutation name=...><auth roles=.../></mutation>` 声明（仓库无既有先例 + 测试 enableActionAuth=FALSE 下不可断言，弃）；选项 C = action-auth.xml 接线（仓库无 mutation 级先例，弃）。备选与理由记录于本 Decision；**残留风险记录**：roleId 字面值与 seed 数据漂移风险（执行期核验 + owner doc 注记维护点）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **`erp-hr/attendance-source` dict 注册 MANUAL**：选项 A（推荐）= 在 `module-hr/model/app-erp-hr.orm.xml` dict 追加 `<option code="MANUAL" label="手工补卡" value="MANUAL"/>`（Q3 纯加性 ORM 变更批量授权：新增 option 不改既有语义/无数据影响；codegen 增量重生成 dict）→ 随后 `mvn clean install -DskipTests` 增量重新生成；选项 B = 不注册 dict，source=MANUAL 字符串直接写入（VARCHAR 列自由值，对齐 A4.2.145 孤儿非字典值先例，UI 下拉不显示 MANUAL）。备选与理由记录于本 Decision；**残留风险记录**：选项 B 下 UI 不显示 MANUAL 来源值（可读性缺口，owner doc 注记）。
      - Skill: `nop-backend-dev`
- [ ] `Add` `ErpHrConstants.ATTENDANCE_SOURCE_MANUAL = "MANUAL"`。
      - Skill: `nop-backend-dev`
- [ ] `Add` `IErpHrAttendanceBiz`：`makeUpClockIn(@Name("employeeId") Long, @Name("date") LocalDate, @Name("clockTime") Timestamp/LocalDateTime, @Name("reason") String, IServiceContext)` + `makeUpClockOut(...)`（签名决策：clockTime 类型与 ORM 列 clockIn/clockOut 的 java.sql.Timestamp 对齐，执行期按框架反序列化惯例定）。
      - Skill: `nop-backend-dev`
- [ ] `Add` `ErpHrAttendanceBizModel` 实现两 mutation：HR 角色守卫 → reason 空抛错误码 → 定位/新建 attendance（date + employeeId 唯一定位，镜像 `findAttendance` helper；新建行 `businessDate` 语义对齐 `defaultPrepareSave:53-55` 兜底 = 补卡日期 date，非 today）→ 写 clockIn/clockOut + source=MANUAL + remark=reason → 若 clockIn 与 clockOut 均非空则重算 workHours → `saveOrUpdateAttendance`（既有 helper）。守卫顺序：先角色后 reason（错误可见性）。
      - Skill: `nop-backend-dev`
- [ ] `Add` 错误码：`ERR_MAKEUP_REASON_REQUIRED`（`erp.err.hr.makeup-reason-required`）+ 权限错误码（按 Decision 选定机制，若用平台通用权限错误则不新增）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 两 mutation 经 BizModel 直调断言落地 + `@BizMutation` 声明自动暴露 GraphQL（与 clockIn 同型机制，Phase 2 含 GraphQL 层冒烟断言）；reason 空拒绝；HR 角色守卫生效（成功模式 = HR 角色可补卡；失败模式 = 非 HR 角色被拒 + reason 空被拒）
- [ ] 无 ORM 结构变更（仅当 Decision 选 dict 注册时为 Q3 纯加性 option 追加 + 增量重生成）

### Phase 2 - dedicated 测试

Status: planned
Targets: `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrAttendanceEngine.java`（扩展）或新建 `TestErpHrAttendanceMakeUp.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Add` 测试矩阵：① makeUpClockIn 新建行（无既有记录）→ source=MANUAL + remark=reason + clockIn=补录时间 + businessDate=补卡日期；② makeUpClockIn 覆盖既有行（同日已有打卡记录）→ clockIn 覆盖 + source 改 MANUAL；③ makeUpClockOut → clockOut + workHours 重算；④ reason 空 → 抛 `ERR_MAKEUP_REASON_REQUIRED`；⑤ HR 角色守卫——**显式构造 IUserContext 并经 `IUserContext.set(...)` 注入**（`enableActionAuth=FALSE` 下平台不自动建 context，`TestErpHrAttendanceEngine:43` 用的裸 `ServiceContextImpl` 不携带角色；须手动构造含 roleIds 的 IUserContext 并 set，用后恢复/清理）+ 可选 DB seed `NopAuthRole`+`NopAuthUser`+`NopAuthUserRole` 对齐（镜像 `TestErpPurPaymentApprovalNotifications.seedRole:259-286`，roleId 与 Decision 常量一致）→ 有角色通过 / 无角色被拒；⑥ 历史日期补卡（date=昨日/多日前）→ 正确写入该日期行。
      - Skill: `nop-testing`
- [ ] `Proof` 断言强度：返回行 source/remark/clockIn/clockOut 精确值 + 错误码 + 角色拒/纳双侧；GraphQL 层冒烟断言（`graphQLEngine.executeRpc` 调 `ErpHrAttendance__makeUpClockIn`，镜像 `TestErpPurPaymentApprovalNotifications:210-214` 范式，证明 mutation 经 GraphQL 可达）；`@NopTestConfig` 隔离（镜像 `TestErpHrAttendanceEngine` 既有 `enableActionAuth=FALSE` 范式——Java 侧角色守卫不受 enableActionAuth 影响，直测 IUserContext 角色集）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 测试矩阵全绿：`mvn test -pl module-hr/erp-hr-service` 全绿（既有 tests 零回归）
- [ ] 角色守卫双侧断言落地（无「守卫存在但零覆盖」缺口）；GraphQL 冒烟断言落地（契约可达性有证据）

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: planned
Targets: `docs/design/human-resource/shift-scheduling.md`（补注手工补卡入口）；`docs/audits/arm-index.md`（P1-RC-014 修复状态）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.7 done）；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [ ] `Add` owner doc 补注：`shift-scheduling.md` 补「手工补卡入口」段（mutation 名 + HR 角色 + reason 必填 + source=MANUAL + remark 承载 + dict 注册状态注记）；不修改需求契约段（真相源冻结条款遵守）。
      - Skill: none
- [ ] `Add` arm-index P1-RC-014 行「修复状态」→ `done (RC-R1.7)` + 修复落地摘要（两 mutation + 角色守卫 + source/reason 审计标记）；roadmap RC-R1.7 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none

Exit Criteria:

- [ ] arm-index/roadmap 状态回填 + owner doc 补注落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_022137fe0ffeq8oZQMq1RDhJ6Y`，fresh session）——0 阻塞 + 3 实质修正（E1：退出标准在 draft 计划中预勾 `[x]` → 改 `[ ]`；E2：Decision1 平台语义错误——`IUserContext.getRoles()` 返回 roleId 集而非 roleName、action-auth roles 属性匹配 roleId → 改为 `isUserInRole(roleId)` + 常量与 seed roleId 一致 + 执行期实仓核验；E3：测试⑤ `setUser` 欠指定——`enableActionAuth=FALSE` 下需显式构造 IUserContext 并经 `IUserContext.set(...)` 注入）+ 2 minor（Phase 1 Exit「GraphQL 可达」无测试锚点 → Phase 2 增 `graphQLEngine.executeRpc` 冒烟断言；新建行 businessDate 语义未定 → 对齐 defaultPrepareSave 兜底 = 补卡日期）+ 1 minor（镜像 seed 的 `roleId="role-"+roleName` 与 roleName 检查冲突——与 E2 同源）→ 全量修订。
- Independent draft review iteration 2: `accept`（本次独立草案审查，mission-driver 2026-08-07-181210 会话）——全量基线主张实仓核验通过（`ErpHrAttendanceBizModel` 仅 clockIn/clockOut/getTodayAttendance 三方法 + helpers findAttendance/saveOrUpdateAttendance/computeWorkHours 齐备 / `IErpHrAttendanceBiz` 同 / `ErpHrConstants:267` ATTENDANCE_SOURCE_CARD 存在、无 MANUAL / dict `erp-hr/attendance-source` orm.xml:137-140 仅 CARD/BIOMETRIC/MOBILE / XMeta `_ErpHrAttendance.xmeta:36,40,60` clockIn/clockOut/source 无字段级 auth 守卫 / `erp-hr.action-auth.xml:78` roles="HR 专员" / `IUserContext.getRoles()`+`isUserInRole(roleId)` 平台语义（`UserContextImpl:212,298` + `LoginServiceImpl:276-291` roleIds 构建）证实 Decision1 选项 A 成立 / `IUserContext.set` 存在（`IUserContext.java:25`）支撑测试⑤ / 生产代码 grep `getRoles|hasRole|checkRole` 零命中（无既有 Java 角色守卫先例）/ roadmap `:375` RC-R1.7 todo + arm-index `:153` P1-RC-014 + expander `:96` 纯 BizModel 类目一致 / seedRole 范式 `TestErpPurPaymentApprovalNotifications:259-286`（roleId="role-"+roleName，测试⑤已声明 seed roleId 与 Decision 常量一致消除漂移）+ GraphQL 范式 `:210-214` / `mvn test -pl module-hr/erp-hr-service` 目标与 closure 验证命令对齐），0 BLOCKER 0 MAJOR；2 MINOR 保留（Phase 1 Exit「BizModel 直调断言/角色守卫」实际落地于 Phase 2 测试矩阵——自我标注 GraphQL 冒烟在 Phase 2，属软出口；front matter Related §4.2 锚点与补卡注记实际落点 §九 略偏——均不阻塞执行）。共识达成，转 active。

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证（`mvn test -pl module-hr/erp-hr-service` + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline——新增 ErrorCode/常量不产生 checker 新违规）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### XMeta 字段级 auth 守卫（clockIn/clockOut/source 禁止标准 CRUD 直写）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 越权风险的修复面 = 提供受控补卡通道（本计划）；XMeta 字段守卫变更影响既有 CRUD 全路径（AMIS 编辑考勤记录等），且 Nop 字段级 auth 机制在 `enableActionAuth=FALSE` 测试环境不可断言，属独立平台级增强。
- Successor Required: no（watch-only；触发条件 = 出现标准 CRUD 直写考勤时间的活跃滥用证据）

### 补卡审批流 / 补卡后 迟到-早退-缺勤 级联重算

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 仅要求「支持手工补卡」，未要求审批；迟到/早退/缺勤级联由 calc 侧既有调度消费（shift-scheduling §4.1），补卡只写时间戳。
- Successor Required: no

## Closure

Status Note: 待执行（draft 阶段）。

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计

Follow-up:

- 无（范围内项目全落地后关闭）
