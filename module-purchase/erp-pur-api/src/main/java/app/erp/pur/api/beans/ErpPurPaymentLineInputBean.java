//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurPaymentLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _paymentId;

    
        @PropMeta(propId=2)
    
        public Long getPaymentId(){
            return _paymentId;
        }

        public void setPaymentId(Long value){
            this._paymentId = value;
        }


        private Long _invoiceId;

    
        @PropMeta(propId=3)
    
        public Long getInvoiceId(){
            return _invoiceId;
        }

        public void setInvoiceId(Long value){
            this._invoiceId = value;
        }


        private String _amount;

    
        @PropMeta(propId=4)
    
        public String getAmount(){
            return _amount;
        }

        public void setAmount(String value){
            this._amount = value;
        }


        private String _remark;

    
        @PropMeta(propId=5)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=6)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
