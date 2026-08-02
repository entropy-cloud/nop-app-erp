//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtVolumeDiscountInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _contractLineId;

    
        @PropMeta(propId=2)
    
        public Long getContractLineId(){
            return _contractLineId;
        }

        public void setContractLineId(Long value){
            this._contractLineId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.math.BigDecimal _fromQty;

    
        @PropMeta(propId=4)
    
        public java.math.BigDecimal getFromQty(){
            return _fromQty;
        }

        public void setFromQty(java.math.BigDecimal value){
            this._fromQty = value;
        }


        private java.math.BigDecimal _toQty;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getToQty(){
            return _toQty;
        }

        public void setToQty(java.math.BigDecimal value){
            this._toQty = value;
        }


        private java.math.BigDecimal _discountPercent;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getDiscountPercent(){
            return _discountPercent;
        }

        public void setDiscountPercent(java.math.BigDecimal value){
            this._discountPercent = value;
        }


        private java.math.BigDecimal _unitPrice;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getUnitPrice(){
            return _unitPrice;
        }

        public void setUnitPrice(java.math.BigDecimal value){
            this._unitPrice = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
