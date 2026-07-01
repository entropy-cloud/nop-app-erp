//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgCostVarianceOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _workOrderId;

    
        @PropMeta(propId=2)
    
        public Long getWorkOrderId(){
            return _workOrderId;
        }

        public void setWorkOrderId(Long value){
            this._workOrderId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _varianceType;

    
        @PropMeta(propId=4)
    
        public String getVarianceType(){
            return _varianceType;
        }

        public void setVarianceType(String value){
            this._varianceType = value;
        }


        private String _costElement;

    
        @PropMeta(propId=5)
    
        public String getCostElement(){
            return _costElement;
        }

        public void setCostElement(String value){
            this._costElement = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=6)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _operationId;

    
        @PropMeta(propId=7)
    
        public Long getOperationId(){
            return _operationId;
        }

        public void setOperationId(Long value){
            this._operationId = value;
        }


        private String _standardAmount;

    
        @PropMeta(propId=8)
    
        public String getStandardAmount(){
            return _standardAmount;
        }

        public void setStandardAmount(String value){
            this._standardAmount = value;
        }


        private String _actualAmount;

    
        @PropMeta(propId=9)
    
        public String getActualAmount(){
            return _actualAmount;
        }

        public void setActualAmount(String value){
            this._actualAmount = value;
        }


        private String _varianceAmount;

    
        @PropMeta(propId=10)
    
        public String getVarianceAmount(){
            return _varianceAmount;
        }

        public void setVarianceAmount(String value){
            this._varianceAmount = value;
        }


        private String _variancePercent;

    
        @PropMeta(propId=11)
    
        public String getVariancePercent(){
            return _variancePercent;
        }

        public void setVariancePercent(String value){
            this._variancePercent = value;
        }


        private String _standardQty;

    
        @PropMeta(propId=12)
    
        public String getStandardQty(){
            return _standardQty;
        }

        public void setStandardQty(String value){
            this._standardQty = value;
        }


        private String _actualQty;

    
        @PropMeta(propId=13)
    
        public String getActualQty(){
            return _actualQty;
        }

        public void setActualQty(String value){
            this._actualQty = value;
        }


        private String _standardPrice;

    
        @PropMeta(propId=14)
    
        public String getStandardPrice(){
            return _standardPrice;
        }

        public void setStandardPrice(String value){
            this._standardPrice = value;
        }


        private String _actualPrice;

    
        @PropMeta(propId=15)
    
        public String getActualPrice(){
            return _actualPrice;
        }

        public void setActualPrice(String value){
            this._actualPrice = value;
        }


        private Long _workcenterId;

    
        @PropMeta(propId=16)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=17)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=18)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private String _remark;

    
        @PropMeta(propId=19)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=20)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=21)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=22)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=24)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=25)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _workOrder;

        public Map<String,Object> getWorkOrder(){
            return _workOrder;
        }

        public void setWorkOrder(Map<String,Object> value){
            this._workOrder = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _workcenter;

        public Map<String,Object> getWorkcenter(){
            return _workcenter;
        }

        public void setWorkcenter(Map<String,Object> value){
            this._workcenter = value;
        }


        private Map<String,Object> _operation;

        public Map<String,Object> getOperation(){
            return _operation;
        }

        public void setOperation(Map<String,Object> value){
            this._operation = value;
        }


    }
