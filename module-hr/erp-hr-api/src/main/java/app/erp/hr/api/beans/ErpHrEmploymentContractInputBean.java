//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrEmploymentContractInputBean extends CrudInputBase {

    
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


        private String _employeeId;

    
        @PropMeta(propId=3)
    
        public String getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(String value){
            this._employeeId = value;
        }


        private String _contractType;

    
        @PropMeta(propId=4)
    
        public String getContractType(){
            return _contractType;
        }

        public void setContractType(String value){
            this._contractType = value;
        }


        private java.time.LocalDate _signDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getSignDate(){
            return _signDate;
        }

        public void setSignDate(java.time.LocalDate value){
            this._signDate = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private Integer _probationMonths;

    
        @PropMeta(propId=8)
    
        public Integer getProbationMonths(){
            return _probationMonths;
        }

        public void setProbationMonths(Integer value){
            this._probationMonths = value;
        }


        private java.math.BigDecimal _workingHoursPerWeek;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getWorkingHoursPerWeek(){
            return _workingHoursPerWeek;
        }

        public void setWorkingHoursPerWeek(java.math.BigDecimal value){
            this._workingHoursPerWeek = value;
        }


        private java.math.BigDecimal _annualSalary;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getAnnualSalary(){
            return _annualSalary;
        }

        public void setAnnualSalary(java.math.BigDecimal value){
            this._annualSalary = value;
        }


        private java.math.BigDecimal _monthlySalary;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getMonthlySalary(){
            return _monthlySalary;
        }

        public void setMonthlySalary(java.math.BigDecimal value){
            this._monthlySalary = value;
        }


        private String _salaryCurrencyId;

    
        @PropMeta(propId=12)
    
        public String getSalaryCurrencyId(){
            return _salaryCurrencyId;
        }

        public void setSalaryCurrencyId(String value){
            this._salaryCurrencyId = value;
        }


        private String _salaryPayMethod;

    
        @PropMeta(propId=13)
    
        public String getSalaryPayMethod(){
            return _salaryPayMethod;
        }

        public void setSalaryPayMethod(String value){
            this._salaryPayMethod = value;
        }


        private java.math.BigDecimal _socialInsuranceBase;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getSocialInsuranceBase(){
            return _socialInsuranceBase;
        }

        public void setSocialInsuranceBase(java.math.BigDecimal value){
            this._socialInsuranceBase = value;
        }


        private java.math.BigDecimal _housingFundBase;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getHousingFundBase(){
            return _housingFundBase;
        }

        public void setHousingFundBase(java.math.BigDecimal value){
            this._housingFundBase = value;
        }


        private String _status;

    
        @PropMeta(propId=16)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _attachmentFileId;

    
        @PropMeta(propId=17)
    
        public String getAttachmentFileId(){
            return _attachmentFileId;
        }

        public void setAttachmentFileId(String value){
            this._attachmentFileId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=18)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=19)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=26)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
