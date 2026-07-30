//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrTimesheetLineInputBean extends CrudInputBase {

    
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


    }
