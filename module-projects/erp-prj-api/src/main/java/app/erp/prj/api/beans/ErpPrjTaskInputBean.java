//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjTaskInputBean extends CrudInputBase {

    
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


        private String _priority;

    
        @PropMeta(propId=14)
    
        public String getPriority(){
            return _priority;
        }

        public void setPriority(String value){
            this._priority = value;
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


        private List<ErpPrjTaskInputBean> _childTasks;

        public List<ErpPrjTaskInputBean> getChildTasks(){
            return _childTasks;
        }

        public void setChildTasks(List<ErpPrjTaskInputBean> value){
            this._childTasks = value;
        }


        private List<ErpPrjTimesheetInputBean> _timesheets;

        public List<ErpPrjTimesheetInputBean> getTimesheets(){
            return _timesheets;
        }

        public void setTimesheets(List<ErpPrjTimesheetInputBean> value){
            this._timesheets = value;
        }


    }
