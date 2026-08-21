//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstSplitLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _splitId;

    
        @PropMeta(propId=2)
    
        public String getSplitId(){
            return _splitId;
        }

        public void setSplitId(String value){
            this._splitId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=4)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _targetAssetCode;

    
        @PropMeta(propId=5)
    
        public String getTargetAssetCode(){
            return _targetAssetCode;
        }

        public void setTargetAssetCode(String value){
            this._targetAssetCode = value;
        }


        private String _targetAssetName;

    
        @PropMeta(propId=6)
    
        public String getTargetAssetName(){
            return _targetAssetName;
        }

        public void setTargetAssetName(String value){
            this._targetAssetName = value;
        }


        private String _categoryId;

    
        @PropMeta(propId=7)
    
        public String getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(String value){
            this._categoryId = value;
        }


        private String _allocationMethod;

    
        @PropMeta(propId=8)
    
        public String getAllocationMethod(){
            return _allocationMethod;
        }

        public void setAllocationMethod(String value){
            this._allocationMethod = value;
        }


        private java.math.BigDecimal _proportion;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getProportion(){
            return _proportion;
        }

        public void setProportion(java.math.BigDecimal value){
            this._proportion = value;
        }


        private java.math.BigDecimal _originalCostAmount;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getOriginalCostAmount(){
            return _originalCostAmount;
        }

        public void setOriginalCostAmount(java.math.BigDecimal value){
            this._originalCostAmount = value;
        }


        private java.math.BigDecimal _accumulatedDepreciationAmount;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getAccumulatedDepreciationAmount(){
            return _accumulatedDepreciationAmount;
        }

        public void setAccumulatedDepreciationAmount(java.math.BigDecimal value){
            this._accumulatedDepreciationAmount = value;
        }


        private java.math.BigDecimal _netBookValue;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getNetBookValue(){
            return _netBookValue;
        }

        public void setNetBookValue(java.math.BigDecimal value){
            this._netBookValue = value;
        }


        private String _targetAssetId;

    
        @PropMeta(propId=13)
    
        public String getTargetAssetId(){
            return _targetAssetId;
        }

        public void setTargetAssetId(String value){
            this._targetAssetId = value;
        }


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
