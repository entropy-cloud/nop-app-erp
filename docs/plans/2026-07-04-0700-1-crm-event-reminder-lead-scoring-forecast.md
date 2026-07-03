# 2026-07-04-0700-1-crm-event-reminder-lead-scoring-forecast CRM 活动提醒 + 线索评分 + 销售预测

> Plan Status: completed
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/extended-roadmap.md` 工作项 3.2 / 3.3 / 3.4（M3 CRM 续）；`docs/design/crm/README.md` §业务规则2/4、`docs/design/crm/lead-scoring.md`、`docs/design/crm/sales-forecast.md`
> Related: `2026-07-04-0549-2-crm-lead-opportunity-quotation-conversion.md`（CRM 3.1 转化基线；其 `Deferred But Adjudicated` 显式将 3.2/3.3/3.4 指向"下一 CRM 计划"——本计划即该后继）；`2026-07-02-2237-2-manufacturing-mrp-engine.md`（FORECAST 需求来源 deferred 项）
> Audit: required

## Current Baseline

- CRM CRUD 全 done（`crud-roadmap.md`）。CRM 3.1 转化已完成（计划 `2026-07-04-0549-2`）：`ErpCrmLead`（`module-crm/model/app-erp-crm.orm.xml:184`）`docStatus` 状态机（qualify/lose/cancel）+ 漏斗阶段流转 `moveStage`（写 `ErpCrmLeadConvLog`）+ 转化闭环（`convertToCustomer`/`convertToQuotation`）+ 线索查重均已在 `ErpCrmLeadBizModel`（112 行）落地。
- **3.2 活动提醒基线**：`ErpCrmEvent`（orm.xml:382）已建模——`eventType`(CALL/EMAIL/MEETING/TASK)、`status`(PLANNED/COMPLETED/CANCELLED)、`startDateTime`/`endDateTime`/`duration`/`reminderMinutesBefore`/`isRecurrent`/`parentEventId`/`priority`/`relatedLeadId` 齐全；`ErpCrmActivity`（轻量操作日志，orm.xml:445）、`ErpCrmEventCategory` 已建模。`ErpCrmEventBizModel`/`ErpCrmActivityBizModel` 为 15 行 codegen 空壳。**剩余差距**：Event 完成/取消时不回写 Lead 的 `lastContactDate`/`nextActivityDate`（字段已在 Lead 上，orm 无 formula，需 BizModel 派生）；无到期事件提醒查询方法；活动时间线聚合查询缺失。`erp-crm/event-status`/`erp-crm/event-priority`/`erp-crm/activity-type`/`erp-crm/event-type` 字典已就绪。
- **3.3 线索评分基线**：评分四实体已建模——`ErpCrmLeadScoreConfig`（orm.xml:519，configName/isActive/effectiveFrom-To/autoQualifyThreshold/minScoreForFollowUp）、`ErpCrmLeadScoreConfigLine`（orm.xml:546，criterionCode/weight/scoringMethod/lookupTable/formula/maxScore/sequence）、`ErpCrmLeadScore`（orm.xml:576，configId/totalScore/scoreBreakdown/autoQualified/triggeredAction/calculatedAt/triggerEvent）、`ErpCrmLeadScoreLine`（configLineId/criterionCode 冗余/rawValue/lookupValue/rawScore/weightedScore；自身有 `scoreId`→ErpCrmLeadScore 的 to-one）。字典 `erp-crm/scoring-method`（LOOKUP/FORMULA/BOOLEAN）**已就绪**（orm.xml:67）。`ErpCrmLeadScoreBizModel` 等 4 个 BizModel 均为 15 行空壳，`IErpCrmLeadScoreBiz` 接口已生成。**ORM 与设计差距**：`ErpCrmLead` **无 `score`/`scoreId` 字段**（lead-scoring.md 设计称"回写 lead.score"，但 Lead ORM 无该列）——本计划按"不扩 ORM"处理，Lead 当前分数由"按 leadId + calculatedAt DESC 取最新 ErpCrmLeadScore.totalScore"派生（见 Phase 2 Decision）。**剩余差距**：无评分计算引擎（config 驱动 LOOKUP/FORMULA/BOOLEAN 准则 → 归一化 totalScore → 写评分记录 append-only）；无 auto-qualify 阈值触发（复用 3.1 的 `qualify`）。
- **3.4 销售预测基线**：预测四实体已建模——`ErpCrmForecastPeriod`（periodType/periodStart-End/status/isCurrent）、`ErpCrmForecast`（orm 含 periodId/territoryId/teamId/ownerId/commitAmount/upsideAmount/weightedAmount/bestCaseAmount/opportunityCount/commitOpportunityCount/expectedClosedRevenue/lastCalculatedAt/notes）、`ErpCrmForecastLine`（leadId/probability/expectedRevenue/weightedRevenue/forecastCategory/includedInCommit/stageName 快照）、`ErpCrmForecastAccuracy`（commitAmount/upsideAmount/actualClosedRevenue/commitAccuracy/upsideAccuracy/deviationAmount）。`ErpCrmForecastBizModel`/`ErpCrmForecastPeriodBizModel`/`ErpCrmForecastAccuracyBizModel`/`ErpCrmForecastLineBizModel` 均为 15 行空壳，IBiz 接口已生成。预测输入字段（Lead 的 probability/expectedRevenue/expectedCloseDate/ownerId/teamId）均就绪。**剩余差距**：无 `refreshForecast` 聚合引擎（按期间 × owner 聚合 → commit/upside/best-case 分类 → 层级 rollup 团队→区域→公司）；无期间状态机（OPEN/FROZEN/CLOSED）；无期间关闭后准确率计算。
- 跨域：CRM 域无独立业财过账 businessType（`crm/README.md §业财过账`）；三个子系统均不出域（评分/预测仅读 CRM Lead/Event/Stage/Team/Territory）。无 ORM 模型变更、无 codegen 重生成需求（实体/字典/IBiz 全就绪）。

## Goals

- **3.2 活动提醒 + 活动时间线**：Event `status → COMPLETED/CANCELLED` 时派生回写关联 Lead 的 `lastContactDate`（最近已完成事件）/`nextActivityDate`（最近未来 PLANNED 事件）；提供到期/临近事件提醒查询方法（供 nop-job 调用，config-gated）；提供 Lead 活动时间线聚合查询（Event + Activity 按时间倒序）。
- **3.3 线索评分引擎**：config 驱动评分计算 `recalculateScore(leadId)`——加载 `isActive=true` 的 LeadScoreConfig，按 configLine 的 `scoringMethod`（LOOKUP 查值表 / FORMULA 表达式 / BOOLEAN 匹配）逐准则计分，归一化 totalScore（0-100），写 `ErpCrmLeadScore`+Line（append-only 行级快照冗余 criterionCode/rawScore/weightedScore）；Lead 当前分数 = 最新一条（按 `calculatedAt DESC`，无 Lead 字段写回）；`totalScore ≥ autoQualifyThreshold` 且 leadType=LEAD 且 docStatus=NEW 时复用 3.1 `qualify` 自动转商机（config-gated `lead-scoring.auto-qualify`）。
- **3.4 销售预测引擎**：`refreshForecast(periodId)`——查期间内 leadType=OPPORTUNITY & docStatus=QUALIFIED & expectedCloseDate 落入期间的商机，按 ownerId 聚合 commit/probability≥阈值 / upside / best-case / weighted，写 `ErpCrmForecast`+`ErpCrmForecastLine`（商机级快照），层级 rollup（团队→区域→公司为聚合行）；期间状态机 `OPEN → FROZEN`（冻结不再重算）/`OPEN → CLOSED`（关闭后算 `ErpCrmForecastAccuracy`）；FROZEN/CLOSED 期间拒绝重算。
- 三子系统均 config-gated、不出域、零核心实体污染（sales/master-data 零字段新增）。

## Non-Goals

- **营销活动 UTM 归因分析报表**（`ErpCrmCampaign` UTM 字段已建模，归因报表属 nop-report 独立面）。
- **销售序列/跟进流程（Sales Sequence/Cadence）**（`sales-sequence.md`，Sequence/SequenceStep/Assignment/LeadSequenceProgress 实体已建模但属独立结果面，非本工作项 3.2/3.3/3.4）。
- **线索漏斗分析报表 / 线索查重自动合并**（3.1 已交付查重提示，自动合并 config 默认关；漏斗报表属 nop-report）。
- **评分 FORMULA 准则的完整规则引擎 / 邮件打开/回复行为跟踪集成**（FORMULA 首版仅支持简单表达式；ENGAGEMENT_SCORE 的 Event 计数为简单 count 聚合；邮件跟踪服务集成属独立面）。
- **nop-job cron 实际注册/调度**（本计划交付可经 GraphQL/测试调用的 BizModel 方法；cron 注册为 config-gated 接线点，实际调度器注册为 Follow-up）。
- **预测配额（Quota）混入 / 多币种汇率换算**（预测与配额两套独立数据；多币种统一换算属独立面）。
- **CRM 域独立业财过账**（CRM 无 businessType，不产生凭证）。

## Task Route

- Type: `implementation-only change`（无 ORM 模型变更——实体/字典/IBiz 接口全就绪，仅 BizModel 自定义动作 + config-gated 门控 + 跨实体同域调用）
- Owner Docs: `docs/design/crm/README.md`（§业务规则2 活动时间线派生 / §业务规则4 事件提醒 Job）、`docs/design/crm/lead-scoring.md`（评分引擎权威设计）、`docs/design/crm/sales-forecast.md`（预测引擎权威设计）、`docs/design/crm/state-machine.md`（Lead docStatus，auto-qualify 复用 NEW→QUALIFIED）
- Skill Selection Basis: BizModel 自定义动作 + config 驱动引擎 + 同域跨实体调用 + 状态机（预测期间）→ 加载 `nop-backend-dev`；评分/预测归一化与 rollup 逻辑由该技能路由的模式约束

## Infrastructure And Config Prereqs

- 配置项（均 config-gated，默认值见各设计文档配置点表）：
  - `erp-crm.event-reminder-enabled`（默认 true，是否启用到期事件提醒查询）
  - `erp-crm.lead-scoring.auto-qualify`（默认 true，评分达标是否自动转商机）
  - `erp-crm.lead-scoring.recalc-on-lead-update`（默认 true，线索评分相关字段变更是否触发重算）
  - `erp-crm.forecast.commit-threshold`（默认 80）/ `erp-crm.forecast.upside-threshold`（默认 30）
  - `erp-crm.forecast.accuracy-auto-compute`（默认 true，期间关闭后是否自动算准确率）
- 无外部服务/端口/密钥依赖；无数据迁移；无 codegen 重生成。

## Execution Plan

### Phase 1 - 活动提醒 + 活动时间线派生（工作项 3.2）

Status: completed
Targets: `ErpCrmEventBizModel.java`、`ErpCrmActivityBizModel.java`；`ErpCrmErrors`/`ErpCrmConstants`；新增 support 类（如 `EventTimelineAggregator`）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无（3.1 Lead 基线已就绪）

- [x] Add: `ErpCrmEventBizModel` —— Event `complete`/`cancel` 动作（`status: PLANNED → COMPLETED`/`→ CANCELLED`，校验迁移合法性），完成后调派生器回写关联 Lead 的 `lastContactDate`（取最近 COMPLETED 事件 startDateTime 最大值）/`nextActivityDate`（取最近未来 PLANNED 事件 startDateTime 最小值）；Event 无关联 Lead 时跳过派生。
  - Skill: `nop-backend-dev`
- [x] Add: 到期/临近事件提醒查询方法 `findDueReminders(windowMinutes)`（`status=PLANNED` 且 `startDateTime BETWEEN now AND now+window`，按 `reminderMinutesBefore` 过滤）—— 供 nop-job 调用（config-gated `event-reminder-enabled`），方法本身可经 GraphQL query 验证。
  - Skill: `nop-backend-dev`
- [x] Add: Lead 活动时间线聚合查询 `getLeadTimeline(leadId)`（Event + Activity 合并按时间倒序，返回只读聚合视图；走同域 to-many 查询，不跨域）。
  - Skill: `nop-backend-dev`
- [x] Decision: `lastContactDate`/`nextActivityDate` 派生时机——选择"Event 状态变更时即时回写 Lead"（推模式，查询零成本）而非"查询时实时聚合"（拉模式，每次查 Lead 都扫 Event 表）。理由：Lead 列表高频加载，推模式避免 N+1；Event 状态变更是低频写。残留风险：批量历史回填需一次性脚本——记为 Follow-up（触发条件：存量 Event 数据需补算 Lead 派生字段时）。
  - Skill: `nop-backend-dev`

Exit Criteria:

> 交付 Event 状态机 + 派生回写 + 提醒查询 + 时间线聚合。完整仓库验证归 Closure Gates。

- [x] Event complete/cancel 迁移合法且非法迁移被拒；completed Event 回写 Lead.lastContactDate 可验证
- [x] findDueReminders / getLeadTimeline 返回正确过滤/排序结果

### Phase 2 - 线索评分引擎（工作项 3.3）

Status: completed
Targets: `ErpCrmLeadScoreBizModel.java`、`ErpCrmLeadScoreConfigBizModel.java`；新增 support 类（如 `LeadScoringEngine`）；`ErpCrmErrors`/`ErpCrmConstants`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1（ENGAGEMENT_SCORE 准则读 ErpCrmEvent 计数，依赖活动时间线/事件已建模）

- [x] Add: 评分计算 `recalculateScore(leadId, triggerEvent)`——加载 `isActive=true` 的 `ErpCrmLeadScoreConfig`（同一时间仅一个生效，多 active 抛错或取最新——记 Decision），遍历 configLine 按 `scoringMethod`：LOOKUP 从 Lead 字段取值查 `lookupTable` JSON 取 rawScore；FORMULA 按表达式计（首版简单算术，ENGAGEMENT_SCORE 走 Event count 聚合）；BOOLEAN 匹配 maxScore 否则 0；算 `weightedScore = rawScore × weight / Σweight`，归一化 `totalScore = Σ(weightedScore)/Σ(maxPossibleWeighted) × 100`；写 `ErpCrmLeadScore`(configId/totalScore/scoreBreakdown JSON/triggerEvent/calculatedAt) + N 行 `ErpCrmLeadScoreLine`(行级快照冗余)。评分记录 append-only，不覆盖旧记录；Lead 当前分数 = 按 leadId + `calculatedAt DESC` 取最新 ErpCrmLeadScore（无 Lead 字段写回）。
  - Skill: `nop-backend-dev`
- [x] Decision: Lead↔Score 联结方式——选"派生查询最新 ErpCrmLeadScore"（Option B，不扩 ORM）而非"加 Lead.scoreId 前向指针"（Option A 会引入 ORM 变更 + codegen 重生成，破坏 implementation-only 定性）。理由：评分历史 append-only，取最新即可表达当前分；列表页取当前分走子查询/缓存而非每行 N+1（残留风险：超大量 Lead 列表取分需优化查询——记 Follow-up，触发条件：列表性能压测不达标时）。偏离 `lead-scoring.md` "回写 lead.score" 表述（该字段 ORM 不存在），在 Phase 3 owner-doc 补注中记录。
  - Skill: `nop-backend-dev`
- [x] Add: 阈值→动作——`totalScore ≥ autoQualifyThreshold` 且 `leadType=LEAD` 且 `docStatus=NEW` 且 config-gated `lead-scoring.auto-qualify=true` → 调 3.1 的 `IErpCrmLeadBiz.qualify`（复用 NEW→QUALIFIED），记 `triggeredAction=AUTO_QUALIFY`/`autoQualified=true`；`minScoreForFollowUp ≤ totalScore < autoQualifyThreshold` → `triggeredAction=NOTIFY_OWNER`（不改 docStatus）；低于 minScoreForFollowUp → NONE。已 QUALIFIED/CONVERTED/LOST/CANCELLED 的 Lead 不参与自动 qualify（手动 recalc 仍出分但不转）。
  - Skill: `nop-backend-dev`
- [x] Add: Lead 字段变更触发——`lead-scoring.recalc-on-lead-update=true` 时，Lead 的评分相关字段（sourceId/companyName/expectedRevenue/industry 等）保存后异步触发 `recalculateScore(triggerEvent=LEAD_UPDATE)`（config-gated）。
  - Skill: `nop-backend-dev`
- [x] Decision: 多 `isActive=true` 配置处理——选择"抛 `ERR_MULTIPLE_ACTIVE_SCORE_CONFIG`"（强制管理员保持单一生效口径，符合设计规则8）而非"静默取最新"。替代方案：取最新 active——被否（口径漂移不可追溯）。
  - Skill: `nop-backend-dev`
- [x] Decision: 无 active 配置时——`recalculateScore` 直接返回（不抛错，评分可选），记 `triggeredAction=NONE`。理由：未配置评分规则的租户不应阻断 Lead 正常保存。
  - Skill: `nop-backend-dev`

Exit Criteria:

> 交付 config 驱动评分引擎 + auto-qualify 触发。完整仓库验证归 Closure Gates。

- [x] recalculateScore 对 LOOKUP/BOOLEAN/FORMULA(简单) 三类准则产出正确归一化 totalScore + 行级快照
- [x] auto-qualify 阈值触发调 qualify 且非 NEW/非 LEAD 不触发；append-only 历史可追溯

### Phase 3 - 销售预测引擎 + 端到端 + 文档/日志

Status: completed
Targets: `ErpCrmForecastBizModel.java`、`ErpCrmForecastPeriodBizModel.java`、`ErpCrmForecastAccuracyBizModel.java`；新增 support 类（如 `ForecastAggregator`）；`ErpCrmErrors`/`ErpCrmConstants`；`docs/design/crm/README.md`/`lead-scoring.md`/`sales-forecast.md` 实现偏离补注；`docs/logs/`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2（预测输入依赖 Lead 的 probability/stage 基线，3.1 已就绪）

- [x] Add: `ErpCrmForecastPeriodBizModel` 期间状态机——`OPEN → FROZEN`（freeze，锁定不再重算，`lastCalculatedAt` 固定）/`OPEN → CLOSED`（closePeriod，触发准确率计算）；FROZEN/CLOSED 拒绝 `refreshForecast`（抛 `ERR_FORECAST_PERIOD_NOT_OPEN`）；`isCurrent` 唯一性（同一 periodType 仅一个 current）。
  - Skill: `nop-backend-dev`
- [x] Add: `refreshForecast(periodId)` 聚合引擎——查期间内 `leadType=OPPORTUNITY` & `docStatus=QUALIFIED` & `expectedCloseDate BETWEEN periodStart AND periodEnd` 的商机；按 ownerId 分组算 commit(`probability ≥ commitThreshold`)/upside(`upsideThreshold ≤ probability < commitThreshold`)/best-case(全部)/weighted(`Σ expectedRevenue × probability/100`)；upsert `ErpCrmForecast`(periodId×ownerId) + 重建 `ErpCrmForecastLine`(每商机一条快照：probability/expectedRevenue/weightedRevenue/forecastCategory/includedInCommit/stageName)；层级 rollup：聚合行（teamId/territoryId 非空、ownerId 空）= SUM 下级对应字段；`docStatus→CONVERTED` 的商机累计 `expectedClosedRevenue`。
  - Skill: `nop-backend-dev`
- [x] Add: 期间关闭后准确率——`closePeriod` 时（config-gated `forecast.accuracy-auto-compute`）创建 `ErpCrmForecastAccuracy`：`commitAccuracy = 1 - |commitAmount - actualClosedRevenue| / MAX(...)`、upside 同口径、`deviationAmount`；按 ownerId/teamId/territoryId 维度对齐。
  - Skill: `nop-backend-dev`
- [x] Decision: 多币种——首版不做汇率换算，`ErpCrmForecast.currencyId` 取商机币种，跨币种聚合时按 Lead 主币种记（多币种统一换算归 Non-Goal）。理由：避免引入汇率主数据依赖；统一换算属独立面。
  - Skill: `nop-backend-dev`
- [x] Proof: 端到端测试 `TestErpCrmForecastAndScoring`（JunitAutoTestCase + GraphQL）——覆盖：评分 LOOKUP+BOOLEAN 准则计分 + auto-qualify；预测 refreshForecast 单 owner commit/upside/best-case 分类 + 层级 rollup；期间 OPEN→CLOSED 准确率计算；FROZEN 拒绝重算。指定验证命令：`mvn test -pl module-crm -am`。
  - Skill: `nop-testing`
- [x] Add: 文档对齐——`crm/README.md`/`lead-scoring.md`/`sales-forecast.md` 补实现偏离补注（评分/预测方法落地实体 BizModel 名、config 门控默认值、多 active 配置策略）；更新 `docs/logs/{year}/{month}-{day}.md`。
  - Skill: none

Exit Criteria:

> 交付预测聚合 + 期间状态机 + 准确率 + 端到端。完整仓库验证归 Closure Gates。

- [x] refreshForecast 产出正确 commit/upside/best-case/weighted 分类 + 层级 rollup + 商机级 ForecastLine 快照
- [x] 期间 FROZEN/CLOSED 拒绝重算；closePeriod 触发准确率计算可验证

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0d5c01ba0ffeNXrvEkKfQiAF3D) because 基线错误声称 `ErpCrmLead.scoreId` 存在（实际 Lead 无 score 字段，score to-one 属 ErpCrmLeadScoreLine），且 `erp-crm/scoring-method` 字典误标"需补齐"（实已就绪）。
- Independent draft review iteration 2: accept (ses_0d5bcb8f0ffe2561q1kRBbeLTG) after 修订——基线改为"Lead 无 score 字段，当前分派生查询最新 ErpCrmLeadScore（Option B 不扩 ORM）"+ Decision 记录选择/替代/残留风险；scoring-method 字典标"已就绪"；Deferred 补"Lead 列表取分性能优化"。所有规则 PASS，无新阻塞问题。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选后关闭。完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（3.2 活动提醒/时间线 + 3.3 评分引擎 + 3.4 预测引擎）
- [x] 相关文档对齐（crm/README、lead-scoring、sales-forecast 偏离补注）
- [x] 已运行验证：`mvn clean install -DskipTests`（全量编译 146 reactor 模块全绿）+ `mvn test -pl module-crm/erp-crm-service -am`（CRM 模块 24 tests 全绿 + sales 跨域零回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### nop-job cron 实际注册/调度（事件提醒 / 评分定时批量 / 预测定时重算）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划交付可经 GraphQL/测试调用的 BizModel 方法（findDueReminders / recalculateScore / refreshForecast），核心业务逻辑可验证；cron 注册属平台调度接线，不阻断功能正确性。
- Successor Required: `yes`（触发条件：生产部署需定时自动触发时，注册 nop-job 调度调用对应方法）

### Lead 派生字段存量回填脚本（lastContactDate/nextActivityDate）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 新 Event 状态变更即时回写；仅存量历史 Event 数据无派生值。
- Successor Required: `yes`（触发条件：导入存量 Event 数据需补算 Lead 派生字段时）

### Lead 列表取当前分数性能优化

- Classification: `optimization candidate`
- Why Not Blocking Closure: 派生查询最新 ErpCrmLeadScore 功能正确；仅超大量 Lead 列表批量取分时可能需子查询/缓存优化。
- Successor Required: `yes`（触发条件：Lead 列表页取当前分数性能压测不达标时）

### 销售序列/跟进流程（Sales Sequence，工作项外但实体已建模）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属独立结果面（sales-sequence.md），非 3.2/3.3/3.4 工作项。
- Successor Required: `yes`（触发条件：销售序列/cadence 需求落地时）

## Closure

Status Note: 三阶段全部交付（3.2 活动提醒/时间线 + 3.3 评分引擎 + 3.4 预测引擎）。`mvn clean install -DskipTests` 全绿（146 reactor 模块），`mvn test -pl module-crm/erp-crm-service -am` 全绿（24 CRM tests + sales 跨域 69 tests 零回归）。独立结束审计已由新会话子代理执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文）
- Evidence:
  - 实时代码核验（anti-hollow）：`ErpCrmEventBizModel`（complete/cancel 状态机 + findDueReminders config-gated + getLeadTimeline 委托 `EventTimelineAggregator`）、`LeadActivityDerivationHelper`（推模式回写 lastContactDate/nextActivityDate）、`LeadScoringEngine`（LOOKUP/FORMULA/BOOLEAN 三准则 + 归一化 totalScore + append-only 历史快照 + auto-qualify 经 `ErpCrmLeadProcessor.qualify`）、`ForecastAggregator`（refreshForecast 按 ownerId 聚合 commit/upside/best-case/weighted + 个人→团队→公司层级 rollup + 清旧重建 ForecastLine + computeAccuracy）、`ErpCrmForecastPeriodBizModel`（OPEN→FROZEN/CLOSED 状态机 + FROZEN/CLOSED 拒绝重算）均为完整实现，无空方法体/return null 占位/吞异常。
  - 运行时接线核验：BizModel 方法经 `@BizMutation`/`@BizQuery` 自动注册为 GraphQL 动作，端到端测试经 `IGraphQLEngine.executeRpc` 调用 `ErpCrmLeadScore__recalculateScore`/`ErpCrmForecast__refreshForecast`/`ErpCrmForecastPeriod__freeze`/`ErpCrmForecastPeriod__closePeriod` 验证可达。
  - 验证命令重跑（全绿）：`mvn test -pl module-crm/erp-crm-service -am -Dtest=TestErpCrmForecastAndScoring` → Tests run: 5, Failures: 0, Errors: 0；`mvn test -pl module-crm/erp-crm-service -am` → CRM 24 tests + sales 69 tests 全绿，BUILD SUCCESS（与 Closure Gates 声明一致）。
  - 文档同步核验：`docs/logs/2026/07-04.md` 含本计划条目；`docs/design/crm/README.md`（§业务规则2/4 实现偏离补注）、`lead-scoring.md`（Option B 派生查询 + 评分方法约定补注）、`sales-forecast.md`（层级 rollup + 清旧重建 + 准确率公式补注）均已更新。
  - Deferred 诚实性：Deferred 项均为 optimization candidate / out-of-scope（nop-job cron 注册、存量回填脚本、列表取分性能优化、销售序列），无范围内的已确认缺陷或契约漂移降级。

Follow-up:

- nop-job cron 注册（见 Deferred，触发条件：生产定时调度需求）
- 存量派生字段回填脚本（见 Deferred，触发条件：存量 Event 导入）
- 销售序列（见 Deferred，触发条件：cadence 需求）
