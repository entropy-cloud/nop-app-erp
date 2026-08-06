# 2026-08-06-1044-3 rc-ma4-a4-1-12-bank-recon-adj-voucher-line-correctness 银行对账调整凭证行级正确性评估

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.12（MA4 运行时行为验证 — A1.4 §7-2：UC-FIN-09/14 断言④ 调整凭证行级 Dr/Cr/科目/金额正确性，`BankReconAdjAcctDocProvider.createFacts` 产出的 2-4 条 VoucherFact 行级无测试断言）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.12；存疑点来源 `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` §7 存疑点 2
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1815-1-rc-ma1-a1-4-finance-f4-bank-recon.md`（A1.4 done，§7 存疑点 2 + §3 测试证据）、`docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（MA2 银行对账既有行为证据输入）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.12 验证报告（落盘 `docs/audits/2026-08-06-1044-rc-ma4-a4-1-12-bank-recon-adj-voucher-line-correctness.md`）+ 必要时 arm-index finding/successor 登记。**不改代码/ORM/api.xml/真相源**（只读评估：读 `BankReconAdjAcctDocProvider.createFacts` 行级生成逻辑 + grep 测试断言 + 复用 MA2/A1.4 + 行级正确性评级）。范式对齐 A4.1.9（已 done 的凭证断言强度评估同型工作项）。

- **存疑点原文**（A1.4 报告 §7 存疑点 2，`2026-08-02-1815-...-a1-4-bank-recon.md` §7）：「UC-FIN-09/14 断言④ 调整凭证行级 Dr/Cr/科目/金额正确性」——L4 仅断言凭证存在性 + billR 计数，`BankReconAdjAcctDocProvider.createFacts:51-70` 产出的 2-4 条 VoucherFact 的行级（Dr bankSubject / Cr adjSubject / 金额 = bankCredit 或 bankDebit）正确性无测试断言——交 MA4 A4.1 按需展开（运行 post 后断言 ErpFinVoucherLine 行级 subjectCode/dcDirection/debitAmount/creditAmount）。

- **关联既有结论**：
  - A1.4 §5：UC-FIN-09/14 断言④（调整凭证生成）= **接受**（BANK_RECON_ADJ 凭证生成路径 L3-L5 一致）。本存疑点不推翻接受结论，只评估行级正确性断言强度。
  - MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`：银行对账为独立子系统，调整凭证生成行为已证实。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:176,288` UC-FIN-09/14 断言④ 逐字「未达账项 → 生成调整凭证(BANK_RECON_ADJ)」。L2（`bank-reconciliation.md §业务规则`）描述调整凭证科目结构（借银行存款/贷调整科目[银行已收] / 借调整科目/贷银行存款[银行已付]）。L1/L2 要求凭证行级 Dr/Cr/科目/金额正确（每对借贷平衡 + 整体平衡），但未显式要求测试须断言每行字段。

- **实现现状（L3，实测锚点，本计划起草时核实）**：
  - 行级生成逻辑：`BankReconAdjAcctDocProvider#createFacts:51-70`——读取 `billData` 中 `BANK_SUBJECT_CODE`(bankSubject):53 / `ADJ_SUBJECT_CODE`(adjSubject):54 / `TOTAL_BANK_CREDIT`(bankCredit):55 / `TOTAL_BANK_DEBIT`(bankDebit):56 → 产出 2-4 条 VoucherFact：
    - bankCredit > 0（银行已收）：`fact(bankSubject, DEBIT, bankCredit, "BANK_RECV"):61` + `fact(adjSubject, CREDIT, bankCredit, "ADJ_BANK_RECV"):62`（借银行存款 / 贷调整科目）
    - bankDebit > 0（银行已付）：`fact(adjSubject, DEBIT, bankDebit, "ADJ_BANK_PAID"):66` + `fact(bankSubject, CREDIT, bankDebit, "BANK_PAID"):67`（借调整科目 / 贷银行存款）
  - 行级正确性（静态推理）：每对借贷同金额（bankCredit 或 bankDebit）→ 借贷平衡；bankCredit 对 + bankDebit 对互不干扰 → 整体平衡。`fact:72-82` 设 subjectCode/dcDirection/amount/accountKey/businessType=BANK_RECON_ADJ。
  - 测试断言点：A1.4 §3 测试证据——L4 仅断言凭证**存在性**（`assertNotNull(voucherId)`）+ billR 计数，**未断言 ErpFinVoucherLine 行级** subjectCode/dcDirection/debitAmount/creditAmount。

- **既有证据（复用输入）**：
  - MA2 A2.5c：调整凭证生成路径已证实。本验证复用其「凭证生成正确」结论，**只补「行级正确性断言强度」差异**。
  - A1.4 §3 已评级该测试为凭证存在性级（未到行级），调整凭证生成 = 接受。

- **初步实测（本计划起草时的部分核验，执行时复核）**：
  - grep `BankReconAdjAcctDocProvider.java` createFacts 逻辑——行级生成对称正确（每对借/贷同科目方向 + 同金额，bankCredit 对 + bankDebit 对互不干扰）。
  - grep 全仓银行对账调整凭证测试 `ErpFinVoucherLine|subjectCode|debitAmount|creditAmount|dcDirection`——A1.4 §3 已确认行级零断言。
  - 即本验证最可能结论 = **接受（行级正确性充分：createFacts 生成逻辑对称正确 + 每对借贷平衡 + 整体平衡；行级断言缺失属测试覆盖补强项非合规缺陷）**或 **P2（测试覆盖补强 successor）**；属**确认性行级正确性评估**（生成逻辑已静态可验证正确，本评估补行级断言强度定级）。

- **剩余差距**：调整凭证行级（Dr/Cr/科目/金额）是否有测试断言——A1.4 标注为「行级断言缺失」但未定级（接受 vs P2 测试覆盖补强）。本验证补全该评级。

- **保护区域**：只读评估（读 createFacts 逻辑 + grep 测试断言 + 引用 MA2/A1.4），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——若登记 P2 测试覆盖补强 successor，修复（测试代码补行级断言）经 MR1（纯测试代码修复预授权类目）。

## Goals

- 行级正确性评级：核验 `BankReconAdjAcctDocProvider#createFacts:51-70` 产出的 VoucherFact 行级（subjectCode/dcDirection/amount）是否正确——每对借贷平衡（bankCredit 对借银行存款/贷调整科目 + bankDebit 对借调整科目/贷银行存款）+ 整体平衡。
- 行级断言强度评估：grep 银行对账调整凭证测试全部断言（凭证存在性 / billR 计数 / 行级），确认行级断言缺失是否削弱「调整凭证行级正确性」覆盖。
- 对齐 UC-FIN-09/14 断言④ + `bank-reconciliation.md §业务规则` 给出结论：①若 createFacts 行级生成逻辑对称正确（每对借贷平衡 + 整体平衡）且行级断言缺失属测试覆盖补强项非合规缺陷 → 接受（行级正确性充分，A1.4 §5 断言④ 接受维持）；②若行级生成逻辑有偏差或断言缺失削弱语义覆盖且属可回归保护点 → P2（测试覆盖补强 successor，非行为缺陷）。
- 产出验证报告 + §8 过程纪律自检；finding/successor（若有）按 §7 裁决登记 arm-index。

## Non-Goals

- **不重新核实 UC-FIN-09/14 断言④ 调整凭证生成符合性**（A1.4 §5 已判接受；本验证只评行级正确性断言强度差异）。
- **不修改测试代码**（只读评估；补行级断言经 MR1）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不实施修复**（修复经 MR1；本验证仅登记 finding/successor）。
- **不展开 A1.4 §7-1/§7-3/§7-4**（A4.1.11/A4.1.13/A4.1.14 范围）。

## Task Route

- Type: `verification or audit work`（行级正确性评估 + 接受/P2 裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议 + MA4↔A5.6 边界）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.12 行）+ `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` §7 存疑点 2 + §3 测试证据（输入）+ `docs/design/finance/bank-reconciliation.md §业务规则`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。行级正确性评估需多维度归类（行级生成逻辑 / 测试断言集 / 行级断言强度评级 / MA4↔A5.6 边界 / 接受-or-P2 裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读 createFacts 逻辑 + grep 测试断言 + 引用 MA2/A1.4）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 调整凭证行级正确性与断言强度评级

Status: planned
Targets: `docs/audits/2026-08-06-1044-rc-ma4-a4-1-12-bank-recon-adj-voucher-line-correctness.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.12 行）；A1.4 done（§7 存疑点 2 已落盘 + §3 测试证据 + §5 断言④ 接受）

- [ ] `Proof` 行级生成逻辑核验：给出 `BankReconAdjAcctDocProvider#createFacts:51-70` 行级生成逻辑（bankCredit 对 借银行存款[DEBIT]/贷调整科目[CREDIT] + bankDebit 对 借调整科目[DEBIT]/贷银行存款[CREDIT]）+ `fact:72-82` 字段设置（subjectCode/dcDirection/amount/accountKey/businessType）证据（file:line）。证实每对借贷平衡（同金额）+ 整体平衡 + 科目方向符合 L2 `bank-reconciliation.md §业务规则`。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 测试断言集全集核验：grep 银行对账调整凭证相关测试（`BankRecon*` / `bank-recon` / `BANK_RECON_ADJ`）全部凭证断言（凭证存在性 / billR 计数 / 行级 subjectCode/dcDirection/debitAmount/creditAmount），产出断言集清单 + 标注行级断言缺失。引用 A1.4 §3 已有评级依据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（行级正确性是否充分），与 A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角）边界按此执行（方法论 §去重协议 MA4↔A5.6）。本验证不重做 A5.6 E2E 断言强度审计，只评单元测试行级断言强度。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 行级正确性裁决（方法论 §2 判据 + 三源对照）：①若 createFacts 行级生成逻辑对称正确（每对借贷平衡 + 整体平衡 + 科目方向符合 L2）且行级断言缺失属测试覆盖补强项非合规缺陷 → 接受（行级正确性充分，A1.4 §5 断言④ 接受维持）；②若行级生成逻辑有偏差或断言缺失削弱语义覆盖且属可回归保护点 → P2（测试覆盖补强 successor，非行为缺陷）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.4 §5 断言④ 接受结论分层一致。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 行级生成逻辑 + 测试断言集清单落盘（全集，无遗漏），每条有证据（file:line）
- [ ] 行级正确性裁决有明确结论（接受 / P2 测试覆盖补强 successor），与 A1.4 §5 断言④ 接受结论分层一致

### Phase 2 - finding/successor 衔接 + §8 自检 + 报告定稿

Status: planned
Targets: `docs/audits/2026-08-06-1044-rc-ma4-a4-1-12-bank-recon-adj-voucher-line-correctness.md`（定稿）；`docs/audits/arm-index.md`（若新 finding/successor）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 行级正确性评级 + 裁决完成

- [ ] `Add` 若定 P2 → 按 §7 grep arm-index finance 银行对账/调整凭证/行级断言同域同控制点裁决「复用 or 新建」`P*-RC-xxx` 或 successor 行，写入 arm-index MA4 分区；双向可追溯注记（finding/successor → MR1）。若接受 → 在报告登记「无新 finding，归 A1.4 §5 断言④ 接受 + §7 存疑点 2 闭合」。禁止未经比对新建。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.4 §5 断言④ 接受 / MA2 A2.5c 银行对账 / A5.6 E2E 断言强度边界 的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（行级生成逻辑 + 断言集 + 裁决 + finding/successor 衔接 + §8 自检齐全）
- [ ] 新 finding/successor（若有）已写入 arm-index MA4 分区并有 grep 依据（本验证若维持接受则无写入，本条 N/A 满足）

## Draft Review Record

- Independent draft review iteration 1: accept (mission-driver 2026-08-04-224309) — format compliant（模板必需段落齐全、Phase 结构合法、item types 与 Skill 标注合规）；Exit Criteria 可测且覆盖全部检查项；单一结果表面、Non-Goals 清晰、无 scope creep；closure evidence 明确（验证报告 + arm-index finding/successor + §8 checker actual-vs-baseline + 独立结束审计）；只读计划正确删除 build/test 门控并说明理由。基线锚点已复核：`BankReconAdjAcctDocProvider#createFacts:51-70` + `fact:72-82` 行级生成逻辑与现状逐字一致（bankCredit 对 借银行存款/贷调整科目 + bankDebit 对 借调整科目/贷银行存款 + businessType=BANK_RECON_ADJ），roadmap A4.1.12 行存在，所有引用 owner docs/methodology/skills/checker/arm-index 落盘。无 Blocker/Major，promote to active。

## Closure Gates

> 本计划为**只读行级正确性评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 行级生成逻辑 + 断言集完整性 + 裁决 + finding/successor 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.1.12 验证报告行级生成逻辑 + 断言集 + 裁决齐全 + finding/successor（若有）登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议（MA4↔A5.6 边界）一致；与 A1.4 §7-2 + §3 测试证据 + §5 断言④ 接受一致
- [ ] 已运行验证：行级生成逻辑 + 断言集完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 调整凭证行级断言补强（若定 P2 测试覆盖补强）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是行级正确性评估，结果表面 = 验证报告 + 裁决 + finding/successor 登记。UC-FIN-09/14 断言④ 调整凭证生成已接受（A1.4 §5）；行级断言缺失属测试覆盖补强项（非行为缺陷，createFacts 生成逻辑对称正确 + 每对借贷平衡）。补行级断言经 MR1（R1.0→RC-R1.n，纯测试代码修复预授权类目）。若裁决为接受（行级正确性充分）则无 successor。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 finding/successor 交叉引用展开；若维持接受则无 successor）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
