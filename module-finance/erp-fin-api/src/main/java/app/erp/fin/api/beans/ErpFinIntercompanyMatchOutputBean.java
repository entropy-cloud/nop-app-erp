//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinIntercompanyMatchOutputBean {

    
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


        private String _pairKey;

    
        @PropMeta(propId=4)
    
        public String getPairKey(){
            return _pairKey;
        }

        public void setPairKey(String value){
            this._pairKey = value;
        }


        private Long _periodId;

    
        @PropMeta(propId=5)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
            this._periodId = value;
        }


        private Long _arSideVoucherId;

    
        @PropMeta(propId=6)
    
        public Long getArSideVoucherId(){
            return _arSideVoucherId;
        }

        public void setArSideVoucherId(Long value){
            this._arSideVoucherId = value;
        }


        private Long _arOrgId;

    
        @PropMeta(propId=7)
    
        public Long getArOrgId(){
            return _arOrgId;
        }

        public void setArOrgId(Long value){
            this._arOrgId = value;
        }


        private Long _apSideVoucherId;

    
        @PropMeta(propId=8)
    
        public Long getApSideVoucherId(){
            return _apSideVoucherId;
        }

        public void setApSideVoucherId(Long value){
            this._apSideVoucherId = value;
        }


        private Long _apOrgId;

    
        @PropMeta(propId=9)
    
        public Long getApOrgId(){
            return _apOrgId;
        }

        public void setApOrgId(Long value){
            this._apOrgId = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=10)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private java.math.BigDecimal _matchedAmount;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getMatchedAmount(){
            return _matchedAmount;
        }

        public void setMatchedAmount(java.math.BigDecimal value){
            this._matchedAmount = value;
        }


        private java.math.BigDecimal _diffAmount;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getDiffAmount(){
            return _diffAmount;
        }

        public void setDiffAmount(java.math.BigDecimal value){
            this._diffAmount = value;
        }


        private String _status;

    
        @PropMeta(propId=13)
    
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


        private Long _delVersion;

    
        @PropMeta(propId=14)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=15)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=16)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=18)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Map<String,Object> _period;

        public Map<String,Object> getPeriod(){
            return _period;
        }

        public void setPeriod(Map<String,Object> value){
            this._period = value;
        }


        private Map<String,Object> _arSideVoucher;

        public Map<String,Object> getArSideVoucher(){
            return _arSideVoucher;
        }

        public void setArSideVoucher(Map<String,Object> value){
            this._arSideVoucher = value;
        }


        private Map<String,Object> _apSideVoucher;

        public Map<String,Object> getApSideVoucher(){
            return _apSideVoucher;
        }

        public void setApSideVoucher(Map<String,Object> value){
            this._apSideVoucher = value;
        }


        private Map<String,Object> _material;

        public Map<String,Object> getMaterial(){
            return _material;
        }

        public void setMaterial(Map<String,Object> value){
            this._material = value;
        }


        private Map<String,Object> _arOrg;

        public Map<String,Object> getArOrg(){
            return _arOrg;
        }

        public void setArOrg(Map<String,Object> value){
            this._arOrg = value;
        }


        private Map<String,Object> _apOrg;

        public Map<String,Object> getApOrg(){
            return _apOrg;
        }

        public void setApOrg(Map<String,Object> value){
            this._apOrg = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
