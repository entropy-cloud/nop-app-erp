//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmEventOutputBean {

    
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


        private String _eventType;

    
        @PropMeta(propId=4)
    
        public String getEventType(){
            return _eventType;
        }

        public void setEventType(String value){
            this._eventType = value;
        }


        private String _eventType_label;

    
        public String getEventType_label(){
            return _eventType_label;
        }

        public void setEventType_label(String value){
            this._eventType_label = value;
        }


        private Long _eventCategoryId;

    
        @PropMeta(propId=5)
    
        public Long getEventCategoryId(){
            return _eventCategoryId;
        }

        public void setEventCategoryId(Long value){
            this._eventCategoryId = value;
        }


        private String _subject;

    
        @PropMeta(propId=6)
    
        public String getSubject(){
            return _subject;
        }

        public void setSubject(String value){
            this._subject = value;
        }


        private String _description;

    
        @PropMeta(propId=7)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private java.sql.Timestamp _startDateTime;

    
        @PropMeta(propId=8)
    
        public java.sql.Timestamp getStartDateTime(){
            return _startDateTime;
        }

        public void setStartDateTime(java.sql.Timestamp value){
            this._startDateTime = value;
        }


        private java.sql.Timestamp _endDateTime;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getEndDateTime(){
            return _endDateTime;
        }

        public void setEndDateTime(java.sql.Timestamp value){
            this._endDateTime = value;
        }


        private Integer _duration;

    
        @PropMeta(propId=10)
    
        public Integer getDuration(){
            return _duration;
        }

        public void setDuration(Integer value){
            this._duration = value;
        }


        private Long _relatedLeadId;

    
        @PropMeta(propId=11)
    
        public Long getRelatedLeadId(){
            return _relatedLeadId;
        }

        public void setRelatedLeadId(Long value){
            this._relatedLeadId = value;
        }


        private String _relatedBillType;

    
        @PropMeta(propId=12)
    
        public String getRelatedBillType(){
            return _relatedBillType;
        }

        public void setRelatedBillType(String value){
            this._relatedBillType = value;
        }


        private String _relatedBillCode;

    
        @PropMeta(propId=13)
    
        public String getRelatedBillCode(){
            return _relatedBillCode;
        }

        public void setRelatedBillCode(String value){
            this._relatedBillCode = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=14)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Long _contactId;

    
        @PropMeta(propId=15)
    
        public Long getContactId(){
            return _contactId;
        }

        public void setContactId(Long value){
            this._contactId = value;
        }


        private String _ownerId;

    
        @PropMeta(propId=16)
    
        public String getOwnerId(){
            return _ownerId;
        }

        public void setOwnerId(String value){
            this._ownerId = value;
        }


        private String _status;

    
        @PropMeta(propId=17)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private String _priority;

    
        @PropMeta(propId=18)
    
        public String getPriority(){
            return _priority;
        }

        public void setPriority(String value){
            this._priority = value;
        }


        private String _priority_label;

    
        public String getPriority_label(){
            return _priority_label;
        }

        public void setPriority_label(String value){
            this._priority_label = value;
        }


        private Boolean _isRecurrent;

    
        @PropMeta(propId=19)
    
        public Boolean getIsRecurrent(){
            return _isRecurrent;
        }

        public void setIsRecurrent(Boolean value){
            this._isRecurrent = value;
        }


        private Long _parentEventId;

    
        @PropMeta(propId=20)
    
        public Long getParentEventId(){
            return _parentEventId;
        }

        public void setParentEventId(Long value){
            this._parentEventId = value;
        }


        private Integer _reminderMinutesBefore;

    
        @PropMeta(propId=21)
    
        public Integer getReminderMinutesBefore(){
            return _reminderMinutesBefore;
        }

        public void setReminderMinutesBefore(Integer value){
            this._reminderMinutesBefore = value;
        }


        private String _remark;

    
        @PropMeta(propId=22)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=23)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=24)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=25)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=26)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=27)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=28)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _relatedLead;

        public Map<String,Object> getRelatedLead(){
            return _relatedLead;
        }

        public void setRelatedLead(Map<String,Object> value){
            this._relatedLead = value;
        }


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _parentEvent;

        public Map<String,Object> getParentEvent(){
            return _parentEvent;
        }

        public void setParentEvent(Map<String,Object> value){
            this._parentEvent = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _eventCategory;

        public Map<String,Object> getEventCategory(){
            return _eventCategory;
        }

        public void setEventCategory(Map<String,Object> value){
            this._eventCategory = value;
        }


    }
