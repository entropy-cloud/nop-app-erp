# 销售单据状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 销售域状态机与采购域镜像对称（三轴分离：业务/审核/收款），差异点已标注。本文聚焦差异，与采购域相同的机制只摘要引用。

## 适用对象

本状态机适用于销售域的**业务单据**：销售订单、销售出库单、销售发票、收款单、销售退货单。

## 三轴状态分离

| 状态轴 | 字段语义 | 适用单据 | 与采购域差异 |
|--------|----------|----------|--------------|
| 单据状态 | docStatus | 全部 | 无（草稿/已生效/已作废） |
| 审核状态 | approveStatus | 全部 | 无（未提交/已提交/已审核/已驳回） |
| 收款状态 | receivedStatus | 销售发票 | 字段名不同（采购用 paidStatus） |

持久化状态码字典以 `module-sales/model/app-erp-sales.orm.xml` 为准。

## 1. 状态定义（审核轴）

与采购域相同（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED），每个状态表达"等待什么"。详见 `purchase/state-machine.md` 第 1 节，此处不重复。

## 2. 迁移完整性（审批轴）

> 本节只讨论 **approveStatus（审批轴）** 迁移。docStatus（业务生命周期轴）独立演化。新建单据的完整态：`docStatus=DRAFT, approveStatus=UNSUBMITTED`。

迁移拓扑与采购域相同：

```
未提交 (UNSUBMITTED)
  ├─ 提交 → 已提交 (SUBMITTED)
  │            ├─ 审核通过 → 已审核 (APPROVED)
  │            │              ├─ [触发后续业务：扣库存/生成应收凭证]
  │            │              │              └─ posted=true 后物理锁定，纠错需红冲/反审核
  │            │              └─ 反审核 → 已驳回（REJECTED，需先冲销已生成结果）
  │            ├─ 撤销提交 → 未提交（UNSUBMITTED，仅审核人未处理时允许）
  │            └─ 驳回 → 已驳回 (REJECTED)
  │                          └─ 修改后重新提交 → 已提交
  └─ 作废 → 已作废

> **反审核目标态**：与采购域一致，反审核目标态是 `REJECTED`（可重新提交），**不是** `UNSUBMITTED`（初始态）。理由见 `../domain-design-guidelines.md` §16.4。

> **撤销提交约束**：仅提交人可操作；审核人一旦开始审核（nop-wf 已激活），提交人不可再撤回。
```

**SUBMITTED → APPROVED 触发的后续业务**（与采购域的差异）：

| 单据类型 | 审核通过后触发 | 与采购域差异 |
|----------|----------------|--------------|
| 销售出库单 | **校验可用量** → 调用 `IErpInvStockMoveBiz` 生成出库移动单（outgoing） | 采购入库不校验可用量（入库增加）；销售出库**必须校验** |
| 销售发票 | 财务域生成应收凭证（AR_INVOICE） | 方向相反（采购是应付） |
| 收款单 | 财务域生成收款凭证（RECEIPT）+ 核销发票 | 方向相反 |
| 销售退货单 | 调用库存域生成入库移动单（incoming）；若已开票生成红字发票；若已收款生成退款 | 方向相反 |
| 销售订单 | 仅状态推进，不直接触发库存/凭证 | 同采购订单 |

> **实现注记（RC-R1.13，P1-RC-020，L1↔L2 冲突收敛）**：L1（`use-cases.md` UC-SAL-01 ①）字面要求订单审核触发可用量校验，L2 本行「仅状态推进」与其冲突——按需求契约以 L1 为准的裁决，订单审核已落地**可选**可用量预校验：`ErpSalOrderProcessor.validateBusinessRulesForApprove` 经 config `erp-sal.order-availability-check-level`（默认 `OFF`，部署启用时设置；`WARN` 不足记告警放行 / `HARD` 不足拒绝审核）按订单行 `materialId`+`warehouseId`（行级缺失回退订单头）经 `IErpInvStockBalanceBiz` 聚合 `availableQuantity` 预检（只读查询，不做预留/reservation）。**出库审核仍是强制校验点**（`ErpSalDeliveryProcessor.triggerOutgoingMove` → 库存域 `validateAvailable` 抛 `ERR_AVAILABLE_INSUFFICIENT`），订单级预校验只是可选前置门禁——本行语义更新为「订单审核可选预校验（config-gated 默认关）+ 状态推进不强制触发库存」。

## 3. 终态与恢复

与采购域相同：
- 终态：`已审核（APPROVED）`、`已作废`。
- 已审核纠错：反审核（需冲销）或红冲单据。
- 已驳回可恢复。

**销售退货的特殊恢复**：退货涉及退款，若原收款已核销，退货退款需生成红字收款单回退发票收款状态。

## 4. 异常路径

| 异常场景 | 处理 | 与采购域差异 |
|----------|------|--------------|
| **出库可用量不足** | SUBMITTED → APPROVED 时拒绝，整个出库单审核回滚 | **销售独有**（采购入库无此约束） |
| 财务过账失败 | 业务单据已审核 + `posted=false`；异步重试 | 同采购 |
| 并发审核 | 乐观锁 | 同采购 |
| 并发出库扣同一批次 | 乐观锁 + 扣减失败重试 | **销售独有**（采购入库不扣减） |
| 重复审核（幂等） | 已审核单据再次审核为空操作 | 同采购 |
| 收款核销时发票已作废 | 拒绝核销 | 同采购（方向相反） |
| 客户停用后开单 | 通过 `IErpMdPartnerBiz` 校验，拒绝新单 | 同采购（供应商→客户） |
| 退货时库存不足（退货入库场景通常无此问题） | 退货是入库，增加库存，通常无可用量约束 | — |

负库存配置 `erp-inv.allow-negative-stock`（默认 false）开启时跳过出库可用量校验。

## 5. 可达性

与采购域相同：从 UNSUBMITTED 可达所有状态；驳回→重提是合法循环（退出条件为 APPROVED）；无死锁。详见 `purchase/state-machine.md` 第 5 节。

## 6. 角色与权限

| 迁移 | 执行角色 | 与采购域差异 |
|------|----------|--------------|
| 提交 | 销售员 | 采购员→销售员 |
| 审核通过 | 审核人/管理员 | 同 |
| 驳回 | 审核人/管理员 | 同 |
| 反审核 | 管理员（需冲销前置；目标态 REJECTED 非 UNSUBMITTED，见 `../domain-design-guidelines.md` §16.4） | 同 |
| 作废 | 销售员（草稿阶段）/ 管理员（已审核后） | 同 |

职责分离：销售员与审核人不可为同一人（程序级强制：approve 守卫比对 createdBy 与审核人 userId，相等抛 `erp.err.sal.approver-is-creator`；plan 2026-07-31-1023-2 R3.3）。角色名见 `roles-and-permissions.md`。

## 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 销售订单来自外部（如电商对接） | 外部订单转为内部 `销售订单（UNSUBMITTED）` |
| 库存写入（跨工程调用） | 通过 `IErpInvStockMoveBiz` 同步调用，**可用量校验在调用前** |
| 财务凭证生成（异步） | post-commit 事件触发 |

## 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| UNSUBMITTED | 是 | assigned（销售员） |
| SUBMITTED | 是 | pool/assigned（审核人） |
| APPROVED | 否 | — |
| REJECTED | 是 | assigned（销售员） |

## 9. 场景演练

### 场景 A：销售出库 happy path

1. 销售员创建销售订单 → UNSUBMITTED → SUBMITTED → APPROVED。
2. 发货时创建销售出库单 → UNSUBMITTED → SUBMITTED → APPROVED：
   - 先校验可用量（现有量 − 预留量）≥ 出库数量。
   - 调用库存域生成出库移动单（outgoing）。
   - 财务域生成存货估值凭证（结转成本）。
3. 开票 → 创建销售发票 → APPROVED → 财务生成应收凭证（AR_INVOICE）。
4. 收款 → 创建收款单（核销发票）→ APPROVED → 财务生成收款凭证，发票 receivedStatus → 已收清。

### 场景 B：出库可用量不足（异常，销售独有）

1. 销售出库单 SUBMITTED，审核时校验可用量。
2. 可用量 < 出库数量 → 拒绝审核，保持 SUBMITTED。
3. 销售员调整出库数量（分批出库）或等待库存补充后重新提交。

### 场景 C：销售退货退款

1. 客户退货 → 创建销售退货单 → APPROVED：
   - 调用库存域生成入库移动单（客户退回）。
   - 若已开票 → 生成红字销售发票冲销应收。
   - 若已收款 → 生成红字收款单，回退发票收款状态。
2. 退款流程由财务域处理。

### 场景 D：赠品与折扣处理

1. 销售订单含赠品行（金额 0、数量计入）与折扣行。
2. 审核时校验：赠品可用量是否充足（赠品也要扣库存）。
3. 折扣影响应收金额（价税分离计算）。

> **实现注记（RC-R1.14 + RC-R1.15）**：
> - **价税分离**：`ErpSalOrderBizModel.recomputeLineAmount` 按 L1 公式重算行级 `taxAmount = net × rate / (1+rate)`（scale=4 HALF_UP，rate=taxRate/100）+ `amountWithTax = net + taxAmount`；`recomputeOrderTotals` 头级 `totalTaxAmount`/`totalAmountWithTax` = Σ 新行值（恒等式 `totalAmountWithTax = totalAmount + totalTaxAmount`）。零税率/null rate 行 `taxAmount=0`。
> - **最低价校验**：`ErpSalOrderBizModel.validatePromotionPrices`（protected step，`persistPricingResult` 落地后逐行触发）复用 master-data `IErpMdMaterialSkuBiz.validatePrice` 三级语义（OFF 放行 / WARN 放行带 LOG.warn / HARD 抛 `ERR_PRICE_BELOW_MIN` propagate + @BizMutation 事务回滚）。`finalPrice = line.amount/line.quantity`（促销后净单价）；`materialCategoryId` 经 `line.getMaterial().getCategoryId()` 解析。**赠品行（amount==0）显式跳过**——L1 UC-SAL-08 要求赠品可成功生成，HARD 级别下不排除将误拒含赠品促销。

## 10. 与设计文档一致性

- 销售域与采购域的对称性见 `sales/README.md`。
- 状态码持久化值归 `module-sales/model/app-erp-sales.orm.xml`。
- 业财打通见 `finance/posting.md`。
- 可用量校验规则见 `inventory/cross-domain.md`。

## 实现模式与守卫边界

> 计划 `2026-07-30-0341-3-r1-17`（P1-MA2-056/057）补注：销售域审批轴动作分两类实现，守卫边界不同。

**PROC 路径**（Delivery/Quotation/Return/Order/Invoice/Receipt 的 submitForApproval/approve/reject/reverseApprove）：由 `ErpSal*Processor` 编排，含完整业务守卫（`validateNotCancelled` + `requireCustomerActive` + `requireLinesNonEmpty` + 状态校验）。

**INLINE 路径**（Contract 全 5 动作 + 6 实体的 withdrawApproval）：直接在 xbiz `<source>` 脚本中实现，守卫边界为 **isCancelled + src 状态校验**（`entity.docStatus === 'CANCELLED'` 阻断 + `approveStatus` 源态校验）。INLINE **不补** requireCustomer/Lines 等业务守卫——这些在 submit 时点已门控，审批时点重复校验冗余。

> 残留风险：INLINE 守卫与 PROC 守卫非完全对齐。若被误用导致 CANCELLED 单据业务规则绕过，successor 迁移到完整大 Processor。

## 收款状态机（销售发票，派生状态）

与采购域付款状态对称：

```
未收 (UNRECEIVED)
  ├─ 收款核销（累计 < 发票金额）→ 部分 (PARTIAL)
  │                                 ├─ 继续收款（累计 ≥ 发票金额）→ 已收清 (RECEIVED)
  │                                 └─ 红冲核销 → 未收或保持部分
  └─ 收款核销（累计 ≥ 发票金额）→ 已收清 (RECEIVED)
```

收款状态是派生状态，由系统根据"累计已核销金额 / 发票金额"自动计算。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- **出库可用量校验**是否在审核时拦截（销售独有，最易遗漏）。
- 并发出库扣减同一批次的乐观锁是否落实。
- 退货退款路径是否完整（红字收款单 + 回退发票状态）。
- 赠品的库存扣减是否被忽略（赠品金额为 0 但仍扣库存）。
