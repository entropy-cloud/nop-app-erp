package app.erp.fin.dao.dto;

import java.math.BigDecimal;

/**
 * 坏账准备反向红冲结果（{@code bad-debt.md §步骤2b 反向红冲}）。{@code reverseBadDebtProvision(periodId)}
 * 经 {@code ErpFinVoucherBillR} 反查指定期间全部 BAD_DEBT_RESERVE/RELEASE 凭证 → 调
 * {@code FinPostingExecutor.reverse(billCode, businessType)} 原子红冲 → 返回本结果。
 *
 * <p>计数维度按方向拆分：reserveCount/Amount 对应 BAD_DEBT_RESERVE 凭证（Dr 6701 信用减值损失 / Cr 1231 坏账准备），
 * releaseCount/Amount 对应 BAD_DEBT_RELEASE 凭证（Dr 1231 / Cr 6701，唯一贷记 Bad Debt Expense 场景）。
 * 反向后调用方可再调 {@code runBadDebtProvision(periodId)} 重提，{@code getAllowanceBalance} 将基于红冲后状态重算。
 *
 * <p>本类型位于 finance-dao（跨层契约面），供 BizModel 返回给调用方/前端。
 */
public class BadDebtProvisionReversalResult {

    private Long periodId;
    private String periodCode;
    private int reversedReserveCount;
    private int reversedReleaseCount;
    private BigDecimal reversedReserveAmount = BigDecimal.ZERO;
    private BigDecimal reversedReleaseAmount = BigDecimal.ZERO;

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public String getPeriodCode() {
        return periodCode;
    }

    public void setPeriodCode(String periodCode) {
        this.periodCode = periodCode;
    }

    public int getReversedReserveCount() {
        return reversedReserveCount;
    }

    public void setReversedReserveCount(int reversedReserveCount) {
        this.reversedReserveCount = reversedReserveCount;
    }

    public int getReversedReleaseCount() {
        return reversedReleaseCount;
    }

    public void setReversedReleaseCount(int reversedReleaseCount) {
        this.reversedReleaseCount = reversedReleaseCount;
    }

    public BigDecimal getReversedReserveAmount() {
        return reversedReserveAmount;
    }

    public void setReversedReserveAmount(BigDecimal reversedReserveAmount) {
        this.reversedReserveAmount = reversedReserveAmount == null ? BigDecimal.ZERO : reversedReserveAmount;
    }

    public BigDecimal getReversedReleaseAmount() {
        return reversedReleaseAmount;
    }

    public void setReversedReleaseAmount(BigDecimal reversedReleaseAmount) {
        this.reversedReleaseAmount = reversedReleaseAmount == null ? BigDecimal.ZERO : reversedReleaseAmount;
    }

    /** 红冲凭证总数（BDR + BDL）。 */
    public int getTotalReversedCount() {
        return reversedReserveCount + reversedReleaseCount;
    }
}
