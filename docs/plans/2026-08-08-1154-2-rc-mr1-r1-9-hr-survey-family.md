# 2026-08-08-1154-2-rc-mr1-r1-9-hr-survey-family RC-R1.9 — hr 员工调研族（P1-RC-016 + P1-MA2-041 reuse 重开，MR1 第一批纯预授权）

> Plan Status: active
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.9（MR1 第一批纯预授权：hr 员工调研族——respondentHash 匿名防重复 + publish/close 状态机 + CLOSED 自动聚合 ErpHrSurveyResult + eNPS + 仪表盘查询，P1-RC-016 + P1-MA2-041 reuse 重开）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.9 行 + `docs/audits/arm-index.md` P1-RC-016 / P1-MA2-041 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3（RC-R1.9 = 纯 BizModel 修复，G2 hr 调研族同域同根因同控制点）
> Related: `docs/design/human-resource/use-cases.md`（L1 UC-HR-11）；`docs/audits/2026-08-07-0530-rc-ma4-a4-2-22-26-hr-payroll-survey-runtime.md`（A4.2.25/A4.2.26 运行时确认）；`docs/plans/2026-08-08-1154-1-rc-mr1-r1-8-hr-timesheet-family.md`（同批 hr 域计划范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-016（arm-index 行）**：UC-HR-11 ㉖㉘㉙——「匿名 respondentHash 防重复」+「CLOSED 自动聚合 ErpHrSurveyResult」+「eNPS 得分 + 仪表盘」完全缺失。L1 `use-cases.md:132-134` 逐字「匿名模式下 employeeId 不存储，仅存 respondentHash 防重复」+「截止后 status→CLOSED，自动聚合 ErpHrSurveyResult」+「异常：同一员工重复提交匿名问卷被 respondentHash 拦截」+「HR 查看结果仪表盘：评分趋势、部门对比、eNPS 得分、驱动因子分析」。
- **finding P1-MA2-041（arm-index 行，reuse 重开）**：调查 OPEN/CLOSED/ARCHIVED 三态死状态 + `ErpHrSurveyBizModel` 18 行 CRUD 桩 + owner doc §状态机声明漂移（DRAFT→OPEN→CLOSED→ARCHIVED + OPEN 可直接→CLOSED + CLOSED 触发自动聚合）。RC 复核（2026-08-03）在 Q4=(a) 下重开经 MR1 实现 publish/close/archive。
- **A4.2.25/A4.2.26 运行时确认（`2026-08-07-0530` 报告）**：grep `aggregateResult|calculateEnps|publishSurvey|closeSurvey|respondentHash` 跨 hr main 零生产命中；ORM `respondentHash`（orm.xml:1429）/`eNpsScore`（:1357/:1499）列零 writer/零校验；`TestErpHrSurveyCrudSmoke` 仅 CRUD 冒烟（㉖㉗㉘㉙ 零断言）。**维持 P1 不撤销**（Q4 强制实现）。
- **实仓**：`ErpHrSurveyBizModel.java` / `ErpHrSurveyResponseBizModel.java` / `ErpHrSurveyResultBizModel.java` / `ErpHrSurveyAnswerBizModel.java` 均 18 行 CRUD 桩；`IErpHrSurveyBiz` / `IErpHrSurveyResponseBiz` / `IErpHrSurveyResultBiz` 空接口；`ErpHrConstants` 无 `SURVEY_STATUS_*` 常量（dict `erp-hr/survey-status` 含 DRAFT/OPEN/CLOSED/ARCHIVED 四值，orm.xml:189-194）。
- **ORM 载体（全齐备，零 ORM 变更）**：
  - `ErpHrSurvey`（orm.xml:1336-1385）：`status`（propId 7，dict survey-status）、`isAnonymous`（propId 6，default true）、`surveyType`（dict：ANNUAL_ENGAGEMENT/PULSE/ENPS/ADHOC）、`startDate/endDate`、`targetDepartmentId`、`includeENps`（default false）+ `eNpsQuestion`、`reminderDays`（default 3）、`totalResponses`（default 0）、`completionRate`、`avgScore`、`eNpsScore`（propId 18）。
  - `ErpHrSurveyResponse`（:1422-1455）：`surveyId`、`employeeId`（可空——匿名时置空）、`respondentHash`（VARCHAR 100，零 writer）、`submittedAt`、`isComplete`（default false）、`orgId`。
  - `ErpHrSurveyQuestion`（:1388-1419）：`questionType`（dict：RATING/SINGLE_CHOICE/MULTI_CHOICE/OPEN_TEXT/**ENPS**）、`driverCategory`（dict：GROWTH/RECOGNITION/MANAGEMENT/WELLBEING/ALIGNMENT）、`ratingScaleMin/Max`、`isRequired`。
  - `ErpHrSurveyAnswer`（:1458-1487）：`responseId`、`questionId`、`ratingValue`、`selectedOption`、`openText`。
  - `ErpHrSurveyResult`（:1490-1523）：`surveyId`、`departmentId`（可空——整体行）、`totalResponses`、`avgScore`、`eNpsScore`、`driverScores`（VARCHAR 2000，JSON 承载）、`questionBreakdown`（VARCHAR 4000，JSON）、`trendData`（VARCHAR 4000，JSON）、`lastCalculatedAt`。**聚合结果全部字段就绪，无 writer**。
- **预授权判据**（第一批纯预授权）：纯 BizModel 代码逻辑修复（新增 mutation + 校验 + 聚合算法 + 测试），**不触 ORM 结构/会计核心/删除**（respondentHash 防重复用查询校验，不增设 DB UK——UK 触 ORM，见 Non-Goals）；**无 ask-first checkbox**。roadmap RC-R1.9 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrSurveyBiz.java`；`module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrSurveyResponseBiz.java`；`module-hr/erp-hr-dao/src/main/java/app/erp/hr/biz/IErpHrSurveyResultBiz.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrSurveyBizModel.java`；`.../ErpHrSurveyResponseBizModel.java`；`.../ErpHrSurveyResultBizModel.java`；`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrConstants.java`；`.../ErpHrErrors.java`；`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/`（新增测试类）。

## Goals

- **P1-MA2-041 修复**：`ErpHrSurveyBizModel` 新增 `publish`（DRAFT→OPEN，校验题目非空 + 起止日期完整）/ `close`（OPEN→CLOSED，触发聚合）/ `archive`（CLOSED→ARCHIVED）三 mutation；publish 后禁止编辑题目（`defaultPrepareUpdate` 守卫，见 Decision）；新增 `ErpHrConstants.SURVEY_STATUS_OPEN/CLOSED/ARCHIVED`。
- **P1-RC-016 修复**：
  - `ErpHrSurveyResponseBizModel` 新增 `submitResponse` mutation：匿名模式（`isAnonymous=true`）下 `employeeId` 置空 + `respondentHash` = 稳定哈希（employeeId + surveyId 派生，见 Decision）；非匿名模式存 employeeId；同人重复提交拦截（匿名按 respondentHash、非匿名按 employeeId）抛 `ERR_HR_SURVEY_ALREADY_SUBMITTED`；仅 OPEN 问卷可提交。
  - CLOSED 自动聚合：`ErpHrSurveyResultBizModel` 新增 `aggregateResult`（close 时调用）：按部门生成 `ErpHrSurveyResult` 行 + 整体行（departmentId=null）；eNPS = (promoters − detractors) / total × 100（promoters=9-10，detractors=0-6）；driverScores = 按 driverCategory 分组平均（JSON）；questionBreakdown = 按题平均（JSON）；回写 `ErpHrSurvey.totalResponses/completionRate/avgScore/eNpsScore`。
  - 仪表盘查询：`ErpHrSurveyResultBizModel` 新增 `@BizQuery`（评分趋势/部门对比/eNPS/驱动因子，形态见 Decision）。
- owner doc `employee-survey.md`（或 payroll.md 调研段）补实现注记；回填 arm-index P1-RC-016/P1-MA2-041 → `done (RC-R1.9)` + roadmap RC-R1.9 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**：不新增 `(surveyId, respondentHash)` DB UK（防重复用查询校验，单请求并发窗口由事务内 check-then-insert + 平台乐观锁兜底——与 A4.2.144 TOCTOU 登记同型，属 watch-only 边界）；不新增列（聚合结果字段全部就绪）。
- **不做「系统通知目标员工填写」与催填（reminderDays）**：UC-HR-11 流程 5 的通知派发 + 催填 TODO 属独立能力（notify 子系统接线），P1-RC-016 范围未含（arm-index 修复描述未列）；登记 Deferred But Adjudicated。
- **不做前端 AMIS 调研填写页/结果仪表盘页面**（后端 @BizQuery 数据能力落地；页面归前端 successor）。
- **不做「问卷发布后不可再编辑题目」的版本化机制**（新建版本替代编辑）：本期只做 publish 后编辑守卫（拒绝），版本化归 successor。
- **不改真相源**（use-cases/employee-survey 需求契约段；仅补实现注记）。
- **不做开放题文本情感分析/驱动因子分析的语义层**（driverScores 按题-因子聚合即满足 L1「驱动因子分析」的数据层）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/human-resource/use-cases.md`（L1 UC-HR-11）+ `docs/design/human-resource/`（调研设计段锚点）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）+ `docs/audits/2026-08-07-0530-rc-ma4-a4-2-22-26-hr-payroll-survey-runtime.md`（A4.2.25/26 运行时证据）
- Skill Selection Basis: 实现面 = BizModel mutation + 聚合算法 + 哈希/JSON 工具（`nop-backend-dev`：@BizMutation/@BizQuery/@Name 签名、CrudBizModel 生命周期钩子、跨实体访问规则、ErrorCode 定义、平台工具[StringHelper/JsonTool]）；测试（`nop-testing`：JunitAutoTestCase/IGraphQLEngine 断言 + _cases/ 快照录制）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新增 infra/config。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-hr/erp-hr-service`。

## Execution Plan

### Phase 1 - 状态机 + publish 后编辑守卫（P1-MA2-041）

Status: planned
Targets: `IErpHrSurveyBiz.java`；`ErpHrSurveyBizModel.java`；`ErpHrConstants.java`；`ErpHrErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无（既有基线）

- [ ] `Decision` **publish 后编辑守卫的守卫面**：选项 A（推荐）= `ErpHrSurveyBizModel` 覆写 `defaultPrepareUpdate`——status != DRAFT 时拒绝对 `questions` 子表及 `surveyType/title/startDate/endDate/isAnonymous` 等问卷配置字段的修改（抛 `ERR_HR_SURVEY_PUBLISHED_IMMUTABLE`）；选项 B = 仅靠前端约束（无后端守卫）。备选与理由记录于本 Decision；**残留风险**：defaultPrepareUpdate 覆写须在既有默认逻辑之后追加（不破坏 CRUD 基线）。默认选项 A。
      - Skill: `nop-backend-dev`
- [ ] `Add` `IErpHrSurveyBiz`：`publish(@Name("surveyId") Long, IServiceContext)` + `close(@Name("surveyId") Long, IServiceContext)` + `archive(@Name("surveyId") Long, IServiceContext)`（@BizMutation）。
      - Skill: `nop-backend-dev`
- [ ] `Add` `ErpHrSurveyBizModel` 实现三 mutation：`publish`（DRAFT→OPEN，校验存在 `questions` 非空 + startDate/endDate 已填[若问卷要求起止日期]，复用 `ERR_HR_SURVEY_ILLEGAL_TRANSITION`[新增] 守卫非法迁移）；`close`（OPEN→CLOSED，调 `aggregateResult`，见 Phase 2）；`archive`（CLOSED→ARCHIVED）。审计字段 = 平台 updatedBy/updateTime 自动填充。
      - Skill: `nop-backend-dev`
- [ ] `Add` 常量 `ErpHrConstants.SURVEY_STATUS_DRAFT/OPEN/CLOSED/ARCHIVED`（对齐 dict 既有值）+ 错误码 `ERR_HR_SURVEY_ILLEGAL_TRANSITION` + `ERR_HR_SURVEY_PUBLISHED_IMMUTABLE`（按 Decision，前缀对齐 hr 模块 `ERR_HR_` 既有约定）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] DRAFT→OPEN→CLOSED→ARCHIVED 全链可达 + 非法迁移拒绝；publish 时题目/日期校验生效
- [ ] publish 后编辑守卫生效（DRAFT 可编辑 / 非 DRAFT 拒绝问卷配置修改）

### Phase 2 - 匿名提交 + 防重复 + 自动聚合 + eNPS + 仪表盘（P1-RC-016）

Status: planned
Targets: `IErpHrSurveyResponseBiz.java`；`ErpHrSurveyResponseBizModel.java`；`IErpHrSurveyResultBiz.java`；`ErpHrSurveyResultBizModel.java`；`ErpHrConstants.java`；`ErpHrErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1 完成

- [ ] `Decision` **respondentHash 生成与防重复校验**：选项 A（推荐）= 哈希 = SHA-256(employeeId + ":" + surveyId)（平台 Digest/`StringHelper` 工具，执行期按 `nop-backend-dev` skill 选平台 util）——稳定可复算；重复校验 = `submitResponse` 事务内按 `(surveyId, respondentHash)` 或 `(surveyId, employeeId)`（非匿名）查询既有答卷，命中抛 `ERR_HR_SURVEY_ALREADY_SUBMITTED`；选项 B = 随机 salt 哈希（不可复算、防重复需存哈希表——放弃）。备选与理由记录于本 Decision；**残留风险**：check-then-insert 单请求窗口（无 DB UK，与 A4.2.144 TOCTOU 同型 watch-only 边界——登记 Deferred，不阻塞）。默认选项 A。
      - Skill: `nop-backend-dev`
- [ ] `Add` `ErpHrSurveyResponseBizModel.submitResponse(@Name("surveyId") Long, @Name("answers") List<...>, IServiceContext)`：校验问卷 status==OPEN（非 OPEN 抛错误码）→ 匿名模式 employeeId 置空 + respondentHash 写入 / 非匿名存 employeeId → 防重复校验（Decision 机制）→ 写 `ErpHrSurveyResponse`（submittedAt=now, isComplete=true）+ `ErpHrSurveyAnswer` 行（ratingValue/selectedOption/openText 按题写入）→ 递增 `ErpHrSurvey.totalResponses`。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **eNPS 与聚合口径**：eNPS = (promoters − detractors) / total × 100（promoters = ratingValue ≥ 9 的 ENPS 题评分，detractors ≤ 6；7-8 passive 不计）；`avgScore` = 全部 RATING 题（含 ENPS 题）评分的算术平均；`driverScores` = 按 `driverCategory` 分组题目的评分平均（JSON map）；`questionBreakdown` = 按题平均（JSON）；`trendData` = 按同 surveyType 历史已 CLOSED 问卷的 avgScore/eNpsScore 序列（JSON 数组，无历史则为空数组）；整体行（departmentId=null）+ 按员工部门分组行（员工部门经 `ErpHrEmployee.departmentId` 解析）。备选与理由记录于本 Decision；**残留风险**：trendData 依赖同类型历史问卷存在（无历史时空数组，语义明确）。
      - Skill: `nop-backend-dev`
- [ ] `Add` `ErpHrSurveyResultBizModel.aggregateResult(@Name("surveyId") Long, IServiceContext)`：按 Decision 口径计算并 upsert（按 surveyId+departmentId 定位，lastCalculatedAt=now）`ErpHrSurveyResult` 行（整体 + 部门）；回写 `ErpHrSurvey.totalResponses/completionRate[responses/目标人数, 无目标人数时置 null]/avgScore/eNpsScore`；由 `close` 触发（Phase 1）。
      - Skill: `nop-backend-dev`
- [ ] `Add` 仪表盘查询：`ErpHrSurveyResultBizModel` 新增 `@BizQuery`（形态按 Decision：如 `getSurveyDashboard(surveyId)` 返回整体+部门行 + trendData 序列，供 AMIS 直接渲染）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 匿名提交 respondentHash 落库 + employeeId 置空；同人重复提交被拦截（匿名/非匿名双路径）；非 OPEN 问卷提交被拒
- [ ] close → aggregateResult 生成整体 + 部门行，eNPS/avgScore/driverScores/questionBreakdown/trendData 数值正确（含 promoters/detractors 边界）
- [ ] 仪表盘 @BizQuery 可查询（整体 + 部门对比 + 趋势数据可达）

### Phase 3 - dedicated 测试

Status: planned
Targets: `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrSurveyLifecycle.java`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [ ] `Add` 测试矩阵（DAO seed + GraphQL 双路径，镜像 `TestErpHrSurveyCrudSmoke` 既有 seed 范式 + `_cases/` 快照录制）：① publish 校验（无题目拒绝/日期缺失拒绝/非法迁移拒绝）+ DRAFT→OPEN；② 匿名提交：respondentHash 非空 + employeeId 空 + 重复提交拒绝（错误码）；③ 非匿名提交：employeeId 存储 + 同人重复拒绝；④ 非 OPEN 问卷提交拒绝；⑤ close → aggregateResult：多部门答卷 → 整体行 + 部门行数值断言（eNPS 边界：promoters/detractors/passive 各档）+ driverScores/questionBreakdown JSON 内容 + survey 头 totalResponses/avgScore/eNpsScore 回写；⑥ archive CLOSED→ARCHIVED；⑦ 仪表盘 @BizQuery 返回结构断言。
      - Skill: `nop-testing`
- [ ] `Proof` GraphQL 冒烟断言（`graphQLEngine.executeRpc` 调 `ErpHrSurvey__publish/close/archive` + `ErpHrSurveyResponse__submitResponse` + `ErpHrSurveyResult__getSurveyDashboard`，证明 mutation/query 经 GraphQL 可达）+ `@NopTestConfig` 隔离（对齐既有 `enableActionAuth=FALSE` 范式）+ 快照录制。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 测试矩阵全绿：`mvn test -pl module-hr/erp-hr-service` 全绿（既有 tests 零回归）
- [ ] 匿名防重复/聚合数值/状态机全链断言落地（无「守卫存在但零覆盖」缺口）；GraphQL 可达性有证据

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: planned
Targets: `docs/design/human-resource/`（调研设计段）；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3 完成

- [ ] `Add` owner doc 补注：调研段补「publish/close/archive 状态机 + submitResponse 匿名防重复 + CLOSED 自动聚合 + eNPS/仪表盘查询」实现注记（mutation 名 + 哈希口径 + 聚合口径 + 通知/催填 successor 注记）；不修改需求契约段（真相源冻结条款遵守）。
      - Skill: none
- [ ] `Add` arm-index P1-RC-016 / P1-MA2-041 行「修复状态」→ `done (RC-R1.9)` + 修复落地摘要；roadmap RC-R1.9 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none

Exit Criteria:

- [ ] arm-index/roadmap 状态回填 + owner doc 补注落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: accept（2026-08-08 draft review）— 格式合规（模板全部必需段齐全）、基线证据已对照实仓核验（BizModel 桩/空 IBiz 接口/常量缺失/dict 四值/ORM 列与 propId/UC-HR-11 L1 逐字/arm-index+roadmap+expander 行全部吻合）；范围边界清晰（Non-Goals + 两项 Decision 均有默认选项与裁决记录 + 预授权判据无 ask-first）；Minor：错误码命名统一为 `ERR_HR_` 前缀（`ERR_HR_SURVEY_ALREADY_SUBMITTED`/`ERR_HR_SURVEY_PUBLISHED_IMMUTABLE`），Closure Status Note 随激活同步更新

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

### 调查通知派发与催填（reminderDays TODO）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: UC-HR-11 流程 5「系统通知目标员工填写」+ 催填属 notify 子系统接线能力，P1-RC-016/P1-MA2-041 修复范围（arm-index 修复描述）未含；本期状态机与聚合数据层落地后，通知可独立接线。
- Successor Required: yes（触发条件 = 后续批次/人工裁决 notify 接线，或目标员工发送机制立项）

### (surveyId, respondentHash) DB UK（防重复并发强约束）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 触 ORM 结构变更（加 UK）属第一批纯预授权边界外；单请求并发窗口由事务内 check-then-insert + 平台乐观锁兜底（与 A4.2.144 TOCTOU 同型 watch-only）。
- Successor Required: no（触发条件 = 出现匿名问卷重复提交的活跃并发证据）

### 问卷发布后版本化（新建版本替代编辑）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 异常段「问卷发布后不可再编辑题目（可新建版本）」——本期落地「不可编辑」守卫（拒绝），版本化机制为扩展能力。
- Successor Required: no

## Closure

Status Note: 待执行（active）

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计

Follow-up:

- 待执行后填写（范围内项目全落地后关闭）
