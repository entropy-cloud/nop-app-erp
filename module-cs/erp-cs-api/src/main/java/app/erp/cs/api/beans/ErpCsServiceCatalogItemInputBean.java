//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsServiceCatalogItemInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=4)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _categoryId;

    
        @PropMeta(propId=5)
    
        public String getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(String value){
            this._categoryId = value;
        }


        private String _parentId;

    
        @PropMeta(propId=6)
    
        public String getParentId(){
            return _parentId;
        }

        public void setParentId(String value){
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


        private String _ticketTypeId;

    
        @PropMeta(propId=9)
    
        public String getTicketTypeId(){
            return _ticketTypeId;
        }

        public void setTicketTypeId(String value){
            this._ticketTypeId = value;
        }


        private String _slaPolicyId;

    
        @PropMeta(propId=10)
    
        public String getSlaPolicyId(){
            return _slaPolicyId;
        }

        public void setSlaPolicyId(String value){
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


    }
