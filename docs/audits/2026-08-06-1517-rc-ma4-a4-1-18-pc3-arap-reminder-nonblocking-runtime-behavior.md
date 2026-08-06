# RC MA4 A4.1.18 — PC-3 AR/AP reminder 非阻断模式运行时行为评估

> Audit Status: closed
> 里程碑：MA4（运行时行为验证）
> 工作项：A4.1.18（MA4 运行时行为验证 — A1.6 §7-1：UC-FIN-06 PC-3 AR/AP reminder 模式运行时行为——auto-post-on-close=true 提示模式下未核销 AR/AP 经 `hasReminders()` 列出但 closePeriod 不阻断，是否实际符合用户对「前置门禁」的期望，关联 P2-RC-006）
> 输入：`docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` §7 存疑点 1 + §5.3 P2-RC-006 + §2.3 PC-3 偏离 L1 + §6.1 P2-RC-006 与 P1-MA2-017 不同控制点
> 验证 plan：`docs/plans/2026-08-06-1517-3-rc-ma4-a4-1-18-pc3-arap-reminder-nonblocking-runtime-behavior.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据[含 P2① 次要验收标准] + §4 Q1 真相源层级与冲突裁决[Q1=(c) 分歧以 L1 为准] + §7 衔接 + §8 过程纪律自检 + §9 真相源冻结 + §去重协议）
> 范式对齐：A4.1.10（`docs/audits/2026-08-06-1044-rc-ma4-a4-1-10-auto-recon-config-gated-disabled-coverage.md`，done — config-gated 路径覆盖缺口评估同型工作项）
> 审计性质：**只读 reminder 模式运行时行为评估**（读 reminder/hasIssues 分流代码路径 + 强制核销模式 config 消费点普查 + 既有测试普查 + 引用 A1.6/A2.3；**不改代码/ORM/api.xml/真相源**）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）
> 审计 HEAD：`192aaf3e2`

---

## 0. TL;DR（核验结论）

| 项 | 结果 |
|---|------|
| 存疑点 | A1.6 §7-1：PC-3 AR/AP reminder 模式运行时行为——auto-post-on-close=true 提示模式下未核销 AR/AP 经 `hasReminders()` 列出但 closePeriod 不阻断——是否实际符合用户对「前置门禁」的期望，关联 P2-RC-006 |
| reminder/hasIssues 分流逻辑（写时实测） | `PeriodPreCheckReport.hasIssues():93-96` = `!unpostedVoucherCodes.isEmpty() \|\| !unresolvedPostingExceptionKeys.isEmpty()`（**不含未核销 AR/AP**）vs `hasReminders():102-105` = `!unsettledArApCodes.isEmpty() \|\| allowanceExcess > 0`（**含未核销 AR/AP**）；`ErpFinAccountingPeriodClosePeriodProcessor:59` `if (!facade.isAutoPostOnClose() && report.hasIssues())` 仅 `hasIssues()` 触发阻断，`hasReminders()` 永不阻断；:57 注释「未核销 AR-AP 为结构化提示（hasReminders），不阻断结账」 |
| 未核销 AR/AP 检出（写时实测） | `ErpFinAccountingPeriodProcessor.findUnsettledArApCodes:442-462`（按 `businessDate ∈ [start,end]` + orgId + 主账套 acctSchemaId 过滤，`status != SETTLED/CANCELLED/WRITTEN_OFF`）→ `PeriodPreCheckReport.unsettledArApCodes` |
| **强制核销模式 config 普查（闭合 P2-RC-006 关键变量）** | **不存在**——grep 全 `module-finance/` `force-settle\|mandatory-recon\|force-recon\|mandatory-settle\|force-settlement\|mandatory-settlement\|强制核销\|强制结算` 生产代码 = **零命中**（仅 docs 命中：use-cases.md L1 + audits/plans/arm-index）；`ErpFinConstants` 全 81 个 `CONFIG_*` 键无「强制核销模式」语义键；`ClosePeriodProcessor`/`findUnsettledArApCodes` 无任何 config-gated 切换 hard block 的消费点 |
| L1↔L2 冲突裁决（§4 Q1=(c) L1 为准） | L1 `use-cases.md:119`（PC-3，UC-FIN-06 heading :110）逐字「若 存在未核销应收应付(强制核销模式) → 拒绝」vs L2 `period-close.md:42-43`「未核销=提示」。按 §4 Q1 L1 为准，L2 推定已向实现妥协。**关键限定词「强制核销模式」活跃性裁决：config 不存在 → L1 限定条件不活跃 → 分歧倾向接受** |
| 前置门禁期望符合性 | reminder 模式提供运营提示（`unsettledArApCodes` 列出可见，向导 Step 1 结构化展示）；L1 PC-3 限定词「强制核销模式」未启用 → reminder 模式仅在非强制场景生效 → 符合「前置门禁」运营提示期望 |
| 测试覆盖边界（写时实测） | `TestErpFinPeriodPreCheck#testPreCheckListsIssues:47-67`（PC-3 检出断言 `unsettledArApCodes.size()==1` + code 字符串 `:65-66`，**深断言检出**）；`TestErpFinPeriodCloseEndToEnd#testFullChain:29-37`（auto-post-on-close=true 提示模式，reminder 不阻断——`:31` 仅断言 `unsettledArApCodes.size()>=1` 检出，`:36-37` 断言 closePeriod 成功 CLOSED，**reminder-不阻断经 closePeriod 成功间接证实，无显式 hard-block-非触发断言**）；强制核销模式 hard block 测试 = **缺口（config 不存在故无测试）** |
| P2-RC-006 决策（§2 判据 + §4 Q1 + 三源对照） | **维持 P2 watch-only（倾向接受）**——决策树分支①命中：强制核销模式 config 不存在 → L1 PC-3 限定条件「强制核销模式」不活跃 → reminder 模式仅非强制场景生效 + `unsettledArApCodes` 列出可见提供运营提示 → L1「强制核销模式→拒绝」活跃分歧不成立 → 维持 P2，不升 P1 |
| 新 finding | **0**（维持 P2-RC-006，无升级，无 successor 触发；arm-index P2-RC-006 行追加 A4.1.18 评估注记） |
| P0 即时通道 | 不触发（未出 P0/P1） |

**核心裁决**：存疑点 A1.6 §7-1 的 PC-3 AR/AP reminder 模式运行时行为评估结论 = **维持 P2-RC-006（P2 watch-only，倾向接受）**。判据三层（决策树分支①）：(1) **强制核销模式 config 不存在**——本报告 §2.2 主证据（grep census + `ErpFinConstants` 全 81 键普查 + `ClosePeriodProcessor`/`findUnsettledArApCodes` 消费点普查）独立核验：L1 PC-3 关键限定词「强制核销模式」无对应生产 config，grep 全 `module-finance/` `force-settle\|mandatory-recon\|...` 生产代码零命中，仅 docs 命中；(2) **L1 PC-3 限定条件不活跃**——按 §4 Q1=(c) L1 为准，L1 逐字「若 存在未核销应收应付(**强制核销模式**) → 拒绝」，限定词「强制核销模式」是分歧活跃性的 hinge：config 不存在 → 限定条件永不满足 → L1「强制核销模式→拒绝」字面要求在当前实现中**无可达触发条件** → 分歧倾向接受（A1.6 §5.3 倾向接受结论确认）；(3) **reminder 模式提供运营提示**——未核销 AR/AP 经 `findUnsettledArApCodes:442` 检出 → `hasReminders():102` 列出 → `PeriodPreCheckReport.unsettledArApCodes` 经向导 Step 1 `preCheck` 结构化展示（`period-close.md §期末结账向导 :413`），用户可见可据此运营决策，符合「前置门禁」运营提示期望。按 plan 决策树分支①裁决（强制核销模式 config 不存在 + reminder 模式仅非强制场景生效 + unsettledArApCodes 列出可见 → P2-RC-006 维持 P2 watch-only）。A1.6 §7 存疑点 1 经本评估**正向消解为接受（P2 维持）**，无遗留运行时存疑点。修复（实现 config-gated 强制核销模式 hard block 或 owner doc 标注）归 MR1 预授权类目，**不触发 §5 ask-first**，归 plan §Deferred But Adjudicated successor。**本验证不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（§6 §1 / §1 L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md`（L1 权威真相源，方法论 §4）。验收标准逐字引用，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。本验证只评 PC-3，引用 A1.6 §1 UC-FIN-06 完整枚举。

**UC-FIN-06 期末结账前置门禁**（`use-cases.md:110`）PC-3 逐字（A1.6 §1 :56）：

```
若 存在未核销应收应付(强制核销模式) → 拒绝
```

**关键限定词「强制核销模式」**：L1 PC-3 字面要求「**强制核销模式**启用时」未核销 AR/AP 应**拒绝**结账（hard block），非无条件「提示」。本限定词是 P2-RC-006 分歧活跃性的核心 hinge——若「强制核销模式」config 存在且启用但 reminder 仍不阻断 → L1 限定条件活跃 → 须升 P1 实现 hard block；若 config 不存在或默认未启用 → L1 限定条件不活跃 → 分歧倾向接受（reminder 模式仅在非强制场景生效）。本验证 §2.2 普查确认该 config 的存在性/启用状态/消费点。

**L2 设计参考**（`period-close.md §结账前置检查 :42-43`）：「检查本期是否有未核销的应收应付 → 查询应收应付核销状态（status≠SETTLED/CANCELLED/WRITTEN_OFF）→ **提示：建议结账前完成核销**」——L2 处理为「提示」非「拒绝」，与 L1 PC-3 字面「拒绝」冲突。按 §4 Q1=(c) **L1 为准**，L2 推定已向实现妥协（A1.6 §4 注记）。

---

## 2. 实现证据（§6 §2 / §1 L3，写时实测 `192aaf3e2`）

### 2.1 reminder/hasIssues 分流逻辑核验（Phase 1 item 1）

> 核验目标：证实未核销 AR/AP 经 `hasReminders()` 列出但 closePeriod 不阻断的运行时行为（A1.6 §2.3 静态确认的运行时复核实）。

| 环节 | 文件:行（写时实测） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| 未核销 AR/AP 检出 | `module-finance/erp-fin-service/.../service/processor/ErpFinAccountingPeriodProcessor.java#findUnsettledArApCodes:442-462` | `:445` 按 `businessDate ∈ [start,end]` + `:448-453` orgId + 主账套 acctSchemaId 过滤（多公司/多账套读路径隔离 P1-MA2-095）；`:455-461` filter `status != SETTLED/CANCELLED/WRITTEN_OFF` → 列号清单写入 `PeriodPreCheckReport.unsettledArApCodes` | ✅ |
| 阻断分流：hasIssues（不含未核销 AR/AP） | `module-finance/erp-fin-dao/.../dao/PeriodPreCheckReport.java#hasIssues:93-96` | `:94-95` `return !unpostedVoucherCodes.isEmpty() \|\| !unresolvedPostingExceptionKeys.isEmpty();`——**仅未过账凭证 + 未处置过账异常，不含未核销 AR/AP**；`:89-91` javadoc「不含未核销 AR-AP 提示——未核销=结构化提示非阻断」 | ✅ |
| 提示分流：hasReminders（含未核销 AR/AP） | `module-finance/erp-fin-dao/.../dao/PeriodPreCheckReport.java#hasReminders:102-105` | `:103-104` `return !unsettledArApCodes.isEmpty() \|\| allowanceExcess.compareTo(ZERO) > 0;`——**含未核销 AR/AP + Allowance 超额**；`:98-100` javadoc「非阻断的结构化提示项（未核销 AR-AP + Allowance 超额），列出供用户参考但不阻止结账」 | ✅ |
| closePeriod 阻断条件 | `module-finance/erp-fin-service/.../service/processor/ErpFinAccountingPeriodClosePeriodProcessor.java#doClosePeriod:59` | `:59` `if (!facade.isAutoPostOnClose() && report.hasIssues())` → `:60-62` `throw new NopException(ERR_PRE_CHECK_BLOCKED)`——**仅 `hasIssues()` 触发阻断，`hasReminders()` 永不阻断**；`:57` 注释「未核销 AR-AP 为结构化提示（hasReminders），不阻断结账」 | ✅ |
| Allowance shortfall 独立硬阻断 | `ErpFinAccountingPeriodClosePeriodProcessor.java:52-56` | `:52` `if (report.hasAllowanceShortfall())` → 抛 `ERR_PRE_CHECK_BLOCKED`（独立于 auto-post-on-close，bad-debt.md shortfall 门控）——本验证范围外，记此以示分流完整 | ✅（旁证分流逻辑边界清晰） |

**reminder/hasIssues 分流逻辑核验结论**：未核销 AR/AP 经 `findUnsettledArApCodes:442` 检出 → `hasReminders():102`（**非 `hasIssues():93`**）→ `ClosePeriodProcessor:59` 仅 `hasIssues()` 触发阻断，`hasReminders()` 永不阻断——证实未核销 AR/AP 经 reminder 列出但 closePeriod 不阻断的运行时行为。A1.6 §2.3 静态确认经 HEAD `192aaf3e2` 复核**无回退**（行号与 A1.6 §2.3 / plan Current Baseline 精确一致：findUnsettledArApCodes:442 / hasIssues():93-96 不含未核销 AR/AP / hasReminders():102-105 含 / ClosePeriodProcessor:59 仅 hasIssues 阻断 + :57 注释）。

### 2.2 强制核销模式 config 消费点普查（Phase 1 item 2，闭合 P2-RC-006 关键变量）

> 核验目标：grep「强制核销模式」相关 config 全集 + 默认值 + 是否被 `ClosePeriodProcessor`/`ErpFinAccountingPeriodProcessor`/`findUnsettledArApCodes` 消费切换为 hard block——确认 L1 PC-3 限定词「强制核销模式」是否实际存在 config + 默认是否启用 + 是否驱动 hasIssues（而非 hasReminders）。**这是 P2-RC-006 决策闭合的核心证据。**

#### 2.2.1 config key 全集普查

| 普查维度 | grep pattern（写时实测） | 命中（生产代码） | 裁决 |
|---|---|---|---|
| 强制核销模式 config 字面变体（生产代码） | `rg 'force-settle\|mandatory-recon\|force-recon\|mandatory-settle\|force-settlement\|mandatory-settlement' module-finance/` | **0** | 无「强制核销模式」config 键 |
| 中文限定词（生产代码） | `rg '强制核销\|强制结算' module-finance/`（排除 docs/） | **0**（全 46 命中均在 `docs/`：use-cases.md L1 + audits/plans/arm-index，生产代码零命中） | 「强制核销模式」是 L1 需求契约措辞，无生产 config 实现 |
| `ErpFinConstants` 全 CONFIG_* 键普查 | `rg 'CONFIG_' module-finance/erp-fin-service/.../ErpFinConstants.java`（81 命中） | 与核销/对账相关的 key 仅：`erp-fin.reconcile-precision`/`allow-over-reconcile`/`auto-reconcile`/`auto-recon-strategy`/`ar-ap-auto-recon-cron`/`auxiliary-recon-gate-enabled`/`bank-recon-*`/`recon-fx-gain-loss-enabled`——**无「强制核销模式」（mandatory settlement）语义键** | 确认无「强制核销模式」config |

#### 2.2.2 消费点普查（ClosePeriodProcessor / findUnsettledArApCodes 是否 config-gated 切换 hard block）

| 消费点 | 文件:行（写时实测） | 是否存在「强制核销模式」config-gated 切换 hard block | 裁决 |
|---|---|---|---|
| `findUnsettledArApCodes` | `ErpFinAccountingPeriodProcessor.java:442-462` | **否**——方法体无条件按 `status != SETTLED/CANCELLED/WRITTEN_OFF` 过滤，无任何 config 读取（无 `AppConfig.var(...)`）切换「强制核销模式→纳入 hasIssues」 | 无 config-gated hard block 路径 |
| `ClosePeriodProcessor.doClosePeriod` 阻断分支 | `ErpFinAccountingPeriodClosePeriodProcessor.java:52-63` | **否**——阻断分支仅三处：`:52` `hasAllowanceShortfall()`（独立硬阻断）+ `:59` `!isAutoPostOnClose() && hasIssues()`（未过账凭证/异常阻断）；**无「强制核销模式启用时 unsettledArApCodes 纳入 hasIssues」分支** | 无 config-gated 强制核销 hard block |
| `ErpFinAccountingPeriodProcessor` config 消费方法簇 | `:633-692`（isAutoPostOnClose / isAutoDepreciationOnClose / isInvCostingRecloseOnClose / isExchangeRevaluationEnabled / isReverseCloseApprovalRequired / isAllowanceGateEnabled / isAnnualCloseEnabled / isPeriodGenerateSkipExisting / isBankFxRevaluationEnabled / isAutoGenerateNextYearPeriods / isAuxiliaryReconGateEnabled） | **否**——11 个 config 消费方法中**无「强制核销模式」消费点**（`isAuxiliaryReconGateEnabled:689-692` 是「辅助账跨年对账门控」，语义≠强制核销） | 无强制核销 config 消费 |

**强制核销模式 config 普查结论（闭合 P2-RC-006 关键变量）**：L1 PC-3 限定词「强制核销模式」**无对应生产 config**——(a) grep 全 `module-finance/` 字面变体 + 中文限定词生产代码零命中；(b) `ErpFinConstants` 全 81 个 `CONFIG_*` 键无「强制核销模式」语义键；(c) `ClosePeriodProcessor`/`findUnsettledArApCodes`/`ErpFinAccountingPeriodProcessor` config 消费方法簇无任何 config-gated 切换 hard block 的消费点。**「强制核销模式」是 L1 需求契约措辞，当前实现未提供对应 config 切换路径**——故 L1 PC-3「强制核销模式→拒绝」的限定条件**永不满足（不活跃）**，reminder 模式在所有当前可达配置下生效。

### 2.3 L1↔L2 冲突裁决（Phase 1 item 3，§4 Q1=(c) L1 为准 + 限定词活跃性）

| 真相源 | 锚点（写时实测） | 字面要求 | 裁决 |
|---|---|---|---|
| **L1**（功能契约，权威） | `use-cases.md:119`（PC-3，UC-FIN-06 heading :110） | 「若 存在未核销应收应付(**强制核销模式**) → 拒绝」——**强制核销模式启用时 hard block** | L1 为准（§4 Q1=(c)） |
| **L2**（设计参考，非真相源） | `period-close.md:42-43` | 「未核销=提示：建议结账前完成核销」——**提示非拒绝** | L2 推定已向实现妥协（§4 Q1） |
| **L3**（实现现状） | §2.1 + §2.2 | 未核销 AR/AP → `hasReminders()`（非 `hasIssues()`）→ closePeriod 不阻断；**无「强制核销模式」config** | 实现 = L2「提示」语义 |

**L1↔L2 冲突裁决**：按 §4 Q1=(c) **L1 为准**，L2「提示」推定已向实现妥协。**关键限定词「强制核销模式」活跃性裁决**（本验证核心增量）：

- **config 不存在（§2.2 证实）→ L1 PC-3 限定条件「强制核销模式」不活跃**——L1 字面要求是「**强制核销模式启用时**未核销 AR/AP 应拒绝」，限定词「强制核销模式」是分歧活跃性的 hinge。当前实现无此 config → 限定条件永不满足 → L1「强制核销模式→拒绝」字面要求**在当前实现中无可达触发条件** → 分歧倾向接受（reminder 模式仅在非强制场景生效，与 L1 非强制场景无字面要求一致）。
- **若 config 存在且启用但 reminder 仍不阻断 → L1 限定条件活跃 → 须升 P1**（决策树分支②）——本验证 §2.2 证伪此分支（config 不存在），故分支②不匹配。

**三源对照结论**：L1 PC-3 限定词「强制核销模式」config 不存在 → L1 限定条件不活跃 → L1↔L2 字面冲突（「拒绝」vs「提示」）在当前实现中**无可达活跃分歧** → 倾向接受（与 A1.6 §5.3 P2-RC-006 倾向接受结论一致）。

### 2.4 reminder 模式与「前置门禁」期望符合性评估（Phase 1 item 4）

| 评估维度 | 证据（写时实测） | 评估结论 |
|---|---|---|
| 未核销 AR/AP 列出可见 | `findUnsettledArApCodes:442-462` → `PeriodPreCheckReport.unsettledArApCodes` → 经向导 Step 1 `preCheck` 结构化展示（`period-close.md §期末结账向导 :413`「Step 1 选择期间 + 前置检查 → 结构化展示 PeriodPreCheckReport（unpostedVoucherCodes/unsettledArApCodes/...）；阻断项红色高亮，禁用继续」） | ✅ reminder 提供运营提示（用户可见 unsettledArApCodes 清单） |
| auto-post-on-close=true 提示模式下大额未核销 AR/AP 不阻断 | `ClosePeriodProcessor:59` 仅 `hasIssues()` 阻断，`hasReminders()` 永不阻断（无论 auto-post-on-close 取值）；`TestErpFinPeriodCloseEndToEnd#testFullChain:29-37` 实测 auto-post-on-close=true 提示模式 + 未核销 AR `unsettledArApCodes.size()>=1` → closePeriod 成功 CLOSED | ✅ 提示模式下 reminder 不阻断，但列出可见（运营提示达成） |
| 强制核销模式启用路径可达性 | §2.2 证实「强制核销模式」config 不存在 → 强制核销模式启用路径**不可达**；config-gated 切换 hard block 缺失面 = L1 限定条件不活跃的直接结果（非实现缺陷，而是限定条件本身未物化为 config） | ⚠ config-gated 强制核销 hard block 缺失（归 MR1 successor，若未来产品化要求强制核销模式须补 config + hard block 分支） |

**前置门禁期望符合性结论**：auto-post-on-close=true 提示模式下大额未核销 AR/AP 不阻断 closePeriod，但 `unsettledArApCodes` 经向导 Step 1 结构化展示**列出可见**，提供足够运营提示供用户决策（符合「前置门禁」运营提示期望）。L1 PC-3 限定词「强制核销模式」未启用（config 不存在）→ reminder 模式仅在非强制场景生效 → 与 L1 非强制场景无字面要求一致。**config-gated 强制核销模式 hard block 缺失**是 L1 限定条件未物化为 config 的直接结果，归 MR1 successor（若未来产品化要求强制核销模式，须补 config + `findUnsettledArApCodes` 结果纳入 `hasIssues()` 触发 hard block 分支），非当前实现缺陷。

### 2.5 MA4↔A5.6 边界声明（Phase 1 item 5）

> 方法论 §去重协议 MA4↔A5.6 边界：MA4 审「行为是否符合需求」（需求契约视角，reminder 模式是否符合 L1 前置门禁）；A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角，全量评级）。

**本验证边界执行声明**：

- 本验证审「PC-3 reminder 模式是否符合 UC-FIN-06「前置门禁」期望」——**需求契约视角**。裁决依据 = §2 判据（L1 PC-3 限定词活跃性 + 强制核销模式 config 存在性 + reminder 提示是否足够）。
- 本验证**不重做 A5.6 E2E 断言强度审计**（A5.6 已对全量 spec 做断言强度分类矩阵）。本验证只评 PC-3 reminder 模式运行时行为这一具体控制点。
- 裁决为「维持 P2 watch-only」→ 无新 finding；reminder-不阻断显式断言缺口（§3）属 A5.6 测试质量维度 successor（纯测试代码 MR1 预授权类目），**非本 MA4 范围**。

---

## 3. 测试证据（§6 §3 / §1 L4，断言强度标注）

### 3.1 测试覆盖边界普查（Phase 1 item 6）

> grep `TestErpFinPeriodPreCheck#testPreCheckListsIssues`（PC-3 检出断言）+ `TestErpFinPeriodCloseEndToEnd#testFullChain`（reminder 不阻断断言缺口）+ 强制核销模式 config 启用场景测试全集。引用 A1.6 §3 已有评级依据。

| 测试方法 | 文件:行（写时实测） | 覆盖范围 | 断言强度 | PC-3 reminder 相关覆盖 |
|---|---|---|---|---|
| `testPreCheckListsIssues` | `TestErpFinPeriodPreCheck.java:47-67` | PC-1（1 张未过账凭证 `V-DRAFT-002`）+ PC-3（1 笔未核销 AR `ARI-OPEN-001`）检出 | **深**（`:65` `assertEquals(1, report.getUnsettledArApCodes().size())` + `:66` `assertEquals("ARI-OPEN-001", ...)` 精确断言检出 + code 字符串） | ✅ **PC-3 检出覆盖**（unsettledArApCodes.size()==1 + code 字符串） |
| `testPreCheckCleanPeriod` | `TestErpFinPeriodPreCheck.java:69-78` | 干净期间 `hasIssues()` = false | **深** | —（无未核销 AR/AP） |
| `testBlockingCloseRejectsWithIssues` | `TestErpFinPeriodPreCheck.java:80-97` | PC-1 hard block（默认 auto-post-on-close=false：未过账凭证 → `assertThrows(NopException.class, ... closePeriod)` + 状态保持 OPEN） | **深**（异常 + 状态守卫） | —（PC-1 阻断，无未核销 AR/AP） |
| `testFullChain` | `TestErpFinPeriodCloseEndToEnd.java:25-66` | 全链 preCheck（unsettledArApCodes 列出）→ closePeriod（CLOSED）→ FX-REVAL + PERIOD-CLOSE 凭证 → finalizePeriod → reverseClose → re-close | **深**（状态 + 凭证 + 模块 status + 计数） | ⚠ **reminder-不阻断经 closePeriod 成功间接证实**——`:31` `assertTrue(report.getUnsettledArApCodes().size() >= 1, "前置检查列出未核销外币应收")`（检出）+ `:36-37` `closePeriod` 成功 `assertEquals(PERIOD_STATUS_CLOSED, ...)`（reminder 不阻断经成功间接证实）；**无显式 hard-block-非触发断言**（如 `assertFalse(report.hasIssues())` 断言未核销 AR/AP 不计入 hasIssues，或显式断言「reminder 存在但 closePeriod 不抛异常」） |
| 强制核销模式 config 启用场景测试 | grep 全 `module-finance/erp-fin-service/src/test` `force-settle\|mandatory-recon\|强制核销\|强制结算` | **0 命中** | — | ❌ **缺口**（config 不存在故无测试，§2.2 一致） |

**测试覆盖边界清单**：

1. **PC-3 检出覆盖** ✅——`testPreCheckListsIssues:65-66` 深断言 `unsettledArApCodes.size()==1` + code 字符串（A1.6 §4.4 已记）。
2. **reminder-不阻断显式断言缺口** ⚠——`testFullChain:29-37` 仅断言检出（`:31`）+ closePeriod 成功（`:36-37`），reminder 不阻断经 closePeriod 成功**间接证实**，但无显式 hard-block-非触发断言（如 `assertFalse(report.hasIssues())` 或「reminder 存在但 assertDoesNotThrow closePeriod」）。A1.6 §4.4 已记「仅断言检出未断言不阻断」。
3. **强制核销模式 hard block 测试缺口** ❌——「强制核销模式」config 不存在（§2.2）故无对应测试；若未来实现 config-gated hard block（MR1 successor），须补 config 启用场景测试（`assertThrows` closePeriod + 未核销 AR/AP）。

**断言强度评级**：PC-3 reminder 模式测试覆盖 = **检出深 + 不阻断间接证实 + 显式 hard-block-非触发断言缺口**。reminder-不阻断经 `testFullChain:36-37` closePeriod 成功间接证实（未核销 AR 存在 + closePeriod 成功 = reminder 不阻断的运行时证据），功能行为正确；显式断言缺口属测试覆盖补强项（A5.6 维度），非合规缺陷（reminder 模式运行时行为已由间接证据证实）。

---

## 4. 运行时行为证据（§6 §4 / §1 L5）

### 4.1 MA2/A1.6 复用（§去重协议）

| 已证实行为 | 引用 | 本验证复用判定 |
|---|---|---|
| reminder/hasIssues 分流行为（P1-MA2-017 resolved 阻断分级重构） | A1.6 §2.3 + §6.2 P1-MA2-017 HEAD 复核 + A2.3 period-close E2E `2026-07-27-1949-arm-ma2-period-close-e2e.md` | ✅ 复用（`hasIssues()` 排除未核销 AR-AP + 新增 `hasReminders()` 分流行为已证实）；本验证只补「强制核销模式 config 存在性 + reminder 模式运行时前置门禁期望符合性」差异 |
| PC-3 reminder 偏离 L1 字面「拒绝」（P2-RC-006 倾向接受） | A1.6 §5.3 + §6.1 P2-RC-006 新建 | ✅ 复用（静态确认 + 倾向接受结论）；本验证闭合决策（确认维持 P2 vs 升 P1） |

**声明**：本验证只补「强制核销模式 config 存在性/启用状态/消费点 census + reminder 模式运行时前置门禁期望符合性 + P2-RC-006 决策闭合」差异（A1.6 §7 存疑点 1 标注为「需运行时确认」），不重新核实 reminder/hasIssues 分流行为本身（A1.6 §2.3 + A2.3 已证实）。

### 4.2 本切片运行时行为增量

本验证相对 A1.6/A2.3 的**运行时行为增量**：

1. **强制核销模式 config census**（A1.6 §7 存疑点 1 触发条件「实际启用强制核销模式（未文档化 config）」未核实）：§2.2 grep census 证实 config **不存在**——触发条件「实际启用强制核销模式」**不可达**，存疑点触发条件本身不成立。
2. **reminder 模式前置门禁期望符合性**（A1.6 §7 存疑点 1「是否符合用户对前置门禁的期望」未评估）：§2.4 评估 reminder 模式提供运营提示（unsettledArApCodes 列出可见）+ 强制核销模式 config 未启用 → 符合「前置门禁」运营提示期望。
3. **P2-RC-006 决策闭合**（A1.6 §5.3 倾向接受未闭合）：§5.1 按决策树分支①裁决维持 P2 watch-only。

---

## 5. 符合性结论（§6 §5 / §2 判据 + 三源对照）

### 5.1 P2-RC-006 决策闭合（Phase 1 item 7，方法论 §2 判据 + §4 Q1 + plan 决策树两分支）

| 决策分支 | 判据条件（plan Phase 1 item 7） | 本验证结果 | 命中 |
|---|---|---|---|
| **① 维持 P2 watch-only（倾向接受）** | 强制核销模式 config 未启用 **且** reminder 模式仅非强制场景生效 **且** unsettledArApCodes 列出可见 → P2-RC-006 维持 P2（L1 限定条件「强制核销模式」不活跃 + reminder 提供运营提示） | (a) 强制核销模式 config **不存在**（§2.2 grep census + ErpFinConstants 全 81 键普查 + 消费点普查三重证实）→ L1 限定条件「强制核销模式」**不活跃**（永不满足）✅；(b) reminder 模式仅非强制场景生效（强制核销模式 config 不存在 → 无强制场景可达路径）✅；(c) unsettledArApCodes 列出可见（§2.4 `findUnsettledArApCodes:442` → 向导 Step 1 结构化展示）✅；(d) reminder 提供运营提示（用户可见清单供决策）✅ | **命中** |
| ② 升 P1（须实现 hard block） | 强制核销模式 config 已启用但 reminder 仍不阻断 → L1「强制核销模式→拒绝」活跃分歧，须实现 hard block（§2 P1① 行为实质偏离） | 强制核销模式 config **不存在**（§2.2）→ 「config 已启用」条件**不成立** → L1 限定条件不活跃 → 无活跃分歧 | 否 |

**裁决 = ① 维持 P2-RC-006（P2 watch-only，倾向接受）**。

> **裁决理由（决策树两分支的关键区分）**：plan 决策树分支②的 P1 升级须满足「强制核销模式 config 已启用但 reminder 仍不阻断」——即 L1 PC-3 限定词「强制核销模式」须**实际存在 config 且已启用**，方使 L1「强制核销模式→拒绝」字面要求成为活跃分歧。本验证 §2.2 grep census 三重证实「强制核销模式」config **不存在**（字面变体生产代码零命中 + ErpFinConstants 全 81 键无语义键 + ClosePeriodProcessor/findUnsettledArApCodes/Processor config 消费簇无消费点）→ L1 限定条件「强制核销模式」**永不满足（不活跃）** → L1「强制核销模式→拒绝」字面要求在当前实现中**无可达触发条件** → 分支②「config 已启用」条件不成立，分支①匹配。**强制核销模式 config 不存在 = L1 限定条件不活跃 = 分歧倾向接受的核心 hinge**。与 A1.6 §5.3 P2-RC-006 倾向接受结论（「L2 已记录有意设计 + 强制核销模式 config 默认未启用」）分层一致——本验证进一步精确化：config 不仅「默认未启用」而是「不存在」，倾向接受结论更强（限定条件不仅默认不活跃，且无 config 路径可激活）。

### 5.2 §2 判据编号 + 三源 + 分层一致性

- **§2 判据**：P2①（次要验收标准未完全满足——主路径[未过账凭证/坏账缺口 hard block + auto-post-on-close=false 默认阻断模式]OK，边界[reminder 模式 + 强制核销模式 config 缺失]弱；L2 owner doc `period-close.md:42-43` 已记录有意设计「未核销=提示」）。
- **§4 Q1**：L1 为准（Q1=(c) 逐项对照，分歧以 L1 为准）；L1↔L2 字面冲突（「拒绝」vs「提示」）经限定词「强制核销模式」活跃性裁决——config 不存在 → L1 限定条件不活跃 → 分歧倾向接受。
- **L1/L2/L3 三源**：L1 `use-cases.md:119`（强制核销模式→拒绝）/ L2 `period-close.md:42-43`（未核销=提示）/ L3 §2.1+§2.2（hasReminders 非 hasIssues + 无强制核销 config）。
- **与 A1.6 §5.3 P2-RC-006 倾向接受分层一致**：A1.6 §5.3 倾向接受理由「L2 已记录有意设计 + 强制核销模式 config 默认未启用」——本验证精确化为「config 不存在」（更强），倾向接受结论确认。
- **与 A1.6 §6.1 P1-MA2-017（不同控制点）分层一致**：P1-MA2-017 = doc↔code 阻断分级重构 + 默认值文本一致性（audit-remediation 视角，已 resolved）；P2-RC-006 = L1↔L2 字面契约冲突（需求契约视角，L1「强制核销模式→拒绝」vs L2「未核销=提示」）。同一代码站点不同审计轴，A1.6 §6.1 已裁决不合并——本验证维持此分层，不重开 P1-MA2-017。
- **与 A2.3 period-close E2E 分层一致**：A2.3 证实 reminder/hasIssues 分流行为（P1-MA2-017 resolved）+ period-close 全链路行为正确；本验证只补需求契约视角的 PC-3 限定词活跃性裁决，不重审行为。

### 5.3 与同族 A4.1 工作项裁决分层对照

| 同族工作项 | 存疑点性质 | 裁决 | 与本验证（A4.1.18）区分 |
|---|---|---|---|
| A4.1.10（done） | config-gated 禁用路径测试覆盖缺口（auto-reconcile feature gate） | 接受（运营开关非 L1 验收标准） | A4.1.10 是 feature gate 覆盖缺口（运营维度）→ 接受；A4.1.18 是 L1 限定词活跃性裁决（需求契约维度）→ 维持 P2。同属 config-gated 相关评估，但控制点不同（feature gate 覆盖 vs L1 限定词活跃性） |

**分层一致**：A4.1.18 与 A4.1.10 同属 A4.1 MA4 运行时行为验证族，结论差异源于控制点性质（L1 限定词活跃性 vs feature gate 覆盖），无矛盾。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 产出 finding 前 grep `arm-index.md` finance PC-3 / reminder / 强制核销模式同域同控制点。本验证裁决 = 维持 P2-RC-006，**产出 0 项新 finding**，**仅追加 A4.1.18 评估注记**。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本验证关系 | 裁决 |
|---|---|---|---|
| **P2-RC-006**（arm-index :137）UC-FIN-06③ PC-3 AR/AP reminder 偏离 L1 | 未核销 AR/AP 实现为 reminder 非 L1 字面「拒绝」 | 本验证是其**运行时行为评估 + 决策闭合**（强制核销模式 config census + 限定词活跃性裁决 + 维持 P2 倾向接受） | **维持 + 追加注记**（§6.2） |
| P1-MA2-017（arm-index，resolved）auto-post-on-close 阻断分级重构 | doc↔code 阻断分级 + 默认值对齐 | **不同控制点/不同维度**（audit-remediation 文本一致性视角 vs 需求契约视角）；已 resolved | 不重开（不同控制点，A1.6 §6.1 已裁决） |
| A1.6 §5.1 UC-FIN-06 整体 P2（PC-3 偏离 L1） | UC-FIN-06 符合性 | 本验证是其**PC-3 reminder 模式运行时行为差异**，确认 P2 维持 | 复用（分层一致，确认维持） |
| A2.3 period-close E2E（P1-MA2-017 resolved） | reminder/hasIssues 分流行为 | 本验证复用其已证实分流行为，不重审 | 复用（§去重协议） |

grep `arm-index.md` 「强制核销模式」「force-settle」「mandatory-recon」「PC-3 reminder 运行时」RC 系列 = **零新控制点命中**（P2-RC-006 是唯一覆盖 PC-3 reminder 的 finding）。

### 6.2 P2-RC-006 分级注记更新（Phase 2 item 1）

**裁决**：**维持 P2-RC-006（P2 watch-only），追加 A4.1.18 评估注记**。在 arm-index P2-RC-006 行追加注记：「**【A4.1.18 运行时行为评估 2026-08-06】** 经强制核销模式 config census（grep 全 module-finance/ 字面变体 + ErpFinConstants 全 81 键 + 消费点三重普查 = 零命中）+ §4 Q1 L1 限定词活跃性裁决，确认 P2 维持：强制核销模式 config **不存在** → L1 PC-3 限定词「强制核销模式」**不活跃**（永不满足）→ L1「强制核销模式→拒绝」字面要求无可达触发条件 → 分歧倾向接受（A1.6 §5.3 倾向接受结论确认并精确化：config 不仅默认未启用，而是不存在）。reminder 模式提供运营提示（unsettledArApCodes 经向导 Step 1 结构化展示列出可见）。决策树分支①命中，不升 P1。详见 `docs/audits/2026-08-06-1517-rc-ma4-a4-1-18-pc3-arap-reminder-nonblocking-runtime-behavior.md`。」状态/分级/修复通道**不变**（successor watch-only，修复归 MR1 预授权类目）。

### 6.3 双向可追溯

- **新 finding → arm-index**：N/A（无新 finding）。
- **静态存疑点闭合**：A1.6 §7 存疑点 1 经本评估**正向消解为接受（P2 维持）**（强制核销模式 config 不存在 → L1 限定条件不活跃 → reminder 模式倾向接受），闭合。
- **与 P2-RC-006（维持）+ P1-MA2-017（不同控制点，不重开）+ A1.6 §5.1 UC-FIN-06 P2（确认维持）+ A2.3 period-close E2E（复用分流行为）分层一致**。

---

## 7. 静态存疑点清单（§6 §7）

无。本验证是 MA4 运行时确认，存疑点 A1.6 §7-1 经强制核销模式 config census + L1 限定词活跃性裁决 + reminder 模式前置门禁期望符合性评估**正向消解为接受（P2 维持）**，无遗留运行时存疑点。

**P0 即时通道**：本验证 Phase 1 定级**未出 P0/P1**（维持 P2），按 §10 **不触发 MR0/MR1**。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（HEAD=`192aaf3e2`），actual vs baseline 汇总如下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0，本验证实测 EXIT=0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => `sys.exit(1)`。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本验证无生产代码变更**（只读评估：读 reminder/hasIssues 分流代码 + 强制核销 config census + 既有测试普查 + 引用 A1.6/A2.3），checker 无回归风险。

  | 规则 | Baseline（`compliance-baseline.md §BASELINE (machine-readable)` 权威块 :300-304） | Actual（本验证 HEAD `192aaf3e2` 实测） | 状态 |
  |------|-----------------------------------------------------|----------------------------|------|
  | R1a (dao().saveEntity BizModel) | 0 | 0 | = ✅ |
  | R1b (dao().updateEntity BizModel) | 0 | 0 | = ✅ |
  | R1c (dao().getEntityById BizModel) | 0 | 0 | = ✅ |
  | R1d (dao().findAllByQuery BizModel) | 14 | 14 | = ✅ |
  | R2a (BizModel daoFor ErpMd*) | 34 | 34 | = ✅ |
  | R2b (BizModel daoFor Erp* 跨域) | 229 | 229 | = ✅ |
  | R2c (全生产 daoFor 总量) | 1382 | （脚本 R2c 段既有行为：未输出计数即返回——A4.1.11/A4.1.13 已记录同型行为） | 不适用（脚本行为，零代码变更无回归风险） |
  | R2d (Processor daoFor ErpMd*) | 34 | 34 | = ✅ |

  > R1/R2（除 R2c 脚本既有行为外）全部 actual == baseline，**0 漂移**。权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 块为准。R2c 脚本输出截断/未返回是既有工具行为（A4.1 展开器 / A4.1.11 / A4.1.13 报告同款记录）；本验证零生产代码变更（docs-only），checker 无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（零新 finding，维持 P2-RC-006）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。交叉去重声明：与 A1.6 §5.3 P2-RC-006（倾向接受，确认维持并精确化）+ 与 A1.6 §6.1 P1-MA2-017（不同控制点/不同维度，不重开）+ 与 A2.3 period-close E2E（复用 reminder/hasIssues 分流行为，§去重协议）+ MA4↔A5.6 边界（需求契约视角 reminder 模式评估 vs 测试质量全量评级，不重做 A5.6）。

---

## 9. 真相源冻结声明（§9）

本验证未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc `period-close.md` 需求契约段落）。只读评估（读 reminder/hasIssues 分流代码 + 强制核销 config census + 既有测试普查 + 引用 A1.6/A2.3），未修改代码/ORM/api.xml/view.xml/真相源。L1↔L2 字面冲突（「拒绝」vs「提示」）记入报告（§2.3），不直改 L1/L2（§9 冻结条款）。

---

## 10. 与 A1.6/A2.3 报告差异增量声明（§去重协议）

本验证复用 A1.6 §2.3（PC-3 reminder 偏离 L1 静态确认）+ §5.3（P2-RC-006 倾向接受）+ §6.1（P2-RC-006 与 P1-MA2-017 不同控制点）+ A2.3 period-close E2E（P1-MA2-017 resolved 阻断分级重构 + reminder/hasIssues 分流行为）+ A4.1.10（config-gated 路径评估同型范式），**不重新核实 reminder/hasIssues 分流行为本身**。只补 A1.6 §7 存疑点 1 标注为「需运行时确认」的差异：

1. **强制核销模式 config census**（A1.6 §7-1 触发条件「实际启用强制核销模式（未文档化 config）」未核实）：§2.2 grep census 三重证实 config **不存在**（字面变体生产代码零命中 + ErpFinConstants 全 81 键普查 + 消费点普查）——触发条件「实际启用强制核销模式」**不可达**，存疑点触发条件本身不成立。
2. **L1 限定词活跃性裁决**（A1.6 §5.3 倾向接受理由「强制核销模式 config 默认未启用」未精确化）：§2.3 + §5.1 按 §4 Q1 裁决 L1 PC-3 限定词「强制核销模式」**不活跃**（config 不存在 → 永不满足）——倾向接受结论精确化（config 不仅默认未启用，而是不存在）。
3. **reminder 模式前置门禁期望符合性**（A1.6 §7-1「是否符合用户对前置门禁的期望」未评估）：§2.4 评估 reminder 模式提供运营提示 + 强制核销 config 未启用 → 符合「前置门禁」运营提示期望。
4. **P2-RC-006 决策闭合**（A1.6 §5.3 倾向接受未闭合）：§5.1 按决策树分支①裁决**维持 P2 watch-only**，arm-index 注记更新（§6.2）。

差异增量与本验证范围一致，无与 A1.6/A2.3 重叠的重新核实。

---

## 11. Verdict

**Verdict: passes requirement-compliance runtime-behavior evaluation**（P2-RC-006 维持 P2 watch-only，零 P0/P1 新 finding，零 successor 触发）

**审查范围**：A1.6 §7-1 存疑点（PC-3 AR/AP reminder 模式运行时行为）评估——reminder/hasIssues 分流逻辑核验（`hasIssues():93-96` 不含未核销 AR/AP / `hasReminders():102-105` 含 / `ClosePeriodProcessor:59` 仅 hasIssues 阻断）+ **强制核销模式 config census**（grep 字面变体 + ErpFinConstants 全 81 键 + 消费点三重普查 = 零命中）+ L1↔L2 冲突裁决（§4 Q1=(c) L1 为准 + 限定词活跃性）+ 前置门禁期望符合性 + 测试覆盖边界普查 + MA4↔A5.6 边界声明 + §2 判据裁决（决策树分支①）+ 与 arm-index 衔接（维持 P2-RC-006 + 追加注记）+ §8 过程纪律自检 + §9 真相源冻结 + §10 差异增量声明。

**维持 P2 类**：PC-3 AR/AP reminder 模式运行时行为评估确认 P2-RC-006 维持——强制核销模式 config **不存在**（grep census 三重证实）→ L1 PC-3 限定词「强制核销模式」**不活跃**（永不满足）→ L1「强制核销模式→拒绝」字面要求无可达触发条件 → 分歧倾向接受（A1.6 §5.3 倾向接受结论确认并精确化）；reminder 模式提供运营提示（unsettledArApCodes 经向导 Step 1 结构化展示列出可见）；决策树分支①命中，不升 P1。

**P0/P1**：无。不触发 MR0/MR1。A1.6 §5.1 UC-FIN-06 整体 P2 维持。

**剩余风险**：无遗留运行时存疑点。config-gated 强制核销模式 hard block 缺失归 MR1 successor（若未来产品化要求强制核销模式，须补 config + `findUnsettledArApCodes` 结果纳入 `hasIssues()` 触发 hard block 分支，BizModel 代码逻辑预授权类目，不触发 §5 ask-first）；reminder-不阻断显式断言缺口归 A5.6 测试质量维度 successor（纯测试代码 MR1 预授权类目）。两者均非本 MA4 范围。
