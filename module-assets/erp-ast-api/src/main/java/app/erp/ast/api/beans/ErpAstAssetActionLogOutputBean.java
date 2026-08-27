//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstAssetActionLogOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _assetId;

    
        @PropMeta(propId=2)
    
        public String getAssetId(){
            return _assetId;
        }

        public void setAssetId(String value){
            this._assetId = value;
        }


        private String _eventType;

    
        @PropMeta(propId=3)
    
        public String getEventType(){
            return _eventType;
        }

        public void setEventType(String value){
            this._eventType = value;
        }


        private String _eventType_label;

    
        public String getEventType_label(){
            return _eventType_label;
        }

        public void setEventType_label(String value){
            this._eventType_label = value;
        }


        private String _fromStatus;

    
        @PropMeta(propId=4)
    
        public String getFromStatus(){
            return _fromStatus;
        }

        public void setFromStatus(String value){
            this._fromStatus = value;
        }


        private String _fromStatus_label;

    
        public String getFromStatus_label(){
            return _fromStatus_label;
        }

        public void setFromStatus_label(String value){
            this._fromStatus_label = value;
        }


        private String _toStatus;

    
        @PropMeta(propId=5)
    
        public String getToStatus(){
            return _toStatus;
        }

        public void setToStatus(String value){
            this._toStatus = value;
        }


        private String _toStatus_label;

    
        public String getToStatus_label(){
            return _toStatus_label;
        }

        public void setToStatus_label(String value){
            this._toStatus_label = value;
        }


        private String _fromDepartmentId;

    
        @PropMeta(propId=6)
    
        public String getFromDepartmentId(){
            return _fromDepartmentId;
        }

        public void setFromDepartmentId(String value){
            this._fromDepartmentId = value;
        }


        private String _toDepartmentId;

    
        @PropMeta(propId=7)
    
        public String getToDepartmentId(){
            return _toDepartmentId;
        }

        public void setToDepartmentId(String value){
            this._toDepartmentId = value;
        }


        private String _fromLocationId;

    
        @PropMeta(propId=8)
    
        public String getFromLocationId(){
            return _fromLocationId;
        }

        public void setFromLocationId(String value){
            this._fromLocationId = value;
        }


        private String _toLocationId;

    
        @PropMeta(propId=9)
    
        public String getToLocationId(){
            return _toLocationId;
        }

        public void setToLocationId(String value){
            this._toLocationId = value;
        }


        private String _fromStaffId;

    
        @PropMeta(propId=10)
    
        public String getFromStaffId(){
            return _fromStaffId;
        }

        public void setFromStaffId(String value){
            this._fromStaffId = value;
        }


        private String _toStaffId;

    
        @PropMeta(propId=11)
    
        public String getToStaffId(){
            return _toStaffId;
        }

        public void setToStaffId(String value){
            this._toStaffId = value;
        }


        private String _refEntityName;

    
        @PropMeta(propId=12)
    
        public String getRefEntityName(){
            return _refEntityName;
        }

        public void setRefEntityName(String value){
            this._refEntityName = value;
        }


        private String _refEntityId;

    
        @PropMeta(propId=13)
    
        public String getRefEntityId(){
            return _refEntityId;
        }

        public void setRefEntityId(String value){
            this._refEntityId = value;
        }


        private String _summary;

    
        @PropMeta(propId=14)
    
        public String getSummary(){
            return _summary;
        }

        public void setSummary(String value){
            this._summary = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=15)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=16)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=17)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=18)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=19)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _asset;

        public Map<String,Object> getAsset(){
            return _asset;
        }

        public void setAsset(Map<String,Object> value){
            this._asset = value;
        }


    }
