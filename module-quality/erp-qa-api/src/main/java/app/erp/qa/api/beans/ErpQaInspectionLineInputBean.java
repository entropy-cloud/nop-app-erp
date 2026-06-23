//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaInspectionLineInputBean extends CrudInputBase {

    
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


        private String _specMin;

    
        @PropMeta(propId=6)
    
        public String getSpecMin(){
            return _specMin;
        }

        public void setSpecMin(String value){
            this._specMin = value;
        }


        private String _specMax;

    
        @PropMeta(propId=7)
    
        public String getSpecMax(){
            return _specMax;
        }

        public void setSpecMax(String value){
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


        private Integer _result;

    
        @PropMeta(propId=10)
    
        public Integer getResult(){
            return _result;
        }

        public void setResult(Integer value){
            this._result = value;
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


    }
