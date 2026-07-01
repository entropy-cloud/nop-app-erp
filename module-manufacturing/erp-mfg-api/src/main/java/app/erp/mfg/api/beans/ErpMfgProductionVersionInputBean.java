//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgProductionVersionInputBean extends CrudInputBase {

    
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


        private Long _productId;

    
        @PropMeta(propId=3)
    
        public Long getProductId(){
            return _productId;
        }

        public void setProductId(Long value){
            this._productId = value;
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


        private java.time.LocalDate _validFrom;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getValidFrom(){
            return _validFrom;
        }

        public void setValidFrom(java.time.LocalDate value){
            this._validFrom = value;
        }


        private java.time.LocalDate _validTo;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getValidTo(){
            return _validTo;
        }

        public void setValidTo(java.time.LocalDate value){
            this._validTo = value;
        }


        private java.math.BigDecimal _lotSizeFrom;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getLotSizeFrom(){
            return _lotSizeFrom;
        }

        public void setLotSizeFrom(java.math.BigDecimal value){
            this._lotSizeFrom = value;
        }


        private java.math.BigDecimal _lotSizeTo;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getLotSizeTo(){
            return _lotSizeTo;
        }

        public void setLotSizeTo(java.math.BigDecimal value){
            this._lotSizeTo = value;
        }


        private Boolean _isDefault;

    
        @PropMeta(propId=10)
    
        public Boolean getIsDefault(){
            return _isDefault;
        }

        public void setIsDefault(Boolean value){
            this._isDefault = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=11)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
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


        private Long _delVersion;

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
