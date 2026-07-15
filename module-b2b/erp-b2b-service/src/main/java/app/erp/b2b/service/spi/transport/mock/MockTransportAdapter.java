package app.erp.b2b.service.spi.transport.mock;

import app.erp.b2b.dao.entity.ErpB2bMftConfig;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.spi.transport.IErpB2bTransportAdapter;
import app.erp.b2b.service.spi.transport.model.InboundFile;
import app.erp.b2b.service.spi.transport.model.TransportResult;
import io.nop.api.core.exceptions.NopException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Mock 传输适配器（{@code protocol="HTTPS"}，无外部网络，内联可测试实现）。
 *
 * <p>用于全链行为验证（TransportManager 路由/重试/死信/写 ErpB2bMftLog）。
 * 真实 AS2/SFTP/FTPS 协议库集成归 follow-up（Non-Goal）。
 *
 * <p><b>测试钩子</b>（static，测试用例可控失败模式以覆盖重试/死信路径）：
 * <ul>
 *   <li>{@link #FAILURE_MODE_SUCCESS}（默认）：send 成功生成确定性 messageId/fileHash。</li>
 *   <li>{@link #FAILURE_MODE_5XX}：抛 5xx 异常（可重试，触发指数退避至死信）。</li>
 *   <li>{@link #FAILURE_MODE_4XX}：抛 4xx 异常（不可重试）。</li>
 * </ul>
 */
public class MockTransportAdapter implements IErpB2bTransportAdapter {

    public static final int FAILURE_MODE_SUCCESS = 0;
    public static final int FAILURE_MODE_5XX = 1;
    public static final int FAILURE_MODE_4XX = 2;

    /** 测试钩子：失败模式（默认成功）。测试用例设置后影响后续 send。 */
    public static volatile int failureMode = FAILURE_MODE_SUCCESS;

    @Override
    public String getSupportedProtocol() {
        return ErpB2bConstants.MFT_PROTOCOL_HTTPS;
    }

    @Override
    public TransportResult send(ErpB2bMftConfig config, String payload, String fileName) {
        if (failureMode == FAILURE_MODE_5XX) {
            throw new NopException(ErpB2bErrors.ERR_B2B_MFT_ADAPTER_NOT_REGISTERED)
                    .param(ErpB2bErrors.ARG_PROTOCOL, config.getProtocol())
                    .param("httpStatus", 500);
        }
        if (failureMode == FAILURE_MODE_4XX) {
            throw new NopException(ErpB2bErrors.ERR_B2B_MFT_ADAPTER_NOT_REGISTERED)
                    .param(ErpB2bErrors.ARG_PROTOCOL, config.getProtocol())
                    .param("httpStatus", 401);
        }
        String fileHash = sha256(payload);
        String messageId = "MOCK-MSG-" + fileHash.substring(0, 16);
        return TransportResult.success(messageId, fileHash);
    }

    @Override
    public List<InboundFile> pullInbound(ErpB2bMftConfig config) {
        // webhook 为 ASN 入站主路径，Mock 返回空列表
        return Collections.emptyList();
    }

    /** 测试重置：清空失败模式（@BeforeEach 调用）。 */
    public void resetTestState() {
        failureMode = FAILURE_MODE_SUCCESS;
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "HASH-ERROR";
        }
    }
}
