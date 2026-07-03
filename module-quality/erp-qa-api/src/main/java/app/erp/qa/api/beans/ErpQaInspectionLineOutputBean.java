//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaInspectionLineOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _inspectionId;

    
        @PropMeta(propId=2)
    
        public Long getInspectionId(){
            return _inspectionId;
        }

        public void setInspectionId(Long value){
            this._inspectionId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private Long _parameterId;

    
        @PropMeta(propId=4)
    
        public Long getParameterId(){
            return _parameterId;
        }

        public void setParameterId(Long value){
            this._parameterId = value;
        }


        private String _parameterName;

    
        @PropMeta(propId=5)
    
        public String getParameterName(){
            return _parameterName;
        }

        public void setParameterName(String value){
            this._parameterName = value;
        }


        private java.math.BigDecimal _specMin;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getSpecMin(){
            return _specMin;
        }

        public void setSpecMin(java.math.BigDecimal value){
            this._specMin = value;
        }


        private java.math.BigDecimal _specMax;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getSpecMax(){
            return _specMax;
        }

        public void setSpecMax(java.math.BigDecimal value){
            this._specMax = value;
        }


        private String _measuredValue;

    
        @PropMeta(propId=8)
    
        public String getMeasuredValue(){
            return _measuredValue;
        }

        public void setMeasuredValue(String value){
            this._measuredValue = value;
        }


        private String _unit;

    
        @PropMeta(propId=9)
    
        public String getUnit(){
            return _unit;
        }

        public void setUnit(String value){
            this._unit = value;
        }


        private String _result;

    
        @PropMeta(propId=10)
    
        public String getResult(){
            return _result;
        }

        public void setResult(String value){
            this._result = value;
        }


        private String _result_label;

    
        public String getResult_label(){
            return _result_label;
        }

        public void setResult_label(String value){
            this._result_label = value;
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


        private Map<String,Object> _inspection;

        public Map<String,Object> getInspection(){
            return _inspection;
        }

        public void setInspection(Map<String,Object> value){
            this._inspection = value;
        }


    }
