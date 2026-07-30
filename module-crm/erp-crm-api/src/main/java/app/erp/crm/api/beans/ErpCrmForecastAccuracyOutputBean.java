//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmForecastAccuracyOutputBean {

    
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


        private Long _periodId;

    
        @PropMeta(propId=4)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private String _ownerId;

    
        @PropMeta(propId=5)
    
        public String getOwnerId(){
            return _ownerId;
        }

        public void setOwnerId(String value){
            this._ownerId = value;
        }


        private Long _teamId;

    
        @PropMeta(propId=6)
    
        public Long getTeamId(){
            return _teamId;
        }

        public void setTeamId(Long value){
            this._teamId = value;
        }


        private Long _territoryId;

    
        @PropMeta(propId=7)
    
        public Long getTerritoryId(){
            return _territoryId;
        }

        public void setTerritoryId(Long value){
            this._territoryId = value;
        }


        private java.math.BigDecimal _commitAmount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getCommitAmount(){
            return _commitAmount;
        }

        public void setCommitAmount(java.math.BigDecimal value){
            this._commitAmount = value;
        }


        private java.math.BigDecimal _upsideAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getUpsideAmount(){
            return _upsideAmount;
        }

        public void setUpsideAmount(java.math.BigDecimal value){
            this._upsideAmount = value;
        }


        private java.math.BigDecimal _actualClosedRevenue;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getActualClosedRevenue(){
            return _actualClosedRevenue;
        }

        public void setActualClosedRevenue(java.math.BigDecimal value){
            this._actualClosedRevenue = value;
        }


        private java.math.BigDecimal _commitAccuracy;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getCommitAccuracy(){
            return _commitAccuracy;
        }

        public void setCommitAccuracy(java.math.BigDecimal value){
            this._commitAccuracy = value;
        }


        private java.math.BigDecimal _upsideAccuracy;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getUpsideAccuracy(){
            return _upsideAccuracy;
        }

        public void setUpsideAccuracy(java.math.BigDecimal value){
            this._upsideAccuracy = value;
        }


        private java.math.BigDecimal _deviationAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getDeviationAmount(){
            return _deviationAmount;
        }

        public void setDeviationAmount(java.math.BigDecimal value){
            this._deviationAmount = value;
        }


        private String _calculatedBy;

    
        @PropMeta(propId=14)
    
        public String getCalculatedBy(){
            return _calculatedBy;
        }

        public void setCalculatedBy(String value){
            this._calculatedBy = value;
        }


        private java.sql.Timestamp _calculatedAt;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCalculatedAt(){
            return _calculatedAt;
        }

        public void setCalculatedAt(java.sql.Timestamp value){
            this._calculatedAt = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=17)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=18)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=20)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=21)
    
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


        private Map<String,Object> _period;

        public Map<String,Object> getPeriod(){
            return _period;
        }

        public void setPeriod(Map<String,Object> value){
            this._period = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _team;

        public Map<String,Object> getTeam(){
            return _team;
        }

        public void setTeam(Map<String,Object> value){
            this._team = value;
        }


        private Map<String,Object> _territory;

        public Map<String,Object> getTerritory(){
            return _territory;
        }

        public void setTerritory(Map<String,Object> value){
            this._territory = value;
        }


    }
