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

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _scenarioId;

    
        @PropMeta(propId=2)
    
        public Long getScenarioId(){
            return _scenarioId;
        }

        public void setScenarioId(Long value){
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


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=5)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Long _periodId;

    
        @PropMeta(propId=6)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=7)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
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


        private Long _costCenterId;

    
        @PropMeta(propId=9)
    
        public Long getCostCenterId(){
            return _costCenterId;
        }

        public void setCostCenterId(Long value){
            this._costCenterId = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=10)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
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


        private Long _partnerId;

    
        @PropMeta(propId=12)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=13)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=14)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
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


        private Long _currencyId;

    
        @PropMeta(propId=17)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
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
