//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSurveyQuestionInputBean extends CrudInputBase {

    
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


        private Boolean _isRequired;

    
        @PropMeta(propId=12)
    
        public Boolean getIsRequired(){
            return _isRequired;
        }

        public void setIsRequired(Boolean value){
            this._isRequired = value;
        }


    }
