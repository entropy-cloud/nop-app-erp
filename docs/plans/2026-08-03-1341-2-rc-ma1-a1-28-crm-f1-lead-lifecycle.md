# 2026-08-03-1341-2 rc-ma1-a1-28-crm-f1-lead-lifecycle crm-F1 线索生命周期需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.28（MA1 需求追踪矩阵审计 — crm-F1 线索生命周期）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.28
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.28 的 0.2 依赖）、`2026-08-03-1341-1-rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard.md`（同批次 N=1，先于本计划）、`2026-08-03-1341-3-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`（同批次 N=3，crm-F2，线索生命周期为营销/预测/配额的依赖基础）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.28 给出 UC 清单 = `UC-CRM-01/02/03/04/09/11`（6 UC），含 `use-cases.md:15/:33/:54/:74/:175/:239` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。CRM 为扩展域，本切片是 CRM 域**首个 RC 切片**（既有 arm-index 无 RC finding 涉及 crm）。

- **L1 需求契约（权威真相源）**：`docs/design/crm/use-cases.md`（机制见 `crm/README.md` §核心业务对象/§状态机/§衔接契约、`crm/state-machine.md`、`crm/lead-scoring.md`、`crm/territory.md`）：
  - UC-CRM-01 线索创建与验证（`:15`）：Lead 创建 leadType=LEAD/docStatus=NEW/contactName/companyName 必填；跟进验证 NEW→QUALIFIED；stageId 取 ErpCrmStage.sequence 第一个阶段；probability 取 stage.defaultProbability（用户可覆盖）；**lastContactDate 自动更新为当前时间**。
  - UC-CRM-02 线索→商机转化（`:33`）：Lead.docStatus==QUALIFIED 且 leadType==LEAD → 执行转化：if convertToCustomer → 创建 ErpMdPartner + 创建 ErpCrmLead(leadType=OPPORTUNITY,partnerId) + 原 lead.docStatus→CONVERTED；**if 不创建客户 → lead.leadType→OPPORTUNITY（直接升格）**；LEAD 类型不可直接转报价单（系统拦截）。
  - UC-CRM-03 商机→报价单转化（`:54`）：Lead.leadType==OPPORTUNITY 且 docStatus==QUALIFIED **且 stage.isWonStage==true** → convertToQuotation 调 IErpSalQuotationBiz 创建 ErpSalQuotation + 回写 lead.relatedBillType=SALES_QUOTATION + lead.docStatus→CONVERTED；ErpSalQuotation 无 CRM 外键（核心零污染）。
  - UC-CRM-04 丢单原因记录（`:74`）：Lead.docStatus 为 NEW 或 QUALIFIED → 标记丢失：lostReasonId 必填（不允许空）→ lead.docStatus→LOST；lostReasonId 为空→拒绝迁移返回校验错误。
  - UC-CRM-09 线索自动评分（`:175`）：管理员建 ErpCrmLeadScoreConfig(isActive=true)+configLines；评分触发（**MANUAL / LEAD_UPDATE / SCHEDULED**）→ 加载生效规则逐条评分（LOOKUP/FORMULA/BOOLEAN）→ 归一化 totalScore(0-100) → 建 ErpCrmLeadScore+Line → 回写 lead.score；if totalScore>=autoQualifyThreshold 且 LEAD 且 NEW → 自动 docStatus→QUALIFIED；if autoQualifyThreshold>totalScore>=minScoreForFollowUp → **NOTIFY_OWNER（通知销售优先跟进）**；评分历史只追加。
  - UC-CRM-11 线索区域自动分配（`:239`）：管理员建 ErpCrmTerritory 树（REGION→AREA→BRANCH→TEAM）+ ErpCrmTerritoryAssignmentRule(conditionType=GEOGRAPHY/INDUSTRY/CUSTOMER_SIZE/CUSTOM_FIELD, assignmentMethod=**ROUND_ROBIN/LOAD_BALANCED/MANUAL**)；线索创建→按优先级遍历 isActive 规则匹配字段（province/industry/companySize/sourceId）→ 首个匹配→按 assignmentMethod 分配（**ROUND_ROBIN 轮流 / LOAD_BALANCED 最少线索 / MANUAL 待分配**）→ 回写 territoryId/teamId/ownerId；无匹配→isDefault 规则；仍无→留空"未分配"。

- **L3 代码实现现状（实测）**——CRM 域非全 stub：核心路径（UC-01/02 convertToCustomer/03 零污染/04/09 评分核心/11 territory 引擎）已实现且测试强，但多个 L1 验收标准/分支/前置条件静默缺失：
  - **UC-CRM-01 线索创建与验证**（✅ 大体实现，⚠️ lastContactDate 缺）：`ErpCrmLeadBizModel.java:44`（CrudBizModel Facade）；`defaultPrepareSave:189-217`（duplicateChecker.checkAndNotify + config-gated assignmentEngine.assign）；qualify NEW→QUALIFIED `ErpCrmLeadBizModel.qualify:82-84`→`ErpCrmLeadQualifyProcessor`→`ErpCrmLeadProcessor.doQualify:121-131`（设 QUALIFIED + 首 stage + default probability）；duplicate `LeadDuplicateChecker.java:42-115`（companyName/email/phone 对非终态 lead，默认非阻塞）。**缺失**：`doQualify:121-131` **不设 lastContactDate**（grep `setLastContactDate` in ErpCrmLeadProcessor/QualifyProcessor = 0；lastContactDate writer 仅在 `LeadActivityDerivationHelper` UC-05 事件驱动路径）——L1 `:27` "lastContactDate 自动更新为当前时间"未满足。contactName/companyName 必填在 XMeta 层（须审计期复核 `_ErpCrmLead.xmeta` mandatory）。
  - **UC-CRM-02 线索→商机转化**（⚠️ PARTIAL——直接升格分支缺失 + 前置条件弱）：convertToCustomer `ErpCrmLeadBizModel.convertToCustomer:169-171`→`ErpCrmConversionConvertToCustomerProcessor:19-28`；`ErpCrmConversionProcessor.createPartnerFromLead:62-73`（IErpMdPartnerBiz.save）+ `createOpportunityFromLead:88-104`（新 ErpCrmLead leadType=OPPORTUNITY）+ `markLeadConverted:106-112`（原 lead CONVERTED + relatedBillType=CRM_LEAD 弱指针）；LEAD 直转报价拦截 `ErpCrmConversionConvertToQuotationProcessor:24 validateLeadType(OPPORTUNITY)`。**缺失 #1（L1 `:44-46`）**：grep `直接升格|directPromote|promoteToOpportunity|setLeadType.*OPPORTUNITY` 业务命中=0——**"不创建客户→lead.leadType→OPPORTUNITY 直接升格"分支未实现**（仅 convertToCustomer 路径）。**缺失 #2（前置条件弱）**：`ErpCrmConversionConvertToCustomerProcessor:19-28` 仅 validateNotConverted + validateLeadType=LEAD，**不检查 docStatus==QUALIFIED**——NEW 状态 LEAD 可被转化（L1 要求 QUALIFIED 才可转化）；无负向测试拒绝 NEW 转化。
  - **UC-CRM-03 商机→报价单转化**（⚠️ PARTIAL——前置条件弱）：convertToQuotation `ErpCrmLeadBizModel.convertToQuotation:175-180`→`ErpCrmConversionConvertToQuotationProcessor:21-30`；跨域 `createQuotationFromOpportunity:75-84`（IErpSalQuotationBiz.save）；弱指针 markLeadConverted（relatedBillType=SALES_QUOTATION+code）；零污染经 `TestErpCrmLeadConversion.testZeroPollutionAssertion:225-233` 强断言（反射验证 ErpSalQuotation/ErpMdPartner 无 getOpportunityId/getLeadId）。**缺失 #3（前置条件弱，L1 `:60-61`）**：`ErpCrmConversionConvertToQuotationProcessor:21-30` 仅 validateNotConverted + validateLeadType=OPPORTUNITY + requireOpportunityPartner，**不检查 docStatus==QUALIFIED 且不检查 stage.isWonStage==true**——任意 docStatus 的 OPPORTUNITY 可转报价单；conversion processor 从不读 isWonStage（won-stage 前置静默丢弃）。
  - **UC-CRM-04 丢单原因记录**（✅ 已实现 & 强测）：`ErpCrmLeadBizModel.lose:88-95`→`ErpCrmLeadLoseProcessor`；`ErpCrmLeadProcessor.validateTransitionForLose:58-64`（仅 NEW/QUALIFIED）+ `requireLostReason:112-117`（null 抛 ERR_LOST_REASON_REQUIRED）+ `doLose:133-140`；负向测试 `TestErpCrmLeadConversion.testLoseWithoutReasonRejected:133-147` 强断言。
  - **UC-CRM-09 线索自动评分**（⚠️ PARTIAL——SCHEDULED 触发器缺失 + L2 失实 + NOTIFY_OWNER 无派发）：`LeadScoringEngine.java:48-417`（config 驱动 LOOKUP/FORMULA/BOOLEAN + 归一化 `:125-145` + 只追加历史 + auto-qualify 阈值触发 `:151-167`）；`ErpCrmLeadScoreBizModel.recalculateScore:33-37`（@BizMutation，MANUAL 触发）；LEAD_UPDATE 触发 `ErpCrmLeadBizModel.defaultPrepareUpdate:219-233`（config-gated CONFIG_LEAD_SCORING_RECALC_ON_LEAD_UPDATE 默认 true）；active config 唯一 `loadActiveConfig:300-310`（>1 抛 ERR_MULTIPLE_ACTIVE_SCORE_CONFIG）；auto-qualify→qualify `:83-85`。**缺失 #4（L1 `:184` SCHEDULED 触发器完全缺失）**：glob `module-crm/**/job/*.java` 仅 4 jobs（ForecastRecalc/SequenceOverdue/EventReminder/FunnelAggregation）——**无 ErpCrmLeadScoringRecalcJob**；无 `scheduler.yaml` under module-crm；**L2 lead-scoring.md:157 失实声称**"SCHEDULED: ErpCrmLeadScoringRecalcJob + scheduler.yaml 已接线"（owner doc 向实现妥协/虚报，对齐 §4 根因）。**缺失 #5（L1 `:199` NOTIFY_OWNER 无派发）**：`determineAction:163-165` 设 triggeredAction=NOTIFY_OWNER 但 **无实际 notify 派发**（grep `IErpSysNotificationBiz|notifyOwner` in LeadScoringEngine = 0）。`ErpCrmLeadScoreConfigBizModel` 为 19 行 CrudBizModel stub（无 isActive=true 保存时唯一性校验，仅评分时校验，P2 纵深）。
  - **UC-CRM-11 线索区域自动分配**（⚠️ PARTIAL——ROUND_ROBIN/LOAD_BALANCED 降级为 MANUAL + 代理字段）：`TerritoryAssignmentEngine.java:25-273`（4 conditionType 全匹配 + 默认 fallback + 无匹配 null）；engine 调用 `defaultPrepareSave:194-217`（config-gated CONFIG_TERRITORY_AUTO_ASSIGN_ON_CREATE）+ `assignLead:122-142`/`reassignLead:144-163`。**缺失 #6（L1 `:255-258` assignmentMethod 2/3 降级）**：`TerritoryAssignmentEngine.toResult:73-84` **显式将非 MANUAL 方法降级为 MANUAL**（设 degraded=true，ownerId=null）——ROUND_ROBIN（轮流）+ LOAD_BALANCED（最少线索）**未实现**；L2 territory.md:224-228 标记 Deferred（"触发条件：ErpCrmTeamMember 实体落地"）。**缺失 #7（L1 `:248/:253` 代理字段）**：GEOGRAPHY 经 companyName.contains(province)（无 province 字段）；INDUSTRY 经 department.contains(code)（无 industryCode）；CUSTOMER_SIZE 经 expectedRevenue（无 companySize）——engine 工作于代理字段非 L1 字面字段。须按 §4 三判据复核 territory.md §实现注记 Deferred：(i) 无独立 plan-audit？(ii) owner doc 注记 AI 落地无人工批准痕迹？(iii) product-scope 未裁剪？三判据不满足→按 Q4=(a) 重开（与 A1.10 P1-RC-009 BOM 快照同型）。

- **L4 测试证据现状**（`module-crm/erp-crm-service/src/test/`）：`TestErpCrmLeadConversion.java`（402 行，9 @Test：testFullConversionChain/testLoseWithoutReasonRejected/testIllegalTransitionRejected/testAlreadyConvertedRejected/testOpportunityWithoutPartnerRejected/testLeadTypeMismatchRejected/testDuplicateDetection/testZeroPollutionAssertion/testCancel——**强**，含反射零污染断言）；`TestErpCrmForecastAndScoring.java`（5 @Test：testScoringAndAutoQualify 断言 totalScore=100 + AUTO_QUALIFY + NEW→QUALIFIED + 只追加 2 记录 / testNoActiveConfigReturnsNull / forecast——**强**）；`TestErpCrmTerritoryQuota.java`（604 行，10 @Test：4 conditionType + 默认 fallback + reassignLead + territory 树 level/fullPath/isLeaf + cycle/depth 拒绝——**强**）。E2E：`tests/e2e/business-actions/crm-lead.action.spec.ts`（71 行，2 test：NEW→QUALIFIED→moveStage→cancel；NEW→CANCELLED——**MEDIUM**，仅覆盖 qualify+moveStage+cancel，**不覆盖** convertToCustomer/convertToQuotation/lose/scoring/territory）。**测试缺口**：#1 直接升格分支无测试（feature 缺失）；#2/#3 前置条件弱无负向测试；#4 SCHEDULED 无测试（job 不存在）；#5 NOTIFY_OWNER 派发无断言；#6 ROUND_ROBIN/LOAD_BALANCED 降级无断言；#7 代理字段无断言；lastContactDate 在 qualify 无断言。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14）：crm Lead 5 态 + Event 3 态 + 转化跨域经 Facade PASS；P1-MA2-075（stageId 单向递增守卫，UC-CRM-06 非本切片，resolved R1.24）+ P1-MA2-076（Event reminderMinutesBefore 死字段，UC-CRM-08 非本切片，resolved R1.24）。
  - `docs/audits/2026-07-27-1430-arm-ma1-platform-conformance-bc-tier.md`（A1.13）：crm 15 维平台合规 15/15 PASS（跨实体经 Facade 0 跨模块写）。
  - `docs/audits/2026-07-28-1510-arm-ma2-multi-company-isolation.md`（A2.18）：crm `ErpCrmLeadBizModel:217 loadActiveRules(orgId)` flagged helper 收 orgId 参数（P1-MA2-093 orgId 查询隔离，resolved R1.29）。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）：crm 4 BizModel + LeadScoringEngine 代码质量 PASS；P1-MA1-009（crm DECIMAL↔double，MR1，A4.5 建议 P2——非本切片 Lead 实体）。
  - **无既有 MA2/MA4 报告审计 UC-CRM-01/02/03/04/09/11 需求契约符合性**——本切片为 CRM 域首个 RC 切片，候选缺口均为新发现。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为（Lead 状态机 + 跨域 Facade + 平台合规），只补"需求契约↔行为"差异（直接升格分支缺失 / 前置条件弱 / SCHEDULED 缺失 / ROUND_ROBIN 降级 / lastContactDate 缺失等）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA1-009`（crm DECIMAL↔double，`:arm-index`，非本切片 Lead 实体）、`P1-MA1-022`（5 域跨域只读 daoFor 含 crm，非本切片）、`P1-MA2-075`（stageId 守卫 UC-06 非本切片 resolved）、`P1-MA2-076`（reminderMinutesBefore UC-08 非本切片 resolved）、`P1-MA2-086`（cron job 并发含 crm event-reminder/sequence-overdue，UC-08/14 非本切片 resolved）、`P1-MA2-093`/`094`（orgId 隔离 resolved R1.29）、`P2-MA4-020`（crm badge 漂移 watch-only 视图层）、`P1-MA3-004`（8 扩展域 README schema resolved R2.1）。**RC 系列对 crm 为零**（A1.28 为 CRM 域首个 RC 切片）。本切片须 grep arm-index crm lead/opportunity/quote/scoring/territory 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1。UC-CRM-01 lastContactDate / UC-CRM-02 直接升格+前置条件 / UC-CRM-03 前置条件 / UC-CRM-09 SCHEDULED job + NOTIFY_OWNER 派发 修复均属**代码逻辑**类（预授权）。**UC-CRM-11 ROUND_ROBIN/LOAD_BALANCED 修复可能触及 ORM**——L2 标记 Deferred 触发条件为 `ErpCrmTeamMember` 实体落地；若修复需新增该实体 → **ORM 结构变更须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，触及行等待人工批准）。须人工确认 product-scope 是否要求直接升格分支/won-stage 前置/SCHEDULED 评分/ROUND_ROBIN 分配（若 L1 明确要求则 P1 强制实现）。

- **剩余差距**：A1.28 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源，且 UC-CRM-02 直接升格+前置条件弱 / UC-CRM-03 won-stage 前置缺失 / UC-CRM-09 SCHEDULED 缺失+L2 失实 / UC-CRM-11 ROUND_ROBIN 降级 是潜在合规风险（任意状态商机可转报价单；评分定时器缺失；区域分配语义偏离）。本计划产出 A1.28 报告并登记 finding，解除 CRM 域首个 RC 切片证据缺口。

## Goals

- 产出 A1.28 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`，含方法论 §6 **9 段全部内容**。
- 对 6 UC（UC-CRM-01/02/03/04/09/11）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并、禁止跳号。
- 对候选缺口给出分级结论：#1 UC-CRM-02 直接升格分支缺失（L1 `:44-46`——倾向 **P1**）、#2 UC-CRM-02 前置条件弱（不查 QUALIFIED——倾向 **P1**）、#3 UC-CRM-03 前置条件弱（不查 QUALIFIED + isWonStage——倾向 **P1**）、#4 UC-CRM-09 SCHEDULED 触发器缺失 + L2 失实（L1 `:184`——倾向 **P1**）、#5 UC-CRM-09 NOTIFY_OWNER 无派发（L1 `:199`——倾向 **P2**）、#6 UC-CRM-11 ROUND_ROBIN/LOAD_BALANCED 降级为 MANUAL（L1 `:255-258`——倾向 **P1**，须 §4 三判据复核 territory.md Deferred，可能触及 ORM ask-first）、#7 UC-CRM-11 代理字段（倾向 **P2**）、#8 UC-CRM-01 lastContactDate 在 qualify 缺失（L1 `:27`——倾向 **P2**）、#9 UC-CRM-09 LeadScoreConfigBizModel stub（P2 纵深）——按 §2 判据定级，若为 P0/P1 则新建 `P1-RC-xxx` 并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/README.md/state-machine.md/lead-scoring.md/territory.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.29 crm-F2 独立 plan；A1.30 crm-F3 CPQ/漏斗独立；A1.28 只覆盖 UC-CRM-01/02/03/04/09/11）。
- **不复审 UC-CRM-06 stageId 守卫**（P1-MA2-075 属 A1.30，resolved R1.24，非本切片）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：Lead 状态机/跨域 Facade 由 A2.14 证实，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议，**§4 三判据为本切片 UC-CRM-11 复核 territory.md Deferred 的关键**）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.28 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.28 UC 锚点）+ `docs/design/crm/use-cases.md`（L1 真相源）+ `docs/design/crm/README.md`+`state-machine.md`+`lead-scoring.md`+`territory.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/A4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测/E2E；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-crm/erp-crm-service -Dtest=TestErpCrmLeadConversion,TestErpCrmForecastAndScoring,TestErpCrmTerritoryQuota`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-CRM-01/02/03/04/09/11 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:15/:33/:54/:74/:175/:239` 验收标准原文；L2 引用 `crm/README.md` §核心业务对象/§状态机/§衔接契约、`state-machine.md`、`lead-scoring.md`、`territory.md`（标注"设计参考，冲突以 L1 为准"，L2↔L1 冲突如 README.md:67-68 仅 convertToCustomer 路径丢直接升格、lead-scoring.md:157 失实声称 SCHEDULED 已接线）；L3 引用 `ErpCrmLeadBizModel.java`/`ErpCrmLeadProcessor.java`/`ErpCrmConversion*Processor.java`/`LeadScoringEngine.java`/`TerritoryAssignmentEngine.java`/`ConditionMatcher.java`（含行号）；L4 引用 `TestErpCrmLeadConversion.java#method`/`TestErpCrmForecastAndScoring.java#method`/`TestErpCrmTerritoryQuota.java#method`（注明断言强度）；L5 复用 A2.14/A1.13/A4.5 + E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-CRM-01 qualify（doQualify:121-131 设 QUALIFIED+首stage+default probability）；②#8 UC-CRM-01 lastContactDate（L1 `:27`——doQualify 不设，复核）；③UC-CRM-02 convertToCustomer（createPartnerFromLead+createOpportunityFromLead+markLeadConverted，复核弱指针）；④#1 UC-CRM-02 直接升格分支（L1 `:44-46`——grep 直接升格=0，复核缺失）；⑤#2 UC-CRM-02 前置条件（不查 QUALIFIED，复核 NEW 可转化）；⑥UC-CRM-03 convertToQuotation + 零污染（testZeroPollutionAssertion 强断言复核）；⑦#3 UC-CRM-03 前置条件（不查 QUALIFIED + isWonStage，复核 isWonStage 是否被任何处消费）；⑧UC-CRM-04 lose + lostReason 必填（强测复核）；⑨UC-CRM-09 评分引擎核心（LOOKUP/FORMULA/BOOLEAN + 归一化 + auto-qualify，强测复核）；⑩#4 UC-CRM-09 SCHEDULED（无 job + 无 scheduler.yaml + L2 失实）；⑪#5 UC-CRM-09 NOTIFY_OWNER（无派发）；⑫UC-CRM-11 territory 引擎 4 conditionType + 默认 fallback（强测复核）；⑬#6 UC-CRM-11 ROUND_ROBIN/LOAD_BALANCED 降级（toResult:73-84 显式 MANUAL 降级）；⑭#7 UC-CRM-11 代理字段（GEOGRAPHY/INDUSTRY/CUSTOMER_SIZE 用 companyName/department/expectedRevenue）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：#1/#2/#3 行为实质偏离验收标准（§2 P1①）——倾向 **P1**（#6 UC-CRM-11 须先按 §4 三判据复核 territory.md Deferred，并标注"修复可能触及 ORM ask-first"）；#4 SCHEDULED 缺失+L2 失实（§2 P1①）——倾向 **P1**；#5/#7/#8/#9 倾向 **P2**；UC-CRM-04 已实现接受。每结论须列明命中判据编号 + 三源对照 + L2 失实记录。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-CRM-01/02/03/04/09/11 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.14/A1.13/A4.5 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#9 有明确分级（非悬空"待查"）；#1/#2/#3/#4/#6 有明确 P1 倾向 + #6 标注 ORM ask-first 风险 + L2 失实（lead-scoring.md:157）记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` crm lead/opportunity/quote/scoring/territory 同域同控制点后裁决——#1-#9 均为 CRM 域**首个 RC 切片新发现**（既有 arm-index 无 RC finding 涉及 crm lead 生命周期/评分/territory 分配）→ 全部**新建 P1-RC-xxx / P2-RC-xxx** 列明差异依据；UC-CRM-04 已证实→复用注记。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1；#6 标注"触及 ORM ask-first"）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 NEW 状态 LEAD 实际能否被 convertToCustomer、任意 docStatus OPPORTUNITY 实际能否转报价单、LEAD_UPDATE 自动评分在并发更新下的触发时序、territory 引擎无匹配时 territoryId 实际留空行为、ROUND_ROBIN 降级 MANUAL 后 ownerId 实际值等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14 Lead 状态机 + 跨域 Facade PASS）+ A1.13（平台合规）+ A4.5（代码质量），列明只补的需求视角差异（直接升格缺失/前置条件弱/SCHEDULED 缺失+L2 失实/ROUND_ROBIN 降级/lastContactDate 缺失/NOTIFY_OWNER 无派发/代理字段）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [ ] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.2 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_039d7267cffeECm0iB5WPg9BhX，fresh session，未起草本计划）。live-baseline 全量独立复核 CONFIRMED——两处最高风险声明均实测为真：①`ErpCrmStage.isWonStage` 字段存在于 ORM（`app-erc-crm.orm.xml:317`，仅视图层+UC-CRM-06 stage-direction 消费，conversion processor 从不读——证实 #3 前置条件弱）；②`lead-scoring.md:157` 逐字失实声称"SCHEDULED: ErpCrmLeadScoringRecalcJob + scheduler.yaml 已接线"（实际 4 jobs 无评分 job + 无 scheduler.yaml——证实 #4 + L2 失实）。其余（直接升格分支缺失 grep=0 / convertToCustomer 不查 QUALIFIED / TerritoryAssignmentEngine.toResult:73-84 降级 MANUAL / territory.md:224-228 Deferred AI 注记 / arm-index crm RC=0 / L1 逐字引用）全 CONFIRMED。Rule 4 单结果面（6 UC）/ anti-slack / Exit localized / Closure Gates 只读审计定制 / 方法论 §4 三判据+§7+§9+Q4=(a) / ORM ask-first（UC-CRM-11 可能触 ErpCrmTeamMember）/ Deferred 诚实 全 PASS。3 项非阻塞观察（#5 NOTIFY_OWNER P2 vs P1④ 辩论空间 / Phase1⑦ isWonStage 复核范围 / territory.md 注记三判据 (ii) AI 无人工痕迹→倾向重开）已记录供执行 agent 注意。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.28 报告 9 段齐全 + 6 UC 逐矩阵行 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.28 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；UC-CRM-01/02/03/09 修复属**代码逻辑**类（预授权）。**UC-CRM-11 ROUND_ROBIN/LOAD_BALANCED 修复可能触及 ORM**（L2 Deferred 触发条件 = ErpCrmTeamMember 实体落地；若修复需新增实体 → **ORM ask-first + 独立 plan-audit**，§5 暂停协议触及行等待人工批准）。#1/#2/#3/#4/#6 须人工确认 product-scope 是否要求（直接升格/won-stage 前置/SCHEDULED 评分/ROUND_ROBIN 分配）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；#6 触及 ORM 行须 ask-first；#1/#2/#3/#4/#6 待人工确认 product-scope 范围）

## Closure

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围
- #6 UC-CRM-11 ROUND_ROBIN/LOAD_BALANCED 修复若需 ErpCrmTeamMember 实体须 ORM ask-first
- #1/#2/#3/#4 须人工确认 product-scope 是否要求对应 L1 验收标准
