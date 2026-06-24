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
    public class ErpPurInvoiceInputBean extends CrudInputBase {

    
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


        private Long _supplierId;

    
        @PropMeta(propId=4)
    
        public Long getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(Long value){
            this._supplierId = value;
        }


        private String _invoiceNo;

    
        @PropMeta(propId=5)
    
        public String getInvoiceNo(){
            return _invoiceNo;
        }

        public void setInvoiceNo(String value){
            this._invoiceNo = value;
        }


        private Integer _invoiceType;

    
        @PropMeta(propId=6)
    
        public Integer getInvoiceType(){
            return _invoiceType;
        }

        public void setInvoiceType(Integer value){
            this._invoiceType = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=8)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private String _exchangeRate;

    
        @PropMeta(propId=9)
    
        public String getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(String value){
            this._exchangeRate = value;
        }


        private String _amountSource;

    
        @PropMeta(propId=10)
    
        public String getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(String value){
            this._amountSource = value;
        }


        private String _amountFunctional;

    
        @PropMeta(propId=11)
    
        public String getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(String value){
            this._amountFunctional = value;
        }


        private String _totalAmount;

    
        @PropMeta(propId=12)
    
        public String getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(String value){
            this._totalAmount = value;
        }


        private String _totalTaxAmount;

    
        @PropMeta(propId=13)
    
        public String getTotalTaxAmount(){
            return _totalTaxAmount;
        }

        public void setTotalTaxAmount(String value){
            this._totalTaxAmount = value;
        }


        private String _totalAmountWithTax;

    
        @PropMeta(propId=14)
    
        public String getTotalAmountWithTax(){
            return _totalAmountWithTax;
        }

        public void setTotalAmountWithTax(String value){
            this._totalAmountWithTax = value;
        }


        private String _paidAmount;

    
        @PropMeta(propId=15)
    
        public String getPaidAmount(){
            return _paidAmount;
        }

        public void setPaidAmount(String value){
            this._paidAmount = value;
        }


        private Integer _docStatus;

    
        @PropMeta(propId=16)
    
        public Integer getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(Integer value){
            this._docStatus = value;
        }


        private Integer _approveStatus;

    
        @PropMeta(propId=17)
    
        public Integer getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(Integer value){
            this._approveStatus = value;
        }


        private Integer _paidStatus;

    
        @PropMeta(propId=18)
    
        public Integer getPaidStatus(){
            return _paidStatus;
        }

        public void setPaidStatus(Integer value){
            this._paidStatus = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=19)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private Long _postedBy;

    
        @PropMeta(propId=21)
    
        public Long getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(Long value){
            this._postedBy = value;
        }


        private Long _approvedBy;

    
        @PropMeta(propId=22)
    
        public Long getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(Long value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=23)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
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


        private List<ErpPurInvoiceLineInputBean> _lines;

        public List<ErpPurInvoiceLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpPurInvoiceLineInputBean> value){
            this._lines = value;
        }


    }
