//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinGlMappingRuleOutputBean {

    
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


        private String _businessType_label;

    
        public String getBusinessType_label(){
            return _businessType_label;
        }

        public void setBusinessType_label(String value){
            this._businessType_label = value;
        }


        private String _accountKey;

    
        @PropMeta(propId=6)
    
        public String getAccountKey(){
            return _accountKey;
        }

        public void setAccountKey(String value){
            this._accountKey = value;
        }


        private String _accountKey_label;

    
        public String getAccountKey_label(){
            return _accountKey_label;
        }

        public void setAccountKey_label(String value){
            this._accountKey_label = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=17)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=18)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=20)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=22)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Map<String,Object> _acctSchema;

        public Map<String,Object> getAcctSchema(){
            return _acctSchema;
        }

        public void setAcctSchema(Map<String,Object> value){
            this._acctSchema = value;
        }


        private Map<String,Object> _materialCategory;

        public Map<String,Object> getMaterialCategory(){
            return _materialCategory;
        }

        public void setMaterialCategory(Map<String,Object> value){
            this._materialCategory = value;
        }


        private Map<String,Object> _warehouse;

        public Map<String,Object> getWarehouse(){
            return _warehouse;
        }

        public void setWarehouse(Map<String,Object> value){
            this._warehouse = value;
        }


        private Map<String,Object> _department;

        public Map<String,Object> getDepartment(){
            return _department;
        }

        public void setDepartment(Map<String,Object> value){
            this._department = value;
        }


        private Map<String,Object> _project;

        public Map<String,Object> getProject(){
            return _project;
        }

        public void setProject(Map<String,Object> value){
            this._project = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
