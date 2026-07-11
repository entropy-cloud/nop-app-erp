//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurOrderInputBean extends CrudInputBase {

    
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


        private Long _requisitionId;

    
        @PropMeta(propId=4)
    
        public Long getRequisitionId(){
            return _requisitionId;
        }

        public void setRequisitionId(Long value){
            this._requisitionId = value;
        }


        private Long _quotationId;

    
        @PropMeta(propId=5)
    
        public Long getQuotationId(){
            return _quotationId;
        }

        public void setQuotationId(Long value){
            this._quotationId = value;
        }


        private Long _supplierId;

    
        @PropMeta(propId=6)
    
        public Long getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(Long value){
            this._supplierId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=7)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.time.LocalDate _deliveryDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getDeliveryDate(){
            return _deliveryDate;
        }

        public void setDeliveryDate(java.time.LocalDate value){
            this._deliveryDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=10)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private java.math.BigDecimal _totalAmount;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal value){
            this._totalAmount = value;
        }


        private java.math.BigDecimal _totalTaxAmount;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getTotalTaxAmount(){
            return _totalTaxAmount;
        }

        public void setTotalTaxAmount(java.math.BigDecimal value){
            this._totalTaxAmount = value;
        }


        private java.math.BigDecimal _totalAmountWithTax;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getTotalAmountWithTax(){
            return _totalAmountWithTax;
        }

        public void setTotalAmountWithTax(java.math.BigDecimal value){
            this._totalAmountWithTax = value;
        }


        private java.math.BigDecimal _discountRate;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getDiscountRate(){
            return _discountRate;
        }

        public void setDiscountRate(java.math.BigDecimal value){
            this._discountRate = value;
        }


        private java.math.BigDecimal _discountAmount;

    
        @PropMeta(propId=18)
    
        public java.math.BigDecimal getDiscountAmount(){
            return _discountAmount;
        }

        public void setDiscountAmount(java.math.BigDecimal value){
            this._discountAmount = value;
        }


        private java.math.BigDecimal _paidAmount;

    
        @PropMeta(propId=19)
    
        public java.math.BigDecimal getPaidAmount(){
            return _paidAmount;
        }

        public void setPaidAmount(java.math.BigDecimal value){
            this._paidAmount = value;
        }


        private Long _settlementMethodId;

    
        @PropMeta(propId=20)
    
        public Long getSettlementMethodId(){
            return _settlementMethodId;
        }

        public void setSettlementMethodId(Long value){
            this._settlementMethodId = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=21)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=22)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _paidStatus;

    
        @PropMeta(propId=23)
    
        public String getPaidStatus(){
            return _paidStatus;
        }

        public void setPaidStatus(String value){
            this._paidStatus = value;
        }


        private String _receiveStatus;

    
        @PropMeta(propId=24)
    
        public String getReceiveStatus(){
            return _receiveStatus;
        }

        public void setReceiveStatus(String value){
            this._receiveStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=30)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpPurOrderLineInputBean> _lines;

        public List<ErpPurOrderLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpPurOrderLineInputBean> value){
            this._lines = value;
        }


    }
