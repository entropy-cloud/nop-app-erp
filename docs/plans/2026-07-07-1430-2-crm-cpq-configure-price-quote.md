# 2026-07-07-1430-2-crm-cpq-configure-price-quote CRM CPQ 配置-定价-报价（UC-CRM-07）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` Non-Goal scope boundary（UC-CRM-07 CPQ 配置定价报价，归后继工作项）+ `docs/design/crm/cpq.md`
> Related: `2026-07-04-0549-2-crm-lead-opportunity-quotation-conversion.md`（Lead→Quotation 转化已完成，`IErpSalQuotationBiz` 建单范式已验证）；`2026-07-04-0700-1-crm-event-reminder-lead-scoring-forecast.md`（CRM 域 BizModel 范式）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **六实体已物化且 BizModel 为空壳**（design `cpq.md` 与 ORM 精确匹配）：
  - `ErpCrmProductConfigurator`（`module-crm/model/app-erp-crm.orm.xml`）—— 产品配置器头，含 `productType`、`configName`、`isActive`、`effectiveFrom`/`effectiveTo`、`wizardLayout`(JSON 向导步骤布局)。BizModel（`module-crm/erp-crm-service/.../entity/`，15 行）仅 `extends CrudBizModel`。
  - `ErpCrmConfigRule` —— 配置规则行，`configuratorId`(→ProductConfigurator)、`ruleType`(REQUIRED/OPTIONAL/EXCLUDED/RECOMMENDED)、`sourceFeatureCode`/`sourceFeatureValue`、`targetFeatureCode`/`targetFeatureValue`、`conditionExpression`、`sequence`。BizModel 为空壳。
  - `ErpCrmBundlePricing`（+`ErpCrmBundlePricingLine`）—— 捆绑定价，`bundleName`、`discountType`(PERCENTAGE/FIXED)、`discountValue`、`bundleAmount`(手工定价覆盖)、`effectiveFrom`/`effectiveTo`、`isActive`；行 `productId`/`quantity`/`unitPrice`/`sequence`。BizModel 为空壳。
  - `ErpCrmPriceRule` —— 价格规则，`ruleType`(VOLUME/PROMOTIONAL/CUSTOMER_SPECIFIC)、`priority`、`productId`/`productCategory`/`customerId`/`customerCategory`(均可空)、`minQuantity`/`maxQuantity`、`priceOverride`/`discountPercent`/`discountAmount`、`currencyId`、`effectiveFrom`/`effectiveTo`、`isActive`。BizModel 为空壳。
  - `ErpCrmQuoteTemplate` —— 报价模板。BizModel 为空壳。
- **报价生成跨域通道已就绪**：`IErpSalQuotationBiz` 经 0549-2 验证（Lead→Quotation 建单），CPQ 配置结果可经此创建正式报价单；`ErpCrmLead` `relatedBillType`/`relatedBillCode` 弱指针回写范式已存在。
- **平台范式已就绪**：CRM 域三件套（`ErpCrmConstants`/`ErpCrmConfigs`/`ErpCrmErrors`）经 0700-1/0549-2 验证；master-data `ErpMdProduct`（产品主数据）只读引用。
- **菜单已生成**：`erp-crm.action-auth.xml` 已含 `crm-cpq` 菜单组（CRUD 页面已生成，本期补业务行为）。
- **关键缺口 1 — 配置规则引擎缺失**：`ErpCrmConfigRule` 表已物化但无规则评估逻辑（design `cpq.md` §1 伪代码：按 `sourceFeatureCode/Value` 匹配选中特征 → REQUIRED/EXCLUDED/RECOMMENDED 标记目标特征）。
- **关键缺口 2 — 价格规则引擎缺失**：`ErpCrmPriceRule` 表已物化但无定价计算逻辑（design §4：客户特定 > 促销 > 数量阶梯 > 标准定价优先级，同规则多条按 `priority` 小者优先）。
- **关键缺口 3 — 捆绑定价计算缺失**：单品合计 → 折扣（PERCENTAGE/FIXED）或手工 `bundleAmount` 覆盖。
- **关键缺口 4 — 配置→报价生成缺失**：配置快照 + 定价结果 → `IErpSalQuotationBiz` 建单 + lead 回写。
- **剩余差距**：四引擎（配置规则/价格规则/捆绑定价/报价生成）均为空壳待实现。

## Goals

- **配置规则引擎**：`ProductConfigRuleEngine`（纯函数式 + 注入加载函数便于单测）—— 输入选中特征 `selectedFeatures`，遍历 `ErpCrmConfigRule`（`configuratorId` + `sequence` 排序），按 `sourceFeatureCode/Value` 匹配，标记目标特征 REQUIRED/EXCLUDED/RECOMMENDED；`conditionExpression`（复杂条件）优先级高于单行条件。
- **价格规则引擎**：`PriceRuleEngine`（纯函数式）—— 按 ruleType 优先级（CUSTOMER_SPECIFIC > PROMOTIONAL > VOLUME）+ `priority` 数值 + 期间有效 + 数量区间匹配，返回最优价格（`priceOverride`/`discountPercent`/`discountAmount`），无匹配回退标准定价（产品主数据价格）。
- **捆绑定价计算**：`BundlePricingCalculator`（纯函数式）—— 单品 `unitPrice×quantity` 合计 → 按 `discountType`(PERCENTAGE/FIXED) 折扣，或 `bundleAmount` 不为空时手工定价覆盖。
- **配置→报价生成**：`IErpCrmProductConfiguratorBiz.generateQuote(configuratorId, selectedFeatures, bundlePricingId?, priceRuleContext?, leadId?, ctx)` @BizMutation —— 配置快照(JSON) + 定价计算 → 调 `IErpSalQuotationBiz` 创建正式报价单 + 回写 `lead.relatedBillType/Code`（0549-2 范式）。
- **字典/矩阵维护钩子**：`IErpCrmConfigRuleBiz`/`IErpCrmPriceRuleBiz`/`IErpCrmBundlePricingBiz` `defaultPrepareSave`/`Update` 钩子（生效日期合理性、`discountType`-`discountValue` 一致性、`maxQuantity>=minQuantity` 校验）。
- **owner doc 收口 + 测试**：行为测试覆盖配置规则各 ruleType、价格规则优先级、捆绑定价、报价生成跨域。

## Non-Goals

- **引导式销售向导前端（wizard 多步骤交互式 UI）**：本期 `wizardLayout` JSON 配置 + 配置规则引擎就绪；交互式多步骤向导 UI 归前端 successor（design `cpq.md` §2 反模式：步骤不应硬编码在前端，本期配置化已解除）。
- **价格审批工作流**：价格规则/客户特定定价直接生效，不做审批流（design 边界：本模块不负责价格审批工作流）。
- **报价单审批流 / 合同签署**：归 sales 域（design 边界）；CPQ 仅生成报价草稿。
- **配置规则可视化编辑器 / wizard 拖拽设计器**：本期 JSON 配置 + 表单维护；可视化编辑器归前端 successor。
- **CPQ 报表 AMIS 前端**：归报表 successor（nop-report 已接线 0504-2）。
- **序列管理 / 漏斗分析（UC-CRM-08/09）**：归独立 successor 计划（`2026-07-07-1430-3`）。
- **与产品主数据 attribute 联动自动生成配置器**：本期配置器手工维护；自动从 `ErpMdProduct` 属性生成归 successor。

## Task Route

- Type: `implementation-only change`（六 BizModel 扩展 + 三纯函数式引擎 + 跨域报价生成，ORM 无变更）。
- Owner Docs: `docs/design/crm/cpq.md`（实体/规则/配置已完整）、`docs/design/crm/use-cases.md`（UC-CRM-13）、`docs/design/crm/README.md`（§衔接契约）。
- Skill Selection Basis: 后端 BizModel/IBiz/ErrorCode/CrudBizModel 钩子 + 单步操作（配置/定价/报价生成各自单步，非多步编排，无需 Processor）+ 三纯函数式引擎便于单测 + 跨域经 `IErpSalQuotationBiz` I*Biz（非 IDaoProvider）→ 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase` → 加载 `nop-testing`。两技能必需输入（cpq.md 既有、六实体 ORM 既有、IErpSalQuotationBiz 既有）均就绪。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移；无 ORM 变更；无 codegen 增量。
- 新增配置键遵循 CRM 域范式（`ErpCrmConstants` 字符串键 + `ErpCrmConfigs` 默认值/reader，对齐 0700-1）：`erp-crm.cpq.max-rules-per-configurator`(100)、`erp-crm.cpq.enable-wizard`(true)、`erp-crm.cpq.default-currency`(CNY)。
- 无新业务类型（无业财过账）。
- 回滚策略：全部改动为应用层 Java + 配置键，git 可逆。

## Execution Plan

### Phase 1 - 配置规则引擎 + 字典/规则维护钩子

Status: completed
Targets: `IErpCrmProductConfiguratorBiz`、`IErpCrmConfigRuleBiz`、`ProductConfigRuleEngine`、`ErpCrmConstants`、`ErpCrmErrors`、`ErpCrmConfigs`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: 无

- [x] `Add`：`ProductConfigRuleEngine`（`module-crm/erp-crm-service/.../cpq/`）—— 纯函数式 + 注入加载函数便于单测：`evaluate(selectedFeatures, rules)` 按 `configuratorId`+`sequence` 遍历，`sourceFeatureCode` 在选中特征且值匹配时按 `ruleType` 标记目标特征（REQUIRED 必选/EXCLUDED 禁用/RECOMMENDED 推荐）；`conditionExpression` 不为空时优先评估（经 XLang 表达式，对齐评分引擎 0700-1 公式范式）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpCrmConfigRuleBiz`/`IErpCrmBundlePricingBiz`/`IErpCrmPriceRuleBiz` 扩展 `defaultPrepareSave`/`Update` 钩子：规则数超 `max-rules-per-configurator` 拒绝；`discountType`-`discountValue` 一致性；`maxQuantity>=minQuantity`；生效日期 `effectiveFrom<=effectiveTo`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCrmErrors` 扩展 ErrorCode：`ERR_CPQ_RULE_LIMIT_EXCEEDED`、`ERR_CPQ_CONFIGURATOR_INACTIVE`、`ERR_CPQ_DISCOUNT_INCONSISTENT`、`ERR_CPQ_QTY_RANGE_INVALID`、`ERR_CPQ_EFFECTIVE_DATE_INVALID`、`ERR_CPQ_NO_PRICE_MATCHED`（中文描述 + ARG_* 参数）。`ErpCrmConstants` 配置键 + `ErpCrmConfigs` reader。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 配置规则引擎各 ruleType（REQUIRED/EXCLUDED/RECOMMENDED）+ conditionExpression 优先可观察；维护钩子各校验可触发 ErrorCode。
- [x] `mvn compile -pl module-crm/erp-crm-service -am` 通过；行为测试在 Phase 3 统一编写。

### Phase 2 - 价格规则引擎 + 捆绑定价 + 配置→报价生成

Status: completed
Targets: `PriceRuleEngine`、`BundlePricingCalculator`、`IErpCrmProductConfiguratorBiz.generateQuote`、`IErpCrmPriceRuleBiz`、`IErpCrmBundlePricingBiz`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（配置规则引擎就绪后配置快照可生成）

- [x] `Add`：`PriceRuleEngine`（纯函数式 + 注入加载函数便于单测）：`resolvePrice(productId, customerId, quantity, currencyId, now, activeRules)` 按 ruleType 优先级（CUSTOMER_SPECIFIC > PROMOTIONAL > VOLUME）+ `priority` 数值小者优先 + 期间有效 + 数量区间匹配，返回最优价格结果（`priceOverride` 或基础价 ± `discountPercent`/`discountAmount`）；无匹配回退标准定价。
  - Skill: `nop-backend-dev`
- [x] `Add`：`BundlePricingCalculator`（纯函数式）：`calculate(bundle, lines)` 单品 `unitPrice×quantity` 合计 → `discountType`=PERCENTAGE 百分比折扣 / FIXED 固定金额折扣；`bundleAmount` 不为空时手工定价覆盖折扣计算。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpCrmProductConfiguratorBiz.generateQuote(configuratorId, selectedFeatures, bundlePricingId?, priceRuleContext?, leadId?, ctx)` @BizMutation —— 生成配置快照(JSON) → `PriceRuleEngine`/`BundlePricingCalculator` 定价 → 调 `IErpSalQuotationBiz` 创建正式报价单（0549-2 范式）→ 回写 `lead.relatedBillType/Code`；`configuratorId` 失效抛 `ERR_CPQ_CONFIGURATOR_INACTIVE`，无价格匹配抛 `ERR_CPQ_NO_PRICE_MATCHED`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 价格规则优先级（三 ruleType + priority + 期间 + 数量区间）+ 无匹配回退、捆绑定价（PERCENTAGE/FIXED/bundleAmount 覆盖）、配置→报价生成跨域（IErpSalQuotationBiz 建单 + lead 回写）均可观察。
- [x] `mvn compile -pl module-crm/erp-crm-service -am` 通过；行为测试在 Phase 3 统一编写。

### Phase 3 - 行为测试 + 日志 + 文档对齐

Status: completed
Targets: `module-crm/erp-crm-service/src/test/.../TestErpCrmCpq*.java`、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1、Phase 2

- [x] `Add`：`TestProductConfigRuleEngine`（纯单元测试）：四 ruleType 标记、conditionExpression 优先、规则数上限。
  - Skill: `nop-testing`
- [x] `Add`：`TestPriceRuleEngine`（纯单元测试）：三 ruleType 优先级、priority 平局、期间失效、数量区间边界、无匹配回退标准定价。
  - Skill: `nop-testing`
- [x] `Add`：`TestBundlePricingCalculator`（纯单元测试）：PERCENTAGE/FIXED 折扣、bundleAmount 覆盖、空行处理。
  - Skill: `nop-testing`
- [x] `Add`：`TestErpCrmCpqGenerateQuote`（集成测试）：配置→定价→报价生成跨域（IErpSalQuotationBiz 建单 + lead 回写）、配置器失效拒绝、维护钩子各 ErrorCode。
  - Skill: `nop-testing`
- [x] `Proof`：`mvn test -pl module-crm/erp-crm-service -am`（含本期新增 + 0700-1/0549-2 既有）→ 0 failures / 0 errors。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` Non-Goal boundary 标注 UC-CRM-07 已承接；`cpq.md` 实现注记（conditionExpression XLang 评估 Decision 等）。
  - Skill: none

Exit Criteria:

- [x] 新增行为测试全绿（单元 + 集成）；crm-service 既有测试无回归。
- [x] 当日日志条目在位；roadmap Non-Goal boundary 标注更新。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_0c50ef6c6ffelk3C2nB4v33mva) — 全部 baseline 声明经实时仓库核实（6 实体 + 字段 + 6 空壳 BizModel + IErpSalQuotationBiz + 三件套 + crm-cpq 菜单 + cpq.md 设计引用均准确），规则 2/4/7/8/9/10/13/14 全部合规，跨域经 I*Biz（非 IDaoProvider）符合 AGENTS.md。
- 非阻塞观察已采纳（实现时落实，不改草案结构）：(1) `ErpCrmConfigs.java` 现为 5 行空接口，baseline「三件套经验证」措辞实指 ErpCrmConstants/ErpCrmErrors，Configs 待填充；(2) cpq.md:183 伪代码 `createFromConfig` 经 IErpSalQuotationBiz 不存在，本期正确改用 `IErpSalQuotationBiz.save`（0549-2 范式），Phase 3 实现注记显式记录此 drift；(3) 配置快照(JSON) 持久化位置在 Phase 2 实现时裁定（lead.remark 或临时结构）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（配置规则引擎 + 价格规则引擎 + 捆绑定价 + 配置→报价生成 + 维护钩子）
- [x] 相关文档对齐（cpq.md 实现注记、roadmap Non-Goal boundary、当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests`（全模块）+ `mvn test -pl module-crm/erp-crm-service -am`（0 failures / 0 errors）
- [x] 无范围内项目降级为 deferred/follow-up（向导前端/价格审批/合同签署/可视化编辑器/报表前端/产品属性自动生成均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 引导式销售向导前端（wizard 多步骤交互式 UI）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期 `wizardLayout` JSON 配置化 + 配置规则引擎就绪；交互式向导 UI 归前端。
- Successor Required: yes（触发条件：CPQ 前端套件建立时）

### 价格审批工作流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: design 边界明确本模块不负责价格审批工作流；价格规则直接生效。
- Successor Required: yes（触发条件：价格管控审批业务上线时）

### 配置规则/wizard 可视化编辑器

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期 JSON 配置 + 表单维护；可视化编辑器归前端 successor。
- Successor Required: yes（触发条件：CPQ 管理前端套件建立时）

### CPQ 报表 AMIS 前端

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归报表 successor（nop-report 已接线 0504-2）；本期后端引擎已就绪。
- Successor Required: yes（触发条件：CRM 报表接入时）

## Closure

Status Note: 全部 3 Phase 已完成（2026-07-07）。新增 6 实体 BizModel 扩展（ProductConfigurator/ConfigRule/BundlePricing/PriceRule 维护钩子 + Configurator generateQuote）+ 3 纯函数式引擎（ProductConfigRuleEngine/PriceRuleEngine/BundlePricingCalculator）+ 6 ErrorCode + 3 配置键 + 4 测试类 34 cases。`mvn clean install -DskipTests` 全模块通过；`mvn test -pl module-crm/erp-crm-service -am` 92 tests / 0 failures / 0 errors（新增 34 + 既有 58 无回归）。design cpq.md 已加实现注记（createFromConfig→save drift、conditionExpression XLang allowUnregisteredScopeVar、currencyId 由 priceRuleContext 提供）。roadmap Non-Goal boundary 已标注 UC-CRM-07 ✅ done。结束审计 gate 由独立子代理执行（见上）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 closure audit（新会话，无执行者上下文），OVERALL: **close**。六项验证全通过：(1) 计划一致性——执行项/退出标准全 `[x]`，3 Phase `Status: completed`，Closure Gates 8/8 `[x]`，唯一遗留 `[ ]`（独立审计门控）由本审计落地；(2) 交付物全在 live repo——3 纯函数式引擎（`ProductConfigRuleEngine` 185 行 / `PriceRuleEngine` 199 行 / `BundlePricingCalculator` 113 行，均非空壳）+ `ErpCrmProductConfiguratorBizModel.generateQuote`（@BizMutation，经 `IErpSalQuotationBiz.save` 跨域建单 + lead 回写）+ 5 BizModel 维护钩子（ConfigRule/BundlePricing/BundlePricingLine/PriceRule/ProductConfigurator）+ 6 ErrorCode（`ErpCrmErrors:151-176`）+ 3 配置键（`ErpCrmConstants:122-126`）+ 4 测试类共 34 cases（TestProductConfigRuleEngine 9 / TestPriceRuleEngine 9 / TestBundlePricingCalculator 8 / TestErpCrmCpqGenerateQuote 8）；(3) 文档对齐——`docs/design/crm/cpq.md:189-191` 实现注记（`createFromConfig`→`save` drift + `conditionExpression` XLang `allowUnregisteredScopeVar` 评估）、`docs/backlog/extended-roadmap.md:52` UC-CRM-07 ✅ done 标注、`docs/logs/2026/07-07.md:564` 当日日志条目含验证状态；(4) 反模式检查——`ProductConfigRuleEngine:138 return null` 为 `norm()` 字符串归一化 helper（合法），非空体/吞异常占位；跨域经 I*Biz（非 IDaoProvider）符合 AGENTS.md；(5) 验证声明——计划声称 `mvn clean install -DskipTests` + `mvn test -pl module-crm/erp-crm-service -am` 92 tests/0 failures/0 errors，与日志记录一致；(6) Deferred 诚实——4 项（wizard 前端/价格审批/可视化编辑器/报表前端）均为 Non-Goal 显式触发条件，无范围内缺陷降级。

Follow-up:

- 引导式销售向导前端（见上方 Deferred）
- 价格审批工作流（见上方 Deferred）
- 配置规则/wizard 可视化编辑器（见上方 Deferred）
- CPQ 报表前端（见上方 Deferred）
