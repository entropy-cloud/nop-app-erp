//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinPostingExceptionOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _traceId;

    
        @PropMeta(propId=2)
    
        public String getTraceId(){
            return _traceId;
        }

        public void setTraceId(String value){
            this._traceId = value;
        }


        private String _billHeadCode;

    
        @PropMeta(propId=3)
    
        public String getBillHeadCode(){
            return _billHeadCode;
        }

        public void setBillHeadCode(String value){
            this._billHeadCode = value;
        }


        private String _businessType;

    
        @PropMeta(propId=4)
    
        public String getBusinessType(){
            return _businessType;
        }

        public void setBusinessType(String value){
            this._businessType = value;
        }


        private String _businessType_label;

    
        public String getBusinessType_label(){
            return _businessType_label;
        }

        public void setBusinessType_label(String value){
            this._businessType_label = value;
        }


        private String _postingType;

    
        @PropMeta(propId=5)
    
        public String getPostingType(){
            return _postingType;
        }

        public void setPostingType(String value){
            this._postingType = value;
        }


        private String _postingType_label;

    
        public String getPostingType_label(){
            return _postingType_label;
        }

        public void setPostingType_label(String value){
            this._postingType_label = value;
        }


        private String _errorCode;

    
        @PropMeta(propId=6)
    
        public String getErrorCode(){
            return _errorCode;
        }

        public void setErrorCode(String value){
            this._errorCode = value;
        }


        private String _errorMessage;

    
        @PropMeta(propId=7)
    
        public String getErrorMessage(){
            return _errorMessage;
        }

        public void setErrorMessage(String value){
            this._errorMessage = value;
        }


        private String _failedStage;

    
        @PropMeta(propId=8)
    
        public String getFailedStage(){
            return _failedStage;
        }

        public void setFailedStage(String value){
            this._failedStage = value;
        }


        private java.time.LocalDate _voucherDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getVoucherDate(){
            return _voucherDate;
        }

        public void setVoucherDate(java.time.LocalDate value){
            this._voucherDate = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=10)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=11)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private String _status;

    
        @PropMeta(propId=12)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private Integer _retryCount;

    
        @PropMeta(propId=13)
    
        public Integer getRetryCount(){
            return _retryCount;
        }

        public void setRetryCount(Integer value){
            this._retryCount = value;
        }


        private String _resolution;

    
        @PropMeta(propId=14)
    
        public String getResolution(){
            return _resolution;
        }

        public void setResolution(String value){
            this._resolution = value;
        }


        private String _resolution_label;

    
        public String getResolution_label(){
            return _resolution_label;
        }

        public void setResolution_label(String value){
            this._resolution_label = value;
        }


        private String _resolutionNote;

    
        @PropMeta(propId=15)
    
        public String getResolutionNote(){
            return _resolutionNote;
        }

        public void setResolutionNote(String value){
            this._resolutionNote = value;
        }


        private String _resolvedBy;

    
        @PropMeta(propId=16)
    
        public String getResolvedBy(){
            return _resolvedBy;
        }

        public void setResolvedBy(String value){
            this._resolvedBy = value;
        }


        private java.time.LocalDateTime _resolvedAt;

    
        @PropMeta(propId=17)
    
        public java.time.LocalDateTime getResolvedAt(){
            return _resolvedAt;
        }

        public void setResolvedAt(java.time.LocalDateTime value){
            this._resolvedAt = value;
        }


        private Long _voucherId;

    
        @PropMeta(propId=18)
    
        public Long getVoucherId(){
            return _voucherId;
        }

        public void setVoucherId(Long value){
            this._voucherId = value;
        }


        private java.sql.Timestamp _occurrenceTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getOccurrenceTime(){
            return _occurrenceTime;
        }

        public void setOccurrenceTime(java.sql.Timestamp value){
            this._occurrenceTime = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=20)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=21)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=22)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=24)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=25)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=27)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=28)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private String _eventData;

    
        @PropMeta(propId=29)
    
        public String getEventData(){
            return _eventData;
        }

        public void setEventData(String value){
            this._eventData = value;
        }


    }
