//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpDrpParameterInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=2)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=3)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private java.math.BigDecimal _safetyStock;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getSafetyStock(){
            return _safetyStock;
        }

        public void setSafetyStock(java.math.BigDecimal value){
            this._safetyStock = value;
        }


        private Integer _replenishmentLeadTime;

    
        @PropMeta(propId=5)
    
        public Integer getReplenishmentLeadTime(){
            return _replenishmentLeadTime;
        }

        public void setReplenishmentLeadTime(Integer value){
            this._replenishmentLeadTime = value;
        }


        private java.math.BigDecimal _orderMultiple;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getOrderMultiple(){
            return _orderMultiple;
        }

        public void setOrderMultiple(java.math.BigDecimal value){
            this._orderMultiple = value;
        }


        private Long _preferredSourceWarehouseId;

    
        @PropMeta(propId=7)
    
        public Long getPreferredSourceWarehouseId(){
            return _preferredSourceWarehouseId;
        }

        public void setPreferredSourceWarehouseId(Long value){
            this._preferredSourceWarehouseId = value;
        }


        private Long _preferredSupplierId;

    
        @PropMeta(propId=8)
    
        public Long getPreferredSupplierId(){
            return _preferredSupplierId;
        }

        public void setPreferredSupplierId(Long value){
            this._preferredSupplierId = value;
        }


        private String _replenishmentMethod;

    
        @PropMeta(propId=9)
    
        public String getReplenishmentMethod(){
            return _replenishmentMethod;
        }

        public void setReplenishmentMethod(String value){
            this._replenishmentMethod = value;
        }


        private java.math.BigDecimal _minStockLevel;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getMinStockLevel(){
            return _minStockLevel;
        }

        public void setMinStockLevel(java.math.BigDecimal value){
            this._minStockLevel = value;
        }


        private java.math.BigDecimal _maxStockLevel;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getMaxStockLevel(){
            return _maxStockLevel;
        }

        public void setMaxStockLevel(java.math.BigDecimal value){
            this._maxStockLevel = value;
        }


        private Integer _reviewPeriodDays;

    
        @PropMeta(propId=12)
    
        public Integer getReviewPeriodDays(){
            return _reviewPeriodDays;
        }

        public void setReviewPeriodDays(Integer value){
            this._reviewPeriodDays = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=13)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
