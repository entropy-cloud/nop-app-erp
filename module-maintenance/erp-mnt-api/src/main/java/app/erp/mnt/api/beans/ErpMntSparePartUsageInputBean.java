//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntSparePartUsageInputBean extends CrudInputBase {

    
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


        private String _visitId;

    
        @PropMeta(propId=4)
    
        public String getVisitId(){
            return _visitId;
        }

        public void setVisitId(String value){
            this._visitId = value;
        }


        private String _requestId;

    
        @PropMeta(propId=5)
    
        public String getRequestId(){
            return _requestId;
        }

        public void setRequestId(String value){
            this._requestId = value;
        }


        private String _equipmentId;

    
        @PropMeta(propId=6)
    
        public String getEquipmentId(){
            return _equipmentId;
        }

        public void setEquipmentId(String value){
            this._equipmentId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=8)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private java.math.BigDecimal _totalAmount;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getTotalAmount(){
            return _totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal value){
            this._totalAmount = value;
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


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpMntSparePartUsageLineInputBean> _lines;

        public List<ErpMntSparePartUsageLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpMntSparePartUsageLineInputBean> value){
            this._lines = value;
        }


    }
