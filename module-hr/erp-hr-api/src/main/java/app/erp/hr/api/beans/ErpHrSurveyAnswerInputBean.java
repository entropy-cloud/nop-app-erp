//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSurveyAnswerInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _responseId;

    
        @PropMeta(propId=2)
    
        public String getResponseId(){
            return _responseId;
        }

        public void setResponseId(String value){
            this._responseId = value;
        }


        private String _questionId;

    
        @PropMeta(propId=3)
    
        public String getQuestionId(){
            return _questionId;
        }

        public void setQuestionId(String value){
            this._questionId = value;
        }


        private Integer _ratingValue;

    
        @PropMeta(propId=4)
    
        public Integer getRatingValue(){
            return _ratingValue;
        }

        public void setRatingValue(Integer value){
            this._ratingValue = value;
        }


        private String _selectedOption;

    
        @PropMeta(propId=5)
    
        public String getSelectedOption(){
            return _selectedOption;
        }

        public void setSelectedOption(String value){
            this._selectedOption = value;
        }


        private String _openText;

    
        @PropMeta(propId=6)
    
        public String getOpenText(){
            return _openText;
        }

        public void setOpenText(String value){
            this._openText = value;
        }


    }
