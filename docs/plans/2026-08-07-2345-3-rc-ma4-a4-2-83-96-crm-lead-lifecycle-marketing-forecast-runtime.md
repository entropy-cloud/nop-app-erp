# 2026-08-07-2345-3 rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime 线索转化前置/评分触发/territory 分配/营销预测配额运行时确认

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.83 / A4.2.84 / A4.2.85 / A4.2.86 / A4.2.87 / A4.2.88 / A4.2.89 / A4.2.90 / A4.2.91 / A4.2.92 / A4.2.93 / A4.2.94 / A4.2.95 / A4.2.96
> Related: `docs/audits/2026-08-05-1030-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（A1.28 §7 存疑点 SP-1..SP-6 + §6 新建 P1-RC-032~036/P2-RC-031~034）、`docs/audits/2026-08-05-1100-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`（A1.29 §7 存疑点 SP-1..SP-8 + §6 新建 P1-RC-037/038/039/P2-RC-035 + reuse P1-MA2-076/P1-MA2-086）
> Audit: required

## Current Baseline

CRM 域两切片（A1.28 线索生命周期 / A1.29 营销-预测-配额-序列-事件提醒）MA1 报告 §7 共列出 14 个静态存疑点（A1.28 SP-1..SP-6 + A1.29 SP-1..SP-8）。CRM 域本身不直接产生会计凭证，故本批存疑点均**不触及业财保护区域探针**（区别于 assets/inventory 批）；但 P1-RC-036（territory 分配 ROUND_ROBIN/LOAD_BALANCED 降级 MANUAL）修复触及 ORM 结构变更须 ask-first。存疑点分两类：(1) 缺陷确认（A4.2.83 P1-RC-033 / A4.2.84 P1-RC-034 / A4.2.87 P1-RC-036 / A4.2.88 P1-RC-032，HEAD 静态判定 = 缺陷，运行时确认闭合维持分级）；(2) 边界/数值/config-gate 行为确认（A4.2.85 评分并发触发时序 + A4.2.86 territory 留空 + A4.2.89~94 派生/归因/预测/配额边界 + A4.2.95/96 cron config-gate）。

- **A4.2.83（A1.28 SP-1 NEW 状态 LEAD 实际能否被 convertToCustomer 转化；P1-RC-033）**：HEAD 静态判定 = 前置条件弱（`convertToCustomerProcessor:21-22` 仅 validateNotConverted+validateLeadType(LEAD) 不查 docStatus；无 NEW 拒绝转化负向测试）。运行时确认 NEW LEAD 经 GraphQL convertToCustomer 是否成功（前置条件未守卫）。
- **A4.2.84（A1.28 SP-2 任意 docStatus OPPORTUNITY 实际能否转报价单；P1-RC-034）**：HEAD 静态判定 = 前置条件弱（`convertToQuotationProcessor:21-25` 不查 docStatus 且不查 isWonStage；单测仅覆盖 QUALIFIED 路径）。运行时确认非 QUALIFIED/won-stage 路径是否成功。
- **A4.2.85（A1.28 SP-3 LEAD_UPDATE 自动评分并发更新下触发时序）**：HEAD 静态判定 = `defaultPrepareUpdate:227-240` 同步触发评分。运行时确认并发更新是否阻塞用户保存 + 是否产生重复 ErpCrmLeadScore 记录。
- **A4.2.86（A1.28 SP-4 territory 引擎无匹配时 territoryId 留空行为）**：HEAD 静态判定 = `assign:70` 返回 null + BizModel:203 if territoryId==null 跳过（lead.territoryId 保持 null "未分配"）。运行时确认 UI/报表展示。
- **A4.2.87（A1.28 SP-5 ROUND_ROBIN 降级 MANUAL 后 ownerId 实际值；P1-RC-036）**：HEAD 静态判定 = `toResult:73-84` 显式将非 MANUAL 方法降级为 MANUAL（degraded=true，AssignmentResult.ownerId 永不设置）；BizModel:144 if ownerId!=null 跳过（lead.ownerId 保持 null "待分配"）。运行时确认分配语义（引擎显式降级不挑人）。**P1-RC-036 修复触及 ORM 结构变更须 ask-first。**
- **A4.2.88（A1.28 SP-6 直接升格分支在其他未审计入口 GraphQL/Delta 补偿实现；P1-RC-032）**：HEAD 静态判定 = grep 主代码直接升格 0 命中（setLeadType(OPPORTUNITY) 唯一命中点在新建 ErpCrmLead 内 createOpportunityFromLead:92），Delta 层未全量扫描。运行时确认 GraphQL 自定义 action / Delta 层是否存在补偿实现。
- **A4.2.89（A1.29 SP-1 UC-CRM-05 lastContactDate 按 COMPLETED 过滤的语义偏差）**：HEAD 静态判定 = `LeadActivityDerivationHelper.latestCompletedStartDateTime:55` 按 COMPLETED 过滤（L1 字面"相关 Event"未限定状态）。运行时确认是否应包含 CANCELLED（倾向接受实现选择——CANCELLED 不算"联系过"）。
- **A4.2.90（A1.29 SP-2 UC-CRM-07 UTM copy 缺失下 utmMedium/utmSource 实际默认值；P1-RC-037）**：HEAD 静态判定 = UTM copy 缺失。运行时确认外部渠道提交不传 utmMedium/utmSource 时字段实际持久化为 NULL（影响归因报表准确性）。
- **A4.2.91（A1.29 SP-3 UC-CRM-07 归因报表缺失下 campaignId 已填但无聚合实际数据状态；P1-RC-038）**：HEAD 静态判定 = campaignId 外键可持久化但无报表消费（`ErpCrmCampaignBizModel.java:11-19` 19 行空 CRUD stub + `ErpCrmReportBizModel.prepareDataset:151-164` 仅 2 报表 + glob `**/report/crm/*.xpt.xml` 仅 2 文件无归因报表模板）。运行时确认是否影响营销 ROI 决策（业务影响 = 报表缺失非数据破坏，但 L1 明确功能点缺失）。
- **A4.2.92（A1.29 SP-4 UC-CRM-10 ForecastAggregator 3 级 rollup 跨 ownerId 边界正确性）**：HEAD 静态判定 = 团队 rollup 按 `ownerTeam:62-72` 映射 ownerId→teamId。运行时确认同一 owner 跨团队迁移场景 rollup 一致性。
- **A4.2.93（A1.29 SP-5 UC-CRM-10 territory tier 缺失下跨区域预测汇总实际行为；P1-RC-039）**：HEAD 静态判定 = Forecast 段无 territory tier 行（ForecastAggregator `:77-101` 仅生成个人/团队/公司行——3 级非 L1 `:228` 要求的"团队→区域→公司"4 级）。运行时确认 Forecast 段在 territory 级管道报表实际展示（0 或跨 territory 聚合失真）。
- **A4.2.94（A1.29 SP-6 UC-CRM-12 QuotaRollupCalculator 显式值优先 team+individual 共存 double-count）**：HEAD 静态判定 = `rollup:94-100` 对 territory 子树所有行（含 team-level explicit + individual-level explicit）求和，可能 double-count。运行时确认 team=1000+个人=500→region=1500 vs L1 严格"region=Σ teams"应为 1000 的偏差面。
- **A4.2.95（A1.29 SP-7 UC-CRM-14/15 FunnelAggregationJob/SequenceOverdueJob cron enabled 默认 false 实际触发链路；P2-RC-035）**：HEAD 静态判定 = cron yaml 注册但 enabled 默认 false（`@cfg:...enabled|false`）+ BizModel 逻辑层强测但 Job bean cron-gating execute() 入口无 dedicated 测试。运行时确认部署 enabled=true 后执行链路（BeanMethodJobInvoker→execute()→cron 空 skip 门控）。
- **A4.2.96（A1.29 SP-8 UC-CRM-14 EMAIL_OPENED/EMAIL_REPLIED 降级 eventType=EMAIL 匹配实际触发面）**：HEAD 静态判定 = 降级后所有 EMAIL 历史均触发步骤推进。运行时确认是否过早推进（邮件未实际打开/回复但步骤已 advance）。

剩余差距：十四项均为只读运行时确认，CRM 域不直接产生会计凭证故不触及业财保护区域探针。缺陷项（A4.2.83 P1-RC-033 / A4.2.84 P1-RC-034 / A4.2.87 P1-RC-036 / A4.2.88 P1-RC-032）修复归 MR1（P1-RC-036 触及 ORM 结构变更须 ask-first；其余纯 BizModel/Processor 预授权）；A4.2.90 P1-RC-037（UTM copy）/ A4.2.91 P1-RC-038（归因报表）/ A4.2.93 P1-RC-039（Forecast territory tier）修复归 MR1（纯 BizModel/报表模板预授权不触 ask-first）；A4.2.95 P2-RC-035（Job bean cron-gating 测试）修复归 MR1（纯测试补充预授权，登记不强制）。本计划仅确认运行时行为以维持/细化裁决，不改变 Q4 强制实现义务。

## Goals

- 对 A4.2.83-A4.2.96 十四项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：缺陷项（A4.2.83/84/87/88 P1-RC-032~036）维持 P1 分级（Q4 强制实现，修复归 MR1）+ 记录运行时证据；A4.2.90 P1-RC-037（UTM copy）/ A4.2.91 P1-RC-038（归因报表）/ A4.2.93 P1-RC-039（Forecast territory tier）维持 P1 + 运行时证据；A4.2.95 P2-RC-035（Job bean cron-gating 测试）维持 P2（登记不强制，config-gate = 部署启用决策）；边界/config-gate 项（A4.2.85/86/89/92/94/96）确认行为正确或登记 watch-only；若运行时发现活跃数据破坏则触发 MR0。
- 完成后回写 roadmap A4.2.83-A4.2.96 `todo → done`，并按裁决更新 arm-index（维持注记，无未经比对新建）。

## Non-Goals

- 不实现直接升格分支（P1-RC-032）/ 转化前置条件守卫（P1-RC-033/P1-RC-034）/ 评分 SCHEDULED 触发器（P1-RC-035）/ territory ROUND_ROBIN 不降级（P1-RC-036）/ UTM copy（P1-RC-037）/ 归因报表（P1-RC-038）/ Forecast territory tier（P1-RC-039）/ funnel-sequence job bean 测试（P2-RC-035）——修复义务归 MR1 R1.0 展开器；P1-RC-036 触及 ORM 结构变更[assignmentMethod]须 ask-first + 独立 plan-audit；其余纯 BizModel/Processor/job-bean 测试补充预授权。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落）。
- 不复跑 MA2 状态机审计（A2.14 CRM Event/sequence 状态机 + P1-MA2-076/P1-MA2-086 resolved 作为既有证据输入，不重新核实行为本身）；不重审 P1-RC-032~039 维度（A1.28/A1.29 已审，本计划仅运行时确认）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-05-1030-rc-ma1-a1-28-crm-f1-lead-lifecycle.md` §5/§6/§7 + `docs/audits/2026-08-05-1100-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md` §5/§6/§7 + `docs/design/crm/`（use-cases.md / state-machine.md / lead-scoring.md / README.md 衔接契约）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划为只读审计，无代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为代码可达性 + 转化 Processor 前置条件守卫 census + 评分触发时序确认 + territory 分配降级行为 + Delta 层补偿实现扫描 + 派生/归因/预测/配额边界 + cron config-gate（grep census / convertToCustomer/Quotation Processor 守卫追踪 / LeadScoringEngine 触发器 census / AssignmentEngine.toResult 降级追踪 / ForecastAggregator/QuotaRollupCalculator rollup 路径追踪 / job yaml + beans.xml wiring 确认），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.83-A4.2.96）

Status: completed
Targets: `docs/audits/2026-08-07-2345-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done ✓；A1.28 done ✓；A1.29 done ✓

- [x] **A4.2.83 NEW 状态 LEAD 实际能否被 convertToCustomer 转化确认（P1-RC-033）**：确认 `convertToCustomerProcessor:21-22` 仅 validateNotConverted+validateLeadType(LEAD) 不查 docStatus；确认无 NEW 拒绝转化负向测试；确认 NEW LEAD 经 GraphQL convertToCustomer 运行时成功（前置条件未守卫）。裁决：维持 P1-RC-033 P1（§2 P1② 异常路径/前置条件未守卫，修复归 MR1 纯 Processor 预授权不触 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.84 任意 docStatus OPPORTUNITY 实际能否转报价单确认（P1-RC-034）**：确认 `convertToQuotationProcessor:21-25` 不查 docStatus 且不查 isWonStage（isWonStage 唯一消费点=board emoji :300）；确认单测仅覆盖 QUALIFIED 路径；确认非 QUALIFIED/won-stage OPPORTUNITY 运行时成功转报价单（前置条件未守卫）。裁决：维持 P1-RC-034 P1（§2 P1②，修复归 MR1 纯 Processor 预授权不触 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.85 LEAD_UPDATE 自动评分并发更新下触发时序确认**：确认 `defaultPrepareUpdate:227-240` 同步触发评分（阻塞用户保存至评分完成）；确认并发更新经乐观锁防重复 ErpCrmLeadScore 记录（@Version 兜底）；确认触发时序对用户保存延迟的实际影响。裁决：登记 watch-only residual（同步触发属实现选择，并发由乐观锁兜底）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.86 territory 引擎无匹配时 territoryId 留空行为确认**：确认 `assign:70` 返回 null + BizModel:203 if territoryId==null 跳过（lead.territoryId 保持 null "未分配"）；确认 UI/报表展示 null = "未分配" 语义。裁决：主路径行为正确闭合（无匹配留空是合理降级，登记 watch-only residual 供报表展示参考）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.87 ROUND_ROBIN 降级 MANUAL 后 ownerId 实际值确认（P1-RC-036）**：确认 `toResult:73-84` 显式将非 MANUAL 方法降级为 MANUAL（degraded=true，AssignmentResult.ownerId 永不设置）；确认 BizModel:144 if ownerId!=null 跳过（lead.ownerId 保持 null "待分配"）；确认 ROUND_ROBIN/LOAD_BALANCED 引擎工作但运行时不挑人。裁决：维持 P1-RC-036 P1（§2 P1① 功能实质偏离验收标准——引擎显式降级不挑人，修复归 MR1 触 ORM 结构变更[assignmentMethod]须 ask-first + 独立 plan-audit §5）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.88 直接升格分支在其他未审计入口补偿实现确认（P1-RC-032）**：grep census 直接升格（setLeadType(OPPORTUNITY) 原地升格非新建）跨 module-crm main + GraphQL 自定义 action + Delta 层；确认主代码直接升格 0 命中（唯一命中点 createOpportunityFromLead:92 在新建 ErpCrmLead 内非原地升格）；确认 Delta 层无补偿实现。裁决：维持 P1-RC-032 P1（§2 P1① 功能完全缺失——直接升格分支运行时不存在，修复归 MR1 纯 BizModel 预授权不触 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.89 lastContactDate 按 COMPLETED 过滤的语义偏差确认**：确认 `LeadActivityDerivationHelper.latestCompletedStartDateTime:55` 按 COMPLETED 过滤（L1 字面"相关 Event"未限定状态）；确认"last contact"语义合理（CANCELLED 不算"联系过"）。裁决：倾向接受实现选择（与 A1.29 §5 倾向接受一致），不记 finding，登记 watch-only 观察。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.90 UTM copy 缺失下 utmMedium/utmSource 实际默认值确认（P1-RC-037）**：确认 UTM copy 缺失（grep UTM copy 路径零命中）；确认外部渠道提交不传 utmMedium/utmSource 时字段实际持久化为 NULL；确认影响后续归因报表准确性。裁决：维持 P1-RC-037 P1（修复归 MR1 纯 BizModel 预授权不触 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.91 归因报表缺失下 campaignId 已填但无聚合实际数据状态确认（P1-RC-038）**：确认 campaignId 外键可被 Lead 持久化但无归因报表消费（`ErpCrmCampaignBizModel.java:11-19` 19 行空 CRUD stub + `ErpCrmReportBizModel.prepareDataset:151-164` 仅 handle lead-conversion-funnel+forecast-accuracy + glob `**/report/crm/*.xpt.xml` 仅 2 文件无 `SELECT campaign.name, count(lead.id), sum(expectedRevenue) GROUP BY campaignId` 归因报表模板）；确认业务影响 = 归因报表完全缺失（L1 明确功能点，campaignId 数据完整仅无聚合视图）。裁决：维持 P1-RC-038 P1（§2 P1① 功能完全缺失——归因报表模板/stub 完全缺失，修复归 MR1 纯 BizModel/报表模板预授权不触 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.92 ForecastAggregator 3 级 rollup 跨 ownerId 边界正确性确认**：确认团队 rollup 按 `ownerTeam:62-72` 映射 ownerId→teamId；确认同一 owner 跨团队迁移场景 rollup 一致性（迁移后归属新团队的 forecast 聚合正确）。裁决：主路径行为正确闭合（ownerTeam 映射驱动 rollup，迁移后按新 teamId 聚合）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.93 territory tier 缺失下跨区域预测汇总实际行为确认（P1-RC-039）**：确认 `getTerritoryPipeline:88-117` 走 QuotaRollupCalculator.accumulatePipeline（含 territory 子树）+ Forecast 段无 territory tier 行（ForecastAggregator `:77-101` 仅生成个人/团队/公司 3 级非 L1 `:228` 要求的"团队→区域→公司"4 级）；确认 Forecast 段在 territory 级管道报表实际展示（0 或跨 territory 聚合失真）。裁决：维持 P1-RC-039 P1（§2 P1① + §4 三判据复核重开[触发条件 Lead.territoryId 现存]，修复归 MR1 纯 BizModel 预授权不触 ask-first——ForecastAggregator 加 territory tier 复用既有 collectSubtreeIds 模式镜像 QuotaRollupCalculator）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.94 QuotaRollupCalculator 显式值优先 team+individual 共存 double-count 确认**：确认 `rollup:94-100` 对 territory 子树所有行（含 team-level explicit + individual-level explicit）求和；确认 team=1000+个人=500→region=1500 vs L1 严格"region=Σ teams"应为 1000 的偏差面。裁决：登记 watch-only residual（可能 P2 successor——L1 字面"区域配额=Σ 团队配额"与显式值优先语义冲突，运行时确认 double-count 偏差面）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.95 FunnelAggregationJob/SequenceOverdueJob cron enabled 默认 false 实际触发链路确认（P2-RC-035）**：确认 cron yaml 注册但 enabled 默认 false（`@cfg:...enabled|false`）+ 全生产 application.yaml 零 override；确认 BizModel 逻辑层强测（testScanOverdueSteps/refreshFunnel）但 Job bean cron-gating execute() 入口无 dedicated TestErpCrmSequenceOverdueJob/TestErpCrmFunnelAggregationJob；确认部署 enabled=true 后执行链路（BeanMethodJobInvoker→execute()→cron 空 skip 门控）。裁决：config-gate = 部署启用决策非契约缺失（与 A4.1.4/A4.2.12/A4.2.13 范式一致），维持 P2-RC-035 P2（Job bean cron-gating 测试缺口归 MR1 纯测试补充预授权，登记不强制）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.96 EMAIL_OPENED/EMAIL_REPLIED 降级 eventType=EMAIL 匹配实际触发面确认**：确认降级后所有 EMAIL 历史均触发步骤推进；确认过早推进风险（邮件未实际打开/回复但步骤已 advance）。裁决：登记 watch-only residual（降级是 config-gated 简化，运行时过早推进风险属部署启用决策）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：十四项存疑点各出 §裁决（主路径闭合 / 维持 P1 + 运行时证据 / 登记 watch-only / config-gate 部署决策 / 触发 MR0）+ §与既有 finding 衔接（P1-RC-032~039 / P2-RC-031~035 / reuse P1-MA2-076/P1-MA2-086 交叉引用）+ §过程纪律自检（checker 退出码门控——无代码变更 actual=baseline；closure-audit 独立性声明）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。CRM 域不直接产生会计凭证，不触及业财保护区域探针。

- [x] 验证报告落盘，含十四项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [x] 每项裁决明确：主路径闭合 / 维持分级（P1 Q4 强制实现 / watch-only / config-gate）+ 运行时证据记录，或升级触发 MR0

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.83-96 done）、`docs/audits/arm-index.md`（维持注记追加）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：P1-RC-032（直接升格缺失）/ P1-RC-033（转化前置弱）/ P1-RC-034（报价单前置弱）维持 P1（运行时确认补偿实现/前置守卫缺失，修复归 MR1 纯 BizModel/Processor 预授权不触 ask-first）；P1-RC-036（territory 降级 MANUAL）维持 P1（修复归 MR1 触 ORM 结构变更[assignmentMethod]须 ask-first + 独立 plan-audit §5）；P1-RC-037（UTM copy）/ P1-RC-038（归因报表）/ P1-RC-039（Forecast territory tier）维持 P1（修复归 MR1 纯 BizModel/报表模板预授权不触 ask-first）；P2-RC-035（funnel-sequence job bean cron-gating 测试）维持 P2（Job bean execute() 测试缺口归 MR1 纯测试补充预授权，登记不强制）；reuse P1-MA2-076/P1-MA2-086 维持 resolved。无新 finding 新建（全部维持）。
- [x] `Add` roadmap A4.2.83-A4.2.96 `todo → done`；`docs/logs/2026/08-07.md` 追加完成条目。

Exit Criteria:

- [x] roadmap 十四项状态已更新为 done 且与报告裁决一致
- [x] arm-index 维持注记已追加（无未经比对直接新建的 finding）

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_0271308f0ffebgyBVVy6T4akei) — one blocking issue: A1.29 marketing finding-ID misattribution vs source report §6. Plan body had shifted IDs by one starting at SP-3 and elevated P2→P1: A4.2.91 (归因报表) was finding-free/misattributed to P1-RC-037 (should be P1-RC-038 P1); A4.2.93 (territory tier) labeled P1-RC-038 (should be P1-RC-039); A4.2.95 (Job bean cron) labeled P1-RC-039 P1 (should be P2-RC-035 P2). Correct mapping per A1.29 §6 lines 347-350: P1-RC-037=UTM copy / P1-RC-038=归因报表 / P1-RC-039=Forecast territory tier / P2-RC-035=Job bean cron-gating test. Structure/template/rule checks all passed; A1.28 items (A4.2.83/84/87/88 → P1-RC-032~036) citation-correct.
- Independent draft review iteration 2: accept (ses_0270dbd66ffe86IB7B0u281CdZ) after citation relabel — blocking issue resolved. All four A1.29 §6 finding-ID↔description↔severity mappings now match source report verbatim across Current Baseline/Goals/Non-Goals/Phase 1 items/Phase 2 Decision/Deferred (8 occurrence points each); no stale references remain (no "P1-RC-038=territory tier", no "P1-RC-039=job bean", P2-RC-035 present + consistently P2). Consensus reached → flipped to active.

## Closure Gates

> 本计划为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更）。closure 时确认 checker 未触发 actual > baseline。

- [x] 范围内行为完成（十四项存疑点均有 file:line 运行时证据 + 明确裁决）
- [x] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [x] 已运行验证（checker actual=baseline 确认；无代码变更故无 build/test 回归风险）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-032~039 / P2-RC-031~035 修复实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认；P1-RC-032（直接升格）/P1-RC-033（转化前置）/P1-RC-034（报价单前置）/P1-RC-037（UTM copy）/P1-RC-038（归因报表）/P1-RC-039（Forecast territory tier）修复归 MR1 纯 BizModel/Processor/报表模板预授权不触 ask-first；P1-RC-036（territory 降级）修复归 MR1 触 ORM 结构变更[assignmentMethod]须 ask-first + 独立 plan-audit（roadmap §横切关注点 #5）；P2-RC-035（funnel-sequence job bean cron-gating 测试）修复归 MR1 纯测试补充预授权（登记不强制）；P2-RC-031~034 修复归 MR1 纯 BizModel 预授权（登记不强制）。本审计维持分级不撤销。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接）

## Closure

Status Note: <completed — 两阶段执行完成，十四项存疑点全数收口（七项维持 P1[A4.2.83 P1-RC-033 / A4.2.84 P1-RC-034 / A4.2.87 P1-RC-036 / A4.2.88 P1-RC-032 / A4.2.90 P1-RC-037 / A4.2.91 P1-RC-038 / A4.2.93 P1-RC-039] + 一项维持 P2[A4.2.95 P2-RC-035] + 六项主路径闭合/watch-only[A4.2.85/86/89/92/94/96]，零新 finding / 不触发 MR0 / 不归 MR1 本审计；CRM 域不直接产生会计凭证故不触及业财保护区域探针；修复义务归 MR1 R1.0 展开器，P1-RC-036 触 ORM 结构变更须 ask-first）>

Closure Audit Evidence:

- Auditor / Agent: 待独立子代理（新会话）执行 closure audit
- Evidence: 验证报告 `docs/audits/2026-08-07-2345-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`（Report Status: done，5 段齐全）+ roadmap A4.2.83-96 done ✅ + arm-index RC 交叉引用注记 + 日志 `docs/logs/2026/08-07.md`

Follow-up:

- 无非阻塞跟进项目（P1 修复义务已明确归 MR1 R1.0 展开器，记录于 Deferred But Adjudicated 节，非本审计 follow-up）
