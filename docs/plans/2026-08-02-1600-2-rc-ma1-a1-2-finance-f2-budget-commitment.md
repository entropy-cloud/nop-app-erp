# 2026-08-02-1600-2 rc-ma1-a1-2-finance-f2-budget-commitment finance-F2 预算与承付需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.2（MA1 需求追踪矩阵审计 — finance-F2 预算与承付）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.2
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.2 的 0.2 依赖）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 同批，先行——过账引擎是 BUDGET/COMMITMENT 凭证生成的基础，其结论影响本切片对"凭证是否生成"的判读）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.2 给出 UC 清单 = `UC-FIN-11/13`（2 UC），含 `use-cases.md:line` 锚点。

- **L1 需求契约（权威真相源）**：`docs/design/finance/use-cases.md`：
  - UC-FIN-11 预算硬拦截（`:204`）：采购订单审核 → 调 `IErpFinBudgetControlBiz.check(科目, 成本中心, 期间, 金额, 来源单)`；预算余量 = 预算(BUDGET 凭证) − 承付(COMMITMENT 凭证) − 实际(NORMAL 凭证)；余量<0 且控制级别==HARD → 返回 BLOCKED → 审核抛异常、订单保持 SUBMITTED；==WARN 写日志放行；==NONE 放行。
  - UC-FIN-13 预算管理(编制/控制/对比)（`:238`）：①预算方案审核通过 → 生成凭证(postingType=BUDGET, businessType=BUDGET_SCENARIO)，借贷按 subject.direction 自动取；②预算控制（采购订单审核强一致校验，同 UC-FIN-11 三通道）；③承付款：采购订单 APPROVED → 生成 COMMITMENT 凭证；订单 CANCELLED 或发票接收 → 红冲 COMMITMENT；④预算对比（报表）：按 (acctSchema, subject, period, costCenter, project, postingType) 分组 VoucherLine 得 Budget/Commitment/Actual 三列，**无需独立预算余额表**。
  - 验收标准要点：三 PostingType（BUDGET/COMMITMENT/NORMAL）并行 + 硬拦截；承付红冲触发条件；三列对比报表数据源 = VoucherLine 聚合（非独立表）。

- **L3 代码实现现状（实测，subagent 探查）**：
  - 预算控制**已实现**：`ErpFinBudgetControlBiz.check():62`（HARD 抛异常 `:88-95` / WARN 放行 / NONE 放行；`applyPostingTypeFilter():151-167` 三通道 BUDGET/COMMITMENT/ACTUAL 拆分）。
  - BUDGET 影子凭证：`BudgetVoucherGenerator.generate():53` / `reverse():79`（审批 → 生成；取消 → 红字冲销）。
  - COMMITMENT 凭证：`CommitmentVoucherGenerator.generateCommitment():61` / `reverseCommitment():77` / `hasUnreversedCommitment():97` / `resolveCommitmentBillType():112`（PO vs SO 派发）；承付门面 `ErpFinBudgetCommitmentBizModel.commit:55 / release:81 / releaseIfPresent:104 / isCommitmentEnabled:117`。
  - 预算方案 Processor：`ErpFinBudgetScenarioProcessor` + 各 mutation Processor（Approve/Cancel/CarryForward/RollForward/SubmitForApproval/Reject）。
  - 跨域承付钩子（业财一体）：`module-purchase/erp-pur-service/.../ErpPurOrderProcessor:188`（预算 check）/`:197`（commit 钩子）；`ErpPurOrderReverseApproveProcessor:42`（取消释放）；`ErpPurInvoiceProcessor:273-296`（发票审核释放 COMMITMENT → ACTUAL）；`ErpPurReturnProcessor:281-295`（退货释放，config-gated `commitment-release-on-return`）。销售侧 SO commit 钩子同型。
  - 预算对比报表：`ErpFinBudgetLineBizModel.getBudgetVsActual():48-108` + XPT `_vfs/nop/main/report/fin/budget-vs-actual.xpt.xml`。
  - **已知注意点 ①（config 默认关闭）**：`isCommitmentEnabled()` 默认 **false**（`ErpFinBudgetCommitmentBizModel:118`）；`isBudgetCheckEnabled()` 默认 **false**（`ErpFinBudgetControlBiz:225-228`）。即开箱默认不启用预算控制/承付——须核实是否与"需求契约要求该行为生效"冲突（§2 判据下默认关闭 + 可配置开启通常属"配置驱动设计"，但须对照验收标准判定是否构成 P1 异常路径/默认行为分歧）。
  - **已知注意点 ②（"实际"口径不一致 + 报表列数）**：`getBudgetVsActual:64-65` 的 "actual" 过滤为 `postingType=BUDGET OR NOT BUDGET`（即把 COMMITMENT 计入 actual），与 `ErpFinBudgetControlBiz.aggregateAmount` 的三通道拆分定义**不一致**；且报表仅出 budget/actual **两列**、**无独立 Commitment 列**（UC-FIN-13 验收标准要求 Budget/Commitment/Actual **三列**对比）——须核实是否影响"三列对比报表"验收标准的正确性（疑似 P1：报表验收标准未满足 / P2）。
  - **已知注意点 ③（承付单行对称）**：`CommitmentVoucherGenerator.writeCommitmentVoucher:154-172` 写**单一资产负债式对称行**（同科目 Dr/Cr 对称），非经典 Dr-准备/Cr-AP-承付；代码注释自述为"客户可配置简化"——须对照需求契约判定是否构成分歧。

- **L4 测试证据现状**：`TestErpFinBudgetEndToEnd`（5 场景：审批→BUDGET 凭证 / HARD 拦截 / WARN 放行+日志 / NONE 放行 / 取消冲销 / `getBudgetVsActual:195`）、`TestErpFinBudgetCommitment`（commit / 取消释放 / 发票审核释放 / 重复释放守卫）、`TestErpFinBudgetIsolation`、`TestErpFinBudgetCarryForward` / `RollForward`、`PropertyErpFinBudgetCommitmentRelease`（jqwik）；跨模块 `TestErpPurOrderCommitment`、`TestErpPurReturnCommitmentRelease`、`TestErpPurBudgetControlIntegration`。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`（期间 + 预算状态机行为）。
  - `docs/audits/2026-07-28-1249-arm-ma2-budget-commitment-release.md`（承付释放机制行为）。
  - `docs/audits/2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md`（预算/AR-AP/成本/期间代码质量）。
  - E2E specs：`tests/e2e/business-actions/fin-budget-scenario.action.spec.ts`、`fin-budget-control.action.spec.ts`、`fin-commitment-accounting.action.spec.ts`、`fin-budget-vs-actual.value.spec.ts`、`fin-budget-rollforward-carryforward.action.spec.ts`、`fin-expense-claim-budget.action.spec.ts`。
  - 本切片须声明与上述 MA2 报告的差异增量（报告段落 9）。

- **保护区域**：只读审计。发现 P0/P1 finding 不在本计划修复——按 §10，P0 经 MR0、P1 经 MR1（R1.0 展开 RC-R1.n）；预算控制/承付属过账派生（生成 BUDGET/COMMITMENT 凭证），触及过账逻辑的修复须 ask-first（§5）。

- **剩余差距**：A1.2 报告缺失 = MA4（A4.1 业财展开器）/ MR1 的该切片证据缺口来源。本计划产出 A1.2 报告并登记 finding。

## Goals

- 产出 A1.2 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-2-finance-f2-budget.md`，含方法论 §6 **9 段全部内容**（UC-FIN-11/13 需求契约原文逐字引用 + 实现证据 `file:line` + 测试证据注明断言强度 + 运行时行为证据复用 MA2/E2E + 五级矩阵 + 每 UC 符合性结论 + arm-index 衔接 + 静态存疑点清单 + 过程纪律自检 + MA2 差异增量声明）。
- 对 2 UC 逐条核验**每条验收标准**（完整枚举，§3）：三 PostingType 并行 + HARD 拦截、承付红冲触发条件、三列对比报表数据源（VoucherLine 聚合非独立表）逐条对照。
- 对已知注意点①②③给出分级结论：config 默认关闭、"actual"口径不一致、承付单行对称——按 §2 判据定级，P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`。

## Non-Goals

- **不修复 finding**（属 MR0 / MR1 R1.0；本计划结果表面 = 报告 + arm-index 登记）。
- **不修改真相源**（product-scope / finance use-cases / owner doc 需求契约段落；§9 冻结——分歧记入报告）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.2 只覆盖 UC-FIN-11/13；承付跨域钩子在 purchase/sales 侧的实现由 A1.15-A1.21 各切片覆盖，本切片只审 finance 侧预算/承付契约）。
- **不执行 MA4 运行时探针展开**（只产存疑点清单供 A4.1）。
- **不重跑既有 MA2 行为审计**（§去重协议：复用已证实行为，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.2 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.2 UC 锚点）+ `docs/design/finance/use-cases.md`（L1）+ `docs/design/finance/budget.md` / `cost-center.md`（L2 设计参考）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap A1.x 指定）。必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主。L5 行为证据默认复用既有 MA2 + E2E recordings（§去重协议）；存疑点即时确认可跑既有 JUnit（`mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinBudget*`）或跨模块承付测试，不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（reporter，恒 0；本审计无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-2-finance-f2-budget.md`（新建，先填 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-FIN-11/13 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:204/238` 验收标准原文；L2 引用 `budget.md`/`cost-center.md` 对应 section（标注"设计参考"）；L3 引用 `ErpFinBudgetControlBiz`/`BudgetVoucherGenerator`/`CommitmentVoucherGenerator`/`ErpFinBudgetCommitmentBizModel`/`ErpFinBudgetLineBizModel` `file:line` + 跨域承付钩子（`ErpPurOrderProcessor`/`ErpPurInvoiceProcessor`/`ErpPurReturnProcessor`）；L4 引用 `TestErpFinBudget*`/`TestErpPur*Commitment` + E2E spec（注明断言强度）；L5 复用 MA2/E2E 已证实行为 + 差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验已知注意点（逐验收标准对照）：①config 默认关闭（`isBudgetCheckEnabled`/`isCommitmentEnabled` 默认 false）是否与"审核即控制"契约冲突；②"actual"口径不一致（`getBudgetVsActual` vs `aggregateAmount`）是否影响三列对比报表正确性；③承付单行对称 vs 经典 Dr-准备/Cr-承付；UC-FIN-11 三通道余量公式（BUDGET−COMMITMENT−NORMAL）实现；UC-FIN-13 承付红冲触发（CANCELLED / 发票接收）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给符合性结论（取最高）：config 默认关闭若构成"默认行为分歧/异常路径"→ P1② 或 P2；"actual"口径不一致若影响报表正确性 → P1④/P2①；承付单行对称若与需求契约冲突 → 按实测定级。每结论列 §2 判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：2 UC 各一矩阵行（L1 逐字、L3 含行号含跨域钩子、L4 注明断言强度、L5 标注 MA2 来源）
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；已知注意点①②③有明确分级（非悬空）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-2-finance-f2-budget.md`（补 §6-§9 定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` finance 预算/承付同域同控制点（如承付释放相关行、P1-MA2-082/083 承付族跨域项等）后裁决复用 or 新建 `P0-RC-xxx`/`P1-RC-xxx`，列明差异依据；禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段（复用/新增裁决 + 双向可追溯）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开；无则注明"无"）。若 Phase 1 定级 P0，按 §10 登记 + 记录"已触发 MR0 追加 R0.n"（本计划不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实跑 `bash docs/audits/nop-compliance-checker.sh` + actual vs baseline 表（无生产代码变更注明"无回归风险"）；closure-audit 独立性声明；交叉去重声明。**不以 checker 退出码 0 作门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 差异增量声明（复用 period-budget / commitment-release MA2 已证实行为，列本切片需求视角差异）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`（新 `P*-RC-xxx` 入分区；既有行追加 RC 交叉引用）。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检（落盘前 §1-§9 全在）。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [ ] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_03e384817ffe7OGeSZy8nfs9AO，fresh session，未起草本计划）。12 项检查 A-L 全 PASS：格式完整、Deps 正确（A1.2 Deps=0.2 done）、单结果表面（UC-FIN-11/13 单切片无合并）、Baseline 准确（check/HARD throw/applyPostingTypeFilter/isBudgetCheckEnabled false/isCommitmentEnabled false/getBudgetVsActual/ErpPur* 钩子行号逐项实测命中；三个 caveat 真实）、UC 覆盖精确、方法论对齐、反松弛合规、Closure Gates audit-only 有据、无范围蔓延、item typing 合规、Skill 就绪、Plan Status=draft。无阻塞。Non-blocking 已吸收：reviewer 指出 `getBudgetVsActual` 除口径不一致外还**仅出两列无 Commitment 列**（UC-FIN-13 要求三列），caveat ② 已据此强化（疑似 P1）。A1.1 作 Related（非 Deps）正确。共识达成，可转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.2 报告 9 段齐全 + 2 UC 逐矩阵行 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.2 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。修复按 §10 经 MR0/MR1 实施；触及预算过账派生（BUDGET/COMMITMENT 凭证生成）的修复须 ask-first + 独立 plan-audit（§5）。
- Successor Required: yes（MR0/MR1 按本报告 finding 展开 R0.n/RC-R1.n）

## Closure

Status Note: <结束审计通过后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理（新会话，cold-context）>
- Evidence: <task id / walkthrough record>

Follow-up:

- 本报告 finding 由 MR0（P0）/ MR1 R1.0（P1）展开；静态存疑点由 A4.1 读取后追加 A4.1.n。
