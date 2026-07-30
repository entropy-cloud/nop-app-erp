//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmLeadScoreInputBean extends CrudInputBase {

    
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


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
