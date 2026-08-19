//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvDrpLeadTimeRecordOutputBean {

    
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


        private Long _supplierId;

    
        @PropMeta(propId=3)
    
        public Long getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(Long value){
            this._supplierId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=4)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private java.time.LocalDate _orderDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getOrderDate(){
            return _orderDate;
        }

        public void setOrderDate(java.time.LocalDate value){
            this._orderDate = value;
        }


        private java.time.LocalDate _receiptDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getReceiptDate(){
            return _receiptDate;
        }

        public void setReceiptDate(java.time.LocalDate value){
            this._receiptDate = value;
        }


        private Integer _actualLeadTime;

    
        @PropMeta(propId=7)
    
        public Integer getActualLeadTime(){
            return _actualLeadTime;
        }

        public void setActualLeadTime(Integer value){
            this._actualLeadTime = value;
        }


        private Integer _expectedLeadTime;

    
        @PropMeta(propId=8)
    
        public Integer getExpectedLeadTime(){
            return _expectedLeadTime;
        }

        public void setExpectedLeadTime(Integer value){
            this._expectedLeadTime = value;
        }


        private Integer _varianceDays;

    
        @PropMeta(propId=9)
    
        public Integer getVarianceDays(){
            return _varianceDays;
        }

        public void setVarianceDays(Integer value){
            this._varianceDays = value;
        }


        private String _purchaseOrderCode;

    
        @PropMeta(propId=10)
    
        public String getPurchaseOrderCode(){
            return _purchaseOrderCode;
        }

        public void setPurchaseOrderCode(String value){
            this._purchaseOrderCode = value;
        }


        private Boolean _isOnTime;

    
        @PropMeta(propId=11)
    
        public Boolean getIsOnTime(){
            return _isOnTime;
        }

        public void setIsOnTime(Boolean value){
            this._isOnTime = value;
        }


        private String _earlyLateFlag;

    
        @PropMeta(propId=12)
    
        public String getEarlyLateFlag(){
            return _earlyLateFlag;
        }

        public void setEarlyLateFlag(String value){
            this._earlyLateFlag = value;
        }


        private String _earlyLateFlag_label;

    
        public String getEarlyLateFlag_label(){
            return _earlyLateFlag_label;
        }

        public void setEarlyLateFlag_label(String value){
            this._earlyLateFlag_label = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
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


        private Map<String,Object> _supplier;

        public Map<String,Object> getSupplier(){
            return _supplier;
        }

        public void setSupplier(Map<String,Object> value){
            this._supplier = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
