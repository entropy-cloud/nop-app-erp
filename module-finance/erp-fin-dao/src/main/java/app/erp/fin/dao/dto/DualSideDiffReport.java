package app.erp.fin.dao.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 双面对账一致性差异报告（{@code IErpFinReconciliationBiz.checkDualSideConsistency} 返回值）。
 *
 * <p>比对 finance 侧 {@code ErpFinArApItem}（核销单正式核销）vs 域级侧
 * （purchase {@code ErpPurPaymentLine} / sales {@code ErpSalReceiptLine} 运营核销）
 * 在同一 partner+direction 下的发票已核销额。差额超 {@code erp-fin.reconcile-precision} 标记 INCONSISTENT。
 *
 * <p>报告只读 + 日志告警，不自动修复（避免静默修改域级核销权威）。
 */
public class DualSideDiffReport {
    private String direction;
    private Long partnerId;
    /** 全部 partner 级差异行（partnerId 为 null 时为全量，非 null 时仅含该 partner）。 */
    private List<DualSideDiffRow> rows = new ArrayList<>();
    /** 是否一致（所有 row 差额 ≤ precision）。 */
    private boolean consistent = true;

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public List<DualSideDiffRow> getRows() {
        return rows;
    }

    public void setRows(List<DualSideDiffRow> rows) {
        this.rows = rows;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public static class DualSideDiffRow {
        private Long partnerId;
        /** finance 侧已核销额（本位币）——ErpFinArApItem.settledAmountFunctional 聚合（发票项）。 */
        private BigDecimal financeSettled;
        /** 域级侧已核销额（本位币）——ErpPurInvoice.paidAmount / ErpSalInvoice.receivedAmount 聚合。 */
        private BigDecimal domainSettled;
        /** 差额 = financeSettled - domainSettled。 */
        private BigDecimal diff;
        /** CONSISTENT / INCONSISTENT。 */
        private String status;

        public Long getPartnerId() {
            return partnerId;
        }

        public void setPartnerId(Long partnerId) {
            this.partnerId = partnerId;
        }

        public BigDecimal getFinanceSettled() {
            return financeSettled;
        }

        public void setFinanceSettled(BigDecimal financeSettled) {
            this.financeSettled = financeSettled;
        }

        public BigDecimal getDomainSettled() {
            return domainSettled;
        }

        public void setDomainSettled(BigDecimal domainSettled) {
            this.domainSettled = domainSettled;
        }

        public BigDecimal getDiff() {
            return diff;
        }

        public void setDiff(BigDecimal diff) {
            this.diff = diff;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
