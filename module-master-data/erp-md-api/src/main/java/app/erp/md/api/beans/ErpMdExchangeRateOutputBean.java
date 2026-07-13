//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdExchangeRateOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _fromCurrencyId;

    
        @PropMeta(propId=2)
    
        public Long getFromCurrencyId(){
            return _fromCurrencyId;
        }

        public void setFromCurrencyId(Long value){
            this._fromCurrencyId = value;
        }


        private Long _toCurrencyId;

    
        @PropMeta(propId=3)
    
        public Long getToCurrencyId(){
            return _toCurrencyId;
        }

        public void setToCurrencyId(Long value){
            this._toCurrencyId = value;
        }


        private String _rateType;

    
        @PropMeta(propId=4)
    
        public String getRateType(){
            return _rateType;
        }

        public void setRateType(String value){
            this._rateType = value;
        }


        private java.math.BigDecimal _rate;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getRate(){
            return _rate;
        }

        public void setRate(java.math.BigDecimal value){
            this._rate = value;
        }


        private java.time.LocalDate _validFrom;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getValidFrom(){
            return _validFrom;
        }

        public void setValidFrom(java.time.LocalDate value){
            this._validFrom = value;
        }


        private java.time.LocalDate _validTo;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getValidTo(){
            return _validTo;
        }

        public void setValidTo(java.time.LocalDate value){
            this._validTo = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=8)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=9)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=10)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=12)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _fromCurrencyName;

    
        public String getFromCurrencyName(){
            return _fromCurrencyName;
        }

        public void setFromCurrencyName(String value){
            this._fromCurrencyName = value;
        }


        private String _toCurrencyName;

    
        public String getToCurrencyName(){
            return _toCurrencyName;
        }

        public void setToCurrencyName(String value){
            this._toCurrencyName = value;
        }


        private Map<String,Object> _fromCurrency;

        public Map<String,Object> getFromCurrency(){
            return _fromCurrency;
        }

        public void setFromCurrency(Map<String,Object> value){
            this._fromCurrency = value;
        }


        private Map<String,Object> _toCurrency;

        public Map<String,Object> getToCurrency(){
            return _toCurrency;
        }

        public void setToCurrency(Map<String,Object> value){
            this._toCurrency = value;
        }


    }
