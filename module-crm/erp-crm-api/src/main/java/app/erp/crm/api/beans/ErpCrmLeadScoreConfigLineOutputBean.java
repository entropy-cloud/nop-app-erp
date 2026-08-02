//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadScoreConfigLineOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _configId;

    
        @PropMeta(propId=2)
    
        public Long getConfigId(){
            return _configId;
        }

        public void setConfigId(Long value){
            this._configId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _criterionCode;

    
        @PropMeta(propId=4)
    
        public String getCriterionCode(){
            return _criterionCode;
        }

        public void setCriterionCode(String value){
            this._criterionCode = value;
        }


        private String _criterionName;

    
        @PropMeta(propId=5)
    
        public String getCriterionName(){
            return _criterionName;
        }

        public void setCriterionName(String value){
            this._criterionName = value;
        }


        private Integer _weight;

    
        @PropMeta(propId=6)
    
        public Integer getWeight(){
            return _weight;
        }

        public void setWeight(Integer value){
            this._weight = value;
        }


        private String _scoringMethod;

    
        @PropMeta(propId=7)
    
        public String getScoringMethod(){
            return _scoringMethod;
        }

        public void setScoringMethod(String value){
            this._scoringMethod = value;
        }


        private String _scoringMethod_label;

    
        public String getScoringMethod_label(){
            return _scoringMethod_label;
        }

        public void setScoringMethod_label(String value){
            this._scoringMethod_label = value;
        }


        private String _lookupTable;

    
        @PropMeta(propId=8)
    
        public String getLookupTable(){
            return _lookupTable;
        }

        public void setLookupTable(String value){
            this._lookupTable = value;
        }


        private String _formula;

    
        @PropMeta(propId=9)
    
        public String getFormula(){
            return _formula;
        }

        public void setFormula(String value){
            this._formula = value;
        }


        private Integer _maxScore;

    
        @PropMeta(propId=10)
    
        public Integer getMaxScore(){
            return _maxScore;
        }

        public void setMaxScore(Integer value){
            this._maxScore = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=11)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
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


        private Map<String,Object> _config;

        public Map<String,Object> getConfig(){
            return _config;
        }

        public void setConfig(Map<String,Object> value){
            this._config = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
