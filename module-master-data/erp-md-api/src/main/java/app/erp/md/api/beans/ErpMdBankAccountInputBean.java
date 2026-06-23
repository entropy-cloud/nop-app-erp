//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdBankAccountInputBean extends CrudInputBase {

    
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


        private String _bankName;

    
        @PropMeta(propId=3)
    
        public String getBankName(){
            return _bankName;
        }

        public void setBankName(String value){
            this._bankName = value;
        }


        private String _bankBranch;

    
        @PropMeta(propId=4)
    
        public String getBankBranch(){
            return _bankBranch;
        }

        public void setBankBranch(String value){
            this._bankBranch = value;
        }


        private String _bankAccount;

    
        @PropMeta(propId=5)
    
        public String getBankAccount(){
            return _bankAccount;
        }

        public void setBankAccount(String value){
            this._bankAccount = value;
        }


        private Integer _accountType;

    
        @PropMeta(propId=6)
    
        public Integer getAccountType(){
            return _accountType;
        }

        public void setAccountType(Integer value){
            this._accountType = value;
        }


        private String _accountHolder;

    
        @PropMeta(propId=7)
    
        public String getAccountHolder(){
            return _accountHolder;
        }

        public void setAccountHolder(String value){
            this._accountHolder = value;
        }


        private Boolean _isDefault;

    
        @PropMeta(propId=8)
    
        public Boolean getIsDefault(){
            return _isDefault;
        }

        public void setIsDefault(Boolean value){
            this._isDefault = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
