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

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _orgId;

    
        @PropMeta(propId=2)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _materialId;

    
        @PropMeta(propId=3)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _skuId;

    
        @PropMeta(propId=4)
    
        public String getSkuId(){
            return _skuId;
        }

        public void setSkuId(String value){
            this._skuId = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=5)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
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


        private String _currencyId;

    
        @PropMeta(propId=12)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
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


        private String _incomingMoveId;

    
        @PropMeta(propId=14)
    
        public String getIncomingMoveId(){
            return _incomingMoveId;
        }

        public void setIncomingMoveId(String value){
            this._incomingMoveId = value;
        }


        private String _acctSchemaId;

    
        @PropMeta(propId=15)
    
        public String getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(String value){
            this._acctSchemaId = value;
        }


    }
