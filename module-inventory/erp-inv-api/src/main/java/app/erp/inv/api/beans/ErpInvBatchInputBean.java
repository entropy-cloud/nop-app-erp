//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvBatchInputBean extends CrudInputBase {

    
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


        private String _batchNo;

    
        @PropMeta(propId=3)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
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


        private String _warehouseId;

    
        @PropMeta(propId=6)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private java.math.BigDecimal _totalQuantity;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getTotalQuantity(){
            return _totalQuantity;
        }

        public void setTotalQuantity(java.math.BigDecimal value){
            this._totalQuantity = value;
        }


        private java.math.BigDecimal _availableQuantity;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getAvailableQuantity(){
            return _availableQuantity;
        }

        public void setAvailableQuantity(java.math.BigDecimal value){
            this._availableQuantity = value;
        }


        private java.time.LocalDate _productionDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getProductionDate(){
            return _productionDate;
        }

        public void setProductionDate(java.time.LocalDate value){
            this._productionDate = value;
        }


        private java.time.LocalDate _expiryDate;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getExpiryDate(){
            return _expiryDate;
        }

        public void setExpiryDate(java.time.LocalDate value){
            this._expiryDate = value;
        }


        private Integer _shelfLifeDays;

    
        @PropMeta(propId=11)
    
        public Integer getShelfLifeDays(){
            return _shelfLifeDays;
        }

        public void setShelfLifeDays(Integer value){
            this._shelfLifeDays = value;
        }


        private String _status;

    
        @PropMeta(propId=12)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
