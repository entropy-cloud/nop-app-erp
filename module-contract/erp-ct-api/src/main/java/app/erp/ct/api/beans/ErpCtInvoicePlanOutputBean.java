//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtInvoicePlanOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _contractLineId;

    
        @PropMeta(propId=2)
    
        public Long getContractLineId(){
            return _contractLineId;
        }

        public void setContractLineId(Long value){
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


        private String _invoiceTerm_label;

    
        public String getInvoiceTerm_label(){
            return _invoiceTerm_label;
        }

        public void setInvoiceTerm_label(String value){
            this._invoiceTerm_label = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=11)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=12)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=14)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _contractLine;

        public Map<String,Object> getContractLine(){
            return _contractLine;
        }

        public void setContractLine(Map<String,Object> value){
            this._contractLine = value;
        }


    }
