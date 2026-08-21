//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtContractVersionOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _contractId;

    
        @PropMeta(propId=2)
    
        public String getContractId(){
            return _contractId;
        }

        public void setContractId(String value){
            this._contractId = value;
        }


        private Integer _versionNo;

    
        @PropMeta(propId=3)
    
        public Integer getVersionNo(){
            return _versionNo;
        }

        public void setVersionNo(Integer value){
            this._versionNo = value;
        }


        private java.time.LocalDate _versionDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getVersionDate(){
            return _versionDate;
        }

        public void setVersionDate(java.time.LocalDate value){
            this._versionDate = value;
        }


        private String _content;

    
        @PropMeta(propId=5)
    
        public String getContent(){
            return _content;
        }

        public void setContent(String value){
            this._content = value;
        }


        private String _attachmentFileId;

    
        @PropMeta(propId=6)
    
        public String getAttachmentFileId(){
            return _attachmentFileId;
        }

        public void setAttachmentFileId(String value){
            this._attachmentFileId = value;
        }


        private Boolean _isCurrent;

    
        @PropMeta(propId=7)
    
        public Boolean getIsCurrent(){
            return _isCurrent;
        }

        public void setIsCurrent(Boolean value){
            this._isCurrent = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
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


        private String _approvedBy;

    
        @PropMeta(propId=9)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.sql.Timestamp _approvedAt;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.sql.Timestamp value){
            this._approvedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=13)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=14)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=16)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=17)
    
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


    }
