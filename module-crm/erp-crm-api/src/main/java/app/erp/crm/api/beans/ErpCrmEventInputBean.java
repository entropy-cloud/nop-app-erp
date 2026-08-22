//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmEventInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
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


        private String _eventCategoryId;

    
        @PropMeta(propId=5)
    
        public String getEventCategoryId(){
            return _eventCategoryId;
        }

        public void setEventCategoryId(String value){
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


        private String _relatedLeadId;

    
        @PropMeta(propId=11)
    
        public String getRelatedLeadId(){
            return _relatedLeadId;
        }

        public void setRelatedLeadId(String value){
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


        private String _partnerId;

    
        @PropMeta(propId=14)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private String _contactId;

    
        @PropMeta(propId=15)
    
        public String getContactId(){
            return _contactId;
        }

        public void setContactId(String value){
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


        private String _priority;

    
        @PropMeta(propId=18)
    
        public String getPriority(){
            return _priority;
        }

        public void setPriority(String value){
            this._priority = value;
        }


        private Boolean _isRecurrent;

    
        @PropMeta(propId=19)
    
        public Boolean getIsRecurrent(){
            return _isRecurrent;
        }

        public void setIsRecurrent(Boolean value){
            this._isRecurrent = value;
        }


        private String _parentEventId;

    
        @PropMeta(propId=20)
    
        public String getParentEventId(){
            return _parentEventId;
        }

        public void setParentEventId(String value){
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


    }
