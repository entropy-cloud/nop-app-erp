//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bPartnerCredentialOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _partnerProfileId;

    
        @PropMeta(propId=2)
    
        public String getPartnerProfileId(){
            return _partnerProfileId;
        }

        public void setPartnerProfileId(String value){
            this._partnerProfileId = value;
        }


        private String _credentialType;

    
        @PropMeta(propId=3)
    
        public String getCredentialType(){
            return _credentialType;
        }

        public void setCredentialType(String value){
            this._credentialType = value;
        }


        private String _credentialType_label;

    
        public String getCredentialType_label(){
            return _credentialType_label;
        }

        public void setCredentialType_label(String value){
            this._credentialType_label = value;
        }


        private String _credentialKey;

    
        @PropMeta(propId=4)
    
        public String getCredentialKey(){
            return _credentialKey;
        }

        public void setCredentialKey(String value){
            this._credentialKey = value;
        }


        private String _credentialValue;

    
        @PropMeta(propId=5)
    
        public String getCredentialValue(){
            return _credentialValue;
        }

        public void setCredentialValue(String value){
            this._credentialValue = value;
        }


        private java.time.LocalDate _issuedAt;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getIssuedAt(){
            return _issuedAt;
        }

        public void setIssuedAt(java.time.LocalDate value){
            this._issuedAt = value;
        }


        private java.time.LocalDate _expiresAt;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getExpiresAt(){
            return _expiresAt;
        }

        public void setExpiresAt(java.time.LocalDate value){
            this._expiresAt = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=8)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=11)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=12)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=14)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=15)
    
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
