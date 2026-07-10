package app.erp.fin.dao.dto;

import java.math.BigDecimal;

/**
 * 预算对比报表行（{@code budget.md §业务规则5 预算对比}）。按 {@code (subjectId, periodId, costCenterId, projectId)}
 * 维度从 {@code ErpFinVoucherLine} 聚合，关联 {@code ErpFinVoucher.postingType} 得到三列：
 * <ul>
 *   <li>{@code budgetAmount} —— postingType=BUDGET 凭证行累计（预算数）</li>
 *   <li>{@code actualAmount} —— postingType=NORMAL（含 NULL）凭证行累计（实际数）</li>
 *   <li>{@code availableAmount} —— budgetAmount − actualAmount（预算余量）</li>
 * </ul>
 *
 * <p>本类型位于 finance-dao（跨层契约面），供 {@code ErpFinBudgetLineBizModel.getBudgetVsActual} 返回给前端/报表。
 */
public class BudgetVsActualRow {

    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long periodId;
    private Long costCenterId;
    private Long projectId;
    private BigDecimal budgetAmount = BigDecimal.ZERO;
    private BigDecimal actualAmount = BigDecimal.ZERO;
    private BigDecimal availableAmount = BigDecimal.ZERO;

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long costCenterId) { this.costCenterId = costCenterId; }

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

    public BigDecimal getAvailableAmount() { return availableAmount; }
    public void setAvailableAmount(BigDecimal availableAmount) {
        this.availableAmount = availableAmount == null ? BigDecimal.ZERO : availableAmount;
    }
}
