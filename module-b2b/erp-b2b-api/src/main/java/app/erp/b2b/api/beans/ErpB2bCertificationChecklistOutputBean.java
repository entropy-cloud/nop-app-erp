//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bCertificationChecklistOutputBean {

    
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


        private String _checklistItem;

    
        @PropMeta(propId=3)
    
        public String getChecklistItem(){
            return _checklistItem;
        }

        public void setChecklistItem(String value){
            this._checklistItem = value;
        }


        private String _requiredDocType;

    
        @PropMeta(propId=4)
    
        public String getRequiredDocType(){
            return _requiredDocType;
        }

        public void setRequiredDocType(String value){
            this._requiredDocType = value;
        }


        private Boolean _isMandatory;

    
        @PropMeta(propId=5)
    
        public Boolean getIsMandatory(){
            return _isMandatory;
        }

        public void setIsMandatory(Boolean value){
            this._isMandatory = value;
        }


        private Boolean _isPassed;

    
        @PropMeta(propId=6)
    
        public Boolean getIsPassed(){
            return _isPassed;
        }

        public void setIsPassed(Boolean value){
            this._isPassed = value;
        }


        private String _checkedBy;

    
        @PropMeta(propId=7)
    
        public String getCheckedBy(){
            return _checkedBy;
        }

        public void setCheckedBy(String value){
            this._checkedBy = value;
        }


        private java.sql.Timestamp _checkedAt;

    
        @PropMeta(propId=8)
    
        public java.sql.Timestamp getCheckedAt(){
            return _checkedAt;
        }

        public void setCheckedAt(java.sql.Timestamp value){
            this._checkedAt = value;
        }


        private String _evidence;

    
        @PropMeta(propId=9)
    
        public String getEvidence(){
            return _evidence;
        }

        public void setEvidence(String value){
            this._evidence = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
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


        private Map<String,Object> _partnerProfile;

        public Map<String,Object> getPartnerProfile(){
            return _partnerProfile;
        }

        public void setPartnerProfile(Map<String,Object> value){
            this._partnerProfile = value;
        }


    }
