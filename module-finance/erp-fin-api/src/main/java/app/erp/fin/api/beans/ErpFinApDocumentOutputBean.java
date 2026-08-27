//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinApDocumentOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _orgId;

    
        @PropMeta(propId=2)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _fileName;

    
        @PropMeta(propId=3)
    
        public String getFileName(){
            return _fileName;
        }

        public void setFileName(String value){
            this._fileName = value;
        }


        private String _fileExt;

    
        @PropMeta(propId=4)
    
        public String getFileExt(){
            return _fileExt;
        }

        public void setFileExt(String value){
            this._fileExt = value;
        }


        private String _mimeType;

    
        @PropMeta(propId=5)
    
        public String getMimeType(){
            return _mimeType;
        }

        public void setMimeType(String value){
            this._mimeType = value;
        }


        private Long _fileLength;

    
        @PropMeta(propId=6)
    
        public Long getFileLength(){
            return _fileLength;
        }

        public void setFileLength(Long value){
            this._fileLength = value;
        }


        private String _fileId;

    
        @PropMeta(propId=7)
    
        public String getFileId(){
            return _fileId;
        }

        public void setFileId(String value){
            this._fileId = value;
        }


        private String _sourceType;

    
        @PropMeta(propId=8)
    
        public String getSourceType(){
            return _sourceType;
        }

        public void setSourceType(String value){
            this._sourceType = value;
        }


        private String _sourceType_label;

    
        public String getSourceType_label(){
            return _sourceType_label;
        }

        public void setSourceType_label(String value){
            this._sourceType_label = value;
        }


        private String _status;

    
        @PropMeta(propId=9)
    
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


        private String _docType;

    
        @PropMeta(propId=10)
    
        public String getDocType(){
            return _docType;
        }

        public void setDocType(String value){
            this._docType = value;
        }


        private String _docType_label;

    
        public String getDocType_label(){
            return _docType_label;
        }

        public void setDocType_label(String value){
            this._docType_label = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=11)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private java.math.BigDecimal _confidence;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getConfidence(){
            return _confidence;
        }

        public void setConfidence(java.math.BigDecimal value){
            this._confidence = value;
        }


        private String _parseResult;

    
        @PropMeta(propId=13)
    
        public String getParseResult(){
            return _parseResult;
        }

        public void setParseResult(String value){
            this._parseResult = value;
        }


        private String _invoiceId;

    
        @PropMeta(propId=14)
    
        public String getInvoiceId(){
            return _invoiceId;
        }

        public void setInvoiceId(String value){
            this._invoiceId = value;
        }


        private String _errorMsg;

    
        @PropMeta(propId=15)
    
        public String getErrorMsg(){
            return _errorMsg;
        }

        public void setErrorMsg(String value){
            this._errorMsg = value;
        }


        private Integer _retryCount;

    
        @PropMeta(propId=16)
    
        public Integer getRetryCount(){
            return _retryCount;
        }

        public void setRetryCount(Integer value){
            this._retryCount = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=17)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=18)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=19)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=21)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=23)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private io.nop.api.core.beans.file.FileStatusBean _fileIdComponentFileStatus;

    
        public io.nop.api.core.beans.file.FileStatusBean getFileIdComponentFileStatus(){
            return _fileIdComponentFileStatus;
        }

        public void setFileIdComponentFileStatus(io.nop.api.core.beans.file.FileStatusBean value){
            this._fileIdComponentFileStatus = value;
        }


    }
