//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdMaterialOutputBean {

    
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


        private String _materialType_label;

    
        public String getMaterialType_label(){
            return _materialType_label;
        }

        public void setMaterialType_label(String value){
            this._materialType_label = value;
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


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private String _costMethod;

    
        @PropMeta(propId=8)
    
        public String getCostMethod(){
            return _costMethod;
        }

        public void setCostMethod(String value){
            this._costMethod = value;
        }


        private String _costMethod_label;

    
        public String getCostMethod_label(){
            return _costMethod_label;
        }

        public void setCostMethod_label(String value){
            this._costMethod_label = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=19)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=20)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=21)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=23)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=25)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Map<String,Object> _category;

        public Map<String,Object> getCategory(){
            return _category;
        }

        public void setCategory(Map<String,Object> value){
            this._category = value;
        }


        private Map<String,Object> _uoM;

        public Map<String,Object> getUoM(){
            return _uoM;
        }

        public void setUoM(Map<String,Object> value){
            this._uoM = value;
        }


        private Map<String,Object> _defaultWarehouse;

        public Map<String,Object> getDefaultWarehouse(){
            return _defaultWarehouse;
        }

        public void setDefaultWarehouse(Map<String,Object> value){
            this._defaultWarehouse = value;
        }


        private Map<String,Object> _defaultTaxRate;

        public Map<String,Object> getDefaultTaxRate(){
            return _defaultTaxRate;
        }

        public void setDefaultTaxRate(Map<String,Object> value){
            this._defaultTaxRate = value;
        }


        private List<Map<String,Object>> _skus;

        public List<Map<String,Object>> getSkus(){
            return _skus;
        }

        public void setSkus(List<Map<String,Object>> value){
            this._skus = value;
        }


    }
