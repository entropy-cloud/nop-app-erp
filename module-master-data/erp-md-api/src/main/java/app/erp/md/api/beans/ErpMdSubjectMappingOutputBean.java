//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdSubjectMappingOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _sourceSubjectId;

    
        @PropMeta(propId=2)
    
        public Long getSourceSubjectId(){
            return _sourceSubjectId;
        }

        public void setSourceSubjectId(Long value){
            this._sourceSubjectId = value;
        }


        private Long _targetAcctSchemaId;

    
        @PropMeta(propId=3)
    
        public Long getTargetAcctSchemaId(){
            return _targetAcctSchemaId;
        }

        public void setTargetAcctSchemaId(Long value){
            this._targetAcctSchemaId = value;
        }


        private Long _targetSubjectId;

    
        @PropMeta(propId=4)
    
        public Long getTargetSubjectId(){
            return _targetSubjectId;
        }

        public void setTargetSubjectId(Long value){
            this._targetSubjectId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=5)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=6)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=7)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=8)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=9)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _sourceSubject;

        public Map<String,Object> getSourceSubject(){
            return _sourceSubject;
        }

        public void setSourceSubject(Map<String,Object> value){
            this._sourceSubject = value;
        }


        private Map<String,Object> _targetAcctSchema;

        public Map<String,Object> getTargetAcctSchema(){
            return _targetAcctSchema;
        }

        public void setTargetAcctSchema(Map<String,Object> value){
            this._targetAcctSchema = value;
        }


        private Map<String,Object> _targetSubject;

        public Map<String,Object> getTargetSubject(){
            return _targetSubject;
        }

        public void setTargetSubject(Map<String,Object> value){
            this._targetSubject = value;
        }


    }
