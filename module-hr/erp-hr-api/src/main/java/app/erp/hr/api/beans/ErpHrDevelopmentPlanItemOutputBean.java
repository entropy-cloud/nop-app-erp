//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrDevelopmentPlanItemOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _planId;

    
        @PropMeta(propId=2)
    
        public String getPlanId(){
            return _planId;
        }

        public void setPlanId(String value){
            this._planId = value;
        }


        private String _competencyId;

    
        @PropMeta(propId=3)
    
        public String getCompetencyId(){
            return _competencyId;
        }

        public void setCompetencyId(String value){
            this._competencyId = value;
        }


        private String _gapId;

    
        @PropMeta(propId=4)
    
        public String getGapId(){
            return _gapId;
        }

        public void setGapId(String value){
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


        private String _mentorId;

    
        @PropMeta(propId=7)
    
        public String getMentorId(){
            return _mentorId;
        }

        public void setMentorId(String value){
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


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private String _progressNote;

    
        @PropMeta(propId=11)
    
        public String getProgressNote(){
            return _progressNote;
        }

        public void setProgressNote(String value){
            this._progressNote = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=13)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=14)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=16)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _plan;

        public Map<String,Object> getPlan(){
            return _plan;
        }

        public void setPlan(Map<String,Object> value){
            this._plan = value;
        }


        private Map<String,Object> _competency;

        public Map<String,Object> getCompetency(){
            return _competency;
        }

        public void setCompetency(Map<String,Object> value){
            this._competency = value;
        }


        private Map<String,Object> _gap;

        public Map<String,Object> getGap(){
            return _gap;
        }

        public void setGap(Map<String,Object> value){
            this._gap = value;
        }


        private Map<String,Object> _mentor;

        public Map<String,Object> getMentor(){
            return _mentor;
        }

        public void setMentor(Map<String,Object> value){
            this._mentor = value;
        }


    }
