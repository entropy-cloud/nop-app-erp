//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstAssetCategoryOutputBean {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private Integer _depreciationMethod;

    
        @PropMeta(propId=4)
    
        public Integer getDepreciationMethod(){
            return _depreciationMethod;
        }

        public void setDepreciationMethod(Integer value){
            this._depreciationMethod = value;
        }


        private String _depreciationMethod_label;

    
        public String getDepreciationMethod_label(){
            return _depreciationMethod_label;
        }

        public void setDepreciationMethod_label(String value){
            this._depreciationMethod_label = value;
        }


        private Integer _usefulLifeMonths;

    
        @PropMeta(propId=5)
    
        public Integer getUsefulLifeMonths(){
            return _usefulLifeMonths;
        }

        public void setUsefulLifeMonths(Integer value){
            this._usefulLifeMonths = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=6)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
            this._subjectId = value;
        }


        private Long _depreciationSubjectId;

    
        @PropMeta(propId=7)
    
        public Long getDepreciationSubjectId(){
            return _depreciationSubjectId;
        }

        public void setDepreciationSubjectId(Long value){
            this._depreciationSubjectId = value;
        }


        private Long _expenseSubjectId;

    
        @PropMeta(propId=8)
    
        public Long getExpenseSubjectId(){
            return _expenseSubjectId;
        }

        public void setExpenseSubjectId(Long value){
            this._expenseSubjectId = value;
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


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Map<String,Object> _subject;

        public Map<String,Object> getSubject(){
            return _subject;
        }

        public void setSubject(Map<String,Object> value){
            this._subject = value;
        }


        private Map<String,Object> _depreciationSubject;

        public Map<String,Object> getDepreciationSubject(){
            return _depreciationSubject;
        }

        public void setDepreciationSubject(Map<String,Object> value){
            this._depreciationSubject = value;
        }


        private Map<String,Object> _expenseSubject;

        public Map<String,Object> getExpenseSubject(){
            return _expenseSubject;
        }

        public void setExpenseSubject(Map<String,Object> value){
            this._expenseSubject = value;
        }


    }
