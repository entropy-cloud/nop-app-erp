package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtSignatureRequest;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtErrors;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

import java.util.Map;
import java.util.Objects;

/**
 * ErpCtSignatureRequest handleSignatureCallback per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含电子签章 webhook 回调编排（HMAC 校验 + eventId 幂等 + 按 event 推进状态机）。
 * 状态机核心辅助在 {@link AbstractErpCtSignatureRequestProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtSignatureRequestHandleSignatureCallbackProcessor extends AbstractErpCtSignatureRequestProcessor {

    public ErpCtSignatureRequest handleSignatureCallback(String providerCode, String signature,
                                                         String eventId, String payload,
                                                         IServiceContext context) {
        // providerCode 注册校验（先校验，确保未注册也走 ErrorCode 而非 NPE）
        providerRegistry.getProvider(providerCode);

        boolean required = AppConfig.var(ErpCtConfigs.CFG_SIGNATURE_CALLBACK_SIGNATURE_REQUIRED,
                ErpCtConfigs.DEFAULT_SIGNATURE_CALLBACK_SIGNATURE_REQUIRED);
        if (required && !verifySignature(payload, signature, WEBHOOK_SECRET)) {
            throw new NopException(ErpCtErrors.ERR_CT_SIGNATURE_CALLBACK_SIGNATURE_INVALID)
                    .param(ErpCtErrors.ARG_PROVIDER_CODE, providerCode);
        }

        Map<String, Object> event = parsePayload(payload);
        String eventType = asString(event.get("eventType"));
        String providerRequestId = asString(event.get("providerRequestId"));
        String signerEmail = asString(event.get("signerEmail"));
        String reason = asString(event.get("reason"));

        ErpCtSignatureRequest request = findRequestByProviderRequestId(providerRequestId);
        if (request == null) {
            return null;
        }

        if (eventId != null && Objects.equals(eventId, request.getRemark())) {
            throw new NopException(ErpCtErrors.ERR_CT_SIGNATURE_CALLBACK_DUPLICATE_EVENT)
                    .param(ErpCtErrors.ARG_EVENT_ID, eventId)
                    .param(ErpCtErrors.ARG_PROVIDER_CODE, providerCode);
        }
        if (eventId != null) {
            request.setRemark(eventId);
        }

        applyEventTransition(request, eventType, signerEmail, reason, context);
        dao().updateEntity(request);
        return request;
    }
}
