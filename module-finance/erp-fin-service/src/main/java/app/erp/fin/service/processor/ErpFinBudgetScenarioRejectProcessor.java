package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.service.ErpFinConstants;
import app.erp.common.service.AbstractRejectProcessor;
import app.erp.fin.service.budget.ErpFinBudgetScenarioProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinBudgetScenario reject per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractRejectProcessor to activate the abstract base class; delegates to ErpFinBudgetScenarioProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinBudgetScenarioRejectProcessor extends AbstractRejectProcessor<ErpFinBudgetScenario> {

    @Inject
    ErpFinBudgetScenarioProcessor processor;

    @Override
    public ErpFinBudgetScenario reject(String id, IServiceContext context) {
        ErpFinBudgetScenario scenario = processor.requireScenario(id);
        processor.validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_REJECTED,
                ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_REJECTED);
        scenario.setApproveStatus(ErpFinConstants.BUDGET_STATUS_REJECTED);
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
    protected boolean isRejected(ErpFinBudgetScenario entity) {
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
    protected String rejectedStatus() {
        return null;
    }
}
