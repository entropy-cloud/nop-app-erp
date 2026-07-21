//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgMrpScenarioVersionOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _scenarioId;

    
        @PropMeta(propId=2)
    
        public Long getScenarioId(){
            return _scenarioId;
        }

        public void setScenarioId(Long value){
            this._scenarioId = value;
        }


        private Integer _versionNo;

    
        @PropMeta(propId=3)
    
        public Integer getVersionNo(){
            return _versionNo;
        }

        public void setVersionNo(Integer value){
            this._versionNo = value;
        }


        private Long _computedMrpPlanId;

    
        @PropMeta(propId=4)
    
        public Long getComputedMrpPlanId(){
            return _computedMrpPlanId;
        }

        public void setComputedMrpPlanId(Long value){
            this._computedMrpPlanId = value;
        }


        private String _snapshotSummary;

    
        @PropMeta(propId=5)
    
        public String getSnapshotSummary(){
            return _snapshotSummary;
        }

        public void setSnapshotSummary(String value){
            this._snapshotSummary = value;
        }


        private String _status;

    
        @PropMeta(propId=6)
    
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


        private Long _promotedPlanId;

    
        @PropMeta(propId=7)
    
        public Long getPromotedPlanId(){
            return _promotedPlanId;
        }

        public void setPromotedPlanId(Long value){
            this._promotedPlanId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=8)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=9)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=10)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=12)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _scenario;

        public Map<String,Object> getScenario(){
            return _scenario;
        }

        public void setScenario(Map<String,Object> value){
            this._scenario = value;
        }


        private Map<String,Object> _computedMrpPlan;

        public Map<String,Object> getComputedMrpPlan(){
            return _computedMrpPlan;
        }

        public void setComputedMrpPlan(Map<String,Object> value){
            this._computedMrpPlan = value;
        }


        private Map<String,Object> _promotedPlan;

        public Map<String,Object> getPromotedPlan(){
            return _promotedPlan;
        }

        public void setPromotedPlan(Map<String,Object> value){
            this._promotedPlan = value;
        }


    }
