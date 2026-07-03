//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinAccountingPeriodStatusInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _periodId;

    
        @PropMeta(propId=2)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private Long _acctSchemaId;

    
        @PropMeta(propId=3)
    
        public Long getAcctSchemaId(){
            return _acctSchemaId;
        }

        public void setAcctSchemaId(Long value){
            this._acctSchemaId = value;
        }


        private Integer _totalVouchers;

    
        @PropMeta(propId=4)
    
        public Integer getTotalVouchers(){
            return _totalVouchers;
        }

        public void setTotalVouchers(Integer value){
            this._totalVouchers = value;
        }


        private Integer _postedVouchers;

    
        @PropMeta(propId=5)
    
        public Integer getPostedVouchers(){
            return _postedVouchers;
        }

        public void setPostedVouchers(Integer value){
            this._postedVouchers = value;
        }


        private Integer _unpostedVouchers;

    
        @PropMeta(propId=6)
    
        public Integer getUnpostedVouchers(){
            return _unpostedVouchers;
        }

        public void setUnpostedVouchers(Integer value){
            this._unpostedVouchers = value;
        }


        private String _arStatus;

    
        @PropMeta(propId=7)
    
        public String getArStatus(){
            return _arStatus;
        }

        public void setArStatus(String value){
            this._arStatus = value;
        }


        private String _apStatus;

    
        @PropMeta(propId=8)
    
        public String getApStatus(){
            return _apStatus;
        }

        public void setApStatus(String value){
            this._apStatus = value;
        }


        private String _invStatus;

    
        @PropMeta(propId=9)
    
        public String getInvStatus(){
            return _invStatus;
        }

        public void setInvStatus(String value){
            this._invStatus = value;
        }


        private String _glStatus;

    
        @PropMeta(propId=10)
    
        public String getGlStatus(){
            return _glStatus;
        }

        public void setGlStatus(String value){
            this._glStatus = value;
        }


        private String _assetStatus;

    
        @PropMeta(propId=11)
    
        public String getAssetStatus(){
            return _assetStatus;
        }

        public void setAssetStatus(String value){
            this._assetStatus = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


    }
