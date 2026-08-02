package app.erp.fin.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 期末结账前置检查报告（{@code period-close.md §结账前置检查}）。由 {@code IErpFinPeriodCloseBiz.preCheck} 产出，
 * 列出本期未过账凭证、未核销应收应付、未处置过账异常、坏账准备充足性等问题清单，供结账编排决定阻断或提示。
 *
 * <p>本类型位于 finance-dao（跨层契约面），供 BizModel 返回给调用方/前端。
 */
public class PeriodPreCheckReport {

    /** 本期未过账凭证号清单（docStatus != POSTED）。 */
    private List<String> unpostedVoucherCodes = new ArrayList<>();
    /** 本期未核销应收应付辅助账号清单（status != SETTLED/CANCELLED/WRITTEN_OFF）。 */
    private List<String> unsettledArApCodes = new ArrayList<>();
    /** 本期未处置过账异常记录键清单（status = PENDING/RETRYING，见 posting-log.md §过账异常处置）。 */
    private List<String> unresolvedPostingExceptionKeys = new ArrayList<>();

    /** 坏账准备必需金额（账龄分桶法计算，{@code bad-debt.md §期末 allowance 充足性门控}）。 */
    private BigDecimal allowanceRequired = BigDecimal.ZERO;
    /** 当前 Allowance GL 账面（cumulative 抵减资产余额）。 */
    private BigDecimal allowanceBalance = BigDecimal.ZERO;
    /** Allowance 缺口（必需 − 账面）；&gt; 0 表示不足，阻止结账（提示补提）。 */
    private BigDecimal allowanceShortfall = BigDecimal.ZERO;
    /** Allowance 超额（账面 − 必需）；&gt; 0 表示过剩，提示释放（非阻断）。 */
    private BigDecimal allowanceExcess = BigDecimal.ZERO;

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

    public List<String> getUnresolvedPostingExceptionKeys() {
        return unresolvedPostingExceptionKeys;
    }

    public void setUnresolvedPostingExceptionKeys(List<String> unresolvedPostingExceptionKeys) {
        this.unresolvedPostingExceptionKeys = unresolvedPostingExceptionKeys == null
                ? Collections.emptyList() : unresolvedPostingExceptionKeys;
    }

    public BigDecimal getAllowanceRequired() {
        return allowanceRequired;
    }

    public void setAllowanceRequired(BigDecimal allowanceRequired) {
        this.allowanceRequired = allowanceRequired == null ? BigDecimal.ZERO : allowanceRequired;
    }

    public BigDecimal getAllowanceBalance() {
        return allowanceBalance;
    }

    public void setAllowanceBalance(BigDecimal allowanceBalance) {
        this.allowanceBalance = allowanceBalance == null ? BigDecimal.ZERO : allowanceBalance;
    }

    public BigDecimal getAllowanceShortfall() {
        return allowanceShortfall;
    }

    public void setAllowanceShortfall(BigDecimal allowanceShortfall) {
        this.allowanceShortfall = allowanceShortfall == null ? BigDecimal.ZERO : allowanceShortfall;
    }

    public BigDecimal getAllowanceExcess() {
        return allowanceExcess;
    }

    public void setAllowanceExcess(BigDecimal allowanceExcess) {
        this.allowanceExcess = allowanceExcess == null ? BigDecimal.ZERO : allowanceExcess;
    }

    /**
     * 是否存在任何阻断性前置检查问题（不含未核销 AR-AP 提示——未核销=结构化提示非阻断，owner doc §结账前置检查；
     * 不含 Allowance shortfall——shortfall 由 closePeriod 独立硬阻断，不受 auto-post-on-close 影响）。
     */
    public boolean hasIssues() {
        return !unpostedVoucherCodes.isEmpty()
                || !unresolvedPostingExceptionKeys.isEmpty();
    }

    /**
     * 是否存在非阻断的结构化提示项（未核销 AR-AP + Allowance 超额）。
     * 这些项列出供用户参考但不阻止结账（owner doc §结账前置检查「未核销=提示」）。
     */
    public boolean hasReminders() {
        return !unsettledArApCodes.isEmpty()
                || allowanceExcess.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Allowance shortfall 是否 > 0（独立硬阻断条件，不受 auto-post-on-close 影响，bad-debt.md §期末 allowance 充足性门控）。
     */
    public boolean hasAllowanceShortfall() {
        return allowanceShortfall.compareTo(BigDecimal.ZERO) > 0;
    }

    public int issueCount() {
        return unpostedVoucherCodes.size() + unresolvedPostingExceptionKeys.size();
    }
}
