//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSurveyResponseOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _surveyId;

    
        @PropMeta(propId=2)
    
        public Long getSurveyId(){
            return _surveyId;
        }

        public void setSurveyId(Long value){
            this._surveyId = value;
        }


        private Long _employeeId;

    
        @PropMeta(propId=3)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private String _respondentHash;

    
        @PropMeta(propId=4)
    
        public String getRespondentHash(){
            return _respondentHash;
        }

        public void setRespondentHash(String value){
            this._respondentHash = value;
        }


        private java.sql.Timestamp _submittedAt;

    
        @PropMeta(propId=5)
    
        public java.sql.Timestamp getSubmittedAt(){
            return _submittedAt;
        }

        public void setSubmittedAt(java.sql.Timestamp value){
            this._submittedAt = value;
        }


        private Integer _timeSpentSeconds;

    
        @PropMeta(propId=6)
    
        public Integer getTimeSpentSeconds(){
            return _timeSpentSeconds;
        }

        public void setTimeSpentSeconds(Integer value){
            this._timeSpentSeconds = value;
        }


        private Boolean _isComplete;

    
        @PropMeta(propId=7)
    
        public Boolean getIsComplete(){
            return _isComplete;
        }

        public void setIsComplete(Boolean value){
            this._isComplete = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=8)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=10)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=11)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=13)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _survey;

        public Map<String,Object> getSurvey(){
            return _survey;
        }

        public void setSurvey(Map<String,Object> value){
            this._survey = value;
        }


        private Map<String,Object> _employee;

        public Map<String,Object> getEmployee(){
            return _employee;
        }

        public void setEmployee(Map<String,Object> value){
            this._employee = value;
        }


        private List<Map<String,Object>> _answers;

        public List<Map<String,Object>> getAnswers(){
            return _answers;
        }

        public void setAnswers(List<Map<String,Object>> value){
            this._answers = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
