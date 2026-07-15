//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdMaterialSkuOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=2)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private String _skuCode;

    
        @PropMeta(propId=3)
    
        public String getSkuCode(){
            return _skuCode;
        }

        public void setSkuCode(String value){
            this._skuCode = value;
        }


        private String _barcode;

    
        @PropMeta(propId=4)
    
        public String getBarcode(){
            return _barcode;
        }

        public void setBarcode(String value){
            this._barcode = value;
        }


        private Long _uoMId;

    
        @PropMeta(propId=5)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
        }


        private java.math.BigDecimal _conversionRate;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getConversionRate(){
            return _conversionRate;
        }

        public void setConversionRate(java.math.BigDecimal value){
            this._conversionRate = value;
        }


        private java.math.BigDecimal _purchasePrice;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getPurchasePrice(){
            return _purchasePrice;
        }

        public void setPurchasePrice(java.math.BigDecimal value){
            this._purchasePrice = value;
        }


        private java.math.BigDecimal _salePrice;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getSalePrice(){
            return _salePrice;
        }

        public void setSalePrice(java.math.BigDecimal value){
            this._salePrice = value;
        }


        private java.math.BigDecimal _wholesalePrice;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getWholesalePrice(){
            return _wholesalePrice;
        }

        public void setWholesalePrice(java.math.BigDecimal value){
            this._wholesalePrice = value;
        }


        private java.math.BigDecimal _retailPrice;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getRetailPrice(){
            return _retailPrice;
        }

        public void setRetailPrice(java.math.BigDecimal value){
            this._retailPrice = value;
        }


        private Long _taxRateId;

    
        @PropMeta(propId=11)
    
        public Long getTaxRateId(){
            return _taxRateId;
        }

        public void setTaxRateId(Long value){
            this._taxRateId = value;
        }


        private Boolean _isDefault;

    
        @PropMeta(propId=12)
    
        public Boolean getIsDefault(){
            return _isDefault;
        }

        public void setIsDefault(Boolean value){
            this._isDefault = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _uoM;

        public Map<String,Object> getUoM(){
            return _uoM;
        }

        public void setUoM(Map<String,Object> value){
            this._uoM = value;
        }


        private Map<String,Object> _taxRate;

        public Map<String,Object> getTaxRate(){
            return _taxRate;
        }

        public void setTaxRate(Map<String,Object> value){
            this._taxRate = value;
        }


    }
