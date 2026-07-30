//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpDrpScenarioInputBean extends CrudInputBase {

    
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


        private Long _baseDrpPlanId;

    
        @PropMeta(propId=4)
    
        public Long getBaseDrpPlanId(){
            return _baseDrpPlanId;
        }

        public void setBaseDrpPlanId(Long value){
            this._baseDrpPlanId = value;
        }


        private String _description;

    
        @PropMeta(propId=5)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _status;

    
        @PropMeta(propId=6)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private List<ErpDrpScenarioVersionInputBean> _versions;

        public List<ErpDrpScenarioVersionInputBean> getVersions(){
            return _versions;
        }

        public void setVersions(List<ErpDrpScenarioVersionInputBean> value){
            this._versions = value;
        }


        private List<ErpDrpScenarioParamInputBean> _params;

        public List<ErpDrpScenarioParamInputBean> getParams(){
            return _params;
        }

        public void setParams(List<ErpDrpScenarioParamInputBean> value){
            this._params = value;
        }


    }
