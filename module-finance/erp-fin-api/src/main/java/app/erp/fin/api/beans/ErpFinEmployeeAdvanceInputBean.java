//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinEmployeeAdvanceInputBean extends CrudInputBase {

    
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


        private Long _employeeId;

    
        @PropMeta(propId=4)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private String _advanceType;

    
        @PropMeta(propId=5)
    
        public String getAdvanceType(){
            return _advanceType;
        }

        public void setAdvanceType(String value){
            this._advanceType = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
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


        private java.math.BigDecimal _settledAmount;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getSettledAmount(){
            return _settledAmount;
        }

        public void setSettledAmount(java.math.BigDecimal value){
            this._settledAmount = value;
        }


        private java.math.BigDecimal _outstandingAmount;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getOutstandingAmount(){
            return _outstandingAmount;
        }

        public void setOutstandingAmount(java.math.BigDecimal value){
            this._outstandingAmount = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=13)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=14)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=15)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=16)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=17)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=18)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=19)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=21)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=22)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
