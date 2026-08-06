# 2026-08-07-0944-3 rc-ma4-a4-1-9-bad-debt-voucher-businesstype-assertion-strength TestErpFinBadDebt 凭证 businessType 枚举断言强度评估

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.1.9（MA4 运行时行为验证 — A1.3 §7-2：`TestErpFinBadDebt` 凭证 businessType 枚举断言强度评估，未断言 BAD_DEBT_WRITE_OFF）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.9；存疑点来源 `docs/audits/2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` §7 存疑点 2
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1715-1-rc-ma1-a1-3-finance-f3-ar-ap-reconciliation.md`（A1.3 done，§3 测试证据汇总 + §7 存疑点 2）、`docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（MA2 A2.5c 坏账命题 W1-W4 既有强测试证据输入）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.9 验证报告（落盘 `docs/audits/2026-08-07-0944-rc-ma4-a4-1-9-bad-debt-voucher-businesstype-assertion-strength.md`）+ 必要时 arm-index finding/successor 登记。**不改代码/ORM/api.xml/真相源**（只读评估：grep 测试断言 + 读凭证 businessType 写入点 + 复用 MA2 + 断言强度评级）。范式对齐 A4.1.6（已 done 的 E2E 断言强度评估同型工作项）。

- **存疑点原文**（A1.3 报告 §7 存疑点 2，`2026-08-02-1715-...-a1-3-arap.md` §7）：「`TestErpFinBadDebt.testWriteOffSetsStatusAndVoucherNoPL:140` 凭证 businessType 断言强度」——测试断言凭证科目/方向/金额/无 6701 费用科目，**未断言 `businessType==BAD_DEBT_WRITE_OFF` 枚举值**。属次要断言强度缺口（凭证内容已实质覆盖核销语义），非合规缺陷。交 MA4 A4.1 按需评估是否补强 businessType 枚举断言。

- **关联既有结论**：
  - A1.3 §3 测试证据汇总：命题 W1（坏账核销）`TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL:140` **强**——断言 status==WRITTEN_OFF / openAmount==0 / settledAmount==500 / 凭证 借Allowance/贷AR / 无 6701 费用科目。**未断言 businessType==BAD_DEBT_WRITE_OFF 枚举值**（次要，凭证内容已实质覆盖核销语义）。
  - A1.3 §5.1 命题 W1 = **接受**（5 验收标准 L3-L5 全证据一致）。本存疑点不推翻接受结论，只评估断言强度补强必要性。
  - A4.1.6 done（`2026-08-06-0847-3-...-budget-vs-actual-e2e-assertion-strength`）：E2E 断言强度评估范本——本验证同型（单元测试断言强度评估）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md` 状态轴 `use-cases.md:11` WRITTEN_OFF + UC-FIN-08（核销不直接产 GL 凭证）。坏账核销凭证经 owner doc `bad-debt.md §步骤3` 描述（借Allowance/贷AR，不进 P&L）。**L1 未显式要求凭证 businessType 枚举断言**（businessType 是过账引擎路由维度，非 L1 验收标准）。L2（`bad-debt.md §步骤3`）描述凭证科目结构，未规定 businessType 枚举契约。

- **实现现状（L3，实测锚点，本计划起草时核实）**：
  - 凭证 businessType 写入点：`module-finance/erp-fin-service/.../service/processor/ErpFinBadDebtProcessor.java` `executeWriteOff:163-182` → `writeBadDebtVoucher(debt, item, ErpFinBusinessType.BAD_DEBT_WRITE_OFF, "坏账核销", lines):180`（businessType 显式传 `BAD_DEBT_WRITE_OFF`）。
  - 测试断言点：`module-finance/erp-fin-service/src/test/java/.../entity/TestErpFinBadDebt.java` `testWriteOffSetsStatusAndVoucherNoPL:140-174`——断言 approvalStatus / voucherId / status==WRITTEN_OFF / openAmount==0 / settledAmount==500 / 凭证行 借Allowance(DEBIT)/贷AR(CREDIT) / 无 6701；**未断言 voucher.businessType==BAD_DEBT_WRITE_OFF**。
  - businessType 枚举：`ErpFinBusinessType.BAD_DEBT_WRITE_OFF`（坏账核销专用 businessType，过账引擎路由用）。

- **既有证据（复用输入）**：
  - MA2 `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（A2.5c）§2.3 场景 d/e/f 证实坏账核销/收回/反审核红冲闭环强一致（凭证 + ArApItem 对称）。本验证复用其「凭证生成正确」结论，**只补「businessType 枚举断言强度」差异**。
  - A1.3 §3 已评级该测试为「强」（凭证科目/方向/金额/无费用科目断言齐备），仅 businessType 枚举未断言。

- **初步实测（本计划起草时的部分核验，执行时复核）**：
  - grep `TestErpFinBadDebt.java` businessType / BAD_DEBT_WRITE_OFF / setBusinessType / getBusinessType——零断言命中（测试未引用 businessType）。
  - grep 全仓坏账相关测试 businessType 断言——`TestErpFinBadDebtReversal`（反审核测试）亦未断言 businessType（断言 isReversed + 行同向取负 + ArApItem 回退）。
  - 即本验证最可能结论 = **接受（断言强度足够：凭证科目/方向/金额已实质覆盖核销语义；businessType 枚举断言为补强项，非合规缺陷）**，属**确认性断言强度评估**（边际信息量较低）；剩余价值 = 断言强度评级 + 补强 successor 评估（是否登记 P2 测试覆盖补强）。

- **剩余差距**：坏账核销测试是否需补强 `businessType==BAD_DEBT_WRITE_OFF` 枚举断言——A1.3 标注为「次要断言强度缺口」但未定级（接受 vs P2 测试覆盖补强）。本验证补全该评级。

- **保护区域**：只读评估（grep + 读测试 + 读凭证写入点 + 引用 MA2/A1.3），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——若登记 P2 测试覆盖补强 successor，修复（测试代码补断言）经 MR1（纯测试代码修复预授权类目）。

## Goals

- 断言强度评级：对 `TestErpFinBadDebt#testWriteOffSetsStatusAndVoucherNoPL:140` 的凭证断言集（科目/方向/金额/无 6701）评级，确认 businessType 枚举断言缺失是否削弱「坏账核销语义」覆盖。
- 凭证 businessType 写入点证据：`executeWriteOff:180` 显式传 `BAD_DEBT_WRITE_OFF`——证实生产代码正确写入（行为正确），评估测试是否须镜像断言。
- 对齐 UC-FIN-08 + 状态轴 WRITTEN_OFF + `bad-debt.md §步骤3` 给出结论：①若凭证科目/方向/金额断言已实质覆盖核销语义（businessType 枚举为路由维度非 L1 验收标准）→ 接受（断言强度足够）；②若 businessType 枚举断言缺失削弱语义覆盖且属可回归保护点 → P2（测试覆盖补强 successor，非行为缺陷）。
- 产出验证报告 + §8 过程纪律自检；finding/successor（若有）按 §7 裁决登记 arm-index。

## Non-Goals

- **不重新核实命题 W1 坏账核销符合性**（A1.3 §5.1 已判接受；本验证只评 businessType 断言强度差异）。
- **不修改测试代码**（只读评估；补断言经 MR1）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不实施修复**（修复经 MR1；本验证仅登记 finding/successor）。
- **不展开 A1.3 §7-1/§7-3**（A4.1.8/A4.1.10 范围）。

## Task Route

- Type: `verification or audit work`（断言强度评估 + 接受/P2 裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议 + MA4↔A5.6 边界）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.9 行）+ `docs/audits/2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` §7 存疑点 2 + §3 测试证据（输入）+ `docs/design/finance/bad-debt.md §步骤3`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。断言强度评估需多维度归类（凭证写入点 / 测试断言集 / 断言强度评级 / MA4↔A5.6 边界 / 接受-or-P2 裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep + 读测试 + 读凭证写入点 + 引用 MA2/A1.3）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 坏账凭证 businessType 断言强度评级

Status: completed
Targets: `docs/audits/2026-08-07-0944-rc-ma4-a4-1-9-bad-debt-voucher-businesstype-assertion-strength.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.9 行）；A1.3 done（§7 存疑点 2 已落盘 + §3 测试证据 + §5.1 命题 W1 接受）

- [x] `Proof` 凭证 businessType 写入点核验：给出 `executeWriteOff:180` 显式传 `ErpFinBusinessType.BAD_DEBT_WRITE_OFF` 证据（file:line）+ 该 businessType 在过账引擎路由的角色（路由维度，区分核销/收回/反审核凭证类型）。证实生产代码正确写入。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 测试断言集全集核验：grep `TestErpFinBadDebt.java`（+ `TestErpFinBadDebtReversal.java`）全部凭证相关断言（科目/方向/金额/isReversed/行同向取负/status/openAmount/settledAmount），产出断言集清单 + 标注 businessType 枚举断言缺失。引用 A1.3 §3 已有「强」评级依据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（断言强度是否足以覆盖核销语义），与 A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角）边界按此执行（方法论 §去重协议 MA4↔A5.6）。本验证不重做 A5.6 E2E 断言强度审计，只评单元测试 businessType 枚举断言。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 断言强度裁决（方法论 §2 判据 + 三源对照）：①若凭证科目/方向/金额断言已实质覆盖核销语义（businessType 为路由维度非 L1 验收标准，L1/L2 未要求枚举断言）→ 接受（断言强度足够，A1.3 §5.1 命题 W1 接受维持）；②若 businessType 枚举断言缺失削弱语义覆盖且属可回归保护点（如 future 凭证类型混入致误判）→ P2（测试覆盖补强 successor，非行为缺陷）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.3 §5.1 命题 W1 接受结论分层一致。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 凭证 businessType 写入点 + 测试断言集清单落盘（全集，无遗漏），每条有证据（file:line）
- [x] 断言强度裁决有明确结论（接受 / P2 测试覆盖补强 successor），与 A1.3 §5.1 命题 W1 接受结论分层一致

### Phase 2 - finding/successor 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-07-0944-rc-ma4-a4-1-9-bad-debt-voucher-businesstype-assertion-strength.md`（定稿）；`docs/audits/arm-index.md`（若新 finding/successor）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 断言强度评级 + 裁决完成

- [x] `Add` 若定 P2 → 按 §7 grep arm-index finance 坏账/凭证 businessType/测试断言强度同域同控制点裁决「复用 or 新建」`P*-RC-xxx` 或 successor 行，写入 arm-index MA4 分区；双向可追溯注记（finding/successor → MR1）。若接受 → 在报告登记「无新 finding，归 A1.3 §5.1 命题 W1 接受 + §7 存疑点 2 闭合」。禁止未经比对新建。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.3 §5.1 命题 W1 / MA2 A2.5c 坏账强测试 / A5.6 E2E 断言强度边界 的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（写入点 + 断言集 + 裁决 + finding/successor 衔接 + §8 自检齐全）
- [x] 新 finding/successor（若有）已写入 arm-index MA4 分区并有 grep 依据（本验证若维持接受则无写入，本条 N/A 满足）

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02b3c58e0ffeZZFhP1r07Iu1c1，fresh session，未起草本计划）。逐项核验 A-I 全 PASS：Deps（A4.1 done roadmap:126 / A1.3 done roadmap:42）、单结果表面（一份验证报告，Non-Goals 显式排除 §7-1[A4.1.8]/§7-3[A4.1.10] + 不重审命题 W1 全接受）、Baseline 逐项实测命中（ErpFinBadDebtProcessor executeWriteOff:163-182 + writeBadDebtVoucher(...,ErpFinBusinessType.BAD_DEBT_WRITE_OFF,...):180 / TestErpFinBadDebt testWriteOffSetsStatusAndVoucherNoPL:140-174 断言 status/openAmount/settledAmount/科目/方向/无6701 + **businessType 零断言命中**[仅 javadoc:48 描述性引用] / §7-2 存疑点忠实引用 / MA2 A2.5c 报告存在）、只读审计正确（预授权类目，P2→MR1 纯测试代码修复）、反松弛合规、item typing + Skill 记录齐、Closure Gates audit-only 删 build/test 有据、Q4 路由正确（§7-2 明确「次要断言强度缺口非合规缺陷」，无 P0/P1 路径，P2→successor）、MA4↔A5.6 边界正确声明（审「行为是否符合需求」断言强度是否覆盖核销语义，非 A5.6 E2E 断言强度测试质量视角）+ 2 分支决策（accept/P2 测试覆盖补强 successor）正确映射 §2 判据 + 与 A1.3 §5.1 命题 W1 接受分层一致。无阻塞。非阻塞观察：plan:34「测试未引用 businessType」略不精确（javadoc:48 描述性引用），但实质结论「零断言命中」正确，执行时 grep 复核。共识达成，转 active。

## Closure Gates

> 本计划为**只读断言强度评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 凭证写入点 + 断言集完整性 + 裁决 + finding/successor 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.9 验证报告写入点 + 断言集 + 裁决齐全 + finding/successor（若有）登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议（MA4↔A5.6 边界）一致；与 A1.3 §7-2 + §3 测试证据 + §5.1 命题 W1 接受一致
- [x] 已运行验证：写入点 + 断言集完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 坏账凭证 businessType 枚举断言补强（若定 P2 测试覆盖补强）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是断言强度评估，结果表面 = 验证报告 + 裁决 + finding/successor 登记。命题 W1 坏账核销符合性已接受（A1.3 §5.1）；businessType 枚举断言缺失属测试覆盖补强项（非行为缺陷，凭证科目/方向/金额已实质覆盖核销语义）。补断言经 MR1（R1.0→RC-R1.n，纯测试代码修复预授权类目）。若裁决为接受（断言强度足够）则无 successor。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 finding/successor 交叉引用展开；若维持接受则无 successor）

## Closure

Status Note: 计划执行完成。A4.1.9 验证报告（`docs/audits/2026-08-07-0944-rc-ma4-a4-1-9-bad-debt-voucher-businesstype-assertion-strength.md`）落盘：凭证 businessType 写入点核验（`executeWriteOff:180` 显式传 `BAD_DEBT_WRITE_OFF` + `findBillLinks:908-912` reverse 路由依赖）+ 测试断言集全集核验（8 项强断言 + businessType 枚举缺失）+ MA4↔A5.6 边界声明 + 断言强度裁决 = **接受（断言强度足够）**（凭证内容断言实质覆盖核销语义 + businessType 为路由维度非 L1 验收标准 + 经 reverse 测试间接回归保护）+ 零新 finding / 零 successor + A1.3 §7 存疑点 2 正向消解为接受 + A1.3 §5.1 命题 W1 接受维持。只读评估，零生产代码变更，checker 全 19 规则 actual == baseline（0 漂移）。独立结束审计为最终门控（见 §Closure Gates）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（new session，未起草/未执行本计划）
- Audit Mode: 语义审计（SCRIPT_CHECK_RESULT=FAIL → 8 项 Closure Gates 未勾选 + Closure 证据占位符）；本代理按 plan-guide 修正前对 live repo 逐项语义复核
- Verification Walkthrough:
  - 报告落盘核验：`docs/audits/2026-08-07-0944-rc-ma4-a4-1-9-bad-debt-voucher-businesstype-assertion-strength.md` 存在（`> Audit Status: closed`，11 段齐全，零 `*(pending)*` 占位符）
  - 凭证 businessType 写入点（L3 live）：`ErpFinBadDebtProcessor.java#executeWriteOff:180` 确实显式传 `ErpFinBusinessType.BAD_DEBT_WRITE_OFF`；`writeBadDebtVoucher:208-214` 经 `CloseVoucherWriter.writeVoucher` 持久化 `businessType.name()`——与报告 §2.1 一致
  - reverse 路由依赖（L3 live）：`ErpFinPostingProcessor.java#findBillLinks:908-912` 确实按 `(billCode, businessType.name())` 双键查 `ErpFinVoucherBillR`——与报告 §4.2 间接回归保护链一致
  - 测试断言集（L4 live）：`TestErpFinBadDebt.java#testWriteOffSetsStatusAndVoucherNoPL:140-176` 确实断言 approvalStatus/voucherId/status==WRITTEN_OFF/openAmount==0/settledAmount==500/借Allowance(1231,DEBIT)/贷AR(1122,CREDIT)/无6701，**无 businessType 断言**——与报告 §3.1 一致
  - 裁决一致性：报告 §5.1 = 接受（断言强度足够），零 P0/P1/P2，零新 finding，零 successor；与 A1.3 §5.1 命题 W1 接受分层一致；与 docs/logs/2026/08-07.md §A4.1.9 条目一致
  - Anti-Hollow：纯只读评估计划，无生产代码变更（报告 §9 真相源冻结声明），「行为完整性」= 报告实质内容完整（非空壳）
  - Docs sync：`docs/logs/2026/08-07.md` §A4.1.9 条目齐全（工作项 + 类型 + 产出 + 裁决 + §去重）
- Script Re-check: 全部 8 项 Closure Gates 已勾选 `[x]` + Closure 证据占位符已替换为真实审计走查记录；`plan-check.mjs --strict` 复跑 PASS

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
