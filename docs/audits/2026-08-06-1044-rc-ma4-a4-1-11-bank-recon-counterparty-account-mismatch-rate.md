# RC MA4 A4.1.11 — UC-FIN-09/14 对方账号缺失致错误 MATCHED 触发率与影响面 验证报告

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 运行时行为验证）
> 工作项：A4.1.11（MA4 运行时行为验证 — A1.4 §7-1：UC-FIN-09/14 断言② 对方账号缺失致错误 MATCHED 的实际触发率，关联 P1-RC-004）
> 验证 plan：`docs/plans/2026-08-06-1044-2-rc-ma4-a4-1-11-bank-recon-counterparty-account-mismatch-rate.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据[含 P0④活跃数据破坏 / P0①] / §7 arm-index 衔接 / §8 过程纪律自检 / §10 MR0 即时通道 / §去重协议）
> 输入存疑点：A1.4 §7 存疑点 1（`docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` §7）
> 输入 finding：`P1-RC-004`（A1.4 §5.2 / §6，UC-FIN-09/14 断言② 自动勾对"对方账号模糊匹配"维度缺失）
> 关联 finding：`P2-RC-002`（valueDate→transactionDate 简化，watch-only）/ `P2-RC-001`（跨多条 statement 去重范围，watch-only）/ MA2 银行对账解耦既有行为（`2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`）
> 验证性质：**只读运行时影响面评估**（读匹配算法 + 读候选过滤 + 引用 MA2/A1.4 + 触发率影响面推理；不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 验证日期：2026-08-06
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **P1-RC-004 P0 升级再评估** | **维持 P1（不升 P0，不降 P2）** | 不触发 MR0 即时通道 |
| 候选过滤维度 | `subjectId + dcDirection(opposite) + amount + voucherId(日期窗口)`，**确无对方账号维度** | 与 A1.4 §5.2 一致（CONFIRMED） |
| 匹配决策（单候选） | `candidates.size()==1 → MATCHED`（不区分对方账号）→ 同额同日不同 partner 且账面仅 1 候选时**错误 MATCHED 确认** | 触发面确认存在 |
| 匹配决策（多候选） | `≥2 → SUSPENSE`（过度挂起，非错误 MATCHED） | 跨 partner 同额 2+ 候选转 SUSPENSE，无错误 MATCHED |
| 触发率定性评估 | **中低**（需「同额 + 同日 ±N 天窗口 + 同资金科目 + 反向方向 + 非对称候选[实际对方无候选/被占用/出窗]」多重前置；非每次对账默认触发；公共金额场景真实存在） | 非默认活跃路径 |
| **兜底复核（精化 A1.4 §5）** | **两处弱于 A1.4 §5 假设**：①`manualMatch:30-34` 守卫**拒绝**已 MATCHED 行（A1.4「可经 manualMatch 取消重勾」不成立——无领域 unmatch 操作）；②余额恒等式对**同额**错误 MATCHED **无效**（双侧等额抵消，恒等式仍成立，不触发不平衡拒绝） | 强化 P1 不可降 P2 |
| 影响面边界 | 错误仅限对账勾对链路（`matchedLineId`），**不影响 GL/凭证过账正确性**（对账为独立子系统，MA2 证实解耦） | P0④ 不成立 |
| 新 finding | **0** | 无新控制点（全部归既有 P1-RC-004） |
| MR0 触发 | **无** | — |

**整体裁决**：A1.4 §7 存疑点 1「对方账号缺失致错误 MATCHED 的实际触发率」**经运行时影响面评估 CONFIRMED**：`BankLedgerQuery.findCandidates:39-84` 候选过滤确无对方账号维度 + `BankStatementMatcher.autoMatch:61-65` 单候选即自动 MATCHED → 当「同额同日不同 partner 且账面仅 1 候选」时产生**错误 MATCHED**（银行行 A 勾对到凭证行 B 的 matchedLineId）。但经 §2 P0 判据三源复核，**维持 P1-RC-004 = P1**：①P0④「会计过账正确性破坏」**不成立**——错误仅限对账勾对链路（matchedLineId 写错），不影响凭证过账/GL 余额正确性（对账子系统与过账解耦，MA2 :48,223,365 证实；凭证本身过账正确）；②P0①「活跃数据破坏」**不成立**——触发需「同额+同日窗口+同资金科目+反向方向+非对称候选」多重前置条件，非默认活跃路径（与 P0 示例「凭证重复过账」每次正常操作即触发不同），且技术可逆。**触发率定性为中低**（公共金额场景如薪资/租金/贷款月供真实存在，但需多重前置巧合）。**两处兜底精化**：本验证发现 A1.4 §5 对可逆性/下游兜底的假设偏强——`manualMatch:30-34` 守卫拒绝已 MATCHED 行（无领域 unmatch 操作，修正需通用 CRUD 重置 matchStatus），且余额恒等式对同额错误 MATCHED 无效（双侧等额抵消不触发不平衡）。这两处精化**强化 P1-RC-004 不可降 P2 的理由**（兜底弱于假设，错误 MATCHED 可能静默持续），但**不升 P0**（核心 P0 判据——GL/过账正确性破坏 + 默认活跃路径破坏——均不成立）。**不触发 MR0，不升 P0，不降 P2，无新 finding。** P1-RC-004 修复仍归 MR1（R1.0→RC-R1.n），触及 ORM 结构变更须 ask-first（§5）。

---

## 1. 需求契约与锚点原文（L1 + L3）

### L1 权威（UC-FIN-09/14 断言②，`docs/design/finance/use-cases.md`）

```
UC-FIN-09 (use-cases.md:172):
  自动勾对: 金额 + 反向方向 + valueDate±N天 + 对方账号 模糊匹配   ← 断言②

UC-FIN-14 (use-cases.md:279):
  按 (金额, 反向方向, valueDate±N天, 对方账号) 模糊匹配
  命中唯一 → MATCHED; 多候选 → UNMATCHED; 金额对户名差 → SUSPENSE   ← 断言②
```

- **本验证对象** = 断言②「对方账号」维度的**缺失触发面**：当对方账号维度缺失时，自动勾对在何种数据分布下产生**错误 MATCHED**（银行行勾对到错误对方账号的凭证行），以及该错误的影响面与可逆性。
- 断言②的 valueDate 维度缺失 = `P2-RC-002`（watch-only，不同控制点）；dedup 三元组 = `P2-RC-001`（watch-only）。均与本验证**不同控制点**（§去重）。

### L2 owner doc 契约（`bank-reconciliation.md §业务规则 2`：98）

```
2. 自动勾对算法:按 (amountSource, direction 反向, valueDate ± N 天, counterpartyAccount) 模糊匹配,
   命中唯一记录则 MATCHED;多条候选则 UNMATCHED 等待人工;金额一致但对方户名差一的标 SUSPENSE 待核。
```

- **L2 与 L1 一致**，均要求对方账号（counterpartyAccount）为 4 维度之一。L3 静默缺失（未文档化，无 §4 人工批准——A1.4 §5.2 已确认）。

### L3 实仓锚点（`module-finance/erp-fin-service/.../bankrecon/`，写时实测）

| 锚点 | 文件:行 | 实现 | 说明 |
|---|---|---|---|
| 候选过滤入口 | `BankLedgerQuery.findCandidates:39-84` | `subjectId(:64) + dcDirection(:65) + voucherId-in-window(:66) + debitAmount/creditAmount(:67-71)` | **本验证核心**——**无 counterpartyAccount 过滤** |
| 日期窗口 | `findCandidates:45-46` + `resolveDaysWindow:125-132` | `[txnDate − N, txnDate + N]`，N=`CONFIG_BANK_MATCH_TOLERANCE_DAYS` 默认 3 | 窗口基于 txnDate（transactionDate），非 valueDate（P2-RC-002） |
| 已占用排除 | `findOccupiedLineIds:105-123` | 按 `statement.fundAccountId` 反查 MATCHED/MANUAL_MATCHED 行的 matchedLineId（:116-120） | 减少重复勾对，不解决跨 partner 同额 |
| 匹配决策入口 | `BankStatementMatcher.autoMatch:41-74` | `size==1→MATCHED(:61-65)` / `empty→UNMATCHED(:66-67)` / `≥2→SUSPENSE(:68-71)` | **单候选即自动 MATCHED，不区分对方账号** |
| manualMatch 守卫 | `ErpFinBankStatementLineManualMatchProcessor.manualMatch:30-34` | 仅允许 UNMATCHED/SUSPENSE 行手工勾对；**MATCHED 行抛 `ERR_BANK_STMT_LINE_ALREADY_MATCHED`** | 可逆性路径关键（见 §4） |
| 余额恒等式守卫 | `BankReconciliationBuilder.generate:67-82` | `diff=(statementBalance−bookBalance)−(bankCreditUnrecorded−bankDebitUnrecorded)`，diff≠0 抛 `ERR_BANK_RECON_NOT_BALANCED`(:77-82) | 下游兜底（见 §4） |
| ORM 对方账号列 | `app-erp-finance.orm.xml`（`ErpFinBankStatementLine:1133+` / `ErpFinVoucherLine`） | `rg "counterparty\|counterpartyAccount" module-finance/model/app-erp-finance.orm.xml` **零命中** | **ORM 无 counterpartyAccount/counterpartyName 列**（与 A1.4 §5.2 一致） |

> **关键观察**：候选过滤 4 个维度中 3 个已实现（金额/反向方向/日期窗口），唯独「对方账号」维度在候选过滤 + ORM 双层静默缺失。本验证回答：**该缺失在何种数据分布下产生错误 MATCHED，影响面多大，是否够格升 P0？**

---

## 2. 候选过滤逻辑核验（Phase 1 `Proof` ①）

### 2.1 过滤条件全集（`BankLedgerQuery.findCandidates:39-84`）

`findCandidates(fundAccount, amount, oppositeDirection, txnDate, daysWindow)` 的候选过滤逻辑（写时实测行号）：

| 步骤 | 行 | 过滤条件 | 对方账号? |
|---|---|---|---|
| null 守卫 | :41-44 | 任一入参 null → 返回空 | — |
| 日期窗口 | :45-46 | `[txnDate − N, txnDate + N]` | — |
| 凭证窗口查询 | :50 → `findVoucherIdsInWindow:87-102` | `docStatus=POSTED` + `voucherDate∈[from,to]` + 排除 `isReversed=true`(:96-98) | — |
| **候选过滤** | :63-72 | `subjectId(:64)` + `dcDirection==oppositeDirection(:65)` + `voucherId∈chunk(:66)` + `debitAmount==amount 或 creditAmount==amount(:67-71)` | **❌ 无** |
| 已占用排除 | :76-83 → `findOccupiedLineIds:105-123` | 排除已被其他银行行 MATCHED/MANUAL_MATCHED 占用的凭证行 ID | — |

**结论**：候选过滤 = `subjectId + dcDirection(反向) + amount + voucherId(日期窗口)`，**确无对方账号过滤**（与 A1.4 §5.2 静态确认一致，本验证运行时复核 CONFIRMED）。ORM 双层（`ErpFinBankStatementLine` 银行行 + `ErpFinVoucherLine` 凭证行）均无 `counterpartyAccount` 列承载——`rg "counterparty" module-finance/model/app-erp-finance.orm.xml` **零命中**。

### 2.2 已占用排除逻辑（`findOccupiedLineIds:105-123`）

- 按 `statement.fundAccountId(:109)` 反查同资金账户所有银行行 → 取 `matchStatus ∈ {MATCHED, MANUAL_MATCHED}` 且 `matchedLineId != null` 的行（:113-120）→ 收集其 `matchedLineId` 集合。
- `findCandidates:76-83` 从候选结果中排除该集合。

**作用边界**：已占用排除**减少重复勾对**（同一凭证行不被多银行行重复 MATCHED），但**不解决跨 partner 同额问题**——它只排除「已被勾对」的凭证行，不排除「不同对方账号但同额同日」的凭证行。因此当一笔银行行 A 的实际对方（partner A）的凭证行不在候选集（未过账/出窗口/已被占用），而 partner B 的同额凭证行是唯一未占用候选时，A 的银行行会错误 MATCHED 到 B 的凭证行。

**证据汇总（file:line）**：
- `BankLedgerQuery.findCandidates:39-84`（候选过滤，无对方账号 :63-72）
- `BankLedgerQuery.findOccupiedLineIds:104-123`（已占用排除，:113-120）
- `BankLedgerQuery.findVoucherIdsInWindow:87-102`（凭证窗口 POSTED + 未红冲）
- `app-erp-finance.orm.xml`（`ErpFinBankStatementLine:1133+` / `ErpFinVoucherLine`，无 counterparty 列——grep 零命中）

---

## 3. 匹配决策核验（Phase 1 `Proof` ②）

### 3.1 决策逻辑（`BankStatementMatcher.autoMatch:41-74`）

```
for each UNMATCHED bank line (:51-52):
    oppositeDirection = 反向(line.dcDirection) (:53)        // DEBIT→CREDIT / CREDIT→DEBIT
    candidates = findCandidates(account, amount, opposite, txnDate, daysWindow) (:58-59)
    if candidates.size() == 1:  → MATCHED  + 回写 matchedLineId (:61-65)   ← 单候选即自动 MATCHED
    elif candidates.isEmpty():  → UNMATCHED (:66-67)
    else (≥2):                  → SUSPENSE (:68-71)                          ← 多候选转挂起
```

**决策行为结论**：
- **单候选（size==1）→ 自动 MATCHED**，且回写 `matchedLineId = candidates.get(0).getId()`（:62-64）。**决策不检查对方账号**——只要金额+反向方向+日期窗口+科目命中唯一即 MATCHED。
- **多候选（≥2）→ SUSPENSE**（非错误 MATCHED，转为人工挂起）。
- **零候选 → UNMATCHED**（留待手工/下轮）。

### 3.2 「同额同日不同 partner 且账面仅 1 候选」场景裁决

| 子场景 | 候选数 | 决策 | 是否错误 MATCHED? |
|---|---|---|---|
| 银行行 A + 凭证行 A（同 partner，唯一候选） | 1 | MATCHED | **否**（正确匹配） |
| 银行行 A（partner A）+ 凭证行 B（partner B，同额同日，唯一候选；A 的凭证行未过账/出窗口/被占用） | 1 | MATCHED | **是**（错误：A 的银行行勾对到 B 的凭证行） |
| 银行行 A + 凭证行 A + 凭证行 B（均同额同日，2 候选） | 2 | SUSPENSE | 否（转挂起，人工干预） |

**确认**：「同额同日不同 partner 且账面仅 1 候选」场景**确实产生错误 MATCHED**——这是 P1-RC-004 的运行时触发面。错误发生的前提是**非对称候选**：银行行 A 的实际对方（partner A）的凭证行**不在候选集**（未过账/出日期窗口/已被其他行占用），而 partner B 的同额同日反向凭证行是**唯一未占用候选**。

### 3.3 既有测试佐证（L4 决策逻辑行为）

- `TestErpFinBankStatementMatch#testUniqueCandidateMatched:62-90`：单候选 → MATCHED + matchedLineId 回写（**强断言**，A1.4 §3 引用）。该测试用单 partner 场景验证决策逻辑，**未构造跨 partner 同额场景**（即未覆盖本触发面）——A1.4 §3 已记录「对方账号匹配维度无测试」。
- `TestErpFinBankStatementMatch#testMultipleCandidatesStayUnmatched:93-120`：多候选 → SUSPENSE（实现取 UC-FIN-14 三态语义，多候选转 SUSPENSE 而非 UNMATCHED）。
- `TestErpFinBankStatementMatch#testMatchedLineIdOccupiedExcludedFromLaterCandidates:224-252`：已占用凭证行被排除——佐证 §2.2 已占用排除逻辑。

> **决策逻辑无歧义**：`candidates.size()==1 → MATCHED` 是确定性行为，不需要运行探针即可定论。错误 MATCHED 的产生完全由「非对称候选」数据分布决定，属运行时数据普查范畴（§5 触发率评估）。

---

## 4. 可逆性兜底核验（Phase 1 `Proof` ③）—— **精化 A1.4 §5 两处假设**

> 本验证发现 A1.4 §5 P1-RC-004 对「可逆性 + 下游兜底」的假设**偏强**。以下两处精化不改变 P1 分级（核心 P0 判据仍不成立），但**强化 P1 不可降 P2 的理由**。

### 4.1 精化①：manualMatch **不能**直接修正错误 MATCHED

A1.4 §5 原文：「错误匹配可经 manualMatch 取消重勾（可逆）」。

**实测反驳**（`ErpFinBankStatementLineManualMatchProcessor.manualMatch:30-34`）：
```java
if (!BANK_MATCH_UNMATCHED.equals(line.getMatchStatus())
        && !BANK_MATCH_SUSPENSE.equals(line.getMatchStatus())) {
    throw new NopException(ERR_BANK_STMT_LINE_ALREADY_MATCHED)...;  // :32-33
}
```
- `manualMatch` 守卫**仅允许** `matchStatus ∈ {UNMATCHED, SUSPENSE}` 的行进入手工勾对。
- 自动勾对产生的错误 MATCHED 行，其 `matchStatus = MATCHED` → 调 `manualMatch` **直接抛 `ERR_BANK_STMT_LINE_ALREADY_MATCHED`**（:32-33）。
- `ErpFinBankStatementLineBizModel`（`:18-46`）仅暴露 `autoMatch` + `manualMatch` 两个领域 mutation，**无领域专属 unmatch / reset / rematch 操作**。
- `autoMatch` 的 `loadUnmatchedLines:96-102` 过滤 `matchStatus==UNMATCHED`，**跳过已 MATCHED 行**——重跑 autoMatch 不会自动修正错误匹配。

**可逆性真相**：错误 MATCHED **不能经领域操作直接取消**。修正路径 = 通用 CRUD `update` 把 `matchStatus` 重置回 `UNMATCHED`（再 manualMatch 到正确凭证行），但这是**手工数据修复操作**，非引导式用户工作流。`BankStatement` docStatus 在 DRAFT 阶段（未 RECONCILED/posted）时尚可如此重置；一旦调节表已 POSTED，错误匹配已固化进对账结果，修正成本更高。

**证据**：`ErpFinBankStatementLineManualMatchProcessor.java:30-34`（守卫）+ `ErpFinBankStatementLineBizModel.java:30-42`（仅 autoMatch/manualMatch）+ `BankStatementMatcher.loadUnmatchedLines:96-102`（仅 UNMATCHED）。

### 4.2 精化②：余额恒等式对**同额**错误 MATCHED **无效**

A1.4 §5 原文：「余额恒等式下游兜底（聚合错误会触发不平衡拒绝）」。

**实测分析**（`BankReconciliationBuilder.generate:67-82`）：
- 恒等式：`(statementBalance − bookBalance) − (bankCreditUnrecorded − bankDebitUnrecorded) == 0`，否则抛 `ERR_BANK_RECON_NOT_BALANCED`（:77-82）。
- `bankCreditUnrecorded`/`bankDebitUnrecorded` 来自 **UNMATCHED** 银行行的聚合（A1.4 §2.3）。

**失效模式推理**：错误 MATCHED（银行行 A 勾对到凭证行 B，二者同额 M）会：
- 把银行行 A 从「UNMATCHED 银行行」移出 → `bankCreditUnrecorded/bankDebitUnrecorded` **减少 M**（A 不再计入未达）。
- 把凭证行 B 标记为已占用（matchedLineId 回写）→ B 不再计入「账面有银行无」在途。
- **双侧等额 M 抵消** → 恒等式 `LHS − RHS` 不变 → **diff 仍 == 0** → **不触发不平衡拒绝**。

**结论**：余额恒等式是**聚合级守卫**，对**行级错误匹配**（同额错配）**无效**——因为金额本就是匹配条件（错配的两者金额必然相等），聚合层面双侧等额抵消，恒等式仍成立。错误 MATCHED 会**静默通过**余额调节，显示「已平衡」但 matched pair 错误（A 的银行收付款被归到 B 的凭证行）。

> 这与 P0 示例「凭证重复过账」（每次正常操作即破坏且聚合可见）不同：错误 MATCHED 的金额聚合不变，仅在勾对链路（matchedLineId）层面错误，且对账子系统与 GL/过账解耦（MA2 :48,223,365）——故 GL 余额/试算平衡/财务报表**均不受影响**。

### 4.3 可逆性兜底综合裁决

| 兜底机制 | A1.4 §5 假设 | 本验证实测 | 裁决 |
|---|---|---|---|
| manualMatch 取消重勾 | 「可经 manualMatch 取消可逆」 | manualMatch 守卫**拒绝** MATCHED 行；无领域 unmatch | **假设偏强**——可逆但需通用 CRUD 重置，非引导式工作流 |
| 余额恒等式下游兜底 | 「聚合错误会触发不平衡拒绝」 | 同额错配双侧等额抵消，恒等式**仍成立**，不触发拒绝 | **假设不成立**——恒等式对本失效模式无效 |

**综合**：可逆性**弱于假设**（存在但非领域引导），下游兜底**对本失效模式无效**。错误 MATCHED 可能**静默持续**（恒等式不报警 + 无领域 unmatch 工作流），直到人工核对 matched pair 时发现并手工重置。这**强化 P1-RC-004 不可降 P2 的理由**（兜底比假设弱，缺陷比初始表征更隐蔽），但**不升 P0**——核心 P0 判据（GL/过账正确性破坏 + 默认活跃路径破坏）仍不成立（§5 详述）。

**证据汇总（file:line）**：
- `ErpFinBankStatementLineManualMatchProcessor.java:30-34`（manualMatch 守卫拒绝 MATCHED）
- `ErpFinBankStatementLineBizModel.java:30-42`（仅 autoMatch + manualMatch，无 unmatch）
- `BankStatementMatcher.java:96-102`（loadUnmatchedLines 仅 UNMATCHED）
- `BankReconciliationBuilder.java:67-82`（恒等式守卫，聚合级）

---

## 5. P1-RC-004 分级确认/调整（Phase 1 `Decision`）

### 5.1 §2 P0 判据三源复核

| 升 P0 必要条件（§2 P0 判据） | 本验证核实 | 成立? |
|---|---|---|
| **P0④ 会计过账正确性破坏**：错误 MATCHED 破坏凭证过账/GL 正确性 | 错误仅限对账勾对链路（`matchedLineId` 写错）；凭证过账本身正确（对账子系统与过账解耦，MA2 :48,223,365 证实 ErpFinBankStatement vs ErpFinReconciliation 独立）；GL 余额/试算平衡/财务报表均不受对账 matchedLineId 影响 | **❌ 不成立** |
| **P0① 活跃数据破坏**：默认活跃路径即触发（如每次正常操作） | 触发需「同额 + 同日 ±N 天窗口 + 同资金科目 + 反向方向 + 非对称候选[实际对方无候选/被占用/出窗]」**多重前置巧合**；非每次对账默认触发（与 P0 示例「凭证重复过账」每次过账即破坏不同） | **❌ 不成立** |
| 不可逆 / 无任何兜底 | 技术可逆（通用 CRUD 重置 matchStatus + manualMatch 重勾，虽无领域引导工作流）；对账 DRAFT 阶段可重置；MA2 证实对账为独立子系统不污染 GL | **❌ 不成立**（弱可逆 + 子系统隔离） |

### 5.2 触发率定性评估（运行时数据分布推理）

触发错误 MATCHED 的**全部前置条件**（合取）：
1. 两笔交易（银行行 + 凭证行）**金额完全相等**（amount 精确匹配，:67-71）。
2. **反向方向**（银行 CREDIT ↔ 账面 DEBIT，自动满足——方向是匹配条件）。
3. 落在**同一日期窗口** `[txnDate − N, txnDate + N]`（N 默认 3，窄窗口）。
4. 命中**同一资金账户科目**（subjectId，:64——同一银行账户）。
5. **非对称候选**：银行行 A 的实际对方（partner A）的凭证行**不在候选集**（未过账/出窗口/已被占用），而 partner B 的同额凭证行是**唯一未占用候选**。

**触发率定性 = 中低**：
- **提高触发率的因素**：公共/整笔金额（薪资同额、月租金、贷款月供、整百整千金额）、同日批量收付、同一银行账户高频交易、多 distinct 对方。
- **降低触发率的因素**：反向方向约束（自动）、±3 天窄窗口、同资金科目约束（仅同银行账户）、`findOccupiedLineIds` 随匹配累积缩小单候选陷阱、银行 refNo 人工核对工作流。
- **关键制约（条件 5）**：需 partner A 的凭证行**恰好不在候选集**（最常见 = A 的收付款尚未过账=真在途）。若 A、B 的凭证行**都在**候选集 → 2 候选 → SUSPENSE（人工干预，无错误 MATCHED）。故错误 MATCHED 仅在**非对称**（一方在途/出窗/被占用）时发生。

**裁决**：触发率**非默认活跃**（需多重前置巧合），但**非可忽略**（公共金额场景真实存在，月末批量对账时同额碰撞概率上升）。综合 = **中低**。

### 5.3 与 A1.4 §5 P1 结论对照

| A1.4 §5 P1-RC-004 维持 P1 的依据 | 本验证核实 |
|---|---|
| 「错误匹配经 manualMatch 取消可逆」 | **精化**：manualMatch 守卫拒绝 MATCHED 行，可逆需通用 CRUD 重置（弱于假设，但技术可逆） |
| 「余额恒等式下游兜底」 | **精化**：同额错配恒等式无效（双侧等额抵消），兜底对本失效模式不生效 |
| 「非 P0④ 活跃数据破坏（与凭证重复过账的默认触发面不同）」 | **CONFIRMED**：触发需多重前置巧合，非默认活跃路径 |
| 「严重性 major（匹配正确性破坏，可逆 + 下游兜底）」 | **精化**：匹配正确性破坏确认；可逆性/兜底弱于假设 → major 维持（隐蔽性更高，但 GL 隔离） |

### 5.4 裁决

**维持 P1-RC-004 = P1。不升 P0，不降 P2。不触发 MR0 即时通道。**

- **P0 不成立**：P0④（会计过账正确性破坏）+ P0①（活跃数据破坏）均不成立——错误仅限对账勾对链路且 GL/过账解耦隔离 + 非默认活跃路径 + 技术可逆。
- **P2 不适用**：§2 P2①「次要验收标准未完全满足（主路径 OK，边界场景弱）」**不适用**——对方账号是 L1+L2 明确要求的**4 维度之一**（非次要边界），其缺失致**错误 MATCHED**（非仅精度下降），属 §2 P1①「行为实质偏离验收标准」。且本验证精化显示兜底弱于假设（错误可能静默持续），**强化不可降 P2**。
- **分级分层一致**：与 A1.4 §5 P1-RC-004 P1 结论分层一致；与 arm-index `:129` P1-RC-004 行（todo，MR1）衔接——本验证**确认维持 P1**，不升 P0 不降 P2。
- **修复仍归 MR1**（R1.0→RC-R1.n）：修复触及 **ORM 结构变更**（`ErpFinBankStatementLine` + `ErpFinVoucherLine` 增 counterpartyAccount 列）+ 匹配算法（`findCandidates` 增对方账号过滤）→ **须 ask-first + 独立 plan-audit**（§5 ORM 结构变更类）。本验证**不实施修复**（plan Non-Goals）。

> **本验证同时建议**（非阻塞，供 MR1 修复参考）：修复 P1-RC-004 时宜一并考虑补领域 unmatch 操作（修正 §4.1 精化①——当前 manualMatch 守卫使错误 MATCHED 无法经领域操作回退），以提供引导式可逆工作流。

---

## 6. §去重声明（与 arm-index 交叉比对）

本验证**未产生新 finding**。全部触发面/影响面归以下既有 finding：

| 既有 finding | 控制点 | 与本验证关系 |
|---|---|---|
| `P1-RC-004` | UC-FIN-09/14 断言② 对方账号匹配维度缺失（`findCandidates` 无对方账号过滤 + ORM 无列） | **本验证对象**。运行时影响面评估确认触发面存在（单候选错误 MATCHED）+ 精化两处兜底假设 → 维持 P1，不升 P0，不降 P2。 |
| `P2-RC-002` | valueDate→transactionDate 简化（`findCandidates:45-46` 用 txnDate 非 valueDate） | 不同维度（日期列 vs 对方账号），不可合并。本验证 §1/§2 引用其日期窗口实现但不重复定级。 |
| `P2-RC-001` | 跨多条 statement refNo 去重范围 | 不同断言（① dedup vs ② 勾对），不可合并。 |
| MA2 银行对账解耦 | 银行对账为独立子系统（`ErpFinBankStatement` vs `ErpFinReconciliation`） | 本验证 §5 P0④ 不成立的关键依据（对账勾对链路错误不污染 GL/过账），引用 MA2 :48,223,365。 |

**无未经比对直接新建的 finding。** P1-RC-004 已登记（arm-index `:129`），本验证只更新分级注记（确认维持 P1 + 两处兜底精化，§7）。

---

## 7. §8 过程纪律自检

### 7.1 checker actual vs baseline

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；方法论 §8 不以退出码 0 为门控；真正门控在 CI workflow `.github/workflows/compliance.yml`）。本计划为**只读评估**（零生产代码变更），故 checker 无回归风险。

| 规则 | actual | baseline（A1.4/A4.1.2 实测基线） | 判定 |
|---|---|---|---|
| R1a (dao().saveEntity BizModel) | 0 | 0 | = |
| R1b (dao().updateEntity BizModel) | 0 | 0 | = |
| R1c (dao().getEntityById BizModel) | 0 | 0 | = |
| R1d (dao().findAllByQuery BizModel) | 14 | 14 | = |
| R2a (BizModel daoFor ErpMd*) | 34 | 34 | = |
| R2b (BizModel daoFor Erp* 跨域) | 229 | 229 | = |
| R2c (全生产 daoFor 总量) | （脚本 R2c 段未输出计数即返回——既有行为，与 A4.1.2 一致） | 1382 | 不适用（脚本行为） |
| R2d (Processor daoFor ErpMd*) | 34 | 34 | = |

**说明**：
1. **R1/R2 计数与 A1.4/A4.1.2 实测基线完全一致（0 漂移）**——本计划零生产代码变更，结构上对计数零贡献。R2c 段脚本既有行为（未输出计数即返回，A4.1.2 已记录），与本验证无关。
2. **门控结论**：本验证无回归风险（零生产代码变更），checker 仅作过程记录，不作通过/失败门控。

### 7.2 closure-audit 独立性声明

本验证报告由主代理（执行者）起草。**结束审计将由独立子代理（新会话）执行**（plan §Closure Gates），执行者未自我审计，未将结束审计留为 `[ ]` 人工门控占位符。

### 7.3 与 arm-index 交叉去重声明

见 §6。全部触发面/影响面归既有 finding（`P1-RC-004` / `P2-RC-002` / `P2-RC-001` / MA2 解耦），无新建 finding。P1-RC-004 分级注记更新见 §0 + plan Phase 2（确认维持 P1 + 两处兜底精化）。

---

## 8. 验证范围与非目标

- **本验证只读**：读匹配算法（`findCandidates` + `autoMatch`）+ 读候选过滤 + 读 manualMatch 守卫 + 读恒等式守卫 + 引用 MA2/A1.4 + 触发率影响面推理。未改任何 `.java`/`.xml`/`.orm.xml`/真相源。
- **不重新核实 P1-RC-004 的维度缺失结论本身**（A1.4 §5.2 已定级；本验证只评估「触发率 + 影响面 + 分级确认/调整」）。
- **不实施修复**（P1-RC-004 修复触及 ORM 结构变更须 ask-first + 独立 plan-audit，归 MR1）。
- **不展开 A1.4 §7-2/§7-3/§7-4**（A4.1.12 调整凭证行级 / A4.1.13 跨多条 statement refNo / A4.1.14 config key 运维认知）。

## 9. MR0 触发登记

**无**。Phase 1 裁决为维持 P1（§5.4），不触发 MR0 即时通道（方法论 §10）。本验证不实施修复。

## 10. 结论

UC-FIN-09/14 断言②「自动勾对按 (金额, 反向方向, valueDate±N天, **对方账号**) 模糊匹配」的**对方账号维度缺失**（P1-RC-004）经运行时影响面评估：`BankLedgerQuery.findCandidates:39-84` 候选过滤确无对方账号维度 + `BankStatementMatcher.autoMatch:61-65` 单候选即自动 MATCHED → 「同额同日不同 partner 且账面仅 1 候选」场景**确认产生错误 MATCHED**（触发面存在）。但**维持 P1-RC-004 = P1，不升 P0**：P0④（会计过账正确性破坏）不成立（错误仅限对账勾对链路，GL/过账解耦隔离）+ P0①（活跃数据破坏）不成立（触发需多重前置巧合，非默认活跃路径）+ 技术可逆。**两处兜底精化**（manualMatch 守卫拒绝 MATCHED 行 + 余额恒等式对同额错配无效）强化不可降 P2 的理由，但不改变 P1 分级。触发率定性**中低**（公共金额场景真实存在但需非对称候选巧合）。**不触发 MR0，不升 P0，不降 P2，无新 finding。** P1-RC-004 修复仍归 MR1（触及 ORM 结构变更须 ask-first）。
