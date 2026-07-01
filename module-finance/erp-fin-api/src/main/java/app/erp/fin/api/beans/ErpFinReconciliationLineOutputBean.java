//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinReconciliationLineOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _reconciliationId;

    
        @PropMeta(propId=2)
    
        public Long getReconciliationId(){
            return _reconciliationId;
        }

        public void setReconciliationId(Long value){
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


        private Long _paymentItemId;

    
        @PropMeta(propId=4)
    
        public Long getPaymentItemId(){
            return _paymentItemId;
        }

        public void setPaymentItemId(Long value){
            this._paymentItemId = value;
        }


        private Long _invoiceItemId;

    
        @PropMeta(propId=5)
    
        public Long getInvoiceItemId(){
            return _invoiceItemId;
        }

        public void setInvoiceItemId(Long value){
            this._invoiceItemId = value;
        }


        private String _settledAmountSource;

    
        @PropMeta(propId=6)
    
        public String getSettledAmountSource(){
            return _settledAmountSource;
        }

        public void setSettledAmountSource(String value){
            this._settledAmountSource = value;
        }


        private String _settledAmountFunctional;

    
        @PropMeta(propId=7)
    
        public String getSettledAmountFunctional(){
            return _settledAmountFunctional;
        }

        public void setSettledAmountFunctional(String value){
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


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=10)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=11)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=13)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _reconciliation;

        public Map<String,Object> getReconciliation(){
            return _reconciliation;
        }

        public void setReconciliation(Map<String,Object> value){
            this._reconciliation = value;
        }


        private Map<String,Object> _paymentItem;

        public Map<String,Object> getPaymentItem(){
            return _paymentItem;
        }

        public void setPaymentItem(Map<String,Object> value){
            this._paymentItem = value;
        }


        private Map<String,Object> _invoiceItem;

        public Map<String,Object> getInvoiceItem(){
            return _invoiceItem;
        }

        public void setInvoiceItem(Map<String,Object> value){
            this._invoiceItem = value;
        }


    }
