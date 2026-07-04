# DRP（分销需求计划）域用例

## UC-DRP-01 DRP 计划创建

| 项目 | 内容 |
|------|------|
| **概述** | 计划员创建 DRP 计划，设置运算期间和参数 |
| **触发条件** | 计划员手工创建 / 定时任务自动触发 |
| **前置条件** | 仓库补货参数已配置；物料主数据已维护；库存数据可用 |
| **基本流程** | 1. 创建 ErpDrpPlan，填写 planName、periodFrom、periodTo<br>2. 初始状态为 DRAFT<br>3. 可选关联特定的仓库/物料范围（若不指定则全量）<br>4. 保存 |
| **后置条件** | DRAFT 计划的 DrpPlan 记录已创建 |
| **异常** | 时间段重叠告警（可选，同一期间允许创建多版本方案） |
| **跨域协作** | 无 |

## UC-DRP-02 净需求计算（DRP 运行）

| 项目 | 内容 |
|------|------|
| **概述** | DRP 引擎运行净需求计算 安全库存+预测-当前库存+已分配-在单=净需求 |
| **触发条件** | 计划员在 DRAFT 计划上点击"运行计算" |
| **前置条件** | DRP 计划处于 DRAFT；仓库/物料补货参数已配置 |
| **基本流程** | 1. 读取范围内每个物料+仓库组合的 ErpDrpParameter<br>2. 从库存模块读取 currentStock、allocatedQty、onOrderQty<br>3. 从销售预测读取 forecastDemand（或使用默认值 0）<br>4. 计算 netRequirement = max(0, safetyStock + forecastDemand - currentStock + allocatedQty - onOrderQty)<br>5. 若 netRequirement > 0，向上取整到 orderMultiple 倍数 → suggestedQty<br>6. 根据 preferredSourceWarehouseId 或 preferredSupplierId 确定 replenishmentType<br>7. 生成 ErpDrpLine（status = SUGGESTED）<br>8. 更新 DrpPlan.totalReplenishmentQty、runAt、runBy<br>9. DrpPlan.status → COMPUTED |
| **后置条件** | DrpLine 已生成（SUGGESTED）；计划状态已更新 |
| **异常** | 物料参数缺失时跳过并告警；netRequirement ≤ 0 时不生成行（库存充足） |
| **跨域协作** | inventory（读取库存数据）；？sales/forecast（读取预测） |

## UC-DRP-03 补货建议审批

| 项目 | 内容 |
|------|------|
| **概述** | 计划主管审查 SUGGESTED 行，调整 approvedQty 后批准 |
| **触发条件** | DRP 计划状态 = COMPUTED |
| **前置条件** | — |
| **基本流程** | 1. 计划主管打开 COMPUTED 的 DRP 计划<br>2. 逐行或批量审查 SUGGESTED 的补货建议<br>3. 可调整 approvedQty（修改 suggestedQty 或直接设为 0 取消）<br>4. 确认后批准 → DrpLine.status = APPROVED<br>5. 所有行审批完成后 DrpPlan.status → APPROVED<br>6. 或在 COMPUTED 发现数据异常时回退到 DRAFT（重新计算） |
| **后置条件** | APPROVED 行等待生成补货单 |
| **异常** | 仅部分行批准时，未批准行保持 SUGGESTED 或手动置为 CANCELLED |
| **跨域协作** | 无（纯 DRP 域操作） |

## UC-DRP-04 自动生成补货单

| 项目 | 内容 |
|------|------|
| **概述** | DRP 计划批准后，系统自动为每条 APPROVED 行生成调拨单或采购单 |
| **触发条件** | DrpPlan.status = APPROVED（自动模式）或计划员手动触发"执行" |
| **前置条件** | APPROVED 行的 approvedQty > 0 |
| **基本流程** | 1. 系统遍历所有 APPROVED 的 DrpLine<br>2. 根据 replenishmentType 执行 <br>   - TRANSFER → 调用 inventory/transfer 创建 TransferOrder（源仓库→目标仓库，数量=approvedQty）<br>   - PURCHASE → 调用 purchase 创建 PurchaseOrder（首选供应商，物料，数量=approvedQty）<br>3. 补货单创建成功后回写 orderBillType/orderBillCode<br>4. DrpLine.status → ORDERED<br>5. 所有行完成后 DrpPlan.status → EXECUTED |
| **后置条件** | TransferOrder/PurchaseOrder 已创建；DrpPlan 执行完毕 |
| **异常** | 补货单创建失败时该行标记错误并重试；已生成补货单的行不可重复生成 |
| **跨域协作** | inventory/transfer（调拨单创建）；purchase（采购单创建） |

## UC-DRP-05 安全库存调整

| 项目 | 内容 |
|------|------|
| **概述** | 仓管员/计划员调整指定物料在指定仓库的安全库存和补货参数 |
| **触发条件** | 安全库存需要变更（季节性调整/新物料引入/历史消耗分析） |
| **前置条件** | ErpDrpParameter 记录已存在或需要新建 |
| **基本流程** | 1. 选择仓库和物料组合<br>2. 查找或创建 ErpDrpParameter<br>3. 编辑参数 safetyStock、replenishmentLeadTime、orderMultiple、replenishmentMethod<br>4. MIN_MAX 方法时设置 minStockLevel/maxStockLevel<br>5. PERIODIC 方法时设置 reviewPeriodDays<br>6. 保存 |
| **后置条件** | 下次 DRP 运行时将使用新参数 |
| **异常** | 参数值校验（safetyStock > maxStockLevel 时告警；orderMultiple 必须为整数或倍数） |
| **跨域协作** | 无（纯主数据配置） |

## UC-DRP-06 安全库存优化

| 项目 | 内容 |
|------|------|
| **概述** | 基于历史需求数据，按统计方法（标准差×服务水平）自动计算安全库存和再订货点建议值 |
| **触发条件** | 计划员手动触发或定时任务按周期自动计算 |
| **前置条件** | 物料历史出库/销售数据可用；ErpDrpParameter 已配置 |
| **基本流程** | 1. 选择物料和仓库，配置分析方法（STATISTICAL/SIMPLE/DDMRP）、服务水平、分析月数、提前期<br>2. 系统读取历史期间的需求数据，计算平均需求与标准差<br>3. 按公式 Z×σ×√L 计算建议安全库存和再订货点<br>4. 结果写入 ErpInvDrpSafetyStockCalc.calculatedSafetyStock/calculatedRop<br>5. 人工审查后可覆盖（overrideSafetyStock），确认后回写 ErpDrpParameter.safetyStock<br>6. 下次 DRP 运行时使用更新后的安全库存参数 |
| **后置条件** | 安全库存计算结果已产生（或已覆盖）；DRP 参数已更新 |
| **异常** | 历史数据不足时降级使用 SIMPLE 方法；计算结果差异超过阈值（默认 20%）时告警 |
| **跨域协作** | inventory（读取历史出库记录）；master-data（物料/仓库） |

## UC-DRP-07 越库（Cross-Dock）

| 项目 | 内容 |
|------|------|
| **概述** | 入站货物不经上架，直接在收货月台匹配出库需求后发往最终客户/门店，减少仓储操作环节 |
| **触发条件** | DRP 计划行标记越库 / ASN 入站识别 / 收货时手工标记 |
| **前置条件** | 越库功能已启用（erp-inv.drp-xdock-enabled=true）；仓库已配置越库暂存区 |
| **基本流程** | 1. DRP 计划行标记 crossDockFlag=true 或 ASN 入站识别越库标记<br>2. 创建 ErpInvDrpCrossDock（status=PENDING）<br>3. 货物入站到越库暂存区（status=STAGING）<br>4. 系统按策略（PRE_ALLOCATED/ON_RECEIPT/MANUAL）匹配目标出库订单<br>5. 匹配成功（status=MATCHED），生成出站移动单，装车（LOADED）<br>6. 出库确认后完成（COMPLETED）<br>7. 月台预约（ErpInvDrpDockAppointment）管理到货时间窗口 |
| **后置条件** | 越库货物已从入站直通出站，不产生库存余额 |
| **异常** | 超时未匹配（默认 24h）自动转为正常入库；物料需质检时在暂存区完成快检 |
| **跨域协作** | inventory（StockMove 入站/出站）；purchase（采购单收货标记越库）；sales（匹配销售订单出库） |

## UC-DRP-08 提前期跟踪与动态安全库存

| 项目 | 内容 |
|------|------|
| **概述** | 采购订单收货时记录实际提前期，统计分析和供应商可靠性评分，动态调整安全库存和补货参数 |
| **触发条件** | 采购订单收货确认（receiptDate 写入）/ 定时统计分析任务 |
| **前置条件** | 采购订单已创建并收货；供应商和物料主数据已维护 |
| **基本流程** | 1. 采购订单收货确认时系统自动计算 actualLeadTime = DATEDIFF(receiptDate, orderDate)<br>2. 创建 ErpInvDrpLeadTimeRecord，记录 supplierId、materialId、orderDate、receiptDate、actualLeadTime、expectedLeadTime、isOnTime<br>3. 统计分析任务按供应商+物料维度计算 μ、σ、准时率、变异系数<br>4. 与安全库存优化模块联动：当提前期变异高（σ/μ > 0.2）时使用联合变异公式计算安全库存<br>5. 分析结果可用于动态更新 ErpDrpParameter.replenishmentLeadTime 和 safetyStock<br>6. 供应商可靠性评分按准时率、稳定性、数量准确率、质量合格率综合计算<br>7. 评分等级影响安全库存调整幅度和补货策略 |
| **后置条件** | 提前期记录保存；统计分析结果可用于动态安全库存调整 |
| **异常** | 订单日期或收货日期缺失时不记录（跳过告警）；历史数据不足（<5 笔）时统计结果仅供参考 |
| **跨域协作** | purchase（采购订单收货事件）；master-data（供应商/物料）；safety-stock-optimization（动态安全库存集成） |
