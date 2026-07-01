//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgMrpPlanLineOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _mrpPlanId;

    
        @PropMeta(propId=2)
    
        public Long getMrpPlanId(){
            return _mrpPlanId;
        }

        public void setMrpPlanId(Long value){
            this._mrpPlanId = value;
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


        private Integer _orderType;

    
        @PropMeta(propId=6)
    
        public Integer getOrderType(){
            return _orderType;
        }

        public void setOrderType(Integer value){
            this._orderType = value;
        }


        private String _orderType_label;

    
        public String getOrderType_label(){
            return _orderType_label;
        }

        public void setOrderType_label(String value){
            this._orderType_label = value;
        }


        private String _grossRequirement;

    
        @PropMeta(propId=7)
    
        public String getGrossRequirement(){
            return _grossRequirement;
        }

        public void setGrossRequirement(String value){
            this._grossRequirement = value;
        }


        private String _scheduledReceipt;

    
        @PropMeta(propId=8)
    
        public String getScheduledReceipt(){
            return _scheduledReceipt;
        }

        public void setScheduledReceipt(String value){
            this._scheduledReceipt = value;
        }


        private String _onHand;

    
        @PropMeta(propId=9)
    
        public String getOnHand(){
            return _onHand;
        }

        public void setOnHand(String value){
            this._onHand = value;
        }


        private String _netRequirement;

    
        @PropMeta(propId=10)
    
        public String getNetRequirement(){
            return _netRequirement;
        }

        public void setNetRequirement(String value){
            this._netRequirement = value;
        }


        private String _plannedQuantity;

    
        @PropMeta(propId=11)
    
        public String getPlannedQuantity(){
            return _plannedQuantity;
        }

        public void setPlannedQuantity(String value){
            this._plannedQuantity = value;
        }


        private java.time.LocalDate _plannedDate;

    
        @PropMeta(propId=12)
    
        public java.time.LocalDate getPlannedDate(){
            return _plannedDate;
        }

        public void setPlannedDate(java.time.LocalDate value){
            this._plannedDate = value;
        }


        private Long _parentLineId;

    
        @PropMeta(propId=13)
    
        public Long getParentLineId(){
            return _parentLineId;
        }

        public void setParentLineId(Long value){
            this._parentLineId = value;
        }


        private Boolean _isFirmed;

    
        @PropMeta(propId=14)
    
        public Boolean getIsFirmed(){
            return _isFirmed;
        }

        public void setIsFirmed(Boolean value){
            this._isFirmed = value;
        }


        private String _convertedBillCode;

    
        @PropMeta(propId=15)
    
        public String getConvertedBillCode(){
            return _convertedBillCode;
        }

        public void setConvertedBillCode(String value){
            this._convertedBillCode = value;
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


        private Map<String,Object> _mrpPlan;

        public Map<String,Object> getMrpPlan(){
            return _mrpPlan;
        }

        public void setMrpPlan(Map<String,Object> value){
            this._mrpPlan = value;
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


        private Map<String,Object> _parentLine;

        public Map<String,Object> getParentLine(){
            return _parentLine;
        }

        public void setParentLine(Map<String,Object> value){
            this._parentLine = value;
        }


    }
