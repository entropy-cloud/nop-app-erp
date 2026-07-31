package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.contract.dao.entity.ErpCtSignatureRequest;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.spi.ErpCtSignatureProviderRegistry;
import app.erp.ct.service.spi.IErpCtSignatureProvider;
import app.erp.ct.service.spi.model.SignatureInitRequest;
import app.erp.ct.service.spi.model.SignatureInitResponse;
import app.erp.ct.service.spi.model.SignatureStatusQueryResponse;
import app.erp.ct.service.spi.model.Signer;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 电子签章请求状态机核心基类（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>3 个 D-mutation（{@code initSignatureRequest}/{@code handleSignatureCallback}/{@code queryAndUpdateStatus}）
 * 共享状态机迁移核心（{@link #applyEventTransition}/{@link #applyStatusTransition}/{@link #completeFullySigned} 等）
 * 及校验/解析辅助，按 helper 归属裁决（同实体多 mutation 共享 helper 显著时抽域专属基类）集中本类。
 * 下游可经 Delta beans.xml 覆盖同名 bean id 的子类逐个覆盖 protected step。
 */
public abstract class AbstractErpCtSignatureRequestProcessor {

    /** webhook HMAC 密钥（mock 测试可控；真实部署可扩展凭证配置）。 */
    public static final String WEBHOOK_SECRET = "erp-ct-signature-callback-secret";

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

    @Inject
    ErpCtSignatureProviderRegistry providerRegistry;

    // ---------- state machine core ----------

    /**
     * webhook event 推进状态机。事件→目标状态映射对应 design e-signature.md §Webhook 回调表。
     * declined 折叠为 REJECTED（design webhook 表列 DECLINED 但状态机/字典无此态，按权威 6 态收敛）。
     */
    protected void applyEventTransition(ErpCtSignatureRequest request, String eventType,
                                        String signerEmail, String reason, IServiceContext context) {
        if (ErpCtConstants.SIGNATURE_EVENT_SIGNER_SIGNED.equals(eventType)) {
            transitionTo(request, ErpCtConstants.SIGNATURE_STATUS_PARTIALLY);
            markSignerSigned(request, signerEmail);
        } else if (ErpCtConstants.SIGNATURE_EVENT_COMPLETED.equals(eventType)) {
            completeFullySigned(request, context);
        } else if (ErpCtConstants.SIGNATURE_EVENT_REJECTED.equals(eventType)
                || ErpCtConstants.SIGNATURE_EVENT_DECLINED.equals(eventType)) {
            transitionTo(request, ErpCtConstants.SIGNATURE_STATUS_REJECTED);
            if (reason != null) {
                request.setErrorMsg(reason);
            }
        } else if (ErpCtConstants.SIGNATURE_EVENT_EXPIRED.equals(eventType)) {
            transitionTo(request, ErpCtConstants.SIGNATURE_STATUS_EXPIRED);
        }
        // signing.started 等非状态推进事件忽略
    }

    /**
     * 主动轮询推进状态机。与 callback 共用迁移语义（provider 中立 status → dict status）。
     */
    protected void applyStatusTransition(ErpCtSignatureRequest request, String targetStatus,
                                         List<String> signedSignerEmails, String errorMsg,
                                         IServiceContext context) {
        if (targetStatus == null) {
            return;
        }
        switch (targetStatus) {
            case ErpCtConstants.SIGNATURE_STATUS_PARTIALLY:
                transitionTo(request, ErpCtConstants.SIGNATURE_STATUS_PARTIALLY);
                if (signedSignerEmails != null) {
                    for (String email : signedSignerEmails) {
                        markSignerSigned(request, email);
                    }
                }
                break;
            case ErpCtConstants.SIGNATURE_STATUS_FULLY:
                completeFullySigned(request, context);
                break;
            case ErpCtConstants.SIGNATURE_STATUS_REJECTED:
                transitionTo(request, ErpCtConstants.SIGNATURE_STATUS_REJECTED);
                if (errorMsg != null) {
                    request.setErrorMsg(errorMsg);
                }
                break;
            case ErpCtConstants.SIGNATURE_STATUS_EXPIRED:
                transitionTo(request, ErpCtConstants.SIGNATURE_STATUS_EXPIRED);
                break;
            default:
                break;
        }
    }

    /**
     * FULLY_SIGNED 完成集成（callback completed 与 query 共用）：
     * <ol>
     *   <li>幂等守门：已 FULLY_SIGNED 抛 {@link ErpCtErrors#ERR_CT_SIGNATURE_ALREADY_COMPLETED}。</li>
     *   <li>retrieveCertificate 下载已签文档 → 存附件 fileId + certificateUrl/evidenceNo/completedAt。</li>
     *   <li>调既有 {@link IErpCtContractVersionBiz#signVersion}（FINALIZED→SIGNED + isCurrent 翻转）。</li>
     * </ol>
     */
    protected void completeFullySigned(ErpCtSignatureRequest request, IServiceContext context) {
        if (ErpCtConstants.SIGNATURE_STATUS_FULLY.equals(request.getStatus())) {
            throw new NopException(ErpCtErrors.ERR_CT_SIGNATURE_ALREADY_COMPLETED)
                    .param(ErpCtErrors.ARG_SIGNATURE_REQUEST_ID, request.getId());
        }
        if (isTerminal(request.getStatus())) {
            throw illegalTransition(request, "non-terminal");
        }

        IErpCtSignatureProvider provider = providerRegistry.getProvider(request.getProvider());
        byte[] certificate = provider.retrieveCertificate(request.getProviderRequestId());

        request.setStatus(ErpCtConstants.SIGNATURE_STATUS_FULLY);
        request.setCompletedAt(CoreMetrics.currentTimestamp());
        if (certificate != null) {
            String fileId = storeCertificateArtifact(request, certificate);
            request.setAttachmentFileId(fileId);
        }
        request.setCertificateUrl("https://mock.sign/cert/" + request.getProviderRequestId());
        request.setEvidenceNo("EVID-" + request.getProviderRequestId());

        // 调既有 signVersion（FINALIZED→SIGNED + isCurrent 翻转）；事务由 @BizMutation 包装。
        contractVersionBiz.signVersion(request.getContractVersionId(), context);
    }

    protected void transitionTo(ErpCtSignatureRequest request, String target) {
        String current = request.getStatus();
        if (Objects.equals(current, target)) {
            return;
        }
        if (!isValidTransition(current, target)) {
            throw illegalTransition(request, target);
        }
        request.setStatus(target);
    }

    protected boolean isValidTransition(String from, String to) {
        if (ErpCtConstants.SIGNATURE_STATUS_PENDING.equals(from)) {
            return ErpCtConstants.SIGNATURE_STATUS_PARTIALLY.equals(to)
                    || ErpCtConstants.SIGNATURE_STATUS_FULLY.equals(to)
                    || ErpCtConstants.SIGNATURE_STATUS_REJECTED.equals(to)
                    || ErpCtConstants.SIGNATURE_STATUS_EXPIRED.equals(to)
                    || ErpCtConstants.SIGNATURE_STATUS_CANCELLED.equals(to);
        }
        if (ErpCtConstants.SIGNATURE_STATUS_PARTIALLY.equals(from)) {
            return ErpCtConstants.SIGNATURE_STATUS_FULLY.equals(to)
                    || ErpCtConstants.SIGNATURE_STATUS_REJECTED.equals(to)
                    || ErpCtConstants.SIGNATURE_STATUS_EXPIRED.equals(to)
                    || ErpCtConstants.SIGNATURE_STATUS_CANCELLED.equals(to);
        }
        return false;
    }

    protected boolean isTerminal(String status) {
        return ErpCtConstants.SIGNATURE_STATUS_FULLY.equals(status)
                || ErpCtConstants.SIGNATURE_STATUS_REJECTED.equals(status)
                || ErpCtConstants.SIGNATURE_STATUS_EXPIRED.equals(status)
                || ErpCtConstants.SIGNATURE_STATUS_CANCELLED.equals(status);
    }

    protected NopException illegalTransition(ErpCtSignatureRequest request, String expected) {
        return new NopException(ErpCtErrors.ERR_CT_SIGNATURE_ILLEGAL_TRANSITION)
                .param(ErpCtErrors.ARG_SIGNATURE_REQUEST_ID, request.getId())
                .param(ErpCtErrors.ARG_CURRENT_STATUS, request.getStatus())
                .param(ErpCtErrors.ARG_EXPECTED_STATUS, expected);
    }

    // ---------- 校验/解析辅助 ----------

    /** Provider 中立状态码 → dict status（PENDING/PARTIALLY/COMPLETED/REJECTED/EXPIRED）。 */
    protected String mapProviderStatus(String providerStatus) {
        if (providerStatus == null) {
            return null;
        }
        switch (providerStatus) {
            case "PENDING":
                return ErpCtConstants.SIGNATURE_STATUS_PENDING;
            case "PARTIALLY":
                return ErpCtConstants.SIGNATURE_STATUS_PARTIALLY;
            case "COMPLETED":
                return ErpCtConstants.SIGNATURE_STATUS_FULLY;
            case "REJECTED":
            case "DECLINED":
                return ErpCtConstants.SIGNATURE_STATUS_REJECTED;
            case "EXPIRED":
                return ErpCtConstants.SIGNATURE_STATUS_EXPIRED;
            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected void markSignerSigned(ErpCtSignatureRequest request, String signerEmail) {
        if (signerEmail == null) {
            return;
        }
        List<Object> signers = parseSignersList(request.getSigners());
        for (Object o : signers) {
            if (o instanceof Map) {
                Map<String, Object> s = (Map<String, Object>) o;
                if (signerEmail.equals(s.get("email"))) {
                    s.put("signedAt", CoreMetrics.currentTimeMillis());
                }
            }
        }
        request.setSigners(JsonTool.serialize(signers, false));
    }

    @SuppressWarnings("unchecked")
    protected List<Object> parseSignersList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        Object parsed = JsonTool.parseNonStrict(json);
        if (parsed instanceof List) {
            return new ArrayList<>((List<Object>) parsed);
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    protected List<Signer> parseSignersFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        Object parsed = JsonTool.parseNonStrict(json);
        if (!(parsed instanceof List)) {
            return Collections.emptyList();
        }
        List<Signer> result = new ArrayList<>();
        for (Object o : (List<Object>) parsed) {
            if (o instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) o;
                Signer s = new Signer();
                s.setName(asString(m.get("name")));
                s.setEmail(asString(m.get("email")));
                s.setPhone(asString(m.get("phone")));
                result.add(s);
            }
        }
        return result;
    }

    protected ErpCtSignatureRequest findRequestByProviderRequestId(String providerRequestId) {
        if (providerRequestId == null) {
            return null;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("providerRequestId", providerRequestId));
        return dao().findFirstByQuery(query);
    }

    protected LocalDate resolveDefaultDeadline() {
        int days = AppConfig.var(ErpCtConfigs.CFG_SIGNATURE_DEADLINE_DEFAULT_DAYS,
                ErpCtConfigs.DEFAULT_SIGNATURE_DEADLINE_DEFAULT_DAYS);
        return CoreMetrics.today().plusDays(days);
    }

    /**
     * 占位证书附件存储。返回 fileId（mock 使用 in-memory 字符串占位；真实部署经 {@code stdDomain="file"}
     * 的 OrmFileComponent 落文件系统/OSS）。本期写回 attachmentFileId 仅用于行为校验。
     */
    protected String storeCertificateArtifact(ErpCtSignatureRequest request, byte[] certificate) {
        return "CERT-" + request.getProviderRequestId();
    }

    protected boolean verifySignature(String payload, String signature, String secret) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            String expected = sb.toString();
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parsePayload(String payload) {
        return (Map<String, Object>) JsonTool.parseNonStrict(payload);
    }

    protected String asString(Object value) {
        return value == null ? null : value.toString();
    }

    protected IEntityDao<ErpCtSignatureRequest> dao() {
        return daoProvider.daoFor(ErpCtSignatureRequest.class);
    }
}
