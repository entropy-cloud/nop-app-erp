//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsCatalogFulfillmentInputBean extends CrudInputBase {

    
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


        private String _catalogItemId;

    
        @PropMeta(propId=4)
    
        public String getCatalogItemId(){
            return _catalogItemId;
        }

        public void setCatalogItemId(String value){
            this._catalogItemId = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=5)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
        }


        private String _actionType;

    
        @PropMeta(propId=6)
    
        public String getActionType(){
            return _actionType;
        }

        public void setActionType(String value){
            this._actionType = value;
        }


        private String _actionConfig;

    
        @PropMeta(propId=7)
    
        public String getActionConfig(){
            return _actionConfig;
        }

        public void setActionConfig(String value){
            this._actionConfig = value;
        }


        private String _assignToRole;

    
        @PropMeta(propId=8)
    
        public String getAssignToRole(){
            return _assignToRole;
        }

        public void setAssignToRole(String value){
            this._assignToRole = value;
        }


        private Integer _estimatedDuration;

    
        @PropMeta(propId=9)
    
        public Integer getEstimatedDuration(){
            return _estimatedDuration;
        }

        public void setEstimatedDuration(Integer value){
            this._estimatedDuration = value;
        }


        private Boolean _isMandatory;

    
        @PropMeta(propId=10)
    
        public Boolean getIsMandatory(){
            return _isMandatory;
        }

        public void setIsMandatory(Boolean value){
            this._isMandatory = value;
        }


    }
