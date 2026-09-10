# 05 — 平台内置能力使用（nop-rule / notify / nop-report / nop-job / N+1）

> 规则依据：conformance 技能维度 8「科目映射、容差校验、审批条件用 nop-rule，不硬编码」、维度 9「跨域事件用 nop-message、报表用 nop-report、作业用 nop-job」+ ai-defaults.md VFS/平台 helper 强制
> 平台能力采用现状：nop-job ✅（35 job.yaml + 12 batch.xml，0 自研定时循环——24 个 *Job.java 全部为 BeanMethodJobInvoker 薄壳）、nop-report ✅（26 个 .xpt.xml 覆盖 11 域，11 个 ReportBizModel 全部注入 IReportEngine 委托 xpt 渲染，无自建报表引擎）、nop-wf 不用 = owner doc 裁决、**nop-rule ❌ 0 采用**（_vfs 无 /nop/rule/ 目录、0 规则文件）、**nop-message ❌ 未采用**（notify 自建）。

## 1. [major] nop-rule 替代候选（自研规则引擎/硬编码规则 Top）

已做对的数据驱动（不计违规）：ct 审批矩阵（ErpCtApprovalMatrix）、返利档位、SLA 策略表、b2b EDI 映射表（ErpB2bCodeMapping）、md 价格校验级别（dict）、hr 个税税率表（taxBrackets JSON）/社保/考勤宽限、SPC 判异规则集（chart.ruleSet 数据驱动）、mnt 维护计划 TIME/RUNTIME（实体列驱动）、aps 替代路由选择（完全数据驱动）。

最应迁移 nop-rule 的站点（按收益排序）：

1. **[major] crm LeadScoringEngine 自创公式 DSL** — `module-crm/erp-crm-service/.../support/LeadScoringEngine.java:200-231`（"ENGAGEMENT_SCORE" 魔法串、"count×N" 解析、parseInt 容错）+ `:148-170` 阈值→动作矩阵 + JSON LOOKUP 表解析，417 行手工规则引擎 → nop-rule 决策矩阵 + XLang 表达式直接替代（purchase 域 ScorecardCalculator 已示范用 XLang 而非自研）；收益 = 去掉一门私有 DSL、评分规则热更新免编译
2. **[major] finance GL 科目映射自研优先级链** — `ErpFinGlMappingResolver.java:99-183`（priority+specificity 排序、7 维 matches、自管 ConcurrentHashMap 缓存 + 自管 CRUD 失效/TTL）→ nop-rule 决策表（内建匹配+缓存），Resolver 保留为适配门面；`ErpFinTransferPriceResolver.java:93-116` 转移定价自研匹配同型
3. **[major] md SKU 价档映射** — `ErpMdMaterialSkuBizModel.java:387-422`（billType→价档 4 分支 + 四档最小正值派生硬编码）→ nop-rule/字典表
4. [minor] finance 坏账账龄矩阵 — `BadDebtProvisionCalculator.java:41-98`（30/60/90/180 分桶 if-else + 5 档损失率散点 config）；`ErpFinReportBizModel.java:714-719` `bucketOf`（30/60/90）与 Calculator 口径独立硬编码存在分叉风险 → 共享决策表
5. [minor] drp 供应商评分权重 — `ErpInvDrpLeadTimeProcessor.java:55-58`（WEIGHT_ON_TIME=40/STABILITY=30/QUANTITY=20/QUALITY=10 常量直参合成）+ `ErpDrpConstants.java:131-133` 等级阈值 90/75/60
6. [minor] hr 薪资默认规则 — `PayrollCalculator.java:40-42`（DEFAULT_OVERTIME_HOURLY_RATE=50、DEFAULT_REQUIRED_WORK_DAYS=22 编译期常量；同文件 ErpHrConfigs 已示范 config 化）
7. [minor] purchase 三单匹配容差 — `ThreeWayMatcher.java:57-59/158-175`（config 已化但为全局单值 5%；strict/POST_DIFFERENCE 字符串三分支）→ nop-rule 决策表按供应商/物料类别/金额档位差异化
8. [minor] sales 定价匹配矩阵 — `ErpSalPricingRuleEngine.java:98-140`（期间/客户组/币种/门槛匹配语义 + `:155-161/:223-228` 类型分派 if-else 硬编码；规则数据已实体化）
9. [minor] qa AQL 抽样方案配置化缺位 — `ErpQaSamplingPlanBizModel.java:11-16` 抽样方案表纯 CRUD 挂空（业务流不读、样本量人工录入）——若产品裁决「人工录入」应登记，否则接入 lotQuantity×AQL level→sampleSize 推导（**待复核**）
10. [minor] cs 分值区间 — `NpsClassifier.java:17-24`（9-10/7-8/0-6）、`ErpCsSurveyBizModel.java:80-88,135-143`（CSAT/NPS/CES 区间）、`TicketPriorityRank.java:16-20`
11. [minor] ct 组合矩阵 — `ErpCtContractBizModel.java:441-448,592-607`（contractType↔direction 矩阵 + "-RN" 续期后缀策略）
12. [minor] inventory 成本策略 — `CostMethodResolver.java:47-55`（isSupported 7 值白名单重复 dict 值域）；`CostAdjustmentService.java:109-113`（applyLine if-else 分支 FIFO vs average-like，而 costing 包 7 个 CostingStrategy Bean 未被复用）

## 2. notify 子系统平台对齐评估（半自建）

外发通道已用平台 nop-integration-api SPI（IEmailSender/ISmsSender，`NotificationDispatcher.java:14-17`）✅；站内信/模板/频控/接收人为自建。

1. **[blocker] 邮件/短信收件人断链** — `module-notify/erp-notify-service/.../dispatch/NotificationDispatcher.java:177-195` — `sendEmailIfPossible`/`sendSmsIfPossible` 构造 EmailMessage/SmsMessage 只设 subject/text，**从不设置收件人地址**（userId→email/手机号映射缺失），EMAIL/SMS 通道实际不可达（Noop sender 掩盖）→ 补 userId→NopAuthUser 邮箱/手机解析，或明确声明通道未接线
2. [major] 同步请求内派发 N+1 写放大 — `NotificationDispatcher.java:76-99` + `NotificationMergeCoordinator.java:56-76` — 每接收人一次 findMergeable 查询 + 每候选一次 isRead 查询 + 每通知一次 save；外发在业务事务线程内同步执行 → 迁 nop-message 异步总线（MergeCoordinator 注释已自认「归 nop-message 异步总线后继」）
3. [major] 自建 `${var}` 正则模板渲染 — `NotificationDispatcher.java:43,206-237`（VAR_PATTERN/Matcher）→ 平台 XLang TemplateStringExpression（与 nop-report/xpt 表达式统一语义，复杂表达式可扩展）
4. [minor] 收件人解析器空实现 — `NotificationRecipientResolver.java:175-181` PARTNER 解析器 WARN 返回空（cs 客户通知 IN_APP 占位根因）→ 登记为已知功能缺口
5. [minor] 通知落库直写 — `ErpSysNotificationNotifyProcessor.java:48-55` daoFor 直写绕过 IErpSysNotificationBiz CRUD 管道

## 3. 报表/看板聚合下推

结论：报表栈已是 nop-report（11/11 ReportBizModel 委托 xpt 渲染），无需整体迁移；问题在**数据集聚合 Java 侧过重、DB 下推不足**：

1. [major] `module-cs/.../report/ErpCsReportBizModel.java:249-251` — `loadTickets` findAllByQuery 无 limit 全量物化后内存聚合 → QueryFieldBean count/sum 下推（`ErpCrmReportBizModel.java:204-239` 的 DB GROUP BY 是正确样板）
2. [major] `module-cs/.../dashboard/ErpCsQualityDashboardBizModel.java:256-269` — `loadClosedTickets` 无界 findAllByQuery（无日期参数时=全历史 CLOSED 工单）内存算 KPI/排名/CSAT → DB 聚合下推或强制默认时间窗
3. [minor] 全仓 11 份 ReportBizModel 的 `resolveReportPath/prepareDataset/renderHtml/download` 近似复制 → 上提 common 基类
4. [minor] md 看板最佳实践参照 — `ErpMdDashboardBizModel.java:73-127` 带 ALERT_MAX_ROWS=5000 硬上限（全仓唯一）；`:80-82` SKU 扫描截断可能导致漏报（materialIdsWithSku 不全时误报无 SKU）→ not-exists 反查
5. [minor] DashboardBizModel 裸查询站点分布（daoFor+findAll/count）：inv 13、qa 10、pur 10、mnt 10、prj 8、md 8、fin 8、mfg 7、ast 6、sal 5、cs 4——多为 runInSession 包裹的 DB 级 GROUP BY 投影（形态健康），逐站点豁免注释覆盖率不一
6. [minor] `module-cs/.../entity/ErpCsTicketBizModel.java:409-459` — `findBoardData` 50 行 Java 看板节点组装（Map 拼装）→ 移 xpt/GraphQL 或专用 BoardBizModel
7. [minor] `module-mfg/.../dashboard/ErpMfgDashboardBizModel.java:269-297` — sumCompletedQtyInRange/computeOnTimeRate 全量物化后内存聚合 → DB 聚合下推

## 4. N+1 查询站点（循环内查库）

| # | severity | 站点 | 问题 | 修复 |
|---|----------|------|------|------|
| 1 | major | `module-mfg/.../mrp/DemandAggregator.java:96-99` | 每销售订单单独查行表（eq("orderId") 循环） | 改 in("orderId", ids) 一次取回 |
| 2 | major | `module-mfg/.../entity/ErpMfgCostRollupBizModel.java:64-68` | 每 FIRMED 卷算头逐个查行 | in(costRollupId, ids)+内存按 businessDate 取最新 |
| 3 | major | `module-mfg/.../costing/ProductionVarianceCalculator.java:360-362,419-424,438-443` | 每卷算头查行 + 每工序 getEntityById(workcenter) 两处 | workcenter 预取 Map / 行表合并 |
| 4 | major | `module-mfg/.../costing/CostRollupService.java:192-198` | 每 BOM 工序 getEntityById(workcenter) | 同上 |
| 5 | major | `module-mnt/.../dashboard/ErpMntDashboardBizModel.java:122-136` | computeOeeList 每设备调 oeeCalculator.computeOee（单次 ~6-8 条查询：StatusLog+mfg 日历/停机/JobCard/TimeLog/WorkOrder/Capacity+qa 回退） | 按 workcenter 批量预载或 SQL 端聚合 |
| 6 | major | `module-qa/.../spc/SpcSamplingService.java:157/247`（经 :367-372） | 循环内逐条 daoFor(ErpQaInspection).getEntityById，而 to-one 已声明 | line.getInspection()（同 `04` §3.4） |
| 7 | major | pur/sal/inv 看板 — `ErpPurDashboardBizModel.java:165,198,234`、`ErpSalDashboardBizModel.java:160,202`、`ErpInvDashboardBizModel.java:159` | getEntityById 循环 | 批量 in() 预载（cs/md/crm 看板已做到） |
| 8 | minor | `module-mfg/.../genealogy/BatchGenealogyWriter.java:236-242` | 每领料单查行表 | 合并 in 查询 |
| 9 | minor | `module-hr/.../entity/ErpHrSurveyResultBizModel.java:148` | getSurveyDashboard 循环内 departmentBiz.get（部门基数小，轻微） | 批量 id-in |
| 10 | minor | `module-qa/.../spc/SpcSamplingService.java:456`（经 :464-474） | 每存量样本一次查询 | 同 #6 |

（`ErpMfgReportBizModel.java:392` 为脚本误报——单行循环后才查询，不计。）

## 5. VFS / 平台 helper / nop-job 复核（全部通过）

- VFS：生产代码 0 处 Files.read*/new FileInputStream/new FileReader/RandomAccessFile；`ModuleMetaReader.java:94` 经 VirtualFileSystem.instance() ✅
- BeanContainer（实体外）：0 处 ✅
- @SqlLibMapper：2 处（sal/pur 价格 Resolver），原子查询语义符合平台定位（sql-lib 定义在 resolver 内嵌注解形态，无散落 .sql-lib.xml）✅
- 自研定时循环：0 处（无 Thread.sleep/@Scheduled/ScheduledExecutorService/Timer）✅
