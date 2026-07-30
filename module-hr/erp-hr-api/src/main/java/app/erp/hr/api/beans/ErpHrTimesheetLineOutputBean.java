//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrTimesheetLineOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _timesheetId;

    
        @PropMeta(propId=2)
    
        public Long getTimesheetId(){
            return _timesheetId;
        }

        public void setTimesheetId(Long value){
            this._timesheetId = value;
        }


        private Long _employeeId;

    
        @PropMeta(propId=3)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private java.time.LocalDate _workDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getWorkDate(){
            return _workDate;
        }

        public void setWorkDate(java.time.LocalDate value){
            this._workDate = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=5)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private Long _taskId;

    
        @PropMeta(propId=6)
    
        public Long getTaskId(){
            return _taskId;
        }

        public void setTaskId(Long value){
            this._taskId = value;
        }


        private String _activityType;

    
        @PropMeta(propId=7)
    
        public String getActivityType(){
            return _activityType;
        }

        public void setActivityType(String value){
            this._activityType = value;
        }


        private java.math.BigDecimal _hours;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getHours(){
            return _hours;
        }

        public void setHours(java.math.BigDecimal value){
            this._hours = value;
        }


        private String _description;

    
        @PropMeta(propId=9)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=11)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=12)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=14)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _timesheet;

        public Map<String,Object> getTimesheet(){
            return _timesheet;
        }

        public void setTimesheet(Map<String,Object> value){
            this._timesheet = value;
        }


        private Map<String,Object> _employee;

        public Map<String,Object> getEmployee(){
            return _employee;
        }

        public void setEmployee(Map<String,Object> value){
            this._employee = value;
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


    }
