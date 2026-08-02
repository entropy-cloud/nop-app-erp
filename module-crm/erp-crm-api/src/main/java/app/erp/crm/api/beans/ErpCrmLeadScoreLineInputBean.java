//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadScoreLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _scoreId;

    
        @PropMeta(propId=2)
    
        public Long getScoreId(){
            return _scoreId;
        }

        public void setScoreId(Long value){
            this._scoreId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _configLineId;

    
        @PropMeta(propId=4)
    
        public Long getConfigLineId(){
            return _configLineId;
        }

        public void setConfigLineId(Long value){
            this._configLineId = value;
        }


        private String _criterionCode;

    
        @PropMeta(propId=5)
    
        public String getCriterionCode(){
            return _criterionCode;
        }

        public void setCriterionCode(String value){
            this._criterionCode = value;
        }


        private String _criterionName;

    
        @PropMeta(propId=6)
    
        public String getCriterionName(){
            return _criterionName;
        }

        public void setCriterionName(String value){
            this._criterionName = value;
        }


        private String _rawValue;

    
        @PropMeta(propId=7)
    
        public String getRawValue(){
            return _rawValue;
        }

        public void setRawValue(String value){
            this._rawValue = value;
        }


        private String _lookupValue;

    
        @PropMeta(propId=8)
    
        public String getLookupValue(){
            return _lookupValue;
        }

        public void setLookupValue(String value){
            this._lookupValue = value;
        }


        private Integer _rawScore;

    
        @PropMeta(propId=9)
    
        public Integer getRawScore(){
            return _rawScore;
        }

        public void setRawScore(Integer value){
            this._rawScore = value;
        }


        private Integer _weightedScore;

    
        @PropMeta(propId=10)
    
        public Integer getWeightedScore(){
            return _weightedScore;
        }

        public void setWeightedScore(Integer value){
            this._weightedScore = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=11)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
        }


    }
