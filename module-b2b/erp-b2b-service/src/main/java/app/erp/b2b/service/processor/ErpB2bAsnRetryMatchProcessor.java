package app.erp.b2b.service.processor;

import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

/**
 * ErpB2bAsn retryMatch per-mutation Processor。
 * 自包含重试匹配编排：幂等短路（MATCHED/RECEIVED_TO_STOCK）→ 必要时回到 RECEIVED → 委托 {@link ErpB2bAsnMatchPurchaseOrderProcessor}。
 * （R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpB2bAsnRetryMatchProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpB2bAsnMatchPurchaseOrderProcessor matchPurchaseOrderProcessor;

    public ErpB2bAsn retryMatch(Long asnId, IServiceContext context) {
        ErpB2bAsn asn = requireAsn(asnId);
        // 幂等：已 MATCHED/RECEIVED_TO_STOCK 直接返回
        if (ErpB2bConstants.ASN_STATUS_MATCHED.equals(asn.getStatus())
                || ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK.equals(asn.getStatus())) {
            return asn;
        }
        // 回到 RECEIVED 后重新匹配
        if (!ErpB2bConstants.ASN_STATUS_RECEIVED.equals(asn.getStatus())) {
            asn.setStatus(ErpB2bConstants.ASN_STATUS_RECEIVED);
            daoProvider.daoFor(ErpB2bAsn.class).saveOrUpdateEntity(asn);
        }
        return matchPurchaseOrderProcessor.matchPurchaseOrder(asnId, context);
    }

    // ---------- 内部辅助 ----------

    protected ErpB2bAsn requireAsn(Long asnId) {
        ErpB2bAsn asn = daoProvider.daoFor(ErpB2bAsn.class).getEntityById(asnId);
        if (asn == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_ASN_ILLEGAL_TRANSITION)
                    .param(ErpB2bErrors.ARG_ASN_ID, asnId);
        }
        return asn;
    }
}
