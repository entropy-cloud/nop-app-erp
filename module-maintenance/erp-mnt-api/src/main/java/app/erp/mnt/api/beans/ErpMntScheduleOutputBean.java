//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntScheduleOutputBean {

    
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


        private Long _equipmentId;

    
        @PropMeta(propId=4)
    
        public Long getEquipmentId(){
            return _equipmentId;
        }

        public void setEquipmentId(Long value){
            this._equipmentId = value;
        }


        private String _scheduleType;

    
        @PropMeta(propId=5)
    
        public String getScheduleType(){
            return _scheduleType;
        }

        public void setScheduleType(String value){
            this._scheduleType = value;
        }


        private String _scheduleType_label;

    
        public String getScheduleType_label(){
            return _scheduleType_label;
        }

        public void setScheduleType_label(String value){
            this._scheduleType_label = value;
        }


        private Integer _frequency;

    
        @PropMeta(propId=6)
    
        public Integer getFrequency(){
            return _frequency;
        }

        public void setFrequency(Integer value){
            this._frequency = value;
        }


        private String _recurrenceType;

    
        @PropMeta(propId=7)
    
        public String getRecurrenceType(){
            return _recurrenceType;
        }

        public void setRecurrenceType(String value){
            this._recurrenceType = value;
        }


        private String _recurrenceType_label;

    
        public String getRecurrenceType_label(){
            return _recurrenceType_label;
        }

        public void setRecurrenceType_label(String value){
            this._recurrenceType_label = value;
        }


        private String _daysOfWeek;

    
        @PropMeta(propId=8)
    
        public String getDaysOfWeek(){
            return _daysOfWeek;
        }

        public void setDaysOfWeek(String value){
            this._daysOfWeek = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private java.time.LocalDate _nextDueDate;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getNextDueDate(){
            return _nextDueDate;
        }

        public void setNextDueDate(java.time.LocalDate value){
            this._nextDueDate = value;
        }


        private Integer _isActive;

    
        @PropMeta(propId=12)
    
        public Integer getIsActive(){
            return _isActive;
        }

        public void setIsActive(Integer value){
            this._isActive = value;
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


        private Map<String,Object> _equipment;

        public Map<String,Object> getEquipment(){
            return _equipment;
        }

        public void setEquipment(Map<String,Object> value){
            this._equipment = value;
        }


    }
