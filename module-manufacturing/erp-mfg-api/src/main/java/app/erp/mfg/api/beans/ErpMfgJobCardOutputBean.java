//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgJobCardOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _workOrderId;

    
        @PropMeta(propId=2)
    
        public Long getWorkOrderId(){
            return _workOrderId;
        }

        public void setWorkOrderId(Long value){
            this._workOrderId = value;
        }


        private Long _operationId;

    
        @PropMeta(propId=3)
    
        public Long getOperationId(){
            return _operationId;
        }

        public void setOperationId(Long value){
            this._operationId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=4)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private java.math.BigDecimal _plannedQuantity;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getPlannedQuantity(){
            return _plannedQuantity;
        }

        public void setPlannedQuantity(java.math.BigDecimal value){
            this._plannedQuantity = value;
        }


        private java.math.BigDecimal _completedQuantity;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getCompletedQuantity(){
            return _completedQuantity;
        }

        public void setCompletedQuantity(java.math.BigDecimal value){
            this._completedQuantity = value;
        }


        private java.math.BigDecimal _scrappedQuantity;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getScrappedQuantity(){
            return _scrappedQuantity;
        }

        public void setScrappedQuantity(java.math.BigDecimal value){
            this._scrappedQuantity = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
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


        private Long _workcenterId;

    
        @PropMeta(propId=9)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private java.sql.Timestamp _actualStartTime;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getActualStartTime(){
            return _actualStartTime;
        }

        public void setActualStartTime(java.sql.Timestamp value){
            this._actualStartTime = value;
        }


        private java.sql.Timestamp _actualEndTime;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getActualEndTime(){
            return _actualEndTime;
        }

        public void setActualEndTime(java.sql.Timestamp value){
            this._actualEndTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _code;

    
        @PropMeta(propId=13)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Long _sourceScheduleId;

    
        @PropMeta(propId=20)
    
        public Long getSourceScheduleId(){
            return _sourceScheduleId;
        }

        public void setSourceScheduleId(Long value){
            this._sourceScheduleId = value;
        }


        private Map<String,Object> _workOrder;

        public Map<String,Object> getWorkOrder(){
            return _workOrder;
        }

        public void setWorkOrder(Map<String,Object> value){
            this._workOrder = value;
        }


        private Map<String,Object> _workcenter;

        public Map<String,Object> getWorkcenter(){
            return _workcenter;
        }

        public void setWorkcenter(Map<String,Object> value){
            this._workcenter = value;
        }


        private Map<String,Object> _operation;

        public Map<String,Object> getOperation(){
            return _operation;
        }

        public void setOperation(Map<String,Object> value){
            this._operation = value;
        }


    }
