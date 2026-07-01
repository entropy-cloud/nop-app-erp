//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstAssetOutputBean {

    
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


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _categoryId;

    
        @PropMeta(propId=5)
    
        public Long getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(Long value){
            this._categoryId = value;
        }


        private java.time.LocalDate _acquisitionDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getAcquisitionDate(){
            return _acquisitionDate;
        }

        public void setAcquisitionDate(java.time.LocalDate value){
            this._acquisitionDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=7)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _originalValue;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getOriginalValue(){
            return _originalValue;
        }

        public void setOriginalValue(java.math.BigDecimal value){
            this._originalValue = value;
        }


        private java.math.BigDecimal _currentValue;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getCurrentValue(){
            return _currentValue;
        }

        public void setCurrentValue(java.math.BigDecimal value){
            this._currentValue = value;
        }


        private java.math.BigDecimal _residualValue;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getResidualValue(){
            return _residualValue;
        }

        public void setResidualValue(java.math.BigDecimal value){
            this._residualValue = value;
        }


        private Integer _depreciationMethod;

    
        @PropMeta(propId=11)
    
        public Integer getDepreciationMethod(){
            return _depreciationMethod;
        }

        public void setDepreciationMethod(Integer value){
            this._depreciationMethod = value;
        }


        private String _depreciationMethod_label;

    
        public String getDepreciationMethod_label(){
            return _depreciationMethod_label;
        }

        public void setDepreciationMethod_label(String value){
            this._depreciationMethod_label = value;
        }


        private java.math.BigDecimal _depreciationRate;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getDepreciationRate(){
            return _depreciationRate;
        }

        public void setDepreciationRate(java.math.BigDecimal value){
            this._depreciationRate = value;
        }


        private Integer _usefulLifeMonths;

    
        @PropMeta(propId=13)
    
        public Integer getUsefulLifeMonths(){
            return _usefulLifeMonths;
        }

        public void setUsefulLifeMonths(Integer value){
            this._usefulLifeMonths = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=14)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
            this._departmentId = value;
        }


        private Long _locationId;

    
        @PropMeta(propId=15)
    
        public Long getLocationId(){
            return _locationId;
        }

        public void setLocationId(Long value){
            this._locationId = value;
        }


        private Long _employeeId;

    
        @PropMeta(propId=16)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private Long _staffId;

    
        @PropMeta(propId=17)
    
        public Long getStaffId(){
            return _staffId;
        }

        public void setStaffId(Long value){
            this._staffId = value;
        }


        private String _brandModel;

    
        @PropMeta(propId=18)
    
        public String getBrandModel(){
            return _brandModel;
        }

        public void setBrandModel(String value){
            this._brandModel = value;
        }


        private Integer _status;

    
        @PropMeta(propId=19)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=20)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=21)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=22)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=24)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=25)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=26)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Map<String,Object> _category;

        public Map<String,Object> getCategory(){
            return _category;
        }

        public void setCategory(Map<String,Object> value){
            this._category = value;
        }


        private Map<String,Object> _department;

        public Map<String,Object> getDepartment(){
            return _department;
        }

        public void setDepartment(Map<String,Object> value){
            this._department = value;
        }


        private Map<String,Object> _location;

        public Map<String,Object> getLocation(){
            return _location;
        }

        public void setLocation(Map<String,Object> value){
            this._location = value;
        }


        private Map<String,Object> _employee;

        public Map<String,Object> getEmployee(){
            return _employee;
        }

        public void setEmployee(Map<String,Object> value){
            this._employee = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _staff;

        public Map<String,Object> getStaff(){
            return _staff;
        }

        public void setStaff(Map<String,Object> value){
            this._staff = value;
        }


    }
