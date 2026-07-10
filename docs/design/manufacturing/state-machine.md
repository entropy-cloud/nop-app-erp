# 制造域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 制造域有两类状态对象：**工单**（WorkOrder，复杂生命周期）与**作业卡**（JobCard，工序执行）。

## 适用对象一：工单（WorkOrder）

### 1. 状态定义

工单状态机有 10 个状态，覆盖完整生产生命周期：

| 状态 | 业务含义（等待什么） | 领料 | 完工 | 入库 |
|------|----------------------|------|------|------|
| 草稿（DRAFT） | 等待提交 | 否 | 否 | 否 |
| 已提交（SUBMITTED） | 等待审核 | 否 | 否 | 否 |
| 未开始（NOT_STARTED） | 已审核等待齐套与开工 | 否 | 否 | 否 |
| 部分齐套（STOCK_PARTIAL） | 部分子件库存不足 | 否 | 否 | 否 |
| 已齐套（STOCK_RESERVED） | 所需子件库存均已预留 | 预留 | 否 | 否 |
| 生产中（IN_PROCESS） | 正在生产，领料/报工进行中 | 进行 | 进行 | 部分 |
| 已完工（COMPLETED） | 终态：全部完工入库 | 完成 | 完成 | 完成 |
| 已停工（STOPPED） | 因故停工（待决策恢复/关闭） | 暂停 | 暂停 | — |
| 已关闭（CLOSED） | 终态：部分完工后关闭（不再继续） | 完成 | 部分 | 部分 |
| 已取消（CANCELLED） | 终态：作废 | 释放预留 | 否 | 否 |

### 2. 迁移完整性

```
草稿 (DRAFT)
  └─ 提交 → 已提交 (SUBMITTED)
              └─ 审核 → 未开始 (NOT_STARTED)
                          ├─ 齐套校验 → 已齐套 (STOCK_RESERVED) 或 部分齐套 (STOCK_PARTIAL)
                          │              └─ 强制开工 / 补料后齐套 → 生产中 (IN_PROCESS)
                          ├─ 直接开工（允许部分齐套时）→ 生产中 (IN_PROCESS)
                          ├─ 取消 → 已取消 (CANCELLED)
                          └─ 生产中 (IN_PROCESS)
                                      ├─ 全部完工入库 → 已完工 (COMPLETED)
                                      ├─ 停工 → 已停工 (STOPPED)
                                      │            ├─ 恢复 → 生产中 (IN_PROCESS)
                                      │            └─ 关闭 → 已关闭 (CLOSED)
                                      └─ 部分完工后关闭 → 已关闭 (CLOSED)
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT→SUBMITTED | 生产计划员 | 工单信息完整、BOM 已选 | 进入审核 |
| SUBMITTED→NOT_STARTED | 生产主管 | 已提交、BOM 有效、仓库有效 | 审核通过，可齐套校验 |
| NOT_STARTED→STOCK_RESERVED | 系统（齐套校验） | 所需子件库存可用量充足 | 预留子件库存 |
| NOT_STARTED→STOCK_PARTIAL | 系统（齐套校验） | 部分子件不足 | 等待补料或强制开工 |
| STOCK_RESERVED→IN_PROCESS | 生产主管 | 已齐套 | 开工，开始领料 |
| STOCK_PARTIAL→IN_PROCESS | 生产主管（强制） | 配置允许部分齐套开工 | 开工（缺料部分后续补领） |
| IN_PROCESS→COMPLETED | 系统（完工达量） | 完工数量 ≥ 工单数量 | 全部完工入库，生成成本结转凭证 |
| IN_PROCESS→STOPPED | 生产主管 | 生产中状态 | 暂停领料/报工 |
| STOPPED→IN_PROCESS | 生产主管 | 已停工、停工原因已解决 | 恢复生产 |
| STOPPED→CLOSED | 生产主管/管理员 | 已停工、确认不再继续 | 部分完工结转，剩余关闭 |
| IN_PROCESS→CLOSED | 生产主管/管理员 | 生产中、确认部分完工后不再继续 | 同上 |
| NOT_STARTED/SUBMITTED→CANCELLED | 生产计划员 | 未开工 | 释放预留 |

### 3. 终态与恢复

- 终态：`已完工（COMPLETED）`、`已关闭（CLOSED）`、`已取消（CANCELLED）`。
- 终态不可直接恢复；若需纠正，新建返工工单（关联原工单）。
- 已停工（STOPPED）可恢复到 IN_PROCESS 或关闭到 CLOSED。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 齐套校验时子件库存不足 | 进入 STOCK_PARTIAL，提示补料或强制开工 |
| 领料时可用量不足（部分齐套开工后） | 拒绝本次领料，等待补库 |
| 报工数量超过工单数量 | 拒绝（除非配置允许超产） |
| BOM 变更影响已开工工单 | 已开工工单不追溯 BOM 变更（快照）；新工单用新 BOM |
| 工作中心停机 | 触发 DowntimeEntry，影响排产（人工决策停工或等待） |
| 完工质检不合格 | quality 域反馈，触发返工（新建返工工单）或降级入库 |
| 并发领料扣减同一批次 | 乐观锁 |
| 重复报工（幂等） | 已报工工序再次提交为空操作 |

### 5. 可达性

- 从 DRAFT 主路径可达 COMPLETED；分支可达 STOPPED→CLOSED、CANCELLED。
- STOCK_PARTIAL 是中间态，可通过补料转到 STOCK_RESERVED 或强制开工。
- 无不可达状态，无死锁。终态无出边。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| 提交（DRAFT→SUBMITTED） | 生产计划员 |
| 审核（SUBMITTED→NOT_STARTED） | 生产主管 |
| 开工（→IN_PROCESS） | 生产主管 |
| 停工/恢复 | 生产主管 |
| 关闭（→CLOSED） | 生产主管/管理员（因影响成本结转） |
| 取消 | 生产计划员 |
| 报工 | 作业员（通过 JobCard） |

危险操作：
- **关闭工单**：需管理员权限，因部分完工结转影响成本。
- **强制部分齐套开工**：需生产主管权限，有缺料风险。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 领料/完工写库存 | 通过 `IErpInvStockMoveBiz` 生成移动单 |
| 成本结转凭证 | 财务域监听工单完工 |
| 完工质检 | quality 域监听工单完工（若 BOM 配置检验要求） |
| 工作中心停机 | maintenance 域的设备停机影响工作中心可用性 |

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| DRAFT | 是 | assigned（计划员） |
| SUBMITTED | 是 | pool/assigned（生产主管审核） |
| NOT_STARTED | 是 | confirm（待齐套校验后开工） |
| STOCK_PARTIAL | 是 | assigned（计划员）—— 缺料待补 |
| IN_PROCESS | 是 | monitor（生产进度监控） |
| STOPPED | 是 | assigned（生产主管）—— 停工待决策 |
| COMPLETED/CLOSED/CANCELLED | 否 | — |

避免"部分齐套工单长期滞留"：STOCK_PARTIAL 产生 TODO 提醒补料或强制开工。

### 9. 场景演练

#### 场景 A：正常生产 happy path

1. 计划员创建工单（DRAFT）→ 提交（SUBMITTED）→ 主管审核（NOT_STARTED）。
2. 齐套校验通过 → STOCK_RESERVED。
3. 开工 → IN_PROCESS → 领料（出库移动单）→ 报工（JobCard）→ 完工（入库移动单）。
4. 完工达量 → COMPLETED → 成本结转凭证。

#### 场景 B：部分齐套开工

1. 工单 NOT_STARTED，齐套校验：某子件缺 10% → STOCK_PARTIAL。
2. 计划员决策强制开工（配置允许）→ IN_PROCESS。
3. 缺料部分后续补领，完工入库。

#### 场景 C：停工与关闭

1. 工单 IN_PROCESS，因设备故障停工 → STOPPED。
2. 维修后恢复 → IN_PROCESS 继续生产。
3. 或决策不再继续 → CLOSED（部分完工结转成本）。

#### 场景 D：完工质检不合格返工

1. 工单完工 → COMPLETED → 触发完工质检。
2. 质检不合格 → 新建返工工单（关联原工单）→ 返工流程。

### 10. 与设计文档一致性

- BOM 与工艺模型见 `bom-and-routing.md`。
- 状态码归 `model/app-erp-manufacturing.orm.xml`。
- 领料/完工的库存协作见 `inventory/cross-domain.md`。

### 质检对工单状态的约束声明

质检判定（quality 域）直接影响工单从 INSPECTING 到终态的迁移，双方必须显式声明：

| 质检判定 | 对工单状态的影响 | 约束声明位置 |
|----------|------------------|------------|
| ACCEPTED（合格） | 工单可从 INSPECTING → COMPLETED | 本文 + `quality/README.md` |
| CONDITIONAL（让步接收） | 工单可从 INSPECTING → COMPLETED（附让步记录） | 本文 + `quality/README.md` |
| REJECTED（不合格） | 工单停留在 INSPECTING，触发返工工单 | 本文 + `quality/README.md` |

> 质检触发规则见 `quality/README.md` "质检对制造域的约束声明"节。双方文档都显式声明此约束。

### 实现偏离补注（plan 2026-07-02-2237-1）

> 本节记录 plan 2237-1 落地时与上方状态机设计文档的实现偏离，供 2.4 quality 落地后裁决回填。

- **INSPECTING 态字典缺失 → config-gated 钩子替代**：上方 §质检约束声明引用工单 INSPECTING 态，但 `erp-mfg/work-order-status` 字典（10 态）**无 INSPECTING 态**。plan 2237-1 以 config-gated 完工门控钩子替代（不加 ORM 字典态）：`reportCompletion` 时若 `ErpMfgBom.inspectionRequired=true` 且 `erp-mfg.inspection-gate-enabled=true`（默认 false）且完工达量，抛 `ERR_INSPECTION_REQUIRED` 拒绝 COMPLETED，工单保持 IN_PROCESS 待质检结果。**触发条件**：2.4 quality 落地后 flip config=true 接线，并裁决是否需向字典补 INSPECTING 态。
- **领料出库 moveType 用 OUTGOING(20) 而非 MANUFACTURING(40)**：库存域 `StockMoveBookkeeper.bookCompletion` 按 moveType 决定方向——MANUFACTURING(40) 视为入库（加库存），OUTGOING(20) 才扣减余额。故领料出库移动单用 `MOVE_TYPE_OUTGOING_ISSUE(20)`（relatedBillType=ERP_MFG_ISSUE）；完工入库移动单用 MANUFACTURING(40)（relatedBillType=ERP_MFG_WORK_ORDER）。计划原写「moveType=MANUFACTURE」为对库存域方向语义的假设偏差，已按库存域实际实现修正。
- **齐套校验只读不写预留**：`checkAvailability`（NOT_STARTED→STOCK_RESERVED/STOCK_PARTIAL）仅读 `ErpInvStockBalance.availableQuantity` 置状态，不写库存预留记录（实际扣减由开工后领料出库移动单 DONE 完成，对齐 `inventory/cross-domain.md §余量校验规则`）。故 `cancel`（→CANCELLED）为纯状态迁移，无预留记录需释放。
- **制造费用 overheadCost=0**：工作中心仅有单一 `hourlyRate`（无独立制造费率分列，同 plan 1538-2），故报工工时成本（`durationMins/60 × hourlyRate`）统一计入 `WorkOrder.laborCost`，`overheadCost`=0。**触发条件**：工作中心费率拆分后细化。
- **完工成本结转凭证已实现**：完工入库移动单生成（产成品入 destWarehouse）+ 产成品存货估值过账凭证（MANUFACTURING_RECEIPT 凭证类型：Dr 产成品存货 1401 / Cr WIP 1411）已由 plan 2026-07-10-1100-5 落地。完工入库移动单 posted=true。领料出库 GL 过账（MANUFACTURING_ISSUE：Dr WIP 1411 / Cr 原材料存货 1401）同步落地，领料单 posted=true。
- **工时/实领数量列类型修正补注**：`ErpMfgJobCardTimeLog.durationMins`/`setupMins`/`runMins`/`hourlyRate`（VARCHAR→DECIMAL，对齐 domain）+ `ErpMfgMaterialIssueLine.issuedQuantity`（BOOLEAN→DECIMAL，对齐 domain）已修正，解除报工成本归集数值计算阻塞（同 plan 1538-2 工时/费率修正范式）。

---

## 适用对象二：作业卡（JobCard）

> **三层分解关系**：WorkOrder（工单级，产品级生产指令）→ OperationOrder（工序级排产，由 APS 模块创建，见 `aps/README.md`）→ JobCard（执行级，实际作业）。JobCard 可按 OperationOrder 的排程结果创建（若 APS 模块启用），否则按工单工序直接创建。

作业卡状态机 8 态：

```
待开始 (OPEN)
  ├─ 开始作业 → 作业中 (WORK_IN_PROGRESS)
  │              ├─ 部分转序 → 部分转序 (PARTIALLY_TRANSFERRED)
  │              ├─ 转序 → 已转序 (MATERIAL_TRANSFERRED)
  │              ├─ 暂停 → 暂停 (ON_HOLD)
  │              │           └─ 恢复 → 作业中
  │              ├─ 提交 → 已提交 (SUBMITTED)
  │              │           └─ 完成 → 已完成 (COMPLETED)
  │              └─ 取消 → 已取消 (CANCELLED)
  └─ （工单取消时联动取消）
```

作业卡承载工时记录（JobCardTimeLog）：作业员记录实际工时，用于成本核算。

### APS 排程来源建卡（plan 2026-07-05-0427-3）

> **触发条件**：APS 排产引擎（plan 0831-1）已落地 + WorkOrder/JobCard 状态机（plan 2237-1）已落地，本节补注排程产物 → 工序卡的自动消费入口。

- **入口**：`ErpMfgWorkOrder__generateJobCardsFromSchedule(workOrderId)` @BizMutation。读取该工单关联的、已排程（`ErpApsOperationOrder.status=PLANNED`，`plannedStartT/plannedEndT` 非空）的工序，按工序生成 JobCard（一工序一卡），JobCard 初始状态 = `OPEN`（对齐本状态机入口），`plannedQuantity` = 工单计划生产量，`workcenterId` 来自 APS 排程。
- **来源标记**：建卡时回写 `JobCard.sourceScheduleId` = 对应 `ErpApsOperationOrder.id`（弱参照），并标记 `WorkOrder.sourceOrderType=APS_SCHEDULE` + `WorkOrder.sourceScheduleId`。`sourceScheduleId`/`sourceOrderType` 为加性可选列/字典项（null/空 = 非排程生成，既有手工建卡流程不受影响）。
- **幂等**：默认重复调用抛 `ERR_JOB_CARDS_ALREADY_GENERATED`；config `erp-mfg.jobcard-incremental-rebuild=true` 时仅补建缺失工序卡（已存在不重建不删，避免破坏已开工卡状态）。
- **状态门**：仅允许在已审核且非终态的工单上建卡（`NOT_STARTED`/`STOCK_RESERVED`/`STOCK_PARTIAL`/`IN_PROCESS`/`STOPPED`）；`DRAFT`/`SUBMITTED`（未审核）/`COMPLETED`/`CLOSED`/`CANCELLED`（终态）拒绝（`ERR_WORK_ORDER_STATUS_NOT_ALLOWED_FOR_JOB_CARD_GEN`）。
- **自动模式**：config-gated 批量入口 `ErpMfgWorkOrder__findWorkOrdersPendingJobCards` @BizQuery + `ErpMfgWorkOrder__generatePendingJobCards` @BizMutation，由 nop-job 双层门控（`erp-mfg.jobcard-auto-generate-cron` + `erp-mfg.jobcard-auto-generate-on-schedule`，默认均关）定时触发；手动入口为主、自动为辅。
- **跨域读 APS**：复用 `IErpApsLoadSourceProvider` SPI（plan 0306-2 范式，声明于 mfg-dao、实现于 aps-service）；APS 模块缺失时 SPI 收集到空 list，`generateJobCardsFromSchedule` 抛 `ERR_NO_SCHEDULED_OPERATIONS`（行为降级）。
- **Non-Goal（后继触发条件）**：事件驱动实时自动（APS 排程完成广播事件→制造域订阅即时建卡）需事件总线基础设施，本期手动 + 轮询；JobCard 重排/同步（APS 重排后已生成卡时间自动更新）需冲突解决（已开工卡是否可改时间），本期仅首次生成。

作业卡的其他维度（异常/角色/TODO）与工单类似，不重复展开；审查时同样使用提示词。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 齐套校验（STOCK_RESERVED/STOCK_PARTIAL）是否覆盖所有子件。
- 部分齐套强制开工的权限与风险控制。
- 停工/关闭的成本结转是否完整。
- 完工质检不合格的返工路径（新建返工工单）。
- BOM 变更对已开工工单的影响（快照原则）。
