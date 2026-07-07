//__XGEN_FORCE_OVERRIDE__
    package app.erp.sal.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSalOrderLineOutputBean {

    
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


        private Long _delVersion;

    
        @PropMeta(propId=18)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=19)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=20)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=22)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=24)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
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


        private Map<String,Object> _project;

        public Map<String,Object> getProject(){
            return _project;
        }

        public void setProject(Map<String,Object> value){
            this._project = value;
        }


    }
