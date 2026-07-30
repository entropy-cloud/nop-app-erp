//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpDrpLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _planId;

    
        @PropMeta(propId=2)
    
        public Long getPlanId(){
            return _planId;
        }

        public void setPlanId(Long value){
            this._planId = value;
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


        private Long _warehouseId;

    
        @PropMeta(propId=5)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _sourceWarehouseId;

    
        @PropMeta(propId=6)
    
        public Long getSourceWarehouseId(){
            return _sourceWarehouseId;
        }

        public void setSourceWarehouseId(Long value){
            this._sourceWarehouseId = value;
        }


        private String _replenishmentType;

    
        @PropMeta(propId=7)
    
        public String getReplenishmentType(){
            return _replenishmentType;
        }

        public void setReplenishmentType(String value){
            this._replenishmentType = value;
        }


        private java.math.BigDecimal _currentStock;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getCurrentStock(){
            return _currentStock;
        }

        public void setCurrentStock(java.math.BigDecimal value){
            this._currentStock = value;
        }


        private java.math.BigDecimal _allocatedQty;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getAllocatedQty(){
            return _allocatedQty;
        }

        public void setAllocatedQty(java.math.BigDecimal value){
            this._allocatedQty = value;
        }


        private java.math.BigDecimal _onOrderQty;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getOnOrderQty(){
            return _onOrderQty;
        }

        public void setOnOrderQty(java.math.BigDecimal value){
            this._onOrderQty = value;
        }


        private java.math.BigDecimal _forecastDemand;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getForecastDemand(){
            return _forecastDemand;
        }

        public void setForecastDemand(java.math.BigDecimal value){
            this._forecastDemand = value;
        }


        private java.math.BigDecimal _safetyStock;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getSafetyStock(){
            return _safetyStock;
        }

        public void setSafetyStock(java.math.BigDecimal value){
            this._safetyStock = value;
        }


        private java.math.BigDecimal _netRequirement;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getNetRequirement(){
            return _netRequirement;
        }

        public void setNetRequirement(java.math.BigDecimal value){
            this._netRequirement = value;
        }


        private java.math.BigDecimal _suggestedQty;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getSuggestedQty(){
            return _suggestedQty;
        }

        public void setSuggestedQty(java.math.BigDecimal value){
            this._suggestedQty = value;
        }


        private java.math.BigDecimal _approvedQty;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getApprovedQty(){
            return _approvedQty;
        }

        public void setApprovedQty(java.math.BigDecimal value){
            this._approvedQty = value;
        }


        private String _orderBillType;

    
        @PropMeta(propId=16)
    
        public String getOrderBillType(){
            return _orderBillType;
        }

        public void setOrderBillType(String value){
            this._orderBillType = value;
        }


        private String _orderBillCode;

    
        @PropMeta(propId=17)
    
        public String getOrderBillCode(){
            return _orderBillCode;
        }

        public void setOrderBillCode(String value){
            this._orderBillCode = value;
        }


        private String _status;

    
        @PropMeta(propId=18)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=19)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
