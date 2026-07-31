package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtSignatureRequest;
import app.erp.ct.service.spi.model.SignatureStatusQueryResponse;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

/**
 * ErpCtSignatureRequest queryAndUpdateStatus per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含电子签章轮询兜底编排（按 Provider.queryStatus 推进状态机，与 callback 共用迁移核心）。
 * 状态机核心辅助在 {@link AbstractErpCtSignatureRequestProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtSignatureRequestQueryAndUpdateStatusProcessor extends AbstractErpCtSignatureRequestProcessor {

    public ErpCtSignatureRequest queryAndUpdateStatus(Long requestId, IServiceContext context) {
        ErpCtSignatureRequest request = dao().getEntityById(requestId);
        if (request == null) {
            throw new NopException(app.erp.ct.service.ErpCtErrors.ERR_CT_SIGNATURE_ILLEGAL_TRANSITION)
                    .param(app.erp.ct.service.ErpCtErrors.ARG_SIGNATURE_REQUEST_ID, requestId);
        }
        if (isTerminal(request.getStatus())) {
            return request;
        }
        SignatureStatusQueryResponse resp = providerRegistry.getProvider(request.getProvider())
                .queryStatus(request.getProviderRequestId());

        String mappedStatus = mapProviderStatus(resp.getStatus());
        applyStatusTransition(request, mappedStatus, resp.getSignedSignerEmails(),
                resp.getErrorMsg(), context);
        dao().updateEntity(request);
        return request;
    }
}
