//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrShiftInputBean extends CrudInputBase {

    
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


        private String _name;

    
        @PropMeta(propId=4)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private String _shiftType;

    
        @PropMeta(propId=5)
    
        public String getShiftType(){
            return _shiftType;
        }

        public void setShiftType(String value){
            this._shiftType = value;
        }


        private String _startTime;

    
        @PropMeta(propId=6)
    
        public String getStartTime(){
            return _startTime;
        }

        public void setStartTime(String value){
            this._startTime = value;
        }


        private String _endTime;

    
        @PropMeta(propId=7)
    
        public String getEndTime(){
            return _endTime;
        }

        public void setEndTime(String value){
            this._endTime = value;
        }


        private Integer _graceLateMinutes;

    
        @PropMeta(propId=8)
    
        public Integer getGraceLateMinutes(){
            return _graceLateMinutes;
        }

        public void setGraceLateMinutes(Integer value){
            this._graceLateMinutes = value;
        }


        private Integer _graceEarlyLeaveMinutes;

    
        @PropMeta(propId=9)
    
        public Integer getGraceEarlyLeaveMinutes(){
            return _graceEarlyLeaveMinutes;
        }

        public void setGraceEarlyLeaveMinutes(Integer value){
            this._graceEarlyLeaveMinutes = value;
        }


        private Boolean _requireClockIn;

    
        @PropMeta(propId=10)
    
        public Boolean getRequireClockIn(){
            return _requireClockIn;
        }

        public void setRequireClockIn(Boolean value){
            this._requireClockIn = value;
        }


        private Boolean _requireClockOut;

    
        @PropMeta(propId=11)
    
        public Boolean getRequireClockOut(){
            return _requireClockOut;
        }

        public void setRequireClockOut(Boolean value){
            this._requireClockOut = value;
        }


        private String _restStartTime;

    
        @PropMeta(propId=12)
    
        public String getRestStartTime(){
            return _restStartTime;
        }

        public void setRestStartTime(String value){
            this._restStartTime = value;
        }


        private String _restEndTime;

    
        @PropMeta(propId=13)
    
        public String getRestEndTime(){
            return _restEndTime;
        }

        public void setRestEndTime(String value){
            this._restEndTime = value;
        }


        private Integer _totalWorkMinutes;

    
        @PropMeta(propId=14)
    
        public Integer getTotalWorkMinutes(){
            return _totalWorkMinutes;
        }

        public void setTotalWorkMinutes(Integer value){
            this._totalWorkMinutes = value;
        }


        private Boolean _allowOvertime;

    
        @PropMeta(propId=15)
    
        public Boolean getAllowOvertime(){
            return _allowOvertime;
        }

        public void setAllowOvertime(Boolean value){
            this._allowOvertime = value;
        }


        private String _colorHex;

    
        @PropMeta(propId=16)
    
        public String getColorHex(){
            return _colorHex;
        }

        public void setColorHex(String value){
            this._colorHex = value;
        }


        private String _description;

    
        @PropMeta(propId=17)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


    }
