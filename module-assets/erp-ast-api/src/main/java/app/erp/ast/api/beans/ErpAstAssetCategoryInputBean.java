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


        private String _depreciationMethod;

    
        @PropMeta(propId=4)
    
        public String getDepreciationMethod(){
            return _depreciationMethod;
        }

        public void setDepreciationMethod(String value){
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


        private String _subjectId;

    
        @PropMeta(propId=6)
    
        public String getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(String value){
            this._subjectId = value;
        }


        private String _depreciationSubjectId;

    
        @PropMeta(propId=7)
    
        public String getDepreciationSubjectId(){
            return _depreciationSubjectId;
        }

        public void setDepreciationSubjectId(String value){
            this._depreciationSubjectId = value;
        }


        private String _expenseSubjectId;

    
        @PropMeta(propId=8)
    
        public String getExpenseSubjectId(){
            return _expenseSubjectId;
        }

        public void setExpenseSubjectId(String value){
            this._expenseSubjectId = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _disposalGainLossSubjectId;

    
        @PropMeta(propId=16)
    
        public String getDisposalGainLossSubjectId(){
            return _disposalGainLossSubjectId;
        }

        public void setDisposalGainLossSubjectId(String value){
            this._disposalGainLossSubjectId = value;
        }


        private String _cipSubjectId;

    
        @PropMeta(propId=17)
    
        public String getCipSubjectId(){
            return _cipSubjectId;
        }

        public void setCipSubjectId(String value){
            this._cipSubjectId = value;
        }


    }
