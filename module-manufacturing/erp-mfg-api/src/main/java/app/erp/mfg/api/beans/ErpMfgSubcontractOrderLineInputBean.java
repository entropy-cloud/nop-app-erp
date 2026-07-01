//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgSubcontractOrderLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _subcontractOrderId;

    
        @PropMeta(propId=2)
    
        public Long getSubcontractOrderId(){
            return _subcontractOrderId;
        }

        public void setSubcontractOrderId(Long value){
            this._subcontractOrderId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=4)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _uoMId;

    
        @PropMeta(propId=5)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
        }


        private java.math.BigDecimal _quantity;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getQuantity(){
            return _quantity;
        }

        public void setQuantity(java.math.BigDecimal value){
            this._quantity = value;
        }


        private java.math.BigDecimal _unitProcessingFee;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getUnitProcessingFee(){
            return _unitProcessingFee;
        }

        public void setUnitProcessingFee(java.math.BigDecimal value){
            this._unitProcessingFee = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
            this._amount = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
