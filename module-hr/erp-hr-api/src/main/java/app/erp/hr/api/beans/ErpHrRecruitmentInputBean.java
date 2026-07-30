//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrRecruitmentInputBean extends CrudInputBase {

    
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


        private Long _positionId;

    
        @PropMeta(propId=3)
    
        public Long getPositionId(){
            return _positionId;
        }

        public void setPositionId(Long value){
            this._positionId = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=4)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
            this._departmentId = value;
        }


        private Integer _headcount;

    
        @PropMeta(propId=5)
    
        public Integer getHeadcount(){
            return _headcount;
        }

        public void setHeadcount(Integer value){
            this._headcount = value;
        }


        private String _candidateName;

    
        @PropMeta(propId=6)
    
        public String getCandidateName(){
            return _candidateName;
        }

        public void setCandidateName(String value){
            this._candidateName = value;
        }


        private String _candidatePhone;

    
        @PropMeta(propId=7)
    
        public String getCandidatePhone(){
            return _candidatePhone;
        }

        public void setCandidatePhone(String value){
            this._candidatePhone = value;
        }


        private String _candidateEmail;

    
        @PropMeta(propId=8)
    
        public String getCandidateEmail(){
            return _candidateEmail;
        }

        public void setCandidateEmail(String value){
            this._candidateEmail = value;
        }


        private String _source;

    
        @PropMeta(propId=9)
    
        public String getSource(){
            return _source;
        }

        public void setSource(String value){
            this._source = value;
        }


        private String _resumeAttachmentFileId;

    
        @PropMeta(propId=10)
    
        public String getResumeAttachmentFileId(){
            return _resumeAttachmentFileId;
        }

        public void setResumeAttachmentFileId(String value){
            this._resumeAttachmentFileId = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _interviewerId;

    
        @PropMeta(propId=12)
    
        public Long getInterviewerId(){
            return _interviewerId;
        }

        public void setInterviewerId(Long value){
            this._interviewerId = value;
        }


        private java.time.LocalDate _interviewDate;

    
        @PropMeta(propId=13)
    
        public java.time.LocalDate getInterviewDate(){
            return _interviewDate;
        }

        public void setInterviewDate(java.time.LocalDate value){
            this._interviewDate = value;
        }


        private java.math.BigDecimal _offerSalary;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getOfferSalary(){
            return _offerSalary;
        }

        public void setOfferSalary(java.math.BigDecimal value){
            this._offerSalary = value;
        }


        private java.time.LocalDate _hiredDate;

    
        @PropMeta(propId=15)
    
        public java.time.LocalDate getHiredDate(){
            return _hiredDate;
        }

        public void setHiredDate(java.time.LocalDate value){
            this._hiredDate = value;
        }


        private Long _employeeId;

    
        @PropMeta(propId=16)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=17)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=18)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=25)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
