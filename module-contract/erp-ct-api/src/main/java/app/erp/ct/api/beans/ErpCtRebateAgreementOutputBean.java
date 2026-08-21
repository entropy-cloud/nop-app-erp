//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtRebateAgreementOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _contractId;

    
        @PropMeta(propId=4)
    
        public String getContractId(){
            return _contractId;
        }

        public void setContractId(String value){
            this._contractId = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=5)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private String _rebateType;

    
        @PropMeta(propId=6)
    
        public String getRebateType(){
            return _rebateType;
        }

        public void setRebateType(String value){
            this._rebateType = value;
        }


        private String _rebateType_label;

    
        public String getRebateType_label(){
            return _rebateType_label;
        }

        public void setRebateType_label(String value){
            this._rebateType_label = value;
        }


        private java.time.LocalDate _agreementDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getAgreementDate(){
            return _agreementDate;
        }

        public void setAgreementDate(java.time.LocalDate value){
            this._agreementDate = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private String _accrualMethod;

    
        @PropMeta(propId=10)
    
        public String getAccrualMethod(){
            return _accrualMethod;
        }

        public void setAccrualMethod(String value){
            this._accrualMethod = value;
        }


        private String _accrualMethod_label;

    
        public String getAccrualMethod_label(){
            return _accrualMethod_label;
        }

        public void setAccrualMethod_label(String value){
            this._accrualMethod_label = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
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


        private java.math.BigDecimal _totalAccumulatedAmount;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getTotalAccumulatedAmount(){
            return _totalAccumulatedAmount;
        }

        public void setTotalAccumulatedAmount(java.math.BigDecimal value){
            this._totalAccumulatedAmount = value;
        }


        private java.math.BigDecimal _estimatedRebateAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getEstimatedRebateAmount(){
            return _estimatedRebateAmount;
        }

        public void setEstimatedRebateAmount(java.math.BigDecimal value){
            this._estimatedRebateAmount = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=16)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=17)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=19)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=21)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Map<String,Object> _contract;

        public Map<String,Object> getContract(){
            return _contract;
        }

        public void setContract(Map<String,Object> value){
            this._contract = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


    }
