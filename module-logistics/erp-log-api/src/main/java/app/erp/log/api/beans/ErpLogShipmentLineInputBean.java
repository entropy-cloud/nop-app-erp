//__XGEN_FORCE_OVERRIDE__
    package app.erp.log.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpLogShipmentLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _shipmentId;

    
        @PropMeta(propId=2)
    
        public Long getShipmentId(){
            return _shipmentId;
        }

        public void setShipmentId(Long value){
            this._shipmentId = value;
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


        private java.math.BigDecimal _quantity;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getQuantity(){
            return _quantity;
        }

        public void setQuantity(java.math.BigDecimal value){
            this._quantity = value;
        }


        private String _unit;

    
        @PropMeta(propId=6)
    
        public String getUnit(){
            return _unit;
        }

        public void setUnit(String value){
            this._unit = value;
        }


        private String _packageDescription;

    
        @PropMeta(propId=7)
    
        public String getPackageDescription(){
            return _packageDescription;
        }

        public void setPackageDescription(String value){
            this._packageDescription = value;
        }


        private String _remark;

    
        @PropMeta(propId=8)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
