//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaSpcChartOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _chartType;

    
        @PropMeta(propId=5)
    
        public String getChartType(){
            return _chartType;
        }

        public void setChartType(String value){
            this._chartType = value;
        }


        private String _chartType_label;

    
        public String getChartType_label(){
            return _chartType_label;
        }

        public void setChartType_label(String value){
            this._chartType_label = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=6)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _inspectionTypeId;

    
        @PropMeta(propId=7)
    
        public Long getInspectionTypeId(){
            return _inspectionTypeId;
        }

        public void setInspectionTypeId(Long value){
            this._inspectionTypeId = value;
        }


        private Long _parameterId;

    
        @PropMeta(propId=8)
    
        public Long getParameterId(){
            return _parameterId;
        }

        public void setParameterId(Long value){
            this._parameterId = value;
        }


        private java.math.BigDecimal _specMin;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getSpecMin(){
            return _specMin;
        }

        public void setSpecMin(java.math.BigDecimal value){
            this._specMin = value;
        }


        private java.math.BigDecimal _specMax;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getSpecMax(){
            return _specMax;
        }

        public void setSpecMax(java.math.BigDecimal value){
            this._specMax = value;
        }


        private Integer _subgroupSize;

    
        @PropMeta(propId=11)
    
        public Integer getSubgroupSize(){
            return _subgroupSize;
        }

        public void setSubgroupSize(Integer value){
            this._subgroupSize = value;
        }


        private String _samplingFrequency;

    
        @PropMeta(propId=12)
    
        public String getSamplingFrequency(){
            return _samplingFrequency;
        }

        public void setSamplingFrequency(String value){
            this._samplingFrequency = value;
        }


        private String _clCenterType;

    
        @PropMeta(propId=13)
    
        public String getClCenterType(){
            return _clCenterType;
        }

        public void setClCenterType(String value){
            this._clCenterType = value;
        }


        private String _clCenterType_label;

    
        public String getClCenterType_label(){
            return _clCenterType_label;
        }

        public void setClCenterType_label(String value){
            this._clCenterType_label = value;
        }


        private String _ruleSet;

    
        @PropMeta(propId=14)
    
        public String getRuleSet(){
            return _ruleSet;
        }

        public void setRuleSet(String value){
            this._ruleSet = value;
        }


        private Integer _alarmThreshold;

    
        @PropMeta(propId=15)
    
        public Integer getAlarmThreshold(){
            return _alarmThreshold;
        }

        public void setAlarmThreshold(Integer value){
            this._alarmThreshold = value;
        }


        private java.math.BigDecimal _ucl;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getUcl(){
            return _ucl;
        }

        public void setUcl(java.math.BigDecimal value){
            this._ucl = value;
        }


        private java.math.BigDecimal _lcl;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getLcl(){
            return _lcl;
        }

        public void setLcl(java.math.BigDecimal value){
            this._lcl = value;
        }


        private java.math.BigDecimal _cl;

    
        @PropMeta(propId=18)
    
        public java.math.BigDecimal getCl(){
            return _cl;
        }

        public void setCl(java.math.BigDecimal value){
            this._cl = value;
        }


        private String _calcStatus;

    
        @PropMeta(propId=19)
    
        public String getCalcStatus(){
            return _calcStatus;
        }

        public void setCalcStatus(String value){
            this._calcStatus = value;
        }


        private String _calcStatus_label;

    
        public String getCalcStatus_label(){
            return _calcStatus_label;
        }

        public void setCalcStatus_label(String value){
            this._calcStatus_label = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=20)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=21)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=22)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _approveStatus_label;

    
        public String getApproveStatus_label(){
            return _approveStatus_label;
        }

        public void setApproveStatus_label(String value){
            this._approveStatus_label = value;
        }


        private String _remark;

    
        @PropMeta(propId=23)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=24)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=25)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=26)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=27)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=28)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=29)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _orgName;

    
        public String getOrgName(){
            return _orgName;
        }

        public void setOrgName(String value){
            this._orgName = value;
        }


        private String _materialName;

    
        public String getMaterialName(){
            return _materialName;
        }

        public void setMaterialName(String value){
            this._materialName = value;
        }


        private String _inspectionTemplateCode;

    
        public String getInspectionTemplateCode(){
            return _inspectionTemplateCode;
        }

        public void setInspectionTemplateCode(String value){
            this._inspectionTemplateCode = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _inspectionType;

        public Map<String,Object> getInspectionType(){
            return _inspectionType;
        }

        public void setInspectionType(Map<String,Object> value){
            this._inspectionType = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private List<Map<String,Object>> _samples;

        public List<Map<String,Object>> getSamples(){
            return _samples;
        }

        public void setSamples(List<Map<String,Object>> value){
            this._samples = value;
        }


    }
