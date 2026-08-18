//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsTicketTimerSessionInputBean extends CrudInputBase {

    
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


        private String _agentId;

    
        @PropMeta(propId=3)
    
        public String getAgentId(){
            return _agentId;
        }

        public void setAgentId(String value){
            this._agentId = value;
        }


        private Long _ticketId;

    
        @PropMeta(propId=4)
    
        public Long getTicketId(){
            return _ticketId;
        }

        public void setTicketId(Long value){
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


        private String _activeFlag;

    
        @PropMeta(propId=11)
    
        public String getActiveFlag(){
            return _activeFlag;
        }

        public void setActiveFlag(String value){
            this._activeFlag = value;
        }


    }
