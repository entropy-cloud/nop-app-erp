# 库存域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。

## 适用对象

本状态机适用于**库存移动单**（StockMove）。盘点单状态机见文末「盘点单状态机（独立）」一节；调拨单/所有权转移单/成本调整单/到岸成本单四个独立 docStatus 状态机见「调拨单状态机（独立）」「所有权转移单状态机（独立）」「成本调整单状态机（独立）」「到岸成本单状态机（独立）」四节。主数据实体（物料/仓库等）的"启用/停用"不是状态机，见 `master-data/README.md`。

> **实体级状态机 Bean 注记**：各节迁移矩阵的权威代码载体为 `erp-inv-service` statemachine 包下的实体级 StateMachine Bean（StockMove=`ErpInvStockMoveStateMachine`、StockTake=`ErpInvStockTakeStateMachine`、TransferOrder=`ErpInvTransferOrderStateMachine`、OwnershipTransfer=`ErpInvOwnershipTransferStateMachine`、CostAdjust=`ErpInvCostAdjustStateMachine`、LandedCost=`ErpInvLandedCostStateMachine`）。固定来源/目标态判断由 Bean 唯一治理（契约 `docs/architecture/entity-state-machine-bean.md`）；本节描述业务语义，持久化状态码字典以 `model/app-erp-inventory.orm.xml` 为准。

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

- **差异调整移动单的自动生成 = 已实现（RC-R1.56 / P1-MA2-062，2026-08-16 落地）**：`ErpInvStockTakeBizModel.completeTake`（Facade 保留 `requireEntity` 权限管道）委托 per-mutation `ErpInvStockTakeCompleteTakeProcessor` 完整盘点闭环——行加载 → **D1 差异口径**（L1 逐字公式 `差异 = actualQuantity − bookQuantity`，`use-cases.md:129`；盘点行字段快照对账，否决实时 `StockBalance.totalQuantity` 比对）逐行计算并回填 `differenceQuantity`/`differenceAmount`（零差异行跳过）→ 逐行经 `IErpInvStockMoveBiz.generateMove` Facade 生成差异移动单（差异 >0 → 盘盈 INCOMING / 差异 <0 → 盘亏 OUTGOING，行量 = |差异|，行级 material/sku/uoM/batchNo/location 映射）→ 置 DONE。**D2 生成语义 = 独立移动单**：`relatedBillType=ERP_INV_STOCK_TAKE` + `relatedBillCode=null` → `StockMoveRequest.isBusinessLinked()==false` → 停 **CONFIRMED** 待库管员二次确认（产生库管员待办，对齐 :129「独立创建的移动单才产生库管员待办」；幂等由 CONFIRMED→DONE 单次迁移守卫保证）。**D3 过账处理 = 跳过**：`InvPostingDispatcher.resolveBusinessType` 跳过集加 `ERP_INV_STOCK_TAKE`——差异移动单 DONE 零凭证、`posted=false` 保持（null relatedBillType 仍按 moveType 误派 PURCHASE_INPUT/SALES_OUTPUT，故类型键 + 跳过集条目为必要组合；盘点差异会计化 = successor，见下）。**D4 关联载体**：移动单 remark 承载「盘点差异 {take.code} 盘盈/盘亏」（零 ORM 变更，审计经 code 引用反向追溯）。**失败语义（D4-b）**：逐行生成失败**不阻断整单**——同事务补偿删除该行孤立 DRAFT 移动单（失败面集中于 confirm 的可用量/效期校验，预留/余额变更之前）→ LOG.warn → config `erp-inv.stocktake-diff-alert-enabled`（默认 false）门控派发 `inv.stocktake-diff-generation-failed` 告警（对齐 A4.2.4 dispatchVarianceFailureAlert 范式，无 ACTIVE 模板静默跳过）；`differenceQuantity`/`differenceAmount` 回填不依赖生成成败（盘点单 DONE 后差异数据完整，运维可经手工 generateMove 补录）。守卫错误码 `ERR_INV_STOCK_TAKE_MOVE_GENERATE`（D4-c）。
- 盘点完成的差异**不直接改余额**，而是经移动单（盘盈 INCOMING/盘亏 OUTGOING 独立单）停 CONFIRMED 待库管员二次确认，走移动单状态机流程（CONFIRMED→DONE 经 `bookCompletion`）才会影响余额。这一原则保留（断言④⑤，`bookCompletion` 全仓唯一调用点 = `ErpInvStockMoveProcessor.doComplete`）。
- 这种设计保证所有余额变动都通过移动单流水可追溯，盘点只是发现差异的入口（入口 = completeTake 自动生成，2026-08-16 起取代手工 generateMove）。
- **盘点差异会计化（successor，触发条件已命名）**：盘盈/盘亏金额当前不入 GL（D3 选项 A 跳过过账）。触发条件 = 运营/审计要求盘点差异会计化[盘盈/盘亏 GL 凭证]时，按会计核心路径立项（专属 businessType + AcctDocProvider + 双独立子 agent 批准 + 独立 plan-audit）。
- **盘点期间出入库冻结运营建议（watch-only residual）**：D1 采用 `bookQuantity` 快照口径，与盘点期间实时余额有差——运营惯例冻结账面快照，建议盘点期间冻结仓库出入库操作（ORM 无显式锁字段，经运营流程保障）。

## 调拨单状态机（独立）

调拨单（ErpInvTransferOrder）有独立状态机，与移动单不同——**仅单边 confirm，无 cancel/complete/reverse**：

```
草稿 (DRAFT) ── 确认 → 已确认 (CONFIRMED) [终态]
```

- **迁移**：`confirm: {DRAFT}→CONFIRMED`；initial={DRAFT}、terminal={CONFIRMED}。
- **仅 confirm 边的原因**：调拨单确认后，实际物理移动由**独立 ErpInvStockMove 流**承载（生成新 DRAFT 移动单走移动单状态机），非本单 docStatus 生命周期。DONE/CANCELLED 生命周期（cancel/complete/reverse writer）属 out-of-scope（路线图 Deferred「TransferOrder DONE/CANCELLED 生命周期 + approveStatus 接入」，触发条件 = PM 要求调拨单取消/完成/审批业务流落地时）。
- **过账边界（较轻保护区）**：confirm **不触发存货成本过账、不生成 stock movement**；仅可选跨法人内部往来 GL hook（`dispatchIntercompanyPosting` → `IErpFinIntercompanyTransferBiz.onTransferConfirmed`，config-gated + **失败吞掉** log warn，不阻塞库存确认）。无 TransferOrderPostingDispatcher。
- **错误码缺陷（confirmed live defect，行为保持不修正）**：confirm 守卫（`ErpInvTransferOrderConfirmProcessor.validateDraft`）抛**盘点单的** `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`（`erp.err.inv.stock-take.illegal-transition`）+ `ARG_TAKE_ID` 参数（copy-paste bug，应为 TransferOrder 自己的码；无 TransferOrder 专属 illegal-transition 码）。路线图 Non-Goal「不借迁移改变既有错误码」→ 行为保持映射既有（错误）码；修正（新增 `ERR_INV_TRANSFER_ORDER_ILLEGAL_TRANSITION`）登记为 successor（见路线图 Deferred）。
- **状态机 Bean**：`ErpInvTransferOrderStateMachine`（confirm 单边；非法来源态 Bean 抛 common 码，Processor 映射为上述既有（错误）领域码 + `ARG_TAKE_ID`/`ARG_CURRENT_STATUS` 参数不变）。
- **动态守卫边界（保留 Processor）**：intercompany hook（config-gated + 失败吞掉）不属于状态轴判断，Bean 不承载。

## 所有权转移单状态机（独立）

所有权转移单（ErpInvOwnershipTransfer）有独立状态机，与移动单不同——**docStatus 用独立字典 `erp-inv/ownership-transfer-status`**（4 值 DRAFT/CONFIRMED/DONE/CANCELLED，值与 move-status 相同但**不复用**，常量 `ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_*` 非 `DOC_STATUS_*`）：

```
草稿 (DRAFT)
  ├─ 确认 → 已确认 (CONFIRMED)
  │           ├─ 完成 → 已完成 (DONE)   [终态]
  │           └─ 取消 → 已取消 (CANCELLED) [终态]
  └─ 取消 → 已取消 (CANCELLED) [终态]
```

- **迁移**：`confirm: {DRAFT}→CONFIRMED`；`done: {CONFIRMED}→DONE`；`cancel: {DRAFT,CONFIRMED}→CANCELLED`；initial={DRAFT}、terminal={DONE, CANCELLED}。无 approveStatus 轴（无审批流）。
- **过账边界（存货成本过账 + 库存强一致保护区）**：
  - done 触发 `OwnershipTransferPostingDispatcher.dispatchIfApplicable`：仅 `transferType==VMI_CONSUME` 且 config `erp-inv.vmi-auto-generate-ap=true` 时过账 `ErpFinBusinessType.OWNERSHIP_TRANSFER`→`IErpFinVoucherBiz.post`（Dr Inventory/Cr AP）；**失败保持 DONE + posted=false**（try/catch 吞掉 log，运营可见悬挂）。
  - done 同时执行**余额重分类**（`reclassifyBalance`，同库位对 (material×warehouse×location×batch) 改 ownershipType/ownerId，**数量守恒非 stock movement**，经 `bookkeeper.updateBalanceWithRetry` 并发兜底）。
  - confirm/cancel 无过账。
- **动态守卫（保留 Processor）**：`ERR_OWNERSHIP_TRACKING_DISABLED`（config `erp-inv.ownership-tracking-enabled=false` 时 done 拒绝）、不变量校验（loc-mismatch：`sourceLocId==destLocId` 物理位置不变；type-inconsistent：transferType 与所有权类型迁移一致性）。
- **错误码**：`ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS`（`erp.err.inv.ownership-transfer-illegal-status`，**专属码**，最接近 StockMove 范式）。
- **状态机 Bean**：`ErpInvOwnershipTransferStateMachine`（confirm/done/cancel 3 动作；用 `OWNERSHIP_TRANSFER_STATUS_*` 常量独立 dict 语义；非法来源态 Bean 抛 common 码，Processor 映射为 `ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS` + `ARG_TRANSFER_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS` 参数不变）。

## 成本调整单状态机（独立）

成本调整单（ErpInvCostAdjust）有独立状态机，与移动单不同——**approveStatus 独立审批轴与 docStatus 双轴并存，Bean 仅迁移 docStatus 轴**：

```
草稿 (DRAFT) ── 应用 → 已完成 (DONE) [终态]
    ↑                      │
    └──── 红冲 ────────────┘
        (reverseCostAdjust: DONE→CONFIRMED, 可逆重应用)
```

- **迁移**：`applyCostAdjust: {DRAFT,CONFIRMED}→DONE`；`reverseCostAdjust: {DONE}→CONFIRMED`；initial={DRAFT}、terminal={DONE}（**CONFIRMED 可逆**——仅由 reverse 到达、可 re-apply，非终态）。
- **双轴联动**：`applyCostAdjust`/`reverseCostAdjust` 的 approveStatus 写 + gating（`erp-fin.cost-adjust-approval` 审批门控 + 5 INLINE 动作 submitForApproval/approve/reject/reverseApprove/withdrawApproval）**保留 Processor**（`ErpInvCostAdjustProcessor`），Bean 仅集中 docStatus 边 + `assertCan*`（docStatus 源态）——option a，同 StockMove Non-Goal 先例。
- **过账边界（存货成本过账 + 库存强一致保护区）**：
  - apply 触发 `CostAdjustmentPostingDispatcher.tryPost`（COST_ADJUSTMENT，借贷方向由 `totalAdjustAmount` 符号定 INCREASE/DECREASE，**净 0 跳过返回 null → DONE+posted=false 边缘**）+ 成本层更新（`CostAdjustmentService.applyCostAdjust`）。
  - reverse 触发红字凭证（`postingDispatcher.reverse`）+ 成本层逆转（`reverseCostAdjust`）。
  - **无 stock movement**（纯成本变更，`LEDGER_MOVE_ID_COST_ADJUST=0L` 标记）。
- **跨实体子单据写（内部编排，Bean 容忍不发明边）**：`ErpInvLandedCostProcessor` facade 写子 CostAdjust docStatus（DRAFT seed / DONE / CANCELLED），刻意绕过 `ErpInvCostAdjustProcessor.applyCostAdjust`（避免 COST_ADJUSTMENT+LANDED_COST 双过账，注释 `createAndApplyCostAdjust`）。CANCELLED 由此路径写入，非 Bean 迁移边。
- **net-0 DONE+posted=false 边缘裁定**：既有代码 apply 无 docStatus 源态守卫（仅 validateNotCancelled/已-applied/审批门），net-0 调整可达 DONE+posted=false 且理论可从 DONE 重 apply；Bean `assertCanApplyCostAdjust({DRAFT,CONFIRMED})` 对 DONE 源态拒绝属**合理收紧**（从 DONE 重 apply 语义错误，正常流程重 apply 必经 reverse→CONFIRMED；无既有测试覆盖此边缘，层 2 四方对照已核实，裁定不违反「保持既有外部行为不变」Non-Goal）。
- **错误码**：docStatus **无专属 illegal-transition 码**（apply/reverse 由 posted/approval 门守卫：`ERR_COST_ADJUST_ALREADY_APPLIED`/`NOT_APPROVED`/`NOT_APPLIED`）——Bean common→既有 generic `ERR_ILLEGAL_STATUS_TRANSITION` 映射（intentional legacy，见计划 Phase 3 Decision）。
- **状态机 Bean**：`ErpInvCostAdjustStateMachine`（applyCostAdjust/reverseCostAdjust 2 动作；approveStatus 轴不在 Bean）。

## 到岸成本单状态机（独立）

到岸成本单（ErpInvLandedCost）有独立状态机，与移动单不同——**approve/reverseApprove 双轴联动（同时写 docStatus 与 approveStatus），Bean 仅迁移 docStatus 边；无 CONFIRMED 写（DRAFT→DONE 直达）**：

```
草稿 (DRAFT) ── 审核 → 已完成 (DONE) [终态]
    │                        │
    │                        └── 红冲 → 已取消 (CANCELLED) [终态]
    └── generateFreightLandedCost（生成路径，seed DRAFT，无迁移边）
```

- **迁移**：`approve: {DRAFT}→DONE`；`reverseApprove: {DONE}→CANCELLED`；initial={DRAFT}、terminal={DONE, CANCELLED}。**无 CONFIRMED 写**。
- **双轴联动（原子写）**：`doPostApprove` 同时写 `docStatus→DONE` + `approveStatus→APPROVED`；`doReverseApprove` 同时写 `docStatus→CANCELLED` + `approveStatus→REJECTED`。Bean 仅集中 docStatus 边 + `assertCan*`（docStatus 源态），approveStatus 写 + approveStatus/posted gating 保留 Processor（option a）。
- **过账边界（存货成本过账 + 库存强一致保护区）**：
  - approve 触发 `LandedCostPostingDispatcher.tryPost`（LANDED_COST Dr Inventory/Cr AP）+ 分配引擎（`LandedCostAllocationEngine`）+ 子 CostAdjust 成本层更新（`createAndApplyCostAdjust` 直接调 `CostAdjustmentService.applyCostAdjust`，避免双过账）。
  - reverse 触发红字凭证（`postingDispatcher.reverse`，**失败吞掉 + 告警** `IErpSysNotificationBiz.notify(NOTIFY_EVENT_LANDED_COST_REVERSE_FAILURE)`，G4 分级）+ 子 CostAdjust 逆转（`reverseCostAdjust` + 子单 posted=false）。
  - **无 stock movement**（纯成本）。
- **生成路径无迁移边（契约 §9.2 选项 c）**：`generateFreightLandedCost`（`createLandedCostHead`）创建新单 seed DRAFT，不调 `assertCan*`。
- **动态守卫（保留 Processor）**：幂等守卫（`ERR_LANDED_COST_ALREADY_APPROVED`）、悲观锁（`lockReceiveForAllocation` SELECT FOR UPDATE 串行化并发同 receiveId 分摊）、`validateNotAlreadyAllocated`（`ERR_LANDED_COST_ALREADY_ALLOCATED`）、reverse posted+APPROVED 守卫（`ERR_LANDED_COST_NOT_POSTED`）、reverse 失败吞掉 + 告警（G4）。
- **错误码**：docStatus **无专属 illegal-transition 码**（approve/reverse 由幂等/posted 门守卫 `ERR_LANDED_COST_ALREADY_APPROVED`/`NOT_POSTED`）——Bean common→既有 generic `ERR_ILLEGAL_STATUS_TRANSITION` 映射（intentional legacy，见计划 Phase 3 Decision）；reverse 失败告警（G4）为 intentional legacy 保留。
- **状态机 Bean**：`ErpInvLandedCostStateMachine`（approve/reverseApprove 2 动作；双轴联动中 Bean 仅 docStatus 边；`generateFreightLandedCost` 无迁移边）。

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
- **Deferred 标注与代码零 writer 一致**：盘点单「自动生成差异移动单」= **已实现**（RC-R1.56，completeTake 经 `ErpInvStockTakeCompleteTakeProcessor` 自动生成盘盈/盘亏移动单，差异回填 `differenceQuantity`/`differenceAmount` 为业务 writer；盘点差异会计化[GL 凭证]仍为 successor）；拣货单 `PICKING`/`PICKED` = 预留死状态（零 writer，WMS successor）。审查时核对 owner doc 声明的迁移/联动在 BizModel 中确有 `setStatus` writer，否则须显式 Deferred 标注。
