//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmForecastLineOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _forecastId;

    
        @PropMeta(propId=2)
    
        public Long getForecastId(){
            return _forecastId;
        }

        public void setForecastId(Long value){
            this._forecastId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _leadId;

    
        @PropMeta(propId=4)
    
        public Long getLeadId(){
            return _leadId;
        }

        public void setLeadId(Long value){
            this._leadId = value;
        }


        private Integer _probability;

    
        @PropMeta(propId=5)
    
        public Integer getProbability(){
            return _probability;
        }

        public void setProbability(Integer value){
            this._probability = value;
        }


        private java.math.BigDecimal _expectedRevenue;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getExpectedRevenue(){
            return _expectedRevenue;
        }

        public void setExpectedRevenue(java.math.BigDecimal value){
            this._expectedRevenue = value;
        }


        private java.math.BigDecimal _weightedRevenue;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getWeightedRevenue(){
            return _weightedRevenue;
        }

        public void setWeightedRevenue(java.math.BigDecimal value){
            this._weightedRevenue = value;
        }


        private String _forecastCategory;

    
        @PropMeta(propId=8)
    
        public String getForecastCategory(){
            return _forecastCategory;
        }

        public void setForecastCategory(String value){
            this._forecastCategory = value;
        }


        private String _forecastCategory_label;

    
        public String getForecastCategory_label(){
            return _forecastCategory_label;
        }

        public void setForecastCategory_label(String value){
            this._forecastCategory_label = value;
        }


        private Boolean _includedInCommit;

    
        @PropMeta(propId=9)
    
        public Boolean getIncludedInCommit(){
            return _includedInCommit;
        }

        public void setIncludedInCommit(Boolean value){
            this._includedInCommit = value;
        }


        private String _stageName;

    
        @PropMeta(propId=10)
    
        public String getStageName(){
            return _stageName;
        }

        public void setStageName(String value){
            this._stageName = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=12)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=13)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=15)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _forecast;

        public Map<String,Object> getForecast(){
            return _forecast;
        }

        public void setForecast(Map<String,Object> value){
            this._forecast = value;
        }


        private Map<String,Object> _lead;

        public Map<String,Object> getLead(){
            return _lead;
        }

        public void setLead(Map<String,Object> value){
            this._lead = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
