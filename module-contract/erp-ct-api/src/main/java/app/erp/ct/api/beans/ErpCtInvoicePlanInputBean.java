//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtInvoicePlanInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _contractLineId;

    
        @PropMeta(propId=2)
    
        public String getContractLineId(){
            return _contractLineId;
        }

        public void setContractLineId(String value){
            this._contractLineId = value;
        }


        private java.time.LocalDate _planDate;

    
        @PropMeta(propId=3)
    
        public java.time.LocalDate getPlanDate(){
            return _planDate;
        }

        public void setPlanDate(java.time.LocalDate value){
            this._planDate = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
            this._amount = value;
        }


        private Boolean _isInvoiced;

    
        @PropMeta(propId=5)
    
        public Boolean getIsInvoiced(){
            return _isInvoiced;
        }

        public void setIsInvoiced(Boolean value){
            this._isInvoiced = value;
        }


        private String _invoiceBillCode;

    
        @PropMeta(propId=6)
    
        public String getInvoiceBillCode(){
            return _invoiceBillCode;
        }

        public void setInvoiceBillCode(String value){
            this._invoiceBillCode = value;
        }


        private java.time.LocalDate _invoiceDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getInvoiceDate(){
            return _invoiceDate;
        }

        public void setInvoiceDate(java.time.LocalDate value){
            this._invoiceDate = value;
        }


        private String _invoiceTerm;

    
        @PropMeta(propId=8)
    
        public String getInvoiceTerm(){
            return _invoiceTerm;
        }

        public void setInvoiceTerm(String value){
            this._invoiceTerm = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
