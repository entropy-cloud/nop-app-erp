//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinArApItemInputBean extends CrudInputBase {

    
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


        private Long _acctSchemaId;

    
        @PropMeta(propId=4)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private String _direction;

    
        @PropMeta(propId=5)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=6)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private String _sourceBillType;

    
        @PropMeta(propId=7)
    
        public String getSourceBillType(){
            return _sourceBillType;
        }

        public void setSourceBillType(String value){
            this._sourceBillType = value;
        }


        private String _sourceBillCode;

    
        @PropMeta(propId=8)
    
        public String getSourceBillCode(){
            return _sourceBillCode;
        }

        public void setSourceBillCode(String value){
            this._sourceBillCode = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.time.LocalDate _dueDate;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getDueDate(){
            return _dueDate;
        }

        public void setDueDate(java.time.LocalDate value){
            this._dueDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=11)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private java.math.BigDecimal _settledAmountSource;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getSettledAmountSource(){
            return _settledAmountSource;
        }

        public void setSettledAmountSource(java.math.BigDecimal value){
            this._settledAmountSource = value;
        }


        private java.math.BigDecimal _settledAmountFunctional;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getSettledAmountFunctional(){
            return _settledAmountFunctional;
        }

        public void setSettledAmountFunctional(java.math.BigDecimal value){
            this._settledAmountFunctional = value;
        }


        private java.math.BigDecimal _openAmountSource;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getOpenAmountSource(){
            return _openAmountSource;
        }

        public void setOpenAmountSource(java.math.BigDecimal value){
            this._openAmountSource = value;
        }


        private java.math.BigDecimal _openAmountFunctional;

    
        @PropMeta(propId=18)
    
        public java.math.BigDecimal getOpenAmountFunctional(){
            return _openAmountFunctional;
        }

        public void setOpenAmountFunctional(java.math.BigDecimal value){
            this._openAmountFunctional = value;
        }


        private String _status;

    
        @PropMeta(propId=19)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _periodId;

    
        @PropMeta(propId=20)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private String _remark;

    
        @PropMeta(propId=21)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=22)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
