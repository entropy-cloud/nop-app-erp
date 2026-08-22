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

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _projectId;

    
        @PropMeta(propId=4)
    
        public String getProjectId(){
            return _projectId;
        }

        public void setProjectId(String value){
            this._projectId = value;
        }


        private String _taskId;

    
        @PropMeta(propId=5)
    
        public String getTaskId(){
            return _taskId;
        }

        public void setTaskId(String value){
            this._taskId = value;
        }


        private String _userId;

    
        @PropMeta(propId=6)
    
        public String getUserId(){
            return _userId;
        }

        public void setUserId(String value){
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


        private String _activityTypeId;

    
        @PropMeta(propId=9)
    
        public String getActivityTypeId(){
            return _activityTypeId;
        }

        public void setActivityTypeId(String value){
            this._activityTypeId = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=10)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
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
