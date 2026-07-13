
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.biz.IErpCtSignatureRequestBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.spi.ErpCtSignatureProviderRegistry;
import app.erp.ct.service.spi.IErpCtSignatureProvider;
import app.erp.ct.service.spi.model.SignatureInitRequest;
import app.erp.ct.service.spi.model.SignatureInitResponse;
import app.erp.ct.service.spi.model.SignatureStatusQueryResponse;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.contract.dao.entity.ErpCtSignatureRequest;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import jakarta.inject.Inject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 电子签章请求 BizModel（plan 2026-07-04-2200-2，design e-signature.md）。
 *
 * <p>签章生命周期：
 * <ul>
 *   <li>{@link #initSignatureRequest}：FINALIZED 守门→建 PENDING_SIGNATURE 请求→调 Provider.initSignature
 *       回填 providerRequestId。config-gated {@code erp-ct.e-signature-enabled}（关则抛错，本期仅支持启用路径，
 *       线下签署附件上传走 ErpCtContractVersion.signVersion 直接接入）。</li>
 *   <li>{@link #handleSignatureCallback}：webhook 入口（@BizMutation 镜像 logistics handleTrackingWebhook 范式）。
 *       HMAC 校验 + eventId 幂等 + 按 event 推进状态机（signer.signed→PARTIALLY / signing.completed→FULLY
 *       +retrieveCertificate+调 signVersion / signing.rejected·declined→REJECTED / signing.expired→EXPIRED）。</li>
 *   <li>{@link #queryAndUpdateStatus}：轮询兜底，按 Provider.queryStatus 推进（与 callback 共用
 *       {@link #applyStatusTransition} 迁移核心）。</li>
 *   <li>{@link #cancelSignatureRequest}/{@link #rejectSignature}：终态迁移。</li>
 *   <li>{@link #findExpiringRequests}：到期查询（cron 注册归 Non-Goal）。</li>
 * </ul>
 *
 * <p>状态机（dict erp-ct/sign-status，design e-signature.md §状态机）：
 * PENDING_SIGNATURE→PARTIALLY_SIGNED（首签）→FULLY_SIGNED（全部）/→REJECTED/→EXPIRED/→CANCELLED。
 * 非法迁移抛 {@link ErpCtErrors#ERR_CT_SIGNATURE_ILLEGAL_TRANSITION}。
 *
 * <p>核心零污染：SignatureRequest→ContractVersion 单向 contractVersionId 引用（弱指针），不在
 * ErpCtContractVersion 加 signatureRequestId 列。FULLY_SIGNED 完成时调既有
 * {@link IErpCtContractVersionBiz#signVersion}（FINALIZED→SIGNED + isCurrent 翻转）。
 */
@BizModel("ErpCtSignatureRequest")
public class ErpCtSignatureRequestBizModel extends CrudBizModel<ErpCtSignatureRequest>
        implements IErpCtSignatureRequestBiz {

    /** webhook HMAC 密钥（mock 测试可控；真实部署可扩展凭证配置）。 */
    public static final String WEBHOOK_SECRET = "erp-ct-signature-callback-secret";

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

    @Inject
    ErpCtSignatureProviderRegistry providerRegistry;

    public ErpCtSignatureRequestBizModel() {
        setEntityName(ErpCtSignatureRequest.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpCtSignatureRequest initSignatureRequest(@Name("contractVersionId") Long contractVersionId,
                                                      @Name("signers") String signersJson,
                                                      @Name("providerCode") String providerCode,
                                                      IServiceContext context) {
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
        IErpCtSignatureProvider provider = providerRegistry.getProvider(effectiveProvider);

        ErpCtSignatureRequest request = newEntity();
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
            SignatureInitResponse initResp = provider.initSignature(initReq);
            request.setProviderRequestId(initResp.getProviderRequestId());
            saveEntity(request, null, context);
            return request;
        } catch (NopException e) {
            throw e;
        } catch (Exception e) {
            throw new NopException(ErpCtErrors.ERR_CT_SIGNATURE_INIT_FAILED, e)
                    .param(ErpCtErrors.ARG_PROVIDER_CODE, effectiveProvider)
                    .param("errorMsg", e.getMessage());
        }
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpCtSignatureRequest handleSignatureCallback(@Name("providerCode") String providerCode,
                                                         @Name("signature") String signature,
                                                         @Name("eventId") String eventId,
                                                         @Name("payload") String payload,
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

        ErpCtSignatureRequest request = findRequestByProviderRequestId(providerRequestId, context);
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
        updateEntity(request, null, context);
        return request;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpCtSignatureRequest queryAndUpdateStatus(@Name("requestId") Long requestId,
                                                      IServiceContext context) {
        ErpCtSignatureRequest request = requireEntity(String.valueOf(requestId), null, context);
        if (isTerminal(request.getStatus())) {
            return request;
        }
        IErpCtSignatureProvider provider = providerRegistry.getProvider(request.getProvider());
        SignatureStatusQueryResponse resp = provider.queryStatus(request.getProviderRequestId());

        String mappedStatus = mapProviderStatus(resp.getStatus());
        applyStatusTransition(request, mappedStatus, resp.getSignedSignerEmails(),
                resp.getErrorMsg(), context);
        updateEntity(request, null, context);
        return request;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpCtSignatureRequest cancelSignatureRequest(@Name("requestId") Long requestId,
                                                        IServiceContext context) {
        ErpCtSignatureRequest request = requireEntity(String.valueOf(requestId), null, context);
        String status = request.getStatus();
        if (!ErpCtConstants.SIGNATURE_STATUS_PENDING.equals(status)
                && !ErpCtConstants.SIGNATURE_STATUS_PARTIALLY.equals(status)) {
            throw illegalTransition(request, ErpCtConstants.SIGNATURE_STATUS_PENDING
                    + "/" + ErpCtConstants.SIGNATURE_STATUS_PARTIALLY);
        }
        request.setStatus(ErpCtConstants.SIGNATURE_STATUS_CANCELLED);
        updateEntity(request, null, context);
        return request;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpCtSignatureRequest rejectSignature(@Name("requestId") Long requestId,
                                                 @Name("reason") String reason,
                                                 IServiceContext context) {
        ErpCtSignatureRequest request = requireEntity(String.valueOf(requestId), null, context);
        String status = request.getStatus();
        if (isTerminal(status)) {
            throw illegalTransition(request, "non-terminal");
        }
        request.setStatus(ErpCtConstants.SIGNATURE_STATUS_REJECTED);
        request.setErrorMsg(reason);
        updateEntity(request, null, context);
        return request;
    }

    @Override
    @BizQuery
    public List<ErpCtSignatureRequest> findExpiringRequests(@Name("asOfDate") LocalDate asOfDate,
                                                            IServiceContext context) {
        // 平台对 signingDeadline 仅允许 eq/in/dateBetween/dateTimeBetween（不支持 lt），
        // 用 [epoch, asOfDate] 的 dateBetween 取所有早于/等于 asOfDate 的请求，再内存过滤非终态。
        QueryBean query = new QueryBean();
        query.addFilter(io.nop.api.core.beans.FilterBeans.dateBetween(
                "signingDeadline", java.time.LocalDate.of(1970, 1, 1), asOfDate));
        List<ErpCtSignatureRequest> all = findList(query, null, context);
        if (all == null) {
            return new ArrayList<>();
        }
        List<ErpCtSignatureRequest> result = new ArrayList<>();
        for (ErpCtSignatureRequest r : all) {
            if (!isTerminal(r.getStatus())) {
                result.add(r);
            }
        }
        return result;
    }

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
        request.setCompletedAt(CoreMetrics.currentDateTime());
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

    // ---------- helpers ----------

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
    protected List<app.erp.ct.service.spi.model.Signer> parseSignersFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        Object parsed = JsonTool.parseNonStrict(json);
        if (!(parsed instanceof List)) {
            return Collections.emptyList();
        }
        List<app.erp.ct.service.spi.model.Signer> result = new ArrayList<>();
        for (Object o : (List<Object>) parsed) {
            if (o instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) o;
                app.erp.ct.service.spi.model.Signer s = new app.erp.ct.service.spi.model.Signer();
                s.setName(asString(m.get("name")));
                s.setEmail(asString(m.get("email")));
                s.setPhone(asString(m.get("phone")));
                result.add(s);
            }
        }
        return result;
    }

    protected ErpCtSignatureRequest findRequestByProviderRequestId(String providerRequestId,
                                                                   IServiceContext context) {
        if (providerRequestId == null) {
            return null;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("providerRequestId", providerRequestId));
        return findFirst(query, null, context);
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

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCtSignatureRequest.class)
    public List<String> orgName(@ContextSource List<ErpCtSignatureRequest> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtSignatureRequest row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtSignatureRequest.class)
    public List<String> providerRequestName(@ContextSource List<ErpCtSignatureRequest> rows) {
        orm().batchLoadProps(rows, Collections.singleton("providerRequest"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtSignatureRequest row : rows) {
            result.add(row.orm_attached() && row.getProviderRequest() != null ? row.getProviderRequest().getProviderRequestId() : null);
        }
        return result;
    }

}
