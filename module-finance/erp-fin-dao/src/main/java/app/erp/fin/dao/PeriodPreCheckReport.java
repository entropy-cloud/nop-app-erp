package app.erp.fin.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 期末结账前置检查报告（{@code period-close.md §结账前置检查}）。由 {@code IErpFinPeriodCloseBiz.preCheck} 产出，
 * 列出本期未过账凭证、未核销应收应付等问题清单，供结账编排决定阻断或提示。
 *
 * <p>本类型位于 finance-dao（跨层契约面），供 BizModel 返回给调用方/前端。
 */
public class PeriodPreCheckReport {

    /** 本期未过账凭证号清单（docStatus != POSTED）。 */
    private List<String> unpostedVoucherCodes = new ArrayList<>();
    /** 本期未核销应收应付辅助账号清单（status != SETTLED/CANCELLED）。 */
    private List<String> unsettledArApCodes = new ArrayList<>();

    public List<String> getUnpostedVoucherCodes() {
        return unpostedVoucherCodes;
    }

    public void setUnpostedVoucherCodes(List<String> unpostedVoucherCodes) {
        this.unpostedVoucherCodes = unpostedVoucherCodes == null ? Collections.emptyList() : unpostedVoucherCodes;
    }

    public List<String> getUnsettledArApCodes() {
        return unsettledArApCodes;
    }

    public void setUnsettledArApCodes(List<String> unsettledArApCodes) {
        this.unsettledArApCodes = unsettledArApCodes == null ? Collections.emptyList() : unsettledArApCodes;
    }

    /** 是否存在任何前置检查问题。 */
    public boolean hasIssues() {
        return !unpostedVoucherCodes.isEmpty() || !unsettledArApCodes.isEmpty();
    }

    public int issueCount() {
        return unpostedVoucherCodes.size() + unsettledArApCodes.size();
    }
}
