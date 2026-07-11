//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurSupplierScorecardInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=2)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
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


        private java.math.BigDecimal _totalScore;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getTotalScore(){
            return _totalScore;
        }

        public void setTotalScore(java.math.BigDecimal value){
            this._totalScore = value;
        }


        private String _standing;

    
        @PropMeta(propId=7)
    
        public String getStanding(){
            return _standing;
        }

        public void setStanding(String value){
            this._standing = value;
        }


        private java.math.BigDecimal _warnThreshold;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getWarnThreshold(){
            return _warnThreshold;
        }

        public void setWarnThreshold(java.math.BigDecimal value){
            this._warnThreshold = value;
        }


        private java.math.BigDecimal _holdThreshold;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getHoldThreshold(){
            return _holdThreshold;
        }

        public void setHoldThreshold(java.math.BigDecimal value){
            this._holdThreshold = value;
        }


        private java.math.BigDecimal _preventThreshold;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getPreventThreshold(){
            return _preventThreshold;
        }

        public void setPreventThreshold(java.math.BigDecimal value){
            this._preventThreshold = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpPurSupplierScorecardCriteriaInputBean> _criterias;

        public List<ErpPurSupplierScorecardCriteriaInputBean> getCriterias(){
            return _criterias;
        }

        public void setCriterias(List<ErpPurSupplierScorecardCriteriaInputBean> value){
            this._criterias = value;
        }


    }
