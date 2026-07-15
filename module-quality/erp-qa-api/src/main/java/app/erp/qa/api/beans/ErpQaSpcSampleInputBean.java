//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaSpcSampleInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _chartId;

    
        @PropMeta(propId=2)
    
        public Long getChartId(){
            return _chartId;
        }

        public void setChartId(Long value){
            this._chartId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Integer _subgroupNo;

    
        @PropMeta(propId=4)
    
        public Integer getSubgroupNo(){
            return _subgroupNo;
        }

        public void setSubgroupNo(Integer value){
            this._subgroupNo = value;
        }


        private java.sql.Timestamp _sampleTime;

    
        @PropMeta(propId=5)
    
        public java.sql.Timestamp getSampleTime(){
            return _sampleTime;
        }

        public void setSampleTime(java.sql.Timestamp value){
            this._sampleTime = value;
        }


        private String _measuredValues;

    
        @PropMeta(propId=6)
    
        public String getMeasuredValues(){
            return _measuredValues;
        }

        public void setMeasuredValues(String value){
            this._measuredValues = value;
        }


        private java.math.BigDecimal _mean;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getMean(){
            return _mean;
        }

        public void setMean(java.math.BigDecimal value){
            this._mean = value;
        }


        private java.math.BigDecimal _range;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getRange(){
            return _range;
        }

        public void setRange(java.math.BigDecimal value){
            this._range = value;
        }


        private java.math.BigDecimal _stdDev;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getStdDev(){
            return _stdDev;
        }

        public void setStdDev(java.math.BigDecimal value){
            this._stdDev = value;
        }


        private String _sourceBillType;

    
        @PropMeta(propId=10)
    
        public String getSourceBillType(){
            return _sourceBillType;
        }

        public void setSourceBillType(String value){
            this._sourceBillType = value;
        }


        private String _sourceCode;

    
        @PropMeta(propId=11)
    
        public String getSourceCode(){
            return _sourceCode;
        }

        public void setSourceCode(String value){
            this._sourceCode = value;
        }


        private String _sourceLineCode;

    
        @PropMeta(propId=12)
    
        public String getSourceLineCode(){
            return _sourceLineCode;
        }

        public void setSourceLineCode(String value){
            this._sourceLineCode = value;
        }


        private Long _inspectorId;

    
        @PropMeta(propId=13)
    
        public Long getInspectorId(){
            return _inspectorId;
        }

        public void setInspectorId(Long value){
            this._inspectorId = value;
        }


        private String _violatedRules;

    
        @PropMeta(propId=14)
    
        public String getViolatedRules(){
            return _violatedRules;
        }

        public void setViolatedRules(String value){
            this._violatedRules = value;
        }


        private Boolean _isOutOfControl;

    
        @PropMeta(propId=15)
    
        public Boolean getIsOutOfControl(){
            return _isOutOfControl;
        }

        public void setIsOutOfControl(Boolean value){
            this._isOutOfControl = value;
        }


        private String _remark;

    
        @PropMeta(propId=16)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
