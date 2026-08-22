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
    public class ErpSalDeliveryInputBean extends CrudInputBase {

    
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _orderId;

    
        @PropMeta(propId=4)
    
        public String getOrderId(){
            return _orderId;
        }

        public void setOrderId(String value){
            this._orderId = value;
        }


        private String _customerId;

    
        @PropMeta(propId=5)
    
        public String getCustomerId(){
            return _customerId;
        }

        public void setCustomerId(String value){
            this._customerId = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=6)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=8)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
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


        private String _remark;

    
        @PropMeta(propId=28)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _exchangeReturnId;

    
        @PropMeta(propId=29)
    
        public String getExchangeReturnId(){
            return _exchangeReturnId;
        }

        public void setExchangeReturnId(String value){
            this._exchangeReturnId = value;
        }


        private List<ErpSalDeliveryLineInputBean> _lines;

        public List<ErpSalDeliveryLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpSalDeliveryLineInputBean> value){
            this._lines = value;
        }


    }
