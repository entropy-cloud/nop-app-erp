//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrPayrollBankFileInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=2)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private java.time.LocalDate _paymentDate;

    
        @PropMeta(propId=3)
    
        public java.time.LocalDate getPaymentDate(){
            return _paymentDate;
        }

        public void setPaymentDate(java.time.LocalDate value){
            this._paymentDate = value;
        }


        private java.math.BigDecimal _totalAmount;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal value){
            this._totalAmount = value;
        }


        private Integer _recordCount;

    
        @PropMeta(propId=5)
    
        public Integer getRecordCount(){
            return _recordCount;
        }

        public void setRecordCount(Integer value){
            this._recordCount = value;
        }


        private String _fileFormat;

    
        @PropMeta(propId=6)
    
        public String getFileFormat(){
            return _fileFormat;
        }

        public void setFileFormat(String value){
            this._fileFormat = value;
        }


        private String _fileContent;

    
        @PropMeta(propId=7)
    
        public String getFileContent(){
            return _fileContent;
        }

        public void setFileContent(String value){
            this._fileContent = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _bankId;

    
        @PropMeta(propId=9)
    
        public Long getBankId(){
            return _bankId;
        }

        public void setBankId(Long value){
            this._bankId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=10)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
