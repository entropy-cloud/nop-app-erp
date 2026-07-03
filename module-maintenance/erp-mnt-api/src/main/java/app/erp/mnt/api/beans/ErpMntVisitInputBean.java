//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntVisitInputBean extends CrudInputBase {

    
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


        private Long _scheduleId;

    
        @PropMeta(propId=3)
    
        public Long getScheduleId(){
            return _scheduleId;
        }

        public void setScheduleId(Long value){
            this._scheduleId = value;
        }


        private Long _equipmentId;

    
        @PropMeta(propId=4)
    
        public Long getEquipmentId(){
            return _equipmentId;
        }

        public void setEquipmentId(Long value){
            this._equipmentId = value;
        }


        private java.time.LocalDate _visitDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getVisitDate(){
            return _visitDate;
        }

        public void setVisitDate(java.time.LocalDate value){
            this._visitDate = value;
        }


        private String _status;

    
        @PropMeta(propId=6)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _assignedTo;

    
        @PropMeta(propId=7)
    
        public Long getAssignedTo(){
            return _assignedTo;
        }

        public void setAssignedTo(Long value){
            this._assignedTo = value;
        }


        private Long _completedBy;

    
        @PropMeta(propId=8)
    
        public Long getCompletedBy(){
            return _completedBy;
        }

        public void setCompletedBy(Long value){
            this._completedBy = value;
        }


        private java.time.LocalDateTime _completedAt;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDateTime getCompletedAt(){
            return _completedAt;
        }

        public void setCompletedAt(java.time.LocalDateTime value){
            this._completedAt = value;
        }


        private java.time.LocalDateTime _startTime;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDateTime getStartTime(){
            return _startTime;
        }

        public void setStartTime(java.time.LocalDateTime value){
            this._startTime = value;
        }


        private java.time.LocalDateTime _endTime;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDateTime getEndTime(){
            return _endTime;
        }

        public void setEndTime(java.time.LocalDateTime value){
            this._endTime = value;
        }


        private java.math.BigDecimal _totalMinutes;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getTotalMinutes(){
            return _totalMinutes;
        }

        public void setTotalMinutes(java.math.BigDecimal value){
            this._totalMinutes = value;
        }


        private String _visitType;

    
        @PropMeta(propId=13)
    
        public String getVisitType(){
            return _visitType;
        }

        public void setVisitType(String value){
            this._visitType = value;
        }


        private String _result;

    
        @PropMeta(propId=14)
    
        public String getResult(){
            return _result;
        }

        public void setResult(String value){
            this._result = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=22)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=23)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=24)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.sql.Timestamp _postedAt;

    
        @PropMeta(propId=25)
    
        public java.sql.Timestamp getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.sql.Timestamp value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=26)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private List<ErpMntVisitTaskInputBean> _tasks;

        public List<ErpMntVisitTaskInputBean> getTasks(){
            return _tasks;
        }

        public void setTasks(List<ErpMntVisitTaskInputBean> value){
            this._tasks = value;
        }


        private List<ErpMntSparePartUsageInputBean> _sparePartUsages;

        public List<ErpMntSparePartUsageInputBean> getSparePartUsages(){
            return _sparePartUsages;
        }

        public void setSparePartUsages(List<ErpMntSparePartUsageInputBean> value){
            this._sparePartUsages = value;
        }


    }
