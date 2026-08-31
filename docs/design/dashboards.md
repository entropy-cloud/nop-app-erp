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

## 看板覆盖范围声明

> 本文件明确列出全部 18 业务域的看板覆盖状态，避免「沉默遗漏」（读者误判某域不属于管理壳）。第二批扩展域（CRM/CS/HR/APS/Contract/DRP/Logistics/B2B）当前无独立看板章节，显式声明为「产品基线外」并给出理由与触发条件，而非沉默缺位。

| 域 | 看板状态 | 说明 |
|---|---|---|
| sales / purchase / inventory / finance / assets / projects / manufacturing / maintenance / quality / master-data | **已设计**（见下方 §1-§9 + 主数据看板） | 10 看板已在本文定义，指标可追溯到实体 |
| crm | **产品基线外** | 触发条件：CRM 深化部署（线索漏斗转化率/活动达成/营销 ROI 看板）。当前 CRM 域有完整数据模型与状态机，看板为后续范围 |
| cs | **已落地**（客服绩效看板，2026-08-26 活仓更正） | `ErpCsQualityDashboard`（SLA 达成率/超时数/平均解决与首响时长/团队 SLA 排名/客服 CSAT-NPS-CES 明细）已随 CS 域深化落地（BizModel + 页面 + 菜单 + 集成测试，`module-cs/erp-cs-service/.../ErpCsQualityDashboardBizModel.java`）——原「产品基线外」表述系扩展域看板落地前的陈旧快照。KPI 口径已登记入下方「KPI 度量目录」§CS |
| hr | **产品基线外** | 触发条件：HR 深化部署（人头/薪酬成本/考勤异常/招聘漏斗看板）。当前 HR 域有员工/薪酬/考勤数据，看板为后续范围 |
| aps | **产品基线外** | 触发条件：APS 深化部署（排产利用率/订单准时率/瓶颈工作中心看板）。APS 排产结果可复用 manufacturing 看板的工单准时率指标 |
| contract | **产品基线外** | 触发条件：合同深化部署（到期/续约/执行率/用量计费看板） |
| drp | **产品基线外** | 触发条件：DRP 深化部署（补货建议达成/库存预警/缺货率看板）。DRP 补货可复用库存看板的缺料预警指标 |
| logistics | **产品基线外** | 触发条件：物流深化部署（发运准时率/承运商绩效/运费成本看板） |
| b2b | **产品基线外** | 触发条件：B2B 深化部署（EDI 成功率/ASN 匹配率/异常处理看板） |
| notify | **产品基线外** | 触发条件：通知深化部署（通知送达率/已读率看板）。notify 为跨域子系统，无独立经营指标 |

> 任何第二批扩展域进入深化部署时，应在本文件补建对应看板章节（结构对齐 §1-§9：目的 + 指标表含数据源/计算口径/类型 + 涉及机制），并更新本范围声明表。

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
| **预警**:齐套不足 | ErpMfgWorkOrder | 未齐套工单计数（`STOCK_PARTIAL` 状态）；缺件明细需物料预留实体（**产品基线外**——`ErpMfgMaterialReservation` 未物化，触发条件：物料预留实体落地时） | 预警列表 |
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
| 设备 OEE | equipment-integration §六 | 可用率×性能×质量（RC-R1.78 已实现：`ErpMntDashboard__computeOee/computeOeeList` 按需计算 + `getDashboardOeeKpi` 卡片聚合——三分量经设备状态记录/工单报工/质检按窗口实时聚合，无数据设备不计入均值显示 "—"；按设备明细经 computeOeeList 数据面承载） | KPI |
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
| 不合格原因 TOP | ErpQaNonConformance | 按 `dispositionType` 聚合降序（`defectType` 未物化，以处置决定 SCRAP/RETURN/CONCESSION/DOWNGRADE 为最接近语义维度） | 占比图+列表 |
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

## KPI 度量目录（Metric Catalog）

> **定位**（E3.1，roadmap §5）：本章是全部看板 KPI **规范口径的单一真相**——名称 / 定义公式 / 数据来源（表+过滤）/ 单位 / 口径说明。用途：value-spec 数值断言对照审计、新增 KPI 口径书写范式、跨域重复口径显式对齐、未来接入平台语义层（nop-metadata Measure/Dimension）的映射输入（触发条件驱动，见 `dashboard-semantic-layer.md` §1）。
>
> **口径与实现对齐**：各条目按对应 `ErpXxxDashboardBizModel` 实现逐项登记（2026-08-26 活仓核验）；value-spec 数值断言（`tests/e2e/dashboards/*.value.spec.ts`）为数据驱动的口径覆盖域，抽样核对一致（如 finance `revenue=1130` = GlBalance CREDIT 科目 `periodCredit-periodDebit`；projects `grossMarginPct=0.4` = `ΣgrossProfit/Σrevenue` = 20000/50000）。
>
> **通用约定**：金额单位=本位币（币种由 amountFunctional/currentBalance 等本位币字段承载）；比率单位=0-1 小数（前端 ×100 显示 %）；count=计数；date 窗口回显字段（startDate/endDate/period）不计入指标。空值统一经 `DashboardUtil.nz()` 归零（OEE 卡片除外：无数据=null，显示 "—"）。预警阈值一律 config 化（`erp-dash.*`，默认 0=禁用），非硬编码。

### 销售域（ErpSalDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| salesAmount | 本期销售额 | Σ amountFunctional | `ErpSalInvoice`，`posted=true` + `businessDate ∈ [startDate,endDate]`（缺省本月1日~今天） | 本位币 | 过账布尔位口径（**非** docStatus） |
| orderCount | 本期订单量 | count(*) | `ErpSalOrder`，`docStatus='ACTIVE'` | count | **无日期过滤**（全量 ACTIVE） |
| invoiceCount | 发票数 | count(*) | 同 salesAmount | count | 与销售额同一发票集 |
| conversionRate | 订单→开票转化率 | invoiceCount / orderCount | 派生 | 比率 | 分母 0 → 0.0 |
| arBalance | 应收余额 | Σ openAmountFunctional | `ErpFinArApItem`（经 `IErpFinArApItemBiz.findOpenItems`），`direction='RECEIVABLE'` + `status ∈ ('OPEN','PARTIAL')` | 本位币 | 跨域只读；与财务域 arBalance 同源同式（见对齐表） |
| 趋势（salesAmount/month） | 销售趋势 | 按 businessDate 月分组 Σ amountFunctional | 同 salesAmount，近 N 月（默认 12） | 本位币 | — |
| 客户 TOP10 | 客户排行 | GROUP BY customerId Σ amountFunctional 降序 | 同 salesAmount（DB 级聚合） | 本位币 | — |
| 应收超期预警 | 预警卡片 | 账龄（`dueDate ?? businessDate` → today）> 天阈值 **且** 余额 > 金额阈值 | 同 arBalance | 天/本位币 | 双阈值同时命中；`erp-dash.sal-ar-overdue-days/-amount` 默认 0=禁用 |

### 采购域（ErpPurDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| purchaseAmount | 本期采购额 | Σ amountFunctional | `ErpPurInvoice`，`docStatus='ACTIVE'` + `businessDate ∈ [startDate,endDate]` | 本位币 | **docStatus 口径**（与销售发票 posted 布尔位不同，易混淆） |
| orderCount | 本期订单量 | count(*) | `ErpPurOrder`，`docStatus='ACTIVE'` | count | 无日期过滤 |
| apBalance | 应付余额 | Σ openAmountFunctional | `ErpFinArApItem`（findOpenItems），`direction='PAYABLE'` + `status ∈ ('OPEN','PARTIAL')` | 本位币 | 与财务域 apBalance 同源同式 |
| onTimeRate | 到货及时率 | count(receive.businessDate ≤ 关联 orderLine.deliveryDate) / count(有 orderId 的 receive) | `ErpPurReceive`(docStatus='ACTIVE') ⟕ `ErpPurOrder` | 比率 | 分母仅计 `orderId != null` 的收货单 |
| 趋势（purchaseAmount/month） | 采购趋势 | 按 businessDate 月分组 Σ amountFunctional | 同 purchaseAmount，近 N 月 | 本位币 | — |
| 供应商 TOP10 | 供应商排行 | 按 supplierId Σ amountFunctional 降序 | 同 purchaseAmount（内存聚合） | 本位币 | — |
| 三单匹配差异 | 预警卡片 | 发票行 unitPrice vs 关联订单行 unitPrice，`|diff|/orderPrice > tolerance` | `ErpPurInvoiceLine` → `receiveLineId → ErpPurReceiveLine.orderLineId → ErpPurOrderLine` | 比率 | `erp-pur.match-price-tolerance` 默认 0.05 |
| 应付超期预警 | 预警卡片 | 账龄（dueDate ?? businessDate）> 天阈值 | 同 apBalance | 天 | `erp-dash.pur-ap-overdue-days` 默认 0=禁用 |

### 库存域（ErpInvDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| totalValue | 库存总值 | Σ totalCost | `ErpInvStockBalance`（DB 级 GROUP BY warehouseId + SUM 后汇总），无过滤 | 本位币 | 全量余额时点值 |
| incomingQty | 本期入库量 | Σ StockMoveLine.quantity（moveType=INCOMING） | `ErpInvStockMove`(docStatus='DONE') + 行，`businessDate ∈ 期内` | 数量 | — |
| outgoingQty | 本期出库量 | Σ \|quantity\|（moveType=OUTGOING，绝对值） | 同上 | 数量 | — |
| turnoverRate | 库存周转率 | 出库成本 / 平均库存（scale 4） | 分子=期内 DONE+OUTGOING 移动行 Σ totalCost；分母≈当前 totalValue | 比率 | **近似口径**：平均库存以当前时点值代替期间均值 |
| 趋势（netValueChange/month） | 库存价值变动 | 按 businessDate 月分组 Σ totalCost | `ErpInvStockLedger`（仅日期过滤，正负净变动） | 本位币 | 流水派生（E3.2 快照同源） |
| 仓库分布 | 占比图 | GROUP BY warehouseId Σ totalCost 降序 | `ErpInvStockBalance` | 本位币 | — |
| 缺料预警 | 预警列表 | availableQuantity < Material.safetyStock | `ErpInvStockBalance` × `ErpMdMaterial`（safetyStock>0 才比对） | 数量 | 扫描上限 5000 |
| 滞销库存 | 预警列表 | totalQuantity>0 且最后出库日 < today−N 天 | `ErpInvStockBalance` × DONE+OUTGOING 移动按 materialId 聚合 | 天 | `erp-dash.inv-slow-moving-days` 默认 0=禁用 |
| 批次效期预警 | 预警列表 | expiryDate ∈ [today, today+N] | `ErpInvBatch` | 天 | `erp-dash.inv-batch-expiry-days` 默认 0=禁用 |

### 财务域（ErpFinDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| revenue | 本期收入 | Σ periodActivity(b)，subject.subjectClass='INCOME' | `ErpFinGlBalance` ⟕ `ErpMdSubject`，`periodId=目标期间` + `orgId=period.orgId` + `acctSchemaId=组织主账套` | 本位币 | periodActivity：DEBIT 科目=periodDebit−periodCredit，CREDIT=periodCredit−periodDebit；期间缺省=最近会计期间，无期间全 0 |
| expense | 本期支出 | Σ periodActivity(b)，subjectClass ∈ ('EXPENSE','COST') | 同上 | 本位币 | — |
| netProfit | 本期净利润 | revenue − expense | 派生 | 本位币 | — |
| bankBalance | 银行存款余额 | Σ currentBalance | `ErpFinFundAccount`，`accountType='BANK'` | 本位币 | — |
| arBalance / apBalance | 应收/应付余额 | Σ openAmountFunctional | `ErpFinArApItem`（RECEIVABLE/PAYABLE，OPEN+PARTIAL，同期间组织/账套 scope） | 本位币 | 与 sales/purchase 域同源（findOpenItems），但多组织/账套 scope 过滤 |
| 收支/利润趋势 | 趋势图 | 按会计期间 month 分组 revenue/expense/netProfit | 期间集合 × GlBalance（orgId/schemaId scope） | 本位币 | — |
| 现金流预警 | 预警卡片 | bankBalance < 阈值 | 同 bankBalance | 本位币 | `erp-dash.fin-cash-flow-threshold` 默认 0=禁用 |

### 资产域（ErpAstDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| originalValue | 资产原值合计 | Σ originalValue | `ErpAstAsset`，`status='IN_SERVICE'` | 本位币 | — |
| accumulatedDepreciation | 累计折旧 | Σ accumulatedDepreciation | 同上 | 本位币 | — |
| netBookValue | 资产净值 | originalValue − accumulatedDepreciation | 派生（总量差） | 本位币 | 逐行净值求和等价 |
| periodDepreciation | 本期折旧 | Σ actualAmount | `ErpAstDepreciationSchedule`，`status='EXECUTED'` + `period=目标期间`（缺省当前 yyyy-MM） | 本位币 | — |
| cipBalance | 在建工程余额 | Σ accumulatedCost | `ErpAstCip`，`isCompleted=false` | 本位币 | 未转固 |
| 类别分布 | 占比图 | 按 categoryId Σ 逐行净值降序 | `ErpAstAsset`(IN_SERVICE) ⟕ Category | 本位币 | — |
| 折旧趋势 | 趋势图 | 按 period 分组 Σ actualAmount | `ErpAstDepreciationSchedule`(EXECUTED)，近 N 月桶 | 本位币 | helper 无日期过滤，全量加载后分桶 |
| 折旧未计提预警 | 预警列表 | IN_SERVICE 且当前 period 无 EXECUTED 条目 | `ErpAstAsset` × Schedule | — | — |

### 项目域（ErpPrjDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| openProjectCount | 在手项目数 | count(*) | `ErpPrjProject`，`status='OPEN'` | count | — |
| totalBudget | 项目总预算 | Σ totalAmount | `ErpPrjBudget`，`projectId ∈ (OPEN 项目)` | 本位币 | 仅计 OPEN 项目 |
| incurredCost | 已发生成本 | Σ totalAmount | `ErpPrjCostCollection`，`projectId ∈ (OPEN 项目)` | 本位币 | 仅计 OPEN 项目 |
| executionRate | 预算执行率 | incurredCost / totalBudget（scale 4） | 派生 | 比率 | 分母 ≤0 → 0 |
| projectCount | 损益汇总项目数 | distinct projectId | `ErpPrjProjectPnl`（ProjectPnlCalculator 周期物化），可选 projectId 过滤 | count | — |
| totalRevenue / totalCost / totalGrossProfit | 收入/成本/毛利合计 | Σ 对应列 | 同上 | 本位币 | — |
| grossMarginPct | 整体毛利率 | Σ grossProfit / Σ revenueAmount（scale 4） | 派生（DECIMAL 直加） | 比率 | 分母 ≤0 → 0；**不用**行级 grossMarginPct 加权（避免字符串列歧义） |
| 状态分布 | 占比图 | GROUP BY status count 降序 | `ErpPrjProject` | count | — |
| 成本超支预警 | 预警列表 | Σ CostCollection > Σ Budget（cost>0 前提） | 逐项目比对 | 本位币 | 扫描上限 5000 |
| 项目延期预警 | 预警列表 | status ≠ 'COMPLETED' 且 endDate < today | `ErpPrjProject` | 天 | — |

### 制造域（ErpMfgDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| inProcessCount | 在制工单数 | count(*) | `ErpMfgWorkOrder`，`docStatus ∈ ('IN_PROCESS','STOCK_RESERVED')` | count | — |
| periodCompletedQty | 本期完工量 | Σ completedQuantity | `ErpMfgWorkOrder`，`docStatus='COMPLETED'` + `actualEndDate ∈ 期内` | 数量 | — |
| stockPartialCount | 齐套待产 | count(*) | `ErpMfgWorkOrder`，`docStatus='STOCK_PARTIAL'` | count | — |
| onTimeRate | 工单准时率 | count(COMPLETED 且 actualEndDate ≤ plannedEndDate) / count(COMPLETED) | `ErpMfgWorkOrder`(COMPLETED) | 比率 | **全量 COMPLETED 不限日期**；分母 0 → 0.0 |
| 状态分布 | 占比图 | GROUP BY docStatus count 降序 | `ErpMfgWorkOrder` | count | — |
| 完工趋势 | 趋势图 | 按 actualEndDate 月分组 Σ completedQuantity | `ErpMfgWorkOrder`(COMPLETED) | 数量 | — |
| 工单延期预警 | 预警列表 | docStatus ∉ ('COMPLETED','CLOSED','CANCELLED') 且 plannedEndDate < today | `ErpMfgWorkOrder` | 天 | — |
| CRP 负荷（getCrpLoadChartData） | 负荷图 | 按 loadDate Σ loadHours / Σ capacityHours / loadRate=load/cap | `CrpLoadCalculator`（WorkcenterCalendar 出勤 × WorkcenterCapacity.efficiencyFactor 派生链） | 小时/比率 | 窗口缺省近 `erp-dash.mfg-crp-default-days`（默认 7）天；cap≤0 且 load>0 → 9999 哨兵 |

### 维护域（ErpMntDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| equipmentTotal | 设备总数 | count(*) | `ErpMntEquipment`，`status ≠ 'DECOMMISSIONED'` | count | — |
| runningCount | 运行中设备 | count(*) | `ErpMntEquipment`，`status='RUNNING'` | count | — |
| openRequestCount | 待处理维护请求 | count(*) | `ErpMntRequest`，`status='OPEN'` | count | 不限日期 |
| periodVisitCount | 本期维护访问数 | count(*) | `ErpMntVisit`，`status='COMPLETED'` + `businessDate ∈ 期内` | count | — |
| OEE 卡片（getDashboardOeeKpi） | 设备 OEE | fleet 级三分量与 OEE 均值（Σ/可计算设备数，scale 4） | `OeeCalculator`（跨域只读 mfg/qa）：availability=runningHours/(calendarHours−downtimeHours)；performance=actualOutput/(capacityPerHour×runningHours)；quality=qualifiedQuantity/actualOutput；OEE=三分量乘积 | 比率 | 无数据设备不计入均值（显示 "—"）；任一分量 null → OEE=null（D4 裁决：无数据 ≠ 零效率） |
| 状态分布 | 占比图 | GROUP BY status count 降序 | `ErpMntEquipment` | count | — |
| 设备停机预警 | 预警卡片 | status='DOWN' 且存在 endTime=null 的 DowntimeEntry | `ErpMntEquipment` × `ErpMntDowntimeEntry` | — | — |
| 维护逾期预警 | 预警列表 | isActive=1 且 nextDueDate < today−N 天 且无 Visit 引用 | `ErpMntSchedule` × `ErpMntVisit` | 天 | `erp-dash.mnt-maintenance-overdue-days` 默认 0；Visit 扫描上限 5000 |

### 质量域（ErpQaDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| inspectionCount | 本期质检数 | count(*) | `ErpQaInspection`，`inspectionDate ∈ 期内` | count | **无状态字段过滤**（result 含 PENDING） |
| passRate | 合格率 | count(result='ACCEPTED') / count(*) | 同上 | 比率 | 分母 0 → 0.0 |
| rejectedCount | 不合格数 | count(result='REJECTED') | 同上 | count | — |
| openNcrCount | 开放 NCR 数 | count(*) | `ErpQaNonConformance`，`status ∈ ('OPEN','IN_REVIEW')` | count | 不限日期 |
| SPC 预警（getSpcOutOfControlWarning） | 预警卡片 | distinct chartId 计数 ×3 | `ErpQaSpcSample`(isOutOfControl=true)；`ErpQaSpcCapability`(capabilityLevel='INADEQUATE')；`ErpQaNonConformance`(sourceType='SPC' 且 OPEN/IN_REVIEW) | count | 后两项 config-gated（`erp-dash.qa-spc-include-inadequate/-ncr`，默认 true） |
| 合格率趋势 | 趋势图 | 按 inspectionDate 月分组 total/accepted/passRate | `ErpQaInspection` | 比率 | — |
| 不合格原因 TOP | 占比图 | GROUP BY dispositionType count 降序 | `ErpQaNonConformance` | count | defectType 未物化，以处置决定为代理维度 |
| CAPA 逾期预警 | 预警列表 | status ≠ 'COMPLETED' 且 dueDate < today−N 天 | `ErpQaAction` | 天 | `erp-dash.qa-capa-overdue-days` 默认 0 |
| SPC 控制图数据 | 控制图 | chartType + cl/ucl/lcl + 子组序列 | `ErpQaSpcChart` + `ErpQaSpcSample`（计数型 defectRate=defectCount/inspectedCount） | — | 控制限 SpcControlLimitCalculator 持久化；chartId 解析：入参 > config > 最新 |

### 主数据域（ErpMdDashboard）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| materialCount | 物料总数 | count(*) | `ErpMdMaterial`，无过滤 | count | — |
| customerCount | 客户数 | count(*) | `ErpMdPartner`，`partnerType='CUSTOMER'` | count | — |
| vendorCount | 供应商数 | count(*) | `ErpMdPartner`，`partnerType='SUPPLIER'` | count | — |
| inactiveMaterialCount | 停用物料数 | count(*) | `ErpMdMaterial`，`status='INACTIVE'` | count | — |
| inactivePartnerCount | 停用往来单位数 | count(*) | `ErpMdPartner`，`status='INACTIVE'` | count | — |
| 无 SKU 物料 | 预警列表 | 无关联 MaterialSku 行 | `ErpMdMaterial` × `ErpMdMaterialSku` | — | 数据质量 |
| 无价格 SKU | 预警列表 | 四价格档全部 ≤0 | `ErpMdMaterialSku` | — | purchasePrice/salePrice/wholesalePrice/retailPrice |

### CS 域（ErpCsQualityDashboard，客服绩效看板）

| 指标 key | 名称 | 定义公式 | 数据来源（表+过滤） | 单位 | 口径说明 |
|---|---|---|---|---|---|
| totalTickets | 已关闭工单数 | count(*) | `ErpCsTicket`，`status='CLOSED'` + `createTime ∈ [start 00:00, end+1d 00:00)` | count | **时间字段 createTime**（非 businessDate）；两参均空=不过滤 |
| slaCompletedCount / slaBreachedCount | SLA 达标/超时数 | count(isSlaCompleted=TRUE) / total − 达标 | 同上 | count | — |
| slaCompletionRate | SLA 达成率 | slaCompleted / total（scale 4） | 派生 | 比率 | total=0 → **null**（非 0） |
| avgResolutionHours | 平均解决时长 | Σ(duration 分钟×60000)/有 duration 工单数/3600000 | 同上（duration>0 才计入） | 小时 | 无样本 → null |
| avgFirstResponseHours | 平均首次响应时长 | Σ(startTime−createTime)/正差样本数/3600000 | 同上（负差丢弃） | 小时 | 无样本 → null |
| 团队 SLA 排名 | 排名表 | 按 team 聚合 total/达标/达成率/平均解决时长，达标数降序 | Ticket ⟕ `slaPolicyId → ErpCsSlaPolicy.teamId → ErpCsTeam` | — | Ticket 无 teamId 列，经策略间接关联；null → "(未分派)" |
| 客服满意度明细 | 明细表 | 按 assignedToId 聚合 ticketCount/surveyCount/avgCsat/avgNps/avgCes（scale 2） | `ErpCsTicket` × `ErpCsSurvey`（csat/nps/ces null→0） | 分 | 无样本 → null |

### 跨域重复口径对齐登记

| 口径 | 各域出现 | 对齐结论 |
|---|---|---|
| 应收余额（arBalance） | sales KPI / finance KPI | **同源同式**：均 Σ openAmountFunctional（RECEIVABLE，OPEN+PARTIAL），sales 经 `IErpFinArApItemBiz.findOpenItems`（无 scope 过滤），finance 同方法 + 期间组织/账套 scope 过滤。多账套时 finance 值 ≤ sales 值（scope 收敛），单账套基线相等 |
| 应付余额（apBalance） | purchase KPI / finance KPI | 同上（PAYABLE 方向） |
| 账龄基准日 | sales 应收超期 / purchase 应付超期 / finance aging 报表 | 统一 `dueDate ?? businessDate`（finance aging 的 AR/AP 基准可 config 切换 `erp-fin.ar/ap-aging-base`，看板预警不切换） |
| 到货及时率 vs 工单准时率 | purchase onTimeRate / manufacturing onTimeRate | 语义同型（实际日期 ≤ 计划日期比率），数据源与字段不同（receive vs workOrder），各自登记 |
| 折旧口径 | assets KPI / finance 凭证 | assets 看板读 Schedule(EXECUTED).actualAmount；财务面经折旧过账凭证（posting-log），金额同源（折旧计提执行即过账） |

### 状态字段口径横向对照（易混淆点速查）

| 域 | 实体 | 字段 | 值 | 备注 |
|---|---|---|---|---|
| sales | ErpSalInvoice | `posted` | `Boolean.TRUE` | 布尔过账位，**不是** docStatus |
| sales/purchase | ErpSalOrder / ErpPurInvoice/Order/Receive | `docStatus` | `'ACTIVE'` | 采购域发票用 docStatus（与销售 posted 不同） |
| inventory | ErpInvStockMove | `docStatus` + `moveType` | `'DONE'` + INCOMING/OUTGOING | — |
| finance | ErpFinGlBalance | `periodId`+`orgId`+`acctSchemaId` | 期间定位 | 科目分类经 `ErpMdSubject.subjectClass/direction` |
| finance | ErpFinArApItem | `direction`+`status` | RECEIVABLE/PAYABLE；OPEN/PARTIAL | SETTLED/CANCELLED/WRITTEN_OFF 排除 |
| assets | ErpAstAsset / Schedule / Cip | `status`/`status`/`isCompleted` | IN_SERVICE / EXECUTED / false | — |
| projects | ErpPrjProject | `status` | OPEN | 预算/成本仅计 OPEN 项目 |
| manufacturing | ErpMfgWorkOrder | `docStatus` | IN_PROCESS+STOCK_RESERVED / COMPLETED / STOCK_PARTIAL | — |
| maintenance | Equipment/Request/Visit | `status` | ≠DECOMMISSIONED / RUNNING / OPEN / COMPLETED | — |
| quality | ErpQaInspection | （无状态过滤） | 仅 inspectionDate 范围 | result ∈ PENDING/ACCEPTED/CONDITIONAL/REJECTED |
| quality | ErpQaNonConformance | `status` | OPEN/IN_REVIEW | — |
| master-data | Material/Partner | `status` | INACTIVE（停用计数） | partnerType 值 CUSTOMER/SUPPLIER |
| cs | ErpCsTicket | `status` | CLOSED | 时间过滤用 createTime |

---

## 实现约定

1. **页面实现**:各看板采用 **flux 三段式 page.yaml**（`data-source` + `card` + `chart` + `crud`，见 `docs/design/page-structure-patterns.md` §3.0 范式），数据通过 GraphQL 查询各域 BizModel 的聚合方法。看板 BizModel 方法命名 `getDashboardKpi`/`getDashboardTrend`/`getDashboardAlerts`。> 历史注记：早先为 AMIS 组合（crud/table + chart + card），flux 全量迁移后已统一为 flux 三段式。
2. **聚合查询**:趋势/占比类用 EQL 聚合(group by + sum/count);KPI 卡片用单值查询;预警列表用带条件的 crud。
3. **权限过滤**:所有查询带 orgId/部门/成本中心过滤(行级权限自动注入)。
4. **性能**:大表聚合(如 GlBalance 按12月)考虑物化或缓存;预警列表分页。
5. **配置化**:阈值(如缺料安全库存、账龄预警天数、现金流下限)放系统配置(NopSysVariable),非硬编码。
6. **取数范式**:看板/报表 flux page.yaml 中 GraphQL 查询的 `$var` 转义、`data-source` 发布消费范式与报表渲染容器范式详见 [`docs/architecture/view-and-page-strategy.md §看板/报表 AMIS 取数范式约定`](../architecture/view-and-page-strategy.md)（flux 模式下 `data-source` 替代 AMIS service+adaptor，取数语义不变）。

## 视觉扩面注记

> 本节为指针性注记，不复制规范正文（单一真相源在 runbook）。

- **像素断言扩面（M2.4）遵循既有方法论**：10 域看板像素断言扩面（`comprehensive-test-data-and-visual-coverage` roadmap M2.4，扩展 `tests/e2e/visual/dashboards.snapshot.spec.ts` 范式至全 26 看板 spec）**必须遵循** `docs/testing/e2e-runbook.md`「视觉方法论（M0.3 固化）」段——AI 截屏仅诊断不裁决、mask 动态区域标准、跨次重跑稳定性阈值、双面重录协议、`assertDashboardPixelSnapshot` 像素 helper 子集扩展规则（不改既有 `assertSnapshot` / DOM 层 `assertDashboardRendered`）；像素层范式基线见同文件「像素级截图视觉回归层」段。
- **M0.1 回调义务登记**：mask 区域与扩面边界的最终值待 M0.1 裁决（plan `2026-09-01-0301-1`）落地后回调修订；**回调触发条件 = M0.1 完成**（M0.1 已于 2026-09-01 完成，裁决落 runbook「视觉断言扩面边界」段；若该裁决后续修订，原位回调修订 runbook 视觉方法论段与本注记）。回调义务与触发条件同步登记于 runbook 视觉方法论段「M0.1 回调义务登记」小节。

## 参考机制文档

- 各域 state-machine.md(状态分布指标)
- finance/ar-ap-reconciliation.md(账龄)、budget.md(预算执行率)、costing-methods.md(周转率)
- inventory/trace-chain.md(批次效期)
- manufacturing/material-reservation.md(齐套)
- maintenance/equipment-integration.md(OEE)
- quality/spc.md(失控预警)
