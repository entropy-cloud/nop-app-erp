//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinIntercompanyMatchInputBean extends CrudInputBase {

    
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


        private String _remark;

    
        @PropMeta(propId=20)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
