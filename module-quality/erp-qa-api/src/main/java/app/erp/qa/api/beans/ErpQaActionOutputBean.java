//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaActionOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _ncrId;

    
        @PropMeta(propId=2)
    
        public Long getNcrId(){
            return _ncrId;
        }

        public void setNcrId(Long value){
            this._ncrId = value;
        }


        private String _actionType;

    
        @PropMeta(propId=3)
    
        public String getActionType(){
            return _actionType;
        }

        public void setActionType(String value){
            this._actionType = value;
        }


        private String _actionType_label;

    
        public String getActionType_label(){
            return _actionType_label;
        }

        public void setActionType_label(String value){
            this._actionType_label = value;
        }


        private String _description;

    
        @PropMeta(propId=4)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private Long _responsiblePerson;

    
        @PropMeta(propId=5)
    
        public Long getResponsiblePerson(){
            return _responsiblePerson;
        }

        public void setResponsiblePerson(Long value){
            this._responsiblePerson = value;
        }


        private java.time.LocalDate _dueDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getDueDate(){
            return _dueDate;
        }

        public void setDueDate(java.time.LocalDate value){
            this._dueDate = value;
        }


        private String _status;

    
        @PropMeta(propId=7)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private Long _completedBy;

    
        @PropMeta(propId=8)
    
        public Long getCompletedBy(){
            return _completedBy;
        }

        public void setCompletedBy(Long value){
            this._completedBy = value;
        }


        private java.sql.Timestamp _completedAt;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getCompletedAt(){
            return _completedAt;
        }

        public void setCompletedAt(java.sql.Timestamp value){
            this._completedAt = value;
        }


        private Long _verificationPerson;

    
        @PropMeta(propId=10)
    
        public Long getVerificationPerson(){
            return _verificationPerson;
        }

        public void setVerificationPerson(Long value){
            this._verificationPerson = value;
        }


        private java.time.LocalDate _verificationDate;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getVerificationDate(){
            return _verificationDate;
        }

        public void setVerificationDate(java.time.LocalDate value){
            this._verificationDate = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
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


        private Map<String,Object> _ncr;

        public Map<String,Object> getNcr(){
            return _ncr;
        }

        public void setNcr(Map<String,Object> value){
            this._ncr = value;
        }


    }
