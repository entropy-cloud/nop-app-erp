# 2026-08-06-0847-3 rc-ma4-a4-1-6-budget-vs-actual-e2e-assertion-strength 预算对比报表 E2E commitment 列断言强度评估

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.6（MA4 运行时行为验证 — A1.2 §7-3：UC-FIN-13 断言④ `fin-budget-vs-actual.value.spec.ts` 是否断言 commitment 独立列，E2E 断言强度评估）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.6；存疑点来源 `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 3
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1600-2-rc-ma1-a1-2-finance-f2-budget-commitment.md`（A1.2 done，§5.3 P1-RC-003 + §7 存疑点 3）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.6 验证报告（落盘 `docs/audits/2026-08-06-0847-rc-ma4-a4-1-6-budget-vs-actual-e2e-assertion-strength.md`）+ 必要时 arm-index finding 登记。**不改代码/ORM/api.xml/真相源**（只读评估：读 E2E spec + 单测 + 复用 MA2/A1.2；运行时维度 = E2E 实际查询字段 + 断言 + seed 数据覆盖面）。

- **存疑点原文**（A1.2 报告 §7 存疑点 3，`2026-08-02-1700-...-a1-2-budget.md:307`）：「UC-FIN-13 断言④ E2E `fin-budget-vs-actual.value.spec.ts` 是否断言 commitment 独立列」——L4 静态确认单测 `testGetBudgetVsActual` 仅断言两列且不 seed commitment。E2E spec 的断言强度属运行时确认（A5.6 评级视角）——交 MA4 A4.1 按需展开（与 P1-RC-003 修复协同：修复后 E2E 须同步补 commitment 列断言）。

- **关联既有 finding**（P1-RC-003，A1.2 §5.3）：UC-FIN-13 断言④「预算对比报表按 postingType 分组得 Budget/Commitment/Actual **三列**」**未满足**——实现 `ErpFinBudgetLineBizModel.getBudgetVsActual:48-108` 仅产出 budget/actual/available **两列**（DTO `BudgetVsActualRow` 无 `commitmentAmount` 字段），voucher 过滤把 COMMITMENT 计入 actual。A1.2 §3 测试证据记录单测 `testGetBudgetVsActual:195-219` **仅断言两列行为且不 seed commitment**，三列需求**零覆盖**。本验证即**运行时核实 E2E spec 的断言强度**——确认 E2E 与单测是否同步偏离 L1，以及修复 P1-RC-003 时 E2E 须补什么断言。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:261-262` UC-FIN-13 断言④——逐字「按 (acctSchema, subject, period, costCenter, project, postingType) 分组 VoucherLine 得到 Budget/Commitment/Actual **三列**, 无需独立预算余额表」。

- **初步实测（本计划起草时已完成的关键普查，执行时复核）**：
  - **E2E spec**：`tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts`（200 行，plan 2026-07-14-1218-2 Phase 2 产物）。
    - 查询字段（:135）：`ErpFinBudgetLine__getBudgetVsActual(...){ subjectId subjectCode subjectName budgetAmount actualAmount availableAmount }`——**仅查三字段，无 commitmentAmount**（与 DTO 无该字段一致）。
    - seed 数据（setupFull :74-131）：建 partner+employee+budget scenario(NONE)+budget line+ExpenseClaim+claim line。**approve budget scenario（BUDGET 凭证）+ approve ExpenseClaim（NORMAL 凭证）**——**无承付凭证 seed（无 PO commit / 无 COMMITMENT 凭证）**。
    - 断言（:172-195）：budgetAmount=1000 / actualAmount=0→200→0（红冲回退）/ availableAmount=1000→800→1000——**仅两列增量断言，零 commitment 验证**。
    - controlLevel=NONE（:95）——避开预算控制，纯报表数值断言。
  - **单测**（A1.2 §3 引用）：`TestErpFinBudgetEndToEnd.java#testGetBudgetVsActual:195-219`——同样仅断言两列 + 不 seed commitment（与 E2E 同步偏离 L1）。
  - **对照：控制引擎强断言**（A1.2 §3）：`testAvailableDeductsCommitmentSeparately:222-249`——**强断言三通道** available=1000−300−200=500，seed 了 COMMITMENT 凭证（commitment 通道=200）。即**控制引擎有 commitment 覆盖，报表没有**。

- **既有证据（复用输入）**：
  - A1.2 §3 已评级单测断言强度 = **强（断言两列行为，非需求三列行为）** + 三列需求零覆盖。
  - A5.6（audit-remediation）E2E 断言强度评级可引用（方法论 §去重协议 MA4↔A5.6 边界：本验证只审"行为是否符合需求"——E2E 是否验证了 L1 要求的三列——不重做 A5.6 测试质量维度全量评级）。

- **剩余差距**：E2E spec 的断言强度须运行时确认——A1.2 baseline 已读 spec 但未从"E2E 是否可作为 P1-RC-003 修复回归门控"视角评估。本验证补全：(1) E2E 与单测同步偏离确认；(2) P1-RC-003 修复后 E2E 须补的断言清单（commitmentAmount 字段 + COMMITMENT 凭证 seed + 三列增量断言）。

- **保护区域**：只读评估（读 spec + 单测 + 引用 A1.2），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——P1-RC-003 修复（含 E2E 补 commitment 断言）经 MR1（代码逻辑修复预授权类目）。

## Goals

- 运行时确认 E2E spec `fin-budget-vs-actual.value.spec.ts` 的断言强度：① 查询字段集（是否含 commitmentAmount）② seed 数据是否覆盖承付凭证（COMMITMENT postingType）③ 断言是否验证 L1 要求的三列独立 ④ 与单测 `testGetBudgetVsActual` 是否同步偏离 L1。
- 产出 P1-RC-003 修复的 E2E 回归门控清单：修复后须补的 E2E 变更（commitmentAmount 字段查询 + COMMITMENT 凭证 seed + 三列增量断言 + available=budget−actual−commitment 公式验证）。
- 裁决是否新建 E2E 断言强度 finding：若 E2E 与单测同步偏离且无独立新控制点（归 P1-RC-003 修复范围）→ 不新建，仅在 P1-RC-003 修复记录中登记 E2E 补断言义务；若有独立新控制点 → 按 §7 新建。
- 与 A5.6 边界声明（方法论 §去重协议）。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修改 E2E spec / 代码 / ORM**（只读评估）。
- **不实施 P1-RC-003 修复**（经 MR1；本验证只产出修复须补的 E2E 断言清单，不执行修复）。
- **不重做 A5.6 全量 E2E 断言强度评级**（方法论 §去重协议边界：本验证只审"是否符合 L1 三列需求"，不重做测试质量维度全量评级）。
- **不修改真相源**（§9 冻结）。

## Task Route

- Type: `verification or audit work`（E2E 断言强度运行时评估 + P1-RC-003 修复回归门控清单产出）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议 MA4↔A5.6）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.6 行）+ `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 3 + §5.3 P1-RC-003 + §3 测试证据（输入）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。E2E 断言强度评估需多维度归类（查询字段 / seed 覆盖 / 断言语义 / 与单测同步性 / 修复回归门控清单）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读 spec + 单测 + 引用 A1.2/A5.6）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - E2E spec 断言强度运行时评估 + P1-RC-003 修复回归门控清单

Status: completed
Targets: `docs/audits/2026-08-06-0847-rc-ma4-a4-1-6-budget-vs-actual-e2e-assertion-strength.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done；A1.2 done（§5.3 P1-RC-003 + §7 存疑点 3 + §3 测试证据已落盘）

- [x] `Proof` E2E spec 断言强度四维核验：①查询字段集（`getBudgetVsActual` GraphQL selection 是否含 commitmentAmount）②seed 数据覆盖（是否 seed COMMITMENT 凭证——PO commit 或直接 seed postingType=COMMITMENT voucher line）③断言语义（是否断言 commitment 独立列 + actual 不含 commitment + available=budget−actual−commitment）④与单测 `testGetBudgetVsActual` 同步性（两者是否同偏离 L1）。逐项给文件:行证据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 与控制引擎强断言对照：`testAvailableDeductsCommitmentSeparately:222-249` seed 了 COMMITMENT 凭证且强断言三通道——确认"控制引擎有 commitment 覆盖，报表(E2E+单测)没有"的不对称（A1.2 §3 已记录，本验证复核 + 给 E2E 侧的差距量化）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` P1-RC-003 修复回归门控清单产出：P1-RC-003 修复（DTO 增 commitmentAmount + getBudgetVsActual 三通道 + available 公式）后，E2E spec 须补：①GraphQL selection 增 commitmentAmount ②seed COMMITMENT 凭证（PO commit 路径或直接 seed）③增三列增量断言（commitmentAmount=200 + actual 不含 commitment + available=budget−actual−commitment）。该清单作为 P1-RC-003 MR1 修复行的 E2E 回归义务登记。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` finding 裁决（方法论 §7）：若 E2E 断言缺口归 P1-RC-003 修复范围（无独立新控制点）→ 不新建 finding，仅在 P1-RC-003 arm-index 行登记 E2E 补断言义务；若有独立新控制点（如 E2E 缺 seed 导致 actual 口径也无法运行时验证）→ 按 §7 grep arm-index 裁决新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] E2E spec 断言强度四维核验落盘（每维有文件:行证据），与单测同步性确认
- [x] P1-RC-003 修复回归门控清单落盘（commitmentAmount 查询 + seed + 断言三项）

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-06-0847-rc-ma4-a4-1-6-budget-vs-actual-e2e-assertion-strength.md`（定稿）；`docs/audits/arm-index.md`（P1-RC-003 行回填 E2E 义务 / 若新 finding）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 四维核验 + 门控清单完成

- [x] `Add` 若裁决不新建 → 在 arm-index P1-RC-003 行回填"E2E 补断言义务（A4.1.6 门控清单）"。若新建 → 按 §7 grep arm-index 裁决后写入 MA4 分区。finding/义务 → MR1 P1-RC-003 修复行双向可追溯。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 P1-RC-003 / P1-MA2-084 的复用关系 + A5.6 边界声明）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（四维核验 + 门控清单 + finding/义务登记 + §8 自检齐全 + A5.6 边界声明）
- [x] 新 finding（若有）已写入 arm-index MA4 分区；P1-RC-003 行已回填 E2E 补断言义务

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02b72ee4bffeVpCnqilamMilxN，fresh session，未起草本计划）。逐项核验全 PASS：Deps（A4.1 done ✅ roadmap:126 / A1.2 done roadmap:41 / A4.1.6 = A1.2 §7-3 todo roadmap:132 实测命中）。E2E spec anti-hollow claim 逐项实测命中——`tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts:135` GraphQL selection 仅 `budgetAmount actualAmount availableAmount` 无 commitmentAmount；setupFull :74-131 无 COMMITMENT 凭证 seed（无 PO commit）；断言 :172-195 仅两列增量零 commitment 验证；controlLevel=NONE :95。DTO `BudgetVsActualRow.java:16-60` 无 commitmentAmount 字段实测。对照单测 `TestErpFinBudgetEndToEnd.java:222-249 testAvailableDeductsCommitmentSeparately` seed `seedCommitmentVoucher` 且断言 available=500 三通道——"控制引擎有 commitment 覆盖，报表没有"不对称确认。L1 use-cases.md:261-262 断言④ 三列要求实测。MA4↔A5.6 边界声明（Goals:43/Non-Goals:50/Task Route/Phase 2 Exit Criteria 全一致，对齐方法论 §去重协议:386-390）。只读评估正确；产出门控清单不执行修复；Closure Gates audit-only 删 build/test/lint 有据；item typing（Phase 1 Proof|Decision + Phase 2 Add|Proof）+ Skill 齐反松弛合规；finding 裁决逻辑健全（E2E 断言缺口 = P1-RC-003 同控制点测试侧镜像 → fold-in 分支正确，"独立→新建"分支保留）。一条非阻塞建议（arm-index:128 P1-RC-003 行已记录单测侧断言缺口，可在 Decision fold-in 分支引用以加强——非必需，当前 §7 推理达同结论）。共识达成，转 active。

## Closure Gates

> 本计划为**只读运行时评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 四维核验完整性 + 门控清单 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.6 验证报告四维核验齐全 + P1-RC-003 修复门控清单 + finding/义务登记
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §7 衔接 + §去重协议 MA4↔A5.6 一致；与 A1.2 §7-3 + §5.3 P1-RC-003 + §3 测试证据一致
- [x] 已运行验证：四维核验完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### E2E commitment 列断言补充（P1-RC-003 修复范围）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是运行时评估，结果表面 = 验证报告 + P1-RC-003 修复门控清单。E2E commitment 列断言补充是 P1-RC-003 修复（MR1 R1.0→RC-R1.n）的回归义务，随修复落地。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 P1-RC-003 修复行按本报告门控清单补 E2E 断言）

## Closure

Status Note: 计划完成。A4.1.6 验证报告（`docs/audits/2026-08-06-0847-rc-ma4-a4-1-6-budget-vs-actual-e2e-assertion-strength.md`）落盘：E2E spec 断言强度四维核验齐全（查询字段无 commitmentAmount / seed 无 COMMITMENT / 断言仅两列 / 与单测同步偏离 L1）+ 控制引擎对照 + P1-RC-003 修复回归门控清单 3 项 + finding 裁决 fold-in P1-RC-003（零新 finding）+ arm-index P1-RC-003 行回填 E2E 补断言义务 + 交叉引用注记 + A1.2 §7 存疑点 3 收口。只读评估，零生产代码变更，checker 0 漂移。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 / fresh session（未执行本计划，未起草本报告；读盘逐项实测复核）
- Verdict: passes closure audit（2026-08-06）
- 复核结论：四维核验齐全（查询字段/seed 覆盖/断言语义/与单测同步性）逐维 file:line 实测命中 + 控制引擎对照 + P1-RC-003 门控清单 3 项 + fold-in 裁决健全（§7 同根因同控制点）+ arm-index P1-RC-003 行已回填 E2E 义务 + 交叉引用注记已追加（arm-index:395）+ 9 段骨架齐全 + MA4↔A5.6 边界声明 + §9 真相源冻结合规（git status 仅 docs .md 变更，零 .java/.orm.xml/.view.xml/真相源改动）。anti-hollow 抽查 10 项 claim 全部 CONFIRMED。无阻塞项。

Follow-up:

- <仅非阻塞跟进项目>
