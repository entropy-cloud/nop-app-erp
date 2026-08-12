# 库存审计快照与周期盘点设计（Inventory Audit Snapshot & Cycle Counting）

## 定位

本文基于 OpenBoxes（开源 WMS）调研，设计 nop-app-erp inventory 域的**库存审计快照（Snapshot）与周期盘点（Cycle Counting）深化**。**当前阶段只产出设计与分析，不进入编码状态**（2026-08-12 用户指示）；既有 3 层模型（Move→Ledger→Balance）保持不变。

## 来源与背景

- 参考报告：`docs/analysis/erp-survey/2026-08-12-0000-openboxes.md`（InventorySnapshot/InventoryAudit/CycleCountTask + 补货-拣货-上架-调拨闭环）。
- 现状基线：`docs/design/inventory/` —— 3 层模型（Move→Ledger→Balance）+ 盘点单（StockTake）+ 批次/序列追溯（`trace-chain.md`）+ 预留量机制。
- 缺口：盘点设计（StockTake）为一次性差异盘点，无**周期化任务视图**与**期末对账快照语义**；盘点与流水/余额的关系无审计快照对照。

## 现状 vs OpenBoxes 对照

| 维度 | nop 现状 | OpenBoxes | 差距 |
|------|---------|-----------|------|
| 盘点 | StockTake（差异盘点，一次性） | CycleCountTask（周期盘点任务） | 周期化任务视图缺失 |
| 审计快照 | 余额由流水驱动（无快照表） | InventorySnapshot + InventoryAudit | 期末对账快照语义缺失 |
| 批次/序列 | 全链路追溯（`trace-chain.md`） | lot/serial 出入库/调拨/盘点贯穿 | 已具备（对照确认） |
| 补货闭环 | 预留量 + DRP 建议 | replenishment/picklist/putaway/stockTransfer | 部分具备（拣货/上架为 WMS 特征，非本期） |

## 设计要点

### 1. 审计快照语义（Snapshot）

- **目标**：在不改变「余额由流水驱动」原则的前提下，提供**时点快照**供期末对账/审计比对（回答「X 时刻账面库存是什么」）。
- **设计**：
  - 快照 = 派生视图，不新增常驻快照表（避免双写与漂移）：经「期初余额 + 截至时点流水汇总」确定性派生（对齐既有 3 层模型）。
  - 快照查询接口（BizQuery）：`getInventorySnapshot(warehouseId, materialIds, asOfDate)` 返回余额行（数量 + 成本 + 库位维度）。
  - 与期末结账（finance 期间）对齐：期间关闭时对账 = 快照 vs 期间流水汇总一致性校验（`domain-design-guidelines.md` 对账机制扩展）。
- **不新增 ORM 实体**（设计阶段）；若未来需要高频快照物化，触发条件=大库存量 + 查询 P95 超标（物化表 ORM 变更已获授权，`erp-enhancement-roadmap.md` §8.1）。

### 2. 周期盘点（Cycle Counting）

- **目标**：把一次性 StockTake 扩展为**可排期的周期盘点任务**（按 ABC 分类/频次轮转，而非全量盘点）。
- **设计**：
  - `CycleCountTask` 视图：盘点批次（物料子集 × 频次 × 周期）+ 任务生成（按 ABC 分类与上次盘点时间）+ 执行入口（复用既有 StockTake 差异处理链）。
  - 差异处理：差异移动单（盘盈/盘亏）走既有移动单流程（`inventory/state-machine.md`）+ 过账（既有盘点差异过账链路）。
  - 任务状态机：DRAFT → SCHEDULED → IN_PROGRESS → COMPLETED / CANCELLED（对照既有 inventory 作业单状态机形态）。
- **实现触发条件**（roadmap E3.2 门控）：真实周期盘点业务需求 + ABC 分类数据就绪；当前一次性 StockTake 保持。

### 3. 对账机制整合（对照 Snapshot 审计语义）

- 每日对账（既有 §6.3 对账机制）增加「账面余额 = 快照派生值」一致性校验项；期末与 finance 期间关闭联动（`period-close.md`）。

## 落地策略（分阶段）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 设计 | 本文档（快照语义 + 周期盘点 + 对账整合） | ✅ 已完成（本批次） |
| 实现（快照查询） | `getInventorySnapshot` BizQuery + 对账校验项 | todo（roadmap E3.2，plan-first；零 ORM 变更） |
| 实现（周期盘点） | CycleCountTask 视图/状态机（复用 StockTake 链） | todo（触发条件驱动；ORM 变更已获授权，`erp-enhancement-roadmap.md` §8.1） |

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|----------|
| AP-1 | 引入常驻快照表与流水双写 | 快照=派生视图（期初+流水汇总），物化仅触发条件驱动 |
| AP-2 | 周期盘点另建差异处理链 | 复用既有 StockTake 差异 → 移动单 → 过账链 |
| AP-3 | 盘点与余额/流水脱节 | 盘点差异始终经移动单，余额由流水驱动不变 |
| AP-4 | 全量盘点替代 ABC 轮转 | 按分类/频次排期，控制盘点工作量 |
| AP-5 | 快照口径与 finance 期间不一致 | 快照 asOfDate 对齐 businessDate/期间语义 |

## 相关文档

- `docs/analysis/erp-survey/2026-08-12-0000-openboxes.md` — 参考报告
- `docs/design/inventory/`（state-machine/trace-chain/README）— 既有 inventory 设计
- `docs/design/finance/period-close.md` — 期末对账联动
- `docs/backlog/erp-enhancement-roadmap.md` — 本主题 roadmap