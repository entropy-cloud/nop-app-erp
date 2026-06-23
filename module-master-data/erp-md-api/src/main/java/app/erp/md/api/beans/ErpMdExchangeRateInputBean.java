//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdExchangeRateInputBean extends CrudInputBase {

    
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


    }
