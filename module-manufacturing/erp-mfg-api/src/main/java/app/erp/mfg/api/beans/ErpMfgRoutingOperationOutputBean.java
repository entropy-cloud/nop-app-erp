//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgRoutingOperationOutputBean {

    
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


        private String _routingCode;

    
        public String getRoutingCode(){
            return _routingCode;
        }

        public void setRoutingCode(String value){
            this._routingCode = value;
        }


        private String _workcenterName;

    
        public String getWorkcenterName(){
            return _workcenterName;
        }

        public void setWorkcenterName(String value){
            this._workcenterName = value;
        }


        private Map<String,Object> _routing;

        public Map<String,Object> getRouting(){
            return _routing;
        }

        public void setRouting(Map<String,Object> value){
            this._routing = value;
        }


        private Map<String,Object> _workcenter;

        public Map<String,Object> getWorkcenter(){
            return _workcenter;
        }

        public void setWorkcenter(Map<String,Object> value){
            this._workcenter = value;
        }


    }
