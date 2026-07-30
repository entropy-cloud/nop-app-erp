//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSurveyAnswerOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _responseId;

    
        @PropMeta(propId=2)
    
        public Long getResponseId(){
            return _responseId;
        }

        public void setResponseId(Long value){
            this._responseId = value;
        }


        private Long _questionId;

    
        @PropMeta(propId=3)
    
        public Long getQuestionId(){
            return _questionId;
        }

        public void setQuestionId(Long value){
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


        private Long _delVersion;

    
        @PropMeta(propId=7)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=8)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=9)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=11)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _response;

        public Map<String,Object> getResponse(){
            return _response;
        }

        public void setResponse(Map<String,Object> value){
            this._response = value;
        }


        private Map<String,Object> _question;

        public Map<String,Object> getQuestion(){
            return _question;
        }

        public void setQuestion(Map<String,Object> value){
            this._question = value;
        }


    }
