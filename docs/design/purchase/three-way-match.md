# 三单匹配与差异处理

## 目的

说明采购域"采购订单 → 入库 → 发票"三单匹配的规则、差异处理与一致性约束。

## 三单匹配模型

三单匹配（Three-way Match）是采购到付款流程的核心校验机制，确保供应商实际发货、开票与采购约定一致。

```
采购订单（PO）
   │ 订单数量、订单单价
   ↓
采购入库（Receive）
   │ 实收数量（可多次部分入库）
   ↓
采购发票（Invoice）
   │ 发票数量、发票单价（回链入库行）
```

### 回链关系

- **采购入库行** 可选回链采购订单行（`source_order_line_id`）。
- **采购发票行** 可选回链采购入库行（`source_receive_line_id`）。
- 回链是"可选"的——支持独立创建入库单/发票单（无订单采购、直接凭发票入库等场景）。

## 匹配规则

### 数量匹配

| 比较项 | 规则 |
|--------|------|
| 订单数量 vs 入库数量 | 允许超收/短收，超收容差由全局配置（如 ±5%） |
| 入库数量 vs 发票数量 | 允许部分开票；发票数量不得超过入库数量（除非配置允许） |

### 价格匹配

| 比较项 | 规则 |
|--------|------|
| 订单单价 vs 发票单价 | 允许价格差异，差异超阈值时提示警告或拦截（由配置决定） |
| 价格差异处理 | 差异金额可计入"采购价格差异"科目（财务域处理） |

### 匹配时机

- **入库时不强制匹配订单**：入库可独立创建，但若指定订单行则校验数量容差。
- **发票时校验匹配**：发票审核时校验"发票行回链的入库行"数量与金额一致性。
- **付款前最终校验**：付款核销时确认发票已完成三单匹配。

#### 付款核销二次门控（R1.8 P1-MA2-003 方案 A 落地）

「付款前最终校验」经 config-gated 二次门控落实，对齐本节「付款核销时确认发票已完成三单匹配」契约：

| 配置 | 默认 | 说明 |
|------|------|------|
| `erp-pur.settle-recheck-three-way-match` | false | 启用后 `PaymentSettler.settle` 在发票 APPROVED 守卫后追加强制 strict 三单匹配复核；任一发票行数量超入库或价格超容差即抛 `erp.err.pur.settle-invoice-match-not-completed` 阻断核销 |

- 默认 false 保护既有基线（非严格模式下 approve 已 warn 放行的发票仍可核销，不破坏既有行为）。
- 启用后复核为**运行时重算**（invoice 无持久化 matchStatus 字段，避免 ORM 变更）——重算依赖 invoice/receive/order 行当前状态；APPROVED 发票回链不允许修改（见 §一致性规则），故重算结果与 approve 时一致。
- 复核失败时原始匹配异常（`ERR_INVOICE_QTY_MISMATCH` / `ERR_INVOICE_PRICE_MISMATCH`）以 cause 链保留在 `ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED` 中，便于定位具体不匹配行。

## 差异处理

### 数量差异

| 场景 | 处理 |
|------|------|
| 入库数量 < 订单数量 | 正常（部分收货），订单未交量减少 |
| 入库数量 > 订单数量（超收） | 超出容差则拒绝入库审核；容差内允许并调整订单数量 |
| 发票数量 < 入库数量 | 正常（部分开票） |
| 发票数量 > 入库数量 | 拒绝（除非配置允许，如运费/杂费明细） |

> **实现注记（RC-R1.11，P1-RC-019）**：超收方向（receive-vs-order）校验已落地——`ErpPurReceiveProcessor.validateOverReceiptTolerance`（入库审核 `validateBusinessRulesForApprove` 内 protected step，位于库存移动/过账触发之前）。复用本文件 §不匹配的处理策略 的 `erp-pur.match-qty-tolerance`（默认 5%）与 `erp-pur.match-strict-mode`（默认 false）：per-order-line 聚合「当前入库单行 + 同订单其他 APPROVED 入库单行」Σ 数量，`Σ > 订单数量 × (1 + 容差%)` 时 strict 模式抛 `erp.err.pur.receive-qty-over-tolerance` 拒绝审核 / 非严格模式 LOG.warn 放行；无 `orderLineId` 行（独立入库）跳过；恰好等于容差边界放行。「容差内允许并调整订单数量」中的"调整"为人工操作语义，校验仅做允许/拒绝判定，不自动改订单行。短收方向（UC-PUR-06 ⑮ 短收差异处理）未随本行落地，归 P2-RC-014 successor watch-only。

### 价格差异

| 场景 | 处理 |
|------|------|
| 发票单价 > 订单单价（涨价） | 差异超阈值时警告/拦截；通过后差异计入采购价格差异科目 |
| 发票单价 < 订单单价（降价） | 同上；差异可能冲减采购成本 |

> **实现注记（RC-R1.50，P1-RC-018）**：「差异计入采购价格差异科目」已落地——策略「接收并过账差异」（`erp-pur.price-diff-strategy=POST_DIFFERENCE`）下，AP_INVOICE 凭证增 1404 材料成本差异 PPV 行（金额 = 差异 × 数量，涨价借/降价贷）+ 1403 在途物资按差异拆分，对齐 L1 UC-PUR-05「让步接收时存在过账行: 科目 == 价格差异科目 且 金额 == 差异 * 数量」。详见 §不匹配的处理策略实现注记。

### 费用分摊（Landed Cost）

采购过程中的附加费用（运费、保险费、关税等）可分摊到入库物料成本：

- 费用发票独立于物料发票。
- 分摊方式：按数量/按金额/指定分摊。
- 分摊结果更新库存单位成本（通过库存流水调整）。
- 详细成本核算规则属于财务域。

## 不匹配的处理策略

配置项控制匹配严格度：

| 配置 | 默认 | 说明 |
|------|------|------|
| `erp-pur.match-qty-tolerance` | 5 (%) | 超收数量容差百分比 |
| `erp-pur.match-price-tolerance` | 5 (%) | 价格差异容差百分比 |
| `erp-pur.match-strict-mode` | false | 严格模式下任何超容差差异都拒绝审核 |
| `erp-pur.price-diff-strategy` | （空 = 拒绝族） | 价格差异处理策略；`POST_DIFFERENCE` = 「接收并过账差异」（见下方实现注记） |

- **非严格模式**（默认）：超容差差异提示警告，允许审核通过。
- **严格模式**：超容差差异拒绝审核，必须调整单据或放宽容差配置。

> **实现注记（RC-R1.50，P1-RC-018）**：L1 UC-PUR-05 三策略 {拒绝, 审批后接收, 接收并过账差异} 落地语义——
>
> 1. **策略选择**：`erp-pur.price-diff-strategy` config 门控。默认空值 = **拒绝族**（既有行为零变化：strict 抛 `ERR_INVOICE_PRICE_MISMATCH` 拒绝 / 非 strict LOG.warn 放行，不产生 PPV 过账数据）；`POST_DIFFERENCE` = **「接收并过账差异」**（价格超容差行 LOG.warn 放行——含 strict 模式下的价格维度放行，数量维度 strict 语义不变——并传递差异数据触发 PPV 过账）。「审批后接收」= L1 三选一子集语义：strict 拒绝即该策略的审批前置（拒绝后调整单据再提交，不新建独立 diff 审批流——无 owner doc 契约支撑，完整审批工作流登记 successor）。
> 2. **PPV 过账载体**：`PurInvoicePostingDispatcher.tryPost` 增差异数据重载（差异 = Σ[(发票单价−订单单价)×数量]，仅超容差行，经 invoiceLine.receiveLineId → receiveLine.orderLineId → orderLine.unitPrice 回链，与 ThreeWayMatcher 同口径同路径），`ErpPurInvoiceProcessor.resolvePriceVariance` protected step 按策略门控写入 billData 键 `PRICE_VARIANCE_AMOUNT`（拒绝族不写）。
> 3. **1403 拆分**：`PurAcctDocProvider.createFacts` AP_INVOICE 在差异 != 0 时按 1403 拆分——1403 在途物资金额 = TOTAL_AMOUNT − 差异 + 增 PPV 行（科目 1404 材料成本差异，`ACCOUNT_KEY_PRICE_VARIANCE` 可经 GL 映射规则覆盖；金额 = |差异| 量值，涨价借 1404 / 降价贷 1404），两者合计恒等于原 TOTAL_AMOUNT（借贷恒等 Dr Σ = Cr Σ 保持）；差异 0/键缺失走既有三行零变化。1404 为 seed `erp_md_subject.csv` 纯加性行。
> 4. **测试**：`TestErpPurPriceVariancePosting` 9 组（涨价 PPV 借/降价 PPV 贷/容差内零差异/无回链跳过/订单价 0 跳过/差异 0/默认拒绝族零 PPV 回归/strict 抛错不变/POST_DIFFERENCE 覆盖 strict）+ `_cases/` 快照。

## 一致性规则

- 三单匹配校验在发票审核时执行，失败则拒绝审核。
- 已审核的发票不允许修改回链关系（避免破坏已生成的应付凭证）。
- 红冲发票（采购退货发票）自动回链原发票，冲销应付。
- 跨币种匹配时按业务日期汇率统一换算后比较金额。

## 与其他域的关系

- **库存域**：入库数量以库存移动单实际执行数量为准；若入库后库存调整（如盘点盘亏），不影响已审核的入库单数量。
- **财务域**：价格差异与费用分摊的成本核算由财务域处理；本域只提供匹配数据。
- **主数据域**：物料/SKU、供应商、币种、税率引用主数据。
