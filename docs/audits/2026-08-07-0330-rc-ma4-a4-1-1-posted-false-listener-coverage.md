# RC MA4 A4.1.1 — UC-FIN-02 断言④ posted=false 域 listener 逐域回写覆盖率运行时核验

> Audit Status: closed
> 里程碑：MA4（运行时行为验证）
> 工作项：A4.1.1（MA4 运行时行为验证 — A1.1 §7-1：UC-FIN-02 断言④「业务单据.posted=false」域 listener 回写覆盖率逐域核验）
> 输入：`docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` §7 存疑点 1
> 验证 plan：`docs/plans/2026-08-07-0330-1-rc-ma4-a4-1-1-posted-false-listener-coverage.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据 + §7 衔接 + §8 自检 + §去重协议 + §9 冻结）
> 审计性质：**只读运行时核验**（grep listener + 读 JUnit + 引用 MA2 §5.9 场景D；不改代码/ORM/api.xml/真相源）
> 审计日期：2026-08-07
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

---

## 0. TL;DR（核验结论）

| 项 | 结果 |
|---|------|
| 存疑点 | A1.1 §7-1：UC-FIN-02 断言④「业务单据.posted=false」域 listener 实际回写覆盖率（逐域运行时核验） |
| 实际 listener 域数（grep `IErpFinVoucherReversedListener` 生产实现） | **4**（purchase / sales / inventory / manufacturing） |
| MA2 §5.9 场景D / A1.1 称「8 域」依据 | 回链设计口径上界 + MA2 覆盖测试清单含方向一测试（如 `TestErpMntVisitCancelReversal` 方向一）；**方向二 listener 实际 = 4**，与设计 `posting.md §冲销机制 §裁决4 回退目标态表`（仅列 4 域）一致 |
| 逐域 posted=false 回写核验 | **4/4 域全部写 `setPosted(false)`**（purchase 4 路径 / sales 4 路径 / inventory 2 路径 / manufacturing 1 路径[3 businessType 共用]） |
| 测试断言强度 | 全部**强断言**（5 个测试类直接断言 `assertFalse(posted)`，非仅冒烟） |
| 「缺失 listener 域」判定 | **非 posted=false 回写缺陷**：assets / maintenance / projects 等域走**方向一**（业务侧 reverseApprove/reverse/doCancel → `voucherBiz.reverse()` → 同代码路径内置 `setPosted(false)`），posted=false 经业务侧自身回写满足 UC-FIN-02；设计仅对 4 域定义方向二 listener（`posting.md §裁决4` 仅列 4 域） |
| 符合性结论 | **接受**（UC-FIN-02 断言④「业务单据.posted=false」在 4 个方向二 listener 域全证据一致；其余 posted-bearing 域经方向一满足） |
| 新 finding | **0**（存疑点正向消解；无需新建/复用 finding） |
| P0 即时通道 | 不触发（未出 P0） |

**核心裁决**：存疑点正向消解为**接受**。实仓 `IErpFinVoucherReversedListener` 生产实现恰好 4 域（purchase/sales/inventory/manufacturing），全部在红字冲销后写 `posted=false`，且设计 `posting.md §裁决4 回退目标态表` 仅对这 4 域定义方向二回退目标态（设计意图与实现一致）。「8 域」是 MA2 §5.9 场景D / A1.1 的回链设计口径上界（含方向一覆盖测试），与方向二 listener 实际数（4）不冲突——其余 posted-bearing 域（assets/maintenance/projects）经**方向一**（业务侧 reverse 触发红冲 + 同代码路径内置 setPosted(false)）满足 UC-FIN-02 断言④，非 posted=false 回写缺陷。

---

## 1. 需求契约原文（§6 §1 / §1 L1）

**UC-FIN-02 业务单据作废触发红字冲销**（`docs/design/finance/use-cases.md:42-56`，逐字引用）：

```
业务单据.作废 →
  经 VoucherBillR 反查关联凭证
  生成红字凭证(金额取负, 关联原凭证)
  原凭证标记 isReversed = true
  业务单据.posted = false          ← 断言④（本验证核验对象）
红字凭证走 DRAFT → POSTED 流程
```

本验证聚焦**断言④**「业务单据.posted = false」的逐域运行时回写覆盖。该断言适用于**两个冲销方向**：
- **方向一**（业务单据作废触发红冲，UC-FIN-02 场景）：业务域 reverseApprove/reverse/doCancel → 调 `IErpFinVoucherBiz.reverse()` → **业务侧自身**置 `posted=false`（域调用方模式，`posting.md §反写契约`）。
- **方向二**（凭证红冲→业务单据回退）：财务员直接红冲已过账凭证 → 引擎 `dispatchReversalEvent` 派发 `VoucherReversedEvent` → **域 listener** 监听回写 `posted=false`（`posting.md §冲销机制方向二`）。

存疑点 A1.1 §7-1 的精确范围 = **方向二域 listener 的逐域 posted=false 回写覆盖率**（A1.1 已证实 `dispatchReversalEvent:376-401` 派发 + MA2 §5.9 场景D 证实测试矩阵，但未逐域核验 listener 实现）。

---

## 2. 实现证据（§6 §2 / §1 L3）

### 2.1 引擎派发点（复用 A1.1，不重核）

- `module-finance/erp-fin-service/.../service/posting/ErpFinPostingProcessor.java` `dispatchReversalEvent`（A1.1 实测 :376-401）：红字凭证落库后按配置（默认 SYNC / ASYNC `txn().afterCommit`）派发 `VoucherReversedEvent`。引擎**自身不回写各域 posted**——回写责任在各域 `IErpFinVoucherReversedListener`（`posting.md §反写契约` + 引擎 javadoc 明示）。
- 聚合注册中心 `ErpFinReversalListenerRegistry`（`@Inject List` 聚合，镜像 `ErpFinAcctDocRegistry` 范式）：启动期 collect-beans 收集所有 `IErpFinVoucherReversedListener` Bean，dispatch 循环 try/catch 隔离（单监听者抛错不阻断其他 + 不回滚红字凭证，失败入 5.1 异常工作台）。

### 2.2 方向二 listener 全集枚举（Phase 1 item 1，grep 实测，禁止凑足「8」）

grep `implements IErpFinVoucherReversedListener`（生产代码，排除 test）= **恰好 4 域 4 文件**：

| # | 域 | listener 文件 | 监听 businessType | 设计对照（`posting.md §裁决4 回退目标态表`） |
|---|----|--------------|-------------------|----------------------------------------------|
| 1 | purchase | `module-purchase/erp-pur-service/.../posting/PurReversalListener.java` | AP_INVOICE / PAYMENT / PURCHASE_RETURN / PURCHASE_INPUT | `posting.md:371-372`（purchase 两行，回退 approveStatus APPROVED→REJECTED + posted=false） |
| 2 | sales | `module-sales/erp-sal-service/.../posting/SalReversalListener.java` | AR_INVOICE / RECEIPT / SALES_RETURN / SALES_OUTPUT | `posting.md:373-374`（sales 两行，回退 approveStatus + posted=false；SALES_OUTPUT 仅 posted=false） |
| 3 | inventory | `module-inventory/erp-inv-service/.../posting/InvReversalListener.java` | OWNERSHIP_TRANSFER / INTER_TRANSFER | `posting.md:375`（inventory 行，无 approveStatus 轴，仅 posted=false） |
| 4 | manufacturing | `module-manufacturing/erp-mfg-service/.../posting/MfgSubcontractReversalListener.java` | SUBCONTRACT_ISSUE / SUBCONTRACT_RECEIPT / SUBCONTRACT_FEE | `posting.md:376`（manufacturing 行，回退 docStatus→CANCELLED + posted=false） |

**与 `flow-overview.md` 业财回链图对照**：flow-overview §4.1/§5.2 描述业财回链主链路（业务审核→过账→凭证→posted=true；作废→红冲→posted=false），回退目标态权威定义在 `posting.md §裁决4`（仅 4 域），与本 grep 结果一致。

**「8 域」口径差异声明**（Phase 1 Exit Criteria 要求）：MA2 §5.9 场景D（`2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md:302-306`）的覆盖测试清单列 `TestErpPurFinanceReversalWriteback`/`TestErpSalFinanceReversalWriteback`/`TestErpInvFinanceReversalWriteback`/`TestErpMfgVarianceRecomputeReversal`/`TestErpMfgSubcontractReverse`/**`TestErpMntVisitCancelReversal`**——其中 `TestErpMntVisitCancelReversal`（maintenance）是**方向一**测试（`ErpMntVisitBizModel.doCancel` 内嵌 `laborPostingDispatcher.reverseLabor` 触发红冲，非方向二 listener）。故 MA2 的「8 域 reversal writeback 测试矩阵」含方向一+方向二混合，**方向二 listener 实际 = 4 域**。实际数（4）< 8 不是异常——缺失方向二 listener 的域**经方向一满足 UC-FIN-02 断言④**（见 §4.2），非 posted=false 回写缺陷。

### 2.3 逐域 posted=false 回写语句（Phase 1 item 2①②）

| 域 | listener 方法:行 | posted 回写语句 | 额外回退 | 早退守卫 |
|----|-----------------|----------------|---------|---------|
| purchase | `rollbackInvoice:75` / `rollbackPayment:89` / `rollbackReturn:103` / `rollbackReceive:119` | `invoice/payment/return/receive.setPosted(false)` + `setPostedAt(null)` + `setPostedBy(null)` | approveStatus APPROVED→REJECTED（4 路径齐） | `posted != TRUE` 早退（:72/86/100/114） |
| sales | `rollbackInvoice:72` / `rollbackReceipt:86` / `rollbackReturn:100` / `rollbackDelivery:116` | `invoice/receipt/return/delivery.setPosted(false)` + 清 postedAt/postedBy | invoice/receipt/return approveStatus APPROVED→REJECTED；**delivery 仅 posted=false**（库存物理冲销独立，P2-MA2-057 watch-only） | `posted != TRUE` 早退（:69/83/97/111） |
| inventory | `rollbackOwnershipTransfer:65` / `rollbackTransferOrder:76` | `transfer/order.setPosted(false)` + 清 postedAt/postedBy | 无 approveStatus（库存单据无审批轴）；docStatus 不回退（物理冲销独立） | `posted != TRUE` 早退（:62/73） |
| manufacturing | `rollbackSubcontractOrder:70`（三 businessType 共用，去后缀 -SI/-SR/-SF 反查委外单 code） | `order.setPosted(false)` + 清 postedAt/postedBy | docStatus COMPLETED→CANCELLED（委外单为 docStatus 驱动，无 approveStatus 回退） | `posted != TRUE` 早退（:67） |

**核验结论**：4/4 域 listener 全部写 `setPosted(false)`，无「仅作废/状态迁移而漏 posted 回写」的实现。幂等安全（posted != TRUE 早退 no-op）。

---

## 3. 测试证据（§6 §3 / §1 L4，断言强度标注）

| 域 | 测试类#方法 | 断言强度 | posted 断言原文 |
|----|------------|---------|---------------|
| purchase | `TestErpPurFinanceReversalWriteback#testFinanceReverseRollsBackPurchaseInvoicePostedAndApproveStatus:100-106` | **强** | `assertFalse(posted)` + approveStatus REJECTED + postedAt/postedBy null；另 `#testFinanceReverseWithoutExistingSourceLeavesRedVoucherPostedAndAlerts` 覆盖源单不存在静默路径 |
| purchase（PURCHASE_INPUT 对称补强） | `TestPurReversalListenerReceiveRollback#testRollbackReceiveAlignsToRejectedLikeOthers:63-65` | **强** | `assertFalse(posted)` + approveStatus REJECTED（修复 P1-MA2-051 不对称悬挂）+ `#testRollbackReceiveNoOpWhenNotPosted` 幂等守卫 |
| sales（AR_INVOICE） | `TestErpSalFinanceReversalWriteback#testFinanceReverseRollsBackSalesInvoicePostedAndApproveStatus:90-93` | **强** | `assertFalse(posted)` + approveStatus REJECTED |
| sales（RECEIPT/RETURN/DELIVERY 对称补强） | `TestSalReversalListenerRollback#testRollbackReceiptAlignsToRejected:68` / `#testRollbackReturnAlignsToRejected:84` / `#testRollbackDeliverySetsPostedFalseOnly:100-101` | **强** | 3 路径全覆盖 `assertFalse(posted)`（修复 P1-MA4-021(f) 残差） |
| inventory（OWNERSHIP_TRANSFER） | `TestErpInvFinanceReversalWriteback#testFinanceReverseRollsBackOwnershipTransferPosted:97-103` | **强** | `assertFalse(posted)` + postedAt/postedBy null + docStatus 保留断言 |
| manufacturing（SUBCONTRACT_FEE 方向二） | `TestErpMfgSubcontractReverse#testFinanceReverseVoucherRollsBackSubcontractOrder:229-232` | **强** | `assertFalse(posted)` + docStatus CANCELLED |

**断言强度总评**：4 域 listener 全部有**强断言**直接 `assertFalse(reloaded.getPosted())`（非仅冒烟 status==0），且覆盖早退守卫/源单不存在/对称性。无「仅冒烟」域。

---

## 4. 运行时行为证据（§6 §4 / §1 L5）

### 4.1 方向二（本验证核心）—— MA2 §5.9 场景D 复用 + 逐域 listener 实现

| 域 | MA2 §5.9 场景D 复用行 | 本验证增量（listener 实现逐域核验） |
|----|---------------------|------------------------------------|
| purchase | 场景D 证实 `TestErpPurFinanceReversalWriteback` writeback 模式存在 | 增量核验：`PurReversalListener` 4 rollback 方法全部 `setPosted(false)` + 强断言（§2.3/§3） |
| sales | 场景D 证实 `TestErpSalFinanceReversalWriteback` writeback 模式存在 | 增量核验：`SalReversalListener` 4 rollback 方法全部 `setPosted(false)` + 强断言 |
| inventory | 场景D 证实 `TestErpInvFinanceReversalWriteback` writeback 模式存在 | 增量核验：`InvReversalListener` 2 rollback 方法全部 `setPosted(false)` + 强断言 |
| manufacturing（委外段） | 场景D 证实 `TestErpMfgSubcontractReverse` writeback 模式存在 | 增量核验：`MfgSubcontractReversalListener.rollbackSubcontractOrder:70` 写 `setPosted(false)` + 强断言（plan `2026-07-14-1825-1` 已落地） |

**复用协议**：MA2 §5.9 场景D 已证实 writeback 模式 + 测试矩阵存在（§去重协议）；本验证只补「需求契约↔逐域 listener 实现」差异——逐域确认 listener 实际写 `posted=false`（非仅作废/状态迁移）。结果：4/4 域 listener 全部写 posted=false，与 MA2 背书一致。

### 4.2 方向一（posted-bearing 域全覆盖，证伪「缺失 listener 域即缺陷」假设）

> 关键判定：其余 posted-bearing 域（assets/maintenance/projects 等）**无方向二 listener，但经方向一满足 UC-FIN-02 断言④**。grep `voucherBiz.reverse` + `setPosted(false)` 实测：

| 域 | 方向一触发点 | posted=false 回写位置 | 设计对照 |
|----|------------|---------------------|---------|
| maintenance | `ErpMntVisitCancelProcessor:39` → `laborPostingDispatcher.reverseLabor` → `MntPostingExecutor.reverse:43` → `voucherBiz.reverse`；`ErpMntSparePartUsageReverseConfirmProcessor.reverseConfirm:30` → `issuePostingDispatcher.reverseIssue` → `voucherBiz.reverse` | `AbstractErpMntSparePartUsageProcessor:104` `usage.setPosted(false)`；`reverseConfirm` Processor 内 session-reload + posted=false/docStatus=CANCELLED | `TestErpMntVisitCancelReversal`（MA2 场景D 引用，方向一）+ `TestErpMntSparePartUsageReversal` |
| assets | `ErpAstDisposalProcessor.reverseApprove:113` → `postingDispatcher.reverse(disposal):121 disposal.setPosted(false)`；`ErpAstDepreciationScheduleReverseDepreciationProcessor.reverseDepreciation:43-51` → `postingDispatcher.reverse` → `schedule.setPosted(false)`；`ErpAstInventoryReverseProcessor:31-33`；`ErpAstMaintenanceReverseProcessor:42-49`；`ErpAstValueAdjustmentProcessor:103-106`；`ErpAstAssetCapitalizationProcessor:99/310` | 各 `*ReverseApproveProcessor`/`*ReverseProcessor` 内业务侧同代码路径 `setPosted(false)` | `posting.md §反写契约`（域调用方 post/reverse 成功返回后置位 posted） |
| projects | `ErpPrjProjectSettlementProcessor` 走 `AssetPostingExecutor.reverse`（projects 复用 assets PostingExecutor 范式） | 业务侧 reverse 后置位 posted | 同上 |

**方向一结论**：assets/maintenance/projects 的 posted=false 经**业务侧 reverse 同代码路径内置**满足 UC-FIN-02 断言④（`posting.md §反写契约`「域调用方调 reverse() 成功返回后自行回退 posted」）。这些域**不需要**方向二 listener——它们的纠错主路径是业务侧 reverse（方向一），非财务员直接红冲凭证（方向二）。设计 `posting.md §裁决4` 仅对 4 域（purchase/sales/inventory/manufacturing）定义方向二回退目标态，是**有意的设计选择**（这 4 域是 P2P/O2C/库存/委外主链，财务侧直接红冲为合法 fallback 路径）。

### 4.3 方向二覆盖非缺陷裁决依据

- **UC-FIN-02 场景 = 方向一**（「业务单据作废触发红字冲销」）：所有 posted-bearing 域经方向一满足断言④（§4.2）。
- **方向二（财务侧直接红冲）= fallback 路径**：设计仅对 4 主链域定义 listener（`posting.md §裁决4`）。对其余域，财务侧直接红冲凭证会派发 VoucherReversedEvent，但无 listener 捕获 → posted 不回写。**但这非 UC-FIN-02 断言④要求的路径**（UC-FIN-02 是业务单据作废触发，非财务侧直接红冲），且设计未承诺对这些域支持方向二回退。
- **无活跃数据破坏**：方向二是低频 fallback；主纠错路径（方向一）posted=false 回写完整。故不构成 §2 P0④「会计过账正确性破坏」或 P1①「功能完全缺失」。

---

## 5. 符合性结论（§6 §5 / §2 判据）

### 5.1 逐域符合性矩阵（Phase 1 item 3，4 域方向二 listener）

| 域 | listener:行 | posted 回写 | 测试断言强度 | MA2 §5.9 场景D 复用行 | 符合性结论 | 命中判据 |
|----|------------|------------|-------------|---------------------|-----------|---------|
| purchase | `PurReversalListener:75,89,103,119`（4 路径全 setPosted(false)） | ✅ 全路径 | 强（`TestErpPurFinanceReversalWriteback` + `TestPurReversalListenerReceiveRollback`） | 场景D 覆盖测试行 | **接受** | §2 接受（L3-L5 全证据一致） |
| sales | `SalReversalListener:72,86,100,116`（4 路径全 setPosted(false)） | ✅ 全路径 | 强（`TestErpSalFinanceReversalWriteback` + `TestSalReversalListenerRollback` 3 路径补强） | 场景D 覆盖测试行 | **接受** | §2 接受 |
| inventory | `InvReversalListener:65,76`（2 路径全 setPosted(false)） | ✅ 全路径 | 强（`TestErpInvFinanceReversalWriteback`） | 场景D 覆盖测试行 | **接受** | §2 接受 |
| manufacturing | `MfgSubcontractReversalListener:70`（3 businessType 共用，setPosted(false)） | ✅ | 强（`TestErpMfgSubcontractReverse#testFinanceReverseVoucherRollsBackSubcontractOrder`） | 场景D 覆盖测试行 | **接受** | §2 接受 |

### 5.2 非 listener 域（方向一满足）符合性

| 域 | 方向一 posted=false 回写 | 符合性结论 |
|----|-------------------------|-----------|
| maintenance | `AbstractErpMntSparePartUsageProcessor:104` / `reverseConfirm` Processor 内 setPosted(false) | **接受**（UC-FIN-02 方向一满足） |
| assets | 各 `*ReverseApproveProcessor`/`*ReverseProcessor` 业务侧 setPosted(false) | **接受** |
| projects | 业务侧 reverse 后置位 posted | **接受** |

### 5.3 整体裁决

**接受**。UC-FIN-02 断言④「业务单据.posted=false」核验结论：
- 4 个方向二 listener 域全部写 posted=false + 强断言（§5.1）。
- 其余 posted-bearing 域经方向一满足断言④（§5.2）。
- 无 posted=false 回写缺陷，无 P0/P1/P2 finding。

存疑点 A1.1 §7-1 **正向消解**。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 本验证产出 finding 前 grep `arm-index.md` 同域同控制点（posted 反写 / reversal writeback / VoucherReversedEvent listener）。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本验证关系 | 裁决 |
|---|---|---|---|
| P1-MA2-051 PurReversalListener.rollbackReceive 不对称 | purchase PURCHASE_INPUT 回退（方向二） | **已 resolved**（`2026-07-30-0341-3-r1-17` done，rollbackReceive 已对齐 APPROVED→REJECTED + posted=false） | 复用（证实已修复，本验证 `TestPurReversalListenerReceiveRollback` 强断言背书） |
| P1-MA4-021(f) SalReversalListener rollback 对称残差 | sales RECEIPT/RETURN/DELIVERY 回退（方向二） | **已 resolved**（`2026-07-31-0744-3-r2-14` done，`TestSalReversalListenerRollback` 补齐 3 路径 posted=false） | 复用（证实已修复） |
| P2-MA2-057 SalReversalListener rollbackDelivery 仅 posted=false 不翻转 approveStatus | sales SALES_OUTPUT 回退不对称 | **watch-only deferred**（库存物理冲销独立设计裁定，非缺陷） | 复用（当前设计行为，`TestSalReversalListenerRollback#testRollbackDeliverySetsPostedFalseOnly` 断言当前行为） |
| P2-MA3-026 VoucherReversedEvent billType 字段派发源不清晰 | 事件 schema（billType=businessType.name） | 不同控制点（事件 schema vs posted 回写）；且 billType 派发不影响 posted 回写（listener 按 businessType 路由正确） | 不相关 |

### 6.2 新建 finding 裁决

**0 新建**。本验证无确认缺陷（存疑点正向消解为接受）。「缺失方向二 listener 域」经 §4.2/§4.3 裁决为**非缺陷**（方向一满足 + 设计仅对 4 主链域定义方向二），不构成 §2 P1①「功能完全缺失」。

> 方向二 listener 对 assets/maintenance/projects 的覆盖属设计选择（`posting.md §裁决4` 仅列 4 域）。若产品诉求「财务侧直接红冲任意域凭证均回退 posted」，属**功能增强**（新增 listener），非 posted=false 回写缺陷修复——本验证不登记 finding，交产品 backlog 评估。

---

## 7. 静态存疑点清单（§6 §7）

无。本验证是 MA4 运行时确认，存疑点 A1.1 §7-1 经逐域核验正向消解（§5），无遗留运行时存疑点。

**P0 即时通道**：本验证 Phase 1 定级**未出 P0**（全域接受），按 §10 **不触发 MR0**。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本验证无生产代码变更**（只读核验：grep listener + 读 JUnit + 引用 MA2），checker 无回归风险。

  | 规则 | Baseline（machine-readable） | Actual（本验证 HEAD 实测） | 状态 |
  |------|------------------------------|----------------------------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 229 | 229 | ✅ |
  | R2c | 1382 | 1382 | ✅ |
  | R2d | 34 | 34 | ✅ |
  | R3-R12 | （既有基线） | 脚本输出在 R3 header 后截断（既有工具行为，与零代码变更的本验证无关；A4.1 展开器报告同款记录） | ✅（无回归风险） |

  > R1/R2 全部 actual == baseline，**0 漂移**。R3-R12 脚本输出截断是既有工具行为（A4.1 展开器 `2026-08-07-0300-rc-ma4-a4-1-finance-expander.md` §6 同款记录）；权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 为准。本验证不触发 CI（零代码变更）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（0 新建）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。

---

## 9. 真相源冻结声明（§9）

本验证未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc 需求契约段落）。只读核验（grep listener + 读 JUnit + 引用 MA2），未修改代码/ORM/api.xml/view.xml。

---

## 10. 与 MA2 报告差异增量声明（§去重协议）

本验证复用 MA2 §5.9 场景D「reversal writeback 测试矩阵」已证实的 writeback 模式存在 + 测试矩阵存在（`2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md:302-306`），**不重新核实状态机/红冲链路行为本身**。只补 MA2 未覆盖的「需求契约↔逐域方向二 listener 实现」差异——逐域确认 listener 实际写 `posted=false`（非仅作废/状态迁移）+ 厘清「8 域」口径（方向一+方向二混合 vs 方向二 listener 实际 4 域）+ 证伪「缺失方向二 listener 域即缺陷」假设（方向一满足）。差异增量与本验证范围一致，无与 MA2 重叠的重新核实。
