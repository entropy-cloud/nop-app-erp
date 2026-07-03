//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgWorkcenterInputBean extends CrudInputBase {

    
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


        private java.math.BigDecimal _capacity;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getCapacity(){
            return _capacity;
        }

        public void setCapacity(java.math.BigDecimal value){
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


        private java.math.BigDecimal _hourlyRate;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getHourlyRate(){
            return _hourlyRate;
        }

        public void setHourlyRate(java.math.BigDecimal value){
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


    }
