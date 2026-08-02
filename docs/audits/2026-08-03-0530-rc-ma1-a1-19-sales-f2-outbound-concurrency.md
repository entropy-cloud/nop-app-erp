# RC MA1 A1.19 — sales-F2 出库与并发 需求-实现符合性审计

> Audit Status: closed
> 里程碑：MA1（需求-实现符合性层 / 五级追踪矩阵维度）
> 工作项：A1.19（MA1 需求追踪矩阵审计 — sales-F2 出库与并发）
> 审计 plan：`docs/plans/2026-08-03-0407-2-rc-ma1-a1-19-sales-f2-outbound-concurrency.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）
> L1 真相源：`docs/design/sales/use-cases.md`（UC-SAL-02/03/10，3 UC）
> L1 锚点清单：`docs/audits/rc-requirement-baseline-inventory.md` §sales + §切片索引 A1.19
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 审计日期：2026-08-03
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 无 MR0 即时通道触发 |
| **P1**（新登记） | **0** | 无新 P1（UC-SAL-02 订单级可用量校验缺失**复用** P1-RC-020 同根因同控制点，已在 A1.18 登记） |
| **P1**（复用既有） | **1** | P1-RC-020（UC-SAL-01 订单级可用量校验缺失，A1.18 已登记）— UC-SAL-02 :62-66 是同根因同控制点的回滚场景断言，**追加 RC 视角注记**到既有 finding 行 |
| **P2**（新登记） | **3** | P2-RC-019（UC-SAL-03 行级 `deliveredQuantity` 列存在零生产 writer）/ P2-RC-020（UC-SAL-03 1 行×2 分批(60+40) 测试缺失）/ P2-RC-021（UC-SAL-10 销售级并发 seam 测试缺失）→ successor watch-only |
| **接受**（符合需求契约） | 多数验收标准 | UC-SAL-02 出库级回滚 + UC-SAL-03 头级 deliveryStatus rollup + 发票/收款 receivedStatus 派生 + UC-SAL-10 库存域乐观锁 + 重试 + UK 兜底（A2.17 强证） |
| MA2 既有行为证据复用 | A2.17 + A2.9 + O2C | 无升级（详见 §4 / §9） |

**整体裁决**：A1.19 切片 3 UC 五级追踪矩阵填齐。**关键发现 = UC-SAL-02 订单级可用量校验缺失（L1↔L2/L3 真相源冲突）**：L1（`use-cases.md:62-66`）逐字「订单.审核通过 触发: 调用库存校验可用量」，L2（`state-machine.md:52,56`）+ L3（`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 仅 requireCustomerActive + creditLimitChecker.check，`@Inject` 簇 :54-85 无 `IErpInvStockMoveBiz`/`IErpInvStockBalanceBiz`）均说校验在**出库审核**环节（`ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `IErpInvStockMoveBiz.generateMove` → `ErpInvStockMoveProcessor.doConfirm:86-98` → `validateAvailable:116-136` 不足抛 `ERR_AVAILABLE_INSUFFICIENT`）。按方法论 §4 Q1 裁决**L1 胜**——L2 推定已向实现妥协；按 §9 冻结条款记入报告**不直改真相源**（修复方向须人工裁决：补订单级校验 OR 修 L1 措辞为出库级）。**与 A1.18 P1-RC-020 同根因同控制点**（同一 `ErpSalOrderProcessor.validateBusinessRulesForApprove` 站点，UC-SAL-01 :27 行为链路 step 1 与 UC-SAL-02 :62-66 是同一控制点的不同 UC 投影）→ 按 §7 复用规则**不新建** finding，仅向 P1-RC-020 既有 arm-index 行追加 A1.19 RC 视角注记。

**UC-SAL-03 派生字段断言不可达**（G2）：`app-erp-sales.orm.xml:402` `<column name="deliveredQuantity" ... defaultValue="0">` 列存在，但 grep 全仓 `setDeliveredQuantity` 生产代码 **零 writer**（仅生成 bean 框架 setter `_ErpSalOrderLine.java:1228` + 生成 GraphQL DTO setter + 2 MFG 测试 seed ZERO）→ L1 断言「订单行.已出库数量 == 60→100」**不可满足**。头级 `deliveryStatus`（UNDELIVERED/PARTIAL/DELIVERED）+ 发票/收款 `receivedStatus`（UNRECEIVED/PARTIAL/RECEIVED）派生**正确**（`rollupOrderDeliveryStatus:270-310` + `ReceiptSettler.recomputeInvoiceReceived:161-177`）。结构等价于 A1.16 P2-RC-013（purchase `receivedQuantity` 列存在零 writer），按 §2 P2① 定级。

**UC-SAL-10 库存域乐观锁完备**（接受）：`ErpInvStockBalance.version`（`app-erp-inventory.orm.xml:389`，`versionProp="version"` :369）+ `StockMoveBookkeeper.updateBalanceWithRetry:255-328`（`dao.tryUpdateWithVersionCheck:271` `UPDATE WHERE id=? AND version=?` 0 行→冲突；重试循环 :264-327，上限 `erp-inv.concurrent-deduct-max-retry` 默认 5 `ErpInvConstants.java:18-19`；冲突 `recordOptimisticLockFailure:290` 计量 + 驱逐 + 重载重算；耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`/`ERR_INV_BALANCE_INSERT_CONFLICT`）+ `UK_INV_STOCK_BALANCE_NATURAL`（`app-erp-inventory.orm.xml:415`，P0-MA2-020 已落地）兜底 INSERT 竞态。销售出库侧无独立锁，完全委托库存域同一 `@BizMutation` 事务。L4 `TestErpInvConcurrentDeduct` 6 测试（3 单线程版本偏斜模拟 + 3 真实多线程 ExecutorService+CountDownLatch）强覆盖；**销售级并发 seam 测试为零**（`module-sales` grep `Executors|CountDownLatch` 零命中）→ P2-RC-021（断言强度，inventory 域已强覆盖，sales seam 缺）。

**MA2 文本陈旧对账**（G5）：`2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md:326-327` 称「无并发场景集成测试」与现存 `TestErpInvConcurrentDeduct`（6 测试，引用 plan `2026-07-07-0024-2`）矛盾——MA2 审计文本陈旧，对账结论记入 §9（不新建 finding ID，纯文本对账）。本审计**不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/sales/use-cases.md`（L1 权威真相源，方法论 §4）。以下逐 UC 逐字引用验收标准原文，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。

### UC-SAL-02 出库可用量不足审核回滚(销售独有)（`use-cases.md:56`）

```
场景:订单审核时库存不足以满足出库,审核被拒绝并回滚。

可验证断言(见 state-machine.md §4 异常路径):

订单.审核通过 触发:
  调用库存校验可用量(见 ../inventory/cross-domain.md §余量校验)
  若 可用量 < 出库数量:
    审核失败,回滚(订单保持 SUBMITTED,不前推)
    不生成出库移动单
    库存余额不变

// 与采购入库的对比:采购入库只增加库存(不校验余量),销售出库必须校验
```

> 涉及机制：`state-machine.md §4`、`../inventory/cross-domain.md §余量校验`

### UC-SAL-03 分批出库与部分收款（`use-cases.md:76`）

```
场景:订单分多次出库,发票按出库开票,收款分次核销。

行为链路:

订单(数量=100) 审核通过
出库1(数量=60) → 发票1(60) → 收款1 核销发票1
出库2(数量=40) → 发票2(40) → 收款2 核销发票2

可验证断言:

订单行.已出库数量 == 60   // 第一次后
订单行.已出库数量 == 100  // 第二次后(派生)
发票1.收款状态 = 部分 (若收款1 < 发票1金额)
发票2.收款状态 = 已收清 (若收款2 = 发票2金额)
```

> 涉及机制：`state-machine.md §收款状态机(派生)`

### UC-SAL-10 并发出库扣同一批次（`use-cases.md:216`）

```
场景:两个销售出库同时扣减同一批次库存,乐观锁保证一致性。

可验证断言(见 state-machine.md §4):

并发出库A、B 扣同一批次:
  一个成功(扣减), 另一个乐观锁冲突 → 重试或失败
最终 批次.现有量 == 初始 - A数量 - B数量 (不超扣)
```

> 涉及机制：`state-machine.md §4`、`../inventory/state-machine.md §4`

---

## 2. 实现证据（L3，`file:line`，跨域调用链列全）

### 2.1 UC-SAL-02 出库可用量校验路径（L1↔L3 关键偏离点 G1）

**订单审核侧（L1 字面控制点；L3 不触发库存校验）**：

- `module-sales/erp-sal-service/.../processor/ErpSalOrderApproveProcessor.java:48-51`
  ```java
  @Override
  protected void validateBusinessRules(ErpSalOrder entity, IServiceContext context) {
      processor.validateBusinessRulesForApprove(entity, context);
  }
  ```
- `module-sales/erp-sal-service/.../processor/ErpSalOrderProcessor.java:166-170`（订单审核业务规则仅客户激活 + 信用额度）
  ```java
  protected void validateBusinessRulesForApprove(ErpSalOrder order, IServiceContext context) {
      requireCustomerActive(order, context);
      creditLimitChecker.check(order.getCustomerId(), order.getTotalAmountWithTax(), order.getExchangeRate(),
              order.getCode(), context);
  }
  ```
- `ErpSalOrderProcessor.java:54-85` `@Inject` 簇：`IDaoProvider` / `IErpMdPartnerBiz` / `CreditLimitChecker` / `IErpFinIntercompanyTransferBiz` / `IErpFinBudgetCommitmentBiz` / 6 个 per-mutation Processor — **无** `IErpInvStockMoveBiz` / `IErpInvStockBalanceBiz` 注入。

**出库审核侧（L3 实际控制点；L2 state-machine.md:52 一致）**：

- `module-sales/erp-sal-service/.../processor/ErpSalDeliveryApproveProcessor.java:37`
  ```java
  ErpInvStockMove move = processor.triggerOutgoingMove(delivery, context);
  ```
- `module-sales/erp-sal-service/.../processor/ErpSalDeliveryProcessor.java:241-245`（出库审核触发跨域库存移动单生成）
  ```java
  protected ErpInvStockMove triggerOutgoingMove(ErpSalDelivery delivery, IServiceContext context) {
      List<ErpSalDeliveryLine> lines = loadLines(delivery.getId());
      StockMoveRequest request = stockMoveBuilder.build(delivery, lines, context);
      return stockMoveBiz.generateMove(request, context);
  }
  ```
- 跨域调用链：`IErpInvStockMoveBiz.generateMove` → `ErpInvStockMoveProcessor.doConfirm:86-98`（DRAFT→CONFIRMED 前调 `validateAvailable`）→ `validateAvailable:116-136`
  ```java
  // ErpInvStockMoveProcessor.java:86-98
  protected void doConfirm(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, IServiceContext context) {
      ...
      validateAvailable(move, lines, context);
      applyReservation(move, lines, true, context);
      move.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
      moveDao().saveOrUpdateEntity(move);
  }
  // ErpInvStockMoveProcessor.java:116-136 关键拒绝分支
  protected void validateAvailable(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, IServiceContext context) {
      if (isNegativeStockAllowed()) { return; }
      if (!reservesOnConfirm(move.getMoveType())) { return; }
      ...
      if (available.compareTo(required) < 0) {
          throw new NopException(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT)
                  .param(...).param(ErpInvErrors.ARG_REQUIRED, required.toPlainString());
      }
  }
  // ErpInvStockMoveProcessor.java:242-248 销售独有性
  protected boolean reservesOnConfirm(String moveType) {
      if (moveType == null) { return false; }
      return Objects.equals(moveType, ErpInvConstants.MOVE_TYPE_OUTGOING)
              || Objects.equals(moveType, ErpInvConstants.MOVE_TYPE_INTERNAL_TRANSFER);
  }
  ```
- `module-inventory/erp-inv-service/.../ErpInvErrors.java:49`：`ERR_AVAILABLE_INSUFFICIENT = ErrorCode.define("erp.err.inv.available-insufficient", "可用量不足...")` — 裸抛（无 try/catch）→ `@BizMutation` 事务回滚 → 出库单保持 SUBMITTED + 无 DONE 移动单 + 余额不变（与 UC-SAL-02 断言②③④ 字面一致，但控制点在出库审核而非订单审核）。
- 负库存配置 `erp-inv.allow-negative-stock`（默认 false）：`validateAvailable:117` `if (isNegativeStockAllowed()) return;` 跳过校验。

### 2.2 UC-SAL-03 分批出库派生（G2 关键缺口 + 头级/发票侧正确）

**行级 `deliveredQuantity` 派生（关键缺口 G2 — 列存在但零生产 writer）**：

- `module-sales/model/app-erp-sales.orm.xml:386` `<entity ... versionProp="version" ...>` 实体头
- `module-sales/model/app-erp-sales.orm.xml:402` `<column name="deliveredQuantity" code="DELIVERED_QUANTITY" displayName="已出库数量" propId="14" domain="quantity" stdSqlType="DECIMAL" precision="20" scale="4" defaultValue="0" ...>`
- **grep 全仓 `setDeliveredQuantity` java（repo-wide）实测 6 命中**：
  | 文件 | 性质 |
  |---|---|
  | `module-sales/erp-sal-dao/.../dao/entity/_gen/_ErpSalOrderLine.java:632` | 生成框架反序列化 |
  | `module-sales/erp-sal-dao/.../dao/entity/_gen/_ErpSalOrderLine.java:1228` | 生成框架 setter |
  | `module-sales/erp-sal-api/.../api/beans/ErpSalOrderLineInputBean.java:206` | 生成 GraphQL Input DTO setter |
  | `module-sales/erp-sal-api/.../api/beans/ErpSalOrderLineOutputBean.java:207` | 生成 GraphQL Output DTO setter |
  | `module-manufacturing/erp-mfg-service/src/test/.../TestErpMfgMrpEngine.java:350` | MFG 测试 seed `BigDecimal.ZERO` |
  | `module-manufacturing/erp-mfg-service/src/test/.../TestErpMfgMrpEndToEnd.java:321` | MFG 测试 seed `BigDecimal.ZERO` |
- **零生产 service / processor / BizModel writer**。`ReturnQtyValidator` 经完整阅读其方法体**不是** writer（仅聚合 `ErpSalReturnLine.qty` 与 `ErpSalDeliveryLine.quantity` 比对，从不调 `setDeliveredQuantity`）— 校正 plan baseline 的措辞（baseline 称其为 reader 实际为不读不写）。

**头级 `deliveryStatus` rollup（正确派生）**：

- `module-sales/erp-sal-service/.../processor/ErpSalDeliveryProcessor.java:270-310` `rollupOrderDeliveryStatus` 内部聚合 `Σ approved DeliveryLine.qty by orderLineId`（:280-287）→ 3 态字符串（:301-308）→ 调 `orderBiz.updateDeliveryStatus(orderId, rolled, context)`（:309）。
  ```java
  // 关键节选 :301-309
  String rolled;
  if (allFullyDelivered) {
      rolled = ErpSalConstants.DELIVERY_STATUS_DELIVERED;
  } else if (anyDelivered) {
      rolled = ErpSalConstants.DELIVERY_STATUS_PARTIAL;
  } else {
      rolled = ErpSalConstants.DELIVERY_STATUS_UNDELIVERED;
  }
  orderBiz.updateDeliveryStatus(orderId, rolled, context);
  ```
- `module-sales/erp-sal-service/.../entity/ErpSalOrderBizModel.java:240-254` `updateDeliveryStatus` 仅写订单头 `deliveryStatus`（不写 orderLine.deliveredQuantity）：
  ```java
  @Override
  @BizAction
  public void updateDeliveryStatus(@Name("orderId") Long orderId,
                                   @Name("deliveryStatus") String deliveryStatus,
                                   IServiceContext context) {
      if (orderId == null) { return; }
      ErpSalOrder order = get(String.valueOf(orderId), true, context);
      if (order == null) { return; }
      order.setDeliveryStatus(deliveryStatus);
      updateEntity(order, null, context);
  }
  ```

**发票/收款侧 `receivedStatus` 派生（UC-SAL-03 后半，正确）**：

- `module-sales/erp-sal-service/.../entity/ReceiptSettler.java:161-177` `recomputeInvoiceReceived` 3 态派生：
  ```java
  private void recomputeInvoiceReceived(Long invoiceId) {
      ormTemplate.flushSession();
      ErpSalInvoice invoice = daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoiceId);
      BigDecimal received = sumInvoiceLines(invoiceId);
      invoice.setReceivedAmount(received);
      BigDecimal withTax = nz(invoice.getTotalAmountWithTax());
      String status;
      if (received.signum() <= 0) {
          status = ErpSalConstants.RECEIVED_STATUS_UNRECEIVED;
      } else if (received.compareTo(withTax) >= 0) {
          status = ErpSalConstants.RECEIVED_STATUS_RECEIVED;
      } else {
          status = ErpSalConstants.RECEIVED_STATUS_PARTIAL;
      }
      invoice.setReceivedStatus(status);
      daoProvider.daoFor(ErpSalInvoice.class).updateEntity(invoice);
  }
  ```

### 2.3 UC-SAL-10 库存域乐观锁（完备，行为接受）

**版本字段**：`module-inventory/model/app-erp-inventory.orm.xml:369` `versionProp="version"`，`:389` `<column name="version" ... stdSqlType="INTEGER" ... defaultValue="0">`

**UK 兜底**：`module-inventory/model/app-erp-inventory.orm.xml:415`
```xml
<unique-key name="UK_INV_STOCK_BALANCE_NATURAL" constraint="UK_INV_STOCK_BALANCE_NATURAL"
            columns="orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId"/>
```
（P0-MA2-020 已落地，INSERT 竞态兜底）

**扣减路径**：`module-inventory/erp-inv-service/.../stock/StockMoveBookkeeper.java:255-328` `updateBalanceWithRetry`
```java
// :255-264 方法入口 + 重试上限
public ErpInvStockBalance updateBalanceWithRetry(ErpInvStockBalance initialBaseline,
                                                 Consumer<ErpInvStockBalance> applyDelta) {
    IOrmEntityDao<ErpInvStockBalance> dao = (IOrmEntityDao<ErpInvStockBalance>)
            daoProvider.daoFor(ErpInvStockBalance.class);
    int maxRetry = AppConfig.var(ErpInvConstants.CONFIG_CONCURRENT_DEDUCT_MAX_RETRY,
            ErpInvConstants.CONCURRENT_DEDUCT_MAX_RETRY_DEFAULT);
    ErpInvStockBalance current = initialBaseline;
    int attempts = 0;

    // :265-286 重试循环 + tryUpdateWithVersionCheck
    while (true) {
        applyDelta.accept(current);
        OrmEntityState state = current.orm_state();
        boolean conflict;
        if (state == OrmEntityState.MANAGED) {
            conflict = !dao.tryUpdateWithVersionCheck(current);  // UPDATE WHERE id=? AND version=?
        } else if (state == OrmEntityState.TRANSIENT) {
            ...
        }
        if (!conflict) { return current; }

        // :290-326 冲突计量 + 驱逐 + 重载重算
        ErpInvConcurrencyMetrics.recordOptimisticLockFailure(null);
        attempts++;
        if (attempts > maxRetry) {
            ErpInvConcurrencyMetrics.recordOptimisticLockFailureExhausted(null);
            throw buildConflictExhaustedEx(state, current, attempts);
        }
        ...
        if (state == OrmEntityState.MANAGED) {
            current = dao.requireEntityById(current.orm_id());
        } else {
            current = findBalanceByNaturalKey(orgId, materialId, skuId, warehouseId,
                    locationId, batchNo, ownerId);
        }
    }
}
```

**配置 + 错误码**：
- `module-inventory/erp-inv-service/.../ErpInvConstants.java:18-19`
  ```java
  String CONFIG_CONCURRENT_DEDUCT_MAX_RETRY = "erp-inv.concurrent-deduct-max-retry";
  int CONCURRENT_DEDUCT_MAX_RETRY_DEFAULT = 5;
  ```
- `module-inventory/erp-inv-service/.../ErpInvErrors.java:49,54,61`：`ERR_AVAILABLE_INSUFFICIENT` / `ERR_INV_CONCURRENT_DEDUCT_CONFLICT` / `ERR_INV_BALANCE_INSERT_CONFLICT`

销售出库侧无独立锁，完全委托库存域 `updateBalanceWithRetry`（同一 `@BizMutation` 事务，无 REQUIRES_NEW 隔离）。

---

## 3. 测试证据（L4，注明断言强度）

### 3.1 UC-SAL-02 测试证据

**`module-sales/erp-sal-service/src/test/.../TestErpSalDeliveryStockMove.java`**（出库级强覆盖）：

| 方法 | 行号 | 断言强度 | 关键断言 |
|---|---|---|---|
| `testApproveInsufficientAvailableRollsBack` | :121-155 | **强** | `assertEquals(ERR_AVAILABLE_INSUFFICIENT, ...)` (:140) + `approveStatus=SUBMITTED` (:145) + `posted=false` (:147) + 无 DONE 移动单 (:150) + 余额不变 = 5 (:153) |
| `testNegativeStockConfigAllowsShortage` | :179-210 | **强** | `erp-inv.allow-negative-stock=true` + 审核 APPROVED (:197) + DONE 移动单 (:201) + 余额 = -5 (:205) — 负库存配置分支 |

**`module-sales/erp-sal-service/src/test/.../TestErpSalOrderApproval.java`**（订单级零覆盖）：

- 类 Javadoc（:33-42）逐字：「Phase 1 服务层集成测试：销售订单三轴审批状态机（**仅状态推进，不触发库存/凭证**）+ 客户启用校验 + 客户信用额度校验（SOFT_WARNING 放行 / HARD_BLOCK 拒绝 / outstanding 口径）」。
- 11 `@Test` 方法全为状态迁移 / 客户激活 / 信用额度（SOFT/HARD/SPECIAL_APPROVAL），**零库存可用量断言**。
- **结论**：UC-SAL-02 订单级回滚未测试；UC-SAL-02 出库级回滚行为本身已强覆盖（`testApproveInsufficientAvailableRollsBack` 强）。

### 3.2 UC-SAL-03 测试证据

| 测试类#方法 | 行号 | 断言强度 | 关键断言 |
|---|---|---|---|
| `TestErpSalDeliveryStockMove#testDeliveryStatusRollupToOrder` | :212-247 | **强（头级）** | 头级 `order.deliveryStatus` PARTIAL (:232) → DELIVERED (:245)；**测 2 行×1 出库/行**（非 L1 字面 1 行×2 分批(60+40)），**从不断言 `orderLine.deliveredQuantity`** |
| `TestErpSalOrderToDeliveryEnd#testOrderToDeliveryToEnd` | :69-125 | **强（单次满量）** | 出库满量 10/库存预置 20 → APPROVED + DONE + 余额=10 (:101) + 主营业务成本 50 (:107) + 订单头 DELIVERED (:113)；反审核后余额恢复 20 (:120)；**单次满量**非分批 |
| `TestErpSalOrderToCashEnd`（部分核销 60/100） | :167-173 | **强（发票侧）** | 部分核销 60 → `invoice.receivedAmount=60` (:171) + `invoice.receivedStatus=PARTIAL` (:172) + `receipt.writtenOffStatus=PARTIAL` (:173) — UC-SAL-03 后半断言强 |

**结论**：UC-SAL-03 L1 字面「订单行.已出库数量 == 60→100」**无测试断言**（与列不被写入互为因果 — 即使写测试也会失败）；头级 + 发票侧派生强覆盖。

### 3.3 UC-SAL-10 测试证据

**`module-inventory/erp-inv-service/src/test/.../TestErpInvConcurrentDeduct.java`**（库存域 6 测试，3 真实多线程 + 3 单线程模拟）：

| # | 方法 | 行号 | 机制 | 断言强度 |
|---|---|---|---|---|
| 1 | `testConcurrentDeductRetrySucceeds` | :87 | 单线程版本偏斜模拟（outer session 缓存陈旧 baseline，inner `runInNewSession` 推进版本，再 `updateBalanceWithRetry` 触发重试） | **强** |
| 2 | `testConcurrentDeductRetryExhaustedThrows` | :131 | 单线程版本偏斜模拟（max-retry=0，断言 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`） | **强** |
| 3 | `testConcurrentDeductNoOversell` | :179 | **真实多线程**：`Executors.newFixedThreadPool(3)` + `CountDownLatch` start/done 门控；3 线程 × 各扣 3 from initial 10 → 最终 1 | **强**（不超卖） |
| 4 | `testConcurrentDeductWithNegativeStockAllowed` | :189 | **真实多线程**：2 线程 × 各扣 2 from initial 2，`allow-negative-stock=true` → 最终 -2 | **强**（负库存并发） |
| 5 | `testConcurrentFirstMoveSameDimensionThrowsAndRetries` | :208 | 单线程 INSERT UK 冲突模拟（同自然键 SAVING 候选） | **强** |
| 6 | `testConcurrentFirstMoveMultiThreadNoDuplicateRows` | :251 | **真实多线程**：2 线程同自然键并发 SAVING 候选，断言无重复行 | **强**（UK 兜底） |

**销售级并发测试**：repo-wide `rg "Executors|newFixedThreadPool|newCachedThreadPool|CountDownLatch" module-sales/` = **0 命中**；`ErpSalDelivery__approve` 在 7 个 sales 测试文件中均经 `IGraphQLEngine.executeRpc` 单线程调用，**无销售级并发 seam 测试**。

**E2E**：`tests/e2e/o2c-chain.spec.ts` 仅单次满量快乐路径，无不足/分批/并发场景（MA5 §A5.6 评级 strong 但覆盖面正向）。

---

## 4. 运行时行为证据（L5，复用 MA2/E2E + 本切片差异）

### 4.1 复用 MA2 已证实行为（§去重协议，不重新核实）

**`2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（A2.17，UC-SAL-10 主证据）**：
- §2 :65,68（sales 27/27 + inventory 31/31 实体 100% `versionProp` 覆盖）。
- §3 :100（`tryUpdateWithVersionCheck` + 重试 sustained 行为）。
- §4 :119-121（UC-INV-08 超卖 / UC-SAL-10 双重核销 / UC-SAL-10 并发扣批次 全部 sustained 缺口证伪）。
- §13 :296-298（UC-SAL-10 PASS）。
- P0-MA2-020（`:260-268` 库存余额自然键 UK）已落地，本切片 HEAD 复核 `app-erp-inventory.orm.xml:415` `UK_INV_STOCK_BALANCE_NATURAL` 物理存在。
- **G5 对账点**：A2.17 `:326-327` 文本称「无并发场景集成测试」与现存 `TestErpInvConcurrentDeduct`（6 测试，plan `2026-07-07-0024-2` 后落地）矛盾 → **审计文本陈旧**（A2.17 撰写于测试落地前/同提交切片），对账结论记入 §9。

**`2026-07-28-0400-arm-ma2-sales-state-machine.md`（A2.9，sales 7 实体状态机）**：
- §维度2 :111（出库 approve 可用量校验前置 PASS）。
- §维度4 :130（出库可用量不足 PASS）。
- 裁决 ⚠️(P1)（P1-MA2-056/057 Contract INLINE 守卫，归 A1.21 切片；与本切片不同控制点）。
- **本切片差异增量**：A2.9 以 L2（state-machine.md）为准证实出库级校验 PASS，**未从 L1 字面「订单审核触发」视角审视控制点偏离** — 本切片 §5 / §9 补需求视角差异（与 A1.18 P1-RC-020 同根因同控制点）。

**`2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（O2C 全链）**：
- §可用量 :39-44（链路 `triggerOutgoingMove → generateMove → doConfirm → validateAvailable`，错误传播 PASS）。
- §UC-SAL-10 :183,195（乐观锁基础具备，并发扣批次交接 A2.17）。
- **本切片差异增量**：O2C 报告未标订单级偏离。

**`2026-07-06-use-case-implementation-audit.md:71-86`（早期 UC 覆盖调查）**：
- UC-SAL-02 ✅（出库级，**未核订单级偏离**）。
- UC-SAL-03 🔶（deliveredQty 未测试）。
- UC-SAL-10 ❌（乐观锁未测试） — 与 A2.17 矛盾，对账记入 §9。
- 本切片须声明与上述报告的差异增量（订单级校验缺失 / deliveredQuantity 从不写入 / 销售级并发测试缺失 / 审计文本陈旧对账）。

### 4.2 L5 行为裁决（逐 UC）

- **UC-SAL-02 出库级回滚行为**：L3+L4+MA2 A2.9 三重证实行为正确（不足→抛异常→@BizMutation 回滚→出库单 SUBMITTED + 无 DONE + 余额不变）。**接受**（行为层面）。
- **UC-SAL-02 订单级可用量校验**：L3 静态确认缺失（订单审核不调库存 Facade），运行时影响 = 销售员接单后到出库环节才发现库存不足（业务运营层面延迟发现，不破坏活跃数据）。**L1↔L3 偏离**（按 §4 L1 胜）。
- **UC-SAL-03 头级 + 发票侧派生**：L3+L4 双重证实行为正确。**接受**。
- **UC-SAL-03 行级 deliveredQuantity**：L3 静态确认零 writer → 派生值永远 0 → L1 断言不可达。运行时影响 = 行级出库进度报表/查询始终得 0（不影响审核流程，因头级 deliveryStatus 正确）。**P2 行为偏离**。
- **UC-SAL-10 库存域乐观锁**：L3+L4+MA2 A2.17 三重证实行为正确（不超卖 + 重试 + UK 兜底）。**接受**。
- **UC-SAL-10 销售级 seam**：L4 零覆盖 → 销售→库存 Facade seam 真实并发下未验证。运行时影响 = 理论上若 sales→inv Facade 存在 seam 缺陷（如 RPC 重试不当），现有测试不可见。**P2 测试覆盖缺口**。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论，§2 判据）

### 5.1 五级追踪矩阵

| UC 编号 | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|---|---|---|---|---|---|---|
| **UC-SAL-02** 出库可用量不足审核回滚(销售独有) | `use-cases.md:56` 标题 + 验收标准原文（见 §1）：「订单.审核通过 触发: 调用库存校验可用量；若 可用量 < 出库数量: 审核失败,回滚(订单保持 SUBMITTED,不前推)；不生成出库移动单；库存余额不变」 | `state-machine.md:52`（销售出库单 校验可用量 → 生成出库移动单）+ `:56`（销售订单 仅状态推进，不直接触发库存/凭证）+ `inventory/cross-domain.md:25-28`（销售出库（扣减库存）触发销售出库单审核通过 + 可用量校验）— **L2 与 L1 冲突**（L2 说出库级，L1 说订单级）→ 按 §4 **以 L1 为准**，L2 推定已向实现妥协 | **订单审核**：`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 仅 requireCustomerActive + creditLimitChecker.check；`@Inject` 簇 :54-85 无 IErpInvStockMoveBiz/IErpInvStockBalanceBiz；`ErpSalOrderApproveProcessor:48-51` 仅委托 processor。<br>**出库审核**：`ErpSalDeliveryApproveProcessor:37` → `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `IErpInvStockMoveBiz.generateMove` → 跨域 `ErpInvStockMoveProcessor.doConfirm:86-98` → `validateAvailable:116-136` 不足抛 `ERR_AVAILABLE_INSUFFICIENT` (`ErpInvErrors.java:49`)；销售独有 `reservesOnConfirm:242-248`（OUTGOING/INTERNAL_TRANSFER 触发，INCOMING 跳过） | `TestErpSalDeliveryStockMove#testApproveInsufficientAvailableRollsBack:121-155` **强**（错误码 + approveStatus=SUBMITTED + posted=false + 无 DONE 移动单 + 余额不变=5）；`#testNegativeStockConfigAllowsShortage:179-210` **强**（负库存配置分支）；`TestErpSalOrderApproval` 全 11 方法**无可用量校验测试**（Javadoc :33-42 明示「仅状态推进，不触发库存/凭证」） | 出库级回滚行为**已证实**（A2.9 §维度2/4 PASS + L4 强覆盖）；订单级校验缺失**已证实**（L3 静态零库存 Facade 注入 + L4 零覆盖）；运行时业务影响 = 接单后到出库才发现库存不足（运营延迟发现，非活跃数据破坏） | **L1↔L3 偏离**（订单级校验缺失）= **复用 P1-RC-020**（A1.18 已登记，同根因同控制点 — UC-SAL-01 :27 step 1 与 UC-SAL-02 :62-66 是同一订单审核站点的不同 UC 投影）。**出库级回滚行为本身** = 接受（断言②③④ 行为已实现，仅控制点偏离 L1 字面）。G1 L1↔L2/L3 冲突按 §4 + §9 记入报告，不直改真相源 |
| **UC-SAL-03** 分批出库与部分收款 | `use-cases.md:76` 标题 + 验收标准原文（见 §1）：「订单行.已出库数量 == 60 // 第一次后」+「订单行.已出库数量 == 100 // 第二次后(派生)」+「发票1.收款状态 = 部分」+「发票2.收款状态 = 已收清」 | `state-machine.md:48-56` SUBMITTED→APPROVED 触发后续业务（销售出库单 校验可用量 → 生成出库移动单）+ `:164-176` 收款状态机（派生 UNRECEIVED/PARTIAL/RECEIVED） | **行级 deliveredQuantity**：`app-erp-sales.orm.xml:402` 列存在但 `setDeliveredQuantity` 全仓零生产 writer（仅生成 bean + DTO setter + 2 MFG 测试 seed ZERO）。<br>**头级 deliveryStatus**：`ErpSalDeliveryProcessor.rollupOrderDeliveryStatus:270-310` 聚合 → `ErpSalOrderBizModel.updateDeliveryStatus:240-254` 写头。<br>**发票侧 receivedStatus**：`ReceiptSettler.recomputeInvoiceReceived:161-177` 3 态派生 | `TestErpSalDeliveryStockMove#testDeliveryStatusRollupToOrder:212-247` **强（头级）** 测 2 行×1 出库/行（**非 L1 字面 1 行×2 分批(60+40)**），**从不断言 orderLine.deliveredQuantity**；`TestErpSalOrderToDeliveryEnd#testOrderToDeliveryToEnd:69-125` **强（单次满量）**；`TestErpSalOrderToCashEnd:167-173` **强（发票侧 60/100→PARTIAL）** | 头级 + 发票侧派生**已证实正确**；行级 deliveredQuantity 派生值永远 0 → L1 断言不可达（不影响审核流程，仅影响行级进度报表/查询） | **行级 deliveredQuantity 派生** = **P2-RC-019 新建**（§2 P2① 次要验收标准未完全满足，主路径[头级 rollup]OK 边界[行级派生字段]弱；与 P2-RC-013 同型不同域/UC）。**1 行×2 分批(60+40) 测试缺失** = **P2-RC-020 新建**（§2 P2① 断言强度，downstream of P2-RC-019）。**头级 deliveryStatus + 发票/收款 receivedStatus** = 接受（断言②③④ + 头级断言 已实现+强覆盖） |
| **UC-SAL-10** 并发出库扣同一批次 | `use-cases.md:216` 标题 + 验收标准原文（见 §1）：「并发出库A、B 扣同一批次: 一个成功(扣减), 另一个乐观锁冲突 → 重试或失败；最终 批次.现有量 == 初始 - A数量 - B数量 (不超扣)」 | `state-machine.md:74`（并发出库扣同一批次 乐观锁 + 扣减失败重试 销售独有）+ `inventory/state-machine.md §4` + `inventory/cross-domain.md` 余量校验规则 | `app-erp-inventory.orm.xml:369/389/415`（version 字段 + UK 兜底）+ `StockMoveBookkeeper.updateBalanceWithRetry:255-328`（`tryUpdateWithVersionCheck:271` + 重试 :264-327 + 上限 5 + `recordOptimisticLockFailure:290` + 耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`/`ERR_INV_BALANCE_INSERT_CONFLICT`）；销售出库侧无独立锁，完全委托库存域同一 @BizMutation 事务 | `TestErpInvConcurrentDeduct` 6 测试（3 单线程版本偏斜 + 3 真实多线程 ExecutorService+CountDownLatch）**强覆盖库存域**；**销售级并发 seam 测试为零**（module-sales grep `Executors|CountDownLatch` 零命中）；E2E `o2c-chain.spec.ts` 仅单次满量快乐路径 | 库存域乐观锁 + 重试 + UK 兜底**已证实完备**（A2.17 §2/§3/§13 PASS + L4 真实多线程不超卖）；销售级 seam 真实并发下未验证（理论风险） | **库存域乐观锁行为** = 接受（断言①② 全部满足，A2.17 强证）。**销售级并发 seam 测试缺失** = **P2-RC-021 新建**（§2 P2① 断言强度，inventory 域已强覆盖，sales seam 缺） |

### 5.2 候选缺口分级（G1-G5）

| 缺口 | 描述 | §2 判据 | 分级 | 通道 |
|---|---|---|---|---|
| **G1** | UC-SAL-02 订单审核不触发可用量校验（L1 字面订单级 vs L3 实际出库级） | §2 P1① 行为实质偏离验收标准 | **P1**（复用 P1-RC-020） | MR1（R1.0 展开为 RC-R1.n）— 已在 A1.18 登记，本切片追加 RC 视角注记 |
| **G2** | UC-SAL-03 行级 `deliveredQuantity` 列存在零生产 writer（L1 派生断言不可达） | §2 P2① 次要验收标准未完全满足（主路径[头级 rollup]OK 边界[行级派生]弱） | **P2**（新建 P2-RC-019） | successor watch-only（与 P2-RC-013 同型不同域/UC） |
| **G3** | UC-SAL-03 无 1 行×2 分批(60+40) 测试（现有为 2 行×1 出库/行） | §2 P2① 断言强度（downstream of G2，即便写测试也会失败） | **P2**（新建 P2-RC-020） | successor watch-only |
| **G4** | UC-SAL-10 销售级并发 seam 测试为零（库存域 6 测试强覆盖，sales seam 缺） | §2 P2① 断言强度（主路径[inventory 域]OK 边界[sales seam]弱） | **P2**（新建 P2-RC-021） | successor watch-only |
| **G5** | MA2 A2.17 审计文本 :326-327 「无并发测试」陈旧（与 TestErpInvConcurrentDeduct 6 测试矛盾） | 不属代码/需求分歧 — 纯审计文本陈旧对账 | **无 finding ID**（对账结论） | §9 差异增量声明记录（不重开/不重审） |

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（G1 复用 P1-RC-020，G2/G3/G4 均为 P2，G5 纯文本对账）— 按 §10 **不触发 MR0**。

**Q4 显式论证（G1 不构成 P0）**：
- (a) 可用量校验功能**存在**（出库审核环节落实，O2C 主链路功能正确）→ 非 §2 P0③ 核心循环断裂。
- (b) 不破坏活跃数据（出库仍守卫，不会超卖）→ 非 §2 P0① 活跃数据破坏。
- (c) 不破坏会计过账（无 GL 影响）→ 非 §2 P0④。
- (d) O2C 核心循环完整（订单→出库→发票→收款）→ 非 §2 P0③。
→ G1 属 §2 P1①（行为实质偏离验收标准），非 P0；且与 A1.18 P1-RC-020 同根因同控制点 → 复用。

**G1 L1↔L2/L3 真相源冲突裁决**（§4 + §9）：
- L1（`use-cases.md:62-66`）字面「订单.审核通过 触发: 调用库存校验可用量」。
- L2（`state-machine.md:52,56`）+ L3（`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 零库存 Facade 注入）均说校验在出库审核环节。
- 按 §4 Q1 裁决**L1 胜**，L2/L3 推定已向实现妥协。
- 按 §9 冻结条款记入报告，**不直改真相源**（修复方向须人工裁决：补订单级校验 OR 修 L1 措辞为出库级，属需求变更须经人工批准 + 登记，非方案 B 降级）。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

> 产出 finding 前已 grep `arm-index.md` sales 同域同控制点。裁决遵循 §7 规则。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本切片 finding 关系 | 裁决 |
|---|---|---|---|
| **`P1-RC-020`** UC-SAL-01 ① 订单级可用量校验缺失（A1.18 已登记） | `ErpSalOrderProcessor.validateBusinessRulesForApprove` 不调库存 Facade | **同根因同控制点**：UC-SAL-01 :27 行为链路 step 1 「审核通过(触发可用量校验 + 出库移动单生成)」与 UC-SAL-02 :62-66 「订单.审核通过 触发: 调用库存校验可用量」是**同一订单审核站点的不同 UC 投影**（前者是主路径触发断言，后者是回滚场景断言；同一代码站点 `ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 零库存 Facade） | **复用 P1-RC-020**（追加 A1.19 RC 视角注记，不新建编号） |
| `P0-MA2-020` 库存余额自然键 UK（A2.17 已 completed） | `UK_INV_STOCK_BALANCE_NATURAL` 兜底 INSERT 竞态 | **同控制点但已 completed**：UC-SAL-10 INSERT 竞态防护 | **复用**（已 completed，本切片 HEAD 复核 `app-erp-inventory.orm.xml:415` 物理存在，无须注记） |
| `P2-MA2-014` ReceiptSettler 并发核销无锁（watch-only） | 收款核销并发（UC-SAL-03 收款侧） | **不同控制点**（核销并发 vs 销售出库并发扣批次） | **不相关**（归 UC-SAL-03 收款侧 watch-only，本切片不复核） |
| `P1-MA1-022` 跨域 daoFor（sales 投影，resolved） | 跨域只读 daoFor | **不同控制点**（daoFor vs 出库/分批/并发） | **不相关** |
| `P1-MA2-009` 多币种 O2C（resolved） | O2C 链路多币种 FX | **不同控制点**（FX 折算 vs 出库/分批/并发） | **不相关**（归 A1.18 已复核） |
| `P1-MA2-056`/`057` Contract INLINE 守卫 | Contract 6 实体状态机守卫 | **不同切片**（归 A1.21 sales-F4 赠品与看板 + Contract） | **不相关** |
| `P2-MA2-013` 收款核销订单维度缺失 | 收款核销维度（发票 vs 订单） | **不同控制点** | **不相关** |
| `P2-RC-013` UC-PUR-03 `receivedQuantity` 列存在零 writer（A1.16 已登记） | purchase 行级派生字段未写入 | **同型不同域/UC**（purchase `receivedQuantity` vs sales `deliveredQuantity`；header 级 rollup 均正确，行级派生字段均零 writer） | **不合并**（不同域/UC/列，按 §7 新建），但**结构等价**注记 |
| arm-index :180 UC-SAL-10 并发扣批次曾交接项 | UC-SAL-10 库存域交接（A2.17 sustained） | **同 UC**：arm-index :180 显式记录 UC-SAL-10 并发扣批次为曾交接项，后 A2.17 sustained（PASS） | **复用**（A2.17 PASS 结论，本切片不重审库存域乐观锁行为，仅补 sales seam 缺口） |

### 6.2 新建 finding 裁决

| Finding ID | UC | 根因/控制点 | 与既有 finding 差异依据 | 裁决 |
|---|---|---|---|---|
| （无新 P1） | — | — | G1 复用 P1-RC-020（同根因同控制点，§6.1 已裁决） | **复用** |
| **P2-RC-019** | UC-SAL-03 行级派生 | `ErpSalOrderLine.deliveredQuantity` 列存在零生产 writer（仅生成 bean + DTO setter + 2 MFG 测试 seed ZERO），头级 rollup 正确 | 与 P2-RC-013（purchase `receivedQuantity`）**同型不同域/UC**（不同模块/不同列/不同 rollup 方法，但模式等价） | **新建**（同型模式注记） |
| **P2-RC-020** | UC-SAL-03 1 行×2 分批(60+40) 测试缺失 | 现有测试 `testDeliveryStatusRollupToOrder` 测 2 行×1 出库/行（结构非 L1 字面）+ `testOrderToDeliveryToEnd` 单次满量，均不断言 `orderLine.deliveredQuantity`（与 P2-RC-019 互为因果 — 即便补测试也会失败） | 与 P1-RC-010（A1.11 mfg 召回报告仅冒烟）+ P2-RC-017/018（A1.18 sales AR 凭证/价格端到端断言强度）同型不同控制点 | **新建**（downstream of P2-RC-019） |
| **P2-RC-021** | UC-SAL-10 销售级并发 seam | `module-sales` 全域 grep `Executors|CountDownLatch` 零命中；库存域 `TestErpInvConcurrentDeduct` 6 测试强覆盖（含 3 真实多线程），但销售→库存 Facade seam 真实并发下未验证 | arm-index 无 sales 域并发 seam 测试缺失 finding；A2.17 覆盖库存域乐观锁行为但不覆盖 sales seam | **新建**（与 A2.17 PASS 互补） |

### 6.3 双向可追溯

- **新 finding → arm-index**：3 P2 finding（P2-RC-019/020/021）将写入 `arm-index.md` MA1 RC finding 分区（§7 归档纪律）。
- **复用 finding → arm-index**：向 P1-RC-020 既有 arm-index 行追加 A1.19 RC 视角注记（UC-SAL-02 :62-66 是同控制点的回滚场景投影）。
- **finding → 修复**：3 P2 successor watch-only 不强制；G1 复用 P1-RC-020 待 MR1 R1.0 展开为 RC-R1.n 修复行。
- **既有 finding 复用注记**：P0-MA2-020（UK 兜底，completed）+ A2.17 UC-SAL-10 库存域乐观锁 PASS 维持不变；arm-index :180 UC-SAL-10 交接项维持 sustained 状态。

---

## 7. 静态存疑点清单（供 MA4 A4.1 展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行；无则注明。

1. **UC-SAL-10 销售级 seam 真实并发下的运行时行为**：L4 静态确认销售级并发测试为零，但「真实多线程下 sales `ErpSalDeliveryProcessor.triggerOutgoingMove` → inv `IErpInvStockMoveBiz.generateMove` 的 Facade seam 在同事务/异常传播/重试边界」属运行时面——交 MA4 A4.1 按需追加 A4.1.n 实体行展开运行时验证（构造 2 个销售出库单同批次并发 approve + ExecutorService + 断言 inv 域重试行为 + 最终余额守恒）。
2. **UC-SAL-02 订单级校验缺失的运行时业务影响**：L3 静态确认订单审核不调库存 Facade，但「销售员实际接单后到出库环节才发现库存不足」的运行时频度/业务影响属运营层面——交 MA4 A4.1 按需展开（grep 实际订单→出库审核拒绝率 + 销售员工作流调研）。
3. **负库存配置下并发结果**：L4 `testConcurrentDeductWithNegativeStockAllowed` 覆盖 inv 域负库存并发，但「负库存配置下 sales 出库同批次并发的最终余额边界」属运行时数值——交 MA4 A4.1 按需展开（与 SP-1 协同）。
4. **UC-SAL-03 `deliveredQuantity` 查询实际返回值**：L3 静态确认零 writer，但「UI 报表/GraphQL 查询读取此列的实际返回值（0 vs null vs 计算值）」属运行时——交 MA4 A4.1 按需展开（grep 实际订单 + delivery 审核后查 `orderLine.deliveredQuantity` 实测）。
5. **UC-SAL-03 1 行×2 分批(60+40) 运行时验证**：L1 字面断言无法静态验证（列不被写入），交 MA4 A4.1 按需展开运行时探针（构造 1 订单行 qty=100 + 2 出库 60+40 → 实测订单头 deliveryStatus + 行级 deliveredQuantity + 库存余额），与 P2-RC-019 修复协同。

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（G1 复用 P1-RC-020，G2/G3/G4 均为 P2，G5 纯文本对账），按 §10 **不触发 MR0**。3 P2 successor watch-only；G1 经 MR1 批量修复通道（R1.0 展开为 RC-R1.n）—— 与 A1.18 P1-RC-020 同根因同控制点，MR1 展开时合并为同一修复行（无须重复）。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。G1 复用 P1-RC-020 经 grep 比对裁决；G2-G4 经结构等价注记（与 P2-RC-013 / P1-RC-010 / P2-RC-017/018 同型不同控制点）后新建。

### checker actual vs baseline 实测表（2026-08-03 实测）

> 本审计为**只读审计**（无生产代码变更），故 checker 无回归风险；actual vs baseline 实测记录如下（基线源 `compliance-baseline.md §BASELINE (machine-readable)` 行 296-316）。

| 规则 | Baseline | Actual | 状态 |
|------|----------|--------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 14 | 14 | ✅ |
| R2a | 34 | 34 | ✅ |
| R2b | 229 | 229 | ✅ |
| R2c | 1382 | 1382 | ✅ |
| R2d | 34 | 34 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅ |
| R6 | 2 | 2 | ✅ |
| R7 | 0 | 0 | ✅ |
| R8 | 0 | 0 | ✅ |
| R10 | 6 | 6 | ✅ |
| R11 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

全 19 规则 actual ≤ baseline，**0 漂移**。本审计无生产代码变更，无回归风险。

---

## 9. 与 MA2 报告差异增量声明（§去重协议）

本切片声明与既有 MA2 报告的差异增量：

- **复用 MA2 已证实行为**（不重新核实）：
  - `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（A2.17，UC-SAL-10 库存域乐观锁主证据）：§2 :65,68（sales/inventory 实体 100% versionProp）+ §3 :100（tryUpdateWithVersionCheck sustained）+ §4 :119-121（UC-INV-08 超卖/UC-SAL-10 双重核销/UC-SAL-10 并发扣批次 全部 sustained 缺口证伪）+ §13 :296-298（UC-SAL-10 PASS）+ P0-MA2-020（:260-268 库存余额自然键 UK，本切片 HEAD 复核 `app-erp-inventory.orm.xml:415` 物理存在已落地）。本切片 UC-SAL-10 库存域乐观锁 L5 行为证据直接引用该报告。
  - `2026-07-28-0400-arm-ma2-sales-state-machine.md`（A2.9，sales 7 实体状态机）：§维度2 :111（出库 approve 可用量校验前置 PASS）+ §维度4 :130（出库可用量不足 PASS）+ §既有 finding 集（P1-MA2-009/056/057 + P2-MA2-010~015/056/057/058 + P1-MA1-022）。本切片不复核状态机维度，仅复用其已证实行为。
  - `2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（O2C 全链主证据）：§可用量 :39-44（链路 `triggerOutgoingMove → generateMove → doConfirm → validateAvailable`，错误传播 PASS）+ §UC-SAL-10 :183,195（乐观锁基础具备，并发扣批次交接 A2.17）。本切片 UC-SAL-02 出库级 + UC-SAL-10 基础乐观锁 L5 行为证据直接引用该报告。
  - `2026-07-29-1430-arm-ma5-e2e-effectiveness.md`（MA5 A5.6）：E2E `o2c-chain.spec.ts` 评 strong（行级凭证断言），但仅单次满量快乐路径，无不足/分批/并发场景覆盖 — 本切片 P2-RC-020/021 从需求契约视角补断言强度。
- **本切片只补的需求视角差异**（MA2 未覆盖）：
  1. **UC-SAL-02 订单级可用量校验缺失**（G1，**复用 P1-RC-020**）：A2.9 §维度2 + O2C §可用量 仅证实"出库审核时跨域可用量校验已落实"（控制点不在订单审核），未从 L1 行为链路 :62-66 字面视角审视"订单审核触发"控制点偏离。本切片从 L1 字面视角追加 UC-SAL-02 投影注记到 P1-RC-020 既有 finding（同根因同控制点不新建）。**L1↔L2 真相源冲突**按 §4 + §9 记入报告，**不直改真相源**。
  2. **UC-SAL-03 行级 `deliveredQuantity` 不写入**（G2，**新建 P2-RC-019**）：A2.9 §维度2 仅证实头级 rollup；MA2 未从 L1 字面「订单行.已出库数量 == 60→100」视角审视行级派生字段不可达。本切片从 L1 字面视角定级 P2（与 P2-RC-013 同型不同域/UC）。
  3. **UC-SAL-03 1 行×2 分批(60+40) 测试缺失**（G3，**新建 P2-RC-020**）：MA5 §A5.6 评级 `TestErpSalDeliveryStockMove#testDeliveryStatusRollupToOrder` 强（头级），但未从 L1 字面「1 行×2 分批(60+40)」结构视角审视测试覆盖缺口。本切片从需求契约视角定级 P2（downstream of P2-RC-019）。
  4. **UC-SAL-10 销售级并发测试缺失**（G4，**新建 P2-RC-021**）：A2.17 §3/§13 证实库存域并发行为 PASS（L4 `TestErpInvConcurrentDeduct` 6 测试强覆盖含真实多线程），但 sales 域 grep `Executors|CountDownLatch` 零命中 — 销售→库存 Facade seam 真实并发下未验证。本切片从需求契约视角定级 P2（与 A2.17 PASS 互补）。
  5. **G5 MA2 审计文本陈旧对账**：A2.17 `:326-327` 称「无并发场景集成测试」与现存 `TestErpInvConcurrentDeduct`（6 测试，引用 plan `2026-07-07-0024-2` 后落地）矛盾——**审计文本陈旧**（A2.17 撰写于测试落地前/同提交切片）。**对账结论**：A2.17 §3/§13 的行为裁决（PASS）与现存测试一致（无逻辑矛盾），仅 :326-327 文本陈述陈旧；按 §9 不直改 MA2 历史报告（属审计历史档案），本切片对账记录备查，不重开 A2.17 finding，不新建 finding ID。
- **MA2 finding 复核无升级**：本切片复核 MA2 已登记的 sales + 跨域相关 finding（P0-MA2-020 已 completed + P2-MA2-014 watch-only + P1-MA1-022 resolved + P1-MA2-009/056/057 + P2-MA2-010~015/056/057/058），运行时行为与 MA2 登记一致，**无升级 P0**（对齐 A2.9 + A2.17 + O2C 结论）。
- **报告校正项**：`docs/audits/2026-07-06-use-case-implementation-audit.md:71-86` 早期 UC 覆盖调查：UC-SAL-02 标 ✅（出库级，未核订单级偏离）/ UC-SAL-03 标 🔶（deliveredQty 未测试）/ UC-SAL-10 标 ❌（乐观锁未测试）—— 与 A2.17（PASS）+ 本切片（G1 复用 P1-RC-020 / G2-G4 新 P2）矛盾，**早期调查文本陈旧**，本切片对账记录备查。
- **真相源冻结条款遵守声明**：本审计**未修改** `docs/design/sales/use-cases.md` / `docs/design/sales/state-machine.md` / `docs/design/inventory/cross-domain.md` / `docs/requirements/product-scope.md` 的需求契约段落（§9 冻结条款）。G1 L1↔L2/L3 冲突记入本报告 §5 / §9，修复方向（补订单级校验 OR 修 L1 措辞为出库级）须经人工批准 + 登记（非方案 B 降级）。

---

## 10. Verdict

**Verdict: passes requirement-compliance audit**（带 0 项新 P1 + 1 项复用 P1 + 3 项新 P2 successor + 多数验收标准接受）

**审查范围**：UC-SAL-02/03/10 共 3 UC 五级追踪矩阵（L1-L5）+ 每 UC 符合性结论（§2 判据）+ 与 arm-index 衔接（§7 复用/新增裁决）+ 静态存疑点清单（供 MA4 A4.1 展开）+ 过程纪律自检 + 与 MA2 差异增量声明。

**接受类**：UC-SAL-02 出库级回滚行为（断言②③④ 已实现）+ UC-SAL-03 头级 `deliveryStatus` rollup + 发票/收款 `receivedStatus` 派生 + UC-SAL-10 库存域乐观锁 + 重试 + UK 兜底（断言①② 全部满足）。

**P1 复用**：P1-RC-020（UC-SAL-02 订单级可用量校验缺失，**复用 A1.18 既有 finding** — 同根因同控制点的回滚场景投影，向既有 arm-index 行追加 RC 视角注记）→ MR1（R1.0 展开为 RC-R1.n），与 A1.18 同修复行合并（无须重复）。

**P2 successor**：P2-RC-019（UC-SAL-03 行级 `deliveredQuantity` 列存在零生产 writer，纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first）/ P2-RC-020（UC-SAL-03 1 行×2 分批(60+40) 测试缺失，纯测试补充，可自动执行）/ P2-RC-021（UC-SAL-10 销售级并发 seam 测试缺失，纯测试补充，可自动执行）→ successor watch-only 不强制。

**P0**：无。不触发 MR0。

**G5 文本对账**：A2.17 `:326-327` + `2026-07-06-use-case-implementation-audit.md:71-86` 审计文本陈旧，对账结论记入 §9，不重开 finding 不直改历史档案。

**剩余风险**：见 §7 静态存疑点清单（5 项交 MA4 A4.1 运行时展开）。

**报告 9 段完整性自检**：§1-§9 全部存在（§0 TL;DR + §1 L1 + §2 L3 + §3 L4 + §4 L5 + §5 矩阵+结论 + §6 arm-index 衔接 + §7 静态存疑点 + §8 过程纪律自检 + §9 MA2 差异增量 + §10 Verdict）— closure audit 核验通过条件齐备。
