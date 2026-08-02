# 2026-08-02-2250-1 rc-ma1-a1-12-hr-f1-employee-organization hr-F1 员工与组织需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.12（MA1 需求追踪矩阵审计 — hr-F1 员工与组织：UC-HR-01 员工入职 + UC-HR-05 招聘录用 + UC-HR-07 合同到期提醒 + UC-HR-08 部门调动 + UC-HR-12 胜任力管理与评估）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.12
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.12 的 0.2 依赖）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，同审计范式）、`2026-08-02-2231-2-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md`（A1.11 done，最近同范式参考）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.12 给出 UC 清单 = `UC-HR-01/05/07/08/12`（5 UC），锚点 `use-cases.md:3 / :51 / :75 / :87 / :137`（baseline inventory :68/:72/:74/:75/:79 + 切片索引 :346 确认一致 ✅）。

- **L1 需求契约（权威真相源）**：`docs/design/human-resource/use-cases.md`：
  - UC-HR-01 员工入职（`:3`）：录入员工信息（姓名/性别/出生日期/证件/联系方式/银行账户）→ 选择部门(ErpHrDepartment)+职位(ErpHrPosition) → 设置直属上级(superiorId) → 填写入职/试用期截止日期 → 雇佣状态 PROBATION/ACTIVE → 社保号/个税档案号 → 创建劳动合同(ErpHrEmploymentContract) → 创建 ErpHrEmployee；异常：部门/职位不存在先创建、证件号码重复提示；可选创建系统账号(UserAccountId)。
  - UC-HR-05 招聘录用（`:51`）：Recruitment 关联职位+部门 → 简历筛选(SCREENING) → 面试(INTERVIEW) → 发 Offer(OFFERED) → 候选人接受入职(HIRED)创建 ErpHrEmployee 并关联 employeeId → 或拒绝(REJECTED) → 岗位关闭(CLOSED)；异常：候选人接受 Offer 后未到岗需状态回退。
  - UC-HR-07 合同到期提醒（`:75`）：定时任务每日扫描 endDate 在 30/60/90 天提醒窗口内的 ACTIVE 合同 → 通知 HR → 续签(创建新合同+原合同 EXPIRED) 或到期终止(原合同 EXPIRED+员工 employmentStatus 联动)；跨域：员工状态联动（不续签→RESIGNED）。
  - UC-HR-08 部门调动（`:87`）：选择员工+目标部门 → 可选调整职位/直属上级 → 设置调动生效日期 → 更新 departmentId/positionId/superiorId → 标记原合同 TERMINATED + 创建新合同；异常：调动日期与已有休假冲突告警；跨域：成本中心变更影响项目工时归集。**use-cases.md :99 显式注记**：`transferEmployee` 单步直接更新无审批、handleContract 三态 AUTO/YES/NO config-gated、休假冲突告警不阻塞；调动单实体+审批工作流归 Deferred（触发条件=调动需人工审批留痕或批量调动报表时，经独立 ORM ask-first 承接）。
  - UC-HR-12 胜任力管理与评估（`:137`）：创建 ErpHrCompetency(SKILL/BEHAVIOR/KNOWLEDGE)+能力组层级 → ErpHrCompetencyLevel(1-5 级行为锚定) → ErpHrRoleCompetency(岗位要求等级/权重/是否关键) → 发起评估周期 ErpHrEmployeeAssessment(SELF/MANAGER/PEER/SUBORDINATE/360) → 各评估人填 ErpHrAssessmentDetail → 按权重聚合(默认 SELF 15%/MANAGER 50%/PEER 25%/SUBORDINATE 10%) → 对比 ErpHrRoleCompetency 计算 ErpHrGapAnalysis(gapValue=requiredLevel-actualLevel) → 标记 gapSeverity(NONE/MINOR/MODERATE/CRITICAL) → 针对 CRITICAL/MODERATE 生成 ErpHrDevelopmentPlan 建议 → HR 审核调整发展计划项。

- **L2 owner doc 设计参考**：`docs/design/human-resource/recruitment.md`（招聘状态机 + 关键业务规则 + 多实体设计 Candidate/Interview/Offer/OnboardingChecklist[注：实现为扁平 ErpHrRecruitment 单实体，P2-MA2-049 watch-only 已登记 Deferred 未注记]）+ `docs/design/human-resource/competency-management.md`（胜任力模型 + ErpRoleCompetency + 评估聚合权重 + GapAnalysis + DevelopmentPlan）+ `docs/design/human-resource/state-machine.md`（适用对象二员工雇佣状态 + 适用对象七考核[若已补]；P2-MA2-047/052 watch-only 已登记 state-machine.md 缺招聘/合同/考核/发展计划/调查独立章节）+ `docs/design/human-resource/README.md`。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准；recruitment.md 多实体设计 vs 扁平实现属 owner-doc drift（MA3 已收口 P2-MA2-049），本切片不重审文本一致性，只补需求契约视角差异。

- **L3 代码实现现状（执行时实测核验）**：
  - **员工入职（UC-HR-01）**：`module-hr/erp-hr-service/.../entity/ErpHrEmployeeBizModel.java`（员工 CRUD + 入职建档；执行时核验：PROBATION/ACTIVE 状态设置 + departmentId/positionId/superiorId 关联 + 证件号码重复校验 + 合同创建联动 + 可选 UserAccountId）；`IErpHrDepartmentBiz`/`IErpHrPositionBiz`/`IErpHrEmploymentContractBiz` 跨实体校验。
  - **招聘录用（UC-HR-05）**：`ErpHrRecruitmentBizModel.java`（7 态状态机 SCREENING/INTERVIEW/OFFERED/HIRED/REJECTED/CLOSED + hire 创建 ErpHrEmployee 并关联 employeeId；close 无守卫 P2-MA2-048 watch-only；reject 守卫拒 HIRED/CLOSED/REJECTED）；`TestErpHrRecruitmentEngine`。
  - **合同到期提醒（UC-HR-07）**：`processor/ErpHrEmploymentContractExpireOverdueContractsProcessor.java`（cron 扫描 endDate 在提醒窗口内的 ACTIVE 合同 + 通知 HR；执行时核验：提醒窗口 30/60/90 天配置来源 + cron 调度接线 + 续签/终止联动 + 员工 employmentStatus 联动）；`TestErpHrContractExpiry`。
  - **部门调动（UC-HR-08）**：`ErpHrEmployeeBizModel.transferEmployee`（`@BizMutation` 单步直接更新无审批；经 `ErpHrEmployeeTransferEmployeeProcessor.java`；handleContract 三态 AUTO/YES/NO + config-gated；`warnIfLeaveConflict` 休假冲突告警不阻塞 P2-MA2-050 watch-only；AMIS 员工页「调动」drawer 入口）；`TestErpHrEmployeeTransfer`。
  - **胜任力管理与评估（UC-HR-12）**：`ErpHrCompetencyBizModel.java` + `ErpHrCompetencyLevelBizModel` + `ErpHrRoleCompetencyBizModel`（胜任力字典+等级+岗位矩阵）+ `ErpHrEmployeeAssessmentBizModel.java`（评估周期 + completeAssessment 经 `ErpHrEmployeeAssessmentCompleteAssessmentProcessor.java`）+ `AssessmentAggregator`（按权重聚合 SELF 15%/MANAGER 50%/PEER 25%/SUBORDINATE 10%，执行时核验权重配置来源）+ `GapAnalysisCalculator.java`（gapValue=requiredLevel-actualLevel + gapSeverity 分级）+ `ErpHrGapAnalysisBizModel.java`（refreshGapAnalysisWithLevels 经 Processor）+ `ErpHrDevelopmentPlanBizModel.java`（发展计划 + updatePlanItemStatus Processor）；`TestErpHrCompetencyManagement` + `TestGapAnalysisCalculator` + `TestAssessmentAggregator`。

- **L4 测试证据现状**：`TestErpHrRecruitmentEngine`（招聘状态机）+ `TestErpHrContractExpiry`（合同到期提醒）+ `TestErpHrEmployeeTransfer`（调动）+ `TestErpHrCompetencyManagement`（胜任力）+ `TestGapAnalysisCalculator`（差距分析）+ `TestAssessmentAggregator`（评估聚合权重）+ `TestErpHrEmployeeReferences`（员工引用）。E2E：`tests/e2e/business-actions/hr-recruitment.action.spec.ts` + `hr-transfer.action.spec.ts` + `hr-assessment-dev-plan.action.spec.ts`。**执行时核验断言强度**（验收标准强断言 vs 仅冒烟）；MA5/A4.4 hr 评级可引用。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`（A2.7a）= hr 员工与组织七组件状态机审查**：Verdict 主路径状态迁移守卫齐全 + 事务边界清晰 + 招聘 hire 跨实体副作用经事务回滚 + 考核 completeAssessment 跨实体刷新经直传 levels。**零 P0**；**4 项 P1**（P1-MA2-039 员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + 离职/退休/转正迁移完全未实现 / P1-MA2-040 合同 SUSPENDED dict 死状态 / P1-MA2-041 调查三态死状态[本切片 UC-HR-12 非调查，但同报告] / P1-MA2-042 发展计划死状态[UC-HR-12 发展计划维度]）+ **5 项 P2**（P2-MA2-047 state-machine.md 缺章节 / P2-MA2-048 招聘 close 无守卫 / P2-MA2-049 recruitment.md 多实体 Deferred 未注记 / P2-MA2-050 调岗请假冲突 warn 非阻断 / P2-MA2-051 长期 PROBATION 未转正无 TODO）。
  - **`docs/audits/2026-07-29-0430-arm-ma4-hr-code-quality.md`（A4.4）**：hr 员工与组织维度代码质量（Recruitment/Survey/DevelopmentPlan BizModel 桩治理 P2-MA4-008），本切片复核 resolved 状态。
  - **注意**：A2.7a/A4.4 覆盖**状态机迁移守卫/事务边界/代码质量**，但本切片从**需求契约↔实现符合性**视角补差异（UC-HR-01 入职字段完整性与状态初始化 + UC-HR-05 招聘 7 态全链与 hire 副作用 + UC-HR-07 合同到期提醒窗口/调度/联动 + UC-HR-08 调动字段更新与合同处理 + UC-HR-12 胜任力字典/等级/岗位矩阵/评估聚合/差距分析/发展计划全链 + resolved finding HEAD 复核）。

- **arm-index 既有 finding 衔接**：员工与组织相关——`P1-MA2-039`（employmentStatus 三态死状态，resolved 状态执行时 HEAD 复核）/ `P1-MA2-040`（合同 SUSPENDED 死状态）/ `P1-MA2-042`（发展计划死状态，UC-HR-12 维度）/ `P2-MA2-047/048/049/050/051`（doc drift watch-only）/ `P2-MA1-020`（orphan dict salary-approval-status）/ `P1-MA1-022`（跨域 daoFor 只读，hr 投影）。UC-HR-07 合同到期提醒调度接线、UC-HR-12 评估聚合权重配置来源、UC-HR-08 调动审批工作流 Deferred 为候选新维度（既有审计未从需求契约视角裁决），执行时 grep `arm-index.md` hr 员工/招聘/合同/调动/胜任力同域同控制点后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及 ORM 结构变更（如调动单实体/审批工作流）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.12 切片的五级追踪审计报告缺失 = MA4（A4.2 扩展域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.12 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.12 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-12-hr-f1-employee-organization.md`，含方法论 §6 **9 段全部内容**：①UC-HR-01/05/07/08/12 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含员工建档 + 招聘状态机 + 合同到期 Processor + 调动 Processor + 胜任力/评估/差距/发展计划全链）③测试证据（注明断言强度）④运行时行为证据（复用 A2.7a/A4.4，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2/MA4 报告差异增量声明。
- 对 5 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-HR-01（员工字段完整性 + 部门/职位/上级关联 + PROBATION/ACTIVE 状态 + 合同创建 + 证件重复校验）+ UC-HR-05（招聘 7 态全链 + hire 创建员工+关联 employeeId + 状态回退异常）+ UC-HR-07（cron 调度 + 30/60/90 提醒窗口 + 续签/终止联动 + 员工状态联动不续签→RESIGNED）+ UC-HR-08（调动字段更新 departmentId/positionId/superiorId + 合同 TERMINATED+新合同 + 休假冲突告警 + 调动单实体/审批工作流 Deferred Q4 裁决）+ UC-HR-12（胜任力字典/等级/岗位矩阵 + 评估周期多视角 + 权重聚合 SELF/MANAGER/PEER/SUBORDINATE + gapValue/gapSeverity + 发展计划生成），各一矩阵行。
- 对候选缺口/偏离给出分级结论：**UC-HR-08 调动单实体+审批工作流 Deferred（use-cases.md:99 显式注记触发条件=调动需人工审批留痕或批量调动报表时承接）——按 §4 Q1 L1 注记裁决（注记本身是 L1 真相源的一部分， Deferred 触发条件已显式登记，裁决是否构成"已登记 successor"非静默降级）**；**UC-HR-07 员工状态联动不续签→RESIGNED 与 P1-MA2-039 employmentStatus 三态死状态交叉**（HEAD 复核 P1-MA2-039 resolved 状态，若仍 open 则 UC-HR-07 联动维度继承 P1）；**UC-HR-12 评估聚合权重配置来源 + gapSeverity 分级 + 发展计划生成**（执行时 HEAD 核验）；UC-HR-01 入职字段完整性 / UC-HR-05 招聘状态回退异常路径（执行时核验）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / hr use-cases / recruitment.md / competency-management.md / state-machine.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.12 只覆盖 UC-HR-01/05/07/08/12；UC-HR-02/06/09 归 A1.13，UC-HR-03/04/10/11 归 A1.14）。**UC-HR-11 员工调研归 A1.14**（虽含 ErpHrSurvey，但 roadmap 切片归属 A1.14）。
- **不重跑既有状态机/代码质量行为审计**（§去重协议：A2.7a/A4.4 已证实状态机迁移守卫/事务边界/代码质量，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.12 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.12 UC 锚点）+ `docs/design/human-resource/use-cases.md`（L1 真相源）+ `docs/design/human-resource/recruitment.md` + `competency-management.md` + `state-machine.md` + `README.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.7a/A4.4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A2.7a/A4.4 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrRecruitmentEngine,TestErpHrContractExpiry,TestErpHrEmployeeTransfer,TestErpHrCompetencyManagement,TestGapAnalysisCalculator,TestAssessmentAggregator,TestErpHrEmployeeReferences`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + resolved finding HEAD 复核

Status: completed
Targets: `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md`（落盘 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-HR-01/05/07/08/12 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:3/:51/:75/:87/:137` 验收标准原文（禁止转述）；L2 引用 `recruitment.md`（招聘状态机 + 关键业务规则）+ `competency-management.md`（胜任力模型 + 评估聚合权重 + GapAnalysis + DevelopmentPlan）+ `state-machine.md`（员工雇佣状态 + 考核，标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpHrEmployeeBizModel.java:line`（入职建档 + transferEmployee）+ `ErpHrEmployeeTransferEmployeeProcessor:line` + `ErpHrRecruitmentBizModel.java:line`（7 态 + hire 副作用）+ `ErpHrEmploymentContractExpireOverdueContractsProcessor.java:line`（cron 扫描 + 提醒窗口 + 通知）+ `ErpHrCompetencyBizModel`/`ErpHrCompetencyLevelBizModel`/`ErpHrRoleCompetencyBizModel:line`（字典+等级+岗位矩阵）+ `ErpHrEmployeeAssessmentBizModel.java:line` + `ErpHrEmployeeAssessmentCompleteAssessmentProcessor:line` + `AssessmentAggregator:line`（权重聚合）+ `GapAnalysisCalculator.java:line`（gapValue/gapSeverity）+ `ErpHrGapAnalysisRefreshGapAnalysisWithLevelsProcessor:line` + `ErpHrDevelopmentPlanBizModel.java:line` + `ErpHrDevelopmentPlanUpdatePlanItemStatusProcessor:line`；L4 引用 `Test*.java#method`（注明断言强度）；L5 复用 A2.7a/A4.4 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-HR-01——①员工字段完整性（姓名/性别/出生日期/证件/联系方式/银行账户）；②部门/职位/上级关联；③PROBATION/ACTIVE 状态初始化；④合同创建联动；⑤证件号码重复校验。UC-HR-05——⑥招聘 7 态全链（SCREENING→INTERVIEW→OFFERED→HIRED/REJECTED→CLOSED）；⑦hire 创建 ErpHrEmployee + 关联 employeeId；⑧候选人接受 Offer 后未到岗状态回退异常路径。UC-HR-07——⑨cron 调度接线（每日扫描）；⑩30/60/90 提醒窗口配置来源；⑪续签(新合同+原 EXPIRED)/终止(原 EXPIRED+员工状态联动)；⑫不续签→员工 RESIGNED 联动（与 P1-MA2-039 交叉）。UC-HR-08——⑬调动字段更新 departmentId/positionId/superiorId；⑭原合同 TERMINATED + 新合同（handleContract 三态）；⑮调动生效日期；⑯休假冲突告警（warnIfLeaveConflict）；⑰调动单实体 + 审批工作流 Deferred Q4 裁决（use-cases.md:99 注记）。UC-HR-12——⑱胜任力字典(SKILL/BEHAVIOR/KNOWLEDGE)+能力组层级；⑲CompetencyLevel 1-5 级行为锚定；⑳RoleCompetency 岗位要求等级/权重/关键；㉑评估周期多视角(SELF/MANAGER/PEER/SUBORDINATE/360)；㉒权重聚合(SELF 15%/MANAGER 50%/PEER 25%/SUBORDINATE 10%)配置来源；㉓gapValue=requiredLevel-actualLevel + gapSeverity 分级；㉔CRITICAL/MODERATE→发展计划生成；㉕HR 审核调整发展计划项。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` **resolved finding HEAD 复核**：对员工与组织相关 finding（P1-MA2-039 employmentStatus 三态死状态 / P1-MA2-040 合同 SUSPENDED 死状态 / P1-MA2-042 发展计划死状态[UC-HR-12 维度] / P1-MA1-022 跨域 daoFor 只读 hr 投影 / P2-MA2-047~051 doc drift——resolved 状态执行时经 arm-index grep 确认，未确认者按"未定"处理）在当前 HEAD 代码实际落地（按逻辑非行号核验），逐条记录复核结论（已落地/回退/部分落地/documented simplification 仍 open successor）。**P1-MA2-039 resolved R1.15 documented simplification**（state-machine.md §适用对象二:126 显式 Deferred 段落 + successor 触发条件）→ UC-HR-07⑫ 不续签→RESIGNED 联动继承 successor（非新缺口）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：UC-HR-01 = **接受**（CrudBizModel 标准 save 入职建档覆盖全字段）。UC-HR-05 = **P2**（⑱未到岗回退异常路径未实现 = P2-RC-010；主路径 7 态完整）。UC-HR-07 = **接受**（⑮不续签→RESIGNED 复用 P1-MA2-039 successor Deferred）。UC-HR-08 = **接受**（⑰调动单实体+审批工作流 Deferred = L1 use-cases.md:99 显式 documented simplification，§4 三判据 (ii) 满足——注记本身是 L1 真相源一部分，Deferred 触发条件已显式登记）。UC-HR-12 = **接受**（㉒权重聚合 config 驱动[ErpHrConfigs 默认 15%/50%/25%/10% + AppConfig.var 可覆盖]非硬编码）。每结论列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-HR-01/05/07/08/12 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.7a/A4.4 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口 ①-㉕ 有明确分级（非悬空"待查"）；UC-HR-08⑰ Deferred 结论含 §4 裁决；UC-HR-07⑫ P1-MA2-039 HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md`（落盘 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` hr 员工/招聘/合同/调动/胜任力同域同控制点（如 P1-MA2-039/040/042）后裁决——**P1-MA2-039 复用**（UC-HR-07⑮ 不续签→RESIGNED 同根因同控制点，追加 RC 交叉引用注记不新建）；**P2-RC-010 新建**（UC-HR-05⑱未到岗回退异常，与 P2-MA2-048 close 无守卫不同控制点，列明差异依据）。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（cron 运行时调度接线 / 30-60-90 多档预警运行时配置 / 评估聚合权重运行时配置覆盖 / handleContract 三态运行时行为 / 未到岗回退运行时处理；每存疑点一行）。**P0 即时通道未触发**（本切片无 P0）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2/MA4 报告差异增量声明：声明复用 A2.7a（员工与组织七组件状态机 + P1-MA2-039/040/042 + P2-MA2-047~051 finding）+ A4.4（Recruitment/Survey/DevelopmentPlan 桩治理 P2-MA4-008）已证实结论，列明本切片只补的需求视角差异（UC-HR-01 入职字段完整性 + UC-HR-05 招聘 7 态全链与 hire 副作用 + UC-HR-07 合同到期提醒窗口/调度/联动 + UC-HR-08 调动字段更新与合同处理 + UC-HR-12 胜任力字典/等级/岗位矩阵/评估聚合/差距分析/发展计划全链 + resolved finding HEAD 复核）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P2-RC-010` 入 P2 finding 分区（MA1 finding 区），既有 P1-MA2-039 行追加 RC A1.12 交叉引用注记；A1.12 完成摘要追加到 MA1 切片完成记录段。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [x] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 `ses_03cef9818ffeh66yYgYAudBjJw`，fresh session，未起草本计划）。逐项实测核验：roadmap 对齐（A1.12 / UC-HR-01/05/07/08/12 / Deps=M0.1+M0.2 done / Skill）、5 UC 锚点 :3/:51/:75/:87/:137 全匹配（完整枚举无跳无合并）、L3 全部代码路径存在（ErpHrEmployeeBizModel.transferEmployee:92 + ErpHrEmployeeTransferEmployeeProcessor.transferEmployee:68/warnIfLeaveConflict:150/handleContract 三态 + ErpHrRecruitmentBizModel 7 态 + close 无守卫[P2-MA2-048] + Competency/Level/RoleCompetency + EmployeeAssessment+CompleteAssessmentProcessor + AssessmentAggregator/GapAnalysisCalculator + GapAnalysisBizModel+RefreshWithLevelsProcessor + DevelopmentPlan+UpdatePlanItemStatusProcessor）、L4 7 测试 + 3 E2E 全存在、L5 dedup 输入 A2.7a/A4.4 存在 + arm-index finding IDs 全命中（P1-MA2-039/040/042 resolved R1.15、P2-MA2-047~051 watch-only、P1-MA1-022 resolved、P2-MA1-020、P2-MA4-008）、跨切片边界正确（UC-HR-11→A1.14、UC-HR-02/06/09→A1.13）、只读审计删门控有据（:132）、保护区域 ORM ask-first 延后 MR0/MR1 全合规、use-cases.md:99 注记 §4 Q1 裁决框架正确（Deferred 带显式触发条件非静默降级，最终定级留执行时 HEAD 核验）、P1-MA2-039/040/042 已 resolved R1.15 保守处理（"HEAD 复核...若仍 open 则继承 P1"非假设）。**无阻塞 issue**，共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + resolved finding HEAD 复核 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.12 报告 9 段齐全 + UC-HR-01/05/07/08/12 逐矩阵行 + resolved finding HEAD 复核 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.12 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及 ORM 结构变更（调动单实体/审批工作流）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: Phase 1 + Phase 2 均已完成（执行者 = 主代理）。报告 `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md` 9 段齐全 + arm-index 已登记 P2-RC-010 + P1-MA2-039 RC 交叉引用注记 + A1.12 完成摘要。独立结束审计已由独立子代理新会话执行通过（本节下方证据）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER session 2026-08-02-204249-mission-driver，fresh session，未执行本计划任何阶段、未起草本计划）
- Evidence: 独立语义验证（非仅结构检查）：
  1. **报告存在性 + 9 段完整性**：报告 `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md` 落盘（48KB），9 段全部存在（§1 需求契约 :26 / §2 实现代证 :125 / §3 测试证据 :183 / §4 运行时行为 :216 / §5 符合性结论 :230 / §6 arm-index 衔接 :276 / §7 静态存疑点 :318 / §8 过程纪律自检 :330 / §9 MA2/MA4 差异增量声明 :13 前置）。
  2. **arm-index 同步**：P2-RC-010 已入 arm-index.md:111（完整 finding 描述 + successor watch-only + 修复方案 A/B）；A1.12 完成摘要已入 arm-index.md:137；P1-MA2-039 行 arm-index.md:231 状态 ✅ resolved (R1.15 done) 与报告 §6 复用裁决一致。
  3. **关键代码声明实测核验**（HEAD `f3ff693e6c81472264b92a9dfba9844231ef3a2` 与报告声明一致）：(a) P2-RC-010 核心 claim「无 rollbackHire/revertHire/noShow mutation」经 `grep rollbackHire|revertHire|noShow module-hr/**/*.java` 实测**零命中**——claim 真实；(b) `ErpHrEmployeeBizModel.transferEmployee:92` + `ErpHrEmployeeTransferEmployeeProcessor.transferEmployee:68/warnIfLeaveConflict:150` 存在且签名匹配；(c) P1-MA2-039/040/042/052 + P1-MA1-022 + P2-MA4-008 resolved 状态在 arm-index 全部确认 ✅。
  4. **Anti-Hollow**：所有 L3 证据带 `file:line`，finding 有具体代码定位与差异说明，无空函数体 / `return null` 占位 / 吞异常。
  5. **五点一致性**：Plan Status: completed ↔ Phase 1/2 Status: completed ↔ 两阶段 Exit Criteria 全 `[x]` ↔ Closure Gates 全 `[x]`（本审计已勾选 gate 7）↔ Closure 证据落盘。
  6. **Deferred honesty**：P2-RC-010 是新登记的 in-scope live 缺陷，已显式入 arm-index + successor = MR1（R1.0 展开 RC-R1.n），**非隐藏于 Deferred 段**；UC-HR-08⑰ 调动单实体+审批工作流 Deferred = L1 use-cases.md:99 显式 documented simplification（§4 三判据 (ii) 满足），非静默降级。
  7. **Docs sync**：本切片为只读审计（无代码/ORM/真相源变更），AGENTS.md §日志要求仅触发 owner docs——arm-index 已更新（前述 #2），无 docs/logs/ 义务（无生产代码变更）。
  Verdict: **approved**（零阻塞 issue，计划可正式闭包）。

Follow-up:

- finding 修复属 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n）successor，非阻塞本审计闭环（§Deferred But Adjudicated 已 adjudicated）
