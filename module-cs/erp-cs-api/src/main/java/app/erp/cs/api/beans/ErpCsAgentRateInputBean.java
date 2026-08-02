//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsAgentRateInputBean extends CrudInputBase {

    
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


        private Long _agentId;

    
        @PropMeta(propId=3)
    
        public Long getAgentId(){
            return _agentId;
        }

        public void setAgentId(Long value){
            this._agentId = value;
        }


        private String _serviceType;

    
        @PropMeta(propId=4)
    
        public String getServiceType(){
            return _serviceType;
        }

        public void setServiceType(String value){
            this._serviceType = value;
        }


        private java.math.BigDecimal _rate;

    
        @PropMeta(propId=5)
    
        public java.math.BigDecimal getRate(){
            return _rate;
        }

        public void setRate(java.math.BigDecimal value){
            this._rate = value;
        }


        private java.time.LocalDate _effectiveDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getEffectiveDate(){
            return _effectiveDate;
        }

        public void setEffectiveDate(java.time.LocalDate value){
            this._effectiveDate = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=7)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


    }
