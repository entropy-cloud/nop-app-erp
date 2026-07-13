//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstMergeLineOutputBean {

    
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


        private Integer _version;

    
        @PropMeta(propId=11)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=12)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=14)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=16)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _mergeCode;

    
        public String getMergeCode(){
            return _mergeCode;
        }

        public void setMergeCode(String value){
            this._mergeCode = value;
        }


        private String _orgName;

    
        public String getOrgName(){
            return _orgName;
        }

        public void setOrgName(String value){
            this._orgName = value;
        }


        private String _sourceAssetCode;

    
        public String getSourceAssetCode(){
            return _sourceAssetCode;
        }

        public void setSourceAssetCode(String value){
            this._sourceAssetCode = value;
        }


        private Map<String,Object> _merge;

        public Map<String,Object> getMerge(){
            return _merge;
        }

        public void setMerge(Map<String,Object> value){
            this._merge = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _sourceAsset;

        public Map<String,Object> getSourceAsset(){
            return _sourceAsset;
        }

        public void setSourceAsset(Map<String,Object> value){
            this._sourceAsset = value;
        }


    }
