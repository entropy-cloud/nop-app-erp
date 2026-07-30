//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtContractVersionInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _contractId;

    
        @PropMeta(propId=2)
    
        public Long getContractId(){
            return _contractId;
        }

        public void setContractId(Long value){
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


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
