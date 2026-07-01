//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinGlBalanceOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=3)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Long _periodId;

    
        @PropMeta(propId=4)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=5)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
            this._subjectId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=6)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _openingDebit;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getOpeningDebit(){
            return _openingDebit;
        }

        public void setOpeningDebit(java.math.BigDecimal value){
            this._openingDebit = value;
        }


        private java.math.BigDecimal _openingCredit;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getOpeningCredit(){
            return _openingCredit;
        }

        public void setOpeningCredit(java.math.BigDecimal value){
            this._openingCredit = value;
        }


        private java.math.BigDecimal _periodDebit;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getPeriodDebit(){
            return _periodDebit;
        }

        public void setPeriodDebit(java.math.BigDecimal value){
            this._periodDebit = value;
        }


        private java.math.BigDecimal _periodCredit;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getPeriodCredit(){
            return _periodCredit;
        }

        public void setPeriodCredit(java.math.BigDecimal value){
            this._periodCredit = value;
        }


        private java.math.BigDecimal _closingDebit;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getClosingDebit(){
            return _closingDebit;
        }

        public void setClosingDebit(java.math.BigDecimal value){
            this._closingDebit = value;
        }


        private java.math.BigDecimal _closingCredit;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getClosingCredit(){
            return _closingCredit;
        }

        public void setClosingCredit(java.math.BigDecimal value){
            this._closingCredit = value;
        }


        private java.math.BigDecimal _yearOpeningDebit;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getYearOpeningDebit(){
            return _yearOpeningDebit;
        }

        public void setYearOpeningDebit(java.math.BigDecimal value){
            this._yearOpeningDebit = value;
        }


        private java.math.BigDecimal _yearOpeningCredit;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getYearOpeningCredit(){
            return _yearOpeningCredit;
        }

        public void setYearOpeningCredit(java.math.BigDecimal value){
            this._yearOpeningCredit = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=15)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=16)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
            this._departmentId = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=17)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=18)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=19)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=20)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=21)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=23)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
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


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _department;

        public Map<String,Object> getDepartment(){
            return _department;
        }

        public void setDepartment(Map<String,Object> value){
            this._department = value;
        }


        private Map<String,Object> _warehouse;

        public Map<String,Object> getWarehouse(){
            return _warehouse;
        }

        public void setWarehouse(Map<String,Object> value){
            this._warehouse = value;
        }


        private Map<String,Object> _project;

        public Map<String,Object> getProject(){
            return _project;
        }

        public void setProject(Map<String,Object> value){
            this._project = value;
        }


    }
