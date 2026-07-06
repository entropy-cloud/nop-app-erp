//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstMergeLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _mergeId;

    
        @PropMeta(propId=2)
    
        public Long getMergeId(){
            return _mergeId;
        }

        public void setMergeId(Long value){
            this._mergeId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
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


        private Long _sourceAssetId;

    
        @PropMeta(propId=5)
    
        public Long getSourceAssetId(){
            return _sourceAssetId;
        }

        public void setSourceAssetId(Long value){
            this._sourceAssetId = value;
        }


        private java.math.BigDecimal _contributionProportion;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getContributionProportion(){
            return _contributionProportion;
        }

        public void setContributionProportion(java.math.BigDecimal value){
            this._contributionProportion = value;
        }


        private java.math.BigDecimal _originalCostAmount;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getOriginalCostAmount(){
            return _originalCostAmount;
        }

        public void setOriginalCostAmount(java.math.BigDecimal value){
            this._originalCostAmount = value;
        }


        private java.math.BigDecimal _accumulatedDepreciationAmount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getAccumulatedDepreciationAmount(){
            return _accumulatedDepreciationAmount;
        }

        public void setAccumulatedDepreciationAmount(java.math.BigDecimal value){
            this._accumulatedDepreciationAmount = value;
        }


        private java.math.BigDecimal _netBookValue;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getNetBookValue(){
            return _netBookValue;
        }

        public void setNetBookValue(java.math.BigDecimal value){
            this._netBookValue = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private String _remark;

    
        @PropMeta(propId=16)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
