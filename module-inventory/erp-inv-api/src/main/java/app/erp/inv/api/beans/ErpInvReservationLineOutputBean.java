//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvReservationLineOutputBean {

    
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


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _reservation;

        public Map<String,Object> getReservation(){
            return _reservation;
        }

        public void setReservation(Map<String,Object> value){
            this._reservation = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _sku;

        public Map<String,Object> getSku(){
            return _sku;
        }

        public void setSku(Map<String,Object> value){
            this._sku = value;
        }


        private Map<String,Object> _warehouse;

        public Map<String,Object> getWarehouse(){
            return _warehouse;
        }

        public void setWarehouse(Map<String,Object> value){
            this._warehouse = value;
        }


        private Map<String,Object> _location;

        public Map<String,Object> getLocation(){
            return _location;
        }

        public void setLocation(Map<String,Object> value){
            this._location = value;
        }


        private Map<String,Object> _uom;

        public Map<String,Object> getUom(){
            return _uom;
        }

        public void setUom(Map<String,Object> value){
            this._uom = value;
        }


    }
