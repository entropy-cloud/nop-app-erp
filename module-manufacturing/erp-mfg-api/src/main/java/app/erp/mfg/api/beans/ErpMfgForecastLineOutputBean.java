//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgForecastLineOutputBean {

    
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


        private Integer _version;

    
        @PropMeta(propId=13)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=14)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=16)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _forecastCode;

    
        public String getForecastCode(){
            return _forecastCode;
        }

        public void setForecastCode(String value){
            this._forecastCode = value;
        }


        private String _materialName;

    
        public String getMaterialName(){
            return _materialName;
        }

        public void setMaterialName(String value){
            this._materialName = value;
        }


        private String _warehouseName;

    
        public String getWarehouseName(){
            return _warehouseName;
        }

        public void setWarehouseName(String value){
            this._warehouseName = value;
        }


        private String _uomName;

    
        public String getUomName(){
            return _uomName;
        }

        public void setUomName(String value){
            this._uomName = value;
        }


        private Map<String,Object> _forecast;

        public Map<String,Object> getForecast(){
            return _forecast;
        }

        public void setForecast(Map<String,Object> value){
            this._forecast = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _warehouse;

        public Map<String,Object> getWarehouse(){
            return _warehouse;
        }

        public void setWarehouse(Map<String,Object> value){
            this._warehouse = value;
        }


        private Map<String,Object> _uoM;

        public Map<String,Object> getUoM(){
            return _uoM;
        }

        public void setUoM(Map<String,Object> value){
            this._uoM = value;
        }


    }
