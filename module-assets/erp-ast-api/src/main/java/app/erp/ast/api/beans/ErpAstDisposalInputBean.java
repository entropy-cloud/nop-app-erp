//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstDisposalInputBean extends CrudInputBase {

    
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


        private Long _assetId;

    
        @PropMeta(propId=4)
    
        public Long getAssetId(){
            return _assetId;
        }

        public void setAssetId(Long value){
            this._assetId = value;
        }


        private Integer _disposalType;

    
        @PropMeta(propId=5)
    
        public Integer getDisposalType(){
            return _disposalType;
        }

        public void setDisposalType(Integer value){
            this._disposalType = value;
        }


        private java.math.BigDecimal _disposalAmount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getDisposalAmount(){
            return _disposalAmount;
        }

        public void setDisposalAmount(java.math.BigDecimal value){
            this._disposalAmount = value;
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


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.math.BigDecimal _gainLoss;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getGainLoss(){
            return _gainLoss;
        }

        public void setGainLoss(java.math.BigDecimal value){
            this._gainLoss = value;
        }


        private Integer _reason;

    
        @PropMeta(propId=11)
    
        public Integer getReason(){
            return _reason;
        }

        public void setReason(Integer value){
            this._reason = value;
        }


        private Integer _docStatus;

    
        @PropMeta(propId=12)
    
        public Integer getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(Integer value){
            this._docStatus = value;
        }


        private Integer _approveStatus;

    
        @PropMeta(propId=13)
    
        public Integer getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(Integer value){
            this._approveStatus = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=14)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=15)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private Long _postedBy;

    
        @PropMeta(propId=16)
    
        public Long getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(Long value){
            this._postedBy = value;
        }


        private Long _approvedBy;

    
        @PropMeta(propId=17)
    
        public Long getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(Long value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=20)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private String _remark;

    
        @PropMeta(propId=26)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=27)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=28)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


    }
