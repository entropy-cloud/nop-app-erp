//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdOrganizationInputBean extends CrudInputBase {

    
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


        private String _parentId;

    
        @PropMeta(propId=4)
    
        public String getParentId(){
            return _parentId;
        }

        public void setParentId(String value){
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


        private String _functionalCurrencyId;

    
        @PropMeta(propId=6)
    
        public String getFunctionalCurrencyId(){
            return _functionalCurrencyId;
        }

        public void setFunctionalCurrencyId(String value){
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


        private List<ErpMdOrganizationInputBean> _children;

        public List<ErpMdOrganizationInputBean> getChildren(){
            return _children;
        }

        public void setChildren(List<ErpMdOrganizationInputBean> value){
            this._children = value;
        }


    }
