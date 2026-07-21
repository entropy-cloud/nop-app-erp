//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBudgetScenarioInputBean extends CrudInputBase {

    
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


        private Long _acctSchemaId;

    
        @PropMeta(propId=5)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Integer _fiscalYear;

    
        @PropMeta(propId=6)
    
        public Integer getFiscalYear(){
            return _fiscalYear;
        }

        public void setFiscalYear(Integer value){
            this._fiscalYear = value;
        }


        private String _scenarioType;

    
        @PropMeta(propId=7)
    
        public String getScenarioType(){
            return _scenarioType;
        }

        public void setScenarioType(String value){
            this._scenarioType = value;
        }


        private Long _parentScenarioId;

    
        @PropMeta(propId=8)
    
        public Long getParentScenarioId(){
            return _parentScenarioId;
        }

        public void setParentScenarioId(Long value){
            this._parentScenarioId = value;
        }


        private java.time.LocalDate _validFrom;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getValidFrom(){
            return _validFrom;
        }

        public void setValidFrom(java.time.LocalDate value){
            this._validFrom = value;
        }


        private java.time.LocalDate _validTo;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getValidTo(){
            return _validTo;
        }

        public void setValidTo(java.time.LocalDate value){
            this._validTo = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=11)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private String _controlLevel;

    
        @PropMeta(propId=15)
    
        public String getControlLevel(){
            return _controlLevel;
        }

        public void setControlLevel(String value){
            this._controlLevel = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=16)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=17)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private Long _voucherId;

    
        @PropMeta(propId=18)
    
        public Long getVoucherId(){
            return _voucherId;
        }

        public void setVoucherId(Long value){
            this._voucherId = value;
        }


        private String _remark;

    
        @PropMeta(propId=19)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _budgetGroupCode;

    
        @PropMeta(propId=26)
    
        public String getBudgetGroupCode(){
            return _budgetGroupCode;
        }

        public void setBudgetGroupCode(String value){
            this._budgetGroupCode = value;
        }


        private String _carryForwardRule;

    
        @PropMeta(propId=27)
    
        public String getCarryForwardRule(){
            return _carryForwardRule;
        }

        public void setCarryForwardRule(String value){
            this._carryForwardRule = value;
        }


        private String _rollForwardStrategy;

    
        @PropMeta(propId=28)
    
        public String getRollForwardStrategy(){
            return _rollForwardStrategy;
        }

        public void setRollForwardStrategy(String value){
            this._rollForwardStrategy = value;
        }


        private java.sql.Timestamp _closedAt;

    
        @PropMeta(propId=29)
    
        public java.sql.Timestamp getClosedAt(){
            return _closedAt;
        }

        public void setClosedAt(java.sql.Timestamp value){
            this._closedAt = value;
        }


        private List<ErpFinBudgetLineInputBean> _lines;

        public List<ErpFinBudgetLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpFinBudgetLineInputBean> value){
            this._lines = value;
        }


    }
