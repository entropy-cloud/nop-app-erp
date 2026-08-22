//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrGapAnalysisInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _employeeId;

    
        @PropMeta(propId=2)
    
        public String getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(String value){
            this._employeeId = value;
        }


        private String _competencyId;

    
        @PropMeta(propId=3)
    
        public String getCompetencyId(){
            return _competencyId;
        }

        public void setCompetencyId(String value){
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


    }
