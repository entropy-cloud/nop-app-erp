//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvStockMoveLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _moveId;

    
        @PropMeta(propId=2)
    
        public String getMoveId(){
            return _moveId;
        }

        public void setMoveId(String value){
            this._moveId = value;
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


        private java.math.BigDecimal _unitCost;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(java.math.BigDecimal value){
            this._unitCost = value;
        }


        private java.math.BigDecimal _totalCost;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(java.math.BigDecimal value){
            this._totalCost = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=10)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=11)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private String _serialNo;

    
        @PropMeta(propId=12)
    
        public String getSerialNo(){
            return _serialNo;
        }

        public void setSerialNo(String value){
            this._serialNo = value;
        }


        private String _sourceLocationId;

    
        @PropMeta(propId=13)
    
        public String getSourceLocationId(){
            return _sourceLocationId;
        }

        public void setSourceLocationId(String value){
            this._sourceLocationId = value;
        }


        private String _destLocationId;

    
        @PropMeta(propId=14)
    
        public String getDestLocationId(){
            return _destLocationId;
        }

        public void setDestLocationId(String value){
            this._destLocationId = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
