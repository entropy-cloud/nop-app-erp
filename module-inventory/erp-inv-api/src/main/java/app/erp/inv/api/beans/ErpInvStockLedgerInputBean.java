//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvStockLedgerInputBean extends CrudInputBase {

    
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


        private String _moveId;

    
        @PropMeta(propId=4)
    
        public String getMoveId(){
            return _moveId;
        }

        public void setMoveId(String value){
            this._moveId = value;
        }


        private String _moveLineId;

    
        @PropMeta(propId=5)
    
        public String getMoveLineId(){
            return _moveLineId;
        }

        public void setMoveLineId(String value){
            this._moveLineId = value;
        }


        private String _materialId;

    
        @PropMeta(propId=6)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _skuId;

    
        @PropMeta(propId=7)
    
        public String getSkuId(){
            return _skuId;
        }

        public void setSkuId(String value){
            this._skuId = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=8)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _locationId;

    
        @PropMeta(propId=9)
    
        public String getLocationId(){
            return _locationId;
        }

        public void setLocationId(String value){
            this._locationId = value;
        }


        private java.math.BigDecimal _quantity;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getQuantity(){
            return _quantity;
        }

        public void setQuantity(java.math.BigDecimal value){
            this._quantity = value;
        }


        private java.math.BigDecimal _unitCost;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(java.math.BigDecimal value){
            this._unitCost = value;
        }


        private java.math.BigDecimal _totalCost;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(java.math.BigDecimal value){
            this._totalCost = value;
        }


        private java.math.BigDecimal _balanceQuantity;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getBalanceQuantity(){
            return _balanceQuantity;
        }

        public void setBalanceQuantity(java.math.BigDecimal value){
            this._balanceQuantity = value;
        }


        private java.math.BigDecimal _balanceTotalCost;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getBalanceTotalCost(){
            return _balanceTotalCost;
        }

        public void setBalanceTotalCost(java.math.BigDecimal value){
            this._balanceTotalCost = value;
        }


        private String _costMethod;

    
        @PropMeta(propId=15)
    
        public String getCostMethod(){
            return _costMethod;
        }

        public void setCostMethod(String value){
            this._costMethod = value;
        }


        private String _acctSchemaId;

    
        @PropMeta(propId=16)
    
        public String getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(String value){
            this._acctSchemaId = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=17)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=19)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private String _serialNo;

    
        @PropMeta(propId=20)
    
        public String getSerialNo(){
            return _serialNo;
        }

        public void setSerialNo(String value){
            this._serialNo = value;
        }


        private String _ownerId;

    
        @PropMeta(propId=27)
    
        public String getOwnerId(){
            return _ownerId;
        }

        public void setOwnerId(String value){
            this._ownerId = value;
        }


        private String _ownershipType;

    
        @PropMeta(propId=28)
    
        public String getOwnershipType(){
            return _ownershipType;
        }

        public void setOwnershipType(String value){
            this._ownershipType = value;
        }


    }
