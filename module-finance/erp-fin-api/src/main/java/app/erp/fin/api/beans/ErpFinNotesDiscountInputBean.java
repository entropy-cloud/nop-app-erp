//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinNotesDiscountInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _notesReceivableId;

    
        @PropMeta(propId=2)
    
        public String getNotesReceivableId(){
            return _notesReceivableId;
        }

        public void setNotesReceivableId(String value){
            this._notesReceivableId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private java.time.LocalDate _discountDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getDiscountDate(){
            return _discountDate;
        }

        public void setDiscountDate(java.time.LocalDate value){
            this._discountDate = value;
        }


        private String _bankId;

    
        @PropMeta(propId=5)
    
        public String getBankId(){
            return _bankId;
        }

        public void setBankId(String value){
            this._bankId = value;
        }


        private java.math.BigDecimal _faceAmount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getFaceAmount(){
            return _faceAmount;
        }

        public void setFaceAmount(java.math.BigDecimal value){
            this._faceAmount = value;
        }


        private java.math.BigDecimal _discountInterest;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getDiscountInterest(){
            return _discountInterest;
        }

        public void setDiscountInterest(java.math.BigDecimal value){
            this._discountInterest = value;
        }


        private java.math.BigDecimal _netAmount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getNetAmount(){
            return _netAmount;
        }

        public void setNetAmount(java.math.BigDecimal value){
            this._netAmount = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=9)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _exchangeGainLoss;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getExchangeGainLoss(){
            return _exchangeGainLoss;
        }

        public void setExchangeGainLoss(java.math.BigDecimal value){
            this._exchangeGainLoss = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
