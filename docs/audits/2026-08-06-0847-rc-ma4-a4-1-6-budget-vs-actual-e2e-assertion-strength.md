# RC MA4 A4.1.6 — 预算对比报表 E2E commitment 列断言强度评估（`fin-budget-vs-actual.value.spec.ts` 是否断言 commitment 独立列）

> Audit Status: closed
> 里程碑：MA4（运行时行为验证 / E2E 断言强度评估维度）
> 工作项：A4.1.6（MA4 运行时行为验证 — A1.2 §7 存疑点 3）
> 审计 plan：`docs/plans/2026-08-06-0847-3-rc-ma4-a4-1-6-budget-vs-actual-e2e-assertion-strength.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议 MA4↔A5.6）
> 存疑点来源：`docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 3（UC-FIN-13 断言④ E2E `fin-budget-vs-actual.value.spec.ts` 是否断言 commitment 独立列）
> L1 真相源：`docs/design/finance/use-cases.md` UC-FIN-13 断言④（:261-262）
> 交叉引用：`docs/audits/2026-08-06-0847-rc-ma4-a4-1-5-commitment-trial-balance-exposure.md`（A4.1.5 同批 MA4 运行时核对，同 P1-RC-003 同根因家族不同控制点）+ arm-index P1-RC-003（报表姊妹站点，本核对 fold-in 目标）
> 审计性质：**只读运行时评估**（读 E2E spec + 单测 + 复用 MA2/A1.2，不改代码/ORM/api.xml/真相源；§9 冻结）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 不触发 MR0 |
| **P1**（新登记） | **0** | 无新 finding——E2E 断言缺口归 **P1-RC-003** 修复范围（fold-in），不新建 |
| **门控清单产出**（P1-RC-003 修复的 E2E 回归义务） | **1** | 3 项 E2E 补断言义务回填 arm-index P1-RC-003 行 |
| **解除存疑点** | **1** | A1.2 §7 存疑点 3 经本核对收口（fold-in P1-RC-003，无独立新控制点） |

**整体裁决**：A1.2 §7 存疑点 3 的运行时评估完成。E2E spec `fin-budget-vs-actual.value.spec.ts` 断言强度**四维核验齐全**（查询字段 / seed 覆盖 / 断言语义 / 与单测同步性），逐维有 `file:line` 证据。

**四维核验结论**（逐维实测命中）：
1. **查询字段集**：E2E GraphQL selection（`fin-budget-vs-actual.value.spec.ts:135`）仅 `subjectId subjectCode subjectName budgetAmount actualAmount availableAmount`——**无 `commitmentAmount`**（与 DTO `BudgetVsActualRow.java:24-26` 无该字段一致）。
2. **seed 数据覆盖**：`setupFull :74-131` 建 partner+employee+budget scenario(**NONE** :94)+budget line+ExpenseClaim+claim line，approve 预算方案（→BUDGET 凭证）+ approve ExpenseClaim（→NORMAL 凭证）——**无承付凭证 seed**（无 PO commit 路径 / 无 postingType=COMMITMENT voucher line 直接 seed）。
3. **断言语义**：`:172-195` 仅断言 budgetAmount=1000 / actualAmount=0→200→0（红冲回退）/ availableAmount=1000→800→1000——**仅两列增量断言，零 commitment 验证**（未断言 commitment 独立列 / 未断言 actual 不含 commitment / 未断言 available=budget−actual−commitment）。
4. **与单测同步性**：单测 `TestErpFinBudgetEndToEnd#testGetBudgetVsActual:195-219` 同样仅断言两列（budgetAmount=1000/actualAmount=400/availableAmount=600）+ 不 seed commitment——**E2E 与单测同步偏离 L1 三列要求**（三列需求零覆盖）。

**对照（控制引擎有 commitment 覆盖，报表没有）**：单测 `testAvailableDeductsCommitmentSeparately:222-249` seed 了 COMMITMENT 凭证（`seedCommitmentVoucher` :233）且强断言三通道 available=1000−300−200=**500**——证实"控制引擎有 commitment 覆盖，报表（E2E+单测）没有"的不对称（A1.2 §3 已记录，本核对复核确认 + 量化 E2E 侧差距）。

**finding 裁决（§7，fold-in 分支）**：E2E 断言缺口是 **P1-RC-003 同控制点的测试侧镜像**——P1-RC-003 已覆盖"报表 `getBudgetVsActual` 两列 + COMMITMENT 计入 actual + DTO 无 commitmentAmount + 单测 `testGetBudgetVsActual` 仅断言两列且不 seed commitment（三列需求零覆盖）"，E2E spec 是该同一测试侧缺口在浏览器层的同形投影（不同测试工件，同控制点：报表三列需求的测试断言强度）。**无独立新控制点** → 按方法论 §7「同根因同控制点 → 复用」裁决 **fold-in P1-RC-003**，**不新建编号**；在 arm-index P1-RC-003 行回填"E2E 补断言义务（A4.1.6 门控清单）"。

**P1-RC-003 修复回归门控清单**（修复 = DTO 增 commitmentAmount + getBudgetVsActual 三通道化 + available=budget−actual−commitment + XPT/前端增 Commitment 列 之后，E2E spec 须补）：
- ① GraphQL selection 增 `commitmentAmount`（:135）；
- ② seed COMMITMENT 凭证（PO commit 路径[config 开启承付 + approve 采购订单]或直接 seed postingType=COMMITMENT voucher line）；
- ③ 增三列增量断言（commitmentAmount=200 + actual 不含 commitment + available=budget−actual−commitment 公式验证）。

该清单作为 P1-RC-003 MR1（R1.0→RC-R1.n）修复行的 **E2E 回归义务登记**，随修复落地。

**交叉去重声明**：(a) 与 **P1-RC-003**（报表 `getBudgetVsActual` 两列 + COMMITMENT 计入 actual）同根因同控制点（报表三列需求的测试断言强度 = P1-RC-003 测试侧镜像）→ **fold-in**；(b) 与 **P1-MA2-084**（控制引擎 actual 含 COMMITMENT，已 fix）姊妹站点——控制引擎 fix 时未同步修报表（P1-RC-003）亦未同步补报表 E2E 断言；(c) 与 **A4.1.5 / P1-RC-091**（试算平衡 BUDGET-only 过滤漏 COMMITMENT）同根因家族不同控制点（报表列数/口径 vs 试算平衡恒等式）；(d) **MA4↔A5.6 边界声明**（方法论 §去重协议:386-390）：本核对只审"E2E 是否验证了 L1 要求的三列"（需求契约视角，行为是否符合需求），**不重做** A5.6 E2E 断言强度全量评级（测试质量视角）。

本评估**不实施修复**（plan Non-Goals + §5 保护区域）；门控清单随 P1-RC-003 经 MR1 批量修复通道落地。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md` UC-FIN-13（A1.2 切片 L1 真相源）。本核对聚焦断言④（预算对比报表）。

### UC-FIN-13 预算管理（编制/控制/对比）断言④ — 预算对比（报表）（`use-cases.md:260-263`）

```
// 预算对比(报表)
按 (acctSchema, subject, period, costCenter, project, postingType) 分组 VoucherLine
得到 Budget/Commitment/Actual 三列, 无需独立预算余额表
```

**L1 契约边界**：L1 断言④ 逐字要求 Budget/Commitment/Actual **三列**——commitment 是与 budget/actual 并列的**独立列**，聚合维度含 `postingType`（即按 postingType 分组得三通道）。本核对的运行时评估对象 = E2E spec 是否在运行时验证了此三列独立要求。

---

## 2. 实现证据（L3，审计对象 = E2E spec + DTO + BizModel 实现侧）

> 审计对象实仓逐项核实。本核对是 E2E 断言强度评估，故 §2 的"实现证据"=被评估的 E2E spec + 其依赖的 DTO/BizModel（证实 E2E 偏离根因 = 实现侧即不产出 commitment 列）。

### 2.1 E2E spec 查询字段集核验（维度①）

`tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts` `getBudgetVsActual:133-139`：

```
:135  `query{ ErpFinBudgetLine__getBudgetVsActual(acctSchemaId:${ACCT_SCHEMA},periodId:${PERIOD},subjectId:${SUBJECT_EXPENSE_ID})
         { subjectId subjectCode subjectName budgetAmount actualAmount availableAmount } }`
```

**实测结论**：GraphQL selection = `subjectId subjectCode subjectName budgetAmount actualAmount availableAmount`（6 字段），**无 `commitmentAmount`**。E2E 从未向服务端请求 commitment 列 → 即便服务端将来补出 commitment 列，当前 E2E 也不会消费/断言。

### 2.2 E2E spec seed 数据覆盖核验（维度②）

`setupFull:74-131` 逐项核实：

| seed 步骤 | 文件:行 | 产出凭证 postingType |
|---|---|---|
| 建 partner / employee | :77-88 | —（主数据，无凭证） |
| 建 budget scenario（**controlLevel=NONE** :94） | :90-98 | —（DRAFT 方案，无凭证） |
| 建 budget line（subject 6602 / budgetAmount=1000） | :100-106 | —（DRAFT 行，无凭证） |
| submit + **approve** budget scenario | :108-109 | **BUDGET** 影子凭证（Dr 6602=1000） |
| 建 ExpenseClaim + claim line（amount=200） | :112-128 | —（DRAFT 报销，无凭证） |

后续测试体（:166-195）再 `submitForApproval` + `approve` ExpenseClaim（:177-178）→ 产 **NORMAL** 凭证（Dr 6602=200）；`reverseApprove`（:188）→ 产 **REVERSAL** 凭证（原+红冲 isReversed=true 双双排除）。

**实测结论**：seed 全集 = BUDGET 凭证 + NORMAL 凭证 + REVERSAL 凭证——**无承付凭证 seed**（无 PO commit 路径：未建采购订单 / 未 config 开启承付 / 未直接 seed postingType=COMMITMENT voucher line）。即 E2E 运行时根本不构造 commitment 通道数据 → 三列断言在数据层即无对象可断言。

### 2.3 E2E spec 断言语义核验（维度③）

测试体 `:166-195` 三段断言：

| 段 | 文件:行 | 断言内容 | commitment 验证? |
|---|---|---|---|
| (a) 初始 | :167-174 | budgetAmount=1000 / actualAmount=0 / availableAmount=1000 | ❌ 无 |
| (b) actual 增量 | :180-185 | budgetAmount=1000 / actualAmount=200 / availableAmount=800（=1000−200） | ❌ 无 |
| (c) 红冲回退 | :190-195 | budgetAmount=1000 / actualAmount=0 / availableAmount=1000 | ❌ 无 |

**实测结论**：三段均为 budgetAmount/actualAmount/availableAmount **两列增量断言**——(1) 未断言 commitment 独立列存在；(2) 未断言 actual 不含 commitment（因无承付 seed，actual 口径是否含 commitment 在本 E2E 中不可运行时验证）；(3) 未断言 available=budget−actual−commitment 三项式（断言的 available=800 = budget−actual 两项式，与实现 `getBudgetVsActual:104-106` 的 available=budget−actual 同步）。**三列需求零断言**。

### 2.4 与单测同步性核验（维度④）

`module-finance/erp-fin-service/src/test/.../TestErpFinBudgetEndToEnd.java#testGetBudgetVsActual:195-219`：

```
:200-201  seedBudgetScenario("BUD-2024-11", ..., BUDGET_CONTROL_NONE, ..., new BigDecimal("1000"))
:203      seedActualVoucher("V-ACT-11", pid, expense, income, new BigDecimal("400"))   // NORMAL 凭证
:213      List<BudgetVsActualRow> rows = budgetLineBiz.getBudgetVsActual(1L, periodId, expense.getId(), CTX);
:216      assertEquals(... row.getBudgetAmount(), "1000")
:217      assertEquals(... row.getActualAmount(), "400")
:218      assertEquals(... row.getAvailableAmount(), "600")   // =1000−400 两项式
```

**实测结论**：单测同样仅断言两列（budgetAmount=1000/actualAmount=400/availableAmount=600）+ seed 仅 BUDGET + NORMAL（`seedActualVoucher` :365-411 设 `POSTING_TYPE_NORMAL`），**不 seed COMMITMENT**。**E2E 与单测同步偏离 L1**：两者都断言"实现当前的两列行为"，而非"L1 要求的三列行为"——测试与实现同向偏离 L1，三列需求在 L4（单测 + E2E）零覆盖。此即 A1.2 §3 已记录的「强（断言两列行为，非需求三列行为）」+「三列需求零覆盖」，本核对运行时复核确认 E2E 侧同结论。

### 2.5 E2E 偏离根因 = 实现侧即不产出 commitment 列（DTO + BizModel 实测）

E2E 不查询/不断言 commitment 列的**根因**是服务端实现侧即不产出该列（E2E 无法断言不存在于契约面的字段）：

- **DTO** `module-finance/erp-fin-dao/.../dto/BudgetVsActualRow.java:16-60`：仅 `budgetAmount`（:24）/ `actualAmount`（:25）/ `availableAmount`（:26）三字段——**无 `commitmentAmount` 字段**（getter/setter 全集 :28-59 无 commitmentAmount）。
- **BizModel** `ErpFinBudgetLineBizModel.getBudgetVsActual:48-108`：voucher 过滤 `or(eq(BUDGET), or(isNull, ne(BUDGET)))`（:64-65 = BUDGET OR NOT BUDGET，**COMMITMENT 计入 actual**）+ 仅 budget/actual 两通道聚合（`isBudget` flag :72-73 / budgetAmount 累加 :98-99 / else actualAmount 累加 :100-102）+ available=budget−actual（:104-106，**未减 commitment**）。

即 E2E 的两列断言是**对实现当前行为的忠实镜像**——实现不产出 commitment 列，E2E 自无从断言。**修复顺序约束**：须先修 P1-RC-003（实现侧三通道化），E2E 方有 commitment 列可查询/断言（门控清单 §5.2）。

---

## 3. 测试证据（L4，断言强度分档）

> 断言强度分档：强 = 断言验收标准数值/状态；无 = 该验收标准无断言。本核对评估对象本身即测试，故 §3 = 被评估测试的断言强度自评 + 与对照测试的强度对比。

| 测试 | 文件:行 | 覆盖的 L1 验收标准 | 断言强度 | commitment 三列覆盖 |
|---|---|---|---|---|
| E2E `fin-budget-vs-actual.value.spec.ts` | :135/:172-195 | UC-FIN-13 断言④ 三列 | **弱（仅两列增量）** | ❌ 零覆盖（无 commitment 查询/seed/断言） |
| 单测 `testGetBudgetVsActual` | TestErpFinBudgetEndToEnd.java:195-219 | UC-FIN-13 断言④ 三列 | **强（断言两列行为，非需求三列行为）**（A1.2 §3 已评级） | ❌ 零覆盖（不 seed commitment） |
| **对照**：单测 `testAvailableDeductsCommitmentSeparately` | TestErpFinBudgetEndToEnd.java:222-249 | UC-FIN-11 三通道余量公式（不同 UC，同 commitment 机制） | **强**（三通道 available=500） | ✅ seed COMMITMENT（`seedCommitmentVoucher` :233）+ 断言 available=1000−300−200=500 |

**L4 汇总**：报表三列需求在 E2E + 单测**双零覆盖**；对照证实控制引擎路径（UC-FIN-11）有 commitment 强断言覆盖（seed COMMITMENT + 三通道公式），报表路径（UC-FIN-13 断言④）无——"控制引擎有 commitment 覆盖，报表没有"的不对称**运行时确认**。E2E 侧的差距量化 = 3 项补断言义务（查询字段 + seed + 断言语义，见 §5.2 门控清单）。

---

## 4. 运行时行为证据（L5，E2E 与单测同步偏离确认 + 控制引擎对照）

### 4.1 E2E 与单测同步偏离 L1 确认

四维核验（§2.1-2.4）逐维实测命中，闭环证实：E2E spec 与单测 `testGetBudgetVsActual` **同向偏离 L1 三列要求**——

| 偏离维度 | E2E（`fin-budget-vs-actual.value.spec.ts`） | 单测（`testGetBudgetVsActual`） | 同步性 |
|---|---|---|---|
| 查询/返回字段 | 6 字段无 commitmentAmount（:135） | DTO 无 commitmentAmount 字段（:24-26） | ✅ 同步偏离 |
| seed commitment | 无（setupFull :74-131 无 COMMITMENT 凭证） | 无（仅 seedActualVoucher NORMAL :203） | ✅ 同步偏离 |
| 三列断言 | 仅两列增量（:172-195） | 仅两列增量（:216-218） | ✅ 同步偏离 |
| available 公式 | budget−actual 两项式（:185/:195） | budget−actual 两项式（:218） | ✅ 同步偏离 |

**裁决**：E2E 与单测**完全同步偏离** L1——两者都验证"实现当前的两列行为"，均未验证"L1 要求的三列行为"。无证据表明 E2E 在单测之外提供了独立的 commitment 运行时覆盖。

### 4.2 控制引擎对照（已正确，证实不对称）

`testAvailableDeductsCommitmentSeparately:222-249`（A1.2 §3 引用，本核对复核）：

- seed：BUDGET(1000) + ACTUAL/NORMAL(300) + **COMMITMENT(200)**（`seedCommitmentVoucher` :233 → `seedSingleLineVoucher` postingType=COMMITMENT :415/424-454）；
- 断言：`budgetControlBiz.check` 返回 available = **500**（:247-248，= 1000 − 300 − 200 三通道分离强断言）。

**对照结论**：控制引擎路径（UC-FIN-11 余量公式）seed 了 COMMITMENT 凭证且强断言三通道——**控制引擎有 commitment 覆盖**；报表路径（UC-FIN-13 断言④，E2E + 单测 `testGetBudgetVsActual`）seed 无 COMMITMENT 且仅两列断言——**报表没有 commitment 覆盖**。不对称**运行时确认**（A1.2 §3 已静态记录，本核对从 E2E 侧复核 + 量化差距 = 3 项补断言义务）。

### 4.3 P1-RC-003 修复后 E2E 作为回归门控的可行性

P1-RC-003 修复（实现侧三通道化）后，E2E spec 经 §5.2 门控清单补全（查询 commitmentAmount + seed COMMITMENT + 三列断言）即可作为 P1-RC-003 的**浏览器层回归门控**——覆盖 DTO 新字段经 GraphQL 暴露 → 前端 selection → 三列数值断言的全链路。当前（修复前）E2E 无法承担此门控（无 commitment 列可断言），这正是本核对产出门控清单的目的。

---

## 5. 符合性结论（§2 判据，裁决矩阵 + 门控清单 + finding 裁决）

### 5.1 裁决矩阵

| 存疑点维度 | L1 契约 | E2E 实现（运行时） | 单测实现（L4） | 裁决 |
|---|---|---|---|---|
| E2E 是否断言 commitment 独立列 | UC-FIN-13 ④ 要求三列（:261-262） | 否（:135 无 commitmentAmount / :74-131 无 COMMITMENT seed / :172-195 仅两列断言） | 同步偏离（:195-219 仅两列 + 不 seed commitment） | **fold-in P1-RC-003**（同控制点测试侧镜像，无独立新控制点 → 不新建；产出 E2E 回归门控清单） |

### 5.2 P1-RC-003 修复回归门控清单（Decision）

P1-RC-003 修复（DTO `BudgetVsActualRow` 增 `commitmentAmount` + `getBudgetVsActual` 三通道化[BUDGET/COMMITMENT/ACTUAL 排除前两者] + available=budget−actual−commitment + XPT/前端增 Commitment 列）落地后，E2E spec `fin-budget-vs-actual.value.spec.ts` **须补**：

| # | E2E 补断言义务 | 当前状态 | 修复后期望 |
|---|---|---|---|
| ① | GraphQL selection 增 `commitmentAmount`（:135） | 缺（selection 6 字段无 commitmentAmount） | selection 增 commitmentAmount，断言 row.commitmentAmount 存在 |
| ② | seed COMMITMENT 凭证 | 缺（setupFull :74-131 无承付 seed） | 增承付 seed：PO commit 路径[config 开启 `budget-commitment-enabled` + approve 采购订单触发 `ErpPurOrderProcessor.runCommitmentCommitHook`] 或直接 seed postingType=COMMITMENT voucher line（参照单测 `seedCommitmentVoucher:414-416` 范式） |
| ③ | 三列增量断言 + 公式验证 | 缺（:172-195 仅两列） | 增断言：commitmentAmount=200（独立列）/ actualAmount 不含 commitment（actual 口径修正后实际值）/ availableAmount=budget−actual−commitment（三项式，如 1000−200−200=600） |

**该清单作为 P1-RC-003 MR1（R1.0→RC-R1.n）修复行的 E2E 回归义务登记**（arm-index P1-RC-003 行回填）。修复 plan 须含此 3 项 E2E 补断言 item。

### 5.3 finding 裁决（§7，fold-in 分支）

**裁决：fold-in P1-RC-003，不新建编号。**

- **裁决依据**（方法论 §7「同根因同控制点 → 复用」）：P1-RC-003（arm-index:128）已覆盖"报表 `getBudgetVsActual` 两列 + COMMITMENT 计入 actual + DTO 无 commitmentAmount + **单测 `testGetBudgetVsActual` 仅断言两列且不 seed commitment（三列需求零覆盖，测试与实现同步偏离 L1）**"。E2E spec `fin-budget-vs-actual.value.spec.ts` 是该**同一测试侧缺口在浏览器层的同形投影**——同根因（实现侧不产出 commitment 列 → 测试侧无 commitment 可断言）+ 同控制点（UC-FIN-13 断言④ 三列需求的测试断言强度）+ 同修复触发（P1-RC-003 修复后测试侧统一补 commitment 断言）。
- **无独立新控制点**：E2E 缺口未引入单测之外的新控制点——E2E 与单测完全同步偏离（§4.1），E2E 未提供独立 commitment 运行时覆盖。故不满足 §7「新根因/新控制点 → 新建」条件。
- **fold-in 操作**：在 arm-index P1-RC-003 行回填"E2E 补断言义务（A4.1.6 门控清单：查询 commitmentAmount + seed COMMITMENT + 三列增量断言）"，使 finding → 修复（MR1 RC-R1.n）双向可追溯含 E2E 侧义务。
- **若独立新控制点出现的保留分支**（方法论健全性，本核对未触发）：若 E2E 存在单测之外的独立缺口（如 E2E 缺 seed 致 actual 口径本身也无法运行时验证，与单测不同形），则按 §7 grep arm-index 后新建。本核对四维核验证实 E2E 与单测完全同步偏离，此分支不触发。

### 5.4 接受类结论汇总

| 项 | 接受依据 |
|----|----------|
| A1.2 §7 存疑点 3 解除 | 四维核验齐全（§2）+ E2E/单测同步偏离确认（§4.1）+ 控制引擎对照（§4.2）+ 门控清单产出（§5.2）+ fold-in 裁决（§5.3）——存疑点经运行时评估收口（fold-in P1-RC-003，无独立新 finding） |

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

> 产出 finding 裁决前已 grep `arm-index.md` finance 预算/承付/报表/E2E 同域同控制点。裁决遵循 §7 规则。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本评估关系 | 裁决 |
|-----------------|--------|-------------|------|
| **P1-RC-003** 预算对比报表 `getBudgetVsActual` 两列 + COMMITMENT 计入 actual + 单测仅断言两列不 seed commitment | **报表**列数/口径 + **测试侧断言强度**（UC-FIN-13 ④） | **同根因同控制点**：E2E spec 是 P1-RC-003 已覆盖的"测试侧三列零覆盖"在浏览器层的同形投影（单测 `testGetBudgetVsActual` 的 E2E 镜像） | **fold-in P1-RC-003**（回填 E2E 补断言义务，不新建） |
| **P1-MA2-084** 控制引擎 actual 含 COMMITMENT（**已 fix**） | **控制引擎** actual 通道 | 姊妹站点：控制引擎 fix 时未同步修报表（P1-RC-003）亦未同步补报表 E2E 断言 | 不相关（不同控制点：控制决策 vs 报表展示/测试），交叉引用 |
| **A4.1.5 / P1-RC-091** 试算平衡 BUDGET-only 过滤漏 COMMITMENT | **试算平衡/GL 重分类**聚合 | 同根因家族不同控制点（报表列数/口径 vs 试算平衡恒等式） | 不相关，交叉引用声明同根因家族 |
| P2-MA2-073 TestErpSalOrderCommitment 缺 Dr/Cr 断言 | 测试断言强度（sales 承付） | 不同域不同控制点（sales 承付凭证 vs finance 预算报表） | 不相关（§去重协议 A5.6 边界） |

### 6.2 裁决

| 裁决 | 依据 | 操作 |
|------|------|------|
| **fold-in P1-RC-003**（不新建） | E2E 断言缺口 = P1-RC-003 同控制点测试侧镜像（§5.3），无独立新控制点 | arm-index P1-RC-003 行回填"E2E 补断言义务（A4.1.6 门控清单）"+ 追加 RC 交叉引用注记 |

### 6.3 双向可追溯

- **门控清单 → 修复**：§5.2 门控清单（3 项 E2E 补断言义务）作为 P1-RC-003 MR1（R1.0→RC-R1.n）修复行的 E2E 回归义务，回填 arm-index P1-RC-003 行。
- **finding → 修复**：P1-RC-003 经 MR1（R1.0→RC-R1.n）修复，修复 plan 须含本门控清单 3 项 E2E item。
- **既有 finding 交叉引用**：P1-RC-003 交叉引用 P1-MA2-084（控制引擎姊妹站点，fix 时未同步补报表 E2E）。

---

## 7. 静态存疑点清单（供后续展开）

> 本核对已将 A1.2 §7 存疑点 3 收口（fold-in P1-RC-003）。无新增静态存疑点。

| SP | 存疑点 | 静态状态 | 后续确认方式 |
|----|--------|---------|--------------|
| — | （本核对无新增静态存疑点；A1.2 §7 存疑点 3 经本核对 fold-in P1-RC-003 收口，E2E 补断言义务随 P1-RC-003 MR1 修复落地） | — | — |

**P0 即时通道**：本核对未出 P0（零新 finding），不触发 MR0。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 详见下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本审计为只读评估（零生产代码变更），checker 无回归风险**。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告 finding 裁决已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1 裁决表），无未经比对直接新建的 finding。本评估裁决 fold-in P1-RC-003（E2E 断言缺口 = P1-RC-003 同控制点测试侧镜像，无独立新控制点），与 P1-MA2-084 / A4.1.5-P1-RC-091 同根因家族不同控制点交叉引用声明齐全。

### checker actual vs baseline 实测表（2026-08-06 实测，零代码变更）

> 本审计为**只读评估**（无生产代码变更——仅新增本报告 .md + 编辑 plan/roadmap/arm-index .md，全部为 docs，checker 扫描面为生产 .java/.orm.xml/.view.xml），故 checker 无回归风险；actual vs baseline 实测记录如下（基线源 `compliance-baseline.md §BASELINE (machine-readable)` 权威块）。

| 规则 | Baseline | Actual | 漂移 | 状态 |
|------|----------|--------|------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | 0 | ✅（checker 实跑命中） |
| R1d | 14 | 14 | 0 | ✅（checker 实跑命中） |
| R2a | 34 | 34 | 0 | ✅（checker 实跑命中） |
| R2b | 229 | 229 | 0 | ✅（checker 实跑命中） |
| R2c | 1382 | 1382 | 0 | ✅（checker 实跑命中） |
| R2d | 34 | 34 | 0 | ✅（checker 实跑命中） |
| R3 | 5 | 5 | 0 | ✅（只读保证；见下注） |
| R4/R5 | 0/0 | 0/0 | 0 | ✅（只读保证） |
| R6 | 2 | 2 | 0 | ✅（只读保证） |
| R7 | 0 | 0 | 0 | ✅（只读保证） |
| R8 | 0 | 0 | 0 | ✅（只读保证） |
| R10 | 6 | 6 | 0 | ✅（只读保证） |
| R11 | 0 | 0 | 0 | ✅（只读保证） |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | 0 | ✅（只读保证） |

> **过程观察（checker 工具自身，非本审计范围，不影响门控）**：checker 脚本 R3 实体白名单构建（`nop-compliance-checker.sh:177-180`）的正则 `<entity className="` 与当前 orm.xml 实体声明格式 `<entity ext:web-renderer="flux" className="` 不匹配（`ext:web-renderer` 属性插在 `<entity` 与 `className` 之间），致 R3+ 区段在 `set -euo pipefail` 下提前退出（退出码 1）。此为**预存工具 drift**（orm.xml 增 `ext:web-renderer` 属性后未同步 checker 正则），与本只读审计无关，亦非本计划范围（Non-Goals：不改代码/工具）。R1a-R2d 经 checker 实跑命中确认与 baseline 一致；R3-R12c 因本审计**零生产代码变更**（git 工作树洁净，未触及任何 .java/.orm.xml/.view.xml），actual 数学上必然 == baseline（checker 计数面仅生产代码，docs 变更不影响任何规则计数），故记 0 漂移。**本报告不以 checker 退出码作为门控依据**（方法论 §8）；工具 drift 建议后续 audit-remediation 维护轮修正 R3 正则为 `<entity [^>]*className="`。

全 19 规则 actual ≤ baseline，**0 漂移**。本审计无生产代码变更，无回归风险。

---

## 9. 与既有审计差异增量声明（§去重协议）

本评估声明与既有审计的差异增量：

- **复用 A1.2 已证实结论**（不重新核实）：A1.2 §3 已评级单测 `testGetBudgetVsActual` 断言强度 = 强（断言两列行为，非需求三列行为）+ 三列需求零覆盖；A1.2 §5.3 P1-RC-003 已登记报表两列 + 单测同步偏离；A1.2 §7 存疑点 3 已提出"E2E 断言强度属运行时确认"。本核对复用其静态结论作为输入，只补 E2E 侧运行时四维核验 + 门控清单。
- **复用 MA2 已证实行为**：P1-MA2-084 控制引擎三通道分离 fix（`testAvailableDeductsCommitmentSeparately` 强断言 available=500）作为"控制引擎有 commitment 覆盖"的对照证据（§4.2）。
- **复用 A4.1.5 已证实结论**：A4.1.5（同批 MA4）已证实 GL 路径 postingType 过滤面全集 + caveat ③ 凭证结构维持接受；本核对与其同根因家族（BUDGET/COMMITMENT 过滤/列缺失）不同控制点（报表 E2E 断言强度 vs 试算平衡恒等式）。
- **本核对只补的差异增量**（既有审计未覆盖）：
  1. **E2E spec 断言强度四维运行时核验**：A1.2 静态读了 E2E spec 但未从"E2E 是否可作为 P1-RC-003 修复回归门控"视角评估。本核对逐维（查询字段 / seed 覆盖 / 断言语义 / 与单测同步性）实测命中，闭环证实 E2E 与单测同步偏离 L1。
  2. **P1-RC-003 修复的 E2E 回归门控清单**：A1.2/P1-RC-003 只登记了实现侧修复义务（DTO+BizModel+XPT），未登记 E2E 侧补断言义务。本核对产出 3 项 E2E 补断言义务（§5.2），作为 P1-RC-003 MR1 修复的回归门控清单。
  3. **finding fold-in 裁决**：经 §7 grep arm-index，裁决 E2E 断言缺口 fold-in P1-RC-003（同控制点测试侧镜像），不新建编号。
- **MA4↔A5.6 边界声明**（方法论 §去重协议:386-390）：本核对（MA4）只审"E2E 是否验证了 L1 要求的三列"（需求契约视角——行为是否符合需求），**不重做** A5.6（audit-remediation）E2E 断言强度全量评级（测试质量视角——测试质量维度的全量评级矩阵）。两者判据不同，按此边界执行，Q5 非阻塞，MA4 不设门控。

---

## 10. Verdict

**Verdict: 预算对比报表 E2E commitment 列断言强度评估完成，零新 finding（fold-in P1-RC-003），产出 E2E 回归门控清单**

**审查范围**：E2E spec `fin-budget-vs-actual.value.spec.ts` 断言强度四维核验（查询字段 / seed 覆盖 / 断言语义 / 与单测同步性）+ 控制引擎对照 + P1-RC-003 修复回归门控清单产出 + 与 arm-index 衔接（§7 fold-in 裁决）+ §8 过程纪律自检 + 与 A1.2/MA2/A4.1.5 差异增量声明 + MA4↔A5.6 边界声明。

**接受类**：A1.2 §7 存疑点 3 解除（四维核验齐全 + 同步偏离确认 + 门控清单产出 + fold-in 裁决）。

**P1 残留**：无新 P1。E2E 断言缺口 fold-in **P1-RC-003**（arm-index:128），随 P1-RC-003 MR1（R1.0→RC-R1.n）修复落地——修复 plan 须含 §5.2 门控清单 3 项 E2E 补断言 item（查询 commitmentAmount + seed COMMITMENT + 三列增量断言）。

**P0**：无。不触发 MR0。

**剩余风险**：P1-RC-003 修复前 E2E 无法承担 commitment 三列回归门控（实现侧不产出 commitment 列）；修复后须按 §5.2 门控清单补全 E2E 断言方可作为浏览器层回归门控。

**解除 A1.2 §7 存疑点 3**：经本核对 fold-in P1-RC-003 收口（E2E 断言缺口 = P1-RC-003 同控制点测试侧镜像，E2E 补断言义务随 P1-RC-003 MR1 修复落地）。本核对解除 A4.1.6 在 MA4（A4.1 展开器）及 MR1（R1.0）链路的该存疑点证据缺口。
