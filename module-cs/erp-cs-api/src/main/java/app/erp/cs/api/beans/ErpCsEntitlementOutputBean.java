//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsEntitlementOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=4)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private String _contractId;

    
        @PropMeta(propId=5)
    
        public String getContractId(){
            return _contractId;
        }

        public void setContractId(String value){
            this._contractId = value;
        }


        private String _slaPolicyId;

    
        @PropMeta(propId=6)
    
        public String getSlaPolicyId(){
            return _slaPolicyId;
        }

        public void setSlaPolicyId(String value){
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


        private String _serviceType_label;

    
        public String getServiceType_label(){
            return _serviceType_label;
        }

        public void setServiceType_label(String value){
            this._serviceType_label = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=17)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=18)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=20)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


        private Map<String,Object> _contract;

        public Map<String,Object> getContract(){
            return _contract;
        }

        public void setContract(Map<String,Object> value){
            this._contract = value;
        }


        private Map<String,Object> _slaPolicy;

        public Map<String,Object> getSlaPolicy(){
            return _slaPolicy;
        }

        public void setSlaPolicy(Map<String,Object> value){
            this._slaPolicy = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
