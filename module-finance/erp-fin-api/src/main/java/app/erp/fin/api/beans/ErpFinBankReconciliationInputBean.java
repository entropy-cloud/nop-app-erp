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
    public class ErpFinBankReconciliationInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _fundAccountId;

    
        @PropMeta(propId=4)
    
        public String getFundAccountId(){
            return _fundAccountId;
        }

        public void setFundAccountId(String value){
            this._fundAccountId = value;
        }


        private String _statementId;

    
        @PropMeta(propId=5)
    
        public String getStatementId(){
            return _statementId;
        }

        public void setStatementId(String value){
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


        private java.sql.Timestamp _reconciledAt;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getReconciledAt(){
            return _reconciledAt;
        }

        public void setReconciledAt(java.sql.Timestamp value){
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


        private String _docStatus;

    
        @PropMeta(propId=13)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpFinBankReconciliationLineInputBean> _adjustmentLines;

        public List<ErpFinBankReconciliationLineInputBean> getAdjustmentLines(){
            return _adjustmentLines;
        }

        public void setAdjustmentLines(List<ErpFinBankReconciliationLineInputBean> value){
            this._adjustmentLines = value;
        }


    }
