//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSalarySimulationOutputBean {

    
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


        private Long _sourceSalaryId;

    
        @PropMeta(propId=4)
    
        public Long getSourceSalaryId(){
            return _sourceSalaryId;
        }

        public void setSourceSalaryId(Long value){
            this._sourceSalaryId = value;
        }


        private Integer _simulationPeriodYear;

    
        @PropMeta(propId=5)
    
        public Integer getSimulationPeriodYear(){
            return _simulationPeriodYear;
        }

        public void setSimulationPeriodYear(Integer value){
            this._simulationPeriodYear = value;
        }


        private Integer _simulationPeriodMonth;

    
        @PropMeta(propId=6)
    
        public Integer getSimulationPeriodMonth(){
            return _simulationPeriodMonth;
        }

        public void setSimulationPeriodMonth(Integer value){
            this._simulationPeriodMonth = value;
        }


        private String _simulationName;

    
        @PropMeta(propId=7)
    
        public String getSimulationName(){
            return _simulationName;
        }

        public void setSimulationName(String value){
            this._simulationName = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
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


        private Long _reviewerId;

    
        @PropMeta(propId=9)
    
        public Long getReviewerId(){
            return _reviewerId;
        }

        public void setReviewerId(Long value){
            this._reviewerId = value;
        }


        private java.sql.Timestamp _reviewedAt;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getReviewedAt(){
            return _reviewedAt;
        }

        public void setReviewedAt(java.sql.Timestamp value){
            this._reviewedAt = value;
        }


        private java.sql.Timestamp _convertedAt;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getConvertedAt(){
            return _convertedAt;
        }

        public void setConvertedAt(java.sql.Timestamp value){
            this._convertedAt = value;
        }


        private Long _convertedSalaryId;

    
        @PropMeta(propId=12)
    
        public Long getConvertedSalaryId(){
            return _convertedSalaryId;
        }

        public void setConvertedSalaryId(Long value){
            this._convertedSalaryId = value;
        }


        private String _notes;

    
        @PropMeta(propId=13)
    
        public String getNotes(){
            return _notes;
        }

        public void setNotes(String value){
            this._notes = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=20)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Map<String,Object> _sourceSalary;

        public Map<String,Object> getSourceSalary(){
            return _sourceSalary;
        }

        public void setSourceSalary(Map<String,Object> value){
            this._sourceSalary = value;
        }


        private Map<String,Object> _reviewer;

        public Map<String,Object> getReviewer(){
            return _reviewer;
        }

        public void setReviewer(Map<String,Object> value){
            this._reviewer = value;
        }


        private Map<String,Object> _convertedSalary;

        public Map<String,Object> getConvertedSalary(){
            return _convertedSalary;
        }

        public void setConvertedSalary(Map<String,Object> value){
            this._convertedSalary = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
