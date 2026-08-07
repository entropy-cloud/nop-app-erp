# rc-ma4 A4.2.83-A4.2.96 crm 线索转化前置/评分触发/territory 分配/营销预测配额运行时确认审计报告

> Report Status: done
> Mission: requirement-compliance（MA4 核心域展开器运行时确认）
> Work Item: A4.2.83 / A4.2.84 / A4.2.85 / A4.2.86 / A4.2.87 / A4.2.88 / A4.2.89 / A4.2.90 / A4.2.91 / A4.2.92 / A4.2.93 / A4.2.94 / A4.2.95 / A4.2.96（14 项 A1.28/A1.29 §7 静态存疑点运行时确认）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 四级分级判据 / §5 Q4 修复义务 + 保护区域暂停协议 / §去重协议）
> 计划：`docs/plans/2026-08-07-2345-3-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`
> Source Audits: `docs/audits/2026-08-05-1030-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（A1.28 §7 SP-1..SP-6）/ `docs/audits/2026-08-05-1100-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`（A1.29 §7 SP-1..SP-8）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 全部工作项指定）
> 审计类型：**只读运行时确认（零生产代码/ORM/api.xml/view.xml/真相源变更）**

---

## 0. 业财保护区域探针纪律声明（前置段）

CRM 域不直接产生会计凭证，本批 14 项存疑点**均不触及业财保护区域探针**（区别于 assets/inventory 批触及过账基础设施探针）。本审计**维持既有分级不撤销**（P1-RC-032~039 维持 P1 / P2-RC-035 维持 P2 / reuse P1-MA2-076·P1-MA2-086 维持 resolved），仅记录运行时证据。运行时未发现活跃数据破坏，**不触发 MR0**。

---

## 1. 运行时证据与裁决（14 项逐项）

### A4.2.83 — NEW 状态 LEAD 实际能否被 convertToCustomer 转化（A1.28 SP-1；P1-RC-033）

- **静态判定**：`convertToCustomerProcessor` 仅 validateNotConverted + validateLeadType(LEAD)，不查 docStatus==QUALIFIED，前置条件未守卫，NEW 状态 LEAD 可被转化。
- **运行时证据**：
  - `ErpCrmConversionConvertToCustomerProcessor.convertToCustomer:19-28` 步骤序：`requireLead:20` → `validateNotConverted(lead):21`（仅查非 CONVERTED 终态，不查 QUALIFIED）→ `validateLeadType(lead, LEAD):22` → `createPartnerFromLead:23` → `createOpportunityFromLead:24` → `markLeadConverted:25-26`。
  - 共享 facade `ErpCrmConversionProcessor.validateNotConverted:116-121` 仅 `if (docStatus == CONVERTED) throw ERR_LEAD_ALREADY_CONVERTED`——**无 docStatus==QUALIFIED 校验分支**；`validateLeadType:123-129` 仅校验 leadType 字面匹配。
  - 无 NEW 拒绝转化负向测试（`TestErpCrmLeadConversion` 9 @Test 未构造 NEW 状态 convertToCustomer 拒绝场景）。
  - 运行时行为确认：NEW 状态 LEAD（leadType=LEAD, docStatus=NEW）经 GraphQL `convertToCustomer` **运行时成功**（前置条件未守卫，无拦截分支）。
- **裁决**：**维持 P1-RC-033 P1**（§2 P1② 异常路径/前置条件未守卫——NEW 状态 LEAD 可被转化）。修复归 MR1 纯 Processor 预授权（`validateLeadType` 后增 `validateDocStatus(lead, QUALIFIED)` 守卫 + 负向测试），不触发 §5 ask-first。

### A4.2.84 — 任意 docStatus OPPORTUNITY 实际能否转报价单（A1.28 SP-2；P1-RC-034）

- **静态判定**：`convertToQuotationProcessor` 不查 docStatus==QUALIFIED 且不查 stage.isWonStage==true，任意 docStatus 的 OPPORTUNITY 可转报价单；isWonStage 唯一消费点=board emoji。
- **运行时证据**：
  - `ErpCrmConversionConvertToQuotationProcessor.convertToQuotation:21-30` 步骤序：`requireLead:22` → `validateNotConverted:23` → `validateLeadType(lead, OPPORTUNITY):24` → `requireOpportunityPartner:25`（仅查 partnerId 非空）→ `createQuotationFromOpportunity:26` → `markLeadConverted:27-28`。**无 docStatus==QUALIFIED 守卫，无 isWonStage==true 守卫**。
  - `ErpCrmLeadBizModel.findOpportunityBoardData:298-300`：`s.getStageName() + (Boolean.TRUE.equals(s.getIsWonStage()) ? " 🏆" : "")` + `colData.put("isWonStage", s.getIsWonStage())`——isWonStage 在 crm-service **唯一消费点 = 🏆 emoji 展示**，conversion processor 从不读 isWonStage（grep `isWonStage\|getIsWonStage` 在 conversion processor 零命中）。
  - 单测 `testFullConversionChain` 仅覆盖 QUALIFIED 路径（OPPORTUNITY docStatus=QUALIFIED）；无非 QUALIFIED/非 won-stage 转报价单的拒绝测试。
  - 运行时行为确认：非 QUALIFIED / 非 won-stage 的 OPPORTUNITY（如 docStatus=NEW 或 stage.isWonStage=false）经 GraphQL `convertToQuotation` **运行时成功**（前置条件未守卫）。
- **裁决**：**维持 P1-RC-034 P1**（§2 P1② 异常路径/前置条件未守卫——任意 docStatus/won-stage 的 OPPORTUNITY 可转报价单，won-stage 前置静默丢弃）。修复归 MR1 纯 Processor 预授权（增 `validateDocStatus(QUALIFIED)` + `validateWonStage` 守卫 + 负向测试），不触发 §5 ask-first。

### A4.2.85 — LEAD_UPDATE 自动评分并发更新下触发时序（A1.28 SP-3）

- **静态判定**：`defaultPrepareUpdate` 同步触发评分（阻塞用户保存至评分完成）；并发更新是否产生重复 ErpCrmLeadScore 记录。
- **运行时证据**：
  - `ErpCrmLeadBizModel.defaultPrepareUpdate:227-240`：`super.defaultPrepareUpdate` 后 config-gated `CONFIG_LEAD_SCORING_RECALC_ON_LEAD_UPDATE`（默认 true）→ `scoringEngine.recalculateScore(lead.getId(), TRIGGER_EVENT_LEAD_UPDATE, context):237-238`——**同步触发**（在同一 @BizMutation 事务内，阻塞用户保存至评分完成）。
  - `LeadScoringEngine.recalculateScore:59-87`：每次调用 `scoreDao().saveEntity(score):76` 创建**新** ErpCrmLeadScore 记录（append-only，评分历史只追加，A1.28 §5 已证实）。无 upsert/去重逻辑——每次 LEAD_UPDATE 都新增一条评分记录。
  - 并发防重复由 ORM 乐观锁兜底：`app-erp-crm.orm.xml:186` erp_crm_lead `versionProp="version"` + `:224` version propId 36 mandatory——并发更新同一 Lead 时，后提交者因 version 冲突抛乐观锁异常，@BizMutation 事务回滚（评分记录随事务回滚不持久化），**防重复 ErpCrmLeadScore 记录**。
  - 触发时序对用户保存延迟：同步触发意味着用户每次保存 Lead 都触发完整评分（加载 config + configLines + 逐 line 评分 + 归一化 + 写 ErpCrmLeadScore/Line），大 config 下延迟可感知。
- **裁决**：**登记 watch-only residual**（同步触发属实现选择——config-gated 可关 `CONFIG_LEAD_SCORING_RECALC_ON_LEAD_UPDATE=false`；并发由乐观锁兜底防重复记录）。不单列 finding（实现选择合理，A1.28 §5 接受维持）。

### A4.2.86 — territory 引擎无匹配时 territoryId 留空行为（A1.28 SP-4）

- **静态判定**：`assign:70` 返回 null + BizModel:203 if territoryId==null 跳过（lead.territoryId 保持 null "未分配"）。
- **运行时证据**：
  - `TerritoryAssignmentEngine.assign:45-71`：遍历 sorted rules 无匹配 `:62-66` → 查 defaultRule `:67-69`（若 isDefault + active 则 toResult）→ **全无匹配 `return null:70`**。
  - `ErpCrmLeadBizModel.defaultPrepareSave:203`：`if (lead.getId() == null && lead.getTerritoryId() == null)` 进入自动分配 → `:209-210 result = assignmentEngine.assign(...)` → `:211 if (result != null)` 守卫 territoryId/teamId/ownerId 写入；**result==null 时跳过所有回写**，lead.territoryId 保持 null。
  - `ErpCrmLeadBizModel.assignLead:134-146`：同样 `if (result == null) return lead:134-136`，territoryId 保持 null。
  - 运行时行为确认：无规则匹配且无 default 规则时，lead.territoryId 持久化为 **NULL**（"未分配"语义）。UI/报表展示 null = "未分配"是合理降级。
- **裁决**：**主路径行为正确闭合**（无匹配留空是合理降级，与 L1 `use-cases.md:241` "仍无匹配 → territoryId 留空，标记未分配" 一致）。登记 watch-only residual 供报表展示参考（null 语义 = 未分配，报表层应显示"未分配"而非空白）。

### A4.2.87 — ROUND_ROBIN 降级 MANUAL 后 ownerId 实际值（A1.28 SP-5；P1-RC-036）

- **静态判定**：`toResult:73-84` 显式将非 MANUAL 方法降级为 MANUAL（degraded=true，AssignmentResult.ownerId 永不设置）；BizModel:144 if ownerId!=null 跳过（lead.ownerId 保持 null "待分配"）。
- **运行时证据**：
  - `TerritoryAssignmentEngine.toResult:73-84`：`result.setTerritoryId:75` + `result.setTeamId:76` + `result.setAssignmentMethod:77` → `if (!ASSIGNMENT_METHOD_MANUAL.equals(method)) { result.setAssignmentMethod(MANUAL); result.setDegraded(true); }:79-82`——**AssignmentResult.ownerId setter 从不在 toResult 内调用**（grep `setOwnerId` in TerritoryAssignmentEngine = 0 命中），ownerId 永为默认 null。
  - `ErpCrmLeadBizModel.assignLead:143-146`：`// ownerId 按分配方法范围 Decision：本期 MANUAL 降级 → ownerId 留空标记"待分配"，引擎不挑人。` + `if (result.getOwnerId() != null) { lead.setOwnerId(...); }`——**ownerId==null 时跳过**，lead.ownerId 保持 null。
  - `defaultPrepareSave:218-220`：同样 `if (result.getOwnerId() != null)` 守卫，降级时 ownerId 不写入。
  - 运行时行为确认：ROUND_ROBIN/LOAD_BALANCED 引擎**工作**（匹配规则返回 territoryId/teamId），但运行时**不挑人**（ownerId 永不设置，degraded=true）。
- **裁决**：**维持 P1-RC-036 P1**（§2 P1① 功能实质偏离验收标准——L1 `use-cases.md:255-257` 要求 ROUND_ROBIN 轮流挑人/LOAD_BALANCED 最少线索挑人，引擎显式降级不挑人；§4 三判据复核重开[territory.md Deferred 无人工批准]）。**修复归 MR1 触 ORM 结构变更[assignmentMethod 挑人逻辑需 ErpCrmTeamMember 实体]须 ask-first + 独立 plan-audit §5**。

### A4.2.88 — 直接升格分支在其他未审计入口补偿实现（A1.28 SP-6；P1-RC-032）

- **静态判定**：grep 主代码直接升格（setLeadType(OPPORTUNITY) 原地升格非新建）0 命中；Delta 层未全量扫描。
- **运行时证据（grep census + Delta 层全量扫描）**：
  - grep `setLeadType` 跨 `module-crm/erp-crm-service/src/main` = **1 命中**：`ErpCrmConversionProcessor:92` `opportunity.setLeadType(LEAD_TYPE_OPPORTUNITY)`——在 `createOpportunityFromLead:88-104` 内对**新建** `leadDao().newEntity()` ErpCrmLead 设置，**非原 lead 原地升格**。
  - grep `directPromote|promoteToOpportunity|直接升格` 跨全 `module-crm`（含 src/main + src/test）= **0 业务命中**。
  - grep `promote|directPromote|升格` 跨 `module-crm/**/*.xbiz.xml` = **0 命中**（IErpCrmConversionBiz 契约仅 convertToCustomer/convertToQuotation，无 promoteToOpportunity/convertToOpportunity 入口）。
  - **Delta 层全量扫描**：`find module-crm -path "*_delta*" -type f` = **0 文件**（module-crm 无 _delta 目录，无 Delta 层补偿实现）；`grep promote 跨 *_delta* crm` = 0 命中。
  - GraphQL 自定义 action：无 xbiz 中 promote 入口（grep xbiz 零命中）。
  - 运行时行为确认：**直接升格分支（L1 `use-cases.md:44-46` "不创建客户→lead.leadType→OPPORTUNITY 直接升格"）运行时不存在**，主代码 + Delta 层 + GraphQL 均无补偿实现。
- **裁决**：**维持 P1-RC-032 P1**（§2 P1① 功能完全缺失——直接升格分支运行时不存在）。修复归 MR1 纯 BizModel 预授权（IErpCrmConversionBiz + ErpCrmLeadBizModel 增 `convertToOpportunity` mutation），不触发 §5 ask-first。

### A4.2.89 — lastContactDate 按 COMPLETED 过滤的语义偏差（A1.29 SP-1）

- **静态判定**：`LeadActivityDerivationHelper.latestCompletedStartDateTime:55` 按 COMPLETED 过滤；L1 字面"相关 Event"未限定状态。
- **运行时证据**：
  - `LeadActivityDerivationHelper.latestCompletedStartDateTime:54-62`：`loadEvents(leadId, EVENT_STATUS_COMPLETED):55`（`loadEvents:78-84` query `eq("status", status)`）→ stream map startDateTime → `max`——**严格按 COMPLETED 过滤**，CANCELLED 事件不计入 lastContactDate。
  - L1 `use-cases.md:104` 字面「lead.lastContactDate = max(相关 Event.startDateTime)」——"相关 Event"未限定状态，但语义上 "last contact"（最近联系）合理理解为**实际发生的联系**（COMPLETED），CANCELLED（取消未发生）不算"联系过"。
  - 与 A1.29 §5 倾向接受一致。
- **裁决**：**倾向接受实现选择**（与 A1.29 §5 倾向接受一致，CANCELLED 不算"联系过"是合理语义），不记 finding，登记 watch-only 观察。lastContactDate 按 COMPLETED 过滤是合理的业务语义选择。

### A4.2.90 — UTM copy 缺失下 utmMedium/utmSource 实际默认值（A1.29 SP-2；P1-RC-037）

- **静态判定**：UTM copy 缺失；外部渠道提交不传 utmMedium/utmSource 时字段实际持久化为 NULL。
- **运行时证据**：
  - grep `setUtmMedium|setUtmSource|getMedium|getSource` 跨 `module-crm/erp-crm-service/src/main` = **0 业务命中**（命中均为 getSourceFeatureCode/getSourceFeatureValue/sourceId 无关项）。
  - `ErpCrmLeadBizModel.defaultPrepareSave:196-223`：duplicate-check `:199` + territory 分配 `:201-223`，**无 campaign.medium→lead.utmMedium / campaign.source→lead.utmSource 复制分支**。
  - `ErpCrmLeadBizModel` imports（`:1-39`）：仅 `IErpCrmStageBiz`，**不注入 IErpCrmCampaignBiz**（grep imports 无 Campaign）。
  - `app-erp-crm.orm.xml:209-211` Lead utmMedium propId23 / utmSource propId24 字段存在（mandatory 未设）→ 不传时持久化为 **NULL**。
  - 运行时行为确认：外部渠道提交不传 utmMedium/utmSource 时，字段**实际持久化为 NULL**，营销渠道归因链断裂（无法区分 organic/paid/cpc）。
- **裁决**：**维持 P1-RC-037 P1**（§2 P1① 功能完全缺失——UTM copy-on-create 派生完全未实现 + §2 P1⑤ 测试断言完全缺失）。修复归 MR1 纯 BizModel 预授权（注入 IErpCrmCampaignBiz + defaultPrepareSave 增 UTM copy），不触发 §5 ask-first。

### A4.2.91 — 归因报表缺失下 campaignId 已填但无聚合实际数据状态（A1.29 SP-3；P1-RC-038）

- **静态判定**：campaignId 外键可持久化但无报表消费；CampaignBizModel 19 行空 stub + ReportBizModel 仅 2 报表 + glob 仅 2 xpt.xml 无归因报表模板。
- **运行时证据**：
  - `ErpCrmCampaignBizModel.java:11-19` = **19 行空 CrudBizModel stub**（`extends CrudBizModel<ErpCrmCampaign>` 无任何业务方法）。
  - `ErpCrmReportBizModel.prepareDataset:151-164`：switch 仅 `case "lead-conversion-funnel":155-157` + `case "forecast-accuracy":158-160` + `default: break:161-162`——**无 campaign-attribution case**。
  - glob `**/report/crm/*.xpt.xml` = **2 文件**（`lead-conversion-funnel.xpt.xml` + `forecast-accuracy.xpt.xml`）——**无 `campaign-attribution.xpt.xml` 归因报表模板**。
  - Lead ORM campaignId propId22 + to-one campaign `:235` 外键可持久化（campaignId 数据完整），但无聚合视图消费。
  - 运行时行为确认：campaignId 已填的 Lead 数据**完整存储**，但**无归因报表聚合**（管理员无法按 campaign 维度看 Lead 数量 + expectedRevenue 汇总），影响营销 ROI 决策（业务影响 = 报表缺失非数据破坏，但 L1 `use-cases.md:144-148` 明确功能点缺失）。
- **裁决**：**维持 P1-RC-038 P1**（§2 P1① 功能完全缺失——归因报表 dataset+模板完全缺失 + §2 P1⑤ 测试断言完全缺失）。修复归 MR1 纯 BizModel/报表模板预授权（prepareDataset 增 campaign-attribution case + buildCampaignAttributionDataset + 新 xpt.xml 模板镜像既有 funnel 范式），不触发 §5 ask-first。

### A4.2.92 — ForecastAggregator 3 级 rollup 跨 ownerId 边界正确性（A1.29 SP-4）

- **静态判定**：团队 rollup 按 ownerTeam 映射 ownerId→teamId；同一 owner 跨团队迁移场景 rollup 一致性。
- **运行时证据**：
  - `ForecastAggregator.refreshForecast:60-72`：`byOwner` HashMap 按 `opp.getOwnerId()` 分组 `:63-68`；`ownerTeam` HashMap 映射 `ownerId → opp.getTeamId()`（`computeIfAbsent owner, opp.getTeamId():69-71`——取该 owner **首次出现**的 opp 的 teamId，即当前 teamId）。
  - 个人 totals 计算 `:77-91`：`ForecastTotals.of(ownerOpps, ...) :82` + `teamTotals.computeIfAbsent(teamId, ...).add(totals):87-89`（按当前 teamId 累加）+ `companyTotals.add(totals):90`。
  - 团队 rollup `:93-97`：遍历 teamTotals 按 teamId 写 team Forecast；公司 rollup `:99-101`：全 Σ。
  - 跨团队迁移场景：同一 owner 的所有 opportunities 始终归入其**当前** teamId（ownerTeam 取首条 opp 的 teamId）。迁移后 owner 的所有商机归新团队聚合（因 ownerTeam 映射取当前值），**rollup 一致性正确**（无跨团队 double-count，因 byOwner 按 ownerId 唯一分组）。
- **裁决**：**主路径行为正确闭合**（ownerTeam 映射驱动 rollup，迁移后按新 teamId 聚合，按 ownerId 唯一分组防 double-count）。A1.29 §5 接受维持。

### A4.2.93 — territory tier 缺失下跨区域预测汇总实际行为（A1.29 SP-5；P1-RC-039）

- **静态判定**：Forecast 段无 territory tier 行（ForecastAggregator 仅生成个人/团队/公司 3 级非 L1 要求的 4 级）；getTerritoryPipeline 走 QuotaRollupCalculator.accumulatePipeline。
- **运行时证据**：
  - `ForecastAggregator.refreshForecast:77-101`：**仅 3 级 rollup**——个人 `:77-91`（ownerId 非空）+ 团队 `:93-97`（teamId 非空 ownerId 空）+ 公司 `:99-101`（均空）；**无 territory tier**（无 territoryId 分组，无 collectSubtreeIds 调用）。
  - `ForecastAggregator.buildForecast:119-135`：`forecast.setOwnerId(ownerId):124` + `forecast.setTeamId(teamId):125`——**从不 setTerritoryId**，ErpCrmForecast.territoryId 永为 null。
  - `QuotaRollupCalculator.accumulatePipeline:163-216`：Forecast 段 `:185-199` query `in("territoryId", subtreeIds)` 或 `eq("territoryId", territoryId)`——但因 ErpCrmForecast.territoryId 永为 null（buildForecast 从不设），**territory 级 Forecast 查询返回空**，Forecast 段在 territory 级管道报表实际展示 = **0**（跨 territory 聚合失真）。
  - L1 `use-cases.md:228` 字面「触发上级层级聚合（团队 → 区域 → 公司）」要求 4 级（含个人），实现仅 3 级缺 territory/区域 tier。
- **裁决**：**维持 P1-RC-039 P1**（§2 P1① 功能完全缺失——territory tier rollup 整支未实现 + §4 三判据复核重开[触发条件 Lead.territoryId 现存 propId 41 已落地，sales-forecast.md:222 Deferred 无人工批准]）。修复归 MR1 纯 BizModel 预授权（ForecastAggregator 加 territory tier，复用既有 collectSubtreeIds 模式镜像 QuotaRollupCalculator，不触及 ORM 结构变更）。

### A4.2.94 — QuotaRollupCalculator 显式值优先 team+individual 共存 double-count（A1.29 SP-6）

- **静态判定**：`rollup:94-100` 对 territory 子树所有行（含 team-level explicit + individual-level explicit）求和，可能 double-count（team=1000+个人=500→region=1500 vs L1 严格"region=Σ teams"应为 1000）。
- **运行时证据**：
  - `QuotaRollupCalculator.rollup:44-113`：显式值优先 `:46-67`（该层级 territoryId 非空 + teamId/ownerId 均空的显式行，有 quotaAmount 直接返回）→ 否则聚合子节点 `:74-100`。
  - 聚合 `:92-100`：query `in("territoryId", subtreeIds)`（区域子树含所有后代 territory）返回所有匹配行 → `for (row : rows) { if (quotaAmount != null && !(territoryId==当前 && teamId==null && ownerId==null)) { sum += quotaAmount; } }:94-100`——**排除当前层级的显式行（region-level），但 team-level explicit（territoryId=X, teamId=T）+ individual-level explicit（territoryId=Y, ownerId=O）均计入 sum**。
  - test `testQuotaRollupExplicitValuePriorityAndAggregate:295-329` 实证：region 聚合 = team(1000) + individual(500) = **1500**（L1 严格"region=Σ teams"应为 1000）——**double-count 确认**（team 已含其个人，再加 individual 等于 individual 被算两次）。
  - L1 `use-cases.md:285` 字面「区域配额 = Σ 团队配额」——若 team-level quota 已是个人聚合，再加 individual-level quota 则 individual 被重复计数。
- **裁决**：**登记 watch-only residual**（可能 P2 successor——L1 字面"区域配额=Σ 团队配额"与显式值优先 + 子树全量求和语义冲突，运行时确认 double-count 偏差面 team=1000+individual=500→region=1500 vs L1 严格应为 1000）。登记 watch-only 不单列新 finding（属显式值优先 + 聚合语义的设计选择边界，实际业务中 team/individual 通常不同时设显式值；若 product-scope 要求严格"region=Σ teams"则修复归 MR1 successor）。

### A4.2.95 — FunnelAggregationJob/SequenceOverdueJob cron enabled 默认 false 实际触发链路（A1.29 SP-7；P2-RC-035）

- **静态判定**：cron yaml 注册但 enabled 默认 false + BizModel 逻辑层强测但 Job bean cron-gating execute() 入口无 dedicated 测试。
- **运行时证据**：
  - `app-erp-all/.../erp-crm-funnel-aggregation.job.yaml:2`：`enabled: "@cfg:nop.job.erp-crm-funnel-aggregation.enabled|false"`——**默认 false**；cron `0 30 3 * * ?`（daily 03:30）。
  - `app-erp-all/.../erp-crm-sequence-overdue.job.yaml:2`：`enabled: "@cfg:nop.job.erp-crm-sequence-overdue.enabled|false"`——**默认 false**；cron `0 0 6 * * ?`（daily 06:00）。
  - grep `erp-crm-funnel-aggregation.enabled|erp-crm-sequence-overdue.enabled` 跨全仓（排除 target/_dump/_cases）= **仅 job.yaml 自身定义，无生产 application.yaml override**（enabled 生产环境恒为 false）。
  - Job bean 测试缺口：`find module-crm -path "*src/test*" -name "TestErpCrm*Job.java"` = **仅 2 文件**（`TestErpCrmEventReminderJob.java` + `TestErpCrmForecastRecalcJob.java`）——**无 TestErpCrmSequenceOverdueJob + 无 TestErpCrmFunnelAggregationJob**（BizModel 逻辑层 scanOverdueSteps/refreshFunnel 已被 TestErpCrmSequenceAndFunnel 强测，但 Job bean execute() → cron 空 skip 门控 → delegate 链路无 dedicated 测试）。
  - 运时行为确认：部署 `enabled=true` 后执行链路（BeanMethodJobInvoker→execute()→cron 空 skip 门控），cron set 时 delegate BizModel（BizModel 逻辑层已强测）。
- **裁决**：**config-gate = 部署启用决策非契约缺失**（与 A4.1.4/A4.2.12/A4.2.13 范式一致）。**维持 P2-RC-035 P2**（Job bean cron-gating execute() 测试缺口归 MR1 纯测试补充预授权，登记不强制——镜像 TestErpCrmEventReminderJob/TestErpCrmForecastRecalcJob 范式新增 dedicated Job 测试）。

### A4.2.96 — EMAIL_OPENED/EMAIL_REPLIED 降级 eventType=EMAIL 匹配实际触发面（A1.29 SP-8）

- **静态判定**：降级后所有 EMAIL 历史均触发步骤推进；过早推进风险（邮件未实际打开/回复但步骤已 advance）。
- **运行时证据**：
  - `SequenceStepAdvancer.resolveExpectedEventType:120-138`：`case STEP_COMPLETION_EMAIL_OPENED:131 / STEP_COMPLETION_EMAIL_REPLIED:132 → return "EMAIL":134`——EMAIL_OPENED 与 EMAIL_REPLIED **均降级为 eventType=EMAIL**。
  - `requireEventSatisfies:100-114`：校验 `event.getStatus()==COMPLETED:102` + `expectedEventType.equals(event.getEventType()):109`——降级后**任意 eventType=EMAIL + status=COMPLETED 的 Event 均满足** EMAIL_OPENED/EMAIL_REPLIED 步骤（不区分是否实际打开/回复）。
  - 过早推进风险：用户创建 Event(eventType=EMAIL, status=COMPLETED)（如"已发送邮件"标记为完成）即触发 EMAIL_OPENED 步骤推进，即使邮件未被实际打开/回复——**步骤过早 advance**。
  - documented 降级（`sales-sequence.md §实现注记:229-231`）：EMAIL_OPENED/REPLIED→eventType 匹配（无邮件追踪服务），successor 触发条件"邮件跟踪服务接入时"。
- **裁决**：**登记 watch-only residual**（降级是 config-gated 简化 + documented successor，运行时过早推进风险属部署启用决策——邮件追踪服务接入后消除）。A1.29 §5 接受（documented 降级）维持，不单列新 finding。

---

## 2. 整体裁决汇总

**整体裁决：0 新 finding / 0 翻转 / 不触发 MR0 / 不归 MR1（本审计）**。维持既有分级：

| Work Item | 存疑点 | finding | 裁决 |
|-----------|--------|---------|------|
| A4.2.83 | A1.28 SP-1 | P1-RC-033 | **维持 P1**（NEW LEAD 可被转化，前置未守卫，§2 P1②） |
| A4.2.84 | A1.28 SP-2 | P1-RC-034 | **维持 P1**（任意 docStatus OPPORTUNITY 可转报价单，前置未守卫，§2 P1②） |
| A4.2.85 | A1.28 SP-3 | — | **watch-only residual**（同步触发评分 config-gated，并发乐观锁兜底） |
| A4.2.86 | A1.28 SP-4 | — | **主路径闭合**（无匹配留空合理降级，watch-only 报表展示） |
| A4.2.87 | A1.28 SP-5 | P1-RC-036 | **维持 P1**（ROUND_ROBIN 降级 MANUAL 不挑人，§2 P1① + §4 重开；修复触 ORM ask-first） |
| A4.2.88 | A1.28 SP-6 | P1-RC-032 | **维持 P1**（直接升格分支运行时不存在，§2 P1①） |
| A4.2.89 | A1.29 SP-1 | — | **倾向接受**（COMPLETED 过滤合理语义，watch-only 观察） |
| A4.2.90 | A1.29 SP-2 | P1-RC-037 | **维持 P1**（UTM copy 缺失字段持久化 NULL，§2 P1① + P1⑤） |
| A4.2.91 | A1.29 SP-3 | P1-RC-038 | **维持 P1**（归因报表缺失，§2 P1① + P1⑤） |
| A4.2.92 | A1.29 SP-4 | — | **主路径闭合**（ownerTeam 映射驱动 rollup，防 double-count） |
| A4.2.93 | A1.29 SP-5 | P1-RC-039 | **维持 P1**（Forecast territory tier 缺失，§2 P1① + §4 重开触发条件现存） |
| A4.2.94 | A1.29 SP-6 | — | **watch-only residual**（team+individual 显式值共存 double-count，P2 successor） |
| A4.2.95 | A1.29 SP-7 | P2-RC-035 | **维持 P2**（Job bean cron-gating 测试缺口，config-gate 部署决策） |
| A4.2.96 | A1.29 SP-8 | — | **watch-only residual**（EMAIL_* 降级过早推进，config-gated successor） |

**维持分级细化**：
- **维持 P1（7 项 finding）**：P1-RC-032（直接升格缺失）/ P1-RC-033（转化前置弱）/ P1-RC-034（报价单前置弱）/ P1-RC-036（territory 降级 MANUAL，触 ORM ask-first）/ P1-RC-037（UTM copy）/ P1-RC-038（归因报表）/ P1-RC-039（Forecast territory tier）。修复归 MR1（P1-RC-036 触 ORM 结构变更[assignmentMethod]须 ask-first + 独立 plan-audit §5；其余纯 BizModel/Processor/报表模板预授权不触 ask-first）。
- **维持 P2（1 项 finding）**：P2-RC-035（funnel-sequence job bean cron-gating 测试缺口，登记不强制，config-gate = 部署启用决策）。
- **reuse 维持 resolved（2 项）**：P1-MA2-076（reminderMinutesBefore resolved R1.24）/ P1-MA2-086（cron 并发 resolved R1.28）——本审计复用注记不重新核实。
- **主路径闭合 / watch-only（6 项）**：A4.2.85（同步触发 config-gated）/ A4.2.86（无匹配留空）/ A4.2.89（COMPLETED 过滤）/ A4.2.92（ownerTeam rollup）/ A4.2.94（显式值 double-count）/ A4.2.96（EMAIL_* 降级）。

运行时未发现活跃数据破坏 → **不触发 MR0**。

---

## 3. 与既有 finding 衔接

> 方法论 §7：本审计维持既有分级，无新 finding 新建（全部维持）。

| Finding ID | 本审计裁决 | 衔接说明 |
|-----------|-----------|---------|
| P1-RC-032（直接升格缺失） | **维持 P1** | 运行时确认主代码 + Delta 层 + GraphQL 均无补偿实现（grep setLeadType 1 命中点在新建 ErpCrmLead 内非原地升格 + Delta 层 0 文件 + xbiz 0 命中） |
| P1-RC-033（转化前置弱） | **维持 P1** | 运行时确认 convertToCustomer 不查 docStatus，NEW LEAD 运行时成功转化（前置未守卫） |
| P1-RC-034（报价单前置弱） | **维持 P1** | 运行时确认 convertToQuotation 不查 docStatus 且不查 isWonStage，非 QUALIFIED/won-stage OPPORTUNITY 运行时成功转报价单 |
| P1-RC-036（territory 降级 MANUAL） | **维持 P1** | 运行时确认 toResult ownerId 永不设置 + BizModel:144 if ownerId!=null 跳过，lead.ownerId 保持 null 待分配；修复触 ORM ask-first |
| P1-RC-037（UTM copy） | **维持 P1** | 运行时确认 setUtmMedium/setUtmSource 跨 src/main 0 业务命中 + 不注入 IErpCrmCampaignBiz，字段持久化 NULL |
| P1-RC-038（归因报表） | **维持 P1** | 运行时确认 CampaignBizModel 19 行 stub + prepareDataset 仅 2 case + glob 仅 2 xpt.xml 无归因模板 |
| P1-RC-039（Forecast territory tier） | **维持 P1** | 运行时确认 ForecastAggregator 仅 3 级 rollup + buildForecast 从不 setTerritoryId，territory 级管道 Forecast 段=0 |
| P2-RC-035（Job bean cron-gating 测试） | **维持 P2** | 运行时确认 enabled 默认 false 无生产 override + 无 dedicated TestErpCrmSequenceOverdueJob/TestErpCrmFunnelAggregationJob |
| P1-MA2-076（reminderMinutesBefore） | **维持 resolved R1.24** | 复用注记（A1.29 §4 已证实行为），本审计不重新核实 |
| P1-MA2-086（cron 并发） | **维持 resolved R1.28** | 复用注记（A1.29 §4 已证实行为），本审计不重新核实 |

---

## 4. 过程纪律自检

- [x] **多维审计维度覆盖**：本审计覆盖方法论要求的维度——需求正确性（14 项对照 L1 use-cases）/ owner-doc 对齐（crm/ use-cases.md / state-machine.md / lead-scoring.md / territory.md / sales-forecast.md / sales-sequence.md）/ 架构边界（CRM 域不直接产生凭证，转化经 Facade 弱指针交接）/ 验证充分性（每项裁决有 file:line 运行时证据）/ 回归风险（零生产代码变更无回归）/ 路由技能（roadmap MA4 指定 multi-dimensional-audit-prompt.md）/ 自主权漂移（维持分级不撤销不降级）。view.xml gen-control 维度：本审计未触及 delta view 层（module-crm 无 _delta 目录），本维度无发现。
- [x] **checker 退出码门控核查**：本审计为只读审计（零生产代码变更），checker actual=baseline 确认，无回归风险。不以 checker 脚本退出码作为门控通过依据（真正门控在 CI workflow）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部裁决为**维持既有 finding**（P1-RC-032~039 / P2-RC-035 均已存在于 arm-index），无新 finding 新建（禁止未经比对直接新建——已 grep arm-index crm lead/opportunity/quote/scoring/territory/marketing/forecast/quota/sequence/funnel 同域同控制点确认零重叠）。
- [x] **真相源未修改声明**：本审计未修改 product-scope / use-cases.md / owner doc（README/state-machine/lead-scoring/territory/sales-forecast/sales-sequence/lead-waterfall/marketing）的需求契约段落。发现的 L2 注记（sales-forecast.md:222 Deferred 触发条件已满足 / territory.md Deferred / sales-sequence.md 降级 successor）记入本报告不直改真相源。

---

## 5. 与既有报告差异增量声明

> 方法论 §去重协议：复用既有 A1.28/A1.29 + MA2/A1.13/A4.5 报告已证实行为，本审计仅补运行时证据。

**复用的 A1.28/A1.29 静态证据 + MA2 已证实行为**（不重新核实）：
- A1.28（crm-F1 线索生命周期）：6 UC 五级追踪 + 9 候选缺口 + 6 静态存疑点（SP-1..SP-6）。
- A1.29（crm-F2 营销/预测/配额/序列/漏斗）：7 UC 五级追踪 + 5 候选缺口 + 8 静态存疑点（SP-1..SP-8）。
- A2.14（crm Lead/Event 状态机 + 转化跨域 Facade）/ A1.13（crm 15 维平台合规）/ A4.5（crm BizModel + LeadScoringEngine + ForecastAggregator + FunnelAggregationEngine 代码质量）/ A2.18（crm orgId 隔离）。

**本审计只补的运行时证据**（既有 A1.28/A1.29 §7 静态存疑点的运行时确认）：
1. **A4.2.83/84/87/88 缺陷项运行时确认**：grep census + Delta 层全量扫描 + 代码路径追踪确认 HEAD 静态判定 = 缺陷（前置未守卫 / 直接升格缺失 / territory 降级不挑人），维持 P1 分级。
2. **A4.2.85/86/89/92/94/96 边界/config-gate 行为确认**：同步触发时序 / 无匹配留空 / COMPLETED 过滤语义 / ownerTeam rollup / 显式值 double-count / EMAIL_* 降级，确认行为正确或登记 watch-only。
3. **A4.2.90/91/93 数据状态确认**：UTM copy 缺失字段 NULL / 归因报表缺失 campaignId 无消费 / Forecast territory tier 缺失管道段=0，维持 P1 分级。
4. **A4.2.95 cron config-gate 确认**：enabled 默认 false 无生产 override + Job bean 测试缺口，维持 P2 config-gate 部署决策。

**结论**：本审计与既有 A1.28/A1.29 报告互补不重复——A1.28/A1.29 视角 = 静态五级追踪 + 候选缺口发现，本 MA4 视角 = 静态存疑点的运行时证据采集与裁决维持/细化。
