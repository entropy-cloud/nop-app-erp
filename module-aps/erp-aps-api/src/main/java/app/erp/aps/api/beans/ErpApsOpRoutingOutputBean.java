//__XGEN_FORCE_OVERRIDE__
    package app.erp.aps.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpApsOpRoutingOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _orgId;

    
        @PropMeta(propId=2)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _operationId;

    
        @PropMeta(propId=3)
    
        public String getOperationId(){
            return _operationId;
        }

        public void setOperationId(String value){
            this._operationId = value;
        }


        private String _machineId;

    
        @PropMeta(propId=4)
    
        public String getMachineId(){
            return _machineId;
        }

        public void setMachineId(String value){
            this._machineId = value;
        }


        private Integer _priority;

    
        @PropMeta(propId=5)
    
        public Integer getPriority(){
            return _priority;
        }

        public void setPriority(Integer value){
            this._priority = value;
        }


        private java.math.BigDecimal _setupTimeDelta;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getSetupTimeDelta(){
            return _setupTimeDelta;
        }

        public void setSetupTimeDelta(java.math.BigDecimal value){
            this._setupTimeDelta = value;
        }


        private java.math.BigDecimal _runtimePerUnitDelta;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getRuntimePerUnitDelta(){
            return _runtimePerUnitDelta;
        }

        public void setRuntimePerUnitDelta(java.math.BigDecimal value){
            this._runtimePerUnitDelta = value;
        }


        private Boolean _isDefault;

    
        @PropMeta(propId=8)
    
        public Boolean getIsDefault(){
            return _isDefault;
        }

        public void setIsDefault(Boolean value){
            this._isDefault = value;
        }


        private Boolean _isEnabled;

    
        @PropMeta(propId=9)
    
        public Boolean getIsEnabled(){
            return _isEnabled;
        }

        public void setIsEnabled(Boolean value){
            this._isEnabled = value;
        }


        private java.time.LocalDate _effectiveFrom;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getEffectiveFrom(){
            return _effectiveFrom;
        }

        public void setEffectiveFrom(java.time.LocalDate value){
            this._effectiveFrom = value;
        }


        private java.time.LocalDate _effectiveTo;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getEffectiveTo(){
            return _effectiveTo;
        }

        public void setEffectiveTo(java.time.LocalDate value){
            this._effectiveTo = value;
        }


        private java.math.BigDecimal _minBatchQty;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getMinBatchQty(){
            return _minBatchQty;
        }

        public void setMinBatchQty(java.math.BigDecimal value){
            this._minBatchQty = value;
        }


        private java.math.BigDecimal _maxBatchQty;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getMaxBatchQty(){
            return _maxBatchQty;
        }

        public void setMaxBatchQty(java.math.BigDecimal value){
            this._maxBatchQty = value;
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


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
