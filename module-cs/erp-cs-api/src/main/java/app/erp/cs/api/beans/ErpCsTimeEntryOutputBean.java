//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsTimeEntryOutputBean {

    
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


        private String _ticketId;

    
        @PropMeta(propId=3)
    
        public String getTicketId(){
            return _ticketId;
        }

        public void setTicketId(String value){
            this._ticketId = value;
        }


        private String _agentId;

    
        @PropMeta(propId=4)
    
        public String getAgentId(){
            return _agentId;
        }

        public void setAgentId(String value){
            this._agentId = value;
        }


        private java.sql.Timestamp _startTime;

    
        @PropMeta(propId=5)
    
        public java.sql.Timestamp getStartTime(){
            return _startTime;
        }

        public void setStartTime(java.sql.Timestamp value){
            this._startTime = value;
        }


        private java.sql.Timestamp _endTime;

    
        @PropMeta(propId=6)
    
        public java.sql.Timestamp getEndTime(){
            return _endTime;
        }

        public void setEndTime(java.sql.Timestamp value){
            this._endTime = value;
        }


        private Integer _duration;

    
        @PropMeta(propId=7)
    
        public Integer getDuration(){
            return _duration;
        }

        public void setDuration(Integer value){
            this._duration = value;
        }


        private Boolean _isBillable;

    
        @PropMeta(propId=8)
    
        public Boolean getIsBillable(){
            return _isBillable;
        }

        public void setIsBillable(Boolean value){
            this._isBillable = value;
        }


        private java.math.BigDecimal _billingRate;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getBillingRate(){
            return _billingRate;
        }

        public void setBillingRate(java.math.BigDecimal value){
            this._billingRate = value;
        }


        private java.math.BigDecimal _billableAmount;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getBillableAmount(){
            return _billableAmount;
        }

        public void setBillableAmount(java.math.BigDecimal value){
            this._billableAmount = value;
        }


        private String _description;

    
        @PropMeta(propId=11)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _approvalStatus;

    
        @PropMeta(propId=12)
    
        public String getApprovalStatus(){
            return _approvalStatus;
        }

        public void setApprovalStatus(String value){
            this._approvalStatus = value;
        }


        private String _approvalStatus_label;

    
        public String getApprovalStatus_label(){
            return _approvalStatus_label;
        }

        public void setApprovalStatus_label(String value){
            this._approvalStatus_label = value;
        }


        private String _approvedById;

    
        @PropMeta(propId=13)
    
        public String getApprovedById(){
            return _approvedById;
        }

        public void setApprovedById(String value){
            this._approvedById = value;
        }


        private java.sql.Timestamp _approvedAt;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.sql.Timestamp value){
            this._approvedAt = value;
        }


        private String _projectId;

    
        @PropMeta(propId=15)
    
        public String getProjectId(){
            return _projectId;
        }

        public void setProjectId(String value){
            this._projectId = value;
        }


        private String _taskId;

    
        @PropMeta(propId=16)
    
        public String getTaskId(){
            return _taskId;
        }

        public void setTaskId(String value){
            this._taskId = value;
        }


        private String _source;

    
        @PropMeta(propId=17)
    
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


        private Map<String,Object> _ticket;

        public Map<String,Object> getTicket(){
            return _ticket;
        }

        public void setTicket(Map<String,Object> value){
            this._ticket = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
