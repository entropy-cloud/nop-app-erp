//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSalaryInputBean extends CrudInputBase {

    
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


        private Integer _year;

    
        @PropMeta(propId=3)
    
        public Integer getYear(){
            return _year;
        }

        public void setYear(Integer value){
            this._year = value;
        }


        private Integer _month;

    
        @PropMeta(propId=4)
    
        public Integer getMonth(){
            return _month;
        }

        public void setMonth(Integer value){
            this._month = value;
        }


        private java.math.BigDecimal _basicSalary;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getBasicSalary(){
            return _basicSalary;
        }

        public void setBasicSalary(java.math.BigDecimal value){
            this._basicSalary = value;
        }


        private java.math.BigDecimal _positionAllowance;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getPositionAllowance(){
            return _positionAllowance;
        }

        public void setPositionAllowance(java.math.BigDecimal value){
            this._positionAllowance = value;
        }


        private java.math.BigDecimal _performanceBonus;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getPerformanceBonus(){
            return _performanceBonus;
        }

        public void setPerformanceBonus(java.math.BigDecimal value){
            this._performanceBonus = value;
        }


        private java.math.BigDecimal _overtimePay;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getOvertimePay(){
            return _overtimePay;
        }

        public void setOvertimePay(java.math.BigDecimal value){
            this._overtimePay = value;
        }


        private java.math.BigDecimal _mealAllowance;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getMealAllowance(){
            return _mealAllowance;
        }

        public void setMealAllowance(java.math.BigDecimal value){
            this._mealAllowance = value;
        }


        private java.math.BigDecimal _transportAllowance;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getTransportAllowance(){
            return _transportAllowance;
        }

        public void setTransportAllowance(java.math.BigDecimal value){
            this._transportAllowance = value;
        }


        private java.math.BigDecimal _otherAllowance;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getOtherAllowance(){
            return _otherAllowance;
        }

        public void setOtherAllowance(java.math.BigDecimal value){
            this._otherAllowance = value;
        }


        private java.math.BigDecimal _grossSalary;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getGrossSalary(){
            return _grossSalary;
        }

        public void setGrossSalary(java.math.BigDecimal value){
            this._grossSalary = value;
        }


        private java.math.BigDecimal _socialInsurance;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getSocialInsurance(){
            return _socialInsurance;
        }

        public void setSocialInsurance(java.math.BigDecimal value){
            this._socialInsurance = value;
        }


        private java.math.BigDecimal _housingFund;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getHousingFund(){
            return _housingFund;
        }

        public void setHousingFund(java.math.BigDecimal value){
            this._housingFund = value;
        }


        private java.math.BigDecimal _taxAmount;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getTaxAmount(){
            return _taxAmount;
        }

        public void setTaxAmount(java.math.BigDecimal value){
            this._taxAmount = value;
        }


        private java.math.BigDecimal _otherDeductions;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getOtherDeductions(){
            return _otherDeductions;
        }

        public void setOtherDeductions(java.math.BigDecimal value){
            this._otherDeductions = value;
        }


        private java.math.BigDecimal _netSalary;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getNetSalary(){
            return _netSalary;
        }

        public void setNetSalary(java.math.BigDecimal value){
            this._netSalary = value;
        }


        private String _paymentStatus;

    
        @PropMeta(propId=18)
    
        public String getPaymentStatus(){
            return _paymentStatus;
        }

        public void setPaymentStatus(String value){
            this._paymentStatus = value;
        }


        private java.time.LocalDate _paymentDate;

    
        @PropMeta(propId=19)
    
        public java.time.LocalDate getPaymentDate(){
            return _paymentDate;
        }

        public void setPaymentDate(java.time.LocalDate value){
            this._paymentDate = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=28)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private java.math.BigDecimal _performanceFactor;

    
        @PropMeta(propId=29)
    
        public java.math.BigDecimal getPerformanceFactor(){
            return _performanceFactor;
        }

        public void setPerformanceFactor(java.math.BigDecimal value){
            this._performanceFactor = value;
        }


        private java.math.BigDecimal _actualWorkDays;

    
        @PropMeta(propId=30)
    
        public java.math.BigDecimal getActualWorkDays(){
            return _actualWorkDays;
        }

        public void setActualWorkDays(java.math.BigDecimal value){
            this._actualWorkDays = value;
        }


        private java.math.BigDecimal _requiredWorkDays;

    
        @PropMeta(propId=31)
    
        public java.math.BigDecimal getRequiredWorkDays(){
            return _requiredWorkDays;
        }

        public void setRequiredWorkDays(java.math.BigDecimal value){
            this._requiredWorkDays = value;
        }


        private java.math.BigDecimal _totalOvertimeHours;

    
        @PropMeta(propId=32)
    
        public java.math.BigDecimal getTotalOvertimeHours(){
            return _totalOvertimeHours;
        }

        public void setTotalOvertimeHours(java.math.BigDecimal value){
            this._totalOvertimeHours = value;
        }


        private java.math.BigDecimal _unpaidLeaveDays;

    
        @PropMeta(propId=33)
    
        public java.math.BigDecimal getUnpaidLeaveDays(){
            return _unpaidLeaveDays;
        }

        public void setUnpaidLeaveDays(java.math.BigDecimal value){
            this._unpaidLeaveDays = value;
        }


        private String _cumulativeData;

    
        @PropMeta(propId=34)
    
        public String getCumulativeData(){
            return _cumulativeData;
        }

        public void setCumulativeData(String value){
            this._cumulativeData = value;
        }


        private String _reviewNote;

    
        @PropMeta(propId=35)
    
        public String getReviewNote(){
            return _reviewNote;
        }

        public void setReviewNote(String value){
            this._reviewNote = value;
        }


        private String _paymentBatchNo;

    
        @PropMeta(propId=36)
    
        public String getPaymentBatchNo(){
            return _paymentBatchNo;
        }

        public void setPaymentBatchNo(String value){
            this._paymentBatchNo = value;
        }


        private String _bankFileId;

    
        @PropMeta(propId=37)
    
        public String getBankFileId(){
            return _bankFileId;
        }

        public void setBankFileId(String value){
            this._bankFileId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=38)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=39)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=92)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
