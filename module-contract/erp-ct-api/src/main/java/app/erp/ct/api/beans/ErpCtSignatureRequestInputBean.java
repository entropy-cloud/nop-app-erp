//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtSignatureRequestInputBean extends CrudInputBase {

    
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


        private String _contractVersionId;

    
        @PropMeta(propId=3)
    
        public String getContractVersionId(){
            return _contractVersionId;
        }

        public void setContractVersionId(String value){
            this._contractVersionId = value;
        }


        private String _provider;

    
        @PropMeta(propId=4)
    
        public String getProvider(){
            return _provider;
        }

        public void setProvider(String value){
            this._provider = value;
        }


        private String _providerRequestId;

    
        @PropMeta(propId=5)
    
        public String getProviderRequestId(){
            return _providerRequestId;
        }

        public void setProviderRequestId(String value){
            this._providerRequestId = value;
        }


        private String _status;

    
        @PropMeta(propId=6)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _signers;

    
        @PropMeta(propId=7)
    
        public String getSigners(){
            return _signers;
        }

        public void setSigners(String value){
            this._signers = value;
        }


        private java.time.LocalDate _signingDeadline;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getSigningDeadline(){
            return _signingDeadline;
        }

        public void setSigningDeadline(java.time.LocalDate value){
            this._signingDeadline = value;
        }


        private java.sql.Timestamp _completedAt;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getCompletedAt(){
            return _completedAt;
        }

        public void setCompletedAt(java.sql.Timestamp value){
            this._completedAt = value;
        }


        private String _certificateUrl;

    
        @PropMeta(propId=10)
    
        public String getCertificateUrl(){
            return _certificateUrl;
        }

        public void setCertificateUrl(String value){
            this._certificateUrl = value;
        }


        private String _evidenceNo;

    
        @PropMeta(propId=11)
    
        public String getEvidenceNo(){
            return _evidenceNo;
        }

        public void setEvidenceNo(String value){
            this._evidenceNo = value;
        }


        private String _attachmentFileId;

    
        @PropMeta(propId=12)
    
        public String getAttachmentFileId(){
            return _attachmentFileId;
        }

        public void setAttachmentFileId(String value){
            this._attachmentFileId = value;
        }


        private String _errorMsg;

    
        @PropMeta(propId=13)
    
        public String getErrorMsg(){
            return _errorMsg;
        }

        public void setErrorMsg(String value){
            this._errorMsg = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
