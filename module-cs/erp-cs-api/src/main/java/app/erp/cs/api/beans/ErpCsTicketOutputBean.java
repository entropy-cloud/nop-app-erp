//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsTicketOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _subject;

    
        @PropMeta(propId=4)
    
        public String getSubject(){
            return _subject;
        }

        public void setSubject(String value){
            this._subject = value;
        }


        private String _description;

    
        @PropMeta(propId=5)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _customerId;

    
        @PropMeta(propId=6)
    
        public String getCustomerId(){
            return _customerId;
        }

        public void setCustomerId(String value){
            this._customerId = value;
        }


        private String _contactId;

    
        @PropMeta(propId=7)
    
        public String getContactId(){
            return _contactId;
        }

        public void setContactId(String value){
            this._contactId = value;
        }


        private String _ticketTypeId;

    
        @PropMeta(propId=8)
    
        public String getTicketTypeId(){
            return _ticketTypeId;
        }

        public void setTicketTypeId(String value){
            this._ticketTypeId = value;
        }


        private String _priority;

    
        @PropMeta(propId=9)
    
        public String getPriority(){
            return _priority;
        }

        public void setPriority(String value){
            this._priority = value;
        }


        private String _priority_label;

    
        public String getPriority_label(){
            return _priority_label;
        }

        public void setPriority_label(String value){
            this._priority_label = value;
        }


        private String _source;

    
        @PropMeta(propId=10)
    
        public String getSource(){
            return _source;
        }

        public void setSource(String value){
            this._source = value;
        }


        private String _source_label;

    
        public String getSource_label(){
            return _source_label;
        }

        public void setSource_label(String value){
            this._source_label = value;
        }


        private String _assignedToId;

    
        @PropMeta(propId=11)
    
        public String getAssignedToId(){
            return _assignedToId;
        }

        public void setAssignedToId(String value){
            this._assignedToId = value;
        }


        private String _slaPolicyId;

    
        @PropMeta(propId=12)
    
        public String getSlaPolicyId(){
            return _slaPolicyId;
        }

        public void setSlaPolicyId(String value){
            this._slaPolicyId = value;
        }


        private java.sql.Timestamp _deadlineDateTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getDeadlineDateTime(){
            return _deadlineDateTime;
        }

        public void setDeadlineDateTime(java.sql.Timestamp value){
            this._deadlineDateTime = value;
        }


        private Boolean _isSlaCompleted;

    
        @PropMeta(propId=14)
    
        public Boolean getIsSlaCompleted(){
            return _isSlaCompleted;
        }

        public void setIsSlaCompleted(Boolean value){
            this._isSlaCompleted = value;
        }


        private java.sql.Timestamp _startDateTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getStartDateTime(){
            return _startDateTime;
        }

        public void setStartDateTime(java.sql.Timestamp value){
            this._startDateTime = value;
        }


        private java.sql.Timestamp _endDateTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getEndDateTime(){
            return _endDateTime;
        }

        public void setEndDateTime(java.sql.Timestamp value){
            this._endDateTime = value;
        }


        private Integer _duration;

    
        @PropMeta(propId=17)
    
        public Integer getDuration(){
            return _duration;
        }

        public void setDuration(Integer value){
            this._duration = value;
        }


        private Integer _progress;

    
        @PropMeta(propId=18)
    
        public Integer getProgress(){
            return _progress;
        }

        public void setProgress(Integer value){
            this._progress = value;
        }


        private String _status;

    
        @PropMeta(propId=19)
    
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


        private String _docStatus;

    
        @PropMeta(propId=20)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=21)
    
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


        private String _remark;

    
        @PropMeta(propId=22)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _catalogItemId;

    
        @PropMeta(propId=23)
    
        public String getCatalogItemId(){
            return _catalogItemId;
        }

        public void setCatalogItemId(String value){
            this._catalogItemId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=24)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=25)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=26)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=27)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=28)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=29)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=200)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.sql.Timestamp _approvedAt;

    
        @PropMeta(propId=201)
    
        public java.sql.Timestamp getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.sql.Timestamp value){
            this._approvedAt = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=202)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Integer _lastEscalationLevel;

    
        @PropMeta(propId=203)
    
        public Integer getLastEscalationLevel(){
            return _lastEscalationLevel;
        }

        public void setLastEscalationLevel(Integer value){
            this._lastEscalationLevel = value;
        }


        private Integer _escalationCount;

    
        @PropMeta(propId=204)
    
        public Integer getEscalationCount(){
            return _escalationCount;
        }

        public void setEscalationCount(Integer value){
            this._escalationCount = value;
        }


        private java.sql.Timestamp _lastEscalationAt;

    
        @PropMeta(propId=205)
    
        public java.sql.Timestamp getLastEscalationAt(){
            return _lastEscalationAt;
        }

        public void setLastEscalationAt(java.sql.Timestamp value){
            this._lastEscalationAt = value;
        }


        private Map<String,Object> _customer;

        public Map<String,Object> getCustomer(){
            return _customer;
        }

        public void setCustomer(Map<String,Object> value){
            this._customer = value;
        }


        private Map<String,Object> _contact;

        public Map<String,Object> getContact(){
            return _contact;
        }

        public void setContact(Map<String,Object> value){
            this._contact = value;
        }


        private Map<String,Object> _ticketType;

        public Map<String,Object> getTicketType(){
            return _ticketType;
        }

        public void setTicketType(Map<String,Object> value){
            this._ticketType = value;
        }


        private Map<String,Object> _slaPolicy;

        public Map<String,Object> getSlaPolicy(){
            return _slaPolicy;
        }

        public void setSlaPolicy(Map<String,Object> value){
            this._slaPolicy = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _catalogItem;

        public Map<String,Object> getCatalogItem(){
            return _catalogItem;
        }

        public void setCatalogItem(Map<String,Object> value){
            this._catalogItem = value;
        }


        private List<Map<String,Object>> _actions;

        public List<Map<String,Object>> getActions(){
            return _actions;
        }

        public void setActions(List<Map<String,Object>> value){
            this._actions = value;
        }


    }
