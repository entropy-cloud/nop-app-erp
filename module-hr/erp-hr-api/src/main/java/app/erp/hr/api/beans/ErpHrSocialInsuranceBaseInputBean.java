//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSocialInsuranceBaseInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _employeeId;

    
        @PropMeta(propId=2)
    
        public String getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(String value){
            this._employeeId = value;
        }


        private String _cityCode;

    
        @PropMeta(propId=3)
    
        public String getCityCode(){
            return _cityCode;
        }

        public void setCityCode(String value){
            this._cityCode = value;
        }


        private java.math.BigDecimal _socialInsuranceBase;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getSocialInsuranceBase(){
            return _socialInsuranceBase;
        }

        public void setSocialInsuranceBase(java.math.BigDecimal value){
            this._socialInsuranceBase = value;
        }


        private java.math.BigDecimal _housingFundBase;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getHousingFundBase(){
            return _housingFundBase;
        }

        public void setHousingFundBase(java.math.BigDecimal value){
            this._housingFundBase = value;
        }


        private java.time.LocalDate _effectiveFrom;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getEffectiveFrom(){
            return _effectiveFrom;
        }

        public void setEffectiveFrom(java.time.LocalDate value){
            this._effectiveFrom = value;
        }


        private java.time.LocalDate _effectiveTo;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getEffectiveTo(){
            return _effectiveTo;
        }

        public void setEffectiveTo(java.time.LocalDate value){
            this._effectiveTo = value;
        }


        private String _orgId;

    
        @PropMeta(propId=8)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
