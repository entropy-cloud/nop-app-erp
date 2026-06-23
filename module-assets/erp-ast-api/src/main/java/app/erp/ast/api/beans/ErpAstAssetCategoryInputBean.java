//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstAssetCategoryInputBean extends CrudInputBase {

    
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


        private Integer _depreciationMethod;

    
        @PropMeta(propId=4)
    
        public Integer getDepreciationMethod(){
            return _depreciationMethod;
        }

        public void setDepreciationMethod(Integer value){
            this._depreciationMethod = value;
        }


        private Integer _usefulLifeMonths;

    
        @PropMeta(propId=5)
    
        public Integer getUsefulLifeMonths(){
            return _usefulLifeMonths;
        }

        public void setUsefulLifeMonths(Integer value){
            this._usefulLifeMonths = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=6)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
            this._subjectId = value;
        }


        private Long _depreciationSubjectId;

    
        @PropMeta(propId=7)
    
        public Long getDepreciationSubjectId(){
            return _depreciationSubjectId;
        }

        public void setDepreciationSubjectId(Long value){
            this._depreciationSubjectId = value;
        }


        private Long _expenseSubjectId;

    
        @PropMeta(propId=8)
    
        public Long getExpenseSubjectId(){
            return _expenseSubjectId;
        }

        public void setExpenseSubjectId(Long value){
            this._expenseSubjectId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
