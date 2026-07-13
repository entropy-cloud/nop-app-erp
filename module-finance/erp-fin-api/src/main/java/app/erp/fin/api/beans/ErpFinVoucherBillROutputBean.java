//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinVoucherBillROutputBean {

    
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


        private String _businessType_label;

    
        public String getBusinessType_label(){
            return _businessType_label;
        }

        public void setBusinessType_label(String value){
            this._businessType_label = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=7)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=8)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=9)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=11)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _voucherCode;

    
        public String getVoucherCode(){
            return _voucherCode;
        }

        public void setVoucherCode(String value){
            this._voucherCode = value;
        }


        private Map<String,Object> _voucher;

        public Map<String,Object> getVoucher(){
            return _voucher;
        }

        public void setVoucher(Map<String,Object> value){
            this._voucher = value;
        }


    }
