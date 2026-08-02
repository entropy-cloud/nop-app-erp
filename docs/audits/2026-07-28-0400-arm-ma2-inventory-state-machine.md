# MA2 库存状态机审查（A2.11）

> 里程碑：MA2（业务正确性层 / 状态机正确性维度）
> 域/功能模块：inventory / 移动单 + 盘点单 + 拣货单 + 业务单据双轴（CostAdjust/LandedCost/TransferOrder）+ 所有权转移 + 批次/序列号/预留
> 审计 plan：`docs/plans/2026-07-28-0400-3-audit-remediation-ma2-inventory-state-machine.md`
> 行为基线：`docs/design/inventory/{state-machine,trace-chain,cross-domain,consignment}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
> 范围：19 状态字段（plan baseline），实际审查经逐文件全文阅读 + grep 验证
> 审计执行：2026-07-28
> 上游基线：MA1 done（P0-MA1-021 done + P1-MA1-022 + P2-MA1-025 已登记）；A2.4 done（P1-MA2-023/024 + P2-MA2-026~030）；A2.5a done；A2.8 done（范本）

## 1. 审查范围与状态字段清单

| 实体 / 组件 | 状态轴（dict） | 实现文件 | 审查方式 |
|------------|---------------|----------|---------|
| **ErpInvStockMove**（移动单生命周期） | `moveType`(erp-inv/operation-type) + `docStatus`(erp-inv/move-status) + `approveStatus`(wf/approve-status) | `ErpInvStockMoveProcessor.java` + `ErpInvStockMoveBizModel.java` | 全文逐行 |
| **ErpInvStockTake**（盘点单独立） | `takeType`(erp-inv/take-type) + `docStatus`(erp-inv/move-status 复用) + `approveStatus` | `ErpInvStockTakeBizModel.java`（仅 3 mutation：startTake/completeTake/cancelTake） | 全文逐行 |
| **ErpInvPickingOrder**（拣货单） | `docStatus`(erp-inv/picking-status) | `ErpInvPickingOrderBizModel.java`（15 行 CRUD 桩，extends CrudBizModel 无任何 setStatus writer） | 全文逐行 |
| **ErpInvCostAdjust**（成本调整双轴） | `adjustType`(erp-inv/adjust-type) + `docStatus`(erp-inv/move-status) + `approveStatus` | `ErpInvCostAdjustProcessor.java` + `ErpInvCostAdjustBizModel.java` + 4 per-mutation Processor | 全文逐行 |
| **ErpInvLandedCost**（到岸成本双轴） | `allocationMethod`(erp-inv/landed-cost-alloc-method) + `docStatus`(erp-inv/move-status) + `approveStatus` | `ErpInvLandedCostProcessor.java` + `ErpInvLandedCostBizModel.java` | 全文逐行 |
| **ErpInvTransferOrder**（调拨单双轴） | `docStatus` + `approveStatus` | `ErpInvTransferOrderBizModel.java`（仅 confirm mutation） | 全文逐行 |
| **ErpInvOwnershipTransfer**（所有权转移） | `transferType`(erp-inv/ownership-transfer-type) + `docStatus`(erp-inv/ownership-transfer-status) + from/toOwnershipType | `ErpInvOwnershipTransferProcessor.java` + `ErpInvOwnershipTransferBizModel.java` | 全文逐行 |
| **ErpInvBatch / ErpInvSerialNumber / ErpInvReservation** | `status`(erp-inv/batch-status / serial-status / reservation-status) | 无独立 Processor；`ErpInvBatchBizModel.java`/`ErpInvSerialNumberBizModel.java`/`ErpInvReservationBizModel.java` 均为 CRUD 桩（仅 setEntityName 构造器）；状态变更无 writer | 全文逐行 |
| **ErpInvStockBalance / ErpInvStockLedger / ErpInvCostLayer** | `costMethod`(erp-md/cost-method) + `ownershipType`(erp-inv/ownership-type) | 数值派生（非状态机） | 只读核验 |

19 状态字段分布在 10 个状态承载实体 + 3 个数值派生实体（与 plan baseline 一致 ✓）。

## 2. 10 维度审查

### 2.1 维度「状态定义」

**裁决：PASS（含 1 项已登记 P2 watch-only）**

#### 移动单 docStatus（erp-inv/move-status DRAFT/CONFIRMED/DONE/CANCELLED 4 态）

✅ **每个状态表达「等待什么」**：DRAFT=等待提交确认；CONFIRMED=等待实际执行搬动；DONE=已执行完成等待后续过账；CANCELLED=终态作废。owner doc `state-machine.md §1` 与 `ErpInvConstants.DOC_STATUS_*` 完全对齐，与 dict option value 逐一对应。

✅ **DONE/CONFIRMED 语义清晰**（owner doc §1 强调「等待什么 vs 做什么」）：DONE 不是「做完成」而是「等待后续过账」（异步后置动作）；CONFIRMED 不是「做确认」而是「等待执行搬动」。

✅ **预留量影响轴清晰**（owner doc §1/§2 表）：DRAFT 否 / CONFIRMED 是（出库类）/ DONE 否（已释放）/ CANCELLED 否（已释放）。实现 `ErpInvStockMoveProcessor.reservesOnConfirm:341-347` 仅 OUTGOING + INTERNAL_TRANSFER 占预留，与 owner doc §2 入库/出库/内部调拨三态表一致 ✓。

#### ⚠️ 盘点单 COUNTING vs dict CONFIRMED（P2-MA1-025 持续 watch-only，无升级）

owner doc `state-machine.md §盘点单状态机 L152` ASCII 图用 **`COUNTING`** 命名「盘点中」态，但 dict `erp-inv/move-status` 无 `COUNTING` 选项——盘点单 `ErpInvStockTake.docStatus` ORM 绑定 `erp-inv/move-status`，盘点中态实际由 **`CONFIRMED`** 承载。兄弟 owner doc `ui-patterns.md:136` 表述正确（CONFIRMED(盘点中)）。

**P2-MA1-025 复核结论**：owner doc 内部不一致持续，按 P2-MA1-025 已登记 watch-only 维持，**不升级**（运行时正确——`startTake:33` 设 `DOC_STATUS_CONFIRMED` 状态机迁移有效；UI dict 渲染正确；仅文档审查者期望 COUNTING 名时困惑）。

#### 业务单据 docStatus 字典差异（move-status vs erp/doc-status）

✅ **核验一致性**：库存业务单据（CostAdjust/LandedCost/TransferOrder/StockMove/StockTake）`docStatus` 列均绑定 `erp-inv/move-status`（DRAFT/CONFIRMED/DONE/CANCELLED），未与全局 `erp/doc-status`（DRAFT/ACTIVE/CANCELLED 3 态）混用。移动单状态机词汇表自洽。owner doc state-machine.md §1 已显式区分。

#### 拣货单 picking-status（PENDING/PICKING/PICKED/CANCELLED 4 态）—— 见维度 5 可达性裁决（**P1-MA2-063 新登记**）

#### 所有权转移 ownership-transfer-status（DRAFT/CONFIRMED/DONE/CANCELLED 4 态）独立字典

✅ **正确独立**（`ErpInvConstants.java:90-94` 显式注释「独立于移动单 move-status，不复用」）—— 因物权转移语义与物理移动语义虽状态名相同但实体不同，独立 dict 允许未来分化。`ErpInvOwnershipTransferProcessor` 4 个 mutation 覆盖 DRAFT→CONFIRMED→DONE / DRAFT/CONFIRMED→CANCELLED ✓。

#### 批次/序列号/预留 status（5/4/5 态）

⚠️ **3 个实体状态轴无主动 writer**——`ErpInvBatchBizModel`/`ErpInvSerialNumberBizModel`/`ErpInvReservationBizModel` 均为 CRUD 桩（仅 `setEntityName` 构造器，无任何 setStatus 调用）；grep 全 `module-inventory/erp-inv-service/src/main/` `setStatus.*ErpInv` 零业务 writer。这些状态轴当前由 codegen 默认值/外部域写入/手工维护承载。**不破坏移动单状态机主路径**（移动单生命周期 + 业务单据双轴不依赖批次/序列号/预留状态字段参与迁移判定；可用量校验读 `ErpInvStockBalance` 数值而非批次状态）。

按 mfg P1-MA2-035/036 + hr P1-MA2-039~042 同型裁决，登记 **P1-MA2-063**（拣货单 PICKING/PICKED 死状态 + CRUD 桩）—— 拣货单本身是独立状态机对象（owner doc §适用对象 §1 移动单外有「拣货」提及）；批次/序列号/预留属于「状态字段但无独立状态机章节」按 owner doc 边界 Deferred（不在本审查范围）。

---

### 2.2 维度「转换完整性」

**裁决：PASS**

#### 移动单生命周期迁移矩阵（核心）

| From → To | 触发 | 前置 | 结果 | 代码位置 | 裁决 |
|-----------|------|------|------|----------|------|
| DRAFT → CONFIRMED | `confirm`/业务联动 | status==DRAFT + 出库类可用量校验 + 应用预留 | setDocStatus(CONFIRMED) | `ErpInvStockMoveProcessor.doConfirm:185-197` | ✅ |
| CONFIRMED → DONE | `complete`/业务联动 | status==CONFIRMED + 释放预留 + bookCompletion 写流水/更新余额 | setDocStatus(DONE) + postingDispatcher.dispatchIfApplicable | `ErpInvStockMoveProcessor.doComplete:199-213` | ✅ |
| CONFIRMED → CANCELLED | `cancel` | status==CONFIRMED + releaseReservation | setDocStatus(CANCELLED) | `ErpInvStockMoveProcessor.cancel:96-113` | ✅ |
| DRAFT → CANCELLED | `cancel` | status==DRAFT（无需 release） | setDocStatus(CANCELLED) | 同上 | ✅ |
| DONE → 冲销（生成反向单新 DRAFT） | `reverse` | status==DONE | 生成反向 StockMove（businessLinked=true→自动 DRAFT→CONFIRMED→DONE），原单保持 DONE | `ErpInvStockMoveProcessor.reverse:115-154` | ✅ |

✅ **每个状态列出所有入/出转换**——owner doc `state-machine.md §2` ASCII 图与代码 1:1 对应，无遗漏。

✅ **出库可用量校验前置落实**（owner doc §2 + cross-domain.md §余量校验规则）：`validateAvailable:215-235` 对 `reservesOnConfirm==true`（OUTGOING/INTERNAL_TRANSFER）的移动单在 doConfirm 阶段校验 `availableQuantity < required` 抛 `ERR_AVAILABLE_INSUFFICIENT` + 整个 @BizMutation 事务回滚。**销售独有约束已确认**——sales 域 `ErpSalDeliveryProcessor.approve` 经 `IErpInvStockMoveBiz.generateMove` → 本类 doConfirm → validateAvailable 强制（与 A2.9 sales 审计结论一致）。

✅ **DONE 冲销非状态回退**（owner doc §3/§5 强制）：`reverse()` 生成 `StockMoveRequest{relatedBillType="REVERSAL", relatedBillCode=原单.code}`，由 `generateMove()` 走正常 DRAFT→CONFIRMED→DONE 流程；原单保持 DONE 不变。`TestErpInvStockMoveBizModel.testReverseCreatesReverseMove:117-135` 验证此契约。**核心不变量——所有余额变动都通过移动单流水可追溯**——经证据确认 ✓。

✅ **冲销反向单可用量校验**（owner doc §4 异常路径）：冲销本质是反向移动（inverseMoveType: incoming→outgoing / outgoing→incoming）。原 incoming 移动（如采购入库）冲销生成 outgoing 反向单 → doConfirm → reservesOnConfirm==true → validateAvailable 强制校验。余额守恒保证 ✓。

#### 业务单据双轴审批状态机

**CostAdjust（DIRECT 模式 5 action）**：

| Action | 前置 | 目标态 | 代码位置 | 裁决 |
|--------|------|--------|----------|------|
| submitForApproval | UNSUBMITTED/REJECTED | SUBMITTED | `ErpInvCostAdjustProcessor.submitForApproval:58-65` | ✅ |
| withdrawApproval | SUBMITTED | UNSUBMITTED | `:67-74` | ✅ |
| approve | SUBMITTED（idempotent if APPROVED） | APPROVED + approvedBy/At | `:76-88` | ✅ |
| reject | SUBMITTED | REJECTED | `:90-97` | ✅ |
| reverseApprove | APPROVED + posted != true | REJECTED | `:99-111` | ✅ |
| applyCostAdjust（域动作） | UNSUBMITTED（免审 config 关）/APPROVED（config 开） + posted != true | DONE + posted=true（if voucherId != null） | `:115-144` | ✅ |
| reverseCostAdjust（域动作） | posted==true | CONFIRMED + posted=false | `:146-166` | ✅ |

✅ **reverseApprove 守卫 posted != true**（`:105-107`）——若已过账需先 reverseCostAdjust 业务红冲（凭证反向 + posted=false）才能 reverseApprove 审批反审；避免审批反审与凭证链路解耦。**P0-MA1-021 修复后状态机正确性复核通过**——`CostAdjustmentPostingDispatcher.reverse` 已改经 `IErpFinVoucherBiz.reverse` Facade（plan `2026-07-27-1430-1` done），不再跨模块直写 `ErpFinVoucher`。

**LandedCost（审核编排无 submit/reject 分离）**：

| Action | 前置 | 目标态 | 代码位置 | 裁决 |
|--------|------|--------|----------|------|
| approve（编排：分摊→成本层→过账） | approveStatus != APPROVED + receive approved + 未重复分摊 | APPROVED + DONE + posted=true | `ErpInvLandedCostProcessor.approve:74-101 + doPostApprove:384-413` | ✅ |
| reverseApprove（红冲） | posted==true + approveStatus==APPROVED | posted=false + REJECTED + docStatus=CANCELLED + CostAdjust 同步 | `:177-247` | ✅ |

⚠️ **LandedCost 与 CostAdjust 的 reverseApprove 目标态不对称**（LandedCost 翻 docStatus=CANCELLED 终态；CostAdjust 仅翻 approveStatus=REJECTED 保持 docStatus=CONFIRMED）。但语义差异合理：LandedCost approve 是「编排动作一步到位」（approve→DONE+posted），reverseApprove 是「完全红冲」（生成红字凭证 + 反向成本层 + 翻终态）；CostAdjust 是「审批与业务动作分离」（approve→APPROVED 但 posted=false；applyCostAdjust 才 posted=true）。**与 owner doc `costing-methods.md §到岸成本/§成本调整` 设计并行非分歧**。

**TransferOrder**：仅实现 confirm（DRAFT→CONFIRMED + intercompany hook 容错），其他迁移（CONFIRMED→DONE/CONFIRMED→CANCELLED/DRAFT→CANCELLED）经 inherited CrudBizModel 默认状态机或 Deferred。intercompany posting 失败吞异常告警保持库存与凭证解耦（与 cross-domain.md §与财务域协作 解耦设计一致）✓。

**OwnershipTransfer**：DRAFT→CONFIRMED→DONE 完整实现（`ErpInvOwnershipTransferProcessor.confirm/done/cancel:54-101`），DONE 内含 `validateInvariants`（sourceLoc==destLoc 物理位置不变 + transferType/ownershipType 一致性）+ `reclassifyBalance`（同库位 ownershipType/ownerId 重分类，数量守恒）。config-gated `ownership-tracking-enabled=false` 时 DONE 抛 ERR_OWNERSHIP_TRACKING_DISABLED ✓。

---

### 2.3 维度「终端状态和恢复」

**裁决：PASS**

✅ **DONE 终态**（移动单 + 业务单据）：DONE 无出边（无 setStatus 翻回 CONFIRMED/DRAFT 的代码路径）。`doConfirm:187-192` 守卫 DRAFT→CONFIRMED 唯一入口，`doComplete:202-207` 守卫 CONFIRMED→DONE 唯一入口，DONE 不再被任何代码 setStatus 改出。`TestErpInvStockMoveBizModel.testIllegalTransitionRejected:86-93` 验证 DONE→CONFIRMED 拒绝（ERR_ILLEGAL_STATUS_TRANSITION）。

✅ **DONE 纠错路径是冲销反向单非状态回退**（owner doc §3/§5 强制 + §trace-chain.md §实现说明）：`reverse:115-154` 不修改原单 docStatus，生成新反向 StockMove 走 generateMove 正常流程；testReverseCreatesReverseMove 证据 ✓。

✅ **CANCELLED 终态**：cancel 守卫 DRAFT/CONFIRMED → CANCELLED，CANCELLED 后无出边。owner doc §3「已取消不可恢复，需重新创建移动单」✓。

✅ **归档与活跃区分**：DONE/CANCELLED 移动单作为历史归档（`useLogicalDelete=true` delVersion 字段承载逻辑删除）；不参与活跃 TODO。

⚠️ **LandedCost.reverseApprove 翻 CANCELLED 是终态迁移**——与 CostAdjust（reverseApprove 不改 docStatus 保持可重审）不同。但 LandedCost 业务语义「审核即完成」，红冲等同取消，无需保留可重审状态。owner doc `costing-methods.md §到岸成本` 已落地此设计 ✓。

---

### 2.4 维度「异常路径」

**裁决：PASS（含 1 项 P2-MA2-028 复核无升级）**

| 异常场景 | 处理 | 代码位置 | 裁决 |
|----------|------|----------|------|
| 出库可用量不足 | DRAFT→CONFIRMED 拒绝 + 事务回滚 | `validateAvailable:227-233` 抛 ERR_AVAILABLE_INSUFFICIENT | ✅ |
| 冲销反向单可用量不足 | 反向单 doConfirm 同样校验（owner doc §4） | `reverse→generateMove→doConfirm→validateAvailable` | ✅ |
| 批次/序列号缺失 | 当前不校验（物料/SKU 校验在 codegen 层；批次管理 Deferred 至独立 successor） | 无主动校验代码 | ⚠️ Deferred（不破坏主路径） |
| 批次过期 | 无主动校验（owner doc §4 「可配放行」） | 无 | ⚠️ Deferred |
| 序列号已售 | 无主动校验 | 无 | ⚠️ Deferred |
| 并发扣减同一批次 | config `concurrent-deduct-max-retry` 默认 5 + 退避（`ErpInvConstants:18-22`） | 重试机制配置就绪 | ⚠️ 系统性并发正确性归 **A2.17** |
| 重复触发（业务单据重复审核） | 幂等键 `(relatedBillType, relatedBillCode)` 反查 | `ErpInvStockMoveProcessor.findExisting:323-329 + findByRelatedBill:156-165`（O-5 id DESC 确定性） | ✅ |
| 负库存配置放行 | config `allow-negative-stock=false` 默认 + validateAvailable 跳过 | `validateAvailable:216-218` + `isNegativeStockAllowed:384-387` | ✅ |

#### today() 破坏 FIFO 队列时序（P2-MA2-028 复核——状态机角度无升级）

**位置**：`ErpInvStockMoveProcessor.reverse:128` `reverseReq.setBusinessDate(CoreMetrics.today())`。

**状态机角度裁决：无升级**——`businessDate` 是数值字段（影响成本队列时序），不参与状态机迁移判定（doConfirm/doComplete/cancel/reverse 均不读 businessDate 决定状态）。冲销反向单仍走 DRAFT→CONFIRMED→DONE 标准流程，状态机正确性不受 today() vs 原 businessDate 选择影响。

**成本角度**：归 A2.4（P2-MA2-028 已登记 watch-only）——`today()` 影响新成本层 incomingDate 排序，FIFO 队列可能错位（详细机制见 `2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`）。本审计**不重复登记根因**，仅复核状态机角度无升级。

---

### 2.5 维度「可达性」

**裁决：PASS（含 1 项新 P1 + 1 项已登记 P2）**

#### 移动单生命周期可达性

✅ 从 DRAFT 可达 CONFIRMED/DONE/CANCELLED 全态（DRAFT→CONFIRMED→DONE/DRAFT→CONFIRMED→CANCELLED/DRAFT→CANCELLED 全实现）。
✅ 无不可达状态：每个状态都有入边（DRAFT 经 newMove/generateMove；CONFIRMED 经 doConfirm；DONE 经 doComplete/reverse 自动 DONE；CANCELLED 经 cancel）。
✅ 无死锁/无限循环：DONE 与 CANCELLED 是终态无出边；DRAFT→CONFIRMED→DONE 是有向无环。冲销反向单是独立新流程，不构成原单循环。
✅ **冲销反向单可达性**：reverse 守卫 status==DONE，仅 DONE 态可触发冲销；冲销反向单本身走 generateMove 流程可达 DONE。

#### 盘点单可达性（P2-MA1-025 持续，COUNTING/CONFIRMED 名漂移）

✅ **盘点单 DRAFT→CONFIRMED→DONE/DRAFT/CONFIRMED→CANCELLED 4 态全可达**——`startTake:24-36`（DRAFT→CONFIRMED）+ `completeTake:38-50`（CONFIRMED→DONE）+ `cancelTake:52-66`（DRAFT/CONFIRMED→CANCELLED）。CONFIRMED 实际承载 owner doc 描述的「盘点中（COUNTING）」语义，仅命名漂移（P2-MA1-025 watch-only）。

#### 拣货单可达性（**新 P1-MA2-063**）

❌ **dict `erp-inv/picking-status` 4 态中 PICKING/PICKED 两态不可达**——`ErpInvPickingOrderBizModel.java` 15 行 CRUD 桩：

```java
public class ErpInvPickingOrderBizModel extends CrudBizModel<ErpInvPickingOrder> implements IErpInvPickingOrderBiz{
    public ErpInvPickingOrderBizModel(){
        setEntityName(ErpInvPickingOrder.class.getName());
    }
}
```

无 `startPicking`/`completePicking`/`cancelPicking` 任何 setStatus(PICKING_STATUS_*) writer。grep 全 `module-inventory/erp-inv-service/src/main/` `PICKING_STATUS\|setDocStatus.*PICKING\|picking-status` **零业务命中**。dict 4 态 PENDING/PICKING/PICKED/CANCELLED 中：PENDING 仅由 codegen 默认值承载；PICKING/PICKED 是 dict 死状态。按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035/036 + hr A2.7a P1-MA2-039~042 同型裁决，登记 **P1-MA2-063**（详 §4）。

#### 业务单据可达性

✅ **CostAdjust/LandedCost/TransferOrder/OwnershipTransfer 全状态可达**——详 §2.2 迁移矩阵。

#### 批次/序列号/预留状态可达性

⚠️ **5/4/5 dict 态大量死状态**——`ErpInvBatchBizModel`/`ErpInvSerialNumberBizModel`/`ErpInvReservationBizModel` 均为 CRUD 桩，无 setStatus writer。但 owner doc `state-machine.md §适用对象` 明示本状态机仅覆盖 StockMove（主）+ StockTake（独立）；批次/序列号/预留不是独立状态机对象，状态字段是数值/标签辅助（如批次 LOCKED 由质量管理域设置——Deferred successor）。本审计**不登记为新 P1**——按 owner doc 边界裁定（与 mfg A2.6b P1-MA2-036 预测 CONSUMED Deferred 同型，已登记 successor）。

---

### 2.6 维度「角色和权限」

**裁决：PASS（owner doc 角色绑定齐全，运行时经 NopAuth @BizMutation 入口权限校验）」

✅ **每个迁移绑定执行角色**（owner doc `state-machine.md §6` 表）：
- 提交确认 DRAFT→CONFIRMED：业务单据审核人（采购员/销售员）/ 库管员（独立创建）
- 执行完成 CONFIRMED→DONE：系统联动 / 库管员（二次确认）
- 取消：库管员
- 冲销：库管员（需财务影响确认）
- 业务单据双轴审批：按各域审批角色（与 finance/purchase/sales 审批流共用）

✅ **危险操作控制**（owner doc §6）：
- **冲销已完成移动单**：库管员权限 + owner doc §6 建议「二次确认」（因触发存货过账冲销）
- **负库存放行**：仅管理员可配置 `erp-inv.allow-negative-stock`（不放行给普通库管员）

⚠️ **二次确认/管理员门控未在代码层显式校验**——代码无显式 role check，依赖：(1) Nop 平台 `@BizMutation` 入口权限注解（由 nop-auth 拦截器统一处理）；(2) config `allow-negative-stock=false` 默认保护性配置。owner doc §6「建议二次确认」是建议层非强制——按当前实现属可接受（与 finance/mfg/pur/sal 同型：业务规则文档化，权限由平台层统一）。**不登记为新 finding**。

✅ **角色名与状态名同源业务词汇**（库管员/采购员/销售员/财务员，见 `roles-and-permissions.md`）。

---

### 2.7 维度「外部依赖」

**裁决：PASS」

✅ **业务单据触发库存移动**（cross-domain.md §与采购/销售协作）：外部触发经 `IErpInvStockMoveBiz.generateMove(StockMoveRequest)` Facade，request 显式包含 moveType/businessDate/sourceWarehouseId/destWarehouseId/relatedBillType 等内部状态映射字段——**不直接使用外部状态值**（owner doc §7 表）。purchase/sales/mfg/assets/contract 域均经此 Facade（DAG 已确认无反向跨模块写）。

✅ **存货过账事件发布给财务域**（DONE 后）：`ErpInvStockMoveProcessor.doComplete:212 postingDispatcher.dispatchIfApplicable(move, lines)` 派发 PostingEvent。**失败不影响移动单终态**——`InvPostingDispatcher` 容错设计（tryPost 吞异常 + LOG.warn/error + 异常工作台 + DeferredPostingSweepJob 兜底重试，与 mfg/hr/assets posting dispatcher 同型）。owner doc §7 + cross-domain.md §与财务域协作「移动单完成即视为库存记账成功，凭证生成是后置异步动作」经证据确认 ✓。

✅ **跨域写库存经 I*Biz Facade**：production 代码无 `daoFor(ErpInvStockMove).saveEntity` 跨域直写（A2.4 + MA1 已确认）。inventory 自身 CostAdjustmentPostingDispatcher 经 P0-MA1-021 修复后已用 `IErpFinVoucherBiz.reverse` Facade。

⚠️ **跨域只读经 IDaoProvider.daoFor**（P1-MA1-022 持续）——`ErpInvLandedCostProcessor:267,473,477 daoFor(ErpPurReceive/Line)` + `StandardCostResolver:99` + `CostMethodResolver:61,70` + `CostAdjustmentService:291 daoFor(ErpMd*)`。**状态机角度复核无升级**：跨域只读是成本解析/采购收货查询的副作用，不破坏状态机迁移（异常路径经 @BizMutation 事务回滚覆盖；LandedCost approve 编排跨域读失败抛异常回滚 DONE 不发生）。维持 P1-MA1-022 todo MR1。

---

### 2.8 维度「TODO / 任务策略」

**裁决：PASS（owner doc §8 避免沉没设计已落实）」

✅ **DRAFT assigned TODO**（独立创建）：`ErpInvStockMoveProcessor.generateMove:74-79` 当 `request.isBusinessLinked()==false` 时仅推进到 CONFIRMED（不调 doComplete），产生库管员待执行 TODO。`TestErpInvStockMoveBizModel.testManualMoveStopsAtConfirmed:76-83` 验证 ✓。

✅ **CONFIRMED confirm TODO**：业务联动通常立即 DONE，独立创建停在 CONFIRMED 等库管员二次确认（owner doc §8）。

✅ **DONE/CANCELLED 不产生 TODO**：终态归档。

✅ **业务联动避免单据沉没**（owner doc §8 关键设计）：`generateMove:74-79` 当 `request.isBusinessLinked()==true` 时立即 doComplete 推进到 DONE，避免独立 TODO（业务单据自身驱动后续）。`testGenerateMoveBusinessLinkedAutoCompletes:55-64` 验证 ✓。

⚠️ **盘点单/拣货单 TODO 策略未在 owner doc 显式定义**——owner doc §8 仅描述移动单。StockTake/StockPicking 的 TODO 由 CrudBizModel 默认机制承载（不产生沉没，归Deferred successor）。

---

### 2.9 维度「场景演练」（最重要）

**裁决：PASS（10 场景全覆盖）」

#### 场景 A：采购入库 happy path

1. `ErpPurReceiveProcessor.approve` 经 `IErpInvStockMoveBiz.generateMove(StockMoveRequest{moveType=INCOMING, relatedBillType=PUR_RECEIPT, ...})`
2. `ErpInvStockMoveProcessor.generateMove:57-80`：findExisting（幂等）→ newMove(DRAFT) → doConfirm（INCOMING 不占预留）→ doComplete（businessLinked=true 自动 DONE：写流水 + 增余额 + 释放预留[无可释放] + 发存货过账事件）
3. 财务域异步收到 PostingEvent → 生成 PURCHASE_INPUT 存货估值凭证

证据：`testGenerateMoveBusinessLinkedAutoCompletes:55-64` ✓。状态机：DRAFT→CONFIRMED→DONE 全迁移有代码路径 ✓。

#### 场景 B：销售出库可用量不足（拒绝回滚）

1. `ErpSalDeliveryProcessor.approve` 经 `IErpInvStockMoveBiz.generateMove(StockMoveRequest{moveType=OUTGOING, ...})`
2. `ErpInvStockMoveProcessor.generateMove` → `doConfirm:185-197` → `validateAvailable:215-235`：OUTGOING 触发 `reservesOnConfirm==true`，校验 `availableQuantity < required` → 抛 `ERR_AVAILABLE_INSUFFICIENT`
3. @BizMutation 事务回滚，移动单未生成（DRAFT 也回滚）

证据：`testCancelReleasesReservation:95-115` 间接验证可用量计算（total 10 − reserved 5 = 5 available），validateAvailable 守卫代码可见 ✓。

#### 场景 C：已完成冲销（owner doc §3 + trace-chain.md）

1. 库管员触发 `ErpInvStockMove__reverse(moveId)` GraphQL mutation
2. `reverse:115-154`：守卫 status==DONE → 构造 reverseReq（inverseMoveType + negateOrSame + originReturnedMoveId） → `generateMove(reverseReq)`
3. generateMove 内：findExisting（REVERSAL+原code）→ newMove(DRAFT) → doConfirm（反向类型，如原 incoming 则反向 outgoing 校验可用量）→ doComplete（businessLinked=true 自动 DONE：写反向流水 + 余额回退 + 反向存货凭证）
4. 原单保持 DONE（无状态回退）

证据：`testReverseCreatesReverseMove:117-135` 完整覆盖（断言新移动单 DONE + 不等于原单 id + 关联 REVERSAL + 原单 DONE 保持）✓。

#### 场景 D：冲销反向单可用量不足（拒绝）

1. 冲销反向单 reverseReq.moveType = inverseMoveType(原 incoming → outgoing)
2. generateMove → doConfirm → validateAvailable：outgoing 类型 reservesOnConfirm==true → 若可用量不足抛 ERR_AVAILABLE_INSUFFICIENT + 事务回滚
3. 冲销失败，原 DONE 单保持不变

代码路径：`reverse:115-154 + doConfirm:185-197 + validateAvailable:215-235` ✓。owner doc §4 异常路径表覆盖 ✓。

#### 场景 E：内部调拨（预留量差异）

1. 调拨 moveType=INTERNAL_TRANSFER，`reservesOnConfirm:341-347` 返回 true → 来源库位占预留
2. applyReservation 增加来源库位 reservedQuantity；doComplete releaseReservation 释放
3. 目的库位入库（增加 totalQuantity）

owner doc §2 表「内部调拨：来源库位占预留量，目的库位不占」经 `reservesOnConfirm==true` + `resolveReservationWarehouseId:349-351` (return sourceWarehouseId) 实现一致 ✓。

#### 场景 F：并发扣减同一批次（乐观锁+重试——交接 A2.17）

1. config `erp-inv.concurrent-deduct-max-retry=5`（默认）+ `erp-inv.concurrent-deduct-retry-backoff-ms=0`（默认）
2. 并发 doConfirm 时 validateAvailable + applyReservation 操作 StockBalance，依赖 ORM versionProp 透明乐观锁
3. 失败重试机制配置就绪（ErpInvConstants.CONFIG_CONCURRENT_DEDUCT_MAX_RETRY）

⚠️ **系统性并发正确性归 A2.17**（roadmap 显式分配）。本审计仅标注观察到的并发敏感点：(1) `ErpInvStockBalance` versionProp（透明乐观锁将 silent lost-update 降为 detectable conflict，与 mfg A2.6a + hr A2.7a/b 同型）；(2) `ErpInvStockMoveProcessor.applyReservation:237-253` 读-改-写 StockBalance 无显式锁，依赖乐观锁 + 重试。

#### 场景 G：盘点完成（**新 P1-MA2-062**——owner doc 声明自动生成盘盈/盘亏移动单未实现）

❌ **owner doc `state-machine.md §盘点单状态机 L153` 声明**：「盘点完成的差异**不直接改余额**，而是生成库存移动单（正数盘盈/负数盘亏），走移动单状态机流程才会影响余额」+ 「保证所有余额变动都通过移动单流水可追溯」。

**实际代码** `ErpInvStockTakeBizModel.completeTake:38-50`：

```java
public ErpInvStockTake completeTake(Long takeId, IServiceContext context) {
    ErpInvStockTake take = requireEntity(...);
    if (!Objects.equals(take.getDocStatus(), ErpInvConstants.DOC_STATUS_CONFIRMED)) {
        throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION)...;
    }
    take.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
    updateEntity(take, null, context);
    return take;
}
```

**仅 setDocStatus=DONE，无 generateMove 调用**。grep 全 `module-inventory/erp-inv-service/src/main/` `generateGain\|generateLoss\|gainMove\|lossMove\|StockTakeLine.*Move` 零匹配。盘点差异经手工或其他 Deferred 通道处理。

按 mfg P1-MA2-035/036（作业卡 TRANSFERRED 死状态 / MRP CANCELLED 死状态）+ hr P1-MA2-039~042（员工离职/合同 SUSPENDED/调查/发展计划迁移缺失）+ finance P1-MA2-031（DRAFT→CANCELLED 不可达）同型裁决——owner doc 声明迁移 + 联动但代码未实现。**不破坏主路径**：StockTake 状态机 DRAFT→CONFIRMED→DONE/CANCELLED 完整覆盖生命周期；done 不自动生成移动单不产生悬挂数据（盘点单 DONE 但无差异调整移动单，需库管员后续手工处理）。登记 **P1-MA2-062**（详 §4）。

#### 场景 H：批次过期拒绝出库（owner doc §4「可配放行」）

⚠️ Deferred——validateAvailable 不校验批次有效期，依赖质量管理域或独立 successor 实现。owner doc §4 「可配放行」语义 Deferred。**不破坏主路径**。

#### 场景 I：序列号已售拒绝再次出库

⚠️ Deferred——validateAvailable 不校验序列号状态。`ErpInvSerialNumberBizModel` 为 CRUD 桩。owner doc §4 列出但属 successor 范围。**不破坏主路径**。

#### 场景 J：负库存配置放行（管理员）

1. config `erp-inv.allow-negative-stock=true`（默认 false，仅管理员可配）
2. `validateAvailable:216-218`：`if (isNegativeStockAllowed()) return;` 跳过校验
3. 出库允许余额为负（特殊业务场景如先发货后入库）

代码 `validateAvailable:215-218 + isNegativeStockAllowed:384-387` ✓。owner doc §6「负库存放行仅管理员可配置」经 config 默认 false + NopSysVariable 运行时覆盖权限保护 ✓。

---

### 2.10 维度「与设计文档一致性」

**裁决：PASS（含 1 项持续 P2-MA1-025 + 1 项新 P2-MA2-062 owner doc 章节缺失）」

| owner doc 章节 | 代码位置 | 一致性 | 裁决 |
|---------------|----------|--------|------|
| `state-machine.md §1 状态定义`（移动单 4 态） | `ErpInvConstants.DOC_STATUS_*` + `erp-inv/move-status` dict | ✅ | ✓ |
| `state-machine.md §2 迁移完整性`（含出库可用量校验 + DONE 冲销反向单） | `ErpInvStockMoveProcessor.doConfirm/doComplete/cancel/reverse` | ✅ | ✓ |
| `state-machine.md §3 终态与恢复` | DONE/CANCELLED 守卫齐全 | ✅ | ✓ |
| `state-machine.md §4 异常路径`（含 today()） | validateAvailable + config gates | ✅ | today() 归 P2-MA2-028 |
| `state-machine.md §5 可达性` | 移动单全态可达 | ✅ | ✓ |
| `state-machine.md §6 角色与权限` | @BizMutation 入口权限 + config 保护性默认 | ✅ | ✓ |
| `state-machine.md §7 外部依赖` | IErpInvStockMoveBiz Facade + 存货过账事件解耦 | ✅ | ✓ |
| `state-machine.md §8 TODO 任务策略` | businessLinked 自动 DONE 避免沉没 | ✅ | ✓ |
| `state-machine.md §盘点单状态机` 用 `COUNTING` | dict/code 实际用 `CONFIRMED` | ❌ owner doc drift | **P2-MA1-025** watch-only（无升级） |
| `state-machine.md §盘点单状态机 L153` DONE 自动生成盘盈/盘亏移动单 | `ErpInvStockTakeBizModel.completeTake` 仅 setDocStatus=DONE 无 generateMove | ❌ 代码未实现 | **P1-MA2-062**（详 §4） |
| `trace-chain.md §追溯链模型`（M2M 自关联） | 实现为「单 uplink 列 + 反向查询」（plan 0700-1 已偏离补注） | ⚠️ 偏离已登记 | 已闭环 |
| `cross-domain.md §余量校验规则` | validateAvailable + reservesOnConfirm | ✅ | ✓ |
| `cross-domain.md §与财务域协作` 存货过账事件解耦 | InvPostingDispatcher 容错设计 | ✅ | ✓ |
| `consignment.md §ErpInvOwnershipTransfer 状态机` | `ErpInvOwnershipTransferProcessor` 4 mutation 完整 | ✅ | ✓ |
| `consignment.md §物理移动 vs 所有权转移严格分离` | `validateInvariants:105-122` 守卫 sourceLoc==destLoc | ✅ | ✓ |
| `processor-extension-pattern.md` Facade+Processor 两层 | 全 Processor 实现 protected step 方法 + IServiceContext 末参 | ✅ | ✓ |
| `posting-exemptions.md` 跨域写豁免登记 | inventory 无新豁免（P0-MA1-021 修复后合规） | ✅ | ✓ |

#### owner doc 章节缺失（**新 P2-MA2-062**）

⚠️ `state-machine.md` 仅含「适用对象：库存移动单（StockMove）」+ 末尾「盘点单状态机（独立）」章节。**6 个其他状态承载实体（拣货单/成本调整/到岸成本/调拨单/所有权转移/批次-序列号-预留）无独立章节**——散落在 `costing-methods.md §成本调整/§到岸成本`、`consignment.md §ErpInvOwnershipTransfer`、各 plan 文件中。与 purchase P2-MA2-053 + sales + mfg P2-MA2-045/047 + hr P2-MA2-047/052 + assets P2-MA2-059 同型（owner doc 缺独立章节）。登记 **P2-MA2-062**（详 §4）。

---

## 3. MA1/MA2 finding 运行时影响复核（库存状态机角度）

| Finding ID | 原登记 | 本审计复核（状态机角度） | 终态 |
|-----------|--------|------------------------|------|
| **P0-MA1-021** | done（plan `2026-07-27-1430-1`） | `ErpInvCostAdjustProcessor.reverseApprove:99-111` 守卫 posted != true；`reverseCostAdjust:146-166` 调 `postingDispatcher.reverse(adjust)` 经 `IErpFinVoucherBiz.reverse` Facade（plan 修复后合规）。状态机迁移正确（reverseApprove→REJECTED + reverseCostAdjust→CONFIRMED+posted=false 双路径） | **sustained done** |
| **P1-MA1-022** | todo MR1（9 域合并） | inv 5 处 daoFor 跨域只读（LandedCostProcessor:267,473,477 + StandardCostResolver:99 + CostMethodResolver:61,70 + CostAdjustmentService:291）维持。状态机角度：跨域只读是成本解析/采购收货查询副作用，不破坏状态机迁移（异常路径经 @BizMutation 事务回滚） | **不升级**（维持 todo MR1） |
| **P1-MA2-023** | todo MR1（A2.4 SPECIFIC 守卫缺失） | 状态机角度：SPECIFIC 历史成本守卫缺失归 A2.4 成本核算。本审计复核 reverse 状态迁移正确性（DONE→冲销反向单新 DRAFT），SPECIFIC 不破坏状态机迁移 | **不升级**（维持 todo MR1） |
| **P1-MA2-024** | todo MR1（A2.4 STANDARD 红冲成本不变量破缺） | 状态机角度：STANDARD 红冲成本不变量归 A2.4。本审计复核 reverse 状态迁移正确（reverse 生成反向单走 generateMove 正常流程，原单 DONE 保持）；posted 标记经 reverseCostAdjust 同步 | **不升级**（维持 todo MR1） |
| **P2-MA1-025** | todo MR1（COUNTING vs CONFIRMED） | 状态机角度：owner doc drift 持续（§2.1 已复核），运行时正确（CONFIRMED 承载盘点中语义），不破坏状态机 | **不升级**（维持 watch-only MR1） |
| **P2-MA2-026~030** | todo MR1（A2.4 测试覆盖） | 状态机角度：测试覆盖归 A5（系统性归 MA5）。本审计仅复核 reverse 状态迁移对测试的影响（today() 时序） | **不升级**（维持 watch-only） |
| **P2-MA2-028** | todo MR1（A2.4 today() 破坏 FIFO 队列时序） | **状态机角度无升级**——`reverse:128 setBusinessDate(CoreMetrics.today())` 影响成本队列时序，不参与状态机迁移判定。冲销反向单仍走 DRAFT→CONFIRMED→DONE 标准流程 | **状态机角度不升级**（成本角度维持 A2.4 watch-only） |

## 4. 新登记 finding

### P1（2 项，目标 MR1）

#### P1-MA2-062 StockTake completeTake 未自动生成盘盈/盘亏移动单

- **位置**：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockTakeBizModel.java:38-50`
- **现象**：owner doc `state-machine.md §盘点单状态机 L153` 声明「盘点完成（DONE）→ 自动生成盘盈/盘亏移动单（新 DRAFT）」+ 「盘点完成的差异**不直接改余额**，而是生成库存移动单（正数盘盈/负数盘亏），走移动单状态机流程才会影响余额」+ 「这种设计保证所有余额变动都通过移动单流水可追溯，盘点只是发现差异的入口」。**实际代码 completeTake 仅 `setDocStatus(DONE)`，无 generateMove 调用**。grep 全 `module-inventory/erp-inv-service/src/main/` `generateGain\|generateLoss\|gainMove\|lossMove\|StockTakeLine.*Move` 零匹配。
- **影响**：盘点差异经手工或其他 Deferred 通道处理，破坏 owner doc「所有余额变动都通过移动单流水可追溯」核心不变量（盘点差异不留下追溯链）。
- **裁决**：按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035/036 + mfg A2.6b P1-MA2-036 + hr A2.7a P1-MA2-039~042 + hr A2.7b P1-MA2-043~045 同型（owner doc 声明迁移/联动但代码未实现）。**不破坏 StockTake 主路径**——DRAFT→CONFIRMED→DONE/CANCELLED 完整覆盖盘点生命周期；done 不自动生成移动单不产生悬挂数据（盘点单 DONE 但无差异调整移动单，需库管员后续手工处理）。
- **修复方式**：MR1 裁决——方案 A（推荐）实现 completeTake 自动比对 StockTakeLine.qtyActual vs StockBalance.totalQuantity → 差异（盘盈正/盘亏负）生成 StockMove（businessLinked=false 停 CONFIRMED 待二次确认 OR businessLinked=true 自动 DONE 直接调整余额）+ owner doc §盘点单状态机补「自动生成移动单条件/默认推进策略」；方案 B owner doc §盘点单状态机 L153 标注「自动生成盘盈/盘亏移动单 Deferred——盘点差异经库管员手工 generateMove 处置」+ 删除 owner doc 「自动生成」语义。

#### P1-MA2-063 PickingOrder PICKING/PICKED dict 死状态 + ErpInvPickingOrderBizModel 15 行 CRUD 桩

- **位置**：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvPickingOrderBizModel.java`（15 行 CRUD 桩）
- **现象**：dict `erp-inv/picking-status` 4 态（PENDING/PICKING/PICKED/CANCELLED），`ErpInvPickingOrderBizModel` 仅 `extends CrudBizModel<ErpInvPickingOrder>` 无任何 setStatus writer。grep 全 `module-inventory/erp-inv-service/src/main/` `PICKING_STATUS\|setDocStatus.*PICKING\|picking-status` 零业务命中。PICKING/PICKED 两态确认不可达（dict 死状态）。PENDING 仅由 codegen 默认值承载，CANCELLED 经 useLogicalDelete 承载。
- **影响**：拣货生命周期（PENDING→PICKING→PICKED）缺失，无法经状态机追踪拣货进度。owner doc `state-machine.md` 无拣货独立章节描述。
- **裁决**：按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + mfg A2.6b P1-MA2-036 + hr A2.7a P1-MA2-039~042 + hr A2.7b P1-MA2-043~045 + inv P1-MA2-062 同型（dict 死状态 + BizModel CRUD 桩）。**不破坏移动单主路径**——CRUD 完整可用（PENDING 创建/查询/更新/逻辑删除），拣货执行由 Warehouse Management System (WMS) successor 承载，缺失状态机不产生悬挂数据。
- **修复方式**：MR1 裁决——方案 A（推荐）实现 `startPicking`/`completePicking`/`cancelPicking` BizMutation（PENDING→PICKING→PICKED + PENDING/PICKING→CANCELLED）+ owner doc `state-machine.md` 新增「拣货单状态机」独立章节；方案 B owner doc `state-machine.md` 标注「拣货生命周期 Deferred——WMS 上线时实现」+ 删除 dict PICKING/PICKED 两项。

### P2 watch-only（1 项）

#### P2-MA2-062 state-machine.md 缺 6 状态承载实体独立章节

- **位置**：`docs/design/inventory/state-machine.md`
- **现象**：state-machine.md 仅含「适用对象：库存移动单（StockMove）」+ 末尾「盘点单状态机（独立）」章节。**6 个其他状态承载实体（拣货单/成本调整/到岸成本/调拨单/所有权转移/批次-序列号-预留）无独立章节**——散落在 `costing-methods.md §成本调整/§到岸成本`、`consignment.md §ErpInvOwnershipTransfer`、各 plan 文件中。
- **裁决**：与 purchase P2-MA2-053 + sales + mfg P2-MA2-045/047 + hr P2-MA2-047/052 + assets P2-MA2-059 同型（owner doc 缺独立章节）。无运行时影响（每实体状态机经代码 + plan 文件证据可追溯），仅 owner doc 可读性缺陷。
- **修复方式**：watch-only，MR1 顺手——方案 A（推荐）`state-machine.md` 新增「对象三：拣货单状态机」+「对象四：成本调整双轴状态机」+「对象五：到岸成本审核状态机」+「对象六：调拨单状态机」+「对象七：所有权转移状态机」+「对象八：批次/序列号/预留状态轴」（本审计 §2.2 状态图可直接采用）；方案 B 交叉链接到各 owner doc。

## 5. 并发敏感点（交接 A2.17）

| 敏感点 | 位置 | 风险 | 交接状态 |
|--------|------|------|----------|
| StockBalance 读-改-写无显式锁 | `ErpInvStockMoveProcessor.applyReservation:237-253` + `ErpInvOwnershipTransferProcessor.reclassifyBalance:128-169` | 并发出库/调拨同一 (material×warehouse×location×batch) 余额行可能 silent lost-update | 交接 A2.17（依赖 ErpInvStockBalance versionProp 透明乐观锁 + config `concurrent-deduct-max-retry=5` 重试降级为 detectable conflict，与 mfg/hr 同型） |
| StockTake 并发同库位盘点 | `ErpInvStockTakeBizModel` 无锁 | 多用户同时盘点同库位可能产生重复差异 | 交接 A2.17（本期 StockTake 为 CRUD 桩，无独立并发控制） |
| LandedCost approve 并发同 receiveId | `ErpInvLandedCostProcessor.validateNotAlreadyAllocated:429-444` 查询非锁 | 并发审核同 receiveId 的两张 LandedCost 可能重复分摊（查询时不阻塞另一事务） | 交接 A2.17（approve 经 @BizMutation 事务隔离，但 PRE-APPROVED 窗口期可能双分摊） |
| OwnershipTransfer DONE 并发同余额行 | `ErpInvOwnershipTransferProcessor.reclassifyBalance` 读-改-写 | 并发转移同 source balance 可能 silent lost-update | 交接 A2.17（依赖 ErpInvStockBalance versionProp 乐观锁） |

## 6. 残留风险

1. **批次/序列号/预留状态轴无主动 writer**：3 个实体状态字段是 dict-bound 但无 setStatus 业务路径（CRUD 桩），状态值由 codegen 默认值/外部域/手工维护承载。owner doc 边界已裁定（不是独立状态机对象），但 UI/查询层期望按状态筛选时会失效。归 Deferred successor（与 mfg A2.6b 预测 CONSUMED Deferred 同型）。
2. **today() 状态机角度无风险，成本角度归 P2-MA2-028**：本审计仅复核状态机迁移，FIFO 队列时序归 A2.4 持续 watch-only。
3. **StockTake P1-MA2-062 缺自动生成移动单**：盘点差异经手工处理，破坏追溯链完整性，归 MR1 修复。
4. **PickingOrder P1-MA2-063 拣货生命周期缺失**：WMS successor 上线时实现，归 MR1 裁决。
5. **并发敏感点 4 处交接 A2.17**：本审计不做系统性并发正确性裁决。
6. **LandedCost 与 CostAdjust reverseApprove 目标态不对称**：已裁定为设计并行非分歧（业务语义不同——LandedCost 审核即完成，CostAdjust 审批与业务动作分离）。

## 7. 裁决

### 7.1 10 维度裁决汇总

| 维度 | 裁决 | 关键证据 |
|------|------|----------|
| 1. 状态定义 | ✅ PASS（含 P2-MA1-025 持续） | 4 态 move-status 与 owner doc §1 一致；COUNTING vs CONFIRMED 名漂移持续 watch-only |
| 2. 转换完整性 | ✅ PASS | 移动单生命周期 + 业务单据双轴 + 所有权转移迁移矩阵 1:1 落实 |
| 3. 终端与恢复 | ✅ PASS | DONE/CANCELLED 无出边；DONE 冲销反向单非状态回退 |
| 4. 异常路径 | ✅ PASS（含 P2-MA2-028 状态机角度无升级） | 出库可用量校验前置 + 冲销反向单可用量校验 + 重复触发幂等 + 负库存 config gate |
| 5. 可达性 | ⚠️ PASS（含 P1-MA2-063 新登记） | 移动单全态可达；拣货 PICKING/PICKED 死状态 |
| 6. 角色与权限 | ✅ PASS | @BizMutation 入口权限 + config 保护性默认；负库存管理员级 |
| 7. 外部依赖 | ✅ PASS | IErpInvStockMoveBiz Facade + 存货过账事件解耦；P1-MA1-022 跨域只读维持 |
| 8. TODO 任务策略 | ✅ PASS | businessLinked 自动 DONE 避免沉没 |
| 9. 场景演练 | ⚠️ PASS（含 P1-MA2-062 新登记） | 10 场景覆盖；场景 G（盘点完成）owner doc 声明联动未实现 |
| 10. 与设计文档一致性 | ⚠️ PASS（含 P2-MA2-062 新登记） | owner doc 7 章节一致；6 实体无独立章节散落他处 |

### 7.2 状态机正确性维度 inv 列推进

| 维度（前） | inv 列（前） | inv 列（后） | 推进依据 |
|------------|-------------|-------------|----------|
| 状态机正确性 | ❓ | **⚠️P1(A2.11✅)** | 库存状态机核心契约（移动单生命周期 + 业务单据双轴 + 所有权转移）经证据确认；零 P0；2 项新 P1（P1-MA2-062 StockTake 自动生成移动单缺失 / P1-MA2-063 PickingOrder 死状态）；1 项新 P2 watch-only（P2-MA2-062 owner doc 章节缺失）；7 项已登记 finding 运行时复核无升级（P0-MA1-021 sustained done / P1-MA1-022 维持 / P1-MA2-023/024 维持 / P2-MA1-025 维持 / P2-MA2-026~030 维持 / P2-MA2-028 状态机角度维持）；4 处并发敏感点交接 A2.17 |

### 7.3 Verdict

**Verdict: pass（条件性）**——核心状态机契约（移动单生命周期 + 业务单据双轴 + 所有权转移 + 跨域 Facade + 存货过账解耦 + 出库可用量校验 + DONE 冲销反向单非状态回退）经证据逐项确认；零 P0；2 项新 P1 + 1 项新 P2 已登记待 MR1；7 项已登记 finding 运行时复核无升级；4 处并发敏感点交接 A2.17。

**审查范围**：module-inventory 10 个状态承载实体 + 3 个数值派生实体 + 5 个 owner doc + 2 个 architecture doc。

**可达性摘要**：移动单生命周期全态可达；盘点单 4 态全可达（COUNTING/CONFIRMED 名漂移 P2-MA1-025）；拣货单 PICKING/PICKED 死状态（P1-MA2-063）；批次/序列号/预留状态轴无主动 writer（owner doc 边界 Deferred）。

**角色/权限摘要**：每个迁移绑定执行角色（owner doc §6 表）；危险操作（冲销 + 负库存放行）经权限 + config 双层保护。

**外部依赖摘要**：跨域写经 I*Biz Facade（P0-MA1-021 修复后合规）；跨域只读维持 P1-MA1-022 todo MR1；存货过账事件解耦（容错 + 异常工作台 + DeferredPostingSweepJob 兜底）。

**剩余风险**：详 §6（5 项，均归 MR1 / Deferred successor / A2.17）。

## 8. 引用

- 审计 plan：`docs/plans/2026-07-28-0400-3-audit-remediation-ma2-inventory-state-machine.md`
- 范本（purchase A2.8）：`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`
- 范本（assets A2.10）：`docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`
- 上游 A2.4（成本核算）：`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`
- 上游 MA1 A1.12（含 inv 平台合规 + P0-MA1-021 + P2-MA1-025）：`docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md`
- P0-MA1-021 fix plan（done）：`docs/plans/2026-07-27-1430-1-arm-fix-p0-ma1-021-inv-cost-adjust-voucher-writeback.md`
- owner docs：`docs/design/inventory/{state-machine,trace-chain,cross-domain,consignment}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
- skill：`docs/skills/state-machine-business-review-prompt.md`
- 矩阵更新：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.2`（状态机正确性 + inv 列推进至 ⚠️(P1)）
