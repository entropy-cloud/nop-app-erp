//__XGEN_FORCE_OVERRIDE__
    package app.erp.sal.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSalOrderLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orderId;

    
        @PropMeta(propId=2)
    
        public Long getOrderId(){
            return _orderId;
        }

        public void setOrderId(Long value){
            this._orderId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=4)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _skuId;

    
        @PropMeta(propId=5)
    
        public Long getSkuId(){
            return _skuId;
        }

        public void setSkuId(Long value){
            this._skuId = value;
        }


        private Long _uoMId;

    
        @PropMeta(propId=6)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
        }


        private java.math.BigDecimal _quantity;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getQuantity(){
            return _quantity;
        }

        public void setQuantity(java.math.BigDecimal value){
            this._quantity = value;
        }


        private java.math.BigDecimal _unitPrice;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getUnitPrice(){
            return _unitPrice;
        }

        public void setUnitPrice(java.math.BigDecimal value){
            this._unitPrice = value;
        }


        private java.math.BigDecimal _taxRate;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getTaxRate(){
            return _taxRate;
        }

        public void setTaxRate(java.math.BigDecimal value){
            this._taxRate = value;
        }


        private Long _taxRateId;

    
        @PropMeta(propId=10)
    
        public Long getTaxRateId(){
            return _taxRateId;
        }

        public void setTaxRateId(Long value){
            this._taxRateId = value;
        }


        private java.math.BigDecimal _taxAmount;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getTaxAmount(){
            return _taxAmount;
        }

        public void setTaxAmount(java.math.BigDecimal value){
            this._taxAmount = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
            this._amount = value;
        }


        private java.math.BigDecimal _amountWithTax;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getAmountWithTax(){
            return _amountWithTax;
        }

        public void setAmountWithTax(java.math.BigDecimal value){
            this._amountWithTax = value;
        }


        private java.math.BigDecimal _deliveredQuantity;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getDeliveredQuantity(){
            return _deliveredQuantity;
        }

        public void setDeliveredQuantity(java.math.BigDecimal value){
            this._deliveredQuantity = value;
        }


        private java.math.BigDecimal _invoicedQuantity;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getInvoicedQuantity(){
            return _invoicedQuantity;
        }

        public void setInvoicedQuantity(java.math.BigDecimal value){
            this._invoicedQuantity = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=16)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=17)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private String _remark;

    
        @PropMeta(propId=24)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.math.BigDecimal _discountRate;

    
        @PropMeta(propId=100)
    
        public java.math.BigDecimal getDiscountRate(){
            return _discountRate;
        }

        public void setDiscountRate(java.math.BigDecimal value){
            this._discountRate = value;
        }


        private java.math.BigDecimal _discountAmount;

    
        @PropMeta(propId=101)
    
        public java.math.BigDecimal getDiscountAmount(){
            return _discountAmount;
        }

        public void setDiscountAmount(java.math.BigDecimal value){
            this._discountAmount = value;
        }


        private String _pricingSource;

    
        @PropMeta(propId=102)
    
        public String getPricingSource(){
            return _pricingSource;
        }

        public void setPricingSource(String value){
            this._pricingSource = value;
        }


    }
