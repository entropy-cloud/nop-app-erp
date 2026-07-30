//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrShiftAssignmentOutputBean {

    
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


        private Long _employeeId;

    
        @PropMeta(propId=3)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private Long _shiftId;

    
        @PropMeta(propId=4)
    
        public Long getShiftId(){
            return _shiftId;
        }

        public void setShiftId(Long value){
            this._shiftId = value;
        }


        private java.time.LocalDate _assignmentDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getAssignmentDate(){
            return _assignmentDate;
        }

        public void setAssignmentDate(java.time.LocalDate value){
            this._assignmentDate = value;
        }


        private java.sql.Timestamp _actualStartTime;

    
        @PropMeta(propId=6)
    
        public java.sql.Timestamp getActualStartTime(){
            return _actualStartTime;
        }

        public void setActualStartTime(java.sql.Timestamp value){
            this._actualStartTime = value;
        }


        private java.sql.Timestamp _actualEndTime;

    
        @PropMeta(propId=7)
    
        public java.sql.Timestamp getActualEndTime(){
            return _actualEndTime;
        }

        public void setActualEndTime(java.sql.Timestamp value){
            this._actualEndTime = value;
        }


        private Boolean _isAbsent;

    
        @PropMeta(propId=8)
    
        public Boolean getIsAbsent(){
            return _isAbsent;
        }

        public void setIsAbsent(Boolean value){
            this._isAbsent = value;
        }


        private String _absenceReason;

    
        @PropMeta(propId=9)
    
        public String getAbsenceReason(){
            return _absenceReason;
        }

        public void setAbsenceReason(String value){
            this._absenceReason = value;
        }


        private String _absenceReason_label;

    
        public String getAbsenceReason_label(){
            return _absenceReason_label;
        }

        public void setAbsenceReason_label(String value){
            this._absenceReason_label = value;
        }


        private Long _leaveRequestId;

    
        @PropMeta(propId=10)
    
        public Long getLeaveRequestId(){
            return _leaveRequestId;
        }

        public void setLeaveRequestId(Long value){
            this._leaveRequestId = value;
        }


        private Long _swapRequestId;

    
        @PropMeta(propId=11)
    
        public Long getSwapRequestId(){
            return _swapRequestId;
        }

        public void setSwapRequestId(Long value){
            this._swapRequestId = value;
        }


        private Long _replacedByAssignmentId;

    
        @PropMeta(propId=12)
    
        public Long getReplacedByAssignmentId(){
            return _replacedByAssignmentId;
        }

        public void setReplacedByAssignmentId(Long value){
            this._replacedByAssignmentId = value;
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


        private Map<String,Object> _shift;

        public Map<String,Object> getShift(){
            return _shift;
        }

        public void setShift(Map<String,Object> value){
            this._shift = value;
        }


        private Map<String,Object> _leaveRequest;

        public Map<String,Object> getLeaveRequest(){
            return _leaveRequest;
        }

        public void setLeaveRequest(Map<String,Object> value){
            this._leaveRequest = value;
        }


        private Map<String,Object> _swapRequest;

        public Map<String,Object> getSwapRequest(){
            return _swapRequest;
        }

        public void setSwapRequest(Map<String,Object> value){
            this._swapRequest = value;
        }


        private Map<String,Object> _replacedByAssignment;

        public Map<String,Object> getReplacedByAssignment(){
            return _replacedByAssignment;
        }

        public void setReplacedByAssignment(Map<String,Object> value){
            this._replacedByAssignment = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
