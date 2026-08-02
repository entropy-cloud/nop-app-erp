//__XGEN_FORCE_OVERRIDE__
    package app.erp.log.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpLogCarrierConfigInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _carrierId;

    
        @PropMeta(propId=2)
    
        public Long getCarrierId(){
            return _carrierId;
        }

        public void setCarrierId(Long value){
            this._carrierId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _configCode;

    
        @PropMeta(propId=4)
    
        public String getConfigCode(){
            return _configCode;
        }

        public void setConfigCode(String value){
            this._configCode = value;
        }


        private String _serviceType;

    
        @PropMeta(propId=5)
    
        public String getServiceType(){
            return _serviceType;
        }

        public void setServiceType(String value){
            this._serviceType = value;
        }


        private String _apiEndpoint;

    
        @PropMeta(propId=6)
    
        public String getApiEndpoint(){
            return _apiEndpoint;
        }

        public void setApiEndpoint(String value){
            this._apiEndpoint = value;
        }


        private String _apiKey;

    
        @PropMeta(propId=7)
    
        public String getApiKey(){
            return _apiKey;
        }

        public void setApiKey(String value){
            this._apiKey = value;
        }


        private String _apiSecret;

    
        @PropMeta(propId=8)
    
        public String getApiSecret(){
            return _apiSecret;
        }

        public void setApiSecret(String value){
            this._apiSecret = value;
        }


        private String _credentials;

    
        @PropMeta(propId=9)
    
        public String getCredentials(){
            return _credentials;
        }

        public void setCredentials(String value){
            this._credentials = value;
        }


        private String _trackingUrlTemplate;

    
        @PropMeta(propId=10)
    
        public String getTrackingUrlTemplate(){
            return _trackingUrlTemplate;
        }

        public void setTrackingUrlTemplate(String value){
            this._trackingUrlTemplate = value;
        }


        private String _printFormat;

    
        @PropMeta(propId=11)
    
        public String getPrintFormat(){
            return _printFormat;
        }

        public void setPrintFormat(String value){
            this._printFormat = value;
        }


        private String _additionalProperties;

    
        @PropMeta(propId=12)
    
        public String getAdditionalProperties(){
            return _additionalProperties;
        }

        public void setAdditionalProperties(String value){
            this._additionalProperties = value;
        }


        private Integer _isActive;

    
        @PropMeta(propId=13)
    
        public Integer getIsActive(){
            return _isActive;
        }

        public void setIsActive(Integer value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
