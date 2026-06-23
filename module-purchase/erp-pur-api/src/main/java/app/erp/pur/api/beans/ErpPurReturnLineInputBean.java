//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurReturnLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _returnId;

    
        @PropMeta(propId=2)
    
        public Long getReturnId(){
            return _returnId;
        }

        public void setReturnId(Long value){
            this._returnId = value;
        }


        private Long _receiveLineId;

    
        @PropMeta(propId=3)
    
        public Long getReceiveLineId(){
            return _receiveLineId;
        }

        public void setReceiveLineId(Long value){
            this._receiveLineId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=4)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=5)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _skuId;

    
        @PropMeta(propId=6)
    
        public Long getSkuId(){
            return _skuId;
        }

        public void setSkuId(Long value){
            this._skuId = value;
        }


        private Long _uoMId;

    
        @PropMeta(propId=7)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
        }


        private String _quantity;

    
        @PropMeta(propId=8)
    
        public String getQuantity(){
            return _quantity;
        }

        public void setQuantity(String value){
            this._quantity = value;
        }


        private String _unitPrice;

    
        @PropMeta(propId=9)
    
        public String getUnitPrice(){
            return _unitPrice;
        }

        public void setUnitPrice(String value){
            this._unitPrice = value;
        }


        private String _taxRate;

    
        @PropMeta(propId=10)
    
        public String getTaxRate(){
            return _taxRate;
        }

        public void setTaxRate(String value){
            this._taxRate = value;
        }


        private String _taxAmount;

    
        @PropMeta(propId=11)
    
        public String getTaxAmount(){
            return _taxAmount;
        }

        public void setTaxAmount(String value){
            this._taxAmount = value;
        }


        private String _amount;

    
        @PropMeta(propId=12)
    
        public String getAmount(){
            return _amount;
        }

        public void setAmount(String value){
            this._amount = value;
        }


        private String _reason;

    
        @PropMeta(propId=13)
    
        public String getReason(){
            return _reason;
        }

        public void setReason(String value){
            this._reason = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
