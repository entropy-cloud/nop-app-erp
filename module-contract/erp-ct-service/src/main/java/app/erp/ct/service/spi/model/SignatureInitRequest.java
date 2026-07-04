package app.erp.ct.service.spi.model;

import java.util.List;

/**
 * 发起签署请求输入（对应 design e-signature.md {@code Provider.initSignature()} 入参）。
 *
 * <p>{@code signingOrder} 字段为本期 SPI 增量（design e-signature.md SPI 接口未列）：
 * "SEQUENTIAL"=顺序签署 / "PARALLEL"=并行签署，由具体 Provider 解释。
 */
public class SignatureInitRequest {

    /** 待签署合同版本 ID（审计/日志用，不直接传给三方）。 */
    private Long contractVersionId;

    /** 已定稿合同文档的附件 fileId（{@code stdDomain="file"}）。Provider 上传到其侧。 */
    private String attachmentFileId;

    /** 签署人列表（至少 1 个）。 */
    private List<Signer> signers;

    /** 签署顺序："SEQUENTIAL"（默认）/ "PARALLEL"。本期 SPI 增量。 */
    private String signingOrder;

    /** 回调通知 URL（Provider webhook 推送目标）。{@code null}=不注册回调，仅轮询。 */
    private String callbackUrl;

    public Long getContractVersionId() {
        return contractVersionId;
    }

    public void setContractVersionId(Long contractVersionId) {
        this.contractVersionId = contractVersionId;
    }

    public String getAttachmentFileId() {
        return attachmentFileId;
    }

    public void setAttachmentFileId(String attachmentFileId) {
        this.attachmentFileId = attachmentFileId;
    }

    public List<Signer> getSigners() {
        return signers;
    }

    public void setSigners(List<Signer> signers) {
        this.signers = signers;
    }

    public String getSigningOrder() {
        return signingOrder;
    }

    public void setSigningOrder(String signingOrder) {
        this.signingOrder = signingOrder;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}
