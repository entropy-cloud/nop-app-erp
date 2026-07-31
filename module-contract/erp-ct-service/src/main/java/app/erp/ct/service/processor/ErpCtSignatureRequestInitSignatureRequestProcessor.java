package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.contract.dao.entity.ErpCtSignatureRequest;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.spi.model.SignatureInitRequest;
import app.erp.ct.service.spi.model.SignatureInitResponse;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

import java.util.Objects;

/**
 * ErpCtSignatureRequest initSignatureRequest per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含电子签章请求初始化编排（FINALIZED 守门→建 PENDING_SIGNATURE 请求→调 Provider.initSignature 回填 providerRequestId）。
 * 状态机核心辅助在 {@link AbstractErpCtSignatureRequestProcessor}。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtSignatureRequestInitSignatureRequestProcessor extends AbstractErpCtSignatureRequestProcessor {

    public ErpCtSignatureRequest initSignatureRequest(Long contractVersionId, String signersJson,
                                                      String providerCode, IServiceContext context) {
        boolean enabled = AppConfig.var(ErpCtConfigs.CFG_E_SIGNATURE_ENABLED, false);
        if (!enabled) {
            throw new NopException(ErpCtErrors.ERR_CT_SIGNATURE_INIT_FAILED)
                    .param(ErpCtErrors.ARG_PROVIDER_CODE, providerCode)
                    .param("errorMsg", "erp-ct.e-signature-enabled=false，未启用电子签章（走线下签署）");
        }

        ErpCtContractVersion version = contractVersionBiz.get(String.valueOf(contractVersionId), false, context);
        if (version == null || !Objects.equals(version.getStatus(), ErpCtConstants.VERSION_STATUS_FINALIZED)) {
            throw new NopException(ErpCtErrors.ERR_CT_SIGNATURE_VERSION_NOT_FINALIZED)
                    .param(ErpCtErrors.ARG_VERSION_ID, contractVersionId);
        }

        String effectiveProvider = providerCode != null ? providerCode
                : AppConfig.var(ErpCtConfigs.CFG_SIGNATURE_DEFAULT_PROVIDER,
                        ErpCtConfigs.DEFAULT_SIGNATURE_DEFAULT_PROVIDER);

        ErpCtSignatureRequest request = dao().newEntity();
        request.setContractVersionId(contractVersionId);
        request.setProvider(effectiveProvider);
        request.setStatus(ErpCtConstants.SIGNATURE_STATUS_PENDING);
        request.setSigners(signersJson != null ? signersJson : "[]");
        request.setSigningDeadline(resolveDefaultDeadline());

        SignatureInitRequest initReq = new SignatureInitRequest();
        initReq.setContractVersionId(contractVersionId);
        initReq.setSigners(parseSignersFromJson(signersJson));
        initReq.setSigningOrder(ErpCtConstants.SIGNING_ORDER_SEQUENTIAL);
        try {
            SignatureInitResponse initResp = providerRegistry.getProvider(effectiveProvider).initSignature(initReq);
            request.setProviderRequestId(initResp.getProviderRequestId());
            dao().saveEntity(request);
            return request;
        } catch (NopException e) {
            throw e;
        } catch (Exception e) {
            throw new NopException(ErpCtErrors.ERR_CT_SIGNATURE_INIT_FAILED, e)
                    .param(ErpCtErrors.ARG_PROVIDER_CODE, effectiveProvider)
                    .param("errorMsg", e.getMessage());
        }
    }
}
