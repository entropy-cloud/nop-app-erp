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


        private Long _moveId;

    
        @PropMeta(propId=4)
    
        public Long getMoveId(){
            return _moveId;
        }

        public void setMoveId(Long value){
            this._moveId = value;
        }


        private Long _moveLineId;

    
        @PropMeta(propId=5)
    
        public Long getMoveLineId(){
            return _moveLineId;
        }

        public void setMoveLineId(Long value){
            this._moveLineId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=6)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _skuId;

    
        @PropMeta(propId=7)
    
        public Long getSkuId(){
            return _skuId;
        }

        public void setSkuId(Long value){
            this._skuId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=8)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _locationId;

    
        @PropMeta(propId=9)
    
        public Long getLocationId(){
            return _locationId;
        }

        public void setLocationId(Long value){
            this._locationId = value;
        }


        private String _quantity;

    
        @PropMeta(propId=10)
    
        public String getQuantity(){
            return _quantity;
        }

        public void setQuantity(String value){
            this._quantity = value;
        }


        private String _unitCost;

    
        @PropMeta(propId=11)
    
        public String getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(String value){
            this._unitCost = value;
        }


        private String _totalCost;

    
        @PropMeta(propId=12)
    
        public String getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(String value){
            this._totalCost = value;
        }


        private String _balanceQuantity;

    
        @PropMeta(propId=13)
    
        public String getBalanceQuantity(){
            return _balanceQuantity;
        }

        public void setBalanceQuantity(String value){
            this._balanceQuantity = value;
        }


        private String _balanceTotalCost;

    
        @PropMeta(propId=14)
    
        public String getBalanceTotalCost(){
            return _balanceTotalCost;
        }

        public void setBalanceTotalCost(String value){
            this._balanceTotalCost = value;
        }


        private Integer _costMethod;

    
        @PropMeta(propId=15)
    
        public Integer getCostMethod(){
            return _costMethod;
        }

        public void setCostMethod(Integer value){
            this._costMethod = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=16)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=17)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
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


        private Long _delVersion;

    
        @PropMeta(propId=21)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
