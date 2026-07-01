//__XGEN_FORCE_OVERRIDE__
    package app.erp.mnt.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMntEquipmentOutputBean {

    
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


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _assetId;

    
        @PropMeta(propId=5)
    
        public Long getAssetId(){
            return _assetId;
        }

        public void setAssetId(Long value){
            this._assetId = value;
        }


        private Long _workcenterId;

    
        @PropMeta(propId=6)
    
        public Long getWorkcenterId(){
            return _workcenterId;
        }

        public void setWorkcenterId(Long value){
            this._workcenterId = value;
        }


        private Long _locationId;

    
        @PropMeta(propId=7)
    
        public Long getLocationId(){
            return _locationId;
        }

        public void setLocationId(Long value){
            this._locationId = value;
        }


        private Long _categoryId;

    
        @PropMeta(propId=8)
    
        public Long getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(Long value){
            this._categoryId = value;
        }


        private Integer _status;

    
        @PropMeta(propId=9)
    
        public Integer getStatus(){
            return _status;
        }

        public void setStatus(Integer value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=17)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=18)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=20)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _location;

        public Map<String,Object> getLocation(){
            return _location;
        }

        public void setLocation(Map<String,Object> value){
            this._location = value;
        }


        private Map<String,Object> _category;

        public Map<String,Object> getCategory(){
            return _category;
        }

        public void setCategory(Map<String,Object> value){
            this._category = value;
        }


        private Map<String,Object> _asset;

        public Map<String,Object> getAsset(){
            return _asset;
        }

        public void setAsset(Map<String,Object> value){
            this._asset = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private List<Map<String,Object>> _visits;

        public List<Map<String,Object>> getVisits(){
            return _visits;
        }

        public void setVisits(List<Map<String,Object>> value){
            this._visits = value;
        }


        private List<Map<String,Object>> _schedules;

        public List<Map<String,Object>> getSchedules(){
            return _schedules;
        }

        public void setSchedules(List<Map<String,Object>> value){
            this._schedules = value;
        }


        private List<Map<String,Object>> _requests;

        public List<Map<String,Object>> getRequests(){
            return _requests;
        }

        public void setRequests(List<Map<String,Object>> value){
            this._requests = value;
        }


        private List<Map<String,Object>> _sparePartUsages;

        public List<Map<String,Object>> getSparePartUsages(){
            return _sparePartUsages;
        }

        public void setSparePartUsages(List<Map<String,Object>> value){
            this._sparePartUsages = value;
        }


        private List<Map<String,Object>> _downtimeEntries;

        public List<Map<String,Object>> getDowntimeEntries(){
            return _downtimeEntries;
        }

        public void setDowntimeEntries(List<Map<String,Object>> value){
            this._downtimeEntries = value;
        }


    }
