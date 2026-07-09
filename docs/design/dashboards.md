# 经营看板设计(Dashboards)

> 定义各业务域经营看板的指标、数据源、布局、刷新机制与异常预警。
> 看板是各域 TOPM 下的「报表看板」分组,页面为占位 page.yaml(`/{moduleId}/pages/dashboard/main.page.yaml`),本设计为其提供实现规格。
> 看板菜单权威定义在各域 action-auth.xml 的 dashboard 分组;指标数据源引用各域实体/机制文档,不重复 schema。

## 设计原则

1. **指标可追溯到实体**:每个指标标注数据源实体与计算口径,AI 可据此实现查询。指标不硬编码数值,由实时聚合得出。
2. **分层展示**:顶部 KPI 卡片(关键数字)→ 中部趋势图(时间序列)→ 底部明细列表(Top N / 异常项)。
3. **时间维度**:支持期间筛选(今日/本周/本月/本季/本年/自定义),默认本期对比上期/同比。
4. **权限**:看板数据受行级权限约束(用户只看自己组织/部门/成本中心的数据,见 roles-and-permissions.md)。
5. **刷新**:默认进入时加载;支持手动刷新;关键看板(如库存预警)可配置定时刷新。

## 通用指标类型

| 类型 | 说明 | 示例 |
|---|---|---|
| KPI 卡片 | 单个关键数字 + 同比/环比 + 趋势小图 | 本月销售额 ¥1.2M ↑12% |
| 趋势图 | 时间序列折线/柱状 | 近 12 个月销售趋势 |
| 占比图 | 饼图/环形图 | 客户 TOP10 占比 |
| 明细列表 | 排行榜/异常清单 | 库存预警物料 TOP20 |
| 预警卡片 | 超阈值高亮 | 应收账龄 >90天 ¥50K |

---

## 1. 销售看板(Sales Dashboard)

**目的**:监控销售业绩、客户应收、订单转化。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 本期销售额 | ErpSalInvoice | Σ amountFunctional(invoiceDate 在期内, posted) | KPI + 同比/环比 |
| 本期订单量 | ErpSalOrder | count(docStatus=ACTIVE) | KPI |
| 订单→开票转化率 | Order/Invoice | count(invoice) / count(order) | KPI |
| 应收余额 | ErpFinArApItem | Σ 余额(partnerType=CUSTOMER) | KPI |
| 销售趋势 | ErpSalInvoice | 按月聚合 amountFunctional(近12月) | 趋势图 |
| 客户 TOP10 | ErpSalInvoice | 按 partner 聚合金额降序 | 占比图+列表 |
| 应收账龄 | ErpFinArApItem | 按 0-30/31-60/61-90/90+ 分组(见 ar-ap-reconciliation §账龄) | 预警卡片 |
| **预警**:应收超期 | ErpFinArApItem | 账龄>90天 且 余额>阈值 | 预警卡片(红色) |

**涉及机制**:../finance/ar-ap-reconciliation.md(账龄)、state-machine.md

---

## 2. 采购看板(Purchase Dashboard)

**目的**:监控采购支出、供应商应付、到货及时率。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 本期采购额 | ErpPurInvoice | Σ amountFunctional | KPI + 同比 |
| 本期订单量 | ErpPurOrder | count(ACTIVE) | KPI |
| 应付余额 | ErpFinArApItem | Σ 余额(partnerType=VENDOR) | KPI |
| 到货及时率 | ErpPurReceive/Order | 按期到货数 / 订单数(receiveDate ≤ orderLine.deliveryDate) | KPI |
| 采购趋势 | ErpPurInvoice | 按月聚合(近12月) | 趋势图 |
| 供应商 TOP10 | ErpPurInvoice | 按 partner 聚合金额降序 | 占比图+列表 |
| 三单匹配差异 | three-way-match | 价格/数量差异待处理数(见 three-way-match §差异处理) | 预警卡片 |
| **预警**:应付超期 | ErpFinArApItem | 账龄>90天 | 预警卡片 |

**涉及机制**:three-way-match.md(差异)、../finance/ar-ap-reconciliation.md(账龄)

---

## 3. 库存看板(Inventory Dashboard)

**目的**:监控库存水平、周转、预警。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 库存总值 | ErpInvStockBalance × ErpInvCostLayer | Σ(qty × unitCost) | KPI |
| 本期出入库量 | ErpInvStockMove | Σ in/out qty(DONE, 期内) | KPI |
| 库存周转率 | CostLayer/StockBalance | 出库成本 / 平均库存(见 costing-methods) | KPI |
| 缺料预警 | ErpInvStockBalance | availableQty < 安全库存阈值(物料级配置) | 预警卡片(列表) |
| 滞销库存 | ErpInvStockBalance | 最后出库日期 > N 天 且 qty > 0 | 预警列表 |
| 批次效期预警 | ErpInvBatch | expiryDate - today < N 天(见 trace-chain §批次) | 预警列表 |
| 库存趋势 | ErpInvStockBalance | 按月库存价值(近12月) | 趋势图 |
| 仓库分布 | ErpInvStockBalance | 按 warehouse 聚合价值 | 占比图 |

**涉及机制**:state-machine.md、trace-chain.md(批次效期)、../finance/costing-methods.md(周转率)

---

## 4. 财务看板(Finance Dashboard)

**目的**:监控企业财务健康状况、现金流、利润。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 本期收入 | ErpFinGlBalance | Σ 收入类科目余额(本期) | KPI |
| 本期支出 | ErpFinGlBalance | Σ 费用/成本类科目余额 | KPI |
| 本期净利润 | ErpFinGlBalance | 收入 - 支出 | KPI + 同比 |
| 银行存款余额 | ErpFinFundAccount | Σ currentBalance(accountType=BANK) | KPI |
| 应收/应付 | ErpFinArApItem | Σ AR / Σ AP | KPI 对比 |
| 收支趋势 | ErpFinGlBalance | 按月收入/支出(近12月) | 趋势图(双线) |
| 利润趋势 | ErpFinGlBalance | 按月净利润 | 趋势图 |
| 预算执行率 | budget.md | 实际/Budget(按维度,见 budget §对比) | 进度条 |
| **预警**:现金流 | ErpFinFundAccount | 银行余额 < 阈值 或 预计流出 > 余额 | 预警卡片 |

**涉及机制**:state-machine.md(GlBalance)、ar-ap-reconciliation.md、budget.md、bank-reconciliation.md

---

## 5. 资产看板(Assets Dashboard)

**目的**:监控固定资产规模、折旧、处置。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 资产原值合计 | ErpAstAsset | Σ originalValue(IN_SERVICE) | KPI |
| 累计折旧 | ErpAstAsset | Σ accumulatedDepreciation | KPI |
| 资产净值 | ErpAstAsset | 原值 - 累计折旧 | KPI |
| 本期折旧 | ErpAstDepreciationSchedule | Σ 月折旧额(本期 EXECUTED) | KPI |
| 在建工程余额 | ErpAstCip | Σ 余额 | KPI |
| 资产类别分布 | ErpAstAsset | 按 category 聚合净值 | 占比图 |
| 折旧趋势 | ErpAstDepreciationSchedule | 按月折旧额(近12月) | 趋势图 |
| **预警**:折旧未计提 | ErpAstAsset | IN_SERVICE 但本期无 EXECUTED 计划条目 | 预警列表 |

**涉及机制**:depreciation-and-posting.md、state-machine.md

---

## 6. 项目看板(Projects Dashboard)

**目的**:监控项目进度、成本、盈利。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 在手项目数 | ErpPrjProject | count(status=OPEN) | KPI |
| 项目总预算 | ErpPrjBudget | Σ budgetAmount(OPEN 项目) | KPI |
| 已发生成本 | ErpPrjCostCollection | Σ amount(OPEN 项目) | KPI |
| 预算执行率 | CostCollection/Budget | 已发生 / 预算(按项目) | 进度条 |
| 项目毛利率 | ErpPrjProjectPnl | Σ grossProfit / Σ revenue(见 profitability) | KPI |
| 项目状态分布 | ErpPrjProject | 按 status 聚合 | 占比图 |
| 成本超支项目 | CostCollection/Budget | 已发生 > 预算 的项目 | 预警列表 |
| **预警**:项目延期 | ErpPrjProject/Milestone | 计划完成日 < today 且 status != COMPLETED | 预警列表 |

**涉及机制**:cost-collection.md、profitability.md、state-machine.md

---

## 7. 制造看板(Manufacturing Dashboard)

**目的**:监控生产进度、工单执行、齐套。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 在制工单数 | ErpMfgWorkOrder | count(status in [IN_PROCESS, STOCK_RESERVED]) | KPI |
| 本期完工量 | ErpMfgWorkOrder | Σ completedQty(本期 COMPLETED) | KPI |
| 工单准时率 | ErpMfgWorkOrder | 按 plannedDate 完成的 / 总数 | KPI |
| 齐套待产 | ErpMfgWorkOrder | count(STOCK_PARTIAL, 缺料待产) | KPI |
| 工单状态分布 | ErpMfgWorkOrder | 按 status 聚合 | 占比图 |
| 产成品产出趋势 | ErpMfgWorkOrder | 按周/月完工量 | 趋势图 |
| **预警**:齐套不足 | ErpMfgMaterialReservation | 未齐套工单 + 缺件明细(见 material-reservation §齐套) | 预警列表 |
| **预警**:工单延期 | ErpMfgWorkOrder | plannedDate < today 且 未 COMPLETED | 预警列表 |

**涉及机制**:state-machine.md、material-reservation.md(齐套)、bom-and-routing.md

---

## 8. 维护看板(Maintenance Dashboard)

**目的**:监控设备状态、维护执行、OEE。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 设备总数 | ErpMntEquipment | count(status != DECOMMISSIONED) | KPI |
| 运行中设备 | ErpMntEquipment | count(RUNNING) | KPI |
| 待处理维护请求 | ErpMntRequest | count(OPEN) | KPI |
| 本期维护访问数 | ErpMntVisit | count(期内 COMPLETED) | KPI |
| 设备 OEE | equipment-integration §六 | 可用率×性能×质量(按设备/产线) | KPI |
| 设备状态分布 | ErpMntEquipment | 按 status 聚合 | 占比图 |
| **预警**:设备停机 | ErpMntEquipment | status=DOWN + ErpMntDowntimeEntry 未恢复 | 预警卡片 |
| **预警**:维护逾期 | ErpMntSchedule | 计划日期 < today 且 未生成 Visit(见 equipment-integration §五) | 预警列表 |

**涉及机制**:equipment-integration.md(OEE/停机/调度)、state-machine.md

---

## 9. 质量看板(Quality Dashboard)

**目的**:监控质量水平、不合格率、CAPA 执行。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 本期质检数 | ErpQaInspection | count(期内) | KPI |
| 合格率 | ErpQaInspection | ACCEPTED / 总数 | KPI |
| 不合格数 | ErpQaInspection | count(REJECTED) | KPI |
| 开放 NCR 数 | ErpQaNonConformance | count(status in [OPEN, IN_REVIEW]) | KPI |
| 合格率趋势 | ErpQaInspection | 按周/月合格率(近12期) | 趋势图 |
| 不合格原因 TOP | ErpQaNonConformance | 按 defectType 聚合降序 | 占比图+列表 |
| SPC 失控预警 | ErpQaSpcSample | isOutOfControl=true 的子组(见 spc.md) | 预警列表 |
| **预警**:CAPA 逾期 | ErpQaAction | 计划完成日 < today 且 未 RESOLVED | 预警列表 |

**涉及机制**:inspection-integration.md、state-machine.md(NCR)、spc.md(失控预警)

---

## 主数据看板(Master Data Dashboard)

**目的**:监控主数据完整性、引用情况(轻量看板)。

| 指标 | 数据源 | 计算口径 | 类型 |
|---|---|---|---|
| 物料总数 | ErpMdMaterial | count | KPI |
| 往来单位总数 | ErpMdPartner | count(按 customer/vendor 分) | KPI |
| 无 SKU 物料 | ErpMdMaterial | 无关联 MaterialSku 的物料(数据质量) | 预警列表 |
| 无价格物料 | ErpMdMaterialSku | 无任何价格档的 SKU | 预警列表 |
| 停用主数据数 | 各主数据 | count(status=INACTIVE) | KPI |

**说明**:主数据看板偏数据治理,指标少且静态,无趋势/预警复杂度。

---

## 实现约定

1. **页面实现**:各看板 page.yaml 用 AMIS 组合(crud/table + chart + card),数据通过 GraphQL 查询各域 BizModel 的聚合方法。看板 BizModel 方法命名 `getDashboardKpi`/`getDashboardTrend`。
2. **聚合查询**:趋势/占比类用 EQL 聚合(group by + sum/count);KPI 卡片用单值查询;预警列表用带条件的 crud。
3. **权限过滤**:所有查询带 orgId/部门/成本中心过滤(行级权限自动注入)。
4. **性能**:大表聚合(如 GlBalance 按12月)考虑物化或缓存;预警列表分页。
5. **配置化**:阈值(如缺料安全库存、账龄预警天数、现金流下限)放系统配置(NopSysVariable),非硬编码。
6. **AMIS 取数范式约定**(plan `2026-07-09-1728-1` 落地，修 P1 缺陷 `docs/bugs/2026-07-09-1249-dashboard-amis-var-mangling.md`):
   - 看板/报表 page.yaml 中 `dataType: raw` 手写 GraphQL 查询字符串里的裸 `$var`（GraphQL 变量语法）**必须**以 `${'$'}` 转义（如 `query(${'$'}periodId:Long){ ...(periodId:${'$'}periodId) }`）。原因:amis-core `dataMapping` 对含 `$` 的字符串值经 `tokenize`(模板模式单趟解析)会把裸 `$var` 当模板变量替换为空,损坏查询致 KPI 恒 0。`${'$'}` 是 YAML 双引号安全的字面 `$` 输出(amis-formula `\$` 转义变体,单趟不回扫)。`variables` 中的 `${expr}` 模板不变。
   - 不要改用平台 `@query:` URL 范式替代:`guessDefinition` 对整数推断 `Int`、浮点 `Float`,无法产出 BizModel 声明的 `Long`/`BigDecimal`,GraphQL 校验会拒绝(`Int` 用于 `Long` 位置)。`dataType: raw` + `${'$'}` 转义是覆盖全参数类型的纯前端方案。
   - **报表渲染容器范式**:渲染 button 用 `actionType: reload target: "reportService"` 触发 form 内部的 `type: service`(name: reportService, initFetch: false),service 的 api 含 adaptor 将 `renderHtml` 返回 HTML 拍平为 `data.reportHtml`,body 为 `type: html html: "${reportHtml}"`。service 必须在 form **内部**(共享表单字段作用域;同级 service 取不到表单字段值)。**禁止**镜像旧 balance-sheet 的 `onEvent: setVariable(event.data.result)+setValue(target)` 范式——该范式运行时损坏(`event.data.result` 恒空、`target` 字段被忽略)。

## 实现状态

> **4 核心业务域后端聚合 API 已落地**（plan `2026-07-06-0935-1`）：销售/采购/库存/财务看板的 `getDashboardKpi`/`getDashboardTrend`/预警查询经 `@BizQuery` 暴露于专用看板 BizModel（`ErpXxxDashboardBizModel`），可独立验证。阈值经 `NopSysVariable` 配置化（`erp-dash.*` 配置键，默认值在代码常量中）。
> **4 核心业务域前端 AMIS 页面已落地**（plan `2026-07-06-1247-2`）：财务/销售/库存看板替换占位 `main.page.yaml` + 采购域新建看板页面与 `pur-dashboard` 菜单组。每页经 `/api/GenericApi` GraphQL 消费 0935-1 已审计 `@BizQuery`，分层布局对齐 §实现约定（form 区间筛选 + service KPI 卡片 + chart 趋势/占比图 + crud 预警列表）。
> **其余 6 域后端聚合 API 已落地**（plan `2026-07-06-1606-1`）：资产/项目/制造/维护/质量/主数据看板的 `getDashboardKpi`/`getDashboardTrend`/预警查询经 `@BizQuery` 暴露于各域专用看板 BizModel（`ErpAstDashboardBizModel`/`ErpPrjDashboardBizModel`/`ErpMfgDashboardBizModel`/`ErpMntDashboardBizModel`/`ErpQaDashboardBizModel`/`ErpMdDashboardBizModel`），镜像 0935-1 范式（`IDaoProvider`/`IOrmTemplate` + `QueryBean` + `ormTemplate.runInSession` 聚合）。阈值经 `NopSysVariable` 配置化（`erp-dash.mnt-maintenance-overdue-days`/`erp-dash.qa-capa-overdue-days`，默认 0=直接 `< today`）。
> 
> **三处设计文档指标原 Non-Goal 已解除**（数据源实体已物化）：
> - §6 项目毛利率 → `ErpPrjProjectPnl` 已物化（plan 0305-1），看板后端接线 `ErpPrjDashboard__getProjectGrossMargin` 已落地（plan 1100-3）；
> - §7 齐套不足缺件明细 → `ErpMfgMaterialReservation` 未物化，本期以 `STOCK_PARTIAL` 状态计数替代 KPI（触发条件：物料预留实体落地时）；
> - §9 SPC 失控预警 → `ErpQaSpcSample` 已物化（plan 0305-2，含 `isOutOfControl`/`violatedRules` 样本列），看板后端接线 `ErpQaDashboard__getSpcOutOfControlWarning` 已落地（plan 1100-3）。
> 
> **§8 维护 OEE 精确计算为 Non-Goal**（设计文档引用 `equipment-integration §六` OEE 可用率×性能×质量；精确性能/质量分量需设备采集数据未落地，本期仅交付可得子集：设备计数/状态分布/停机预警；触发条件：设备 OEE 采集数据落地时）。
> 
> **§9 不合格原因 TOP** 字段 `defectType` 在 ORM 未物化，本期以 `ErpQaNonConformance.dispositionType`（处置决定：SCRAP/RETURN/CONCESSION/DOWNGRADE）为聚合维度（语义最接近且为规范枚举）。
> 
> **6 域看板前端 AMIS 页面已落地**（plan `2026-07-06-1606-2`）：资产/项目/制造/维护/质量/主数据看板替换 6 个占位 `main.page.yaml`。每页经 `/api/GenericApi` GraphQL 消费 1606-1 已审计 `@BizQuery`，分层布局对齐 §实现约定（form 区间筛选 + service KPI 卡片 + chart 趋势/占比图 + crud 预警列表；主数据看板无趋势图，仅 KPI + 预警，对齐 §说明「指标少且静态」）。6 域复用既有 `*-dashboard` action-auth 菜单组（仅替换占位页内容，不新建菜单组）。页面 `ErpXxxDashboard__*` 调用逐一映射到 1606-1 BizModel 真实 `@BizQuery` 方法（ast 4 / md 3 / prj 4 / mfg 4 / mnt 4 / qa 4）。质量 SPC 失控预警卡片与项目毛利率卡片后续由 plan 1100-3 补齐（数据源物化后）。
> 
> **6 域看板前端 AMIS 页面**为独立 successor（plan `2026-07-06-1606-2`，触发条件=本计划后端 API 落地后前端定制启动时）。
> 报表渲染能力（`nop-report` + `IReportEngine` + `.xpt.*` 模板）已就绪（plan `2026-07-06-0504-2` 报表渲染子系统落地 `ErpFinReportBizModel` + 五张财务种子报表），看板聚合方法经同一 ORM 实体取数。

## 参考机制文档

- 各域 state-machine.md(状态分布指标)
- finance/ar-ap-reconciliation.md(账龄)、budget.md(预算执行率)、costing-methods.md(周转率)
- inventory/trace-chain.md(批次效期)
- manufacturing/material-reservation.md(齐套)
- maintenance/equipment-integration.md(OEE)
- quality/spc.md(失控预警)
