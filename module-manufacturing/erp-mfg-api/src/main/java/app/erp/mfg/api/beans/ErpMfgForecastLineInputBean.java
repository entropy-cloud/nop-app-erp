//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgForecastLineInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _forecastId;

    
        @PropMeta(propId=2)
    
        public Long getForecastId(){
            return _forecastId;
        }

        public void setForecastId(Long value){
            this._forecastId = value;
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


        private Long _warehouseId;

    
        @PropMeta(propId=5)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _uoMId;

    
        @PropMeta(propId=6)
    
        public Long getUoMId(){
            return _uoMId;
        }

        public void setUoMId(Long value){
            this._uoMId = value;
        }


        private java.time.LocalDate _periodStart;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getPeriodStart(){
            return _periodStart;
        }

        public void setPeriodStart(java.time.LocalDate value){
            this._periodStart = value;
        }


        private java.time.LocalDate _periodEnd;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getPeriodEnd(){
            return _periodEnd;
        }

        public void setPeriodEnd(java.time.LocalDate value){
            this._periodEnd = value;
        }


        private java.math.BigDecimal _forecastQty;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getForecastQty(){
            return _forecastQty;
        }

        public void setForecastQty(java.math.BigDecimal value){
            this._forecastQty = value;
        }


        private String _sourcedFlag;

    
        @PropMeta(propId=10)
    
        public String getSourcedFlag(){
            return _sourcedFlag;
        }

        public void setSourcedFlag(String value){
            this._sourcedFlag = value;
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
