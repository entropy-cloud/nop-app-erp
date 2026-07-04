package app.erp.fin.dao.dto;

/**
 * 自动核销未匹配项报告行。记录某 partner+direction 下未能匹配的开口项及原因。
 */
public class AutoReconUnmatched {
    private Long partnerId;
    private String direction;
    /** 未匹配的辅助账项 ID。 */
    private Long arApItemId;
    /** 未匹配项的开口余额（本位币）。 */
    private java.math.BigDecimal openAmount;
    /** 未匹配原因：NO_COUNTERPART（无对侧开口项）/ NO_CANDIDATE（金额无候选，如 BY_AMOUNT 非唯一）/ OVER_AMOUNT（超额）。 */
    private String unmatchedReason;

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getArApItemId() {
        return arApItemId;
    }

    public void setArApItemId(Long arApItemId) {
        this.arApItemId = arApItemId;
    }

    public java.math.BigDecimal getOpenAmount() {
        return openAmount;
    }

    public void setOpenAmount(java.math.BigDecimal openAmount) {
        this.openAmount = openAmount;
    }

    public String getUnmatchedReason() {
        return unmatchedReason;
    }

    public void setUnmatchedReason(String unmatchedReason) {
        this.unmatchedReason = unmatchedReason;
    }
}
