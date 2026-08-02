//__XGEN_FORCE_OVERRIDE__
    package app.erp.log.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpLogShipmentInputBean extends CrudInputBase {

    
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


        private String _freightSettlementStatus;

    
        @PropMeta(propId=14)
    
        public String getFreightSettlementStatus(){
            return _freightSettlementStatus;
        }

        public void setFreightSettlementStatus(String value){
            this._freightSettlementStatus = value;
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


        private String _remark;

    
        @PropMeta(propId=33)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=40)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private List<ErpLogShipmentLineInputBean> _lines;

        public List<ErpLogShipmentLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpLogShipmentLineInputBean> value){
            this._lines = value;
        }


        private List<ErpLogShipmentParcelInputBean> _parcels;

        public List<ErpLogShipmentParcelInputBean> getParcels(){
            return _parcels;
        }

        public void setParcels(List<ErpLogShipmentParcelInputBean> value){
            this._parcels = value;
        }


        private List<ErpLogShipmentLogInputBean> _logs;

        public List<ErpLogShipmentLogInputBean> getLogs(){
            return _logs;
        }

        public void setLogs(List<ErpLogShipmentLogInputBean> value){
            this._logs = value;
        }


    }
