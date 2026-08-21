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


        private String _remark;

    
        @PropMeta(propId=19)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
