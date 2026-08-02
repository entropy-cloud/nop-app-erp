//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrAssessmentDetailInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _assessmentId;

    
        @PropMeta(propId=2)
    
        public Long getAssessmentId(){
            return _assessmentId;
        }

        public void setAssessmentId(Long value){
            this._assessmentId = value;
        }


        private Long _competencyId;

    
        @PropMeta(propId=3)
    
        public Long getCompetencyId(){
            return _competencyId;
        }

        public void setCompetencyId(Long value){
            this._competencyId = value;
        }


        private Integer _actualLevel;

    
        @PropMeta(propId=4)
    
        public Integer getActualLevel(){
            return _actualLevel;
        }

        public void setActualLevel(Integer value){
            this._actualLevel = value;
        }


        private String _comment;

    
        @PropMeta(propId=5)
    
        public String getComment(){
            return _comment;
        }

        public void setComment(String value){
            this._comment = value;
        }


        private String _sourceType;

    
        @PropMeta(propId=6)
    
        public String getSourceType(){
            return _sourceType;
        }

        public void setSourceType(String value){
            this._sourceType = value;
        }


    }
