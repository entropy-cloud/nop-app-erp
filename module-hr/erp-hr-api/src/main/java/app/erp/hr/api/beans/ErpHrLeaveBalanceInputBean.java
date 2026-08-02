//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrLeaveBalanceInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _employeeId;

    
        @PropMeta(propId=2)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private String _leaveType;

    
        @PropMeta(propId=3)
    
        public String getLeaveType(){
            return _leaveType;
        }

        public void setLeaveType(String value){
            this._leaveType = value;
        }


        private Integer _fiscalYear;

    
        @PropMeta(propId=4)
    
        public Integer getFiscalYear(){
            return _fiscalYear;
        }

        public void setFiscalYear(Integer value){
            this._fiscalYear = value;
        }


        private java.math.BigDecimal _entitledDays;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getEntitledDays(){
            return _entitledDays;
        }

        public void setEntitledDays(java.math.BigDecimal value){
            this._entitledDays = value;
        }


        private java.math.BigDecimal _carriedForwardDays;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getCarriedForwardDays(){
            return _carriedForwardDays;
        }

        public void setCarriedForwardDays(java.math.BigDecimal value){
            this._carriedForwardDays = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=7)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=15)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
