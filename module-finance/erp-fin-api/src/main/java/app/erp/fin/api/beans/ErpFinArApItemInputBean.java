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


        private String _direction;

    
        @PropMeta(propId=5)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=6)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
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


        private String _currencyId;

    
        @PropMeta(propId=11)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
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


        private String _periodId;

    
        @PropMeta(propId=20)
    
        public String getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(String value){
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


    }
