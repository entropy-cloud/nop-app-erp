//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdEmployeeOutputBean {

    
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


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _position;

    
        @PropMeta(propId=5)
    
        public String getPosition(){
            return _position;
        }

        public void setPosition(String value){
            this._position = value;
        }


        private String _phone;

    
        @PropMeta(propId=6)
    
        public String getPhone(){
            return _phone;
        }

        public void setPhone(String value){
            this._phone = value;
        }


        private String _email;

    
        @PropMeta(propId=7)
    
        public String getEmail(){
            return _email;
        }

        public void setEmail(String value){
            this._email = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=8)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private String _status;

    
        @PropMeta(propId=9)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=10)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=11)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=12)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=14)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _orgName;

    
        public String getOrgName(){
            return _orgName;
        }

        public void setOrgName(String value){
            this._orgName = value;
        }


        private String _partnerName;

    
        public String getPartnerName(){
            return _partnerName;
        }

        public void setPartnerName(String value){
            this._partnerName = value;
        }


        private Map<String,Object> _organization;

        public Map<String,Object> getOrganization(){
            return _organization;
        }

        public void setOrganization(Map<String,Object> value){
            this._organization = value;
        }


        private Map<String,Object> _partner;

        public Map<String,Object> getPartner(){
            return _partner;
        }

        public void setPartner(Map<String,Object> value){
            this._partner = value;
        }


    }
