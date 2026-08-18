//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsTicketFulfillmentStepOutputBean {

    
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


        private String _actionType_label;

    
        public String getActionType_label(){
            return _actionType_label;
        }

        public void setActionType_label(String value){
            this._actionType_label = value;
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


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=16)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=17)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=19)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=20)
    
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


        private Map<String,Object> _fulfillment;

        public Map<String,Object> getFulfillment(){
            return _fulfillment;
        }

        public void setFulfillment(Map<String,Object> value){
            this._fulfillment = value;
        }


        private Map<String,Object> _catalogItem;

        public Map<String,Object> getCatalogItem(){
            return _catalogItem;
        }

        public void setCatalogItem(Map<String,Object> value){
            this._catalogItem = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
