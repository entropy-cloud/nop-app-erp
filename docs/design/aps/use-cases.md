# APS（高级排产）域用例

## UC-APS-01 工序工单创建（从主工单）

| 项目 | 内容 |
|------|------|
| **概述** | 主工单（WorkOrder）下达后，按工艺路线自动生成工序工单 |
| **触发条件** | manufacturing 域的 WorkOrder 状态变更（如 RELEASED）或计划员手动触发 |
| **前置条件** | WorkOrder 已存在且绑定了工艺路线（含工序/工作中心/工时定额） |
| **基本流程** | 1. manufacturing 域发布 WorkOrder 下达事件<br>2. APS 域订阅，读取 WorkOrder 的工艺路线（工序列表）<br>3. 按 sequence 依次创建每条工序的 OperationOrder（status = DRAFT）<br>4. 继承 workOrderId、operationName、machineId、setupTime、runtimePerUnit、qty<br>5. 计算 totalDuration = setupTime + runtimePerUnit × qty |
| **后置条件** | 该 WorkOrder 下所有 OperationOrder 已创建，等待排产 |
| **异常** | 工艺路线缺失时跳过并告警；工作中心不存在时拒绝创建 |
| **跨域协作** | manufacturing（读取 WorkOrder 和工艺路线） |

## UC-APS-02 前向排产

| 项目 | 内容 |
|------|------|
| **概述** | 从 WorkOrder 的计划开始时间正向排产各工序时间 |
| **触发条件** | 计划员执行排产方案（schedule mode = FORWARD） |
| **前置条件** | OperationOrder 处于 DRAFT 状态；工作中心产能/约束已配置 |
| **基本流程** | 1. 选择待排产的 OperationOrder（可筛选 WorkOrder/工作中心/时间段）<br>2. 系统按优先级（priority）+ sequence 排序<br>3. 从 WorkOrder.plannedStartDate 或当前时间开始正向填充<br>4. 同一工作中心同一时间只安排一个工序（有限产能约束）<br>5. 相邻工序之间预留换模/清理时间<br>6. 每个 OperationOrder 的 plannedStartDateT/plannedEndDateT 写入<br>7. status→PLANNED |
| **后置条件** | 排产方案已发布；OperationOrder 已锁定产能 |
| **异常** | 产能不足时系统标记冲突，等待计划员手动调整 |
| **跨域协作** | 读取 maintenance/downtime 约束（ErpApsConstraint） |

## UC-APS-03 后向排产

| 项目 | 内容 |
|------|------|
| **概述** | 从客户要求交期反向推算各工序的最晚完成时间 |
| **触发条件** | 计划员执行排产方案（schedule mode = BACKWARD） |
| **前置条件** | 同前向排产，且 WorkOrder.requiredEndDate（从销售订单交期映射）已配置 |
| **基本流程** | 1. 从 WorkOrder.requiredEndDate 出发逆向推算每个工序的结束时间<br>2. 同一工作中心从后向前检查空闲时段<br>3. 计算每道工序的最晚开工时间<br>4. 若最终最晚开工时间早于当前时间，标记"无法按期交付" |
| **后置条件** | OperationOrder 的排程时间已写入（PLANNED） |
| **异常** | 交期不可达时告警，建议计划员改为前向排产并通知销售 |
| **跨域协作** | sales（读取客户交期） |

## UC-APS-04 插单/急单插入

| 项目 | 内容 |
|------|------|
| **概述** | 紧急订单要求插入生产线，触发区间重排 |
| **触发条件** | 新插急单创建 OperationOrder 并设置高优先级 |
| **前置条件** | 被影响区间内的 OperationOrder 尚未开始执行（非 IN_PROGRESS） |
| **基本流程** | 1. 计划员创建急单 OperationOrder，设置低 priority（数字小 = 优先级高）<br>2. 系统检测新单预计插入时间窗口<br>3. 仅将受影响的 OperationOrder（时间窗口内且优先级低于新单）回退到 DRAFT<br>4. 不影响窗口外的 OperationOrder（避免牛顿效应）<br>5. 执行区间重排，所有受影响工序重新写入 plannedStartDateT/plannedEndDateT |
| **后置条件** | 急单已插入，受影响工序已重排 |
| **异常** | 若受影响工序已 IN_PROGRESS，不可重排，需人工决定 |
| **跨域协作** | 无 |

## UC-APS-05 ATP/CTP 交期承诺

| 项目 | 内容 |
|------|------|
| **概述** | 销售订单审核时通过 APS 模拟排产获取可承诺交期 |
| **触发条件** | 销售订单行审核前调用 ATP/CTP 检查 |
| **前置条件** | 物料工艺路线已定义；工作中心产能配置完整 |
| **基本流程** | 1. 销售订单审核触发 ATP/CTP 请求<br>2. APS 域接收物料 + 数量 + 期望交期<br>3. 创建"影子"OperationOrder（不持久化）<br>4. 在现有排产方案上模拟排产（考虑产能占用）<br>5. 前向排产 计算最早可交付日期（ATP）<br>6. 或后向排产 按期望交期检查产能是否足够（CTP）<br>7. 返回结果给销售域 最早交付日期或"交期可行" |
| **后置条件** | 无持久化变更（仅模拟） |
| **异常** | 产能不足时返回建议交期（最早可用时间） |
| **跨域协作** | sales（ATP/CTP 接口）；master-data（物料/工艺路线） |

## UC-APS-06 替代工艺路线

| 项目 | 内容 |
|------|------|
| **概述** | 同一工序配置多个可用工作中心（主选+备选），排产时按优先级自动选择，主选产能不足时自动降级到备选 |
| **触发条件** | 排产引擎在选择工作中心时触发路由选择 |
| **前置条件** | ErpApsOpRouting 已配置；各工作中心产能/日历正确 |
| **基本流程** | 1. 计划员配置工序的替代路由（ErpApsOpRouting），设定优先级、换模时间差、单件加工时间差<br>2. 排产引擎在分配工作中心时，按 priority ASC 依次检查路由可用性<br>3. 如主选（isDefault=true）可用则使用主选；如主选产能不足则自动尝试备选<br>4. 选中的路由回写操作工单的 selectedRoutingId<br>5. 如全部路由不可用则标记为 UNSCHEDULABLE<br>6. 计划员可强制指定某条路由（manualOverride） |
| **后置条件** | 操作工单已绑定最终选用的工作中心和路由 |
| **异常** | 所有备选路由均不可用时工序标记为不可排产并告警 |
| **跨域协作** | scheduling（排产引擎调用路由选择逻辑）；manufacturing（工艺路线/BOM） |

## UC-APS-07 自动派工

| 项目 | 内容 |
|------|------|
| **概述** | 根据预定义规则（物料齐套+操作工可用+工装可用），自动将 PLANNED 状态的工序推动为 IN_PROGRESS |
| **触发条件** | 定时任务每分钟扫描符合自动派工条件的工序 |
| **前置条件** | ErpApsDispatchRule 已配置；OperationOrder 状态为 PLANNED |
| **基本流程** | 1. 配置工作中心的自动派工规则（ErpApsDispatchRule）：打开/关闭自动派工、条件检查开关、前瞻窗口等<br>2. 自动派工引擎周期性扫描 PLANNED 工序，检查 plannedStartDateT 在窗口内<br>3. 按规则检查物料齐套、操作工可用、工装可用<br>4. 全部条件满足 → OperationOrder.status=IN_PROGRESS，记录 ErpApsDispatchLog<br>5. 条件不满足则跳过（等待下一轮）<br>6. 计划员可手动强制派工（跳过条件检查）或将工序置为 HOLD 暂不派工 |
| **后置条件** | 工序已进入执行状态；JobCard 允许开始报工 |
| **异常** | 自动派工后发现物料不足 → 工序暂停（ON_HOLD）并通知计划员 |
| **跨域协作** | inventory（物料齐套检查）；manufacturing（创建 JobCard 开始报工） |
