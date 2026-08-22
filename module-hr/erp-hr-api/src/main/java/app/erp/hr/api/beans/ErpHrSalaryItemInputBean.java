//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSalaryItemInputBean extends CrudInputBase {

    
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


        private String _itemGroup;

    
        @PropMeta(propId=5)
    
        public String getItemGroup(){
            return _itemGroup;
        }

        public void setItemGroup(String value){
            this._itemGroup = value;
        }


        private String _calcMethod;

    
        @PropMeta(propId=6)
    
        public String getCalcMethod(){
            return _calcMethod;
        }

        public void setCalcMethod(String value){
            this._calcMethod = value;
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


    }
