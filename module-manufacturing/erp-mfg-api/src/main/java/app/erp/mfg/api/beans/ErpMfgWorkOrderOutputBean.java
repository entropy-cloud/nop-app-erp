//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgWorkOrderOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _bomId;

    
        @PropMeta(propId=4)
    
        public Long getBomId(){
            return _bomId;
        }

        public void setBomId(Long value){
            this._bomId = value;
        }


        private Long _routingId;

    
        @PropMeta(propId=5)
    
        public Long getRoutingId(){
            return _routingId;
        }

        public void setRoutingId(Long value){
            this._routingId = value;
        }


        private Long _productionVersionId;

    
        @PropMeta(propId=6)
    
        public Long getProductionVersionId(){
            return _productionVersionId;
        }

        public void setProductionVersionId(Long value){
            this._productionVersionId = value;
        }


        private Long _sourceMrpPlanId;

    
        @PropMeta(propId=7)
    
        public Long getSourceMrpPlanId(){
            return _sourceMrpPlanId;
        }

        public void setSourceMrpPlanId(Long value){
            this._sourceMrpPlanId = value;
        }


        private String _sourceOrderType;

    
        @PropMeta(propId=8)
    
        public String getSourceOrderType(){
            return _sourceOrderType;
        }

        public void setSourceOrderType(String value){
            this._sourceOrderType = value;
        }


        private String _sourceOrderType_label;

    
        public String getSourceOrderType_label(){
            return _sourceOrderType_label;
        }

        public void setSourceOrderType_label(String value){
            this._sourceOrderType_label = value;
        }


        private String _sourceOrderCode;

    
        @PropMeta(propId=9)
    
        public String getSourceOrderCode(){
            return _sourceOrderCode;
        }

        public void setSourceOrderCode(String value){
            this._sourceOrderCode = value;
        }


        private Long _productId;

    
        @PropMeta(propId=10)
    
        public Long getProductId(){
            return _productId;
        }

        public void setProductId(Long value){
            this._productId = value;
        }


        private java.math.BigDecimal _plannedQuantity;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getPlannedQuantity(){
            return _plannedQuantity;
        }

        public void setPlannedQuantity(java.math.BigDecimal value){
            this._plannedQuantity = value;
        }


        private java.math.BigDecimal _completedQuantity;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getCompletedQuantity(){
            return _completedQuantity;
        }

        public void setCompletedQuantity(java.math.BigDecimal value){
            this._completedQuantity = value;
        }


        private java.math.BigDecimal _scrappedQuantity;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getScrappedQuantity(){
            return _scrappedQuantity;
        }

        public void setScrappedQuantity(java.math.BigDecimal value){
            this._scrappedQuantity = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=14)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.time.LocalDate _plannedStartDate;

    
        @PropMeta(propId=15)
    
        public java.time.LocalDate getPlannedStartDate(){
            return _plannedStartDate;
        }

        public void setPlannedStartDate(java.time.LocalDate value){
            this._plannedStartDate = value;
        }


        private java.time.LocalDate _plannedEndDate;

    
        @PropMeta(propId=16)
    
        public java.time.LocalDate getPlannedEndDate(){
            return _plannedEndDate;
        }

        public void setPlannedEndDate(java.time.LocalDate value){
            this._plannedEndDate = value;
        }


        private java.time.LocalDate _actualStartDate;

    
        @PropMeta(propId=17)
    
        public java.time.LocalDate getActualStartDate(){
            return _actualStartDate;
        }

        public void setActualStartDate(java.time.LocalDate value){
            this._actualStartDate = value;
        }


        private java.time.LocalDate _actualEndDate;

    
        @PropMeta(propId=18)
    
        public java.time.LocalDate getActualEndDate(){
            return _actualEndDate;
        }

        public void setActualEndDate(java.time.LocalDate value){
            this._actualEndDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=19)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _materialCost;

    
        @PropMeta(propId=20)
    
        public java.math.BigDecimal getMaterialCost(){
            return _materialCost;
        }

        public void setMaterialCost(java.math.BigDecimal value){
            this._materialCost = value;
        }


        private java.math.BigDecimal _laborCost;

    
        @PropMeta(propId=21)
    
        public java.math.BigDecimal getLaborCost(){
            return _laborCost;
        }

        public void setLaborCost(java.math.BigDecimal value){
            this._laborCost = value;
        }


        private java.math.BigDecimal _overheadCost;

    
        @PropMeta(propId=22)
    
        public java.math.BigDecimal getOverheadCost(){
            return _overheadCost;
        }

        public void setOverheadCost(java.math.BigDecimal value){
            this._overheadCost = value;
        }


        private java.math.BigDecimal _subcontractCost;

    
        @PropMeta(propId=23)
    
        public java.math.BigDecimal getSubcontractCost(){
            return _subcontractCost;
        }

        public void setSubcontractCost(java.math.BigDecimal value){
            this._subcontractCost = value;
        }


        private java.math.BigDecimal _totalCost;

    
        @PropMeta(propId=24)
    
        public java.math.BigDecimal getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(java.math.BigDecimal value){
            this._totalCost = value;
        }


        private java.math.BigDecimal _unitCost;

    
        @PropMeta(propId=25)
    
        public java.math.BigDecimal getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(java.math.BigDecimal value){
            this._unitCost = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=26)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=27)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _approveStatus_label;

    
        public String getApproveStatus_label(){
            return _approveStatus_label;
        }

        public void setApproveStatus_label(String value){
            this._approveStatus_label = value;
        }


        private String _priority;

    
        @PropMeta(propId=28)
    
        public String getPriority(){
            return _priority;
        }

        public void setPriority(String value){
            this._priority = value;
        }


        private String _priority_label;

    
        public String getPriority_label(){
            return _priority_label;
        }

        public void setPriority_label(String value){
            this._priority_label = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=29)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=30)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=31)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private String _remark;

    
        @PropMeta(propId=32)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=33)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=34)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=35)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=36)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=37)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=38)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Long _sourceScheduleId;

    
        @PropMeta(propId=39)
    
        public Long getSourceScheduleId(){
            return _sourceScheduleId;
        }

        public void setSourceScheduleId(Long value){
            this._sourceScheduleId = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=200)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=201)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private String _exchangeRate;

    
        @PropMeta(propId=202)
    
        public String getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(String value){
            this._exchangeRate = value;
        }


        private String _amountSource;

    
        @PropMeta(propId=203)
    
        public String getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(String value){
            this._amountSource = value;
        }


        private String _amountFunctional;

    
        @PropMeta(propId=204)
    
        public String getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(String value){
            this._amountFunctional = value;
        }


        private Map<String,Object> _bom;

        public Map<String,Object> getBom(){
            return _bom;
        }

        public void setBom(Map<String,Object> value){
            this._bom = value;
        }


        private Map<String,Object> _routing;

        public Map<String,Object> getRouting(){
            return _routing;
        }

        public void setRouting(Map<String,Object> value){
            this._routing = value;
        }


        private Map<String,Object> _productionVersion;

        public Map<String,Object> getProductionVersion(){
            return _productionVersion;
        }

        public void setProductionVersion(Map<String,Object> value){
            this._productionVersion = value;
        }


        private Map<String,Object> _product;

        public Map<String,Object> getProduct(){
            return _product;
        }

        public void setProduct(Map<String,Object> value){
            this._product = value;
        }


        private Map<String,Object> _currency;

        public Map<String,Object> getCurrency(){
            return _currency;
        }

        public void setCurrency(Map<String,Object> value){
            this._currency = value;
        }


        private List<Map<String,Object>> _lines;

        public List<Map<String,Object>> getLines(){
            return _lines;
        }

        public void setLines(List<Map<String,Object>> value){
            this._lines = value;
        }


        private List<Map<String,Object>> _jobCards;

        public List<Map<String,Object>> getJobCards(){
            return _jobCards;
        }

        public void setJobCards(List<Map<String,Object>> value){
            this._jobCards = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _sourceMrpPlan;

        public Map<String,Object> getSourceMrpPlan(){
            return _sourceMrpPlan;
        }

        public void setSourceMrpPlan(Map<String,Object> value){
            this._sourceMrpPlan = value;
        }


    }
