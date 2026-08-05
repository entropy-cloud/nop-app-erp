# 2026-08-05-1830-1 rc-ma1-a1-30-crm-f3-cpq-funnel-advancement crm-F3 CPQ/漏斗推进需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.30（MA1 需求追踪矩阵审计 — crm-F3 CPQ 配置-定价-报价 / 漏斗阶段推进）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.30
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.30 的 0.2 依赖）、`2026-08-03-1341-2-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（crm-F1 done，本切片阶段推进为 F1 线索转化后置环节）、`2026-08-03-1341-3-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`（crm-F2 done，含 P1-MA2-075 stageId 守卫复用注记 + FunnelAggregationEngine 引擎）
> Audit Report: `docs/audits/2026-08-05-1830-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.30 给出 UC 清单 = `UC-CRM-06/13`（2 UC），含 `use-cases.md:113/:300` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。CRM 域首批 RC 切片为 A1.28/A1.29（均 done），本切片为 CRM 域第三个（末个）RC 切片，完成后 CRM 域 MA1 全 done。

- **L1 需求契约（权威真相源）**：`docs/design/crm/use-cases.md`（机制见 `crm/README.md §ErpCrmStage`、`crm/cpq.md`、`crm/state-machine.md §2`）：
  - UC-CRM-06 漏斗阶段推进（`:113`）：`docStatus==QUALIFIED 且 leadType==OPPORTUNITY` → stageId 只能沿 `ErpCrmStage.sequence` **递增前移**（`newStage.sequence > currentStage.sequence` 允许；`<=` 拒绝，不可跳级回退）；记录 `ErpCrmLeadConvLog(fromStageId, toStageId, changedAt, changedBy)`；`isWonStage==true` 允许触发 UC-CRM-03（转化）；stageId 变更不修改 docStatus（仍 QUALIFIED）。
  - UC-CRM-13 CPQ 配置-定价-报价（`:300`）：管理员建 `ErpCrmProductConfigurator(isActive, productType)` + configLines + wizardLayout；用户按步选特征 → 每步触发 `ErpCrmConfigRule` 规则引擎（REQUIRED→必选 / EXCLUDED→禁用 / RECOMMENDED→推荐，UI 即时更新可选列表）；配置完成 → 应用 `ErpCrmPriceRule`（VOLUME/PROMOTIONAL/CUSTOMER_SPECIFIC 优先级）+ 可选 `ErpCrmBundlePricing`（捆绑包）→ 生成**配置快照(JSON)**；生成报价 → 调用 `IErpSalQuotationBiz.createFromConfig(leadId, configSnapshot, bundlePricingId?, priceRuleIds?)` → 创建 `ErpSalQuotation` + 回写 `lead.relatedBillType/Code`。

- **L3 代码实现现状（实测）**——2 UC 均已实现且测试强，**无功能完全缺失级缺口**：
  - **UC-CRM-06 漏斗阶段推进**（✅ 已实现 & 强测，P1-MA2-075 fixed R1.24）：`ErpCrmLeadProcessor.java:86-105 validateStageDirection`（stageId 单向递增守卫——fromStageId 非 null 时比较 from/to sequence，回退 `toSeq<fromSeq` 在 STRICT 模式 `ErpCrmConfigs.allowStageBackward()=false`[默认] 抛 `ErpCrmErrors.ERR_STAGE_BACKWARD_MOVE`，allow-backward=true 时 LOG.warn 放行保留 convLog 审计）+ `:152 doMoveStage`（写 convLog 全量留痕）+ `:201-203` 首阶段取 sequence 升序首条；自包含编排 `processor/ErpCrmLeadMoveStageProcessor.java`；ConvLog 持久化 `ErpCrmLeadConvLogBizModel.java`（CrudBizModel）+ 实体 `ErpCrmLeadConvLog`（_gen 含 fromStageId/toStageId/changedAt/changedBy 字段）。**与 L1 一致**——单向递增守卫已落地（owner doc §stageId 迁移规则 + §审查提示「sequence 单向递增约束」），config-gated 回退窗口（`erp-crm.allow-stage-backward`）是 R1.24 修复时的 deliberate 决策（allow-backward 默认 false=STRICT 对齐 L1）。
  - **UC-CRM-13 CPQ 配置-定价-报价**（✅ 已实现 & 强测）：
    - 配置规则引擎 `support/ProductConfigRuleEngine.java:46 evaluate`（纯函数式：按 sequence 升序遍历 `ErpCrmConfigRule`，标记目标特征 REQUIRED/EXCLUDED/RECOMMENDED/OPTIONAL；EXCLUDED 禁用优先不被后续覆盖 `:109-114`）——**与 L1 一致**。
    - 价格规则引擎 `support/PriceRuleEngine.java:37 resolvePrice`（ruleType 优先级 CUSTOMER_SPECIFIC > PROMOTIONAL > VOLUME `:122-134`，同 ruleType 内 priority 数值小者优先，期限/数量范围/币种匹配）——**与 L1 一致**（L1 要求 VOLUME/PROMOTIONAL/CUSTOMER_SPECIFIC 优先级）。
    - 捆绑定价 `support/BundlePricingCalculator.java:33 calculate`（bundleAmount 手工覆盖 / PERCENTAGE / FIXED，FIXED 不低于 0）——**与 L1 一致**。
    - 配置→定价→报价跨域链路 `processor/ErpCrmProductConfiguratorGenerateQuoteProcessor.java:38-122`（配置规则评估 → 捆绑/价格规则/标准定价 `:76-90` → buildConfigSnapshot `:70` 生成 JSON 快照 → 跨域建报价单 `IErpSalQuotationBiz.save` `:109-122` → lead 弱指针回写）；入口 `entity/ErpCrmProductConfiguratorBizModel.java:36-43 generateQuote(@BizMutation)` 委托 processor；注入 `IErpSalQuotationBiz quotationBiz`（`:47`，跨域 Facade）。
    - 实体全存在：`ErpCrmProductConfigurator`/`ErpCrmConfigRule`/`ErpCrmPriceRule`/`ErpCrmBundlePricing`/`ErpCrmBundlePricingLine`（dao 层 + api 层全生成）。
    - **候选偏差（documented scope 待核）**：① L1 `:321` 调用方法名为 `IErpSalQuotationBiz.createFromConfig(...)`，实测为 `IErpSalQuotationBiz.save(...)`——契约方法名漂移（行为等价，须核 IErpSalQuotationBiz 是否有 createFromConfig 别名）；② L1 `:313`「UI 即时更新可选列表」= 前端配置向导交互，后端规则引擎就绪但 AMIS wizard 页面属前端 successor（与 A1.28/A1.29 同型前端可视化 successor）；③ configSnapshot JSON 是否持久化到 lead/quotation（L1 仅要求「生成配置快照(JSON)」供报价，实测 `:70` buildConfigSnapshot 传入 quotation 创建，须核落库去向）。

- **L4 测试证据现状**（`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/`）：
  - UC-CRM-06：`TestErpCrmStageDirectionGuard.java`（testBackwardMoveRejectedByDefault:51 / testForwardMoveSucceedsWithConvLog:67 / testFirstFunnelEntrySkipsDirectionCheck:84 / testEqualSequenceForwardSucceeds:96——**强**，断言 ERR_STAGE_BACKWARD_MOVE + convLog 留痕）+ `TestErpCrmStageDirectionGuardAllowBackward.java`（testBackwardMoveAllowedWithConvLog:52——config-gated 回退路径）。
  - UC-CRM-13：`TestProductConfigRuleEngine.java`（9 @Test：testRequiredRule/testExcludedOverridesRecommended/testExcludedNotOverriddenByLaterRecommended/testSequenceOrdering 等——**强**）+ `TestPriceRuleEngine.java`（9 @Test：testRuleTypePriorityCustomerSpecificWins/testPriorityTieBreakerLowerWins/testPeriodExpired/testQuantityRangeBoundary/testCurrencyMismatch 等——**强**）+ `TestBundlePricingCalculator.java`（8 @Test：testPercentageDiscount/testFixedDiscount/testBundleAmountOverride/testFixedDiscountNotNegative 等——**强**）+ `TestErpCrmCpqGenerateQuote.java`（8 @Test：testGenerateQuoteViaBundlePricing:77/testGenerateQuoteViaPriceRule:108/testInactiveConfiguratorRejected:133/testNoPriceMatchedRejected:148/testNoPriceContextRejected:164——**强**，断言跨域建报价单 + 定价路径 + 异常拒绝）。
  - **⚠️ 潜在测试缺口 / 边界观察**：①UC-CRM-06 L1 `:122`「`newStage.sequence <= currentStage.sequence → 拒绝（不可跳级回退）」要求**等值拒绝**，但实测 `ErpCrmLeadProcessor.validateStageDirection:99` 仅守卫 `toSeq < fromSeq`（**等值放行**），且 `TestErpCrmStageDirectionGuard.testEqualSequenceForwardSucceeds:96` 主动断言等值**成功**——L1 `<=` vs 代码 `<` 边界分歧（Phase 1 逐字 L1 对照将裁决，倾向 P2 边界）；②configSnapshot JSON 落库去向断言、lead 弱指针回写（relatedBillType/Code）断言强度——审计复核 L4 断言是否覆盖 L1 每条验收标准；③候选缺口④（isWonStage→UC-CRM-03 触发）与 A1.28 `P1-RC-034`（convertToQuotation 不查 isWonStage）共享控制点，Phase 2 arm-index grep 须交叉引用。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14）：crm stageId 单向递增守卫 **P1-MA2-075 resolved R1.24**（UC-CRM-06 直接相关——缺口已 CLOSED，实测 `ErpCrmLeadProcessor.validateStageDirection:86-105` 现读 sequence 比较抛 ERR_STAGE_BACKWARD_MOVE + config-gated allow-stage-backward）。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）：crm PriceRuleEngine 代码质量 PASS（UC-CRM-13 直接相关）。
  - `docs/audits/2026-08-05-1100-rc-ma1-a1-29-...`（A1.29）：FunnelAggregationEngine 按 sequence 排序假设 monotonic progression——UC-CRM-06 阶段回退经 config-gated allow-backward 默认 false 已守卫（A1.29 已注记 stage 守卫 resolved，本切片复核不复开）。
  - **无既有 MA2/MA4/MA5 报告审计 UC-CRM-13 CPQ 跨域建报价链路**——本切片 CPQ 需求视角为新发现增量（无 MA2 报告覆盖 CPQ createFromConfig/configSnapshot/弱指针回写）。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用 P1-MA2-075（UC-CRM-06 守卫已修 R1.24）+ A4.5（PriceRuleEngine 代码质量 PASS），只补需求视角差异（UC-CRM-06 convLog 字段完整性 / isWonStage→UC-CRM-03 触发链 / UC-CRM-13 createFromConfig 方法名漂移 + configSnapshot 落库 + 弱指针回写 + 前端 wizard successor）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-075`（stageId 单向递增守卫 **UC-CRM-06 resolved R1.24**）、`P1-MA1-009`（crm DECIMAL↔double MR1 建议 P2 触 stage probability 字段非本切片）、`P2-MA4-013`（crm Forecast stageName stub watch-only 触 UC-10/15 非本切片）、`P2-MA4-020`（crm badge 漂移 watch-only 视图层非本切片）。**RC 系列对 crm stage/CPQ 为零**（A1.28 覆盖 UC-CRM-01/02/03/04/09/11、A1.29 覆盖 UC-CRM-05/07/08/10/12/14/15，均未触 UC-CRM-06/13）。本切片须 grep arm-index crm stage/convLog/CPQ/configurator/priceRule/bundlePricing 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1。本切片候选偏差（createFromConfig 方法名漂移 / configSnapshot 落库 / 弱指针回写）若确认为真实分歧，修复均属**代码逻辑**类（预授权——BizModel/Processor 调整，不涉及 ORM 结构变更）；前端 wizard successor 属 P2 登记不强制。

- **剩余差距**：A1.30 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.30 报告并登记 finding，完成后 CRM 域 MA1 三切片（A1.28/A1.29/A1.30）全 done。

## Goals

- 产出 A1.30 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`，含方法论 §6 **9 段全部内容**。
- 对 2 UC（UC-CRM-06/13）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并、禁止跳号。
- 对候选缺口给出分级结论：①UC-CRM-06 convLog 四字段（fromStageId/toStageId/changedAt/changedBy）完整性（倾向**接受**复核）；②UC-CRM-06 isWonStage==true→UC-CRM-03 转化触发链是否由 stage 推进自动守卫（倾向**接受/P2** 复核）；③UC-CRM-13 `createFromConfig` vs `save` 契约方法名漂移（倾向**P2** 行为等价）；④UC-CRM-13 configSnapshot JSON 落库去向（倾向**P2** 复核断言强度）；⑤UC-CRM-13 lead 弱指针回写（relatedBillType/Code）断言强度（倾向**P2**）；⑥UC-CRM-13 前端配置向导 wizard（倾向**P2 successor** watch-only）——按 §2 判据定级，若为 P0/P1 则新建 `P1-RC-xxx`（自 P1-RC-040 起）并按 §10 触发 MR1（本计划仅登记，不实施修复）；UC-CRM-06 主路径已实现且 P1-MA2-075 resolved → 复核接受。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/README.md/cpq.md/state-machine.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.28 crm-F1 / A1.29 crm-F2 独立 done；A1.30 只覆盖 UC-CRM-06/13）。
- **不复审 UC-CRM-03 商机→报价单转化**（属 A1.28 done，本切片仅核 isWonStage→UC-CRM-03 的触发守卫是否存在，不重审转化主路径）。
- **不重跑 P1-MA2-075 stageId 守卫行为审计**（§去重协议：UC-CRM-06 守卫由 P1-MA2-075 resolved R1.24，只补需求视角差异[convLog 字段/isWonStage 触发]）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不实现前端 CPQ 配置向导**（属 successor，登记不强制）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.30 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.30 UC 锚点）+ `docs/design/crm/use-cases.md`（L1 真相源）+ `docs/design/crm/README.md`+`cpq.md`+`state-machine.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/A4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-crm/erp-crm-service -Dtest=TestErpCrmStageDirectionGuard,TestErpCrmStageDirectionGuardAllowBackward,TestProductConfigRuleEngine,TestPriceRuleEngine,TestBundlePricingCalculator,TestErpCrmCpqGenerateQuote`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-05-1830-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-CRM-06/13 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:113/:300` 验收标准原文；L2 引用 `crm/README.md §ErpCrmStage`+`cpq.md §业务规则 §配置规则引擎 §价格规则引擎`+`state-machine.md §2`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpCrmLeadProcessor.java`/`ErpCrmLeadMoveStageProcessor.java`/`ErpCrmLeadConvLogBizModel.java`/`ProductConfigRuleEngine.java`/`PriceRuleEngine.java`/`BundlePricingCalculator.java`/`ErpCrmProductConfiguratorGenerateQuoteProcessor.java`/`ErpCrmProductConfiguratorBizModel.java`（含行号）；L4 引用对应 `Test*.java#method`（注明断言强度）；L5 复用 A2.14（P1-MA2-075 resolved）+ A4.5 + E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-CRM-06 sequence 单向递增守卫（validateStageDirection:86-105 + ERR_STAGE_BACKWARD_MOVE，P1-MA2-075 resolved R1.24 复核）；②UC-CRM-06 convLog 四字段完整性（ErpCrmLeadConvLog 实体 fromStageId/toStageId/changedAt/changedBy 是否全写入）；③UC-CRM-06 stageId 变更不修改 docStatus（doMoveStage 是否仅 setStageId 不触 docStatus）；④UC-CRM-06 isWonStage==true→UC-CRM-03 触发链（推进至 won stage 是否守卫/提示转化，还是仅数据标记）；⑤UC-CRM-13 配置规则引擎 REQUIRED/EXCLUDED/RECOMMENDED（ProductConfigRuleEngine:46 evaluate，EXCLUDED 禁用优先:109-114）；⑥UC-CRM-13 价格规则 VOLUME/PROMOTIONAL/CUSTOMER_SPECIFIC 优先级（PriceRuleEngine:122-134）；⑦UC-CRM-13 捆绑定价（BundlePricingCalculator:33）；⑧UC-CRM-13 configSnapshot JSON 生成与落库（buildConfigSnapshot:70 + quotation 字段）；⑨UC-CRM-13 跨域建报价单（IErpSalQuotationBiz.save vs L1 createFromConfig 方法名漂移，:109-122）；⑩UC-CRM-13 lead 弱指针回写（relatedBillType/Code）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：UC-CRM-06 主路径已实现 + P1-MA2-075 resolved → 倾向**接受**（convLog/isWonStage 触发若有偏差则 P2）；UC-CRM-13 配置/定价/报价主路径已实现 → 倾向**接受**（createFromConfig 方法名漂移/configSnapshot 落库/弱指针回写断言/前端 wizard 若为真实分歧则 P2 successor）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-CRM-06/13 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.14/A4.5 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 ①-⑥ 有明确分级（非悬空"待查"）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-05-1830-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` crm stage/convLog/CPQ/configurator/priceRule/bundlePricing 同域同控制点后裁决——UC-CRM-06 stageId 守卫已由 P1-MA2-075 resolved→复用注记（不重开）；UC-CRM-13 候选偏差为**新发现**（既有 arm-index 无 RC finding 涉及 crm CPQ 跨域建报价/configSnapshot）→ 若确认为分歧则新建 `P1-RC-xxx`（自 P1-RC-040 起）/ `P2-RC-xxx`（自 P2-RC-036 起）列明差异依据。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 allow-stage-backward=true 放行时 FunnelAggregationEngine 报表 sequence 排序偏差实际值、configSnapshot JSON 实际落库字段与 quotation 关联、generateQuote 弱指针回写的实际 relatedBillType 枚举值等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14 P1-MA2-075 UC-CRM-06 stageId 守卫 resolved R1.24）+ A4.5（PriceRuleEngine 代码质量 PASS），列明只补的需求视角差异（UC-CRM-06 convLog 字段/isWonStage 触发 / UC-CRM-13 createFromConfig 方法名漂移 + configSnapshot 落库 + 弱指针回写 + 前端 wizard successor）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；audit reports 表新增 A1.30 行。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding（若有）已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_030db9742ffegKSVTFCP8CUGBb，fresh session，未起草本计划）。live-baseline 全量独立复核 CONFIRMED——L1 锚点精确（UC-CRM-06 `:113` / UC-CRM-13 `:300` 验收标准逐字匹配）；L3 代码/行号确认（validateStageDirection:91-110 + ERR_STAGE_BACKWARD_MOVE:105 + allowStageBackward config-gate:100 / doMoveStage:152-157 / convLog 四字段:163-166 / ProductConfigRuleEngine:46 + EXCLUDED 优先:109-117 / PriceRuleEngine priority:125-139 / BundlePricingCalculator:33 / GenerateQuoteProcessor IErpSalQuotationBiz.save:123 + 弱指针回写:126-130 / BizModel @BizMutation:36-45）；**createFromConfig 漂移确认为真**（IErpSalQuotationBiz 接口无 createFromConfig，仅 save）；L4 测试文件/方法均存在；P1-MA2-075 resolved R1.24 + 新 finding 计数器（P1-RC-040/P2-RC-036）正确；scope/methodology/closure-gate（只读审计）全 PASS。4 项非阻塞观察已记录并据以微调：①UC-CRM-06 L1 `<=`（等值拒绝）vs 代码 `<`（等值放行）边界 + testEqualSequenceForwardSucceeds:96 断言分歧——已补入候选观察；②TestPriceRuleEngine 11→9、TestBundlePricingCalculator 9→8 @Test 计数已修正；③`IErpCrmQuotationBiz` typo 已修正为 `IErpSalQuotationBiz`；④候选缺口④ isWonStage 触发与 A1.28 P1-RC-034 共享控制点——已补交叉引用。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.30 报告 9 段齐全 + 2 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.30 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（createFromConfig 方法名/configSnapshot 落库/弱指针回写）均属**代码逻辑**类（预授权——BizModel/Processor 调整，不涉及 ORM 结构变更）；前端 wizard successor 属 P2 登记不强制。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: 执行者（本会话）完成 Phase 1 + Phase 2 全部 12 个已勾选项 + 5 个 Exit Criteria 已勾选。产出审计报告 `docs/audits/2026-08-05-1830-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`（9 段齐全）+ arm-index 新增 4 项 P2 finding（P2-RC-036/037/038/039）+ audit reports 表 A1.30 行 + RC 交叉引用注记。roadmap A1.30 todo→done（CRM 域 MA1 三切片 A1.28/A1.29/A1.30 全 done）。本计划为只读审计（零代码/ORM/api.xml/view.xml/真相源变更），Phase 内未跑 mvn build/test（per Closure Gates 删除完整仓库 typecheck/build/lint/test 验证命令门控的裁决），验证 = 报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录（全 19 规则 actual ≤ baseline 精确匹配）+ finding 复用/新增裁决可追溯。**零 P0/零 P1/4 项新 P2 watch-only**（等值边界 / 前端 wizard successor / createFromConfig 方法名漂移 / configSnapshot 落库断言弱，均属预授权类目不触发 §5 ask-first）。Closure Gates 已由独立结束审计子代理（新会话）勾选确认——执行者不自我审计，未将 Closure Gates 留为未勾选状态由执行者自审。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，fresh session，未执行本计划任何 Phase 1/Phase 2 工作，不重用执行者上下文）
- Evidence: 独立结束审计 walkthrough record（2026-08-05）——
  - **交付物存在性核实**：审计报告 `docs/audits/2026-08-05-1830-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`（323 行）落盘；`docs/audits/arm-index.md` 含 A1.30 audit reports 表行（:98）+ 4 项新 P2 finding 行（P2-RC-036/037/038/039 :180-183）+ RC 交叉引用注记（:189）。
  - **报告 9 段完整性核实**：`grep "^## " 报告` 确认 §1-§9 全存在（§1 L1 逐字引用 :13 / §2 L3 代码路径 :85 / §3 L4 测试 :125 / §4 L5 运行时 :160 / §5 符合性结论矩阵 :173 / §6 arm-index 衔接 :209 / §7 静态存疑点 :239 / §8 过程纪律自检 :252 / §9 MA2 差异增量 :281）+ 段落完整性自检表（:299）+ 整体裁决（:315）。
  - **anti-hollow 实仓代码复核（live repo 抽样）**：①`ErpCrmLeadProcessor.java:99` 字面 `toSeq < fromSeq`（严格小于，等值放行）+ `:100` config-gated `allowStageBackward()` + `:105` 抛 `ERR_STAGE_BACKWARD_MOVE`——与报告 P2-RC-036 等值边界 finding 描述精确一致；②`ErpCrmProductConfiguratorGenerateQuoteProcessor.java:123` 字面 `quotationBiz.save(quotationData, context)`（ICrudBiz.save，非 L1 字面 createFromConfig）+ `:127-128` `setRelatedBillType(SALES_QUOTATION)`+`setRelatedBillCode` 弱指针回写——与报告 P2-RC-038 方法名漂移 finding + ⑫弱指针回写描述精确一致；③`IErpSalQuotationBiz.java:19` `extends ICrudBiz<ErpSalQuotation>` 无 createFromConfig 方法——确认方法名漂移成立。报告 L3 行号引用全部命中实仓真实代码，无空头引用。
  - **2 UC 矩阵行覆盖核实**：§5 符合性结论含 UC-CRM-06（接受 on ①②③⑤⑥⑦ + P2 on ④ 等值边界）+ UC-CRM-13（接受 on ①②③④⑤⑦⑧⑨⑩⑪⑫ + P2 on ⑥⑩-方法名漂移⑪），每 UC 一矩阵行，零 P0/零 P1，4 项新 P2 全部 watch-only successor。
  - **finding arm-index 衔接核实**：§6 grep arm-index crm stage/convLog/CPQ/configurator/priceRule/bundlePricing 同域同控制点，确认 4 项新 finding 与既有 RC finding 零重叠（A1.28/A1.29 未触 UC-CRM-06/13），P1-MA2-075 复用注记 + P1-RC-034 共享控制点互补注记，禁止未经比对新建规则已遵守。
  - **§8 过程纪律自检核实**：checker actual vs baseline 实测表 19 规则全部 actual ≤ baseline 精确匹配（R1d 14/14、R2c 1382/1382、R12a-c 69/66/40 等），closure-audit 独立性声明 + arm-index 交叉去重声明均存在，区分 checker reporter 退出码 vs CI 门控退出码（不以 reporter 退出码 0 为门控通过依据）。
  - **零代码变更核实**：本审计为只读审计，Closure Gates 已按计划指南删除完整仓库 typecheck/build/lint/test 命令门控（有正当理由：无生产代码变更），验证 = 报告 9 段完整性 + §8 checker actual vs baseline + finding 可追溯。
  - **文本一致性核实**：Plan Status `completed`（:3）/ Phase 1 Status `completed`（:82）/ Phase 2 Status `completed`（:103）/ 两阶段 Exit Criteria 全 `[x]`（:98-99/:127-129）/ Closure Gates 现全 `[x]` / Deferred 仅含 MR0/MR1 finding 修复（correctly out-of-scope，已 Classification）—— 全部一致，无顶部 completed 而内部 draft 的矛盾。
  - **不可降级规则核实**：候选偏差（createFromConfig 方法名漂移/configSnapshot 落 remark 截断/弱指针回写断言弱/等值边界）均登记为 P2-RC-xxx watch-only successor（非 P0/P1 缺陷），finding 修复属 MR0/MR1 实施义务明确移出本审计范围并 Classification——无范围内项目静默降级为 deferred/follow-up。
  - **裁决**：所有 Closure Gates 门控满足，结束审计通过。CRM 域 MA1 三切片（A1.28/A1.29/A1.30）全 done。

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围。本切片 4 项 P2 successor watch-only 不强制，登记供后续 roadmap 处理。
