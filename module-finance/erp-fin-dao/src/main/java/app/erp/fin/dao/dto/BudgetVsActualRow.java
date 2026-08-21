package app.erp.fin.dao.dto;

import java.math.BigDecimal;

/**
 * 预算对比报表行（{@code budget.md §业务规则5 预算对比}）。按 {@code (subjectId, periodId, costCenterId, projectId)}
 * 维度从 {@code ErpFinVoucherLine} 聚合，关联 {@code ErpFinVoucher.postingType} 得到三列：
 * <ul>
 *   <li>{@code budgetAmount} —— postingType=BUDGET 凭证行累计（预算数）</li>
 *   <li>{@code commitmentAmount} —— postingType=COMMITMENT 凭证行累计（承付款）</li>
 *   <li>{@code actualAmount} —— postingType 非 BUDGET/COMMITMENT（NORMAL/NULL/RESERVATION 等）凭证行累计（实际数）</li>
 *   <li>{@code availableAmount} —— budgetAmount − actualAmount − commitmentAmount（预算余量）</li>
 * </ul>
 *
 * <p>本类型位于 finance-dao（跨层契约面），供 {@code ErpFinBudgetLineBizModel.getBudgetVsActual} 返回给前端/报表。
 */
public class BudgetVsActualRow {

    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String periodId;
    private String costCenterId;
    private Long projectId;
    private BigDecimal budgetAmount = BigDecimal.ZERO;
    private BigDecimal commitmentAmount = BigDecimal.ZERO;
    private BigDecimal actualAmount = BigDecimal.ZERO;
    private BigDecimal availableAmount = BigDecimal.ZERO;

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getPeriodId() { return periodId; }
    public void setPeriodId(String periodId) { this.periodId = periodId; }

    public String getCostCenterId() { return costCenterId; }
    public void setCostCenterId(String costCenterId) { this.costCenterId = costCenterId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount == null ? BigDecimal.ZERO : budgetAmount;
    }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount == null ? BigDecimal.ZERO : actualAmount;
    }

    public BigDecimal getCommitmentAmount() { return commitmentAmount; }
    public void setCommitmentAmount(BigDecimal commitmentAmount) {
        this.commitmentAmount = commitmentAmount == null ? BigDecimal.ZERO : commitmentAmount;
    }

    public BigDecimal getAvailableAmount() { return availableAmount; }
    public void setAvailableAmount(BigDecimal availableAmount) {
        this.availableAmount = availableAmount == null ? BigDecimal.ZERO : availableAmount;
    }
}
