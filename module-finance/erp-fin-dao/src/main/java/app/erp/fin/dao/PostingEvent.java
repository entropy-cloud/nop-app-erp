package app.erp.fin.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业财过账事件。由业务域调用方在单据审核通过后构造，提交给凭证聚合根 facade
 * {@code IErpFinVoucherBiz.post(PostingEvent, IServiceContext)}。
 *
 * <p>{@code billData} 承载单据头+行数据（含金额占位键值与科目映射维度），由各 Provider 自行解释。
 *
 * <p>本类型位于 finance-dao（跨域契约面），供各业务域（如 inventory）跨模块构造与提交；
 * 引擎只持有本快照，不持有任意源业务单据的 ORM 引用，因此不置位源单据的 {@code posted} 标志
 * （源 {@code posted} 由域调用方在 {@code post()} 成功返回后自行置位）。
 */
public class PostingEvent {
    private ErpFinBusinessType businessType;
    private String billHeadCode;
    /**
     * 端到端追踪 ID。由业务域调用方在单据审核通过后构造时可选传入；引擎入口缺失时生成
     * （{@code StringHelper.generateUUID()}），透传至 {@code AcctDocContext} 与异常记录，
     * 串联业务域审核 → 事件派发 → 过账编排 → GL 写入全链路（见 {@code posting-log.md §关联穿透}）。
     */
    private String traceId;
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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
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
