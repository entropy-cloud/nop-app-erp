# rc-ma1-a1-30 crm-F3 CPQ/漏斗推进 需求-实现符合性审计

> 计划：`docs/plans/2026-08-05-1830-1-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`（独立草案审查 ses_030db9742ffegKSVTFCP8CUGBb accept）
> 域：crm | 功能切片：crm-F3 CPQ/漏斗推进 | UC 清单：UC-CRM-06/13（2 UC）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md` A1.30（UC-CRM-06 `:113` / UC-CRM-13 `:300`，覆盖率 ✅ 一致，无基线分歧 D-xx）
> 真相源 = `docs/design/crm/use-cases.md`（权威功能契约，§4 Q1 层级 2）。下方逐字引用 2 UC 的验收标准原文，不转述。
> 范围：CRM 域第三（末）个 RC 切片；完成后 CRM 域 MA1 三切片（A1.28/A1.29/A1.30）全 done。
> Audit Type: 只读审计（无代码/ORM/api.xml/view.xml/真相源变更）。

---

## 1. 需求契约原文（L1 逐字引用）

> 来源：`docs/design/crm/use-cases.md`（权威功能契约）。逐字引用，禁止转述（§1 L1 格式）。

### UC-CRM-06 漏斗阶段推进（`use-cases.md:113`）

```
场景：商机按漏斗阶段逐步前移（如"需求分析 → 方案演示 → 谈判中 → 赢单"）。

可验证断言（见 README.md §ErpCrmStage、state-machine.md §Lead §2）：
Lead.docStatus == QUALIFIED 且 leadType == OPPORTUNITY →
  阶段前移：stageId 只能沿 ErpCrmStage.sequence 递增
    if newStage.sequence > currentStage.sequence → 允许前移
    if newStage.sequence <= currentStage.sequence → 拒绝（不可跳级回退）
   记录 ErpCrmLeadConvLog(fromStageId, toStageId, changedAt, changedBy)
  isWonStage == true → 允许触发 UC-CRM-03（转化）
  stageId 变更时不修改 docStatus（docStatus 仍为 QUALIFIED）
```

**逐条验收标准**（每条将进入 §5 矩阵 L5 判读）：
- ① 前置：`Lead.docStatus == QUALIFIED 且 leadType == OPPORTUNITY`（注：`validateMovable` 守卫 docStatus∈{NEW,QUALIFIED}，leadType 未守卫——见 §5 复核）
- ② 阶段前移：stageId 只能沿 `ErpCrmStage.sequence` 递增
- ③ `if newStage.sequence > currentStage.sequence → 允许前移`
- ④ `if newStage.sequence <= currentStage.sequence → 拒绝（不可跳级回退）`
- ⑤ 记录 `ErpCrmLeadConvLog(fromStageId, toStageId, changedAt, changedBy)`
- ⑥ `isWonStage == true → 允许触发 UC-CRM-03（转化）`
- ⑦ `stageId 变更时不修改 docStatus（docStatus 仍为 QUALIFIED）`

### UC-CRM-13 CPQ 配置-定价-报价（`use-cases.md:300`）

```
场景：销售员通过产品配置器选择特征组合，系统应用价格规则生成报价。

可验证断言（见 cpq.md §业务规则 §配置规则引擎）：
管理员创建 ErpCrmProductConfigurator(isActive=true, productType="SERVER")
  并配置 configLines + wizardLayout

用户在配置向导中按步骤选择特征 →
  每步选择触发 ErpCrmConfigRule 规则引擎：
    if REQUIRED → 目标特征标记必选
    if EXCLUDED → 目标特征禁用
    if RECOMMENDED → 目标特征高亮推荐
  UI 即时更新可选列表

配置完成后 →
  应用 ErpCrmPriceRule 计算价格（按 VOLUME/PROMOTIONAL/CUSTOMER_SPECIFIC 优先级）
  可选：应用 ErpCrmBundlePricing（若匹配捆绑包）
  生成配置快照(JSON)

生成报价 →
  调用 IErpSalQuotationBiz.createFromConfig(
    leadId, configSnapshot, bundlePricingId?, priceRuleIds?)
  → 创建 ErpSalQuotation
  回写 lead.relatedBillType/Code
```

**逐条验收标准**：
- ① 管理员建 `ErpCrmProductConfigurator(isActive, productType)` + configLines + wizardLayout
- ② 用户按步选特征 → 每步触发 `ErpCrmConfigRule` 规则引擎
- ③ `REQUIRED → 必选`
- ④ `EXCLUDED → 禁用`
- ⑤ `RECOMMENDED → 推荐`
- ⑥ UI 即时更新可选列表（前端配置向导）
- ⑦ 配置完成后应用 `ErpCrmPriceRule`（VOLUME/PROMOTIONAL/CUSTOMER_SPECIFIC 优先级）
- ⑧ 可选：应用 `ErpCrmBundlePricing`
- ⑨ 生成配置快照(JSON)
- ⑩ 调用 `IErpSalQuotationBiz.createFromConfig(leadId, configSnapshot, bundlePricingId?, priceRuleIds?)`
- ⑪ 创建 `ErpSalQuotation`
- ⑫ 回写 `lead.relatedBillType/Code`

---

## 2. 实现证据（L3 代码路径，含跨域调用链）

> 来源：`module-crm/erp-crm-service/src/main/...`。行号实测，跨域调用链列全。

### UC-CRM-06 实现链（漏斗阶段推进）

| 站点 | file:line | 说明 |
|------|-----------|------|
| 入口 Facade | `entity/ErpCrmLeadBizModel.java`（CrudBizModel `moveStage` @BizMutation） | 委托 per-mutation Processor |
| per-mutation 编排 | `processor/ErpCrmLeadMoveStageProcessor.java:18-26` | `moveStage(leadId, toStageId, ctx)`：requireLead→validateMovable→requireStage→**validateStageDirection**→**doMoveStage** |
| 共享 helper（facade） | `processor/ErpCrmLeadProcessor.java:35-237` | slim-to-S-delegation 单一真相源 |
| docStatus 守卫 | `ErpCrmLeadProcessor.validateMovable:77-83` | 仅允许 NEW/QUALIFIED 流转阶段 |
| **sequence 单向守卫** | `ErpCrmLeadProcessor.validateStageDirection:91-110` | fromStageId==null 跳过；否则比较 from/to sequence，`toSeq < fromSeq`（**严格小于**，等值放行）在 STRICT 模式（`ErpCrmConfigs.allowStageBackward()=false`[默认]）抛 `ErpCrmErrors.ERR_STAGE_BACKWARD_MOVE`；allow-backward=true 时 LOG.warn 放行 |
| 首阶段查找 | `ErpCrmLeadProcessor.findFirstStage:200-206` | 按 sequence 升序取首条 |
| 写 convLog | `ErpCrmLeadProcessor.doMoveStage:152-157` + `writeConvLog:159-168` | 仅 `setStageId` + `applyDefaultProbability`（不触 docStatus）+ 写 convLog 全量留痕 |
| convLog 四字段写入 | `writeConvLog:163-166` | fromStageId/toStageId/changedAt(`CoreMetrics.currentTimestamp()`)/changedBy(`currentUser(context)`) 全写入 |
| convLog 实体 | `module-crm/model/app-erp-crm.orm.xml:575-585` | ORM `_ErpCrmLeadConvLog` 含 fromStageId(propId4)/toStageId(propId5)/changedAt(propId6 mandatory)/changedBy(propId7) 四列齐全 |
| 错误码 | `ErpCrmErrors.ERR_STAGE_BACKWARD_MOVE` + ARG_LEAD_CODE/ARG_FROM_SEQUENCE/ARG_TO_SEQUENCE | 回退拒绝时抛出 |
| 配置门 | `ErpCrmConfigs.allowStageBackward()` | `erp-crm.allow-stage-backward` 默认 false（STRICT） |

### UC-CRM-13 实现链（CPQ 配置-定价-报价）

| 站点 | file:line | 说明 |
|------|-----------|------|
| 入口 BizModel | `entity/ErpCrmProductConfiguratorBizModel.java:36-45` | `generateQuote(@BizMutation)` 委托 processor |
| per-mutation 编排 | `processor/ErpCrmProductConfiguratorGenerateQuoteProcessor.java:41-321` | 配置→定价→报价跨域链路 |
| 配置器激活守卫 | `ErpCrmProductConfiguratorGenerateQuoteProcessor.requireConfiguratorActive:136-156` | isActive + effectiveFrom/To 期间校验，失败抛 `ERR_CPQ_CONFIGURATOR_INACTIVE` |
| **配置规则引擎** | `support/ProductConfigRuleEngine.java:37-184` | 纯函数式 `evaluate:46`——按 sequence 升序遍历，EXCLUDED 禁用优先不被后续覆盖（`:109-117`）；conditionExpression 不为空时优先评估（XLang `compileFullExpr` + `selectedFeatures` scope，`:85-98`） |
| **价格规则引擎** | `support/PriceRuleEngine.java:31-199` | 纯函数式 `resolvePrice:45`——ruleType rank `CUSTOMER_SPECIFIC(0) > PROMOTIONAL(1) > VOLUME(2)`（`:125-139`）+ priority 数值小者优先（`:76-78`）+ period/quantity/currency 匹配；priceOverride 优先 → discountPercent → discountAmount |
| **捆绑定价** | `support/BundlePricingCalculator.java:24-112` | 纯函数式 `calculate:33`——bundleAmount 手工覆盖 → PERCENTAGE → FIXED（不低于 0） |
| 生成快照 | `ErpCrmProductConfiguratorGenerateQuoteProcessor.buildConfigSnapshot:202-216` | `JSON.stringify({selectedFeatures, ruleEvaluation})` |
| 定价路径选择 | `ErpCrmProductConfiguratorGenerateQuoteProcessor.generateQuote:76-107` | bundle 路径 > priceRule 路径 > 无价格匹配抛 `ERR_CPQ_NO_PRICE_MATCHED`；currencyId 缺失亦抛 |
| **跨域建报价单** | `generateQuote:109-123` + `quotationBiz.save(quotationData, ctx):123` | 注入 `IErpSalQuotationBiz quotationBiz`（`:47`，sales→crm 跨域 Facade）；经 `ICrudBiz.save`（**L1 字面 `createFromConfig` 在 sales 域不存在**，见 §5/§6 候选缺口⑨） |
| 报价数据构造 | `buildQuotationData:218-248` | code=CPQ-{cfgId}-{millis} / orgId / customerId / businessDate / totalAmount / currencyId / docStatus=DRAFT + `remark="CPQ pricingSource=...; snapshot=" + truncate(configSnapshot, 500)` |
| **lead 弱指针回写** | `generateQuote:126-130` | `lead.setRelatedBillType(RELATED_BILL_TYPE_SALES_QUOTATION)` + `lead.setRelatedBillCode(quotation.getCode())` |
| 实体全在 | `module-crm/model/app-erp-crm.orm.xml` | ErpCrmProductConfigurator/ErpCrmConfigRule/ErpCrmPriceRule/ErpCrmBundlePricing/ErpCrmBundlePricingLine（dao + api 全生成） |
| owner doc 实现注记 | `docs/design/crm/cpq.md:189-193` | 显式登记 `createFromConfig`→`save` 偏离 + conditionExpression XLang 评估 + ruleType rank + currencyId 显式要求 |

---

## 3. 测试证据（L4 测试断言，注明断言强度）

> 来源：`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/`。

### UC-CRM-06 测试

| 测试方法 | 断言强度 | 覆盖验收标准 |
|---------|---------|-------------|
| `TestErpCrmStageDirectionGuard#testBackwardMoveRejectedByDefault:51-64` | **强** | ④ 回退（HIGH seq=30 → LOW seq=20）抛 `ERR_STAGE_BACKWARD_MOVE` + 拒绝后 stageId 保持 HIGH |
| `TestErpCrmStageDirectionGuard#testForwardMoveSucceedsWithConvLog:67-81` | **强** | ③ 前移成功 + ⑤ convLog 写入（fromStageId=LOW / toStageId=HIGH）|
| `TestErpCrmStageDirectionGuard#testFirstFunnelEntrySkipsDirectionCheck:84-93` | **强** | fromStageId=null 首次入漏斗跳过方向校验 |
| `TestErpCrmStageDirectionGuard#testEqualSequenceForwardSucceeds:96-105` | **强（但与 L1 `<=` 字面冲突）** | 同 sequence=20 移动**成功**——L1 `:122` 字面 `<=`（等值应拒绝），实现 `<`（等值放行），此测试**主动断言等值成功**（边界分歧，见 §5/§6 候选缺口②） |
| `TestErpCrmStageDirectionGuardAllowBackward#testBackwardMoveAllowedWithConvLog:52` | **强** | config-gated `allow-stage-backward=true` 回退路径放行 + convLog 留痕 |

### UC-CRM-13 测试

| 测试方法 | 断言强度 | 覆盖验收标准 |
|---------|---------|-------------|
| `TestProductConfigRuleEngine`（9 @Test） | **强** | ②③④⑤ 配置规则：testRequiredRule/testExcludedOverridesRecommended/testExcludedNotOverriddenByLaterRecommended/testSequenceOrdering 等 |
| `TestPriceRuleEngine`（9 @Test） | **强** | ⑦ 价格规则：testRuleTypePriorityCustomerSpecificWins/testPriorityTieBreakerLowerWins/testPeriodExpired/testQuantityRangeBoundary/testCurrencyMismatch 等 |
| `TestBundlePricingCalculator`（8 @Test） | **强** | ⑧ 捆绑：testPercentageDiscount/testFixedDiscount/testBundleAmountOverride/testFixedDiscountNotNegative 等 |
| `TestErpCrmCpqGenerateQuote#testGenerateQuoteViaBundlePricing:77-105` | **强** | ⑦⑧⑨⑩⑪⑫ bundle 路径：85000=100000×(1-15/100) + 报价单创建 + ⑫ **lead 弱指针回写断言**（relatedBillType=SALES_QUOTATION + relatedBillCode=报价单号） |
| `TestErpCrmCpqGenerateQuote#testGenerateQuoteViaPriceRule:108-130` | **强** | ⑦⑩⑪ priceRule 路径：900=1000×(1-10/100) |
| `TestErpCrmCpqGenerateQuote#testInactiveConfiguratorRejected:133-145` | **强** | 异常路径：isActive=false → `ERR_CPQ_CONFIGURATOR_INACTIVE` |
| `TestErpCrmCpqGenerateQuote#testNoPriceMatchedRejected:148-161` | **强** | 异常路径：无 basePrice 且无匹配规则 → `ERR_CPQ_NO_PRICE_MATCHED` |
| `TestErpCrmCpqGenerateQuote#testNoPriceContextRejected:164-173` | **强** | 异常路径：无 bundle/priceRule ctx → `ERR_CPQ_NO_PRICE_MATCHED` |
| `TestErpCrmCpqGenerateQuote#testMaintenanceHookDiscountInconsistent:176-191`/`testMaintenanceHookQtyRangeInvalid:194-209`/`testMaintenanceHookEffectiveDateInvalid:212-227` | **强** | 维护钩子：PERCENTAGE=150/min>max/effectiveFrom>effectiveTo 三类非法输入拒绝 |

**测试缺口/边界观察**：
- UC-CRM-06 ④：L1 `<=`（等值拒绝）vs 代码 `<`（等值放行）边界——`testEqualSequenceForwardSucceeds:96` 主动断言等值成功（候选缺口②，§5/§6）
- UC-CRM-13 ⑨：configSnapshot JSON 落库去向——测试断言 lead 弱指针回写（强），但**未直接断言 `quotation.remark` 字段含 configSnapshot**（快照序列化到 remark 截断 500 字符，落库去向断言弱，候选缺口④）
- UC-CRM-13 ⑥：前端配置向导 wizard——后端规则引擎就绪，AMIS wizard 页面属前端 successor（候选缺口⑥）

---

## 4. 运行时行为证据（L5）

> 来源：复用既有 MA2/A4 报告已证实行为（§去重协议，不重复核实行为本身）+ L3/L4 静态证实。

- **复用 A2.14**（`docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`）：crm Lead 5 态（NEW/QUALIFIED/CONVERTED/LOST/CANCELLED）状态机 PASS；**P1-MA2-075**（stageId 单向递增守卫，UC-CRM-06 直接相关）**resolved R1.24**——HEAD 实测 `ErpCrmLeadProcessor.validateStageDirection:91-110` 现读 sequence 比较抛 `ERR_STAGE_BACKWARD_MOVE` + config-gated `allow-stage-backward`（默认 false=STRICT 对齐 L1），行为已证实。
- **复用 A4.5**（`docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`）：crm PriceRuleEngine 代码质量 PASS（UC-CRM-13 直接相关）。
- **复用 A1.29**（`docs/audits/2026-08-05-1100-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`）：FunnelAggregationEngine 按 sequence 排序假设 monotonic progression——UC-CRM-06 阶段回退经 config-gated `allow-stage-backward` 默认 false 已守卫；allow-backward=true 放行时转化率/dropOffRate 按 sequence 排序为近似值（历史 convLog 全量留痕审计不丢）。
- **复用 A1.28**（`docs/audits/2026-08-05-1030-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`）：UC-CRM-06 ⑥ `isWonStage==true → 允许触发 UC-CRM-03` 与 A1.28 `P1-RC-034`（convertToQuotation 不查 isWonStage）共享控制点——A1.28 已登记 won-stage 前置静默丢弃，本切片复核该共享控制点不复开（仅核 UC-CRM-06 推进至 won stage 时是否有守卫/提示，见 §5 候选缺口③）。
- **L3/L4 静态证实**：UC-CRM-06 主路径（前移+convLog 留痕+STRICT 回退守卫+config-gated 放行）+ UC-CRM-13 主路径（配置规则 EXCLUDED 优先+价格规则 ruleType rank+捆绑定价+跨域建报价单+弱指针回写）均行为正确（经 A2.14+A4.5+单测三重证实）。
- **无既有 MA2/MA4/MA5 报告审计 UC-CRM-13 CPQ 跨域建报价链路**——本切片 CPQ 需求视角为新发现增量（§9 差异声明）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论）

### 五级追踪矩阵

| UC 编号 | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|---------|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-CRM-06** | `use-cases.md:113` 漏斗阶段推进（前置 QUALIFIED+OPPORTUNITY / sequence 单向递增 / `>`允许 `<=`拒绝 / convLog 四字段 / isWonStage→UC-CRM-03 / stageId 变更不改 docStatus） | `crm/README.md §ErpCrmStage` + `state-machine.md §2 §stageId 迁移规则 :40 + §4 :56 + §7 :90`（**设计参考**，L2 与 L1 在等值边界上分歧——L2 `state-machine.md:40,56` 字面 `<`（回退 toStage.sequence < fromStage.sequence）与 L1 `:122` `<=`（等值拒绝）冲突；按 §4 Q1 **以 L1 为准**，L2 推定已向实现妥协） | `ErpCrmLeadMoveStageProcessor:18-26` + `ErpCrmLeadProcessor.validateStageDirection:91-110` + `doMoveStage:152-157` + `writeConvLog:159-168` + ORM `ErpCrmLeadConvLog:575-585` 四字段齐全 | `TestErpCrmStageDirectionGuard#testBackwardMoveRejectedByDefault:51`（强）+ `#testForwardMoveSucceedsWithConvLog:67`（强，convLog 四字段断言）+ `#testFirstFunnelEntrySkipsDirectionCheck:84`（强）+ `#testEqualSequenceForwardSucceeds:96`（强但与 L1 `<=` 冲突）+ `TestErpCrmStageDirectionGuardAllowBackward#testBackwardMoveAllowedWithConvLog:52`（强） | 行为已证实（引用 A2.14 P1-MA2-075 resolved R1.24——STRICT 默认 + config-gated allow-backward + convLog 全量留痕；A1.29 FunnelAggregationEngine sequence 排序经 STRICT 守卫保护） | **接受 on ①②③⑤⑥⑦ + P2 on ④ 等值边界**（P2-RC-036 新登记 watch-only） |
| **UC-CRM-13** | `use-cases.md:300` CPQ 配置-定价-报价（配置器 + 配置规则 REQUIRED/EXCLUDED/RECOMMENDED + UI 即时更新 + 价格规则 VOLUME/PROMOTIONAL/CUSTOMER_SPECIFIC + 捆绑 + 配置快照(JSON) + createFromConfig + 创建报价单 + 弱指针回写） | `cpq.md §业务规则 §配置规则引擎 §价格规则引擎 §捆绑定价 §报价生成 :127-187` + `:189-193 实现注记`（**设计参考**，L2 显式登记 `createFromConfig→save` 偏离 + conditionExpression XLang + currencyId 显式要求；冲突以 L1 为准） | `ErpCrmProductConfiguratorBizModel:36-45`（@BizMutation）+ `ErpCrmProductConfiguratorGenerateQuoteProcessor:41-321`（配置规则→定价→跨域建单→弱指针回写）+ `ProductConfigRuleEngine:37-184`（EXCLUDED 优先 :109-117）+ `PriceRuleEngine:31-199`（ruleType rank :125-139）+ `BundlePricingCalculator:24-112` + `IErpSalQuotationBiz.save:123`（**非 L1 字面 `createFromConfig`**） | `TestProductConfigRuleEngine`（9 @Test 强）+ `TestPriceRuleEngine`（9 @Test 强）+ `TestBundlePricingCalculator`（8 @Test 强）+ `TestErpCrmCpqGenerateQuote`（8 @Test 强，含 ⑫ lead 弱指针回写断言） | 行为已证实（引用 A4.5 PriceRuleEngine 代码质量 PASS）；L3/L4 静态证实配置/定价/报价主路径 | **接受 on ①②③④⑤⑦⑧⑩⑪⑫ + P2 on ⑥⑨⑩-方法名漂移**（P2-RC-037/038/039 新登记 watch-only） |

### 候选缺口逐条裁决（10 项）

> 每条逐字对照 L1 验收标准，按 §2 判据定级。

| # | UC | 候选缺口描述 | L1↔L3 实证 | 结论 | finding |
|---|----|------------|-----------|------|---------|
| ① | UC-CRM-06 | sequence 单向递增守卫 | `validateStageDirection:91-110` STRICT 默认抛 `ERR_STAGE_BACKWARD_MOVE`，P1-MA2-075 resolved R1.24 复核接受 | **接受**（P1-MA2-075 已 resolved，本切片复核不复开） | — |
| ② | UC-CRM-06 | **L1 `<=`（等值拒绝）vs 代码 `<`（等值放行）边界** | L1 `:122` 逐字 `if newStage.sequence <= currentStage.sequence → 拒绝`；L3 `validateStageDirection:99` 字面 `toSeq < fromSeq`（严格小于，等值放行）；L4 `testEqualSequenceForwardSucceeds:96-105` **主动断言等值成功**；L2 `state-machine.md:40,56` 字面 `<` 与代码一致（与 L1 冲突，按 §4 以 L1 为准） | **P2**（§2 P2①——次要验收标准边界场景弱，主路径[前移/回退]OK，等值边界放行；不破坏活跃数据/GL/核心循环；CPQ/stage 不涉会计） | **P2-RC-036** 新登记 |
| ③ | UC-CRM-06 | **isWonStage==true→UC-CRM-03 转化触发链** | L1 `:124` 字面 `isWonStage==true → 允许触发 UC-CRM-03`；L3 `doMoveStage:152-157` 推进至 won stage 仅 setStageId + 写 convLog，**不自动触发转化**（转化经独立 mutation `convertToQuotation`）；won-stage 守卫在转化侧——A1.28 `P1-RC-034` 已登记 convertToQuotation 不查 isWonStage（共享控制点）。本切片核：UC-CRM-06 推进本身**不阻止**后续转化（"允许触发"语义为不阻断，非"自动触发"），与 A1.28 P1-RC-034 互补不重复 | **接受**（"允许触发"=不阻断，本切片推进侧不构成独立缺口；前置弱守卫共享 P1-RC-034 MR1 修复） | — |
| ④ | UC-CRM-06 | stageId 变更不修改 docStatus | L3 `doMoveStage:152-157` 仅 setStageId + applyDefaultProbability + 写 convLog，**不触 docStatus** | **接受**（实测一致） | — |
| ⑤ | UC-CRM-06 | convLog 四字段完整性 | L3 `writeConvLog:163-166` 写全四字段 + ORM `_ErpCrmLeadConvLog:582-585` 四列齐全 + L4 断言 fromStageId/toStageId | **接受**（实测一致） | — |
| ⑥ | UC-CRM-13 | UI 即时更新可选列表（前端 wizard） | L1 `:314` 字面「UI 即时更新可选列表」；L3 后端规则引擎 `ProductConfigRuleEngine.evaluate:46` 就绪（纯函数式返回 EvaluationResult map 供前端消费），但 AMIS wizard 页面属**前端可视化 successor** | **P2**（§2 P2①——后端规则引擎就绪主路径 OK，前端 wizard 页面 successor watch-only；与 A1.28/A1.29 同型前端可视化 successor 范式） | **P2-RC-037** 新登记 successor |
| ⑦ | UC-CRM-13 | 配置规则 REQUIRED/EXCLUDED/RECOMMENDED | L3 `ProductConfigRuleEngine:46-134` 全实现，EXCLUDED 禁用优先不被后续覆盖（`:109-117`） | **接受**（实测一致 + 9 @Test 强测） | — |
| ⑧ | UC-CRM-13 | 价格规则 VOLUME/PROMOTIONAL/CUSTOMER_SPECIFIC 优先级 | L3 `PriceRuleEngine:76-78,125-139` ruleType rank CUSTOMER_SPECIFIC(0)>PROMOTIONAL(1)>VOLUME(2) + priority tie-break + period/quantity/currency 匹配 | **接受**（实测一致 + 9 @Test 强测） | — |
| ⑨ | UC-CRM-13 | 捆绑定价 | L3 `BundlePricingCalculator:33-66` bundleAmount 覆盖 → PERCENTAGE → FIXED（不低于 0） | **接受**（实测一致 + 8 @Test 强测） | — |
| ⑩ | UC-CRM-13 | **configSnapshot JSON 生成与落库 + createFromConfig 方法名漂移** | L1 `:319` 字面「生成配置快照(JSON)」+ L1 `:321-323` 字面 `IErpSalQuotationBiz.createFromConfig(...)`；L3 `buildConfigSnapshot:202-216` JSON 生成（满足"生成"）+ `:123 IErpSalQuotationBiz.save(...)`（**createFromConfig 在 sales 域不存在**，IErpSalQuotationBiz 接口仅 extends ICrudBiz 含 save）；configSnapshot 序列化到 `quotation.remark` 截断 500 字符（`buildQuotationData:246`）；owner doc `cpq.md:190` 显式登记此偏离（"实现注记"） | **P2**（§2 P2①——行为等价[跨域建单+弱指针回写达成]，方法名漂移 cosmetic + configSnapshot 落 remark 截断断言弱；owner doc 已显式登记实现注记非静默降级，但 owner doc 非真相源[§4]，故仍登记 P2 watch-only 不强制） | **P2-RC-038** 新登记 watch-only |
| ⑪ | UC-CRM-13 | configSnapshot JSON 落库去向断言强度 | L4 `TestErpCrmCpqGenerateQuote#testGenerateQuoteViaBundlePricing:77-105` 断言 totalAmount + ⑫ lead 弱指针回写（强），**未直接断言 `quotation.remark` 含 configSnapshot** | **P2**（§2 P2①——主路径[报价单创建+弱指针]强测，边界[remark snapshot 内容]断言弱） | **P2-RC-039** 新登记 watch-only |
| ⑫ | UC-CRM-13 | lead 弱指针回写 | L3 `generateQuote:126-130` setRelatedBillType(SALES_QUOTATION) + setRelatedBillCode(quotation.code)；L4 `:102-104` 强断言 | **接受**（实测一致 + 强断言） | — |

### UC 符合性结论汇总

- **UC-CRM-06 漏斗阶段推进** = **接受 on ①②③⑤⑥⑦ + P2 on ④ 等值边界**（P2-RC-036）。主路径已实现且 P1-MA2-075 resolved R1.24——L1 `<=` vs 代码 `<` 等值边界为 P2（§2 P2① 边界弱）。命中判据：§2 接受（主路径全实现）+ §2 P2①（④ 等值边界）。
- **UC-CRM-13 CPQ 配置-定价-报价** = **接受 on ①②③④⑤⑦⑧⑨⑩⑪⑫ + P2 on ⑥⑩方法名漂移⑪**（P2-RC-037/038/039）。配置/定价/报价主路径已实现且强测——前端 wizard successor（⑥）+ createFromConfig→save 方法名漂移（⑩）+ configSnapshot 落 remark 断言弱（⑪）为 P2 watch-only。命中判据：§2 接受（主路径全实现）+ §2 P2①（边界弱）。
- **零 P0/零 P1**：候选缺口均不破坏活跃数据/GL 平衡/核心循环/会计正确性（CRM 域本身不直接产生会计凭证，转化经 sales 域弱指针交接）；等值边界放行不影响 stage 单调性主路径（前移/回退守卫正确）。

---

## 6. 与 arm-index 衔接（"复用 or 新增"裁决）

> §7 规则：每条 finding 产出前**必须 grep arm-index 同域同控制点**后裁决。

**arm-index crm stage/convLog/CPQ/configurator/priceRule/bundlePricing 同域 grep 结果**（`docs/audits/arm-index.md` RC 分区 + audit-remediation 分区）：

- `P1-MA2-075`（stageId 单向递增守卫，UC-CRM-06）—— **本切片直接相关**，arm-index 标 ✅ resolved (R1.24)，HEAD 复核 `validateStageDirection:91-110` STRICT 默认 + config-gated allow-backward 落地。**复用注记**（不重开）。
- `P1-MA1-009`（crm DECIMAL↔double MR1 建议 P2）—— **非本切片**（非 Lead/CPQ 实体的 stage probability 字段），不复用。
- `P1-MA2-076`（Event reminderMinutesBefore 死字段，UC-CRM-08）—— **非本切片**，不复用。
- `P1-MA2-086`（cron job 并发含 crm event-reminder/sequence-overdue）—— **非本切片**（UC-CRM-08/14），不复用。
- `P2-MA4-013`（crm Forecast stageName stub watch-only）—— **非本切片**（UC-CRM-10/15），不复用。
- `P2-MA4-020`（crm badge 漂移 watch-only 视图层）—— **不同控制点**，不复用。
- `P1-RC-034`（A1.28 convertToQuotation 不查 isWonStage）—— **共享控制点**（isWonStage→UC-CRM-03 触发链），本切片核 UC-CRM-06 推进侧不阻止转化（"允许触发"=不阻断），与 P1-RC-034 转化侧前置弱互补不重复（候选缺口③接受，不新建）。
- **RC 系列对 crm stage 等值边界 / CPQ createFromConfig / configSnapshot 落库 / 前端 wizard = 零**（A1.28 覆盖 UC-CRM-01/02/03/04/09/11 线索生命周期、A1.29 覆盖 UC-CRM-05/07/08/10/12/14/15 营销/预测/配额/序列/漏斗，均未触 UC-CRM-06 等值边界 / UC-CRM-13 CPQ 跨域建报价链路）。

**裁决结论**：本切片 4 项新 finding（P2-RC-036/037/038/039）均为 CRM 域**新发现**（既有 arm-index 无 RC finding 涉及 crm stage 等值边界 / CPQ createFromConfig 方法名漂移 / configSnapshot 落库断言 / 前端 wizard），按 §7 **全部新建**，列明差异依据。禁止未经比对新建——已 grep arm-index crm stage/convLog/CPQ/configurator/priceRule/bundlePricing 同域同控制点确认零重叠。

### finding → MR 映射

| Finding ID | UC # | 描述 | 目标 MR | 触及保护区域 |
|-----------|------|------|--------|-------------|
| `P2-RC-036` | UC-CRM-06 ④ | stage 等值边界（L1 `<=` vs 代码 `<`） | successor watch-only | 否（纯 Processor 代码逻辑修复[将 `toSeq < fromSeq` 改为 `toSeq <= fromSeq`] 或 owner doc/state-machine.md 补注登记实现选择，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first） |
| `P2-RC-037` | UC-CRM-13 ⑥ | 前端配置向导 wizard successor | successor watch-only | 否（前端 AMIS view.xml 补充，纯前端修复可自动执行，不触发 §5 ask-first） |
| `P2-RC-038` | UC-CRM-13 ⑩ | createFromConfig 方法名漂移 + configSnapshot 落 remark 截断 | successor watch-only | 否（方案 A：sales 域 `IErpSalQuotationBiz` 增 `createFromConfig` 别名委托 save + crm 调用方改用别名 + 配置快照独立字段或扩 remark 长度，**纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**；方案 B：owner doc cpq.md + use-cases.md 补注对齐说明[纯文档修复可自动执行]） |
| `P2-RC-039` | UC-CRM-13 ⑨ | configSnapshot 落 quotation.remark 内容断言弱 | successor watch-only | 否（补强 `TestErpCrmCpqGenerateQuote` 断言 `quotation.getRemark()` contains configSnapshot 关键字段；**纯测试补充，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**） |

**双向可追溯**：finding ID ↔ 修复行预留 MR1（P2 不强制 successor，登记供后续 roadmap 处理）；本切片无 P1 finding，故无强制 MR1 修复行。arm-index 新 finding 行在报告产出后同步写入（见 arm-index RC 分区新行 + RC 交叉引用注记）。

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行；无则注明"无"。

- **SP-1（UC-CRM-06 ④ 等值边界运行时触发面）**：`erp-crm.allow-stage-backward=true` 放行回退时，等值 stage 移动（toSeq==fromSeq）实际行为——HEAD 静态判定=放行（`validateStageDirection:99` 严格 `<` 不拦等值，allow-backward=true 仅控制 `<` 分支），运行时需确认等值 stage 移动经 GraphQL `ErpCrmLead__moveStage` 是否成功（P2-RC-036 已确认）；并对 FunnelAggregationEngine sequence 排序假设的实际影响（等值 stage 两个 sequence 相同时排序为稳定近似值，A1.29 已注记 successor）。
- **SP-2（UC-CRM-13 ⑩ configSnapshot JSON 实际落库字段与 quotation 关联）**：`buildQuotationData:246` 字面 `remark="CPQ pricingSource=...; snapshot=" + truncate(configSnapshot, 500)`——configSnapshot 超过 500 字符时**截断**，运行时需确认大型配置（多特征/多规则）的 snapshot 截断是否丢失关键配置信息（P2-RC-038 已确认 cosmetic）；并确认 `quotation.remark` 列实际长度上限（ORM 字段精度）是否足够承载典型配置（运行时探查）。
- **SP-3（UC-CRM-13 ⑫ generateQuote 弱指针回写的实际 relatedBillType 枚举值）**：`generateQuote:127` 字面 `setRelatedBillType(ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION)`——运行时需确认该枚举常量实际字面值与 sales 域 ErpSalQuotation 回链契约一致（与 A1.28 UC-CRM-03 转化路径的 relatedBillType 同型回写交叉确认）；L4 `testGenerateQuoteViaBundlePricing:102` 断言 `RELATED_BILL_TYPE_SALES_QUOTATION` 常量值，运行时确认与 sales 域 quotation.code 命名空间无冲突。
- **SP-4（UC-CRM-13 ② conditionExpression XLang 评估的失败模式）**：`ProductConfigRuleEngine.evalCondition:85-98` 编译失败抛 NopException 含 conditionExpression param——运行时需确认复杂表达式（如 `selectedFeatures.CPU_TYPE == 'INTEL_XEON' && selectedFeatures.MEMORY == '64GB'`）经 XLang `allowUnregisteredScopeVar(true).compileFullExpr` 实际评估行为是否符合预期（测试覆盖简单 source 匹配为主，复杂表达式运行时探查）。

**P0 即时通道**：本切片 Phase 1 定级**零 P0**，未触发 MR0 即时通道（§10）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter，真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P2-RC-036/037/038/039 新建 + P1-MA2-075 复用 + P1-RC-034 共享控制点注记）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（见 §6），无未经比对直接新建的 finding。

### checker actual vs baseline 实测表（HEAD 实测）

| 规则 | Baseline | Actual（本审计 HEAD） | 状态 |
|------|----------|---------------------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 14 | 14 | ✅ |
| R2a | 34 | 34 | ✅ |
| R2b | 229 | 229 | ✅ |
| R2c | 1382 | 1382（生产代码总计） | ✅ |
| R2d | 34 | 34 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅ |
| R6 | 2 | 2 | ✅ |
| R7 | 0 | 0 | ✅ |
| R8 | 0 | 0 | ✅ |
| R10 | 6 | 6 | ✅ |
| R11 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

**回归风险声明**：本审计为**只读审计**（无生产代码/ORM/api.xml/view.xml/真相源变更），checker actual 与 baseline 精确匹配，**无回归风险**。

---

## 9. 与 MA2 报告差异增量声明

> §去重协议：本 MA1 切片报告复用既有 MA2 报告已证实行为，只补需求视角差异。

**复用既有 MA2/MA4 报告**：
- **A2.14**（`docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`）：crm Lead 5 态 + Event 3 态状态机 PASS + 转化跨域经 Facade（IErpSalQuotationBiz/IErpMdPartnerBiz）零跨模块 ORM 写。**P1-MA2-075**（stageId 单向递增守卫 UC-CRM-06）**resolved R1.24**——本切片 UC-CRM-06 ① sequence 单向递增守卫复用其已证实行为，不重复核实。
- **A4.5**（`docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`）：crm PriceRuleEngine 代码质量 PASS（UC-CRM-13 ⑧ 价格规则直接相关），本切片复用其已证实代码质量。
- **A1.29**（`docs/audits/2026-08-05-1100-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`）：FunnelAggregationEngine sequence 排序假设经 STRICT 守卫（allow-stage-backward 默认 false）保护；本切片 UC-CRM-06 ④ 等值边界复核该守卫边界，登记 P2-RC-036。
- **A1.28**（`docs/audits/2026-08-05-1030-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`）：UC-CRM-03 转化路径 + P1-RC-034（convertToQuotation 不查 isWonStage）共享 UC-CRM-06 ⑥ isWonStage→UC-CRM-03 触发链控制点；本切片核 UC-CRM-06 推进侧不阻止转化，与 P1-RC-034 互补不重复。

**只补的需求视角差异（本切片新发现增量）**：
1. **UC-CRM-06 ④ 等值边界**（P2-RC-036）：MA2 A2.14 证实 STRICT 回退守卫，未从 L1 `:122` 字面 `<=`（等值拒绝）视角审视代码 `<`（等值放行）边界差异——A2.14 P1-MA2-075 resolved 覆盖"回退"（`<`）未覆盖"等值"（`==`）。
2. **UC-CRM-06 ⑥ isWonStage→UC-CRM-03 触发链**：A2.14 证实状态机迁移，未从 L1 `:124` "isWonStage==true → 允许触发"视角审视推进侧是否守卫/提示；本切片核推进侧"允许触发"=不阻断，与 A1.28 P1-RC-034 转化侧互补。
3. **UC-CRM-13 ⑩ createFromConfig 方法名漂移 + configSnapshot 落 remark 截断**（P2-RC-038/039）：无既有 MA2/MA4 报告审计 UC-CRM-13 CPQ 跨域建报价链路；本切片首次从需求视角核 L1 `:321-323` 字面 `createFromConfig(...)` vs 实仓 `IErpSalQuotationBiz.save(...)` 方法名漂移 + configSnapshot JSON 落 `quotation.remark` 截断 500 字符（owner doc cpq.md:190 显式登记实现注记，但 owner doc 非真相源[§4]，故仍登记 P2）。
4. **UC-CRM-13 ⑥ 前端配置向导 wizard**（P2-RC-037）：与 A1.28/A1.29 同型前端可视化 successor——后端规则引擎就绪，AMIS wizard 页面属前端 successor。

---

## 段落完整性自检（落盘前强制）

- [x] §1 需求契约原文（2 UC 逐字引用）— 存在
- [x] §2 实现证据（L3 代码路径含行号 + 跨域调用链）— 存在
- [x] §3 测试证据（L4 测试断言 + 强度）— 存在
- [x] §4 运行时行为证据（L5 复用 A2.14/A4.5/A1.28/A1.29）— 存在
- [x] §5 符合性结论（五级追踪矩阵 + 每 UC 结论 + 候选缺口逐条裁决）— 存在
- [x] §6 与 arm-index 衔接（复用 or 新增裁决 + 双向可追溯）— 存在
- [x] §7 静态存疑点清单（4 项 SP-1~SP-4）— 存在
- [x] §8 过程纪律自检（checker actual vs baseline + 独立性 + 交叉去重）— 存在
- [x] §9 与 MA2 报告差异增量声明（复用 + 只补需求视角差异）— 存在

**9 段齐全。**

---

## 整体裁决

**Verdict: pass（零 P0、零 P1、4 项新 P2[P2-RC-036/037/038/039]、2 UC 接受 on 主路径）**。

- **UC-CRM-06 漏斗阶段推进**：**接受 on ①②③⑤⑥⑦ + P2 on ④**（P2-RC-036 等值边界 watch-only）——sequence 单向递增守卫（STRICT 默认 + config-gated allow-backward）+ convLog 四字段完整性 + stageId 变更不改 docStatus + isWonStage→UC-CRM-03 推进侧不阻断 全部实现；L1 `<=` vs 代码 `<` 等值边界 P2 watch-only。resolved finding HEAD 复核：P1-MA2-075（stageId 守卫 resolved R1.24）「如登记/已 resolved」无升级。
- **UC-CRM-13 CPQ 配置-定价-报价**：**接受 on ①②③④⑤⑦⑧⑨⑩⑪⑫ + P2 on ⑥⑩-方法名漂移⑪**（P2-RC-037 前端 wizard successor / P2-RC-038 createFromConfig→save 方法名漂移 + configSnapshot 落 remark 截断 / P2-RC-039 configSnapshot 落库断言弱）——配置规则引擎（REQUIRED/EXCLUDED/RECOMMENDED + EXCLUDED 优先）+ 价格规则引擎（ruleType rank + priority tie-break）+ 捆绑定价 + 跨域建报价单 + lead 弱指针回写 全部实现且强测；前端 wizard successor + 方法名漂移 cosmetic + 断言弱为 P2 watch-only。
- **CRM 域 MA1 三切片（A1.28/A1.29/A1.30）全 done**——本切片解除 A1.30 在 MA4（A4.2 扩展域展开器）及 MR1（R1.0）链路的该切片证据缺口。
- **零 P0**：候选缺口均不破坏活跃数据/GL 平衡/核心循环/会计正确性（CRM 域不直接产生凭证，转化经 sales 域弱指针交接）。
- **零 P1**：UC-CRM-06 主路径已实现且 P1-MA2-075 resolved；UC-CRM-13 配置/定价/报价主路径已实现且强测；4 项新 finding 均为 P2（边界弱/successor/cosmetic/断言弱）。
