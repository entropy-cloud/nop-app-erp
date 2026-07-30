//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bCertificationChecklistInputBean extends CrudInputBase {

    
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


    }
