//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinCreditFacilityInputBean extends CrudInputBase {

    
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


        private Long _fundAccountId;

    
        @PropMeta(propId=4)
    
        public Long getFundAccountId(){
            return _fundAccountId;
        }

        public void setFundAccountId(Long value){
            this._fundAccountId = value;
        }


        private String _facilityType;

    
        @PropMeta(propId=5)
    
        public String getFacilityType(){
            return _facilityType;
        }

        public void setFacilityType(String value){
            this._facilityType = value;
        }


        private java.math.BigDecimal _totalAmount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal value){
            this._totalAmount = value;
        }


        private java.math.BigDecimal _usedAmount;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getUsedAmount(){
            return _usedAmount;
        }

        public void setUsedAmount(java.math.BigDecimal value){
            this._usedAmount = value;
        }


        private java.math.BigDecimal _availableAmount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getAvailableAmount(){
            return _availableAmount;
        }

        public void setAvailableAmount(java.math.BigDecimal value){
            this._availableAmount = value;
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


        private String _status;

    
        @PropMeta(propId=11)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
