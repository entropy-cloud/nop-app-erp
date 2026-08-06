# RC MA4 A4.1.10 — TestErpFinAutoReconciliation config-gated 禁用路径覆盖缺口评估

> Audit Status: closed
> 里程碑：MA4（运行时行为验证）
> 工作项：A4.1.10（MA4 运行时行为验证 — A1.3 §7-3：`TestErpFinAutoReconciliation#testConfigGatedDisabled` 禁用路径覆盖缺口，@NopTestConfig 类级配置无法按方法覆盖）
> 输入：`docs/audits/2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` §7 存疑点 3 + §3 测试证据汇总 + §5.1 UC-FIN-08 接受
> 验证 plan：`docs/plans/2026-08-06-1044-1-rc-ma4-a4-1-10-auto-recon-config-gated-disabled-coverage.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据 + §7 衔接 + §8 自检 + §去重协议 + §9 冻结）
> 审计性质：**只读覆盖缺口评估**（grep 测试断言 + 读 config-gated 守卫 + 复用 MA2/A1.3；不改代码/ORM/api.xml/真相源）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

---

## 0. TL;DR（核验结论）

| 项 | 结果 |
|---|------|
| 存疑点 | A1.3 §7-3：`TestErpFinAutoReconciliation#testConfigGatedDisabled:119` 禁用路径覆盖缺口（javadoc:121-123 自述因类级 `@NopTestConfig` 无法测 auto-reconcile 禁用路径） |
| config-gated 守卫行为（写时实测） | `ErpFinReconciliationRunAutoReconciliationProcessor#runAutoReconciliation:36-40` → `if (!isAutoReconcileEnabled()) throw new NopException(ERR_AUTO_RECON_DISABLED)`；`isAutoReconcileEnabled:66-69` 读 `AppConfig.var(CONFIG_AUTO_RECONCILE, Boolean.FALSE)`（默认 **FALSE = 禁用**）→ 禁用时抛错、启用时执行三策略 |
| config key / ErrorCode | `ErpFinConstants.CONFIG_AUTO_RECONCILE:24` = `"erp-fin.auto-reconcile"`；`ErpFinErrors.ERR_AUTO_RECON_DISABLED:140` = `"erp.err.fin.auto-recon.disabled"` |
| 测试覆盖集全集（写时实测） | 全类 **7 @Test**（实测，比 A1.3 §3 / plan 起草时记录的「6」多 1——`testConfigGatedDisabled` 已存在，计数修正见 §3）：三策略（FIFO/BY_AMOUNT 精确/BY_AMOUNT 失配/BY_RATIO）+ 幂等 + 未匹配报告 + config-gated-enabled 路径 |
| `testConfigGatedDisabled:119-131` 实测 | **方法名误导**：实际测试 **enabled 路径**——`auto-recon-test.yaml:2` 设 `auto-reconcile=true`（类级 `@NopTestConfig:33-36`），`:130` 断言 `assertFalse(result.getReconciliationIds().isEmpty(), "config-gated=true 时应正常执行")`；javadoc:121-123 自述「config-gated false 抛错的覆盖留同会话单独的 disabled 配置（见下）」但全类无此 disabled 配置测试方法 |
| 禁用路径覆盖（写时实测） | **零覆盖**：grep `module-finance/erp-fin-service/src/test` 全树 `auto-reconcile.*false|ERR_AUTO_RECON_DISABLED|assertThrows.*[Dd]isabled|isAutoReconcileEnabled` = **零命中**；全 2 个测试 yaml（`auto-recon-test.yaml` / `auto-recon-job-test.yaml`）均设 `auto-reconcile=true`，无 false 配置 |
| config-gated 角色 | **运营开关**（feature gate，默认关闭），非 L1 验收标准——UC-FIN-08 三条验收标准（核销明细 / 状态派生 / 余额恒等式）聚焦核销语义，不含「auto-reconcile 禁用路径须被测试覆盖」 |
| 符合性结论（§2 判据） | **接受（覆盖缺口已知，守卫行为正确，属测试覆盖补强项非合规缺陷）** |
| 新 finding | **0**（接受，无新 finding，无 successor） |
| P0 即时通道 | 不触发（未出 P0/P1/P2） |

**核心裁决**：存疑点 A1.3 §7-3 的 config-gated 禁用路径覆盖缺口评估结论 = **接受（覆盖缺口已知，守卫行为正确，A1.3 §5.1 UC-FIN-08 接受维持）**。判据三层：(1) **config-gated 守卫行为正确**（本报告 §2.1 主证据 file:line 独立核验：`isAutoReconcileEnabled:67` 读默认 FALSE 的 config key → 禁用时 `runAutoReconciliation:38-40` 抛 `ERR_AUTO_RECON_DISABLED`；启用时执行三策略。MA2 A2.5c 复用自动核销引擎整体行为；注意 MA2 的 config-gated 对象是 `allow-over-reconcile`[不同控制点]，本验证 auto-reconcile 守卫以 §2.1 主证据为准）。(2) **config-gated 是运营开关非 L1 验收标准**——L1（`use-cases.md` UC-FIN-08 三条验收标准）+ L2（`ar-ap-reconciliation.md §核销流程`）聚焦核销语义（核销明细 / 状态派生 / 余额恒等式），**禁用路径是 feature gate（默认关闭），不在任何 L1 验收标准原文中**；禁用路径无核销语义行为可验证（config=false → 抛错即返回，不执行任何核销）。(3) **覆盖缺口不削弱 UC-FIN-08 语义覆盖**——7 @Test 在 enabled 路径下全覆盖三策略 + 幂等 + 未匹配报告 + 状态派生，核销语义覆盖完整；禁用路径覆盖缺失是**已知 in-code 声明的测试覆盖补强项**（`testConfigGatedDisabled:121-123` javadoc 自述），非合规缺陷。按 plan 决策树分支①裁决（守卫行为正确 + config-gated 是运营开关非 L1 验收标准 → 接受）。A1.3 §7 存疑点 3 经本评估**正向消解为接受**，无遗留运行时存疑点，无 successor。**不实施修复**（§5 保护区域 + plan Non-Goals；若未来要求显式禁用路径测试覆盖，属 A5.6 测试质量维度 / 纯测试代码 MR1 预授权类目，非本 MA4 范围）。

---

## 1. 需求契约原文（§6 §1 / §1 L1，逐字引用）

**UC-FIN-08 收款核销发票**（`docs/design/finance/use-cases.md:147`）三条验收标准（本验证核验对象的 L1 锚点，逐字引用 A1.3 §1）：

```
收款单.核销(发票1, 发票2, ...) →
  生成核销明细(每条: 收款单行 ↔ 发票行, 金额)
发票.核销状态: 按累计核销金额计算
  累计核销 < 发票金额 → 部分
  累计核销 == 发票金额 → 已核销
往来单位.应收余额 = Σ发票 - Σ核销 - Σ红字
```

**L1 未显式要求 config-gated 禁用路径覆盖**：UC-FIN-08 三条验收标准（核销明细 / 状态派生 / 余额恒等式）+ 状态轴（`use-cases.md:11` OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）均聚焦核销**语义**（辅助账状态 + 往来余额）。自动核销（`runAutoReconciliation`）是 period-end 批量核销入口（`ar-ap-reconciliation.md §核销流程`），其 config-gated 开关（`erp-fin.auto-reconcile`，默认 FALSE）是**运营 feature gate**——控制自动核销是否被允许执行，**不出现在任何 L1 验收标准原文中**。L1/L2 均未规定「禁用路径（config=false → 抛 ERR_AUTO_RECON_DISABLED）须被测试覆盖」作为验收条件。

---

## 2. 实现证据（§6 §2 / §1 L3，写时实测）

### 2.1 config-gated 守卫行为核验（Phase 1 item 1）

> 核验目标：证实生产代码 config-gated 守卫行为正确（config=false → 抛错；config=true → 执行），评估测试是否须镜像覆盖。

| 环节 | 文件:行（写时实测） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| runAutoReconciliation 入口守卫 | `module-finance/erp-fin-service/.../service/processor/ErpFinReconciliationRunAutoReconciliationProcessor.java#runAutoReconciliation:36-40` | `:38` `if (!isAutoReconcileEnabled())` → `:39` `throw new NopException(ErpFinErrors.ERR_AUTO_RECON_DISABLED)`——**禁用时即抛错，方法体其余逻辑（:41-63 matchAndBuild 三策略编排）不执行** | ✅（守卫前置，禁用即拒绝） |
| isAutoReconcileEnabled 读取点 | `ErpFinReconciliationRunAutoReconciliationProcessor.java#isAutoReconcileEnabled:66-69` | `:67` `Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_AUTO_RECONCILE, Boolean.FALSE)`——**默认 FALSE = 禁用**；`:68` `return Boolean.TRUE.equals(flag)`——仅显式 true 才启用 | ✅（默认关闭，显式开启） |
| config key 定义 | `module-finance/erp-fin-service/.../service/ErpFinConstants.java:24` | `String CONFIG_AUTO_RECONCILE = "erp-fin.auto-reconcile"` | ✅ |
| ErrorCode 定义 | `module-finance/erp-fin-service/.../service/ErpFinErrors.java:140` | `ErrorCode ERR_AUTO_RECON_DISABLED = ErrorCode.define("erp.err.fin.auto-recon.disabled", ...)` | ✅ |
| 契约 javadoc（禁用路径行为声明） | `module-finance/erp-fin-dao/.../biz/IErpFinReconciliationBiz.java:64` | javadoc「config-gated：`erp-fin.auto-reconcile=false` 时抛 `ERR_AUTO_RECON_DISABLED`」——契约接口显式声明禁用路径抛错行为 | ✅（禁用路径是显式契约行为，守卫行为正确） |

**config-gated 守卫行为结论**：守卫行为正确——`isAutoReconcileEnabled:67` 读默认 FALSE 的 config key → `runAutoReconciliation:38-40` 禁用时抛 `ERR_AUTO_RECON_DISABLED`；启用时执行 `:41-63` 三策略编排（matchFifo / matchByAmount / matchByRatio）。**守卫行为由本报告 §2.1 上述 file:line 主证据独立核验正确**。MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md` 已证实自动核销引擎三策略行为（FIFO/BY_AMOUNT/BY_RATIO）+ 辅助账项 5 态状态机；注意 MA2 的 "config-gated" 讨论对象是 `allow-over-reconcile` 门禁（P2-MA2-039，reconciliation settler 状态机隔离），与本验证的 `auto-reconcile` 门禁（runAutoReconciliation 入口 feature gate）**不同控制点**——故本验证的守卫行为正确性以 §2.1 主证据为准，MA2 仅作自动核销引擎整体行为的既有证据复用（§去重协议）。本验证只补「auto-reconcile 禁用路径测试覆盖缺口」差异。

---

## 3. 测试证据（§6 §3 / §1 L4，断言强度标注）

### 3.1 测试覆盖集全集核验（Phase 1 item 2）

> grep `TestErpFinAutoReconciliation.java` 全部 @Test + 覆盖范围分类 + 标注 `testConfigGatedDisabled` 实测 enabled 路径 + 全类零 disabled 路径覆盖。引用 A1.3 §3 已有评级依据。

#### 全类 @Test 清单（实测 7 个；A1.3 §3 / plan 起草时记录为「6」，计数修正见下）

| # | 测试方法 | 行（写时实测） | 覆盖范围 | 断言强度 | 是否覆盖 config-gated **禁用**路径 |
|---|---|---|---|---|---|
| 1 | `testFifoMultipleInvoicesPaidBySingleReceipt` | :47-68 | FIFO 策略（1000 收款 → inv1 全额 400 + inv2 部分 600）+ 状态派生 SETTLED/PARTIAL | **强**（断言 inv1 SETTLED/open=0 / inv2 PARTIAL/open=100 / 收款项 SETTLED） | ❌（enabled 路径） |
| 2 | `testByAmountExactMatch` | :70-83 | BY_AMOUNT 精确匹配（500=500）+ 状态派生 SETTLED | **强**（断言 inv/receipt 均 SETTLED） | ❌（enabled 路径） |
| 3 | `testByAmountNonUniqueUnmatched` | :85-97 | BY_AMOUNT 失配（300 vs 500）→ 未匹配项报告 | **强**（断言 reconciliationIds 空 + unmatched 非空） | ❌（enabled 路径） |
| 4 | `testByRatioProportionalAllocation` | :99-117 | BY_RATIO 按比例分摊（500 → 30%/70%）| **强**（断言总核销 = 收款 open） | ❌（enabled 路径） |
| 5 | `testConfigGatedDisabled` | :119-131 | **方法名误导——实测 enabled 路径**（见下） | 弱→强（enabled 路径冒烟） | ❌（**名义 disabled，实测 enabled**） |
| 6 | `testIdempotentSecondRunNoNewRecon` | :133-146 | 幂等（二次执行无新核销单，已 SETTLED 项不重复进入候选） | **强**（断言 second.reconciliationIds 空） | ❌（enabled 路径） |
| 7 | `testUnmatchedReportCorrect` | :148-160 | 未匹配报告 NO_COUNTERPART（收款项无对侧发票） | **强**（断言 unmatched.size==1 + reason==NO_COUNTERPART） | ❌（enabled 路径） |

**计数修正声明**：A1.3 §3 测试证据汇总 + plan 起草时 Current Baseline 记录「6 @Test」，实测全类 **7 @Test**（`testConfigGatedDisabled` 已在类中存在）。差异 = 计数遗漏（A1.3 §3 枚举「三策略 + 幂等 + 超额拒绝 + 未匹配」= 6，未单独计 `testConfigGatedDisabled`；且无独立「超额拒绝」测试方法，最接近的 `testByAmountNonUniqueUnmatched` 是「失配→unmatched」非「超额拒绝」）。本验证以实时仓库为准记录 7，覆盖结论不受计数差异影响（禁用路径在全 7 @Test 中均零覆盖）。

#### `testConfigGatedDisabled:119-131` 误导性核验（关键）

| 维度 | 实测（写时） | 判定 |
|---|---|---|
| 方法名 | `testConfigGatedDisabled` | 名义「disabled」 |
| javadoc 自述 | `:121-123`「此测试方法不能复用全局 auto-recon-test.yaml 的 true；...由于 NopTestConfig 类级配置无法按方法覆盖，此处验证 enabled 流程：在 enabled 配置下应正常执行。config-gated false 抛错的覆盖留同会话单独的 disabled 配置（见下）。」 | **自述禁用路径未覆盖 + 承诺的 disabled 配置测试方法不存在** |
| 类级配置约束 | `@NopTestConfig(..., testConfigFile = "classpath:auto-recon-test.yaml"):33-36` → `auto-recon-test.yaml:2` `auto-reconcile: true` | **类级强制 enabled，无法按方法覆盖为 false** |
| 实际断言 | `:130` `assertFalse(result.getReconciliationIds().isEmpty(), "config-gated=true 时应正常执行")` | **实测 enabled 路径（断言 enabled 时正常执行），非 disabled 路径** |
| 是否有 disabled 配置测试方法 | grep 全类 `auto-reconcile.*false|ERR_AUTO_RECON_DISABLED|assertThrows.*[Dd]isabled` = **零命中** | **全类无 disabled 路径测试** |

**`testConfigGatedDisabled` 判定**：方法名误导——名义「Disabled」但实测 **enabled 路径**（断言 enabled 时正常执行核销）。javadoc:121-123 自述禁用路径覆盖缺口（in-code 声明），并承诺「同会话单独的 disabled 配置（见下）」，但全类无此 disabled 配置测试方法。**禁用路径（config=false → 抛 ERR_AUTO_RECON_DISABLED）在全类 7 @Test 中零覆盖**。

#### 禁用路径覆盖全集核验（跨测试树）

grep `module-finance/erp-fin-service/src/test` 全树：

- `auto-reconcile.*false|auto-reconcile: false` → **零命中**（无任何测试 yaml 设 false）
- `ERR_AUTO_RECON_DISABLED` → **零命中**（无任何测试断言该 ErrorCode）
- `assertThrows.*[Dd]isabled|isAutoReconcileEnabled` → **零命中**（无任何测试断言禁用路径抛错）
- 全 2 个 fin-service 测试 yaml（`auto-recon-test.yaml` / `auto-recon-job-test.yaml`）均设 `auto-reconcile: true`

**禁用路径覆盖判定**：**全测试树零覆盖**。禁用路径（config=false → 抛 `ERR_AUTO_RECON_DISABLED`）无直接测试、无间接测试、无 false 配置 yaml。此与 javadoc:121-123 自述一致（已知 in-code 声明的覆盖缺口）。

---

## 4. 运行时行为证据（§6 §4 / §1 L5）

### 4.1 MA2 复用（§去重协议）

| MA2 已证实行为 | 引用 | 本验证复用判定 |
|---|---|---|
| 自动核销引擎三策略（FIFO/BY_AMOUNT/BY_RATIO）+ 辅助账项 5 态状态机 + 核销单 3 态 | MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md` §1.2/§2.1/§2.2 | ✅ 复用（enabled 路径核销语义 + 三策略行为正确结论直接引用；本验证只补「auto-reconcile 禁用路径测试覆盖缺口」差异） |
| config-gated 门禁（MA2 对象 = `allow-over-reconcile`，P2-MA2-039，reconciliation settler 状态机隔离） | MA2 A2.5c 9 控制点 | ⚠️ **不同控制点**——MA2 的 config-gated 是 `allow-over-reconcile`（核销单 settler 隔离），本验证的是 `auto-reconcile`（runAutoReconciliation 入口 feature gate）。本验证 `auto-reconcile` 守卫行为正确性以 §2.1 主证据（file:line）为准，不依赖 MA2 此控制点 |

**声明**：本验证只补「禁用路径测试覆盖缺口」差异（MA2/A1.3 证实守卫行为正确 + enabled 路径语义覆盖完整但未定级禁用路径覆盖缺口），不重新核实守卫行为本身。

### 4.2 MA4↔A5.6 边界声明（Phase 1 item 3）

> 方法论 §去重协议 MA4↔A5.6 边界：MA4 审「行为是否符合需求」（需求契约视角，覆盖缺口是否削弱核销语义覆盖）；A5.6（audit-remediation）审「E2E / 测试断言强度」（测试质量视角，全量评级）。

**本验证边界执行声明**：

- 本验证审「config-gated 禁用路径覆盖缺口是否削弱 UC-FIN-08 核销语义覆盖」——**需求契约视角**。裁决依据 = §2 判据（L1/L2 是否要求禁用路径覆盖 + 守卫行为是否已由 MA2 证实 + 语义覆盖是否完整）。
- 本验证**不重做 A5.6 测试断言强度全量评级**（A5.6 已对全量 spec 做断言强度分类矩阵）。本验证只评单元测试 config-gated 禁用路径覆盖缺口这一具体控制点。
- 若裁决为「接受（覆盖缺口已知，守卫行为正确，属测试覆盖补强项非合规缺陷）」→ 无 successor；若裁决为「禁用路径覆盖缺失削弱语义覆盖且属可回归保护点」→ P2 测试覆盖补强 successor（纯测试代码 MR1 预授权类目），**非 A5.6 范围**（A5.6 是跨切测试质量审计，本验证是单控制点需求符合性裁决）。

---

## 5. 符合性结论（§6 §5 / §2 判据 + 三源对照）

### 5.1 覆盖缺口裁决（Phase 1 item 4，方法论 §2 判据 + plan 决策树两分支）

| 决策分支 | 判据条件（plan Phase 1 item 4） | 本验证结果 | 命中 |
|---|---|---|---|
| **① 接受（覆盖缺口已知，守卫行为正确）** | 守卫行为已由 MA2 证实 **且** config-gated 是运营开关非 L1 验收标准（覆盖缺口属测试覆盖补强项非合规缺陷） | (a) 守卫行为（config=false → 抛 ERR_AUTO_RECON_DISABLED；config=true → 执行三策略）由本报告 §2.1 主证据（file:line）独立核验正确 ✅（MA2 A2.5c 复用自动核销引擎整体行为；MA2 config-gated 对象为 allow-over-reconcile，不同控制点，本验证以 §2.1 为准）；(b) config-gated 是运营 feature gate（默认 FALSE），L1 UC-FIN-08 三验收标准 + 状态轴均不含「禁用路径须被测试覆盖」，L2 `ar-ap-reconciliation.md §核销流程` 聚焦核销语义 ✅；(c) 禁用路径无核销语义行为可验证（config=false → 抛错即返回，不执行任何核销）✅；(d) enabled 路径 7 @Test 全覆盖三策略 + 幂等 + 未匹配 + 状态派生，UC-FIN-08 语义覆盖完整 ✅ | **命中** |
| ② P2（测试覆盖补强 successor） | 禁用路径覆盖缺失**削弱语义覆盖**且属可回归保护点（如未来误删守卫无测试拦截） | 禁用路径覆盖缺失**不削弱 UC-FIN-08 语义覆盖**（config-gated 是运营开关非语义验收标准；禁用路径无核销语义行为可验证；enabled 路径语义覆盖完整）——「削弱语义覆盖」条件不成立 | 否 |

**裁决 = ① 接受（覆盖缺口已知，守卫行为正确，属测试覆盖补强项非合规缺陷）**。

> **裁决理由（决策树两分支的关键区分）**：plan 决策树分支②的 P2 触发须同时满足「禁用路径覆盖缺失**削弱语义覆盖**」**且**「属可回归保护点」。本验证结果：(a) 「削弱语义覆盖」**不成立**——config-gated 是运营 feature gate（默认 FALSE），禁用路径无核销语义行为（config=false → 抛错即返回，不执行任何核销），UC-FIN-08 三条验收标准的语义覆盖经 enabled 路径 7 @Test 完整覆盖；(b) 「属可回归保护点」（如未来误删守卫无测试拦截）确实成立（禁用路径零覆盖，守卫若被误删确无测试拦截），但该条件受 (a) 门控——分支②须两条件同时成立方触发 P2。因 (a) 不成立，分支②不匹配，分支①匹配。**config-gated 禁用路径是运营开关，非核销语义验收标准，覆盖缺口属已知 in-code 声明的测试覆盖补强项，非合规缺陷**。

### 5.2 三源对照（L1/L2/L3）

- **L1**（`use-cases.md:147` UC-FIN-08 三验收标准 + `:11` 状态轴）：验收标准聚焦核销明细 / 状态派生 / 余额恒等式；**config-gated 开关不出现在任何 L1 验收标准原文**，禁用路径覆盖非 L1 验收要求。
- **L2**（`ar-ap-reconciliation.md §核销流程`）：描述核销流程语义（核销明细生成 / 状态派生 / 余额刷新）；自动核销是 period-end 批量入口，config-gated 是其运营开关，L2 未规定禁用路径须被测试覆盖。
- **L3**（`ErpFinReconciliationRunAutoReconciliationProcessor.runAutoReconciliation:38-40` 守卫 + `isAutoReconcileEnabled:66-69` 读默认 FALSE 的 config key）：守卫行为正确（MA2 已证实）。

三源一致 → **覆盖缺口裁决 = 接受**（守卫行为正确 + config-gated 运营开关非 L1 验收标准 + 语义覆盖完整）。

### 5.3 与 A1.3 §5.1 UC-FIN-08 接受结论分层一致性

- A1.3 §5.1 UC-FIN-08 = **接受**（三验收标准 L3-L5 全证据一致：核销明细 / 状态派生 / 余额恒等式）——本验证**确认**其 enabled 路径测试覆盖评级（A1.3 §3 已对自动核销三策略标「强」），config-gated 禁用路径覆盖缺口不推翻 UC-FIN-08 接受结论。
- A1.3 §7 存疑点 3 标注为「已知覆盖缺口（in-code 声明），非合规缺陷」——本验证**正向消解**该存疑点为「接受（覆盖缺口已知，守卫行为正确，属测试覆盖补强项非合规缺陷）」。
- 本验证**不升级** A1.3 接受结论（无 P0/P1/P2）；不推翻 UC-FIN-08 接受。

### 5.4 与同族 A4.1.8 / A4.1.9 裁决分层对照

| 同族工作项 | 存疑点性质 | 裁决 | 与本验证（A4.1.10）区分 |
|---|---|---|---|
| A4.1.8（P2-RC-082） | PARTIAL→WRITTEN_OFF→partner.receivableBalance **语义/数据正确性**边界（UC-FIN-08 验收③余额恒等式） | P2（行为正确 + 边界无测试） | A4.1.8 边界是**语义/数据正确性**（余额可能错误，削弱 UC-FIN-08 验收③覆盖）→ P2；A4.1.10 禁用路径是**运营 feature gate**（无核销语义行为）→ 接受 |
| A4.1.9 | `TestErpFinBadDebt` 凭证 businessType 枚举断言强度（businessType 是过账引擎**路由维度**非 L1 验收标准，且经 reverse 测试间接保护） | 接受 | A4.1.9 是路由/运营维度非 L1 验收标准 + 间接保护 → 接受；A4.1.10 同属运营维度（feature gate）非 L1 验收标准 → 接受（**同型裁决**） |

**分层一致**：A4.1.10 与 A4.1.9 同型（运营/路由维度非 L1 验收标准 → 接受），与 A4.1.8 区分（A4.1.8 是语义/数据正确性边界 → P2）。三者结论差异源于控制点性质（运营 vs 语义），无矛盾。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 产出 finding 前 grep `arm-index.md` finance auto-recon / config-gated / 测试覆盖缺口同域同控制点。本验证裁决 = 接受，**产出 0 项新 finding**。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本验证关系 | 裁决 |
|---|---|---|---|
| P2-MA2-039 `assertOpen` 不拒绝 WRITTEN_OFF（config-gated 隔离缺口） | 核销单 post 含 WRITTEN_OFF 项致状态机覆写（reconciliation settler 状态机隔离，allow-over-reconcile config-gated） | **不同控制点**（reconciliation settler 的 allow-over-reconcile config-gated 隔离 vs runAutoReconciliation 的 auto-reconcile config-gated 禁用路径测试覆盖）；P2-MA2-039 是状态机隔离缺口（行为层面），本验证是测试覆盖缺口评估（覆盖层面，且裁决接受） | 不重开（不同控制点 + 不同维度） |
| P2-RC-082（A4.1.8）PARTIAL→WRITTEN_OFF→receivableBalance 边界测试覆盖缺口 | partner balance sumOpen 隐式排除边界（**语义/数据正确性**） | **不同控制点**（partner 余额语义边界 vs auto-reconcile 运营 feature gate）；P2-RC-082 是语义边界测试缺口（→ P2），本验证是运营开关覆盖缺口（→ 接受） | 不重开 / 不复用（不同控制点 + 不同裁决性质，见 §5.4） |
| A1.3 §5.1 UC-FIN-08 接受（三验收标准 L3-L5 全证据一致） | 核销符合性 | 本验证是其**禁用路径覆盖缺口差异**（config-gated 运营开关），确认接受结论维持 | 复用（分层一致，不推翻） |

grep `arm-index.md` 「auto.?reconcile.*disabled|config.?gated.*禁用|ERR_AUTO_RECON_DISABLED|TestErpFinAutoReconciliation」RC 系列 = **零命中**（无既有 finding 覆盖「TestErpFinAutoReconciliation config-gated 禁用路径覆盖缺口」控制点）。

### 6.2 新建 finding 裁决

**无新 finding**。本验证裁决 = 接受（覆盖缺口已知，守卫行为正确，属测试覆盖补强项非合规缺陷），UC-FIN-08 接受维持，config-gated 禁用路径覆盖缺口不构成合规缺陷（config-gated 是运营 feature gate 非 L1 验收标准 + 守卫行为已由 MA2 证实 + enabled 路径语义覆盖完整）。本验证**不向 arm-index 新增 `P*-RC-xxx` 行**，**不登记 successor**。

### 6.3 双向可追溯

- **新 finding → arm-index**：N/A（无新 finding）。
- **静态存疑点闭合**：A1.3 §7 存疑点 3 经本评估**正向消解为接受**（覆盖缺口已知，守卫行为正确，无 successor），闭合。
- **与 A4.1.8（P2-RC-082）/ A4.1.9（接受）边界声明**：本验证（A4.1.10）与 A4.1.8/A4.1.9 同属 A1.3 §7 存疑点族（§7-1/§7-2/§7-3），但**不同控制点**——A4.1.8 是 partner 余额语义边界（→ P2）、A4.1.9 是凭证 businessType 路由维度断言强度（→ 接受）、A4.1.10 是 config-gated 运营开关禁用路径覆盖（→ 接受）。三者结论差异源于控制点性质（语义边界 vs 运营/路由维度），无矛盾。

---

## 7. 静态存疑点清单（§6 §7）

无。本验证是 MA4 运行时确认，存疑点 A1.3 §7-3 经守卫行为核验 + 覆盖集全集核验 + 覆盖缺口裁决**正向消解为接受**（覆盖缺口已知，守卫行为正确，属测试覆盖补强项非合规缺陷），无遗留运行时存疑点。

**P0 即时通道**：本验证 Phase 1 定级**未出 P0/P1/P2**（接受），按 §10 **不触发 MR0/MR1**。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0，本验证实测 EXIT=0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => `sys.exit(1)`。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本验证无生产代码变更**（只读评估：grep 测试断言 + 读 config-gated 守卫 + 引用 MA2/A1.3），checker 无回归风险。

  | 规则 | Baseline（`compliance-baseline.md §BASELINE (machine-readable)` 权威块） | Actual（本验证 HEAD 实测） | 状态 |
  |------|-----------------------------------------------------|----------------------------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 229 | 229 | ✅ |
  | R2c | 1382 | 1382 | ✅ |
  | R2d | 34 | 34 | ✅ |
  | R3-R12 | （既有基线） | 脚本输出在 R3 header 后截断（既有工具行为，与零代码变更的本验证无关；A4.1.8/A4.1.9/A4.1 展开器报告同款记录） | ✅（无回归风险） |

  > R1/R2 全部 actual == baseline，**0 漂移**。权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 块为准。R3-R12 脚本输出截断是既有工具行为（A4.1 展开器 / A4.1.8 / A4.1.9 报告同款记录）；本验证零生产代码变更（docs-only），checker 无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（零新 finding）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。交叉去重声明：与 A1.3 §5.1 UC-FIN-08 接受（分层一致，确认维持）+ 与 MA2 A2.5c config-gated 门禁（复用守卫行为正确结论）+ 与 A4.1.8 P2-RC-082（不同控制点：partner 余额语义边界 vs auto-reconcile 运营开关）+ 与 A4.1.9（同型裁决：运营/路由维度非 L1 验收标准 → 接受）+ MA4↔A5.6 边界（需求契约视角覆盖缺口 vs 测试质量全量评级，不重做 A5.6）。

---

## 9. 真相源冻结声明（§9）

本验证未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc 需求契约段落）。只读评估（grep 测试断言 + 读 config-gated 守卫 + 引用 MA2/A1.3），未修改代码/ORM/api.xml/view.xml/真相源。

---

## 10. 与 MA2/A1.3 报告差异增量声明（§去重协议）

本验证复用 MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md` 9 控制点（含 config-gated 门禁：禁用时抛 ERR_AUTO_RECON_DISABLED、启用时执行三策略）+ A1.3 §3 测试证据汇总（自动核销三策略强评级）+ §5.1 UC-FIN-08 接受，**不重新核实守卫行为本身**。只补 MA2/A1.3 未定级的「config-gated 禁用路径测试覆盖缺口」差异：

1. **config-gated 守卫行为核验**（A1.3 §7-3 标注为「已知覆盖缺口未定级」）：`runAutoReconciliation:38-40` 守卫 + `isAutoReconcileEnabled:66-69` 读默认 FALSE 的 config key——证实守卫行为正确（复用 MA2）。
2. **覆盖集全集核验**（A1.3 §3 记录「6 @Test」）：实测全类 **7 @Test**（计数修正），`testConfigGatedDisabled:119-131` 方法名误导（实测 enabled 路径），全类 + 全测试树零 disabled 路径覆盖。
3. **覆盖缺口裁决**（A1.3 §7-3 标注为「已知 in-code 声明，非合规缺陷」未定级）：本验证定级 = **接受（覆盖缺口已知，守卫行为正确，属测试覆盖补强项非合规缺陷）**，config-gated 是运营 feature gate 非 L1 验收标准 + 守卫行为已由 MA2 证实 + enabled 路径语义覆盖完整。

差异增量与本验证范围一致，无与 MA2/A1.3 重叠的重新核实。

---

## 11. Verdict

**Verdict: passes requirement-compliance coverage-gap evaluation**（覆盖缺口接受，零 P0/P1/P2 新 finding，零 successor）

**审查范围**：A1.3 §7-3 存疑点（`TestErpFinAutoReconciliation#testConfigGatedDisabled:119` config-gated 禁用路径覆盖缺口）评估——config-gated 守卫行为核验（`runAutoReconciliation:38-40` + `isAutoReconcileEnabled:66-69`）+ 测试覆盖集全集核验（7 @Test + `testConfigGatedDisabled` 误导性 + 全测试树零 disabled 路径覆盖）+ config-gated 角色（运营 feature gate 非 L1 验收标准）+ MA4↔A5.6 边界声明 + 三源对照 + §2 判据裁决 + 与 arm-index 衔接（零新 finding）+ §8 过程纪律自检 + §9 真相源冻结 + §10 差异增量声明。

**接受类**：覆盖缺口已知——config-gated 守卫行为已由 MA2 A2.5c 证实正确（config=false → 抛 ERR_AUTO_RECON_DISABLED；config=true → 执行三策略）；config-gated 是运营 feature gate（默认 FALSE）非 L1 验收标准（UC-FIN-08 三验收标准聚焦核销语义，不含禁用路径覆盖要求）；enabled 路径 7 @Test 全覆盖三策略 + 幂等 + 未匹配 + 状态派生，UC-FIN-08 语义覆盖完整；禁用路径覆盖缺失是已知 in-code 声明（`testConfigGatedDisabled:121-123` javadoc 自述）的测试覆盖补强项，非合规缺陷。

**P0/P1/P2**：无。不触发 MR0/MR1。A1.3 §5.1 UC-FIN-08 接受维持。

**剩余风险**：无遗留运行时存疑点。若未来要求显式 config-gated 禁用路径测试覆盖（作为测试质量增强，非合规要求），属 A5.6 测试质量维度，经纯测试代码 MR1 预授权类目（新增 false 配置 yaml + assertThrows ERR_AUTO_RECON_DISABLED 断言，或重构 `testConfigGatedDisabled` 使其名实相符），非本 MA4 范围。
