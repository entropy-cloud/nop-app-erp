//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinIntercompanyTransferPriceInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _fromOrgId;

    
        @PropMeta(propId=5)
    
        public Long getFromOrgId(){
            return _fromOrgId;
        }

        public void setFromOrgId(Long value){
            this._fromOrgId = value;
        }


        private Long _toOrgId;

    
        @PropMeta(propId=6)
    
        public Long getToOrgId(){
            return _toOrgId;
        }

        public void setToOrgId(Long value){
            this._toOrgId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=7)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _materialCategoryId;

    
        @PropMeta(propId=8)
    
        public Long getMaterialCategoryId(){
            return _materialCategoryId;
        }

        public void setMaterialCategoryId(Long value){
            this._materialCategoryId = value;
        }


        private String _pricingMethod;

    
        @PropMeta(propId=9)
    
        public String getPricingMethod(){
            return _pricingMethod;
        }

        public void setPricingMethod(String value){
            this._pricingMethod = value;
        }


        private java.math.BigDecimal _markupRate;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getMarkupRate(){
            return _markupRate;
        }

        public void setMarkupRate(java.math.BigDecimal value){
            this._markupRate = value;
        }


        private java.math.BigDecimal _fixedPrice;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getFixedPrice(){
            return _fixedPrice;
        }

        public void setFixedPrice(java.math.BigDecimal value){
            this._fixedPrice = value;
        }


        private String _marketRefSource;

    
        @PropMeta(propId=12)
    
        public String getMarketRefSource(){
            return _marketRefSource;
        }

        public void setMarketRefSource(String value){
            this._marketRefSource = value;
        }


        private java.time.LocalDate _validFrom;

    
        @PropMeta(propId=13)
    
        public java.time.LocalDate getValidFrom(){
            return _validFrom;
        }

        public void setValidFrom(java.time.LocalDate value){
            this._validFrom = value;
        }


        private java.time.LocalDate _validTo;

    
        @PropMeta(propId=14)
    
        public java.time.LocalDate getValidTo(){
            return _validTo;
        }

        public void setValidTo(java.time.LocalDate value){
            this._validTo = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=15)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=22)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
