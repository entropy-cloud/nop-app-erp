# DRP 越库（Cross-Dock）设计

## 目的

设计越库功能：入站货物不经入库上架，直接在收货月台完成 inbound → outbound 匹配后发往最终客户/门店。减少仓储操作（收货→上架→拣货→发货 压缩为 收货→越库分拨→发货），适用于快消品、生鲜、JIT 补货场景。

## 设计依据

> 参考 **Odoo Cross-Docking**：收货时标记为越库（Cross-Dock），系统自动匹配出库需求并创建出库移动。
>
> 参考 **WMS Cross-Dock** 通用功能：入站 ASN/PO 收货时识别越库标记，绕过 putaway 直接进入 staging 区并分配 outbound dock。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §供应链计划。

## 概念模型

```
传统入库流程：
  ┌────────┐   ┌──────────┐   ┌────────┐   ┌──────────┐   ┌─────────┐
  │ 收货   │ → │ 上架     │ → │ 存储   │ → │ 拣货     │ → │ 发货    │
  └────────┘   └──────────┘   └────────┘   └──────────┘   └─────────┘

越库流程：
  ┌────────┐   ┌──────────────┐   ┌─────────┐
  │ 收货   │ → │ 越库分拨区   │ → │ 发货    │
  └────────┘   │（暂存分拣） │   └─────────┘
               └──────────────┘
                     ↓
           inbound → outbound 匹配
           push 到出库 dock
```

## 越库触发方式

### 方式 1：DRP 计划行标记越库

DRP 计划员在审批补货建议时，将特定行标记为 cross-dock 模式。系统生成补货单（采购单/调拨单）时同时标记，收货时识别。

### 方式 2：ASN 入站时标记越库

供应商推送 ASN 时，系统根据物料/仓库配置自动识别该批次是否需要越库（如物料配置了 cross-dock 策略、有匹配的销售订单等待出库）。

### 方式 3：收货时手工标记

仓管员在接收入站货物时，临时决定对某批次执行越库（如该物料库存充足、有紧急出库订单）。

## ErpInvDrpCrossDock（越库执行记录）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | |
| drpLineId | 关联 DRP 行（可选） | |
| inboundMoveId | 入站移动单 ID（→ StockMove IN） | 🟢 Odoo Stock Move |
| outboundMoveId | 出站移动单 ID（→ StockMove OUT，越库匹配后生成） | 🟢 Odoo Stock Move |
| sourceBillType/sourceBillCode | 来源业务单（采购单/调拨单号） | |
| targetBillType/targetBillCode | 目标业务单（销售订单/调拨单号） | |
| materialId | 物料 | |
| quantity | 越库数量 | |
| stagingLocationId | 越库暂存库位（→ ErpMdWarehouseLocation） | |
| dockSlotTime | 月台时间窗口（datetime） | |
| status | dict `erp-inv/drp-xdock-status`：PENDING / MATCHED / STAGING / LOADED / COMPLETED / CANCELLED | |
| matchedAt | 匹配时间 | |
| loadedAt | 装车完成时间 | |
| 标准审计字段 | | |

## 越库暂存库位

### 库位模型

系统在仓库中预定义越库暂存区（Staging Area），不同于正常存储库位：

| 库位类型 | 用途 | 库存计入 |
|----------|------|----------|
| 收货月台（RECEIVING_DOCK） | 入站卸货 | 暂不计入可用库存 |
| 越库暂存区（CROSS_DOCK_STAGING） | 越库货物暂存、分拣 | 计入"越库在途"（非可用库存） |
| 发货月台（SHIPPING_DOCK） | 出站装车 | 扣减后发出 |

### 库存处理

```
收货 → 入站移动 (StockMove IN) → 收货月台
    ↓（识别为越库）
越库暂存区 (Staging Location)
    ↓（匹配出站）
出站移动 (StockMove OUT) → 发货月台 → 发出
    ↓
越库状态 → COMPLETED
```

越库货物在暂存区的库存**不纳入可用库存计算**（ERP 层面不可被其他订单占用），仅作为中转计数。

## Inbound → Outbound 匹配

### 匹配策略

| 策略 | 方式 | 适用场景 |
|------|------|----------|
| PRE_ALLOCATED（预分配） | DRP 计划时已指定越库和目标订单 | 已知买家/门店的计划补货 |
| ON_RECEIPT（收货时匹配） | 收货时系统查找待出库的销售订单 | 紧急补货、门店要货 |
| MANUAL（人工匹配） | 仓管员在界面上指定目标订单 | 异常处理、临时决策 |

### 预分配匹配流程

```
DRP 计划标记越库
    │
    ├─► DrpLine.approvedQty = 1000，crossDockFlag = true
    │
    ├─► 生成采购单（PURCHASE 类型）→ 到货仓库 = 越库暂存区
    │
    ├─► 生成 ErpInvDrpCrossDock（PENDING）
    │     drpLineId, sourceBillCode, materialId, quantity
    │
    ├─► 采购收货 → 入站移动 → 越库暂存区
    │     CrossDock.inboundMoveId 回写
    │     CrossDock.status → STAGING
    │
    ├─► 系统查找预分配的目标订单（如门店要货单）
    │     CrossDock.targetBillType/targetBillCode 填充
    │     CrossDock.status → MATCHED
    │
    ├─► 生成出站移动（StockMove OUT → 发货月台）
    │     CrossDock.outboundMoveId 回写
    │     CrossDock.status → LOADED
    │
    └─► 出库确认 → CrossDock.status → COMPLETED
```

### 收货时匹配流程

```
ASN/PO 收货时识别 crossDockFlag = true（或操作员手工标记）
    │
    ├─► 系统扫描该物料待出库的销售订单
    │     按优先级排序：承诺发货日期 ASC → 创建时间 ASC
    │
    ├─► 找到匹配 > 系统自动分配目标订单 + 数量
    │     剩余未匹配货物 → 提示仓管员：是否进入正常入库或继续等待匹配
    │
    ├─► 生成出站移动（如完全匹配）
    │
    └─► 仓管员确认越库操作
```

## 月台预约调度

### ErpInvDrpDockAppointment（月台预约）

| 字段 | 含义 |
|------|------|
| id/warehouseId | 标准 |
| dockId | 月台编号（→ ErpMdWarehouseLocation where type=DOCK） |
| appointmentDate | 预约日期 |
| slotStart/slotEnd | 时间窗口（30min~2h 粒度） |
| crossDockId | 关联 ErpInvDrpCrossDock |
| carrierInfo | 承运商信息（车牌/司机/联系方式） |
| status | dict：AVAILABLE / BOOKED / ARRIVED / COMPLETED / CANCELLED |

### 调度规则

| 规则 | 说明 |
|------|------|
| 冲突检测 | 同一月台同一时间窗口不可重复预约 |
| 窗口粒度 | 默认 1 小时，可配置 |
| 预到达通知 | 预约时间前 1 小时通知仓管员准备 |
| 超时释放 | 超过预约时间 30 分钟未签到 → 自动释放月台 |

## 越库状态机

```
PENDING（待匹配）
  │
  ├─► [inbound 到达 + 匹配目标订单] → MATCHED
  │
  ├─► [inbound 到达 + 未匹配] → STAGING（等待匹配）
  │
  └─► [取消] → CANCELLED

STAGING（暂存等待）
  │
  ├─► [匹配目标订单] → MATCHED
  │
  ├─► [超时未匹配] → 提示转正常入库
  │
  └─► [取消] → CANCELLED

MATCHED（已匹配）
  │
  ├─► [生成出站移动 + 装车] → LOADED
  │
  └─► [取消] → CANCELLED

LOADED（已装车）
  │
  └─► [出库确认] → COMPLETED（终态）

COMPLETED（已完成，终态）
CANCELLED（已取消，终态）
```

## 业务规则

1. **越库不产生库存余额**：货物从入站移动直接转到出站移动，不在 warehouse 的可用库存余额中停留。
2. **部分越库**：同一采购单可分批次处理——部分进越库、部分正常入库。`quantity` 字段区分越库数量。
3. **越库失败回退**：STAGING 超过配置时间（`erp-inv.drp-xdock-staging-timeout`，默认 24h）未匹配到目标订单 → 自动转为正常入库（从 staging 移到正常存储位）。
4. **越库与质检**：若物料需要质检（inspection_required=true），越库货物需先完成质检（或在越库暂存区执行快检），合格后才可继续出库。
5. **越库与 ASN 关联**：ASN 入站时若标记 crossDockFlag=true，则在 ASN→采购入库流程中直接进入越库暂存区，不走正常入库。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| inventory | 生成入站/出站 StockMove，跨月台库位流转 |
| purchase | 采购单收货时识别 crossDockFlag，进入越库流程 |
| sales | 越库匹配目标销售订单，生成销售出库 StockMove |
| drp | DRP 计划行标记 crossDockFlag，预分配目标订单 |
| b2b | ASN 入站时识别 crossDockFlag |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-inv.drp-xdock-enabled` | false | 越库功能是否启用 |
| `erp-inv.drp-xdock-staging-timeout` | 24 | 越库暂存超时（小时），超时后自动转正常入库 |
| `erp-inv.drp-xdock-default-strategy` | ON_RECEIPT | 默认匹配策略 |
| `erp-inv.drp-xdock-dock-slot-duration` | 60 | 月台时间窗口长度（分钟） |
| `erp-inv.drp-xdock-dock-no-show-timeout` | 30 | 月台未签到超时释放（分钟） |

## 反模式警示

- ⛔ **越库货物计入可用库存**——越库中转货物不应被其他订单占用，应放在独立 staging 库位且不作为可用库存。
- ⛔ **越库不走入出库移动**——即便不在存储位停留，仍需要用 StockMove IN → StockMove OUT 记录完整物流轨迹。
- ⛔ **所有物料都越库**——越库适合高频、标准包装、无需质检的物料。高价值、需质检、特殊存储条件的物料不应越库。
- ⛔ **越库不匹配目标订单直接发出**——每笔越库必须有明确的目标订单（销售单/调拨单），否则变成"盲目发货"。

## 证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| Odoo Cross-Docking 核心流程 | 🟢 | Odoo stock cross-dock 操作：收货→越库→发货 |
| 越库暂存区 + 绕过 putaway | 🟢 | WMS 标准越库实践 |
| 月台预约调度 | 🟢 | 仓库管理领域通用功能 |
| 预分配 vs 收货时匹配 | ⚪ | 越库策略分类（行业实践） |
| 越库超时回退正常入库 | ⚪ | 防呆机制（领域常识） |

## 参考

- `drp/README.md`（DRP 模块总述）
- `drp/use-cases.md`（DRP 计划创建与执行）
- `docs/design/inventory/README.md`（库存移动模型）
- `docs/design/b2b/asn-processing.md`（ASN 入站处理）
