//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjMilestoneOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=2)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private String _code;

    
        @PropMeta(propId=3)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private String _name;

    
        @PropMeta(propId=4)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private java.time.LocalDate _plannedDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getPlannedDate(){
            return _plannedDate;
        }

        public void setPlannedDate(java.time.LocalDate value){
            this._plannedDate = value;
        }


        private java.time.LocalDate _actualDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getActualDate(){
            return _actualDate;
        }

        public void setActualDate(java.time.LocalDate value){
            this._actualDate = value;
        }


        private java.math.BigDecimal _billingAmount;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getBillingAmount(){
            return _billingAmount;
        }

        public void setBillingAmount(java.math.BigDecimal value){
            this._billingAmount = value;
        }


        private Boolean _isBillingTrigger;

    
        @PropMeta(propId=8)
    
        public Boolean getIsBillingTrigger(){
            return _isBillingTrigger;
        }

        public void setIsBillingTrigger(Boolean value){
            this._isBillingTrigger = value;
        }


        private String _status;

    
        @PropMeta(propId=9)
    
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


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=12)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=13)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=15)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _projectName;

    
        public String getProjectName(){
            return _projectName;
        }

        public void setProjectName(String value){
            this._projectName = value;
        }


        private Map<String,Object> _project;

        public Map<String,Object> getProject(){
            return _project;
        }

        public void setProject(Map<String,Object> value){
            this._project = value;
        }


    }
