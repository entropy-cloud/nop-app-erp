//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinVoucherBillRInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _voucherId;

    
        @PropMeta(propId=2)
    
        public Long getVoucherId(){
            return _voucherId;
        }

        public void setVoucherId(Long value){
            this._voucherId = value;
        }


        private String _billType;

    
        @PropMeta(propId=3)
    
        public String getBillType(){
            return _billType;
        }

        public void setBillType(String value){
            this._billType = value;
        }


        private String _billCode;

    
        @PropMeta(propId=4)
    
        public String getBillCode(){
            return _billCode;
        }

        public void setBillCode(String value){
            this._billCode = value;
        }


        private String _billLineCode;

    
        @PropMeta(propId=5)
    
        public String getBillLineCode(){
            return _billLineCode;
        }

        public void setBillLineCode(String value){
            this._billLineCode = value;
        }


        private String _businessType;

    
        @PropMeta(propId=6)
    
        public String getBusinessType(){
            return _businessType;
        }

        public void setBusinessType(String value){
            this._businessType = value;
        }


    }
