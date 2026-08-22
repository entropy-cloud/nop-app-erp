//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrCompetencyInputBean extends CrudInputBase {

    
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


        private String _description;

    
        @PropMeta(propId=4)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _category;

    
        @PropMeta(propId=5)
    
        public String getCategory(){
            return _category;
        }

        public void setCategory(String value){
            this._category = value;
        }


        private String _competencyGroup;

    
        @PropMeta(propId=6)
    
        public String getCompetencyGroup(){
            return _competencyGroup;
        }

        public void setCompetencyGroup(String value){
            this._competencyGroup = value;
        }


        private Boolean _isTechnical;

    
        @PropMeta(propId=7)
    
        public Boolean getIsTechnical(){
            return _isTechnical;
        }

        public void setIsTechnical(Boolean value){
            this._isTechnical = value;
        }


        private String _parentId;

    
        @PropMeta(propId=8)
    
        public String getParentId(){
            return _parentId;
        }

        public void setParentId(String value){
            this._parentId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=9)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpHrCompetencyLevelInputBean> _levels;

        public List<ErpHrCompetencyLevelInputBean> getLevels(){
            return _levels;
        }

        public void setLevels(List<ErpHrCompetencyLevelInputBean> value){
            this._levels = value;
        }


    }
