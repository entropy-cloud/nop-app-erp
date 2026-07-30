//__XGEN_FORCE_OVERRIDE__
    package app.erp.notify.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSysNotificationTemplateOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
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


        private String _recipientResolver_label;

    
        public String getRecipientResolver_label(){
            return _recipientResolver_label;
        }

        public void setRecipientResolver_label(String value){
            this._recipientResolver_label = value;
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


        private String _mergeStrategy_label;

    
        public String getMergeStrategy_label(){
            return _mergeStrategy_label;
        }

        public void setMergeStrategy_label(String value){
            this._mergeStrategy_label = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
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


    }
