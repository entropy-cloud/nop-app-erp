//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadScoreOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _leadId;

    
        @PropMeta(propId=2)
    
        public Long getLeadId(){
            return _leadId;
        }

        public void setLeadId(Long value){
            this._leadId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _configId;

    
        @PropMeta(propId=4)
    
        public Long getConfigId(){
            return _configId;
        }

        public void setConfigId(Long value){
            this._configId = value;
        }


        private Integer _totalScore;

    
        @PropMeta(propId=5)
    
        public Integer getTotalScore(){
            return _totalScore;
        }

        public void setTotalScore(Integer value){
            this._totalScore = value;
        }


        private String _scoreBreakdown;

    
        @PropMeta(propId=6)
    
        public String getScoreBreakdown(){
            return _scoreBreakdown;
        }

        public void setScoreBreakdown(String value){
            this._scoreBreakdown = value;
        }


        private Boolean _autoQualified;

    
        @PropMeta(propId=7)
    
        public Boolean getAutoQualified(){
            return _autoQualified;
        }

        public void setAutoQualified(Boolean value){
            this._autoQualified = value;
        }


        private String _triggeredAction;

    
        @PropMeta(propId=8)
    
        public String getTriggeredAction(){
            return _triggeredAction;
        }

        public void setTriggeredAction(String value){
            this._triggeredAction = value;
        }


        private String _triggeredAction_label;

    
        public String getTriggeredAction_label(){
            return _triggeredAction_label;
        }

        public void setTriggeredAction_label(String value){
            this._triggeredAction_label = value;
        }


        private java.sql.Timestamp _calculatedAt;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getCalculatedAt(){
            return _calculatedAt;
        }

        public void setCalculatedAt(java.sql.Timestamp value){
            this._calculatedAt = value;
        }


        private String _triggerEvent;

    
        @PropMeta(propId=10)
    
        public String getTriggerEvent(){
            return _triggerEvent;
        }

        public void setTriggerEvent(String value){
            this._triggerEvent = value;
        }


        private String _triggerEvent_label;

    
        public String getTriggerEvent_label(){
            return _triggerEvent_label;
        }

        public void setTriggerEvent_label(String value){
            this._triggerEvent_label = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
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


        private Map<String,Object> _lead;

        public Map<String,Object> getLead(){
            return _lead;
        }

        public void setLead(Map<String,Object> value){
            this._lead = value;
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
