# 2026-08-25-0330-2-fix-closeperiod-fx-flush-profit-loss 修复 closePeriod 同事务 FX 凭证未 flush 致损益结转永缺 FX 腿

> Plan Status: completed
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

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java`、`docs/bugs/`
Skill: nop-backend-dev

- Item Types: `Fix | Decision | Proof`
- Prereqs: 计划 1（种子聚合先行，隔离快照重录归因；非硬阻塞——若计划 1 未先行，本阶段快照翻转须与种子重录差量逐项归因）

- [x] Fix: `docs/bugs/2026-08-25-closeperiod-fx-flush-profit-loss-missing-leg.md` 登记（机制/证据链/两套件语义冲突/影响面），并在 `docs/audits/arm-index.md` P1 发现汇总区补行回链（OA-02 反向回填通道，含源审计路径）。
      - Skill: none
- [x] Decision: flush 落点选型——候选：①`closeGlModule` 内 revalue 后立即 flush（窄）；②`ProfitLossClosingService.close()` 聚合入口 flush（聚合自防御，覆盖未来新增同事务凭证源）；③`CloseVoucherWriter.writeVoucher` 末尾统一 flush（写侧自防御；主源码 **6 个调用方**：AnnualCloseService、BadDebtProvisionService、ProfitLossClosingService、ExchangeRevaluationService、AbstractErpFinReconciliationProcessor、ErpFinBadDebtProcessor——含核销 FX 与坏账写凭证方，影响面须逐一评估）。记录选择、替代方案与残留风险；须与「AST/INV 模块关账凭证可见性」复核结论一致（若折旧凭证同型不可见，选②或在模块关账编排边界统一 flush）；**closeAnnual 可见性一并纳入权衡**（`AnnualCloseService` 同事务聚合 CLP 凭证，静态已可推演同型不可见——选①/②时年度结转聚合仍见不到未 flush 凭证，选③则覆盖；权衡结论写入 Decision 记录，年度结转修复本身仍归 Deferred 项）。
      - Skill: nop-backend-dev
- [x] Fix: 落地 flush 边界（生产代码单点，预计 ≤5 行；遵循平台 helper 与既有 `facade.orm().flushSession()` 先例）。
      - Skill: nop-backend-dev
- [x] Proof: `mvn test -pl module-finance/erp-fin-service`（聚焦 `TestErpFinProfitLossClosing`/`TestErpFinPeriodCloseEndToEnd` 及期间关账族）全绿——含 FX 语义保持。
      - Skill: nop-testing

Exit Criteria:

- [x] fin-service 模块测试全绿（本地化验证，解除 Phase 2 阻塞）
- [x] flush 落点 Decision 记录在案（选型 + AST/INV 可见性复核结论）

#### Phase 1 Decision Record（flush 落点选型：③ 写侧自防御）

- **选择**：③ `CloseVoucherWriter.writeVoucher` 末尾统一 `flushSession()`（`((IOrmEntityDao<?>) voucherDao).getOrmTemplate().flushSession()`，与 `ErpFinAccountingPeriodClosePeriodProcessor:95` + `AbstractErpFinReconciliationProcessor.flushBeforeBalance:155-157` 先例同机制；空行/零金额早退路径不触发）。
- **决定性证据（①②不可行的实仓新证据）**：试算平衡表快照（`populateTrialBalanceForAllSchemas`，DB 直查）在 PL 凭证写入后执行——同事务 FX **与 PL 两凭证全部不可见**（C13 快照 `erp_fin_trial_balance.csv` 实证：仅 seed 科目行，无 6603/4103 腿）。①仅覆盖 FX→PL；②覆盖 FX→PL 但 PL 凭证由聚合服务自身在聚合后写入——**任何聚合前 flush（①②）都无法让试算平衡表看到 PL 凭证腿**，只有写侧 flush（③）单点全覆盖（PL 聚合 / 试算平衡 / 年度结转聚合 / 未来新增同事务凭证源）。
- **closeAnnual 权衡**：`AnnualCloseService.subjectNetForYear`/`aggregateYearSubjectActivity` 同事务 DB 直查聚合——选③时 PL(CLP)/ACY 凭证写入即 flush，年度结转聚合可见（同型缺陷顺带覆盖）；年度结账端到端验证仍归 Deferred 项 successor 触发条件（C13 为 7 月期间不覆盖 12 月分支）。
- **6 调用方逐一评估**（批准审查 A/B 双复核）：ProfitLossClosingService（写后无依赖读，多账套循环经 PERIOD_CLOSE 自排除防双计）/ ExchangeRevaluationService（银行账聚合先于写入）/ AnnualCloseService（聚合先于写入，populate 受益）/ BadDebtProvisionService（allowance 读先于写，多账套防陈旧双计——改进）/ AbstractErpFinReconciliationProcessor（写后 hasFxVoucher 同事务反查 billR 现正确命中——改进）/ ErpFinBadDebtProcessor（ArApItem 变更先完成，写后无依赖读）。均为事务内单凭证流，flush≠commit，原子性/回滚语义不变。
- **AST/INV 可见性复核结论**：AST 折旧凭证走 `IErpFinVoucherBiz.post` `@Transactional(REQUIRES_NEW)` 独立事务已提交——天然可见，无需 flush；INV 成本兜底重算不产 GL 凭证（仅成本层/流水）且自带 `ormTemplate.flushSession()`——无可见性问题。
- **残留风险**：flush 提前触发脏实体的乐观锁 VERSION 递增与序列分配顺序漂移（纯实现列/ID 漂移，业务字段零变化——fin-service 7 类 15 处快照差量全数归因，见 Phase 1 Proof）；负面凭证金额腿（收益场景 Cr −50）为既有 fin-service 语义（`testProfitLossClosingIncludesFxGainLoss` 钉死），本修复不改写入策略（Non-Goal）。
- **Proof 记录**：`mvn test -pl module-finance/erp-fin-service` 497/497 全绿。伴随语义对齐：`TestErpFinAnnualClose.testBankFxRevaluationForeignAccount` 原「银行 FX 不触发 P&L 结转」前提为缺陷行为产物——补 seed 4103 科目 + 6603 归零断言（含 FX 语义）；7 类快照 RECORDING→CHECKING 往返重录（差量 = VERSION/序列 ID 漂移 + 试算平衡表 PL 腿新增，逐项归因如上）。

### Phase 2 - C13 断言与快照翻转

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/it/TestErpC13FinPeriodCloseReverse.java`、`app-erp-all/_cases/io/nop/app/all/it/TestErpC13FinPeriodCloseReverse/`
Skill: nop-testing

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] Fix: 断言翻转——`PL_TOTAL` 1130 → 1280；PERIOD_CLOSE 凭证补汇兑结转腿 = **Cr 6603 150**（费用类科目经 `ProfitLossClosingService.java:124-131` 以**贷方**分录结转；**Dr 6603 150 是 FX-REVAL 重估凭证的腿**，已断言于 `TestErpC13FinPeriodCloseReverse.java:172-177`，勿混淆方向）；1280 总额分解 = Dr 5001 1130 + Dr 4103 150 ↔ Cr 4103 1130 + Cr 6603 150（本年利润腿总额拆分，`ProfitLossClosingService.java:141-148`；修复后 4103 有 Dr 150 + Cr 1130 两腿，断言须按借贷方向分别锚定，不能单腿匹配）；汇兑损益科目结账后净额归零断言对齐 fin-service 语义；反结账→重结账幂等断言金额同步（1280 口径）。
      - Skill: nop-testing
- [x] Fix: C13 快照重录（RECORDING→CHECKING 往返；保持通配纪律——金额/科目/借贷方向全字面值）。
      - Skill: nop-testing
- [x] Proof: `mvn test -pl app-erp-all` 全绿（54/0/0/1）；其余 21 用例零漂移复核（本修复理论影响面 = 走 closePeriod 的用例——C13 及任何含期间结账步骤的用例，执行期以全量跑实证）。
      - Skill: nop-testing

Exit Criteria:

- [x] app-erp-all 54/0/0/1 全绿；两套件（fin-service 单测 + app-erp-all 集成测）对「期间收入 + FX 损益 → 结转总额」断言语义一致（含 FX）

#### Phase 2 执行记录

- 断言翻转落地：`PL_TOTAL=1280`/`PL_FX=150` 常量；新增 `findLineBySubjectAndDirection`（4103 双腿 Dr 150 + Cr 1130 方向化锚定）与 `subjectNetAmount`（6603 结账后净额归零，对齐 fin-service `testProfitLossClosingIncludesFxGainLoss` 语义）；反结账/重结账幂等断言经 `PL_TOTAL` 常量自动同步 1280 口径（红字凭证 -1280、重结新凭证 1280）；类 javadoc Decision ② 勘误叙事改为「flush 边界缺陷已修复（2026-08-25）」。
- 快照重录（forceSaveOutput 单方法往返）：output tables 语义差量 = PL 凭证 4 腿（原 2 腿）1280 + 红字 -1280 四腿 + 重结 1280 四腿 + 试算平衡两批次各 +2 行（6603 Dr150/Cr150 归零、4103 Dr150/Cr1130 → 净贷 980）+ 1122 含 FX Cr150（净贷 150）+ 5001 含结转 Dr1130 归零 + status VERSION 3→5（flush 拆分）+ 序列 ID 位移；响应 json5 与 input tables 零语义差量（volatile 字段还原 HEAD 通配纪律——重录曾以字面量覆盖 `*`，已按 B4/B6 通配纪律恢复）。CHECKING 往返绿。

### Phase 3 - owner-doc 裁决与勘误修正

Status: completed
Targets: `docs/design/finance/period-close.md`、`docs/design/integration-testing.md`
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: Phase 2

- [x] Decision: `period-close.md` 显式裁决登记——「closePeriod 同事务产生的期末凭证（汇兑重估等）对损益结转聚合可见（flush 边界为实现契约）；PERIOD_CLOSE 凭证必含 FX 腿」；裁决落盘前不得新增钉死「不含 FX」现状的用例（该禁令随本裁决落盘解除）。
      - Skill: none
- [x] Fix: `integration-testing.md:409` 勘误(2) 修正——「行为差异以实仓为准」改为「flush 边界缺陷已于 2026-08-25 修复（plan 本计划），PERIOD_CLOSE 含 FX 腿（1280）」。
      - Skill: none

Exit Criteria:

- [x] 两处 owner-doc 修正落盘且与测试断言语义一致

#### Phase 3 执行记录

- `period-close.md` §期末结账步骤 流程图后新增「同事务期末凭证可见性裁决（2026-08-25）」blockquote：flush 边界为实现契约（CloseVoucherWriter 写侧统一 flushSession）+ PERIOD_CLOSE 必含 FX 腿 + 汇兑损益科目结账后净额归零派生不变量 + bug 回链。「不含 FX」用例禁令随裁决落盘解除。
- `integration-testing.md` C13 勘误(2) 改写为「PERIOD_CLOSE 凭证含 FX 腿（勘误已修复）」：1280 四腿分解 + 裁决回链 + 两套件语义统一 + 试算平衡同步说明。

## Draft Review Record

- Independent draft review iteration 1: acceptable (task `ses_fca9b087bffehkZGj0ohbdtTql`, fresh session) — 全部机制断言活仓核验通过（closeGlModule 无 flush 序列、CloseVoucherWriter saveEntity 无 flush 且 6 调用方清单逐一核实完整、flush 先例 :95、聚合 findAllByQuery :79、费用类贷方结转 :124-131、两测试断言与 1280 算术、period-close.md 步骤5 + 6603 费用类、「余额归零」确为派生推论且计划如实标注、勘误(2) 原文、arm-index P1 汇总区在位）；OA-02 五项修复方向全覆盖；Deferred 项触发条件已具名。无阻塞项。采纳非阻塞注记：closeAnnual 可见性显式纳入 flush 落点权衡（静态已可推演同型不可见）、Phase 2 断言补「4103 双腿按方向分别锚定」执行注记、Phase 3 禁令措辞直白化。已按注记修订，共识达成 → active。

## Protected-Area Approval Record（会计保护区域：双独立子代理批准，2026-08-25）

> 规则：`docs/skills/README.md` §保护区域（auto + dual-agent-approval，fresh session 互不共享执行者上下文）+ `docs/context/ai-autonomy-policy.md`。变更对象：`CloseVoucherWriter.writeVoucher` 写侧 flush（期末结账凭证可见性——业财过账正确性保护区域）。

- Approver A（task `ses_fca4d82ddffe4sQvjZuu04cnLI`，fresh session）：**APPROVE**。核验：缺陷机制（closeGlModule 顺序 + saveEntity 无 flush + findAllByQuery 直查）、flush≠commit 语义（nop-entropy IOrmTemplate/IOrmSession 源码级确认 + :95 先例注释的事务回滚语义）、6 调用方逐一确认 flush 安全、FX 含入语义正确（6603 费用类贷方结转 + CYP 双腿方向）、写侧选型优于①②（试算平衡/年度结转覆盖）。条件：fin-service 快照重录（已完成的 Phase 1 Proof 覆盖）+ Phase 2 C13 翻转 + Phase 3 裁决落盘 + 结束验证——全部为本计划内后续 Phase。
- Approver B（task `ses_fca4d594dffeBVtpHWcCYZQQuJ`, fresh session）：**APPROVE**。核验：flush 位置（全部 save 之后、早退路径不触发）、平台 API 与三处仓内先例一致（含 `AbstractErpFinReconciliationProcessor.flushBeforeBalance:155-157` javadoc 同型论证）、7 调用点逐一审计（多账套双计经 PERIOD_CLOSE 排除中性化、坏账多账套陈旧读预防——改进）、1280 算术独立重推导、约束安全（save 顺序 voucher→lines→billR 全字段先于 flush 就位）、保护区域 operative 规则满足。条件：C13 翻转 + owner-doc 裁决 + 第二批准与记录 + 验证运行——均为计划内 Phase/本记录覆盖。
- 双 APPROVE → 变更获准实施（已于 Phase 1 落地并全绿）。

## Closure Gates

- [x] 范围内行为完成（flush 边界 + C13 翻转 + 登记/裁决/勘误）
- [x] 相关文档对齐（period-close.md / integration-testing.md / docs/bugs/ / arm-index 回链）
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service`（497/0/0）+ `mvn test -pl app-erp-all`（54/0/0/1）+ 全 reactor `mvn test`（**3834/0/0/1/642**，对照 known-good-baselines 2026-08-25 V.1 行逐项零漂移，差量全额归因本修复：fin-service 7 类 + sales TestErpSalMultiCurrencyReconFx 1 类快照重录 + C13 翻转重录；surefire-reports 清零后干净口径）+ `mvn clean install -DskipTests`（156 模块 BUILD SUCCESS 01:41）+ `bash docs/audits/nop-compliance-checker.sh`（全规则 actual==baseline 零漂移：R1d=14/R2a=34/R2b=237/n=1505/R2d=38/R3=5/R6=2/R10=12/n/b/c=70/66/41）
- [x] 会计保护区域变更经双独立子代理批准（批准记录落于本计划，见 §Protected-Area Approval Record）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 年度结转（closeAnnual/AnnualCloseService）同型 flush 复核（若 Phase 1 复核发现）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 年度结转为 12 月/年末 config-gated 分支，C13（7 月期间）不覆盖；若复核发现同型缺陷仅登记 bugs 带 successor 触发条件（年度结账端到端验证时修复）。
- Successor Required: yes（触发条件：年度结账端到端用例落地或 12 月期间结账验证时）

## Closure

Status Note: closed（2026-08-25 三 Phase 全部完成 + 全量验证绿（fin-service 497/0/0 + app-erp-all 54/0/0/1 + 全 reactor 3834/0/0/1/642 零漂移 + install 156 模块 + compliance 零漂移）+ 会计保护区域双独立子代理批准 + 独立结束审计 PASS；Plan Status → completed。）

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor（fresh session task `ses_fca19306affeMTpW4VXXbBeYmC`，非执行者）
- Evidence: **CLOSURE VERDICT: PASS，零阻塞项**——12 项活仓核验全过：①bug 文档机制/证据/修复节在位；②arm-index P1 汇总区（:728）OA-02 回链含 bug+plan 双路径；③CloseVoucherWriter :142 写侧 flush（helper :147-150）位于全部 save 之后、早退路径未触碰，diff +11 行；④Decision Record 含①②排除决定性证据 + AST（REQUIRES_NEW 天然可见）/INV（无 GL 凭证）复核结论 + closeAnnual 权衡；⑤C13 断言 PL_TOTAL=1280（:121）+ 4103 双腿方向化（:193-194/:201-202）+ Cr 6603 FX 腿（:195/:203）+ 6603 归零（:205-207）；⑥快照 1280/-1280/1280 三凭证 + PERIOD_CLOSE 四腿 + 试算平衡 6603 归零/4103 净贷 980 行；⑦响应 json5 通配纪律（无字面运行时间戳）；⑧period-close.md :113 裁决 blockquote；⑨integration-testing.md:409 勘误(2) 已修复口径；⑩三 Phase Status: completed + 全 item/gate [x] + 双 APPROVE 记录 + 日志条目 + 基线行 + Deferred 项原样保留；⑪FX-REVAL 凭证腿（Dr 6603）与 PL 凭证 FX 结转腿（Cr 6603）方向相反独立断言无混淆；⑫生产代码唯一变更 = CloseVoucherWriter（零 ORM/生成代码触碰）。

Follow-up:

- （无；已确认缺陷不得出现在此处）
