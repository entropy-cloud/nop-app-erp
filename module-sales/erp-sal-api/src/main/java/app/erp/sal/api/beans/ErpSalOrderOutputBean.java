//__XGEN_FORCE_OVERRIDE__
    package app.erp.sal.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSalOrderOutputBean {

    
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


        private Long _quotationId;

    
        @PropMeta(propId=4)
    
        public Long getQuotationId(){
            return _quotationId;
        }

        public void setQuotationId(Long value){
            this._quotationId = value;
        }


        private Long _contractId;

    
        @PropMeta(propId=5)
    
        public Long getContractId(){
            return _contractId;
        }

        public void setContractId(Long value){
            this._contractId = value;
        }


        private Long _customerId;

    
        @PropMeta(propId=6)
    
        public Long getCustomerId(){
            return _customerId;
        }

        public void setCustomerId(Long value){
            this._customerId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=7)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.time.LocalDate _deliveryDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getDeliveryDate(){
            return _deliveryDate;
        }

        public void setDeliveryDate(java.time.LocalDate value){
            this._deliveryDate = value;
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


        private java.math.BigDecimal _totalAmount;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal value){
            this._totalAmount = value;
        }


        private java.math.BigDecimal _totalTaxAmount;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getTotalTaxAmount(){
            return _totalTaxAmount;
        }

        public void setTotalTaxAmount(java.math.BigDecimal value){
            this._totalTaxAmount = value;
        }


        private java.math.BigDecimal _totalAmountWithTax;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getTotalAmountWithTax(){
            return _totalAmountWithTax;
        }

        public void setTotalAmountWithTax(java.math.BigDecimal value){
            this._totalAmountWithTax = value;
        }


        private java.math.BigDecimal _discountRate;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getDiscountRate(){
            return _discountRate;
        }

        public void setDiscountRate(java.math.BigDecimal value){
            this._discountRate = value;
        }


        private java.math.BigDecimal _discountAmount;

    
        @PropMeta(propId=18)
    
        public java.math.BigDecimal getDiscountAmount(){
            return _discountAmount;
        }

        public void setDiscountAmount(java.math.BigDecimal value){
            this._discountAmount = value;
        }


        private java.math.BigDecimal _receivedAmount;

    
        @PropMeta(propId=19)
    
        public java.math.BigDecimal getReceivedAmount(){
            return _receivedAmount;
        }

        public void setReceivedAmount(java.math.BigDecimal value){
            this._receivedAmount = value;
        }


        private Long _settlementMethodId;

    
        @PropMeta(propId=20)
    
        public Long getSettlementMethodId(){
            return _settlementMethodId;
        }

        public void setSettlementMethodId(Long value){
            this._settlementMethodId = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=21)
    
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

    
        @PropMeta(propId=22)
    
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


        private String _receivedStatus;

    
        @PropMeta(propId=23)
    
        public String getReceivedStatus(){
            return _receivedStatus;
        }

        public void setReceivedStatus(String value){
            this._receivedStatus = value;
        }


        private String _receivedStatus_label;

    
        public String getReceivedStatus_label(){
            return _receivedStatus_label;
        }

        public void setReceivedStatus_label(String value){
            this._receivedStatus_label = value;
        }


        private String _deliveryStatus;

    
        @PropMeta(propId=24)
    
        public String getDeliveryStatus(){
            return _deliveryStatus;
        }

        public void setDeliveryStatus(String value){
            this._deliveryStatus = value;
        }


        private String _deliveryStatus_label;

    
        public String getDeliveryStatus_label(){
            return _deliveryStatus_label;
        }

        public void setDeliveryStatus_label(String value){
            this._deliveryStatus_label = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=25)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.sql.Timestamp _postedAt;

    
        @PropMeta(propId=26)
    
        public java.sql.Timestamp getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.sql.Timestamp value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=27)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=28)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.sql.Timestamp _approvedAt;

    
        @PropMeta(propId=29)
    
        public java.sql.Timestamp getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.sql.Timestamp value){
            this._approvedAt = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=30)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=31)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=32)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=33)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=34)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=35)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=36)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Map<String,Object> _customer;

        public Map<String,Object> getCustomer(){
            return _customer;
        }

        public void setCustomer(Map<String,Object> value){
            this._customer = value;
        }


        private Map<String,Object> _warehouse;

        public Map<String,Object> getWarehouse(){
            return _warehouse;
        }

        public void setWarehouse(Map<String,Object> value){
            this._warehouse = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private Map<String,Object> _settlementMethod;

        public Map<String,Object> getSettlementMethod(){
            return _settlementMethod;
        }

        public void setSettlementMethod(Map<String,Object> value){
            this._settlementMethod = value;
        }


        private List<Map<String,Object>> _lines;

        public List<Map<String,Object>> getLines(){
            return _lines;
        }

        public void setLines(List<Map<String,Object>> value){
            this._lines = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _quotation;

        public Map<String,Object> getQuotation(){
            return _quotation;
        }

        public void setQuotation(Map<String,Object> value){
            this._quotation = value;
        }


        private Map<String,Object> _contract;

        public Map<String,Object> getContract(){
            return _contract;
        }

        public void setContract(Map<String,Object> value){
            this._contract = value;
        }


    }
