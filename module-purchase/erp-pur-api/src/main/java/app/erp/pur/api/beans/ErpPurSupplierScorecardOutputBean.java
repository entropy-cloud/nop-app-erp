//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurSupplierScorecardOutputBean {

    
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


        private Integer _standing;

    
        @PropMeta(propId=7)
    
        public Integer getStanding(){
            return _standing;
        }

        public void setStanding(Integer value){
            this._standing = value;
        }


        private String _standing_label;

    
        public String getStanding_label(){
            return _standing_label;
        }

        public void setStanding_label(String value){
            this._standing_label = value;
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


        private Integer _status;

    
        @PropMeta(propId=11)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _supplier;

        public Map<String,Object> getSupplier(){
            return _supplier;
        }

        public void setSupplier(Map<String,Object> value){
            this._supplier = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private List<Map<String,Object>> _criterias;

        public List<Map<String,Object>> getCriterias(){
            return _criterias;
        }

        public void setCriterias(List<Map<String,Object>> value){
            this._criterias = value;
        }


    }
