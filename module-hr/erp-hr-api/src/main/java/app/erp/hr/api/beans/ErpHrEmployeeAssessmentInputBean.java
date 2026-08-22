//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrEmployeeAssessmentInputBean extends CrudInputBase {

    
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


        private String _assessmentType;

    
        @PropMeta(propId=3)
    
        public String getAssessmentType(){
            return _assessmentType;
        }

        public void setAssessmentType(String value){
            this._assessmentType = value;
        }


        private String _assessorId;

    
        @PropMeta(propId=4)
    
        public String getAssessorId(){
            return _assessorId;
        }

        public void setAssessorId(String value){
            this._assessorId = value;
        }


        private java.time.LocalDate _assessmentDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getAssessmentDate(){
            return _assessmentDate;
        }

        public void setAssessmentDate(java.time.LocalDate value){
            this._assessmentDate = value;
        }


        private String _status;

    
        @PropMeta(propId=6)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private java.math.BigDecimal _overallScore;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getOverallScore(){
            return _overallScore;
        }

        public void setOverallScore(java.math.BigDecimal value){
            this._overallScore = value;
        }


        private String _orgId;

    
        @PropMeta(propId=8)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=16)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private List<ErpHrAssessmentDetailInputBean> _details;

        public List<ErpHrAssessmentDetailInputBean> getDetails(){
            return _details;
        }

        public void setDetails(List<ErpHrAssessmentDetailInputBean> value){
            this._details = value;
        }


    }
