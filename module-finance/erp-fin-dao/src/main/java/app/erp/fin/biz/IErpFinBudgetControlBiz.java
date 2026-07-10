package app.erp.fin.biz;

import app.erp.fin.dao.dto.BudgetCheckResult;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;

import java.math.BigDecimal;

/**
 * 预算控制跨域 SPI（{@code budget.md §业务规则2/8}）。作为业务校验扩展点，在采购/付款/报销等域
 * 审核动作的事务内同步调用，实现强一致预算余量校验。
 *
 * <p>实现：{@code ErpFinBudgetControlBiz}（finance-service）。预算余量从 {@code ErpFinVoucherLine}
 * 按关联凭证 {@code postingType} 聚合（BUDGET 凭证=预算数，NORMAL 凭证=实际数），不写 GlBalance。
 *
 * <p>本接口位于 finance-dao（跨层契约面），供其他域注入。控制开关 {@code erp-fin.budget-check-enabled}（默认 false）。
 */
public interface IErpFinBudgetControlBiz {

    /**
     * 检查指定维度的预算余量是否足以承担申请金额。
     *
     * @param subjectId     预算科目
     * @param costCenterId  成本中心（可空）
     * @param periodId      会计期间（可空，空表示不按期间约束）
     * @param amount        申请占用金额（本位币）
     * @param sourceBillType 触发单据类型（如 PURCHASE_ORDER）
     * @param sourceBillCode 触发单据号
     * @param context       服务上下文
     * @return 控制结果：PASS/WARNED/BLOCKED + availableAmount + budgetLineId
     */
    @BizMutation
    BudgetCheckResult check(@Name("subjectId") Long subjectId,
                            @Name("costCenterId") Long costCenterId,
                            @Name("periodId") Long periodId,
                            @Name("amount") BigDecimal amount,
                            @Name("sourceBillType") String sourceBillType,
                            @Name("sourceBillCode") String sourceBillCode,
                            IServiceContext context);
}
