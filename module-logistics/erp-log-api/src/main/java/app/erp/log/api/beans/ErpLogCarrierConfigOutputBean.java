//__XGEN_FORCE_OVERRIDE__
    package app.erp.log.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpLogCarrierConfigOutputBean {

    
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


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=16)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=17)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=19)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _carrier;

        public Map<String,Object> getCarrier(){
            return _carrier;
        }

        public void setCarrier(Map<String,Object> value){
            this._carrier = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
