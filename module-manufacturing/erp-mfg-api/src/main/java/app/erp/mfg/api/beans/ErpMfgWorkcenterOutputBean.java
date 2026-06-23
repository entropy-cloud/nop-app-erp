//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgWorkcenterOutputBean {

    
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


        private String _capacity;

    
        @PropMeta(propId=4)
    
        public String getCapacity(){
            return _capacity;
        }

        public void setCapacity(String value){
            this._capacity = value;
        }


        private String _capacityUnit;

    
        @PropMeta(propId=5)
    
        public String getCapacityUnit(){
            return _capacityUnit;
        }

        public void setCapacityUnit(String value){
            this._capacityUnit = value;
        }


        private String _hourlyRate;

    
        @PropMeta(propId=6)
    
        public String getHourlyRate(){
            return _hourlyRate;
        }

        public void setHourlyRate(String value){
            this._hourlyRate = value;
        }


        private java.math.BigDecimal _workHoursPerDay;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getWorkHoursPerDay(){
            return _workHoursPerDay;
        }

        public void setWorkHoursPerDay(java.math.BigDecimal value){
            this._workHoursPerDay = value;
        }


        private Boolean _isExternal;

    
        @PropMeta(propId=8)
    
        public Boolean getIsExternal(){
            return _isExternal;
        }

        public void setIsExternal(Boolean value){
            this._isExternal = value;
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


    }
