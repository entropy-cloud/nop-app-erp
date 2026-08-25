# ck-finance-period-misc — finance「期间结账与其他」切片实现代码检查报告

> 工作项：C3.4。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话，read-only 检查阶段）。
> 范围：`module-finance/erp-fin-service/src/main/java` 期间结账与其他切片 41 个手写生产文件——期间结账链（`entity/ErpFinAccountingPeriodBizModel`、`statemachine/ErpFinAccountingPeriodStateMachine`、`processor/ErpFinAccountingPeriod{,PreCheck,ClosePeriod,FinalizePeriod,ReverseClose,OpenPeriod,GenerateNextYearPeriods}Processor` 7 类、`close/CloseVoucherWriter`）；期末子服务（`profitloss/ProfitLossClosingService`、`annualclose/AnnualCloseService`、`fx/ExchangeRevaluationService`）；费用报销/员工借款（`entity/ErpFinExpenseClaim{,Line}BizModel`、`entity/ErpFinEmployeeAdvanceBizModel`、`processor/ErpFinExpenseClaim{,Approve,Cancel}Processor` 等审批轴 + `ErpFinEmployeeAdvance{,Approve,Cancel}Processor` 系列、`statemachine/ErpFinExpenseClaim*`/`ErpFinEmployeeAdvance*` 4 状态机）；银行对账（`bankrecon/` 全 7 类 + `processor/ErpFinBankReconciliation{Generate,Post,Reverse}Processor` + `ErpFinBankStatement{ImportStatement,LineAutoMatch,LineManualMatch}Processor` + `entity/ErpFinBank{Statement,StatementLine,Reconciliation,ReconciliationLine}BizModel` + `bank-recon-auto-reverse.batch.xml` + `app-erp-all/.../erp-fin-bank-recon-adj-reverse.job.yaml`）；公司间/合并抵销（`intercompany/ErpFinIntercompanyTransferBizModel`、`intercompany/IntercompanyVoucherGenerator`、`entity/ErpFinIntercompanyMatchBizModel`、`entity/ErpFinConsolidationEliminationBizModel`、`processor/ErpFinIntercompanyMatchRunMatchingProcessor`、`processor/ErpFinConsolidationElimination{GenerateEliminationCandidates,PostElimination}Processor`）；期初余额承载（`entity/ErpFinGlBalanceBizModel`、`entity/ErpFinTrialBalanceBizModel`）。跨域核实：`ErpInvTransferOrderConfirmProcessor`（inventory intercompany 调用点）、`ErpFinBadDebtProcessor`（WRITTEN_OFF openAmount 语义）、`EmployeeAdvancePostingDispatcher#postCashRepay`、`SchemaPropagator#resolveTargetSchemas`、`ErpFinPostingProcessor` BillR 写入/查询语义（businessType= name 一致性）、`model/app-erp-finance.orm.xml`（7 实体 versionProp）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。核心链逐文件深读（期间结账全 8 类 + 3 子服务 + 银行对账 7 类 + intercompany 2 主类全读）+ 测试交叉验证（`TestErpFinExchangeRevaluation`/`TestErpFinAnnualClose` 银行 FX 与年度结转场景）+ arm-index 复用裁决。
> 切片边界：凭证/过账引擎归 C3.1（`ck-finance-posting.md`，已产出）、AR/AP 核销与坏账归 C3.2（`ck-finance-arap.md`，`AdvanceOffsetOrchestrator`/`BadDebtProvision*` 属其范围）、预算成本归 C3.3（`ck-finance-budget-costing.md`）；本切片仅核对其与期间结账链的交点（预算 check hook 调用点、承付凭证 postingType 排除、坏账 allowance 门控）。
> **背景核对结论（勿重复登记项）**：① OA-02 修复**在位**（见「验证为正确」首条）；② lesson 09 B1 preCheck 悬挂覆盖**在位**（PENDING/RETRYING/MANUAL + assets/inventory G4 兜底，见「验证为正确」）；③ `P1-CK-fin-005`（acctSchema null 静默成功）——preCheck **无独立拦截机会**（三个扫描面 unpostedVoucher/unsettledArAp/postingException 全部落空：该场景无凭证、无异常记录、无辅助账行），不构成缓解、不重复登记；④ `P1-CK-fin2-002`（FX 核销红冲不对称）属核销链——本切片 FX 重估（FX 凭证）为不同控制点，已按新控制点正常检查（见 P1-CK-fin4-001/P2-CK-fin4-005）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-fin4-001（D6）银行存款 FX 重估的「账面本位币」基准只聚合本期分录——跨期余额账户每月重复生成全额重估凭证，GL 银行科目金额虚增

- **控制点**：`app/erp/fin/service/fx/ExchangeRevaluationService.java#revalueBankDeposits`（L179 `Map<String, BigDecimal> bookBySubject = aggregateBankSubjectBookFunctional(period.getId());` → L194-196 `BigDecimal revaluedFunctional = sourceBalance.multiply(periodEndRate); BigDecimal bookFunctional = bookBySubject.getOrDefault(acc.getSubjectId(), BigDecimal.ZERO); BigDecimal diff = revaluedFunctional.subtract(bookFunctional);`）+ `#aggregateBankSubjectBookFunctional`（L219-253：`vq.addFilter(eq("periodId", periodId))` 仅本期已过账分录，L243-245 又排除 EXCHANGE_GAIN_LOSS 自身分录）
- **证据**：比较基准两侧量纲不一致——`ErpFinFundAccount.currentBalance` 是账户**累计**当前余额（orm L1057「当前余额」，生产代码无重置 writer），而账面侧 `aggregateBankSubjectBookFunctional` 只聚合**结账当期**的银行科目分录净额。测试 `TestErpFinAnnualClose#testBankFxRevaluationForeignAccount:109-141` 的凭证恰好建在被结账期间内（单期场景 book==累计，碰巧正确），未覆盖跨期账户。且 FX 调整分录被 book 聚合排除（L243-245）——调整永不落入账面基准，下月 diff 不收敛。
- **问题**：任何有期初/前期余额的外币银行账户（运行第 2 个月起的常态），每月结账生成 `X×rate − 0` 的**全额**「重估」凭证（而非汇率变动差额）：银行科目/汇兑损益科目每月被虚增全额重估值，GL 科目余额与实际银行余额彻底脱钩；下月因 FX 分录被排除、book 仍为 0，同一全额凭证**再次**生成。附带缺陷：`bookBySubject` 按科目聚合——同科目多账户时每个账户的 diff 都对着共享 book 计算，互相扣减错配。两个 config（`erp-fin.exchange-revaluation-enabled`、`erp-fin.bank-fx-revaluation-enabled`）**默认均 true**，默认部署即触发。凭证自身借贷平衡（不触发失衡告警），属静默金额错报。
- **建议修复方向**：账面基准改为**累计**口径——聚合该科目自始（或跨全部期间）的已过账分录净额（或维护 FundAccount 的本位币账面余额字段）；同科目多账户需按账户维度拆分 book（或按账户聚合其专属凭证行）。修复时补「跨期账户两月连续结账」回归测试（现测试只覆盖单期）。
- **arm-index 裁决**：新增。`P2-MA4-003(a)` 只登记该方法的**全表扫描性能**维度（watch-only，验证仍在位）；`P1-MA2-022`（resolved，documented simplification）只覆盖 AR/AP 分支的「无前期 reversal 累计漂移」——本条是银行分支**比较基准口径**缺陷，均为不同控制点。

### P1-CK-fin4-002（D6/D8）多账套模式下损益结转/年度结转聚合无账套过滤——每个账套的结转凭证都含全域金额（N 倍重复入账），年初余额 populate 循环互删只余最后账套

- **控制点**：`app/erp/fin/service/profitloss/ProfitLossClosingService.java#close`（L59-67：`for (String schemaId : schemas) lastVoucherId = closeForSchema(period, schemaId, context);`）+ `#closeForSchema`（L71-79：`findPostedVoucherIds(period.getId())` 与行查询 `in("voucherId", voucherIds)` 均**无 acctSchemaId 过滤**，聚合结果却按循环账套写一张凭证 L152-155）；`app/erp/fin/service/annualclose/AnnualCloseService.java#executeAnnualCloseForSchema`（L85-89 → `#subjectNetForYear` L277-292 无账套过滤，每账套各写一张含全域净额的年度结转凭证）+ `#populateNextYearOpening`（L157-161：`clearQ.addFilter(eq("periodId", nextJan.getId()))` **无 acctSchemaId**——多账套循环中迭代 2 删除迭代 1 写入的次年 `ErpFinGlBalance` 行，最终只余最后账套的年初快照）
- **证据**：对照同文件族正确范式——`ErpFinAccountingPeriodProcessor#populateTrialBalanceForAllSchemas`（L333-385）显式按 `l.getAcctSchemaId()` 分组聚合、逐账套独立生成快照行；`#findUnsettledArApCodes`（L450-458）按 orgId+主账套限定（P1-MA2-095 修复范式）。PL/FX/年度三个子服务的 `findPostedVoucherIds`/`findYearPostedVoucherIds` 均无此过滤。
- **问题**：`erp-fin.multi-schema-enabled=true` 且主账套 isPropagate 时：schema A 的损益结转凭证包含 schema B 的收入/费用全额（反之亦然）——每个账套的损益类科目被超额结转、本年利润翻 N 倍；年度结转同型；次年年初余额只保留最后迭代账套（其余账套快照被 clear 删除）。各账套试算平衡表（按行 acctSchemaId 分组）与各账套结转凭证（含全域金额）互相矛盾。默认 `multi-schema-enabled=false` 缓解（降 P1 非 P0 依据），但一旦启用即系统性 GL 错报。
- **建议修复方向**：`closeForSchema`/`subjectNetForYear`/`aggregateYearSubjectActivity` 的行聚合增加 `acctSchemaId` 维度过滤（按行 acctSchemaId 分组或 `eq(line.acctSchemaId, schemaId)` + null 回退主账套）；`populateNextYearOpening` 的 clear 增加 `eq("acctSchemaId", acctSchemaId)`（与写入行同维度）。修复时与 C3.1 `P1-CK-fin-011`（辅助账账套幂等）同批验证。
- **arm-index 裁决**：新增（带复用注记）——`P1-MA2-095`（多账套**读路径**双计，resolved R1.29）与 C3.1 `P1-CK-fin-011`（辅助账生成账套幂等）同族；本条是**结账写路径聚合**控制点，grep「closeForSchema/subjectNetForYear 账套」零命中。

### P1-CK-fin4-003（D6/D5）跨法人调拨凭证金额 = 转移定价「单价」（无数量参与）且 materialId 传 null——凭证金额按单价入账（N 倍失真）+ 物料级定价规则永不命中

- **控制点**：`app/erp/fin/service/intercompany/ErpFinIntercompanyTransferBizModel.java#onTransferConfirmed`（L86 `TransferPriceResult pricing = transferPriceResolver.resolvePrice(fromLegalId, toLegalId, null, businessDate);` 第三参 materialId 恒 null；L99 `BigDecimal amount = pricing.getUnitPrice();` 直接作凭证金额）+ 接口签名（L59-63 无数量参数）+ 调用点 `module-inventory/.../ErpInvTransferOrderConfirmProcessor.java#dispatchIntercompanyPosting`（L57-60 只传 id/仓库/日期，未传数量与物料）
- **证据**：`ErpFinTransferPriceResolver#resolvePrice`（L56-78）返回 `result.setUnitPrice(computeUnitPrice(winner))`——语义为**单价**；`TransferPriceResult` 仅 `unitPrice` 字段（erp-fin-dao dto L12-15），无金额字段。`IntercompanyVoucherGenerator#generatePairedVouchers` 将该值同时写入双方法人的 AR/AP 两侧凭证 `totalDebit/totalCredit` 与行金额（L302-303/L316/L335）。
- **问题**：`erp-fin.intercompany-posting-enabled=true` 时，转移 100 件 × 单价 7 的跨法人调拨，双法人各入账 7（而非 700）——内部应收/收入/成本/应付全部按单价错报，配对抵销与合并报表随之失真。materialId=null 使按物料/物料类别的定价规则（pickBest 的 materialId/materialCategoryId 维度）结构性失效，永远命中组织级兜底规则。config 默认 false 保护（降 P1 非 P0 依据），但该缺陷是功能启用后的**主路径核心计算**错误（非边界），每次跨法人交易必触发。
- **建议修复方向**：扩展 `IErpFinIntercompanyTransferBiz.onTransferConfirmed` 签名携带数量（与物料，或直接传调拨单行聚合）——`amount = unitPrice × Σ数量`；inventory 调用点传 `order` 行数量与物料。属跨域接口变更（finance api + inventory 调用点），修复阶段按 DAG 核对。
- **arm-index 裁决**：新增（grep「unitPrice 数量/intercompany 金额/转移定价 单价」零命中）。

### P2-CK-fin4-004（D8/D6）FX 重估与年度结转聚合均无 orgId 维度——A 组织结账把 B 组织的外币项目/全年凭证重估结转进 A 的账

- **控制点**：`app/erp/fin/service/fx/ExchangeRevaluationService.java#revalueArAp`（L106-112：`notIn("status",...)` + `ne("currencyId",...)` 后**无 orgId/acctSchemaId 过滤**，凭证却写 `period.getOrgId()` L153）+ `#revalueBankDeposits`（L165-169：`ne("currencyId",...)` 后无 orgId，`ErpFinFundAccount` 全表外币账户进入本组织重估）+ `app/erp/fin/service/annualclose/AnnualCloseService.java#findYearPostedVoucherIds`（L320-347：期间仅 `eq("year", year)` 无 orgId，跨组织全年凭证聚合）+ `#sumArApOpenFunctional`（L241-249 无 orgId）+ `#findNextYearJanuaryPeriod`（L350-356：签名收 orgId 参数但查询**忽略**之）
- **证据**：对照 `ErpFinAccountingPeriodProcessor#findUnsettledArApCodes` L450-458——同域同实体查询显式 `eq("orgId", orgId)` + 主账套限定（P1-MA2-095 修复范式）；本切片三个服务未跟进。
- **问题**：多组织部署下，组织 A 的期间结账把组织 B 的外币 AR/AP 开放项、外币银行账户全部重估进 A 的 EXCHANGE_GAIN_LOSS 凭证；组织 A 的年度结转把 B 组织全年分录并入本年利润结转与次年年初余额。单组织默认部署（seed org=1）无影响。`ErpFinArApItem`/`ErpFinFundAccount`/`ErpFinVoucher` 均有 orgId 列，属透传缺失非模型限制。
- **建议修复方向**：三服务聚合查询统一补 `eq("orgId", period.getOrgId())`（银行账户按 `fundAccount.orgId` 或经 statement org 关联）；`findNextYearJanuaryPeriod` 补 orgId 过滤（与 P2-CK-fin4-010 同批修复）。
- **arm-index 裁决**：新增（grep「FX orgId/年度结转 组织过滤」零命中；P1-MA2-095 是账套维度且已 resolved）。

### P2-CK-fin4-005（D6）FX AR/AP 重估未排除 WRITTEN_OFF 状态——部分核销坏账后残留 openAmount 的已核销项仍被重估，生成幽灵汇兑损益

- **控制点**：`app/erp/fin/service/fx/ExchangeRevaluationService.java#revalueArAp`（L107-108 `q.addFilter(notIn("status", Arrays.asList(AR_AP_STATUS_SETTLED, AR_AP_STATUS_CANCELLED)));` ——缺 `AR_AP_STATUS_WRITTEN_OFF`）
- **证据**：同族守卫均排除三态——`ErpFinAccountingPeriodProcessor#findUnsettledArApCodes` L460-463（SETTLED/CANCELLED/**WRITTEN_OFF**）、`BadDebtProvisionCalculator` L57（同）。`ErpFinBadDebtProcessor#executeWriteOff` L171-177：`validateAmount`（L295-301）只要求 `amount <= openAmountFunctional`——**部分核销合法**，核销后 status=WRITTEN_OFF 但 openAmount 可残留正值。
- **问题**：部分坏账核销后的残留开放额（应收已从 Allowance/AR 出账的部分）仍被每月 FX 重估——对着已核销资产生成 AR/汇兑损益分录，AR 科目与 Allowance 口径被污染。全额核销项 openAmount=0 被 `diff==0` 短路（L130-132）无影响，故仅部分核销场景触发。
- **建议修复方向**：notIn 集合补 `AR_AP_STATUS_WRITTEN_OFF`（与 findUnsettledArApCodes 对齐）。
- **arm-index 裁决**：新增（grep「FX WRITTEN_OFF/重估 坏账核销」零命中）。

### P2-CK-fin4-006（D2/D8）合并抵销候选识别非幂等——重复 generateEliminationCandidates 产生重复候选行，postElimination 后 GL 重复抵销

- **控制点**：`app/erp/fin/service/processor/ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor.java#generateEliminationCandidates`（L44-115：对每条 MATCHED 配对 new 1-3 条 `ErpFinConsolidationElimination`，**无任何既有候选去重**；对照同域 `ErpFinIntercompanyMatchRunMatchingProcessor` 的幂等前置 `findExistingPairKeys`——P1-MA2-098 修复范式）
- **证据**：方法内 grep 无 exists/dedup 逻辑；`(periodId, pairKey, eliminationType, matchId)` 无唯一约束兜底（候选实体 UK 未见）。postElimination 只按 candidateId 单条执行——重复候选各自生成 DRAFT 抵销凭证。
- **问题**：`erp-fin.consolidation-elimination-enabled=true`（期末批处理启用，owner doc 配置表）后，同一期间重复运行候选识别（批处理重跑/手动+定时双入口）→ 候选行翻倍 → 逐条 post 后抵销凭证翻倍，合并报表抵销过度。config 默认 false 缓解。
- **建议修复方向**：对齐 runMatching 幂等范式——生成前查同期同 `(pairKey, eliminationType)` 既有候选跳过；或 orm 侧补唯一键（保护区域，dual-agent）。
- **arm-index 裁决**：新增（带复用注记）——`P1-MA2-098`（核销非幂等，resolved）同型不同控制点（elimination 候选生成）。

### P2-CK-fin4-007（D8/D3/D10）intercompany/抵销凭证直写引擎外路径：voucherDate=今天而非业务日期、无期间锁定守卫、科目解析失败静默降级 subjectId=null

- **控制点**：`app/erp/fin/service/intercompany/IntercompanyVoucherGenerator.java#writeIntercompanyVoucher`（L298 `voucher.setVoucherDate(CoreMetrics.today());`——调用方已解析 businessDate 的 periodId（`ErpFinIntercompanyTransferBizModel#resolvePeriodId` L238-255）却丢弃业务日期；直接 `voucherDao.saveEntity` 绕过引擎 `resolveOpenPeriod`/`assertPeriodNotLocked` 期间守卫）+ `#writeIntercompanyReversalFromLines`（L206 同 `CoreMetrics.today()`）+ `#applySubject`（L357-366：`findSubjectByCode` 未命中时仅写 subjectCode/Name、**subjectId 留 null**，不抛错）；`ErpFinConsolidationEliminationPostEliminationProcessor#writeDraftEliminationVoucher` 同型（voucherDate=today L81 附近、subjectId 可 null、`setAcctSchemaId("1")` 硬编码）
- **证据**：对照引擎路径——`ErpFinPostingProcessor` 经 `resolveOpenPeriod`（CLOSED 抛 `ERR_PERIOD_CLOSED`）+ `resolveSubjects`（科目未解析抛错），凭证日期取 event.voucherDate（业务日期，见各 Provider/Dispatcher）。GL 聚合消费方 `ErpFinAccountingPeriodProcessor#populateTrialBalanceForAllSchemas` L352-354 与 `ProfitLossClosingService#closeForSchema` L85-87 均 `if (subjectId == null) continue`——**subjectId null 的凭证行金额从试算平衡/损益结转聚合中蒸发**。
- **问题**：(a) 迟到确认的跨法人调拨（businessDate 落在已 CLOSED 期间）直接向 CLOSED 期间写 POSTED 凭证，无锁定拦截，期间锁语义被旁路；(b) voucherDate（今天）与 periodId（业务日期期间）脱钩——按 voucherDate 解析期间的消费方（引擎 resolveOpenPeriod、报表）与按 periodId 的消费方（TB/损益结转）对该凭证归属不一致；(c) GL 映射回落科目编码在科目表不存在时（如默认 5001/6001/1401/2202 未 seed）凭证照常生成但行 subjectId=null → GL 聚合静默丢金额。config 默认 false 缓解。
- **建议修复方向**：voucherDate 改用 businessDate（调用方透传）；写前补期间状态守卫（复用 resolveOpenPeriod 或显式 assertPeriodNotLocked）；applySubject 解析失败抛 `NopException`（对齐引擎 ERR_SUBJECT_NOT_RESOLVED 语义）。修复时可与 C3.1 `P2-CK-fin-015`（reverseVoucher 旁路）一并审视「引擎外凭证直写」清单。
- **arm-index 裁决**：新增（grep「intercompany voucherDate/科目回落 null」零命中）。

### P2-CK-fin4-008（D2）跨法人调拨过账失败被 inventory 侧 catch(RuntimeException) 吞咽仅 warn——intercompany 凭证缺失无异常工作台记录、无告警、期末不拦截

- **控制点**：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvTransferOrderConfirmProcessor.java#dispatchIntercompanyPosting`（L59-66：`catch (RuntimeException e) { ...warn("intercompany posting failed for transfer {}: {}", ...); }`——不记录 `ErpFinPostingException`、不派发告警）
- **证据**：intercompany 链不走引擎 `IErpFinVoucherBiz.post`（Generator 直写，类 javadoc L25-26 明示避开 Provider 路由）——引擎的 recordPostFailure 异常工作台通道对本链**结构性不存在**；期末 `findUnresolvedPostingExceptionKeys` 只扫异常工作台 + assets/inventory posted 标志，intercompany 凭证缺失三者皆不覆盖（调拨单自身的 posted 标志与 intercompany 凭证无关联）。
- **问题**：`intercompany-posting-enabled=true` 时，调拨确认后 intercompany 凭证生成失败（如转移定价规则缺失抛 ERR_TRANSFER_PRICE_NOT_FOUND 的相邻分支、DB 故障）→ 双法人账套静默缺凭证，仅 warn 日志一条，无补救通道（调拨单 DONE 后无重试入口）。B1 家族（lesson 09）在 intercompany 链的新投影。
- **建议修复方向**：catch 内落 finance 异常工作台（经 `IErpFinVoucherBiz` 侧记录通道或新增 billType=INTERCOMPANY 的悬挂扫描面），或最简：rethrow 由调拨确认事务回滚（强一致，符合「真实故障阻断」语义）。修复阶段 inventory/finance 联合。
- **arm-index 裁决**：新增（带复用注记）——与 `P1-MA4-004`（期间编排吞咽，resolved R1.16）/ `P1-MA2-060`（assets tryPost 吞咽）同型根因，控制点为 intercompany dispatch 链（arm-index grep 零命中；ck-inventory.md 亦未登记）。

### P2-CK-fin4-009（D5）银行对账单导入幂等去重只对比「最近一张」对账单——跨月重导与历史单据去重失效，重复流水可入账

- **控制点**：`app/erp/fin/service/bankrecon/BankStatementImporter.java#findStatementIdByAccount`（L201-210：`q.addOrderField("statementDate", true); q.setLimit(1);` 取最近一张）+ `#existsByRefNo`（L184-190）/`#existsByComposite`（L192-199：均只在该 statementId 的行内查重）
- **证据**：注释自认「经 statement.fundAccountId 关联反查……（避免全表扫描）」——但代价是把账户级幂等收窄为「最近单据级」。设计 `bank-reconciliation.md §业务规则1`：幂等键为账户维度唯一（`(fundAccountId, statementDate, bankTxnCode)`）。
- **问题**：1 月对账单导入后，2 月单已导入，再重导 1 月单（补导/纠错场景）→ 去重只查 2 月单 → 1 月 refNo 全部放行 → 重复流水行进入系统，勾对与调节恒等式被重复金额破坏（同额重复行会命中「多候选 SUSPENSE」或直接双入未达）。
- **建议修复方向**：去重查询改为账户全部对账单的行集（先取该账户全部 statementId 列表再 `in("statementId", ids)` 分批查，或行表冗余 fundAccountId 列）。
- **arm-index 裁决**：新增（grep「对账单导入 幂等/去重 refNo」零命中）。

### P2-CK-fin4-010（D8）次年期间生成/存在性检查/次月定位均无 orgId 维度——自动建次年期间恒挂 orgId="1"，多组织第二家年末结账抛错、反结账门控被他组织误触发

- **控制点**：`app/erp/fin/service/processor/ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor.java#generateNextYearPeriods`（L40-42 存在性查询仅 `eq("year", year)`；L54 `String orgId = existing.isEmpty() ? facade.resolveDefaultOrgId() : existing.get(0).getOrgId();`——closeAnnual 首次调用时次年必为空 → orgId 恒 "1"（`ErpFinAccountingPeriodProcessor#resolveDefaultOrgId` L138-141）；非空时取**任意一行**的 org）+ `ErpFinAccountingPeriodProcessor#hasNextYearPeriods`（L130-136 仅 `eq("year", nextYear)`）+ `AnnualCloseService#findNextYearJanuaryPeriod`（L350-356 忽略 orgId 参数）
- **证据**：`(code, orgId)` 是期间唯一键（orm，C3.1 报告核对）；默认 `period-generate-skip-existing=false`——org A 结账建 2027（挂 org 1）后 org B 结账再调 `generateNextYearPeriods(2027)` → `existing` 非空且 skip-existing=false → 抛 `ERR_PERIODS_ALREADY_EXIST` → **org B 的 12 月 closePeriod 整体失败**；反结账门控 `hasNextYearPeriods` 同理被他组织次年期间误阻断。
- **问题**：多组织部署下年度结转链（自动建期 + 反结账门控 + 年初余额 populate 目标期）系统性串组织。单组织默认部署（org=1）行为正确。次年年初余额 populate 的 `gl.setOrgId(period.getOrgId())`（AnnualCloseService L169）与期间行 orgId="1" 跨组织错配。
- **建议修复方向**：三处查询补 `eq("orgId", ...)` 维度（generate 用触发结账期间的 orgId；默认组织回退保留单组织兼容）；与 P2-CK-fin4-004 的 findNextYearJanuaryPeriod 修复合并。
- **arm-index 裁决**：新增（grep「次年期间 orgId/多组织期间生成」零命中）。

### P2-CK-fin4-011（D9）autoMatch 逐行全量重查——每条未勾对行触发一次窗口凭证全量加载 + 一次账户已勾对行全量加载（N+1）

- **控制点**：`app/erp/fin/service/bankrecon/BankStatementMatcher.java#autoMatch`（L51-73：`for (ErpFinBankStatementLine line : unmatched)` 循环内逐行调 `bankLedgerQuery.findCandidates(...)`）+ `app/erp/fin/service/bankrecon/BankLedgerQuery.java#findCandidates`（L56 `findVoucherIdsInWindow(from, to)` 每行全量查窗口内已过账凭证；L82 `findOccupiedLineIds(fundAccount.getId())` 每行全量查账户全部已勾对流水行——两者均无跨行缓存）
- **证据**：`findVoucherIdsInWindow`（L103-118）无科目过滤，日期窗口重叠的对账单行（同日 ±3 天窗口高度重叠）重复拉取同一批凭证实体；`findOccupiedLineIds` 的结果在整个循环中单调增长却每行重查。
- **问题**：千行级月度对账单自动勾对 = O(N) 次凭证表 + O(N) 次流水行全量查询（每次均为实体全列载入），自动勾对耗时随行数平方级恶化；结果正确性不受影响（occupied 集每行刷新依赖 flush-before-query 可见同事务已勾对行）。
- **建议修复方向**：窗口凭证按对账单日期 min/max 一次加载内存过滤；occupied 集循环外预载一次 + 循环内增量维护（本行勾对命中即加入集合，消除对 flush 时序的隐式依赖）。
- **arm-index 裁决**：新增（`P2-MA4-003`/`P2-MA7-005` 家族为新站点——grep「autoMatch N+1/勾对 逐行」零命中；A7.3 抽样范围是列表 findList 站点未覆盖本批处理路径）。

### P3-CK-fin4-012（D8）反结账不清理试算平衡快照——reverseClose 后 ErpFinTrialBalance 残留旧快照，期间 OPEN 期间无失效标记

- **控制点**：`app/erp/fin/service/processor/ErpFinAccountingPeriodReverseCloseProcessor.java#reverseClose`（L37-90：无 `ErpFinTrialBalance` 清理；清理仅存在于 `ErpFinAccountingPeriodProcessor#populateTrialBalanceForAllSchemas` L336-340 的 close 路径）
- **问题**：反结账（期间回 OPEN、凭证可能被修改/红冲）后，期间名下的 TB 快照行原样残留且 `generatedAt` 不变——报表/看板消费该快照得到与实况脱钩的「已结账」数据，直至下次 closePeriod 重建。无 `stale` 标记可辨。
- **建议修复方向**：reverseClose 增加快照清理（delete by periodId）或置 stale 标记；最低限度在 owner doc 登记该窗口语义。
- **arm-index 裁决**：新增。

### P3-CK-fin4-013（D1）生产代码使用 LocalDate.now()（非可控时钟）——银行对账自动红冲候选窗口

- **控制点**：`app/erp/fin/service/bankrecon/ErpFinBankReconAutoReverseHelper.java#findCandidates`（L117 `q.addFilter(lt("reconciliationDate", LocalDate.now().withDayOfMonth(1)));`）+ 同语义 batch.xml loader（`bank-recon-auto-reverse.batch.xml` 内 `${java.time.LocalDate.now().withDayOfMonth(1)}`）
- **证据**：已知失败模式 4 家族（`docs/skills/README.md §已知失败模式` 4）；同包正确范式存在——`IntercompanyVoucherGenerator` L206/L298 用 `CoreMetrics.today()`。测试无法注入时间控制「跨期候选」边界。
- **建议修复方向**：helper 改 `CoreMetrics.today()`；batch.xml 的 loader 表达式如平台不支持 CoreMetrics 则在 processor 侧二次过滤兜底。
- **arm-index 裁决**：新增（compliance checker R3 只查 System.currentTimeMillis/LocalDateTime.now，LocalDate.now 为漏网变体——建议 checker 规则扩展）。

### P3-CK-fin4-014（D5）expenseType 必填校验误挂「审批开关」配置键下——关闭审批开关同时静默关闭行类型校验

- **控制点**：`app/erp/fin/service/processor/ErpFinExpenseClaimProcessor.java#isExpenseTypeRequired`（L375-378 `Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_EXPENSE_APPROVAL_REQUIRED, Boolean.TRUE);`——键为 `erp-fin.expense-approval-required`，`ErpFinConstants` L50）
- **证据**：全仓无 `expense-type-required` 键（grep `CONFIG_EXPENSE` 仅 3 键：budget-check/approval-required/reason-required）；方法名与消费点（`#requireLinesValid` L203-207 expenseType 必填）语义为「类型必填」。owner doc `expense-claim.md §配置点` 两键均无此键。
- **问题**：`erp-fin.expense-approval-required=false`（业务上只想免审批）时行 expenseType 校验被一并关闭——空类型报销行可过审进费用归集。配置键语义耦合 + 疑似 copy-paste 键误。
- **建议修复方向**：新增独立键 `erp-fin.expense-type-required`（默认 true）；若有意耦合须在 owner doc 配置表补登记。
- **arm-index 裁决**：新增。

### P3-CK-fin4-015（D3）BankStatement docStatus 永驻 DRAFT——设计声明的 DRAFT→POSTED→CANCELLED 状态机未接线（dict 死状态 + posted 三件套未用）

- **控制点**：`app/erp/fin/service/bankrecon/BankStatementImporter.java#importStatement`（L72 `head.setDocStatus(ErpFinConstants.VOUCHER_STATUS_DRAFT);` 为全仓唯一 statement docStatus writer；grep `bankrecon/` setStatus/setDocStatus 其余 3 处全在 `BankReconciliationBuilder` 写 recon 实体）
- **证据**：owner doc `bank-reconciliation.md` ErpFinBankStatement 实体表与 §schema 补注「状态机（docStatus 复用 erp-fin/voucher-status）：DRAFT → POSTED（差异已处理）→ CANCELLED（红冲调整凭证）」——声明在**对账单**实体上；实现把 POSTED/CANCELLED 流转全部落在 `ErpFinBankReconciliation`，statement 侧 POSTED/CANCELLED 成死状态（B2 dict 死状态家族），实体表的 posted/postedBy/postedAt 亦未使用。
- **问题**：对账单生命周期状态不可从数据面判别（已调节完成的对账单仍显示 DRAFT）；与设计声明的状态机漂移。影响面为展示/查询语义，勾对与调节功能本身自洽。
- **建议修复方向**：调节表 post/reverse 时联动 statement docStatus（POSTED/CANCELLED），或修订 owner doc 把状态机声明迁移到 recon 实体（doc 侧收敛，登记偏离）。
- **arm-index 裁决**：新增（带复用注记）——lesson 10 dict 死状态家族新站点。

### P3-CK-fin4-016（D6）REVENUE_COST/INVENTORY_PROFIT 抵销额复用 AR/AP 配对余额（存量当流量）——代码注释标「简化」但 owner doc 未登记偏离

- **控制点**：`app/erp/fin/service/processor/ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor.java`（REVENUE_COST 循环 L73-86 注释「简化：复用 MATCHED 记录金额作为收入/成本抵消额」；INVENTORY_PROFIT 循环同额复制，无未实现利润计算；`ErpFinConsolidationEliminationPostEliminationProcessor#resolveEliminationSubjectCode` 硬编码科目 2202/1131/5001/1401，注释「真实经 GlMappingResolver 解析归 successor」）
- **问题**：内部销售抵销的会计对象是当期**收入/成本流量**（owner doc `intercompany-consolidation.md §内部交易类型`），实现用 AR/AP 配对**余额**顶替——期末未清余额 ≠ 当期交易额（部分收款/跨期交易时二者显著不同）；INVENTORY_PROFIT 类型无「存货中未实现利润」计算，抵销额语义双失真。config 默认 false。偏离仅代码注释自证，owner doc 无对应 simplification 登记（对照 period-close.md 的范式）。
- **建议修复方向**：短期在 owner doc `intercompany-consolidation.md` 登记「当期以 AR/AP 配对余额近似收入/成本抵销额」为 documented simplification + successor 触发条件；长期按 VoucherLine 聚合当期内部交易收入/成本。
- **arm-index 裁决**：新增。

### P3-CK-fin4-017（D2/D5）postElimination 候选不存在误抛 ERR_ELIMINATION_ALREADY_POSTED + DRAFT 抵销凭证会触发期末前置检查阻断结账（合并流程与结账互锁无提示）

- **控制点**：`app/erp/fin/service/processor/ErpFinConsolidationEliminationPostEliminationProcessor.java#postElimination`（L37-42：`candidate == null` 与「非 CANDIDATE 状态」共用 `ERR_ELIMINATION_ALREADY_POSTED`——不存在应报 NOT_FOUND 语义，误导排障）+ `#writeDraftEliminationVoucher`（DRAFT 凭证写入结账期间）× `ErpFinAccountingPeriodProcessor#findUnpostedVoucherCodes`（L436-444：`docStatus != POSTED` 即列 unposted → `hasIssues()` 默认阻断 closePeriod）
- **问题**：合并抵销工作流的正常中间态（DRAFT 草稿待合并复核）会阻断财务期间结账——用户在 preCheck 看到「未过账凭证」清单里的 ELI-* 草稿，无任何提示说明这是抵销草稿及其处置路径；错误码复用放大排查成本。
- **建议修复方向**：candidate==null 换专用错误码；findUnpostedVoucherCodes 排除 `ELIMINATION_VOUCHER_BILL_CODE_PREFIX` 草稿（或在 preCheck 报告中标注来源与处置建议）。
- **arm-index 裁决**：新增。

### P3-CK-fin4-018（D6）IntercompanyVoucherGenerator 红冲行 dcDirection 保留原方向但借贷互换——与承付红冲（P3-CK-fin-018）同族凭证范式分裂

- **控制点**：`app/erp/fin/service/intercompany/IntercompanyVoucherGenerator.java#writeIntercompanyReversalFromLines`（L229-231 `line.setDcDirection(ol.getDcDirection()); line.setDebitAmount(origCredit); line.setCreditAmount(origDebit);`——方向标志与金额侧互换不一致；L234-235 amountSource=origDebit+origCredit 正数）
- **问题**：与 C3.1 `P3-CK-fin-018`（`CommitmentVoucherGenerator#writeReversalFromLines` 同型）及引擎红冲范式（同方向负金额）三方并存。GL 净额正确（按 debit/credit 汇总），但按 `dcDirection` 过滤的消费方（凭证行展示/报表分类）把红冲行误判方向。
- **建议修复方向**：与 P3-CK-fin-018 合并为单一修复项——生成路径红冲范式统一（dcDirection 交换或对齐引擎负数语义）。
- **arm-index 裁决**：新增（复用注记：P3-CK-fin-018 家族新站点，修复应一并；arm-index grep 零命中）。

### P3-CK-fin4-019（D10）对账单 endingBalance 无 balanceAfter 输入时回退账户 currentBalance——银行侧期末余额被账面余额顶替，调节恒等式退化为 book vs book

- **控制点**：`app/erp/fin/service/bankrecon/BankStatementImporter.java#importStatement`（L67-68 `head.setBeginningBalance/EndingBalance(account.getCurrentBalance()...)` 初始为账面余额；L78 `BigDecimal lastBalance = head.getBeginningBalance();` L102-104 仅行提供 balanceAfter 时推进 → L108 `head.setEndingBalance(lastBalance);`）
- **问题**：`BankStatementLineInput.balanceAfter` 全部缺省时，对账单「期末余额」= 资金账户账面 currentBalance（GL 口径）而非银行侧真值——`BankReconciliationBuilder#generate` 的恒等式 `statementBalance − bookBalance = 未达差` 两侧同源，有未达项时恒 fail（响亮错误但语义错误：报「调节不平」实为「期末余额缺失」），无未达项时恒 pass（掩盖真实银行差额）。
- **建议修复方向**：endingBalance 改为导入入参必填（或行缺 balanceAfter 时置 null 并在 generate 前置校验拒绝），禁止用账面余额伪造银行侧数据。
- **arm-index 裁决**：新增。

### P3-CK-fin4-020（D1/D5）BankReconAdjustmentVoucherBuilder 配置键与 billData 键字符串字面量绕过常量约定

- **控制点**：`app/erp/fin/service/bankrecon/BankReconAdjustmentVoucherBuilder.java#resolveAdjSubjectCode`（L133 `AppConfig.var("erp-fin.bank-recon-adj-subject-code", "2240OTHER")`——`ErpFinConstants` 无此常量，grep 零命中）+ `#post`（L90-92 `billData.put("ADJ_SUBJECT_CODE", ...)`/`"TOTAL_BANK_CREDIT"`/`"TOTAL_BANK_DEBIT"` 字面量，同方法 L89 已用 `ErpFinConstants.BILL_DATA_BANK_SUBJECT_CODE` 常量——两种范式并存）+ 消费端 `BankReconAdjAcctDocProvider#createFacts`（L54-56 同字面量）
- **问题**：配置键/协议键无单一真相源——键名改动时写读两侧无编译期保护（当前值一致故行为正确）；与全仓「配置键入 `ErpFinConstants`」约定漂移。
- **建议修复方向**：3 个配置/协议键提升为 `ErpFinConstants` 常量（纯重构，零行为变化）。
- **arm-index 裁决**：新增。

### P3-CK-fin4-021（D7）closePeriod 与在途过账竞态——凭证过账事务先读 OPEN 后提交，可落进已 CLOSED 期间（无提交时复查）

- **控制点**：`ErpFinAccountingPeriodClosePeriodProcessor#doClosePeriod`（L91-92 状态翻转在结账事务末尾）× `ErpFinPostingProcessor#resolveOpenPeriod`（C3.1 报告 L511-532：过账路径按 voucherDate 查期间状态，读后无提交前复查；凭证事务不触碰期间行 → 无乐观锁冲突信号）
- **问题**：时序 = 过账事务 T1 读期间 OPEN → closePeriod 事务 T2 提交 CLOSED → T1 提交（凭证落 CLOSED 期间）。preCheck 的 unposted 扫描发生在 T2 开始前，抓不到 T1（T1 的凭证此刻尚未提交/可能已是 POSTED 状态在途）。窗口窄（秒级并发），但后果是已锁期间被追加凭证，且 `posted=true` 使后续无任何检测面。
- **建议修复方向**：过账凭证持久化前对期间行做版本锁定读取（`getEntityById` 后触及 version 或 select-for-update 语义），或结账事务对期间行以乐观锁强制串行化（voucher 路径写前 reload period 校验状态）。触发条件为真实并发，修复优先级低但应登记。
- **arm-index 裁决**：新增（`P0-MA2-018` 是 billR 并发双 INSERT，不同竞态面）。

## 验证为正确（显式排除，防误报）

- **OA-02 修复在位（背景核对①）**：`CloseVoucherWriter#writeVoucher` L140-142 写侧统一 `flushSession(voucherDao)`（`IOrmTemplate.flushSession`），注释锚定 period-close.md 裁决与 bugs/2026-08-25——closePeriod 同事务的损益结转聚合/试算平衡/年度结转对期末凭证可见性契约落地。
- **lesson 09 B1 preCheck 悬挂覆盖在位（背景核对②）**：`ErpFinAccountingPeriodProcessor#findUnresolvedFinanceExceptions` L491-496 扫 `in(status, [PENDING, RETRYING, MANUAL]) + isNull(voucherId)`（MANUAL 终态未补录亦阻断），并叠加 assets 折旧 posted=false（L510-531，仅 EXECUTED 防 REVERSED 误判）与 inventory 到岸成本 posted=false+APPROVED（L534-553）两个 G4 兜底面；单域实体缺失 try/catch 安全跳过。**P1-CK-fin-005 场景（acctSchema null 静默零凭证零记录）三个扫描面全落空——preCheck 无独立拦截机会，不构成缓解**（已在报告头部声明，不重复登记）。
- **BillR.businessType 语义一致性**（疑似点证伪）：`CloseVoucherWriter` 的 `businessTypeCode` 参数名有误导性，但全部 3 个调用方（ProfitLoss L152-153 / FX L151-152、L212-213 / Annual L126-128）实传 `.name()`——与引擎 `persistVoucher` L899-901（billType/businessType 均 name）及 `reverseCloseVoucher`/`findBillLinks` 的 `eq("businessType", name)` 查询两侧一致，反结账红冲反查链路成立。
- **closePeriod 单事务原子性（D2）**：整个 doClosePeriod 在 Facade `@BizMutation` 事务内——模块状态推进/期末凭证/CLOSING→CLOSED 翻转同事务，任一步失败整体回滚，无「部分模块已结」悬挂态；CLOSING 为事务内瞬态不持久化（状态机类 javadoc L36-41 与实现一致）。
- **preCheck 阻断分级**：`PeriodPreCheckReport#hasIssues`（unposted+unresolved）/`hasReminders`（未核销 AR/AP+allowance excess）/`hasAllowanceShortfall`（独立硬阻断）三级与 owner doc §配置项/§已知简化的阶梯（auto-post-on-close 门控、坏账 shortfall 硬阻断、未核销提示）逐条对应；字段默认 ZERO 无 NPE。
- **模块关账顺序守卫**：`advanceModule` 前驱未 CLOSED 抛 `ERR_MODULE_OUT_OF_ORDER`（AR→AP→INV→AST→GL 链）；FX 重估先于损益结转（closeGlModule L167-170）符合 ar-ap-reconciliation.md 重估排序。
- **损益结转方向与排除集（P0-MA2-016 修复在位）**：收入类贷净额→借收入/贷本年利润、费用+成本借净额→借本年利润/贷费用（L115-148）；仅排除 PERIOD_CLOSE 自身分录（L88-93），EXCHANGE_GAIN_LOSS 正常结转（FX 腿语义，TestErpFinAnnualClose:135-137 断言汇兑损益科目结账后净额归零）；无发生额干净期间跳过 CYP 配置解析（L134-139 D10 边界正确）。
- **FX AR/AP 方向映射**：diff = openFunctional − openSource×rate；应收 diff<0（升值）=收益→借往来/贷汇兑损益，应付 diff>0=收益→贷往来/借汇兑损益（L136-145）——资产/负债方向均正确（银行分支方向 L200-204 亦正确；基准口径缺陷另登记 001）。汇率延迟解析（干净期间不因汇率未配阻断，L80-89 注释与实现一致）。
- **年度结转方向**：cypNet>0（净利）借本年利润/贷未分配利润、<0（净亏）反向（L112-123）正确；`subjectNetForYear` 聚合排除预算/承付影子凭证（findYearPostedVoucherIds L340-342，budget.md 规则 4/6/8 三处一致落实——PL/TB/FX/Annual 四个聚合点全部排除）。
- **CloseVoucherWriter**：借贷平衡守卫（L83-87 unbalanced 抛错）+ 空行/零额短路（L68-70/L80-82）+ POSTED 直写 + postedAt=CoreMetrics + amountSource/amountFunctional 双写 + rate null 回退 1（本位币结账凭证语义）；`(code,orgId)` 前缀码 + UUID 12 位防碰撞。
- **反结账闭环（P1-RC-006/RC-R1.44 修复在位）**：reason 必填守卫先于状态守卫（fail-fast）；kill-switch 默认拒绝；年末次年期间已创建阻断；PL/FX/ANNUAL 三类凭证红冲 + 条件折旧冲销（G3 分级：NopException 阻断、impl 未就绪跳过）；审计三列（reverseCloseReason/reversedBy/reverseCloseAt）状态翻转后副作用前写入。
- **次年期间生成幂等策略**：默认抛 `ERR_PERIODS_ALREADY_EXIST`、skip-existing=true 仅补缺失月份、1 月 OPEN 其余 NEVER_OPENED（orgId 维度缺陷另登记 010）。
- **银行对账核心链**：调节恒等式 `statementBalance − bookBalance = bankCreditUnrecorded − bankDebitUnrecorded` 与 owner doc §schema 补注承载一致（diff≠0 抛 ERR_BANK_RECON_NOT_BALANCED）；generate 期间门控（glStatus=CLOSED 拒绝）；post/reverse 状态机守卫（DRAFT→POSTED→CANCELLED）；post/reverse 的凭证经引擎（voucherBiz.post/reverse）——期间锁/平衡守卫由引擎继承；auto-reverse helper 单条 REQUIRES_NEW 失败隔离 + WARN + 候选保持 POSTED 下月重试，job（enabled 默认 false + cron 每月 1 日 01:30）+ batch.xml + 业务 config 三层门控接线完整（D4 在位）。
- **自动勾对方向语义**：银行 DEBIT↔账面 CREDIT 反向映射（`oppositeDirection` + findCandidates 的 dcDirection/debitAmount/creditAmount 配对一致，owner doc §业务规则 3）；唯一命中 MATCHED/多候选 SUSPENSE/零候选 UNMATCHED；occupied 排除防重复占用 + in 分批 500；counterparty 非空精确匹配、任一侧空放行（RC-R1.43 语义逐条落地）。manualMatch 状态守卫（仅 UNMATCHED/SUSPENSE 可人工）+ 凭证行存在校验。行更新依赖 @BizMutation 会话脏检查 flush（BizModel 注解在位）。
- **报销/借款链**：三轴状态机（docStatus/approveStatus/posted）+ SoD 守卫（approve 断言审批人≠创建人）+ approve→tryPost→markPosted→offsetOrchestrator.offset 顺序正确；reverseApprove/cancel 先 reverseOffset 再红冲凭证再清 posted（强一致次序）；金额一致性校验（头 amountWithTax == Σ行）；cashRepay 三守卫（posted+APPROVED / amount>0 / ≤outstanding）+ billHeadCode 含 CoreMetrics millis 防碰撞 + reverseCashRepay「先红冲后回退字段」次序（与 cashRepay 的「字段先于凭证」残留风险范式相反，注释明示补救路径语义）；cashRepay 失败经引擎 catch 落异常工作台 → 期末 preCheck 可拦截（残留风险有兜底面，documented）。
- **D1 机械扫描全零（1 例外）**：切片 41 文件 `@Inject private`=0、`System.currentTimeMillis`/`LocalDateTime.now`=0、`extends RuntimeException/Exception`=0、字符串 `==` 比较=0、手编生成产物=0；`LocalDate.now()` 1 处（P3-CK-fin4-013）。7 个切片实体 `versionProp="version"` 全在位（orm 逐实体核对）。
- **intercompany runMatching 幂等**：`findExistingPairKeys` 前置去重（P1-MA2-098 修复范式在新代码正确应用）+ 双侧审计列（arSideVoucherId/apSideVoucherId）。
- **G3 错误传播分级（P1-MA4-004 修复在位复核）**：runDepreciation/recloseInvCosts/reverseDepreciation 三处均「bizObjectManager 解析失败容错跳过 WARN」+「NopException rethrow 阻断」双分支——吞咽缺陷已修复，不重复登记。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `银行存款重估基准`/`closeForSchema 账套`/`populateNextYearOpening 互删`/`unitPrice 数量`/`WRITTEN_OFF 重估`/`抵销候选 幂等`/`对账单去重 最近`/`次年期间 orgId`/`autoMatch N+1`/`expense-approval-required 类型必填`/`LocalDate.now`/`endingBalance 回退` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记，报告中注记）**：
  - 银行重估全表扫描 `erp_fin_voucher_line` → **P2-MA4-003(a)**（watch-only）——验证仍在位（`aggregateBankSubjectBookFunctional` L238 `findAllByQuery(new QueryBean())`），性能维度归其修复范围；本报告 001 只登记其**语义**维度（比较基准口径）。
  - AR/AP FX 无前期 reversal / 不更新 openAmountFunctional → **P1-MA2-022**（resolved，documented simplification）——AR/AP 分支口径维持，不登记；银行分支基准缺陷（001）与 WRITTEN_OFF 过滤（005）为不同控制点。
  - 年初余额非累计（本年发生额净额）→ **P1-MA2-018**（resolved，documented simplification）——populate 语义复用不登记；002 中的多账套互删为不同控制点。
  - 多账套读路径双计 → **P1-MA2-095**（resolved R1.29）——002 为结账**写路径**同族新控制点（带注记）。
  - 辅助账生成缺账套幂等 → **P1-CK-fin-011**（C3.1）——002 与其平行（同批修复建议）。
  - 红冲行 dcDirection 保留借贷互换 → **P3-CK-fin-018**（C3.1，承付生成器）——018 为同族新站点（IntercompanyVoucherGenerator），修复应合并为单一范式统一项。
  - 吞异常悬挂家族（P1-MA4-004 resolved / P1-MA2-060）——008（intercompany dispatch 链）为同型新控制点；G3 分级修复在位已复核（验证节）。
  - 核销非幂等 → **P1-MA2-098**（resolved）——006 为 elimination 候选生成的同型新控制点（带注记）。
  - dict 死状态家族（lesson 10）——015 为新站点（BankStatement POSTED/CANCELLED）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 3 | P1-CK-fin4-001..003 |
| P2 | 8 | P2-CK-fin4-004..011 |
| P3 | 10 | P3-CK-fin4-012..021 |

按维度（主维度计）：D6×7（001/003/005/016/018 计 D6，002/013 计 D6/D8 归 002=D6、013 计 D1）——修正明细：D6×5（001/002/003/005/018）、D8×4（004/007/010/012）、D2×3（006/008/017）、D5×2（009/014）、D9×1（011）、D1×2（013/020）、D3×1（015）、D10×1（019）、D7×1（021）、D6/D5 复合（003 计 D6）、016 计 D6。（015 跨 D3；017 跨 D2/D5 计 D2。）

背景核对：OA-02 flush 修复在位 ✓；preCheck 悬挂覆盖在位 ✓；P1-CK-fin-005 无 preCheck 独立拦截机会（不缓解、不重复登记）；P1-CK-fin2-002 属核销链未触碰，本切片 FX 凭证控制点已独立检查（001/005）。

## 剩余风险（查了什么/没查什么）

- **已查**：期间结账全链 8 类逐行深读（状态机/前置检查/结账/反结账/终关/开期/次年期间/编排）；损益结转+年度结转+FX 重估 3 子服务全读（含多账套循环逐迭代推演、银行 FX 与 TestErpFinAnnualClose/TestErpFinExchangeRevaluation 测试交叉验证）；报销/借款 BizModel+Processor+状态机（approve/cancel/reverseApprove/cashRepay/reverseCashRepay 全路径 + AdvanceOffsetOrchestrator 调用时序）；银行对账 7 类全读 + 3 Processor + BizModel + batch.xml/job.yaml D4 接线；intercompany 2 主类全读 + match/elimination 4 类 + inventory 调用点；orm 7 实体 versionProp 核对；D1 机械扫描（41 文件）；arm-index 全量关键词裁决。
- **未深查**：`erp-fin-web` 期间结账向导/银行对账 AMIS view 与后端契约 drift（归 C8.2）；`ErpFinBusinessMetrics` 指标注册细节（仅核 closePeriod 调用点 null registry 回退语义）；`BankStatementLineInput` DTO 校验完整性（导入入口在 GraphQL 层的入参边界）；treasury 域 `ErpFinFundAccountBizModel`（账户 CRUD 与 currentBalance 维护面——本切片仅消费该字段，P1-001 的修复可能需 treasury 配合）；测试代码（103 个 finance 测试仅用于交叉验证语义，未逐个审计断言强度）；`IErpFinTransferPriceResolver` 的 pickBest 规则选择算法（只核 unitPrice 语义与 materialId 传导）；期初余额导入**无实现代码**（owner doc `opening-balance.md` 的导入流程/状态机未落地，`ErpFinGlBalanceBizModel` 为裸 CRUD + 年度结转 populate 承载年初余额——属需求覆盖面非代码缺陷，requirement-compliance mission 已闭合，此处仅登记覆盖事实不立 finding）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-fin4-001**（银行 FX 基准口径）——若部署约定外币资金账户 `currentBalance` 每月重置或仅存当期发生额（生产无 writer、orm 注释「当前余额」，本代理按累计语义定性），影响面收窄为「多账户共享科目错配」；建议修复阶段先写跨期两月连续结账测试实证再定级。
  2. **P1-CK-fin4-002**（多账套结账聚合）——触发前提是 `multi-schema-enabled=true` 且主账套 isPropagate；若多账套部署在本项目定位为未启用能力，可与 C3.1 `P1-CK-fin-004/011` 合并降级为一个多账套专项修复簇。
  3. **P3-CK-fin4-021**（结账/过账竞态）——纯并发推演（无测试可依），窗口与实际隔离级别相关（默认 read-committed 下成立）；若平台 Session 对期间行有本代理未见的锁行为则不成立，建议修复阶段以并发测试证伪/证实。
