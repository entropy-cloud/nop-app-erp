# 2026-08-07-0330-1 rc-ma4-a4-1-1-posted-false-listener-coverage UC-FIN-02 断言④ posted=false 8 域 listener 回写覆盖率运行时核验

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.1.1（MA4 运行时行为验证 — A1.1 §7-1：UC-FIN-02 断言④「业务单据.posted=false」8 域 listener 回写覆盖率逐域核验）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.1；存疑点来源 `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` §7 存疑点 1
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行 / deferred successor）、`docs/plans/2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done）、`docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（MA2 §5.9 场景D 8 域 reversal writeback 矩阵，既有证据输入）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.1 验证报告（落盘 `docs/audits/2026-08-07-0330-rc-ma4-a4-1-1-posted-false-listener-coverage.md`）+ 必要时 arm-index finding 登记。**不改代码/ORM/api.xml/真相源**（只读核验：grep 域 listener + 读既有 JUnit/E2E + 复用 MA2 证据）。

- **存疑点原文**（A1.1 报告 §7 存疑点 1，`2026-08-02-1645-...-a1-1-posting.md:293`）：「UC-FIN-02 断言④「业务单据.posted=false」域 listener 实际回写覆盖率」——L3 证实 `ErpFinPostingProcessor.dispatchReversalEvent:376-401` 派发 `VoucherReversedEvent`，MA2 §5.9 场景D 证实 8 域 reversal writeback 测试矩阵；但「posted=false 是否在所有 8 域 listener 中一致回写」属逐域运行时行为，A1.1 仅引用 MA2 证实，**未逐域核验 listener 实现**。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:42` UC-FIN-02 断言④逐字「业务单据.posted = false」（业务单据作废触发红字冲销后，源单据 posted 标志回写为 false）。

- **实现现状（L3，实测锚点）**：
  - 引擎派发：`module-finance/erp-fin-service/.../service/posting/ErpFinPostingProcessor.java` `dispatchReversalEvent:376-401`（SYNC 默认 / ASYNC afterCommit :388-394 派发 `VoucherReversedEvent`）；引擎自身**不直接回写各域单据 posted**——回写责任在**各域监听 `VoucherReversedEvent` 的 listener**（per-domain writeback 模式，`posting.md §反写契约` + 引擎 javadoc:59-60 明示）。
  - 「8 域」= MA2 §5.9 场景D 所述业财回链覆盖域的上界口径（purchase / sales / inventory / assets / manufacturing / projects / finance[expense-claim/notes 等] / quality 或 maintenance 等）。**注意（独立草案审查实测）**：实仓 grep `IErpFinVoucherReversedListener` 目前仅命中约 4 个域 listener（inventory / manufacturing / purchase / sales）——「8 域」是 MA2 的回链设计口径，**实际 listener 实现数以 Phase 1 grep 全集为准**：实际数 < 8 不是异常，**缺失 listener 的域本身就是本验证要捕获的 P1 finding（UC-FIN-02 断言④ posted=false 回写未覆盖）**；执行者须按实际 grep 结果逐域矩阵化，**禁止凑足 8**。确切清单以 `docs/design/flow-overview.md` 业财回链图 + 实仓 grep `VoucherReversedEvent`/`IErpFinVoucherReversedListener` 监听点为准。

- **既有证据（复用输入，方法论 §去重协议）**：
  - MA2 `2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md` §5.9 场景D「8 域 reversal writeback 测试矩阵」——证实 writeback 模式存在 + 测试矩阵存在；本验证**只补「需求契约↔逐域 listener 实现」差异**（每个域 listener 是否实际写 `posted=false`，而非仅作废/状态机迁移），不重新核实 MA2 已证实的状态机行为。
  - A1.1 §4.1 复用 MA2 §5.7+§5.9 场景C/D 判定为「接受」，本存疑点是其遗留的运行时逐域核验项。

- **剩余差距**：8 域 listener 逐域 `posted=false` 回写覆盖率**未逐域核验**（MA2 证 writeback 模式存在，但未按 UC-FIN-02 断言④逐域确认 posted 字段回写）。若某域 listener 缺 posted=false 回写（仅作废/状态机迁移），即为需求分歧 finding（定级按方法论 §2）。

- **保护区域**：只读核验（grep listener + 读 JUnit/E2E + 引用 MA2），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——确认的缺陷 finding 经 MR0（P0）/MR1（R1.0→RC-R1.n，P1）修复。

## Goals

- 逐域（8 域全集，禁止抽样）核验监听 `VoucherReversedEvent` 的域 listener 是否在红字冲销后将源单据 `posted` 回写为 `false`，对齐 UC-FIN-02 断言④。
- 每域给出运行时行为证据：listener 文件:行 + posted 回写语句（或缺失）+ 既有 JUnit/E2E 断言强度（强/弱/无）。
- 对每域给出符合性结论（接受 / P0 / P1 / P2），对确认缺陷按 §7 复用 or 新建裁决登记 finding（`P*-RC-xxx` 系列）并写入 arm-index MA4 分区。
- 产出验证报告（§6 报告骨架的验证变体）+ §8 过程纪律自检（checker actual vs baseline + closure-audit 独立性 + arm-index 交叉去重声明）。

## Non-Goals

- **不修改代码/ORM/api.xml/BizModel/view.xml**（只读核验）。
- **不重新核实 MA2 已证实的状态机/红冲链路行为**（§去重协议；只补逐域 posted 回写差异）。
- **不实施 finding 的修复**（修复经 MR0/MR1；本验证仅登记 finding）。
- **不修改真相源**（product-scope/use-cases/owner doc；§9 冻结）。
- **不展开扩展域存疑点**（A4.2 范围）。

## Task Route

- Type: `verification or audit work`（运行时行为逐域核验）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据 + §7 衔接 + §8 自检 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.1 行）+ `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` §7 存疑点 1（输入）+ `docs/design/flow-overview.md`（业财回链域清单）+ `docs/design/finance/posting.md §反写契约`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。运行时逐域核验需多维度归类（listener 实现 / 测试断言 / MA2 复用），复用其维度框架。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读核验（grep + 读 JUnit/E2E + 引用 MA2 报告）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；本验证无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 8 域 listener 全集枚举与逐域 posted=false 回写核验

Status: completed
Targets: `docs/audits/2026-08-07-0330-rc-ma4-a4-1-1-posted-false-listener-coverage.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.1 行）；MA1 done（业财域 A1.1 报告 §7 存疑点已落盘）

- [x] `Proof` 枚举 listener 全集：grep `VoucherReversedEvent`/`IErpFinVoucherReversedListener` 监听点（`@Observer`/事件订阅/registry 注册）+ 交叉对照 `flow-overview.md` 业财回链域图，产出**实际** listener 域清单（禁止抽样；禁止凑足「8」——实际数 < 8 时，缺失域即 P1 finding 主体，须在报告声明实际数 + 与 MA2 §5.9 场景D「8 域」口径的差异依据）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 执行证据：实仓 `rg -l "implements IErpFinVoucherReversedListener"`（生产代码）= 恰好 4 域 4 文件（PurReversalListener/SalReversalListener/InvReversalListener/MfgSubcontractReversalListener）。报告 §2.2 已声明实际数（4）+ 与 MA2「8 域」口径差异依据（MA2 §5.9 场景D 覆盖测试清单含方向一测试如 `TestErpMntVisitCancelReversal`，方向二 listener 实际=4，与设计 `posting.md §裁决4` 仅列 4 域一致）。
- [x] `Proof` 逐域核验：对每域 listener 给出 ①listener 文件:行 ②是否写 `setPosted(false)`（语句:行 or 缺失）③既有 JUnit/E2E 是否断言 posted==false（引用 + 断言强度强/弱/无）④MA2 §5.9 场景D 矩阵对应行（复用证据）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 执行证据：报告 §2.3（逐域 posted 回写语句:行）+ §3（逐域测试断言强度，4 域全部强断言）+ §4.1（MA2 §5.9 场景D 复用行）。4/4 域全部写 setPosted(false)，无缺失。
- [x] `Decision` 逐域符合性结论（方法论 §2 判据）：listener 写 posted=false 且有强断言 → 接受；写 posted=false 但仅弱/无断言 → P2（测试覆盖缺口，非行为缺陷）；未写 posted=false（仅作废/状态迁移）→ P1（UC-FIN-02 断言④行为未实现）；默认活跃路径且破坏会计回链一致性 → 评估 P0。每结论列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 执行证据：报告 §5.1 逐域符合性矩阵——4 域全部「接受」（§2 接受判据：L3-L5 全证据一致）+ §5.2 非 listener 域经方向一满足（assets/maintenance/projects 方向一 setPosted(false)）+ §4.3 命中判据裁决（无 P0/P1，方向二覆盖非缺陷）。每结论列明 §2 判据编号 + 三源对照。

Exit Criteria:

- [x] 8 域 listener 逐域矩阵落盘（每域 4 字段齐备：listener:行 / posted 回写 / 测试断言强度 / MA2 复用行）—— 实际 4 域（方向二 listener grep 实测），报告 §2.2/§2.3/§3/§4.1 矩阵每域 4 字段齐备
- [x] 每域有明确符合性结论（接受/P0/P1/P2），无悬空「待查」—— 报告 §5.1/§5.2 全域结论明确（4 方向二域接受 + 3 方向一域接受），无待查

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-07-0330-rc-ma4-a4-1-1-posted-false-listener-coverage.md`（定稿）；`docs/audits/arm-index.md`（若新 finding）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 逐域矩阵完成

- [x] `Add` 对确认缺陷按 §7 裁决「复用 or 新建」：grep arm-index 同域同控制点（如 posted 反写 / reversal writeback 相关行）——同根因同控制点复用既有 ID（追加 RC 交叉注记），新根因/新控制点新建 `P*-RC-xxx` 并列差异依据。禁止未经比对新建。
      - Skill: none
      - 执行证据：报告 §6.1 grep 比对（P1-MA2-051 已 resolved / P1-MA4-021(f) 已 resolved / P2-MA2-057 watch-only / P2-MA3-026 不相关）；§6.2 裁决=0 新建（存疑点正向消解为接受，无确认缺陷）。
- [x] `Add` finding（若有）写入 `arm-index.md` MA4 RC finding 分区；finding → MR0/MR1 修复行双向可追溯注记（本验证不实施修复）。
      - Skill: none
      - 执行证据：本验证 0 新 finding（存疑点正向消解为接受），无需写入 arm-index MA4 分区。§6.2 已声明无新建。
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（本验证无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明。不以 checker 脚本退出码 0 作为门控通过依据（区分 reporter vs CI 门控）。
      - Skill: none
      - 执行证据：报告 §8 已落盘——checker actual vs baseline 表（R1/R2 全 == baseline 0 漂移 + R3-R12 脚本截断既有工具行为无回归风险）+ closure-audit 独立性声明 + arm-index 交叉去重声明 + 区分 reporter vs CI 门控声明。
- [x] `Proof` P0 即时通道评估：若 Phase 1 定级出 P0（活跃数据破坏/会计回链一致性破坏），按方法论 §10 在报告登记「已触发 MR0 追加 R0.n 实体行」（本验证不实施修复）。
      - Skill: none
      - 执行证据：报告 §7 已声明「本验证 Phase 1 定级未出 P0（全域接受），按 §10 不触发 MR0」。

Exit Criteria:

- [x] 验证报告定稿（逐域矩阵 + 符合性结论 + finding 衔接 + §8 自检齐全）—— 报告 §1-§10 9 段落齐全（验证变体），已落盘 `docs/audits/2026-08-07-0330-rc-ma4-a4-1-1-posted-false-listener-coverage.md`
- [x] 新 finding（若有）已写入 arm-index MA4 分区并有 grep 依据 —— 本验证 0 新 finding，N/A（§6.2 声明无新建）

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02c7b35bdffe5vU7kJbw6DJgv0，fresh session，未起草本计划）。逐项核验 A-J 全 PASS：Deps（A4.1 expander 经实测 done：Plan Status completed + Closure Gates 全 [x] + 独立 closure audit passes）、单结果表面（A4.1.1 一份验证报告，无跨切片合并）、Baseline 逐项实测命中（dispatchReversalEvent:376-401 派发 VoucherReversedEvent / EXCHANGE_RATE_DEFAULT:78 / ErpFinAcctDocRegistry.init/getProvider / A1.1 §7 存疑点 1 逐字忠实引用 / MA2 §5.9 场景D 复用正确）、只读审计正确（无代码/ORM/真相源变更，保护区域门控正确）、反松弛合规（"若有/必要时"为合法分支非 in-scope 模糊词）、item typing 合规、Skill 记录齐、Closure Gates audit-only 删除 build/test 有据（执行规则 7）、完整枚举纪律（listener 域全集禁止抽样）、Q4 路由正确（P0→MR0 / P1→MR1，禁方案 B）。无阻塞。采纳非阻塞建议（已修订入计划）：①实仓 grep `IErpFinVoucherReversedListener` 仅约 4 域（inventory/manufacturing/purchase/sales），「8 域」为 MA2 回链设计口径——已将 baseline/Phase 1/Closure Gates 显式改为「实际数以 grep 为准，<8 时缺失域即 P1 finding 主体，禁止凑足 8」；②"out-of-scope improvement" 措辞对 P1 修复略不精确——Successor Required=yes + MR0/MR1 路由正确，保持。共识达成，转 active。

## Closure Gates

> 本计划为**只读运行时核验**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——验证报告产出不触发编译或测试。验证 = 逐域矩阵完整性（8 域全覆盖）+ 符合性结论齐备 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.1 验证报告逐域矩阵齐全（实际 grep 得到的 listener 域全集覆盖，不强制等于 8；缺失域即 finding 主体）+ 每域符合性结论 + finding（若有）登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议一致；与 A1.1 §7 存疑点 1 + MA2 §5.9 场景D 一致
- [x] 已运行验证：逐域矩阵完整性 + §8 checker actual vs baseline 实测记录 + finding 复用/新建裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 确认缺陷的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是运行时验证，结果表面 = 验证报告 + finding 登记。缺陷修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

- **Date**: 2026-08-07
- **Auditor**: 独立子代理（fresh session，未起草本计划，未执行本验证）
- **OVERALL verdict**: **close**

### 12 项结束审计清单逐项 PASS（live-repo 证据）

1. **PASS** — 阶段状态一致性：Phase 1/2 均 `Status: completed`（plan:60,84），8 项 phase body items 全 `[x]`（plan:67,70,73,79,80,91,94,97,100,106,107），4 项 Exit Criteria 全 `[x]`（plan:79,80,106,107），phase body 无残留 `[ ]`。
2. **PASS** — 报告 9 段落完整性：§1 需求契约（report:33-50）+ §2 实现证据（report:54-86）+ §3 测试证据（report:89-101）+ §4 运行时行为（report:104-134）+ §5 符合性结论（report:137-164）+ §6 arm-index 衔接（report:167-184）+ §7 静态存疑点（report:188-192）+ §8 过程纪律自检（report:196-213）+ §9 真相源冻结（report:217-219）+ §10 MA2 差异增量（report:223-225）齐全。
3. **PASS** — Listener 枚举准确性（ANTI-HOLLOW）：`rg -l "implements IErpFinVoucherReversedListener"` 排除 test = 恰好 4 个生产 listener：`PurReversalListener.java` / `SalReversalListener.java` / `InvReversalListener.java` / `MfgSubcontractReversalListener.java`（finance 测试文件正确排除）。报告 §2.2 声明 4 域，并解释「8 域」= MA2 §5.9 场景D 回链设计口径上界（含方向一测试如 `TestErpMntVisitCancelReversal`），方向二 listener 实际 = 4（report:74）。
4. **PASS** — posted=false 回写逐域（ANTI-HOLLOW）：4/4 listener 全部写 `setPosted(false)`：
   - `PurReversalListener.java:75,89,103,119`（4 路径全 match 报告 §2.3）
   - `SalReversalListener.java:72,86,100,116`（4 路径全 match）
   - `InvReversalListener.java:65,76`（2 路径全 match）
   - `MfgSubcontractReversalListener.java:70`（match）
   全部含 `posted != TRUE` 早退守卫。
5. **PASS** — 测试断言强度（ANTI-HOLLOW）：5 个测试类（6 文件路径）全部含 `assertFalse(...getPosted())` 强断言：
   - `TestErpPurFinanceReversalWriteback.java:101`
   - `TestPurReversalListenerReceiveRollback.java:63`
   - `TestErpSalFinanceReversalWriteback.java:90`
   - `TestSalReversalListenerRollback.java:68,84,100`（3 路径补强）
   - `TestErpInvFinanceReversalWriteback.java:97`
   - `TestErpMfgSubcontractReverse.java#testFinanceReverseVoucherRollsBackSubcontractOrder:208,229`
6. **PASS** — 方向一 non-listener 域裁决（key judgment）：`ErpAstDisposalProcessor.reverseApprove:107-121`（assets）调 `postingDispatcher.reverse(disposal):113` 后置 `disposal.setPosted(false):121`；`ErpAstDepreciationScheduleReverseDepreciationProcessor.java:51 schedule.setPosted(false)`；`AbstractErpMntSparePartUsageProcessor:104 usage.setPosted(false)`（maintenance）。证伪「缺方向二 listener 域即缺陷」假设（report §4.2/§4.3）。
7. **PASS** — 设计背书：`docs/design/finance/posting.md §裁决4 回退目标态表`（around lines 365-376）仅列 4 域方向二回退目标态（purchase/sales/inventory/manufacturing），与 grep 结果一致。
8. **PASS** — §8 自检：checker actual vs baseline 表齐全（report:200-208），R1/R2 全 == baseline（实测对 `compliance-baseline.md §BASELINE (machine-readable)` R1d=14/R2a=34/R2b=229/R2c=1382/R2d=34 一致）；含 closure-audit 独立性声明（report:212）+ arm-index 交叉去重声明（report:213）+ 区分 reporter vs CI 门控声明（report:198「不以 checker 脚本退出码 0 作为门控通过依据」）。
9. **PASS** — arm-index 衔接：报告 §6.1 grep 4 候选 finding 给裁决（P1-MA2-051 resolved / P1-MA4-021(f) resolved / P2-MA2-057 watch-only / P2-MA3-026 不相关），§6.2 裁决 = 0 新建（存疑点正向消解为接受）。注：P2-MA3-026 在 arm-index 无独立行（仅属 MA3 P2-MA3-023~035 范围 summary），但报告裁决为「不相关」+ 0 新建结论不受影响——非阻塞观察。
10. **PASS** — 真相源冻结：`git status --short` 仅显示 4 个未跟踪文件（本 plan + 本 report + 2 sibling A4.1.2/A4.1.3 plan，均为 docs 新增），无 ORM/Java/api.xml/view.xml/product-scope/use-cases/owner doc 需求契约段落变更。
11. **PASS** — UC-FIN-02 断言④ verbatim：`docs/design/finance/use-cases.md:52` 逐字「业务单据.posted = false」，与报告 §1（report:42）逐字引用一致。
12. **PASS** — 无范围内项目降级 deferred：4 域方向二矩阵 + 3 域方向一矩阵 + 整体裁决（接受）+ finding 衔接（0 新建）+ §8 自检全齐。Phase body 无残留 `[ ]`，Closure Gates 经本次审计全部 `[x]`（见下）。`Deferred But Adjudicated` 段为方法论 §10 通用模板（本验证 0 缺陷，无可 deferred 修复项），非 in-scope 降级。
