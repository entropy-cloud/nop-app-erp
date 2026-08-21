//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtSignatureRequestOutputBean {

    
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


        private String _provider_label;

    
        public String getProvider_label(){
            return _provider_label;
        }

        public void setProvider_label(String value){
            this._provider_label = value;
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


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
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


        private io.nop.api.core.beans.file.FileStatusBean _attachmentFileIdComponentFileStatus;

    
        public io.nop.api.core.beans.file.FileStatusBean getAttachmentFileIdComponentFileStatus(){
            return _attachmentFileIdComponentFileStatus;
        }

        public void setAttachmentFileIdComponentFileStatus(io.nop.api.core.beans.file.FileStatusBean value){
            this._attachmentFileIdComponentFileStatus = value;
        }


        private Map<String,Object> _contractVersion;

        public Map<String,Object> getContractVersion(){
            return _contractVersion;
        }

        public void setContractVersion(Map<String,Object> value){
            this._contractVersion = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _providerRequest;

        public Map<String,Object> getProviderRequest(){
            return _providerRequest;
        }

        public void setProviderRequest(Map<String,Object> value){
            this._providerRequest = value;
        }


    }
