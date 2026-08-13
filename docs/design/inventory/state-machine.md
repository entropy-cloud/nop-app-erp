# 库存域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。

## 适用对象

本状态机适用于**库存移动单**（StockMove）。盘点单状态机见文末单独一节。主数据实体（物料/仓库等）的"启用/停用"不是状态机，见 `master-data/README.md`。

## 1. 状态定义

每个状态表达"等待什么"，不是"做什么"。

| 状态 | 业务含义（等待什么） | 占预留量 | 影响余额 | 影响流水 |
|------|----------------------|----------|----------|----------|
| 草稿（DRAFT） | 等待提交确认 | 否 | 否 | 否 |
| 已确认（CONFIRMED） | 等待实际执行搬动 | 是（出库类） | 否 | 否 |
| 已完成（DONE） | 已执行完成，等待后续过账 | 否（已释放） | 是 | 是（写一条流水） |
| 已取消（CANCELLED） | 终态：作废 | 否（已释放） | 否 | 否 |

持久化状态码字典以 `model/app-erp-inventory.orm.xml` 为准；本文只描述业务语义。

## 2. 迁移完整性

```
草稿 (DRAFT)
  ├─ 提交确认 → 已确认 (CONFIRMED)
  │              ├─ 执行完成 → 已完成 (DONE)
  │              │              └─ 冲销 → 生成反向移动单（新 DRAFT，数量取负）
  │              └─ 取消 → 已取消 (CANCELLED)
  └─ 取消 → 已取消 (CANCELLED)
```

每条迁移的触发、前置、结果：

| 迁移 | 触发人/系统 | 前置条件 | 结果 |
|------|-------------|----------|------|
| DRAFT → CONFIRMED | 业务单据审核 / 库管员 | 来源/目的库位有效、物料/SKU 有效、出库类需可用量充足 | 出库类增加预留量 |
| CONFIRMED → DONE | 系统（业务单据审核联动 / 库管员二次确认） | 已确认状态 | 写一条不可变流水、更新余额、释放预留、发存货过账事件 |
| CONFIRMED → CANCELLED | 库管员 | 已确认状态 | 释放预留量，不影响余额与流水 |
| DRAFT → CANCELLED | 库管员 | 草稿状态 | 直接作废 |
| DONE → 冲销（生成反向单） | 库管员 | 已完成状态 | 不改原单；生成反向移动单（新 DRAFT，数量取负），走正常流程 |

**入库类 vs 出库类的预留量差异**：

| 移动类型 | 预留量影响 |
|----------|------------|
| 入库（incoming） | 不占预留量（入库增加库存） |
| 出库（outgoing） | 已确认状态占用预留量，已完成时释放并扣减现有量 |
| 内部调拨（internal） | 来源库位占预留量，目的库位不占 |

## 3. 终态与恢复

- **终态**：`已取消（CANCELLED）` 与 `已完成（DONE）` 均无后续出边（`DONE` 的"冲销"是生成新单，不是状态迁移）。
- **已完成不可直接修改**：已完成的移动单已写入不可变流水，不可反审核回退到已确认。纠错路径是生成反向冲销移动单（新 DRAFT）。
- **已取消不可恢复**：取消是终态，需重新创建移动单。
- **归档与活跃区分**：已完成/已取消的移动单作为历史归档查询，不参与活跃待办。

## 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 出库可用量不足 | DRAFT → CONFIRMED 时拒绝（抛业务异常），整个业务单据审核回滚 |
| 批次/序列号缺失 | 启用批次/序列号的物料，移动单必须指定批次/序列号；缺失拒绝确认 |
| 批次过期 | 出库时校验批次是否在有效期；过期批次拒绝出库（可配置放行） |
| 序列号已售 | 出库时校验序列号状态；已售序列号拒绝再次出库 |
| 并发扣减同一批次的可用量 | 乐观锁 + 扣减失败重试；重试仍失败则拒绝确认 |
| 并发首次入库/初始化同维度 | DB 唯一约束 `UK_INV_STOCK_BALANCE_NATURAL` 兜底；`StockMoveBookkeeper.updateBalanceWithRetry` 的 SAVING 分支 flush 后捕获 ConstraintViolation → evict + 按自然键 reload 已存在行 + 转更新路径，重试上限 `erp-inv.concurrent-deduct-max-retry`（默认 5）。SQL NULL 语义限制：含 NULL 列的键不参与 UNIQUE 比较，仅由应用层 retry-on-conflict 兜底 |
| 冲销反向单的可用量不足 | 冲销本质是反向移动（入库变出库/出库变入库），同样校验可用量 |
| 重复触发（业务单据重复审核） | 幂等：同一业务单据对同一移动单的触发生成需幂等，已生成则不重复生成 |

负库存配置：全局配置 `erp-inv.allow-negative-stock`（默认 false），开启时跳过可用量校验，允许余额为负（特殊业务场景如先发货后入库）。

批次效期拦截实现注记（RC-R1.20 / P1-RC-031，UC-INV-06 ④，2026-08-08 落地）：

- **拦截点**：`ErpInvStockMoveProcessor.validateAvailable`（`doConfirm` 内、`applyReservation` 之前）首行调用 `validateBatchExpiry`——拒绝路径不进入 `applyReservation`，`reservedQuantity`/余额不变（A4.2.79 验收一致性）。
- **触发条件**：per-line 带 batchNo + 物料 `isBatchManaged=true` + 批次 `expiryDate < 当前日期` → 抛 `ERR_BATCH_EXPIRED`（`erp.err.inv.batch-expired`），确认失败。
- **null 语义**（A4.2.78 设计输入）：`expiryDate == null` → 跳过拦截（视为永不过期）。
- **可配置放行**（本条「可配置放行」落地）：`erp-inv.batch-expiry-check-enabled`（默认 true），false 时守卫整体放行。
- **移动单类型范围**：仅出库（`OUTGOING`）与内部转移（`INTERNAL_TRANSFER`，即 `reservesOnConfirm` 命中类型）拦截；INCOMING 类移动单（采购入库/退货入库）不入拦截（收过期批次属质检域职责）。
- **负库存不豁免**：效期守卫为合规门禁，先于 `isNegativeStockAllowed()` 短路执行，`allow-negative-stock=true` 不豁免过期拦截。

## 5. 可达性

- **从 DRAFT 可达**：CONFIRMED、DONE、CANCELLED 全部可达。
- **无不可达状态**：每个状态都有入边。
- **无死锁/无限循环**：DONE 与 CANCELLED 是终态无出边；DRAFT→CONFIRMED→DONE 是有向无环路径。冲销生成的反向单是独立新流程，不构成原单的循环。
- **合法循环的退出条件**：无循环设计（冲销是新建独立单，非状态回退）。

## 6. 角色与权限

每个迁移绑定执行角色：

| 迁移 | 执行角色 |
|------|----------|
| 提交确认（DRAFT→CONFIRMED） | 业务单据审核人（采购员/销售员审核触发）/ 库管员（独立创建的移动单） |
| 执行完成（CONFIRMED→DONE） | 系统（业务单据审核联动）/ 库管员（需二次确认时） |
| 取消 | 库管员 |
| 冲销 | 库管员（需财务影响确认，因冲销会触发存货过账冲销） |

危险操作控制：
- **冲销已完成移动单**：需库管员权限，且因影响存货凭证，建议二次确认。
- **负库存放行**：仅管理员可配置，不放行给普通库管员。
- 角色名与状态名同源业务词汇（库管员/采购员/销售员/财务员），见 `roles-and-permissions.md`。

## 7. 外部依赖

库存移动单本身不直接依赖外部系统。但触发它的业务单据可能涉及外部集成：

| 外部场景 | 内部状态映射 |
|----------|--------------|
| 业务单据（采购入库/销售出库）来自外部系统触发 | 外部触发转为内部 `DRAFT` 或直接 `CONFIRMED`（取决于配置），不直接使用外部状态值 |
| 存货过账事件发布给财务域 | 移动单 DONE 后发布事件；财务域订阅，失败不影响移动单终态 |

外部触发渠道：
- 业务单据审核联动（主要渠道，内部 BizModel 调用）。
- 库管员手工创建（独立移动单，如盘点调整、其他出入库）。

## 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 | 说明 |
|------|---------------|-----------|------|
| DRAFT | 是（若需人工确认） | assigned（分配给库管员） | 独立创建的移动单待库管员确认 |
| CONFIRMED | 是（若需二次确认执行） | confirm（待执行确认） | 已确认待实际执行（业务联动时通常立即 DONE） |
| DONE | 否 | — | 终态，归档 |
| CANCELLED | 否 | — | 终态，归档 |

业务单据联动的移动单通常自动从 DRAFT 推进到 DONE，不产生人工 TODO；只有独立创建的移动单（盘点、其他出入库）才产生库管员待办。避免"状态存在但无 TODO 导致单据沉没"。

## 9. 场景演练

### 场景 A：采购入库（happy path）

1. 采购入库单审核通过 → 自动创建移动单（incoming）→ DRAFT。
2. 系统联动立即推进 → CONFIRMED → DONE（写流水、增余额、发过账事件）。
3. 财务域异步收到事件 → 生成存货估值凭证。

### 场景 B：销售出库（可用量不足异常）

1. 销售出库单审核通过 → 尝试创建移动单（outgoing）。
2. DRAFT → CONFIRMED 时校验可用量不足 → 拒绝（抛异常）。
3. 整个销售出库单审核回滚，移动单未生成。

### 场景 C：已完成冲销

1. 库管员发现某已完成入库移动单录错（数量多）。
2. 生成冲销反向单（outgoing，数量取负）→ DRAFT。
3. 反向单走 DRAFT → CONFIRMED → DONE → 余额回退、生成反向存货凭证。

## 10. 与设计文档一致性

- 每个状态在 `inventory/README.md` 三层模型中有对应业务含义。
- 状态码（DRAFT/CONFIRMED/DONE/CANCELLED）的持久化值归 `model/app-erp-inventory.orm.xml`，本文不重复。
- 跨域协作规则（业务单据触发、过账事件）见 `inventory/cross-domain.md`。
- 全局流程编排见 `flow-overview.md` L2 状态映射。

## 盘点单状态机（独立）

盘点单有独立状态机，与移动单不同：

```
草稿 (DRAFT)
  ├─ 开始盘点 → 盘点中 (COUNTING)
  │              ├─ 完成盘点 → 已完成（DONE）
  │              └─ 取消 → 已取消 (CANCELLED)
  └─ 取消 → 已取消 (CANCELLED)
```

> **标签映射注记（COUNTING ↔ CONFIRMED，doc label drift，行为一致）**：上图标签「盘点中 (COUNTING)」为业务语义描述；实际持久化 dict `erp-inv/move-status`（`model/app-erp-inventory.orm.xml`）**无 COUNTING 值**，盘点单复用移动单字典，`startTake` 的目标态实际写入 `CONFIRMED`（`ErpInvStockTakeBizModel.startTake` → `setDocStatus(CONFIRMED)`）。即「盘点中 (COUNTING)」标签 ↔ 实际 dict/code 值 `CONFIRMED` 存在标签/命名漂移，**行为一致**（owner doc 的「盘点中」= 代码的 CONFIRMED）。实体级状态机 Bean `ErpInvStockTakeStateMachine` 按既有 writer 建模（`startTakeTargetStatus()=CONFIRMED`），保留 CONFIRMED 行为不改 dict/绑。分类 = `doc label drift`（标签漂移，非行为漂移）。

- **差异调整移动单的自动生成 = Deferred（owner doc 语义对齐）**：`ErpInvStockTakeBizModel.completeTake` 当前仅将盘点单置为 DONE（源态守卫 CONFIRMED → `DOC_STATUS_DONE` + `updateEntity`），**无任何 `StockTakeLine.qtyActual` vs `StockBalance.totalQuantity` 比对、无 `IErpInvStockMoveBiz.generateMove` 调用**——即不自动生成盘盈/盘亏移动单。差异调整当前经库管员**手工 `generateMove`** 处置（创建新 DRAFT 移动单，正数盘盈/负数盘亏），走下方移动单状态机流程。盘点单 DONE 后无悬挂数据（差异未自动入账但不阻塞盘点闭环）。
- **Successor 触发条件**：盘点闭环自动化需求落地时，在 `completeTake` 内自动比对 `qtyActual` vs `totalQuantity` → 经 `IErpInvStockMoveBiz.generateMove` Facade 生成差异移动单（实现路径与现有 Facade 可复用）。
- 盘点完成的差异**不直接改余额**，而是经移动单（正数盘盈/负数盘亏）走移动单状态机流程才会影响余额。这一原则保留；当前唯一的偏差是「自动生成」降级为「手工入口」（Deferred）。
- 这种设计保证所有余额变动都通过移动单流水可追溯，盘点只是发现差异的入口（差异入口当前为手工 `generateMove`，自动比对留 successor）。

## 拣货单生命周期（Deferred）

> owner doc 语义对齐。拣货执行由 WMS successor 承载，本期无独立状态机章节，仅作预留与 Deferred 标注。

- **dict 与代码现状**：dict `erp-inv/picking-status`（`module-inventory/model/app-erp-inventory.orm.xml`，4 值：PENDING/PICKING/PICKED/CANCELLED）保留为预留语义入口。`ErpInvPickingOrderBizModel` 为 `extends CrudBizModel<ErpInvPickingOrder>` 的 CRUD 桩（零 `setStatus`/`setDocStatus` writer）。
- **可达与不可达**：
  - `PENDING`：可达——由 codegen 默认值承载（新建拣货单初值）。
  - `CANCELLED`：可达——经 `useLogicalDelete` 逻辑删除承载。
  - `PICKING` / `PICKED`：**预留死状态（零 writer，不可达）**。全 `module-inventory/erp-inv-service` grep `PICKING_STATUS` / `setDocStatus.*PICKING` 零业务命中。dict 值保留不删除（对齐「保留 dict 死状态为预留」先例）。
- **主路径可用性**：CRUD 桩为 PENDING 创建/查询/逻辑删除主路径可用，不破坏既有行为。
- **Successor 触发条件**：WMS（仓储管理系统）上线时，实现 `startPicking`/`completePicking`/`cancelPicking` BizMutation（迁移 `PENDING→PICKING→PICKED` + `PENDING/PICKING→CANCELLED`）+ owner doc 新增「拣货单状态机」独立章节。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 是否有未覆盖的异常路径（如批次过期、并发扣减）。
- 角色权限是否每个迁移都绑定。
- TODO 策略是否避免单据沉没。
- 冲销路径是否完整（已完成单的纠错只能冲销，不能反审核）。
- **Deferred 标注与代码零 writer 一致**：盘点单「自动生成差异移动单」= Deferred（当前手工 `generateMove` 入口，`completeTake` 仅置 DONE）；拣货单 `PICKING`/`PICKED` = 预留死状态（零 writer，WMS successor）。审查时核对 owner doc 声明的迁移/联动在 BizModel 中确有 `setStatus` writer，否则须显式 Deferred 标注。
