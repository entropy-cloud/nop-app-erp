//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrCompetencyLevelInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _competencyId;

    
        @PropMeta(propId=2)
    
        public String getCompetencyId(){
            return _competencyId;
        }

        public void setCompetencyId(String value){
            this._competencyId = value;
        }


        private Integer _levelNumber;

    
        @PropMeta(propId=3)
    
        public Integer getLevelNumber(){
            return _levelNumber;
        }

        public void setLevelNumber(Integer value){
            this._levelNumber = value;
        }


        private String _levelName;

    
        @PropMeta(propId=4)
    
        public String getLevelName(){
            return _levelName;
        }

        public void setLevelName(String value){
            this._levelName = value;
        }


        private String _behavioralAnchor;

    
        @PropMeta(propId=5)
    
        public String getBehavioralAnchor(){
            return _behavioralAnchor;
        }

        public void setBehavioralAnchor(String value){
            this._behavioralAnchor = value;
        }


        private Integer _sortOrder;

    
        @PropMeta(propId=6)
    
        public Integer getSortOrder(){
            return _sortOrder;
        }

        public void setSortOrder(Integer value){
            this._sortOrder = value;
        }


    }
