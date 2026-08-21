//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtRebateTierInputBean extends CrudInputBase {

    
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


        private java.math.BigDecimal _fromAmount;

    
        @PropMeta(propId=3)
    
        public java.math.BigDecimal getFromAmount(){
            return _fromAmount;
        }

        public void setFromAmount(java.math.BigDecimal value){
            this._fromAmount = value;
        }


        private java.math.BigDecimal _toAmount;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getToAmount(){
            return _toAmount;
        }

        public void setToAmount(java.math.BigDecimal value){
            this._toAmount = value;
        }


        private java.math.BigDecimal _rebatePercent;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getRebatePercent(){
            return _rebatePercent;
        }

        public void setRebatePercent(java.math.BigDecimal value){
            this._rebatePercent = value;
        }


        private java.math.BigDecimal _rebateAmount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getRebateAmount(){
            return _rebateAmount;
        }

        public void setRebateAmount(java.math.BigDecimal value){
            this._rebateAmount = value;
        }


        private String _remark;

    
        @PropMeta(propId=7)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
