//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvSerialNumberInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _orgId;

    
        @PropMeta(propId=2)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _serialNo;

    
        @PropMeta(propId=3)
    
        public String getSerialNo(){
            return _serialNo;
        }

        public void setSerialNo(String value){
            this._serialNo = value;
        }


        private String _materialId;

    
        @PropMeta(propId=4)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _skuId;

    
        @PropMeta(propId=5)
    
        public String getSkuId(){
            return _skuId;
        }

        public void setSkuId(String value){
            this._skuId = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=6)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _locationId;

    
        @PropMeta(propId=7)
    
        public String getLocationId(){
            return _locationId;
        }

        public void setLocationId(String value){
            this._locationId = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _inBillType;

    
        @PropMeta(propId=9)
    
        public String getInBillType(){
            return _inBillType;
        }

        public void setInBillType(String value){
            this._inBillType = value;
        }


        private String _inBillCode;

    
        @PropMeta(propId=10)
    
        public String getInBillCode(){
            return _inBillCode;
        }

        public void setInBillCode(String value){
            this._inBillCode = value;
        }


        private String _outBillType;

    
        @PropMeta(propId=11)
    
        public String getOutBillType(){
            return _outBillType;
        }

        public void setOutBillType(String value){
            this._outBillType = value;
        }


        private String _outBillCode;

    
        @PropMeta(propId=12)
    
        public String getOutBillCode(){
            return _outBillCode;
        }

        public void setOutBillCode(String value){
            this._outBillCode = value;
        }


        private String _remark;

    
        @PropMeta(propId=13)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
