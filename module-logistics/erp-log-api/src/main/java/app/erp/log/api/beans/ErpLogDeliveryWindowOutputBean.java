//__XGEN_FORCE_OVERRIDE__
    package app.erp.log.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpLogDeliveryWindowOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=2)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Integer _weekday;

    
        @PropMeta(propId=4)
    
        public Integer getWeekday(){
            return _weekday;
        }

        public void setWeekday(Integer value){
            this._weekday = value;
        }


        private String _startTime;

    
        @PropMeta(propId=5)
    
        public String getStartTime(){
            return _startTime;
        }

        public void setStartTime(String value){
            this._startTime = value;
        }


        private String _endTime;

    
        @PropMeta(propId=6)
    
        public String getEndTime(){
            return _endTime;
        }

        public void setEndTime(String value){
            this._endTime = value;
        }


        private Integer _maxCapacity;

    
        @PropMeta(propId=7)
    
        public Integer getMaxCapacity(){
            return _maxCapacity;
        }

        public void setMaxCapacity(Integer value){
            this._maxCapacity = value;
        }


        private Integer _currentBooked;

    
        @PropMeta(propId=8)
    
        public Integer getCurrentBooked(){
            return _currentBooked;
        }

        public void setCurrentBooked(Integer value){
            this._currentBooked = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=9)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private java.time.LocalDate _effectiveFrom;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getEffectiveFrom(){
            return _effectiveFrom;
        }

        public void setEffectiveFrom(java.time.LocalDate value){
            this._effectiveFrom = value;
        }


        private java.time.LocalDate _effectiveTo;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getEffectiveTo(){
            return _effectiveTo;
        }

        public void setEffectiveTo(java.time.LocalDate value){
            this._effectiveTo = value;
        }


        private String _allowedShipmentTypes;

    
        @PropMeta(propId=12)
    
        public String getAllowedShipmentTypes(){
            return _allowedShipmentTypes;
        }

        public void setAllowedShipmentTypes(String value){
            this._allowedShipmentTypes = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
