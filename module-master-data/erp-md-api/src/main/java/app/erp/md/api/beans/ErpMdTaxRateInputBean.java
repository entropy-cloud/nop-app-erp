//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdTaxRateInputBean extends CrudInputBase {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private String _taxType;

    
        @PropMeta(propId=4)
    
        public String getTaxType(){
            return _taxType;
        }

        public void setTaxType(String value){
            this._taxType = value;
        }


        private java.math.BigDecimal _rate;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getRate(){
            return _rate;
        }

        public void setRate(java.math.BigDecimal value){
            this._rate = value;
        }


        private Boolean _isZeroRated;

    
        @PropMeta(propId=6)
    
        public Boolean getIsZeroRated(){
            return _isZeroRated;
        }

        public void setIsZeroRated(Boolean value){
            this._isZeroRated = value;
        }


        private Boolean _isExempt;

    
        @PropMeta(propId=7)
    
        public Boolean getIsExempt(){
            return _isExempt;
        }

        public void setIsExempt(Boolean value){
            this._isExempt = value;
        }


        private java.time.LocalDate _validFrom;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getValidFrom(){
            return _validFrom;
        }

        public void setValidFrom(java.time.LocalDate value){
            this._validFrom = value;
        }


        private java.time.LocalDate _validTo;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getValidTo(){
            return _validTo;
        }

        public void setValidTo(java.time.LocalDate value){
            this._validTo = value;
        }


        private String _status;

    
        @PropMeta(propId=10)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
