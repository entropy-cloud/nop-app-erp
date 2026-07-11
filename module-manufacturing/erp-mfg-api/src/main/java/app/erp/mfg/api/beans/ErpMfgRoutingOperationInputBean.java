//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgRoutingOperationInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _routingId;

    
        @PropMeta(propId=2)
    
        public Long getRoutingId(){
            return _routingId;
        }

        public void setRoutingId(Long value){
            this._routingId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _operationCode;

    
        @PropMeta(propId=4)
    
        public String getOperationCode(){
            return _operationCode;
        }

        public void setOperationCode(String value){
            this._operationCode = value;
        }


        private String _operationName;

    
        @PropMeta(propId=5)
    
        public String getOperationName(){
            return _operationName;
        }

        public void setOperationName(String value){
            this._operationName = value;
        }


        private Long _workcenterId;

    
        @PropMeta(propId=6)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private java.math.BigDecimal _standardTime;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getStandardTime(){
            return _standardTime;
        }

        public void setStandardTime(java.math.BigDecimal value){
            this._standardTime = value;
        }


        private String _timeUnit;

    
        @PropMeta(propId=8)
    
        public String getTimeUnit(){
            return _timeUnit;
        }

        public void setTimeUnit(String value){
            this._timeUnit = value;
        }


        private java.math.BigDecimal _setupTime;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getSetupTime(){
            return _setupTime;
        }

        public void setSetupTime(java.math.BigDecimal value){
            this._setupTime = value;
        }


        private java.math.BigDecimal _runTime;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getRunTime(){
            return _runTime;
        }

        public void setRunTime(java.math.BigDecimal value){
            this._runTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
