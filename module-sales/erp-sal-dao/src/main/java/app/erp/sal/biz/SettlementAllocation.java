package app.erp.sal.biz;

import java.math.BigDecimal;

/**
 * 收款→发票域级核销分配明细（{@link IErpSalReceiptBiz#settle} 入参元素）。
 *
 * <p>每条分配指定「核销到哪张发票 + 核销多少金额」。核销约束（同客户、双方已审核、金额不超余额）
 * 由 {@code ReceiptSettler} 在执行时校验，违例抛 {@link io.nop.api.core.exceptions.NopException}。
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
