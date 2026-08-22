//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSurveyResultOutputBean {

    
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


        private String _departmentId;

    
        @PropMeta(propId=3)
    
        public String getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(String value){
            this._departmentId = value;
        }


        private Integer _totalResponses;

    
        @PropMeta(propId=4)
    
        public Integer getTotalResponses(){
            return _totalResponses;
        }

        public void setTotalResponses(Integer value){
            this._totalResponses = value;
        }


        private java.math.BigDecimal _avgScore;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getAvgScore(){
            return _avgScore;
        }

        public void setAvgScore(java.math.BigDecimal value){
            this._avgScore = value;
        }


        private Integer _eNpsScore;

    
        @PropMeta(propId=6)
    
        public Integer getENpsScore(){
            return _eNpsScore;
        }

        public void setENpsScore(Integer value){
            this._eNpsScore = value;
        }


        private String _driverScores;

    
        @PropMeta(propId=7)
    
        public String getDriverScores(){
            return _driverScores;
        }

        public void setDriverScores(String value){
            this._driverScores = value;
        }


        private String _questionBreakdown;

    
        @PropMeta(propId=8)
    
        public String getQuestionBreakdown(){
            return _questionBreakdown;
        }

        public void setQuestionBreakdown(String value){
            this._questionBreakdown = value;
        }


        private String _trendData;

    
        @PropMeta(propId=9)
    
        public String getTrendData(){
            return _trendData;
        }

        public void setTrendData(String value){
            this._trendData = value;
        }


        private java.sql.Timestamp _lastCalculatedAt;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getLastCalculatedAt(){
            return _lastCalculatedAt;
        }

        public void setLastCalculatedAt(java.sql.Timestamp value){
            this._lastCalculatedAt = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=12)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=13)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=15)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=16)
    
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


        private Map<String,Object> _department;

        public Map<String,Object> getDepartment(){
            return _department;
        }

        public void setDepartment(Map<String,Object> value){
            this._department = value;
        }


    }
