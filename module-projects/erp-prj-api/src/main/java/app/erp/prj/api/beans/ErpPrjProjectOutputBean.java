//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjProjectOutputBean {

    
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


        private Long _projectTypeId;

    
        @PropMeta(propId=5)
    
        public Long getProjectTypeId(){
            return _projectTypeId;
        }

        public void setProjectTypeId(Long value){
            this._projectTypeId = value;
        }


        private Long _customerId;

    
        @PropMeta(propId=6)
    
        public Long getCustomerId(){
            return _customerId;
        }

        public void setCustomerId(Long value){
            this._customerId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=7)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private java.math.BigDecimal _budget;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getBudget(){
            return _budget;
        }

        public void setBudget(java.math.BigDecimal value){
            this._budget = value;
        }


        private java.math.BigDecimal _committedCost;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getCommittedCost(){
            return _committedCost;
        }

        public void setCommittedCost(java.math.BigDecimal value){
            this._committedCost = value;
        }


        private java.math.BigDecimal _actualCost;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getActualCost(){
            return _actualCost;
        }

        public void setActualCost(java.math.BigDecimal value){
            this._actualCost = value;
        }


        private java.math.BigDecimal _billedAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getBilledAmount(){
            return _billedAmount;
        }

        public void setBilledAmount(java.math.BigDecimal value){
            this._billedAmount = value;
        }


        private String _status;

    
        @PropMeta(propId=14)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private Long _managerId;

    
        @PropMeta(propId=15)
    
        public Long getManagerId(){
            return _managerId;
        }

        public void setManagerId(Long value){
            this._managerId = value;
        }


        private String _remark;

    
        @PropMeta(propId=16)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=17)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=18)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=19)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=21)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _projectType;

        public Map<String,Object> getProjectType(){
            return _projectType;
        }

        public void setProjectType(Map<String,Object> value){
            this._projectType = value;
        }


        private Map<String,Object> _manager;

        public Map<String,Object> getManager(){
            return _manager;
        }

        public void setManager(Map<String,Object> value){
            this._manager = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private List<Map<String,Object>> _tasks;

        public List<Map<String,Object>> getTasks(){
            return _tasks;
        }

        public void setTasks(List<Map<String,Object>> value){
            this._tasks = value;
        }


        private List<Map<String,Object>> _timesheets;

        public List<Map<String,Object>> getTimesheets(){
            return _timesheets;
        }

        public void setTimesheets(List<Map<String,Object>> value){
            this._timesheets = value;
        }


        private List<Map<String,Object>> _members;

        public List<Map<String,Object>> getMembers(){
            return _members;
        }

        public void setMembers(List<Map<String,Object>> value){
            this._members = value;
        }


        private List<Map<String,Object>> _budgets;

        public List<Map<String,Object>> getBudgets(){
            return _budgets;
        }

        public void setBudgets(List<Map<String,Object>> value){
            this._budgets = value;
        }


        private List<Map<String,Object>> _milestones;

        public List<Map<String,Object>> getMilestones(){
            return _milestones;
        }

        public void setMilestones(List<Map<String,Object>> value){
            this._milestones = value;
        }


        private Map<String,Object> _customer;

        public Map<String,Object> getCustomer(){
            return _customer;
        }

        public void setCustomer(Map<String,Object> value){
            this._customer = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
