//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmSequenceStepOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _sequenceId;

    
        @PropMeta(propId=2)
    
        public Long getSequenceId(){
            return _sequenceId;
        }

        public void setSequenceId(Long value){
            this._sequenceId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _stepName;

    
        @PropMeta(propId=4)
    
        public String getStepName(){
            return _stepName;
        }

        public void setStepName(String value){
            this._stepName = value;
        }


        private Integer _stepOrder;

    
        @PropMeta(propId=5)
    
        public Integer getStepOrder(){
            return _stepOrder;
        }

        public void setStepOrder(Integer value){
            this._stepOrder = value;
        }


        private Integer _dueDays;

    
        @PropMeta(propId=6)
    
        public Integer getDueDays(){
            return _dueDays;
        }

        public void setDueDays(Integer value){
            this._dueDays = value;
        }


        private String _activityType;

    
        @PropMeta(propId=7)
    
        public String getActivityType(){
            return _activityType;
        }

        public void setActivityType(String value){
            this._activityType = value;
        }


        private String _activityType_label;

    
        public String getActivityType_label(){
            return _activityType_label;
        }

        public void setActivityType_label(String value){
            this._activityType_label = value;
        }


        private String _stepDescription;

    
        @PropMeta(propId=8)
    
        public String getStepDescription(){
            return _stepDescription;
        }

        public void setStepDescription(String value){
            this._stepDescription = value;
        }


        private String _completionCondition;

    
        @PropMeta(propId=9)
    
        public String getCompletionCondition(){
            return _completionCondition;
        }

        public void setCompletionCondition(String value){
            this._completionCondition = value;
        }


        private String _completionCondition_label;

    
        public String getCompletionCondition_label(){
            return _completionCondition_label;
        }

        public void setCompletionCondition_label(String value){
            this._completionCondition_label = value;
        }


        private Boolean _isMandatory;

    
        @PropMeta(propId=10)
    
        public Boolean getIsMandatory(){
            return _isMandatory;
        }

        public void setIsMandatory(Boolean value){
            this._isMandatory = value;
        }


        private Boolean _autoCreateEvent;

    
        @PropMeta(propId=11)
    
        public Boolean getAutoCreateEvent(){
            return _autoCreateEvent;
        }

        public void setAutoCreateEvent(Boolean value){
            this._autoCreateEvent = value;
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


        private Map<String,Object> _sequence;

        public Map<String,Object> getSequence(){
            return _sequence;
        }

        public void setSequence(Map<String,Object> value){
            this._sequence = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
