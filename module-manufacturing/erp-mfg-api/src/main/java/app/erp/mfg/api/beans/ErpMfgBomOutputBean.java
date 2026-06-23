//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgBomOutputBean {

    
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


        private Long _productId;

    
        @PropMeta(propId=3)
    
        public Long getProductId(){
            return _productId;
        }

        public void setProductId(Long value){
            this._productId = value;
        }


        private Integer _bomType;

    
        @PropMeta(propId=4)
    
        public Integer getBomType(){
            return _bomType;
        }

        public void setBomType(Integer value){
            this._bomType = value;
        }


        private String _bomType_label;

    
        public String getBomType_label(){
            return _bomType_label;
        }

        public void setBomType_label(String value){
            this._bomType_label = value;
        }


        private Integer _consumption;

    
        @PropMeta(propId=5)
    
        public Integer getConsumption(){
            return _consumption;
        }

        public void setConsumption(Integer value){
            this._consumption = value;
        }


        private String _consumption_label;

    
        public String getConsumption_label(){
            return _consumption_label;
        }

        public void setConsumption_label(String value){
            this._consumption_label = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=6)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private Boolean _isDefault;

    
        @PropMeta(propId=7)
    
        public Boolean getIsDefault(){
            return _isDefault;
        }

        public void setIsDefault(Boolean value){
            this._isDefault = value;
        }


        private Boolean _useMultiLevelBom;

    
        @PropMeta(propId=8)
    
        public Boolean getUseMultiLevelBom(){
            return _useMultiLevelBom;
        }

        public void setUseMultiLevelBom(Boolean value){
            this._useMultiLevelBom = value;
        }


        private Boolean _inspectionRequired;

    
        @PropMeta(propId=9)
    
        public Boolean getInspectionRequired(){
            return _inspectionRequired;
        }

        public void setInspectionRequired(Boolean value){
            this._inspectionRequired = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=12)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=13)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=15)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _product;

        public Map<String,Object> getProduct(){
            return _product;
        }

        public void setProduct(Map<String,Object> value){
            this._product = value;
        }


        private List<Map<String,Object>> _lines;

        public List<Map<String,Object>> getLines(){
            return _lines;
        }

        public void setLines(List<Map<String,Object>> value){
            this._lines = value;
        }


        private List<Map<String,Object>> _operations;

        public List<Map<String,Object>> getOperations(){
            return _operations;
        }

        public void setOperations(List<Map<String,Object>> value){
            this._operations = value;
        }


        private List<Map<String,Object>> _byproducts;

        public List<Map<String,Object>> getByproducts(){
            return _byproducts;
        }

        public void setByproducts(List<Map<String,Object>> value){
            this._byproducts = value;
        }


    }
