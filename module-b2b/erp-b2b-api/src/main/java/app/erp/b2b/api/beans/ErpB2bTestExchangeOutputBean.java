//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bTestExchangeOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _partnerProfileId;

    
        @PropMeta(propId=2)
    
        public Long getPartnerProfileId(){
            return _partnerProfileId;
        }

        public void setPartnerProfileId(Long value){
            this._partnerProfileId = value;
        }


        private String _direction;

    
        @PropMeta(propId=3)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private String _direction_label;

    
        public String getDirection_label(){
            return _direction_label;
        }

        public void setDirection_label(String value){
            this._direction_label = value;
        }


        private String _formatCode;

    
        @PropMeta(propId=4)
    
        public String getFormatCode(){
            return _formatCode;
        }

        public void setFormatCode(String value){
            this._formatCode = value;
        }


        private String _testCaseCode;

    
        @PropMeta(propId=5)
    
        public String getTestCaseCode(){
            return _testCaseCode;
        }

        public void setTestCaseCode(String value){
            this._testCaseCode = value;
        }


        private String _sentPayload;

    
        @PropMeta(propId=6)
    
        public String getSentPayload(){
            return _sentPayload;
        }

        public void setSentPayload(String value){
            this._sentPayload = value;
        }


        private String _receivedPayload;

    
        @PropMeta(propId=7)
    
        public String getReceivedPayload(){
            return _receivedPayload;
        }

        public void setReceivedPayload(String value){
            this._receivedPayload = value;
        }


        private String _expectedResult;

    
        @PropMeta(propId=8)
    
        public String getExpectedResult(){
            return _expectedResult;
        }

        public void setExpectedResult(String value){
            this._expectedResult = value;
        }


        private String _actualResult;

    
        @PropMeta(propId=9)
    
        public String getActualResult(){
            return _actualResult;
        }

        public void setActualResult(String value){
            this._actualResult = value;
        }


        private Boolean _passed;

    
        @PropMeta(propId=10)
    
        public Boolean getPassed(){
            return _passed;
        }

        public void setPassed(Boolean value){
            this._passed = value;
        }


        private String _testedBy;

    
        @PropMeta(propId=11)
    
        public String getTestedBy(){
            return _testedBy;
        }

        public void setTestedBy(String value){
            this._testedBy = value;
        }


        private java.sql.Timestamp _testedAt;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getTestedAt(){
            return _testedAt;
        }

        public void setTestedAt(java.sql.Timestamp value){
            this._testedAt = value;
        }


        private String _notes;

    
        @PropMeta(propId=13)
    
        public String getNotes(){
            return _notes;
        }

        public void setNotes(String value){
            this._notes = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _partnerProfile;

        public Map<String,Object> getPartnerProfile(){
            return _partnerProfile;
        }

        public void setPartnerProfile(Map<String,Object> value){
            this._partnerProfile = value;
        }


    }
