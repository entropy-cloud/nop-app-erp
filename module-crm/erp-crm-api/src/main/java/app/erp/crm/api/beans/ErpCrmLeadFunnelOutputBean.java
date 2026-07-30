//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadFunnelOutputBean {

    
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


        private String _funnelName;

    
        @PropMeta(propId=3)
    
        public String getFunnelName(){
            return _funnelName;
        }

        public void setFunnelName(String value){
            this._funnelName = value;
        }


        private java.time.LocalDate _periodStart;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getPeriodStart(){
            return _periodStart;
        }

        public void setPeriodStart(java.time.LocalDate value){
            this._periodStart = value;
        }


        private java.time.LocalDate _periodEnd;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getPeriodEnd(){
            return _periodEnd;
        }

        public void setPeriodEnd(java.time.LocalDate value){
            this._periodEnd = value;
        }


        private Long _territoryId;

    
        @PropMeta(propId=6)
    
        public Long getTerritoryId(){
            return _territoryId;
        }

        public void setTerritoryId(Long value){
            this._territoryId = value;
        }


        private Long _teamId;

    
        @PropMeta(propId=7)
    
        public Long getTeamId(){
            return _teamId;
        }

        public void setTeamId(Long value){
            this._teamId = value;
        }


        private Long _sourceId;

    
        @PropMeta(propId=8)
    
        public Long getSourceId(){
            return _sourceId;
        }

        public void setSourceId(Long value){
            this._sourceId = value;
        }


        private Integer _totalLeadsAtTop;

    
        @PropMeta(propId=9)
    
        public Integer getTotalLeadsAtTop(){
            return _totalLeadsAtTop;
        }

        public void setTotalLeadsAtTop(Integer value){
            this._totalLeadsAtTop = value;
        }


        private Integer _totalOpportunities;

    
        @PropMeta(propId=10)
    
        public Integer getTotalOpportunities(){
            return _totalOpportunities;
        }

        public void setTotalOpportunities(Integer value){
            this._totalOpportunities = value;
        }


        private Integer _totalWon;

    
        @PropMeta(propId=11)
    
        public Integer getTotalWon(){
            return _totalWon;
        }

        public void setTotalWon(Integer value){
            this._totalWon = value;
        }


        private Integer _totalLost;

    
        @PropMeta(propId=12)
    
        public Integer getTotalLost(){
            return _totalLost;
        }

        public void setTotalLost(Integer value){
            this._totalLost = value;
        }


        private java.math.BigDecimal _totalRevenue;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getTotalRevenue(){
            return _totalRevenue;
        }

        public void setTotalRevenue(java.math.BigDecimal value){
            this._totalRevenue = value;
        }


        private java.math.BigDecimal _lostRevenue;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getLostRevenue(){
            return _lostRevenue;
        }

        public void setLostRevenue(java.math.BigDecimal value){
            this._lostRevenue = value;
        }


        private java.math.BigDecimal _weightedRevenue;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getWeightedRevenue(){
            return _weightedRevenue;
        }

        public void setWeightedRevenue(java.math.BigDecimal value){
            this._weightedRevenue = value;
        }


        private java.math.BigDecimal _avgDealSize;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getAvgDealSize(){
            return _avgDealSize;
        }

        public void setAvgDealSize(java.math.BigDecimal value){
            this._avgDealSize = value;
        }


        private java.math.BigDecimal _avgSalesCycleDays;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getAvgSalesCycleDays(){
            return _avgSalesCycleDays;
        }

        public void setAvgSalesCycleDays(java.math.BigDecimal value){
            this._avgSalesCycleDays = value;
        }


        private java.sql.Timestamp _calculatedAt;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getCalculatedAt(){
            return _calculatedAt;
        }

        public void setCalculatedAt(java.sql.Timestamp value){
            this._calculatedAt = value;
        }


        private String _calculatedBy;

    
        @PropMeta(propId=19)
    
        public String getCalculatedBy(){
            return _calculatedBy;
        }

        public void setCalculatedBy(String value){
            this._calculatedBy = value;
        }


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=21)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=22)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=23)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=25)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=26)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
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


        private Map<String,Object> _source;

        public Map<String,Object> getSource(){
            return _source;
        }

        public void setSource(Map<String,Object> value){
            this._source = value;
        }


    }
