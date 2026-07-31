package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.entity.NcrLifecycleService;
import app.erp.qa.service.posting.NcrPostingDispatcher;
import app.erp.qa.service.posting.NcrReturnOrchestrator;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaNonConformance resolve per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 IN_REVIEW→RESOLVED 关闭编排（CAPA 闭环门控 + 财务过账分派：SCRAP 自动过账 / RETURN 编排退货）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaNonConformanceProcessor}。
 */
public class ErpQaNonConformanceResolveProcessor extends AbstractErpQaNonConformanceProcessor {

    @Inject
    NcrLifecycleService ncrLifecycleService;
    @Inject
    NcrPostingDispatcher ncrPostingDispatcher;
    @Inject
    NcrReturnOrchestrator ncrReturnOrchestrator;

    public ErpQaNonConformance resolve(Long ncrId,
                                       String resolution,
                                       String noCapaReason,
                                       IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        requireNcrStatus(ncr, ErpQaConstants.NCR_STATUS_IN_REVIEW, "IN_REVIEW");
        // CAPA 闭环门控：有措施须全 COMPLETED + 验证人/验证日期；无措施须显式提供 noCapaReason（误开/降级场景）
        ncrLifecycleService.requireResolveGate(ncrId, ncr.getCode(), noCapaReason);
        ncr.setStatus(ErpQaConstants.NCR_STATUS_RESOLVED);
        if (resolution != null) {
            ncr.setResolution(resolution);
        }
        if (StringHelper.isNotBlank(noCapaReason)) {
            ncr.setNoCapaReason(noCapaReason);
        }
        ncr.setResolvedAt(CoreMetrics.currentTimestamp());
        ncrDao().updateEntity(ncr);

        // 财务过账分派（plan 2026-07-05-2352-2）
        dispatchFinancialImpact(ncr, context);
        return ncr;
    }

    /**
     * resolve 时按 dispositionType + config-gated 分派财务处理。
     * SCRAP：AUTO_POST 自动过账 / MANUAL_POST 跳过（待人工 postNcr）。
     * RETURN：编排退货域创建退货单，NCR 侧登记 returnCode。
     * CONCESSION/DOWNGRADE/ESCALATED_TO_RECALL：无额外处理。
     */
    private void dispatchFinancialImpact(ErpQaNonConformance ncr, IServiceContext context) {
        String disposition = ncr.getDispositionType();
        if (disposition == null) {
            return;
        }
        if (isScrap(disposition)) {
            if (ErpQaConfigs.isNcrAutoPosting()) {
                ncrPostingDispatcher.dispatchScrap(ncr, context);
                ncrDao().updateEntity(ncr);
            }
        } else if (isReturn(disposition)) {
            ncrReturnOrchestrator.orchestrateReturn(ncr, context);
            ncrDao().updateEntity(ncr);
        }
    }
}
