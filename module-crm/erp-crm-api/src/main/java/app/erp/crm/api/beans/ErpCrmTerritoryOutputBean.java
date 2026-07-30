//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmTerritoryOutputBean {

    
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


        private Long _parentId;

    
        @PropMeta(propId=5)
    
        public Long getParentId(){
            return _parentId;
        }

        public void setParentId(Long value){
            this._parentId = value;
        }


        private String _territoryType;

    
        @PropMeta(propId=6)
    
        public String getTerritoryType(){
            return _territoryType;
        }

        public void setTerritoryType(String value){
            this._territoryType = value;
        }


        private String _territoryType_label;

    
        public String getTerritoryType_label(){
            return _territoryType_label;
        }

        public void setTerritoryType_label(String value){
            this._territoryType_label = value;
        }


        private Long _managerId;

    
        @PropMeta(propId=7)
    
        public Long getManagerId(){
            return _managerId;
        }

        public void setManagerId(Long value){
            this._managerId = value;
        }


        private String _description;

    
        @PropMeta(propId=8)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _fullPath;

    
        @PropMeta(propId=9)
    
        public String getFullPath(){
            return _fullPath;
        }

        public void setFullPath(String value){
            this._fullPath = value;
        }


        private Integer _level;

    
        @PropMeta(propId=10)
    
        public Integer getLevel(){
            return _level;
        }

        public void setLevel(Integer value){
            this._level = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=11)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private Boolean _isLeaf;

    
        @PropMeta(propId=12)
    
        public Boolean getIsLeaf(){
            return _isLeaf;
        }

        public void setIsLeaf(Boolean value){
            this._isLeaf = value;
        }


        private Integer _sortOrder;

    
        @PropMeta(propId=13)
    
        public Integer getSortOrder(){
            return _sortOrder;
        }

        public void setSortOrder(Integer value){
            this._sortOrder = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=16)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=17)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=19)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _parent;

        public Map<String,Object> getParent(){
            return _parent;
        }

        public void setParent(Map<String,Object> value){
            this._parent = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
