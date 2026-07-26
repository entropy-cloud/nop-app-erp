# 2026-07-26-0410 采购承付 release hook 容错缺口（与销售不对称）

> Source: plan `docs/plans/2026-07-26-0410-2-commitment-accounting-browser-e2e.md` Phase 1（latent defect Fix）
> Date: 2026-07-26
> Severity: medium（阻断 config 启用后采购订单反审核/作废业务流）
> Status: fixed

## 现象

启用 `erp-fin.budget-commitment-enabled=true` 后，P2P 链路（PO→Receive→Invoice）末端发票审核（`ErpPurInvoice.approve`）触发接入点 #3（release-on-invoice-approve）已红冲原承付凭证。此后若对采购订单做 `reverseApprove` 或 `cancel`，接入点 #2（release-on-cancel）的 `release()` 因无未红冲承付抛 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`，且**采购 release hook 无 try-catch** → mutation 失败，阻断业务流。

## 根因

`ErpPurOrderProcessor.runCommitmentReleaseHook`（`module-purchase/erp-pur-service/.../processor/ErpPurOrderProcessor.java` 原行 239-245）直接调 `budgetCommitmentBiz.release(...)` 无 try-catch，与销售侧 `ErpSalOrderProcessor.runCommitmentReleaseHook:359-370`（有 try-catch）不对称。

不对称的来源：A2 承付落地（plan 2026-07-21-1206-2）的 release-on-cancel hook 设计本意是"无原凭证静默跳过（容错路径）"，但采购 hook 实现遗漏了 try-catch；销售扩展（plan 2026-07-24-1351-3）落地时正确镜像了"容错路径"语义。两域同时存在 latent defect，但因 config 默认关闭 + 既有 e2e 套件无 `runP2pChain` 后 `ErpPurOrder__reverseApprove` 的 spec（grep 全无命中），缺陷在全量门控下未暴露。

## Fix

`ErpPurOrderProcessor.runCommitmentReleaseHook` 补 try-catch + LOG 字段，镜像销售 hook `:359-370` 范式：

```java
protected void runCommitmentReleaseHook(ErpPurOrder order, IServiceContext context) {
    if (!Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE))) {
        return;
    }
    try {
        budgetCommitmentBiz.release(
                ErpFinConstants.COMMITMENT_SOURCE_BILL_PURCHASE_ORDER, order.getCode(), context);
    } catch (NopException e) {
        // 容错：无原凭证（ERR_BUDGET_COMMITMENT_ALREADY_RELEASED）静默跳过
        LOG.debug("commitment release skipped for purchase order {}: {}", order.getCode(), e.getMessage());
    }
}
```

catch `NopException` 后 LOG.debug 静默跳过（与销售 hook 同宽泛容错，不区分 ErrorCode），不阻断 reverseApprove/cancel 业务流。

## 预防

- **release/commit hook 容错对称性**：跨域同型 hook（如采购/销售 release）应保持容错策略对称。新增 hook 时应 grep 同型 hook 参照其容错范式，避免遗漏 try-catch。
- **config-gated 特性启用门控**：config 默认关闭的 feature 因门控下 latent defect 不易暴露，需在 config 启用的浏览器层 E2E（如本计划 `fin-commitment-accounting.action.spec.ts`）显式构造触发路径覆盖。
- **release-on-invoice-approve 与 release-on-cancel 的交互**：发票审核释放后订单反审核的"二次释放"场景是 config 启用后的回归必备覆盖点（接入点 #3 → 接入点 #2 序列）。
