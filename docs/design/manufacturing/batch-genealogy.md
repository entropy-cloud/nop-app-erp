# 生产批次追溯（Batch Genealogy / Traceability）

> 原料批次 → 生产批次 → 成品批次全链路追溯，支持前向追溯与反向追溯。
> 参考：ISO 9001 追溯性要求、FDA 21 CFR Part 11、SAP batch management

## 业务目标

- 建立原料批次 → 生产批次 → 成品批次的完整追溯链
- 前向追溯：给定成品批次，识别使用了哪些原料批次
- 反向追溯：给定原料批次，识别进入了哪些成品批次
- 支持法规合规：召回范围识别、质量事件调查
- 批次构成记录：每个产出批次的各输入批次数量

## 追溯模型

### 追溯方向

```
原料批次 ──→ 生产工序 ──→ 中间品批次 ──→ 生产工序 ──→ 成品批次
    ↑                                             ↑
    └────────── 反向追溯（原料→成品）──────────────┘
    ──────────────────────────────────────────────→
              前向追溯（成品→原料）
```

### 追溯链类型

| 类型 | 方向 | 示例问题 |
|------|------|---------|
| **前向追溯** | 成品批次 → 原料批次 | 某批成品用了哪些批次的原料？ |
| **反向追溯** | 原料批次 → 成品批次 | 某批原料出现在了哪些成品中？ |
| **节点追溯** | 某工序/某批次 | 某批次在某个工序上的加工记录 |

## 数据模型

### ErpMfgBatchGenealogy（批次追溯记录）

每条记录表示一个输入批次到产出批次的消耗关系。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| workOrderId | BIGINT | 工单ID |
| jobCardId | BIGINT | 作业卡ID（可选） |
| operationId | BIGINT | 工序ID（可选） |
| inputLotId | BIGINT | 输入批次ID（原料/中间品） |
| inputMaterialId | BIGINT | 输入物料ID |
| inputQty | DECIMAL(20,4) | 投入数量 |
| inputUoMId | BIGINT | 投入计量单位 |
| outputLotId | BIGINT | 产出批次ID（中间品/成品） |
| outputMaterialId | BIGINT | 产出物料ID |
| outputQty | DECIMAL(20,4) | 产出数量 |
| outputUoMId | BIGINT | 产出计量单位 |
| productionDate | DATE | 生产日期 |
| productionTime | DATETIME | 生产时间 |
| lineNo | INT | 行号 |
| lotStatus | VARCHAR(50) | 批次状态：ACTIVE / QUARANTINE / RELEASED / REJECTED |
| isInputConsumed | BOOLEAN | 输入是否已消耗 |
| remark | VARCHAR(1000) | 备注 |
| delVersion | BIGINT | 逻辑删除版本 |
| version | INT | 数据版本 |
| createdBy | VARCHAR(50) | 创建人 |
| createTime | TIMESTAMP | 创建时间 |
| updatedBy | VARCHAR(50) | 修改人 |
| updateTime | TIMESTAMP | 修改时间 |

### 核心查询

```sql
-- 前向追溯：给定产出批次，查找所有输入批次
SELECT * FROM erp_mfg_batch_genealogy
WHERE output_lot_id = :outputLotId;

-- 反向追溯：给定输入批次，查找所有产出批次
SELECT * FROM erp_mfg_batch_genealogy
WHERE input_lot_id = :inputLotId;

-- 全链追溯：递归查找多级
-- 前向：用 output_lot_id 找到 input_lot_ids → 这些 input_lot 作为上游 output_lot 继续查找
-- 反向：用 input_lot_id 找到 output_lot_ids → 这些 output_lot 作为下游 input_lot 继续查找
```

## 使用场景

### 场景 1：成品批次召回
```
1. 质检发现某成品批次（BATCH-FG-2025-001）不合格
2. 前向追溯：查找该批次使用了哪些原料批次
3. 反向追溯：这些原料批次还用于哪些其他成品批次
4. 识别所有受影响成品批次 → 确定召回范围
```

### 场景 2：原料问题追溯
```
1. 供应商通知某原料批次（RAW-2025-015）有问题
2. 反向追溯：该原料批次用于哪些成品批次
3. 定位所有受影响成品 → 发起质检或召回
```

### 场景 3：生产过程合规
```
1. 监管审计要求提供某批次完整生产记录
2. 全链追溯：从最终成品到最初原料批次
3. 导出批次图谱报告
```

## 涉及的领域机制

- `state-machine.md` — 工单状态触发批次记录
- `bom-and-routing.md` — 生产版本与BOM路由
- `material-reservation.md` — 领料与批次消耗
- `../inventory/lot-management.md` — 库存批次管理（lot master）
- `../quality/inspection-integration.md` — 质检与批次隔离
