# 2026-07-05-1838-2-manufacturing-production-variance 生产成本差异分析引擎（工单完工触发）

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/design/manufacturing/variance-analysis.md`（权威设计，§使用流程 :81 显式标注"工单完工→差异计算→差异入账"为**后继触发面**）+ `docs/plans/2026-07-05-0427-2-standard-costing-strategy.md` Deferred「生产差异」（注：其触发条件"工单完工差异分析需求落地时"为同义反复；本切片选择依据见 Task Route Decision）
> Related: `docs/plans/2026-07-05-0427-2-standard-costing-strategy.md`（STANDARD 计价 + PPV 已完成，生产差异是其显式 Deferred，提供技术前置 + PPV 过账范式）；`docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`（BOM/工艺 + 成本卷算已完成，标准用量/工时来源）；`docs/plans/2026-07-02-1538-1-inventory-costing-engine.md`（STANDARD 策略 + StandardCostResolver）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **`ErpMfgCostVariance` 实体已落地**（`module-manufacturing/model/app-erp-manufacturing.orm.xml:1049-1085`）：25 列齐全（workOrderId/lineNo/varianceType/costElement/materialId/operationId/standardAmount/actualAmount/varianceAmount/variancePercent/standardQty/actualQty/standardPrice/actualPrice/workcenterId/businessDate/posted/remark + 标准审计列）+ 4 to-one（workOrder/material/workcenter/operation）。**注**：`variance-analysis.md:81` 自述"ErpMfgCostVariance 模型尚未落地"——该陈述已过时，实体在本计划执行前已存在。
- **`varianceType`/`costElement` 列无字典绑定**（`:1056-1057`，plain VARCHAR(50) 无 `dict`/`stdDataType` 字典）。需新增 `erp-mfg/variance-type`（5 码值：MATERIAL_USAGE/LABOR_EFFICIENCY/LABOR_RATE/OVERHEAD/VOLUME）+ `erp-mfg/cost-element`（4 码值：MATERIAL/LABOR/OVERHEAD/SUBCONTRACT）字典——ORM 模型变更（ask-first 保护区域）。
- **`ErpMfgCostVarianceBizModel` 仅为生成骨架**（`module-manufacturing/erp-mfg-service/.../entity/ErpMfgCostVarianceBizModel.java`， CrudBizModel 无自定义方法）——无差异计算引擎、无完工触发、无过账。
- **工单完工触发点已就绪**（`ErpMfgWorkOrderProcessor.reportCompletion` :159-200）：累加 completedQuantity → recomputeTotals → generateCompletionMove → `willFinish` 时置 COMPLETED（:195）。差异计算 hook 点 = `willFinish` 分支。
- **工单实际成本四要素已累加**（`ErpMfgWorkOrder` ORM :491-495）：`materialCost`/`laborCost`/`overheadCost`/`subcontractCost`/`totalCost`/`plannedQuantity`/`completedQuantity`。`recomputeTotals` 在完工时重算 totalCost = 四要素之和，unitCost = total/completed。
- **标准成本来源已就绪**（plan `1538-2` + `0427-2`）：`CostRollupService.rollup(bomId)` → `ErpMfgCostRollupLine`（每物料 materialCost/laborCost/overheadCost/unitCost）；`StandardCostResolver` 读最近 FIRMED rollup 行 unitCost（plan `0427-2`）。BOM 行提供标准物料用量，工艺行提供标准工时/费率。
- **PPV 过账范式可复用**（plan `0427-2`）：`InvPostingDispatcher.dispatchPurchasePriceVariance` + `PURCHASE_PRICE_VARIANCE` 业务类型（ErpFinBusinessType 330）+ `PurchasePriceVarianceAcctDocProvider` 方向相关 Dr/Cr。生产差异过账承接此范式。

### 剩余差距

1. 无差异计算引擎（标准 vs 实际按成本要素逐项对比 → 写 ErpMfgCostVariance 行）。
2. 无完工触发（reportCompletion 的 `willFinish` 分支未调差异计算）。
3. varianceType/costElement 无字典（码值无约束）。
4. 无生产差异过账业务类型 + Provider（差异未入账 GL）。
5. 无差异查询/报表入口（BizModel 仅 CRUD 骨架）。
6. `variance-analysis.md:81` 过时陈述需收口。

## Goals

- **差异计算引擎**：工单完工（`willFinish`→COMPLETED）时，按成本要素（材料/人工/制造费用/委外）对比标准成本（BOM/工艺/cost rollup）vs 实际成本（WorkOrder 累加四要素），逐要素写 `ErpMfgCostVariance` 行（含差异类型分类、金额、数量、百分比）。config-gated 自动触发。
- **完工触发接线**：`ErpMfgWorkOrderProcessor.reportCompletion` 的 `willFinish` 分支调用差异计算（config-gated `erp-mfg.variance-auto-calc-enabled`，默认关）。
- **差异过账**：新增 `PRODUCTION_VARIANCE` 业务类型 + `ProductionVarianceAcctDocProvider`，按成本要素方向相关借贷分解（承接 PPV 范式），差异净额入账差异科目。
- **字典与查询**：variance-type/cost-element 字典落地；BizModel 增 `calculateVariances(workOrderId)` @BizMutation（手动重算入口）+ 按 workOrder/类型/要素/期间查询。
- **测试 + owner doc**：差异计算（各要素标准 vs 实际）+ 过账 + 完工触发 config-gated 行为测试；`variance-analysis.md` 收口过时陈述。

## Non-Goals

- **差异报表渲染**（按工作中心/产品/期间下钻的多维报表 UI）：属 nop-report 报表面；本期交付 ErpMfgCostVariance 数据 + 查询入口供报表消费。
- **差异预警通知通道**（差异超阈值触发通知）：依赖通知派发通道（`0306-1` Deferred「通知派发通道」触发条件）。
- **标准成本重估/成本调整单**：归 `1538-1` Deferred（标准成本周期重估）；本期只读消费 cost rollup 标准值。
- **在制品（WIP）差异重算 / 多次部分完工的滚动差异**：本期仅完工达量（COMPLETED）一次性计算；部分完工（未达 plannedQuantity）不触发差异计算。
- **联合产品/副产品成本分配 / 阶段性差异（按工序卡）**：本期工单级汇总；工序卡级差异归 Deferred。
- **差异冲回/重算凭证红冲链**：本期完工一次计算；反完工（如 reopen 工单）的差异冲回归 Deferred。
- **cron 定时批量差异计算**：本期完工实时触发 + 手动入口；批量定时归 `0306-1` Deferred 范式（需新 BizModel 方法类）。

## Task Route

- Type: `implementation-only change`（差异引擎 + 触发 + 过账 + 查询，ORM 实体已存在）+ `app-layer design change`（variance-type/cost-element 字典 ORM 新增 + PRODUCTION_VARIANCE 业务类型 + owner doc）。
- Owner Docs: `docs/design/manufacturing/variance-analysis.md`（权威，§使用流程 + §数据模型收口）、`docs/design/finance/costing-methods.md`（标准成本差异段）、`docs/design/manufacturing/state-machine.md`（完工触发点）、`docs/design/finance/posting.md`（差异过账承接 PPV 范式）。
- Skill Selection Basis: BizModel/Processor 改造（完工触发 hook + 差异引擎 service-helper 对齐 `CostRollupService` 范式）、跨实体（WorkOrder/BOM/Rollup/CostVariance）、AcctDocProvider 过账（承接 PPV）、ErrorCode、config-gated、ORM 字典新增（保护区域 ask-first）、JunitAutoTestCase——匹配 `nop-backend-dev`。前端不涉及。
- **Decision（切片选择诚实性）**：`0427-2` Deferred 触发条件原文为"工单完工差异分析需求落地时"——该表述是**同义反复**（"决定做时做"），非具体就绪门控，不能宣称因 STANDARD+PPV（采购侧价差，另一结果面）完成而"触发条件已满足"。本计划选择此切片的**真实依据**为：(a) `variance-analysis.md:81` 明确将"工单完工→差异计算→差异入账"标注为**后继触发面**（设计权威显式指定）；(b) 全部技术前置已就绪（STANDARD 计价 + cost rollup + BOM/工艺 + PPV 过账范式 + ErpMfgCostVariance 实体均已落地）；(c) `ErpMfgCostVariance` 实体已预落地（模型工作事实上已部分开始）。故本计划是基于设计文档指定 + 技术就绪选择此切片，而非依据同义反复触发条件。**残留风险**：无产品侧显式业务请求——以设计文档指定 + 技术就绪为决策依据，记录于此而非掩盖。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/.env/外部服务/数据迁移。
- 依赖 `ErpMfgCostVariance` 实体已落地（基线已就绪）。
- 依赖 `CostRollupService` + `StandardCostResolver` + BOM/工艺已落地（plan `1538-2`/`0427-2`）。
- 依赖 finance 过账管道 `IErpFinAcctDocProvider` SPI + `ErpFinBusinessType`（plan `0811-1`/`2030-1`）。
- **保护区域门控**：variance-type/cost-element 字典新增 + PRODUCTION_VARIANCE 业务类型触及 `module-manufacturing/model/app-erp-manufacturing.orm.xml` 与 `module-finance/model/app-erp-finance.orm.xml`（ask-first）。ORM 阶段实施前须：人工批准 + 本计划草案审查通过。
- 回滚策略：ORM 字典/业务类型新增为加性（code 增量重生成可逆）；差异引擎 Java 为应用层 git 可逆；config-gated 默认关闭完工自动触发。

## Execution Plan

### Phase 1 - 字典 + 业务类型落地（ORM·ask-first）

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`（variance-type/cost-element 字典）、`module-finance/model/app-erp-finance.orm.xml`（PRODUCTION_VARIANCE 业务类型）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: 人工批准（ask-first）+ 本计划草案审查通过

- [x] `Decision`：差异类型分类口径——`variance-analysis.md:35` 原码值为 `PRICE_USAGE / LABOR_EFFICIENCY / LABOR_RATE / OVERHEAD / VOLUME`。**选择**对材料段**收窄重命名**为 `MATERIAL_USAGE`（材料用量），因为材料**价格**差异已由 PPV（`0427-2`，`PURCHASE_PRICE_VARIANCE`）在采购入库捕获，本期材料差异仅算"用量差异"避免重复计入。其余 4 类（`LABOR_EFFICIENCY`/`LABOR_RATE`/`OVERHEAD`/`VOLUME`）码值与设计文档一致。**此重命名是对设计文档 `variance-analysis.md:35` 的有意偏离（范围收窄），Phase 5 必须同步更新设计文档码值并补"实现偏离补注"**，非宣称"对齐"。**替代**：保留 `PRICE_USAGE` 但语义为用量——码值与语义不符，误导，rejected。成本要素 4 类：MATERIAL/LABOR/OVERHEAD/SUBCONTRACT。**残留风险**：材料价格差异（外因）与用量差异（内因）在分析报表中分离——本期仅用量，价格归 PPV 查询。
  - Skill: none
- [x] `Add`：manufacturing ORM 增 `erp-mfg/variance-type` 字典（5 码值 + i18n）+ `erp-mfg/cost-element` 字典（4 码值 + i18n）；`ErpMfgCostVariance.varianceType`/`costElement` 列绑字典。
  - Skill: none
- [x] `Add`：finance ORM business-type dict 增 `PRODUCTION_VARIANCE`（新业务类型码，承接 PPV=330 之后的下一可用码，执行时核实序列）；`ErpFinBusinessType` 枚举 + 字典 YAML 双侧同步（`0427-2` 结束审计修复范式：真相源 ORM `<dict>` → codegen 重生成 YAML）。
  - Skill: none
- [x] Proof: `mvn clean install -DskipTests` 增量重生成；字典 well-formed + 业务类型双侧一致
  - Skill: none

Exit Criteria:

> Phase 1 交付字典与业务类型。解除 Phase 2/3 的阻塞（引擎写 ErpMfgCostVariance 需码值约束；过账需业务类型）。

- [x] variance-type(5)/cost-element(4) 字典在 ORM 落地且 codegen 重生成 dict.yaml；`ErpMfgCostVariance` 两列绑字典
- [x] PRODUCTION_VARIANCE 业务类型在 `ErpFinBusinessType` 枚举 + finance ORM dict + 重生成 YAML 三点一致

### Phase 2 - 差异计算引擎 + 完工触发

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../costing/ProductionVarianceCalculator.java`（新 service-helper）、`ErpMfgWorkOrderProcessor.reportCompletion`（hook）、`IErpMfgCostVarianceBiz`/`ErpMfgCostVarianceBizModel`（手动入口 + 查询）、`ErpMfgConstants`/`ErpMfgErrors`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：`ProductionVarianceCalculator.calculateVariances(workOrderId, ctx)`（service-helper，对齐 `CostRollupService` 范式）：读 WorkOrder（plannedQuantity/completedQuantity/四要素 actual）→ 读 BOM 标准用量 + 工艺标准工时/费率（或 cost rollup 标准单位成本 × completedQuantity 得标准成本）→ 逐成本要素对比 standard vs actual → 写 `ErpMfgCostVariance` 行（varianceType/costElement/standard/actual/variance/percent/qty/price/workcenter/businessDate）。材料段仅算用量差异（价格归 PPV），人工拆效率/费率，制造费用汇总，产量差异按 (actual-planned)×standardUnit。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpMfgWorkOrderProcessor.reportCompletion` 的 `willFinish` 分支（:194-197）增差异计算调用（config-gated `erp-mfg.variance-auto-calc-enabled` 默认关）；**失败隔离 try/catch 仅记 ERROR 日志**（差异计算失败不阻断完工；不接入异常工作台以保持本期范围紧凑，异常工作台接入归 Deferred）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpMfgCostVarianceBiz` + `ErpMfgCostVarianceBizModel` 增 `calculateVariances(workOrderId)` @BizMutation（手动重算/补算入口，幂等：先删该 workOrder 旧行再重算）+ `findByWorkOrder`/`aggregateByType` @BizQuery 查询。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpMfgErrors` 增 ErrorCode（`ERR_VARIANCE_NO_STANDARD_COST` 无标准成本/`ERR_VARIANCE_WORKORDER_NOT_COMPLETED` 非 COMPLETED 拒手动计算等）+ `ErpMfgConstants` 配置键/码值常量。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `ProductionVarianceCalculator` 非空实现（逐要素 standard vs actual 对比写行），无 `return null`/空体；完工 hook config-gated 可关
- [x] `calculateVariances` @BizMutation 经 GraphQL 可达且幂等；查询 @BizQuery 可达
- [x] 本地化编译通过（`mvn compile -pl module-manufacturing/erp-mfg-service -am`）

### Phase 3 - 差异过账 Provider

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../posting/ProductionVarianceAcctDocProvider.java`（新）、`ProductionVarianceDispatcher`（新，manufacturing 域侧独立 dispatcher）、`app-service.beans.xml` 注册
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1（业务类型）、Phase 2（差异行数据）

- [x] `Decision`：过账粒度——**选择**按成本要素汇总净差异入账（MATERIAL/LABOR/OVERHEAD/SUBCONTRACT 各一条分录方向相关借贷，承接 PPV `PurchasePriceVarianceAcctDocProvider` 范式），差异类型作为 ErpMfgCostVariance 分析维度不入账。**替代**：按 5 差异类型分别入账——科目映射组合爆炸（5 类型 × 借/贷方向），且 PPV 已按要素范式，rejected。**残留风险**：要素汇总丢失类型级科目精度——归 Deferred（按类型分科目需求落地时）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：过账 dispatcher 落位——**选择** manufacturing 域侧**独立** `ProductionVarianceDispatcher`（在 `ProductionVarianceCalculator` 计算完成后组装 PostingEvent 经 `IErpFinVoucherBiz.post` 提交，`posted` 标志回写 ErpMfgCostVariance）。**替代**：复用 inventory 域 `InvPostingDispatcher`——生产差异属 manufacturing 结果面，跨域写 inventory dispatcher 违反 DAG 内聚（inventory 不持有 manufacturing 差异语义），rejected。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ProductionVarianceAcctDocProvider`（实现 `IErpFinAcctDocProvider`，按成本要素 Dr/Cr 分解到对应差异科目），对齐 `PurchasePriceVarianceAcctDocProvider` 结构；`ProductionVarianceDispatcher` 在差异计算后组装 PostingEvent 提交过账，`posted` 标志回写 ErpMfgCostVariance。
  - Skill: `nop-backend-dev`
- [x] `Add`：`app-service.beans.xml` 注册 Provider/Dispatcher（对齐 PPV 注册范式）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `ProductionVarianceAcctDocProvider` 按四要素分支生成实际借/贷 fact，非空实现；`posted` 回写 ErpMfgCostVariance
- [x] Provider/Dispatcher 经 beans.xml 注册，运行时经差异计算入口可达

### Phase 4 - 行为测试

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/test/.../TestErpMfgProductionVariance.java`
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1-3

- [x] `Proof`：至少覆盖：(a) 材料用量差异（实际超耗 vs BOM 标准）；(b) 人工效率差异（实际工时 vs 标准）；(c) 制造费用差异汇总；(d) 产量差异（完工≠计划）；(e) 完工触发 config-gated（开→自动算 / 关→不算）；(f) 手动 calculateVariances 幂等（重算先删旧）；(g) 差异过账凭证生成 + posted 标志；(h) 无标准成本抛 `ERR_VARIANCE_NO_STANDARD_COST`。断言 ErpMfgCostVariance 行金额/类型 + 凭证。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 测试类全绿；`mvn test -pl module-manufacturing/erp-mfg-service -am` + `mvn test -pl module-finance/erp-fin-service -am`（零回归）

### Phase 5 - owner doc 收口

Status: completed
Targets: `docs/design/manufacturing/variance-analysis.md`、`docs/design/finance/costing-methods.md`、当日日志
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-4

- [x] `Add`：`variance-analysis.md:81` 过时陈述收口（"ErpMfgCostVariance 模型尚未落地"→"已落地，差异引擎/过账由 plan 2026-07-05-1838-2 交付"）；§使用流程"后继触发面"标注落地；`:35` 差异类型码值同步本计划范围收窄（`PRICE_USAGE`→`MATERIAL_USAGE`，补"实现偏离补注：材料价格差异归 PPV"）；配置点表回流 `erp-mfg.variance-auto-calc-enabled`。
  - Skill: none
- [x] `Add`：`costing-methods.md` STANDARD 段补"生产差异"落地标记（解除 0427-2 Deferred「生产差异」）。
  - Skill: none

Exit Criteria:

- [x] `variance-analysis.md`/`costing-methods.md` 与实现一致，无过时陈述

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（ses_0ce203dcfffefKG148qb11lwhG，general 独立子代理新会话）。0 BLOCKER + 2 MAJOR + 3 MINOR；基线核实全通过（`ErpMfgCostVariance` 实体/字段/完工 hook/PPV 范式/0427-2 Deferred 全部实时确认）：
  - **M1**：varianceType 码值（`MATERIAL_USAGE`）与设计文档 `variance-analysis.md:35`（`PRICE_USAGE`）偏离，但计划宣称"对齐"。**已修订**：Phase 1 Decision 改为显式记录"范围收窄重命名"（材料价格归 PPV），Phase 5 增 `:35` 码值同步 + 实现偏离补注。
  - **M2**：Deferred 触发条件"工单完工差异分析需求落地时"为同义反复；计划宣称因 STANDARD+PPV 完成"触发条件已满足"属过度声称。**已修订**：Source 行注明同义反复；Task Route 增切片选择诚实性 Decision（真实依据 = 设计文档 :81 显式指定后继触发面 + 技术前置就绪 + 实体预落地，非触发条件满足）。
  - **m1**：Closure Gates 提及 cron 但 Deferred 无条目。**已修订**：增「差异计算 cron 定时批量（含异常工作台接入）」Deferred 条目。
  - **m2**：Phase 2 失败隔离"`IErpFinPostingExceptionBiz` 范式可选"含 `可选`。**已修订**：明确为"仅记 ERROR 日志"，异常工作台接入归 Deferred。
  - **m3**：Phase 3 dispatcher "WorkOrder 管道或独立 dispatch"含 `或`。**已修订**：增 dispatcher 落位 Decision（manufacturing 域侧独立 `ProductionVarianceDispatcher`，不复用 inventory `InvPostingDispatcher`）。
- Independent draft review iteration 2: `accept`（ses_0ce194117ffeK7570GPWqyYwUz，general 独立子代理新会话）— M1/M2/m1/m2/m3 全部实时核实已修复；基线抽查全通过（ErpMfgCostVariance 实体/字段/完工 hook/PPV 范式/0427-2 Deferred）；保护区域 ask-first 门控记录；anti-slack 仅剩 Draft Review Record 引用与模板行（非范围项）；保护区域门控到位。共识达成，转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（差异引擎 + 完工触发 + 过账 + 字典 + 查询 + 测试）
- [x] 相关文档对齐（`variance-analysis.md` / `costing-methods.md` / 当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + `mvn test -pl module-manufacturing/erp-mfg-service -am` + `mvn test -pl module-finance/erp-fin-service -am`（零回归）
- [x] 无范围内项目降级为 deferred/follow-up（报表渲染/通知/标准成本重估/WIP/工序卡级/冲回/cron 定时均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 保护区域（model/*.orm.xml 字典 + 业务类型）实施前已获人工批准
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 差异报表渲染（工作中心/产品/期间多维下钻 UI）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 nop-report 报表面；本期交付 ErpMfgCostVariance 数据 + @BizQuery 查询入口供报表消费。
- Successor Required: yes（触发条件：nop-report 接线时）

### 差异预警通知通道（超阈值触发通知）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖通知派发通道（站内消息/邮件/webhook）落地。
- Successor Required: yes（触发条件：通知派发通道基础设施落地时，承接 `0306-1` Deferred）

### 按差异类型分科目入账精度

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期按成本要素汇总净差异入账（承接 PPV 范式）；5 类型分科目组合映射归后继。
- Successor Required: yes（触发条件：按类型分科目核算需求落地时）

### 在制品（WIP）差异 / 多次部分完工滚动差异 / 工序卡级差异

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期仅完工达量（COMPLETED）一次性工单级计算；部分完工/WIP/工序卡级需滚动差异模型，独立结果面。
- Successor Required: yes（触发条件：WIP 差异/工序卡级成本控制需求落地时）

### 差异冲回/重算凭证红冲链（反完工 reopen）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期完工一次计算；反完工的差异冲回需工单 reopen + 凭证红冲链，独立结果面。
- Successor Required: yes（触发条件：工单反完工/差异重算需求落地时）

### 标准成本重估/成本调整单

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 `1538-1` Deferred；本期只读消费 cost rollup 标准值。
- Successor Required: yes（触发条件：标准成本周期重估需求落地时）

### 差异计算 cron 定时批量（含异常工作台接入）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期完工实时触发 + 手动 `calculateVariances` 入口；批量定时计算需新独立 job bean（`0306-1` Deferred「需新 BizModel 方法的扫描类作业」范式）+ 异常工作台接入（失败隔离本期仅日志）。归后继。
- Successor Required: yes（触发条件：批量定时差异计算/异常工作台统一接入需求落地时）

## Closure

Status Note: 全部 5 个阶段退出标准已勾选，所有 Closure Gates 经独立子代理新会话审计通过。实时仓库逐项核实（非采信执行者自述）：差异引擎/完工触发/过账 Provider+Dispatcher/字典+业务类型/查询入口/行为测试/owner doc 收口/当日日志全部落地且非空壳（anti-hollow 通过）。范围内无项目降级为 deferred（Deferred But Adjudicated 全部为显式 Non-Goal 附触发条件）。计划可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，closure-audit flow，未重用执行者上下文）
- Evidence:
  - `ProductionVarianceCalculator.java` 362 行非空（`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/costing/`），逐成本要素 standard vs actual 对比写 `ErpMfgCostVariance` 行；`deleteByWorkOrder` 提供幂等删旧
  - `ProductionVarianceAcctDocProvider.java` 144 行 + `ProductionVarianceDispatcher.java` 183 行（`posting/`），非空实现，`posted` 标志回写
  - `ErpMfgWorkOrderProcessor.java:212` `willFinish && isVarianceAutoCalcEnabled()` config-gated 触发，失败隔离 `LOG.error`（不阻断完工，符合 Phase 2 决策）
  - `ErpMfgCostVarianceBizModel.java` `calculateVariances` @BizMutation（幂等：先 `deleteByWorkOrder` 再重算 + 调 `dispatchIfApplicable` 过账）+ `findByWorkOrder`/`aggregateByType` @BizQuery，经 GraphQL `ErpMfgCostVariance__calculateVariances` 可达
  - 字典 `erp-mfg/variance-type`（5 码值含 `MATERIAL_USAGE`）+ `erp-mfg/cost-element`（4 码值）在 `module-manufacturing/model/app-erp-manufacturing.orm.xml:145/153` 落地，`ErpMfgCostVariance.varianceType`/`costElement` 列绑字典（`:1072-1073`）
  - `PRODUCTION_VARIANCE` 业务类型在 `module-finance/model/app-erp-finance.orm.xml:96` 落地
  - 测试类 `TestErpMfgProductionVariance.java` 550 行，7 个测试方法覆盖材料用量/人工效率+费率/制造费用/产量/config-gated/幂等/过账凭证+posted/无标准成本抛错；7 个 `_cases/` 录制回放目录齐全
  - owner doc 收口：`variance-analysis.md:35` 码值同步为 `MATERIAL_USAGE` + 实现偏离补注；`:81` 过时陈述收口（标注由本 plan 交付）；`costing-methods.md:48/50` STANDARD 段生产差异 Non-Goal 收口 + 实现注记
  - 当日日志 `docs/logs/2026/07-05.md` 含本计划条目（grep 命中 `1838-2`/`生产差异`）
  - 文本一致性：Plan Status=completed / 全部 5 个 Phase Status=completed / 全部 Exit Criteria `[x]` / Closure Gates 全 `[x]` / 日志条目一致

Follow-up:

- 差异报表渲染（见上方 Deferred）
- 差异预警通知（见上方 Deferred）
- 按类型分科目入账（见上方 Deferred）
- WIP/工序卡级差异（见上方 Deferred）
