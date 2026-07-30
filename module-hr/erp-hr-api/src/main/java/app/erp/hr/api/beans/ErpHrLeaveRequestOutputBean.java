//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrLeaveRequestOutputBean {

    
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


        private Long _employeeId;

    
        @PropMeta(propId=3)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private String _leaveType;

    
        @PropMeta(propId=4)
    
        public String getLeaveType(){
            return _leaveType;
        }

        public void setLeaveType(String value){
            this._leaveType = value;
        }


        private String _leaveType_label;

    
        public String getLeaveType_label(){
            return _leaveType_label;
        }

        public void setLeaveType_label(String value){
            this._leaveType_label = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private java.math.BigDecimal _durationDays;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getDurationDays(){
            return _durationDays;
        }

        public void setDurationDays(java.math.BigDecimal value){
            this._durationDays = value;
        }


        private String _reason;

    
        @PropMeta(propId=8)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


        private String _status;

    
        @PropMeta(propId=9)
    
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


        private Long _approverId;

    
        @PropMeta(propId=10)
    
        public Long getApproverId(){
            return _approverId;
        }

        public void setApproverId(Long value){
            this._approverId = value;
        }


        private java.sql.Timestamp _approvedAt;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.sql.Timestamp value){
            this._approvedAt = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=12)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Map<String,Object> _employee;

        public Map<String,Object> getEmployee(){
            return _employee;
        }

        public void setEmployee(Map<String,Object> value){
            this._employee = value;
        }


        private Map<String,Object> _approver;

        public Map<String,Object> getApprover(){
            return _approver;
        }

        public void setApprover(Map<String,Object> value){
            this._approver = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
