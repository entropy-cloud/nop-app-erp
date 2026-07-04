package app.erp.ct.service.spi.model;

import java.util.List;

/**
 * 签署状态查询响应（轮询兜底用，与 webhook 回调互补）。
 *
 * <p>返回的是 Provider 侧的中立状态——业务侧（BizModel）按其推进 ErpCtSignatureRequest 状态机。
 */
public class SignatureStatusQueryResponse {

    /**
     * Provider 侧状态码（与 design e-signature.md webhook 事件同词表）：
     * "PENDING"/"PARTIALLY"/"COMPLETED"/"REJECTED"/"EXPIRED"。
     */
    private String status;

    /** 已签署的签署人邮箱列表（用于回写 signers JSON 的 signedAt 标记）。 */
    private List<String> signedSignerEmails;

    /** 证书是否可下载（COMPLETED 后为 true）。 */
    private boolean certificateAvailable;

    /** Provider 错误描述（查询失败/拒签原因等）。 */
    private String errorMsg;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSignedSignerEmails() {
        return signedSignerEmails;
    }

    public void setSignedSignerEmails(List<String> signedSignerEmails) {
        this.signedSignerEmails = signedSignerEmails;
    }

    public boolean isCertificateAvailable() {
        return certificateAvailable;
    }

    public void setCertificateAvailable(boolean certificateAvailable) {
        this.certificateAvailable = certificateAvailable;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
