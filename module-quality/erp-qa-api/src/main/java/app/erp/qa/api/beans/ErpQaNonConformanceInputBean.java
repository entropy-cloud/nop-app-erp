//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaNonConformanceInputBean extends CrudInputBase {

    
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


        private String _quantity;

    
        @PropMeta(propId=8)
    
        public String getQuantity(){
            return _quantity;
        }

        public void setQuantity(String value){
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


        private Integer _severity;

    
        @PropMeta(propId=10)
    
        public Integer getSeverity(){
            return _severity;
        }

        public void setSeverity(Integer value){
            this._severity = value;
        }


        private Integer _dispositionType;

    
        @PropMeta(propId=11)
    
        public Integer getDispositionType(){
            return _dispositionType;
        }

        public void setDispositionType(Integer value){
            this._dispositionType = value;
        }


        private Integer _status;

    
        @PropMeta(propId=12)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
            this._status = value;
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


        private String _specMin;

    
        @PropMeta(propId=16)
    
        public String getSpecMin(){
            return _specMin;
        }

        public void setSpecMin(String value){
            this._specMin = value;
        }


        private String _specMax;

    
        @PropMeta(propId=17)
    
        public String getSpecMax(){
            return _specMax;
        }

        public void setSpecMax(String value){
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


        private List<ErpQaActionInputBean> _actions;

        public List<ErpQaActionInputBean> getActions(){
            return _actions;
        }

        public void setActions(List<ErpQaActionInputBean> value){
            this._actions = value;
        }


    }
