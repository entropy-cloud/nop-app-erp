//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBudgetCarryForwardLogOutputBean {

    
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


        private String _rule;

    
        @PropMeta(propId=6)
    
        public String getRule(){
            return _rule;
        }

        public void setRule(String value){
            this._rule = value;
        }


        private String _rule_label;

    
        public String getRule_label(){
            return _rule_label;
        }

        public void setRule_label(String value){
            this._rule_label = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _scenario;

        public Map<String,Object> getScenario(){
            return _scenario;
        }

        public void setScenario(Map<String,Object> value){
            this._scenario = value;
        }


        private Map<String,Object> _sourceScenario;

        public Map<String,Object> getSourceScenario(){
            return _sourceScenario;
        }

        public void setSourceScenario(Map<String,Object> value){
            this._sourceScenario = value;
        }


        private Map<String,Object> _targetScenario;

        public Map<String,Object> getTargetScenario(){
            return _targetScenario;
        }

        public void setTargetScenario(Map<String,Object> value){
            this._targetScenario = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
