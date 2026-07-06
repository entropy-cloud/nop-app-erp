# 2026-07-06-0935-1-core-business-dashboards-backend 核心业务看板后端聚合 API（财务/销售/采购/库存）

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: `docs/design/dashboards.md` §1-4 + §实现约定（权威设计，「实现状态」标注看板聚合方法 + 前端接线归独立后续计划）。本计划为**新增后端前置**，解除 `docs/plans/2026-07-06-0504-2-report-rendering-subsystem.md` Deferred「经营看板（dashboards.md 各域 KPI/趋势/预警）前端实现」（Classification: `out-of-scope improvement`；Successor Required: `yes`；触发条件=看板前端定制启动时）的**取数阻塞**——本计划**不继承**该 Deferred 项（其为前端面），仅交付后端聚合 API 使该前端 successor 届时可启动；前端 AMIS 页面仍为独立 successor
> Related: `2026-07-06-0504-2-report-rendering-subsystem.md`（报表渲染引擎，已就绪，看板经同一 ORM 实体取数）
> Audit: required

## Current Baseline

- **看板设计完整但后端未实现**：`docs/design/dashboards.md` 定义 10 张看板（销售/采购/库存/财务/资产/项目/制造/维护/质量/主数据），含每域 KPI/趋势/预警指标口径与「实现约定」（§217-221：`getDashboardKpi`/`getDashboardTrend` 命名、EQL 聚合、行级权限过滤、阈值入 `NopSysVariable`）。
- **9 张看板页面均为占位**：`/{moduleId}/pages/dashboard/main.page.yaml` 在 9 域 web 树中存在但为占位骨架（purchase 域占位页缺失，归本计划范围外的前端 successor）。
- **0 个看板 BizModel 方法**：全仓 `getDashboardKpi`/`getDashboardTrend` 无任何实现（grep 零命中）。看板数据 API 完全缺失。
- **数据源实体全部就绪**：财务 `ErpFinGlBalance`/`ErpFinArApItem`/`ErpFinFundAccount`（经 0300-3/0540-1/1000-1 等落地）、销售 `ErpSalInvoice`/`ErpSalOrder`、采购 `ErpPurInvoice`/`ErpPurOrder`/`ErpPurReceive`、库存 `ErpInvStockBalance`/`ErpInvCostLayer`/`ErpInvStockMove`/`ErpInvBatch` 均已存在并经多计划验证。
- **聚合范式可复用**：`ErpFinReportBizModel`（erp-fin-service）已建立「注入 `IDaoProvider`/`IOrmTemplate` → `QueryBean` 过滤 → `ormTemplate.runInSession` 内聚合 → 返回 `List<Map>`」的聚合数据集范式（见该类 `buildArApAgingDataset`/`buildBalanceSheetDataset`），本计划看板方法沿用同范式。
- **阈值配置基础设施就绪**：平台 `NopSysVariable` 已用于既有 config-gated 开关（如 `erp-fin.auxiliary-recon-gate-enabled`、`erp-mfg.crp-overload-threshold`），看板预警阈值沿用同机制。
- **剩余差距**：看板后端聚合 API（本计划）+ 看板 AMIS 前端页面（独立 successor，0504-2 Deferred「经营看板前端实现」）+ 其余 6 域看板（资产/项目/制造/维护/质量/主数据，独立 successor）。

## Goals

- 为**财务/销售/采购/库存** 4 个核心业务域交付看板后端聚合 API：每域 `getDashboardKpi`（KPI 卡片单值/对比）+ `getDashboardTrend`（时间序列）+ 预警列表查询，口径严格对齐 `dashboards.md` §1-4 指标表。
- 聚合方法以 `@BizQuery` 暴露于 GraphQL，可独立验证（不依赖前端），并解除下游「看板 AMIS 前端」successor 的阻塞。
- 预警阈值经 `NopSysVariable` 配置化，非硬编码。

## Non-Goals

- **看板 AMIS 前端页面**（crud/table + chart + card 组合、页面布局、图表渲染）——独立 successor（0504-2 Deferred「经营看板前端实现」，触发条件=本计划后端 API 落地）。
- **其余 6 域看板**（资产/项目/制造/维护/质量/主数据）——独立 successor，本计划仅覆盖 4 个核心业务域。
- **物化视图/缓存**（dashboards.md §实现约定 §4「大表聚合考虑物化或缓存」）——optimization candidate，归后续性能计划。
- **角色/组织行级权限精确过滤**——当前依赖平台既有 orgId 过滤机制；精确角色路由依赖「ERP 角色定义基础设施」（0315-1/0504-1 多计划 Deferred，未触发）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/dashboards.md`（§1 销售 / §2 采购 / §3 库存 / §4 财务 / §实现约定 / §参考机制文档）；数据源口径引用 `docs/design/finance/ar-ap-reconciliation.md`（账龄）、`docs/design/finance/costing-methods.md`（周转率）、`docs/design/purchase/three-way-match.md`（三单匹配差异）
- Skill Selection Basis: 任务为新增 `@BizQuery` 聚合方法 + 跨实体只读聚合（`IDaoProvider`/`IOrmTemplate`），匹配 `nop-backend-dev`（实体服务自定义动作、跨实体调用、ErrorCode、产品化自检）。不涉及 view.xml/AMIS（前端归 successor），故不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。预警阈值新增 `NopSysVariable` 种子项（见各域 Phase 的 config 项），无外部服务/端口/密钥依赖。

## Execution Plan

### Phase 1 - 看板基础设施 Decision + 财务看板（prove pattern）

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java`；config 种子（`NopSysVariable` 现金流预警阈值）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

- [x] **Decision: 看板方法落位**——每域新建**专用看板 BizModel**（`ErpXxxDashboardBizModel`，`@BizModel("ErpXxxDashboard")`），而非塞入既有订单/发票 BizModel。
  - 理由：镜像 `ErpFinReportBizModel` 隔离范式，避免污染生成的主实体 BizModel；聚合查询集中于单一可发现入口；与 dashboards.md §实现约定 §1「各域 BizModel 的聚合方法」语义一致（专用 BizModel 仍是「各域 BizModel」）。
  - 替代方案：塞入主实体 BizModel（拒绝：污染 + 职责混杂）；单一全局 DashboardBizModel（拒绝：跨域 R 依赖违反域边界）。
  - 残留风险：低——镜像已验证的 `ErpFinReportBizModel` 域隔离范式，无新模式引入。
  - Skill: `nop-backend-dev`
- [x] **Add: `ErpFinDashboardBizModel`**——`@BizModel("ErpFinDashboard")`，注入 `IDaoProvider`/`IOrmTemplate`。
  - `getDashboardKpi(@Optional periodId)` → 财务看板 KPI：本期收入 / 本期支出 / 本期净利润（`ErpFinGlBalance` 损益类科目本期发生净额，复用 `ErpFinReportBizModel.periodActivity` 口径）/ 银行存款余额（`ErpFinFundAccount` Σ currentBalance, accountType=BANK）/ 应收余额 / 应付余额（`ErpFinArApItem` 按 direction 聚合 OPEN+PARTIAL openAmountFunctional）。对齐 dashboards.md §4。
  - `getDashboardTrend(@Optional months)` → 近 N 月（默认 12）收入/支出/净利润月度序列（`ErpFinGlBalance` 按 period 维度聚合）。
  - `findCashFlowAlert()` → 现金流预警：银行余额 < 阈值 或 预计流出 > 余额。阈值读 `NopSysVariable` `erp-dash.fin-cash-flow-threshold`（默认 0=关闭）。
  - Skill: `nop-backend-dev`
- [x] **Add: `erp-dash.fin-cash-flow-threshold` `NopSysVariable` 种子**（默认 0）。
      - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpFinDashboard`**（`JunitAutoTestCase` 或 plain JUnit）——构造样本 `ErpFinGlBalance`/`ErpFinArApItem`/`ErpFinFundAccount`，经 GraphQL `ErpFinDashboard__getDashboardKpi`/`getDashboardTrend`/`findCashFlowAlert` 断言聚合值正确（收入/支出/净利润算术、AR/AP 余额方向、现金流预警触发/不触发两路径）。
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段交付的可观察结果 + 解除后续阶段阻塞的本地化检查。

- [x] `ErpFinDashboard__getDashboardKpi`/`getDashboardTrend`/`findCashFlowAlert` 经 GraphQL 返回正确聚合（成功/失败模式：空数据集返回零值不报错；有数据算术正确）
- [x] 看板方法落位 Decision 已记录，后续 3 域沿用同范式（解除 Phase 2-4 阻塞）

### Phase 2 - 销售看板

Status: completed
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（范式确立）

- [x] **Add: `ErpSalDashboardBizModel`**——`@BizModel("ErpSalDashboard")`，对齐 dashboards.md §1。
  - `getDashboardKpi(@Optional startDate,@Optional endDate)` → 本期销售额（`ErpSalInvoice` Σ amountFunctional, posted, invoiceDate 期内）/ 本期订单量（`ErpSalOrder` count ACTIVE）/ 订单→开票转化率（count invoice / count order）/ 应收余额（跨域读 `ErpFinArApItem` direction=CUSTOMER，经 `IErpFinArApItemBiz` 注入，R 跨域只读）。
  - `getDashboardTrend(@Optional months)` → 近 12 月销售趋势（`ErpSalInvoice` 按月聚合 amountFunctional）。
  - `findCustomerTopN(@Optional limit)` → 客户 TOP10（按 partner 聚合金额降序）。
  - `findArOverdueAlert()` → 应收超期预警（账龄 > 阈值 且 余额 > 阈值，复用 finance 账龄口径；阈值 `erp-dash.sal-ar-overdue-days`/`sal-ar-overdue-amount` `NopSysVariable`，默认关闭）。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpSalDashboard`**——样本销售订单/发票 + AR 辅助账，GraphQL 断言销售额/订单量/转化率/趋势/客户 TOP10/超期预警。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `ErpSalDashboard__*` 经 GraphQL 返回正确聚合（含跨域 AR 余额只读聚合正确）

### Phase 3 - 采购看板

Status: completed
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] **Add: `ErpPurDashboardBizModel`**——`@BizModel("ErpPurDashboard")`，对齐 dashboards.md §2。
  - `getDashboardKpi` → 本期采购额（`ErpPurInvoice` Σ amountFunctional）/ 本期订单量（`ErpPurOrder` count ACTIVE）/ 应付余额（跨域 `IErpFinArApItemBiz` direction=VENDOR）/ 到货及时率（`ErpPurReceive` 按期到货数 / `ErpPurOrder` 订单数，receiveDate ≤ orderLine.deliveryDate）。
  - `getDashboardTrend` → 近 12 月采购趋势（`ErpPurInvoice` 按月聚合）。
  - `findVendorTopN(@Optional limit)` → 供应商 TOP10。
  - `findThreeWayMatchDiffAlert()` → 三单匹配差异待处理数预警（口径对齐 `purchase/three-way-match.md` §差异处理，读既有差异状态）。
  - `findApOverdueAlert()` → 应付超期预警（账龄 > 阈值，阈值 `NopSysVariable`）。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpPurDashboard`**——样本采购订单/收货/发票 + AP 辅助账，GraphQL 断言采购额/订单量/到货及时率/趋势/供应商 TOP10/三单差异/超期预警。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `ErpPurDashboard__*` 经 GraphQL 返回正确聚合（含到货及时率日期比对 + 三单匹配差异口径对齐）

### Phase 4 - 库存看板

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] **Add: `ErpInvDashboardBizModel`**——`@BizModel("ErpInvDashboard")`，对齐 dashboards.md §3。
  - `getDashboardKpi` → 库存总值（`ErpInvStockBalance` × `ErpInvCostLayer` Σ qty×unitCost）/ 本期出入库量（`ErpInvStockMove` Σ in/out qty, DONE, 期内）/ 库存周转率（出库成本 / 平均库存，口径对齐 `finance/costing-methods.md`）。
  - `getDashboardTrend` → 近 12 月库存价值趋势（`ErpInvStockBalance` 按月）。
  - `findWarehouseDistribution()` → 仓库分布（按 warehouse 聚合价值）。
  - `findShortageAlert()` → 缺料预警（availableQty < 安全库存阈值，物料级配置）。
  - `findSlowMovingAlert()` → 滞销库存（最后出库日期 > N 天 且 qty > 0，N 阈值 `NopSysVariable`）。
  - `findBatchExpiryAlert()` → 批次效期预警（`ErpInvBatch` expiryDate - today < N 天，口径对齐 `inventory/trace-chain.md`）。
  - Skill: `nop-backend-dev`
- [x] **Proof: `TestErpInvDashboard`**——样本库存余额/成本层/移动单/批次，GraphQL 断言库存总值/周转率/出入库量/仓库分布/缺料/滞销/批次效期预警（触发/不触发两路径）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `ErpInvDashboard__*` 经 GraphQL 返回正确聚合（含三类预警触发/不触发路径 + 批次效期日期比对）

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0caea1e32ffeplPuBOarho0gdw`) — Source 行误引 0504-2 Deferred 触发条件（编造 `report-backed KPI 接线落地` 短语，0504-2 实为 `看板前端定制启动时`）且将关系误框为「承接」（0504-2 Deferred 为纯前端面，本计划交付后端 API，应为「新增后端前置」解除前端 successor 取数阻塞）。非阻塞：Phase 1 Decision 缺残留风险（规则 9）、NopSysVariable seed 项缺 Skill 行。Baseline 全部经实时仓库核实。
- Independent draft review iteration 2: accept (`ses_0cae17699ffec9pAY2vISFrX3Z`) — 修正后 Source 行准确引用 0504-2 触发条件 `看板前端定制启动时`，重框为「新增后端前置 / 不继承 / 解除取数阻塞」；Phase 1 Decision 补残留风险行、seed 项补 Skill 行。无新阻塞问题，规则 1-14/anti-slack/Exit 规则 7/命名全通过。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（4 域看板聚合 API 全部 GraphQL 可验证）
- [x] 相关文档对齐（`dashboards.md` §实现状态更新：4 核心域后端 API done；前端 + 其余 6 域标注为 successor）
- [x] 已运行验证：`mvn clean install -DskipTests`（154+ reactor 模块全绿）+ `mvn test -pl module-finance/erp-fin-service,module-sales/erp-sal-service,module-purchase/erp-pur-service,module-inventory/erp-inv-service -am`（4 域看板测试全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 看板 AMIS 前端页面（crud/table + chart + card 布局）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前端属 AMIS 定制面，独立验证路径（页面渲染）；本计划交付后端聚合 API 即解除前端取数阻塞。
- Successor Required: `yes`（触发条件：本计划后端 API 落地后，看板前端定制启动时）

### 其余 6 域看板（资产/项目/制造/维护/质量/主数据）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划聚焦 4 核心业务域（最高管理价值 + 数据最丰富）；其余 6 域同范式 successor。
- Successor Required: `yes`（触发条件：对应域看板需求落地时）

### 大表聚合物化视图/缓存

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划实时聚合满足 bootstrap；物化/缓存归性能优化计划。
- Successor Required: `yes`（触发条件：看板聚合查询时延压测不达标时）

## Closure

Status Note: 4 Phase 全部完成（财务/销售/采购/库存看板后端聚合 API 落地，4 域 27 测试全绿，154 模块 `mvn clean install -DskipTests` 全绿）。解除 0504-2 Deferred「经营看板前端实现」取数阻塞。独立子代理结束审计已通过（见下方证据）。

Closure Audit Evidence:

- 4 Phase Status 全部 `completed`，所有 `[ ]` 已 tick 为 `[x]`
- `mvn clean install -DskipTests` BUILD SUCCESS（154 reactor 模块）— 2026-07-06 执行
- `mvn test -pl module-finance/erp-fin-service,module-sales/erp-sal-service,module-purchase/erp-pur-service,module-inventory/erp-inv-service -Dtest=TestErpFinDashboard,TestErpSalDashboard,TestErpPurDashboard,TestErpInvDashboard -am -Dsurefire.failIfNoSpecifiedTests=false` → Tests run: 5+6+8+8=27, Failures: 0, Errors: 0 — 2026-07-06 执行
- 文件交付清单：4 BizModel + 1 IBiz 接口扩展 + 1 BizModel 实现 + 4 Constants + 4 bean 注册 + 4 测试类
- `docs/design/dashboards.md` §实现状态 + `docs/backlog/core-business-roadmap.md` M4 work item + `docs/logs/2026/07-06.md` 已更新
- Auditor / Agent: 独立子代理（新会话，closure-auditor，不重用执行者上下文）
- Evidence: 实时仓库逐项核实通过——4 BizModel（`ErpFinDashboardBizModel`/`ErpSalDashboardBizModel`/`ErpPurDashboardBizModel`/`ErpInvDashboardBizModel`）均为实质实现（无空函数体 / 无 `return null` 占位 / 无吞异常），`@BizQuery` 方法 `getDashboardKpi`/`getDashboardTrend`/各域预警查询经 `IDaoProvider`+`IOrmTemplate`+`QueryBean` 聚合并 `ormTemplate.runInSession` 包裹（镜像 `ErpFinReportBizModel` 范式）；`IErpFinArApItemBiz.findOpenItems(direction, ctx)` 跨域只读接口存在且 finance 域实体所有权保持（对齐 nop-backend-dev「跨实体优先 I*Biz」）；4 域 `app-service.beans.xml` 各 1 bean 注册（`ioc:type="@bean:id"`，服务型 BizObject 显式注册，运行时可解析可达，非 Anti-Hollow）；4 `IErpXxxConstants.CONFIG_DASH_*` 阈值键声明，预警阈值经 `AppConfig.var` 配置化非硬编码；`TestErpFinDashboard` 5 测试方法覆盖空数据集零值 + 算术 + 月度趋势 + 预警触发/不触发双路径（实读非空断言）；`dashboards.md` §实现状态已标 4 核心域后端 API 落地 + 前端/其余 6 域为 successor；`docs/logs/2026/07-06.md` 计划条目存在且与 Closure 一致。Deferred 3 项分类正确（前端面/其余 6 域/物化视图均 `out-of-scope improvement` + `Successor Required: yes` + 触发条件明确，无范围内缺陷隐藏）。五点一致性通过（Plan Status completed / 4 Phase Status completed / 4 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 证据在文件）。
