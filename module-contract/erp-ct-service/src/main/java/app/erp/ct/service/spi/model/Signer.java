package app.erp.ct.service.spi.model;

/**
 * 签署人信息（SPI 中立 DTO，对应 design e-signature.md {@code signers JSON} 元素）。
 *
 * <p>持久化时序列化为 {@code ErpCtSignatureRequest.signers} JSON 数组元素，
 * 含运行时回填的 {@code signedAt}/{@code rejectedReason}（非 SPI 输入字段）。
 */
public class Signer {

    private String name;
    private String email;
    private String phone;

    /** 运行时回填：签署完成时间（毫秒时间戳，0=未签）。回调 {@code signer.signed} 写入。 */
    private long signedAt;

    /** 运行时回填：拒签原因（回调 {@code signing.rejected/declined} 写入）。 */
    private String rejectedReason;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(long signedAt) {
        this.signedAt = signedAt;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }
}
