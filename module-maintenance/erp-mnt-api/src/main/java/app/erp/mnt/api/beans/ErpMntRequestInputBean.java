//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntRequestInputBean extends CrudInputBase {

    
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


        private Long _equipmentId;

    
        @PropMeta(propId=3)
    
        public Long getEquipmentId(){
            return _equipmentId;
        }

        public void setEquipmentId(Long value){
            this._equipmentId = value;
        }


        private java.time.LocalDate _requestDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getRequestDate(){
            return _requestDate;
        }

        public void setRequestDate(java.time.LocalDate value){
            this._requestDate = value;
        }


        private String _description;

    
        @PropMeta(propId=5)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private Integer _priority;

    
        @PropMeta(propId=6)
    
        public Integer getPriority(){
            return _priority;
        }

        public void setPriority(Integer value){
            this._priority = value;
        }


        private Integer _status;

    
        @PropMeta(propId=7)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
            this._status = value;
        }


        private Long _requestedBy;

    
        @PropMeta(propId=8)
    
        public Long getRequestedBy(){
            return _requestedBy;
        }

        public void setRequestedBy(Long value){
            this._requestedBy = value;
        }


        private Long _assignedTo;

    
        @PropMeta(propId=9)
    
        public Long getAssignedTo(){
            return _assignedTo;
        }

        public void setAssignedTo(Long value){
            this._assignedTo = value;
        }


        private Long _acceptedBy;

    
        @PropMeta(propId=10)
    
        public Long getAcceptedBy(){
            return _acceptedBy;
        }

        public void setAcceptedBy(Long value){
            this._acceptedBy = value;
        }


        private Long _completedBy;

    
        @PropMeta(propId=11)
    
        public Long getCompletedBy(){
            return _completedBy;
        }

        public void setCompletedBy(Long value){
            this._completedBy = value;
        }


        private java.time.LocalDateTime _completedAt;

    
        @PropMeta(propId=12)
    
        public java.time.LocalDateTime getCompletedAt(){
            return _completedAt;
        }

        public void setCompletedAt(java.time.LocalDateTime value){
            this._completedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
