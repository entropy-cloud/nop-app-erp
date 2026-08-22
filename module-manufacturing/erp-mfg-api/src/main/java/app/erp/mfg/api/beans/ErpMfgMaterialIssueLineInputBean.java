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

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _issueId;

    
        @PropMeta(propId=2)
    
        public String getIssueId(){
            return _issueId;
        }

        public void setIssueId(String value){
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


        private String _materialId;

    
        @PropMeta(propId=4)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _skuId;

    
        @PropMeta(propId=5)
    
        public String getSkuId(){
            return _skuId;
        }

        public void setSkuId(String value){
            this._skuId = value;
        }


        private String _uoMId;

    
        @PropMeta(propId=6)
    
        public String getUoMId(){
            return _uoMId;
        }

        public void setUoMId(String value){
            this._uoMId = value;
        }


        private String _workOrderLineId;

    
        @PropMeta(propId=7)
    
        public String getWorkOrderLineId(){
            return _workOrderLineId;
        }

        public void setWorkOrderLineId(String value){
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


        private String _locationId;

    
        @PropMeta(propId=13)
    
        public String getLocationId(){
            return _locationId;
        }

        public void setLocationId(String value){
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


    }
