//__XGEN_FORCE_OVERRIDE__
    package app.erp.aps.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpApsDispatchRuleInputBean extends CrudInputBase {

    
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


        private String _workcenterId;

    
        @PropMeta(propId=3)
    
        public String getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(String value){
            this._workcenterId = value;
        }


        private String _ruleName;

    
        @PropMeta(propId=4)
    
        public String getRuleName(){
            return _ruleName;
        }

        public void setRuleName(String value){
            this._ruleName = value;
        }


        private Boolean _enableAuto;

    
        @PropMeta(propId=5)
    
        public Boolean getEnableAuto(){
            return _enableAuto;
        }

        public void setEnableAuto(Boolean value){
            this._enableAuto = value;
        }


        private Boolean _requireMaterial;

    
        @PropMeta(propId=6)
    
        public Boolean getRequireMaterial(){
            return _requireMaterial;
        }

        public void setRequireMaterial(Boolean value){
            this._requireMaterial = value;
        }


        private Boolean _requireOperator;

    
        @PropMeta(propId=7)
    
        public Boolean getRequireOperator(){
            return _requireOperator;
        }

        public void setRequireOperator(Boolean value){
            this._requireOperator = value;
        }


        private Boolean _requireTooling;

    
        @PropMeta(propId=8)
    
        public Boolean getRequireTooling(){
            return _requireTooling;
        }

        public void setRequireTooling(Boolean value){
            this._requireTooling = value;
        }


        private Integer _maxLookaheadMinutes;

    
        @PropMeta(propId=9)
    
        public Integer getMaxLookaheadMinutes(){
            return _maxLookaheadMinutes;
        }

        public void setMaxLookaheadMinutes(Integer value){
            this._maxLookaheadMinutes = value;
        }


        private Integer _dispatchAheadMinutes;

    
        @PropMeta(propId=10)
    
        public Integer getDispatchAheadMinutes(){
            return _dispatchAheadMinutes;
        }

        public void setDispatchAheadMinutes(Integer value){
            this._dispatchAheadMinutes = value;
        }


        private Boolean _autoConfirmMaterial;

    
        @PropMeta(propId=11)
    
        public Boolean getAutoConfirmMaterial(){
            return _autoConfirmMaterial;
        }

        public void setAutoConfirmMaterial(Boolean value){
            this._autoConfirmMaterial = value;
        }


        private Integer _maxConcurrentOps;

    
        @PropMeta(propId=12)
    
        public Integer getMaxConcurrentOps(){
            return _maxConcurrentOps;
        }

        public void setMaxConcurrentOps(Integer value){
            this._maxConcurrentOps = value;
        }


        private Integer _priorityThreshold;

    
        @PropMeta(propId=13)
    
        public Integer getPriorityThreshold(){
            return _priorityThreshold;
        }

        public void setPriorityThreshold(Integer value){
            this._priorityThreshold = value;
        }


        private String _enabledHours;

    
        @PropMeta(propId=14)
    
        public String getEnabledHours(){
            return _enabledHours;
        }

        public void setEnabledHours(String value){
            this._enabledHours = value;
        }


        private java.sql.Timestamp _holdUntil;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getHoldUntil(){
            return _holdUntil;
        }

        public void setHoldUntil(java.sql.Timestamp value){
            this._holdUntil = value;
        }


        private String _holdReason;

    
        @PropMeta(propId=16)
    
        public String getHoldReason(){
            return _holdReason;
        }

        public void setHoldReason(String value){
            this._holdReason = value;
        }


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
