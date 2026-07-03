//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinNotesPayableInputBean extends CrudInputBase {

    
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


        private String _notesType;

    
        @PropMeta(propId=4)
    
        public String getNotesType(){
            return _notesType;
        }

        public void setNotesType(String value){
            this._notesType = value;
        }


        private String _notesNo;

    
        @PropMeta(propId=5)
    
        public String getNotesNo(){
            return _notesNo;
        }

        public void setNotesNo(String value){
            this._notesNo = value;
        }


        private String _payeeName;

    
        @PropMeta(propId=6)
    
        public String getPayeeName(){
            return _payeeName;
        }

        public void setPayeeName(String value){
            this._payeeName = value;
        }


        private String _payeeBank;

    
        @PropMeta(propId=7)
    
        public String getPayeeBank(){
            return _payeeBank;
        }

        public void setPayeeBank(String value){
            this._payeeBank = value;
        }


        private java.time.LocalDate _issueDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getIssueDate(){
            return _issueDate;
        }

        public void setIssueDate(java.time.LocalDate value){
            this._issueDate = value;
        }


        private java.time.LocalDate _dueDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getDueDate(){
            return _dueDate;
        }

        public void setDueDate(java.time.LocalDate value){
            this._dueDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=10)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=14)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Long _creditFacilityId;

    
        @PropMeta(propId=15)
    
        public Long getCreditFacilityId(){
            return _creditFacilityId;
        }

        public void setCreditFacilityId(Long value){
            this._creditFacilityId = value;
        }


        private String _sourceBillType;

    
        @PropMeta(propId=16)
    
        public String getSourceBillType(){
            return _sourceBillType;
        }

        public void setSourceBillType(String value){
            this._sourceBillType = value;
        }


        private String _sourceBillCode;

    
        @PropMeta(propId=17)
    
        public String getSourceBillCode(){
            return _sourceBillCode;
        }

        public void setSourceBillCode(String value){
            this._sourceBillCode = value;
        }


        private String _status;

    
        @PropMeta(propId=18)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=19)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=20)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=21)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=22)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=23)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
