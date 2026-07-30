//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrTimesheetInputBean extends CrudInputBase {

    
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


        private java.time.LocalDate _periodFrom;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getPeriodFrom(){
            return _periodFrom;
        }

        public void setPeriodFrom(java.time.LocalDate value){
            this._periodFrom = value;
        }


        private java.time.LocalDate _periodTo;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getPeriodTo(){
            return _periodTo;
        }

        public void setPeriodTo(java.time.LocalDate value){
            this._periodTo = value;
        }


        private java.math.BigDecimal _totalHours;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getTotalHours(){
            return _totalHours;
        }

        public void setTotalHours(java.math.BigDecimal value){
            this._totalHours = value;
        }


        private String _status;

    
        @PropMeta(propId=7)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=8)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=16)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private List<ErpHrTimesheetLineInputBean> _lines;

        public List<ErpHrTimesheetLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpHrTimesheetLineInputBean> value){
            this._lines = value;
        }


    }
