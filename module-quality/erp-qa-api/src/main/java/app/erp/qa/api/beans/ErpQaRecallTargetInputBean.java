//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaRecallTargetInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _recallId;

    
        @PropMeta(propId=2)
    
        public Long getRecallId(){
            return _recallId;
        }

        public void setRecallId(Long value){
            this._recallId = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=3)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=4)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private String _serialNo;

    
        @PropMeta(propId=5)
    
        public String getSerialNo(){
            return _serialNo;
        }

        public void setSerialNo(String value){
            this._serialNo = value;
        }


        private Long _salesDeliveryId;

    
        @PropMeta(propId=6)
    
        public Long getSalesDeliveryId(){
            return _salesDeliveryId;
        }

        public void setSalesDeliveryId(Long value){
            this._salesDeliveryId = value;
        }


        private java.math.BigDecimal _shippedQty;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getShippedQty(){
            return _shippedQty;
        }

        public void setShippedQty(java.math.BigDecimal value){
            this._shippedQty = value;
        }


        private java.sql.Timestamp _notifiedAt;

    
        @PropMeta(propId=8)
    
        public java.sql.Timestamp getNotifiedAt(){
            return _notifiedAt;
        }

        public void setNotifiedAt(java.sql.Timestamp value){
            this._notifiedAt = value;
        }


        private String _notifiedBy;

    
        @PropMeta(propId=9)
    
        public String getNotifiedBy(){
            return _notifiedBy;
        }

        public void setNotifiedBy(String value){
            this._notifiedBy = value;
        }


        private String _returnStatus;

    
        @PropMeta(propId=10)
    
        public String getReturnStatus(){
            return _returnStatus;
        }

        public void setReturnStatus(String value){
            this._returnStatus = value;
        }


        private Long _generatedReturnId;

    
        @PropMeta(propId=11)
    
        public Long getGeneratedReturnId(){
            return _generatedReturnId;
        }

        public void setGeneratedReturnId(Long value){
            this._generatedReturnId = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
