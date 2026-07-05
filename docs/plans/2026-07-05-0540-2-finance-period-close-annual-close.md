# 2026-07-05-0540-2-finance-period-close-annual-close 年度结转（本年利润→未分配利润 + 次年期间创建）+ 银行存款外币汇兑重估

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: deferred 项承接（`2026-07-02-1000-3-finance-period-close.md` Deferred「年度结转（本年利润→未分配利润 + 辅助账跨年 + 次年期间创建）」触发条件「跨年运营需求时」+ Deferred「银行存款外币汇兑重估」触发条件「多币种银行对账/资金面落地时」——`2026-07-05-0115-2-finance-bank-reconciliation.md` 银行对账子面已 done，多币种银行资金面已就绪，触发条件满足）；`docs/design/finance/period-close.md` §年度结转规则（设计已就绪）
> Related: `2026-07-02-1000-3-finance-period-close.md`（月度结账基线，本计划为其年度后继）；`2026-07-05-0115-2-finance-bank-reconciliation.md`（银行对账，多币种资金面就绪使银行存款重估触发条件满足）；`2026-07-05-0540-1-finance-bad-debt-provision.md`（同批次，正交：坏账 allowance 门控与年度结转不同方法）
> Mission: erp
> Work Item: finance 期末结账年度结转 + 银行存款外币重估（承接 1000-3 双 Deferred）
> Audit: required

## Current Baseline

（实时核实于 2026-07-05）

- **月度结账已全链落地（基线）**：`ErpFinAccountingPeriodBizModel` + `ErpFinAccountingPeriodProcessor`（`module-finance/erp-fin-service/.../processor/`）已实现期间状态机（OPEN→CLOSING→CLOSED→CLOSED_FINAL + 反结账）、前置检查、AR→AP→INV→AST→GL 模块关账编排、损益结转（收入/费用/成本三类 PERIOD_CLOSING 凭证）、汇兑重估（EXCHANGE_GAIN_LOSS 承接 0300-3）、折旧集成门控、试算平衡表快照（1000-3 Closure，93 测试全绿）。
- **年度结转设计已就绪但未实现**：`period-close.md` §年度结转规则（`:231-271`）定义 6 步——(1) 常规期末结账[已有] (2) 损益结转[月度已做收入/费用/成本三类] (3) 本年利润→未分配利润 (4) 辅助账跨年结转（存货余额/AR-AP 未清项/资产净值） (5) 新开次年 12 期间 (6) 年度报表[nop-report，Non-Goal]。其中 **(3)(5) 零实现**，`ErpFinAccountingPeriodBizModel.closePeriod` 无年度分支；`ErpFinAccountingPeriod` 实体有 `year`（propId 5）/`startDate`/`endDate` 字段（`app-erp-finance.orm.xml:419,421,422`），可判定"是否 12 月/年末"。
- **年初余额字段已预留但未填充**：`app-erp-finance.orm.xml:605-606` 试算/科目余额实体已有 `yearOpeningDebit`/`yearOpeningCredit`（年初借/贷）字段，年度结转时需 populate（当前结账未写入年初余额）。
- **汇兑重估当前仅覆盖 AR/AP 往来**：`ExchangeRevaluationService`（1000-3 Phase 3，`module-finance/erp-fin-service/.../ExchangeRevaluationService.java:36` 自述 Non-Goal「不重估货币性科目余额」）按 `ErpFinArApItem.currencyId != functionalCurrency` 重估未核销往来项；**银行存款（`ErpFinFundAccount`，有 `currencyId` propId 9，`app-erp-finance.orm.xml:691`，实体定义 `:679`）外币余额未重估**——1000-3 显式 Non-Goal「银行存款外币重估需科目级币种标记」。
- **次年期间创建无自动化**：次年会计期间需手工创建；`ErpFinAccountingPeriodBizModel` 无"批量生成年期间"入口。

剩余差距：年度结转 (3) 本年利润→未分配利润 + (5) 次年期间自动创建 + 年初余额 populate 缺失；银行存款外币余额期末未重估。

## Goals

- `ErpFinAccountingPeriodBizModel.closePeriod` 增年度结转分支：当结账期间为 12 月/年末时，在常规月度结账后追加 (a) 本年利润余额→未分配利润结转凭证（新增业务类型 `PROFIT_TO_RETAINED_EARNINGS` 或复用 PERIOD_CLOSING 内部分支，Decision 裁定）；(b) populate 科目年初余额（`yearOpeningDebit/Credit`）为次年比较基线；(c) 触发次年期间创建。
- 次年期间自动创建入口 `generateNextYearPeriods(year)` @BizMutation：按给定年生成 1-12 月会计期间（状态 1 月视配置 OPEN、其余 NOT_STARTED/未来），幂等（已存在跳过/报错，Decision 裁定）。
- `ExchangeRevaluationService` 扩展银行存款外币重估：期末按 `ErpFinFundAccount.currencyId != functional` × 期间末汇率重估银行存款余额，生成 EXCHANGE_GAIN_LOSS 凭证（与既有 AR/AP 重估同业务类型、同事务）。
- 反结账红冲覆盖年度结转凭证（与月度反结账一致范式）。

## Non-Goals

- **年度报表渲染（资产负债表/利润表/现金流量表）**：属 nop-report 报表面（`period-close.md` 步骤6），归后继。
- **辅助账跨年数据搬移**：存货余额/AR-AP 未清项/资产净值天然跨年延续（不随期间关闭归零），年度结转步骤4 主要是"跨账对账"校验而非数据移动——本期仅做对账校验门控（余额一致才允许年度结账），不做物理搬移。
- **多账套/合并报表年度结转**：归 `intercompany-consolidation.md`（独立结果面）。
- **本年利润科目→未分配利润的多级利润分配（提取盈余公积/分红等）**：仅做"本年利润→未分配利润"基础结转；利润分配明细（法定盈余公积/任意盈余公积/应付股利）归后继。
- **历史年度追溯结转/年度重算**：仅向前（当前年末→次年初）；历史年度已结账重算归后继。

## Task Route

- Type: `implementation-only change`（owner doc 已设计的年度结转落地 + 已有重估服务扩展）
- Owner Docs: `docs/design/finance/period-close.md`（§年度结转规则落地标记 + §汇兑重估银行存款补注）；`docs/design/finance/treasury.md`（银行存款外币重估，若该 owner doc 有相关面则补注）
- Skill Selection Basis: BizModel 方法（年度分支 + 批量生成期间）+ 业财过账业务类型 + 扩展既有重估服务读银行账户 + 反结账红冲 + ORM 已有字段 populate——加载 `nop-backend-dev`（实体服务、跨实体调用、过账 Provider 范式、ErrorCode）。

### Key Decisions

- **Decision: 本年利润→未分配利润的业务类型**
  - 选择：新增业务类型 `PROFIT_TO_RETAINED_EARNINGS`（`ErpFinBusinessType` 枚举 + 字典同步，参照 0831-2 保护区域同步范式），专用于"本年利润科目余额→未分配利润科目"年度结转凭证（区别于月度 PERIOD_CLOSING 损益结转，便于年度审计追溯）。
  - 替代方案：复用 PERIOD_CLOSING 内部分支——拒绝，月度损益结转（收入/费用→本年利润）与年度利润分配（本年利润→未分配利润）是两层不同结转，混用类型致规则命中日志与审计难追溯。
  - 残留风险：新增类型属保护区域扩展——经字典同步 + owner doc 补注缓解。
- **Decision: 年度结转触发判定**
  - 选择：`closePeriod` 在常规月度结账步骤完成后，判定该期间是否为"年末"（`year` 匹配且 `endDate` 为该年 12-31 或期间序号=12）；是则执行年度结转追加步骤。config-gated `erp-fin.annual-close-enabled`（默认 true，关闭则仅月度结账不年度结转）。
  - 替代方案：独立 `closeYear(year)` 入口与月度解耦——拒绝，年度结转必须在 12 月结账后同事务执行（否则 12 月已 CLOSED_FINAL 后再年度结转需反结账），耦合更安全。
  - 残留风险：判定"年末"依赖期间 `year` + 序号准确——以前置检查（期间 startDate/endDate 跨度=1 个月 + year 字段非空）缓解。
- **Decision: 次年期间创建幂等策略**
  - 选择：`generateNextYearPeriods(year)` 按 year 生成 1-12 月期间；已存在同年期间 → 抛 `ERR_PERIODS_ALREADY_EXIST`（默认），config-gated `erp-fin.period-generate-skip-existing`（默认 false）true 时跳过已存在仅补建缺失月份。
  - 替代方案：每次重建——拒绝，已有关账期间不能随意删除（状态机保护）。
- **Decision: 银行存款重估范围与汇率**
  - 选择：重估 `ErpFinFundAccount.currencyId != functionalCurrency` 的银行存款账户余额（账户余额经 `ErpFinVoucher` 科目聚合或 FundAccount.balance 字段，Phase 1 核实账户余额真相源）；期间末汇率（与 AR/AP 重估同汇率源）；差额生成 EXCHANGE_GAIN_LOSS 凭证（借/贷银行存款 / 贷/借汇兑损益）。
  - 替代方案：仅重估外币凭证未结算项——拒绝，银行存款余额是时点余额（非未清项），需按账户余额整体重估。

## Infrastructure And Config Prereqs

- 新增 config 项：`erp-fin.annual-close-enabled`（默认 true）、`erp-fin.period-generate-skip-existing`（默认 false）、`erp-fin.bank-fx-revaluation-enabled`（默认 true，银行存款外币重估总开关）。
- 依赖既有汇率源（`ErpMdExchangeRate`，AR/AP 重估已用）；依赖会计科目表（本年利润/未分配利润科目）；无新基础设施。
- 无数据迁移（用既有 ORM 字段 + 加性业务类型）；回滚策略：config 关闭年度结转/银行重估 + 移除年度分支 + 移除业务类型（yearOpening 字段保留无害）。

## Execution Plan

### Phase 1 - 业务类型加性 + 次年期间生成入口

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（`erp-fin/business-type` 加 PROFIT_TO_RETAINED_EARNINGS）；`ErpFinBusinessType` 枚举；`ErpFinAccountingPeriodBizModel`（`generateNextYearPeriods`）；`ErpFinErrors`/`ErpFinConstants`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`：在计划记录本年利润→未分配利润业务类型 + 年度触发判定 + 次年期间幂等 + 银行重估范围裁决（见 Task Route Key Decisions）
  - Skill: `nop-backend-dev`
- [x] `Add`：`erp-fin/business-type` 加性追加 `PROFIT_TO_RETAINED_EARNINGS` + `ErpFinBusinessType` 枚举同步（保护区域契约同步，参照 0831-2 范式）
  - Skill: `nop-backend-dev`
- [x] `Add`：`generateNextYearPeriods(year)` @BizMutation——按 year 生成 1-12 月期间（startDate/endDate 按月首月末 + year 字段 + status 1 月视配置、其余未来态）；幂等门控（默认抛 `ERR_PERIODS_ALREADY_EXIST`；config skip-existing=true 时仅补缺失）；重新 codegen 无需（用既有 ErpFinAccountingPeriod 实体）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅证明业务类型一致 + 期间生成入口可用；年度结转接线在 Phase 2 验证。

- [x] `PROFIT_TO_RETAINED_EARNINGS` 在 ORM 字典与枚举一致（grep 双向命中）；`generateNextYearPeriods(2027)` 生成 12 条期间，重复调用抛错或按 config 补缺

### Phase 2 - 年度结转分支（本年利润→未分配利润 + 年初余额 + 触发次年创建）

Status: completed
Targets: `ErpFinAccountingPeriodProcessor` / `ErpFinAccountingPeriodBizModel`（年度分支）；`ProfitToRetainedEarningsService`（或扩展既有 `ProfitLossClosingService`）；过账 Provider
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：`closePeriod` 增年度分支——常规月度结账完成后，判定期间为年末（year + 序号=12）且 config `annual-close-enabled` → 执行年度追加：(a) 本年利润科目余额→未分配利润科目结转凭证（PROFIT_TO_RETAINED_EARNINGS，借/贷本年利润 / 贷/借未分配利润，本年利润清零）；(b) populate 次年科目年初余额（yearOpeningDebit/Credit）；(c) 调 `generateNextYearPeriods(year+1)` 触发次年创建（config-gated 是否自动触发）
  - Skill: `nop-backend-dev`
- [x] `Add`：辅助账跨年对账门控——年度结转前校验存货/AR-AP/资产辅助账余额与总账一致（不一致抛 ErrorCode 阻止年度结账；校验范围 config-gated），不做物理搬移
  - Skill: `nop-backend-dev`
- [x] `Add`：反结账覆盖——`reverseClose` 红冲年度结转凭证（PROFIT_TO_RETAINED_EARNINGS）与月度反结账一致范式；次年期间已创建则阻止反结账（需先删次年期间，Decision 裁定或抛错）
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 12 月结账后本年利润科目清零 + 未分配利润科目累计本年净利润 + 生成 PROFIT_TO_RETAINED_EARNINGS 凭证；次年科目 yearOpening 余额 populate
- [x] 辅助账与总账不一致时年度结账被阻止（ErrorCode）
- [x] 反结账红冲年度结转凭证；次年期间已存在时反结账按 Decision 处置（阻止/级联）

### Phase 3 - 银行存款外币汇兑重估 + 测试 + owner doc

Status: completed
Targets: `ExchangeRevaluationService`（银行存款分支）；行为测试（fin-service）；`docs/design/finance/period-close.md`；`docs/design/finance/treasury.md`；`docs/logs/2026/07-05.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Add`：`ExchangeRevaluationService` 扩展——重估 `ErpFinFundAccount.currencyId != functional` 银行存款余额（账户余额真相源 Phase 1 核实：FundAccount.balance 或科目聚合）× 期间末汇率；差额 EXCHANGE_GAIN_LOSS 凭证（借/贷银行存款 / 贷/借汇兑损益）；config-gated `bank-fx-revaluation-enabled`；与 AR/AP 重估同事务同汇率源
  - Skill: `nop-backend-dev`
- [x] `Proof`：行为测试——年度结转（12 月结账→本年利润清零→未分配利润累计→PROFIT_TO_RETAINED_EARNINGS 凭证→次年 yearOpening populate→次年 12 期间创建）+ 反结账红冲 + 辅助账对账门控 + 银行存款外币重估（外币账户差额凭证/本位币账户无重估/config 关闭）+ 次年期间幂等；策略 `JunitAutoTestCase`，`mvn test -pl module-finance/erp-fin-service -am`
  - Skill: `nop-backend-dev`
- [x] `Add`：owner doc 对齐——`period-close.md` §年度结转规则标记落地 + §汇兑重估银行存款补注（移除 Non-Goal「银行存款外币重估」）+ §配置项增 annual-close/period-generate/bank-fx 键；`treasury.md` 银行存款外币重估补注
  - Skill: none

Exit Criteria:

- [x] 新增年度结转 + 银行重估行为测试全绿；1000-3 月度结账套件（93）零回归
- [x] owner doc 偏离补注收口（年度结转 Non-Goal + 银行存款重估 Non-Goal 移除/标记落地）

## Draft Review Record

- Independent draft review iteration 1: needs-revision（ses_0d0e5b105ffeRtvomQHta3D0ST，general 独立子代理新会话）because Current Baseline 事实错误（规则 1）：银行存款 `ErpFinFundAccount.currencyId` 引用写成 `orm.xml:295 propId 10`（实属 `ErpFinVoucherLine`），真实位置 `app-erp-finance.orm.xml:691 propId 9`（实体定义 :679）。其余基线全核实通过（月度结账 closePeriod 无年度分支、period-close.md §年度结转规则 :231、ErpFinAccountingPeriod year/startDate/endDate、yearOpeningDebit/Credit :605-606、ExchangeRevaluationService :36 Non-Goal、business-type 无 PROFIT_TO_RETAINED_EARNINGS、1000-3 双 Deferred + 0115-2 completed）。年结+银行 FX 同结果面（period-close 完整性）合规。
- 已修订（iter1→iter2）：currencyId 引用修正为 `app-erp-finance.orm.xml:691 propId 9`（实体 :679）+ 补 ExchangeRevaluationService.java:36 Non-Goal 出处。
- Independent draft review iteration 2: accept（ses_0d0e25e41ffe4fCM6BpaLu7PDu，general 独立子代理新会话）— 修正核实通过（:691 propId 9 属实，旧 :295/propId 10 属 ErpFinVoucherLine 属实）；全 Current Baseline 复核无新事实错误；14 规则 + 反松弛全过；年结+银行 FX 同结果面（规则 14 组件增强形态）合规。共识达成，转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（年度结转三步 + 次年期间生成 + 反结账覆盖 + 银行存款外币重估）
- [x] 相关文档对齐（period-close.md / treasury.md / 当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 年度报表渲染（资产负债表/利润表/现金流量表）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 nop-report 报表面；本计划仅做结转与年初余额 populate 供报表消费。
- Successor Required: yes（触发条件：nop-report 接线时）

### 利润分配明细（法定/任意盈余公积、应付股利）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅"本年利润→未分配利润"基础结转；多级利润分配是独立结果面。
- Successor Required: yes（触发条件：利润分配政策配置需求落地时）

### 多账套/合并报表年度结转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 `intercompany-consolidation.md` 独立结果面。
- Successor Required: yes（触发条件：多公司合并上线时）

### 历史年度追溯结转/年度重算

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 仅向前（当前年末→次年初）；历史年度已 CLOSED_FINAL 重算需反结账链。
- Successor Required: yes（触发条件：历史年度重述需求时）

## Closure

Status Note: 全部三个 Phase 实现完成并验证通过（fin-service 162 测试全绿含 7 新增；全仓 `mvn test` BUILD SUCCESS 零回归）。年度结转三步（本年利润→未分配利润 + 次年年初余额 populate + 次年 12 期间创建）+ 辅助账跨年对账门控 + 反结账覆盖（次年期间已存在阻止）+ 银行存款外币汇兑重估均已落地；owner doc（period-close.md / treasury.md）Non-Goal 移除/标记落地 + 配置项回流。独立结束审计由独立子代理（新会话）完成：实时核实 AnnualCloseService / Processor（closeAnnual/isYearEnd/hasNextYearPeriods/reverseClose 年度覆盖/generateNextYearPeriods+ERR_PERIODS_ALREADY_EXIST）/ ExchangeRevaluationService.revalueBankDeposits（:69 接线）/ ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS=380 + 字典双向同步 / 测试三件（TestErpFinAnnualClose/TestErpFinAuxiliaryReconGate/TestErpFinReverseClose）全部存在于仓库，且运行时接线闭环（closePeriod→isYearEnd→closeAnnual→generateNextYearPeriods；revalue→revalueBankDeposits），无 hollow 占位。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-audit 新会话，不复用执行者上下文）
- Evidence: 
  - 实现：`module-finance/erp-fin-service/.../annualclose/AnnualCloseService.java`、`.../processor/ErpFinAccountingPeriodProcessor.java`（closeAnnual/isYearEnd/hasNextYearPeriods/reverseClose 年度覆盖/generateNextYearPeriods）、`.../fx/ExchangeRevaluationService.java`（revalueBankDeposits）、`.../entity/ErpFinAccountingPeriodBizModel.java` + `module-finance/erp-fin-dao/.../biz/IErpFinPeriodCloseBiz.java`（generateNextYearPeriods 契约）、`.../dao/ErpFinBusinessType.java`（PROFIT_TO_RETAINED_EARNINGS=380）、`module-finance/model/app-erp-finance.orm.xml`（business-type dict）、`.../service/ErpFinErrors.java` + `ErpFinConstants.java`、`.../beans/app-service.beans.xml`（AnnualCloseService bean）
  - 测试：`TestErpFinAnnualClose`（6 case：年度结转清零+未分配利润累计+次年期间+yearOpening / 反结账次年存在阻止 / 年度凭证生成 / 次年期间幂等抛错 / 银行外币重估 / 本位币无重估）、`TestErpFinAuxiliaryReconGate`（1 case：辅助账不一致阻止）、`TestErpFinReverseClose` 调整为 6 月隔离月度反结账
  - 验证：`mvn clean install -DskipTests`（全仓 146 模块）BUILD SUCCESS；`mvn test -pl module-finance/erp-fin-service -am` Tests run: 162, Failures: 0, Errors: 0；全仓 `mvn test` BUILD SUCCESS

Follow-up:

- 年度报表渲染（见上方 Deferred）
- 利润分配明细（见上方 Deferred）
