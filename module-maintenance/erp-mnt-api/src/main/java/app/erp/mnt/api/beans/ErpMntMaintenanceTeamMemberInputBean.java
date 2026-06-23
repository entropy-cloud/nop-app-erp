//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntMaintenanceTeamMemberInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _teamId;

    
        @PropMeta(propId=2)
    
        public Long getTeamId(){
            return _teamId;
        }

        public void setTeamId(Long value){
            this._teamId = value;
        }


        private Long _employeeId;

    
        @PropMeta(propId=3)
    
        public Long getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(Long value){
            this._employeeId = value;
        }


        private String _role;

    
        @PropMeta(propId=4)
    
        public String getRole(){
            return _role;
        }

        public void setRole(String value){
            this._role = value;
        }


        private java.time.LocalDateTime _joinedAt;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDateTime getJoinedAt(){
            return _joinedAt;
        }

        public void setJoinedAt(java.time.LocalDateTime value){
            this._joinedAt = value;
        }


        private java.time.LocalDateTime _leftAt;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDateTime getLeftAt(){
            return _leftAt;
        }

        public void setLeftAt(java.time.LocalDateTime value){
            this._leftAt = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=7)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
