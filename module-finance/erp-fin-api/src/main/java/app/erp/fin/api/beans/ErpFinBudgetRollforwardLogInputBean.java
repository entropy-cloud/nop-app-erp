//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBudgetRollforwardLogInputBean extends CrudInputBase {

    
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


        private Long _scenarioId;

    
        @PropMeta(propId=3)
    
        public Long getScenarioId(){
            return _scenarioId;
        }

        public void setScenarioId(Long value){
            this._scenarioId = value;
        }


        private Long _sourceScenarioId;

    
        @PropMeta(propId=4)
    
        public Long getSourceScenarioId(){
            return _sourceScenarioId;
        }

        public void setSourceScenarioId(Long value){
            this._sourceScenarioId = value;
        }


        private Long _targetScenarioId;

    
        @PropMeta(propId=5)
    
        public Long getTargetScenarioId(){
            return _targetScenarioId;
        }

        public void setTargetScenarioId(Long value){
            this._targetScenarioId = value;
        }


        private String _strategy;

    
        @PropMeta(propId=6)
    
        public String getStrategy(){
            return _strategy;
        }

        public void setStrategy(String value){
            this._strategy = value;
        }


        private Integer _newFiscalYear;

    
        @PropMeta(propId=7)
    
        public Integer getNewFiscalYear(){
            return _newFiscalYear;
        }

        public void setNewFiscalYear(Integer value){
            this._newFiscalYear = value;
        }


        private java.math.BigDecimal _sourceAmount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getSourceAmount(){
            return _sourceAmount;
        }

        public void setSourceAmount(java.math.BigDecimal value){
            this._sourceAmount = value;
        }


        private java.math.BigDecimal _targetAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getTargetAmount(){
            return _targetAmount;
        }

        public void setTargetAmount(java.math.BigDecimal value){
            this._targetAmount = value;
        }


        private java.sql.Timestamp _rolledAt;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getRolledAt(){
            return _rolledAt;
        }

        public void setRolledAt(java.sql.Timestamp value){
            this._rolledAt = value;
        }


        private String _rolledBy;

    
        @PropMeta(propId=11)
    
        public String getRolledBy(){
            return _rolledBy;
        }

        public void setRolledBy(String value){
            this._rolledBy = value;
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
