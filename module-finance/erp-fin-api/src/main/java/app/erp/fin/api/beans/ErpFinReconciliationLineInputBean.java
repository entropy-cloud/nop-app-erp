//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinReconciliationLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _reconciliationId;

    
        @PropMeta(propId=2)
    
        public String getReconciliationId(){
            return _reconciliationId;
        }

        public void setReconciliationId(String value){
            this._reconciliationId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _paymentItemId;

    
        @PropMeta(propId=4)
    
        public String getPaymentItemId(){
            return _paymentItemId;
        }

        public void setPaymentItemId(String value){
            this._paymentItemId = value;
        }


        private String _invoiceItemId;

    
        @PropMeta(propId=5)
    
        public String getInvoiceItemId(){
            return _invoiceItemId;
        }

        public void setInvoiceItemId(String value){
            this._invoiceItemId = value;
        }


        private java.math.BigDecimal _settledAmountSource;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getSettledAmountSource(){
            return _settledAmountSource;
        }

        public void setSettledAmountSource(java.math.BigDecimal value){
            this._settledAmountSource = value;
        }


        private java.math.BigDecimal _settledAmountFunctional;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getSettledAmountFunctional(){
            return _settledAmountFunctional;
        }

        public void setSettledAmountFunctional(java.math.BigDecimal value){
            this._settledAmountFunctional = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
