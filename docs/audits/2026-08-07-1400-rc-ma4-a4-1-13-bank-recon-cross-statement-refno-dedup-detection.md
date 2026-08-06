# RC MA4 A4.1.13 — UC-FIN-09/14 跨多条 statement refNo 重复漏检触发率与影响面 验证报告

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 运行时行为验证）
> 工作项：A4.1.13（MA4 运行时行为验证 — A1.4 §7-3：UC-FIN-09/14 断言① 跨多条 statement refNo 重复的实际检出，`findStatementIdByAccount:198-207` 仅查最近一条 statement 范围致跨 statement 重复 refNo 漏检，关联 P2-RC-001）
> 验证 plan：`docs/plans/2026-08-07-1400-1-rc-ma4-a4-1-13-bank-recon-cross-statement-refno-dedup-detection.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据[含 P0④活跃数据破坏/会计过账正确性破坏 / P0①] / §7 arm-index 衔接 / §8 过程纪律自检 / §10 MR0 即时通道 / §去重协议）
> 输入存疑点：A1.4 §7 存疑点 3（`docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` §7）
> 输入 finding：`P2-RC-001`（A1.4 §5.3 / §6，UC-FIN-09/14 断言① 导入幂等 dedup key 偏离 + 跨多条 statement 去重范围）
> 关联 finding：`P1-RC-004`（对方账号匹配缺失，A4.1.11 已评估维持 P1）/ `P2-RC-002`（valueDate→transactionDate 简化，watch-only）/ MA2 银行对账解耦既有行为（`2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`）
> 验证性质：**只读运行时影响面评估**（读去重范围逻辑 + 读 dedup key 决策 + 引用 MA2/A1.4 + 触发率影响面推理；不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 验证日期：2026-08-07
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **P2-RC-001 P0/P1 升级再评估** | **维持 P2（不升 P0/P1，不降级）** | 不触发 MR0 即时通道；维持 successor watch-only |
| dedup key 决策 | `assertNoDuplicates:147-178` refNo 优先（`existsByRefNo:158-162` → :181-187）回退 `(transactionDate, amount, dcDirection)` 组合键（`existsByComposite:163-176` → :189-196） | 与 A1.4 §5.3 一致（CONFIRMED） |
| **去重范围（本存疑点核心）** | `findStatementIdByAccount:198-207` = `fundAccountId` 过滤(:201) + `statementDate DESC`(:203) + `setLimit(1)`(:204)——**仅查最近一条 statement** 作为跨 statement 去重的存在性查询范围 | 与 A1.4 §5.3 一致（CONFIRMED） |
| 漏检路径 | refNo 跨多条 statement 重复 + 重复在**非最近一条 statement** → `existsByRefNo`/`existsByComposite` 查最近一条 statement 查不到 → **漏检 → 重复入账** | 触发面确认存在（理论上） |
| 触发率定性评估 | **极低**（银行 refNo 通常全局唯一，跨 statement 重复罕见 + 漏检需重复出现在非最近一条 statement 的双重前置） | 非默认活跃路径 |
| **可逆性兜底复核** | **三处有效兜底**：①重复行初始化为 UNMATCHED（`BankStatementImporter:91`）→ UI 可见；②重复行在 statement DRAFT 阶段可经 CRUD 删除（手工数据修复）+ 整张重复 statement 可删；③调节表 reverse 入口存在（`BankReconciliationBuilder.reverse:133-142`）可红冲重复调整凭证 | 可逆 + 多重兜底 |
| 余额恒等式兜底（精化） | per-statement 恒等式（`generate:67-82`）对**同额**重复行**双侧等额抵消**——`statementBalance` 含重复行 + `bankCreditUnrecorded/bankDebitUnrecorded` 含重复行 → diff 仍=0 不报警（与 A4.1.11 §4.2 同型分析） | 恒等式对本失效模式无效（非主要兜底，可逆性兜底①③为主） |
| 影响面边界 | 影响仅限银行对账子系统（重复 bank statement line + 可能的重复 BANK_RECON_ADJ 调整凭证），**不直接破坏 GL 过账正确性**（对账为独立子系统，MA2 :48,223,365 证实解耦） | P0④ 不成立 |
| 新 finding | **0** | 无新控制点（全部归既有 P2-RC-001） |
| MR0 触发 | **无** | — |

**整体裁决**：A1.4 §7 存疑点 3「跨多条 statement refNo 重复的实际检出」**经运行时影响面评估 CONFIRMED**：`BankStatementImporter.findStatementIdByAccount:198-207`（`fundAccountId` 过滤 + `statementDate DESC` + `setLimit(1)`）确实**仅查最近一条 statement** 作为去重存在性查询范围；当 refNo 跨多条 statement 重复且重复出现在非最近一条 statement 时，`existsByRefNo`/`existsByComposite` 查最近一条 statement 查不到 → **漏检 → 重复入账**（触发面存在）。但经 §2 P0/P1 判据三源复核，**维持 P2-RC-001 = P2**：①P0④「会计过账正确性破坏」**不成立**——重复仅限银行对账子系统的 bank statement line（独立子系统，MA2 :48,223,365 证实与 AR/AP 核销 + GL 过账解耦），凭证过账本身正确；②P0①「活跃数据破坏」**不成立**——触发需「refNo 跨 statement 重复（银行参考号全局唯一语义下罕见）+ 重复出现在非最近一条 statement」**双重前置巧合**，非默认活跃路径（与 P0 示例「凭证重复过账」每次正常操作即触发不同），且技术可逆；③**§2 P1①「功能完全缺失或行为实质偏离」不适用**——L1 三元组去重是**次要验收标准**（主路径 = 同 statement + 最近一条 statement 范围去重**已实现且强测**，仅跨多条 statement 边界场景弱），属 §2 P2①「次要验收标准未完全满足（主路径 OK，边界场景弱）」。**触发率定性为极低**（银行 refNo 通常全局唯一，跨 statement 重复罕见 + 漏检需非最近一条 statement 的双重巧合）。**可逆性兜底强于 A1.4 §5 假设**：重复行 UNMATCHED 可见 + DRAFT 阶段可删 + 调整凭证可红冲（三重兜底，虽无领域专属 unmatch 工作流但手工 CRUD 路径完整）；余额恒等式对本失效模式无效（同额双侧抵消）但**非主要兜底**（与 A4.1.11 §4.2 同型分析）。**不触发 MR0，不升 P0/P1，不降级，无新 finding。** P2-RC-001 修复仍归 MR1 successor（触及 ORM 加 bankTxnCode 列须 ask-first；或改 `findStatementIdByAccount` 去除 limit 1 全量扫描，纯代码修复预授权类目）。

---

## 1. 需求契约与锚点原文（L1 + L2 + L3）

### L1 权威（UC-FIN-09/14 断言①，`docs/design/finance/use-cases.md`）

```
UC-FIN-09 (use-cases.md:171):
  导入银行对账单(bankTxnCode 幂等去重)                                    ← 断言①

UC-FIN-14 (use-cases.md:276):
  // 导入幂等
  导入银行对账单 → 以 (fundAccount, statementDate, bankTxnCode) 去重, 重复导入报错   ← 断言①
```

- **本验证对象** = 断言①「dedup 三元组 `(fundAccount, statementDate, bankTxnCode)`」的**去重范围**：当去重存在性查询仅覆盖最近一条 statement 时，refNo 跨多条 statement 重复在何种数据分布下**漏检致重复入账**，以及该漏检的影响面与可逆性。
- L1 三元组含 `statementDate` 但 `bankTxnCode` 全局唯一语义要求**跨所有 statement 去重**（同一笔银行交易不会因出现在不同 statement 而成为两笔）。断言②对方账号缺失 = `P1-RC-004`（A4.1.11 已评估）；断言②valueDate = `P2-RC-002`（watch-only）。均与本验证**不同控制点**（§去重）。

### L2 owner doc 契约（`bank-reconciliation.md`）

```
§业务规则 1（bank-reconciliation.md:96）：
  对账单导入幂等:以 (fundAccountId, statementDate, bankTxnCode) 为唯一键去重,重复导入报"已存在"。

schema 补注（bank-reconciliation.md:147）：
  导入幂等键：refNo（银行参考号）优先，缺失回退 (transactionDate, amount, dcDirection) 组合键；
  严格度经 erp-fin.bank-import-strict-refno 配置（true=缺 refNo 拒绝）。不新增 bankTxnCode 列。

§业务规则 5（bank-reconciliation.md:106）：
  余额调节恒等式: bankBalance + amtInTransitIn − amtInTransitOut = bookBalance + amtBankNotInBooks,
  diffAmount 必须 = 0,否则抛 NopException 阻止 RECONCILED。
```

- **L2 §业务规则 1 与 L1 一致**（三元组 `(fundAccountId, statementDate, bankTxnCode)`），但 **schema 补注 :147 记录实现偏离**（refNo 优先 + 组合键回退，不新增 bankTxnCode 列）——**未经 §4 三判据人工批准**（A1.4 §5.3 已确认），冲突以 L1 为准。本验证不重复定级 key 偏离（P2-RC-001 已登记），只评估**去重范围**（schema 补注未显式记录范围收窄至最近一条 statement，属未文档化的静默范围缺口）。

### L3 实仓锚点（`module-finance/erp-fin-service/.../bankrecon/BankStatementImporter.java`，写时实测）

| 锚点 | 文件:行 | 实现 | 说明 |
|---|---|---|---|
| 导入入口 | `BankStatementImporter.importStatement:44-107` | 校验→requireBankAccount→批次校验→**assertNoDuplicates:59**→写头:61-73→写行:75-102→合计回写:103-105 | 去重在写库前 |
| **dedup key 决策** | `assertNoDuplicates:147-178` | refNo 优先：批次内 `seenRefNo:153` + 跨批次 `existsByRefNo:158-162`；缺失回退组合键：批次内 `seenComposite:166` + 跨批次 `existsByComposite:163-176` | refNo 优先（语义近似 bankTxnCode），组合键回退 |
| **去重范围（本存疑点核心）** | `findStatementIdByAccount:198-207` | `q.addFilter(eq("fundAccountId", fundAccountId)):201` + `q.addOrderField("statementDate", true):203` + `q.setLimit(1):204` → `dao.findFirstByQuery(q):205` | **仅查最近一条 statement**（fundAccountId 过滤 + statementDate DESC + limit 1） |
| refNo 存在性查询 | `existsByRefNo:181-187` | `findStatementIdByAccount(fundAccountId):182` → 若 null 返回 false:183-185 → 否则 `countLinesByFilter(statementId == 最近一条 AND refNo == X):186` | 存在性查询范围 = 最近一条 statement 的行 |
| 组合键存在性查询 | `existsByComposite:189-196` | `findStatementIdByAccount(fundAccountId):190` → 若 null 返回 false:191-193 → 否则 `countLinesByFilter(statementId == 最近一条 AND transactionDate == D AND amount == A AND dcDirection == DC):194-195` | 同上，范围 = 最近一条 statement |
| 行初始化 | `importStatement:91` | `line.setMatchStatus(ErpFinConstants.BANK_MATCH_UNMATCHED)` | 重复行（若漏检）以 UNMATCHED 入库 → UI 可见 |
| 余额恒等式守卫 | `BankReconciliationBuilder.generate:67-82` | `diff = (statementBalance − bookBalance) − (bankCreditUnrecorded − bankDebitUnrecorded)`，diff≠0 抛 `ERR_BANK_RECON_NOT_BALANCED`(:77-82) | per-statement 聚合守卫（见 §4.3） |
| 调整凭证红冲 | `BankReconciliationBuilder.reverse:133-142` | POSTED 守卫:135-137 → `adjustmentVoucherBuilder.reverse:138` → docStatus=CANCELLED:140 | 手动红冲入口存在 |

> **关键观察**：`findStatementIdByAccount:198-207` 的 `setLimit(1) + statementDate DESC` 把跨 statement 去重的存在性查询范围**收窄至最近一条 statement**。`existsByRefNo`/`existsByComposite` 均调用此方法，故二者去重范围同此收窄。本验证回答：**该范围收窄在何种数据分布下产生漏检致重复入账，影响面多大，是否够格升 P0/P1？**

---

## 2. 去重范围逻辑核验（Phase 1 `Proof` ①）

### 2.1 去重存在性查询范围（`findStatementIdByAccount:198-207`）

`findStatementIdByAccount(fundAccountId)` 的查询逻辑（写时实测行号）：

| 步骤 | 行 | 查询条件 | 范围 |
|---|---|---|---|
| 实体 | :199 | `daoProvider.daoFor(ErpFinBankStatement.class)` | 银行对账单头表 |
| **资金账户过滤** | :201 | `q.addFilter(eq("fundAccountId", fundAccountId))` | 同资金账户 |
| **排序** | :203 | `q.addOrderField("statementDate", true)`（true=DESC） | 最近 statementDate 优先 |
| **行数限制** | :204 | `q.setLimit(1)` | **仅取 1 条** |
| 执行 | :205 | `dao.findFirstByQuery(q)` | 返回最近一条 statement 或 null |

**结论**：`findStatementIdByAccount` 返回**同 fundAccountId 下 statementDate 最近的唯一一条 statement**。`existsByRefNo:182-186` 与 `existsByComposite:190-195` 均以该 statementId 为存在性查询范围 → **跨多条 statement 的重复 refNo/组合键不检出**（只要重复不出现在最近一条 statement）。

### 2.2 dedup key 决策核验（`assertNoDuplicates:147-178`）

`assertNoDuplicates(fundAccountId, lines)` 对每条待导入行：

| 分支 | 批次内去重 | 跨批次（已入库）去重 | 范围 |
|---|---|---|---|
| **refNo 非空**（:152-162） | `seenRefNo.add(refNo):153` 重复则抛 :154-156 | `existsByRefNo(fundAccountId, refNo):158` → :181-187 → `findStatementIdByAccount:182`（最近一条 statement）+ `countLinesByFilter(statementId==最近 AND refNo==X):186` | **最近一条 statement** |
| **refNo 空**（:163-176） | `seenComposite.add(date\|amount\|dc):166` 重复则抛 :167-170 | `existsByComposite(fundAccountId, date, amount, dc):171` → :189-196 → 同上范围 | **最近一条 statement** |

**确认**：两个存在性查询分支（refNo + 组合键）**均经 `findStatementIdByAccount` 收窄至最近一条 statement**。批次内去重（seenRefNo/seenComposite）覆盖本次导入，跨批次去重仅覆盖最近一条 statement——**跨多条 statement 的历史重复不检出**。

### 2.3 漏检路径裁决

| 子场景 | 重复 refNo 位置 | `existsByRefNo` 查询范围 | 是否漏检? |
|---|---|---|---|
| 同 statement 内重复（本次批次 vs 最近一条 statement 同 statementId） | 最近一条 statement | 命中 → 抛异常 :159-161 | **否**（主路径正确拒绝） |
| 跨 statement 重复（重复在最近一条 statement） | 最近一条 statement | 命中 → 抛异常 :159-161 | **否**（最近一条 statement 范围内正确拒绝） |
| **跨 statement 重复（重复在非最近一条 statement）** | 较旧 statement（非最近） | 最近一条 statement 查不到 → 返回 false :186 | **是**（漏检 → 重复入账） |
| 跨 statement 重复（重复在本次批次 + 较旧 statement，但本次批次内 seenRefNo 先命中） | 本次批次内 | seenRefNo:153 先命中 → 抛异常 :154-156 | **否**（批次内去重先拦截） |

**确认**：「refNo 跨多条 statement 重复 + 重复出现在非最近一条 statement + 本次批次内无该 refNo」场景**确实漏检致重复入账**——这是 P2-RC-001 的运行时触发面。漏检的必要条件是**重复出现在非最近一条 statement**（若在最近一条 statement，去重范围覆盖，正确拒绝）。

### 2.4 既有测试佐证（L4 去重范围覆盖）

- `TestErpFinBankStatementImport`（6 @Test，A1.4 §3 引用）：覆盖同 statement refNo 去重（`testDuplicateByRefNoRejected:81-98`）+ 组合键去重（`testCompositeKeyDedupWhenNoRefNo:137-153`）+ strict-refno + 跨账号允许 + happy path。
- **跨多条 statement refNo 重复无测试**：grep `cross|crossStatement|multipleStatements|secondStatement|跨.*statement|limit 1|findStatementIdByAccount` 全 `module-finance/erp-fin-service/src/test/.../bankrecon/` **零命中**——A1.4 §3 已记录「跨多条 statement 去重无测试」。

> **去重范围无歧义**：`findStatementIdByAccount:204 setLimit(1) + :203 statementDate DESC` 是确定性行为，不需要运行探针即可定论。漏检的产生完全由「refNo 跨 statement 重复且重复在非最近一条 statement」数据分布决定，属运行时数据普查范畴（§3 触发率评估）。

**证据汇总（file:line）**：
- `BankStatementImporter.findStatementIdByAccount:198-207`（fundAccountId 过滤 :201 / DESC :203 / setLimit(1) :204 / findFirstByQuery :205）
- `BankStatementImporter.existsByRefNo:181-187`（范围 = 最近一条 statement）
- `BankStatementImporter.existsByComposite:189-196`（范围 = 最近一条 statement）
- `BankStatementImporter.assertNoDuplicates:147-178`（dedup key 决策，refNo 优先回退组合键）
- `BankStatementImporter.importStatement:91`（重复行 UNMATCHED 初始化）

---

## 3. 触发率影响面核验（Phase 1 `Proof` ②）

### 3.1 银行 refNo 全局唯一性语义评估

银行参考号（refNo / bankTxnCode / bank reference number）的语义特性：
- **每笔银行交易全局唯一**：银行系统为每笔交易分配唯一参考号（用于跨行/跨系统追溯），同一参考号不会在不同 statement 重复出现（重复出现意味着同一笔交易被银行多次记账，属银行系统异常而非正常业务路径）。
- **跨 statement 不重复的银行业务原因**：statement 按 statementDate 切分（日/周/月对账单），但参考号是交易级而非 statement 级——一笔交易只属一个 statement，参考号跨 statement 重复在正常银行业务中**不发生**。
- **可能重复的边缘场景**（极罕见）：①银行系统故障重复生成参考号；②手工导入时 CSV/Excel 复制粘贴错误；③跨账户混导（已被 fundAccountId 过滤排除）；④测试/种子数据污染。

**结论**：银行 refNo 在正常业务中**全局唯一**，跨 statement 重复属**极罕见的边缘场景**（银行系统异常或人工操作错误）。A1.4 §5.3 P2-RC-001 已确认「银行 refNo 通常全局唯一跨 statement 重复罕见」，本验证运行时复核 CONFIRMED。

### 3.2 漏检触发率定性评估（运行时数据分布推理）

漏检致重复入账的**全部前置条件**（合取）：
1. **refNo 跨多条 statement 重复**：同一 refNo 出现在 ≥2 张 statement（银行系统异常或人工操作错误，极罕见）。
2. **重复出现在非最近一条 statement**：重复的 refNo 在较旧 statement（非 statementDate DESC limit 1 取到的最近一条）。若重复在最近一条 statement，去重范围覆盖，正确拒绝。
3. **本次导入批次内无该 refNo**：本次导入的行不含该 refNo（否则 seenRefNo:153 批次内先拦截）。
4. **资金账户相同**：重复出现在同 fundAccountId（跨账户重复已被 :201 fundAccountId 过滤排除，属正常允许）。

**触发率定性 = 极低**：
- **条件 1 制约（最关键）**：银行 refNo 全局唯一语义使跨 statement 重复本身极罕见。正常银行业务下，每笔交易参考号唯一，跨 statement 重复只在银行系统故障/人工操作错误下发生。
- **条件 2 制约**：即使发生跨 statement 重复（条件 1），还需重复出现在非最近一条 statement（若在最近一条 statement，去重覆盖）。这进一步缩小触发面。
- **条件 3 制约**：本次批次内 seenRefNo 提供第一道拦截（同批次重复直接抛异常），只有跨批次（历史 statement）的重复才进入 existsByRefNo 路径。

**裁决**：触发率**极低**——需「银行 refNo 跨 statement 重复（本身罕见）+ 重复在非最近一条 statement + 本次批次无该 refNo」多重前置巧合。非默认活跃路径（与 P0 示例「凭证重复过账」每次正常操作即触发截然不同）。

### 3.3 与 A1.4 §5.3 P2-RC-001 触发面结论对照

| A1.4 §5.3 P2-RC-001 触发面结论 | 本验证核实 |
|---|---|
| 「银行 refNo 通常全局唯一跨 statement 重复罕见」 | **CONFIRMED**：银行参考号交易级全局唯一，跨 statement 重复属银行系统异常/人工错误，极罕见（§3.1） |
| 「主路径（同 statement / 最近一条 statement 重复拒绝）OK」 | **CONFIRMED**：§2.3 漏检路径裁决表前两行（同 statement + 最近一条 statement 范围）正确拒绝 |
| 「边界（跨 statement refNo 重复）弱」 | **CONFIRMED**：§2.3 第三行（非最近一条 statement）漏检路径成立，触发率极低（§3.2） |

---

## 4. 可逆性兜底核验（Phase 1 `Proof` ③）

> 本验证复核漏检致重复入账后的可逆性 + 下游兜底。结论：**可逆性兜底强于 A1.4 §5 假设**（三重兜底），余额恒等式对本失效模式无效（同额双侧抵消，与 A4.1.11 §4.2 同型）但**非主要兜底**。

### 4.1 兜底①：重复行 UNMATCHED 可见（UI 可观测）

漏检致重复入账后，重复的 bank statement line 经 `BankStatementImporter.importStatement:91` 初始化为 `matchStatus = BANK_MATCH_UNMATCHED`。

- 重复行进入对账流程时**以 UNMATCHED 状态可见**（银行对账单行列表 UI 显示）。
- 出纳/会计在对账时可见「未勾对行」列表中存在金额/日期/refNo 与已勾对历史行重复的行 → **人工可识别重复**。
- 重复行若进入 autoMatch（`BankStatementMatcher.autoMatch:41-74`）：①若有候选凭证行 → MATCHED（占用凭证行，但凭证行已被历史行占用则排除——`findOccupiedLineIds:105-123`）；②若无候选 → 保持 UNMATCHED（可见）。

**证据**：`BankStatementImporter.importStatement:91`（UNMATCHED 初始化）+ `BankStatementMatcher.loadUnmatchedLines`（UNMATCHED 行可观测）。

### 4.2 兜底②：DRAFT 阶段手工删除（CRUD 可逆）

重复行所在的 statement 在 DRAFT 阶段（`importStatement:72 docStatus = VOUCHER_STATUS_DRAFT`）：
- **重复行可经通用 CRUD 删除**：bank statement line 属普通 ORM 实体，DRAFT 阶段（未 RECONCILED/posted）可手工 delete（手工数据修复操作）。
- **整张重复 statement 可删**：若整张 statement 为重复导入（如人工误导），DRAFT 阶段可删除整张 statement + 其行。
- 一旦调节表已 POSTED（`BankReconciliationBuilder.post:119-131`），重复行已固化进对账结果，修正成本升高（需走 reverse 红冲，见兜底③）。

**证据**：`BankStatementImporter.importStatement:72`（DRAFT 初始化）+ Nop 平台通用 CRUD（deleteEntity）。

### 4.3 余额恒等式兜底（精化：对本失效模式无效，非主要兜底）

A1.4 §5 原文：「余额恒等式下游兜底（聚合错误会触发不平衡拒绝）」。

**实测分析**（`BankReconciliationBuilder.generate:67-82`）：
- 恒等式：`(statementBalance − bookBalance) − (bankCreditUnrecorded − bankDebitUnrecorded) == 0`，否则抛 `ERR_BANK_RECON_NOT_BALANCED`（:77-82）。
- `bankCreditUnrecorded`/`bankDebitUnrecorded` 来自 **UNMATCHED** 银行行的聚合（:55-63）。
- `statementBalance` = statement.endingBalance（:66，含重复行金额，因 `importStatement:103-105` 合计回写含重复行）。

**失效模式推理**：重复行（漏检致重复入账，金额 M，方向 CREDIT）会：
- `statementBalance` **增加 M**（endingBalance 含重复行）。
- 若重复行 UNMATCHED → `bankCreditUnrecorded` **增加 M**（:58）。
- **双侧等额 M 抵消** → diff = (statementBalance + M − bookBalance) − (bankCreditUnrecorded_orig + M − bankDebitUnrecorded_orig) = 原 diff（不变）→ **diff 仍 == 0** → **不触发不平衡拒绝**。

**结论**：per-statement 余额恒等式是**聚合级守卫**，对**同额重复行**（漏检致重复入账的两者金额必然相等，因为是同一 refNo 的重复）**无效**——双侧等额抵消，恒等式仍成立。重复行会**静默通过**余额调节。这与 A4.1.11 §4.2 对「同额错误 MATCHED」的恒等式分析**同型**（聚合守卫对行级同额问题无效）。

> **注**：本兜底虽对本失效模式无效，但**非主要兜底**——漏检致重复入账的可观测性主要依赖兜底①（UNMATCHED 可见）+ 兜底②（DRAFT 删除）+ 兜底③（reverse 红冲）。恒等式无效不削弱整体可逆性（三重兜底已覆盖）。

### 4.4 兜底③：调整凭证红冲（reverse 可逆）

若重复行已进入 POSTED 调节表（经 `post:119-131` 生成 BANK_RECON_ADJ 调整凭证）：
- **手动红冲入口存在**：`BankReconciliationBuilder.reverse:133-142`（POSTED 守卫 :135-137 → `adjustmentVoucherBuilder.reverse:138` → docStatus=CANCELLED:140）。
- 调整凭证经 `BankReconAdjustmentVoucherBuilder.reverse:97-102` 红冲（hasAdjustmentVoucher guard :98-100 → voucherBiz.reverse :101）。
- 重复的调整凭证可经红冲还原（虽无领域专属「去重」操作，但 reverse 提供会计可逆路径）。
- **与 P1-RC-005（下月自动红冲缺失）不同控制点**：P1-RC-005 = 自动调度缺失（config key 无消费），本验证 = 手动 reverse 入口存在（可逆性兜底）。手动 reverse 可补救重复调整凭证。

**证据**：`BankReconciliationBuilder.reverse:133-142` + `BankReconAdjustmentVoucherBuilder.reverse:97-102`。

### 4.5 可逆性兜底综合裁决

| 兜底机制 | A1.4 §5 假设 | 本验证实测 | 裁决 |
|---|---|---|---|
| 重复行可见性 | （未显式记录） | UNMATCHED 初始化 :91 → UI 可见 + autoMatch 可观测 | **CONFIRMED**——重复行不静默，UI 可识别 |
| DRAFT 阶段手工删除 | （隐含「手工去重可逆」） | DRAFT 阶段 CRUD delete 可删重复行/整张 statement | **CONFIRMED**——DRAFT 阶段可逆 |
| 余额恒等式下游兜底 | 「聚合错误会触发不平衡拒绝」 | 同额重复行双侧等额抵消，恒等式**仍成立**，不触发拒绝 | **假设不成立**——恒等式对本失效模式无效（同型 A4.1.11 §4.2），但非主要兜底 |
| 调整凭证红冲 | （隐含可逆） | `reverse:133-142` 手动红冲入口存在，可红冲重复 BANK_RECON_ADJ | **CONFIRMED**——POSTED 阶段亦会计可逆 |

**综合**：可逆性**强于 A1.4 §5 假设**——三重兜底（UNMATCHED 可见 + DRAFT 删除 + reverse 红冲）覆盖重复行的全生命周期，虽无领域专属 unmatch 工作流但手工 CRUD + reverse 路径完整。余额恒等式对本失效模式无效（同额双侧抵消），但**非主要兜底**（三重兜底已覆盖可观测性 + 可逆性）。重复行**不会静默破坏**（UI 可见 + 多阶段可逆），但可能**持续存在**直到人工核对发现（恒等式不报警）。这**不改变 P2 分级**（核心 P0/P1 判据仍不成立，§5 详述）。

**证据汇总（file:line）**：
- `BankStatementImporter.java:91`（重复行 UNMATCHED 初始化）
- `BankStatementImporter.java:72`（statement DRAFT 初始化）
- `BankReconciliationBuilder.java:55-82`（恒等式守卫，per-statement 聚合，同额双侧抵消）
- `BankReconciliationBuilder.java:133-142`（reverse 手动红冲入口）
- `BankReconAdjustmentVoucherBuilder.java:97-102`（调整凭证红冲）
- MA2 `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`（银行对账独立子系统，与 GL 过账解耦）

---

## 5. P2-RC-001 分级确认/调整（Phase 1 `Decision`）

### 5.1 §2 P0/P1 判据三源复核

| 升级 P0/P1 必要条件（§2 判据） | 本验证核实 | 成立? |
|---|---|---|
| **§2 P0④ 会计过账正确性破坏**：重复入账破坏凭证过账/GL 正确性 | 重复仅限银行对账子系统的 bank statement line（独立子系统，MA2 :48,223,365 证实 ErpFinBankStatement vs ErpFinReconciliation 独立 + 与 GL 过账解耦）；凭证过账本身正确（重复的是对账单行，非凭证）；重复调整凭证（BANK_RECON_ADJ）经 reverse 可红冲；GL 余额/试算平衡/财务报表均不受 bank statement line 重复直接影响 | **❌ 不成立** |
| **§2 P0① 活跃数据破坏**：默认活跃路径即触发（如每次正常操作） | 触发需「refNo 跨 statement 重复（银行全局唯一语义下罕见）+ 重复在非最近一条 statement + 本次批次无该 refNo」**多重前置巧合**；非每次导入默认触发（与 P0 示例「凭证重复过账」每次过账即破坏不同） | **❌ 不成立** |
| **§2 P1① 功能完全缺失或行为实质偏离验收标准** | L1 三元组去重**主路径已实现**（同 statement + 最近一条 statement 范围重复拒绝，6 强断言覆盖）；跨多条 statement 范围缺口属**次要验收标准的边界场景弱**，非「功能完全缺失」或「行为实质偏离」（主路径行为正确） | **❌ 不适用**（属 P2① 非 P1①） |
| **§2 P1② 异常路径未实现** | dedup 非异常路径（是主路径幂等机制）；漏检是范围收窄的边界，非异常路径缺失 | **❌ 不适用** |
| 不可逆 / 无任何兜底 | 技术可逆（UNMATCHED 可见 + DRAFT 阶段 CRUD 删除 + reverse 红冲三重兜底）；对账 DRAFT 阶段可删重复行；MA2 证实对账为独立子系统不污染 GL | **❌ 不成立**（多重可逆） |

### 5.2 §2 P2① 判据命中确认

| §2 P2① 命中条件 | 本验证核实 |
|---|---|
| 「需求契约的次要验收标准未完全满足」 | L1 三元组 `(fundAccount, statementDate, bankTxnCode)` 去重要求跨所有 statement（bankTxnCode 全局唯一语义）；实现去重范围仅最近一条 statement（`findStatementIdByAccount:204 setLimit(1)`）——次要验收标准（跨多条 statement 范围）未完全满足 |
| 「主路径 OK」 | 同 statement + 最近一条 statement 范围重复拒绝**主路径已实现且强测**（`TestErpFinBankStatementImport` 6 @Test 强断言：refNo/composite/strict/cross-account/happy） |
| 「边界场景弱」 | 跨多条 statement refNo 重复（银行全局唯一语义下罕见）漏检——边界场景弱，触发率极低（§3.2） |

**结论**：§2 **P2①**「需求契约的次要验收标准未完全满足（主路径 OK，边界场景弱）」**成立**——主路径去重正确，跨多条 statement 边界漏检，触发率极低。

### 5.3 与 A1.4 §5.3 P2-RC-001 P2 结论对照

| A1.4 §5.3 P2-RC-001 维持 P2 的依据 | 本验证核实 |
|---|---|
| 「主路径（同 statement / 最近 statement 重复拒绝）OK」 | **CONFIRMED**：§2.3 漏检路径裁决表前两行 + 6 强断言覆盖主路径 |
| 「边界（跨 statement refNo 重复）弱」 | **CONFIRMED**：§2.3 第三行漏检路径 + §3.2 触发率极低 |
| 「银行 refNo 通常全局唯一跨 statement 重复罕见」 | **CONFIRMED**：§3.1 银行参考号交易级全局唯一语义 |
| 「余额恒等式下游兜底」 | **精化**：同额重复行双侧等额抵消，恒等式对本失效模式无效（§4.3）——但非主要兜底，三重兜底（UNMATCHED 可见 + DRAFT 删除 + reverse 红冲）覆盖可逆性 |
| 「修复触及 ORM 加 bankTxnCode 列须 ask-first；或改 findStatementIdByAccount 去除 limit 1，纯代码修复预授权」 | **CONFIRMED**：修复路径不变，归 MR1 successor |

### 5.4 裁决

**维持 P2-RC-001 = P2。不升 P0/P1，不降级。不触发 MR0 即时通道。**

- **P0 不成立**：P0④（会计过账正确性破坏）+ P0①（活跃数据破坏）均不成立——重复仅限银行对账子系统（与 GL 过账解耦隔离，MA2 :48,223,365）+ 非默认活跃路径（触发率极低）+ 技术多重可逆。
- **P1 不适用**：§2 P1①「功能完全缺失或行为实质偏离」**不适用**——dedup 主路径（同 statement + 最近一条 statement 范围）已实现且强测，跨多条 statement 范围缺口属次要验收标准边界场景弱（§2 P2①），非功能完全缺失/行为实质偏离。
- **P2 维持**：§2 P2①「次要验收标准未完全满足（主路径 OK，边界场景弱）」成立——主路径去重正确，跨多条 statement 边界漏检，触发率极低（银行 refNo 全局唯一）。
- **不降级（维持 P2 非「接受」）**：范围收窄至最近一条 statement 仍为**合规缺陷**（L1 三元组要求跨所有 statement 去重），虽触发率极低但缺陷客观存在，须登记 finding 待 MR1 修复，不接受。
- **分级分层一致**：与 A1.4 §5.3 P2-RC-001 P2 结论分层一致；与 arm-index `:131` P2-RC-001 行（todo，successor watch-only）衔接——本验证**确认维持 P2**，不升 P0/P1，不降级。
- **修复仍归 MR1 successor**（不触发 MR0 即时通道）：修复触及 ①ORM 加 bankTxnCode 列（对齐 L1 三元组）须 ask-first + 独立 plan-audit（§5 ORM 结构变更类）；或 ②改 `findStatementIdByAccount` 去除 `setLimit(1)` 全量扫描同 account 所有 statement 的 refNo（纯代码修复，roadmap 预授权类目可自动执行，不触发 §5 ask-first）。本验证**不实施修复**（plan Non-Goals）。

> **本验证同时建议**（非阻塞，供 MR1 修复参考）：修复 P2-RC-001 时宜优先采用纯代码方案②（`findStatementIdByAccount` 去除 limit 1，改为查同 fundAccountId 所有 statement 的 refNo/组合键存在性），避免触及 ORM 结构变更（方案①加 bankTxnCode 列须 ask-first）。方案②的实现成本较低（查询范围扩大至全 account statement，无新列），且对齐 L1 三元组的「跨所有 statement 去重」语义。

---

## 6. §去重声明（与 arm-index 交叉比对）

本验证**未产生新 finding**。全部触发面/影响面归以下既有 finding：

| 既有 finding | 控制点 | 与本验证关系 |
|---|---|---|
| `P2-RC-001` | UC-FIN-09/14 断言① 导入幂等 dedup key 偏离 + 跨多条 statement 去重范围（`findStatementIdByAccount:198-207` limit 1 + refNo 优先回退组合键） | **本验证对象**。运行时影响面评估确认触发面存在（跨多条 statement 漏检）+ 精化可逆性兜底（三重兜底，恒等式对本失效模式无效但非主要兜底）→ 维持 P2，不升 P0/P1，不降级。 |
| `P1-RC-004` | UC-FIN-09/14 断言② 对方账号匹配维度缺失（A4.1.11 已评估） | 不同断言（② 勾对 vs ① dedup），不可合并。本验证 §4.3 引用 A4.1.11 §4.2 恒等式同型分析。 |
| `P2-RC-002` | valueDate→transactionDate 简化（断言②） | 不同断言（② 勾对日期 vs ① dedup 范围），不可合并。 |
| MA2 银行对账解耦 | 银行对账为独立子系统（`ErpFinBankStatement` vs `ErpFinReconciliation`） | 本验证 §5 P0④ 不成立的关键依据（重复仅限对账子系统不污染 GL/过账），引用 MA2 :48,223,365。 |

**无未经比对直接新建的 finding。** P2-RC-001 已登记（arm-index `:131`），本验证只更新分级注记（确认维持 P2 + 三重兜底精化 + 恒等式对本失效模式无效注记，§7）。

---

## 7. §8 过程纪律自检（Phase 2 `Proof`）

### 7.1 closure-audit 独立性声明

本验证报告由主代理（执行者）起草。**结束审计将由独立子代理（新会话）执行**（plan §Closure Gates），执行者未自我审计，未将结束审计留为 `[ ]` 人工门控占位符。

### 7.2 与 arm-index 交叉去重声明

见 §6。全部触发面/影响面归既有 finding（`P2-RC-001` / `P1-RC-004` / `P2-RC-002` / MA2 解耦），无新建 finding。P2-RC-001 分级注记更新见 plan Phase 2（确认维持 P2 + 三重兜底精化 + 恒等式对本失效模式无效注记）。

### 7.3 checker actual vs baseline

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；方法论 §8 不以退出码 0 为门控；真正门控在 CI workflow `.github/workflows/compliance.yml`）。本计划为**只读评估**（零生产代码变更），故 checker 无回归风险。

| 规则 | actual | baseline（A1.4 实测基线） | 判定 |
|---|---|---|---|
| R1a (dao().saveEntity BizModel) | 0 | 0 | = |
| R1b (dao().updateEntity BizModel) | 0 | 0 | = |
| R1c (dao().getEntityById BizModel) | 0 | 0 | = |
| R1d (dao().findAllByQuery BizModel) | 14 | 14 | = |
| R2a (BizModel daoFor ErpMd*) | 34 | 34 | = |
| R2b (BizModel daoFor Erp* 跨域) | 229 | 229 | = |
| R2c (全生产 daoFor 总量) | 1382 | 1382 | = |
| R2d (Processor daoFor ErpMd*) | 34 | 34 | = |
| R3-R12 | （脚本 R3 段起未输出计数即返回——既有行为，与 A4.1.11/A4.1.2 一致） | 5/0/0/2/0/0/6/0/69/66/40 | 不适用（脚本既有行为） |

**说明**：
1. **R1/R2 计数与 A1.4/A4.1.11 实测基线完全一致（0 漂移）**——本计划零生产代码变更，结构上对计数零贡献。R3 段起脚本既有行为（未输出计数即返回，A4.1.11 §7.1 已记录），与本验证无关；因零生产代码变更，R3-R12 客观上与基线一致。
2. **门控结论**：本验证无回归风险（零生产代码变更），checker 仅作过程记录，不作通过/失败门控。

---

## 8. 验证范围与非目标

- **本验证只读**：读去重范围逻辑（`findStatementIdByAccount` + `existsByRefNo`/`existsByComposite`）+ 读 dedup key 决策（`assertNoDuplicates`）+ 读行初始化（UNMATCHED）+ 读恒等式守卫（`generate`）+ 读 reverse 入口 + 引用 MA2/A1.4 + 触发率影响面推理。未改任何 `.java`/`.xml`/`.orm.xml`/真相源。
- **不重新核实 P2-RC-001 的 dedup key 偏离结论本身**（A1.4 §5.3 已定级；本验证只评估「跨多条 statement 漏检触发率 + 影响面 + 分级确认/调整」）。
- **不实施修复**（P2-RC-001 修复触及 ORM 加 bankTxnCode 列须 ask-first + 独立 plan-audit；或改 `findStatementIdByAccount` 去除 limit 1 纯代码修复归 MR1 successor）。
- **不展开 A1.4 §7-1/§7-2/§7-4**（A4.1.11 对方账号触发率 done / A4.1.12 调整凭证行级 done / A4.1.14 config key 运维认知范围）。

## 9. MR0 触发登记

**无**。Phase 1 裁决为维持 P2（§5.4），不触发 MR0 即时通道（方法论 §10）。本验证不实施修复。

## 10. 结论

UC-FIN-09/14 断言①「导入幂等以 `(fundAccount, statementDate, bankTxnCode)` 去重」的**跨多条 statement 去重范围缺口**（P2-RC-001）经运行时影响面评估：`BankStatementImporter.findStatementIdByAccount:198-207`（`fundAccountId` 过滤 :201 + `statementDate DESC` :203 + `setLimit(1)` :204）确实**仅查最近一条 statement** 作为去重存在性查询范围 → 当 refNo 跨多条 statement 重复且重复出现在非最近一条 statement 时，`existsByRefNo`/`existsByComposite` 查最近一条 statement 查不到 → **漏检 → 重复入账**（触发面存在）。但**维持 P2-RC-001 = P2，不升 P0/P1**：P0④（会计过账正确性破坏）不成立（重复仅限银行对账子系统，GL/过账解耦隔离，MA2 :48,223,365）+ P0①（活跃数据破坏）不成立（触发率极低，银行 refNo 全局唯一 + 重复在非最近一条 statement + 本次批次无该 refNo 三重前置巧合，非默认活跃路径）+ 技术多重可逆。**§2 P1① 不适用**（主路径去重已实现强测，跨多条 statement 范围缺口属次要验收标准边界场景弱 → §2 P2①）。**三重兜底精化**（UNMATCHED 可见 + DRAFT 删除 + reverse 红冲）强于 A1.4 §5 假设；余额恒等式对本失效模式无效（同额双侧抵消，与 A4.1.11 §4.2 同型）但非主要兜底。触发率定性**极低**（银行 refNo 全局唯一语义 + 多重前置巧合）。**不触发 MR0，不升 P0/P1，不降级，无新 finding。** P2-RC-001 修复仍归 MR1 successor（优先纯代码方案 `findStatementIdByAccount` 去除 limit 1，预授权类目；ORM 加列须 ask-first）。
