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
    public class ErpFinExpenseClaimOutputBean {

    
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


        private Long _claimantId;

    
        @PropMeta(propId=4)
    
        public Long getClaimantId(){
            return _claimantId;
        }

        public void setClaimantId(Long value){
            this._claimantId = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=5)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
            this._departmentId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _paymentMode;

    
        @PropMeta(propId=7)
    
        public String getPaymentMode(){
            return _paymentMode;
        }

        public void setPaymentMode(String value){
            this._paymentMode = value;
        }


        private String _paymentMode_label;

    
        public String getPaymentMode_label(){
            return _paymentMode_label;
        }

        public void setPaymentMode_label(String value){
            this._paymentMode_label = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=8)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private java.math.BigDecimal _amountWithoutTax;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAmountWithoutTax(){
            return _amountWithoutTax;
        }

        public void setAmountWithoutTax(java.math.BigDecimal value){
            this._amountWithoutTax = value;
        }


        private java.math.BigDecimal _taxAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getTaxAmount(){
            return _taxAmount;
        }

        public void setTaxAmount(java.math.BigDecimal value){
            this._taxAmount = value;
        }


        private java.math.BigDecimal _amountWithTax;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getAmountWithTax(){
            return _amountWithTax;
        }

        public void setAmountWithTax(java.math.BigDecimal value){
            this._amountWithTax = value;
        }


        private Long _settleAdvanceId;

    
        @PropMeta(propId=15)
    
        public Long getSettleAdvanceId(){
            return _settleAdvanceId;
        }

        public void setSettleAdvanceId(Long value){
            this._settleAdvanceId = value;
        }


        private String _reason;

    
        @PropMeta(propId=16)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=17)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=18)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _approveStatus_label;

    
        public String getApproveStatus_label(){
            return _approveStatus_label;
        }

        public void setApproveStatus_label(String value){
            this._approveStatus_label = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=19)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.sql.Timestamp _approvedAt;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.sql.Timestamp value){
            this._approvedAt = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=21)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=22)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private java.sql.Timestamp _postedAt;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.sql.Timestamp value){
            this._postedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=24)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=25)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=26)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=27)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=28)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=29)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=30)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _claimant;

        public Map<String,Object> getClaimant(){
            return _claimant;
        }

        public void setClaimant(Map<String,Object> value){
            this._claimant = value;
        }


        private Map<String,Object> _department;

        public Map<String,Object> getDepartment(){
            return _department;
        }

        public void setDepartment(Map<String,Object> value){
            this._department = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _settleAdvance;

        public Map<String,Object> getSettleAdvance(){
            return _settleAdvance;
        }

        public void setSettleAdvance(Map<String,Object> value){
            this._settleAdvance = value;
        }


        private List<Map<String,Object>> _lines;

        public List<Map<String,Object>> getLines(){
            return _lines;
        }

        public void setLines(List<Map<String,Object>> value){
            this._lines = value;
        }


    }
