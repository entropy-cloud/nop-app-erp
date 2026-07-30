//__XGEN_FORCE_OVERRIDE__
    package app.erp.log.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpLogShipmentLogOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _shipmentId;

    
        @PropMeta(propId=2)
    
        public Long getShipmentId(){
            return _shipmentId;
        }

        public void setShipmentId(Long value){
            this._shipmentId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _gatewayId;

    
        @PropMeta(propId=4)
    
        public String getGatewayId(){
            return _gatewayId;
        }

        public void setGatewayId(String value){
            this._gatewayId = value;
        }


        private String _actionType;

    
        @PropMeta(propId=5)
    
        public String getActionType(){
            return _actionType;
        }

        public void setActionType(String value){
            this._actionType = value;
        }


        private String _actionType_label;

    
        public String getActionType_label(){
            return _actionType_label;
        }

        public void setActionType_label(String value){
            this._actionType_label = value;
        }


        private String _requestBody;

    
        @PropMeta(propId=6)
    
        public String getRequestBody(){
            return _requestBody;
        }

        public void setRequestBody(String value){
            this._requestBody = value;
        }


        private String _responseBody;

    
        @PropMeta(propId=7)
    
        public String getResponseBody(){
            return _responseBody;
        }

        public void setResponseBody(String value){
            this._responseBody = value;
        }


        private Integer _httpStatus;

    
        @PropMeta(propId=8)
    
        public Integer getHttpStatus(){
            return _httpStatus;
        }

        public void setHttpStatus(Integer value){
            this._httpStatus = value;
        }


        private String _errorCode;

    
        @PropMeta(propId=9)
    
        public String getErrorCode(){
            return _errorCode;
        }

        public void setErrorCode(String value){
            this._errorCode = value;
        }


        private String _errorMessage;

    
        @PropMeta(propId=10)
    
        public String getErrorMessage(){
            return _errorMessage;
        }

        public void setErrorMessage(String value){
            this._errorMessage = value;
        }


        private Boolean _isSuccess;

    
        @PropMeta(propId=11)
    
        public Boolean getIsSuccess(){
            return _isSuccess;
        }

        public void setIsSuccess(Boolean value){
            this._isSuccess = value;
        }


        private java.sql.Timestamp _executedAt;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getExecutedAt(){
            return _executedAt;
        }

        public void setExecutedAt(java.sql.Timestamp value){
            this._executedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _shipment;

        public Map<String,Object> getShipment(){
            return _shipment;
        }

        public void setShipment(Map<String,Object> value){
            this._shipment = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
