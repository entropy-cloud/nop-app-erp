//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSurveyResultInputBean extends CrudInputBase {

    
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


        private Long _departmentId;

    
        @PropMeta(propId=3)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
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


    }
