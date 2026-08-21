//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBadDebtInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _acctSchemaId;

    
        @PropMeta(propId=4)
    
        public String getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(String value){
            this._acctSchemaId = value;
        }


        private String _docType;

    
        @PropMeta(propId=5)
    
        public String getDocType(){
            return _docType;
        }

        public void setDocType(String value){
            this._docType = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=6)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private String _sourceArApItemId;

    
        @PropMeta(propId=7)
    
        public String getSourceArApItemId(){
            return _sourceArApItemId;
        }

        public void setSourceArApItemId(String value){
            this._sourceArApItemId = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
            this._amount = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=9)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _reason;

    
        @PropMeta(propId=12)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


        private String _approvalStatus;

    
        @PropMeta(propId=13)
    
        public String getApprovalStatus(){
            return _approvalStatus;
        }

        public void setApprovalStatus(String value){
            this._approvalStatus = value;
        }


        private String _periodId;

    
        @PropMeta(propId=14)
    
        public String getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(String value){
            this._periodId = value;
        }


        private String _voucherId;

    
        @PropMeta(propId=15)
    
        public String getVoucherId(){
            return _voucherId;
        }

        public void setVoucherId(String value){
            this._voucherId = value;
        }


        private String _remark;

    
        @PropMeta(propId=16)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
