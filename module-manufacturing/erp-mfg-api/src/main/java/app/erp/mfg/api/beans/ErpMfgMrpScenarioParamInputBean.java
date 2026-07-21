//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgMrpScenarioParamInputBean extends CrudInputBase {

    
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


        private Long _materialId;

    
        @PropMeta(propId=3)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private String _paramType;

    
        @PropMeta(propId=4)
    
        public String getParamType(){
            return _paramType;
        }

        public void setParamType(String value){
            this._paramType = value;
        }


        private java.math.BigDecimal _paramValue;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getParamValue(){
            return _paramValue;
        }

        public void setParamValue(java.math.BigDecimal value){
            this._paramValue = value;
        }


    }
