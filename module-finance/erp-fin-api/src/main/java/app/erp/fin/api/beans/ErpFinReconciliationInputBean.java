//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinReconciliationInputBean extends CrudInputBase {

    
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


        private String _acctSchemaId;

    
        @PropMeta(propId=4)
    
        public String getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(String value){
            this._acctSchemaId = value;
        }


        private String _direction;

    
        @PropMeta(propId=5)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=6)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _currencyId;

    
        @PropMeta(propId=8)
    
        public String getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(String value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _totalAmountSource;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getTotalAmountSource(){
            return _totalAmountSource;
        }

        public void setTotalAmountSource(java.math.BigDecimal value){
            this._totalAmountSource = value;
        }


        private java.math.BigDecimal _totalAmountFunctional;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getTotalAmountFunctional(){
            return _totalAmountFunctional;
        }

        public void setTotalAmountFunctional(java.math.BigDecimal value){
            this._totalAmountFunctional = value;
        }


        private java.math.BigDecimal _fxGainLoss;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getFxGainLoss(){
            return _fxGainLoss;
        }

        public void setFxGainLoss(java.math.BigDecimal value){
            this._fxGainLoss = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=13)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=16)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpFinReconciliationLineInputBean> _lines;

        public List<ErpFinReconciliationLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpFinReconciliationLineInputBean> value){
            this._lines = value;
        }


    }
