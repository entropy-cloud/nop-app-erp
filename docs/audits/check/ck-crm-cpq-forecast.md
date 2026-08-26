# ck-crm-cpq-forecast — crm「CPQ 与营销预测」切片实现代码检查报告

> 工作项：C6.4。执行日期：2026-08-26。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-crm/erp-crm-service/src/main/java` CPQ/营销/预测/序列切片 30 个手写生产文件——CPQ（`support/ProductConfigRuleEngine` 185 行 + `support/PriceRuleEngine` 200 行 + `support/BundlePricingCalculator` 113 行 + `processor/ErpCrmProductConfiguratorGenerateQuoteProcessor` 311 行 + `entity/ErpCrmProductConfiguratorBizModel`/`ErpCrmConfigRuleBizModel`/`ErpCrmPriceRuleBizModel`/`ErpCrmBundlePricingBizModel`(+Line)/`ErpCrmQuoteTemplateBizModel`）、营销（`entity/ErpCrmCampaignBizModel` 19 行裸 stub + `report/ErpCrmReportBizModel` campaign-attribution/forecast-accuracy 数据集段）、销售预测（`support/ForecastAggregator` 402 行 + `entity/ErpCrmForecastBizModel`/`ErpCrmForecastPeriodBizModel`(+3 薄 stub) + `processor/ErpCrmForecastRefreshForecastProcessor`/`ErpCrmForecastPeriodClosePeriodProcessor` + `job/ErpCrmForecastRecalcJob` 84 行）、销售序列（`support/SequenceAssignmentEngine` 240 行 + `support/SequenceStepAdvancer` 211 行 + `entity/ErpCrmLeadSequenceProgressBizModel` 311 行 + 3 个 per-mutation Processor[Assign/Advance/Switch] + `job/ErpCrmSequenceOverdueJob` 118 行 + 4 个薄 stub BizModel[Sequence/Step/Assignment/…])。跨文件核实：`module-crm/model/app-erp-crm.orm.xml`（13 实体列集/versionProp/UK）、`_vfs/erp/crm/beans/app-service.beans.xml`、`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-crm-{forecast-recalc,sequence-overdue}.job.yaml`、`erp-crm-meta` 10 个 dict.yaml、`erp-crm-web` ErpCrmPriceRule.view.xml 消费面、`ErpCrmConfigs`/`ErpCrmConstants`/`ErpCrmErrors` 常量接线。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。切片文件逐行深读 + 平台源码实证 2 处（`CrudBizModel.saveEntity` L1988 签名 `saveEntity(T entity, String action, IServiceContext context)` 含唯一性/数据权限/状态机初始化——序列 Processor `eventBiz.saveEntity(event, null, context)` 调用合法；`GraphQLTransactionOperationInvoker` L29 `operationType == mutation` 才包装事务——job 直调 Java 接口无事务，印证 P3-CK-crm-015 先例）+ orm/dict/beans/job-yaml/view 接线核对 + arm-index 复用裁决（crm 相关 RC/MA 系列逐条）。
> 切片边界：线索/商机/territory/quota/漏斗/评分/转化归 C6.3 已查（ck-crm-lead.md）勿重复；`ErpCrmEventReminderJob` 归 A1.29 切片；web AMIS 页面契约 drift 归 C8.2。已知同型基线（CRUD 守卫族/cron 键漂移族/orgId 隔离族/坏配置冒泡族/job 告警族）按任务指令登记 crm2 站点。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-crm2-001（D6）countConsecutiveOverdueSteps 前向扫描把已按期完成的步骤计为逾期——按期推进到第 3 步以后的序列进度系统性误报逾期并派发提醒

- **控制点**：`app/erp/crm/service/entity/ErpCrmLeadSequenceProgressBizModel.java#countConsecutiveOverdueSteps`（L249-261：`for (int i = 0; i <= currentIndex && i < steps.size(); i++) { ... LocalDateTime dueAt = startedAt.plusDays(cumulativeDays + grace); if (now.isAfter(dueAt)) count++; else break; }`——循环**从 0 正向扫描到 currentIndex**）
- **证据**：注释 L249 自称「反向扫描：从 currentIndex 起向前累计」、方法 javadoc L236-239「从当前步起向前连续逾期的步骤数…第一个未逾期步骤即终止」——实现与声明方向相反。推演：progress 位于 currentIndex=N 时，步骤 j<N 的到期时刻（startedAt+ΣdueDays[0..j]+grace）必然早于当前步——只要线索推进到第 3 步（index≥2），即使每步都**按期甚至提前完成**，步骤 0..N-1 的墙钟到期日均已过去 → count 轻易达到 ≥ max-overdue-steps(3) → `scanOverdueSteps` L120 `consecutiveOverdue >= maxOverdue` 命中 → `ErpCrmSequenceOverdueJob` 向负责人派发 `crm.sequence-overdue` 误报提醒。唯一不误报的形态是低 index 且进度领先于排期。正确语义应为从 currentIndex 起反序、且当前步未逾期时计数为 0（已完成步骤无 per-step 完成时间戳，无法回溯判定其是否逾期——数据模型限制下最接近语义是「当前步逾期深度」）。测试盲区实证：`TestErpCrmSequenceAndFunnel#testScanOverdueSteps` L270-283 只断言真逾期场景（连续逾期 3 步），无「按期推进到第 5 步应零误报」反向用例。owner doc sales-sequence.md §3 超时处理：「步骤到期日 > now + gracePeriod → 标记步骤为逾期…连续逾期步骤 >= 3 → 提醒」——已完成步骤不可再「逾期」。
- **问题**：D6 度量/日期计算正确性——序列逾期提醒（UC-CRM-14 组成部分）启用后系统性误报。job 默认 cron 空=不调度（触发频率低），但定性对齐 P1-CK-crm-002 先例（「度量语义系统性错」而非按当前触发频率定级）。
- **建议修复方向**：循环改为从 currentIndex 反序且先判当前步（当前步未逾期 → 0）；或按数据模型限制改口径为「当前步逾期且 currentIndex+1 ≥ 阈值」并同步修正注释与 owner doc §3 语义；补按期推进零误报反向测试。
- **arm-index 裁决**：新增（grep arm-index「scanOverdue/逾期 误报/overdue」零命中；P2-RC-035 只覆盖 job 测试缺失维度）。

### P1-CK-crm2-002（D6/D5）PriceRuleEngine 完全忽略 productCategory/customerCategory 维度——类别限定规则退化为全局规则，对所有产品/客户错误匹配定价

- **控制点**：`app/erp/crm/service/support/PriceRuleEngine.java#ruleMatchesProduct`（L85-88：`return rule.getProductId() == null || Objects.equals(rule.getProductId(), productId);`——**无 productCategory 判定**）+ `#ruleMatchesCustomer`（L90-97：仅 customerId，**无 customerCategory 判定**）+ 消费面 `app/erp/crm/service/processor/ErpCrmProductConfiguratorGenerateQuoteProcessor.java#loadActivePriceRules`（L185-198：预过滤也只按 productId 缩小）
- **证据**：orm 实证 `ErpCrmPriceRule` 列集含 `productCategory`/`customerCategory`（module-crm/model/app-erp-crm.orm.xml PriceRule 块）；owner doc cpq.md §ErpCrmPriceRule 字段表逐字「productCategory | 适用产品品类（可空）」「customerCategory | 适用客户类别（可空）」；前端消费面实证 `erp-crm-web/pages/ErpCrmPriceRule/ErpCrmPriceRule.view.xml` L34-35/L56-57 明文展示 `productCategory[产品类别]`/`customerCategory[客户类别]` 可编辑列——管理员按设计录入「productCategory=SERVER、productId 留空」的促销规则后，`ruleMatchesProduct` 对 productId=null 恒真 → 该规则匹配**所有产品**的报价，错误折扣/覆盖价直接进入 `generateQuote` 产出的 `ErpSalQuotation.totalAmount`。同理 customerCategory。4 个匹配器（product/customer/currency/period/quantity）中唯二类别维度被静默丢弃。
- **问题**：D6 定价正确性 + D5 入参维度缺失——类别限定配置形态下报价金额错误（用户可见正确性）。P1（配置面按设计文档+ORM+前端三层宣示可用，陷阱可达）。
- **建议修复方向**：`ruleMatchesProduct` 增 productCategory 匹配（需经 `IErpMdProductBiz` 反查产品类别，或调用方在 priceRuleContext 传入 productCategory）；customerCategory 同理（partner 类别）。若本期不做类别维度，须在 owner doc cpq.md 实现注记显式降级 + 前端隐藏该二列（防误配置）。
- **arm-index 裁决**：新增（grep「productCategory/类别 匹配/price rule category」arm-index 零命中；A1.30 CPQ 切片 verdict 只覆盖引擎主路径存在性，未覆盖类别维度）。

### P2-CK-crm2-003（D6/D8，关联 P1-CK-crm-001/002）预测准确率局部实现——实际关闭段按 expectedCloseDate 代理口径而非转化时间、closePeriod 不做期末终刷、team/territory/company 维度准确率行永不生成

- **控制点**：`app/erp/crm/service/support/ForecastAggregator.java#sumConvertedRevenue`（L262-280：`eq("docStatus", CONVERTED)` + `ge/le("expectedCloseDate", period 起止)`——「期间内实际关闭」用**预期关闭日期**代理，无转化时间过滤）+ `#computeAccuracy`（L123-131：`loadPersonalForecasts` L254-260 仅取 `ownerId != null` 行——团队/区域/公司行不生成 accuracy）+ `processor/ErpCrmForecastPeriodClosePeriodProcessor.java#closePeriod`（L28-40：置 CLOSED 后直接 computeAccuracy，**无期末最后一次 refreshForecast**）
- **证据**：三个子症状：① **口径代理失真**——owner doc sales-forecast.md §ErpCrmForecastAccuracy 逐字「actualClosedRevenue 实际关闭收入（**期间内** `ErpCrmLead` **CONVERTED** 的 expectedRevenue 汇总）」。「期间内 CONVERTED」应以转化发生时刻圈定；实现以 expectedCloseDate 圈定：期间内转化但 expectedCloseDate 滑到下期的商机漏计、expectedCloseDate 在期间内但期间结束后才转化的商机多计——双向失真。根因与 P1-CK-crm-002 同源（doLose/markLeadConverted 不写 ConvLog、无转化时间戳——crm 站点已登记，本条不重复登记根因，登记 forecast 消费面口径缺陷）；② **陈旧快照对比**——design §准确率计算时机「期间末的最终 commitAmount」：若期间末商机变动后未刷新（job 未启用/未手动刷），closePeriod 对陈旧 forecast 计算 accuracy；③ **维度缺失**——Accuracy 实体有 ownerId/teamId/territoryId 三维度列（orm 实证），实现只生成 personal 行，团队/区域/公司准确率永不可见。缓解：accuracy 公式本体正确（见「验证为正确」）。
- **问题**：D6 准确率口径 + D8 声明维度未落地。P2（预测准确率是管理层只读参考面，非资金路径）。
- **建议修复方向**：① 转化时间来源与 P1-CK-crm-002 修复联动（ConvLog 补写后按转化时刻圈定）；② closePeriod 在置 CLOSED 前先内部 refreshForecast（需放开 requireOpen 时序——先刷后关）；③ computeAccuracy 补 team/territory/company 行（复用 forecast 行聚合）。
- **arm-index 裁决**：新增（grep「sumConvertedRevenue/准确率 口径/accuracy 期间」零命中；P2-MA4-013(b) 只覆盖 accuracyOf 中间值测试缺失维度）。

### P2-CK-crm2-004（D4/D6）ForecastRecalcJob 每次运行只重算一个 OPEN 期间（setLimit(1) 无排序）——设计要求「重算所有 OPEN 期间」，月度+季度并行期间下部分期间永不被刷新

- **控制点**：`app/erp/crm/service/job/ErpCrmForecastRecalcJob.java#findOpenPeriod`（L72-79：`q.addFilter(eq("status", OPEN)); q.addFilter(le("periodStart", today)); q.addFilter(ge("periodEnd", today)); q.setLimit(1); return forecastPeriodBiz.findFirst(q, null, ctx);`——**单期间、无 addOrderField**）
- **证据**：owner doc sales-forecast.md §业务规则 4 逐字「定时 Job（每日凌晨**重算所有 OPEN 期间**）」。forecast-period-type 字典含 MONTHLY/QUARTERLY/ANNUAL 三型（dict.yaml 实证），正常运营下同日可同时存在 2026-08（月度）与 2026-Q3（季度）两个 OPEN 期间：job 每日只刷 findFirst 返回的一条，且无排序导致命中的行不确定（可能恒同一条）——另一期间的 forecast 行停留在上次手动刷新的陈旧状态。注：P1-MA2-086 的 job 并发维度不覆盖本条（本条是单次运行内的覆盖面缺失，非并发重复）。
- **问题**：D4 调度链覆盖面 + D6 聚合新鲜度。
- **建议修复方向**：改 `findList`（查全部 OPEN 期间）逐期间刷新（单期间失败隔离 try/catch per period，镜像 SequenceOverdueJob 单条隔离范式）；或至少 addOrderField 确定性排序 + 轮转。
- **arm-index 裁决**：新增（grep「findOpenPeriod/单期间/重算所有」零命中）。

### P2-CK-crm2-005（D5/D8）advanceStep 不校验事件与进度的线索归属——任一其他线索的已完成事件可推进本线索序列步骤

- **控制点**：`app/erp/crm/service/processor/ErpCrmLeadSequenceProgressAdvanceStepProcessor.java#advanceStep`（L46-52：`requireProgress(progressId)` + `eventBiz.requireEntity(eventId, ...)` + `sequenceStepAdvancer.advance(progress, event, steps)`——**无 `event.relatedLeadId == progress.leadId` 判定**）+ `app/erp/crm/service/support/SequenceStepAdvancer.java#requireEventSatisfies`（L101-115：仅校验 status=COMPLETED + eventType 匹配）
- **证据**：`ErpCrmEvent` 有 `relatedLeadId` 列且 `createEventForStep` L136 写入 `event.setRelatedLeadId(lead.getId())`——归属字段现成可用但推进链不消费。advanceStep 是双 ID 显式入参 mutation：progressId=A 线索 + eventId=B 线索的同类型已完成事件 → A 序列照常推进 + 为 A 建下一步 Event。owner doc sales-sequence.md §步骤推进语义「当用户创建了**当前步骤类型的活动**并完成」（指该线索的活动）；§5.2「线索状态变化实时更新」。误配后果：序列推进数据污染（转化效果分析 §5 失真）+ 给 A 线索凭空创建排程 Event。
- **问题**：D5 入参边界（跨实体一致性校验缺失）+ D8 联动正确性。
- **建议修复方向**：`advanceStep` 在 advance 前补 `Objects.equals(event.getRelatedLeadId(), progress.getLeadId())` 否则抛 `ERR_SEQUENCE_STEP_NOT_DUE`（带 leadId/eventId 参数）；补跨线索事件拒绝测试。
- **arm-index 裁决**：新增（grep「relatedLeadId 校验/事件 归属」零命中）。

### P2-CK-crm2-006（D5/D8）generateQuote 无 lead 类型/状态守卫且无条件覆盖弱指针——可作用于任意状态线索并破坏原 lead 的 CRM_LEAD 反查指针；raw dao 更新绕过 Lead BizModel 更新钩子

- **控制点**：`app/erp/crm/service/processor/ErpCrmProductConfiguratorGenerateQuoteProcessor.java#generateQuote`（L113-119：`lead = leadDao().getEntityById(leadId); if (lead != null) { leadOrgId/leadCustomerId 读取 }`——**无 leadType==OPPORTUNITY / docStatus==QUALIFIED 校验**；L126-130：`lead.setRelatedBillType(SALES_QUOTATION); lead.setRelatedBillCode(quotation.getCode()); leadDao().updateEntity(lead);`——**无条件覆盖既有指针**；updateEntity 走 raw dao 而非 `IErpCrmLeadBiz`，绕过 `ErpCrmLeadBizModel.defaultPrepareUpdate` 的重评分触发）
- **证据**：CPQ 设计定位（cpq.md §边界）「CPQ 引擎是 CRM `ErpCrmLead` 商机阶段…的输出工具」——隐含 leadType=OPPORTUNITY 前提，实现零校验。反查断链路径：convertToCustomer 后**原 lead** 持 `relatedBillType=CRM_LEAD + relatedBillCode=opportunity.code`（ck-crm-lead「验证为正确」节实证的 getCreatedOpportunity 反查闭环）；对该原 lead 误调 generateQuote（任何 leadId 均被接受）→ 指针被覆盖为 SALES_QUOTATION → `getCreatedOpportunity(原 leadId)` 反查失效。NEW/LOST/CANCELLED 状态线索同样可生成报价并覆盖指针。另：raw `leadDao().updateEntity` 绕过 biz 管道（同域 dao 使用在 ck-crm-lead 校准下可接受，但绕过 defaultPrepareUpdate 重评分 + 数据权限管道是行为差异点）。
- **问题**：D5 状态/类型守卫缺失 + D8 弱指针单槽位覆盖风险。
- **建议修复方向**：generateQuote 前置守卫：leadId 非空时校验 `leadType=OPPORTUNITY && docStatus=QUALIFIED`（否则抛 `NopException`，可复用 ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION 语义或新增错误码）；lead 回写改经 `IErpCrmLeadBiz`（或在注释记录 raw dao 理由，对齐已知失败模式 3 的注释要求）。
- **arm-index 裁决**：新增（grep「generateQuote 守卫/弱指针 覆盖」零命中；P2-RC-038 是方法名/remark 截断维度不同控制点）。

### P2-CK-crm2-007（D3/D4/D8）营销活动状态机与 ROI 计算零实现——ORM 无 status/campaignType/revenue 列、无任何迁移 mutation、无 endDate 自动结束 job、无 ROI 公式（设计-实现分歧登记不裁决）

- **控制点**：`app/erp/crm/service/entity/ErpCrmCampaignBizModel.java`（全文件 19 行裸 `CrudBizModel` stub——**零 mutation、零维护钩子**）+ orm 实证 `ErpCrmCampaign` 列集（module-crm/model/app-erp-crm.orm.xml L448-470：仅 id/code/name/orgId/campaignName/medium/source/startDate/endDate/budgetAmount/actualCost/remark + 审计列——**无 status、无 campaignType、无 expectedRevenue/actualRevenue、无 ownerId/teamId/isTemplate**）+ grep 实证 service 全目录无 `PLANNING/ACTIVE/ANALYZING` 任何 writer（campaign 状态机零落地）
- **证据**：owner doc marketing.md §1.1 声明 status dict `erp-crm/campaign-status` 四值 + §1.2 五迁移状态机（含 budgetAmount 必填守卫）+ §三 ROI/CPA/CPL 计算公式与 ANALYZING 触发写入 + §八「nop-sys（定时任务）| 活动到期自动结束（endDate 检查 Job）」。实现侧：四迁移 mutation 不存在、campaign-status dict 文件不存在（dict 目录零命中）、ROI/actualRevenue 汇总不存在（`ErpCrmReportBizModel` 仅落地 RC-R1.24 的 attribution 报表——lead 侧 expectedRevenue 聚合，非 campaign 行回写）、endDate 自动结束 job 不存在（app-erp-all job/conf 无 campaign 相关 yaml）。marketing.md §2.4 实现注记只登记了 UTM copy-on-create + 归因报表两项落地，**未声明生命周期/ROI 降级**——非 documented simplification。参照 P1-RC-039 的 §4 三判据精神：无 owner doc 显式降级 + 无 product-scope 裁剪痕迹 → 分歧登记。
- **问题**：D3 状态机整支缺失 + D4 调度缺失 + D8 字段缺失。P2（非 P1：attribution 主路径[UC-CRM-07 L1 验收面]已落地且修复验证在位；缺失部分是设计声明未落地，属范围分歧，须人工/主 agent 裁决 product-scope 是否裁剪——同 P1-RC-039 的「须人工确认 product-scope」处置路径）。
- **建议修复方向**：主 agent 裁决二选一：① product-scope 确认裁剪 → marketing.md §1.1/§1.2/§三补显式 successor/降级注记（纯文档）；② 未裁剪 → ORM 补列（保护区域 dual-agent-approval）+ CampaignBizModel 补 start/finish/close mutation + endDate job + ROI 计算。
- **arm-index 裁决**：新增（grep「campaign-status/活动 状态机/ROI」arm-index 零命中；P1-RC-037/038 是 UTM copy 与 attribution 报表维度且均已 resolved）。

### P2-CK-crm2-008（D4/D8）序列自动分配流程未接线——auto-assign-on-qualify 死配置（默认 true 零消费）+ assignSequence 无匹配时抛错与设计「不分配序列」语义相悖

- **控制点**：`app/erp/crm/service/ErpCrmConfigs.java` L40-42（`sequenceAutoAssignOnQualify()` 默认 `Boolean.TRUE`——grep 实证除定义外**零调用方**）+ L60-63（`sequenceDefaultTemplate()` 默认 NEW_LEAD——同样零调用方）+ `app/erp/crm/service/processor/ErpCrmLeadSequenceProgressAssignSequenceProcessor.java#assignSequence`（L62-65：`if (matched == null || matched.getSequenceId() == null) throw new NopException(ERR_SEQUENCE_NO_MATCH)`——无匹配抛错而非不分配）+ grep 实证 `ErpCrmLeadQualifyProcessor`/`ErpCrmLeadBizModel` 无任何 assignSequence 调用
- **证据**：owner doc sales-sequence.md §业务规则 1 逐字「线索创建 / 进入 QUALIFIED 状态 → 按优先级遍历…仍无匹配 → **不分配序列**（手动选择）」+ 配置点表 `erp-crm.sequence.auto-assign-on-qualify` 默认 true。实现：自动分配整支未接线（qualify/create 路径不触发），唯一入口是手动 mutation `assignSequence`；且该 mutation 在无规则+无默认时抛 `ERR_SEQUENCE_NO_MATCH`——若未来按设计接线到 qualify 热路径，无规则租户的每次 qualify 都会失败（throw 语义与「不分配」no-op 相悖，是接线的隐性阻断）。default-template 兜底序列查找亦未实现（engine 只消费 SequenceAssignment.isDefault 规则）。
- **问题**：D4 触发链接线缺失 + D8 设计语义偏差（throw vs no-op）。
- **建议修复方向**：`ErpCrmLeadQualifyProcessor` 补 config-gated（`sequenceAutoAssignOnQualify`）的 assignSequence 调用（no-match 静默跳过）；assignSequence 无匹配改为可配置返回 null 或仅手动路径抛错；`sequenceDefaultTemplate` 接入 engine 兜底或删除死配置。
- **arm-index 裁决**：新增（grep「auto-assign-on-qualify/序列 自动分配」零命中；A1.29 verdict 只登记 job 测试缺失）。

### P2-CK-crm2-009（D8，同型 orgId 隔离族）切片全部聚合/规则加载查询无 orgId 过滤——跨组织数据混入预测、序列分配、CPQ 定价与三张报表

- **控制点（同型家族 crm2 站点清单）**：`support/ForecastAggregator.java#loadOpportunities`（L239-252 无 orgId）/`#sumConvertedRevenue`（L262-280）/`#loadPersonalForecasts`（L254-260）；`entity/ErpCrmLeadSequenceProgressBizModel.java#loadAllInProgress`（L201-205）/`#loadSequencesByTemplate`（L227-233）；`processor/ErpCrmLeadSequenceProgressAssignSequenceProcessor.java#loadAssignmentRules`/`#loadDefaultRule`（L98-111）；`processor/ErpCrmProductConfiguratorGenerateQuoteProcessor.java#loadActivePriceRules`（L185-198）/`#loadConfigRules`（L160-164）；`report/ErpCrmReportBizModel.java#buildCampaignAttributionDataset`/`#buildLeadConversionFunnelDataset`/`#loadForecasts`（L309-315 forecastId 空时全表）
- **证据**：forecast 行写入侧 `buildForecast` L143 取 `period.getOrgId()` 传播 orgId，但读取侧（商机加载/实际收入汇总）无组织边界——多组织部署下 A 组织的商机金额计入 B 组织 period 的预测行（period 无组织唯一性约束时）；价格规则/配置规则/序列分配规则同理跨组织命中（PriceRule UK=code+org 实证多组织是显式设计维度）。对齐同型族先例（P2-CK-fin2-007/P2-CK-mfg-007/P2-CK-hr-003④/P3-CK-crm-010②）。
- **问题**：D8 orgId 透传缺失（同型族 crm2 站点登记，不复用展开）。
- **建议修复方向**：各加载点按 `IUserContext`/period.orgId 补 `eq("orgId", ...)`；报表数据集至少按调用方 ctx 组织过滤（与家族联合修复统一裁决单租户放宽条件）。
- **arm-index 裁决**：同型登记（orgId 隔离族，crm2 站点新增计数）。

### P2-CK-crm2-010（D7）LeadSequenceProgress 无 (leadId, status) 唯一键——并发 assign/switch 产生双活跃进度行；Forecast 缺设计声明的 period×维度 UK（并发重复面本体复用 P2-MA4-013(a)）

- **控制点**：`app/erp/crm/service/processor/ErpCrmLeadSequenceProgressAssignSequenceProcessor.java#assignSequence`（L51-56 `findActiveProgress` 查重 + L68-75 插入——check-then-insert 无锁竞态）+ orm 实证 `ErpCrmLeadSequenceProgress` **零 unique-key**（module-crm/model/app-erp-crm.orm.xml progress 块）——DB 层无兜底；`ErpCrmForecast` 同样**零 UK**（design sales-forecast.md §ErpCrmForecast 逐字「每个 `periodId × territoryId × teamId × ownerId` 唯一」未落地）
- **证据**：并发双击 assignSequence（或 assign+switch 并发）：两事务都过 findActiveProgress 空检查 → 两条 IN_PROGRESS 行。后果：`findActiveProgress`/switch 的 limit(1) 无排序任取一条（另一条成为僵尸活跃行，被 overdue job 持续扫描提醒）；advanceStep 按显式 progressId 仍可分别推进双份步骤 Event。@BizMutation 事务不提供互斥（平台实证：事务包装≠锁）。Forecast UK 缺失的并发重复行本体已由 **P2-MA4-013(a)**（watch-only）登记——本条不重复登记 forecast TOCTOU，只登记 progress 侧新控制点 + 引用 UK 缺失作为两处共同 DB 兜底缺失面。
- **问题**：D7 并发一致性。P2（双活跃行破坏「一 Lead 一活跃序列」既定方案[owner doc 实现注记]，且僵尸行放大 001 的误报面）。
- **建议修复方向**：orm 补 UK(leadId, status) 或 (leadId, status=IN_PROGRESS 的部分唯一性经冗余 activeFlag 列实现——ORM 变更走 dual-agent-approval）；短期可在 assign/switch 前置 `SELECT ... FOR UPDATE`（IOrmTemplate 锁 lead 行）。
- **arm-index 裁决**：新增（progress 控制点零命中）+ 复用注记（Forecast TOCTOU → P2-MA4-013(a)）。

### P2-CK-crm2-011（D2/D7/D4，同型 P3-CK-crm-015 族）ForecastRecalcJob 直调 biz 无事务包装（清旧+重建非原子）+ 顶层失败仅 LOG.error 无告警通道

- **控制点**：`app/erp/crm/service/job/ErpCrmForecastRecalcJob.java#execute`（L60-65 `try { runRefreshForecast(...) } catch (Exception e) { LOG.error("erp-crm-forecast-recalc-failed: periodId={}", period.getId(), e); }`——无 `IErpSysNotificationBiz` 告警）+ `#runRefreshForecast`（L68-70 `forecastBiz.refreshForecast(periodId, ctx)` **Java 直调 biz 接口**——平台实证 `GraphQLTransactionOperationInvoker` L29 仅 GraphQL mutation 入口包装事务，直调无事务）+ `support/ForecastAggregator.java#refreshForecast`（L58-59 `loadOpportunities` 之后 `clearPeriodForecasts`：L282-291 逐 forecast 删 line 删头 + L92-117 逐条 save 重建——中途异常留下「旧已删、新部分写」的部分快照）
- **证据**：与 P3-CK-crm-015（FunnelAggregationJob 直调 refreshFunnel 同型）完全同构：job 路径清旧重建非原子（手动 GraphQL 路径 `ErpCrmForecastBizModel.refreshForecast` @BizMutation ✓ 有事务——差异仅在 job 路径）；job 次日重跑 clearPeriodForecasts 精确清旧自愈（影响窗口 ≤ 一个 job 周期）；顶层 catch 无 notify 告警（B1 job 变体，同型 P3-CK-hr-013 族注记）。SequenceOverdueJob 对照：只读扫描 + per-row 隔离 + notify 派发 ✓（其顶层失败同样仅 LOG.error，随本条家族注记）。
- **问题**：D2 失败无告警闭环 + D7 事务窗口 + D4 job 质量（同型 crm2 站点登记）。
- **建议修复方向**：execute 包 `transactionTemplate.runInTransaction`（或 runRefreshForecast 改经 GraphQL mutation 入口）；顶层 catch 增 notify 告警（复用 best-effort 范式）。
- **arm-index 裁决**：同型登记（P3-CK-crm-015 job 族，crm2 站点新增计数；非 P3 级因 forecast 面板是管理层决策数据、失真窗口内可见）。

### P2-CK-crm2-012（D5/D3，同型 P1-CK-pur-003/P2-CK-crm-007 族）切片实体 CRUD update 无已审守卫——ForecastPeriod 状态/期间可经 update_ 直改绕过 closePeriod/freeze 语义

- **控制点**：`app/erp/crm/service/entity/ErpCrmForecastPeriodBizModel.java`（无 defaultPrepareSave/Update 守卫——`ErpCrmForecastPeriod__update_` 可直改 `status`：OPEN→CLOSED 跳过 accuracy 计算、CLOSED/FROZEN→OPEN **终态复活**重开重算窗口；可直改 periodStart/periodEnd 使已聚合 forecast 与期间定义失同步）+ `ErpCrmSequenceBizModel`/`ErpCrmSequenceStepBizModel`/`ErpCrmSequenceAssignmentBizModel`/`ErpCrmCampaignBizModel`/`ErpCrmForecastLineBizModel`/`ErpCrmForecastAccuracyBizModel`（裸 CRUD：进行中序列的 steps stepOrder/sequenceId 可直改使 currentStepIndex 错位指向错误步骤；ForecastLine 快照/Accuracy 结果可直改篡改管理层视图）
- **证据**：同型全域族（P1-CK-pur-003/P1-CK-sal-004/P1-CK-inv-005/P2-CK-mfg-006/P2-CK-hr-004/P2-CK-crm-007）。本切片最重站点是 ForecastPeriod（命名 mutation 面 requireOpen 守卫完备[freeze/closePeriod 双站点]，CRUD 面零守卫——终态复活后 refreshForecast 可重算已关闭期间并覆盖 FROZEN 冻结语义「不再重新计算」）。PriceRule/BundlePricing/ConfigRule 三实体已有 validate 钩子 ✓（不在本条范围）。
- **问题**：同型登记（crm2 站点：ForecastPeriod status/period 边界 + Sequence 步骤配置 + 快照/准确率行可篡改）。
- **建议修复方向**：`ErpCrmForecastPeriodBizModel` 补 defaultPrepareUpdate：status 变更仅允许经 freeze/closePeriod（CRUD 拒写）；CLOSED/FROZEN 行拒改 periodStart/periodEnd；ForecastLine/ForecastAccuracy 建议 xmeta 只读化。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，crm2 站点新增计数）。

### P3-CK-crm2-013（D6）ownerTeam putIfAbsent 首见优先 + loadOpportunities 无排序——owner 商机跨团队时团队归属不确定，团队 rollup 口径漂移

- **控制点**：`app/erp/crm/service/support/ForecastAggregator.java` L74-76（`if (opp.getTeamId() != null) { ownerTeam.putIfAbsent(owner, opp.getTeamId()); }`——同 owner 多商机不同 teamId 时**首见**生效，遍历顺序来自 `loadOpportunities` L239-252 **无 addOrderField** 的 findAllByQuery，行序不确定）+ L88/L91（owner 行 teamId 与 teamTotals 归属均取该首见值）
- **证据**：owner 的商机 A（team1）与商机 B（team2）并存（reassignLead 改 team 或跨团队建单后可见）：全部金额滚入首见 team——team1 行含 B 金额、team2 行缺失；且不同刷新轮次首见顺序可能不同（无排序），团队行金额非确定。设计 forecast 行唯一键 periodId×territoryId×teamId×ownerId 隐含 owner 单团队假设，实现未收敛该假设。
- **问题**：D6 聚合归属正确性（低频数据形态触发）。
- **建议修复方向**：loadOpportunities 补排序（如 id 升序）使归属至少确定；或 owner 行按 (owner, team) 细分行/多 team 时 team rollup 按 opp 实际 teamId 分组累计。
- **arm-index 裁决**：新增（grep「ownerTeam/putIfAbsent」零命中）。

### P3-CK-crm2-014（D8）声明字段/死配置族——Forecast.currencyId/expectedClosedRevenue、Accuracy.calculatedBy、Period.isCurrent、Sequence.isDefault、Step.isMandatory 永不写入/消费；3 个 CPQ/序列 config 键零消费

- **控制点与证据**（orm 列在、代码零 writer/consumer 逐一实证）：`ForecastAggregator#buildForecast` L154 `forecast.setExpectedClosedRevenue(BigDecimal.ZERO)` 恒零（design §规则 4「CONVERTED → 重新计算 + 累计 expectedClosedRevenue」未落地；下游 `ErpCrmReportBizModel#buildForecastAccuracyDataset` L325 `r.put("expectedClosedRevenue", nz(...))` 把恒零列当报表列输出——forecast-accuracy 报表该列永零）+ currencyId 恒 null（owner doc §实现约定自称「currencyId 取商机币种」——未实现）；`#buildAccuracy` L209-222 不 set calculatedBy（design「计算人」）；`ForecastPeriod.isCurrent` defaultValue=false 无任何翻转 writer（grep 零命中——design「是否当前活跃期间」语义空转，job 用 status+日期替代）；`Sequence.isDefault` 零消费（兜底走 SequenceAssignment.isDefault）；`SequenceStep.isMandatory` 零消费（design「非必须步骤可跳过」——无 skip-step mutation，见 019 关联）；`ErpCrmConfigs` L28-35 `cpqEnableWizard()`/`cpqDefaultCurrency()`（后者 owner doc cpq.md 实现注记已声明无法解析 FK——documented，前者 wizard 归 P2-RC-037 successor）+ L60-63 `sequenceDefaultTemplate()`（归 008）零消费。`ForecastLine.stageName` 恒 null（`#resolveStageName` L193-195）已由 **P2-MA4-010(i)** 登记——复用不重复计数。
- **问题**：D8 死字段/死配置（报表列误导 + 配置面板键无效）。
- **建议修复方向**：expectedClosedRevenue 在 computeAccuracy/closePeriod 链回填或报表列删除；currencyId 取首商机币种写入；isCurrent/isDefault/isMandatory 或落地或 owner doc 登记降级；死 config 键删除或接线。
- **arm-index 裁决**：新增（stageName 维度复用 P2-MA4-010(i)；其余符号 grep 零命中）。

### P3-CK-crm2-015（D4，同型 cron 键漂移家族）forecast-recalc + sequence-overdue 两个 job.yaml 内外层 cron 键不一致——启用需同时配 3 键，description 声明的门控键与 trigger 实际消费键不同

- **控制点**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-crm-forecast-recalc.job.yaml`（L2 `enabled: "@cfg:nop.job.erp-crm-forecast-recalc.enabled|false"` + L7 `cronExpr: "@cfg:nop.job.erp-crm-forecast-recalc.cron-expr|0 3 * * *"` 外层双层键；L4 description 逐字「cron 经 erp-crm.forecast.recalc-cron 配置门控」）对照 `ErpCrmForecastRecalcJob#execute` L48-52 内层门控键 `erp-crm.forecast.recalc-cron` 默认空=跳过；`erp-crm-sequence-overdue.job.yaml` L2/L4/L7 与 `ErpCrmSequenceOverdueJob#execute` L59-63（内层 `erp-crm.sequence.overdue-check-cron`）完全同构
- **证据**：同 P3-CK-crm-014（funnel job）逐字同型：真正启用需同时配 `nop.job.*.enabled=true` + `nop.job.*.cron-expr`（默认可用）+ `erp-crm.*.cron` 非空三键；运维按 description 直觉配内层键则 job 根本不被调度（enabled 默认 false），只配 enabled 则每次 INFO 跳过。对照已修复形态 `erp-crm-lead-scoring-recalc.job.yaml`（cronExpr 消费与内层同键）两 job 未对齐。owner doc sales-forecast.md 配置点表声称「SCHEDULED：…已接线，空值=跳过」——接线存在但键形态漂移。
- **问题**：同型家族（P3-CK-mfg-013/inv-021/sal-023/qa-019/hr-012/crm-014）crm2 两站点。
- **建议修复方向**：与家族联合裁决——job.yaml cronExpr 改消费内层键（对齐 lead-scoring 修复形态）或删内层门控；description 修正。
- **arm-index 裁决**：同型登记（cron 键漂移家族，crm2 站点新增计数 ×2）。

### P3-CK-crm2-016（D9）clearPeriodForecasts 逐 forecast 查行删 N+1 + loadActivePriceRules 全表 active 规则加载后内存过滤

- **控制点**：`app/erp/crm/service/support/ForecastAggregator.java#clearPeriodForecasts`（L282-291：`for (forecast : existing) { lines = lineDao().findAllByQuery(byForecast(id)); for (line : lines) lineDao().deleteEntity(line); forecastDao().deleteEntity(forecast); }`——每 forecast 一次 line 查询 + 逐实体 delete，可下推 `in("forecastId", ids)` 单查询批量删）+ `processor/ErpCrmProductConfiguratorGenerateQuoteProcessor.java#loadActivePriceRules`（L185-198：`findAllByQuery(eq("isActive", true))` **全表** active 规则载入内存再按 productId 过滤——可下推 `or(isNull("productId"), eq("productId", productId))` 单查询）
- **证据**：forecast 删除随期间内 owner/team/territory 行数 × 每行商机数放大（批处理路径，量级中等）；price rule 加载在每次 generateQuote 热路径（规则表规模化后全实体载入）。对照同域正确范式：`ErpCrmLeadBizModel#loadDefaultRule` filter+limit 下推（ck-crm-lead 引证）。
- **问题**：D9 N+1/全表加载（非热路径主害，P3）。
- **建议修复方向**：clearPeriodForecasts 先收集 forecastId 集再单查询删 line；loadActivePriceRules 下推 or-filter。
- **arm-index 裁决**：新增（grep「clearPeriodForecasts/loadActivePriceRules」零命中；同型注记 P3-CK-hr-014 全实体家族）。

### P3-CK-crm2-017（D10，同型 P3-CK-crm-012 族）坏配置冒泡——SequenceAssignmentEngine 坏 JSON 与 ProductConfigRuleEngine 坏表达式无隔离，单条损坏配置阻塞整个 assign/报价生成操作

- **控制点**：`app/erp/crm/service/support/SequenceAssignmentEngine.java#ConditionMatcher#parse`（L195-206：`JsonTool.parseNonStrict(json)` 异常直接冒泡，仅处理「非 Map」返回空分支——与 TerritoryAssignmentEngine 同名缺陷逐字同型）→ `#matches` L85-103 无 try/catch → `assignSequence` L58-65 循环内无隔离；`app/erp/crm/service/support/ProductConfigRuleEngine.java#evalCondition`（L85-98：编译失败 `throw e.param("conditionExpression", expr)` 直接冒泡——`evaluate` L55-67 无 per-rule 隔离 → 单条坏表达式使 `generateQuote` 整体失败）
- **证据**：同 P3-CK-crm-012（territory conditionValue 坏 JSON 阻塞 lead 创建）模式：管理员录入一条非法 JSON/表达式后，每次 assignSequence（或含该规则的 generateQuote）抛异常。差异缓解：本两处是显式手动 mutation（非 lead 创建热路径），错误带 param 定位——降 P3。对照同引擎正向范式：`matchCustomField` L166-172 per-field try/catch 跳过 ✓。
- **问题**：D10 配置边界（同型族 crm2 站点）。
- **建议修复方向**：parse/evalCondition 捕获异常返回不匹配 + WARN（规则 code 定位），与 crm-012 家族联合修复。
- **arm-index 裁决**：同型登记（P3-CK-crm-012 坏配置冒泡族，crm2 站点新增计数 ×2）。

### P3-CK-crm2-018（D6）getSequencePerformance 度量语义漂移——stepDropOffRate 实为序列级跳过率（非设计步骤级流失率）；avgCompletionDays 整天截断

- **控制点**：`app/erp/crm/service/entity/ErpCrmLeadSequenceProgressBizModel.java#getSequencePerformance`（L157-160 `totalSkipped` 计数 + `dropOffRate = totalSkipped / totalAssigned` 输出为 `stepDropOffRate`；L167 `Duration.between(...).toDays()` 截断小数天）
- **证据**：owner doc sales-sequence.md §5 逐字「按步骤统计流失率：**完成 stepN 但未完成 stepN+1 的比例**」——实现输出的是 SKIPPED 序列数/总分配数（序列级放弃率），步骤级流失（每步的完成→下一步转化）因无 per-step 完成时间戳不可算（数据模型限制，同 001 根因）。avgCompletionDays 用 toDays 截断（0.9 天 → 0）——均值偏低精度。completionRate=COMPLETED/总分配 ✓ 符合设计「序列完成率」。
- **问题**：D6 度量口径（分析只读面）。
- **建议修复方向**：输出键改名 sequenceAbandonRate 或补步骤级流失实现（需 per-step 完成时间，与 001/数据模型联动）；avgCompletionDays 保留 1-2 位小数（minutes/1440 计算）。
- **arm-index 裁决**：新增（grep「stepDropOffRate/序列 流失率」零命中）。

### P3-CK-crm2-019（D5）序列 mutation 无 lead 终态守卫 + 空步骤序列可分配但永不可完成 + switchSequence 错误码语义漂移 + loadDefaultRule 无排序任取

- **控制点**：`app/erp/crm/service/processor/ErpCrmLeadSequenceProgressAssignSequenceProcessor.java#assignSequence`（L49 `leadBiz.requireEntity(leadId, ...)` 后**无 docStatus/leadType 校验**——CONVERTED/LOST/CANCELLED 终态线索可分配/切换/推进序列）+ L78-84（`steps.isEmpty()` 仅跳过建 Event，进度照建——`SequenceStepAdvancer#stepAt` L81-89 对空步骤列表 index 0 越界抛 ERR_SEQUENCE_STEP_NOT_DUE：空序列进度永远卡死且报错误导）+ `processor/ErpCrmLeadSequenceProgressSwitchSequenceProcessor.java#switchSequence` L52-56（未启用序列抛 `ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION`——错误码语义是「状态迁移非法」，此处是「序列未启用」，复用错码）+ `#loadDefaultRule` L105-111（`setLimit(1)` 无排序——多条 isDefault 规则时任取）
- **证据**：同型 P3-CK-crm-016（assignLead/reassignLead 无终态守卫——crm 站点已登记，本条是序列面新站点）；owner doc sales-sequence.md「序列在 docStatus=QUALIFIED 后开始」隐含活跃线索前提。空步骤序列：ErpCrmSequence 无 step 数量下限校验（裸 CRUD 可建零步骤序列）。
- **问题**：D5 守卫缺失（弱影响面：手动 mutation + 配置错误场景）。
- **建议修复方向**：assign/switch 前置 `stateMachine.isTerminal(docStatus)` 拒绝；assignSequence 对空步骤序列抛明确错误（或直接置 COMPLETED）；switchSequence 换 ERR_SEQUENCE_* 语义匹配错误码或新增；loadDefaultRule 补排序。
- **arm-index 裁决**：新增（终态守卫维度同型注记 P3-CK-crm-016；其余零命中）。

### P3-CK-crm2-020（D6）quotation code 毫秒时间戳并发碰撞 + currencyId 缺失误用 ERR_CPQ_NO_PRICE_MATCHED 错误码

- **控制点**：`app/erp/crm/service/processor/ErpCrmProductConfiguratorGenerateQuoteProcessor.java#buildQuotationData`（L225 `data.put("code", "CPQ-" + configurator.getId() + "-" + CoreMetrics.currentTimeMillis());`——同 configurator 同毫秒并发生成两单 → code 相同 → ErpSalQuotation UK(code,orgId) 一胜一败回滚（DB 兜底在位，非数据损坏））+ `#generateQuote` L101-107（currencyId null 时抛 `ERR_CPQ_NO_PRICE_MATCHED`——错误码语义「无价格匹配」，实际是「缺币种」，排障误导；owner doc cpq.md 实现注记已声明 currencyId 须显式提供，本条只登错误码维度）
- **证据**：对照转化链幂等键范式 `"SQ-"+leadId`（业务键锚定）；CPQ 无业务键可用时毫秒戳是弱唯一性。P0-CK-mfg-001 固定幂等键家族核查：本条非同型（键含时间戳非进程固定值，且 UK 兜底失败侧干净回滚）。
- **问题**：D6 唯一性/错误码语义（低危）。
- **建议修复方向**：code 追加随机段或序列号（`StringHelper.generateRandomId`）；currency 缺失新增专用错误码。
- **arm-index 裁决**：新增（grep「CPQ- 时间戳/code 碰撞」零命中）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | crm2 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-pur-003` 族（CRUD update 无守卫，含 crm 站点 P2-CK-crm-007） | ForecastPeriod status/periodStart 直改（绕过 close/freeze/accuracy）、Sequence/Step/Assignment 裸 CRUD、ForecastLine/Accuracy 快照可篡改 | 同型登记为 P2-CK-crm2-012 |
| cron 键漂移家族（mfg-013/inv-021/sal-023/qa-019/hr-012/crm-014） | forecast-recalc + sequence-overdue 两 job.yaml 内外层键 | 同型登记为 P3-CK-crm2-015 |
| orgId 隔离族（fin2-007/mfg-007/hr-003④/crm-010②） | ForecastAggregator 3 查询 + SequenceProgress 2 + Assign rules + GenerateQuote rules + Report 3 数据集 | 同型登记为 P2-CK-crm2-009 |
| `P3-CK-crm-015`（job 直调无事务 + 无告警族）/ `P3-CK-hr-013`（job 告警族） | ForecastRecalcJob 直调 refreshForecast | 同型登记为 P2-CK-crm2-011 |
| `P3-CK-crm-012`（坏配置冒泡族） | SequenceAssignmentEngine.parse + ProductConfigRuleEngine.evalCondition | 同型登记为 P3-CK-crm2-017 |
| `P3-CK-crm-016`（assignLead 终态守卫缺失） | assignSequence/switchSequence/advanceStep 无 lead 终态守卫 | 并入 P3-CK-crm2-019 |
| `P1-CK-crm-001/002`（无转化时间戳/口径混乱根因） | sumConvertedRevenue 以 expectedCloseDate 代理实际关闭时间——forecast 汇总消费同口径数据的关联注记 | 关联注记于 P2-CK-crm2-003（根因修复联动，不重复登记） |
| `P2-MA4-013(a)`（ForecastAggregator TOCTOU watch-only） | 并发 refresh 重复行（含 ErpCrmForecast 无 UK 兜底缺失） | 复用不登记；UK 缺失面引用于 P2-CK-crm2-010 |
| `P2-MA4-010(i)`（resolveStageName stub） | ForecastLine.stageName 恒 null | 复用不登记（注记于 P3-CK-crm2-014） |
| `P1-MA2-086`（10 cron job 并发重复副作用全局裁决） | SequenceOverdueJob 通知重复维度 | 全局已裁决，本切片不重复 |
| `P2-RC-035`（SequenceOverdue/FunnelAggregation job 测试缺失 todo） | TestErpCrmSequenceOverdueJob 仍不存在（grep 实证） | 复用不登记 |
| `P2-RC-037`（CPQ wizard 前端 successor） | `cpqEnableWizard` 后端零消费、交互式规则评估端点不存在 | 复用不登记（wizard 归 successor；evaluate 仅 quote 生成一次性调用非热路径） |
| `P2-RC-038`（createFromConfig→save 漂移 + remark 截断 500） | generateQuote L123 save + L246 truncate | 复用不登记（owner doc cpq.md 实现注记已登记） |
| `P2-RC-039`（configSnapshot 断言弱） | TestErpCrmCpqGenerateQuote 断言面 | 复用不登记 |
| `P2-RC-032` 同类（代理字段匹配 watch-only） | matchProductLine 用 department/utmSource contains 派生（注释自承，与 TerritoryAssignmentEngine 范式一致） | 复用不登记 |
| EMAIL_OPENED/REPLIED 降级 + TASK→event-type 映射 | SequenceStepAdvancer L121-139 | documented 决策（owner doc 实现注记）→「验证为正确」 |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：切片 30 文件 `@Inject private`=0、`System.currentTimeMillis()/LocalDateTime.now()/new Date()`=0（时间一律 `CoreMetrics.currentTimestamp()/today()/currentDateTime()/currentTimeMillis()`——后者为平台批准 API）、`extends RuntimeException/Exception`=0（业务异常全部 `NopException` + `ErpCrmErrors` 中文 ErrorCode）、字符串 `==` 比较=0（`Objects.equals`/`.equals()` 全覆盖，含 `requireOpen` 的常量前置写法）、`printStackTrace`=0。
- **负折扣/超 100% 折扣入参校验在位（任务 D5 点名核查）**：`ErpCrmPriceRuleBizModel#validateDiscounts`（discountPercent 0-100、discountAmount 非负，save+update 双钩子）+ `validateQtyRange`（min<=max）+ `validateEffectiveDate`（from<=to）；`ErpCrmBundlePricingBizModel#validateDiscount`（PERCENTAGE 0-100 / FIXED 非负双分支）。`PriceRuleEngine#applyRule` L149-164 的 percent 路径虽无 clamp，但入口校验已拒负值/超百（负 basePrice 场景归 P2-CK-crm-007 负 expectedRevenue 族——非本切片控制点）。
- **准确率公式与除零（任务 D10 点名核查）**：`accuracyOf` L228-235 `1 - |预测-实际|/MAX(预测,实际)`，`max.signum() <= 0` 返回 1——预测与实际均为 0 时 1.0，与 owner doc §实现约定逐字一致；divide scale 4 HALF_UP；deviationAmount=|commit-actual| 独立维护 ✓。
- **期间边界无重叠**：`loadOpportunities` L245-250 `ge(start)+le(end)` 日期粒度闭区间，MONTHLY 相邻期间无共享日 → 无 P3-CK-crm-011 同型 off-by-one。
- **加载口径对齐设计规则 1**：`eq(leadType, OPPORTUNITY) + eq(docStatus, QUALIFIED) + expectedCloseDate BETWEEN` 与 sales-forecast.md §业务规则 1 逐条一致；ownerless 商机排除 + 公司=Σowner 恒等式与 §实现约定「ownerless 不进任何 totals」一致（L69-72 owner null continue 在 territory 收集之前 ✓）。
- **territory tier rollup 修复在位（P1-RC-039/RC-R1.25 验证）**：L107-113 byTerritory leaf-exact 行（有商机直接归属的节点才生成行、行金额=直接商机 Σ、不进 companyTotals 不双计）；TestErpCrmForecastTerritoryRollup 存在。
- **FROZEN/CLOSED 终态守卫双站点**：`ForecastAggregator#requireOpen` L304-311（refresh 入口）+ `ErpCrmForecastPeriodBizModel#requireOpen`/ClosePeriodProcessor `requireOpen`（freeze/close 入口）——命名 mutation 面无终态复活（CRUD 旁路归 P2-CK-crm2-012）。
- **closePeriod 事务原子**：@BizMutation 经 GraphQL 入口（平台实证 L29 mutation→事务包装）：status 置 CLOSED + computeAccuracy 同事务——accuracy 计算失败整体回滚，无「已关无准确率」悬挂中途态。
- **generateQuote 跨域链原子**：@BizMutation（`ErpCrmProductConfiguratorBizModel#generateQuote`）——quotationBiz.save + lead 弱指针回写同事务；`IErpSalQuotationBiz.save` 跨域经 I*Biz ✓（0549-2 范式，P2-RC-038 documented）。
- **配置器守卫齐**：`requireConfiguratorActive` L136-155 null/不存在/isActive/有效期 from/to 四段校验。
- **配置规则引擎语义正确**：conditionExpression 优先于单行 source（L57-62）；EXCLUDED 禁用优先不被后续覆盖（L109-117）；sequence 升序（L51-52）；matchSource 空 sourceCode 恒真 + 空 sourceValue 通配（L71-83 对齐设计伪代码允许无条件规则）；XLang scope 绑定 selectedFeatures + allowUnregisteredScopeVar 与 owner doc 实现注记一致。
- **价格规则引擎排序正确**：ruleTypeRank CUSTOMER_SPECIFIC(0)>PROMOTIONAL(1)>VOLUME(2) + priority 小者先（L76-78），对齐 cpq.md §4 优先级；priceOverride 优先→percent→amount 顺序（L149-164）与实现注记一致；期间/数量区间空端开放（L103-119）。
- **捆绑计算正确**：bundleAmount 覆盖优先（L39-41）；PERCENTAGE scale 6 HALF_UP + setScale 保精度（L51-53）；FIXED clamp 非负（L56-59）；空行/空字段跳过（L76-83）。
- **步骤完成条件映射正确（documented 决策）**：TASK→event-type TASK（字典在位实证）；EMAIL_OPENED/REPLIED 降级 eventType=EMAIL+COMPLETED（owner doc 实现注记明示 successor）；step-completion-condition dict 5 值（含 TASK_DONE）advancer 全分支处理；activity-type 字典无 TASK 与常量无死值。
- **eventBiz.saveEntity 平台 API 合法**：`CrudBizModel.saveEntity(T, String action, IServiceContext)`（平台源码 L1988，含唯一性/数据权限/状态机初始化）——序列三 Processor 建事件走 biz 管道非裸 dao ✓。
- **乐观锁全在位**：切片 13 实体 orm 逐实体 `versionProp="version"` 实证（Campaign/ForecastPeriod/Forecast/ForecastLine/ForecastAccuracy/ProductConfigurator/ConfigRule/BundlePricing/BundlePricingLine/PriceRule/Sequence/SequenceStep/SequenceAssignment/LeadSequenceProgress）。
- **beans.xml 接线完整（D4）**：切片 16 bean（ForecastAggregator/3 引擎/3 序列引擎/6 Processor/2 job）全部注册于 app-service.beans.xml（L36-37/49-50/70-92/138-153 逐 bean 核对）；job.yaml invoker bean id 对上。
- **dict 无死状态（D3）**：forecast-period-status 3 值（OPEN=创建态、CLOSED=closePeriod、FROZEN=freeze 全有 writer）；sequence-progress-status 3 值（IN_PROGRESS=assign/switch、COMPLETED=advance、SKIPPED=switch 全有 writer）；forecast-category 3 值 classifyCategory 全路径产出。Campaign 无 status 列故无 dict 死值问题（缺失面归 P2-CK-crm2-007）。
- **无 Pattern B 绕过面**：erp-crm-service `_vfs` 无任何 `*.xbiz.xml` override（find 实证）。
- **enforceRuleLimit 计数语义正确**：新建 `count >= max` 拦截第 max+1 条、更新 `count > max`（count 含自身）不误拦临界更新（ErpCrmConfigRuleBizModel L52-70）。
- **advanceStep 事件状态前置校验**：requireEventSatisfies 拒绝非 COMPLETED/类型不匹配事件（L103-115）——设计「不匹配→仍为待办」在显式 mutation 语境下以明确报错实现，行为可接受（非缺陷）。
- **测试基线存在**：切片 8 个测试类在位（TestPriceRuleEngine/TestProductConfigRuleEngine/TestBundlePricingCalculator/TestErpCrmCpqGenerateQuote/TestErpCrmForecastAndScoring/TestErpCrmForecastTerritoryRollup/TestErpCrmSequenceAndFunnel/TestSequenceAssignmentEngine/TestSequenceStepAdvancer）——修复阶段落点可用。

## arm-index 复用 or 新增裁决（汇总）

- 新增关键符号（arm-index 零命中）：`countConsecutiveOverdueSteps 前向`/`productCategory 忽略`/`sumConvertedRevenue expectedCloseDate`/`findOpenPeriod limit 1`/`advanceStep relatedLeadId`/`generateQuote lead 守卫`/`campaign 状态机 零实现`/`auto-assign-on-qualify 死配置`/`LeadSequenceProgress UK`/`ownerTeam putIfAbsent`/`expectedClosedRevenue 恒零`/`isCurrent 死字段`/`clearPeriodForecasts N+1`/`stepDropOffRate 漂移`/`空步骤序列`/`CPQ- 毫秒` → **新增**。
- **复用（不重复登记，报告中注记）**：P2-MA4-013(a)（forecast TOCTOU）/ P2-MA4-010(i)（stageName stub）/ P2-RC-035（job 测试缺失）/ P2-RC-037（wizard successor）/ P2-RC-038（save+remark 截断）/ P2-RC-039（snapshot 断言）/ P1-MA2-086（job 并发重复）/ P2-RC-032 同类（productLine 代理字段）/ P1-CK-crm-001/002（转化时间戳根因——关联注记）。
- **同型登记**：P2-CK-crm2-012（pur-003 族）/ P2-CK-crm2-009（orgId 族）/ P2-CK-crm2-011（crm-015 job 族）/ P3-CK-crm2-015（cron 键漂移家族）/ P3-CK-crm2-017（crm-012 坏配置族）/ P3-CK-crm2-019 内含 crm-016 终态守卫模式。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 2 | P1-CK-crm2-001/002 |
| P2 | 10 | P2-CK-crm2-003..012 |
| P3 | 8 | P3-CK-crm2-013..020 |

按主维度：D6×6（001/002/003/013/018/020）、D5×4（005/006/012/019）、D4×3（004/008/015）、D8×2（009/014）+ 跨计（003/005/006/007/008/010 含 D8、009 含 D9 面）、D3×1（007，跨 D4/D8）、D7×1（010，011 跨 D2/D7）、D2×1（011）、D9×1（016）、D10×1（017）。（精确主维度归属：001 D6、002 D6、003 D6、004 D4、005 D5、006 D5、007 D3、008 D4、009 D8、010 D7、011 D2、012 D5、013 D6、014 D8、015 D4、016 D9、017 D10、018 D6、019 D5、020 D6。）

同型/复用裁决：同型登记 5 条 finding（011/012/015/017 显式同型 + 009 orgId 族；019 内含 crm-016 模式注记）+ arm-index 复用不登记 9 项 + 关联注记 2 项（003←crm-001/002；010←MA4-013a UK 面）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 30 文件逐行深读（ForecastAggregator 402 / GenerateQuoteProcessor 311 / LeadSequenceProgressBizModel 311 / SequenceAssignmentEngine 240 / SequenceStepAdvancer 211 / PriceRuleEngine 200 / ProductConfigRuleEngine 185 / 3 序列 Processor 132-170 / 2 job 84-118 / BundlePricingCalculator 113 / 10 薄 BizModel 桩确认 / ReportBizModel 数据集段 + forecast-accuracy dataset）；平台源码实证 2 处（`CrudBizModel.saveEntity` 签名与管道、`GraphQLTransactionOperationInvoker` mutation 事务门控）；orm 核对（13 实体列集/versionProp 全在位/UK 清单含 Forecast·Progress·Accuracy·Line 无 UK 实证/Campaign 无 status 列实证）；接线核对（beans.xml 16 bean + 2 job.yaml 键形态 + 10 dict 值集与常量逐一相符 + 无 xbiz override）；配置消费面 grep（sequenceAutoAssignOnQualify/sequenceDefaultTemplate/cpqDefaultCurrency/cpqEnableWizard/auto-create-period 零消费实证；funnelRetention 归 crm-015）；前端消费面抽查（ErpCrmPriceRule.view.xml 类别列可达性）；测试文件清单核对（8 测试类 + testScanOverdueSteps 盲区定位）；arm-index crm 相关 RC/MA 条目逐条裁决。
- **未深查**：`ErpSalQuotationBiz.save` 内部校验链（sales 域——generateQuote 传入的 docStatus/approveStatus 字面量与 sales 状态机兼容性，归 C2.2 已查域交叉引用）；`ErpCrmEventReminderJob` 本体（归 A1.29）；`erp-crm-web` 其余 AMIS 页面契约 drift（归 C8.2）；测试代码正确性（仅用于行为语义交叉验证）；XLang 表达式求值安全面（conditionExpression 为管理员配置面——信任边界评估超出本切片 D1-D10 范围）；`scheduler.yaml`/nop-job-local 加载器对 job.yaml 的装载机制（沿 funnel/scoring 同形推定，job.yaml 即注册）；`ErpCrmQuoteTemplate`（orm 有 templateContent/isDefault 列但设计 cpq.md 未声明该实体——无设计契约故无 drift 可判，templateContent 无消费者注记于 014）；`wizardLayout` JSON 的前端解析（P2-RC-037 successor 面）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-crm2-001**（overdue 前向扫描）——「正确」语义需 owner doc 裁决：注释声明的反序扫描意图 vs 可能的「迟到历史计数」意图；若产品意图是「当前步逾期即提醒（不看历史）」则修复方向完全不同。建议以一个「按期推进到 step 5」的实测用例向主 agent 演示误报再定性。
  2. **P2-CK-crm2-007**（campaign 状态机/ROI 零实现）——需求分歧类：须按 P1-RC-039 §4(iii) 同路径人工确认 product-scope 是否裁剪 campaign 生命周期；本报告只登记不裁决。
  3. **P1-CK-crm2-002**（productCategory 忽略）——若产品意图是「本期类别维度仅展示不参与匹配」，应走 owner doc 显式降级 + 前端隐藏列（降 P3）；当前三层宣示（设计表/ORM 列/前端可编辑）使 P1 定性基于「配置陷阱可达」，请主 agent 复核配置面实际使用频率后定级。
  4. **P2-CK-crm2-003** ②（closePeriod 无期末终刷）——若产品语义是「准确率对比用户最后一次主动提交的预测」（Salesforce 语义近似），则无终刷是特性非缺陷，仅需 owner doc 澄清。
