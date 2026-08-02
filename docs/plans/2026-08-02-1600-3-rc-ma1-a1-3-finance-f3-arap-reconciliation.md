# 2026-08-02-1600-3 rc-ma1-a1-3-finance-f3-arap-reconciliation finance-F3 AR/AP 核销与坏账需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.3（MA1 需求追踪矩阵审计 — finance-F3 AR/AP 核销与坏账）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.3
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.3 的 0.2 依赖）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 同批先行——核销/坏账的可选汇兑损益凭证经核销 PostProcessor 调用过账引擎）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.3 给出 UC 清单 = `UC-FIN-08`（1 UC），含 `use-cases.md:line` 锚点。

- **L1 需求契约（权威真相源）**：`docs/design/finance/use-cases.md`：
  - UC-FIN-08 收款核销发票（`:147`）：收款单核销一张或多张应收发票（部分/全额）。验收标准（逐字）：
    - 收款单.核销(发票1, 发票2, ...) → 生成核销明细（每条: 收款单行 ↔ 发票行, 金额）；
    - 发票.核销状态按累计核销金额计算：累计核销 < 发票金额 → 部分；累计核销 == 发票金额 → 已核销；
    - 往来单位.应收余额 = Σ发票 − Σ核销 − Σ红字。
  - 涉及机制：`ar-ap-reconciliation.md §核销流程/§状态/§余额计算`。状态轴（use-cases:11）：核销状态 `OPEN(未核销)/PARTIAL(部分)/SETTLED(已核销)/CANCELLED(已作废)/WRITTEN_OFF(已坏账核销)`。
  - 注：roadmap A1.3 标题为"AR/AP 核销与坏账"——坏账（WRITTEN_OFF 状态 + 坏账核销/收回/准备）虽无独立 UC 编号，但状态轴含 WRITTEN_OFF 且 `ar-ap-reconciliation.md` 衍生命题覆盖坏账；审计按 L1 UC-FIN-08 逐字验收标准为主，坏账作为状态轴衍生命题一并核查（§3 不抽样：凡 L1 契约触及的控制点逐条核）。

- **L3 代码实现现状（实测，subagent 探查）**：
  - 核销主链**已实现**：`ErpFinReconciliationBizModel`（`create:69 / post:79 / reverse:85 / previewReverse:95 / runAutoReconciliation:126 / checkDualSideConsistency:135`）。
  - 核销过账 Processor：`ErpFinReconciliationPostProcessor.post:21-50`（validateLine → settle/settleWithFx → 状态翻转 → 往来余额刷新；可选汇兑损益凭证，gated by `isReconFxGainLossEnabled`）；`ErpFinReconciliationReverseProcessor`；`ErpFinReconciliationRunAutoReconciliationProcessor:27`（自动核销 FIFO/BY_AMOUNT/BY_RATIO）。
  - 自动核销引擎：`AutoReconciliationEngine:32/167`（三策略）。
  - 结算状态机：`ReconciliationSettler.settle:32 / settleWithFx:58 / reverseSettle:97 / resolveStatus:128`（OPEN/PARTIAL/SETTLED，per-item functional for FX）。
  - 双边一致性：`DualSideConsistencyChecker`；往来余额刷新：`PartnerBalanceUpdater.refresh:33-44`（重算 `ErpMdPartner.receivableBalance/payableBalance`）。
  - 坏账：`ErpFinBadDebtBizModel`（`writeOff:55 / reverseApprove:83 / reverseBadDebtProvision:101`）；`ErpFinBadDebtProcessor`（`executeWriteOff:163` ArApItem→WRITTEN_OFF + BAD_DEBT_WRITE_OFF 凭证 / `executeRecovery:188`）；各 mutation Processor；`BadDebtProvisionCalculator` + `BadDebtProvisionService:295`（排除 SETTLED/WRITTEN_OFF/CANCELLED）。
  - **已知注意点 ①（核销本身不写 GL 凭证）**：`ErpFinReconciliationBizModel:46` javadoc 明示"凭证由收付款审核时生成"——核销 `post` 的唯一 GL 影响是**可选**汇兑损益凭证（`ErpFinReconciliationPostProcessor:37-42`，gated）。须对照 UC-FIN-08 验收标准判定"核销是否应自身产凭证"（契约未要求核销产 GL 凭证，仅要求核销明细 + 状态派生 + 余额更新，故此为设计一致，预计"接受"）。
  - **已知注意点 ②（往来余额是 ErpMdPartner 缓存字段）**：`PartnerBalanceUpdater:22-23` javadoc 自述"机制 B"（master-data 为 test-scope 故直用 DAO）；往来余额非独立余额账表，而是 `ErpMdPartner` 上的缓存字段。须核实是否与"应收余额 = Σ发票 − Σ核销 − Σ红字"恒等式一致（机制上由 refresh 重算保证，预计一致）。另须核实 `sumOpen`（仅排除 SETTLED/CANCELLED，**未显式排除 WRITTEN_OFF**）对 WRITTEN_OFF 的隐式排除依赖 `executeWriteOff` 将 `openAmount` 置零（`:168`）——若有"部分核销后坏账"等边界场景，列为静态存疑点交 MA4。
  - **已知注意点 ③（WRITTEN_OFF 一致排除）**：`WRITTEN_OFF` 是真实状态（`erp-fin/ar-ap-status.dict.yaml:25` / `ErpFinConstants:370`），经 `executeWriteOff:170` 到达；在期末前置检查（`ErpFinAccountingPeriodProcessor:459`）、FX 重估、坏账准备基线中一致排除——须核实排除一致性（预计一致）。

- **L4 测试证据现状**：`TestErpFinReconciliation`（部分/全额核销、跨伙伴拒绝、超额拒绝、日期序拒绝、冲销、状态机守卫）、`TestErpFinAutoReconciliation`（FIFO/BY_AMOUNT/BY_RATIO）、`TestErpFinDualSideConsistency`、`TestErpFinPartnerBalance`、`TestErpFinReconciliationReversePreview`、`TestErpFinBadDebt`（账龄桶准备 + 坏账核销/收回/释放/期末门禁）、`TestErpFinBadDebtReversal`、`TestErpFinBadDebtProvisionReversal`、`TestErpFinAging`、`TestErpFinAuxiliaryReconGate`。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（AR/AP 结算状态机行为）。
  - `docs/audits/2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md`（预算/AR-AP/成本/期间代码质量）。
  - E2E specs：`tests/e2e/business-actions/fin-reconciliation.action.spec.ts`（核销 create/post/reverse + 双边查询 + 5 负向守卫）、`fin-auto-recon.action.spec.ts`、`fin-bad-debt.action.spec.ts`、`fin-bad-debt-reverse-approve.action.spec.ts`、`fin-bad-debt-provision-reverse.action.spec.ts`、`reports/fin-ar-ap-aging.{smoke,value}.spec.ts`。
  - 本切片须声明与上述 MA2 报告的差异增量（报告段落 9）。

- **保护区域**：只读审计。发现 P0/P1 不在本计划修复——按 §10 经 MR0/MR1；坏账核销产 BAD_DEBT_WRITE_OFF/RECOVERY 凭证属过账派生，触及过账逻辑的修复须 ask-first（§5）。

- **剩余差距**：A1.3 报告缺失 = MA4 / MR1 的该切片证据缺口来源。本计划产出 A1.3 报告并登记 finding。

## Goals

- 产出 A1.3 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-3-finance-f3-arap.md`，含方法论 §6 **9 段全部内容**（UC-FIN-08 需求契约原文逐字引用 + 实现证据 `file:line` + 测试证据注明断言强度 + 运行时行为证据复用 MA2/E2E + 五级矩阵 + 符合性结论 + arm-index 衔接 + 静态存疑点清单 + 过程纪律自检 + MA2 差异增量声明）。
- 对 UC-FIN-08 逐条核验**每条验收标准**（完整枚举，§3）：核销明细生成、累计核销金额→部分/已核销状态派生、应收余额恒等式（Σ发票 − Σ核销 − Σ红字）；坏账 WRITTEN_OFF 状态轴衍生命题（核销/收回/准备）一并核查。
- 对已知注意点①②③给出分级结论：核销不自身产 GL 凭证、往来余额缓存字段、WRITTEN_OFF 一致排除——按 §2 判据定级，P0/P1 则新建 `P*-RC-xxx` 并按 §10 触发 MR0/MR1（仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`。

## Non-Goals

- **不修复 finding**（属 MR0 / MR1 R1.0；本计划结果表面 = 报告 + arm-index 登记）。
- **不修改真相源**（product-scope / finance use-cases / owner doc 需求契约段落；§9 冻结——分歧记入报告）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.3 只覆盖 UC-FIN-08 + 坏账状态轴衍生命题；银行对账 UC-FIN-09/14 属 A1.4）。
- **不执行 MA4 运行时探针展开**（只产存疑点清单供 A4.1）。
- **不重跑既有 MA2 行为审计**（§去重协议）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.3 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.3 UC 锚点）+ `docs/design/finance/use-cases.md`（L1）+ `docs/design/finance/ar-ap-reconciliation.md`（L2 设计参考）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap A1.x 指定）。必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主。L5 行为证据默认复用既有 MA2 + E2E recordings（§去重协议）；存疑点即时确认可跑既有 JUnit（`mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinReconciliation,TestErpFinBadDebt*`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（reporter，恒 0；本审计无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-3-finance-f3-arap.md`（新建，先填 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-FIN-08 **一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:147` 三条验收标准原文（核销明细 / 累计核销→部分|已核销 / 应收余额恒等式）；L2 引用 `ar-ap-reconciliation.md §核销流程/§状态/§余额计算`（标注"设计参考"）；L3 引用 `ErpFinReconciliationBizModel`/`ErpFinReconciliationPostProcessor`/`ReconciliationSettler`/`PartnerBalanceUpdater` `file:line` + 坏账 `ErpFinBadDebtProcessor` 调用链；L4 引用 `TestErpFinReconciliation`/`TestErpFinBadDebt*` + E2E spec（注明断言强度）；L5 复用 MA2/E2E 已证实行为 + 差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验已知注意点（逐验收标准对照）：①核销本身不写 GL 凭证（契约是否要求，预计一致→接受）；②往来余额缓存字段与"Σ发票−Σ核销−Σ红字"恒等式是否一致（refresh 重算保证）；③WRITTEN_OFF 在期末门禁/FX 重估/坏账准备基线的一致排除；坏账 WRITTEN_OFF↔OPEN 状态轴派生（核销/收回/准备）覆盖。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据给符合性结论（取最高）：注意点①②③按实测定级（预计多为"接受"/P2，若发现状态派生或恒等式破坏则 P1）。每结论列 §2 判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-FIN-08 一矩阵行（L1 逐字、L3 含行号含坏账调用链、L4 注明断言强度、L5 标注 MA2 来源）+ 坏账状态轴衍生命题核查
- [ ] 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；注意点①②③有明确分级（非悬空）

### Phase 2 - finding 登记 / arm-index 补 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-3-finance-f3-arap.md`（补 §6-§9 定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` finance AR-AP 核销/坏账同域同控制点（如 AR/AP 结算状态机相关行）后裁决复用 or 新建 `P0-RC-xxx`/`P1-RC-xxx`，列明差异依据；禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段（复用/新增裁决 + 双向可追溯）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开；无则注明"无"）。若 Phase 1 定级 P0，按 §10 登记 + 记录"已触发 MR0 追加 R0.n"（本计划不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实跑 `bash docs/audits/nop-compliance-checker.sh` + actual vs baseline 表（无生产代码变更注明"无回归风险"）；closure-audit 独立性声明；交叉去重声明。**不以 checker 退出码 0 作门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 差异增量声明（复用 arap-settlement-state-machine MA2 已证实行为，列本切片需求视角差异）。
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

- Independent draft review iteration 1: `accept`（独立子代理 ses_03e3827b5ffetgpvKteeJqqPBR，fresh session，未起草本计划）。12 项检查 A-L 全 PASS：格式完整、Deps 正确（A1.3 Deps=0.2 done）、单结果表面、Baseline 准确（create/post/reverse/runAutoReconciliation/checkDualSideConsistency 行号逐项实测命中；javadoc:46"凭证由收付款审核时生成"逐字；PostProcessor FX 门控/Settler/PartnerBalanceUpdater/executeWriteOff→WRITTEN_OFF+BAD_DEBT_WRITE_OFF/ar-ap-status.dict:25/AccountingPeriodProcessor:459 排除 全部核实）、UC 覆盖精确、方法论对齐、反松弛合规、Closure Gates audit-only 有据、无范围蔓延、item typing 合规、Skill 就绪、Plan Status=draft。**范围裁决（A1.3 关键问题）**：审计 UC-FIN-08 + 坏账状态轴衍生命题判定为 **(a) 合法完整枚举**——roadmap A1.3 标题即"AR/AP 核销与坏账"、UC-FIN-08 状态轴显式含 WRITTEN_OFF、`ar-ap-reconciliation.md` 衍生坏账命题、坏账无独立 UC 无法单独成切片（§3=功能切片×显式 UC 清单）、跳过 WRITTEN_OFF 反构成"验收标准抽样"反模式。非范围蔓延。无阻塞。Non-blocking 已吸收：reviewer 指出 `sumOpen` 仅排除 SETTLED/CANCELLED 未显式排除 WRITTEN_OFF，隐式依赖 `executeWriteOff` 置零 openAmount（:168），caveat ② 已补此边界为静态存疑点候选。共识达成，可转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.3 报告 9 段齐全 + UC-FIN-08 矩阵行 + 坏账状态轴命题核查 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.3 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。修复按 §10 经 MR0/MR1 实施；触及坏账过账派生（BAD_DEBT_WRITE_OFF/RECOVERY 凭证）或核销汇兑损益凭证的修复须 ask-first + 独立 plan-audit（§5）。
- Successor Required: yes（MR0/MR1 按本报告 finding 展开 R0.n/RC-R1.n）

## Closure

Status Note: <结束审计通过后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理（新会话，cold-context）>
- Evidence: <task id / walkthrough record>

Follow-up:

- 本报告 finding 由 MR0（P0）/ MR1 R1.0（P1）展开；静态存疑点由 A4.1 读取后追加 A4.1.n。
