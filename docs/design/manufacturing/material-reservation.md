# 工单物料预留设计

## 目的

说明制造域工单的物料预留机制，包括预留量计算、预留状态管理、齐套校验、预留释放等。参考 ERPNext 的 Work Order 物料预留独立状态维度设计。

本文件是 `erpnext.md` 调研结论的落地设计，是 `manufacturing/README.md` 的详细展开。

> **预留实体归属说明**：物料预留的持久化真相源是**库存域**的 `ErpInvReservation`/`ErpInvReservationLine`（见 `module-inventory/model/app-erp-inventory.orm.xml`），而非本域独立的 `ErpMfgMaterialReservation`。制造域工单通过库存域预留机制为子件创建预留记录（`ErpInvReservationLine` 关联工单）。下文出现的 `ErpMfgMaterialReservation` 及其字段（`reservedQty`/`pickedQty`/`releasedQty`/`reservationStatus`）为**业务语义说明**，实际落位以库存域 `ErpInvReservation*` 模型为准；预留量的读写跨域走 `IErpInvReservationBiz` 接口，不在制造域做 ORM 跨工程引用。预留量字段（`reservedQty`/可用量）的真相源是 `ErpInvStockBalance`。

## 设计背景

### 调研发现

从 ERPNext 的调研中发现：

| 发现 | 说明 |
|------|------|
| 工单物料预留独立状态 | Stock Reserved / Partially Reserved |
| 预留量与可用量分离 | reserved_quantity / available_quantity |
| 齐套校验 | 工单进入生产前校验所有子件库存可用量 |
| 预留释放 | 工单取消/完成时释放预留 |

### 核心价值

- **物料预留**：工单审核后预留子件库存
- **齐套校验**：生产前校验所有子件是否齐套
- **预留状态**：独立状态维度追踪预留进度
- **预留释放**：工单取消/完成时自动释放

## 物料预留模型

### 预留记录

工单审核后为每个子件创建预留记录，每行包含以下信息：

- workOrderId：关联工单
- workOrderLineId：关联工单行（BOM 子件行）
- materialId / skuId：物料与 SKU
- warehouseId：仓库
- requiredQty：需求数量（BOM 展开量）
- reservedQty：已预留数量
- pickedQty：已领料数量
- releasedQty：已释放数量
- reservationStatus：预留状态（UNRESERVED / PARTIAL_RESERVED / RESERVED / PARTIAL_PICKED / PICKED / RELEASED）

### 预留状态

| 状态 | 编码 | 说明 |
|------|------|------|
| 未预留 | UNRESERVED | 工单未审核，无预留 |
| 部分预留 | PARTIAL_RESERVED | 部分子件已预留 |
| 已预留 | RESERVED | 所有子件已预留 |
| 部分领料 | PARTIAL_PICKED | 部分子件已领料 |
| 已领料 | PICKED | 所有子件已领料 |
| 已释放 | RELEASED | 预留已释放 |

### 工单预留状态维度

工单增加独立的预留状态维度 reservationStatus，与 docStatus（业务生命周期）、approveStatus（审批状态）组成三轴状态：

## 预留流程

### 工单审核触发预留

```
工单审核触发预留
        │
        ├─► 步骤1：工单审核通过
        │      └─ 触发物料预留
        │
        ├─► 步骤2：计算子件需求
        │      ├─ 按 BOM 展开子件清单
        │      ├─ 计算每个子件需求量 = BOM子件量 × 工单产出量
        │      └─ 考虑多级 BOM 展开
        │
        ├─► 步骤3：创建预留记录
        │      ├─ 为每个子件创建预留记录
        │      ├─ requiredQty = 子件需求量
        │      └─ reservedQty = 0（初始）
        │
        ├─► 步骤4：执行预留
        │      ├─ 查询子件可用量
        │      ├─ 可用量充足：reservedQty = requiredQty
        │      ├─ 可用量不足：reservedQty = 可用量
        │      └─ 更新库存预留量
        │
        ├─► 步骤5：更新预留状态
        │      ├─ 所有子件预留完成：reservationStatus = RESERVED
        │      ├─ 部分子件预留完成：reservationStatus = PARTIAL_RESERVED
        │      └─ 无预留：reservationStatus = UNRESERVED
        │
        └─► 步骤6：生成预留报告
               ├─ 预留成功清单
               ├─ 预留不足清单
               └─ 建议补货清单
```

### 预留量计算

- 预留量 = min(需求量, 可用量)，可用量 = 现有量 - 已预留量（从 ErpInvStockBalance 读取）
- 无库存时预留量为 0
- 每个子件预留完成后更新工单整体预留状态

### 库存预留量更新

- 预留/释放/领料时同步更新 ErpInvStockBalance 的 reservedQty 与 availableQty
- reservedQty 增加对应预留，减少对应释放或领料
- availableQty = onHandQty - reservedQty（自动计算）

## 齐套校验

### 齐套定义

齐套指所有子件库存可用量满足工单需求：

```
齐套校验规则
        │
        ├─► 齐套条件：
        │      所有子件可用量 ≥ 需求量
        │
        ├─► 部分齐套条件：
        │      部分子件可用量 ≥ 需求量
        │
        └─► 不齐套条件：
               所有子件可用量 < 需求量
```

### 齐套校验流程

```
齐套校验流程
        │
        ├─► 步骤1：获取工单子件清单
        │      ├─ 按 BOM 展开子件
        │      └─ 计算每个子件需求量
        │
        ├─► 步骤2：查询每个子件可用量
        │      ├─ 可用量 = 现有量 - 已预留量
        │      └─ 考虑多仓库汇总
        │
        ├─► 步骤3：对比需求量与可用量
        │      ├─ 可用量 ≥ 需求量：齐套
        │      ├─ 可用量 < 需求量：不齐套
        │      └─ 记录缺口数量
        │
        ├─► 步骤4：生成齐套报告
        │      ├─ 齐套子件清单
        │      ├─ 不齐套子件清单（缺口数量）
        │      └─ 建议补货数量
        │
        └─► 步骤5：齐套状态更新
               ├─ 全部齐套：齐套状态 = 齐套
               ├─ 部分齐套：齐套状态 = 部分齐套
               └─ 全部不齐套：齐套状态 = 不齐套
```

### 齐套校验规则

- 遍历所有预留记录，对比每项的可用量（onHandQty - reservedQty）与需求量（requiredQty）
- 所有子件可用量 ≥ 需求量 → 齐套（KITTED）
- 部分子件可用量 ≥ 需求量 → 部分齐套（PARTIAL_KITTED）
- 所有子件可用量 < 需求量 → 不齐套（NOT_KITTED）
- 记录缺口数量（shortageQty）用于补料建议

### 齐套状态与生产

| 齐套状态 | 生产动作 |
|----------|----------|
| 齐套（KITTED） | 可以开始生产 |
| 部分齐套（PARTIAL_KITTED） | 可以部分生产，或等待补货 |
| 不齐套（NOT_KITTED） | 不能开始生产，需先补货 |

## 预留释放

### 预留释放场景

| 场景 | 说明 |
|------|------|
| 工单取消 | 取消工单，释放所有预留 |
| 工单完成 | 完成工单，已领料部分消耗，未领料部分释放 |
| 工单减量 | 减少产出量，释放多余预留 |
| 手工释放 | 手工释放部分预留 |

### 预留释放流程

```
预留释放流程
        │
        ├─► 步骤1：计算释放数量
        │      ├─ 工单取消：释放所有预留
        │      ├─ 工单完成：释放未领料部分
        │      └─ 工单减量：释放多余预留
        │
        ├─► 步骤2：更新预留记录
        │      ├─ releasedQty += 释放数量
        │      └─ reservedQty -= 释放数量
        │
        ├─► 步骤3：更新库存预留量
        │      ├─ 库存预留量 -= 释放数量
        │      └─ 库存可用量 += 释放数量
        │
        └─► 步骤4：更新预留状态
               ├─ 全部释放：reservationStatus = RELEASED
               └─ 部分释放：更新为相应状态
```

### 预留释放处理

- 工单取消：释放所有子件的全部预留量
- 工单完成：释放未领料部分的预留量，已领料部分消耗
- 工单减量：按比例释放多余预留（reservedQty = reservedQty × 新量/原量）
- 每次释放同步更新库存预留量（减少）并重算工单预留状态

## 领料与预留

### 领料扣减预留

领料时扣减预留量：

```
领料扣减预留
        │
        ├─► 步骤1：创建领料移动单
        │      ├─ 关联工单
        │      └─ 按预留记录选择物料
        │
        ├─► 步骤2：领料数量校验
        │      ├─ 领料数量 ≤ 预留数量
        │      ├─ 超过预留：拒绝或警告
        │      └─ 未预留物料：按可用量校验
        │
        ├─► 步骤3：更新预留记录
        │      ├─ pickedQty += 领料数量
        │      └─ reservedQty -= 领料数量
        │
        ├─► 步骤4：更新库存预留量
        │      ├─ 预留量 -= 领料数量
        │      └─ 现有量 -= 领料数量（出库）
        │
        └─► 步骤5：更新预留状态
               ├─ 全部领料：reservationStatus = PICKED
               └─ 部分领料：reservationStatus = PARTIAL_PICKED
```

### 领料处理

- 领料时校验领料数量 ≤ 预留数量（超过预留时拒绝或按配置告警）
- 领料成功后更新预留记录：pickedQty += 领料量，reservedQty -= 领料量
- 同步更新库存预留量（减少）和可用量
- 全部领料完成时工单预留状态变为 PICKED

## 预留报表

### 预留报表清单

| 报表 | 说明 |
|------|------|
| 工单预留明细表 | 按工单展示预留详情 |
| 物料预留汇总表 | 按物料展示预留汇总 |
| 齐套状态报表 | 按工单展示齐套状态 |
| 预留缺口报表 | 展示预留不足的物料 |

### 预留查询

- 按物料查询：查询某物料的所有预留记录（物料预留汇总）
- 按工单查询：查询某工单的所有预留明细（工单预留明细）
- 预留缺口查询：筛选 availableQty < requiredQty 的记录（补料建议）

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-mfg.reservation-enabled` | true | 是否启用物料预留 |
| `erp-mfg.reservation-on-approve` | true | 审核时是否自动预留 |
| `ErpMfgBom.consumption` | STRICT | 齐套严格度,按 BOM 配置(见 ORM 字段) |
| `erp-mfg.over-pick-warning` | true | 超预留领料是否警告 |
| `erp-mfg.auto-release-on-complete` | true | 完工时是否自动释放未领料预留 |

## 开源参考

| 项目 | 参考维度 | 具体借鉴 |
|------|----------|----------|
| ERPNext | 工单预留状态 | Stock Reserved / Partially Reserved |
| ERPNext | 齐套校验 | Work Order 齐套检查 |
| Odoo | 预留量字段 | stock.quant 的 reserved_quantity |
| Odoo | 预留机制 | _action_assign() 预留逻辑 |