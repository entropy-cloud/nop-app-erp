//__XGEN_FORCE_OVERRIDE__
    package app.erp.notify.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSysNotificationTemplateInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _notificationType;

    
        @PropMeta(propId=2)
    
        public String getNotificationType(){
            return _notificationType;
        }

        public void setNotificationType(String value){
            this._notificationType = value;
        }


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private String _channelSet;

    
        @PropMeta(propId=4)
    
        public String getChannelSet(){
            return _channelSet;
        }

        public void setChannelSet(String value){
            this._channelSet = value;
        }


        private String _subjectTpl;

    
        @PropMeta(propId=5)
    
        public String getSubjectTpl(){
            return _subjectTpl;
        }

        public void setSubjectTpl(String value){
            this._subjectTpl = value;
        }


        private String _bodyTpl;

    
        @PropMeta(propId=6)
    
        public String getBodyTpl(){
            return _bodyTpl;
        }

        public void setBodyTpl(String value){
            this._bodyTpl = value;
        }


        private String _recipientResolver;

    
        @PropMeta(propId=7)
    
        public String getRecipientResolver(){
            return _recipientResolver;
        }

        public void setRecipientResolver(String value){
            this._recipientResolver = value;
        }


        private String _recipientConfig;

    
        @PropMeta(propId=8)
    
        public String getRecipientConfig(){
            return _recipientConfig;
        }

        public void setRecipientConfig(String value){
            this._recipientConfig = value;
        }


        private Integer _mergeWindowSeconds;

    
        @PropMeta(propId=9)
    
        public Integer getMergeWindowSeconds(){
            return _mergeWindowSeconds;
        }

        public void setMergeWindowSeconds(Integer value){
            this._mergeWindowSeconds = value;
        }


        private String _mergeStrategy;

    
        @PropMeta(propId=10)
    
        public String getMergeStrategy(){
            return _mergeStrategy;
        }

        public void setMergeStrategy(String value){
            this._mergeStrategy = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
