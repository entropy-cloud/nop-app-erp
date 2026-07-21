//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinIntercompanyTransferPriceOutputBean {

    
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


        private String _pricingMethod_label;

    
        public String getPricingMethod_label(){
            return _pricingMethod_label;
        }

        public void setPricingMethod_label(String value){
            this._pricingMethod_label = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=17)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=18)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=20)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=22)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Map<String,Object> _fromOrg;

        public Map<String,Object> getFromOrg(){
            return _fromOrg;
        }

        public void setFromOrg(Map<String,Object> value){
            this._fromOrg = value;
        }


        private Map<String,Object> _toOrg;

        public Map<String,Object> getToOrg(){
            return _toOrg;
        }

        public void setToOrg(Map<String,Object> value){
            this._toOrg = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _materialCategory;

        public Map<String,Object> getMaterialCategory(){
            return _materialCategory;
        }

        public void setMaterialCategory(Map<String,Object> value){
            this._materialCategory = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
