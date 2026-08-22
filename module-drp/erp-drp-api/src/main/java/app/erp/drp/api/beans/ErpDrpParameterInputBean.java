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

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=2)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _materialId;

    
        @PropMeta(propId=3)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
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


        private String _preferredSourceWarehouseId;

    
        @PropMeta(propId=7)
    
        public String getPreferredSourceWarehouseId(){
            return _preferredSourceWarehouseId;
        }

        public void setPreferredSourceWarehouseId(String value){
            this._preferredSourceWarehouseId = value;
        }


        private String _preferredSupplierId;

    
        @PropMeta(propId=8)
    
        public String getPreferredSupplierId(){
            return _preferredSupplierId;
        }

        public void setPreferredSupplierId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=13)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
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
