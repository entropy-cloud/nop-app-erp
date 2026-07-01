# 库存追溯链设计

## 目的

说明库存移动单的自追溯链设计，支持采购到货→生产领料→销售出库的全链路追溯。参考 Odoo 的 `move_orig_ids`/`move_dest_ids` 自关联设计。

本文件是 `odoo.md` 调研结论的落地设计，是 `inventory/README.md` 的详细展开。

## 设计背景

### 调研发现

从 Odoo 的调研中发现：

| 发现 | 说明 |
|------|------|
| 移动单自追溯链 | `move_orig_ids`/`move_dest_ids` M2M 自关联 |
| 退货反查 | `origin_returned_move_id`/`returned_move_ids` |
| 上下游移动关联 | 支持全链路追溯 |
| 规则触发 | `rule_id` 触发本移动 |

### 核心价值

- **全链路追溯**：从原材料采购到产成品销售的完整链路
- **退货反查**：快速定位退货的原始入库
- **批次追溯**：关联批次信息，支持效期管理
- **业财回链**：库存移动关联凭证，支持成本核算

## 追溯链模型

### 移动单自关联

库存移动单通过自关联字段建立追溯链：

| 字段 | 说明 | 关系 |
|------|------|------|
| originMoveId | 上游移动单ID | 关联触发本移动的上游移动单 |
| destMoveIds | 下游移动单ID列表 | 本移动触发的下游移动单 |
| originReturnedMoveId | 原始退货移动单ID | 退货移动单关联的原出库移动单 |
| returnedMoveIds | 退货移动单ID列表 | 本移动关联的退货移动单 |

> **实现说明（存储模型偏离，2026-07-02 计划 0700-1）**：上表概念为 M2M 自关联（Odoo `move_orig_ids`/`move_dest_ids`），但本仓**实际落地为「单 uplink 列 + 反向查询」**——移动单只持久化两个可空上链列 `originMoveId`（上游移动单）与 `originReturnedMoveId`（退货指向的原出/入库移动单），下游链 `destMoveIds`/`returnedMoveIds` 以**反向查询**表达（按 `originMoveId=?` / `originReturnedMoveId=?` 反查），不存 M2M 中间表、不做双向维护。理由：避免 M2M 中间表 + 删除时双向清理的复杂度；现网所有联动（采购入库→出库、调拨出→入、退货→原单）均为单上游。残留风险：多源合并移动单（理论多上游）单 uplink 只记主上游，多源场景出现时改 M2M/多 uplink 列（Follow-up）。

### 追溯链结构

```
库存追溯链结构
        │
        ├─► 采购入库移动单（MOVE001）
        │      ├─ originMoveId: null（无上游）
        │      ├─ destMoveIds: [MOVE002, MOVE003]
        │      └─ returnedMoveIds: [MOVE005]
        │
        ├─► 生产领料移动单（MOVE002）
        │      ├─ originMoveId: MOVE001
        │      ├─ destMoveIds: [MOVE004]
        │      └─ returnedMoveIds: []
        │
        ├─► 生产领料移动单（MOVE003）
        │      ├─ originMoveId: MOVE001
        │      ├─ destMoveIds: []
        │      └─ returnedMoveIds: []
        │
        ├─► 生产完工入库移动单（MOVE004）
        │      ├─ originMoveId: MOVE002
        │      ├─ destMoveIds: [MOVE006]
        │      └─ returnedMoveIds: []
        │
        ├─► 采购退货移动单（MOVE005）
        │      ├─ originMoveId: null
        │      ├─ originReturnedMoveId: MOVE001
        │      ├─ destMoveIds: []
        │      └─ returnedMoveIds: []
        │
        └─► 销售出库移动单（MOVE006）
               ├─ originMoveId: MOVE004
               ├─ destMoveIds: []
               ├─ originReturnedMoveId: null
               └─ returnedMoveIds: [MOVE007]
        
        销售退货移动单（MOVE007）
               ├─ originMoveId: null
               ├─ originReturnedMoveId: MOVE006
               └─ destMoveIds: []
```

### 追溯链方向

| 方向 | 说明 | 用途 |
|------|------|------|
| 正向追溯 | origin → dest | 从采购到销售的完整链路 |
| 反向追溯 | dest → origin | 从销售追溯到采购来源 |
| 退货追溯 | returned → origin | 退货追溯到原出库/入库 |

## 追溯链场景

### 采购→销售链路

```
采购→销售完整链路
        │
        ├─► 采购订单审核 → 生成采购入库移动单（MOVE001）
        │
        ├─► 生产工单审核 → 生成生产领料移动单（MOVE002）
        │      └─ originMoveId = MOVE001（追溯采购入库）
        │
        ├─► 生产工单完工 → 生成生产完工入库移动单（MOVE003）
        │      └─ originMoveId = MOVE002（追溯生产领料）
        │
        └─► 销售订单审核 → 生成销售出库移动单（MOVE004）
               └─ originMoveId = MOVE003（追溯生产完工）
        
正向追溯查询：
  MOVE001 → MOVE002 → MOVE003 → MOVE004
  从采购入库追溯到最终销售出库
```

### 退货追溯链路

```
采购退货追溯链路
        │
        ├─► 采购入库移动单（MOVE001）
        │      └─ returnedMoveIds = [MOVE002]
        │
        └─► 采购退货移动单（MOVE002）
               ├─ originReturnedMoveId = MOVE001
               └─ 追溯到原入库移动单
        
反向追溯查询：
  MOVE002 → MOVE001
  从采购退货追溯到原采购入库
```

```
销售退货追溯链路
        │
        ├─► 销售出库移动单（MOVE001）
        │      └─ returnedMoveIds = [MOVE002]
        │
        └─► 销售退货移动单（MOVE002）
               ├─ originReturnedMoveId = MOVE001
               └─ 追溯到原出库移动单
        
反向追溯查询：
  MOVE002 → MOVE001
  从销售退货追溯到原销售出库
```

### 调拨追溯链路

```
调拨追溯链路
        │
        ├─► 调拨出库移动单（MOVE001）
        │      ├─ destMoveIds = [MOVE002]
        │      └─ 调拨单触发的出库
        │
        └─► 调拨入库移动单（MOVE002）
               ├─ originMoveId = MOVE001
               └─ 调拨单触发的入库
        
双向追溯：
  MOVE001 → MOVE002（正向）
  MOVE002 → MOVE001（反向）
```

## 追溯链查询

### 正向追溯查询

- 正向追溯：通过移动单的 destMoveIds 查询所有下游移动单
- 递归正向追溯：逐层遍历下游移动单，构建完整下游链路（最大深度由配置项 erp-inv.trace-chain-max-depth 控制）
- 应用场景：从采购入库追溯至最终销售出库

### 反向追溯查询

- 反向追溯：通过移动单的 originMoveId 查询上游移动单
- 递归反向追溯：逐层遍历上游移动单，构建完整上游链路
- 应用场景：从销售出库追溯至原始采购入库

### 退货追溯查询

- 退货追溯：通过 originReturnedMoveId 查询退货移动单的原出库/入库移动单
- 反向退货追溯：通过 returnedMoveIds 查询原移动单关联的所有退货移动单
- 应用场景：从采购退货追溯至原采购入库，从销售退货追溯至原销售出库

## 追溯链与批次

### 批次追溯

移动单行关联批次信息，支持批次追溯：

```
批次追溯链路
        │
        ├─► 采购入库移动单行
        │      ├─ batchId: BATCH001
        │      ├─ 生产日期: 2026-06-01
        │      └─ 保质期: 365天
        │
        ├─► 生产领料移动单行
        │      ├─ sourceMoveLineId: 入库行ID
        │      └─ batchId: BATCH001（继承原批次）
        │
        ├─► 生产完工入库移动单行
        │      ├─ batchId: BATCH002（新批次）
        │      └─ 生产日期: 2026-06-15
        │
        └─► 销售出库移动单行
               ├─ sourceMoveLineId: 完工行ID
               └─ batchId: BATCH002（继承完工批次）
        
批次追溯查询：
  从销售出库追溯到生产批次 → 原材料批次
```

### 批次效期追溯

```
批次效期追溯
        │
        ├─► 查询即将过期批次
        │      ├─ 批次效期 < 当前日期 + 预警天数
        │      └─ 追溯批次来源移动单
        │
        ├─► 查询已过期批次库存
        │      ├─ 批次效期 < 当前日期
        │      └─ 追溯批次入库移动单
        │
        └─► 查询批次销售去向
               ├─ 批次出库移动单
               └─ 追溯销售订单/客户
```

## 追溯链与凭证

### 业财回链

移动单完成触发凭证生成，凭证关联移动单：

```
业财回链
        │
        ├─► 移动单完成
        │      ├─ 触发存货估值凭证生成
        │      └─ 凭证关联移动单
        │
        ├─► 凭证记录
        │      ├─ voucherType: STOCK_MOVE
        │      ├─ voucherNo: MOVE001
        │      └─ 业财回链表记录
        │
        └─► 成本追溯
               ├─ 从凭证追溯到移动单
               ├─ 从移动单追溯到批次
               └─ 计算存货成本
```

### 成本追溯查询

- 凭证→移动单追溯：通过业财回链表（FinVoucherBillR）按 billType=STOCK_MOVE 反查移动单
- 移动单→成本追溯：通过凭证行查询移动单存货成本（借方或贷方金额）

## 追溯链维护

### 创建移动单时建立追溯链

- 创建移动单时传入 originMoveId 建立上游关联
- 同时更新上游移动单的 destMoveIds 列表（双向关联）
- 正向链路：originMoveId → destMoveIds 双向维护

### 创建退货移动单时建立追溯链

- 创建退货移动单时设置 originReturnedMoveId 建立与原移动单的关联
- 同时更新原移动单的 returnedMoveIds 列表（双向关联）
- 退货链路：originReturnedMoveId ↔ returnedMoveIds 双向维护

### 取消移动单时清理追溯链

- 取消移动单时从其上游移动单的 destMoveIds 中移除本移动单引用
- 从其原移动单的 returnedMoveIds 中移除本移动单引用
- 清空下游移动单的 originMoveId 引用
- 保证追溯链在单据取消后保持一致性

## 追溯链可视化

### 追溯链树形展示

```
追溯链树形展示
        │
        ├─► 采购入库（MOVE001）
        │      ├─ 生产领料（MOVE002）
        │      │    └─ 生产完工（MOVE004）
        │      │         └─ 销售出库（MOVE006）
        │      │              └─ 销售退货（MOVE007）
        │      ├─ 生产领料（MOVE003）
        │      └─ 采购退货（MOVE005）
        │
        └─► 查询展示
               ├─ 树形结构
               ├─ 节点信息（移动单类型、数量、日期）
               └─ 点击节点查看详情
```

### 追溯链报表

| 报表 | 说明 |
|------|------|
| 物料追溯报表 | 按物料查询完整追溯链 |
| 批次追溯报表 | 按批次查询完整追溯链 |
| 订单追溯报表 | 按订单查询关联移动单 |
| 成本追溯报表 | 按凭证追溯到移动单 |

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-inv.trace-chain-enabled` | true | 是否启用追溯链 |
| `erp-inv.trace-chain-max-depth` | 10 | 追溯链最大深度 |
| `erp-inv.trace-chain-batch-required` | true | 是否必须追溯批次 |
| `erp-inv.trace-chain-voucher-link` | true | 是否关联凭证 |

## 开源参考

| 项目 | 参考维度 | 具体借鉴 |
|------|----------|----------|
| Odoo | 移动单自追溯链 | `move_orig_ids`/`move_dest_ids` M2M 自关联 |
| Odoo | 退货反查 | `origin_returned_move_id`/`returned_move_ids` |
| ERPNext | 不可变库存流水 | `voucher_type`+`voucher_no` 反查源单 |
| 赤龙 | 业财回链 | `FinVoucherBillR` 凭证与业务单据关联 |