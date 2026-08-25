# ck-hr-org — human-resource「组织与员工」切片实现代码检查报告

> 工作项：C6.1。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-hr/erp-hr-service/src/main/java` 组织与员工切片 13 个手写生产文件——组织（`entity/ErpHrDepartmentBizModel` 含 findDepartmentTree）+ 员工（`entity/ErpHrEmployeeBizModel` 含 transferEmployee 入口/PII 脱敏/countReferences + `processor/ErpHrEmployeeTransferEmployeeProcessor`）+ 合同（`entity/ErpHrEmploymentContractBizModel` 含 scanExpiringContracts/renew + `processor/ErpHrEmploymentContractExpireOverdueContractsProcessor` + `job/ErpHrContractExpiryJob`）+ 招聘（`entity/ErpHrRecruitmentBizModel` 状态机 5 mutation + `processor/ErpHrRecruitmentHireProcessor`）+ 职位（`entity/ErpHrPositionBizModel`）+ 状态机 Bean 2 类（`ErpHrEmployeeStateMachine`/`ErpHrEmploymentContractStateMachine`）+ 根常量 3 类（`ErpHrConstants`/`ErpHrConfigs`/`ErpHrErrors` 相关段）。跨文件核实：`module-hr/model/app-erp-hr.orm.xml`（5 实体 header/UK/versionProp/dict）、`_vfs/erp/hr/beans/app-service.beans.xml`（接线）、`app-erp-all/_vfs/nop/job/conf/erp-hr-contract-expiry.job.yaml`（D4）、切片 5 实体 `*.xbiz.xml`（override 检查）、`erp-hr-web` org-chart.flux.yaml 与 `ErpHrEmployee.view.xml`（消费面抽查）、notify 子系统 `ErpSysNotificationNotifyProcessor`/`NotificationMergeCoordinator`（best-effort 与频控）、测试 3 文件（Transfer/RecruitmentEngine/EmployeeReferences + ContractExpiry）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。切片 13 文件逐行深读 + 平台源码实证 2 处（`GraphQLTransactionOperationInvoker`/`DefaultExecutionFunctionInvoker`——@BizMutation 事务仅在 GraphQL mutation 入口生效；`ReflectionGraphQLTypeFactory` L97-105——@Name 无 @Optional/@Nullable 即 NonNull mandatory）+ orm/beans/job-yaml/xbiz 接线核对 + arm-index 复用裁决。
> 切片边界：考勤/休假/薪酬/排班/工时/调研/胜任力（attendance/leave/salary/shift/timesheet/survey/competency/gap/devplan 族）归 C6.2 勿重复；`ErpHrReportBizModel`（薪酬报表）归 C6.2；`ErpMdEmployee`（master-data 业务经办人）归 md 域（本报告「验证为正确」节裁决两实体设计分离）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-hr-001（D8）部门/职位删除无任何引用守卫——删除含在职员工的部门、有编制的职位后 Employee/Recruitment/子部门悬挂引用静默产生

- **控制点**：`app/erp/hr/service/entity/ErpHrDepartmentBizModel.java`（全类 103 行裸 `CrudBizModel<ErpHrDepartment>`，零 `defaultPrepareDelete`/引用预览覆盖）+ `app/erp/hr/service/entity/ErpHrPositionBizModel.java`（L12-18 裸桩，无任何守卫）对照同文件族正向示范 `ErpHrEmployeeBizModel#countReferences`（L119-132 为员工删除提供合同/工时/薪酬/考勤/休假五类引用计数，`ErpHrEmployee.view.xml:328-350` 前端删除预览消费）
- **证据**：`ErpHrEmployee.departmentId/positionId/superiorId`、`ErpHrRecruitment.departmentId/positionId`、`ErpHrDepartment.parentId` 均为无 FK 约束的 BIGINT 弱引用（orm.xml 实证，仅 to-one join 无 onDelete）。通过 `ErpHrDepartment__delete_` 删除一个仍有 10 名在职员工的部门：逻辑删除（`useLogicalDelete` delVersion）后 join 查询自动过滤已删行——员工部门列显示空、组织树中该节点消失但其子部门 parentId 指向已删节点（`findDepartmentTree` L71 `nodeMap.containsKey(pid)` 不命中 → 子部门被提升为根节点，树结构静默重组）；招聘单 positionId 悬挂后 `hire` 联动（`ErpHrRecruitmentHireProcessor` L65-66）把悬挂 departmentId/positionId 直接写入新员工。职位删除同型（员工 positionId/招聘 positionId 悬挂）。员工删除有五类引用预览兜底，部门/职位删除**零拦截零告警**——不对称。
- **问题**：组织主数据的引用完整性无守卫。触发条件极常见（清理测试部门/组织重构时误删在用部门）。UC-HR-01 异常声明「部门/职位不存在时提示先创建」体现了引用敏感性的 owner 意识，删除方向的对称面缺失。
- **建议修复方向**：`ErpHrDepartmentBizModel`/`ErpHrPositionBizModel` 覆写 `defaultPrepareDelete`（或 `doBeforeDelete`）：统计 `Employee.departmentId`（含 superiorId 反查）/`Recruitment.departmentId|positionId`/子部门 `parentId` 引用计数，非零抛 `NopException`（如 `ERR_DEPT_HAS_EMPLOYEES`）；部门删除至少要求先移走子部门与在职员工。
- **arm-index 裁决**：新增（grep arm-index「部门删除/组织删除 引用/Department delete」零命中；md 域 `ErpMdOrganizationReferenceChecker` SPI 只覆盖 `ErpMdEmployee.orgId`/`ErpMdWarehouse.orgId`，hr 部门无对应实现——`visible-on-patterns.md:305` 注记「下游域（如 ErpHrDepartment.orgId）的引用计数由各自域追加实现或经 successor 补齐」自证缺口）。

### P2-CK-hr-002（D8）调动跨组织时员工 orgId 不随目标部门同步——员工 orgId 与所属部门 orgId 失配并沿续签合同扩散

- **控制点**：`app/erp/hr/service/processor/ErpHrEmployeeTransferEmployeeProcessor.java#transferEmployee`（L89 `employee.setDepartmentId(targetDept.getId());`——只写部门不写 org）对照 `requireTargetDepartment`（L127-137，不校验 `targetDept.getOrgId()` 与 `employee.getOrgId()` 一致）+ `newContractFrom`（L258 `c.setOrgId(employee.getOrgId())`——续签合同沿用失配 orgId）
- **证据**：`ErpHrDepartment.orgId` 与 `ErpHrEmployee.orgId` 均为业务组织列（orm 实证），员工 UK `(code,orgId)`。员工从组织 A 的部门调到组织 B 的部门：部门更新成功、orgId 仍为 A——之后按 orgId 聚合的薪酬归集/看板把该员工计入 A；调动续签的新合同 orgId 也记 A 而其员工实际在 B 部门。单组织部署无影响（多组织隔离是 ORM 显式设计维度，同 P2-CK-mfg-007 降级逻辑）。
- **问题**：D8 orgId 透传缺失（调动场景）。修复时机与 pur-003 族守卫修复解耦，可先行。
- **建议修复方向**：`transferEmployee` 在 `setDepartmentId` 同时 `employee.setOrgId(targetDept.getOrgId())`（或校验目标部门 orgId 与员工一致、不一致抛错——按产品语义二选一，owner doc UC-HR-08 未明确跨组织调动是否允许，修复时需先裁决）。
- **arm-index 裁决**：新增（grep「调动 orgId/transferEmployee orgId」零命中）。

### P2-CK-hr-003（D9/D8）findDepartmentTree 双表全量加载 + 硬编码 limit 5000 静默截断 + 无 orgId 隔离 + empCount 无雇佣状态口径

- **控制点**：`app/erp/hr/service/entity/ErpHrDepartmentBizModel.java#findDepartmentTree`（L40 `deptQuery.setLimit(5000)` + L44 `empQuery.setLimit(5000)` 双表全实体加载；L47-52 员工计数循环**无 employmentStatus 过滤**；两个 QueryBean 均**无 orgId filter**）
- **证据**：① 部门 >5000 或员工 >5000 时静默截断——被截断的部门不出现在树中、被截断的员工不进 empCount，无警告无分页（对照 org-chart.flux.yaml L61 直接展示 `(node?.empCount ?? 0)人`，用户无从发现少计）；② 为数员工数加载**全部员工实体**（含 PII 大字段），仅需 departmentId 计数——`eq("departmentId",x)+findCount` 或 GROUP BY 下推即可（同文件族正确示范：`ErpHrEmployeeBizModel#warnIfLeaveConflict` L173 用 `findCount`）；③ empCount 把所有状态员工（未来含 RESIGNED/RETIRED——当前死状态无 writer 暂不可触发，successor 填充 writer 后即失真）计入「人数」；④ 跨组织部门混合成一张树（orgId 隔离缺失，同 P2-CK-fin2-007/P2-CK-mfg-007 orgId 族）。
- **问题**：组织树查询是 org-chart 看板唯一数据源（`@query:ErpHrDepartment__findDepartmentTree`），规模增长后每次打开看板全量双表载入；截断与跨组织混合在多组织/大组织下产生静默错误数据。
- **建议修复方向**：员工计数改 GROUP BY departmentId 聚合下推（或 findCount per dept 一次性 in 查询）；limit 5000 改分页累计或超限告警；补 orgId 过滤（从 IUserContext 取当前组织）；empCount 口径过滤 `employmentStatus in (ACTIVE,PROBATION)` 并在 javadoc 声明。
- **arm-index 裁决**：新增（带同族注记——orgId 维度同 P2-CK-fin2-007/P2-CK-mfg-007 族 hr 站点；grep「findDepartmentTree/组织树 limit」arm-index 零命中）。

### P2-CK-hr-004（D5/D3，同型 P1-CK-pur-003 族）通用 CRUD update 无状态守卫——HIRED 招聘单/终态合同/员工雇佣状态可被通用 mutation 直接改写

- **控制点**：`app/erp/hr/service/entity/` 下 `ErpHrEmployeeBizModel`/`ErpHrRecruitmentBizModel`/`ErpHrEmploymentContractBizModel`/`ErpHrDepartmentBizModel`/`ErpHrPositionBizModel` 全部为裸 `CrudBizModel`（仅 Recruitment/Contract 有 `defaultPrepareSave` 兜底 businessDate/status，零 `defaultPrepareUpdate/Delete` 守卫）——`update_`/`delete_` 对任何状态实体开放
- **证据**：① HIRED 招聘单可经 `ErpHrRecruitment__update_` 直接改 `employeeId`（破坏 hire 回写闭环）/`status`（绕过状态机写任意值）；② TERMINATED/EXPIRED 合同可改 `status` 回 ACTIVE + 改 `endDate`（终态复活，绕过 `ErpHrEmploymentContractStateMachine` 全部 assertCan*）；③ **员工 `employmentStatus` 可经 `ErpHrEmployee__update_` 直接写 RESIGNED/TERMINATED/RETIRED**——这正是 P1-MA2-039「三终态零 writer」Deferred 裁决的旁路通道：CRUD 一行就把员工置离职而合同不终止、账号不停用（owner doc §场景 D 联动全缺），使「死状态不可达」的状态机前提失效；④ 员工可改 `departmentId` 绕过 transferEmployee 的目标部门校验。
- **问题**：同 P1-CK-pur-003/P1-CK-sal-004/P1-CK-inv-005/P2-CK-mfg-006 全域同型——命名动作链的状态守卫可被通用 CRUD 旁路。hr 站点按约定登记为同型 finding（不复用展开）；③ 是本站点独有的状态机前提破坏点，修复优先。
- **建议修复方向**：终态守卫（`defaultPrepareUpdate`/`defaultPrepareDelete` 校验：Recruitment HIRED/CLOSED/REJECTED 不可 update/delete 关键字段；Contract 终态不可 update；Employee employmentStatus 变更仅允许经未来命名 mutation，CRUD 侧拒绝三终态写入）。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，hr 站点新增计数）。

### P2-CK-hr-005（D8）hire 联动不校验招聘单 departmentId/positionId 存在性——悬挂部门/职位直接写入新员工主数据

- **控制点**：`app/erp/hr/service/processor/ErpHrRecruitmentHireProcessor.java#createEmployeeFromRecruitment`（L65-66 `employee.setDepartmentId(rec.getDepartmentId()); employee.setPositionId(rec.getPositionId());`——直接透传无存在性校验）+ 对照同切片调动链的正向示范 `ErpHrEmployeeTransferEmployeeProcessor#requireTargetDepartment`（L127-137 目标部门不存在抛 `ERR_TRANSFER_TARGET_DEPT_NOT_FOUND`）
- **证据**：招聘单创建（`ErpHrRecruitmentBizModel#defaultPrepareSave` L57-66 仅兜底 businessDate/status）与 hire 联动全链无部门/职位存在性校验。招聘单创建后其部门/职位被删除（P1-CK-hr-001 无守卫可删）或招聘单创建时即填错 ID：hire 时新员工的 departmentId/positionId 悬挂（get 无 FK），入职员工在组织树中不可见（join 过滤）。UC-HR-01 前置「ErpHrDepartment、ErpHrPosition 已存在」+ 异常「部门/职位不存在时提示先创建」——入职联动路径未落地该语义。
- **问题**：入职是员工主数据的创建源头，脏组织引用从源头进入员工档案且无自愈通道（HR 需手动调岗修正）。
- **建议修复方向**：`hire` 前置校验 `rec.getDepartmentId()/getPositionId()` 存在（非空时查库，复用 `IErpHrDepartmentBiz/IErpHrPositionBiz` 范式），缺失抛 `NopException`（可新增 `ERR_RECRUITMENT_DEPT_NOT_FOUND` 或复用调动领域码语义）。
- **arm-index 裁决**：新增（grep「hire departmentId 校验/招聘 部门 悬挂」零命中；P2-RC-010 只覆盖 UC-HR-05 状态回退维度）。

### P3-CK-hr-006（D10）hire/transferEmployee 关键日期参数 null 时 NPE 而非业务错误——直接 Java 调用路径暴露

- **控制点**：`app/erp/hr/service/processor/ErpHrRecruitmentHireProcessor.java#createContractForNewEmployee`（L86 `contract.setCode("HIRE-" + rec.getId() + "-" + hiredDate.toString());`——hiredDate null 即 NPE）+ `ErpHrRecruitmentHireProcessor#hire` L40 `rec.setHiredDate(hiredDate)` 亦透传 null + `ErpHrEmployeeTransferEmployeeProcessor#buildSuccessorCode`（L277 `String base = "TRF-" + employeeIdStr + "-" + effectiveDate.toString();`——effectiveDate null 且合同处理启用（`DEFAULT_TRANSFER_AUTO_HANDLE_CONTRACT=true`，`ErpHrConfigs` L43）即 NPE）
- **证据**：生产面已收窄——平台实证 `ReflectionGraphQLTypeFactory`（nop-entropy `.../graphql/core/reflection/ReflectionGraphQLTypeFactory.java` L97-105）：`@Name` 参数无 `@Optional`/`@Nullable` 即 `nonNullType + mandatory=true`，GraphQL 层拒绝缺参/显式 null；`IErpHrRecruitmentBiz.hire`/`IErpHrEmployeeBiz.transferEmployee`/`IErpHrEmploymentContractBiz.renew` 接口三参数均无 @Optional。但直接 Java 调用（测试/未来 job/内部编排——本切片 job 直调 biz 已是既有范式）不经过 GraphQL 校验，NPE 以裸 NullPointerException 而非 `NopException` 业务错误码冒出。
- **问题**：防御性缺失（同 mfg-012 负数静默族的 null 维度）。测试面零覆盖（TestErpHrRecruitmentEngine 4 用例全部传非空日期）。
- **建议修复方向**：`hire`/`transferEmployee` 入口对 `hiredDate`/`effectiveDate` 做 null 前置校验抛 `NopException`（中文描述），或至少 `Objects.requireNonNull` 带参数名。
- **arm-index 裁决**：新增（grep「hiredDate null/effectiveDate NPE」零命中）。

### P3-CK-hr-007（D5/D6）renew 无 newEndDate 边界校验——可将合同到期日改为早于生效日/缩短/清空

- **控制点**：`app/erp/hr/service/entity/ErpHrEmploymentContractBizModel.java#renew`（L106-107 `contract.setStatus(stateMachine.renewTargetStatus()); contract.setEndDate(newEndDate);`——仅状态守卫 `assertCanRenew`（ACTIVE/EXPIRED），对 newEndDate 零校验）
- **证据**：① `newEndDate` 早于 `startDate`（orm L6 startDate mandatory 存量值）→ 合同区间倒序（endDate < startDate）写入；② `newEndDate` 早于当前 `endDate`（缩短合同）→ 「续签」语义下静默缩短，且若 newEndDate < today 则续签后立即又落入 `expireOverdueContracts` 的 `lt("endDate", now)` 扫描窗口（下轮 job 立即翻 EXPIRED——续了个寂寞）；③ `newEndDate` null 经 updateEntity 落库（endDate 可空列）→ ACTIVE 无期限合同意外制造（无固定期限合同应走 contractType=NO_FIXED_TERM 语义而非 endDate null）。
- **问题**：续签动作的「延长」语义无守卫，边界输入产生语义矛盾数据（部分由下轮 job 自愈，部分如区间倒序永久驻留）。
- **建议修复方向**：`renew` 前置校验 `newEndDate != null && (newEndDate.isAfter(contract.getEndDate()) || contract.getEndDate() == null) && newEndDate.isAfter(contract.getStartDate())`，违反抛业务错误码。
- **arm-index 裁决**：新增（P2-RC-087 只裁决多档预警单阈值维持，未覆盖 renew 入参边界；grep「renew endDate 校验」零命中）。

### P3-CK-hr-008（D3/D6，需求分歧只登记不裁决）renew 实现语义与 UC-HR-07 基本流程分歧（就地延长 vs 设计「新建合同+原合同 EXPIRED」）+「连续合同次数到无固定期限提示」未实现

- **控制点**：`app/erp/hr/service/entity/ErpHrEmploymentContractBizModel.java#renew`（L92-110：同一合同就地 `setStatus(ACTIVE) + setEndDate(newEndDate)`，不新建合同、原合同记录被改写）对照 owner doc `docs/design/human-resource/use-cases.md` UC-HR-07 基本流程 3（L82）：「HR 操作续签（**创建新合同**，原合同 endDate 不变但 status→EXPIRED）」+ 异常（L84）：「**连续合同次数到达无固定期限条件时系统提示**」
- **证据**：实现语义 = 就地延长（单合同记录覆盖式续签，历史期限信息丢失——原 endDate 被覆盖后不可追溯第二次固定期限合同起算点）；设计语义 = 链式新合同（原合同 EXPIRED 保历史，新合同独立 code——与 transferEmployee 的 terminate+successor 模式一致）。连续合同次数提示：grep 全 hr main 零「连续/无固定期限/NO_FIXED」判断逻辑，无连续次数计数字段。
- **问题**：代码与 owner doc 字面行为分歧（数据模型层面：续签历史可追溯性）。按检查纪律登记不裁决（UC-HR-07 措辞是流程描述非显式契约；两种语义业内均存在）。
- **建议修复方向**：owner doc 显式裁决二选一——(a) 实现改链式（复用 transfer 的 terminate+successor 范式 + 连续次数计数 + 次数达限提示）；(b) doc 修订为实现语义（就地延长 + 声明连续次数追踪归 successor）。
- **arm-index 裁决**：新增（P2-RC-087 覆盖 UC-HR-07 的 30/60/90 多档预警维度并裁决单阈值维持，本条是 renew 动作语义维度；grep「续签 新建合同/连续合同」arm-index 零命中）。

### P3-CK-hr-009（D5）招聘 mutation 入参无边界——makeOffer 负薪资直通合同月薪、scheduleInterview 的 interviewerId/date 无校验

- **控制点**：`app/erp/hr/service/entity/ErpHrRecruitmentBizModel.java#makeOffer`（L95-103：`offerSalary` 无 signum 校验直接 `rec.setOfferSalary(offerSalary)`）+ `#scheduleInterview`（L80-90：`interviewerId` 无存在性校验、`interviewDate` 无「不早于今天」校验）+ 下游 `ErpHrRecruitmentHireProcessor#createContractForNewEmployee`（L91-93 `if (rec.getOfferSalary() != null) contract.setMonthlySalary(rec.getOfferSalary());`——负数 offer 无损透传为合同月薪，成为薪酬核算输入）
- **证据**：负 offerSalary（误输符号）经 OFFERED→HIRED 全链无拦截，落库为合同 `monthlySalary=-15000`；C6.2 薪酬链是否对负月薪有二次校验未查（跨切片边界，已在剩余风险声明）。
- **问题**：入参边界缺失（同 P3-CK-mfg-012 负数族 hr 站点）。
- **建议修复方向**：`makeOffer` 前置 `offerSalary != null && offerSalary.signum() > 0` 校验抛业务错误；`scheduleInterview` 至少校验 interviewDate 非过去（语义弱可裁剪）。
- **arm-index 裁决**：新增（同族注记 P3-CK-mfg-012）。

### P3-CK-hr-010（D5/D10）targetSuperiorId 无存在性/自引用/成环校验——直接上级链可造环或指向不存在员工

- **控制点**：`app/erp/hr/service/processor/ErpHrEmployeeTransferEmployeeProcessor.java#transferEmployee`（L93-95 `if (targetSuperiorId != null) { employee.setSuperiorId(targetSuperiorId); }`——非空即写，无校验）
- **证据**：① 可设为不存在的员工 ID（悬挂引用）；② 可设为员工自己（self-superior）；③ 两次调用可造环（A 的上级 B、B 的上级 A）——当前 org-chart 不遍历 superior 链故无即时死循环，但按汇报线聚合的任何后续消费（薪酬审批流/组织层级报表）会循环。对照 `requireTargetDepartment/requireTargetPosition` 均有存在性校验，superior 维度缺失不对称。
- **问题**：边界校验缺失（当前无消费面放大，P3）。
- **建议修复方向**：非空时校验存在（`IErpHrEmployeeBiz` 反查）+ 拒绝自引用 + 沿 superior 链上溯 N 步（上限如 50）检测环。
- **arm-index 裁决**：新增（grep「superiorId 校验/上级 环」零命中）。

### P3-CK-hr-011（D1）BizModel 与 per-mutation Processor 双份死代码副本（legacy dup）——同一逻辑双源漂移风险

- **控制点**：`app/erp/hr/service/entity/ErpHrEmployeeBizModel.java`（L143-335：`requireTransferableEmployee`/`isTransferable`/`requireTargetDepartment`/`requireTargetPosition`/`warnIfLeaveConflict`/`resolveHandleContract`/`normalizeHandleContract`/`findActiveContract`/`newContractFrom`/`buildSuccessorCode` + 3 常量——与 `ErpHrEmployeeTransferEmployeeProcessor` L105-294 **逐方法重复**，而 `transferEmployee` L108-109 已完全委托 processor，BizModel 副本零生产调用方）+ `app/erp/hr/service/entity/ErpHrRecruitmentBizModel.java`（L139-201：`createEmployeeFromRecruitment`/`createContractForNewEmployee`/`generateEmployeeCode`/`extractFirst/LastName` 与 `ErpHrRecruitmentHireProcessor` L53-115 重复，`hire` L111 已委托）——`ErpHrEmploymentContractStateMachine` javadoc L45-46 自证「`ErpHrRecruitmentBizModel:149` 入职新建 legacy dup」
- **证据**：测试均经真实链路（`TestErpHrEmployeeTransfer` L263 注释「非直接静态调用」），BizModel 副本为死代码。历史：arm-index P1-MA2-039 时代 BizModel 版本是主路径，processor 化重构后未删旧副本。漂移实例风险：`buildSuccessorCode` 若未来只修一处（如再次调整截断策略），两份代码产生行为分叉且静态分析难以发现（当前两份一致——本报告逐行比对确认）。
- **问题**：可维护性（有行为影响潜质——双源漂移），无当前行为差异。
- **建议修复方向**：删除 BizModel 死副本（保留 `transferEmployee`/`hire` 委托入口 + `countReferences`/BizLoader 活代码），或提取共享 helper 由两处委托。
- **arm-index 裁决**：新增（grep「legacy dup/双份副本」arm-index 零命中）。

### P3-CK-hr-012（D4，同型 P3-CK-mfg-013/P3-CK-inv-021/P3-CK-sal-023/P3-CK-qa-019 家族）合同到期 job cron 三层键漂移——启用需同时配 3 个键，代码内层门控键与 nop-job 接线键不一致

- **控制点**：`app/erp/hr/service/job/ErpHrContractExpiryJob.java#execute`（L55-59 `String cron = resolveCronConfig(); if (StringHelper.isEmpty(cron)) { LOG.info("...skipped..."); return; }`——内层门控键 `erp-hr.contract-expiry-cron`（`ErpHrConstants` L255），**默认空=执行体永远跳过**）对照 `app-erp-all/_vfs/nop/job/conf/erp-hr-contract-expiry.job.yaml`（L2 `enabled: "@cfg:nop.job.erp-hr-contract-expiry.enabled|false"` + L7 `cronExpr: "@cfg:nop.job.erp-hr-contract-expiry.cron-expr|0 0 1 * * ?"`——外层调度双层键）
- **证据**：真正启用到期扫描需同时配置：`nop.job.erp-hr-contract-expiry.enabled=true`（job 被调度）+ `nop.job.erp-hr-contract-expiry.cron-expr`（默认值可用）+ `erp-hr.contract-expiry-cron` 非空（execute 不跳过）。运维按 job.yaml 直觉只配 `nop.job.*.enabled=true` 时：job 每日 01:00 被触发但 execute 每次打 info 日志即跳过——**到期扫描与通知静默不发生**，且 INFO 级日志易被忽略。README 配置点表无该键说明；javadoc（L25-26）声明的「erp-hr.contract-expiry-cron 配置为空时跳过（"不调度"语义）」与 nop-job enabled 键职责重叠（双层「不调度」语义叠加）。beans.xml 接线完整（L44 `erpHrContractExpiryJob` bean + job.yaml invoker bean/method 对上）——非孤立 job，纯键漂移。
- **问题**：同型家族（mfg-013/inv-021/sal-023/qa-019）hr 站点。声明键与实现键不一致 + 三层门控使启用路径反直觉。
- **建议修复方向**：与家族联合裁决——统一为 nop-job 双层键（enabled + cron-expr）并删除内层 `erp-hr.contract-expiry-cron` 门控；或 owner doc 明示三层键启用清单。内层跳过日志升 WARN。
- **arm-index 裁决**：同型登记（cron 键漂移家族，hr 站点新增计数）。

### P3-CK-hr-013（D2）ContractExpiryJob 顶层失败仅 LOG.error 无告警通道——到期扫描连续失败静默

- **控制点**：`app/erp/hr/service/job/ErpHrContractExpiryJob.java#execute`（L61-67 `try { runExpiryWarnings; runExpirations; } catch (Exception e) { LOG.error("erp-hr-contract-expiry-failed", e); }`——无 `IErpSysNotificationBiz` 异常告警派发）
- **证据**：job 主链任一异常（如 `scanExpiringContracts` DB 异常、`expireOverdueContracts` 非预期 RuntimeException）被顶层吞掉只留日志。对照：单条通知失败有隔离（L78-84 warn），mfg 域差异失败链有 `dispatchVarianceFailureAlert` G3 通知先例。合同到期无人提醒是合规敏感场景（中国劳动合同法到期续签时限），job 连续失败（如配置错误/DB 故障）对 HR 无任何系统可见信号。
- **问题**：失败无告警闭环（B1 弱变体——非悬挂状态而是调度静默失效）。
- **建议修复方向**：顶层 catch 增加 `notificationBiz.notify` 异常告警事件（复用 notify best-effort 语义，失败不二次抛）。
- **arm-index 裁决**：新增（带注记——job 镜像 `ErpCsEntitlementExpiryJob` 范式，cs 站点归 C7.1 届时同型合并；lesson 09 B1 决策树的 job 变体）。

### P3-CK-hr-014（D9）countReferences 全实体加载计数——五类引用统计各 findAllByQuery().size()

- **控制点**：`app/erp/hr/service/entity/ErpHrEmployeeBizModel.java#countByEmployee`（L135-139 `return daoProvider().daoFor(entityClass).findAllByQuery(q).size();`——为取 count 加载全实体，5 类各跑一次）
- **证据**：员工删除预览（前端 `ErpHrEmployee.view.xml:342` GraphQL 调用）每次触发 5 个全实体结果集（含 Salary/Attendance 明细实体），仅需行数。同文件 `warnIfLeaveConflict` L173 已示范 `findCount` 正确用法。
- **问题**：删除预览为低频操作但每次线性 IO 放大；高引用员工（数年考勤记录）结果集大。正确性无影响。
- **建议修复方向**：`findAllByQuery(q).size()` → `findCount(q)`（或 dao countByQuery）。
- **arm-index 裁决**：新增（grep「countReferences findAllByQuery」零命中；同型 P3-CK-mfg-014 全实体内存聚合家族远亲，但本条是 count 语义更简单）。

### P3-CK-hr-015（D5/D8）UC-HR-01「证件号码重复提示」未实现——idCardNo 无任何唯一性校验

- **控制点**：owner doc `use-cases.md` UC-HR-01 异常（L12）逐字「证件号码重复提示」对照 `module-hr/model/app-erp-hr.orm.xml`（`idCardNo` 列无 UK、无唯一索引）+ grep 全 `erp-hr-service/src/main` idCardNo 仅 2 处 BizLoader 脱敏（`ErpHrEmployeeBizModel` L343-346）零校验逻辑
- **证据**：两名员工可录入同一身份证号（CRUD save/update 均无查重）——重名同人入职场景无拦截，影响后续薪酬/社保唯一性（socialSecurityNo 同样无查重，一并注明）。
- **问题**：owner doc 异常声明未落地（D5 入参边界 + D8 主数据一致性）。触发需人工录入重号，中低频。
- **建议修复方向**：`defaultPrepareSave/Update` 增加 idCardNo 非空时查重（同 orgId 范围，命中抛业务错误码）。
- **arm-index 裁决**：新增（grep「证件号码重复/idCardNo 唯一」arm-index 零命中；A1.12 员工与组织批次（arm-index L429 摘要 1 P2 + 1 P1 复用）未见此维度）。

### P3-CK-hr-016（D6/D10）hire 联动新员工 gender 硬编码 "MALE" + 中文姓名拆分假设——入职主数据占位值直落档案

- **控制点**：`app/erp/hr/service/processor/ErpHrRecruitmentHireProcessor.java#createEmployeeFromRecruitment`（L61 `employee.setGender("MALE");` 无条件硬编码 + L58-59 `extractFirstName/extractLastName(rec.getCandidateName())`——L103-115 拆分逻辑为「首字符=姓，其余=名」中文假设，英文名 "John Smith" 拆成姓 "J" 名 "ohn Smith"）
- **证据**：招聘单（扁平实体）无 gender 字段（orm 实证 ErpHrRecruitment 列集）——联动时无处取性别，实现以常量 MALE 兜底占位。女性新员工档案性别恒错（除非 HR 手动修正）；英文名候选人姓名拆分语义错乱。
- **问题**：主数据源头占位值（用户可见错误数据，但可事后修正且 gender 为弱字段，P3）。
- **建议修复方向**：`ErpHrRecruitment` 增 candidateGender 字段（ORM 变更走 dual-agent）联动透传；短期先置 null（若列可空）而非错误的 MALE。
- **arm-index 裁决**：新增（grep「gender MALE/姓名拆分」零命中）。

### P3-CK-hr-017（D3，需求分歧只登记不裁决）hire 联动员工初始 employmentStatus=ACTIVE 与 recruitment.md §6.3「PROBATION」分歧——三份 owner doc 互相矛盾，实现取第三种（恒 ACTIVE 无入参）

- **控制点**：`app/erp/hr/service/processor/ErpHrRecruitmentHireProcessor.java#createEmployeeFromRecruitment`（L63 `employee.setEmploymentStatus(ErpHrConstants.EMPLOYMENT_ACTIVE);` 无入参控制）对照 owner doc `docs/design/human-resource/recruitment.md` §6.3 入职流程（L336）逐字「创建 ErpHrEmployee（employmentStatus = PROBATION）」+ `state-machine.md §适用对象二`（PROBATION 为 §1 初始态之一）vs `use-cases.md` UC-HR-01 基本流程 5（L10）「设置雇佣状态为 PROBATION（试用期）**或 ACTIVE（免试用）**」
- **证据**：测试 `TestErpHrRecruitmentEngine:66` 断言 ACTIVE（测试背书当前行为）。recruitment.md 单一来源说 PROBATION；use-cases 说二选一（HR 决定）；实现+测试=恒 ACTIVE（无试用期入口）。连带：`ErpHrEmployeeStateMachine.initialStatuses()` 含 PROBATION 的「零 writer 不对称」（Bean javadoc L36-40 自证）在该分歧下无解——PROBATION 状态在当前实现下经 hire 不可达、经 CRUD 可写（P2-CK-hr-004 ③）。
- **问题**：需求三源不一致，只登记不裁决（试用期是劳动合同法敏感语义：试用期工资/期限约束在合同 probationMonths 字段存在的前提下，员工状态轴是否区分试用期影响薪酬核算口径——C6.2 关联）。
- **建议修复方向**：owner doc 三处对齐（建议 hire 增加 optional probation 标志或从合同 probationMonths>0 推导初始状态），修复时联动核对 C6.2 薪酬链对 PROBATION 员工的处理分支。
- **arm-index 裁决**：新增（P1-MA2-039/P2-MA2-051 覆盖「三终态死状态」与「长期 PROBATION 无提醒」，未覆盖「hire 初始状态选择」维度；grep「PROBATION hire 初始」零命中）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | hr 受影响面 | 结论 |
| --- | --- | --- |
| `P2-CK-mfg-007`/`P2-CK-fin2-007`（orgId 隔离族） | `findDepartmentTree` 双查询无 orgId filter（P2-CK-hr-003 ④ 已并入本域登记） | 同族站点已并入 hr-003，不另立 |
| `P1-CK-pur-003`（CRUD update 无守卫全域族） | hr 5 实体裸 CrudBizModel | 同型登记为 P2-CK-hr-004 |
| cron 键漂移家族（mfg-013/inv-021/sal-023/qa-019） | `erp-hr.contract-expiry-cron` 内层键 + `nop.job.*` 外层双层键 | 同型登记为 P3-CK-hr-012 |
| `P3-CK-mfg-012`（负数入参静默族） | makeOffer 负 offerSalary | 同族站点 P3-CK-hr-009 |
| `ErpCsEntitlementExpiryJob` 范式（job 双层门控镜像） | `ErpHrContractExpiryJob` 顶层 catch 无告警是该范式的弱点投影 | P3-CK-hr-013 登记，cs 站点 C7.1 届时合并裁决 |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：切片 13 文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now/new Date()`=0（时间一律 `CoreMetrics.today()/currentTimeMillis()`）、`extends RuntimeException/Exception`=0、字典字符串 `==` 比较=0（reject 守卫等均 `.equals()` 正确用法）。
- **乐观锁在位**：切片 5 实体（Employee/Department/Position/Recruitment/EmploymentContract）全部 `versionProp="version"`（orm 逐实体实证）——调动与合同续签并发由版本冲突检测。
- **hire 联动生产事务原子性在位（D7 核查点）**：`ErpHrRecruitmentBizModel.hire` 为 `@BizMutation`，经 GraphQL 入口时由 `GraphQLTransactionOperationInvoker`（nop-entropy `.../graphql/core/engine/GraphQLTransactionOperationInvoker.java` L27-33：operation type==mutation → transactionalInvoker 包裹）开启事务——招聘 HIRED+员工创建+合同创建+employeeId 回写整体提交/回滚，无「员工已建合同缺失」中途态。E2E 已覆盖 hire 联动。直接 Java 调用（测试路径）无事务属平台语义（见下条），非生产面。
- **job 直调 biz 无事务但 best-effort 语义成立（D7/D2 核查点）**：`ErpHrContractExpiryJob` 经 `@Inject IErpHrEmploymentContractBiz` 直接 Java 调用（非 GraphQL 入口）→ 无 @BizMutation 事务包装；`expireOverdueContracts` 循环逐条 `contractDao().updateEntity(c)` 各自独立提交 + 失败单 catch 跳过——「失败不影响已推进单、失败单下轮 job 幂等重扫（filter `status=ACTIVE AND endDate<now` 自排除已推进单）」的 best-effort 意图与实际行为一致。与 P3-CK-pur-013/mfg-017（单事务内吞异常部分提交）不同型——此处无共享事务故无部分回滚错位。
- **重复 hire 无双重建员工（任务指定「findFirst 反查健壮性」核查点）**：`ErpHrRecruitmentHireProcessor#requireStatus`（L39 单源 `OFFERED→HIRED` 守卫）——第二次 hire 时 status=HIRED≠OFFERED 抛 `ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION`，幂等由状态守卫保证，无需 findFirst 反查员工存在性（`rec.setEmployeeId` 仅一次可达）。测试 `testIllegalTransitionOpenToHire` 覆盖守卫。
- **transferEmployee 生产事务在位**：`@BizMutation` + GraphQL 入口 → 员工更新+原合同 TERMINATED+successor 保存原子（同 hire 依据）；`warnIfLeaveConflict` 告警不阻塞为 owner doc 裁决行为（P2-MA2-050 已裁决设计接受，不重复登记）。
- **buildSuccessorCode 防溢出修复在位**：截断+MD5 摘要分支（plan 2026-07-18-0347-1）双份实现（BizModel/Processor）逐行比对一致，测试 4 场景覆盖（短码逐字符/固定段保留/不同长不同码/无 active 走 base）。
- **close 无守卫 = 已裁决 watch-only（任务指定核查点）**：`ErpHrRecruitmentBizModel#close` 任意状态可关——**P2-MA2-048 已登记**（watch-only，MR1 裁决方案 A/B 二选一），且 `TestErpHrRecruitmentEngine#testCloseFromHired` 显式断言 HIRED→CLOSED 合法（测试背书入职后清理语义）——复用不重复登记。
- **员工/合同死状态 = 已裁决 Deferred（任务指定 D3 核查点）**：员工 RESIGNED/TERMINATED/RETIRED（P1-MA2-039 resolved——owner doc §适用对象二 Deferred 注记 + `ErpHrEmployeeStateMachine` 退化 Bean 如实编码）与合同 SUSPENDED（P1-MA2-040 resolved——§适用对象五 Deferred）均已有裁决闭环——不重复登记。recruitment dict 7 值全可达（OPEN 初始 + 4 链式 + REJECTED/CLOSED）无死状态。
- **Pattern B custom override 绕过守卫（mfg3-003 族）不适用**：切片 5 实体 `ErpHr{Employee,Department,EmploymentContract,Recruitment,Position}.xbiz.xml` 全部为空 `<actions/>` 纯 extends——无 custom override 面。
- **双轴状态机 doReject 不回写 docStatus（mfg-002 族）不适用**：切片全部为单轴状态机（recruitment `status` / contract `status` / employee `employmentStatus`），无 docStatus+approveStatus 双轴联动结构。
- **dashboard 无死状态消费（D3 核查点）**：hr web dashboard 仅 `org-chart`（消费 findDepartmentTree，不读 employmentStatus）+ `payroll-approval`（归 C6.2）；org-chart L61 `node?.empCount ?? 0` null 安全。
- **PII 后端脱敏在位（P1-MA4-025 修复验证）**：`ErpHrEmployeeBizModel` 5 个 `@BizLoader`（idCardNo/mobilePhone/bankAccountId/socialSecurityNo/taxFileNo）委托 `MaskHelper` fail-closed + 角色/字段参数齐全；合同 `socialInsuranceBase` 同型脱敏（L116-119）——旧 view.xml `LEFT()/RIGHT()` 非法函数已被后端 Loader 替代。
- **员工↔md Partner EMPLOYEE「双写联动」检查点不适用（D8）**：`ErpHrEmployee`（HR 完整人事档案，表 `erp_hr_employee`）与 `ErpMdEmployee`（master-data 业务经办人轻量引用，表 `erp_md_employee`）为**设计上分离的不同表**——`IErpHrEmployeeBiz` L54 javadoc + `docs/design/visible-on-patterns.md:299` 实体身份纠正（员工删除引用计数必须留在 HR 域内、不可经 `ErpParty__findReferences(EMPLOYEE)` SPI——实体错配）双重裁决在位。无双写联动要求，不登记缺失。`ErpHrReportBizModel` 的 partner 读侧（员工净余额报表）归 C6.2。
- **notify 链路 best-effort 语义完备（D2）**：到期提醒经 `IErpSysNotificationBiz.notify` → `ErpSysNotificationNotifyProcessor`（模板缺失 config-gated 静默跳过 + 全 catch 不阻断调用方）+ job 侧单条隔离（L78-84）；同合同 30 天窗口内每日重复派发风险由通知侧 `NotificationMergeCoordinator`（mergeStrategy+mergeWindowSeconds 频控合并）按模板配置兜底——模板机制面，不在本切片登记。
- **processor 经 daoFor(本域实体) 更新 = 既有范式**：3 个切片 processor（Transfer/Hire/Expire）对**本域实体**用 `daoProvider.daoFor(...)` 更新（processor 若注入本域 I*Biz 会与 BizModel→Processor 注入成环）——processor-extension-pattern 惯例，compliance R2c 基线内，非 D1 反模式。
- **到期扫描窗口边界正确（D6）**：`scanExpiringContracts` `dateBetween("endDate", now, now.plusDays(window))` 含当日到期合同；`expireOverdueContracts` `lt("endDate", now)` 严格排除当日（当日到期仍 ACTIVE 待明日翻牌）——两窗口无缝衔接不重不漏。
- **beans.xml 接线完整（D4）**：切片 3 processor（L98-99/L106-107/L112-113）+ 2 状态机 Bean（L121-126）+ job bean（L44）全部注册；job.yaml invoker `bean: erpHrContractExpiryJob, method: execute` 与 bean id 对上——无孤立声明/漏调。
- **requireTargetPosition 宽松归属语义合理**：`position.getDepartmentId() == null` 时跳过部门归属比对（L144-147 三重非空才比）——无部门职位可跨部门使用，属设计选择而非缺陷。

## arm-index 复用 or 新增裁决（汇总）

- 新增关键符号（arm-index 零命中）：`部门删除 引用守卫`/`调动 orgId`/`findDepartmentTree`/`limit 5000`/`hire departmentId 校验`/`hiredDate null`/`renew endDate 边界`/`续签 新建合同`/`superiorId`/`legacy dup`/`证件号码重复`/`gender MALE`/`PROBATION hire 初始` → **新增**。
- **复用（不重复登记，报告中注记）**：
  - 招聘 close 无守卫 → **P2-MA2-048**（watch-only，测试背书 HIRED→CLOSED 合法）——不登记。
  - 员工三终态死状态 + 离职/转正迁移缺失 → **P1-MA2-039**（resolved Deferred）——不登记。
  - 合同 SUSPENDED 死状态 → **P1-MA2-040**（resolved Deferred）——不登记。
  - UC-HR-05「候选人接受 Offer 后未到岗状态回退」缺失 → **P2-RC-010**（open）——不登记。
  - UC-HR-07 多档预警 30/60/90 → **P2-RC-087**（单阈值接受维持）——不登记；P3-CK-hr-007/008 是其未覆盖的 renew 入参与动作语义维度（新增）。
  - 调动休假冲突告警不阻塞 → **P2-MA2-050**（设计接受）——验证为正确。
  - 长期 PROBATION 无提醒 → **P2-MA2-051**（watch-only）——不登记。
  - PII 掩码 view.xml 非法函数 → **P1-MA4-025**（已修复，后端 @BizLoader 落地验证）——验证为正确。
  - CRUD 无守卫 → **P1-CK-pur-003 族**（本 mission）——P2-CK-hr-004 同型登记。
  - orgId 隔离缺失 → **P2-CK-fin2-007/P2-CK-mfg-007 族**——并入 P2-CK-hr-003。
  - cron 键漂移 → **P3-CK-mfg-013/inv-021/sal-023/qa-019 家族**——P3-CK-hr-012 同型登记。
  - 负数入参静默 → **P3-CK-mfg-012 族**——P3-CK-hr-009 同族注记。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 1 | P1-CK-hr-001 |
| P2 | 4 | P2-CK-hr-002..005 |
| P3 | 12 | P3-CK-hr-006..017 |

按主维度：D8×4（001/002/005 + 003 跨计 D9 后主归 D9、015 跨 D5 主归 D5）、D5×4（004/007/009/015 + 010 跨 D10 主归 D5/D10 计 D5）、D9×2（003/014）、D3×2（008/017）、D10×1（006）、D1×1（011）、D4×1（012）、D2×1（013）、D6×1（016 跨 D10 主归 D6）。（精确主维度归属：001 D8、002 D8、003 D9、004 D5、005 D8、006 D10、007 D5、008 D3、009 D5、010 D5、011 D1、012 D4、013 D2、014 D9、015 D5、016 D6、017 D3。）

同型/复用裁决：同型登记 3（004 pur-003 族 / 012 cron 键漂移家族 / 003 含 orgId 族并入）+ arm-index 复用不登记 8 项（048/039/040/RC-010/RC-087/050/051/MA4-025）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 13 文件逐行深读（EmployeeBizModel 375 行/TransferProcessor 300 行/HireProcessor 144 行/Department 103 行/ContractBizModel 122 行/Recruitment 219 行/ExpireProcessor 63 行/ContractExpiryJob 112 行/2 状态机 Bean 全文/Position 桩确认/常量与配置相关段）；平台源码实证 3 处（`GraphQLTransactionOperationInvoker` 与 `DefaultExecutionFunctionInvoker` 事务仅 GraphQL mutation 入口、`ReflectionGraphQLTypeFactory` @Name 必填语义、`CrudBizModel` 无内部事务包装）；orm 核对（5 实体 versionProp/UK(code,orgId)/关键列 mandatory/3 dict 全值）；接线核对（beans.xml 13 相关 bean + job.yaml invoker + 5 xbiz 空 override）；消费面抽查（org-chart.flux.yaml、ErpHrEmployee.view.xml 删除预览与 PII 段）；notify 子系统 best-effort 与频控链；测试 4 文件行为语义交叉验证（Transfer 6 用例/Recruitment 4 用例/References 1 用例/ContractExpiry 5 用例）；arm-index 全 hr 相关条目（L142/149/150/155/535/536/632/779-782/857/887）逐条裁决。
- **未深查**：考勤/休假/薪酬/排班/工时/调研/胜任力全族（归 C6.2——P3-CK-hr-009 的负薪资下游是否被 PayrollCalculator 二次拦截归 C6.2 核）；`ErpHrReportBizModel` 本体（报表，归 C6.2）；`erp-hr-web` 其余 AMIS 页面契约 drift（归 C8.2）；`ErpMdOrganizationReferenceChecker` md 侧实现细节（md 域已检）；通知模板 seed 是否含 `hr.contract-expiry-warning`（部署配置面，notify 子系统归 C8.1 ck-notify）；测试代码正确性（仅用于行为语义交叉验证）；xmeta 层前端必填拦截对 006/007 触发面的进一步收窄（GraphQL NonNull 已实证主拦截层）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-hr-001**（部门/职位删除无引用守卫）——若产品裁决接受「逻辑删除可还原 = 悬挂可恢复」语义（部门误删后恢复 delVersion 即还原引用），可降 P2；但「删除含在职员工的部门零拦截零告警」的字面事实无争议，且子部门提升为根节点的树重组是静默结构性破坏。建议主 agent 以一次删除含员工部门的集成测试实证悬挂面。
  2. **P2-CK-hr-002**（调动跨组织 orgId 不同步）——若产品语义限定「hr 部门树单组织内调动」（requireTargetDepartment 未限制目标部门与员工同 org，通道字面开放），失配不可触发则降 P3；多组织部署是 ORM 显式设计维度（UK 含 orgId），倾向保持 P2。
  3. **P3-CK-hr-017**（hire 初始 ACTIVE vs PROBATION）——三份 owner doc 互相矛盾（recruitment.md PROBATION / use-cases 二选一 / 实现+测试恒 ACTIVE），且 PROBATION 状态影响试用期薪酬口径（C6.2 联动）——建议主 agent 裁决 owner doc 真相源后定向（若裁决 PROBATION 则升 P2）。
  4. **P3-CK-hr-012**（cron 三层键）——与 mfg-013 家族联合修复裁决时统一方向（内层键删除 vs doc 明示三层清单），单域修复易造成家族内不一致。
