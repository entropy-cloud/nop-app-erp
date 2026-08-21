package app.erp.fin.dao.dto;

import java.math.BigDecimal;

/**
 * 核销单行输入 DTO（{@code IErpFinReconciliationBiz.create} 的行参数）。
 * 每行指定一笔付款/收款项与一笔发票项之间的核销金额。
 */
public class ReconciliationLineInput {
    /** 付款/收款项辅助账 ID（ErpFinArApItem）。 */
    private String paymentItemId;
    /** 被核销发票项辅助账 ID（ErpFinArApItem）。 */
    private String invoiceItemId;
    /** 核销金额（源币）。 */
    private BigDecimal settledAmountSource;
    /** 核销金额（本位币）。 */
    private BigDecimal settledAmountFunctional;

    public String getPaymentItemId() {
        return paymentItemId;
    }

    public void setPaymentItemId(String paymentItemId) {
        this.paymentItemId = paymentItemId;
    }

    public String getInvoiceItemId() {
        return invoiceItemId;
    }

    public void setInvoiceItemId(String invoiceItemId) {
        this.invoiceItemId = invoiceItemId;
    }

    public BigDecimal getSettledAmountSource() {
        return settledAmountSource;
    }

    public void setSettledAmountSource(BigDecimal settledAmountSource) {
        this.settledAmountSource = settledAmountSource;
    }

    public BigDecimal getSettledAmountFunctional() {
        return settledAmountFunctional;
    }

    public void setSettledAmountFunctional(BigDecimal settledAmountFunctional) {
        this.settledAmountFunctional = settledAmountFunctional;
    }
}
