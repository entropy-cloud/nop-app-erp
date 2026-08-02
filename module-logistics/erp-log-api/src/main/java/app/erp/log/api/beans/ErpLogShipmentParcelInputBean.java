//__XGEN_FORCE_OVERRIDE__
    package app.erp.log.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpLogShipmentParcelInputBean extends CrudInputBase {

    
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


        private String _parcelNo;

    
        @PropMeta(propId=3)
    
        public String getParcelNo(){
            return _parcelNo;
        }

        public void setParcelNo(String value){
            this._parcelNo = value;
        }


        private String _trackingNo;

    
        @PropMeta(propId=4)
    
        public String getTrackingNo(){
            return _trackingNo;
        }

        public void setTrackingNo(String value){
            this._trackingNo = value;
        }


        private String _labelUrl;

    
        @PropMeta(propId=5)
    
        public String getLabelUrl(){
            return _labelUrl;
        }

        public void setLabelUrl(String value){
            this._labelUrl = value;
        }


        private java.math.BigDecimal _weight;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getWeight(){
            return _weight;
        }

        public void setWeight(java.math.BigDecimal value){
            this._weight = value;
        }


        private java.math.BigDecimal _length;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getLength(){
            return _length;
        }

        public void setLength(java.math.BigDecimal value){
            this._length = value;
        }


        private java.math.BigDecimal _width;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getWidth(){
            return _width;
        }

        public void setWidth(java.math.BigDecimal value){
            this._width = value;
        }


        private java.math.BigDecimal _height;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getHeight(){
            return _height;
        }

        public void setHeight(java.math.BigDecimal value){
            this._height = value;
        }


        private java.math.BigDecimal _declaredValue;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getDeclaredValue(){
            return _declaredValue;
        }

        public void setDeclaredValue(java.math.BigDecimal value){
            this._declaredValue = value;
        }


        private Integer _isActive;

    
        @PropMeta(propId=11)
    
        public Integer getIsActive(){
            return _isActive;
        }

        public void setIsActive(Integer value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
