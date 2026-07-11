//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgCostRollupLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _costRollupId;

    
        @PropMeta(propId=2)
    
        public Long getCostRollupId(){
            return _costRollupId;
        }

        public void setCostRollupId(Long value){
            this._costRollupId = value;
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


        private java.math.BigDecimal _materialCost;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getMaterialCost(){
            return _materialCost;
        }

        public void setMaterialCost(java.math.BigDecimal value){
            this._materialCost = value;
        }


        private java.math.BigDecimal _laborCost;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getLaborCost(){
            return _laborCost;
        }

        public void setLaborCost(java.math.BigDecimal value){
            this._laborCost = value;
        }


        private java.math.BigDecimal _overheadCost;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getOverheadCost(){
            return _overheadCost;
        }

        public void setOverheadCost(java.math.BigDecimal value){
            this._overheadCost = value;
        }


        private java.math.BigDecimal _subcontractCost;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getSubcontractCost(){
            return _subcontractCost;
        }

        public void setSubcontractCost(java.math.BigDecimal value){
            this._subcontractCost = value;
        }


        private java.math.BigDecimal _totalCost;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(java.math.BigDecimal value){
            this._totalCost = value;
        }


        private java.math.BigDecimal _unitCost;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(java.math.BigDecimal value){
            this._unitCost = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=12)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


    }
