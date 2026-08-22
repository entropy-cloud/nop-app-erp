//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSalaryItemOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _itemCategory;

    
        @PropMeta(propId=4)
    
        public String getItemCategory(){
            return _itemCategory;
        }

        public void setItemCategory(String value){
            this._itemCategory = value;
        }


        private String _itemCategory_label;

    
        public String getItemCategory_label(){
            return _itemCategory_label;
        }

        public void setItemCategory_label(String value){
            this._itemCategory_label = value;
        }


        private String _itemGroup;

    
        @PropMeta(propId=5)
    
        public String getItemGroup(){
            return _itemGroup;
        }

        public void setItemGroup(String value){
            this._itemGroup = value;
        }


        private String _itemGroup_label;

    
        public String getItemGroup_label(){
            return _itemGroup_label;
        }

        public void setItemGroup_label(String value){
            this._itemGroup_label = value;
        }


        private String _calcMethod;

    
        @PropMeta(propId=6)
    
        public String getCalcMethod(){
            return _calcMethod;
        }

        public void setCalcMethod(String value){
            this._calcMethod = value;
        }


        private String _calcMethod_label;

    
        public String getCalcMethod_label(){
            return _calcMethod_label;
        }

        public void setCalcMethod_label(String value){
            this._calcMethod_label = value;
        }


        private String _formula;

    
        @PropMeta(propId=7)
    
        public String getFormula(){
            return _formula;
        }

        public void setFormula(String value){
            this._formula = value;
        }


        private Boolean _isTaxable;

    
        @PropMeta(propId=8)
    
        public Boolean getIsTaxable(){
            return _isTaxable;
        }

        public void setIsTaxable(Boolean value){
            this._isTaxable = value;
        }


        private Boolean _isSocialInsuranceBase;

    
        @PropMeta(propId=9)
    
        public Boolean getIsSocialInsuranceBase(){
            return _isSocialInsuranceBase;
        }

        public void setIsSocialInsuranceBase(Boolean value){
            this._isSocialInsuranceBase = value;
        }


        private Boolean _isMandatory;

    
        @PropMeta(propId=10)
    
        public Boolean getIsMandatory(){
            return _isMandatory;
        }

        public void setIsMandatory(Boolean value){
            this._isMandatory = value;
        }


        private Integer _sortOrder;

    
        @PropMeta(propId=11)
    
        public Integer getSortOrder(){
            return _sortOrder;
        }

        public void setSortOrder(Integer value){
            this._sortOrder = value;
        }


        private String _orgId;

    
        @PropMeta(propId=12)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
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
