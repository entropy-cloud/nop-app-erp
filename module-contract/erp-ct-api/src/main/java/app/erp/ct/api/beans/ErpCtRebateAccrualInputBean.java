//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtRebateAccrualInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _rebateAgreementId;

    
        @PropMeta(propId=2)
    
        public Long getRebateAgreementId(){
            return _rebateAgreementId;
        }

        public void setRebateAgreementId(Long value){
            this._rebateAgreementId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _sourceBillType;

    
        @PropMeta(propId=4)
    
        public String getSourceBillType(){
            return _sourceBillType;
        }

        public void setSourceBillType(String value){
            this._sourceBillType = value;
        }


        private String _sourceBillCode;

    
        @PropMeta(propId=5)
    
        public String getSourceBillCode(){
            return _sourceBillCode;
        }

        public void setSourceBillCode(String value){
            this._sourceBillCode = value;
        }


        private java.math.BigDecimal _billAmountSource;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getBillAmountSource(){
            return _billAmountSource;
        }

        public void setBillAmountSource(java.math.BigDecimal value){
            this._billAmountSource = value;
        }


        private java.math.BigDecimal _accruedRebate;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getAccruedRebate(){
            return _accruedRebate;
        }

        public void setAccruedRebate(java.math.BigDecimal value){
            this._accruedRebate = value;
        }


        private java.time.LocalDate _accrualDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getAccrualDate(){
            return _accrualDate;
        }

        public void setAccrualDate(java.time.LocalDate value){
            this._accrualDate = value;
        }


        private Boolean _isSettled;

    
        @PropMeta(propId=9)
    
        public Boolean getIsSettled(){
            return _isSettled;
        }

        public void setIsSettled(Boolean value){
            this._isSettled = value;
        }


        private java.time.LocalDate _settledDate;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getSettledDate(){
            return _settledDate;
        }

        public void setSettledDate(java.time.LocalDate value){
            this._settledDate = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
