//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtContractOutputBean {

    
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


        private String _contractName;

    
        @PropMeta(propId=4)
    
        public String getContractName(){
            return _contractName;
        }

        public void setContractName(String value){
            this._contractName = value;
        }


        private String _contractType;

    
        @PropMeta(propId=5)
    
        public String getContractType(){
            return _contractType;
        }

        public void setContractType(String value){
            this._contractType = value;
        }


        private String _contractType_label;

    
        public String getContractType_label(){
            return _contractType_label;
        }

        public void setContractType_label(String value){
            this._contractType_label = value;
        }


        private String _contractDirection;

    
        @PropMeta(propId=6)
    
        public String getContractDirection(){
            return _contractDirection;
        }

        public void setContractDirection(String value){
            this._contractDirection = value;
        }


        private String _contractDirection_label;

    
        public String getContractDirection_label(){
            return _contractDirection_label;
        }

        public void setContractDirection_label(String value){
            this._contractDirection_label = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=7)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=8)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _totalAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal value){
            this._totalAmount = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private java.time.LocalDate _signDate;

    
        @PropMeta(propId=12)
    
        public java.time.LocalDate getSignDate(){
            return _signDate;
        }

        public void setSignDate(java.time.LocalDate value){
            this._signDate = value;
        }


        private String _status;

    
        @PropMeta(propId=13)
    
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


        private Long _templateId;

    
        @PropMeta(propId=14)
    
        public Long getTemplateId(){
            return _templateId;
        }

        public void setTemplateId(Long value){
            this._templateId = value;
        }


        private Long _parentContractId;

    
        @PropMeta(propId=15)
    
        public Long getParentContractId(){
            return _parentContractId;
        }

        public void setParentContractId(Long value){
            this._parentContractId = value;
        }


        private String _description;

    
        @PropMeta(propId=16)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _attachmentFileId;

    
        @PropMeta(propId=17)
    
        public String getAttachmentFileId(){
            return _attachmentFileId;
        }

        public void setAttachmentFileId(String value){
            this._attachmentFileId = value;
        }


        private String _remark;

    
        @PropMeta(propId=18)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=19)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=20)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=21)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=23)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=25)
    
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


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _template;

        public Map<String,Object> getTemplate(){
            return _template;
        }

        public void setTemplate(Map<String,Object> value){
            this._template = value;
        }


        private Map<String,Object> _parentContract;

        public Map<String,Object> getParentContract(){
            return _parentContract;
        }

        public void setParentContract(Map<String,Object> value){
            this._parentContract = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private List<Map<String,Object>> _lines;

        public List<Map<String,Object>> getLines(){
            return _lines;
        }

        public void setLines(List<Map<String,Object>> value){
            this._lines = value;
        }


        private List<Map<String,Object>> _versions;

        public List<Map<String,Object>> getVersions(){
            return _versions;
        }

        public void setVersions(List<Map<String,Object>> value){
            this._versions = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


    }
