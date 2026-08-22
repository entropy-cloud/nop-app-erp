//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrRoleCompetencyInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _positionId;

    
        @PropMeta(propId=2)
    
        public String getPositionId(){
            return _positionId;
        }

        public void setPositionId(String value){
            this._positionId = value;
        }


        private String _competencyId;

    
        @PropMeta(propId=3)
    
        public String getCompetencyId(){
            return _competencyId;
        }

        public void setCompetencyId(String value){
            this._competencyId = value;
        }


        private Integer _requiredLevel;

    
        @PropMeta(propId=4)
    
        public Integer getRequiredLevel(){
            return _requiredLevel;
        }

        public void setRequiredLevel(Integer value){
            this._requiredLevel = value;
        }


        private java.math.BigDecimal _weight;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getWeight(){
            return _weight;
        }

        public void setWeight(java.math.BigDecimal value){
            this._weight = value;
        }


        private Boolean _isCritical;

    
        @PropMeta(propId=6)
    
        public Boolean getIsCritical(){
            return _isCritical;
        }

        public void setIsCritical(Boolean value){
            this._isCritical = value;
        }


    }
