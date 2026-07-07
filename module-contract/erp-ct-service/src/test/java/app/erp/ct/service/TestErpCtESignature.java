package app.erp.ct.service;

import app.erp.ct.biz.IErpCtSignatureRequestBiz;
import app.erp.ct.service.entity.ErpCtSignatureRequestBizModel;
import app.erp.ct.service.spi.ErpCtSignatureProviderRegistry;
import app.erp.ct.service.spi.mock.MockSignatureProvider;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.contract.dao.entity.ErpCtSignatureRequest;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 电子签章全链行为测试（plan 2026-07-04-2200-2 Phase 3）。
 *
 * <p>mock Provider（{@link MockSignatureProvider}）覆盖：
 * <ul>
 *   <li>initSignatureRequest：FINALIZED 守门（非 FINALIZED 抛错）+ 建请求 PENDING_SIGNATURE + 回填 providerRequestId；</li>
 *   <li>handleSignatureCallback：HMAC 签名校验（通过/失败）+ eventId 幂等 + 状态推进
 *       （signer.signed→PARTIALLY / signing.completed→FULLY+retrieveCertificate+signVersion /
 *        signing.rejected·declined→REJECTED / signing.expired→EXPIRED）；</li>
 *   <li>queryAndUpdateStatus：轮询推进（mock 计数器 PARTIALLY→COMPLETED）；</li>
 *   <li>重复完成幂等（ERR_CT_SIGNATURE_ALREADY_COMPLETED）；</li>
 *   <li>cancel/reject + 非法迁移 ErrorCode；</li>
 *   <li>Registry 未注册抛错；findExpiringRequests 查询。</li>
 * </ul>
 *
 * <p>沿用 {@link TestErpCtContractRebate} 的 JunitAutoTestCase + @NopTestConfig schema bootstrap 样板。
 * ContractVersion 经 DAO 直接构造（绕过合同头审核管道），保证 isCurrent=true + FINALIZED 满足 signVersion 前置。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtESignature extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpCtSignatureRequestBiz signatureRequestBiz;
    @Inject
    ErpCtSignatureProviderRegistry providerRegistry;
    @Inject
    MockSignatureProvider mockSignatureProvider;

    @BeforeEach
    void enableESignature() {
        // 启用电子签章 + 重置 mock 共享状态
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_E_SIGNATURE_ENABLED, "true");
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCtConfigs.CFG_SIGNATURE_DEFAULT_PROVIDER, ErpCtConstants.SIGNATURE_PROVIDER_MOCK);
        mockSignatureProvider.resetTestState();
    }

    // ============ initSignatureRequest ============

    @Test
    public void testInitRejectsNonFinalizedVersion() {
        long contractId = seedContract();
        long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_DRAFT);

        NopException ex = assertThrows(NopException.class,
                () -> signatureRequestBiz.initSignatureRequest(versionId,
                        "[{\"name\":\"张三\",\"email\":\"zhang@ex.com\"}]", null, CTX));
        assertEquals(ErpCtErrors.ERR_CT_SIGNATURE_VERSION_NOT_FINALIZED.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testInitCreatesPendingSignatureRequest() {
        long contractId = seedContract();
        long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_FINALIZED);
        String signersJson = "[{\"name\":\"张三\",\"email\":\"zhang@ex.com\"},{\"name\":\"李四\",\"email\":\"li@ex.com\"}]";

        ErpCtSignatureRequest request = signatureRequestBiz.initSignatureRequest(
                versionId, signersJson, ErpCtConstants.SIGNATURE_PROVIDER_MOCK, CTX);

        assertEquals(ErpCtConstants.SIGNATURE_STATUS_PENDING, request.getStatus());
        assertEquals(ErpCtConstants.SIGNATURE_PROVIDER_MOCK, request.getProvider());
        assertEquals(versionId, request.getContractVersionId());
        assertNotNull(request.getProviderRequestId(), "providerRequestId 应由 Provider 回填");
        assertNotNull(request.getSigningDeadline(), "默认签署截止日期应填充");
        assertEquals(signersJson, request.getSigners());
    }

    @Test
    public void testInitFailsWhenESignatureDisabled() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_E_SIGNATURE_ENABLED, "false");
        long contractId = seedContract();
        long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_FINALIZED);

        NopException ex = assertThrows(NopException.class,
                () -> signatureRequestBiz.initSignatureRequest(versionId, "[]", null, CTX));
        assertEquals(ErpCtErrors.ERR_CT_SIGNATURE_INIT_FAILED.getErrorCode(), ex.getErrorCode());
    }

    // ============ handleSignatureCallback ============

    @Test
    public void testCallbackInvalidSignatureRejected() {
        ErpCtSignatureRequest request = seedPendingRequest();
        String payload = buildPayload(request.getProviderRequestId(),
                ErpCtConstants.SIGNATURE_EVENT_SIGNER_SIGNED, "zhang@ex.com", null);

        NopException ex = assertThrows(NopException.class,
                () -> signatureRequestBiz.handleSignatureCallback(
                        ErpCtConstants.SIGNATURE_PROVIDER_MOCK, "badsignature", "evt-1", payload, CTX));
        assertEquals(ErpCtErrors.ERR_CT_SIGNATURE_CALLBACK_SIGNATURE_INVALID.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testCallbackSignerSignedTransitionsToPartially() {
        ErpCtSignatureRequest request = seedPendingRequestWithSigners();
        String payload = buildPayload(request.getProviderRequestId(),
                ErpCtConstants.SIGNATURE_EVENT_SIGNER_SIGNED, "zhang@ex.com", null);

        signatureRequestBiz.handleSignatureCallback(
                ErpCtConstants.SIGNATURE_PROVIDER_MOCK, hmac(payload), "evt-signed-1", payload, CTX);

        ErpCtSignatureRequest reloaded = reload(request.getId());
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_PARTIALLY, reloaded.getStatus());
        assertTrue(reloaded.getSigners().contains("\"signedAt\""), "signers JSON 应含 signedAt 标记");
    }

    @Test
    public void testCallbackCompletedTransitionsToFullySignedAndSignsVersion() {
        long contractId = seedContract();
        long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_FINALIZED);
        ErpCtSignatureRequest request = seedPendingRequestForVersion(versionId);
        String payload = buildPayload(request.getProviderRequestId(),
                ErpCtConstants.SIGNATURE_EVENT_COMPLETED, null, null);

        signatureRequestBiz.handleSignatureCallback(
                ErpCtConstants.SIGNATURE_PROVIDER_MOCK, hmac(payload), "evt-completed", payload, CTX);

        ErpCtSignatureRequest reloaded = reload(request.getId());
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_FULLY, reloaded.getStatus());
        assertNotNull(reloaded.getCompletedAt(), "completedAt 应填充");
        assertNotNull(reloaded.getCertificateUrl(), "certificateUrl 应填充");
        assertNotNull(reloaded.getEvidenceNo(), "evidenceNo 应填充");
        assertNotNull(reloaded.getAttachmentFileId(), "attachmentFileId 应填充");

        // ContractVersion → SIGNED + isCurrent 翻转
        ErpCtContractVersion version = daoProvider.daoFor(ErpCtContractVersion.class).getEntityById(versionId);
        assertEquals(ErpCtConstants.VERSION_STATUS_SIGNED, version.getStatus());
        assertTrue(Boolean.TRUE.equals(version.getIsCurrent()));
    }

    @Test
    public void testCallbackDuplicateEventIdRejected() {
        ErpCtSignatureRequest request = seedPendingRequestWithSigners();
        String payload = buildPayload(request.getProviderRequestId(),
                ErpCtConstants.SIGNATURE_EVENT_SIGNER_SIGNED, "zhang@ex.com", null);
        String sig = hmac(payload);

        signatureRequestBiz.handleSignatureCallback(
                ErpCtConstants.SIGNATURE_PROVIDER_MOCK, sig, "evt-dup-1", payload, CTX);

        // 同 eventId 重复回调 → 抛 DUPLICATE_EVENT
        NopException ex = assertThrows(NopException.class,
                () -> signatureRequestBiz.handleSignatureCallback(
                        ErpCtConstants.SIGNATURE_PROVIDER_MOCK, sig, "evt-dup-1", payload, CTX));
        assertEquals(ErpCtErrors.ERR_CT_SIGNATURE_CALLBACK_DUPLICATE_EVENT.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testCallbackRejectedTransitionsToRejected() {
        ErpCtSignatureRequest request = seedPendingRequestWithSigners();
        String payload = buildPayload(request.getProviderRequestId(),
                ErpCtConstants.SIGNATURE_EVENT_REJECTED, null, "missing stamp");

        signatureRequestBiz.handleSignatureCallback(
                ErpCtConstants.SIGNATURE_PROVIDER_MOCK, hmac(payload), "evt-rej", payload, CTX);

        ErpCtSignatureRequest reloaded = reload(request.getId());
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_REJECTED, reloaded.getStatus());
        assertEquals("missing stamp", reloaded.getErrorMsg());
    }

    @Test
    public void testCallbackDeclinedFoldsToRejected() {
        // design webhook 表列 DECLINED，但状态机/字典无此态——本期按权威 6 态收敛 declined→REJECTED
        ErpCtSignatureRequest request = seedPendingRequestWithSigners();
        String payload = buildPayload(request.getProviderRequestId(),
                ErpCtConstants.SIGNATURE_EVENT_DECLINED, null, "declined reason");

        signatureRequestBiz.handleSignatureCallback(
                ErpCtConstants.SIGNATURE_PROVIDER_MOCK, hmac(payload), "evt-decl", payload, CTX);

        ErpCtSignatureRequest reloaded = reload(request.getId());
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_REJECTED, reloaded.getStatus());
    }

    @Test
    public void testCallbackExpiredTransitionsToExpired() {
        ErpCtSignatureRequest request = seedPendingRequestWithSigners();
        String payload = buildPayload(request.getProviderRequestId(),
                ErpCtConstants.SIGNATURE_EVENT_EXPIRED, null, null);

        signatureRequestBiz.handleSignatureCallback(
                ErpCtConstants.SIGNATURE_PROVIDER_MOCK, hmac(payload), "evt-exp", payload, CTX);

        ErpCtSignatureRequest reloaded = reload(request.getId());
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_EXPIRED, reloaded.getStatus());
    }

    // ============ queryAndUpdateStatus ============

    @Test
    public void testQueryAndUpdateStatusPollingAdvancesState() {
        long contractId = seedContract();
        long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_FINALIZED);
        ErpCtSignatureRequest request = seedPendingRequestForVersion(versionId);

        // 首次轮询：mock 返回 PARTIALLY → PENDING→PARTIALLY
        signatureRequestBiz.queryAndUpdateStatus(request.getId(), CTX);
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_PARTIALLY, reload(request.getId()).getStatus());

        // 二次轮询：mock 返回 COMPLETED → PARTIALLY→FULLY + 调 signVersion
        signatureRequestBiz.queryAndUpdateStatus(request.getId(), CTX);
        ErpCtSignatureRequest fully = reload(request.getId());
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_FULLY, fully.getStatus());

        ErpCtContractVersion version = daoProvider.daoFor(ErpCtContractVersion.class).getEntityById(versionId);
        assertEquals(ErpCtConstants.VERSION_STATUS_SIGNED, version.getStatus());
    }

    @Test
    public void testQueryAndUpdateStatusRejectedPath() {
        ErpCtSignatureRequest request = seedPendingRequestWithSigners();
        MockSignatureProvider.forceStatus = "REJECTED";

        signatureRequestBiz.queryAndUpdateStatus(request.getId(), CTX);
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_REJECTED, reload(request.getId()).getStatus());
    }

    // ============ 重复完成幂等 ============

    @Test
    public void testFullySignedIdempotentRejectsRepeatCompletion() {
        // 已 FULLY_SIGNED 的请求再次收到 completed → 抛 ALREADY_COMPLETED
        long contractId = seedContract();
        long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_SIGNED);
        ErpCtSignatureRequest request = seedRequestForVersion(versionId, ErpCtConstants.SIGNATURE_STATUS_FULLY);
        String payload = buildPayload(request.getProviderRequestId(),
                ErpCtConstants.SIGNATURE_EVENT_COMPLETED, null, null);

        NopException ex = assertThrows(NopException.class,
                () -> signatureRequestBiz.handleSignatureCallback(
                        ErpCtConstants.SIGNATURE_PROVIDER_MOCK, hmac(payload), "evt-repeat", payload, CTX));
        assertEquals(ErpCtErrors.ERR_CT_SIGNATURE_ALREADY_COMPLETED.getErrorCode(), ex.getErrorCode());
    }

    // ============ cancel / reject ============

    @Test
    public void testCancelSignatureRequest() {
        ErpCtSignatureRequest request = seedPendingRequestWithSigners();

        ErpCtSignatureRequest result = signatureRequestBiz.cancelSignatureRequest(request.getId(), CTX);
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_CANCELLED, result.getStatus());
    }

    @Test
    public void testCancelRejectsTerminalState() {
        ErpCtSignatureRequest request = seedRequest(ErpCtConstants.SIGNATURE_STATUS_CANCELLED);

        NopException ex = assertThrows(NopException.class,
                () -> signatureRequestBiz.cancelSignatureRequest(request.getId(), CTX));
        assertEquals(ErpCtErrors.ERR_CT_SIGNATURE_ILLEGAL_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testRejectSignatureWritesReason() {
        ErpCtSignatureRequest request = seedPendingRequestWithSigners();

        ErpCtSignatureRequest result = signatureRequestBiz.rejectSignature(request.getId(), "manual reject", CTX);
        assertEquals(ErpCtConstants.SIGNATURE_STATUS_REJECTED, result.getStatus());
        assertEquals("manual reject", result.getErrorMsg());
    }

    // ============ Registry + findExpiringRequests ============

    @Test
    public void testRegistryThrowsOnUnregisteredProvider() {
        NopException ex = assertThrows(NopException.class,
                () -> providerRegistry.getProvider("UNKNOWN_PROVIDER"));
        assertEquals(ErpCtErrors.ERR_CT_SIGNATURE_PROVIDER_NOT_REGISTERED.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testRegistryRegistersMockProvider() {
        List<String> codes = providerRegistry.getRegisteredProviderCodes();
        assertTrue(codes.contains(ErpCtConstants.SIGNATURE_PROVIDER_MOCK),
                "MockSignatureProvider 应已注册（beans.xml 配置）");
    }

    @Test
    public void testFindExpiringRequests() {
        // 已过期 + 非终态 → 命中
        seedRequestWithDeadline(ErpCtConstants.SIGNATURE_STATUS_PENDING, LocalDate.now().minusDays(1));
        // 未来 + 非终态 → 不命中
        seedRequestWithDeadline(ErpCtConstants.SIGNATURE_STATUS_PENDING, LocalDate.now().plusDays(10));
        // 已过期 + 终态 → 不命中
        seedRequestWithDeadline(ErpCtConstants.SIGNATURE_STATUS_FULLY, LocalDate.now().minusDays(2));

        List<ErpCtSignatureRequest> expiring =
                signatureRequestBiz.findExpiringRequests(LocalDate.now(), CTX);
        assertEquals(1, expiring.size(), "仅 过期+非终态 的请求应命中");
    }

    // ============ helpers ============

    private long seedContract() {
        return ormTemplate.runInSession(session -> {
            long partnerId = seedPartner();
            IEntityDao<app.erp.contract.dao.entity.ErpCtContract> dao =
                    daoProvider.daoFor(app.erp.contract.dao.entity.ErpCtContract.class);
            app.erp.contract.dao.entity.ErpCtContract c = new app.erp.contract.dao.entity.ErpCtContract();
            c.setCode("CT-SIG-" + System.nanoTime());
            c.setContractName("签章测试合同");
            c.setContractType(ErpCtConstants.CONTRACT_TYPE_PURCHASE);
            c.setContractDirection(ErpCtConstants.CONTRACT_DIRECTION_INBOUND);
            c.setPartnerId(partnerId);
            c.setStatus(ErpCtConstants.CONTRACT_STATUS_ACTIVE);
            c.setStartDate(LocalDate.of(2026, 1, 1));
            c.setEndDate(LocalDate.of(2027, 12, 31));
            dao.saveEntity(c);
            return c.getId();
        });
    }

    private long seedPartner() {
        ErpMdPartner p = new ErpMdPartner();
        p.setCode("SIG-PARTNER-" + System.nanoTime());
        p.setName("签章测试伙伴");
        p.setPartnerType("SUPPLIER");
        p.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
        return p.getId();
    }

    private long seedVersion(long contractId, int versionNo, boolean isCurrent, String status) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpCtContractVersion> dao = daoProvider.daoFor(ErpCtContractVersion.class);
            ErpCtContractVersion v = new ErpCtContractVersion();
            v.setContractId(contractId);
            v.setVersionNo(versionNo);
            v.setVersionDate(LocalDate.of(2026, 1, 1));
            v.setIsCurrent(isCurrent);
            v.setStatus(status);
            dao.saveEntity(v);
            return v.getId();
        });
    }

    private ErpCtSignatureRequest seedPendingRequest() {
        return ormTemplate.runInSession(session -> {
            long contractId = seedContract();
            long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_FINALIZED);
            return seedRequestForVersion(versionId, ErpCtConstants.SIGNATURE_STATUS_PENDING);
        });
    }

    private ErpCtSignatureRequest seedPendingRequestWithSigners() {
        return ormTemplate.runInSession(session -> {
            long contractId = seedContract();
            long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_FINALIZED);
            ErpCtSignatureRequest r = new ErpCtSignatureRequest();
            r.setContractVersionId(versionId);
            r.setProvider(ErpCtConstants.SIGNATURE_PROVIDER_MOCK);
            r.setStatus(ErpCtConstants.SIGNATURE_STATUS_PENDING);
            r.setProviderRequestId("MOCK-REQ-SEED-" + System.nanoTime());
            r.setSigners("[{\"name\":\"张三\",\"email\":\"zhang@ex.com\"},{\"name\":\"李四\",\"email\":\"li@ex.com\"}]");
            r.setSigningDeadline(LocalDate.now().plusDays(15));
            daoProvider.daoFor(ErpCtSignatureRequest.class).saveEntity(r);
            return r;
        });
    }

    private ErpCtSignatureRequest seedPendingRequestForVersion(long versionId) {
        return seedRequestForVersion(versionId, ErpCtConstants.SIGNATURE_STATUS_PENDING);
    }

    private ErpCtSignatureRequest seedRequestForVersion(long versionId, String status) {
        return ormTemplate.runInSession(session -> {
            ErpCtSignatureRequest r = new ErpCtSignatureRequest();
            r.setContractVersionId(versionId);
            r.setProvider(ErpCtConstants.SIGNATURE_PROVIDER_MOCK);
            r.setStatus(status);
            r.setProviderRequestId("MOCK-REQ-SEED-" + System.nanoTime());
            r.setSigners("[{\"name\":\"张三\",\"email\":\"zhang@ex.com\"}]");
            r.setSigningDeadline(LocalDate.now().plusDays(15));
            if (ErpCtConstants.SIGNATURE_STATUS_FULLY.equals(status)) {
                r.setCompletedAt(CoreMetrics.currentDateTime());
            }
            daoProvider.daoFor(ErpCtSignatureRequest.class).saveEntity(r);
            return r;
        });
    }

    private ErpCtSignatureRequest seedRequest(String status) {
        return ormTemplate.runInSession(session -> {
            long contractId = seedContract();
            long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_FINALIZED);
            return seedRequestForVersion(versionId, status);
        });
    }

    private void seedRequestWithDeadline(String status, LocalDate deadline) {
        ormTemplate.runInSession(session -> {
            long contractId = seedContract();
            long versionId = seedVersion(contractId, 1, true, ErpCtConstants.VERSION_STATUS_FINALIZED);
            ErpCtSignatureRequest r = new ErpCtSignatureRequest();
            r.setContractVersionId(versionId);
            r.setProvider(ErpCtConstants.SIGNATURE_PROVIDER_MOCK);
            r.setStatus(status);
            r.setProviderRequestId("MOCK-REQ-EXP-" + System.nanoTime());
            r.setSigners("[]");
            r.setSigningDeadline(deadline);
            daoProvider.daoFor(ErpCtSignatureRequest.class).saveEntity(r);
            return null;
        });
    }

    private ErpCtSignatureRequest reload(Long requestId) {
        return daoProvider.daoFor(ErpCtSignatureRequest.class).getEntityById(requestId);
    }

    private String buildPayload(String providerRequestId, String eventType, String signerEmail, String reason) {
        StringBuilder sb = new StringBuilder("{\"providerRequestId\":\"" + providerRequestId + "\"");
        sb.append(",\"eventType\":\"").append(eventType).append("\"");
        if (signerEmail != null) {
            sb.append(",\"signerEmail\":\"").append(signerEmail).append("\"");
        }
        if (reason != null) {
            sb.append(",\"reason\":\"").append(reason).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /** webhook HMAC 密钥——与 {@link ErpCtSignatureRequestBizModel#WEBHOOK_SECRET} 一致。 */
    private static String hmac(String payload) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    ErpCtSignatureRequestBizModel.WEBHOOK_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 防 unused-import 警告（QueryBean 留作未来扩展查询）。 */
    @SuppressWarnings("unused")
    private QueryBean eqQuery(String field, Object value) {
        QueryBean q = new QueryBean();
        q.addFilter(eq(field, value));
        return q;
    }

    /** 防 unused 警告（assertNotEquals 留作后续断言扩展）。 */
    @SuppressWarnings("unused")
    private void assertNotEmpty(List<?> list) {
        assertNotEquals(0, list.size());
        assertNull(null);
    }
}
