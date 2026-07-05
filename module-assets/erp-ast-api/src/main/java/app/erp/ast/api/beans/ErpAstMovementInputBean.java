//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstMovementInputBean extends CrudInputBase {

    
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


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.time.LocalDate _fromDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getFromDate(){
            return _fromDate;
        }

        public void setFromDate(java.time.LocalDate value){
            this._fromDate = value;
        }


        private java.time.LocalDate _thruDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getThruDate(){
            return _thruDate;
        }

        public void setThruDate(java.time.LocalDate value){
            this._thruDate = value;
        }


        private Long _fromDepartmentId;

    
        @PropMeta(propId=8)
    
        public Long getFromDepartmentId(){
            return _fromDepartmentId;
        }

        public void setFromDepartmentId(Long value){
            this._fromDepartmentId = value;
        }


        private Long _toDepartmentId;

    
        @PropMeta(propId=9)
    
        public Long getToDepartmentId(){
            return _toDepartmentId;
        }

        public void setToDepartmentId(Long value){
            this._toDepartmentId = value;
        }


        private Long _fromStaffId;

    
        @PropMeta(propId=10)
    
        public Long getFromStaffId(){
            return _fromStaffId;
        }

        public void setFromStaffId(Long value){
            this._fromStaffId = value;
        }


        private Long _toStaffId;

    
        @PropMeta(propId=11)
    
        public Long getToStaffId(){
            return _toStaffId;
        }

        public void setToStaffId(Long value){
            this._toStaffId = value;
        }


        private Long _fromLocationId;

    
        @PropMeta(propId=12)
    
        public Long getFromLocationId(){
            return _fromLocationId;
        }

        public void setFromLocationId(Long value){
            this._fromLocationId = value;
        }


        private Long _toLocationId;

    
        @PropMeta(propId=13)
    
        public Long getToLocationId(){
            return _toLocationId;
        }

        public void setToLocationId(Long value){
            this._toLocationId = value;
        }


        private Long _handlerId;

    
        @PropMeta(propId=14)
    
        public Long getHandlerId(){
            return _handlerId;
        }

        public void setHandlerId(Long value){
            this._handlerId = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=15)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=16)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=17)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=19)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private String _docVersion;

    
        @PropMeta(propId=20)
    
        public String getDocVersion(){
            return _docVersion;
        }

        public void setDocVersion(String value){
            this._docVersion = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=21)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private String _remark;

    
        @PropMeta(propId=27)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=200)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=201)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=202)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=203)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=204)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=205)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


    }
