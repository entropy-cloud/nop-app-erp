//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgMrpScenarioVersionInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _scenarioId;

    
        @PropMeta(propId=2)
    
        public String getScenarioId(){
            return _scenarioId;
        }

        public void setScenarioId(String value){
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


        private String _computedMrpPlanId;

    
        @PropMeta(propId=4)
    
        public String getComputedMrpPlanId(){
            return _computedMrpPlanId;
        }

        public void setComputedMrpPlanId(String value){
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


        private String _promotedPlanId;

    
        @PropMeta(propId=7)
    
        public String getPromotedPlanId(){
            return _promotedPlanId;
        }

        public void setPromotedPlanId(String value){
            this._promotedPlanId = value;
        }


    }
