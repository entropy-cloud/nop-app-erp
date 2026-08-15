package app.erp.ct.biz;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开票计划批量生成项 DTO（RC-R1.33 P1-RC-074，D2/D3 契约）。
 *
 * <p>生成契约经入参承载 invoiceTerm（L1「合同行已配置 invoiceTerm」与 ORM 结构错配的入参化解释——
 * {@code ErpCtInvoicePlan.invoiceTerm} mandatory 而 {@code ErpCtContractLine} 无 invoiceTerm 列，
 * 裁决见 plan 2026-08-15-0456-3 Phase 1 Decision Record D2）。planDate 由调用方显式提供（D3）。
 */
public class ErpCtInvoicePlanGenerateItem {

    private Long contractLineId;

    private String invoiceTerm;

    private LocalDate planDate;

    private BigDecimal amount;

    public Long getContractLineId() {
        return contractLineId;
    }

    public void setContractLineId(Long contractLineId) {
        this.contractLineId = contractLineId;
    }

    public String getInvoiceTerm() {
        return invoiceTerm;
    }

    public void setInvoiceTerm(String invoiceTerm) {
        this.invoiceTerm = invoiceTerm;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
