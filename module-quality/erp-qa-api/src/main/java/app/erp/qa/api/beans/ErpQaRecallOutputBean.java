//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaRecallOutputBean {

    
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


        private String _recallName;

    
        @PropMeta(propId=3)
    
        public String getRecallName(){
            return _recallName;
        }

        public void setRecallName(String value){
            this._recallName = value;
        }


        private String _triggerType;

    
        @PropMeta(propId=4)
    
        public String getTriggerType(){
            return _triggerType;
        }

        public void setTriggerType(String value){
            this._triggerType = value;
        }


        private String _triggerType_label;

    
        public String getTriggerType_label(){
            return _triggerType_label;
        }

        public void setTriggerType_label(String value){
            this._triggerType_label = value;
        }


        private Long _sourceNcrId;

    
        @PropMeta(propId=5)
    
        public Long getSourceNcrId(){
            return _sourceNcrId;
        }

        public void setSourceNcrId(Long value){
            this._sourceNcrId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=6)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _batchId;

    
        @PropMeta(propId=7)
    
        public Long getBatchId(){
            return _batchId;
        }

        public void setBatchId(Long value){
            this._batchId = value;
        }


        private String _serialNo;

    
        @PropMeta(propId=8)
    
        public String getSerialNo(){
            return _serialNo;
        }

        public void setSerialNo(String value){
            this._serialNo = value;
        }


        private String _rootCause;

    
        @PropMeta(propId=9)
    
        public String getRootCause(){
            return _rootCause;
        }

        public void setRootCause(String value){
            this._rootCause = value;
        }


        private String _severityLevel;

    
        @PropMeta(propId=10)
    
        public String getSeverityLevel(){
            return _severityLevel;
        }

        public void setSeverityLevel(String value){
            this._severityLevel = value;
        }


        private String _severityLevel_label;

    
        public String getSeverityLevel_label(){
            return _severityLevel_label;
        }

        public void setSeverityLevel_label(String value){
            this._severityLevel_label = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Boolean _notifyCustomer;

    
        @PropMeta(propId=12)
    
        public Boolean getNotifyCustomer(){
            return _notifyCustomer;
        }

        public void setNotifyCustomer(Boolean value){
            this._notifyCustomer = value;
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


        private String _approveStatus;

    
        @PropMeta(propId=14)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _approveStatus_label;

    
        public String getApproveStatus_label(){
            return _approveStatus_label;
        }

        public void setApproveStatus_label(String value){
            this._approveStatus_label = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=15)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=16)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=18)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=19)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=20)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=22)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _sourceNcr;

        public Map<String,Object> getSourceNcr(){
            return _sourceNcr;
        }

        public void setSourceNcr(Map<String,Object> value){
            this._sourceNcr = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private List<Map<String,Object>> _targets;

        public List<Map<String,Object>> getTargets(){
            return _targets;
        }

        public void setTargets(List<Map<String,Object>> value){
            this._targets = value;
        }


    }
