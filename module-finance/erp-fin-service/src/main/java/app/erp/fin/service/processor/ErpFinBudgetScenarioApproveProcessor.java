package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractApproveProcessor;
import app.erp.fin.service.budget.ErpFinBudgetScenarioProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinBudgetScenario approve per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractApproveProcessor to activate the abstract base class; delegates to ErpFinBudgetScenarioProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinBudgetScenarioApproveProcessor extends AbstractApproveProcessor<ErpFinBudgetScenario> {

    @Inject
    ErpFinBudgetScenarioProcessor processor;

    @Override
    public ErpFinBudgetScenario approve(String id, IServiceContext context) {
        ErpFinBudgetScenario scenario = processor.requireScenario(Long.valueOf(id));
        processor.validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_APPROVED,
                ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        processor.generateBudgetVoucher(scenario, context);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        scenario.setApproveStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        processor.save(scenario);
        return scenario;
    }

    @Override
    protected IEntityDao<ErpFinBudgetScenario> dao() {
        return daoProvider.daoFor(ErpFinBudgetScenario.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpFinBudgetScenario entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpFinBudgetScenario entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedBy(ErpFinBudgetScenario entity, String userId) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedAt(ErpFinBudgetScenario entity, java.sql.Timestamp ts) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isApproved(ErpFinBudgetScenario entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpFinBudgetScenario entity) {
        return false;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }

    @Override
    protected String approvedStatus() {
        return null;
    }
}
