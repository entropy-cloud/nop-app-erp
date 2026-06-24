//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstAssetInputBean extends CrudInputBase {

    
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


        private Long _delVersion;

    
        @PropMeta(propId=20)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private String _remark;

    
        @PropMeta(propId=26)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
