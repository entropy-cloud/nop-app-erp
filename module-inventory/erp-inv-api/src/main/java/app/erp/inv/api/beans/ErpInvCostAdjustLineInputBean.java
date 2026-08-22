//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvCostAdjustLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _adjustId;

    
        @PropMeta(propId=2)
    
        public String getAdjustId(){
            return _adjustId;
        }

        public void setAdjustId(String value){
            this._adjustId = value;
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


        private String _warehouseId;

    
        @PropMeta(propId=5)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=6)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private java.math.BigDecimal _oldUnitCost;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getOldUnitCost(){
            return _oldUnitCost;
        }

        public void setOldUnitCost(java.math.BigDecimal value){
            this._oldUnitCost = value;
        }


        private java.math.BigDecimal _newUnitCost;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getNewUnitCost(){
            return _newUnitCost;
        }

        public void setNewUnitCost(java.math.BigDecimal value){
            this._newUnitCost = value;
        }


        private java.math.BigDecimal _adjustQty;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getAdjustQty(){
            return _adjustQty;
        }

        public void setAdjustQty(java.math.BigDecimal value){
            this._adjustQty = value;
        }


        private java.math.BigDecimal _adjustAmount;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getAdjustAmount(){
            return _adjustAmount;
        }

        public void setAdjustAmount(java.math.BigDecimal value){
            this._adjustAmount = value;
        }


        private String _adjustReason;

    
        @PropMeta(propId=11)
    
        public String getAdjustReason(){
            return _adjustReason;
        }

        public void setAdjustReason(String value){
            this._adjustReason = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=12)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
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
