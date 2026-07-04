package app.erp.ct.service.spi.mock;

import app.erp.ct.service.spi.IErpCtSignatureProvider;
import app.erp.ct.service.spi.model.SignatureInitRequest;
import app.erp.ct.service.spi.model.SignatureInitResponse;
import app.erp.ct.service.spi.model.SignatureStatusQueryResponse;
import app.erp.ct.service.spi.model.Signer;
import io.nop.api.core.time.CoreMetrics;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock 签章提供商（{@code providerCode="MOCK"}，无外部 HTTP，内联可测试实现）。
 *
 * <p>用于全链行为验证（init→status 推进→getSignUrl→retrieveCertificate）。
 * 真实三方供应商 HTTP 集成（e签宝/DocuSign/Tsign）归 follow-up（计划 Non-Goal）。
 *
 * <p><b>状态推进模型</b>：每个 providerRequestId 持有计数器，{@link #queryStatus} 按调用次数确定性推进——
 * <ul>
 *   <li>第 1 次：{@code PARTIALLY}（首个签署人已签）</li>
 *   <li>第 2 次：{@code COMPLETED}（全部签署完成）</li>
 * </ul>
 * 测试可通过 {@link #forceStatus} 强制覆盖（覆盖拒签/过期路径）。
 *
 * <p>回调路径不经过 Provider（webhook payload 由测试/BizModel 构造），故 Mock 仅服务轮询入口。
 */
public class MockSignatureProvider implements IErpCtSignatureProvider {

    public static final String PROVIDER_CODE = "MOCK";

    /** 测试钩子：强制覆盖下次 queryStatus 的返回状态（覆盖 REJECTED/EXPIRED 路径）。{@code null}=按计数器推进。 */
    public static volatile String forceStatus;

    /** 共享状态：providerRequestId → 轮询调用次数（确定性状态推进）。 */
    private final Map<String, AtomicInteger> queryCounts = new ConcurrentHashMap<>();

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public SignatureInitResponse initSignature(SignatureInitRequest request) {
        String providerRequestId = "MOCK-REQ-" + System.nanoTime();
        queryCounts.put(providerRequestId, new AtomicInteger(0));

        SignatureInitResponse resp = new SignatureInitResponse();
        resp.setProviderRequestId(providerRequestId);
        resp.setSignUrl("https://mock.sign/" + providerRequestId);
        resp.setInitiated(CoreMetrics.currentTimeMillis());
        return resp;
    }

    @Override
    public SignatureStatusQueryResponse queryStatus(String providerRequestId) {
        SignatureStatusQueryResponse resp = new SignatureStatusQueryResponse();
        // 懒注册：未知 providerRequestId（如测试直接 seed 或服务重启后查询）也按计数器推进，
        // 避免测试必须经 initSignature 才能 query。
        AtomicInteger counter = queryCounts.computeIfAbsent(providerRequestId, k -> new AtomicInteger(0));
        int n = counter.incrementAndGet();
        String status;
        if (forceStatus != null) {
            status = forceStatus;
            // 强制状态消费后清空，避免污染后续断言
            forceStatus = null;
        } else {
            status = n <= 1 ? "PARTIALLY" : "COMPLETED";
        }

        resp.setStatus(status);
        resp.setSignedSignerEmails(buildSignedEmails(providerRequestId, status));
        resp.setCertificateAvailable("COMPLETED".equals(status));
        if ("REJECTED".equals(status)) {
            resp.setErrorMsg("mock 拒签测试");
        }
        return resp;
    }

    @Override
    public String getSignUrl(String providerRequestId, String signerEmail) {
        return "https://mock.sign/" + providerRequestId + "?email=" + signerEmail;
    }

    @Override
    public byte[] retrieveCertificate(String providerRequestId) {
        // 占位证书字节（mock 不生成真实 PDF/证书链）
        return ("MOCK-CERTIFICATE-" + providerRequestId).getBytes(StandardCharsets.UTF_8);
    }

    /** 测试重置：清空共享状态与强制钩子（@BeforeEach 调用）。 */
    public void resetTestState() {
        queryCounts.clear();
        forceStatus = null;
    }

    private List<String> buildSignedEmails(String providerRequestId, String status) {
        if ("COMPLETED".equals(status)) {
            // 真实场景由 Provider 基于其侧状态返回；mock 简化为单元素占位（业务侧按 webhook signers JSON 为准）
            return new ArrayList<>(Collections.singletonList("mock-signed@example.com"));
        }
        return Collections.emptyList();
    }
}
