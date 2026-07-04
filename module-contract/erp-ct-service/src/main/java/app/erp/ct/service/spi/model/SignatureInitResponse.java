package app.erp.ct.service.spi.model;

/**
 * 发起签署请求响应。Provider 返回确定性 {@code providerRequestId} 供后续查询/回调匹配。
 */
public class SignatureInitResponse {

    private String providerRequestId;

    /** 签署入口 URL（首个签署人或汇总页）。{@code null}=Provider 不提供。 */
    private String signUrl;

    /** 发起时间戳（毫秒）。 */
    private long initiated;

    public String getProviderRequestId() {
        return providerRequestId;
    }

    public void setProviderRequestId(String providerRequestId) {
        this.providerRequestId = providerRequestId;
    }

    public String getSignUrl() {
        return signUrl;
    }

    public void setSignUrl(String signUrl) {
        this.signUrl = signUrl;
    }

    public long getInitiated() {
        return initiated;
    }

    public void setInitiated(long initiated) {
        this.initiated = initiated;
    }
}
