//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgMaterialIssueLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _issueId;

    
        @PropMeta(propId=2)
    
        public Long getIssueId(){
            return _issueId;
        }

        public void setIssueId(Long value){
            this._issueId = value;
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


        private Long _skuId;

    
        @PropMeta(propId=5)
    
        public Long getSkuId(){
            return _skuId;
        }

        public void setSkuId(Long value){
            this._skuId = value;
        }


        private Long _uoMId;

    
        @PropMeta(propId=6)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
        }


        private Long _workOrderLineId;

    
        @PropMeta(propId=7)
    
        public Long getWorkOrderLineId(){
            return _workOrderLineId;
        }

        public void setWorkOrderLineId(Long value){
            this._workOrderLineId = value;
        }


        private java.math.BigDecimal _requiredQuantity;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getRequiredQuantity(){
            return _requiredQuantity;
        }

        public void setRequiredQuantity(java.math.BigDecimal value){
            this._requiredQuantity = value;
        }


        private java.math.BigDecimal _issuedQuantity;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getIssuedQuantity(){
            return _issuedQuantity;
        }

        public void setIssuedQuantity(java.math.BigDecimal value){
            this._issuedQuantity = value;
        }


        private java.math.BigDecimal _unitCost;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getUnitCost(){
            return _unitCost;
        }

        public void setUnitCost(java.math.BigDecimal value){
            this._unitCost = value;
        }


        private java.math.BigDecimal _totalCost;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getTotalCost(){
            return _totalCost;
        }

        public void setTotalCost(java.math.BigDecimal value){
            this._totalCost = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=12)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private Long _locationId;

    
        @PropMeta(propId=13)
    
        public Long getLocationId(){
            return _locationId;
        }

        public void setLocationId(Long value){
            this._locationId = value;
        }


        private String _remark;

    
        @PropMeta(propId=14)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
