//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaCalibrationOutputBean {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _instrumentName;

    
        @PropMeta(propId=4)
    
        public String getInstrumentName(){
            return _instrumentName;
        }

        public void setInstrumentName(String value){
            this._instrumentName = value;
        }


        private String _instrumentCode;

    
        @PropMeta(propId=5)
    
        public String getInstrumentCode(){
            return _instrumentCode;
        }

        public void setInstrumentCode(String value){
            this._instrumentCode = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _standardRef;

    
        @PropMeta(propId=7)
    
        public String getStandardRef(){
            return _standardRef;
        }

        public void setStandardRef(String value){
            this._standardRef = value;
        }


        private String _measuredValue;

    
        @PropMeta(propId=8)
    
        public String getMeasuredValue(){
            return _measuredValue;
        }

        public void setMeasuredValue(String value){
            this._measuredValue = value;
        }


        private String _targetValue;

    
        @PropMeta(propId=9)
    
        public String getTargetValue(){
            return _targetValue;
        }

        public void setTargetValue(String value){
            this._targetValue = value;
        }


        private String _tolerance;

    
        @PropMeta(propId=10)
    
        public String getTolerance(){
            return _tolerance;
        }

        public void setTolerance(String value){
            this._tolerance = value;
        }


        private Integer _result;

    
        @PropMeta(propId=11)
    
        public Integer getResult(){
            return _result;
        }

        public void setResult(Integer value){
            this._result = value;
        }


        private String _result_label;

    
        public String getResult_label(){
            return _result_label;
        }

        public void setResult_label(String value){
            this._result_label = value;
        }


        private java.time.LocalDate _nextCalibrationDate;

    
        @PropMeta(propId=12)
    
        public java.time.LocalDate getNextCalibrationDate(){
            return _nextCalibrationDate;
        }

        public void setNextCalibrationDate(java.time.LocalDate value){
            this._nextCalibrationDate = value;
        }


        private Long _calibratedBy;

    
        @PropMeta(propId=13)
    
        public Long getCalibratedBy(){
            return _calibratedBy;
        }

        public void setCalibratedBy(Long value){
            this._calibratedBy = value;
        }


        private Integer _docStatus;

    
        @PropMeta(propId=14)
    
        public Integer getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(Integer value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private Integer _approveStatus;

    
        @PropMeta(propId=15)
    
        public Integer getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(Integer value){
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

    
        @PropMeta(propId=16)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=17)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=18)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=19)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=21)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _calibratedByEmployee;

        public Map<String,Object> getCalibratedByEmployee(){
            return _calibratedByEmployee;
        }

        public void setCalibratedByEmployee(Map<String,Object> value){
            this._calibratedByEmployee = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
