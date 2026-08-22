//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrShiftAssignmentInputBean extends CrudInputBase {

    
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


        private String _employeeId;

    
        @PropMeta(propId=3)
    
        public String getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(String value){
            this._employeeId = value;
        }


        private String _shiftId;

    
        @PropMeta(propId=4)
    
        public String getShiftId(){
            return _shiftId;
        }

        public void setShiftId(String value){
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


        private String _leaveRequestId;

    
        @PropMeta(propId=10)
    
        public String getLeaveRequestId(){
            return _leaveRequestId;
        }

        public void setLeaveRequestId(String value){
            this._leaveRequestId = value;
        }


        private String _swapRequestId;

    
        @PropMeta(propId=11)
    
        public String getSwapRequestId(){
            return _swapRequestId;
        }

        public void setSwapRequestId(String value){
            this._swapRequestId = value;
        }


        private String _replacedByAssignmentId;

    
        @PropMeta(propId=12)
    
        public String getReplacedByAssignmentId(){
            return _replacedByAssignmentId;
        }

        public void setReplacedByAssignmentId(String value){
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


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
