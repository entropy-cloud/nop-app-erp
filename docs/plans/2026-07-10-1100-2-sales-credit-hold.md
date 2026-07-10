# 2026-07-10-1100-2-sales-credit-hold 信用冻结：出库/发票审核环节信用控制

> Plan Status: active
> Last Reviewed: 2026-07-10 (iteration 1 — consensus)
> Source: erp-survey 对标调研（ERPNext Credit Limit 在 Sales Order + Sales Invoice 双点校验为标准功能）+ `docs/design/sales/README.md:98-99` Deferred successor
> Related: `docs/design/sales/README.md`（§信用额度控制 Non-Goals）、`docs/plans/2026-07-10-1100-1-sales-pricing-engine.md`（同属销售域增强）
> Audit: required

## Current Baseline

### 已实现

- **信用额度校验 `CreditLimitChecker`**（271 行）：三级策略（SOFT_WARNING/HARD_BLOCK/SPECIAL_APPROVAL）+ 多币种本位币折算 + AR 未核销余额纳入 + 超额度通知。`module-sales/erp-sal-service/.../entity/CreditLimitChecker.java`
- **唯一调用点**：`ErpSalOrderProcessor.validateBusinessRulesForApprove`（L158），订单审核 SUBMITTED→APPROVED 时调用。`module-sales/erp-sal-service/.../processor/ErpSalOrderProcessor.java`
- **配置项**：`erp-sal.credit-check-level`（默认 SOFT_WARNING）、`erp-sal.credit-check-include-ar`（默认 true）、`erp-sal.credit-check-ar-fallback`（默认 true）、`erp-sal.credit-notify-enabled`（默认 true）。`ErpSalConstants.java:44-62`
- **错误码**：`ERR_CREDIT_LIMIT_EXCEEDED`、`ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED`。`ErpSalErrors.java:86-92`
- **测试覆盖**：`TestErpSalOrderApproval`（三级策略）、`TestErpSalQuotationToOrder`（信用校验）、`TestErpSalCreditNotify`（通知）

### 剩余差距

- **出库单审核（`ErpSalDeliveryProcessor.validateBusinessRulesForApprove`）不调用信用校验**——仅校验客户启用（`requireCustomerActive`）+ 强制质检门控（`enforceInspectionGate`）
- **发票审核（`ErpSalInvoiceProcessor.validateBusinessRulesForApprove`）不调用信用校验**——仅校验客户启用
- **场景缺口**：客户订单审核通过后、出库前，若信用状况恶化（其他应收逾期、新增 AR 发票），无法在出库/开票环节拦截

### 对标依据

| 开源 ERP | 信用控制点 | 状态 |
|----------|-----------|------|
| **ERPNext** | Sales Order（创建/提交）+ Sales Invoice（提交）双点校验 | 核心内置 |
| **Odoo** | Sales Order 信用限额（社区/企业模块） | 核心内置 |
| **本项目** | 仅 Sales Order 审核（单一拦截点） | **gap（缺出库/发票点）** |

## Goals

- 在出库单审核环节增加信用额度校验（credit hold），拦截信用状况恶化的客户出库
- 在发票审核环节增加信用额度校验（credit hold），在开票前预警/拦截
- 复用现有 `CreditLimitChecker` 三级策略和配置体系，保持语义一致
- 新增配置开关控制是否启用出库/发票环节信用检查（默认关闭，保持向后兼容）

## Non-Goals

- **信用占用预留机制（credit reservation）**——即"订单审核时冻结额度、出库时释放、开票时转为 AR 占用"的正式预留/释放生命周期。本期仅做"当前信用状况"检查（point-in-time check），不做额度预留/释放记账。归 Deferred。
- **多级 `.xwf` 信用审批工作流链**——与现有 Non-Goal 一致，归 Deferred。
- **客户风险评分体系联动信用额度动态调整**——依赖 CRM 评分，归 Deferred。
- **跨账套 AR 余额聚合**——归 Deferred。
- **信用冻结自动通知财务总监**——通知使用现有 `erp-sal.credit-notify-enabled` 机制，不新增通知模板。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/sales/README.md`（§信用额度控制 Non-Goals → 标注已实现）、`docs/design/sales/state-machine.md`（§审核状态机）
- Skill Selection Basis: 修改现有 Processor + 新增 BizModel 方法 + 新增配置 → nop-backend-dev；GraphQL Engine 测试 → nop-testing

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - CreditLimitChecker 扩展 + 配置 + 错误码

Status: planned
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java`、`ErpSalConstants.java`、`ErpSalErrors.java`
Skill: nop-backend-dev

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] Decision: 信用冻结检查语义
  - 订单审核信用检查：`available >= thisOrderAmount`（本单金额加到 outstanding 上后是否超额）
  - 出库/发票审核信用检查：`available >= 0`（客户当前是否已超额，本单不额外增加 outstanding，因为订单已审核、额度已占用）
  - 理由：订单审核通过后额度已被占用；出库/发票审核是检查"额度占用后信用是否恶化"，而非新增占用
  - 替代方案：在出库审核时重新加入"未审核出库单金额"到 outstanding（更保守）——rejected，因为出库单来源于已审核订单，金额已在 outstanding 中
  - 残留风险：订单部分出库时，已出库部分会从 `sumOutstandingOrders` 中移除（deliveryStatus 变更），但暂估应收可能尚未进入 AR——本期接受此时间差（保守口径，不重复计入）
  - Skill: nop-backend-dev

- [ ] Add: `CreditLimitChecker.checkCreditHold(customerId, billCode, billType, context)` 新方法
  - 语义：检查客户**当前**信用状况是否已超额（`available < 0`）
  - 复用现有 `sumOutstanding` + 三级策略逻辑
  - `billType` 参数（"DELIVERY"/"INVOICE"）用于错误消息区分
  - 与现有 `check(customerId, amount, rate, orderCode, context)` 并行，不修改现有方法签名
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalConstants` 新增配置键
  - `CONFIG_CREDIT_CHECK_ON_DELIVERY = "erp-sal.credit-check-on-delivery"`（默认 `false`，向后兼容）
  - `CONFIG_CREDIT_CHECK_ON_INVOICE = "erp-sal.credit-check-on-invoice"`（默认 `false`，向后兼容）
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalErrors` 新增错误码
  - `ERR_CREDIT_HOLD_DELIVERY` = "客户 {customerId} 信用额度不足，出库单 {billCode} 被信用冻结：额度={creditLimit}，可用={available}"
  - `ERR_CREDIT_HOLD_INVOICE` = "客户 {customerId} 信用额度不足，发票 {billCode} 被信用冻结：额度={creditLimit}，可用={available}"
  - 理由：与订单审核的 `ERR_CREDIT_LIMIT_EXCEEDED` 区分，便于前端按场景展示不同提示；错误码不同但错误处理路径相同（NopException → GraphQL error）
  - Skill: nop-backend-dev

Exit Criteria:

- [ ] `CreditLimitChecker.checkCreditHold` 方法通过 nop-backend-dev 19 项自检
- [ ] 新增配置键和错误码编译无错误（`erp-sal-service` 模块 `mvn compile` 通过）

### Phase 2 - 出库/发票 Processor 集成

Status: planned
Targets: `ErpSalDeliveryProcessor.java`、`ErpSalInvoiceProcessor.java`
Skill: nop-backend-dev

- Item Types: `Add`
- Prereqs: Phase 1

- [ ] Add: `ErpSalDeliveryProcessor.validateBusinessRulesForApprove` 增加信用冻结检查
  - 在现有 `requireCustomerActive` 之后、`enforceInspectionGate` 之前插入：
  ```
  if config("erp-sal.credit-check-on-delivery", false):
      creditLimitChecker.checkCreditHold(delivery.customerId, delivery.code, "DELIVERY", context)
  ```
  - `@Inject CreditLimitChecker creditLimitChecker` 已在 OrderProcessor 中注入；DeliveryProcessor 需新增注入
  - Skill: nop-backend-dev

- [ ] Add: `ErpSalInvoiceProcessor.validateBusinessRulesForApprove` 增加信用冻结检查
  - 在现有 `requireCustomerActive` 之后插入：
  ```
  if config("erp-sal.credit-check-on-invoice", false):
      creditLimitChecker.checkCreditHold(invoice.customerId, invoice.code, "INVOICE", context)
  ```
  - Skill: nop-backend-dev

Exit Criteria:

- [ ] 出库单审核时，`credit-check-on-delivery=true` 且客户超额 → 抛 `ERR_CREDIT_HOLD_DELIVERY`，状态不变
- [ ] 发票审核时，`credit-check-on-invoice=true` 且客户超额 → 抛 `ERR_CREDIT_HOLD_INVOICE`，状态不变
- [ ] `credit-check-on-delivery=false`（默认）→ 出库审核不触发信用检查（向后兼容）

### Phase 3 - GraphQL Engine 集成测试

Status: planned
Targets: `module-sales/erp-sal-service/src/test/java/.../`
Skill: nop-testing

- Item Types: `Proof`
- Prereqs: Phase 2

- [ ] Proof: `TestErpSalCreditHoldOnDelivery`
  - 场景 1（HARD_BLOCK 拦截）：客户 creditLimit=10000，已有 APPROVED 未发货订单 8000 + AR open 3000 → outstanding=11000 > 10000 → 出库审核被拦截（`ERR_CREDIT_HOLD_DELIVERY`），状态保持 SUBMITTED
  - 场景 2（SOFT_WARNING 放行）：同上但 level=SOFT_WARNING → 出库审核通过（放行带告警）
  - 场景 3（SPECIAL_APPROVAL 权限放行）：同上但 level=SPECIAL_APPROVAL + 持有权限 → 出库审核通过
  - 场景 4（config 关闭）：`credit-check-on-delivery=false` → 即使超额也通过（向后兼容）
  - 场景 5（信用正常）：客户未超额 → 出库审核通过
  - 场景 6（多币种）：客户 creditLimit 本位币，订单/AR 含外币 → 汇率折算后超额 → 拦截
  - Skill: nop-testing

- [ ] Proof: `TestErpSalCreditHoldOnInvoice`
  - 场景 1（HARD_BLOCK 拦截）：同出库模式，发票审核被拦截
  - 场景 2（SOFT_WARNING 放行）
  - 场景 3（config 关闭 → 向后兼容）
  - Skill: nop-testing

- [ ] Proof: 回归验证——现有 `TestErpSalOrderApproval` 信用测试仍全绿（信用控制在订单环节行为不变）
  - Skill: nop-testing

Exit Criteria:

- [ ] 出库信用冻结测试全绿（≥6 场景）
- [ ] 发票信用冻结测试全绿（≥3 场景）
- [ ] 现有订单信用测试回归无失败

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0b65a53e3ffe3NCmnaEh30fiNY) — baseline 全部 8 项已核实（CreditLimitChecker 三级策略 + AR 纳入 :121-139/:196-234，唯一调用点 OrderProcessor:158，配置键 :45/51/55/62，错误码 :86/90，Delivery/Invoice Processor 确认未调信用校验）；Decision 理由+被拒替代+残留风险齐备；向后兼容（config 默认 false）一致；范围/退出标准/类型/技能/保护区域/测试覆盖/无范围蔓延均 PASS；无阻塞项。**草案审查一次收敛，状态 draft→active。**

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（`docs/design/sales/README.md` §信用额度控制 Non-Goals 更新：标注"出库/发票信用冻结已实现"；原 Deferred successor 标注已完成）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ `mvn test -pl module-sales/erp-sal-service`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 信用占用预留机制（credit reservation）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 正式预留/释放生命周期（订单审核冻结额度→出库释放→开票转 AR 占用）是更高级的信用管理。本期 point-in-time check 已覆盖核心场景（信用恶化拦截）
- Successor Required: yes（触发条件：需要精确额度预留会计的高信用管控场景落地时）

### 发票审核信用冻结在全电发票场景的适用性

- Classification: `watch-only residual`
- Why Not Blocking Closure: 全电发票（数电发票）场景下，开票可能通过外部税控平台直连完成，ERP 发票审核环节可能被绕过。此处信用冻结仅适用于 ERP 内部发票审核流程
- Successor Required: no

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- none
