# 2026-07-31-0744-1-r2-11-mfg-test-effectiveness R2.11 manufacturing 工单/MRP/委外链路测试有效性（残差补强）

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR2 R2.11（P1-MA4-009 + P1-MA4-011 残差）
> Related: `docs/audits/arm-index.md`（P1-MA4-009/011/007/010）、R1.16（业财悬挂统一裁决，已落地 SubcontractPostingDispatcher.dispatchFailureAlert 但未覆盖 ManufacturingIssue/ProductionVariance 告警 + 未补 mfg 测试）、R2.10/R2.13（同族测试有效性残差范式）
> Audit: required（独立草案审查 + 独立 closure audit）

## Current Baseline

P1-MA4-009/011（finding 写于 R1.x 测试落地之前）的多个子项**已由既有测试闭合**。独立草案审查（fresh-session 实测）确认下列既有测试已覆盖原 finding 列为「缺口」的多项，本计划仅补**残差缺口**。逐项实测基线（grep 全 mfg 测试树：`getAmountSource|getAmountFunctional|getExchangeRate` = 0 命中；`Mockito|mock(|when(` = 0 命中——mfg 测试无 mock 基础设施）：

**已闭合子项（不在本计划范围，避免重复）**：
- 报工超量 ERR_OVER_REPORT：`TestErpMfgWorkOrderStateMachine.testOverReportRejected`（:181-193，completedQty>plannedQuantity→asserts ERR_OVER_REPORT）
- 齐套不足 STOCK_PARTIAL 强制开工：`TestErpMfgWorkOrderStateMachine.testKitCheckPartialGoesStockPartial`（:98-113，M2 short→STOCK_PARTIAL，allow-partial-kit-start=false→ERR_PARTIAL_KIT_START_FORBIDDEN）+ 正向 `testPartialKitStartAllowedWhenConfigEnabled`（:115-133）
- CRP overload 阈值边界 + APS fallback：`TestErpMfgCrpLoad`（:169 default=1.0 overloaded=true；:175 threshold=3.0 boundary 不超载）+ `TestErpMfgCrpLoadSource`（:145-171 APS mode 无 slot→fallback WorkOrder date）+ `TestErpMfgReportRendering:115-116`
- BomExpander 成环 ERR_BOM_CYCLE：`TestErpMfgBomExplosion.testCycleDetection`（:137-147，P→SA→P cycle）—— **注：仅覆盖 BomExpander.explode 路径，不覆盖 CostRollupService.rollup 自有成环路径**
- 业财悬挂告警闭环（finance 层基础设施）：R1.16 已落地 `ErpFinPostingExceptionRecorder`/`DeferredPostingRetryHelper` + finance workbench 测试，但 mfg dispatchers 未接线 finance 异常记录，故 mfg 层 posted=false 悬挂对测试不可见

**残差缺口（本计划范围）**：
- **G1（P1-MA4-009(a) + 011(b)）业财过账失败悬挂 posted=false 可观测**：mfg 3 类 dispatcher 的 tryPost catch 吞咽路径无测试触发——`ManufacturingIssuePostingDispatcher`（catch LOG.warn 保持 posted=false，:107-113）/ `ProductionVarianceDispatcher`（catch LOG.warn，:111-117）/ `SubcontractPostingDispatcher`（issue/receipt/fee catch + dispatchFailureAlert 通知，:145-168）。现有 mfg 过账测试仅断言黄金路径 posted=true（TestErpMfgIssuePosting:94/136、CompletionPosting:105/154/190、ProductionVariance:249、Subcontracting:124、CostFlowEndToEnd:110/132）。finance 层悬挂测试（TestErpFinPostingExceptionWorkbench 等）零引用 MANUFACTURING_ISSUE/SUBCONTRACT_FEE/PRODUCTION_VARIANCE 业务类型，不传递覆盖。**注**：mfg 测试无 Mockito 基础设施——按 `TestErpInvPosting.testPostingFailureLeavesMoveDonePostedFalse`（:107-115，seed 无会计期间→post 失败→断言 posted=false）+ R2.13 G2（清空科目配置确定性触发失败）的既有无 mock 确定性失败诱导范式实现。
- **G2（P1-MA4-009(b) + 011(a)）完工入库/差异/委外多币种凭证行级断言**：`TestErpMfgWorkOrderEndToEnd`（432 行）完全不断言任何凭证行（无 ErpFinVoucherLine import）；`TestErpMfgCompletionPosting`（findVoucherLine:426）仅查 debitAmount/creditAmount/dcDirection，`CURRENCY_ID=6501L` 且 `acctSchema.functionalCurrencyId=CURRENCY_ID`（:208）→ source==functional rate 隐式 ONE；`TestErpMfgProductionVariance`（:209-251/368-399）+ `TestErpMfgSubcontracting`（:84-141，order.exchangeRate=BigDecimal.ONE :322）同型单币种。repo-wide `exchangeRate` 在 mfg 测试恒 ONE（CostRollup:453、Subcontracting:322、SubcontractReverse:335、Dashboard:160）。残差 = seed 第 2 币种 ≠ functionalCurrency + exchangeRate≠ONE，断言凭证行 amountSource≠amountFunctional/exchangeRate≠ONE/借贷方按折算正确。
- **G3（P1-MA4-011(c) 残差）CostRollupService.rollup 自有成环路径**：`CostRollupService.rollup`（:136 自有 path 成环检测抛 ERR_BOM_CYCLE）不经 BomExpander.explode，`TestErpMfgCostRollup`（501 行）无成环测试（仅覆盖 ERR_ROLLUP_BASE_COST_MISSING :120-129）。BomExpander 成环已覆盖不替代 CostRollupService 自有路径。
- **G4（P1-MA4-011(d)）MrpEngine scheduledReceipt 断言**：`TestErpMfgMrpEngine`（392 行）零引用 scheduledReceipt；字段存在于实体（ErpMfgMrpPlanLine.scheduledReceipt）但无测试读取 getScheduledReceipt()。残差 = seed 含在途 scheduledReceipt 的计划→断言 netRequirement 正确扣除 scheduledReceipt（或显式断言恒 0 的 owner-doc 语义）。

剩余差距：G1 + G2 + G3 + G4 四个残差。本计划为**纯测试新增**（无生产 Java/ORM/view.xml 变更），不触及制造/会计保护区域运行时行为——仅补测试使业财悬挂状态、多币种凭证行、CostRollup 成环、MRP 在途对测试可观测。

## Goals

- G1：业财过账失败悬挂测试——确定性诱导 3 类 mfg dispatcher（ManufacturingIssue/ProductionVariance/Subcontract）post 失败，断言 posted=false 持续（+ Subcontract 断言 dispatchFailureAlert 触发），闭合 P1-MA4-007/010/009(a)/011(b) 测试可见性
- G2：多币种凭证行级 E2E——seed 非 ONE exchangeRate 的完工入库/差异/委外过账，使凭证行级多币种维度（amountSource/amountFunctional/exchangeRate/currencyId）对测试可观测。实施发现 P1-MA3-039 残差（mfg 三 AcctDocProvider 未拆分 amountSource/amountFunctional；ProductionVarianceDispatcher/InvPostingDispatcher 硬编码 exchangeRate=ONE，仅 SubcontractPostingDispatcher 透传 order.exchangeRate）→ 锁定当前行为为回归基线 + 升级 successor Fix（闭合 P1-MA3-039 mfg 投影可见性）
- G3：CostRollupService 成环 assertThrows ERR_BOM_CYCLE——构造 CostRollup 自有 path 成环（不经 BomExpander.explode），断言抛 ERR_BOM_CYCLE
- G4：MrpEngine scheduledReceipt 断言——seed 含在途 scheduledReceipt 的计划，断言 netRequirement 正确扣除（或按 owner-doc 断言恒 0 语义）

## Non-Goals

- 不重复实现已闭合子项（报工超量 / 齐套强制开工 / CRP overload+APS fallback / BomExpander 成环——见 Current Baseline 既有测试清单）
- 不修改任何生产 Java 代码（BizModel/Processor/Dispatcher/Engine/Calculator）——若 G1-G4 测试发现与 owner doc 不符的真实行为缺陷，按不可降级 Fix 规则升级为独立修复计划，不在本测试计划中静默修改生产代码
- 不为 ManufacturingIssue/ProductionVariance dispatcher 补告警 dispatch（R1.16 未覆盖的代码侧告警）——属代码修复非测试有效性；若 G1 测试证实 posted=false 悬挂且无告警，作为 successor 记录，不在本计划改生产代码
- 不补 finance/assets/hr/pur+sal+inv 测试有效性（分别归 R2.10 done / R2.12 / R2.13 done / R2.14）
- 不补 R2.15 view.xml drift（含 P2-MA4-014 mfg 进度 badge STARTED/ACTIVE 死状态——P2 watch-only，不同结果表面）

## Task Route

- Type: `implementation-only change`（纯测试新增）
- Owner Docs: `docs/design/manufacturing/state-machine.md`（报工/齐套状态）、`docs/design/manufacturing/mrp.md`（MRP net 需求 + scheduledReceipt + CostRollup 卷积）、`docs/design/manufacturing/`（完工/差异/委外过账 + 业财悬挂 owner-doc 语义）。测试断言的预期行为须与 owner doc 一致
- Skill Selection Basis: 工作方法为 Nop 服务层集成测试（`JunitAutoTestCase` + Facade Java API + seed/output/assert + 无 mock 确定性失败诱导）→ `nop-testing`（基类选择、@NopTestConfig、seed 只追加、拒绝路径快照处理、三层验证模型）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（H2 localDb 集成测试，无端口/外部服务；确定性失败诱导复用 inventory/hr 既有范式——seed 无会计期间/清空科目配置触发 post 失败，无需 Mockito）

## Execution Plan

### Phase 1 - 业财过账失败悬挂 + 多币种凭证行级 + CostRollup 成环 + MRP scheduledReceipt（G1+G2+G3+G4）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgCompletionPosting.java`、`TestErpMfgProductionVariance.java`、`TestErpMfgSubcontracting.java`、`TestErpMfgCostRollup.java`、`TestErpMfgMrpEngine.java`（新增测试方法 + 对应 `_cases/` 快照/seed）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: R2.0 done（已 done）；R1.16 done（业财悬挂统一裁决，Subcontract 告警已落地，ManufacturingIssue/ProductionVariance 告警未覆盖——G1 仅测 posted=false 可观测，告警代码侧为 successor）

- [x] Add: G1 业财过账失败悬挂测试 — 确定性诱导 post 失败（seed 无会计期间或清空科目映射，复用 `TestErpInvPosting:107-115` 无 mock 范式），对 ManufacturingIssuePostingDispatcher / ProductionVarianceDispatcher / SubcontractPostingDispatcher(issue/receipt/fee) 各断言 posted=false 持续 + 业务单据终态不受影响（Subcontract 额外断言 dispatchFailureAlert 触发）；闭合 P1-MA4-007/010/009(a)/011(b) 测试可见性
  - Skill: `nop-testing`
- [x] Add: G2 多币种凭证行级 E2E — 完工入库/生产差异/委外过账各 seed 第 2 币种 + exchangeRate≠ONE（如 6.5），断言凭证行级多币种维度对测试可观测。**实施发现 P1-MA3-039 残差**：mfg 三 Provider 未拆分 amountSource/amountFunctional（GL 引擎「方案 A」不计算 functional=source×rate）；ProductionVarianceDispatcher/InvPostingDispatcher 硬编码 exchangeRate=ONE（仅 Subcontract 透传 6.5）→ 三测试锁定当前 amountSource==amountFunctional 基线（Subcontract 另断 exchangeRate=6.5 透传），升级 successor Fix（见 Deferred 段）
  - Skill: `nop-testing`
- [x] Add: G3 CostRollupService 成环 assertThrows — 构造 CostRollupService.rollup 自有 path 成环（产品 A 卷积依赖 B，B 依赖 A，不经 BomExpander.explode），断言抛 ERR_BOM_CYCLE；闭合 P1-MA4-011(c) CostRollup 路径残差
  - Skill: `nop-testing`
- [x] Add: G4 MrpEngine scheduledReceipt 断言 — seed 含在途 scheduledReceipt 的 MRP 计划，运行 MrpEngine，断言 netRequirement 正确扣除 scheduledReceipt（或按 mrp.md owner-doc 断言恒 0 的语义并锁定为回归基线）；闭合 P1-MA4-011(d)
  - Skill: `nop-testing`
- [x] Proof: Phase 1 新增测试方法首次 RECORDING 后切 CHECKING 全绿
  - `mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgCompletionPosting,TestErpMfgProductionVariance,TestErpMfgSubcontracting,TestErpMfgCostRollup,TestErpMfgMrpEngine`
  - Skill: none

Exit Criteria:

> mfg 业财悬挂状态 + 多币种凭证行 + CostRollup 成环 + MRP 在途补齐，使 4 类缺陷对测试可观测。

- [x] G1（posted=false 持续 + 终态不受影响 + Subcontract 告警）+ G2（凭证行级多币种维度 currencyId/exchangeRate/amountSource/amountFunctional 对测试可观测；实施发现 P1-MA3-039 残差并锁定回归基线 + successor Fix）+ G3（CostRollup 成环 ERR_BOM_CYCLE）+ G4（scheduledReceipt 断言）测试在 CHECKING 模式绿
- [x] 若 G1/G2 测试发现与 owner doc 不符的真实行为缺陷，升级为独立 Fix 计划并记录（不静默改生产代码）

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_04a9318ebffe8mrxgUP8uOiXah) — fresh-session 实测复核：4 项「已闭合」声明全部精确（testOverReportRejected:181-193 / testKitCheckPartialGoesStockPartial:98-113 / CrpLoad:169,175 + CrpLoadSource:148-172 / BomExplosion.testCycleDetection:137-147）；G1-G4 残差全部真实且完整（grep posted.*false 全为 reversal 非 failure；amountSource/amountFunctional/exchangeRate 零命中；CostRollupService.computeUnit:130-139 自有 path 成环不经 explode；scheduledReceipt 零命中）；P1-MA4-009/011 全子项无遗漏（009 c/d closed、011 e closed）；无 mock 可行性确认（mfg 测试零 Mockito + seedOpenPeriod helper 存在，省略期间 seed 即可）。无阻塞项。已采纳 2 项非阻塞 citation 精修（ReportRendering:114→:115-116、CostRollup:378/453→:453）。草案审查收敛，转 active。

## Closure Gates

> 纯测试新增，无生产代码/ORM/view.xml 变更。完整仓库验证在此处一次。

- [x] 范围内行为完成（G1 + G2 + G3 + G4 残差测试方法落地并 CHECKING 绿）
- [x] 相关文档对齐（G2 实施发现 P1-MA3-039 多币种残差真实缺陷，已升级 successor Fix 并记录于 Deferred 段；G4 引用 owner-doc mrp.md:93 恒 0 语义）
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿 + `mvn test -pl module-manufacturing/erp-mfg-service` 全绿（152 tests, 0 failures/errors，含 8 新测试）+ `bash docs/audits/nop-compliance-checker.sh` 零新增命中（纯测试新增，无生产代码变更）
- [x] 无范围内项目降级为 deferred/follow-up（G2 发现的真实缺陷按不可降级规则升级 successor Fix，不降级；ManufacturingIssue/ProductionVariance 告警为 Non-Goal 既定边界）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（G2 Goal/Item/Exit 文案已按实施发现修正为「锁定基线 + successor」）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ManufacturingIssue/ProductionVariance dispatcher 告警 dispatch（R1.16 未覆盖的代码侧告警）

- Classification: `watch-only residual`（沿用 R1.16 裁决边界——mfg 2 dispatcher 仍纯 LOG 吞咽无 IErpSysNotificationBiz 告警）
- Why Not Blocking Closure: 本计划为测试有效性（R2.11）；告警代码侧补齐属代码修复非测试；G1 仅测 posted=false 状态可观测（悬挂对测试可见即达测试有效性目标），告警闭环 successor 由 G1 测试证据触发独立代码修复 plan
- Successor Required: `yes`（触发 = G1 测试落地后确认 posted=false 悬挂且无告警 → 独立告警补齐 plan，触及制造保护区域需 owner doc）

### mfg 多币种凭证行 amountSource/amountFunctional 未拆分（P1-MA3-039 残差，G2 测试发现的真实行为缺陷）

- Classification: `discovered real behavior defect → successor Fix`（G2 测试落地时按本计划 Exit Criteria #2「不可降级 Fix」规则记录，未静默改生产代码）
- Evidence: G2 三条基线测试（`testMultiCurrencyFeeVoucherLineBaseline` / `testMultiCurrencyVarianceVoucherLineBaseline` / `testMultiCurrencyCompletionReceiptVoucherLineBaseline`）锁定当前行为：
  - GL 引擎不计算 `amountFunctional = amountSource × exchangeRate`（「方案 A」由 Provider 显式传递，`ErpFinPostingProcessor:811-830`）；mfg 三 AcctDocProvider（`SubcontractFeeAcctDocProvider.fact:72-83` / `ProductionVarianceAcctDocProvider` / inventory `InvAcctDocProvider.fact:99-111`）均仅 `setAmount`，未设 amountSource/amountFunctional → `amountSource==amountFunctional`。
  - 汇率透传不一致：`SubcontractPostingDispatcher:198/232/266` 透传 `order.exchangeRate`（行 exchangeRate=6.5 可观测）；`ProductionVarianceDispatcher:164` 与 inventory `InvPostingDispatcher:207` 硬编码 `exchangeRate=ONE`（行 exchangeRate=1）。
- Successor Required: `yes`（独立 Fix plan：mfg/inv 三 Provider 按 PurAcctDocProvider 范式补 amountSource/amountFunctional 折算 + ProductionVarianceDispatcher/InvPostingDispatcher 透传实体汇率；触及会计保护区域需 owner doc + 计划审计。Fix 落地后三条基线断言转红，强制更新为 amountFunctional=source×rate）
- 本计划贡献：使该缺陷对测试可观测并锁定回归基线（R2.11 测试有效性目标达成）

## Closure

Status Note: 纯测试新增计划完成。G1（3 dispatcher 业财悬挂 posted=false 可观测，含 Subcontract dispatchFailureAlert）+ G2（3 多币种凭证行级基线，发现 P1-MA3-039 残差并锁定回归基线 + successor Fix）+ G3（CostRollupService 自有 path 成环 ERR_BOM_CYCLE）+ G4（MrpEngine scheduledReceipt 恒 0 owner-doc 基线）共 8 新测试方法落地。无生产代码/ORM/view.xml 变更。独立结束审计 verdict=PASS（task ses_04a7550a0ffe6Hxrp67uzPu6He）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 ses_04a7550a0ffe6Hxrp67uzPu6He），非执行者
- Evidence: 
  - Verdict: PASS
  - 8 新方法逐一 EXISTS/MATCHES 验证（G1a/G1b/G1c/G2a/G2b/G2c/G3/G4）
  - `git diff --stat -- '*/src/main/*' '*/model/*' '*.orm.xml' '*.view.xml' '*.xbiz.xml'` → 空（零生产代码变更）
  - 无 mock 确定性失败诱导（grep Mockito=0；seed 无会计期间范式）
  - 全量 `mvn test -pl module-manufacturing/erp-mfg-service` → Tests run: 152, Failures: 0, Errors: 0, Skipped: 0
  - G2 successor 记录与生产源码交叉验证（SubcontractFeeAcctDocProvider.fact:78 仅 setAmount / ProductionVarianceDispatcher.buildEvent:164 硬编码 ONE / InvPostingDispatcher.buildEvent:207 硬编码 ONE / ErpFinPostingProcessor:814-815 amountSource==functional 回退）
  - 非阻塞观察（G2 文案已按建议修正、Closure 已填写、dev log 已补）均已处置

Follow-up:

- successor Fix：mfg 多币种凭证行 amountSource/amountFunctional 折算（P1-MA3-039 残差）— G2 测试触发，独立 Fix plan 待建（触及会计保护区域需 owner doc + 计划审计；Fix 落地后三条 G2 基线断言转红，强制更新为 amountFunctional=source×rate）
- successor Fix：ManufacturingIssue/ProductionVariance dispatcher 告警 dispatch（R1.16 未覆盖）— G1 测试触发，独立告警补齐 plan 待建
