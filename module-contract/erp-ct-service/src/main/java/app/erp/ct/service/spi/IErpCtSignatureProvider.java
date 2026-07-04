package app.erp.ct.service.spi;

import app.erp.ct.service.spi.model.SignatureInitRequest;
import app.erp.ct.service.spi.model.SignatureInitResponse;
import app.erp.ct.service.spi.model.SignatureStatusQueryResponse;

/**
 * 电子签章提供商 SPI（对应 {@code docs/design/contract/e-signature.md §SPI 接口}）。
 *
 * <p>每个具体供应商（e签宝/DocuSign/Tsign/...）实现一个 Bean，
 * 由 {@link ErpCtSignatureProviderRegistry} 按 {@link #getProviderCode()} 建图派发。
 *
 * <p>实现约束：
 * <ul>
 *   <li>{@link #initSignature} 必须返回确定性 {@code providerRequestId}（用于后续查询/回调匹配）。</li>
 *   <li>{@link #retrieveCertificate} 仅在签署完成（FULLY_SIGNED）后有意义；未完成返回 {@code null} 或空。</li>
 *   <li>所有方法应为非阻塞；外部 HTTP 调用应在 Provider 实现内做超时/重试控制。</li>
 * </ul>
 *
 * <p>本期实现：{@code app.erp.ct.service.spi.mock.MockSignatureProvider}（providerCode="mock"，
 * 无外部 HTTP，全链可测试）。真实三方供应商归 follow-up（计划 Non-Goal）。
 */
public interface IErpCtSignatureProvider {

    /** 提供商编码，对应 dict {@code erp-ct/sign-provider} 的 code（如 "MOCK"/"ESIGN_BAO"/"DOCUSIGN"/"TSIGN"）。 */
    String getProviderCode();

    /** 发起签署请求：上传文档 + 指定签署人/顺序 + 注册回调 → 返回提供商侧请求 ID 与签署入口 URL。 */
    SignatureInitResponse initSignature(SignatureInitRequest request);

    /** 主动查询签署状态（轮询兜底，与 webhook 回调互补）。 */
    SignatureStatusQueryResponse queryStatus(String providerRequestId);

    /** 取某签署人的签署链接（邮件/短信分发用）。 */
    String getSignUrl(String providerRequestId, String signerEmail);

    /** 签署完成后下载已签署文档/证书（含签章图形的 PDF + 证据存证）。 */
    byte[] retrieveCertificate(String providerRequestId);
}
