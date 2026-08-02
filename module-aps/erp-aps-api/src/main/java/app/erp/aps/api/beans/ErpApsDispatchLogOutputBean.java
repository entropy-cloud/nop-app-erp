//__XGEN_FORCE_OVERRIDE__
    package app.erp.aps.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpApsDispatchLogOutputBean {

    
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


        private Long _operationOrderId;

    
        @PropMeta(propId=3)
    
        public Long getOperationOrderId(){
            return _operationOrderId;
        }

        public void setOperationOrderId(Long value){
            this._operationOrderId = value;
        }


        private Long _workcenterId;

    
        @PropMeta(propId=4)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private String _dispatchType;

    
        @PropMeta(propId=5)
    
        public String getDispatchType(){
            return _dispatchType;
        }

        public void setDispatchType(String value){
            this._dispatchType = value;
        }


        private String _dispatchType_label;

    
        public String getDispatchType_label(){
            return _dispatchType_label;
        }

        public void setDispatchType_label(String value){
            this._dispatchType_label = value;
        }


        private String _previousStatus;

    
        @PropMeta(propId=6)
    
        public String getPreviousStatus(){
            return _previousStatus;
        }

        public void setPreviousStatus(String value){
            this._previousStatus = value;
        }


        private String _newStatus;

    
        @PropMeta(propId=7)
    
        public String getNewStatus(){
            return _newStatus;
        }

        public void setNewStatus(String value){
            this._newStatus = value;
        }


        private String _conditionCheckResult;

    
        @PropMeta(propId=8)
    
        public String getConditionCheckResult(){
            return _conditionCheckResult;
        }

        public void setConditionCheckResult(String value){
            this._conditionCheckResult = value;
        }


        private String _dispatchedBy;

    
        @PropMeta(propId=9)
    
        public String getDispatchedBy(){
            return _dispatchedBy;
        }

        public void setDispatchedBy(String value){
            this._dispatchedBy = value;
        }


        private java.sql.Timestamp _dispatchedAt;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getDispatchedAt(){
            return _dispatchedAt;
        }

        public void setDispatchedAt(java.sql.Timestamp value){
            this._dispatchedAt = value;
        }


        private Boolean _materialAvailable;

    
        @PropMeta(propId=11)
    
        public Boolean getMaterialAvailable(){
            return _materialAvailable;
        }

        public void setMaterialAvailable(Boolean value){
            this._materialAvailable = value;
        }


        private Boolean _operatorAvailable;

    
        @PropMeta(propId=12)
    
        public Boolean getOperatorAvailable(){
            return _operatorAvailable;
        }

        public void setOperatorAvailable(Boolean value){
            this._operatorAvailable = value;
        }


        private Boolean _toolingAvailable;

    
        @PropMeta(propId=13)
    
        public Boolean getToolingAvailable(){
            return _toolingAvailable;
        }

        public void setToolingAvailable(Boolean value){
            this._toolingAvailable = value;
        }


        private String _note;

    
        @PropMeta(propId=14)
    
        public String getNote(){
            return _note;
        }

        public void setNote(String value){
            this._note = value;
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


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _operationOrder;

        public Map<String,Object> getOperationOrder(){
            return _operationOrder;
        }

        public void setOperationOrder(Map<String,Object> value){
            this._operationOrder = value;
        }


    }
