//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinAccountingPeriodStatusOutputBean {

    
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


        private Integer _arStatus;

    
        @PropMeta(propId=7)
    
        public Integer getArStatus(){
            return _arStatus;
        }

        public void setArStatus(Integer value){
            this._arStatus = value;
        }


        private String _arStatus_label;

    
        public String getArStatus_label(){
            return _arStatus_label;
        }

        public void setArStatus_label(String value){
            this._arStatus_label = value;
        }


        private Integer _apStatus;

    
        @PropMeta(propId=8)
    
        public Integer getApStatus(){
            return _apStatus;
        }

        public void setApStatus(Integer value){
            this._apStatus = value;
        }


        private String _apStatus_label;

    
        public String getApStatus_label(){
            return _apStatus_label;
        }

        public void setApStatus_label(String value){
            this._apStatus_label = value;
        }


        private Integer _invStatus;

    
        @PropMeta(propId=9)
    
        public Integer getInvStatus(){
            return _invStatus;
        }

        public void setInvStatus(Integer value){
            this._invStatus = value;
        }


        private String _invStatus_label;

    
        public String getInvStatus_label(){
            return _invStatus_label;
        }

        public void setInvStatus_label(String value){
            this._invStatus_label = value;
        }


        private Integer _glStatus;

    
        @PropMeta(propId=10)
    
        public Integer getGlStatus(){
            return _glStatus;
        }

        public void setGlStatus(Integer value){
            this._glStatus = value;
        }


        private String _glStatus_label;

    
        public String getGlStatus_label(){
            return _glStatus_label;
        }

        public void setGlStatus_label(String value){
            this._glStatus_label = value;
        }


        private Integer _assetStatus;

    
        @PropMeta(propId=11)
    
        public Integer getAssetStatus(){
            return _assetStatus;
        }

        public void setAssetStatus(Integer value){
            this._assetStatus = value;
        }


        private String _assetStatus_label;

    
        public String getAssetStatus_label(){
            return _assetStatus_label;
        }

        public void setAssetStatus_label(String value){
            this._assetStatus_label = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=13)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=14)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=16)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _period;

        public Map<String,Object> getPeriod(){
            return _period;
        }

        public void setPeriod(Map<String,Object> value){
            this._period = value;
        }


        private Map<String,Object> _acctSchema;

        public Map<String,Object> getAcctSchema(){
            return _acctSchema;
        }

        public void setAcctSchema(Map<String,Object> value){
            this._acctSchema = value;
        }


    }
