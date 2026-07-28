# MA3 owner doc vs 代码 drift 审计（A3.3 finance / A3.4 manufacturing / A3.5 pur+sal+inv）

> Report ID: `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift`
> 里程碑：MA3（文档-实现一致性层）/ 工作项 A3.3 + A3.4 + A3.5
> 审计维度：owner doc vs 代码 drift（`multi-dimensional-audit-prompt.md` 7 维度适配"doc vs code"主题 + 项目特定维度[过账/会计/配置门控]）
> 审计日期：2026-07-28
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`（多维挑战 7 维度 + 反窄化自检，适配"doc vs code drift"主题）
> 来源计划：`docs/plans/2026-07-28-1953-1-audit-remediation-ma3-owner-doc-vs-code-drift.md`
> 审查对象：`docs/design/{finance,manufacturing,purchase,sales,inventory}/`（19+12+8+7+8=54 文件）vs 各域 `module-<domain>/erp-<short>-service/` Java + `erp-<short>-dao/` 实体 + `model/app-erp-<domain>.orm.xml`
> 互补关系：本审计做设计 **vs 代码** 逐项 drift 比对（A3.3-A3.5）。Sibling A3.1（done）审设计文档**内部**质量；A3.2（done）做前瞻性缺失扫描。MA2（done）审状态机正确性并在审计过程中登记了多处 owner-doc drift（本审计复核）。
> 审计性质：纯文档-代码比对审查（不改应用代码；产出为本报告 + arm-index P1/P2 登记 + scope matrix §2.3 终态标记）。drift 修复在 MR2（文档类）/MR1 或 P0 通道（代码侧）批量进行。

## 0. 裁决

**Verdict: FAIL（有 drift）**

理由：本审计发现 **零 BLOCKER**（无"设计与代码直接矛盾且破坏业财一致或数据正确"的 doc↔code 冲突——所有 blocker 级 drift 均为"实施者按错误基线实现"风险而非活跃数据破坏），但发现 **22 项 NEW MAJOR → P1-MA3-024~045**（目标 MR2 文档类 / MR1 代码侧，与 A3.1/A3.2 已登记 P1-MA3-001~023 经交叉去重无重复登记）+ **13 项 NEW MINOR → P2-MA3-023~035** watch-only。

**drift 分布**：finance 域 drift 密度最高（16 P1 + 9 P2 = 25 项），集中在**配置/门控维度**（8 项）和**字段/实体语义维度**（5 项）——finance 有 19 个 owner doc 文件、~30 个 businessType、~120 个 config key，是全域最复杂域，drift 风险与复杂度正相关。manufacturing 域 drift 集中在**业务规则/计算维度**（material-reservation.md 整个子系统未实现 [blocker] + UC-MFG-12 差异公式列表错误）。pur+sal+inv 三域 drift 最少（仅 2 项 P2 minor），核心机制文档与代码高度一致——MA2 已登记的 drift 覆盖了大部分问题。

**MA2 owner-doc drift 复核结论**：范围内域簇（finance P1-MA2-031~034 / mfg P1-MA2-035~038 / pur P1-MA2-049~051 / sal P1-MA2-056~057 / inv P1-MA2-062~063）分类与归属**全部确认一致，无升级/降级/重新分类**。本审计横扩至状态机以外维度后发现的新 drift 均为文档类（owner doc 错误/过时/矛盾）或配置类（config key 漂移），目标 MR2 文档修复；若发现代码侧确有缺陷则标注走 MR1。**零 P0**（本审计为文档-实现一致性层，原则上不产生 P0；若 drift 致错误实现风险且代码侧确有缺陷，升级标注走 P0 即时通道——本审计范围内无此情况）。

**关键裁决**：
- finance 预算余量公式 drift（P1-MA3-025）：doc 说 `budget − commitment − actual` 三项式，code javadoc 说 `budgetBalance − actualBalance`（actualBalance=NORMAL/NULL，不含 COMMITMENT）。**与 P1-MA2-084 存在矛盾读法**（MA2-084 称 COMMITMENT 经 actualBalance 通道被减去，结果等价正确）——本审计裁决：doc-vs-javadoc drift 确认存在（doc 三项式 vs javadoc 二项式），code 内部是否隐含 COMMITMENT 经 actualBalance 通道需 MR1 裁决核实。注册为 P1（doc drift 确认）+ 标注与 MA2-084 的交叉核实需求。
- finance `IErpFinVoucherBiz.reverse()` REQUIRES_NEW drift（P1-MA3-030）：posting.md:399 明示"不像 post() 叠加 REQUIRES_NEW"，code ErpFinVoucherBizModel.java:79 实际有 REQUIRES_NEW + O-7 注释承认 doc 未更新。**blocker 级 doc→code drift**——实施者按 doc 假设 reverse() 跟随调用方事务，实际独立事务，红冲失败不回滚调用方主事务。

---

## Phase 1 — finance owner doc vs 代码 drift（A3.3，S 级）

审查目标：`docs/design/finance/`（19 文件）；`module-finance/erp-fin-service/`（122 Java 文件）；`module-finance/model/app-erp-finance.orm.xml`。

### 1.1 维度裁决（7 维度逐项）

#### 维度 1：状态迁移/工作流 drift

**裁决：有 NEW drift（1 blocker + 1 minor）+ MA2 复核确认**

- **P1-MA3-024**（blocker，NEW）：**期间状态机 CLOSED 语义三源冲突 + NEVER_OPENED 缺失**。`state-machine.md:130,138` 用 CLOSED 表示"未开启"（"未开启（CLOSED）| 期间未到或已财务关闭"），`period-close.md:153-158` 用 CLOSED 表示"已结账待复核"，`use-cases.md:11` 列 4 态无 NEVER_OPENED。Code + ORM（`ErpFinConstants.java:139-143` + `orm.xml:200-206`）有 5 态含 NEVER_OPENED（`generateNextYearPeriods` 次年 2-12 月置 NEVER_OPENED），CLOSED=已结账。三源互斥：按 state-machine.md 写守卫会与 period-close.md 反向。**MA2 复核**：确认并扩展 P1-MA2-033（NEVER_OPENED→OPEN 缺失）——P1-MA2-033 是迁移路径缺失，本 finding 是 CLOSED 语义在三个文档间互斥冲突。
- **P2-MA3-023**（minor，NEW）：**reverseClose 路径文档与代码状态迁移不一致**。`period-close.md:186` 声明 CLOSED_FINAL→CLOSING→OPEN 三步，code `ErpFinAccountingPeriodProcessor.java:291` 直接 CLOSED_FINAL→OPEN 一步。
- **MA2 复核确认**（无新 ID）：
  - P1-MA2-031（DRAFT→CANCELLED 不可达 + 红字凭证终态归属未定义）：**确认**。state-machine.md:20,35,42 声明 DRAFT→CANCELLED 迁移，code `ErpFinVoucherBizModel.java:86-114` 无 cancelVoucher mutation；state-machine.md:39 终态列表无 isReversed 槽位。分类维持 P1。
  - P1-MA2-032（IGNORED 凭证悬挂缺告警闭环）：**确认**。`ErpFinAccountingPeriodProcessor.java:589-602` findUnresolvedPostingExceptionKeys 仅扫 PENDING+RETRYING，IGNORED 不进结账门控。分类维持 P1。
  - P1-MA2-033（NEVER_OPENED→OPEN 迁移路径缺失）：**确认并扩展**→见 P1-MA3-024。
  - P1-MA2-034（carryForward 不校验源年度全 CLOSED 前置）：**确认**。分类维持 P1。

#### 维度 2：业务规则/计算 drift

**裁决：有 NEW drift（1 major + 2 minor）**

- **P1-MA3-025**（major，NEW）：**预算余量公式 doc 三项式 vs code javadoc 二项式**。`budget.md:59` + `use-cases.md:212,253` 声明 `available = budget − commitment − actual`（三项式）。Code `ErpFinBudgetControlBiz.java:40-42,74-76` javadoc 声明 `actualBalance = postingType=NORMAL（含 NULL）`（不含 COMMITMENT），`available = budgetBalance − actualBalance`（二项式）。**与 P1-MA2-084 交叉**：MA2-084 称 COMMITMENT 经 actualBalance 通道被减去（结果等价正确但语义混淆）；本审计 javadoc 读法是 COMMITMENT 不在 actualBalance 内。两者对同一 code 的读法矛盾——doc 三项式 vs code javadoc 二项式的 drift 确认存在，code 内部是否隐含 COMMITMENT 需 MR1 裁决核实。**drift 方向：doc→code**。
- **P2-MA3-024**（minor，NEW）：**银行对账余额调节恒等式简化未在文档标注**。`bank-reconciliation.md:106` 声明完整四项式（bankBalance + amtInTransitIn − amtInTransitOut = bookBalance + amtBankNotInBooks），code `BankReconciliationBuilder.java:67-73` 实现简化二项式（在途=0 为 Non-Goal）。§业务规则5 仍以完整恒等式为硬规则。
- **P2-MA3-025**（minor，NEW）：**坏账计提范围排除项"争议发票"doc 声明为规则 vs code 标注为 config-gated deferred**。`bad-debt.md:171-174,257` 声明排除争议发票，code `BadDebtProvisionCalculator.java:22,50-59` 标注 exclude-disputed 为 config-gated 预留未实现。

#### 维度 3：字段/实体语义 drift

**裁决：有 NEW drift（1 blocker + 3 major）**

- **P1-MA3-026**（blocker，NEW）：**postingType 字典三处真相源不一致**。`budget.md:96` 用 `ACTUAL=10/BUDGET=20/COMMITMENT=30/RESERVATION=40`（数值码 + 含 RESERVATION），`README.md`/`state-machine.md` 非正式用 NORMAL/REVERSAL/BUDGET/COMMITMENT，ORM `orm.xml:40-50` 有 7 个字符串值（NORMAL/OPENING_BALANCE/ADJUSTMENT/CLOSING/REVERSAL/BUDGET/COMMITMENT，无 ACTUAL 无 RESERVATION），`ErpFinConstants.java:164-169` 仅 4 常量（缺 OPENING_BALANCE/ADJUSTMENT/CLOSING）。三个 doc 值 + ORM 值 + 常量值互不一致。**部分确认 P1-MA1-018**（enum↔dict 漂移 4 项）的 ACTUAL↔NORMAL 半；新增未文档化的 OPENING_BALANCE/ADJUSTMENT/CLOSING 三个 dict 值。
- **P1-MA3-027**（major，NEW）：**ar-ap-status 命名与文档语义不符**。`ar-ap-reconciliation.md:139-153` + `use-cases.md:11` 声明 UNRECONCILED/PARTIAL/RECONCILED/OVER，ORM `orm.xml:222-228` 有 OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF。doc 的 RECONCILED 在 dict 不存在（实际 SETTLED），doc 的 OVER 状态完全不存在（code 以 ERR_RECONCILIATION_OVER_AMOUNT 错误码拒绝，不设 OVER 状态）。
- **P1-MA3-028**（major，NEW）：**bank-stmt-status 字典文档自相矛盾**。`bank-reconciliation.md:27,89` 声明 erp-fin/bank-stmt-status dict（DRAFT/RECONCILING/RECONCILED/CANCELLED），同文件 :145 撤回（实现复用 voucher-status DRAFT/POSTED/CANCELLED，不加 bank-stmt-status dict）。ORM 无 bank-stmt-status dict，code `BankReconciliationBuilder.java:96,129,140` 用 VOUCHER_STATUS_*。doc 内部自相矛盾。
- **P1-MA3-029**（major，NEW）：**合并抵消实体命名文档与代码不一致**。`intercompany-consolidation.md:40-46` 列 5 实体名（ConsolidationScheme/ConsolidationScope/IntercompanyReconciliation/ConsolidationAdjustment/ConsolidationReport），code 实际仅 2 实体（ErpFinConsolidationElimination/ErpFinIntercompanyMatch），5 个 doc 名全部不存在于 code。

#### 维度 4：跨域协作 drift

**裁决：有 NEW drift（1 blocker + 1 minor）**

- **P1-MA3-030**（blocker，NEW）：**`IErpFinVoucherBiz.reverse()` REQUIRES_NEW 事务边界文档与代码冲突**。`posting.md:399` 明示"跟随 @BizMutation 事务（REQUIRED），**不像 post() 叠加 REQUIRES_NEW**"。Code `ErpFinVoucherBizModel.java:77-79` 实际有 `@Transactional(propagation = TransactionPropagation.REQUIRES_NEW)` + O-7 注释（:49）承认 doc 未更新。**drift 方向：doc→code**——实施者按 doc 假设 reverse() 跟随调用方 @BizMutation 事务，红冲失败回滚调用方主事务；实际 reverse() 独立事务，红冲失败不回滚调用方。
- **P2-MA3-026**（minor，NEW）：**VoucherReversedEvent billType 字段派发源不清晰**。`posting.md:376-384` 事件 schema 列 billType 为独立字段（源单类型），code `ErpFinPostingProcessor.java:370-371` 设 `event.setBillType(businessType.name())`（billType = businessType 名称，非源单类型）。域监听者按 billType 路由会收到会计事件名而非源单名。

#### 维度 5：过账/会计 drift

**裁决：有 NEW drift（1 blocker + 2 minor）**

- **P1-MA3-031**（blocker，NEW）：**CommitmentAcctDocProvider budget.md vs posting.md 矛盾**。`budget.md:264-267` 说 Provider 支持 PURCHASE_ORDER_COMMITMENT 两种 businessType 并生成 VoucherFacts；`posting.md:541-543` 说 Provider 返回空集 + 承付凭证由 CommitmentVoucherGenerator 直接写入。Code `CommitmentAcctDocProvider.java:33-44` getSupportedBusinessTypes 返回 emptySet + createFacts 返回 emptyList，匹配 posting.md 不匹配 budget.md。两 doc 互斥——实施者读 budget.md（承付 owner doc）会实现返回 businessType 集合 + 生成 facts，与 code 反向。
- **P2-MA3-027**（minor，NEW）：**GL 映射试点清单状态自相矛盾**。`gl-mapping-rules.md:266-273` §5.3 说仅 PurAcctDocProvider × AP_INVOICE 试点，§8（:379-476）声明扩展至全部 28 Provider + 23 新 accountKey（ORM dict 已含 23 key）。§5.3 与 §8 状态矛盾。
- **P2-MA3-028**（minor，NEW）：**红字凭证同向取负 code 两种约定**。`posting.md:41` 声明"金额取负同向"，code `ErpFinPostingProcessor.java:730-755` 遵循取负同向，但 `CommitmentVoucherGenerator.java:222-244` 承付红冲用 Dr↔Cr swap 约定（反方向）。doc 仅描述取负同向。

#### 维度 6：配置/门控 drift（drift 最密集维度）

**裁决：有 NEW drift（4 blocker + 4 major）**

- **P1-MA3-032**（blocker，NEW）：**auto-post-on-close 默认值文档与代码相反**。`period-close.md:285` 声明默认 **true**（"结账时自动触发未过账单据过账"），code `ErpFinConstants.java:114` 注释"默认 false（阻断）"+ `ErpFinAccountingPeriodProcessor.java:681-684` isAutoPostOnClose 默认 FALSE。**相关 P1-MA2-017**（语义双重偏离），本 finding 聚焦 config 维度的默认值反转。
- **P1-MA3-033**（blocker，NEW）：**auto-depreciation 配置键名漂移**。`period-close.md:287` + `domain-design-guidelines.md:662` 声明键 `erp-fin.auto-depreciation`，code `ErpFinConstants.java:116` 实际键 `erp-fin.auto-depreciation-on-close`。运营设 doc 键名无效。
- **P1-MA3-034**（major，NEW）：**多账套配置项文档与代码大面积不一致**。`multiple-accounting-schemas.md:251-256` 声明 4 键（default-schema / multi-schema-enabled / schema-inheritance / auto-create-all-schemas），code `ErpFinConstants.java:193-196` 仅 2 键（multi-schema-enabled ✓ + default-schema-nature 键名不匹配）。schema-inheritance + auto-create-all-schemas 仅在 doc 出现 grep 零 code 引用。
- **P1-MA3-035**（major，NEW）：**合并抵消配置项文档与代码零重叠**。`intercompany-consolidation.md:147-150` 声明 4 键（consolidation-currency / consolidation-method / intercompany-tolerance / consolidation-schedule），code `ErpFinConstants.java:455-460` 有 3 键（intercompany-posting-enabled / consolidation-elimination-enabled / elimination-inventory-profit-enabled）。4 doc 键全部 grep 零 code 引用，3 code 键全部未在 owner doc 出现。
- **P1-MA3-036**（major，NEW）：**reverse-close-approval-required 审批框架 vs 代码硬阻断**。`state-machine.md:152-153,185-186` + `period-close.md:165,287` 框架为"审批门控"（暗示审批流存在），code `ErpFinAccountingPeriodProcessor.java:278-281,701-704` 默认 true 时直接 throw（无审批 action，纯 kill-switch）。"管理员审批后反结账"路径不可达。**相关 P1-MA2-020**（反结账 approval kill-switch 无审批流）。
- **P1-MA3-037**（major，NEW）：**报销/借款配置项默认值相反 + 幻影键**。`expense-claim.md:186` 声明 `expense-budget-check-enabled` 默认 **true**，code `ErpFinConstants.java:47-48` 默认 **false**（注释"预算模块未落地"）。同 doc :188 `imprest-topup-threshold` grep 零 code 引用（幻影键）。
- **P1-MA3-038**（major，NEW）：**AR/AP 自动核销规则配置项命名漂移**。`ar-ap-reconciliation.md:122-127` 声明 4 规则键（auto-match-exact-amount / auto-match-by-ratio / priority-by-aging / priority-by-due-date），code `ErpFinConstants.java:24-26` 实际 1 策略枚举 `auto-recon-strategy`（FIFO/BY_AMOUNT/BY_RATIO）。4 doc 键全部不存在。
- **P2-MA3-029**（minor，NEW）：**承付 release-on-receive 排除规则在代码层无 explicit guard**。`posting.md:519-521` 声明 reject release-on-receive-complete，code `CommitmentVoucherGenerator.java` 无防御性检查（依赖 purchase 域调用方自律）。

#### 维度 7：未文档化行为（code→doc 反向 drift）

**裁决：有 NEW drift（1 major + 2 minor）**

- **P1-MA3-039**（major，NEW）：**`persistVoucher` 写死 `amountSource = amountFunctional`，多币种凭证丢失源币金额**。`posting.md:481-484` 声明凭证分录行同时记录 amountSource/amountFunctional/币种/汇率，code `ErpFinPostingProcessor.java:816-819` 设 `line.setAmountSource(amt); line.setAmountFunctional(amt);`（两者相等，无币种折算）。多币种凭证 amountSource 被本位币金额覆盖。**相关 P1-MA2-002/009**（多币种 P2P/O2C 本位币凭证路径未验证），本 finding 从 voucher line 层确认 drift。
- **P2-MA3-030**（minor，NEW）：**`ErpFinConfigs.java` 是空壳接口**。`ErpFinConfigs.java:1-5` 接口体为空，所有 ~120 config key 合并入 `ErpFinConstants.java:14-509`。configs/constants 分离意图未实现。
- **P2-MA3-031**（minor，NEW）：**期间结账 CLOSED→CLOSED_FINAL 是独立步骤而非自动**。`period-close.md:163-165` 暗示自动衔接，code `ErpFinAccountingPeriodProcessor.java:157-158,204-210` 需独立 `finalizePeriod` @BizMutation。

### 1.2 finance MA2 owner-doc drift 复核表

| MA2 Finding ID | 本审计复核结论 | 升级/降级 | 新发现代码侧 drift |
|---|---|---|---|
| P1-MA2-031（DRAFT→CANCELLED 不可达 + 红字凭证终态归属未定义） | **确认**。state-machine.md 声明迁移 + 终态，code 无实现。 | 维持 P1 | 无新代码侧 drift |
| P1-MA2-032（IGNORED 凭证悬挂缺告警闭环） | **确认**。pre-close scan 不覆盖 IGNORED。 | 维持 P1 | 无新代码侧 drift |
| P1-MA2-033（NEVER_OPENED→OPEN 迁移路径缺失） | **确认并扩展**→P1-MA3-024（CLOSED 语义三源冲突）。 | 维持 P1（P1-MA3-024 扩展为 blocker） | 无新代码侧 drift |
| P1-MA2-034（carryForward 不校验源年度全 CLOSED 前置） | **确认**。 | 维持 P1 | 无新代码侧 drift |
| P1-MA2-017~022（期间结账 batch） | **确认**。本审计新增 config 维度 drift（P1-MA3-032/033）+ state 维度 drift（P1-MA3-024）。 | 维持 P1 | 无新代码侧 drift |
| P1-MA2-081~084（承付 batch） | **确认**。本审计新增 Provider 矛盾（P1-MA3-031）+ 公式 drift（P1-MA3-025）+ 红冲约定（P2-MA3-028）。 | 维持 P1 | P1-MA3-025 与 P1-MA2-084 存在矛盾读法，需 MR1 交叉核实 |
| P1-MA2-093~099（多公司 batch） | **确认**。本审计新增合并抵消实体命名（P1-MA3-029）+ 配置项零重叠（P1-MA3-035）。 | 维持 P1 | 无新代码侧 drift |
| P1-MA1-018（enum↔dict 漂移 4 项） | **确认并扩展**→P1-MA3-026（postingType 三源）+ P1-MA3-027（ar-ap-status）+ P1-MA3-028（bank-stmt-status）。 | 维持 P1 | 无新代码侧 drift |

### 1.3 finance drift finding 清单

（见 §0 裁决 + §1.1 维度裁决，共 16 NEW P1 [P1-MA3-024~039] + 9 NEW P2 [P2-MA3-023~031]）

---

## Phase 2 — manufacturing owner doc vs 代码 drift（A3.4，S 级）

审查目标：`docs/design/manufacturing/`（12 文件）；`module-manufacturing/erp-mfg-service/`（77 Java 文件）；`module-manufacturing/model/app-erp-manufacturing.orm.xml`。

### 2.1 维度裁决（7 维度逐项）

#### 维度 1：状态迁移/工作流 drift

**裁决：有 NEW drift（2 major）+ MA2 复核确认**

- **P1-MA3-040**（major，NEW）：**state-machine.md §质检约束声明引用不存在的 INSPECTING 工单状态**。`state-machine.md:155-165` §质检约束声明表引用"工单可从 INSPECTING → COMPLETED"，ORM `orm.xml:35-46` work-order-status 10 态无 INSPECTING，code `ErpMfgWorkOrderProcessor.java:188-201` 质检门控 config-gated throw 保持 IN_PROCESS。§实现偏离补注（:171）承认偏离但 §质检约束声明表仍以 INSPECTING 为真实状态——doc 内部矛盾。
- **P1-MA3-041**（major，NEW）：**state-machine.md 声明可配置超产但 code 无此 config**。`state-machine.md:71` "报工超过工单数量→拒绝（除非配置允许超产）"，code `ErpMfgWorkOrderProcessor.java:181-185` 硬编码拒绝 + `ErpMfgErrors.java:98-99` 错误信息引用"未启用超产配置"但 `ErpMfgConstants` 无此 config key。doc 承诺的逃生通道不存在。
- **MA2 复核确认**（无新 ID）：
  - P1-MA2-035（作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态）：**确认**。
  - P1-MA2-036（MRP CANCELLED + 预测 CONSUMED dict 死状态）：**确认**。
  - P1-MA2-037（mrp.md §建议单释放 "RELEASED" vs isFirmed 布尔）：**确认**。
  - P1-MA2-038（MrpReleaseService 委外单 APPROVED O-4 豁免登记缺失）：**确认**。subcontracting.md 未文档化此 bypass。

#### 维度 2：业务规则/计算 drift

**裁决：有 NEW drift（1 blocker + 1 major）**

- **P1-MA3-042**（blocker，NEW）：**material-reservation.md 整个预留子系统未实现**。`material-reservation.md`（288 行）声明完整预留子系统：WorkOrder 上 `reservationStatus` 6 态维度 + `ErpMfgMaterialReservation` 实体（reservedQty/pickedQty/releasedQty）+ 审核触发预留 flow + 领料扣减预留 + 预留释放 + 5 config key。**Code 完全未实现**——`KitAvailabilityChecker.java:30-33` 显式只读（"不写预留"），`ErpMfgWorkOrder` 无 reservationStatus 字段，5 config key 全部不在 `ErpMfgConstants`。doc :9 声明"业务语义说明，实际落位以库存域为准"不足以覆盖 doc 后续详细描述的 mfg 侧 flow/config/状态维度。
- **P1-MA3-043**（major，NEW）：**use-cases.md UC-MFG-12 差异公式列表错误**。`use-cases.md:223-227` UC-MFG-12 列 4 公式（材料用量/材料价格/人工效率/人工费率），code `ProductionVarianceCalculator.java:128-203` 实现 6 类（含制造费用 OVERHEAD + 产量 VOLUME + 委外 SUBCONTRACT，不含材料价格[PPV 归采购]）。UC 漏 3 实现类型 + 含 1 非生产类型。兄弟文档 `variance-analysis.md:62-68` 正确列全——两 design doc 互斥。

#### 维度 3：字段/实体语义 drift

**裁决：有 NEW drift（1 major + 1 minor）**

- **P1-MA3-044**（major，NEW）：**README 列 DowntimeEntry + ProductionPlan 实体但 ORM 不存在**。`README.md:36-37` 列 ProductionPlan（生产计划）+ DowntimeEntry（停机记录）为核心业务对象，ORM 34 实体无 ErpMfgDowntimeEntry 无 ErpMfgProductionPlan（最接近 ErpMfgMrpPlan）。`crp.md:129` 承认 maintenance downtime 为 Non-Goal 但 README 无免责声明。
- **P2-MA3-032**（minor，NEW）：**variance-analysis.md §差异分类表列 7 类但 dict/code 有 6 类**。`variance-analysis.md:14-23` 含材料价格差异行，ORM `erp-mfg/variance-type` dict + code 6 值无 MATERIAL_PRICE。:36 补注承认但顶层表仍列。

#### 维度 4：跨域协作 drift

**裁决：本维度无 NEW drift。** 完工入库 / 领料出库 / 质检门控 / 委外采购 / CRP→APS SPI 全部 Facade 调用与设计声明一致。

#### 维度 5：过账/会计 drift

**裁决：本维度无 NEW drift。** MANUFACTURING_RECEIPT/ISSUE / SUBCONTRACT 三段 SI/SR/SF / PRODUCTION_VARIANCE / 重算红冲 reverseIfExists 全部 Provider/Dispatcher 实现与设计声明一致。

#### 维度 6：配置/门控 drift

**裁决：本维度无独立 NEW drift（material-reservation 5 config key 缺失已并入 P1-MA3-042）。** 其他声明 config（variance-auto-calc-enabled / inspection-gate-enabled / subcontract-release-enabled / subcontract-posting-enabled / simulation-enabled / overhead-allocation-enabled / forecast-consume-enabled）全部在 code 存在且默认值匹配。

#### 维度 7：未文档化行为（code→doc 反向 drift）

**裁决：有 NEW drift（1 major + 1 minor）**

- **P1-MA3-045**（major，NEW）：**差异阈值预警已实现但 doc 标 Deferred**。`variance-analysis.md:89` 标"异常预警 Deferred，依赖通知派发通道"，code `ProductionVarianceCalculator.java:225-261` 已完整实现 `dispatchVarianceAlertIfOverThreshold`（调 `IErpSysNotificationBiz.notify` + event `mfg.production-variance`），`ErpMfgConstants.java:214-223` 有 2 config key（variance-alert-enabled 默认 true + variance-alert-threshold 默认 100）。doc §配置点表（:113-115）未列此 2 config。
- **P2-MA3-033**（minor，NEW）：**Simulation RUNNING 状态 + pegging reset 语义文档不足**。`simulation-engine.md` 无 4 态定义表（RUNNING 仅隐含），`SimulationMrpEngine.java:217` promote 时 parentLineId 重置 null（pegging 刻意丢弃）未在 doc 说明。

### 2.2 mfg MA2 owner-doc drift 复核表

| MA2 Finding ID | 本审计复核结论 | 升级/降级 | 新发现代码侧 drift |
|---|---|---|---|
| P1-MA2-035（作业卡 TRANSFERRED dict 死状态） | **确认**。 | 维持 P1 | 无 |
| P1-MA2-036（MRP CANCELLED + 预测 CONSUMED dict 死状态） | **确认**。 | 维持 P1 | 无 |
| P1-MA2-037（mrp.md "RELEASED" vs isFirmed 布尔） | **确认**。 | 维持 P1 | 无 |
| P1-MA2-038（MrpReleaseService 委外单 O-4 豁免登记缺失） | **确认**。subcontracting.md 未文档化。 | 维持 P1 | 无 |

### 2.3 mfg drift finding 清单

（见 §0 裁决 + §2.1 维度裁决，共 6 NEW P1 [P1-MA3-040~045] + 2 NEW P2 [P2-MA3-032~033]）

---

## Phase 3 — pur+sal+inv owner doc vs 代码 drift（A3.5，A 级合并）

审查目标：`docs/design/purchase/`（8 文件）+ `docs/design/sales/`（7 文件）+ `docs/design/inventory/`（8 文件）；各域 `erp-<short>-service/` + `erp-<short>-dao/` + `model/app-erp-<domain>.orm.xml`。

### 3.1 维度裁决（7 维度 × 3 域逐项）

#### 维度 1：状态迁移/工作流 drift

**裁决：pur/sal 核心状态机一致；inv 有 1 NEW minor drift + MA2 复核确认**

- **purchase**：核心 6 实体状态机（Order/Receive/Invoice/Payment/Return/Requisition）**一致**——全 Processor doReverseApprove 设 REJECTED + 守卫齐全。Quotation/Rfq drift 属 MA2-049/050 范畴（且 state-machine.md §适用对象显式排除 Quotation/Rfq，属 doc 覆盖范围界定问题非直接矛盾）。
- **sales**：核心 5 实体状态机（Order/Delivery/Invoice/Receipt/Return）**一致**。sales Quotation **正确**（有完整 Processor 设 REJECTED，与 purchase Quotation 不同）。Contract drift 属 MA2-056/057 范畴（且 sales/state-machine.md §适用对象 + contract.md 均沉默 Contract approveStatus 轴，属 doc 覆盖范围界定问题）。
- **inventory**：
  - **P2-MA3-034**（minor，NEW）：**StockTake "盘点中"状态名 COUNTING vs code CONFIRMED**。`state-machine.md:152-156` 声明 DRAFT→COUNTING→DONE，code `ErpInvStockTakeBizModel.java:33,47` + ORM dict 用 CONFIRMED。**与 P2-MA1-025 相关**（owner doc 用 COUNTING 命名"盘点中"，code 复用 move-status 的 CONFIRMED）——P2-MA1-025 已登记为 watch-only，本审计确认 owner doc 仍未同步。
  - **MA2 复核确认**：P1-MA2-062（StockTake completeTake 未自动生成盘盈/盘亏移动单）+ P1-MA2-063（PickingOrder PICKING/PICKED dict 死状态 + CRUD 桩）均**确认**。

#### 维度 2：业务规则/计算 drift

**裁决：pur/sal 一致；inv 有 1 NEW minor drift**

- **purchase**：三单匹配（invoice approve 时 `ThreeWayMatcher.match`）**一致**。付款核销缺三单匹配复核属 MA2-003 范畴（已确认）。
- **sales**：信用控制三级策略 + 定价引擎**一致**。
- **inventory**：
  - **P2-MA3-035**（minor，NEW）：**冲销反向移动数量取负 vs code 翻转 moveType**。`state-machine.md:41` + `trace-chain.md:42` 声明"数量取负"，code `ErpInvStockMoveProcessor.java:357-375` 保持正数 + 翻转 moveType（incoming↔outgoing）。净效果等价但字面声明不一致。方法名 `negateOrSame` 暴露 intent-vs-implementation gap。
  - 7 costMethod + 到岸成本分摊（BY_AMOUNT/BY_QUANTITY/BY_WEIGHT）**一致**。

#### 维度 3：字段/实体语义 drift

**裁决：pur/sal/inv 三域字段语义一致，无 NEW drift。** sales returnStatus/refundStatus 派生视图偏离已在 `returns.md:88-93` 显式登记（plan 0456-2）。

#### 维度 4：跨域协作 drift

**裁决：pur/sal/inv 三域 Facade 调用一致，无 NEW drift。** 承付 hook / intercompany hook 在 purchase/sales owner doc 未提及（doc 覆盖范围界定问题，行为在 finance/budget.md + multi-company.md 有文档），非直接矛盾。

#### 维度 5：过账/会计 drift

**裁决：pur/sal/inv 三域过账 Provider 一致，无 NEW drift。** AP_INVOICE/PAYMENT/PURCHASE_RETURN/PURCHASE_INPUT + AR_INVOICE/RECEIPT/SALES_RETURN/SALES_OUTPUT + LANDED_COST 全部 Provider 实现与设计声明一致。PurReversalListener.rollbackReceive 不对称属 MA2-051 范畴（已确认，intentional per posting.md）。

#### 维度 6：配置/门控 drift

**裁决：pur/sal/inv 三域配置项一致，无 NEW drift。** 所有声明 config key 在 code 存在且默认值匹配（match-qty-tolerance / match-price-tolerance / match-strict-mode / credit-check-* / allow-negative-stock / trace-chain-* / budget-commitment-enabled）。

#### 维度 7：未文档化行为（code→doc 反向 drift）

**裁决：pur/sal/inv 三域无 NEW blocker/major code→doc drift。** 跨域行为（质检门控 / intercompany / commitment hook）在所属域 owner doc 有文档，仅在 pur/sal/inv owner doc 未重复——属 doc 覆盖范围界定问题非矛盾。

### 3.2 pur+sal+inv MA2 owner-doc drift 复核表

| MA2 Finding ID | 本审计复核结论 | 升级/降级 | 新发现代码侧 drift |
|---|---|---|---|
| P1-MA2-049（Quotation/Rfq reverseApprove→SUBMITTED） | **确认**。sales Quotation 侧**正确**（有完整 Processor 设 REJECTED），仅 purchase Quotation/Rfq 默认 SUBMITTED。 | 维持 P1 | 无 |
| P1-MA2-050（INLINE reject/withdrawApproval 缺守卫） | **确认**。 | 维持 P1 | 无 |
| P1-MA2-051（PurReversalListener.rollbackReceive 不对称） | **确认**。intentional per posting.md javadoc，非 purchase owner doc 违反。 | 维持 P1 | 无 |
| P1-MA2-056（Contract reverseApprove→SUBMITTED） | **确认**。sales state-machine.md + contract.md 沉默 Contract approveStatus 轴。 | 维持 P1 | 无 |
| P1-MA2-057（INLINE withdrawApproval + Contract 全 INLINE 缺守卫） | **确认**。 | 维持 P1 | 无 |
| P1-MA2-062（StockTake completeTake 未自动生成移动单） | **确认**。 | 维持 P1 | 无 |
| P1-MA2-063（PickingOrder PICKING/PICKED dict 死状态 + CRUD 桩） | **确认**。 | 维持 P1 | 无 |
| P1-MA2-001/002/003/009/023/024/081~084 | **上下文确认**——均为代码完整性缺口，触及 owner doc 处经 doc 覆盖范围界定（行为在 finance/budget owner doc 有文档），非直接矛盾。 | 维持原分类 | 无 |

### 3.3 pur+sal+inv drift finding 清单

（见 §0 裁决 + §3.1 维度裁决，共 0 NEW P1 + 2 NEW P2 [P2-MA3-034~035]）

---

## Phase 4 — finding 汇总交接 + 分类裁决

### 4.1 NEW P1 finding 汇总（22 项，目标 MR2 文档类 / MR1 代码侧）

| Finding ID | 域 | 维度 | drift 方向 | 严重性 | 一句话描述 | 目标 MR |
|---|---|---|---|---|---|---|
| P1-MA3-024 | finance | 1 状态 | doc↔doc↔code | blocker | 期间状态机 CLOSED 语义三源冲突 + NEVER_OPENED 缺失 | MR2 |
| P1-MA3-025 | finance | 2 计算 | doc→code | major | 预算余量公式 doc 三项式 vs code javadoc 二项式（与 MA2-084 矛盾读法需 MR1 核实） | MR2+MR1 |
| P1-MA3-026 | finance | 3 字段 | doc↔doc↔code | blocker | postingType 字典三处真相源不一致（ACTUAL/NORMAL + 未文档化 OPENING_BALANCE/ADJUSTMENT/CLOSING） | MR2 |
| P1-MA3-027 | finance | 3 字段 | doc→code | major | ar-ap-status 命名漂移（UNRECONCILED/RECONCILED/OVER vs OPEN/SETTLED） | MR2 |
| P1-MA3-028 | finance | 3 字段 | doc↔doc | major | bank-stmt-status 字典文档自相矛盾（:27/89 声明 vs :145 撤回） | MR2 |
| P1-MA3-029 | finance | 3 字段 | doc→code | major | 合并抵消实体命名 doc 5 名 vs code 2 实体 | MR2 |
| P1-MA3-030 | finance | 4 跨域 | doc→code | blocker | IErpFinVoucherBiz.reverse() REQUIRES_NEW doc 说否 code 是 | MR2 |
| P1-MA3-031 | finance | 5 过账 | doc↔doc | blocker | CommitmentAcctDocProvider budget.md vs posting.md 矛盾 | MR2 |
| P1-MA3-032 | finance | 6 配置 | doc→code | blocker | auto-post-on-close 默认值 doc true vs code false | MR2 |
| P1-MA3-033 | finance | 6 配置 | doc→code | blocker | auto-depreciation 配置键名漂移（auto-depreciation vs auto-depreciation-on-close） | MR2 |
| P1-MA3-034 | finance | 6 配置 | doc→code | major | 多账套配置项大面积不一致（4 doc 键 vs 2 code 键） | MR2 |
| P1-MA3-035 | finance | 6 配置 | doc→code | major | 合并抵消配置项零重叠（4 doc 键 vs 3 code 键） | MR2 |
| P1-MA3-036 | finance | 6 配置 | doc→code | major | reverse-close-approval-required 审批框架 vs kill-switch | MR2 |
| P1-MA3-037 | finance | 6 配置 | doc→code | major | 报销/借款配置项默认值相反 + 幻影键 | MR2 |
| P1-MA3-038 | finance | 6 配置 | doc→code | major | AR/AP 自动核销规则配置项命名漂移 | MR2 |
| P1-MA3-039 | finance | 7 code→doc | code→doc | major | persistVoucher 写死 amountSource=amountFunctional 多币种丢失 | MR2+MR1 |
| P1-MA3-040 | mfg | 1 状态 | doc→code | major | state-machine.md §质检约束声明引用不存在的 INSPECTING 状态 | MR2 |
| P1-MA3-041 | mfg | 1 状态 | doc→code | major | state-machine.md 声明可配置超产但 code 无此 config | MR2 |
| P1-MA3-042 | mfg | 2 计算 | doc→code | blocker | material-reservation.md 整个预留子系统未实现 | MR2 |
| P1-MA3-043 | mfg | 2 计算 | doc→code | major | use-cases.md UC-MFG-12 差异公式列表错误（4 公式 vs code 6 类） | MR2 |
| P1-MA3-044 | mfg | 3 字段 | doc→code | major | README 列 DowntimeEntry + ProductionPlan 实体但 ORM 不存在 | MR2 |
| P1-MA3-045 | mfg | 7 code→doc | code→doc | major | 差异阈值预警已实现但 doc 标 Deferred | MR2 |

> 红字凭证终态归属（P1-MA2-031 第二半）经本审计复核确认分类维持 P1-MA2-031，不另占新 P1-MA3 ID（见 §1.2 复核表）。

### 4.2 NEW P2 finding 汇总（13 项，watch-only）

| Finding ID | 域 | 维度 | drift 方向 | 一句话描述 |
|---|---|---|---|---|
| P2-MA3-023 | finance | 1 状态 | doc→code | reverseClose 路径 3-step vs 1-step |
| P2-MA3-024 | finance | 2 计算 | code→doc | 银行对账余额调节恒等式简化未在文档标注 |
| P2-MA3-025 | finance | 2 计算 | doc→code | 坏账计提"争议发票"排除 doc 声明规则 vs code config-gated deferred |
| P2-MA3-026 | finance | 4 跨域 | code→doc | VoucherReversedEvent billType = businessType.name() 非源单类型 |
| P2-MA3-027 | finance | 5 过账 | doc↔doc | GL 映射试点清单 §5.3 vs §8 状态矛盾 |
| P2-MA3-028 | finance | 5 过账 | code→doc | 红字凭证两种约定（取负同向 vs Dr↔Cr swap）doc 仅述一种 |
| P2-MA3-029 | finance | 6 配置 | doc→code | 承付 release-on-receive 排除规则 code 无 explicit guard |
| P2-MA3-030 | finance | 7 code→doc | code→doc | ErpFinConfigs.java 空壳接口 configs/constants 未分离 |
| P2-MA3-031 | finance | 7 code→doc | code→doc | CLOSED→CLOSED_FINAL 需独立 finalizePeriod 步骤 |
| P2-MA3-032 | mfg | 3 字段 | doc→code | variance-analysis.md §差异分类表 7 类 vs dict/code 6 类 |
| P2-MA3-033 | mfg | 7 code→doc | code→doc | Simulation RUNNING 状态 + pegging reset 语义文档不足 |
| P2-MA3-034 | inv | 1 状态 | doc→code | StockTake COUNTING vs code CONFIRMED（确认 P2-MA1-025） |
| P2-MA3-035 | inv | 2 计算 | doc→code | 冲销反向移动"数量取负" vs code 翻转 moveType |

### 4.3 分类裁决

- **文档类 drift（owner doc 错误/过时/矛盾）**：P1-MA3-024/026/027/028/029/030/031/032/033/034/035/036/037/038/040/041/042/043/044/045/046 → **目标 MR2**（依赖 MA3+MA4 done 后由 R2.0 展开机制转化为具体修复工作项行）。
- **代码侧 drift（设计正确但代码错误或需核实）**：
  - P1-MA3-025（预算公式与 MA2-084 矛盾读法）→ **目标 MR1**（需 MR1 裁决核实 code 内部是否隐含 COMMITMENT）。
  - P1-MA3-039（persistVoucher amountSource=amountFunctional）→ **目标 MR1**（与 P1-MA2-002/009 多币种凭证路径一并裁决）。
- **MA2 复核结论**：范围内域簇全部 MA2 owner-doc drift finding（P1-MA2-031~034/035~038/049~051/056~057/062~063）分类与归属**全部确认一致，无升级/降级/重新分类**。

### 4.4 与 A3.1/A3.2 已登记 P1 交叉去重

| 本审计 NEW finding | A3.1/A3.2 已登记 | 去重结论 |
|---|---|---|
| P1-MA3-024~046 | P1-MA3-001~023 | **无重复**——A3.1 审设计文档内部质量（dim 2/3/5/6/9/12），A3.2 审前瞻性缺失（dim 4/5），本审计审 design vs code drift（7 维度）。三批 finding 正交不重叠。 |
| P1-MA3-026（postingType 三源） | P1-MA1-018（enum↔dict 漂移 4 项） | **部分重叠**——P1-MA1-018 覆盖 ACTUAL↔NORMAL 半；本 finding 扩展为三源 + 未文档化 OPENING_BALANCE/ADJUSTMENT/CLOSING。注册为扩展非重复。 |
| P1-MA3-032（auto-post-on-close 默认值） | P1-MA2-017（语义双重偏离） | **相关非重复**——P1-MA2-017 聚焦 doc/code 语义偏离 + AR-AP 阻断分级；本 finding 聚焦 config 维度默认值反转（true vs false）。 |
| P1-MA3-036（reverse-close kill-switch） | P1-MA2-020（反结账 approval kill-switch 无审批流） | **相关非重复**——P1-MA2-020 聚焦审批流缺失；本 finding 聚焦 doc 框架（"审批门控"）vs code 行为（kill-switch）的 drift 描述。 |

### 4.5 裁决通过/失败

**Verdict: FAIL**（drift 存在但零 BLOCKER 级活跃数据破坏）——22 项 P1 + 13 项 P2 全部为文档类/配置类 drift，目标 MR2/MR1 批量修复。本审计为文档-实现一致性层，原则上无 P0。

### 4.6 剩余风险

1. **finance 域 drift 密度最高**（17 P1 + 9 P2 = 26 项），配置/门控维度最严重（8 项）——MR2 应优先修复 finance owner doc 的 config key 表（P1-MA3-032~038），使运营可按 doc 正确配置系统。
2. **P1-MA3-025 预算公式 drift 与 P1-MA2-084 矛盾读法**需 MR1 优先裁决——若 code 确实不含 COMMITMENT 在 actualBalance 内，则承付启用时预算控制形同虚设（HARD 控制实际 SOFT）。
3. **P1-MA3-030 reverse() REQUIRES_NEW drift** 是最危险的 contract drift——跨域调用方（11 域 PostingExecutor/Dispatcher）按 doc 假设 reverse() 跟随调用方事务，实际独立事务。MR2 应优先更新 posting.md:399。
4. **P1-MA3-042 material-reservation.md 整个子系统未实现**是 mfg 域最大单一 drift——288 行 owner doc 描述的行为几乎完全未实现。MR2 应大幅改写或标注 Deferred。
5. **pur+sal+inv 三域 drift 最少**（仅 2 项 P2 minor），核心机制文档与代码高度一致——MA2 已登记的 drift 覆盖了大部分问题。

---

## 附录 A：审计方法与覆盖度

- **审计 skill**：`multi-dimensional-audit-prompt.md` 7 维度（状态迁移/业务规则/字段语义/跨域协作/过账会计/配置门控/未文档化行为）+ 反窄化自检。
- **覆盖度**：finance 19/19 doc 文件全覆盖 + 122 Java 文件抽样核实；mfg 12/12 doc 全覆盖 + 77 Java 文件抽样；pur 8/8 + sal 7/7 + inv 8/8 doc 全覆盖 + 222 Java 文件抽样。
- **MA2 复核覆盖**：范围内域簇全部 MA2 owner-doc drift finding（P1-MA2-031~034/035~038/049~051/056~057/062~063）逐项复核。
- **与 A3.1/A3.2 交叉去重**：P1-MA3-024~046 与 P1-MA3-001~023 经交叉去重无重复登记。
- **审计性质**：文档-代码比对审查，不改应用代码，产出为本报告 + arm-index P1/P2 登记 + scope matrix §2.3 终态标记。
