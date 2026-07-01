//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBankReconciliationOutputBean {

    
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


        private Long _statementId;

    
        @PropMeta(propId=5)
    
        public Long getStatementId(){
            return _statementId;
        }

        public void setStatementId(Long value){
            this._statementId = value;
        }


        private java.time.LocalDate _reconciliationDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getReconciliationDate(){
            return _reconciliationDate;
        }

        public void setReconciliationDate(java.time.LocalDate value){
            this._reconciliationDate = value;
        }


        private java.math.BigDecimal _bookBalance;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getBookBalance(){
            return _bookBalance;
        }

        public void setBookBalance(java.math.BigDecimal value){
            this._bookBalance = value;
        }


        private java.math.BigDecimal _statementBalance;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getStatementBalance(){
            return _statementBalance;
        }

        public void setStatementBalance(java.math.BigDecimal value){
            this._statementBalance = value;
        }


        private java.math.BigDecimal _unreconciledDiff;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getUnreconciledDiff(){
            return _unreconciledDiff;
        }

        public void setUnreconciledDiff(java.math.BigDecimal value){
            this._unreconciledDiff = value;
        }


        private Boolean _isBalanced;

    
        @PropMeta(propId=10)
    
        public Boolean getIsBalanced(){
            return _isBalanced;
        }

        public void setIsBalanced(Boolean value){
            this._isBalanced = value;
        }


        private java.time.LocalDateTime _reconciledAt;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDateTime getReconciledAt(){
            return _reconciledAt;
        }

        public void setReconciledAt(java.time.LocalDateTime value){
            this._reconciledAt = value;
        }


        private Long _reconciledBy;

    
        @PropMeta(propId=12)
    
        public Long getReconciledBy(){
            return _reconciledBy;
        }

        public void setReconciledBy(Long value){
            this._reconciledBy = value;
        }


        private Integer _docStatus;

    
        @PropMeta(propId=13)
    
        public Integer getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(Integer value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=16)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=17)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=19)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _fundAccount;

        public Map<String,Object> getFundAccount(){
            return _fundAccount;
        }

        public void setFundAccount(Map<String,Object> value){
            this._fundAccount = value;
        }


        private Map<String,Object> _statement;

        public Map<String,Object> getStatement(){
            return _statement;
        }

        public void setStatement(Map<String,Object> value){
            this._statement = value;
        }


        private List<Map<String,Object>> _adjustmentLines;

        public List<Map<String,Object>> getAdjustmentLines(){
            return _adjustmentLines;
        }

        public void setAdjustmentLines(List<Map<String,Object>> value){
            this._adjustmentLines = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
