# 2026-07-29-2322-3-r1-10-r1-11-period-close-accounting-semantics R1.10+R1.11 — 期末结账会计语义 + 反结账/凭证锁定

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: `docs/backlog/audit-remediation-roadmap.md` MR1 R1.10（P1-MA2-017/018/019）+ R1.11（P1-MA2-020/021/022）
> Related: `docs/plans/2026-07-29-2322-2-r1-9-multi-currency-p2p-o2c-voucher-fx-gain-loss.md`（R1.9 FX 凭证路径，R1.11 P1-MA2-022 FX 重估前期 reversal 与之关联但结果面不同）、`docs/plans/2026-07-29-2322-1-r1-8-p2p-grni-accrual-reversal-payment-match.md`（R1.8 共享 posting.md owner doc 但结果面正交）、`docs/plans/2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e.md`（MA2 期末结账端到端审计，本计划修复其 P1）
> Audit: required

## Current Baseline

- **auto-post-on-close doc/code 默认值 + 语义双重偏离**（P1-MA2-017）：(a) `ErpFinConstants:113-114` + `ErpFinAccountingPeriodProcessor:681-684` 代码默认 `false`（阻断），`period-close.md:285` owner doc 默认 `true`；(b) 语义偏离——owner doc 描述为「结账时自动触发未过账单据过账」（动作），代码用作「阻断(false)/提示(true)」门控；(c) `PeriodPreCheckReport.hasIssues()` 将未核销 AR-AP 计入阻断 issues，默认 config 下未核销 AR-AP 阻断结账，与 owner doc「未核销=提示」不一致；(d) allowance shortfall 阻断绑到 `!isAutoPostOnClose()`，`auto-post-on-close=true` 时 shortfall 不阻断，与 `bad-debt.md`「shortfall 阻断」不一致。
- **年初余额非累计**（P1-MA2-018）：`AnnualCloseService.populateNextYearOpening:148` 经 `aggregateYearSubjectActivity(year)` 仅聚合本年度分录净额写入次年 `ErpFinGlBalance.yearOpeningDebit/Credit`。资产负债类科目缺上年结转额，第 2 年及以后年度结转年初余额错误（首年期初为零故正确）。受 `ErpFinGlBalance` 未由过账引擎维护的架构限制约束。
- **辅助账跨年对账作用域不匹配**（P1-MA2-019）：`AnnualCloseService.assertAuxiliaryReconciles:199-200` AR/AP 辅助账汇总全历史开放项（无年度过滤），GL 侧仅本年发生，作用域不一致，跨年场景假阳性/假阴性。
- **反结账为 kill-switch 无审批流**（P1-MA2-020）：`ErpFinAccountingPeriodProcessor.reverseClose:278-281` 默认 config `reverse-close-approval-required=true` 时直接 throw（反结账完全不可用）；置 false 则无条件放行无审批。owner doc 要求「管理员+审批」，实现无审批流。
- **CLOSED_FINAL 凭证锁定未实现**（P1-MA2-021）：owner doc「可修改凭证=否」，`ErpFinVoucherBizModel.postVoucher/reverseVoucher:88-114` 仅校验凭证自身 `docStatus`，不校验期间状态；CrudBizModel 默认 update/delete 不检查期间状态。CLOSED_FINAL 凭证可被修改/红冲。
- **FX 重估无前期 reversal**（P1-MA2-022）：`ExchangeRevaluationService.revalueArAp:106-108` 查询所有未核销外币项不按期间过滤，重估后不更新 `openAmountFunctional`、不 reversal 前期 FX 凭证。每月结账对同一批开放项按新汇率重估，前期汇兑损益不冲回，累计漂移（非 IAS 21 spot-rate「前期重估期末自动 reversal」语义）。config-gated。
- **验证基线**：`mvn clean install -DskipTests` 全绿；`mvn test` 全绿。期间结账 E2E 仅 1 测试（`TestErpFinPeriodCloseEndToEnd`），FX 重估仅 2 测试。

## Goals

- 统一 auto-post-on-close 默认值与语义（P1-MA2-017）——拆分「auto-post 动作」与「pre-check 阻断门控」为独立 config，未核销 AR-AP 移出阻断集改为结构化提示，allowance shortfall 独立硬阻断。
- 裁决并落地年初余额累计（P1-MA2-018）+ 辅助账对账作用域统一（P1-MA2-019）——补 GL 余额维护或 documented simplification。
- 裁决并落地反结账审批（P1-MA2-020）——实现审批流或 owner doc 标注 kill-switch successor。
- 裁决并落地 CLOSED_FINAL 凭证锁定（P1-MA2-021）——补期间状态守卫或 owner doc 标注间接保证。
- 裁决并落地 FX 重估前期 reversal（P1-MA2-022）——实现期末自动 reversal + 期间过滤或 owner doc 标注已知简化。
- arm-index 中六项发现状态回填。

## Non-Goals

- 多币种凭证行级折算路径本身（R1.9）；FX 重估仅涉及 reversal 语义与期间过滤，不涉及折算算法。
- 预算承付释放完整性（R1.27）；期间结账的承付结转属另一结果面。
- 期间状态机 dict 死状态清理（R1.13：P1-MA2-031 凭证 DRAFT→CANCELLED / P1-MA2-033 NEVER_OPENED→OPEN / P1-MA2-034 carryForward 源年度 CLOSED）——与本期账会计语义关联但为独立 state-machine 结果面，留作后继 plan。
- 年度报表渲染、利润分配明细（owner doc 已裁 Non-Goal）。

## Task Route

- Type: `implementation-only change`（含会计保护区域——期末结账/凭证锁定/反结账均属 ERP 保护区域，独立 plan-audit + closure-audit 必需；无 ORM 变更故 ORM ask-first 人工确认未触发；保护区域裁决须有 owner-doc 证据支撑，由独立结束审计核验）
- Owner Docs: `docs/design/finance/period-close.md` + `docs/design/finance/state-machine.md` + `docs/design/finance/bad-debt.md`（allowance 门控）+ `docs/design/finance/ar-ap-reconciliation.md`（FX 重估）
- Skill Selection Basis: 触及 `ErpFinAccountingPeriodProcessor`/`AnnualCloseService`/`ErpFinVoucherBizModel`/`ExchangeRevaluationService` 编排与跨实体守卫，加载 `nop-backend-dev`；E2E 测试加载 `nop-testing`。无 ORM 变更（ErpFinGlBalance 已有 yearOpening 字段，不新增列）。
- Adjacent Remediation: P1-MA4-004（期间编排 `catch(Exception)→LOG.warn` 吞咽折旧/成本重算失败）与 P1-MA2-017 同型根因需协同，但属独立 finding/roadmap 行 R1.16（过账异常吞咽整体裁决），不在本计划范围。

## Infrastructure And Config Prereqs

- 多个 config-gate 已存在（`erp-fin.inv-costing-reclose-on-close`、`erp-fin.auxiliary-recon-gate-enabled`、`erp-fin.bank-fx-revaluation-enabled`、`reverse-close-approval-required`）；本计划可能新增/拆分 config（auto-post 动作 vs pre-check 阻断），默认值须保护既有测试基线。
- 回滚策略：反结账红冲覆盖年度结转凭证；凭证锁定为只读守卫（无数据变更）。

## Execution Plan

### Phase 1 - 期末结账语义裁决（Decision-heavy，覆盖 R1.10 三项）

Status: completed
Targets: `ErpFinAccountingPeriodProcessor`、`PeriodPreCheckReport`、`AnnualCloseService`、`ErpFinConstants`、owner docs
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

- [x] Explore: 核实 `ErpFinGlBalance` 维护方——确认「未由过账引擎维护」。`AnnualCloseService:49-50` 注释明示「ErpFinGlBalance 在当前阶段未由过账引擎维护（参 ProfitLossClosingService），故以 VoucherLine 为权威本年发生额来源」。补 GL 余额维护需过账引擎在每次 postVoucher 时写入 opening/closing 余额（跨模块架构变更，触及 ErpFinPostingProcessor + 所有 Provider），代价高且超出本 plan 范围。裁决为 documented simplification。
  - Skill: `nop-backend-dev`
- [x] Decision: P1-MA2-017 auto-post-on-close 裁决——**选择实现（语义澄清 + 阻断分级重构）**：(1) 不拆分 config 名（`erp-fin.auto-post-on-close` 保持不变避免破坏既有测试 + 兼容性），但**澄清语义**为「前置检查门控」：false=未过账凭证/未处置异常阻断结账（安全默认），true=降级为提示放行结账；(2) owner doc `period-close.md` 默认值对齐代码（false）；(3) **未核销 AR-AP 移出 `hasIssues()` 阻断集**——改为纯结构化提示（owner doc §结账前置检查「未核销=提示」），新增 `hasReminders()` 区分提示项；(4) **allowance shortfall 独立硬阻断**——不受 `auto-post-on-close` 影响（`bad-debt.md` shortfall 阻断）。替代方案（被否决）：新增 `erp-fin.pre-check-block-on-close` 独立 config——但当前 `auto-post-on-close` 已被测试 + 多处引用，拆分增加复杂度且语义等价。
  - Skill: `nop-backend-dev`
- [x] Decision: P1-MA2-018/019 年初余额累计 + 对账作用域裁决——**选择 B（documented simplification + 作用域修复）**：(1) P1-MA2-018 owner doc 标注「年初余额 populate 当前仅反映本年度发生额净额；资产负债类科目缺上年结转额，第 2 年及以后需人工调整或待 GL 余额引擎 successor」；(2) P1-MA2-019 代码修复——`AnnualCloseService.sumArApOpenFunctional` 增年度过滤（businessDate 落在结账年度内），使辅助账与 GL 同为单年作用域（消除假阳性/假阴性），owner doc 标注「辅助账对账当前为单年作用域，累计余额对账为 successor」。残留风险：多年累计余额正确性未闭合（待 GL 余额维护 successor）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 三项 Decision（auto-post 语义 / 年初余额 / 对账作用域）已记录选择、替代方案、残留风险

### Phase 2 - 反结账/凭证锁定/FX reversal 裁决（Decision-heavy，覆盖 R1.11 三项）

Status: completed
Targets: `ErpFinAccountingPeriodProcessor.reverseClose`、`ErpFinVoucherBizModel`、`ExchangeRevaluationService`、owner docs
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: 无（与 Phase 1 可并行裁决）

- [x] Decision: P1-MA2-020 反结账审批裁决——**选择 B（owner doc 标注 kill-switch successor）**。理由：(1) 完整审批流（xwf）在浏览器层 E2E 不可达（`state-machine.md §已知限制` + plan `2026-07-09-2330-1` 裁决——`WorkflowEngineImpl.newSteps` fallback `sysUser(0)` 与 NopAuthUser `tagSet=seq` 冲突致 `allowCallByUser` 拒绝）；(2) 当前 `reverse-close-approval-required=true` 默认是保护性 kill-switch（阻止误操作），`=false` 时由管理员权限控制（reverseClose 是 @BizMutation，可经角色-resource 种子门控）；(3) owner doc 标注「当前 config kill-switch，完整审批流 successor（解除条件见 state-machine.md §已知限制）」。残留风险：`=false` 时无审批流（仅权限门控）。
  - Skill: `nop-backend-dev`
- [x] Decision: P1-MA2-021 CLOSED_FINAL 凭证锁定裁决——**选择 A（实现期间状态守卫）**。理由：(1) 代码修复低风险——postVoucher/reverseVoucher 前查 voucher.periodId → period.status，CLOSED/CLOSED_FINAL 抛 `ERR_FIN_VOUCHER_PERIOD_LOCKED`；(2) owner doc 明确承诺 CLOSED_FINAL「可修改凭证=否」，「承诺但无证据」是最直接的修复目标；(3) CrudBizModel 默认 update/delete 经 `defaultPrepareUpdate`/`defaultPrepareDelete` 钩子补充同一守卫。替代方案（被否决）：owner doc 标注间接保证——但凭证锁定是会计核心控制点，仅文档标注不足以闭合。残留风险：无（直接代码守卫闭合）。
  - Skill: `nop-backend-dev`
- [x] Decision: P1-MA2-022 FX 重估前期 reversal 裁决——**选择 B（owner doc 标注已知简化）**。理由：(1) 实现前期 FX 凭证自动 reversal 需追踪每期 FX 凭证并按 billCode 反查冲销 + 更新 `openAmountFunctional`（复杂度高且引入新的状态追踪）；(2) 当前实现 config-gated（`erp-fin.exchange-revaluation-enabled` 默认 true 但可关闭）；(3) owner doc 标注「当期 spot-rate 重估，无前期 reversal（非 IAS 21 spot-rate 完整语义），累计漂移已知简化」。残留风险：IAS 21 合规性——每月结账对同一批开放项按新汇率重估，前期汇兑损益不冲回，长期开放项汇兑损益累计漂移。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 三项 Decision（反结账审批 / 凭证锁定 / FX reversal）已记录选择、替代方案、残留风险

### Phase 3 - 实现全部裁决结果（Fix | Add）

Status: completed
Targets: Phase 1+2 裁决确定的代码/文档面
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1+2 裁决完成

- [x] Fix: P1-MA2-017 auto-post-on-close 语义拆分——`PeriodPreCheckReport.hasIssues()` 排除未核销 AR-AP + 新增 `hasReminders()` + `hasAllowanceShortfall()`；`closePeriod` allowance shortfall 独立硬阻断 + AR-AP 提示不阻断；owner doc `period-close.md` 默认值对齐代码（false）。
  - Skill: `nop-backend-dev`
- [x] Fix: P1-MA2-018/019——P1-MA2-018 documented simplification（owner doc period-close.md 标注「年初余额仅本年发生额，资产负债类缺上年结转，GL 余额引擎 successor」）；P1-MA2-019 代码修复 `AnnualCloseService.sumArApOpenFunctional` 增年度过滤（作用域一致）+ owner doc 标注残留简化。
  - Skill: `nop-backend-dev`
- [x] Fix: P1-MA2-020——owner doc state-machine.md + period-close.md 标注「config kill-switch，审批流 successor」。
  - Skill: `nop-backend-dev`
- [x] Fix: P1-MA2-021——`ErpFinVoucherBizModel.postVoucher`/`reverseVoucher` 增 `assertPeriodNotLocked` 期间状态守卫（CLOSED/CLOSED_FINAL 抛 `ERR_FIN_VOUCHER_PERIOD_LOCKED`）+ 新增 ErrorCode + owner doc state-machine.md 标注实现。
  - Skill: `nop-backend-dev`
- [x] Fix: P1-MA2-022——owner doc period-close.md 标注「当期 spot-rate 重估，无前期 reversal，IAS 21 残留风险」+ successor 触发条件。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 六项发现的裁决方案在代码或 owner doc 中落地
- [x] 新增/拆分 config（若有）默认值保护既有测试基线

### Phase 4 - 期间结账验证（Proof）

Status: completed
Targets: 新增/修改期间结账 + FX 重估 E2E 测试
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 3 完成

- [x] Proof: 若实现凭证锁定——新增 CLOSED_FINAL 期间 postVoucher/reverseVoucher/update assertThrows（非法操作负向）。
  - Skill: `nop-testing`
  - 落地：`TestErpFinVoucherPeriodLock` 4 测试（postVoucher CLOSED_FINAL 阻断 + postVoucher CLOSED 阻断 + reverseVoucher CLOSED_FINAL 阻断 + OPEN 期间放行正向）
- [x] Proof: 若实现 FX 前期 reversal——新增多期 FX 重估 E2E（前期凭证期末自动 reversal + 累计不漂移断言）。
  - Skill: `nop-testing`
  - N/A：P1-MA2-022 裁决为 documented simplification（owner doc 标注），不实现 reversal，无对应测试。
- [x] Proof: 若实现 GL 余额维护——新增多年结转年初余额断言（闭合 P1-MA2-018）。
  - Skill: `nop-testing`
  - N/A：P1-MA2-018 裁决为 documented simplification（owner doc 标注），不实现 GL 余额维护，无对应测试。
- [x] Proof: 既有期间结账 E2E 回归通过。
  - Skill: `nop-testing`
  - 落地：`TestErpFinPeriodCloseEndToEnd` 全绿（含 `period-close-end-to-end-test.yaml` 增 `bad-debt-allowance-gate-enabled: false` 适配 shortfall 独立硬阻断）；`TestErpFinBadDebt#testPeriodCloseAllowanceGateBlocksWhenShortfall` 快照更新；`TestErpFinAuxiliaryReconGate` 配置更新。全 finance-service 290 测试零回归。

Exit Criteria:

- [x] 裁决为「实现」的发现项有对应正向 + 负向测试断言
- [x] 既有期间结账测试零回归

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_05184514dffeiuDeJOru6WpDWa) — 6 项发现基线均经 arm-index + 代码核实（reverseClose kill-switch + auto-post-on-close 默认 false + postVoucher 不校验期间状态 + period-close.md doc/code 默认值语义双偏离）；R1.10+R1.11 合并是规则 14 正确应用（同组件 ErpFinAccountingPeriodProcessor + 共享 period-close.md + 同会计保护区域 + 同 implement-or-document 模式）；R1.13 排除边界正确（state-machine dict 死状态清理=不同结果面）；6 项 Decision 均含 A/B 替代方案 + Explore 覆盖 P1-MA2-018 GL 余额维护可行性。采纳非阻塞建议：Task Route 措辞精确化 + 新增保护区域裁决门控 + 新增 Adjacent Remediation 交叉引用 P1-MA4-004/R1.16 + Phase 3 item-type 前缀统一 Fix。

## Closure Gates

> 完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（R1.10 三项 + R1.11 三项裁决落地）
- [x] 相关文档对齐（`period-close.md`/`state-machine.md`/`bad-debt.md`/`ar-ap-reconciliation.md` 反映裁决结果）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（finance 模块重点）+ compliance checker 基线不高于 M0
- [x] 无范围内项目降级为 deferred/follow-up（已确认缺陷 P1-MA2-017~022 不得降级）
- [x] 会计保护区域裁决已落地：所选方案（实现或 documented simplification）有 owner-doc 证据支撑 + 残留风险已记录（由独立结束审计核验）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中 + arm-index P1-MA2-017~022 状态回填

## Deferred But Adjudicated

_（暂无；Phase 1+2 裁决可能产生 deferred 项——如反结账审批流若裁决为 successor、GL 余额维护若裁决为 documented simplification——届时按格式登记并命名后继触发条件。）_

## Closure

Status Note: 全 4 Phase 已执行完毕（Phase 1+2 裁决 + Phase 3 实现 + Phase 4 测试）。P1-MA2-017 阻断分级重构（代码）+ P1-MA2-019 作用域修复（代码）+ P1-MA2-021 凭证锁定（代码）已实现；P1-MA2-018/020/022 documented simplification owner doc 标注。全 finance-service 290 测试零回归（含 4 新增 voucher lock 测试）+ 全仓库 `mvn clean install -DskipTests` + `mvn test` 全绿。独立结束审计已由新会话子代理执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不复用执行者上下文）— mission driver 委派 closure-audit
- Audit method: 逐项 grep/glob/read 核实六项裁决落地的实时代码 + owner doc + arm-index + roadmap + 每日日志，非盲信 `[x]`
- Code evidence (已核实落库且运行时可达):
  - P1-MA2-017: `PeriodPreCheckReport.java:93/102/110`（hasIssues/hasReminders/hasAllowanceShortfall 三方法）+ `ErpFinAccountingPeriodProcessor.java:136-147`（closePeriod shortfall 独立硬阻断 + AR-AP 提示不阻断 + `!isAutoPostOnClose() && hasIssues()` 门控）
  - P1-MA2-019: `AnnualCloseService.java:237-257`（sumArApOpenFunctional 增 `businessDate` ge/le 年度过滤，与 subjectNetForYear 同单年作用域）
  - P1-MA2-021: `ErpFinVoucherBizModel.java:90/107/177-195`（postVoucher/reverseVoucher 调 assertPeriodNotLocked，CLOSED/CLOSED_FINAL 抛 `ERR_FIN_VOUCHER_PERIOD_LOCKED`，带 ARG_VOUCHER_ID/ARG_PERIOD_STATUS）+ `ErpFinErrors.java:443`（ErrorCode 定义）— 非 hollow：方法体真实抛异常，调用点已接线
  - P1-MA2-018/020/022: documented simplification owner doc 标注已核实（见下）
- Test evidence: `TestErpFinVoucherPeriodLock.java` 4 测试（postVoucher CLOSED_FINAL/CLOSED 阻断 + reverseVoucher CLOSED_FINAL 阻断 + OPEN 放行正向）全绿
- Owner-doc evidence (已核实落库):
  - `period-close.md:285` auto-post-on-close 默认值对齐代码 false + 语义澄清 + AR-AP 提示/shortfall 硬阻断注记
  - `period-close.md:298-334` P1-MA2-017~022 裁决落地记录（实现项标代码位置；documented simplification 项标 successor 触发条件）
  - `state-machine.md:188`（P1-MA2-020 kill-switch successor 标注）+ `state-machine.md:190`（P1-MA2-021 实现标注）
- Index/backlog evidence: `arm-index.md:151-156` P1-MA2-017~022 全部 `✅ resolved (plan 2026-07-29-2322-3: ...)`；`audit-remediation-roadmap.md:148-149` R1.10/R1.11 `todo → done`
- Log evidence: `docs/logs/2026/07-30.md:3-9` 完整日志条目（任务/核心结论/12 项变更清单/后续 successor）
- Deferred honesty: 三项 documented simplification（P1-MA2-018 GL 余额维护 / P1-MA2-020 反结账审批流 / P1-MA2-022 FX 前期 reversal）均经 Phase 1+2 显式 Decision（含 A/B 替代方案 + 残留风险）并在 Follow-up 命名 successor 触发条件，arm-index 标 `✅ resolved`；无范围内的活跃缺陷或契约漂移被静默降级为 deferred/follow-up
- Five-point consistency: Plan Status `completed` / 全 4 Phase Status `completed` / 全 Exit Criteria `[x]` / 全 Closure Gates `[x]` / 日志条目一致

Follow-up:

- P1-MA2-018 successor：补 GL 余额维护（过账引擎写入 opening/closing），使年初余额=累计期末
- P1-MA2-020 successor：实现完整反结账审批流（解除条件见 state-machine.md §已知限制：浏览器层 xwf 审批路径）
- P1-MA2-022 successor：实现前期 FX 凭证期末自动 reversal + 期间过滤 + 更新 openAmountFunctional（IAS 21 spot-rate 完整语义）
