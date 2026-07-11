//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinExpenseClaimLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _claimId;

    
        @PropMeta(propId=2)
    
        public Long getClaimId(){
            return _claimId;
        }

        public void setClaimId(Long value){
            this._claimId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _expenseType;

    
        @PropMeta(propId=4)
    
        public String getExpenseType(){
            return _expenseType;
        }

        public void setExpenseType(String value){
            this._expenseType = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=5)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private Long _costCenterId;

    
        @PropMeta(propId=6)
    
        public Long getCostCenterId(){
            return _costCenterId;
        }

        public void setCostCenterId(Long value){
            this._costCenterId = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=7)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
            this._subjectId = value;
        }


        private String _subjectCode;

    
        @PropMeta(propId=8)
    
        public String getSubjectCode(){
            return _subjectCode;
        }

        public void setSubjectCode(String value){
            this._subjectCode = value;
        }


        private java.math.BigDecimal _amountWithoutTax;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getAmountWithoutTax(){
            return _amountWithoutTax;
        }

        public void setAmountWithoutTax(java.math.BigDecimal value){
            this._amountWithoutTax = value;
        }


        private java.math.BigDecimal _taxRate;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getTaxRate(){
            return _taxRate;
        }

        public void setTaxRate(java.math.BigDecimal value){
            this._taxRate = value;
        }


        private java.math.BigDecimal _taxAmount;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getTaxAmount(){
            return _taxAmount;
        }

        public void setTaxAmount(java.math.BigDecimal value){
            this._taxAmount = value;
        }


        private java.math.BigDecimal _amountWithTax;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAmountWithTax(){
            return _amountWithTax;
        }

        public void setAmountWithTax(java.math.BigDecimal value){
            this._amountWithTax = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
