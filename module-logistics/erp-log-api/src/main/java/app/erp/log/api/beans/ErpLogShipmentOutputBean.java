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
    public class ErpLogShipmentOutputBean {

    
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


        private Long _carrierId;

    
        @PropMeta(propId=4)
    
        public Long getCarrierId(){
            return _carrierId;
        }

        public void setCarrierId(Long value){
            this._carrierId = value;
        }


        private Long _carrierConfigId;

    
        @PropMeta(propId=5)
    
        public Long getCarrierConfigId(){
            return _carrierConfigId;
        }

        public void setCarrierConfigId(Long value){
            this._carrierConfigId = value;
        }


        private String _relatedBillType;

    
        @PropMeta(propId=6)
    
        public String getRelatedBillType(){
            return _relatedBillType;
        }

        public void setRelatedBillType(String value){
            this._relatedBillType = value;
        }


        private String _relatedBillCode;

    
        @PropMeta(propId=7)
    
        public String getRelatedBillCode(){
            return _relatedBillCode;
        }

        public void setRelatedBillCode(String value){
            this._relatedBillCode = value;
        }


        private java.time.LocalDate _shipmentDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getShipmentDate(){
            return _shipmentDate;
        }

        public void setShipmentDate(java.time.LocalDate value){
            this._shipmentDate = value;
        }


        private String _trackingNo;

    
        @PropMeta(propId=9)
    
        public String getTrackingNo(){
            return _trackingNo;
        }

        public void setTrackingNo(String value){
            this._trackingNo = value;
        }


        private String _labelUrl;

    
        @PropMeta(propId=10)
    
        public String getLabelUrl(){
            return _labelUrl;
        }

        public void setLabelUrl(String value){
            this._labelUrl = value;
        }


        private java.math.BigDecimal _freightAmount;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getFreightAmount(){
            return _freightAmount;
        }

        public void setFreightAmount(java.math.BigDecimal value){
            this._freightAmount = value;
        }


        private Long _freightCurrencyId;

    
        @PropMeta(propId=12)
    
        public Long getFreightCurrencyId(){
            return _freightCurrencyId;
        }

        public void setFreightCurrencyId(Long value){
            this._freightCurrencyId = value;
        }


        private String _freightTerms;

    
        @PropMeta(propId=13)
    
        public String getFreightTerms(){
            return _freightTerms;
        }

        public void setFreightTerms(String value){
            this._freightTerms = value;
        }


        private String _freightTerms_label;

    
        public String getFreightTerms_label(){
            return _freightTerms_label;
        }

        public void setFreightTerms_label(String value){
            this._freightTerms_label = value;
        }


        private String _freightSettlementStatus;

    
        @PropMeta(propId=14)
    
        public String getFreightSettlementStatus(){
            return _freightSettlementStatus;
        }

        public void setFreightSettlementStatus(String value){
            this._freightSettlementStatus = value;
        }


        private String _freightSettlementStatus_label;

    
        public String getFreightSettlementStatus_label(){
            return _freightSettlementStatus_label;
        }

        public void setFreightSettlementStatus_label(String value){
            this._freightSettlementStatus_label = value;
        }


        private java.math.BigDecimal _totalWeight;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getTotalWeight(){
            return _totalWeight;
        }

        public void setTotalWeight(java.math.BigDecimal value){
            this._totalWeight = value;
        }


        private java.math.BigDecimal _totalVolume;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getTotalVolume(){
            return _totalVolume;
        }

        public void setTotalVolume(java.math.BigDecimal value){
            this._totalVolume = value;
        }


        private Integer _totalParcels;

    
        @PropMeta(propId=17)
    
        public Integer getTotalParcels(){
            return _totalParcels;
        }

        public void setTotalParcels(Integer value){
            this._totalParcels = value;
        }


        private String _receiverName;

    
        @PropMeta(propId=18)
    
        public String getReceiverName(){
            return _receiverName;
        }

        public void setReceiverName(String value){
            this._receiverName = value;
        }


        private String _receiverPhone;

    
        @PropMeta(propId=19)
    
        public String getReceiverPhone(){
            return _receiverPhone;
        }

        public void setReceiverPhone(String value){
            this._receiverPhone = value;
        }


        private String _receiverAddress;

    
        @PropMeta(propId=20)
    
        public String getReceiverAddress(){
            return _receiverAddress;
        }

        public void setReceiverAddress(String value){
            this._receiverAddress = value;
        }


        private String _receiverCountry;

    
        @PropMeta(propId=21)
    
        public String getReceiverCountry(){
            return _receiverCountry;
        }

        public void setReceiverCountry(String value){
            this._receiverCountry = value;
        }


        private String _receiverProvince;

    
        @PropMeta(propId=22)
    
        public String getReceiverProvince(){
            return _receiverProvince;
        }

        public void setReceiverProvince(String value){
            this._receiverProvince = value;
        }


        private String _receiverCity;

    
        @PropMeta(propId=23)
    
        public String getReceiverCity(){
            return _receiverCity;
        }

        public void setReceiverCity(String value){
            this._receiverCity = value;
        }


        private String _receiverDistrict;

    
        @PropMeta(propId=24)
    
        public String getReceiverDistrict(){
            return _receiverDistrict;
        }

        public void setReceiverDistrict(String value){
            this._receiverDistrict = value;
        }


        private String _senderName;

    
        @PropMeta(propId=25)
    
        public String getSenderName(){
            return _senderName;
        }

        public void setSenderName(String value){
            this._senderName = value;
        }


        private String _senderPhone;

    
        @PropMeta(propId=26)
    
        public String getSenderPhone(){
            return _senderPhone;
        }

        public void setSenderPhone(String value){
            this._senderPhone = value;
        }


        private String _senderAddress;

    
        @PropMeta(propId=27)
    
        public String getSenderAddress(){
            return _senderAddress;
        }

        public void setSenderAddress(String value){
            this._senderAddress = value;
        }


        private java.time.LocalDate _estimatedDeliveryDate;

    
        @PropMeta(propId=28)
    
        public java.time.LocalDate getEstimatedDeliveryDate(){
            return _estimatedDeliveryDate;
        }

        public void setEstimatedDeliveryDate(java.time.LocalDate value){
            this._estimatedDeliveryDate = value;
        }


        private java.time.LocalDate _actualDeliveryDate;

    
        @PropMeta(propId=29)
    
        public java.time.LocalDate getActualDeliveryDate(){
            return _actualDeliveryDate;
        }

        public void setActualDeliveryDate(java.time.LocalDate value){
            this._actualDeliveryDate = value;
        }


        private String _signedBy;

    
        @PropMeta(propId=30)
    
        public String getSignedBy(){
            return _signedBy;
        }

        public void setSignedBy(String value){
            this._signedBy = value;
        }


        private Long _shipperId;

    
        @PropMeta(propId=31)
    
        public Long getShipperId(){
            return _shipperId;
        }

        public void setShipperId(Long value){
            this._shipperId = value;
        }


        private String _status;

    
        @PropMeta(propId=32)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private String _remark;

    
        @PropMeta(propId=33)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=34)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=35)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=36)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=37)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=38)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=39)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=40)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=41)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private Map<String,Object> _carrier;

        public Map<String,Object> getCarrier(){
            return _carrier;
        }

        public void setCarrier(Map<String,Object> value){
            this._carrier = value;
        }


        private Map<String,Object> _carrierConfig;

        public Map<String,Object> getCarrierConfig(){
            return _carrierConfig;
        }

        public void setCarrierConfig(Map<String,Object> value){
            this._carrierConfig = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _shipper;

        public Map<String,Object> getShipper(){
            return _shipper;
        }

        public void setShipper(Map<String,Object> value){
            this._shipper = value;
        }


        private List<Map<String,Object>> _lines;

        public List<Map<String,Object>> getLines(){
            return _lines;
        }

        public void setLines(List<Map<String,Object>> value){
            this._lines = value;
        }


        private List<Map<String,Object>> _parcels;

        public List<Map<String,Object>> getParcels(){
            return _parcels;
        }

        public void setParcels(List<Map<String,Object>> value){
            this._parcels = value;
        }


        private List<Map<String,Object>> _logs;

        public List<Map<String,Object>> getLogs(){
            return _logs;
        }

        public void setLogs(List<Map<String,Object>> value){
            this._logs = value;
        }


        private Map<String,Object> _freightCurrency;

        public Map<String,Object> getFreightCurrency(){
            return _freightCurrency;
        }

        public void setFreightCurrency(Map<String,Object> value){
            this._freightCurrency = value;
        }


    }
