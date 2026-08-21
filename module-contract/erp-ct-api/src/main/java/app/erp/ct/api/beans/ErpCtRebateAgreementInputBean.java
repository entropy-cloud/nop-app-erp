//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtRebateAgreementInputBean extends CrudInputBase {

    
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


        private String _contractId;

    
        @PropMeta(propId=4)
    
        public String getContractId(){
            return _contractId;
        }

        public void setContractId(String value){
            this._contractId = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=5)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private String _rebateType;

    
        @PropMeta(propId=6)
    
        public String getRebateType(){
            return _rebateType;
        }

        public void setRebateType(String value){
            this._rebateType = value;
        }


        private java.time.LocalDate _agreementDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getAgreementDate(){
            return _agreementDate;
        }

        public void setAgreementDate(java.time.LocalDate value){
            this._agreementDate = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private String _accrualMethod;

    
        @PropMeta(propId=10)
    
        public String getAccrualMethod(){
            return _accrualMethod;
        }

        public void setAccrualMethod(String value){
            this._accrualMethod = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private java.math.BigDecimal _totalAccumulatedAmount;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getTotalAccumulatedAmount(){
            return _totalAccumulatedAmount;
        }

        public void setTotalAccumulatedAmount(java.math.BigDecimal value){
            this._totalAccumulatedAmount = value;
        }


        private java.math.BigDecimal _estimatedRebateAmount;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getEstimatedRebateAmount(){
            return _estimatedRebateAmount;
        }

        public void setEstimatedRebateAmount(java.math.BigDecimal value){
            this._estimatedRebateAmount = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=21)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
