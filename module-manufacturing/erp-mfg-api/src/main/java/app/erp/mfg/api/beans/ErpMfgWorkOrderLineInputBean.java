//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgWorkOrderLineInputBean extends CrudInputBase {

    
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


        private String _lineType;

    
        @PropMeta(propId=4)
    
        public String getLineType(){
            return _lineType;
        }

        public void setLineType(String value){
            this._lineType = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=5)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _skuId;

    
        @PropMeta(propId=6)
    
        public Long getSkuId(){
            return _skuId;
        }

        public void setSkuId(Long value){
            this._skuId = value;
        }


        private Long _uoMId;

    
        @PropMeta(propId=7)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
        }


        private java.math.BigDecimal _plannedQuantity;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getPlannedQuantity(){
            return _plannedQuantity;
        }

        public void setPlannedQuantity(java.math.BigDecimal value){
            this._plannedQuantity = value;
        }


        private java.math.BigDecimal _actualQuantity;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getActualQuantity(){
            return _actualQuantity;
        }

        public void setActualQuantity(java.math.BigDecimal value){
            this._actualQuantity = value;
        }


        private java.math.BigDecimal _scrappedQuantity;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getScrappedQuantity(){
            return _scrappedQuantity;
        }

        public void setScrappedQuantity(java.math.BigDecimal value){
            this._scrappedQuantity = value;
        }


        private Long _sourceWarehouseId;

    
        @PropMeta(propId=11)
    
        public Long getSourceWarehouseId(){
            return _sourceWarehouseId;
        }

        public void setSourceWarehouseId(Long value){
            this._sourceWarehouseId = value;
        }


        private Long _destWarehouseId;

    
        @PropMeta(propId=12)
    
        public Long getDestWarehouseId(){
            return _destWarehouseId;
        }

        public void setDestWarehouseId(Long value){
            this._destWarehouseId = value;
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


    }
