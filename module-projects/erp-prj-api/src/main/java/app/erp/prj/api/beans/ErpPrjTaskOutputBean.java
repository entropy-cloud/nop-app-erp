//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjTaskOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=2)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private Long _parentTaskId;

    
        @PropMeta(propId=3)
    
        public Long getParentTaskId(){
            return _parentTaskId;
        }

        public void setParentTaskId(Long value){
            this._parentTaskId = value;
        }


        private String _title;

    
        @PropMeta(propId=4)
    
        public String getTitle(){
            return _title;
        }

        public void setTitle(String value){
            this._title = value;
        }


        private Long _assigneeId;

    
        @PropMeta(propId=5)
    
        public Long getAssigneeId(){
            return _assigneeId;
        }

        public void setAssigneeId(Long value){
            this._assigneeId = value;
        }


        private java.time.LocalDate _plannedStartDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getPlannedStartDate(){
            return _plannedStartDate;
        }

        public void setPlannedStartDate(java.time.LocalDate value){
            this._plannedStartDate = value;
        }


        private java.time.LocalDate _plannedEndDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getPlannedEndDate(){
            return _plannedEndDate;
        }

        public void setPlannedEndDate(java.time.LocalDate value){
            this._plannedEndDate = value;
        }


        private java.time.LocalDate _actualStartDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getActualStartDate(){
            return _actualStartDate;
        }

        public void setActualStartDate(java.time.LocalDate value){
            this._actualStartDate = value;
        }


        private java.time.LocalDate _actualEndDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getActualEndDate(){
            return _actualEndDate;
        }

        public void setActualEndDate(java.time.LocalDate value){
            this._actualEndDate = value;
        }


        private java.math.BigDecimal _estimatedHours;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getEstimatedHours(){
            return _estimatedHours;
        }

        public void setEstimatedHours(java.math.BigDecimal value){
            this._estimatedHours = value;
        }


        private java.math.BigDecimal _actualHours;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getActualHours(){
            return _actualHours;
        }

        public void setActualHours(java.math.BigDecimal value){
            this._actualHours = value;
        }


        private Long _dependsOnId;

    
        @PropMeta(propId=12)
    
        public Long getDependsOnId(){
            return _dependsOnId;
        }

        public void setDependsOnId(Long value){
            this._dependsOnId = value;
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


        private String _priority;

    
        @PropMeta(propId=14)
    
        public String getPriority(){
            return _priority;
        }

        public void setPriority(String value){
            this._priority = value;
        }


        private String _priority_label;

    
        public String getPriority_label(){
            return _priority_label;
        }

        public void setPriority_label(String value){
            this._priority_label = value;
        }


        private String _blockReason;

    
        @PropMeta(propId=15)
    
        public String getBlockReason(){
            return _blockReason;
        }

        public void setBlockReason(String value){
            this._blockReason = value;
        }


        private Integer _sortNum;

    
        @PropMeta(propId=16)
    
        public Integer getSortNum(){
            return _sortNum;
        }

        public void setSortNum(Integer value){
            this._sortNum = value;
        }


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=18)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=19)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=20)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=22)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _projectName;

    
        public String getProjectName(){
            return _projectName;
        }

        public void setProjectName(String value){
            this._projectName = value;
        }


        private String _parentTaskName;

    
        public String getParentTaskName(){
            return _parentTaskName;
        }

        public void setParentTaskName(String value){
            this._parentTaskName = value;
        }


        private String _assigneeName;

    
        public String getAssigneeName(){
            return _assigneeName;
        }

        public void setAssigneeName(String value){
            this._assigneeName = value;
        }


        private String _dependsOnTaskName;

    
        public String getDependsOnTaskName(){
            return _dependsOnTaskName;
        }

        public void setDependsOnTaskName(String value){
            this._dependsOnTaskName = value;
        }


        private Map<String,Object> _project;

        public Map<String,Object> getProject(){
            return _project;
        }

        public void setProject(Map<String,Object> value){
            this._project = value;
        }


        private Map<String,Object> _parentTask;

        public Map<String,Object> getParentTask(){
            return _parentTask;
        }

        public void setParentTask(Map<String,Object> value){
            this._parentTask = value;
        }


        private Map<String,Object> _assignee;

        public Map<String,Object> getAssignee(){
            return _assignee;
        }

        public void setAssignee(Map<String,Object> value){
            this._assignee = value;
        }


        private Map<String,Object> _dependsOn;

        public Map<String,Object> getDependsOn(){
            return _dependsOn;
        }

        public void setDependsOn(Map<String,Object> value){
            this._dependsOn = value;
        }


        private List<Map<String,Object>> _childTasks;

        public List<Map<String,Object>> getChildTasks(){
            return _childTasks;
        }

        public void setChildTasks(List<Map<String,Object>> value){
            this._childTasks = value;
        }


        private List<Map<String,Object>> _timesheets;

        public List<Map<String,Object>> getTimesheets(){
            return _timesheets;
        }

        public void setTimesheets(List<Map<String,Object>> value){
            this._timesheets = value;
        }


    }
