package app.erp.md.biz;

import java.math.BigDecimal;

/**
 * 核销分配明细——指定核销到哪张发票/应付单（invoiceId）以及核销多少金额（amount）。
 *
 * <p>提供者只负责将分配指令传递给 settler；核销约束（同往来单位、双方已审核、金额不超余额）
 * 由各域 {@code Settler} 在执行时校验，违例抛 {@link io.nop.api.core.exceptions.NopException}。
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
