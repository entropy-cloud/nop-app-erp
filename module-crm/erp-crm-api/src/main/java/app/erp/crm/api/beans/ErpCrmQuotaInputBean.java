//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmQuotaInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=2)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _territoryId;

    
        @PropMeta(propId=3)
    
        public Long getTerritoryId(){
            return _territoryId;
        }

        public void setTerritoryId(Long value){
            this._territoryId = value;
        }


        private Long _teamId;

    
        @PropMeta(propId=4)
    
        public Long getTeamId(){
            return _teamId;
        }

        public void setTeamId(Long value){
            this._teamId = value;
        }


        private String _ownerId;

    
        @PropMeta(propId=5)
    
        public String getOwnerId(){
            return _ownerId;
        }

        public void setOwnerId(String value){
            this._ownerId = value;
        }


        private String _periodType;

    
        @PropMeta(propId=6)
    
        public String getPeriodType(){
            return _periodType;
        }

        public void setPeriodType(String value){
            this._periodType = value;
        }


        private Integer _fiscalYear;

    
        @PropMeta(propId=7)
    
        public Integer getFiscalYear(){
            return _fiscalYear;
        }

        public void setFiscalYear(Integer value){
            this._fiscalYear = value;
        }


        private String _periodLabel;

    
        @PropMeta(propId=8)
    
        public String getPeriodLabel(){
            return _periodLabel;
        }

        public void setPeriodLabel(String value){
            this._periodLabel = value;
        }


        private java.math.BigDecimal _quotaAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getQuotaAmount(){
            return _quotaAmount;
        }

        public void setQuotaAmount(java.math.BigDecimal value){
            this._quotaAmount = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=10)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private Boolean _isFinalized;

    
        @PropMeta(propId=11)
    
        public Boolean getIsFinalized(){
            return _isFinalized;
        }

        public void setIsFinalized(Boolean value){
            this._isFinalized = value;
        }


        private String _notes;

    
        @PropMeta(propId=12)
    
        public String getNotes(){
            return _notes;
        }

        public void setNotes(String value){
            this._notes = value;
        }


    }
