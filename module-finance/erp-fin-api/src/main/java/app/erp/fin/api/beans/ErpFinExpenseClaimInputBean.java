//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinExpenseClaimInputBean extends CrudInputBase {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _claimantId;

    
        @PropMeta(propId=4)
    
        public Long getClaimantId(){
            return _claimantId;
        }

        public void setClaimantId(Long value){
            this._claimantId = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=5)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
            this._departmentId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _paymentMode;

    
        @PropMeta(propId=7)
    
        public String getPaymentMode(){
            return _paymentMode;
        }

        public void setPaymentMode(String value){
            this._paymentMode = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=8)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private java.math.BigDecimal _amountWithoutTax;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAmountWithoutTax(){
            return _amountWithoutTax;
        }

        public void setAmountWithoutTax(java.math.BigDecimal value){
            this._amountWithoutTax = value;
        }


        private java.math.BigDecimal _taxAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getTaxAmount(){
            return _taxAmount;
        }

        public void setTaxAmount(java.math.BigDecimal value){
            this._taxAmount = value;
        }


        private java.math.BigDecimal _amountWithTax;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getAmountWithTax(){
            return _amountWithTax;
        }

        public void setAmountWithTax(java.math.BigDecimal value){
            this._amountWithTax = value;
        }


        private Long _settleAdvanceId;

    
        @PropMeta(propId=15)
    
        public Long getSettleAdvanceId(){
            return _settleAdvanceId;
        }

        public void setSettleAdvanceId(Long value){
            this._settleAdvanceId = value;
        }


        private String _reason;

    
        @PropMeta(propId=16)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=17)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=18)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=24)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpFinExpenseClaimLineInputBean> _lines;

        public List<ErpFinExpenseClaimLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpFinExpenseClaimLineInputBean> value){
            this._lines = value;
        }


    }
