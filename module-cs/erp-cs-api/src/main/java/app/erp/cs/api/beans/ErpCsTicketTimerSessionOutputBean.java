//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsTicketTimerSessionOutputBean {

    
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


        private String _agentId;

    
        @PropMeta(propId=3)
    
        public String getAgentId(){
            return _agentId;
        }

        public void setAgentId(String value){
            this._agentId = value;
        }


        private String _ticketId;

    
        @PropMeta(propId=4)
    
        public String getTicketId(){
            return _ticketId;
        }

        public void setTicketId(String value){
            this._ticketId = value;
        }


        private java.sql.Timestamp _startTime;

    
        @PropMeta(propId=5)
    
        public java.sql.Timestamp getStartTime(){
            return _startTime;
        }

        public void setStartTime(java.sql.Timestamp value){
            this._startTime = value;
        }


        private java.sql.Timestamp _stopTime;

    
        @PropMeta(propId=6)
    
        public java.sql.Timestamp getStopTime(){
            return _stopTime;
        }

        public void setStopTime(java.sql.Timestamp value){
            this._stopTime = value;
        }


        private java.sql.Timestamp _pauseStartDateTime;

    
        @PropMeta(propId=7)
    
        public java.sql.Timestamp getPauseStartDateTime(){
            return _pauseStartDateTime;
        }

        public void setPauseStartDateTime(java.sql.Timestamp value){
            this._pauseStartDateTime = value;
        }


        private Integer _cumulativePauseMinutes;

    
        @PropMeta(propId=8)
    
        public Integer getCumulativePauseMinutes(){
            return _cumulativePauseMinutes;
        }

        public void setCumulativePauseMinutes(Integer value){
            this._cumulativePauseMinutes = value;
        }


        private String _pauseReason;

    
        @PropMeta(propId=9)
    
        public String getPauseReason(){
            return _pauseReason;
        }

        public void setPauseReason(String value){
            this._pauseReason = value;
        }


        private String _status;

    
        @PropMeta(propId=10)
    
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


        private String _activeFlag;

    
        @PropMeta(propId=11)
    
        public String getActiveFlag(){
            return _activeFlag;
        }

        public void setActiveFlag(String value){
            this._activeFlag = value;
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
