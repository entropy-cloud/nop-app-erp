//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtApprovalMatrixOutputBean {

    
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


        private java.math.BigDecimal _minAmount;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getMinAmount(){
            return _minAmount;
        }

        public void setMinAmount(java.math.BigDecimal value){
            this._minAmount = value;
        }


        private java.math.BigDecimal _maxAmount;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getMaxAmount(){
            return _maxAmount;
        }

        public void setMaxAmount(java.math.BigDecimal value){
            this._maxAmount = value;
        }


        private String _approverRole;

    
        @PropMeta(propId=6)
    
        public String getApproverRole(){
            return _approverRole;
        }

        public void setApproverRole(String value){
            this._approverRole = value;
        }


        private Integer _approvalOrder;

    
        @PropMeta(propId=7)
    
        public Integer getApprovalOrder(){
            return _approvalOrder;
        }

        public void setApprovalOrder(Integer value){
            this._approvalOrder = value;
        }


        private String _contractType;

    
        @PropMeta(propId=8)
    
        public String getContractType(){
            return _contractType;
        }

        public void setContractType(String value){
            this._contractType = value;
        }


        private Boolean _allowSkip;

    
        @PropMeta(propId=9)
    
        public Boolean getAllowSkip(){
            return _allowSkip;
        }

        public void setAllowSkip(Boolean value){
            this._allowSkip = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=10)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
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


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
