//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSurveyResponseInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _surveyId;

    
        @PropMeta(propId=2)
    
        public String getSurveyId(){
            return _surveyId;
        }

        public void setSurveyId(String value){
            this._surveyId = value;
        }


        private String _employeeId;

    
        @PropMeta(propId=3)
    
        public String getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=8)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private List<ErpHrSurveyAnswerInputBean> _answers;

        public List<ErpHrSurveyAnswerInputBean> getAnswers(){
            return _answers;
        }

        public void setAnswers(List<ErpHrSurveyAnswerInputBean> value){
            this._answers = value;
        }


    }
