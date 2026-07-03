//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurSupplierScorecardVariableInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _criteriaId;

    
        @PropMeta(propId=2)
    
        public Long getCriteriaId(){
            return _criteriaId;
        }

        public void setCriteriaId(Long value){
            this._criteriaId = value;
        }


        private String _variableName;

    
        @PropMeta(propId=3)
    
        public String getVariableName(){
            return _variableName;
        }

        public void setVariableName(String value){
            this._variableName = value;
        }


        private String _path;

    
        @PropMeta(propId=4)
    
        public String getPath(){
            return _path;
        }

        public void setPath(String value){
            this._path = value;
        }


        private java.math.BigDecimal _value;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getValue(){
            return _value;
        }

        public void setValue(java.math.BigDecimal value){
            this._value = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=6)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
