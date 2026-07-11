//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvStockBalanceInputBean extends CrudInputBase {

    
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


        private Long _locationId;

    
        @PropMeta(propId=6)
    
        public Long getLocationId(){
            return _locationId;
        }

        public void setLocationId(Long value){
            this._locationId = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=7)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private java.math.BigDecimal _totalQuantity;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getTotalQuantity(){
            return _totalQuantity;
        }

        public void setTotalQuantity(java.math.BigDecimal value){
            this._totalQuantity = value;
        }


        private java.math.BigDecimal _reservedQuantity;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getReservedQuantity(){
            return _reservedQuantity;
        }

        public void setReservedQuantity(java.math.BigDecimal value){
            this._reservedQuantity = value;
        }


        private java.math.BigDecimal _lockedQuantity;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getLockedQuantity(){
            return _lockedQuantity;
        }

        public void setLockedQuantity(java.math.BigDecimal value){
            this._lockedQuantity = value;
        }


        private java.math.BigDecimal _availableQuantity;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getAvailableQuantity(){
            return _availableQuantity;
        }

        public void setAvailableQuantity(java.math.BigDecimal value){
            this._availableQuantity = value;
        }


        private String _costMethod;

    
        @PropMeta(propId=12)
    
        public String getCostMethod(){
            return _costMethod;
        }

        public void setCostMethod(String value){
            this._costMethod = value;
        }


        private java.math.BigDecimal _avgCost;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getAvgCost(){
            return _avgCost;
        }

        public void setAvgCost(java.math.BigDecimal value){
            this._avgCost = value;
        }


        private java.math.BigDecimal _totalCost;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(java.math.BigDecimal value){
            this._totalCost = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=15)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private Long _ownerId;

    
        @PropMeta(propId=22)
    
        public Long getOwnerId(){
            return _ownerId;
        }

        public void setOwnerId(Long value){
            this._ownerId = value;
        }


        private String _ownershipType;

    
        @PropMeta(propId=23)
    
        public String getOwnershipType(){
            return _ownershipType;
        }

        public void setOwnershipType(String value){
            this._ownershipType = value;
        }


    }
