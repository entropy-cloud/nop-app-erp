//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvCostLayerOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=3)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _skuId;

    
        @PropMeta(propId=4)
    
        public Long getSkuId(){
            return _skuId;
        }

        public void setSkuId(Long value){
            this._skuId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=5)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=6)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private Integer _costMethod;

    
        @PropMeta(propId=7)
    
        public Integer getCostMethod(){
            return _costMethod;
        }

        public void setCostMethod(Integer value){
            this._costMethod = value;
        }


        private String _costMethod_label;

    
        public String getCostMethod_label(){
            return _costMethod_label;
        }

        public void setCostMethod_label(String value){
            this._costMethod_label = value;
        }


        private String _incomingQuantity;

    
        @PropMeta(propId=8)
    
        public String getIncomingQuantity(){
            return _incomingQuantity;
        }

        public void setIncomingQuantity(String value){
            this._incomingQuantity = value;
        }


        private String _remainingQuantity;

    
        @PropMeta(propId=9)
    
        public String getRemainingQuantity(){
            return _remainingQuantity;
        }

        public void setRemainingQuantity(String value){
            this._remainingQuantity = value;
        }


        private String _unitCost;

    
        @PropMeta(propId=10)
    
        public String getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(String value){
            this._unitCost = value;
        }


        private String _totalCost;

    
        @PropMeta(propId=11)
    
        public String getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(String value){
            this._totalCost = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=12)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.time.LocalDate _incomingDate;

    
        @PropMeta(propId=13)
    
        public java.time.LocalDate getIncomingDate(){
            return _incomingDate;
        }

        public void setIncomingDate(java.time.LocalDate value){
            this._incomingDate = value;
        }


        private Long _incomingMoveId;

    
        @PropMeta(propId=14)
    
        public Long getIncomingMoveId(){
            return _incomingMoveId;
        }

        public void setIncomingMoveId(Long value){
            this._incomingMoveId = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=15)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=17)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=18)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=20)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _sku;

        public Map<String,Object> getSku(){
            return _sku;
        }

        public void setSku(Map<String,Object> value){
            this._sku = value;
        }


        private Map<String,Object> _warehouse;

        public Map<String,Object> getWarehouse(){
            return _warehouse;
        }

        public void setWarehouse(Map<String,Object> value){
            this._warehouse = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _incomingMove;

        public Map<String,Object> getIncomingMove(){
            return _incomingMove;
        }

        public void setIncomingMove(Map<String,Object> value){
            this._incomingMove = value;
        }


        private Map<String,Object> _acctSchema;

        public Map<String,Object> getAcctSchema(){
            return _acctSchema;
        }

        public void setAcctSchema(Map<String,Object> value){
            this._acctSchema = value;
        }


    }
