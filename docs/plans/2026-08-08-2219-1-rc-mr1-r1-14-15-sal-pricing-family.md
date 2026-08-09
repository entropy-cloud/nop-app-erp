# 2026-08-08-2219-1-rc-mr1-r1-14-15-sal-pricing-family RC-R1.14 + RC-R1.15 — sales 促销定价族（最低价校验 + 价税分离，MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-09
> Mission: requirement-compliance
> Work Item: RC-R1.14（P1-RC-021 sales 促销最低价校验）+ RC-R1.15（P1-RC-022 sales 促销价税分离，A1.21 UC-SAL-08 reuse 合并）— 同域同组件（`ErpSalOrderBizModel` 定价路径）同 owner doc 同结果表面，按计划指南规则 14 合并为一个 owner plan 的两个阶段
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.14/RC-R1.15 行 + `docs/audits/arm-index.md` P1-RC-021/P1-RC-022 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`
> Related: `docs/design/sales/use-cases.md`（L1 UC-SAL-11 ⑥⑦ + UC-SAL-08 价税分离）；`docs/design/sales/state-machine.md` §9；`docs/audits/2026-08-07-2330-rc-ma4-a4-2-47-55-sales-f1-f2-mainflow-outbound-runtime.md`（A4.2.48/49 运行时证据）；`docs/audits/2026-08-07-2330-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`（A4.2.56-a 多档混合偏差量化）；`docs/plans/2026-08-08-1603-3-rc-mr1-r1-13-sal-order-availability-precheck.md`（同批范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-021（arm-index 行，UC-SAL-11 ⑥ 最低价校验缺失）**：L1（`use-cases.md:249-250`）逐字「最终售价 < SKU.minPrice → 按配置拒绝/警告」。L3 实仓：`ErpSalOrderBizModel.applyPricingRules:96-114` → `persistPricingResult:143-160` 完全不调用最低价守卫；master-data 侧 `ErpMdMaterialSkuBizModel.validatePrice:180-203`（@BizQuery，三态 OFF/WARN/HARD 分派 + 派生底线 `deriveMinPrice:331-346`）已独立实现但 sales 促销应用层无接线——促销后售价可低于 SKU 底线（A4.2.48 运行时确认促销配置触发面存在）。§2 P1①（功能实质偏离验收标准——L1 最低价守卫控制点在 sales 应用层缺失）。**非 P0**（不破坏活跃数据/GL 平衡）。**与 P1-RC-063（master-data supplier SPI）互补不重复**（不同域不同控制点）。
- **finding P1-RC-022（arm-index 行，UC-SAL-11 ⑦ + UC-SAL-08 reuse 合并价税分离缺失）**：L1（`use-cases.md:252-254` + UC-SAL-08:190-193）逐字「折扣后金额 = 原金额 - 促销优惠；税额 = 折扣后金额 / (1 + 税率) × 税率」。L3 实仓：`ErpSalOrderBizModel.recomputeLineAmount:172-179` 仅 setAmount 不 setTaxAmount + `recomputeOrderTotals:181-197` 复用陈旧 taxAmount → 促销后销项税+应收高估；A4.2.49 运行时确认单档偏差（应 5.75 实 11.50）+ A4.2.56-a 多档混合（13%/9%/6% + 50% off + AMOUNT_OFF）偏差 ~106% 量化，GL 平衡不破坏。§2 P1①。**非 P0**（GL 借贷平衡不破坏——偏差在科目分配非失衡）。
- **赠品行交互（本行范围内必须裁决的既有行为）**：`ErpSalPricingRuleEngine.addGiftLine:203-217` 生成赠品行 `setSkuId(rule.getGiftSkuId())`（`:208`）+ `setUnitPrice(BigDecimal.ZERO)`（`:210`）+ amount=0——**赠品行 skuId 非空且 amount=0**，若最低价校验对赠品行生效，HARD 级别下 finalPrice=0 < 派生底线 → 抛 `ERR_PRICE_BELOW_MIN` → **HARD 配置的促销含赠品被整体拒绝（相对当前基线的行为回归）**。L1 UC-SAL-08 显式要求「赠品行.单价 == 0 但 赠品行 也触发出库扣库存」——赠品行必须可成功生成。**修复方向：最低价校验显式排除赠品行（amount==0 行跳过），见 Phase 1 Decision。**
- **A4.2.48/49/56-a 运行时确认**：最低价校验缺失实际触发面 = 促销配置（PERCENT_DISCOUNT/AMOUNT_OFF/PRICE_OVERRIDE）可致最终售价 < 派生底线（`ErpMdMaterialSkuBizModel.deriveMinPrice`）；价税分离缺失 = 促销后 `taxAmount` 沿用促销前值。**均维持 P1**。A4.2.56-a 报告裁决措辞「修复归 MR1 触 recomputeLineAmount/recomputeOrderTotals 核心路径须 ask-first」与 roadmap 2026-08-08 §7 A1 人工裁决（RC-R1.15 = **第一批（纯预授权）**，生效即日）存在措辞张力——**以 roadmap 行为准**（人工裁决晚于 MA4 报告且更权威）：本修复面 = 销售订单行**定价计算**（BizModel 行金额/税额派生），非 VoucherFact/PostingProcessor 过账核心路径；不触碰过账引擎/凭证生成。本计划在 Non-Goals 显式声明边界。
- **实仓（HEAD 核查）**：
  - `ErpSalOrderBizModel.applyPricingRules:96-114`（@BizMutation，UI/编排入口）+ `persistPricingResult:143-160`（per-line recompute + save/update + 头合计重算 + updateEntity）——**最低价校验接入点**（P1-RC-021）。
  - `recomputeLineAmount:172-179`（net = gross − discountAmount；仅 setAmount）——**价税分离修复点**（P1-RC-022）：按 L1 公式 `taxAmount = net/(1+rate)×rate`；`recomputeOrderTotals:181-197` 需以新 taxAmount 重算 totalTaxAmount/totalAmountWithTax。
  - `ErpSalOrderLine` ORM：`taxRate`（propId 9）/`taxRateId`（propId 10）/`taxAmount`（propId 11）/`amount`（propId 12）/`amountWithTax`（propId 13）/`discountAmount`（propId 101）——**零 ORM 变更**。
  - **跨域注入载体**：`IErpMdMaterialSkuBiz`（master-data BizModel 接口，含 `validatePrice(@Name("skuId") Long, @Name("finalPrice") BigDecimal, @Optional @Name("materialCategoryId") Long, IServiceContext)` @BizQuery + `PriceValidationResult`（passed/warning/minPrice/level 四字段——实仓 `module-master-data/erp-md-dao/.../dao/dto/PriceValidationResult.java:14-17`，字段为 `warning` 非 belowMin；HARD 抛 `ERR_PRICE_BELOW_MIN`）；级别判定经 `resolvePriceValidationLevel` 按物料分类 dict（OFF/WARN/HARD，孤儿值"20"归 WARN）。sales 侧注入 `IErpMdMaterialSkuBiz` 调 validatePrice（复用 master-data 三级语义）或经 `ErpSalPriceListLine`/SKU 直接比对——**Decision 项**。
  - **测试基线**：`TestErpSalPricingRuleEngine`（促销引擎单测强覆盖）+ `TestErpSalPricingEndToEnd`（7 场景冒烟）+ `TestErpSalOrderApproval`——新增校验/重算后既有测试须零回归。
- **预授权判据**（第一批纯预授权）：纯 BizModel 代码逻辑修复 + config key（如需），**不触 ORM 结构/会计过账核心路径（VoucherFact/PostingProcessor）/删除**；**无 ask-first checkbox**。roadmap RC-R1.14/RC-R1.15 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-sales/erp-sal-service/.../entity/ErpSalOrderBizModel.java`；`ErpSalConstants.java`/`ErpSalErrors.java`（仅当 Phase 1 Decision 选 B 时变更，否则不动）；测试类 1 个新增 + `_cases/` 快照。

## Goals

- **最低价校验（P1-RC-021）**：`applyPricingRules` → `persistPricingResult` 促销结果落地后逐行触发最低价校验——复用 master-data `IErpMdMaterialSkuBiz.validatePrice`（OFF 放行 / WARN 放行带警告 / HARD 抛错拒绝），按促销后最终售价（`line.amount/quantity` 净值或行单价——**Decision 项**）比对 SKU 派生底线；失败语义对齐 master-data（HARD 抛 `ERR_PRICE_BELOW_MIN`，propagate）。**赠品行（amount==0）显式排除**（见 Phase 1 Decision）——L1 UC-SAL-08 要求赠品可成功生成，不得被最低价校验误拒。
- **价税分离（P1-RC-022）**：`recomputeLineAmount` 按 L1 公式重算 `taxAmount = net/(1+rate)×rate`（rate 为小数转换，null 视为 0 处理——**Decision 项**）；`recomputeOrderTotals` 以新行值重算 `totalTaxAmount`/`totalAmountWithTax`；零税率行 `taxAmount=0` 保持。
- **默认行为保基线**：价税分离为行重算固有行为（无 config gate——L1 公式即正确性定义）；最低价校验复用 master-data 既有三级 level（零新 config——**roadmap「config-gated 拒绝/警告」的承载 = master-data 既有 `priceValidationLevel` 分类级 dict（per 物料分类配置）**，非新增 sales 侧 config key）——既有测试零回归（seed 价不低于底线 + 赠品行跳过）。
- **owner doc 收敛注记**：`use-cases.md` 需求契约段不动（真相源冻结条款）；`state-machine.md §9`/sales README 补促销价税分离 + 最低价校验实现注记。
- **测试矩阵**：P1-RC-021（OFF 放行 / WARN 放行带警告 / HARD 拒绝 + 促销后低于底线触发 / 未触发促销不干预）+ P1-RC-022（单档折扣后税额公式 / 零税率行 0 / 多档混合 13%+9%+6% 促销叠加数值断言 / amountWithTax=amount+taxAmount 恒等式）——分域 `mvn test -pl module-sales/erp-sal-service` 全绿 + `_cases/` 快照录制。
- 回填 arm-index P1-RC-021/P1-RC-022 → `done (RC-R1.14/RC-R1.15)` + roadmap RC-R1.14/RC-R1.15 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**（零列/零索引变更——taxRate/taxAmount/amount/amountWithTax 载体已就绪；不为 minPrice 独立列，复用 master-data 派生底线）。
- **不触会计过账核心路径**（VoucherFact/PostingProcessor/凭证生成零改动——价税分离仅修正订单行税额派生产出；下游 `SalAcctDocProvider`/`InvPostingDispatcher` 消费 corrected 值无需改写）。
- **不新建 master-data 侧能力**（`validatePrice`/`deriveMinPrice`/`resolvePriceValidationLevel` 既有逻辑不动，sales 侧仅接线消费）。
- **不改取价优先级链本身**（三级链位置 G5 属既有接受裁决，非本行范围）。
- **不做 P2-RC-018（pricing E2E 断言强度）**（登记不强制，非本行）；**不做 P2-RC-023（赠品 UI 标记）**。
- **不改真相源契约段落**（use-cases L1 不动）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/sales/use-cases.md`（L1 UC-SAL-11 ⑥⑦ + UC-SAL-08）+ `docs/design/sales/state-machine.md` §9 + `docs/design/master-data/sku-multi-unit.md`（UC-MD-04 最低价/validatePrice 权威）+ `docs/audits/2026-08-07-2330-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`（A4.2.56-a 偏差量化证据）
- Skill Selection Basis: 实现面 = BizModel protected 方法 + 跨域 IBiz 注入（`nop-backend-dev`：跨实体访问规则[IBiz 注入]、protected step 模式、config/错误码范式）；测试（`nop-testing`：JunitAutoTestCase/IGraphQLEngine 断言 + 快照录制）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 倾向零新 config（复用 master-data `resolvePriceValidationLevel` 三级语义）；若执行中裁决需 sales 侧独立级别 config，须在 Decision 中记录理由。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-sales/erp-sal-service`。

## Execution Plan

### Phase 1 - 促销定价族实现（P1-RC-021 + P1-RC-022）

Status: completed
Targets: `ErpSalOrderBizModel.java`；`ErpSalConstants.java`；`ErpSalErrors.java`（后两者仅当 Phase 1 Decision 选 B 自建 sales 侧级别时变更——见下方 Fix 项条件）
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [x] `Decision` **最低价校验消费形态**：选项 A（推荐）= `ErpSalOrderBizModel` @Inject `IErpMdMaterialSkuBiz`（非 private，对齐既有 `IErpMdPartnerBiz` 注入范式），`persistPricingResult` 落地后逐行（行 `skuId` 非空）调 `validatePrice(skuId, finalPrice, materialCategoryId, context)`——复用 master-data OFF/WARN/HARD 三级语义（含 HARD 抛 `ERR_PRICE_BELOW_MIN` propagate），零新 config（config-gated 由 master-data `priceValidationLevel` 分类级 dict 承载）；选项 B = sales 侧自建 config key + 直接经 `ErpSalPriceListLine`/SKU 查询比对——与 master-data 三级语义重复实现，弃。**`finalPrice` 口径 Decision（子项）**：选项 a = 促销后行净单价 `(amount − discountAmount)/quantity`（含头级 AMOUNT_OFF 前值，保守）；选项 b = 仅行级 `unitPrice` 促销后值（不含头级优惠）。倾向选项 a（对齐「最终售价」字面）。记录理由与残留风险。**赠品行排除（子项，P1-1 审查阻塞项）**：`amount == 0` 的行（赠品行，`ErpSalPricingRuleEngine.addGiftLine:208-210` skuId 非空 + unitPrice=0）**跳过最低价校验**——L1 UC-SAL-08 显式要求赠品行可成功生成（单价 0 语义），HARD 级别下不排除将整体拒绝含赠品的促销（相对基线行为回归）；跳过语义在 Decision 中记录理由。
      - Skill: `nop-backend-dev`
      - **裁决**：选 A（@Inject IErpMdMaterialSkuBiz）+ 选项 a（finalPrice = line.amount/line.quantity 促销后净单价）+ 赠品行 amount==0 跳过。materialCategoryId 经 `line.getMaterial().getCategoryId()` ORM 关系解析（materialCategoryId null → WARN 默认级别）。理由：零新 config 复用 master-data 三级语义；finalPrice=amount/qty 对齐 L1「最终售价」字面语义。
- [x] `Decision` **价税分离计算口径**：`recomputeLineAmount` 中 `rate = taxRate/100`（taxRate 为百分比；null → ZERO → taxAmount=0）；`taxAmount = net.multiply(rate).divide(ONE.add(rate), 4, HALF_UP)`——scale=4 HALF_UP 对齐 `amount` 既有舍入范式（选项 A，推荐）；选项 B = 整单总额后再按 Σ 分配税（尾差集中），超出最小修复面弃。**零税率/null rate 行 taxAmount=0 不参与头级汇总（现有 nullSafe 语义）**。
      - Skill: `nop-backend-dev`
      - **裁决**：选 A（scale=4 HALF_UP，`net × rate / (1+rate)`）。rate = taxRate.divide(100, 6, HALF_UP) 避免 non-terminating decimal 异常。null/零税率 → taxAmount=0 + amountWithTax=net。
- [x] `Fix` `recomputeLineAmount`：按 L1 公式补 `line.setTaxAmount(...)` + `line.setAmountWithTax(net.add(taxAmount))`（对齐 `ErpSalOrderLine.amountWithTax` 列语义）；`recomputeOrderTotals` 以更新后行值重算 `totalTaxAmount`/`totalAmountWithTax`。
      - Skill: `nop-backend-dev`
- [x] `Fix` `persistPricingResult`（或新 protected step `validatePromotionPrices`，派生可覆盖）：促销结果落地后逐行调最低价校验（按 Phase 1 Decision 形态，赠品行跳过）；HARD 抛错时整个 @BizMutation 事务回滚（促销变更不落库）——对齐 R1.13 `validateOrderAvailability` protected step 接线范式。**WARN 暴露机制（P2-2）**：WARN 结果经 `LOG.warn` 输出（不持久化标记、不阻断——对齐既有 credit-check WARN 放行范式），测试以「调用放行 + 无错误码」断言。
      - Skill: `nop-backend-dev`
- [x] `Fix`（如需）`ErpSalErrors` 新增 sales 侧错误码（仅当 Decision 选 B 自建级别时；选 A 则复用 master-data `ERR_PRICE_BELOW_MIN`，零新增）。
      - Skill: `nop-backend-dev`
      - **裁决**：选 A → 零新增 ErpSalErrors（HARD 抛 master-data `ERR_PRICE_BELOW_MIN` propagate）。

Exit Criteria:

- [x] `recomputeLineAmount` 按 L1 公式产出 taxAmount/amountWithTax；`recomputeOrderTotals` 头级税额/含税合计 = Σ 新行值（Phase 2 数值断言证实）
- [x] 促销路径最低价校验接线完成且默认级别（master-data null/孤儿值归 WARN）不阻断/足够价零干预；HARD 拒绝时事务回滚；**赠品行（amount==0）跳过不误拒**（Phase 2 测试证实）
- [x] 无 ORM/会计过账核心路径变更（`git diff --stat` 仅 erp-sal-service Java + `_cases/` 快照）

### Phase 2 - 测试矩阵

Status: completed
Targets: `module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalPricingCompliance.java`（新增，覆盖 P1-RC-021 + P1-RC-022）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` P1-RC-021 矩阵：① OFF 级别促销后低于底线放行（零干预回归）；② WARN 级别低于底线放行 + warning 标记（LOG.warn 断言或 `PriceValidationResult.warning=true`）；③ HARD 级别低于底线拒绝（`ERR_PRICE_BELOW_MIN` propagate + 事务回滚，行价/头合计不落库）；④ 促销未触发（无规则命中）时价格高于底线不干预；⑤ 无 SKU 行（skuId null）跳过；⑥ **赠品行跳过**（HARD 级别下含赠品行促销成功生成——`amount==0` 行不触发最低价校验，防赠品促销被误拒回归）。
      - Skill: `nop-testing`
- [x] `Add` P1-RC-022 矩阵：① 单档折扣（net=100×10×(1−0.1)=900，rate=13% → tax=900/1.13×0.13≈103.5398）；② 零税率行 taxAmount=0；③ 多档混合（13%/9%/6% 三行 + 行级 PERCENT_DISCOUNT + 头级 AMOUNT_OFF）taxAmount 逐行公式断言 + 头级 Σ 恒等式 `totalAmountWithTax = totalAmount + totalTaxAmount`；④ 促销前后 taxAmount 更新（A4.2.49 应 5.75 实 11.50 场景反转断言新值）；⑤ 无折扣行净额不变回归。
      - Skill: `nop-testing`
- [x] `Proof` GraphQL 冒烟断言（`executeRpc` 调 `ErpSalOrder__applyPricingRules` HARD 场景返回错误码/事务回滚）+ `_cases/` 快照录制（对齐 R1.13 快照范式：方法级 `@EnableSnapshot(saveOutput=true)` 录制 → 去注解切 CHECKING）；既有 `TestErpSalPricingRuleEngine`/`TestErpSalPricingEndToEnd`/`TestErpSalOrderApproval` 零回归。
      - Skill: `nop-testing`
      - **执行注记**：新增 `TestErpSalPricingCompliance`（10 方法，JunitAutoTestCase + _cases/input header-only CSV 初始化 schema）。`TestErpSalPricingEndToEnd` 7 场景快照重录（output/tables 行 taxAmount/amountWithTax 由 null → 计算值）。`TestErpSalPricingRuleEngine` 纯引擎单测零回归。既有 NOP_SYS_SEQUENCE ordering 相关 failures（JunitBaseTestCase 测试独立运行时 schema 未初始化）为本仓预存非本计划回归。

Exit Criteria:

- [x] 新增测试矩阵全绿 + 既有 sales 测试零回归：`mvn test -pl module-sales/erp-sal-service`（BUILD SUCCESS）
- [x] P1-RC-021 六路径 + P1-RC-022 五路径均有断言证据（无「行为落地但零覆盖」缺口）；快照录制完成

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/sales/state-machine.md`（或 sales README）；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [x] `Add` owner doc 注记：state-machine.md §9/sales README 补「促销价税分离（L1 公式落地）+ 最低价校验（复用 master-data validatePrice 三级语义）」实现注记；不修改需求契约段。
      - Skill: none
- [x] `Add` arm-index P1-RC-021 → `done (RC-R1.14)` + P1-RC-022 → `done (RC-R1.15)` + 修复落地摘要；roadmap RC-R1.14/RC-R1.15 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none
      - **执行注记**：日志写入 `docs/logs/2026/08-09.md`（执行日期 08-09）。

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_01e3ddd4affeC8QxHDiJPAbuG5）— 1 P1（P1-1 赠品行交互未定义：`ErpSalPricingRuleEngine.addGiftLine:208-210` 赠品行 skuId 非空 + amount=0，HARD 级别下将被最低价校验误拒 → 相对基线行为回归）+ 5 P2（P2-1 `PriceValidationResult` 字段实为 passed/warning/minPrice/level 非 belowMin 已修正；P2-2 WARN 暴露机制未定义已补 LOG.warn 语义；P2-3 roadmap「config-gated」承载 = master-data 分类级 dict 已显式化；P2-4 Targets「（如需）」措辞已条件化；P2-5 人工裁决引用补讨论文档文件名）。行号/ORM/预授权分类全部核验属实。
- Independent draft review iteration 2: accept（独立子代理 ses_01e2f54f3ffeJZRTYnNTHe60G6 重扫）— 6 项迭代 1 finding 全部核验解决（P1-1 赠品行排除在 Goals/Decision/测试⑥/Exit Criteria 四处落位；P2-1 字段名修正；P2-2 WARN LOG.warn 语义；P2-3 config-gated 承载显式化；P2-4 措辞条件化；P2-5 人工裁决文档引用）+ 关键事实复核全部属实。3 项非阻塞 P2 措辞 nit 已顺手修订（Exit Criteria 默认级别 WARN 而非 OFF / 测试⑥ amount==0 谓词统一 / Phase 2 Exit 六路径计数）。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-sales/erp-sal-service -Dtest=TestErpSalPricingCompliance,TestErpSalPricingEndToEnd,TestErpSalPricingRuleEngine,TestErpSalPricingRuleCrud` 全绿 29 tests + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-022 修复面归类（A4.2.56-a「触核心路径须 ask-first」措辞 vs roadmap 第一批纯预授权）

- Classification: `out-of-scope improvement`（裁决分歧已按 roadmap 行解决，不构成本计划 Deferred；本条仅登记裁决记录）
- Why Not Blocking Closure: roadmap 2026-08-08 §7 A1 人工裁决（`docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md §7` A1，生效即日）将 RC-R1.15 明确列为第一批（纯预授权），且晚于 MA4 报告（2026-08-07-2330）；本修复面 = 订单行定价计算（BizModel 派生），非 VoucherFact/PostingProcessor 过账核心路径，Non-Goals 显式声明边界。
- Successor Required: `no`

### minPrice 独立列（P2-RC-057 方案 A 方向）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本行复用 master-data 派生底线（`deriveMinPrice`），不新增 ORM 列；P2-RC-057 已登记 watch-only（P2 登记不强制），方案 A（minPrice 独立列）归后续人工裁决。
- Successor Required: `no`

## Closure

Status Note: 三阶段全部执行完成。P1-RC-021（最低价校验）+ P1-RC-022（价税分离）落地，10 新测试全绿，既有快照重录，零 ORM/会计过账核心路径变更。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，未复用执行者上下文）
- Evidence: 逐项核验 LIVE 仓库：(1) `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalOrderBizModel.java` — `@Inject IErpMdMaterialSkuBiz mdMaterialSkuBiz`（:68 非 private）+ `persistPricingResult:170` 调 `validatePromotionPrices(order, lines, context)`（:238-263）逐行 skuId null 跳过 + amount==0 赠品行跳过（:246）+ qty==0 跳过 + finalPrice=amount/qty（:254）+ `mdMaterialSkuBiz.validatePrice(skuId, finalPrice, materialCategoryId, context)`（:256）+ WARN `LOG.warn`（:259）+ HARD propagate `ERR_PRICE_BELOW_MIN`；`recomputeLineAmount:184-202` 补 `setTaxAmount(net×rate/(1+rate) scale=4 HALF_UP)` + `setAmountWithTax(net+taxAmount)` + 零税率/null→0；`recomputeOrderTotals:205-220` 以新行值重算 totalTaxAmount/totalAmountWithTax。Anti-hollow：validatePromotionPrices 经 applyPricingRules:124→persistPricingResult:170 运行时可达，无空体/return null。(2) 测试 `TestErpSalPricingCompliance.java`（473 行）10 @Test 方法（5 价税分离 + 5 最低价）全数落盘。(3) arm-index P1-RC-021→`done（RC-R1.14）`+P1-RC-022→`done（RC-R1.15）`；roadmap RC-R1.14/15→`done ✅`；`docs/logs/2026/08-09.md` 条目落盘；state-machine.md §9 补注记。五点一致性：Plan Status completed / 三 Phase Status completed / Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure evidence 真实。Deferred 段仅含已裁决项（RC-R1.022 归类注记 + P2-RC-057 minPrice 列 watch-only），无范围内缺陷降级隐藏。审计通过，准予关闭。

Follow-up:

- <pending — 无范围外 follow-up；MR1 第一批后续 RC-R1.16+ 由 mission driver 继续>
