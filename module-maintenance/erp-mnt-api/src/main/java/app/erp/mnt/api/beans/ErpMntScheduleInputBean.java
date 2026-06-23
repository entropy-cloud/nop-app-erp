//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntScheduleInputBean extends CrudInputBase {

    
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


        private Integer _scheduleType;

    
        @PropMeta(propId=5)
    
        public Integer getScheduleType(){
            return _scheduleType;
        }

        public void setScheduleType(Integer value){
            this._scheduleType = value;
        }


        private Integer _frequency;

    
        @PropMeta(propId=6)
    
        public Integer getFrequency(){
            return _frequency;
        }

        public void setFrequency(Integer value){
            this._frequency = value;
        }


        private Integer _recurrenceType;

    
        @PropMeta(propId=7)
    
        public Integer getRecurrenceType(){
            return _recurrenceType;
        }

        public void setRecurrenceType(Integer value){
            this._recurrenceType = value;
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


    }
