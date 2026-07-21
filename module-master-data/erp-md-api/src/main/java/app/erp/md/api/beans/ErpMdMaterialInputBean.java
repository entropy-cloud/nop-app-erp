//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdMaterialInputBean extends CrudInputBase {

    
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


        private String _materialType;

    
        @PropMeta(propId=4)
    
        public String getMaterialType(){
            return _materialType;
        }

        public void setMaterialType(String value){
            this._materialType = value;
        }


        private Long _categoryId;

    
        @PropMeta(propId=5)
    
        public Long getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(Long value){
            this._categoryId = value;
        }


        private Long _uoMId;

    
        @PropMeta(propId=6)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
        }


        private String _status;

    
        @PropMeta(propId=7)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _costMethod;

    
        @PropMeta(propId=8)
    
        public String getCostMethod(){
            return _costMethod;
        }

        public void setCostMethod(String value){
            this._costMethod = value;
        }


        private Boolean _isBatchManaged;

    
        @PropMeta(propId=9)
    
        public Boolean getIsBatchManaged(){
            return _isBatchManaged;
        }

        public void setIsBatchManaged(Boolean value){
            this._isBatchManaged = value;
        }


        private Boolean _isSerialManaged;

    
        @PropMeta(propId=10)
    
        public Boolean getIsSerialManaged(){
            return _isSerialManaged;
        }

        public void setIsSerialManaged(Boolean value){
            this._isSerialManaged = value;
        }


        private Long _defaultWarehouseId;

    
        @PropMeta(propId=11)
    
        public Long getDefaultWarehouseId(){
            return _defaultWarehouseId;
        }

        public void setDefaultWarehouseId(Long value){
            this._defaultWarehouseId = value;
        }


        private java.math.BigDecimal _minStock;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getMinStock(){
            return _minStock;
        }

        public void setMinStock(java.math.BigDecimal value){
            this._minStock = value;
        }


        private java.math.BigDecimal _maxStock;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getMaxStock(){
            return _maxStock;
        }

        public void setMaxStock(java.math.BigDecimal value){
            this._maxStock = value;
        }


        private java.math.BigDecimal _safetyStock;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getSafetyStock(){
            return _safetyStock;
        }

        public void setSafetyStock(java.math.BigDecimal value){
            this._safetyStock = value;
        }


        private Integer _leadTimeDays;

    
        @PropMeta(propId=15)
    
        public Integer getLeadTimeDays(){
            return _leadTimeDays;
        }

        public void setLeadTimeDays(Integer value){
            this._leadTimeDays = value;
        }


        private java.math.BigDecimal _weight;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getWeight(){
            return _weight;
        }

        public void setWeight(java.math.BigDecimal value){
            this._weight = value;
        }


        private java.math.BigDecimal _volume;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getVolume(){
            return _volume;
        }

        public void setVolume(java.math.BigDecimal value){
            this._volume = value;
        }


        private Long _defaultTaxRateId;

    
        @PropMeta(propId=18)
    
        public Long getDefaultTaxRateId(){
            return _defaultTaxRateId;
        }

        public void setDefaultTaxRateId(Long value){
            this._defaultTaxRateId = value;
        }


        private String _remark;

    
        @PropMeta(propId=25)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.math.BigDecimal _vatRate;

    
        @PropMeta(propId=26)
    
        public java.math.BigDecimal getVatRate(){
            return _vatRate;
        }

        public void setVatRate(java.math.BigDecimal value){
            this._vatRate = value;
        }


        private java.math.BigDecimal _drawbackRate;

    
        @PropMeta(propId=27)
    
        public java.math.BigDecimal getDrawbackRate(){
            return _drawbackRate;
        }

        public void setDrawbackRate(java.math.BigDecimal value){
            this._drawbackRate = value;
        }


        private String _customsHS;

    
        @PropMeta(propId=28)
    
        public String getCustomsHS(){
            return _customsHS;
        }

        public void setCustomsHS(String value){
            this._customsHS = value;
        }


        private String _countryOfOrigin;

    
        @PropMeta(propId=29)
    
        public String getCountryOfOrigin(){
            return _countryOfOrigin;
        }

        public void setCountryOfOrigin(String value){
            this._countryOfOrigin = value;
        }


        private String _preferenceCode;

    
        @PropMeta(propId=30)
    
        public String getPreferenceCode(){
            return _preferenceCode;
        }

        public void setPreferenceCode(String value){
            this._preferenceCode = value;
        }


        private String _customsNameCn;

    
        @PropMeta(propId=31)
    
        public String getCustomsNameCn(){
            return _customsNameCn;
        }

        public void setCustomsNameCn(String value){
            this._customsNameCn = value;
        }


        private String _customsNameEn;

    
        @PropMeta(propId=32)
    
        public String getCustomsNameEn(){
            return _customsNameEn;
        }

        public void setCustomsNameEn(String value){
            this._customsNameEn = value;
        }


        private String _declarationUnit;

    
        @PropMeta(propId=33)
    
        public String getDeclarationUnit(){
            return _declarationUnit;
        }

        public void setDeclarationUnit(String value){
            this._declarationUnit = value;
        }


        private String _supervisionCondition;

    
        @PropMeta(propId=34)
    
        public String getSupervisionCondition(){
            return _supervisionCondition;
        }

        public void setSupervisionCondition(String value){
            this._supervisionCondition = value;
        }


        private List<ErpMdMaterialSkuInputBean> _skus;

        public List<ErpMdMaterialSkuInputBean> getSkus(){
            return _skus;
        }

        public void setSkus(List<ErpMdMaterialSkuInputBean> value){
            this._skus = value;
        }


    }
