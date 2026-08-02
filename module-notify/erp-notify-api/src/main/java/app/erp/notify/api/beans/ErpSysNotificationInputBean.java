//__XGEN_FORCE_OVERRIDE__
    package app.erp.notify.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSysNotificationInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _templateId;

    
        @PropMeta(propId=2)
    
        public Long getTemplateId(){
            return _templateId;
        }

        public void setTemplateId(Long value){
            this._templateId = value;
        }


        private String _notificationType;

    
        @PropMeta(propId=3)
    
        public String getNotificationType(){
            return _notificationType;
        }

        public void setNotificationType(String value){
            this._notificationType = value;
        }


        private String _recipientUserId;

    
        @PropMeta(propId=4)
    
        public String getRecipientUserId(){
            return _recipientUserId;
        }

        public void setRecipientUserId(String value){
            this._recipientUserId = value;
        }


        private Long _recipientPartnerId;

    
        @PropMeta(propId=5)
    
        public Long getRecipientPartnerId(){
            return _recipientPartnerId;
        }

        public void setRecipientPartnerId(Long value){
            this._recipientPartnerId = value;
        }


        private Long _recipientDeptId;

    
        @PropMeta(propId=6)
    
        public Long getRecipientDeptId(){
            return _recipientDeptId;
        }

        public void setRecipientDeptId(Long value){
            this._recipientDeptId = value;
        }


        private String _channel;

    
        @PropMeta(propId=7)
    
        public String getChannel(){
            return _channel;
        }

        public void setChannel(String value){
            this._channel = value;
        }


        private String _subject;

    
        @PropMeta(propId=8)
    
        public String getSubject(){
            return _subject;
        }

        public void setSubject(String value){
            this._subject = value;
        }


        private String _body;

    
        @PropMeta(propId=9)
    
        public String getBody(){
            return _body;
        }

        public void setBody(String value){
            this._body = value;
        }


        private String _payloadJson;

    
        @PropMeta(propId=10)
    
        public String getPayloadJson(){
            return _payloadJson;
        }

        public void setPayloadJson(String value){
            this._payloadJson = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _mergeGroupId;

    
        @PropMeta(propId=12)
    
        public String getMergeGroupId(){
            return _mergeGroupId;
        }

        public void setMergeGroupId(String value){
            this._mergeGroupId = value;
        }


        private Integer _mergeCount;

    
        @PropMeta(propId=13)
    
        public Integer getMergeCount(){
            return _mergeCount;
        }

        public void setMergeCount(Integer value){
            this._mergeCount = value;
        }


        private java.sql.Timestamp _sentAt;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getSentAt(){
            return _sentAt;
        }

        public void setSentAt(java.sql.Timestamp value){
            this._sentAt = value;
        }


        private String _errorMsg;

    
        @PropMeta(propId=15)
    
        public String getErrorMsg(){
            return _errorMsg;
        }

        public void setErrorMsg(String value){
            this._errorMsg = value;
        }


    }
