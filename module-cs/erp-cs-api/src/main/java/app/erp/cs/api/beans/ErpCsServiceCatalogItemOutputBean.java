//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsServiceCatalogItemOutputBean {

    
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


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _categoryId;

    
        @PropMeta(propId=5)
    
        public Long getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(Long value){
            this._categoryId = value;
        }


        private Long _parentId;

    
        @PropMeta(propId=6)
    
        public Long getParentId(){
            return _parentId;
        }

        public void setParentId(Long value){
            this._parentId = value;
        }


        private String _shortDescription;

    
        @PropMeta(propId=7)
    
        public String getShortDescription(){
            return _shortDescription;
        }

        public void setShortDescription(String value){
            this._shortDescription = value;
        }


        private String _fullDescription;

    
        @PropMeta(propId=8)
    
        public String getFullDescription(){
            return _fullDescription;
        }

        public void setFullDescription(String value){
            this._fullDescription = value;
        }


        private Long _ticketTypeId;

    
        @PropMeta(propId=9)
    
        public Long getTicketTypeId(){
            return _ticketTypeId;
        }

        public void setTicketTypeId(Long value){
            this._ticketTypeId = value;
        }


        private Long _slaPolicyId;

    
        @PropMeta(propId=10)
    
        public Long getSlaPolicyId(){
            return _slaPolicyId;
        }

        public void setSlaPolicyId(Long value){
            this._slaPolicyId = value;
        }


        private String _fulfillmentProcessId;

    
        @PropMeta(propId=11)
    
        public String getFulfillmentProcessId(){
            return _fulfillmentProcessId;
        }

        public void setFulfillmentProcessId(String value){
            this._fulfillmentProcessId = value;
        }


        private String _requestFormConfig;

    
        @PropMeta(propId=12)
    
        public String getRequestFormConfig(){
            return _requestFormConfig;
        }

        public void setRequestFormConfig(String value){
            this._requestFormConfig = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=13)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private Boolean _isPublic;

    
        @PropMeta(propId=14)
    
        public Boolean getIsPublic(){
            return _isPublic;
        }

        public void setIsPublic(Boolean value){
            this._isPublic = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=15)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
        }


        private String _estimatedResolution;

    
        @PropMeta(propId=16)
    
        public String getEstimatedResolution(){
            return _estimatedResolution;
        }

        public void setEstimatedResolution(String value){
            this._estimatedResolution = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=17)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=18)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=19)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=21)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=22)
    
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


        private Map<String,Object> _parent;

        public Map<String,Object> getParent(){
            return _parent;
        }

        public void setParent(Map<String,Object> value){
            this._parent = value;
        }


        private Map<String,Object> _ticketType;

        public Map<String,Object> getTicketType(){
            return _ticketType;
        }

        public void setTicketType(Map<String,Object> value){
            this._ticketType = value;
        }


        private Map<String,Object> _slaPolicy;

        public Map<String,Object> getSlaPolicy(){
            return _slaPolicy;
        }

        public void setSlaPolicy(Map<String,Object> value){
            this._slaPolicy = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
