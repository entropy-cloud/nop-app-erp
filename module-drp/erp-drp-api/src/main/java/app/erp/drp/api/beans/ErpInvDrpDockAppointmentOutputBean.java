//__XGEN_FORCE_OVERRIDE__
    package app.erp.drp.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvDrpDockAppointmentOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=2)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private Long _dockId;

    
        @PropMeta(propId=3)
    
        public Long getDockId(){
            return _dockId;
        }

        public void setDockId(Long value){
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


        private Long _crossDockId;

    
        @PropMeta(propId=7)
    
        public Long getCrossDockId(){
            return _crossDockId;
        }

        public void setCrossDockId(Long value){
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


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=12)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=13)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=15)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=17)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Map<String,Object> _warehouse;

        public Map<String,Object> getWarehouse(){
            return _warehouse;
        }

        public void setWarehouse(Map<String,Object> value){
            this._warehouse = value;
        }


        private Map<String,Object> _dock;

        public Map<String,Object> getDock(){
            return _dock;
        }

        public void setDock(Map<String,Object> value){
            this._dock = value;
        }


        private Map<String,Object> _crossDock;

        public Map<String,Object> getCrossDock(){
            return _crossDock;
        }

        public void setCrossDock(Map<String,Object> value){
            this._crossDock = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
