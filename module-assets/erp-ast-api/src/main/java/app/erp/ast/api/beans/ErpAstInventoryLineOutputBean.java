//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstInventoryLineOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _inventoryId;

    
        @PropMeta(propId=2)
    
        public Long getInventoryId(){
            return _inventoryId;
        }

        public void setInventoryId(Long value){
            this._inventoryId = value;
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


        private Long _assetId;

    
        @PropMeta(propId=5)
    
        public Long getAssetId(){
            return _assetId;
        }

        public void setAssetId(Long value){
            this._assetId = value;
        }


        private String _assetCodeSnapshot;

    
        @PropMeta(propId=6)
    
        public String getAssetCodeSnapshot(){
            return _assetCodeSnapshot;
        }

        public void setAssetCodeSnapshot(String value){
            this._assetCodeSnapshot = value;
        }


        private String _assetNameSnapshot;

    
        @PropMeta(propId=7)
    
        public String getAssetNameSnapshot(){
            return _assetNameSnapshot;
        }

        public void setAssetNameSnapshot(String value){
            this._assetNameSnapshot = value;
        }


        private Long _categoryId;

    
        @PropMeta(propId=8)
    
        public Long getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(Long value){
            this._categoryId = value;
        }


        private Integer _bookQuantity;

    
        @PropMeta(propId=9)
    
        public Integer getBookQuantity(){
            return _bookQuantity;
        }

        public void setBookQuantity(Integer value){
            this._bookQuantity = value;
        }


        private Integer _actualQuantity;

    
        @PropMeta(propId=10)
    
        public Integer getActualQuantity(){
            return _actualQuantity;
        }

        public void setActualQuantity(Integer value){
            this._actualQuantity = value;
        }


        private Integer _varianceQuantity;

    
        @PropMeta(propId=11)
    
        public Integer getVarianceQuantity(){
            return _varianceQuantity;
        }

        public void setVarianceQuantity(Integer value){
            this._varianceQuantity = value;
        }


        private String _varianceType;

    
        @PropMeta(propId=12)
    
        public String getVarianceType(){
            return _varianceType;
        }

        public void setVarianceType(String value){
            this._varianceType = value;
        }


        private String _varianceType_label;

    
        public String getVarianceType_label(){
            return _varianceType_label;
        }

        public void setVarianceType_label(String value){
            this._varianceType_label = value;
        }


        private java.math.BigDecimal _bookValue;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getBookValue(){
            return _bookValue;
        }

        public void setBookValue(java.math.BigDecimal value){
            this._bookValue = value;
        }


        private java.math.BigDecimal _assessedValue;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getAssessedValue(){
            return _assessedValue;
        }

        public void setAssessedValue(java.math.BigDecimal value){
            this._assessedValue = value;
        }


        private java.math.BigDecimal _varianceAmount;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getVarianceAmount(){
            return _varianceAmount;
        }

        public void setVarianceAmount(java.math.BigDecimal value){
            this._varianceAmount = value;
        }


        private String _disposition;

    
        @PropMeta(propId=16)
    
        public String getDisposition(){
            return _disposition;
        }

        public void setDisposition(String value){
            this._disposition = value;
        }


        private String _disposition_label;

    
        public String getDisposition_label(){
            return _disposition_label;
        }

        public void setDisposition_label(String value){
            this._disposition_label = value;
        }


        private Long _newAssetId;

    
        @PropMeta(propId=17)
    
        public Long getNewAssetId(){
            return _newAssetId;
        }

        public void setNewAssetId(Long value){
            this._newAssetId = value;
        }


        private Long _capitalizationId;

    
        @PropMeta(propId=18)
    
        public Long getCapitalizationId(){
            return _capitalizationId;
        }

        public void setCapitalizationId(Long value){
            this._capitalizationId = value;
        }


        private Long _disposalId;

    
        @PropMeta(propId=19)
    
        public Long getDisposalId(){
            return _disposalId;
        }

        public void setDisposalId(Long value){
            this._disposalId = value;
        }


        private String _investigatedRemark;

    
        @PropMeta(propId=20)
    
        public String getInvestigatedRemark(){
            return _investigatedRemark;
        }

        public void setInvestigatedRemark(String value){
            this._investigatedRemark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=21)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=22)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=23)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=25)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=26)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=27)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _inventoryCode;

    
        public String getInventoryCode(){
            return _inventoryCode;
        }

        public void setInventoryCode(String value){
            this._inventoryCode = value;
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


        private String _newAssetCode;

    
        public String getNewAssetCode(){
            return _newAssetCode;
        }

        public void setNewAssetCode(String value){
            this._newAssetCode = value;
        }


        private String _capitalizationCode;

    
        public String getCapitalizationCode(){
            return _capitalizationCode;
        }

        public void setCapitalizationCode(String value){
            this._capitalizationCode = value;
        }


        private String _disposalCode;

    
        public String getDisposalCode(){
            return _disposalCode;
        }

        public void setDisposalCode(String value){
            this._disposalCode = value;
        }


        private Map<String,Object> _inventory;

        public Map<String,Object> getInventory(){
            return _inventory;
        }

        public void setInventory(Map<String,Object> value){
            this._inventory = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _asset;

        public Map<String,Object> getAsset(){
            return _asset;
        }

        public void setAsset(Map<String,Object> value){
            this._asset = value;
        }


        private Map<String,Object> _category;

        public Map<String,Object> getCategory(){
            return _category;
        }

        public void setCategory(Map<String,Object> value){
            this._category = value;
        }


        private Map<String,Object> _newAsset;

        public Map<String,Object> getNewAsset(){
            return _newAsset;
        }

        public void setNewAsset(Map<String,Object> value){
            this._newAsset = value;
        }


        private Map<String,Object> _capitalization;

        public Map<String,Object> getCapitalization(){
            return _capitalization;
        }

        public void setCapitalization(Map<String,Object> value){
            this._capitalization = value;
        }


        private Map<String,Object> _disposal;

        public Map<String,Object> getDisposal(){
            return _disposal;
        }

        public void setDisposal(Map<String,Object> value){
            this._disposal = value;
        }


    }
