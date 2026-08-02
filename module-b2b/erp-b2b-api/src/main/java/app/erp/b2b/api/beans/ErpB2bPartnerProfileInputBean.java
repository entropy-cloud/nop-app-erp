//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bPartnerProfileInputBean extends CrudInputBase {

    
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


        private String _partnerName;

    
        @PropMeta(propId=5)
    
        public String getPartnerName(){
            return _partnerName;
        }

        public void setPartnerName(String value){
            this._partnerName = value;
        }


        private String _status;

    
        @PropMeta(propId=6)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _protocol;

    
        @PropMeta(propId=7)
    
        public String getProtocol(){
            return _protocol;
        }

        public void setProtocol(String value){
            this._protocol = value;
        }


        private String _transportEndpoint;

    
        @PropMeta(propId=8)
    
        public String getTransportEndpoint(){
            return _transportEndpoint;
        }

        public void setTransportEndpoint(String value){
            this._transportEndpoint = value;
        }


        private String _authMethod;

    
        @PropMeta(propId=9)
    
        public String getAuthMethod(){
            return _authMethod;
        }

        public void setAuthMethod(String value){
            this._authMethod = value;
        }


        private String _webhookSecret;

    
        @PropMeta(propId=10)
    
        public String getWebhookSecret(){
            return _webhookSecret;
        }

        public void setWebhookSecret(String value){
            this._webhookSecret = value;
        }


        private java.time.LocalDate _certExpiry;

    
        @PropMeta(propId=11)
    
        public java.time.LocalDate getCertExpiry(){
            return _certExpiry;
        }

        public void setCertExpiry(java.time.LocalDate value){
            this._certExpiry = value;
        }


        private String _certFingerprint;

    
        @PropMeta(propId=12)
    
        public String getCertFingerprint(){
            return _certFingerprint;
        }

        public void setCertFingerprint(String value){
            this._certFingerprint = value;
        }


        private String _allowedFormats;

    
        @PropMeta(propId=13)
    
        public String getAllowedFormats(){
            return _allowedFormats;
        }

        public void setAllowedFormats(String value){
            this._allowedFormats = value;
        }


        private String _timezone;

    
        @PropMeta(propId=14)
    
        public String getTimezone(){
            return _timezone;
        }

        public void setTimezone(String value){
            this._timezone = value;
        }


        private String _contactName;

    
        @PropMeta(propId=15)
    
        public String getContactName(){
            return _contactName;
        }

        public void setContactName(String value){
            this._contactName = value;
        }


        private String _contactEmail;

    
        @PropMeta(propId=16)
    
        public String getContactEmail(){
            return _contactEmail;
        }

        public void setContactEmail(String value){
            this._contactEmail = value;
        }


        private String _contactPhone;

    
        @PropMeta(propId=17)
    
        public String getContactPhone(){
            return _contactPhone;
        }

        public void setContactPhone(String value){
            this._contactPhone = value;
        }


        private String _notes;

    
        @PropMeta(propId=18)
    
        public String getNotes(){
            return _notes;
        }

        public void setNotes(String value){
            this._notes = value;
        }


        private java.time.LocalDate _goLiveDate;

    
        @PropMeta(propId=19)
    
        public java.time.LocalDate getGoLiveDate(){
            return _goLiveDate;
        }

        public void setGoLiveDate(java.time.LocalDate value){
            this._goLiveDate = value;
        }


        private java.sql.Timestamp _archivedAt;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getArchivedAt(){
            return _archivedAt;
        }

        public void setArchivedAt(java.sql.Timestamp value){
            this._archivedAt = value;
        }


    }
