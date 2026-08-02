//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsSlaPolicyOutputBean {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private Long _ticketTypeId;

    
        @PropMeta(propId=4)
    
        public Long getTicketTypeId(){
            return _ticketTypeId;
        }

        public void setTicketTypeId(Long value){
            this._ticketTypeId = value;
        }


        private String _minPriority;

    
        @PropMeta(propId=5)
    
        public String getMinPriority(){
            return _minPriority;
        }

        public void setMinPriority(String value){
            this._minPriority = value;
        }


        private String _minPriority_label;

    
        public String getMinPriority_label(){
            return _minPriority_label;
        }

        public void setMinPriority_label(String value){
            this._minPriority_label = value;
        }


        private Long _teamId;

    
        @PropMeta(propId=6)
    
        public Long getTeamId(){
            return _teamId;
        }

        public void setTeamId(Long value){
            this._teamId = value;
        }


        private Integer _resolveHours;

    
        @PropMeta(propId=7)
    
        public Integer getResolveHours(){
            return _resolveHours;
        }

        public void setResolveHours(Integer value){
            this._resolveHours = value;
        }


        private Integer _resolveDays;

    
        @PropMeta(propId=8)
    
        public Integer getResolveDays(){
            return _resolveDays;
        }

        public void setResolveDays(Integer value){
            this._resolveDays = value;
        }


        private Boolean _isWorkingDays;

    
        @PropMeta(propId=9)
    
        public Boolean getIsWorkingDays(){
            return _isWorkingDays;
        }

        public void setIsWorkingDays(Boolean value){
            this._isWorkingDays = value;
        }


        private Long _escalationUserId;

    
        @PropMeta(propId=10)
    
        public Long getEscalationUserId(){
            return _escalationUserId;
        }

        public void setEscalationUserId(Long value){
            this._escalationUserId = value;
        }


        private String _description;

    
        @PropMeta(propId=11)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
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


        private Map<String,Object> _ticketType;

        public Map<String,Object> getTicketType(){
            return _ticketType;
        }

        public void setTicketType(Map<String,Object> value){
            this._ticketType = value;
        }


        private Map<String,Object> _team;

        public Map<String,Object> getTeam(){
            return _team;
        }

        public void setTeam(Map<String,Object> value){
            this._team = value;
        }


    }
