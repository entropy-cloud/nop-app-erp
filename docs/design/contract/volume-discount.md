# 合同批量折扣与返利设计

## 目的

设计合同域的分级价格折扣（Tiered Pricing）和年度返利协议（Volume Rebate）功能。支持数量区间 → 折扣百分比映射、年度累计阶梯返利、期间内跨越层级时自动追溯调整、返利信用单生成与财务集成。

## 设计依据

> 参考 **ERPNext Pricing Rule**（价格规则引擎：数量区间 + 折扣率 + 客户/物料维度）。
>
> 参考 **Odoo Rebate Management**（返利协议 + 返利结算 + 信用单生成）。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §合同管理。

## 分级价格折扣

### 业务场景

同一合同下，采购方购买不同数量区间的同一物料享受不同折扣。例如：

| 数量区间 | 折扣率 |
|----------|--------|
| 0 ~ 100 | 0%（标准价） |
| 101 ~ 500 | 5% |
| 501 ~ 2000 | 8% |
| 2000+ | 12% |

系统自动根据订单行的实际数量匹配对应的折扣率，计算折后价。

### ErpCtVolumeDiscount（批量折扣）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/contractLineId/orgId | 标准 | 🟢 ERPNext Pricing Rule |
| fromQty | 起始数量（含） | 🟢 ERPNext min_qty |
| toQty | 截止数量（含，null 无上限） | 🟢 ERPNext max_qty |
| discountPercent | 折扣百分比（0~100） | 🟢 ERPNext discount_percentage |
| unitPrice | 可选：覆盖单价（不填则按合同行单价×折扣率计算） | |

**唯一约束**：同一 contractLineId 内，fromQty ~ toQty 区间不可重叠。

### 折扣应用逻辑

```
合同行标准单价: ¥100
合同行配置区间:
  0~100   → 0%  → 折后价 ¥100
  101~500 → 5%  → 折后价 ¥95
  501+    → 12% → 折后价 ¥88

采购订单引用合同行，下单数量 300：
  1. 按 contractLineId 查找 ErpCtVolumeDiscount
  2. 匹配数量 300 → 区间 101~500 → discountPercent=5%
  3. 折后单价 = ¥100 × (1 - 5%) = ¥95
  4. 行金额 = 300 × ¥95 = ¥28,500
```

## 年度返利协议

### 业务场景

与供应商/客户约定年度累计采购/销售金额达到某一阶梯时，返还一定比例。例如：

| 年度累计金额 | 返利比例 |
|-------------|----------|
| ¥0 ~ ¥1,000,000 | 0% |
| ¥1,000,000 ~ ¥5,000,000 | 2% |
| ¥5,000,000 ~ ¥10,000,000 | 3% |
| ¥10,000,000+ | 5% |

### ErpCtRebateAgreement（返利协议）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | 🟢 Odoo Rebate Agreement |
| contractId | 关联合同（可选，独立协议可无合同） | |
| partnerId | 对方伙伴 | |
| rebateType | dict `erp-ct/rebate-type`：PURCHASE（采购返利）/ SALES（销售返利） | |
| agreementDate | 协议签订日期 | |
| startDate/endDate | 协议有效期（通常一个财年） | |
| accrualMethod | dict：PERIOD_END（期末一次性）/ PROGRESSIVE（逐笔累计） | |
| status | dict：DRAFT / ACTIVE / EXPIRED / SETTLED | |
| totalAccumulatedAmount | 当前累计金额（已过账单据汇总，只读派生） | |
| estimatedRebateAmount | 预估返利金额（按当前累计计算） | |

### ErpCtRebateTier（返利阶梯）

| 字段 | 含义 |
|------|------|
| id/rebateAgreementId | 标准 |
| fromAmount | 起始金额（含） |
| toAmount | 截止金额（含，null 无上限） |
| rebatePercent | 返利比例（0~100） |
| rebateAmount | 可选：固定返利金额（优先于比例计算） |

### ErpCtRebateAccrual（返利计提明细）

| 字段 | 含义 |
|------|------|
| id/rebateAgreementId/orgId | 标准 |
| sourceBillType/sourceBillCode | 来源单据（AP/AR 发票） |
| billAmountSource | 单据金额（源币种） |
| accruedRebate | 该单据产生的返利计提金额 |
| accrualDate | 计提日期 |
| isSettled | 是否已结算 |
| settledDate | 结算日期 |

## 追溯调整

### 触发条件

年度累计量在期间内跨越了一个或多个层级，系统需要对已过账但按低层级比例计算的返利进行追溯补差。

### 流程

```
假设返利阶梯：
  ¥0 ~ ¥1M     → 0%
  ¥1M ~ ¥5M    → 2%
  ¥5M ~ ¥10M   → 3%

场景：
  1. 上半年累计采购 ¥800K → 返利 ¥0（0% 层级）
  2. 下半年新增采购 ¥400K → 累计 ¥1.2M → 跨越 2% 层级
  3. 追溯调整：
     - 前 ¥800K 按 2% 补返利 = ¥16,000（追溯）
     - 新增 ¥400K 按 2% 计算的返利 = ¥8,000（当前）
     - 合计返利 ¥24,000
```

### 追溯调整规则

| 场景 | 处理 |
|------|------|
| 累计金额跨越一个层级 | 所有已过账但是按低层级比例计提的单据，补差额（新比例 - 旧比例） |
| 累计金额跨越多个层级 | 逐级追溯补差，每个层级的差额独立计算 |
| 跨越后累计回落（退货） | 重新计算当前有效层级，如回落则冲销多计提的返利 |
| 协议到期未跨越 | 不产生返利，已计提的返利冲回 |

## 返利信用单

### 业务语义

返利最终以**信用单（Credit Memo）**形式结算。系统基于已达成的返利总金额自动生成 AP/AR 信用单，走标准财务过账流程。

### ErpCtRebateSettlement（返利结算单）

| 字段 | 含义 |
|------|------|
| id/rebateAgreementId/orgId | 标准 |
| settlementDate | 结算日期 |
| totalRebateAmount | 结算返利总额 |
| creditMemoBillType/creditMemoBillCode | 信用单号（→ AP/AR Credit Memo） |
| status | dict：DRAFT / POSTED / CANCELLED |
| postedAt/postedBy | 过账信息 |

### 结算流程

> **实现约定**：贷项凭证复用既有
> `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz` 以**负额发票**表达，不新增 finance CreditMemo 实体。
> `rebateType=PURCHASE`→AP 负额发票（冲减应付），`rebateType=SALES`→AR 负额发票（冲减应收）。
> 经负额走标准 AP_INVOICE/AR_INVOICE 过账产生红字凭证 + 负 openAmount 辅助账
>（与退货红字模式、PaymentSettler negate 模式一致）。

```
返利协议到期或手动触发结算
    │
    ├─► 系统计算协议期间内累计返利总额
    │     = ∑(ErpCtRebateAccrual.accruedRebate) - 已结算金额
    │
    ├─► 生成 ErpCtRebateSettlement（DRAFT）
    │
    ├─► 生成贷项凭证（负额发票，经 IDaoProvider 直接持久化）：
    │     rebateType=PURCHASE → 生成负额 AP 发票（冲减应付）
    │     rebateType=SALES   → 生成负额 AR 发票（冲减应收）
    │
    ├─► 贷项发票后续走 purchase/sales 域标准审核过账流程（红字凭证）
    │
    └─► Settlement 状态 → POSTED
          ErpCtRebateAccrual.isSettled → true
```

### 财务集成

| 返利类型 | 信用单类型 | 财务科目 | 说明 |
|----------|-----------|----------|------|
| PURCHASE（采购返利） | AP Credit Memo | 贷：应付账款；借：采购成本（红字） | 供应商应返还给采购方 |
| SALES（销售返利） | AR Credit Memo | 借：应收账款；贷：销售收入（红字） | 销售方应返还给客户 |

## 业务规则

1. **折扣优先于返利**：行级折扣（VolumeDiscount）在订单行上直接应用，影响订单金额。返利是期间汇总后的后置结算，不影响订单明细。
2. **返利不计复利**：跨层级调整时只补差值，不在已补差值上再计算返利。
3. **汇率处理**：多币种场景下，返利累计金额统一折算为本位币计算阶梯，汇率取每笔过账单据的业务日期汇率。
4. **退货扣减**：退货单金额从累计金额中扣减（负值参与累计），可能导致层级回落、冲销已计提返利。
5. **协议续签**：到期后新协议重新开始累计，不继承上一期累计金额（除非同属一个框架合同下的连续协议）。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| purchase/sales | 订单行引用合同行时应用 VolumeDiscount 折扣；发票过账时通知 contract 域记录返利计提 |
| finance | 返利结算生成 AP/AR Credit Memo，走财务标准过账 |
| master-data（Currency） | 汇率用于返利跨币种累计折算 |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-ct.volume-discount-enabled` | true | 批量折扣是否启用 |
| `erp-ct.rebate-enabled` | false | 年度返利协议是否启用 |
| `erp-ct.rebate-auto-settle` | true | 协议到期是否自动触发结算 |
| `erp-ct.rebate-accrual-method` | PERIOD_END | 默认计提方法 |

## 消耗计费与开票计划生成（RC-R1.33 实现注记）

> **实现注记（plan 2026-08-15-0456-3，RC-R1.33，P1-RC-074 + P1-RC-075）**：UC-CT-03/04 的生成面与计费面落地。契约段落（use-cases L1）未修改，本注记记录实现语义与裁决。

### 开票计划批量生成（generateInvoicePlansByTerm，UC-CT-03 A）

- **入口**：`ErpCtInvoicePlanBizModel#generateInvoicePlansByTerm` @BizMutation（委托 per-mutation Processor `ErpCtInvoicePlanGenerateByTermProcessor`，R6.7 范式）。
- **入参**（D2 裁决）：`contractId` + 生成项列表 `[{contractLineId, invoiceTerm, planDate, amount}]`——**生成契约经入参承载 invoiceTerm**（ORM 结构注记：`ErpCtInvoicePlan.invoiceTerm` mandatory，而 `ErpCtContractLine` **无 invoiceTerm 列**；L1「合同行已配置 invoiceTerm」按入参化解释，调用方按 L1 流程传入 ADVANCE/MILESTONE/MONTHLY/COMPLETION）。
- **守卫链**：合同 ACTIVE（SUSPENDED 专属 `ERR_CT_CONTRACT_SUSPENDED` + 通用非 ACTIVE `ERR_CT_CONTRACT_NOT_ACTIVE`，对齐 triggerInvoice 守卫语义）→ 行归属校验（`ERR_CT_INVOICE_PLAN_LINE_NOT_IN_CONTRACT`）→ 幂等查重（D3：contractLineId+invoiceTerm+planDate，重复抛 `ERR_CT_INVOICE_PLAN_DUPLICATE`）。
- **planDate 推导**（D3）：入参显式提供，不引入 config 推导键（ADVANCE N 天/MONTHLY 固定日语义由调用方推导；config 推导属调度 successor 范畴）。
- **落库**：`isInvoiced=false` + invoiceTerm + planDate + amount，返回生成计划集合。

### 已开票禁改守卫（isInvoiced 锁，UC-CT-03 C）

- `ErpCtInvoicePlanBizModel#defaultPrepareUpdate` 增守卫（D4 裁决）：**isInvoiced=true 时 amount/planDate/invoiceTerm 变更拒绝**，抛 `ERR_CT_INVOICE_PLAN_INVOICED_IMMUTABLE`（参数 invoicePlanId）。remark 等非计费字段放行。
- 已开票状态经 ORM 脏值追踪取"更新前"持久化值（客户端同请求改 isInvoiced 防解锁绕过，对齐 R1.9 publish 守卫范式）。
- **不影响 triggerInvoice 回写**：触发面经 Processor `dao().updateEntity` 直写绕过 CrudBizModel 更新路径（既有行为不变）。

### 消耗计费周期汇总（periodSummarize，UC-CT-04 B/C/D）

- **入口**：`ErpCtConsumptionLineBizModel#periodSummarize` @BizMutation（委托 per-mutation Processor `ErpCtConsumptionPeriodSummarizeProcessor`）。入参 `{contractLineId, fromDate, toDate, invoiceTerm, planDate}`——**行级聚合入口**（D6 裁决；合同级循环归 successor）。
- **汇总**：期间内 Σ ConsumptionLine.quantity 对比合同行预估总量 `line.quantity`。
- **超量**（Σ > line.quantity）：生成额外 InvoicePlan（**复用 generateInvoicePlansByTerm 内部能力**，amount 按 D6 选项 A = `(Σquantity − line.quantity) × line.unitPrice`，scale 4 HALF_UP）+ 复用 `triggerInvoice` 路径生成 AP/AR 发票草稿（O-4 豁免语义不变，非新增过账逻辑）。
  - **量/额不一致声明**（D6）：超量发票草稿行级 quantity/unitPrice 镜像合同行（triggerInvoice Processor 既有语义），header amount = 超量金额——刻意选择（header 是计费基准，行级仅供明细展示）。
  - 同事务内新实体 NEW 态处理：flush + evict 后重新 DB 加载再触发（triggerInvoice 的 `dao().updateEntity` 要求 DB 加载态）。
- **120% 通知**（Σ > line.quantity × 1.2，预估 0 时任何消耗即超限）：经 `IErpSysNotificationBiz.notify` 派发事件 **`ct.consumption-over-120-percent`**（`ErpCtConstants.NOTIFY_EVENT_CONSUMPTION_OVER_120`），context 键集 {contractId, contractCode, contractLineId, lineDescription, estimatedQuantity, totalConsumedQuantity, overRatio}。**无 ACTIVE 模板静默跳过**（R1.4 范式，best-effort 不阻断业务事实；运营侧 CRUD 登记模板后生效，测试侧 seed 模板断言落库）。
- **返回**：`ErpCtConsumptionPeriodSummarizeResult`（totalConsumedQuantity/estimatedQuantity/overQuantity/overAmount/overagePlanId/invoiceBillCode/notificationSent/overRatio）。

### 结构注记

- **Line 无 invoiceTerm 列**：`ErpCtContractLine` ORM 仅含 quantity/unitPrice/amount/description（`module-contract/model/app-erp-contract.orm.xml:195-231`）——生成契约经入参承载（D2）；未来如需行级 term 持久化属 ORM 变更（ask-first successor）。
- **消耗计费定时调度**：周期触发 job 属部署/调度 successor——本期仅提供手工 @BizMutation 能力面（对齐 triggerDuePlans 手工入口先例）。

## 反模式警示

- ⛔ **返利直接在订单行上折扣**——返利是期间后置结算，不是行级折扣。行级折扣走 VolumeDiscount，返利走 RebateAgreement → RebateSettlement。
- ⛔ **返利不提计直接结算**——返利应按月/按季预先计提（Accrual），期间结束时汇总结算，否则财务数据波动大。
- ⛔ **跨层级补差时只补当前层级**——如果跨越了多个层级（如直接跳到第三层），需逐层补差，不可仅补最高层级差额。

## 证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| Pricing Rule 数量区间折扣 | 🟢 | ERPNext Pricing Rule（min_qty / max_qty / discount_percentage） |
| Rebate Agreement 阶梯返利 | 🟢 | Odoo rebate 模块（协议+阶梯+计提+结算） |
| Credit Memo 结算返利 | 🟢 | Odoo rebate 信用单生成流程 |
| 追溯调整跨层级补差 | ⚪ | 返利计算通用逻辑 |

## 参考

- `contract/README.md`（合同实体）
- `docs/design/purchase/README.md`（采购订单引用合同行）
- `docs/design/sales/README.md`（销售订单引用合同行）
- `docs/design/finance/posting.md`（信用单过账）
