//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsEntitlementInputBean extends CrudInputBase {

    
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


        private Long _partnerId;

    
        @PropMeta(propId=4)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private Long _contractId;

    
        @PropMeta(propId=5)
    
        public Long getContractId(){
            return _contractId;
        }

        public void setContractId(Long value){
            this._contractId = value;
        }


        private Long _slaPolicyId;

    
        @PropMeta(propId=6)
    
        public Long getSlaPolicyId(){
            return _slaPolicyId;
        }

        public void setSlaPolicyId(Long value){
            this._slaPolicyId = value;
        }


        private String _serviceType;

    
        @PropMeta(propId=7)
    
        public String getServiceType(){
            return _serviceType;
        }

        public void setServiceType(String value){
            this._serviceType = value;
        }


        private java.time.LocalDate _startDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getStartDate(){
            return _startDate;
        }

        public void setStartDate(java.time.LocalDate value){
            this._startDate = value;
        }


        private java.time.LocalDate _endDate;

    
        @PropMeta(propId=9)
    
        public java.time.LocalDate getEndDate(){
            return _endDate;
        }

        public void setEndDate(java.time.LocalDate value){
            this._endDate = value;
        }


        private Integer _maxTickets;

    
        @PropMeta(propId=10)
    
        public Integer getMaxTickets(){
            return _maxTickets;
        }

        public void setMaxTickets(Integer value){
            this._maxTickets = value;
        }


        private Integer _usedTickets;

    
        @PropMeta(propId=11)
    
        public Integer getUsedTickets(){
            return _usedTickets;
        }

        public void setUsedTickets(Integer value){
            this._usedTickets = value;
        }


        private Integer _maxResponseTime;

    
        @PropMeta(propId=12)
    
        public Integer getMaxResponseTime(){
            return _maxResponseTime;
        }

        public void setMaxResponseTime(Integer value){
            this._maxResponseTime = value;
        }


        private Integer _maxResolutionTime;

    
        @PropMeta(propId=13)
    
        public Integer getMaxResolutionTime(){
            return _maxResolutionTime;
        }

        public void setMaxResolutionTime(Integer value){
            this._maxResolutionTime = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=14)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _notes;

    
        @PropMeta(propId=15)
    
        public String getNotes(){
            return _notes;
        }

        public void setNotes(String value){
            this._notes = value;
        }


    }
