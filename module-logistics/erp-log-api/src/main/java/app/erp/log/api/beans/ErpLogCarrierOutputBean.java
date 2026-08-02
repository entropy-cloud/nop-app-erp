//__XGEN_FORCE_OVERRIDE__
    package app.erp.log.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpLogCarrierOutputBean {

    
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


        private String _carrierName;

    
        @PropMeta(propId=4)
    
        public String getCarrierName(){
            return _carrierName;
        }

        public void setCarrierName(String value){
            this._carrierName = value;
        }


        private String _carrierType;

    
        @PropMeta(propId=5)
    
        public String getCarrierType(){
            return _carrierType;
        }

        public void setCarrierType(String value){
            this._carrierType = value;
        }


        private String _carrierType_label;

    
        public String getCarrierType_label(){
            return _carrierType_label;
        }

        public void setCarrierType_label(String value){
            this._carrierType_label = value;
        }


        private String _gatewayId;

    
        @PropMeta(propId=6)
    
        public String getGatewayId(){
            return _gatewayId;
        }

        public void setGatewayId(String value){
            this._gatewayId = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=7)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Integer _isActive;

    
        @PropMeta(propId=8)
    
        public Integer getIsActive(){
            return _isActive;
        }

        public void setIsActive(Integer value){
            this._isActive = value;
        }


        private String _trackingUrlTemplate;

    
        @PropMeta(propId=9)
    
        public String getTrackingUrlTemplate(){
            return _trackingUrlTemplate;
        }

        public void setTrackingUrlTemplate(String value){
            this._trackingUrlTemplate = value;
        }


        private java.math.BigDecimal _maxParcelWeight;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getMaxParcelWeight(){
            return _maxParcelWeight;
        }

        public void setMaxParcelWeight(java.math.BigDecimal value){
            this._maxParcelWeight = value;
        }


        private String _supportedServiceTypes;

    
        @PropMeta(propId=11)
    
        public String getSupportedServiceTypes(){
            return _supportedServiceTypes;
        }

        public void setSupportedServiceTypes(String value){
            this._supportedServiceTypes = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
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


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private List<Map<String,Object>> _configs;

        public List<Map<String,Object>> getConfigs(){
            return _configs;
        }

        public void setConfigs(List<Map<String,Object>> value){
            this._configs = value;
        }


    }
