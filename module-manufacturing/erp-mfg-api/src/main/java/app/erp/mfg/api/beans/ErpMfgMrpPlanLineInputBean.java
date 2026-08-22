//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgMrpPlanLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _mrpPlanId;

    
        @PropMeta(propId=2)
    
        public String getMrpPlanId(){
            return _mrpPlanId;
        }

        public void setMrpPlanId(String value){
            this._mrpPlanId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _materialId;

    
        @PropMeta(propId=4)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _uoMId;

    
        @PropMeta(propId=5)
    
        public String getUoMId(){
            return _uoMId;
        }

        public void setUoMId(String value){
            this._uoMId = value;
        }


        private String _orderType;

    
        @PropMeta(propId=6)
    
        public String getOrderType(){
            return _orderType;
        }

        public void setOrderType(String value){
            this._orderType = value;
        }


        private java.math.BigDecimal _grossRequirement;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getGrossRequirement(){
            return _grossRequirement;
        }

        public void setGrossRequirement(java.math.BigDecimal value){
            this._grossRequirement = value;
        }


        private java.math.BigDecimal _scheduledReceipt;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getScheduledReceipt(){
            return _scheduledReceipt;
        }

        public void setScheduledReceipt(java.math.BigDecimal value){
            this._scheduledReceipt = value;
        }


        private java.math.BigDecimal _onHand;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getOnHand(){
            return _onHand;
        }

        public void setOnHand(java.math.BigDecimal value){
            this._onHand = value;
        }


        private java.math.BigDecimal _netRequirement;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getNetRequirement(){
            return _netRequirement;
        }

        public void setNetRequirement(java.math.BigDecimal value){
            this._netRequirement = value;
        }


        private java.math.BigDecimal _plannedQuantity;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getPlannedQuantity(){
            return _plannedQuantity;
        }

        public void setPlannedQuantity(java.math.BigDecimal value){
            this._plannedQuantity = value;
        }


        private java.time.LocalDate _plannedDate;

    
        @PropMeta(propId=12)
    
        public java.time.LocalDate getPlannedDate(){
            return _plannedDate;
        }

        public void setPlannedDate(java.time.LocalDate value){
            this._plannedDate = value;
        }


        private String _parentLineId;

    
        @PropMeta(propId=13)
    
        public String getParentLineId(){
            return _parentLineId;
        }

        public void setParentLineId(String value){
            this._parentLineId = value;
        }


        private Boolean _isFirmed;

    
        @PropMeta(propId=14)
    
        public Boolean getIsFirmed(){
            return _isFirmed;
        }

        public void setIsFirmed(Boolean value){
            this._isFirmed = value;
        }


        private String _convertedBillCode;

    
        @PropMeta(propId=15)
    
        public String getConvertedBillCode(){
            return _convertedBillCode;
        }

        public void setConvertedBillCode(String value){
            this._convertedBillCode = value;
        }


    }
