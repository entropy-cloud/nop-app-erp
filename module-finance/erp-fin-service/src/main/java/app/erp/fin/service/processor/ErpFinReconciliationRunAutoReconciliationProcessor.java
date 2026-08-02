package app.erp.fin.service.processor;

import app.erp.fin.dao.dto.AutoReconResult;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.reconciliation.AutoReconciliationEngine;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * ErpFinReconciliation runAutoReconciliation per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含自动核销编排：按 partner 维度 matchAndBuild 候选行，复用 {@link ErpFinReconciliationCreateProcessor} +
 * {@link ErpFinReconciliationPostProcessor} 落核销单。共享 helper 单一真相源在
 * {@link AbstractErpFinReconciliationProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinReconciliationRunAutoReconciliationProcessor extends AbstractErpFinReconciliationProcessor {

    @Inject
    AutoReconciliationEngine autoReconciliationEngine;
    @Inject
    ErpFinReconciliationCreateProcessor createProcessor;
    @Inject
    ErpFinReconciliationPostProcessor postProcessor;

    public AutoReconResult runAutoReconciliation(String direction, Long partnerId, String strategy,
                                                 IServiceContext context) {
        if (!isAutoReconcileEnabled()) {
            throw new NopException(ErpFinErrors.ERR_AUTO_RECON_DISABLED);
        }
        IServiceContext ctx = context != null ? context : new ServiceContextImpl();
        String effectiveStrategy = resolveStrategy(strategy);
        LocalDate businessDate = CoreMetrics.today();

        AutoReconResult result = new AutoReconResult();
        List<Long> partnerIds = partnerId != null
                ? Collections.singletonList(partnerId)
                : autoReconciliationEngine.findPartnersWithOpenItems(direction, ctx);

        for (Long pid : partnerIds) {
            AutoReconciliationEngine.MatchResult match =
                    autoReconciliationEngine.matchAndBuild(direction, pid, effectiveStrategy, ctx);
            result.getUnmatched().addAll(match.getUnmatched());
            if (match.getLines().isEmpty()) {
                continue;
            }
            List<ReconciliationLineInput> matchLines = match.getLines();
            ErpFinReconciliation head = createProcessor.create(direction, pid, businessDate, matchLines, ctx);
            orm().flushSession();
            postProcessor.post(head.getId(), ctx);
            result.getReconciliationIds().add(head.getId());
        }
        return result;
    }

    protected boolean isAutoReconcileEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_AUTO_RECONCILE, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    protected String resolveStrategy(String strategy) {
        if (!StringHelper.isBlank(strategy)) {
            return strategy.toUpperCase();
        }
        String s = AppConfig.var(ErpFinConstants.CONFIG_AUTO_RECON_STRATEGY,
                ErpFinConstants.AUTO_RECON_STRATEGY_FIFO);
        return !StringHelper.isBlank(s) ? s.toUpperCase() : ErpFinConstants.AUTO_RECON_STRATEGY_FIFO;
    }
}
