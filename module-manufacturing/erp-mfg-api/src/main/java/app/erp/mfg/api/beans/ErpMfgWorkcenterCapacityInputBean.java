//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgWorkcenterCapacityInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _workcenterId;

    
        @PropMeta(propId=2)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=4)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private java.math.BigDecimal _capacityPerHour;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getCapacityPerHour(){
            return _capacityPerHour;
        }

        public void setCapacityPerHour(java.math.BigDecimal value){
            this._capacityPerHour = value;
        }


        private java.math.BigDecimal _setupTime;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getSetupTime(){
            return _setupTime;
        }

        public void setSetupTime(java.math.BigDecimal value){
            this._setupTime = value;
        }


        private java.math.BigDecimal _cleanupTime;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getCleanupTime(){
            return _cleanupTime;
        }

        public void setCleanupTime(java.math.BigDecimal value){
            this._cleanupTime = value;
        }


        private java.math.BigDecimal _efficiencyFactor;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getEfficiencyFactor(){
            return _efficiencyFactor;
        }

        public void setEfficiencyFactor(java.math.BigDecimal value){
            this._efficiencyFactor = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=9)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
