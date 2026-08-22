//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrTaxSpecialDeductionInputBean extends CrudInputBase {

    
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


        private String _deductionType;

    
        @PropMeta(propId=5)
    
        public String getDeductionType(){
            return _deductionType;
        }

        public void setDeductionType(String value){
            this._deductionType = value;
        }


        private java.math.BigDecimal _monthlyAmount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getMonthlyAmount(){
            return _monthlyAmount;
        }

        public void setMonthlyAmount(java.math.BigDecimal value){
            this._monthlyAmount = value;
        }


        private Boolean _verified;

    
        @PropMeta(propId=7)
    
        public Boolean getVerified(){
            return _verified;
        }

        public void setVerified(Boolean value){
            this._verified = value;
        }


        private String _orgId;

    
        @PropMeta(propId=8)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
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


    }
