# 2026-08-25-0330-2-fix-closeperiod-fx-flush-profit-loss 修复 closePeriod 同事务 FX 凭证未 flush 致损益结转永缺 FX 腿

> Plan Status: active
> Last Reviewed: 2026-08-25
> Source: 开放式审计 `docs/audits/2026-08-24-2233-open-audit-integration-test.md` P1 发现 OA-02
> Related: `docs/plans/2026-08-24-0541-2-b7-c13-c14-finance.md`（C13 落地时以勘误收档本缺陷）、`docs/plans/2026-08-25-0330-1-aggregate-notify-cs-seeds-into-app-init-data.md`（同批计划 1，先行执行以隔离快照重录归因）
> Audit: required

## Current Baseline

- **缺陷机制（实仓核验）**：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java:165-170` `closeGlModule` 内 `exchangeRevaluationService.revalue()` 经 `CloseVoucherWriter.writeVoucher`（voucherDao.saveEntity/lineDao.saveEntity/billRDao.saveEntity，CloseVoucherWriter.java:104/128/136——直接 save **无 flush**）写 FX 重估凭证后，紧接 `profitLossClosingService.close()` 以 `findAllByQuery` DB 直查聚合（ProfitLossClosingService.java:79）——同事务未 flush 的 FX 凭证行对聚合不可见 → PERIOD_CLOSE 凭证永缺 FX 腿。
- **flush 先例在位**：`ErpFinAccountingPeriodClosePeriodProcessor.java:95` closePeriod 编排末尾已有 `facade.orm().flushSession()`——机制模式有先例，缺的是聚合前边界。
- **两套件钉死相反业务语义**：fin-service `TestErpFinProfitLossClosing.testProfitLossClosingIncludesFxGainLoss`（跨 session 已 flush 场景）断言本年利润净额 = 收入 100 + 汇兑收益 50 = **150（含 FX）**；app-erp-all `TestErpC13FinPeriodCloseReverse`（同事务场景）`PL_TOTAL=1130` 断言 PERIOD_CLOSE **不含** FX 腿（含腿应为 1280 = 5001 收入 1130 + 6603 汇兑 150）。两测试对等价业务场景（期间收入 + FX 损益）钉死不同结转总额。
- **反结账→重结账路径同样缺腿**：红字凭证被 isReversed 过滤（正确语义）+ 新 FX 凭证同样同事务未 flush → 该期间损益结转在**任何 closePeriod 路径下**都结不出 FX 腿。
- **勘误文本形成修复阻力**：`docs/design/integration-testing.md:409` 勘误(2) 以「行为差异以实仓为准」收档；C13 快照基线按现行为（缺腿）录制——正确修复落地时 C13 及其快照必红，须同步翻转。
- **未路由**：`docs/bugs/` 无记录；arm-index fin period-close 分区无 flush 时序控制点。
- **owner doc 语义基准在位（含推论标注）**：`docs/design/finance/period-close.md` §步骤5 要求费用类科目结转至本年利润（汇兑损益为费用类）；「结账后汇兑损益科目余额归零」为由此派生的推论（`docs/design/finance/ar-ap-reconciliation.md:297` 重估先于结账的排序佐证）——fin-service 单测注释显式引用该节（P0-MA2-016 回归）。

## Goals

- closePeriod 编排中，损益结转聚合能看到同事务产生的全部期末凭证：`closeGlModule` 在 revalue 写凭证后、`profitLossClosingService.close()` 聚合前 flush session；并复核 AST/INV 模块关账产物（折旧/成本重算凭证，若同型未 flush）对聚合的可见性，一并在聚合边界前 flush。
- 任何 closePeriod 路径（首结/反结账后重结）下 PERIOD_CLOSE 凭证包含 FX 腿；汇兑损益科目结账后净额归零。
- C13 断言与快照同步翻转：`PL_TOTAL` 1130 → 1280，PERIOD_CLOSE 凭证含 6603 汇兑腿；重结账幂等断言金额同步。
- fin-service 既有单测全绿（`TestErpFinProfitLossClosing` 含 FX 语义保持——两套件语义统一为「含 FX」）。
- 缺陷登记 `docs/bugs/`（P1 会计正确性）+ `period-close.md` 显式裁决登记（同事务 FX 腿必含，flush 边界为实现契约）+ `integration-testing.md:409` 勘误(2) 修正为「已修复」。
- 会计保护区域（业财过账正确性）变更经双独立子代理批准（ operative 规则见 `docs/skills/README.md` §保护区域（auto + dual-agent-approval，批准记录落于计划文件）与 `docs/context/ai-autonomy-policy.md`）。

## Non-Goals

- 不重构 `CloseVoucherWriter` 写入策略本身（仅补 flush 边界，写入路径零行为变更）。
- 不改变 reverseClose 红字凭证 isReversed 过滤语义（现状正确）。
- 不顺手修改年度结转（`closeAnnual`/`AnnualCloseService`）内部逻辑——若复核发现同型 flush 缺陷仅登记 bugs，归 successor。
- 不处理 OA-01（种子聚合）/ OA-03（ASN orgId）——归本批计划 1/3。

## Task Route

- Type: `bug investigation` + `implementation-only change`（机制已审计实证，修复面窄）
- Owner Docs: `docs/design/finance/period-close.md`（步骤5 FX 结转语义、flush 边界裁决落点）、`docs/design/integration-testing.md`（勘误(2) 修正）
- Skill Selection Basis: 修复为 Java 后端单点边界修正 → `nop-backend-dev`；快照翻转与三层全比对回归 → `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（H2 fresh-DB 集成测试机制；运行前 `lsof` 确认无 live server）。

## Execution Plan

### Phase 1 - 缺陷登记与 flush 边界修复

Status: planned
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java`、`docs/bugs/`
Skill: nop-backend-dev

- Item Types: `Fix | Decision | Proof`
- Prereqs: 计划 1（种子聚合先行，隔离快照重录归因；非硬阻塞——若计划 1 未先行，本阶段快照翻转须与种子重录差量逐项归因）

- [ ] Fix: `docs/bugs/2026-08-25-closeperiod-fx-flush-profit-loss-missing-leg.md` 登记（机制/证据链/两套件语义冲突/影响面），并在 `docs/audits/arm-index.md` P1 发现汇总区补行回链（OA-02 反向回填通道，含源审计路径）。
      - Skill: none
- [ ] Decision: flush 落点选型——候选：①`closeGlModule` 内 revalue 后立即 flush（窄）；②`ProfitLossClosingService.close()` 聚合入口 flush（聚合自防御，覆盖未来新增同事务凭证源）；③`CloseVoucherWriter.writeVoucher` 末尾统一 flush（写侧自防御；主源码 **6 个调用方**：AnnualCloseService、BadDebtProvisionService、ProfitLossClosingService、ExchangeRevaluationService、AbstractErpFinReconciliationProcessor、ErpFinBadDebtProcessor——含核销 FX 与坏账写凭证方，影响面须逐一评估）。记录选择、替代方案与残留风险；须与「AST/INV 模块关账凭证可见性」复核结论一致（若折旧凭证同型不可见，选②或在模块关账编排边界统一 flush）；**closeAnnual 可见性一并纳入权衡**（`AnnualCloseService` 同事务聚合 CLP 凭证，静态已可推演同型不可见——选①/②时年度结转聚合仍见不到未 flush 凭证，选③则覆盖；权衡结论写入 Decision 记录，年度结转修复本身仍归 Deferred 项）。
      - Skill: nop-backend-dev
- [ ] Fix: 落地 flush 边界（生产代码单点，预计 ≤5 行；遵循平台 helper 与既有 `facade.orm().flushSession()` 先例）。
      - Skill: nop-backend-dev
- [ ] Proof: `mvn test -pl module-finance/erp-fin-service`（聚焦 `TestErpFinProfitLossClosing`/`TestErpFinPeriodCloseEndToEnd` 及期间关账族）全绿——含 FX 语义保持。
      - Skill: nop-testing

Exit Criteria:

- [ ] fin-service 模块测试全绿（本地化验证，解除 Phase 2 阻塞）
- [ ] flush 落点 Decision 记录在案（选型 + AST/INV 可见性复核结论）

### Phase 2 - C13 断言与快照翻转

Status: planned
Targets: `app-erp-all/src/test/java/io/nop/app/all/it/TestErpC13FinPeriodCloseReverse.java`、`app-erp-all/_cases/io/nop/app/all/it/TestErpC13FinPeriodCloseReverse/`
Skill: nop-testing

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [ ] Fix: 断言翻转——`PL_TOTAL` 1130 → 1280；PERIOD_CLOSE 凭证补汇兑结转腿 = **Cr 6603 150**（费用类科目经 `ProfitLossClosingService.java:124-131` 以**贷方**分录结转；**Dr 6603 150 是 FX-REVAL 重估凭证的腿**，已断言于 `TestErpC13FinPeriodCloseReverse.java:172-177`，勿混淆方向）；1280 总额分解 = Dr 5001 1130 + Dr 4103 150 ↔ Cr 4103 1130 + Cr 6603 150（本年利润腿总额拆分，`ProfitLossClosingService.java:141-148`；修复后 4103 有 Dr 150 + Cr 1130 两腿，断言须按借贷方向分别锚定，不能单腿匹配）；汇兑损益科目结账后净额归零断言对齐 fin-service 语义；反结账→重结账幂等断言金额同步（1280 口径）。
      - Skill: nop-testing
- [ ] Fix: C13 快照重录（RECORDING→CHECKING 往返；保持通配纪律——金额/科目/借贷方向全字面值）。
      - Skill: nop-testing
- [ ] Proof: `mvn test -pl app-erp-all` 全绿（54/0/0/1）；其余 21 用例零漂移复核（本修复理论影响面 = 走 closePeriod 的用例——C13 及任何含期间结账步骤的用例，执行期以全量跑实证）。
      - Skill: nop-testing

Exit Criteria:

- [ ] app-erp-all 54/0/0/1 全绿；两套件（fin-service 单测 + app-erp-all 集成测）对「期间收入 + FX 损益 → 结转总额」断言语义一致（含 FX）

### Phase 3 - owner-doc 裁决与勘误修正

Status: planned
Targets: `docs/design/finance/period-close.md`、`docs/design/integration-testing.md`
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: Phase 2

- [ ] Decision: `period-close.md` 显式裁决登记——「closePeriod 同事务产生的期末凭证（汇兑重估等）对损益结转聚合可见（flush 边界为实现契约）；PERIOD_CLOSE 凭证必含 FX 腿」；裁决落盘前不得新增钉死「不含 FX」现状的用例（该禁令随本裁决落盘解除）。
      - Skill: none
- [ ] Fix: `integration-testing.md:409` 勘误(2) 修正——「行为差异以实仓为准」改为「flush 边界缺陷已于 2026-08-25 修复（plan 本计划），PERIOD_CLOSE 含 FX 腿（1280）」。
      - Skill: none

Exit Criteria:

- [ ] 两处 owner-doc 修正落盘且与测试断言语义一致

## Draft Review Record

- Independent draft review iteration 1: acceptable (task `ses_fca9b087bffehkZGj0ohbdtTql`, fresh session) — 全部机制断言活仓核验通过（closeGlModule 无 flush 序列、CloseVoucherWriter saveEntity 无 flush 且 6 调用方清单逐一核实完整、flush 先例 :95、聚合 findAllByQuery :79、费用类贷方结转 :124-131、两测试断言与 1280 算术、period-close.md 步骤5 + 6603 费用类、「余额归零」确为派生推论且计划如实标注、勘误(2) 原文、arm-index P1 汇总区在位）；OA-02 五项修复方向全覆盖；Deferred 项触发条件已具名。无阻塞项。采纳非阻塞注记：closeAnnual 可见性显式纳入 flush 落点权衡（静态已可推演同型不可见）、Phase 2 断言补「4103 双腿按方向分别锚定」执行注记、Phase 3 禁令措辞直白化。已按注记修订，共识达成 → active。

## Closure Gates

- [ ] 范围内行为完成（flush 边界 + C13 翻转 + 登记/裁决/勘误）
- [ ] 相关文档对齐（period-close.md / integration-testing.md / docs/bugs/ / arm-index 回链）
- [ ] 已运行验证：`mvn test -pl module-finance/erp-fin-service` + `mvn test -pl app-erp-all`（54/0/0/1）+ 全 reactor `mvn test`（对照 known-good-baselines 2026-08-25 V.1 行 3834/0/0/1/642 口径，差量全额归因本修复）+ `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`（零漂移或逐项归因）
- [ ] 会计保护区域变更经双独立子代理批准（批准记录落于本计划）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 年度结转（closeAnnual/AnnualCloseService）同型 flush 复核（若 Phase 1 复核发现）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 年度结转为 12 月/年末 config-gated 分支，C13（7 月期间）不覆盖；若复核发现同型缺陷仅登记 bugs 带 successor 触发条件（年度结账端到端验证时修复）。
- Successor Required: yes（触发条件：年度结账端到端用例落地或 12 月期间结账验证时）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- （无；已确认缺陷不得出现在此处）
