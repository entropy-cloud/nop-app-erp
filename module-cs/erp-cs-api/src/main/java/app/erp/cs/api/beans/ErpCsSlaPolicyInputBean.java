//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsSlaPolicyInputBean extends CrudInputBase {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private String _ticketTypeId;

    
        @PropMeta(propId=4)
    
        public String getTicketTypeId(){
            return _ticketTypeId;
        }

        public void setTicketTypeId(String value){
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


        private String _teamId;

    
        @PropMeta(propId=6)
    
        public String getTeamId(){
            return _teamId;
        }

        public void setTeamId(String value){
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


        private String _escalationUserId;

    
        @PropMeta(propId=10)
    
        public String getEscalationUserId(){
            return _escalationUserId;
        }

        public void setEscalationUserId(String value){
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


        private String _secondEscalationUserId;

    
        @PropMeta(propId=18)
    
        public String getSecondEscalationUserId(){
            return _secondEscalationUserId;
        }

        public void setSecondEscalationUserId(String value){
            this._secondEscalationUserId = value;
        }


        private Integer _escalationDelayHours;

    
        @PropMeta(propId=19)
    
        public Integer getEscalationDelayHours(){
            return _escalationDelayHours;
        }

        public void setEscalationDelayHours(Integer value){
            this._escalationDelayHours = value;
        }


    }
