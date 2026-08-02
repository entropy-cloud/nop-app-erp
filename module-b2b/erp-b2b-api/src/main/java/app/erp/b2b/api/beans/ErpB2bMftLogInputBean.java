//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bMftLogInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _configId;

    
        @PropMeta(propId=3)
    
        public Long getConfigId(){
            return _configId;
        }

        public void setConfigId(Long value){
            this._configId = value;
        }


        private String _relatedBillType;

    
        @PropMeta(propId=4)
    
        public String getRelatedBillType(){
            return _relatedBillType;
        }

        public void setRelatedBillType(String value){
            this._relatedBillType = value;
        }


        private String _relatedBillCode;

    
        @PropMeta(propId=5)
    
        public String getRelatedBillCode(){
            return _relatedBillCode;
        }

        public void setRelatedBillCode(String value){
            this._relatedBillCode = value;
        }


        private String _direction;

    
        @PropMeta(propId=6)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private String _fileName;

    
        @PropMeta(propId=7)
    
        public String getFileName(){
            return _fileName;
        }

        public void setFileName(String value){
            this._fileName = value;
        }


        private Long _fileSize;

    
        @PropMeta(propId=8)
    
        public Long getFileSize(){
            return _fileSize;
        }

        public void setFileSize(Long value){
            this._fileSize = value;
        }


        private String _fileHash;

    
        @PropMeta(propId=9)
    
        public String getFileHash(){
            return _fileHash;
        }

        public void setFileHash(String value){
            this._fileHash = value;
        }


        private String _messageId;

    
        @PropMeta(propId=10)
    
        public String getMessageId(){
            return _messageId;
        }

        public void setMessageId(String value){
            this._messageId = value;
        }


        private String _mdnStatus;

    
        @PropMeta(propId=11)
    
        public String getMdnStatus(){
            return _mdnStatus;
        }

        public void setMdnStatus(String value){
            this._mdnStatus = value;
        }


        private String _protocol;

    
        @PropMeta(propId=12)
    
        public String getProtocol(){
            return _protocol;
        }

        public void setProtocol(String value){
            this._protocol = value;
        }


        private String _status;

    
        @PropMeta(propId=13)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private java.sql.Timestamp _startTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getStartTime(){
            return _startTime;
        }

        public void setStartTime(java.sql.Timestamp value){
            this._startTime = value;
        }


        private java.sql.Timestamp _endTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getEndTime(){
            return _endTime;
        }

        public void setEndTime(java.sql.Timestamp value){
            this._endTime = value;
        }


        private Long _durationMs;

    
        @PropMeta(propId=16)
    
        public Long getDurationMs(){
            return _durationMs;
        }

        public void setDurationMs(Long value){
            this._durationMs = value;
        }


        private String _errorCode;

    
        @PropMeta(propId=17)
    
        public String getErrorCode(){
            return _errorCode;
        }

        public void setErrorCode(String value){
            this._errorCode = value;
        }


        private String _errorMsg;

    
        @PropMeta(propId=18)
    
        public String getErrorMsg(){
            return _errorMsg;
        }

        public void setErrorMsg(String value){
            this._errorMsg = value;
        }


        private Integer _retryCount;

    
        @PropMeta(propId=19)
    
        public Integer getRetryCount(){
            return _retryCount;
        }

        public void setRetryCount(Integer value){
            this._retryCount = value;
        }


        private Boolean _isCompressed;

    
        @PropMeta(propId=20)
    
        public Boolean getIsCompressed(){
            return _isCompressed;
        }

        public void setIsCompressed(Boolean value){
            this._isCompressed = value;
        }


        private Boolean _isEncrypted;

    
        @PropMeta(propId=21)
    
        public Boolean getIsEncrypted(){
            return _isEncrypted;
        }

        public void setIsEncrypted(Boolean value){
            this._isEncrypted = value;
        }


        private Boolean _isSigned;

    
        @PropMeta(propId=22)
    
        public Boolean getIsSigned(){
            return _isSigned;
        }

        public void setIsSigned(Boolean value){
            this._isSigned = value;
        }


        private String _remark;

    
        @PropMeta(propId=23)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
