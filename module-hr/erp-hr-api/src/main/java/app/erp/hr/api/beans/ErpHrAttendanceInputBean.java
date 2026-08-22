//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrAttendanceInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _employeeId;

    
        @PropMeta(propId=2)
    
        public String getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(String value){
            this._employeeId = value;
        }


        private java.time.LocalDate _date;

    
        @PropMeta(propId=3)
    
        public java.time.LocalDate getDate(){
            return _date;
        }

        public void setDate(java.time.LocalDate value){
            this._date = value;
        }


        private java.sql.Timestamp _clockIn;

    
        @PropMeta(propId=4)
    
        public java.sql.Timestamp getClockIn(){
            return _clockIn;
        }

        public void setClockIn(java.sql.Timestamp value){
            this._clockIn = value;
        }


        private java.sql.Timestamp _clockOut;

    
        @PropMeta(propId=5)
    
        public java.sql.Timestamp getClockOut(){
            return _clockOut;
        }

        public void setClockOut(java.sql.Timestamp value){
            this._clockOut = value;
        }


        private java.math.BigDecimal _workHours;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getWorkHours(){
            return _workHours;
        }

        public void setWorkHours(java.math.BigDecimal value){
            this._workHours = value;
        }


        private Integer _lateMinutes;

    
        @PropMeta(propId=7)
    
        public Integer getLateMinutes(){
            return _lateMinutes;
        }

        public void setLateMinutes(Integer value){
            this._lateMinutes = value;
        }


        private Integer _earlyLeaveMinutes;

    
        @PropMeta(propId=8)
    
        public Integer getEarlyLeaveMinutes(){
            return _earlyLeaveMinutes;
        }

        public void setEarlyLeaveMinutes(Integer value){
            this._earlyLeaveMinutes = value;
        }


        private Boolean _isAbsent;

    
        @PropMeta(propId=9)
    
        public Boolean getIsAbsent(){
            return _isAbsent;
        }

        public void setIsAbsent(Boolean value){
            this._isAbsent = value;
        }


        private String _source;

    
        @PropMeta(propId=10)
    
        public String getSource(){
            return _source;
        }

        public void setSource(String value){
            this._source = value;
        }


        private String _leaveRequestId;

    
        @PropMeta(propId=11)
    
        public String getLeaveRequestId(){
            return _leaveRequestId;
        }

        public void setLeaveRequestId(String value){
            this._leaveRequestId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=12)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
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


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
