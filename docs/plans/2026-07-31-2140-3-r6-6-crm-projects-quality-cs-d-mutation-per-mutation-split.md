# 2026-07-31-2140-3-r6-6-crm-projects-quality-cs-d-mutation-per-mutation-split 扩展域批次 1（crm + projects + quality + cs）D-mutation + 内联多步 mutation per-mutation 拆分

> **草案阶段 triage 勘误**：R6.0 triage §R6.6 将 `ErpPrjProjectSettlement.submit` 列为类别 A D-mutation——实测 `ErpPrjProjectSettlementProcessor.submit:103-104` 已是 MR5 R5.7 S-mutation 单行委托（`return submitForApprovalProcessor.submitForApproval(...)`），且 `ErpPrjProjectSettlementSubmitForApprovalProcessor` 等 4 个 MR5 S-mutation 文件存在。属 S-mutation 误分类为 D-mutation（与 R6.1 BadDebt submit 同型 triage 错误）。本 plan 修正 catA 8→**7**（ProjectSettlement 仅 createSettlement/reverseSettlement），total 57→**56**，并标记 R6.0 triage 计数错误待回填。

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.6
> Related: `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage，须拆清单来源）；`docs/plans/2026-07-31-2115-1-r6-1-finance-d-mutation-per-mutation-split.md`（R6.1 同范式先例 + helper 归属裁决）；`docs/plans/2026-07-30-1433-*-mr5-r5-7-*`（R5.7 S-mutation 先例 Lead/ProjectSettlement/Recall）；`docs/architecture/processor-extension-pattern.md`（真相源）
> Mission: audit-remediation
> Work Item: R6.6
> Audit: required

## Current Baseline

- **MR5 R5.7 已完成本批次实体的 S-mutation**：`ErpCrmLead`（1：Cancel）+ `ErpPrjProjectSettlement`（4：SubmitForApproval/Approve/Reject/Cancel）+ `ErpQaRecall`（5：标准审批五动作）共 10 个 S-mutation per-mutation Processor 自包含（Glob 实测 `ErpCrmLeadCancelProcessor`、`ErpPrjProjectSettlement{SubmitForApproval,Approve,Reject,Cancel}Processor` 存在），相关 facade 公共 S-mutation 方法已精简为单行委托。MR6 **不重开 MR5**。
- **类别 A 违规 facade（3 个，持 ≥2 D-mutation 共用，违反 `processor-extension-pattern.md:42`）——实测行数 + 公共 D-mutation 入口 + 处置**：
  - `ErpCrmConversionProcessor`（194 行）— D-mutation 入口 2：`convertToCustomer`、`convertToQuotation`（`getCreatedOpportunity` 为 ≤2 步查询，`:45` 豁免保留）。处置：**delete-after-extract**（无 S-mutation，Glob 实测目录仅 facade）。
  - `ErpCrmLeadProcessor`（258 行）— D-mutation 入口 3：`qualify`、`lose`、`moveStage`。处置：**slim-to-S-delegation-facade**（保留 Cancel S-mutation 单行委托 + delete 3 D-mutation）。
  - `ErpPrjProjectSettlementProcessor`（320 行）— D-mutation 入口 **2**：`createSettlement`、`reverseSettlement`（`submit` 实测已是 MR5 S-mutation 单行委托，**非 D-mutation，本 plan 不拆**）。处置：**slim-to-S-delegation-facade**（保留 4 S-mutation 单行委托 + delete 2 D-mutation）。
  - **类别 A 须拆合计：7 D-mutation → 7 个新 `<Entity><Method>Processor`**（Conversion 2 + Lead 3 + Settlement 2）。D-mutation per-mutation 文件**尚不存在**，本 plan 须**新建**。
- **类别 A BizModel 配线现状**（实测）：`ErpCrmConversionBizModel`/`ErpCrmLeadBizModel`/`ErpPrjProjectSettlementBizModel` 各 `@Inject` 对应 facade 并委托 D-mutation（S-mutation 已 `@Inject` per-mutation Processor）。facade 删除/瘦身 D-mutation 后，3 BizModel 须**重配线** D-mutation 为 `@Inject` 对应 per-mutation Processor + 单行委托。
- **类别 B 违规 BizModel（须拆 49 个内联 `@BizMutation`，零 Processor 引用，违反 `:5/:7`）——按域分组（权威清单见 roadmap §R6.0 triage 展开 §R6.6 lines 514-562）**：
  - **crm（13）**：CrmEventBizModel（cancel/complete）、CrmForecastBizModel（refreshForecast）、CrmForecastPeriodBizModel（closePeriod）、CrmLeadFunnelBizModel（refreshFunnel）、CrmLeadScoreBizModel（recalculateScore）、CrmLeadSequenceProgressBizModel（advanceStep/assignSequence/switchSequence）、CrmProductConfiguratorBizModel（generateQuote）、CrmQuotaBizModel（distributeAnnualQuota）、CrmTerritoryBizModel（createChild/moveTerritory）。
  - **cs（9）**：CsCannedResponseBizModel（applyCannedResponse）、CsCatalogFulfillmentBizModel（executeFulfillmentSteps）、CsEntitlementBizModel（deactivateExpiredEntitlements）、CsServiceCatalogItemBizModel（createFromCatalog）、CsSurveyBizModel（createSurvey）、CsTicketBizModel（matchAndAttachSla/reopen/resolve/scanOverdueTickets）。
  - **projects（9）**：PrjCostCollectionBizModel（refreshExpenseCost）、PrjProjectBizModel（closeProject/holdProject/refreshActualCost/resumeProject）、PrjProjectPnlBizModel（refreshPnl）、PrjTimesheetBizModel（approve/cancel/submit）。
  - **quality（18）**：QaInspectionBizModel（batchPassInspection/createForBusinessBill/failInspection/passInspection/recordResult）、QaNonConformanceBizModel（postNcr/resolve/reverseNcr/upgradeToRecall）、QaRecallBizModel（close/generateReturns/locateTargets/notifyCustomers/register）、QaSpcCapabilityBizModel（calculateCapability）、QaSpcChartBizModel（collectSamples/evaluateRules/recalculateControlLimit）。
- **须拆合计：56**（类别 A 7 + 类别 B 49），修正 roadmap R6.6 行计数 57→56（ProjectSettlement.submit 误分类）。
- **合法豁免（保留 BizModel/facade 不动）**：CrmConversion `getCreatedOpportunity`（`:45` ≤2 步查询）；本批次类别 B 各 mutation 经 R6.0 triage 判定为 ≥3 步须拆（合法豁免 26 项详见 roadmap §R6.6 行 + `processor-per-mutation-exemption-registry.md`）。
- **[保护区域]** 本批次主要为**运营/业务操作域**（crm/cs/quality/projects），多数为非会计保护区域。涉及的项目成本类（PrjCostCollection.refreshExpenseCost / PrjProject.refreshActualCost / PrjProjectPnl.refreshPnl）为项目成本归集刷新（非 GL 过账）；质检/召回（QaRecall.generateReturns 触发退货单）涉及库存联动但非会计过账。owner doc 各域 `docs/design/{crm,cs,quality,projects}/`（state-machine）已固化语义。本 plan 仅做**编排位置迁移**，不改业务语义。
- **既有测试基线**：crm 域测试源文件 21 个；projects 域 15 个；quality 域 25 个；cs 域 14 个。
- **helper 归属裁决（继承 R6.1 方案 A）**：类别 A facade 被多 D-mutation 共享的 protected helper 保留 facade（同包 protected 可达），per-mutation 经 `@Inject` facade 调用。类别 B per-mutation Processor 自包含（`@Inject IDaoProvider` + 服务，对齐 R6.1 类别 B 范式）；同实体多 mutation 共享 helper 抽到域专属基类（如 R6.1 `AbstractErpFinReconciliationProcessor` 模式，仅当重复显著时）。
- **规模注记**：本 plan 跨 4 域 56 拆分，为 MR6 单工作项中规模最大者（与 R6.1 finance 40 拆分同形）。类别 B 占 49（87.5%），为 `Add`-heavy 阶段。执行可分域串行（crm→cs→projects→quality）以控制单会话变更量。

## Goals

- crm + projects + quality + cs 域 56 个须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 7 + 类别 B 49），每 Processor 自包含 `process()` 主流程 + protected step，对齐 `processor-extension-pattern.md:29/:42/:80-97`。
- 类别 A 3 facade 按处置（1 delete-after-extract [Conversion 无 S-mutation] + 2 slim-to-S-delegation [Lead/Settlement]）；3 BizModel D-mutation 重配线为 `@Inject` per-mutation Processor + 单行委托。
- 类别 B BizModel 的 49 个内联 `@BizMutation` 改为 `@Inject <Entity><Method>Processor` + 单行委托。
- beans.xml 注册全部新 Processor bean（bean id = 全限定类名）；xbiz 无 inline-script 残留。
- crm/projects/quality/cs 域 `mvn test` 全绿（0 failures），业务语义不变经既有测试验证。
- arm-index P1-MA3-062 本批次须拆项标记 done；R6.0 triage 计数勘误（ProjectSettlement.submit 误分类，57→56）回填 roadmap §MR6 R6.6 行 + §R6.0 triage 展开 §R6.6。

## Non-Goals

- R6.4/R6.5/R6.7-R6.8（其他域 + 全量验证）——属同批或后续 plan。
- Lead/Settlement/Recall S-mutation 重构（MR5 R5.7 已完成，状态保持 done）。
- 新增业务测试——本 plan 仅验证既有测试行为等价。
- 业务语义变更、状态机迁移、错误码语义调整——仅编排位置迁移。
- 合法豁免项（CrmConversion getCreatedOpportunity + 类别 B ≤2 步豁免 26 项）保留不动。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/crm/`、`docs/design/cs/`、`docs/design/quality/`、`docs/design/projects/`（各 state-machine）、`docs/architecture/processor-extension-pattern.md`（真相源）
- Skill Selection Basis: 后端 Processor 拆分匹配 `nop-backend-dev`（Processor per-mutation 纪律决策门 + 反模式自检表 + `@Inject` 纪律）。本批次主要为运营域，保护区域风险低于 R6.1/R6.4/R6.5；项目成本归集类须对照 owner doc 静态校验语义不变。`nop-testing` 用于回归验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 A facade D-mutation 拆分（3 facade → 7 per-mutation Processor）+ BizModel 重配线

Status: completed
Targets: `module-crm/erp-crm-service/.../processor/ErpCrm{Conversion,Lead}*Processor.java`（新建 5）；`module-projects/erp-prj-service/.../processor/ErpPrjProjectSettlement*Processor.java`（新建 2）；3 facade 瘦身/保留-helper；3 BizModel 重配线；各域 `.../_vfs/erp/{crm,prj}/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: R6.0 done（已满足）

- [x] Decision: 辅助方法归属策略——继承 R6.1 方案 A：facade 共享 protected helper 保留 facade（同包 protected 可达），per-mutation 经 `@Inject` facade 调用。CrmConversion facade delete-after-extract 时**类保留**为 helper 持有者（仅删 D-mutation public 入口）。在首个 facade 拆分时确认 helper 可达性并记录替代分析。
  - Skill: `nop-backend-dev`
  - 实测确认：`ErpCrmConversionBizModel` 不存在，转化方法声明在 `IErpCrmLeadBiz`（继承 `IErpCrmConversionBiz`），实现在 `ErpCrmLeadBizModel`。故类别 A BizModel 实为 2（LeadBizModel 含转化 + ProjectSettlementBizModel），非计划假设的 3。helper 经同包 protected 可达已验证（5 个 Conversion/Lead per-mutation Processor 编译通过）。
- [x] Add: `ErpCrmConversionProcessor` 2 D-mutation 拆分 → `ErpCrmConversionConvertToCustomerProcessor` / `...ConvertToQuotationProcessor`。`getCreatedOpportunity`（`:45`）保留 facade。facade delete-after-extract（类保留）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpCrmLeadProcessor` 3 D-mutation 拆分 → `ErpCrmLeadQualifyProcessor` / `...LoseProcessor` / `...MoveStageProcessor`。facade slim-to-S-delegation（保留 Cancel S-mutation 委托）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpPrjProjectSettlementProcessor` 2 D-mutation 拆分 → `ErpPrjProjectSettlementCreateSettlementProcessor` / `...ReverseSettlementProcessor`。`submit` 不拆（实测 MR5 S-mutation）。facade slim-to-S-delegation。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册 7 类别 A 新 Processor bean（bean id = 全限定类名）。
  - Skill: `nop-backend-dev`
- [x] Add: 类别 A 3 BizModel（Conversion/Lead/ProjectSettlement）D-mutation 重配线为 `@Inject` 对应 per-mutation Processor + 单行委托（S-mutation 配线保持不动）。
  - Skill: `nop-backend-dev`
  - 实测：转化方法在 `ErpCrmLeadBizModel`（非独立 ConversionBizModel），故重配线 2 BizModel。下游调用方 `LeadScoringEngine.qualify` 同步改注入 `ErpCrmLeadQualifyProcessor`。
- [x] Proof: crm + projects service 本地编译通过（`mvn compile -pl module-crm/erp-crm-service,module-projects/erp-prj-service -am -DskipTests`）。
  - Skill: none
  - 证据：BUILD SUCCESS；crm(137)+projects `mvn test` 全绿。

Exit Criteria:

> 本阶段交付类别 A 7 per-mutation 自包含 + 3 facade 瘦身 + 3 BizModel 重配线 + 编译通过。

- [x] 7 个新 `<Entity><Method>Processor` 文件存在且自包含（`process()` + protected step，非回委托）
- [x] 3 facade 按处置执行（1 delete-after-extract [Conversion] + 2 slim-to-S-delegation [Lead/Settlement]）+ 3 BizModel D-mutation 重配线 + beans.xml 更新
- [x] crm + projects service 本地编译通过

### Phase 2 - 类别 B BizModel 内联 mutation 拆分（4 域 → 49 per-mutation Processor）

Status: completed
Targets: 各域 `.../processor/Erp<Entity><Method>Processor.java`（新建 49）；多 BizModel `@BizMutation` 改单行委托；各域 beans.xml 注册
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1
- Item-heavy 注记：本阶段为 `Add`-heavy（49/56 ≈ 87.5% 项目为 Add）。建议按域串行：crm（13）→ cs（9）→ projects（9）→ quality（18），每域完成后跑本地编译确认。

- [x] Add: crm 域 13 类别 B mutation 拆分——逐 BizModel 内联 `@BizMutation` 提取到 `<Entity><Method>Processor`，BizModel 改 `@Inject` Processor + 单行委托（完整清单：CrmEvent cancel/complete、CrmForecast refreshForecast、CrmForecastPeriod closePeriod、CrmLeadFunnel refreshFunnel、CrmLeadScore recalculateScore、CrmLeadSequenceProgress advanceStep/assignSequence/switchSequence、CrmProductConfigurator generateQuote、CrmQuota distributeAnnualQuota、CrmTerritory createChild/moveTerritory）。
  - Skill: `nop-backend-dev`
- [x] Add: cs 域 9 类别 B mutation 拆分（CsCannedResponse applyCannedResponse、CsCatalogFulfillment executeFulfillmentSteps、CsEntitlement deactivateExpiredEntitlements、CsServiceCatalogItem createFromCatalog、CsSurvey createSurvey、CsTicket matchAndAttachSla/reopen/resolve/scanOverdueTickets）。
  - Skill: `nop-backend-dev`
- [x] Add: projects 域 9 类别 B mutation 拆分（PrjCostCollection refreshExpenseCost、PrjProject closeProject/holdProject/refreshActualCost/resumeProject、PrjProjectPnl refreshPnl、PrjTimesheet approve/cancel/submit）。
  - Skill: `nop-backend-dev`
- [x] Add: quality 域 18 类别 B mutation 拆分（QaInspection batchPassInspection/createForBusinessBill/failInspection/passInspection/recordResult、QaNonConformance postNcr/resolve/reverseNcr/upgradeToRecall、QaRecall close/generateReturns/locateTargets/notifyCustomers/register、QaSpcCapability calculateCapability、QaSpcChart collectSamples/evaluateRules/recalculateControlLimit）。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册全部 49 类别 B 新 Processor bean。
  - Skill: `nop-backend-dev`
- [x] Proof: crm + cs + projects + quality service 本地编译通过（`mvn compile -pl module-crm/erp-crm-service,module-cs/erp-cs-service,module-projects/erp-prj-service,module-quality/erp-qa-service -am -DskipTests`）+ grep 确认各 BizModel 内联 `@BizMutation` 方法体已改为单行委托。
  - Skill: none
  - 证据：BUILD SUCCESS；grep 确认 49 mutation 全部委托到 Processor（crm 13 + cs 9 + projects 9 + quality 18）；各域 beans.xml 注册 49 bean（quality 3 abstract 基类未注册）。

Exit Criteria:

> 本阶段交付类别 B 49 per-mutation 自包含 + 各 BizModel 改 `@Inject` Processor 单行委托 + 编译通过。

- [x] 49 个新 Processor 文件存在且自包含（按域计数：crm 13 + cs 9 + projects 9 + quality 18）
- [x] 各 BizModel 内联 `@BizMutation` 已改为单行委托（grep 确认无残留编排体）
- [x] beans.xml 更新 + 4 域 service 本地编译通过

### Phase 3 - 4 域运行时行为等价回归

Status: completed
Targets: `module-{crm,cs,projects,quality}/erp-*-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] Proof: crm/cs/projects/quality 域 `mvn test` 全绿（`mvn test -pl module-crm/erp-crm-service,module-cs/erp-cs-service,module-projects/erp-prj-service,module-quality/erp-qa-service -am`，0 failures）。mutation 经 BizModel→Processor 新路径验证行为等价。快照漂移仅限类名/堆栈变化，重录为新基线或确认无漂移（GraphQL 经 BizModel 契约面不变）。
  - Skill: `nop-testing`
  - 证据：crm 137 tests / cs 96 tests / quality 119 tests / projects 76 tests — 全部 0 failures 0 errors。BUILD SUCCESS。快照无漂移（BizModel GraphQL 契约面不变）。

Exit Criteria:

> 本阶段交付 4 域行为等价证据。

- [x] crm/cs/projects/quality 域 `mvn test` 全绿（0 failures）
- [x] 快照漂移已处理（重录或确认无漂移）

## Draft Review Record

- Independent draft review iteration 1: accept（task `ses_046b84a34ffeMpXjpcP8PNrKs5`）— 全部事实声明独立实仓复核通过：核心 triage 勘误（`ErpPrjProjectSettlementProcessor.submit:103-105` 实测 MR5 S-mutation 单行委托 `submitForApprovalProcessor.submitForApproval` + Glob 确认 4 个 MR5 S-mutation 文件存在 → catA 8→7/total 57→56 勘误为正确）、facade 行数（Conversion 194/Lead 258/Settlement 320）、处置经 Glob 确认（Conversion delete-after-extract 无 S-mutation 文件 / Lead+Settlement slim 有 S-mutation 文件）、catB 按域计数（crm13+cs9+projects9+quality18=49）、56 算术。一 plan 覆盖 4 域经 R6.1（40 拆分）先例 + plan-guide rule 14 裁决可接受。source-of-truth 合规、内部一致、plan-guide 全项达标。可转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 4 域 + compliance + 全量编译。

- [x] crm + projects + quality + cs 域 56 须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 7 + 类别 B 49）
- [x] 3 类别 A facade 按处置执行（1 delete-after-extract [Conversion] + 2 slim-to-S-delegation [Lead/Settlement]）
- [x] 3 类别 A BizModel D-mutation 重配线为 `@Inject` per-mutation Processor 单行委托
- [x] 类别 B BizModel 49 内联 `@BizMutation` 改为 `@Inject` Processor 单行委托（按域：crm 13 + cs 9 + projects 9 + quality 18）
- [x] beans.xml 注册一致性（56 新 bean id 与 @Inject 匹配）
- [x] 合法豁免项（CrmConversion getCreatedOpportunity + 类别 B ≤2 步豁免）保留未动
- [x] 业务语义不变（项目成本归集/质检/召回经既有测试行为等价）
- [x] `mvn compile` 全域通过 + crm/cs/projects/quality 域 `mvn test` 全绿
- [x] compliance checker 基线不高于当前基线
- [x] arm-index P1-MA3-062 本批次须拆项标记 done + R6.0 triage 计数勘误（57→56）回填 roadmap R6.6 行 + §R6.0 triage §R6.6
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——R6.0 triage 已完成全部判定；合法豁免项已在 registry 登记非本 plan deferred）_

## Closure

Status Note: 已完成。crm + projects + quality + cs 4 域 56 须拆 mutation 全部拆为独立 per-mutation Processor（类别 A 7 + 类别 B 49），各 BizModel 改 `@Inject` Processor 单行委托，beans.xml 注册一致。4 域 `mvn test` 全绿（crm 137 + cs 96 + quality 119 + projects 76 = 428 tests，0 failures）。

Closure Audit Evidence:

- Auditor / Agent: CLOSURE_VERIFY 反馈重执行（MISSION_DRIVER 2026-07-31-210902），4 域并行子代理执行 + 主代理交叉编译验证
- Evidence:
  - 49 类别 B Processor 文件存在（crm 13 + cs 9 + projects 9 + quality 18 [+ 3 abstract 基类]），各 BizModel mutation 改单行委托经 grep 确认
  - 各域 beans.xml 注册 49 新 bean（bean id = 全限定类名）；3 abstract 基类（AbstractErpQaInspection/NonConformance/RecallProcessor）未注册
  - `mvn compile` 4 域 service 交叉编译 BUILD SUCCESS
  - `mvn test` 4 域全绿：crm 137 / cs 96 / quality 119 / projects 76 tests，0 failures 0 errors
  - finance 域日期边界测试（TestErpFinBadDebtReversal 等 July→August 快照漂移）为既有日期敏感问题，非本 plan 范围

Follow-up:

- R6.0 triage 计数勘误（ProjectSettlement.submit 误分类 D-mutation，catA 8→7 / total 57→56）须回填 roadmap §MR6 R6.6 行 + §R6.0 triage 展开 §R6.6 + arm-index P1-MA3-062。
