package app.erp.fin.dao.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动核销结果（{@code IErpFinReconciliationBiz.runAutoReconciliation} 返回值）。
 *
 * <p>包含本次生成的核销单 ID 列表 + 未匹配项报告（按 partner 汇总未匹配原因）。
 * 幂等：重复执行只处理剩余开口项，已 SETTLED/CANCELLED 项不进入候选。
 */
public class AutoReconResult {
    /** 本次生成的核销单 ID 列表（每 partner+direction+批次一张）。 */
    private List<Long> reconciliationIds = new ArrayList<>();
    /** 未匹配项报告（按 partner 汇总）。 */
    private List<AutoReconUnmatched> unmatched = new ArrayList<>();

    public List<Long> getReconciliationIds() {
        return reconciliationIds;
    }

    public void setReconciliationIds(List<Long> reconciliationIds) {
        this.reconciliationIds = reconciliationIds;
    }

    public List<AutoReconUnmatched> getUnmatched() {
        return unmatched;
    }

    public void setUnmatched(List<AutoReconUnmatched> unmatched) {
        this.unmatched = unmatched;
    }
}
