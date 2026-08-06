# 2026-08-07-1400-1 rc-ma4-a4-1-13-bank-recon-cross-statement-refno-dedup-detection 银行对账跨多条 statement refNo 重复检出率评估

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.1.13（MA4 运行时行为验证 — A1.4 §7-3：UC-FIN-09/14 断言① 跨多条 statement refNo 重复的实际检出，`findStatementIdByAccount:198-207` 仅查最近一条 statement 范围致跨 statement 重复 refNo 漏检，关联 P2-RC-001）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.13；存疑点来源 `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` §7 存疑点 3
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1815-1-rc-ma1-a1-4-finance-f4-bank-reconciliation.md`（A1.4 plan done）+ `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md`（A1.4 报告 §7 存疑点 3 + §5 P2-RC-001 finding + §3 测试覆盖空洞）、`docs/plans/2026-08-06-1044-2-rc-ma4-a4-1-11-bank-recon-counterparty-account-mismatch-rate.md`（A4.1.11 done，同 A1.4 §7-1 触发率评估同型范式）、`docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（MA2 银行对账独立子系统既有行为证据输入）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.13 验证报告（落盘 `docs/audits/2026-08-07-1400-rc-ma4-a4-1-13-bank-recon-cross-statement-refno-dedup-detection.md`）+ 必要时 arm-index finding/successor 登记。**不改代码/ORM/api.xml/真相源**（只读评估：读 `BankStatementImporter.findStatementIdByAccount` 去重范围逻辑 + 读 `assertNoDuplicates` dedup key 决策 + 复用 MA2/A1.4 + 跨多条 statement 漏检触发率运行时影响面评估）。范式对齐 A4.1.11（已 done 的触发率评估同型工作项，同样源自 A1.4 §7 存疑点族）。

- **存疑点原文**（A1.4 报告 §7 存疑点 3，`2026-08-02-1815-...-a1-4-bank-recon.md` §7）：「UC-FIN-09/14 断言① 跨多条 statement refNo 重复的实际检出」——L3 静态确认 `findStatementIdByAccount:198-207` 仅查最近一条 statement（`fundAccountId` 过滤 + `statementDate DESC` + `limit 1`），但「refNo 跨多条 statement 重复 + 漏检致重复入账」的实际影响需运行时构造多 statement 场景验证——交 MA4 A4.1 按需展开。

- **关联既有 finding**：
  - **P2-RC-001**（arm-index `:131`）：UC-FIN-09/14 断言① 导入幂等 dedup key 偏离（refNo 优先回退组合键，非 L1 三元组 `(fundAccount, statementDate, bankTxnCode)`）+ **去重范围仅最近一条 statement**（`findStatementIdByAccount:198-207` `limit 1` statementDate DESC）——L1 要求三元组去重，L3 key 偏离 + 范围收窄。运行时影响：跨多条 statement 的重复 refNo 漏检 → 重复入账（影响对账准确性，但银行 refNo 通常全局唯一跨 statement 重复罕见，且余额恒等式下游兜底 + 手工去重可逆，故非 P0/P1）。修复触及 ORM 加 bankTxnCode 列须 ask-first；或改 `findStatementIdByAccount` 去除 `limit 1` 全量扫描同 account 所有 statement 的 refNo（纯代码修复预授权类目）。**状态：todo（MR1 RC-R1.n 展开待修复）。**
  - 本验证**不重复登记** P2-RC-001（已登记），只评估其跨多条 statement 漏检的实际触发率 + 运行时影响面，确认/调整 P2-RC-001 分级（P2 维持 vs 升 P1 vs 降级）。

- **关联既有结论**：
  - A1.4 §5：UC-FIN-09/14 断言①（导入幂等）= **P2**（P2-RC-001），dedup key 偏离 + 跨多条 statement 范围缺口；主路径（同 statement / 最近一条 statement 范围重复导入拒绝）行为已证实。
  - MA2 A2.5c `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`：银行对账为独立子系统，与 AR/AP 核销解耦。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:165,269` UC-FIN-09/14 断言① 逐字「导入幂等去重 `(fundAccount, statementDate, bankTxnCode)` 三元组，重复报错」——去重范围隐含**同 fundAccount 跨所有 statement**（三元组含 statementDate 但 bankTxnCode 全局唯一语义要求跨 statement 去重）。L2（`bank-reconciliation.md §业务规则 1` + schema 补注 :139-150）记录 key 偏离（refNo 优先，不新增 bankTxnCode 列）但**未经 §4 人工批准**。

- **实现现状（L3，实测锚点，本计划起草时核实）**：
  - dedup key 决策：`BankStatementImporter#assertNoDuplicates:147-178`——refNo 优先（`seenRefNo` + `existsByRefNo:158-162`），缺失回退 `(transactionDate, amount, dcDirection)` 组合键（`seenComposite` + `existsByComposite:163-176`）。
  - 去重范围（本存疑点核心）：`findStatementIdByAccount:198-207`——`fundAccountId` 过滤 + `statementDate DESC` + `limit 1`——**仅查最近一条 statement** 作为跨 statement refNo 去重的存在性查询范围。
  - 影响推导：refNo 跨多条 statement 重复时，若重复出现在非最近一条 statement，`existsByRefNo` 查最近一条 statement 查不到 → 漏检 → 重复入账。

- **既有证据（复用输入）**：
  - MA2 A2.5c：银行对账导入主路径（同 statement refNo 去重）行为已证实。本验证复用其「导入去重主路径正确」结论，**只补「跨多条 statement 范围漏检」差异**。
  - A1.4 §6 P2-RC-001：已静态确认 dedup key 偏离 + 范围收窄至最近一条 statement。

- **初步实测（本计划起草时的部分核验，执行时复核）**：
  - grep `BankStatementImporter.java` `findStatementIdByAccount|limit 1|statementDate`——确认 `limit 1` 语句存在（去重范围收窄）。
  - grep 银行对账导入测试 `TestErpFinBankStatementImport` 全集——A1.4 §3 已确认 6 @Test（refNo/composite/strict/cross-account/happy）覆盖**同 statement / 单 statement** 范围，**跨多条 statement 重复无测试**。
  - 即本验证核心 = 评估「refNo 跨多条 statement 重复 + 漏检致重复入账」场景的实际触发率 + 可逆性兜底，确认 P2-RC-001 分级（P2 维持最可能：银行 refNo 通常全局唯一跨 statement 重复罕见 + 重复入账可经手工删除可逆 + 余额恒等式下游兜底，非活跃数据破坏故非 P0/P1）。

- **剩余差距**：P2-RC-001 跨多条 statement 范围漏检的实际触发率未运行时评估——「refNo 跨 statement 重复」实际数据分布 + 「漏检致重复入账」可逆性兜底有效性 + 是否影响对账准确性。本验证补全该运行时影响面评估。

- **保护区域**：只读评估（读 dedup 范围逻辑 + 读 dedup key 决策 + 引用 MA2/A1.4 + 触发率影响面推理），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**（P2-RC-001 修复触及 ORM 结构变更[加 bankTxnCode 列]须 ask-first，或纯代码修复 `findStatementIdByAccount` 去除 limit 1，归 MR1）。

## Goals

- 去重范围漏检核验：核验 `BankStatementImporter.findStatementIdByAccount:198-207`（`fundAccountId` + `statementDate DESC` + `limit 1`，仅查最近一条 statement）+ `assertNoDuplicates:147-178` dedup key 决策（refNo 优先回退组合键），评估「refNo 跨多条 statement 重复」场景下漏检的实际触发路径与运行时影响面。
- 可逆性兜底核验：漏检致重复入账后，重复 bank statement line 进入对账流程的影响（是否破坏余额恒等式 / 是否可经手工删除/红冲可逆）+ 余额恒等式下游兜底（`BankReconciliationBuilder.generate` 不平衡抛异常）的运行时有效性。
- 对齐 UC-FIN-09/14 断言① + `bank-reconciliation.md §业务规则 1` 给出结论：确认/调整 P2-RC-001 分级——①若 refNo 跨 statement 重复实际触发率极低（银行 refNo 全局唯一）+ 漏检可逆 + 下游兜底有效 → P2 维持（范围收窄仍为合规缺陷但非 P1，A1.4 §5 维持）；②若实际触发率不低且不可逆/无兜底 → 升 P1（触发 MR1 优先修复）；③若触发率仅限理论边缘场景 → 维持 P2 或降级（须列明降级依据）。
- 产出验证报告 + §8 过程纪律自检；finding/successor（若有）按 §7 裁决登记 arm-index（P2-RC-001 已登记，本验证只更新分级注记或确认维持）。

## Non-Goals

- **不修复 P2-RC-001**（dedup key 偏离 + 跨多条 statement 范围缺口——修复触及 ORM 结构变更[加 bankTxnCode 列]须 ask-first + 独立 plan-audit；或纯代码修复 `findStatementIdByAccount` 去除 limit 1，归 MR1）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 UC-FIN-09/14 全部验收标准**（A1.4 §5 已判 P2-RC-001；本验证只评跨多条 statement 漏检触发率差异）。
- **不实施修复**（修复经 MR1；本验证仅登记 finding/successor）。
- **不展开 A1.4 §7-1/§7-2/§7-4**（A4.1.11/A4.1.12 done / A4.1.14 范围）。

## Task Route

- Type: `verification or audit work`（触发率评估 + P2-RC-001 分级确认/调整）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议 + MA4↔A5.6 边界）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.13 行）+ `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` §7 存疑点 3 + §6 P2-RC-001 + §3 测试覆盖空洞（输入）+ `docs/design/finance/bank-reconciliation.md §业务规则 1`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。触发率评估需多维度归类（去重范围逻辑 / dedup key 决策 / 触发率影响面 / 可逆性兜底 / P2 维持-or-升 P1-or-降级 裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读 dedup 范围逻辑 + 读 dedup key 决策 + 引用 MA2/A1.4 + 触发率影响面推理）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 跨多条 statement refNo 重复漏检触发率与影响面评估

Status: completed
Targets: `docs/audits/2026-08-07-1400-rc-ma4-a4-1-13-bank-recon-cross-statement-refno-dedup-detection.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.13 行）；A1.4 done（§7 存疑点 3 已落盘 + §6 P2-RC-001 已登记 + §3 测试覆盖空洞已记录）

- [x] `Proof` 去重范围逻辑核验：给出 `BankStatementImporter.findStatementIdByAccount:198-207` 查询条件（`fundAccountId` 过滤 + `statementDate DESC` + `limit 1`，确认仅查最近一条 statement）+ `assertNoDuplicates:147-178` dedup key 决策（refNo 优先 `existsByRefNo:158-162` + 缺失回退组合键 `existsByComposite:163-176`）证据（file:line）。证实「refNo 跨多条 statement 重复 + 重复在非最近一条 statement」漏检路径成立。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 触发率影响面核验：评估银行 refNo 实际全局唯一性（银行参考号语义——通常每笔交易全局唯一跨 statement 不重复）+ 「refNo 跨多条 statement 重复」的实际数据分布（罕见 vs 常见）。引用 A1.4 §5 P2-RC-001 已确认「银行 refNo 通常全局唯一跨 statement 重复罕见」结论。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 可逆性兜底核验：漏检致重复入账后，重复 bank statement line 进入对账流程的影响——是否破坏余额恒等式（`BankReconciliationBuilder.generate:67-82` 不平衡抛异常）+ 是否可经手工删除/红冲可逆 + 是否被下游 autoMatch/manualMatch 发现（重复行成为无匹配候选 → UNMATCHED/SUSPENSE 可见）。引用 A1.4 §5 P2-RC-001 已确认的可逆 + 下游兜底结论。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` P2-RC-001 分级确认/调整（方法论 §2 判据 + 三源对照）：①若 refNo 跨 statement 重复实际触发率极低 + 漏检可逆 + 下游兜底有效 → P2 维持（范围收窄仍为合规缺陷但非 P1，A1.4 §5 维持）；②若实际触发率不低且不可逆/无兜底 → 升 P1（触发 MR1 优先修复）；③若触发率仅限理论边缘场景 → 维持 P2 或降级。裁决须列明 §2 判据编号 + 与 A1.4 §5 P2-RC-001 P2 结论分层一致 + 与 arm-index `:131` P2-RC-001 行衔接（更新分级注记或确认维持）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 去重范围逻辑 + 触发率影响面 + 可逆性兜底证据落盘，每条有证据（file:line）
- [x] P2-RC-001 分级确认/调整有明确结论（P2 维持 / 升 P1 / 降级），与 A1.4 §5 P2-RC-001 P2 结论分层一致

### Phase 2 - finding/successor 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-07-1400-rc-ma4-a4-1-13-bank-recon-cross-statement-refno-dedup-detection.md`（定稿）；`docs/audits/arm-index.md`（P2-RC-001 分级注记更新）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 触发率评估 + 分级确认完成

- [x] `Add` P2-RC-001 分级注记更新：若 P2 维持 → 在 arm-index `:131` P2-RC-001 行追加「A4.1.13 运行时触发率评估确认 P2 维持」注记（含跨多条 statement 漏检触发率结论 + 可逆性兜底证据 + file:line）；若升 P1 → 在 P2-RC-001 行标注升级 + 触发 MR1 优先修复（纯代码修复预授权类目 `findStatementIdByAccount` 去 limit 1；ORM 加列须 ask-first）；若降级 → 更新分级 + 列明降级依据。禁止未经比对新建重复 finding。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.4 §6 P2-RC-001 / MA2 A2.5c 银行对账解耦 / A4.1.11 P1-RC-004 对方账号触发率[同 A1.4 §7 族不同控制点] 的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（去重范围逻辑 + 触发率 + 可逆兜底 + 分级确认 + finding 衔接 + §8 自检齐全）
- [x] P2-RC-001 分级注记已更新入 arm-index（确认维持/升 P1/降级）并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: accept (mission-driver 2026-08-04-224309 独立子代理 ses_02a71986dffeVqutWtDPchAHlD) — format compliant（模板必需段落齐全、Phase 结构合法、item types 与 Skill 标注合规）；Exit Criteria 可测且覆盖 P2 维持/升 P1/降级三分支裁决；单一结果表面（验证报告 + arm-index 注记）；Non-Goals 清晰排除 §7-1/§7-2/§7-4 兄弟项；只读计划正确删除 build/test 门控并说明理由；Deferred But Adjudicated 覆盖 P2-RC-001 带 MR1 successor。基线锚点经实测核验：`BankStatementImporter.findStatementIdByAccount:198-207`（filter :201 / DESC :203 / setLimit(1) :204）、`assertNoDuplicates:147-178`、`BankReconciliationBuilder.generate:47-82` 平衡守卫、A1.4 §7 存疑点 3 + §6 P2-RC-001、roadmap A4.1.13 行（`:139` todo，Deps A4.1 done 满足）均存在。1 项 minor 已修订：arm-index P2-RC-001 行号 `:130`→`:131`（:130 实为 P1-RC-005）。无 Blocker/Major，promote to active。

## Closure Gates

> 本计划为**只读触发率评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 去重范围逻辑 + dedup key 决策 + 触发率影响面 + 可逆兜底 + 分级确认 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.13 验证报告去重范围逻辑 + 触发率 + 分级确认齐全 + P2-RC-001 分级注记更新入 arm-index
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据一致；与 A1.4 §7-3 + §6 P2-RC-001 + §5 P2 结论一致
- [x] 已运行验证：去重范围逻辑 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P2-RC-001 dedup key 偏离 + 跨多条 statement 范围修复

- Classification: `optimization candidate`（已登记 P2-RC-001，修复归 MR1）
- Why Not Blocking Closure: 本计划是触发率评估，结果表面 = 验证报告 + P2-RC-001 分级确认。P2-RC-001 已登记为 P2，修复（ORM 加 bankTxnCode 列[ask-first] + 或纯代码修复 `findStatementIdByAccount` 去 limit 1 全量扫描）归 MR1（R1.0→RC-R1.n）。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 P2-RC-001 分级确认展开修复；本验证若维持 P2 不触发 MR0 即时通道）

## Closure

Status Note: 执行完成。Phase 1（去重范围逻辑核验 + 触发率影响面评估 + 可逆性兜底核验 + P2-RC-001 分级确认）+ Phase 2（arm-index P2-RC-001 注记更新 + §8 过程纪律自检）均已完成。整体裁决 = 维持 P2-RC-001 = P2（不升 P0/P1，不降级，不触发 MR0，无新 finding）。验证报告定稿于 `docs/audits/2026-08-07-1400-rc-ma4-a4-1-13-bank-recon-cross-statement-refno-dedup-detection.md`；arm-index `:131` P2-RC-001 行已追加 A4.1.13 注记。本计划为只读评估（零生产代码变更），无 build/test 回归风险；§8 checker actual vs baseline 0 漂移。独立结束审计已完成（见下方证据）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，mission-driver 2026-08-04-224309 closure-audit 步骤），无执行者上下文复用。
- Evidence: 独立结束审计 walkthrough（2026-08-07）：(1) Phase 状态/项目一致性 — Phase 1/2 全部 `[x]`，无残留 `- [ ]`；(2) Exit Criteria vs 实仓 — 实测 `BankStatementImporter.java` `findStatementIdByAccount:198-207`（filter :201 / `addOrderField("statementDate", true)` :203 / `setLimit(1)` :204）、`assertNoDuplicates:147-178`（refNo 优先 seenRefNo :153 + existsByRefNo :158；组合键回退 seenComposite :166 + existsByComposite :171）、`existsByRefNo:181-187` / `existsByComposite:189-196` 均经 findStatementIdByAccount 收窄至最近一条 statement，与计划 §Current Baseline + 报告 §1/§2 锚点逐字一致；(3) 反空壳 — 报告 355 行实质分析（去重范围逻辑 / 漏检路径裁决表 / 触发率定性 / 三重可逆兜底 / 恒等式对本失效模式无效精化 / §2 P0/P1 判据三源复核 / 与 A1.4 §5.3 分层对照），无空体/return null/吞异常；arm-index `:131` P2-RC-001 行已落 A4.1.13 注记（grep 命中「【A4.1.13 运行时触发率评估（2026-08-07）确认 P2 维持】」）；(4) 五点一致性 — Plan Status: completed / 两 Phase Status: completed / 全部 Exit Criteria `[x]` / Closure Gates 全部 `[x]`（本审计勾选 closure-audit 独立性门）/ Closure 证据实化，全部一致；(5) Deferred honesty — P2-RC-001 作 MR1 successor（optimization candidate，successor watch-only）正确登记，无范围内的活跃缺陷/契约漂移隐藏在 Deferred；(6) §8 自检 — checker actual vs baseline 0 漂移（R1/R2 全 = A1.4 基线），closure-audit 独立性声明 + arm-index 交叉去重声明齐全，符合方法论 §8（不以 checker 退出码 0 为门控）。审计结论 = approved（无 Blocker/Major），计划可关闭。

Follow-up:

- P2-RC-001 修复归 MR1 successor（优先纯代码方案 `findStatementIdByAccount` 去除 limit 1，预授权类目；ORM 加 bankTxnCode 列须 ask-first）——非阻塞跟进项目，已登记于 arm-index `:131`。
