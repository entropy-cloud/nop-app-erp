//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrCompetencyLevelOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _competencyId;

    
        @PropMeta(propId=2)
    
        public Long getCompetencyId(){
            return _competencyId;
        }

        public void setCompetencyId(Long value){
            this._competencyId = value;
        }


        private Integer _levelNumber;

    
        @PropMeta(propId=3)
    
        public Integer getLevelNumber(){
            return _levelNumber;
        }

        public void setLevelNumber(Integer value){
            this._levelNumber = value;
        }


        private String _levelName;

    
        @PropMeta(propId=4)
    
        public String getLevelName(){
            return _levelName;
        }

        public void setLevelName(String value){
            this._levelName = value;
        }


        private String _behavioralAnchor;

    
        @PropMeta(propId=5)
    
        public String getBehavioralAnchor(){
            return _behavioralAnchor;
        }

        public void setBehavioralAnchor(String value){
            this._behavioralAnchor = value;
        }


        private Integer _sortOrder;

    
        @PropMeta(propId=6)
    
        public Integer getSortOrder(){
            return _sortOrder;
        }

        public void setSortOrder(Integer value){
            this._sortOrder = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=7)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=8)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=9)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=11)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _competency;

        public Map<String,Object> getCompetency(){
            return _competency;
        }

        public void setCompetency(Map<String,Object> value){
            this._competency = value;
        }


    }
