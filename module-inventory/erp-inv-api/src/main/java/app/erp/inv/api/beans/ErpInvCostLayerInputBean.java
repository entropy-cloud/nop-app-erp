//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvCostLayerInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=3)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _skuId;

    
        @PropMeta(propId=4)
    
        public Long getSkuId(){
            return _skuId;
        }

        public void setSkuId(Long value){
            this._skuId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=5)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=6)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private String _costMethod;

    
        @PropMeta(propId=7)
    
        public String getCostMethod(){
            return _costMethod;
        }

        public void setCostMethod(String value){
            this._costMethod = value;
        }


        private java.math.BigDecimal _incomingQuantity;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getIncomingQuantity(){
            return _incomingQuantity;
        }

        public void setIncomingQuantity(java.math.BigDecimal value){
            this._incomingQuantity = value;
        }


        private java.math.BigDecimal _remainingQuantity;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getRemainingQuantity(){
            return _remainingQuantity;
        }

        public void setRemainingQuantity(java.math.BigDecimal value){
            this._remainingQuantity = value;
        }


        private java.math.BigDecimal _unitCost;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(java.math.BigDecimal value){
            this._unitCost = value;
        }


        private java.math.BigDecimal _totalCost;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(java.math.BigDecimal value){
            this._totalCost = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=12)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.time.LocalDate _incomingDate;

    
        @PropMeta(propId=13)
    
        public java.time.LocalDate getIncomingDate(){
            return _incomingDate;
        }

        public void setIncomingDate(java.time.LocalDate value){
            this._incomingDate = value;
        }


        private Long _incomingMoveId;

    
        @PropMeta(propId=14)
    
        public Long getIncomingMoveId(){
            return _incomingMoveId;
        }

        public void setIncomingMoveId(Long value){
            this._incomingMoveId = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=15)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
