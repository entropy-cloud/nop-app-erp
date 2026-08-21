//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstValueAdjustmentInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _assetId;

    
        @PropMeta(propId=4)
    
        public String getAssetId(){
            return _assetId;
        }

        public void setAssetId(String value){
            this._assetId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=6)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private String _adjustmentType;

    
        @PropMeta(propId=8)
    
        public String getAdjustmentType(){
            return _adjustmentType;
        }

        public void setAdjustmentType(String value){
            this._adjustmentType = value;
        }


        private java.math.BigDecimal _adjustmentAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getAdjustmentAmount(){
            return _adjustmentAmount;
        }

        public void setAdjustmentAmount(java.math.BigDecimal value){
            this._adjustmentAmount = value;
        }


        private String _reason;

    
        @PropMeta(propId=10)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=11)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=12)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=18)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=19)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private String _remark;

    
        @PropMeta(propId=26)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
