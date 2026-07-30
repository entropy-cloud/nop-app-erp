//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmBundlePricingOutputBean {

    
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


        private String _bundleName;

    
        @PropMeta(propId=5)
    
        public String getBundleName(){
            return _bundleName;
        }

        public void setBundleName(String value){
            this._bundleName = value;
        }


        private String _description;

    
        @PropMeta(propId=6)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _discountType;

    
        @PropMeta(propId=7)
    
        public String getDiscountType(){
            return _discountType;
        }

        public void setDiscountType(String value){
            this._discountType = value;
        }


        private String _discountType_label;

    
        public String getDiscountType_label(){
            return _discountType_label;
        }

        public void setDiscountType_label(String value){
            this._discountType_label = value;
        }


        private java.math.BigDecimal _discountValue;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getDiscountValue(){
            return _discountValue;
        }

        public void setDiscountValue(java.math.BigDecimal value){
            this._discountValue = value;
        }


        private java.math.BigDecimal _bundleAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getBundleAmount(){
            return _bundleAmount;
        }

        public void setBundleAmount(java.math.BigDecimal value){
            this._bundleAmount = value;
        }


        private java.time.LocalDate _effectiveFrom;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getEffectiveFrom(){
            return _effectiveFrom;
        }

        public void setEffectiveFrom(java.time.LocalDate value){
            this._effectiveFrom = value;
        }


        private java.time.LocalDate _effectiveTo;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getEffectiveTo(){
            return _effectiveTo;
        }

        public void setEffectiveTo(java.time.LocalDate value){
            this._effectiveTo = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=12)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
