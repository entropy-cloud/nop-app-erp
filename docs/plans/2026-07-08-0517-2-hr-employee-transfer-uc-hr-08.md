# 2026-07-08-0517-2-hr-employee-transfer-uc-hr-08 HR 员工部门调动（UC-HR-08）

> Plan Status: completed
> Mission: erp
> Work Item: HR 员工部门调动（UC-HR-08，承接 extended-roadmap M3 Non-Goal 后继「员工调动段」）
> Last Reviewed: 2026-07-08
> Source: `docs/design/human-resource/use-cases.md` UC-HR-08（部门调动）；`docs/backlog/extended-roadmap.md` M3 Non-Goal boundary「UC-HR-08（员工调动/胜任力）✅ done 胜任力段（plan 1100-2）…员工调动段归 successor」
> Related: `docs/plans/2026-07-07-1100-2-hr-competency-management.md`（completed，胜任力段，与本调动段同属 UC-HR-08 拆分）、`docs/plans/2026-07-08-0517-1-test-code-localdatetime-now-cleanup.md`（同批 N=1，本计划独立可后行）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **UC-HR-08 设计已收敛**：`docs/design/human-resource/use-cases.md:87` UC-HR-08 部门调动——触发=部门主管/HR 发起调动申请；前置=目标部门和职位已存在；基本流程=HR 选员工→填目标部门（ErpHrDepartment）→可选调整职位与直属上级→设调动生效日期→提交→系统更新 `ErpHrEmployee.departmentId/positionId/superiorId`→如有劳动合同标记原合同→TERMINATED、创建新合同；后置=员工新部门/职位已生效；异常=调动日期与已有休假冲突时告警；跨域=成本中心变更可能影响项目工时归集。**无独立 state-machine 设计文档**（仅 use-case 表），调动为单步直接更新，非多态状态机——本计划以 use-case 为权威源。
- **后继触发条件已满足**：extended-roadmap M3 Non-Goal boundary 明示「员工调动段归 successor」，项目处于「业务逻辑深化与运营成熟度收尾」阶段（AGENTS.md），UC-HR-08 为定义完整的剩余 UC。胜任力段已由 plan `2026-07-07-1100-2` 完成，调动段为同 UC 拆分的剩余段。
- **实体与字段就绪（零 ORM）**：`module-hr/model/app-erp-hr.orm.xml:278` `ErpHrEmployee` 已含 `departmentId`(:284,→ErpHrDepartment)/`positionId`(:285,→ErpHrPosition)/`superiorId`(:287,自引用 ErpHrEmployee)/`employmentStatus`(:294,dict `erp-hr/employment-status`)。调动仅更新既有列，**无需新增实体/列**。
- **合同实体就绪**：`ErpHrEmploymentContract`(:420 区) 已含 `status`(:438,dict `erp-hr/contract-status`=ACTIVE/EXPIRED/TERMINATED/SUSPENDED)+`contractType`+`startDate`/`endDate`+`employeeId`。`IErpHrEmploymentContractBiz`（`module-hr/erp-hr-dao/.../biz/`）+ `ErpHrEmploymentContractBizModel`（service）codegen 已生成。UC-HR-08「原合同→TERMINATED、创建新合同」可用既有实体与接口。
- **休假实体就绪（异常告警）**：`ErpHrLeaveRequest`(:476) 已含 `status`(dict `erp-hr/leave-status`=DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED)+`employeeId`+日期。`IErpHrLeaveRequestBiz` 已生成。UC-HR-08「调动日期与已有休假冲突时告警」可查 APPROVED 休假区间相交。
- **BizModel 现状（空壳 CRUD）**：`module-hr/erp-hr-service/.../entity/ErpHrEmployeeBizModel.java:11` `extends CrudBizModel<ErpHrEmployee> implements IErpHrEmployeeBiz`，零自定义方法——无调动服务。`IErpHrEmployeeBiz`（dao 层）为 codegen 空壳。`ErpHrLeaveRequestBizModel` 同为空壳 CRUD（非状态机）。
- **域内范式（镜像基准）**：HR 域既有 BizModel 自定义动作范式见 `ErpHrShiftAssignmentBizModel`（3.8 assignSingle/assignBatch `@BizMutation` + 跨实体校验 + ErrorCode 守门）与 `ErpHrShiftBizModel.onLeaveApproved`（休假审批通过后跨实体回写 `ErpHrShiftAssignment` 联动——`IErpHrShiftBiz` 接口声明该方法）。调动方法应对齐此范式（`@BizMutation` + 校验守门 + 跨实体经 `I*Biz` 接口 + ErrorCode）。`IErpHrDepartmentBiz`/`IErpHrPositionBiz`（dao 层）已生成，可注入校验目标部门/职位。
- **ErrorCode/Constants 基线**：`ErpHrErrors`/`ErpHrConstants`/`ErpHrConfigs`（`module-hr/erp-hr-service/.../`）已存在，可新增调动相关 ErrorCode + config 键。
- **保护区域**：纯服务层 + AMIS view + ErrorCode/config，**无 ORM/契约/认证变更**，非 `ask-first`。属 `plan-first`（跨实体服务层 + 前端 + 测试，>5 文件）。

剩余差距：(1) `ErpHrEmployeeBizModel` 无调动 `@BizMutation`；(2) UC-HR-08 合同处理（原合同 TERMINATED + 新合同）+ 休假冲突告警未实现；(3) AMIS 员工页无调动入口。

## Goals

- 在 `ErpHrEmployeeBizModel` 落地 `transferEmployee(employeeId, targetDepartmentId, targetPositionId?, targetSuperiorId?, effectiveDate, handleContract?)` `@BizMutation`——校验目标部门/职位存在 + 员工状态可调动 → 更新 `departmentId`/`positionId`/`superiorId` → 按 `handleContract`（或 config-gated）处理合同（原 ACTIVE 合同→TERMINATED + 创建新 ACTIVE 合同承袭类型/期限并更新部门）。
- 落地调动生效日期与 APPROVED 休假区间冲突告警（config-gated，告警不阻塞——对齐 use-case「告警」非「拒绝」）。
- 在 AMIS 员工页补「调动」动作按钮 + 表单（目标部门/职位/上级/生效日期/合同处理选项）。
- 对齐 owner doc（use-cases.md UC-HR-08 标注已实现 + 调动段状态）。

## Non-Goals

- **不**新增调动单实体（如 `ErpHrEmployeeTransfer` 头-行审批文档）——use-case 为单步直接更新「提交→系统更新」，无审批/多态状态机；调动审计经平台 `NopSysChangeLog`（`tagSet="audit"`， CrudBizModel update 管道自动记录字段变更）。调动单实体 + 审批工作流归 Deferred（触发条件=调动需人工审批留痕或批量调动报表时，属 ORM ask-first）。
- **不**实现调动审批工作流（use-case 无审批语义；如需审批归上述调动单实体 Deferred）。
- **不**实现跨域项目工时归集联动（use-case「成本中心变更可能影响项目工时归集」为 projects 域读侧关注——projects 工时归集在读员工部门时取最新值，HR 调动无需主动推送；归 projects 域后继）。
- **不**实现调动历史查询服务（经平台 `NopSysChangeLog` 按 entity=ErpHrEmployee + 字段=departmentId 查询即可，无需专用聚合）。
- **不**触及 `module-hr/model/*.orm.xml`（纯服务层 + view，零 ORM 变更）。
- **不**承接胜任力段（已由 1100-2 完成）。

## Task Route

- Type: `implementation-only change`（HR 域业务逻辑补齐：自定义 `@BizMutation` + 跨实体合同/休假 + AMIS 动作；零 ORM/契约变更）
- Owner Docs: `docs/design/human-resource/use-cases.md` UC-HR-08、`docs/design/human-resource/README.md`、`docs/design/human-resource/state-machine.md`（跨实体联动范式参考）
- Skill Selection Basis: `nop-backend-dev`（自定义 `@BizMutation` 动作、跨实体 `I*Biz` 注入、ErrorCode、config 键、校验守门）；`nop-frontend-dev`（AMIS 员工页调动按钮 + 表单 drawer）。既有 HR 域 BizModel 范式（3.8 排班/休假联动）已验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 无 ORM 变更，故无 ask-first 保护区域门控

## Execution Plan

### Phase 1 - 调动后端服务

Status: completed
Targets: `module-hr/erp-hr-service/.../entity/ErpHrEmployeeBizModel.java`、`IErpHrEmployeeBiz`、`ErpHrErrors`、`ErpHrConstants`/`ErpHrConfigs`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`：合同处理口径裁决——`handleContract` 参数三态（`AUTO`/`YES`/`NO`，缺省 `AUTO`）：`AUTO`=按 config `erp-hr.transfer-auto-handle-contract`（默认 true）+ 仅当员工存在 ACTIVE 合同时处理；`YES`=强制处理（无 ACTIVE 合同时创建新合同）；`NO`=跳过合同处理仅更新部门。原合同→TERMINATED + 创建新 ACTIVE 合同（承袭 `contractType`/期限类型，`startDate`=effectiveDate，部门取新部门）。记录选择 + 替代方案（rejected：仅终止不新建——破坏员工合同连续性；rejected：要求调动单实体——超出 use-case 单步语义）+ 残留风险（合同终止/新建属法律效力变更，无审批——已 config-gated 可关）。
      - Skill: `nop-backend-dev`
- [x] `Add`：`ErpHrConstants`/`ErpHrConfigs` 增 config 键 `erp-hr.transfer-auto-handle-contract`（默认 true）+ `erp-hr.transfer-leave-conflict-warn`（默认 true，告警不阻塞）；`ErpHrErrors` 增 `ERR_EMPLOYEE_NOT_TRANSFERABLE`（employmentStatus 不可调动，如 RESIGNED/TERMINATED 等）/`ERR_TRANSFER_TARGET_DEPT_NOT_FOUND`/`ERR_TRANSFER_TARGET_POSITION_NOT_FOUND`（镜像域内 ErrorCode 范式，不跨域 import）。
      - Skill: `nop-backend-dev`
- [x] `Add`：`transferEmployee(@BizMutation Long employeeId, @BizMutation Long targetDepartmentId, @BizMutation Long targetPositionId, @BizMutation Long targetSuperiorId, @BizMutation LocalDate effectiveDate, @BizMutation String handleContract)` —— 校验：(1) 员工存在且 `employmentStatus` 可调动（非 RESIGNED/TERMINATED 等）；(2) 目标部门存在（注入 `IErpHrDepartmentBiz` 或 `dao()` 查询）；(3) 目标职位存在且归属目标部门（若传入）；(4) 休假冲突告警（config-gated，查 `IErpHrLeaveRequestBiz` APPROVED 休假与 effectiveDate 区间相交——相交则记日志/返回 warning，**不抛异常不阻塞**，对齐 use-case「告警」）。更新 `departmentId`/`positionId`/`superiorId`；按 `handleContract` Decision 处理合同（经 `IErpHrEmploymentContractBiz` 终止原 ACTIVE 合同 status=TERMINATED + 新建 ACTIVE 合同）。经 `CoreMetrics.currentDate()`/平台时间 API（非 `LocalDate.now()`）。
      - Skill: `nop-backend-dev`
- [x] `Add`：`IErpHrEmployeeBiz` 接口声明 `transferEmployee`（dao 层接口同步，对齐域内既有自定义动作声明范式）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `transferEmployee` 经 GraphQL 可调用；不可调动状态员工抛 `ERR_EMPLOYEE_NOT_TRANSFERABLE`；目标部门/职位不存在抛对应 ErrorCode；调动后 `departmentId`/`positionId`/`superiorId` 更新（单元测试断言）。
- [x] 合同处理：`handleContract=AUTO` 且存在 ACTIVE 合同时原合同 status=TERMINATED + 新合同 status=ACTIVE + startDate=effectiveDate；`handleContract=NO` 时不触及合同。
- [x] 休假冲突告警 config-gated 生效（相交时不阻塞调动，记录 warning）。

### Phase 2 - AMIS 员工页调动入口

Status: completed
Targets: `module-hr/erp-hr-web/.../ErpHrEmployee.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 后端 `@BizMutation` 可用

- [x] `Add`：员工列表/编辑页补「调动」动作按钮——点击打开 drawer/dialog 表单（目标部门 selector→ErpDepartment / 目标职位 selector→ErpPosition / 目标上级 selector→ErpEmployee / 生效日期 input-date / 合同处理 select=AUTO|YES|NO），提交调 `/api/GenericApi` GraphQL `ErpHrEmployee__transferEmployee`；成功后刷新员工详情。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 员工页「调动」按钮可打开表单，提交后员工部门/职位/上级更新；合同处理选项传入后端生效。

### Phase 3 - 测试与 owner-doc 对齐

Status: completed
Targets: `module-hr/erp-hr-service/src/test/...`、`docs/design/human-resource/use-cases.md`、`docs/design/human-resource/README.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 1/2 落地

- [x] `Proof`：`JunitAutoTestCase` + GraphQL `request.json5` 覆盖——正常调动（部门/职位/上级更新）+ 不可调动状态守门 + 目标部门/职位不存在守门 + 合同处理 AUTO（原 TERMINATED + 新 ACTIVE）+ 合同处理 NO（不触及）+ 休假冲突告警不阻塞。指定测试策略与命令 `mvn test -pl module-hr -am`。
      - Skill: `nop-testing`
- [x] `Add`：`docs/design/human-resource/use-cases.md` UC-HR-08 末尾标注「实现：transferEmployee @BizMutation（直接更新，无审批；调动单实体 + 审批归 Deferred）」；`README.md` §员工管理标注调动已落地（附方法名）；`docs/backlog/extended-roadmap.md` M3 Non-Goal boundary「员工调动段」由 successor 改 ✅ done（附本计划指针）。
      - Skill: none

Exit Criteria:

- [x] `mvn test -pl module-hr -am` 新增测试 0 failures/0 errors，既有 HR 测试无回归。
- [x] owner doc + roadmap 标注与实现一致（无样板填充）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c18a5cd1ffeA4u9jZSMnf2t1H) because 1 BLOCKER + 3 NOTE：
  - B1：Baseline 虚称 `ErpHrLeaveRequestBizModel`「休假审批状态机 + 跨实体联动 onLeaveApproved」——实时核实 `ErpHrLeaveRequestBizModel` 为 15 行空壳 CrudBizModel（零自定义方法），`onLeaveApproved` 实际在 `ErpHrShiftBizModel.java:128`（接口声明 `IErpHrShiftBiz:55`），跨实体回写 `ErpHrShiftAssignment`。已更正：范式基准改为 `ErpHrShiftAssignmentBizModel`（assignSingle/assignBatch + ErrorCode 守门）+ `ErpHrShiftBizModel.onLeaveApproved`（跨实体联动，正确归属）；明示 `ErpHrLeaveRequestBizModel` 同为空壳。
  - N1：`erp-hr/leave-status` 字典为 5 值（DRAFT/SUBMITTED/APPROVED/REJECTED/**CANCELLED**），非 4 值——已补全 CANCELLED。
  - N2：Phase 3 `mvn test -pl module-hr -am` 与 Closure Gates 轻微重复——Phase 3 为测试阶段，模块级 `mvn test` 即其可观察交付，保留（边界合规）。
  - N3：`IErpHrDepartmentBiz`/`IErpHrPositionBiz` 已确认存在（dao 层），跨实体注入方案可行——已补入范式段。
  - 正面确认（无需变更）：UC-HR-08 设计匹配（use-cases.md:87-97）；后继触发条件已满足（extended-roadmap:54「员工调动段归 successor」+ 1100-2 completed）；ErpHrEmployeeBizModel 空壳/ErpHrEmployee 列就绪/ErpHrEmploymentContract(contract-status 4 值)+IErpHrEmploymentContractBiz 就绪/ErpHrLeaveRequest+IErpHrLeaveRequestBiz 就绪/ErpHrErrors·Constants·Configs 存在/无既有调动实现/零 ORM 非 ask-first/Rule 9 Decision 完整/反松弛合规/执行规则 7 全工作区验证在 Closure Gates/无活缺陷藏匿 Follow-up。
- Independent draft review iteration 2: `acceptable as-is` (MISSION_DRIVER draft-review pass) because 格式合规（全部必需段 + 模板字段名正确 + 三阶段 Status/Targets/Skill/Item Types/Prereqs/Exit Criteria 齐全）、命名约定合规、范围单一结果表面（调动服务 + AMIS 入口）无 scope creep、Non-Goals 与 Deferred 均带触发条件（规则 4/91 合规）、Exit Criteria 可测可验、Closure Gates 定义可验证结束证据（含 154 模块 install + HR 域 mvn test + view.xml well-formed + 独立结束审计）、Decision 记录选择 + 拒绝方案 + 残留风险（规则 9 合规）、技能逐阶段逐项声明（规则 8 合规）、规则 7 边界合规（Phase 3 模块级 mvn test 为测试阶段可观察交付，非重复全仓验证）。未发现 Blocker/Major；Minor（不阻塞）：`handleContract` 字符串参数可在实现时改 enum——实现细节，非计划级缺陷。

## Closure Gates

> 本计划为纯服务层 + view（零 ORM/契约/认证变更），结束前运行一次完整仓库验证。

- [x] 范围内行为完成（transferEmployee 后端 + 合同处理 + 休假告警 + AMIS 入口）
- [x] 相关文档对齐（use-cases.md UC-HR-08 + README + extended-roadmap 调动段状态）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-hr -am`（HR 域无回归）+ 员工 view.xml well-formed
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 调动单实体 + 审批工作流（ErpHrEmployeeTransfer 头-行）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: use-case UC-HR-08 为单步直接更新「提交→系统更新」，无审批/多态状态机语义。调动审计经平台 `NopSysChangeLog`（CrudBizModel update 管道自动记录 departmentId/positionId/superiorId 字段变更）可追溯。调动单实体 + 审批工作流需新增 ORM 实体（ask-first）+ xwf 接线，超出本期「服务层调动」结果面。
- Successor Required: `yes`
- Trigger Condition: 当调动需人工审批留痕（如跨部门调动需双方部门主管审批）或需批量调动报表/历史聚合查询时，经独立 ORM ask-first 计划承接。

### 跨域项目工时归集联动

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: use-case「成本中心变更可能影响项目工时归集」为 projects 域读侧关注——projects 工时归集（2.6）在读员工部门时取最新值，HR 调动无需主动推送。属 projects 域后继。
- Successor Required: `no`

## Closure

Status Note: 全部 3 Phase 落地完成。`transferEmployee` `@BizMutation`（合同三态处理 + 休假告警 config-gated）+ AMIS 调动 drawer 入口 + 9 测试 cases（正常/守门/合同/告警）。验证全绿：154 模块 install + HR 域 89 tests 0 failures + view.xml well-formed。调动单实体 + 审批工作流归 Deferred。独立结束审计已由新会话子代理执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文），执行于 2026-07-08。逐项核实实时仓库（read/grep/glob + view.xml well-formed 解析），非采信 [x] 标记。
- Audit Verdict: approved（通过）
- Anti-Hollow 复核：`transferEmployee` 完整实现校验守门（3 ErrorCode 抛出）+ 合同三态处理（AUTO/YES/NO，终止原 ACTIVE + 新建 ACTIVE）+ 休假冲突告警（config-gated，不阻塞，仅 LOG.warn），无空体/return null/吞异常；4 个 `I*Biz` 跨实体注入非 private，均在运行时被调用。9 测试 case 真实断言（部门/职位/上级更新 + 状态守门 + 部门/职位不存在守门 + 合同 AUTO/NO/YES + 休假冲突不阻塞 + PROBATION 允许）。
- Five-point 一致性：Plan Status=completed / 三 Phase Status=completed / 三 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure evidence 具体且非占位符——全部一致。
- Deferred 诚实性：调动单实体 + 审批工作流与跨域项目工时归集均带触发条件，无活缺陷藏匿。
- Evidence:
  - **后端**：`module-hr/erp-hr-service/.../entity/ErpHrEmployeeBizModel.java` 含 `transferEmployee` `@BizMutation` + `@SingleSession`，注入 `IErpHrDepartmentBiz`/`IErpHrPositionBiz`/`IErpHrEmploymentContractBiz`/`IErpHrLeaveRequestBiz`（非 private），校验守门（3 ErrorCode）+ 合同三态处理（AUTO/YES/NO，config-gated）+ 休假冲突告警（config-gated，不阻塞）。
  - **接口**：`module-hr/erp-hr-dao/.../biz/IErpHrEmployeeBiz.java` 声明 `transferEmployee`（`@BizMutation` + `@Name` + `IServiceContext` 末参）。
  - **ErrorCode/Constants/Configs**：`ErpHrErrors` 增 3 调动 ErrorCode；`ErpHrConstants` 增 2 config 键 + 3 合同处理三态常量 + 不可调动状态 + 合同状态；`ErpHrConfigs` 增 2 helper 方法。
  - **AMIS**：`module-hr/erp-hr-web/.../ErpHrEmployee/ErpHrEmployee.view.xml` 增 `transfer` form（5 custom cells）+ `<simple name="transfer">` page（调 `@mutation:ErpHrEmployee__transferEmployee`）+ row action「调动」drawer 按钮；XML well-formed。
  - **测试**：`module-hr/erp-hr-service/src/test/.../TestErpHrEmployeeTransfer.java` 9 cases，全绿。
  - **验证**：`mvn clean install -DskipTests`（154 模块）BUILD SUCCESS；`mvn test -pl module-hr/erp-hr-service -am` BUILD SUCCESS / 89 tests / 0 failures / 0 errors。
  - **owner-doc**：`use-cases.md` UC-HR-08 标注实现状态；`README.md` §员工管理标注调动已落地；`extended-roadmap.md` 调动段 successor→✅ done。

Follow-up:

- 调动单实体 + 审批工作流（Deferred，触发条件=调动需人工审批留痕或批量调动报表时，经独立 ORM ask-first 计划承接）。
