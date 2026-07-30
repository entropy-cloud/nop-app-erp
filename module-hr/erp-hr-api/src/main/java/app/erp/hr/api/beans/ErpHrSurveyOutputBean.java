//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrSurveyOutputBean {

    
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


        private String _title;

    
        @PropMeta(propId=3)
    
        public String getTitle(){
            return _title;
        }

        public void setTitle(String value){
            this._title = value;
        }


        private String _description;

    
        @PropMeta(propId=4)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _surveyType;

    
        @PropMeta(propId=5)
    
        public String getSurveyType(){
            return _surveyType;
        }

        public void setSurveyType(String value){
            this._surveyType = value;
        }


        private String _surveyType_label;

    
        public String getSurveyType_label(){
            return _surveyType_label;
        }

        public void setSurveyType_label(String value){
            this._surveyType_label = value;
        }


        private Boolean _isAnonymous;

    
        @PropMeta(propId=6)
    
        public Boolean getIsAnonymous(){
            return _isAnonymous;
        }

        public void setIsAnonymous(Boolean value){
            this._isAnonymous = value;
        }


        private String _status;

    
        @PropMeta(propId=7)
    
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


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private Long _targetDepartmentId;

    
        @PropMeta(propId=10)
    
        public Long getTargetDepartmentId(){
            return _targetDepartmentId;
        }

        public void setTargetDepartmentId(Long value){
            this._targetDepartmentId = value;
        }


        private Boolean _includeENps;

    
        @PropMeta(propId=11)
    
        public Boolean getIncludeENps(){
            return _includeENps;
        }

        public void setIncludeENps(Boolean value){
            this._includeENps = value;
        }


        private String _eNpsQuestion;

    
        @PropMeta(propId=12)
    
        public String getENpsQuestion(){
            return _eNpsQuestion;
        }

        public void setENpsQuestion(String value){
            this._eNpsQuestion = value;
        }


        private Integer _reminderDays;

    
        @PropMeta(propId=13)
    
        public Integer getReminderDays(){
            return _reminderDays;
        }

        public void setReminderDays(Integer value){
            this._reminderDays = value;
        }


        private Integer _totalQuestions;

    
        @PropMeta(propId=14)
    
        public Integer getTotalQuestions(){
            return _totalQuestions;
        }

        public void setTotalQuestions(Integer value){
            this._totalQuestions = value;
        }


        private Integer _totalResponses;

    
        @PropMeta(propId=15)
    
        public Integer getTotalResponses(){
            return _totalResponses;
        }

        public void setTotalResponses(Integer value){
            this._totalResponses = value;
        }


        private java.math.BigDecimal _completionRate;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getCompletionRate(){
            return _completionRate;
        }

        public void setCompletionRate(java.math.BigDecimal value){
            this._completionRate = value;
        }


        private java.math.BigDecimal _avgScore;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getAvgScore(){
            return _avgScore;
        }

        public void setAvgScore(java.math.BigDecimal value){
            this._avgScore = value;
        }


        private Integer _eNpsScore;

    
        @PropMeta(propId=18)
    
        public Integer getENpsScore(){
            return _eNpsScore;
        }

        public void setENpsScore(Integer value){
            this._eNpsScore = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=19)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=21)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=22)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=23)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=25)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=26)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private List<Map<String,Object>> _questions;

        public List<Map<String,Object>> getQuestions(){
            return _questions;
        }

        public void setQuestions(List<Map<String,Object>> value){
            this._questions = value;
        }


        private List<Map<String,Object>> _responses;

        public List<Map<String,Object>> getResponses(){
            return _responses;
        }

        public void setResponses(List<Map<String,Object>> value){
            this._responses = value;
        }


        private Map<String,Object> _targetDepartment;

        public Map<String,Object> getTargetDepartment(){
            return _targetDepartment;
        }

        public void setTargetDepartment(Map<String,Object> value){
            this._targetDepartment = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
