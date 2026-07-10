package app.erp.fin.dao.dto;

import java.math.BigDecimal;

/**
 * 预算控制检查结果（{@code budget.md §业务规则2/8}）。由 {@code IErpFinBudgetControlBiz.check} 返回：
 * <ul>
 *   <li>{@code actionResult} —— PASS（静默放行）/ WARNED（超预算但告警放行，写日志）/ BLOCKED（HARD 级别拦截，由调用方抛异常）</li>
 *   <li>{@code availableAmount} —— 检查时余量（budgetBalance − actualBalance，均从 VoucherLine 聚合）</li>
 *   <li>{@code budgetLineId} —— 命中的预算明细行（无匹配预算行时为 null，actionResult=PASS）</li>
 * </ul>
 *
 * <p>本类型位于 finance-dao（跨层契约面），供采购/付款/报销等域注入 {@code IErpFinBudgetControlBiz} 同步校验。
 */
public class BudgetCheckResult {

    public static final String ACTION_PASS = "PASS";
    public static final String ACTION_WARNED = "WARNED";
    public static final String ACTION_BLOCKED = "BLOCKED";

    private String actionResult = ACTION_PASS;
    private BigDecimal availableAmount = BigDecimal.ZERO;
    private Long budgetLineId;

    public BudgetCheckResult() {
    }

    public BudgetCheckResult(String actionResult, BigDecimal availableAmount, Long budgetLineId) {
        this.actionResult = actionResult;
        this.availableAmount = availableAmount == null ? BigDecimal.ZERO : availableAmount;
        this.budgetLineId = budgetLineId;
    }

    public String getActionResult() {
        return actionResult;
    }

    public void setActionResult(String actionResult) {
        this.actionResult = actionResult;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(BigDecimal availableAmount) {
        this.availableAmount = availableAmount == null ? BigDecimal.ZERO : availableAmount;
    }

    public Long getBudgetLineId() {
        return budgetLineId;
    }

    public void setBudgetLineId(Long budgetLineId) {
        this.budgetLineId = budgetLineId;
    }

    public boolean isBlocked() {
        return ACTION_BLOCKED.equals(actionResult);
    }
}
