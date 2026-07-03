//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurSupplierScorecardVariableOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _criteriaId;

    
        @PropMeta(propId=2)
    
        public Long getCriteriaId(){
            return _criteriaId;
        }

        public void setCriteriaId(Long value){
            this._criteriaId = value;
        }


        private String _variableName;

    
        @PropMeta(propId=3)
    
        public String getVariableName(){
            return _variableName;
        }

        public void setVariableName(String value){
            this._variableName = value;
        }


        private String _path;

    
        @PropMeta(propId=4)
    
        public String getPath(){
            return _path;
        }

        public void setPath(String value){
            this._path = value;
        }


        private java.math.BigDecimal _value;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getValue(){
            return _value;
        }

        public void setValue(java.math.BigDecimal value){
            this._value = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=6)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=7)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=8)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=10)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _criteria;

        public Map<String,Object> getCriteria(){
            return _criteria;
        }

        public void setCriteria(Map<String,Object> value){
            this._criteria = value;
        }


    }
