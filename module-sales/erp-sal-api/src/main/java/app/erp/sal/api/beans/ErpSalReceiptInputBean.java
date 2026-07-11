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
    public class ErpSalReceiptInputBean extends CrudInputBase {

    
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


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=6)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private java.math.BigDecimal _totalAmount;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal value){
            this._totalAmount = value;
        }


        private Long _settlementMethodId;

    
        @PropMeta(propId=11)
    
        public Long getSettlementMethodId(){
            return _settlementMethodId;
        }

        public void setSettlementMethodId(Long value){
            this._settlementMethodId = value;
        }


        private String _receiptMethod;

    
        @PropMeta(propId=12)
    
        public String getReceiptMethod(){
            return _receiptMethod;
        }

        public void setReceiptMethod(String value){
            this._receiptMethod = value;
        }


        private Long _bankAccountId;

    
        @PropMeta(propId=13)
    
        public Long getBankAccountId(){
            return _bankAccountId;
        }

        public void setBankAccountId(Long value){
            this._bankAccountId = value;
        }


        private Long _partnerBankAccountId;

    
        @PropMeta(propId=14)
    
        public Long getPartnerBankAccountId(){
            return _partnerBankAccountId;
        }

        public void setPartnerBankAccountId(Long value){
            this._partnerBankAccountId = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=15)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=16)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _writtenOffStatus;

    
        @PropMeta(propId=17)
    
        public String getWrittenOffStatus(){
            return _writtenOffStatus;
        }

        public void setWrittenOffStatus(String value){
            this._writtenOffStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=29)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpSalReceiptLineInputBean> _lines;

        public List<ErpSalReceiptLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpSalReceiptLineInputBean> value){
            this._lines = value;
        }


    }
