//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinConsolidationEliminationInputBean extends CrudInputBase {

    
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


        private String _eliminationType;

    
        @PropMeta(propId=4)
    
        public String getEliminationType(){
            return _eliminationType;
        }

        public void setEliminationType(String value){
            this._eliminationType = value;
        }


        private Long _periodId;

    
        @PropMeta(propId=5)
    
        public Long getPeriodId(){
            return _periodId;
        }

        public void setPeriodId(Long value){
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


        private Long _matchId;

    
        @PropMeta(propId=7)
    
        public Long getMatchId(){
            return _matchId;
        }

        public void setMatchId(Long value){
            this._matchId = value;
        }


        private Long _fromOrgId;

    
        @PropMeta(propId=8)
    
        public Long getFromOrgId(){
            return _fromOrgId;
        }

        public void setFromOrgId(Long value){
            this._fromOrgId = value;
        }


        private Long _toOrgId;

    
        @PropMeta(propId=9)
    
        public Long getToOrgId(){
            return _toOrgId;
        }

        public void setToOrgId(Long value){
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


        private Long _draftVoucherId;

    
        @PropMeta(propId=11)
    
        public Long getDraftVoucherId(){
            return _draftVoucherId;
        }

        public void setDraftVoucherId(Long value){
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


        private String _remark;

    
        @PropMeta(propId=19)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
