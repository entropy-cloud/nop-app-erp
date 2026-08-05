# 2026-08-07-0330-2 rc-ma4-a4-1-2-fx-rate-missing-trigger-surface UC-FIN-12 汇率缺失触发面运行时普查

> Plan Status: active
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.1.2（MA4 运行时行为验证 — A1.1 §7-2：UC-FIN-12 汇率缺失触发面实测，各域 Provider 外币场景是否显式传 rate 普查）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.2；存疑点来源 `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` §7 存疑点 2
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行 / deferred successor）、`docs/plans/2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，P1-RC-002 汇率缺失守卫未实现）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.2 验证报告（落盘 `docs/audits/2026-08-07-0330-rc-ma4-a4-1-2-fx-rate-missing-trigger-surface.md`）+ 必要时 arm-index finding 登记。**不改代码/ORM/api.xml/真相源**（只读普查：grep 各域 Provider 的 PostingEvent 构造/setExchangeRate 调用点 + 读既有 JUnit + 复用 MA2）。

- **存疑点原文**（A1.1 报告 §7 存疑点 2，`2026-08-02-1645-...-a1-1-posting.md:294`）：「UC-FIN-12 汇率缺失触发面实测」——L3 静态确认回退逻辑（`ErpFinPostingProcessor.prepareContext:537`：`event.getExchangeRate() != null ? event.getExchangeRate() : EXCHANGE_RATE_DEFAULT`，`EXCHANGE_RATE_DEFAULT=1` :78），但「当前各域 Provider 是否在所有外币场景显式传 rate」属**运行时调用面普查**——交 MA4 A4.1 展开（grep 各域 `PostingEvent.setExchangeRate` 调用点）。

- **关联既有 finding**（P1-RC-002，A1.1 报告 §5.2）：UC-FIN-12 断言 2「汇率缺失→报错拒绝过账」守卫**未实现**（实现为静默回退 rate=1）。A1.1 已评估**维持 P1 不升 P0**，理由之一 =「当前各域 Provider 显式传 rate（`NotesReceivableAcctDocProvider` 等 per baseline），无活跃错误数据」。本验证即**运行时核实该 P1→P0 升级评估的关键前提**：若普查发现存在域 Provider 在外币场景**漏传 rate**，则 P1-RC-002 触发面含默认活跃路径 → 升 P0 → 触发 MR0 即时通道。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:223` UC-FIN-12 断言 1「凭证行.本位币金额 == 源币金额 × 汇率」+ 断言 2「若 汇率缺失 → 报错拒绝过账」。

- **实现现状（L3，实测锚点）**：
  - 回退点：`ErpFinPostingProcessor.java` `prepareContext:537` + `persistVoucher:817-820`（exchangeRate 回退 `EXCHANGE_RATE_DEFAULT`）；行级双金额 `persistVoucher:826-828`（amtSource/amtFunctional 回退到 amt，无 FX 折算）。
  - 调用面（本验证对象）：各域 `IErpFinAcctDocProvider` 实现 + 各域构造 `PostingEvent`/过账事件的调用方——是否在 `currencyId != functionalCurrency`（外币）场景显式 `setExchangeRate(rate)`。**确切调用点清单以实仓 grep 为准**（Phase 1 全集枚举，禁止抽样）。

- **既有证据（复用输入）**：
  - MA2 `2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md` §5.12「line 级无 FX 折算，状态机不因币种失败」→ P1-MA2-002/009（FX 折算缺失）。
  - A1.1 §5.2 P1-RC-002 已做 P0 升级评估（维持 P1，前提 = 各域显式传 rate）。本验证核实该前提。

- **剩余差距**：各域 Provider 外币场景 setExchangeRate 调用面**未全集普查**（A1.1 baseline 仅举 `NotesReceivableAcctDocProvider` 为例，未逐域/逐外币场景核验）。漏传 rate 的域 = P1-RC-002 实际触发面。

- **保护区域**：只读普查（grep + 读 JUnit），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——若升 P0 则触发 MR0（MR0 P0 行触及会计过账逻辑须 ask-first + 独立 plan-audit，§5 保护区域暂停协议）。

## Goals

- 全集枚举各域 `IErpFinAcctDocProvider` 实现及其 PostingEvent 构造/调用方，逐调用点核验：在 `currencyId != functionalCurrency`（外币）场景是否显式 `setExchangeRate(rate)`。
- 对每调用点给出运行时证据：文件:行 + 是否传 rate（显式传 / 漏传 / 仅本位币场景不适用）+ 既有 JUnit 外币用例覆盖（有/无 + 断言强度）。
- 重新评估 P1-RC-002 的 P0 升级：若存在域 Provider 在外币场景漏传 rate（默认活跃路径错误数据）→ 升 P0 → 触发 MR0；否则维持 P1。
- 产出验证报告 + §8 过程纪律自检；finding（若有新控制点）按 §7 裁决登记 arm-index。

## Non-Goals

- **不修改代码/ORM/api.xml/BizModel**（只读普查）。
- **不重新核实 P1-RC-002 的守卫缺失结论本身**（A1.1 已定级；本验证只核实「触发面」前提，决定 P0 升级与否）。
- **不实施修复**（修复经 MR0/MR1）。
- **不修改真相源**（§9 冻结）。

## Task Route

- Type: `verification or audit work`（运行时调用面普查 + P0 升级评估）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据[含 P0④活跃数据破坏] + §7 衔接 + §8 自检 + §10 MR0 即时通道）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.2 行）+ `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` §7 存疑点 2 + §5.2 P1-RC-002（输入）+ `docs/design/finance/posting.md §多币种`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。调用面普查需多维度归类（Provider 实现 / 外币场景识别 / 测试覆盖 / P0 触发面评估）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读普查（grep + 读 JUnit + 引用 MA2/A1.1）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 各域 Provider 外币场景 setExchangeRate 调用面全集普查

Status: planned
Targets: `docs/audits/2026-08-07-0330-rc-ma4-a4-1-2-fx-rate-missing-trigger-surface.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done；A1.1 done（P1-RC-002 + §7 存疑点 2 已落盘）

- [ ] `Proof` 枚举调用面全集：grep 所有 `IErpFinAcctDocProvider` 实现 + 构造/派发 `PostingEvent`（或过账事件）的调用方（含各域 BizModel/Executor），产出调用点清单（禁止抽样）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 逐调用点核验：①文件:行 ②是否识别外币场景（currencyId != functionalCurrency 判定存在性）③外币场景是否显式 setExchangeRate（传 / 漏传 / 仅本位币不适用）④既有 JUnit 外币用例覆盖（引用 + 断言强度强/弱/无）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` P1-RC-002 P0 升级再评估（方法论 §2 P0④「活跃数据破坏」+ A1.1 §5.2 评估框架）：若普查发现某域 Provider 在外币场景漏传 rate 且该路径为默认活跃路径（非需 caller bug 前置）→ 升 P0；否则维持 P1。列明触发面证据 + 与 A1.1 §5.2 三条理由的对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 调用点全集矩阵落盘（每点 4 字段齐备），无遗漏
- [ ] P1-RC-002 P0 升级再评估有明确裁决（升 P0 / 维持 P1）+ 触发面证据

### Phase 2 - MR0 触发/fishing 衔接 + §8 自检 + 报告定稿

Status: planned
Targets: `docs/audits/2026-08-07-0330-rc-ma4-a4-1-2-fx-rate-missing-trigger-surface.md`（定稿）；`docs/audits/arm-index.md`（若新 finding）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 调用点矩阵 + P0 升级裁决完成

- [ ] `Proof` P0 即时通道（方法论 §10）：若 Phase 1 升 P0 → 在报告登记 + 在本计划记录「已触发 MR0 追加 R0.n 实体行」（标注触及会计过账逻辑 → ask-first + 独立 plan-audit）。本验证不实施修复。
      - Skill: none
- [ ] `Add` 若发现新控制点（非 P1-RC-002 同根因）→ 按 §7 grep arm-index 同域同控制点裁决「复用 or 新建」`P*-RC-xxx`，写入 arm-index MA4 分区；finding → MR0/MR1 双向可追溯。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 P1-RC-002 / P1-MA2-002/009 / P1-MA3-039 的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（调用点矩阵 + P0 升级裁决 + MR0 触发登记[若升 P0] + §8 自检齐全）
- [ ] 新 finding（若有）已写入 arm-index MA4 分区并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02c7b35bdffe5vU7kJbw6DJgv0，fresh session，未起草本计划）。本计划为三计划中最强：明确对 P1-RC-002 做 P0 升级再评估（§2 P0④「活跃数据破坏」+ 交叉对照 A1.1 §5.2 维持 P1 的三条理由），正是存疑点 2 的目的。逐项核验 A-J 全 PASS：Deps（A4.1 done 实测）、Baseline 逐项实测命中（prepareContext:537 exchangeRate 回退 / EXCHANGE_RATE_DEFAULT:78 / persistVoucher:817-820,826-828 / P1-RC-002 §5.2 引用正确）、只读审计正确、MR0 P0 行触及会计过账逻辑须 ask-first + 独立 plan-audit（§5）正确、finding→MR0/MR1 为 Non-Goal 一致、§去重 声明（P1-MA2-002/009 + P1-MA3-039 复用关系）准确、完整枚举纪律（调用点全集矩阵无遗漏）、反松弛合规、item typing/Skill 记录齐、Closure Gates audit-only 删 build/test 有据。无阻塞，无非阻塞修订建议。共识达成，转 active。

## Closure Gates

> 本计划为**只读运行时普查**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 调用点矩阵完整性（全集覆盖）+ P0 升级裁决 + MR0 触发登记 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.1.2 验证报告调用点矩阵齐全（全集）+ P0 升级裁决 + finding（若有）登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 P0④判据 + §10 MR0 + §去重协议一致；与 A1.1 §7-2 + P1-RC-002 一致
- [ ] 已运行验证：调用点矩阵完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-002（汇率缺失守卫）及新 finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是运行时普查，结果表面 = 验证报告 + P0 升级裁决 + finding 登记。P1-RC-002 守卫修复 + 升 P0 项（若有）经 MR0（P0 即时通道，触及会计过账逻辑须 ask-first + 独立 plan-audit）/ MR1（R1.0→RC-R1.n，P1 批量）实施。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

> （独立结束审计通过后填入）
