//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvDrpSupplierScoreOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _supplierId;

    
        @PropMeta(propId=3)
    
        public Long getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(Long value){
            this._supplierId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=4)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Integer _sampleCount;

    
        @PropMeta(propId=5)
    
        public Integer getSampleCount(){
            return _sampleCount;
        }

        public void setSampleCount(Integer value){
            this._sampleCount = value;
        }


        private java.math.BigDecimal _avgLeadTime;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getAvgLeadTime(){
            return _avgLeadTime;
        }

        public void setAvgLeadTime(java.math.BigDecimal value){
            this._avgLeadTime = value;
        }


        private java.math.BigDecimal _leadTimeStdDev;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getLeadTimeStdDev(){
            return _leadTimeStdDev;
        }

        public void setLeadTimeStdDev(java.math.BigDecimal value){
            this._leadTimeStdDev = value;
        }


        private java.math.BigDecimal _onTimeRate;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getOnTimeRate(){
            return _onTimeRate;
        }

        public void setOnTimeRate(java.math.BigDecimal value){
            this._onTimeRate = value;
        }


        private java.math.BigDecimal _variationCoefficient;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getVariationCoefficient(){
            return _variationCoefficient;
        }

        public void setVariationCoefficient(java.math.BigDecimal value){
            this._variationCoefficient = value;
        }


        private java.math.BigDecimal _quantityAccuracy;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getQuantityAccuracy(){
            return _quantityAccuracy;
        }

        public void setQuantityAccuracy(java.math.BigDecimal value){
            this._quantityAccuracy = value;
        }


        private java.math.BigDecimal _qualityPassRate;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getQualityPassRate(){
            return _qualityPassRate;
        }

        public void setQualityPassRate(java.math.BigDecimal value){
            this._qualityPassRate = value;
        }


        private java.math.BigDecimal _onTimeScore;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getOnTimeScore(){
            return _onTimeScore;
        }

        public void setOnTimeScore(java.math.BigDecimal value){
            this._onTimeScore = value;
        }


        private java.math.BigDecimal _stabilityScore;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getStabilityScore(){
            return _stabilityScore;
        }

        public void setStabilityScore(java.math.BigDecimal value){
            this._stabilityScore = value;
        }


        private java.math.BigDecimal _quantityScore;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getQuantityScore(){
            return _quantityScore;
        }

        public void setQuantityScore(java.math.BigDecimal value){
            this._quantityScore = value;
        }


        private java.math.BigDecimal _qualityScore;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getQualityScore(){
            return _qualityScore;
        }

        public void setQualityScore(java.math.BigDecimal value){
            this._qualityScore = value;
        }


        private java.math.BigDecimal _totalScore;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getTotalScore(){
            return _totalScore;
        }

        public void setTotalScore(java.math.BigDecimal value){
            this._totalScore = value;
        }


        private String _grade;

    
        @PropMeta(propId=17)
    
        public String getGrade(){
            return _grade;
        }

        public void setGrade(String value){
            this._grade = value;
        }


        private String _grade_label;

    
        public String getGrade_label(){
            return _grade_label;
        }

        public void setGrade_label(String value){
            this._grade_label = value;
        }


        private String _missingDimensions;

    
        @PropMeta(propId=18)
    
        public String getMissingDimensions(){
            return _missingDimensions;
        }

        public void setMissingDimensions(String value){
            this._missingDimensions = value;
        }


        private java.time.LocalDate _windowFrom;

    
        @PropMeta(propId=19)
    
        public java.time.LocalDate getWindowFrom(){
            return _windowFrom;
        }

        public void setWindowFrom(java.time.LocalDate value){
            this._windowFrom = value;
        }


        private java.time.LocalDate _windowTo;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDate getWindowTo(){
            return _windowTo;
        }

        public void setWindowTo(java.time.LocalDate value){
            this._windowTo = value;
        }


        private java.sql.Timestamp _lastCalculatedAt;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getLastCalculatedAt(){
            return _lastCalculatedAt;
        }

        public void setLastCalculatedAt(java.sql.Timestamp value){
            this._lastCalculatedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=22)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=23)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=24)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=25)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=26)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=27)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=28)
    
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


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
