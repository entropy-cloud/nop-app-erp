//__XGEN_FORCE_OVERRIDE__
    package app.erp.sal.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSalPricingRuleInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _ruleName;

    
        @PropMeta(propId=2)
    
        public String getRuleName(){
            return _ruleName;
        }

        public void setRuleName(String value){
            this._ruleName = value;
        }


        private String _ruleCode;

    
        @PropMeta(propId=3)
    
        public String getRuleCode(){
            return _ruleCode;
        }

        public void setRuleCode(String value){
            this._ruleCode = value;
        }


        private String _ruleType;

    
        @PropMeta(propId=4)
    
        public String getRuleType(){
            return _ruleType;
        }

        public void setRuleType(String value){
            this._ruleType = value;
        }


        private String _targetType;

    
        @PropMeta(propId=5)
    
        public String getTargetType(){
            return _targetType;
        }

        public void setTargetType(String value){
            this._targetType = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=6)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _materialCategoryId;

    
        @PropMeta(propId=7)
    
        public Long getMaterialCategoryId(){
            return _materialCategoryId;
        }

        public void setMaterialCategoryId(Long value){
            this._materialCategoryId = value;
        }


        private String _customerGroupCode;

    
        @PropMeta(propId=8)
    
        public String getCustomerGroupCode(){
            return _customerGroupCode;
        }

        public void setCustomerGroupCode(String value){
            this._customerGroupCode = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=9)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private java.math.BigDecimal _minOrderAmount;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getMinOrderAmount(){
            return _minOrderAmount;
        }

        public void setMinOrderAmount(java.math.BigDecimal value){
            this._minOrderAmount = value;
        }


        private java.math.BigDecimal _discountPercent;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getDiscountPercent(){
            return _discountPercent;
        }

        public void setDiscountPercent(java.math.BigDecimal value){
            this._discountPercent = value;
        }


        private java.math.BigDecimal _discountAmount;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getDiscountAmount(){
            return _discountAmount;
        }

        public void setDiscountAmount(java.math.BigDecimal value){
            this._discountAmount = value;
        }


        private Long _giftMaterialId;

    
        @PropMeta(propId=13)
    
        public Long getGiftMaterialId(){
            return _giftMaterialId;
        }

        public void setGiftMaterialId(Long value){
            this._giftMaterialId = value;
        }


        private Long _giftSkuId;

    
        @PropMeta(propId=14)
    
        public Long getGiftSkuId(){
            return _giftSkuId;
        }

        public void setGiftSkuId(Long value){
            this._giftSkuId = value;
        }


        private java.math.BigDecimal _giftQuantity;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getGiftQuantity(){
            return _giftQuantity;
        }

        public void setGiftQuantity(java.math.BigDecimal value){
            this._giftQuantity = value;
        }


        private java.math.BigDecimal _priceOverride;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getPriceOverride(){
            return _priceOverride;
        }

        public void setPriceOverride(java.math.BigDecimal value){
            this._priceOverride = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=17)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private Integer _priority;

    
        @PropMeta(propId=18)
    
        public Integer getPriority(){
            return _priority;
        }

        public void setPriority(Integer value){
            this._priority = value;
        }


        private Boolean _stackable;

    
        @PropMeta(propId=19)
    
        public Boolean getStackable(){
            return _stackable;
        }

        public void setStackable(Boolean value){
            this._stackable = value;
        }


        private java.sql.Timestamp _validFrom;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getValidFrom(){
            return _validFrom;
        }

        public void setValidFrom(java.sql.Timestamp value){
            this._validFrom = value;
        }


        private java.sql.Timestamp _validTo;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getValidTo(){
            return _validTo;
        }

        public void setValidTo(java.sql.Timestamp value){
            this._validTo = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=22)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=29)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
