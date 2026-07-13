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
    public class ErpMdOrganizationOutputBean {

    
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


        private String _orgType;

    
        @PropMeta(propId=5)
    
        public String getOrgType(){
            return _orgType;
        }

        public void setOrgType(String value){
            this._orgType = value;
        }


        private String _orgType_label;

    
        public String getOrgType_label(){
            return _orgType_label;
        }

        public void setOrgType_label(String value){
            this._orgType_label = value;
        }


        private Long _functionalCurrencyId;

    
        @PropMeta(propId=6)
    
        public Long getFunctionalCurrencyId(){
            return _functionalCurrencyId;
        }

        public void setFunctionalCurrencyId(Long value){
            this._functionalCurrencyId = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=8)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=9)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=10)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=12)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _parentName;

    
        public String getParentName(){
            return _parentName;
        }

        public void setParentName(String value){
            this._parentName = value;
        }


        private String _functionalCurrencyName;

    
        public String getFunctionalCurrencyName(){
            return _functionalCurrencyName;
        }

        public void setFunctionalCurrencyName(String value){
            this._functionalCurrencyName = value;
        }


        private Map<String,Object> _parent;

        public Map<String,Object> getParent(){
            return _parent;
        }

        public void setParent(Map<String,Object> value){
            this._parent = value;
        }


        private Map<String,Object> _functionalCurrency;

        public Map<String,Object> getFunctionalCurrency(){
            return _functionalCurrency;
        }

        public void setFunctionalCurrency(Map<String,Object> value){
            this._functionalCurrency = value;
        }


        private List<Map<String,Object>> _children;

        public List<Map<String,Object>> getChildren(){
            return _children;
        }

        public void setChildren(List<Map<String,Object>> value){
            this._children = value;
        }


    }
