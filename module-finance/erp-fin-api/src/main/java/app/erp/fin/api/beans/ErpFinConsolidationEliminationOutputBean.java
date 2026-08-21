//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinConsolidationEliminationOutputBean {

    
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


        private String _eliminationType;

    
        @PropMeta(propId=4)
    
        public String getEliminationType(){
            return _eliminationType;
        }

        public void setEliminationType(String value){
            this._eliminationType = value;
        }


        private String _eliminationType_label;

    
        public String getEliminationType_label(){
            return _eliminationType_label;
        }

        public void setEliminationType_label(String value){
            this._eliminationType_label = value;
        }


        private String _periodId;

    
        @PropMeta(propId=5)
    
        public String getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(String value){
            this._periodId = value;
        }


        private String _pairKey;

    
        @PropMeta(propId=6)
    
        public String getPairKey(){
            return _pairKey;
        }

        public void setPairKey(String value){
            this._pairKey = value;
        }


        private String _matchId;

    
        @PropMeta(propId=7)
    
        public String getMatchId(){
            return _matchId;
        }

        public void setMatchId(String value){
            this._matchId = value;
        }


        private String _fromOrgId;

    
        @PropMeta(propId=8)
    
        public String getFromOrgId(){
            return _fromOrgId;
        }

        public void setFromOrgId(String value){
            this._fromOrgId = value;
        }


        private String _toOrgId;

    
        @PropMeta(propId=9)
    
        public String getToOrgId(){
            return _toOrgId;
        }

        public void setToOrgId(String value){
            this._toOrgId = value;
        }


        private java.math.BigDecimal _eliminationAmount;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getEliminationAmount(){
            return _eliminationAmount;
        }

        public void setEliminationAmount(java.math.BigDecimal value){
            this._eliminationAmount = value;
        }


        private String _draftVoucherId;

    
        @PropMeta(propId=11)
    
        public String getDraftVoucherId(){
            return _draftVoucherId;
        }

        public void setDraftVoucherId(String value){
            this._draftVoucherId = value;
        }


        private String _status;

    
        @PropMeta(propId=12)
    
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

    
        @PropMeta(propId=13)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=14)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=15)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=17)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=19)
    
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


        private Map<String,Object> _match;

        public Map<String,Object> getMatch(){
            return _match;
        }

        public void setMatch(Map<String,Object> value){
            this._match = value;
        }


        private Map<String,Object> _draftVoucher;

        public Map<String,Object> getDraftVoucher(){
            return _draftVoucher;
        }

        public void setDraftVoucher(Map<String,Object> value){
            this._draftVoucher = value;
        }


        private Map<String,Object> _fromOrg;

        public Map<String,Object> getFromOrg(){
            return _fromOrg;
        }

        public void setFromOrg(Map<String,Object> value){
            this._fromOrg = value;
        }


        private Map<String,Object> _toOrg;

        public Map<String,Object> getToOrg(){
            return _toOrg;
        }

        public void setToOrg(Map<String,Object> value){
            this._toOrg = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
