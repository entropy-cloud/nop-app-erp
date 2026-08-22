//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurOrderLineOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _orderId;

    
        @PropMeta(propId=2)
    
        public String getOrderId(){
            return _orderId;
        }

        public void setOrderId(String value){
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


        private String _materialId;

    
        @PropMeta(propId=4)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _skuId;

    
        @PropMeta(propId=5)
    
        public String getSkuId(){
            return _skuId;
        }

        public void setSkuId(String value){
            this._skuId = value;
        }


        private String _uoMId;

    
        @PropMeta(propId=6)
    
        public String getUoMId(){
            return _uoMId;
        }

        public void setUoMId(String value){
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


        private String _taxRateId;

    
        @PropMeta(propId=10)
    
        public String getTaxRateId(){
            return _taxRateId;
        }

        public void setTaxRateId(String value){
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


        private java.math.BigDecimal _receivedQuantity;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getReceivedQuantity(){
            return _receivedQuantity;
        }

        public void setReceivedQuantity(java.math.BigDecimal value){
            this._receivedQuantity = value;
        }


        private java.math.BigDecimal _invoicedQuantity;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getInvoicedQuantity(){
            return _invoicedQuantity;
        }

        public void setInvoicedQuantity(java.math.BigDecimal value){
            this._invoicedQuantity = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=16)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _projectId;

    
        @PropMeta(propId=17)
    
        public String getProjectId(){
            return _projectId;
        }

        public void setProjectId(String value){
            this._projectId = value;
        }


        private String _remark;

    
        @PropMeta(propId=18)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=19)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=20)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=21)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=23)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _ctContractLineId;

    
        @PropMeta(propId=25)
    
        public String getCtContractLineId(){
            return _ctContractLineId;
        }

        public void setCtContractLineId(String value){
            this._ctContractLineId = value;
        }


        private Map<String,Object> _order;

        public Map<String,Object> getOrder(){
            return _order;
        }

        public void setOrder(Map<String,Object> value){
            this._order = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _sku;

        public Map<String,Object> getSku(){
            return _sku;
        }

        public void setSku(Map<String,Object> value){
            this._sku = value;
        }


        private Map<String,Object> _uoM;

        public Map<String,Object> getUoM(){
            return _uoM;
        }

        public void setUoM(Map<String,Object> value){
            this._uoM = value;
        }


        private Map<String,Object> _taxRateMd;

        public Map<String,Object> getTaxRateMd(){
            return _taxRateMd;
        }

        public void setTaxRateMd(Map<String,Object> value){
            this._taxRateMd = value;
        }


        private Map<String,Object> _warehouse;

        public Map<String,Object> getWarehouse(){
            return _warehouse;
        }

        public void setWarehouse(Map<String,Object> value){
            this._warehouse = value;
        }


    }
