//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgMrpScenarioParamOutputBean {

    
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


        private String _paramType_label;

    
        public String getParamType_label(){
            return _paramType_label;
        }

        public void setParamType_label(String value){
            this._paramType_label = value;
        }


        private java.math.BigDecimal _paramValue;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getParamValue(){
            return _paramValue;
        }

        public void setParamValue(java.math.BigDecimal value){
            this._paramValue = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=6)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=7)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=8)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=10)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=11)
    
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


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


    }
