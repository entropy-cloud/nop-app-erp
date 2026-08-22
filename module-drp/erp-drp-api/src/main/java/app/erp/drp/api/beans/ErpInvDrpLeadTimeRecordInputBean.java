//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvDrpLeadTimeRecordInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _orgId;

    
        @PropMeta(propId=2)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _supplierId;

    
        @PropMeta(propId=3)
    
        public String getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(String value){
            this._supplierId = value;
        }


        private String _materialId;

    
        @PropMeta(propId=4)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private java.time.LocalDate _orderDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getOrderDate(){
            return _orderDate;
        }

        public void setOrderDate(java.time.LocalDate value){
            this._orderDate = value;
        }


        private java.time.LocalDate _receiptDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getReceiptDate(){
            return _receiptDate;
        }

        public void setReceiptDate(java.time.LocalDate value){
            this._receiptDate = value;
        }


        private Integer _actualLeadTime;

    
        @PropMeta(propId=7)
    
        public Integer getActualLeadTime(){
            return _actualLeadTime;
        }

        public void setActualLeadTime(Integer value){
            this._actualLeadTime = value;
        }


        private Integer _expectedLeadTime;

    
        @PropMeta(propId=8)
    
        public Integer getExpectedLeadTime(){
            return _expectedLeadTime;
        }

        public void setExpectedLeadTime(Integer value){
            this._expectedLeadTime = value;
        }


        private Integer _varianceDays;

    
        @PropMeta(propId=9)
    
        public Integer getVarianceDays(){
            return _varianceDays;
        }

        public void setVarianceDays(Integer value){
            this._varianceDays = value;
        }


        private String _purchaseOrderCode;

    
        @PropMeta(propId=10)
    
        public String getPurchaseOrderCode(){
            return _purchaseOrderCode;
        }

        public void setPurchaseOrderCode(String value){
            this._purchaseOrderCode = value;
        }


        private Boolean _isOnTime;

    
        @PropMeta(propId=11)
    
        public Boolean getIsOnTime(){
            return _isOnTime;
        }

        public void setIsOnTime(Boolean value){
            this._isOnTime = value;
        }


        private String _earlyLateFlag;

    
        @PropMeta(propId=12)
    
        public String getEarlyLateFlag(){
            return _earlyLateFlag;
        }

        public void setEarlyLateFlag(String value){
            this._earlyLateFlag = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
