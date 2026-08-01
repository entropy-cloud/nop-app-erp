
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBudgetScenarioBiz;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.service.processor.ErpFinBudgetScenarioApproveProcessor;
import app.erp.fin.service.processor.ErpFinBudgetScenarioCancelProcessor;
import app.erp.fin.service.processor.ErpFinBudgetScenarioCarryForwardProcessor;
import app.erp.fin.service.processor.ErpFinBudgetScenarioRejectProcessor;
import app.erp.fin.service.processor.ErpFinBudgetScenarioRollForwardProcessor;
import app.erp.fin.service.processor.ErpFinBudgetScenarioSubmitForApprovalProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 预算方案聚合根 Biz（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 状态机（DRAFT→SUBMITTED→APPROVED / →REJECTED / APPROVED→CANCELLED）与 BUDGET 凭证生成委托
 * 对应 per-mutation Processor；{@code @BizMutation} 钉事务/会话边界。
 *
 * <p>语义见 {@code budget.md §ErpFinBudgetScenario}。
 */
@BizModel("ErpFinBudgetScenario")
public class ErpFinBudgetScenarioBizModel extends CrudBizModel<ErpFinBudgetScenario> implements IErpFinBudgetScenarioBiz {

    @Inject
    ErpFinBudgetScenarioSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpFinBudgetScenarioApproveProcessor approveProcessor;

    @Inject
    ErpFinBudgetScenarioRejectProcessor rejectProcessor;

    @Inject
    ErpFinBudgetScenarioCancelProcessor cancelProcessor;

    @Inject
    ErpFinBudgetScenarioRollForwardProcessor rollForwardProcessor;

    @Inject
    ErpFinBudgetScenarioCarryForwardProcessor carryForwardProcessor;

    public ErpFinBudgetScenarioBizModel() {
        setEntityName(ErpFinBudgetScenario.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinBudgetScenario submit(@Name("id") Long id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(String.valueOf(id), context);
    }

    @Override
    @BizMutation
    public ErpFinBudgetScenario approve(@Name("id") Long id, IServiceContext context) {
        return approveProcessor.approve(String.valueOf(id), context);
    }

    @Override
    @BizMutation
    public ErpFinBudgetScenario reject(@Name("id") Long id, IServiceContext context) {
        return rejectProcessor.reject(String.valueOf(id), context);
    }

    @Override
    @BizMutation
    public ErpFinBudgetScenario cancel(@Name("id") Long id, IServiceContext context) {
        return cancelProcessor.cancel(String.valueOf(id), context);
    }

    @Override
    @BizMutation
    public ErpFinBudgetScenario rollForward(@Name("id") Long id,
                                            @Name("newFiscalYear") Integer newFiscalYear,
                                            @Name("strategy") String strategy,
                                            IServiceContext context) {
        return rollForwardProcessor.rollForward(id, newFiscalYear, strategy, context);
    }

    @Override
    @BizMutation
    public ErpFinBudgetScenario carryForward(@Name("id") Long id,
                                             @Name("targetScenarioId") Long targetScenarioId,
                                             @Name("rule") String rule,
                                             IServiceContext context) {
        return carryForwardProcessor.carryForward(id, targetScenarioId, rule, context);
    }
}
