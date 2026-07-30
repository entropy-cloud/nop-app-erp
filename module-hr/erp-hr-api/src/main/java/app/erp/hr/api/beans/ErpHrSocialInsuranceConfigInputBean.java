//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSocialInsuranceConfigInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _cityCode;

    
        @PropMeta(propId=2)
    
        public String getCityCode(){
            return _cityCode;
        }

        public void setCityCode(String value){
            this._cityCode = value;
        }


        private String _insuranceType;

    
        @PropMeta(propId=3)
    
        public String getInsuranceType(){
            return _insuranceType;
        }

        public void setInsuranceType(String value){
            this._insuranceType = value;
        }


        private java.math.BigDecimal _companyRate;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getCompanyRate(){
            return _companyRate;
        }

        public void setCompanyRate(java.math.BigDecimal value){
            this._companyRate = value;
        }


        private java.math.BigDecimal _employeeRate;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getEmployeeRate(){
            return _employeeRate;
        }

        public void setEmployeeRate(java.math.BigDecimal value){
            this._employeeRate = value;
        }


        private java.math.BigDecimal _baseLowerLimit;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getBaseLowerLimit(){
            return _baseLowerLimit;
        }

        public void setBaseLowerLimit(java.math.BigDecimal value){
            this._baseLowerLimit = value;
        }


        private java.math.BigDecimal _baseUpperLimit;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getBaseUpperLimit(){
            return _baseUpperLimit;
        }

        public void setBaseUpperLimit(java.math.BigDecimal value){
            this._baseUpperLimit = value;
        }


        private java.time.LocalDate _effectiveFrom;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getEffectiveFrom(){
            return _effectiveFrom;
        }

        public void setEffectiveFrom(java.time.LocalDate value){
            this._effectiveFrom = value;
        }


        private java.time.LocalDate _effectiveTo;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getEffectiveTo(){
            return _effectiveTo;
        }

        public void setEffectiveTo(java.time.LocalDate value){
            this._effectiveTo = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=10)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
