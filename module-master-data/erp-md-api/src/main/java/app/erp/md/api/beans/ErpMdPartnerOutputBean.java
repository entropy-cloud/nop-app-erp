//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdPartnerOutputBean {

    
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


        private String _partnerType;

    
        @PropMeta(propId=4)
    
        public String getPartnerType(){
            return _partnerType;
        }

        public void setPartnerType(String value){
            this._partnerType = value;
        }


        private String _partnerType_label;

    
        public String getPartnerType_label(){
            return _partnerType_label;
        }

        public void setPartnerType_label(String value){
            this._partnerType_label = value;
        }


        private String _status;

    
        @PropMeta(propId=5)
    
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


        private String _contactPerson;

    
        @PropMeta(propId=6)
    
        public String getContactPerson(){
            return _contactPerson;
        }

        public void setContactPerson(String value){
            this._contactPerson = value;
        }


        private String _phone;

    
        @PropMeta(propId=7)
    
        public String getPhone(){
            return _phone;
        }

        public void setPhone(String value){
            this._phone = value;
        }


        private String _email;

    
        @PropMeta(propId=8)
    
        public String getEmail(){
            return _email;
        }

        public void setEmail(String value){
            this._email = value;
        }


        private String _address;

    
        @PropMeta(propId=9)
    
        public String getAddress(){
            return _address;
        }

        public void setAddress(String value){
            this._address = value;
        }


        private String _taxNo;

    
        @PropMeta(propId=10)
    
        public String getTaxNo(){
            return _taxNo;
        }

        public void setTaxNo(String value){
            this._taxNo = value;
        }


        private java.math.BigDecimal _creditLimit;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getCreditLimit(){
            return _creditLimit;
        }

        public void setCreditLimit(java.math.BigDecimal value){
            this._creditLimit = value;
        }


        private Integer _creditPeriodDays;

    
        @PropMeta(propId=12)
    
        public Integer getCreditPeriodDays(){
            return _creditPeriodDays;
        }

        public void setCreditPeriodDays(Integer value){
            this._creditPeriodDays = value;
        }


        private java.math.BigDecimal _receivableBalance;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getReceivableBalance(){
            return _receivableBalance;
        }

        public void setReceivableBalance(java.math.BigDecimal value){
            this._receivableBalance = value;
        }


        private java.math.BigDecimal _payableBalance;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getPayableBalance(){
            return _payableBalance;
        }

        public void setPayableBalance(java.math.BigDecimal value){
            this._payableBalance = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=16)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=17)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=19)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=21)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _customerGroup;

    
        @PropMeta(propId=100)
    
        public String getCustomerGroup(){
            return _customerGroup;
        }

        public void setCustomerGroup(String value){
            this._customerGroup = value;
        }


        private List<Map<String,Object>> _addresses;

        public List<Map<String,Object>> getAddresses(){
            return _addresses;
        }

        public void setAddresses(List<Map<String,Object>> value){
            this._addresses = value;
        }


        private List<Map<String,Object>> _contacts;

        public List<Map<String,Object>> getContacts(){
            return _contacts;
        }

        public void setContacts(List<Map<String,Object>> value){
            this._contacts = value;
        }


        private List<Map<String,Object>> _bankAccounts;

        public List<Map<String,Object>> getBankAccounts(){
            return _bankAccounts;
        }

        public void setBankAccounts(List<Map<String,Object>> value){
            this._bankAccounts = value;
        }


    }
