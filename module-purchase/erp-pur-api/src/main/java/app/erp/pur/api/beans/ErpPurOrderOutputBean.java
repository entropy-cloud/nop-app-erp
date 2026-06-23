//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurOrderOutputBean {

    
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


        private Long _requisitionId;

    
        @PropMeta(propId=4)
    
        public Long getRequisitionId(){
            return _requisitionId;
        }

        public void setRequisitionId(Long value){
            this._requisitionId = value;
        }


        private Long _quotationId;

    
        @PropMeta(propId=5)
    
        public Long getQuotationId(){
            return _quotationId;
        }

        public void setQuotationId(Long value){
            this._quotationId = value;
        }


        private Long _supplierId;

    
        @PropMeta(propId=6)
    
        public Long getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(Long value){
            this._supplierId = value;
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


        private String _exchangeRate;

    
        @PropMeta(propId=11)
    
        public String getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(String value){
            this._exchangeRate = value;
        }


        private String _amountSource;

    
        @PropMeta(propId=12)
    
        public String getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(String value){
            this._amountSource = value;
        }


        private String _amountFunctional;

    
        @PropMeta(propId=13)
    
        public String getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(String value){
            this._amountFunctional = value;
        }


        private String _totalAmount;

    
        @PropMeta(propId=14)
    
        public String getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(String value){
            this._totalAmount = value;
        }


        private String _totalTaxAmount;

    
        @PropMeta(propId=15)
    
        public String getTotalTaxAmount(){
            return _totalTaxAmount;
        }

        public void setTotalTaxAmount(String value){
            this._totalTaxAmount = value;
        }


        private String _totalAmountWithTax;

    
        @PropMeta(propId=16)
    
        public String getTotalAmountWithTax(){
            return _totalAmountWithTax;
        }

        public void setTotalAmountWithTax(String value){
            this._totalAmountWithTax = value;
        }


        private String _discountRate;

    
        @PropMeta(propId=17)
    
        public String getDiscountRate(){
            return _discountRate;
        }

        public void setDiscountRate(String value){
            this._discountRate = value;
        }


        private String _discountAmount;

    
        @PropMeta(propId=18)
    
        public String getDiscountAmount(){
            return _discountAmount;
        }

        public void setDiscountAmount(String value){
            this._discountAmount = value;
        }


        private String _paidAmount;

    
        @PropMeta(propId=19)
    
        public String getPaidAmount(){
            return _paidAmount;
        }

        public void setPaidAmount(String value){
            this._paidAmount = value;
        }


        private Long _settlementMethodId;

    
        @PropMeta(propId=20)
    
        public Long getSettlementMethodId(){
            return _settlementMethodId;
        }

        public void setSettlementMethodId(Long value){
            this._settlementMethodId = value;
        }


        private Integer _docStatus;

    
        @PropMeta(propId=21)
    
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


        private Integer _approveStatus;

    
        @PropMeta(propId=22)
    
        public Integer getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(Integer value){
            this._approveStatus = value;
        }


        private String _approveStatus_label;

    
        public String getApproveStatus_label(){
            return _approveStatus_label;
        }

        public void setApproveStatus_label(String value){
            this._approveStatus_label = value;
        }


        private Integer _paidStatus;

    
        @PropMeta(propId=23)
    
        public Integer getPaidStatus(){
            return _paidStatus;
        }

        public void setPaidStatus(Integer value){
            this._paidStatus = value;
        }


        private String _paidStatus_label;

    
        public String getPaidStatus_label(){
            return _paidStatus_label;
        }

        public void setPaidStatus_label(String value){
            this._paidStatus_label = value;
        }


        private Integer _receiveStatus;

    
        @PropMeta(propId=24)
    
        public Integer getReceiveStatus(){
            return _receiveStatus;
        }

        public void setReceiveStatus(Integer value){
            this._receiveStatus = value;
        }


        private String _receiveStatus_label;

    
        public String getReceiveStatus_label(){
            return _receiveStatus_label;
        }

        public void setReceiveStatus_label(String value){
            this._receiveStatus_label = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=25)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=26)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private Long _postedBy;

    
        @PropMeta(propId=27)
    
        public Long getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(Long value){
            this._postedBy = value;
        }


        private Long _approvedBy;

    
        @PropMeta(propId=28)
    
        public Long getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(Long value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=29)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=30)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=31)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=32)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=33)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=34)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=35)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=36)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _supplier;

        public Map<String,Object> getSupplier(){
            return _supplier;
        }

        public void setSupplier(Map<String,Object> value){
            this._supplier = value;
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


    }
