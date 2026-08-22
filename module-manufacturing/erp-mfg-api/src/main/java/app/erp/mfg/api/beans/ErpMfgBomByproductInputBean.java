//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgBomByproductInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _bomId;

    
        @PropMeta(propId=2)
    
        public String getBomId(){
            return _bomId;
        }

        public void setBomId(String value){
            this._bomId = value;
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


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _byproductType;

    
        @PropMeta(propId=9)
    
        public String getByproductType(){
            return _byproductType;
        }

        public void setByproductType(String value){
            this._byproductType = value;
        }


        private java.math.BigDecimal _yieldRate;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getYieldRate(){
            return _yieldRate;
        }

        public void setYieldRate(java.math.BigDecimal value){
            this._yieldRate = value;
        }


        private java.math.BigDecimal _costAllocationPercent;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getCostAllocationPercent(){
            return _costAllocationPercent;
        }

        public void setCostAllocationPercent(java.math.BigDecimal value){
            this._costAllocationPercent = value;
        }


    }
