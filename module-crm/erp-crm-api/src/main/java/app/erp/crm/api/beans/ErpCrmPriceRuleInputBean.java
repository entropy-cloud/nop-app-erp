//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmPriceRuleInputBean extends CrudInputBase {

    
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


        private String _ruleType;

    
        @PropMeta(propId=5)
    
        public String getRuleType(){
            return _ruleType;
        }

        public void setRuleType(String value){
            this._ruleType = value;
        }


        private Integer _priority;

    
        @PropMeta(propId=6)
    
        public Integer getPriority(){
            return _priority;
        }

        public void setPriority(Integer value){
            this._priority = value;
        }


        private Long _productId;

    
        @PropMeta(propId=7)
    
        public Long getProductId(){
            return _productId;
        }

        public void setProductId(Long value){
            this._productId = value;
        }


        private String _productCategory;

    
        @PropMeta(propId=8)
    
        public String getProductCategory(){
            return _productCategory;
        }

        public void setProductCategory(String value){
            this._productCategory = value;
        }


        private Long _customerId;

    
        @PropMeta(propId=9)
    
        public Long getCustomerId(){
            return _customerId;
        }

        public void setCustomerId(Long value){
            this._customerId = value;
        }


        private String _customerCategory;

    
        @PropMeta(propId=10)
    
        public String getCustomerCategory(){
            return _customerCategory;
        }

        public void setCustomerCategory(String value){
            this._customerCategory = value;
        }


        private java.math.BigDecimal _minQuantity;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getMinQuantity(){
            return _minQuantity;
        }

        public void setMinQuantity(java.math.BigDecimal value){
            this._minQuantity = value;
        }


        private java.math.BigDecimal _maxQuantity;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getMaxQuantity(){
            return _maxQuantity;
        }

        public void setMaxQuantity(java.math.BigDecimal value){
            this._maxQuantity = value;
        }


        private java.math.BigDecimal _priceOverride;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getPriceOverride(){
            return _priceOverride;
        }

        public void setPriceOverride(java.math.BigDecimal value){
            this._priceOverride = value;
        }


        private java.math.BigDecimal _discountPercent;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getDiscountPercent(){
            return _discountPercent;
        }

        public void setDiscountPercent(java.math.BigDecimal value){
            this._discountPercent = value;
        }


        private java.math.BigDecimal _discountAmount;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getDiscountAmount(){
            return _discountAmount;
        }

        public void setDiscountAmount(java.math.BigDecimal value){
            this._discountAmount = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=16)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.time.LocalDate _effectiveFrom;

    
        @PropMeta(propId=17)
    
        public java.time.LocalDate getEffectiveFrom(){
            return _effectiveFrom;
        }

        public void setEffectiveFrom(java.time.LocalDate value){
            this._effectiveFrom = value;
        }


        private java.time.LocalDate _effectiveTo;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDate getEffectiveTo(){
            return _effectiveTo;
        }

        public void setEffectiveTo(java.time.LocalDate value){
            this._effectiveTo = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=19)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
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
