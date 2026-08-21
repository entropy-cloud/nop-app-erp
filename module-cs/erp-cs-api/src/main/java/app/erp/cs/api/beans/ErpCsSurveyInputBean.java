//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsSurveyInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _orgId;

    
        @PropMeta(propId=2)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _ticketId;

    
        @PropMeta(propId=3)
    
        public String getTicketId(){
            return _ticketId;
        }

        public void setTicketId(String value){
            this._ticketId = value;
        }


        private String _surveyToken;

    
        @PropMeta(propId=4)
    
        public String getSurveyToken(){
            return _surveyToken;
        }

        public void setSurveyToken(String value){
            this._surveyToken = value;
        }


        private Integer _csatScore;

    
        @PropMeta(propId=5)
    
        public Integer getCsatScore(){
            return _csatScore;
        }

        public void setCsatScore(Integer value){
            this._csatScore = value;
        }


        private Integer _npsScore;

    
        @PropMeta(propId=6)
    
        public Integer getNpsScore(){
            return _npsScore;
        }

        public void setNpsScore(Integer value){
            this._npsScore = value;
        }


        private Integer _cesScore;

    
        @PropMeta(propId=7)
    
        public Integer getCesScore(){
            return _cesScore;
        }

        public void setCesScore(Integer value){
            this._cesScore = value;
        }


        private String _comment;

    
        @PropMeta(propId=8)
    
        public String getComment(){
            return _comment;
        }

        public void setComment(String value){
            this._comment = value;
        }


        private java.sql.Timestamp _respondedAt;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getRespondedAt(){
            return _respondedAt;
        }

        public void setRespondedAt(java.sql.Timestamp value){
            this._respondedAt = value;
        }


        private java.sql.Timestamp _surveySentAt;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getSurveySentAt(){
            return _surveySentAt;
        }

        public void setSurveySentAt(java.sql.Timestamp value){
            this._surveySentAt = value;
        }


        private String _surveyChannel;

    
        @PropMeta(propId=11)
    
        public String getSurveyChannel(){
            return _surveyChannel;
        }

        public void setSurveyChannel(String value){
            this._surveyChannel = value;
        }


        private String _status;

    
        @PropMeta(propId=18)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Integer _failureCount;

    
        @PropMeta(propId=19)
    
        public Integer getFailureCount(){
            return _failureCount;
        }

        public void setFailureCount(Integer value){
            this._failureCount = value;
        }


    }
