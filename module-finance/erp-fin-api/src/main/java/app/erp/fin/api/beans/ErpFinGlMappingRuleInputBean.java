//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinGlMappingRuleInputBean extends CrudInputBase {

    
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


        private String _businessType;

    
        @PropMeta(propId=5)
    
        public String getBusinessType(){
            return _businessType;
        }

        public void setBusinessType(String value){
            this._businessType = value;
        }


        private String _accountKey;

    
        @PropMeta(propId=6)
    
        public String getAccountKey(){
            return _accountKey;
        }

        public void setAccountKey(String value){
            this._accountKey = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=7)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Long _partnerGroupId;

    
        @PropMeta(propId=8)
    
        public Long getPartnerGroupId(){
            return _partnerGroupId;
        }

        public void setPartnerGroupId(Long value){
            this._partnerGroupId = value;
        }


        private Long _materialCategoryId;

    
        @PropMeta(propId=9)
    
        public Long getMaterialCategoryId(){
            return _materialCategoryId;
        }

        public void setMaterialCategoryId(Long value){
            this._materialCategoryId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=10)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=11)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
            this._departmentId = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=12)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private String _targetSubjectCode;

    
        @PropMeta(propId=13)
    
        public String getTargetSubjectCode(){
            return _targetSubjectCode;
        }

        public void setTargetSubjectCode(String value){
            this._targetSubjectCode = value;
        }


        private Integer _priority;

    
        @PropMeta(propId=14)
    
        public Integer getPriority(){
            return _priority;
        }

        public void setPriority(Integer value){
            this._priority = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=15)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=22)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
