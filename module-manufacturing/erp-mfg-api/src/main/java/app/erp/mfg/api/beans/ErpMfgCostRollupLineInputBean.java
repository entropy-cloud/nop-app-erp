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


        private String _materialCost;

    
        @PropMeta(propId=6)
    
        public String getMaterialCost(){
            return _materialCost;
        }

        public void setMaterialCost(String value){
            this._materialCost = value;
        }


        private String _laborCost;

    
        @PropMeta(propId=7)
    
        public String getLaborCost(){
            return _laborCost;
        }

        public void setLaborCost(String value){
            this._laborCost = value;
        }


        private String _overheadCost;

    
        @PropMeta(propId=8)
    
        public String getOverheadCost(){
            return _overheadCost;
        }

        public void setOverheadCost(String value){
            this._overheadCost = value;
        }


        private String _subcontractCost;

    
        @PropMeta(propId=9)
    
        public String getSubcontractCost(){
            return _subcontractCost;
        }

        public void setSubcontractCost(String value){
            this._subcontractCost = value;
        }


        private String _totalCost;

    
        @PropMeta(propId=10)
    
        public String getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(String value){
            this._totalCost = value;
        }


        private String _unitCost;

    
        @PropMeta(propId=11)
    
        public String getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(String value){
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


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
