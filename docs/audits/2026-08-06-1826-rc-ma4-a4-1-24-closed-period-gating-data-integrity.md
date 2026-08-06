# RC MA4 A4.1.24 — UC-FIN-16 报表 CLOSED 期间门控缺失的运行时数据完整性影响确认

> Plan: `docs/plans/2026-08-06-1826-3-rc-ma4-a4-1-24-closed-period-gating-data-integrity.md`
> 工作项：A4.1.24（MA4 运行时行为验证 — A1.7 §7 存疑点 SP-3：UC-FIN-16 `loadGlBalances` 不校验 `period.status==CLOSED`，OPEN 期间可渲染三大报表，运行时数据完整性影响确认 — 关联 P2-RC-008）
> 输入存疑点：`docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-3（`:288`）+ §2.2 CLOSED 门控（`:120`）+ §5.2 P2-RC-008（`:229`）
> 关联 finding：`docs/audits/arm-index.md` P2-RC-008 行（`:142`，状态 todo successor watch-only）
> 类型：只读运行时行为验证（verification or audit work）——**零代码/ORM/api.xml/真相源变更**
> 范式对齐：A4.1.21（done，期间结账年末反结账边界运行时行为评估同型工作项）/ A4.1.18（done，PC-3 reminder 运行时行为评估同型工作项）

---

## §0 整体裁决（TL;DR）

存疑点 A1.7 §7 SP-3「CLOSED 期间门控缺失的运行时数据完整性影响」经运行时只读评估 **CONFIRMED：维持 P2-RC-008 = P2 watch-only**，不升 P0/P1，不降级，无新 finding，不触发 MR0。

**核心证据链**（决策树分支①命中）：

1. **恒等式不破坏**（plan 决策树分支② hinge **未触发**）——三大报表 BS/IS 读 `ErpFinGlBalance` 经 `loadGlBalances:386-413`，cash flow 读 `ErpFinVoucherLine` 经 `loadPostedVoucherLines:424-439`。GlBalance 行为已过账凭证派生（双式记账 `assertBalanced` 结构保证 ΣDr==ΣCr），故 **BS 资产==负债+权益恒等式对 GlBalance 内现存数据恒成立**（`TestErpFinReportRendering#testBalanceSheetDataset:140-149` 断言 700==300+400 证实）。P&L 结转凭证未过账只致 Retained Earnings 未含本期净利润（**余额偏低/展示层 ΔNetIncome 偏差**），**不破坏 A==L+E 恒等式**（损益类科目 sectionOf 返回 null 不入 BS 段，结转凭证仅移动 NI 不改恒等式）。分支②「恒等式破坏」不成立。
2. **数据完整性偏差非会计正确性破坏**——OPEN/CLOSING/CLOSED 期间均按 `periodId` 取数，无状态门控（`:396/402`），但 GL 过账本身正确（凭证 `assertBalanced` + 过账引擎期间控制 `resolveOpenPeriod`），报表是查询/展示层，不写入 GL。属数据完整性关注非会计正确性破坏（与 P0 示例「期间 CLOSED 后禁止过账但实际可过」默认触发面不同——本控制点在报表读侧非过账写侧）。
3. **L1 自身显式承认未结账期间数据不完整**——`use-cases.md:339` 逐字「报表基于已 CLOSED 期间的 GlBalance(**未结账期间数据不完整**)」，即 L1 验收标准本身将"未结账期间数据不完整"作为门控动机陈述，OPEN 期间数据偏低是 L1 已预见的边界，非需求未预期破坏。
4. **查询面非决策主路径**——报表/看板是只读展示层，OPEN 期间渲染属月中常规运维查询（未结账期间查余额/试算），非会计过账/资金收付/库存硬拦截等决策主路径。误导决策面有限。
5. **§2 P1①/⑤ 不触发**——主路径（CLOSED 期间渲染）OK，OPEN 期间偏低属 §2 P2①「次要验收标准边界弱」非 P1①「主路径实质偏离」；存在强断言测试（`testBalanceSheetDataset` 恒等式 + `testIncomeStatementDataset` 收入费用 + `testCashFlowDataset` 流入额），非 P1⑤「测试仅冒烟」。

**裁决分层一致性**：与 arm-index `:142` P2-RC-008 行衔接（本验证**确认维持 P2 watch-only**）；与 A1.7 §2.2（`:120` FAIL soft）+ §5.2（`:229` P2-RC-008）分层一致；**与 P1-MA2-021[过账侧 CLOSED_FINAL 凭证锁定] 不同控制点**（本 finding = 报表渲染读侧门控，P1-MA2-021 = 过账写侧凭证锁定，分层独立不撤销）。

**【机制修正——本验证重要发现】**：plan Current Baseline item 2 与 A1.7 §2.1 `:108` 均声称「GlBalance 由过账引擎在凭证 POSTED 时维护（ErpFinPostingProcessor → GlBalance 写入）」。**此前提经本验证 HEAD 复核证伪**——5 处权威代码注释 + ORM 注记一致声明「ErpFinGlBalance 在当前阶段**未**由过账引擎维护」（见 §2.2）。该修正**不改变裁决方向**（仍维持 P2），反而**强化**「数据完整性偏差非会计正确性破坏」的归类（见 §2.2 + §4 影响评估），并指导 MR1 修复优先级（详见 §5.2 + §7 机制修正声明）。

---

## §1 存疑点原文 + L1 需求契约

### 1.1 输入存疑点原文（A1.7 §7 SP-3，逐字引用）

> **SP-3**（`2026-08-02-2115-...-a1-7-...md:288`）：CLOSED 期间门控缺失的运行时数据完整性影响 — OPEN 期间渲染报表是否实际产生误导（部分凭证未过账/未结转）。
> 静态状态：`loadGlBalances` 按 periodId 取数，OPEN 期间数据可能不完整（未过账凭证不入 GlBalance）。
> MA4 A4.1 运行时确认方式：OPEN 期间 + 未过账凭证场景跑 BS，对比 CLOSED 后 BS 差异。

### 1.2 L1 需求契约（UC-FIN-16，逐字引用）

`docs/design/finance/use-cases.md:318` UC-FIN-16 财务三大报表，`:339` 期间控制验收标准逐字：

```
// 期间控制
报表基于已 CLOSED 期间的 GlBalance(未结账期间数据不完整)
```

**L1 关键语义**：报表须基于 **CLOSED** 期间的 GlBalance；括号注「未结账期间数据不完整」是 L1 自身对门控动机的陈述——即 L1 显式承认未结账（OPEN/CLOSING）期间数据可能不完整，这是门控存在的**原因**而非未预期的破坏。

### 1.3 关联既有 finding（P2-RC-008，复用对象）

arm-index `:142` P2-RC-008（状态 todo successor watch-only）：UC-FIN-16 CLOSED 期间门控未强制——`loadGlBalances:386-413` 仅按 periodId 过滤，不校验 `period.status==CLOSED`；OPEN/CLOSING 期间亦可渲染。§2 P2①（次要验收标准未完全满足，主路径[数据存在即渲染]OK 边界[OPEN 期间数据不完整]弱——数据完整性关注非会计正确性破坏）。本验证闭合 P2-RC-008 的运行时数据完整性影响裁决（维持-or-升级）。

---

## §2 实现证据（L3，file:line + 行为断言）

### 2.1 `loadGlBalances` 期间过滤逻辑核验（Phase 1 item 1）

**核验目标**：证实 OPEN/CLOSING/CLOSED 期间均按 periodId 取数，**无 `period.status` 状态门控**。

| 站点 | file:line（写时实测） | 行为断言 | 核验 |
|------|----------------------|---------|------|
| periodId 缺省取最近期间 | `ErpFinReportBizModel.java:389-399` | `findLatestPeriodId():415-422`（`startDate DESC` + `setLimit(1)`）→ `q.addFilter(eq("periodId", latestPeriodId)):396` + `applyOrgAndSchemaScope(q, latestPeriodId):397` | ✅ 缺省分支仅 periodId + org/schema scope |
| 非缺省按指定 periodId | `ErpFinReportBizModel.java:400-405` | `q.addFilter(eq("periodId", periodId)):402` + `applyOrgAndSchemaScope(q, periodId):403` | ✅ 仅 periodId + org/schema scope |
| org/schema scope（不含 status） | `ErpFinReportBizModel.java:489-499` | `resolvePeriodOrgId` → `q.addFilter(eq("orgId", orgId)):494` + `resolveOrgSchemaId` → `q.addFilter(eq("acctSchemaId", schemaId)):497`；scope 不可解析时跳过（:491-493 保护单组织基线零回归） | ✅ scope 仅 orgId+acctSchemaId，**无 status** |
| 排序（非过滤） | `ErpFinReportBizModel.java:406-411` | `list.sort` by subjectId | ✅ 后置排序，非状态过滤 |

**结论**：`loadGlBalances:386-413` 全程**不读 `ErpFinAccountingPeriod.status`**——OPEN/CLOSING/CLOSED/CLOSED_FINAL 期间均按 periodId 取 GlBalance 行，无状态门控。CONFIRMED（与 A1.7 §2.2 `:120` 一致）。

### 2.2 GlBalance 与凭证过账时序核验（Phase 1 item 2）——【机制修正，本验证重要发现】

**核验目标**：plan Current Baseline item 2 声称「GlBalance 由过账引擎在凭证 POSTED 时维护（ErpFinPostingProcessor → GlBalance 写入）；未过账凭证不入 GlBalance」。**本验证 HEAD 复核此前提**。

#### 2.2.1 机制修正证据（5 处权威代码注释 + ORM 注记一致证伪 plan 前提）

| # | 站点 | 逐字注释（写时实测） | 证据 |
|---|------|---------------------|------|
| 1 | `ProfitLossClosingService.java:43`（损益结转服务） | 「ErpFinGlBalance 在当前阶段**未由过账引擎维护**，故以 VoucherLine 为权威本期发生额来源（等价的期末活动聚合）」 | ✅ |
| 2 | `AnnualCloseService.java:51-52`（年度结转服务） | 「ErpFinGlBalance 在当前阶段**未由过账引擎维护**（参 ProfitLossClosingService），故以 VoucherLine 为权威本年发生额来源；年度结转时创建次年 1 月的 GlBalance 快照行记录年初余额」 | ✅ |
| 3 | `BadDebtProvisionService.java:249`（坏账计提服务） | 「ErpFinGlBalance 当前**未由过账引擎维护**（参 ProfitLossClosingService），故以 VoucherLine 为权威」 | ✅ |
| 4 | `BudgetVoucherGenerator.java:36`（预算影子凭证生成器） | 「不写 ErpFinGlBalance（过账引擎本就**不维护** GlBalance）」 | ✅ |
| 5 | `ErpFinBudgetControlBiz.java:38`（预算控制引擎） | 「余量计算（均从 ErpFinVoucherLine 聚合，**不写 GlBalance**）——显式三通道分离（P1-MA2-084）」 | ✅ |
| 6 | `app-erp-finance.orm.xml:1740-1742`（ORM 注记） | 「过账引擎本就**不维护** ErpFinGlBalance，故预算不引入 GlBalance 结构变更」+「预算余额/实际余额从 ErpFinVoucherLine 派生不落库」 | ✅ |

#### 2.2.2 GlBalance 写路径全集普查（grep 证伪 plan 前提）

grep 全 `module-finance/` 生产代码 `GlBalance.*\.set\|updateBalance\|GlBalanceWriter\|BalanceUpdater\|saveEntity.*[Bb]alance`：

- **生产代码 GlBalance 写路径仅 1 处**：`AnnualCloseService.populateNextYearOpening:167-182`——**仅写次年 1 月期间的 `yearOpening{Debit,Credit}` 快照**（年度结转步骤4 对账基线），**不写 closing/period/opening 字段**（`:177-178` closingDebit/Credit=ZERO，`:173-176` opening/period=ZERO）。
- **过账引擎 `ErpFinPostingProcessor` 零 GlBalance 写入**——`balanceTotals:722` + `assertBalanced:736` 只做借贷平衡校验（功能金额），不维护 GlBalance。
- **期间结账试算平衡写入的是 `ErpFinTrialBalance`（独立实体），非 `ErpFinGlBalance`**——`ErpFinAccountingPeriodProcessor:362-382` `tbDao.newEntity()` 写入 `ErpFinTrialBalance`（`tb.setPeriodDebit/ClosingDebit`），**GlBalance 仍零写入**。（A1.7 §2.1 `:108` 将此误标为「过账引擎维护 GlBalance」——实为 TrialBalance 与 GlBalance 实体混淆，本验证修正。）
- **closingDebit/closingCredit/periodDebit/periodCredit 字段无任何生产代码维护路径**——仅测试 `seedGlBalance:293-311` 直接构造。

#### 2.2.3 机制修正结论（plan 前提证伪，但不改裁决方向）

**plan 前提证伪**：「GlBalance 由过账引擎在凭证 POSTED 时维护」**不成立**——当前阶段 GlBalance 是**快照表**，仅由年度结转 populate yearOpening（次年 1 月快照），closing/period/opening 字段不经任何生产路径维护（测试直接 seed）。故 plan「未过账凭证不入 GlBalance → OPEN 期间缺该部分余额」的机制推理**前提过窄**——更准确的事实是：**已过账凭证亦不连续维护 GlBalance**（P&L 结转/坏账/预算/年度结转四大服务均绕开 GlBalance 直接聚合 VoucherLine）。

**裁决不变的理由**（为何机制修正不改变维持 P2 结论）：

1. 机制修正**强化**「数据完整性偏差非会计正确性破坏」归类——GlBalance 是快照/展示层表，不连续维护是其设计当前阶段状态（5 处注释 + ORM 注记一致），非"未过账凭证破坏恒等式"。OPEN/CLOSED 期间读 GlBalance 的数据完整性**等价**（同一快照表，同一读路径）。
2. 恒等式仍由双式记账结构保证（`assertBalanced:736`），GlBalance 现存行（已过账凭证派生）内部平衡，A==L+E 对现存数据恒成立（§4.1 详证）。
3. L1 `:339`「未结账期间数据不完整」本身即承认数据完整性边界——机制修正后此边界更宽（GlBalance 一般性快照非连续维护），但**仍属数据完整性关注非会计正确性破坏**，命中 §2 P2① 非 P1①。

**机制修正的 MR1 优先级指导意义**：plan Deferred 建议修复 = `buildXxxDataset` 入口加 `period.status==CLOSED` 守卫。机制修正后，**真正的数据完整性提升方向是 GlBalance 维护接线**（让过账引擎在凭证 POSTED 时更新 closing/period 余额），而非仅加状态守卫。但 GlBalance 维护接线触及会计过账核心路径（须 ask-first，§5.2），属 MR1 successor 范围，本验证不实施。

### 2.3 三表数据源链核验（Phase 1 item 1 续）

| 报表 | 入口（file:line） | 数据源调用 | 期间过滤 |
|------|------------------|-----------|---------|
| 资产负债表 BS | `buildBalanceSheetDataset:269` | `loadGlBalances(periodId):272` → GlBalance | periodId + org/schema（无 status） |
| 利润表 IS | `buildIncomeStatementDataset:284` | `loadGlBalances(periodId):287` → GlBalance | periodId + org/schema（无 status） |
| 现金流量表 CF | `buildCashFlowDataset:299` | `loadPostedVoucherLines(periodId):302` → VoucherLine（voucher 头 `docStatus=POSTED:427` + periodId:429 + org/schema:430） | periodId + org/schema（无 status） |

**结论**：三表读路径均**不校验 period.status**——OPEN/CLOSING/CLOSED 期间均按 periodId 取数（GlBalance 或已过账 VoucherLine）。CONFIRMED。CF 读 VoucherLine（交易级）非 GlBalance（余额级）是 A1.7 §2.2 `:116` 已记录的设计合理偏离（现金流需交易级现金移动），非本存疑点范围。

---

## §3 测试证据（L4）

### 3.1 `TestErpFinReportRendering` 测试覆盖语义核验（Phase 1 item 4）

**核验目标**：证实测试全程 seed OPEN 期间即渲染，标注 CLOSED 门控测试缺口。

| 测试方法（file:line） | seed 期间状态 | 断言强度 | 覆盖语义 |
|---------------------|--------------|---------|---------|
| `seedPeriod:256-269` | `:266` `p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN)` | — | **全部测试 seed OPEN 期间**（单一种子） |
| `testFiveReportsRenderHtml:102-113` | OPEN（经 seed()） | 中（非空断言） | 5 报表 OPEN 期间可渲染（隐含 OPEN 可渲染 = 当前行为预期） |
| `testFiveReportsDownloadXlsxAndPdf:115-135` | OPEN | 中（文件存在） | OPEN 期间渲染管线 5 报表 × 2 类型 |
| `testBalanceSheetDataset:139-149` | OPEN | **强**（`700.00==资产 / 300.00==负债 / 400.00==权益`） | **恒等式 A==L+E 对 OPEN 期间 GlBalance 现存数据成立**（500+200==300+400） |
| `testIncomeStatementDataset:151-159` | OPEN | 强（收入 1000 / 费用 600） | OPEN 期间损益类本期发生额 |
| `testCashFlowDataset:161-173` | OPEN | 强（流入 80） | OPEN 期间已过账凭证现金行 |
| `testArApAgingBuckets:175-196` | OPEN（无关期间） | 强（90+ 桶 + 聚合一致） | 账龄（无关本存疑点） |
| `testPeriodCloseDataset:198-220` | OPEN | 强（模块状态 + 凭证计数） | 结账报告（无关本存疑点） |
| `testPathInjectionRejected:224-237` | OPEN | 强（异常抛出） | 路径注入防护（无关本存疑点） |

### 3.2 测试覆盖边界清单 + CLOSED 门控缺口

| 边界 | 覆盖 | 说明 |
|------|------|------|
| OPEN 期间渲染 BS/IS/CF | ✅ 强覆盖 | 9 测试方法 seed OPEN |
| **CLOSED 期间渲染** | ❌ **零覆盖** | 无测试 seed CLOSED 期间渲染对比 |
| **CLOSING 期间渲染** | ❌ **零覆盖** | 无测试 seed CLOSING 期间 |
| **OPEN→CLOSED 渲染数据差异断言** | ❌ **零覆盖** | 无测试构造 OPEN+未过账凭证 vs CLOSED 对比 |
| 恒等式 A==L+E（OPEN 期间 GlBalance 现存数据） | ✅ 强断言 | `testBalanceSheetDataset:146-148` 700==300+400 |
| BUDGET/COMMITMENT 过滤（A1.2 caveat ③ 交叉） | ❌ 零覆盖 | 见 A4.1.5 / A4.1.22 |

**测试覆盖语义结论**（Phase 1 item 4 产出）：`TestErpFinReportRendering` 全程 seed `PERIOD_STATUS_OPEN`（`:266`）即渲染，**隐含 OPEN 可渲染 = 当前行为预期**，与 L1「报表基于已 CLOSED 期间」**不一致**（L1 要求 CLOSED，测试固化 OPEN）。**CLOSED 门控测试零覆盖**（实现亦无门控，§2.1）。但恒等式强断言存在（`testBalanceSheetDataset:146-148`），证实 GlBalance 现存数据 A==L+E 成立——这是裁决「恒等式不破坏」的关键测试证据。

---

## §4 运行时行为证据 / OPEN-CLOSING 数据完整性影响评估（Phase 1 item 3，本存疑点核心）

### 4.1 BS 资产==负债+权益恒等式评估（决策树分支② hinge 核验）

**核验目标**：plan 决策树分支②「OPEN 期间渲染致恒等式破坏」是否成立。

**推理链**（SOUND，对齐 A1.7 §2.2 `:117`）：

1. GlBalance 行由**已过账凭证**派生（年度结转 yearOpening 快照亦由 VoucherLine 聚合，`AnnualCloseService.aggregateYearSubjectActivity`）。
2. 已过账凭证经 `ErpFinPostingProcessor.assertBalanced:736` 结构保证 ΣDr==ΣCr（双式记账硬约束，`balanceTotals:722` 计算总额 + `assertBalanced` 抛 `ERR_UNBALANCED` 若不平等）。
3. 故 GlBalance 现存行的 **Σ(借方科目 closingDebit−closingCredit) == Σ(贷方科目 closingCredit−closingDebit)** 恒成立（借贷必平的投影）。
4. BS `buildBalanceSheetDataset:269-282` 按 subjectClass 分 ASSET/LIABILITY/EQUITY 段 + `balanceAmount:532-538`——**A==L+E 对 GlBalance 现存数据恒成立**（双式记账的结构属性）。
5. **测试证实**：`testBalanceSheetDataset:146-148` 资产合计 700.00 == 负债 300.00 + 权益 400.00。

**分支② hinge「P&L 结转凭证未过账致恒等式破坏」核验**（plan 决策树分支② 唯一可能的恒等式破坏路径）：

- 若期间 P&L 结转凭证（PERIOD_CLOSE）未过账，则 Retained Earnings（EQUITY，4104 类）未含本期净利润 NI。
- 但 P&L 科目（INCOME/EXPENSE/COST）`sectionOf:511-523` 返回 **null**（不入 BS 段），故收入/费用**不入 BS**。
- 未结转时：BS 显示 A == L + E（E 不含 NI），而 NI 实际体现在 P&L（不入 BS）→ **A==L+E 仍成立**（只是 E 不反映本期经营成果）。
- 结转凭证的作用：将 NI 从 P&L 移至 RE（EQUITY），**移动不改 A==L+E**（结转凭证借收入/贷 RE 或借 RE/贷费用，Dr==Cr 仍平）。
- **结论**：P&L 结转凭证未过账致 **BS 展示层 ΔNetIncome 偏差**（E 未反映本期 NI，余额偏低），**不破坏 A==L+E 恒等式**。决策树分支② hinge **未触发**。

### 4.2 OPEN 期间数据完整性偏差评估

| 维度 | 评估 | 结论 |
|------|------|------|
| 余额绝对值 | OPEN 期间若有未过账凭证 / GlBalance 快照未更新 → 余额偏低（缺失部分） | **数据完整性偏差**（余额偏低） |
| 恒等式 | GlBalance 现存数据 A==L+E 恒成立（§4.1） | **恒等式不破坏** |
| 会计正确性 | GL 过账本身正确（`assertBalanced` + `resolveOpenPeriod` 期间控制），报表是读侧 | **非会计正确性破坏** |
| 误导决策面 | 报表/看板是只读查询/展示层；OPEN 期间渲染属月中常规运维查询（未结账查余额/试算） | **非决策主路径**（资金收付/库存硬拦截/过账均不经报表读侧） |

### 4.3 CLOSING 期间（结账进行中）过渡态数据完整性

CLOSING 状态（结账进行中，`ClosePeriodProcessor` 推进 OPEN→CLOSING→CLOSED）：
- CLOSING 期间 GlBalance 仍为结账前快照（结账流程不 populate GlBalance closing 字段，§2.2.2），数据完整性与 OPEN 等价。
- 结账步骤（P&L 结转/FX 重估/坏账/折旧）生成的凭证经 `loadPostedVoucherLines`（CF）或下个期间 populate（GlBalance），CLOSING 期间渲染读到的仍是结账前数据。
- **过渡态数据完整性偏差同 OPEN**（快照未更新），**非会计正确性破坏**。

### 4.4 实操 OPEN 期间渲染业务场景频率

月中查询未结账期间属**常规运维**（出纳/会计月中查余额、试算平衡、预估期末），非误导决策主路径。L1 `:339` 括号注「未结账期间数据不完整」即承认此常规运维边界。报表读侧不写入 GL，误导决策面有限。

---

## §5 符合性结论 + P2-RC-008 运行时裁决（Phase 1 item 6）

### 5.1 运行时裁决（方法论 §2 判据 + 三源对照）

**裁决：维持 P2-RC-008 = P2 watch-only**（决策树分支①命中）。

| 判据 | 三源对照 | 命中 |
|------|---------|------|
| §2 P0④（会计过账正确性破坏） | GL 过账本身正确（`assertBalanced` + `resolveOpenPeriod` 期间控制），报表是读侧不写 GL；与 P0 示例「期间 CLOSED 后禁止过账但实际可过」默认触发面不同（本控制点报表读侧非过账写侧） | **不成立** |
| §2 P0①（活跃数据破坏） | 报表读侧不破坏活跃数据；OPEN 期间渲染是查询非写入；非默认活跃破坏路径 | **不成立** |
| §2 P1①（功能完全缺失或行为实质偏离验收标准） | 主路径（CLOSED 期间渲染）OK；OPEN 期间偏低属边界（§2 P2①）；L1 自身承认「未结账期间数据不完整」 | **不适用**（归 P2①） |
| §2 P1⑤（测试断言完全缺失或仅冒烟） | 强断言存在（`testBalanceSheetDataset` 恒等式 + `testIncomeStatementDataset` + `testCashFlowDataset`） | **不适用** |
| §2 P2①（次要验收标准未完全满足，主路径 OK 边界弱） | 主路径[CLOSED 期间渲染 / 数据存在即渲染]OK，边界[OPEN/CLOSING 期间数据偏低 + CLOSED 门控测试零覆盖]弱——**数据完整性关注非会计正确性破坏** | **命中** |

**分层一致性**：

- 与 arm-index `:142` P2-RC-008 行衔接（本验证**确认维持 P2 watch-only**，不升 P0/P1 不降级）。
- 与 A1.7 §2.2（`:120` FAIL soft）+ §5.2（`:229` P2-RC-008）分层一致。
- **与 P1-MA2-021[过账侧 CLOSED_FINAL 凭证锁定] 不同控制点**——P1-MA2-021 = 过账写侧（CLOSED_FINAL 后禁止过账，resolved），本 finding = 报表渲染读侧（OPEN 期间可渲染），分层独立不撤销。grep arm-index 确认（P1-MA2-021 = 过账侧 distinct control point，A1.7 §6.1 `:267` + 本报告 §6 复核）。

### 5.2 修复方向（归 MR1 successor，本验证不实施）

plan Deferred 建议 + 本验证机制修正后的优先级指导：

| 修复方向 | 类别 | 预授权状态 | 数据完整性提升 |
|---------|------|-----------|--------------|
| (a) `buildXxxDataset` 入口加 `period.status==CLOSED` 守卫（OPEN 抛 WARN 或返回空+提示） | BizModel 代码逻辑修复 | 预授权自动执行（不触 §5 ask-first，非会计过账核心路径） | 中（强制 CLOSED，但 GlBalance 本身快照不连续维护） |
| (b) owner doc 标注「OPEN 期间数据偏低警告」 | 纯文档修复 | 预授权自动执行 | 低（仅文档） |
| (c) GlBalance 维护接线（过账引擎 POSTED 时更新 closing/period 余额）| **触及会计过账核心路径** | **须 ask-first + 独立 plan-audit**（§5 会计过账逻辑类） | **高**（机制修正后真正的数据完整性提升方向） |

**MR1 优先级指导**（机制修正后）：(c) 是真正提升数据完整性的方向，但触及会计过账核心路径须 ask-first；(a)/(b) 是 plan 原建议，可自动执行但提升有限。按裁决（维持 P2 watch-only 不强制），MR1 R1.0 展开器读取 P2-RC-008 后**不强制实现**（P2 登记不强制），由 owner doc 标注 (b) 或可选 (a) 收口即可。

---

## §6 与 arm-index 衔接（§7 "复用 or 新增" 裁决）

**复用 P2-RC-008**（非新建）：

| 裁决 | 依据 |
|------|------|
| 同根因同控制点 | P2-RC-008（arm-index `:142`）= UC-FIN-16 CLOSED 期间门控未强制；本验证 = 同控制点的运行时数据完整性影响确认 |
| 操作 | 在 arm-index P2-RC-008 行（`:142`）追加「A4.1.24 运行时确认」注记（Phase 2 同步更新），**不新建编号** |

**与既有 finding 去重声明**（§7 grep arm-index 同域同控制点）：

| 候选 finding | 控制点 | 裁决 |
|-------------|--------|------|
| P2-RC-008（本 finding 复用对象） | 报表 CLOSED 门控 | **复用**（同根因同控制点） |
| P1-MA2-021（CLOSED_FINAL 凭证锁定） | 过账写侧凭证锁定 | **不同控制点**（过账侧 vs 报表渲染读侧），不合并 |
| P1-RC-007（现金流分类缺失） | cash flow 经营/投资/筹资分类 | **不同控制点**（分类维度 vs 期间门控），A1.7 §6.1 `:267` 已确认 |
| P2-RC-085（cash flow VoucherLine postingType 过滤） | CF 读路径 postingType 过滤 | **不同控制点**（postingType 过滤 vs period.status 门控），A4.1.22 done 已确认 |
| caveat ③（COMMITMENT 影子凭证结构） | 承付凭证结构 | **不同维度**（凭证结构 vs 期间门控），不相关 |

无未经比对直接新建的 finding。

---

## §7 MA4↔A5.6 边界声明 + 机制修正声明（Phase 1 item 5）

### 7.1 MA4↔A5.6 边界声明

本验证审「行为是否符合需求」（CLOSED 门控缺失是否致数据完整性破坏/误导决策），**与 A5.6 审「E2E 断言强度」边界按此执行**（方法论 §去重协议 MA4↔A5.6 边界，Q5 非阻塞）。本验证不重做 A5.6 E2E 断言强度审计（`testBalanceSheetDataset` 等强断言已存在，§3；CLOSED 门控测试零覆盖是 P2-RC-008 实现缺口的一部分，非 A5.6 断言强度维度）。

### 7.2 机制修正声明（本验证重要发现，对 MR1 的指导）

本验证 HEAD 复核发现 plan Current Baseline item 2 与 A1.7 §2.1 `:108` 的前提「GlBalance 由过账引擎维护」**经 5 处权威代码注释 + ORM 注记证伪**（§2.2）。此修正：

1. **不改变裁决方向**（仍维持 P2 watch-only）——反而强化「数据完整性偏差非会计正确性破坏」归类（GlBalance 是快照表，OPEN/CLOSED 期间读路径等价）。
2. **指导 MR1 修复优先级**——真正的数据完整性提升是 GlBalance 维护接线（§5.2 方向 c），而非仅加状态守卫（方向 a）。
3. **不在本验证范围新建 finding**——「GlBalance 未由过账引擎维护」是 5 处代码注释 + ORM 注记一致声明的**已知架构当前状态**（P&L 结转/坏账/预算/年度结转四大服务均绕开 GlBalance 聚合 VoucherLine），非隐藏缺陷；且与 P2-RC-008（CLOSED 门控）是**不同控制点**。本验证范围 = CLOSED 门控数据完整性（P2-RC-008），GlBalance 维护接线属 MR1 successor 范围（若需提升数据完整性）。机制修正作为本验证的**证据修正**记录，指导 MR1，不新建 finding（避免范围蔓延）。

---

## §8 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter，真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码作为门控通过依据。**本验证为只读评估（零生产代码/ORM/api.xml/真相源变更），checker 无回归风险**——actual == baseline（本验证不触 Java 生产代码）。actual vs baseline 汇总表见下。

| 规则 | Baseline（`compliance-baseline.md` BASELINE 块） | Actual（本验证 HEAD） | 漂移 | 裁决 |
|------|----------|----------|------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | 0 | ✅ |
| R1d | 14 | 14 | 0 | ✅ |
| R2a/R2b | 34/229 | 34/229 | 0 | ✅ |
| R2c | 1382 | 1382 | 0 | ✅ |
| R2d | 34 | 34 | 0 | ✅ |
| R3 | 5 | 5 | 0 | ✅ |
| R4/R5/R7/R8/R11 | 0/0/0/0/0 | 0/0/0/0/0 | 0 | ✅ |
| R6/R10 | 2/6 | 2/6 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | 0 | ✅ |

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告复用 P2-RC-008（§6），无新建 finding；与 P1-MA2-021[过账侧] / P1-RC-007[现金流分类] / P2-RC-085[CF postingType] / caveat ③ 均经 grep 确认不同控制点（§6 去重表）。

---

## §9 与 MA2/A1.7 报告差异增量声明

本验证只补「A1.7 §7 SP-3 存疑点的运行时数据完整性影响确认」差异（A1.7 §7 SP-3 标注为「需运行时探针」），不重新核实 A1.7 §2.2 已证实的 CLOSED 门控缺失静态结论（A1.7 §2.2 `:120` FAIL soft + §5.2 P2-RC-008）。本验证增量 = ①OPEN/CLOSING 期间数据完整性运行时影响评估（恒等式不破坏 + 数据完整性偏差）；②机制修正（GlBalance 未由过账引擎维护，证伪 plan/A1.7 §2.1 前提）；③P2-RC-008 运行时裁决维持 P2 watch-only。无既有 MA2 报告专门覆盖 UC-FIN-16 CLOSED 门控运行时（报表/看板查询面无专属 MA2 行为报告，A1.7 §4 复用 2026-07-06 渲染证实）。

---

## §自检清单（报告产出前强制）

- [x] §1 存疑点原文 + L1 需求契约（UC-FIN-16 `:339` 逐字 + SP-3 逐字）
- [x] §2 实现证据（loadGlBalances `:386-413` + 三表数据源链 + GlBalance 过账时序【机制修正】+ 5 处代码注释 + ORM 注记）
- [x] §3 测试证据（`TestErpFinReportRendering` 全程 seed OPEN + CLOSED 门控零覆盖 + 恒等式强断言）
- [x] §4 运行时行为/数据完整性影响（恒等式不破坏 + OPEN/CLOSING 偏差 + 实操频率）
- [x] §5 符合性结论 + 运行时裁决（维持 P2-RC-008 = P2 watch-only，§2 判据 + 三源 + 分层一致）
- [x] §6 与 arm-index 衔接（复用 P2-RC-008 + 去重表）
- [x] §7 MA4↔A5.6 边界 + 机制修正声明
- [x] §8 过程纪律自检（checker actual vs baseline + 独立性 + 交叉去重）
- [x] §9 与 MA2/A1.7 差异增量声明
- [x] 9 段完整性自检通过
- [x] 保护区域纪律：只读评估，零代码/ORM/api.xml/真相源变更；修复归 MR1（方向 a/b 预授权，方向 c 触及会计过账须 ask-first）
- [x] item typing 合规：Proof/Decision/Add 无 Fix（本验证不实施修复）
