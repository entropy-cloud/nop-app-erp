//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstDepreciationScheduleInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _assetId;

    
        @PropMeta(propId=2)
    
        public Long getAssetId(){
            return _assetId;
        }

        public void setAssetId(Long value){
            this._assetId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _period;

    
        @PropMeta(propId=4)
    
        public String getPeriod(){
            return _period;
        }

        public void setPeriod(String value){
            this._period = value;
        }


        private java.math.BigDecimal _plannedAmount;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getPlannedAmount(){
            return _plannedAmount;
        }

        public void setPlannedAmount(java.math.BigDecimal value){
            this._plannedAmount = value;
        }


        private java.math.BigDecimal _actualAmount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getActualAmount(){
            return _actualAmount;
        }

        public void setActualAmount(java.math.BigDecimal value){
            this._actualAmount = value;
        }


        private java.math.BigDecimal _accumulatedDepreciation;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getAccumulatedDepreciation(){
            return _accumulatedDepreciation;
        }

        public void setAccumulatedDepreciation(java.math.BigDecimal value){
            this._accumulatedDepreciation = value;
        }


        private java.math.BigDecimal _netBookValue;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getNetBookValue(){
            return _netBookValue;
        }

        public void setNetBookValue(java.math.BigDecimal value){
            this._netBookValue = value;
        }


        private String _status;

    
        @PropMeta(propId=9)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private java.time.LocalDateTime _executedAt;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDateTime getExecutedAt(){
            return _executedAt;
        }

        public void setExecutedAt(java.time.LocalDateTime value){
            this._executedAt = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=11)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=12)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=13)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private Long _voucherId;

    
        @PropMeta(propId=14)
    
        public Long getVoucherId(){
            return _voucherId;
        }

        public void setVoucherId(Long value){
            this._voucherId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=21)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=22)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private String _exchangeRate;

    
        @PropMeta(propId=23)
    
        public String getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(String value){
            this._exchangeRate = value;
        }


        private String _amountSource;

    
        @PropMeta(propId=24)
    
        public String getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(String value){
            this._amountSource = value;
        }


        private String _amountFunctional;

    
        @PropMeta(propId=25)
    
        public String getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(String value){
            this._amountFunctional = value;
        }


    }
