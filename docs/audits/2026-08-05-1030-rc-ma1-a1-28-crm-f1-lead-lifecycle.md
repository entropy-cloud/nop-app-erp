# rc-ma1-a1-28 crm-F1 线索生命周期 需求-实现符合性审计

> 报告类型：MA1(RC) 五级追踪审计（requirement-compliance mission，A1.28 切片）
> 域：crm | 功能切片：crm-F1 线索生命周期 | UC 清单：UC-CRM-01/02/03/04/09/11（6 UC）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md` A1.28（UC 锚点 `use-cases.md:15/:33/:54/:74/:175/:239`，覆盖率 ✅ 一致，无基线分歧 D-xx）
> 计划：`docs/plans/2026-08-03-1341-2-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（独立草案审查 ses_039d7267cffeECm0iB5WPg9BhX accept）
> 审计性质：**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更）
> 报告日期：2026-08-05

---

## 1. 需求契约原文（L1，逐字引用）

> 真相源 = `docs/design/crm/use-cases.md`（权威功能契约，§4 Q1 层级 2）。下方逐字引用 6 UC 的验收标准原文，不转述。

### UC-CRM-01 线索创建与验证（`use-cases.md:15`）

```
Lead 创建：leadType=LEAD, docStatus=NEW, contactName/companyName 必填
Lead 跟进验证 →
  docStatus: NEW → QUALIFIED
  stageId 可设（取 ErpCrmStage.sequence 的第一个阶段）
  probability 取 stage.defaultProbability（用户可覆盖）
  lastContactDate 自动更新为当前时间
```

### UC-CRM-02 线索 → 商机转化（`use-cases.md:33`）

```
Lead.docStatus == QUALIFIED 且 leadType == LEAD →
  执行转化操作：
    if convertToCustomer:
      创建 ErpMdPartner（派生自 contactName/companyName/contactPhone/contactEmail）
      创建 ErpCrmLead(leadType=OPPORTUNITY, partnerId=新建客户)
      原 lead.docStatus → CONVERTED
    if 不创建客户:
      lead.leadType → OPPORTUNITY（直接升格）
  LEAD 类型不可直接转报价单（系统拦截）
```

### UC-CRM-03 商机 → 报价单转化（`use-cases.md:54`）

```
Lead.leadType == OPPORTUNITY 且 lead.docStatus == QUALIFIED
 且 stage.isWonStage == true →
   convertToQuotation(leadId, quotationData) →
    调用 IErpSalQuotationBiz 创建 ErpSalQuotation（跨域 I*Biz）
    回写 lead.relatedBillType = 'SALES_QUOTATION'
    回写 lead.relatedBillCode = 报价单号
    lead.docStatus → CONVERTED
  转化后：ErpSalQuotation 无 CRM 外键（核心零污染）
```

### UC-CRM-04 丢单原因记录（`use-cases.md:74`）

```
Lead.docStatus 为 NEW 或 QUALIFIED →
  标记丢失 →
    lostReasonId 必填（不允许空）
    lostReasonDesc 可选（补充说明）
    lead.docStatus → LOST（终态）
  若 lostReasonId 为空 → 拒绝迁移，返回校验错误
```

### UC-CRM-09 线索自动评分（`use-cases.md:175`）

```
管理员创建 ErpCrmLeadScoreConfig(isActive=true) 并配置 configLines
（如 SOURCE_WEIGHT=LOOKUP, ENGAGEMENT_SCORE=FORMULA）

评分触发（MANUAL / LEAD_UPDATE / SCHEDULED）→
  加载当前生效的评分规则 →
  按 configLines 逐条评分（LOOKUP/FORMULA/BOOLEAN）→
  计算 totalScore（归一化 0-100）→
  创建 ErpCrmLeadScore + ErpCrmLeadScoreLine 记录 →
  回写 lead.score = totalScore

if totalScore >= autoQualifyThreshold
  且 lead.leadType == LEAD 且 lead.docStatus == NEW →
    自动执行：
      lead.docStatus → QUALIFIED
      lead.score = totalScore
      ErpCrmLeadScore.triggeredAction = AUTO_QUALIFY

if autoQualifyThreshold > totalScore >= minScoreForFollowUp →
  NOTIFY_OWNER（通知销售优先跟进不改变 docStatus）

评分历史只追加：每次评分创建新记录，lead 当前分数取最新一条
```

### UC-CRM-11 线索区域自动分配（`use-cases.md:239`）

```
管理员配置 ErpCrmTerritory 树形结构（REGION → AREA → BRANCH → TEAM）
  并创建 ErpCrmTerritoryAssignmentRule(
    priority=1, conditionType=GEOGRAPHY,
    conditionValue={"province":["上海","浙江"]},
    assignmentMethod=ROUND_ROBIN)

线索创建（未指派 owner/team）→
  按优先级遍历 isActive=true 的规则 →
    匹配线索字段（province/industry/companySize/sourceId）→
    找到首个匹配规则 →
    按 assignmentMethod 分配：
      ROUND_ROBIN → 轮流分给团队内成员
      LOAD_BALANCED → 分给线索最少的成员
      MANUAL → 标记待分配
    回写 lead.territoryId / lead.teamId / lead.ownerId

无规则匹配 → 使用 isDefault=true 的规则
仍无匹配 → territoryId 留空，标记"未分配"
```

---

## 2. 实现证据（L3 代码路径，含行号）

> 跨域调用链列全（Facade → Processor → 跨域 I*Biz）。L3 行号经 HEAD 实仓复核。

| UC | 代码路径（含行号） |
|----|-------------------|
| UC-CRM-01 | `ErpCrmLeadBizModel.java:48`（CrudBizModel Facade）；`qualify:89-91`→`ErpCrmLeadQualifyProcessor`→`ErpCrmLeadProcessor.doQualify:121-131`（设 QUALIFIED + 首 stage `findFirstStage:200-206` + `applyDefaultProbability:170-174`）；contactName/companyName 必填在 XMeta 层（`_ErpCrmLead.xmeta` mandatory）；**`doQualify:121-131` 不设 `lastContactDate`**（grep `setLastContactDate` in ErpCrmLeadProcessor/QualifyProcessor = 0 命中；lastContactDate writer 仅在 `LeadActivityDerivationHelper` UC-05 事件驱动路径） |
| UC-CRM-02 | `ErpCrmLeadBizModel.convertToCustomer:176-178`→`ErpCrmConversionConvertToCustomerProcessor.convertToCustomer:19-28`（仅 `validateNotConverted:21` + `validateLeadType(LEAD):22`，**不查 docStatus==QUALIFIED**）→`ErpCrmConversionProcessor.createPartnerFromLead:62-73`（IErpMdPartnerBiz.save）+ `createOpportunityFromLead:88-104`（新 ErpCrmLead leadType=OPPORTUNITY `:92`）+ `markLeadConverted:106-112`（原 lead CONVERTED + relatedBillType=CRM_LEAD 弱指针）；LEAD 直转报价拦截 `ErpCrmConversionConvertToQuotationProcessor:24 validateLeadType(OPPORTUNITY)`；**grep `直接升格\|directPromote\|promoteToOpportunity` 业务命中=0**——`setLeadType(OPPORTUNITY)` 唯一命中点 `ErpCrmConversionProcessor:92` 在 `createOpportunityFromLead` 内（新建 ErpCrmLead），**"不创建客户→lead.leadType→OPPORTUNITY 直接升格"分支未实现** |
| UC-CRM-03 | `ErpCrmLeadBizModel.convertToQuotation:182-187`→`ErpCrmConversionConvertToQuotationProcessor.convertToQuotation:21-30`（仅 `validateNotConverted:23` + `validateLeadType(OPPORTUNITY):24` + `requireOpportunityPartner:25`，**不查 docStatus==QUALIFIED 且不查 stage.isWonStage==true**）；跨域 `createQuotationFromOpportunity:75-84`（IErpSalQuotationBiz.save）；弱指针 `markLeadConverted`（relatedBillType=SALES_QUOTATION+code）；**`isWonStage` 在 crm-service 唯一消费点 = `ErpCrmLeadBizModel.findOpportunityBoardData:300`（🏆 emoji 展示），conversion processor 从不读 isWonStage**——won-stage 前置静默丢弃 |
| UC-CRM-04 | `ErpCrmLeadBizModel.lose:95-102`→`ErpCrmLeadLoseProcessor`→`ErpCrmLeadProcessor.validateTransitionForLose:58-64`（仅 NEW/QUALIFIED）+ `requireLostReason:112-117`（null 抛 ERR_LOST_REASON_REQUIRED）+ `doLose:133-140` |
| UC-CRM-09 | `LeadScoringEngine.recalculateScore:59-87`（LOOKUP/FORMULA/BOOLEAN `scoreLine:91-119` + 归一化 `normalize:125-145` + 只追加历史 `scoreDao().saveEntity:76` + auto-qualify 阈值触发 `determineAction:151-167`→`qualifyProcessor.qualify:84`）；`ErpCrmLeadScoreBizModel.recalculateScore`（@BizMutation，MANUAL 触发）；LEAD_UPDATE 触发 `ErpCrmLeadBizModel.defaultPrepareUpdate:227-240`（config-gated CONFIG_LEAD_SCORING_RECALC_ON_LEAD_UPDATE 默认 true）；active config 唯一 `loadActiveConfig:300-310`（>1 抛 ERR_MULTIPLE_ACTIVE_SCORE_CONFIG）；**SCHEDULED 触发器完全缺失**——`module-crm/.../job/` 仅 4 jobs（ForecastRecalc/SequenceOverdue/EventReminder/FunnelAggregation），**无 ErpCrmLeadScoringRecalcJob**；`app-service.beans.xml:47-93` 仅注册 4 job bean，无评分 job；无 scheduler.yaml under module-crm；**NOTIFY_OWNER 无派发**——`determineAction:163-164` 返回 NOTIFY_OWNER 但 `recalculateScore:59-87` 仅在 autoQualified 时调 qualify，grep `IErpSysNotificationBiz\|notifyOwner\|notify` in LeadScoringEngine = 0 命中；`ErpCrmLeadScoreConfigBizModel.java` = **19 行 CrudBizModel stub**（无 isActive=true 保存时唯一性校验，仅评分时 `loadActiveConfig` 校验） |
| UC-CRM-11 | `TerritoryAssignmentEngine.assign:45-71`（4 conditionType 匹配 `ConditionMatcher:88-222` + 默认 fallback `:67-69` + 无匹配 null `:70`）；engine 调用 `ErpCrmLeadBizModel.defaultPrepareSave:196-223`（config-gated CONFIG_TERRITORY_AUTO_ASSIGN_ON_CREATE）+ `assignLead:127-149`/`reassignLead:151-170`；**ROUND_ROBIN/LOAD_BALANCED 降级**——`toResult:73-84` 显式将非 MANUAL 方法降级为 MANUAL（设 `degraded=true :81`，ownerId 永不设置）；**代理字段**——GEOGRAPHY `matchGeography:114-126` 经 `companyName.contains(province)`（无 province 字段，`:111-112` 注释自承）；INDUSTRY `matchIndustry:132-144` 经 `department.contains(code)`（无 industryCode 字段，`:130` 注释自承）；CUSTOMER_SIZE `matchCustomerSize:150-164` 经 `expectedRevenue`（无 companySize 字段，`:148` 注释自承） |

---

## 3. 测试证据（L4 测试断言，注明强度）

> 测试位于 `module-crm/erp-crm-service/src/test/`。E2E 位于 `tests/e2e/business-actions/crm-lead.action.spec.ts`。

| 测试 | 覆盖 UC | 断言强度 |
|------|---------|---------|
| `TestErpCrmLeadConversion.java`（402 行，9 @Test） | UC-CRM-01/02/03/04 | **强**——`testFullConversionChain`（NEW→QUALIFIED→convertToCustomer→OPPORTUNITY→convertToQuotation→CONVERTED 全链）+ `testLoseWithoutReasonRejected:133-147`（lostReason 必填负向断言）+ `testIllegalTransitionRejected` + `testAlreadyConvertedRejected` + `testOpportunityWithoutPartnerRejected` + `testLeadTypeMismatchRejected`（LEAD 直转报价拦截）+ `testDuplicateDetection` + `testZeroPollutionAssertion:225-233`（**反射零污染断言**——证实 ErpSalQuotation/ErpMdPartner 无 getOpportunityId/getLeadId）+ `testCancel` |
| `TestErpCrmForecastAndScoring.java`（5 @Test） | UC-CRM-09 | **强**——`testScoringAndAutoQualify`（断言 totalScore=100 + AUTO_QUALIFY + NEW→QUALIFIED + 只追加 2 记录）+ `testNoActiveConfigReturnsNull`（无 active config 返回 null 不阻断）+ forecast |
| `TestErpCrmTerritoryQuota.java`（604 行，10 @Test） | UC-CRM-11 | **强**——4 conditionType（GEOGRAPHY/INDUSTRY/CUSTOMER_SIZE/CUSTOM_FIELD）+ 默认 fallback + `reassignLead` + territory 树 level/fullPath/isLeaf + cycle/depth 拒绝 |
| E2E `crm-lead.action.spec.ts`（71 行，2 test） | UC-CRM-01（部分） | **MEDIUM**——仅覆盖 NEW→QUALIFIED→moveStage→cancel；NEW→CANCELLED。**不覆盖** convertToCustomer/convertToQuotation/lose/scoring/territory |

**测试缺口**（按候选缺口对应）：① 直接升格分支无测试（feature 缺失）；②③ 前置条件弱无负向测试（无 NEW 状态 LEAD 转 customer / 非 QUALIFIED 非 won-stage 转报价的拒绝测试）；④ SCHEDULED 无测试（job 不存在）；⑤ NOTIFY_OWNER 派发无断言；⑥ ROUND_ROBIN/LOAD_BALANCED 降级无断言；⑦ 代理字段无断言；⑧ lastContactDate 在 qualify 无断言。

---

## 4. 运行时行为证据（L5）

> 来源：复用既有 MA2/A1.13/A4.5 报告已证实行为（§去重协议，不重新核实行为本身）+ 本切片 L3/L4 静态证据。运行时存疑点入 §7。

- **复用 A2.14**（`2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`）：crm Lead 5 态（NEW/QUALIFIED/CONVERTED/LOST/CANCELLED）+ Event 3 态状态机 PASS；转化跨域经 Facade（IErpSalQuotationBiz/IErpMdPartnerBiz）PASS（零跨模块 ORM 写）。P1-MA2-075（stageId 单向递增守卫 UC-CRM-06 非本切片 resolved R1.24）+ P1-MA2-076（Event reminderMinutesBefore 死字段 UC-CRM-08 非本切片 resolved R1.24）。
- **复用 A1.13**（`2026-07-27-1430-arm-ma1-platform-conformance-bc-tier.md`）：crm 15 维平台合规 15/15 PASS（跨实体访问经 Facade 0 跨模块写）。
- **复用 A4.5**（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`）：crm 4 BizModel + LeadScoringEngine 代码质量 PASS（P1-MA1-009 crm DECIMAL↔double 非本切片 Lead 实体）。
- **复用 A2.18**（`2026-07-28-1510-arm-ma2-multi-company-isolation.md`）：crm `ErpCrmLeadBizModel.loadActiveRules` 收 orgId 参数（P1-MA2-093 orgId 查询隔离 resolved R1.29）。
- **L3/L4 静态证实**：UC-01 qualify 主路径（QUALIFIED+首stage+default probability）、UC-02 convertToCustomer 主路径（创建 Partner+Opportunity+原 lead CONVERTED）、UC-03 零污染（反射强断言）、UC-04 lose+lostReason 必填（强负向断言）、UC-09 评分核心（LOOKUP/FORMULA/BOOLEAN+归一化+auto-qualify 强测）、UC-11 territory 4 conditionType+默认 fallback（强测）均行为正确（经 A2.14+A4.5+单测三重证实）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论）

> 方法论 §2 判据取最高；§4 Q1 真相源层级（L1 权威，L2 冲突以 L1 为准）；§5 Q4 修复义务（P0/P1 必须实现禁方案 B 无例外）。

### UC-CRM-01 线索创建与验证 — **接受 on 主路径 + P2 on lastContactDate**

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| leadType=LEAD/docStatus=NEW/contactName/companyName 必填 | XMeta mandatory + CrudBizModel | TestErpCrmLeadConversion seed | ✅ 接受 |
| NEW→QUALIFIED | `doQualify:121-122` | testFullConversionChain | ✅ 接受 |
| stageId 取 sequence 首个阶段 | `findFirstStage:200-206`+`doQualify:123-129` | testFullConversionChain | ✅ 接受 |
| probability 取 stage.defaultProbability（用户可覆盖） | `applyDefaultProbability:170-174`（仅 null 时设） | testFullConversionChain | ✅ 接受 |
| **lastContactDate 自动更新为当前时间** | **`doQualify:121-131` 不设 lastContactDate**（grep setLastContactDate=0） | **无 lastContactDate 断言** | **❌ P2（P2-RC-033）** |

**结论**：主路径（qualify 状态迁移+首stage+default probability）接受；**lastContactDate 自动更新缺失 → P2-RC-033**（§2 P2①——次要验收标准未满足，主路径 OK 边界弱；lastContactDate 经 UC-05 事件驱动路径 `LeadActivityDerivationHelper` 写入，但 qualify 动作本身不写，L1 字面"自动更新为当前时间"在 qualify 时刻未满足）。

### UC-CRM-02 线索 → 商机转化 — **P1**

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 前置：docStatus==QUALIFIED 且 leadType==LEAD | `convertToCustomerProcessor:21-22` 仅 validateNotConverted+validateLeadType(LEAD)，**不查 docStatus** | 无 NEW 拒绝转化负向测试 | **❌ P1（P1-RC-033）** |
| if convertToCustomer：创建 Partner+Opportunity(leadType=OPPORTUNITY,partnerId)+原 lead CONVERTED | `createPartnerFromLead:62-73`+`createOpportunityFromLead:88-104`+`markLeadConverted:106-112` | testFullConversionChain + testZeroPollutionAssertion | ✅ 接受 |
| **if 不创建客户：lead.leadType→OPPORTUNITY（直接升格）** | **grep 直接升格=0；setLeadType(OPPORTUNITY) 唯一命中点在新建 ErpCrmLead 内（createOpportunityFromLead:92），无原 lead 升格分支** | **无直接升格测试** | **❌ P1（P1-RC-032）** |
| LEAD 不可直接转报价单（系统拦截） | `convertToQuotationProcessor:24 validateLeadType(OPPORTUNITY)` | testLeadTypeMismatchRejected | ✅ 接受 |

**结论**：**P1**——两条候选缺口均成立：#1 直接升格分支缺失（P1-RC-032，§2 P1① 功能完全缺失）+ #2 前置条件弱不查 QUALIFIED（P1-RC-033，§2 P1② 异常路径/前置条件未守卫——NEW 状态 LEAD 可被转化）。L2 `README.md §衔接契约:67` + `state-machine.md:36` 仅描述 convertToCustomer 路径，**丢直接升格分支**——按 §4 Q1 L2↔L1 冲突以 L1 为准，L2 推定已向实现妥协。

### UC-CRM-03 商机 → 报价单转化 — **P1**

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 前置：leadType==OPPORTUNITY 且 docStatus==QUALIFIED 且 stage.isWonStage==true | `convertToQuotationProcessor:21-25` 仅 validateNotConverted+validateLeadType(OPPORTUNITY)+requireOpportunityPartner，**不查 docStatus 且不查 isWonStage**；isWonStage 唯一消费点=board emoji `:300` | 无前置条件负向测试 | **❌ P1（P1-RC-034）** |
| convertToQuotation→IErpSalQuotationBiz 创建 ErpSalQuotation | `createQuotationFromOpportunity:75-84` | testFullConversionChain | ✅ 接受 |
| 回写 relatedBillType=SALES_QUOTATION + relatedBillCode | `markLeadConverted` | testFullConversionChain | ✅ 接受 |
| lead.docStatus→CONVERTED | `markLeadConverted:106-112` | testFullConversionChain | ✅ 接受 |
| ErpSalQuotation 无 CRM 外键（核心零污染） | — | **testZeroPollutionAssertion:225-233 反射强断言** | ✅ 接受 |

**结论**：**P1**——#3 前置条件弱（P1-RC-034，§2 P1②——任意 docStatus 的 OPPORTUNITY 可转报价单 + won-stage 前置静默丢弃；conversion processor 从不读 isWonStage）。主路径（跨域创建+零污染+弱指针回写）接受。

### UC-CRM-04 丢单原因记录 — **接受**

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 前置：docStatus 为 NEW 或 QUALIFIED | `validateTransitionForLose:58-64` | testIllegalTransitionRejected | ✅ 接受 |
| lostReasonId 必填（不允许空） | `requireLostReason:112-117`（null 抛 ERR_LOST_REASON_REQUIRED） | **testLoseWithoutReasonRejected:133-147 强负向断言** | ✅ 接受 |
| lostReasonDesc 可选 | `doLose:136-138`（null check） | testFullConversionChain | ✅ 接受 |
| docStatus→LOST（终态） | `doLose:133-134` | testFullConversionChain | ✅ 接受 |
| 若 lostReasonId 为空→拒绝返回校验错误 | `requireLostReason:112-117` | testLoseWithoutReasonRejected | ✅ 接受 |

**结论**：**接受**（已实现且强测）。

### UC-CRM-09 线索自动评分 — **P1**

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 管理员建 ErpCrmLeadScoreConfig(isActive=true)+configLines | `ErpCrmLeadScoreConfigBizModel` 19 行 CRUD stub | — | ✅ 接受（CRUD 可用） |
| **评分触发 MANUAL / LEAD_UPDATE / SCHEDULED** | MANUAL `ErpCrmLeadScoreBizModel.recalculateScore` + LEAD_UPDATE `defaultPrepareUpdate:227-240`；**SCHEDULED 完全缺失**（4 jobs 无评分 job + 无 scheduler.yaml + beans.xml 无评分 job bean） | SCHEDULED 无测试 | **❌ P1（P1-RC-035）** |
| 加载生效规则逐条评分（LOOKUP/FORMULA/BOOLEAN） | `scoreLine:91-119` | testScoringAndAutoQualify | ✅ 接受 |
| 归一化 totalScore(0-100) | `normalize:125-145` | testScoringAndAutoQualify（totalScore=100） | ✅ 接受 |
| 建 ErpCrmLeadScore+Line | `recalculateScore:75-81` | testScoringAndAutoQualify（2 记录） | ✅ 接受 |
| 回写 lead.score | Lead ORM 无 score 列（Option B 派生查询最新 totalScore，lead-scoring.md §实现约定） | — | ✅ 接受（L2 实现约定记录） |
| totalScore>=autoQualifyThreshold 且 LEAD 且 NEW→自动 QUALIFIED+triggeredAction=AUTO_QUALIFY | `determineAction:151-167`+`qualifyProcessor.qualify:84` | testScoringAndAutoQualify（NEW→QUALIFIED+AUTO_QUALIFY） | ✅ 接受 |
| **autoQualifyThreshold>totalScore>=minScoreForFollowUp→NOTIFY_OWNER（通知销售优先跟进）** | `determineAction:163-164` 返回 NOTIFY_OWNER 但 **无实际 notify 派发**（grep notify in LeadScoringEngine=0） | 无 NOTIFY 派发断言 | **❌ P2（P2-RC-031）** |
| 评分历史只追加 | `scoreDao().saveEntity:76`（每次新记录） | testScoringAndAutoQualify（2 记录只追加） | ✅ 接受 |

**结论**：**P1**——#4 SCHEDULED 触发器完全缺失 + L2 失实（P1-RC-035，§2 P1①——L1 `:184` 三触发器之一完全缺失；**L2 `lead-scoring.md:157` 逐字失实声称**「SCHEDULED：ErpCrmLeadScoringRecalcJob + scheduler.yaml 已接线」实际无此 job）。#5 NOTIFY_OWNER 无派发（P2-RC-031，§2 P2①——triggeredAction 字段写入但无实际通知发送，主路径评分正确边界通知缺失）。#9 LeadScoreConfigBizModel stub（P2-RC-034，§2 P2① 纵深——保存时无 isActive=true 唯一性校验，仅评分时 loadActiveConfig 校验）。评分核心（LOOKUP/FORMULA/BOOLEAN+归一化+auto-qualify+只追加）接受。

### UC-CRM-11 线索区域自动分配 — **P1**

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 管理员建 territory 树(REGION→AREA→BRANCH→TEAM) | ErpCrmTerritory ORM + territory.md | TestErpCrmTerritoryQuota（树 level/fullPath/isLeaf） | ✅ 接受 |
| assignmentRule(conditionType=GEOGRAPHY/INDUSTRY/CUSTOMER_SIZE/CUSTOM_FIELD, assignmentMethod=ROUND_ROBIN/LOAD_BALANCED/MANUAL) | ErpCrmTerritoryAssignmentRule ORM | TestErpCrmTerritoryQuota（4 conditionType） | ✅ 接受 |
| 线索创建→按优先级遍历 isActive 规则 | `assign:45-71` | TestErpCrmTerritoryQuota | ✅ 接受 |
| **匹配字段（province/industry/companySize/sourceId）** | GEOGRAPHY 经 companyName（无 province）+ INDUSTRY 经 department（无 industryCode）+ CUSTOMER_SIZE 经 expectedRevenue（无 companySize）——**代理字段非 L1 字面** | 无代理字段断言 | **❌ P2（P2-RC-032）** |
| 找到首个匹配规则 | `assign:62-66` | TestErpCrmTerritoryQuota | ✅ 接受 |
| **按 assignmentMethod 分配：ROUND_ROBIN 轮流 / LOAD_BALANCED 最少线索 / MANUAL 待分配** | `toResult:73-84` **显式将非 MANUAL 方法降级为 MANUAL**（degraded=true，ownerId 永不设置） | 无 ROUND_ROBIN/LOAD_BALANCED 降级断言 | **❌ P1（P1-RC-036）** |
| 回写 territoryId/teamId/ownerId | `assign:73-83`+BizModel:137-146/212-220（territoryId/teamId 回写，ownerId 因降级留空） | TestErpCrmTerritoryQuota | ✅ 接受（territory/team） |
| 无匹配→isDefault 规则 | `assign:67-69` | TestErpCrmTerritoryQuota（默认 fallback） | ✅ 接受 |
| 仍无→territoryId 留空"未分配" | `assign:70` 返回 null | TestErpCrmTerritoryQuota | ✅ 接受 |

**结论**：**P1**——#6 ROUND_ROBIN/LOAD_BALANCED 降级为 MANUAL（P1-RC-036，§2 P1①——功能实质偏离验收标准，引擎显式降级不挑人）。#7 代理字段（P2-RC-032，§2 P2①——GEOGRAPHY/INDUSTRY/CUSTOMER_SIZE 用 companyName/department/expectedRevenue 代理，引擎工作于代理字段非 L1 字面字段）。

**#6 §4 三判据复核 territory.md:215-228 实现注记 §2 Deferred**（"触发条件：ErpCrmTeamMember 实体落地"）：
- (i) plan 含独立 plan-audit 通过记录？——territory.md 注记引用 "Phase 1 Decision/Explore 结论"（plan `2026-07-07-1100-1`），但方法论 §4 line 168 明确**独立 AI 审计 ≠ 人工批准**；
- (ii) owner doc 显式 documented simplification 标注且经人工批准？——territory.md:215-228 有 Deferred 标注，但为 AI 落地补注，git log 无人工批准痕迹可追溯；
- (iii) product-scope 范围裁剪登记？——product-scope 未将 ROUND_ROBIN/LOAD_BALANCED 列入范围裁剪。
- **三判据在"人工批准"意义上均不满足 → 非 documented simplification → 按 Q4=(a) 重开 P1**。**须人工确认 product-scope 是否要求 ROUND_ROBIN/LOAD_BALANCED**：若裁剪→§4(iii) 改真相源非降级；若未裁剪→P1 强制实现。**修复可能触及 ORM**（ErpCrmTeamMember 实体落地）→ **ORM 结构变更须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议触及行等待人工批准）。

### 候选缺口分级汇总（9 项）

| # | UC | 候选缺口 | 倾向分级 | finding ID | §2 判据 |
|---|----|---------|---------|-----------|---------|
| #1 | UC-CRM-02 | 直接升格分支缺失 | **P1** | P1-RC-032 | §2 P1① 功能完全缺失 |
| #2 | UC-CRM-02 | 前置条件弱（不查 QUALIFIED） | **P1** | P1-RC-033 | §2 P1② 异常路径/前置条件未守卫 |
| #3 | UC-CRM-03 | 前置条件弱（不查 QUALIFIED + isWonStage） | **P1** | P1-RC-034 | §2 P1② 异常路径/前置条件未守卫 |
| #4 | UC-CRM-09 | SCHEDULED 触发器缺失 + L2 失实 | **P1** | P1-RC-035 | §2 P1① 功能完全缺失 |
| #5 | UC-CRM-09 | NOTIFY_OWNER 无派发 | **P2** | P2-RC-031 | §2 P2① 次要验收标准未满足 |
| #6 | UC-CRM-11 | ROUND_ROBIN/LOAD_BALANCED 降级 MANUAL | **P1** | P1-RC-036 | §2 P1① + §4 三判据复核重开；修复触及 ORM ask-first |
| #7 | UC-CRM-11 | 代理字段（companyName/department/expectedRevenue） | **P2** | P2-RC-032 | §2 P2① 次要验收标准未满足 |
| #8 | UC-CRM-01 | lastContactDate 在 qualify 缺失 | **P2** | P2-RC-033 | §2 P2① 次要验收标准未满足 |
| #9 | UC-CRM-09 | LeadScoreConfigBizModel 19 行 stub | **P2** | P2-RC-034 | §2 P2① 纵深 |

**整体裁决**：6 UC 结论 = UC-CRM-01 接受 on 主路径+P2 / UC-CRM-02 P1 / UC-CRM-03 P1 / UC-CRM-04 接受 / UC-CRM-09 P1 / UC-CRM-11 P1。**5 项新 P1（P1-RC-032~036）+ 4 项新 P2（P2-RC-031~034）**。零 P0（候选缺口均不破坏活跃数据/GL 平衡/核心循环/会计正确性——CRM 域本身不直接产生会计凭证，转化经 sales 域弱指针交接）。

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

> 方法论 §7：每条 finding 产出前 grep arm-index 同域同控制点后裁决（禁止未经比对直接新建）。

**arm-index crm lead/opportunity/quote/scoring/territory 同域 grep 结果**：
- `P1-MA1-009`（crm DECIMAL↔double）——**非本切片**（非 Lead 实体），不复用。
- `P1-MA1-022`（5 域跨域只读 daoFor 含 crm）——**非本切片**（daoFor 维度非转化/评分/territory 控制点），不复用。
- `P1-MA2-075`（stageId 守卫 UC-CRM-06）——**非本切片**（UC-CRM-06 归 A1.30），resolved R1.24，不复用。
- `P1-MA2-076`（reminderMinutesBefore UC-CRM-08）——**非本切片**（UC-CRM-08 归 A1.29），resolved R1.24，不复用。
- `P1-MA2-086`（cron job 并发含 crm event-reminder/sequence-overdue）——**非本切片**（UC-CRM-08/14），不复用。
- `P1-MA2-093`/`094`（orgId 隔离）——resolved R1.29，**不同控制点**（orgId 隔离 vs 转化前置/评分触发/territory 方法），不复用。
- `P2-MA4-020`（crm badge 漂移 watch-only 视图层）——**不同控制点**，不复用。
- `P1-MA3-004`（8 扩展域 README schema）——resolved R2.1，**不同维度**，不复用。
- **RC 系列对 crm lead 生命周期/评分/territory 分配 = 零**（A1.28 为 CRM 域首个 RC 切片）。

**裁决结论**：#1-#9 均为 CRM 域**首个 RC 切片新发现**（既有 arm-index 无 RC finding 涉及 crm lead 生命周期/转化前置/评分触发器/territory 分配方法）→ **全部新建 P1-RC-032~036 / P2-RC-031~034**，列明差异依据。UC-CRM-04 已证实→接受（无 finding）。禁止未经比对新建——已 grep arm-index crm lead/opportunity/quote/scoring/territory 同域同控制点确认零重叠。

| Finding ID | UC | 描述（简） | 目标 MR | 触及保护区域 |
|-----------|----|-----------|--------|-------------|
| P1-RC-032 | UC-CRM-02 #1 | 直接升格分支缺失 | MR1（RC-R1.n） | 否（代码逻辑类预授权） |
| P1-RC-033 | UC-CRM-02 #2 | convertToCustomer 不查 QUALIFIED | MR1（RC-R1.n） | 否（代码逻辑类预授权） |
| P1-RC-034 | UC-CRM-03 #3 | convertToQuotation 不查 QUALIFIED + isWonStage | MR1（RC-R1.n） | 否（代码逻辑类预授权） |
| P1-RC-035 | UC-CRM-09 #4 | SCHEDULED 触发器完全缺失 + L2 失实 | MR1（RC-R1.n） | 否（代码逻辑+调度接线预授权） |
| P2-RC-031 | UC-CRM-09 #5 | NOTIFY_OWNER 无派发 | successor watch-only | 否 |
| P1-RC-036 | UC-CRM-11 #6 | ROUND_ROBIN/LOAD_BALANCED 降级 MANUAL | MR1（RC-R1.n）/ §4(iii) | **是——可能触及 ORM（ErpCrmTeamMember），须 ask-first + 独立 plan-audit；须人工确认 product-scope** |
| P2-RC-032 | UC-CRM-11 #7 | 代理字段 | successor watch-only | 否（或触及 ORM ask-first 若补 province/industryCode/companySize 列） |
| P2-RC-033 | UC-CRM-01 #8 | lastContactDate 在 qualify 缺失 | successor watch-only | 否 |
| P2-RC-034 | UC-CRM-09 #9 | LeadScoreConfigBizModel stub | successor watch-only | 否 |

**双向可追溯**：finding ID ↔ 修复行预留 MR1（RC-R1.n）；P1-RC-036 标注"触及 ORM ask-first"；arm-index 新 finding 行在 §6 落盘后同步写入 arm-index（见 arm-index RC 交叉引用注记）。

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点（每存疑点一行）。

- SP-1：NEW 状态 LEAD 实际能否被 `convertToCustomer` 转化（前置条件弱 P1-RC-033 的运行时触发面）——单测未构造该负向场景，运行时需确认 NEW LEAD 经 GraphQL convertToCustomer 是否成功。
- SP-2：任意 docStatus 的 OPPORTUNITY（如仍为 NEW 或已 LOST）实际能否转报价单（P1-RC-034 运行时触发面）——单测仅覆盖 QUALIFIED 路径，运行时需确认非 QUALIFIED/won-stage 路径是否成功。
- SP-3：LEAD_UPDATE 自动评分（`defaultPrepareUpdate:227-240`）在并发更新下的触发时序（同步触发是否阻塞用户保存/并发重新评分是否产生重复 ErpCrmLeadScore 记录）。
- SP-4：territory 引擎无匹配（`assign:70` 返回 null）时 territoryId 实际留空行为（BizModel:203 if territoryId==null 跳过，lead.territoryId 保持 null "未分配"）——运行时确认 UI/报表展示。
- SP-5：ROUND_ROBIN 降级 MANUAL 后 ownerId 实际值（AssignmentResult.ownerId 永不设置 → BizModel:144 if ownerId!=null 跳过 → lead.ownerId 保持 null "待分配"）——运行时确认分配语义。
- SP-6：直接升格分支（P1-RC-032）是否在其他未审计入口（如 GraphQL 自定义 action / Delta）存在补偿实现——grep 主代码 0 命中，但 Delta 层未全量扫描。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，退出码 = 0（纯 reporter，恒 0）。actual vs baseline 抽样（R1 dao() 直接调用=0 / R2 daoFor() 绕 I*Biz=34 / R3 new Erp*() 构造=229 / 生产代码总计=1382）——均为既有项目状态，**本审计为只读审计无生产代码变更，checker 无回归风险**。**不以 checker 脚本退出码 0 作为门控通过依据**（真正门控在 CI workflow `.github/workflows/compliance.yml`）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点（crm lead/opportunity/quote/scoring/territory）后给出"复用 or 新增"裁决（见 §6），无未经比对直接新建的 finding。
- [x] **真相源未修改声明**（§9 冻结条款）：本审计未修改 product-scope / use-cases.md / owner doc（README/state-machine/lead-scoring/territory）的需求契约段落。发现的 L2 失实（lead-scoring.md:157 SCHEDULED 已接线 + territory.md Deferred）记入本报告不直改真相源。
- [x] **9 段完整性自检**：落盘前自查 §1-§9 全部存在（§1 需求契约原文 / §2 实现证据 / §3 测试证据 / §4 运行时行为 / §5 符合性结论 / §6 arm-index 衔接 / §7 静态存疑点 / §8 过程纪律自检 / §9 与 MA2 报告差异增量）。

---

## 9. 与 MA2 报告差异增量声明

> 方法论 §去重协议：复用既有 MA2 报告已证实行为，只补需求视角差异。

**复用的 MA2/A1.13/A4.5 已证实行为**（不重新核实）：
- A2.14：crm Lead 5 态 + Event 3 态状态机 PASS + 转化跨域经 Facade（IErpSalQuotationBiz/IErpMdPartnerBiz）零跨模块 ORM 写。
- A1.13：crm 15 维平台合规 15/15 PASS。
- A4.5：crm 4 BizModel + LeadScoringEngine 代码质量 PASS。
- A2.18：crm orgId 查询隔离（resolved R1.29）。

**本切片只补的"需求契约↔实际行为"差异**（既有 MA2 未覆盖的需求视角）：
1. **UC-CRM-02 直接升格分支缺失**（P1-RC-032）——MA2 A2.14 证实 convertToCustomer 路径行为，未从 L1 `:44-46` "不创建客户→直接升格"视角审视分支缺失。
2. **UC-CRM-02/03 转化前置条件弱**（P1-RC-033/034）——MA2 A2.14 证实状态机迁移守卫（docStatus 终态不可恢复），未从 L1 "QUALIFIED 才可转化"/"won-stage 才可转报价"视角审视前置条件。
3. **UC-CRM-09 SCHEDULED 触发器完全缺失 + L2 失实**（P1-RC-035）——MA2/A4.5 证实评分核心引擎代码质量，未从 L1 `:184` 三触发器视角审视 SCHEDULED 缺失；**L2 `lead-scoring.md:157` 逐字失实声称 SCHEDULED 已接线**（owner doc 向实现妥协/虚报，对齐 §4 根因）。
4. **UC-CRM-09 NOTIFY_OWNER 无派发**（P2-RC-031）——A4.5 代码质量 PASS 未覆盖通知派发维度。
5. **UC-CRM-11 ROUND_ROBIN/LOAD_BALANCED 降级 MANUAL**（P1-RC-036）——MA2 A2.14 证实 territory 引擎行为（经 Facade），未从 L1 `:255-258` assignmentMethod 三方法视角审视降级；territory.md:224-228 Deferred 标注 AI 落地无人工批准痕迹，§4 三判据不满足→重开。
6. **UC-CRM-11 代理字段**（P2-RC-032）——MA2 未覆盖 conditionValue 字段映射维度。
7. **UC-CRM-01 lastContactDate 在 qualify 缺失**（P2-RC-033）——MA2 A2.14 证实 qualify 状态迁移，未从 L1 `:27` "lastContactDate 自动更新"视角审视。

**结论**：本切片与既有 MA2/A1.13/A4.5 报告互补不重复——MA2 视角 = 状态机/链路行为/代码质量，本 RC 切片视角 = 需求契约（use-case 验收标准）符合性。
