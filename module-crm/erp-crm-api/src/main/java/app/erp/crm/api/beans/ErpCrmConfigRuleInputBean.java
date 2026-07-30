//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmConfigRuleInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _configuratorId;

    
        @PropMeta(propId=2)
    
        public Long getConfiguratorId(){
            return _configuratorId;
        }

        public void setConfiguratorId(Long value){
            this._configuratorId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _ruleType;

    
        @PropMeta(propId=4)
    
        public String getRuleType(){
            return _ruleType;
        }

        public void setRuleType(String value){
            this._ruleType = value;
        }


        private String _sourceFeatureCode;

    
        @PropMeta(propId=5)
    
        public String getSourceFeatureCode(){
            return _sourceFeatureCode;
        }

        public void setSourceFeatureCode(String value){
            this._sourceFeatureCode = value;
        }


        private String _sourceFeatureValue;

    
        @PropMeta(propId=6)
    
        public String getSourceFeatureValue(){
            return _sourceFeatureValue;
        }

        public void setSourceFeatureValue(String value){
            this._sourceFeatureValue = value;
        }


        private String _targetFeatureCode;

    
        @PropMeta(propId=7)
    
        public String getTargetFeatureCode(){
            return _targetFeatureCode;
        }

        public void setTargetFeatureCode(String value){
            this._targetFeatureCode = value;
        }


        private String _targetFeatureValue;

    
        @PropMeta(propId=8)
    
        public String getTargetFeatureValue(){
            return _targetFeatureValue;
        }

        public void setTargetFeatureValue(String value){
            this._targetFeatureValue = value;
        }


        private String _conditionExpression;

    
        @PropMeta(propId=9)
    
        public String getConditionExpression(){
            return _conditionExpression;
        }

        public void setConditionExpression(String value){
            this._conditionExpression = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=10)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
