package app.erp.fin.dao.dto;

import java.math.BigDecimal;

/**
 * 坏账准备计提结果（{@code bad-debt.md §坏账准备计提方法}）。由账龄分桶法计算：
 * {@code 必需准备 = Σ(各账龄区间应收 openAmount × 该区间历史损失率)}，排除争议/负余额/已核销项。
 *
 * <p>本类型位于 finance-dao（跨层契约面），供 BizModel 返回给调用方/前端，并供期末 allowance 门控消费。
 */
public class BadDebtProvisionResult {

    private BigDecimal requiredProvision = BigDecimal.ZERO;
    private BigDecimal allowanceBalance = BigDecimal.ZERO;
    private BigDecimal bucket030 = BigDecimal.ZERO;
    private BigDecimal bucket3160 = BigDecimal.ZERO;
    private BigDecimal bucket6190 = BigDecimal.ZERO;
    private BigDecimal bucket91180 = BigDecimal.ZERO;
    private BigDecimal bucket180Plus = BigDecimal.ZERO;
    private BigDecimal totalConsidered = BigDecimal.ZERO;
    /** 本次动作：RESERVE（补提）/ RELEASE（释放）/ NONE（充足，无动作）。 */
    private String action = "NONE";
    /** 本次生成的凭证 ID（无动作时为 null）。 */
    private Long voucherId;

    public BigDecimal getRequiredProvision() {
        return requiredProvision;
    }

    public void setRequiredProvision(BigDecimal requiredProvision) {
        this.requiredProvision = requiredProvision == null ? BigDecimal.ZERO : requiredProvision;
    }

    public BigDecimal getAllowanceBalance() {
        return allowanceBalance;
    }

    public void setAllowanceBalance(BigDecimal allowanceBalance) {
        this.allowanceBalance = allowanceBalance == null ? BigDecimal.ZERO : allowanceBalance;
    }

    public BigDecimal getBucket030() {
        return bucket030;
    }

    public void setBucket030(BigDecimal bucket030) {
        this.bucket030 = bucket030 == null ? BigDecimal.ZERO : bucket030;
    }

    public BigDecimal getBucket3160() {
        return bucket3160;
    }

    public void setBucket3160(BigDecimal bucket3160) {
        this.bucket3160 = bucket3160 == null ? BigDecimal.ZERO : bucket3160;
    }

    public BigDecimal getBucket6190() {
        return bucket6190;
    }

    public void setBucket6190(BigDecimal bucket6190) {
        this.bucket6190 = bucket6190 == null ? BigDecimal.ZERO : bucket6190;
    }

    public BigDecimal getBucket91180() {
        return bucket91180;
    }

    public void setBucket91180(BigDecimal bucket91180) {
        this.bucket91180 = bucket91180 == null ? BigDecimal.ZERO : bucket91180;
    }

    public BigDecimal getBucket180Plus() {
        return bucket180Plus;
    }

    public void setBucket180Plus(BigDecimal bucket180Plus) {
        this.bucket180Plus = bucket180Plus == null ? BigDecimal.ZERO : bucket180Plus;
    }

    public BigDecimal getTotalConsidered() {
        return totalConsidered;
    }

    public void setTotalConsidered(BigDecimal totalConsidered) {
        this.totalConsidered = totalConsidered == null ? BigDecimal.ZERO : totalConsidered;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }
}
