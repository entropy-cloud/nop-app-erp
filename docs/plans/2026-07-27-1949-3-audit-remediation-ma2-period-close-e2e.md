# 2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e MA2 期末结账端到端审计（A2.3）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A2.3 期末结账端到端（期间+结转+坏账+成本）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.3）
> Related: `docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（A2.1 P2P，其应付辅助账期末未核销项是本审计前置检查输入）；`docs/plans/2026-07-27-1949-2-audit-remediation-ma2-order-to-cash-e2e.md`（A2.2 O2C，其应收辅助账期末未核销项是本审计前置检查输入）；`docs/plans/2026-07-27-1227-1-audit-remediation-ma1-cross-module-dag-audit.md`（P1-MA1-016 finance IDaoProvider 跨域 DAO 查询 reverseDepreciation + P1-MA1-017 owner doc §3.2/§4.4 finance 纯读规则不完整——均直指期末结账跨域编排）；`docs/plans/2026-07-27-1227-2-audit-remediation-ma1-platform-conformance-s-tier.md`（P1-MA1-018 finance enum↔dict 漂移含 PERIOD_CLOSE/EXCHANGE_GAIN_LOSS 业务类型）；`docs/audits/2026-07-05-1400-cross-review-synthesis.md`（C-11 flow-overview:499 分布式事务描述过时）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法）
> Audit: required

## Current Baseline

期末结账是 ERP 财务核算的关键收口环节，也是跨域编排密度最高的链路（finance 触发 inventory/assets/business 域的写）。owner doc `docs/design/finance/period-close.md`（388 行）定义完整流程：前置检查（posted 全过账 / 凭证全审核 / AR-AP 核销提示 / 坏账 allowance 充足性门控 / 折旧已执行 / 成本核算完成）→ 8 步结账（业务过账检查 / 成本核算 / 折旧 / 费用摊销 / 损益结转 / 结账凭证 / 标记期间 / 结账报告）→ 期间状态机（OPEN→CLOSING→CLOSED→CLOSED_FINAL）→ 反结账（红冲结转/折旧/成本凭证 + 解锁业务单据 + 重开期间）→ 年度结转（本年利润→未分配利润 + 辅助账跨年对账门控 + 次年年初余额 populate + 次年 12 期间自动创建）。概念总览见 `flow-overview.md §一 L4` + `§六 数据一致性保障`。

实时仓库已落地的期末结账实现（逐项核实）：

- **核心 BizModel**（`module-finance/erp-fin-service/.../service/entity/`）：`ErpFinAccountingPeriodBizModel`（期间 + `preCheck`/`closePeriod`/`finalizePeriod`/`reverseClose`/`generateNextYearPeriods` Facade mutation）+ `ErpFinAccountingPeriodStatusBizModel`（per-module 关账状态 arStatus/apStatus/invStatus/glStatus/assetStatus）+ `ErpFinBadDebtBizModel`（坏账准备/释放）。
- **月度结账链路**：`closePeriod` 一次性同步编排 AR→AP→INV→AST→GL 模块关账 + 损益结转 + 汇兑重估（owner doc §期末结账向导 步骤映射）。步骤2 存货成本核算经 `closeInvModule` 调 `IErpInvCostingBiz.reclosePeriodCosts`（finance→inventory R，DAG 合法，config-gated `erp-fin.inv-costing-reclose-on-close`）。
- **跨域编排**（`flow-overview.md §六.1` 事务边界 + period-close.md 实现范围注记）：期间结账触发 `IErpAstDepreciationScheduleBiz.executeBatchDepreciation/reverseDepreciation`（finance→assets command 编排）+ `IErpInvCostingBiz.reclosePeriodCosts`（finance→inventory command 编排）。
- **汇兑重估**：`ExchangeRevaluationService` 重估 AR/AP 外币余额 + 银行存款外币余额（config-gated `erp-fin.bank-fx-revaluation-enabled`），差额生成 EXCHANGE_GAIN_LOSS 凭证。
- **年度结转**：12 月 `closePeriod` 增年度分支——辅助账跨年对账门控（config-gated `erp-fin.auxiliary-recon-gate-enabled`）→ 本年利润→未分配利润结转凭证（PROFIT_TO_RETAINED_EARNINGS）→ 次年 `ErpFinGlBalance.yearOpeningDebit/Credit` populate → `generateNextYearPeriods(year+1)`；反结账覆盖年度结转凭证红冲。
- **坏账准备充足性门控**：期末前置检查新增 Allowance 充足性校验（账龄分桶计算必需准备 vs GL Allowance 账面，不足阻止结账，config-gated `erp-fin.bad-debt-allowance-gate-enabled`，详见 `bad-debt.md §期末 allowance 充足性门控`）。
- **E2E / 单测**：`TestErpFinPeriodPreCheck` + `TestErpFinProfitLossClosing` + `TestErpFinPeriodCloseEndToEnd`（`use-case-implementation-audit` UC-FIN-06 已确认）；E2E `fin-period-close-wizard.action.spec.ts`（preCheck→closePeriod→finalizePeriod→reverseClose 全链 + 非法状态守卫）+ `fin-period-close-wizard.visual.spec.ts`。

**已登记的直指期末结账链路的 MA1 / 历史 finding（本审计须复核其运行时行为，部分为 owner doc 文字缺陷待 MR1）**：

- `P1-MA1-016`：`ErpFinAccountingPeriodProcessor.reverseDepreciation`（line ~389）使用 `daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q)` 直接跨域 DAO 查询 assets 实体，违反跨域查询必须经 I*Biz 规则 —— **直接影响期末结账反结账的折旧红冲路径**。MR1 改为 `IErpAstDepreciationScheduleBiz.findList()`。本审计须复核该跨域 DAO 查询是否产生运行时数据正确性问题（应仅治理问题，只读查询语义正确）。
- `P1-MA1-017`：owner doc `data-dependency-matrix.md §3.2/§4.4`「finance 对业务域纯读不回写」规则不完整——未覆盖期末结账期间的跨域 command/request 编排（finance 调 assets/inventory I*Biz mutation）。**本审计的 owner-doc 对齐维度复核此编排的合法性**。
- `P1-MA1-018`：finance `ErpFinBusinessType` enum 名 ↔ dict 漂移 4 项含 `PERIOD_CLOSE(120)↔PERIOD_CLOSING` + `EXCHANGE_GAIN_LOSS(130)↔FX_REVALUATION` —— **代码以 `enum.name()` 持久化，UI dict 下拉值与 DB 存储值不符，筛选查询漏命中**，直接影响期末结账/汇兑重估凭证的查询与对账。MR1 修复。本审计复核该漂移是否导致期末对账（凭证汇总 vs 总账）漏算。
- **C-11**（cross-review-synthesis C-11）：`flow-overview.md:499`「单据审核+凭证生成 = 分布式事务（REQUIRES_NEW）」描述过时（项目为单库 Quarkus，非分布式事务）—— 影响期末结账事务边界理解的准确性。

**但从未做过一次覆盖期末结账全链、按 `multi-dimensional-audit-prompt.md` 维度的系统性业务正确性审计**。已知未核验输入：

- **前置检查完整性**：owner doc 列 6 项前置检查（posted 全过账 / 凭证全审核 / AR-AP 核销提示 / 坏账 allowance 门控 / 折旧已执行 / 成本核算完成），实现的 `preCheck` 是否覆盖全 6 项 + 阻断 vs 提示的分级是否与 owner doc 一致（坏账 shortfall 阻断、AR-AP 未核销仅提示）。
- **模块关账顺序与隔离性**：closePeriod 编排 AR→AP→INV→AST→GL 的顺序依赖；中途某模块关账失败时的事务边界（`flow-overview.md §六.1`：期末结账=单库事务 REQUIRED）与已关账模块的回滚/残留。
- **损益结转正确性**：收入→本年利润（贷）、费用→本年利润（借）的科目分类与结转金额；结转后收入/费用科目余额归零的验证。
- **汇兑重估正确性**：AR/AP/银行存款外币余额按期末汇率重估的差额计算；EXCHANGE_GAIN_LOSS 凭证借贷平衡；与 P1-MA1-018 enum 漂移的交互（重估凭证查询漏命中风险）。
- **反结账完整性**：反结账红冲结转凭证 + 折旧凭证 + 成本凭证 + 解锁业务单据 + 重开期间的 8 步概念模型实现完整性；反结账后期间内单据重新过账的一致性；P1-MA1-016 reverseDepreciation 跨域 DAO 查询的运行时正确性。
- **年度结转正确性**：本年利润→未分配利润结转凭证（本年利润清零）；次年年初余额 populate（`yearOpeningDebit/Credit`）与上年期末余额一致；辅助账跨年对账门控（AR/AP 辅助账合计 vs 总账科目余额）；次年 12 期间自动创建幂等性；反结账覆盖年度结转凭证红冲。
- **期间状态机纪律**：OPEN→CLOSING→CLOSED→CLOSED_FINAL 转移的合法性；CLOSED_FINAL 期间凭证锁定（不可修改）；期间已被下游引用（如已上报税务/次年期间已创建）时反结账阻止。

剩余差距：需要一次系统性多维审计，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（结账后余额不归零 / 年初余额 populate 错误 / 反结账数据残留 / 汇兑重估借贷失衡 / 模块关账顺序致脏数据）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 多维上下文对期末结账全链（前置检查 → 月度模块关账 AR/AP/INV/AST/GL → 损益结转 → 汇兑重估 → 终关；反结账 8 步；年度结转本年利润→未分配利润 + 年初余额 populate + 辅助账对账 + 次年期间）做系统性业务正确性审计，产出审计报告。
- 重点核验 7 个已识别控制点：(1) 前置检查完整性（6 项 + 阻断/提示分级）；(2) 模块关账顺序 AR→AP→INV→AST→GL 与中途失败的事务边界；(3) 损益结转科目分类与余额归零；(4) 汇兑重估差额计算与 EXCHANGE_GAIN_LOSS 凭证借贷平衡；(5) 反结账 8 步完整性（含 P1-MA1-016 reverseDepreciation 运行时正确性复核）；(6) 年度结转（本年利润→未分配利润 + 年初余额 populate 一致性 + 辅助账对账门控 + 次年期间幂等）；(7) 期间状态机纪律（CLOSED_FINAL 锁定 + 反结账阻止条件）。
- 复核已登记 finding 在期末结账运行时的行为影响：P1-MA1-016（reverseDepreciation 跨域 DAO 查询）/ P1-MA1-017（owner doc 跨域编排规则不完整）/ P1-MA1-018（enum↔dict 漂移致期末对账漏算风险）/ C-11（事务描述过时），标注终态（仅治理/文字缺陷 / 产生运行时缺陷升级）。
- scope matrix §2.2 "业财端到端" 行 finance/期间结账 相关列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.3 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.1 P2P / A2.2 O2C 链路本身 — 期末结账的 AR/AP 前置检查依赖 P2P/O2C 生成的辅助账正确性，但本审计只确认前置检查**消费**辅助账数据的正确性，不审计辅助账生成（见 `2026-07-27-1949-1/2`）。
- **不**审计 A2.4 库存核算一致性 — 步骤2 存货成本核算（`IErpInvCostingBiz.reclosePeriodCosts`）的成本核算方法正确性归 A2.4；本审计只确认期末结账**调用**该 command 的编排正确性（触发时机/失败处理/config-gate），不审计 FIFO/移动加权算法。
- **不**审计 A2.5a-c finance 状态机 / A2.16 预算 commitment 释放 — 期间状态机的系统性可达性审查归 A2.5b（预算与期间）；本审计覆盖期末结账链路中期间状态转移的**业务正确性**（含反结账阻止条件），但不做 finance 域状态机系统性审查。预算 commitment 释放路径完整性归 A2.16。
- **不**审计 A4.3 assets 折旧引擎与 Processor 链路 — 步骤3 折旧计提的折旧方法正确性归 A4.3；本审计只确认期末结账**触发** `executeBatchDepreciation` 的编排正确性与门控（auto-depreciation config）。
- **不**审计 A2.17 并发与乐观锁 — 期末结账并发风险（并发结账同一期间）归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 Non-Goal 子项（owner doc 已裁定）：BATCH/INDIVIDUAL/LIFO 计价、费用摊销/待摊费用（模块未落地）、年度报表渲染（nop-report 面）、利润分配明细、多账套/合并报表年度结转、历史年度追溯结转。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1（P1-MA1-016/017/018 已登记待 MR1，本审计复核其运行时影响不重复裁决根因）。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/finance/period-close.md`（期末结账流程/期间状态机/反结账/年度结转/配置项权威）；`docs/design/flow-overview.md`（§一 L4 期末结算层 + §六 数据一致性保障/事务边界）；`docs/design/finance/bad-debt.md`（§期末 allowance 充足性门控）；`docs/design/finance/budget.md`（§结转规则引擎 + commitment 与结转）；`docs/architecture/data-dependency-matrix.md`（§3.2/§4.4 finance 跨域编排规则，P1-MA1-017 待补注）；`docs/design/finance/posting.md`（业务类型映射，P1-MA1-018 enum↔dict 漂移点）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A2.3 指定此 skill，业财端到端多维审计专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：会计/财务（期间状态机/结转凭证/年初余额/反结账红冲）与 ORM 模型是 ask-first **最高级别**保护区域。期末结账直接触及会计保护区域。P0 即时修复若触及 `closePeriod`/`reverseClose`/年度结转/期间状态机/过账 Provider/ORM，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（`project-context.md §AI 阻塞条件`）。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 期末结账全链多维审计

Status: planned
Targets: `module-finance/erp-fin-service/.../service/entity/ErpFinAccountingPeriodBizModel.java`+`ErpFinAccountingPeriodStatusBizModel.java`+`ErpFinBadDebtBizModel.java`；`ErpFinAccountingPeriodProcessor`（reverseDepreciation，P1-MA1-016）；`ExchangeRevaluationService`；损益结转/年度结转服务；finance 过账 Provider（PERIOD_CLOSE/EXCHANGE_GAIN_LOSS/PROFIT_TO_RETAINED_EARNINGS，P1-MA1-018 enum 漂移点）；`docs/design/finance/period-close.md`+`flow-overview.md §一 L4/§六`+`bad-debt.md`+`budget.md §结转`；`TestErpFinPeriodPreCheck`+`TestErpFinProfitLossClosing`+`TestErpFinPeriodCloseEndToEnd`+`fin-period-close-wizard.action.spec.ts`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-016/017/018 已登记待 MR1 供本审计复核运行时影响）；A2.1/A2.2 done（推荐前置——AR/AP 辅助账期末未核销项是本审计前置检查输入，但非硬阻塞：本审计可基于既有辅助账实现独立核验前置检查逻辑）

- [ ] 维度「需求正确性」：对照 `period-close.md` 前置检查 6 项 + 8 步结账 + 反结账 8 步 + 年度结转，确认实现声明的流程与范围不偏离；找「承诺但无证据」的控制点（如「CLOSED_FINAL 期间凭证锁定」是否真正禁止修改）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「owner-doc 对齐」：`period-close.md` 期间状态机（OPEN/CLOSING/CLOSED/CLOSED_FINAL + 反结账）/ 反结账 8 步 / 年度结转步骤映射（向导 §步骤映射）/ `bad-debt.md` allowance 门控 / `budget.md` 结转与 commitment，逐条核对实现是否符合 owner doc；复核 P1-MA1-017（owner doc §3.2/§4.4 finance 跨域 command 编排规则不完整）的合法性——command 编排在 I*Biz 层合法（业务域自管实体的写）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「业务正确性 — 前置检查完整性」：核验 `preCheck` 是否覆盖全 6 项（posted 全过账 / 凭证全审核 / AR-AP 核销提示 / 坏账 allowance 门控 / 折旧已执行 / 成本核算完成）+ 阻断 vs 提示分级与 owner doc 一致（坏账 shortfall 阻断、AR-AP 未核销仅提示）；结构化 `PeriodPreCheckReport` 字段完整性。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「业务正确性 — 模块关账顺序与隔离性」：核验 closePeriod 编排 AR→AP→INV→AST→GL 顺序依赖；中途某模块关账失败时的事务边界（单库事务 REQUIRED）与已关账模块的回滚/残留；per-module status（arStatus/apStatus/invStatus/glStatus/assetStatus）的状态转移正确性。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「业务正确性 — 损益结转」：核验收入→本年利润（贷）、费用→本年利润（借）的科目分类与结转金额；结转后收入/费用科目余额归零的验证路径；结转凭证的借贷平衡。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「业务正确性 — 汇兑重估」：核验 AR/AP/银行存款外币余额按期末汇率重估的差额计算（currentBalance×期末汇率 vs 科目账面本位币聚合）；EXCHANGE_GAIN_LOSS 凭证借贷平衡；复核 P1-MA1-018 enum↔dict 漂移（PERIOD_CLOSE/EXCHANGE_GAIN_LOSS）是否导致期末对账（凭证汇总 vs 总账）漏算——代码以 `enum.name()` 持久化与 UI dict 筛选值不符的运行时影响。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「业务正确性 — 反结账完整性」：核验反结账 8 步概念模型实现完整性（红冲结转凭证 + 折旧凭证 + 成本凭证 + 解锁业务单据 + 重开期间）；反结账后期间内单据重新过账的一致性；**复核 P1-MA1-016 `ErpFinAccountingPeriodProcessor.reverseDepreciation` 跨域 DAO 查询（line ~389）的运行时正确性**——应仅治理问题（只读查询语义正确），不产生数据错误。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「业务正确性 — 年度结转」：核验本年利润→未分配利润结转凭证（本年利润清零，PROFIT_TO_RETAINED_EARNINGS）；次年 `ErpFinGlBalance.yearOpeningDebit/Credit` populate 与上年期末余额一致；辅助账跨年对账门控（AR/AP 辅助账合计 vs 总账科目余额，config-gated）；`generateNextYearPeriods` 幂等性（`erp-fin.period-generate-skip-existing`）；反结账覆盖年度结转凭证红冲 + 次年期间已创建时阻止反结账。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「业务正确性 — 期间状态机纪律」：核验 OPEN→CLOSING→CLOSED→CLOSED_FINAL 转移合法性；CLOSED_FINAL 期间凭证锁定（不可修改）；反结账阻止条件（次年期间已创建 / 已上报税务等下游引用）；高权限+审批门控。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「架构或边界影响」：复核期末结账跨域编排（finance→assets `executeBatchDepreciation/reverseDepreciation` + finance→inventory `reclosePeriodCosts`）的 DAG 合法性与事务隔离（`flow-overview.md §六.1` 单库事务 REQUIRED vs `§八.2` 业财过账 Facade REQUIRES_NEW）；核对 C-11（flow-overview:499 分布式事务描述过时）对事务边界理解的影响。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「验证充分性」：对期末结账 E2E（`TestErpFinPeriodPreCheck`+`TestErpFinProfitLossClosing`+`TestErpFinPeriodCloseEndToEnd`+`fin-period-close-wizard.action.spec.ts`）的每个验收断言，问「如果它假了，我怎么知道？」；核验断言是否覆盖反结账红冲完整性、年初余额 populate 一致性、辅助账对账门控、汇兑重估借贷平衡等关键路径。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「回归风险」：寻找「仅偶然通过狭窄验证」的期末结账代码——如年度结转仅在 12 月单期间测试通过、反结账仅在无次年期间场景验证、汇兑重估仅在单一外币场景验证、模块关账顺序失败回滚仅在第一模块失败验证。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「路由和技能选择正确性」：复核期末结账向导（纯 UI 编排既有 Facade mutation）+ Facade mutation（preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods）+ 跨域 command 编排的任务路由与技能选择是否与工作类型匹配。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「待办或自主权策略漂移」：复核 period-close.md 实现范围注记声明的 Non-Goal 裁定（BATCH/LIFO/费用摊销/年度报表/利润分配/多账套结转/历史追溯）是否在代码中无声扩大或缩窄范围。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 复核已登记 MA1 finding 运行时影响：P1-MA1-016（reverseDepreciation 跨域 DAO，应仅治理）/ P1-MA1-017（owner doc 文字缺陷）/ P1-MA1-018（enum↔dict 漂移致期末对账漏算风险，本审计须确认是否升级为运行时缺陷）/ C-11（事务描述过时）。标注每项终态。
      - Skill: none
- [ ] 产出审计报告 `docs/audits/2026-07-27-1949-arm-ma2-period-close-e2e.md`（含：链路覆盖矩阵、各维度通过/失败裁决、finding 按 P0/P1/P2 分级、MA1 finding 运行时影响复核表 [P1-MA1-016/017/018 + C-11]、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

- [ ] 期末结账全链 7 个已识别控制点（前置检查 / 模块关账顺序与隔离 / 损益结转余额归零 / 汇兑重估借贷平衡 / 反结账完整性 / 年度结转 / 期间状态机纪律）均有通过/失败裁决与证据
- [ ] 每个多维审计维度（至少 7 维 + 项目特定期末结账维度）至少一句裁决（含「本维度无发现」）
- [ ] MA1 finding（P1-MA1-016 / P1-MA1-017 / P1-MA1-018）+ C-11 运行时影响复核结论已记录（含 P1-MA1-018 enum 漂移是否升级为运行时对账缺陷的裁决）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: planned
Targets: 期末结账审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.2
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（结账后余额不归零 / 年初余额 populate 错误 / 反结账数据残留 / 汇兑重估借贷失衡 / 模块关账顺序致脏数据 / 期间状态机非法转移）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo），供 R1.0 展开机制转化为具体修复工作项行。注意：本审计对 P1-MA1-016/017/018 只复核运行时影响不重复登记根因；若发现新 P1（如 P1-MA1-018 enum 漂移升级为对账缺陷）按新 finding ID 登记。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.2 "业财端到端" 行 finance/期间结账 相关列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [ ] arm-index 报告清单 + scope matrix §2.2 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05c913099`，独立 general 子代理，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：3 个 finance BizModel + `ErpFinAccountingPeriodProcessor` + `ExchangeRevaluationService` 全部存在；P1-MA1-016 跨域 DAO 实测精确（`ErpFinAccountingPeriodProcessor.java:381-389 reverseDepreciation` line 389 `daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q)`）；period-close.md 388 行含 8 步结账 + 期间状态机 + 年度结转 + 坏账门控；finding（P1-MA1-016/017/018 + C-11）全部存在；5 个 config-gate 全部真实（ErpFinConstants + javadoc + period-close.md 表）；测试全部存在；零 anti-slack 违规；5 个 Deferred 项均含 Successor Required + 继任工作项；**会计保护区域正确标记**（Infrastructure And Config Prereqs + Closure Gates 均声明 P0 即时修复须人工确认，符合 project-context.md §AI 阻塞条件）；Plan Status 正确保持 draft。Phase 1 Proof-heavy（13/14 ≥80% 阈值，Rule 7）。可选改进：Deferred 触发条件措辞可更具体（非阻塞，保持现状）。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。期末结账触及会计保护区域，P0 即时修复须额外人工确认。

- [ ] 范围内行为完成（A2.3 期末结账全链多维审计报告产出 + arm-index 更新 + scope matrix §2.2 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix、period-close/flow-overview/bad-debt/budget owner doc 结论已反映）
- [ ] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test` 作回归基线确认；若有 P0 即时修复则该修复子切片独立验证 + 人工确认
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 不得降级为 MR）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.4 步骤2 存货成本核算方法正确性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计确认期末结账**调用** `IErpInvCostingBiz.reclosePeriodCosts` 的编排正确性（触发时机/失败处理/config-gate），但 FIFO/移动加权成本核算算法的正确性归 A2.4 库存核算一致性。
- Successor Required: `yes`——A2.4 执行时复核。

### A4.3 步骤3 折旧计提方法正确性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计确认期末结账**触发** `IErpAstDepreciationScheduleBiz.executeBatchDepreciation` 的编排正确性与门控（auto-depreciation config），但折旧方法（直线法/双倍余额递减/工作量法）正确性归 A4.3 assets 折旧引擎审计。
- Successor Required: `yes`——A4.3 执行时复核。

### A2.5b 期间状态机系统性可达性审查

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计覆盖期末结账链路中期间状态转移的业务正确性（含反结账阻止条件），但 finance 域状态机的系统性可达性审查归 A2.5b（预算与期间）。
- Successor Required: `yes`——A2.5b 执行时复核。

### A2.16 预算 commitment 释放路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 预算结转与 commitment 释放路径完整性归 A2.16；本审计只复核期间状态机对预算结转的硬前置（源 Scenario 年度期间 CLOSED）。
- Successor Required: `yes`——A2.16 执行时复核。

### A2.17 并发风险（并发结账同一期间）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点，不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

## Closure

Status Note: _（待执行 + 独立 closure audit 后填充）_

Closure Audit Evidence:

- _（待独立子代理 closure audit 填充）_

Follow-up:

- P1 finding 经 R1.0 展开机制进入 MR1（P1-MA1-016/017/018 运行时影响复核结论回填 arm-index）
- 成本核算交接 A2.4 / 折旧交接 A4.3 / 期间状态机交接 A2.5b / commitment 交接 A2.16 / 并发交接 A2.17
- 若 P0 即时修复注入 fix plan，该 fix plan 独立 closure + 人工确认（会计保护区域）
