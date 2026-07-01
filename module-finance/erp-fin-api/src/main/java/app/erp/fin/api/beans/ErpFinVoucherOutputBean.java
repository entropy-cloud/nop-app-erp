//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinVoucherOutputBean {

    
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


        private Integer _voucherType;

    
        @PropMeta(propId=3)
    
        public Integer getVoucherType(){
            return _voucherType;
        }

        public void setVoucherType(Integer value){
            this._voucherType = value;
        }


        private String _voucherType_label;

    
        public String getVoucherType_label(){
            return _voucherType_label;
        }

        public void setVoucherType_label(String value){
            this._voucherType_label = value;
        }


        private Integer _postingType;

    
        @PropMeta(propId=4)
    
        public Integer getPostingType(){
            return _postingType;
        }

        public void setPostingType(Integer value){
            this._postingType = value;
        }


        private String _postingType_label;

    
        public String getPostingType_label(){
            return _postingType_label;
        }

        public void setPostingType_label(String value){
            this._postingType_label = value;
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


        private String _totalDebit;

    
        @PropMeta(propId=10)
    
        public String getTotalDebit(){
            return _totalDebit;
        }

        public void setTotalDebit(String value){
            this._totalDebit = value;
        }


        private String _totalCredit;

    
        @PropMeta(propId=11)
    
        public String getTotalCredit(){
            return _totalCredit;
        }

        public void setTotalCredit(String value){
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


        private Integer _docStatus;

    
        @PropMeta(propId=14)
    
        public Integer getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(Integer value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=15)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=16)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=18)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=19)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=20)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=22)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _period;

        public Map<String,Object> getPeriod(){
            return _period;
        }

        public void setPeriod(Map<String,Object> value){
            this._period = value;
        }


        private Map<String,Object> _acctSchema;

        public Map<String,Object> getAcctSchema(){
            return _acctSchema;
        }

        public void setAcctSchema(Map<String,Object> value){
            this._acctSchema = value;
        }


        private List<Map<String,Object>> _lines;

        public List<Map<String,Object>> getLines(){
            return _lines;
        }

        public void setLines(List<Map<String,Object>> value){
            this._lines = value;
        }


        private List<Map<String,Object>> _billLinks;

        public List<Map<String,Object>> getBillLinks(){
            return _billLinks;
        }

        public void setBillLinks(List<Map<String,Object>> value){
            this._billLinks = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _reversalOfVoucher;

        public Map<String,Object> getReversalOfVoucher(){
            return _reversalOfVoucher;
        }

        public void setReversalOfVoucher(Map<String,Object> value){
            this._reversalOfVoucher = value;
        }


    }
