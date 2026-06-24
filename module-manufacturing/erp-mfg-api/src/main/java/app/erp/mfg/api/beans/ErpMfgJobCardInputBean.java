//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgJobCardInputBean extends CrudInputBase {

    
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


        private String _plannedQuantity;

    
        @PropMeta(propId=5)
    
        public String getPlannedQuantity(){
            return _plannedQuantity;
        }

        public void setPlannedQuantity(String value){
            this._plannedQuantity = value;
        }


        private String _completedQuantity;

    
        @PropMeta(propId=6)
    
        public String getCompletedQuantity(){
            return _completedQuantity;
        }

        public void setCompletedQuantity(String value){
            this._completedQuantity = value;
        }


        private String _scrappedQuantity;

    
        @PropMeta(propId=7)
    
        public String getScrappedQuantity(){
            return _scrappedQuantity;
        }

        public void setScrappedQuantity(String value){
            this._scrappedQuantity = value;
        }


        private Integer _status;

    
        @PropMeta(propId=8)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
            this._status = value;
        }


        private Long _workcenterId;

    
        @PropMeta(propId=9)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private java.time.LocalDateTime _actualStartTime;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDateTime getActualStartTime(){
            return _actualStartTime;
        }

        public void setActualStartTime(java.time.LocalDateTime value){
            this._actualStartTime = value;
        }


        private java.time.LocalDateTime _actualEndTime;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDateTime getActualEndTime(){
            return _actualEndTime;
        }

        public void setActualEndTime(java.time.LocalDateTime value){
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


    }
