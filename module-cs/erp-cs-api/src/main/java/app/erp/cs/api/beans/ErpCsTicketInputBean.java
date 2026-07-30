//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsTicketInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _subject;

    
        @PropMeta(propId=4)
    
        public String getSubject(){
            return _subject;
        }

        public void setSubject(String value){
            this._subject = value;
        }


        private String _description;

    
        @PropMeta(propId=5)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private Long _customerId;

    
        @PropMeta(propId=6)
    
        public Long getCustomerId(){
            return _customerId;
        }

        public void setCustomerId(Long value){
            this._customerId = value;
        }


        private Long _contactId;

    
        @PropMeta(propId=7)
    
        public Long getContactId(){
            return _contactId;
        }

        public void setContactId(Long value){
            this._contactId = value;
        }


        private Long _ticketTypeId;

    
        @PropMeta(propId=8)
    
        public Long getTicketTypeId(){
            return _ticketTypeId;
        }

        public void setTicketTypeId(Long value){
            this._ticketTypeId = value;
        }


        private String _priority;

    
        @PropMeta(propId=9)
    
        public String getPriority(){
            return _priority;
        }

        public void setPriority(String value){
            this._priority = value;
        }


        private String _source;

    
        @PropMeta(propId=10)
    
        public String getSource(){
            return _source;
        }

        public void setSource(String value){
            this._source = value;
        }


        private String _assignedToId;

    
        @PropMeta(propId=11)
    
        public String getAssignedToId(){
            return _assignedToId;
        }

        public void setAssignedToId(String value){
            this._assignedToId = value;
        }


        private Long _slaPolicyId;

    
        @PropMeta(propId=12)
    
        public Long getSlaPolicyId(){
            return _slaPolicyId;
        }

        public void setSlaPolicyId(Long value){
            this._slaPolicyId = value;
        }


        private java.sql.Timestamp _deadlineDateTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getDeadlineDateTime(){
            return _deadlineDateTime;
        }

        public void setDeadlineDateTime(java.sql.Timestamp value){
            this._deadlineDateTime = value;
        }


        private Boolean _isSlaCompleted;

    
        @PropMeta(propId=14)
    
        public Boolean getIsSlaCompleted(){
            return _isSlaCompleted;
        }

        public void setIsSlaCompleted(Boolean value){
            this._isSlaCompleted = value;
        }


        private java.sql.Timestamp _startDateTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getStartDateTime(){
            return _startDateTime;
        }

        public void setStartDateTime(java.sql.Timestamp value){
            this._startDateTime = value;
        }


        private java.sql.Timestamp _endDateTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getEndDateTime(){
            return _endDateTime;
        }

        public void setEndDateTime(java.sql.Timestamp value){
            this._endDateTime = value;
        }


        private Integer _duration;

    
        @PropMeta(propId=17)
    
        public Integer getDuration(){
            return _duration;
        }

        public void setDuration(Integer value){
            this._duration = value;
        }


        private Integer _progress;

    
        @PropMeta(propId=18)
    
        public Integer getProgress(){
            return _progress;
        }

        public void setProgress(Integer value){
            this._progress = value;
        }


        private String _status;

    
        @PropMeta(propId=19)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=20)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=21)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=22)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _catalogItemId;

    
        @PropMeta(propId=23)
    
        public Long getCatalogItemId(){
            return _catalogItemId;
        }

        public void setCatalogItemId(Long value){
            this._catalogItemId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=202)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private List<ErpCsTicketActionInputBean> _actions;

        public List<ErpCsTicketActionInputBean> getActions(){
            return _actions;
        }

        public void setActions(List<ErpCsTicketActionInputBean> value){
            this._actions = value;
        }


    }
