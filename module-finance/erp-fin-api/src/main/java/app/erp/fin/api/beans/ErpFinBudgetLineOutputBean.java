//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBudgetLineOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _scenarioId;

    
        @PropMeta(propId=2)
    
        public String getScenarioId(){
            return _scenarioId;
        }

        public void setScenarioId(String value){
            this._scenarioId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _orgId;

    
        @PropMeta(propId=4)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _acctSchemaId;

    
        @PropMeta(propId=5)
    
        public String getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(String value){
            this._acctSchemaId = value;
        }


        private String _periodId;

    
        @PropMeta(propId=6)
    
        public String getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(String value){
            this._periodId = value;
        }


        private String _subjectId;

    
        @PropMeta(propId=7)
    
        public String getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(String value){
            this._subjectId = value;
        }


        private String _subjectCode;

    
        @PropMeta(propId=8)
    
        public String getSubjectCode(){
            return _subjectCode;
        }

        public void setSubjectCode(String value){
            this._subjectCode = value;
        }


        private String _costCenterId;

    
        @PropMeta(propId=9)
    
        public String getCostCenterId(){
            return _costCenterId;
        }

        public void setCostCenterId(String value){
            this._costCenterId = value;
        }


        private String _departmentId;

    
        @PropMeta(propId=10)
    
        public String getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(String value){
            this._departmentId = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=11)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=12)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=13)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _materialId;

    
        @PropMeta(propId=14)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private java.math.BigDecimal _budgetAmountSource;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getBudgetAmountSource(){
            return _budgetAmountSource;
        }

        public void setBudgetAmountSource(java.math.BigDecimal value){
            this._budgetAmountSource = value;
        }


        private java.math.BigDecimal _budgetAmountFunctional;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getBudgetAmountFunctional(){
            return _budgetAmountFunctional;
        }

        public void setBudgetAmountFunctional(java.math.BigDecimal value){
            this._budgetAmountFunctional = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=17)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=18)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private String _remark;

    
        @PropMeta(propId=19)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=20)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=21)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=22)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=24)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=25)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _scenario;

        public Map<String,Object> getScenario(){
            return _scenario;
        }

        public void setScenario(Map<String,Object> value){
            this._scenario = value;
        }


        private Map<String,Object> _acctSchema;

        public Map<String,Object> getAcctSchema(){
            return _acctSchema;
        }

        public void setAcctSchema(Map<String,Object> value){
            this._acctSchema = value;
        }


        private Map<String,Object> _period;

        public Map<String,Object> getPeriod(){
            return _period;
        }

        public void setPeriod(Map<String,Object> value){
            this._period = value;
        }


        private Map<String,Object> _subject;

        public Map<String,Object> getSubject(){
            return _subject;
        }

        public void setSubject(Map<String,Object> value){
            this._subject = value;
        }


        private Map<String,Object> _costCenter;

        public Map<String,Object> getCostCenter(){
            return _costCenter;
        }

        public void setCostCenter(Map<String,Object> value){
            this._costCenter = value;
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


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _warehouse;

        public Map<String,Object> getWarehouse(){
            return _warehouse;
        }

        public void setWarehouse(Map<String,Object> value){
            this._warehouse = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
