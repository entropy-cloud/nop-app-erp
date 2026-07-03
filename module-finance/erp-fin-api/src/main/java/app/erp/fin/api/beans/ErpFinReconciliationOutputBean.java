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
    public class ErpFinReconciliationOutputBean {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=4)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private String _direction;

    
        @PropMeta(propId=5)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private String _direction_label;

    
        public String getDirection_label(){
            return _direction_label;
        }

        public void setDirection_label(String value){
            this._direction_label = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=6)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=8)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _totalAmountSource;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getTotalAmountSource(){
            return _totalAmountSource;
        }

        public void setTotalAmountSource(java.math.BigDecimal value){
            this._totalAmountSource = value;
        }


        private java.math.BigDecimal _totalAmountFunctional;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getTotalAmountFunctional(){
            return _totalAmountFunctional;
        }

        public void setTotalAmountFunctional(java.math.BigDecimal value){
            this._totalAmountFunctional = value;
        }


        private java.math.BigDecimal _fxGainLoss;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getFxGainLoss(){
            return _fxGainLoss;
        }

        public void setFxGainLoss(java.math.BigDecimal value){
            this._fxGainLoss = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=13)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
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

    
        @PropMeta(propId=14)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=15)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=16)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=17)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=18)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=19)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=21)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _acctSchema;

        public Map<String,Object> getAcctSchema(){
            return _acctSchema;
        }

        public void setAcctSchema(Map<String,Object> value){
            this._acctSchema = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private List<Map<String,Object>> _lines;

        public List<Map<String,Object>> getLines(){
            return _lines;
        }

        public void setLines(List<Map<String,Object>> value){
            this._lines = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
