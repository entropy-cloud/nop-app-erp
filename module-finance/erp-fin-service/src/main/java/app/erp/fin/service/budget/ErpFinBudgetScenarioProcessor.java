package app.erp.fin.service.budget;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.processor.ErpFinBudgetScenarioApproveProcessor;
import app.erp.fin.service.processor.ErpFinBudgetScenarioCancelProcessor;
import app.erp.fin.service.processor.ErpFinBudgetScenarioRejectProcessor;
import app.erp.fin.service.processor.ErpFinBudgetScenarioSubmitForApprovalProcessor;
import app.erp.fin.service.statemachine.ErpFinBudgetScenarioDocumentStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 预算方案编排 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 * Facade {@code ErpFinBudgetScenarioBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>状态机（{@code budget.md §ErpFinBudgetScenario}）：
 * <pre>
 *   DRAFT → SUBMITTED → APPROVED（生成 postingType=BUDGET 影子凭证）
 *   REJECTED → DRAFT（修改重提）
 *   SUBMITTED → REJECTED
 *   APPROVED → CANCELLED（红冲原 BUDGET 凭证）
 * </pre>
 *
 * <p>配置余地：状态机迁移（{@link #validateTransition}）、凭证生成（{@link #generateBudgetVoucher}）、
 * 凭证红冲（{@link #reverseBudgetVoucher}）均为 {@code protected} 方法、以 {@link IServiceContext} 为末参，
 * 下游可逐 step 覆盖。
 */
public class ErpFinBudgetScenarioProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinBudgetScenarioProcessor.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    BudgetVoucherGenerator budgetVoucherGenerator;

    @Inject
    ErpFinBudgetScenarioSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpFinBudgetScenarioApproveProcessor approveProcessor;

    @Inject
    ErpFinBudgetScenarioRejectProcessor rejectProcessor;

    @Inject
    ErpFinBudgetScenarioCancelProcessor cancelProcessor;

    @Inject
    ErpFinBudgetScenarioDocumentStateMachine documentStateMachine;

    public ErpFinBudgetScenario submit(Long id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(String.valueOf(id), context);
    }

    public ErpFinBudgetScenario approve(Long id, IServiceContext context) {
        return approveProcessor.approve(String.valueOf(id), context);
    }

    public ErpFinBudgetScenario reject(Long id, IServiceContext context) {
        return rejectProcessor.reject(String.valueOf(id), context);
    }

    public ErpFinBudgetScenario cancel(Long id, IServiceContext context) {
        return cancelProcessor.cancel(String.valueOf(id), context);
    }

    // ==================== A2 滚动预算自动复制 + 结转规则引擎（plan 2026-07-21-1206-2） ====================
    //
    // rollForward / carryForward 编排已提取为独立 per-mutation Processor（R6.9，processor-extension-pattern.md 每 mutation 一 Processor）：
    //   - ErpFinBudgetScenarioRollForwardProcessor
    //   - ErpFinBudgetScenarioCarryForwardProcessor
    // 本 facade 按方案 A（facade-as-helper-holder）保留二者共享的 helper：requireScenario / save /
    //   loadBudgetLines / resolveUserId（被 S-mutation 子类 + 新 per-mutation Processor 共享）。

    /** 审核通过时生成 BUDGET 影子凭证；首张凭证 ID 回写方案头供审计。 */
    public void generateBudgetVoucher(ErpFinBudgetScenario scenario, IServiceContext context) {
        List<Long> voucherIds = budgetVoucherGenerator.generate(scenario);
        if (voucherIds.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NO_LINES)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, scenario.getCode());
        }
        scenario.setVoucherId(voucherIds.get(0));
        LOG.info("预算方案 {} 审核通过，生成 {} 张 BUDGET 凭证：{}", scenario.getCode(), voucherIds.size(), voucherIds);
    }

    /** 作废时红冲全部 BUDGET 凭证。 */
    public void reverseBudgetVoucher(ErpFinBudgetScenario scenario, IServiceContext context) {
        List<Long> reversalIds = budgetVoucherGenerator.reverse(scenario);
        LOG.info("预算方案 {} 作废，红冲 {} 张 BUDGET 凭证：{}", scenario.getCode(), reversalIds.size(), reversalIds);
    }

    public void validateTransition(ErpFinBudgetScenario scenario, String target, String... allowedFrom) {
        String current = scenario.getDocStatus();
        try {
            if (ErpFinConstants.BUDGET_STATUS_SUBMITTED.equals(target)) {
                documentStateMachine.assertCanSubmit(current);
            } else if (ErpFinConstants.BUDGET_STATUS_APPROVED.equals(target)) {
                documentStateMachine.assertCanApprove(current);
            } else if (ErpFinConstants.BUDGET_STATUS_REJECTED.equals(target)) {
                documentStateMachine.assertCanReject(current);
            } else if (ErpFinConstants.BUDGET_STATUS_CANCELLED.equals(target)) {
                documentStateMachine.assertCanCancel(current);
            } else {
                throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION)
                        .param(ErpFinErrors.ARG_SCENARIO_CODE, scenario.getCode())
                        .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                        .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, target);
            }
        } catch (NopException e) {
            if (ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode().equals(e.getErrorCode())) {
                throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION, e)
                        .param(ErpFinErrors.ARG_SCENARIO_CODE, scenario.getCode())
                        .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                        .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS,
                                e.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS));
            }
            throw e;
        }
    }

    public ErpFinBudgetScenario requireScenario(Long id) {
        IEntityDao<ErpFinBudgetScenario> dao = daoProvider.daoFor(ErpFinBudgetScenario.class);
        ErpFinBudgetScenario scenario = dao.getEntityById(id);
        if (scenario == null) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION)
                    .param(ErpFinErrors.ARG_SCENARIO_ID, id);
        }
        return scenario;
    }

    public void save(ErpFinBudgetScenario scenario) {
        daoProvider.daoFor(ErpFinBudgetScenario.class).updateEntity(scenario);
    }

    /** 加载方案的预算行（按 scenarioId 查询，返回所有未逻辑删除的行）。 */
    public List<ErpFinBudgetLine> loadBudgetLines(Long scenarioId) {
        IEntityDao<ErpFinBudgetLine> dao = daoProvider.daoFor(ErpFinBudgetLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("scenarioId", scenarioId));
        return dao.findAllByQuery(q);
    }

    public String resolveUserId(IServiceContext context) {
        try {
            if (context != null && context.getUserContext() != null) {
                return context.getUserContext().getUserId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
