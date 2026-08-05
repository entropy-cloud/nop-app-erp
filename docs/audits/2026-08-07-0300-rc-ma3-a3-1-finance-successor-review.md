# rc-ma3-a3-1-finance-successor-review finance MA3 successor 追踪完整性与回队复查报告（A3.1）

> Plan Status: completed
> 产出时间：2026-08-07
> 来源 Plan：`docs/plans/2026-08-07-0300-2-rc-ma3-a3-1-finance-successor-review.md`（Work Item A3.1）
> Mission：requirement-compliance（MA3 successor 触发条件复查）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§4 三判据 / §5 Q4 + 保护区域 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）
> 路线图：`docs/backlog/requirement-compliance-roadmap.md`（A3.1 finance 域 successor 复查 + Work Item Details MA3）
> 复查全集：`docs/audits/rc-existing-inventory.md`（§successor 三源对账清单 finance 域分组 — 8 项 + §对账差异登记 #3）
> Skill：`docs/skills/open-ended-audit-prompt.md`
> 审计性质：**只读审计**——读 arm-index / owner doc / backlog README / 实仓代码 / config / SPI 裁决 successor 触发条件，**不修改任何代码/ORM/api.xml/真相源**

---

## §复查口径与 Q4 修复义务边界

本报告复查对象 = M0.3（`rc-existing-inventory.md` §successor 三源对账清单）导出的 finance 域 design-level successor 去重并集 **8 项**。逐项完成方法论 §MA3 四任务：① 触发条件是否已满足（grep 实仓代码/config/SPI 验证）；② 是否该回队（已满足→回队 MR1 R1.0；未满足→维持 backlog successor）；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

**Q4 修复义务边界（§5）**：successor 触发条件**已满足**者须回队 MR1（R1.0 展开为 RC-R1.n，Q4 强制实现禁方案 B）；触发条件**未满足**者维持 backlog successor 登记（不强制实现，待触发）。`P0-MA2-018` successor（#6）经 Q4 强制——其触发条件实质为「须更深设计变更」，Q4 无技术不可行例外，故**应回队 MR1**（与 A2.1 复查结论交叉一致）。

**finding 路由 vs successor 触发条件路由（防执行者混淆）**：本 A3.x 只裁决 **successor 触发条件**是否回队，不重审方案 B 关闭裁决本身（属 A2.x，已由 `2026-08-06-1400-rc-ma2-a2-1-2-finance-simplification-review.md` 收口）。successor 回队与否（A3.x）≠ finding 是否修复（A2.x→MR1），两者各自裁决、交叉引用不冲突。

---

## 1. successor 三源对账清单（finance 域，段 1，§6 MA3 适配）

> 三源：S1 = `docs/audits/arm-index.md` 行内 successor/触发条件声明 / S2 = owner doc 内嵌 successor / Deferred 段落 / S3 = `docs/backlog/README.md` 既有追踪行。

| # | successor 项 | 三源覆盖 | 触发条件摘要 | 复杂度 | A2.x 关闭裁决交叉（two-faces） |
|---|-------------|---------|-------------|--------|------------------------------|
| 1 | GRNI 正向 receive→invoice 自动冲回（方案 A） | S1+S2 | 双向钩子[approve 红冲+reverseApprove 反冲回]+部分开票覆盖判定+跨期语义；inventory 域 `repostPurchaseInput` SPI | S | `P1-MA2-001`（A2.1：有意设计，§4(i) 成立，保留 P2 successor） |
| 2 | GL 余额维护引擎（opening/closing） | S1+S2 | 补过账引擎 postVoucher 时维护 opening/closing 余额 | S | `P1-MA2-018`（A2.1：有意设计，§4(i) 成立，保留 P2 successor） |
| 3 | 累计余额对账（辅助账跨年） | S1+S2 | GL 余额维护 successor 落地后（#2 前置） | S | `P1-MA2-019`（A2.1：有意设计，§4(i) 成立，保留 P2 successor） |
| 4 | 反结账完整审批流（xwf） | S1+S2 | 浏览器层 xwf 审批路径落地 | S | `P1-MA2-020`（A2.1：有意设计，§4(i) 成立，保留 P2 successor） |
| 5 | FX 重估前期 reversal + 期间过滤（IAS 21 完整语义） | S1+S2 | IAS 21 完整语义需求 + config-gated 关闭默认 | S | `P1-MA2-022`（A2.1：有意设计，§4(i) 成立，保留 P2 successor） |
| 6 | 凭证幂等键字面 UK 方向 A/B/C/D | S1 | 重构 billR 加判别列（acctSchemaId/postingType/isReversed）+ 对应 UK | S | `P0-MA2-018`（A2.1：**静默降级，Q4 强制重开 MR1**） |
| 7 | 多币种全域源币金额迁移（其余域 Provider） | S2 | 各域启用多币种业务路径时 | S | 无独立 A2.x finding（P1-MA2-002/009 已实现修复，残留为 successor） |
| 8 | 凭证 `reversedVoucherId` 双向回链 | S2 | 报表需求驱动时 | S | 无独立 A2.x finding（红冲闭环功能完整，双向回链为 successor） |

> §对账差异登记 #3 覆盖：#7/#8 仅 S2 覆盖（owner doc 内嵌但 arm-index 无独立行），本复查已纳入避免遗漏。

---

## 2. 逐项四任务核证（段 2，§6 MA3 适配）

> 四任务：① 触发条件是否已满足（grep 实仓代码/config/SPI）；② 是否该回队；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

### 2.1 #1 GRNI 正向 receive→invoice 自动冲回

- **① 触发条件状态**：**未满足**。实仓 grep `repostPurchaseInput` 跨 `module-inventory` 全模块 **零命中**（SPI 缺失）。owner doc `purchase/returns.md §暂估应付冲减` + `finance/posting.md §GRNI 暂估冲回 documented simplification:95` 明示「reverseApprove 反冲回需 inventory 域 `repostPurchaseInput` SPI 缺失」+「reverse() 仅全额红冲致部分开票少计暂估」+「跨期语义」三重前置未落地。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足；属跨域依赖，inventory 域 SPI 落地归 A3.2 范畴）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P1-MA2-001` 行 + owner doc 双锚点均存在）。
- **④ README 覆盖复核**：`docs/backlog/README.md` 无独立 GRNI design successor 行（其 81 行经 M0.3 §对账差异登记 #4 核实为 E2E 测试 successor，非 design successor）。design successor 经 S1（arm-index `P1-MA2-001` 行内 successor 声明）+ S2（owner doc 内嵌）覆盖，**无「已登记但从未触发」风险**（触发条件 SPI 缺失明确，未误标 done）。

### 2.2 #2 GL 余额维护引擎（opening/closing）

- **① 触发条件状态**：**未满足**。实仓 grep 证实 `AnnualCloseService.java:51` 注释「ErpFinGlBalance 在当前阶段未由过账引擎维护」+ `ProfitLossClosingService.java:43` 同注释 + `BudgetVoucherGenerator.java:36`「过账引擎本就不维护 GlBalance」——postVoucher 路径不维护 opening/closing 余额，年初余额 populate 仅聚合本年度分录净额（`AnnualCloseService.populateNextYearOpening:137-179`）。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足；补 GL 余额维护触及 PostingProcessor + 全 Provider 跨模块架构变更，属 ask-first 会计过账保护区域，修复归 MR1 而非本审计）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，owner doc `period-close.md §已知简化「年初余额非累计」:309-314` 显式 successor 标注）。
- **④ README 覆盖复核**：无独立 design successor 行（同 #1，README 81 行为 E2E 测试 successor）。S1+S2 覆盖充分，无悬空。
- **结构性约束标注**：本项是 #3（累计余额对账）的**共同前置**——回队顺序依赖：#2 须先于 #3 落地。

### 2.3 #3 累计余额对账（辅助账跨年）

- **① 触发条件状态**：**未满足**（依赖 #2 GL 余额维护 successor 落地）。owner doc `period-close.md §已知简化「辅助账跨年对账作用域」:316-319` 明示「当前为单年作用域对账，累计余额对账需 GL 余额维护 successor」。`P1-MA2-019` 主缺陷（`sumArApOpenFunctional` 年度过滤）已代码修复，仅累计余额对账残留。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足；显式依赖 #2 前置）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖）。
- **④ README 覆盖复核**：无独立 design successor 行。S1+S2 覆盖充分。
- **结构性约束标注**：**回队顺序依赖 #2**——#2 落地后方可回队 #3。

### 2.4 #4 反结账完整审批流（xwf）

- **① 触发条件状态**：**未满足**。successor 原依赖浏览器层 xwf 审批路径，`docs/plans/2026-07-09-2330-1-use-workflow-browser-e2e-feasibility.md` 裁决 xwf 浏览器层 **NOT FEASIBLE**（`WorkflowEngineImpl.newSteps` fallback `sysUser(0)` 与 `NopAuthUser.userId` `tagSet="seq"` 冲突）。owner doc `state-machine.md §已知限制：浏览器层 xwf 审批路径:266-275` + `period-close.md §反结账审批:321-325` 显式标注 successor 触发条件 = xwf 落地。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——xwf 浏览器层 NOT FEASIBLE）。**关键边界**（methodology §MA2↔MA3 协作）：successor 维持 backlog ≠ finding 不修复。若 A2.1 裁决其关闭为「静默降级」则该 **finding** 经 A2.1 重开入 MR1（Q4 强制，修复须找**非 xwf 替代审批机制**，Q4 无技术不可行例外）。经核实 A2.1 裁决 #4 关闭为「有意设计（§4(i) 成立）」——故 finding 不重开，successor 亦维持 backlog，两者一致不冲突。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖）。
- **④ README 覆盖复核**：无独立 design successor 行。S1+S2 覆盖充分。

### 2.5 #5 FX 重估前期 reversal + 期间过滤（IAS 21 完整语义）

- **① 触发条件状态**：**未满足**。实仓 grep `ExchangeRevaluationService.revalueArAp:103` 查询所有未核销外币项（不按期间过滤），重估后不更新 `openAmountFunctional`、不 reversal 前期 FX 凭证。`TestErpFinExchangeRevaluation.java:105-112` 测试注释显式记录「documented simplification：无前期 reversal」+ 断言「P1 FX 凭证未被冲销」。owner doc `period-close.md §FX 重估无前期 reversal:331-336` config-gated `erp-fin.exchange-revaluation-enabled` 关闭默认。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——IAS 21 完整语义需求未提 + config-gated 关闭默认）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖）。
- **④ README 覆盖复核**：无独立 design successor 行。S1+S2 覆盖充分。

### 2.6 #6 凭证幂等键字面 UK 方向 A/B/C/D（P0-MA2-018 successor）

- **① 触发条件状态**：**实质未满足**（须更深设计变更，非字面 UK 回归）。实仓 grep `module-finance/model/app-erp-finance.orm.xml` 证实判别列 `acctSchemaId`/`postingType`/`isReversed` 存在于 `ErpFinVoucher`（`:418/:422/:426`）但**不在** `ErpFinVoucherBillR`（billR 仅有 `(voucherId)` 非唯一索引）。deferred plan `2026-07-28-1249-arm-fix-p0-ma2-018` Plan Status=deferred，独立 plan-audit REJECT/BLOCK（字面 UK 与红冲「同键 2 行」+ 多账套「同键 N 行」+ 软删除三重契约冲突）。
- **② 回队决策**：**回队 MR1**（Q4 强制）。successor 触发条件实质 = 「须更深设计变更（反范式化判别列到 billR 或部分唯一索引）」，**非**「技术不可行」——Q4 无技术不可行例外通道（§5），故应回队 MR1。**与 A2.1 复查结论交叉一致**：A2.1 裁决 `P0-MA2-018` 三判据均不成立→静默降级→Q4 强制重开 MR1（R1.0 展开为 RC-R1.n，既有 P0 deferred 边界：经 MA2 重新分级入 MR1 非 MR0）。修复行触及 ORM 结构变更 + 会计过账逻辑，须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P0-MA2-018` 行 deferred plan 含方向 A/B/C/D）。
- **④ README 覆盖复核**：无独立 design successor 行。S1（arm-index + deferred plan）覆盖充分。

### 2.7 #7 多币种全域源币金额迁移（其余域 Provider）

- **① 触发条件状态**：**未满足**。owner doc `finance/posting.md:451-453` 明示「P2P（purchase 域 Provider）+ O2C（sales 域 Provider）已迁移双字段；其余域 Provider 单币种 fallback（全域迁移 successor，`Deferred But Adjudicated`）」。实仓 grep `amountSource|amountFunctional` 跨 `module-inventory`/`module-assets`/`module-hr`/`module-maintenance` 主源码：assets 命中仅为 `erp-ast-api` 生成的 InputBean/OutputBean 字段（generic bean，非 Provider 显式填充），inventory/hr/maintenance 主源码零业务填充命中——确认其余域 Provider 仍单币种 fallback。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——「各域启用多币种业务路径时」未触发，其余域业务路径仍单币种）。
- **③ 补登记**：**需补登记（M0.3 §对账差异登记 #3）**。本项仅 S2 覆盖（owner doc `posting.md:453` 内嵌），arm-index 无独立 successor 行（`P1-MA2-002/009` 已实现修复关闭，残留 successor 仅 owner doc 内嵌）。按 §7 纪律，补登记以实仓 grep 为准——本复查经 grep 核实 owner doc 内嵌 successor 描述与实仓一致（其余域 Provider 单币种 fallback 属实），**补登记入 arm-index finance successor 分区**（段 6 衔接），不松弛。
- **④ README 覆盖复核**：无独立 design successor 行。补登记后 S1+S2 双源覆盖。

### 2.8 #8 凭证 `reversedVoucherId` 双向回链

- **① 触发条件状态**：**未满足**。实仓 grep `reversedVoucherId` 跨 `module-finance` 全模块**零命中**（字段未落地）。owner doc `state-machine.md:42` 明示「红冲在原凭证上置 `isReversed=true` 单边标记（保留 POSTED），不建立 `reversedVoucherId` 双向回链。红冲闭环功能完整（含 `reverseVoucher` 与业财回链红冲）。`reversedVoucherId` 双向回链为 successor（报表需求驱动时实现）」。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——「报表需求驱动时」未触发，红冲闭环功能完整无活跃缺陷）。
- **③ 补登记**：**需补登记（M0.3 §对账差异登记 #3）**。本项仅 S2 覆盖（owner doc `state-machine.md:42` 内嵌），arm-index 无独立行（红冲闭环功能完整，仅双向回链为报表 successor）。按 §7 纪律，经 grep 核实 owner doc 描述与实仓一致（字段未落地属实），**补登记入 arm-index finance successor 分区**（段 6 衔接），不松弛。
- **④ README 覆盖复核**：无独立 design successor 行。补登记后 S1+S2 双源覆盖。

---

## 3. 既有行为证据（段 3，复用既有 arm 审计，§去重协议）

> 本复查为 successor 触发条件复查（需求契约视角），不重做 doc↔code 文本一致性 / 状态机行为 / 代码质量。实现证据复用既有 arm MA2/MA4 报告 + A2.1 RC 复查报告已证实的代码路径，仅列锚点供四任务核证溯源。

| # | successor 项 | 代码锚点（复用 arm MA2/MA4 + A2.1 RC 已证实） | 既有证实报告 |
|---|-------------|----------------------------------------------|-------------|
| 1 | GRNI 自动冲回 | `InvPostingDispatcher.java:203`（PURCHASE_INPUT billHeadCode=stockMove.code）+ `PurInvoicePostingDispatcher.java:74`（AP_INVOICE billHeadCode=invoice.code，无共享键）+ inventory `repostPurchaseInput` SPI 零命中 | `2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`；A2.1 RC §3 |
| 2 | GL 余额维护 | `AnnualCloseService.populateNextYearOpening:137-179`（仅本年聚合）+ `:51` 注释 + `ProfitLossClosingService:43` + `BudgetVoucherGenerator:36` | `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md:40`；A2.1 RC §3 |
| 3 | 累计余额对账 | `AnnualCloseService.sumArApOpenFunctional:237-257`（已增年度过滤，残留累计对账 successor） | 同上 MA2 状态机报告 :41；A2.1 RC §3 |
| 4 | 反结账审批 | `ErpFinAccountingPeriodProcessor.reverseClose:278-281` config kill-switch + `isReverseCloseApprovalRequired:653-656` | 同上 MA2 状态机报告 :42, :273-297；A2.1 RC §3 |
| 5 | FX 前期 reversal | `ExchangeRevaluationService.revalueArAp:103`（无期间过滤 + 无前期 reversal）+ `TestErpFinExchangeRevaluation:105-112` 测试注释 | 同上 MA2 状态机报告 :44；A2.1 RC §3 |
| 6 | 字面 UK A/B/C/D | `ErpFinPostingProcessor.alreadyPosted:472` TOCTOU pre-check + `erp_fin_voucher_bill_r` 无判别列（`app-erp-finance.orm.xml` billR 仅有 `(voucherId)` IDX；判别列在 voucher `:418/:422/:426`） | `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md §11`；`2026-07-28-1510-arm-ma2-multi-company-isolation.md §6.4`；A2.1 RC §3 |
| 7 | 多币种全域迁移 | `ErpFinPostingProcessor.persistVoucher`（按 Provider 显式 `fact.amountSource`/`fact.amountFunctional` 写库，未设置 fallback `fact.amount`）+ 其余域 Provider 单币种 fallback | `finance/posting.md:451-453`（P1-MA3-039 R1.9 已核实注记）；A2.1 RC §3 |
| 8 | reversedVoucherId | `ErpFinPostingProcessor.reverseProcess` + `markOriginalVoucherReversed`（原凭证置 `isReversed=true` 单边标记）+ `reversedVoucherId` 字段零命中 | `2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`；A2.1 RC §3 |

---

## 4. 运行时行为证据（段 4，复用既有 arm MA2/MA3，§去重协议）

> 本 mission MA3 = successor 触发条件复查（需求契约视角），与 audit-remediation MA2（状态机/链路行为视角）/ MA3（doc↔code drift）/ MA4（代码质量）维度不重叠（methodology §去重协议 §MA2(本)↔MA3(audit-remediation) 边界）。既有 arm 报告 + A2.1 RC 报告已证实的运行时行为直接引用：

- **#1 GRNI**：GL 2202 暂估应付双计 + 1403/1401 存货双计，辅助账层（ErpFinArApItem）不受影响——经 P2P E2E 报告 + A2.1 RC §4 证实。
- **#2/#3 GL 余额/累计对账**：经 `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md` 逐项运行时复核确认「仅治理缺陷/数值漂移/作用域精度」，状态机迁移路径正确，无运行时数据破坏升级。
- **#4 反结账审批**：`reverseClose` 为 `@BizMutation`，kill-switch 默认 true 直接拒绝，false 时由角色权限门控（无独立审批 action）——经 MA2 状态机报告 :273-297 + A2.1 RC §4 证实。
- **#5 FX 重估**：当期 spot-rate 重估，前期汇兑损益不冲回，累计漂移（非 IAS 21 完整语义），config-gated——经 MA2 状态机报告 :44 + A2.1 RC §4 证实。
- **#6 字面 UK**：并发 post/兜底重试可双 INSERT 重复凭证（`ErpFinDeferredPostingRetryHelper` REQUIRES_NEW 同 TOCTOU race），GL 借贷双计——经 `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md` + A2.1 RC §4 证实。
- **#7 多币种迁移**：P2P/O2C 双字段生效，其余域单币种 fallback（source==functional==amount 三者相等无币种折算）——经 `posting.md:451` P1-MA3-039 R1.9 核实注记 + A2.1 RC §4 证实。
- **#8 reversedVoucherId**：红冲闭环功能完整（`reverseVoucher` + 业财回链红冲 + `isReversed=true` 单边标记），仅无双向回链——经 `2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md` + A2.1 RC §4 证实。

---

## 5. 复查结论（段 5，§6 MA3 适配：触发条件状态 + 回队决策）

> 复查结论三分：`回队 MR1`（触发条件已满足 / Q4 强制）/ `维持 backlog successor`（触发条件未满足）/ `补登记`（owner doc 内嵌但 arm-index 无行）。

### 5.1 逐项复查结论

| # | successor 项 | 触发条件状态 | 证据 | 回队决策 | 与 A2.x 关闭裁决交叉 |
|---|-------------|-------------|------|---------|---------------------|
| 1 | GRNI 自动冲回 | ❌ 未满足（SPI 缺失） | inventory `repostPurchaseInput` 零命中 | **维持 backlog successor** | #1 ↔ `P1-MA2-001`（A2.1：有意设计）一致 |
| 2 | GL 余额维护引擎 | ❌ 未满足 | `AnnualCloseService:51` + `ProfitLossClosingService:43` 注释 | **维持 backlog successor** | #2 ↔ `P1-MA2-018`（A2.1：有意设计）一致；**是 #3 前置** |
| 3 | 累计余额对账 | ❌ 未满足（依赖 #2） | owner doc `period-close.md:319` 显式依赖 #2 | **维持 backlog successor** | #3 ↔ `P1-MA2-019`（A2.1：有意设计）一致；**回队顺序依赖 #2** |
| 4 | 反结账审批流 | ❌ 未满足（xwf NOT FEASIBLE） | `2026-07-09-2330-1` 裁决 + owner doc §已知限制 | **维持 backlog successor** | #4 ↔ `P1-MA2-020`（A2.1：有意设计）一致；successor 维持 backlog ≠ finding 重开（finding 经 A2.1 裁决有意设计亦不重开，两者一致） |
| 5 | FX 前期 reversal | ❌ 未满足 | `ExchangeRevaluationService.revalueArAp:103` 无期间过滤 + 测试注释 | **维持 backlog successor** | #5 ↔ `P1-MA2-022`（A2.1：有意设计）一致 |
| 6 | 字面 UK A/B/C/D | ❌ 实质未满足（须更深设计变更） | billR 无判别列 + deferred plan-audit REJECT | **回队 MR1（Q4 强制）** | #6 ↔ `P0-MA2-018`（A2.1：**静默降级，Q4 强制重开 MR1**）**交叉一致** |
| 7 | 多币种全域迁移 | ❌ 未满足 | `posting.md:453` + 其余域 Provider 单币种 fallback grep 核实 | **维持 backlog successor** + **补登记** | 无独立 A2.x finding（P1-MA2-002/009 已实现修复）；§对账差异 #3 |
| 8 | reversedVoucherId 双向回链 | ❌ 未满足 | `reversedVoucherId` 跨 module-finance 零命中 | **维持 backlog successor** + **补登记** | 无独立 A2.x finding（红冲闭环完整）；§对账差异 #3 |

### 5.2 统计

- **回队 MR1**：1 项（#6 字面 UK A/B/C/D，Q4 强制，与 A2.1 `P0-MA2-018` 重开交叉一致）
- **维持 backlog successor**：8 项（#1-#8 全部维持 backlog；其中 #6 同时回队 MR1——其「触发条件实质 = 须更深设计变更」经 Q4 强制实现，backlog successor 登记同步消解于 MR1 修复行）
- **补登记**：2 项（#7 多币种全域迁移 / #8 reversedVoucherId 双向回链，§对账差异 #3，owner doc 内嵌但 arm-index 无独立行）
- **本审计新发现 P0**：0 项（无 MR0 即时通道触发）

### 5.3 结构性约束（回队顺序依赖）

- **#2 → #3 前置链**：GL 余额维护引擎（#2）是累计余额对账（#3）的共同前置。MR1 R1.0 展开时若 #2/#3 同时回队，须按 #2 先于 #3 排序。
- **#1 跨域依赖**：GRNI 自动冲回（#1）依赖 inventory 域 `repostPurchaseInput` SPI（跨域，A3.2 范畴）。inventory SPI 落地方属 A3.2 复查触发条件，本 A3.1 只登记依赖不裁决跨域回队。
- **#6 触及保护区域**：字面 UK 修复（#6）触及 ORM 结构变更（billR 判别列 + UK）+ 会计过账逻辑，须 ask-first + 独立 plan-audit（§5）。

---

## 6. 与 arm-index 衔接（段 6，§7「复用 or 新增」裁决）

> §7 规则：successor 项均源自既有 arm finding，本复查原则上**复用既有 finding ID**追加 RC MA3 注记；仅当发现 owner doc 内嵌但 arm-index 无独立行的 successor（#7/#8）才补登记。

### 6.1 逐项「复用 or 补登记」裁决

| # | successor 项 | arm-index grep 结果 | 裁决 | 操作 |
|---|-------------|---------------------|------|------|
| 1 | GRNI 自动冲回 | 既有 `P1-MA2-001` 行含 successor 声明 | **复用** | 既有行追加「RC MA3 复查（A3.1）：触发条件未满足[SPI 缺失]→维持 backlog successor」注记 |
| 2 | GL 余额维护 | 既有 `P1-MA2-018` 行含 successor 声明 | **复用** | 既有行追加「RC MA3 复查（A3.1）：触发条件未满足→维持 backlog successor；是 #3 前置」注记 |
| 3 | 累计余额对账 | 既有 `P1-MA2-019` 行含 successor 声明 | **复用** | 既有行追加「RC MA3 复查（A3.1）：触发条件未满足[依赖 #2]→维持 backlog successor；回队顺序依赖 #2」注记 |
| 4 | 反结账审批 | 既有 `P1-MA2-020` 行含 successor 声明 | **复用** | 既有行追加「RC MA3 复查（A3.1）：触发条件未满足[xwf NOT FEASIBLE]→维持 backlog successor（successor 维持 ≠ finding 重开，finding 经 A2.1 裁决有意设计亦不重开）」注记 |
| 5 | FX 前期 reversal | 既有 `P1-MA2-022` 行含 successor 声明 | **复用** | 既有行追加「RC MA3 复查（A3.1）：触发条件未满足→维持 backlog successor」注记 |
| 6 | 字面 UK A/B/C/D | 既有 `P0-MA2-018` 行含 deferred plan + 方向 A/B/C/D | **复用** | 既有行追加「RC MA3 复查（A3.1）：触发条件实质=须更深设计变更→**回队 MR1（Q4 强制）**，与 A2.1 重开结论交叉一致」注记 |
| 7 | 多币种全域迁移 | arm-index 无独立行（`P1-MA2-002/009` 已实现修复关闭，残留 successor 仅 owner doc 内嵌） | **补登记** | `P1-MA2-002` 行追加「RC MA3 复查（A3.1）：其余域 Provider 单币种 fallback 残留 successor 补登记[§对账差异 #3]，触发条件=各域启用多币种业务路径时，未满足→维持 backlog」注记 |
| 8 | reversedVoucherId 双向回链 | arm-index 无独立行（红冲闭环功能完整，仅双向回链为报表 successor） | **补登记** | 入 arm-index finance successor 分区：「RC MA3 补登记（A3.1，§对账差异 #3）：凭证 `reversedVoucherId` 双向回链 successor——红冲闭环完整仅无双向回链，触发条件=报表需求驱动时，未满足→维持 backlog」 |

**裁决依据**：#1-#6 为既有 arm finding 的同一根因/同一控制点 successor，复用既有 ID 追加 RC MA3 注记；#7/#8 经 grep 核实 owner doc 内嵌 successor 描述与实仓一致（§7 纪律，非松弛），补登记入 arm-index。**不新建 `P*-RC-xxx`**（禁止未经比对直接新建）——#7/#8 补登记为 successor 行注记/分区追加，非新 finding 编号。

### 6.2 双向可追溯

- **回队项 ↔ MR1 R1.0 预留展开行**：#6（`P0-MA2-018` successor）→ MR1 R1.0 展开为 `RC-R1.n`（修复行须含 finding ID 交叉引用 `P0-MA2-018` + 触及保护区域标注「ORM 结构变更 + 会计过账逻辑」+ Skill）。与 A2.1 RC §6.2 重开项登记**同一 RC-R1.n**（两面归一：A2.1 裁决 finding 重开，A3.1 裁决 successor 回队，同一 MR1 修复行承接）。
- **维持 backlog 项 ↔ A3.x successor 登记**：#1-#5/#7/#8 维持 backlog，交叉引用本 A3.1 报告 + arm-index successor 注记。
- **arm-index 回填**：§6.1 注记已写入 `arm-index.md`（既有 7 行追加 + #8 补登记分区项）。

---

## 7. 静态存疑点清单（段 7，供 MA4 A4.1 展开）

> L5 无法静态定论、需运行时确认的点。本复查为 successor 触发条件复查（读 arm-index/owner doc/实仓代码/config），以下为复查中静态无法定论、建议 MA4 运行时确认的点：

1. **#6 字面 UK 修复方向运行时验证**：反范式化判别列（方向 B：billR 加 acctSchemaId/postingType/isReversed）或部分唯一索引（方向 A）落地后，并发 `IErpFinVoucherBiz.post` + `ErpFinDeferredPostingRetryHelper` 兜底重试 + 人工重试的 TOCTOU 实际触发面需运行时并发探针确认（静态已确认 race window 存在，触发频率依赖部署负载）——建议 MR1 修复行（RC-R1.n）附并发负向测试。与 A2.1 RC §7 存疑点 1 同一（两面归一）。
2. **#4 反结账 `=false` 时权限门控实际强度**（复用 A2.1 RC §7 存疑点 2）：`reverseClose` 为 `@BizMutation`，理论上经角色-resource 种子门控，但 MA2 报告指出「无显式 @BizAuth，依赖配置层 enableActionAuth」。`=false` 时「任何能调 mutation 的角色均可反结账」的实际权限强度需运行时角色矩阵确认——交接 #4 successor（完整审批流）+ A2.18 权限注解审计 successor。

> 其余 6 项（#1/#2/#3/#5/#7/#8）的运行时行为已由既有 arm MA2/MA4 报告 + A2.1 RC §4 充分证实（§4），无新增静态存疑点。

---

## 8. 过程纪律自检（段 8，§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（actual 见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码不反映 actual vs baseline），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以 checker 脚本退出码作为门控通过依据**。**本审计无生产代码变更（纯审计报告 + arm-index 文档注记），checker 无回归风险**——actual 计数与本审计行为正交（未触及任何生产代码），任何 actual vs baseline 差异均非本审计引入。

  | 规则 | 基线（compliance-baseline.md §BASELINE machine-readable） | actual（本次实测） | 漂移 | 归因 |
  |------|-------------------------------|-------------------|------|------|
  | R1a | 0 | 0 | 0 | — |
  | R1b | 0 | 0 | 0 | — |
  | R1c | 0 | 0 | 0 | — |
  | R1d | 14 | 14 | 0 | — |
  | R2a | 34 | 34 | 0 | — |
  | R2b | 229 | 229 | 0 | — |
  | R2c | 1382 | 1382（生产代码总计） | 0 | — |
  | R2d | 34 | 34 | 0 | — |

  > 本审计仅产出本报告 + `arm-index.md` 注记（纯文档），未触及 `module-*/` 任何生产代码。actual 全规则 = baseline，零漂移，无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（见来源 plan Closure Gates）。
- [x] **与 arm-index 交叉去重声明**：本报告全部 8 项 successor 已按 §7 规则 grep arm-index 同域同控制点后给出「复用 or 补登记」裁决（§6.1），无未经比对直接新建的 `P*-RC-xxx` finding（#7/#8 为 successor 行注记/分区追加，非新 finding 编号）。

---

## 9. 与既有审计差异增量声明（段 9，§去重协议）

本报告与既有 arm 审计（`docs/audits/2026-07-2*-arm-ma2-*` / `arm-ma3-*` / `arm-ma4-*`）+ A2.1 RC 复查报告（`2026-08-06-1400-rc-ma2-a2-1-2-finance-simplification-review.md`）的差异增量：

- **复用既有证据**（不重复验证）：
  - `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（#6 P0-MA2-018 并发缺陷已证实）；
  - `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`（#2/#3/#4/#5 状态机行为已证实，运行时复核无升级）；
  - `2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（#1 GRNI 冲回行为已证实）；
  - `2026-07-28-1510-arm-ma2-multi-company-isolation.md §6.4`（#6 多公司维度复核）；
  - `2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（#8 红冲闭环行为已证实）；
  - `2026-08-06-1400-rc-ma2-a2-1-2-finance-simplification-review.md`（A2.1/A2.2 方案 B 关闭裁决七项 §4 三判据核证 + 交叉引用）。

- **本复查只补的差异增量**：**successor 触发条件是否已满足 + 是否该回队**——从 methodology §MA3 四任务（① 触发条件状态 grep 实仓验证 / ② 回队决策 / ③ 补登记 / ④ README 覆盖复核）出发，逐项核证 8 项 finance successor 的触发条件现状。这是既有 arm 审计（doc↔code / 状态机行为 / 代码质量维度）+ A2.1 RC（方案 B 关闭裁决正当性维度）未覆盖的「successor 触发条件完整性 + 回队决策」维度（methodology §去重协议 §MA2↔MA3 协作——关闭裁决归 A2.x，successor 触发条件归 A3.x，交叉引用不重复）。

- **不重复**：不重做 doc↔code 文本一致性（audit-remediation MA3 已收口）、不重做状态机/链路行为（arm MA2 已收口）、不重做代码质量（arm MA4 已收口）、不重审方案 B 关闭裁决本身（A2.1 RC 已收口，本 A3.1 只复查 successor 触发条件，两面交叉引用）。

---

## 结论

finance MA3 successor 复查（A3.1）完成：8 项 design-level successor 逐项经 §MA3 四任务核证。

- **回队 MR1**：1 项（#6 字面 UK A/B/C/D，Q4 强制实现，触发条件实质=须更深设计变更；与 A2.1 `P0-MA2-018` 重开结论交叉一致；修复须更深设计变更非字面 UK，触及 ORM + 会计过账保护区域须 ask-first）。
- **维持 backlog successor**：8 项（#1-#8 全部；其中 #6 同时回队 MR1，backlog successor 登记同步消解于 MR1 修复行）。
- **补登记**：2 项（#7 多币种全域迁移 / #8 reversedVoucherId 双向回链，§对账差异 #3，owner doc 内嵌但 arm-index 无独立行，经 grep 核实补登记）。
- **结构性约束**：#2 是 #3 前置（回队顺序依赖）；#1 跨域依赖 inventory SPI（A3.2 范畴）；#6 触及保护区域（ask-first + 独立 plan-audit）。
- **arm-index 衔接**：8 项全部复用既有 ID 追加 RC MA3 注记（无新 `P*-RC-xxx`）；#7/#8 补登记入 arm-index finance successor 分区；#6 回队标记与 A2.1 同一 MR1 RC-R1.n 修复行（两面归一）。
- **本审计无生产代码变更**（纯报告 + arm-index 文档注记），§9 真相源冻结条款遵守（未修改 product-scope / owner doc 需求契约段落 / arm-index 已关闭 finding 的关闭事实 / backlog README）。
