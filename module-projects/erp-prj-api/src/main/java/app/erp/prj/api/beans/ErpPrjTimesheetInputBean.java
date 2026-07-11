//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjTimesheetInputBean extends CrudInputBase {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=4)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private Long _taskId;

    
        @PropMeta(propId=5)
    
        public Long getTaskId(){
            return _taskId;
        }

        public void setTaskId(Long value){
            this._taskId = value;
        }


        private Long _userId;

    
        @PropMeta(propId=6)
    
        public Long getUserId(){
            return _userId;
        }

        public void setUserId(Long value){
            this._userId = value;
        }


        private java.time.LocalDate _workDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getWorkDate(){
            return _workDate;
        }

        public void setWorkDate(java.time.LocalDate value){
            this._workDate = value;
        }


        private java.math.BigDecimal _hours;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getHours(){
            return _hours;
        }

        public void setHours(java.math.BigDecimal value){
            this._hours = value;
        }


        private Long _activityTypeId;

    
        @PropMeta(propId=9)
    
        public Long getActivityTypeId(){
            return _activityTypeId;
        }

        public void setActivityTypeId(Long value){
            this._activityTypeId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=10)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _costRate;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getCostRate(){
            return _costRate;
        }

        public void setCostRate(java.math.BigDecimal value){
            this._costRate = value;
        }


        private java.math.BigDecimal _costAmount;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getCostAmount(){
            return _costAmount;
        }

        public void setCostAmount(java.math.BigDecimal value){
            this._costAmount = value;
        }


        private String _status;

    
        @PropMeta(propId=13)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=19)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
