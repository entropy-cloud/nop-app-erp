//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgWorkcenterCalendarOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _workcenterId;

    
        @PropMeta(propId=2)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _calendarName;

    
        @PropMeta(propId=4)
    
        public String getCalendarName(){
            return _calendarName;
        }

        public void setCalendarName(String value){
            this._calendarName = value;
        }


        private Integer _shiftType;

    
        @PropMeta(propId=5)
    
        public Integer getShiftType(){
            return _shiftType;
        }

        public void setShiftType(Integer value){
            this._shiftType = value;
        }


        private String _shiftType_label;

    
        public String getShiftType_label(){
            return _shiftType_label;
        }

        public void setShiftType_label(String value){
            this._shiftType_label = value;
        }


        private Integer _workDatePattern;

    
        @PropMeta(propId=6)
    
        public Integer getWorkDatePattern(){
            return _workDatePattern;
        }

        public void setWorkDatePattern(Integer value){
            this._workDatePattern = value;
        }


        private String _workDatePattern_label;

    
        public String getWorkDatePattern_label(){
            return _workDatePattern_label;
        }

        public void setWorkDatePattern_label(String value){
            this._workDatePattern_label = value;
        }


        private String _startTime;

    
        @PropMeta(propId=7)
    
        public String getStartTime(){
            return _startTime;
        }

        public void setStartTime(String value){
            this._startTime = value;
        }


        private String _endTime;

    
        @PropMeta(propId=8)
    
        public String getEndTime(){
            return _endTime;
        }

        public void setEndTime(String value){
            this._endTime = value;
        }


        private java.time.LocalDate _effectiveFrom;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getEffectiveFrom(){
            return _effectiveFrom;
        }

        public void setEffectiveFrom(java.time.LocalDate value){
            this._effectiveFrom = value;
        }


        private java.time.LocalDate _effectiveTo;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getEffectiveTo(){
            return _effectiveTo;
        }

        public void setEffectiveTo(java.time.LocalDate value){
            this._effectiveTo = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=11)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
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


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _workcenter;

        public Map<String,Object> getWorkcenter(){
            return _workcenter;
        }

        public void setWorkcenter(Map<String,Object> value){
            this._workcenter = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
