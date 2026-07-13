//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstSplitLineOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _splitId;

    
        @PropMeta(propId=2)
    
        public Long getSplitId(){
            return _splitId;
        }

        public void setSplitId(Long value){
            this._splitId = value;
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


        private String _splitCode;

    
        public String getSplitCode(){
            return _splitCode;
        }

        public void setSplitCode(String value){
            this._splitCode = value;
        }


        private String _orgName;

    
        public String getOrgName(){
            return _orgName;
        }

        public void setOrgName(String value){
            this._orgName = value;
        }


        private String _categoryName;

    
        public String getCategoryName(){
            return _categoryName;
        }

        public void setCategoryName(String value){
            this._categoryName = value;
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


        private Long _categoryId;

    
        @PropMeta(propId=7)
    
        public Long getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(Long value){
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


        private String _allocationMethod_label;

    
        public String getAllocationMethod_label(){
            return _allocationMethod_label;
        }

        public void setAllocationMethod_label(String value){
            this._allocationMethod_label = value;
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


        private Long _targetAssetId;

    
        @PropMeta(propId=13)
    
        public Long getTargetAssetId(){
            return _targetAssetId;
        }

        public void setTargetAssetId(Long value){
            this._targetAssetId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Map<String,Object> _split;

        public Map<String,Object> getSplit(){
            return _split;
        }

        public void setSplit(Map<String,Object> value){
            this._split = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _category;

        public Map<String,Object> getCategory(){
            return _category;
        }

        public void setCategory(Map<String,Object> value){
            this._category = value;
        }


        private Map<String,Object> _targetAsset;

        public Map<String,Object> getTargetAsset(){
            return _targetAsset;
        }

        public void setTargetAsset(Map<String,Object> value){
            this._targetAsset = value;
        }


    }
