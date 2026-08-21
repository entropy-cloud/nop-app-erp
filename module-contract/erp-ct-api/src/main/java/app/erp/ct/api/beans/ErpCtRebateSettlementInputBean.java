//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtRebateSettlementInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _rebateAgreementId;

    
        @PropMeta(propId=2)
    
        public String getRebateAgreementId(){
            return _rebateAgreementId;
        }

        public void setRebateAgreementId(String value){
            this._rebateAgreementId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private java.time.LocalDate _settlementDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getSettlementDate(){
            return _settlementDate;
        }

        public void setSettlementDate(java.time.LocalDate value){
            this._settlementDate = value;
        }


        private java.math.BigDecimal _totalRebateAmount;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getTotalRebateAmount(){
            return _totalRebateAmount;
        }

        public void setTotalRebateAmount(java.math.BigDecimal value){
            this._totalRebateAmount = value;
        }


        private String _creditMemoBillType;

    
        @PropMeta(propId=6)
    
        public String getCreditMemoBillType(){
            return _creditMemoBillType;
        }

        public void setCreditMemoBillType(String value){
            this._creditMemoBillType = value;
        }


        private String _creditMemoBillCode;

    
        @PropMeta(propId=7)
    
        public String getCreditMemoBillCode(){
            return _creditMemoBillCode;
        }

        public void setCreditMemoBillCode(String value){
            this._creditMemoBillCode = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


    }
