//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSocialInsuranceConfigOutputBean {

    
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


        private String _cityCode_label;

    
        public String getCityCode_label(){
            return _cityCode_label;
        }

        public void setCityCode_label(String value){
            this._cityCode_label = value;
        }


        private String _insuranceType;

    
        @PropMeta(propId=3)
    
        public String getInsuranceType(){
            return _insuranceType;
        }

        public void setInsuranceType(String value){
            this._insuranceType = value;
        }


        private String _insuranceType_label;

    
        public String getInsuranceType_label(){
            return _insuranceType_label;
        }

        public void setInsuranceType_label(String value){
            this._insuranceType_label = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=13)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=14)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=16)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
