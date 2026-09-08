# ck-finance-period-misc-r3 — finance「期间结账与银行对账 + 跨域凭证链路」fin-4 五维符合性审计报告（ai-check-r3 M1.4）

> 工作项：M1.4（U05 × 五维 × fin-4，冻结清单 §4 映射表第 4 行 / §3.2 切片登记；期间/结账/银行对账/合并抵销/跨法人切片）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `65750e0701faad23f27bc0ac364222df89a62efe`；脏面 = 仅 1 个未跟踪 plan 文件（本计划 `docs/plans/2026-09-08-2238-3-*.md` 自身），零已跟踪文件改动，零生产路径脏面。计划 Current Baseline 所记 `dc31a2555` 为起草时点，姊妹切片（M1.2 `b21e0f74d` / M1.3 `65750e070`）正常落库后 HEAD 前进，按计划口径「审计证据以实跑时 HEAD + 脏面披露为准」（MI.9 收官审计先例）。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U05 行 fin-4 列 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：期间/结账/银行对账/合并抵销/跨法人（`module-finance/erp-fin-{dao,service,web}` src/main 中 fin-4 面——`service/close`（CloseVoucherWriter）/`service/annualclose`（AnnualCloseService）/`service/profitloss`（ProfitLossClosingService）/`service/bankrecon`（7 类）/`service/intercompany`（2 类）/`service/fx`（ExchangeRevaluationService）6 包 + `ErpFinAccountingPeriod(+Status)`/`ErpFinTrialBalance`/`ErpFinBankReconciliation(+Line)`/`ErpFinBankStatement(+Line)`/`ErpFinConsolidationElimination`/`ErpFinIntercompanyMatch`/`ErpFinIntercompanyTransferPrice` 10 BizModel 族 + `IErpFinIntercompanyTransferBiz` SPI 实现 + 16 per-mutation Processor（AccountingPeriod 7 / BankReconciliation 3 / BankStatement 3 / ConsolidationElimination 2 / IntercompanyMatch 1）+ `ErpFinAccountingPeriodStateMachine`）；owner docs `docs/design/finance/period-close.md` + `docs/design/finance/bank-reconciliation.md`（抵销/跨法人判定面辅锚 `docs/design/finance/intercompany-consolidation.md`）。跨域凭证链路边界：inventory 侧调拨确认 processor 消费点归 inv 切片（M1.13 已闭合），本切片审 finance 侧本体（intercompany 转移/定价/匹配 BizModel 与凭证生成通道）。
> Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物。
> 共享代码边界（冻结清单 §3.2）：posting processor 族本体归 fin-1（已闭合）——本切片只审结账/结转/重估/抵销/跨法人对凭证引擎的**消费侧调用点**及引擎外直写通道（CloseVoucherWriter / IntercompanyVoucherGenerator / 抵销 DRAFT 凭证写入 = 生成器本体消费面，属本格）；common 抽象族行为缺陷归 U20（本切片只审调用点）；聚合横切面归 U21；notify 派发子系统本体归 U11。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（fin-4 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（三焦点：结转账套过滤/FX 重估累计口径/期间竞态；重点①②③⑧⑨）+ 维度⑮断言抽样 2 doc × 4 断言 | 反模式族 fin-4 scope 65 文件全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0 / `@Transactional`=0——R6=2 基线条目均在 fin-1 VoucherBizModel）；checker 19 规则逐项 = M0.3 快照行零漂移；非可控时钟=0——**P3-CK-fin4-013 open 站点已修**（`e37ddfb15` P2-15 LocalDate.now→CoreMetrics，L117 现场在位，索引 open=回填缺口 → 复用）；`IDaoProvider|IOrmTemplate` 命中 15 文件 63 站点——豁免注释在位 3 文件，**13 文件无注释理由**（跨域 11 站点）→ 新立 P3-CK-fin4-022-r3；codegen 命中全为 erp-fin-meta dict.yaml 只读校验点 + `_gen` 零脏面；E1 勘误路径实存 + `erp-fin.action-auth.xml` 注册在位；15/15 无跳维：⑧期间 5 态 dict 实仓 dump 与 StateMachine Bean 4 命名动作逐边一致、BankStatement docStatus 永驻 DRAFT 站点在位（归并 015）vs BankReconciliation DRAFT→POSTED→CANCELLED 三态 writer+守卫在位；⑨前置检查链与 owner doc 逐条对齐 + autoMatch per-line N+1 在位（归并 011）+ **FNPT 注册面缺口**（13 mutation 零注册）→ 新立 P3-CK-fin4-023-r3；三焦点：FX 累计口径（`aggregateBankSubjectBookFunctional` L225-258 全期间聚合+自身分录/影子凭证排除 = P1-CK-fin4-001 复用在位）、账套过滤（AnnualClose L346-348/L306-309/L158-163 + ProfitLoss L81/L194-197 = P1-CK-fin4-002 复用在位）、期间竞态（closePeriod L51 入口守卫 + L87-95 无提交时复查 → 归并 P3-CK-fin4-021）；⑥面新立 P3-CK-fin4-024-r3（intercompany/抵销通道 currencyId/acctSchemaId 硬编码 "1"，currency 面无 successor 登记）；⑮2 doc × 4 断言 = 8 核查点全一致零漂移（期间 5 态+迁移/反结账审计轨迹+向导 5 mutation/flush 契约+FX 腿；调节恒等式实现承载/无 adjustVoucherId 列/方向语义/导入幂等键） | **finding**（3 新立 P3 + 17 归并 + 4 复用 + 1 归属注记 + 1 跨切片追加，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + fin-4 面 = 期间管理(+Status)/期末结账向导/试算平衡/银行对账(+Line)/银行对账单(+Line)/合并抵销/跨法人匹配/转移定价 11 页族 | `npm run validate:flux` step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；整体 exit 1 余项 = 325 条全部 `variant=primary` dropdown-button 既有 stub 外部漂移族（非 variant ERR = 0，successor 在案，非本切片 finding）；flux-only：`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0；fin-4 面 11 页族逐页走查——10 实体页族保留层 view.xml 全 `x:extends="_gen/_X.view.xml"` + main/picker page.yaml 纯 codegen 面（M0.4 矩阵边界一致），手写页 period-close-wizard（M0.4 L46-47 行 MI.8 i18nEn 已补：flux.yaml 21 处 + page.yaml 47 处）步骤映射与 owner doc §期末结账向导 5 mutation 链一致、零后端 delta；数据访问全 REST `@query:`/`@mutation:` 零 GraphQL（preCheck/closePeriod/finalizePeriod/reverseClose + findPage/get），codegen 面 i18n-en 承载在 ORM 模型源（orm.xml `i18n-en:displayName` 1051 处）；E2E：fin-4 涉 4 action spec + 1 visual spec——`data-slot\|data-testid\|.cxd-` 全零 + `E2E_ENGINE` 缺省 flux 实证（engine.ts L7-12 `return 'flux'`）+ 3 spec 头注释明示 GraphQL `/graphql` 为非页面路径 setup/action/cleanup API 断言通道（runbook 豁免，fin-1/fin-2/fin-3 同判例）+ PageObject 经 `_helper`+`pages` adapter 层 | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + 期间/余额 seed 一致性抽查 | `TestErpSeedDataIntegrity` **4/4 全绿**（BUILD SUCCESS，363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning）；`git status --porcelain _init-data/` 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL**（= 冻结口径）；deploy `_seed_*.sql` 命中 cs/notify 两族（各三方言）均在 seed-data.md 登记处表显式聚合裁决（fin-3 r3 同跑同果），fin 无 deploy seed 无第三态；fin-4 seed 抽查：accounting_period 单行 2026-07 org=2 STATUS=OPEN（org 维度在位）、gl_balance 6 行 FK 齐且行内算术自洽（1130.00−960.50=169.50）、fund_account 4 行 BANK subjectId FK 非空、bank_statement/reconciliation/elimination/intercompany 族种子在册（FK 由门禁背书） | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + 关键业务流 5 行 + F2.4 修复回归复核 | `mvn test -pl module-finance/erp-fin-service` **533/0/0/0 全绿**（= known-good fin 计数零回归）；覆盖对账：fin-4 BizModel 15 个自定义公开方法（AccountingPeriod 6 / BankStatement importStatement / Line autoMatch+manualMatch / Recon generate+post+reverse / Elimination 2 / Match runMatching+checkDualSideConsistency）逐项有测试；关键业务流清单 5 行全覆盖——期末结账+损益结转（PeriodCloseEndToEnd/ProfitLossClosing/PeriodPreCheck/ModuleCloseOrder/ClosingMultiSchema）、年度结转+年初余额（AnnualClose/AuxiliaryReconGate）、FX 重估累计口径（ExchangeRevaluation + testBankFxRevaluationCrossPeriodCumulative）、银行对账自动匹配/自动红冲（BankStatementImport/Match/CounterpartyMatch + ReconEndToEnd + ReconAutoReverseJob）、合并抵销生成与过账（IntercompanyMatchingAndElimination + PropertyErpFinConsolidationElimination + DualSideConsistency）；F2.4 三修复体回归测试全部在位（001 跨期累计 / 002 ClosingMultiSchema 账套过滤 / 003 testOnTransferConfirmedQuantityAmount）——无 fin3-019 型缺口；快照纪律 `SnapshotTest.RECORDING` = 0 残留、delVersion 命中为审计行为断言合规 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT；170 baseline files，totals CAT1..4=0/0/209/1318，单向收紧成立）；`--self-test` **PASS**；fin 探针族（CAT-1 54 / CAT-2 20）维持清零零回归；WHITELIST「批 1/2 finance」5 条抽 3（IErpFinApDocumentBiz E3 豁免 / ApDocRuleClassifier C2 功能型字面量 / ApDocumentPipelineProcessor C2 OCR Pattern）：3/3 四要素齐备（路径/理由/owner doc=i18n-compliance.md 准绳表 #5 + CAT-3 行/裁决来源=plan 2026-09-07-0902-1 Phase 1 Decision）且 HEAD 实仓复核登记准确（3 字面量/4 Pattern/1 @Description 逐项在位）；`grep -L @Locale` *Errors.java = 空；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ai-check-index.md` §报告清单/§Finding 追踪（同域报告 `ck-finance-period-misc.md` C3.4 fin4 族 21 条逐一比对：P1-001..003 fixed / P2-004..011 open / P3-012..021 open）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/`（无 fin4 同型独立登记）+ §Mission 基线快照（R2b/R2c/R10/R12 命中均为已裁决偏离，不重复报告）+ fin-1 r3 报告 §2 归属标注（posting 引擎内部归 fin-1；本格无涉引擎内部新站）。**本轮新立 3 条**（`P3-CK-fin4-022/023/024-r3`，全 P3）；历史 21 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 4 条

| 原 ID | 修复在位证据（T0 = HEAD `65750e07`） |
| --- | --- |
| P1-CK-fin4-001（银行 FX 重估账面基准只聚合本期） | F2.4 在位：`ExchangeRevaluationService.aggregateBankSubjectBookFunctional`（L225-258）累计口径（无 periodId 过滤注释自述 L229）+ EXCHANGE_GAIN_LOSS/PERIOD_CLOSE/PROFIT_TO_RETAINED_EARNINGS 自身分录排除（L248-253）+ BUDGET/COMMITMENT 影子凭证排除（L233-235）；javadoc L218-224 登记 P1-CK-fin4-001；修复回归 testBankFxRevaluationCrossPeriodCumulative 在位 |
| P1-CK-fin4-002（损益结转/年度结转聚合无账套过滤） | F2.4 在位：`ProfitLossClosingService.findPostedVoucherIds` L194-197 acctSchemaId 过滤 + 行级 L81 双保险（注释登记 P1-CK-fin4-002）；`AnnualCloseService.findYearPostedVoucherIds` L346-348 + `aggregateYearSubjectActivity` 行级 L306-309 + `populateNextYearOpening` clear L158-163 账套维度（注释 L156-157「否则多账套循环互删只余最后账套」）；修复回归 TestErpFinClosingMultiSchema 在位 |
| P1-CK-fin4-003（跨法人凭证金额=单价且 materialId=null） | F2.4 在位：`ErpFinIntercompanyTransferBizModel.onTransferConfirmed(..., qtyByMaterial, ...)` 重载（L67-133）——`amount = unitPrice × Σ数量`（L128-129）+ 首正数量物料参与定价解析（L100-115）；多物料「首物料单价 × Σ数量」近似语义 javadoc 显式登记 successor（L95-98，dual-agent review 2026-08-31）；修复回归 testOnTransferConfirmedQuantityAmount（100×150=15000）在位 |
| P3-CK-fin4-013（生产代码 LocalDate.now 非可控时钟） | 修复在 HEAD 有效：commit `e37ddfb15`（2026-08-28，plan-2026-08-28-0219-1 P2-15「LocalDate.now → CoreMetrics」）将 `ErpFinBankReconAutoReverseHelper.findCandidates` 候选窗口改 `CoreMetrics.currentDate().withDayOfMonth(1)`（L117 现场复核在位）；fin-4 scope 非可控时钟 grep = 0。**索引行状态仍 open = 回填缺口**（hr-1 r3 P1-CK-hr-001 同判例，lesson-11 同型：状态回填归索引 owner 流程，本报告按「复用已 fixed」登记） |

### 2.2 归并（同型 open 追加证据至原 ID）— 17 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-fin4-004 | FX 重估与年度结转聚合无 orgId 维度原样：`revalueArAp`（L105-112 仅 status+currencyId 过滤）/`revalueBankDeposits`（L164-169）/`aggregateBankSubjectBookFunctional`（L225-258 全凭证聚合）三处零 orgId/acctSchemaId 输入过滤；`AnnualCloseService.findYearPostedVoucherIds` 期间查询 L330-331 仅 `eq("year", year)` 跨组织聚合同年度期间现症在位 |
| P2-CK-fin4-005 | FX AR/AP 重估未排除 WRITTEN_OFF 原样：`revalueArAp` L107-108 `notIn("status", [SETTLED, CANCELLED])`——WRITTEN_OFF 不在排除集，已核销残留 openAmount 项仍被重估现症在位 |
| P2-CK-fin4-006 | 抵销候选识别非幂等原样：`generateEliminationCandidates` L45-106 三类循环均无 exists 前检（matchId/pairKey 无唯一性守卫），重复调用按 MATCHED 记录重复生成候选行现症在位 |
| P2-CK-fin4-007 | intercompany/抵销凭证直写引擎外路径原样：`voucherDate=CoreMetrics.today()`（IntercompanyVoucherGenerator L298 / 红冲 L206 / PostEliminationProcessor L82）而非业务日期；writeIntercompanyVoucher 全程无期间 glStatus 锁定守卫；科目解析失败静默降级（`resolveSubjectCode` L276-280 catch RuntimeException→LOG.debug→defaultCode + `applySubject` L357-366 subject=null→subjectId 不设）现症在位 |
| P2-CK-fin4-008 | inventory 侧 catch(RuntimeException) 吞咽——站点实仓复核在位（`ErpInvTransferOrderConfirmProcessor` L65-67 warn only）；**归属注记**：按冻结清单 §3.2 + 计划边界，调拨确认 processor 消费点行为归 inv 切片（M1.13 已闭合，`ck-inventory-r3.md` 在案），本格不重复立项、原 ID 状态不动，修复随 inv 侧 M2.x 批次收口；finance 侧本体（转移/定价/匹配/生成通道）已按 007/018 归并证据覆盖 |
| P2-CK-fin4-009 | 对账单导入幂等去重只对比最近一张原样：`findStatementIdByAccount` L201-210 statementDate DESC + limit 1，`existsByRefNo`/`existsByComposite` 仅查该单行——跨月重导与历史单据去重失效现症在位 |
| P2-CK-fin4-010 | 次年期间生成/存在性检查/次月定位无 orgId 原样：`generateNextYearPeriods` 存在性检查 L40-42 仅 `eq("year", year)` + orgId 兜底 L54（无同年期间→`resolveDefaultOrgId()`="1"，有→他组织值）；`findNextYearJanuaryPeriod` L360-366 收 orgId 参数查询不用；`hasNextYearPeriods` L130-136 仅 eq year（反结账门控可被他组织次年期间误触发）现症在位 |
| P2-CK-fin4-011 | autoMatch 逐行全量重查原样：`autoMatch` L52-60 每未勾对行调 `BankLedgerQuery.findCandidates`——内部 `findVoucherIdsInWindow` 窗口凭证全量加载 + `findOccupiedLineIds` 已勾对行全量加载 per-line 重复现症在位（分批 in 500 优化仅缓解 SQL 长度非 N+1） |
| P3-CK-fin4-012 | 反结账不清理试算平衡快照原样：`reverseClose` L63-88 红冲/回开副作用零 `ErpFinTrialBalance` 处理（对照 `populateTrialBalanceForAllSchemas` L333-340 clear+重建仅在 closeGlModule）——CLOSED_FINAL→OPEN 后残留旧快照无失效标记现症在位 |
| P3-CK-fin4-014 | expenseType 必填校验误挂审批开关原样：`ErpFinExpenseClaimProcessor.isExpenseTypeRequired` L374-377 读 `CONFIG_EXPENSE_APPROVAL_REQUIRED`——关闭审批开关同时静默关闭行类型校验现症在位（物理站点在费用报销面，r1 归属 C3.4 fin4 族，本格随族复核不重分类） |
| P3-CK-fin4-015 | BankStatement docStatus 永驻 DRAFT 原样：全 fin-4 树 `setDocStatus` 仅 `BankStatementImporter` L72 写 DRAFT——无 POSTED/CANCELLED writer（对照 BankReconciliationBuilder L96/129/140 三态全 + illegalTransition 守卫），owner doc §状态机 DRAFT→POSTED→CANCELLED 未接线现症在位 |
| P3-CK-fin4-016 | REVENUE_COST/INVENTORY_PROFIT 抵销额复用 AR/AP 配对余额原样：`generateEliminationCandidates` L72-87/89-106 注释自述「简化：复用 MATCHED 记录金额」——存量当流量，owner doc（intercompany-consolidation.md）未登记该偏离现症在位 |
| P3-CK-fin4-017 | postElimination 候选不存在误抛 ALREADY_POSTED + DRAFT 抵销凭证阻断结账原样：L47-50 `candidate == null` → `ERR_ELIMINATION_ALREADY_POSTED`（误导性错误码）；L89 抵销凭证 docStatus=DRAFT 落库（结账前置检查 unposted 凭证清单命中即阻断，与合并流程互锁无提示）现症在位 |
| P3-CK-fin4-018 | 红冲行 dcDirection 保留原方向但借贷互换原样：`writeIntercompanyReversalFromLines` L229-231 `setDcDirection(ol.getDcDirection())` + `debitAmount=origCredit/creditAmount=origDebit`——与承付红冲（P3-CK-fin-018，fin-1 格）同族凭证范式分裂现症在位 |
| P3-CK-fin4-019 | endingBalance 无 balanceAfter 回退账户 currentBalance 原样：`BankStatementImporter` L68 初值=account.getCurrentBalance()，L108 末值=lastBalance（balanceAfter 链）——无 balanceAfter 输入时 endingBalance=账面余额，调节恒等式退化为 book vs book 现症在位（调节侧 L66-67 statementBalance 即取该值） |
| P3-CK-fin4-020 | 配置键与 billData 键字符串字面量绕过常量约定原样：`BankReconAdjustmentVoucherBuilder` L90-92 `"ADJ_SUBJECT_CODE"/"TOTAL_BANK_CREDIT"/"TOTAL_BANK_DEBIT"` 字面量（对照 L89 `ErpFinConstants.BILL_DATA_BANK_SUBJECT_CODE` 常量在位不对称）+ L133/136 config 键 `"erp-fin.bank-recon-adj-subject-code"` 字面量 ×2 现症在位 |
| P3-CK-fin4-021 | closePeriod 与在途过账竞态原样：`doClosePeriod` L51 `assertCanClose(period.getStatus())` 事务入口读态 + L87-95 CLOSING→CLOSED 状态簿记后 flush——无提交时期间状态复查/无锁，凭证过账事务「先读 OPEN 后提交」可落进已 CLOSED 期间现症在位（同事务内的模块关账链不受影响；窗口仅在跨事务并发过账面） |

### 2.3 新立 `-r3` — 3 条

**P3-CK-fin4-022-r3**（DIM-B 维度② 跨实体访问）

- **控制点**：fin-4 族 13 文件 IDaoProvider/IOrmTemplate 直查命中处无注释理由——`AnnualCloseService`（L64 + L155/243/285/303/329/341/361/383/392/400/412）/ `BankLedgerQuery`（L42 + L61/104/123）/ `BankReconAdjustmentVoucherBuilder`（L51 + L111）/ `BankReconciliationBuilder`（L43 + L84/100/147/157/167/177/184/207/217）/ `BankStatementMatcher`（L39 + L78/88/98）/ `CloseVoucherWriter`（L67/151 + L92-94）/ `ExchangeRevaluationService`（L64 + L105/164/227/243/267/276/289/298/306）/ `ErpFinIntercompanyTransferBizModel`（L52 + L206/229/240/274）/ `IntercompanyVoucherGenerator`（L52 + L134/153/167/174/197-199/254/287-288/372）/ `ErpFinBankStatementLineManualMatchProcessor`（L21）/ `ErpFinConsolidationEliminationGenerateEliminationCandidatesProcessor`（L34 + L48-52）/ `ErpFinConsolidationEliminationPostEliminationProcessor`（L38 + L44/74-76/167）/ `ErpFinIntercompanyMatchRunMatchingProcessor`（L39）/ `ProfitLossClosingService`（L54 + L77/176/185）+ `ErpFinIntercompanyMatchBizModel`（BizModel 内 `daoProvider().daoFor` 自域查询 L62）。其中跨域站点 11 处：`ErpMdSubject` ×7（AnnualClose L383 / ExchangeRevaluation L267+L276 / ProfitLoss L176+L185 / PostElimination L167 / IntercompanyGenerator L372）+ `ErpMdCurrency` ×2（AnnualClose L412 / ExchangeRevaluation L289）+ `ErpMdWarehouse` ×1（TransferBizModel L229）+ `ErpMdOrganization` ×1（TransferBizModel L240）——`IErpMd*Biz` 在位未注入；`ErpFinIntercompanyTransferBizModel` 作为 BizModel 直接注入 `IDaoProvider` 直查 md 族为 ② 最尖锐形态。
- **问题**：冻结清单 §1.1 程式⑤「命中处须有注释理由」机械命中——同切片对照面 3 文件已合规登记（BankStatementImporter L36-37「跨实体访问经 daoProvider…无独立 IBiz」/ ErpFinBankReconAutoReverseHelper L110-111「batch helper 非 BizModel…对齐 ErpFinDeferredPostingRetryHelper 范式」/ ErpFinAccountingPeriodProcessor L65-66 session 边界注记），其余 13 文件零登记，范式不对称。
- **三态裁决**：新立。r1 fin4 族 21 条无此形态；r2 无同型独立登记；同型家族跨单元先例 = P2-CK-fin-007（fin-1）/ P3-CK-ast-028-r3 / P2-CK-qa-027-r3 / P3-CK-fin3-017-r3（最近先例，同域上一切片）——按「跨单元另立先例自立 ID」既定判例新立。级别 **P3**：命中全为只读（getEntityById 主键查/等值查/findFirst），无跨域写、无行为分歧（fin3-017-r3 同形态 P3 先例）。全仓同型扫描归 U20/M1.15。
- **修复建议**：归 M2.x——md 直查站点注入 `IErpMdSubjectBiz`/`IErpMdCurrencyBiz` 等（或按 fin-2 先例补豁免注释）；同域 daoFor 站点补一行豁免理由注释（对齐 ErpFinBankReconAutoReverseHelper L110-111 措辞）；与 fin-007/ast-028-r3/qa-027-r3/fin3-017-r3 同族一并收口。

**P3-CK-fin4-023-r3**（DIM-B 维度⑨/⑭ action-auth 注册面）

- **控制点**：fin-4 自定义 mutation 13 个零 FNPT 注册——保留层 `erp-fin.action-auth.xml` 全量 grep `FNPT:ErpFin*` 仅命中 `ErpFinAccountingPeriod:closePeriod`（L52）与 `ErpFinAccountingPeriod:reverseClose`（L56）2 项；未注册：`preCheck`/`finalizePeriod`/`openPeriod`/`generateNextYearPeriods`（AccountingPeriod）+ `importStatement`（BankStatement）+ `autoMatch`/`manualMatch`（BankStatementLine）+ `generate`/`post`/`reverse`（BankReconciliation）+ `generateEliminationCandidates`/`postElimination`（ConsolidationElimination）+ `runMatching`/`checkDualSideConsistency`（IntercompanyMatch）。
- **问题**：deny-by-default 权限模型下非「财务员」角色对这些动作无权限载体——菜单页面可达但页面内业务动作按钮全部 deny（内部不一致：同页 CRUD 基线 FNPT 允许 update 而业务 mutation 无注册）；`closePeriod`/`reverseClose` 已注册证明注册范式在位，其余 13 个为遗漏而非裁决豁免（action-auth 与 xbiz 均无对应豁免登记）。
- **三态裁决**：新立。r1 fin4 族无此形态；r2 无同型；同族跨单元先例 = P3-CK-drp-018（drp 22 mutation 零 FNPT）/ P3-CK-b2b-011（4 mutation 缺注册）/ P3-CK-prj-022-r3 / P3-CK-qa-029-r3（xbiz auth 面）——r1 同族均 P3 校准。级别 **P3**（主守卫=状态机+F1.3 状态锁基类在位，权限面为纵深防御层；且核心资金动作 closePeriod/reverseClose 已注册）。
- **修复建议**：归 M2.x——保留层 action-auth 按 closePeriod 范式补 13 个 FNPT 注册（roles=财务员，对齐 useCases）；与 drp-018/b2b-011/prj-022-r3/qa-029-r3 同批跨域对齐收口。

**P3-CK-fin4-024-r3**（DIM-B 维度⑥ 多币种/账套族）

- **控制点**：intercompany 配对凭证与合并抵销凭证通道的币种/账套维度硬编码——`ErpFinIntercompanyTransferBizModel` L127/L177 `String currencyId = "1"`（onTransferConfirmed 与 onTradeDocumentApproved 双路径）+ `resolveOrgAcctSchemaId` L263-266 恒返 `"1"`（有「多账套精确解析归 successor」注释，schema 面已登记）；`ErpFinConsolidationEliminationPostEliminationProcessor` L84 `voucher.setAcctSchemaId("1")` + L107/L131 `currencyId("1")` + L111/L135 行级 acctSchemaId "1"（**schema 面亦无 successor 登记**）。
- **问题**：currencyId 恒 "1" 无任何 successor/豁免登记——非本位币法人对间交易凭证币种错配；对照同族 P1-CK-fin3-005 修复体（`resolveCurrencyId` 经账套本位币解析 + 无账套回退占位注释）预算通道已修而本双通道未覆盖，修复范式不对称。
- **三态裁决**：新立。r1 fin4 族无此形态（fin4-007 直写路径清单未含币种维度）；同型家族先例 = P2-CK-fin3-010（预算多币字段失真）/ P2-CK-inv-010 / P3-CK-mnt-016 / P3-CK-mfg-021（PostingEvent 汇率硬编码）。级别 **P3**：双通道均 config-gated 默认关（`erp-fin.intercompany-posting-enabled`=false / `erp-fin.consolidation-elimination-enabled`=false，owner doc §配置项背书），投产前为潜伏面（mnt-016/mfg-021 P3 同校准）。
- **修复建议**：归 M2.x——镜像 fin3-005 修复范式：经法人根账套解析本位币（无账套回退占位注释）；PostElimination acctSchemaId 同步接 GlMappingResolver 或候选行携带维度；config 投产启用时升 P1。

**跨切片追加证据（不重复立项、不计数）**：`P3-CK-fin2-017` 族「账套解析魔法默认 "1"」追加 fin-4 站点 3 处——`ExchangeRevaluationService.resolveAcctSchemaId` L314 / `AnnualCloseService.resolveAcctSchemaId` L408 + `resolveFunctionalCurrencyId` L420（无账套/无本位币配置时回退 "1"，与 BadDebtProvisionService 同型）；原 ID 状态不动，证据随本报告 §2.2 在案。

### 2.4 归属标注（§3.2 共享代码边界）

- 本报告全部控制点属 fin-4 格（期间/结账/银行对账/合并抵销/跨法人切片本体：6 服务包 + 10 BizModel + 16 per-mutation Processor + StateMachine Bean + CloseVoucherWriter/IntercompanyVoucherGenerator 消费面写入通道）。
- posting 引擎内部归 fin-1（已闭合）：本格消费点仅经 `CloseVoucherWriter`（flush 契约 = owner doc 裁决实现载体）与引擎外直写通道写凭证，未发现涉引擎内部新站。
- `P2-CK-fin4-008` inventory 侧消费点（ErpInvTransferOrderConfirmProcessor catch 吞咽）按 §3.2 归属 inv 切片（M1.13 已闭合），本格注记不重复立项（§2.2 归并行内）。
- common 抽象族（`AbstractErpCrudBizModel` 状态锁基类）调用点已审合规（fin-4 BizModel 全接入）；基类行为缺陷归 U20（M1.15）。
- `validate:flux` 全局面与 E2E runbook 全局合规抽样归 U21（M1.16）；本格仅按 §1.2 ③ 核 fin 涉及 spec（结果见矩阵 DIM-F 行）。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 3（fin4-001..003） | 0 |
| P2 | 0 | 0 | 8（fin4-004..011，含 008 归属注记） |
| P3 | 3（fin4-022/023/024-r3） | 1（fin4-013，索引回填缺口注记） | 9（fin4-012/014..021） |
| **合计** | **3** | **4** | **17** |

新立合计 3（全 P3，DIM-B 全部：② daoFor 豁免注释族 / ⑨⑭ FNPT 注册面 / ⑥ 币种账套硬编码族）；复用合计 4（P1×3 + P3×1）；归并合计 17（P2×8 + P3×9）+ 跨切片追加证据 1（P3-CK-fin2-017 族 3 站点，不计数）+ 归属注记 1（P2-CK-fin4-008 → inv 切片，在归并行内）；历史 21 fin4 ID + 跨域家族 ID 零覆写。五格 verdict：DIM-B **finding** / DIM-F **pass** / DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：fin-4 范围 65 文件全读（6 服务包 13 + entity BizModel 10 + processor 16 + statemachine 1 + dao 实体/DTO/接口 25）逐行走查；三焦点（FX 累计口径/账套过滤/期间竞态）+ r1 fin4 族 21 ID 逐一现场复核；机械程式全套实跑（checker 19 规则零漂移 / 反模式族全零 / 时钟族零命中 / IDaoProvider 逐文件注释核对 / codegen 安全 / E1 聚合路径 / validate:flux 双数字 / flux-only 双 grep / TestErpSeedDataIntegrity 4/4 + 期间余额 seed 抽查 / fin 回归 533 全绿 + 覆盖对账 15 方法 × 测试逐项 + 快照纪律 / --strict + --self-test 双 PASS + 白名单 3 抽查 HEAD 复核）；15 维度无跳维；⑮ 2 doc × 4 断言 = 8 核查点。
- **未深查（边界归属）**：inventory 侧调拨确认 processor 行为（归 inv 切片，M1.13 已闭合）；posting 引擎内部（归 fin-1，已闭合）；`erp-fin-web` `_gen` 视图细节（codegen 面，M0.4 矩阵判定）；费用报销面除 fin4-014 站点外本体（归 fin-2 格已闭合）；测试代码仅作覆盖对账与 F2.4 回归复核消费；nop-report 报表子系统 fin 面（报表种子归报表批次，r1 未列 fin4 格）。
- **残留风险（登记不裁决）**：① 17 条归并 open finding 修复归 M2.x（finance 修复批），建议优先 fin4-004（FX/年度聚合无 orgId——多组织部署显性化）与 fin4-010（次年期间 orgId 缺失——第二组织年末结账即抛错）；② 3 条新立 P3 与同族跨域站点（daoFor 豁免注释族全仓扫描归 U20/M1.15；FNPT 注册面与 drp/b2b/prj/qa 同批收口）M2.x 一并处理；③ fin4-024-r3 的双通道在 `intercompany-posting-enabled`/`consolidation-elimination-enabled` 任一投产启用时即升 P1（先写失败测试）；④ `validate:flux` 325 条 variant 漂移与 compliance 机器基线块批前在案差距均为外部事项（successor：`ai-check-r3-compliance-baseline-raise` / nop-chaos-flux dist 基线裁决），非本切片范围。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项；`erp-fin.intercompany-posting-enabled`/`consolidation-elimination-enabled`/`erp-fin.bank-fx-revaluation-enabled`/`erp-fin.annual-close-enabled` 任一 config 投产启用时，fin4-024-r3 及 fin4-004/010 应即升优先级先写失败测试；P3-CK-fin4-013 索引行状态回填归索引 owner 流程（lesson-11 同型）。
