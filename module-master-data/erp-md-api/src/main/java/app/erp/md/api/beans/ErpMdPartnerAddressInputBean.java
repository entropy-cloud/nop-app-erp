//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdPartnerAddressInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _partnerId;

    
        @PropMeta(propId=2)
    
        public Long getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(Long value){
            this._partnerId = value;
        }


        private String _addressType;

    
        @PropMeta(propId=3)
    
        public String getAddressType(){
            return _addressType;
        }

        public void setAddressType(String value){
            this._addressType = value;
        }


        private String _contactPerson;

    
        @PropMeta(propId=4)
    
        public String getContactPerson(){
            return _contactPerson;
        }

        public void setContactPerson(String value){
            this._contactPerson = value;
        }


        private String _phone;

    
        @PropMeta(propId=5)
    
        public String getPhone(){
            return _phone;
        }

        public void setPhone(String value){
            this._phone = value;
        }


        private String _address;

    
        @PropMeta(propId=6)
    
        public String getAddress(){
            return _address;
        }

        public void setAddress(String value){
            this._address = value;
        }


        private Boolean _isDefault;

    
        @PropMeta(propId=7)
    
        public Boolean getIsDefault(){
            return _isDefault;
        }

        public void setIsDefault(Boolean value){
            this._isDefault = value;
        }


    }
