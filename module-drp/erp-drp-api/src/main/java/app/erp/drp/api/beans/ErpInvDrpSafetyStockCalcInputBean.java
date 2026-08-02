//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvDrpSafetyStockCalcInputBean extends CrudInputBase {

    
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


        private String _method;

    
        @PropMeta(propId=6)
    
        public String getMethod(){
            return _method;
        }

        public void setMethod(String value){
            this._method = value;
        }


        private String _serviceLevel;

    
        @PropMeta(propId=7)
    
        public String getServiceLevel(){
            return _serviceLevel;
        }

        public void setServiceLevel(String value){
            this._serviceLevel = value;
        }


        private Integer _historyMonths;

    
        @PropMeta(propId=8)
    
        public Integer getHistoryMonths(){
            return _historyMonths;
        }

        public void setHistoryMonths(Integer value){
            this._historyMonths = value;
        }


        private Integer _leadTimeDays;

    
        @PropMeta(propId=9)
    
        public Integer getLeadTimeDays(){
            return _leadTimeDays;
        }

        public void setLeadTimeDays(Integer value){
            this._leadTimeDays = value;
        }


        private java.math.BigDecimal _calculatedSafetyStock;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getCalculatedSafetyStock(){
            return _calculatedSafetyStock;
        }

        public void setCalculatedSafetyStock(java.math.BigDecimal value){
            this._calculatedSafetyStock = value;
        }


        private java.math.BigDecimal _calculatedRop;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getCalculatedRop(){
            return _calculatedRop;
        }

        public void setCalculatedRop(java.math.BigDecimal value){
            this._calculatedRop = value;
        }


        private java.math.BigDecimal _overrideSafetyStock;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getOverrideSafetyStock(){
            return _overrideSafetyStock;
        }

        public void setOverrideSafetyStock(java.math.BigDecimal value){
            this._overrideSafetyStock = value;
        }


        private java.sql.Timestamp _lastCalculatedAt;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getLastCalculatedAt(){
            return _lastCalculatedAt;
        }

        public void setLastCalculatedAt(java.sql.Timestamp value){
            this._lastCalculatedAt = value;
        }


        private String _overwrittenBy;

    
        @PropMeta(propId=14)
    
        public String getOverwrittenBy(){
            return _overwrittenBy;
        }

        public void setOverwrittenBy(String value){
            this._overwrittenBy = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
