//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bEdiDocOutputBean {

    
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


        private Long _formatId;

    
        @PropMeta(propId=4)
    
        public Long getFormatId(){
            return _formatId;
        }

        public void setFormatId(Long value){
            this._formatId = value;
        }


        private String _relatedBillType;

    
        @PropMeta(propId=5)
    
        public String getRelatedBillType(){
            return _relatedBillType;
        }

        public void setRelatedBillType(String value){
            this._relatedBillType = value;
        }


        private String _relatedBillCode;

    
        @PropMeta(propId=6)
    
        public String getRelatedBillCode(){
            return _relatedBillCode;
        }

        public void setRelatedBillCode(String value){
            this._relatedBillCode = value;
        }


        private String _state;

    
        @PropMeta(propId=7)
    
        public String getState(){
            return _state;
        }

        public void setState(String value){
            this._state = value;
        }


        private String _state_label;

    
        public String getState_label(){
            return _state_label;
        }

        public void setState_label(String value){
            this._state_label = value;
        }


        private String _blockingLevel;

    
        @PropMeta(propId=8)
    
        public String getBlockingLevel(){
            return _blockingLevel;
        }

        public void setBlockingLevel(String value){
            this._blockingLevel = value;
        }


        private String _blockingLevel_label;

    
        public String getBlockingLevel_label(){
            return _blockingLevel_label;
        }

        public void setBlockingLevel_label(String value){
            this._blockingLevel_label = value;
        }


        private String _error;

    
        @PropMeta(propId=9)
    
        public String getError(){
            return _error;
        }

        public void setError(String value){
            this._error = value;
        }


        private Integer _retryCount;

    
        @PropMeta(propId=10)
    
        public Integer getRetryCount(){
            return _retryCount;
        }

        public void setRetryCount(Integer value){
            this._retryCount = value;
        }


        private String _attachmentFileId;

    
        @PropMeta(propId=11)
    
        public String getAttachmentFileId(){
            return _attachmentFileId;
        }

        public void setAttachmentFileId(String value){
            this._attachmentFileId = value;
        }


        private java.sql.Timestamp _sentAt;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getSentAt(){
            return _sentAt;
        }

        public void setSentAt(java.sql.Timestamp value){
            this._sentAt = value;
        }


        private java.sql.Timestamp _acknowledgedAt;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getAcknowledgedAt(){
            return _acknowledgedAt;
        }

        public void setAcknowledgedAt(java.sql.Timestamp value){
            this._acknowledgedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=16)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=17)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=19)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=21)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private io.nop.api.core.beans.file.FileStatusBean _attachmentFileIdComponentFileStatus;

    
        public io.nop.api.core.beans.file.FileStatusBean getAttachmentFileIdComponentFileStatus(){
            return _attachmentFileIdComponentFileStatus;
        }

        public void setAttachmentFileIdComponentFileStatus(io.nop.api.core.beans.file.FileStatusBean value){
            this._attachmentFileIdComponentFileStatus = value;
        }


        private Map<String,Object> _format;

        public Map<String,Object> getFormat(){
            return _format;
        }

        public void setFormat(Map<String,Object> value){
            this._format = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
