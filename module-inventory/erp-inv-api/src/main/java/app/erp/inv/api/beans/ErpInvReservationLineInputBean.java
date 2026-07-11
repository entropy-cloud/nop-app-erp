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

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _reservationId;

    
        @PropMeta(propId=2)
    
        public Long getReservationId(){
            return _reservationId;
        }

        public void setReservationId(Long value){
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


        private Long _locationId;

    
        @PropMeta(propId=7)
    
        public Long getLocationId(){
            return _locationId;
        }

        public void setLocationId(Long value){
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


        private Long _uomId;

    
        @PropMeta(propId=11)
    
        public Long getUomId(){
            return _uomId;
        }

        public void setUomId(Long value){
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
