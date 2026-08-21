//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtRebateAccrualOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _rebateAgreementId;

    
        @PropMeta(propId=2)
    
        public String getRebateAgreementId(){
            return _rebateAgreementId;
        }

        public void setRebateAgreementId(String value){
            this._rebateAgreementId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _sourceBillType;

    
        @PropMeta(propId=4)
    
        public String getSourceBillType(){
            return _sourceBillType;
        }

        public void setSourceBillType(String value){
            this._sourceBillType = value;
        }


        private String _sourceBillCode;

    
        @PropMeta(propId=5)
    
        public String getSourceBillCode(){
            return _sourceBillCode;
        }

        public void setSourceBillCode(String value){
            this._sourceBillCode = value;
        }


        private java.math.BigDecimal _billAmountSource;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getBillAmountSource(){
            return _billAmountSource;
        }

        public void setBillAmountSource(java.math.BigDecimal value){
            this._billAmountSource = value;
        }


        private java.math.BigDecimal _accruedRebate;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getAccruedRebate(){
            return _accruedRebate;
        }

        public void setAccruedRebate(java.math.BigDecimal value){
            this._accruedRebate = value;
        }


        private java.time.LocalDate _accrualDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getAccrualDate(){
            return _accrualDate;
        }

        public void setAccrualDate(java.time.LocalDate value){
            this._accrualDate = value;
        }


        private Boolean _isSettled;

    
        @PropMeta(propId=9)
    
        public Boolean getIsSettled(){
            return _isSettled;
        }

        public void setIsSettled(Boolean value){
            this._isSettled = value;
        }


        private java.time.LocalDate _settledDate;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getSettledDate(){
            return _settledDate;
        }

        public void setSettledDate(java.time.LocalDate value){
            this._settledDate = value;
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
