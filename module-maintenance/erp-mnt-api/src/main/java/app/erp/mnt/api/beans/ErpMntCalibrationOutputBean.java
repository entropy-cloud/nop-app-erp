//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntCalibrationOutputBean {

    
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


        private Long _equipmentId;

    
        @PropMeta(propId=4)
    
        public Long getEquipmentId(){
            return _equipmentId;
        }

        public void setEquipmentId(Long value){
            this._equipmentId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _standardRef;

    
        @PropMeta(propId=6)
    
        public String getStandardRef(){
            return _standardRef;
        }

        public void setStandardRef(String value){
            this._standardRef = value;
        }


        private java.math.BigDecimal _measuredValue;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getMeasuredValue(){
            return _measuredValue;
        }

        public void setMeasuredValue(java.math.BigDecimal value){
            this._measuredValue = value;
        }


        private java.math.BigDecimal _targetValue;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getTargetValue(){
            return _targetValue;
        }

        public void setTargetValue(java.math.BigDecimal value){
            this._targetValue = value;
        }


        private java.math.BigDecimal _tolerance;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getTolerance(){
            return _tolerance;
        }

        public void setTolerance(java.math.BigDecimal value){
            this._tolerance = value;
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


        private java.time.LocalDate _nextCalibrationDate;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getNextCalibrationDate(){
            return _nextCalibrationDate;
        }

        public void setNextCalibrationDate(java.time.LocalDate value){
            this._nextCalibrationDate = value;
        }


        private Long _calibratedBy;

    
        @PropMeta(propId=12)
    
        public Long getCalibratedBy(){
            return _calibratedBy;
        }

        public void setCalibratedBy(Long value){
            this._calibratedBy = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=13)
    
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

    
        @PropMeta(propId=14)
    
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

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=17)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=18)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=20)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=200)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=201)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private Map<String,Object> _equipment;

        public Map<String,Object> getEquipment(){
            return _equipment;
        }

        public void setEquipment(Map<String,Object> value){
            this._equipment = value;
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
