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
    public class ErpQaNonConformanceOutputBean {

    
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


        private java.time.LocalDate _ncrDate;

    
        @PropMeta(propId=3)
    
        public java.time.LocalDate getNcrDate(){
            return _ncrDate;
        }

        public void setNcrDate(java.time.LocalDate value){
            this._ncrDate = value;
        }


        private String _sourceType;

    
        @PropMeta(propId=4)
    
        public String getSourceType(){
            return _sourceType;
        }

        public void setSourceType(String value){
            this._sourceType = value;
        }


        private String _sourceCode;

    
        @PropMeta(propId=5)
    
        public String getSourceCode(){
            return _sourceCode;
        }

        public void setSourceCode(String value){
            this._sourceCode = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=6)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _inspectionId;

    
        @PropMeta(propId=7)
    
        public Long getInspectionId(){
            return _inspectionId;
        }

        public void setInspectionId(Long value){
            this._inspectionId = value;
        }


        private java.math.BigDecimal _quantity;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getQuantity(){
            return _quantity;
        }

        public void setQuantity(java.math.BigDecimal value){
            this._quantity = value;
        }


        private String _description;

    
        @PropMeta(propId=9)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _severity;

    
        @PropMeta(propId=10)
    
        public String getSeverity(){
            return _severity;
        }

        public void setSeverity(String value){
            this._severity = value;
        }


        private String _severity_label;

    
        public String getSeverity_label(){
            return _severity_label;
        }

        public void setSeverity_label(String value){
            this._severity_label = value;
        }


        private String _dispositionType;

    
        @PropMeta(propId=11)
    
        public String getDispositionType(){
            return _dispositionType;
        }

        public void setDispositionType(String value){
            this._dispositionType = value;
        }


        private String _dispositionType_label;

    
        public String getDispositionType_label(){
            return _dispositionType_label;
        }

        public void setDispositionType_label(String value){
            this._dispositionType_label = value;
        }


        private String _status;

    
        @PropMeta(propId=12)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private Long _supplierId;

    
        @PropMeta(propId=13)
    
        public Long getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(Long value){
            this._supplierId = value;
        }


        private String _parameterName;

    
        @PropMeta(propId=14)
    
        public String getParameterName(){
            return _parameterName;
        }

        public void setParameterName(String value){
            this._parameterName = value;
        }


        private String _measuredValue;

    
        @PropMeta(propId=15)
    
        public String getMeasuredValue(){
            return _measuredValue;
        }

        public void setMeasuredValue(String value){
            this._measuredValue = value;
        }


        private java.math.BigDecimal _specMin;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getSpecMin(){
            return _specMin;
        }

        public void setSpecMin(java.math.BigDecimal value){
            this._specMin = value;
        }


        private java.math.BigDecimal _specMax;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getSpecMax(){
            return _specMax;
        }

        public void setSpecMax(java.math.BigDecimal value){
            this._specMax = value;
        }


        private Long _assignedTo;

    
        @PropMeta(propId=18)
    
        public Long getAssignedTo(){
            return _assignedTo;
        }

        public void setAssignedTo(Long value){
            this._assignedTo = value;
        }


        private Long _resolvedBy;

    
        @PropMeta(propId=19)
    
        public Long getResolvedBy(){
            return _resolvedBy;
        }

        public void setResolvedBy(Long value){
            this._resolvedBy = value;
        }


        private java.time.LocalDateTime _resolvedAt;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDateTime getResolvedAt(){
            return _resolvedAt;
        }

        public void setResolvedAt(java.time.LocalDateTime value){
            this._resolvedAt = value;
        }


        private String _resolution;

    
        @PropMeta(propId=21)
    
        public String getResolution(){
            return _resolution;
        }

        public void setResolution(String value){
            this._resolution = value;
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


        private Boolean _posted;

    
        @PropMeta(propId=30)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=31)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=32)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private String _returnCode;

    
        @PropMeta(propId=33)
    
        public String getReturnCode(){
            return _returnCode;
        }

        public void setReturnCode(String value){
            this._returnCode = value;
        }


        private List<Map<String,Object>> _actions;

        public List<Map<String,Object>> getActions(){
            return _actions;
        }

        public void setActions(List<Map<String,Object>> value){
            this._actions = value;
        }


        private Map<String,Object> _inspection;

        public Map<String,Object> getInspection(){
            return _inspection;
        }

        public void setInspection(Map<String,Object> value){
            this._inspection = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _supplier;

        public Map<String,Object> getSupplier(){
            return _supplier;
        }

        public void setSupplier(Map<String,Object> value){
            this._supplier = value;
        }


    }
