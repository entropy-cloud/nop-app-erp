//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpDrpScenarioParamInputBean extends CrudInputBase {

    
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


        private String _materialId;

    
        @PropMeta(propId=3)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=4)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _paramType;

    
        @PropMeta(propId=5)
    
        public String getParamType(){
            return _paramType;
        }

        public void setParamType(String value){
            this._paramType = value;
        }


        private java.math.BigDecimal _paramValue;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getParamValue(){
            return _paramValue;
        }

        public void setParamValue(java.math.BigDecimal value){
            this._paramValue = value;
        }


    }
