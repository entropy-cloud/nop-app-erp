//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinFundAccountInputBean extends CrudInputBase {

    
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


        private String _accountType;

    
        @PropMeta(propId=5)
    
        public String getAccountType(){
            return _accountType;
        }

        public void setAccountType(String value){
            this._accountType = value;
        }


        private Long _subjectId;

    
        @PropMeta(propId=6)
    
        public Long getSubjectId(){
            return _subjectId;
        }

        public void setSubjectId(Long value){
            this._subjectId = value;
        }


        private String _bankName;

    
        @PropMeta(propId=7)
    
        public String getBankName(){
            return _bankName;
        }

        public void setBankName(String value){
            this._bankName = value;
        }


        private String _bankAccount;

    
        @PropMeta(propId=8)
    
        public String getBankAccount(){
            return _bankAccount;
        }

        public void setBankAccount(String value){
            this._bankAccount = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=9)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _openingBalance;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getOpeningBalance(){
            return _openingBalance;
        }

        public void setOpeningBalance(java.math.BigDecimal value){
            this._openingBalance = value;
        }


        private java.math.BigDecimal _currentBalance;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getCurrentBalance(){
            return _currentBalance;
        }

        public void setCurrentBalance(java.math.BigDecimal value){
            this._currentBalance = value;
        }


        private String _status;

    
        @PropMeta(propId=12)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
