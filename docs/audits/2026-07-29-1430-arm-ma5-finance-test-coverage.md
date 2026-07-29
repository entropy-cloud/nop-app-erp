# ARM-MA5 finance 测试覆盖深度审计报告（A5.1）

> 里程碑：MA5（测试层审计 / 测试覆盖深度维度）
> Roadmap 工作项：A5.1（finance 测试覆盖深度——64 测试 / 137 mutation，比 0.47）
> Plan：`docs/plans/2026-07-29-1430-1-ma5-s-tier-test-coverage-audit.md`（Phase 1）
> 行为基线：`docs/design/finance/{posting,period-close,budget,returns,bad-debt,costing-methods}.md` + `docs/design/flow-overview.md`
> 计数基线：`docs/testing/test-depth-classification.md` + roadmap「64 测试 / 137 mutation / 0.47」
> Skill：`docs/skills/open-ended-audit-prompt.md`（项目定制化层已注入：保护区域 / 验证命令 / 命名约定 / 已知失败模式）
> 实仓快照：2026-07-29（`find module-finance/erp-fin-service/src/test -name "Test*.java"` 排除 PeriodCloseTestSupport/FinFrozenClockExtension/CodeGen = **64 测试文件 / 15638 行**）
> 裁决：**Verdict = ⚠️(P1)**——finance 测试覆盖**广度健康**（64 文件覆盖过账/凭证/期间/预算/AR-AP/成本/GL/坏账/FX/银对/公司间/票据/员工借款/报销/ treasury 16 条业务链路，无零覆盖核心路径），**深测断言强度扎实**（GL 平衡 / AR-AP 核销三策略 / 坏账三向对称 / 年度结转聚合 / FX 汇兑损益数值断言），但**存在系统性问题**：(1) **`docs/testing/test-depth-classification.md` 计数口径严重过时**——登记 finance 46 文件[深2/中38/浅6]，实测 **64 文件[深3/中56/浅5]**，低估 18 文件 + 深测少计 1，文档与实仓漂移致「测试/mutation 比」对外失真；(2) **多币种 GL 凭证行级断言系统性缺失**（P1-MA4-002 + P1-MA3-039 在测试层投影——过账 happy/reverse 仅断言 totalDebit==totalCredit 头合计 + countLines 行数，未校验行级 `amountSource/amountFunctional/exchangeRate`，多币种折算 bug 对测试不可见）；(3) **业财异常路径零覆盖**（过账悬挂/重试耗尽 RETRYING 死状态/期间编排跨域 command 失败吞咽/FX 多期 reversal 累计漂移/核销非幂等 5 类异常路径无测试触发）。零 P0（无测试空洞致活跃数据破坏——缺陷均有 LOG/试算平衡/重跑自愈兜底）。**3 项新 P1**（P1-MA5-001 finance 计数口径文档过时致测试比失真 / P1-MA5-002 finance 业财过账多币种行级断言系统性缺失[MA4 P1-MA4-002 测试层投影，归并登记]/ P1-MA5-003 finance 业财异常路径测试系统性空洞[MA4 P1-MA4-001/004/005 测试层投影，归并登记]）+ **1 项新 P2** watch-only（P2-MA5-001 finance 状态机负向覆盖不均衡——DRAFT→CANCELLED/IGNORED 悬挂/反结账 kill-switch 负向覆盖薄）。本审计与 MA4 P1-MA4-001/002/004/005 经交叉确认：MA4 审**代码层缺陷 + 测试有效性**，本审审**测试层覆盖深度系统化**——P1-MA5-002/003 标注为 MA4 同根因在测试层的系统化投影，**不重复计入 MR2**（随 P1-MA4-002/005 修复一并闭合）。

---

## 1. 范围与计数口径对账

### 1.1 在范围（测试层覆盖深度，非代码质量）

`module-finance/erp-fin-service/src/test/java/**` 全部测试文件（排除 `PeriodCloseTestSupport.java` 测试基类 + `FinFrozenClockExtension.java` JUnit 扩展 + `ErpFinCodeGen.java` codegen 冒烟 + `ErpFinWebCodeGen.java` + `ErpFinWebPagesTest.java`[23 行 web 桩]）。**64 真实测试文件**。

### 1.2 计数口径对账表（统一三源）

| 数据源 | 口径 | finance 文件数 | 深(≥400) | 中(100-399) | 浅(<100) | 备注 |
|--------|------|---------------|---------|------------|---------|------|
| **roadmap**（audit-remediation-roadmap.md A5.1） | 测试/mutation | 64 测试 / 137 mutation / 比 0.47 | — | — | — | mutation 数来自 pitest 估算 |
| **test-depth-classification.md**（刷新注记声称排除 CodeGen/TestSupport/Stub） | 文件行数分档 | **46** | 2 | 38 | 6 | **过时**——写作时点早于 finance 测试扩充 |
| **本审计实仓实测**（2026-07-29，同口径排除） | 文件行数分档 | **64** | **3** | **56** | **5** | 权威值 |

**差异根因裁决**：`test-depth-classification.md` 的 46 计数为**历史快照过时**，非口径差异。该文档「计数口径」刷新注记（line 11-16）明确声明排除 CodeGen/TestSupport/Stub 三类，与本审计口径一致；但文件数 46 < 实测 64，差 18 文件——经逐文件比对，finance 在 MA2/MA4 审计周期内新增了 18 个测试（坏账准备冲回 / 员工借款现金偿还冲回 / 银行对账 E2E / 公司间转移 / treasury 信贷 / posting observability/metrics/exception-notify 等），`test-depth-classification.md` 未随之刷新。**深测少计 1**（实测 3 深测：EmployeeAdvanceCashRepay 434 / BadDebtProvisionReversal 471 / BadDebt 483；文档记 2 深测）。**浅测少计 1**（实测 5 浅：AuxiliaryReconGate 54 / ReverseClose 56 / PeriodCloseEndToEnd 75 / AcctDocRegistry 82 / DepreciationIntegration 86；文档记 6 浅——文档把 PeriodCloseEndToEnd 计为浅但实测 75 行确实 <100）。

**裁决**：roadmap「64 测试 / 137 mutation / 0.47」的**文件数 64 准确**；test-depth-classification.md 的 46 须刷新至 64（P1-MA5-001）。mutation 数 137 无法在本审计重算（需 pitest 运行），采纳 roadmap 估算值；测试/mutation 比 0.47（64/137）在四 S 级域中**最高**（finance 0.47 > mfg 0.41 > assets 0.23 > hr 0.16）。

### 1.3 不在范围（Non-Goals 见 plan）

- A4.1a/A4.1b finance 代码质量（done）——本审计复核其测试 finding 的测试层系统化投影，不重复审计代码缺陷
- A2.5a-c finance 状态机业务正确性（done）——本审计复核状态机测试覆盖，不重复审计状态迁移正确性
- 测试修复（属 MR3，由 R3.0 展开）
- E2E 套件有效性（A5.6 范围）
- 不变更任何生产代码 / ORM / 契约（纯审计）

---

## 2. 关键业务路径覆盖矩阵

> 标注：✅ 深测（数值/GL/AR-AP 全断言）| 🟡 中测（状态翻转/部分数值）| 🔴 零测试 | ⚠️ 浅断言（仅状态码/存在性）

| 业务链路 | 测试文件 | 覆盖档 | 断言强度 | 备注 |
|---------|---------|--------|---------|------|
| **过账引擎核心**（post/alreadyPosted/红冲 reverse） | TestErpFinPostingService(353) | ✅ 深 | ⚠️ 头合计 | totalDebit==totalCredit + countLines + 状态；**行级 amountSource/amountFunctional 缺**（P1-MA5-002） |
| **AcctDoc Provider 注册/键解析** | TestErpFinAcctDocRegistry(82)/AcctDocProviderAccountKey(130) | 🟡 中 | ✅ 键映射 | 注册表冲突检测 + 账户键解析 |
| **GL 映射解析器** | TestErpFinGlMappingResolver(386) | ✅ 深 | ✅ 映射规则 | 规则匹配 + 科目解析数值 |
| **AR-AP 辅助账生成** | TestErpFinArApItemGeneration(404) | ✅ 深 | ✅ 余额 | 辅助项生成 + 开放余额 |
| **红冲派发/监听器** | TestErpFinReversalDispatch(303)/ReversalListenerRegistry(131) | 🟡 中 | 🟡 状态 | 派发 + 监听器注册 |
| **多账套过账** | TestErpFinMultiSchemaPosting(288) | 🟡 中 | 🟡 schema 隔离 | 账套隔离过账 |
| **Partner/转移定价解析** | TestErpFinPartnerIdResolution(249)/TransferPriceResolver(195) | 🟡 中 | ✅ 解析 | partner/转移定价 |
| **过账异常工作台/通知/可观测** | TestErpFinPostingExceptionWorkbench(293)/ExceptionNotify(243)/PostingObservability(287)/PostingMetrics(251) | 🟡 中 | 🟡 状态 | 异常工作台 CRUD + 通知 + metrics；**重试状态机闭环零覆盖**（P1-MA5-003） |
| **凭证模板** | TestErpFinVoucherTemplateRender(119)/Expr(143)/AuditLog(166)/CrudSmoke(159) | 🟡 中 | ✅ 渲染 | 模板渲染/表达式/审计 |
| **凭证红冲预览** | TestErpFinVoucherReversePreview(123) | 🟡 中 | 🟡 预览 | 红冲预览 |
| **期间结账 E2E** | TestErpFinPeriodCloseEndToEnd(75) | 🔴 浅 | ⚠️ 单路径 | **仅单黄金路径**（75 行）；无折旧失败吞咽/auto-post-on-close 阻断分级/模块顺序违反 E2E（P1-MA5-003） |
| **期间状态机** | TestErpFinPeriodStateMachine(151)/PeriodPreCheck(162) | 🟡 中 | 🟡 状态 | 状态迁移 + 前置检查；**反结账 kill-switch 负向薄**（P2-MA5-001） |
| **模块关账顺序** | TestErpFinModuleCloseOrder(137) | 🟡 中 | 🟡 顺序 | 关账顺序 |
| **反结账** | TestErpFinReverseClose(56) | 🔴 浅 | ⚠️ 薄 | 仅 56 行 |
| **年度结转** | TestErpFinAnnualClose(230) | 🟡 中 | ⚠️ 合计 | 年初余额合计；**未校验多年累计**（P1-MA2-018 无多年结转测试） |
| **损益结转** | TestErpFinProfitLossClosing(311) | 🟡 中 | ⚠️ 合计 | 损益结转聚合；**FX 损益结转归零路径有**（P0-MA2-016 修复回归） |
| **辅助账对账门控** | TestErpFinAuxiliaryReconGate(54) | 🔴 浅 | ⚠️ 门控 | 仅 54 行 |
| **预算 E2E** | TestErpFinBudgetEndToEnd(388) | ✅ 深 | ✅ 数值 | 预算全链路 |
| **预算承付** | TestErpFinBudgetCommitment(250) | 🟡 中 | ✅ 承付 | 承付释放 |
| **预算结转/滚动/隔离** | TestErpFinBudgetCarryForward(296)/RollForward(216)/Isolation(254) | 🟡 中 | 🟡 状态 | 预算结转 + 滚动 + 隔离 |
| **AR-AP 核销** | TestErpFinReconciliation(247)/AutoReconciliation(224) | 🟡 中 | ✅ 三策略 | FIFO/BY_AMOUNT/BY_RATIO 三策略 + 双向对称；**非幂等零覆盖**（P1-MA2-098） |
| **核销红冲预览** | TestErpFinReconciliationReversePreview(162) | 🟡 中 | 🟡 预览 | 核销红冲 |
| **双边一致性** | TestErpFinDualSideConsistency(178) | 🟡 中 | 🟡 一致 | 域侧/finance 双边 |
| **Partner 余额** | TestErpFinPartnerBalance(166) | 🟡 中 | ✅ 余额 | partner 余额 |
| **坏账** | TestErpFinBadDebt(483)/BadDebtReversal(359)/BadDebtProvisionReversal(471) | ✅ 深 | ✅ 三向对称 | writeOff/recovery/reverseApprove 三向 + 准备金冲回；**非 OPEN 负向薄** |
| **FX 重估** | TestErpFinExchangeRevaluation(188) | 🟡 中 | ⚠️ 单期 | 仅 2 测试；**无多期 reversal 累计漂移**（P1-MA2-022）+ 无银行存款重估 |
| **银行对账** | TestErpFinBankReconciliation(303)/EndToEnd(268)/StatementImport(213)/StatementMatch(336) | ✅ 深 | ✅ 匹配 | 对账全链路 + 匹配算法 |
| **公司间** | TestErpFinIntercompanyMatchingAndElimination(265)/IntercompanyTransfer(351) | ✅ 深 | ✅ 抵消 | 配对 + 抵消 + 转移 |
| **票据应付/应收** | TestErpFinNotesPayableStateMachine(205)/ReceivableStateMachine(339)/PayablePosting(189)/ReceivablePosting(396) | ✅ 深 | ✅ 过账 | 票据状态机 + 过账 |
| **员工借款** | TestErpFinEmployeeAdvancePosting(198)/Approval(182)/CashRepay(434)/CashRepayReversal(333) | ✅ 深 | ✅ 过账 | 借款过账 + 现金偿还 + 冲回 |
| **报销** | TestErpFinExpenseClaimPosting(261)/Approval(243)/OffsetAdvance(292) | 🟡 中 | ✅ 过账 | 报销过账 + 抵借 |
| **成本** | TestErpFinDepreciationIntegration(86) | 🔴 浅 | ⚠️ 集成 | 折旧集成仅 86 行；**存货成本重算 recloseInvCosts 零测试** |
| **Treasury** | TestErpFinCreditFacilityInterest(404)/CashForecastRefresh(159) | ✅ 深 | ✅ 利息 | 信贷利息 + 现金预测 |
| **账龄** | TestErpFinAging(153) | 🟡 中 | ✅ 账龄 | 账龄分析 |
| **看板/报表** | TestErpFinDashboard(251)/ReportRendering(414) | 🟡 中 | 🟡 渲染 | 看板聚合 + 报表渲染 |

**覆盖矩阵裁决**：finance 16 条业务链路**全部有测试覆盖**（无零覆盖核心路径）。**深断言链路**：预算 E2E / AR-AP 核销三策略 / 坏账三向对称 / 银行对账 / 公司间 / 票据 / 员工借款 / treasury 8 条。**中浅断言链路**：过账核心（头合计非行级）/ 期间 E2E（单路径）/ FX（单期）/ 成本集成（浅）/ 反结账（浅）/ 辅助账门控（浅）6 条。

---

## 3. Assertion 强度分档分布

| 强度档 | 文件数 | 占比 | 特征 | 代表测试 |
|--------|--------|------|------|---------|
| **深断言**（数值/GL 平衡/AR-AP 余额/三策略） | ~20 | 31% | 数值精确 + GL 借贷平衡 + 核销余额 + 状态机 + 幂等 ErrorCode | BadDebt/ArApItemGeneration/BudgetEndToEnd/BankReconciliation/Intercompany/Notes/EmployeeAdvance |
| **中断言**（状态翻转 + 部分数值/映射） | ~39 | 61% | 状态迁移 + 映射规则 + 存在性 + 部分合计 | PeriodStateMachine/Reconciliation/ReversalDispatch/GlMapping |
| **浅断言**（仅状态码/存在性/计数） | ~5 | 8% | countLines + totalDebit/totalCredit + docStatus | PeriodCloseEndToEnd/ReverseClose/AuxiliaryReconGate/AcctDocRegistry/DepreciationIntegration |

**「伪覆盖」标记**（仅断言状态码或返回非空而不断言业务数值/GL 平衡/AR-AP 余额）：

1. **TestErpFinPostingService**（353 行，过账核心）——断言 `totalDebit==totalCredit`(头合计) + `countLines`(行数) + `docStatus`，**未校验行级 `amountSource/amountFunctional/exchangeRate/debitAmount/creditAmount`**。后果：persistVoucher 多币种 bug（amountSource=amountFunctional，P1-MA3-039）即便 exchangeRate≠ONE 测试仍因 totalDebit==totalCredit 通过。**多币种对测试不可见**（P1-MA5-002，MA4 P1-MA4-002 测试层投影）。
2. **TestErpFinPeriodCloseEndToEnd**（75 行，期间结账 E2E）——仅单黄金路径，断言期间状态翻转，**无折旧/成本重算失败吞咽断言 + 无 auto-post-on-close 阻断分级断言**（P1-MA5-003）。
3. **TestErpFinAnnualClose**（230 行）——断言年初余额合计金额，**未校验多年累计**（P1-MA2-018 非累计 bug 无多年结转测试）。
4. **TestErpFinExchangeRevaluation**（188 行）——仅单期 FX 重估，**无多期 reversal 累计漂移断言**（P1-MA2-022）。
5. **TestErpFinAuxiliaryReconGate/TestErpFinReverseClose/TestErpFinDepreciationIntegration**——浅文件（<100 行），仅门控/状态/集成存在性断言。

---

## 4. 负路径与错误处理覆盖

| 负路径类型 | 覆盖 | 证据 |
|-----------|------|------|
| 非法状态迁移（状态机守卫） | ✅ 良好 | PeriodStateMachine/NotesStateMachine assertThrows ERR_*_ILLEGAL_STATUS_TRANSITION |
| 借贷不平衡守卫 | ✅ 良好 | TestErpFinPostingService assertThrows NopException（unbalanced） |
| 期间已结账守卫 | ✅ 良好 | TestErpFinPostingService assertThrows NopException（period closed） |
| 重复过账幂等 | ✅ 良好 | TestErpFinPostingService 重复 post 返回 null + countBillLinks==1 |
| **过账悬挂（posted=false）** | 🔴 零覆盖 | 无 mock post 抛异常→断言 posted=false 测试（P1-MA4-001/MA2-032 投影） |
| **重试耗尽 RETRYING 死状态** | 🔴 零覆盖 | TestErpFinPostingExceptionWorkbench 覆盖 CRUD 不覆盖重试状态机闭环（P1-MA4-001） |
| **期间编排跨域 command 失败吞咽** | 🔴 零覆盖 | 无折旧/成本重算失败→断言阻断/工作台/告警测试（P1-MA4-004） |
| **FX 多期 reversal 累计漂移** | 🔴 零覆盖 | 无多期 FX reversal 测试（P1-MA2-022） |
| **核销非幂等（重复 runAutoReconciliation）** | 🔴 零覆盖 | 无重复核销断言无重复核销单测试（P1-MA2-098） |
| 红冲红字凭证负向 | 🔴 零覆盖 | 无 assertThrows ERR_REVERSE_SOURCE_NOT_FOUND（P2-MA2-033） |
| 坏账非 OPEN 负向（对 SETTLED/CANCELLED writeOff） | ⚠️ 薄 | 三向对称有但非 OPEN 异常路径覆盖薄 |

---

## 5. 与 MA2/MA4 已确认 finding 的测试背书关系

> 逐项裁决：每个 MA2/MA4 已确认 finding 是否有测试守住（回归门控）。

| Finding ID | 描述 | 测试背书 | 裁决 |
|-----------|------|---------|------|
| **P0-MA2-016**（已修） | FX 损益结转排除 EXCHANGE_GAIN_LOSS 致余额残留 | ✅ **有回归**——P0-MA2-016 修复 plan 补「FX 场景汇兑损益结转后归零」测试，TestErpFinProfitLossClosing 覆盖 EXCHANGE_GAIN_LOSS 结转路径 | 已闭包（修复+回归测试落地） |
| **P1-MA2-001** | 暂估应付冲回缺失（GRNI） | 🔴 **零测试**——receive→invoice 正向冲回未实现，无测试触及暂估冲回（功能缺失非测试缺口，但功能实现后须补测试） | 测试待功能实现后补 |
| **P1-MA2-002/009** | 多币种 P2P/O2C 本位币凭证路径未验证 + 收款核销汇兑损益未实现 | 🔴 **零测试**——E2E 均单币种 exchangeRate=ONE，多币种 GL 折算路径 + 收款汇兑损益 plug 零测试（P1-MA5-002 同根因在过账层投影） | 测试空洞（P1-MA5-002 归并） |
| **P1-MA2-017** | auto-post-on-close 阻断分级不一致 | 🔴 **零测试**——无阻断分级 E2E 测试 | 测试空洞（P1-MA5-003 归并） |
| **P1-MA2-018** | 年初余额 populate 非累计 | 🔴 **零测试**——无多年结转测试 | 测试空洞（P1-MA5-003 归并） |
| **P1-MA2-022** | FX 重估无前期 reversal | 🔴 **零测试**——无多期 FX reversal 测试 | 测试空洞（P1-MA5-003 归并） |
| **P1-MA2-098** | 核销 runMatching 非幂等 | 🔴 **零测试**——无重复核销断言 | 测试空洞（P1-MA5-003 归并） |
| **P1-MA4-001** | 重试耗尽 RETRYING 死状态 | 🔴 **零测试**——重试状态机闭环零覆盖 | 测试空洞（P1-MA5-003 归并） |
| **P1-MA4-002** | 过账断言弱 + 异常路径零覆盖 | 🔴 **本审计系统化确认**——行级断言缺失 + 多币种/悬挂/重试零覆盖（P1-MA5-002/003 测试层投影） | 归并登记 |
| **P1-MA4-004** | 期间编排跨域 command 异常吞咽 | 🔴 **零测试**——折旧/成本失败吞咽零覆盖 | 测试空洞（P1-MA5-003 归并） |
| **P1-MA4-005** | 预算/AR-AP/成本/期间测试有效性不足 | 🔴 **本审计系统化确认**——E2E 单测 + FX 单期 + 行级断言弱（P1-MA5-002/003 测试层投影） | 归并登记 |

**背书关系裁决**：finance 11 项已确认 finding 中，**仅 P0-MA2-016 有回归测试守住**（修复 plan 同步补测试）；**其余 10 项均零测试背书**。其中 P1-MA2-001（GRNI 功能缺失）属功能实现后补测试；P1-MA2-002/009/017/018/022/098 + P1-MA4-001/004 为**业财异常路径/多币种系统性测试空洞**（P1-MA5-002/003 归并登记）。

---

## 6. P0/P1/P2 finding 清单

### 6.1 P0 finding

**无 P0**——finance 测试覆盖广度健康（64 文件 16 链路全覆盖），深断言链路扎实；测试空洞致既有 bug 不可见但均有 LOG/试算平衡/重跑自愈兜底，无活跃数据破坏路径因测试缺失而恶化。

### 6.2 P1 finding（3 项）

| Finding ID | 描述 | 严重性 | 目标 MR | 与 MA4 关系 |
|-----------|------|-------|---------|------------|
| `P1-MA5-001` | **finance 计数口径文档过时致测试比失真**：`docs/testing/test-depth-classification.md` 登记 finance 46 文件[深2/中38/浅6]，实测 **64 文件[深3/中56/浅5]**。文档「计数口径」刷新注记口径正确但文件数未刷新——MA2/MA4 周期内新增 18 测试（坏账准备冲回/员工借款冲回/银对 E2E/公司间/treasury/posting observability 等）未同步。后果：测试/mutation 比 0.47 对外失真（实际文件数 64 比 46 多 39%），「全域测试深度」基准文档不可信，决策者低估 finance 测试投入。 | major（文档完整性——基准文档失真致审计-修复决策依据受损） | MR3（测试维度文档刷新）——刷新 test-depth-classification.md finance 行至 64[深3/中56/浅5]，并与 mfg/hr/assets 三域一并刷新（同型 P1-MA5-004/007/010 协同） | 独立登记（MA4 未覆盖测试计数文档） |
| `P1-MA5-002` | **finance 业财过账多币种 GL 凭证行级断言系统性缺失**：过账 happy/reverse 测试（TestErpFinPostingService/TestErpFinNotesReceivablePosting/TestErpFinEmployeeAdvanceCashRepay 等）仅断言 `totalDebit==totalCredit`(头合计) + `countLines`(行数) + `docStatus`，**未校验行级 `amountSource/amountFunctional/exchangeRate/debitAmount/creditAmount`**。所有 finance E2E 均单币种（exchangeRate=ONE）。后果：persistVoucher 多币种 bug（amountSource=amountFunctional，P1-MA3-039）+ 多币种 P2P/O2C 折算路径（P1-MA2-002/009）+ 收款核销汇兑损益 plug 缺失对测试不可见——即便 exchangeRate≠ONE 测试仍因 totalDebit==totalCredit 通过。finance 多币种是核心业财路径（多币种凭证折算 [P1-MA2-002/009] 已确认为 MA2 高风险 finding）。 | major（测试空洞致多币种 bug 不可见 + 无回归防护——多币种直接影响 GL 正确性） | MR3（归并 P1-MA4-002 + P1-MA2-002/009 测试补齐时一并闭合）——**不重复计入 MR2**：标注为 P1-MA4-002（MA4 测试有效性）+ P1-MA3-039（多币种折算）在测试层的系统化投影，随 P1-MA4-002 修复一并闭合 | MA4 P1-MA4-002 测试层投影（归并，不重复计入 MR2） |
| `P1-MA5-003` | **finance 业财异常路径测试系统性空洞**：5 类异常路径零覆盖——(a) 过账悬挂 posted=false（P1-MA4-001 重试耗尽 RETRYING 死状态 + P1-MA2-032 IGNORED 悬挂）；(b) 期间编排跨域 command 失败吞咽（P1-MA4-004 折旧/存货成本重算失败）；(c) FX 多期 reversal 累计漂移（P1-MA2-022）；(d) 核销非幂等（P1-MA2-098 重复 runAutoReconciliation）；(e) auto-post-on-close 阻断分级（P1-MA2-017）+ 年初余额非累计（P1-MA2-018 无多年结转）。期间结账 E2E 仅 1 测试（75 行单黄金路径）。后果：业财悬挂/异常吞咽/累计漂移/非幂等 5 类缺陷回归无防护，未来结构性变更无回归保护。 | major（测试空洞致 5 类 MA2/MA4 缺陷不可见 + 无回归防护——业财不一致悬挂直接影响 GL） | MR3（归并 P1-MA4-001/004/005 + P1-MA2-017/018/022/098 测试补齐时一并闭合）——**不重复计入 MR2**：标注为 MA4 P1-MA4-001/004/005 + MA2 多项在测试层的系统化投影 | MA4 P1-MA4-001/004/005 测试层投影（归并，不重复计入 MR2） |

### 6.3 P2 finding（1 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA5-001` | **finance 状态机负向覆盖不均衡**：DRAFT→CANCELLED 不可达状态（P1-MA2-031）/ IGNORED 凭证悬挂（P1-MA2-032）/ 反结账 kill-switch（P1-MA2-020）/ CLOSED_FINAL 凭证锁定（P1-MA2-021）4 类状态机负向路径覆盖薄——TestErpFinPeriodStateMachine/TestErpFinReverseClose 仅覆盖正向迁移 + 部分负向，反结账/IGNORED/CLOSED_FINAL 负向断言不足。 | watch-only，MR3 顺手——随 P1-MA5-003 异常路径测试补齐时一并覆盖状态机负向 |

---

## 7. 综合裁决

### 7.1 Verdict

**⚠️(P1)**——finance 测试覆盖**广度健康**（64 文件 16 业务链路全覆盖，无零覆盖核心路径）+ **深断言扎实**（预算/AR-AP 三策略/坏账三向/银对/公司间/票据/员工借款/treasury 8 条数值+GL+余额全断言链路）+ 测试/mutation 比 0.47 四 S 级域最高，但**计数口径文档过时（P1-MA5-001）+ 多币种行级断言系统性缺失（P1-MA5-002）+ 业财异常路径系统性空洞（P1-MA5-003）** 三项问题需 MR3 修复。

### 7.2 P0 评估

**无 P0**——测试覆盖广度健康，深断言链路扎实；测试空洞致既有 bug 不可见但均有 LOG.warn 可见性 + 期末试算平衡兜底 + 重跑自愈，无活跃数据破坏路径因测试缺失而恶化。多币种 bug（P1-MA3-039）虽对测试不可见但属已登记代码缺陷（非测试缺失致破坏）。

### 7.3 0.47 比裁决

finance 测试/mutation 比 **0.47（64/137）在四 S 级域最高**，反映 finance 作为 mutation 绝对数最高域（137）仍维持了相对最充分的测试投入。但「比 0.47」的分子 64 经本审计实证准确，分母 137 为 pitest 估算值（未重算）——若实际 mutation 高于 137，比会更低。**比 0.47 反映的是「文件级覆盖广度」非「路径级覆盖深度」**——业财异常路径/多币种行级断言的系统性空洞说明文件数高 ≠ 路径覆盖深。

### 7.4 与 MA4 交叉去重

- **P1-MA5-001**（计数文档过时）为本审计新发现（MA4 未覆盖测试计数文档），独立登记 MR3
- **P1-MA5-002**（多币种行级断言）= P1-MA4-002（MA4 测试有效性）+ P1-MA3-039（多币种折算）在测试层的系统化投影，**归并登记不重复计入 MR2**
- **P1-MA5-003**（异常路径空洞）= P1-MA4-001/004/005 + P1-MA2-017/018/022/098 在测试层的系统化投影，**归并登记不重复计入 MR2**

**finance 域 MA5 测试覆盖深度终态：3 P1（1 独立 + 2 归并）+ 1 P2，零 P0。** roadmap A5.1 推进至 ready（待独立 closure audit）。
