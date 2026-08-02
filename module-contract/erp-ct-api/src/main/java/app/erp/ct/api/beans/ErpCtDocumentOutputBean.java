//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtDocumentOutputBean {

    
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


        private Long _contractId;

    
        @PropMeta(propId=3)
    
        public Long getContractId(){
            return _contractId;
        }

        public void setContractId(Long value){
            this._contractId = value;
        }


        private String _code;

    
        @PropMeta(propId=4)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private String _docName;

    
        @PropMeta(propId=5)
    
        public String getDocName(){
            return _docName;
        }

        public void setDocName(String value){
            this._docName = value;
        }


        private String _docType;

    
        @PropMeta(propId=6)
    
        public String getDocType(){
            return _docType;
        }

        public void setDocType(String value){
            this._docType = value;
        }


        private String _attachmentFileId;

    
        @PropMeta(propId=7)
    
        public String getAttachmentFileId(){
            return _attachmentFileId;
        }

        public void setAttachmentFileId(String value){
            this._attachmentFileId = value;
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


        private String _mimeType;

    
        @PropMeta(propId=10)
    
        public String getMimeType(){
            return _mimeType;
        }

        public void setMimeType(String value){
            this._mimeType = value;
        }


        private String _ocrText;

    
        @PropMeta(propId=11)
    
        public String getOcrText(){
            return _ocrText;
        }

        public void setOcrText(String value){
            this._ocrText = value;
        }


        private String _ocrStatus;

    
        @PropMeta(propId=12)
    
        public String getOcrStatus(){
            return _ocrStatus;
        }

        public void setOcrStatus(String value){
            this._ocrStatus = value;
        }


        private String _fullTextSearch;

    
        @PropMeta(propId=13)
    
        public String getFullTextSearch(){
            return _fullTextSearch;
        }

        public void setFullTextSearch(String value){
            this._fullTextSearch = value;
        }


        private String _metadataTags;

    
        @PropMeta(propId=14)
    
        public String getMetadataTags(){
            return _metadataTags;
        }

        public void setMetadataTags(String value){
            this._metadataTags = value;
        }


        private java.time.LocalDate _retentionDate;

    
        @PropMeta(propId=15)
    
        public java.time.LocalDate getRetentionDate(){
            return _retentionDate;
        }

        public void setRetentionDate(java.time.LocalDate value){
            this._retentionDate = value;
        }


        private java.time.LocalDate _archiveDate;

    
        @PropMeta(propId=16)
    
        public java.time.LocalDate getArchiveDate(){
            return _archiveDate;
        }

        public void setArchiveDate(java.time.LocalDate value){
            this._archiveDate = value;
        }


        private java.time.LocalDate _purgeDate;

    
        @PropMeta(propId=17)
    
        public java.time.LocalDate getPurgeDate(){
            return _purgeDate;
        }

        public void setPurgeDate(java.time.LocalDate value){
            this._purgeDate = value;
        }


        private Boolean _isArchived;

    
        @PropMeta(propId=18)
    
        public Boolean getIsArchived(){
            return _isArchived;
        }

        public void setIsArchived(Boolean value){
            this._isArchived = value;
        }


        private Integer _versionNo;

    
        @PropMeta(propId=19)
    
        public Integer getVersionNo(){
            return _versionNo;
        }

        public void setVersionNo(Integer value){
            this._versionNo = value;
        }


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=21)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=22)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=23)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=25)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=26)
    
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


        private Map<String,Object> _contract;

        public Map<String,Object> getContract(){
            return _contract;
        }

        public void setContract(Map<String,Object> value){
            this._contract = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
