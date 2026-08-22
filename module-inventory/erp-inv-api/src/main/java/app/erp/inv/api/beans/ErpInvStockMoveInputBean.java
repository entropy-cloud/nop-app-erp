//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvStockMoveInputBean extends CrudInputBase {

    
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


        private String _moveType;

    
        @PropMeta(propId=3)
    
        public String getMoveType(){
            return _moveType;
        }

        public void setMoveType(String value){
            this._moveType = value;
        }


        private String _orgId;

    
        @PropMeta(propId=4)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _sourceWarehouseId;

    
        @PropMeta(propId=6)
    
        public String getSourceWarehouseId(){
            return _sourceWarehouseId;
        }

        public void setSourceWarehouseId(String value){
            this._sourceWarehouseId = value;
        }


        private String _sourceLocationId;

    
        @PropMeta(propId=7)
    
        public String getSourceLocationId(){
            return _sourceLocationId;
        }

        public void setSourceLocationId(String value){
            this._sourceLocationId = value;
        }


        private String _destWarehouseId;

    
        @PropMeta(propId=8)
    
        public String getDestWarehouseId(){
            return _destWarehouseId;
        }

        public void setDestWarehouseId(String value){
            this._destWarehouseId = value;
        }


        private String _destLocationId;

    
        @PropMeta(propId=9)
    
        public String getDestLocationId(){
            return _destLocationId;
        }

        public void setDestLocationId(String value){
            this._destLocationId = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=10)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=11)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _relatedBillType;

    
        @PropMeta(propId=15)
    
        public String getRelatedBillType(){
            return _relatedBillType;
        }

        public void setRelatedBillType(String value){
            this._relatedBillType = value;
        }


        private String _relatedBillCode;

    
        @PropMeta(propId=16)
    
        public String getRelatedBillCode(){
            return _relatedBillCode;
        }

        public void setRelatedBillCode(String value){
            this._relatedBillCode = value;
        }


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _originMoveId;

    
        @PropMeta(propId=24)
    
        public String getOriginMoveId(){
            return _originMoveId;
        }

        public void setOriginMoveId(String value){
            this._originMoveId = value;
        }


        private String _originReturnedMoveId;

    
        @PropMeta(propId=25)
    
        public String getOriginReturnedMoveId(){
            return _originReturnedMoveId;
        }

        public void setOriginReturnedMoveId(String value){
            this._originReturnedMoveId = value;
        }


        private List<ErpInvStockMoveLineInputBean> _lines;

        public List<ErpInvStockMoveLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpInvStockMoveLineInputBean> value){
            this._lines = value;
        }


    }
