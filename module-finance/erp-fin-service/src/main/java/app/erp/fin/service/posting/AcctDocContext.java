package app.erp.fin.service.posting;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 过账上下文。由过账编排服务在调用 Provider/Validator 前装配，承载期间/账套/币种/汇率等解析结果，
 * 供 Provider 与 Validator 共享，避免各组件重复查询主数据。
 */
public class AcctDocContext {
    /** 端到端追踪 ID（由 PostingEvent 透传，串联全链路日志与异常记录）。 */
    private String traceId;
    private LocalDate voucherDate;
    private Long acctSchemaId;
    private Long orgId;
    private Long currencyId;
    private BigDecimal exchangeRate;
    private Long periodId;
    private String periodStatus;
    private String voucherType;

    public LocalDate getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(LocalDate voucherDate) {
        this.voucherDate = voucherDate;
    }

    public Long getAcctSchemaId() {
        return acctSchemaId;
    }

    public void setAcctSchemaId(Long acctSchemaId) {
        this.acctSchemaId = acctSchemaId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Long currencyId) {
        this.currencyId = currencyId;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public String getPeriodStatus() {
        return periodStatus;
    }

    public void setPeriodStatus(String periodStatus) {
        this.periodStatus = periodStatus;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
