# 2026-08-02-1815-1 rc-ma1-a1-4-finance-f4-bank-reconciliation finance-F4 银行对账需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.4（MA1 需求追踪矩阵审计 — finance-F4 银行对账与未达账项）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.4
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.4 的 0.2 依赖）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，同 finance 审计范式）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.4 给出 UC 清单 = `UC-FIN-09/14`（2 UC），含 `use-cases.md:165` / `:269` 锚点，并登记 **D-01 基线分歧**（UC-FIN-09 与 UC-FIN-14 标题重复"银行对账与未达账项"，断言详略不同，roadmap 已裁决同属 A1.4，不直改真相源）。

- **L1 需求契约（权威真相源）**：`docs/design/finance/use-cases.md`：
  - UC-FIN-09 银行对账与未达账项（`:165`）：导入银行对账单（bankTxnCode 幂等去重）；自动勾对（金额 + 反向方向 + valueDate±N 天 + 对方账号 模糊匹配）；余额调节恒等式（银行余额 + 在途 == 账面余额 + 未达，差额 == 0 才可 RECONCILED）；未达账项 → 生成调整凭证（businessType=BANK_RECON_ADJ），下月红冲。
  - UC-FIN-14 银行对账与未达账项（`:269`，更详细断言版）：导入幂等 `(fundAccount, statementDate, bankTxnCode)` 去重，重复导入报错；自动勾对 `(金额, 反向方向, valueDate±N天, 对方账号)` → MATCHED（唯一命中）/ UNMATCHED（多候选）/ SUSPENSE（金额对户名差）；余额调节恒等式差额 == 0 才可 RECONCILED 否则抛异常；未达账项调整凭证 + 下月初自动红冲（跨期还原）；与 AR/AP 核销解耦（ErpFinReconciliation，只确认钱到账/已付，不替代发票核销）。
  - **基线分歧 D-01**（`rc-requirement-baseline-inventory.md §基线分歧登记`）：09/14 标题重复，断言重叠但 14 更详细。审计时按 L1 逐字引用两条验收标准，不合并（§9 冻结）；若断言重叠在报告记录"断言重叠"观察。

- **L3 代码实现现状（实测，subagent 探查）**——功能**已大量实现**（非 stub）：
  - BizModel（薄 facade）+ 6 Processor（R6.1 per-mutation 拆分）+ 5 helper 类位于 `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/bankrecon/`。
  - 导入幂等：`BankStatementImporter.java:44`（importStatement），dedup key = `refNo` 优先、回退 `(transactionDate, amount, dcDirection)`（`:147-178`）；**注意**：实现 dedup key 与 UC-FIN-14 的 `(fundAccount, statementDate, bankTxnCode)` **不同**——ORM 无 `bankTxnCode` 列（`bank-reconciliation.md:147` 记为简化）；且 `findStatementIdByAccount:198-207` 仅查"该 fundAccount 最近一条 statement"做去重范围（limit 1，statementDate DESC），**跨多条 statement 的重复 refNo 不会被检出**（疑似缺口，无测试覆盖）。
  - 自动勾对：`BankStatementMatcher.java:41`（autoMatch，1 候选→MATCHED / 0→UNMATCHED / ≥2→SUSPENSE）；`BankLedgerQuery.java:39`（findCandidates，按 `subjectId + dcDirection + amount + 日期窗口` 过滤）；反向方向映射 `:105-113` ✓；**注意**：UC-FIN-14 要求的"对方账号模糊匹配"**缺失**（无对应 ORM 列，`BankLedgerQuery` 不过滤对方账号，静默缺口，arm-index 无追踪 finding）；日期窗口用 `transactionDate` 而非 spec 的 `valueDate`（ORM 无 valueDate 列，简化）。
  - 余额调节恒等式：`BankReconciliationBuilder.java:47`（generate），实现**简化 2 项恒等式** `diff = (statementBalance − bookBalance) − (bankCreditUnrecorded − bankDebitUnrecorded)`（`:71-82`），`|diff| > precision` 抛 `ERR_BANK_RECON_NOT_BALANCED`；spec 为 4 项恒等式（inTransit=0 为 Non-Goal，`bank-reconciliation.md:149`）；期间 CLOSED 守卫 `:191-201`。
  - 未达调整凭证：`BankReconAdjustmentVoucherBuilder.java:58`（post，businessType=BANK_RECON_ADJ，`exchangeRate=BigDecimal.ONE` 硬编码 `:86`——**多币种未处理，spec 规则 10 未实现，静默缺口**）；`reverse:97-102`（手动红冲）；`BankReconAdjAcctDocProvider.java:51`（createFacts）。
  - **注意（疑似缺口）**：UC-FIN-14"下月初自动红冲"——config key `erp-fin.bank-recon-auto-reverse-next-month` 定义于 `ErpFinConstants.java:289` 但**无 scheduler/cron 消费**，仅手动 `reverse()` 存在（静默缺口，无测试）。
  - 与 AR/AP 解耦：独立实体/BizModel/表，由 MA2 报告 `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365` 证实。

- **L4 测试证据现状**：4 测试类（19 @Test）于 `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/bankrecon/`：`TestErpFinBankStatementImport`（6，含 refNo 去重/非 BANK 账户拒绝/strict-refno/composite-key/跨账号允许）、`TestErpFinBankStatementMatch`（7，唯一→MATCHED/多→SUSPENSE/无→UNMATCHED/同向拒绝/manualMatch/occupied 排除）、`TestErpFinBankReconciliation`（5，平衡/不平衡拒绝/无未达不产凭证/post 产 BANK_RECON_ADJ + reverse/期间 CLOSED 拒绝）、`TestErpFinBankReconciliationEndToEnd`（1，全链 import→match→generate→post→reverse）。E2E：`tests/e2e/business-actions/fin-bank-recon.action.spec.ts`（282 行，3 case）。MA5 `2026-07-29-1430-arm-ma5-finance-test-coverage.md:74,92` 评级"✅ 深 / ✅ 匹配"。**注意**：调整凭证测试仅断言凭证存在性 + `ErpFinVoucherBillR` 计数（`:131,161`），**未断言行级 Dr/Cr/科目/金额**；跨多条 statement 的去重、对方账号匹配、valueDate 窗口、多币种调整、下月自动红冲均**无测试**。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **无专用 MA2/MA4 银行对账审计报告**——银行对账仅作为旁证出现在其他主题报告中。
  - `docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`（声明银行对账为独立子系统/Non-Goal，解耦已证实）。
  - `docs/audits/2026-07-28-1510-arm-ma3-design-completeness-scan.md:72,195`（确认 `bank-reconciliation.md` 存在）。
  - `docs/audits/2026-07-29-0430-arm-ma4-finance-mfg-view-xml-drift.md:97,116`（P2-MA4-014 badge 漂移波及 BankReconciliation/BankStatement）。
  - `docs/audits/2026-07-29-1430-arm-ma5-finance-test-coverage.md:74,92`（测试覆盖证据）。
  - **陈旧/失实证据**：`docs/audits/2026-07-06-use-case-implementation-audit.md:116,121` 将 UC-FIN-09/14 标 ✅，引用 **`AutoReconJob` 类**——该类**不存在**（全仓仅 AR/AP 的 `AutoReconciliationEngine`，不同子系统）。该"✅"部分建立在失实引用上，本审计须重新核验。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实的解耦行为，只补"需求契约↔行为"差异（对方账号匹配缺失/下月自动红冲缺失/多币种调整缺失/调整凭证行级断言缺失等）。

- **arm-index 既有 finding 衔接**：`P1-MA3-028`（bank-stmt-status 字典文档自相矛盾，done R2.3）、`P2-MA3-024`（余额调节恒等式简化未记录，watch-only）、`P2-MA4-014`（badge 漂移，watch-only）、`P1-MA7-001`（ErpFinVoucherBillR 缺索引，done R3.6）。**本切片新发现的静默缺口**（对方账号匹配缺失/下月自动红冲缺失/多币种调整缺失/跨多条 statement 去重范围/调整凭证行级断言缺失）须按 §7 grep 比对后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及会计过账逻辑（如 BANK_RECON_ADJ 凭证生成/红冲/多币种）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.4 切片的五级追踪审计报告缺失 = MA4（A4.1 业财展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.4 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.4 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-4-finance-f4-bank-recon.md`，含方法论 §6 **9 段全部内容**：①UC-FIN-09/14 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，调用链列全）③测试证据（注明断言强度）④运行时行为证据（复用 MA2/E2E，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2 报告差异增量声明。
- 对 2 UC 逐条核验**每条验收标准**（完整枚举，§3）：禁止 UC 跳号、禁止验收标准抽样、禁止跨 UC 合并行；UC-FIN-09/14 各一矩阵行（D-01：断言重叠在报告记录，不合并）。
- 对候选缺口给出分级结论：对方账号匹配缺失、下月自动红冲缺失、多币种调整（exchangeRate=ONE）、跨多条 statement 去重范围、调整凭证行级断言缺失、dedup key 偏离、valueDate→transactionDate 简化、AutoReconJob 失实引用——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / finance use-cases / owner doc 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.1-A1.3 done；A1.5-A1.51 各自独立 plan；A1.4 只覆盖 UC-FIN-09/14）。
- **不执行 MA4 运行时探针展开**（A4.1 展开器读取本报告静态存疑点清单后追加 A4.1.n 实体行；本计划只产出存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：MA2 已证实行为直接引用，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.4 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.4 UC 锚点 + D-01 基线分歧）+ `docs/design/finance/use-cases.md`（L1 真相源）+ `docs/design/finance/bank-reconciliation.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA3/MA4/MA5 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用既有 MA2 报告 + E2E recordings（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinBank*`）或读 E2E 录制，不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-4-finance-f4-bank-recon.md`（新建，先填 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [ ] `Proof` 对 UC-FIN-09/14 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:165/:269` 验收标准原文（禁止转述）；L2 引用 `bank-reconciliation.md` 对应 section（标注"设计参考，冲突以 L1 为准"，注意 `:139-150` schema 补注记录的实现偏离）；L3 引用 `module-finance/erp-fin-service/.../bankrecon/<file>:line`（含 `BankStatementImporter`/`BankStatementMatcher`/`BankLedgerQuery`/`BankReconciliationBuilder`/`BankReconAdjustmentVoucherBuilder`/`BankReconAdjAcctDocProvider` 调用链列全）；L4 引用 `Test*.java#method` / E2E spec（注明断言强度，引用 MA5 评级）；L5 复用 MA2/E2E 已证实行为 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①导入幂等 dedup key 偏离（实现 `refNo/(txnDate,amount,dc)` vs spec `(fundAccount,statementDate,bankTxnCode)`，ORM 无 bankTxnCode 列）；②`findStatementIdByAccount:198-207` 去重范围仅最近一条 statement（跨多条 statement 重复不检出）；③自动勾对"对方账号模糊匹配"缺失（无 ORM 列，`BankLedgerQuery` 不过滤）；④日期窗口用 `transactionDate` 而非 `valueDate`（ORM 无 valueDate 列）；⑤余额调节恒等式简化 2 项 vs spec 4 项（inTransit=0 Non-Goal）；⑥下月自动红冲：config key 定义无消费（仅手动 reverse）；⑦多币种调整：`exchangeRate=ONE` 硬编码（spec 规则 10 未实现）；⑧与 AR/AP 解耦（已证实）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受，取最高）：候选缺口③④⑥⑦属"功能缺失/会计正确性"——若确认为 P0/P1 则定级并触发 §10（本计划仅登记）；⑤已有 P2-MA3-024 追踪（裁决复用 or 增量）；AutoReconJob 失实引用（2026-07-06 审计）记录为报告校正项。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-FIN-09/14 各一矩阵行（D-01 断言重叠在报告记录，不合并），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 MA2 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口①-⑧有明确分级（非悬空"待查"）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-4-finance-f4-bank-recon.md`（补 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [ ] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` finance 银行对账同域同控制点（如 P2-MA3-024 恒等式简化、P1-MA3-028 字典矛盾、P1-MA7-001 索引等行）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：声明复用 `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（解耦）等已证实行为，列明本切片只补的需求视角差异（对方账号匹配缺失 / 下月自动红冲缺失 / 多币种调整缺失 / AutoReconJob 失实引用校正等）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [ ] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.1 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_03e04731dffeV7oMUaUMsybova，fresh session，未起草本计划）。10 项检查 A-J 全 PASS：格式完整、Deps 正确（A1.4 Deps=0.2 done）、单结果表面（A1.4 报告，无跨切片合并，§3 合规）、Baseline 准确（逐项实测命中：BankStatementImporter dedup key + findStatementIdByAccount 仅最近一条 statement 范围 / BankLedgerQuery 无对方账号过滤 / BankReconciliationBuilder 2 项恒等式 / BankReconAdjustmentVoucherBuilder:86 exchangeRate=ONE / config key 无消费 / ORM 无 bankTxnCode/valueDate 列 / 4 测试类 19 @Test 调整凭证测试无行级断言 / AutoReconJob 类不存在），UC 覆盖 UC-FIN-09/14 精确（D-01 断言重叠在报告记录不合并），方法论 §1-§10 + §去重对齐，反松弛合规，Closure Gates audit-only 删除 build/test 有据（§8 reporter-not-gate），无范围蔓延（finding→MR0/MR1，真相源不动），item typing 合规，Skill 就绪。无阻塞。Non-blocking（已评估，无需修订）：①"5 helper 类"措辞——`bankrecon/` 实有 6 .java 文件，第 6 个 `BankReconAdjAcctDocDocProvider` 属业财过账 Provider 类别，已在 L3 全部显式命名，分类可辩护；②MA2 L5 行内锚点 `:48,223,365` 未逐字核验（低关键度引用，报告主题正确）。共识达成，可转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。（§8 含 checker 实测记录，但 checker 是 reporter 非门控；门控真值在 CI workflow。）

- [ ] 范围内行为完成：A1.4 报告 9 段齐全 + 2 UC 逐矩阵行 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.4 锚点 + D-01 一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及会计过账逻辑（如 BANK_RECON_ADJ 凭证生成/红冲/多币种）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）
