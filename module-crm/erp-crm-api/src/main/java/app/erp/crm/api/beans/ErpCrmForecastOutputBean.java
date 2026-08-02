//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmForecastOutputBean {

    
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


        private Long _periodId;

    
        @PropMeta(propId=3)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private Long _territoryId;

    
        @PropMeta(propId=4)
    
        public Long getTerritoryId(){
            return _territoryId;
        }

        public void setTerritoryId(Long value){
            this._territoryId = value;
        }


        private Long _teamId;

    
        @PropMeta(propId=5)
    
        public Long getTeamId(){
            return _teamId;
        }

        public void setTeamId(Long value){
            this._teamId = value;
        }


        private String _ownerId;

    
        @PropMeta(propId=6)
    
        public String getOwnerId(){
            return _ownerId;
        }

        public void setOwnerId(String value){
            this._ownerId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=7)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
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


        private java.math.BigDecimal _weightedAmount;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getWeightedAmount(){
            return _weightedAmount;
        }

        public void setWeightedAmount(java.math.BigDecimal value){
            this._weightedAmount = value;
        }


        private java.math.BigDecimal _bestCaseAmount;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getBestCaseAmount(){
            return _bestCaseAmount;
        }

        public void setBestCaseAmount(java.math.BigDecimal value){
            this._bestCaseAmount = value;
        }


        private Integer _opportunityCount;

    
        @PropMeta(propId=12)
    
        public Integer getOpportunityCount(){
            return _opportunityCount;
        }

        public void setOpportunityCount(Integer value){
            this._opportunityCount = value;
        }


        private Integer _commitOpportunityCount;

    
        @PropMeta(propId=13)
    
        public Integer getCommitOpportunityCount(){
            return _commitOpportunityCount;
        }

        public void setCommitOpportunityCount(Integer value){
            this._commitOpportunityCount = value;
        }


        private java.math.BigDecimal _expectedClosedRevenue;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getExpectedClosedRevenue(){
            return _expectedClosedRevenue;
        }

        public void setExpectedClosedRevenue(java.math.BigDecimal value){
            this._expectedClosedRevenue = value;
        }


        private java.sql.Timestamp _lastCalculatedAt;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getLastCalculatedAt(){
            return _lastCalculatedAt;
        }

        public void setLastCalculatedAt(java.sql.Timestamp value){
            this._lastCalculatedAt = value;
        }


        private String _notes;

    
        @PropMeta(propId=16)
    
        public String getNotes(){
            return _notes;
        }

        public void setNotes(String value){
            this._notes = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=17)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=18)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=19)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=21)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _period;

        public Map<String,Object> getPeriod(){
            return _period;
        }

        public void setPeriod(Map<String,Object> value){
            this._period = value;
        }


        private Map<String,Object> _territory;

        public Map<String,Object> getTerritory(){
            return _territory;
        }

        public void setTerritory(Map<String,Object> value){
            this._territory = value;
        }


        private Map<String,Object> _team;

        public Map<String,Object> getTeam(){
            return _team;
        }

        public void setTeam(Map<String,Object> value){
            this._team = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


    }
