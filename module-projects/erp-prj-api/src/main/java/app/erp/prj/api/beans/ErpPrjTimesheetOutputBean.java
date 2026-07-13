//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjTimesheetOutputBean {

    
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


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=14)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=15)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=16)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=17)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=19)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=20)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=21)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=22)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=24)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=25)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _orgName;

    
        public String getOrgName(){
            return _orgName;
        }

        public void setOrgName(String value){
            this._orgName = value;
        }


        private String _projectName;

    
        public String getProjectName(){
            return _projectName;
        }

        public void setProjectName(String value){
            this._projectName = value;
        }


        private String _taskName;

    
        public String getTaskName(){
            return _taskName;
        }

        public void setTaskName(String value){
            this._taskName = value;
        }


        private String _userName;

    
        public String getUserName(){
            return _userName;
        }

        public void setUserName(String value){
            this._userName = value;
        }


        private String _activityTypeName;

    
        public String getActivityTypeName(){
            return _activityTypeName;
        }

        public void setActivityTypeName(String value){
            this._activityTypeName = value;
        }


        private String _currencyName;

    
        public String getCurrencyName(){
            return _currencyName;
        }

        public void setCurrencyName(String value){
            this._currencyName = value;
        }


        private Map<String,Object> _project;

        public Map<String,Object> getProject(){
            return _project;
        }

        public void setProject(Map<String,Object> value){
            this._project = value;
        }


        private Map<String,Object> _task;

        public Map<String,Object> getTask(){
            return _task;
        }

        public void setTask(Map<String,Object> value){
            this._task = value;
        }


        private Map<String,Object> _user;

        public Map<String,Object> getUser(){
            return _user;
        }

        public void setUser(Map<String,Object> value){
            this._user = value;
        }


        private Map<String,Object> _activityType;

        public Map<String,Object> getActivityType(){
            return _activityType;
        }

        public void setActivityType(Map<String,Object> value){
            this._activityType = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
