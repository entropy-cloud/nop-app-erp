//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurOrderInputBean extends CrudInputBase {

    
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


        private Integer _approveStatus;

    
        @PropMeta(propId=22)
    
        public Integer getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(Integer value){
            this._approveStatus = value;
        }


        private Integer _paidStatus;

    
        @PropMeta(propId=23)
    
        public Integer getPaidStatus(){
            return _paidStatus;
        }

        public void setPaidStatus(Integer value){
            this._paidStatus = value;
        }


        private Integer _receiveStatus;

    
        @PropMeta(propId=24)
    
        public Integer getReceiveStatus(){
            return _receiveStatus;
        }

        public void setReceiveStatus(Integer value){
            this._receiveStatus = value;
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


        private List<ErpPurOrderLineInputBean> _lines;

        public List<ErpPurOrderLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpPurOrderLineInputBean> value){
            this._lines = value;
        }


    }
