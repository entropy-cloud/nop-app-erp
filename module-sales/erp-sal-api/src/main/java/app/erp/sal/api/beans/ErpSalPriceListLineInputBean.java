//__XGEN_FORCE_OVERRIDE__
    package app.erp.sal.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSalPriceListLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _priceListId;

    
        @PropMeta(propId=2)
    
        public String getPriceListId(){
            return _priceListId;
        }

        public void setPriceListId(String value){
            this._priceListId = value;
        }


        private String _materialId;

    
        @PropMeta(propId=3)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _skuId;

    
        @PropMeta(propId=4)
    
        public String getSkuId(){
            return _skuId;
        }

        public void setSkuId(String value){
            this._skuId = value;
        }


        private String _uoMId;

    
        @PropMeta(propId=5)
    
        public String getUoMId(){
            return _uoMId;
        }

        public void setUoMId(String value){
            this._uoMId = value;
        }


        private java.math.BigDecimal _unitPrice;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getUnitPrice(){
            return _unitPrice;
        }

        public void setUnitPrice(java.math.BigDecimal value){
            this._unitPrice = value;
        }


        private java.math.BigDecimal _minQuantity;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getMinQuantity(){
            return _minQuantity;
        }

        public void setMinQuantity(java.math.BigDecimal value){
            this._minQuantity = value;
        }


        private java.math.BigDecimal _maxQuantity;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getMaxQuantity(){
            return _maxQuantity;
        }

        public void setMaxQuantity(java.math.BigDecimal value){
            this._maxQuantity = value;
        }


        private java.time.LocalDate _validFrom;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getValidFrom(){
            return _validFrom;
        }

        public void setValidFrom(java.time.LocalDate value){
            this._validFrom = value;
        }


        private java.time.LocalDate _validTo;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getValidTo(){
            return _validTo;
        }

        public void setValidTo(java.time.LocalDate value){
            this._validTo = value;
        }


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
