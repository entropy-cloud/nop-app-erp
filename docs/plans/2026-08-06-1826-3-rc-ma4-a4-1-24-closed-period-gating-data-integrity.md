# 2026-08-06-1826-3 rc-ma4-a4-1-24-closed-period-gating-data-integrity 报表 CLOSED 期间门控缺失的运行时数据完整性影响确认

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.24（MA4 运行时行为验证 — A1.7 §7 存疑点 SP-3：UC-FIN-16 `loadGlBalances` 不校验 `period.status==CLOSED`，OPEN 期间可渲染三大报表，运行时数据完整性影响确认 — 关联 P2-RC-008）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.24；存疑点来源 `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-3
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done）、`docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`（A1.7 报告 §2.2 CLOSED 门控 + §5.2 P2-RC-008 + §7 SP-3）、`docs/audits/arm-index.md`（P2-RC-008 行）、`docs/design/finance/use-cases.md:318,339`（UC-FIN-16 L1）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.24 验证报告（落盘 `docs/audits/2026-08-06-1826-rc-ma4-a4-1-24-closed-period-gating-data-integrity.md`）+ 必要时 arm-index P2-RC-008 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：读 `loadGlBalances:386-413` 期间过滤逻辑 + GlBalance 与凭证过账时序关系 + OPEN/CLOSING 期间数据不完整性推理 + 既有测试普查 + P2-RC-008 已登记 finding 复核）。范式对齐 A4.1.21（done — period-close 运行时行为评估同型工作项）+ A4.1.11（done — PC-3 reminder 运行时行为评估同型工作项）。

- **存疑点原文**（A1.7 报告 §7 SP-3，`2026-08-02-2115-...-a1-7-...md` §7）：「CLOSED 期间门控缺失的运行时数据完整性影响 — OPEN 期间渲染报表是否实际产生误导（部分凭证未过账/未结转）」。静态状态：`loadGlBalances` 按 periodId 取数，OPEN 期间数据可能不完整（未过账凭证不入 GlBalance）。MA4 运行时确认方式：OPEN 期间 + 未过账凭证场景跑 BS，对比 CLOSED 后 BS 差异。

- **关联既有 finding**：
  - **P2-RC-008**（A1.7 §2.2 + §5.2 + arm-index `:141`，状态 todo successor watch-only）：UC-FIN-16 CLOSED 期间门控未强制。L1（`use-cases.md:339`）逐字「报表基于已 CLOSED 期间的 GlBalance(未结账期间数据不完整)」。L3 `loadGlBalances:386-413` 仅按 periodId 过滤（`:396/402`），**不校验 period.status==CLOSED**——OPEN/CLOSING 期间亦可渲染。L4 `TestErpFinReportRendering` 全部测试 seed `PERIOD_STATUS_OPEN`（`:266`）即渲染，无 CLOSED 门控测试。主路径（数据存在即渲染）OK，边界（OPEN 期间数据不完整）弱。属数据完整性关注非会计正确性破坏。**与 P1-MA2-021 CLOSED_FINAL 凭证锁定不同控制点**（P1-MA2-021=过账侧，本 finding=报表渲染侧）。本验证闭合 P2-RC-008 的运行时数据完整性影响裁决（维持 P2 或升级）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:318` UC-FIN-16 财务三大报表；`:339` 验收标准逐字「报表基于已 CLOSED 期间的 GlBalance(未结账期间数据不完整)」。L1 显式要求报表基于 CLOSED 期间，未结账期间数据不完整。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实，全在 module-finance/erp-fin-service）**：
  - GlBalance 加载（`ErpFinReportBizModel.java:386-413`）：periodId 缺省时取最近期间（`:391 findLatestPeriodId`）；非缺省时 `q.addFilter(eq("periodId", periodId));:402` + `applyOrgAndSchemaScope(q, periodId);:403`。**全程不校验 period.status**——OPEN/CLOSING/CLOSED 期间均按 periodId 取 GlBalance 行。
  - GlBalance 数据源（过账引擎写入）：GlBalance 由过账引擎在凭证 POSTED 时维护（`ErpFinPostingProcessor` → GlBalance 写入）；**未过账凭证不入 GlBalance**（凭证 docStatus=DRAFT/SAVED 不触发 GlBalance 更新）。故 OPEN 期间若有未过账凭证 → GlBalance 缺该部分余额 → 报表数据不完整。
  - BS/IS 数据源（`buildBalanceSheetDataset:269` / `buildIncomeStatementDataset:284`）：均经 `loadGlBalances(periodId)` → GlBalance；cash flow（`buildCashFlowDataset:299`）经 `loadPostedVoucherLines(periodId)`（已过账凭证行，仍不校验期间状态）。
  - 期间状态模型：`ErpFinAccountingPeriod.status`（OPEN/CLOSING/CLOSED/CLOSED_FINAL）；结账流程 `ClosePeriodProcessor` 推进 status。报表渲染不读 period.status。

- **既有证据（复用输入）**：
  - A1.7 §2.2（`:120`）：CLOSED 期间门控 FAIL(soft)——`loadGlBalances:386-413` 仅按 periodId 过滤，不校验 period.status==CLOSED；OPEN 期间亦可渲染；L1「报表基于已 CLOSED 期间」未强制。
  - A1.7 §5.2（P2-RC-008）：CLOSED 门控 P2——命中 §2 P2①（次要验收标准未完全满足，主路径[数据存在即渲染]OK，边界[OPEN 期间数据不完整]弱——数据完整性关注非会计正确性破坏）。
  - arm-index P2-RC-008（`:141`）：与 P1-MA2-021 不同控制点（过账侧 vs 报表渲染侧）；修复 = buildXxxDataset 入口加 period.status==CLOSED 守卫[BizModel 代码逻辑预授权] 或 owner doc 标注 OPEN 期间警告[纯文档预授权]。

- **剩余差距**：CLOSED 门控缺失的**运行时实际数据完整性影响**未确认 —— ①OPEN 期间渲染报表实际是否产生误导（未过账凭证不入 GlBalance 致余额缺失的具体偏差量级）；②CLOSING 期间（结账进行中）渲染的过渡态数据完整性；③既有测试 `TestErpFinReportRendering` 全程 seed OPEN 期间（`:266`）即渲染的实际覆盖语义（测试隐含 OPEN 可渲染 = 行为预期，但 L1 要求 CLOSED）；④维持 P2-RC-008 还是升级 P1（若 OPEN 期间渲染致严重误导决策）。本验证闭合 P2-RC-008 的运行时数据完整性影响裁决。

- **保护区域**：只读评估（读 loadGlBalances 期间过滤 + GlBalance 与凭证过账时序 + OPEN/CLOSING 数据不完整性推理 + 既有测试普查 + P2-RC-008 复核），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若发现升级 P1，登记归 MR1；修复 = buildXxxDataset 入口加 period.status 守卫属 BizModel 代码逻辑[预授权自动执行]，不触 §5 ask-first — 非会计过账核心路径，仅报表读侧门控；owner doc 标注 OPEN 期间警告纯文档预授权）。

## Goals

- `loadGlBalances` 期间过滤逻辑核验：给出 `ErpFinReportBizModel.java:386-413`（periodId 缺省取最近 `:391`；非缺省 `eq("periodId", periodId):402` + `applyOrgAndSchemaScope:403`，**不校验 period.status**）+ BS/IS/cash flow 数据源链（`:269/:284/:299`）证据（file:line）。证实 OPEN/CLOSING/CLOSED 期间均按 periodId 取数，无状态门控。
- GlBalance 与凭证过账时序核验：核验 GlBalance 由过账引擎在凭证 POSTED 时维护（未过账凭证不入 GlBalance）—— 确认 OPEN 期间若有未过账凭证 → GlBalance 缺该部分余额 → 报表数据不完整的机制成立。
- OPEN/CLOSING 期间数据完整性影响评估（本存疑点核心）：评估 OPEN 期间渲染报表的实际误导面 —— ①未过账凭证不入 GlBalance 致余额缺失的具体偏差（BS 资产==负债+权益恒等式是否仍成立[GlBalance 内部平衡] vs 余额绝对值偏低）；②CLOSING 期间（结账进行中）过渡态数据完整性；③实操中 OPEN 期间渲染报表的业务场景频率（月中查询未结账期间属常规运维 vs 误导决策）。评估是数据完整性偏差（余额偏低但恒等式成立）还是会计正确性破坏（恒等式破坏）。
- 既有测试覆盖语义核验：核验 `TestErpFinReportRendering` 全程 seed `PERIOD_STATUS_OPEN`（`:266`）即渲染的实际语义 —— 测试隐含 OPEN 可渲染 = 当前行为预期，但与 L1「报表基于已 CLOSED 期间」不一致；标注 CLOSED 门控测试缺口（零覆盖）。
- 对齐 L1 `:339` + P2-RC-008 + §2 判据给出运行时裁决：①若 OPEN 期间渲染致余额偏低但恒等式仍成立（数据完整性偏差非会计正确性破坏）+ 实操属常规月中查询非误导决策主路径 → 维持 P2-RC-008 = P2 watch-only（主路径[CLOSED 期间渲染]OK，边界[OPEN 期间数据偏低]弱，§2 P2①）；②若 OPEN 期间渲染致恒等式破坏或严重误导决策主路径 → 升级 P2-RC-008 = P1（归 MR1，§2 P1①）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 P1-MA2-021[过账侧 CLOSED_FINAL 锁定]分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复 CLOSED 期间门控**（若维持 P2 或升级 P1，修复归 MR1；修复 = buildXxxDataset 入口加 period.status==CLOSED 守卫[BizModel 代码逻辑预授权] 或 owner doc 标注 OPEN 期间警告[纯文档预授权]，不触 §5 ask-first）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 UC-FIN-16 全部验收标准**（A1.7 §5 已判 ⑧ P1[P1-RC-007] + ⑨ caveat ③ 收口 + CLOSED 门控 P2[P2-RC-008]；本验证只评 P2-RC-008 的运行时数据完整性影响维度）。
- **不重审 P1-MA2-021**（CLOSED_FINAL 凭证锁定 = 过账侧，resolved；本验证只评报表渲染侧门控，不同控制点）。
- **不展开 A1.7 §7 SP-1/SP-2/SP-4**（cash flow postingType / 多账套渲染 / 看板行级权限，独立工作项 A4.1.22/A4.1.23/A4.1.25）。
- **不实际执行 OPEN 期间注入未过账凭证重现**（只读 loadGlBalances 期间过滤 + GlBalance 过账时序 + 数据不完整性推理 + 既有测试 + P2-RC-008 复核；真实 OPEN 期间偏差重现属 MR1 修复验证范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（CLOSED 期间门控缺失的运行时数据完整性影响确认 + P2-RC-008 运行时裁决维持-or-升级）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.24 行）+ `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-3 + §2.2 CLOSED 门控 + §5.2 P2-RC-008（输入）+ `docs/audits/arm-index.md` P2-RC-008 行（`:141`）+ `docs/design/finance/use-cases.md:318,339`（UC-FIN-16 L1）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。CLOSED 门控数据完整性评估需多维度归类（loadGlBalances 期间过滤 / GlBalance 与凭证过账时序 / OPEN-CLOSING 数据不完整性[余额偏低 vs 恒等式破坏] / 既有测试覆盖语义[P2-RC-008 隐含行为] / P2-RC-008 维持-or-升级裁决 / P1-MA2-021 过账侧分层 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读 loadGlBalances 期间过滤 + GlBalance 过账时序 + OPEN/CLOSING 数据不完整性推理 + 既有测试普查 + P2-RC-008 复核）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - CLOSED 期间门控数据完整性影响评估

Status: planned
Targets: `docs/audits/2026-08-06-1826-rc-ma4-a4-1-24-closed-period-gating-data-integrity.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.24 行）；A1.7 done（§7 SP-3 已落盘 + §2.2 CLOSED 门控 + §5.2 P2-RC-008）

- [ ] `Proof` `loadGlBalances` 期间过滤逻辑核验：给出 `ErpFinReportBizModel.java:386-413`（periodId 缺省取最近 `:391`；非缺省 `eq("periodId", periodId):402` + `applyOrgAndSchemaScope:403`，**不校验 period.status**）+ BS/IS/cash flow 数据源链（`buildBalanceSheetDataset:269` → loadGlBalances:272 / `buildIncomeStatementDataset:284` → loadGlBalances:287 / `buildCashFlowDataset:299` → loadPostedVoucherLines:302）证据（file:line）。证实 OPEN/CLOSING/CLOSED 期间均按 periodId 取数，无状态门控。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` GlBalance 与凭证过账时序核验：核验 GlBalance 由过账引擎在凭证 POSTED 时维护（`ErpFinPostingProcessor` → GlBalance 写入），未过账凭证（docStatus=DRAFT/SAVED）不入 GlBalance。确认 OPEN 期间若有未过账凭证 → GlBalance 缺该部分余额 → 报表数据不完整的机制成立。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` OPEN/CLOSING 期间数据完整性影响评估（本存疑点核心）：评估 OPEN 期间渲染报表的实际误导面 —— ①未过账凭证不入 GlBalance 致余额缺失的具体偏差（BS 资产==负债+权益恒等式是否仍成立[GlBalance 内部平衡——已过账凭证借贷必平] vs 余额绝对值偏低[未过账部分缺失]）；②CLOSING 期间（结账进行中）过渡态数据完整性；③实操中 OPEN 期间渲染报表的业务场景频率（月中查询未结账期间属常规运维 vs 误导决策）。评估是数据完整性偏差（余额偏低但恒等式成立）还是会计正确性破坏（恒等式破坏）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 既有测试覆盖语义核验：核验 `TestErpFinReportRendering` 全程 seed `PERIOD_STATUS_OPEN`（`:266 p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN)`）即渲染的实际语义 —— 测试隐含 OPEN 可渲染 = 当前行为预期，但与 L1「报表基于已 CLOSED 期间」不一致；标注 CLOSED 门控测试缺口（零覆盖）。产出测试覆盖边界清单。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（CLOSED 门控缺失是否致数据完整性破坏/误导），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` P2-RC-008 运行时裁决（方法论 §2 判据 + 三源对照）：①若 OPEN 期间渲染致余额偏低但恒等式仍成立（数据完整性偏差非会计正确性破坏）+ 实操属常规月中查询非误导决策主路径 → **维持 P2-RC-008 = P2 watch-only**（主路径[CLOSED 期间渲染]OK，边界[OPEN 期间数据偏低]弱，§2 P2①）；②若 OPEN 期间渲染致恒等式破坏或严重误导决策主路径 → **升级 P2-RC-008 = P1**（归 MR1，§2 P1①）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 P1-MA2-021[过账侧 CLOSED_FINAL 锁定]分层一致（本 finding = 报表渲染侧门控，非过账侧凭证锁定）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] loadGlBalances 期间过滤 + GlBalance 过账时序 + OPEN/CLOSING 数据完整性影响 + 测试覆盖语义证据落盘（全集，无遗漏），每条有证据（file:line）
- [ ] P2-RC-008 运行时裁决有明确结论（维持 P2 watch-only 或升级 P1），与 P1-MA2-021[过账侧]分层一致

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: planned
Targets: `docs/audits/2026-08-06-1826-rc-ma4-a4-1-24-closed-period-gating-data-integrity.md`（定稿）；`docs/audits/arm-index.md`（P2-RC-008 注记更新，若有升级/维持记录）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 CLOSED 门控数据完整性评估 + 运行时裁决完成

- [ ] `Add` finding/注记更新：若维持 P2-RC-008 = P2 → 在 arm-index P2-RC-008 行（`:141`）追加「A4.1.24 运行时确认：OPEN 期间渲染余额偏低但恒等式成立，数据完整性偏差非会计正确性破坏，维持 P2 watch-only」注记；若升级 P1 → 更新 P2-RC-008 分级为 P1 + 归 MR1（按 §7 复用规则 grep 确认与 P1-MA2-021 不同控制点后裁决，不新建重复 finding）。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.7 §2.2/§5.2 P2-RC-008 / P1-MA2-021 过账侧 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（loadGlBalances 期间过滤 + GlBalance 过账时序 + OPEN/CLOSING 数据完整性 + 测试覆盖语义 + 运行时裁决 + finding 衔接 + §8 自检齐全）
- [ ] P2-RC-008 注记已更新入 arm-index（维持/升级记录）并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_0295f91d1ffeQ2Vcfgv35Yq6Lr，新会话不重用执行者上下文）— 全 12 checklist 项 PASS，零信任核对 live code（loadGlBalances:386-413 无 period.status==CLOSED 校验[仅 periodId + applyOrgAndSchemaScope + sort] / buildBalanceSheetDataset:269→loadGlBalances:272 / buildIncomeStatementDataset:284→:287 / buildCashFlowDataset:299→loadPostedVoucherLines:302 / use-cases.md:339 CLOSED 逐字 / TestErpFinReportRendering:266 seed PERIOD_STATUS_OPEN / arm-index:141 P2-RC-008 + P1-MA2-021:483 过账侧 distinct control point 确认）零漂移；单一结果表面；anti-slack 零命中；item typing 合规（Proof/Decision/Add 无 Fix）；Deps 门控满足；保护区域纪律（只读 + 修复归 MR1 BizModel 读侧门卫/文档预授权非 ask-first）；方法学对齐（§2 P2①/P1① + §4 Q1 + §7 dedup + §8）；恒等式推理 SOUND（已过账凭证借贷必平→ΣDr==ΣCr 保持→余额偏低但 GL 内部平衡成立；Decision 分支②「恒等式破坏」对 BS 展示层 ΔNetIncome[P&L 结转凭证未过账致 RE 未更新]开放未预烘）；正确复用既有 P2-RC-008 非新建重复。1 non-blocking soft note（执行时关注 BS 展示层 A==L+E 在 P&L 结转凭证未过账场景的 ΔNetIncome 偏差——Decision 分支②已容纳）。promote to active。

## Closure Gates

> 本计划为**只读 CLOSED 期间门控数据完整性影响评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = loadGlBalances 期间过滤 + GlBalance 过账时序 + OPEN/CLOSING 数据完整性 + 测试覆盖语义 + 运行时裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.1.24 验证报告 loadGlBalances 期间过滤 + GlBalance 过账时序 + OPEN/CLOSING 数据完整性 + 测试覆盖语义 + 运行时裁决齐全 + P2-RC-008 注记更新
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.7 §7 SP-3 + §2.2 + §5.2 P2-RC-008 一致
- [ ] 已运行验证：loadGlBalances 期间过滤 + GlBalance 过账时序 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### CLOSED 期间门控修复（P2-RC-008 successor，归 MR1）

- Classification: `out-of-scope improvement`（本验证是数据完整性影响评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是数据完整性影响评估，结果表面 = 验证报告 + P2-RC-008 注记更新。修复（P2-RC-008 todo successor）归 MR1（R1.0→RC-R1.n），修复 = buildXxxDataset 入口加 period.status==CLOSED 守卫[BizModel 代码逻辑预授权自动执行] 或 owner doc 标注 OPEN 期间警告[纯文档预授权自动执行]，**不触 §5 ask-first**（非会计过账核心路径，仅报表读侧门控）。本验证闭环不阻塞于修复落地（finding 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（MR1 R1.0 展开器读取 P2-RC-008[若升级 P1 则强制实现，若维持 P2 则 P2 登记不强制] → RC-R1.n 修复，按报告裁决方向：①维持 P2→owner doc 标注 OPEN 期间数据偏低警告 + 可选 buildXxxDataset CLOSED 守卫；②升级 P1→buildXxxDataset 入口强制 period.status==CLOSED 守卫）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- MR1 修复 CLOSED 期间门控（P2-RC-008 successor）：BizModel 代码逻辑/纯文档预授权自动执行，不触 ask-first
