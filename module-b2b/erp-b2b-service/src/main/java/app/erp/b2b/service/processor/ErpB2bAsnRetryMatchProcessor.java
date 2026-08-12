package app.erp.b2b.service.processor;

import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.statemachine.ErpB2bAsnStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

/**
 * ErpB2bAsn retryMatch per-mutation Processor。
 * 自包含重试匹配编排：幂等短路（MATCHED/RECEIVED_TO_STOCK）→ 必要时回到 RECEIVED → 委托 {@link ErpB2bAsnMatchPurchaseOrderProcessor}。
 * （R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>幂等短路判定经 {@link ErpB2bAsnStateMachine#isIdempotentRetryStatus(String)} Bean（retryMatch 非矩阵迁移边，
 * 而是幂等重置 + 委托 match，Bean 提供 helper 供 Processor 短路）。回到 RECEIVED + 委托 match 的动态行为保留原位。
 */
public class ErpB2bAsnRetryMatchProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpB2bAsnMatchPurchaseOrderProcessor matchPurchaseOrderProcessor;

    @Inject
    ErpB2bAsnStateMachine stateMachine;

    public ErpB2bAsn retryMatch(Long asnId, IServiceContext context) {
        ErpB2bAsn asn = requireAsn(asnId);
        // 幂等：已 MATCHED/RECEIVED_TO_STOCK 直接返回（经 Bean helper 判定）
        if (stateMachine.isIdempotentRetryStatus(asn.getStatus())) {
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
