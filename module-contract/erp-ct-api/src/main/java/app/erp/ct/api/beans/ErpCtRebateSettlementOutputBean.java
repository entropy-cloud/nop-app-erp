//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtRebateSettlementOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _rebateAgreementId;

    
        @PropMeta(propId=2)
    
        public Long getRebateAgreementId(){
            return _rebateAgreementId;
        }

        public void setRebateAgreementId(Long value){
            this._rebateAgreementId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.time.LocalDate _settlementDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getSettlementDate(){
            return _settlementDate;
        }

        public void setSettlementDate(java.time.LocalDate value){
            this._settlementDate = value;
        }


        private java.math.BigDecimal _totalRebateAmount;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getTotalRebateAmount(){
            return _totalRebateAmount;
        }

        public void setTotalRebateAmount(java.math.BigDecimal value){
            this._totalRebateAmount = value;
        }


        private String _creditMemoBillType;

    
        @PropMeta(propId=6)
    
        public String getCreditMemoBillType(){
            return _creditMemoBillType;
        }

        public void setCreditMemoBillType(String value){
            this._creditMemoBillType = value;
        }


        private String _creditMemoBillCode;

    
        @PropMeta(propId=7)
    
        public String getCreditMemoBillCode(){
            return _creditMemoBillCode;
        }

        public void setCreditMemoBillCode(String value){
            this._creditMemoBillCode = value;
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


        private java.sql.Timestamp _postedAt;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.sql.Timestamp value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=10)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
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


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=19)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private Map<String,Object> _rebateAgreement;

        public Map<String,Object> getRebateAgreement(){
            return _rebateAgreement;
        }

        public void setRebateAgreement(Map<String,Object> value){
            this._rebateAgreement = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
