//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvDrpCrossDockInputBean extends CrudInputBase {

    
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


        private Long _drpLineId;

    
        @PropMeta(propId=4)
    
        public Long getDrpLineId(){
            return _drpLineId;
        }

        public void setDrpLineId(Long value){
            this._drpLineId = value;
        }


        private Long _inboundMoveId;

    
        @PropMeta(propId=5)
    
        public Long getInboundMoveId(){
            return _inboundMoveId;
        }

        public void setInboundMoveId(Long value){
            this._inboundMoveId = value;
        }


        private Long _outboundMoveId;

    
        @PropMeta(propId=6)
    
        public Long getOutboundMoveId(){
            return _outboundMoveId;
        }

        public void setOutboundMoveId(Long value){
            this._outboundMoveId = value;
        }


        private String _sourceBillType;

    
        @PropMeta(propId=7)
    
        public String getSourceBillType(){
            return _sourceBillType;
        }

        public void setSourceBillType(String value){
            this._sourceBillType = value;
        }


        private String _sourceBillCode;

    
        @PropMeta(propId=8)
    
        public String getSourceBillCode(){
            return _sourceBillCode;
        }

        public void setSourceBillCode(String value){
            this._sourceBillCode = value;
        }


        private String _targetBillType;

    
        @PropMeta(propId=9)
    
        public String getTargetBillType(){
            return _targetBillType;
        }

        public void setTargetBillType(String value){
            this._targetBillType = value;
        }


        private String _targetBillCode;

    
        @PropMeta(propId=10)
    
        public String getTargetBillCode(){
            return _targetBillCode;
        }

        public void setTargetBillCode(String value){
            this._targetBillCode = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=11)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private java.math.BigDecimal _quantity;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getQuantity(){
            return _quantity;
        }

        public void setQuantity(java.math.BigDecimal value){
            this._quantity = value;
        }


        private Long _stagingLocationId;

    
        @PropMeta(propId=13)
    
        public Long getStagingLocationId(){
            return _stagingLocationId;
        }

        public void setStagingLocationId(Long value){
            this._stagingLocationId = value;
        }


        private java.sql.Timestamp _dockSlotTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getDockSlotTime(){
            return _dockSlotTime;
        }

        public void setDockSlotTime(java.sql.Timestamp value){
            this._dockSlotTime = value;
        }


        private String _status;

    
        @PropMeta(propId=15)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private java.sql.Timestamp _matchedAt;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getMatchedAt(){
            return _matchedAt;
        }

        public void setMatchedAt(java.sql.Timestamp value){
            this._matchedAt = value;
        }


        private java.sql.Timestamp _loadedAt;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getLoadedAt(){
            return _loadedAt;
        }

        public void setLoadedAt(java.sql.Timestamp value){
            this._loadedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=18)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
