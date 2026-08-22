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

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _teamId;

    
        @PropMeta(propId=2)
    
        public String getTeamId(){
            return _teamId;
        }

        public void setTeamId(String value){
            this._teamId = value;
        }


        private String _employeeId;

    
        @PropMeta(propId=3)
    
        public String getEmployeeId(){
            return _employeeId;
        }

        public void setEmployeeId(String value){
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


        private java.sql.Timestamp _joinedAt;

    
        @PropMeta(propId=5)
    
        public java.sql.Timestamp getJoinedAt(){
            return _joinedAt;
        }

        public void setJoinedAt(java.sql.Timestamp value){
            this._joinedAt = value;
        }


        private java.sql.Timestamp _leftAt;

    
        @PropMeta(propId=6)
    
        public java.sql.Timestamp getLeftAt(){
            return _leftAt;
        }

        public void setLeftAt(java.sql.Timestamp value){
            this._leftAt = value;
        }


    }
