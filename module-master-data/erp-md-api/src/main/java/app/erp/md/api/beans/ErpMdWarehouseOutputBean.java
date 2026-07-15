//__XGEN_FORCE_OVERRIDE__
    package app.erp.md.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMdWarehouseOutputBean {

    
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


        private String _warehouseType;

    
        @PropMeta(propId=4)
    
        public String getWarehouseType(){
            return _warehouseType;
        }

        public void setWarehouseType(String value){
            this._warehouseType = value;
        }


        private String _warehouseType_label;

    
        public String getWarehouseType_label(){
            return _warehouseType_label;
        }

        public void setWarehouseType_label(String value){
            this._warehouseType_label = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=5)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _address;

    
        @PropMeta(propId=6)
    
        public String getAddress(){
            return _address;
        }

        public void setAddress(String value){
            this._address = value;
        }


        private Long _managerId;

    
        @PropMeta(propId=7)
    
        public Long getManagerId(){
            return _managerId;
        }

        public void setManagerId(Long value){
            this._managerId = value;
        }


        private String _status;

    
        @PropMeta(propId=8)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _status_label;

    
        public String getStatus_label(){
            return _status_label;
        }

        public void setStatus_label(String value){
            this._status_label = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=9)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=10)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=11)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=12)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=13)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private String _batchSelectionStrategy;

    
        @PropMeta(propId=16)
    
        public String getBatchSelectionStrategy(){
            return _batchSelectionStrategy;
        }

        public void setBatchSelectionStrategy(String value){
            this._batchSelectionStrategy = value;
        }


        private String _batchSelectionStrategy_label;

    
        public String getBatchSelectionStrategy_label(){
            return _batchSelectionStrategy_label;
        }

        public void setBatchSelectionStrategy_label(String value){
            this._batchSelectionStrategy_label = value;
        }


        private Map<String,Object> _organization;

        public Map<String,Object> getOrganization(){
            return _organization;
        }

        public void setOrganization(Map<String,Object> value){
            this._organization = value;
        }


        private Map<String,Object> _manager;

        public Map<String,Object> getManager(){
            return _manager;
        }

        public void setManager(Map<String,Object> value){
            this._manager = value;
        }


        private List<Map<String,Object>> _locations;

        public List<Map<String,Object>> getLocations(){
            return _locations;
        }

        public void setLocations(List<Map<String,Object>> value){
            this._locations = value;
        }


    }
