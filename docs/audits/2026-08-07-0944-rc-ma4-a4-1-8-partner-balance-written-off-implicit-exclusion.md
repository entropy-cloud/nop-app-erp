# RC MA4 A4.1.8 — PartnerBalanceUpdater.sumOpen 对 WRITTEN_OFF 隐式排除 PARTIAL→WRITTEN_OFF 边界运行时核验

> Audit Status: closed
> 里程碑：MA4（运行时行为验证）
> 工作项：A4.1.8（MA4 运行时行为验证 — A1.3 §7-1：`PartnerBalanceUpdater.sumOpen` 对 WRITTEN_OFF 隐式排除的边界运行时核验，PARTIAL→WRITTEN_OFF 边缘场景）
> 输入：`docs/audits/2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` §7 存疑点 1（注意点②边界）+ §5.2 注意点②③（接受结论）
> 验证 plan：`docs/plans/2026-08-07-0944-2-rc-ma4-a4-1-8-partner-balance-written-off-implicit-exclusion.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据 + §7 衔接 + §8 自检 + §去重协议 + §9 冻结）
> 审计性质：**只读运行时核验**（grep sumOpen 排除点 + 读守卫链 + 读既有 JUnit 边界覆盖 + 复用 MA2/A1.3；不改代码/ORM/api.xml/真相源）
> 审计日期：2026-08-07
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

---

## 0. TL;DR（核验结论）

| 项 | 结果 |
|---|------|
| 存疑点 | A1.3 §7-1：`PartnerBalanceUpdater.sumOpen` 对 WRITTEN_OFF 的隐式排除——PARTIAL→WRITTEN_OFF 边界 sumOpen 余额正确性 |
| sumOpen 排除点（写时实测） | `PartnerBalanceUpdater.sumOpen:46-62` `notIn("status", [SETTLED, CANCELLED])` :51-52，**未显式排除 WRITTEN_OFF** |
| WRITTEN_OFF 贡献 0 机制（守卫链，写时实测） | `ErpFinBadDebtWriteOffProcessor.writeOff:22` **恒以 `item.getOpenAmountFunctional()`（全量剩余 open）作为坏账金额** → `executeWriteOff:168` open-=amount 使 open 恒归零 → `validateAmount:285-294` amount ≤ open 守卫兜底 |
| PARTIAL→WRITTEN_OFF 边界静态推理 | **成立**（守卫链比计划起草时认知更强：writeOff 入口恒取全量 openAmount，非仅 ≤ openAmount，故 open 经 open-=amount 恒归零） |
| 既有测试边界覆盖 | **缺口**：`TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL` 覆盖 OPEN→WRITTEN_OFF（断言 openAmount==0，未断言 partner.receivableBalance）；`TestErpFinPartnerBalance#testPayableBalanceDrivenByOpenAmount` 覆盖 PARTIAL（核销后）但未续坏账；grep PARTIAL+writeOff+receivableBalance 组合 **零命中** |
| 符合性结论（§2 判据） | **接受（边界行为正确）+ P2-RC-082（测试覆盖缺口，非行为缺陷）** |
| 新 finding | **1 项 P2 watch-only**（P2-RC-082：PARTIAL→WRITTEN_OFF→partner.receivableBalance 边界测试覆盖缺口） |
| P0 即时通道 | 不触发（未出 P0/P1） |

**核心裁决**：存疑点 PARTIAL→WRITTEN_OFF 边界的 **行为正确性确认性成立**（静态守卫链证实 WRITTEN_OFF 项 openAmount 恒归零 → sumOpen 隐式排除依赖的不变量成立），与 A1.3 §5.2 注意点②「接受」结论分层一致。但该**特定边界组合（PARTIAL 辅助账项执行 writeOff 后断言 partner.receivableBalance）运行时无单独测试覆盖**——按 plan 决策树分支②定 **P2（测试覆盖缺口，非行为缺陷）**，登记 P2-RC-082 successor（MR1 纯测试代码补强，预授权类目）。不变量失效条件枚举（若 any）：仅当未来代码变更使 writeOff 入口以非全量 openAmount 调用 `executeWriteOff`（amount < open）致 open 残留非 0 时，WRITTEN_OFF 项会贡献正余额致虚高——当前 writeOff 入口（`ErpFinBadDebtWriteOffProcessor:22` 恒取 `item.getOpenAmountFunctional()`）+ `validateAmount` 守卫共同保证此条件不可达。

---

## 1. 需求契约原文（§6 §1 / §1 L1，逐字引用）

**UC-FIN-08 收款核销发票**（`docs/design/finance/use-cases.md:147`）断言③（本验证核验对象的 L1 锚点）：

```
往来单位.应收余额 = Σ发票 - Σ核销 - Σ红字
```

状态轴（`use-cases.md:11`）逐字声明：

```
核销状态(erp-fin/ar-ap-status): OPEN(未核销) / PARTIAL(部分) / SETTLED(已核销) / CANCELLED(已作废) / WRITTEN_OFF(已坏账核销)
```

**隐含契约**：已坏账核销项（WRITTEN_OFF，openAmount=0）不应再计入应收余额（UC-FIN-08 断言③恒等式）。本验证聚焦该隐含契约在 **PARTIAL→WRITTEN_OFF 边界**（非全额核销后坏账）下经 `sumOpen` 隐式排除（依赖 openAmount=0 不变量）是否成立。

---

## 2. 实现证据（§6 §2 / §1 L3，写时实测）

### 2.1 隐式排除依赖链（Phase 1 item 1）

| 环节 | 文件:行（写时实测） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| sumOpen 查询路径 | `PartnerBalanceUpdater.java#sumOpen:46-62` | `q.addFilter(notIn("status", [SETTLED, CANCELLED])):51-52`——**仅显式排除 SETTLED/CANCELLED**；Σ `openAmountFunctional`（:56-59）；WRITTEN_OFF 项进入查询 | ✅（未显式排除 WRITTEN_OFF，依赖贡献 0） |
| WRITTEN_OFF 贡献 0 机制（金额变异） | `ErpFinBadDebtProcessor.java#executeWriteOff:163-182` | `validateAmount(amount, item):165` → `settled+=amount:166-167` → **`open-=amount:168-169`** → **`status=WRITTEN_OFF:170`** → 凭证:173-181 | ✅（open 经减 amount 变异） |
| **writeOff 入口金额恒取全量 open**（关键发现，比计划认知更强） | `ErpFinBadDebtWriteOffProcessor.java#writeOff:19-29` | `:22` `facade.newBadDebt(WRITE_OFF, item, item.getOpenAmountFunctional(), reason)`——**坏账金额 = item.openAmountFunctional（全量剩余 open）** | ✅（open-=amount 使 open 恒归零，非仅 ≤ openAmount） |
| 金额守卫 | `ErpFinBadDebtProcessor.java#validateAmount:285-294` | amount ≤ 0 → `ERR_BAD_DEBT_AMOUNT_INVALID:287`；amount > openAmountFunctional → `ERR_BAD_DEBT_AMOUNT_OVER_OPEN:289-292` | ✅（兜底守卫，amount ≤ open） |
| item 状态前置守卫 | `ErpFinBadDebtProcessor.java#requireOpenArApItem:255-271` | 仅 OPEN/PARTIAL 可进入 writeOff（:261-265）；open ≤ 0 拒绝（:266-269） | ✅（PARTIAL 可达 writeOff 入口） |

**静态推理链（PARTIAL→WRITTEN_OFF 边界）**：
1. PARTIAL 辅助账项：`status=PARTIAL`，`open = total − settled > 0`（如 total=1000, settled=400, open=600）
2. writeOff 入口（`ErpFinBadDebtWriteOffProcessor:22`）取 amount = `openAmountFunctional` = 600（全量剩余 open）
3. `executeWriteOff:168` open -= 600 → open = 600 − 600 = **0**
4. `executeWriteOff:170` status = WRITTEN_OFF
5. sumOpen 查询含该 WRITTEN_OFF 项（notIn 仅排除 SETTLED/CANCELLED），但其 openAmountFunctional = **0** → 贡献 0
6. → partner.receivableBalance 正确反映坏账（不计入已核销余额）

**结论**：静态守卫链证实「PARTIAL→WRITTEN_OFF 时 open 经 open-=amount 归零」成立。且**比计划起草时认知更强**——writeOff 入口恒以 `item.getOpenAmountFunctional()`（全量剩余 open）作为坏账金额（`:22`），非仅靠 `validateAmount`（amount ≤ open）守卫保证 amount 不超 open；故 open 经减法**恒归零**（amount 恒等于 open），不存在「amount < open 致 open 残留」的合法路径。

### 2.2 不变量失效条件枚举（若 any）

WRITTEN_OFF 项 openAmount 非 0 致余额虚高的**唯一可达路径** = 未来代码变更使 `executeWriteOff` 以 amount < openAmount 调用（致 open -= amount 后 open > 0 且 status=WRITTEN_OFF）。当前不可达：
- 唯一生产调用方 `ErpFinBadDebtWriteOffProcessor.writeOff:22` 恒取 `item.getOpenAmountFunctional()`（全量）
- `validateAmount:289` 守卫拒绝 amount > open（防超额）
- 无其他生产路径调用 `executeWriteOff`（grep 确认仅 `approveInternal:150` 经 docType=WRITE_OFF 分发，其上游仍经 writeOff 入口取全量 open）

→ 不变量当前不可被绕过。若未来新增「部分坏账」（amount < open 的 writeOff）需求，须同步在 sumOpen 显式排除 WRITTEN_OFF 或保证残留 open 的语义正确性——本验证登记此为 P2 successor 的回归守卫动机（见 §6）。

---

## 3. 测试证据（§6 §3 / §1 L4，断言强度标注）

### 3.1 既有测试边界覆盖核验（Phase 1 item 2）

grep `module-finance/erp-fin-service/src/test/java/` 坏账 + 核销 + partner balance 联合，确认是否存在「先部分核销（PARTIAL）再坏账（WRITTEN_OFF）后断言 partner.receivableBalance」的边界用例：

| 测试类#方法 | 覆盖场景 | 断言强度 | 是否覆盖 PARTIAL→WRITTEN_OFF→receivableBalance 边界 |
|---|---|---|---|
| `TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL:140` | **OPEN→WRITTEN_OFF**（全量坏账，amount=500=open），断言 status==WRITTEN_OFF / openAmount==0 / settledAmount==500 / 凭证无 6701 | **强** | ❌（OPEN 非 PARTIAL；**未断言 partner.receivableBalance**） |
| `TestErpFinPartnerBalance#testReceivableBalanceViaReconciliation:70` | 全额核销 OPEN→SETTLED，断言 receivableBalance==0 | **强** | ❌（SETTLED 非 WRITTEN_OFF） |
| `TestErpFinPartnerBalance#testPayableBalanceDrivenByOpenAmount:46` | 部分核销 OPEN→PARTIAL（200/1000），断言 payableBalance==800 | **强** | ❌（PARTIAL 但**未续坏账 writeOff**，且为 PAYABLE 非 RECEIVABLE） |
| `TestErpFinBadDebt#testRecoveryRestoresArApItem:179` | WRITTEN_OFF→OPEN（收回），断言 openAmount 恢复 | **强** | ❌（反向恢复，非本边界） |
| `TestErpFinBadDebt#testProvisionExcludesNegativeAndWrittenOff:114` | 坏账准备排除 WRITTEN_OFF（基线计算） | **强** | ❌（准备基线，非 sumOpen 余额） |

grep `PARTIAL.*writeOff|writeOff.*PARTIAL|receivableBalance.*writeOff|writeOff.*receivableBalance|部分核销.*坏账` 跨 `module-finance/erp-fin-service/src/test/java/` = **零命中**。

**边界覆盖判定**：**无该边界用例**。既有测试覆盖了边界的**组成片段**（writeOff 归零 openAmount / sumOpen=Σ openAmount / PARTIAL 核销后余额），但**未组合验证**「PARTIAL 项 → writeOff(剩余 open) → 断言 partner.receivableBalance」这一完整边界链路。

---

## 4. 运行时行为证据（§6 §4 / §1 L5）

### 4.1 MA2 复用（Phase 1 item 3，§去重协议）

| MA2 已证实行为 | 引用 | 本验证复用判定 |
|---|---|---|
| WRITTEN_OFF 经 executeWriteOff 可达（非死状态） | MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md` §1.2 + §2.1 矩阵（OPEN/PARTIAL→WRITTEN_OFF 迁移 PASS，守卫 requireOpenArApItem+validateAmount） | ✅ 复用（本验证只补 sumOpen 该边界余额差异，不重核状态机） |
| 辅助账 5 态全部可达（含 WRITTEN_OFF） | MA2 §1.2 | ✅ 复用（PARTIAL 可达 + WRITTEN_OFF 可达） |
| 期末门禁排除 WRITTEN_OFF | MA2 §1.4 + 控制点 7 | ✅ 复用（WRITTEN_OFF 一致排除 4 处之期末门禁行，A1.3 §5.2 注意点③ 已接受） |

**声明**：本验证只补「PARTIAL→WRITTEN_OFF 边界 sumOpen 余额正确性」差异（MA2 证实 WRITTEN_OFF 可达但未核该边界余额；A1.3 静态推理成立但未边界测试），不重新核实状态机行为。

### 4.2 A1.3 复用

A1.3 §5.2 注意点②（往来余额缓存字段）= **接受**（恒等式经 Σ openAmount 数学等价）+ §5.2 注意点③（WRITTEN_OFF 一致排除 4 处）= **接受**。本验证是注意点②③交叉的**边界场景**（PARTIAL→WRITTEN_OFF 后余额刷新是否正确），与 A1.3 接受结论**分层一致**（行为正确性确认 + 测试覆盖缺口登记）。

---

## 5. 符合性结论（§6 §5 / §2 判据 + 决策树三源对照）

### 5.1 决策树裁决（plan Phase 1 item 4）

| 决策分支 | 判据条件 | 本验证结果 | 命中 |
|---|---|---|---|
| ① 接受 | 静态守卫链证实 open 归零（边界成立）**+ 既有测试覆盖该边界** | 守卫链证实 open 归零 ✅；既有测试**未覆盖**该边界 ❌ | 否 |
| **② P2（测试覆盖缺口）** | 边界成立**但既有测试未覆盖该边界** | 边界成立 ✅ + 既有测试未覆盖 ✅ | **命中** |
| ③ P1（余额错误） | 不变量可被绕过（WRITTEN_OFF 项 openAmount 非 0 致余额虚高） | 当前不可达（writeOff 入口恒取全量 open + validateAmount 守卫） | 否 |

**裁决 = ② P2（测试覆盖缺口，非行为缺陷）**。

### 5.2 三源对照（L1/L2/L3）

- **L1**（`use-cases.md:147` UC-FIN-08 断言③ + `:11` 状态轴 WRITTEN_OFF）：已坏账核销项不应计入应收余额——PARTIAL→WRITTEN_OFF 边界下该隐含契约经 sumOpen 隐式排除成立。
- **L2**（`ar-ap-reconciliation.md §余额计算`）：Σ openAmount 实现恒等式；WRITTEN_OFF 项 openAmount=0 贡献 0。
- **L3**（`PartnerBalanceUpdater.sumOpen:51-52` + `ErpFinBadDebtWriteOffProcessor.writeOff:22` + `executeWriteOff:168`）：实现与 L1/L2 一致（边界行为正确）。

三源一致 → **边界行为正确性 = 接受**；**测试覆盖 = P2 缺口**。

### 5.3 与 A1.3 §5.2 注意点②③ 接受结论分层一致性

- A1.3 §5.2 注意点②（往来余额缓存字段）= 接受（恒等式成立）——本验证**确认**其边界（PARTIAL→WRITTEN_OFF）行为正确，不推翻接受。
- A1.3 §5.2 注意点③（WRITTEN_OFF 一致排除 4 处）= 接受——本验证**确认** sumOpen 该 1 处的隐式排除（依赖 openAmount=0 不变量）边界成立。
- 本验证**不升级** A1.3 接受结论（无 P0/P1）；仅补 P2 测试覆盖缺口（行为正确但无边界测试）。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 产出 finding 前 grep `arm-index.md` finance AR-AP 核销/坏账/往来余额同域同控制点。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本验证关系 | 裁决 |
|---|---|---|---|
| P2-MA2-039 `assertOpen` 不拒绝 WRITTEN_OFF（config-gated 隔离缺口） | 核销单 post 含 WRITTEN_OFF 项致状态机覆写（reconciliation 状态机隔离） | **不同控制点**（reconciliation settler 状态机隔离 vs partner balance sumOpen 隐式排除）；P2-MA2-039 的 config-gated 覆写不改变 openAmount=0 不变量（allow-over-reconcile=true 时覆写 status 但 openAmount 仍 0） | 不重开（不同维度） |
| A1.3 §5.2 注意点②（往来余额缓存字段，接受） | 余额恒等式数学等价 | 本验证是其**边界场景**（PARTIAL→WRITTEN_OFF），确认接受结论的边界成立 + 补测试缺口 | 复用（分层一致，不推翻） |
| A1.3 §5.2 注意点③（WRITTEN_OFF 一致排除 4 处，接受） | 4 处一致排除 | 本验证是 sumOpen 该 1 处的边界（隐式排除），确认成立 | 复用（分层一致） |

### 6.2 新建 finding 裁决

**1 项 P2 watch-only**（§7 规则 grep 后确认无同根因同控制点既有 finding）：

- **P2-RC-082**：PARTIAL→WRITTEN_OFF→partner.receivableBalance 边界测试覆盖缺口（行为正确经静态守卫链证实，但无运行时边界测试）。**新根因**（grep arm-index「PARTIAL WRITTEN_OFF 余额」「sumOpen 边界测试」「partner balance writeOff 边界」RC 系列零命中；与 P2-MA2-039 不同控制点[reconciliation 状态机隔离 vs sumOpen 余额隐式排除]）。

### 6.3 双向可追溯

- **新 finding → arm-index**：P2-RC-082 写入 arm-index MA4 分区（§Phase 2 落盘）。
- **successor 触发条件**：MR1（R1.0 展开为 RC-R1.n，纯测试代码修复预授权类目）——补边界测试：构造 PARTIAL 辅助账项执行 writeOff(剩余 open)，断言 partner.receivableBalance 正确反映 openAmount=0。
- **finding → MR1**：P2-RC-082 修复 = 纯测试代码（新增 TestErpFinPartnerBalance 或 TestErpFinBadDebt 边界用例），不触及 ORM/会计过账/数据迁移，按 roadmap 预授权类目自动执行。

---

## 7. 静态存疑点清单（§6 §7）

无。本验证是 MA4 运行时确认，存疑点 A1.3 §7-1 经守卫链核验 + 边界覆盖核验**正向消解**（边界行为正确 + 测试覆盖缺口已登记 P2-RC-082），无遗留运行时存疑点。

**P0 即时通道**：本验证 Phase 1 定级**未出 P0/P1**（边界行为接受 + P2 测试缺口），按 §10 **不触发 MR0/MR1 即时通道**（P2 登记不强制，successor 经 R1.0 展开）。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => `sys.exit(1)`。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本验证无生产代码变更**（只读核验：grep sumOpen + 读守卫链 + 读 JUnit + 引用 MA2/A1.3），checker 无回归风险。

  | 规则 | Baseline（`compliance-baseline.md §BASELINE (machine-readable)`） | Actual（本验证 HEAD 实测） | 状态 |
  |------|-------------------------------------------------------------------|----------------------------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 229 | 229 | ✅ |
  | R2c | 1382 | 1382 | ✅ |
  | R2d | 34 | 34 | ✅ |
  | R3-R12 | （既有基线） | 脚本输出在 R3 header 后截断（既有工具行为，与零代码变更的本验证无关；A4.1.1/A4.1 展开器报告同款记录） | ✅（无回归风险） |

  > R1/R2 全部 actual == baseline，**0 漂移**。R3-R12 脚本输出截断是既有工具行为（A4.1 展开器 `2026-08-07-0300-rc-ma4-a4-1-finance-expander.md` §6 同款记录）；权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 为准。本验证不触发 CI（零代码变更）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（1 项 P2-RC-082）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。交叉去重声明：与 A1.3 §5.2 注意点②③（接受）分层一致；与 MA2 A2.5c WRITTEN_OFF 一致排除（复用行为证据）；与 P2-MA2-039（assertOpen 不拒绝 WRITTEN_OFF，config-gated）不同控制点（reconciliation 状态机隔离 vs sumOpen 余额隐式排除）。

---

## 9. 真相源冻结声明（§9）

本验证未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc 需求契约段落）。只读核验（grep sumOpen + 读守卫链 + 读 JUnit + 引用 MA2/A1.3），未修改代码/ORM/api.xml/view.xml。

---

## 10. 与 MA2 报告差异增量声明（§去重协议）

本验证复用 MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md` §1.2（WRITTEN_OFF 经 executeWriteOff 可达 + 5 态完整）+ §2.1（辅助账迁移矩阵 OPEN/PARTIAL→WRITTEN_OFF PASS）+ §1.4（期末门禁排除 WRITTEN_OFF）已证实行为，**不重新核实状态机行为本身**。只补 MA2 未覆盖的「PARTIAL→WRITTEN_OFF 边界 sumOpen 余额正确性」差异——守卫链证实 open 恒归零（writeOff 入口恒取全量 openAmount）+ 边界测试覆盖缺口登记（P2-RC-082）。差异增量与本验证范围一致，无与 MA2 重叠的重新核实。
