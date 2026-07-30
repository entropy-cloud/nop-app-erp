//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bAsnInputBean extends CrudInputBase {

    
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


        private Long _sourceEdiDocId;

    
        @PropMeta(propId=4)
    
        public Long getSourceEdiDocId(){
            return _sourceEdiDocId;
        }

        public void setSourceEdiDocId(Long value){
            this._sourceEdiDocId = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=5)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private java.time.LocalDate _shipmentDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getShipmentDate(){
            return _shipmentDate;
        }

        public void setShipmentDate(java.time.LocalDate value){
            this._shipmentDate = value;
        }


        private java.time.LocalDate _estimatedArrivalDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getEstimatedArrivalDate(){
            return _estimatedArrivalDate;
        }

        public void setEstimatedArrivalDate(java.time.LocalDate value){
            this._estimatedArrivalDate = value;
        }


        private String _trackingNo;

    
        @PropMeta(propId=8)
    
        public String getTrackingNo(){
            return _trackingNo;
        }

        public void setTrackingNo(String value){
            this._trackingNo = value;
        }


        private String _relatedBillType;

    
        @PropMeta(propId=9)
    
        public String getRelatedBillType(){
            return _relatedBillType;
        }

        public void setRelatedBillType(String value){
            this._relatedBillType = value;
        }


        private String _relatedBillCode;

    
        @PropMeta(propId=10)
    
        public String getRelatedBillCode(){
            return _relatedBillCode;
        }

        public void setRelatedBillCode(String value){
            this._relatedBillCode = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=19)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private List<ErpB2bAsnLineInputBean> _lines;

        public List<ErpB2bAsnLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpB2bAsnLineInputBean> value){
            this._lines = value;
        }


    }
