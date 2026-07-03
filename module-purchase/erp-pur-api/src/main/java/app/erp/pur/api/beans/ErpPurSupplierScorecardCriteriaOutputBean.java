//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurSupplierScorecardCriteriaOutputBean {

    
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


        private Long _delVersion;

    
        @PropMeta(propId=8)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=9)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=10)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=12)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _scorecard;

        public Map<String,Object> getScorecard(){
            return _scorecard;
        }

        public void setScorecard(Map<String,Object> value){
            this._scorecard = value;
        }


        private List<Map<String,Object>> _variables;

        public List<Map<String,Object>> getVariables(){
            return _variables;
        }

        public void setVariables(List<Map<String,Object>> value){
            this._variables = value;
        }


    }
