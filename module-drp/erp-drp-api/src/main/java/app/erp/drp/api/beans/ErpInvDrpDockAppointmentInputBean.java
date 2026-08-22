//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvDrpDockAppointmentInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _warehouseId;

    
        @PropMeta(propId=2)
    
        public String getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(String value){
            this._warehouseId = value;
        }


        private String _dockId;

    
        @PropMeta(propId=3)
    
        public String getDockId(){
            return _dockId;
        }

        public void setDockId(String value){
            this._dockId = value;
        }


        private java.time.LocalDate _appointmentDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getAppointmentDate(){
            return _appointmentDate;
        }

        public void setAppointmentDate(java.time.LocalDate value){
            this._appointmentDate = value;
        }


        private java.sql.Timestamp _slotStart;

    
        @PropMeta(propId=5)
    
        public java.sql.Timestamp getSlotStart(){
            return _slotStart;
        }

        public void setSlotStart(java.sql.Timestamp value){
            this._slotStart = value;
        }


        private java.sql.Timestamp _slotEnd;

    
        @PropMeta(propId=6)
    
        public java.sql.Timestamp getSlotEnd(){
            return _slotEnd;
        }

        public void setSlotEnd(java.sql.Timestamp value){
            this._slotEnd = value;
        }


        private String _crossDockId;

    
        @PropMeta(propId=7)
    
        public String getCrossDockId(){
            return _crossDockId;
        }

        public void setCrossDockId(String value){
            this._crossDockId = value;
        }


        private String _carrierInfo;

    
        @PropMeta(propId=8)
    
        public String getCarrierInfo(){
            return _carrierInfo;
        }

        public void setCarrierInfo(String value){
            this._carrierInfo = value;
        }


        private String _status;

    
        @PropMeta(propId=9)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _orgId;

    
        @PropMeta(propId=17)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


    }
