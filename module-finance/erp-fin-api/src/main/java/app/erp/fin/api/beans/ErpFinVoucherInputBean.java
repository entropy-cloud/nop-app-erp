//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinVoucherInputBean extends CrudInputBase {

    
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


        private String _voucherType;

    
        @PropMeta(propId=3)
    
        public String getVoucherType(){
            return _voucherType;
        }

        public void setVoucherType(String value){
            this._voucherType = value;
        }


        private String _postingType;

    
        @PropMeta(propId=4)
    
        public String getPostingType(){
            return _postingType;
        }

        public void setPostingType(String value){
            this._postingType = value;
        }


        private java.time.LocalDate _voucherDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getVoucherDate(){
            return _voucherDate;
        }

        public void setVoucherDate(java.time.LocalDate value){
            this._voucherDate = value;
        }


        private String _voucherNo;

    
        @PropMeta(propId=6)
    
        public String getVoucherNo(){
            return _voucherNo;
        }

        public void setVoucherNo(String value){
            this._voucherNo = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=7)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=8)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Long _periodId;

    
        @PropMeta(propId=9)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private java.math.BigDecimal _totalDebit;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getTotalDebit(){
            return _totalDebit;
        }

        public void setTotalDebit(java.math.BigDecimal value){
            this._totalDebit = value;
        }


        private java.math.BigDecimal _totalCredit;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getTotalCredit(){
            return _totalCredit;
        }

        public void setTotalCredit(java.math.BigDecimal value){
            this._totalCredit = value;
        }


        private Boolean _isReversed;

    
        @PropMeta(propId=12)
    
        public Boolean getIsReversed(){
            return _isReversed;
        }

        public void setIsReversed(Boolean value){
            this._isReversed = value;
        }


        private Long _reversalOfVoucherId;

    
        @PropMeta(propId=13)
    
        public Long getReversalOfVoucherId(){
            return _reversalOfVoucherId;
        }

        public void setReversalOfVoucherId(Long value){
            this._reversalOfVoucherId = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=14)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpFinVoucherLineInputBean> _lines;

        public List<ErpFinVoucherLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpFinVoucherLineInputBean> value){
            this._lines = value;
        }


        private List<ErpFinVoucherBillRInputBean> _billLinks;

        public List<ErpFinVoucherBillRInputBean> getBillLinks(){
            return _billLinks;
        }

        public void setBillLinks(List<ErpFinVoucherBillRInputBean> value){
            this._billLinks = value;
        }


    }
