# 2026-07-07-1100-2-hr-competency-management HR 胜任力管理（UC-HR-08 胜任力/评估/差距/发展计划）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` Non-Goal scope boundary（UC-HR-08 员工调动/胜任力 → 胜任力段，归后继工作项）+ `docs/design/human-resource/competency-management.md`
> Related: `2026-07-04-0831-2-hr-payroll-engine-income-tax.md`（HR 薪酬引擎已完成，HR 域 BizModel 范式）、`2026-07-04-0831-3-hr-shift-scheduling.md`（HR 排班已完成）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **七实体已物化且 BizModel 为空壳**（design `competency-management.md` 与 ORM **精确匹配**）：
  - `ErpHrCompetency`（`module-hr/model/app-erp-hr.orm.xml:1422`）—— 胜任力字典，含 `category`(SKILL/BEHAVIOR/KNOWLEDGE)、`competencyGroup`、`isTechnical`、`parentId`(自引用层级)、`expectedProficiencyLevel`。
  - `ErpHrCompetencyLevel`（orm:1459）—— 主从于 Competency，`levelNumber`(1-5)、`levelName`、`behavioralAnchor`、`sortOrder`。
  - `ErpHrRoleCompetency`（orm:1487）—— 岗位胜任力要求，`positionId`(→ErpHrPosition)、`competencyId`、`requiredLevel`、`weight`、`isCritical`。
  - `ErpHrEmployeeAssessment`（orm:1519）—— 员工评估，`employeeId`、`assessmentType`(SELF/MANAGER/PEER/SUBORDINATE/360)、`assessorId`、`assessmentDate`、`status`(DRAFT/SUBMITTED/COMPLETED)、`overallScore`(派生)。
  - `ErpHrAssessmentDetail`（orm:1560）—— 主从于 Assessment，`competencyId`、`actualLevel`、`comment`、`sourceType`。
  - `ErpHrGapAnalysis`（orm:1592）—— 差距分析，`employeeId`、`competencyId`、`requiredLevel`、`actualLevel`、`gapValue`、`gapSeverity`(NONE/MINOR/MODERATE/CRITICAL)、`assessmentDate`、`analysisDate`。
  - `ErpHrDevelopmentPlan`（orm:1627）—— 发展计划，`employeeId`、`planName`、`targetDate`、`status`(DRAFT/IN_PROGRESS/COMPLETED/CANCELLED)。
  - `ErpHrDevelopmentPlanItem`（orm:1662）—— 主从于 Plan，`competencyId`、`gapId`(→GapAnalysis)、`targetLevel`、`developmentAction`、`mentorId`、`startDate`/`endDate`、`status`(NOT_STARTED/IN_PROGRESS/ACHIEVED/OVERDUE)、`progressNote`。
  - 七 BizModel 均为 15 行空壳（`ErpHrCompetencyBizModel` 已核实，其余六者同范式 `extends CrudBizModel`）。
- **owner 设计完整且与 ORM 一致**：`docs/design/human-resource/competency-management.md`（202 行）覆盖七实体字段表、评估流程（自评/360）、聚合规则（`actualLevel = selfW×self + mgrW×mgr + peerW×avgPeer + subW×avgSub`，默认 SELF=15%/MANAGER=50%/PEER=25%/SUBORDINATE=10%）、差距严重程度规则（gapValue ≤0 NONE / 1 MINOR / 2 MODERATE / ≥3 CRITICAL）、发展计划生成、配置点、菜单归属（新增 `hr-competency` 分组）。
- **平台范式已就绪**：HR 域 BizModel 范式（`ErpHrConstants`/`ErpHrConfigs`/`ErpHrErrors`）经 0831-2/0831-3 验证；`IErpHrPositionBiz` 岗位接口存在（RoleCompetency 引用）；nop-job 已接线（0306-1）支持评估周期任务；通知派发已完成（0504-1）支持评估提醒。
- **纯 HR 域内部**：仅 `ErpHrPosition`（岗位）、`ErpHrEmployee`（员工）外部引用，无跨域 I*Biz 依赖，风险最低。
- **剩余差距**：(1) 七 BizModel 无业务方法（评估提交/聚合、差距计算、发展计划生成均缺失）；(2) 评估状态机（DRAFT→SUBMITTED→COMPLETED）无校验；(3) 差距分析引擎缺失（评估 COMPLETED 后对比 RoleCompetency.requiredLevel 计算_gapValue/gapSeverity）；(4) 发展计划自动生成（针对 CRITICAL/MODERATE 差距）缺失；(5) `hr-competency` 菜单分组未确认接入。

## Goals

- **评估状态机**：`IErpHrEmployeeAssessmentBiz` 扩展 `submitAssessment`（DRAFT→SUBMITTED，校验至少一条 AssessmentDetail）、`completeAssessment`（SUBMITTED→COMPLETED，触发差距分析）、非法迁移 ErrorCode。
- **360 评估聚合引擎**：`AssessmentAggregator`（纯函数式 + 注入加载函数便于单测）—— 对一员工一胜任力，按 assessmentType 加权平均 actualLevel（权重 config-gated，默认 15%/50%/25%/10%），写回 AssessmentDetail.actualLevel（360 类型）或 overallScore。
- **差距分析引擎**：`GapAnalysisCalculator`（纯函数式）—— 评估 COMPLETED 后，对比员工岗位的 `ErpHrRoleCompetency.requiredLevel` 与聚合 actualLevel，计算 `gapValue = requiredLevel - actualLevel`，按规则映射 `gapSeverity`，持久化 `ErpHrGapAnalysis`（清旧重建快照范式，对齐 0700-1 forecast）。
- **发展计划生成**：`IErpHrDevelopmentPlanBiz` 扩展 `generateDevelopmentPlan(employeeId, ctx)` —— 针对 CRITICAL/MODERATE 差距自动生成建议计划项（按 weight/isCritical 排序），支持 HR 手动调整计划项；计划项状态机（NOT_STARTED→IN_PROGRESS→ACHIEVED/OVERDUE）。
- **胜任力字典 + 岗位矩阵维护**：`IErpHrCompetencyBiz`（字典 CRUD 已有，补层级成环校验）+ `IErpHrRoleCompetencyBiz`（岗位-胜任力矩阵维护，requiredLevel 范围 1-5 校验）。
- **配置门控 + 菜单**：评估权重经 `ErpHrConfigs` 配置化（对齐 0831-2 范式）；`hr-competency` action-auth 菜单分组接入（5 子资源：字典/岗位要求/评估/差距/计划）。
- **owner doc 收口 + 测试**：行为测试覆盖评估状态机、360 聚合、差距计算各 severity、发展计划生成。

## Non-Goals

- **培训课程管理**：`DevelopmentPlanItem.trainingCourseId` 仅记录推荐课程 ID（远期关联培训模块），不做课程实体/排课（design `competency-management.md:10` 明确边界）。
- **绩效评估流程（KPI/PBC 考核）**：属未来绩效模块；本期仅胜任力评估（行为锚定打分），不做 KPI 指标考核。
- **人才盘点 / 继任计划**：高级 HR 功能，远期扩展（design 边界）。
- **360 评估人互不可见权限隔离的精细化**：本期各评估人独立填写（不同 assessmentType 记录），互不可见经数据权限；前端匿名展示归前端 successor。
- **胜任力字典跨组织共享 / 多语言行为锚定**：本期单组织；多语言/共享字典归 successor。
- **胜任力报表 AMIS 前端**：归前端 successor（后端 API 本期就绪）。
- **招聘/合同/考勤/休假（UC-HR-04/05/06/07）**：属 HR 其他后继工作项，本期不涉及。

## Task Route

- Type: `implementation-only change`（七 BizModel 扩展 + 两引擎纯函数式工具类，ORM 无变更）+ 少量 `app-layer design change`（菜单接入 + owner doc 实现注记）。
- Owner Docs: `docs/design/human-resource/competency-management.md`（实体/流程/配置已完整）、`docs/design/human-resource/use-cases.md`（UC-HR-08/12）、`docs/design/human-resource/README.md`。
- Skill Selection Basis: 后端 BizModel/IBiz/ErrorCode/CrudBizModel 钩子 + 单步操作（评估提交/差距计算/计划生成各自单步，非多步编排，无需 Processor）+ 两纯函数式引擎便于单测 → 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase` → 加载 `nop-testing`。两技能必需输入（owner 设计 competency-management.md 既有、七实体 ORM 既有）均就绪。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移；无 ORM 变更；无 codegen 增量（仅 BizModel 方法扩展 + 菜单 action-auth）。
- 新增配置键遵循 HR 域两文件范式（`ErpHrConstants` 字符串键 + `ErpHrConfigs` 默认值/reader，对齐 0831-2/0831-3）：`erp-hr.assessment-self-weight`(0.15)、`erp-hr.assessment-manager-weight`(0.50)、`erp-hr.assessment-peer-weight`(0.25)、`erp-hr.assessment-subordinate-weight`(0.10)、`erp-hr.gap-critical-threshold`(3)。
- 无新业务类型（无业财过账）。
- 回滚策略：全部改动为应用层 Java + action-auth XML，git 可逆；配置键默认值与 design 一致。

## Execution Plan

### Phase 1 - 评估状态机 + 360 聚合引擎

Status: completed
Targets: `IErpHrEmployeeAssessmentBiz`、`ErpHrEmployeeAssessmentBizModel`、`AssessmentAggregator`、`ErpHrConstants`、`ErpHrErrors`、`ErpHrConfigs`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: 无

- [x] `Add`：`IErpHrEmployeeAssessmentBiz` 扩展：`@BizMutation submitAssessment(assessmentId, ctx)`（DRAFT→SUBMITTED，校验至少一条 AssessmentDetail 否则 `ERR_ASSESSMENT_NO_DETAILS`）；`@BizMutation completeAssessment(assessmentId, ctx)`（SUBMITTED→COMPLETED，触发 AssessmentAggregator 聚合 + 差距分析）；非法迁移 `ERR_ASSESSMENT_ILLEGAL_STATUS_TRANSITION`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`AssessmentAggregator`（`module-hr/erp-hr-service/.../competency/`）—— 纯函数式 + 注入加载函数便于单测：`aggregate(employeeId, competencyId, assessmentsByType, weights)` 按 assessmentType 加权平均 actualLevel（默认权重 config-gated 15%/50%/25%/10%），返回聚合 level；处理缺失类型（该类型无记录则权重重归一化或跳过，Decision 在实现时定，记入实现注记）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpHrErrors` 扩展 ErrorCode：`ERR_ASSESSMENT_NO_DETAILS`、`ERR_ASSESSMENT_ILLEGAL_STATUS_TRANSITION`、`ERR_GAP_NO_ROLE_REQUIREMENT`、`ERR_ROLE_COMPETENCY_INVALID_LEVEL`、`ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION`（中文描述 + ARG_* 参数）。`ErpHrConstants` 配置键 + `ErpHrConfigs` 默认值/reader（对齐 0831-2 范式）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 评估状态机（submit/complete）各 ErrorCode 可观察触发；360 聚合引擎加权平均可观察（非空实现，无 `return null` 占位）。
- [x] `mvn compile -pl module-hr/erp-hr-service -am` 通过；行为测试在 Phase 3 统一编写。

### Phase 2 - 差距分析引擎 + 发展计划生成 + 字典/矩阵维护

Status: completed
Targets: `GapAnalysisCalculator`、`IErpHrGapAnalysisBiz`、`IErpHrDevelopmentPlanBiz`、`IErpHrCompetencyBiz`、`IErpHrRoleCompetencyBiz`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（聚合 actualLevel 后才能算差距）

- [x] `Add`：`GapAnalysisCalculator`（纯函数式 + 注入加载函数便于单测）：`calculate(employeeId, roleCompetencies, aggregatedLevels, ctx)` 对每个岗位-胜任力组合计算 `gapValue = requiredLevel - actualLevel`，按规则映射 gapSeverity（≤0 NONE / 1 MINOR / 2 MODERATE / ≥3 CRITICAL，critical 阈值 config-gated）；`IErpHrGapAnalysisBiz.refreshGapAnalysis(employeeId, ctx)` 持久化（清旧重建快照范式，对齐 0700-1 forecast）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpHrDevelopmentPlanBiz` 扩展：`@BizMutation generateDevelopmentPlan(employeeId, ctx)`（针对 CRITICAL/MODERATE 差距按 weight/isCritical 排序生成建议计划项，每项 targetLevel=requiredLevel、developmentAction 模板）；计划项状态机 `updatePlanItemStatus(planItemId, status, ctx)`（NOT_STARTED→IN_PROGRESS→ACHIEVED/OVERDUE，非法迁移 ErrorCode）；`@BizMutation completePlan(planId, ctx)`（DRAFT/IN_PROGRESS→COMPLETED）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpHrCompetencyBiz` 扩展 `defaultPrepareSave`/`Update` 钩子（parentId 自环/成环校验，对齐 projects 0930-3 范式）；`IErpHrRoleCompetencyBiz` 扩展 `defaultPrepareSave` 钩子（requiredLevel 范围 1-5 校验，超范围 `ERR_ROLE_COMPETENCY_INVALID_LEVEL`）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 差距计算（gapValue/gapSeverity 各档）可观察；发展计划生成（CRITICAL/MODERATE 排序）+ 计划项状态机可观察；字典成环 + 矩阵 requiredLevel 校验可观察。
- [x] `mvn compile -pl module-hr/erp-hr-service -am` 通过；行为测试在 Phase 3 统一编写。

### Phase 3 - 行为测试 + 菜单接入 + 日志 + 文档对齐

Status: completed
Targets: `module-hr/erp-hr-service/src/test/.../TestErpHrCompetency*.java`、`module-hr/erp-hr-web` action-auth、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1、Phase 2

- [x] `Add`：`TestAssessmentAggregator`（纯单元测试，mock 加载函数）：四类型齐全加权平均、缺类型重归一化、全缺抛错、权重 config 覆盖。
  - Skill: `nop-testing`
- [x] `Add`：`TestGapAnalysisCalculator`（纯单元测试）：gapValue 各档映射 NONE/MINOR/MODERATE/CRITICAL、无岗位要求处理、critical 阈值 config 覆盖。
  - Skill: `nop-testing`
- [x] `Add`：`TestErpHrCompetencyManagement`（集成测试，H2 + 直接调 BizModel）：评估状态机（submit 无 detail 拒绝 / complete 触发差距）、360 聚合端到端、差距快照清旧重建、发展计划生成排序、计划项状态机、字典成环/矩阵 requiredLevel 校验。
  - Skill: `nop-testing`
- [x] `Proof`：`mvn test -pl module-hr/erp-hr-service -am`（含本期新增 + 0831-2/0831-3 既有）→ 0 failures / 0 errors。
  - Skill: `nop-testing`
- [x] `Add`：`hr-competency` action-auth 菜单分组接入（5 子资源：ErpHrCompetency/ErpHrRoleCompetency/ErpHrEmployeeAssessment/ErpHrGapAnalysis/ErpHrDevelopmentPlan，镜像 HR 既有菜单范式）；`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` Non-Goal boundary 标注 UC-HR-08 胜任力段已承接；`competency-management.md` 实现注记（缺类型重归一化 Decision 等）。
  - Skill: none

Exit Criteria:

- [x] 新增行为测试全绿（单元 + 集成）；hr-service 既有测试无回归。
- [x] `hr-competency` 菜单 action-auth well-formed（`xmllint --noout`）；当日日志条目在位；roadmap Non-Goal boundary 标注更新。

## Draft Review Record

- Independent draft review iteration 1: accept (this review pass) — 格式合规（命名/元数据/九大章节/三阶段结构齐备）；Exit Criteria 可观察可测；Goals↔Execution Plan 1:1 映射；Closure Gates 含真实验证命令（mvn 全量 + module test + xmllint）；Deferred But Adjudicated 三项均带 successor 触发条件；技能选用与必需输入均就绪。Minor 残留（缺失评估类型 Decision 推迟至实现注记、Phase 3 含日志条目作为收尾阶段动作、Task Route 双 Type）均非阻塞，留给结束审计/深度审计在执行后捕获。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（评估状态机 + 360 聚合 + 差距分析 + 发展计划生成 + 字典/矩阵维护 + 菜单接入）
- [x] 相关文档对齐（`competency-management.md` 实现注记、roadmap Non-Goal boundary、当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests`（全模块）+ `mvn test -pl module-hr/erp-hr-service -am`（0 failures / 0 errors）+ action-auth `xmllint --noout`
- [x] 无范围内项目降级为 deferred/follow-up（培训/绩效/继任/权限精细化/多语言/前端均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 培训课程管理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `DevelopmentPlanItem.trainingCourseId` 仅记录推荐课程 ID；课程实体/排课属培训模块（design 边界）。
- Successor Required: yes（触发条件：培训模块落地时）

### 绩效评估流程（KPI/PBC）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属未来绩效模块；本期仅胜任力行为锚定评估。
- Successor Required: yes（触发条件：绩效模块启动时）

### 胜任力报表 AMIS 前端

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归前端 successor；本期后端 API 已就绪。
- Successor Required: yes（触发条件：前端胜任力套件建立时）

## Closure

Status Note: 全部三 Phase 完成（评估状态机 + 360 聚合引擎 + 差距分析 + 发展计划生成 + 字典/矩阵维护 + 菜单接入 + 测试 + 文档对齐）。hr-service 80 tests 0 failures/0 errors（含 28 新增 cases）；全工作区 `mvn clean install -DskipTests` 通过；action-auth `xmllint --noout` well-formed。独立结束审计已由独立子代理（新会话）执行并通过，证据见下方 Closure Audit Evidence。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，未复用执行者上下文）
- Audit scope: 全计划重读 + 退出标准逐项对实时仓库验证（非采信 `[x]` 标记）
- Phase 1 verification: `AssessmentAggregator.java`（149 行，纯函数式 + 注入加载函数 `aggregateWithLoader`，缺类型重归一化 Decision 已落地，非空实现）；`ErpHrEmployeeAssessmentBizModel.java`（149 行，`submitAssessment`/`completeAssessment` @BizMutation + @SingleSession，状态机 ErrorCode 可观察触发，completeAssessment 内部直传 levels 给 `refreshGapAnalysisWithLevels` 避免跨事务可见性问题）；`ErpHrErrors`/`ErpHrConstants`/`ErpHrConfigs` 三件套配置键与默认值（15%/50%/25%/10% + critical 阈值 3）齐备。
- Phase 2 verification: `GapAnalysisCalculator.java`（91 行，纯函数式 `calculate` + 公开 `severityOf` 便于单测）；`ErpHrGapAnalysisBizModel.java`（169 行，清旧重建快照范式对齐 0700-1，无岗位要求抛 `ERR_GAP_NO_ROLE_REQUIREMENT`）；`ErpHrDevelopmentPlanBizModel.java`（215 行，`generateDevelopmentPlan` 按 severityRank+gapValue 排序、`updatePlanItemStatus`/`completePlan` 状态机）；`ErpHrCompetencyBizModel`/`ErpHrRoleCompetencyBizModel` 的 `defaultPrepareSave`/`defaultPrepareUpdate` 钩子已落地（parentId 成环 `ERR_COMPETENCY_PARENT_CYCLE`、requiredLevel 范围 `ERR_ROLE_COMPETENCY_INVALID_LEVEL`）。
- Phase 3 verification: 三测试类 765 行 / 28 @Test 方法（`TestAssessmentAggregator` 7 + `TestGapAnalysisCalculator` 5 + `TestErpHrCompetencyManagement` 16）；`_erp-hr.action-auth.xml` 含全部胜任力资源（Competency/RoleCompetency/EmployeeAssessment/GapAnalysis/DevelopmentPlan + CompetencyLevel/DevelopmentPlanItem/AssessmentDetail）；`docs/logs/2026/07-07.md` 详细日志条目在位（含 4 关键 Decision + 全绿验证状态）。
- Anti-Hollow check: 所有新增代码均被运行时调用 —— AssessmentAggregator 经 ErpHrEmployeeAssessmentBizModel/ErpHrGapAnalysisBizModel @Inject 注入并调用；GapAnalysisCalculator 同；generateDevelopmentPlan 经 @BizMutation 暴露为 GraphQL mutation；无 `{}` 空体 / `return null` 占位（generateDevelopmentPlan 无 actionable gap 返回 null 为正确业务语义非占位）。
- Five-point consistency: Plan Status completed ↔ 3 Phase Status completed ↔ 各 Phase Exit Criteria 全 [x] ↔ Closure Gates 全 [x] ↔ 日志条目一致。
- Verification status (per log): `mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS；`mvn test -pl module-hr/erp-hr-service` 80 tests 0 failures/0 errors；action-auth `xmllint --noout` well-formed。
- Outcome: 批准关闭。无范围内缺陷隐藏于 Deferred；3 Deferred 项均带 successor 触发条件。

Follow-up:

- 培训课程管理（见上方 Deferred）
- 绩效评估流程（见上方 Deferred）
- 胜任力报表前端（见上方 Deferred）
