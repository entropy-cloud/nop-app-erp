# WI-06 open P2 残量全量分诊矩阵（确认阶段报告）

- **任务**：successor-followup-closure-roadmap WI-06 确认阶段——`docs/audits/check/ai-check-index.md` 全部 open P2 逐条对照 HEAD 复核，四档分诊。
- **执行目录**：`docs/audits/check/2026-10-06-2300-successor-wi06/`（本次执行隔离区，只读历史目录，未修改任何现有文件）。
- **HEAD**：`22e2a9b8a`（2026-10-06）。
- **方法学**：`docs/skills/code-history-deferred-triangulation-audit-prompt.md`（隔离纪律即本子目录形态）。同域同型簇（索引行标注「同型 XXX 族」）共享一次代码核验（代表站点核验 + 成员抽查），每行单独给判定与锚点。
- **枚举**：`grep '| open |'` 且级别列 P2 = **113 条**（WI-06 概算 125 条系 roadmap 撰写时点快照；f31x 各批修复后现存 113，无抽样，全量覆盖）。
- **校准锚点采信**：P2-CK-mnt-008 仍存在（f31 补审实证，本次仅复核 `ErpMntDashboardBizModel.java:371` resolveOrgId 零调用点一致）；f311 批 13 条 ast 修复（`77dcc08ca`）索引回填核对 → **ast2-010 为唯一 ast open P2 残留 = 回填缺口**（详见 §4）。

---

## §1 分诊矩阵总表

判定档：**仍存在**（缺陷代码原样在位）/ **已消失**（给修复提交）/ **降级**（缺陷收窄至 P3 及以下，给降级理由）/ **需转注册面**（修复需 owner doc 先行 / 外部仓 / ORM 保护区，给阻塞事实）。

### master-data（5 条：4 已消失 / 1 仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-md-001 | md | 已消失 | `ErpMdSupplierApprovalBizModel.java:207-239` | findEffectiveByPartner 已补 validFrom/validTo 窗口过滤 + validFrom DESC 确定性排序 | 85d38b26c（f3.5） | f3.5 md 批 |
| P2-CK-md-002 | md | 已消失 | `ErpMdMaterialSkuBizModel.java:198-204` | resolvePriceWithSource 已补供应商价格层（对齐四层优先级） | 85d38b26c | f3.5 |
| P2-CK-md-003 | md | 已消失 | `ErpMdMaterialSkuBizModel.java:394` 区 | 未定价 SKU 返回 null，不再兜底 0 | 85d38b26c | f3.5 |
| P2-CK-md-004 | md | **仍存在** | `ErpMdCurrencyRefreshRatesFromApiProcessor.java:67,86-94` | validTo=plusDays(1) 闭区间 2 天 + dao 直写绕过 enforceMutex（`ErpMdExchangeRateBizModel.java:63`）；**plan 2026-09-16-1000-1 对本条勾选 [x] 失实**（85d38b26c 仅触 4 文件、不含本处理器，file git log 证实从未被修复批触及） | — | — |
| P2-CK-md-005 | md | 已消失 | `ErpMdUoMConversionBizModel.java` convertQty | 物料级/通用级反向（倒数）换算 4 分支在位 | 85d38b26c | f3.5 |

### inventory（10 条：全仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-inv-006 | inv | 仍存在 | `ErpInvOwnershipTransferProcessor.java:140` | `divide(quantity, ROUND_HALF_UP)` 无 scale，非整除 ArithmeticException | — | — |
| P2-CK-inv-007 | inv | 仍存在 | 同文件 `:115` | `nz(source.getAvgCost())` 对 FIFO/LIFO/BATCH/SPECIFIC 恒 0 成本转移 | — | 与 inv-006 同文件簇 |
| P2-CK-inv-008 | inv | 仍存在 | `SpecificCostingStrategy.java:174-198` | findSpecificLayers 接收 serialNo 参数但查询体零 serialNo 过滤 | — | — |
| P2-CK-inv-009 | inv | 仍存在 | `ErpInvCostingReclosePeriodCostsProcessor.java:171-200` | recomputeWeightedAverageOutgoing 逐流水重建、balance 逐条回写，顺序依赖公式与 javadoc 不符 | — | — |
| P2-CK-inv-010 | inv | 仍存在 | `InvPostingDispatcher.java:213,254`（+OwnershipTransfer:103、CostAdjustment:91） | setExchangeRate(BigDecimal.ONE) 硬编码 4 站点 | — | 多币种族（含 ct-016） |
| P2-CK-inv-011 | inv | 仍存在 | `ErpInvStockMoveProcessor.java:365-367` + `ErpInvDashboardBizModel.java:125` | 行 totalCost=请求价×量（常 0），看板周转分子读该列 | — | — |
| P2-CK-inv-013 | inv | 仍存在 | `ErpInvStockMoveProcessor.java:316-318` + `FifoCostingStrategy.java:118` | 预留增减/层消耗 plain saveOrUpdate，不走 updateBalanceWithRetry | — | — |
| P2-CK-inv-014 | inv | 仍存在 | `CostAdjustmentService.java:105` | adjustAmount=(new−old)×onHand 重估公式仍覆盖分摊预置额 | — | — |
| P2-CK-inv-015 | inv | 仍存在 | `StandardCostResolver.java:84-102` | FIRMED 全表加载 + 逐 header 行查询 N+1 + 无 orgId 过滤 | — | orgId 族边缘站点 |
| P2-CK-inv-016 | inv | 仍存在 | `ErpInvCostingReclosePeriodCostsProcessor.java:65-72` | move→line→ledger 三层逐条查询期末 N+1 | — | D9 性能族 |

### manufacturing / mfg2 / ast2（4 条：1 已消失 / 3 仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-mfg-009 | mfg | 仍存在 | `ErpMfgWorkOrderDocumentStateMachine.java:133-142,110-116` | cancel 来源 {DRAFT,SUBMITTED,NOT_STARTED}、close 来源 {STOPPED,IN_PROCESS}，STOCK_RESERVED/PARTIAL 双向不可出 | — | — |
| P2-CK-mfg2-005 | mfg | 仍存在 | `MrpReleaseService.java:171-185` + `CrpLoadCalculator.java:259-262,370` | MRP 释放工单不写 routingId/plannedEndDate；CRP distributeByWorkOrder routingId=null 返回 0 + ge(plannedEndDate) 排除 NULL | — | — |
| P2-CK-mfg2-008 | mfg | 仍存在 | `ErpMfgCostRollupBizModel.java:33,55` | FIRMED 仅常量+查询过滤两处引用，零命名 writer、零迁移守卫 | — | — |
| P2-CK-ast2-010 | ast | **已消失** | `ErpAstValueAdjustmentProcessor.java:269-272` | 代码注释明示「P2-CK-ast-016≡ast2-010（分支 A）」：死代码块删除、config 退役 no-op（净值轨迹口径） | 77dcc08ca（f311） | f311（ast-016 合并修复）；**索引行未回填 = §4 缺口** |

### projects（6 条：全仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-prj-009 | prj | 仍存在 | `ErpPrjTimesheetSubmitProcessor.java:56` + `ErpPrjProjectSettlementCreateSettlementProcessor.java:27-42` | nz(hours) 无负值校验；settlementType 仅 FINAL/CLOSE 特判、无字典校验 | — | 入参边界族 |
| P2-CK-prj-010 | prj | 仍存在 | `ProjectSettlementPostingDispatcher.java:49-52` | 失败仅 LOG.warn/error，无 dispatchFailureAlert（工时链 TimesheetPostingDispatcher 有 prj.timesheet-posting-failure 对照） | — | 告警通道族（同 crm2-011/mnt-005） |
| P2-CK-prj-011 | prj | 仍存在 | `ProjectPnlCalculator.java:66` | 默认 periodTo=CoreMetrics.today()，幂等键逐日漂移 | — | — |
| P2-CK-prj-012 | prj | 仍存在 | `MaterialCostAggregator.java:62,75-76` + `ProjectCostAggregator.java:82,95-96` | 存量头仅累加 totalAmount，amountSource/Functional 停留首笔 | — | — |
| P2-CK-prj-013 | prj | 仍存在 | `ProjectSettlementAcctDocProvider.java:138-149` | 局部 fact() 只 setAmount（对照 ProjectCostCollectionProvider.java:107-108 已按 RC-R1.64 双金额） | — | — |
| P2-CK-prj-014 | prj | 仍存在 | `ErpPrjProjectSettlementProcessor.java:227` | daoProvider.daoFor(ErpAstAsset) 跨域直写绕过 IErpAstAssetBiz | — | 跨域写族 |

### quality（5 条：全仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-qa-007 | qa | 仍存在 | `ErpQaInspectionRecordResultProcessor.java:71-74` | 让步路径同事务 setApproveStatus + approvedBy=记录者本人 | — | — |
| P2-CK-qa-008 | qa | 仍存在 | `SpcCapabilityCalculator.java:264-267` | classifyByCpk(null) 返回 INADEQUATE | — | — |
| P2-CK-qa-009 | qa | 仍存在 | `SpcSamplingService.java:375-380,415-419` | findApprovedInspectionLines 无时间窗全量 + findSamples 全样本 + getEntityById 循环 | — | D9 性能族 |
| P2-CK-qa-010 | qa | 仍存在 | `SpcControlLimitCalculator.java:236-243` + `SpcSamplingService.java:124` | D2 表 n=2..10 越界静默回落 n=10；subgroupSize<2 拒绝 X_MR | — | — |
| P2-CK-qa-014 | qa | 仍存在 | `NcrReturnOrchestrator.java:99,117` | 退货单仅头 data map（remark 等），零行实体创建 | — | — |

### hr / mnt（5 条：全仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-hr-005 | hr | 仍存在 | `ErpHrRecruitmentHireProcessor.java`（hire 编排体） | 零 departmentId/positionId 存在性校验（仅员工创建失败与状态迁移错误码） | — | — |
| P2-CK-mnt-004 | mnt | 仍存在 | `ScheduleDueGenerator.java:184-200` | generateVisit data map 无 businessDate 键（全 mnt 域 setBusinessDate 仅备件服务 1 处） | — | — |
| P2-CK-mnt-005 | mnt | 仍存在 | `ErpMntDueVisitJob.java:48-50` + `ScheduleDueGenerator.java:97-111` | job 顶层 catch 仅 LOG.error 无告警无重试；循环仅 DECOMMISSIONED 特例跳过（注释自认无 per-schedule try/catch） | — | 告警通道族 |
| P2-CK-mnt-008 | mnt | 仍存在（校准锚点采信） | `ErpMntDashboardBizModel.java:371` | resolveOrgId 声明处零调用点（本次独立复核一致）；dashboard 查询零 eq("orgId") 接线 | — | **orgId 隔离族族首**（f31 已转 WI-06 首批候选） |
| P2-CK-mnt-009 | mnt | 仍存在 | `SparePartIssueService.java:31` | 仅 ERR_USAGE_LINES_EMPTY；数量/仓库/时长零校验 | — | 入参边界族 |

### hr2（8 条：1 降级 / 1 需转注册面 / 6 仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-hr2-007 | hr | **降级** | `AbstractErpCrudBizModel.java:42-49` + `ErpCrudStatusLock.java:37-53` | F1.3 基类守卫已封头号向量（incoming approveStatus=APPROVED 即拒 update）；ErpHrSalary 有 approveStatus 列→守卫激活。残余面：paymentStatus=PAID 直写绕 280 凭证 + APPROVED→非终态回退向——影响显著低于原登记 | 家族修复（F1.3 统一基类） | P1-CK-pur-003 族 |
| P2-CK-hr2-008 | hr | 仍存在 | `ErpHrLeaveRequestBizModel.java:140-142` vs `:125-129` | checkLeaveBalance `balance==null → return` 放行；getBalance 同数据算负余额 | — | — |
| P2-CK-hr2-009 | hr | 仍存在 | 同文件 `:157` 区 checkDateOverlap | `eq("leaveType", ...)` 重叠守卫仅同假别 | — | — |
| P2-CK-hr2-010 | hr | **需转注册面** | `_app.orm.xml` IDX_HR_SALARY_*（全 unique="false"）+ `ErpHrSalaryBizModel.java:247,255` | 根治 = (employeeId,year,month) UK（**ORM 保护区，auto+dual-agent-approval**）；existsNonVoidSalary 前置检查在位但 TOCTOU 本质不可在服务层根修 | — | UK 缺失族（同 crm2-010/drp-013/cs-008 半边） |
| P2-CK-hr2-011 | hr | 仍存在 | `PayrollCalculator.java:290-298` | findActiveContract 仅 eq(employeeId)+setLimit(1)，无 status 过滤无排序 | — | — |
| P2-CK-hr2-012 | hr | 仍存在 | `ErpHrSalarySimulationApplyBatchAdjustmentProcessor.java:59-69` | `newBasic=basicSalary+adjustment` 对全部 adjustType 生效，ALLOWANCE 错置 | — | — |
| P2-CK-hr2-013 | hr | 仍存在 | `ErpHrSalarySimulationBizModel.java:173,200,228` + `ErpHrSalaryRunPayrollProcessor.java:44-55` | 三个 @BizQuery 全量重算 + runPayroll 逐员工 N×6 查询 | — | D9 性能族 |
| P2-CK-hr2-014 | hr | 仍存在 | `ErpHrShiftRotationPatternBizModel.java:77` + `GenerateRotationProcessor.java:116` | 活跃口径仅 eq(SCHEDULED)；裸 saveEntity 无 UK flush-catch | — | — |

### crm / crm2（15 条：全仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-crm-003 | crm | 仍存在 | `ErpCrmConversionProcessor.java:100-117` | createOpportunityFromLead 字段清单无 territoryId/campaignId | — | — |
| P2-CK-crm-004 | crm | 仍存在 | `QuotaRollupCalculator.java:97-103` | 聚合排除仅当前层显式行（L98-102），中间层显式行与子明细行并存时上级双计 | — | — |
| P2-CK-crm-005 | crm | 仍存在 | 同文件 `:140-166` | distributeAnnual 无既有子行守卫（重复调用翻倍）；`each=total/12` 无末行吸收 | — | — |
| P2-CK-crm-006 | crm | 仍存在 | `LeadDuplicateChecker.java:110-116` | loadNonTerminalLeads 全表加载内存匹配 + 无 orgId | — | orgId 族边缘站点 |
| P2-CK-crm-008 | crm | 仍存在 | `ErpCrmTerritoryBizModel.java:81-91` | defaultPrepareDelete 仅子节点守卫（ERR_TERRITORY_HAS_CHILDREN） | — | — |
| P2-CK-crm-027 | crm | 仍存在 | `ForecastAggregator.java:60,117-118` | 公司行写入点单一（静态读），但 DBG×2 实证后该文件零修复提交（末次触改 818759a61 早于实证）——机制未钉，修复批次须先红测试复现 | — | F3.5 crm 簇（登记注记所指批次实际未执行，见 §4 注 b） |
| P2-CK-crm2-003 | crm | 仍存在 | `ForecastAggregator.java:127-131` | accuracy 循环仅 personalForecasts；team/territory/company 维度行永不生成 | — | — |
| P2-CK-crm2-004 | crm | 仍存在 | `ErpCrmForecastRecalcJob.java:74-77` | eq(OPEN)+setLimit(1) 无排序，每次只重算一个期间 | — | — |
| P2-CK-crm2-005 | crm | 仍存在 | `ErpCrmLeadSequenceProgressAdvanceStepProcessor.java:47-53` + `SequenceStepAdvancer.java`（全文件零 leadId 引用） | advanceStep 不校验事件线索归属 | — | — |
| P2-CK-crm2-006 | crm | 仍存在 | `ErpCrmProductConfiguratorGenerateQuoteProcessor.java:128-130` | 无 lead 类型/状态守卫；无条件覆盖 relatedBill* 弱指针 + raw leadDao().updateEntity | — | — |
| P2-CK-crm2-007 | crm | 仍存在 | `_gen/_ErpCrmCampaign.java`（零 status/campaignType/revenue 属性）+ crm-service 零 ROI | 活动状态机与 ROI 零实现 | — | — |
| P2-CK-crm2-008 | crm | 仍存在 | `ErpCrmConstants.java:153`（零消费）+ `AssignSequenceProcessor.java:63-66` | auto-assign-on-qualify 死配置；无匹配抛 ERR_SEQUENCE_NO_MATCH | — | — |
| P2-CK-crm2-009 | crm | 仍存在 | `ErpCrmForecastBizModel.java`/`ForecastAggregator.java`（零 orgId 引用） | 切片聚合/规则/CPQ/报表跨组织混入 | — | **orgId 隔离族** |
| P2-CK-crm2-010 | crm | 仍存在 | `_app.orm.xml` IDX_CRM_LEAD_SEQ_PROGRESS_*（unique="false"） | 无 (leadId,status) UK；并发双活跃进度（UK 半边 ORM 注记，守卫半边可批修） | — | UK 缺失族 |
| P2-CK-crm2-011 | crm | 仍存在 | `ErpCrmForecastRecalcJob.java:60-65` | 顶层 catch 仅 LOG.error；清旧+重建无事务包装 | — | 告警通道族 |

### cs（10 条：1 降级 / 9 仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-cs-004 | cs | 仍存在 | `ErpCsTicketReopenProcessor.java`（零 isSlaCompleted/deadline 处理） | reopen 不重置 SLA，超时扫描永久排除 | — | — |
| P2-CK-cs-005 | cs | 仍存在 | `ErpCsTicketMatchAndAttachSlaProcessor.java:35-43` | 手动 mutation 无已挂载幂等守卫，matchAndConsumeEntitlement 重复扣减 | — | — |
| P2-CK-cs-006 | cs | **降级** | `ErpCsConstants.java:220` + `ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java:677` vs `_seed_erp-notify.sql:236-237` | 二键之一 cs.entitlement-expiry 已补种（f2.15 Phase 4）；cs.fulfillment-approval-request 模板**仍缺**（消费点在、种子无行）。残余=单键补种，影响半减 | c7ae2ed3f（半修） | f2.15 模板补齐 |
| P2-CK-cs-007 | cs | 仍存在 | `ErpCsCsatReminderJob.java:82-96` | runReminders 每运行全量通知，无 remindedAt 去重载体，REMINDER/EXPIRED 双发 | — | — |
| P2-CK-cs-008 | cs | 仍存在 | `ErpCsSurveyCreateSurveyProcessor.java:64-66`（setLimit(1) 存在性检查）+ survey 实体零 unique 索引 | 并发/重复调查 TOCTOU + reopen 清理不全；DB UK 半边注记 ORM 保护区 | — | UK 缺失族（半边） |
| P2-CK-cs-009 | cs | 仍存在 | ErpCsTicket `status` 列（非 approveStatus→F1.3 守卫惰性）+ `ErpCsTicket.view.xml:432-433` update drawer | 通用 __update 直改 setStatus 绕过 resolve/close 副作用（UI 层 23e7da109 可能已只读，API 通道未封） | — | P1-CK-pur-003 族变体（status 列） |
| P2-CK-cs-010 | cs | 仍存在 | `ErpCsTicketBizModel.java:158` | assign 守卫 `!NEW || assignedToId!=null` 拒绝，ASSIGNED 改派无 mutation 载体 | — | — |
| P2-CK-cs-011 | cs | 仍存在 | `ErpCsReportBizModel.java`（零 orgId 引用） | cs 读面 orgId 过滤缺失 + 聚合无界 | — | **orgId 隔离族** |
| P2-CK-cs-013 | cs | 仍存在 | `ErpCsReportBizModel.java:181-197,236` | totalTickets 不过滤 CLOSED；isSlaCompleted 二值分桶污染 | — | — |
| P2-CK-cs-014 | cs | 仍存在 | `app-erp-all/.../erp-cs-sla-scan.job.yaml`（nop.job.* 双键）+ `ErpCsSlaScanJob.java:37-38`（erp-cs.sla-scan-cron 内层门控） | 启用需外层 enabled+cron-expr 与内层 config 三键齐配，description 指内层键 | — | **cron 键漂移族**（f3.2 仅修 8 处、6 个 cs job 未在其中） |

### contract（12 条：全仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-ct-005 | ct | 仍存在 | `ErpCtContractBizModel.java:255-260,480-486` | guardTerminationRecord 仅审记录 PENDING+审批人身份，无合同状态再守卫 | — | 状态机守卫族 |
| P2-CK-ct-006 | ct | 仍存在 | `ErpCtContractStateMachine.java:145-153` | assertCanRejectAmend 仅判 DRAFT 源，全新 DRAFT 一键 ACTIVE 绕签署/法务 | — | 状态机守卫族 |
| P2-CK-ct-007 | ct | 仍存在 | `ErpCtContractBizModel.java:294-303` | 手工 expire 仅状态守卫，无 endDate 到达校验 | — | 状态机守卫族 |
| P2-CK-ct-008 | ct | 仍存在 | `ErpCtDocumentBizModel.java:152,229-236,526-536` | purgeOverdueDocuments 逐条调 purge→checkPurgeRole 要求 IUserContext 角色，job 上下文无用户→逐条隔离跳过全灭 | — | job 上下文族 |
| P2-CK-ct-009 | ct | 仍存在 | `ErpCtSignatureRequestHandleSignatureCallbackProcessor.java:43-48` | eventId 幂等复用 remark 列只记最后一个；旧事件重放穿透 | — | — |
| P2-CK-ct-010 | ct | 仍存在 | `ErpCtRebateTierBizModel.java:13` | 18 行裸 stub extends 基类，零边界/重叠校验 | — | — |
| P2-CK-ct-011 | ct | 仍存在 | `ErpCtRebateAgreementRunAccrualProcessor.java:103-105`（代表站点） | loadAccruedBillCodes 全量加载等 O(N²)/全表扫结构在位 | — | D9 性能族 |
| P2-CK-ct-012 | ct | 仍存在 | `ErpCtRebateAccrualBizModel.java`/`ErpCtSignatureRequestBizModel.java`（零 orgId） | 计提/签章 orgId 不透传 + 聚合无过滤 | — | **orgId 隔离族** |
| P2-CK-ct-014 | ct | 仍存在 | `erp-ct-approval-timeout.job.yaml` + `ErpCtApprovalTimeoutEscalationJob.java:90` | 外层 nop.job.* 双键 + 内层 erp-ct.approval-timeout-cron 三键齐配 | — | **cron 键漂移族** |
| P2-CK-ct-015 | ct | 仍存在 | `ErpCtContractExpiryJob.java:123` | notifyByTier 每日重复派发，无去重载体 | — | — |
| P2-CK-ct-016 | ct | 仍存在 | `ErpCtRebateSettlementBizModel.java:94,127` | 返利发票 exchangeRate 硬编码 ONE | — | 多币种族（含 inv-010） |
| P2-CK-ct-017 | ct | 仍存在 | `ErpCtRebateAgreementRunAccrualProcessor.java:56-57` | periodEnd=asOfDate/today 不钳 agreement.endDate | — | — |

### b2b（7 条：全仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-b2b-002 | b2b | 仍存在 | `ErpB2bAsnHandleInboundWebhookProcessor.java:97-100` + `ErpB2bEdiDocCreateInboundProcessor.java:60-75` | 解析失败 errorDoc relatedBillCode=null → 防重键 (formatId,ASN_INBOUND,NULL) 塌缩 | — | — |
| P2-CK-b2b-004 | b2b | 仍存在 | `ErpB2bConfigs.java:9`（生产零消费，仅 2 测试文件引用） | erp-b2b.enabled 主开关零接线 | — | — |
| P2-CK-b2b-005 | b2b | 仍存在 | `ErpB2bEdiDocBizModel.java:124-129` | retry 无方向守卫，入站 ERROR→TO_SEND；注释 D-B2B-6 defer 仅覆盖「入站重试通道」，方向错置半边不在 defer 内 | — | — |
| P2-CK-b2b-006 | b2b | 仍存在 | `ErpB2bAsnHandleInboundWebhookProcessor.java`（全文件零 quantity 校验） | 负/零/非数值数量全程穿透 | — | 入参边界族 |
| P2-CK-b2b-007 | b2b | 仍存在 | `ErpB2bConfigs.java:15`（零消费）+ `_ErpB2bAsnLine.java`（零 receivedQty） | partial-receipt-enabled 死配置 + 行级计数器零载体 | — | — |
| P2-CK-b2b-008 | b2b | 仍存在 | `ErpB2bAsnMatchPurchaseOrderProcessor.java:80-90` | 超额仅 setRemark("...blockingLevel=WARN") 文本；r3 M1.14 勘误归并 open 复核一致 | — | — |
| P2-CK-b2b-009 | b2b | 仍存在 | 9 个 stub BizModel（approveStatus=0；EdiDoc `state`/Asn `status` 列 → F1.3 惰性） | CRUD 直改无守卫；家族收敛依赖 F2.15 列名裁决 | — | P1-CK-pur-003 族 |

### drp（9 条：全仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-drp-004 | drp | 仍存在 | `DrpEngine.java:127-138` | resetToDraft 仅清 SUGGESTED 行 + totalReplenishmentQty=null 抹已释放事实 | — | — |
| P2-CK-drp-005 | drp | 仍存在 | `ErpDrpLineBizModel.java:69-80` + `DrpReleaseService.java:207,232` | approveLine 只翻状态不回填 approvedQty；release 按 nz(getApprovedQty()) 建 0 数量单 | — | P2-RC-069 家族加重点 |
| P2-CK-drp-006 | drp | 仍存在 | `DrpEngine.java:83` | 直读 param.getSafetyStock()；三级优先链（SafetyStockEngine 侧）未接入 runDrp | — | — |
| P2-CK-drp-007 | drp | 仍存在 | `DrpDemandAggregator.java`（每参数行多次查询扇出结构在位） | 聚合器 N+1 + 未到货/在途全表加载 | — | D9 性能族 |
| P2-CK-drp-008 | drp | 仍存在 | `DrpDemandAggregator.java:176-192,224-225` | 余额按 warehouse+material、在途按 toWarehouse——读侧零 orgId | — | **orgId 隔离族** |
| P2-CK-drp-009 | drp | 仍存在 | `SafetyStockEngine.java:307-313,319-331` | byMonth 仅聚合有出库记录月份，无零填充；EXCLUDE 政策（89015c8a6 起）不解决 KEEP 语义缺零月 | — | — |
| P2-CK-drp-010 | drp | 仍存在 | `SimulationDrpEngine.java:87-96,150` | 仿真产真实 ErpDrpPlan（COMPUTED），可被标准 approve/release 释放 | — | — |
| P2-CK-drp-011 | drp | 仍存在 | `ErpDrpSimulationParamResolver.java:30,80-87` | 进程级 Map 缓存无 TTL，invalidateCache 仅测试调用 | — | — |
| P2-CK-drp-013 | drp | 仍存在 | `_app.orm.xml` IDX_DRP_PARAMETER_*（unique="false"） | 无业务 UK，重复参数行物理可能（UK 半边 ORM 注记） | — | UK 缺失族 |

### logistics（7 条：全仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-log-003 | log | 仍存在 | `ErpLogShipmentHandleTrackingWebhookProcessor.java:66-68` | resolveWebhookSecret 返回 carrier.getCode()（注释自认 mock 语义） | — | — |
| P2-CK-log-004 | log | 仍存在 | `GatewayDispatcher.java:256-263` | findShipmentByTrackingNo 仅 eq(trackingNo)，不按 carrierId 收窄 | — | — |
| P2-CK-log-005 | log | 仍存在 | `AbstractErpLogShipmentDeliveredProcessor.java:207-214` | 单 Shipment DELIVERED 直写订单 deliveryStatus=DELIVERED，无数量 rollup | — | — |
| P2-CK-log-006 | log | 仍存在 | `GatewayDispatcher.java:79,105,124` | 重试循环仅 catch NopException | — | — |
| P2-CK-log-007 | log | 仍存在 | `ErpLogDeliveryBookingBizModel.java:77,157` | markMissed 后 MISSED 仍被 findActive 判重阻断；priorityScore 零消费 | — | — |
| P2-CK-log-008 | log | 仍存在 | 同文件 `:205-206` | validateWindowBookable 以 CoreMetrics.today() 为生效期基准 | — | — |
| P2-CK-log-009 | log | 仍存在 | 6 个 BizModel approveStatus=0（status/freightSettlementStatus 列 → F1.3 惰性） | CRUD 直改无守卫 | — | P1-CK-pur-003 族 |

### aps（6 条：1 已消失 / 5 仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-aps-003 | aps | 仍存在 | `ErpApsCapacityReservationBizModel.java`（16 行裸 stub） | 零 cancel/complete/release 方法，ghost 预留只增不减 | — | — |
| P2-CK-aps-004 | aps | 仍存在 | `ErpApsOperationOrderBizModel.java:357-369` | updateSchedule 仅 null-start 检查，零产能/状态/预留同步 | — | — |
| P2-CK-aps-005 | aps | 仍存在 | `ErpApsSchedulingEngine.java:719-723` | sortByForward (priority, latestEndDateT, sequence, woId)，同 WO 优先级乱序时 sequence 被降序倒挂 | — | — |
| P2-CK-aps-006 | aps | **已消失** | `module-notify/deploy/sql/postgresql/_seed_erp-notify.sql:201-216` | aps.workorder-no-routing / aps.operation-workcenter-missing / aps.dispatch-material-shortage 三模板已补种 | c7ae2ed3f（f2.15 Phase 4） | f2.15 模板补齐；**索引行未回填 = §4 缺口** |
| P2-CK-aps-007 | aps | 仍存在 | `ErpApsWorkOrderScanJob.java` | biz 直调无 runInSession 包裹 | — | — |
| P2-CK-aps-008 | aps | 仍存在 | `ErpApsOperationOrderBizModel.java`（approveStatus=0，`status` 列 → F1.3 惰性） | CRUD 直改绕 13 边状态机 | — | P1-CK-pur-003 族 |

### notify / common（4 条：1 已消失 / 3 仍存在）

| 编号 | 域 | 判定 | HEAD 锚点 | 证据要点 | 修复提交 | 簇归属 |
| --- | --- | --- | --- | --- | --- | --- |
| P2-CK-notify-003 | notify | 仍存在 | `NotificationDispatcher.java:112,125-133,160-166` | merge 命中实例仍进 notifications 列表被 dispatchExternalChannels 逐实例外发 | — | — |
| P2-CK-notify-005 | notify | 仍存在 | 同文件 `:212-216,227-231` | EmailMessage 仅 setSubject/setText、SmsMessage 仅 setText，零收件地址载体 | — | — |
| P2-CK-notify-006 | notify | 仍存在 | `ErpSysTemplateBizModel.java`（approveStatus=0；`status` 列） | Template status 可直改；索引行已注记归 F2.15 裁决 | — | P1-CK-pur-003 族 |
| P2-CK-common-001 | common | **已消失** | `MaskAuditRecorder.java:35-59` | traceId 分桶换代（桶生命周期=单请求），null-traceId 保守多记 | f9748697d（plan 2026-10-05-0400-4） | —；**索引行未回填 = §4 缺口** |

---

## §2 四档汇总统计

| 判定 | 条数 | 占比 | 编号清单 |
| --- | --- | --- | --- |
| **仍存在** | **103** | 91.2% | md-004；inv-006/007/008/009/010/011/013/014/015/016；mfg-009；mfg2-005/008；prj-009..014；qa-007/008/009/010/014；hr-005；mnt-004/005/008/009；hr2-008/009/011/012/013/014；crm-003/004/005/006/008/027；crm2-003..011（9 条）；cs-004/005/007/008/009/010/011/013/014；ct-005..012/014/015/016/017（12 条）；b2b-002/004/005/006/007/008/009；drp-004..011/013（9 条）；log-003..009（7 条）；aps-003/004/005/007/008；notify-003/005/006 |
| **已消失**（修复提交在案） | **7** | 6.2% | md-001/002/003/005（85d38b26c）、ast2-010（77dcc08ca）、aps-006（c7ae2ed3f）、common-001（f9748697d） |
| **降级**（P3 残余） | **2** | 1.8% | hr2-007（F1.3 已封头号向量，残余 paymentStatus 轴）、cs-006（半修，单键补种残余） |
| **需转注册面** | **1** | 0.9% | hr2-010（UK 根治在 ORM 保护区；另有 crm2-010/drp-013/cs-008/b2b-009 等含 ORM 半边注记但主向量可批修，仍归仍存在） |

结构分布（仍存在 103 条内）：orgId 隔离族 5 条（mnt-008/crm2-009/cs-011/ct-012/drp-008）+ 边缘站点 2（crm-006/inv-015）；P1-CK-pur-003 族（status 列变体）5 条（cs-009/b2b-009/log-009/aps-008/notify-006）；cron 键漂移族 2 条（cs-014/ct-014）；多币种族 2 条（inv-010/ct-016）；D9 性能/N+1 族 8 条；告警通道缺失族 3 条（prj-010/mnt-005/crm2-011）；UK 缺失族 4 条（hr2-010/crm2-010/drp-013/cs-008 半边）。

---

## §3 首批收尾建议（12 条，单计划可交付）

排序原则：高危（财务/薪酬正确性、门控绕过）优先 + 同域同文件聚簇（共享一次核验与测试基建）+ 族首带成员。

1. **mnt-008**（orgId 族首，f31 已转 WI-06 首批候选；dashboard 零接线实证在案，修复体最小——接 resolveOrgId 或删死代码改显式过滤）
2. **crm2-009**、3. **cs-011**、4. **ct-012**、5. **drp-008**（orgId 族 4 成员，与 mnt-008 同批交付：同一守卫范式 ×4 域聚合/报表查询，红绿测试模板复用）
6. **hr2-012**（调津贴实际调基本工资——薪酬数据错误，业务危害最高且修复点单文件单方法）
7. **hr2-011**（同文件簇 PayrollCalculator：findActiveContract 补 status=ACTIVE 过滤+生效期排序，与 hr2-012 同计划同域）
8. **inv-006**（除法无 scale——VMI DONE 直接崩溃，单行修复 + 边界测试）
9. **inv-007**（同文件簇：层式计价零成本转移，与 inv-006 同一 Processor）
10. **md-004**（汇率窗口 2 天覆盖 + dao 直写绕 MUTEX——财务基础数据；**兼收口 plan 2026-09-16-1000-1 勾选失实**，修复即可回填该计划注记）
11. **ct-006**（全新 DRAFT 经 rejectAmend 一键 ACTIVE 绕过签署/法务全部门控——合同治理高危，补 amend 来源判别）
12. **drp-005**（approveLine 不回填 approvedQty → 0 数量调拨/采购单，P2-RC-069 家族加重控制点，与家族修复范式对齐）

首批不计入（理由）：cs-004/ct-005 等仍存在但单条修复面跨状态机语义需 owner doc 裁决先行；crm-027 需先钉复现机制（红测试前置工作量不确定）；D9 性能族与 cron 键漂移族建议独立批次（横切面大、风险低）。

---

## §4 索引回填缺口（ai-check-index.md 应更新行，本任务只读未回填）

a. **P2-CK-md-001/002/003/005**（4 行）：已被 85d38b26c（2026-09-16 f3.5，plan 2026-09-16-1000-1）修复，索引 4 行仍 `open`。
b. **P2-CK-ast2-010**（1 行）：f311 批以 ast-016≡ast2-010 合并修复（77dcc08ca，代码注释明示 ≡），ast-016 行已回填 fixed 而 ast2-010 行仍 `open`——此即 f311 校准核对所指唯一 ast open P2 残留，定性为回填缺口而非真阳性。
c. **P2-CK-common-001**（1 行）：已被 f9748697d（2026-10-05，plan 2026-10-05-0400-4）修复，索引行仍 `open`。
d. **P2-CK-aps-006**（1 行）：模板种子已由 c7ae2ed3f（f2.15 Phase 4）补齐，索引行仍 `open`。

**计划簿记漂移（WI-04 同型新实例）**：plan `2026-09-16-1000-1`（f3.5 md 批）对 md-004 勾选 `[x]` 且 Status Note 声称「五条 P2 全部 fixed 终态」，但该提交仅触 4 文件、`ErpMdCurrencyRefreshRatesFromApiProcessor` 全历史零修复提交——md-004 修复为虚报，实际仍存在（本表 §1 仍存在档）。建议随 md-004 修复批次一并订正该计划注记。

**附注**：crm-027 索引行注记「归 F3.5 crm P2 簇」——实际 f3.5 批次为 md 域（2026-09-16-1000-1），crm P2 批次从未执行（plans 目录无对应计划），crm-003..008/027 与 crm2-003..011 共 15 条仍全部 open，与本报告 §1 一致。

---

## §5 注销/降级逐条理由

**已消失（7 条，注销并给提交）**：
- md-001/002/003/005：f3.5 批修复（85d38b26c），HEAD 逐条锚点核验在位（§1 表），索引回填见 §4.a。
- ast2-010：与 ast-016 同根（减值/重估折旧基数死代码），f311 分支 A 以净值轨迹口径合并修复（77dcc08ca），config 退役 no-op 如实披露；§4.b 回填。
- aps-006：三个告警事件模板 2026-09-12 已补种（c7ae2ed3f seed L201-216 实证）；§4.d 回填。
- common-001：traceId 分桶换代修复（f9748697d，13/13 绿 + red-green 在案）；§4.c 回填。

**降级（2 条）**：
- hr2-007：原头号向量「ErpHrSalary__update_ 直写 approveStatus=APPROVED」已被 F1.3 统一基类封锁（shouldBlock 对 incoming approveStatus=APPROVED 拒 update，基类于 common-service 全域生效，ErpHrSalary 有该列→激活）。残余两向（paymentStatus=PAID 直写、APPROVED→非终态回退）影响显著低于原 P2 定级，降 P3 随 hr 域后续批次。
- cs-006：双缺失键之一 cs.entitlement-expiry 已补种（f2.15 Phase 4），权益到期提醒恢复；残余 cs.fulfillment-approval-request 单键补种（1 行 seed + i18n），降 P3。

**需转注册面（1 条）**：
- hr2-010：并发重复薪酬行的根治 = (employeeId, year, month) 唯一键，位于 `module-hr/model/app-erp-hr.orm.xml` ORM 保护区（auto + dual-agent-approval）；服务层 existsNonVoidSalary 前置检查只能缓解 TOCTOU 不能根修。阻塞事实：ORM 列/索引变更须先立 owner doc 裁决 + 双独立子代理批准，不适合普通修复批次。另注：crm2-010 / drp-013 / cs-008 的 UK 半边同属保护区（已在 §1 行内注记），但其服务层守卫半边可批修，故主判定保留仍存在。

## §6 剩余风险声明

- **未深查区域**：各 finding 仅做「缺陷形态是否原样在位」的锚点核验，未重跑行为级验证（如 inv-009 的多流水顺序错误需构造性测试复现）；crm-027 的 ×2 复现机制未钉死（静态单写点 vs DBG 实证的张力已在 §1 注明，修复批次必须红测试先行）。
- **残余风险**：b2b-005 的入站重试通道属 owner doc D-B2B-6 显式 Deferred（触发未满足，不重复登记）；方向错置半边仍存在已入仍存在档。
- **证伪项**：无（本次为分诊复核，不新立 finding；核验中发现的两处索引/计划失真已入 §4）。
