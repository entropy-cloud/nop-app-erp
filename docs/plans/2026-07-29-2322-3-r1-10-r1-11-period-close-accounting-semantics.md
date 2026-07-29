# 2026-07-29-2322-3-r1-10-r1-11-period-close-accounting-semantics R1.10+R1.11 — 期末结账会计语义 + 反结账/凭证锁定

> Plan Status: active
> Last Reviewed: 2026-07-29
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

Status: planned
Targets: `ErpFinAccountingPeriodProcessor`、`PeriodPreCheckReport`、`AnnualCloseService`、`ErpFinConstants`、owner docs
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

- [ ] Explore: 核实 `ErpFinGlBalance` 维护方——确认是否「未由过账引擎维护」（P1-MA2-018 注释明示），评估补 GL 余额维护（过账引擎写入 opening/closing）的可行性 vs documented simplification 的代价。
  - Skill: `nop-backend-dev`
- [ ] Decision: P1-MA2-017 auto-post-on-close 裁决——拆分为独立 config（auto-post 动作 config + pre-check 阻断 config），统一默认值（推荐代码 false=阻断更安全），未核销 AR-AP 移出 `hasIssues` 阻断集改为结构化提示，allowance shortfall 独立硬阻断（不受 auto-post-on-close 影响）。记录替代方案。
  - Skill: `nop-backend-dev`
- [ ] Decision: P1-MA2-018/019 年初余额累计 + 对账作用域裁决——选择 A（补 GL 余额维护使年初余额=累计期末 + 辅助账按年度过滤）或 B（documented simplification + owner doc 标注「当前仅首年精确」+ 辅助账对账作用域限定单年）。记录残留风险。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 三项 Decision（auto-post 语义 / 年初余额 / 对账作用域）已记录选择、替代方案、残留风险

### Phase 2 - 反结账/凭证锁定/FX reversal 裁决（Decision-heavy，覆盖 R1.11 三项）

Status: planned
Targets: `ErpFinAccountingPeriodProcessor.reverseClose`、`ErpFinVoucherBizModel`、`ExchangeRevaluationService`、owner docs
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: 无（与 Phase 1 可并行裁决）

- [ ] Decision: P1-MA2-020 反结账审批裁决——选择 A（实现审批流：反结账申请→审批→执行）或 B（owner doc 标注「当前 config kill-switch，审批流 successor」）。注意：完整审批流（xwf）可行性已有 plan `2026-07-09-2330-1` 裁决参考。记录理由。
  - Skill: `nop-backend-dev`
- [ ] Decision: P1-MA2-021 CLOSED_FINAL 凭证锁定裁决——选择 A（postVoucher/reverseVoucher/update/delete 前校验 period.status != CLOSED/CLOSED_FINAL + ErrorCode）或 B（owner doc 标注「锁定语义经期间状态机 + 操作权限间接保证」）。
  - Skill: `nop-backend-dev`
- [ ] Decision: P1-MA2-022 FX 重估前期 reversal 裁决——选择 A（实现前期 FX 凭证期末自动 reversal + 期间过滤 + 更新 openAmountFunctional）或 B（owner doc 标注「当期 spot-rate 重估，无前期 reversal」为已知简化，config-gated）。记录 IAS 21 合规性残留风险。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 三项 Decision（反结账审批 / 凭证锁定 / FX reversal）已记录选择、替代方案、残留风险

### Phase 3 - 实现全部裁决结果（Fix | Add）

Status: planned
Targets: Phase 1+2 裁决确定的代码/文档面
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1+2 裁决完成

- [ ] Fix: P1-MA2-017 auto-post-on-close 语义拆分——拆分 config + 未核销 AR-AP 改结构化提示 + allowance shortfall 独立硬阻断 + owner doc `period-close.md` 默认值对齐代码。
  - Skill: `nop-backend-dev`
- [ ] Fix: 按 P1-MA2-018/019 裁决落地（GL 余额维护 or documented simplification + owner doc 标注）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 按 P1-MA2-020 裁决落地（审批流 or kill-switch successor owner doc 标注）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 按 P1-MA2-021 裁决落地（期间状态守卫 or owner doc 间接保证标注）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 按 P1-MA2-022 裁决落地（FX 前期 reversal + 期间过滤 or 已知简化 owner doc 标注）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 六项发现的裁决方案在代码或 owner doc 中落地
- [ ] 新增/拆分 config（若有）默认值保护既有测试基线

### Phase 4 - 期间结账验证（Proof）

Status: planned
Targets: 新增/修改期间结账 + FX 重估 E2E 测试
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 3 完成

- [ ] Proof: 若实现凭证锁定——新增 CLOSED_FINAL 期间 postVoucher/reverseVoucher/update assertThrows（非法操作负向）。
  - Skill: `nop-testing`
- [ ] Proof: 若实现 FX 前期 reversal——新增多期 FX 重估 E2E（前期凭证期末自动 reversal + 累计不漂移断言）。
  - Skill: `nop-testing`
- [ ] Proof: 若实现 GL 余额维护——新增多年结转年初余额断言（闭合 P1-MA2-018）。
  - Skill: `nop-testing`
- [ ] Proof: 既有期间结账 E2E 回归通过。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 裁决为「实现」的发现项有对应正向 + 负向测试断言
- [ ] 既有期间结账测试零回归

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_05184514dffeiuDeJOru6WpDWa) — 6 项发现基线均经 arm-index + 代码核实（reverseClose kill-switch + auto-post-on-close 默认 false + postVoucher 不校验期间状态 + period-close.md doc/code 默认值语义双偏离）；R1.10+R1.11 合并是规则 14 正确应用（同组件 ErpFinAccountingPeriodProcessor + 共享 period-close.md + 同会计保护区域 + 同 implement-or-document 模式）；R1.13 排除边界正确（state-machine dict 死状态清理=不同结果面）；6 项 Decision 均含 A/B 替代方案 + Explore 覆盖 P1-MA2-018 GL 余额维护可行性。采纳非阻塞建议：Task Route 措辞精确化 + 新增保护区域裁决门控 + 新增 Adjacent Remediation 交叉引用 P1-MA4-004/R1.16 + Phase 3 item-type 前缀统一 Fix。

## Closure Gates

> 完整仓库验证在结束时运行一次。

- [ ] 范围内行为完成（R1.10 三项 + R1.11 三项裁决落地）
- [ ] 相关文档对齐（`period-close.md`/`state-machine.md`/`bad-debt.md`/`ar-ap-reconciliation.md` 反映裁决结果）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（finance 模块重点）+ compliance checker 基线不高于 M0
- [ ] 无范围内项目降级为 deferred/follow-up（已确认缺陷 P1-MA2-017~022 不得降级）
- [ ] 会计保护区域裁决已落地：所选方案（实现或 documented simplification）有 owner-doc 证据支撑 + 残留风险已记录（由独立结束审计核验）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中 + arm-index P1-MA2-017~022 状态回填

## Deferred But Adjudicated

_（暂无；Phase 1+2 裁决可能产生 deferred 项——如反结账审批流若裁决为 successor、GL 余额维护若裁决为 documented simplification——届时按格式登记并命名后继触发条件。）_

## Closure

Status Note: <待 closure audit>

Closure Audit Evidence:

- Auditor / Agent: <独立审计子代理>
- Evidence: <task id / log link>

Follow-up:

- <非阻塞跟进项；已确认缺陷不得出现在此>
