//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvStockTakeLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _takeId;

    
        @PropMeta(propId=2)
    
        public Long getTakeId(){
            return _takeId;
        }

        public void setTakeId(Long value){
            this._takeId = value;
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


        private Long _uoMId;

    
        @PropMeta(propId=6)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
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


        private java.math.BigDecimal _bookQuantity;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getBookQuantity(){
            return _bookQuantity;
        }

        public void setBookQuantity(java.math.BigDecimal value){
            this._bookQuantity = value;
        }


        private java.math.BigDecimal _actualQuantity;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getActualQuantity(){
            return _actualQuantity;
        }

        public void setActualQuantity(java.math.BigDecimal value){
            this._actualQuantity = value;
        }


        private java.math.BigDecimal _differenceQuantity;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getDifferenceQuantity(){
            return _differenceQuantity;
        }

        public void setDifferenceQuantity(java.math.BigDecimal value){
            this._differenceQuantity = value;
        }


        private java.math.BigDecimal _unitCost;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(java.math.BigDecimal value){
            this._unitCost = value;
        }


        private java.math.BigDecimal _differenceAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getDifferenceAmount(){
            return _differenceAmount;
        }

        public void setDifferenceAmount(java.math.BigDecimal value){
            this._differenceAmount = value;
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
