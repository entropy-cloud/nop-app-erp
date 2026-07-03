//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgBomOperationInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _bomId;

    
        @PropMeta(propId=2)
    
        public Long getBomId(){
            return _bomId;
        }

        public void setBomId(Long value){
            this._bomId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private Long _operationId;

    
        @PropMeta(propId=4)
    
        public Long getOperationId(){
            return _operationId;
        }

        public void setOperationId(Long value){
            this._operationId = value;
        }


        private Long _workcenterId;

    
        @PropMeta(propId=5)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private java.math.BigDecimal _standardTime;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getStandardTime(){
            return _standardTime;
        }

        public void setStandardTime(java.math.BigDecimal value){
            this._standardTime = value;
        }


        private String _timeUnit;

    
        @PropMeta(propId=7)
    
        public String getTimeUnit(){
            return _timeUnit;
        }

        public void setTimeUnit(String value){
            this._timeUnit = value;
        }


        private java.math.BigDecimal _rate;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getRate(){
            return _rate;
        }

        public void setRate(java.math.BigDecimal value){
            this._rate = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
