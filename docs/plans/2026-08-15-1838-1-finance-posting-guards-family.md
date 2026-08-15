# 2026-08-15-1838-1-finance-posting-guards-family RC-R1.41 + RC-R1.42 — finance 过账引擎守卫族（GlDistribution 科目分摊 + 汇率缺失拒绝，MR1 第二批 B 类预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.41（P1-RC-001 finance GlDistribution 科目分摊）+ RC-R1.42（P1-RC-002 finance 汇率缺失拒绝过账）— 同域（finance）同 owner doc（`posting.md`）同结果表面（过账引擎前置校验），按计划指南规则 14 合并为一个 owner plan 的两个阶段
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.41/RC-R1.42 行 + `docs/audits/arm-index.md` P1-RC-001/P1-RC-002 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 B 类：降级为预授权自动执行，不触 ORM ask-first**）
> Related: `docs/design/finance/use-cases.md`（L1 UC-FIN-04/15/12）；`docs/design/finance/posting.md`（§FactsValidator）；`docs/design/finance/cost-center.md`（§ErpFinGlDistribution 设计参考）；`docs/audits/2026-08-07-0330-rc-ma4-a4-1-2-fx-rate-missing-trigger-surface.md`（A4.1.2 汇率缺失触发面）；`docs/plans/2026-08-08-1603-2-rc-mr1-r1-12-commitment-restore-symmetry.md`（同批 Q4 收敛性会计修复范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-001（arm-index 行，UC-FIN-04/15 科目分摊完全缺失）**：L1（`use-cases.md:76,298`）逐字要求 GlDistribution 规则拆行 + Σpercent!=100 拒绝过账 + 平衡保持 + `ErpFinGlDistributionValidator(IErpFinFactsValidator 实现)` Bean。L3 实仓（HEAD 核查）：`ErpFinGlDistributionValidator` 类不存在（`rg "implements IErpFinFactsValidator"` 全仓零命中）；`ErpFinGlDistribution` ORM 实体不存在（`module-finance/model/app-erp-finance.orm.xml` 零命中）；FactsValidator 调用循环 `ErpFinPostingProcessor.java:563-565` 在无注入时为死代码（`registry.getValidators()` 返回空列表）；"科目分摊"唯一痕迹是 UI 菜单 `erp-fin.action-auth.xml:189-196`。§2 P1①（功能完全缺失）。**非 P0**（分摊未启用时主路径不受影响）。
- **finding P1-RC-002（arm-index 行，UC-FIN-12 断言② 汇率缺失静默回退）**：L1（`use-cases.md:230`）逐字「若 汇率缺失 → 报错拒绝过账」。L3 实仓：`ErpFinPostingProcessor.prepareContext:531-539` 实现 `ctx.setExchangeRate(event.getExchangeRate() != null ? event.getExchangeRate() : EXCHANGE_RATE_DEFAULT)`（`EXCHANGE_RATE_DEFAULT=new BigDecimal("1")` :78）——**静默回退到 1 继续过账，与「拒绝」直接冲突**；`persistVoucher:817-820` 同型回退。§2 P1②（异常路径未实现）。**非 P0**（触发面依赖域调用方漏传 rate，非默认活跃路径；A4.1.2 已普查各域 Provider 显式传 rate）。
- **实仓（HEAD 核查）**：
  - `IErpFinFactsValidator` 接口（`app/erp/fin/service/posting/IErpFinFactsValidator.java`）：`List<VoucherFact> validate(List<VoucherFact> facts, AcctDocContext ctx)` + `int getOrder()`（升序执行）——**SPI 已存在且被编排消费**。
  - `ErpFinAcctDocRegistry`（`posting/ErpFinAcctDocRegistry.java`）：`setValidators(List<IErpFinFactsValidator>)`（:41-42）+ `init()` 按 getOrder 升序排序（:76-78）+ `getValidators()`（:85）。IoC 接线：`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml:14-24` — `<property name="validators"><ioc:collect-beans by-type="app.erp.fin.service.posting.IErpFinFactsValidator" .../>` → **新增 Validator Bean 即自动聚合进 FactsValidator 链，零核心改动**。
  - 调用点：`ErpFinPostingProcessor.generateFacts:563-565`（`for (IErpFinFactsValidator validator : registry.getValidators()) facts = validator.validate(facts, ctx);`）。
  - 成本中心维度载体**已就绪**：`ErpFinVoucherLine.costCenterId`（`app-erp-finance.orm.xml:499` propId 23 + to-one `costCenter` :518）——L2 `cost-center.md §业务规则 1` 要求的凭证行 costCenterId 已存在；`ErpMdCostCenter` 实体存在（`app-erp-master-data.orm.xml:1121`）。
  - **2026-08-12 批量裁决 B 类（路线图头部登记，批准人：用户）**：RC-R1.41 经 5 路并行独立 agent 核实，裁定「FactsValidator SPI 已存在，实现 Validator bean」——**不需要 ORM 结构变更**（`ErpFinGlDistribution`/`ErpFinGlDistributionLine` 实体不新增），从越界项 ask-first **降级为预授权自动执行**。规则载体须在预授权范围内裁决（见 Phase 1 Decision：config/bean 驱动规则，**不新增 ORM 实体**；若 Explore 阶段证实实体不可避免 → 超出 B 类授权范围 → 按「越界回落 ask-first」暂停等待人工，见 Non-Goals 与 Deferred 节）。L2 `cost-center.md §ErpFinGlDistribution`（:32-60 实体设计）与裁决的关系 = 设计参考非强制载体，本行按裁决不物化实体（记录于 owner doc 注记）。
  - 汇率守卫判定载体：`ErpMdCurrency.isFunctional`（`app-erp-master-data.orm.xml:702`）——**本位币判定字段已存在**，可区分「本位币 rate=1 正确语义」vs「外币 rate 缺失应拒绝」（Decision 项，见 Phase 1）。
  - 测试基线：`TestErpFinPostingService`（`posting/` 包）覆盖多币种行级断言（`testMultiCurrencyPostingLineLevelAssertions:250`，rate 6.5 显式传）；`TestErpFinAcctDocRegistry` 覆盖 registry 聚合；`TestErpFinVoucherTemplateCrudSmoke`。GlDistribution/FX 守卫零测试。
- **预授权判据（2026-08-12 裁决 B 类）**：纯代码逻辑（Validator Bean + config/bean 规则载体 + prepareContext 守卫 + 错误码 + 测试），**不触 ORM 结构（零列/零实体）/会计过账核心路径[不触及 VoucherFact 写入链路本身，Validator 是既有 SPI 消费点，prepareContext 是既有 ctx 构造点，守卫属前置校验]/删除**；roadmap RC-R1.41/42 行 `todo`，Deps（R1.0 done）已满足。**仍需独立 plan-audit**（Q4 收敛性会计修复：「核心路径改动行为仍须独立 plan-audit」——独立草案审查 + 独立结束审计为本计划标准义务）。
- **涉及文件**：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java`（prepareContext 守卫 + FactsValidator 链消费点上下文）；新增 `ErpFinGlDistributionValidator.java`（`posting/` 包，implements IErpFinFactsValidator）；`ErpFinErrors.java`（新增 `ERR_EXCHANGE_RATE_REQUIRED` + 分摊相关错误码如 `ERR_GL_DISTRIBUTION_PERCENT_SUM`）；`ErpFinConfigs.java` 或 `ErpFinConstants.java`（规则载体 config key 若采用 config 驱动）；`app-service.beans.xml`（Validator Bean 注册，如非自动收集则显式注册）；测试 `TestErpFinGlDistribution.java` + `TestErpFinFxRateGuard.java`；owner doc `posting.md`（§FactsValidator 实现注记）+ `cost-center.md`（实体不物化的裁决注记）+ arm-index/roadmap/`docs/logs/`（回填）。

## Goals

- **GlDistribution 科目分摊运行时成立（P1-RC-001 核心）**：新增 `ErpFinGlDistributionValidator implements IErpFinFactsValidator`（Bean 注册，经 `ErpFinAcctDocRegistry` 自动聚合）——对命中分摊规则的 VoucherFact 行按规则拆行（源 costCenterId + 目标维度 → 多条目标行），Σ 拆分行金额 == 原行金额（平衡保持），规则 Σpercent != 100 → 抛 NopException 拒绝过账（L1 断言逐字满足）。
- **规则载体在预授权范围内裁决**：规则按 B 类裁决以非 ORM 载体承载（Phase 1 Explore 裁决：config 驱动（如 AppConfig JSON 或 Bean 内静态规则表）优先；**不新增 ErpFinGlDistribution 实体**——超出 B 类授权即暂停 ask-first）。
- **汇率缺失拒绝过账（P1-RC-002 核心）**：`prepareContext` 加 rate 缺失守卫——事件币种为非本位币（`ErpMdCurrency.isFunctional=false`）且 `event.getExchangeRate()==null` → 抛新增 `ERR_EXCHANGE_RATE_REQUIRED` 拒绝过账；本位币（isFunctional=true）rate 缺失 → 保留 rate=1 语义（本位币折算恒等式，非「汇率缺失」场景）——**Decision 项须显式记录**（见 Phase 1）。
- **错误码**：`ERR_EXCHANGE_RATE_REQUIRED`（`erp.err.fin.exchange-rate-required`，中文描述含 `{currencyCode}` 参数）+ GlDistribution 相关错误码（如 `ERR_GL_DISTRIBUTION_PERCENT_SUM`，参数含 ruleCode/实际 Σ/预期 100），对齐 `ErpFinErrors` 既有风格。
- **测试**：`TestErpFinGlDistribution` 覆盖——① 命中规则拆行金额守恒（60/40 断言）；② Σpercent!=100 拒绝（错误码 + 过账不落库）；③ 无规则命中原样透传（零回归）；④ 规则载体 config 加载路径。`TestErpFinFxRateGuard` 覆盖——① 外币 rate 缺失拒绝（错误码）；② 外币 rate 显式传放行；③ 本位币 rate 缺失放行（rate=1）；④ 既有多币种测试零回归。
- **owner doc 收敛**：`posting.md §FactsValidator` 补 GlDistribution 实现注记（Validator Bean + 规则载体 + 平衡保持语义）**并修正 :228 失实声明**（「本工程不强制实现 GL Distribution…」——本行落地后该句陈旧，改为实现注记）；`cost-center.md §ErpFinGlDistribution` 补「实体未物化（2026-08-12 裁决 B 类），规则以非 ORM 载体承载」注记；不修改需求契约段（use-cases L1 不动）。
- **零回归**：既有 finance 测试全绿（`TestErpFinPostingService` 等）+ 全仓构建 + compliance checker 零漂移（Validator Bean 无 daoFor/无 REQUIRES_NEW——规则载体为 config/静态则零 R2c 漂移；若裁决需要查询 ErpMdCostCenter 须经 `IErpMdCostCenterBiz` 注入则登记 per-site 证据）。
- **裁决与路线图行的关系（超期记录）**：roadmap RC-R1.41/42 行（:433-434）与 arm-index P1-RC-001/002 行仍携带裁决前「须…ask-first 人工确认 checkbox」字样——2026-08-12 批量裁决 B 类（roadmap 头部 :48-49，批准人：用户，生效即日）**正式降级为预授权自动执行**，本计划以头部裁决为准；闭包回填（roadmap 行 → done）时同步消除该字样歧义。

## Non-Goals

- **不新增 ErpFinGlDistribution/ErpFinGlDistributionLine ORM 实体**（2026-08-12 裁决 B 类排除 ORM；Explore 若证实必须 → 越界回落 ask-first 暂停，不静默越界）。
- **不实现 P1-RC-004（自动勾对对方账号维度）**（独立 finding，属 RC-R1.43，ORM 3 列 A 类授权，非本行范围）。
- **不实现 P1-RC-091（试算平衡 COMMITMENT 排除）**（独立 finding，属 RC-R1.46，5 GL 读路径过滤，非本行范围）。
- **不改 VoucherFact/Provider/persistVoucher 写入链路**（守卫在 prepareContext 前置 + Validator 在 SPI 消费点执行，不重构过账编排）。
- **不实现成本中心树形汇总/利润中心结转/行级权限**（cost-center.md §业务规则 4-7，独立后续工作，非本行）。
- **不实现分摊规则 CRUD 管理界面**（规则载体为 config/静态时无 UI 面；若未来实体化按 ask-first 流程单独立项）。
- **不改真相源契约段落**（use-cases L1 不动；posting.md 既有 §FactsValidator 语义不动）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B；2026-08-12 裁决 B 类预授权）
- Owner Docs: `docs/design/finance/use-cases.md`（L1 UC-FIN-04/15/12）+ `docs/design/finance/posting.md`（§FactsValidator）+ `docs/design/finance/cost-center.md`（§ErpFinGlDistribution 设计参考）
- Skill Selection Basis: 实现面 = FactsValidator SPI 实现 + prepareContext 守卫 + 错误码 + config 规则载体（`nop-backend-dev`：SPI 实现范式、错误码范式、Bean 注册、平台工具）；测试（`nop-testing`：JunitBaseTestCase 直断言 + GraphQL RPC 冒烟范式）。无 view.xml/xbiz/ORM 变更（Validator 是纯 Java SPI 实现 + Bean 注册；守卫在既有 Processor 方法内）。

## Infrastructure And Config Prereqs

- 无新环境变量/外部服务（规则载体若采用 config 驱动 = AppConfig key，无基础设施依赖）。
- 分域验证前置：`mvn install -DskipTests`（若涉及 config key 常量无生成链变更则仅编译）后 `mvn test -pl module-finance/erp-fin-service`。

## Execution Plan

### Phase 1 - Explore 规则载体 + FX 守卫判定裁决（Decision）

Status: completed
Targets: `IErpFinFactsValidator.java`；`ErpFinAcctDocRegistry.java`；`ErpFinPostingProcessor.java`（prepareContext/generateFacts）；`ErpMdCurrency.isFunctional`；`posting.md`；`cost-center.md`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **GlDistribution 规则载体裁决（D1）**：**选项 A（选定）** = Bean 内静态规则表（`ErpFinGlDistributionValidator` 持有 `List<ErpFinGlDistributionRule>`，setter 注入；规则 = ruleCode + 源匹配键[sourceSubjectCode/sourceCostCenterId 任一非空] + 生效窗口 validFrom/validTo + isActive + 目标行[targetCostCenterId, percent]列表）。生产 beans.xml 注册 Validator Bean（默认空规则表 = 零行为变更，分摊未启用时主路径不受影响）；规则表经 beans.xml property 或 Delta 同名 bean 覆盖注入（下游可定制）。**选项 B（否决，越界）** = 新增 `ErpFinGlDistribution` ORM 实体承载规则（超出 2026-08-12 裁决 B 类「不需要 ORM 结构变更」授权 → 须 ask-first 暂停）。**选项 A'（否决）** = `ErpFinVoucherTemplateLine.amountExpression` 模板行驱动（orm.xml:556+）——模板行仅承载 subjectCode/amountExpression，**无 targetCostCenterId 目标维度字段**，无法表达「目标成本中心 + percent」规则结构 → 不可复用，记录。**理由**：裁决 B 明示「FactsValidator SPI 已存在，实现 Validator bean」= 纯代码逻辑修复；L1 UC-FIN-04/15 断言核心是「命中规则 → 拆行 + Σ 守恒 + Σpercent!=100 拒绝」，规则来源未限定 ORM；Bean 静态规则表零 ORM 变更面 + 零 daoFor（compliance R2c 零漂移）+ 下游 Delta 可覆盖（产品化可定制性对齐 `nop-backend-dev` skill 自检）。**残余风险**：规则变更须重启/Delta 覆盖（无运行时管理 UI）——归 Deferred But Adjudicated（实体化 successor 已登记于计划 Deferred 节）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **FX 守卫判定边界（D2）**：**选项 A（选定）** = 仅当 `event.getCurrencyId()!=null` 且币种 `isFunctional=false` 且 `event.getExchangeRate()==null` → 抛 `ERR_EXCHANGE_RATE_REQUIRED`；本位币或 currencyId=null 保留 rate=1 语义。**实现机制（实仓核查修正）**：事件币种行查询经 `bizObjectManager.getBizObject("ErpMdCurrency").asProxy()`（`IErpMdCurrencyBiz`）按 `eq("id", currencyId)` + limit 1 调 `findFirst`，读该行 `isFunctional` 字段判定——判定载体 = `ErpMdCurrency.isFunctional`（`eq("isFunctional",TRUE)` 查询范式同载体，对齐既有 5 处消费范式；`isFunctional` 在 `_ErpMdCurrency.xmeta:46` `queryable="true"` 已 grep 核实 → ICrudBiz 管道过滤可通行，**零 daoFor → compliance R2c 零漂移**）——对齐同文件 `resolveSubjects:611-612` 的非 BizModel 编排 bean 按名解析范式（`@Inject I*Biz` 字段不适用于本类：@BizModel 实现非标准 IoC 类型注入 bean，注释 :610 已记录该约束）；币种行不存在 → LOG.warn 保守放行（与 currencyId=null 同语义，登记残余风险）。**选项 B（否决）** = 一律 rate 缺失即拒绝（本位币单币种场景被误拒，破坏既有单币种主路径——`persistVoucher:817-820` 对大量单币种单据恒 rate=1 语义，误拒即回归）。**理由**：L1「汇率缺失 → 拒绝」的语义对象是外币折算缺失（A4.1.2 普查证实各域 Provider 外币场景显式传 rate，单币种不传是既有正确语义）；A 方案守住单币种主路径零回归 + 外币缺失显式报错，满足 L1 字面且不破坏既有行为。**备选载体记录**：本位币判定亦可经 `ErpMdAcctSchema.functionalCurrencyId`（`app-erp-master-data.orm.xml:970`，mandatory + 索引）解析——既有 5 处消费范式（`ExchangeRevaluationService:283`/`ProfitLossClosingService:219`/`AnnualCloseService:401`/`BadDebtProvisionService:367`/`ErpFinNotesReceivableProcessor:322` 均 `eq("isFunctional",TRUE)` 查询，本实现复用之），`ErpFinNotesReceivableProcessor:308-316` 是 D2 选项 A 的精确同型先例（currencyId==null → 保守放行）；`functionalCurrencyId` 作兜底载体记录（schema 级本位币多币种细分归 successor，本守卫沿用 schema 无关的 isFunctional 单查询范式）。**残余风险**：currencyId=null 或币种不存在时无法判定币种归属 → 放行 rate=1（保守放行，登记 Deferred watch-only）。
      - Skill: `nop-backend-dev`
- [x] `Proof` **运行时验证前置**：既有测试基线确认——`TestErpFinPostingService.testMultiCurrencyPostingLineLevelAssertions`（:250-283）显式传 rate 6.5 全绿；`TestErpFinAcctDocRegistry`（:55-81）聚合验证全绿；无 GlDistribution/FX 守卫测试（grep `implements IErpFinFactsValidator` 生产代码零实现——仅接口定义 + registry 集合引用，已核实）。Validator 链消费点运行时可达（`generateFacts:563-565`，registry.getValidators() 经 beans.xml:20-23 collect-beans 聚合）。新增测试采用 R1.32 直断言范式（JunitAutoTestCase + Java 断言 + 不录快照，拒绝路径不录 `_cases`）。
      - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] D1（规则载体）+ D2（FX 守卫边界）裁决记录落盘计划（选择 + 备选 + 理由 + 残余风险），Explore 证据（SPI 消费点 + isFunctional 字段 + 测试基线）确认
- [x] 三个已命名风险点核查完成：错误码定义位置（`ErpFinErrors.java` 既有 `erp.err.fin.*` 风格先例——:436-446 `erp.err.fin.voucher.*` 族 + :250-252 `notes.discount-fx-rate-required` 先例）、Validator Bean 注册方式（`app-service.beans.xml:20-23` `ioc:collect-beans by-type` 自动聚合已核实）、`IErpMdCurrencyBiz`（`erp-md-dao/.../biz/IErpMdCurrencyBiz.java:12`）+ `IErpMdCostCenterBiz`（`erp-md-dao/.../biz/IErpMdCostCenterBiz.java:8`）注入点接口存在——逐项 grep 证据；注：D1 静态规则载体为纯内存匹配，**无需** `IErpMdCostCenterBiz` 查询；D2 经 bizObjectManager 按名解析 `IErpMdCurrencyBiz`（非 @Inject 字段，记录修正）

### Phase 2 - GlDistribution Validator 落地（P1-RC-001 核心）

Status: completed
Targets: 新增 `ErpFinGlDistributionValidator.java`；`ErpFinErrors.java`；`app-service.beans.xml`（如需显式注册）；owner doc `posting.md`/`cost-center.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 完成（D1 裁决）

- [x] `Add` 新增错误码 `ERR_GL_DISTRIBUTION_PERCENT_SUM`（字符串 `erp.err.fin.gl-distribution.percent-sum`，描述中文「科目分摊规则 {ruleCode} 分摊比例合计 {percentSum} 不等于 100，拒绝过账」+ ARG_RULE_CODE/ARG_PERCENT_SUM 参数），对齐 `ErpFinErrors` 既有 `ErrorCode.define` 风格。
      - Skill: `nop-backend-dev`
- [x] `Add` 实现 `ErpFinGlDistributionValidator implements IErpFinFactsValidator`（D1 裁决 Bean 静态规则表载体）：`validate(facts, ctx)` 对每条 fact 匹配规则（源 subjectCode/costCenterId 判定[任一非空源键 AND 语义] + 生效窗口 validFrom/validTo + 启用态）→ 命中则拆行（每条目标行复制原 fact 字段 + 改 costCenterId + 金额 = 原金额 × percent/100，scale 4 HALF_UP，末行补差保证 Σ==原金额）→ Σpercent 校验（`abs(Σ−100) > 0.000001` → 抛 `ERR_GL_DISTRIBUTION_PERCENT_SUM`）；`getOrder()` = 100（较高值，对齐 L2 cost-center.md §业务规则 2 + L1 UC-FIN-15）；Bean 注册（`app-service.beans.xml` 显式注册，经 `ioc:collect-beans by-type` 自动聚合）。
      - Skill: `nop-backend-dev`
- [x] `Fix` 拆行平衡保持断言：拆行后 Σ 拆分行金额（source 口径）== 原行金额——split() 内末行补差保证（对齐 `ErpFinPostingProcessor.assertBalanced:736-742` 语义，不破坏借贷平衡）；**扩展**：`amountSource`/`amountFunctional` 同比例拆分（仅原值非 null 时设置，null 保持回退语义——persistVoucher 对 null 回退到 amount，单币种向后兼容；避免仅拆 `amount` 致 FX-capable Provider 双金额行 debit/credit 重复计量）。
      - Skill: `nop-backend-dev`
- [x] `Add` 测试 `TestErpFinGlDistribution`（JunitAutoTestCase + 直断言范式，R1.32，不录快照）：**测试策略记录** = 引擎级容器测试——`@NopTestConfig(testBeansFile="/erp/fin/beans/test-gl-distribution.beans.xml")` 注入测试专用规则 Validator（test 前缀 bean id，add-test-mock-bean.md 范式，与生产空规则表 Validator 一同被 registry 聚合），经 `voucherBiz.post` 断言引擎链路行为：① 命中规则 60/40 拆行（6602 → 101:60 / 102:40，4 行，行级金额 60/40，Σ==100，头级借贷合计不变）+ 金额守恒 + costCenterId 正确；② Σpercent=95 拒绝（9901 规则，错误码 + ruleCode/Σ 参数 + 凭证/回链零落库）；③ 无规则命中透传（7701，3 行不变 + costCenterId null）；④ 规则载体 Bean 静态表加载路径（`BeanContainer.instance().getBean("testGlDistributionValidator")` 取回规则断言）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] `ErpFinGlDistributionValidator` 实现 + Bean 注册 + 错误码落地（Validator 经 registry 聚合 + `ERR_GL_DISTRIBUTION_PERCENT_SUM` 定义 + 测试全绿：拆行守恒/Σ 拒绝/透传三断言通过）——TestErpFinGlDistribution 4 组全绿
- [x] owner doc 注记落地（`posting.md` §FactsValidator 实现注记 + 陈旧声明修正 + `cost-center.md` §ErpFinGlDistribution 实体未物化裁决注记 + §业务规则 2 实现注记）

### Phase 3 - FX 汇率缺失守卫落地（P1-RC-002 核心）

Status: completed
Targets: `ErpFinPostingProcessor.java`（prepareContext:531-539）；`ErpFinErrors.java`；owner doc `posting.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 完成（D2 裁决）

- [x] `Add` 新增错误码 `ERR_EXCHANGE_RATE_REQUIRED`（字符串 `erp.err.fin.exchange-rate-required`，描述中文「币种 {currencyCode} 的汇率缺失，无法折算过账（外币过账必须显式提供 exchangeRate）」+ ARG_CURRENCY_CODE 参数）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `prepareContext`（:531-539）加守卫 `guardExchangeRate`（protected step，派生可覆盖）：按 D2 裁决——`event.getCurrencyId()!=null` 且币种非本位币（经 `IErpMdCurrencyBiz` 查询 `ErpMdCurrency.isFunctional=false`）且 `event.getExchangeRate()==null` → 抛 `ERR_EXCHANGE_RATE_REQUIRED`；否则保留既有 `?: EXCHANGE_RATE_DEFAULT` 回退语义（本位币/currencyId=null/币种不存在 放行 rate=1，币种不存在 LOG.warn 保守放行）。实现机制 = `bizObjectManager.getBizObject("ErpMdCurrency").asProxy()` 按名解析（对齐同文件 resolveSubjects:611-612 非 BizModel bean 范式 + `eq("id", currencyId)` + limit 1 经 ICrudBiz.findFirst，isFunctional xmeta queryable=true 已核实，**零 daoFor → compliance R2c 零漂移**）。`persistVoucher:817-820` 兜底回退**不动**（ctx 已由 prepareContext 守卫保证；直调 persistVoucher 的路径为内部受控，不做双守卫——Decision 理由已记录）。
      - Skill: `nop-backend-dev`
- [x] `Add` 测试 `TestErpFinFxRateGuard`（JunitAutoTestCase + 直断言范式）：① 外币（USD isFunctional=false 种子）rate 缺失 → 拒绝 + 错误码 + ARG_CURRENCY_CODE="USD" + 回链零落库；② 外币 rate 显式传（6.5）→ 放行 + 行级 rate/币种落库断言；③ 本位币（RMB isFunctional=true 种子）rate 缺失 → 放行（rate=1，回归既有单币种语义）；④ 币种不存在（id=999）+ rate 缺失 → 保守放行（D2 残余风险回归断言）；既有 `TestErpFinPostingService.testMultiCurrencyPostingLineLevelAssertions` 零回归（快照不动）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] `prepareContext` 守卫接线 + `ERR_EXCHANGE_RATE_REQUIRED` 定义 + `TestErpFinFxRateGuard` 全绿（四断言通过）+ 既有多币种测试零回归（TestErpFinPostingService 全绿）
- [x] owner doc 注记落地（`posting.md` §多币种处理 补汇率缺失守卫实现注记 + D2 裁决边界）

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_ffaf7fd42ffe0IYbrUl3Xzv1E9`) — 零信任实仓核实全部 baseline 声明 PASS（SPI 接口/registry 聚合/beans.xml collect-beans/ErpFinPostingProcessor 消费点/costCenterId 载体/isFunctional 字段/零 GlDistribution 实体/测试基线/08-12 裁决 B 类文本），且补证架构核心假设端到端成立（`persistVoucher:853` 写 fact.costCenterId → Validator 改写 fact 流入落库行）；四维 checklist 全 PASS，ask-first 框架与裁决一致（B 类预授权 + 越界回落 ask-first），D1/D2 为真实开放 Decision。无 BLOCKER / 无 MAJOR。5 MINOR 已修正：(1) 条件注记 `IErpMdCurrencyBiz`→`IErpMdCostCenterBiz` 笔误；(2) 超期记录——roadmap/arm-index 行旧字样 vs 头部裁决的歧义显式声明；(3) D2 备选载体补充（`ErpMdAcctSchema.functionalCurrencyId` + 5 处 isFunctional 查询范式 + `ErpFinNotesReceivableProcessor:308-316` 同型先例）；(4) owner doc 收敛补 `posting.md:228` 失实声明修正；(5) Phase 1 Exit Criterion 2 改为三个已命名可核查风险点。

## Closure Gates

- [x] 范围内行为完成（R1.41 GlDistribution Validator + R1.42 FX 守卫）
- [x] 相关文档对齐（posting.md/cost-center.md 注记 + arm-index P1-RC-001/002 → done (RC-R1.41/42) + roadmap 行 done）
- [x] 已运行验证（`mvn clean install -DskipTests` BUILD SUCCESS（156 模块）+ `mvn test -pl module-finance/erp-fin-service` 479/479 全绿（471 基线 + 8 新增）+ `bash docs/audits/nop-compliance-checker.sh` actual == baseline 零漂移（R1d=14/R2a=34/R2b=230/R2c=1399/R2d=34/R3=5/R10=9/R12a=69/R12b=66/R12c=40 精确一致）；全 reactor `mvn test` 3 项预存在失败（TestAuthSeedLoadingProof NPE + mfg ErpMfgCostRollupLine materialBand cell-not-prop 双页面测试）经 known-good-baselines.md:63-64 核实与本次变更无关）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### currencyId=null 时 FX 守卫保守放行

- Classification: `watch-only residual`
- Why Not Blocking Closure: currencyId=null 的事件无法判定币种归属，D2 裁决放行 rate=1（保守语义）；当前各域 Provider 均显式传 currencyId（A4.1.2 普查），null 场景为边角
- Successor Required: `no`

### GlDistribution 规则实体化（ErpFinGlDistribution 实体）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 2026-08-12 裁决 B 类排除 ORM；config/静态规则载体满足 L1 UC-FIN-04/15 断言（拆行 + Σ 守恒 + Σpercent!=100 拒绝）；实体化（CRUD 管理界面 + 生效窗口管理）属产品化增强，须 ask-first 流程单独立项
- Successor Required: `yes`（若未来需要规则运行时管理界面，按 ORM ask-first 流程立项）

## Closure

Status Note: 三 Phase 全部执行完成（Phase 1 D1/D2 裁决 + Phase 2 GlDistribution Validator 落地 + Phase 3 FX 汇率缺失守卫落地）；独立结束审计五门全 PASS（计划一致性/代码存在性与正确性/反模式合规/测试与验证/文档回填），零 BLOCKER 零 MAJOR；RC-R1.41 + RC-R1.42 两个 finding 落地（arm-index done + roadmap done ✅ + owner doc 注记 + logs 首条），验证全绿（erp-fin-service 479 tests + 全量构建 + compliance 零漂移），可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_ffac8a7f3ffe9DOojOZm0kodU5（新会话结束审计，执行者未自我审计）
- Evidence: 独立审计五门逐项 PASS——(1) 计划一致性：三 Phase Status 全 completed + 全执行项/Exit Criteria `[x]`（审计时 Closure Gates 留 `[ ]` 符合 rule 12，闭包补齐后经文本一致性复查）+ D1/D2 决策含选择/备选/理由/残余风险；(2) 代码存在性：`ErpFinGlDistributionValidator`（implements IErpFinFactsValidator，ORDER=100，matchRule 源键 AND 语义 + 生效窗口 + isActive，split scale 4 HALF_UP 末行补差 Σ==原金额 + amountSource/amountFunctional null 守卫，Σpercent 0.000001 容忍抛 ERR_GL_DISTRIBUTION_PERCENT_SUM，copyOf 覆盖 VoucherFact 全 18 字段）+ `ErpFinGlDistributionRule`/`GlDistributionTarget` DTO + `ErpFinErrors` 2 新码 + `ErpFinPostingProcessor.guardExchangeRate`（protected，bizObjectManager 按名解析 IErpMdCurrencyBiz + findFirst(eq id) 零 daoFor，isFunctional=true → rate=1 / false → 抛含 currencyCode / 币种不存在 LOG.warn 保守放行，persistVoucher 兜底未动）+ beans.xml Validator 注册（collect-beans 自动聚合）；(3) 反模式：三新生产文件零 @Inject private/零 System.currentTimeMillis/零 new Erp*/零 daoFor/零 REQUIRES_NEW + `git status` 零 ORM 模型变更 + checker actual == baseline 精确一致；(4) 测试与验证：`TestErpFinGlDistribution` 4 组 + `TestErpFinFxRateGuard` 4 组全绿（审计实跑 8/8）+ erp-fin-service 479/479 全绿（471 基线 + 8 新增零回归）；(5) 文档回填：arm-index P1-RC-001/002 → done (RC-R1.41/42) + roadmap RC-R1.41/42 → done ✅（消除陈旧 ask-first 字样）+ posting.md §FactsValidator 陈旧声明修正为实现注记 + §多币种处理守卫注记 + cost-center.md 实体未物化裁决注记 + §业务规则 2 实现注记 + logs 首条。审计 MINOR 3 项（空 autotest.yaml 标记 = 仓库既有跟踪范式；countVouchersOfBill 命名系回链推断（方法内注释已说明）；计划内既有行号引用相对守卫插入后有漂移但语义引用均正确）——均非阻塞，已知悉。

Follow-up:

- 无（范围内已确认缺陷零遗留；Deferred But Adjudicated 两项已登记：GlDistribution 规则实体化 successor yes + currencyId=null/币种不存在 FX 守卫保守放行 watch-only）
