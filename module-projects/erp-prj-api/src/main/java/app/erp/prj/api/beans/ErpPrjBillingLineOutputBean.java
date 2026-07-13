//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjBillingLineOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _billingId;

    
        @PropMeta(propId=2)
    
        public Long getBillingId(){
            return _billingId;
        }

        public void setBillingId(Long value){
            this._billingId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _costCategory;

    
        @PropMeta(propId=4)
    
        public String getCostCategory(){
            return _costCategory;
        }

        public void setCostCategory(String value){
            this._costCategory = value;
        }


        private Long _taskId;

    
        @PropMeta(propId=5)
    
        public Long getTaskId(){
            return _taskId;
        }

        public void setTaskId(Long value){
            this._taskId = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=6)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
            this._subjectId = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
            this._amount = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=10)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=11)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=13)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _billingCode;

    
        public String getBillingCode(){
            return _billingCode;
        }

        public void setBillingCode(String value){
            this._billingCode = value;
        }


        private String _taskName;

    
        public String getTaskName(){
            return _taskName;
        }

        public void setTaskName(String value){
            this._taskName = value;
        }


        private String _subjectName;

    
        public String getSubjectName(){
            return _subjectName;
        }

        public void setSubjectName(String value){
            this._subjectName = value;
        }


        private Map<String,Object> _billing;

        public Map<String,Object> getBilling(){
            return _billing;
        }

        public void setBilling(Map<String,Object> value){
            this._billing = value;
        }


        private Map<String,Object> _subject;

        public Map<String,Object> getSubject(){
            return _subject;
        }

        public void setSubject(Map<String,Object> value){
            this._subject = value;
        }


        private Map<String,Object> _task;

        public Map<String,Object> getTask(){
            return _task;
        }

        public void setTask(Map<String,Object> value){
            this._task = value;
        }


    }
