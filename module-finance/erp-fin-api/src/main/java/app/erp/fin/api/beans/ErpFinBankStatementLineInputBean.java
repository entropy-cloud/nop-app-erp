//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBankStatementLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _statementId;

    
        @PropMeta(propId=2)
    
        public Long getStatementId(){
            return _statementId;
        }

        public void setStatementId(Long value){
            this._statementId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private java.time.LocalDate _transactionDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getTransactionDate(){
            return _transactionDate;
        }

        public void setTransactionDate(java.time.LocalDate value){
            this._transactionDate = value;
        }


        private String _description;

    
        @PropMeta(propId=5)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _refNo;

    
        @PropMeta(propId=6)
    
        public String getRefNo(){
            return _refNo;
        }

        public void setRefNo(String value){
            this._refNo = value;
        }


        private Integer _dcDirection;

    
        @PropMeta(propId=7)
    
        public Integer getDcDirection(){
            return _dcDirection;
        }

        public void setDcDirection(Integer value){
            this._dcDirection = value;
        }


        private String _amount;

    
        @PropMeta(propId=8)
    
        public String getAmount(){
            return _amount;
        }

        public void setAmount(String value){
            this._amount = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=9)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private String _balanceAfter;

    
        @PropMeta(propId=10)
    
        public String getBalanceAfter(){
            return _balanceAfter;
        }

        public void setBalanceAfter(String value){
            this._balanceAfter = value;
        }


        private Integer _matchStatus;

    
        @PropMeta(propId=11)
    
        public Integer getMatchStatus(){
            return _matchStatus;
        }

        public void setMatchStatus(Integer value){
            this._matchStatus = value;
        }


        private Long _matchedLineId;

    
        @PropMeta(propId=12)
    
        public Long getMatchedLineId(){
            return _matchedLineId;
        }

        public void setMatchedLineId(Long value){
            this._matchedLineId = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
