//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjProjectPnlOutputBean {

    
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


        private Long _projectId;

    
        @PropMeta(propId=3)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.time.LocalDate _periodFrom;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getPeriodFrom(){
            return _periodFrom;
        }

        public void setPeriodFrom(java.time.LocalDate value){
            this._periodFrom = value;
        }


        private java.time.LocalDate _periodTo;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getPeriodTo(){
            return _periodTo;
        }

        public void setPeriodTo(java.time.LocalDate value){
            this._periodTo = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=7)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private java.math.BigDecimal _revenueAmount;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getRevenueAmount(){
            return _revenueAmount;
        }

        public void setRevenueAmount(java.math.BigDecimal value){
            this._revenueAmount = value;
        }


        private java.math.BigDecimal _costLabor;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getCostLabor(){
            return _costLabor;
        }

        public void setCostLabor(java.math.BigDecimal value){
            this._costLabor = value;
        }


        private java.math.BigDecimal _costMaterial;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getCostMaterial(){
            return _costMaterial;
        }

        public void setCostMaterial(java.math.BigDecimal value){
            this._costMaterial = value;
        }


        private java.math.BigDecimal _costExpense;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getCostExpense(){
            return _costExpense;
        }

        public void setCostExpense(java.math.BigDecimal value){
            this._costExpense = value;
        }


        private java.math.BigDecimal _costSubcontract;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getCostSubcontract(){
            return _costSubcontract;
        }

        public void setCostSubcontract(java.math.BigDecimal value){
            this._costSubcontract = value;
        }


        private java.math.BigDecimal _totalCost;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(java.math.BigDecimal value){
            this._totalCost = value;
        }


        private java.math.BigDecimal _grossProfit;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getGrossProfit(){
            return _grossProfit;
        }

        public void setGrossProfit(java.math.BigDecimal value){
            this._grossProfit = value;
        }


        private java.math.BigDecimal _grossMarginPct;

    
        @PropMeta(propId=18)
    
        public java.math.BigDecimal getGrossMarginPct(){
            return _grossMarginPct;
        }

        public void setGrossMarginPct(java.math.BigDecimal value){
            this._grossMarginPct = value;
        }


        private java.math.BigDecimal _committedCost;

    
        @PropMeta(propId=19)
    
        public java.math.BigDecimal getCommittedCost(){
            return _committedCost;
        }

        public void setCommittedCost(java.math.BigDecimal value){
            this._committedCost = value;
        }


        private java.math.BigDecimal _budgetAmount;

    
        @PropMeta(propId=20)
    
        public java.math.BigDecimal getBudgetAmount(){
            return _budgetAmount;
        }

        public void setBudgetAmount(java.math.BigDecimal value){
            this._budgetAmount = value;
        }


        private java.math.BigDecimal _forecastCompleteCost;

    
        @PropMeta(propId=21)
    
        public java.math.BigDecimal getForecastCompleteCost(){
            return _forecastCompleteCost;
        }

        public void setForecastCompleteCost(java.math.BigDecimal value){
            this._forecastCompleteCost = value;
        }


        private String _calcStatus;

    
        @PropMeta(propId=22)
    
        public String getCalcStatus(){
            return _calcStatus;
        }

        public void setCalcStatus(String value){
            this._calcStatus = value;
        }


        private String _calcStatus_label;

    
        public String getCalcStatus_label(){
            return _calcStatus_label;
        }

        public void setCalcStatus_label(String value){
            this._calcStatus_label = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=23)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=24)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _approveStatus_label;

    
        public String getApproveStatus_label(){
            return _approveStatus_label;
        }

        public void setApproveStatus_label(String value){
            this._approveStatus_label = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=25)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.sql.Timestamp _postedAt;

    
        @PropMeta(propId=26)
    
        public java.sql.Timestamp getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.sql.Timestamp value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=27)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private String _remark;

    
        @PropMeta(propId=28)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=29)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=30)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=31)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=32)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=33)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=34)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _project;

        public Map<String,Object> getProject(){
            return _project;
        }

        public void setProject(Map<String,Object> value){
            this._project = value;
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
