# 2026-08-06-1708-3 rc-ma4-a4-1-21-year-end-reverse-close-block-boundary 年末反结账阻断边界与 GlBalance yearOpening 残留一致性评估

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.21（MA4 运行时行为验证 — A1.6 §7 存疑点 4：UC-FIN-07 RC-4 年度结转凭证冲销 + RC-3 反结账——`ReverseCloseProcessor:32-36` 年末反结账阻断边界：次年期间已手动删除但 `ErpFinGlBalance` yearOpening 残留时，反结账红冲年度结转凭证是否致次年年初余额与凭证不一致）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.21；存疑点来源 `docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` §7 存疑点 4
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done）、`docs/plans/2026-08-06-1708-1-rc-ma4-a4-1-19-pc4-depreciation-autoexecute-vs-dangling-block-interaction.md` + `docs/plans/2026-08-06-1708-2-rc-ma4-a4-1-20-rc9-reverse-close-audit-trail-degraded-evidence.md`（同批次 period-close 同切片 A1.6）、`docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md`（A1.6 报告 §2.8 RC-3 年末反结账阻断 + §2.9 RC-4 年度结转凭证冲销 + §5.2 RC-3/RC-4 接受 + §7 存疑点 4 + §3.6 testReverseCloseBlockedWhenNextYearExists/testReverseCloseReversesAnnualVoucherWhenNoNextYear）、`docs/design/finance/period-close.md §反结账流程 :170-228`（L2 设计参考）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.21 验证报告（落盘 `docs/audits/2026-08-06-1708-rc-ma4-a4-1-21-year-end-reverse-close-block-boundary.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：读 `ReverseCloseProcessor:32-36` 年末阻断 + `hasNextYearPeriods` 逻辑 + 年度结转凭证红冲 + `ErpFinGlBalance` yearOpening 存储与 `AnnualCloseService.populateNextYearOpening` 填充 + 手动删次年期间后残留一致性推理 + 既有测试普查）。范式对齐 A4.1.18（done — period-close 运行时行为评估同型工作项）。

- **存疑点原文**（A1.6 报告 §7 存疑点 4，`2026-08-02-2100-...-a1-6-period-close.md` §7）：「年末反结账阻断边界——`ReverseCloseProcessor:32-36` 年末反结账阻断：若次年期间已手动删除但 `ErpFinGlBalance`/yearOpening 残留，反结账红冲年度结转凭证是否致次年年初余额与凭证不一致」。触发条件 = 12 月期间反结账（手动删次年期间后）。交 A4.1 运行时探针（闭合 RC-3 年末阻断 + RC-4 年度结转冲销在手动删次年期间边界场景的运行时数据一致性风险裁决）。

- **关联既有 finding**：
  - **RC-3**（A1.6 §5.2 裁决：接受）：UC-FIN-07 RC-3 `CLOSED_FINAL→OPEN` 一步迁移 + 年末反结账阻断（`:32-36 isYearEnd && hasNextYearPeriods → ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`）。
  - **RC-4**（A1.6 §5.2 裁决：接受）：UC-FIN-07 RC-4 冲销年度结转凭证（`:46-49 isYearEnd → reverseCloseVoucher(ANNUAL-CLOSE- + PROFIT_TO_RETAINED_EARNINGS)`）。
  - 本验证评估 RC-3 年末阻断 + RC-4 年度结转冲销在**手动删次年期间边界场景**的运行时数据一致性——年末阻断依赖 `hasNextYearPeriods`（查次年期间是否存在），若次年期间被手动删除但 `ErpFinGlBalance` yearOpening 残留（由 `AnnualCloseService.populateNextYearOpening` 创建，keyed by nextJan.getId()），则阻断失效 + 红冲年度凭证 + 残留 yearOpening 孤立 → 数据一致性风险。A1.6 §5.2 RC-3/RC-4 接受是**正常路径**（次年期间存在 → 阻断；无次年 → 红冲无残留）；本验证补**边界路径**（次年期间被删 + 残留）差异。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:129`（UC-FIN-07 heading）/ `:131-132`（RC-3 `CLOSED_FINAL → OPEN` + RC-4 冲销结转凭证逐字）/ `:135`（RC-9 全程审计，关联反结账约束）。L2 `period-close.md §反结账流程 :170-228`（年末反结账约束，设计参考）。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实，HEAD 经独立子代理实测，全在 module-finance）**：
  - 年末阻断（`ErpFinAccountingPeriodReverseCloseProcessor.java:32-36`）：`if (facade.isYearEnd(period) && period.getYear() != null && facade.hasNextYearPeriods(period.getYear() + 1)) → ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`。注释 :31「年度结转反结账门控：若该期间为年末且次年期间已创建，阻止反结账（须先删次年期间）」。
  - `hasNextYearPeriods`（`ErpFinAccountingPeriodProcessor.java:128-134`）：`daoProvider.daoFor(ErpFinAccountingPeriod.class)` + `q.addFilter(eq("year", nextYear))` + `setLimit(1)` + `return !findAllByQuery(q).isEmpty()`——查次年**任意期间**是否存在（非特指 1 月），setLimit(1) 返回非空即 true。
  - `isYearEnd`（`ErpFinAccountingPeriodProcessor.java:121-125`）：`year != null && month != null && month == 12`。
  - 年度结转凭证红冲（`ReverseCloseProcessor:46-49`）：`if (isYearEnd(period)) → reverseCloseVoucher(period, ANNUAL_BILL_CODE_PREFIX + code, PROFIT_TO_RETAINED_EARNINGS, context)`。`reverseCloseVoucher:553-562`（按 billCode + businessType 反查 `ErpFinVoucherBillR`，存在则 `voucherBiz.reverse`，无则 no-op）。此块**仅在年末阻断（:32-36）未触发时执行**（即无次年期间）。
  - `ErpFinGlBalance` yearOpening 存储（`app-erp-finance.orm.xml:924-925`）：`yearOpeningDebit`(:924)/`yearOpeningCredit`(:925) 列直接存于 `erp_fin_gl_balance`（非独立结构）。
  - yearOpening 填充（`AnnualCloseService.populateNextYearOpening:137-184`）：创建新 `gl` 行 `periodId = nextJan.getId()`，`setYearOpeningDebit/Credit` from net balance（:180-181）。幂等（:156-160 先清次年 1 月快照）。数据源 = 全年 `ErpFinVoucherLine` 聚合（排除 PROFIT_TO_RETAINED_EARNINGS，:293-317）。**nextJan 为空时静默跳过**（:142-146，次年期间未创建）。接线 = `ClosePeriodProcessor.closeAnnual:107` → `annualCloseService.executeAnnualClose` → `executeAnnualCloseForSchema:84-88` → `populateNextYearOpening`。
  - **边界场景（本存疑点核心）**：年末结账 `closeAnnual` 自动创建次年 12 期间 + 填充次年 1 月 yearOpening（`auto-generate-next-year-periods` 默认 true）。若运维**手动删除次年期间**（`ErpFinAccountingPeriod` 次年 12 行）但**未清** `ErpFinGlBalance` 次年 1 月 yearOpening 行（残留，keyed by 已删除的 nextJan.getId()）→ 反结账时 `hasNextYearPeriods` 返回 false（次年期间已删）→ 年末阻断失效 → 红冲年度结转凭证（恢复本年未分配利润）→ **残留 yearOpening 孤立**（次年期间已不存在，yearOpening 行无所属期间）→ 次年年初余额与凭证不一致（若次年期间被重建，yearOpening 残留可能与新期间错配）。

- **既有证据（复用输入）**：
  - A1.6 §2.8 + §5.2：RC-3 年末反结账阻断 + RC-4 年度结转凭证冲销已静态确认（正常路径接受）；§3.6 测试 `testReverseCloseBlockedWhenNextYearExists:77-85`（年末阻断覆盖，仅断言 NopException 未断言具体 ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS 错误码/参数）+ `testReverseCloseReversesAnnualVoucherWhenNoNextYear:89-96`（**名实不符**——仅断言年度结转凭证**生成**count>=1，**未调 reverseClose** + 未禁用次年创建[默认仍 auto-create 次年]，方法名承诺的"红冲"路径未验证）。本验证补「手动删次年期间边界场景」+ 既有测试缺口差异。
  - A2.3 period-close E2E（`2026-07-27-1949-arm-ma2-period-close-e2e.md`）：年末反结账正常路径行为 PASS（不含手动删次年边界场景）。

- **剩余差距**：年末反结账阻断的**边界场景**未验证——①手动删次年期间后 `hasNextYearPeriods` 返回 false 致阻断失效 + 红冲年度凭证 + 残留 yearOpening 孤立；②残留 yearOpening 的实际数据一致性影响（次年期间重建时错配 vs 永久孤立）；③既有测试 `testReverseCloseReversesAnnualVoucherWhenNoNextYear` 名实不符（未验证红冲路径）的实际风险。本验证闭合 RC-3/RC-4 在边界场景的运行时数据一致性风险裁决。

- **保护区域**：只读评估（读年末阻断 + hasNextYearPeriods + 年度凭证红冲 + yearOpening 存储/填充 + 手动删次年边界推理 + 既有测试普查），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若发现边界数据一致性风险，登记 finding 归 MR1，触及 ReverseCloseProcessor/hasNextYearPeriods/AnnualCloseService 逻辑须评估保护区域[RC 反结账属会计过账逻辑，若修复触及过账须 ask-first；纯 yearOpening 残留清理或 owner doc 标注可自动执行]）。

## Goals

- 年末反结账阻断逻辑核验：核验 `ReverseCloseProcessor:32-36`（`isYearEnd && hasNextYearPeriods(year+1) → ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`）+ `hasNextYearPeriods:128-134`（查次年任意期间 setLimit(1) 非特指 1 月）+ `isYearEnd:121-125`（month==12）——确认正常路径（次年期间存在 → 阻断）行为正确。
- 手动删次年期间边界场景一致性评估（本存疑点核心）：核验边界场景运行时行为——运维手动删次年 `ErpFinAccountingPeriod` 12 行但 `ErpFinGlBalance` 次年 1 月 yearOpening 行残留（keyed by 已删 nextJan.getId()）→ ①`hasNextYearPeriods` 返回 false（次年期间已删）→ 年末阻断失效；②红冲年度结转凭证（恢复本年未分配利润）；③残留 yearOpening 孤立（无所属期间）→ 次年年初余额与凭证不一致。评估残留 yearOpening 的实际数据一致性影响（永久孤立 vs 次年重建错配）。
- yearOpening 存储与填充核验：核验 `ErpFinGlBalance` yearOpening 直接存储（orm.xml:924-925 yearOpeningDebit/Credit）+ `AnnualCloseService.populateNextYearOpening:137-184`（创建 gl 行 periodId=nextJan.getId() + 幂等清快照 + nextJan 为空静默跳过 + 数据源全年 VoucherLine 聚合排除 PROFIT_TO_RETAINED_EARNINGS）——确认 yearOpening 由年度结账填充，手动删次年期间不清 yearOpening 行（无级联）。
- 年度结转凭证红冲核验：核验 `ReverseCloseProcessor:46-49`（`isYearEnd → reverseCloseVoucher(ANNUAL-CLOSE- + PROFIT_TO_RETAINED_EARNINGS)`）+ `reverseCloseVoucher:553-562`（按 billCode+businessType 反查 VoucherBillR，存在则 reverse，无则 no-op）——确认红冲仅恢复本年未分配利润，不触及次年 yearOpening 行。
- 既有测试覆盖边界普查：grep `testReverseCloseBlockedWhenNextYearExists:77-85`（年末阻断覆盖，仅断言 NopException 未断言错误码/参数）+ `testReverseCloseReversesAnnualVoucherWhenNoNextYear:89-96`（**名实不符**——仅断言凭证生成未调 reverseClose 未禁用次年创建）+ 手动删次年边界场景测试 全集，产出测试覆盖边界清单 + 标注名实不符测试缺口 + 边界场景缺口（零覆盖）。
- 对齐 UC-FIN-07 RC-3/RC-4 + §5.2 RC-3/RC-4 接受给出边界场景运行时裁决：①若手动删次年期间非常规运维 + 残留 yearOpening 孤立影响有限（次年期间不存在则 yearOpening 无消费方，无即时数据破坏）→ 登记 P2 watch-only（边界场景需手动删次年非常规操作 + 残留无即时消费方，主路径[次年存在阻断/无次年红冲]接受）；②若残留 yearOpening 在次年重建时错配致数据破坏 → 登记 P1（触及一致性，归 MR1）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.6 §5.2 RC-3/RC-4 接受[正常路径]分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复年末反结账边界一致性**（若发现残留风险，登记 finding 归 MR1；触及 ReverseCloseProcessor/hasNextYearPeriods 逻辑[反结账属会计过账] 须评估 ask-first；纯 yearOpening 残留级联清理或 owner doc 标注可自动执行）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 UC-FIN-07 全部验收标准**（A1.6 §5 已判 RC-3/RC-4 接受[正常路径] + RC-9 P1 + RC-1 复用 P1-MA3-046 + 其余接受；本验证只评 RC-3/RC-4 边界场景差异）。
- **不展开 A1.6 §7-2/§7-3**（A4.1.19 PC-4 折旧交互 / A4.1.20 RC-9 审计缺失）。
- **不实际执行手动删次年期间 + 反结账重现**（只读边界逻辑推理 + yearOpening 存储分析 + 既有测试 + 残留影响推理；真实手动删次年重现属 MR1 修复验证范围，非本验证范围）。
- **不重新裁决 RC-6 反结账成本凭证冲销缺失**（P2-RC-007 watch-only，A1.6 §5.3 已裁决）。

## Task Route

- Type: `verification or audit work`（年末反结账阻断边界一致性评估 + RC-3/RC-4 边界场景运行时裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.21 行）+ `docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` §7 存疑点 4 + §2.8 RC-3 + §2.9 RC-4 + §5.2 RC-3/RC-4 接受 + §3.6 测试覆盖（输入）+ `docs/design/finance/period-close.md §反结账流程 :170-228`（L2 设计参考）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。年末阻断边界评估需多维度归类（hasNextYearPeriods 查询逻辑 / 年度凭证红冲 / yearOpening 存储 + AnnualCloseService 填充 / 手动删次年边界残留推理 / 测试覆盖边界 + 名实不符测试 / RC-3/RC-4 接受维持-or-边界登记 finding / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读年末阻断 + hasNextYearPeriods + 年度凭证红冲 + yearOpening 存储/填充 + 手动删次年边界推理 + 既有测试普查）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 年末反结账阻断边界与 yearOpening 残留一致性评估

Status: planned
Targets: `docs/audits/2026-08-06-1708-rc-ma4-a4-1-21-year-end-reverse-close-block-boundary.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.21 行）；A1.6 done（§7 存疑点 4 已落盘 + §2.8 RC-3 + §2.9 RC-4 + §5.2 RC-3/RC-4 接受 + §3.6 测试）

- [ ] `Proof` 年末反结账阻断逻辑核验：给出 `ReverseCloseProcessor:32-36`（`isYearEnd && hasNextYearPeriods(year+1) → ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`）+ `hasNextYearPeriods:128-134`（查次年任意期间 setLimit(1)）+ `isYearEnd:121-125`（month==12）证据（file:line）。证实正常路径（次年期间存在 → 阻断）行为正确。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 手动删次年期间边界场景一致性评估（本存疑点核心）：核验边界场景运行时行为——运维手动删次年 `ErpFinAccountingPeriod` 12 行但 `ErpFinGlBalance` 次年 1 月 yearOpening 行残留 → ①`hasNextYearPeriods` 返回 false（次年期间已删）→ 年末阻断失效；②红冲年度结转凭证（恢复本年未分配利润）；③残留 yearOpening 孤立（keyed by 已删 nextJan.getId()，无所属期间）。评估残留 yearOpening 实际数据一致性影响：次年期间不存在则 yearOpening 无消费方（无即时数据破坏，报表读次年 periodId 取不到行）；次年重建时 populateNextYearOpening 幂等清快照（:156-160）是否覆盖残留[需核验清快照 filter 是否 by periodId=nextJan.getId() — 若 nextJan 重建为新 id 则旧残留不被清，错配风险]。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` yearOpening 存储与填充核验：给出 `ErpFinGlBalance` yearOpening 直接存储证据（orm.xml:924-925 yearOpeningDebit/Credit）+ `AnnualCloseService.populateNextYearOpening:137-184`（创建 gl 行 periodId=nextJan.getId() + 幂等清快照 :156-160 + nextJan 为空静默跳过 :142-146 + 数据源全年 VoucherLine 聚合排除 PROFIT_TO_RETAINED_EARNINGS :293-317）。证实 yearOpening 由年度结账填充，手动删次年期间不清 yearOpening 行（无级联删除——ErpFinGlBalance 与 ErpFinAccountingPeriod 无 DB FK 级联，删期间不影响 GlBalance 行）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 年度结转凭证红冲核验：给出 `ReverseCloseProcessor:46-49`（`isYearEnd → reverseCloseVoucher(ANNUAL-CLOSE- + PROFIT_TO_RETAINED_EARNINGS)`）+ `reverseCloseVoucher:553-562`（按 billCode+businessType 反查 VoucherBillR，存在则 reverse，无则 no-op）证据（file:line）。证实红冲仅恢复本年未分配利润（PROFIT_TO_RETAINED_EARNINGS 凭证红冲），不触及次年 yearOpening 行（yearOpening 在次年 GlBalance，红冲操作本年凭证）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 既有测试覆盖边界普查：grep `testReverseCloseBlockedWhenNextYearExists:77-85`（年末阻断覆盖，仅断言 NopException 未断言 ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS 错误码/ARG_NEXT_YEAR 参数）+ `testReverseCloseReversesAnnualVoucherWhenNoNextYear:89-96`（**名实不符**——方法名承诺"红冲"但仅断言年度凭证生成 count>=1，未调 reverseClose，未禁用次年创建[默认 auto-create]）+ 手动删次年边界场景测试 全集，产出测试覆盖边界清单 + 标注名实不符测试缺口 + 边界场景缺口（零覆盖）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（年末反结账边界是否致数据不一致），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` RC-3/RC-4 边界场景运行时裁决（方法论 §2 判据 + 三源对照）：①若手动删次年期间非常规运维 + 残留 yearOpening 孤立无即时消费方（次年不存在则报表取不到行，无即时数据破坏）→ 登记 **P2 watch-only**（边界场景需手动删次年非常规操作 + 残留无即时消费方，主路径[次年存在阻断/无次年红冲]接受，§2 P2① 次要验收标准边界弱）；②若残留 yearOpening 在次年重建时错配致数据破坏（populateNextYearOpening 幂等清快照不覆盖旧残留）→ 登记 **P1**（触及一致性，归 MR1，§2 P1①）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.6 §5.2 RC-3/RC-4 接受[正常路径]分层一致（边界场景是正常路径之外的残留风险，不撤销正常路径接受）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 年末阻断逻辑 + 边界场景一致性 + yearOpening 存储/填充 + 年度凭证红冲 + 测试覆盖边界证据落盘（全集，无遗漏），每条有证据（file:line）
- [ ] RC-3/RC-4 边界场景运行时裁决有明确结论（P2 watch-only 或 P1 MR1），与 A1.6 §5.2 RC-3/RC-4 接受[正常路径]分层一致

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: planned
Targets: `docs/audits/2026-08-06-1708-rc-ma4-a4-1-21-year-end-reverse-close-block-boundary.md`（定稿）；`docs/audits/arm-index.md`（新 finding 或注记，若有）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 年末阻断边界评估 + 运行时裁决完成

- [ ] `Add` finding/注记更新：若 P2 watch-only → 新建 finding（P2-RC-xxx，年末反结账边界 yearOpening 残留 watch-only，与既有 finding 不同控制点——arm-index period-close 分区无"年末反结账边界 yearOpening 残留"同控制点 finding）；若 P1 → 新建 finding（P1-RC-xxx，归 MR1）；若维持接受无新 finding（残留无即时消费方 + 主路径接受）→ 在 arm-index RC-3/RC-4 相关行追加边界场景注记。禁止未经比对新建重复 finding（grep arm-index 同域同控制点后裁决）。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.6 §2.8/§2.9/§5.2 RC-3/RC-4 / A2.3 period-close E2E 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（年末阻断 + 边界场景一致性 + yearOpening 存储/填充 + 年度凭证红冲 + 测试覆盖边界 + 运行时裁决 + finding 衔接 + §8 自检齐全）
- [ ] 新 finding 或注记已登记入 arm-index（若有变更）或有明确「维持接受无变更」记录并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: accept (mission-driver 独立子代理 ses_029a58ba8ffeiHqwAp9oD3LcXC，新会话不重用执行者上下文) — 全 9 checklist 项 PASS，零信任核对 live code（年末阻断 ReverseCloseProcessor:32-36 + hasNextYearPeriods:128-134 setLimit(1) 查次年任意期间 + isYearEnd:121-125 month==12 / 年度凭证红冲 :46-49 ANNUAL-CLOSE-+PROFIT_TO_RETAINED_EARNINGS + reverseCloseVoucher:553-562 / populateNextYearOpening:137-184 gl.setPeriodId(nextJan.getId()):170 + setYearOpeningDebit/Credit:180-181 / orm.xml:924-925 yearOpeningDebit/Credit / to-one period 关系 :940 无 cascade 属性→删期间不级联删 GlBalance / 测试 testReverseCloseBlockedWhenNextYearExists:77-85 + testReverseCloseReversesAnnualVoucherWhenNoNextYear:89-96 名实不符[仅断言凭证生成未调 reverseClose 未禁用次年创建]确认）零漂移；格式合规；单一结果表面；anti-slack 零命中（"必要时"为合法条件输出非 slack）；item typing 合规（Proof/Decision/Add 无 Fix）；Deps 门控满足（A4.1 expander done + A1.6 done）；保护区域纪律（只读 + 修复归 MR1，反结账逻辑 ask-first / 纯文档级联清理 auto）；逻辑健全且核心机制 CONFIRMED（①删期间无级联→GlBalance yearOpening 残留孤立 SOUND；②populateNextYearOpening 幂等清快照 :156-160 keys by nextJan.getId()→次年重建新 id 不清旧残留→错配风险 SOUND，Phase 1 item 2 已正确框架为"需核验"未预烘；③双分支决策 P2 watch-only[P2①]/P1[P1①] 互斥覆盖 + 开放未预烘，方法学 SOUND）；Closure Gates 删除全仓 typecheck/build（只读）对齐 guide + A4.1.18。无 Blocker/Major。3 non-blocking Minors（M1 测试 @Test 注解行 :76 vs 方法体 :77 — 非真实漂移，方法体引用正确；M2 "必要时" 措辞边界 — Phase 2 item 已显式枚举全分支合法；M3 分支②"错配致数据破坏"标签略戏剧 vs 实际可能"孤立死行+正确新行"数据卫生 — Phase 1 item 2 调查框架已准确）。promote to active。

## Closure Gates

> 本计划为**只读年末反结账阻断边界一致性评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 年末阻断逻辑 + 边界场景一致性 + yearOpening 存储/填充 + 年度凭证红冲 + 测试覆盖边界 + 运行时裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.1.21 验证报告年末阻断 + 边界场景一致性 + yearOpening 存储/填充 + 年度凭证红冲 + 测试覆盖边界 + 运行时裁决齐全 + finding/注记更新（若有）
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.6 §7-4 + §2.8 RC-3 + §2.9 RC-4 + §5.2 RC-3/RC-4 接受 一致
- [ ] 已运行验证：年末阻断 + 边界场景一致性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 年末反结账边界 yearOpening 残留修复（若 A4.1.21 登记 finding 后修复归口）

- Classification: `out-of-scope improvement`（本验证是边界一致性评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是边界一致性评估，结果表面 = 验证报告 + finding/注记登记。修复（若有）归 MR1（R1.0→RC-R1.n），触及 ReverseCloseProcessor/hasNextYearPeriods 逻辑[反结账属会计过账] 须评估 ask-first；纯 yearOpening 残留级联清理或 owner doc 标注可自动执行。本验证闭环不阻塞于修复落地（finding 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（MR1 R1.0 展开器读取本报告 finding[若有] → RC-R1.n 修复，按报告裁决方向：①残留无即时消费方→owner doc 标注边界 + 可选 yearOpening 级联清理；②次年重建错配→hasNextYearPeriods 或 populateNextYearOpening 幂等清快照加固）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- MR1 修复年末反结账边界 yearOpening 残留（若登记 finding）：触及反结账逻辑须评估 ask-first；纯文档/级联清理可自动执行
