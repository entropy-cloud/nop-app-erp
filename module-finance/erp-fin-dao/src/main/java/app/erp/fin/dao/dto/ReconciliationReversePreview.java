package app.erp.fin.dao.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 核销单冲销预览（{@code IErpFinReconciliationBiz.previewReverse} 返回值，plan 2026-07-23-1145-2 Phase 2）。
 *
 * <p>只读预览，不执行实际冲销。反映 {@code reverse(reconciliationId)} 的真实副作用：
 * 双方辅助账（{@code ErpFinArApItem}）SETTLED→OPEN/PARTIAL 回退 + 往来单位余额将刷新。
 *
 * <p>核销冲销<b>不生成 GL 凭证</b>（凭证由收付款审核时生成，{@code ar-ap-reconciliation.md §核销流程} 步骤5），
 * 故本预览不含红字凭证段。
 */
public class ReconciliationReversePreview {
    private Long reconciliationId;
    private String code;
    private String direction;
    private BigDecimal totalAmountFunctional;
    private Long partnerId;
    /** reverse 将把核销单 docStatus 置为 REVERSED。 */
    private boolean willSetReversed;
    /** partner 余额将刷新（refresh）。 */
    private boolean willRefreshPartnerBalance;
    /** 将回退的 AR/AP 辅助账项列表。 */
    private List<RevertedItem> revertedItems = new ArrayList<>();

    public Long getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(Long reconciliationId) { this.reconciliationId = reconciliationId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public BigDecimal getTotalAmountFunctional() { return totalAmountFunctional; }
    public void setTotalAmountFunctional(BigDecimal totalAmountFunctional) { this.totalAmountFunctional = totalAmountFunctional; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public boolean isWillSetReversed() { return willSetReversed; }
    public void setWillSetReversed(boolean willSetReversed) { this.willSetReversed = willSetReversed; }
    public boolean isWillRefreshPartnerBalance() { return willRefreshPartnerBalance; }
    public void setWillRefreshPartnerBalance(boolean willRefreshPartnerBalance) { this.willRefreshPartnerBalance = willRefreshPartnerBalance; }
    public List<RevertedItem> getRevertedItems() { return revertedItems; }
    public void setRevertedItems(List<RevertedItem> revertedItems) { this.revertedItems = revertedItems; }

    public static class RevertedItem {
        private Long arApItemId;
        private String sourceBillType;
        private String sourceBillCode;
        /** 核销单行侧（payment 或 invoice）。 */
        private String side;
        private String currentStatus;
        /** 回退后预估状态（OPEN=完全回退开口 / PARTIAL=部分核销保留）。 */
        private String willBecomeStatus;
        /** 本次将恢复到开口的核销金额（本位币）。 */
        private BigDecimal restoreAmountFunctional;

        public Long getArApItemId() { return arApItemId; }
        public void setArApItemId(Long arApItemId) { this.arApItemId = arApItemId; }
        public String getSourceBillType() { return sourceBillType; }
        public void setSourceBillType(String sourceBillType) { this.sourceBillType = sourceBillType; }
        public String getSourceBillCode() { return sourceBillCode; }
        public void setSourceBillCode(String sourceBillCode) { this.sourceBillCode = sourceBillCode; }
        public String getSide() { return side; }
        public void setSide(String side) { this.side = side; }
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
        public String getWillBecomeStatus() { return willBecomeStatus; }
        public void setWillBecomeStatus(String willBecomeStatus) { this.willBecomeStatus = willBecomeStatus; }
        public BigDecimal getRestoreAmountFunctional() { return restoreAmountFunctional; }
        public void setRestoreAmountFunctional(BigDecimal restoreAmountFunctional) { this.restoreAmountFunctional = restoreAmountFunctional; }
    }
}
