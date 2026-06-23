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

```java
/**
 * 正向追溯：查询移动单触发的所有下游移动单
 */
public List<ErpInvStockMove> findDownstreamMoves(Long moveId) {
    ErpInvStockMove move = stockMoveDao.findById(moveId);
    if (move.getDestMoveIds() == null || move.getDestMoveIds().isEmpty()) {
        return Collections.emptyList();
    }
    return stockMoveDao.findByIds(move.getDestMoveIds());
}

/**
 * 递归正向追溯：查询完整下游链路
 */
public List<ErpInvStockMove> findDownstreamChain(Long moveId) {
    List<ErpInvStockMove> chain = new ArrayList<>();
    collectDownstreamMoves(moveId, chain);
    return chain;
}

private void collectDownstreamMoves(Long moveId, List<ErpInvStockMove> chain) {
    List<ErpInvStockMove> downstream = findDownstreamMoves(moveId);
    for (ErpInvStockMove move : downstream) {
        chain.add(move);
        collectDownstreamMoves(move.getId(), chain); // 递归
    }
}
```

### 反向追溯查询

```java
/**
 * 反向追溯：查询移动单的上游移动单
 */
public ErpInvStockMove findUpstreamMove(Long moveId) {
    ErpInvStockMove move = stockMoveDao.findById(moveId);
    if (move.getOriginMoveId() == null) {
        return null;
    }
    return stockMoveDao.findById(move.getOriginMoveId());
}

/**
 * 递归反向追溯：查询完整上游链路
 */
public List<ErpInvStockMove> findUpstreamChain(Long moveId) {
    List<ErpInvStockMove> chain = new ArrayList<>();
    collectUpstreamMoves(moveId, chain);
    return chain;
}

private void collectUpstreamMoves(Long moveId, List<ErpInvStockMove> chain) {
    ErpInvStockMove upstream = findUpstreamMove(moveId);
    if (upstream != null) {
        chain.add(upstream);
        collectUpstreamMoves(upstream.getId(), chain); // 递归
    }
}
```

### 退货追溯查询

```java
/**
 * 退货追溯：查询退货移动单的原出库/入库移动单
 */
public ErpInvStockMove findOriginReturnMove(Long moveId) {
    ErpInvStockMove move = stockMoveDao.findById(moveId);
    if (move.getOriginReturnedMoveId() == null) {
        return null;
    }
    return stockMoveDao.findById(move.getOriginReturnedMoveId());
}

/**
 * 查询移动单关联的所有退货移动单
 */
public List<ErpInvStockMove> findReturnedMoves(Long moveId) {
    ErpInvStockMove move = stockMoveDao.findById(moveId);
    if (move.getReturnedMoveIds() == null || move.getReturnedMoveIds().isEmpty()) {
        return Collections.emptyList();
    }
    return stockMoveDao.findByIds(move.getReturnedMoveIds());
}
```

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

```java
/**
 * 从凭证追溯到移动单
 */
public ErpInvStockMove findMoveByVoucher(Long voucherId) {
    FinVoucherBillR billR = voucherBillRDao.findByVoucherId(voucherId);
    if (billR.getBillType().equals("STOCK_MOVE")) {
        return stockMoveDao.findById(billR.getBillId());
    }
    return null;
}

/**
 * 计算移动单成本
 */
public BigDecimal calculateMoveCost(Long moveId) {
    // 追溯凭证获取成本
    FinVoucherLine line = voucherLineDao.findBySourceBill("STOCK_MOVE", moveId);
    return line.getDrAmount(); // 或 crAmount
}
```

## 追溯链维护

### 创建移动单时建立追溯链

```java
/**
 * 创建移动单时建立追溯链
 */
public void createMoveWithTrace(ErpInvStockMove move, Long originMoveId) {
    // 设置上游移动单
    move.setOriginMoveId(originMoveId);
    
    // 更新上游移动单的下游列表
    if (originMoveId != null) {
        ErpInvStockMove originMove = stockMoveDao.findById(originMoveId);
        originMove.addDestMoveId(move.getId());
        stockMoveDao.save(originMove);
    }
    
    stockMoveDao.save(move);
}
```

### 创建退货移动单时建立追溯链

```java
/**
 * 创建退货移动单时建立追溯链
 */
public void createReturnMoveWithTrace(ErpInvStockMove returnMove, Long originReturnedMoveId) {
    // 设置原出库/入库移动单
    returnMove.setOriginReturnedMoveId(originReturnedMoveId);
    
    // 更新原移动单的退货列表
    ErpInvStockMove originMove = stockMoveDao.findById(originReturnedMoveId);
    originMove.addReturnedMoveId(returnMove.getId());
    stockMoveDao.save(originMove);
    
    stockMoveDao.save(returnMove);
}
```

### 取消移动单时清理追溯链

```java
/**
 * 取消移动单时清理追溯链
 */
public void cancelMoveWithTrace(Long moveId) {
    ErpInvStockMove move = stockMoveDao.findById(moveId);
    
    // 清理上游移动单的下游列表
    if (move.getOriginMoveId() != null) {
        ErpInvStockMove originMove = stockMoveDao.findById(move.getOriginMoveId());
        originMove.removeDestMoveId(moveId);
        stockMoveDao.save(originMove);
    }
    
    // 清理原移动单的退货列表
    if (move.getOriginReturnedMoveId() != null) {
        ErpInvStockMove originMove = stockMoveDao.findById(move.getOriginReturnedMoveId());
        originMove.removeReturnedMoveId(moveId);
        stockMoveDao.save(originMove);
    }
    
    // 清理下游移动单的上游引用
    if (move.getDestMoveIds() != null) {
        for (Long destId : move.getDestMoveIds()) {
            ErpInvStockMove destMove = stockMoveDao.findById(destId);
            destMove.setOriginMoveId(null);
            stockMoveDao.save(destMove);
        }
    }
}
```

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