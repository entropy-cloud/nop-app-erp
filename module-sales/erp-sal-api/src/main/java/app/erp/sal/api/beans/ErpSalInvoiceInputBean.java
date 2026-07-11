//__XGEN_FORCE_OVERRIDE__
    package app.erp.sal.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSalInvoiceInputBean extends CrudInputBase {

    
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


        private Long _customerId;

    
        @PropMeta(propId=4)
    
        public Long getCustomerId(){
            return _customerId;
        }

        public void setCustomerId(Long value){
            this._customerId = value;
        }


        private String _invoiceNo;

    
        @PropMeta(propId=5)
    
        public String getInvoiceNo(){
            return _invoiceNo;
        }

        public void setInvoiceNo(String value){
            this._invoiceNo = value;
        }


        private String _invoiceType;

    
        @PropMeta(propId=6)
    
        public String getInvoiceType(){
            return _invoiceType;
        }

        public void setInvoiceType(String value){
            this._invoiceType = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
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


        private java.math.BigDecimal _totalAmount;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal value){
            this._totalAmount = value;
        }


        private java.math.BigDecimal _totalTaxAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getTotalTaxAmount(){
            return _totalTaxAmount;
        }

        public void setTotalTaxAmount(java.math.BigDecimal value){
            this._totalTaxAmount = value;
        }


        private java.math.BigDecimal _totalAmountWithTax;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getTotalAmountWithTax(){
            return _totalAmountWithTax;
        }

        public void setTotalAmountWithTax(java.math.BigDecimal value){
            this._totalAmountWithTax = value;
        }


        private java.math.BigDecimal _receivedAmount;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getReceivedAmount(){
            return _receivedAmount;
        }

        public void setReceivedAmount(java.math.BigDecimal value){
            this._receivedAmount = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=16)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=17)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _receivedStatus;

    
        @PropMeta(propId=18)
    
        public String getReceivedStatus(){
            return _receivedStatus;
        }

        public void setReceivedStatus(String value){
            this._receivedStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=30)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpSalInvoiceLineInputBean> _lines;

        public List<ErpSalInvoiceLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpSalInvoiceLineInputBean> value){
            this._lines = value;
        }


    }
