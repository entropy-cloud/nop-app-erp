//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrDevelopmentPlanItemInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _planId;

    
        @PropMeta(propId=2)
    
        public Long getPlanId(){
            return _planId;
        }

        public void setPlanId(Long value){
            this._planId = value;
        }


        private Long _competencyId;

    
        @PropMeta(propId=3)
    
        public Long getCompetencyId(){
            return _competencyId;
        }

        public void setCompetencyId(Long value){
            this._competencyId = value;
        }


        private Long _gapId;

    
        @PropMeta(propId=4)
    
        public Long getGapId(){
            return _gapId;
        }

        public void setGapId(Long value){
            this._gapId = value;
        }


        private Integer _targetLevel;

    
        @PropMeta(propId=5)
    
        public Integer getTargetLevel(){
            return _targetLevel;
        }

        public void setTargetLevel(Integer value){
            this._targetLevel = value;
        }


        private String _developmentAction;

    
        @PropMeta(propId=6)
    
        public String getDevelopmentAction(){
            return _developmentAction;
        }

        public void setDevelopmentAction(String value){
            this._developmentAction = value;
        }


        private Long _mentorId;

    
        @PropMeta(propId=7)
    
        public Long getMentorId(){
            return _mentorId;
        }

        public void setMentorId(Long value){
            this._mentorId = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private String _status;

    
        @PropMeta(propId=10)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _progressNote;

    
        @PropMeta(propId=11)
    
        public String getProgressNote(){
            return _progressNote;
        }

        public void setProgressNote(String value){
            this._progressNote = value;
        }


    }
