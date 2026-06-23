//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvPickingOrderLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _pickingId;

    
        @PropMeta(propId=2)
    
        public Long getPickingId(){
            return _pickingId;
        }

        public void setPickingId(Long value){
            this._pickingId = value;
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


        private Long _sourceLocationId;

    
        @PropMeta(propId=7)
    
        public Long getSourceLocationId(){
            return _sourceLocationId;
        }

        public void setSourceLocationId(Long value){
            this._sourceLocationId = value;
        }


        private String _quantity;

    
        @PropMeta(propId=8)
    
        public String getQuantity(){
            return _quantity;
        }

        public void setQuantity(String value){
            this._quantity = value;
        }


        private String _pickedQuantity;

    
        @PropMeta(propId=9)
    
        public String getPickedQuantity(){
            return _pickedQuantity;
        }

        public void setPickedQuantity(String value){
            this._pickedQuantity = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=10)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
