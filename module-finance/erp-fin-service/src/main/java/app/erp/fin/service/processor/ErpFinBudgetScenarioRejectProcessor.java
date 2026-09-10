package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.common.service.AbstractRejectProcessor;
import app.erp.fin.service.budget.ErpFinBudgetScenarioProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinBudgetScenario）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
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

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION（模板参数 scenarioCode/currentDocStatus/expectedDocStatus）。
     */
    @Override
    protected NopException illegalStatusException(ErpFinBudgetScenario entity, String current, String... expected) {
        return new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION)
                .param(ErpFinErrors.ARG_SCENARIO_CODE, entity.getCode())
                .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
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
