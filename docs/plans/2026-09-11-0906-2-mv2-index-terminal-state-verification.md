---
status: active
mission: ai-check-r3
work-item: MV.2
group: "2026-09-11-0906"
verify: [test]
---

# 2026-09-11-0906-2 MV.2 索引终态校验（finding 终态 × 覆盖矩阵 × 跨轮状态机连续）

## Current Baseline

- **MV.1 已 done**（plan `2026-09-11-0457-2`）：五命令门全量实跑（install 156/156 + 全 reactor `mvn test` 4084/0/0/1 零新增失败 + checker R2b=242/R2c=1543≤1543/R12a=71 + CJK report 0/0/0/0 strict PASS + i18n PASS），`ai-check-r3-mv1` 终态行登记 `docs/testing/known-good-baselines.md` 表尾。MV.2 依赖（Deps: MV.1）满足。
- **r3 finding 终态现状（M2.9 收官审计核证，2026-09-11）**：跨轮索引 `docs/audits/check/ai-check-index.md` §Finding 追踪 93 条 `-r3` ID 全量终态——fixed=89 / deferred=4（b2b-020/cs-024/qa-031/qa-033，逐条理由锚 `m2-0-family-adjudication.md` §2.10）/ open=0 / not-a-problem=0（严重级 P0=0/P1=3/P2=17/P3=73）；回填 diff 恰 101 行、per-ID 权威层 byte-identical 零覆写，独立收官审计 `passes closure audit` 零 Blocking。
- **覆盖矩阵现状**：M1.17 收官核账（plan `2026-09-10-0251-1`）落盘 `docs/audits/check/2026-09-06-1645-ai-check-r3/m1-17-coverage-matrix-final.md`——105 基础格（finding 38 / pass 65 / n-a 2[U20 DIM-F/S]）零缺失，140 切片子格，本轮索引 28 报告行 ↔ 跨轮索引 §报告清单 28 行机械对账闭合，独立收官审计 ACCEPT（抽验 8/8）。
- **跨轮索引现状**：§报告清单 + §Finding 追踪由 r1/r2/r3 三 mission 共写；M2.9 已修正 M1.17 登记的 8 行 P1/P2 计数列列序误植（10 个 P2 归位，逐行勘误来源注记）；ID 冲突按 `code-history-deferred-triangulation-audit-prompt.md §3.1` `-r3` 后缀纪律，历史 ID 永不覆写。
- **范围边界（必须显式）**：r1/r2 为并行 mission（roadmap 横切关注点 5「不重复、不吞并」），其 open finding（r1 通道 mfg3-012/pur-005/sal-010/ct-024/drp-019/aps-013 等核注维持 open + F2.13~F2.15/F3.x/V/G todo；r2 M1.x/M2.x todo）**不在本项终态范围**——本项「全部 finding」= ai-check-r3 mission 的 finding 集（93 条 `-r3` ID + M2.0 继承裁决面）；「状态机连续」= 跨轮全索引逐行状态变迁可溯源（fixed 行有证据指针、open 行有归属通道、无 ID 冲突）。
- **残余风险兜底语义**：M2.9 注记「后续新发现 P0 走 M2.1 通道语义由 MV.2 终态校验兜底」——本项机械再推导即该兜底动作的执行载体。
- **本组前置**：同组 N=1 计划（`m29-r1b-r1d-baseline-adjudication`）落地 R1b/R1d 漂移裁决；本项 Phase 2 连续性核验应反映其终态（checker 全表复归证据在其计划内，索引层面无 r3 finding 状态变化）。
- **剩余差距**：MV.2 终态校验注记未落盘（deferred 计数与逐条理由显式报告义务未履行）；MV.3/MG.1/MG.2 全部阻塞于本项。

## Goals

- r3 finding 终态机械再推导：93 条 `-r3` ID 终态分布断言（fixed/not-a-problem/deferred，open=0）+ deferred 4 条逐条理由指针可解析 + fixed 行证据指针抽样核验——M2.1 兜底语义履行（零新发 P0 断言）。
- 覆盖矩阵三方对账 + 跨轮状态机连续核验：`m1-17-coverage-matrix-final.md` 105 格 ↔ 本轮索引 28 报告行 ↔ 跨轮 §报告清单 28 行逐值一致；全索引逐行状态变迁可溯源、无 ID 冲突、`-r3` 后缀纪律、M2.9 8 行勘误在位。
- 终态校验注记落盘本轮索引（`ai-check-r3-index.md`）：deferred 计数与逐条理由显式报告 + 残余风险登记；发现的索引层勘误就最小面修复（逐行来源注记，历史 ID 零覆写）。

## Non-Goals

- 零生产代码/ORM/api.xml/seed/页面改动（roadmap `docs/backlog/ai-check-r3-roadmap.md` 规则 6 审计纪律延续；本项为纯索引/文档核验面）。
- 不关闭 r1/r2 通道 finding（并行 mission 各自所有）；不做五维重新审计；不重开已裁决 finding（仅当某行声称状态与实仓矛盾时按勘误程序处理并登记）。
- 不做 roadmap 状态翻转、不改 M2.9/MV.1 已闭合计划内容；MG.2 的全 roadmap 状态回写不在本项。
- 不修改 checker 脚本与基线（R1b/R1d 裁决归同组 N=1 计划，本项仅消费其结论）。

## Phase 1 — r3 finding 终态机械再推导（Proof）

> 统一类型：Proof（3 项 Proof）。
> Skill: closure-audit-prompt（roadmap MV.2 行指定）
> Targets: 无文件写入（核验证据落本计划勾选注记；勘误留待 Phase 3）
> Prereqs: 同组 N=1 计划落地（checker 全表复归，避免终态断言基线被在途漂移污染）

- [ ] <Proof> 终态分布断言：机械枚举跨轮 §Finding 追踪 `-r3` 93 行 → 断言 fixed=89 / deferred=4 / not-a-problem/open=0 与 M2.9 收官审计记录逐值一致；P0=0/P1=3/P2=17/P3=73 严重级分布复核（M2.1 兜底断言：无未登记新发 P0）。
      - Skill: closure-audit-prompt
- [ ] <Proof> deferred 逐条理由显式报告：4 条 deferred（b2b-020/cs-024/qa-031/qa-033）逐一核对 `m2-0-family-adjudication.md` §2.10 理由段可解析、分类（watch-only/out-of-scope 等）与索引行一致；任一理由缺失或失配登记为勘误候选。
      - Skill: closure-audit-prompt
- [ ] <Proof> fixed 证据指针抽样核验：≥8 条 fixed 行（覆盖 M2.2~M2.8 各批 + 状态继承行如 P2-CK-mfg-010/P2-CK-notify-004）证据指针解析到实仓 plan/测试/报告且与声称状态一致（镜像 M2.9 收官审计抽样强度）；发现幽灵指针即登记勘误候选。
      - Skill: closure-audit-prompt

Exit Criteria:

- [ ] 93 行终态分布断言通过且逐值注记在案（分布 + 严重级 + P0=0 兜底断言）
- [ ] deferred 4 条理由逐条显式可解析；fixed 抽样 ≥8 条零幽灵指针（或勘误候选已登记待 Phase 3 处置）

## Phase 2 — 覆盖矩阵三方对账与跨轮状态机连续（Proof）

> 统一类型：Proof（2 项 Proof）。
> Skill: closure-audit-prompt
> Targets: 无文件写入
> Prereqs: Phase 1 完成（终态分布已锚定）

- [ ] <Proof> 覆盖矩阵三方对账：`m1-17-coverage-matrix-final.md` 105 基础格（finding 38/pass 65/n-a 2，U20 DIM-F/S n-a 带理由）↔ 本轮索引 28 报告行 ↔ 跨轮 §报告清单 28 行（P0/P1/P2/P3 计数列含 M2.9 勘误后归位值）逐值一致；28 报告文件在执行目录实存（`ls` 机械核对零缺零余）。
      - Skill: closure-audit-prompt
- [ ] <Proof> 跨轮状态机连续扫描：全索引逐行核验——fixed/not-a-problem 行均有证据或 plan 指针；open 行均归属 r1/r2 有效通道（含 r3 核注移交面 mfg3-012 族登记在案）；无重复 ID / 无历史覆写痕迹 / `-r3` 后缀纪律一致；r1/r2 行状态与本 roadmap §当前基线「并行 mission」描述不矛盾（本轮不吞并语义保持）。
      - Skill: closure-audit-prompt

Exit Criteria:

- [ ] 三方对账逐值一致注记在案（105 格 + 28↔28 行 + 计数列），任何失配已登记勘误候选
- [ ] 状态机连续扫描完成：fixed 行证据指针齐、open 行通道归属齐、ID 零冲突、`-r3` 纪律零违例

## Phase 3 — 终态校验注记落盘与勘误处置（Add）

> 统一类型：Add（2 项 Add）。
> Skill: closure-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ai-check-r3-index.md`（终态注记）；跨轮索引（仅勘误候选确认成立时）
> Prereqs: Phase 1 + Phase 2 完成

- [ ] <Add> 本轮索引追加 §MV.2 终态校验注记：终态分布（fixed 89/not-a-problem 0/deferred 4 + 逐条理由指针 + open=0）+ P0=0 兜底断言 + 覆盖矩阵三方对账结论 + 状态机连续结论 + 残余风险登记（r1/r2 通道开放面归各自 mission；R1b/R1d 裁决指针引同组 N=1 计划）。
      - Skill: closure-audit-prompt
- [ ] <Add> Phase 1/2 勘误候选处置：逐条复核实仓后确认成立者以最小 diff 修复（逐行附勘误来源注记，对齐 M2.9「8 行列序勘误」范式，历史 ID 语义零覆写）；不成立者逐条登记 not-an-errata 结论。若出现声称状态与实仓矛盾的 finding 行（非笔误级），登记为 MV.2 阻塞注记并按 M2.1 通道语义移交，不在本项内改判。
      - Skill: closure-audit-prompt

Exit Criteria:

- [ ] 本轮索引 §MV.2 终态校验注记落盘（deferred 计数 + 逐条理由 + 残余风险四要素齐备）
- [ ] 勘误候选全部处置（修复或 not-an-errata 逐条登记）；阻塞级发现（如有）已按通道语义移交注记

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-11-0906-2-mv2-index-terminal-state-verification-1-b19251e1 to opencode/glm-5.3-flash
- 2026-09-11：iteration 1，共识 accept #review-2026-09-09-210030-mission-driver-2026-09-11-0906-2-mv2-index-terminal-state-verification-1-b19251e1

## Verification

> 本计划为纯索引/文档核验面（Non-Goals：零生产代码/ORM/api.xml/seed/页面/checker 脚本改动）。按计划指南「无代码更改的计划删除验证命令门控并说明原因」：不适用 `mvn` build/test、compliance checker、CJK/i18n 门控；验证证据由 Phase 1~2 的机械核对勾选注记（逐值分布、对账结果、抽样清单）与 Phase 3 落盘的 §MV.2 终态校验注记承载。收官时 `git status`/diff 复核写入面仅限本计划 + 本轮索引 +（如有）跨轮索引勘误注记。

- [ ] Phase 1~3 全部执行项与 Exit Criteria 勾选 `[x]`，勾选注记含逐值证据（终态分布、严重级分布、deferred 4 条理由、fixed 抽样 ≥8 清单、105 格与 28↔28 行对账结果）
- [ ] 本轮索引 §MV.2 终态校验注记落盘且可解析（deferred 计数 + 逐条理由指针、P0=0 兜底断言、三方对账结论、状态机连续结论 + 残余风险登记）

## Closure

Status Note: <why the plan can close>

Closure Gates:

- [ ] 范围内行为完成（Goals 三项全部落地且注记在案）
- [ ] 相关文档对齐（本轮索引注记落盘；勘误候选逐条处置；历史 ID 零覆写）
- [ ] 已运行验证（按 Verification 节：纯文档核验面，以机械核对注记为验证证据，构建/测试命令门控不适用——理由已注记）
- [ ] 无范围内项目降级为 deferred/follow-up（阻塞级发现按 M2.1 通道语义移交注记，属移交而非降级）
- [ ] 独立草案审查已完成并记录（见 Draft Review Record）
- [ ] 文本一致性已验证：frontmatter status、各 Phase Status、Exit Criteria、Closure Gates 与 `docs/logs/` 条目一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中（Closure Audit Evidence + 勾选注记）

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
