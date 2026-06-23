//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjBudgetLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _budgetId;

    
        @PropMeta(propId=2)
    
        public Long getBudgetId(){
            return _budgetId;
        }

        public void setBudgetId(Long value){
            this._budgetId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _costCategory;

    
        @PropMeta(propId=4)
    
        public String getCostCategory(){
            return _costCategory;
        }

        public void setCostCategory(String value){
            this._costCategory = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=5)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
            this._subjectId = value;
        }


        private Long _taskId;

    
        @PropMeta(propId=6)
    
        public Long getTaskId(){
            return _taskId;
        }

        public void setTaskId(Long value){
            this._taskId = value;
        }


        private String _plannedAmount;

    
        @PropMeta(propId=7)
    
        public String getPlannedAmount(){
            return _plannedAmount;
        }

        public void setPlannedAmount(String value){
            this._plannedAmount = value;
        }


        private String _committedAmount;

    
        @PropMeta(propId=8)
    
        public String getCommittedAmount(){
            return _committedAmount;
        }

        public void setCommittedAmount(String value){
            this._committedAmount = value;
        }


        private String _actualAmount;

    
        @PropMeta(propId=9)
    
        public String getActualAmount(){
            return _actualAmount;
        }

        public void setActualAmount(String value){
            this._actualAmount = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
