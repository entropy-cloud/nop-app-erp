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
    public class ErpMdSubjectInputBean extends CrudInputBase {

    
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


        private String _subjectClass;

    
        @PropMeta(propId=5)
    
        public String getSubjectClass(){
            return _subjectClass;
        }

        public void setSubjectClass(String value){
            this._subjectClass = value;
        }


        private String _direction;

    
        @PropMeta(propId=6)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private String _balanceType;

    
        @PropMeta(propId=7)
    
        public String getBalanceType(){
            return _balanceType;
        }

        public void setBalanceType(String value){
            this._balanceType = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=8)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
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


        private String _remark;

    
        @PropMeta(propId=24)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _cashFlowType;

    
        @PropMeta(propId=25)
    
        public String getCashFlowType(){
            return _cashFlowType;
        }

        public void setCashFlowType(String value){
            this._cashFlowType = value;
        }


        private List<ErpMdSubjectInputBean> _children;

        public List<ErpMdSubjectInputBean> getChildren(){
            return _children;
        }

        public void setChildren(List<ErpMdSubjectInputBean> value){
            this._children = value;
        }


    }
