//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurSupplierPriceListInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _supplierId;

    
        @PropMeta(propId=2)
    
        public Long getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(Long value){
            this._supplierId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=3)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _uoMId;

    
        @PropMeta(propId=4)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=5)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private String _unitPrice;

    
        @PropMeta(propId=6)
    
        public String getUnitPrice(){
            return _unitPrice;
        }

        public void setUnitPrice(String value){
            this._unitPrice = value;
        }


        private String _taxRate;

    
        @PropMeta(propId=7)
    
        public String getTaxRate(){
            return _taxRate;
        }

        public void setTaxRate(String value){
            this._taxRate = value;
        }


        private java.math.BigDecimal _minOrderQuantity;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getMinOrderQuantity(){
            return _minOrderQuantity;
        }

        public void setMinOrderQuantity(java.math.BigDecimal value){
            this._minOrderQuantity = value;
        }


        private Integer _leadTimeDays;

    
        @PropMeta(propId=9)
    
        public Integer getLeadTimeDays(){
            return _leadTimeDays;
        }

        public void setLeadTimeDays(Integer value){
            this._leadTimeDays = value;
        }


        private java.time.LocalDate _validFrom;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getValidFrom(){
            return _validFrom;
        }

        public void setValidFrom(java.time.LocalDate value){
            this._validFrom = value;
        }


        private java.time.LocalDate _validTo;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getValidTo(){
            return _validTo;
        }

        public void setValidTo(java.time.LocalDate value){
            this._validTo = value;
        }


        private Integer _priority;

    
        @PropMeta(propId=12)
    
        public Integer getPriority(){
            return _priority;
        }

        public void setPriority(Integer value){
            this._priority = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=13)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
