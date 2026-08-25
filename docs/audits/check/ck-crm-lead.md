# ck-crm-lead — crm「线索与商机」切片实现代码检查报告

> 工作项：C6.3。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-crm/erp-crm-service/src/main/java` 线索与商机切片 26 个手写生产文件——线索生命周期（`entity/ErpCrmLeadBizModel` 455 行含 assignLead/reassignLead/defaultPrepareSave/Update/findOpportunityBoardData + `processor/ErpCrmLeadProcessor` 255 行共享 helper + 4 个 per-mutation Processor[Qualify/Lose/Cancel/MoveStage] + `statemachine/ErpCrmLeadStateMachine`）、评分链（`entity/ErpCrmLeadScoreBizModel` + `processor/ErpCrmLeadScoreRecalculateScoreProcessor` + `support/LeadScoringEngine` 417 行 + `job/ErpCrmLeadScoringRecalcHelper`）、转化链（`processor/ErpCrmConversionProcessor` 232 行 + 3 个 Convert processor[ToCustomer/ToQuotation/ToOpportunity]）、区域链（`support/TerritoryAssignmentEngine` 381 行 + `entity/ErpCrmTerritoryBizModel` + `processor/ErpCrmTerritoryCreateChildProcessor`/`MoveTerritoryProcessor` + `entity/ErpCrmQuotaBizModel` + `processor/ErpCrmQuotaDistributeAnnualQuotaProcessor` + `support/QuotaRollupCalculator` 276 行）、漏斗链（`support/FunnelAggregationEngine` 459 行 + `processor/ErpCrmLeadFunnelRefreshFunnelProcessor` + `entity/ErpCrmLeadFunnelBizModel`/`ErpCrmFunnelStageMetricsBizModel` + `job/ErpCrmFunnelAggregationJob`）、支撑（`support/LeadDuplicateChecker`/`LeadActivityDerivationHelper`/`EventTimelineAggregator` + `entity/ErpCrmEventBizModel` + `processor/ErpCrmEventComplete/CancelProcessor` + `statemachine/ErpCrmEventStateMachine`）+ 根常量相关段。跨文件核实：`module-crm/model/app-erp-crm.orm.xml`（versionProp/UK/dict/列集）、`_vfs/erp/crm/beans/app-service.beans.xml`、`_vfs/nop/batch-task/crm/lead-scoring-recalc.batch.xml`、`app-erp-all/_vfs/nop/job/conf/erp-crm-{funnel-aggregation,lead-scoring-recalc}.job.yaml`、5 实体 `*.xbiz.xml` override、`erp-crm-meta` `lead-doc-status.dict.yaml`、`erp-crm-web` `opportunity-kanban.flux.yaml` 消费面。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。切片文件逐行深读 + 平台源码实证 2 处（`CrudBizModel.doUpdate` L836-860——update 流程 requireEntity 加载 attach 实体 → copyToEntity 先于 defaultPrepareUpdate；`CrudBizModel.get(id, ignoreUnknown, ctx)`——第二参是 ignoreUnknown 非锁）+ orm/beans/batch/job-yaml/xbiz/dict 接线核对 + arm-index 复用裁决（RC 系列 crm 三切片 9 finding + MA4 crm 3 + MA1/MA2 crm 3 逐条裁决）。
> 切片边界：CPQ/营销/预测（ForecastAggregator/ForecastRecalcJob）/销售序列（Sequence*）归 C6.4 勿重复；`ErpCrmReportBizModel` 报表渲染（归 C6.4 marketing attribution 已修复面）；web AMIS 页面契约 drift 归 C8.2。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-crm-001（D6/D8）getTerritoryPipeline 管道三段聚合口径混乱——公司级实际/预测段只计 territoryId IS NULL 的记录 + 实际段无 leadType 过滤（convertToCustomer 链同笔生意双计）+ 实际/预测段无期间过滤

- **控制点**：`app/erp/crm/service/support/QuotaRollupCalculator.java#accumulatePipeline`（L201-214 实际段：`actualQuery.addFilter(eq("docStatus", CONVERTED))` + `if (territoryId == null) actualQuery.addFilter(isNull("territoryId"))`——注释 L201 自称「聚合 territoryId 子树内已 CONVERTED **商机** expectedRevenue」但 query **无 leadType=OPPORTUNITY 过滤**；L185-199 预测段：`forecastQuery` **无 periodLabel filter**；实际段同样无期间过滤）
- **证据**：三个独立子症状：① **公司级口径错误**——`getTerritoryPipeline(territoryId=null, periodLabel)` 是「目标/预测/实际三段同屏」（owner doc territory.md §实现注记 4）的公司级入口，但 quota 段 rollup(null) 聚合**全部**配额行（L75-76 注释「所有配额行都参与聚合（不限制 territoryId）」），而 forecast 段（L186-187）与 actual 段（L204-205）在 territoryId==null 时只统计 `isNull("territoryId")` 的行——绝大多数商机经分配引擎持有非空 territoryId，公司级管道的预测/实际段系统性接近零，与配额段（全部）三段口径互斥，对比视图完全失真；② **同笔生意双计**——convertToCustomer 链产生两条 CONVERTED 记录（原 LEAD `markLeadConverted` 后 docStatus=CONVERTED 且 expectedRevenue 保留 + 新建 OPPORTUNITY lead 经 `createOpportunityFromLead` L111 `ifPresent(opportunity::setExpectedRevenue, lead.getExpectedRevenue())` 复制同额，其后续 convertToQuotation 再置 CONVERTED），actual 段按 docStatus=CONVERTED 无 leadType 过滤把两条都计入 → `actualRevenue`/`convertedCount` 双倍（公司级 isNull 过滤下原 lead 有 territoryId 计入、新商机 territoryId=null 也计入——两条都命中）；③ **期间口径不对称**——periodLabel 只过滤 quota 段（L167-173 rollup 传 periodLabel），forecast 段（ErpCrmForecast 按期间多行）与 actual 段（CONVERTED 任意时间）全历史累计 vs 当期配额对比。
- **问题**：区域管道对比（territory.md §业务规则 3「实际 / 预测 / 目标同屏」）的核心金额数字在常见配置下不可信。D6 金额汇总正确性 + D8 口径一致性。
- **建议修复方向**：① 公司级（territoryId=null）三段统一为「全部行」（或三段统一为「子树+未分配」并写明语义）；② actual 段补 `eq("leadType", OPPORTUNITY)`（对齐自身注释「已 CONVERTED 商机」）；③ forecast/actual 段补期间过滤（ErpCrmForecast 按 periodLabel；actual 段需 Lead 侧转化时间来源——relatedBill 写入时刻无审计列，可近似用 updateTime 或补 convLog 见 P1-CK-crm-002 修复方向联动）。
- **arm-index 裁决**：新增（grep arm-index「getTerritoryPipeline/accumulatePipeline」仅命中 P1-RC-039[ForecastAggregator territory tier 缺失，已修复 RC-R1.25，不同控制点——那是 Forecast 引擎，本条是 QuotaRollupCalculator 管道对比]；「双计」crm 相关仅 P2-MA4-013(a)[Forecast TOCTOU]——不同控制点）。

### P1-CK-crm-002（D6/D8）漏斗聚合 won/lost 期间口径系统性失真——doLose/markLeadConverted 不写 ConvLog，期间内唯一动作是丢失/转化的线索从该期漏斗中消失；totalWon/totalRevenue 无 leadType 过滤双计

- **控制点 A**：`app/erp/crm/service/processor/ErpCrmLeadProcessor.java#doLose`（L143-150 仅 setDocStatus/setLostReasonId + updateEntity，**无 writeConvLog**）+ `#doCancel`（L152-155 同）+ `app/erp/crm/service/processor/ErpCrmConversionProcessor.java#markLeadConverted`（L116-122 仅弱指针 + docStatus + updateEntity，**无 writeConvLog**）对照 `#doMoveStage`（L162-167 逐一写 convLog）。
- **控制点 B**：`app/erp/crm/service/processor/ErpCrmLeadFunnelRefreshFunnelProcessor.java#refreshFunnel`（L61-64：`List<ErpCrmLeadConvLog> convLogs = loadConvLogs(periodStart, periodEnd); List<String> leadIds = convLogs.stream().map(getLeadId)...distinct().collect(toList()); List<ErpCrmLead> leads = loadLeads(leadIds, ...)`——**leads 集合完全由「期间内有 ConvLog 的 lead」圈定**）+ `app/erp/crm/service/support/FunnelAggregationEngine.java#computeHeader`（L124-151：totalWon/totalLost/totalRevenue/lostRevenue 遍历 leads 按**当前** docStatus 计数，无 leadType 过滤、无转化/丢失时间过滤）。
- **证据**：lead 上月 moveStage 入 stage3、本月 lose：本月聚合 `loadConvLogs(本月)` 无该 lead 任何记录 → leadIds 不含 → `lostByStage`/`computeHeader` 均不计 → **本月漏斗 totalLost/lostRevenue/lostReasonTop 全部漏计**（owner doc lead-waterfall.md §聚合计算流程「totalLost = 期间内 docStatus=LOST 的线索数」）。同理赢单：商机上月 moveStage 到 won stage、本月 convertToQuotation → 本月 totalWon/totalRevenue 漏计。丢失/转化时刻自身无任何日志（doLose/markLeadConverted 零审计行），「期间内唯一动作是 lose/convert」的线索对期间聚合不可见。**双计叠加**：当月推进+当月转化场景，convertToCustomer 链的原 lead（CONVERTED、expectedRevenue 保留）与新建商机 lead（CONVERTED、复制同额）双双进入 leads（两者当月均有 convLog 时），totalWon=2、totalRevenue=2×金额——设计口径「docStatus=CONVERTED 且 isWonStage=true」（lead-waterfall.md L95）亦未校验 isWonStage。
- **问题**：漏斗报表（UC-CRM-15，job 每日刷新 + 手动 refresh）的赢单/丢单/收入度量系统性失真（跨月边界漏计 + 转化链双计）。
- **建议修复方向**：doLose/markLeadConverted（及 doCancel）写 ConvLog（toStageId 语义扩展为保留当前 stage 或专用哨兵，扩展 `computeStageMetrics` 的 exit 索引），使期间圈定覆盖丢失/转化事件；totalWon 计入条件补 `leadType=OPPORTUNITY`（或 isWonStage 校验）消除双计。
- **arm-index 裁决**：新增（grep「漏斗 期间/lost 漏计/convLog lose」arm-index 零命中；P2-RC-035 只覆盖 FunnelAggregationJob 测试缺失维度）。

### P2-CK-crm-003（D8）convertToCustomer 新建商机不透传 territoryId/campaignId——转化后商机在区域管道/区域预测中失明

- **控制点**：`app/erp/crm/service/processor/ErpCrmConversionProcessor.java#createOpportunityFromLead`（L98-114：仅复制 orgId/code/leadType/partnerId/docStatus/contact×3/companyName/ownerId/teamId/expectedRevenue——**无 territoryId、无 campaignId/sourceId**）对照 `ErpCrmLeadBizModel#defaultPrepareSave`（L227-247 新建线索经分配引擎写入 territoryId）与 owner doc territory.md §业务规则 6「一个线索只有一个 territoryId（分配落到叶子节点）。上级区域的管道 = 子区域管道聚合」。
- **证据**：转化产生的新商机（leadType=OPPORTUNITY，真正进入管道推进的主体）territoryId=null：① `QuotaRollupCalculator.accumulatePipeline` 实际段按 territoryId 子树过滤（L206-209）——该商机 CONVERTED 后不计入任何区域实际收入；② P1-RC-039 修复落地的 Forecast territory tier rollup（`ForecastAggregator` 按 Lead.territoryId 聚合，RC-R1.25）对转化后商机无区域归属；③ 营销归因维度（campaignId/sourceId/utm，RC-R1.24 落地的 UTM copy-on-create 只在原 lead 上）在新商机断链。商机要恢复区域归属只能人工 reassignLead。
- **问题**：D8 转化链 orgId/owner 透传维度的 territoryId/campaignId 透传缺失——区域维度报表对「转化产生的商机」系统性遗漏。触发条件：每次 convertToCustomer（主转化路径）。
- **建议修复方向**：`createOpportunityFromLead` 补 `ifPresent(opportunity::setTerritoryId, lead.getTerritoryId())` + campaignId（及 sourceId 如适用）；补断言测试（转化后商机 territoryId == 原 lead territoryId）。
- **arm-index 裁决**：新增（grep「territoryId 透传/转化 territoryId」arm-index 命中 P1-RC-032[直接升格缺失，已修复 RC-R1.21]与 A4.2 运行时注记——不同控制点[升格分支存在性 vs 字段透传]；零同型）。

### P2-CK-crm-004（D6）QuotaRollupCalculator.rollup 层级覆盖语义按平面求和——中间层显式覆盖值与其子明细行并存时上级聚合双计

- **控制点**：`app/erp/crm/service/support/QuotaRollupCalculator.java#rollup`（L92-100 聚合分支：`for (row : rows) if (row.getQuotaAmount() != null && !(territoryId != null && territoryId.equals(row.getTerritoryId()) && row.getTeamId() == null && row.getOwnerId() == null)) sum += row.getQuotaAmount()`——**仅排除「当前查询层级自身」的显式行，子区域层的显式覆盖行与其下团队/个人明细行同时被平面求和**）
- **证据**：owner doc territory.md §配额层级汇总 L113-116「聚合规则：子节点配额求和（**显式值优先，无显式值则向下聚合**）」——「显式值优先」应递归应用于每个子节点（子区域有显式值则取显式值、否则递归聚合孙节点）。实现是子树全行平面求和：区域 A 有显式行 1000（语义=A 覆盖值，已含其团队 t1 的 500）+ A 下团队 t1 明细行 500 → `rollup(null, ...)`（公司级）= 1000 + 500 = **1500**，而层级覆盖语义应为 1000。t1 的 500 被双计。触发条件：管理员为中间层直接配置覆盖值且其下仍有明细行（owner doc L116「管理员可直接为各层级写入显式配额值（覆盖聚合值）」明示该用法合法）。
- **问题**：D6 层级汇总正确性——覆盖语义下上级配额虚增。P2（需中间层显式+子明细并存的配置形态触发）。
- **建议修复方向**：rollup 改递归聚合：对每个直接子节点先查显式行（命中取显式值），否则递归子树；或平面求和时排除「已有父级显式行覆盖的子树」的行。修复时与 territory.md §实现注记 4 对齐裁决（当前注记只描述了单层显式优先，未覆盖递归语义——可能需 owner doc 先澄清意图）。
- **arm-index 裁决**：新增（grep「rollup 双计/显式值优先」arm-index 仅命中 A1.29 交叉注记「UC-CRM-12 territory tier 复核为不成立（QuotaRollupCalculator.rollup 实际支持 territory 子树聚合）」——该裁决覆盖「子树聚合存在性」维度，未覆盖「层级覆盖递归语义」维度）。

### P2-CK-crm-005（D5/D6）distributeAnnualQuota 无重复分配幂等守卫 + 均分尾差不守恒——重复调用生成重复子行使聚合翻倍，Σ子行≠年度总额且无末行吸收

- **控制点**：`app/erp/crm/service/support/QuotaRollupCalculator.java#distributeAnnual`（L118-158：仅校验 ANNUAL 类型 + 未定稿，**无「已存在同 (territoryId,teamId,ownerId,periodType,fiscalYear,periodLabel) 子行」的查重**，重复调用直接再插 4/12 行；L137-138 `BigDecimal each = total.divide(BigDecimal.valueOf(count), 2, HALF_UP)`——每行 2 位舍入后 **无末行吸收尾差**）
- **证据**：orm 实证 `erp_crm_quota` 实体**无任何 unique-key**（module-crm/model/app-erp-crm.orm.xml quota 块唯一键清单零命中）——DB 层无兜底。① 误操作两次 `distributeAnnualQuota(同 annualId)` → 8/24 行重复，rollup 按 periodType=QUARTERLY 聚合时配额翻倍；② 年度 100 万均分 12 月 each=83,333.33 ×12=999,999.96 ≠ 1,000,000（尾差 0.04 无人行吸收）——配额守恒（Σ子=父）不成立，与 quota 段 rollup(ANNUAL 显式) 并读时出现 0.04 级不一致；定稿锁定（isFinalized）只锁父行不锁子行（子行 isFinalized=false 可再 distribute？子行非 ANNUAL 被 L124 类型守卫拦截 ✓，但子行本身可被 CRUD update_ 改额度——归 P2-CK-crm-007 同族）。
- **问题**：D5 幂等边界 + D6 金额守恒。触发：管理员重复点击/重试（无前端幂等承诺）。
- **建议修复方向**：distributeAnnual 前置查重（同维度同期间已存在子行抛 `ERR_QUOTA_*` 业务错误或返回既有行）；尾差并入最后一行（`last = total - each×(count-1)`）。
- **arm-index 裁决**：新增（grep「distributeAnnual/均分」arm-index 零命中）。

### P2-CK-crm-006（D9/D8）LeadDuplicateChecker 每次 lead save 全表加载全部非终态线索实体到内存逐一匹配 + 无 orgId 隔离

- **控制点**：`app/erp/crm/service/support/LeadDuplicateChecker.java#loadNonTerminalLeads`（L109-115：`q.addFilter(in("docStatus", [NEW, QUALIFIED])); return dao.findAllByQuery(q)`——**无 companyName/email/phone 下推 filter、无 limit、全实体加载**）+ `#findDuplicates`（L55-65 全列表内存逐一 `keyHits`）对照消费点 `ErpCrmLeadBizModel#defaultPrepareSave`（L223 `duplicateChecker.checkAndNotify(entityData.getEntity(), context)`——**每次线索创建必经**）
- **证据**：① 查重条件（companyName/contactEmail/contactPhone 命中）完全可下推为 `or(eq(companyName,x), eq(contactEmail,y), eq(contactPhone,z))` 单条 SQL（同文件族正确范式：`ErpCrmLeadBizModel#loadDefaultRule` L293-302 用 filter+limit 下推）；规模化后（数万活跃线索）每次创建 lead 加载全表实体（含 remark 等大字段）——O(N) 内存 + IO；② 无 orgId filter：跨组织同公司名/邮箱命中（UK(code,orgId) 表明多组织是显式设计维度）→ 查重误报，`auto-convert-duplicate-lead=true` 配置下跨组织线索保存被 ERR_DUPLICATE_LEAD_FOUND 误阻断。
- **问题**：D9 全表加载（同型 P3-CK-hr-014 findAllByQuery().size() 家族的更重形态——本条在**每次 save 热路径**且无界）+ D8 orgId 隔离。
- **建议修复方向**：`loadNonTerminalLeads` 改按 keys 下推 or-filter（+ orgId=lead.orgId）；keys 为空已提前返回 ✓ 保持。
- **arm-index 裁决**：新增（grep「LeadDuplicateChecker/查重 全表」arm-index 零命中；同族注记 P3-CK-hr-014 全实体计数家族）。

### P2-CK-crm-007（D5/D3，同型 P1-CK-pur-003 族）CRUD update 无已审守卫——Lead docStatus/leadType/stageId 可经 update_ 直改绕过状态机与阶段方向守卫；负 expectedRevenue/越界 probability 无校验

- **控制点**：`app/erp/crm/service/entity/ErpCrmLeadBizModel.java`（`defaultPrepareUpdate` L267-280 仅触发重评分，**无 docStatus/leadType/stageId 变更守卫**；`defaultPrepareSave` 无 expectedRevenue signum / probability 0-100 / expectedCloseDate 边界校验）+ `ErpCrmTerritoryBizModel`/`ErpCrmQuotaBizModel`/`ErpCrmLeadScoreConfigBizModel`（后两者裸 CRUD，isFinalized 配额行可经 update_ 直改 quotaAmount 绕过 finalizeQuota 锁定语义——territory.md §业务规则 5「isFinalized=true 后配额不可修改」仅 mutation 面拦截）
- **证据**：① `ErpCrmLead__update_` 可把 LOST lead 的 docStatus 改回 QUALIFIED（终态复活，绕过 `ErpCrmLeadStateMachine` 全部 assertCan*）、可直改 stageId 跳过 `validateStageDirection` 的 STRICT 回退拦截（P1-MA2-075 修复的守卫被旁路）、可改 leadType 破坏转化链 leadType 门控前提；② negative expectedRevenue / probability=150 / expectedCloseDate=过去日期经 save_/update_ 直落库，成为看板/漏斗/管道金额与加权计算的输入（weightedRevenue = 负额×probability/100 → 负加权）；③ 定稿配额行 update_ 直改额度。
- **问题**：同型 P1-CK-pur-003/P1-CK-sal-004/P1-CK-inv-005/P2-CK-mfg-006/P2-CK-hr-004 全域同型——命名动作链状态守卫可被通用 CRUD 旁路，crm 站点登记（不复用展开）；② 负数入参族同型注记 P3-CK-mfg-012/P3-CK-hr-009。
- **建议修复方向**：`defaultPrepareUpdate/Save` 守卫：docStatus/leadType 变更仅允许经命名 mutation（CRUD 侧拒绝写入）；stageId 变更走方向校验复用 `validateStageDirection` 语义；expectedRevenue signum>0、probability 0-100、isFinalized 行拒改 quotaAmount。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，crm 站点新增计数）。

### P2-CK-crm-008（D8）Territory 删除仅查子节点守卫——不查 Lead/AssignmentRule/Quota 引用，删叶子区域后悬挂引用静默产生

- **控制点**：`app/erp/crm/service/entity/ErpCrmTerritoryBizModel.java#defaultPrepareDelete`（L80-90：仅 `eq("parentId", entity.getId())` 子节点检查抛 `ERR_TERRITORY_HAS_CHILDREN`，**无 `ErpCrmLead.territoryId`/`ErpCrmTerritoryAssignmentRule.territoryId`/`ErpCrmQuota.territoryId`/`ErpCrmLeadFunnel.territoryId` 引用计数**）
- **证据**：`ErpCrmLead.territoryId`（orm propId 41）+ `ErpCrmTerritoryAssignmentRule.territoryId` + `ErpCrmQuota.territoryId` 均为无 FK 的弱引用。删除一个已分配 500 条线索的叶子区域：lead.territoryId 悬挂（join 查询失效）→ `accumulatePipeline` 子树聚合漏计 + `assignLead` 规则命中后回写悬挂 territoryId；AssignmentRule.territoryId 悬挂后继续匹配新线索（`TerritoryAssignmentEngine.toResult` L96 `result.setTerritoryId(rule.getTerritoryId())` 不校验存在性）。对照同切片正向守卫：子节点禁删已实现（引用守卫半边）。owner doc territory.md §业务规则 7 只声明「已有线索保持历史 territoryId 不变」（停用语义），未覆盖删除路径——删除在用区域零拦截零告警。
- **问题**：同型 P1-CK-hr-001（部门/职位删除无引用守卫）crm 站点模式；因区域引用面（分配规则匹配继续产出悬挂引用）有放大通道，定 P2。
- **建议修复方向**：`defaultPrepareDelete` 增三类引用计数（Lead/Rule/Quota 非零抛 `ERR_TERRITORY_IN_USE`）；或至少 AssignmentRule 引用必查（分配引擎持续消费）。
- **arm-index 裁决**：新增（grep「Territory 删除/区域删除 引用」arm-index 零命中；同型注记 P1-CK-hr-001）。

### P3-CK-crm-009（D8）moveTerritory 不维护 isLeaf 冗余字段——新父节点仍标叶子、原子树移空后旧父节点仍标非叶

- **控制点**：`app/erp/crm/service/processor/ErpCrmTerritoryMoveTerritoryProcessor.java#applyMove`（L73-85：更新 node 的 parentId/level/fullPath + `relocateChildren`，**不翻转 newParent.isLeaf、不回翻 oldParent.isLeaf**）对照同切片正向实现 `ErpCrmTerritoryCreateChildProcessor#createChild`（L49-54 建 child 时翻父 isLeaf=false）
- **证据**：把区域 A 移到叶子 X 下：X.isLeaf 保持 true（X 已有子节点 A）；A 的唯一子节点移走后 A.isLeaf 保持 false。isLeaf 当前消费面：`ErpCrmTerritory.view.xml` 列表展示 + createChild 父翻转逻辑——无业务引擎消费（grep `getIsLeaf` 业务命中仅 createChild），故降 P3（展示字段失真 + territory.md「分配规则通常落到叶子层级」的未来消费前提被破坏）。
- **问题**：D8 冗余字段失同步（同型 orgId 失同步族的 isLeaf 变体，消费面弱）。
- **建议修复方向**：applyMove 前记录 oldParentId，移动后 newParent.isLeaf=false + oldParent 无剩余子时 isLeaf=true。
- **arm-index 裁决**：新增（grep「isLeaf」arm-index 零命中）。

### P3-CK-crm-010（D9/D8）findOpportunityBoardData 商机硬编码 limit 200 静默截断 + 双查询均无 orgId 过滤

- **控制点**：`app/erp/crm/service/entity/ErpCrmLeadBizModel.java#findOpportunityBoardData`（L398-401：`leadQuery.addFilter(eq("leadType","OPPORTUNITY")); leadQuery.setLimit(200);`——无排序（截断哪 200 条取决于返回顺序）、无超限提示；stageQuery/leadQuery 均无 orgId filter）
- **证据**：商机看板（`opportunity-kanban.flux.yaml` L13/L20 两次消费 `@query:ErpCrmLead__findOpportunityBoardData`）第 201 条商机起从看板静默消失；多组织部署下跨组织商机混入同一看板（同型 orgId 族 P2-CK-fin2-007/mfg-007/hr-003 ④）。对照 hr-003 的 limit 5000 同型（本条 limit 200 更早触发）。
- **问题**：D9 截断 + D8 orgId 隔离（同型族 crm 站点）。
- **建议修复方向**：limit 提升配置化 + 超限时在返回结构附带 truncated 标志（前端提示）；补 orgId filter（从 IUserContext）。
- **arm-index 裁决**：新增（grep「limit 200/看板截断」arm-index 零命中；同型注记 P2-CK-hr-003 limit 5000 静默截断族）。

### P3-CK-crm-011（D6）loadConvLogs 期间上界 off-by-one——le(periodEnd+1day 00:00) 把次日零点整的流转计入本期（跨期毫秒级双计）

- **控制点**：`app/erp/crm/service/processor/ErpCrmLeadFunnelRefreshFunnelProcessor.java#loadConvLogs`（L150-152：`LocalDateTime to = periodEnd.plusDays(1).atStartOfDay(); q.addFilter(le("changedAt", to));`）
- **证据**：changedAt 恰为 `periodEnd+1日 00:00:00.000` 的 ConvLog 同时满足本期 `le` 与下期 `ge(from)`——被两期各计一次（首末毫秒重合窗口）。正确写法 `lt(to)`（严格小于）。发生率低（恰零点毫秒）但边界语义错。对照 `ErpCrmEventBizModel#findDueReminders` L98-99 的 `ge(now)+le(now+window)` 是连续窗口无重叠语义 ✓ 不同场景。
- **问题**：D6 日期边界（任务点名维度）。
- **建议修复方向**：`le` → `lt`。
- **arm-index 裁决**：新增（grep「off-by-one/plusDays(1)」arm-index 零命中）。

### P3-CK-crm-012（D10）AssignmentRule conditionValue 坏 JSON 时异常冒泡——一条配置损坏阻塞此后全部线索创建（auto-assign 默认开启）

- **控制点**：`app/erp/crm/service/support/TerritoryAssignmentEngine.java#ConditionMatcher#parse`（L292-303：`JsonTool.parseNonStrict(json)` 异常直接冒泡，仅处理了「解析结果非 Map」的返回空分支）→ `#matches`（L198-216 无 try/catch）→ `assign` L78-82 循环内无隔离 → `ErpCrmLeadBizModel#defaultPrepareSave` L233-234（`erp-crm.territory.auto-assign-on-create` 默认 TRUE 路径必经）
- **证据**：管理员录入一条 conditionValue 非法 JSON 的规则（如 `{"province": ["上海"` 截断）后，每次创建不带 ownerId/teamId 的 lead 都在 `parseNonStrict` 抛 RuntimeException → 整个 save 失败。规则的 matches 抛错无 degrade（对照同引擎 resolver 查询失败 L111-113/L124-126 有 catch → MANUAL 降级的正向范式——匹配器缺同等隔离）。
- **问题**：D10 配置边界——单点配置错误放大为创建通道全阻塞。
- **建议修复方向**：`parse` 捕获解析异常返回 emptyMap（该规则视为不匹配 + WARN 日志），与 resolver 降级范式对齐。
- **arm-index 裁决**：新增（grep「conditionValue JSON/parseNonStrict」arm-index 零命中）。

### P3-CK-crm-013（D6）computeDaysInStage 忽略仍停留中的线索——avgDaysInStage 只统计已离开阶段的记录，在停 lead 的「分析期末截断」时长缺失，停留均值系统性偏低

- **控制点**：`app/erp/crm/service/support/FunnelAggregationEngine.java#computeDaysInStage`（L315-336：`for (int i = 0; i < logs.size() - 1; i++)` 只对「有下一条流转」的区间累计——**末条 ConvLog（当前停留中）不产生任何停留时长**）对照 owner doc lead-waterfall.md §业务规则 1「停留时间 = MIN(进入 Stage N+1 的 changedAt, 丢失/转化的 changedAt, **分析期末**) - 进入 Stage N 的 changedAt」
- **证据**：100 个 lead 进入 stage2 后 90 个仍在停留、10 个次日离开：avgDaysInStage(stage2) 只按 10 个「已离开」样本（约 1 天）计算，90 个「停留 30 天+」样本完全缺失——长停留阶段均值显著低估（存活者偏差）。
- **问题**：D6 度量口径（owner doc 显式要求分析期末截断，实现缺失）。
- **建议修复方向**：末条 ConvLog 补 `periodEnd.atStartOfDay - changedAt` 区间（clamp 非负）。
- **arm-index 裁决**：新增（grep「avgDaysInStage/停留」arm-index 命中均非本控制点；P1-MA1-009 覆盖 DECIMAL/double 精度维度已 resolved，本条是样本口径维度）。

### P3-CK-crm-014（D4，同型 cron 键漂移家族）FunnelAggregationJob 内外层 cron 键不一致——启用需同时配 3 个键，job.yaml description 声明的门控键与 trigger 实际消费键不同

- **控制点**：`app/erp/crm/service/job/ErpCrmFunnelAggregationJob.java#execute`（L45-49 `String cron = ErpCrmConfigs.funnelAggregationCron(); if (StringHelper.isEmpty(cron)) { LOG.info("...skipped..."); return; }`——内层门控键 `erp-crm.funnel.aggregation-cron` **默认空 = execute 永远跳过**，`ErpCrmConfigs` L66-68 实证默认 ""）对照 `app-erp-all/_vfs/nop/job/conf/erp-crm-funnel-aggregation.job.yaml`（L2 `enabled: "@cfg:nop.job.erp-crm-funnel-aggregation.enabled|false"` + L7 `cronExpr: "@cfg:nop.job.erp-crm-funnel-aggregation.cron-expr|0 30 3 * * ?"`——外层双层键；L4 description 逐字「cron 经 erp-crm.funnel.aggregation-cron 配置门控」与 trigger 实际消费的 `nop.job.*.cron-expr` 键**不一致**）
- **证据**：真正启用漏斗聚合需同时配置 `nop.job.erp-crm-funnel-aggregation.enabled=true`（job 被调度）+ `nop.job.erp-crm-funnel-aggregation.cron-expr`（默认可用）+ `erp-crm.funnel.aggregation-cron` 非空（execute 不跳过）。运维按 description 直觉配 `erp-crm.funnel.aggregation-cron=0 30 3 * * ?`：job 因 enabled 默认 false 根本不被调度；反之只配 enabled=true：job 触发但每次 INFO 跳过。对照同域修复后的 `erp-crm-lead-scoring-recalc.job.yaml`（cronExpr 消费 `erp-crm.lead-scoring.schedule-cron` 与内层同键——RC-R1.23 修复形态），funnel job 未跟进对齐。
- **问题**：同型家族（P3-CK-mfg-013/inv-021/sal-023/qa-019/hr-012）crm 站点。
- **建议修复方向**：与家族联合裁决——job.yaml cronExpr 改消费 `erp-crm.funnel.aggregation-cron`（对齐 lead-scoring 形态）或删内层门控；description 修正。
- **arm-index 裁决**：同型登记（cron 键漂移家族，crm 站点新增计数）。

### P3-CK-crm-015（D2/D4/D7）FunnelAggregationJob 顶层失败仅 LOG.error 无告警通道 + retention-period-months 死配置 + job 直调路径 refreshFunnel 无事务原子性

- **控制点**：`app/erp/crm/service/job/ErpCrmFunnelAggregationJob.java#execute`（L50-56 `try { funnelBiz.refreshFunnel(...) } catch (Exception e) { LOG.error(...) }`——无 `IErpSysNotificationBiz` 告警；javadoc L21 声称「按 retention-period-months 计算」但 execute 未消费该键——`erp-crm.funnel.retention-period-months`（默认 24，owner doc lead-waterfall.md 配置点表）**历史快照无限累积零清理**）+ `#refreshFunnel` 经 `IErpCrmLeadFunnelBiz` **Java 直调**（非 GraphQL mutation 入口 → 无事务包装[平台实证见 ck-hr-org：@BizMutation 事务仅 GraphQL 入口生效]）——`ErpCrmLeadFunnelRefreshFunnelProcessor#refreshFunnel` 的清旧（L135-141 逐条删）+ 重建（L74-97 头+明细逐条 save）**非原子**，中途异常（如部分 metrics save 失败）留下「旧快照已删、新头已存、部分明细」的部分快照，getFunnelView 当期展示不完整数据。
- **证据**：缓解因子：job 每日重跑 `clearExistingSnapshots` 精确匹配同期间+维度清旧重建（自愈），失败影响窗口 ≤ 一个 job 周期（但期间手动刷新报错后当天看板失真）；retention 清理完全未实现（无任何 job 消费该键——grep 实证仅 `ErpCrmConfigs.funnelRetentionPeriodMonths()` 定义零调用方）。
- **问题**：D2 失败无告警闭环（B1 job 变体，同型 P3-CK-hr-013）+ D4 死配置 + D7 事务窗口。合为一条 job 质量登记（同文件同入口三症状）。
- **建议修复方向**：顶层 catch 增 notify 告警（复用 notify best-effort）；execute 包 `transactionTemplate.runInTransaction` 使清旧+重建原子；retention 清理补实现或 owner doc 登记 successor。
- **arm-index 裁决**：新增（retention/告警维度 grep 零命中；P2-RC-035 覆盖测试缺失维度不重复；同型注记 P3-CK-hr-013 job 告警族）。

### P3-CK-crm-016（D5）assignLead/reassignLead 无终态守卫 + reassignLead 三 ID 无存在性校验

- **控制点**：`app/erp/crm/service/entity/ErpCrmLeadBizModel.java#assignLead`（L145-167 `requireEntity` 后直接跑分配引擎 + updateEntity，**无 docStatus 校验**）+ `#reassignLead`（L171-188 `if (territoryId != null) lead.setTerritoryId(territoryId);` ×3——territoryId/teamId/ownerId **无存在性校验**可写任意不存在的 ID）
- **证据**：① CONVERTED/LOST/CANCELLED 终态 lead 可被 assignLead 重新分配（区域/团队/owner 在终态单据上变动，与「终态不可操作」语义冲突——弱于 update_ 旁路[P2-CK-crm-007 已覆盖 CRUD 面]，本条是命名 mutation 面缺失）；② reassignLead 写悬挂 territoryId 后该 lead 在区域管道/预测聚合中消失（同 P2-CK-crm-008 悬挂放大链）。对照同切片正向守卫：`ErpCrmConversionProcessor#validateWonStage` 有 stage 存在性反查范式。
- **问题**：D5 入参边界（终态守卫 + 存在性校验双缺）。
- **建议修复方向**：assignLead/reassignLead 前置 `stateMachine.isTerminal(docStatus)` 拒绝；reassignLead 非空 ID 反查存在（territory/team 至少）。
- **arm-index 裁决**：新增（grep「reassignLead」arm-index 零命中）。

### P3-CK-crm-017（D1）getCreatedOpportunity 只读查询标注 @BizMutation——查询走写事务路径

- **控制点**：`app/erp/crm/service/entity/ErpCrmLeadBizModel.java#getCreatedOpportunity`（L213-217 `@Override @BizMutation public ErpCrmLead getCreatedOpportunity(...)`——方法体 `conversionProcessor.getCreatedOpportunity(leadId, context)` 纯读[requireLead + 按弱指针 code 反查]，零写操作）
- **证据**：@BizMutation 经 GraphQL mutation 入口触发 `GraphQLTransactionOperationInvoker` 事务包装（平台实证）——只读查询承担写事务开销与连接占用；且 mutation 语义污染 API 契约（`IErpCrmConversionBiz.getCreatedOpportunity` 应为 query）。
- **问题**：D1 平台注解误用（低危——行为正确，事务/契约语义错）。
- **建议修复方向**：改 `@BizQuery`（确认无隐藏写路径——已核 method 链纯读）。
- **arm-index 裁决**：新增（grep「getCreatedOpportunity」arm-index 零命中）。

### P3-CK-crm-018（D10/D9）countCompletedEvents leadId=null 退化为全系统事件计数 + findAllByQuery().size() 计数模式

- **控制点**：`app/erp/crm/service/support/LeadScoringEngine.java#countCompletedEvents`（L241-249：`if (leadId != null) q.addFilter(eq("relatedLeadId", leadId)); ... return (int) dao.findAllByQuery(q).size();`——**leadId 为 null 时无 relatedLeadId filter 统计全系统全部 COMPLETED 事件**；且全实体加载仅为计数）
- **证据**：null 分支当前生产不可达（`scoreByFormula` L205 传 `lead.getId()` 非空、`formValue` L232 `lead != null ? getId() : null` 的 lead 实参链上游均非空），但 FORMULA 分支把「计数语义」写成「leadId 空则全局计数」是错误防御方向（应返回 0）；`findAllByQuery(q).size()` 应 `findCount`（同型 P3-CK-hr-014 家族）。
- **问题**：D10 防御分支语义 + D9 计数模式。
- **建议修复方向**：leadId==null 直接 return 0；计数改 findCount。
- **arm-index 裁决**：新增（grep「countCompletedEvents」arm-index 零命中；同型注记 P3-CK-hr-014）。

### P3-CK-crm-019（D10/D5）查重「默认仅提示」实际零提示——checkAndNotify 返回的候选列表被调用方丢弃，无日志无事件

- **控制点**：`app/erp/crm/service/entity/ErpCrmLeadBizModel.java#defaultPrepareSave`（L223 `duplicateChecker.checkAndNotify(entityData.getEntity(), context);`——返回值丢弃，注释 L222 自称「查重默认仅提示不阻断；候选结果可经 findDuplicates 查询」）对照 `LeadDuplicateChecker#checkAndNotify` javadoc（L73「默认配置（false）下仅返回候选，**调用方可记录日志**，不阻断」）
- **证据**：默认 `erp-crm.auto-convert-duplicate-lead=false` 下：不抛异常、返回候选被忽略、无 LOG、无 notify 事件——「提示」语义实际是「静默放行」。state-machine.md §4「重复线索提交 → 查重服务**提示**合并/跳过」的 L1 语义在创建时刻用户零感知（只有主动调 findDuplicates 才可见）。轻微：owner doc A1.28 已裁决 UC-CRM-01 接受 on 主路径（P2-RC-033 是 lastContactDate 维度），查重提示维度无独立 finding——本条从「提示未落地」角度登记。
- **问题**：D10/D5——声明行为与实际行为漂移（无用户可见影响数据，仅提示通道缺失）。
- **建议修复方向**：checkAndNotify 或调用点对非空候选 LOG.warn（lead.code + 候选数），或接 notify 弱提示事件（复用 best-effort）。
- **arm-index 裁决**：新增（grep「checkAndNotify/查重 提示」arm-index 零命中）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | crm 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-pur-003`（CRUD update 无守卫全域族） | Lead/Quota/ScoreConfig 裸 CRUD update 面（docStatus/leadType/stageId/额度直改） | 同型登记为 P2-CK-crm-007 |
| cron 键漂移家族（mfg-013/inv-021/sal-023/qa-019/hr-012） | `erp-crm.funnel.aggregation-cron` 内层键 + `nop.job.erp-crm-funnel-aggregation.*` 外层双层键 | 同型登记为 P3-CK-crm-014 |
| `P2-CK-fin2-007`/`P2-CK-mfg-007`/`P2-CK-hr-003`④（orgId 隔离族） | findOpportunityBoardData 双查询 + LeadDuplicateChecker 查重无 orgId | 并入 P3-CK-crm-010 与 P2-CK-crm-006 |
| `P1-CK-hr-001`（主数据删除无引用守卫） | Territory 删除只查子节点不查业务引用 | 同型模式登记为 P2-CK-crm-008 |
| `P3-CK-hr-014`（findAllByQuery().size() 计数） | countCompletedEvents 全实体计数 | 同型注记于 P3-CK-crm-018 |
| `P3-CK-mfg-012`（负数入参静默族） | 负 expectedRevenue/越界 probability 无校验 | 并入 P2-CK-crm-007 子症状 |
| `P1-MA2-086`（10 cron job 并发副作用全局裁决） | FunnelAggregation/LeadScoringRecalc job 并发重复 | 全局已裁决，本切片不重复 |
| `P0-CK-mfg-001`（固定幂等键） | 转化幂等键 `"CUS-/OPP-/SQ-" + leadId` 含 leadId 非固定 + validateNotConverted 拒 CONVERTED + UK(code,orgId) DB 兜底 | **核查不成立**——非同型，见「验证为正确」 |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：切片 26 文件 `@Inject private`=0、`System.currentTimeMillis()/LocalDateTime.now()/new Date()`=0（时间一律 `CoreMetrics.currentTimestamp()/today()/currentDateTime()`）、`extends RuntimeException/Exception`=0（业务异常全部 `NopException` + `ErpCrmErrors` 中文描述）、字典字符串 `==` 比较=0（全 `Objects.equals()`/`.equals()`）、`printStackTrace`=0。
- **addOrderField 布尔参数 8 处全部正确**（C1.2 校准先例：第二参是 desc）：`ErpCrmLeadBizModel` L324 `("id", false)`=成员 id 升序 ✓（territory.md 注记「id 升序」）/ L343 `("createTime", true)`=createTime 降序取上次分配 ✓ / L394 `("sequence", false)`=看板阶段升序 ✓；`ErpCrmLeadProcessor` L213 first stage sequence 升序 ✓；`LeadScoringEngine` L316 准则 sequence 升序 ✓；`ErpCrmEventBizModel` L100 startDateTime 升序 ✓ + L129 `("reminderMinutesBefore", true)`=降序取最大 ✓（注释 desc 一致）；`ErpCrmTerritoryBizModel` L75 sortOrder 升序 ✓。
- **defaultPrepareUpdate 触发重评分读到新值（任务 D6 时序核查点）**：平台实证 `CrudBizModel.doUpdate`（nop-entropy `.../biz/crud/CrudBizModel.java` L836-860）：`buildEntityDataForUpdate` → L962 `requireEntity(id, METHOD_UPDATE)` 从 db 加载 **attach 实体** → L846 `copyToEntity` 把请求字段拷贝上去 → **L850-851 才调 defaultPrepareUpdate**。`LeadScoringEngine.requireLead` 的 `leadDao().getEntityById(leadId)` 在同一 session 命中**同一 attach 实例**（已带新值）——评分计算基于本次更新后的字段，不存在「评分滞后一轮」。
- **auto-qualify 与 update_ 无乐观锁自毁（任务 D7 核查点推演）**：defaultPrepareUpdate 内 `qualifyProcessor.qualify` 对同一 attach 实例 `setDocStatus(QUALIFIED)+updateEntity`，随后 doUpdate 流程 L857 `doUpdateEntity` 再次 updateEntity **同一实例**（非 detached version-carrier）——无 stale version 冲突，update 正常提交（docStatus=QUALIFIED 随后落库）。无「编辑评分达标 NEW 线索永久失败循环」。
- **转化前置守卫齐（RC-R1.21/22 修复在位验证）**：`convertToCustomer` 链 `validateNotConverted → validateLeadType(LEAD) → validateDocStatus(QUALIFIED)`；`convertToQuotation` 链 `validateNotConverted → validateLeadType(OPPORTUNITY) → validateDocStatus(QUALIFIED) → validateWonStage → requireOpportunityPartner`（守卫顺序与 state-machine.md 实现注记逐条一致）；`convertToOpportunity`（直接升格）经 `promoteToOpportunity` 的 `validateDocStatus(QUALIFIED)` 间接拒绝 CONVERTED（错误码为 ERR_LEAD_NOT_QUALIFIED 而非 ERR_LEAD_ALREADY_CONVERTED，语义 drift 可忽略——拦截行为等价）。
- **P0-CK-mfg-001 固定幂等键同型核查不成立**：`createPartnerFromLead` L74 `"CUS-"+lead.getId()`、`createOpportunityFromLead` L101 `"OPP-"+lead.getId()`、`createQuotationFromOpportunity` L90 `"SQ-"+lead.getId()`——幂等键均含 leadId 非进程固定值；重复转化被 `validateNotConverted`（CONVERTED→抛 ERR_LEAD_ALREADY_CONVERTED）拒绝；并发双转化由 `UK(code,orgId)`（Lead/Partner/Quotation 三端 orm 实证）DB 兜底，一胜一败回滚。
- **dict 无死状态（任务 D3 核查点）**：`lead-doc-status.dict.yaml` 5 值 NEW/QUALIFIED/CONVERTED/LOST/CANCELLED 全部有 writer（NEW=创建默认 + qualify/lose/cancel/convert 四 mutation）且 code 常量（ErpCrmConstants L14-18）与 dict 值逐一相符；event-status 3 值全可达（complete/cancel processor 在位）。**前端消费死值**（Lead.view.xml badge 比较不存在的 'ACTIVE'）已由 **P2-MA4-020** 登记（view.xml drift，归 C8.2）——复用不重复登记。
- **终态不可推进（任务 D3 CLOSED_WON/LOST 核查点）**：`validateMovable` 拒绝 CONVERTED/LOST/CANCELLED 的 moveStage；`assertCanQualify` 仅 NEW、`assertCanLose/Cancel` 仅 NEW/QUALIFIED——终态无出边（命名 mutation 面；CRUD 旁路归 P2-CK-crm-007 同型）。
- **stage 方向守卫在位（P1-MA2-075 修复验证）**：`validateStageDirection` STRICT 默认（`allowStageBackward()` 默认 false）抛 ERR_STAGE_BACKWARD_MOVE + config-gate 放行 + LOG.warn + convLog 全量留痕；fromStageId null（首次入漏斗）跳过 ✓；fromStage 已删（getEntityById null → fromSeq null）防御放行 ✓。
- **乐观锁在位**：切片实体（Lead/Territory/Quota/LeadScore/LeadFunnel/FunnelStageMetrics/Event/ConvLog 等）orm 逐实体 `versionProp="version"` 实证——qualify/lose/moveStage/assign/update 并发由版本冲突检测；同 lead 并发 assignLead 一胜一 OL 失败（无锁但写安全）。
- **转化三链事务原子性在位（D7 核查点）**：三个 convert mutation 均 @BizMutation 经 GraphQL 入口（平台实证 `GraphQLTransactionOperationInvoker`：operation==mutation → 事务包装）——partner 创建 + 新商机 + 弱指针回写 + CONVERTED 整体提交/回滚，无「客户已建商机缺失」中途态。
- **reminderMinutesBefore 已实现（P1-MA2-076 修复验证）**：`findDueReminders` per-event `effectiveReminder = reminderMinutesBefore ?: 全局 window` + 扫描窗口按 max(per-event) 拓宽（L86-92）+ fallback 语义与 state-machine.md §7 一致。
- **SCHEDULED 评分接线完整（P1-RC-035/RC-R1.23 修复验证）**：`erp-crm-lead-scoring-recalc.job.yaml`（enabled 默认 false 部署 opt-in + cronExpr **消费 `erp-crm.lead-scoring.schedule-cron` 与内层同键**——非漂移形态）→ nopBatchTaskRunner → `lead-scoring-recalc.batch.xml`（orm-reader loader **排除 CONVERTED/LOST/CANCELLED 终态**）→ `ErpCrmLeadScoringRecalcHelper.recalculateOne`（schedule-cron 空值=跳过 + REQUIRES_NEW + 单条 try/catch WARN 隔离 + session flush + 空 ctx 兜底 ServiceContextImpl）——batch-task 接线与 owner doc lead-scoring.md 配置表行一致。
- **ROUND_ROBIN/LOAD_BALANCED 挑人已实现（P1-RC-036/RC-R1.57 修复验证）**：`TerritoryAssignmentEngine.toResult` 经 `TeamMemberResolver`（BizModel dao 实现：成员 id 升序 / lastOwner createTime desc 取下一位循环 / 活跃线索计数排除终态）+ config 门控 `assignment-method-enabled` 默认 TRUE + 成员空/resolver null/查询失败 catch → MANUAL 降级（degraded=true）——与 territory.md §实现注记 2 逐条一致（代理字段匹配维度归 P2-RC-032 todo 复用不登）。
- **评分归一化计算正确（D6 核查点）**：`normalize` `Σ(raw×weight)/Σ(max×weight)×100`、`sumMaxWeighted.signum()<=0` 除零防护返回 0、`divide(...,0,HALF_UP)`；weightedRevenue `probability/100` scale 2 HALF_UP；avgDealSize 除零守卫（totalWon>0）；avgCycleDays 除零守卫；lookup 表非数字 score `intOf`→0 防御；`loadActiveConfig` >1 active 抛 ERR_MULTIPLE_ACTIVE_SCORE_CONFIG（业务规则 8 评分时点强制单一口径）；`extractLeadField` `orm_propId<=0`→null 无 NPE；评分 append-only（saveEntity 新建不 update）✓ 业务规则 6。
- **Pattern B custom override 绕过守卫面不存在**：切片 5 实体（Lead/Event/Territory/Quota/LeadScore）`*.xbiz.xml` 全部空 `<actions/>` 纯 extends。
- **beans.xml 接线完整（D4）**：切片 14 个 per-mutation Processor + 2 状态机 Bean + 6 support 引擎/helper + 2 job bean 全部注册（app-service.beans.xml 逐 bean 核对）；job.yaml invoker `bean: erpCrmFunnelAggregationJob, method: execute` / `bean: nopBatchTaskRunner` 与 bean id 对上——无孤立声明/漏调。
- **recalculateScore 与 update_ 同实例幂等**：评分 saveEntity + scoreLine 逐行 saveEntity + autoQualified qualify 均同一 @BizMutation 事务内原子（经 BizModel mutation 入口时）；score save 后 id 已生成再建 line（`score.getId()` L272 时序正确）。
- **EventTimelineAggregator null 安全**：timestamp null 走 nullsLast；leadId null 早退；合并两源排序正确（Comparator.nullsLast(reverseOrder)）——「客户 360 视图」时间线聚合（getLeadTimeline）无 D10 风险。
- **markLeadConverted 弱指针与 getCreatedOpportunity 反查闭环**：relatedBillType=CRM_LEAD + relatedBillCode=opportunity.code（UK code 反查唯一命中）；convertToQuotation 侧 SALES_QUOTATION + quotation code（`"SQ-"+leadId` 唯一）。

## arm-index 复用 or 新增裁决（汇总）

- 新增关键符号（arm-index 零命中）：`getTerritoryPipeline 公司级口径`/`actual leadType 过滤`/`convLog lose 转化漏计`/`转化 territoryId 透传`/`rollup 显式覆盖双计`/`distributeAnnual 幂等`/`LeadDuplicateChecker 全表`/`Territory 删除 引用`/`isLeaf 失同步`/`limit 200`/`plusDays(1) off-by-one`/`parseNonStrict 阻塞`/`avgDaysInStage 期末截断`/`retention-period-months`/`reassignLead 守卫`/`getCreatedOpportunity @BizMutation`/`countCompletedEvents`/`checkAndNotify 静默` → **新增**。
- **复用（不重复登记，报告中注记）**：
  - NOTIFY_OWNER 无派发 → **P2-RC-031**（todo watch-only）——验证修复在位（引擎仍无 notify 调用），不登记。
  - territory 匹配代理字段（companyName/department/expectedRevenue）→ **P2-RC-032**（todo watch-only）——引擎注释自承，不登记。
  - qualify 不更新 lastContactDate → **P2-RC-033**（todo watch-only）——不登记。
  - LeadScoreConfigBizModel 裸 stub（isActive 保存时唯一性）→ **P2-RC-034**（todo watch-only）——本报告确认仍为 19 行裸 stub，不登记。
  - FunnelAggregationJob/SequenceOverdueJob 测试缺失 → **P2-RC-035**（todo）——cron 键漂移是不同维度，同型家族登记为 P3-CK-crm-014。
  - stage 等值边界（L1 `<=` vs 代码 `<`）→ **P2-RC-036**（todo watch-only）——validateStageDirection L109 `toSeq < fromSeq` 形态未变，不登记。
  - 漏斗度量列 DECIMAL vs double 精度 → **P1-MA1-009**（resolved ✅）——round4/round2 double 舍入不重复登记。
  - Lead.view.xml docStatus badge 消费死值 ACTIVE → **P2-MA4-020**（view drift，归 C8.2）——D3 dashboard 死状态消费维度复用不登。
  - ForecastAggregator TOCTOU 并发 → **P2-MA4-013(a)**（watch-only）+ forecast territory tier → **P1-RC-039**（resolved RC-R1.25）——归 C6.4 forecast 切片。
  - stageId 单向守卫缺失 → **P1-MA2-075**（resolved R1.24）——修复在位验证（见「验证为正确」）。
  - Event reminderMinutesBefore 死字段 → **P1-MA2-076**（resolved）——修复在位验证。
  - 10 cron job 并发重复副作用 → **P1-MA2-086**（全局合并裁决）——job 并发维度不重复。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 2 | P1-CK-crm-001/002 |
| P2 | 6 | P2-CK-crm-003..008 |
| P3 | 11 | P3-CK-crm-009..019 |

按主维度：D6×5（001/002/004/011/013）、D8×3（003/008/009）+ 跨计（001/002 含 D8、010 主 D9）、D5×3（005/007/016）、D9×2（006/010）、D10×3（012/018/019，019 跨 D5 主归 D10）、D4×1（014）、D2×1（015，跨 D4/D7）、D1×1（017）。（精确主维度归属：001 D6、002 D6、003 D8、004 D6、005 D5、006 D9、007 D5、008 D8、009 D8、010 D9、011 D6、012 D10、013 D6、014 D4、015 D2、016 D5、017 D1、018 D10、019 D10。）

同型/复用裁决：同型登记 2（007 pur-003 族 / 014 cron 键漂移家族）+ 同型模式/注记 4（008 hr-001 模式 / 010+006 orgId 族并入 / 018 hr-014 家族 / 007 内含 mfg-012 负数族）+ arm-index 复用不登记 11 项（RC-031/032/033/034/035/036、MA1-009、MA4-020、MA4-013a、RC-039、MA2-086）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 26 文件逐行深读（LeadBizModel 455 / LeadProcessor 255 / LeadScoringEngine 417 / TerritoryAssignmentEngine 381 / FunnelAggregationEngine 459 / ConversionProcessor 232 / QuotaRollupCalculator 276 / RefreshFunnelProcessor 237 / 10 个薄 BizModel+Processor 桩确认 / 2 状态机 Bean 全文 / LeadDuplicateChecker / LeadActivityDerivationHelper / EventTimelineAggregator / 2 Event processor / 2 job + helper）；平台源码实证 2 处（`CrudBizModel.doUpdate` update 时序与 attach 语义、`CrudBizModel.get(id,ignoreUnknown)` 非锁语义）；orm 核对（切片实体 versionProp 全在位 / UK 清单含 Quota 无 UK 实证 / lead-doc-status dict 5 值 / Lead 关键列 territoryId propId41）；接线核对（beans.xml 26 相关 bean + batch.xml loader 终态排除 + 2 job.yaml 键形态 + 5 xbiz 空 override）；消费面抽查（opportunity-kanban.flux.yaml、ErpCrmTerritory.view.xml isLeaf）；测试文件清单核对（19 测试类含 TestFunnelAggregationEngine/TestErpCrmTerritoryQuota 等，行为语义交叉验证用）；arm-index crm 相关 15+ 条逐条裁决。
- **未深查**：CPQ/营销/预测/销售序列全族（ForecastAggregator/ForecastRecalcJob/PriceRule/BundlePricing/ProductConfigurator/Sequence*/Campaign——归 C6.4）；`ErpCrmReportBizModel` 数据集构造细节（报表渲染归 C6.4 的 marketing attribution 修复面）；`erp-crm-web` 其余 AMIS 页面契约 drift（归 C8.2）；`ErpCrmEventReminderJob` 本体（UC-CRM-08 归 A1.29 切片，本切片仅核对 findDueReminders 语义）；测试代码正确性（仅用于行为语义交叉验证）；ForecastAggregator 的 periodLabel 多行结构细节（决定 P1-CK-crm-001 预测段期间过滤修复方案时需 C6.4 侧联动核对）；Lead xmeta 层前端必填拦截对 P2-CK-crm-007 触发面的进一步收窄。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-crm-001**——公司级 `isNull("territoryId")` 口径是否有产品语义背书（若设计意图就是「公司级=未分配区域」则降 P2，但 quota 段公司级聚合全部行的三段互斥使该解读难以成立）；双计子症状依赖「原 LEAD 与新商机均 CONVERTED 且均进 leads 集合」——建议主 agent 以一次 convertToCustomer + convertToQuotation 的集成测试实证 `actualRevenue` 是否翻倍。
  2. **P1-CK-crm-002**——跨月漏计的影响面依赖漏斗 job 实际启用（job 默认 enabled=false，当前部署可能从未跑过）；若产品只在月内短窗口看漏斗影响减半。定性为 P1 基于「度量语义系统性错」而非当前触发频率。
  3. **P2-CK-crm-004**——「显式值优先递归应用于子节点」是我的 owner doc 解读（territory.md L113-116 措辞「子节点配额求和（显式值优先，无显式值则向下聚合）」），修复前需 owner doc 裁决递归语义是否为设计意图（若是平面求和意图则本条 not-a-problem）。
  4. **P3-CK-crm-014**——与 cron 键漂移家族联合修复裁决时统一方向（内层键删除 vs job.yaml 改消费内层键），单域修复易造成家族内不一致（对齐 lead-scoring-recalc 修复形态是现成方向）。
