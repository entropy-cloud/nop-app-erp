//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsTicketFulfillmentStepInputBean extends CrudInputBase {

    
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


        private Long _fulfillmentId;

    
        @PropMeta(propId=4)
    
        public Long getFulfillmentId(){
            return _fulfillmentId;
        }

        public void setFulfillmentId(Long value){
            this._fulfillmentId = value;
        }


        private Long _catalogItemId;

    
        @PropMeta(propId=5)
    
        public Long getCatalogItemId(){
            return _catalogItemId;
        }

        public void setCatalogItemId(Long value){
            this._catalogItemId = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=6)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
        }


        private String _actionType;

    
        @PropMeta(propId=7)
    
        public String getActionType(){
            return _actionType;
        }

        public void setActionType(String value){
            this._actionType = value;
        }


        private String _actionConfig;

    
        @PropMeta(propId=8)
    
        public String getActionConfig(){
            return _actionConfig;
        }

        public void setActionConfig(String value){
            this._actionConfig = value;
        }


        private String _status;

    
        @PropMeta(propId=9)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Integer _retryCount;

    
        @PropMeta(propId=10)
    
        public Integer getRetryCount(){
            return _retryCount;
        }

        public void setRetryCount(Integer value){
            this._retryCount = value;
        }


        private String _lastError;

    
        @PropMeta(propId=11)
    
        public String getLastError(){
            return _lastError;
        }

        public void setLastError(String value){
            this._lastError = value;
        }


        private java.sql.Timestamp _executedAt;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getExecutedAt(){
            return _executedAt;
        }

        public void setExecutedAt(java.sql.Timestamp value){
            this._executedAt = value;
        }


        private String _executedBy;

    
        @PropMeta(propId=13)
    
        public String getExecutedBy(){
            return _executedBy;
        }

        public void setExecutedBy(String value){
            this._executedBy = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
