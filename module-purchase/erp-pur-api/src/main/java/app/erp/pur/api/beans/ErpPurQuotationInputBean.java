//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurQuotationInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _rfqId;

    
        @PropMeta(propId=4)
    
        public Long getRfqId(){
            return _rfqId;
        }

        public void setRfqId(Long value){
            this._rfqId = value;
        }


        private Long _supplierId;

    
        @PropMeta(propId=5)
    
        public Long getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(Long value){
            this._supplierId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=6)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.time.LocalDate _validFrom;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getValidFrom(){
            return _validFrom;
        }

        public void setValidFrom(java.time.LocalDate value){
            this._validFrom = value;
        }


        private java.time.LocalDate _validTo;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getValidTo(){
            return _validTo;
        }

        public void setValidTo(java.time.LocalDate value){
            this._validTo = value;
        }


        private Boolean _isAccepted;

    
        @PropMeta(propId=11)
    
        public Boolean getIsAccepted(){
            return _isAccepted;
        }

        public void setIsAccepted(Boolean value){
            this._isAccepted = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=12)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=13)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=200)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=201)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private List<ErpPurQuotationLineInputBean> _lines;

        public List<ErpPurQuotationLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpPurQuotationLineInputBean> value){
            this._lines = value;
        }


    }
