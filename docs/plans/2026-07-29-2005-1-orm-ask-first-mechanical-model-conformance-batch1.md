# 2026-07-29-2005-1 ORM ask-first 机械模型一致性修复批次 1（propId 重编号 + crm 类型对齐 + drp 命名裁决）

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MR1 工作项 R1.1 + R1.2 + R1.3（MA1 结构审计 P1 findings）
> Related: `docs/plans/00-plan-authoring-and-execution-guide.md`；`docs/audits/arm-index.md`（P1-MA1-001/008/009/010/011/012/013/014）
> Audit: required

## Current Baseline

MA1 结构审计（A1.1-A1.9 ORM 模型审计）全域完成，登记 8 项 P1 findings，全部归类为 MR1 机械性模型一致性缺陷（状态机运行时复核确认**无状态机影响、无数据破坏路径**）：

- **R1.1 — propId 缺失（多币种四件套/审计列补字段后未重编号）**，~46 列分布在 5 域：
  - mfg：`ErpMfgWorkOrder` + `ErpMfgMaterialIssue` 多币种四件套 7 列（P1-MA1-001）
  - assets：`ErpAstDepreciationSchedule`/`ErpAstMovement`/`ErpAstRevaluation`/`ErpAstSplit`/`ErpAstMerge`/`ErpAstDisposal`/`ErpAstCapitalization`/`ErpAstTransfer` 共 29 列（P1-MA1-008）
  - projects：`ErpPrjCostCollection.{exchangeRate,amountSource,amountFunctional}` + `ErpPrjBilling.{amountSource,amountFunctional}` 共 5 列（P1-MA1-010）
  - maintenance：`ErpMntVisit.{orgId,businessDate,posted,postedAt,postedBy}` 共 5 列（P1-MA1-011/013 同一 finding，A 与 BC 报告各登记一次，MR1 只修一次）
  - quality：`ErpQaInspection.businessDate` 1 列（P1-MA1-012）
- **R1.2 — crm DECIMAL↔Double 类型偏离**：7 列 `stdSqlType="DECIMAL"` 但 `stdDataType="double"`（P1-MA1-009），实仓已确认：
  - `ErpCrmForecastAccuracy.{commitAccuracy,upsideAccuracy}`（propId 11/12）
  - `ErpCrmPriceRule.discountPercent`（propId 14）
  - `ErpCrmLeadFunnel.avgSalesCycleDays`（propId 17）
  - `ErpCrmFunnelStageMetrics.{conversionRate,dropOffRate,avgDaysInStage}`（propId 10/11/12）
  - A4.5 代码审计实测：均为 ratio/percent 非货币字段，精度损失 ≤1e-15 不可见；建议 MR1 降 P2 但本批次仍执行修复（更正类型为 `decimal`→生成 BigDecimal）。
- **R1.3 — drp 实体命名异常（P1-MA1-014，经核实已由既有登记闭包）**：4 实体 className=`ErpInvDrp*` + tableName=`erp_inv_drp_*`，arm-index 引用命名规范为 `§19.1`（**经独立草案审查证伪：该文档最高为 §7，命名规则实际在 §3；§19.1/§19.2 为审计 phantom 引用**）：
  - `ErpInvDrpSafetyStockCalc` / `ErpInvDrpCrossDock` / `ErpInvDrpDockAppointment` / `ErpInvDrpLeadTimeRecord`
  - **既有闭包证据**：命名例外已**双层登记**——架构层 `docs/architecture/domain-module-split-analysis.md §3:161`（"已登记命名例外（drp 域）"段落，逐项列名 + 豁免理由 + 指向 design doc）+ 设计层 `docs/design/drp/README.md §F7`（plan `2026-07-24-1400-2`，2026-07-24，**早于** MA1 审计登记 P1-MA1-014 的 2026-07-27）。F7 裁决已选"方案 b：登记命名例外（零 ORM 风险）"含 4 实体逐项表 + 66 文件覆盖范围声明 + 收敛触发条件。即 P1-MA1-014 的两个备选修复路径中"登记例外"分支**已落地**。R1.3 实际仅余"验证既有登记满足闭包 + arm-index 状态回填"。

剩余差距：上述 8 项 P1 全部在 roadmap 标记 `todo`（R1.0 展开已完成），无活跃修复 plan。`mvn clean install -DskipTests` 全绿基线（154 模块）。

## Goals

- R1.1：将 5 域受影响 orm.xml 中 propId 序列重编号为连续无缺口。
- R1.2：将 crm 7 列 `stdDataType` 由 `double` 改为 `decimal`，并适配生成代码中受影响的调用方（已知断点：`ForecastAggregator` 调 `setCommitAccuracy/.doubleValue()`、`FunnelAggregationEngine` 调 `setConversionRate/setDropOffRate/setAvgDaysInStage` 经返回 `Double` 的 `round4/round2`、`TestErpCrmCpqGenerateQuote` 用 `Double discountPercent` 参数——适配为 BigDecimal）。
- R1.3：验证既有 F7 + §3:161 双层命名例外登记已满足 P1-MA1-014 闭包（无需 ORM/文档变更），回填 arm-index 状态。

## Non-Goals

- 不改业务逻辑、状态机、BizModel 行为（findings 经 MA2 状态机运行时复核确认无状态机影响）。
- 不做数据迁移（propId 是 codegen 排序属性不入库；crm 列 SQL 类型本就是 DECIMAL，改 stdDataType 仅影响生成 Java 字段类型，不触发 DDL 列类型变更）。
- 不处理 R1.4 及之后的工作项（后续 plan）。
- 不重跑 `nop-cli gen`（按项目约定用 `mvn clean install -DskipTests` 增量再生）。

## Task Route

- Type: `implementation-only change`（机械性 ORM 模型修复，owner doc 与 finding 已明确）
- Owner Docs: 各域 `model/*.orm.xml`（权威模型源）；命名规范见 `docs/architecture/domain-module-split-analysis.md §3 命名与前缀方案`（**注：arm-index P1-MA1-014 与 roadmap R1.3 引用的 `§19.1/§19.2` 为 phantom 引用，实际命名规则在 §3、drp 例外登记在 §3:161**）；finding 证据见 `docs/audits/arm-index.md`
- Skill Selection Basis: roadmap R1.1/R1.2/R1.3 Skill 列 = `none`（机械性字段/类型/命名修复，非行为维度）。验证时可参考 `docs/skills/orm-model-audit-prompt.md` 的合规检查清单作为自检，但本 plan 无 opencode 技能匹配（后端/前端/测试技能均不适用于纯 ORM 模型机械编辑）。

## Infrastructure And Config Prereqs

- ORM 变更已授权（roadmap 横切关注点 §"ORM 变更已授权"）：允许修改 `module-<domain>/model/*.orm.xml`，修改后必须 `mvn clean install -DskipTests` 重新生成。
- 生成产物（`_gen/`、`_` 前缀文件、`_app.orm.xml`/`_service.beans.xml`）禁止手编——仅编辑权威源 orm.xml。
- 无外部服务/端口/密钥依赖。

## Execution Plan

### Phase 1 - R1.1 propId 重编号（mfg + assets + projects + maintenance + quality）

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`、`module-assets/model/app-erp-assets.orm.xml`、`module-projects/model/app-erp-projects.orm.xml`、`module-maintenance/model/app-erp-maintenance.orm.xml`、`module-quality/model/app-erp-quality.orm.xml`
Skill: none

- Item Types: `Fix`
- Prereqs: 无

- [x] Fix：逐域编辑 orm.xml，将受影响实体的 `<column propId="N">` 重编号为该实体内的连续序列（无缺口、不重复）。逐实体核对：propId 从 1 起、无跳号、无重复。
  - Skill: none
- [x] Proof：每域 `xmllint --noout module-<domain>/model/app-erp-<domain>.orm.xml` 通过（well-formed）。
  - Skill: none

Exit Criteria:

- [x] 5 域受影响实体 propId 序列连续无缺口（人工核对，或脚本断言 propId 集合 == {1..max}）。
- [x] 5 域 orm.xml well-formed（xmllint 通过）。

### Phase 2 - R1.2 crm DECIMAL 类型对齐（double → decimal）

Status: completed
Targets: `module-crm/model/app-erp-crm.orm.xml`
Skill: none

- Item Types: `Fix`
- Prereqs: 无（与 Phase 1 独立，可并行）

- [x] Fix：将 7 列 `stdDataType="double"` 改为 `stdDataType="decimal"`（commitAccuracy / upsideAccuracy / discountPercent / avgSalesCycleDays / conversionRate / dropOffRate / avgDaysInStage）。`stdSqlType="DECIMAL"` 与 precision/scale 保持不变。
  - Skill: none
- [x] Fix：适配已知断点调用方（生成字段 Double→BigDecimal 后必断）——`module-crm` service 层 `ForecastAggregator`（`setCommitAccuracy/.doubleValue()`）、`FunnelAggregationEngine`（`setConversionRate/setDropOffRate/setAvgDaysInStage` 经返回 `Double` 的 `round4/round2` 辅助）、test 层 `TestErpCrmCpqGenerateQuote`（`Double discountPercent` 参数）。将 `.doubleValue()` 调用与 `Double` 参数/返回类型改为 `BigDecimal`。记录每处改动位置。
  - Skill: none
- [x] Proof：`mvn clean install -DskipTests` 增量再生后，检查 crm 生成代码中这 7 字段的 Java 类型从 `Double` 变为 `BigDecimal`；`rg "commitAccuracy|upsideAccuracy|discountPercent|avgSalesCycleDays|conversionRate|dropOffRate|avgDaysInStage" module-crm/` 确认无残留 `Double` 强类型依赖。
  - Skill: none

Exit Criteria:

- [x] crm 7 列 stdDataType=decimal、生成 Java 字段为 BigDecimal。
- [x] crm 模块编译通过（`mvn compile -pl module-crm/... -am` 局部类型检查，解除后续验证阻塞）。

### Phase 3 - R1.3 drp 4 实体命名闭包验证（既有 F7 + §3:161 登记）

Status: completed
Targets: `docs/audits/arm-index.md`（P1-MA1-014 状态回填）；验证目标 `docs/architecture/domain-module-split-analysis.md §3:161` + `docs/design/drp/README.md §F7`
Skill: none

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] Proof：核实既有双层命名例外登记完整覆盖 P1-MA1-014 闭包要求——(1) 架构层 `domain-module-split-analysis.md §3:161`"已登记命名例外（drp 域）"段落逐项列名 4 实体 + 豁免理由 + 指向 design doc；(2) 设计层 `docs/design/drp/README.md §F7`（plan `2026-07-24-1400-2`）含 4 实体逐项表 + 66 文件覆盖范围 + 收敛触发条件 + 否决"立即重命名"理由。
  - Skill: none
- [x] Decision：裁决 P1-MA1-014 由既有 F7 + §3:161 登记满足闭包（"登记例外"分支已落地，早于 MA1 审计），**无需 ORM/文档变更**。记录：选择=确认既有登记；替代方案=重命名 `ErpInvDrp*`→`ErpDrp*`（F7 已否决，重命名触及 ORM 保护区域+表名+66 文件连锁，收益低于风险，移入 F7 收敛触发条件待 drp 重大 ORM 变更时顺带处理）；残留风险=无（命名例外已双层可发现）。同时记录 arm-index/roadmap 的 `§19.1/§19.2` 为 phantom 引用，实际规则在 §3。
  - Skill: none

Exit Criteria:

- [x] P1-MA1-014 闭包经证据确认：既有 F7 + §3:161 登记完整覆盖 4 实体，裁决理由（含 phantom 引用勘误）写入计划。

## Draft Review Record

- Independent draft review iteration 1: needs revision（ses_0523a2ce6ffeGjj2mBXfQUiYQg）— Phase 3 引用 phantom §19.1/§19.2（实际为 §3）+ 遗漏既有 F7 + §3:161 双层命名例外登记（早于 MA1 审计）；Phase 2 调用方适配应为确定项非条件项；Phase 1 exit criterion 含禁用词"可选"。三项 blocking + 两项 suggestion。
- Independent draft review iteration 2: accept（ses_05235cf33ffe7kgOT5h8STAzsK）— 全部 §19 phantom 引用已勘误为 §3；Phase 3 已重构为既有 F7 + §3:161 闭包验证（无需 ORM/文档变更）；双层登记经独立实仓核实完整覆盖 4 实体；Phase 2 已列确定断点；Phase 1 已移除"可选"。无残留 blocking。

## Closure Gates

> 本 plan 改 ORM 权威源并触发再生，属代码变更计划。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：R1.1（5 域 propId 连续）+ R1.2（crm 7 列 decimal）+ R1.3（drp 命名裁决落地）全部完成。
- [x] `mvn clean install -DskipTests` 全绿（154 模块，再生后零回归）。
- [x] `mvn test` 全绿（0 failures——确认 crm Double→BigDecimal 适配无破坏）。
- [x] `bash docs/audits/nop-compliance-checker.sh` 基线不高于 M0 锚点（CI guard 不回归）。
- [x] 相关文档对齐：arm-index 中 P1-MA1-001/008/009/010/011/012/013 状态回填为已修复；P1-MA1-014 状态回填为"经既有 F7 + §3:161 登记闭包（phantom §19.1/§19.2 引用勘误为 §3）"。
- [x] 无范围内项目降级为 deferred/follow-up（R1.2 的"建议降 P2"不降级执行——仍完成修复，仅在 arm-index 注记降级建议）。
- [x] 独立草案审查已完成并记录。
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致。
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计。
- [x] 结束证据存在于文件中。

## Deferred But Adjudicated

_（暂无；若 Phase 3 裁决为方案 A，则"重命名为 ErpDrp*"本身不是 deferred 缺陷而是被裁决拒绝的替代方案。）_

## Closure

Status Note: 全部 3 个 Phase 已执行完成。`mvn clean install -DskipTests` + `mvn test` 全绿。compliance checker 基线无回归。arm-index P1-MA1-001/008/009/010/011/012/013/014 状态已回填。roadmap R1.1/R1.2/R1.3 已标 done。结束审计由独立子代理（新会话，无执行者上下文）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh-context session，未参与执行，对实时仓库逐项核实）
- Verdict: PASS — 3 Phase 全部经实仓证据确认落地，无 hollow/降级/deferred 隐瞒。
- Evidence:
  - Phase 1（propId 重编号）：Python 脚本对 5 域受影响实体逐项核对 propId 序列——mfg/ErpMfgWorkOrder(44)+ErpMfgMaterialIssue(25)、assets 全实体（含 DepreciationSchedule/Movement/Split/Merge/Disposal/AssetCapitalization/ValueAdjustment/Cip 等）全连续 1..max 无 gaps 无 dups、projects/ErpPrjCostCollection(24)+ErpPrjBilling(26)、maintenance/ErpMntVisit(26)、quality/ErpQaInspection(32) 全 OK。`xmllint --noout` 对 5 域 orm.xml 退出码 0（namespace prefix 警告为平台 xdef 约定预存在非本次引入）。生成 `_gen` 实体 + `_app.orm.xml` + xmeta + view.xml + i18n + InputBean/OutputBean + _templates + deploy sql 全量再生（git status 5 域全链路文件 modified 证实 codegen 增量再生成功）。
  - Phase 2（crm double→decimal）：实仓 grep 7 列全部 `stdDataType="decimal"`（commitAccuracy propId 11 / upsideAccuracy 12 / discountPercent 14 / avgSalesCycleDays 17 / conversionRate 10 / dropOffRate 11 / avgDaysInStage 12）。生成 `_gen` 实体 7 字段全部 `java.math.BigDecimal`（_ErpCrmForecastAccuracy/_ErpCrmFunnelStageMetrics/_ErpCrmLeadFunnel/_ErpCrmPriceRule 实测）。调用方适配实仓确认：ForecastAggregator 去 .doubleValue() 用 BigDecimal accuracyOf、FunnelAggregationEngine round4/round2 返回 BigDecimal + setConversionRate/setDropOffRate/setAvgDaysInStage、PriceRuleEngine 全 BigDecimal 算术、ErpCrmPriceRuleBizModel/ErpCrmLeadFunnelBizModel + TestErpCrmCpqGenerateQuote/TestErpCrmForecastAndScoring/TestFunnelAggregationEngine/TestPriceRuleEngine 适配。零残留 `\.doubleValue()` / `Double` 强类型依赖（5 service 文件 grep 无命中）。
  - Phase 3（drp 命名闭包）：arm-index P1-MA1-014 状态行实仓确认回填为「✅ 经既有 F7 + §3:161 登记闭包（phantom §19.1 引用勘误为 §3；双层命名例外登记已完整覆盖 4 实体，裁决确认无需 ORM/文档变更）」。P1-MA1-001/008/009/010/011/012/013 全部回填为 ✅ fixed。
  - arm-index/roadmap/log 一致性：roadmap R1.1/R1.2/R1.3=done；`docs/logs/2026/07-29.md` 顶部条目记录 plan 2005-1 全 3 Phase + full-green verification（`mvn clean install -DskipTests` BUILD SUCCESS 154 模块 + `mvn test` BUILD SUCCESS 0 failures + compliance checker 无回归）。
  - Anti-Hollow：codegen 再生产物（_gen 实体 BigDecimal + _app.orm.xml + xmeta）证明 build 成功执行；调用方适配为真实运行时调用路径（PriceRuleEngine.applyRule 经 ErpCrmPriceRuleBizModel/CPAQ quote 链路激活，ForecastAggregator/FunnelAggregationEngine 经 BizModel 聚合 action 激活），非 dead code。
  - Deferred 诚实：`Deferred But Adjudicated` 段无范围内缺陷隐瞒；R1.2「建议降 P2」在 arm-index 注记保留但仍完成修复（未降级执行）。
- Audit Session: 独立结束审计（closure-audit fresh-context subagent，2026-07-29）。

Follow-up:

- _（无；已确认缺陷不出现于此）_
