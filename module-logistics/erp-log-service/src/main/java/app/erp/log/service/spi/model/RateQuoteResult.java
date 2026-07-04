package app.erp.log.service.spi.model;

import java.math.BigDecimal;

/**
 * 比价结果 DTO（getRateQuote 返回：承运商、服务类型、运费、预计时效）。
 */
public class RateQuoteResult {
    private String gatewayId;
    private String serviceType;
    private BigDecimal freight;
    private String currency;
    /** 预计时效（天），如 "1-2"。 */
    private String estimatedTransitDays;

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public BigDecimal getFreight() {
        return freight;
    }

    public void setFreight(BigDecimal freight) {
        this.freight = freight;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getEstimatedTransitDays() {
        return estimatedTransitDays;
    }

    public void setEstimatedTransitDays(String estimatedTransitDays) {
        this.estimatedTransitDays = estimatedTransitDays;
    }
}
