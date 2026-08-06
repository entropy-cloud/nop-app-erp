# 2026-08-06-1044-2 rc-ma4-a4-1-11-bank-recon-counterparty-account-mismatch-rate 银行对账对方账号缺失致错误 MATCHED 触发率评估

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.11（MA4 运行时行为验证 — A1.4 §7-1：UC-FIN-09/14 断言② 对方账号缺失致错误 MATCHED 的实际触发率，关联 P1-RC-004）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.11；存疑点来源 `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` §7 存疑点 1
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1815-1-rc-ma1-a1-4-finance-f4-bank-reconciliation.md`（A1.4 plan done）+ `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md`（A1.4 报告 §7 存疑点 1 + §6 P1-RC-004 finding）、`docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（MA2 银行对账与 AR/AP 解耦既有行为证据输入）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.11 验证报告（落盘 `docs/audits/2026-08-06-1044-rc-ma4-a4-1-11-bank-recon-counterparty-account-mismatch-rate.md`）+ 必要时 arm-index finding/successor 登记。**不改代码/ORM/api.xml/真相源**（只读评估：读 `BankLedgerQuery.findCandidates` 候选过滤逻辑 + 读 `BankStatementMatcher.autoMatch` 匹配决策 + 复用 MA2/A1.4 + 触发率运行时影响面评估）。范式对齐 A4.1.2（已 done 的触发面普查同型工作项）。

- **存疑点原文**（A1.4 报告 §7 存疑点 1，`2026-08-02-1815-...-a1-4-bank-recon.md` §7）：「UC-FIN-09/14 断言② 对方账号缺失致错误 MATCHED 的实际触发率」——L3 静态确认 `BankLedgerQuery.findCandidates` 无对方账号过滤，但「同额同日不同对方账号且账面仅 1 候选」的实际发生率属运行时数据普查——交 MA4 A4.1 按需展开（构造同额同日不同 partner 的 voucher line + bank line，运行 autoMatch 观察是否错误 MATCHED）。

- **关联既有 finding**：
  - **P1-RC-004**（arm-index `:129`）：UC-FIN-09/14 断言② 自动勾对"对方账号模糊匹配"维度缺失——L1+L2 均要求 4 维度（金额 + 反向方向 + valueDate±N天 + **对方账号**），L3 `BankLedgerQuery.findCandidates:39-84` 无对方账号过滤 + ORM 无 counterpartyAccount 列。运行时影响：同额同日不同对方账号且账面仅 1 候选 → 错误 MATCHED（影响对账准确性，可经 manualMatch 取消可逆 + 余额恒等式下游兜底，故非 P0）。修复触及 ORM 结构变更须 ask-first + 独立 plan-audit。**状态：todo（MR1 RC-R1.n 展开待修复）。**
  - 本验证**不重复登记** P1-RC-004（已登记），只评估其实际触发率 + 运行时影响面，确认/调整 P1-RC-004 分级（P1 维持 vs 升 P0 vs 降 P2）。

- **关联既有结论**：
  - A1.4 §5：UC-FIN-09/14 断言② = **P1**（P1-RC-004），对方账号匹配维度缺失。
  - MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`：银行对账为独立子系统，与 AR/AP 核销解耦。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:172,279` UC-FIN-09/14 断言② 逐字「自动勾对: 金额 + 反向方向 + valueDate±N天 + **对方账号** 模糊匹配」——对方账号是 4 维度之一。L2（`bank-reconciliation.md §业务规则 2:98`）一致要求。L3 静默缺失。

- **实现现状（L3，实测锚点，本计划起草时核实）**：
  - 候选过滤逻辑：`BankLedgerQuery#findCandidates:39-84`——过滤条件 = `subjectId:64` + `dcDirection(oppositeDirection):65` + `voucherId(日期窗口 in):66` + `debitAmount 或 creditAmount:67-71`（:63-72），**无 counterpartyAccount 过滤**。
  - 已勾对排除：`findOccupiedLineIds:105-123`——排除已被其他银行流水行 MATCHED/MANUAL_MATCHED 占用的凭证行（:104-122），减少重复匹配但不解决跨 partner 同额问题。
  - 匹配决策：`BankStatementMatcher.autoMatch`（需执行时读取决策逻辑——单候选时是否自动 MATCHED，多候选时是否拒绝/留待手工）。
  - 可逆性：错误 MATCHED 后可经 manualMatch 取消（A1.4 §5 P1-RC-004 已确认可逆）。

- **既有证据（复用输入）**：
  - MA2 A2.5c：银行对账与 AR/AP 解耦行为已证实。本验证复用其「对账子系统独立性」结论，**只补「对方账号缺失致错误 MATCHED 的实际触发率」差异**。
  - A1.4 §6 P1-RC-004：已静态确认匹配算法缺对方账号维度。

- **初步实测（本计划起草时的部分核验，执行时复核）**：
  - grep `BankLedgerQuery.java` `counterparty|oppositeAccount|partner`——零命中（候选过滤确无对方账号维度）。
  - grep `BankStatementMatcher.java` `autoMatch|singleCandidate|multipleCandidate`——匹配决策逻辑待执行时读取（单候选自动 MATCHED vs 多候选拒绝）。
  - 即本验证核心 = 评估「同额同日不同对方账号且账面仅 1 候选」场景的实际触发率 + 匹配决策行为 + 可逆性兜底，确认 P1-RC-004 分级（P1 维持最可能：错误 MATCHED 可逆 + 余额恒等式下游兜底，非活跃数据破坏故非 P0）。

- **剩余差距**：P1-RC-004 的实际触发率未运行时评估——「单候选自动 MATCHED」决策 + 「同额同日跨 partner」实际数据分布 + 可逆性兜底有效性。本验证补全该运行时影响面评估。

- **保护区域**：只读评估（读匹配算法 + 读候选过滤 + 引用 MA2/A1.4 + 触发率影响面推理），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**（P1-RC-004 修复触及 ORM 结构变更须 ask-first，归 MR1）。

## Goals

- 触发率评估：核验 `BankLedgerQuery.findCandidates:39-84` 候选过滤逻辑（确无对方账号维度）+ `BankStatementMatcher.autoMatch` 匹配决策（单候选自动 MATCHED vs 多候选拒绝），评估「同额同日不同对方账号且账面仅 1 候选」场景的实际触发率与运行时影响面。
- 可逆性兜底核验：错误 MATCHED 后经 manualMatch 取消的可逆性 + 余额恒等式下游兜底（A1.4 §5 P1-RC-004 已确认可逆）的运行时有效性。
- 对齐 UC-FIN-09/14 断言② + `bank-reconciliation.md §业务规则 2` 给出结论：确认/调整 P1-RC-004 分级——①若错误 MATCHED 实际触发率低 + 可逆 + 下游兜底有效 → P1 维持（对方账号维度缺失仍为合规缺陷但非 P0）；②若实际触发率高且不可逆/无兜底 → 升 P0（活跃数据破坏，触发 MR0 即时修复）；③若触发率极低且仅影响边缘场景 → 维持 P1 或降 P2（须列明降级依据）。
- 产出验证报告 + §8 过程纪律自检；finding/successor（若有）按 §7 裁决登记 arm-index（P1-RC-004 已登记，本验证只更新分级注记或确认维持）。

## Non-Goals

- **不修复 P1-RC-004**（对方账号匹配维度缺失——修复触及 ORM 结构变更[ErpFinBankStatementLine + ErpFinVoucherLine 增 counterpartyAccount 列] + 匹配算法变更，须 ask-first + 独立 plan-audit，归 MR1）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 UC-FIN-09/14 全部验收标准**（A1.4 §5 已判 P1；本验证只评对方账号缺失触发率差异）。
- **不展开 A1.4 §7-2/§7-3/§7-4**（A4.1.12/A4.1.13/A4.1.14 范围）。

## Task Route

- Type: `verification or audit work`（触发率评估 + P1-RC-004 分级确认/调整）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.11 行）+ `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` §7 存疑点 1 + §6 P1-RC-004（输入）+ `docs/design/finance/bank-reconciliation.md §业务规则 2`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。触发率评估需多维度归类（候选过滤逻辑 / 匹配决策 / 触发率影响面 / 可逆性兜底 / P1 维持-or-升 P0-or-降 P2 裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读匹配算法 + 读候选过滤 + 引用 MA2/A1.4 + 触发率影响面推理）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 对方账号缺失致错误 MATCHED 触发率与影响面评估

Status: planned
Targets: `docs/audits/2026-08-06-1044-rc-ma4-a4-1-11-bank-recon-counterparty-account-mismatch-rate.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.11 行）；A1.4 done（§7 存疑点 1 已落盘 + §6 P1-RC-004 已登记）

- [ ] `Proof` 候选过滤逻辑核验：给出 `BankLedgerQuery.findCandidates:39-84` 过滤条件（subjectId + dcDirection + amount + voucherId 日期窗口，确无对方账号维度）+ `findOccupiedLineIds:105-123` 已勾对排除逻辑（减少重复但不解决跨 partner 同额）证据（file:line）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 匹配决策核验：读取 `BankStatementMatcher.autoMatch` 决策逻辑——单候选时是否自动 MATCHED / 多候选时是否拒绝留待手工，评估「同额同日不同 partner 且账面仅 1 候选」场景的匹配决策行为（是否产生错误 MATCHED）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 可逆性兜底核验：错误 MATCHED 后经 manualMatch 取消的可逆路径 + 余额恒等式下游兜底（`bank-reconciliation.md` 恒等式守卫）的运行时有效性。引用 A1.4 §5 P1-RC-004 已确认的「可经 manualMatch 取消可逆 + 余额恒等式下游兜底」结论。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` P1-RC-004 分级确认/调整（方法论 §2 判据 + 三源对照）：①若错误 MATCHED 实际触发率低 + 可逆 + 下游兜底有效 → P1 维持（对方账号维度缺失仍为合规缺陷但非 P0，A1.4 §5 维持）；②若实际触发率高且不可逆/无兜底 → 升 P0（活跃数据破坏，触发 MR0 即时修复）；③若触发率极低且仅影响边缘场景 → 维持 P1 或降 P2。裁决须列明 §2 判据编号 + 与 A1.4 §5 P1-RC-004 P1 结论分层一致 + 与 arm-index `:129` P1-RC-004 行衔接（更新分级注记或确认维持）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 候选过滤逻辑 + 匹配决策 + 可逆性兜底证据落盘，每条有证据（file:line）
- [ ] P1-RC-004 分级确认/调整有明确结论（P1 维持 / 升 P0 / 降 P2），与 A1.4 §5 P1-RC-004 P1 结论分层一致

### Phase 2 - finding/successor 衔接 + §8 自检 + 报告定稿

Status: planned
Targets: `docs/audits/2026-08-06-1044-rc-ma4-a4-1-11-bank-recon-counterparty-account-mismatch-rate.md`（定稿）；`docs/audits/arm-index.md`（P1-RC-004 分级注记更新）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 触发率评估 + 分级确认完成

- [ ] `Add` P1-RC-004 分级注记更新：若 P1 维持 → 在 arm-index `:129` P1-RC-004 行追加「A4.1.11 运行时触发率评估确认 P1 维持」注记（含触发率结论 + 可逆性兜底证据 + file:line）；若升 P0 → 在 P1-RC-004 行标注升级 + 触发 MR0 即时通道（须 ask-first ORM 结构变更）；若降 P2 → 更新分级 + 列明降级依据。禁止未经比对新建重复 finding。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.4 §6 P1-RC-004 / MA2 A2.5c 银行对账解耦 / P2-RC-001 dedup scope 的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（过滤逻辑 + 匹配决策 + 触发率 + 可逆兜底 + 分级确认 + finding 衔接 + §8 自检齐全）
- [ ] P1-RC-004 分级注记已更新入 arm-index（确认维持/升 P0/降 P2）并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (2026-08-06 draft review) because 全部必需节齐全（Current Baseline / Goals / Non-Goals / Task Route / Infrastructure / Execution Plan[2 phases] / Draft Review Record / Closure Gates / Deferred But Adjudicated / Closure），Phase 结构合法（Status/Targets/Skill/Item Types/Prereqs/items/Exit Criteria），item 类型合法（Phase 1 `Proof|Decision` 4 项无单一类型 ≥80% 故逐项标注；Phase 2 `Add|Proof`），技能逐项记录，Exit Criteria 清晰可测（证据 file:line + P1-RC-004 分级结论），范围无 "and also..." 蔓延（Non-Goals 显式排除 P1-RC-004 修复 / 全 UC 重核 / §7-2~4 展开），只读计划已按模板删除 build/test 门控并说明理由，Deferred But Adjudicated 覆盖 P1-RC-004 带 successor。基线锚点经实测核验：`BankLedgerQuery.findCandidates:39-84`（过滤 subjectId+dcDirection+voucherId 日期窗口+amount :63-72，确无 counterpartyAccount）、`findOccupiedLineIds:105-123`、`BankStatementMatcher.autoMatch:41`（单候选→MATCHED :63 / 多候选→SUSPENSE-UNMATCHED，javadoc :23-28 自陈决策，与触发率评估前提一致）、arm-index `:129` P1-RC-004、A1.4 报告 §7/§6 均存在。修复一处 Blocker：`> Related:` 行 A1.4 plan 路径断裂（`-bank-recon.md`→实际 `-bank-reconciliation.md`），已更正并把 §7/§6 归属指向 A1.4 审计报告（该报告已在 `> Source:` 正确引用）。无 anti-slack 违规（条件分支为决策结果三分支全覆盖，非模糊可选）。可进入执行。

## Closure Gates

> 本计划为**只读触发率评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 候选过滤 + 匹配决策 + 触发率影响面 + 可逆兜底 + 分级确认 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.1.11 验证报告过滤逻辑 + 匹配决策 + 触发率 + 分级确认齐全 + P1-RC-004 分级注记更新入 arm-index
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据一致；与 A1.4 §7-1 + §6 P1-RC-004 + §5 P1 结论一致
- [ ] 已运行验证：过滤逻辑 + 匹配决策 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-004 对方账号匹配维度缺失修复

- Classification: `optimization candidate`（已登记 P1-RC-004，修复归 MR1）
- Why Not Blocking Closure: 本计划是触发率评估，结果表面 = 验证报告 + P1-RC-004 分级确认。P1-RC-004 已登记为 P1，修复（ORM 增 counterpartyAccount 列 + 匹配算法增对方账号过滤）触及 ORM 结构变更须 ask-first + 独立 plan-audit，归 MR1（R1.0→RC-R1.n）。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 P1-RC-004 分级确认展开修复；若升 P0 则触发 MR0 即时通道）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
