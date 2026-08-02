package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * ErpAstMaintenance decideTreatment per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含资本化/费用化裁决编排（含 capitalize 阈值门控）；共享 protected helper 单一真相源在
 * {@link ErpAstMaintenanceProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstMaintenanceDecideTreatmentProcessor {

    @Inject
    ErpAstMaintenanceProcessor facade;

    public ErpAstMaintenance decideTreatment(Long id, String treatment, BigDecimal capitalizedAmount,
                                               IServiceContext context) {
        ErpAstMaintenance m = facade.requireMaintenance(id, context);
        facade.validateTransition(m, ErpAstConstants.MAINTENANCE_STATUS_COMPLETED, "decideTreatment");
        if (!Objects.equals(treatment, ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE)
                && !Objects.equals(treatment, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE)) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_TREATMENT_NOT_DECIDED)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode())
                    .param(ErpAstErrors.ARG_TREATMENT, treatment);
        }
        BigDecimal totalCost = facade.aggregateCost(id);
        BigDecimal capAmount = capitalizedAmount != null ? capitalizedAmount : totalCost;

        if (Objects.equals(treatment, ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE)) {
            BigDecimal threshold = AppConfig.var(ErpAstConstants.CONFIG_MAINTENANCE_CAPITALIZE_THRESHOLD,
                    BigDecimal.ZERO);
            if (threshold.signum() > 0 && capAmount.compareTo(threshold) < 0) {
                throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_CAPITALIZE_BELOW_THRESHOLD)
                        .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode())
                        .param(ErpAstErrors.ARG_AMOUNT, capAmount)
                        .param(ErpAstErrors.ARG_THRESHOLD, threshold);
            }
        }

        m.setTreatment(treatment);
        m.setCapitalizedAmount(capAmount);
        m.setTotalCostAmount(totalCost);
        facade.maintenanceDao().updateEntity(m);
        return m;
    }
}
