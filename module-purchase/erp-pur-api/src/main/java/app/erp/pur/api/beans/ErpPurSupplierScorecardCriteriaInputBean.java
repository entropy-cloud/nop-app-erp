//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurSupplierScorecardCriteriaInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _scorecardId;

    
        @PropMeta(propId=2)
    
        public Long getScorecardId(){
            return _scorecardId;
        }

        public void setScorecardId(Long value){
            this._scorecardId = value;
        }


        private String _criteriaName;

    
        @PropMeta(propId=3)
    
        public String getCriteriaName(){
            return _criteriaName;
        }

        public void setCriteriaName(String value){
            this._criteriaName = value;
        }


        private java.math.BigDecimal _weight;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getWeight(){
            return _weight;
        }

        public void setWeight(java.math.BigDecimal value){
            this._weight = value;
        }


        private String _formula;

    
        @PropMeta(propId=5)
    
        public String getFormula(){
            return _formula;
        }

        public void setFormula(String value){
            this._formula = value;
        }


        private java.math.BigDecimal _score;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getScore(){
            return _score;
        }

        public void setScore(java.math.BigDecimal value){
            this._score = value;
        }


        private java.math.BigDecimal _weightedScore;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getWeightedScore(){
            return _weightedScore;
        }

        public void setWeightedScore(java.math.BigDecimal value){
            this._weightedScore = value;
        }


        private List<ErpPurSupplierScorecardVariableInputBean> _variables;

        public List<ErpPurSupplierScorecardVariableInputBean> getVariables(){
            return _variables;
        }

        public void setVariables(List<ErpPurSupplierScorecardVariableInputBean> value){
            this._variables = value;
        }


    }
