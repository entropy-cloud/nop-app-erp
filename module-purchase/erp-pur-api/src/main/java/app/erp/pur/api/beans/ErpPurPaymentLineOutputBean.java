//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurPaymentLineOutputBean {

    
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


        private Integer _version;

    
        @PropMeta(propId=7)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=8)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=10)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _payment;

        public Map<String,Object> getPayment(){
            return _payment;
        }

        public void setPayment(Map<String,Object> value){
            this._payment = value;
        }


        private Map<String,Object> _invoice;

        public Map<String,Object> getInvoice(){
            return _invoice;
        }

        public void setInvoice(Map<String,Object> value){
            this._invoice = value;
        }


    }
