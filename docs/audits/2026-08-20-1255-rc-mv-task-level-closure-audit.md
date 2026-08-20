# 2026-08-20 MV V.3 任务级结束审计 — MR1 全 89 修复行闭包核验报告

> 日期：2026-08-20（初裁）+ 2026-08-20 同日复审（发现项 F1/F2/F3 裁决收口后 re-run，见 §4.1 与 §5 终裁）
> 审计者：独立子代理（cold session，未参与任何 MR1 执行工作）
> 审计对象：MR1 全 89 行修复（RC-R1.1 ~ RC-R1.89，`docs/backlog/requirement-compliance-roadmap.md:392-481`）任务级闭包
> 方法引用：`docs/skills/closure-audit-prompt.md`（计划级方法升维到任务级多计划清单）+ 计划 `docs/plans/2026-08-20-1255-1-rc-mv-full-verification-closure-audit.md` Phase 3 范围定义
> 项目定制注入：`docs/skills/README.md §项目定制化层（nop-app-erp）`（保护区域 / 已知失败模式 8 closure-pending、11 arm-index 不回填、13 批量授权不可类推）
> 约束遵守：只读审计 + 本报告落盘；未运行 mvn / compliance checker（V.1/V.2 已由计划 Phase 1-2 执行）

---

## §1 审计范围与方法

### 1.1 Layer 1 — 文档面全覆盖（89/89 行，五点一致性）

逐行机械核验（rg/python 列解析 + 定向 grep）：

1. roadmap 行 Status = `done`（含 ✅ 证据注记）；
2. 行引用的修复计划存在 AND `Plan Status: completed` AND 计划含独立结束审计证据（非执行者自审——lesson 8）；
3. 行引用的 arm-index finding（`docs/audits/arm-index.md`）已回填 done/resolved（lesson 11）；
4. 行引用 owner doc 含声称的回填注记（定向 grep `RC-R1.<n>` / finding ID 标记，不全文阅读）；
5. 仅 21 行 A 类 ORM（RC-R1.43/44/45/49/51/57/58/60/66/67/70/71/72/73/74/75/80/81/82/84/87）：授权链三选一（(a) 2026-08-12 批量授权引用 / (b) 双独立子 agent 批准记录 / (c) 无回落执行记录）。

计划映射：67 份 glob `*rc-mr1-r1-*.md` 家族计划（覆盖多行）+ 3 份 finance 1838 家族（R1.41/42/43/46 行内引用）+ C 类「已实现确认」2 行（R1.47/55，无修复计划属预期）。

### 1.2 Layer 2 — 深度行为核验抽样

**Tier 1（30 行，全量）**：

- (a) A 类 ORM 21 行（清单同上）。
- (b) 会计过账核心路径行——机械提取过程：对 70 份计划 grep `PostingDispatcher|AcctDocProvider|VoucherFact|ErpFinPostingProcessor`，18 份命中；逐一阅读 Targets/涉及文件/Non-Goals 判定是否**真实改写/接线该生产代码**（背景提及/否决候选/Non-Goal 声明不算）。**最终清单 9 行**：RC-R1.17（SalReturnPostingDispatcher.buildEvent 运营代理门控）、RC-R1.18（SalReturnPostingDispatcher 同源消费 ReturnCostStrategyResolver）、RC-R1.42（ErpFinPostingProcessor.prepareContext 增 guardExchangeRate）、RC-R1.50（PurAcctDocProvider 1403 拆分 + PurInvoicePostingDispatcher 差异重载）、RC-R1.53（DisposalAcctDocProvider.createFacts 1606 两步流重构）、RC-R1.56（InvPostingDispatcher 跳过集扩展，`InvPostingDispatcher.java:159` 实证）、RC-R1.63（ProjectSettlementAcctDocProvider 质保金平衡腿，`ProjectSettlementAcctDocProvider.java:69-111` 实证）、RC-R1.64（TimesheetPostingDispatcher.buildEvent 汇率三态）、RC-R1.89（SalaryPostingDispatcher 三连计提，`SalaryPostingDispatcher.java:113-149` 实证）。
- 机械剔除 9 份（仅背景提及，附理由）：R1.61（候选 A InvPostingDispatcher 加分支被 D 裁决否决，接线走 purchase Processor）、R1.51（明示「不新增过账 Provider」）、R1.14/15（Non-Goal「不触会计过账核心路径」）、R1.46（纯读侧过滤，明示不触 PostingProcessor）、R1.8（跨域归集 Deferred）、R1.2（Non-Goal 不改 reverseProcess）、R1.12（明示不触及 PostingProcessor）、R1.27（Provider/VoucherFact 显式出范围）、R1.0（展开器元计划）。

**Tier 2（22 行，域分层确定性抽样）**：Tier 1 之外 59 行，按行号等距 + 每个触及域至少 1 行（抽样率 37% ≥ 20%）：

**R1.1（fin）、R1.3（mfg）、R1.5（hr）、R1.9（hr）、R1.11（pur）、R1.13（sal）、R1.19（sal）、R1.20（inv）、R1.23（crm）、R1.27（prj）、R1.28（cs）、R1.31（mnt）、R1.35（ct）、R1.36（b2b）、R1.37（log）、R1.40（md）、R1.47（fin/inv C 类）、R1.54（ast）、R1.55（inv/fin C 类）、R1.59（qa）、R1.62（prj）、R1.86（aps）**

域覆盖：fin/mfg/hr/pur/sal/inv/crm/prj/cs/mnt/ct/b2b/log/md/ast/qa/aps 全 17 个适用域各 ≥1 行（notify 为跨域子系统，无独立修复行）。

核验三维度：(i) 测试存在性（计划/roadmap 证据点名的测试文件 glob 实存）；(ii) 关键断言强度（打开测试文件核对断言覆盖核心行为 claim，非冒烟/return-null）；(iii) owner doc 契约对齐（每行抽 2 个关键断言对照 orm.xml / BizModel / *Errors.java / *Constants.java，≥2 漂移才 needs revision）。

### 1.3 过程纪律附加核验

roadmap §横切关注点 #4 + M0.1：抽样行 roadmap 行 ↔ arm-index finding 双向可追溯（行引 finding ID；finding done 行回引修复计划文件）。

---

## §2 Layer 1 文档面五点一致性 — 89 行核验结果

### 2.1 汇总

| 核验点 | 结果 | 说明 |
|---|---|---|
| (1) Status done + ✅ 注记 | 84/89 OK | 5 行裸 `done`（见 F2）；R1.70/71 有证据注记但缺 ✅ 符号（外观性，不计发现） |
| (2) 计划 completed + 独立 CAE | 88/89 OK | 70 份计划全部 `Plan Status: completed`；69 份有独立结束审计证据；**2040-3（R1.86/87/88）例外**（见 F1）；R1.47/55 C 类无修复计划属预期（已实现确认，arm-index resolved + 代码实存核验通过） |
| (3) arm-index 回填 | 89/89 OK | 107 个 finding 引用全部 done/resolved/修复落地（lesson-11 零陈旧；残存 todo 行均为 P2 watch-only successor，P2 登记不强制） |
| (4) owner doc 回填 | 87/89 OK | R1.52 证据声称的注记不存在（见 F3）；R1.83 无回填声称（并入 F2 裸 done）；R1.29 经 P1-RC-063 标记证实（`sku-multi-unit.md:201`） |
| (5) A 类授权链 | 21/21 OK | 全部持有 (a) 2026-08-12 批量授权引用（plan/roadmap 行标签）；其中 ≥12 行另持有 (b) 双独立子 agent 批准（R1.44 ses_ff99a6ee/ses_ff99a4b5、R1.49 ses_ff7cde42/ses_ff7cdbfb、R1.51 ses_ff71efed/ses_ff71d656、R1.57 ses_ff627bd0/ses_ff627a22、R1.58 ses_ff5e7709/ses_ff5e75a4、R1.60 ses_ff487f01/ses_ff487d53、R1.73-75 ses_fe852c87/ses_fe8529b3、R1.80 ses_fe312b7c/ses_fe312882、R1.87 appr1-7f3a2c/appr2-7f3a91 等） |

**Layer 1 完全 OK 行数：80/89**（9 行被 F1/F2/F3 波及：R1.86/87/88、R1.16/17/83/84/85、R1.52）。

### 2.2 发现项（详见 §4 裁决表）

- **F1（P1，lesson 8 closure-pending）**：`docs/plans/2026-08-19-2040-3-rc-mr1-r1-86-87-88-aps-auto-create-routing-dispatch-family.md:173` Status Note 自述「Closure Gates 8 项全部勾选，Plan Status → completed，**待独立结束审计 round 2 复核**」；`:188` 该计划唯一落盘的 Closure Audit Evidence 为 round 1 裁决「**needs completion（closure gaps，返回 EXECUTE）**」（五项收尾缺口）；执行者补齐缺口后自行翻 completed，**round 2 独立复核从未运行、无任何落盘证据**（全计划、docs/audits/、docs/logs/2026/08-20.md 均无 round 2 记录）。对照 R1.89（0518-3 计划 :152-154 同场景有 Iteration 1 FAIL → remediation → Iteration 2 PASS 完整链），2040-3 缺最后一环。波及 roadmap 行 `:478-480`（RC-R1.86/87/88）。
- **F2（doc-drift）**：5 行 Status 裸 `done` 无 ✅ 证据注记且行内无计划引用——`requirement-compliance-roadmap.md:408`（RC-R1.16）、`:409`（RC-R1.17）、`:475-477`（RC-R1.83/84/85）。其修复实质（计划 completed + arm-index done + owner doc 注记）均核验通过（16/17 的 returns.md:200/359、83-85 的 delivery-window.md:61/111 与 state-machine.md:100 均有注记），纯属登记面漂移。
- **F3（doc-drift）**：`requirement-compliance-roadmap.md:444`（RC-R1.52）证据列声称「owner doc `depreciation-and-posting.md` 注记」——该文件无任何补提/catchUp 注记（grep `补提|catchUpDepreciation|RC-R1.52` 仅命中先于修复的通用行 :214/:332；文件内唯一实现注记是 R1.53 的 1606 块 :139）。实质行为（catchUpDepreciation mutation + IDLE 守卫语义）已在代码与 state-machine.md:46（R1.54 注记内提及「catchUp 拒绝 IDLE（Phase 1 守卫）」）落地，非行为缺失。

---

## §3 Layer 2 深度核验结果

### 3.1 Tier 1 逐行（30 行）

**(i) 测试存在性：30/30 通过。** 33 个点名测试文件全部实存（含家族计划成员测试），0 缺失。代表映射：R1.43→TestErpFinBankStatementCounterpartyMatch、R1.44→TestErpFinReverseCloseAuditTrail、R1.49→TestErpMfgBomSnapshot、R1.51→TestErpSalReturnExchange、R1.56→TestErpInvStockTakeCompleteDiffMove、R1.63→TestErpPrjProjectSettlementRetention、R1.66→TestErpCsTicketTimerSession、R1.70→TestErpCsSurveySendJob、R1.71→TestErpCsCatalogFulfillmentEngine（16 @Test）、R1.81/82→TestErpDrpCrossDock/TestErpDrpLeadTimeStats、R1.84→TestErpLogDeliveryBooking、R1.87→TestErpApsAlternativeRouting、R1.89→TestErpHrSalaryPostingChain 等。

**(ii) 关键断言强度：通过。** 断言密度普查全部健康（asserts/@Test 均 ≥ 1.3，数值断言普遍存在）；深读 4 个会计关键文件证实断言为真实核心行为断言，非冒烟：

- R1.50：`TestErpPurPriceVariancePosting.java:96-98` 断言 AP_INVOICE 4 行结构 + `1403 = TOTAL_AMOUNT − 差异 = 50 借` + `PPV = 差异 × 数量 = 50 借（涨价）`（拆分金额逐行断言 + 借贷恒等）；
- R1.53：`TestErpAstAcctDocProviderAccountKey.java:51-101` 四组合（SCRAPPED±/SOLD±）行级结构 + **1606 网为零恒等式**断言；
- R1.89：`TestErpHrSalaryPostingChain.java:129-149` 三凭证科目/金额断言（社保 ER=15000×15%=**2250.00**、公积金 ER=**1800.00** 镜像口径）+ 三凭证 Dr==Cr 试算平衡收敛；
- R1.64：`TestErpPrjTimesheetMulticurrencyPosting.java:120` 断言 `amountFunctional=amount×rate=56000（折算失真消除）` + `:163` 错误码 `ERR_EXCHANGE_RATE_REQUIRED`（复用 R1.42 语义）。

**(iii) owner doc 契约对齐：0 漂移。** A 类 21 行 ORM 列全部在权威 orm.xml 实证：counterparty 3 列、reverseClose 3 列、cashFlowType、BOM 快照 3 实体 + 2 列、returnType/exchangeDeliveryId/exchangeReturnId（`app-erp-sales.orm.xml:904-905`）、ErpCrmTeamMember、isCritical×2、costRate + ErpPrjRole、ErpCsTicketTimerSession、escalation 5 列（`app-erp-cs.orm.xml:200-202/303-304`）、failureCount（:524）、ErpCsTicketFulfillmentStep、sku status、triggerType、matchingStrategy（`app-erp-drp.orm.xml:312`）、ErpInvDrpSupplierScore（:435）、ErpLogDeliveryBooking、aps 4 列（`app-erp-aps.orm.xml:104-106`）。过账 9 行代码对齐实证：`DisposalAcctDocProvider.java:40`（SUBJECT_DISPOSAL_CLEARING="1606"）、`ErpFinPostingProcessor.java:543/554/566`（guardExchangeRate + ERR_EXCHANGE_RATE_REQUIRED）、`SalaryPostingDispatcher.java:113-149`（tryPostSocialInsuranceER/HousingFundER + buildCompanyBorneEvent）、`InvPostingDispatcher.java:159`（盘点差异跳过集）。

### 3.2 Tier 2 逐行（22 行）

**(i) 测试存在性：22/22 通过。** 20 个点名测试类全部实存（TestErpFinBudgetEndToEnd、TestErpMfgBatchGenealogy、TestErpHrSurveyLifecycle、TestErpPurReceiveOverReceiptTolerance、TestErpSalOrderAvailabilityCheck、TestErpSalReturnCostAndGuards、TestErpInvBatchExpiryInterception、TestErpCrmLeadScoringRecalcJob、TestErpPrjPnlCalcJob、TestErpCsServiceCatalog、TestErpMntVisitReportAdditionalFault、TestErpCtContractExpiryJob、TestErpB2bPartnerOnboarding + StateMachineMatrix、TestErpLogDraftEscalationJob、TestErpMdSkuPriceValidation、TestErpAstIdleStateMachine、TestErpQaBusinessCancelLinkage、TestErpPrjExpenseAggregation）；R1.5 last-wins 断言在 TestErpHrAttendanceEngine 实存；R1.47/55 C 类以代码实存核验（`validateNotAlreadyAllocated` pessimistic-lock 链、`CostAdjustmentService.appendFifoAdjustLayer` 均在）。

**(ii) 关键断言强度（抽 4 行深读）：通过。**

- R1.1：`TestErpFinBudgetEndToEnd.java:196-197` 三通道 seed（BUDGET 1000/NORMAL 400/COMMITMENT 200）+ `available = 1000−400−200 = 400` 三项式数值断言 + :229 无 COMMITMENT 退化边界等价；
- R1.20：`TestErpInvBatchExpiryInterception.java:91-95` 过期批次 + 出库 → `ERR_BATCH_EXPIRED` 整笔回滚 + reserved/balance 不变断言；
- R1.36：TestErpB2bPartnerOnboarding 含 pass-rate config（CONFIG_ONBOARDING_TEST_PASS_RATE）门槛与 24h 监控窗口（goLiveDate 窗口数学）断言；
- R1.59：`TestErpQaBusinessCancelLinkage.java:63-69` PENDING 软删 → findByRelatedBill 查无 + delVersion 置位可审计 + 终态不动断言。

**(iii) owner doc 契约对齐（抽 3 行 × 2 断言）：0 漂移。**

- R1.1：budget.md 三项式 claim ↔ `ErpFinBudgetLineBizModel.java:115`（`available = budget − actual − commitment`）+ `:46` commitment 通道常量；
- R1.20：state-machine.md §4「可配置放行」claim ↔ `ErpInvErrors.java:59-60`（ERR_BATCH_EXPIRED + 注释引 owner doc）+ `ErpInvStockMoveProcessor` config 默认 true 消费；
- R1.36：partner-onboarding.md 门槛/监控 claim ↔ `ErpB2bOnboardingMonitorJob.java:133-135`（goLiveDate 窗口语义 + dateBetween 过滤）。

### 3.3 过程纪律（横切 #4 + M0.1）

抽样 52 行双向可追溯全部成立：roadmap 行引 finding ID（P1-RC-xxx 等 107 引用）↔ arm-index finding done 行回引修复计划（如 P1-RC-018 → plan 2026-08-16-0424-1、P1-RC-030 → 0424-2、P1-RC-041/042 → 1838-1 等，arm-index:159/:185 等逐行实证）。

---

## §4 发现项裁决表

| 编号 | 严重度 | 描述 | 证据（file:line） | 裁决建议 |
|---|---|---|---|---|
| F1 | **P1**（lesson 8 closure-pending） | 计划 2026-08-19-2040-3（RC-R1.86/87/88）声称 completed，但唯一落盘的独立结束审计是 round 1「needs completion」裁决；执行者补齐五项缺口后**自行翻 completed 并自述「待 round 2 复核」**，round 2 独立复核从未运行/未落盘 | `docs/plans/2026-08-19-2040-3-rc-mr1-r1-86-87-88-aps-auto-create-routing-dispatch-family.md:173`（自述待复核）、`:188`（round 1 needs completion 为唯一 CAE）；波及 `docs/backlog/requirement-compliance-roadmap.md:478-480` | **arm-index successor 登记 / 立即补跑**：由独立子代理（新会话）对 2040-3 补跑 round 2 closure audit 并落盘计划 CAE 节（对照 R1.89 0518-3 的 Iteration 2 PASS 范式）；补跑通过前 RC-R1.86/87/88 三行的闭包证据链不完整。非活跃代码缺陷（V.1/V.2 全绿已证行为面），故不走 MR0 |
| F2 | doc-drift | 5 行 roadmap Status 裸 `done`，无 ✅ 证据注记、无计划引用（修复实质已全部核验通过，纯登记面漂移） | `docs/backlog/requirement-compliance-roadmap.md:408`（R1.16）、`:409`（R1.17）、`:475-477`（R1.83/84/85） | **doc-drift-fix（当场可修）**：五行 Status 补 `done ✅（日期 + plan 引用 + 一句验证摘要）`，对齐其余 84 行格式 |
| F3 | doc-drift | RC-R1.52 证据列声称「owner doc `depreciation-and-posting.md` 注记」，该文件不存在补提注记（唯一注记是 R1.53 的 1606 块） | `docs/backlog/requirement-compliance-roadmap.md:444`；`docs/design/assets/depreciation-and-posting.md:139`（仅 R1.53 注记）、`:214/:332`（先于修复的通用文本） | **doc-drift-fix（当场可修）**：二选一——在 depreciation-and-posting.md 补「补提方式 B 实现注记（RC-R1.52）」块，或将 roadmap 证据列修正为实际回填位置（state-machine.md:46 R1.54 注记内含 catchUp IDLE 守卫语义） |

**零 P0**：未发现任何活跃数据破坏/会计错误/安全漏洞类缺陷；V.1/V.2（156 模块 install + 3789/0/0/1 test + checker 零漂移）与本审计行为面抽样互证。

**不计发现的观察**（登记备查，无行动义务）：RC-R1.70/71（`requirement-compliance-roadmap.md:462-463`）Status 为 `done（…证据注记…）` 但缺 ✅ 符号——证据实质完整，属格式外观差异。

### §4.1 裁决收口（2026-08-20 复审，本审计者 re-run）

执行者对 F1/F2/F3 完成裁决处置后，本审计者于同日对活仓逐项复审（定向 grep/read，只读）：

| 编号 | 处置声称 | 复审实证 | 收口状态 |
|---|---|---|---|
| F1 | 独立子代理（fresh session）补跑 round 2 closure audit，verdict passes | `docs/plans/2026-08-19-2040-3-...md:190-199` 新增 round 2 证据块——审计者声明「independent closing audit round 2（new session，2026-08-20，冷上下文独立审计者；非本计划执行者）」+ 五项 round-1 缺口逐项实仓复核（roadmap :478-480 三行 done ✅ / arm-index :291-293 三行 done / 08-20.md:16 日志条目 / equipment-integration.md §4.2 successor ②收口 / Closure Gates 全仓验证 + compliance-baseline.md:625 R2c 上调注记）+ `:199` 裁决「**passes closure audit**」+ `:173` Status Note 尾句同步更新 | **已收口 ✅**（残余 nit 见下方观察 1） |
| F2 | 5 行裸 done 补 `done ✅（日期 + plan 引用 + 验证摘要 + 结束审计 + arm-index）` 注记 | `requirement-compliance-roadmap.md:408-409`（R1.16/17 引 plan 2219-2 + `TestErpSalReturnCompliance` 5+4 路径 + 27 测试 + 独立结束审计通过 + returns.md/arm-index 回填）与 `:475-477`（R1.83/84/85 引 plan 2040-1 Phase 1/2/3 + TestErpLogShipmentDuplicateGuard/Booking/SalesDeliveryLinkage）；事实核对全过——5 注记点名的测试类与计数对活仓逐一相符（TestErpSalReturnCompliance 9 @Test = 5+4；DuplicateGuard 4 / Booking 9 / SalesDeliveryLinkage 5 @Test） | **已收口 ✅** |
| F3 | depreciation-and-posting.md §5.1 增「折旧补提（catch-up）方式 B 实现注记（RC-R1.52，P1-RC-029 收敛）」 | `docs/design/assets/depreciation-and-posting.md:220` 注记实存且五要素与 plan 2026-08-16-0424-2 claims 逐项相符——①`catchUpDepreciation(assetId, currentPeriod, missedPeriods[])` @BizMutation + per-mutation Processor；②IN_SERVICE 守卫（IDLE 不允许补提）；③单张汇总凭证（billHeadCode `资产编码#currentPeriod#CATCHUP` + 行 memo「补提 {periods}」+ 单期红冲 follow-up 声明）；④出售侧接线 `catchUpDepreciationToDisposalPeriod`；⑤`TestErpAstCatchUpDepreciation` 7 组（活仓实数 7 @Test 相符）；roadmap `:444` R1.52 证据声称由此为真 | **已收口 ✅** |

**复审残余观察（非阻塞，无行动义务）**：

1. F1 round-2 证据块以角色声明（新会话/冷上下文/非执行者）标识审计者独立性，未落盘会话 ID（转写声称 `ses_fe1f93ed2ffeaNLj9kVnZiiYrV` 在计划文件中 grep 零命中）。与本仓既有惯例一致（多份已接受 CAE 块同样仅角色声明、无 ses ID，如 0424-3/0518-1 等），不构成 lesson 8 违规；留作可追溯性 nit。
2. 原观察（RC-R1.70/71 缺 ✅ 符号）维持原状，证据实质完整，非阻塞。

---

## §5 VERDICT（2026-08-20 复审后最终）

**passes closure audit**（发现项全部裁决收口后复审通过；初裁 needs revision → 2026-08-20 同日复审改判）

**复审依据**（详见 §4.1）：

- F1（唯一阻止项）已闭合：plan `2026-08-19-2040-3` 落盘 round 2 独立结束审计证据（`:190-199`，裁决 **passes closure audit**，五项 round-1 缺口逐项以持久文件证据复核闭合；`:173` Status Note 尾句同步）——RC-R1.86/87/88 三行闭包证据链补全，满足本计划 Phase 3 退出标准「发现项全部裁决收口后复审通过」。
- F2/F3（doc-drift）已当场修复并经本审计者活仓复核：F2 五行注记格式与事实（测试类名/计数）全部相符；F3 注记五要素与 plan claims 逐项相符。
- Layer 1 由 80/89 → **89/89** 五点一致性通过；Layer 2（Tier 1 30 行 + Tier 2 22 行）三维度核验维持零漂移；零 P0 维持。

**残余非阻塞观察**（§4.1 复审观察 1/2，登记备查，无行动义务）：round-2 证据块以角色声明标识独立性、未落盘会话 ID（与本仓既有 CAE 惯例一致）；RC-R1.70/71 缺 ✅ 符号（证据实质完整）。

本 VERDICT 为终裁：roadmap MV V.3 行可据此翻 done（V.1/V.2 已由计划 Phase 1-2 全绿证据承载）。
