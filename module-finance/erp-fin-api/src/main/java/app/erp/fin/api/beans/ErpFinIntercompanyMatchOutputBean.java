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

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _orgId;

    
        @PropMeta(propId=3)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
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


        private String _periodId;

    
        @PropMeta(propId=5)
    
        public String getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(String value){
            this._periodId = value;
        }


        private String _arSideVoucherId;

    
        @PropMeta(propId=6)
    
        public String getArSideVoucherId(){
            return _arSideVoucherId;
        }

        public void setArSideVoucherId(String value){
            this._arSideVoucherId = value;
        }


        private String _arOrgId;

    
        @PropMeta(propId=7)
    
        public String getArOrgId(){
            return _arOrgId;
        }

        public void setArOrgId(String value){
            this._arOrgId = value;
        }


        private String _apSideVoucherId;

    
        @PropMeta(propId=8)
    
        public String getApSideVoucherId(){
            return _apSideVoucherId;
        }

        public void setApSideVoucherId(String value){
            this._apSideVoucherId = value;
        }


        private String _apOrgId;

    
        @PropMeta(propId=9)
    
        public String getApOrgId(){
            return _apOrgId;
        }

        public void setApOrgId(String value){
            this._apOrgId = value;
        }


        private String _materialId;

    
        @PropMeta(propId=10)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
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
