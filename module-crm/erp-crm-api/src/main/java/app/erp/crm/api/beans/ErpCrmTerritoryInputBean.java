//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmTerritoryInputBean extends CrudInputBase {

    
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


        private String _orgId;

    
        @PropMeta(propId=4)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _parentId;

    
        @PropMeta(propId=5)
    
        public String getParentId(){
            return _parentId;
        }

        public void setParentId(String value){
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


        private String _managerId;

    
        @PropMeta(propId=7)
    
        public String getManagerId(){
            return _managerId;
        }

        public void setManagerId(String value){
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


    }
