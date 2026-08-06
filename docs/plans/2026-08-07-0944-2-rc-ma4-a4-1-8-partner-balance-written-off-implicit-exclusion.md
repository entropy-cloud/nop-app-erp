# 2026-08-07-0944-2 rc-ma4-a4-1-8-partner-balance-written-off-implicit-exclusion PartnerBalanceUpdater.sumOpen 对 WRITTEN_OFF 隐式排除运行时核验

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.1.8（MA4 运行时行为验证 — A1.3 §7-1：`PartnerBalanceUpdater.sumOpen` 对 WRITTEN_OFF 隐式排除的边界运行时核验，PARTIAL→WRITTEN_OFF 边缘场景）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.8；存疑点来源 `docs/audits/2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` §7 存疑点 1（注意点②边界）
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1715-1-rc-ma1-a1-3-finance-f3-ar-ap-reconciliation.md`（A1.3 done，§5.2 注意点②接受 + §7 存疑点 1）、`docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（MA2 A2.5c 辅助账 5 态 + WRITTEN_OFF 一致排除既有证据输入）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.8 验证报告（落盘 `docs/audits/2026-08-07-0944-rc-ma4-a4-1-8-partner-balance-written-off-implicit-exclusion.md`）+ 必要时 arm-index finding 登记。**不改代码/ORM/api.xml/真相源**（只读核验：grep sumOpen 排除点 + 读既有 JUnit 边界覆盖 + 复用 MA2 + 必要时构造边界场景推理/读测试）。范式对齐 A4.1.1/A4.1.3（已 done 的逐点运行时核验）。

- **存疑点原文**（A1.3 报告 §7 存疑点 1 / 注意点②边界，`2026-08-02-1715-...-a1-3-arap.md` §7）：「注意点②边界：`PartnerBalanceUpdater.sumOpen` 对 WRITTEN_OFF 的隐式排除」——`PartnerBalanceUpdater.java:51-52` sumOpen 仅显式排除 `notIn(status, [SETTLED, CANCELLED])`，**未显式排除 WRITTEN_OFF**。WRITTEN_OFF 项进入查询后贡献 0（依赖 `ErpFinBadDebtProcessor.executeWriteOff:168-169` 置 openAmount=0）。静态推理：「部分核销后坏账」（PARTIAL→WRITTEN_OFF）经 `validateAmount:285-294`（amount ≤ openAmount）+ executeWriteOff:168（open-=amount）保证归零，成立；但该边界（非全额核销后坏账）运行时未单独覆盖。交 MA4 A4.1 按需追加 A4.1.n 实体行验证。

- **关联既有结论**：
  - A1.3 §5.2 注意点②（往来余额 ErpMdPartner 缓存字段）= **接受**（恒等式经 Σ openAmount 数学等价）+ §7 存疑点 1（WRITTEN_OFF 隐式排除边界交 MA4）。
  - A1.3 §5.2 注意点③（WRITTEN_OFF 一致排除 4 处）= **接受**：开放项查询（正向包含 OPEN/PARTIAL）/ 期末门禁 / 坏账准备基线 / 往来余额刷新 4 处一致排除——本存疑点是注意点②③交叉的**边界场景**（PARTIAL→WRITTEN_OFF 后余额刷新是否正确）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:147` UC-FIN-08 断言③——「往来单位.应收余额 = Σ发票 − Σ核销 − Σ红字」。状态轴 `use-cases.md:11` 含 WRITTEN_OFF（已坏账核销）。隐含契约：已坏账核销项（openAmount=0）不应再计入应收余额。L2（`ar-ap-reconciliation.md §余额计算`）Σ openAmount 实现该恒等式。

- **实现现状（L3，实测锚点，本计划起草时核实）**：
  - 往来余额刷新：`module-finance/erp-fin-service/.../service/reconciliation/PartnerBalanceUpdater.java` `refresh:33-44`（partner.receivableBalance = sumOpen(RECEIVABLE) / payableBalance = sumOpen(PAYABLE)）/ `sumOpen:46-62`（`q.addFilter(notIn("status", [SETTLED, CANCELLED])):51-52`，Σ openAmountFunctional）——**未显式排除 WRITTEN_OFF**。
  - 坏账核销生效：`module-finance/erp-fin-service/.../service/processor/ErpFinBadDebtProcessor.java` `executeWriteOff:163-182`（validateAmount :165 → settled+=amount :166-167 → **open-=amount :168-169** → **status=WRITTEN_OFF :170** → 凭证 :173-181）。
  - 金额守卫：`ErpFinBadDebtProcessor.validateAmount:285-294`——`amount.signum() <= 0` 抛 `ERR_BAD_DEBT_AMOUNT_INVALID` :287；`amount > openAmountFunctional` 抛 `ERR_BAD_DEBT_AMOUNT_OVER_OPEN` :289-292。
  - **边界场景（本验证对象）**：PARTIAL 辅助账项（settled < total，open > 0）执行 writeOff(amount=剩余 open) → executeWriteOff:168 open-=amount 使 open 归零 → status=WRITTEN_OFF。此时 sumOpen 查询含该 WRITTEN_OFF 项但贡献 0 → 余额正确。**静态推理成立**（validateAmount 守卫 amount ≤ open + open-=amount 归零），但运行时边界（PARTIAL→WRITTEN_OFF）未单独测试覆盖。

- **既有证据（复用输入）**：
  - MA2 `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（A2.5c）§1.2 + §2.1 证实辅助账 5 态全部可达（含 WRITTEN_OFF 经 executeWriteOff 可达）+ §1.4 期末门禁排除 WRITTEN_OFF。本验证复用其「WRITTEN_OFF 可达 + 一致排除」结论，**只补「PARTIAL→WRITTEN_OFF 边界下 sumOpen 余额正确性」差异**。
  - A1.3 §3 `TestErpFinPartnerBalance#testReceivableBalanceViaReconciliation:70`（强断言全核销后 receivableBalance==0）+ `#testPayableBalanceDrivenByOpenAmount:46`——但均**全额核销/全场景**，未单独覆盖「PARTIAL 后坏账」边界。

- **初步实测（本计划起草时的部分核验，执行时复核）**：
  - grep `module-finance/erp-fin-service/src/test/java/` WRITTEN_OFF + PartnerBalance / receivableBalance 联合——现有坏账测试（`TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL:140` 全额坏账 openAmount→0）未构造「先部分核销再坏账」的 PARTIAL→WRITTEN_OFF 边界。
  - 即本验证最可能结论 = **接受（隐式排除依赖 openAmount=0 不变量，静态推理 + 守卫链成立）**，属**确认性边界核验**（边际信息量较低）；剩余价值 = 边界场景的运行时/测试覆盖确认 + 不变量失效条件枚举（若 any）。

- **剩余差距**：「PARTIAL→WRITTEN_OFF 边界下 sumOpen 余额正确性」未单独运行时覆盖（A1.3 静态推理成立但未边界测试；MA2 证实 WRITTEN_OFF 可达但未核该边界余额）。

- **保护区域**：只读核验（grep + 读 JUnit + 读守卫链 + 引用 MA2/A1.3），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——若发现隐式排除依赖被破坏（如未来 executeWriteOff 改为不归零 openAmount）即 P1 finding（余额计算错误），修复经 MR1（BizModel 代码逻辑修复预授权类目，非会计过账核心路径 VoucherFact/PostingProcessor）。

## Goals

- 边界场景运行时核验：构造/检索「PARTIAL 辅助账项（open > 0）执行 writeOff(amount=剩余 open) 后 partner.receivableBalance 是否正确反映 openAmount=0」的证据。优先复用既有测试覆盖；若既有测试未覆盖该边界，则以静态守卫链推理（validateAmount :285-294 + executeWriteOff :168-169 + sumOpen :51-62）证实不变量成立，并枚举「不变量失效条件」（若 any）。
- 给出隐式排除依赖证据：sumOpen 查询路径（file:line）+ WRITTEN_OFF 进入查询但贡献 0 的机制（executeWriteOff:168 open-=amount）+ 守卫链（validateAmount amount ≤ open）。
- 对齐 UC-FIN-08 断言③ + 状态轴 WRITTEN_OFF 给出边界结论：①若边界成立（open 归零，余额正确）→ 接受（隐式排除依赖 openAmount=0 不变量，守卫链保证）；②若发现不变量可被绕过（如未来代码改动致 WRITTEN_OFF 项 openAmount 非 0）→ P1（余额计算错误，UC-FIN-08 断言③恒等式破坏）；③若仅测试覆盖缺口（行为正确但无边界测试）→ P2（测试覆盖缺口，非行为缺陷）。
- 产出验证报告 + §8 过程纪律自检；finding（若有）按 §7 裁决登记 arm-index。

## Non-Goals

- **不重新核实 UC-FIN-08 三验收标准 + 坏账 4 命题全场景符合性**（A1.3 §5.1 已判接受；本验证只补 PARTIAL→WRITTEN_OFF 边界差异）。
- **不重新核实 WRITTEN_OFF 一致排除 4 处**（A1.3 §5.2 注意点③已判接受；本验证只核 sumOpen 该 1 处的边界）。
- **不修改代码/ORM/api.xml/BizModel**（只读核验）。
- **不实施修复**（修复经 MR1；本验证仅登记 finding）。
- **不修改真相源**（§9 冻结）。
- **不展开 A1.3 §7-2/§7-3**（A4.1.9/A4.1.10 范围）。

## Task Route

- Type: `verification or audit work`（边界场景运行时核验 + 接受/分歧裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.8 行）+ `docs/audits/2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` §7 存疑点 1 + §5.2 注意点②③（输入）+ `docs/design/finance/ar-ap-reconciliation.md §余额计算` + `docs/design/finance/bad-debt.md §步骤3`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。边界核验需多维度归类（sumOpen 查询路径 / WRITTEN_OFF 不变量 / 守卫链 / 测试覆盖 / MA2 复用 / 接受-or-分歧裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读核验（grep + 读 JUnit + 读守卫链 + 引用 MA2/A1.3）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - PARTIAL→WRITTEN_OFF 边界 sumOpen 余额正确性核验

Status: completed
Targets: `docs/audits/2026-08-07-0944-rc-ma4-a4-1-8-partner-balance-written-off-implicit-exclusion.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.8 行）；A1.3 done（§7 存疑点 1 已落盘 + §5.2 注意点②③接受）

- [x] `Proof` 隐式排除依赖链核验：给出 sumOpen 查询路径（`PartnerBalanceUpdater.java:46-62`，notIn 仅 SETTLED/CANCELLED :51-52）+ WRITTEN_OFF 进入查询但贡献 0 的机制（`executeWriteOff:168-169` open-=amount）+ 守卫链（`validateAmount:285-294` amount ≤ openAmountFunctional + amount > 0）。证实「PARTIAL→WRITTEN_OFF 时 open 经 open-=amount 归零」的静态推理。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 既有测试边界覆盖核验：grep `module-finance/erp-fin-service/src/test/java/` 坏账 + 核销 + partner balance 联合测试，确认是否存在「先部分核销（PARTIAL）再坏账（WRITTEN_OFF）后断言 partner.receivableBalance」的边界用例。引用 `TestErpFinBadDebt` / `TestErpFinPartnerBalance` / `TestErpFinReconciliation` 全集，标注断言强度（强/弱/无该边界）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` MA2 复用：引用 MA2 A2.5c §1.2/§2.1（WRITTEN_OFF 经 executeWriteOff 可达 + 5 态完整）+ §1.4（期末门禁排除 WRITTEN_OFF），声明本验证只补「PARTIAL→WRITTEN_OFF 边界 sumOpen 余额」差异，不重新核实状态机行为。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 边界符合性结论（方法论 §2 判据 + 三源对照）：①若静态守卫链证实 open 归零（边界成立）+ 既有测试覆盖该边界 → 接受；②若边界成立但既有测试未覆盖该边界 → P2（测试覆盖缺口，非行为缺陷，登记 successor 补边界测试）；③若发现不变量可被绕过（WRITTEN_OFF 项 openAmount 非 0 致余额虚高）→ P1（UC-FIN-08 断言③恒等式破坏）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.3 §5.2 注意点②③接受结论分层一致。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 隐式排除依赖链证据落盘（sumOpen 查询路径 + WRITTEN_OFF 贡献 0 机制 + 守卫链 file:line 齐备）
- [x] 边界符合性结论明确（接受 / P2 测试覆盖缺口 / P1 余额错误），与 A1.3 §5.2 注意点②③接受结论分层一致

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-07-0944-rc-ma4-a4-1-8-partner-balance-written-off-implicit-exclusion.md`（定稿）；`docs/audits/arm-index.md`（若新 finding/successor）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 边界核验 + 结论完成

- [x] `Add` 若定 P1/P2 → 按 §7 grep arm-index finance AR-AP 核销/坏账/往来余额同域同控制点裁决「复用 or 新建」`P*-RC-xxx` 或 successor 行，写入 arm-index MA4 分区；双向可追溯注记（finding → MR1 / successor 触发条件）。若接受 → 在报告登记「无新 finding，归 A1.3 §5.2 注意点②接受 + §7 存疑点 1 闭合」。禁止未经比对新建。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.3 §5.2 注意点②③ / MA2 A2.5c WRITTEN_OFF 一致排除 / P2-MA2-039 坏账状态机隔离 config-gated 的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（依赖链 + 边界结论 + finding/successor 衔接 + §8 自检齐全）
- [x] 新 finding/successor（若有）已写入 arm-index MA4 分区并有 grep 依据（本验证若维持接受则无写入，本条 N/A 满足）

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02b3c8081ffeQL3157UTce2fmq，fresh session，未起草本计划）。逐项核验 A-I 全 PASS：Deps（A4.1 done roadmap:126 / A1.3 done roadmap:42）、单结果表面（一份验证报告，Non-Goals 显式排除 §7-2[A4.1.9]/§7-3[A4.1.10] + 不重审注意点②③全场景接受）、Baseline 逐项实测命中（PartnerBalanceUpdater refresh:33-44 + sumOpen:46-62 + notIn[SETTLED,CANCELLED]:51-52 **无 WRITTEN_OFF** / ErpFinBadDebtProcessor executeWriteOff:163-182[open-=amount :168-169 / status=WRITTEN_OFF :170] + validateAmount:285-294[amount ≤ openAmountFunctional 守卫] / §7-1 存疑点忠实引用 / MA2 A2.5c 报告存在）、只读审计正确（预授权类目，P1→MR1 BizModel 代码逻辑非 VoucherFact/PostingProcessor 核心路径）、反松弛合规（"若 any" 为合法空集条件枚举非模糊延迟）、item typing + Skill 记录齐、Closure Gates audit-only 删 build/test 有据、Q4 路由正确、决策树健全（3 分支 accept/P2测试覆盖缺口/P1余额错误 正确映射 §2 判据 + 与 A1.3 §5.2 注意点②③接受分层一致非冲突 + PARTIAL→WRITTEN_OFF 静态推理链 validateAmount 守卫 amount ≤ open + executeWriteOff open-=amount 归零 证立）。无阻塞。非阻塞观察：决策树无显式 P0 分支——可辩护（partner balance 为缓存派生字段非 GL 过账正确性，最坏 P1 验收③恒等式破坏非 P0④）。共识达成，转 active。

## Closure Gates

> 本计划为**只读运行时核验**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 隐式排除依赖链完整性 + 边界结论 + finding/successor 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.8 验证报告依赖链 + 边界结论齐全 + finding/successor（若有）登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议一致；与 A1.3 §7-1 + §5.2 注意点②③ + MA2 A2.5c 一致
- [x] 已运行验证：依赖链 + 边界结论完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### PARTIAL→WRITTEN_OFF 边界测试补强（若定 P2 测试覆盖缺口）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是运行时边界核验，结果表面 = 验证报告 + 边界结论 + finding/successor 登记。若结论为「边界行为正确（open 归零）但既有测试未覆盖该边界」→ P2 测试覆盖缺口（非行为缺陷），补边界测试经 MR1（R1.0→RC-R1.n，纯测试代码修复预授权类目）。若结论为接受（边界成立 + 既有测试覆盖）则无 successor。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 finding/successor 交叉引用展开；若维持接受则无 successor）

## Closure

Status Note: 独立结束审计（fresh session 子代理，不重用执行者上下文）通过。验证报告 `docs/audits/2026-08-07-0944-rc-ma4-a4-1-8-partner-balance-written-off-implicit-exclusion.md`（`> Audit Status: closed`，10 段齐全）依赖链 + 边界结论（接受 + P2-RC-082 测试覆盖缺口）+ §8 自检齐全；plan Phase 1+2 全 `[x]` + Status `completed`；roadmap A4.1.8 done；docs/logs/2026/08-07.md:3 已记录。审计性质 = 只读核验，无代码/ORM/api.xml/真相源变更，故删除 build/test 门控。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，未起草/未执行本计划，仅做结束审计）
- Evidence: 逐项复核 Phase 1 依赖链 file:line 命中实时代码——`PartnerBalanceUpdater.java#sumOpen:46-62`（`notIn("status",[SETTLED,CANCELLED]):51-52` **未显式排除 WRITTEN_OFF**，实测确认）；`ErpFinBadDebtWriteOffProcessor.java#writeOff:19-29`（`:22` 恒取 `item.getOpenAmountFunctional()` 全量剩余 open，报告"比计划认知更强"关键发现确认）；`ErpFinBadDebtProcessor.java#executeWriteOff:163-182`（open-=amount:168-169 / status=WRITTEN_OFF:170 / 凭证:173-181）+ `validateAmount:285-294`（amount ≤ 0 / amount > open 双守卫）+ `requireOpenArApItem:255-271`（仅 OPEN/PARTIAL 可达 writeOff）全部命中。静态推理链成立：PARTIAL→writeOff(全量 open)→executeWriteOff open-=amount→open 恒归零→WRITTEN_OFF 项 sumOpen 贡献 0。测试方法行号实测命中 5/5（testWriteOffSetsStatusAndVoucherNoPL:140 / testReceivableBalanceViaReconciliation:70 / testPayableBalanceDrivenByOpenAmount:46 / testRecoveryRestoresArApItem:179 / testProvisionExcludesNegativeAndWrittenOff:114），grep PARTIAL+writeOff+receivableBalance 组合零命中 → P2 测试覆盖缺口确认。
- Evidence: Phase 2 衔接复核——`docs/audits/arm-index.md:294` P2-RC-082 entry 落盘且证据与报告一致（含"新根因"grep 比对 + 与 P2-MA2-039 不同控制点声明 + MR1 successor 触发条件）；§8 重跑 `bash docs/audits/nop-compliance-checker.sh` actual == baseline（R1a/R1b/R1c=0/0/0、R1d=14，finance 模块零命中，证实只读无回归风险）；docs/logs/2026/08-07.md:3 已记录 A4.1.8 完成条目。
- Evidence: 五点一致性已验证——Plan Status: completed / Phase 1 Status: completed + Exit Criteria 2/2 [x] / Phase 2 Status: completed + Exit Criteria 2/2 [x] / Closure Gates 8/8 [x] / Closure 本节真实证据（非占位符）。反空洞：验证报告非空壳（10 段、212 行、file:line 锚点齐备且经独立复核命中实时代码）。延迟诚实：P2-RC-082 为 watch-only successor（纯测试代码补强，预授权类目，不触及 ORM/过账/数据迁移），无已确认缺陷隐藏为 follow-up。

Follow-up:

- MR1（R1.0→RC-R1.n）按 P2-RC-082 successor 触发条件展开：补「PARTIAL 辅助账项 → writeOff(剩余 open) → 断言 partner.receivableBalance」边界测试用例（纯测试代码，roadmap 预授权类目，不阻塞本验证闭环）。
