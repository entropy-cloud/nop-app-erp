//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinBankStatementOutputBean {

    
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


        private String _beginningBalance;

    
        @PropMeta(propId=6)
    
        public String getBeginningBalance(){
            return _beginningBalance;
        }

        public void setBeginningBalance(String value){
            this._beginningBalance = value;
        }


        private String _endingBalance;

    
        @PropMeta(propId=7)
    
        public String getEndingBalance(){
            return _endingBalance;
        }

        public void setEndingBalance(String value){
            this._endingBalance = value;
        }


        private String _totalDebit;

    
        @PropMeta(propId=8)
    
        public String getTotalDebit(){
            return _totalDebit;
        }

        public void setTotalDebit(String value){
            this._totalDebit = value;
        }


        private String _totalCredit;

    
        @PropMeta(propId=9)
    
        public String getTotalCredit(){
            return _totalCredit;
        }

        public void setTotalCredit(String value){
            this._totalCredit = value;
        }


        private java.time.LocalDateTime _importTime;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDateTime getImportTime(){
            return _importTime;
        }

        public void setImportTime(java.time.LocalDateTime value){
            this._importTime = value;
        }


        private Integer _docStatus;

    
        @PropMeta(propId=11)
    
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

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
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


    }
