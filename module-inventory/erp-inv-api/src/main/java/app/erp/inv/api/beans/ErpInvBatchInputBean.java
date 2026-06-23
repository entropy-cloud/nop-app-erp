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


        private String _batchNo;

    
        @PropMeta(propId=3)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
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


        private Long _warehouseId;

    
        @PropMeta(propId=6)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private String _totalQuantity;

    
        @PropMeta(propId=7)
    
        public String getTotalQuantity(){
            return _totalQuantity;
        }

        public void setTotalQuantity(String value){
            this._totalQuantity = value;
        }


        private String _availableQuantity;

    
        @PropMeta(propId=8)
    
        public String getAvailableQuantity(){
            return _availableQuantity;
        }

        public void setAvailableQuantity(String value){
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


        private Integer _status;

    
        @PropMeta(propId=12)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
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


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
