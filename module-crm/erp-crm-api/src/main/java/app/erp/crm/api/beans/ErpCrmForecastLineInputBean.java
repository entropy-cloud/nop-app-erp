//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmForecastLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _forecastId;

    
        @PropMeta(propId=2)
    
        public String getForecastId(){
            return _forecastId;
        }

        public void setForecastId(String value){
            this._forecastId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _leadId;

    
        @PropMeta(propId=4)
    
        public String getLeadId(){
            return _leadId;
        }

        public void setLeadId(String value){
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


    }
