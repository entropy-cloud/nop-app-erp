//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrTaxSpecialDeductionOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _employeeId;

    
        @PropMeta(propId=2)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private Integer _year;

    
        @PropMeta(propId=3)
    
        public Integer getYear(){
            return _year;
        }

        public void setYear(Integer value){
            this._year = value;
        }


        private Integer _month;

    
        @PropMeta(propId=4)
    
        public Integer getMonth(){
            return _month;
        }

        public void setMonth(Integer value){
            this._month = value;
        }


        private String _deductionType;

    
        @PropMeta(propId=5)
    
        public String getDeductionType(){
            return _deductionType;
        }

        public void setDeductionType(String value){
            this._deductionType = value;
        }


        private String _deductionType_label;

    
        public String getDeductionType_label(){
            return _deductionType_label;
        }

        public void setDeductionType_label(String value){
            this._deductionType_label = value;
        }


        private java.math.BigDecimal _monthlyAmount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getMonthlyAmount(){
            return _monthlyAmount;
        }

        public void setMonthlyAmount(java.math.BigDecimal value){
            this._monthlyAmount = value;
        }


        private Boolean _verified;

    
        @PropMeta(propId=7)
    
        public Boolean getVerified(){
            return _verified;
        }

        public void setVerified(Boolean value){
            this._verified = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=8)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
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


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=11)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=12)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=14)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _employee;

        public Map<String,Object> getEmployee(){
            return _employee;
        }

        public void setEmployee(Map<String,Object> value){
            this._employee = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
