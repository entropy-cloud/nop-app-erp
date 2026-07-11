//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdPartnerInputBean extends CrudInputBase {

    
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


        private String _status;

    
        @PropMeta(propId=5)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
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


        private List<ErpMdPartnerAddressInputBean> _addresses;

        public List<ErpMdPartnerAddressInputBean> getAddresses(){
            return _addresses;
        }

        public void setAddresses(List<ErpMdPartnerAddressInputBean> value){
            this._addresses = value;
        }


        private List<ErpMdPartnerContactInputBean> _contacts;

        public List<ErpMdPartnerContactInputBean> getContacts(){
            return _contacts;
        }

        public void setContacts(List<ErpMdPartnerContactInputBean> value){
            this._contacts = value;
        }


        private List<ErpMdBankAccountInputBean> _bankAccounts;

        public List<ErpMdBankAccountInputBean> getBankAccounts(){
            return _bankAccounts;
        }

        public void setBankAccounts(List<ErpMdBankAccountInputBean> value){
            this._bankAccounts = value;
        }


    }
