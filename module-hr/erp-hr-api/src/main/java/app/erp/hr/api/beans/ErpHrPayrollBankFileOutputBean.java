//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrPayrollBankFileOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _fileFormat_label;

    
        public String getFileFormat_label(){
            return _fileFormat_label;
        }

        public void setFileFormat_label(String value){
            this._fileFormat_label = value;
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


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private String _bankId;

    
        @PropMeta(propId=9)
    
        public String getBankId(){
            return _bankId;
        }

        public void setBankId(String value){
            this._bankId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=10)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
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


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=13)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=14)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=16)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _bank;

        public Map<String,Object> getBank(){
            return _bank;
        }

        public void setBank(Map<String,Object> value){
            this._bank = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
