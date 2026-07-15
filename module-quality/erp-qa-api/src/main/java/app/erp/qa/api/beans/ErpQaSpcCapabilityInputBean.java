//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaSpcCapabilityInputBean extends CrudInputBase {

    
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


        private java.time.LocalDate _periodFrom;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getPeriodFrom(){
            return _periodFrom;
        }

        public void setPeriodFrom(java.time.LocalDate value){
            this._periodFrom = value;
        }


        private java.time.LocalDate _periodTo;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getPeriodTo(){
            return _periodTo;
        }

        public void setPeriodTo(java.time.LocalDate value){
            this._periodTo = value;
        }


        private Integer _sampleCount;

    
        @PropMeta(propId=6)
    
        public Integer getSampleCount(){
            return _sampleCount;
        }

        public void setSampleCount(Integer value){
            this._sampleCount = value;
        }


        private Integer _totalObservations;

    
        @PropMeta(propId=7)
    
        public Integer getTotalObservations(){
            return _totalObservations;
        }

        public void setTotalObservations(Integer value){
            this._totalObservations = value;
        }


        private java.math.BigDecimal _grandMean;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getGrandMean(){
            return _grandMean;
        }

        public void setGrandMean(java.math.BigDecimal value){
            this._grandMean = value;
        }


        private java.math.BigDecimal _overallStdDev;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getOverallStdDev(){
            return _overallStdDev;
        }

        public void setOverallStdDev(java.math.BigDecimal value){
            this._overallStdDev = value;
        }


        private java.math.BigDecimal _withinStdDev;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getWithinStdDev(){
            return _withinStdDev;
        }

        public void setWithinStdDev(java.math.BigDecimal value){
            this._withinStdDev = value;
        }


        private java.math.BigDecimal _cp;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getCp(){
            return _cp;
        }

        public void setCp(java.math.BigDecimal value){
            this._cp = value;
        }


        private java.math.BigDecimal _cpk;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getCpk(){
            return _cpk;
        }

        public void setCpk(java.math.BigDecimal value){
            this._cpk = value;
        }


        private java.math.BigDecimal _pp;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getPp(){
            return _pp;
        }

        public void setPp(java.math.BigDecimal value){
            this._pp = value;
        }


        private java.math.BigDecimal _ppk;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getPpk(){
            return _ppk;
        }

        public void setPpk(java.math.BigDecimal value){
            this._ppk = value;
        }


        private java.math.BigDecimal _cpm;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getCpm(){
            return _cpm;
        }

        public void setCpm(java.math.BigDecimal value){
            this._cpm = value;
        }


        private String _capabilityLevel;

    
        @PropMeta(propId=16)
    
        public String getCapabilityLevel(){
            return _capabilityLevel;
        }

        public void setCapabilityLevel(String value){
            this._capabilityLevel = value;
        }


        private Boolean _isStable;

    
        @PropMeta(propId=17)
    
        public Boolean getIsStable(){
            return _isStable;
        }

        public void setIsStable(Boolean value){
            this._isStable = value;
        }


        private String _calculatedBy;

    
        @PropMeta(propId=18)
    
        public String getCalculatedBy(){
            return _calculatedBy;
        }

        public void setCalculatedBy(String value){
            this._calculatedBy = value;
        }


        private java.sql.Timestamp _calculatedAt;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCalculatedAt(){
            return _calculatedAt;
        }

        public void setCalculatedAt(java.sql.Timestamp value){
            this._calculatedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
