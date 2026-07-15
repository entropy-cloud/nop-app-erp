//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBankStatementInputBean extends CrudInputBase {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _fundAccountId;

    
        @PropMeta(propId=4)
    
        public Long getFundAccountId(){
            return _fundAccountId;
        }

        public void setFundAccountId(Long value){
            this._fundAccountId = value;
        }


        private java.time.LocalDate _statementDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getStatementDate(){
            return _statementDate;
        }

        public void setStatementDate(java.time.LocalDate value){
            this._statementDate = value;
        }


        private java.math.BigDecimal _beginningBalance;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getBeginningBalance(){
            return _beginningBalance;
        }

        public void setBeginningBalance(java.math.BigDecimal value){
            this._beginningBalance = value;
        }


        private java.math.BigDecimal _endingBalance;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getEndingBalance(){
            return _endingBalance;
        }

        public void setEndingBalance(java.math.BigDecimal value){
            this._endingBalance = value;
        }


        private java.math.BigDecimal _totalDebit;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getTotalDebit(){
            return _totalDebit;
        }

        public void setTotalDebit(java.math.BigDecimal value){
            this._totalDebit = value;
        }


        private java.math.BigDecimal _totalCredit;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getTotalCredit(){
            return _totalCredit;
        }

        public void setTotalCredit(java.math.BigDecimal value){
            this._totalCredit = value;
        }


        private java.sql.Timestamp _importTime;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getImportTime(){
            return _importTime;
        }

        public void setImportTime(java.sql.Timestamp value){
            this._importTime = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=11)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
