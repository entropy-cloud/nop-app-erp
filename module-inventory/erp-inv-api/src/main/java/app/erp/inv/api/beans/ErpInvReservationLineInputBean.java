//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvReservationLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _reservationId;

    
        @PropMeta(propId=2)
    
        public String getReservationId(){
            return _reservationId;
        }

        public void setReservationId(String value){
            this._reservationId = value;
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


        private String _warehouseId;

    
        @PropMeta(propId=6)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _locationId;

    
        @PropMeta(propId=7)
    
        public String getLocationId(){
            return _locationId;
        }

        public void setLocationId(String value){
            this._locationId = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=8)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private java.math.BigDecimal _reservedQuantity;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getReservedQuantity(){
            return _reservedQuantity;
        }

        public void setReservedQuantity(java.math.BigDecimal value){
            this._reservedQuantity = value;
        }


        private java.math.BigDecimal _consumedQuantity;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getConsumedQuantity(){
            return _consumedQuantity;
        }

        public void setConsumedQuantity(java.math.BigDecimal value){
            this._consumedQuantity = value;
        }


        private String _uomId;

    
        @PropMeta(propId=11)
    
        public String getUomId(){
            return _uomId;
        }

        public void setUomId(String value){
            this._uomId = value;
        }


        private String _sourceLineCode;

    
        @PropMeta(propId=12)
    
        public String getSourceLineCode(){
            return _sourceLineCode;
        }

        public void setSourceLineCode(String value){
            this._sourceLineCode = value;
        }


    }
