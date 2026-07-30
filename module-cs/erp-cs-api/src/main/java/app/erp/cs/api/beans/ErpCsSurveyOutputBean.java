//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsSurveyOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _ticketId;

    
        @PropMeta(propId=3)
    
        public Long getTicketId(){
            return _ticketId;
        }

        public void setTicketId(Long value){
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


        private String _surveyChannel_label;

    
        public String getSurveyChannel_label(){
            return _surveyChannel_label;
        }

        public void setSurveyChannel_label(String value){
            this._surveyChannel_label = value;
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


        private Map<String,Object> _ticket;

        public Map<String,Object> getTicket(){
            return _ticket;
        }

        public void setTicket(Map<String,Object> value){
            this._ticket = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
