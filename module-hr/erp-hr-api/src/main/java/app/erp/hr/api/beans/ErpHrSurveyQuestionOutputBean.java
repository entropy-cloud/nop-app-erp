//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSurveyQuestionOutputBean {

    
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


        private Integer _sortOrder;

    
        @PropMeta(propId=3)
    
        public Integer getSortOrder(){
            return _sortOrder;
        }

        public void setSortOrder(Integer value){
            this._sortOrder = value;
        }


        private String _questionText;

    
        @PropMeta(propId=4)
    
        public String getQuestionText(){
            return _questionText;
        }

        public void setQuestionText(String value){
            this._questionText = value;
        }


        private String _questionType;

    
        @PropMeta(propId=5)
    
        public String getQuestionType(){
            return _questionType;
        }

        public void setQuestionType(String value){
            this._questionType = value;
        }


        private String _questionType_label;

    
        public String getQuestionType_label(){
            return _questionType_label;
        }

        public void setQuestionType_label(String value){
            this._questionType_label = value;
        }


        private Integer _ratingScaleMin;

    
        @PropMeta(propId=6)
    
        public Integer getRatingScaleMin(){
            return _ratingScaleMin;
        }

        public void setRatingScaleMin(Integer value){
            this._ratingScaleMin = value;
        }


        private Integer _ratingScaleMax;

    
        @PropMeta(propId=7)
    
        public Integer getRatingScaleMax(){
            return _ratingScaleMax;
        }

        public void setRatingScaleMax(Integer value){
            this._ratingScaleMax = value;
        }


        private String _ratingLabelMin;

    
        @PropMeta(propId=8)
    
        public String getRatingLabelMin(){
            return _ratingLabelMin;
        }

        public void setRatingLabelMin(String value){
            this._ratingLabelMin = value;
        }


        private String _ratingLabelMax;

    
        @PropMeta(propId=9)
    
        public String getRatingLabelMax(){
            return _ratingLabelMax;
        }

        public void setRatingLabelMax(String value){
            this._ratingLabelMax = value;
        }


        private String _options;

    
        @PropMeta(propId=10)
    
        public String getOptions(){
            return _options;
        }

        public void setOptions(String value){
            this._options = value;
        }


        private String _driverCategory;

    
        @PropMeta(propId=11)
    
        public String getDriverCategory(){
            return _driverCategory;
        }

        public void setDriverCategory(String value){
            this._driverCategory = value;
        }


        private String _driverCategory_label;

    
        public String getDriverCategory_label(){
            return _driverCategory_label;
        }

        public void setDriverCategory_label(String value){
            this._driverCategory_label = value;
        }


        private Boolean _isRequired;

    
        @PropMeta(propId=12)
    
        public Boolean getIsRequired(){
            return _isRequired;
        }

        public void setIsRequired(Boolean value){
            this._isRequired = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
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


    }
