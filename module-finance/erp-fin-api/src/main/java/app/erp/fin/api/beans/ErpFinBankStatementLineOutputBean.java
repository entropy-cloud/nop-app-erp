//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBankStatementLineOutputBean {

    
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


        private String _dcDirection_label;

    
        public String getDcDirection_label(){
            return _dcDirection_label;
        }

        public void setDcDirection_label(String value){
            this._dcDirection_label = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
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


        private java.math.BigDecimal _balanceAfter;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getBalanceAfter(){
            return _balanceAfter;
        }

        public void setBalanceAfter(java.math.BigDecimal value){
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


        private String _matchStatus_label;

    
        public String getMatchStatus_label(){
            return _matchStatus_label;
        }

        public void setMatchStatus_label(String value){
            this._matchStatus_label = value;
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


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _statement;

        public Map<String,Object> getStatement(){
            return _statement;
        }

        public void setStatement(Map<String,Object> value){
            this._statement = value;
        }


        private Map<String,Object> _matchedLine;

        public Map<String,Object> getMatchedLine(){
            return _matchedLine;
        }

        public void setMatchedLine(Map<String,Object> value){
            this._matchedLine = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


    }
