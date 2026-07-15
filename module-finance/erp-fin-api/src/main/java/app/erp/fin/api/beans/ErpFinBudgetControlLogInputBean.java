//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBudgetControlLogInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=3)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Long _scenarioId;

    
        @PropMeta(propId=4)
    
        public Long getScenarioId(){
            return _scenarioId;
        }

        public void setScenarioId(Long value){
            this._scenarioId = value;
        }


        private Long _budgetLineId;

    
        @PropMeta(propId=5)
    
        public Long getBudgetLineId(){
            return _budgetLineId;
        }

        public void setBudgetLineId(Long value){
            this._budgetLineId = value;
        }


        private String _sourceBillType;

    
        @PropMeta(propId=6)
    
        public String getSourceBillType(){
            return _sourceBillType;
        }

        public void setSourceBillType(String value){
            this._sourceBillType = value;
        }


        private String _sourceBillCode;

    
        @PropMeta(propId=7)
    
        public String getSourceBillCode(){
            return _sourceBillCode;
        }

        public void setSourceBillCode(String value){
            this._sourceBillCode = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=8)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
            this._subjectId = value;
        }


        private Long _costCenterId;

    
        @PropMeta(propId=9)
    
        public Long getCostCenterId(){
            return _costCenterId;
        }

        public void setCostCenterId(Long value){
            this._costCenterId = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=10)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private Long _periodId;

    
        @PropMeta(propId=11)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private java.math.BigDecimal _requestedAmount;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getRequestedAmount(){
            return _requestedAmount;
        }

        public void setRequestedAmount(java.math.BigDecimal value){
            this._requestedAmount = value;
        }


        private java.math.BigDecimal _committedAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getCommittedAmount(){
            return _committedAmount;
        }

        public void setCommittedAmount(java.math.BigDecimal value){
            this._committedAmount = value;
        }


        private java.math.BigDecimal _availableAmount;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getAvailableAmount(){
            return _availableAmount;
        }

        public void setAvailableAmount(java.math.BigDecimal value){
            this._availableAmount = value;
        }


        private String _actionResult;

    
        @PropMeta(propId=15)
    
        public String getActionResult(){
            return _actionResult;
        }

        public void setActionResult(String value){
            this._actionResult = value;
        }


        private String _operatorId;

    
        @PropMeta(propId=16)
    
        public String getOperatorId(){
            return _operatorId;
        }

        public void setOperatorId(String value){
            this._operatorId = value;
        }


        private java.sql.Timestamp _operatedAt;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getOperatedAt(){
            return _operatedAt;
        }

        public void setOperatedAt(java.sql.Timestamp value){
            this._operatedAt = value;
        }


        private String _reason;

    
        @PropMeta(propId=18)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


    }
