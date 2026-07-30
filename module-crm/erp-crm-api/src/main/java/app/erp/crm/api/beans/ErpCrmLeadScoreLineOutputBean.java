//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadScoreLineOutputBean {

    
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


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=13)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=14)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=16)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _score;

        public Map<String,Object> getScore(){
            return _score;
        }

        public void setScore(Map<String,Object> value){
            this._score = value;
        }


        private Map<String,Object> _configLine;

        public Map<String,Object> getConfigLine(){
            return _configLine;
        }

        public void setConfigLine(Map<String,Object> value){
            this._configLine = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
