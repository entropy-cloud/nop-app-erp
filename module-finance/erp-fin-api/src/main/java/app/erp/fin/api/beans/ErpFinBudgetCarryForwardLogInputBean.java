//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBudgetCarryForwardLogInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _orgId;

    
        @PropMeta(propId=2)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _scenarioId;

    
        @PropMeta(propId=3)
    
        public String getScenarioId(){
            return _scenarioId;
        }

        public void setScenarioId(String value){
            this._scenarioId = value;
        }


        private String _sourceScenarioId;

    
        @PropMeta(propId=4)
    
        public String getSourceScenarioId(){
            return _sourceScenarioId;
        }

        public void setSourceScenarioId(String value){
            this._sourceScenarioId = value;
        }


        private String _targetScenarioId;

    
        @PropMeta(propId=5)
    
        public String getTargetScenarioId(){
            return _targetScenarioId;
        }

        public void setTargetScenarioId(String value){
            this._targetScenarioId = value;
        }


        private String _rule;

    
        @PropMeta(propId=6)
    
        public String getRule(){
            return _rule;
        }

        public void setRule(String value){
            this._rule = value;
        }


        private java.math.BigDecimal _sourceRemaining;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getSourceRemaining(){
            return _sourceRemaining;
        }

        public void setSourceRemaining(java.math.BigDecimal value){
            this._sourceRemaining = value;
        }


        private java.math.BigDecimal _sourceUsed;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getSourceUsed(){
            return _sourceUsed;
        }

        public void setSourceUsed(java.math.BigDecimal value){
            this._sourceUsed = value;
        }


        private java.math.BigDecimal _carriedAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getCarriedAmount(){
            return _carriedAmount;
        }

        public void setCarriedAmount(java.math.BigDecimal value){
            this._carriedAmount = value;
        }


        private java.sql.Timestamp _carriedAt;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getCarriedAt(){
            return _carriedAt;
        }

        public void setCarriedAt(java.sql.Timestamp value){
            this._carriedAt = value;
        }


        private String _carriedBy;

    
        @PropMeta(propId=11)
    
        public String getCarriedBy(){
            return _carriedBy;
        }

        public void setCarriedBy(String value){
            this._carriedBy = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
