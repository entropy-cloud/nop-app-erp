//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgCostRollupLineOutputBean {

    
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


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _costRollupCode;

    
        public String getCostRollupCode(){
            return _costRollupCode;
        }

        public void setCostRollupCode(String value){
            this._costRollupCode = value;
        }


        private String _materialName;

    
        public String getMaterialName(){
            return _materialName;
        }

        public void setMaterialName(String value){
            this._materialName = value;
        }


        private String _uomName;

    
        public String getUomName(){
            return _uomName;
        }

        public void setUomName(String value){
            this._uomName = value;
        }


        private String _currencyName;

    
        public String getCurrencyName(){
            return _currencyName;
        }

        public void setCurrencyName(String value){
            this._currencyName = value;
        }


        private Map<String,Object> _costRollup;

        public Map<String,Object> getCostRollup(){
            return _costRollup;
        }

        public void setCostRollup(Map<String,Object> value){
            this._costRollup = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _uoM;

        public Map<String,Object> getUoM(){
            return _uoM;
        }

        public void setUoM(Map<String,Object> value){
            this._uoM = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


    }
