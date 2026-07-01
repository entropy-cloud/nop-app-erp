//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgJobCardTimeLogOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _jobCardId;

    
        @PropMeta(propId=2)
    
        public Long getJobCardId(){
            return _jobCardId;
        }

        public void setJobCardId(Long value){
            this._jobCardId = value;
        }


        private Long _workOrderId;

    
        @PropMeta(propId=3)
    
        public Long getWorkOrderId(){
            return _workOrderId;
        }

        public void setWorkOrderId(Long value){
            this._workOrderId = value;
        }


        private String _operatorId;

    
        @PropMeta(propId=4)
    
        public String getOperatorId(){
            return _operatorId;
        }

        public void setOperatorId(String value){
            this._operatorId = value;
        }


        private java.time.LocalDate _workDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getWorkDate(){
            return _workDate;
        }

        public void setWorkDate(java.time.LocalDate value){
            this._workDate = value;
        }


        private java.time.LocalDateTime _startTime;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDateTime getStartTime(){
            return _startTime;
        }

        public void setStartTime(java.time.LocalDateTime value){
            this._startTime = value;
        }


        private java.time.LocalDateTime _endTime;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDateTime getEndTime(){
            return _endTime;
        }

        public void setEndTime(java.time.LocalDateTime value){
            this._endTime = value;
        }


        private String _durationMins;

    
        @PropMeta(propId=8)
    
        public String getDurationMins(){
            return _durationMins;
        }

        public void setDurationMins(String value){
            this._durationMins = value;
        }


        private String _setupMins;

    
        @PropMeta(propId=9)
    
        public String getSetupMins(){
            return _setupMins;
        }

        public void setSetupMins(String value){
            this._setupMins = value;
        }


        private String _runMins;

    
        @PropMeta(propId=10)
    
        public String getRunMins(){
            return _runMins;
        }

        public void setRunMins(String value){
            this._runMins = value;
        }


        private java.math.BigDecimal _completedQuantity;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getCompletedQuantity(){
            return _completedQuantity;
        }

        public void setCompletedQuantity(java.math.BigDecimal value){
            this._completedQuantity = value;
        }


        private java.math.BigDecimal _scrappedQuantity;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getScrappedQuantity(){
            return _scrappedQuantity;
        }

        public void setScrappedQuantity(java.math.BigDecimal value){
            this._scrappedQuantity = value;
        }


        private String _hourlyRate;

    
        @PropMeta(propId=13)
    
        public String getHourlyRate(){
            return _hourlyRate;
        }

        public void setHourlyRate(String value){
            this._hourlyRate = value;
        }


        private java.math.BigDecimal _laborCost;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getLaborCost(){
            return _laborCost;
        }

        public void setLaborCost(java.math.BigDecimal value){
            this._laborCost = value;
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


        private Integer _version;

    
        @PropMeta(propId=17)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=18)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=20)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _jobCard;

        public Map<String,Object> getJobCard(){
            return _jobCard;
        }

        public void setJobCard(Map<String,Object> value){
            this._jobCard = value;
        }


        private Map<String,Object> _workOrder;

        public Map<String,Object> getWorkOrder(){
            return _workOrder;
        }

        public void setWorkOrder(Map<String,Object> value){
            this._workOrder = value;
        }


        private Map<String,Object> _operator;

        public Map<String,Object> getOperator(){
            return _operator;
        }

        public void setOperator(Map<String,Object> value){
            this._operator = value;
        }


    }
