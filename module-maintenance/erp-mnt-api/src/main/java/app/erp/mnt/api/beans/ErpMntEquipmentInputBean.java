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
    public class ErpMntEquipmentInputBean extends CrudInputBase {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private String _orgId;

    
        @PropMeta(propId=4)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _assetId;

    
        @PropMeta(propId=5)
    
        public String getAssetId(){
            return _assetId;
        }

        public void setAssetId(String value){
            this._assetId = value;
        }


        private String _workcenterId;

    
        @PropMeta(propId=6)
    
        public String getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(String value){
            this._workcenterId = value;
        }


        private String _locationId;

    
        @PropMeta(propId=7)
    
        public String getLocationId(){
            return _locationId;
        }

        public void setLocationId(String value){
            this._locationId = value;
        }


        private String _categoryId;

    
        @PropMeta(propId=8)
    
        public String getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(String value){
            this._categoryId = value;
        }


        private String _status;

    
        @PropMeta(propId=9)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _serialNo;

    
        @PropMeta(propId=10)
    
        public String getSerialNo(){
            return _serialNo;
        }

        public void setSerialNo(String value){
            this._serialNo = value;
        }


        private String _manufacturer;

    
        @PropMeta(propId=11)
    
        public String getManufacturer(){
            return _manufacturer;
        }

        public void setManufacturer(String value){
            this._manufacturer = value;
        }


        private String _model;

    
        @PropMeta(propId=12)
    
        public String getModel(){
            return _model;
        }

        public void setModel(String value){
            this._model = value;
        }


        private java.time.LocalDate _installDate;

    
        @PropMeta(propId=13)
    
        public java.time.LocalDate getInstallDate(){
            return _installDate;
        }

        public void setInstallDate(java.time.LocalDate value){
            this._installDate = value;
        }


        private java.time.LocalDate _warrantyExpiry;

    
        @PropMeta(propId=14)
    
        public java.time.LocalDate getWarrantyExpiry(){
            return _warrantyExpiry;
        }

        public void setWarrantyExpiry(java.time.LocalDate value){
            this._warrantyExpiry = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpMntVisitInputBean> _visits;

        public List<ErpMntVisitInputBean> getVisits(){
            return _visits;
        }

        public void setVisits(List<ErpMntVisitInputBean> value){
            this._visits = value;
        }


        private List<ErpMntScheduleInputBean> _schedules;

        public List<ErpMntScheduleInputBean> getSchedules(){
            return _schedules;
        }

        public void setSchedules(List<ErpMntScheduleInputBean> value){
            this._schedules = value;
        }


        private List<ErpMntRequestInputBean> _requests;

        public List<ErpMntRequestInputBean> getRequests(){
            return _requests;
        }

        public void setRequests(List<ErpMntRequestInputBean> value){
            this._requests = value;
        }


        private List<ErpMntSparePartUsageInputBean> _sparePartUsages;

        public List<ErpMntSparePartUsageInputBean> getSparePartUsages(){
            return _sparePartUsages;
        }

        public void setSparePartUsages(List<ErpMntSparePartUsageInputBean> value){
            this._sparePartUsages = value;
        }


        private List<ErpMntDowntimeEntryInputBean> _downtimeEntries;

        public List<ErpMntDowntimeEntryInputBean> getDowntimeEntries(){
            return _downtimeEntries;
        }

        public void setDowntimeEntries(List<ErpMntDowntimeEntryInputBean> value){
            this._downtimeEntries = value;
        }


    }
