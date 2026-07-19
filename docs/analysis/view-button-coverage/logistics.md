# Logistics 视图按钮需求覆盖分析

## 分析范围

| 实体 | 分类 | 说明 |
|------|------|------|
| ErpLogShipment | Custom | CRUD 基线满足但缺失全部域专用按钮；有状态机无 workflow 按钮 |
| ErpLogCarrier | CRUD | 纯配置主数据，CRUD 基线完整 |
| ErpLogCarrierConfig | CRUD | 配置子实体，CRUD 基线完整 |
| ErpLogShipmentLine | CRUD | 明细子实体，CRUD 基线完整 |
| ErpLogShipmentParcel | CRUD | 包裹子实体，CRUD 基线完整 |
| ErpLogShipmentLog | CRUD | 审计日志，CRUD 基线完整 |
| ErpLogDeliveryWindow | CRUD | 配置实体，CRUD 基线完整 |

注：RateQuote 是设计文档中比价面板的业务概念，**无独立 view.xml 页面文件**（比价面板为弹窗/侧栏，不映射为独立 ORM 实体页面）；"CarrierGatewayLog" 即 ErpLogShipmentLog（网关交互日志）。

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：所有实体期望 `add-button` / `batch-delete-button` / `row-view-button` / `row-update-button` / `row-delete-button`。
2. **ErpLogShipment 域专用按钮**（`ui-patterns.md`）：
   - 编辑页工具栏：`[保存草稿] [确认发运] [取消] [获取面单] [打印面单]`（§编辑页结构，line 62）
   - 状态驱动按钮变化（line 119）：DRAFT 显示 [保存草稿][确认发运]；ADVISED 显示 [取消][重试网关]（网关异常时）；DISPATCHED/IN_TRANSIT 显示 [获取面单][打印面单]
   - 比价面板入口（line 109）：`[比价]`
   - 子表工具栏 `[从出库单导入]`（line 84）
3. **状态机**（`state-machine.md`）：发运单有 DRAFT → ADVISED → DISPATCHED → IN_TRANSIT → DELIVERED → CANCELLED 六态。METHODOLOGY §1.2 中 `row-cancel-button`（作废/取消）适用于 CANCELLABLE 实体。
4. **ErpLogCarrierConfig**（`ui-patterns.md`）：凭证管理页有 `[测试连通性]`、`[重置凭证]`（line 259）。

## 逐实体分析

### ErpLogShipment — Custom

- **期望按钮**：
  - CRUD 基线：`add-button`, `batch-delete-button`, `row-view-button`, `row-update-button`, `row-delete-button`
  - 域专用（列表行或编辑页工具栏）：`row-confirm-shipment-button`（确认发运，DRAFT→ADVISED）, `row-cancel-button`（取消 →CANCELLED）, `row-retry-gateway-button`（重试网关）, `row-get-label-button`（获取面单）, `row-print-label-button`（打印面单）, `row-compare-price-button`（比价）
  - 编辑页子表：`[从出库单导入]`
- **实际按钮**：CRUD 基线 5 个，无任何域专用按钮
- **差距**：
  - `row-confirm-shipment-button`: **missing (blocker)** — ui-patterns.md §编辑页结构 line 62 明确列出 [确认发运]；状态机 DRAFT→ADVISED 是发运单核心业务操作
  - `row-cancel-button`: **missing (blocker)** — ui-patterns.md line 62 明确列出 [取消]；状态机定义 DRAFT/ADVISED/IN_TRANSIT 均可 → CANCELLED；METHODOLOGY §1.2 要求可作废实体有此按钮
  - `row-retry-gateway-button`: **missing (major)** — ui-patterns.md line 119 "ADVISED 显示[取消][重试网关]（网关异常时）"
  - `row-get-label-button`: **missing (major)** — ui-patterns.md line 62 和 line 119 均提及 [获取面单]
  - `row-print-label-button`: **missing (info)** — per METHODOLOGY §2，[打印] 类按钮为 info 级差距
  - `row-compare-price-button`: **missing (info)** — ui-patterns.md §比价面板 line 305 入口来自发运单编辑页 [比价]；属弹窗入口按钮，非核心流转
  - `[从出库单导入]`: **missing (info)** — ui-patterns.md line 84 子表工具栏按钮，属增强功能
  - `row-delete-button` 出现在所有行（包括已发运），但状态机中 ADVISED/DISPATCHED/IN_TRANSIT 应禁止删除、走取消流程
- **判定**：**blocker**。核心状态流转按钮全部缺失，edit 页面无任何 toolbar actions，发运单基本业务操作无法在页面执行。

### ErpLogCarrier — CRUD

- **期望按钮**：CRUD 基线
- **实际按钮**：CRUD 基线完整
- **差距**：无
- **判定**：**clean**

### ErpLogCarrierConfig — CRUD

- **期望按钮**：CRUD 基线 + `[测试连通性]`、`[重置凭证]`（编辑页内动作，非 list 行 action）
- **实际按钮**：CRUD 基线完整；edit/view page 无额外 actions
- **差距**：
  - `[测试连通性]`: **missing (info)** — ui-patterns.md §承运商配置 line 259 操作区提及；属配置验证功能增强
  - `[重置凭证]`: **missing (info)** — 同上 §承运商配置 line 259；凭证管理增强功能
- **判定**：**clean**（info 级差距不改变分类）

### ErpLogShipmentLine — CRUD

- **期望按钮**：CRUD 基线（子表角色，主要操作在 Shipment edit 页内联）
- **实际按钮**：CRUD 基线完整
- **差距**：无
- **判定**：**clean**

### ErpLogShipmentParcel — CRUD

- **期望按钮**：CRUD 基线（子表角色，主要操作在 Shipment edit 页内联）
- **实际按钮**：CRUD 基线完整
- **差距**：无
- **判定**：**clean**

### ErpLogShipmentLog — CRUD

- **期望按钮**：CRUD 基线（审计日志只读）
- **实际按钮**：CRUD 基线完整
- **差距**：无
- **判定**：**clean**

### ErpLogDeliveryWindow — CRUD

- **期望按钮**：CRUD 基线（配置实体）
- **实际按钮**：CRUD 基线完整
- **差距**：无
- **判定**：**clean**

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| Custom | ErpLogShipment | 7 | blocker | 核心发运操作按钮全部缺失 |
| CRUD | ErpLogCarrier | 0 | clean | CRUD 基线完整 |
| CRUD | ErpLogCarrierConfig | 2 | info | 编辑页缺 [测试连通性][重置凭证] 增强按钮 |
| CRUD | ErpLogShipmentLine | 0 | clean | 子表 CRUD 基线完整 |
| CRUD | ErpLogShipmentParcel | 0 | clean | 子表 CRUD 基线完整 |
| CRUD | ErpLogShipmentLog | 0 | clean | 审计日志 CRUD 基线完整 |
| CRUD | ErpLogDeliveryWindow | 0 | clean | 配置实体 CRUD 基线完整 |

### 总评

- 总实体数：7
- 无差距实体：4（57%）
- Blocker 差距：2（均在 ErpLogShipment：确认发运、取消）
- Major 差距：2（均在 ErpLogShipment：重试网关、获取面单）
- Minor 差距：0
- Info 差距：4（ErpLogShipment: 打印面单, 比价, 从出库单导入；ErpLogCarrierConfig: 测试连通性, 重置凭证）

**关键发现**：发运单（ErpLogShipment）作为 logistics 域的核心业务实体，view.xml 仅提供了标准的 CRUD 操作，完全没有实现 `ui-patterns.md` 和 `state-machine.md` 定义的任何域专用状态流转按钮（确认发运、取消、重试网关、获取面单、打印面单、比价）。edit 页面（simple）没有任何 toolbar actions。其他 6 个配置/审计/子表实体的 CRUD 基线均完整。

**建议**：ErpLogShipment 的主列表页需要增加 `row-confirm-shipment-button`（DRAFT→ADVISED）和 `row-cancel-button`（→CANCELLED）作为优先补齐项。编辑页（edit/update/view）需要增加状态相关的 toolbar actions（确认发运/取消/重试网关/获取面单）。
