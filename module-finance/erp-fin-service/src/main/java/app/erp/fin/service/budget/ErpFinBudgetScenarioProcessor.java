package app.erp.fin.service.budget;

import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

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

    public ErpFinBudgetScenario submit(Long id, IServiceContext context) {
        ErpFinBudgetScenario scenario = requireScenario(id);
        validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_SUBMITTED,
                ErpFinConstants.BUDGET_STATUS_DRAFT, ErpFinConstants.BUDGET_STATUS_REJECTED);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        scenario.setApproveStatus(ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        save(scenario);
        return scenario;
    }

    public ErpFinBudgetScenario approve(Long id, IServiceContext context) {
        ErpFinBudgetScenario scenario = requireScenario(id);
        validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_APPROVED,
                ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        generateBudgetVoucher(scenario, context);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        scenario.setApproveStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        save(scenario);
        return scenario;
    }

    public ErpFinBudgetScenario reject(Long id, IServiceContext context) {
        ErpFinBudgetScenario scenario = requireScenario(id);
        validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_REJECTED,
                ErpFinConstants.BUDGET_STATUS_SUBMITTED);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_REJECTED);
        scenario.setApproveStatus(ErpFinConstants.BUDGET_STATUS_REJECTED);
        save(scenario);
        return scenario;
    }

    public ErpFinBudgetScenario cancel(Long id, IServiceContext context) {
        ErpFinBudgetScenario scenario = requireScenario(id);
        validateTransition(scenario, ErpFinConstants.BUDGET_STATUS_CANCELLED,
                ErpFinConstants.BUDGET_STATUS_APPROVED);
        reverseBudgetVoucher(scenario, context);
        scenario.setDocStatus(ErpFinConstants.BUDGET_STATUS_CANCELLED);
        save(scenario);
        return scenario;
    }

    /** 审核通过时生成 BUDGET 影子凭证；首张凭证 ID 回写方案头供审计。 */
    protected void generateBudgetVoucher(ErpFinBudgetScenario scenario, IServiceContext context) {
        List<Long> voucherIds = budgetVoucherGenerator.generate(scenario);
        if (voucherIds.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_NO_LINES)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, scenario.getCode());
        }
        scenario.setVoucherId(voucherIds.get(0));
        LOG.info("预算方案 {} 审核通过，生成 {} 张 BUDGET 凭证：{}", scenario.getCode(), voucherIds.size(), voucherIds);
    }

    /** 作废时红冲全部 BUDGET 凭证。 */
    protected void reverseBudgetVoucher(ErpFinBudgetScenario scenario, IServiceContext context) {
        List<Long> reversalIds = budgetVoucherGenerator.reverse(scenario);
        LOG.info("预算方案 {} 作废，红冲 {} 张 BUDGET 凭证：{}", scenario.getCode(), reversalIds.size(), reversalIds);
    }

    protected void validateTransition(ErpFinBudgetScenario scenario, String target, String... allowedFrom) {
        String current = scenario.getDocStatus();
        boolean ok = false;
        for (String s : allowedFrom) {
            if (Objects.equals(s, current)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION)
                    .param(ErpFinErrors.ARG_SCENARIO_CODE, scenario.getCode())
                    .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                    .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, join(allowedFrom));
        }
    }

    protected ErpFinBudgetScenario requireScenario(Long id) {
        IEntityDao<ErpFinBudgetScenario> dao = daoProvider.daoFor(ErpFinBudgetScenario.class);
        ErpFinBudgetScenario scenario = dao.getEntityById(id);
        if (scenario == null) {
            throw new NopException(ErpFinErrors.ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION)
                    .param(ErpFinErrors.ARG_SCENARIO_ID, id);
        }
        return scenario;
    }

    protected void save(ErpFinBudgetScenario scenario) {
        daoProvider.daoFor(ErpFinBudgetScenario.class).updateEntity(scenario);
    }

    protected static String join(String[] arr) {
        return String.join("/", arr);
    }
}
