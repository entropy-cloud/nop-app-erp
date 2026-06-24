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
    public class ErpMdMaterialCategoryInputBean extends CrudInputBase {

    
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


        private Integer _sortNum;

    
        @PropMeta(propId=5)
    
        public Integer getSortNum(){
            return _sortNum;
        }

        public void setSortNum(Integer value){
            this._sortNum = value;
        }


        private Integer _priceValidationLevel;

    
        @PropMeta(propId=6)
    
        public Integer getPriceValidationLevel(){
            return _priceValidationLevel;
        }

        public void setPriceValidationLevel(Integer value){
            this._priceValidationLevel = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=7)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private List<ErpMdMaterialCategoryInputBean> _children;

        public List<ErpMdMaterialCategoryInputBean> getChildren(){
            return _children;
        }

        public void setChildren(List<ErpMdMaterialCategoryInputBean> value){
            this._children = value;
        }


    }
