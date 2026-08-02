//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrShiftSwapRequestOutputBean {

    
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


        private Long _requesterId;

    
        @PropMeta(propId=4)
    
        public Long getRequesterId(){
            return _requesterId;
        }

        public void setRequesterId(Long value){
            this._requesterId = value;
        }


        private Long _targetEmployeeId;

    
        @PropMeta(propId=5)
    
        public Long getTargetEmployeeId(){
            return _targetEmployeeId;
        }

        public void setTargetEmployeeId(Long value){
            this._targetEmployeeId = value;
        }


        private Long _sourceAssignmentId;

    
        @PropMeta(propId=6)
    
        public Long getSourceAssignmentId(){
            return _sourceAssignmentId;
        }

        public void setSourceAssignmentId(Long value){
            this._sourceAssignmentId = value;
        }


        private Long _targetAssignmentId;

    
        @PropMeta(propId=7)
    
        public Long getTargetAssignmentId(){
            return _targetAssignmentId;
        }

        public void setTargetAssignmentId(Long value){
            this._targetAssignmentId = value;
        }


        private java.time.LocalDate _swapDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getSwapDate(){
            return _swapDate;
        }

        public void setSwapDate(java.time.LocalDate value){
            this._swapDate = value;
        }


        private String _reason;

    
        @PropMeta(propId=9)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


        private String _status;

    
        @PropMeta(propId=10)
    
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


        private String _approvedById;

    
        @PropMeta(propId=11)
    
        public String getApprovedById(){
            return _approvedById;
        }

        public void setApprovedById(String value){
            this._approvedById = value;
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


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Map<String,Object> _requester;

        public Map<String,Object> getRequester(){
            return _requester;
        }

        public void setRequester(Map<String,Object> value){
            this._requester = value;
        }


        private Map<String,Object> _targetEmployee;

        public Map<String,Object> getTargetEmployee(){
            return _targetEmployee;
        }

        public void setTargetEmployee(Map<String,Object> value){
            this._targetEmployee = value;
        }


        private Map<String,Object> _sourceAssignment;

        public Map<String,Object> getSourceAssignment(){
            return _sourceAssignment;
        }

        public void setSourceAssignment(Map<String,Object> value){
            this._sourceAssignment = value;
        }


        private Map<String,Object> _targetAssignment;

        public Map<String,Object> getTargetAssignment(){
            return _targetAssignment;
        }

        public void setTargetAssignment(Map<String,Object> value){
            this._targetAssignment = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
