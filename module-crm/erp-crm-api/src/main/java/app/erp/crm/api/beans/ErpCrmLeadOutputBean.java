//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _leadType;

    
        @PropMeta(propId=4)
    
        public String getLeadType(){
            return _leadType;
        }

        public void setLeadType(String value){
            this._leadType = value;
        }


        private String _leadType_label;

    
        public String getLeadType_label(){
            return _leadType_label;
        }

        public void setLeadType_label(String value){
            this._leadType_label = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=5)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private String _contactName;

    
        @PropMeta(propId=6)
    
        public String getContactName(){
            return _contactName;
        }

        public void setContactName(String value){
            this._contactName = value;
        }


        private String _contactPhone;

    
        @PropMeta(propId=7)
    
        public String getContactPhone(){
            return _contactPhone;
        }

        public void setContactPhone(String value){
            this._contactPhone = value;
        }


        private String _contactEmail;

    
        @PropMeta(propId=8)
    
        public String getContactEmail(){
            return _contactEmail;
        }

        public void setContactEmail(String value){
            this._contactEmail = value;
        }


        private String _companyName;

    
        @PropMeta(propId=9)
    
        public String getCompanyName(){
            return _companyName;
        }

        public void setCompanyName(String value){
            this._companyName = value;
        }


        private String _jobTitle;

    
        @PropMeta(propId=10)
    
        public String getJobTitle(){
            return _jobTitle;
        }

        public void setJobTitle(String value){
            this._jobTitle = value;
        }


        private String _department;

    
        @PropMeta(propId=11)
    
        public String getDepartment(){
            return _department;
        }

        public void setDepartment(String value){
            this._department = value;
        }


        private Long _sourceId;

    
        @PropMeta(propId=12)
    
        public Long getSourceId(){
            return _sourceId;
        }

        public void setSourceId(Long value){
            this._sourceId = value;
        }


        private Long _leadStatusId;

    
        @PropMeta(propId=13)
    
        public Long getLeadStatusId(){
            return _leadStatusId;
        }

        public void setLeadStatusId(Long value){
            this._leadStatusId = value;
        }


        private Long _stageId;

    
        @PropMeta(propId=14)
    
        public Long getStageId(){
            return _stageId;
        }

        public void setStageId(Long value){
            this._stageId = value;
        }


        private java.math.BigDecimal _expectedRevenue;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getExpectedRevenue(){
            return _expectedRevenue;
        }

        public void setExpectedRevenue(java.math.BigDecimal value){
            this._expectedRevenue = value;
        }


        private java.math.BigDecimal _bestCaseAmount;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getBestCaseAmount(){
            return _bestCaseAmount;
        }

        public void setBestCaseAmount(java.math.BigDecimal value){
            this._bestCaseAmount = value;
        }


        private java.math.BigDecimal _worstCaseAmount;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getWorstCaseAmount(){
            return _worstCaseAmount;
        }

        public void setWorstCaseAmount(java.math.BigDecimal value){
            this._worstCaseAmount = value;
        }


        private java.math.BigDecimal _recurringRevenue;

    
        @PropMeta(propId=18)
    
        public java.math.BigDecimal getRecurringRevenue(){
            return _recurringRevenue;
        }

        public void setRecurringRevenue(java.math.BigDecimal value){
            this._recurringRevenue = value;
        }


        private String _recurringPlan;

    
        @PropMeta(propId=19)
    
        public String getRecurringPlan(){
            return _recurringPlan;
        }

        public void setRecurringPlan(String value){
            this._recurringPlan = value;
        }


        private java.time.LocalDate _expectedCloseDate;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDate getExpectedCloseDate(){
            return _expectedCloseDate;
        }

        public void setExpectedCloseDate(java.time.LocalDate value){
            this._expectedCloseDate = value;
        }


        private Integer _probability;

    
        @PropMeta(propId=21)
    
        public Integer getProbability(){
            return _probability;
        }

        public void setProbability(Integer value){
            this._probability = value;
        }


        private Long _campaignId;

    
        @PropMeta(propId=22)
    
        public Long getCampaignId(){
            return _campaignId;
        }

        public void setCampaignId(Long value){
            this._campaignId = value;
        }


        private String _utmMedium;

    
        @PropMeta(propId=23)
    
        public String getUtmMedium(){
            return _utmMedium;
        }

        public void setUtmMedium(String value){
            this._utmMedium = value;
        }


        private String _utmSource;

    
        @PropMeta(propId=24)
    
        public String getUtmSource(){
            return _utmSource;
        }

        public void setUtmSource(String value){
            this._utmSource = value;
        }


        private String _ownerId;

    
        @PropMeta(propId=25)
    
        public String getOwnerId(){
            return _ownerId;
        }

        public void setOwnerId(String value){
            this._ownerId = value;
        }


        private Long _teamId;

    
        @PropMeta(propId=26)
    
        public Long getTeamId(){
            return _teamId;
        }

        public void setTeamId(Long value){
            this._teamId = value;
        }


        private Long _lostReasonId;

    
        @PropMeta(propId=27)
    
        public Long getLostReasonId(){
            return _lostReasonId;
        }

        public void setLostReasonId(Long value){
            this._lostReasonId = value;
        }


        private String _lostReasonDesc;

    
        @PropMeta(propId=28)
    
        public String getLostReasonDesc(){
            return _lostReasonDesc;
        }

        public void setLostReasonDesc(String value){
            this._lostReasonDesc = value;
        }


        private java.time.LocalDate _lastContactDate;

    
        @PropMeta(propId=29)
    
        public java.time.LocalDate getLastContactDate(){
            return _lastContactDate;
        }

        public void setLastContactDate(java.time.LocalDate value){
            this._lastContactDate = value;
        }


        private java.time.LocalDate _nextActivityDate;

    
        @PropMeta(propId=30)
    
        public java.time.LocalDate getNextActivityDate(){
            return _nextActivityDate;
        }

        public void setNextActivityDate(java.time.LocalDate value){
            this._nextActivityDate = value;
        }


        private String _relatedBillType;

    
        @PropMeta(propId=31)
    
        public String getRelatedBillType(){
            return _relatedBillType;
        }

        public void setRelatedBillType(String value){
            this._relatedBillType = value;
        }


        private String _relatedBillCode;

    
        @PropMeta(propId=32)
    
        public String getRelatedBillCode(){
            return _relatedBillCode;
        }

        public void setRelatedBillCode(String value){
            this._relatedBillCode = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=33)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private String _remark;

    
        @PropMeta(propId=34)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=35)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=36)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=37)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=38)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=39)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=40)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Long _territoryId;

    
        @PropMeta(propId=41)
    
        public Long getTerritoryId(){
            return _territoryId;
        }

        public void setTerritoryId(Long value){
            this._territoryId = value;
        }


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _source;

        public Map<String,Object> getSource(){
            return _source;
        }

        public void setSource(Map<String,Object> value){
            this._source = value;
        }


        private Map<String,Object> _leadStatus;

        public Map<String,Object> getLeadStatus(){
            return _leadStatus;
        }

        public void setLeadStatus(Map<String,Object> value){
            this._leadStatus = value;
        }


        private Map<String,Object> _stage;

        public Map<String,Object> getStage(){
            return _stage;
        }

        public void setStage(Map<String,Object> value){
            this._stage = value;
        }


        private Map<String,Object> _campaign;

        public Map<String,Object> getCampaign(){
            return _campaign;
        }

        public void setCampaign(Map<String,Object> value){
            this._campaign = value;
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


        private Map<String,Object> _lostReason;

        public Map<String,Object> getLostReason(){
            return _lostReason;
        }

        public void setLostReason(Map<String,Object> value){
            this._lostReason = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private List<Map<String,Object>> _events;

        public List<Map<String,Object>> getEvents(){
            return _events;
        }

        public void setEvents(List<Map<String,Object>> value){
            this._events = value;
        }


        private List<Map<String,Object>> _activities;

        public List<Map<String,Object>> getActivities(){
            return _activities;
        }

        public void setActivities(List<Map<String,Object>> value){
            this._activities = value;
        }


        private List<Map<String,Object>> _convLogs;

        public List<Map<String,Object>> getConvLogs(){
            return _convLogs;
        }

        public void setConvLogs(List<Map<String,Object>> value){
            this._convLogs = value;
        }


    }
