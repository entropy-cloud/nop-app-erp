//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bEdiDocInputBean extends CrudInputBase {

    
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


        private String _formatId;

    
        @PropMeta(propId=4)
    
        public String getFormatId(){
            return _formatId;
        }

        public void setFormatId(String value){
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


        private String _blockingLevel;

    
        @PropMeta(propId=8)
    
        public String getBlockingLevel(){
            return _blockingLevel;
        }

        public void setBlockingLevel(String value){
            this._blockingLevel = value;
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


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=21)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
