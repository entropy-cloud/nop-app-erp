package app.erp.pur.biz;

import java.math.BigDecimal;

/**
 * 付款→发票域级核销分配明细（{@link IErpPurPaymentBiz#settle} 入参元素）。
 *
 * <p>每条分配指定「核销到哪张发票 + 核销多少金额」。核销约束（同供应商、双方已审核、金额不超余额）
 * 由 {@code PaymentSettler} 在执行时校验，违例抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public class SettlementAllocation {
    private Long invoiceId;
    private BigDecimal amount;

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
