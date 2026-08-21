//__XGEN_FORCE_OVERRIDE__
    package app.erp.aps.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpApsOperationOrderOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _workOrderId;

    
        @PropMeta(propId=3)
    
        public String getWorkOrderId(){
            return _workOrderId;
        }

        public void setWorkOrderId(String value){
            this._workOrderId = value;
        }


        private String _operationName;

    
        @PropMeta(propId=4)
    
        public String getOperationName(){
            return _operationName;
        }

        public void setOperationName(String value){
            this._operationName = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=5)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
        }


        private String _machineId;

    
        @PropMeta(propId=6)
    
        public String getMachineId(){
            return _machineId;
        }

        public void setMachineId(String value){
            this._machineId = value;
        }


        private Integer _priority;

    
        @PropMeta(propId=7)
    
        public Integer getPriority(){
            return _priority;
        }

        public void setPriority(Integer value){
            this._priority = value;
        }


        private java.sql.Timestamp _plannedStartDateT;

    
        @PropMeta(propId=8)
    
        public java.sql.Timestamp getPlannedStartDateT(){
            return _plannedStartDateT;
        }

        public void setPlannedStartDateT(java.sql.Timestamp value){
            this._plannedStartDateT = value;
        }


        private java.sql.Timestamp _plannedEndDateT;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getPlannedEndDateT(){
            return _plannedEndDateT;
        }

        public void setPlannedEndDateT(java.sql.Timestamp value){
            this._plannedEndDateT = value;
        }


        private java.sql.Timestamp _realStartDateT;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getRealStartDateT(){
            return _realStartDateT;
        }

        public void setRealStartDateT(java.sql.Timestamp value){
            this._realStartDateT = value;
        }


        private java.sql.Timestamp _realEndDateT;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getRealEndDateT(){
            return _realEndDateT;
        }

        public void setRealEndDateT(java.sql.Timestamp value){
            this._realEndDateT = value;
        }


        private java.math.BigDecimal _setupTime;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getSetupTime(){
            return _setupTime;
        }

        public void setSetupTime(java.math.BigDecimal value){
            this._setupTime = value;
        }


        private java.math.BigDecimal _runtimePerUnit;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getRuntimePerUnit(){
            return _runtimePerUnit;
        }

        public void setRuntimePerUnit(java.math.BigDecimal value){
            this._runtimePerUnit = value;
        }


        private java.math.BigDecimal _qty;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getQty(){
            return _qty;
        }

        public void setQty(java.math.BigDecimal value){
            this._qty = value;
        }


        private java.math.BigDecimal _totalDuration;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getTotalDuration(){
            return _totalDuration;
        }

        public void setTotalDuration(java.math.BigDecimal value){
            this._totalDuration = value;
        }


        private String _assignedToId;

    
        @PropMeta(propId=16)
    
        public String getAssignedToId(){
            return _assignedToId;
        }

        public void setAssignedToId(String value){
            this._assignedToId = value;
        }


        private Boolean _isOutsourced;

    
        @PropMeta(propId=17)
    
        public Boolean getIsOutsourced(){
            return _isOutsourced;
        }

        public void setIsOutsourced(Boolean value){
            this._isOutsourced = value;
        }


        private String _status;

    
        @PropMeta(propId=18)
    
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


        private String _orgId;

    
        @PropMeta(propId=19)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=21)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=22)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=23)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=25)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=26)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private java.sql.Timestamp _earliestStartDateT;

    
        @PropMeta(propId=27)
    
        public java.sql.Timestamp getEarliestStartDateT(){
            return _earliestStartDateT;
        }

        public void setEarliestStartDateT(java.sql.Timestamp value){
            this._earliestStartDateT = value;
        }


        private java.sql.Timestamp _latestEndDateT;

    
        @PropMeta(propId=28)
    
        public java.sql.Timestamp getLatestEndDateT(){
            return _latestEndDateT;
        }

        public void setLatestEndDateT(java.sql.Timestamp value){
            this._latestEndDateT = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=29)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _selectedRoutingId;

    
        @PropMeta(propId=30)
    
        public String getSelectedRoutingId(){
            return _selectedRoutingId;
        }

        public void setSelectedRoutingId(String value){
            this._selectedRoutingId = value;
        }


        private String _routingSelectionReason;

    
        @PropMeta(propId=31)
    
        public String getRoutingSelectionReason(){
            return _routingSelectionReason;
        }

        public void setRoutingSelectionReason(String value){
            this._routingSelectionReason = value;
        }


        private String _routingSelectionReason_label;

    
        public String getRoutingSelectionReason_label(){
            return _routingSelectionReason_label;
        }

        public void setRoutingSelectionReason_label(String value){
            this._routingSelectionReason_label = value;
        }


        private Boolean _manualOverride;

    
        @PropMeta(propId=32)
    
        public Boolean getManualOverride(){
            return _manualOverride;
        }

        public void setManualOverride(Boolean value){
            this._manualOverride = value;
        }


        private Boolean _allowFallback;

    
        @PropMeta(propId=33)
    
        public Boolean getAllowFallback(){
            return _allowFallback;
        }

        public void setAllowFallback(Boolean value){
            this._allowFallback = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
