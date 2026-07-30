//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsCannedResponseOutputBean {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _title;

    
        @PropMeta(propId=4)
    
        public String getTitle(){
            return _title;
        }

        public void setTitle(String value){
            this._title = value;
        }


        private String _content;

    
        @PropMeta(propId=5)
    
        public String getContent(){
            return _content;
        }

        public void setContent(String value){
            this._content = value;
        }


        private Long _categoryId;

    
        @PropMeta(propId=6)
    
        public Long getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(Long value){
            this._categoryId = value;
        }


        private String _variableDefs;

    
        @PropMeta(propId=7)
    
        public String getVariableDefs(){
            return _variableDefs;
        }

        public void setVariableDefs(String value){
            this._variableDefs = value;
        }


        private Long _macroTicketTypeId;

    
        @PropMeta(propId=8)
    
        public Long getMacroTicketTypeId(){
            return _macroTicketTypeId;
        }

        public void setMacroTicketTypeId(Long value){
            this._macroTicketTypeId = value;
        }


        private String _macroPriority;

    
        @PropMeta(propId=9)
    
        public String getMacroPriority(){
            return _macroPriority;
        }

        public void setMacroPriority(String value){
            this._macroPriority = value;
        }


        private String _macroPriority_label;

    
        public String getMacroPriority_label(){
            return _macroPriority_label;
        }

        public void setMacroPriority_label(String value){
            this._macroPriority_label = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=10)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=11)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private Integer _usageCount;

    
        @PropMeta(propId=12)
    
        public Integer getUsageCount(){
            return _usageCount;
        }

        public void setUsageCount(Integer value){
            this._usageCount = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _category;

        public Map<String,Object> getCategory(){
            return _category;
        }

        public void setCategory(Map<String,Object> value){
            this._category = value;
        }


        private Map<String,Object> _macroTicketType;

        public Map<String,Object> getMacroTicketType(){
            return _macroTicketType;
        }

        public void setMacroTicketType(Map<String,Object> value){
            this._macroTicketType = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
