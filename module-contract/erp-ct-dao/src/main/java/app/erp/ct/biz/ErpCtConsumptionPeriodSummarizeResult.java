package app.erp.ct.biz;

import java.math.BigDecimal;

/**
 * 消耗计费周期汇总结果 DTO（RC-R1.33 P1-RC-075，D6 契约）。
 *
 * <p>超量金额推导（D6 选项 A）：{@code overQuantity × line.unitPrice}（scale 4 HALF_UP）；
 * 超量 InvoicePlan 生成 + 发票草稿触发的落库结果经 {@code overagePlanId}/{@code invoiceBillCode} 返回。
 */
public class ErpCtConsumptionPeriodSummarizeResult {

    /** 合同行 ID（入参回显）。 */
    private String contractLineId;

    /** 合同行预估总量（line.quantity）。 */
    private BigDecimal estimatedQuantity;

    /** 期间内 ConsumptionLine 汇总总量（Σ quantity）。 */
    private BigDecimal totalConsumedQuantity;

    /** 超量数量（Σ − 预估，≤0 时为 0）。 */
    private BigDecimal overQuantity;

    /** 超量金额（overQuantity × line.unitPrice，scale 4 HALF_UP）。 */
    private BigDecimal overAmount;

    /** 消耗/预估比值（预估为 0 时为 null）。 */
    private BigDecimal overRatio;

    /** 超量生成的 InvoicePlan ID（未超量为 null）。 */
    private String overagePlanId;

    /** 超量发票草稿单号（未超量或未触发为 null）。 */
    private String invoiceBillCode;

    /** 是否已派发超 120% 审批通知（无 ACTIVE 模板静默跳过时为 false）。 */
    private boolean notificationSent;

    public String getContractLineId() {
        return contractLineId;
    }

    public void setContractLineId(String contractLineId) {
        this.contractLineId = contractLineId;
    }

    public BigDecimal getEstimatedQuantity() {
        return estimatedQuantity;
    }

    public void setEstimatedQuantity(BigDecimal estimatedQuantity) {
        this.estimatedQuantity = estimatedQuantity;
    }

    public BigDecimal getTotalConsumedQuantity() {
        return totalConsumedQuantity;
    }

    public void setTotalConsumedQuantity(BigDecimal totalConsumedQuantity) {
        this.totalConsumedQuantity = totalConsumedQuantity;
    }

    public BigDecimal getOverQuantity() {
        return overQuantity;
    }

    public void setOverQuantity(BigDecimal overQuantity) {
        this.overQuantity = overQuantity;
    }

    public BigDecimal getOverAmount() {
        return overAmount;
    }

    public void setOverAmount(BigDecimal overAmount) {
        this.overAmount = overAmount;
    }

    public BigDecimal getOverRatio() {
        return overRatio;
    }

    public void setOverRatio(BigDecimal overRatio) {
        this.overRatio = overRatio;
    }

    public String getOveragePlanId() {
        return overagePlanId;
    }

    public void setOveragePlanId(String overagePlanId) {
        this.overagePlanId = overagePlanId;
    }

    public String getInvoiceBillCode() {
        return invoiceBillCode;
    }

    public void setInvoiceBillCode(String invoiceBillCode) {
        this.invoiceBillCode = invoiceBillCode;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }
}
