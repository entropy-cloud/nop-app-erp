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
    public class ErpMdSubjectOutputBean {

    
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


        private Long _parentId;

    
        @PropMeta(propId=4)
    
        public Long getParentId(){
            return _parentId;
        }

        public void setParentId(Long value){
            this._parentId = value;
        }


        private String _subjectClass;

    
        @PropMeta(propId=5)
    
        public String getSubjectClass(){
            return _subjectClass;
        }

        public void setSubjectClass(String value){
            this._subjectClass = value;
        }


        private String _subjectClass_label;

    
        public String getSubjectClass_label(){
            return _subjectClass_label;
        }

        public void setSubjectClass_label(String value){
            this._subjectClass_label = value;
        }


        private String _direction;

    
        @PropMeta(propId=6)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private String _direction_label;

    
        public String getDirection_label(){
            return _direction_label;
        }

        public void setDirection_label(String value){
            this._direction_label = value;
        }


        private String _balanceType;

    
        @PropMeta(propId=7)
    
        public String getBalanceType(){
            return _balanceType;
        }

        public void setBalanceType(String value){
            this._balanceType = value;
        }


        private String _balanceType_label;

    
        public String getBalanceType_label(){
            return _balanceType_label;
        }

        public void setBalanceType_label(String value){
            this._balanceType_label = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=8)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private Boolean _isAuxiliaryPartner;

    
        @PropMeta(propId=9)
    
        public Boolean getIsAuxiliaryPartner(){
            return _isAuxiliaryPartner;
        }

        public void setIsAuxiliaryPartner(Boolean value){
            this._isAuxiliaryPartner = value;
        }


        private Boolean _isAuxiliaryDepartment;

    
        @PropMeta(propId=10)
    
        public Boolean getIsAuxiliaryDepartment(){
            return _isAuxiliaryDepartment;
        }

        public void setIsAuxiliaryDepartment(Boolean value){
            this._isAuxiliaryDepartment = value;
        }


        private Boolean _isAuxiliaryProject;

    
        @PropMeta(propId=11)
    
        public Boolean getIsAuxiliaryProject(){
            return _isAuxiliaryProject;
        }

        public void setIsAuxiliaryProject(Boolean value){
            this._isAuxiliaryProject = value;
        }


        private Boolean _isAuxiliaryWarehouse;

    
        @PropMeta(propId=12)
    
        public Boolean getIsAuxiliaryWarehouse(){
            return _isAuxiliaryWarehouse;
        }

        public void setIsAuxiliaryWarehouse(Boolean value){
            this._isAuxiliaryWarehouse = value;
        }


        private Boolean _isAuxiliaryProduct;

    
        @PropMeta(propId=13)
    
        public Boolean getIsAuxiliaryProduct(){
            return _isAuxiliaryProduct;
        }

        public void setIsAuxiliaryProduct(Boolean value){
            this._isAuxiliaryProduct = value;
        }


        private Boolean _isAuxiliaryCostCenter;

    
        @PropMeta(propId=14)
    
        public Boolean getIsAuxiliaryCostCenter(){
            return _isAuxiliaryCostCenter;
        }

        public void setIsAuxiliaryCostCenter(Boolean value){
            this._isAuxiliaryCostCenter = value;
        }


        private Boolean _isBudgetable;

    
        @PropMeta(propId=15)
    
        public Boolean getIsBudgetable(){
            return _isBudgetable;
        }

        public void setIsBudgetable(Boolean value){
            this._isBudgetable = value;
        }


        private Boolean _isLeaf;

    
        @PropMeta(propId=16)
    
        public Boolean getIsLeaf(){
            return _isLeaf;
        }

        public void setIsLeaf(Boolean value){
            this._isLeaf = value;
        }


        private String _status;

    
        @PropMeta(propId=17)
    
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


        private Long _delVersion;

    
        @PropMeta(propId=18)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=19)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=20)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=22)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=24)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _parentName;

    
        public String getParentName(){
            return _parentName;
        }

        public void setParentName(String value){
            this._parentName = value;
        }


        private String _currencyName;

    
        public String getCurrencyName(){
            return _currencyName;
        }

        public void setCurrencyName(String value){
            this._currencyName = value;
        }


        private Map<String,Object> _parent;

        public Map<String,Object> getParent(){
            return _parent;
        }

        public void setParent(Map<String,Object> value){
            this._parent = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private List<Map<String,Object>> _children;

        public List<Map<String,Object>> getChildren(){
            return _children;
        }

        public void setChildren(List<Map<String,Object>> value){
            this._children = value;
        }


    }
