//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgWorkOrderBomLineSnapshotInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _snapshotId;

    
        @PropMeta(propId=2)
    
        public String getSnapshotId(){
            return _snapshotId;
        }

        public void setSnapshotId(String value){
            this._snapshotId = value;
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


        private String _operationId;

    
        @PropMeta(propId=8)
    
        public String getOperationId(){
            return _operationId;
        }

        public void setOperationId(String value){
            this._operationId = value;
        }


        private java.math.BigDecimal _scrapRate;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getScrapRate(){
            return _scrapRate;
        }

        public void setScrapRate(java.math.BigDecimal value){
            this._scrapRate = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=10)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _alternativeMaterialId;

    
        @PropMeta(propId=11)
    
        public String getAlternativeMaterialId(){
            return _alternativeMaterialId;
        }

        public void setAlternativeMaterialId(String value){
            this._alternativeMaterialId = value;
        }


    }
