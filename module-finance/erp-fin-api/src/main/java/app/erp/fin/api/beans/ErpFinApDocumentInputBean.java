//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinApDocumentInputBean extends CrudInputBase {

    
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


        private String _status;

    
        @PropMeta(propId=9)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _docType;

    
        @PropMeta(propId=10)
    
        public String getDocType(){
            return _docType;
        }

        public void setDocType(String value){
            this._docType = value;
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


        private String _remark;

    
        @PropMeta(propId=23)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
