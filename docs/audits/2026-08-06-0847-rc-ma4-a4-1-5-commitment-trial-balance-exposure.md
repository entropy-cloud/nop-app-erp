# RC MA4 A4.1.5 — 承付凭证借贷不平时报平衡暴露核对（试算平衡 / 三大报表 / GL 路径是否过滤 COMMITMENT 影子凭证）

> Audit Status: closed
> 里程碑：MA4（运行时行为验证 / 平衡恒等式风险评估维度）
> 工作项：A4.1.5（MA4 运行时行为验证 — A1.2 §7 存疑点 2）
> 审计 plan：`docs/plans/2026-08-06-0847-2-rc-ma4-a4-1-5-commitment-trial-balance-exposure.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）
> 存疑点来源：`docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 2（承付凭证 header 借贷不平时报暴露）
> L1 真相源：`docs/design/finance/use-cases.md` UC-FIN-13 断言③（:257-258）
> 交叉引用：`docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-1（= A4.1.22 CF 污染，同根因家族不同控制点）
> 审计性质：**只读运行时核对**（grep + 读 Java + 引用 MA2/A1.2/A1.7），不改代码/ORM/api.xml/真相源（§9 冻结）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 不触发 MR0 |
| **P1**（新登记） | **1** | **P1-RC-091**（试算平衡快照 + 4 GL 重分类/重估服务[年度结转 / 损益结转 / 坏账准备 / 汇兑重估] exclude BUDGET only 未排 COMMITMENT → 平衡聚合路径纳入单边 Dr 承付行 → 跨组件口径不一致）→ 待 MR1（R1.0 展开为 RC-R1.n） |
| **接受**（caveat ③ 凭证结构本身） | **1** | L1 层面维持接受（L1 未规定凭证平衡；L2 drift `budget.md:255` 两行 vs 实现单行归 MA3） |

**整体裁决**：A1.2 §7 存疑点 2 的运行时核对完成。GL 路径过滤面**全集普查**（14 消费点，禁止抽样）+ 逐路径过滤条件核验 + 平衡恒等式破坏风险评估齐全。

**关键裁决——升级为 P1-RC-091**：试算平衡快照路径 `ErpFinAccountingPeriodProcessor.findPostedVoucherIds:385-394` 过滤条件 = `or(isNull(postingType), ne(postingType, BUDGET))`（:392）**仅排除 BUDGET，未排除 COMMITMENT**。承付凭证经 `CommitmentVoucherGenerator.writeCommitmentVoucher:119-182` 写**单行单边**（header `totalDebit=absAmount/totalCredit=0` :143-144，单行 `debitAmount=absAmount/creditAmount=0` :161-162）且 `docStatus=POSTED`（:149）+ `isReversed=false`（:145，正常 commit）→ 满足 `findPostedVoucherIds` 全部条件 → `populateTrialBalanceForAllSchemas:350-358` 将单边 Dr 行 `debitAmount` 累加入 `periodDebit`/`closingDebit`（`creditAmount=0` 不累加 credit）→ **试算平衡快照 closingDebit/closingCredit 求和失衡（ΣDebit > ΣCredit）**。即 config 开启承付（`budget-commitment-enabled=true`）+ PO approve 产生承付凭证时，**试算平衡恒等式（ΣDebit == ΣCredit）被破坏**。年度结转 / 损益结转 / 坏账准备 / 汇兑重估 4 路径同型同根因（共享 `or(isNull, ne(BUDGET))` BUDGET-only 模式 → 同纳单边 Dr 行）。

**与控制引擎对照（已正确，证实跨组件口径不一致）**：控制引擎 `ErpFinBudgetControlBiz.applyPostingTypeFilter:151-167` ACTUAL 通道 `notIn(BUDGET, COMMITMENT)` 正确排两者（P1-MA2-084 fix）；结转 `ErpFinBudgetScenarioCarryForwardProcessor:221-222` 排两者；报表 `ErpFinBudgetLineBizModel.getBudgetVsActual`（P1-RC-003，姊妹站点）。本 finding 的 5 GL 路径**未同步** P1-MA2-084 三通道分离修正 → 跨组件口径不一致（§2 P1④）。

**非 P0 论证**：(1) config 默认关闭（`isCommitmentEnabled` 默认 `Boolean.FALSE`），默认部署无承付凭证故默认路径试算平衡恒等式不破坏；(2) L1 UC-FIN-13 断言③（:257-258）仅要求"生成 COMMITMENT 凭证 + 红冲"，**未规定**试算平衡/三大报表过滤影子凭证——试算平衡过滤属实现侧完整性（非需求验收标准）；(3) COMMITMENT 凭证经专用 Generator 直接写入（`budget.md:283`），**不经** `ErpFinPostingProcessor.assertBalanced`（过账引擎 balance 校验），过账引擎 balance 校验**不触及**——即"会计过账正确性"（§2 P0④）的"过账"本身不被破坏（COMMITMENT 是 by design 的影子凭证，bypass 过账 balance 校验是其设计），破坏的是**试算平衡快照聚合层**（report/snapshot aggregation）；(4) 不破坏活跃数据（试算平衡是只读快照报表，不影响 GL 写入/库存余额/预算硬拦截决策——控制引擎独立三通道正确）。

**维持接受的项**：caveat ③ 承付凭证结构本身（单行单边 header 借贷不平）在 L1 层面维持接受（L1 未规定凭证平衡；与 A1.2 §5.2 caveat ③ 一致）——本核对确认"凭证结构本身"接受不变，但"试算平衡/聚合层是否暴露此不平衡"=不同控制点 → 升级 P1-RC-091（finding 针对的是**过滤逻辑缺口**而非**凭证结构**）。

**交叉去重声明**：(a) 与 **A1.7 §7 SP-1（= A4.1.22）CF 污染**同根因家族（BUDGET/COMMITMENT 过滤缺失）但**不同控制点**——A4.1.22 = `buildCashFlowDataset:299-323` 读 VoucherLine 不过滤 postingType（CF 仅取现金科目行），本 finding = 试算平衡快照 + 4 GL 重分类服务 BUDGET-only 过滤（Dr==Cr 聚合）；(b) 与 **P1-RC-003**（预算对比报表 `getBudgetVsActual` 两列 + COMMITMENT 计入 actual）同根因家族但**不同控制点**——P1-RC-003 = 报表列数/口径，本 finding = 试算平衡恒等式 + GL 重分类聚合；(c) 与 **P1-MA2-084**（控制引擎 actual 含 COMMITMENT，已 fix）姊妹站点——P1-MA2-084 fix 时未同步修 5 GL 路径。三者各自独立 finding，同根因家族。

本审计**不实施修复**（plan Non-Goals + §5 保护区域）；finding 经 MR1 批量修复通道（R1.0 展开为 RC-R1.n），修复触及损益结转/试算平衡核心路径须 ask-first 评估。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md` UC-FIN-13（A1.2 切片 L1 真相源）。本核对聚焦断言③（承付款）。

### UC-FIN-13 预算管理（编制/控制/对比）断言③ — 承付款（`use-cases.md:257-258`）

```
// 承付款
采购订单.APPROVED → 生成 COMMITMENT 凭证
订单 CANCELLED 或发票接收 → 红冲 COMMITMENT
```

**L1 契约边界判定（关键）**：L1 断言③ **仅要求**：(a) PO.APPROVED → 生成 COMMITMENT 凭证；(b) CANCELLED / 发票接收 → 红冲 COMMITMENT。L1 **未规定**：① 承付凭证的 Dr/Cr 结构（单行 vs 两行、借方/贷方科目）；② 承付凭证 header 借贷是否平衡（totalDebit == totalCredit）；③ 试算平衡报表 / 财务三大报表是否过滤 COMMITMENT 影子凭证。即"试算平衡是否暴露承付不平衡"是**实现侧完整性**问题，非 L1 验收标准。本判定是"非 P0/P1-by-需求契约"的根因——但 §2 P1④「跨组件契约行为不一致」仍适用（控制引擎已三通道分离，5 GL 路径未同步）。

---

## 2. 实现证据（L3，路径矩阵 + 逐路径过滤条件核验）

> 审计对象实仓 `module-finance/erp-fin-service/.../`。全集枚举所有读取/聚合 `ErpFinVoucherLine` / `ErpFinVoucher` 且按 `postingType` 过滤的 GL 路径（grep 关键词 `postingType` 全 finance 主代码，禁止抽样）。

### 2.0 承付凭证结构（L3 实测，复核 A1.2 §5.2 caveat ③）

`module-finance/erp-fin-service/.../service/budget/CommitmentVoucherGenerator.java` `writeCommitmentVoucher:119-182`：

- `setPostingType(POSTING_TYPE_COMMITMENT)`（:135）
- `voucher.setDocStatus(VOUCHER_STATUS_POSTED)`（:149）— **POSTED 状态**
- `voucher.setIsReversed(isReversal)`（:145）— 正常 commit 时 isReversal=false → **isReversed=false**
- DEBIT 科目（`resolveDcDirection` 非 CREDIT）：`totalDebit=absAmount` / `totalCredit=0`（:143-144）→ **header 借贷不平**
- 单行：`debitAmount=absAmount` / `creditAmount=0`（:161-162）→ **单边 Dr 行**

**关键结论**：承付凭证满足 `findPostedVoucherIds` 全部筛选条件（POSTED + isReversed=false + postingType≠BUDGET[=COMMITMENT]）→ **进入所有 BUDGET-only 过滤的聚合路径**。

### 2.1 GL 路径 postingType 过滤面全集普查（14 消费点，逐路径 5 字段核验）

> 过滤条件分类：**A** = 仅排除 BUDGET（`or(isNull, ne(BUDGET))`）；**B** = 排除 BUDGET+COMMITMENT（`notIn(BUDGET,COMMITMENT)`）；**C** = 仅取特定类型（`eq(X)`）；**D** = 不过滤（含 isNull 全取）。

| # | 路径 | 文件:行（写时实测） | 过滤分类 | 参与 Dr==Cr 平衡聚合? | COMMITMENT 单边 Dr 进入? | 既有 JUnit 承付场景覆盖 |
|---|------|---------------------|---------|---------------------|------------------------|------------------------|
| **1** | **试算平衡快照** | `ErpFinAccountingPeriodProcessor.java` findPostedVoucherIds:385-394（:392 过滤）+ populateTrialBalanceForAllSchemas:331-382（聚合 closingDebit/closingCredit :357-378） | **A**（仅排 BUDGET） | **是**（Σdebit→periodDebit/closingDebit，Σcredit→periodCredit/closingCredit） | **是**（COMMITMENT POSTED+isReversed=false+≠BUDGET → 入选 → 单边 Dr 累入 debit） | **无**（`TestErpFinPeriodClosePerf`/`TestErpFinPeriodCloseEndToEnd` 未启 `budget-commitment-enabled`） |
| **2** | **年度结转** | `AnnualCloseService.java`:339（过滤） | **A**（仅排 BUDGET） | 是（余额聚合 → 未分配利润结转） | 是（单边 Dr 累入余额 → 年结余额失真） | 无（`TestErpFinAnnualClose` 未启承付） |
| **3** | **损益结转** | `ProfitLossClosingService.java` findPostedVoucherIds:185-193（:192 过滤） | **A**（仅排 BUDGET） | 是（损益科目余额 → 本年利润结转） | 是（单边 Dr 累入损益余额 → 结转失真） | 无（`TestErpFinProfitLossClosing` 未启承付） |
| **4** | **坏账准备** | `BadDebtProvisionService.java`:310（过滤）+ :265-269 聚合（`creditAmount − debitAmount`） | **A**（仅排 BUDGET） | 是（按科目余额聚合计提基数） | 是（单边 Dr 增 debit → 计提基数失真） | 无（`TestErpFinBadDebt`/`TestErpFinBadDebtProvisionReversal` 未启承付） |
| **5** | **汇兑重估** | `ExchangeRevaluationService.java`:227（过滤）+ aggregateBankSubjectBookFunctional:219 聚合 | **A**（仅排 BUDGET） | 部分（银行科目余额重估） | **低影响**（承付追踪费用/AP 科目非银行科目 :179-219，实操不触；但过滤逻辑缺口同型） | 无（`TestErpFinExchangeRevaluation` 未启承付） |
| **6** | 预算对比报表 | `ErpFinBudgetLineBizModel.java` getBudgetVsActual:48-108（:64-65 过滤） | **A 变体**（`or(eq(BUDGET), or(isNull, ne(BUDGET)))` = BUDGET OR not-BUDGET） | 否（报表列数/口径） | 是（COMMITMENT 计入 actual） | **P1-RC-003**（已登记，姊妹站点；不同控制点：报表列数） |
| **7** | 控制引擎 ACTUAL 通道 | `ErpFinBudgetControlBiz.java` applyPostingTypeFilter:162-164 | **B**（`notIn(BUDGET,COMMITMENT)`） | 否（控制决策） | **否**（正确排除两者） | **强**（`testAvailableDeductsCommitmentSeparately` available=500 三通道） |
| **8** | 控制引擎 BUDGET/COMMITMENT 通道 | `ErpFinBudgetControlBiz.java`:154/157 | **C**（`eq(BUDGET)` / `eq(COMMITMENT)`） | 否（按通道分离） | by 设计（COMMITMENT 通道） | 强 |
| **9** | 预算方案结转 carryForward | `ErpFinBudgetScenarioCarryForwardProcessor.java`:221-222 | **B**（显式跳过 BUDGET 与 COMMITMENT） | 否（结转实际余额） | **否**（正确排除两者） | 强（与 P1-MA2-084 同期 fix） |
| **10** | BUDGET 凭证反查（红冲） | `BudgetVoucherGenerator.java`:253 | **C**（`eq(BUDGET)`） | 否（找待红冲凭证） | N/A（不查 COMMITMENT） | 强 |
| **11** | COMMITMENT 凭证反查（红冲） | `CommitmentVoucherGenerator.java`:296 | **C**（`eq(COMMITMENT)`） | 否（找待红冲凭证） | N/A（精确查 COMMITMENT） | 强 |
| **12** | **三大报表 BS/IS** | `ErpFinReportBizModel.java` loadGlBalances:386-413（读 GlBalance 表） | **N/A**（GlBalance 不持 BUDGET/COMMITMENT，`orm.xml:1740-1742` 注记） | 是（资产负债表/利润表恒等式） | **否**（安全 — BUDGET/COMMITMENT 不入 GlBalance） | A1.7 已证 BS/IS 安全（静态） |
| **13** | **三大报表 CF（现金流量表）** | `ErpFinReportBizModel.java` buildCashFlowDataset:299-323 → loadPostedVoucherLines:424-439（**无 postingType 过滤**）+ isCashSubjectCode:549-553 现金科目守卫 | **D**（不过滤 postingType）+ 现金科目守卫 | 部分（现金流量） | **低影响**（承付追踪费用/AP 非现金科目 1001/1002/1012/1031，实操不触） | **A1.7 §7 SP-1 = A4.1.22**（不同控制点：CF 不过滤 vs 试算平衡 BUDGET-only） |
| **14** | AR/AP 辅助账（PartnerBalance） | `PartnerBalanceUpdater.java` sumOpen:46-55（读 ErpFinArApItem）+ `ErpFinArApItemGenerator` | **N/A**（读 ArApItem 非 VoucherLine；COMMITMENT 不写 ArApItem） | 否（未核销辅助项） | 否（承付不产生 AR/AP 辅助项） | 强（承付凭证无 ArApItem 业财回链） |

**全集普查完整性自检（禁止抽样）**：grep `postingType` 全 finance 主代码（`rg "postingType" --glob '*.java' -g '!**/test/**' -g '!**/_gen/**' module-finance/`）命中的过滤表达式站点 = #1-#11（路径 #12-14 经 GlBalance/ArApItem 间接消费或不过滤）。query-level `addFilter(... postingType ...)` 站点全覆盖；reverse-lookup（#10/#11 `eq(X)`）与凭证写入（`setPostingType`）不计聚合缺口。**无遗漏消费点**。

### 2.2 关键聚合行为实测（试算平衡快照）

`ErpFinAccountingPeriodProcessor.populateTrialBalanceForAllSchemas:331-382`：

```
:332  List<Long> voucherIds = findPostedVoucherIds(period.getId());   // :392 BUDGET-only 过滤 → 含 COMMITMENT
:346  List<ErpFinVoucherLine> lines = lineDao.findAllByQuery(q);     // q: in("voucherId", voucherIds) → 含承付单边 Dr 行
:357  a.debit  = a.debit.add(l.getDebitAmount());                    // 承付行 debitAmount=absAmount 累入
:358  a.credit = a.credit.add(l.getCreditAmount());                  // 承付行 creditAmount=0 不累加
:377  tb.setClosingDebit(net > 0 ? net : ZERO);                      // net = debit−credit，承付行致 net 虚高
:378  tb.setClosingCredit(net < 0 ? net.negate() : ZERO);
```

**实测结论**：config 开启承付 + PO approve（金额 X）→ 试算平衡快照 ΣclosingDebit 比 ΣclosingCredit 多 X → **Dr > Cr，平衡恒等式破坏 X**。

---

## 3. 测试证据（L4，承付场景覆盖）

> 断言强度分档：强 = 断言平衡数值/凭证字段；无 = 该 GL 路径无承付场景测试。

| GL 路径 | 测试引用 | 承付场景覆盖 | 断言强度 |
|---------|----------|--------------|----------|
| #1 试算平衡快照 | `TestErpFinPeriodClosePerf` / `TestErpFinPeriodCloseEndToEnd` / `PeriodCloseTestSupport` | **未启** `budget-commitment-enabled=true` | 无承付场景（grep `budget-commitment-enabled\|budgetCommitmentEnabled\|COMMITMENT_ENABLED\|enableCommitment` 全 `*Test*.java` = **0 命中**） |
| #2 年度结转 | `TestErpFinAnnualClose` | 未启承付 | 无承付场景 |
| #3 损益结转 | `TestErpFinProfitLossClosing` | 未启承付 | 无承付场景 |
| #4 坏账准备 | `TestErpFinBadDebt` / `TestErpFinBadDebtProvisionReversal` | 未启承付 | 无承付场景 |
| #5 汇兑重估 | `TestErpFinExchangeRevaluation` | 未启承付 | 无承付场景 |
| #7/#9 对照（已正确） | `TestErpFinBudgetEndToEnd#testAvailableDeductsCommitmentSeparately` | 启承付 + 强断言 available=500 三通道 | 强（对照证实控制引擎正确） |

**L4 汇总**：5 个 BUDGET-only GL 聚合路径（#1-#5）**零承付运行时覆盖**——承付凭证经 `TestErpFinBudgetCommitment` 6 用例（A1.2 L4）验证生成/红冲，但这些测试**不触发**试算平衡快照/年结/损益结转/坏账/汇兑重估聚合路径。即"承付凭证存在时这些 GL 路径的平衡行为" = 运行时未验证（本核对以静态 grep + 代码阅读证实缺口）。

---

## 4. 运行时行为证据（L5，静态推导 + config 默认关闭的运行时影响）

### 4.1 平衡恒等式破坏风险评估（方法论 §2 判据）

**场景**：config `erp-fin.budget-commitment-enabled=true` + 采购订单 approve（金额 X）+ 期间结账。

**推导链**（每步 L3 实测锚点）：
1. PO.approve → `ErpPurOrderProcessor.runCommitmentCommitHook:197-211`（config-gate :198）→ `budgetCommitmentBiz.commit(...)` → `CommitmentVoucherGenerator.writeCommitmentVoucher:119-182` 写 POSTED 单边 Dr 凭证（totalDebit=X/totalCredit=0）。
2. 期间结账 → `ErpFinAccountingPeriodProcessor.populateTrialBalanceForAllSchemas:331` → `findPostedVoucherIds:385-394`（:392 `or(isNull, ne(BUDGET))`）→ COMMITMENT 凭证入选（postingType=COMMITMENT ≠ BUDGET）。
3. :350-358 聚合 → 承付单边 Dr 行 debitAmount=X 累入 periodDebit，creditAmount=0 不累加 credit。
4. :377-378 → closingDebit 含 X，closingCredit 不含 → ΣclosingDebit − ΣclosingCredit = X > 0 → **试算平衡恒等式破坏**。

**裁决**：试算平衡快照路径（#1）在 config 开启承付 + PO approve 时**确实**纳入单边 Dr 承付行 → **破坏 Dr==Cr 平衡恒等式**。年度结转（#2）/ 损益结转（#3）/ 坏账准备（#4）同型（共享 BUDGET-only 过滤，单边 Dr 累入余额聚合 → 余额失真）；汇兑重估（#5）低影响（承付非银行科目实操不触，但过滤逻辑缺口同型）。

### 4.2 三大报表路径裁决（交叉引用 A1.7）

- **BS/IS（#12）**：读 `GlBalance` 表，BUDGET/COMMITMENT 不入 GlBalance（`orm.xml:1740-1742` 注记「过账引擎本就不维护 ErpFinGlBalance，故预算不引入 GlBalance 结构变更」）→ **安全**（A1.7 已静态证，本核对复核确认）。
- **CF（#13）**：`buildCashFlowDataset` 读 VoucherLine 不过滤 postingType，但仅取现金科目（`isCashSubjectCode` 1001/1002/1012/1031）；承付追踪费用/AP 科目实操不触 → **低风险**（归 A1.7 §7 SP-1 = **A4.1.22**，不同控制点）。

### 4.3 config 默认关闭的运行时影响

- `ErpFinBudgetCommitmentBizModel.isCommitmentEnabled`（`AppConfig.var(..., Boolean.FALSE)`）默认 **false** → 默认部署无承付凭证 → 5 GL 路径 BUDGET-only 过滤在默认 config 下**不破坏平衡**（无 COMMITMENT 行可纳入）。
- **但**：`budget-commitment-enabled` 是**已文档化的可选功能**（`budget.md §配置项` + UC-FIN-13 断言③），客户启用即触发本 finding 描述的平衡破坏。故 config 默认关闭**降低默认路径风险**但**不消除** finding（启用即破坏，属真实运行时缺陷非理论）。

---

## 5. 符合性结论（§2 判据，裁决矩阵）

### 5.1 裁决矩阵

| 存疑点维度 | L1 契约 | L3 实现 | 运行时行为 | 裁决 |
|------------|---------|---------|------------|------|
| caveat ③ 承付凭证**结构本身**（单行单边 header 借贷不平） | UC-FIN-13 ③ 仅要求生成+红冲，**未规定**凭证平衡 | `writeCommitmentVoucher:119-182` 单行单边（A1.2 §5.2 实测） | 凭证生成+红冲行为正确（`TestErpFinBudgetCommitment` 6 用例强断言） | **接受**（L1 层面；L2 `budget.md:255` 两行 drift 归 MA3，与 A1.2 一致） |
| **试算平衡/聚合层是否过滤 COMMITMENT**（本核对核心） | L1 未规定（实现侧完整性） | 5 GL 路径（#1-#5）BUDGET-only 过滤；控制引擎（#7）+ carryForward（#9）正确排两者 | config 开启承付 + PO approve → 试算平衡快照 ΣDr > ΣCr X，平衡恒等式破坏 | **P1-RC-091**（§2 P1④ 跨组件契约行为不一致：控制引擎三通道分离 vs 5 GL 路径 BUDGET-only） |

### 5.2 P1 分级判据命中明细（§2）

#### P1-RC-091 — 试算平衡快照 + 4 GL 重分类/重估服务 exclude BUDGET only 未排 COMMITMENT（平衡恒等式破坏风险）

- **命中判据**：§2 **P1④**「需求契约要求的跨域契约行为不一致」——同一"actual/余额聚合"语义在**控制引擎**（`ErpFinBudgetControlBiz` ACTUAL 通道 `notIn(BUDGET,COMMITMENT)` 正确排两者，P1-MA2-084 fix）+ **carryForward**（`ErpFinBudgetScenarioCarryForwardProcessor:221-222` 排两者）与 **5 GL 聚合服务**（试算平衡/年结/损益结转/坏账/汇兑 `or(isNull, ne(BUDGET))` 仅排 BUDGET）间**口径不一致**。取最高 = P1。
- **三源对照**：
  - L1（`use-cases.md:257-258`）：仅要求生成+红冲，**未规定**试算平衡过滤（→ 非 P0④「需求契约要求的会计过账正确性破坏」——过账 balance 校验本身不被破坏，COMMITMENT 是 by design bypass 过账校验的影子凭证）。
  - L2（`budget.md:16` 「复用 ErpFinGlBalance 的 postingType 维度」+ `:97` BUDGET/COMMITMENT 影子凭证并行入账 + `:283` 承付凭证不经 AcctDocRegistry 路由）：设计意图 = BUDGET/COMMITMENT 为影子凭证，余额从 VoucherLine 派生。但 L2 未显式声明"试算平衡/重分类服务须过滤 COMMITMENT"——属实现侧完整性，L2 drift（`:16` 称复用 GlBalance 但 `orm.xml:1740-1742` 称不维护 GlBalance）归 MA3。
  - L3：5 GL 路径 #1-#5 = BUDGET-only（实测 :392/:339/:192/:310/:227）；控制引擎 #7 + carryForward #9 = 排两者（对照）。
- **运行时影响**：config 开启承付 + PO approve（金额 X）→ 试算平衡快照 ΣclosingDebit 比 ΣclosingCredit 多 X（Dr > Cr）；年结/损益结转/坏账余额聚合失真（承付单边 Dr 累入）；汇兑重估低影响（承付非银行科目）。**不影响预算硬拦截决策**（控制引擎独立三通道正确）+ **不破坏活跃数据**（试算平衡是只读快照）+ **不影响 GL 写入**（过账引擎 balance 校验对 COMMITMENT by design 不触及）。
- **严重性**：major（试算平衡是核心会计报表，config 开启承付时恒等式破坏，但 config 默认关闭 + 非活跃数据破坏 + L1 未规定过滤）。
- **P0 升级评估**：**维持 P1 不升 P0**。理由：(1) config 默认关闭，默认部署不破坏；(2) L1 未规定试算平衡过滤（非 §2 P0④「需求契约要求的会计过账正确性破坏」——过账正确性指 VoucherFact/PostingProcessor 路径，COMMITMENT by design bypass，过账 balance 校验不被破坏）；(3) 试算平衡是只读快照报表（非 GL 写入），破坏的是报表/快照聚合层；(4) 与 MA2 对承付族 + P1-RC-003 报表口径的 P1 分级一致（报表/快照正确性分歧，非活跃数据破坏）。
- **修复义务**：§5 Q4=(a) 强制实现，禁止方案 B。经 MR1（R1.0 展开为 RC-R1.n）。修复 = 5 GL 路径过滤条件从 `or(isNull, ne(BUDGET))` 改为 `or(isNull, notIn(BUDGET, COMMITMENT))`（与控制引擎 #7 / carryForward #9 对齐）。**触及损益结转/试算平衡核心路径**——按 §5 保护区域须 ask-first 评估是否属"会计过账逻辑核心路径"（VoucherFact/PostingProcessor）：
  - **不属核心路径**（仅 Voucher **查询过滤**，非 VoucherFact/PostingProcessor 写入/校验）→ 按 roadmap 预授权类目（代码逻辑修复）可自动执行。
  - **若裁决属核心路径**（试算平衡快照是会计过账衍生）→ 须 ask-first + 独立 plan-audit。
  - MR1 R1.0 展开时按 §5 暂停协议标注触及保护区域类别，由 plan 显式裁决。
- **与既有 finding 关系（§7 复用 or 新增）**：见 §6。

### 5.3 接受类结论汇总

| 项 | 接受依据 |
|----|----------|
| caveat ③ 承付凭证结构本身 | L1 UC-FIN-13 ③ 仅要求生成+红冲，未规定凭证平衡；L3 生成+红冲强测试覆盖（A1.2 L4）；L2 `budget.md:255` 两行 drift 归 MA3（§去重协议） |

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

> 产出 finding 前已 grep `arm-index.md` finance 试算平衡/三大报表/影子凭证过滤同域同控制点。裁决遵循 §7 规则。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本核对 finding 关系 | 裁决 |
|-----------------|--------|----------------------|------|
| **P1-RC-003** 预算对比报表 `getBudgetVsActual` 两列 + COMMITMENT 计入 actual | **报表**列数/口径（`ErpFinBudgetLineBizModel`） | **同根因家族**（BUDGET-only 过滤漏 COMMITMENT）但**不同控制点**：P1-RC-003 = 报表列数（Budget/Commitment/Actual 三列）+ DTO 字段缺失；本核对 = 试算平衡快照 Dr==Cr 恒等式 + 4 GL 重分类聚合（不同方法/不同类） | **新建 P1-RC-091** + 交叉引用 P1-RC-003（同根因家族姊妹站点） |
| **P1-MA2-084** 控制引擎 `aggregateAmount` actual 含 COMMITMENT（**已 fix**） | **控制引擎** actual 通道 | **姊妹站点**：P1-MA2-084 fix 时同步修了 carryForward（#9）但**未同步** 5 GL 聚合服务（#1-#5）+ 报表（P1-RC-003） | 新建 P1-RC-091，交叉引用 P1-MA2-084（fix 时遗漏的姊妹站点） |
| **A1.7 §7 SP-1（= A4.1.22）** CF 读 VoucherLine 不过滤 postingType | **CF 现金流量表**（`buildCashFlowDataset`） | **同根因家族**（BUDGET/COMMITMENT 过滤缺失）但**不同控制点**：A4.1.22 = CF 不过滤（含现金科目守卫），本核对 = 试算平衡 BUDGET-only（Dr==Cr 聚合） | 不同控制点，各自独立 finding；交叉引用声明同根因家族 |
| P2-RC-008 报表 CLOSED 期间门控 | 报表期间门控 | 不同控制点（期间门控 vs postingType 过滤） | 不相关 |
| P1-RC-007 CF 三分类缺失 | CF 分类维度 | 不同控制点（分类 vs 过滤） | 不相关 |

### 6.2 新建 finding 裁决

| Finding ID | 核对源 | 根因/控制点 | 与既有 finding 差异依据 | 裁决 |
|------------|--------|-------------|------------------------|------|
| **P1-RC-091** | A1.2 §7 存疑点 2 | 试算平衡快照 + 4 GL 重分类/重估服务 BUDGET-only 过滤漏 COMMITMENT（5 站点 #1-#5），config 开启承付时单边 Dr 行破坏 Dr==Cr 恒等式 + 跨组件口径不一致（控制引擎/carryForward 正确排两者，5 站点未同步） | P1-RC-003 = 报表列数；P1-MA2-084 = 控制引擎（已 fix，遗漏姊妹站点）；A4.1.22 = CF 不过滤——**新控制点**（试算平衡恒等式 + GL 重分类聚合），不可合并 | **新建**（交叉引用 P1-RC-003 + P1-MA2-084 + A4.1.22 同根因家族） |

### 6.3 双向可追溯

- **新 finding → arm-index**：P1-RC-091 写入 `arm-index.md` RC 发现追踪分区（§7 归档纪律）。
- **finding → 修复**：待 MR1 R1.0 展开为 RC-R1.n 修复行（本核对不实施修复）。
- **既有 finding 交叉引用**：P1-RC-091 交叉引用 P1-RC-003（报表姊妹站点）+ P1-MA2-084（控制引擎 fix 遗漏）+ A4.1.22（CF 同根因家族），MR1 修复时协同（5 站点过滤条件统一改 `notIn(BUDGET,COMMITMENT)`，与控制引擎/carryForward 已 fix 的模式对齐）。

---

## 7. 静态存疑点清单（供后续 MA4 展开）

> 本核对已将 A1.2 §7 存疑点 2 收口为 P1-RC-091（升级）。剩余存疑点交后续展开。

| SP | 存疑点 | 静态状态 | 后续确认方式 |
|----|--------|---------|--------------|
| — | （本核对无新增静态存疑点；A1.2 §7 存疑点 2 已升级 P1-RC-091 收口） | — | — |

**P0 即时通道**：本核对未出 P0（P1-RC-091 为 P1），按 §10 **不触发 MR0**，经 MR1 批量修复通道。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 详见下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本审计为只读核对（零生产代码变更），checker 无回归风险**。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1 裁决表），无未经比对直接新建的 finding。P1-RC-091 经 grep 确认为新控制点（试算平衡恒等式 + GL 重分类聚合），与 P1-RC-003/P1-MA2-084/A4.1.22 同根因家族不同控制点。

### checker actual vs baseline 实测表（2026-08-06 实测，零代码变更）

> 本审计为**只读核对**（无生产代码变更），故 checker 无回归风险；actual vs baseline 实测记录如下（基线源 `compliance-baseline.md §BASELINE (machine-readable)` 权威块）。

| 规则 | Baseline | Actual | 漂移 | 状态 |
|------|----------|--------|------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | 0 | ✅ |
| R1d | 14 | 14 | 0 | ✅ |
| R2a | 34 | 34 | 0 | ✅ |
| R2b | 229 | 229 | 0 | ✅ |
| R2c | 1382 | 1382 | 0 | ✅ |
| R2d | 34 | 34 | 0 | ✅ |
| R3 | 5 | 5 | 0 | ✅ |
| R4/R5 | 0/0 | 0/0 | 0 | ✅ |
| R6 | 2 | 2 | 0 | ✅ |
| R7 | 0 | 0 | 0 | ✅ |
| R8 | 0 | 0 | 0 | ✅ |
| R10 | 6 | 6 | 0 | ✅ |
| R11 | 0 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | 0 | ✅ |

全 19 规则 actual ≤ baseline，**0 漂移**。本审计无生产代码变更，无回归风险。

---

## 9. 与既有审计差异增量声明（§去重协议）

本核对声明与既有审计的差异增量：

- **复用 A1.2 已证实行为**（不重新核实）：A1.2 §5.2 caveat ③ 承付凭证结构（单行单边）+ §5.3 P1-RC-003（报表姊妹站点）+ L4 承付凭证生成/红冲强测试覆盖。
- **复用 A1.7 已证实行为**：A1.7 §2 ⑨ BS/IS 安全（GlBalance 不持 BUDGET/COMMITMENT）+ §7 SP-1 CF 低风险（不同控制点 = A4.1.22）。
- **复用 MA2 已证实行为**：P1-MA2-084 控制引擎三通道分离 fix（对照证实跨组件口径不一致）。
- **本核对只补的差异增量**（既有审计未覆盖）：
  1. **GL 路径过滤面全集普查**（14 消费点）：A1.2 仅核报表（getBudgetVsActual）+ 控制引擎；A1.7 仅核三大报表（BS/IS/CF）。本核对**首次**普查试算平衡快照 + 年结 + 损益结转 + 坏账 + 汇兑重估 5 GL 聚合路径的 postingType 过滤。
  2. **平衡恒等式破坏运行时推导**：A1.2 §7 存疑点 2 提出存疑（"是否被通用试算平衡报表暴露"），本核对以代码阅读 + 推导链证实 = **是，config 开启承付时破坏**，升级 P1-RC-091。
  3. **caveat ③ 收口**：A1.2 §5.2 caveat ③ 凭证结构维持接受（L1 层面），但"试算平衡暴露"维度升级 finding（不同控制点）。

---

## 10. Verdict

**Verdict: 承付凭证借贷不平→试算平衡暴露核对完成，升级 1 项新 P1（P1-RC-091）**

**审查范围**：GL 路径 postingType 过滤面全集普查（14 消费点）+ 逐路径 5 字段核验 + 平衡恒等式破坏风险评估 + 与 arm-index 衔接（§7 复用/新增裁决）+ §8 过程纪律自检 + 与 A1.2/A1.7/MA2 差异增量声明。

**接受类**：caveat ③ 承付凭证结构本身（L1 层面，与 A1.2 一致）。

**P1 残留**：P1-RC-091（试算平衡快照 + 4 GL 重分类/重估服务 BUDGET-only 过滤漏 COMMITMENT → config 开启承付时 Dr==Cr 恒等式破坏 + 跨组件口径不一致）→ MR1（R1.0 展开为 RC-R1.n）。修复触及损益结转/试算平衡核心路径，MR1 展开时按 §5 暂停协议标注触及保护区域类别（query 过滤非 VoucherFact/PostingProcessor 核心，倾向预授权自动执行，但 plan 须显式裁决）。

**P0**：无。不触发 MR0。

**剩余风险**：见 §4.3 config 默认关闭的运行时影响（默认部署不破坏，启用即破坏，属真实缺陷非理论）。

**解除 A1.2 §7 存疑点 2 + caveat ③ 收口**：A1.2 §7 存疑点 2（承付凭证借贷不平时报暴露）经本核对升级 P1-RC-091 收口；A1.2 §5.2 caveat ③（凭证结构）维持接受。本核对解除 A4.1.5 在 MA4（A4.1 展开器）及 MR1（R1.0）链路的该存疑点证据缺口。
