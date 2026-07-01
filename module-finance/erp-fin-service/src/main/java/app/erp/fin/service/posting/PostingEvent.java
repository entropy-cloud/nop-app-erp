package app.erp.fin.service.posting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业财过账事件。由业务域调用方在单据审核通过后构造，提交给 {@link ErpFinPostingService#post(PostingEvent)}。
 *
 * <p>{@code billData} 承载单据头+行数据（含金额占位键值与科目映射维度），由各 Provider 自行解释。
 *
 * <p>引擎只持有本快照，不持有任意源业务单据的 ORM 引用，因此不置位源单据的 {@code posted} 标志
 * （源 {@code posted} 由域调用方在 {@code post()} 成功返回后自行置位）。
 */
public class PostingEvent {
    private ErpFinBusinessType businessType;
    private String billHeadCode;
    private String tenantId;
    private Long acctSchemaId;
    private Long orgId;
    private Long currencyId;
    private BigDecimal exchangeRate;
    private LocalDate voucherDate;
    private Map<String, Object> billData = new LinkedHashMap<>();

    public ErpFinBusinessType getBusinessType() {
        return businessType;
    }

    public void setBusinessType(ErpFinBusinessType businessType) {
        this.businessType = businessType;
    }

    public String getBillHeadCode() {
        return billHeadCode;
    }

    public void setBillHeadCode(String billHeadCode) {
        this.billHeadCode = billHeadCode;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public LocalDate getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(LocalDate voucherDate) {
        this.voucherDate = voucherDate;
    }

    public Map<String, Object> getBillData() {
        return billData;
    }

    public void setBillData(Map<String, Object> billData) {
        this.billData = billData;
    }
}
