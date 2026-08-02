//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrGapAnalysisOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _employeeId;

    
        @PropMeta(propId=2)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private Long _competencyId;

    
        @PropMeta(propId=3)
    
        public Long getCompetencyId(){
            return _competencyId;
        }

        public void setCompetencyId(Long value){
            this._competencyId = value;
        }


        private Integer _requiredLevel;

    
        @PropMeta(propId=4)
    
        public Integer getRequiredLevel(){
            return _requiredLevel;
        }

        public void setRequiredLevel(Integer value){
            this._requiredLevel = value;
        }


        private Integer _actualLevel;

    
        @PropMeta(propId=5)
    
        public Integer getActualLevel(){
            return _actualLevel;
        }

        public void setActualLevel(Integer value){
            this._actualLevel = value;
        }


        private Integer _gapValue;

    
        @PropMeta(propId=6)
    
        public Integer getGapValue(){
            return _gapValue;
        }

        public void setGapValue(Integer value){
            this._gapValue = value;
        }


        private String _gapSeverity;

    
        @PropMeta(propId=7)
    
        public String getGapSeverity(){
            return _gapSeverity;
        }

        public void setGapSeverity(String value){
            this._gapSeverity = value;
        }


        private String _gapSeverity_label;

    
        public String getGapSeverity_label(){
            return _gapSeverity_label;
        }

        public void setGapSeverity_label(String value){
            this._gapSeverity_label = value;
        }


        private java.time.LocalDate _assessmentDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getAssessmentDate(){
            return _assessmentDate;
        }

        public void setAssessmentDate(java.time.LocalDate value){
            this._assessmentDate = value;
        }


        private java.time.LocalDate _analysisDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getAnalysisDate(){
            return _analysisDate;
        }

        public void setAnalysisDate(java.time.LocalDate value){
            this._analysisDate = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=11)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=12)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=14)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _employee;

        public Map<String,Object> getEmployee(){
            return _employee;
        }

        public void setEmployee(Map<String,Object> value){
            this._employee = value;
        }


        private Map<String,Object> _competency;

        public Map<String,Object> getCompetency(){
            return _competency;
        }

        public void setCompetency(Map<String,Object> value){
            this._competency = value;
        }


    }
